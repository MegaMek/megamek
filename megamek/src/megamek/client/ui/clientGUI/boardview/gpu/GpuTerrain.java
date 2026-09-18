/* Copyright (C) 2026 The MegaMek Team. SPDX-License-Identifier: GPL-3.0-or-later */
package megamek.client.ui.clientGUI.boardview.gpu;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import com.badlogic.gdx.graphics.Camera;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.VertexAttributes;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.graphics.g3d.Environment;
import com.badlogic.gdx.graphics.g3d.Material;
import com.badlogic.gdx.graphics.g3d.Model;
import com.badlogic.gdx.graphics.g3d.ModelBatch;
import com.badlogic.gdx.graphics.g3d.ModelInstance;
import com.badlogic.gdx.graphics.g3d.attributes.BlendingAttribute;
import com.badlogic.gdx.graphics.g3d.attributes.ColorAttribute;
import com.badlogic.gdx.graphics.g3d.attributes.DepthTestAttribute;
import com.badlogic.gdx.graphics.g3d.attributes.IntAttribute;
import com.badlogic.gdx.graphics.g3d.attributes.TextureAttribute;
import com.badlogic.gdx.graphics.g3d.environment.DirectionalShadowLight;
import com.badlogic.gdx.graphics.g3d.utils.DepthShaderProvider;
import com.badlogic.gdx.graphics.g3d.utils.MeshPartBuilder;
import com.badlogic.gdx.graphics.g3d.utils.ModelBuilder;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Matrix4;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.math.collision.BoundingBox;
import com.badlogic.gdx.utils.Disposable;
import megamek.common.board.Coords;

/** Static, textured hex geometry. Only changed terrain rebuilds meshes; camera changes reuse them. */
final class GpuTerrain implements Disposable {
    private static final int CHUNK_SIZE = 16;
    /**
     * How much of the artwork's edge the 3D surfaces keep clear of, in hexagon radii: the captured tile art
     * ends at its hexagon edge, so the outermost texels carry the tileset's own hex grid and the transparent
     * pixels around it. A surface edge that samples them, which is what the hex surface does without an
     * interpolation inset, draws the filter's mix of tile and transparency as a dark rim around every hex,
     * and the opaque surface has no alpha blending to soften it. Zero samples the raw artwork edges instead.
     */
    private static final float ARTWORK_EDGE_MARGIN = 0.04f;
    /** Artwork radius the hex surface, the padding bands and the corner caps may read. */
    private static final float PAINTED_LIMIT = 1 - ARTWORK_EDGE_MARGIN;
    /**
     * The 3D view's hex grid: every hex outlines its own edge with a darkened band of its own artwork,
     * the way the tileset draws the grid into the tile edge. Drawing the outline here keeps it crisp and
     * even, instead of leaving each surface to filter the tile against the transparent margin around it.
     * The band's darkening is {@link BoardGeometry#HEX_FRAME_SHADE}, which the tuning panel drives.
     */
    private static final boolean HEX_FRAME = true;
    /** Frame width as a fraction of the hexagon radius, running inward from the hexagon edge. */
    private static final float HEX_FRAME_WIDTH = 0.03f;
    /** Small lift so the frame draws over the hex surface it outlines, which shares its plane. */
    private static final float HEX_FRAME_LIFT = 0.05f;
    /** Artwork radius where the hex fan leaves off; the padding picks up from there. */
    private static final float EDGE_SAMPLE = 0.9f;
    /** How far the feature layer floats above its hex, so it draws over the surface and the padding alike. */
    private static final float FEATURE_LIFT = 0.1f;
    /** Deepest artwork radius a step band samples at the midline; the legacy bank or face lies inside it. */
    private static final float DEEPEST_SAMPLE = 0.6f;
    /** How much deeper each level of a step pushes the sampled artwork radius. */
    private static final float SAMPLE_DEPTH_PER_LEVEL = 0.1f;
    private static final long ATTRIBUTES = VertexAttributes.Usage.Position | VertexAttributes.Usage.ColorPacked
          | VertexAttributes.Usage.TextureCoordinates | VertexAttributes.Usage.Normal;
    private final GpuTextures<Coords> textures = new GpuTextures<>();
    private final GpuTextures<Coords> bases = new GpuTextures<>();
    private final GpuTextures<Coords> featureArtwork = new GpuTextures<>();
    private final GpuTextures<Coords> annotations = new GpuTextures<>();
    private final ModelBatch batch = new ModelBatch();
    private final ModelBatch blendBatch = new ModelBatch();
    private final ModelBatch depthBatch = new ModelBatch(new DepthShaderProvider());
    private final Environment environment = new Environment();
    private final List<Chunk> chunks = new ArrayList<>();
    private List<BoardScene.Tile> tiles;
    private DirectionalShadowLight shadow;
    private BoardScene.Light light;
    private final BoundingBox shadowBounds = new BoundingBox();
    private final List<Model> shadowModels = new ArrayList<>();
    private final List<Matrix4> shadowTransforms = new ArrayList<>();
    private boolean shadowDirty;
    private float shadowDiameter;
    private int shadowMapSize;
    private int tuning = BoardGeometry.revision();

    private record Chunk(Model model, ModelInstance instance, Model padModel, ModelInstance padInstance,
          Model blendModel, ModelInstance blendInstance, Model featureModel, ModelInstance featureInstance,
          BoundingBox bounds, boolean tactical) { }

    void update(BoardScene scene) {
        boolean lightChanged = !Objects.equals(light, scene.light());
        // Every mesh is built from the tuned geometry, so a tuning change rebuilds the board from the scene.
        boolean tuningChanged = tuning != BoardGeometry.revision();
        tuning = BoardGeometry.revision();
        if (tiles == scene.tiles() && !lightChanged && !tuningChanged) {
            return;
        }
        Map<Coords, BoardScene.Pixels> ground = new HashMap<>();
        Map<Coords, BoardScene.Pixels> base = new HashMap<>();
        Map<Coords, BoardScene.Pixels> features = new HashMap<>();
        Map<Coords, BoardScene.Pixels> tactical = new HashMap<>();
        for (BoardScene.Tile tile : scene.tiles()) {
            ground.put(tile.coords(), tile.image());
            base.put(tile.coords(), tile.base());
            if (tile.features() != null) {
                features.put(tile.coords(), tile.features());
            }
            if (tile.tactical() != null) {
                tactical.put(tile.coords(), tile.tactical());
            }
        }
        boolean layoutChanged = textures.update(ground);
        layoutChanged |= bases.update(base);
        layoutChanged |= featureArtwork.update(features);
        layoutChanged |= annotations.update(tactical);
        boolean sameGeometry = !tuningChanged && tiles != null && tiles.size() == scene.tiles().size();
        if (sameGeometry) {
            for (int i = 0; i < tiles.size(); i++) {
                BoardScene.Tile before = tiles.get(i);
                BoardScene.Tile after = scene.tiles().get(i);
                if (!before.coords().equals(after.coords()) || before.elevation() != after.elevation()
                      || before.water() != after.water()) {
                    sameGeometry = false;
                    break;
                }
            }
        }
        tiles = scene.tiles();
        if (sameGeometry && !layoutChanged) {
            if (lightChanged) {
                updateShadows(scene);
            }
            return;
        }
        clearMeshes();
        float floor = BoardGeometry.floor(scene);
        for (int x = 0; x < scene.width(); x += CHUNK_SIZE) {
            for (int y = 0; y < scene.height(); y += CHUNK_SIZE) {
                buildChunk(scene, x, y, floor, false);
                buildChunk(scene, x, y, floor, true);
            }
        }
        if (!sameGeometry || lightChanged) {
            updateShadows(scene);
        }
    }

    /** The same terrain meshes cast and receive shadows. Camera motion and overlay updates reuse the depth map. */
    private void updateShadows(BoardScene scene) {
        if (shadow != null) {
            environment.remove(shadow);
            shadow.dispose();
            shadow = null;
        }
        light = scene.light();
        shadowDirty = true;
        environment.shadowMap = null;
        environment.set(ColorAttribute.createAmbientLight(1, 1, 1, 1));
        if (light == null) {
            return;
        }
                shadowBounds.inf();
                chunks.stream().filter(chunk -> !chunk.tactical()).forEach(chunk -> shadowBounds.ext(chunk.bounds()));
                shadowDiameter = shadowBounds.getDimensions(new Vector3()).len() * 1.15f;
                shadowMapSize = shadowDiameter > 2048 ? 4096 : 2048;
                shadow = new DirectionalShadowLight(shadowMapSize, shadowMapSize,
              shadowDiameter, shadowDiameter, 1, shadowDiameter + 2);
        shadow.set(0.5f, 0.5f, 0.5f, sunX(), sunY(), -BoardGeometry.LEVEL);
        shadow.getCamera().up.set(Vector3.Z);
        environment.add(shadow);
        environment.set(ColorAttribute.createAmbientLight(0.5f, 0.5f, 0.5f, 1));
        environment.shadowMap = shadow;
        renderShadows(List.of());
    }

    Environment environment() {
        return environment;
    }

    void renderShadows(List<ModelInstance> units) {
        if (shadow == null) {
            return;
        }
        boolean changed = shadowDirty || units.size() != shadowModels.size();
        for (int index = 0; !changed && index < units.size(); index++) {
            changed = units.get(index).model != shadowModels.get(index)
                || !Arrays.equals(units.get(index).transform.val, shadowTransforms.get(index).val);
        }
        if (!changed) {
            return;
        }
        shadowModels.clear();
        shadowTransforms.clear();
        BoundingBox bounds = new BoundingBox(shadowBounds);
        for (ModelInstance unit : units) {
            shadowModels.add(unit.model);
            shadowTransforms.add(new Matrix4(unit.transform));
            bounds.ext(unit.calculateBoundingBox(new BoundingBox()).mul(unit.transform));
        }
        float diameter = Math.max(shadowDiameter, bounds.getDimensions(new Vector3()).len() * 1.15f);
        shadow.getCamera().viewportWidth = diameter;
        shadow.getCamera().viewportHeight = diameter;
        shadow.getCamera().far = diameter + 2;
        shadow.begin(bounds.getCenter(new Vector3()), Vector3.Zero);
        depthBatch.begin(shadow.getCamera());
        for (Chunk chunk : chunks) {
            if (!chunk.tactical()) {
                depthBatch.render(chunk.instance());
                if (chunk.padInstance() != null) {
                    depthBatch.render(chunk.padInstance());
                }
            }
        }
        for (ModelInstance unit : units) {
            depthBatch.render(unit);
        }
        depthBatch.end();
        shadow.end();
        // Bias receiver depth by the PCF footprint on a horizontal hex. The stock shader has no shadow bias;
        // without it, adjacent depth samples incorrectly shadow the same flat surface.
        float slope = (float) Math.hypot(sunX(), sunY()) / BoardGeometry.LEVEL;
        shadow.getProjViewTrans().val[Matrix4.M23] -= 3 * Math.max(1, slope) / shadowMapSize;
        shadowDirty = false;
    }

    /**
     * The light's ground direction in world units. The classic light direction is an offset in tile artwork
     * pixels, so it scales with the hexes exactly as {@link BoardGeometry#LEVEL} does; without that, raising
     * {@code HEX_SCALE} steepens the sun and the taller terrain would cast shorter shadows.
     */
    private float sunX() {
        return light.x() * BoardGeometry.HEX_SCALE;
    }

    private float sunY() {
        return light.y() * BoardGeometry.HEX_SCALE;
    }

    /**
     * The mesh parts one chunk builds: hex artwork and tactical cells in the main model, padding bands and
     * faces in their own builder, and the padded fades and the transparent feature layers in their own.
     */
    private record Builders(ModelBuilder modelBuilder, Map<Texture, MeshPartBuilder> parts, ModelBuilder padBuilder,
          Map<Texture, MeshPartBuilder> padParts, ModelBuilder blendBuilder,
          Map<Texture, MeshPartBuilder> fadeParts, ModelBuilder featureBuilder,
          Map<Texture, MeshPartBuilder> featureParts) { }

    private void buildChunk(BoardScene scene, int startX, int startY, float floor, boolean tactical) {
        ModelBuilder modelBuilder = new ModelBuilder();
        modelBuilder.begin();
        // Padding geometry needs its own builder: a builder keeps one part open at a time, so bands added to
        // the hex builder would pull every following tile's hex artwork into the padding part. The blended
        // fades and the feature overlays need theirs for the same reason.
        ModelBuilder padBuilder = new ModelBuilder();
        padBuilder.begin();
        ModelBuilder blendBuilder = new ModelBuilder();
        blendBuilder.begin();
        ModelBuilder featureBuilder = new ModelBuilder();
        featureBuilder.begin();
        Map<Texture, MeshPartBuilder> parts = new HashMap<>();
        Map<Texture, MeshPartBuilder> padParts = new HashMap<>();
        Map<Texture, MeshPartBuilder> fadeParts = new HashMap<>();
        Map<Texture, MeshPartBuilder> featureParts = new HashMap<>();
        Builders builders = new Builders(modelBuilder, parts, padBuilder, padParts, blendBuilder, fadeParts,
              featureBuilder, featureParts);
        for (int x = startX; x < Math.min(startX + CHUNK_SIZE, scene.width()); x++) {
            for (int y = startY; y < Math.min(startY + CHUNK_SIZE, scene.height()); y++) {
                BoardScene.Tile tile = scene.tiles().get(x * scene.height() + y);
                if (tactical && tile.tactical() == null) {
                    continue;
                }
                TextureRegion region = (tactical ? annotations : textures).region(tile.coords());
                MeshPartBuilder mesh = parts.computeIfAbsent(region.getTexture(), texture -> {
                    Material material = new Material(TextureAttribute.createDiffuse(texture),
                          IntAttribute.createCullFace(GL20.GL_NONE));
                    if (tactical) {
                        material.set(new BlendingAttribute(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA));
                        material.set(new DepthTestAttribute(GL20.GL_ALWAYS, false));
                    }
                    return modelBuilder.part("terrain" + parts.size(), GL20.GL_TRIANGLES, ATTRIBUTES, material);
                });
                if (tactical) {
                    Vector3 center = BoardGeometry.center(tile.coords(), tile.elevation());
                    for (int edge = 0; edge < 6; edge++) {
                        Vector3 a = BoardGeometry.cellCorner(tile.coords(), tile.elevation(), edge);
                        Vector3 b = BoardGeometry.cellCorner(tile.coords(), tile.elevation(), edge + 1);
                        mesh.triangle(cellVertex(center, center, region, 1), cellVertex(a, center, region, 1),
                              cellVertex(b, center, region, 1));
                    }
                    continue;
                }
                TextureRegion baseRegion = bases.region(tile.coords());
                buildGround(scene, tile, floor, mesh, baseRegion, builders);
            }
        }
        Model model = modelBuilder.end();
        ModelInstance instance = new ModelInstance(model);
        Model padModel = padParts.isEmpty() ? null : padBuilder.end();
        ModelInstance padInstance = padModel == null ? null : new ModelInstance(padModel);
        Model blendModel = blendBuilder.end();
        ModelInstance blendInstance = new ModelInstance(blendModel);
        Model featureModel = featureBuilder.end();
        ModelInstance featureInstance = new ModelInstance(featureModel);
        BoundingBox bounds = instance.calculateBoundingBox(new BoundingBox());
        if (padInstance != null) {
            bounds.ext(padInstance.calculateBoundingBox(new BoundingBox()));
        }
        bounds.ext(blendInstance.calculateBoundingBox(new BoundingBox()));
        bounds.ext(featureInstance.calculateBoundingBox(new BoundingBox()));
        chunks.add(new Chunk(model, instance, padModel, padInstance, blendModel, blendInstance, featureModel,
              featureInstance, bounds, tactical));
    }

    /**
     * One tile's ground geometry: the flat hex surface and its half of every padding band. A band ramps to
     * the midline when the levels differ by less than CLIFF_LEVELS, otherwise it protrudes flat and the
     * raised tile drops a face at the midline. {@code mesh} carries the hex artwork, the padding parts built
     * through {@code padBuilder} carry the ground artwork the padding continues, and {@code fadeParts}
     * receive the neighboring artwork. Two water tiles hand their band to {@code mesh}, so it continues the
     * water surface instead of the waterless bank artwork their other sides show.
     */
    private void buildGround(BoardScene scene, BoardScene.Tile tile, float floor, MeshPartBuilder mesh,
          TextureRegion baseRegion, Builders builders) {
        ModelBuilder padBuilder = builders.padBuilder();
        Map<Texture, MeshPartBuilder> padParts = builders.padParts();
        ModelBuilder blendBuilder = builders.blendBuilder();
        Map<Texture, MeshPartBuilder> fadeParts = builders.fadeParts();
        Coords coords = tile.coords();
        int elevation = tile.elevation();
        TextureRegion region = textures.region(coords);
        Vector3 center = BoardGeometry.center(coords, elevation);
        for (int corner = 0; corner < 6; corner++) {
            Vector3 a = BoardGeometry.insetCorner(coords, elevation, corner);
            Vector3 b = BoardGeometry.insetCorner(coords, elevation, corner + 1);
            mesh.triangle(surfaceVertex(center, center, region, 1), surfaceVertex(a, center, region, 1),
                  surfaceVertex(b, center, region, 1));
        }
        // At full transparency the frame repeats the surface exactly, so it is left out as if HEX_FRAME were false.
        if (HEX_FRAME && BoardGeometry.HEX_FRAME_SHADE < 1) {
            hexFrame(mesh, coords, elevation, center, region);
        }
        // The part is created with its first band, so a tile whose bands all join water leaves it out.
        MeshPartBuilder pad = null;
        for (int edge = 0; edge < 6; edge++) {
            BoardScene.Tile neighbor = scene.tile(coords.translated(BoardGeometry.edgeDirection(edge)));
            // Two water tiles join with the hex artwork, so the water surface runs through the band; every
            // other band continues the ground artwork, which for a water tile is the waterless bank art.
            boolean water = neighbor != null && tile.water() && neighbor.water();
            // The padding artwork only exists while the hexes are spaced apart; with no gap a band lies inside
            // the hex edge and draws that hex's own artwork instead.
            boolean groundArt = BoardGeometry.GAP > 0 && !water;
            // A bank or face is drawn on the edge of a level step and wherever water meets land; mirroring that
            // artwork would repeat it, so those bands sample the surface behind it instead.
            float depth = bandDepth(tile, neighbor);
            // Both tiles of a band pin their half to this line, so a slope meets its neighbor on one edge.
            Vector3 midlineA = BoardGeometry.padPoint(new Vector3(), scene, coords, elevation, edge, 0, 1);
            Vector3 midlineB = BoardGeometry.padPoint(new Vector3(), scene, coords, elevation, edge, 1, 1);
            if (BoardGeometry.HAS_PADDING) {
                if (groundArt) {
                    pad = pad == null ? padPart(padBuilder, padParts, baseRegion.getTexture()) : pad;
                    band(pad, scene, coords, elevation, edge, center, baseRegion, depth);
                } else {
                    band(mesh, scene, coords, elevation, edge, center, region, depth);
                }
            }
            MeshPartBuilder wallMesh = groundArt ? pad : mesh;
            // A wall shows the tile's own ground art, except on a water join, where the water surface runs
            // down the step; a water tile's land sides keep their waterless bank art.
            TextureRegion wallRegion = water ? region : baseRegion;
            if (neighbor == null) {
                // The board edge protrudes flat to the midline, then drops to the floor.
                wall(wallMesh, midlineA, midlineB, new Vector3(midlineA.x, midlineA.y, floor),
                      new Vector3(midlineB.x, midlineB.y, floor), center, wallRegion, 1, 1);
                continue;
            }
            // The raised side of a level step drops a face at the shared line, and the lowered tile's artwork
            // fades in down that face, so a step interpolates between its two tiles the way a padded cliff
            // does. The face is drawn whether or not a band reaches the line: with no padding, no gap, or a
            // water join, it is the only thing closing the step.
            boolean cliff = BoardGeometry.hasCliff(elevation, neighbor.elevation());
            if (cliff) {
                float bottom = neighbor.elevation() * BoardGeometry.LEVEL;
                Vector3 lowA = new Vector3(midlineA.x, midlineA.y, bottom);
                Vector3 lowB = new Vector3(midlineB.x, midlineB.y, bottom);
                wall(wallMesh, midlineA, midlineB, lowA, lowB, center, wallRegion, 1, 1);
                // The lowered tile's artwork fades in down the face: its bank art, so land never takes on
                // water over a shore, and its water art on a water join, which runs the water down the step.
                // Padded bands have already faded to half at the shared line, where the face continues;
                // without them the face carries the whole interpolation from the raised artwork to the
                // lowered one.
                TextureRegion neighborArt = water ? textures.region(neighbor.coords())
                      : bases.region(neighbor.coords());
                MeshPartBuilder fadeWall = fadePart(blendBuilder, fadeParts, neighborArt.getTexture());
                wall(fadeWall, midlineA, midlineB, lowA, lowB,
                      BoardGeometry.center(neighbor.coords(), neighbor.elevation()), neighborArt,
                      groundArt ? 0.5f : 0f, 1);
            }
            if (!BoardGeometry.HAS_PADDING || BoardGeometry.GAP <= 0 || water) {
                // The band carries no fade: without padding there is no band, without a gap there is nothing
                // to cover, and both halves of a water join draw the same water surface.
                continue;
            }
            TextureRegion neighborBase = bases.region(neighbor.coords());
            Vector3 neighborCenter = BoardGeometry.center(neighbor.coords(), neighbor.elevation());
            MeshPartBuilder fade = fadePart(blendBuilder, fadeParts, neighborBase.getTexture());
            int facing = BoardGeometry.facingEdge(BoardGeometry.edgeDirection(edge));
            Vector3 neighborEdgeA = BoardGeometry.corner(neighbor.coords(), neighbor.elevation(), facing);
            Vector3 neighborEdgeB = BoardGeometry.corner(neighbor.coords(), neighbor.elevation(), facing + 1);
            // The neighbor's artwork, mirrored over its own facing edge, fades in toward the midline.
            blendBand(fade, scene, coords, elevation, edge, neighborCenter, neighborBase, neighborEdgeA,
                  neighborEdgeB, depth);
        }
        for (int corner = 0; corner < 6; corner++) {
            BoardScene.Tile first = scene.tile(coords.translated(BoardGeometry.edgeDirection(corner - 1)));
            BoardScene.Tile second = scene.tile(coords.translated(BoardGeometry.edgeDirection(corner)));
            boolean water = tile.water() && first != null && first.water()
                  && second != null && second.water();
            if (BoardGeometry.cornerBevel(scene, coords, elevation, corner) > 0) {
                if (BoardGeometry.HAS_PADDING) {
                    if (BoardGeometry.GAP > 0 && !water) {
                        pad = pad == null ? padPart(padBuilder, padParts, baseRegion.getTexture()) : pad;
                        cap(pad, scene, coords, elevation, corner, center, baseRegion,
                              Math.max(bandDepth(tile, first), bandDepth(tile, second)));
                    } else {
                        cap(mesh, scene, coords, elevation, corner, center, region,
                              Math.max(bandDepth(tile, first), bandDepth(tile, second)));
                    }
                }
                continue;
            }
            float firstLevel = BoardGeometry.padLevel(scene, coords, elevation, corner - 1);
            float secondLevel = BoardGeometry.padLevel(scene, coords, elevation, corner);
            if (firstLevel == secondLevel) {
                continue;
            }
            // The two band halves of this corner reach different levels: close it with a small face.
            boolean groundArt = BoardGeometry.GAP > 0 && !water;
            MeshPartBuilder face = groundArt ? pad : mesh;
            TextureRegion faceRegion = groundArt ? baseRegion : region;
            Vector3 apex = BoardGeometry.insetCorner(coords, elevation, corner);
            Vector3 firstPoint = BoardGeometry.cellCorner(coords, elevation, corner);
            firstPoint.z = firstLevel * BoardGeometry.LEVEL;
            Vector3 secondPoint = BoardGeometry.cellCorner(coords, elevation, corner);
            secondPoint.z = secondLevel * BoardGeometry.LEVEL;
            Vector3 normal = new Vector3(firstPoint.y - apex.y, apex.x - firstPoint.x, 0).nor();
            face.triangle(wallVertex(apex, center, normal, faceRegion, 1),
                  wallVertex(firstPoint, center, normal, faceRegion, 1),
                  wallVertex(secondPoint, center, normal, faceRegion, 1));
        }
        if (tile.features() != null) {
            // The feature layer floats over hex and padding alike, so trees, rubble and buildings can reach
            // past the hex edge instead of being cut at it.
            TextureRegion featureRegion = featureArtwork.region(coords);
            featureQuad(featurePart(builders.featureBuilder(), builders.featureParts(), featureRegion.getTexture()),
                  coords, elevation, featureRegion);
        }
    }

    /** Draws one hex's feature artwork over the whole hex box, transparent wherever the hex has no feature. */
    private void featureQuad(MeshPartBuilder mesh, Coords coords, int elevation, TextureRegion region) {
        Vector3 center = BoardGeometry.center(coords, elevation);
        float left = center.x - BoardGeometry.WIDTH / 2;
        float right = center.x + BoardGeometry.WIDTH / 2;
        float bottom = center.y - BoardGeometry.HEIGHT / 2;
        float top = center.y + BoardGeometry.HEIGHT / 2;
        float z = center.z + FEATURE_LIFT;
        mesh.rect(featureVertex(left, top, z, region, 0, 0), featureVertex(right, top, z, region, 1, 0),
              featureVertex(right, bottom, z, region, 1, 1), featureVertex(left, bottom, z, region, 0, 1));
    }

    /** One corner of a feature quad, sampled across the whole captured artwork. */
    private MeshPartBuilder.VertexInfo featureVertex(float x, float y, float z, TextureRegion region, float u,
          float v) {
        return vertexInfo(new Vector3(x, y, z), Vector3.Z, u, v, region, 1);
    }

    /**
     * The feature part for one artwork texture, created on first use: transparent pixels blend over hex and
     * padding, so it never writes depth and later features stay visible through each other.
     */
    private static MeshPartBuilder featurePart(ModelBuilder featureBuilder, Map<Texture, MeshPartBuilder> featureParts,
          Texture texture) {
        return featureParts.computeIfAbsent(texture, key ->
              featureBuilder.part("features" + featureParts.size(), GL20.GL_TRIANGLES, ATTRIBUTES,
                    new Material(TextureAttribute.createDiffuse(key),
                          new BlendingAttribute(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA),
                          new DepthTestAttribute(GL20.GL_LEQUAL, false),
                          IntAttribute.createCullFace(GL20.GL_NONE))));
    }

    /**
     * The premultiplied fade part for one padding texture, created on first use. The color contribution is
     * alpha * artwork while the opaque ground underneath keeps the framebuffer alpha intact.
     */
    private static MeshPartBuilder fadePart(ModelBuilder blendBuilder, Map<Texture, MeshPartBuilder> fadeParts,
          Texture texture) {
        return fadeParts.computeIfAbsent(texture, key ->
              blendBuilder.part("fade" + fadeParts.size(), GL20.GL_TRIANGLES, ATTRIBUTES,
                    new Material(TextureAttribute.createDiffuse(key),
                          new BlendingAttribute(GL20.GL_ONE, GL20.GL_ONE_MINUS_SRC_ALPHA),
                          new DepthTestAttribute(GL20.GL_LEQUAL, false),
                          IntAttribute.createCullFace(GL20.GL_NONE))));
    }

    /** The ground artwork part for one padding texture, created on first use. */
    private static MeshPartBuilder padPart(ModelBuilder modelBuilder, Map<Texture, MeshPartBuilder> padParts,
          Texture texture) {
        return padParts.computeIfAbsent(texture, key ->
              modelBuilder.part("padding" + padParts.size(), GL20.GL_TRIANGLES, ATTRIBUTES,
                    new Material(TextureAttribute.createDiffuse(key),
                          IntAttribute.createCullFace(GL20.GL_NONE))));
    }

    /**
     * Slanted patch closing a capped corner: the quad between this tile's two band halves plus the tip that
     * reaches the lattice corner, so the corner folds instead of showing a vertical face.
     */
    private void cap(MeshPartBuilder mesh, BoardScene scene, Coords coords, int elevation, int corner,
          Vector3 center, TextureRegion region, float depth) {
        // Each side mirrors over the edge of the band it closes and the tip over the corner line, so the
        // artwork wraps around the corner instead of being stretched across it.
        Vector3 cornerPoint = BoardGeometry.corner(coords, elevation, corner);
        Vector3 firstEnd = BoardGeometry.corner(coords, elevation, corner - 1);
        Vector3 secondEnd = BoardGeometry.corner(coords, elevation, corner + 1);
        Vector3 bisector = BoardGeometry.cellCorner(coords, elevation, corner);
        Vector3 firstTip = BoardGeometry.padPoint(new Vector3(), scene, coords, elevation, corner - 1, 1, 1);
        Vector3 secondTip = BoardGeometry.padPoint(new Vector3(), scene, coords, elevation, corner, 0, 1);
        Vector3 tip = new Vector3(bisector.x, bisector.y,
              BoardGeometry.cornerHeight(scene, coords, elevation, corner) * BoardGeometry.LEVEL);
        mesh.triangle(capVertex(firstTip, scene, coords, elevation, corner - 1, 1, center, region, firstEnd,
                    cornerPoint, depth),
              padVertex(tip, padSource(tip, cornerPoint, bisector, center, 1, depth), center, Vector3.Z, region, 1),
              capVertex(secondTip, scene, coords, elevation, corner, 1, center, region, cornerPoint, secondEnd,
                    depth));
        Vector3 leftInner = BoardGeometry.padPoint(new Vector3(), scene, coords, elevation, corner - 1, 1, 0);
        Vector3 leftOuter = BoardGeometry.padPoint(new Vector3(), scene, coords, elevation, corner - 1, 1, 1);
        Vector3 rightInner = BoardGeometry.padPoint(new Vector3(), scene, coords, elevation, corner, 0, 0);
        Vector3 rightOuter = BoardGeometry.padPoint(new Vector3(), scene, coords, elevation, corner, 0, 1);
        mesh.rect(capVertex(leftInner, scene, coords, elevation, corner - 1, 0, center, region, firstEnd,
                    cornerPoint, depth),
              capVertex(rightInner, scene, coords, elevation, corner, 0, center, region, cornerPoint, secondEnd,
                    depth),
              capVertex(rightOuter, scene, coords, elevation, corner, 1, center, region, cornerPoint, secondEnd,
                    depth),
              capVertex(leftOuter, scene, coords, elevation, corner - 1, 1, center, region, firstEnd,
                    cornerPoint, depth));
    }

    /** Cap vertex: the band's own row normal keeps the cap shading continuous with the band it closes. */
    private MeshPartBuilder.VertexInfo capVertex(Vector3 point, BoardScene scene, Coords coords, int elevation,
          int edge, float t, Vector3 center, TextureRegion region, Vector3 edgeA, Vector3 edgeB,
          float depth) {
        Vector3 normal = BoardGeometry.padNormal(new Vector3(), scene, coords, elevation, edge, 0.5f, t);
        return padVertex(point, padSource(point, edgeA, edgeB, center, t, depth), center, normal, region, 1);
    }

    /** This tile's half of a padding band: its artwork continues mirrored over the edge it leaves. */
    private void band(MeshPartBuilder mesh, BoardScene scene, Coords coords, int elevation, int edge,
          Vector3 center, TextureRegion region, float depth) {
        Vector3 edgeA = BoardGeometry.corner(coords, elevation, edge);
        Vector3 edgeB = BoardGeometry.corner(coords, elevation, edge + 1);
        bandQuad(mesh, scene, coords, elevation, edge, center, region, 1, 1, edgeA, edgeB, depth);
    }

    /**
     * The neighbor's artwork fading in over this half: mirrored over the neighbor's own facing edge so it
     * lines up with the half the neighbor draws for itself. The fade follows the surface, so the artwork
     * crosses over exactly as the band changes height.
     */
    private void blendBand(MeshPartBuilder mesh, BoardScene scene, Coords coords, int elevation, int edge,
          Vector3 center, TextureRegion region, Vector3 edgeA, Vector3 edgeB, float depth) {
        bandQuad(mesh, scene, coords, elevation, edge, center, region, 0, 0.5f, edgeA, edgeB, depth);
    }

    /**
     * This tile's half of a padding band as one quad, from the hex edge to the midline. The artwork is
     * sampled at the point mirrored over {@code edgeA}-{@code edgeB} so it continues outward as a mirror
     * image, and the normals at the two rows let the quad shade as one straight slope, with no rows to break
     * it into bands.
     */
    private void bandQuad(MeshPartBuilder mesh, BoardScene scene, Coords coords, int elevation, int edge,
          Vector3 center, TextureRegion region, float innerAlpha, float outerAlpha, Vector3 edgeA, Vector3 edgeB,
          float depth) {
        Vector3 innerA = BoardGeometry.padPoint(new Vector3(), scene, coords, elevation, edge, 0, 0);
        Vector3 innerB = BoardGeometry.padPoint(new Vector3(), scene, coords, elevation, edge, 1, 0);
        Vector3 outerA = BoardGeometry.padPoint(new Vector3(), scene, coords, elevation, edge, 0, 1);
        Vector3 outerB = BoardGeometry.padPoint(new Vector3(), scene, coords, elevation, edge, 1, 1);
        Vector3 innerNormal = BoardGeometry.padNormal(new Vector3(), scene, coords, elevation, edge, 0.5f, 0);
        Vector3 outerNormal = BoardGeometry.padNormal(new Vector3(), scene, coords, elevation, edge, 0.5f, 1);
        mesh.rect(padVertex(innerA, padSource(innerA, edgeA, edgeB, center, 0, depth), center, innerNormal,
                    region, innerAlpha),
              padVertex(innerB, padSource(innerB, edgeA, edgeB, center, 0, depth), center, innerNormal,
                    region, innerAlpha),
              padVertex(outerB, padSource(outerB, edgeA, edgeB, center, 1, depth), center, outerNormal,
                    region, outerAlpha),
              padVertex(outerA, padSource(outerA, edgeA, edgeB, center, 1, depth), center, outerNormal,
                    region, outerAlpha));
    }

    /**
     * Artwork radius a band samples at the midline, or a negative value to mirror the artwork over the edge
     * instead. A level step or a water side draws a bank or face just inside the hex edge; it gets wider with
     * the step, so the sampled surface moves further in as well.
     */
    private static float bandDepth(BoardScene.Tile tile, BoardScene.Tile neighbor) {
        if (neighbor == null) {
            return -1;
        }
        int delta = Math.abs(neighbor.elevation() - tile.elevation());
        if (delta == 0 && neighbor.water() == tile.water()) {
            return -1;
        }
        return MathUtils.clamp(EDGE_SAMPLE - SAMPLE_DEPTH_PER_LEVEL * delta, DEEPEST_SAMPLE, EDGE_SAMPLE);
    }

    /**
     * Where a padding sample reads the artwork. Neighbors that continue the same ground mirror the artwork over
     * the shared edge, so the terrain runs across the gap. An edge that carries a bank or face — a level step,
     * or water meeting land — draws that artwork just inside the hex edge, and mirroring it would repeat the
     * bank outward, so those bands follow the surface behind it from `EDGE_SAMPLE` at the hex edge to `depth`
     * at the midline instead.
     */
    private static Vector3 padSource(Vector3 point, Vector3 edgeA, Vector3 edgeB, Vector3 center, float t,
          float depth) {
        if (BoardGeometry.GAP <= 0) {
            // The band lies inside the hex edge, so it draws the tile's own artwork right there.
            return point;
        }
        if (depth <= 0) {
            return mirrored(point, edgeA, edgeB);
        }
        float x = point.x - center.x;
        float y = point.y - center.y;
        float hexagon = hexagonDistance(x, y);
        if (hexagon <= 0) {
            return point;
        }
        float scale = MathUtils.lerp(EDGE_SAMPLE, depth, t) / hexagon;
        return new Vector3(center.x + x * scale, center.y + y * scale, point.z);
    }

    /** Reflection of a padding point across an edge, so the artwork continues over it as a mirror image. */
    private static Vector3 mirrored(Vector3 point, Vector3 edgeA, Vector3 edgeB) {
        float dx = edgeB.x - edgeA.x;
        float dy = edgeB.y - edgeA.y;
        float length = (float) Math.hypot(dx, dy);
        float offset = ((point.x - edgeA.x) * -dy + (point.y - edgeA.y) * dx) / length;
        return new Vector3(point.x + 2 * offset * dy / length, point.y - 2 * offset * dx / length, point.z);
    }

    /** Vertical face at the cell edge; artwork is sampled from the tile interior like legacy cliff art. */
    private void wall(MeshPartBuilder mesh, Vector3 topA, Vector3 topB, Vector3 bottomA, Vector3 bottomB,
          Vector3 center, TextureRegion region, float topAlpha, float bottomAlpha) {
        Vector3 normal = new Vector3(topB.y - topA.y, topA.x - topB.x, 0).nor();
        mesh.rect(wallVertex(topA, center, normal, region, topAlpha),
              wallVertex(bottomA, center, normal, region, bottomAlpha),
              wallVertex(bottomB, center, normal, region, bottomAlpha),
              wallVertex(topB, center, normal, region, topAlpha));
    }

    /**
     * The flat hex surface samples the captured artwork across the hexagon itself. It stays off the artwork's
     * outer texels for the same reason the padding does: the tile art ends at its hexagon edge, so a surface
     * edge that reaches it — which is where the surface stops without an interpolation inset — draws the
     * filter's mix of the tile and the transparent pixels beyond it, and the surface has no alpha blending to
     * soften that into a dark rim around every hex. The hex frame draws that edge instead.
     */
    private MeshPartBuilder.VertexInfo surfaceVertex(Vector3 point, Vector3 center, TextureRegion region,
          float alpha) {
        return padVertex(point, point, center, Vector3.Z, region, alpha);
    }

    /**
     * One tile's hex frame: the band between its hexagon edge and that edge pulled in by
     * {@code HEX_FRAME_WIDTH}, drawn in the tile's own artwork like the tileset's hex grid and darkened by
     * {@link BoardGeometry#HEX_FRAME_SHADE}. It lies on the tile's surface, inside the edge the padding
     * continues, so it stays under the feature artwork that reaches over the hex edge.
     */
    private void hexFrame(MeshPartBuilder mesh, Coords coords, int elevation, Vector3 center,
          TextureRegion region) {
        for (int corner = 0; corner < 6; corner++) {
            Vector3 outerA = BoardGeometry.corner(coords, elevation, corner);
            Vector3 outerB = BoardGeometry.corner(coords, elevation, corner + 1);
            Vector3 innerA = BoardGeometry.insetCorner(new Vector3(), coords, elevation, corner,
                  HEX_FRAME_WIDTH);
            Vector3 innerB = BoardGeometry.insetCorner(new Vector3(), coords, elevation, corner + 1,
                  HEX_FRAME_WIDTH);
            outerA.z += HEX_FRAME_LIFT;
            outerB.z += HEX_FRAME_LIFT;
            innerA.z += HEX_FRAME_LIFT;
            innerB.z += HEX_FRAME_LIFT;
            mesh.rect(frameVertex(innerA, center, region), frameVertex(innerB, center, region),
                  frameVertex(outerB, center, region), frameVertex(outerA, center, region));
        }
    }

    /** Frame corner: the surface's own sample, darkened, so the frame repeats the artwork it outlines. */
    private MeshPartBuilder.VertexInfo frameVertex(Vector3 point, Vector3 center, TextureRegion region) {
        float shade = BoardGeometry.HEX_FRAME_SHADE;
        return padVertex(point, point, center, Vector3.Z, region, 1)
              .setCol(shade, shade, shade, 1);
    }

    /**
     * Padding vertex: the artwork is mapped over the hexagon itself, so a padded point samples the texel
     * just inside the hex edge it continues instead of a smear of stretched edge pixels. Mirroring can push
     * a sample outside the hexagon, where the artwork is transparent, so it is pulled back onto the hexagon.
     * A point that leaves its tile's level walks that height toward the hexagon's centre in the artwork, at
     * the artwork's own scale, so a side that stands almost vertical shows its texels at the surface's own
     * scale instead of stretching a thin sliver of the artwork. Flat points move nothing and sample as before.
     */
    private MeshPartBuilder.VertexInfo padVertex(Vector3 point, Vector3 source, Vector3 center, Vector3 normal,
          TextureRegion region, float alpha) {
        float x = source.x - center.x;
        float y = source.y - center.y;
        float hexagon = hexagonDistance(x, y);
        if (hexagon > PAINTED_LIMIT) {
            x *= PAINTED_LIMIT / hexagon;
            y *= PAINTED_LIMIT / hexagon;
        }
        float row = 0.5f - y / BoardGeometry.HEIGHT;
        float height = Math.abs(center.z - point.z) / BoardGeometry.HEIGHT;
        float walk = row >= 0.5f ? row - height : row + height;
        // Fold the walk into the artwork's rows and back, so a side that walks past an edge of the artwork
        // repeats it there instead of freezing on one stretched row.
        float loop = walk - (float) Math.floor(walk / 2f) * 2f;
        float v = loop > 1f ? 2f - loop : loop;
        // The walked sample is kept on the painted hexagon, which narrows toward the artwork's top and
        // bottom: a row of the artwork is that wide however far the walk has moved it.
        float halfWidth = (PAINTED_LIMIT - Math.abs(0.5f - v)) / 2;
        float u = MathUtils.clamp(0.5f + x / BoardGeometry.WIDTH, 0.5f - halfWidth, 0.5f + halfWidth);
        return vertexInfo(point, normal, u, v, region, alpha);
    }

    /** Distance from the hex centre: 1 on the artwork's hexagon, below 1 inside the painted area. */
    private static float hexagonDistance(float x, float y) {
        return Math.max(2 * Math.abs(y) / BoardGeometry.HEIGHT,
              2 * Math.abs(x) / BoardGeometry.WIDTH + Math.abs(y) / BoardGeometry.HEIGHT);
    }

    /**
     * Face vertex: legacy inset sampling keeps walls inside the artwork's opaque area. The sample starts at
     * the artwork row of the wall's own edge and walks toward the hexagon's centre by the height the face
     * covers, at the artwork's own scale, folding back at the artwork's edge: a tall face keeps showing
     * texels instead of freezing on one stretched row.
     */
    private MeshPartBuilder.VertexInfo wallVertex(Vector3 point, Vector3 center, Vector3 normal,
          TextureRegion region, float alpha) {
        float u = 0.15f + 0.7f * (0.5f + (point.x - center.x) / BoardGeometry.WIDTH);
        float row = 0.5f - (point.y - center.y) / BoardGeometry.HEIGHT;
        float height = Math.abs(center.z - point.z) / BoardGeometry.HEIGHT;
        float walk = row >= 0.5f ? row - height : row + height;
        float loop = walk - (float) Math.floor(walk / 2f) * 2f;
        float v = 0.5f + 0.7f * ((loop > 1f ? 2f - loop : loop) - 0.5f);
        return vertexInfo(point, normal, u, v, region, alpha);
    }

    /** Shared vertex layout: padding alpha premultiplies the color so blending keeps the target opaque. */
    private MeshPartBuilder.VertexInfo vertexInfo(Vector3 point, Vector3 normal, float u, float v,
          TextureRegion region, float alpha) {
        return new MeshPartBuilder.VertexInfo().setPos(point)
              .setNor(normal).setCol(alpha, alpha, alpha, alpha)
              .setUV(region.getU() + u * (region.getU2() - region.getU()),
                    region.getV() + v * (region.getV2() - region.getV()));
    }

    /** Tactical layer pixels cover the whole lattice cell, so padding bands stay highlighted with their hex. */
    private MeshPartBuilder.VertexInfo cellVertex(Vector3 point, Vector3 center, TextureRegion region, float alpha) {
        float u = (point.x - center.x) / BoardGeometry.CELL_WIDTH + 0.5f;
        float v = 0.5f - (point.y - center.y) / BoardGeometry.CELL_HEIGHT;
        return vertexInfo(point, Vector3.Z, u, v, region, alpha);
    }

    void render(Camera camera, boolean tactical) {
        batch.begin(camera);
        for (Chunk chunk : chunks) {
            if (chunk.tactical() == tactical && camera.frustum.boundsInFrustum(chunk.bounds())) {
                batch.render(chunk.instance(), tactical ? null : environment);
                if (chunk.padInstance() != null) {
                    batch.render(chunk.padInstance(), environment);
                }
            }
        }
        batch.end();
        if (tactical) {
            return;
        }
        // Padding blends are translucent, so they follow every opaque surface in their own pass, and the
        // feature layers follow those again so a tree or a building reaches over the padded gap.
        blendBatch.begin(camera);
        for (Chunk chunk : chunks) {
            if (camera.frustum.boundsInFrustum(chunk.bounds())) {
                blendBatch.render(chunk.blendInstance(), environment);
            }
        }
        blendBatch.end();
        blendBatch.begin(camera);
        for (Chunk chunk : chunks) {
            if (camera.frustum.boundsInFrustum(chunk.bounds())) {
                blendBatch.render(chunk.featureInstance(), environment);
            }
        }
        blendBatch.end();
    }

    private void clearMeshes() {
        for (Chunk chunk : chunks) {
            chunk.model().dispose();
            if (chunk.padModel() != null) {
                chunk.padModel().dispose();
            }
            chunk.blendModel().dispose();
            chunk.featureModel().dispose();
        }
        chunks.clear();
    }

    @Override
    public void dispose() {
        clearMeshes();
        textures.dispose();
        bases.dispose();
        featureArtwork.dispose();
        annotations.dispose();
        batch.dispose();
        blendBatch.dispose();
        depthBatch.dispose();
        if (shadow != null) {
            shadow.dispose();
        }
    }
}

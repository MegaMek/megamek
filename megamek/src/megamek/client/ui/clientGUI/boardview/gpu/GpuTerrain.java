/* Copyright (C) 2026 The MegaMek Team. SPDX-License-Identifier: GPL-3.0-or-later */
package megamek.client.ui.clientGUI.boardview.gpu;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Consumer;

import com.badlogic.gdx.graphics.Camera;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.Mesh;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.VertexAttributes;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.graphics.g3d.Environment;
import com.badlogic.gdx.graphics.g3d.Material;
import com.badlogic.gdx.graphics.g3d.Model;
import com.badlogic.gdx.graphics.g3d.ModelBatch;
import com.badlogic.gdx.graphics.g3d.ModelCache;
import com.badlogic.gdx.graphics.g3d.ModelInstance;
import com.badlogic.gdx.graphics.g3d.Renderable;
import com.badlogic.gdx.graphics.g3d.RenderableProvider;
import com.badlogic.gdx.graphics.g3d.attributes.BlendingAttribute;
import com.badlogic.gdx.graphics.g3d.attributes.ColorAttribute;
import com.badlogic.gdx.graphics.g3d.attributes.DepthTestAttribute;
import com.badlogic.gdx.graphics.g3d.attributes.IntAttribute;
import com.badlogic.gdx.graphics.g3d.attributes.TextureAttribute;
import com.badlogic.gdx.graphics.g3d.environment.DirectionalShadowLight;
import com.badlogic.gdx.graphics.g3d.utils.DepthShaderProvider;
import com.badlogic.gdx.graphics.g3d.utils.MeshPartBuilder;
import com.badlogic.gdx.graphics.g3d.utils.ModelBuilder;
import com.badlogic.gdx.math.Intersector;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Matrix4;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.math.collision.BoundingBox;
import com.badlogic.gdx.math.collision.Ray;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.Disposable;
import megamek.common.board.Coords;

/** Chunked solid hex columns, flat decals and authored features; owns all GL resources it creates. */
final class GpuTerrain implements Disposable {
    static final int CHUNK_SIZE = 16;
    static final int SHADOW_RESOLUTION = 2048;
    private static final long ATTRIBUTES = VertexAttributes.Usage.Position | VertexAttributes.Usage.Normal
          | VertexAttributes.Usage.TextureCoordinates | VertexAttributes.Usage.ColorPacked;
    private final GpuAssets assets = new GpuAssets();
    private final GpuTextures<Coords> ground = new GpuTextures<>(true);
    private final GpuTextures<Coords> decals = new GpuTextures<>();
    private final GpuTextures<Coords> tactical = new GpuTextures<>();
    private final ModelBatch batch = new ModelBatch();
    private final ModelBatch depthBatch = new ModelBatch(new DepthShaderProvider());
    private final Environment environment = new Environment();
    private final List<Chunk> chunks = new ArrayList<>();
    private final List<Model> shadowModels = new ArrayList<>();
    private final List<Matrix4> shadowTransforms = new ArrayList<>();
    private final Map<Model, BoundingBox> unitBounds = new HashMap<>();
    private final Map<Model, List<Vector3>> featureTriangles = new HashMap<>();
    private final BoundingBox shadowBounds = new BoundingBox();
    private final Matrix4 shadowView = new Matrix4();
    private List<BoardScene.Tile> tiles;
    private BoardScene.Light light;
    private BoardAtmosphere.Lighting atmosphere;
    private DirectionalShadowLight shadow;
    private boolean shadowDirty;
    private int tuning = -1;
    private float clock;
    private float floor;
    private int chunkRows;

    private record Prop(Coords coords, ModelInstance instance, BoundingBox bounds) { }
    private record WaterSurface(Material material, int depth, boolean falling) { }

    private static final class Chunk implements Disposable {
        final List<ModelInstance> opaque = new ArrayList<>();
        final List<ModelInstance> overlays = new ArrayList<>();
        final List<ModelInstance> water = new ArrayList<>();
        final List<WaterSurface> waterMaterials = new ArrayList<>();
        final List<ModelInstance> tactical = new ArrayList<>();
        final List<Prop> props = new ArrayList<>();
        final Array<Renderable> propRenderables = new Array<>();
        final RenderableProvider solidProps = (out, pool) -> {
            for (Renderable renderable : propRenderables) {
                renderable.shader = null;
                renderable.environment = null;
            }
            out.addAll(propRenderables);
        };
        final BoundingBox bounds = new BoundingBox().inf();
        Set<Coords> faded = Set.of();

        void cacheProps() {
            disposePropMeshes();
            // ModelCache's default pool reserves 65,536 vertices per chunk. Its mesh builder also retains
            // scratch arrays after end(). Use tight meshes, and transfer those static meshes to this chunk
            // so the temporary builder and sorting/pooling buffers can be collected after each build.
            ModelCache.TightMeshPool meshes = new ModelCache.TightMeshPool();
            ModelCache builder = new ModelCache(new ModelCache.Sorter(), meshes);
            try {
                builder.begin();
                for (Prop prop : props) {
                    if (!faded.contains(prop.coords())) {
                        builder.add(prop.instance());
                    }
                }
                builder.end();
                builder.getRenderables(propRenderables, null);
            } catch (RuntimeException | Error failure) {
                meshes.dispose();
                propRenderables.clear();
                throw failure;
            }
        }

        private void disposePropMeshes() {
            Set<Mesh> meshes = new HashSet<>();
            for (Renderable renderable : propRenderables) {
                meshes.add(renderable.meshPart.mesh);
            }
            meshes.forEach(Mesh::dispose);
            propRenderables.clear();
        }

        @Override
        public void dispose() {
            disposePropMeshes();
            for (List<ModelInstance> layer : List.of(opaque, overlays, water, tactical)) {
                layer.forEach(instance -> instance.model.dispose());
            }
        }
    }

    /** Collect by material before opening a mesh part: ModelBuilder has only one active part at a time. */
    private static final class Layer {
        final Map<Material, List<Consumer<MeshPartBuilder>>> geometry = new LinkedHashMap<>();

        void add(Material material, Consumer<MeshPartBuilder> shape) {
            geometry.computeIfAbsent(material, key -> new ArrayList<>()).add(shape);
        }

        void finish(List<ModelInstance> destination) {
            if (geometry.isEmpty()) {
                return;
            }
            ModelBuilder builder = new ModelBuilder();
            builder.begin();
            int index = 0;
            for (var entry : geometry.entrySet()) {
                MeshPartBuilder mesh = builder.part("surface-" + index++, GL20.GL_TRIANGLES, ATTRIBUTES, entry.getKey());
                entry.getValue().forEach(shape -> shape.accept(mesh));
            }
            destination.add(new ModelInstance(builder.end()));
        }
    }

    void update(BoardScene scene) {
        boolean changedTuning = tuning != BoardGeometry.revision();
        boolean changedLight = !Objects.equals(light, scene.light());
        if (tiles == scene.tiles() && !changedTuning && !changedLight) {
            return;
        }
        Map<Coords, BoardScene.Pixels> terrainPixels = new HashMap<>();
        Map<Coords, BoardScene.Pixels> decalPixels = new HashMap<>();
        Map<Coords, BoardScene.Pixels> tacticalPixels = new HashMap<>();
        float nextFloor = BoardGeometry.floor(scene);
        boolean rebuildAll = changedTuning || tiles == null || tiles.size() != scene.tiles().size() || nextFloor != floor;
        Set<Coords> changedChunks = new HashSet<>();
        for (int index = 0; index < scene.tiles().size(); index++) {
            BoardScene.Tile tile = scene.tiles().get(index);
            terrainPixels.put(tile.coords(), tile.ground());
            if (tile.decals() != null) {
                decalPixels.put(tile.coords(), tile.decals());
            }
            if (tile.tactical() != null) {
                tacticalPixels.put(tile.coords(), tile.tactical());
            }
            if (!rebuildAll) {
                BoardScene.Tile before = tiles.get(index);
                // Rim vertex colors come from the selected ground artwork, not the atlas texture.
                if (!before.ground().equals(tile.ground())) {
                    dirtyChunk(changedChunks, tile.coords());
                }
                if (!before.coords().equals(tile.coords())) {
                    rebuildAll = true;
                } else if (before.elevation() != tile.elevation()
                      || before.waterDepth() != tile.waterDepth() || before.frozen() != tile.frozen()
                      || before.roadExits() != tile.roadExits()
                      || before.surface() != tile.surface()
                      || !before.features().equals(tile.features())) {
                    dirtyChunk(changedChunks, tile.coords());
                    for (int direction = 0; direction < 6; direction++) {
                        Coords neighbor = tile.coords().translated(direction);
                        if (scene.tile(neighbor) != null) {
                            dirtyChunk(changedChunks, neighbor);
                        }
                    }
                }
            }
        }
        rebuildAll |= ground.update(terrainPixels);
        rebuildAll |= decals.update(decalPixels);
        boolean markingsChanged = tactical.update(tacticalPixels);
        tiles = scene.tiles();
        tuning = BoardGeometry.revision();
        floor = nextFloor;
        if (rebuildAll) {
            chunks.forEach(Chunk::dispose);
            chunks.clear();
        }
        int chunkIndex = 0;
        chunkRows = (scene.height() + CHUNK_SIZE - 1) / CHUNK_SIZE;
        for (int x = 0; x < scene.width(); x += CHUNK_SIZE) {
            for (int y = 0; y < scene.height(); y += CHUNK_SIZE) {
                if (rebuildAll) {
                    chunks.add(build(scene, x, y, floor));
                } else if (changedChunks.contains(new Coords(x / CHUNK_SIZE, y / CHUNK_SIZE))) {
                    chunks.get(chunkIndex).dispose();
                    chunks.set(chunkIndex, build(scene, x, y, floor));
                } else if (markingsChanged) {
                    buildMarkings(scene, chunks.get(chunkIndex), x, y);
                }
                chunkIndex++;
            }
        }
        if (rebuildAll || !changedChunks.isEmpty() || changedLight) {
            updateLight(scene.light());
        }
    }

    private static void dirtyChunk(Set<Coords> chunks, Coords coords) {
        chunks.add(new Coords(coords.getX() / CHUNK_SIZE, coords.getY() / CHUNK_SIZE));
    }

    private Chunk build(BoardScene scene, int startX, int startY, float floor) {
        Chunk chunk = new Chunk();
        Layer solid = new Layer();
        Layer overlay = new Layer();
        Layer liquid = new Layer();
        for (int x = startX; x < Math.min(scene.width(), startX + CHUNK_SIZE); x++) {
            for (int y = startY; y < Math.min(scene.height(), startY + CHUNK_SIZE); y++) {
                BoardScene.Tile tile = scene.tile(new Coords(x, y));
                BoardSurface surface = new BoardSurface(scene, tile);
                TextureRegion top = ground.region(tile.coords());
                for (BoardSurface.Face face : surface.faces) {
                    boolean artwork = face.finish() == BoardSurface.Finish.TOP || face.finish() == BoardSurface.Finish.ICE
                          || face.finish() == BoardSurface.Finish.SHORE;
                    BoardScene.Tile land = face.landEdge() < 0 ? null
                          : scene.tile(tile.coords().translated(BoardGeometry.edgeDirection(face.landEdge())));
                    if (land != null && !land.water()) {
                        TextureRegion bankArt = ground.region(land.coords());
                        solid.add(material(bankArt.getTexture(), false),
                              mesh -> bank(mesh, tile.coords(), face, land.coords(), bankArt));
                    } else {
                        Texture texture = artwork ? top.getTexture()
                              : assets.material(face.finish() == BoardSurface.Finish.BED ? "bed" : tile.water() ? "terrain/sand" : tile.surface().wall);
                        solid.add(material(texture, false), mesh -> surface(mesh, tile.coords(), face, artwork ? top : null, 0));
                    }
                    chunk.bounds.ext(face.a()).ext(face.b()).ext(face.c());
                    if (face.finish() == BoardSurface.Finish.SHORE) {
                        overlay.add(material(assets.material("terrain/sand"), true), mesh -> shore(mesh, tile, face));
                    }
                    if (tile.decals() != null && artwork) {
                        TextureRegion art = decals.region(tile.coords());
                        overlay.add(material(art.getTexture(), true), mesh -> surface(mesh, tile.coords(), face, art, 0.08f));
                    }
                }
                if (!tile.water() && tile.roadExits() == 0 && surface.ramps == 0 && BoardGeometry.tuning().gridShade() < 1) {
                    solid.add(material(top.getTexture(), false),
                          mesh -> grid(mesh, tile.coords(), BoardGeometry.groundZ(tile), top));
                }
                for (BoardSurface.Side side : surface.sides(scene, floor)) {
                    Texture wall = assets.material(tile.surface().wall);
                    solid.add(material(wall, false), mesh -> wall(mesh, side));
                    chunk.bounds.ext(side.a().x, side.a().y, side.lowA()).ext(side.b().x, side.b().y, side.lowB());
                    if (!tile.water()) {
                        overlay.add(material(assets.material(tile.surface().rim), true), mesh -> cornice(mesh, tile, side));
                        Material incline = material(assets.incline(tile.surface()), true);
                        incline.set(new BlendingAttribute(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA, 0.62f));
                        overlay.add(incline, mesh -> incline(mesh, surface, side));
                    }
                }
                if (!surface.water.isEmpty()) {
                    Material water = material(assets.water(tile.waterDepth(), 0), true);
                    water.set(new BlendingAttribute(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA, 0.48f));
                    water.id = "water-" + tile.waterDepth();
                    TextureRegion waterArt = new TextureRegion(assets.water(tile.waterDepth(), 0));
                    for (BoardSurface.Face face : surface.waterFaces) {
                        liquid.add(water, mesh -> surface(mesh, tile.coords(), face, waterArt, 0));
                        chunk.bounds.ext(face.a()).ext(face.b()).ext(face.c());
                    }
                    if (!surface.waterfalls.isEmpty()) {
                        Material fall = material(assets.water(tile.waterDepth(), 0), true);
                        fall.id = "falls-" + tile.waterDepth();
                        fall.set(new BlendingAttribute(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA, 0.8f));
                        for (BoardSurface.Side drop : surface.waterfalls) {
                            liquid.add(fall, mesh -> waterfall(mesh, drop));
                            chunk.bounds.ext(drop.a().x, drop.a().y, drop.lowA());
                            chunk.bounds.ext(drop.b().x, drop.b().y, drop.lowB());
                        }
                    }
                }
                for (BoardScene.Feature feature : tile.features()) {
                    ModelInstance instance = new ModelInstance(assets.model(feature.asset()));
                    for (Material material : instance.materials) {
                        if (material.id.equals("wall")) {
                            material.get(TextureAttribute.class, TextureAttribute.Diffuse).scaleV = feature.height();
                        }
                    }
                    float px = BoardGeometry.centerX(tile.coords()) + feature.x() * BoardGeometry.HEX_SCALE;
                    float py = BoardGeometry.centerY(tile.coords()) + feature.y() * BoardGeometry.HEX_SCALE;
                    // Zero-elevation decks otherwise coincide with the dry riverbank and road decals.
                    float base = feature.asset().equals("bridge")
                          ? tile.elevation() * BoardGeometry.LEVEL + 0.16f * BoardGeometry.HEX_SCALE
                          : surface.height(px, py);
                    instance.transform.setToTranslation(px, py,
                          base + feature.elevation() * BoardGeometry.LEVEL)
                          .rotate(Vector3.Z, feature.rotation())
                          .scale(feature.scale() * BoardGeometry.HEX_SCALE, feature.scale() * BoardGeometry.HEX_SCALE,
                                feature.height() * BoardGeometry.LEVEL);
                    BoundingBox bounds = instance.calculateBoundingBox(new BoundingBox()).mul(instance.transform);
                    chunk.props.add(new Prop(tile.coords(), instance, bounds));
                    chunk.bounds.ext(bounds);
                }
            }
        }
        solid.finish(chunk.opaque);
        overlay.finish(chunk.overlays);
        liquid.finish(chunk.water);
        // The floating markings remain visible when only their raised edge enters the viewport.
        chunk.bounds.max.z += BoardGeometry.LEVEL / 3;
        buildMarkings(scene, chunk, startX, startY);
        // ModelInstance copies materials; animate those owned by the rendered instances.
        for (ModelInstance instance : chunk.water) {
            for (Material material : instance.materials) {
                chunk.waterMaterials.add(new WaterSurface(material, Integer.parseInt(material.id.substring(6)),
                      material.id.startsWith("falls-")));
            }
        }
        chunk.cacheProps();
        return chunk;
    }

    private void buildMarkings(BoardScene scene, Chunk chunk, int startX, int startY) {
        chunk.tactical.forEach(instance -> instance.model.dispose());
        chunk.tactical.clear();
        Layer marks = new Layer();
        for (int x = startX; x < Math.min(scene.width(), startX + CHUNK_SIZE); x++) {
            for (int y = startY; y < Math.min(scene.height(), startY + CHUNK_SIZE); y++) {
                BoardScene.Tile tile = scene.tile(new Coords(x, y));
                if (tile.tactical() == null) {
                    continue;
                }
                TextureRegion art = tactical.region(tile.coords());
                Material mark = material(art.getTexture(), true);
                // The shared blended material tests opaque depth without hiding later annotations.
                // Reuse the terrain triangles so road approaches cannot bury a flat marking plane.
                BoardSurface surface = new BoardSurface(scene, tile);
                for (BoardSurface.Face face : surface.faces) {
                    marks.add(mark, mesh -> mesh.triangle(markingVertex(face.a(), tile, art),
                          markingVertex(face.b(), tile, art), markingVertex(face.c(), tile, art)));
                }
            }
        }
        marks.finish(chunk.tactical);
    }

    private static Material material(Texture texture, boolean blend) {
        Material material = new Material("surface", TextureAttribute.createDiffuse(texture),
              IntAttribute.createCullFace(GL20.GL_NONE));
        if (blend) {
            material.set(new BlendingAttribute(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA));
            material.set(new DepthTestAttribute(GL20.GL_LEQUAL, false));
        }
        return material;
    }

    private static MeshPartBuilder.VertexInfo vertex(Vector3 p, Vector3 normal, float u, float v, Color color) {
        return new MeshPartBuilder.VertexInfo().setPos(p).setNor(normal).setUV(u, v).setCol(color);
    }

    private static MeshPartBuilder.VertexInfo topVertex(Vector3 p, Coords coords, TextureRegion region, Color color) {
        // Sample just inside the artwork's alpha border, without moving the actual geometry.
        float u = 0.5f + (p.x - BoardGeometry.centerX(coords)) / BoardGeometry.WIDTH * 0.96f;
        float v = 0.5f - (p.y - BoardGeometry.centerY(coords)) / BoardGeometry.HEIGHT * 0.96f;
        return vertex(p, Vector3.Z, region.getU() + u * (region.getU2() - region.getU()),
              region.getV() + v * (region.getV2() - region.getV()), color);
    }

    private static MeshPartBuilder.VertexInfo markingVertex(Vector3 point, BoardScene.Tile tile, TextureRegion region) {
        Coords coords = tile.coords();
        float u = 0.5f + (point.x - BoardGeometry.centerX(coords)) / BoardGeometry.WIDTH;
        float v = 0.5f - (point.y - BoardGeometry.centerY(coords)) / BoardGeometry.HEIGHT;
        // Keep river markings above the water, following the banks and dry road ramps elsewhere.
        float base = tile.water() ? Math.max(point.z, BoardGeometry.surfaceZ(tile)) : point.z;
        Vector3 raised = new Vector3(point.x, point.y, base + BoardGeometry.LEVEL / 3);
        return vertex(raised, Vector3.Z, region.getU() + u * (region.getU2() - region.getU()),
              region.getV() + v * (region.getV2() - region.getV()), Color.WHITE);
    }

    private static void surface(MeshPartBuilder mesh, Coords coords, BoardSurface.Face face,
          TextureRegion art, float lift) {
        Vector3 normal = new Vector3(face.b()).sub(face.a()).crs(new Vector3(face.c()).sub(face.a())).nor();
        mesh.triangle(surfaceVertex(face.a(), coords, normal, art, lift),
              surfaceVertex(face.b(), coords, normal, art, lift), surfaceVertex(face.c(), coords, normal, art, lift));
    }

    private static MeshPartBuilder.VertexInfo surfaceVertex(Vector3 point, Coords coords, Vector3 normal,
          TextureRegion art, float lift) {
        Vector3 raised = new Vector3(point).add(0, 0, lift);
        if (art != null) {
            return topVertex(raised, coords, art, Color.WHITE).setNor(normal);
        }
        float repeat = 96 * BoardGeometry.HEX_SCALE;
        float u = Math.abs(normal.z) > 0.7f ? point.x / repeat
              : (Math.abs(normal.y) > Math.abs(normal.x) ? point.x : point.y) / repeat;
        float v = Math.abs(normal.z) > 0.7f ? point.y / repeat : -point.z / repeat;
        return vertex(raised, normal, u, v, Color.WHITE);
    }

    private static void wall(MeshPartBuilder mesh, BoardSurface.Side side) {
        Vector3 a = side.a(), b = side.b();
        Vector3 normal = new Vector3(b.x - a.x, b.y - a.y, 0).crs(Vector3.Z).nor();
        Vector3 lowerA = new Vector3(a.x, a.y, side.lowA());
        Vector3 lowerB = new Vector3(b.x, b.y, side.lowB());
        float repeat = 96 * BoardGeometry.HEX_SCALE;
        float length = (float) Math.hypot(a.x - b.x, a.y - b.y);
        float u = (a.x * (b.x - a.x) + a.y * (b.y - a.y)) / length / repeat;
        float endU = u + length / repeat;
        mesh.rect(vertex(a, normal, u, -a.z / repeat, Color.WHITE),
              vertex(lowerA, normal, u, -side.lowA() / repeat, Color.WHITE),
              vertex(lowerB, normal, endU, -side.lowB() / repeat, Color.WHITE),
              vertex(b, normal, endU, -b.z / repeat, Color.WHITE));
    }

    /** Cover geometry clips a fixed-scale material; changing its depth never stretches the texture. */
    static void cornice(MeshPartBuilder mesh, BoardScene.Tile tile, BoardSurface.Side side) {
        Vector3 direction = new Vector3(side.b()).sub(side.a());
        direction.z = 0;
        float length = direction.len();
        direction.scl(1 / length);
        Vector3 normal = new Vector3(direction).crs(Vector3.Z);
        float repeat = 96 * BoardGeometry.HEX_SCALE;
        float u = side.a().dot(direction) / repeat;
        float fade = 1.5f * BoardGeometry.HEX_SCALE;
        int segments = Math.max(1, (int) Math.ceil(length / (6 * BoardGeometry.HEX_SCALE)));
        for (int segment = 0; segment < segments; segment++) {
            float from = segment / (float) segments, to = (segment + 1f) / segments;
            Vector3 a = new Vector3(side.a()).lerp(side.b(), from);
            Vector3 b = new Vector3(side.a()).lerp(side.b(), to);
            float lowA = side.lowA() + (side.lowB() - side.lowA()) * from;
            float lowB = side.lowA() + (side.lowB() - side.lowA()) * to;
            float depthA = corniceDepth(tile.surface(), a), depthB = corniceDepth(tile.surface(), b);
            lowA = Math.max(lowA, a.z - depthA);
            lowB = Math.max(lowB, b.z - depthB);
            Color colorA = corniceColor(tile, a), colorB = corniceColor(tile, b);
            float startU = u + length * from / repeat, endU = u + length * to / repeat;
            for (int band = 0; band < 2; band++) {
                float startA = band == 0 ? 0 : Math.min(a.z - lowA, depthA - fade);
                float startB = band == 0 ? 0 : Math.min(b.z - lowB, depthB - fade);
                float endA = Math.min(a.z - lowA, band == 0 ? depthA - fade : depthA);
                float endB = Math.min(b.z - lowB, band == 0 ? depthB - fade : depthB);
                if (endA > startA || endB > startB) {
                    mesh.rect(corniceVertex(a, startA, depthA, normal, startU, colorA),
                          corniceVertex(a, endA, depthA, normal, startU, colorA),
                          corniceVertex(b, endB, depthB, normal, endU, colorB),
                          corniceVertex(b, startB, depthB, normal, endU, colorB));
                }
            }
        }
    }

    private static float corniceDepth(BoardScene.Surface surface, Vector3 point) {
        if (surface == BoardScene.Surface.CONCRETE) {
            return 9 * BoardGeometry.HEX_SCALE;
        }
        float x = point.x / BoardGeometry.HEX_SCALE, y = point.y / BoardGeometry.HEX_SCALE;
        return (9 + 2 * (float) Math.sin(x * 0.09f + y * 0.05f)
              + 3 * (float) Math.sin(y * 0.65f - x * 0.43f)) * BoardGeometry.HEX_SCALE;
    }

    /** Orient one material's 08 edge patch on each exposed segment, leaving road mouths open. */
    private static void incline(MeshPartBuilder mesh, BoardSurface surface, BoardSurface.Side side) {
        Vector3 a = BoardGeometry.corner(surface.tile.coords(), surface.tile.elevation(), side.edge());
        Vector3 b = BoardGeometry.corner(surface.tile.coords(), surface.tile.elevation(), side.edge() + 1);
        Vector3 along = new Vector3(b).sub(a).nor();
        Vector3 inward = new Vector3(Vector3.Z).crs(along);
        float length = a.dst(b);
        for (BoardSurface.Face face : surface.faces) {
            if (face.finish() != BoardSurface.Finish.TOP) {
                continue;
            }
            List<Vector3> polygon = List.of(face.a(), face.b(), face.c());
            // Keep the original artwork's footprint, rotated onto the actual top surface.
            polygon = clip(polygon, inward, a.dot(inward) + 26 * BoardGeometry.HEX_SCALE, false);
            if (!side.a().epsilonEquals(a, 0.02f)) {
                polygon = clip(polygon, along, side.a().dot(along), true);
            }
            if (!side.b().epsilonEquals(b, 0.02f)) {
                polygon = clip(polygon, along, side.b().dot(along), false);
            }
            Vector3 normal = new Vector3(face.b()).sub(face.a()).crs(new Vector3(face.c()).sub(face.a())).nor();
            for (int index = 1; index + 1 < polygon.size(); index++) {
                mesh.triangle(inclineVertex(polygon.getFirst(), a, along, inward, normal, length),
                      inclineVertex(polygon.get(index), a, along, inward, normal, length),
                      inclineVertex(polygon.get(index + 1), a, along, inward, normal, length));
            }
        }
    }

    private static MeshPartBuilder.VertexInfo corniceVertex(Vector3 edge, float depth, float fullDepth,
          Vector3 normal, float u, Color color) {
        Vector3 point = new Vector3(edge).add(0, 0, -depth).mulAdd(normal, 0.06f * BoardGeometry.HEX_SCALE);
        float alpha = Math.clamp((fullDepth - depth) / (1.5f * BoardGeometry.HEX_SCALE), 0, 1);
        return vertex(point, normal, u, -point.z / (96 * BoardGeometry.HEX_SCALE), color)
              .setCol(color.r, color.g, color.b, alpha);
    }

    /** Pale neutral rim detail takes its palette from opaque pixels just inside the selected hex's edge. */
    private static Color corniceColor(BoardScene.Tile tile, Vector3 edge) {
        BoardScene.Pixels pixels = tile.ground();
        float u = 0.5f + (edge.x - BoardGeometry.centerX(tile.coords())) / BoardGeometry.WIDTH * 0.9f;
        float v = 0.5f - (edge.y - BoardGeometry.centerY(tile.coords())) / BoardGeometry.HEIGHT * 0.9f;
        int centerX = Math.round(u * (pixels.width() - 1)), centerY = Math.round(v * (pixels.height() - 1));
        float red = 0, green = 0, blue = 0, weight = 0;
        for (int y = centerY - 1; y <= centerY + 1; y++) {
            for (int x = centerX - 1; x <= centerX + 1; x++) {
                int rgba = pixels.rgba(Math.clamp(y, 0, pixels.height() - 1) * pixels.width()
                      + Math.clamp(x, 0, pixels.width() - 1));
                float alpha = (rgba & 255) / 255f;
                red += (rgba >>> 24) * alpha;
                green += ((rgba >>> 16) & 255) * alpha;
                blue += ((rgba >>> 8) & 255) * alpha;
                weight += 255 * alpha;
            }
        }
        return weight == 0 ? new Color(Color.WHITE) : new Color(red / weight, green / weight, blue / weight, 1);
    }

    private static void waterfall(MeshPartBuilder mesh, BoardSurface.Side drop) {
        Vector3 normal = new Vector3(drop.b()).sub(drop.a()).crs(Vector3.Z).nor();
        Vector3 a = new Vector3(drop.a()).mulAdd(normal, 0.06f * BoardGeometry.HEX_SCALE);
        Vector3 b = new Vector3(drop.b()).mulAdd(normal, 0.06f * BoardGeometry.HEX_SCALE);
        float repeat = 48 * BoardGeometry.HEX_SCALE;
        float u = a.dst(b) / (24 * BoardGeometry.HEX_SCALE);
        // Increasing V samples upward in world space. A positive animated offset makes the artwork fall downward.
        mesh.rect(vertex(a, normal, 0, a.z / repeat, Color.WHITE),
              vertex(new Vector3(a.x, a.y, drop.lowA()), normal, 0, drop.lowA() / repeat, Color.WHITE),
              vertex(new Vector3(b.x, b.y, drop.lowB()), normal, u, drop.lowB() / repeat, Color.WHITE),
              vertex(b, normal, u, b.z / repeat, Color.WHITE));
    }

    private static List<Vector3> clip(List<Vector3> polygon, Vector3 normal, float distance, boolean positive) {
        if (polygon.isEmpty()) {
            return polygon;
        }
        List<Vector3> result = new ArrayList<>();
        Vector3 previous = polygon.getLast();
        float before = (previous.dot(normal) - distance) * (positive ? 1 : -1);
        for (Vector3 point : polygon) {
            float after = (point.dot(normal) - distance) * (positive ? 1 : -1);
            if ((before >= 0) != (after >= 0)) {
                result.add(new Vector3(previous).lerp(point, before / (before - after)));
            }
            if (after >= 0) {
                result.add(point);
            }
            previous = point;
            before = after;
        }
        return result;
    }

    private static MeshPartBuilder.VertexInfo inclineVertex(Vector3 point, Vector3 a, Vector3 along,
          Vector3 inward, Vector3 normal, float length) {
        Vector3 offset = new Vector3(point).sub(a);
        float u = 0.25f + offset.dot(along) / (2 * length);
        float v = 1 - offset.dot(inward) / BoardGeometry.HEIGHT;
        return vertex(new Vector3(point).add(0, 0, 0.1f * BoardGeometry.HEX_SCALE), normal, u, v, Color.WHITE);
    }

    /** Continue the neighboring land's selected artwork across the edge, beneath the existing sand fade. */
    private static void bank(MeshPartBuilder mesh, Coords water, BoardSurface.Face face, Coords land, TextureRegion art) {
        Vector3 a = BoardGeometry.corner(water, 0, face.landEdge());
        Vector3 outward = BoardGeometry.corner(water, 0, face.landEdge() + 1).sub(a).crs(Vector3.Z).nor();
        Vector3 normal = new Vector3(face.b()).sub(face.a()).crs(new Vector3(face.c()).sub(face.a())).nor();
        mesh.triangle(bankVertex(face.a(), land, art, normal, a, outward),
              bankVertex(face.b(), land, art, normal, a, outward), bankVertex(face.c(), land, art, normal, a, outward));
    }

    private static MeshPartBuilder.VertexInfo bankVertex(Vector3 point, Coords land, TextureRegion art,
          Vector3 normal, Vector3 edge, Vector3 outward) {
        // Mirror only the texture sample into the neighbor: geometry and the waterline stay unchanged.
        Vector3 sample = new Vector3(point).mulAdd(outward, -2 * new Vector3(point).sub(edge).dot(outward));
        return topVertex(sample, land, art, Color.WHITE).setPos(point).setNor(normal);
    }

    private static void shore(MeshPartBuilder mesh, BoardScene.Tile tile, BoardSurface.Face face) {
        Vector3 normal = new Vector3(face.b()).sub(face.a()).crs(new Vector3(face.c()).sub(face.a())).nor();
        mesh.triangle(shoreVertex(tile, face.a(), normal), shoreVertex(tile, face.b(), normal),
              shoreVertex(tile, face.c(), normal));
    }

    private static MeshPartBuilder.VertexInfo shoreVertex(BoardScene.Tile tile, Vector3 point, Vector3 normal) {
        float wet = Math.max(0, Math.min(1, (tile.elevation() * BoardGeometry.LEVEL - point.z) / BoardGeometry.HEX_SCALE));
        // Sand appears gradually across the bank and darkens slightly at the waterline.
        float shade = 1 - wet * 0.12f;
        return surfaceVertex(point, tile.coords(), normal, null, 0.035f * BoardGeometry.HEX_SCALE)
              .setCol(shade, shade, shade, wet);
    }

    private static void grid(MeshPartBuilder mesh, Coords coords, float z, TextureRegion region) {
        Vector3 center = BoardGeometry.center(coords, 0);
        center.z = z + 0.03f;
        float shade = BoardGeometry.tuning().gridShade();
        Color color = new Color(shade, shade, shade, 1);
        for (int edge = 0; edge < 6; edge++) {
            Vector3 a = BoardGeometry.corner(coords, 0, edge);
            Vector3 b = BoardGeometry.corner(coords, 0, edge + 1);
            a.z = b.z = center.z;
            Vector3 innerA = new Vector3(a).lerp(center, 0.012f);
            Vector3 innerB = new Vector3(b).lerp(center, 0.012f);
            mesh.rect(topVertex(a, coords, region, color), topVertex(b, coords, region, color),
                  topVertex(innerB, coords, region, color), topVertex(innerA, coords, region, color));
        }
    }

    private BoundingBox unitBounds(ModelInstance unit) {
        return new BoundingBox(unitBounds.computeIfAbsent(unit.model,
              model -> model.calculateBoundingBox(new BoundingBox()))).mul(unit.transform);
    }

    /** Unit instances already contain the shared animation's current position; hidden units never enter this list. */
    void animate(float delta, List<ModelInstance> units) {
        clock += delta;
        unitBounds.keySet().retainAll(units.stream().map(unit -> unit.model).toList());
        List<BoundingBox> occupied = units.stream().map(this::unitBounds).toList();
        for (Chunk chunk : chunks) {
            Set<Coords> faded = new HashSet<>();
            for (BoundingBox unit : occupied) {
                if (!chunk.bounds.intersects(unit)) {
                    continue;
                }
                for (Prop prop : chunk.props) {
                    if (prop.bounds().intersects(unit)) {
                        faded.add(prop.coords());
                    }
                }
            }
            if (!faded.equals(chunk.faded)) {
                chunk.faded = Set.copyOf(faded);
                for (Prop prop : chunk.props) {
                    for (Material material : prop.instance().materials) {
                        if (faded.contains(prop.coords())) {
                            material.set(new BlendingAttribute(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA, 0.24f));
                            material.set(new DepthTestAttribute(GL20.GL_LEQUAL, false));
                            material.set(IntAttribute.createCullFace(GL20.GL_BACK));
                        } else {
                            material.remove(BlendingAttribute.Type);
                            material.remove(DepthTestAttribute.Type);
                            material.remove(IntAttribute.CullFace);
                        }
                    }
                }
                chunk.cacheProps();
                shadowDirty = true;
            }
            for (WaterSurface water : chunk.waterMaterials) {
                TextureAttribute texture = water.material().get(TextureAttribute.class, TextureAttribute.Diffuse);
                texture.textureDescription.texture = assets.water(water.depth(), clock);
                if (water.falling()) {
                    texture.offsetV = clock % 1;
                }
            }
        }
    }

    Environment environment() {
        return environment;
    }

    BoundingBox roofBounds(Coords coords) {
        BoundingBox result = null;
        int index = (coords.getX() / CHUNK_SIZE) * chunkRows + coords.getY() / CHUNK_SIZE;
        if (index < 0 || index >= chunks.size()) {
            return null;
        }
        for (Prop prop : chunks.get(index).props) {
            if (prop.coords().equals(coords)
                  && (result == null || prop.bounds().max.z > result.max.z)) {
                result = prop.bounds();
            }
        }
        return result;
    }

    /** Roofs, courtyard openings and walls use the authored mesh, not a bounding-box proxy. */
    Coords pick(BoardScene scene, Ray ray) {
        Coords result = null;
        float nearest = Float.POSITIVE_INFINITY;
        List<BoardScene.Tile> candidates = new ArrayList<>();
        Vector3 hit = new Vector3();
        for (int index = 0; index < chunks.size(); index++) {
            Chunk chunk = chunks.get(index);
            if (!Intersector.intersectRayBoundsFast(ray, chunk.bounds)) {
                continue;
            }
            // Reuse render bounds before invoking the shared surface picker; a pointer event must not
            // inspect six neighbors and allocate geometry bounds for every hex of a 40,000-hex board.
            int startX = (index / chunkRows) * CHUNK_SIZE;
            int startY = (index % chunkRows) * CHUNK_SIZE;
            for (int x = startX; x < Math.min(startX + CHUNK_SIZE, scene.width()); x++) {
                for (int y = startY; y < Math.min(startY + CHUNK_SIZE, scene.height()); y++) {
                    candidates.add(scene.tile(new Coords(x, y)));
                }
            }
            for (Prop prop : chunk.props) {
                if (!Intersector.intersectRayBoundsFast(ray, prop.bounds())) {
                    continue;
                }
                Ray local = new Ray(ray.origin, ray.direction).mul(new Matrix4(prop.instance().transform).inv());
                List<Vector3> triangles = featureTriangles.computeIfAbsent(prop.instance().model, GpuTerrain::triangles);
                for (int i = 0; i < triangles.size(); i += 3) {
                    if (Intersector.intersectRayTriangle(local, triangles.get(i), triangles.get(i + 1), triangles.get(i + 2), hit)) {
                        float distance = ray.origin.dst2(hit.mul(prop.instance().transform));
                        if (distance < nearest) {
                            nearest = distance;
                            result = prop.coords();
                        }
                    }
                }
            }
        }
        BoardGeometry.Hit groundHit = BoardGeometry.hit(scene, ray, candidates, floor);
        return groundHit != null && groundHit.distance() <= nearest ? groundHit.coords() : result;
    }

    private static List<Vector3> triangles(Model model) {
        List<Vector3> result = new ArrayList<>();
        // Board assets are exported in model coordinates with identity nodes.
        for (var mesh : model.meshes) {
            int stride = mesh.getVertexSize() / Float.BYTES;
            int offset = mesh.getVertexAttribute(VertexAttributes.Usage.Position).offset / Float.BYTES;
            float[] vertices = new float[mesh.getNumVertices() * stride];
            short[] indices = new short[mesh.getNumIndices()];
            mesh.getVertices(vertices);
            mesh.getIndices(indices);
            for (short index : indices) {
                int at = Short.toUnsignedInt(index) * stride + offset;
                result.add(new Vector3(vertices[at], vertices[at + 1], vertices[at + 2]));
            }
        }
        return result;
    }

    /** Opaque world first. Tactical overlays are a separate final pass. */
    void render(Camera camera, boolean drawTactical) {
        batch.begin(camera);
        for (Chunk chunk : chunks) {
            if (!camera.frustum.boundsInFrustum(chunk.bounds)) {
                continue;
            }
            if (drawTactical) {
                chunk.tactical.forEach(instance -> batch.render(instance));
            } else {
                chunk.opaque.forEach(instance -> batch.render(instance, environment));
                batch.render(chunk.solidProps, environment);
            }
        }
        batch.end();
        if (!drawTactical) {
            batch.begin(camera);
            for (Chunk chunk : chunks) {
                if (camera.frustum.boundsInFrustum(chunk.bounds)) {
                    chunk.overlays.forEach(instance -> batch.render(instance, environment));
                }
            }
            batch.end();
        }
    }

    /** Water and faded features follow units, with depth testing but no depth writes. */
    void renderTransparent(Camera camera) {
        batch.begin(camera);
        for (Chunk chunk : chunks) {
            if (camera.frustum.boundsInFrustum(chunk.bounds)) {
                chunk.water.forEach(instance -> batch.render(instance, environment));
                for (Prop prop : chunk.props) {
                    if (chunk.faded.contains(prop.coords())) {
                        batch.render(prop.instance(), environment);
                    }
                }
            }
        }
        batch.end();
    }

    private void updateLight(BoardScene.Light next) {
        light = next;
        shadowDirty = true;
        shadowBounds.inf();
        chunks.forEach(chunk -> shadowBounds.ext(chunk.bounds));
        applyLight();
    }

    void setAtmosphere(BoardAtmosphere.Lighting next) {
        if (!Objects.equals(atmosphere, next)) {
            atmosphere = next;
            shadowDirty = true;
            applyLight();
        }
    }

    private void applyLight() {
        if (light == null && atmosphere == null) {
            if (shadow != null) {
                environment.remove(shadow);
                shadow.dispose();
                shadow = null;
            }
            environment.shadowMap = null;
            environment.set(ColorAttribute.createAmbientLight(0.85f, 0.85f, 0.85f, 1));
            return;
        }
        if (shadow == null) {
            shadow = new DirectionalShadowLight(SHADOW_RESOLUTION, SHADOW_RESOLUTION, 1, 1, 1, 2);
            environment.add(shadow);
            environment.shadowMap = shadow;
        }
        if (atmosphere == null) {
            shadow.set(0.55f, 0.53f, 0.48f, light.x() * BoardGeometry.HEX_SCALE,
                  light.y() * BoardGeometry.HEX_SCALE, -BoardGeometry.LEVEL);
            environment.set(ColorAttribute.createAmbientLight(0.55f, 0.58f, 0.62f, 1));
        } else {
            shadow.set(atmosphere.direct(), atmosphere.direction());
            environment.set(ColorAttribute.createAmbientLight(atmosphere.ambient()));
        }
    }

    /** The same opaque geometry feeds both shadow maps and the atmosphere's camera-depth buffer. */
    void renderDepth(Camera camera, List<ModelInstance> units, ModelBatch pass) {
        pass.begin(camera);
        for (Chunk chunk : chunks) {
            if (camera.frustum.boundsInFrustum(chunk.bounds)) {
                chunk.opaque.forEach(pass::render);
                pass.render(chunk.solidProps);
            }
        }
        units.forEach(pass::render);
        pass.end();
    }

    void renderShadows(List<ModelInstance> units) {
        renderShadows(null, units);
    }

    void renderShadows(OrthographicCamera view, List<ModelInstance> units) {
        if (shadow == null) {
            return;
        }
        boolean changed = shadowDirty || units.size() != shadowModels.size()
              || (view != null && !Arrays.equals(view.combined.val, shadowView.val));
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
            bounds.ext(unitBounds(unit));
        }
        fitShadowCamera(view, shadow.getCamera(), bounds, shadow.direction);
        if (view != null) {
            shadowView.set(view.combined);
        }
        shadow.begin();
        renderDepth(shadow.getCamera(), units, depthBatch);
        shadow.end();
        float slope = (float) Math.hypot(shadow.direction.x, shadow.direction.y) / Math.abs(shadow.direction.z);
        Camera lightCamera = shadow.getCamera();
        float texel = Math.max(lightCamera.viewportWidth, lightCamera.viewportHeight) / SHADOW_RESOLUTION;
        shadow.getProjViewTrans().val[Matrix4.M23] -= 3 * Math.max(1, slope) * texel
              / (lightCamera.far - lightCamera.near);
        shadowDirty = false;
    }

    /** Focus texels on visible receivers, retaining the full light depth for offscreen shadow casters. */
    static void fitShadowCamera(OrthographicCamera view, Camera target, BoundingBox bounds, Vector3 direction) {
        BoundingBox receivers = new BoundingBox(bounds);
        if (view != null) {
            receivers.inf();
            Vector3 right = new Vector3(view.direction).crs(view.up).nor();
            for (int x : new int[] { -1, 1 }) {
                for (int y : new int[] { -1, 1 }) {
                    Vector3 origin = new Vector3(view.position)
                          .mulAdd(right, x * view.viewportWidth * view.zoom / 2)
                          .mulAdd(view.up, y * view.viewportHeight * view.zoom / 2);
                    for (float z : new float[] { bounds.min.z, bounds.max.z }) {
                        receivers.ext(new Vector3(origin).mulAdd(view.direction, (z - origin.z) / view.direction.z));
                    }
                }
            }
            receivers.min.x = MathUtils.clamp(receivers.min.x, bounds.min.x, bounds.max.x);
            receivers.min.y = MathUtils.clamp(receivers.min.y, bounds.min.y, bounds.max.y);
            receivers.max.x = MathUtils.clamp(receivers.max.x, bounds.min.x, bounds.max.x);
            receivers.max.y = MathUtils.clamp(receivers.max.y, bounds.min.y, bounds.max.y);
            receivers.min.z = bounds.min.z;
            receivers.max.z = bounds.max.z;
            receivers.update();
        }
        target.direction.set(direction).nor();
        Vector3 right = new Vector3(target.direction)
              .crs(Math.abs(target.direction.z) > 0.99f ? Vector3.Y : Vector3.Z).nor();
        target.up.set(right).crs(target.direction).nor();
        BoundingBox lightSpace = new BoundingBox().inf();
        float near = Float.POSITIVE_INFINITY;
        float far = Float.NEGATIVE_INFINITY;
        for (int corner = 0; corner < 8; corner++) {
            Vector3 point = new Vector3((corner & 1) == 0 ? receivers.min.x : receivers.max.x,
                  (corner & 2) == 0 ? receivers.min.y : receivers.max.y,
                  (corner & 4) == 0 ? receivers.min.z : receivers.max.z);
            lightSpace.ext(point.dot(right), point.dot(target.up), 0);
            point.set((corner & 1) == 0 ? bounds.min.x : bounds.max.x,
                  (corner & 2) == 0 ? bounds.min.y : bounds.max.y,
                  (corner & 4) == 0 ? bounds.min.z : bounds.max.z);
            float depth = point.dot(target.direction);
            near = Math.min(near, depth);
            far = Math.max(far, depth);
        }
        // Leave a filtering border, and align the light grid to texels to avoid crawling edges while panning.
        float border = 1 + 4f / SHADOW_RESOLUTION;
        target.viewportWidth = Math.max(1, lightSpace.getWidth()) * border;
        target.viewportHeight = Math.max(1, lightSpace.getHeight()) * border;
        float texelX = target.viewportWidth / SHADOW_RESOLUTION;
        float texelY = target.viewportHeight / SHADOW_RESOLUTION;
        Vector3 center = lightSpace.getCenter(new Vector3());
        center.x = Math.round(center.x / texelX) * texelX;
        center.y = Math.round(center.y / texelY) * texelY;
        target.position.set(right).scl(center.x).mulAdd(target.up, center.y).mulAdd(target.direction, near - 1);
        target.near = 1;
        target.far = far - near + 2;
        target.update();
    }

    @Override
    public void dispose() {
        chunks.forEach(Chunk::dispose);
        chunks.clear();
        ground.dispose();
        decals.dispose();
        tactical.dispose();
        batch.dispose();
        depthBatch.dispose();
        if (shadow != null) {
            shadow.dispose();
        }
        assets.dispose();
        featureTriangles.clear();
    }
}

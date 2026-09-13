/* Copyright (C) 2026 The MegaMek Team. SPDX-License-Identifier: GPL-3.0-or-later */
package megamek.client.ui.clientGUI.boardview.gpu;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import com.badlogic.gdx.graphics.Camera;
import com.badlogic.gdx.graphics.Color;
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
    private static final long ATTRIBUTES = VertexAttributes.Usage.Position | VertexAttributes.Usage.ColorPacked
          | VertexAttributes.Usage.TextureCoordinates | VertexAttributes.Usage.Normal;
    private final GpuTextures<Coords> textures = new GpuTextures<>();
    private final GpuTextures<Coords> annotations = new GpuTextures<>();
    private final ModelBatch batch = new ModelBatch();
    private final ModelBatch depthBatch = new ModelBatch(new DepthShaderProvider());
    private final Environment environment = new Environment();
    private final List<Chunk> chunks = new ArrayList<>();
    private List<BoardScene.Tile> tiles;
    private DirectionalShadowLight shadow;
    private BoardScene.Light light;

    private record Chunk(Model model, ModelInstance instance, BoundingBox bounds, boolean tactical) { }

    void update(BoardScene scene) {
        boolean lightChanged = !Objects.equals(light, scene.light());
        if (tiles == scene.tiles() && !lightChanged) {
            return;
        }
        Map<Coords, BoardScene.Pixels> ground = new HashMap<>();
        Map<Coords, BoardScene.Pixels> tactical = new HashMap<>();
        for (BoardScene.Tile tile : scene.tiles()) {
            ground.put(tile.coords(), tile.image());
            if (tile.tactical() != null) {
                tactical.put(tile.coords(), tile.tactical());
            }
        }
        boolean layoutChanged = textures.update(ground);
        layoutChanged |= annotations.update(tactical);
        boolean sameGeometry = tiles != null && tiles.size() == scene.tiles().size();
        if (sameGeometry) {
            for (int i = 0; i < tiles.size(); i++) {
                BoardScene.Tile before = tiles.get(i);
                BoardScene.Tile after = scene.tiles().get(i);
                if (!before.coords().equals(after.coords()) || before.elevation() != after.elevation()) {
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
        environment.shadowMap = null;
        environment.set(ColorAttribute.createAmbientLight(1, 1, 1, 1));
        if (light == null) {
            return;
        }
        BoundingBox bounds = new BoundingBox();
        chunks.stream().filter(chunk -> !chunk.tactical()).forEach(chunk -> bounds.ext(chunk.bounds()));
        float diameter = bounds.getDimensions(new Vector3()).len() * 1.05f;
        int mapSize = diameter > 2048 ? 4096 : 2048;
        shadow = new DirectionalShadowLight(mapSize, mapSize, diameter, diameter, 1, diameter + 2);
        shadow.set(0.5f, 0.5f, 0.5f, light.x(), light.y(), -BoardGeometry.LEVEL);
        shadow.getCamera().up.set(Vector3.Z);
        environment.add(shadow);
        environment.set(ColorAttribute.createAmbientLight(0.65f, 0.65f, 0.65f, 1));
        environment.shadowMap = shadow;
        shadow.begin(bounds.getCenter(new Vector3()), Vector3.Zero);
        depthBatch.begin(shadow.getCamera());
        for (Chunk chunk : chunks) {
            if (!chunk.tactical()) {
                depthBatch.render(chunk.instance());
            }
        }
        depthBatch.end();
        shadow.end();
        // Bias receiver depth by the PCF footprint on a horizontal hex. The stock shader has no shadow bias;
        // without it, adjacent depth samples incorrectly shadow the same flat surface.
        float slope = (float) Math.hypot(light.x(), light.y()) / BoardGeometry.LEVEL;
        shadow.getProjViewTrans().val[Matrix4.M23] -= 3 * Math.max(1, slope) / mapSize;
    }

    private void buildChunk(BoardScene scene, int startX, int startY, float floor, boolean tactical) {
        ModelBuilder modelBuilder = new ModelBuilder();
        modelBuilder.begin();
        Map<Texture, MeshPartBuilder> parts = new HashMap<>();
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
                Vector3 center = BoardGeometry.center(tile.coords(), tile.elevation());
                for (int edge = 0; edge < 6; edge++) {
                    Vector3 a = BoardGeometry.corner(tile.coords(), tile.elevation(), edge);
                    Vector3 b = BoardGeometry.corner(tile.coords(), tile.elevation(), edge + 1);
                    mesh.triangle(vertex(center, center, region, Vector3.Z), vertex(a, center, region, Vector3.Z),
                          vertex(b, center, region, Vector3.Z));
                    if (tactical) {
                        continue;
                    }
                    // Side UVs sample the same tile artwork; surface normals provide directional lighting.
                    float bottom = BoardGeometry.wallBottom(scene, tile, edge, floor);
                    if (bottom >= center.z) {
                        continue;
                    }
                    Vector3 lowA = new Vector3(a.x, a.y, bottom);
                    Vector3 lowB = new Vector3(b.x, b.y, bottom);
                    Vector3 normal = new Vector3(b.y - a.y, a.x - b.x, 0).nor();
                    mesh.rect(vertex(a, center, region, normal), vertex(lowA, center, region, normal),
                          vertex(lowB, center, region, normal), vertex(b, center, region, normal));
                }
            }
        }
        Model model = modelBuilder.end();
        ModelInstance instance = new ModelInstance(model);
        chunks.add(new Chunk(model, instance, instance.calculateBoundingBox(new BoundingBox()), tactical));
    }

    private MeshPartBuilder.VertexInfo vertex(Vector3 point, Vector3 center, TextureRegion region, Vector3 normal) {
        float u = (point.x - center.x) / BoardGeometry.WIDTH + 0.5f;
        float v = 0.5f - (point.y - center.y) / BoardGeometry.HEIGHT;
        if (normal.z == 0) {
            // Sample inside the tile, not its transparent border, when texturing a cliff.
            u = 0.15f + u * 0.7f;
            v = MathUtils.clamp(0.15f + v * 0.7f + (center.z - point.z) / BoardGeometry.HEIGHT, 0.15f, 0.85f);
        }
        return new MeshPartBuilder.VertexInfo().setPos(point)
              .setNor(normal).setCol(Color.WHITE)
              .setUV(region.getU() + u * (region.getU2() - region.getU()),
                    region.getV() + v * (region.getV2() - region.getV()));
    }

    void render(Camera camera, boolean tactical) {
        batch.begin(camera);
        for (Chunk chunk : chunks) {
            if (chunk.tactical() == tactical && camera.frustum.boundsInFrustum(chunk.bounds())) {
                batch.render(chunk.instance(), tactical ? null : environment);
            }
        }
        batch.end();
    }

    private void clearMeshes() {
        for (Chunk chunk : chunks) {
            chunk.model().dispose();
        }
        chunks.clear();
    }

    @Override
    public void dispose() {
        clearMeshes();
        textures.dispose();
        annotations.dispose();
        batch.dispose();
        depthBatch.dispose();
        if (shadow != null) {
            shadow.dispose();
        }
    }
}

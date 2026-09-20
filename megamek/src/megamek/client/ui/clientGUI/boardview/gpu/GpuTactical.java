/* Copyright (C) 2026 The MegaMek Team. SPDX-License-Identifier: GPL-3.0-or-later */
package megamek.client.ui.clientGUI.boardview.gpu;

import java.awt.BasicStroke;
import java.awt.Shape;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Camera;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.VertexAttributes;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g3d.Material;
import com.badlogic.gdx.graphics.g3d.ModelBatch;
import com.badlogic.gdx.graphics.g3d.ModelInstance;
import com.badlogic.gdx.graphics.g3d.attributes.BlendingAttribute;
import com.badlogic.gdx.graphics.g3d.attributes.ColorAttribute;
import com.badlogic.gdx.graphics.g3d.attributes.DepthTestAttribute;
import com.badlogic.gdx.graphics.g3d.attributes.IntAttribute;
import com.badlogic.gdx.graphics.g3d.utils.MeshPartBuilder;
import com.badlogic.gdx.graphics.g3d.utils.ModelBuilder;
import com.badlogic.gdx.math.Matrix4;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.utils.Disposable;
import megamek.client.ui.clientGUI.boardview.BoardTactical;
import megamek.client.ui.util.UIUtil;
import megamek.common.board.Coords;

/** GL-owned geometry and cached text. Camera movement never rebuilds the surface meshes or label artwork. */
final class GpuTactical implements Disposable {
    private record TextImage(BoardScene.Pixels pixels, float x, float y) { }
    private final ModelBatch batch = new ModelBatch();
    private final SpriteBatch textBatch = new SpriteBatch();
    private final GpuTextures<List<BoardTactical.Text>> textures = new GpuTextures<>(true);
    private final Map<List<BoardTactical.Text>, TextImage> images = new HashMap<>();
    private final Map<BoardTactical.Point, List<BoardTactical.Text>> labels = new LinkedHashMap<>();
    private final Material material = new Material(ColorAttribute.createDiffuse(Color.WHITE),
          new BlendingAttribute(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA),
          new DepthTestAttribute(GL20.GL_LEQUAL, false), IntAttribute.createCullFace(GL20.GL_NONE));
    private BoardScene previous;
    private ModelInstance instance;
    private int tuning = -1;
    private long builds;

    void update(BoardScene scene) {
        boolean terrainChanged = previous == null || previous.boardId() != scene.boardId()
              || previous.width() != scene.width() || previous.height() != scene.height() || !sameTerrain(scene);
        if (tuning != BoardGeometry.revision() || terrainChanged
              || !previous.tactical().fills().equals(scene.tactical().fills())) {
            rebuild(scene);
            tuning = BoardGeometry.revision();
        }
        if (previous == null || !previous.tactical().labels().equals(scene.tactical().labels())) {
            labels.clear();
            scene.tactical().labels().forEach(label ->
                  labels.computeIfAbsent(label.anchor(), key -> new ArrayList<>()).add(label.text()));
            labels.replaceAll((anchor, texts) -> List.copyOf(texts));
            images.keySet().retainAll(labels.values());
            Map<List<BoardTactical.Text>, BoardScene.Pixels> pixels = new HashMap<>();
            labels.values().forEach(texts -> pixels.put(texts, images.computeIfAbsent(texts, GpuTactical::paintText).pixels()));
            textures.update(pixels);
        }
        previous = scene;
    }

    private boolean sameTerrain(BoardScene scene) {
        if (previous.tiles() == scene.tiles()) {
            return true;
        }
        for (int i = 0; i < scene.tiles().size(); i++) {
            BoardScene.Tile a = previous.tiles().get(i), b = scene.tiles().get(i);
            if (a.elevation() != b.elevation() || a.waterDepth() != b.waterDepth() || a.frozen() != b.frozen()
                  || a.roadExits() != b.roadExits()) {
                return false;
            }
        }
        return true;
    }

    private void rebuild(BoardScene scene) {
        builds++;
        if (instance != null) {
            instance.model.dispose();
            instance = null;
        }
        if (scene.tactical().fills().isEmpty()) {
            return;
        }
        ModelBuilder builder = new ModelBuilder();
        builder.begin();
        MeshPartBuilder[] mesh = new MeshPartBuilder[1];
        int[] count = { 0 };
        BoardTacticalGeometry.drape(scene, triangle -> {
            // Three independent vertices per triangle, below the unsigned-short index limit even on large maps.
            if (count[0] % 10000 == 0) {
                mesh[0] = builder.part("tactical-" + count[0], GL20.GL_TRIANGLES,
                      VertexAttributes.Usage.Position | VertexAttributes.Usage.ColorPacked, material);
            }
            Color color = color(triangle.argb());
            mesh[0].triangle(vertex(triangle.a(), color), vertex(triangle.b(), color), vertex(triangle.c(), color));
            count[0]++;
        });
        var model = builder.end();
        if (count[0] == 0) {
            model.dispose();
        } else {
            instance = new ModelInstance(model);
        }
    }

    private static MeshPartBuilder.VertexInfo vertex(Vector3 point, Color color) {
        return new MeshPartBuilder.VertexInfo().setPos(point).setCol(color);
    }

    private static Color color(int argb) {
        return new Color(((argb >>> 16) & 255) / 255f, ((argb >>> 8) & 255) / 255f,
              (argb & 255) / 255f, (argb >>> 24) / 255f);
    }

    void render(Camera camera) {
        if (instance != null) {
            batch.begin(camera);
            batch.render(instance);
            batch.end();
        }
    }

    void renderLabels(Camera camera) {
        if (labels.isEmpty()) {
            return;
        }
        Gdx.gl.glDisable(GL20.GL_DEPTH_TEST);
        textBatch.setProjectionMatrix(new Matrix4().setToOrtho2D(0, 0, camera.viewportWidth, camera.viewportHeight));
        textBatch.begin();
        for (var entry : labels.entrySet()) {
            var anchor = entry.getKey();
            int column = Math.round((anchor.x() - BoardGeometry.TILE_WIDTH / 2) / (BoardGeometry.TILE_WIDTH * 0.75f));
            float halfHeight = BoardGeometry.TILE_HEIGHT / 2;
            Coords coords = new Coords(column,
                  Math.round((anchor.y() - halfHeight - (column & 1) * halfHeight) / BoardGeometry.TILE_HEIGHT));
            var tile = previous.tile(coords);
            if (tile == null) {
                continue;
            }
            Vector3 position = new Vector3(anchor.x() * BoardGeometry.HEX_SCALE, -anchor.y() * BoardGeometry.HEX_SCALE,
                  BoardGeometry.surfaceZ(tile) + 2 * BoardGeometry.HEX_SCALE);
            if (!camera.frustum.sphereInFrustum(position, BoardGeometry.WIDTH)) {
                continue;
            }
            Vector3 right = new Vector3(camera.direction).crs(camera.up).nor().scl(BoardGeometry.HEX_SCALE);
            Vector3 screen = camera.project(new Vector3(position), 0, 0, camera.viewportWidth, camera.viewportHeight);
            float scale = camera.project(new Vector3(position).add(right), 0, 0,
                  camera.viewportWidth, camera.viewportHeight).dst(screen);
            TextImage image = images.get(entry.getValue());
            var region = textures.region(entry.getValue());
            textBatch.draw(region, screen.x + image.x() * scale,
                  screen.y - (image.y() + image.pixels().height() / 2f) * scale,
                  image.pixels().width() * scale / 2, image.pixels().height() * scale / 2);
        }
        textBatch.end();
    }

    private static TextImage paintText(List<BoardTactical.Text> texts) {
        var metrics = new BufferedImage(1, 1, BufferedImage.TYPE_INT_ARGB).createGraphics();
        List<Shape> glyphs = new ArrayList<>();
        Rectangle2D bounds = null;
        try {
            for (var text : texts) {
                Shape shape = text.font().createGlyphVector(metrics.getFontRenderContext(), text.value())
                      .getOutline(text.x(), text.y());
                glyphs.add(shape);
                bounds = bounds == null ? shape.getBounds2D() : bounds.createUnion(shape.getBounds2D());
            }
        } finally {
            metrics.dispose();
        }
        int x = (int) Math.floor(bounds.getX()) - 2, y = (int) Math.floor(bounds.getY()) - 2;
        BufferedImage image = new BufferedImage(Math.max(1, (int) Math.ceil(bounds.getMaxX() - x + 2) * 2),
              Math.max(1, (int) Math.ceil(bounds.getMaxY() - y + 2) * 2), BufferedImage.TYPE_INT_ARGB);
        var graphics = image.createGraphics();
        try {
            UIUtil.setHighQualityRendering(graphics);
            graphics.scale(2, 2);
            graphics.translate(-x, -y);
            graphics.setStroke(new BasicStroke(1, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            for (int i = 0; i < texts.size(); i++) {
                graphics.setColor(new java.awt.Color(0, 0, 0, texts.get(i).argb() >>> 24));
                graphics.draw(glyphs.get(i));
                graphics.setColor(new java.awt.Color(texts.get(i).argb(), true));
                graphics.fill(glyphs.get(i));
            }
        } finally {
            graphics.dispose();
        }
        return new TextImage(new BoardScene.Pixels(image), x, y);
    }

    long builds() {
        return builds;
    }

    @Override
    public void dispose() {
        if (instance != null) {
            instance.model.dispose();
            instance = null;
        }
        batch.dispose();
        textBatch.dispose();
        textures.dispose();
        images.clear();
        labels.clear();
    }
}

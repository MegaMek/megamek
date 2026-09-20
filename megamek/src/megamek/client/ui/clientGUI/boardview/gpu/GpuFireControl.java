/* Copyright (C) 2026 The MegaMek Team. SPDX-License-Identifier: GPL-3.0-or-later */
package megamek.client.ui.clientGUI.boardview.gpu;

import java.awt.Font;
import java.awt.RenderingHints;
import java.awt.font.FontRenderContext;
import java.awt.image.BufferedImage;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Camera;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.VertexAttributes;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g3d.Material;
import com.badlogic.gdx.graphics.g3d.Model;
import com.badlogic.gdx.graphics.g3d.ModelBatch;
import com.badlogic.gdx.graphics.g3d.ModelInstance;
import com.badlogic.gdx.graphics.g3d.attributes.BlendingAttribute;
import com.badlogic.gdx.graphics.g3d.attributes.ColorAttribute;
import com.badlogic.gdx.graphics.g3d.attributes.DepthTestAttribute;
import com.badlogic.gdx.graphics.g3d.attributes.IntAttribute;
import com.badlogic.gdx.graphics.g3d.attributes.TextureAttribute;
import com.badlogic.gdx.graphics.g3d.utils.MeshPartBuilder;
import com.badlogic.gdx.graphics.g3d.utils.ModelBuilder;
import com.badlogic.gdx.math.Matrix4;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.utils.Disposable;
import megamek.MMConstants;
import megamek.client.ui.clientGUI.boardview.BoardView;
import megamek.client.ui.clientGUI.boardview.sprite.TextMarkerSprite;

/** Unlit, depth-tested tactical volumes shared by both cameras. Owns its meshes and batch on the GL thread. */
final class GpuFireControl implements Disposable {
    /** Positive size multiplier for firing-line thickness and the arrowhead; 1 keeps the current size. */
    static final float TARGET_ARROW_SIZE = 1f;
    /** Blank spaces between repeated range labels on the contour. */
    static final int RANGE_LABEL_SPACES = 3;
    /** Travel along the contour in unscaled board pixels per second; negative values reverse it. */
    static final float RANGE_SCROLL_SPEED = 20;
    /** Keep the entire flat label above the hex surface while it follows the camera. */
    static final float RANGE_LABEL_CLEARANCE_LEVELS = 1f / 3;
    private static final int LABEL_ART_SCALE = 3;
    private static final long ATTRIBUTES = VertexAttributes.Usage.Position | VertexAttributes.Usage.ColorPacked;
    private static final long TEXTURED_ATTRIBUTES = ATTRIBUTES | VertexAttributes.Usage.TextureCoordinates;
    /** Beam width as a fraction of its offset from the hex edge. */
    private static final float FRAME_RATIO = 0.6f;
    private final ModelBatch batch = new ModelBatch();
    private final SpriteBatch labelBatch = new SpriteBatch();
    private final GpuTextures<String> labelTextures = new GpuTextures<>(true);
    private final Map<String, BoardScene.Pixels> labelImages = new HashMap<>();
    private final Matrix4 labelMatrix = new Matrix4();
    private final Material material = new Material(ColorAttribute.createDiffuse(Color.WHITE),
          new BlendingAttribute(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA),
          new DepthTestAttribute(GL20.GL_LEQUAL, false), IntAttribute.createCullFace(GL20.GL_NONE));
    private List<BoardScene.FiringLine> firingLines = List.of();
    private List<BoardScene.RangeBorder> rangeBorders = List.of();
    private List<BoardScene.RangeLabel> rangeLabels = List.of();
    private BoardScene scene;
    private ModelInstance instance;
    private int tuning = -1;
    private double scrollDistance;

    void update(BoardScene scene) {
        update(scene, false);
    }

    void update(BoardScene scene, boolean hideArrows) {
        updateLabels(scene);
        List<BoardScene.FiringLine> shownLines = hideArrows ? List.of() : scene.firingLines();
        boolean changed = tuning != BoardGeometry.revision() || !firingLines.equals(shownLines)
              || !rangeBorders.equals(scene.rangeBorders()) || !sameTerrain(scene.tiles());
        this.scene = scene;
        if (!changed) {
            return;
        }
        tuning = BoardGeometry.revision();
        firingLines = shownLines;
        rangeBorders = scene.rangeBorders();
        if (instance != null) {
            instance.model.dispose();
            instance = null;
        }
        if (firingLines.isEmpty() && rangeBorders.isEmpty()) {
            return;
        }
        ModelBuilder builder = new ModelBuilder();
        builder.begin();
        int part = 0;
        // Register each shared texture with the model so clearing or rebuilding disposes it exactly once.
        Map<String, Texture> labels = new HashMap<>();
        for (List<BoardFiringGeometry.RangeSide> contour : BoardFiringGeometry.rangeContours(scene)) {
            List<BoardFiringGeometry.RangeEdge> walls = contour.stream()
                  .map(side -> BoardFiringGeometry.rangeWall(scene, side)).toList();
            Color color = color(contour.getFirst().border().rgb(), 0.18f);
            // Keep even a fragmented, very large range boundary below the mesh's 16-bit index limit.
            for (int first = 0; first < walls.size(); first += 512) {
                int limit = Math.min(first + 512, walls.size());
                MeshPartBuilder frame = builder.part("range-frame-" + part++, GL20.GL_TRIANGLES, ATTRIBUTES, material);
                for (int i = first; i < limit; i++) {
                    range(frame, walls.get(i), color);
                }
            }
            if (!BoardView.GPU_SCROLLING_RANGE_LABELS) {
                continue;
            }
            float[] distances = new float[walls.size() + 1];
            for (int i = 0; i < walls.size(); i++) {
                distances[i + 1] = distances[i] + walls.get(i).topA().dst(walls.get(i).topB());
            }
            float perimeter = distances[walls.size()];
            Texture texture = labels.computeIfAbsent(contour.getFirst().border().label(), label -> {
                Texture created = rangeTexture(label);
                builder.manage(created);
                return created;
            });
            float repeatLength = BoardFiringGeometry.RANGE_HEIGHT * BoardGeometry.LEVEL
                  * texture.getWidth() / texture.getHeight();
            // Fit a whole number of repeats, so a letter crosses the closing edge without a phase jump.
            float repeats = Math.max(1, Math.round(perimeter / repeatLength));
            Material outside = rangeMaterial("range-outside-" + part, texture, repeats / perimeter);
            Material inside = rangeMaterial("range-inside-" + part, texture, -repeats / perimeter);
            for (int first = 0; first < walls.size(); first += 512) {
                int limit = Math.min(first + 512, walls.size());
                for (int face = 0; face < 2; face++) {
                    MeshPartBuilder mesh = builder.part("range-face-" + part++, GL20.GL_TRIANGLES,
                          TEXTURED_ATTRIBUTES, face == 0 ? outside : inside);
                    for (int i = first; i < limit; i++) {
                        rangeFace(mesh, walls.get(i), color, distances[i], distances[i + 1], face == 1);
                    }
                }
            }
        }
        for (BoardScene.FiringLine line : firingLines) {
            MeshPartBuilder mesh = builder.part("attack-" + part++, GL20.GL_TRIANGLES, ATTRIBUTES, material);
            Color color = color(line.rgb(), 1).lerp(Color.WHITE, 0.22f);
            List<Vector3> path = BoardFiringGeometry.trajectory(scene, line);
            for (int i = 1; i < path.size(); i++) {
                tube(mesh, path.get(i - 1), path.get(i), .725f * BoardGeometry.HEX_SCALE * TARGET_ARROW_SIZE, color);
            }
            Vector3 end = path.getLast();
            Vector3 direction = new Vector3(end).sub(path.get(path.size() - 2)).nor();
            float length = Math.min(12 * BoardGeometry.HEX_SCALE, path.getFirst().dst(end) * 0.2f) * TARGET_ARROW_SIZE;
            cone(mesh, new Vector3(end).mulAdd(direction, -length), end, length * 0.2f, color);
        }
        Model model = builder.end();
        instance = new ModelInstance(model);
    }

    /** Target bands follow the current selection's assignments independently of arrow visibility. */
    boolean targets(int entityId) {
        return scene != null && scene.selectedId() >= 0 && scene.firingLines().stream()
              .anyMatch(line -> line.attackerId() == scene.selectedId() && line.targetId() == entityId);
    }

    private void updateLabels(BoardScene scene) {
        List<BoardScene.RangeLabel> next = BoardView.GPU_SCROLLING_RANGE_LABELS ? List.of() : scene.rangeLabels();
        if (rangeLabels.equals(next)) {
            return;
        }
        rangeLabels = next;
        List<String> text = next.stream().map(BoardScene.RangeLabel::label).distinct().toList();
        labelImages.keySet().retainAll(text);
        for (String label : text) {
            labelImages.computeIfAbsent(label, GpuFireControl::labelArtwork);
        }
        labelTextures.update(labelImages);
    }

    private static BoardScene.Pixels labelArtwork(String label) {
        BufferedImage image = new BufferedImage((int) BoardGeometry.TILE_WIDTH * LABEL_ART_SCALE,
              (int) BoardGeometry.TILE_HEIGHT * LABEL_ART_SCALE, BufferedImage.TYPE_INT_ARGB);
        var graphics = image.createGraphics();
        try {
            TextMarkerSprite.drawMarker(graphics, label, java.awt.Color.WHITE,
                  image.getWidth(), image.getHeight(), LABEL_ART_SCALE);
        } finally {
            graphics.dispose();
        }
        return new BoardScene.Pixels(image);
    }

    /** No time input: range letters are always flat and upright, including while orbiting or switching cameras. */
    static void labelTransform(Matrix4 out, Camera camera, BoardScene.Tile tile) {
        Vector3 right = new Vector3(camera.direction).crs(camera.up).nor();
        float clearance = BoardGeometry.LEVEL * RANGE_LABEL_CLEARANCE_LEVELS
              + (BoardGeometry.WIDTH * Math.abs(right.z) + BoardGeometry.HEIGHT * Math.abs(camera.up.z)) / 2;
        Vector3 center = BoardGeometry.center(tile.coords(), tile.elevation()).add(0, 0, clearance);
        out.set(center, GpuMarkers.orientation(camera, true));
    }

    /** Ground labels and deployment highlights do not invalidate this geometry. */
    private boolean sameTerrain(List<BoardScene.Tile> next) {
        List<BoardScene.Tile> tiles = scene == null ? List.of() : scene.tiles();
        if (tiles == next) {
            return true;
        }
        if (tiles.size() != next.size()) {
            return false;
        }
        for (int i = 0; i < tiles.size(); i++) {
            BoardScene.Tile a = tiles.get(i), b = next.get(i);
            if (!a.coords().equals(b.coords()) || a.elevation() != b.elevation() || !a.features().equals(b.features())) {
                return false;
            }
        }
        return true;
    }

    private static Color color(int rgb, float alpha) {
        return new Color(((rgb >>> 16) & 255) / 255f, ((rgb >>> 8) & 255) / 255f, (rgb & 255) / 255f, alpha);
    }

    private Material rangeMaterial(String id, Texture texture, float scale) {
        Material result = material.copy();
        result.id = id;
        TextureAttribute diffuse = TextureAttribute.createDiffuse(texture);
        diffuse.scaleU = scale;
        // Each face is readable from its own side and moves in the same world direction.
        result.set(diffuse, IntAttribute.createCullFace(GL20.GL_BACK));
        return result;
    }

    private static Texture rangeTexture(String label) {
        Font font = new Font(MMConstants.FONT_SANS_SERIF, Font.BOLD, 96);
        FontRenderContext context = new FontRenderContext(null, true, true);
        var glyph = font.createGlyphVector(context, label);
        var bounds = glyph.getPixelBounds(context, 0, 0);
        int gap = (int) Math.ceil(font.getStringBounds(" ".repeat(RANGE_LABEL_SPACES), context).getWidth());
        BufferedImage image = new BufferedImage(Math.max(1, bounds.width + gap), 128, BufferedImage.TYPE_INT_ARGB);
        var graphics = image.createGraphics();
        try {
            graphics.setColor(new java.awt.Color(255, 255, 255, 46));
            graphics.fillRect(0, 0, image.getWidth(), image.getHeight());
            graphics.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            graphics.setColor(java.awt.Color.WHITE);
            graphics.drawGlyphVector(glyph, (image.getWidth() - bounds.width) / 2f - bounds.x,
                  (image.getHeight() - bounds.height) / 2f - bounds.y);
        } finally {
            graphics.dispose();
        }
        Pixmap pixels = GpuTextures.pixmap(new BoardScene.Pixels(image), 0, false);
        try {
            Texture texture = new Texture(pixels, true);
            texture.setFilter(Texture.TextureFilter.MipMapLinearLinear, Texture.TextureFilter.Linear);
            texture.setWrap(Texture.TextureWrap.Repeat, Texture.TextureWrap.ClampToEdge);
            return texture;
        } finally {
            pixels.dispose();
        }
    }

    private static Vector3 thickness(BoardFiringGeometry.RangeEdge edge) {
        Vector3 along = new Vector3(edge.bottomB().x - edge.bottomA().x,
              edge.bottomB().y - edge.bottomA().y, 0).nor();
        return new Vector3(Vector3.Z).crs(along)
              .scl(BoardGeometry.MARKER_INSET * BoardGeometry.WIDTH / 2 * FRAME_RATIO);
    }

    private static void rangeFace(MeshPartBuilder mesh, BoardFiringGeometry.RangeEdge edge, Color color,
          float from, float to, boolean inside) {
        Vector3 shift = inside ? thickness(edge) : new Vector3();
        Vector3 a = new Vector3(inside ? edge.bottomB() : edge.bottomA()).add(shift);
        Vector3 b = new Vector3(inside ? edge.bottomA() : edge.bottomB()).add(shift);
        Vector3 c = new Vector3(inside ? edge.topA() : edge.topB()).add(shift);
        Vector3 d = new Vector3(inside ? edge.topB() : edge.topA()).add(shift);
        Color ink = new Color(color.r, color.g, color.b, 1);
        float height = BoardFiringGeometry.RANGE_HEIGHT * BoardGeometry.LEVEL;
        float vA = (d.z - a.z) / height, vB = (c.z - b.z) / height;
        float uA = inside ? to : from, uB = inside ? from : to;
        // Clamp V below the top band, retaining a translucent skirt at cliffs without stretching the letters.
        mesh.rect(vertex(a, ink).setUV(uA, vA), vertex(b, ink).setUV(uB, vB),
              vertex(c, ink).setUV(uB, 0), vertex(d, ink).setUV(uA, 0));
    }

    private static void range(MeshPartBuilder mesh, BoardFiringGeometry.RangeEdge edge, Color color) {
        Vector3 a = edge.bottomA(), b = edge.bottomB(), c = edge.topB(), d = edge.topA();
        Vector3 shift = thickness(edge);
        Vector3 innerA = new Vector3(a).add(shift), innerB = new Vector3(b).add(shift);
        Vector3 innerC = new Vector3(c).add(shift), innerD = new Vector3(d).add(shift);
        if (!BoardView.GPU_SCROLLING_RANGE_LABELS) {
            quad(mesh, a, b, c, d, color);
            quad(mesh, innerB, innerA, innerD, innerC, color);
        }
        quad(mesh, a, innerA, innerB, b, color);
        quad(mesh, a, d, innerD, innerA, color);
        quad(mesh, b, innerB, innerC, c, color);
        Color cap = new Color(color).mul(1, 1, 1, 1.5f);
        quad(mesh, d, c, innerC, innerD, cap);
        tube(mesh, d, c, 0.65f * BoardGeometry.HEX_SCALE, new Color(color.r, color.g, color.b, 0.85f));
    }

    private static void quad(MeshPartBuilder mesh, Vector3 a, Vector3 b, Vector3 c, Vector3 d, Color color) {
        mesh.rect(vertex(a, color), vertex(b, color), vertex(c, color), vertex(d, color));
    }

    private static MeshPartBuilder.VertexInfo vertex(Vector3 point, Color color) {
        return new MeshPartBuilder.VertexInfo().setPos(point).setCol(color);
    }

    private static Vector3[] ring(Vector3 start, Vector3 end, float radius) {
        Vector3 direction = new Vector3(end).sub(start).nor();
        Vector3 side = new Vector3(direction).crs(Math.abs(direction.z) > 0.95f ? Vector3.Y : Vector3.Z).nor().scl(radius);
        Vector3 up = new Vector3(direction).crs(side).nor().scl(radius);
        return new Vector3[] { side, up, new Vector3(side).scl(-1), new Vector3(up).scl(-1) };
    }

    private static void tube(MeshPartBuilder mesh, Vector3 start, Vector3 end, float radius, Color color) {
        if (start.dst2(end) < 0.0001f) {
            return;
        }
        Vector3[] ring = ring(start, end, radius);
        for (int i = 0; i < ring.length; i++) {
            Vector3 a = ring[i], b = ring[(i + 1) % ring.length];
            quad(mesh, new Vector3(start).add(a), new Vector3(start).add(b),
                  new Vector3(end).add(b), new Vector3(end).add(a), color);
        }
    }

    private static void cone(MeshPartBuilder mesh, Vector3 base, Vector3 tip, float radius, Color color) {
        Vector3[] ring = ring(base, tip, radius);
        for (int i = 0; i < ring.length; i++) {
            mesh.triangle(vertex(new Vector3(base).add(ring[i]), color),
                  vertex(new Vector3(base).add(ring[(i + 1) % ring.length]), color), vertex(tip, color));
        }
    }

    void render(Camera camera, float deltaSeconds) {
        if (BoardView.GPU_SCROLLING_RANGE_LABELS) {
            scrollDistance += deltaSeconds * RANGE_SCROLL_SPEED * BoardGeometry.HEX_SCALE;
        }
        if (instance != null) {
            if (BoardView.GPU_SCROLLING_RANGE_LABELS) {
                for (Material face : instance.materials) {
                    TextureAttribute texture = face.get(TextureAttribute.class, TextureAttribute.Diffuse);
                    if (texture != null) {
                        double offset = -scrollDistance * texture.scaleU;
                        texture.offsetU = (float) (offset - Math.floor(offset));
                    }
                }
            }
            batch.begin(camera);
            batch.render(instance);
            batch.end();
        }
    }

    /** Draw after ground markings so their text cannot overpaint the camera-facing letters. */
    void renderLabels(Camera camera) {
        if (!rangeLabels.isEmpty()) {
            Gdx.gl.glEnable(GL20.GL_DEPTH_TEST);
            Gdx.gl.glDepthFunc(GL20.GL_LEQUAL);
            labelBatch.setProjectionMatrix(camera.combined);
            labelBatch.begin();
            for (BoardScene.RangeLabel label : rangeLabels) {
                BoardScene.Tile tile = scene.tile(label.coords());
                if (tile == null) {
                    continue;
                }
                labelTransform(labelMatrix, camera, tile);
                labelBatch.setTransformMatrix(labelMatrix);
                labelBatch.setColor(color(label.rgb(), 1));
                labelBatch.draw(labelTextures.region(label.label()), -BoardGeometry.WIDTH / 2,
                      -BoardGeometry.HEIGHT / 2, BoardGeometry.WIDTH, BoardGeometry.HEIGHT);
            }
            labelBatch.end();
        }
    }

    @Override
    public void dispose() {
        if (instance != null) {
            instance.model.dispose();
            instance = null;
        }
        batch.dispose();
        labelBatch.dispose();
        labelTextures.dispose();
        labelImages.clear();
    }
}

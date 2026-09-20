/* Copyright (C) 2026 The MegaMek Team. SPDX-License-Identifier: GPL-3.0-or-later */
package megamek.client.ui.clientGUI.boardview.gpu;

import java.awt.BasicStroke;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.Shape;
import java.awt.geom.AffineTransform;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.Collection;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import com.badlogic.gdx.graphics.Camera;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.VertexAttributes;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.graphics.g3d.Material;
import com.badlogic.gdx.graphics.g3d.Model;
import com.badlogic.gdx.graphics.g3d.ModelInstance;
import com.badlogic.gdx.graphics.g3d.attributes.ColorAttribute;
import com.badlogic.gdx.graphics.g3d.attributes.IntAttribute;
import com.badlogic.gdx.graphics.g3d.attributes.TextureAttribute;
import com.badlogic.gdx.graphics.g3d.utils.ModelBuilder;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Matrix4;
import com.badlogic.gdx.math.Quaternion;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.math.collision.BoundingBox;
import com.badlogic.gdx.utils.Disposable;
import megamek.client.ui.clientGUI.boardview.BoardMarker;
import megamek.client.ui.clientGUI.boardview.sprite.CollapseWarningSprite;
import megamek.client.ui.util.FontHandler;
import megamek.client.ui.util.UIUtil;
import megamek.common.board.Coords;

/** Shared GL-owned marker artwork and camera-aware placement. No game rules or entity state live here. */
final class GpuMarkers implements Disposable {
    /** Degrees away from directly overhead: at or below this tilt, markers face the camera without spinning. */
    static final float FLAT_TILT_DEGREES = 30;
    static final float SPIN_PERIOD_SECONDS = 8;
    static final float SIZE_IN_HEXES = 0.5f;
    static final float SENSOR_SIZE_IN_HEXES = 0.85f;
    static final float SENSOR_CROWDED_SIZE_IN_HEXES = 0.6f;
    static final float SENSOR_DENSE_SIZE_IN_HEXES = 0.5f;
    static final float THICKNESS_RATIO = 0.09f;
    static final float CLEARANCE_LEVELS = 0.3f;
    static final float GROUP_SCALE = 0.7f;
    static final float GROUP_SPACING = 1.2f;
    static final int GROUP_COLUMNS = 3;
    static final float TOP_GROUP_SIZE_IN_HEXES = 0.28f;
    static final float TOP_GROUP_RADIUS_IN_HEXES = 0.28f;
    static final float TOP_UNIT_MARKER_SIZE_IN_HEXES = 0.17f;
    static final float TOP_UNIT_MARKER_RADIUS_IN_HEXES = 0.375f;
    static final float TOP_UNIT_MARKER_SIDE_ARC_DEGREES = 60;
    /** Each symbol can opt out independently. The existing See-through intensity slider still applies. */
    static final boolean SENSOR_CONTACT_OUTLINE = true;
    static final boolean COLLAPSE_WARNING_OUTLINE = true;
    static final boolean MINEFIELD_OUTLINE = true;
    static final boolean DEMOLITION_CHARGE_OUTLINE = true;
    static final boolean ARTILLERY_INCOMING_OUTLINE = true;
    static final boolean ARTILLERY_TARGET_OUTLINE = true;
    static final boolean ARTILLERY_ADJUSTED_OUTLINE = true;
    static final boolean ARTILLERY_AUTO_HIT_OUTLINE = true;
    static final boolean ORBITAL_INCOMING_OUTLINE = true;
    static final boolean NUKE_INCOMING_OUTLINE = true;
    static final boolean OBJECTIVE_OUTLINE = true;
    static final boolean PLAYER_NOTE_OUTLINE = true;
    static final boolean CARGO_OUTLINE = true;
    static final boolean FLARE_OUTLINE = true;
    static final boolean SAW_CLEARING_OUTLINE = true;
    static final boolean BRIDGE_REPAIRED_OUTLINE = true;
    static final boolean BRIDGE_BUILD_OUTLINE = true;
    static final boolean FORTIFY_BUILD_OUTLINE = true;
    static final boolean RUBBLE_CLEAR_OUTLINE = true;
    static final boolean DUG_IN_OUTLINE = true;
    static final int SENSOR_RGB = BoardMarker.Kind.SENSOR_CONTACT.rgb();
    static final int COLLAPSE_RGB = BoardMarker.Kind.COLLAPSE_WARNING.rgb();
    private static final int ART_SIZE = 96;
    private static final Font LABEL_FONT = new Font(Font.SANS_SERIF, Font.BOLD, 16);

    private final GpuTextures<BoardMarker.Kind> textures = new GpuTextures<>(true);
    private final Map<BoardMarker.Kind, GpuUnitModel> models = new EnumMap<>(BoardMarker.Kind.class);
    private final Map<BoardMarker, ModelInstance> locations = new LinkedHashMap<>();
    private final GpuTextures<String> labelTextures = new GpuTextures<>();
    private final Map<String, BoardScene.Pixels> labelImages = new HashMap<>();
    private final Map<BoardMarker, Vector3> labelAnchors = new LinkedHashMap<>();
    private List<BoardMarker> previousMarkers = List.of();
    private Map<Coords, List<BoardMarker>> groups = Map.of();
    private float clock;

    GpuMarkers() {
        Map<BoardMarker.Kind, BoardScene.Pixels> artwork = new EnumMap<>(BoardMarker.Kind.class);
        for (var kind : BoardMarker.Kind.values()) {
            artwork.put(kind, artwork(kind));
        }
        textures.update(artwork);
        artwork.forEach((kind, pixels) -> {
            GpuUnitModel model = new GpuUnitModel(symbol(pixels, textures.region(kind)));
            tint(model.instance, kind.rgb());
            // Instance materials are copies. Future instances (including sensors) need the same default tint.
            model.instance.model.materials.forEach(material -> material.set(ColorAttribute.createDiffuse(color(kind.rgb()))));
            models.put(kind, model);
        });
    }

    GpuUnitModel model(BoardMarker.Kind kind) {
        return models.get(kind);
    }

    /** Spinning map symbols retain artwork on both faces; this geometry is never used for a known unit. */
    private static Model symbol(BoardScene.Pixels pixels, TextureRegion region) {
        ModelBuilder builder = new ModelBuilder();
        builder.begin();
        var caps = builder.part("symbol", GL20.GL_TRIANGLES,
              VertexAttributes.Usage.Position | VertexAttributes.Usage.TextureCoordinates | VertexAttributes.Usage.Normal,
              new Material(TextureAttribute.createDiffuse(region.getTexture()), IntAttribute.createCullFace(GL20.GL_NONE)));
        var sides = builder.part("edge", GL20.GL_TRIANGLES, VertexAttributes.Usage.Position | VertexAttributes.Usage.Normal,
              new Material(ColorAttribute.createDiffuse(GpuCutout.averageColor(pixels)), IntAttribute.createCullFace(GL20.GL_NONE)));
        GpuCutout.extrude(pixels, region, Vector3.Zero, 1, 1, true, caps, sides);
        return builder.end();
    }

    /** One cosmetic clock and transform for location markers and sensor contacts, independent of playback speed. */
    void beginFrame(List<BoardMarker> markers, float seconds) {
        clock = (clock + seconds) % SPIN_PERIOD_SECONDS;
        if (!previousMarkers.equals(markers)) {
            locations.keySet().retainAll(Set.copyOf(markers));
            Set<String> labels = markers.stream().map(BoardMarker::label).filter(label -> !label.isBlank())
                  .collect(Collectors.toSet());
            labelImages.keySet().retainAll(labels);
            labels.forEach(label -> labelImages.computeIfAbsent(label, GpuMarkers::labelArtwork));
            labelTextures.update(labelImages);
            groups = markers.stream().collect(Collectors.groupingBy(BoardMarker::coords,
                  LinkedHashMap::new, Collectors.toList()));
            previousMarkers = List.copyOf(markers);
        }
    }

    /** Called after unit poses, movement and hover have been applied, using only this frame's rendered geometry. */
    void update(Collection<ModelInstance> units, Camera camera) {
        labelAnchors.clear();
        List<BoundingBox> bounds = groups.isEmpty() ? List.of() : units.stream().map(UnitBounds::world).toList();
        for (List<BoardMarker> group : groups.values()) {
            Coords coords = group.getFirst().coords();
            int count = group.size();
            BoundingBox occupied = occupiedBounds(coords, bounds);
            float size = markerSize(camera, count, occupied.isValid());
            float elevation = (float) group.stream().mapToDouble(BoardMarker::elevation).max().orElse(0);
            Vector3 base = locationSupport(coords, elevation, occupied);
            for (int index = 0; index < group.size(); index++) {
                BoardMarker marker = group.get(index);
                ModelInstance instance = locations.computeIfAbsent(marker, key -> {
                    ModelInstance created = new ModelInstance(model(key.kind()).instance.model);
                    tint(created, key.rgb());
                    return created;
                });
                Vector3 support = new Vector3(base).add(groupOffset(camera, index, count, occupied.isValid()));
                transform(instance.transform, camera, support, clock, size,
                      BoardGeometry.LEVEL * CLEARANCE_LEVELS);
                labelAnchors.put(marker, anchor(instance, camera, size));
            }
        }
    }

    static float markerSize(Camera camera, int count, boolean occupied) {
        if (flat(camera) && (occupied || count > 1)) {
            float size = BoardGeometry.HEIGHT * (occupied ? TOP_UNIT_MARKER_SIZE_IN_HEXES : TOP_GROUP_SIZE_IN_HEXES);
            if (occupied) {
                int perSide = (count + 1) / 2;
                return perSide == 1 ? size : Math.min(size, 2 * topRadius(true)
                      * MathUtils.sinDeg(TOP_UNIT_MARKER_SIDE_ARC_DEGREES / (perSide - 1) / 2) / GROUP_SPACING);
            }
            return count == 1 ? size : Math.min(size, 2 * topRadius(occupied) * MathUtils.sin(MathUtils.PI / count)
                  / GROUP_SPACING);
        }
        int columns = Math.min(count, GROUP_COLUMNS);
        return Math.min(size() * (count == 1 ? 1 : GROUP_SCALE),
              BoardGeometry.HEIGHT / ((columns - 1) * GROUP_SPACING + 1 + THICKNESS_RATIO));
    }

    private static float topRadius(boolean occupied) {
        return BoardGeometry.HEIGHT * (occupied ? TOP_UNIT_MARKER_RADIUS_IN_HEXES : TOP_GROUP_RADIUS_IN_HEXES);
    }

    /** Overhead symbols flank units, or form a ring in empty hexes. Angled symbols form a grid above the unit. */
    static Vector3 groupOffset(Camera camera, int index, int count, boolean occupied) {
        Vector3 right = new Vector3(camera.direction).crs(camera.up).nor();
        if (flat(camera)) {
            if (count == 1 && !occupied) {
                return new Vector3();
            }
            Vector3 up = new Vector3(camera.up.x, camera.up.y, 0).nor();
            if (occupied) {
                // Alternate sides, keeping each side centred and the question mark's hook and dot unobstructed.
                int sideCount = (count + 1 - index % 2) / 2;
                float angle = sideCount == 1 ? 0
                      : TOP_UNIT_MARKER_SIDE_ARC_DEGREES * (0.5f - (index / 2) / (float) (sideCount - 1));
                float side = index % 2 == 0 ? -1 : 1;
                return right.scl(side * MathUtils.cosDeg(angle) * topRadius(true))
                      .mulAdd(up, MathUtils.sinDeg(angle) * topRadius(true));
            }
            float angle = -90 + index * 360f / count;
            return right.scl(MathUtils.cosDeg(angle) * topRadius(occupied))
                  .mulAdd(up, MathUtils.sinDeg(angle) * topRadius(occupied));
        }
        int columns = Math.min(count, GROUP_COLUMNS);
        int rowColumns = Math.min(columns, count - (index / columns) * columns);
        float spacing = markerSize(camera, count, occupied) * GROUP_SPACING;
        return right.scl((index % columns - (rowColumns - 1) / 2f) * spacing)
              .mulAdd(Vector3.Z, (index / columns) * spacing);
    }

    /** Clear posed units over this hex, including a moving or multi-hex model whose origin is elsewhere. */
    static BoundingBox occupiedBounds(Coords coords, Collection<BoundingBox> units) {
        Vector3 center = BoardGeometry.center(coords, 0);
        BoundingBox occupied = new BoundingBox().inf();
        for (BoundingBox bounds : units) {
            // The point of the world bounds nearest the hex centre determines XY overlap with its convex hexagon.
            if (BoardGeometry.contains(coords, MathUtils.clamp(center.x, bounds.min.x, bounds.max.x),
                  MathUtils.clamp(center.y, bounds.min.y, bounds.max.y))) {
                occupied.ext(bounds);
            }
        }
        return occupied;
    }

    static Vector3 locationSupport(Coords coords, float elevation, BoundingBox occupied) {
        return BoardGeometry.center(coords, occupied.isValid()
              ? Math.max(elevation, occupied.max.z / BoardGeometry.LEVEL) : elevation);
    }

    /** A contact occupies the unit's position, never a small location-marker slot or a neighbouring roof. */
    Vector3 placeSensor(Coords coords, ModelInstance instance, Camera camera, Vector3 position) {
        int count = groups.getOrDefault(coords, List.of()).size();
        float hexes = flat(camera) && count > 0
              ? (count > 4 ? SENSOR_DENSE_SIZE_IN_HEXES : SENSOR_CROWDED_SIZE_IN_HEXES) : SENSOR_SIZE_IN_HEXES;
        float size = BoardGeometry.HEIGHT * hexes;
        transform(instance.transform, camera, position, clock, size, 0.5f * BoardGeometry.HEX_SCALE);
        return anchor(instance, camera, size);
    }

    Collection<ModelInstance> instances() {
        return locations.values();
    }

    Map<BoardMarker, ModelInstance> locatedInstances() {
        return locations;
    }

    List<Rectangle> labelObstacles(Camera camera) {
        List<Rectangle> result = new ArrayList<>();
        for (ModelInstance instance : locations.values()) {
            var bounds = UnitBounds.world(instance);
            if (!camera.frustum.boundsInFrustum(bounds)) {
                continue;
            }
            float minX = Float.POSITIVE_INFINITY, minY = Float.POSITIVE_INFINITY;
            float maxX = Float.NEGATIVE_INFINITY, maxY = Float.NEGATIVE_INFINITY;
            for (int corner = 0; corner < 8; corner++) {
                Vector3 point = camera.project(new Vector3((corner & 1) == 0 ? bounds.min.x : bounds.max.x,
                      (corner & 2) == 0 ? bounds.min.y : bounds.max.y, (corner & 4) == 0 ? bounds.min.z : bounds.max.z),
                      0, 0, camera.viewportWidth, camera.viewportHeight);
                minX = Math.min(minX, point.x);
                minY = Math.min(minY, point.y);
                maxX = Math.max(maxX, point.x);
                maxY = Math.max(maxY, point.y);
            }
            result.add(new Rectangle(minX, minY, maxX - minX, maxY - minY));
        }
        return result;
    }

    List<ModelInstance> outlinedInstances() {
        return locations.entrySet().stream().filter(entry -> outlineEnabled(entry.getKey().kind()))
              .map(Map.Entry::getValue).toList();
    }

    static boolean outlineEnabled(BoardMarker.Kind kind) {
        return switch (kind) {
            case SENSOR_CONTACT -> SENSOR_CONTACT_OUTLINE;
            case COLLAPSE_WARNING -> COLLAPSE_WARNING_OUTLINE;
            case MINEFIELD -> MINEFIELD_OUTLINE;
            case DEMOLITION_CHARGE -> DEMOLITION_CHARGE_OUTLINE;
            case ARTILLERY_INCOMING -> ARTILLERY_INCOMING_OUTLINE;
            case ARTILLERY_TARGET -> ARTILLERY_TARGET_OUTLINE;
            case ARTILLERY_ADJUSTED -> ARTILLERY_ADJUSTED_OUTLINE;
            case ARTILLERY_AUTO_HIT -> ARTILLERY_AUTO_HIT_OUTLINE;
            case ORBITAL_INCOMING -> ORBITAL_INCOMING_OUTLINE;
            case NUKE_INCOMING -> NUKE_INCOMING_OUTLINE;
            case OBJECTIVE -> OBJECTIVE_OUTLINE;
            case PLAYER_NOTE -> PLAYER_NOTE_OUTLINE;
            case CARGO -> CARGO_OUTLINE;
            case FLARE -> FLARE_OUTLINE;
            case SAW_CLEARING -> SAW_CLEARING_OUTLINE;
            case BRIDGE_REPAIRED -> BRIDGE_REPAIRED_OUTLINE;
            case BRIDGE_BUILD -> BRIDGE_BUILD_OUTLINE;
            case FORTIFY_BUILD -> FORTIFY_BUILD_OUTLINE;
            case RUBBLE_CLEAR -> RUBBLE_CLEAR_OUTLINE;
            case DUG_IN -> DUG_IN_OUTLINE;
        };
    }

    private static Color color(int rgb) {
        Color color = new Color();
        Color.rgb888ToColor(color, rgb);
        return color;
    }

    private static void tint(ModelInstance instance, int rgb) {
        Color color = color(rgb);
        instance.materials.forEach(material -> material.set(ColorAttribute.createDiffuse(color)));
        instance.userData = color;
    }

    Vector3 place(ModelInstance instance, Camera camera, Vector3 support) {
        transform(instance.transform, camera, support, clock);
        return anchor(instance, camera, size());
    }

    private static Vector3 anchor(ModelInstance instance, Camera camera, float size) {
        return new Vector3(0, 0, 0.5f).mul(instance.transform).mulAdd(camera.up, size / 2);
    }

    static float size() {
        return BoardGeometry.HEIGHT * SIZE_IN_HEXES;
    }

    static boolean flat(Camera camera) {
        return -camera.direction.z >= MathUtils.cosDeg(FLAT_TILT_DEGREES);
    }

    static void transform(Matrix4 out, Camera camera, Vector3 support, float seconds) {
        transform(out, camera, support, seconds, size(), BoardGeometry.LEVEL * CLEARANCE_LEVELS);
    }

    /** Artwork lies in local XY, extruded from Z=0 to 1. Clear the entire support in either camera mode. */
    static void transform(Matrix4 out, Camera camera, Vector3 support, float seconds, float size, float clearance) {
        float thickness = size * THICKNESS_RATIO;
        boolean flat = flat(camera);
        out.set(center(support, size, clearance), orientation(camera, flat));
        if (!flat) {
            out.rotate(Vector3.Y, (seconds % SPIN_PERIOD_SECONDS) * 360 / SPIN_PERIOD_SECONDS);
        }
        out.scale(size / ART_SIZE, size / ART_SIZE, thickness).translate(0, 0, -0.5f);
    }

    private static Vector3 center(Vector3 support, float size, float clearance) {
        return new Vector3(support).add(0, 0, clearance + size * (1 + THICKNESS_RATIO) / 2);
    }

    /** Shared facing for XY artwork. Flat labels follow both camera tilt and rotation, without a spin phase. */
    static Quaternion orientation(Camera camera, boolean flat) {
        Vector3 up = flat ? camera.up : Vector3.Z;
        Vector3 normal = new Vector3(camera.direction).scl(-1);
        if (!flat) {
            normal.z = 0;
        }
        normal.nor();
        Vector3 right = new Vector3(up).crs(normal).nor();
        return new Quaternion().setFromAxes(right.x, up.x, normal.x,
              right.y, up.y, normal.y, right.z, up.z, normal.z);
    }

    /** Screen-facing text is cached separately from spinning artwork and shares unit-label collision avoidance. */
    void renderLabels(SpriteBatch batch, Camera camera, float layoutScale, List<Rectangle> occupied) {
        for (var entry : labelAnchors.entrySet()) {
            String label = entry.getKey().label();
            if (label.isBlank() || !camera.frustum.pointInFrustum(entry.getValue())) {
                continue;
            }
            Vector3 point = camera.project(new Vector3(entry.getValue()), 0, 0, camera.viewportWidth, camera.viewportHeight);
            var region = labelTextures.region(label);
            float scale = Math.min(layoutScale, camera.viewportWidth / region.getRegionWidth());
            float width = region.getRegionWidth() * scale;
            float height = region.getRegionHeight() * scale;
            Rectangle bounds = GpuBattleView.spreadAnnotation(new Rectangle(point.x - width / 2,
                  point.y + 3 * layoutScale, width, height), occupied, camera.viewportWidth, camera.viewportHeight,
                  2 * layoutScale);
            occupied.add(bounds);
            batch.draw(region, bounds.x, bounds.y, bounds.width, bounds.height);
        }
    }

    private static BoardScene.Pixels labelArtwork(String label) {
        String[] lines = label.split("\\n");
        BufferedImage measure = new BufferedImage(1, 1, BufferedImage.TYPE_INT_ARGB);
        Graphics2D graphics = measure.createGraphics();
        graphics.setFont(LABEL_FONT);
        var metrics = graphics.getFontMetrics();
        int width = 0;
        for (String line : lines) {
            width = Math.max(width, metrics.stringWidth(line));
        }
        int lineHeight = metrics.getHeight();
        int ascent = metrics.getAscent();
        graphics.dispose();
        BufferedImage image = new BufferedImage(width + 12, lines.length * lineHeight + 6, BufferedImage.TYPE_INT_ARGB);
        graphics = image.createGraphics();
        try {
            UIUtil.setHighQualityRendering(graphics);
            graphics.setFont(LABEL_FONT);
            graphics.setColor(new java.awt.Color(25, 28, 32, 225));
            graphics.fillRoundRect(0, 0, image.getWidth(), image.getHeight(), 8, 8);
            graphics.setColor(java.awt.Color.WHITE);
            for (int index = 0; index < lines.length; index++) {
                graphics.drawString(lines[index], (image.getWidth() - metrics.stringWidth(lines[index])) / 2,
                      3 + ascent + index * lineHeight);
            }
        } finally {
            graphics.dispose();
        }
        return new BoardScene.Pixels(image);
    }

    /** Neutral artwork is tinted per instance, preserving objective-owner and user-configured charge colours. */
    static BoardScene.Pixels artwork(BoardMarker.Kind kind) {
        BufferedImage image = new BufferedImage(ART_SIZE, ART_SIZE, BufferedImage.TYPE_INT_ARGB);
        Graphics2D graphics = image.createGraphics();
        try {
            UIUtil.setHighQualityRendering(graphics);
            graphics.setColor(java.awt.Color.WHITE);
            if (kind == BoardMarker.Kind.SENSOR_CONTACT) {
                glyph(graphics, "?", new Font(Font.SANS_SERIF, Font.BOLD, ART_SIZE), ART_SIZE - 4);
            } else if (kind == BoardMarker.Kind.OBJECTIVE) {
                graphics.fillRoundRect(23, 12, 7, 74, 3, 3);
                graphics.fillPolygon(new int[] { 30, 77, 64, 77, 30 }, new int[] { 15, 15, 32, 49, 49 }, 5);
            } else {
                graphics.setColor(new java.awt.Color(0x686868));
                graphics.fillOval(1, 1, ART_SIZE - 2, ART_SIZE - 2);
                graphics.setColor(java.awt.Color.WHITE);
                graphics.fillOval(5, 5, ART_SIZE - 10, ART_SIZE - 10);
                graphics.setColor(new java.awt.Color(0x232323));
                graphics.setStroke(new BasicStroke(5, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
                symbol(graphics, kind);
            }
        } finally {
            graphics.dispose();
        }
        return new BoardScene.Pixels(image);
    }

    private static void symbol(Graphics2D g, BoardMarker.Kind kind) {
        switch (kind) {
            case COLLAPSE_WARNING -> glyph(g, CollapseWarningSprite.WARNING_SIGN, FontHandler.symbolFont(), ART_SIZE * 0.67f);
            case MINEFIELD, SAW_CLEARING -> {
                int teeth = kind == BoardMarker.Kind.MINEFIELD ? 8 : 12;
                for (int index = 0; index < teeth; index++) {
                    double angle = index * Math.PI * 2 / teeth;
                    g.drawLine(48 + (int) (18 * Math.cos(angle)), 48 + (int) (18 * Math.sin(angle)),
                          48 + (int) (29 * Math.cos(angle)), 48 + (int) (29 * Math.sin(angle)));
                }
                g.fillOval(27, 27, 42, 42);
                g.setColor(java.awt.Color.WHITE);
                if (kind == BoardMarker.Kind.SAW_CLEARING) {
                    g.fillOval(41, 41, 14, 14);
                } else {
                    g.fillOval(37, 35, 8, 8);
                }
            }
            case DEMOLITION_CHARGE -> {
                g.fillOval(27, 34, 42, 42);
                g.drawPolyline(new int[] { 48, 48, 63, 70 }, new int[] { 35, 24, 24, 16 }, 4);
                g.drawLine(65, 14, 76, 20);
            }
            case ARTILLERY_TARGET, ARTILLERY_ADJUSTED, ARTILLERY_AUTO_HIT -> {
                g.drawOval(27, 27, 42, 42);
                g.drawLine(18, 48, 34, 48);
                g.drawLine(62, 48, 78, 48);
                g.drawLine(48, 18, 48, 34);
                g.drawLine(48, 62, 48, 78);
                if (kind == BoardMarker.Kind.ARTILLERY_AUTO_HIT) {
                    g.drawPolyline(new int[] { 37, 45, 59 }, new int[] { 48, 56, 40 }, 3);
                } else if (kind == BoardMarker.Kind.ARTILLERY_ADJUSTED) {
                    g.fillOval(51, 34, 10, 10);
                } else {
                    g.fillOval(44, 44, 8, 8);
                }
            }
            case ARTILLERY_INCOMING, ORBITAL_INCOMING -> {
                g.drawOval(29, 45, 38, 27);
                g.drawLine(48, 18, 48, 52);
                g.drawPolyline(new int[] { 36, 48, 60 }, new int[] { 39, 52, 39 }, 3);
                if (kind == BoardMarker.Kind.ORBITAL_INCOMING) {
                    g.drawArc(18, 40, 60, 38, 185, 165);
                    g.fillOval(68, 32, 9, 9);
                }
            }
            case NUKE_INCOMING -> {
                for (int angle = 30; angle < 360; angle += 120) {
                    g.fillArc(19, 19, 58, 58, angle, 60);
                }
                g.setColor(java.awt.Color.WHITE);
                g.fillOval(37, 37, 22, 22);
                g.setColor(new java.awt.Color(0x232323));
                g.fillOval(42, 42, 12, 12);
            }
            case PLAYER_NOTE -> {
                g.drawRoundRect(28, 21, 40, 54, 4, 4);
                for (int y = 34; y < 68; y += 11) {
                    g.drawLine(37, y, 59, y);
                }
            }
            case CARGO -> {
                g.drawPolygon(new int[] { 48, 72, 72, 48, 24, 24 }, new int[] { 20, 33, 63, 77, 63, 33 }, 6);
                g.drawPolyline(new int[] { 24, 48, 72 }, new int[] { 33, 47, 33 }, 3);
                g.drawLine(48, 47, 48, 77);
                g.drawLine(36, 27, 60, 40);
            }
            case FLARE -> {
                g.fillOval(38, 38, 20, 20);
                for (int angle = 0; angle < 360; angle += 45) {
                    g.drawLine(48 + (int) (19 * MathUtils.cosDeg(angle)), 48 + (int) (19 * MathUtils.sinDeg(angle)),
                          48 + (int) (30 * MathUtils.cosDeg(angle)), 48 + (int) (30 * MathUtils.sinDeg(angle)));
                }
            }
            case BRIDGE_BUILD, BRIDGE_REPAIRED -> {
                g.drawLine(21, 48, 75, 48);
                g.drawLine(27, 40, 27, 70);
                g.drawLine(69, 40, 69, 70);
                g.drawArc(28, 30, 40, 32, 0, 180);
                if (kind == BoardMarker.Kind.BRIDGE_REPAIRED) {
                    g.drawPolyline(new int[] { 40, 48, 61 }, new int[] { 65, 73, 58 }, 3);
                } else {
                    g.drawLine(48, 55, 48, 72);
                    g.drawLine(39, 63, 57, 63);
                }
            }
            case FORTIFY_BUILD -> {
                g.drawRoundRect(22, 51, 26, 17, 6, 6);
                g.drawRoundRect(48, 51, 26, 17, 6, 6);
                g.drawRoundRect(35, 32, 26, 17, 6, 6);
                g.drawLine(20, 75, 76, 75);
            }
            case RUBBLE_CLEAR, DUG_IN -> {
                g.drawLine(58, 23, 38, 60);
                g.drawLine(49, 19, 66, 29);
                g.fillPolygon(new int[] { 28, 46, 43, 30 }, new int[] { 52, 62, 74, 73 }, 4);
                if (kind == BoardMarker.Kind.RUBBLE_CLEAR) {
                    g.drawPolyline(new int[] { 48, 55, 63, 70, 78 }, new int[] { 75, 58, 66, 54, 75 }, 5);
                } else {
                    g.drawPolyline(new int[] { 20, 26, 26, 70, 70, 78 }, new int[] { 53, 53, 80, 80, 53, 53 }, 6);
                }
            }
            default -> throw new IllegalArgumentException("No coin symbol for " + kind);
        }
    }

    private static void glyph(Graphics2D graphics, String text, Font font, float size) {
        Shape shape = font.createGlyphVector(graphics.getFontRenderContext(), text).getOutline();
        var bounds = shape.getBounds2D();
        double scale = size / Math.max(bounds.getWidth(), bounds.getHeight());
        AffineTransform transform = AffineTransform.getTranslateInstance(ART_SIZE / 2.0, ART_SIZE / 2.0);
        transform.scale(scale, scale);
        transform.translate(-bounds.getCenterX(), -bounds.getCenterY());
        graphics.fill(transform.createTransformedShape(shape));
    }

    @Override
    public void dispose() {
        locations.clear();
        labelAnchors.clear();
        labelImages.clear();
        labelTextures.dispose();
        models.values().forEach(GpuUnitModel::dispose);
        models.clear();
        textures.dispose();
    }
}

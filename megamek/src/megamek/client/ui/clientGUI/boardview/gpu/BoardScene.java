/* Copyright (C) 2026 The MegaMek Team. SPDX-License-Identifier: GPL-3.0-or-later */
package megamek.client.ui.clientGUI.boardview.gpu;

import java.awt.image.BufferedImage;
import java.awt.image.DataBufferInt;
import java.awt.image.Raster;
import java.awt.image.SinglePixelPackedSampleModel;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import megamek.client.ui.clientGUI.boardview.BoardView;
import megamek.common.board.Coords;
import megamek.common.units.EntityMovementType;

/** A presentation snapshot. Only the Swing thread reads the game; the GPU thread owns rendering. */
record BoardScene(int boardId, int width, int height, List<Tile> tiles, List<Unit> units,
      List<Waypoint> plannedPath, int selectedId, String phase, List<Command> commands, Light light,
      List<FiringLine> firingLines, List<RangeBorder> rangeBorders) {

    BoardScene(int boardId, int width, int height, List<Tile> tiles, List<Unit> units,
          List<Waypoint> plannedPath, int selectedId, String phase, List<Command> commands, Light light) {
        this(boardId, width, height, tiles, units, plannedPath, selectedId, phase, commands, light, List.of(), List.of());
    }

    BoardScene(int boardId, int width, int height, List<Tile> tiles, List<Unit> units,
          List<Waypoint> plannedPath, int selectedId, String phase, List<Command> commands) {
        this(boardId, width, height, tiles, units, plannedPath, selectedId, phase, commands, null);
    }

    /** World-space shadow travel per elevation level; null means directional shadows are disabled. */
    public record Light(float x, float y) { }

    public BoardScene {
        if (width < 1 || height < 1 || tiles.size() != width * height) {
            throw new IllegalArgumentException("A board snapshot must contain every hex in column order");
        }
        tiles = List.copyOf(tiles);
        units = List.copyOf(units);
        plannedPath = List.copyOf(plannedPath);
        commands = List.copyOf(commands);
        firingLines = List.copyOf(firingLines);
        rangeBorders = List.copyOf(rangeBorders);
    }

    /** Absolute endpoint levels and displayed attack modes, copied from the existing visible attack sprites. */
    record FiringLine(Waypoint source, Waypoint target, int rgb, boolean indirect) { }

    /** The weapon handler already determines these edges, brackets and colours. No range rules live in the renderer. */
    record RangeBorder(Coords coords, int edges, int rgb) { }

    public Tile tile(Coords coords) {
        return coords.getX() < 0 || coords.getY() < 0 || coords.getX() >= width || coords.getY() >= height
              ? null : tiles.get(coords.getX() * height + coords.getY());
    }

    /** A material family determines the exposed geology and the overhanging surface cover. */
    enum Surface {
        GRASS("terrain/dirt", "grass-rim"), DIRT("terrain/dirt", "dirt-rim"), SAND("terrain/sand", "sand-rim"),
        ROCK("terrain/rock", "rock-rim"), CONCRETE("terrain/concrete", "concrete-rim"), SNOW("terrain/rock", "snow-rim");

        final String wall;
        final String rim;

        Surface(String wall, String rim) {
            this.wall = wall;
            this.rim = rim;
        }
    }

    enum FeatureKind { PROP, BUILDING, TREE }

    /** Shared authored model, placement in tile pixels, and height/type copied from game terrain. */
    record Feature(String asset, float x, float y, float rotation, float scale, float height, float elevation,
          FeatureKind kind) {
        Feature(String asset, float x, float y, float rotation, float scale, float height, float elevation) {
            this(asset, x, y, rotation, scale, height, elevation, FeatureKind.PROP);
        }
    }

    /** Water depth -1 means dry. Ground and decals are independent from solid feature geometry. */
    record Tile(Coords coords, int elevation, int waterDepth, boolean frozen, int roadExits, Surface surface, Pixels ground, Pixels decals,
          Pixels tactical, List<Feature> features, List<BoardView.HexText> text) {
        Tile {
            features = List.copyOf(features);
            text = List.copyOf(text);
        }

        boolean water() {
            return waterDepth >= 0;
        }
    }

    /** Stand/flight elevation and occupied levels come from the game, including the unit's current stance. */
    public record Unit(int id, int part, String name, Waypoint location, Pixels image, boolean sensorContact,
          Pixels annotations, int height, boolean airborne, UnitModel model, int outlineRgb) {
        Unit(int id, int part, String name, Waypoint location, Pixels image, boolean sensorContact,
              Pixels annotations, int height, boolean airborne) {
            this(id, part, name, location, image, sensorContact, annotations, height, airborne, null, 0xFFC0C0C0);
        }
    }

    /**
     * Derived on Swing after visibility filtering; the render thread never reads an Entity.
     *
     * @param asset    the descriptor chosen for the unit, relative to the model directory
     * @param fallback the generic descriptor used when {@code asset} cannot be loaded, or {@code null}
     * @param variant  the key of the unit's loadout or formation inside the descriptor
     * @param figures  the number of figures a formation shows
     * @param twist    hexsides the displayed facing (a Mek's torso) is turned clockwise from the unit's own facing
     *                 (its legs), from {@code -2} to {@code 3}; {@code 0} when the two agree
     * @param damage   the locations to show as lost or destroyed
     */
    record UnitModel(String asset, String fallback, String variant, int figures, int twist, LocationDamage damage) {
        UnitModel(String asset, String fallback, String variant, int figures) {
            this(asset, fallback, variant, figures, 0, LocationDamage.NONE);
        }
    }

    /**
     * The locations of a unit that a model shows as lost, each by the game's own abbreviation ({@code LA},
     * {@code RT}, ...), which is also the name of that location's part in the model.
     *
     * @param removed locations shown as gone altogether: a lost arm
     * @param wrecked locations shown still in place but burnt out: a destroyed leg, side torso or head, which the
     *                rest of the model stands on or hangs from
     */
    record LocationDamage(Set<String> removed, Set<String> wrecked) {
        static final LocationDamage NONE = new LocationDamage(Set.of(), Set.of());

        LocationDamage {
            removed = Set.copyOf(removed);
            wrecked = Set.copyOf(wrecked);
        }

        boolean isNone() {
            return removed.isEmpty() && wrecked.isEmpty();
        }
    }

    public record Waypoint(Coords coords, float elevation, float facing) { }

    /** The action marshals back to Swing and rechecks the original button before invoking it. */
    public record Command(String id, String label, String detail, boolean enabled, boolean commit, boolean boardTool,
          List<Command> children, Runnable action) {
        public Command {
            children = List.copyOf(children);
        }

        public Command(String label, boolean enabled, Runnable action) {
            this(label, label, "", enabled, false, false, List.of(), action);
        }

        public Command(String id, String label, String detail, boolean enabled, boolean commit,
              List<Command> children, Runnable action) {
            this(id, label, detail, enabled, commit, false, children, action);
        }
    }

    public record Context(Coords coords, List<Command> commands) {
        public Context {
            commands = List.copyOf(commands);
        }
    }

    /** Swing owns attack selection and calculations; the GL thread receives only their presentation. */
    public record Attack(String targetName, String weaponDetails, String targetDetails, int selectedWeapon,
          List<String> orders) {
        public Attack {
            orders = List.copyOf(orders);
        }
    }

    public record Movement(int entityId, int boardId, List<Waypoint> path, EntityMovementType type, int jumpMP) {
        public Movement {
            path = List.copyOf(path);
        }
    }

    /** Immutable pixel ownership avoids accessing AWT images or the tileset from the GL thread. */
    public static final class Pixels {
        private final int width;
        private final int height;
        private final int[] argb;
        private final int hash;

        public Pixels(BufferedImage image) {
            this(image.getWidth(), image.getHeight(), read(image, null));
        }

        private static int[] read(BufferedImage image, int[] target) {
            int width = image.getWidth();
            int height = image.getHeight();
            if (image.getType() != BufferedImage.TYPE_INT_ARGB) {
                return image.getRGB(0, 0, width, height, target, 0, width);
            }
            int[] result = target == null ? new int[width * height] : target;
            Raster raster = image.getRaster();
            DataBufferInt data = (DataBufferInt) raster.getDataBuffer();
            SinglePixelPackedSampleModel sample = (SinglePixelPackedSampleModel) raster.getSampleModel();
            int offset = data.getOffset() + sample.getOffset(raster.getMinX() - raster.getSampleModelTranslateX(),
                  raster.getMinY() - raster.getSampleModelTranslateY());
            int[] source = data.getData();
            for (int row = 0; row < height; row++) {
                System.arraycopy(source, offset + row * sample.getScanlineStride(), result, row * width, width);
            }
            return result;
        }

        private Pixels(int width, int height, int[] argb) {
            this.width = width;
            this.height = height;
            this.argb = argb;
            hash = 31 * (31 * width + height) + Arrays.hashCode(argb);
        }

        public static Pixels capture(BufferedImage image, Pixels previous) {
            if (image == null) {
                return null;
            }
            Pixels next = new Pixels(image);
            return next.equals(previous) ? previous : next;
        }

        @Override
        public int hashCode() {
            return hash;
        }

        @Override
        public boolean equals(Object other) {
            return this == other || other instanceof Pixels pixels && width == pixels.width && height == pixels.height
                  && hash == pixels.hash && Arrays.equals(argb, pixels.argb);
        }

        public int width() {
            return width;
        }

        public int height() {
            return height;
        }

        public int rgba(int index) {
            return (argb[index] << 8) | ((argb[index] >>> 24) & 0xff);
        }
    }

    /** Exact sharing, owned by the Swing source. Only artwork still referenced by the current scene is retained. */
    static final class PixelPool {
        private final Map<Pixels, Pixels> images = new HashMap<>();
        private final Map<Integer, int[]> buffers = new HashMap<>();

        Pixels capture(BufferedImage image, Pixels previous) {
            if (image == null) {
                return null;
            }
            int[] buffer = buffers.computeIfAbsent(image.getWidth() * image.getHeight(), size -> new int[size]);
            Pixels.read(image, buffer);
            Pixels lookup = new Pixels(image.getWidth(), image.getHeight(), buffer);
            if (lookup.equals(previous)) {
                return previous;
            }
            Pixels shared = images.get(lookup);
            if (shared != null) {
                return shared;
            }
            Pixels owned = new Pixels(lookup.width, lookup.height, buffer.clone());
            images.put(owned, owned);
            return owned;
        }

        Pixels captureOverlay(BufferedImage image, Pixels previous) {
            Pixels pixels = capture(image, previous);
            return pixels == null || Arrays.stream(pixels.argb).allMatch(pixel -> (pixel >>> 24) == 0) ? null : pixels;
        }

        void retain(List<Tile> tiles) {
            Set<Pixels> used = new HashSet<>();
            for (Tile tile : tiles) {
                used.add(tile.ground());
                used.add(tile.decals());
                used.add(tile.tactical());
            }
            images.keySet().retainAll(used);
        }

        void clear() {
            images.clear();
            buffers.clear();
        }
    }
}

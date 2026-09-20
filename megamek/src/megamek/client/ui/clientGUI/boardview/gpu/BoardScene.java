/* Copyright (C) 2026 The MegaMek Team. SPDX-License-Identifier: GPL-3.0-or-later */
package megamek.client.ui.clientGUI.boardview.gpu;

import java.awt.Image;
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
import java.util.stream.Stream;
import javax.swing.ImageIcon;

import megamek.client.ui.clientGUI.boardview.BoardFieldOfView;
import megamek.client.ui.clientGUI.boardview.BoardMarker;
import megamek.client.ui.clientGUI.boardview.BoardTactical;
import megamek.client.ui.clientGUI.boardview.BoardView;
import megamek.common.board.Coords;
import megamek.common.units.EntityMovementType;

/** A presentation snapshot. Only the Swing thread reads the game; the GPU thread owns rendering. */
record BoardScene(int boardId, int width, int height, List<Tile> tiles, List<Unit> units,
      List<Waypoint> plannedPath, int selectedId, String phase, List<Command> commands, Light light,
      List<FiringLine> firingLines, List<RangeBorder> rangeBorders, List<BoardMarker> markers, BoardTactical tactical,
      List<RangeLabel> rangeLabels, BoardFieldOfView fieldOfView) {

    BoardScene(int boardId, int width, int height, List<Tile> tiles, List<Unit> units,
          List<Waypoint> plannedPath, int selectedId, String phase, List<Command> commands, Light light,
          List<FiringLine> firingLines, List<RangeBorder> rangeBorders, List<BoardMarker> markers, BoardTactical tactical,
          List<RangeLabel> rangeLabels) {
        this(boardId, width, height, tiles, units, plannedPath, selectedId, phase, commands, light, firingLines,
              rangeBorders, markers, tactical, rangeLabels, BoardFieldOfView.EMPTY);
    }

    BoardScene(int boardId, int width, int height, List<Tile> tiles, List<Unit> units,
          List<Waypoint> plannedPath, int selectedId, String phase, List<Command> commands, Light light,
          List<FiringLine> firingLines, List<RangeBorder> rangeBorders, List<BoardMarker> markers, BoardTactical tactical) {
        this(boardId, width, height, tiles, units, plannedPath, selectedId, phase, commands, light, firingLines,
              rangeBorders, markers, tactical, List.of());
    }

    BoardScene(int boardId, int width, int height, List<Tile> tiles, List<Unit> units,
          List<Waypoint> plannedPath, int selectedId, String phase, List<Command> commands, Light light,
          List<FiringLine> firingLines, List<RangeBorder> rangeBorders, List<BoardMarker> markers) {
        this(boardId, width, height, tiles, units, plannedPath, selectedId, phase, commands, light, firingLines,
              rangeBorders, markers, BoardTactical.EMPTY);
    }

    BoardScene(int boardId, int width, int height, List<Tile> tiles, List<Unit> units,
          List<Waypoint> plannedPath, int selectedId, String phase, List<Command> commands, Light light,
          List<FiringLine> firingLines, List<RangeBorder> rangeBorders) {
        this(boardId, width, height, tiles, units, plannedPath, selectedId, phase, commands, light, firingLines,
              rangeBorders, List.of());
    }

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
        rangeLabels = List.copyOf(rangeLabels);
        markers = List.copyOf(markers);
    }

    /** Absolute endpoint levels and displayed attack modes, copied from the existing visible attack sprites. */
    record FiringLine(Waypoint source, Waypoint target, int rgb, boolean indirect) { }

    /** The weapon handler already determines these edges, brackets and colours. No range rules live in the renderer. */
    record RangeBorder(Coords coords, int edges, int rgb, String label) { }

    /** Flat lettering at positions chosen by the weapon handler, independently oriented toward the camera. */
    record RangeLabel(Coords coords, int rgb, String label) { }

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

    enum FeatureKind { PROP, BUILDING, TREE, LIMB, SCATTER }

    /** Authored model or scatter shape, placement in tile pixels, and height in elevation levels. */
    record Feature(String asset, float x, float y, float rotation, float scale, float height, float elevation,
          FeatureKind kind) {
        Feature(String asset, float x, float y, float rotation, float scale, float height, float elevation) {
            this(asset, x, y, rotation, scale, height, elevation, FeatureKind.PROP);
        }
    }

    /** Water depth -1 means dry. Ground and decals are independent from solid feature geometry. */
    record Tile(Coords coords, int elevation, int waterDepth, boolean frozen, int roadExits, Surface surface, Pixels ground,
          Pixels normals, Pixels decals, Pixels decalsWithoutLimbs,
          Pixels tactical, List<Feature> features, List<BoardView.HexText> text) {
        Tile(Coords coords, int elevation, int waterDepth, boolean frozen, int roadExits, Surface surface, Pixels ground,
              Pixels normals, Pixels decals, Pixels tactical, List<Feature> features, List<BoardView.HexText> text) {
            this(coords, elevation, waterDepth, frozen, roadExits, surface, ground, normals, decals, null, tactical, features, text);
        }

        Tile(Coords coords, int elevation, int waterDepth, boolean frozen, int roadExits, Surface surface, Pixels ground,
              Pixels decals, Pixels tactical, List<Feature> features, List<BoardView.HexText> text) {
            this(coords, elevation, waterDepth, frozen, roadExits, surface, ground, null, decals, tactical, features, text);
        }

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
          Pixels annotations, int height, boolean airborne, UnitModel model, int outlineRgb, List<Coords> footprint) {
        public Unit {
            footprint = List.copyOf(footprint);
        }

        Unit(int id, int part, String name, Waypoint location, Pixels image, boolean sensorContact,
              Pixels annotations, int height, boolean airborne, UnitModel model, int outlineRgb) {
            this(id, part, name, location, image, sensorContact, annotations, height, airborne, model, outlineRgb,
                  List.of(location.coords()));
        }
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
     * @param state    immutable live components and posture, or {@code null} for legacy review fixtures
     */
    record UnitModel(String asset, String fallback, String variant, int figures, int twist, LocationDamage damage,
          UnitModelState state) {
        UnitModel(String asset, String fallback, String variant, int figures, int twist, LocationDamage damage) {
            this(asset, fallback, variant, figures, twist, damage, null);
        }

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

    /** Observed aerospace state, kept separate from the absolute height used for drawing. */
    public enum AeroState { LANDED, ELEVATED, AIRBORNE }

    public record Waypoint(Coords coords, float elevation, float facing, megamek.common.units.ProneCause proneCause,
          AeroState aeroState, List<Coords> footprint) {
        public Waypoint {
            footprint = List.copyOf(footprint);
        }

        public Waypoint(Coords coords, float elevation, float facing, megamek.common.units.ProneCause proneCause,
              AeroState aeroState) {
            this(coords, elevation, facing, proneCause, aeroState, List.of());
        }
        public Waypoint(Coords coords, float elevation, float facing) {
            this(coords, elevation, facing, null, null);
        }

        public Waypoint(Coords coords, float elevation, float facing, megamek.common.units.ProneCause proneCause) {
            this(coords, elevation, facing, proneCause, null);
        }

        Waypoint withProneCause(megamek.common.units.ProneCause cause) {
            return new Waypoint(coords, elevation, facing, cause, aeroState, footprint);
        }

        Waypoint withAeroState(AeroState state) {
            return new Waypoint(coords, elevation, facing, proneCause, state, footprint);
        }

        Waypoint withFootprint(List<Coords> occupied) {
            return new Waypoint(coords, elevation, facing, proneCause, aeroState, occupied);
        }

        /** Captured fitting metadata does not create another movement step at a queued path boundary. */
        boolean samePose(Waypoint other) {
            return coords.equals(other.coords) && elevation == other.elevation && facing == other.facing
                  && proneCause == other.proneCause && aeroState == other.aeroState;
        }
    }

    BoardScene withUnits(List<Unit> shown) {
        return new BoardScene(boardId, width, height, tiles, shown, plannedPath, selectedId, phase, commands, light,
              firingLines, rangeBorders, markers, tactical, rangeLabels, fieldOfView);
    }

    /** Board artwork, terrain and authorized contacts advance with the queue; interactive tools stay live. */
    BoardScene duringPlayback(BoardScene settled, boolean hideMovement) {
        BoardScene world = settled == null ? this : settled;
        var heldMarkers = settled == null ? Stream.<BoardMarker>empty() : settled.markers.stream();
        var shownMarkers = Stream.concat(heldMarkers.filter(marker ->
                    (!hideMovement || marker.kind() != BoardMarker.Kind.COLLAPSE_WARNING)
                          && marker.kind() != BoardMarker.Kind.PLAYER_NOTE),
              markers.stream().filter(marker -> marker.kind() == BoardMarker.Kind.PLAYER_NOTE)).toList();
        return new BoardScene(boardId, width, height, world.tiles, world.units,
              hideMovement ? List.of() : world.plannedPath, selectedId, phase, commands, light,
              hideMovement ? List.of() : world.firingLines, hideMovement ? List.of() : world.rangeBorders, shownMarkers,
              tactical.duringPlayback(settled == null ? BoardTactical.EMPTY : settled.tactical, hideMovement),
              hideMovement ? List.of() : world.rangeLabels, hideMovement ? BoardFieldOfView.EMPTY : world.fieldOfView);
    }

    /** Excludes HUD commands and other live controls, which do not need a playback checkpoint. */
    boolean samePlaybackState(BoardScene other) {
        return other != null && boardId == other.boardId && width == other.width && height == other.height
              && tiles.equals(other.tiles) && units.equals(other.units) && markers.equals(other.markers)
              && tactical.equals(other.tactical) && plannedPath.equals(other.plannedPath)
              && firingLines.equals(other.firingLines) && rangeBorders.equals(other.rangeBorders)
              && rangeLabels.equals(other.rangeLabels) && fieldOfView.equals(other.fieldOfView);
    }

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

    interface Animation {
        int entityId();
        int boardId();
    }

    /** An immutable, visibility-filtered checkpoint in packet order; it consumes no animation time. */
    record SceneUpdate(BoardScene scene) implements Animation {
        @Override
        public int entityId() { return -1; }

        @Override
        public int boardId() { return scene.boardId(); }
    }

    record Combat(megamek.common.ResolvedAttack result, Unit attacker, Unit target, Waypoint destination) implements Animation {
        @Override
        public int entityId() { return attacker.id(); }

        @Override
        public int boardId() { return result.attacker().boardId(); }
    }

    public record Movement(int entityId, int boardId, List<Waypoint> path, EntityMovementType type, int jumpMP, int movementMP,
          Unit unit) implements Animation {
        public Movement {
            path = List.copyOf(path);
        }

        public Movement(int entityId, int boardId, List<Waypoint> path, EntityMovementType type, int jumpMP, int movementMP) {
            this(entityId, boardId, path, type, jumpMP, movementMP, null);
        }

        public Movement(int entityId, int boardId, List<Waypoint> path, EntityMovementType type, int jumpMP) {
            this(entityId, boardId, path, type, jumpMP, 0);
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

        static Pixels copy(Image source) {
            ImageIcon loaded = new ImageIcon(source);
            BufferedImage copy = new BufferedImage(Math.max(1, loaded.getIconWidth()), Math.max(1, loaded.getIconHeight()),
                  BufferedImage.TYPE_INT_ARGB);
            var graphics = copy.createGraphics();
            try {
                graphics.drawImage(loaded.getImage(), 0, 0, null);
            } finally {
                graphics.dispose();
            }
            return new Pixels(copy);
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
                used.add(tile.normals());
                used.add(tile.decals());
                used.add(tile.decalsWithoutLimbs());
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

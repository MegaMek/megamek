/* Copyright (C) 2026 The MegaMek Team. SPDX-License-Identifier: GPL-3.0-or-later */
package megamek.client.ui.clientGUI.boardview.gpu;

import java.awt.image.BufferedImage;
import java.util.Arrays;
import java.util.List;

import megamek.client.ui.clientGUI.boardview.BoardView;
import megamek.common.board.Coords;
import megamek.common.units.EntityMovementType;

/** A presentation snapshot. Only the Swing thread reads the game; the GPU thread owns rendering. */
record BoardScene(int boardId, int width, int height, List<Tile> tiles, List<Unit> units,
      List<Waypoint> plannedPath, int selectedId, String phase, List<Command> commands, Light light) {

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
    }

    public Tile tile(Coords coords) {
        return coords.getX() < 0 || coords.getY() < 0 || coords.getX() >= width || coords.getY() >= height
              ? null : tiles.get(coords.getX() * height + coords.getY());
    }

    /**
     * {@code base} is the ground artwork the padding continues around the hex; {@code water} marks a water
     * surface, so a water side joins with water and its land sides with a bank; {@code features} holds the
     * tileset's other terrain layers on transparent pixels, drawn over hex and padding alike.
     */
    public record Tile(Coords coords, int elevation, Pixels image, Pixels base, boolean water, Pixels features,
          Pixels tactical, List<BoardView.HexText> text) {
        public Tile {
            text = List.copyOf(text);
        }

        public Tile(Coords coords, int elevation, Pixels image, Pixels base, boolean water) {
            this(coords, elevation, image, base, water, null, null, List.of());
        }

        public Tile(Coords coords, int elevation, Pixels image, Pixels base, Pixels tactical,
              List<BoardView.HexText> text) {
            this(coords, elevation, image, base, false, null, tactical, text);
        }

        public Tile(Coords coords, int elevation, Pixels image, Pixels base, Pixels tactical) {
            this(coords, elevation, image, base, false, null, tactical, List.of());
        }

        public Tile(Coords coords, int elevation, Pixels image, Pixels tactical, List<BoardView.HexText> text) {
            this(coords, elevation, image, image, false, null, tactical, text);
        }

        public Tile(Coords coords, int elevation, Pixels image, Pixels tactical) {
            this(coords, elevation, image, image, false, null, tactical, List.of());
        }

        public Tile(Coords coords, int elevation, Pixels image) {
            this(coords, elevation, image, image, false, null, null, List.of());
        }
    }

        /**
         * {@code location} is the meeple's stand or flight height, {@code height} its own occupied levels and
         * {@code airborne} true while it floats at that flight height instead of standing on the tile.
         */
        public record Unit(int id, int part, String name, Waypoint location, Pixels image, boolean sensorContact,
                    Pixels annotations, int height, boolean airborne) { }

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

        public Pixels(BufferedImage image) {
            width = image.getWidth();
            height = image.getHeight();
            argb = image.getRGB(0, 0, width, height, null, 0, width);
        }

        public static Pixels capture(BufferedImage image, Pixels previous) {
            Pixels next = new Pixels(image);
            return previous != null && next.width == previous.width && next.height == previous.height
                  && Arrays.equals(next.argb, previous.argb) ? previous : next;
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
}

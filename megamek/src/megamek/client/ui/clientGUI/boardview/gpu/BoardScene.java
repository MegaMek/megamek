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

    /** A material family determines the exposed geology and the overhanging surface cover. */
    enum Surface {
        GRASS("dirt"), DIRT("dirt"), SAND("sand"), ROCK("rock"), CONCRETE("concrete"), SNOW("rock");

        final String wall;

        Surface(String wall) {
            this.wall = wall;
        }
    }

    /** Shared authored model, placement in tile pixels, and height in game levels. */
    record Feature(String asset, float x, float y, float rotation, float scale, float height, float elevation) { }

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
            if (image == null) {
                return null;
            }
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

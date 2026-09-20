/* Copyright (C) 2026 The MegaMek Team. SPDX-License-Identifier: GPL-3.0-or-later */
package megamek.client.ui.clientGUI.boardview;

import java.awt.Font;
import java.awt.geom.Path2D;
import java.util.List;
import java.util.stream.Stream;

/** Immutable drawing commands in unscaled board pixels; game state remains on the Swing thread. */
public record BoardTactical(List<Fill> fills, List<Label> labels) {
    public static final BoardTactical EMPTY = new BoardTactical(List.of(), List.of());

    /** Measurement tools stay live, unit overlays hide, and map-state markings retain their last displayed state. */
    public enum Playback { LIVE, HIDE_DURING_MOVEMENT, HOLD_DURING_PLAYBACK }

    public BoardTactical {
        fills = List.copyOf(fills);
        labels = List.copyOf(labels);
    }

    public BoardTactical duringPlayback(BoardTactical settled, boolean hideMovement) {
        // Keep live measurements above retained map markings.
        return new BoardTactical(Stream.concat(
              settled.fills.stream().filter(fill -> retained(fill.playback(), hideMovement)),
              fills.stream().filter(fill -> fill.playback() == Playback.LIVE)).toList(),
              Stream.concat(settled.labels.stream().filter(label -> retained(label.playback(), hideMovement)),
                    labels.stream().filter(label -> label.playback() == Playback.LIVE)).toList());
    }

    private static boolean retained(Playback playback, boolean hideMovement) {
        return playback != Playback.LIVE && (!hideMovement || playback == Playback.HOLD_DURING_PLAYBACK);
    }

    public record Point(float x, float y) { }

    public record Contour(List<Point> points) {
        public Contour {
            points = List.copyOf(points);
        }
    }

    public record Fill(List<Contour> contours, int winding, int argb, Playback playback) {
        public Fill(List<Contour> contours, int winding, int argb) {
            this(contours, winding, argb, Playback.LIVE);
        }

        public Fill {
            contours = List.copyOf(contours);
        }

        public Path2D shape() {
            Path2D result = new Path2D.Float(winding);
            for (Contour contour : contours) {
                if (contour.points().isEmpty()) {
                    continue;
                }
                Point first = contour.points().getFirst();
                result.moveTo(first.x(), first.y());
                for (int i = 1; i < contour.points().size(); i++) {
                    Point point = contour.points().get(i);
                    result.lineTo(point.x(), point.y());
                }
                result.closePath();
            }
            return result;
        }
    }

    /** Position relative to a shared board anchor, so related text/shadows stay together as the camera turns. */
    public record Text(String value, Font font, float x, float y, int argb) { }
    public record Label(Point anchor, Text text, Playback playback) {
        public Label(Point anchor, Text text) {
            this(anchor, text, Playback.LIVE);
        }
    }
}

/* Copyright (C) 2026 The MegaMek Team. SPDX-License-Identifier: GPL-3.0-or-later */
package megamek.client.ui.clientGUI.boardview;

import java.util.List;

/** Immutable presentation of the shared LOS/sensor calculation, in column order. */
public record BoardFieldOfView(int width, int height, List<Hex> hexes, int darkenAlpha, int highlightAlpha,
      boolean darken, boolean grayscale, boolean spotting) {
    public static final BoardFieldOfView EMPTY = new BoardFieldOfView(0, 0, List.of(), 0, 0, false, false, false);

    public enum Visibility { NONE, VISIBLE, ORIGIN, SENSOR, BLOCKED }

    /** Tint is the existing painter's ARGB color, before any native presentation style is applied. */
    public record Hex(Visibility visibility, int tint, boolean outsideSensorRange) {
        public static final Hex NONE = new Hex(Visibility.NONE, 0);
        public static final Hex VISIBLE = new Hex(Visibility.VISIBLE, 0);

        /** LOS obstruction alone does not establish that a hex is outside a known sensor range. */
        public Hex(Visibility visibility, int tint) {
            this(visibility, tint, false);
        }

        public boolean hasLineOfSight() {
            return visibility != Visibility.SENSOR && visibility != Visibility.BLOCKED;
        }
    }

    public BoardFieldOfView {
        hexes = List.copyOf(hexes);
        if (width < 0 || height < 0 || hexes.size() != width * height) {
            throw new IllegalArgumentException("Field of view must contain one result per hex");
        }
    }

    public boolean active() {
        return hexes.stream().anyMatch(hex -> hex.visibility() != Visibility.NONE);
    }
}

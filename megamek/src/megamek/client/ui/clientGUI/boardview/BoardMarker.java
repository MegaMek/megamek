/* Copyright (C) 2026 The MegaMek Team. SPDX-License-Identifier: GPL-3.0-or-later */
package megamek.client.ui.clientGUI.boardview;

import megamek.common.board.Coords;

/** Immutable presentation of an already-visible point marker. Elevation is its absolute support height. */
public record BoardMarker(Kind kind, Coords coords, float elevation, int rgb, String label) {
    public enum Kind {
        SENSOR_CONTACT(0xE63D3D), COLLAPSE_WARNING(0xFFCC42), MINEFIELD(0xF0BB42),
        DEMOLITION_CHARGE(0xEF7750), ARTILLERY_INCOMING(0xF36B58), ARTILLERY_TARGET(0xEAA654),
        ARTILLERY_ADJUSTED(0x6CC5F2), ARTILLERY_AUTO_HIT(0x85DA85), ORBITAL_INCOMING(0xCE8EF2),
        NUKE_INCOMING(0xF7D54B), OBJECTIVE(0x81B9FA), PLAYER_NOTE(0xEFE5B1), CARGO(0xCDA471),
        FLARE(0xFFF1A0), SAW_CLEARING(0xBCC8D5), BRIDGE_REPAIRED(0xE19623),
        BRIDGE_BUILD(0xE7B862), FORTIFY_BUILD(0xD2BC81), RUBBLE_CLEAR(0xBDB1A3), DUG_IN(0xB1C18D);

        private final int rgb;

        Kind(int rgb) {
            this.rgb = rgb;
        }

        public int rgb() {
            return rgb;
        }
    }

    public BoardMarker {
        label = label == null ? "" : label;
        rgb &= 0xFFFFFF;
    }

    public BoardMarker(Kind kind, Coords coords, float elevation) {
        this(kind, coords, elevation, kind.rgb(), "");
    }
}

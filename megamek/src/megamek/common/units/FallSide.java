/* Copyright (C) 2026 The MegaMek Team. SPDX-License-Identifier: GPL-3.0-or-later */
package megamek.common.units;

/** Last resolved fall's impact side. Presentation metadata; it changes no posture, facing or damage rule. */
public enum FallSide {
    FRONT, RIGHT, REAR, LEFT;

    public static FallSide fromDirection(int direction) {
        return switch (Math.floorMod(direction, 6)) {
            case 1, 2 -> RIGHT;
            case 3 -> REAR;
            case 4, 5 -> LEFT;
            default -> FRONT;
        };
    }
}

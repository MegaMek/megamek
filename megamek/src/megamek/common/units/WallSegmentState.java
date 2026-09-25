/*
 * Copyright (C) 2026 The MegaMek Team. All Rights Reserved.
 *
 * This file is part of MegaMek.
 *
 * MegaMek is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License (GPL),
 * version 3 or (at your option) any later version,
 * as published by the Free Software Foundation.
 *
 * MegaMek is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty
 * of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU General Public License for more details.
 *
 * A copy of the GPL should have been included with this project;
 * if not, see <https://www.gnu.org/licenses/>.
 *
 * NOTICE: The MegaMek organization is a non-profit group of volunteers
 * creating free software for the BattleTech community.
 *
 * MechWarrior, BattleMech, `Mech and AeroTech are registered trademarks
 * of The Topps Company, Inc. All Rights Reserved.
 *
 * Catalyst Game Labs and the Catalyst Game Labs logo are trademarks of
 * InMediaRes Productions, LLC.
 *
 * MechWarrior Copyright Microsoft Corporation. MegaMek was created under
 * Microsoft's "Game Content Usage Rules"
 * <https://www.xbox.com/en-US/developers/rules> and it is not endorsed by or
 * affiliated with Microsoft.
 */

package megamek.common.units;

import java.io.Serial;
import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

import megamek.common.board.CubeCoords;

/** CF and armor belong to each wall/fence hexside, never to the adjacent hex volume (TO:AR p.114). */
public final class WallSegmentState implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;

    // Arrays contain CF, armor and phase-start CF for six clockwise sides.
    private final Map<CubeCoords, int[][]> hexes = new HashMap<>();

    public WallSegmentState() { }

    public WallSegmentState(WallSegmentState source) {
        source.hexes.forEach((hex, values) -> {
            int[][] copy = new int[6][];
            for (int side = 0; side < 6; side++) {
                copy[side] = values[side] == null ? null : values[side].clone();
            }
            hexes.put(hex, copy);
        });
    }

    public void initialize(AbstractBuildingEntity building) {
        for (CubeCoords hex : building.getInternalBuilding().getOriginalCoordsList()) {
            int[][] sides = hexes.computeIfAbsent(hex, ignored -> new int[6][]);
            for (int side = 0; side < 6; side++) {
                if ((building.getDesign().wallSides(hex) & (1 << side)) != 0 && sides[side] == null) {
                    int cf = building.getInternalBuilding().getCurrentCF(hex);
                    sides[side] = new int[] { cf, building.getInternalBuilding().getArmor(hex), cf };
                }
            }
        }
    }

    public int getCF(CubeCoords hex, int side) { return value(hex, side, 0); }
    public int getArmor(CubeCoords hex, int side) { return value(hex, side, 1); }
    public int getPhaseCF(CubeCoords hex, int side) { return value(hex, side, 2); }

    private int value(CubeCoords hex, int side, int index) {
        int[][] values = hexes.get(hex);
        return values == null || side < 0 || side >= 6 || values[side] == null ? 0 : values[side][index];
    }

    public void setCF(CubeCoords hex, int side, int value) { hexes.get(hex)[side][0] = Math.max(0, value); }
    public void setArmor(CubeCoords hex, int side, int value) { hexes.get(hex)[side][1] = Math.max(0, value); }

    public void startPhase() {
        for (int[][] sides : hexes.values()) {
            for (int[] values : sides) {
                if (values != null) {
                    values[2] = values[0];
                }
            }
        }
    }
}

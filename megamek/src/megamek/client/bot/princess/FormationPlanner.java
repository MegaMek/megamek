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
package megamek.client.bot.princess;

import megamek.common.board.Coords;
import megamek.common.orders.FormationShape;

/**
 * Where each unit of a formation should stand, relative to its leader. The shape is laid out from the leader's hex and
 * turned to the leader's heading; spacing is the number of hexes along each step of the shape.
 *
 * <p>Hex facings run 0-5 clockwise from north. With the leader heading north (0), an Echelon Right steps back along
 * the south-east hex line (2), an Echelon Left along the south-west line (4), a Wedge down both, a Vee up the
 * north-east (1) and north-west (5) lines, and a Column straight back (3). A Line runs across the heading; a hex map
 * has no straight row across, so it alternates between the two hex lines either side of the perpendicular.</p>
 */
final class FormationPlanner {

    private static final int FACING_COUNT = 6;

    private FormationPlanner() {
    }

    /**
     * @param leaderPosition the leader's hex
     * @param heading        the direction the formation faces, 0-5
     * @param shape          the formation's shape
     * @param spacing        hexes between neighbouring slots
     * @param slotIndex      the unit's place, 1 for the unit next to the leader, then 2, 3, ...
     *
     * @return the hex the unit should stand in; it may be off the board or somewhere the unit cannot go
     */
    static Coords idealSlot(Coords leaderPosition, int heading, FormationShape shape, int spacing, int slotIndex) {
        int armStep = (slotIndex + 1) / 2;
        boolean isLeftArm = (slotIndex % 2) == 1;
        return switch (shape) {
            case COLUMN -> leaderPosition.translated(turn(heading, 3), spacing * slotIndex);
            case ECHELON_RIGHT -> leaderPosition.translated(turn(heading, 2), spacing * slotIndex);
            case ECHELON_LEFT -> leaderPosition.translated(turn(heading, 4), spacing * slotIndex);
            case WEDGE -> leaderPosition.translated(turn(heading, isLeftArm ? 4 : 2), spacing * armStep);
            case VEE -> leaderPosition.translated(turn(heading, isLeftArm ? 5 : 1), spacing * armStep);
            case LINE -> lineSlot(leaderPosition, heading, spacing * armStep, isLeftArm);
        };
    }

    /**
     * Steps across the heading, alternating between the two hex lines either side of the perpendicular, so the line
     * stays as level as a hex map allows.
     */
    private static Coords lineSlot(Coords leaderPosition, int heading, int steps, boolean isLeftArm) {
        int firstDirection = turn(heading, isLeftArm ? 5 : 1);
        int secondDirection = turn(heading, isLeftArm ? 4 : 2);
        Coords position = leaderPosition;
        for (int step = 0; step < steps; step++) {
            position = position.translated(((step % 2) == 0) ? firstDirection : secondDirection);
        }
        return position;
    }

    private static int turn(int heading, int hexSides) {
        return (heading + hexSides) % FACING_COUNT;
    }
}

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
 * the south-east hex line (2), an Echelon Left along the south-west line (4), and a Column straight back (3). A Wedge,
 * a Vee and a Line are laid out as a tank platoon forms them, the commander and the second-in-command side by side in
 * the middle with their wingmen outside (HammerGS, 2026-09-27: mirror what the military does). A Line runs across the
 * heading; a hex map has no straight row across, so it alternates between the two hex lines either side of the
 * perpendicular.</p>
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
        return switch (shape) {
            case COLUMN -> leaderPosition.translated(turn(heading, 3), spacing * slotIndex);
            case ECHELON_RIGHT -> leaderPosition.translated(turn(heading, 2), spacing * slotIndex);
            case ECHELON_LEFT -> leaderPosition.translated(turn(heading, 4), spacing * slotIndex);
            case WEDGE -> sectionSlot(leaderPosition, heading, spacing, slotIndex, 4, 2);
            case VEE -> sectionSlot(leaderPosition, heading, spacing, slotIndex, 5, 1);
            case LINE -> lineAbreast(leaderPosition, heading, spacing, slotIndex);
        };
    }

    /**
     * Steps across the heading, alternating between the two hex lines either side of the perpendicular, so the line
     * stays as level as a hex map allows.
     */
    /**
     * A Wedge or Vee as a tank platoon forms it: the commander and the second-in-command side by side in the middle,
     * the second-in-command on the commander's right, each with a wingman out to its own side - behind for a Wedge,
     * ahead for a Vee (FM 3-21.71: "both the platoon leader and platoon sergeant stay in the center of the formation,
     * with their wingmen located to the rear of and outside of them"). Places 1 and 4 are the commander's side, 2 is
     * the second-in-command, 3 and 5 its side; a sixth goes on the commander's side.
     *
     * @param commanderSide     the hexside, counted from the heading, the commander's wingmen step out along
     * @param secondInCommandSide the hexside the second-in-command's wingmen step out along
     */
    private static Coords sectionSlot(Coords leaderPosition, int heading, int spacing, int slotIndex, int commanderSide,
          int secondInCommandSide) {
        Coords secondInCommand = lineSlot(leaderPosition, heading, spacing, false);
        if (slotIndex == 2) {
            return secondInCommand;
        }
        if (slotIndex == 1) {
            return leaderPosition.translated(turn(heading, commanderSide), spacing);
        }
        int wingPlace = slotIndex - 3;
        boolean isSecondInCommandSide = (wingPlace % 2) == 0;
        int steps = isSecondInCommandSide ? ((wingPlace / 2) + 1) : (((wingPlace + 1) / 2) + 1);
        return isSecondInCommandSide
              ? secondInCommand.translated(turn(heading, secondInCommandSide), spacing * steps)
              : leaderPosition.translated(turn(heading, commanderSide), spacing * steps);
    }

    /**
     * A Line as a tank platoon forms it: the commander's wingman on its left, the second-in-command on its right and
     * that one's wingman beyond it (2 1 3 4, left to right); a fifth and sixth extend each end.
     */
    private static Coords lineAbreast(Coords leaderPosition, int heading, int spacing, int slotIndex) {
        return switch (slotIndex) {
            case 1 -> lineSlot(leaderPosition, heading, spacing, true);
            case 2 -> lineSlot(leaderPosition, heading, spacing, false);
            case 3 -> lineSlot(leaderPosition, heading, spacing * 2, false);
            case 4 -> lineSlot(leaderPosition, heading, spacing * 2, true);
            default -> lineSlot(leaderPosition, heading, spacing * (((slotIndex - 1) / 2) + 1), (slotIndex % 2) == 0);
        };
    }

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

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
package megamek.utilities.botorders;

import java.util.List;

/**
 * One line of a bot orders script: at the start of the given round, apply an action to the selected units.
 *
 * <p>Test tooling only. Scripted orders stand in for a player giving the bot orders through the Bot Commands panel,
 * so a headless game can reproduce "the player told the unit X, did it do X?".</p>
 *
 * @param round       the game round the order is applied in, before that round's movement phase
 * @param targetKind  how the units are selected
 * @param targetValue the unit name, unit id or bot name, depending on {@code targetKind}; empty for {@code ALL}
 * @param action      what to do
 * @param arguments   the action's arguments, for example hex numbers or an edge
 * @param lineNumber  the script line, for log messages
 * @param sourceText  the script line as written
 */
public record ScriptedOrder(int round, TargetKind targetKind, String targetValue, OrderAction action,
      List<String> arguments, int lineNumber, String sourceText) {

    /** How a scripted order selects its units. */
    public enum TargetKind {
        /** Every unit whose display name, short name or "chassis model" matches, ignoring case. */
        UNIT_NAME,
        /** The single unit with this entity id (the {@code id:} in the scenario file). */
        UNIT_ID,
        /** Every unit owned by the named bot player. */
        BOT,
        /** The listed units, by id, in the given order ({@code units 101 102 103}); the first leads a formation. */
        UNIT_IDS,
        /** Every unit owned by any bot. */
        ALL
    }

    /** The actions a script can give. */
    public enum OrderAction {
        /** Replace the unit's waypoints with the given hexes; an optional first argument sets the priority. */
        WAYPOINTS,
        /** Append the given hexes to the unit's waypoints. */
        ADD_WAYPOINTS,
        /** Clear the unit's waypoints. */
        CLEAR,
        /** Bot-wide flee toward an edge; NONE cancels. Applied to the bots owning the selected units. */
        FLEE,
        /** Test only: damage the internal structure of the side torsos so the unit counts as crippled. */
        CRIPPLE,
        /** Test only: remove the given percentage of internal structure from every location except the head. */
        DAMAGE_INTERNAL,
        /** Hold in place, keeping the route. Unit orders model only. */
        PAUSE,
        /** Carry on after a pause. Unit orders model only. */
        RESUME,
        /** Clear every order and hold this round. Unit orders model only. */
        STOP,
        /** Move to the given edge and hold there. Unit orders model only. */
        MOVE_TO_EDGE,
        /** Move to the given edge and leave the board by it. Unit orders model only. */
        EXIT_BY_EDGE,
        /** Set the facing while moving and when stopped (N, NE, SE, S, SW, NW, 0-5 or AUTO). Unit orders only. */
        FACING,
        /** Set the route priority, NORMAL or IMPERATIVE. Unit orders model only. */
        PRIORITY,
        /**
         * Put the selected units in a formation: shape, then optional {@code spacing N}, {@code pace WALK|RUN} and
         * {@code contact BREAK|HOLD}. With {@code units a b c} the first unit leads and the rest take slots in order.
         */
        FORMATION,
        /** Take the selected units out of their formation. */
        FORMATION_OFF
    }
}

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
        /** Every unit owned by any bot. */
        ALL
    }

    /** The actions a script can give. */
    public enum OrderAction {
        /** Replace the unit's waypoints with the given hexes. */
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
        DAMAGE_INTERNAL
    }
}

/*
 * Copyright (C) 2021-2025 The MegaMek Team. All Rights Reserved.
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
package megamek.client.ui.swing.lobby.sorters;

import java.util.Comparator;
import java.util.OptionalInt;

import megamek.client.Client;
import megamek.common.Entity;
import megamek.common.Player;
import megamek.common.options.IGameOptions;

/** An interface for the Comparators used for the lobby Mek table. */
public interface MekTableSorter extends Comparator<Entity> {
    enum Sorting {
        ASCENDING(1),
        DESCENDING(-1);
        private final int direction;

        Sorting(int direction) {
            this.direction = direction;
        }

        public int getDirection() {
            return direction;
        }
    }

    String RESOURCE_BUNDLE = "megamek.client.sorters";

    /**
     * Compares two entities. The comparison is done by first comparing the
     * players, then the unit roles.
     *
     * @param client the client reference to access player information
     * @param a the first entity
     * @param b the second entity
     * @return a negative integer, zero, or a positive integer as the first argument is less than, equal to, or
     * greater than the second, or empty if there is no comparison possible
     */
    default OptionalInt getPlayerTeamIndexPosition(Client client, Entity a, Entity b) {
        final Player playerA = client.getGame().getPlayer(a.getOwnerId());
        final Player playerB = client.getGame().getPlayer(b.getOwnerId());
        final Player localPlayer = client.getLocalPlayer();

        if (playerA != null && playerB != null) {
            final int teamA = playerA.getTeam();
            final int teamB = playerB.getTeam();
            if (playerA.equals(localPlayer) && !playerB.equals(localPlayer)) {
                return OptionalInt.of(-1);
            } else if (!playerA.equals(localPlayer) && playerB.equals(localPlayer)) {
                return OptionalInt.of(1);
            } else if ((teamA == localPlayer.getTeam()) && (teamB != localPlayer.getTeam())) {
                return OptionalInt.of(-1);
            } else if ((teamB == localPlayer.getTeam()) && (teamA != localPlayer.getTeam())) {
                return OptionalInt.of(1);
            } else if (teamA != teamB) {
                return OptionalInt.of(teamA - teamB);
            } else if (!playerA.equals(playerB)) {
                return OptionalInt.of(playerA.getName().compareTo(playerB.getName()));
            }
        }
        return OptionalInt.empty();
    }


    /**
     * Returns the info that is displayed in the column header to show
     * the sorting that is used, such as "Team / BV".
     */
    String getDisplayName();

    /**
     * Returns the column index of the Mek Table that this sorter is to be used with.
     */
    int getColumnIndex();

    /**
     * Returns true if this Sorter is currently allowed. Sorters might not be allowed
     * e.g. when they would give away info in blind drops.
     */
    default boolean isAllowed(IGameOptions opts) {
        return true;
    }

    /** Returns the sorting direction. */
    default Sorting getSortingDirection() {
        // This probably should be Sorting.ASCENDING, having a default is better than relying on a null behavior
        // that is implementation dependent
        return null;
    }

    /**
     * Returns 1 if dir is ASCENDING, -1 otherwise.
     * @deprecated use {@link Sorting#getDirection()} instead
     */
    @Deprecated(forRemoval = true, since = "0.50.06")
    default int bigger(Sorting dir) {
        return dir == Sorting.ASCENDING ? 1 : -1;
    }

    /**
     * Returns -1 if dir is ASCENDING, 1 otherwise.
     * @deprecated use {@link Sorting#getDirection()} instead
     */
    @Deprecated(forRemoval = true, since = "0.50.06")
    default int smaller(Sorting dir) {
        return dir == Sorting.ASCENDING ? -1 : 1;
    }

}

/*
 * Copyright (C) 2020-2026 The MegaMek Team. All Rights Reserved.
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
package megamek.client.ui.panels.phaseDisplay.lobby.sorters;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.OptionalInt;
import java.util.Set;

import megamek.client.Client;
import megamek.common.units.Entity;
import megamek.common.Player;
import megamek.common.options.IGameOptions;

/**
 * Abstract Class for Mek Table Sorters.
 *
 * @author Luana Coppio
 */
public abstract class MekTableSorter implements Comparator<Entity> {

    protected static final String RESOURCE_BUNDLE = "megamek.client.sorters";
    private final int columnIndex;
    private final String displayName;
    private final Sorting sorting;

    public MekTableSorter(String displayName, int columnIndex) {
        this(displayName, columnIndex, Sorting.ASCENDING);
    }

    public MekTableSorter(String displayName, int columnIndex, Sorting sorting) {
        this.displayName = displayName;
        this.columnIndex = columnIndex;
        this.sorting = sorting;
    }

    /** Guards against a malformed load looping forever when walking up to a unit's outermost carrier. */
    private static final int MAX_CARRIER_DEPTH = 16;

    /**
     * Wraps a sorter so anything riding on another unit stays with it, whatever the sorter is ordering by.
     * <p>
     * A carrier and its load are one object on the board, so they should be one block in the list. Without this each
     * unit is sorted on its own merits and drifts away from what it is riding on: sorting by tonnage puts a 10 ton
     * carriage nowhere near the 75 ton tractor pulling it, and a DropShip nowhere near its JumpShip.
     * </p>
     * <p>
     * The wrapped sorter is asked about the outermost carriers instead, so a whole stack sorts wherever its carrier
     * would. Below that, units are ordered by where they sit in the stack: a carrier comes before the things it
     * carries, and trailers follow their tractor in hitch order, which is the order they occupy hexes in.
     * </p>
     *
     * @param baseSorter the sorter the player chose
     *
     * @return a sorter that applies the player's choice to whole carrier stacks
     */
    public static Comparator<Entity> keepingCarriedUnitsTogether(Comparator<Entity> baseSorter) {
        return (a, b) -> {
            List<Entity> pathA = carrierPath(a);
            List<Entity> pathB = carrierPath(b);
            Entity rootA = pathA.get(0);
            Entity rootB = pathB.get(0);

            if (rootA.getId() != rootB.getId()) {
                int rootComparison = baseSorter.compare(rootA, rootB);

                // The sorter sees no difference between these stacks. Returning zero would let a stable sort leave a
                // load stranded on the far side of a unit the sorter considers identical, so break the tie on the
                // carrier to keep each stack in one piece.
                return (rootComparison != 0) ? rootComparison : (rootA.getId() - rootB.getId());
            }

            // The same stack. Walk down until the two part company.
            int depth = 0;
            while ((depth < pathA.size()) && (depth < pathB.size())
                  && (pathA.get(depth).getId() == pathB.get(depth).getId())) {
                depth++;
            }

            // One is carrying the other, directly or further down. The carrier is listed first.
            if ((depth >= pathA.size()) || (depth >= pathB.size())) {
                return pathA.size() - pathB.size();
            }

            return compareStackSiblings(pathA.get(depth), pathB.get(depth));
        };
    }

    /**
     * Orders two units riding on the same carrier. Trailers keep hitch order; anything else falls back to unit id so
     * the result is stable.
     */
    private static int compareStackSiblings(Entity a, Entity b) {
        int hitchOrderA = hitchOrder(a);
        int hitchOrderB = hitchOrder(b);

        if ((hitchOrderA != Entity.NONE) && (hitchOrderB != Entity.NONE)) {
            return hitchOrderA - hitchOrderB;
        }

        return a.getId() - b.getId();
    }

    /** Where a trailer sits in its train, or {@link Entity#NONE} when the unit is not towed. */
    private static int hitchOrder(Entity entity) {
        if ((entity.getTractor() == Entity.NONE) || (entity.getGame() == null)) {
            return Entity.NONE;
        }

        Entity tractor = entity.getGame().getEntity(entity.getTractor());
        return (tractor == null) ? Entity.NONE : tractor.getAllTowedUnits().indexOf(entity.getId());
    }

    /**
     * The chain of units from the outermost carrier down to this one, the unit itself last.
     * <p>
     * Carriage nests, so a Mek in a DropShip in a JumpShip yields all three. Towing does not nest, but a train can be
     * carried, so both links are followed.
     * </p>
     */
    private static List<Entity> carrierPath(Entity entity) {
        List<Entity> path = new ArrayList<>();
        Set<Integer> visited = new HashSet<>();
        Entity current = entity;

        while ((current != null) && visited.add(current.getId()) && (path.size() < MAX_CARRIER_DEPTH)) {
            path.add(current);
            current = carrierOf(current);
        }

        Collections.reverse(path);
        return path;
    }

    /** The unit this one is riding on, whether carried in a bay or towed behind, or {@code null} when neither. */
    private static Entity carrierOf(Entity entity) {
        if (entity.getGame() == null) {
            return null;
        }

        if (entity.getTransportId() != Entity.NONE) {
            return entity.getGame().getEntity(entity.getTransportId());
        }
        if (entity.getTractor() != Entity.NONE) {
            return entity.getGame().getEntity(entity.getTractor());
        }

        return null;
    }

    /**
     * Compares two entities. The comparison is done by first comparing the players, then the unit roles.
     *
     * @param client the client reference to access player information
     * @param a      the first entity
     * @param b      the second entity
     *
     * @return a negative integer, zero, or a positive integer as the first argument is less than, equal to, or greater
     *       than the second, or empty if there is no comparison possible
     */
    protected OptionalInt getPlayerTeamIndexPosition(Client client, Entity a, Entity b) {
        // entity.getOwner() does not work properly because teams are not necessarily updated correctly for entities
        // when the user switches teams mid-game
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
     * Returns the info that is displayed in the column header to show the sorting that is used, such as "Team / BV".
     */
    public String getDisplayName() {
        return displayName;
    }

    /**
     * Returns the column index of the Mek Table that this sorter is to be used with.
     */
    public int getColumnIndex() {
        return columnIndex;
    }

    /**
     * Returns true if this Sorter is currently allowed. Sorters might not be allowed e.g. when they would give away
     * info in blind drops.
     */
    public boolean isAllowed(IGameOptions opts) {
        return true;
    }

    /** Returns the sorting direction. */
    public Sorting getSortingDirection() {
        return sorting;
    }

    /**
     * Returns the sorting direction as an int.
     *
     * @return 1 if sorting is ascending, -1 if sorting is descending.
     */
    protected int getSortingDirectionInt() {
        return sorting.getDirection();
    }

    /**
     * Returns 1 if dir is ASCENDING, -1 otherwise.
     *
     * @deprecated use {@link Sorting#getDirection()} instead
     */
    @Deprecated(forRemoval = true, since = "0.50.06")
    public int bigger(Sorting dir) {
        return dir == Sorting.ASCENDING ? 1 : -1;
    }

    /**
     * Returns -1 if dir is ASCENDING, 1 otherwise.
     *
     * @deprecated use {@link Sorting#getDirection()} instead
     */
    @Deprecated(forRemoval = true, since = "0.50.06")
    public int smaller(Sorting dir) {
        return dir == Sorting.ASCENDING ? -1 : 1;
    }

}

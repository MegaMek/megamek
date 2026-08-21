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
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.OptionalInt;
import java.util.Set;

import megamek.client.Client;
import megamek.common.Player;
import megamek.common.annotations.Nullable;
import megamek.common.options.IGameOptions;
import megamek.common.units.Entity;

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
    /**
     * Wraps a sorter so C3 network members stay together as one hierarchy-ordered block, whatever the sorter is
     * ordering by - the same guarantee {@link #keepingCarriedUnitsTogether(Comparator)} gives carrier stacks, and
     * for the same reason: the lobby draws branch glyphs in front of network members, which only tell the truth
     * while a member sits directly under its master. Works on carrier-stack roots so both wrappers compose: a
     * towed unit follows its tractor first, and the whole stack follows the tractor's network.
     *
     * @param entities   the list about to be sorted, used to precompute one representative per network
     * @param baseSorter the (already stack-aware) sorter the player chose
     *
     * @return a sorter that applies the player's choice to whole networks
     */
    public static Comparator<Entity> keepingC3NetworksTogether(List<Entity> entities,
          Comparator<Entity> baseSorter) {
        // Two passes over the list, no per-entity game scans: a net id shared by 2+ carrier-stack roots in the
        // list marks a network; solo units keep a null key and sort as ordinary units
        Map<String, Integer> netIdCounts = new HashMap<>();
        for (Entity entity : entities) {
            String networkId = computeNetworkKey(entity);
            if (networkId != null) {
                netIdCounts.merge(networkId, 1, Integer::sum);
            }
        }
        Map<Integer, String> networkKeys = new HashMap<>();
        Map<String, Entity> representatives = new HashMap<>();
        for (Entity entity : entities) {
            String networkId = computeNetworkKey(entity);
            if ((networkId == null) || (netIdCounts.getOrDefault(networkId, 0) < 2)) {
                networkKeys.put(entity.getId(), null);
                continue;
            }
            networkKeys.put(entity.getId(), networkId);
            representatives.merge(networkId, entity,
                  (first, second) -> (first.getId() <= second.getId()) ? first : second);
        }
        return (a, b) -> {
            String netA = networkKeys.get(a.getId());
            String netB = networkKeys.get(b.getId());
            if ((netA == null) && (netB == null)) {
                return baseSorter.compare(a, b);
            }
            if (!Objects.equals(netA, netB)) {
                // Different networks (or one unit un-networked): the base sorter judges the representatives, so
                // a whole network sorts where its representative would; tie-break keeps blocks from interleaving
                Entity repA = (netA == null) ? a : representatives.get(netA);
                Entity repB = (netB == null) ? b : representatives.get(netB);
                int repComparison = baseSorter.compare(repA, repB);
                return (repComparison != 0) ? repComparison
                      : String.valueOf(netA).compareTo(String.valueOf(netB));
            }
            // Same network: walk the master chains, parent before dependents, sibling branches by unit id
            List<Entity> pathA = c3Path(a);
            List<Entity> pathB = c3Path(b);
            int depth = 0;
            while ((depth < pathA.size()) && (depth < pathB.size())
                  && (pathA.get(depth).getId() == pathB.get(depth).getId())) {
                depth++;
            }
            if ((depth >= pathA.size()) && (depth >= pathB.size())) {
                return baseSorter.compare(a, b);
            }
            if (depth >= pathA.size()) {
                return -1;
            }
            if (depth >= pathB.size()) {
                return 1;
            }
            return Integer.compare(pathA.get(depth).getId(), pathB.get(depth).getId());
        };
    }

    /** The C3 net id of the unit's carrier-stack root, {@code null} when the root carries no C3 system. */
    @Nullable
    private static String computeNetworkKey(Entity entity) {
        Entity root = carrierPath(entity).get(0);
        if (!root.hasAnyC3System()) {
            return null;
        }
        return root.getC3NetId();
    }

    /** Guards against a malformed master chain looping while walking up to the network top. */
    private static final int MAX_C3_CHAIN_DEPTH = 4;

    /** The chain from the network top down to the unit's carrier-stack root: [top, ..., root]. */
    private static List<Entity> c3Path(Entity entity) {
        List<Entity> path = new ArrayList<>();
        Entity current = carrierPath(entity).get(0);
        int guard = 0;
        while ((current != null) && (guard++ < MAX_C3_CHAIN_DEPTH)) {
            path.add(current);
            Entity master = current.getC3Master();
            if ((master == null) || (master.getId() == current.getId())) {
                break;
            }
            current = master;
        }
        Collections.reverse(path);
        return path;
    }

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

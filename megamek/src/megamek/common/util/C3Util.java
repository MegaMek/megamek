/*
 * Copyright (c) 2024 - The MegaMek Team. All Rights Reserved.
 *
 * This file is part of MegaMek.
 *
 * MegaMek is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * MegaMek is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with MegaMek. If not, see <http://www.gnu.org/licenses/>.
 */
package megamek.common.util;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import megamek.common.Entity;
import megamek.common.Game;

public class C3Util {

    public static class MissingC3MException extends Exception {
    }
    public static class MismatchingC3MException extends Exception {
    }
    public static class C3CapacityException extends Exception {
    }
    /**
     * Adds C3 connections when new units are being added.
     * 
     * @param game   The Game the unit is being added to, the unit should already be
     *               in the Game.
     * @param entity The entity being added.
     * @return A list of units affected
     */
    public static List<Entity> wireC3(Game game, Entity entity) {
        ArrayList<Entity> affectedUnits = new ArrayList<>();
        if (!entity.hasC3() && !entity.hasC3i() && !entity.hasNavalC3()) {
            return affectedUnits;
        }

        boolean C3iSet = false;

        for (Entity e : game.getEntitiesVector()) {

            // C3 Checks
            if (entity.hasC3()) {
                if ((entity.getC3MasterIsUUIDAsString() != null)
                        && entity.getC3MasterIsUUIDAsString().equals(e.getC3UUIDAsString())) {
                    entity.setC3Master(e, false);
                    entity.setC3MasterIsUUIDAsString(null);
                } else if ((e.getC3MasterIsUUIDAsString() != null)
                        && e.getC3MasterIsUUIDAsString().equals(entity.getC3UUIDAsString())) {
                    e.setC3Master(entity, false);
                    e.setC3MasterIsUUIDAsString(null);

                    affectedUnits.add(e);
                }
            }

            // C3i Checks
            if (entity.hasC3i() && !C3iSet) {
                entity.setC3NetIdSelf();
                int pos = 0;
                while (pos < Entity.MAX_C3i_NODES) {
                    // We've found a network, join it.
                    if ((entity.getC3iNextUUIDAsString(pos) != null)
                            && (e.getC3UUIDAsString() != null)
                            && entity.getC3iNextUUIDAsString(pos)
                                    .equals(e.getC3UUIDAsString())) {
                        entity.setC3NetId(e);
                        C3iSet = true;
                        break;
                    }

                    pos++;
                }
            }

            // NC3 Checks
            if (entity.hasNavalC3() && !C3iSet) {
                entity.setC3NetIdSelf();
                int pos = 0;
                while (pos < Entity.MAX_C3i_NODES) {
                    // We've found a network, join it.
                    if ((entity.getNC3NextUUIDAsString(pos) != null)
                            && (e.getC3UUIDAsString() != null)
                            && entity.getNC3NextUUIDAsString(pos)
                                    .equals(e.getC3UUIDAsString())) {
                        entity.setC3NetId(e);
                        C3iSet = true;
                        break;
                    }

                    pos++;
                }
            }
        }

        return affectedUnits;
    }

    /**
     * Disconnects the passed entities from their C3 network, if any.
     * Due to the way C3 networks are represented in Entity, units
     * cannot disconnect from a C3 network with an id that is the
     * entity's own id.
     */
    public static Set<Entity> disconnectFromNetwork(Game game, Collection<Entity> entities) {
        final Set<Entity> updateCandidates = performDisconnect(game, entities);
        return updateCandidates;
    }

    /**
     * Performs a disconnect from C3 networks for the given entities without sending
     * an update.
     * Returns a set of all affected units.
     */
    private static HashSet<Entity> performDisconnect(Game game, Collection<Entity> entities) {
        HashSet<Entity> updateCandidates = new HashSet<>();
        for (Entity entity : entities) {
            if (entity.hasNhC3()) {
                entity.setC3NetIdSelf();
                updateCandidates.add(entity);
            } else if (entity.hasAnyC3System()) {
                entity.setC3Master(null, true);
                updateCandidates.add(entity);
            }
        }
        // Also disconnect all units connected *to* that entity
        for (Entity entity : game.getEntitiesVector()) {
            if (entity.getC3Master() == null) {
                continue;
            }
            if (entities.contains(entity.getC3Master())) {
                entity.setC3Master(null, true);
                updateCandidates.add(entity);
            }
        }
        return updateCandidates;
    }

    /** Sets the entities' C3M to act as a Company Master. */
    public static void setCompanyMaster(Collection<Entity> entities) throws MissingC3MException {
        if (!entities.stream().allMatch(Entity::hasC3M)) {
            throw new MissingC3MException();
        }
        entities.forEach(e -> e.setC3Master(e.getId(), true));
    }

    /** Sets the entities' C3M to act as a Lance Master (aka normal mode). */
    public static void setLanceMaster(Collection<Entity> entities) throws MissingC3MException {
        if (!entities.stream().allMatch(Entity::hasC3M)) {
            throw new MissingC3MException();
        }
        entities.forEach(e -> e.setC3Master(-1, true));
    }

    /**
     * Returns true if a and b share at least one non-hierarchic C3 system (C3i, Naval C3, Nova CEWS). Symmetrical (the
     * order of a and b does not matter).
     */
    public static boolean sameNhC3System(Entity a, Entity b) {
        return (a.hasC3i() && b.hasC3i()) || (a.hasNavalC3() && b.hasNavalC3()) || (a.hasNovaCEWS() && b.hasNovaCEWS());
    }

    /**
     * Connects the passed entities to a nonhierarchic C3 (NC3, C3i or Nova CEWS)
     * identified by masterID.
     */
    public static void joinNh(Game game, Collection<Entity> entities, int masterID, boolean disconnectFirst) throws MismatchingC3MException, C3CapacityException {
        Entity master = game.getEntity(masterID);
        if (!master.hasNhC3() || !entities.stream().allMatch(e -> sameNhC3System(master, e))) {
            throw new MismatchingC3MException();
        }
        if (disconnectFirst) {
            performDisconnect(game, entities);
        }
        int freeNodes = master.calculateFreeC3Nodes();
        freeNodes += entities.contains(master) ? 1 : 0;
        if (entities.size() > freeNodes) {
            throw new C3CapacityException();
        }
        entities.forEach(e -> e.setC3NetId(master));
    }

    /**
     * Connects the passed entities to a standard C3M
     * identified by masterID.
     */
    public static Set<Entity> connect(Game game, Collection<Entity> entities, int masterID, boolean disconnectFirst) throws MismatchingC3MException, C3CapacityException {
        Entity master = game.getEntity(masterID);
        // To make it possible to mark a C3S/C3S/C3S/C3M lance and connect it:
        entities.remove(master);
        boolean connectMS = master.isC3IndependentMaster() && entities.stream().allMatch(Entity::hasC3S);
        boolean connectMM = master.isC3CompanyCommander() && entities.stream().allMatch(Entity::hasC3M);
        boolean connectSMM = master.hasC3MM() && entities.stream().allMatch(e -> e.hasC3S() || e.hasC3M());
        if (!connectMM && !connectMS && !connectSMM) {
            throw new MismatchingC3MException();
        }
        Set<Entity> updateCandidates = new HashSet<>(entities);
        if (disconnectFirst) { // this is only true when a C3 lance is formed from SSSM
            updateCandidates.addAll(performDisconnect(game, entities));
            updateCandidates.addAll(performDisconnect(game, Arrays.asList(master)));
        }
        int newC3nodeCount = entities.stream().mapToInt(e -> game.getC3SubNetworkMembers(e).size()).sum();
        int masC3nodeCount = game.getC3NetworkMembers(master).size();
        if (newC3nodeCount + masC3nodeCount > Entity.MAX_C3_NODES || entities.size() > master.calculateFreeC3Nodes()) {
            throw new C3CapacityException();
        }
        entities.forEach(e -> e.setC3Master(master, true));
        return updateCandidates;
    }

    public static List<Entity> broadcastChanges(Game game, Entity entity) {
        // When we customize a single entity's C3 network setting,
        // **ALL** members of the network may get changed.
        Entity c3master = entity.getC3Master();
        List<Entity> c3members = new ArrayList<>();
        for (Entity unit : game.getEntitiesVector()) {
            if (entity.onSameC3NetworkAs(unit)) {
                c3members.add(unit);
            }
        }
        // Do we need to update the members of our C3 network?
        if (((c3master != null) && !c3master.equals(entity.getC3Master()))
                || ((c3master == null) && (entity.getC3Master() != null))) {
            return c3members;
        }
        return List.of();
    }

    /**
     * Copies the C3 network setup from the source to the target.
     *
     * @param source The source {@link Entity}.
     * @param target The target {@link Entity}.
     */
    public static void copyC3Networks(Entity source, Entity target) {
        target.setC3UUIDAsString(source.getC3UUIDAsString());
        target.setC3Master(source.getC3Master(), false);
        target.setC3MasterIsUUIDAsString(source.getC3MasterIsUUIDAsString());

        // Reassign the C3NetId
        target.setC3NetId(source.getC3NetId());

        for (int pos = 0; pos < Entity.MAX_C3i_NODES; ++pos) {
            target.setC3iNextUUIDAsString(pos, source.getC3iNextUUIDAsString(pos));
            target.setNC3NextUUIDAsString(pos, source.getNC3NextUUIDAsString(pos));
        }
    }
}

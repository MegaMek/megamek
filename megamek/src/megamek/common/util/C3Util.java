/*
 * Copyright (C) 2024-2025 The MegaMek Team. All Rights Reserved.
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

package megamek.common.util;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import megamek.common.game.Game;
import megamek.common.units.Entity;
import megamek.logging.MMLogger;

public class C3Util {
    private static final MMLogger logger = MMLogger.create(C3Util.class);

    public static class MissingC3MException extends Exception {
    }

    public static class MismatchingC3MException extends Exception {
    }

    public static class C3CapacityException extends Exception {
    }

    /**
     * Adds C3 connections when new units are being added.
     *
     * @param game   The Game the unit is being added to, the unit should already be in the Game.
     * @param entity The entity being added.
     *
     * @return A list of units affected
     */
    public static List<Entity> wireC3(Game game, Entity entity) {
        ArrayList<Entity> affectedUnits = new ArrayList<>();
        if (!entity.hasC3() && !entity.hasC3i() && !entity.hasNavalC3() && !entity.hasNovaCEWS()) {
            return affectedUnits;
        }

        logger.debug("[C3Util] wireC3() called for entity {} ({}), hasNovaCEWS={}, currentNetId={}",
            entity.getId(), entity.getShortName(), entity.hasNovaCEWS(), entity.getC3NetId());

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
                // Only initialize network ID if not already set (preserves lobby configuration)
                if (entity.getC3NetId() == null) {
                    entity.setC3NetIdSelf();
                }
                int pos = 0;
                while (pos < Entity.MAX_C3i_NODES) {
                    // We've found a potential network partner
                    if ((entity.getC3iNextUUIDAsString(pos) != null)
                          && (e.getC3UUIDAsString() != null)
                          && entity.getC3iNextUUIDAsString(pos)
                          .equals(e.getC3UUIDAsString())) {

                        if (tryEstablishBidirectionalPartnership(entity, e, pos, false)) {
                            C3iSet = true;
                            break;
                        }
                    }

                    pos++;
                }
            }

            // NC3 Checks
            if (entity.hasNavalC3() && !C3iSet) {
                // Only initialize network ID if not already set (preserves lobby configuration)
                if (entity.getC3NetId() == null) {
                    entity.setC3NetIdSelf();
                }
                int pos = 0;
                while (pos < Entity.MAX_C3i_NODES) {
                    // We've found a potential network partner
                    if ((entity.getNC3NextUUIDAsString(pos) != null)
                          && (e.getC3UUIDAsString() != null)
                          && entity.getNC3NextUUIDAsString(pos)
                          .equals(e.getC3UUIDAsString())) {

                        if (tryEstablishBidirectionalPartnership(entity, e, pos, true)) {
                            C3iSet = true;
                            break;
                        }
                    }

                    pos++;
                }
            }

            // Nova CEWS Checks (uses same UUID array as NC3)
            if (entity.hasNovaCEWS() && !C3iSet) {
                logger.debug("[C3Util] Nova CEWS check for entity {} ({})", entity.getId(), entity.getShortName());
                logger.debug("[C3Util]   Current network ID: {}", entity.getC3NetId());

                // DEBUGGING: Log entire NC3UUID array
                StringBuilder uuidDump = new StringBuilder("[");
                for (int debugPos = 0; debugPos < Entity.MAX_C3i_NODES; debugPos++) {
                    String debugUUID = entity.getNC3NextUUIDAsString(debugPos);
                    if (debugPos > 0) uuidDump.append(", ");
                    uuidDump.append(debugUUID == null ? "null" : "\"" + debugUUID + "\"");
                }
                uuidDump.append("]");
                logger.debug("[C3Util]   NC3UUID array contents: {}", uuidDump);

                // Only initialize network ID if not already set (preserves lobby configuration)
                if (entity.getC3NetId() == null) {
                    entity.setC3NetIdSelf();
                    logger.debug("[C3Util]   Network ID was null, initialized to: {}", entity.getC3NetId());
                } else {
                    logger.debug("[C3Util]   Network ID already set, preserving: {}", entity.getC3NetId());
                }

                int pos = 0;
                while (pos < Entity.MAX_C3i_NODES) {
                    String uuidAtPos = entity.getNC3NextUUIDAsString(pos);
                    if (uuidAtPos != null) {
                        logger.debug("[C3Util]   Checking entity {} UUID: \"{}\"", e.getId(), e.getC3UUIDAsString());
                        logger.debug("[C3Util]   Against stored UUID at pos {}: \"{}\"", pos, uuidAtPos);
                    }

                    // We've found a potential network partner
                    if ((entity.getNC3NextUUIDAsString(pos) != null)
                          && (e.getC3UUIDAsString() != null)
                          && entity.getNC3NextUUIDAsString(pos)
                          .equals(e.getC3UUIDAsString())) {
                        logger.debug("[C3Util]   MATCH FOUND! Entity {} UUID matches at pos {}", e.getId(), pos);

                        if (tryEstablishBidirectionalPartnership(entity, e, pos, true)) {
                            logger.debug("[C3Util]   Bidirectional check PASSED - joined network: {}",
                                  entity.getC3NetId());
                            C3iSet = true;
                            break;
                        } else {
                            logger.debug("[C3Util]   Bidirectional check FAILED - cleared orphaned UUID at pos {}",
                                  pos);
                        }
                    }

                    pos++;
                }

                if (!C3iSet) {
                    logger.debug("[C3Util]   No valid network partners found in UUID array");
                }
            }
        }

        logger.debug("[C3Util] wireC3() complete for entity {} ({}), final network ID: {}, C3iSet={}",
            entity.getId(), entity.getShortName(), entity.getC3NetId(), C3iSet);

        return affectedUnits;
    }

    /**
     * Checks if an entity has a specific UUID in its C3i array.
     * Used to verify bidirectional network partnerships.
     *
     * @param entity The entity to check
     * @param targetUUID The UUID to search for
     * @return true if the UUID is found in the entity's C3i array
     */
    private static boolean hasEntityInC3iArray(Entity entity, String targetUUID) {
        if (targetUUID == null) {
            return false;
        }
        for (int i = 0; i < Entity.MAX_C3i_NODES; i++) {
            if (targetUUID.equals(entity.getC3iNextUUIDAsString(i))) {
                return true;
            }
        }
        return false;
    }

    /**
     * Checks if an entity has a specific UUID in its NC3 array.
     * Used to verify bidirectional network partnerships for Naval C3 and Nova CEWS.
     *
     * @param entity The entity to check
     * @param targetUUID The UUID to search for
     * @return true if the UUID is found in the entity's NC3 array
     */
    private static boolean hasEntityInNC3Array(Entity entity, String targetUUID) {
        if (targetUUID == null) {
            return false;
        }
        for (int i = 0; i < Entity.MAX_C3i_NODES; i++) {
            if (targetUUID.equals(entity.getNC3NextUUIDAsString(i))) {
                return true;
            }
        }
        return false;
    }

    /**
     * Attempts to establish a bidirectional network partnership for non-hierarchic C3 systems. If the partner entity
     * has this entity's UUID in its array, joins the network. If not, clears the orphaned UUID reference.
     *
     * @param entity  The entity trying to join a network
     * @param partner The potential network partner
     * @param pos     The position in the UUID array where partner's UUID was found
     * @param useNC3  true to use NC3 arrays (Naval C3/Nova CEWS), false to use C3i arrays
     *
     * @return true if partnership was established (entity joined partner's network), false if orphaned UUID was cleared
     */
    private static boolean tryEstablishBidirectionalPartnership(Entity entity, Entity partner, int pos,
          boolean useNC3) {
        String entityUUID = entity.getC3UUIDAsString();

        boolean hasBidirectional = useNC3
              ? hasEntityInNC3Array(partner, entityUUID)
              : hasEntityInC3iArray(partner, entityUUID);

        if (hasBidirectional) {
            // Valid bidirectional partnership - join network
            entity.setC3NetId(partner);
            return true;
        } else {
            // Orphaned UUID - clear it
            if (useNC3) {
                entity.setNC3NextUUIDAsString(pos, null);
            } else {
                entity.setC3iNextUUIDAsString(pos, null);
            }
            return false;
        }
    }

    /**
     * Disconnects the passed entities from their C3 network, if any. Due to the way C3 networks are represented in
     * Entity, units cannot disconnect from a C3 network with an id that is the entity's own id.
     */
    public static Set<Entity> disconnectFromNetwork(Game game, Collection<Entity> entities) {
        return performDisconnect(game, entities);
    }

    /**
     * Performs a disconnect from C3 networks for the given entities without sending an update. Returns a set of all
     * affected units.
     */
    private static HashSet<Entity> performDisconnect(Game game, Collection<Entity> entities) {
        HashSet<Entity> updateCandidates = new HashSet<>();
        for (Entity entity : entities) {
            if (entity.hasNhC3()) {
                // CRITICAL FIX: Find all network members BEFORE disconnecting
                // We need to remove the disconnecting entity's UUID from their arrays
                List<Entity> networkMembers = new ArrayList<>();
                for (Entity other : game.getEntitiesVector()) {
                    if (!other.equals(entity) && entity.onSameC3NetworkAs(other)) {
                        networkMembers.add(other);
                    }
                }

                // Disconnect this entity
                entity.setC3NetIdSelf();

                // Clear UUID arrays on disconnecting entity
                // Prevents stale UUIDs from interfering with future network joins
                for (int pos = 0; pos < Entity.MAX_C3i_NODES; pos++) {
                    if (entity.hasNavalC3() || entity.hasNovaCEWS()) {
                        entity.setNC3NextUUIDAsString(pos, null);
                    } else if (entity.hasC3i()) {
                        entity.setC3iNextUUIDAsString(pos, null);
                    }
                }

                // CRITICAL FIX: Remove disconnecting entity's UUID from ALL network members
                // This prevents orphaned UUIDs that can cause network corruption
                String disconnectingUUID = entity.getC3UUIDAsString();
                for (Entity other : networkMembers) {
                    for (int pos = 0; pos < Entity.MAX_C3i_NODES; pos++) {
                        String storedUUID = null;
                        if (other.hasNavalC3() || other.hasNovaCEWS()) {
                            storedUUID = other.getNC3NextUUIDAsString(pos);
                            if (disconnectingUUID.equals(storedUUID)) {
                                other.setNC3NextUUIDAsString(pos, null);
                            }
                        } else if (other.hasC3i()) {
                            storedUUID = other.getC3iNextUUIDAsString(pos);
                            if (disconnectingUUID.equals(storedUUID)) {
                                other.setC3iNextUUIDAsString(pos, null);
                            }
                        }
                    }
                    updateCandidates.add(other);  // Mark network member for update
                }

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
     * Connects the passed entities to a non-hierarchic C3 (NC3, C3i or Nova CEWS) identified by masterID.
     */
    public static void joinNh(Game game, Collection<Entity> entities, int masterID, boolean disconnectFirst)
          throws MismatchingC3MException, C3CapacityException {
        Entity master = game.getEntity(masterID);

        if (master == null) {
            return;
        }

        if (!master.hasNhC3() || !entities.stream().allMatch(e -> sameNhC3System(master, e))) {
            throw new MismatchingC3MException();
        }

        if (disconnectFirst) {
            performDisconnect(game, entities);
        }

        // CRITICAL VALIDATION: Calculate total network size AFTER join and verify it doesn't exceed maximum
        // This prevents oversized networks when master units fail to disconnect (Bug #6754)
        int currentNetworkSize = 1;  // Master counts as 1
        for (Entity e : game.getEntitiesVector()) {
            if (!e.equals(master) && master.onSameC3NetworkAs(e)) {
                currentNetworkSize++;
            }
        }

        // Count NEW entities being added (exclude those already in network)
        int newEntities = 0;
        for (Entity e : entities) {
            if (!e.equals(master) && !master.onSameC3NetworkAs(e)) {
                newEntities++;
            }
        }

        // Calculate total network size after join
        int totalAfterJoin = currentNetworkSize + newEntities;

        // Determine max network size based on system type
        int maxNetworkSize;
        if (master.hasNovaCEWS()) {
            maxNetworkSize = Entity.MAX_NOVA_CEWS_NODES;  // 3
        } else if (master.hasC3i() || master.hasNavalC3()) {
            maxNetworkSize = Entity.MAX_C3i_NODES;  // 6
        } else {
            maxNetworkSize = Entity.MAX_C3i_NODES;  // Default to 6
        }

        // Validate total network size
        if (totalAfterJoin > maxNetworkSize) {
            throw new C3CapacityException();
        }

        // Original validation (keep as secondary check)
        int freeNodes = master.calculateFreeC3Nodes();
        freeNodes += entities.contains(master) ? 1 : 0;

        if (entities.size() > freeNodes) {
            throw new C3CapacityException();
        }

        // Copy network ID to all entities
        entities.forEach(e -> e.setC3NetId(master));

        // Build complete list of ALL network members, including those already in the network
        // This is critical for lobby configuration where units are added one at a time
        List<Entity> networkMembers = new ArrayList<>();
        for (Entity e : game.getEntitiesVector()) {
            // Include entities that:
            // 1. Are being added now (in entities collection)
            // 2. Are the master
            // 3. Are already networked with the master (same C3 system and same network ID)
            if (entities.contains(e) || e.equals(master) ||
                (sameNhC3System(master, e) && e.onSameC3NetworkAs(master))) {
                networkMembers.add(e);
            }
        }

        logger.debug("[C3Util] joinNh() storing UUID cross-references for {} network members", networkMembers.size());

        // Store UUID cross-references for all network members
        // This allows wireC3() to reconnect the network when the game starts
        for (Entity entity : networkMembers) {
            // CRITICAL FIX: Clear all UUID slots before assigning new network partners
            // This prevents stale UUIDs from previous network configurations
            for (int clearPos = 0; clearPos < Entity.MAX_C3i_NODES; clearPos++) {
                if (entity.hasNovaCEWS() || entity.hasNavalC3()) {
                    entity.setNC3NextUUIDAsString(clearPos, null);
                } else if (entity.hasC3i()) {
                    entity.setC3iNextUUIDAsString(clearPos, null);
                }
            }

            // Now assign UUIDs for current network partners
            int pos = 0;
            for (Entity partner : networkMembers) {
                if (!partner.equals(entity) && pos < Entity.MAX_C3i_NODES) {
                    String partnerUUID = partner.getC3UUIDAsString();
                    if (entity.hasNovaCEWS() || entity.hasNavalC3()) {
                        entity.setNC3NextUUIDAsString(pos, partnerUUID);
                        logger.debug("[C3Util]   Entity {} ({}): NC3UUID[{}] = \"{}\" (from entity {} {})",
                            entity.getId(), entity.getShortName(), pos, partnerUUID, partner.getId(), partner.getShortName());
                    } else if (entity.hasC3i()) {
                        entity.setC3iNextUUIDAsString(pos, partnerUUID);
                        logger.debug("[C3Util]   Entity {} ({}): C3iUUID[{}] = \"{}\" (from entity {} {})",
                            entity.getId(), entity.getShortName(), pos, partnerUUID, partner.getId(), partner.getShortName());
                    }
                    pos++;
                }
            }
        }
    }

    /**
     * Connects the passed entities to a standard C3M identified by masterID.
     */
    public static Set<Entity> connect(Game game, Collection<Entity> entities, int masterID, boolean disconnectFirst)
          throws MismatchingC3MException, C3CapacityException {
        Entity master = game.getEntity(masterID);

        if (master == null) {
            return Collections.emptySet();
        }

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
            updateCandidates.addAll(performDisconnect(game, List.of(master)));
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

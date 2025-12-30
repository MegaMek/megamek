/*
 * Copyright (C) 2025 The MegaMek Team. All Rights Reserved.
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
 * BattleMek, `Mek and AeroTek are registered trademarks
 * of The Topps Company, Inc. All Rights Reserved.
 *
 * Catalyst Game Labs and the Catalyst Game Labs logo are trademarks of
 * InMediaRes Productions, LLC.
 *
 * BattleMekWarrior Copyright Microsoft Corporation. MegaMek was created under
 * Microsoft's "Game Content Usage Rules"
 * <https://www.xbox.com/en-US/developers/rules> and it is not endorsed by or
 * affiliated with Microsoft.
 */

package megamek.server.totalWarfare;

import megamek.common.units.AbstractBuildingEntity;
import megamek.common.units.Entity;
import megamek.common.units.Infantry;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Tracks active infantry vs. infantry combat actions inside buildings.
 *
 * <p>This tracker is specific to building combat. A separate tracker would be
 * needed for naval boarding actions.</p>
 */
public class InfantryBuildingCombatTracker {

    /**
     * Represents an active building combat.
     */
    public static class BuildingCombat {
        /** The target building entity ID */
        public final int buildingEntityId;

        /** Entity IDs of attacking infantry */
        public final List<Integer> attackerIds;

        /** Entity IDs of defending infantry */
        public final List<Integer> defenderIds;

        /** Number of turns this combat has been active */
        public int turnCount;

        /**
         * True if attackers have achieved Partial control (P result).
         * When true, defenders lose half-damage bonus (TOAR p. 172).
         */
        public boolean hasPartialControl = false;

        /**
         * Creates a new building combat.
         *
         * @param buildingEntityId the building entity ID
         */
        public BuildingCombat(int buildingEntityId) {
            this.buildingEntityId = buildingEntityId;
            this.attackerIds = new ArrayList<>();
            this.defenderIds = new ArrayList<>();
            this.turnCount = 0;
            this.hasPartialControl = false;
        }

        /**
         * Add an attacker to this combat.
         *
         * @param entityId the attacking infantry entity ID
         */
        public void addAttacker(int entityId) {
            if (!attackerIds.contains(entityId)) {
                attackerIds.add(entityId);
            }
        }

        /**
         * Add a defender to this combat.
         *
         * @param entityId the defending infantry entity ID
         */
        public void addDefender(int entityId) {
            if (!defenderIds.contains(entityId)) {
                defenderIds.add(entityId);
            }
        }

        /**
         * Remove an entity from combat (eliminated or withdrawn).
         *
         * @param entityId the entity ID to remove
         */
        public void removeEntity(int entityId) {
            attackerIds.remove(Integer.valueOf(entityId));
            defenderIds.remove(Integer.valueOf(entityId));
        }

        /**
         * Check if this combat is still active.
         *
         * @return true if both sides have combatants
         */
        public boolean isActive() {
            return !attackerIds.isEmpty() && !defenderIds.isEmpty();
        }

        /**
         * Get total number of combatants.
         *
         * @return total combatants (attackers + defenders)
         */
        public int getTotalCombatants() {
            return attackerIds.size() + defenderIds.size();
        }
    }

    /** Map of active combats, keyed by building entity ID */
    private final Map<Integer, BuildingCombat> activeCombats;

    /**
     * Creates a new combat tracker.
     */
    public InfantryBuildingCombatTracker() {
        this.activeCombats = new HashMap<>();
    }

    /**
     * Start a new combat or add to existing combat.
     *
     * @param buildingId the building entity ID
     * @param attacker the attacking infantry entity
     * @param defender the defending infantry entity (can be null if joining existing)
     */
    public void addCombat(int buildingId, Entity attacker, Entity defender) {
        BuildingCombat combat = activeCombats.get(buildingId);

        if (combat == null) {
            // Start new combat
            combat = new BuildingCombat(buildingId);
            activeCombats.put(buildingId, combat);
        }

        // Add attacker
        if (attacker instanceof Infantry) {
            combat.addAttacker(attacker.getId());
            attacker.setInfantryCombatTargetId(buildingId);
            attacker.setInfantryCombatAttacker(true);
            attacker.setInfantryCombatTurnCount(0);
        }

        // Add defender (if specified)
        if (defender instanceof Infantry || defender instanceof AbstractBuildingEntity) {
            combat.addDefender(defender.getId());
            defender.setInfantryCombatTargetId(buildingId);
            defender.setInfantryCombatAttacker(false);
            defender.setInfantryCombatTurnCount(0);
        }
    }

    /**
     * Add a reinforcement to an existing combat.
     *
     * @param buildingId the building entity ID
     * @param entity the reinforcing infantry entity
     * @param isAttacker true if reinforcing attackers, false for defenders
     * @return true if combat exists and reinforcement was added
     */
    public boolean addReinforcement(int buildingId, Entity entity, boolean isAttacker) {
        BuildingCombat combat = activeCombats.get(buildingId);
        if (combat == null || !(entity instanceof Infantry)) {
            return false;
        }

        if (isAttacker) {
            combat.addAttacker(entity.getId());
        } else {
            combat.addDefender(entity.getId());
        }

        // Set entity state
        entity.setInfantryCombatTargetId(buildingId);
        entity.setInfantryCombatAttacker(isAttacker);
        entity.setInfantryCombatTurnCount(0);  // Reinforcements start at turn 0

        return true;
    }

    /**
     * Remove a combat from tracking.
     *
     * @param buildingId the building entity ID
     * @return the removed combat, or null if not found
     */
    public BuildingCombat removeCombat(int buildingId) {
        return activeCombats.remove(buildingId);
    }

    /**
     * Get a specific combat.
     *
     * @param buildingId the building entity ID
     * @return the combat, or null if not found
     */
    public BuildingCombat getCombat(int buildingId) {
        return activeCombats.get(buildingId);
    }

    /**
     * Get all active combats.
     *
     * @return map of all active combats
     */
    public Map<Integer, BuildingCombat> getAllCombats() {
        return new HashMap<>(activeCombats);
    }

    /**
     * Check if there's an active combat in a building.
     *
     * @param buildingId the building entity ID
     * @return true if combat exists
     */
    public boolean hasCombat(int buildingId) {
        return activeCombats.containsKey(buildingId);
    }

    /**
     * Remove all attackers from a combat (withdrawal).
     *
     * @param buildingId the building entity ID
     * @return list of attacker IDs that were removed
     */
    public List<Integer> withdrawAttackers(int buildingId) {
        BuildingCombat combat = activeCombats.get(buildingId);
        if (combat == null) {
            return new ArrayList<>();
        }

        List<Integer> withdrawnIds = new ArrayList<>(combat.attackerIds);
        combat.attackerIds.clear();

        // If no defenders left either, remove combat
        if (!combat.isActive()) {
            activeCombats.remove(buildingId);
        }

        return withdrawnIds;
    }

    /**
     * Remove an entity from a combat (eliminated).
     *
     * @param buildingId the building entity ID
     * @param entityId the entity ID to remove
     */
    public void removeEntity(int buildingId, int entityId) {
        BuildingCombat combat = activeCombats.get(buildingId);
        if (combat != null) {
            combat.removeEntity(entityId);

            // If combat is no longer active, remove it
            if (!combat.isActive()) {
                activeCombats.remove(buildingId);
            }
        }
    }

    /**
     * Increment turn counters for all active combats.
     */
    public void incrementAllTurnCounters() {
        for (BuildingCombat combat : activeCombats.values()) {
            combat.turnCount++;
        }
    }

    /**
     * Clear all tracked combats.
     */
    public void clearAll() {
        activeCombats.clear();
    }

    /**
     * Get the number of active combats.
     *
     * @return number of active combats
     */
    public int getActiveCombatCount() {
        return activeCombats.size();
    }
}

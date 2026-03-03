/*
 * Copyright (C) 2025-2026 The MegaMek Team. All Rights Reserved.
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

package megamek.server.totalWarfare;

import megamek.common.units.AbstractBuildingEntity;
import megamek.common.units.Entity;
import megamek.common.units.Infantry;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Tracks active infantry vs. infantry actions per TOAR p. 167-174.
 *
 * <p>Infantry vs. infantry actions resolve large-scale infantry-only combat
 * inside buildings, Large Naval Vessels, and aerospace units (Small/Large Craft).
 * This tracker supports all target types using generic "infantry action" terminology
 * from TOAR.</p>
 *
 * <p><strong>Note:</strong> Mobile structure grappling is not yet implemented.</p>
 */
public class InfantryActionTracker {

    /**
     * Represents an active infantry vs. infantry action.
     *
     * <p>Targets can be buildings, Large Naval Vessels, or aerospace units.
     * The generic term "action" follows TOAR terminology (TOAR p. 167).</p>
     */
    public static class InfantryAction {
        /** The target entity ID (building, ship, or aerospace unit) */
        public final int targetId;

        /** Entity IDs of attacking infantry */
        public final List<Integer> attackerIds;

        /** Entity IDs of defending infantry and crew */
        public final List<Integer> defenderIds;

        /** Number of turns this action has been active */
        public int turnCount;

        /**
         * True if attackers have achieved Partial control (P result).
         * When true, defenders lose half-damage bonus (TOAR p. 172).
         */
        public boolean hasPartialControl = false;

        /**
         * Creates a new infantry action.
         *
         * @param targetId the target entity ID (building, ship, or aerospace unit)
         */
        public InfantryAction(int targetId) {
            this.targetId = targetId;
            this.attackerIds = new ArrayList<>();
            this.defenderIds = new ArrayList<>();
            this.turnCount = 0;
            this.hasPartialControl = false;
        }

        /**
         * Add an attacker to this action.
         *
         * @param entityId the attacking infantry entity ID
         */
        public void addAttacker(int entityId) {
            if (!attackerIds.contains(entityId)) {
                attackerIds.add(entityId);
            }
        }

        /**
         * Add a defender to this action.
         *
         * @param entityId the defending infantry or crew entity ID
         */
        public void addDefender(int entityId) {
            if (!defenderIds.contains(entityId)) {
                defenderIds.add(entityId);
            }
        }

        /**
         * Remove an entity from the action (eliminated or withdrawn).
         *
         * @param entityId the entity ID to remove
         */
        public void removeEntity(int entityId) {
            attackerIds.remove(Integer.valueOf(entityId));
            defenderIds.remove(Integer.valueOf(entityId));
        }

        /**
         * Check if this action is still active.
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

    /** Map of active actions, keyed by target entity ID */
    private final Map<Integer, InfantryAction> activeActions;

    /**
     * Creates a new infantry action tracker.
     */
    public InfantryActionTracker() {
        this.activeActions = new HashMap<>();
    }

    /**
     * Manually add an action to the Infantry Action Tracker. Used during reinitialization.
     * @param action Manually created action to add
     */
    public void restoreCombat(InfantryAction action) {
        if (action != null && !activeActions.containsKey(action.targetId)) {
            activeActions.put(action.targetId, action);
        }
    }

    /**
     * Start a new action or add to existing action.
     *
     * @param targetId the target entity ID (building, ship, or aerospace unit)
     * @param attacker the attacking infantry entity
     * @param defender the defending entity (infantry, crew, or target with crew)
     */
    public void addCombat(int targetId, Entity attacker, Entity defender) {
        InfantryAction action = activeActions.get(targetId);

        if (action == null) {
            // Start new action
            action = new InfantryAction(targetId);
            activeActions.put(targetId, action);
        }

        // Add attacker
        if (attacker instanceof Infantry) {
            action.addAttacker(attacker.getId());
            attacker.setInfantryCombatTargetId(targetId);
            attacker.setInfantryCombatAttacker(true);
            attacker.setInfantryCombatTurnCount(0);
        }

        // Add defender (can be infantry or building/ship with crew)
        if (defender instanceof Infantry || defender instanceof AbstractBuildingEntity) {
            action.addDefender(defender.getId());
            defender.setInfantryCombatTargetId(targetId);
            defender.setInfantryCombatAttacker(false);
            defender.setInfantryCombatTurnCount(0);
        }
    }

    /**
     * Add a reinforcement to an existing action.
     *
     * @param targetId the target entity ID
     * @param entity the reinforcing infantry entity
     * @param isAttacker true if reinforcing attackers, false for defenders
     * @return true if action exists and reinforcement was added
     */
    public boolean addReinforcement(int targetId, Entity entity, boolean isAttacker) {
        InfantryAction action = activeActions.get(targetId);
        if (action == null || !(entity instanceof Infantry)) {
            return false;
        }

        if (isAttacker) {
            action.addAttacker(entity.getId());
        } else {
            action.addDefender(entity.getId());
        }

        // Set entity state
        entity.setInfantryCombatTargetId(targetId);
        entity.setInfantryCombatAttacker(isAttacker);
        entity.setInfantryCombatTurnCount(0);  // Reinforcements start at turn 0

        return true;
    }

    /**
     * Remove an action from tracking.
     *
     * @param targetId the target entity ID
     * @return the removed action, or null if not found
     */
    public InfantryAction removeCombat(int targetId) {
        return activeActions.remove(targetId);
    }

    /**
     * Get a specific action.
     *
     * @param targetId the target entity ID
     * @return the action, or null if not found
     */
    public InfantryAction getCombat(int targetId) {
        return activeActions.get(targetId);
    }

    /**
     * Get all active actions.
     *
     * @return map of all active actions
     */
    public Map<Integer, InfantryAction> getAllCombats() {
        return new HashMap<>(activeActions);
    }

    /**
     * Check if there's an active action at a target.
     *
     * @param targetId the target entity ID
     * @return true if action exists
     */
    public boolean hasCombat(int targetId) {
        return activeActions.containsKey(targetId);
    }

    /**
     * Remove all attackers from an action (withdrawal).
     *
     * @param targetId the target entity ID
     * @return list of attacker IDs that were removed
     */
    public List<Integer> withdrawAttackers(int targetId) {
        InfantryAction action = activeActions.get(targetId);
        if (action == null) {
            return new ArrayList<>();
        }

        List<Integer> withdrawnIds = new ArrayList<>(action.attackerIds);
        action.attackerIds.clear();

        // If no defenders left either, remove action
        if (!action.isActive()) {
            activeActions.remove(targetId);
        }

        return withdrawnIds;
    }

    /**
     * Remove an entity from an action (eliminated).
     *
     * @param targetId the target entity ID
     * @param entityId the entity ID to remove
     */
    public void removeEntity(int targetId, int entityId) {
        InfantryAction action = activeActions.get(targetId);
        if (action != null) {
            action.removeEntity(entityId);

            // If action is no longer active, remove it
            if (!action.isActive()) {
                activeActions.remove(targetId);
            }
        }
    }

    /**
     * Increment turn counters for all active actions.
     */
    public void incrementAllTurnCounters() {
        for (InfantryAction action : activeActions.values()) {
            action.turnCount++;
        }
    }

    /**
     * Clear all tracked actions.
     */
    public void clearAll() {
        activeActions.clear();
    }

    /**
     * Get the number of active actions.
     *
     * @return number of active actions
     */
    public int getActiveCombatCount() {
        return activeActions.size();
    }
}

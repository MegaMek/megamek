/*
 * Copyright (C) 2025 The MegaMek Team. All Rights Reserved.
 *
 * This file is part of MegaMek.
 *
 * MegaMek is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License (GPL),
 * version 2 or (at your option) any later version,
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
 */
package megamek.client.bot.caspar;

import megamek.client.bot.common.formation.Formation;
import megamek.client.bot.common.GameState;
import megamek.common.Compute;
import megamek.common.Coords;
import megamek.common.Entity;
import megamek.common.Hex;
import megamek.common.UnitRole;
import megamek.common.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Manages high-level tactical planning for the AI.
 * Identifies objectives, assigns formations to objectives, and coordinates overall strategy.
 */
public class TacticalPlanner {
    private final Map<String, TacticalObjective> objectives = new HashMap<>();
    private final Map<String, String> formationAssignments = new HashMap<>();
    private final DifficultyManager difficultyManager;

    /**
     * Creates a tactical planner with the specified difficulty manager.
     *
     * @param difficultyManager The difficulty manager to use
     */
    public TacticalPlanner(DifficultyManager difficultyManager) {
        this.difficultyManager = difficultyManager;
    }

    /**
     * Updates tactical plan based on current game state.
     *
     * @param gameState The current game state
     * @param formations All friendly formations
     */
    public void updateTacticalPlan(GameState gameState, Collection<Formation> formations) {
        // Clear existing assignments
        formationAssignments.clear();

        // Identify objectives
        identifyObjectives(gameState);

        // Assign formations to objectives
        assignFormationsToObjectives(formations);
    }

    /**
     * Identifies tactical objectives based on game state.
     *
     * @param gameState The current game state
     */
    private void identifyObjectives(GameState gameState) {
        objectives.clear();

        // Identify defensive objectives
        identifyDefensiveObjectives(gameState);

        // Identify offensive objectives
        identifyOffensiveObjectives(gameState);

        // Identify scouting objectives
        identifyScoutingObjectives(gameState);
    }

    /**
     * Identifies defensive objectives like protecting VIPs or holding positions.
     *
     * @param gameState The current game state
     */
    private void identifyDefensiveObjectives(GameState gameState) {
        // Protect VIPs or important units
        List<Entity> vips = gameState.getFriendlyUnits().stream()
            .filter(u -> isVIP(u) || isTransport(u))
            .toList();

        for (Entity vip : vips) {
            TacticalObjective objective = new TacticalObjective(
                "protect_" + vip.getId(),
                ObjectiveType.PROTECT,
                vip,
                2,
                1.0
            );
            objectives.put(objective.getId(), objective);
        }

        // Hold strategic points
        for (Hex hex : gameState.getStrategicPoints()) {
            TacticalObjective objective = new TacticalObjective(
                "hold_" + hex.getCoords().toFriendlyString(),
                ObjectiveType.HOLD_POSITION,
                hex.getCoords(),
                3,
                calculateStrategicValue(hex, gameState)
            );
            objectives.put(objective.getId(), objective);
        }
    }

    /**
     * Identifies offensive objectives like attacking enemy units or breaking through lines.
     *
     * @param gameState The current game state
     */
    private void identifyOffensiveObjectives(GameState gameState) {
        // Group enemy units by role and location
        Map<String, List<Entity>> enemyClusters = identifyEnemyClusters(gameState);

        // Create attack objectives for each cluster
        for (Map.Entry<String, List<Entity>> cluster : enemyClusters.entrySet()) {
            List<Entity> enemies = cluster.getValue();

            if (enemies.isEmpty()) {
                continue;
            }

            // Calculate priority based on threat level and composition
            double priority = calculateThreatLevel(enemies);

            // Create objective
            TacticalObjective objective = new TacticalObjective(
                  "attack_" + cluster.getKey(),
                  ObjectiveType.ATTACK,
                  enemies,
                  enemies.size(),
                  priority
            );
            objectives.put(objective.getId(), objective);
        }
    }

    /**
     * Identifies scouting objectives for reconnaissance.
     *
     * @param gameState The current game state
     */
    private void identifyScoutingObjectives(GameState gameState) {
        // Find unexplored areas
        Set<Coords> unexploredAreas = gameState.getUnexploredAreas();
        int index = 0;
        // Create scouting objectives
        for (Coords area : unexploredAreas) {
            TacticalObjective objective = new TacticalObjective(
                "scout_" + index++,
                ObjectiveType.SCOUT,
                area,
                1,
                0.5
            );
            objectives.put(objective.getId(), objective);
        }
    }

    /**
     * Identifies clusters of enemy units.
     *
     * @param gameState The current game state
     * @return Map of cluster ID to list of units
     */
    private Map<String, List<Entity>> identifyEnemyClusters(GameState gameState) {
        // Simple clustering by distance
        Map<String, List<Entity>> clusters = new HashMap<>();
        List<Entity> unassigned = new ArrayList<>(gameState.getEnemyUnits());

        int clusterIndex = 0;
        while (!unassigned.isEmpty()) {
            Entity seed = unassigned.remove(0);
            List<Entity> cluster = new ArrayList<>();
            cluster.add(seed);

            // Find nearby units
            List<Entity> nearby = unassigned.stream()
                .filter(u -> distance(u.getPosition(), seed.getPosition()) <= 5)
                .toList();

            cluster.addAll(nearby);
            unassigned.removeAll(nearby);

            clusters.put("cluster_" + clusterIndex, cluster);
            clusterIndex++;
        }

        return clusters;
    }

    /**
     * Calculates the center point of a list of coordinates.
     *
     * @param positions List of positions
     * @return The center point
     */
    private Coords calculateCenter(List<Coords> positions) {
        return Coords.average(positions);
    }

    /**
     * Calculates the threat level of a group of enemy units.
     *
     * @param enemies List of enemy units
     * @return A threat score
     */
    private double calculateThreatLevel(List<Entity> enemies) {
        // Sum of individual unit threat levels
        return enemies.stream()
            .mapToDouble(this::calculateUnitThreatLevel)
            .sum();
    }

    /**
     * Calculates the threat level of an individual unit.
     *
     * @param unit The unit to evaluate
     * @return A threat score
     */
    private double calculateUnitThreatLevel(Entity unit) {
        // Would be implemented based on unit capabilities
        int unitInitialBV = unit.getOwner().getInitialBV();
        return unit.getInitialBV() / (double) unitInitialBV;
    }

    /**
     * Calculates the strategic value of a hex.
     *
     * @param hex The hex to evaluate
     * @param gameState The current game state
     * @return A strategic value score
     */
    private double calculateStrategicValue(Hex hex, GameState gameState) {
        // Would be implemented based on terrain, cover, and position
        return 1.0;
    }

    /**
     * Assigns formations to objectives.
     *
     * @param formations All friendly formations
     */
    private void assignFormationsToObjectives(Collection<Formation> formations) {
        // Sort objectives by priority
        List<TacticalObjective> sortedObjectives = objectives.values().stream()
            .sorted((a, b) -> Double.compare(b.getPriority(), a.getPriority()))
            .toList();

        // Sort formations by capability
        Map<UnitRole, List<Formation>> formationsByRole = formations.stream()
            .collect(Collectors.groupingBy(Formation::getPrimaryRole));

        // Assign transport/VIP protection objectives first
        assignObjectivesByType(ObjectiveType.PROTECT, formationsByRole.getOrDefault(UnitRole.JUGGERNAUT, List.of()));
        assignObjectivesByType(ObjectiveType.PROTECT, formationsByRole.getOrDefault(UnitRole.BRAWLER, List.of()));
        assignObjectivesByType(ObjectiveType.PROTECT, formationsByRole.getOrDefault(UnitRole.SKIRMISHER, List.of()));

        // Assign scouting objectives
        assignObjectivesByType(ObjectiveType.SCOUT, formationsByRole.getOrDefault(UnitRole.SCOUT, List.of()));

        // Assign remaining objectives to combat formations
        List<Formation> combatFormations = formations.stream()
            .filter(f -> !formationAssignments.containsKey(f.getId()))
            .collect(Collectors.toList());

        assignRemainingObjectives(combatFormations);
    }

    /**
     * Assigns objectives of a specific type to suitable formations.
     *
     * @param type The objective type
     * @param suitableFormations Formations suitable for this objective type
     */
    private void assignObjectivesByType(ObjectiveType type, List<Formation> suitableFormations) {
        // Get objectives of this type
        List<TacticalObjective> typeObjectives = objectives.values().stream()
            .filter(o -> o.getType() == type)
            .sorted((a, b) -> Double.compare(b.getPriority(), a.getPriority()))
            .toList();

        // Assign formations
        for (int i = 0; i < Math.min(suitableFormations.size(), typeObjectives.size()); i++) {
            Formation formation = suitableFormations.get(i);
            TacticalObjective objective = typeObjectives.get(i);

            formationAssignments.put(formation.getId(), objective.getId());
        }
    }

    /**
     * Assigns remaining objectives to combat formations.
     *
     * @param combatFormations Available combat formations
     */
    private void assignRemainingObjectives(List<Formation> combatFormations) {
        // Get unassigned objectives
        Set<String> assignedObjectives = new HashSet<>(formationAssignments.values());

        List<TacticalObjective> unassignedObjectives = objectives.values().stream()
            .filter(o -> !assignedObjectives.contains(o.getId()))
            .sorted((a, b) -> Double.compare(b.getPriority(), a.getPriority()))
            .collect(Collectors.toList());

        // Assign based on formation capabilities and objective requirements
        for (Formation formation : combatFormations) {
            if (unassignedObjectives.isEmpty()) {
                break;
            }

            // Find the most suitable objective
            TacticalObjective bestObjective = findBestObjective(formation, unassignedObjectives);

            if (bestObjective != null) {
                formationAssignments.put(formation.getId(), bestObjective.getId());
                unassignedObjectives.remove(bestObjective);
            }
        }
    }

    /**
     * Finds the best objective for a formation.
     *
     * @param formation The formation to assign
     * @param availableObjectives Available objectives
     * @return The best matching objective, or null if none
     */
    private TacticalObjective findBestObjective(Formation formation, List<TacticalObjective> availableObjectives) {
        if (availableObjectives.isEmpty()) {
            return null;
        }

        // Simple matching for now - use the highest priority objective
        return availableObjectives.get(0);
    }

    /**
     * Gets the assigned objective for a formation.
     *
     * @param formation The formation to check
     * @return The assigned objective, or null if none
     */
    public TacticalObjective getAssignedObjective(Formation formation) {
        String objectiveId = formationAssignments.get(formation.getId());
        return objectiveId != null ? objectives.get(objectiveId) : null;
    }

    /**
     * Calculates the distance between two positions.
     *
     * @param a First position
     * @param b Second position
     * @return The distance
     */
    private double distance(Coords a, Coords b) {
        // Manhattan distance for hex grid
        return Math.abs(a.getX() - b.getX()) + Math.abs(a.getY() - b.getY());
    }

    private boolean isVIP(Entity unit) {
        // TODO - implement a way to find out if VIP
        return false;
    }

    private boolean isTransport(Entity unit) {
        // TODO - implement a way to find out if transport
        return false;
    }

    /**
     * Represents a tactical objective for formations to pursue.
     */
    public static class TacticalObjective {
        private final String id;
        private final ObjectiveType type;
        private final Coords position;
        private final int[] unitIds;
        private final int requiredUnits;
        private final double priority;

        public TacticalObjective(String id, ObjectiveType type, Coords position,
                                 int requiredUnits, double priority) {
            this.id = id;
            this.type = type;
            this.position = position;
            this.unitIds = new int[0];
            this.requiredUnits = requiredUnits;
            this.priority = priority;
        }

        public TacticalObjective(String id, ObjectiveType type, Entity unit,
                                 int requiredUnits, double priority) {
            this.id = id;
            this.type = type;
            this.position = null;
            this.unitIds = new int[] {unit.getId()};
            this.requiredUnits = requiredUnits;
            this.priority = priority;
        }

        public TacticalObjective(String id, ObjectiveType type, List<Entity> units,
                                 int requiredUnits, double priority) {
            this.id = id;
            this.type = type;
            this.position = null;
            this.unitIds = units.stream().mapToInt(Entity::getId).toArray();
            this.requiredUnits = requiredUnits;
            this.priority = priority;
        }

        public String getId() {
            return id;
        }

        public ObjectiveType getType() {
            return type;
        }

        public Coords getPosition() {
            return position;
        }

        public int getRequiredUnits() {
            return requiredUnits;
        }

        public int[] getUnitIds() {
            return unitIds;
        }

        public double getPriority() {
            return priority;
        }
    }

    /**
     * Types of tactical objectives.
     */
    public enum ObjectiveType {
        ATTACK,
        DEFEND,
        HOLD_POSITION,
        SCOUT,
        PROTECT,
        BREAKTHROUGH,
        RETREAT
    }
}

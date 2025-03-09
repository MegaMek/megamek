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

package megamek.client.bot.common.formation;


import megamek.client.bot.common.UnitClassifier;
import megamek.client.ratgenerator.MissionRole;
import megamek.common.Entity;
import megamek.common.UnitRole;
import megamek.common.util.Counter;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Manages formations of units for coordinated movement and tactics.
 * @author Luana Coppio
 */
public class FormationManager {
    private final Map<String, Formation> formations = new HashMap<>();
    private final Map<Entity, String> unitFormationMap = new HashMap<>();

    /**
     * Creates formations based on unit roles and positions.
     *
     * @param friendlyUnits All friendly units to organize
     */
    public void organizeFormations(List<Entity> friendlyUnits) {
        // Clear existing formations
        formations.clear();
        unitFormationMap.clear();

        // Group units by role

        // Create special-purpose formations first
        createTransportEscortFormations(friendlyUnits);
        createSniperFormations(friendlyUnits);
        createScoutFormations(friendlyUnits);

        // Group remaining combat units into balanced formations
        createCombatFormations(getUnassignedUnits(friendlyUnits));
    }


    /**
     * Creates formations of transports and their escorts.
     *
     * @param units Units grouped by role
     */
    private void createTransportEscortFormations(List<Entity> units) {
        List<Entity> transports = units.stream()
                .filter(e ->
                        UnitClassifier.containsAnyRole(e, Set.of(MissionRole.CIVILIAN, MissionRole.CARGO))).toList();
        List<Entity> escorts = units.stream()
                .filter(e -> UnitClassifier.containsAnyRole(e,
                        Set.of(MissionRole.CAVALRY, MissionRole.INF_SUPPORT, MissionRole.RAIDER))).toList();

        if (transports.isEmpty()) {
            return;
        }

        // Group transports and escorts into formations
        int formationCount = (int) Math.ceil(transports.size() / 3.0);

        for (int i = 0; i < formationCount; i++) {
            Set<Entity> formationUnits = new HashSet<>();

            // Add up to 3 transports per formation
            for (int j = 0; j < 3 && (i * 3 + j) < transports.size(); j++) {
                Entity transport = transports.get(i * 3 + j);
                formationUnits.add(transport);
            }

            // Add escorts proportionally
            int escortsPerFormation = escorts.size() / formationCount;
            for (int j = 0; j < escortsPerFormation && (i * escortsPerFormation + j) < escorts.size(); j++) {
                Entity escort = escorts.get(i * escortsPerFormation + j);
                formationUnits.add(escort);
            }

            // Create the formation
            if (!formationUnits.isEmpty()) {

                Entity leader = findFormationLeader(formationUnits);

                Formation formation = new Formation(
                    "transport_" + i,
                    formationUnits,
                    UnitRole.NONE,
                    leader,
                    Formation.FormationType.COLUMN
                );

                registerFormation(formation);
            }
        }
    }

    /**
     * Creates formations of sniper and missile boat units.
     *
     * @param units Units grouped by role
     */
    private void createSniperFormations(List<Entity> units) {
        List<Entity> rangedUnits = units.stream()
                .filter(e ->
                      UnitClassifier.containsAnyRole(e, Set.of(MissionRole.MIXED_ARTILLERY, MissionRole.ARTILLERY,
                                MissionRole.MISSILE_ARTILLERY, MissionRole.FIRE_SUPPORT))).toList();
        List<Entity> spotters = units.stream()
                .filter(e -> UnitClassifier.containsAnyRole(e,
                        Set.of(MissionRole.SPOTTER))).toList();

        if (rangedUnits.isEmpty()) {
            return;
        }

        // Create balanced ranged formations
        int formationCount = (int) Math.ceil(rangedUnits.size() / 4.0);

        for (int i = 0; i < formationCount; i++) {
            Set<Entity> formationUnits = new HashSet<>();

            // Add ranged units
            for (int j = 0; j < 4 && (i * 4 + j) < rangedUnits.size(); j++) {
                Entity unit = rangedUnits.get(i * 4 + j);
                formationUnits.add(unit);
            }

            // Add a spotter if available
            if (i < spotters.size()) {
                formationUnits.add(spotters.get(i));
            }

            // Create the formation
            if (!formationUnits.isEmpty()) {
                Entity leader = findFormationLeader(formationUnits);

                Formation formation = new Formation(
                    "ranged_" + i,
                    formationUnits,
                    UnitRole.SNIPER,
                    leader,
                    Formation.FormationType.SCATTERED
                );

                registerFormation(formation);
            }
        }
    }

    /**
     * Creates scout formations for reconnaissance.
     *
     * @param units Units grouped by role
     */
    private void createScoutFormations(List<Entity> units) {
        List<Entity> scouts = units.stream().filter(e -> UnitClassifier.containsAnyRole(e, Set.of(MissionRole.RECON))).toList();

        if (scouts.isEmpty()) {
            return;
        }

        // Create small scout groups (1-2 units each)
        int formationCount = (int) Math.ceil(scouts.size() / 2.0);

        for (int i = 0; i < formationCount; i++) {
            Set<Entity> formationUnits = new HashSet<>();

            // Add scouts
            for (int j = 0; j < 2 && (i * 2 + j) < scouts.size(); j++) {
                Entity scout = scouts.get(i * 2 + j);
                formationUnits.add(scout);
            }

            // Create the formation
            if (!formationUnits.isEmpty()) {
                Entity leader = findFormationLeader(formationUnits);

                Formation formation = new Formation(
                    "scout_" + i,
                    formationUnits,
                    UnitRole.SCOUT,
                    leader,
                    Formation.FormationType.SCATTERED
                );

                registerFormation(formation);
            }
        }
    }

    /**
     * Creates balanced combat formations with remaining units.
     *
     * @param unassignedUnits Units not yet assigned to a formation
     */
    private void createCombatFormations(List<Entity> unassignedUnits) {
        if (unassignedUnits.isEmpty()) {
            return;
        }

        // Group by weight class
        Map<WeightClass, List<Entity>> unitsByWeight = unassignedUnits.stream()
                .collect(Collectors.groupingBy(this::determineWeightClass));

        List<Entity> assault = unitsByWeight.getOrDefault(WeightClass.ASSAULT, List.of());
        List<Entity> heavy = unitsByWeight.getOrDefault(WeightClass.HEAVY, List.of());
        List<Entity> medium = unitsByWeight.getOrDefault(WeightClass.MEDIUM, List.of());
        List<Entity> light = unitsByWeight.getOrDefault(WeightClass.LIGHT, List.of());

        // Create balanced formations
        List<List<Entity>> formations = new ArrayList<>();
        int totalUnits = unassignedUnits.size();
        int formationCount = (int) Math.ceil(totalUnits / 4.0);

        for (int i = 0; i < formationCount; i++) {
            List<Entity> formationUnits = new ArrayList<>();
            formations.add(formationUnits);
        }

        // Distribute assault units first, then heavy, medium, light
        distributeUnitsToFormations(assault, formations);
        distributeUnitsToFormations(heavy, formations);
        distributeUnitsToFormations(medium, formations);
        distributeUnitsToFormations(light, formations);

        // Create formation objects
        for (int i = 0; i < formations.size(); i++) {
            List<Entity> formationUnits = formations.get(i);
            if (formationUnits.isEmpty()) {
                continue;
            }

            // Determine formation role based on composition
            UnitRole formationRole = determineFormationRole(formationUnits);

            // Find the best unit to lead
            Entity leader = findFormationLeader(formationUnits);

            // Create the formation
            Formation formation = new Formation(
                "combat_" + i,
                new HashSet<>(formationUnits),
                formationRole,
                leader,
                Formation.FormationType.BOX
            );

            registerFormation(formation);
        }
    }

    /**
     * Distributes units evenly across formations.
     *
     * @param units Units to distribute
     * @param formations List of formation unit lists
     */
    private void distributeUnitsToFormations(List<Entity> units, List<List<Entity>> formations) {
        // Sort formations by size (ascending)
        formations.sort(Comparator.comparingInt(List::size));

        // Distribute units
        for (Entity unit : units) {
            // Add to smallest formation
            formations.get(0).add(unit);

            // Re-sort
            formations.sort(Comparator.comparingInt(List::size));
        }
    }

    /**
     * Determines the weight class of a unit.
     *
     * @param unit The unit to analyze
     * @return The weight class
     */
    private WeightClass determineWeightClass(Entity unit) {
        return WeightClass.values()[Math.min(unit.getWeightClass(), WeightClass.values().length)];
    }

    /**
     * Determines the primary role of a formation based on its composition.
     *
     * @param units The units in the formation
     * @return The determined formation role
     */
    private UnitRole determineFormationRole(List<Entity> units) {
        // Count unit roles
        Counter<UnitRole> roleCounter = new Counter<>(units.stream().map(Entity::getRole).toList());
        // Find the most common role
        return roleCounter.top();
    }

    /**
     * Finds the best unit to lead a formation.
     * @param units Units in the formation
     * @return The selected leader
     */
    private Entity findFormationLeader(Collection<Entity> units) {
        return units.stream()
                .filter(e -> e.isCommander() || e.isC3CompanyCommander())
                .findFirst()
                .orElseGet(() -> units.stream()
                        .filter(e -> !UnitClassifier.containsAnyRole(e, Set.of(MissionRole.CIVILIAN, MissionRole.CARGO)))
                        .findFirst()
                        .orElse(units.iterator().next()));
    }

    /**
     * Gets units that have not been assigned to a formation.
     *
     * @param allUnits All friendly units
     * @return List of unassigned units
     */
    private List<Entity> getUnassignedUnits(List<Entity> allUnits) {
        return allUnits.stream()
            .filter(unit -> !unitFormationMap.containsKey(unit))
            .collect(Collectors.toList());
    }

    /**
     * Registers a formation and its units.
     *
     * @param formation The formation to register
     */
    private void registerFormation(Formation formation) {
        formations.put(formation.getId(), formation);

        for (Entity unit : formation.getMembers()) {
            unitFormationMap.put(unit, formation.getId());
        }
    }

    /**
     * Gets the formation a unit belongs to.
     *
     * @param unit The unit to check
     * @return The formation, or null if not in a formation
     */
    public Optional<Formation> getUnitFormation(Entity unit) {
        String formationId = unitFormationMap.get(unit);
        return formationId != null ? Optional.ofNullable(formations.get(formationId)) : Optional.empty();
    }

    /**
     * Gets all current formations.
     *
     * @return Collection of all formations
     */
    public Collection<Formation> getAllFormations() {
        return formations.values();
    }

    /**
     * Enum for unit weight classes.
     */
    public enum WeightClass {
        LIGHT,
        MEDIUM,
        HEAVY,
        ASSAULT
    }
}

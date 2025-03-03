package megamek.client.bot.caspar;


import megamek.common.Entity;
import megamek.common.UnitRole;

import java.util.*;
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
        Map<UnitRole, List<Entity>> unitsByRole = friendlyUnits.stream()
            .collect(Collectors.groupingBy(this::determineUnitRole));

        // Create special-purpose formations first
        createTransportEscortFormations(unitsByRole);
        createSniperFormations(unitsByRole);
        createScoutFormations(unitsByRole);

        // Group remaining combat units into balanced formations
        createCombatFormations(getUnassignedUnits(friendlyUnits));
    }

    /**
     * Determines the primary role of a unit based on its capabilities.
     *
     * @param unit The unit to analyze
     * @return The determined role
     */
    private UnitRole determineUnitRole(Entity unit) {
        return unit.getRole();
    }

    /**
     * Creates formations of transports and their escorts.
     *
     * @param unitsByRole Units grouped by role
     */
    private void createTransportEscortFormations(Map<UnitRole, List<Entity>> unitsByRole) {
        List<Entity> transports = unitsByRole.getOrDefault(UnitRole.NONE, List.of());
        List<Entity> escorts = unitsByRole.getOrDefault(UnitRole.SKIRMISHER, List.of());
        escorts.addAll(unitsByRole.getOrDefault(UnitRole.BRAWLER, List.of()));

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
                Entity leader = formationUnits.stream()
                    .filter(u -> determineUnitRole(u) != UnitRole.NONE)
                    .findFirst()
                    .orElse(formationUnits.iterator().next());

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
     * @param unitsByRole Units grouped by role
     */
    private void createSniperFormations(Map<UnitRole, List<Entity>> unitsByRole) {
        List<Entity> snipers = unitsByRole.getOrDefault(UnitRole.SNIPER, List.of());
        List<Entity> missileBoats = unitsByRole.getOrDefault(UnitRole.MISSILE_BOAT, List.of());
        List<Entity> spotters = unitsByRole.getOrDefault(UnitRole.SCOUT, List.of());

        List<Entity> rangedUnits = new ArrayList<>();
        rangedUnits.addAll(snipers);
        rangedUnits.addAll(missileBoats);

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
                Entity leader = formationUnits.stream()
                    .filter(u -> determineUnitRole(u) == UnitRole.SNIPER)
                    .findFirst()
                    .orElse(formationUnits.iterator().next());

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
     * @param unitsByRole Units grouped by role
     */
    private void createScoutFormations(Map<UnitRole, List<Entity>> unitsByRole) {
        List<Entity> scouts = unitsByRole.getOrDefault(UnitRole.SCOUT, List.of());

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
                Entity leader = formationUnits.iterator().next();

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
        Map<UnitRole, Long> roleCounts = units.stream()
            .collect(Collectors.groupingBy(this::determineUnitRole, Collectors.counting()));

        // Find the most common role
        return roleCounts.entrySet().stream()
            .max(Map.Entry.comparingByValue())
            .map(Map.Entry::getKey)
            .orElse(UnitRole.BRAWLER);
    }

    /**
     * Finds the best unit to lead a formation.
     *
     * @param units The units in the formation
     * @return The selected leader
     */
    private Entity findFormationLeader(List<Entity> units) {
        // Implementation would pick the most suitable leader
        // For now, just pick the first unit
        return units.get(0);
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
    public Formation getUnitFormation(Entity unit) {
        String formationId = unitFormationMap.get(unit);
        return formationId != null ? formations.get(formationId) : null;
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

/*
 * Copyright (C) 2016-2026 The MegaMek Team. All Rights Reserved.
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
package megamek.client.ratgenerator;

import static megamek.common.units.UnitRole.*;

import java.util.*;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import megamek.common.annotations.Nullable;
import megamek.common.compute.Compute;
import megamek.common.equipment.AmmoType;
import megamek.common.equipment.EquipmentType;
import megamek.common.equipment.MiscType;
import megamek.common.equipment.WeaponType;
import megamek.common.loaders.MekSummary;
import megamek.common.units.EntityMovementMode;
import megamek.common.units.EntityWeightClass;
import megamek.common.units.UnitRole;
import megamek.common.units.UnitType;
import megamek.common.weapons.artillery.ArtilleryWeapon;
import megamek.common.weapons.autoCannons.ACWeapon;
import megamek.common.weapons.autoCannons.LBXACWeapon;
import megamek.common.weapons.autoCannons.UACWeapon;
import megamek.common.weapons.lrms.LRMWeapon;
import megamek.common.weapons.srms.SRMWeapon;
import megamek.common.weapons.tag.TAGWeapon;
import megamek.logging.MMLogger;

/**
 * Defines a Campaign Operations formation type (e.g., Battle Lance, Assault Lance, Aerospace Superiority Squadron),
 * its composition rules, and the logic for generating or validating sets of units against those rules.
 *
 * <p>Each instance represents one named formation: its allowed unit types, weight class bounds, ideal role, main
 * filter ({@link #getMainCriteria}), secondary {@link Constraint}s ({@link #getOtherCriteria}), and an optional
 * {@link GroupingConstraint} for paired or matched-chassis subsets. Instances are registered in a static lookup
 * table populated lazily on first access by {@link #createFormationTypes()}; client code retrieves a formation by
 * name through {@link #getFormationType(String)} or iterates all of them with {@link #getAllFormations()}.
 *
 * <p>The two main entry points are:
 * <ul>
 *   <li>{@link #generateFormation(Parameters, int, int, boolean)} (and its overloads) — builds a list of
 *       {@link MekSummary} units that satisfy the formation's rules, sampled from a {@link UnitTable}.</li>
 *   <li>{@link #qualifies(List)} — tests whether an existing list of units already meets the formation's rules.</li>
 * </ul>
 *
 * @author Neoancient
 * @see Constraint
 * @see GroupingConstraint
 */
public class FormationType {
    private static final MMLogger LOGGER = MMLogger.create(FormationType.class);

    /** Bit flag identifying {@link UnitType#MEK} units. */
    public static final int FLAG_MEK = 1 << UnitType.MEK;
    /** Bit flag identifying {@link UnitType#TANK} units. */
    public static final int FLAG_TANK = 1 << UnitType.TANK;
    /** Bit flag identifying {@link UnitType#BATTLE_ARMOR} units. */
    public static final int FLAG_BATTLE_ARMOR = 1 << UnitType.BATTLE_ARMOR;
    /** Bit flag identifying {@link UnitType#INFANTRY} units. */
    public static final int FLAG_INFANTRY = 1 << UnitType.INFANTRY;
    /** Bit flag identifying {@link UnitType#PROTOMEK} units. */
    public static final int FLAG_PROTOMEK = 1 << UnitType.PROTOMEK;
    /** Bit flag identifying {@link UnitType#VTOL} units. */
    public static final int FLAG_VTOL = 1 << UnitType.VTOL;
    /** Bit flag identifying {@link UnitType#NAVAL} units. */
    public static final int FLAG_NAVAL = 1 << UnitType.NAVAL;

    /** Bit flag identifying {@link UnitType#CONV_FIGHTER} units. */
    public static final int FLAG_CONV_FIGHTER = 1 << UnitType.CONV_FIGHTER;
    /** Bit flag identifying {@link UnitType#AEROSPACE_FIGHTER} units. */
    public static final int FLAG_AERO = 1 << UnitType.AEROSPACE_FIGHTER;
    /** Bit flag identifying {@link UnitType#SMALL_CRAFT} units. */
    public static final int FLAG_SMALL_CRAFT = 1 << UnitType.SMALL_CRAFT;
    /** Bit flag identifying {@link UnitType#DROPSHIP} units. */
    public static final int FLAG_DROPSHIP = 1 << UnitType.DROPSHIP;

    /** Composite flag covering all ground unit types (Mek, Tank, BA, Infantry, ProtoMek, VTOL, Naval). */
    public static final int FLAG_GROUND = FLAG_MEK |
          FLAG_TANK |
          FLAG_BATTLE_ARMOR |
          FLAG_INFANTRY |
          FLAG_PROTOMEK |
          FLAG_VTOL |
          FLAG_NAVAL;

    /**
     * Composite flag covering ground unit types excluding Infantry and VTOL. Used by formations whose rules implicitly
     * assume armored/heavy ground forces (e.g., Battle Lance variants). Note: the {@code _NO_LIGHT} suffix refers to
     * omitting these unit-type categories, not weight class.
     */
    public static final int FLAG_GROUND_NO_LIGHT = FLAG_MEK |
          FLAG_TANK |
          FLAG_BATTLE_ARMOR |
          FLAG_PROTOMEK |
          FLAG_NAVAL;

    /** Composite flag covering aerospace and conventional fighters. */
    public static final int FLAG_FIGHTER = FLAG_CONV_FIGHTER | FLAG_AERO;
    /** Composite flag covering all airborne categories (fighters, small craft, dropships). */
    public static final int FLAG_AIR = FLAG_CONV_FIGHTER | FLAG_AERO | FLAG_SMALL_CRAFT | FLAG_DROPSHIP;
    /** Composite flag covering combat vehicles (Tank, Naval, VTOL). */
    public static final int FLAG_VEHICLE = FLAG_TANK | FLAG_NAVAL | FLAG_VTOL;
    /** Composite flag covering every supported unit type. */
    public static final int FLAG_ALL = FLAG_GROUND | FLAG_AIR;

    private static HashMap<String, FormationType> allFormationTypes = null;

    /**
     * Returns the formation registered under the given name, lazily initializing the registry on first call.
     *
     * @param key the formation name (e.g., {@code "Assault"}, {@code "Recon"},
     *            {@code "Aerospace Superiority Squadron"})
     *
     * @return the matching {@link FormationType}, or {@code null} if no formation is registered under that name
     */
    public static FormationType getFormationType(String key) {
        if (allFormationTypes == null) {
            createFormationTypes();
        }
        return allFormationTypes.get(key);
    }

    /**
     * Returns every registered formation, lazily initializing the registry on first call. The returned collection is
     * backed by the registry; callers should treat it as read-only.
     *
     * @return all registered formation types
     */
    public static Collection<FormationType> getAllFormations() {
        if (allFormationTypes == null) {
            createFormationTypes();
        }
        return allFormationTypes.values();
    }

    /**
     * Constructs a formation whose category is the same as its name (used for top-level formations that are not a
     * variant of another). Subclasses populate the remaining fields by direct access before registering.
     *
     * @param name the formation name; also used as the category
     */
    protected FormationType(String name) {
        this(name, name);
    }

    /**
     * Constructs a formation with an explicit category. Used for variants (e.g., {@code "Heavy Battle"} in category
     * {@code "Battle"}). Subclasses populate the remaining fields by direct access before registering.
     *
     * @param name     the formation name
     * @param category the parent/grouping category for organizational purposes
     */
    protected FormationType(String name, String category) {
        this.name = name;
        this.category = category;
    }

    private final String name;
    private final String category;
    private int allowedUnitTypes = FLAG_GROUND;

    // Some formation types allow units not normally generated for general combat roles (e.g., artillery, cargo)
    private final EnumSet<MissionRole> missionRoles = EnumSet.noneOf(MissionRole.class);

    // If all units in the force have this role, other constraints can be ignored.
    private UnitRole idealRole = UnitRole.UNDETERMINED;
    private String exclusiveFaction = null;

    private int minWeightClass = 0;
    private int maxWeightClass = EntityWeightClass.WEIGHT_COLOSSAL;

    // Used as a filter when generating units
    private Predicate<MekSummary> mainCriteria = ms -> true;

    // Additional criteria that have to be fulfilled by a portion of the force
    private final List<Constraint> otherCriteria = new ArrayList<>();
    private GroupingConstraint groupingCriteria = null;

    // Provide values for the various criteria for reporting purposes
    private String mainDescription = null;
    private final Map<String, Function<MekSummary, ?>> reportMetrics = new HashMap<>();

    /** @return the formation's display name (e.g., {@code "Heavy Battle"}). */
    public String getName() {
        return name;
    }

    /**
     * @return the formation's category, which groups variants (e.g., {@code "Battle"} for {@code "Heavy Battle"}).
     *       Top-level formations have a category equal to their name.
     */
    public String getCategory() {
        return category;
    }

    /**
     * Tests whether the given {@link UnitType} value is included in this formation's allowed unit types.
     *
     * @param ut a {@link UnitType} constant (the int value, not a {@code FLAG_*} mask)
     *
     * @return {@code true} if units of that type may participate in this formation
     */
    public boolean isAllowedUnitType(int ut) {
        return (allowedUnitTypes & (1 << ut)) != 0;
    }

    /**
     * @return {@code true} if this formation does not allow aerospace fighters; that is, it is a ground formation.
     *       Convenience for distinguishing ground lances from aerospace squadrons.
     */
    public boolean isGround() {
        return (allowedUnitTypes & FLAG_AERO) == 0;
    }

    /**
     * @return the formation name, suffixed with the faction key in parentheses if the formation is exclusive to a
     *       single faction (e.g., {@code "Anvil (FWL)"}). Returns just the name if not faction-exclusive.
     */
    public String getNameWithFaction() {
        return exclusiveFaction == null ? name : name + " (" + exclusiveFaction + ")";
    }

    /**
     * Returns a stable resource-bundle key for this formation's tooltip text, suitable for lookup in
     * {@code megamek.client.messages}. The key has the form {@code FormationType.<sanitizedName>.tooltip}, where spaces
     * and forward slashes in the formation name are replaced with underscores so the key is a valid properties
     * identifier.
     *
     * <p>UI code is expected to call {@code Messages.getString(ft.getTooltipKey())} (with a missing-key fallback)
     * rather than constructing the key inline. This keeps {@link FormationType} ignorant of the UI Messages bundle
     * while letting per-formation tooltip strings live in {@code messages.properties}.
     *
     * @return the resource-bundle key for this formation's tooltip
     */
    public String getTooltipKey() {
        return "FormationType." + name.replace(' ', '_').replace('/', '_') + ".tooltip";
    }

    /**
     * @return the minimum allowed {@link EntityWeightClass} for units in this formation; {@code 0} (no minimum) by
     *       default
     */
    public int getMinWeightClass() {
        return minWeightClass;
    }

    /**
     * @return the maximum allowed {@link EntityWeightClass} for units in this formation;
     *       {@link EntityWeightClass#WEIGHT_COLOSSAL} (no maximum) by default
     */
    public int getMaxWeightClass() {
        return maxWeightClass;
    }

    /**
     * @return mission roles applied to RAT generation for this formation. Some formations admit units in normally
     *       non-combat roles (e.g., {@link MissionRole#MIXED_ARTILLERY} for Anti-Air and Artillery Fire Lances).
     */
    public Set<MissionRole> getMissionRoles() {
        return missionRoles;
    }

    /**
     * @return the ideal {@link UnitRole} for this formation. If every unit in a candidate force has this role, the
     *       formation's other constraints are bypassed (CamOps "ideal role" loophole). Returns
     *       {@link UnitRole#UNDETERMINED} for formations without an ideal role.
     */
    public UnitRole getIdealRole() {
        return idealRole;
    }

    /** @return the predicate every unit must satisfy to participate in this formation. */
    public Predicate<MekSummary> getMainCriteria() {
        return mainCriteria;
    }

    /**
     * @return a human-readable description of the main criteria (e.g., {@code "Armor 105+"}), suitable for UI display.
     *       May be {@code null} if no main criteria are imposed.
     */
    public String getMainDescription() {
        return mainDescription;
    }

    /**
     * @return an iterator over secondary {@link Constraint}s — count, percent, and paired-OR rules that a portion of
     *       the force must satisfy
     */
    public Iterator<Constraint> getOtherCriteria() {
        return otherCriteria.iterator();
    }

    /**
     * @return the optional {@link GroupingConstraint} for paired or matched-chassis subsets, or {@code null} if this
     *       formation does not impose grouping
     */
    public GroupingConstraint getGroupingCriteria() {
        return groupingCriteria;
    }

    /**
     * @return iterator over the keys of report-metric extractors registered for this formation. Used by UI to display
     *       per-unit diagnostic columns alongside the formation's qualifications report.
     */
    public Iterator<String> getReportMetricKeys() {
        return reportMetrics.keySet().iterator();
    }

    /**
     * @param key a report-metric key from {@link #getReportMetricKeys()}
     *
     * @return the function that extracts the metric value for a given unit, or {@code null} if no such metric is
     *       registered
     */
    public Function<MekSummary, ?> getReportMetric(String key) {
        return reportMetrics.get(key);
    }

    private static Set<MissionRole> getMissionRoles(MekSummary ms) {
        ModelRecord mRec = RATGenerator.getInstance().getModelRecord(ms.getName());
        return mRec == null ? EnumSet.noneOf(MissionRole.class) : mRec.getRoles();
    }

    private static IntSummaryStatistics damageAtRangeStats(MekSummary ms, int range) {
        List<Integer> retVal = new ArrayList<>();
        for (int i = 0; i < ms.getEquipmentNames().size(); i++) {
            if (EquipmentType.get(ms.getEquipmentNames().get(i)) instanceof WeaponType weapon) {
                if (weapon.getLongRange() < range) {
                    continue;
                }
                int damage = 0;
                if (weapon.getAmmoType() != AmmoType.AmmoTypeEnum.NA) {
                    Optional<EquipmentType> ammo = ms.getEquipmentNames()
                          .stream()
                          .map(EquipmentType::get)
                          .filter(eq -> eq instanceof AmmoType &&
                                ((AmmoType) eq).getAmmoType() ==
                                      weapon.getAmmoType() &&
                                ((AmmoType) eq).getRackSize() ==
                                      weapon.getRackSize())
                          .findFirst();
                    if (ammo.isPresent()) {
                        damage = ((AmmoType) ammo.get()).getDamagePerShot() *
                              Math.max(1, ((AmmoType) ammo.get()).getRackSize());
                    }
                } else {
                    damage = weapon.getDamage(range);
                }
                if (damage > 0) {
                    for (int j = 0; j < ms.getEquipmentQuantities().get(i); j++) {
                        retVal.add(damage);
                    }
                }
            }
        }
        return retVal.stream().mapToInt(Integer::intValue).summaryStatistics();
    }

    private static long getDamageAtRange(MekSummary ms, int range) {
        return Math.max(0, damageAtRangeStats(ms, range).getSum());
    }

    private static long getSingleWeaponDamageAtRange(MekSummary ms, int range) {
        return Math.max(0, damageAtRangeStats(ms, range).getMax());
    }

    private static int getNetworkMask(MekSummary ms) {
        ModelRecord mRec = RATGenerator.getInstance().getModelRecord(ms.getName());
        return mRec == null ? ModelRecord.NETWORK_NONE : mRec.getNetworkMask();
    }

    /**
     * Convenience overload of {@link #generateFormation(List, List, int, boolean, int, int)} for formations whose units
     * all share a single set of {@link Parameters} (the common single-unit-type case).
     *
     * @param params      the RAT generation parameters
     * @param numUnits    the number of units to generate
     * @param networkMask C3/C3i/Nova network requirement (use {@link ModelRecord#NETWORK_NONE} for no requirement)
     * @param bestEffort  if {@code true}, returns a partial result when not all constraints can be met
     *
     * @return the generated units, or an empty list if the formation could not be built and {@code bestEffort} is
     *       {@code false}
     */
    public List<MekSummary> generateFormation(Parameters params, int numUnits, int networkMask, boolean bestEffort) {
        List<Parameters> parametersArrayList = new ArrayList<>();
        parametersArrayList.add(params);
        List<Integer> numUnitsIDList = new ArrayList<>();
        numUnitsIDList.add(numUnits);
        return generateFormation(parametersArrayList, numUnitsIDList, networkMask, bestEffort, -1, -1);
    }

    /**
     * Overload of {@link #generateFormation(List, List, int, boolean, int, int)} that uses the formation's own grouping
     * configuration without per-call overrides.
     *
     * @param params      one {@link Parameters} per unit-type group
     * @param numUnits    one count per {@link Parameters}, parallel to {@code params}
     * @param networkMask C3/C3i/Nova network requirement
     * @param bestEffort  if {@code true}, returns a partial result when not all constraints can be met
     *
     * @return the generated units, or an empty list if the formation could not be built and {@code bestEffort} is
     *       {@code false}
     */
    public List<MekSummary> generateFormation(List<Parameters> params, List<Integer> numUnits, int networkMask,
          boolean bestEffort) {
        return generateFormation(params, numUnits, networkMask, bestEffort, -1, -1);
    }

    /**
     * Resolves the weight classes one parameter set will draw from. Starts from this formation's own min/max range (the
     * air range drops Assault for fighters), then intersects it with any weight classes the caller requested (the Force
     * Generator passes the lance's element weights). An empty intersection means the request is incompatible with the
     * formation, so it falls back to the formation's own range rather than failing.
     *
     * @param parameters  the parameter set to update in place
     * @param groundRange the formation's full weight-class range (used for ground unit types)
     * @param airRange    the formation's weight-class range with Assault removed (used for fighters)
     */
    private void applyFormationWeightClasses(Parameters parameters, List<Integer> groundRange,
          List<Integer> airRange) {
        parameters.addRoles(missionRoles);
        List<Integer> formationRange = (parameters.getUnitType() < UnitType.CONV_FIGHTER) ? groundRange : airRange;
        Collection<Integer> requested = parameters.getWeightClasses();
        if (requested.isEmpty()) {
            parameters.setWeightClasses(formationRange);
            LOGGER.debug("[ForceGen][Formation]   weightIntersect: requested=[] formationRange={} -> final={}"
                  + " (no caller weight; using formation range)", formationRange, parameters.getWeightClasses());
            return;
        }
        List<Integer> intersection = requested.stream()
              .filter(formationRange::contains)
              .collect(Collectors.toList());
        parameters.setWeightClasses(intersection.isEmpty() ? formationRange : intersection);
        LOGGER.debug("[ForceGen][Formation]   weightIntersect: requested={} formationRange={} -> final={}{}",
              requested, formationRange, parameters.getWeightClasses(),
              intersection.isEmpty() ? " (EMPTY intersection; fell back to formation range)" : "");
    }

    /**
     * Builds a list of units that satisfy this formation's rules, sampled from {@link UnitTable}s derived from the
     * provided parameters. Handles paired-OR constraints, network role distribution (C3 master/slave, C3i, Nova), mixed
     * unit types via parallel parameter sets, and movement-mode resolution for vehicles/infantry whose mode is left
     * unspecified.
     *
     * <p>If the formation defines an {@link #getIdealRole() ideal role} and direct generation falls short of all
     * constraints, falls back to attempting an all-ideal-role formation (CamOps loophole).
     *
     * @param params      one {@link Parameters} per unit-type group; size must match {@code numUnits}
     * @param numUnits    the count of units to generate per parameter group
     * @param networkMask the C3/C3i/Nova network requirement encoded as a {@link ModelRecord} {@code NETWORK_*}
     *                    bitmask; pass {@link ModelRecord#NETWORK_NONE} to skip network requirements
     * @param bestEffort  if {@code true}, returns whatever could be generated when constraints cannot be fully met; if
     *                    {@code false}, returns an empty list on failure
     * @param groupSize   override for the formation's grouping constraint group size; pass {@code -1} to use the
     *                    formation's own value
     * @param nGroups     override for the formation's grouping constraint group count; pass {@code -1} to use the
     *                    formation's own value
     *
     * @return the generated units (concatenated across parameter groups, in input order), or an empty list on failure
     *       when {@code bestEffort} is {@code false}
     *
     * @throws IllegalArgumentException if {@code params} and {@code numUnits} have different sizes or are empty
     */
    public List<MekSummary> generateFormation(List<Parameters> params, List<Integer> numUnits, int networkMask,
          boolean bestEffort, int groupSize, int nGroups) {
        if (params.size() != numUnits.size() || params.isEmpty()) {
            throw new IllegalArgumentException(
                  "Formation parameter list and numUnit list must have the same number of elements.");
        }

        LOGGER.debug("[ForceGen][Formation] ENTER formation='{}' minWC={} maxWC={} idealRole={} bestEffort={}"
                    + " networkMask={} paramSets={} totalUnits={}",
              name, minWeightClass, maxWeightClass, idealRole, bestEffort, networkMask, params.size(),
              numUnits.stream().mapToInt(Integer::intValue).sum());
        for (int paramIndex = 0; paramIndex < params.size(); paramIndex++) {
            Parameters parameters = params.get(paramIndex);
            LOGGER.debug("[ForceGen][Formation]   param[{}] unitType={} requestedWC={} roles={} moves={} numUnits={}",
                  paramIndex, parameters.getUnitType(), parameters.getWeightClasses(), parameters.getRoles(),
                  parameters.getMovementModes(), numUnits.get(paramIndex));
        }

        final GroupingConstraint useGrouping;
        if (null == groupingCriteria) {
            useGrouping = null;
        } else {
            useGrouping = groupingCriteria.copy();
            if (groupSize > 0) {
                useGrouping.groupSize = groupSize;
                useGrouping.numGroups = 0;
            }
            if (nGroups > 0) {
                useGrouping.numGroups = nGroups;
                useGrouping.groupSize = 0;
            }
        }

        List<Integer> weightClasses = IntStream.rangeClosed(minWeightClass,
              Math.min(maxWeightClass, EntityWeightClass.WEIGHT_SUPER_HEAVY)).boxed().collect(Collectors.toList());
        List<Integer> airWeightClasses = weightClasses.stream()
              .filter(weightClass -> weightClass < EntityWeightClass.WEIGHT_ASSAULT)
              .collect(Collectors.toList());

        params.forEach(parameters -> applyFormationWeightClasses(parameters, weightClasses, airWeightClasses));

        List<UnitTable> tables = params.stream().map(UnitTable::findTable).toList();

        // If there are any parameter sets that cannot generate a table, return an empty list.
        if (!tables.stream().allMatch(UnitTable::hasUnits) && !bestEffort) {
            return new ArrayList<>();
        }

        /*
         * Check whether we have vees or infantry that do not have the movement mode(s) set. If so, we will attempt
         * to conform them to a single type. Any that are a set are ignored; there is no attempt to conform to the mode
         * already in the force. If they are intended to conform, they ought to be set.
         */
        List<Integer> undeterminedVees = new ArrayList<>();
        List<Integer> undeterminedInfantry = new ArrayList<>();
        for (int i = 0; i < params.size(); i++) {
            if (params.get(i).getMovementModes().isEmpty()) {
                if (params.get(i).getUnitType() == UnitType.TANK) {
                    undeterminedVees.add(i);
                }
                if (params.get(i).getUnitType() == UnitType.INFANTRY) {
                    undeterminedInfantry.add(i);
                }
            }
        }
        /*
         * Look at the table for each group of parameters and determine the motive type ratio, then weight those
         * values according to the number of units using those parameters.
         */
        Map<String, Integer> veeMap = new HashMap<>();
        Map<String, Integer> infMap = new HashMap<>();

        for (int i = 0; i < undeterminedVees.size(); i++) {
            for (int j = 0; j < tables.get(i).getNumEntries(); j++) {
                if (tables.get(i).getMekSummary(j) != null) {
                    veeMap.merge(tables.get(i).getMekSummary(j).getUnitSubType(),
                          tables.get(i).getEntryWeight(j) * numUnits.get(i),
                          Integer::sum);
                }
            }
        }

        for (int i = 0; i < undeterminedInfantry.size(); i++) {
            for (int j = 0; j < tables.get(i).getNumEntries(); j++) {
                if (tables.get(i).getMekSummary(j) != null) {
                    infMap.merge(tables.get(i).getMekSummary(j).getUnitSubType(),
                          tables.get(i).getEntryWeight(j) * numUnits.get(i),
                          Integer::sum);
                }
            }
        }

        /*
         * Order modes in a way that those modes that are better represented are more likely to be attempted first.
         */
        List<String> veeModeAttemptOrder = new ArrayList<>();
        List<String> infModeAttemptOrder = new ArrayList<>();

        while (!veeMap.isEmpty()) {
            int total = veeMap.values().stream().mapToInt(Integer::intValue).sum();
            int r = Compute.randomInt(total);
            String mode = "Tracked";
            for (String m : veeMap.keySet()) {
                if (r < veeMap.get(m)) {
                    mode = m;
                    break;
                } else {
                    r -= veeMap.get(m);
                }
            }
            veeModeAttemptOrder.add(mode);
            veeMap.remove(mode);
        }

        while (!infMap.isEmpty()) {
            int total = infMap.values().stream().mapToInt(Integer::intValue).sum();
            int r = Compute.randomInt(total);
            String mode = "Leg";
            for (String m : infMap.keySet()) {
                if (r < infMap.get(m)) {
                    mode = m;
                    break;
                } else {
                    r -= infMap.get(m);
                }
            }
            infModeAttemptOrder.add(mode);
            infMap.remove(mode);
        }

        /*
         * if there are no units of a given type, we want to make sure we have at least one iteration
         */
        if (veeModeAttemptOrder.isEmpty() && !infModeAttemptOrder.isEmpty()) {
            veeModeAttemptOrder.add("Tracked");
        }
        if (infModeAttemptOrder.isEmpty() && !veeModeAttemptOrder.isEmpty()) {
            infModeAttemptOrder.add("Leg");
        }
        for (String veeMode : veeModeAttemptOrder) {
            for (String infMode : infModeAttemptOrder) {
                List<Parameters> tempParams = params.stream().map(Parameters::copy).collect(Collectors.toList());
                for (int index : undeterminedVees) {
                    tempParams.get(index).addMovementMode(EntityMovementMode.parseFromString(veeMode));
                }
                for (int index : undeterminedInfantry) {
                    tempParams.get(index).addMovementMode(EntityMovementMode.parseFromString(infMode));
                }
                List<MekSummary> list = generateFormation(tempParams, numUnits, networkMask, false);
                if (!list.isEmpty()) {
                    return list;
                }
            }
        }
        /*
         * If we cannot meet all criteria with a specific motive type, try without
         * respect to the motive type
         */

        int cUnits = numUnits.stream().mapToInt(Integer::intValue).sum();

        /* Simple case: all units have the same requirements. */
        if (otherCriteria.isEmpty() && useGrouping == null && networkMask == ModelRecord.NETWORK_NONE) {
            List<MekSummary> retVal = new ArrayList<>();
            for (int i = 0; i < params.size(); i++) {
                retVal.addAll(tables.get(i).generateUnits(numUnits.get(i), ms -> mainCriteria.test(ms)));
            }
            LOGGER.debug("[ForceGen][Formation] path=simple-case(mainCriteria only) primaryResult={}/{} units={}",
                  retVal.size(), cUnits, summarize(retVal));
            if (retVal.size() < cUnits) {
                List<MekSummary> matchRole = tryIdealRole(params, numUnits);
                if (matchRole != null) {
                    LOGGER.debug("[ForceGen][Formation] path=simple-case -> tryIdealRole SUCCESS units={}",
                          summarize(matchRole));
                    return matchRole;
                }
                LOGGER.debug("[ForceGen][Formation] path=simple-case -> tryIdealRole null; returning partial {}",
                      summarize(retVal));
            }
            return retVal;
        }

        /* Simple case: a single set of parameters and a single additional criterion. */
        if (params.size() == 1 &&
              otherCriteria.size() == 1 &&
              useGrouping == null &&
              networkMask == ModelRecord.NETWORK_NONE) {
            List<MekSummary> retVal = new ArrayList<>();
            int criterionMin = otherCriteria.getFirst().getMinimum(numUnits.getFirst());
            retVal.addAll(tables.getFirst()
                  .generateUnits(criterionMin,
                        ms -> mainCriteria.test(ms) && otherCriteria.getFirst().criterion.test(ms)));
            LOGGER.debug("[ForceGen][Formation] path=single-criterion('{}') constraintMin={} satisfied={}/{} units={}",
                  otherCriteria.getFirst().description, criterionMin, retVal.size(), criterionMin, summarize(retVal));
            if (retVal.size() < criterionMin) {
                List<MekSummary> onRole = tryIdealRole(params, numUnits);
                if (onRole != null) {
                    LOGGER.debug("[ForceGen][Formation] path=single-criterion -> tryIdealRole SUCCESS units={}",
                          summarize(onRole));
                    return onRole;
                } else if (!bestEffort) {
                    LOGGER.debug("[ForceGen][Formation] path=single-criterion -> tryIdealRole null, bestEffort=false;"
                          + " returning EMPTY");
                    return new ArrayList<>();
                }
                LOGGER.debug("[ForceGen][Formation] path=single-criterion -> tryIdealRole null, bestEffort=true;"
                      + " filling remainder with mainCriteria");
            }
            if (retVal.size() >= criterionMin || bestEffort) {
                retVal.addAll(tables.getFirst()
                      .generateUnits(numUnits.getFirst() - retVal.size(), ms -> mainCriteria.test(ms)));
            }
            LOGGER.debug("[ForceGen][Formation] path=single-criterion FINAL units={}", summarize(retVal));
            return retVal;
        }

        LOGGER.debug("[ForceGen][Formation] path=complex (otherCriteria={} grouping={} network={})",
              otherCriteria.size(), useGrouping != null, networkMask != ModelRecord.NETWORK_NONE);

        /*
         * If a network is indicated, we decide which units are part of the network (usually all, but not
         * necessarily) and which combination to use, then assign one of them to the primary role if any. A company
         * command lance has two configuration options: a unit with two masters, or two masters and two slaves.
         */
        int numNetworked = 0;
        int numMasters = 0;
        int altNumMasters = 0;
        int masterType = ModelRecord.NETWORK_NONE;
        int slaveType = ModelRecord.NETWORK_NONE;
        int validNetworkUnits = FLAG_MEK | FLAG_VEHICLE | FLAG_BATTLE_ARMOR;

        if ((networkMask & ModelRecord.NETWORK_C3_MASTER) != 0) {
            numNetworked = 4;
            numMasters = 1;
            masterType = networkMask | (networkMask & ModelRecord.NETWORK_BOOSTED);
            slaveType = ModelRecord.NETWORK_C3_SLAVE | (networkMask & ModelRecord.NETWORK_BOOSTED);
            if ((networkMask & ModelRecord.NETWORK_COMPANY_COMMAND) != 0) {
                altNumMasters = 2;
            }
        } else if ((networkMask & ModelRecord.NETWORK_C3I) != 0) {
            numNetworked = 6;
            slaveType = ModelRecord.NETWORK_C3I;
            /* This mask is also used for naval C3 */
            validNetworkUnits |= FLAG_SMALL_CRAFT | FLAG_DROPSHIP;
        } else if ((networkMask & ModelRecord.NETWORK_NOVA) != 0) {
            numNetworked = 3;
            slaveType = ModelRecord.NETWORK_NOVA;
        }
        int networkEligible = 0;
        for (int i = 0; i < params.size(); i++) {
            if ((validNetworkUnits & (1 << params.get(i).getUnitType())) != 0) {
                networkEligible += numUnits.get(i);
            }
        }
        if (numNetworked > networkEligible) {
            numNetworked = networkEligible;
        }

        /*
         * General case:
         * Select randomly from all unique combinations of the various criteria. Each combination is represented by a
         * <code>Map&lt;Integer, Integer&gt;</code> in which the various criteria are encoded as the keys and the
         * value mapped to the index is the number of units that must fulfill those criteria. The lowest order bits
         * map to otherCriteria, one bit for each constraint. These are built by shifting left for each new one
         * added, so the one at index 0 is the leftmost bit of this section. A 1 indicates that the number of units
         * at that index must meet the constraint, while a 0 means the constraint is not tested, and a unit may or
         * may not fulfill it.
         *
         * Example: if otherCriteria.size() == 3, then the value of combinations[6] is the number of units that must
         * meet the first two constraints (110), while combinations[7] must meet all three and combinations[0] need
         * not meet any.
         *
         * The next three bits indicate C3 network requirements. The lowest order is the number that must have a C3
         * slave, C3i, NC3, or Nova, depending on the value of networkMask. The middle bit is the number of required
         * C3 masters, and the highest bit is the number of dual-C3M units. Note that only one of these three bits
         * can be set; while a unit can have a C3M and a C3S, only one can fulfill its role in the network.
         *
         * The highest order section is the unit type. Each element of the params list has one bit, beginning with
         * the lowest order a bit at index 0. As with networks, only one bit in this section can be set.
         */

        do {
            List<Map<Integer, Integer>> combinations;
            /*
             * We can get here with an empty otherCriteria if there is a groupingConstraint, which is the case with
             * the Order formation.
             */
            if (otherCriteria.isEmpty()) {
                Map<Integer, Integer> combo = new HashMap<>();
                combo.put(0, cUnits);
                combinations = new ArrayList<>();
                combinations.add(combo);
            } else {
                combinations = findCombinations(cUnits);
            }
            // Group units by param index, so they can be returned in the order requested.
            Map<Integer, List<MekSummary>> list = new TreeMap<>();
            final int POS_C3S = 0;
            final int POS_C3M = 1;
            final int POS_C3MM = 2;
            final int POS_C3_NUM = 3;
            while (!combinations.isEmpty()) {
                int index = Compute.randomInt(combinations.size());
                Map<Integer, Integer> baseCombo = combinations.get(index);

                int[] networkGroups = new int[POS_C3_NUM];
                networkGroups[POS_C3S] = Math.max(0, numNetworked - numMasters);
                if ((networkMask & ModelRecord.NETWORK_COMPANY_COMMAND) == 0) {
                    networkGroups[POS_C3M] = numMasters;
                } else {
                    networkGroups[POS_C3MM] = numMasters;
                }
                List<Map<Integer, Integer>> networkGroupings = findGroups(baseCombo,
                      networkGroups,
                      otherCriteria.size());
                if (altNumMasters > 0) {
                    networkGroups[POS_C3S] = Math.max(0, numNetworked - altNumMasters);
                    networkGroups[POS_C3M] = altNumMasters;
                    networkGroups[POS_C3MM] = 0;
                    networkGroupings.addAll(findGroups(baseCombo, networkGroups, otherCriteria.size()));
                }

                while (!networkGroupings.isEmpty()) {
                    list.clear();
                    int networkIndex = Compute.randomInt(networkGroupings.size());
                    Map<Integer, Integer> combo = networkGroupings.get(networkIndex);

                    int[] unitsPerGroup = new int[params.size()];
                    for (int i = 0; i < numUnits.size(); i++) {
                        unitsPerGroup[i] = numUnits.get(i);
                    }
                    List<Map<Integer, Integer>> unitTypeGroupings = findGroups(combo,
                          unitsPerGroup,
                          otherCriteria.size() + POS_C3_NUM);
                    while (!unitTypeGroupings.isEmpty()) {
                        list.clear();
                        int utIndex = Compute.randomInt(unitTypeGroupings.size());
                        combo = unitTypeGroupings.get(utIndex);

                        if (useGrouping != null &&
                              params.stream().anyMatch(p -> useGrouping.appliesTo(p.getUnitType()))) {
                            /*
                             * Create a temporary map that only includes units that have a grouping
                             * criterion
                             */
                            Map<Integer, Integer> groupedUnits = new LinkedHashMap<>();
                            for (int p = 0; p < params.size(); p++) {
                                if (useGrouping.appliesTo(params.get(p).getUnitType())) {
                                    for (Integer i : combo.keySet()) {
                                        if ((i & (1 << (p + otherCriteria.size() + POS_C3_NUM))) != 0) {
                                            groupedUnits.merge(i, combo.get(i), Integer::sum);
                                        }
                                    }
                                }
                            }
                            List<List<Map<Integer, Integer>>> groups = findMatchedGroups(groupedUnits, useGrouping);

                            while (!groups.isEmpty()) {
                                int gIndex = Compute.randomInt(groups.size());
                                list.clear();
                                Map<Integer, List<MekSummary>> found = new TreeMap<>();
                                Map<Integer, Integer> workingCombo = new HashMap<>(combo);
                                for (Map<Integer, Integer> g : groups.get(gIndex)) {
                                    /*
                                     * The first unit selected may lead to a dead end if the constraints for the
                                     * other group members cannot be met in a unit that matches the base. To deal
                                     * with this, we make a second attempt if necessary, subjecting all members of
                                     * the group to all constraints assigned to any group member.
                                     */
                                    int extraCriteria = 0;
                                    int attempts = 0;
                                    while (attempts < 2) {
                                        found.clear();
                                        MekSummary base = null;
                                        for (int i : combo.keySet()) {
                                            if (g.containsKey(i)) {
                                                // Decode unit type
                                                int tableIndex = getTableIndex(params, i, POS_C3_NUM);
                                                final Predicate<MekSummary> filter = getFilterFromIndex(i |
                                                            extraCriteria,
                                                      slaveType,
                                                      masterType);
                                                for (int j = 0; j < g.get(i); j++) {
                                                    if (base == null) {
                                                        base = tables.get(tableIndex).generateUnit(filter::test);
                                                        if (base != null) {
                                                            found.putIfAbsent(tableIndex, new ArrayList<>());
                                                            found.get(tableIndex).add(base);
                                                        }
                                                    } else {
                                                        final MekSummary b = base;
                                                        MekSummary unit = tables.get(tableIndex)
                                                              .generateUnit(ms -> filter.test(ms) &&
                                                                    useGrouping.matches(
                                                                          ms,
                                                                          b));
                                                        if (unit != null) {
                                                            found.putIfAbsent(tableIndex, new ArrayList<>());
                                                            found.get(tableIndex).add(unit);
                                                        }
                                                    }
                                                }
                                            }
                                        }
                                        if (found.values().stream().mapToInt(List::size).sum() <
                                              g.values().stream().mapToInt(Integer::intValue).sum()) {
                                            found.clear();
                                            int mask = (1 << otherCriteria.size()) - 1;
                                            extraCriteria = 0;
                                            for (int k : g.keySet()) {
                                                extraCriteria |= k & mask;
                                            }
                                            attempts++;
                                        } else {
                                            break;
                                        }
                                    }

                                    for (Map.Entry<Integer, List<MekSummary>> e : found.entrySet()) {
                                        list.putIfAbsent(e.getKey(), new ArrayList<>());
                                        list.get(e.getKey()).addAll(e.getValue());
                                    }

                                    for (Integer k : g.keySet()) {
                                        workingCombo.merge(k, -g.get(k), Integer::sum);
                                    }
                                }

                                for (int i : workingCombo.keySet()) {
                                    if (workingCombo.get(i) > 0) {
                                        // Decode unit type
                                        int tableIndex = getTableIndex(params, i, POS_C3_NUM);
                                        final Predicate<MekSummary> filter = getFilterFromIndex(i,
                                              slaveType,
                                              masterType);
                                        for (int j = 0; j < workingCombo.get(i); j++) {
                                            MekSummary unit = tables.get(tableIndex).generateUnit(filter::test);
                                            if (unit != null) {
                                                list.putIfAbsent(tableIndex, new ArrayList<>());
                                                list.get(tableIndex).add(unit);
                                            }
                                        }
                                    }
                                }
                                List<MekSummary> retVal = list.values()
                                      .stream()
                                      .flatMap(Collection::stream)
                                      .collect(Collectors.toList());
                                if (retVal.size() < cUnits) {
                                    groups.remove(gIndex);
                                } else {
                                    return retVal;
                                }
                            }
                        } else {
                            for (int i : combo.keySet()) {
                                // Decode unit type
                                int tableIndex = getTableIndex(params, i, POS_C3_NUM);
                                final Predicate<MekSummary> filter = getFilterFromIndex(i, slaveType, masterType);
                                for (int j = 0; j < combo.get(i); j++) {
                                    MekSummary unit = tables.get(tableIndex).generateUnit(filter::test);
                                    if (unit != null) {
                                        list.putIfAbsent(tableIndex, new ArrayList<>());
                                        list.get(tableIndex).add(unit);
                                    }
                                }
                            }
                        }
                        List<MekSummary> retVal = list.values()
                              .stream()
                              .flatMap(Collection::stream)
                              .collect(Collectors.toList());
                        if (retVal.size() < cUnits) {
                            unitTypeGroupings.remove(utIndex);
                        } else {
                            return retVal;
                        }
                    }
                    List<MekSummary> retVal = list.values()
                          .stream()
                          .flatMap(Collection::stream)
                          .collect(Collectors.toList());
                    if (retVal.size() < cUnits) {
                        networkGroupings.remove(networkIndex);
                    } else {
                        return retVal;
                    }
                }
                combinations.remove(index);
            }
            numNetworked--;
        } while (numNetworked >= 0);

        List<MekSummary> onRole = tryIdealRole(params, numUnits);
        return (onRole == null) ? new ArrayList<>() : onRole;
    }

    private int getTableIndex(List<Parameters> params, int index, int posC3Num) {
        int tableIndex = 0;
        if (!params.isEmpty()) {
            int tmp = index >> (otherCriteria.size() + posC3Num);
            while (tmp != 0 && (tmp & 1) == 0) {
                tableIndex++;
                tmp >>= 1;
            }
        }
        return tableIndex;
    }

    private Predicate<MekSummary> getFilterFromIndex(int index, int slaveType, int masterType) {
        Predicate<MekSummary> retVal = mainCriteria;
        int mask = 1 << (otherCriteria.size() - 1);
        for (Constraint c : otherCriteria) {
            if ((index & mask) != 0) {
                retVal = retVal.and(c.criterion);
            }
            mask >>= 1;
        }
        mask = 1 << otherCriteria.size();
        if (slaveType > 0 && (mask & index) != 0) {
            retVal = retVal.and(ms -> (getNetworkMask(ms) & slaveType) != 0);
        }
        mask <<= 1;
        if (masterType > 0 && (mask & index) != 0) {
            retVal = retVal.and(ms -> (getNetworkMask(ms) & masterType) != 0);
        }
        mask <<= 1;
        if (masterType > 0 && (mask & index) != 0) {
            retVal = retVal.and(ms -> (getNetworkMask(ms) & (masterType | ModelRecord.NETWORK_COMPANY_COMMAND)) != 0);
        }
        return retVal;
    }

    /**
     * Attempts to build a unit entirely on an ideal role. Returns null if unsuccessful.
     */
    private @Nullable List<MekSummary> tryIdealRole(List<Parameters> params, List<Integer> numUnits) {
        if (idealRole.equals(UnitRole.UNDETERMINED)) {
            LOGGER.debug("[ForceGen][Formation] tryIdealRole skipped: idealRole=UNDETERMINED");
            return null;
        }
        List<Parameters> tmpParams = params.stream().map(Parameters::copy).toList();
        // NOTE: do NOT clear weight classes here. The caller (Force Generator) supplies the
        // lance's tree-assigned weight class, and the Formation Builder supplies the formation
        // type's own min/max weight range. Clearing them lets the ideal-role rescue search every
        // weight class, which silently upweights a Light lance into Mediums/Heavies and lets a
        // Light Battle Lance pick Assault units past its own maxWeightClass. Keep the constraint;
        // return null if the ideal role can't be filled within it, and let the caller fall back.
        List<MekSummary> retVal = new ArrayList<>();
        for (int i = 0; i < tmpParams.size(); i++) {
            UnitTable t = UnitTable.findTable(tmpParams.get(i));
            List<MekSummary> units = t.generateUnits(numUnits.get(i), ms -> ms.getRole() == idealRole);
            LOGGER.debug("[ForceGen][Formation]   tryIdealRole role={} wc={} need={} found={} units={}",
                  idealRole, tmpParams.get(i).getWeightClasses(), numUnits.get(i), units.size(), summarize(units));
            if (units.size() < numUnits.get(i)) {
                LOGGER.debug("[ForceGen][Formation]   tryIdealRole FAILED at param[{}] (insufficient {} units at"
                      + " weight {}); returning null", i, idealRole, tmpParams.get(i).getWeightClasses());
                return null;
            }
            retVal.addAll(units);
        }
        return retVal;
    }

    /**
     * Compact one-line summary of a unit list for the [ForceGen][Formation] trace: each entry as "Name(weightClass)",
     * e.g. "Locust LCT-1V(1), Stinger STG-3R(1)". Returns "[]" for an empty list.
     */
    private static String summarize(List<MekSummary> units) {
        if (units == null || units.isEmpty()) {
            return "[]";
        }
        return units.stream()
              .map(ms -> ms.getName() + "(" + ms.getWeightClass() + ")")
              .collect(Collectors.joining(", "));
    }

    /**
     * Finds all unique distributions of constraints among the units that fulfill the minimum number for each
     * constraint. The map keys indicate a combination of constraints, with the highest order a bit being the first
     * constraint in the list, and the value mapped to that key being the number of units that must meet the
     * constraint.
     */
    private List<Map<Integer, Integer>> findCombinations(int numUnits) {
        /*
         * This list is remade with each additional constraint, building on the previous
         * values
         */
        List<Map<Integer, Integer>> frequencies = new ArrayList<>();

        for (Constraint c : otherCriteria) {
            int req = c.getMinimum(numUnits);
            /*
             * If this is the first pass, we simply need to initialize the frequency list
             */
            if (frequencies.isEmpty()) {
                Map<Integer, Integer> freq = new LinkedHashMap<>();
                freq.put(0, numUnits - req);
                freq.put(1, req);
                frequencies.add(freq);
            } else {
                /* Create a new list to hold the values built off the previous one */
                List<Map<Integer, Integer>> newFrequencies = new ArrayList<>();
                /* Iterate through all the values from the previous pass and extend them */
                for (Map<Integer, Integer> freq : frequencies) {
                    /* We need to be able to access the keys by position */
                    List<Integer> keyList = new ArrayList<>(freq.keySet());
                    /* For each position, note how many total slots there are in later positions */
                    int[] remaining = new int[freq.size()];
                    int rem = 0;
                    for (int i = keyList.size() - 1; i >= 0; i--) {
                        rem += freq.get(keyList.get(i));
                        remaining[i] = rem;
                    }
                    int index = 0;
                    int toAllocate = req;
                    /*
                     * current holds the number of units at each index of the previous iteration
                     * that will meet the current constraint
                     */
                    int[] current = new int[keyList.size()];
                    outer:
                    while (remaining[index] >= toAllocate) {
                        current[index] = Math.min(freq.get(keyList.get(index)), toAllocate);
                        toAllocate -= current[index];
                        index++;
                        if (index == keyList.size()) {
                            if (c.isPairedWithPrevious()) {
                                Map<Integer, Integer> prevValues = new LinkedHashMap<>();
                                for (int i : freq.keySet()) {
                                    prevValues.put(i << 1, freq.get(i));
                                }
                                newFrequencies.add(prevValues);
                            }
                            Map<Integer, Integer> result = new LinkedHashMap<>();
                            for (int i = 0; i < current.length; i++) {
                                int key = keyList.get(i);
                                if (c.isPairedWithPrevious()) {
                                    key &= ~1;
                                }
                                if (freq.get(keyList.get(i)) > current[i]) {
                                    result.merge(key << 1, freq.get(keyList.get(i)) - current[i], Integer::sum);
                                }
                                if (current[i] > 0) {
                                    result.merge((key << 1) + 1, current[i], Integer::sum);
                                }
                            }
                            newFrequencies.add(result);
                            index--;
                            /*
                             * Keep backing up until we find one we can decrease, or we reach the beginning. We can
                             * decrease if the current value is > 0 and the remaining slots are big enough to hold
                             * toAllocate + 1.
                             */
                            while (index >= 0) {
                                if (current[index] == 0 ||
                                      index + 1 == current.length ||
                                      remaining[index + 1] <= toAllocate) {
                                    toAllocate += current[index];
                                    index--;
                                } else {
                                    current[index]--;
                                    toAllocate++;
                                    index++;
                                    continue outer;
                                }
                            }
                            break;
                        }
                    }
                }
                frequencies = newFrequencies;
            }
        }
        return frequencies;
    }

    /**
     * Finds all possible ways to distribute criteria beyond the general formation criteria in which the groups are
     * mutually exclusive; that is, a unit can only qualify for one of the criteria in the set. This is used for mixed
     * unit types and C3 networks. While a single unit could fulfill the requirements for speed and weight class, it
     * could not function as both a C3 slave and a C3 master or be both a Mek and a Tank.
     *
     * @param combination   The current criteria distribution as generated by
     *                      <code>findCombinations</code>
     * @param itemsPerGroup Array with length equal to number of groups and each value indicates the number of units in
     *                      that group.
     *
     * @return A map the same format as <code>combination</code> in which higher order bits in the key indicate a group.
     *       For example, in a formation with two criteria,
     *       <code>combination.length</code> == 2^2. If there are three additional
     *       groups, the return value will be 2 ^ (2+3). The value mapped to 11 (== 01011) will be the number of units
     *       that are in the second group and fulfill both formation criteria.
     */
    private List<Map<Integer, Integer>> findGroups(Map<Integer, Integer> combination, int[] itemsPerGroup,
          int indexBits) {
        List<Integer> keyList = new ArrayList<>(combination.keySet());

        List<int[][]> list = new ArrayList<>();
        int[][] initialVal = new int[1][keyList.size()];
        list.add(initialVal);

        /*
         * Compute distribution for each group sequentially, building on previously
         * calculated
         * distributions for each successive group.
         */
        for (int group = 0; group < itemsPerGroup.length; group++) {
            /*
             * Create a new list that we will fill out by copying the current values and
             * adding
             * the next group calculated during this iteration.
             */
            List<int[][]> newList = new ArrayList<>();
            /*
             * Cycle through all previous combinations and add all combinations for the current group
             */
            for (int[][] prev : list) {
                /*
                 * Initialize an array with the number of units at each position that have already been assigned to
                 * groups.
                 */
                int[] total = new int[keyList.size()];
                for (int[] integers : prev) {
                    for (int p = 0; p < integers.length; p++) {
                        total[p] += integers[p];
                    }
                }
                /* Create an array to track attempted distribution of the current group */
                int[] dist = new int[keyList.size()];
                dist[0] = itemsPerGroup[group];
                /* Shift values through the array until they are all in the final position */
                while (dist[dist.length - 1] <= itemsPerGroup[group]) {
                    /*
                     * Test whether there is room for the current distribution, and if so, add it to the list
                     */
                    boolean hasRoom = true;
                    for (int i = 0; i < dist.length; i++) {
                        if (total[i] + dist[i] > combination.get(keyList.get(i))) {
                            hasRoom = false;
                            break;
                        }
                    }
                    if (hasRoom) {
                        int[][] newVal = new int[group + 1][];
                        System.arraycopy(prev, 0, newVal, 0, group);
                        newVal[group] = new int[dist.length];
                        System.arraycopy(dist, 0, newVal[group], 0, dist.length);
                        newList.add(newVal);
                    }
                    /*
                     * Shift the values in the current distribution. Find the value > 0 closest to the end (not
                     * counting the final position), decrease it by 1, and set the value in the next position to 1
                     * plus whatever was in the tail position (which becomes 0 before incrementing
                     */
                    if (dist[dist.length - 1] == itemsPerGroup[group]) {
                        break;
                    }
                    int tail = dist[dist.length - 1];
                    dist[dist.length - 1] = 0;
                    for (int i = dist.length - 2; i >= 0; i--) {
                        if (dist[i] > 0) {
                            dist[i]--;
                            dist[i + 1] = tail + 1;
                            break;
                        }
                    }
                }
            }
            /* Replace the old list with one from this iteration */
            list = newList;
        }
        /* Use generated distributions to produce a new combination list */
        List<Map<Integer, Integer>> retVal = new ArrayList<>();
        for (int[][] val : list) {
            Map<Integer, Integer> newVal = new LinkedHashMap<>(combination);
            for (int g = 0; g < val.length; g++) {
                for (int i = 0; i < val[g].length; i++) {
                    if (val[g][i] > 0) {
                        newVal.put((1 << (g + indexBits)) + keyList.get(i), val[g][i]);
                        newVal.merge(keyList.get(i), -val[g][i], Integer::sum);
                        if (newVal.get(keyList.get(i)) <= 0) {
                            newVal.remove(keyList.get(i));
                        }
                    }
                }
            }
            retVal.add(newVal);
        }
        return retVal;
    }

    /**
     * Special case version of <code>findGroups</code> for matched units (such as paired ASFs). Because each group has
     * identical criteria, the number of possible results can be reduced.
     *
     * @param combination The current criteria distribution as generated by
     *                    <code>findCombinations</code>
     *
     * @return A list of possible groupings. Each entry is a list of size() equal to numGroups. The entry for each group
     *       is a map of the same format as
     *       <code>combination</code>.
     */
    private List<List<Map<Integer, Integer>>> findMatchedGroups(Map<Integer, Integer> combination,
          GroupingConstraint groupingCriteria) {
        int numUnits = combination.values().stream().mapToInt(Integer::intValue).sum();
        int size = Math.min(groupingCriteria.getGroupSize(), numUnits);
        int numGroups = Math.max(groupingCriteria.getNumGroups(), 1);
        if (groupingCriteria.getGroupSize() == 0 && groupingCriteria.getNumGroups() > 0) {
            numGroups = groupingCriteria.getNumGroups();
            size = Math.max(1, numUnits / numGroups);
        } else if (groupingCriteria.getNumGroups() == 0 && groupingCriteria.getGroupSize() > 0) {
            size = groupingCriteria.getGroupSize();
            numGroups = Math.max(1, numUnits / size);
        }
        List<Integer> keyList = new ArrayList<>(combination.keySet());

        List<int[][]> list = new ArrayList<>();
        int[][] initialVal = new int[1][keyList.size()];
        list.add(initialVal);

        /*
         * Compute distribution for each group sequentially, building on previously
         * calculated
         * distributions for each successive group.
         */
        for (int group = 0; group < numGroups; group++) {
            /*
             * Create a new list that we will fill out by copying the current values and
             * adding
             * the next group calculated during this iteration.
             */
            List<int[][]> newList = new ArrayList<>();
            /*
             * Cycle through all previous combinations and add all combinations for the current group
             */
            for (int[][] prev : list) {
                /*
                 * Initialize an array with the number of units at each position that have already been assigned to
                 * groups.
                 */
                int[] total = new int[keyList.size()];
                for (int[] integers : prev) {
                    for (int p = 0; p < integers.length; p++) {
                        total[p] += integers[p];
                    }
                }
                /*
                 * Find the starting position for the current group. We don't want to start earlier than the first
                 * position that has been assigned to a group; that will be a permutation of a result that has
                 * already been calculated.
                 */

                int startPos = -1;
                for (int i = 0; i < total.length; i++) {
                    if (total[i] > 0) {
                        startPos = i;
                        break;
                    }
                }
                startPos = Math.max(0, startPos);

                /* Create an array to track attempted distribution of the current group */
                int[] dist = new int[keyList.size()];
                dist[startPos] = size;
                /* Shift values through the array until they are all in the final position */
                while (dist[dist.length - 1] <= size) {
                    /*
                     * Test whether there is room for the current distribution, and if so, add it to the list
                     */
                    boolean hasRoom = true;
                    for (int i = 0; i < dist.length; i++) {
                        if (total[i] + dist[i] > combination.get(keyList.get(i))) {
                            hasRoom = false;
                            break;
                        }
                    }

                    if (hasRoom) {
                        int[][] newVal = new int[group + 1][];
                        System.arraycopy(prev, 0, newVal, 0, group);
                        newVal[group] = new int[dist.length];
                        System.arraycopy(dist, 0, newVal[group], 0, dist.length);
                        newList.add(newVal);
                    }
                    /*
                     * Shift the values in the current distribution. Find the value > 0 closest to the end (not
                     * counting the final position), decrease it by 1, and set the value in the next position to 1
                     * plus whatever was in the tail position (which becomes 0 before incrementing
                     */
                    if (dist[dist.length - 1] == size) {
                        break;
                    }
                    int tail = dist[dist.length - 1];
                    dist[dist.length - 1] = 0;
                    for (int i = dist.length - 2; i >= 0; i--) {
                        if (dist[i] > 0) {
                            dist[i]--;
                            dist[i + 1] = tail + 1;
                            break;
                        }
                    }
                }
            }
            /* Replace the old list with one from this iteration */
            list = newList;
        }

        return getRetValForMatchedGroups(list, keyList);
    }

    private static List<List<Map<Integer, Integer>>> getRetValForMatchedGroups(List<int[][]> list,
          List<Integer> keyList) {
        List<List<Map<Integer, Integer>>> retVal = new ArrayList<>();
        for (int[][] grouping : list) {
            List<Map<Integer, Integer>> newGrouping = new ArrayList<>();
            for (int[] integers : grouping) {
                Map<Integer, Integer> map = new HashMap<>();
                for (int p = 0; p < integers.length; p++) {
                    map.put(keyList.get(p), integers[p]);
                }
                newGrouping.add(map);
            }
            retVal.add(newGrouping);
        }
        return retVal;
    }

    /**
     * Tests whether a list of units qualifies for the formation type. Note that unit roles are not available for all
     * units.
     *
     * @param units A list of units to test
     *
     * @return Whether the list of units meets the qualifications for this formation.
     */
    public boolean qualifies(List<MekSummary> units) {
        if (units.stream().anyMatch(ms -> !isAllowedUnitType(ModelRecord.parseUnitType(ms.getUnitType())))) {
            return false;
        }
        if (!idealRole.equals(UnitRole.UNDETERMINED)) {
            if (units.stream().allMatch(ms -> ms.getRole() == idealRole)) {
                return true;
            }
        }
        for (MekSummary ms : units) {
            if (!mainCriteria.test(ms) ||
                  ms.getWeightClass() < minWeightClass ||
                  ms.getWeightClass() > maxWeightClass) {
                return false;
            }
        }
        for (int i = 0; i < otherCriteria.size(); i++) {
            final Constraint c = otherCriteria.get(i);
            if (c.isPairedWithPrevious()) {
                continue;
            }
            long matches = units.stream().filter(c::matches).count();
            if (matches < c.getMinimum(units.size())) {
                if (c.isPairedWithNext() && i + 1 < otherCriteria.size()) {
                    // The pair is satisfied only if the alternative also meets its minimum.
                    final Constraint alternative = otherCriteria.get(i + 1);
                    long altMatches = units.stream().filter(alternative::matches).count();
                    if (altMatches < alternative.getMinimum(units.size())) {
                        return false;
                    }
                    i++;
                } else {
                    return false;
                }
            }
        }
        if (groupingCriteria != null) {
            /*
             * First group by chassis, then test whether each group fulfills the
             * requirement.
             * If not, regroup by name.
             */
            List<MekSummary> groupedUnits = units.stream()
                  .filter(ms -> groupingCriteria.appliesTo(ModelRecord.parseUnitType(ms.getUnitType())))
                  .toList();
            if (!groupedUnits.isEmpty()) {
                Map<String, List<MekSummary>> groups = groupedUnits.stream()
                      .collect(Collectors.groupingBy(MekSummary::getChassis));
                GROUP_LOOP:
                for (List<MekSummary> group : groups.values()) {
                    for (int i = 0; i < group.size() - 1; i++) {
                        for (int j = i + 1; j < group.size(); j++) {
                            if (!groupingCriteria.matches(group.get(i), group.get(j))) {
                                groups = groupedUnits.stream().collect(Collectors.groupingBy(MekSummary::getName));
                                break GROUP_LOOP;
                            }
                        }
                    }
                }
                int groupSize = Math.min(groupingCriteria.getGroupSize(), groupedUnits.size());
                int numGroups = Math.min(groupingCriteria.getNumGroups(), groupedUnits.size() / groupSize);
                /* Allow for the possibility that two or more groups may be identical */
                int groupCount = 0;
                for (List<MekSummary> g : groups.values()) {
                    groupCount += g.size() / groupSize;
                }
                return groupCount >= numGroups;
            }
        }
        return true;
    }

    /**
     * Tests whether a list of units qualifies for the formation type. Note that unit roles are not available for all
     * units.
     *
     * @param units A list of units to test
     *
     * @return Whether the list of units meets the qualifications for this formation.
     */
    public String qualificationReport(List<MekSummary> units) {
        List<MekSummary> wrongUnits = new ArrayList<>();
        List<MekSummary> weight = new ArrayList<>();
        List<MekSummary> main = new ArrayList<>();
        List<List<MekSummary>> other = new ArrayList<>();
        for (int i = 0; i < otherCriteria.size(); i++) {
            other.add(new ArrayList<>());
        }

        for (MekSummary ms : units) {
            if (!isAllowedUnitType(ModelRecord.parseUnitType(ms.getUnitType()))) {
                wrongUnits.add(ms);
            }

            if (ms.getWeightClass() >= minWeightClass && ms.getWeightClass() <= maxWeightClass) {
                weight.add(ms);
            }

            if (mainCriteria.test(ms)) {
                main.add(ms);
            }

            for (int i = 0; i < otherCriteria.size(); i++) {
                if (otherCriteria.get(i).matches(ms)) {
                    other.get(i).add(ms);
                }
            }
        }
        StringBuilder sb = new StringBuilder("<html>");
        if (!wrongUnits.isEmpty()) {
            sb.append("<font color='red'>Wrong unit type:</font>\n\t");
            sb.append(wrongUnits.stream().map(MekSummary::getName).collect(Collectors.joining("\n\t")))
                  .append("<br/><br/>\n");
        }
        sb.append("Unit Roles:<br/>\n&nbsp;&nbsp;&nbsp;");
        sb.append(units.stream()
              .map(ms -> ms.getName() + ": " + ms.getRole())
              .collect(Collectors.joining("<br/>\n&nbsp;&nbsp;&nbsp;"))).append("<br/><br/>\n");
        if (!idealRole.equals(UnitRole.UNDETERMINED)) {
            sb.append("Ideal role: ").append(idealRole).append("<br/><br/>\n");
        }

        if (weight.size() < units.size()) {
            sb.append("<font color='red'>");
        }
        sb.append("Weight class ")
              .append(EntityWeightClass.getClassName(Math.max(minWeightClass, EntityWeightClass.WEIGHT_LIGHT)))
              .append("-")
              .append(EntityWeightClass.getClassName(Math.min(maxWeightClass, EntityWeightClass.WEIGHT_ASSAULT)))
              .append("<br/>\n");
        if (weight.size() < units.size()) {
            sb.append("</font>");
        }

        if (!weight.isEmpty()) {
            sb.append("&nbsp;&nbsp;&nbsp;")
                  .append(weight.stream()
                        .map(ms -> ms.getName() + ": " + EntityWeightClass.getClassName(ms.getWeightClass()))
                        .collect(Collectors.joining("<br/>\n&nbsp;&nbsp;&nbsp;")))
                  .append("<br/><br/>\n");
        } else {
            sb.append("&nbsp;&nbsp;&nbsp;None<br/><br/>\n");
        }

        if (mainDescription != null) {
            if (main.size() < units.size()) {
                sb.append("<font color='red'>");
            }
            sb.append(mainDescription).append(" (").append(units.size()).append(")<br/>\n");
            if (main.size() < units.size()) {
                sb.append("</font>");
            }

            if (!main.isEmpty()) {
                sb.append("&nbsp;&nbsp;&nbsp;")
                      .append("\t")
                      .append(main.stream()
                            .map(MekSummary::getName)
                            .collect(Collectors.joining("<br/>\n&nbsp;&nbsp;&nbsp;")))
                      .append("<br/><br/>\n");
            } else {
                sb.append("&nbsp;&nbsp;&nbsp;None<br/><br/>\n");
            }
        }

        for (int i = 0; i < otherCriteria.size(); i++) {
            boolean isShort = false;
            if (other.get(i).size() < otherCriteria.get(i).getMinimum(units.size())) {
                if (otherCriteria.get(i).isPairedWithNext()) {
                    isShort = i + 1 < otherCriteria.size() &&
                          other.get(i + 1).size() < otherCriteria.get(i + 1).getMinimum(units.size());
                } else if (otherCriteria.get(i).isPairedWithPrevious()) {
                    isShort = i - 1 > 0 && other.get(i - 1).size() < otherCriteria.get(i - 1).getMinimum(units.size());
                } else {
                    isShort = true;
                }
            }
            if (isShort) {
                sb.append("<font color='red'>");
            }

            if (otherCriteria.get(i).isPairedWithPrevious()) {
                sb.append("<b>or</b> ");
            }
            sb.append(otherCriteria.get(i).description)
                  .append(" (")
                  .append(otherCriteria.get(i).getMinimum(units.size()))
                  .append(")");
            sb.append("<br />\n");
            if (isShort) {
                sb.append("</font>");
            }

            if (!other.get(i).isEmpty()) {
                sb.append("&nbsp;&nbsp;&nbsp;")
                      .append(other.get(i)
                            .stream()
                            .map(MekSummary::getName)
                            .collect(Collectors.joining("<br/>\n&nbsp;&nbsp;&nbsp;")))
                      .append("<br/><br/>\n");
            } else {
                sb.append("&nbsp;&nbsp;&nbsp;None<br/><br/>\n");
            }
        }

        if (groupingCriteria != null) {
            List<MekSummary> groupedUnits = units.stream()
                  .filter(ms -> groupingCriteria.appliesTo(ModelRecord.parseUnitType(ms.getUnitType())))
                  .toList();
            if (!groupedUnits.isEmpty()) {
                Map<String, List<MekSummary>> groups = groupedUnits.stream()
                      .collect(Collectors.groupingBy(MekSummary::getChassis));
                GROUP_LOOP:
                for (List<MekSummary> group : groups.values()) {
                    for (int i = 0; i < group.size() - 1; i++) {
                        for (int j = i + 1; j < group.size(); j++) {
                            if (!groupingCriteria.matches(group.get(i), group.get(j))) {
                                groups = groupedUnits.stream().collect(Collectors.groupingBy(MekSummary::getName));
                                break GROUP_LOOP;
                            }
                        }
                    }
                }
                int groupSize = Math.min(groupingCriteria.getGroupSize(), groupedUnits.size());
                int numGroups = Math.min(groupingCriteria.getNumGroups(), groupedUnits.size() / groupSize);
                /* Allow for the possibility that two or more groups may be identical */
                int groupCount = 0;
                for (List<MekSummary> g : groups.values()) {
                    groupCount += g.size() / groupSize;
                }
                if (groupCount < numGroups) {
                    sb.append("<font color='red'>");
                }
                sb.append(groupingCriteria.getDescription())
                      .append(" (")
                      .append(numGroups)
                      .append("x")
                      .append(groupSize)
                      .append(")");
                if (groupCount < numGroups) {
                    sb.append("</font>");
                }
                sb.append("<br/>\n");
                if (groupCount > 0) {
                    for (String groupName : groups.keySet()) {
                        int size = groups.get(groupName).size();
                        while (size >= groupSize) {
                            sb.append("&nbsp;&nbsp;&nbsp;")
                                  .append(groupName)
                                  .append(" (")
                                  .append(groupSize)
                                  .append(")<br/>\n");
                            size -= groupSize;
                        }
                    }
                } else {
                    sb.append("&nbsp;&nbsp;&nbsp;None<br/><br/>\n");
                }
            }
        }
        sb.append("</html>");
        return sb.toString();
    }

    /**
     * Initializes (or reinitializes) the static registry of all known formation types, registering each formation
     * defined by the {@code create*Lance()} / {@code create*Squadron()} factories below. Called lazily by
     * {@link #getFormationType(String)} and {@link #getAllFormations()} on first access; rarely invoked directly.
     */
    public static void createFormationTypes() {
        allFormationTypes = new HashMap<>();
        createAntiMekLance();
        createAssaultLance();
        createAnvilLance();
        createFastAssaultLance();
        createHunterLance();
        createBattleLance();
        createLightBattleLance();
        createMediumBattleLance();
        createHeavyBattleLance();
        createRifleLance();
        createBerserkerLance();
        createCommandLance();
        createOrderLance();
        createVehicleCommandLance();
        createFireLance();
        createAntiAirLance();
        createArtilleryFireLance();
        createDirectFireLance();
        createFireSupportLance();
        createLightFireLance();
        createPursuitLance();
        createProbeLance();
        createSweepLance();
        createReconLance();
        createHeavyReconLance();
        createLightReconLance();
        createSecurityLance();
        createStrikerCavalryLance();
        createHammerLance();
        createHeavyStrikerCavalryLance();
        createHordeLance();
        createLightStrikerCavalryLance();
        createRangerLance();
        createSupportLance();
        createUrbanLance();
        createAerospaceSuperioritySquadron();
        createEWSquadron();
        createFireSupportSquadron();
        createInterceptorSquadron();
        createStrikeSquadron();
        createTransportSquadron();

        // Not registered above — meta-formations that current architecture cannot represent:
        //
        // Air Lance (CamOps p. 61): a ground lance of any non-infantry type plus a pair of
        // identical aerospace or conventional fighters. The ground half must independently
        // satisfy a chosen ground Formation Type (Battle, Recon, etc.); the two fighters
        // must be identical units. Implementing this needs a "composite formation" concept
        // that holds references to two sub-formations and delegates qualifies()/
        // generateFormation() to them rather than evaluating its own constraint list. The
        // attached fighters do not benefit from the ground formation's bonus ability and
        // do not count toward its requirements.
        //
        // Nova (CamOps p. 64): a Clan-only combined formation made of a Star of OmniMeks
        // (which also satisfies another Mek Formation Type, e.g., Battle Star) and a Star
        // of mechanized battle armor. Vehicle Nova and Aerospace Nova variants exist.
        // Needs the same composite-formation infrastructure as Air Lance, plus cross-Star
        // equipment validation: all Meks must have OMNI; all BA must have MEC. Non-Clan
        // equivalents substitute XMEC BA one-for-one for non-Omni Meks. Bonus abilities
        // apply only to the Mek Star; the BA Star receives no formation bonus.
    }

    /**
     * Registers the Anti-Mek Lance (Campaign Operations p. 61): an infantry/battle armor formation trained to swarm and
     * disrupt enemy Meks.
     */
    private static void createAntiMekLance() {
        FormationType ft = new FormationType("Anti-Mek");
        ft.allowedUnitTypes = FLAG_INFANTRY | FLAG_BATTLE_ARMOR;
        allFormationTypes.put(ft.name, ft);
    }

    /**
     * Registers the Assault Lance (Campaign Operations p. 61): the powerhouse formation of any force, with reduced
     * speed offset by massive firepower and armor. Ideal role: Juggernaut.
     */
    private static void createAssaultLance() {
        FormationType ft = new FormationType("Assault");
        ft.allowedUnitTypes = FLAG_GROUND_NO_LIGHT;
        ft.idealRole = UnitRole.JUGGERNAUT;
        ft.minWeightClass = EntityWeightClass.WEIGHT_MEDIUM;
        ft.mainCriteria = ms -> ms.getTotalArmor() >= 135;
        ft.mainDescription = "Armor 135+";
        ft.otherCriteria.add(new PercentConstraint(0.75, ms -> getDamageAtRange(ms, 7) >= 25, "25 damage at range 7"));
        ft.otherCriteria.add(new CountConstraint(3,
              ms -> ms.getWeightClass() >= EntityWeightClass.WEIGHT_HEAVY,
              "Heavy+"));
        Constraint c = new CountConstraint(1, ms -> ms.getRole() == JUGGERNAUT, "Juggernaut");
        c.setPairedWithNext(true);
        ft.otherCriteria.add(c);
        c = new CountConstraint(2, ms -> ms.getRole() == SNIPER, "Sniper");
        c.setPairedWithPrevious(true);
        ft.otherCriteria.add(c);
        ft.reportMetrics.put("Armor", MekSummary::getTotalArmor);
        ft.reportMetrics.put("Damage @ 7", ms -> getDamageAtRange(ms, 7));
        allFormationTypes.put(ft.name, ft);
    }

    /**
     * Registers the Anvil Lance (Campaign Operations p. 62): a House Marik Assault Lance variant trained to hold ground
     * and absorb the enemy's advance while heavier units engage. Ideal role: Juggernaut.
     */
    private static void createAnvilLance() {
        FormationType ft = new FormationType("Anvil", "Assault");
        ft.allowedUnitTypes = FLAG_GROUND_NO_LIGHT;
        ft.idealRole = UnitRole.JUGGERNAUT;
        ft.exclusiveFaction = "FWL";
        ft.minWeightClass = EntityWeightClass.WEIGHT_MEDIUM;
        // Campaign Operations (Anvil Lance): all units must possess at least 105 armor points.
        ft.mainCriteria = ms -> ms.getTotalArmor() >= 105;
        ft.mainDescription = "Armor 105+";
        ft.otherCriteria.add(new PercentConstraint(0.5,
              ms -> ms.getEquipmentNames()
                    .stream()
                    .map(EquipmentType::get)
                    .anyMatch(eq -> eq instanceof ACWeapon ||
                          eq instanceof LBXACWeapon ||
                          eq instanceof UACWeapon ||
                          eq instanceof SRMWeapon ||
                          eq instanceof LRMWeapon),
              "AC, SRM, or LRM"));
        ft.reportMetrics.put("AC/SRM/LRM", ms -> ft.otherCriteria.getFirst().criterion.test(ms));
        allFormationTypes.put(ft.name, ft);
    }

    /**
     * Registers the Fast Assault Lance (Campaign Operations p. 62): an Assault Lance variant requiring Walk/Cruise 5+
     * or jump capability on every unit.
     */
    private static void createFastAssaultLance() {
        FormationType ft = new FormationType("Fast Assault", "Assault");
        ft.allowedUnitTypes = FLAG_GROUND_NO_LIGHT;
        ft.idealRole = UnitRole.JUGGERNAUT;
        ft.minWeightClass = EntityWeightClass.WEIGHT_MEDIUM;
        ft.mainCriteria = ms -> ms.getTotalArmor() >= 135 && (ms.getWalkMp() >= 5 || ms.getJumpMp() > 0);
        ft.mainDescription = "Walk 5+ or Jump 1+";
        ft.otherCriteria.add(new PercentConstraint(0.75, ms -> getDamageAtRange(ms, 7) >= 25, "Damage 25+ at range 7"));
        ft.otherCriteria.add(new CountConstraint(3,
              ms -> ms.getWeightClass() >= EntityWeightClass.WEIGHT_HEAVY,
              "Heavy+"));
        // Campaign Operations (Assault Lance, inherited by Fast Assault): at least one Juggernaut OR
        // two Snipers. Encoded as a paired-OR pair of CountConstraints.
        Constraint c = new CountConstraint(1, ms -> ms.getRole() == JUGGERNAUT, "Juggernaut");
        c.setPairedWithNext(true);
        ft.otherCriteria.add(c);
        c = new CountConstraint(2, ms -> ms.getRole() == SNIPER, "Sniper");
        c.setPairedWithPrevious(true);
        ft.otherCriteria.add(c);
        ft.reportMetrics.put("Damage @ 7", ms -> getDamageAtRange(ms, 7));
        allFormationTypes.put(ft.name, ft);
    }

    /**
     * Registers the Hunter Lance (Campaign Operations p. 62): an Assault Lance variant favoring heavy woods or urban
     * terrain for ambush combat. As an Assault variant it inherits the full Assault Lance base requirements (no light
     * units, 135+ armor, 75% able to do 25 damage at range 7, three or more Heavy+, and one Juggernaut or two Snipers)
     * and adds its own rule: at least half the units must be Ambushers or Juggernauts. Ideal role: Ambusher.
     */
    private static void createHunterLance() {
        FormationType ft = new FormationType("Hunter", "Assault");
        ft.allowedUnitTypes = FLAG_GROUND_NO_LIGHT;
        ft.idealRole = UnitRole.AMBUSHER;
        ft.minWeightClass = EntityWeightClass.WEIGHT_MEDIUM;
        // Campaign Operations (Assault Lance base, inherited by Hunter): 135+ armor.
        ft.mainCriteria = ms -> ms.getTotalArmor() >= 135;
        ft.mainDescription = "Armor 135+";
        ft.otherCriteria.add(new PercentConstraint(0.75, ms -> getDamageAtRange(ms, 7) >= 25, "25 damage at range 7"));
        ft.otherCriteria.add(new CountConstraint(3,
              ms -> ms.getWeightClass() >= EntityWeightClass.WEIGHT_HEAVY,
              "Heavy+"));
        // Campaign Operations (Assault Lance base, inherited by Hunter): at least one Juggernaut OR
        // two Snipers. Encoded as a paired-OR pair of CountConstraints.
        Constraint juggernautConstraint = new CountConstraint(1, ms -> ms.getRole() == JUGGERNAUT, "Juggernaut");
        juggernautConstraint.setPairedWithNext(true);
        ft.otherCriteria.add(juggernautConstraint);
        Constraint sniperConstraint = new CountConstraint(2, ms -> ms.getRole() == SNIPER, "Sniper");
        sniperConstraint.setPairedWithPrevious(true);
        ft.otherCriteria.add(sniperConstraint);
        // Hunter-specific rule: at least 50% of the units must be Ambushers or Juggernauts.
        ft.otherCriteria.add(new PercentConstraint(0.5,
              ms -> ms.getRole().isAnyOf(JUGGERNAUT, AMBUSHER),
              "Juggernaut or Ambusher"));
        ft.reportMetrics.put("Armor", MekSummary::getTotalArmor);
        ft.reportMetrics.put("Damage @ 7", ms -> getDamageAtRange(ms, 7));
        allFormationTypes.put(ft.name, ft);
    }

    /**
     * Registers the Battle Lance (Campaign Operations p. 62): the standard line formation, intended to close with the
     * enemy on the strength of armor and firepower. Ideal role: Brawler.
     */
    private static void createBattleLance() {
        FormationType ft = new FormationType("Battle");
        ft.allowedUnitTypes = FLAG_GROUND_NO_LIGHT;
        ft.idealRole = UnitRole.BRAWLER;
        ft.otherCriteria.add(new PercentConstraint(0.5,
              ms -> ms.getWeightClass() >= EntityWeightClass.WEIGHT_HEAVY,
              "Heavy+"));
        ft.otherCriteria.add(new CountConstraint(3,
              ms -> ms.getRole().isAnyOf(BRAWLER, SNIPER, SKIRMISHER),
              "Brawler, Sniper, Skirmisher"));
        ft.groupingCriteria = new GroupingConstraint(FLAG_VEHICLE,
              2,
              2,
              ms -> ms.getWeightClass() == EntityWeightClass.WEIGHT_HEAVY,
              FormationType::checkUnitMatch,
              "Same model, Heavy");
        allFormationTypes.put(ft.name, ft);
    }

    /**
     * Registers the Light Battle Lance (Campaign Operations p. 63): a light-weight Battle Lance variant requiring at
     * least one Scout.
     */
    private static void createLightBattleLance() {
        FormationType ft = new FormationType("Light Battle", "Battle");
        ft.allowedUnitTypes = FLAG_GROUND;
        ft.maxWeightClass = EntityWeightClass.WEIGHT_HEAVY;
        ft.otherCriteria.add(new PercentConstraint(0.75,
              ms -> ms.getWeightClass() == EntityWeightClass.WEIGHT_LIGHT,
              "Light"));
        ft.otherCriteria.add(new CountConstraint(1, ms -> ms.getRole() == SCOUT, "Scout"));
        ft.groupingCriteria = new GroupingConstraint(FLAG_VEHICLE,
              2,
              2,
              ms -> ms.getWeightClass() == EntityWeightClass.WEIGHT_LIGHT,
              FormationType::checkUnitMatch,
              "Same model, Light");
        allFormationTypes.put(ft.name, ft);
    }

    /** Registers the Medium Battle Lance (Campaign Operations p. 63): a medium-weight Battle Lance variant. */
    private static void createMediumBattleLance() {
        FormationType ft = new FormationType("Medium Battle", "Battle");
        ft.allowedUnitTypes = FLAG_GROUND_NO_LIGHT;
        ft.maxWeightClass = EntityWeightClass.WEIGHT_HEAVY;
        ft.otherCriteria.add(new PercentConstraint(0.5,
              ms -> ms.getWeightClass() == EntityWeightClass.WEIGHT_MEDIUM,
              "Medium"));
        ft.groupingCriteria = new GroupingConstraint(FLAG_VEHICLE,
              2,
              2,
              ms -> ms.getWeightClass() == EntityWeightClass.WEIGHT_MEDIUM,
              FormationType::checkUnitMatch,
              "Same model, Medium");
        allFormationTypes.put(ft.name, ft);
    }

    /** Registers the Heavy Battle Lance (Campaign Operations p. 63): a heavy-weight Battle Lance variant; light-weight units are excluded. */
    private static void createHeavyBattleLance() {
        FormationType ft = new FormationType("Heavy Battle", "Battle");
        ft.allowedUnitTypes = FLAG_GROUND_NO_LIGHT;
        ft.minWeightClass = EntityWeightClass.WEIGHT_MEDIUM;
        ft.otherCriteria.add(new PercentConstraint(0.5,
              ms -> ms.getWeightClass() >= EntityWeightClass.WEIGHT_HEAVY,
              "Heavy+"));
        ft.groupingCriteria = new GroupingConstraint(FLAG_VEHICLE,
              2,
              2,
              ms -> ms.getWeightClass() >= EntityWeightClass.WEIGHT_HEAVY,
              FormationType::checkUnitMatch,
              "Same model, Heavy+");
        allFormationTypes.put(ft.name, ft);
    }

    /**
     * Registers the Rifle Lance (Campaign Operations p. 63): a House Davion Battle Lance variant built around
     * autocannon-armed mobile units (medium and heavy weight, Walk/Cruise 4+).
     */
    private static void createRifleLance() {
        FormationType ft = new FormationType("Rifle", "Battle");
        ft.allowedUnitTypes = FLAG_GROUND_NO_LIGHT;
        ft.exclusiveFaction = "FS";
        ft.minWeightClass = EntityWeightClass.WEIGHT_MEDIUM;
        ft.mainCriteria = ms -> ms.getWalkMp() >= 4;
        ft.mainDescription = "Walk/Cruise 4+";
        ft.otherCriteria.add(new PercentConstraint(0.75,
              ms -> ms.getWeightClass() <= EntityWeightClass.WEIGHT_HEAVY,
              "Medium, Heavy"));
        ft.otherCriteria.add(new PercentConstraint(0.5,
              ms -> ms.getEquipmentNames()
                    .stream()
                    .map(EquipmentType::get)
                    .anyMatch(eq -> eq instanceof ACWeapon ||
                          eq instanceof LBXACWeapon ||
                          eq instanceof UACWeapon),
              // UAC includes RAC
              "AC weapon"));
        ft.reportMetrics.put("AC", ms -> ft.otherCriteria.get(1).criterion.test(ms));
        allFormationTypes.put(ft.name, ft);
    }

    /**
     * Registers the Berserker/Close Combat Lance (Campaign Operations p. 63): a Mek-only Battle Lance variant
     * trained to close with and physically attack the enemy. Ideal role: Brawler.
     */
    private static void createBerserkerLance() {
        FormationType ft = new FormationType("Berserker/Close", "Battle");
        ft.allowedUnitTypes = FLAG_MEK | FLAG_PROTOMEK;
        ft.idealRole = BRAWLER;
        ft.otherCriteria.add(new PercentConstraint(0.5,
              ms -> ms.getWeightClass() >= EntityWeightClass.WEIGHT_HEAVY,
              "Heavy+"));
        ft.otherCriteria.add(new CountConstraint(3,
              ms -> ms.getRole().isAnyOf(BRAWLER, SNIPER, SKIRMISHER),
              "Brawler, Sniper, Skirmisher"));
        allFormationTypes.put(ft.name, ft);
    }

    /**
     * Registers the Command Lance (Campaign Operations p. 63): a formation built around the force commander,
     * with diverse capabilities to support and protect the leader on the battlefield.
     */
    private static void createCommandLance() {
        FormationType ft = new FormationType("Command", "Command");
        ft.allowedUnitTypes = FLAG_MEK | FLAG_PROTOMEK;
        ft.otherCriteria.add(new PercentConstraint(0.5,
              ms -> ms.getRole().isAnyOf(SNIPER, MISSILE_BOAT, SKIRMISHER, JUGGERNAUT),
              "Sniper, Missile Boat, Skirmisher, Juggernaught"));
        ft.otherCriteria.add(new CountConstraint(1,
              ms -> ms.getRole().isAnyOf(BRAWLER, STRIKER, SCOUT),
              "Brawler, Striker, Scout"));
        allFormationTypes.put(ft.name, ft);
    }

    /**
     * Registers the Order Lance (Campaign Operations p. 63): a House Kurita Command Lance variant where every
     * unit shares the same model and weight class.
     */
    private static void createOrderLance() {
        FormationType ft = new FormationType("Order", "Command");
        ft.allowedUnitTypes = FLAG_GROUND;
        ft.exclusiveFaction = "DC";
        ft.groupingCriteria = new GroupingConstraint(FLAG_GROUND,
              0,
              1,
              ms -> true,
              FormationType::checkUnitMatch,
              "Same model");
        allFormationTypes.put(ft.name, ft);
    }

    /**
     * Registers the Vehicle Command Lance (Campaign Operations p. 63): a Command Lance variant for combat
     * vehicles, with one matched pair fulfilling the role requirement.
     */
    private static void createVehicleCommandLance() {
        FormationType ft = new FormationType("Vehicle Command", "Command");
        ft.allowedUnitTypes = FLAG_TANK | FLAG_VTOL | FLAG_NAVAL;
        ft.otherCriteria.add(new CountConstraint(1,
              ms -> ms.getRole().isAnyOf(BRAWLER, STRIKER, SCOUT),
              "Brawler, Striker, Scout"));
        // Campaign Operations (Vehicle Command Lance): only one pair of vehicles needs to have one
        // of the Sniper, Missile Boat, Skirmisher, or Juggernaut roles.
        ft.groupingCriteria = new GroupingConstraint(FLAG_VEHICLE,
              2,
              1,
              ms -> ms.getRole().isAnyOf(SNIPER, MISSILE_BOAT, SKIRMISHER, JUGGERNAUT),
              (ms0, ms1) -> ms0.getName().equals(ms1.getName()),
              "Same model");
        allFormationTypes.put(ft.name, ft);
    }

    /**
     * Registers the Fire Lance (Campaign Operations p. 64): a long-range fire formation that engages from a safe
     * distance with powerful weaponry. Ideal role: Missile Boat.
     */
    private static void createFireLance() {
        FormationType ft = new FormationType("Fire");
        ft.allowedUnitTypes = FLAG_GROUND;
        ft.idealRole = UnitRole.MISSILE_BOAT;
        ft.otherCriteria.add(new PercentConstraint(0.75,
              ms -> ms.getRole().isAnyOf(SNIPER, MISSILE_BOAT),
              "Sniper, Missile Boat"));
        allFormationTypes.put(ft.name, ft);
    }

    /**
     * Registers the Anti-Air Lance (Campaign Operations p. 64): a Fire Lance variant emphasizing anti-aircraft
     * autocannons, LBX, artillery, or units with the Anti-Aircraft Targeting Quirk.
     */
    private static void createAntiAirLance() {
        FormationType ft = new FormationType("Anti-Air", "Fire");
        ft.allowedUnitTypes = FLAG_GROUND;
        ft.missionRoles.add(MissionRole.MIXED_ARTILLERY);
        ft.otherCriteria.add(new PercentConstraint(0.75,
              ms -> ms.getRole().isAnyOf(SNIPER, MISSILE_BOAT),
              "Sniper, Missile Boat"));
        ft.otherCriteria.add(new CountConstraint(2,
              // should indicate it has anti-aircraft targeting quirk without having to load
              // all entities
              ms -> getMissionRoles(ms).contains(MissionRole.ANTI_AIRCRAFT) ||
                    ms.getEquipmentNames()
                          .stream()
                          .map(EquipmentType::get)
                          .anyMatch(eq -> eq instanceof ACWeapon ||
                                eq instanceof LBXACWeapon ||
                                eq instanceof ArtilleryWeapon),
              "Standard AC, LBX, Artillery weapon, Anti-Air targeting quirk"));
        ft.reportMetrics.put("AC/LBX/Artillery/AA Quirk", ms -> ft.otherCriteria.get(1).criterion.test(ms));
        allFormationTypes.put(ft.name, ft);
    }

    /** Registers the Artillery Fire Lance (Campaign Operations p. 64): a Fire Lance variant requiring at least two artillery-armed units. */
    private static void createArtilleryFireLance() {
        FormationType ft = new FormationType("Artillery Fire", "Fire");
        ft.allowedUnitTypes = FLAG_GROUND;
        ft.missionRoles.add(MissionRole.MIXED_ARTILLERY);
        ft.otherCriteria.add(new CountConstraint(2,
              ms -> ms.getEquipmentNames()
                    .stream()
                    .map(EquipmentType::get)
                    .anyMatch(eq -> eq instanceof ArtilleryWeapon),
              "Artillery"));
        ft.reportMetrics.put("Artillery", ms -> ft.otherCriteria.getFirst().criterion.test(ms));
        allFormationTypes.put(ft.name, ft);
    }

    /** Registers the Direct Fire Lance (Campaign Operations p. 64): a Fire Lance variant requiring all units deliver at least 10 damage at range 18. */
    private static void createDirectFireLance() {
        FormationType ft = new FormationType("Direct Fire", "Fire");
        ft.allowedUnitTypes = FLAG_GROUND_NO_LIGHT;
        ft.mainCriteria = ms -> getDamageAtRange(ms, 18) >= 10;
        ft.mainDescription = "Damage 10 at range 18";
        ft.otherCriteria.add(new CountConstraint(2,
              ms -> ms.getWeightClass() >= EntityWeightClass.WEIGHT_HEAVY,
              "Heavy+"));
        ft.reportMetrics.put("Damage @ 18", ms -> getDamageAtRange(ms, 18));
        allFormationTypes.put(ft.name, ft);
    }

    /** Registers the Fire Support Lance (Campaign Operations p. 64): a Fire Lance variant requiring at least three units with indirect-fire capability. */
    private static void createFireSupportLance() {
        FormationType ft = new FormationType("Fire Support", "Fire");
        ft.allowedUnitTypes = FLAG_GROUND;
        ft.otherCriteria.add(new CountConstraint(3,
              ms -> ms.getEquipmentNames()
                    .stream()
                    .map(EquipmentType::get)
                    .anyMatch(eq -> (eq instanceof WeaponType) && ((WeaponType) eq).hasIndirectFire()),
              "Indirect fire weapon"));
        ft.reportMetrics.put("Indirect", ms -> ft.otherCriteria.getFirst().criterion.test(ms));
        allFormationTypes.put(ft.name, ft);
    }

    /**
     * Registers the Light Fire Lance (Campaign Operations p. 64): a Fire Lance variant restricted to light and
     * medium weight where at least 50% of the units have the Missile Boat or Sniper role; light Meks combine
     * fire to bring down larger targets.
     */
    private static void createLightFireLance() {
        FormationType ft = new FormationType("Light Fire", "Fire");
        ft.allowedUnitTypes = FLAG_GROUND;
        ft.maxWeightClass = EntityWeightClass.WEIGHT_MEDIUM;
        // Campaign Operations (Light Fire Lance): at least 50% must have the Missile Boat or Sniper role.
        ft.otherCriteria.add(new PercentConstraint(0.5,
              ms -> ms.getRole().isAnyOf(SNIPER, MISSILE_BOAT),
              "Sniper, Missile Boat"));
        allFormationTypes.put(ft.name, ft);
    }

    /**
     * Registers the Pursuit Lance (Campaign Operations p. 65): a high-speed scout-hunter formation built for
     * mobility and selective firepower. Ideal role: Striker.
     */
    private static void createPursuitLance() {
        FormationType ft = new FormationType("Pursuit");
        ft.allowedUnitTypes = FLAG_GROUND;
        // Campaign Operations (Pursuit Lance): Ideal Role - Striker.
        ft.idealRole = STRIKER;
        ft.maxWeightClass = EntityWeightClass.WEIGHT_MEDIUM;
        ft.otherCriteria.add(new PercentConstraint(0.75, ms -> ms.getWalkMp() >= 6, "Walk/Cruise 6+"));
        ft.otherCriteria.add(new CountConstraint(1,
              ms -> getSingleWeaponDamageAtRange(ms, 15) >= 5,
              "Weapon with damage 5+ at range 15"));
        ft.reportMetrics.put("Damage @ 15", ms -> getSingleWeaponDamageAtRange(ms, 15));
        allFormationTypes.put(ft.name, ft);
    }

    /** Registers the Probe Lance (Campaign Operations p. 65): a Pursuit Lance variant excluding assault weight, requiring all units deliver 10+ damage at range 9. */
    private static void createProbeLance() {
        FormationType ft = new FormationType("Probe", "Pursuit");
        ft.allowedUnitTypes = FLAG_GROUND;
        ft.maxWeightClass = EntityWeightClass.WEIGHT_HEAVY;
        ft.mainCriteria = ms -> getDamageAtRange(ms, 9) >= 10;
        ft.mainDescription = "Damage 10+ at range 9";
        ft.otherCriteria.add(new PercentConstraint(0.75, ms -> ms.getWalkMp() >= 6, "Walk/Cruise 6+"));
        ft.reportMetrics.put("Damage @ 9", ms -> getDamageAtRange(ms, 9));
        allFormationTypes.put(ft.name, ft);
    }

    /** Registers the Sweep Lance (Campaign Operations p. 65): a Pursuit Lance variant emphasizing short-range damage at speed; all units must deliver 10+ damage at range 6. */
    private static void createSweepLance() {
        FormationType ft = new FormationType("Sweep", "Pursuit");
        ft.allowedUnitTypes = FLAG_GROUND;
        ft.maxWeightClass = EntityWeightClass.WEIGHT_MEDIUM;
        ft.mainCriteria = ms -> ms.getWalkMp() >= 5 && getDamageAtRange(ms, 6) >= 10;
        ft.mainDescription = "Walk/Cruise 5+, Damage 10+ at range 6";
        ft.reportMetrics.put("Damage @ 6", ms -> getDamageAtRange(ms, 6));
        allFormationTypes.put(ft.name, ft);
    }

    /**
     * Registers the Recon Lance (Campaign Operations p. 65): a fast scouting formation that runs ahead of the
     * main force to gather intelligence and harass enemies. Ideal role: Scout.
     */
    private static void createReconLance() {
        FormationType ft = new FormationType("Recon");
        ft.allowedUnitTypes = FLAG_GROUND;
        ft.idealRole = UnitRole.SCOUT;
        ft.mainCriteria = ms -> ms.getWalkMp() >= 5;
        ft.mainDescription = "Walk/Cruise 5+";
        ft.otherCriteria.add(new CountConstraint(2, ms -> ms.getRole().isAnyOf(SCOUT, STRIKER), "Scout, Striker"));
        allFormationTypes.put(ft.name, ft);
    }

    /** Registers the Heavy Recon Lance (Campaign Operations p. 65): a Recon Lance variant including a heavy-weight backbone and excluding infantry/VTOL. */
    private static void createHeavyReconLance() {
        FormationType ft = new FormationType("Heavy Recon", "Recon");
        ft.allowedUnitTypes = FLAG_GROUND_NO_LIGHT;
        ft.mainCriteria = ms -> ms.getWalkMp() >= 4;
        ft.mainDescription = "Walk/Cruise 4+";
        ft.otherCriteria.add(new CountConstraint(2, ms -> ms.getWalkMp() >= 5, "Walk/Cruise 5+"));
        ft.otherCriteria.add(new CountConstraint(2, ms -> ms.getRole() == SCOUT, "Scout"));
        ft.otherCriteria.add(new CountConstraint(1,
              ms -> ms.getWeightClass() >= EntityWeightClass.WEIGHT_HEAVY,
              "Heavy+"));
        allFormationTypes.put(ft.name, ft);
    }

    /** Registers the Light Recon Lance (Campaign Operations p. 65): a Recon Lance variant restricted to light weight and the Scout role. */
    private static void createLightReconLance() {
        FormationType ft = new FormationType("Light Recon", "Recon");
        ft.allowedUnitTypes = FLAG_GROUND;
        ft.maxWeightClass = EntityWeightClass.WEIGHT_LIGHT;
        ft.mainCriteria = ms -> ms.getWalkMp() >= 6 && ms.getRole() == SCOUT;
        ft.mainDescription = "Walk/Cruise 6+, Scout";
        allFormationTypes.put(ft.name, ft);
    }

    /**
     * Registers the Security Lance (Campaign Operations p. 65): a defensive formation built for independent
     * operations, blending scouts/strikers with snipers; at most one assault unit allowed.
     */
    private static void createSecurityLance() {
        FormationType ft = new FormationType("Security");
        ft.allowedUnitTypes = FLAG_GROUND;
        ft.otherCriteria.add(new CountConstraint(1, ms -> ms.getRole().isAnyOf(SCOUT, STRIKER), "Scout, Striker"));
        ft.otherCriteria.add(new CountConstraint(1,
              ms -> ms.getRole().isAnyOf(SNIPER, MISSILE_BOAT),
              "Sniper, Missile Boat"));
        ft.otherCriteria.add(new MaxCountConstraint(1,
              ms -> ms.getWeightClass() >= EntityWeightClass.WEIGHT_ASSAULT,
              "Not assault"));
        allFormationTypes.put(ft.name, ft);
    }

    /**
     * Registers the Striker/Cavalry Lance (Campaign Operations p. 66): a fast-moving formation that brings
     * firepower into combat quickly while remaining able to withdraw. Ideal role: Striker.
     */
    private static void createStrikerCavalryLance() {
        FormationType ft = new FormationType("Striker/Cavalry");
        ft.allowedUnitTypes = FLAG_GROUND;
        ft.idealRole = UnitRole.STRIKER;
        ft.maxWeightClass = EntityWeightClass.WEIGHT_HEAVY;
        ft.mainCriteria = ms -> ms.getWalkMp() >= 5 || ms.getJumpMp() >= 4;
        ft.mainDescription = "Walk/Cruise 5+ or Jump 4+";
        ft.otherCriteria.add(new PercentConstraint(0.5,
              ms -> ms.getRole().isAnyOf(STRIKER, SKIRMISHER),
              "Striker, Skirmisher"));
        allFormationTypes.put(ft.name, ft);
    }

    /**
     * Registers the Hammer Lance (Campaign Operations p. 66): a House Marik Striker/Cavalry variant trained to
     * flank or rear-attack while the Anvil Lance holds the front. Ideal role: Striker.
     */
    private static void createHammerLance() {
        FormationType ft = new FormationType("Hammer", "Striker/Cavalry");
        ft.allowedUnitTypes = FLAG_GROUND;
        ft.exclusiveFaction = "FWL";
        ft.idealRole = UnitRole.STRIKER;
        ft.mainCriteria = ms -> ms.getWalkMp() >= 5;
        ft.mainDescription = "Walk/Cruise 5+";
        allFormationTypes.put(ft.name, ft);
    }

    /** Registers the Heavy Striker/Cavalry Lance (Campaign Operations p. 66): a heavy-weight Striker/Cavalry variant requiring long-range firepower. */
    private static void createHeavyStrikerCavalryLance() {
        FormationType ft = new FormationType("Heavy Striker/Cavalry", "Striker/Cavalry");
        ft.allowedUnitTypes = FLAG_GROUND_NO_LIGHT;
        ft.minWeightClass = EntityWeightClass.WEIGHT_MEDIUM;
        ft.mainCriteria = ms -> ms.getWalkMp() >= 4;
        ft.mainDescription = "Walk/Cruise 4+";
        ft.otherCriteria.add(new CountConstraint(3,
              ms -> ms.getWeightClass() >= EntityWeightClass.WEIGHT_HEAVY,
              "Heavy+"));
        ft.otherCriteria.add(new CountConstraint(2,
              ms -> ms.getRole().isAnyOf(STRIKER, SKIRMISHER),
              "Striker, Skirmisher"));
        ft.otherCriteria.add(new CountConstraint(1,
              ms -> getSingleWeaponDamageAtRange(ms, 18) >= 5,
              "Weapon with damage 5+ at range 18"));
        ft.reportMetrics.put("Damage @ 18", ms -> getSingleWeaponDamageAtRange(ms, 18));
        allFormationTypes.put(ft.name, ft);
    }

    /**
     * Registers the Horde (Campaign Operations p. 66): a Striker/Cavalry variant of light Meks swarming larger
     * opponents through numbers; CamOps requires 5–10 units (size enforced outside this class).
     */
    private static void createHordeLance() {
        FormationType ft = new FormationType("Horde", "Striker/Cavalry");
        ft.allowedUnitTypes = FLAG_GROUND;
        ft.maxWeightClass = EntityWeightClass.WEIGHT_LIGHT;
        ft.mainCriteria = ms -> getDamageAtRange(ms, 9) <= 10;
        ft.mainDescription = "Damage <= 10 at range 9";
        ft.reportMetrics.put("Damage @ 9", ms -> getDamageAtRange(ms, 9));
        allFormationTypes.put(ft.name, ft);
    }

    /** Registers the Light Striker/Cavalry Lance (Campaign Operations p. 66): a light-weight Striker/Cavalry variant emphasizing long-range damage. */
    private static void createLightStrikerCavalryLance() {
        FormationType ft = new FormationType("Light Striker/Cavalry", "Striker/Cavalry");
        ft.allowedUnitTypes = FLAG_GROUND;
        ft.maxWeightClass = EntityWeightClass.WEIGHT_MEDIUM;
        ft.mainCriteria = ms -> ms.getWalkMp() >= 5;
        ft.mainDescription = "Walk/Cruise 5+";
        ft.otherCriteria.add(new CountConstraint(2,
              ms -> getSingleWeaponDamageAtRange(ms, 18) >= 5,
              "Weapon with damage 5+ at range 18"));
        ft.otherCriteria.add(new CountConstraint(2,
              ms -> ms.getRole().isAnyOf(STRIKER, SKIRMISHER),
              "Striker, Skirmisher"));
        ft.reportMetrics.put("Damage @ 18", ms -> getSingleWeaponDamageAtRange(ms, 18));
        allFormationTypes.put(ft.name, ft);
    }

    /**
     * Registers the Support Lance (Campaign Operations p. 66): a multi-role formation that backs up other formations
     * rather than excelling on its own. CamOps states "Requirements: None; Ideal Role: None"; the bonus ability
     * (mirroring the supported formation's SPAs) is a gameplay-time effect handled outside this class, so the
     * registration here is intentionally constraint-free.
     */
    private static void createSupportLance() {
        FormationType ft = new FormationType("Support");
        ft.allowedUnitTypes = FLAG_GROUND;
        allFormationTypes.put(ft.name, ft);
    }

    /**
     * Registers the Ranger Lance (Campaign Operations p. 66): a Striker/Cavalry variant trained for combat in
     * difficult terrain. Ideal role: Skirmisher.
     */
    private static void createRangerLance() {
        FormationType ft = new FormationType("Ranger", "Striker/Cavalry");
        ft.allowedUnitTypes = FLAG_GROUND;
        // Campaign Operations (Ranger Lance): Ideal Role - Skirmisher.
        ft.idealRole = SKIRMISHER;
        ft.maxWeightClass = EntityWeightClass.WEIGHT_HEAVY;
        allFormationTypes.put(ft.name, ft);
    }

    /**
     * Registers the Urban Combat Lance (Campaign Operations p. 67): a formation favoring jump movement and slow
     * units for short-range city fighting. Ideal role: Ambusher.
     */
    private static void createUrbanLance() {
        FormationType ft = new FormationType("Urban");
        ft.allowedUnitTypes = FLAG_GROUND;
        ft.idealRole = UnitRole.AMBUSHER;
        ft.otherCriteria.add(new PercentConstraint(0.5,
              ms -> ms.getJumpMp() > 0 ||
                    ms.getUnitType().equals(UnitType.getTypeName(UnitType.INFANTRY)) ||
                    ms.getUnitType().equals(UnitType.getTypeName(UnitType.BATTLE_ARMOR)),
              "Jump 1+ or Infantry/BA"));
        ft.otherCriteria.add(new PercentConstraint(0.5, ms -> ms.getWalkMp() <= 4, "Walk/Cruise <= 4"));
        allFormationTypes.put(ft.name, ft);
    }

    /**
     * Registers the Aerospace Superiority Squadron (Campaign Operations p. 67): the air-combat counterpart to
     * the Battle Lance, built around Interceptor and Fast Dogfighter roles.
     */
    private static void createAerospaceSuperioritySquadron() {
        FormationType ft = new FormationType("Aerospace Superiority Squadron");
        ft.allowedUnitTypes = FLAG_FIGHTER;
        ft.otherCriteria.add(new PercentConstraint(0.51,
              ms -> ms.getRole().isAnyOf(INTERCEPTOR, FAST_DOGFIGHTER),
              "Interceptor/Fast Dogfighter"));
        ft.groupingCriteria = new GroupingConstraint(FLAG_FIGHTER,
              2,
              0,
              ms -> true,
              (ms0, ms1) -> ms0.getChassis().equals(ms1.getChassis()),
              "Same chassis");
        allFormationTypes.put(ft.name, ft);
    }

    /**
     * Registers the Electronic Warfare Squadron (Campaign Operations p. 67): a fighter formation dedicated to
     * disrupting enemy communications and ECM via Probe, ECM suite, or TAG equipment.
     */
    private static void createEWSquadron() {
        FormationType ft = new FormationType("Electronic Warfare Squadron");
        ft.allowedUnitTypes = FLAG_FIGHTER;
        ft.otherCriteria.add(new PercentConstraint(0.51,
              ms -> ms.getEquipmentNames()
                    .stream()
                    .map(EquipmentType::get)
                    .anyMatch(et -> et instanceof TAGWeapon ||
                          (et instanceof MiscType &&
                                (et.hasFlag(MiscType.F_BAP) || et.hasFlag(MiscType.F_ECM)))),
              "Probe, ECM, TAG"));
        ft.groupingCriteria = new GroupingConstraint(FLAG_FIGHTER,
              2,
              0,
              ms -> true,
              (ms0, ms1) -> ms0.getChassis().equals(ms1.getChassis()),
              "Same chassis");
        ft.reportMetrics.put("Probe/ECM/TAG", ms -> ft.otherCriteria.getFirst().criterion.test(ms));
        allFormationTypes.put(ft.name, ft);
    }

    /**
     * Registers the Fire Support Squadron (Campaign Operations p. 68): an aerospace formation suited to
     * ground-attack and long-range engagement, blending Fire Support and Dogfighter roles.
     */
    private static void createFireSupportSquadron() {
        FormationType ft = new FormationType("Fire Support Squadron");
        ft.allowedUnitTypes = FLAG_FIGHTER;
        ft.mainCriteria = ms -> ms.getRole().isAnyOf(FIRE_SUPPORT, DOGFIGHTER);
        ft.mainDescription = "Fire Support, Dogfighter";
        ft.otherCriteria.add(new PercentConstraint(0.5, ms -> ms.getRole() == FIRE_SUPPORT, "Fire Support"));
        ft.groupingCriteria = new GroupingConstraint(FLAG_FIGHTER,
              2,
              0,
              ms -> true,
              (ms0, ms1) -> ms0.getChassis().equals(ms1.getChassis()),
              "Same chassis");
        allFormationTypes.put(ft.name, ft);
    }

    /**
     * Registers the Interceptor Squadron (Campaign Operations p. 68): a fast aerospace formation built to deliver
     * the first strike against opposing aerospace threats.
     */
    private static void createInterceptorSquadron() {
        FormationType ft = new FormationType("Interceptor Squadron");
        ft.allowedUnitTypes = FLAG_FIGHTER;
        ft.otherCriteria.add(new PercentConstraint(0.51, ms -> ms.getRole() == INTERCEPTOR, "Interceptor"));
        ft.groupingCriteria = new GroupingConstraint(FLAG_FIGHTER,
              2,
              0,
              ms -> true,
              (ms0, ms1) -> ms0.getChassis().equals(ms1.getChassis()),
              "Same chassis");
        allFormationTypes.put(ft.name, ft);
    }

    /**
     * Registers the Strike Squadron (Campaign Operations p. 68): an aerospace formation suited to close air
     * support and ground-attack, balancing firepower with armor.
     */
    private static void createStrikeSquadron() {
        FormationType ft = new FormationType("Strike Squadron");
        ft.allowedUnitTypes = FLAG_FIGHTER;
        ft.otherCriteria.add(new PercentConstraint(0.51,
              ms -> ms.getRole().isAnyOf(ATTACK_FIGHTER, DOGFIGHTER),
              "Attack, Dogfighter"));
        ft.groupingCriteria = new GroupingConstraint(FLAG_FIGHTER,
              2,
              0,
              ms -> true,
              (ms0, ms1) -> ms0.getChassis().equals(ms1.getChassis()),
              "Same chassis");
        allFormationTypes.put(ft.name, ft);
    }

    /**
     * Registers the Transport Squadron (Campaign Operations p. 68): an aerospace cargo/troop-movement formation
     * that may include support aircraft, fighters, small craft, or DropShips.
     */
    private static void createTransportSquadron() {
        FormationType ft = new FormationType("Transport Squadron");
        ft.allowedUnitTypes = FLAG_FIGHTER | FLAG_SMALL_CRAFT | FLAG_DROPSHIP;
        ft.otherCriteria.add(new PercentConstraint(0.5, ms -> ms.getRole() == TRANSPORT, "Transport"));
        ft.groupingCriteria = new GroupingConstraint(FLAG_FIGHTER,
              2,
              Integer.MAX_VALUE,
              ms -> true,
              (ms0, ms1) -> ms0.getChassis().equals(ms1.getChassis()),
              "Same chassis");
        allFormationTypes.put(ft.name, ft);
    }

    /**
     * Helper function used by some grouping constraints to compare units. Units are considered to match if they are the
     * same model, but OmniMeks can match with different configurations. This is used primarily for ground units;
     * aerospace units match based on chassis.
     *
     * @param ms0 {@link MekSummary} First
     * @param ms1 {@link MekSummary} Second
     *
     * @return Whether the two units are considered the same for grouping considerations.
     */
    private static boolean checkUnitMatch(final MekSummary ms0, final MekSummary ms1) {
        final ModelRecord mRec = RATGenerator.getInstance().getModelRecord(ms0.getName());
        if (null != mRec && mRec.isOmni()) {
            return ms0.getChassis().equals(ms1.getChassis());
        } else {
            return ms0.getName().equals(ms1.getName());
        }
    }

    /**
     * Abstract base class for a secondary requirement on a formation: a predicate that some number of units must
     * satisfy.
     *
     * <p>Constraints support an OR-style "paired" mechanism for cases where the rule allows one of two
     * alternatives (e.g., Assault Lance requires <em>1 Juggernaut OR 2 Snipers</em>). Mark the first alternative
     * with {@link #setPairedWithNext(boolean)} and the second with {@link #setPairedWithPrevious(boolean)}; the
     * pair must be added consecutively to {@link FormationType#getOtherCriteria()}.
     *
     * @see CountConstraint
     * @see GroupingConstraint
     */
    public static abstract class Constraint {
        Predicate<MekSummary> criterion;
        String description;
        boolean pairedWithNext;
        boolean pairedWithPrevious;

        /**
         * @param criterion   predicate identifying units that satisfy this constraint
         * @param description human-readable rule description, used in the qualifications report
         */
        protected Constraint(Predicate<MekSummary> criterion, String description) {
            this.criterion = criterion;
            this.description = description;
        }

        /**
         * @param unitSize the total number of units in the formation
         *
         * @return the minimum number of units that must satisfy {@link #matches(MekSummary)} for this constraint
         *       to be considered met. May depend on {@code unitSize} (e.g., percent constraints).
         */
        public abstract int getMinimum(int unitSize);

        /** @return the human-readable description of this constraint */
        public String getDescription() {
            return description;
        }

        /**
         * @param ms the unit to test
         *
         * @return whether the unit satisfies this constraint's predicate
         */
        public boolean matches(MekSummary ms) {
            return criterion.test(ms);
        }

        /**
         * @return {@code true} if this constraint represents the second alternative in an OR pair; the constraint
         *       is then evaluated only if the preceding constraint's minimum is not met
         */
        public boolean isPairedWithPrevious() {
            return pairedWithPrevious;
        }

        /** Marks this constraint as the second alternative in an OR pair. */
        public void setPairedWithPrevious(boolean paired) {
            pairedWithPrevious = paired;
        }

        /**
         * @return {@code true} if this constraint is the first alternative in an OR pair; the next constraint in
         *       the list is evaluated when this one falls short
         */
        public boolean isPairedWithNext() {
            return pairedWithNext;
        }

        /** Marks this constraint as the first alternative in an OR pair. */
        public void setPairedWithNext(boolean paired) {
            pairedWithNext = paired;
        }
    }

    /**
     * A {@link Constraint} requiring a fixed minimum number of units to satisfy the criterion, regardless of
     * formation size. Used for rules like "at least 3 units must be Heavy or larger".
     */
    public static class CountConstraint extends Constraint {
        int count;

        /**
         * @param min         the minimum count of matching units
         * @param criterion   predicate identifying units that satisfy this constraint
         * @param description human-readable rule description
         */
        public CountConstraint(int min, Predicate<MekSummary> criterion, String description) {
            super(criterion, description);
            count = min;
        }

        @Override
        public int getMinimum(int unitSize) {
            return count;
        }
    }

    private static class MaxCountConstraint extends CountConstraint {

        public MaxCountConstraint(int max, Predicate<MekSummary> criterion, String description) {
            super(max, criterion.negate(), description);
        }

        @Override
        public int getMinimum(int unitSize) {
            return unitSize - count;
        }
    }

    private static class PercentConstraint extends Constraint {
        double pct;

        public PercentConstraint(double min, Predicate<MekSummary> criterion, String description) {
            super(criterion, description);
            pct = min;
        }

        @Override
        public int getMinimum(int unitSize) {
            return (int) Math.ceil(pct * unitSize);
        }
    }

    /**
     * A {@link Constraint} that requires a subset of the formation to form one or more matched groups (typically
     * pairs) of units sharing a chassis or model. Used for rules like "two matched pairs of heavy vehicles" or
     * "fighters operate in identical pairs".
     *
     * <p>A grouping is described by:
     * <ul>
     *   <li>{@code unitTypes} — which {@code FLAG_*} categories the rule applies to (other unit types in the
     *       formation are ignored by the grouping check);</li>
     *   <li>{@code groupSize} — how many units make up one group (typically 2 for "pairs");</li>
     *   <li>{@code numGroups} — how many such groups are required;</li>
     *   <li>a per-unit {@code generalConstraint} (e.g., "must be Heavy") and a pairwise {@code groupConstraint}
     *       (e.g., "same chassis").</li>
     * </ul>
     */
    public static class GroupingConstraint extends Constraint {
        int unitTypes = FLAG_ALL;
        int groupSize = 2;
        int numGroups = 1;
        BiFunction<MekSummary, MekSummary, Boolean> groupConstraint;

        /**
         * Constructs a grouping constraint that applies to all unit types ({@link #FLAG_ALL}), with the default
         * {@code groupSize=2} and {@code numGroups=1}.
         *
         * @param generalConstraint per-unit predicate (which units the rule applies to)
         * @param groupConstraint   pairwise predicate testing whether two units belong to the same group
         * @param description       human-readable rule description
         */
        public GroupingConstraint(Predicate<MekSummary> generalConstraint,
              BiFunction<MekSummary, MekSummary, Boolean> groupConstraint, String description) {
            super(generalConstraint, description);
            this.groupConstraint = groupConstraint;
        }

        /**
         * @param unitTypes         bitwise-OR of {@code FLAG_*} masks identifying which unit-type categories this
         *                          grouping applies to (units of other types are exempt)
         * @param groupSize         number of units per group (e.g., {@code 2} for pairs)
         * @param numGroups         number of groups required
         * @param generalConstraint per-unit predicate (which units the rule applies to)
         * @param groupConstraint   pairwise predicate testing whether two units belong to the same group
         * @param description       human-readable rule description
         */
        public GroupingConstraint(int unitTypes, int groupSize, int numGroups, Predicate<MekSummary> generalConstraint,
              BiFunction<MekSummary, MekSummary, Boolean> groupConstraint, String description) {
            this(generalConstraint, groupConstraint, description);
            this.unitTypes = unitTypes;
            this.groupSize = groupSize;
            this.numGroups = numGroups;
        }

        /**
         * @param unitType a {@link UnitType} constant
         *
         * @return whether this grouping constraint applies to the given unit type
         */
        public boolean appliesTo(int unitType) {
            return ((1 << unitType) & unitTypes) != 0;
        }

        /** @return the required number of groups */
        public int getNumGroups() {
            return numGroups;
        }

        /** @return the required number of units per group (e.g., {@code 2} for pairs) */
        public int getGroupSize() {
            return groupSize;
        }

        /**
         * Tests whether the unit satisfies the per-unit ("general") predicate. Unlike the base
         * {@link Constraint#matches(MekSummary)}, returns {@code true} when no general predicate is set.
         */
        @Override
        public boolean matches(MekSummary ms) {
            return criterion == null || criterion.test(ms);
        }

        /**
         * @param ms1 first unit
         * @param ms2 second unit
         *
         * @return whether the two units belong to the same group under the pairwise predicate (e.g., same chassis)
         */
        public boolean matches(MekSummary ms1, MekSummary ms2) {
            return groupConstraint.apply(ms1, ms2);
        }

        /**
         * @param unitSize the formation size
         *
         * @return {@code groupSize * numGroups}, clamped so the requirement does not exceed the formation size
         */
        @Override
        public int getMinimum(int unitSize) {
            int gs = Math.min(groupSize, unitSize);
            int ng = numGroups;
            if (gs > 0) {
                ng = Math.min(ng, unitSize / gs);
            }
            return gs * ng;
        }

        /** @return whether a per-unit ("general") predicate is set; if not, every unit is eligible for grouping */
        public boolean hasGeneralCriteria() {
            return criterion != null;
        }

        /** @return an independent copy of this grouping constraint, used for per-call overrides during generation */
        public GroupingConstraint copy() {
            return new GroupingConstraint(this.unitTypes,
                  this.groupSize,
                  this.numGroups,
                  this.criterion,
                  this.groupConstraint,
                  this.description);
        }
    }
}

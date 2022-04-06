package megamek.client.ratgenerator;

import megamek.common.*;
import megamek.common.weapons.artillery.ArtilleryWeapon;
import megamek.common.weapons.autocannons.ACWeapon;
import megamek.common.weapons.autocannons.LBXACWeapon;
import megamek.common.weapons.autocannons.UACWeapon;
import megamek.common.weapons.lrms.LRMWeapon;
import megamek.common.weapons.srms.SRMWeapon;
import megamek.common.weapons.tag.TAGWeapon;

import java.util.*;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

/**
 * Campaign Operations rules for force generation.
 * 
 * @author Neoancient
 */
public class FormationType {
    
    public static final int FLAG_MEK = 1 << UnitType.MEK;
    public static final int FLAG_TANK = 1 << UnitType.TANK;
    public static final int FLAG_BATTLE_ARMOR = 1 << UnitType.BATTLE_ARMOR;
    public static final int FLAG_INFANTRY = 1 << UnitType.INFANTRY;
    public static final int FLAG_PROTOMEK = 1 << UnitType.PROTOMEK;
    public static final int FLAG_VTOL = 1 << UnitType.VTOL;
    public static final int FLAG_NAVAL = 1 << UnitType.NAVAL;
    
    public static final int FLAG_CONV_FIGHTER = 1 << UnitType.CONV_FIGHTER;
    public static final int FLAG_AERO = 1 << UnitType.AERO;
    public static final int FLAG_SMALL_CRAFT = 1 << UnitType.SMALL_CRAFT;
    public static final int FLAG_DROPSHIP = 1 << UnitType.DROPSHIP;

    public static final int FLAG_GROUND = FLAG_MEK | FLAG_TANK | FLAG_BATTLE_ARMOR | FLAG_INFANTRY
            | FLAG_PROTOMEK | FLAG_VTOL | FLAG_NAVAL;
    public static final int FLAG_GROUND_NO_LIGHT = FLAG_MEK | FLAG_TANK | FLAG_BATTLE_ARMOR
            | FLAG_PROTOMEK | FLAG_NAVAL;
    public static final int FLAG_FIGHTER = FLAG_CONV_FIGHTER | FLAG_AERO;
    public static final int FLAG_AIR = FLAG_CONV_FIGHTER | FLAG_AERO | FLAG_SMALL_CRAFT
            | FLAG_DROPSHIP;
    public static final int FLAG_VEHICLE = FLAG_TANK | FLAG_NAVAL | FLAG_VTOL;
    public static final int FLAG_ALL = FLAG_GROUND | FLAG_AIR;
    
    private static HashMap<String, FormationType> allFormationTypes = null;
    public static FormationType getFormationType(String key) {
        if (allFormationTypes == null) {
            createFormationTypes();
        }
        return allFormationTypes.get(key);
    }
    
    public static Collection<FormationType> getAllFormations() {
        if (allFormationTypes == null) {
            createFormationTypes();
        }
        return allFormationTypes.values();
    }
    
    protected FormationType(String name) {
        this(name, name);
    }
    
    protected FormationType(String name, String category) {
        this.name = name;
        this.category = category;
    }
    
    private String name = "Support";
    private String category = null;
    private int allowedUnitTypes = FLAG_GROUND;
    // Some formation types allow units not normally generated for general combat roles (e.g. artillery, cargo)  
    private EnumSet<MissionRole> missionRoles = EnumSet.noneOf(MissionRole.class);
    // If all units in the force have this role, other constraints can be ignored.
    private UnitRole idealRole = UnitRole.UNDETERMINED;
    private String exclusiveFaction = null;
    
    private int minWeightClass = 0;
    private int maxWeightClass = EntityWeightClass.WEIGHT_COLOSSAL;
    // Used as a filter when generating units
    private Predicate<MechSummary> mainCriteria = ms -> true;
    // Additional criteria that have to be fulfilled by a portion of the force
    private List<Constraint> otherCriteria = new ArrayList<>();
    private GroupingConstraint groupingCriteria = null;
    
    // Provide values for the various criteria for reporting purposes
    private String mainDescription = null;
    private Map<String, Function<MechSummary,?>> reportMetrics = new HashMap<>();
    
    public String getName() {
        return name;
    }
    
    public String getCategory() {
        return category;
    }
    
    public int getAllowedUnitTypes() {
        return allowedUnitTypes;
    }
    
    public boolean isAllowedUnitType(int ut) {
        return (allowedUnitTypes & (1 << ut)) != 0;
    }
    
    public boolean isGround() {
        return (allowedUnitTypes & FLAG_AERO) == 0;
    }
    
    public UnitRole getIdealRole() {
        return idealRole;
    }
    
    public String getExclusiveFaction() {
        return exclusiveFaction;
    }
    
    public String getNameWithFaction() {
        return exclusiveFaction == null? name : name + " (" + exclusiveFaction + ")";
    }

    public int getMinWeightClass() {
        return minWeightClass;
    }

    public int getMaxWeightClass() {
        return maxWeightClass;
    }
    
    public Set<MissionRole> getMissionRoles() {
        return missionRoles;
    }
    
    public Predicate<MechSummary> getMainCriteria() {
        return mainCriteria;
    }
    
    public String getMainDescription() {
        return mainDescription;
    }
    
    public Iterator<Constraint> getOtherCriteria() {
        return otherCriteria.iterator();
    }
    
    public int getOtherCriteriaCount() {
        return otherCriteria.size();
    }
    
    public Constraint getConstraint(int index) {
        return otherCriteria.get(index);
    }
    
    public GroupingConstraint getGroupingCriteria() {
        return groupingCriteria;
    }
    
    public int getReportMetricsSize() {
        return reportMetrics.size();
    }
    
    public Iterator<String> getReportMetricKeys() {
        return reportMetrics.keySet().iterator();
    }
    
    public Function<MechSummary,?> getReportMetric(String key) {
        return reportMetrics.get(key);
    }
    
    private static Set<MissionRole> getMissionRoles(MechSummary ms) {
        ModelRecord mRec = RATGenerator.getInstance().getModelRecord(ms.getName());
        return mRec == null? EnumSet.noneOf(MissionRole.class) : mRec.getRoles();
    }
    
    private static IntSummaryStatistics damageAtRangeStats(MechSummary ms, int range) {
        List<Integer> retVal = new ArrayList<>();
        for (int i = 0; i < ms.getEquipmentNames().size(); i++) {
            if (EquipmentType.get(ms.getEquipmentNames().get(i)) instanceof WeaponType) {
                final WeaponType weapon = (WeaponType) EquipmentType.get(ms.getEquipmentNames().get(i));
                if (weapon.getLongRange() < range) {
                    continue;
                }
                int damage = 0;
                if (weapon.getAmmoType() != AmmoType.T_NA) {
                    Optional<EquipmentType> ammo = ms.getEquipmentNames().stream()
                        .map(EquipmentType::get)
                        .filter(eq -> eq instanceof AmmoType
                                && ((AmmoType) eq).getAmmoType() == weapon.getAmmoType()
                                && ((AmmoType) eq).getRackSize() == weapon.getRackSize())
                        .findFirst();
                    if (ammo.isPresent()) {
                        damage = ((AmmoType) ammo.get()).getDamagePerShot()
                                * Math.max(1, ((AmmoType) ammo.get()).getRackSize());
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
    
    private static long getDamageAtRange(MechSummary ms, int range) {
        return Math.max(0, damageAtRangeStats(ms, range).getSum());
    }
    
    private static long getSingleWeaponDamageAtRange(MechSummary ms, int range) {
        return Math.max(0, damageAtRangeStats(ms, range).getMax());
    }
    
    private static int getNetworkMask(MechSummary ms) {
        ModelRecord mRec = RATGenerator.getInstance().getModelRecord(ms.getName());
        return mRec == null? ModelRecord.NETWORK_NONE : mRec.getNetworkMask();
    }
    
    public List<MechSummary> generateFormation(UnitTable.Parameters params, int numUnits,
            int networkMask, boolean bestEffort) {
        List<UnitTable.Parameters> p = new ArrayList<>();
        p.add(params);
        List<Integer> n = new ArrayList<>();
        n.add(numUnits);
        return generateFormation(p, n, networkMask, bestEffort, -1, -1);
    }
    
    public List<MechSummary> generateFormation(List<UnitTable.Parameters> params, List<Integer> numUnits,
            int networkMask, boolean bestEffort) {
        return generateFormation(params, numUnits, networkMask, bestEffort, -1, -1);
    }
    
    public List<MechSummary> generateFormation(List<UnitTable.Parameters> params, List<Integer> numUnits,
            int networkMask, boolean bestEffort, int groupSize, int nGroups) {
        if (params.size() != numUnits.size() || params.isEmpty()) {
            throw new IllegalArgumentException("Formation parameter list and numUnit list must have the same number of elements.");
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
                useGrouping.numGroups =  nGroups;
                useGrouping.groupSize = 0;
            }
        }
        
        List<Integer> wcs = IntStream.rangeClosed(minWeightClass,
                Math.min(maxWeightClass, EntityWeightClass.WEIGHT_SUPER_HEAVY))
                .boxed()
                .collect(Collectors.toList());
        List<Integer> airWcs = wcs.stream().filter(wc -> wc < EntityWeightClass.WEIGHT_ASSAULT)
                .collect(Collectors.toList()); 
        params.forEach(p -> {
            p.getRoles().addAll(missionRoles);
            p.setWeightClasses(p.getUnitType() < UnitType.CONV_FIGHTER ? wcs : airWcs);
        });
        List<UnitTable> tables = params.stream().map(UnitTable::findTable).collect(Collectors.toList());
        //If there are any parameter sets that cannot generate a table, return an empty list. 
        if (!tables.stream().allMatch(UnitTable::hasUnits) && !bestEffort) {
            return new ArrayList<>();
        }
        
        /* Check whether we have vees or infantry that do not have the movement mode(s) set. If so,
         * we will attempt to conform them to a single type. Any that are set are ignored;
         * there is no attempt to conform to mode already in the force. If they are intended
         * to conform, they ought to be set.
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
        /* Look at the table for each group of parameters and determine the motive type
         * ratio, then weight those values according to the number of units using those
         * parameters.
         */
        Map<String,Integer> veeMap = new HashMap<>();
        Map<String,Integer> infMap = new HashMap<>();
        for (int i = 0; i < undeterminedVees.size(); i++) {
            for (int j = 0; j < tables.get(i).getNumEntries(); j++) {
                if (tables.get(i).getMechSummary(j) != null) {
                    veeMap.merge(tables.get(i).getMechSummary(j).getUnitSubType(),
                            tables.get(i).getEntryWeight(j) * numUnits.get(i), Integer::sum);
                }
            }
        }
        for (int i = 0; i < undeterminedInfantry.size(); i++) {
            for (int j = 0; j < tables.get(i).getNumEntries(); j++) {
                if (tables.get(i).getMechSummary(j) != null) {
                    infMap.merge(tables.get(i).getMechSummary(j).getUnitSubType(),
                            tables.get(i).getEntryWeight(j) * numUnits.get(i), Integer::sum);
                }
            }
        }
        
        /* Order modes in a way that those modes that are better represented are more likely to
         * be attempted first.
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

        /* if there are no units of a given type, we want to make sure we have at least one iteration */
        if (veeModeAttemptOrder.isEmpty() && !infModeAttemptOrder.isEmpty()) {
            veeModeAttemptOrder.add("Tracked");
        }
        if (infModeAttemptOrder.isEmpty() && !veeModeAttemptOrder.isEmpty()) {
            infModeAttemptOrder.add("Leg");
        }
        for (String veeMode : veeModeAttemptOrder) {
            for (String infMode : infModeAttemptOrder) {
                List<UnitTable.Parameters> tempParams = params.stream().map(UnitTable.Parameters::copy)
                        .collect(Collectors.toList());
                for (int index : undeterminedVees) {
                    tempParams.get(index).getMovementModes().add(EntityMovementMode.parseFromString(veeMode));
                }
                for (int index : undeterminedInfantry) {
                    tempParams.get(index).getMovementModes().add(EntityMovementMode.parseFromString(infMode));
                }
                List<MechSummary> list = generateFormation(tempParams, numUnits, networkMask, false);
                if (!list.isEmpty()) {
                    return list;
                }
            }
        }
        /* If we cannot meet all criteria with a specific motive type, try without respect to motive type */
        
        int cUnits = numUnits.stream().mapToInt(Integer::intValue).sum();

        /* Simple case: all units have the same requirements. */
        if (otherCriteria.isEmpty() && useGrouping == null
                && networkMask == ModelRecord.NETWORK_NONE) {
            List<MechSummary> retVal = new ArrayList<>();
            for (int i = 0; i < params.size(); i++) {
                retVal.addAll(tables.get(i).generateUnits(numUnits.get(i),
                        ms -> mainCriteria.test(ms)));
            }
            if (retVal.size() < cUnits) {
                List<MechSummary> matchRole = tryIdealRole(params, numUnits);
                if (matchRole != null) {
                    return matchRole;
                }
            }
            return retVal;
        }
        
        /* Simple case: single set of parameters and single additional criterion. */
        if (params.size() == 1 && otherCriteria.size() == 1 && useGrouping == null
                && networkMask == ModelRecord.NETWORK_NONE) {
            List<MechSummary> retVal = new ArrayList<>();
            retVal.addAll(tables.get(0).generateUnits(otherCriteria.get(0).getMinimum(numUnits.get(0)),
                    ms -> mainCriteria.test(ms) && otherCriteria.get(0).criterion.test(ms)));
            if (retVal.size() < otherCriteria.get(0).getMinimum(numUnits.get(0))) {
                List<MechSummary> onRole = tryIdealRole(params, numUnits);
                if (onRole != null) {
                    return onRole;
                } else if (!bestEffort) {
                    return new ArrayList<>();
                }
            }
            if (retVal.size() >= otherCriteria.get(0).getMinimum(numUnits.get(0)) || bestEffort) {
                retVal.addAll(tables.get(0).generateUnits(numUnits.get(0) - retVal.size(),
                        ms -> mainCriteria.test(ms)));
            }
            return retVal;
        }
        
        /* If a network is indicated, we decide which units are part of the network (usually
         * all, but not necessarily) and which combination to use, then assign one of them
         * to the master role if any. A company command lance has two configuration options:
         * a unit with two masters, or two master and two slaves.
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
            numMasters = 0;
            slaveType = ModelRecord.NETWORK_C3I;
            /* This mask is also used for naval C3 */
            validNetworkUnits |= FLAG_SMALL_CRAFT | FLAG_DROPSHIP;
        } else if ((networkMask & ModelRecord.NETWORK_NOVA) != 0) {
            numNetworked = 3;
            numMasters = 0;
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
        
        /* General case:
         * Select randomly from all unique combinations of the various criteria. Each combination
         * is represented by a Map<Integer,Integer in which the various criteria are encoded as the keys
         * and the value mapped to the index is the number of units that must fulfill those criteria.
         * The lowest order bits map to otherCriteria, one bit for each constraint. These are built
         * by shifting left for each new one added, so the one at index 0 is the leftmost bit of this
         * section. A 1 indicates that the number of units at that index must meet the constraint, while
         * a 0 means the constraint is not tested, and a unit may or may not fulfill it.
         * Example: if otherCriteria.size() == 3, then the value of combinations[6] is the number of
         * units that must meet the first two constraints (110), while combinations[7] must meet all
         * three and combinations[0] need not meet any.
         * 
         * The next three bits indicate C3 network requirements. The lowest order is the number that
         * must have a C3 slave, C3i, NC3, or Nova, depending on the value of networkMask. The middle bit
         * is the number of required C3 masters, and the highest bit is the number of dual-C3M units.
         * Note that only one of these three bits can be set; while a unit can have a C3M and a C3S,
         * only one can fulfill its role in the network.
         * 
         *  The highest order section is the unit type. Each element of the params list has one
         *  bit, beginning with the lowest order bit at index 0. As with networks, only one 
         *  bit in this section can be set.
         */
        
        do {
            List<Map<Integer,Integer>> combinations;
            /* We can get here with an empty otherCriteria if there is a groupingConstraint,
             * which is the case with the Order formation.
             */
            if (otherCriteria.isEmpty()) {
                Map<Integer,Integer> combo = new HashMap<>();
                combo.put(0, cUnits);
                combinations = new ArrayList<>();
                combinations.add(combo);
            } else {
                combinations = findCombinations(cUnits);
            }
            //Group units by param index so they can be returned in the order requested.
            Map<Integer,List<MechSummary>> list = new TreeMap<>();
            final int POS_C3S = 0;
            final int POS_C3M = 1;
            final int POS_C3MM = 2;
            final int POS_C3_NUM = 3;
            while (combinations.size() > 0) {
                int index = Compute.randomInt(combinations.size());
                Map<Integer,Integer> baseCombo = combinations.get(index);

                int[] networkGroups = new int[POS_C3_NUM];
                networkGroups[POS_C3S] = Math.max(0, numNetworked - numMasters);
                if ((networkMask & ModelRecord.NETWORK_COMPANY_COMMAND) == 0) {
                    networkGroups[POS_C3M] = Math.max(0, numMasters);
                } else {
                    networkGroups[POS_C3MM] = Math.max(0, numMasters);
                }
                List<Map<Integer,Integer>> networkGroupings = findGroups(baseCombo, networkGroups,
                        otherCriteria.size());
                if (altNumMasters > 0) {
                    networkGroups[POS_C3S] = Math.max(0, numNetworked - altNumMasters);
                    networkGroups[POS_C3M] = Math.max(0, altNumMasters);
                    networkGroups[POS_C3MM] = 0;
                    networkGroupings.addAll(findGroups(baseCombo, networkGroups, otherCriteria.size()));
                }
                while (networkGroupings.size() > 0) {
                    list.clear();
                    int networkIndex = Compute.randomInt(networkGroupings.size());
                    Map<Integer,Integer> combo = networkGroupings.get(networkIndex);

                    int[] unitsPerGroup = new int[params.size()];
                    for (int i = 0; i < numUnits.size(); i++) {
                        unitsPerGroup[i] = numUnits.get(i);
                    }
                    List<Map<Integer,Integer>> unitTypeGroupings = findGroups(combo, unitsPerGroup,
                            otherCriteria.size() + POS_C3_NUM);
                    while (unitTypeGroupings.size() > 0) {
                        list.clear();
                        int utIndex = Compute.randomInt(unitTypeGroupings.size());
                        combo = unitTypeGroupings.get(utIndex);

                        if (useGrouping != null
                                && params.stream().anyMatch(p -> useGrouping.appliesTo(p.getUnitType()))) {
                            /* Create a temporary map that only includes units that have a grouping criterion */
                            Map<Integer,Integer> groupedUnits = new LinkedHashMap<>();
                            for (int p = 0; p < params.size(); p++) {
                                if (useGrouping.appliesTo(params.get(p).getUnitType())) {
                                    for (Integer i : combo.keySet()) {
                                        if ((i & (1 << (p + otherCriteria.size() + POS_C3_NUM))) != 0) {
                                            groupedUnits.merge(i, combo.get(i), Integer::sum);
                                        }
                                    }
                                }
                            }
                            List<List<Map<Integer,Integer>>> groups = findMatchedGroups(groupedUnits, useGrouping);

                            while (groups.size() > 0) {
                                int gIndex = Compute.randomInt(groups.size());
                                list.clear();
                                Map<Integer,List<MechSummary>> found = new TreeMap<>();
                                Map<Integer,Integer> workingCombo = new HashMap<>(combo);
                                for (Map<Integer,Integer> g : groups.get(gIndex)) {
                                    /* The first unit selected may lead to a dead end, if the constraints
                                     * for the other group members cannot be met in a unit that matches the
                                     * base. To deal with this we make a second attempt if necessary
                                     * subjecting all members of the group to all constraints assigned
                                     * to any group member. */
                                    int extraCriteria = 0;
                                    int attempts = 0;
                                    while (attempts < 2) {
                                        found.clear();
                                        MechSummary base = null;
                                        for (int i : combo.keySet()) {
                                            if (g.containsKey(i)) {
                                                // Decode unit type
                                                int tableIndex = 0;
                                                if (params.size() > 0) {
                                                    int tmp = i >> (otherCriteria.size() + POS_C3_NUM);
                                                    while (tmp != 0 && (tmp & 1) == 0) {
                                                        tableIndex++;
                                                        tmp >>= 1;
                                                    }
                                                }
                                                final Predicate<MechSummary> filter = getFilterFromIndex(i | extraCriteria,
                                                        slaveType, masterType);
                                                for (int j = 0; j < g.get(i); j++) {
                                                    if (base == null) {
                                                        base = tables.get(tableIndex).generateUnit(filter::test);
                                                        if (base != null) {
                                                            found.putIfAbsent(tableIndex, new ArrayList<>());
                                                            found.get(tableIndex).add(base);
                                                        }
                                                    } else {
                                                        final MechSummary b = base;
                                                        MechSummary unit = tables.get(tableIndex).generateUnit(ms -> filter.test(ms)
                                                                && useGrouping.matches(ms, b));
                                                        if (unit != null) {
                                                            found.putIfAbsent(tableIndex, new ArrayList<>());
                                                            found.get(tableIndex).add(unit);
                                                        }
                                                    }
                                                }
                                            }
                                        }
                                        if (found.values().stream().mapToInt(List::size).sum()
                                                < g.values().stream().mapToInt(Integer::intValue).sum()) {
                                            found.clear();
                                            base = null;
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
                                    for (Map.Entry<Integer, List<MechSummary>> e : found.entrySet()) {
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
                                        int tableIndex = 0;
                                        if (params.size() > 0) {
                                            int tmp = i >> (otherCriteria.size() + POS_C3_NUM);
                                            while (tmp != 0 && (tmp & 1) == 0) {
                                                tableIndex++;
                                                tmp >>= 1;
                                            }
                                        }
                                        final Predicate<MechSummary> filter = getFilterFromIndex(i, slaveType, masterType);
                                        for (int j = 0; j < workingCombo.get(i); j++) {
                                            MechSummary unit = tables.get(tableIndex).generateUnit(filter::test);
                                            if (unit != null) {
                                                list.putIfAbsent(tableIndex, new ArrayList<>());
                                                list.get(tableIndex).add(unit);
                                            }
                                        }
                                    }
                                }
                                List<MechSummary> retVal = list.values().stream()
                                        .flatMap(Collection::stream).collect(Collectors.toList());
                                if (retVal.size() < cUnits) {
                                    groups.remove(gIndex);
                                } else {
                                    return retVal;
                                }
                            }
                        } else {
                            for (int i : combo.keySet()) {
                                // Decode unit type
                                int tableIndex = 0;
                                if (params.size() > 0) {
                                    int tmp = i >> (otherCriteria.size() + POS_C3_NUM);
                                    while (tmp != 0 && (tmp & 1) == 0) {
                                        tableIndex++;
                                        tmp >>= 1;
                                    }
                                }
                                final Predicate<MechSummary>filter = getFilterFromIndex(i, slaveType, masterType);
                                for (int j = 0; j < combo.get(i); j++) {
                                    MechSummary unit = tables.get(tableIndex).generateUnit(filter::test);
                                    if (unit != null) {
                                        list.putIfAbsent(tableIndex, new ArrayList<>());
                                        list.get(tableIndex).add(unit);
                                    }
                                }
                            }
                        }
                        List<MechSummary> retVal = list.values().stream()
                                .flatMap(Collection::stream).collect(Collectors.toList());
                        if (retVal.size() < cUnits) {
                            unitTypeGroupings.remove(utIndex);
                        } else {
                            return retVal;
                        }
                    }
                    List<MechSummary> retVal = list.values().stream()
                            .flatMap(Collection::stream).collect(Collectors.toList());
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
        
        List<MechSummary> onRole = tryIdealRole(params, numUnits);
        if (onRole != null) {
            return onRole;
        }
        return new ArrayList<>();
    }
    
    private Predicate<MechSummary> getFilterFromIndex(int index, int slaveType, int masterType) {
        Predicate<MechSummary> retVal = mainCriteria;
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
            retVal = retVal.and(ms -> (getNetworkMask(ms)
                    & (masterType | ModelRecord.NETWORK_COMPANY_COMMAND)) != 0);
        }
        return retVal;
    }
    
    /**
     * Attempts to build unit entirely on ideal role. Returns null if unsuccessful.
     */
    private List<MechSummary> tryIdealRole(List<UnitTable.Parameters> params, List<Integer> numUnits) {
        if (idealRole.equals(UnitRole.UNDETERMINED)) {
            return null;
        }
        List<UnitTable.Parameters> tmpParams = params.stream()
                .map(UnitTable.Parameters::copy).collect(Collectors.toList());
        tmpParams.forEach(p -> p.getWeightClasses().clear());
        List<MechSummary> retVal = new ArrayList<>();
        for (int i = 0; i < tmpParams.size(); i++) {
            UnitTable t = UnitTable.findTable(tmpParams.get(i));
            List<MechSummary> units = t.generateUnits(numUnits.get(i),
                    ms -> UnitRoleHandler.getRoleFor(ms).equals(idealRole));
            if (units.size() < numUnits.get(i)) {
                return null;
            }
        }
        return retVal;
    }

    /**
     * Finds all unique distributions of constraints among the units that fulfills the minimum
     * number for each constraint. The map keys indicate a combination of constraints, with the
     * highest order bit being the first constraint in the list, and the value mapped to that key being
     * the number of units that must meet the constraint.
     */
    private List<Map<Integer,Integer>> findCombinations(int numUnits) {
        /* This list is remade with each additional constraint, building on the previous values */
        List<Map<Integer,Integer>> frequencies = new ArrayList<>();

        for (Constraint c : otherCriteria) {
            int req = c.getMinimum(numUnits);
            /* If this is the first pass, we simply need to initialize the frequencies list */
            if (frequencies.isEmpty()) {
                Map<Integer,Integer> freq = new LinkedHashMap<>();
                freq.put(0, numUnits - req);
                freq.put(1, req);
                frequencies.add(freq);
            } else {
                /* Create a new list to hold the values built off the previous one */
                List<Map<Integer,Integer>> newFrequencies = new ArrayList<>();
                /* Iterate through all the values from the previous pass and extend them */
                for (Map<Integer,Integer> freq : frequencies) {
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
                    /* current holds the number of units at each index of the previous iteration
                     * that will meet the current constraint */
                    int[] current = new int[keyList.size()];
                    outer: while (remaining[index] >= toAllocate) {
                        current[index] = Math.min(freq.get(keyList.get(index)), toAllocate);
                        toAllocate -= current[index];
                        index++;
                        if (index == keyList.size()) {
                            if (c.isPairedWithPrevious()) {
                                Map<Integer,Integer> prevValues = new LinkedHashMap<>();
                                for (int i : freq.keySet()) {
                                    prevValues.put(i << 1, freq.get(i));
                                }
                                newFrequencies.add(prevValues);
                            }
                            Map<Integer,Integer> result = new LinkedHashMap<>();
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
                            /* Keep backing up until we find one we can decrease or we reach the beginning.
                             * We can decrease if the current value is > 0 and the remaining slots are
                             * big enough to hold toAllocate + 1.
                             */
                            while (index >= 0) {
                                if (current[index] == 0 || index + 1 == current.length
                                        || remaining[index + 1] <= toAllocate
                                        ) {
                                    toAllocate += current[index];
                                    index--;
                                } else {
                                    current[index]--;
                                    toAllocate++;
                                    index++;
                                    continue outer;
                                }
                            }
                            break outer;
                        }
                    }
                }
                frequencies = newFrequencies;
            }
        }
        return frequencies;
    }    
    
    /**
     * Finds all possible ways to distribute criteria beyond the general formation criteria in
     * which the groups are mutually exclusive; that is, a unit can only qualify for one
     * of the criteria in the set. This is used for mixed unit types and C3 networks. While a single
     * unit could fulfill the requirements for speed and weight class, it could not function
     * as both a C3 slave and a C3 master or be both a Mek and a Tank.
     *  
     * @param combination The current criteria distribution as generated by <code>findCombinations</code>
     * @param itemsPerGroup Array with length equal to number of groups and each value indicates
     * the number of units in that group.
     * @return A map the same format as <code>combination</code> in which higher order bits
     * in the key indicate a group. For example: in a formation with two criteria,
     * <code>combination.length</code> == 2^2. If there are three additional groups,
     * the return value will be 2 ^ (2+3). The value mapped to 11 (== 01011) will be the
     * number of units that are in the second group and fulfill both formation criteria.
     */
    private List<Map<Integer,Integer>> findGroups(Map<Integer,Integer> combination, int[] itemsPerGroup, int indexBits) {
        List<Integer> keyList = new ArrayList<>(combination.keySet());

        List<int[][]> list = new ArrayList<>();
        int[][] initialVal = new int[1][keyList.size()];
        list.add(initialVal);

        /* Compute distribution for each group sequentially, building on previously calculated
         * distributions for each successive group. */
        for (int group = 0; group < itemsPerGroup.length; group++) {
            /* Create a new list that we will fill out by copying the current values and adding
             * the next group calculated during this iteration. */
            List<int[][]> newList = new ArrayList<>();
            /* Cycle through all previous combinations and add all combinations for current group */
            for (int[][] prev : list) {
                /* Initialize array with the number of units at each position that have already been
                 * assigned to groups. */
                int[] total = new int[keyList.size()];
                for (int g = 0; g < prev.length; g++) {
                    for (int p = 0; p < prev[g].length; p++) {
                        total[p] += prev[g][p];
                    }
                }
                /* Create an array to track attempted distribution of the current group */
                int[] dist = new int[keyList.size()];
                dist[0] = itemsPerGroup[group];
                /* Shift values through the array until they are all in the final position */
                while (dist[dist.length - 1] <= itemsPerGroup[group]) {
                    /* Test whether there is room for the current distribution, and if so add it to
                     * the list */
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
                    /* Shift the values in the current distribution. Find the value > 0 closest to the
                     * end (not counting the final position), decrease it by 1, and set the value in
                     * the next position to 1 plus whatever was in the tail position (which becomes 0
                     * prior to incrementing */
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
        List<Map<Integer,Integer>> retVal = new ArrayList<>();
        for (int[][] val : list) {
            Map<Integer,Integer> newVal = new LinkedHashMap<>(combination);
            for (int g = 0; g < val.length; g++) {
                for (int i = 0; i < val[g].length; i++) {
                    if (val[g][i] > 0) {
                        newVal.put((1 << (g + indexBits)) + keyList.get(i), val[g][i]);
                        newVal.merge(keyList.get(i), - val[g][i], Integer::sum);
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
     * Special case version of <code>findGroups</code> for matched units (such as paired ASFs).
     * Because each group has identical criteria the number of possible results can be reduced.
     *  
     * @param combination The current criteria distribution as generated by <code>findCombinations</code>
     * @return A list of possible groupings. Each entry is a list of size() equal to numGroups.
     * The entry for each group is a map of the same format as <code>combination</code>.
     */
    private List<List<Map<Integer,Integer>>> findMatchedGroups(Map<Integer,Integer> combination,
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

        /* Compute distribution for each group sequentially, building on previously calculated
         * distributions for each successive group. */
        for (int group = 0; group < numGroups; group++) {
            /* Create a new list that we will fill out by copying the current values and adding
             * the next group calculated during this iteration. */
            List<int[][]> newList = new ArrayList<>();
            /* Cycle through all previous combinations and add all combinations for current group */
            for (int[][] prev : list) {
                /* Initialize array with the number of units at each position that have already been
                 * assigned to groups. */
                int[] total = new int[keyList.size()];
                for (int g = 0; g < prev.length; g++) {
                    for (int p = 0; p < prev[g].length; p++) {
                        total[p] += prev[g][p];
                    }
                }
                /* Find the starting position for the current group. We don't want to start earlier
                 * that the first position that has been assigned to a group; that will be a permutation
                 * of a result that has already been calculated. */

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
                    /* Test whether there is room for the current distribution, and if so add it to
                     * the list */
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
                    /* Shift the values in the current distribution. Find the value > 0 closest to the
                     * end (not counting the final position), decrease it by 1, and set the value in
                     * the next position to 1 plus whatever was in the tail position (which becomes 0
                     * prior to incrementing */
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
        List<List<Map<Integer,Integer>>> retVal = new ArrayList<>();
        for (int[][] grouping : list) {
            List<Map<Integer,Integer>> newGrouping = new ArrayList<>();
            for (int g = 0; g < grouping.length; g++) {
                Map<Integer,Integer> map = new HashMap<>();
                for (int p = 0; p < grouping[g].length; p++) {
                    map.put(keyList.get(p), grouping[g][p]);
                }
                newGrouping.add(map);
            }
            retVal.add(newGrouping);
        }

        return retVal;
    }
    
    /**
     * Tests whether a list of units qualifies for the formation type. Note that unit roles are
     * not available for all units.
     * @param units A list of units to test
     * @return Whether the list of units meets the qualifications for this formation.
     */
    public boolean qualifies(List<MechSummary> units) {
        if (units.stream().anyMatch(ms -> !isAllowedUnitType(ModelRecord.parseUnitType(ms.getUnitType())))) {
            return false;
        }
        if (!idealRole.equals(UnitRole.UNDETERMINED)) {
            if (units.stream().allMatch(ms -> idealRole.equals(UnitRoleHandler.getRoleFor(ms)))) {
                return true;
            }
        }
        for (MechSummary ms : units) {
            if (!mainCriteria.test(ms)
                    || ms.getWeightClass() < minWeightClass
                    || ms.getWeightClass() > maxWeightClass) {
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
                    i++;
                } else {
                    return false;
                }
            }
        }
        if (groupingCriteria != null) {
            /* First group by chassis, then test whether each group fulfills the requirement.
             * If not, regroup by name. */
            List<MechSummary> groupedUnits = units.stream()
                    .filter(ms -> groupingCriteria.appliesTo(ModelRecord.parseUnitType(ms.getUnitType())))
                    .collect(Collectors.toList());
            if (groupedUnits.size() > 0) {
                Map<String,List<MechSummary>> groups = groupedUnits.stream()
                        .collect(Collectors.groupingBy(MechSummary::getChassis));
                GROUP_LOOP: for (List<MechSummary> group : groups.values()) {
                    for (int i = 0; i < group.size() - 1; i++) {
                        for (int j = i + 1; j < group.size(); j++) {
                            if (!groupingCriteria.matches(group.get(i), group.get(j))) {
                                groups = groupedUnits.stream()
                                        .collect(Collectors.groupingBy(MechSummary::getName));
                                break GROUP_LOOP;
                            }
                        }
                    }
                }
                int groupSize = Math.min(groupingCriteria.getGroupSize(), groupedUnits.size());
                int numGroups = Math.min(groupingCriteria.getNumGroups(), groupedUnits.size() / groupSize);
                /* Allow for the possibility that two or more groups may be identical */
                int groupCount = 0;
                for (List<MechSummary> g : groups.values()) {
                    groupCount += g.size() / groupSize;
                }
                return groupCount >= numGroups;
            }
        }
        return true;
    }
    
    /**
     * Tests whether a list of units qualifies for the formation type. Note that unit roles are
     * not available for all units.
     * @param units A list of units to test
     * @return Whether the list of units meets the qualifications for this formation.
     */
    public String qualificationReport(List<MechSummary> units) {
        List<MechSummary> wrongUnits = new ArrayList<>();
        List<MechSummary> weight = new ArrayList<>();
        List<MechSummary> main = new ArrayList<>();
        List<List<MechSummary>> other = new ArrayList<>();
        for (int i = 0; i < otherCriteria.size(); i++) {
            other.add(new ArrayList<>());
        }

        for (MechSummary ms : units) {
            if (!isAllowedUnitType(ModelRecord.parseUnitType(ms.getUnitType()))) {
                wrongUnits.add(ms);
            }

            if (ms.getWeightClass() >= minWeightClass
                    && ms.getWeightClass() <= maxWeightClass) {
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
            sb.append(wrongUnits.stream().map(MechSummary::getName).collect(Collectors.joining("\n\t")))
                .append("<br/><br/>\n");
        }
        sb.append("Unit Roles:<br/>\n&nbsp;&nbsp;&nbsp;");
        sb.append(units.stream().map(ms -> ms.getName() + ": " + UnitRoleHandler.getRoleFor(ms))
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
            sb.append("&nbsp;&nbsp;&nbsp;").append(weight.stream().map(ms -> ms.getName() + ": "
                    + EntityWeightClass.getClassName(ms.getWeightClass()))
                    .collect(Collectors.joining("<br/>\n&nbsp;&nbsp;&nbsp;"))).append("<br/><br/>\n");
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
                sb.append("&nbsp;&nbsp;&nbsp;").append("\t").append(main.stream().map(MechSummary::getName)
                        .collect(Collectors.joining("<br/>\n&nbsp;&nbsp;&nbsp;"))).append("<br/><br/>\n");
            } else {
                sb.append("&nbsp;&nbsp;&nbsp;None<br/><br/>\n");
            }
        }

        for (int i = 0; i < otherCriteria.size(); i++) {
            boolean isShort = false;
            if (other.get(i).size() < otherCriteria.get(i).getMinimum(units.size())) {
                if (otherCriteria.get(i).isPairedWithNext()) {
                    isShort = i + 1 < otherCriteria.size()
                            && other.get(i + 1).size() < otherCriteria.get(i + 1).getMinimum(units.size());
                } else if (otherCriteria.get(i).isPairedWithPrevious()) {
                    isShort = i - 1 > 0
                            && other.get(i - 1).size() < otherCriteria.get(i - 1).getMinimum(units.size());
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
            sb.append(otherCriteria.get(i).description).append(" (")
                .append(otherCriteria.get(i).getMinimum(units.size())).append(")");
            sb.append("<br />\n");
            if (isShort) {
                sb.append("</font>");
            }

            if (other.get(i).size() > 0) {
                sb.append("&nbsp;&nbsp;&nbsp;").append(other.get(i).stream().map(MechSummary::getName)
                        .collect(Collectors.joining("<br/>\n&nbsp;&nbsp;&nbsp;"))).append("<br/><br/>\n");
            } else {
                sb.append("&nbsp;&nbsp;&nbsp;None<br/><br/>\n");
            }
        }

        if (groupingCriteria != null) {
            List<MechSummary> groupedUnits = units.stream()
                    .filter(ms -> groupingCriteria.appliesTo(ModelRecord.parseUnitType(ms.getUnitType())))
                    .collect(Collectors.toList());
            if (groupedUnits.size() > 0) {
                Map<String,List<MechSummary>> groups = groupedUnits.stream()
                        .collect(Collectors.groupingBy(MechSummary::getChassis));
                GROUP_LOOP: for (List<MechSummary> group : groups.values()) {
                    for (int i = 0; i < group.size() - 1; i++) {
                        for (int j = i + 1; j < group.size(); j++) {
                            if (!groupingCriteria.matches(group.get(i), group.get(j))) {
                                groups = groupedUnits.stream()
                                        .collect(Collectors.groupingBy(MechSummary::getName));
                                break GROUP_LOOP;
                            }
                        }
                    }
                }
                int groupSize = Math.min(groupingCriteria.getGroupSize(), groupedUnits.size());
                int numGroups = Math.min(groupingCriteria.getNumGroups(), groupedUnits.size() / groupSize);
                /* Allow for the possibility that two or more groups may be identical */
                int groupCount = 0;
                for (List<MechSummary> g : groups.values()) {
                    groupCount += g.size() / groupSize;
                }
                if (groupCount < numGroups) {
                    sb.append("<font color='red'>");
                }
                sb.append(groupingCriteria.getDescription()).append(" (").append(numGroups)
                    .append("x").append(groupSize).append(")");
                if (groupCount < numGroups) {
                    sb.append("</font>");
                }
                sb.append("<br/>\n");
                if (groupCount > 0) {
                    for (String groupName : groups.keySet()) {
                        int size = groups.get(groupName).size();
                        while (size >= groupSize) {
                            sb.append("&nbsp;&nbsp;&nbsp;").append(groupName)
                                .append(" (").append(groupSize).append(")<br/>\n");
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
        createUrbanLance();
        createAerospaceSuperioritySquadron();
        createEWSquadron();
        createFireSupportSquadron();
        createInterceptorSquadron();
        createStrikeSquadron();
        createTransportSquadron();
    }
    
    private static void createAntiMekLance() {
        FormationType ft = new FormationType("Anti-Mek");
        ft.allowedUnitTypes = FLAG_INFANTRY | FLAG_BATTLE_ARMOR;
        allFormationTypes.put(ft.name, ft);
    }
    
    private static void createAssaultLance() {
        FormationType ft = new FormationType("Assault");
        ft.allowedUnitTypes = FLAG_GROUND_NO_LIGHT;
        ft.idealRole = UnitRole.JUGGERNAUT;
        ft.minWeightClass = EntityWeightClass.WEIGHT_MEDIUM;
        ft.mainCriteria = ms -> ms.getTotalArmor() >= 135;
        ft.mainDescription = "Armor 135+";
        ft.otherCriteria.add(new PercentConstraint(0.75,
                ms -> getDamageAtRange(ms, 7) >= 25,
                "25 damage at range 7"));
        ft.otherCriteria.add(new CountConstraint(3,
                ms -> ms.getWeightClass() >= EntityWeightClass.WEIGHT_HEAVY,
                "Heavy+"));
        Constraint c = new CountConstraint(1, ms -> UnitRoleHandler.getRoleFor(ms).equals(UnitRole.JUGGERNAUT),
                "Juggernaut");
        c.setPairedWithNext(true);
        ft.otherCriteria.add(c);
        c = new CountConstraint(2, ms -> UnitRoleHandler.getRoleFor(ms).equals(UnitRole.SNIPER),
                "Sniper");
        c.setPairedWithPrevious(true);
        ft.otherCriteria.add(c);
        ft.reportMetrics.put("Armor", MechSummary::getTotalArmor);
        ft.reportMetrics.put("Damage @ 7", ms -> getDamageAtRange(ms, 7));
        allFormationTypes.put(ft.name, ft);
    }
    
    private static void createAnvilLance() {
        FormationType ft = new FormationType("Anvil", "Assault");
        ft.allowedUnitTypes = FLAG_GROUND_NO_LIGHT;
        ft.exclusiveFaction = "FWL";
        ft.minWeightClass = EntityWeightClass.WEIGHT_MEDIUM;
        ft.mainCriteria = ms -> ms.getTotalArmor() >= 40;
        ft.mainDescription = "Armor 40+";
        ft.otherCriteria.add(new PercentConstraint(0.5,
                ms -> ms.getEquipmentNames().stream().map(EquipmentType::get)
                    .anyMatch(eq -> eq instanceof ACWeapon
                            || eq instanceof LBXACWeapon
                            || eq instanceof UACWeapon
                            || eq instanceof SRMWeapon
                            || eq instanceof LRMWeapon),
                "AC, SRM, or LRM"));
        ft.reportMetrics.put("AC/SRM/LRM", ms -> ft.otherCriteria.get(0).criterion.test(ms));
        allFormationTypes.put(ft.name, ft);
    }
    
    private static void createFastAssaultLance() {
        FormationType ft = new FormationType("Fast Assault", "Assault");
        ft.allowedUnitTypes = FLAG_GROUND_NO_LIGHT;
        ft.minWeightClass = EntityWeightClass.WEIGHT_MEDIUM;
        ft.mainCriteria = ms -> ms.getTotalArmor() >= 135
                && (ms.getWalkMp() >= 5 || ms.getJumpMp() > 0);
        ft.mainDescription = "Walk 5+ or Jump 1+";
        ft.otherCriteria.add(new PercentConstraint(0.75,
                ms -> getDamageAtRange(ms, 7) >= 25,
                "Damage 25+ at range 7"));
        ft.otherCriteria.add(new CountConstraint(3,
                ms -> ms.getWeightClass() >= EntityWeightClass.WEIGHT_HEAVY,
                "Heavy+"));
        //FIXME: The actual requirement is one juggernaut or two snipers; there needs to be
        // a way to combine constraints with ||.
        ft.otherCriteria.add(new CountConstraint(2,
                ms -> EnumSet.of(UnitRole.JUGGERNAUT, UnitRole.SNIPER).contains(UnitRoleHandler.getRoleFor(ms)),
                "Juggernaut or Sniper"));
        ft.reportMetrics.put("Damage @ 7", ms -> getDamageAtRange(ms, 7));
        allFormationTypes.put(ft.name, ft);
    }
    
    private static void createHunterLance() {
        FormationType ft = new FormationType("Hunter", "Assault");
        ft.allowedUnitTypes = FLAG_GROUND;
        ft.idealRole = UnitRole.AMBUSHER;
        ft.otherCriteria.add(new PercentConstraint(0.5,
                ms -> EnumSet.of(UnitRole.JUGGERNAUT, UnitRole.AMBUSHER).contains(UnitRoleHandler.getRoleFor(ms)),
                "Juggernaut or Ambusher"));
        allFormationTypes.put(ft.name, ft);
    }
        
    private static void createBattleLance() {
        FormationType ft = new FormationType("Battle");
        ft.allowedUnitTypes = FLAG_GROUND_NO_LIGHT;
        ft.idealRole = UnitRole.BRAWLER;
        ft.otherCriteria.add(new PercentConstraint(0.5,
                ms -> ms.getWeightClass() >= EntityWeightClass.WEIGHT_HEAVY,
                "Heavy+"));
        ft.otherCriteria.add(new CountConstraint(3,
                ms -> EnumSet.of(UnitRole.BRAWLER, UnitRole.SNIPER, UnitRole.SKIRMISHER)
                    .contains(UnitRoleHandler.getRoleFor(ms)),
                    "Brawler, Sniper, Skirmisher"));
        ft.groupingCriteria = new GroupingConstraint(FLAG_VEHICLE, 2, 2,
                ms -> ms.getWeightClass() == EntityWeightClass.WEIGHT_HEAVY,
                FormationType::checkUnitMatch,
                "Same model, Heavy");
        allFormationTypes.put(ft.name, ft);
    }

    private static void createLightBattleLance() {
        FormationType ft = new FormationType("Light Battle", "Battle");
        ft.allowedUnitTypes = FLAG_GROUND;
        ft.maxWeightClass = EntityWeightClass.WEIGHT_HEAVY;
        ft.otherCriteria.add(new PercentConstraint(0.75,
                ms -> ms.getWeightClass() == EntityWeightClass.WEIGHT_LIGHT,
                "Light"));
        ft.otherCriteria.add(new CountConstraint(1,
                ms -> UnitRoleHandler.getRoleFor(ms).equals(UnitRole.SCOUT),
                "Scout"));
        ft.groupingCriteria = new GroupingConstraint(FLAG_VEHICLE, 2, 2,
                ms -> ms.getWeightClass() == EntityWeightClass.WEIGHT_LIGHT,
                FormationType::checkUnitMatch, "Same model, Light");
        allFormationTypes.put(ft.name, ft);
    }
    
    private static void createMediumBattleLance() {
        FormationType ft = new FormationType("Medium Battle", "Battle");
        ft.allowedUnitTypes = FLAG_GROUND_NO_LIGHT;
        ft.maxWeightClass = EntityWeightClass.WEIGHT_HEAVY;
        ft.otherCriteria.add(new PercentConstraint(0.5,
                ms -> ms.getWeightClass() == EntityWeightClass.WEIGHT_MEDIUM,
                "Medium"));
        ft.groupingCriteria = new GroupingConstraint(FLAG_VEHICLE, 2, 2,
                ms -> ms.getWeightClass() == EntityWeightClass.WEIGHT_MEDIUM,
                FormationType::checkUnitMatch, "Same model, Medium");
        allFormationTypes.put(ft.name, ft);
    }
    
    private static void createHeavyBattleLance() {
        FormationType ft = new FormationType("Heavy Battle", "Battle");
        ft.allowedUnitTypes = FLAG_GROUND_NO_LIGHT;
        ft.minWeightClass = EntityWeightClass.WEIGHT_MEDIUM;
        ft.otherCriteria.add(new PercentConstraint(0.5,
                ms -> ms.getWeightClass() >= EntityWeightClass.WEIGHT_HEAVY,
                "Heavy+"));
        ft.groupingCriteria = new GroupingConstraint(FLAG_VEHICLE, 2, 2,
                ms -> ms.getWeightClass() >= EntityWeightClass.WEIGHT_HEAVY,
                FormationType::checkUnitMatch, "Same model, Heavy+");
        allFormationTypes.put(ft.name, ft);
    }
    
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
                ms -> ms.getEquipmentNames().stream().map(EquipmentType::get)
                    .anyMatch(eq -> eq instanceof ACWeapon
                            || eq instanceof LBXACWeapon
                            || eq instanceof UACWeapon), //UAC includes RAC
                "AC weapon"));
        ft.reportMetrics.put("AC", ms -> ft.otherCriteria.get(1).criterion.test(ms));
        allFormationTypes.put(ft.name, ft);
    }
    
    private static void createBerserkerLance() {
        FormationType ft = new FormationType("Berserker/Close", "Battle");
        ft.allowedUnitTypes = FLAG_MEK | FLAG_PROTOMEK;
        ft.idealRole = UnitRole.BRAWLER;
        ft.otherCriteria.add(new PercentConstraint(0.5,
                ms -> ms.getWeightClass() >= EntityWeightClass.WEIGHT_HEAVY,
                "Heavy+"));
        ft.otherCriteria.add(new CountConstraint(3,
                ms -> EnumSet.of(UnitRole.BRAWLER, UnitRole.SNIPER, UnitRole.SKIRMISHER)
                    .contains(UnitRoleHandler.getRoleFor(ms)),
                "Brawler, Sniper, Skirmisher"));
        allFormationTypes.put(ft.name, ft);
    }
    
    private static void createCommandLance() {
        FormationType ft = new FormationType("Command", "Command");
        ft.allowedUnitTypes = FLAG_MEK | FLAG_PROTOMEK;
        ft.otherCriteria.add(new PercentConstraint(0.5,
                ms -> EnumSet.of(UnitRole.SNIPER, UnitRole.MISSILE_BOAT, UnitRole.SKIRMISHER,
                        UnitRole.JUGGERNAUT)
                    .contains(UnitRoleHandler.getRoleFor(ms)),
                "Sniper, Missile Boat, Skirmisher, Juggernaught"));
        ft.otherCriteria.add(new CountConstraint(1,
                ms -> EnumSet.of(UnitRole.BRAWLER, UnitRole.STRIKER, UnitRole.SCOUT)
                    .contains(UnitRoleHandler.getRoleFor(ms)),
                "Brawler, Striker, Scout"));
        allFormationTypes.put(ft.name, ft);
    }
    
    private static void createOrderLance() {
        FormationType ft = new FormationType("Order", "Command");
        ft.allowedUnitTypes = FLAG_GROUND;
        ft.exclusiveFaction = "DC";
        ft.groupingCriteria = new GroupingConstraint(FLAG_GROUND, 0, 1,
                ms -> true, FormationType::checkUnitMatch, "Same model");
        allFormationTypes.put(ft.name, ft);
    }
    
    private static void createVehicleCommandLance() {
        FormationType ft = new FormationType("Vehicle Command", "Command");
        ft.allowedUnitTypes = FLAG_TANK | FLAG_VTOL | FLAG_NAVAL;
        ft.otherCriteria.add(new CountConstraint(1,
                ms -> EnumSet.of(UnitRole.BRAWLER, UnitRole.STRIKER, UnitRole.SCOUT)
                    .contains(UnitRoleHandler.getRoleFor(ms)),
                "Brawler, Striker, Scout"));
        /* The description does not state how many pairs there need to be, but the reference to
         * "one of the pairs" implies there need to be at least two.
         */
        ft.groupingCriteria = new GroupingConstraint(FLAG_VEHICLE, 2, 2,
                ms -> EnumSet.of(UnitRole.SNIPER, UnitRole.MISSILE_BOAT, UnitRole.SKIRMISHER,
                        UnitRole.JUGGERNAUT)
                    .contains(UnitRoleHandler.getRoleFor(ms)),
                    (ms0, ms1) -> ms0.getName().equals(ms1.getName()),
                "Same model");
        allFormationTypes.put(ft.name, ft);
    }
    
    private static void createFireLance() {
        FormationType ft = new FormationType("Fire");
        ft.allowedUnitTypes = FLAG_GROUND;
        ft.idealRole = UnitRole.MISSILE_BOAT;
        ft.otherCriteria.add(new PercentConstraint(0.75,
                ms -> EnumSet.of(UnitRole.SNIPER, UnitRole.MISSILE_BOAT).contains(UnitRoleHandler.getRoleFor(ms)),
                "Sniper, Missile Boat"));
        allFormationTypes.put(ft.name, ft);
    }
    
    private static void createAntiAirLance() {
        FormationType ft = new FormationType("Anti-Air", "Fire");
        ft.allowedUnitTypes = FLAG_GROUND;
        ft.missionRoles.add(MissionRole.MIXED_ARTILLERY);
        ft.otherCriteria.add(new PercentConstraint(0.75,
                ms -> EnumSet.of(UnitRole.SNIPER, UnitRole.MISSILE_BOAT).contains(UnitRoleHandler.getRoleFor(ms)),
                "Sniper, Missile Boat"));
        ft.otherCriteria.add(new CountConstraint(2,
                // should indicate it has anti-aircraft targeting quirk without having to load all entities
                ms -> getMissionRoles(ms).contains(MissionRole.ANTI_AIRCRAFT) 
                || ms.getEquipmentNames().stream().map(EquipmentType::get)
                    .anyMatch(eq -> eq instanceof ACWeapon
                            || eq instanceof LBXACWeapon
                            || eq instanceof ArtilleryWeapon),
                "Standard AC, LBX, Artillery weapon, Anti-Air targeting quirk"));
        ft.reportMetrics.put("AC/LBX/Artillery/AA Quirk", ms -> ft.otherCriteria.get(1).criterion.test(ms));
        allFormationTypes.put(ft.name, ft);
    }
    
    private static void createArtilleryFireLance() {
        FormationType ft = new FormationType("Artillery Fire", "Fire");
        ft.allowedUnitTypes = FLAG_GROUND;
        ft.missionRoles.add(MissionRole.MIXED_ARTILLERY);
        ft.otherCriteria.add(new CountConstraint(2,
                ms -> ms.getEquipmentNames().stream().map(EquipmentType::get)
                    .anyMatch(eq -> eq instanceof ArtilleryWeapon),
                "Artillery"));
        ft.reportMetrics.put("Artillery", ms -> ft.otherCriteria.get(0).criterion.test(ms));
        allFormationTypes.put(ft.name, ft);
    }
    
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
    
    private static void createFireSupportLance() {
        FormationType ft = new FormationType("Fire Support", "Fire");
        ft.allowedUnitTypes = FLAG_GROUND;
        ft.otherCriteria.add(new CountConstraint(3, ms -> ms.getEquipmentNames().stream()
                .map(EquipmentType::get)
                .filter(eq -> eq instanceof WeaponType && eq.hasModes())
                .anyMatch(eq -> {
                    for (Enumeration<EquipmentMode> e = eq.getModes(); e.hasMoreElements();) {
                        if (e.nextElement().toString().equals("Indirect")) {
                            return true;
                        }
                    }
                    return false;
                }),
                "Indirect fire weapon"));
        ft.reportMetrics.put("Indirect", ms -> ft.otherCriteria.get(0).criterion.test(ms));
        allFormationTypes.put(ft.name, ft);
    }
    
    private static void createLightFireLance() {
        FormationType ft = new FormationType("Light Fire", "Fire");
        ft.allowedUnitTypes = FLAG_GROUND;
        ft.maxWeightClass = EntityWeightClass.WEIGHT_MEDIUM;
        allFormationTypes.put(ft.name, ft);
    }
    
    private static void createPursuitLance() {
        FormationType ft = new FormationType("Pursuit");
        ft.allowedUnitTypes = FLAG_GROUND;
        ft.maxWeightClass = EntityWeightClass.WEIGHT_MEDIUM;
        ft.otherCriteria.add(new PercentConstraint(0.75,
                ms -> ms.getWalkMp() >= 6,
                "Walk/Cruise 6+"));
        ft.otherCriteria.add(new CountConstraint(1,
                ms -> getSingleWeaponDamageAtRange(ms, 15) >= 5,
                "Weapon with damage 5+ at range 15"));
        ft.reportMetrics.put("Damage @ 15", ms -> getSingleWeaponDamageAtRange(ms, 15));
        allFormationTypes.put(ft.name, ft);        
    }

    private static void createProbeLance() {
        FormationType ft = new FormationType("Probe", "Pursuit");
        ft.allowedUnitTypes = FLAG_GROUND;
        ft.maxWeightClass = EntityWeightClass.WEIGHT_HEAVY;
        ft.mainCriteria = ms -> getDamageAtRange(ms, 9) >= 10;
        ft.mainDescription = "Damage 10+ at range 9";
        ft.otherCriteria.add(new PercentConstraint(0.75,
                ms -> ms.getWalkMp() >= 6,
                "Walk/Cruise 6+"));
        ft.reportMetrics.put("Damage @ 9", ms -> getDamageAtRange(ms, 9));
        allFormationTypes.put(ft.name, ft);        
    }

    private static void createSweepLance() {
        FormationType ft = new FormationType("Sweep", "Pursuit");
        ft.allowedUnitTypes = FLAG_GROUND;
        ft.maxWeightClass = EntityWeightClass.WEIGHT_MEDIUM;
        ft.mainCriteria = ms -> ms.getWalkMp() >= 5
                && getDamageAtRange(ms, 6) >= 10;
        ft.mainDescription = "Walk/Cruise 5+, Damage 10+ at range 6";
        ft.reportMetrics.put("Damage @ 6", ms -> getDamageAtRange(ms, 6));
        allFormationTypes.put(ft.name, ft);
    }
    
    private static void createReconLance() {
        FormationType ft = new FormationType("Recon");
        ft.allowedUnitTypes = FLAG_GROUND;
        ft.idealRole = UnitRole.SCOUT;
        ft.mainCriteria = ms -> ms.getWalkMp() >= 5;        
        ft.mainDescription = "Walk/Cruise 5+";
        ft.otherCriteria.add(new CountConstraint(2,
                ms -> EnumSet.of(UnitRole.SCOUT, UnitRole.STRIKER).contains(UnitRoleHandler.getRoleFor(ms)),
                "Scout, Striker"));
        allFormationTypes.put(ft.name, ft);
    }

    private static void createHeavyReconLance() {
        FormationType ft = new FormationType("Heavy Recon", "Recon");
        ft.allowedUnitTypes = FLAG_GROUND_NO_LIGHT;
        ft.mainCriteria = ms -> ms.getWalkMp() >= 4;        
        ft.mainDescription = "Walk/Cruise 4+";
        ft.otherCriteria.add(new CountConstraint(2,
                ms -> ms.getWalkMp() >= 5,
                "Walk/Cruise 5+"));
        ft.otherCriteria.add(new CountConstraint(2,
                ms -> UnitRoleHandler.getRoleFor(ms).equals(UnitRole.SCOUT),
                "Scout"));
        ft.otherCriteria.add(new CountConstraint(1,
                ms -> ms.getWeightClass() >= EntityWeightClass.WEIGHT_HEAVY,
                "Heavy+"));
        allFormationTypes.put(ft.name, ft);        
    }

    private static void createLightReconLance() {
        FormationType ft = new FormationType("Light Recon", "Recon");
        ft.allowedUnitTypes = FLAG_GROUND;
        ft.maxWeightClass = EntityWeightClass.WEIGHT_LIGHT;
        ft.mainCriteria = ms -> ms.getWalkMp() >= 6
                && UnitRoleHandler.getRoleFor(ms).equals(UnitRole.SCOUT);
        ft.mainDescription = "Walk/Cruise 6+, Scout";
        allFormationTypes.put(ft.name, ft);        
    }
    
    private static void createSecurityLance() {
        FormationType ft = new FormationType("Security");
        ft.allowedUnitTypes = FLAG_GROUND;
        ft.otherCriteria.add(new CountConstraint(1,
                ms -> EnumSet.of(UnitRole.SCOUT, UnitRole.STRIKER).contains(UnitRoleHandler.getRoleFor(ms)),
                "Scout, Striker"));
        ft.otherCriteria.add(new CountConstraint(1,
                ms -> EnumSet.of(UnitRole.SNIPER, UnitRole.MISSILE_BOAT).contains(UnitRoleHandler.getRoleFor(ms)),
                "Sniper, Missile Boat"));
        ft.otherCriteria.add(new MaxCountConstraint(1,
                ms -> ms.getWeightClass() >= EntityWeightClass.WEIGHT_ASSAULT,
                "Not assault"));
        allFormationTypes.put(ft.name, ft);        
    }

    private static void createStrikerCavalryLance() {
        FormationType ft = new FormationType("Striker/Cavalry");
        ft.allowedUnitTypes = FLAG_GROUND;
        ft.idealRole = UnitRole.STRIKER;
        ft.maxWeightClass = EntityWeightClass.WEIGHT_HEAVY;
        ft.mainCriteria = ms -> ms.getWalkMp() >= 5 || ms.getJumpMp() >= 4;
        ft.mainDescription = "Walk/Cruise 5+ or Jump 4+";
        ft.otherCriteria.add(new PercentConstraint(0.5,
                ms -> EnumSet.of(UnitRole.STRIKER, UnitRole.SKIRMISHER).contains(UnitRoleHandler.getRoleFor(ms)),
                "Striker, Skirmisher"));
        allFormationTypes.put(ft.name, ft);        
    }

    private static void createHammerLance() {
        FormationType ft = new FormationType("Hammer", "Striker/Cavalry");
        ft.allowedUnitTypes = FLAG_GROUND;
        ft.exclusiveFaction = "FWL";
        ft.idealRole = UnitRole.STRIKER;
        ft.mainCriteria = ms -> ms.getWalkMp() >= 5;
        ft.mainDescription = "Walk/Cruise 5+";
        allFormationTypes.put(ft.name, ft);        
    }

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
                ms -> EnumSet.of(UnitRole.STRIKER, UnitRole.SKIRMISHER).contains(UnitRoleHandler.getRoleFor(ms)),
                "Striker, Skirmisher"));
        ft.otherCriteria.add(new CountConstraint(1,
                ms -> getSingleWeaponDamageAtRange(ms, 18) >= 5,
                "Weapon with damage 5+ at range 18"));
        ft.reportMetrics.put("Damage @ 18", ms -> getSingleWeaponDamageAtRange(ms, 18));
        allFormationTypes.put(ft.name, ft);        
    }

    private static void createHordeLance() {
        FormationType ft = new FormationType("Horde", "Striker/Cavalry");
        ft.allowedUnitTypes = FLAG_GROUND;
        ft.maxWeightClass = EntityWeightClass.WEIGHT_LIGHT;
        ft.mainCriteria = ms -> getDamageAtRange(ms, 9) <= 10;
        ft.mainDescription = "Damage <= 10 at range 9";
        ft.reportMetrics.put("Damage @ 9", ms -> getDamageAtRange(ms, 9));
        allFormationTypes.put(ft.name, ft);        
    }

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
                ms -> EnumSet.of(UnitRole.STRIKER, UnitRole.SKIRMISHER).contains(UnitRoleHandler.getRoleFor(ms)),
                "Striker, Skirmisher"));
        ft.reportMetrics.put("Damage @ 18", ms -> getSingleWeaponDamageAtRange(ms, 18));
        allFormationTypes.put(ft.name, ft);        
    }

    private static void createRangerLance() {
        FormationType ft = new FormationType("Ranger", "Striker/Cavalry");
        ft.allowedUnitTypes = FLAG_GROUND;
        ft.maxWeightClass = EntityWeightClass.WEIGHT_HEAVY;
        allFormationTypes.put(ft.name, ft);        
    }

    private static void createUrbanLance() {
        FormationType ft = new FormationType("Urban");
        ft.allowedUnitTypes = FLAG_GROUND;
        ft.idealRole = UnitRole.AMBUSHER;
        ft.otherCriteria.add(new PercentConstraint(0.5,
                ms -> ms.getJumpMp() > 0
                    || ms.getUnitType().equals(UnitType.getTypeName(UnitType.INFANTRY))
                    || ms.getUnitType().equals(UnitType.getTypeName(UnitType.BATTLE_ARMOR)),
                "Jump 1+ or Infantry/BA"));
        ft.otherCriteria.add(new PercentConstraint(0.5,
                ms -> ms.getWalkMp() <= 4,
                "Walk/Cruise <= 4"));
        allFormationTypes.put(ft.name, ft);        
    }
    
    private static void createAerospaceSuperioritySquadron() {
        FormationType ft = new FormationType("Aerospace Superiority Squadron");
        ft.allowedUnitTypes = FLAG_FIGHTER;
        ft.otherCriteria.add(new PercentConstraint(0.51,
                ms -> EnumSet.of(UnitRole.INTERCEPTOR, UnitRole.FAST_DOGFIGHTER).contains(UnitRoleHandler.getRoleFor(ms)),
                "Interceptor/Fast Dogfighter"));
        ft.groupingCriteria = new GroupingConstraint(FLAG_FIGHTER, 2, 0,
                ms -> true,
                (ms0, ms1) -> ms0.getChassis().equals(ms1.getChassis()),
                "Same chassis");
        allFormationTypes.put(ft.name, ft);
    }

    private static void createEWSquadron() {
        FormationType ft = new FormationType("Electronic Warfare Squadron");
        ft.allowedUnitTypes = FLAG_FIGHTER;
        ft.otherCriteria.add(new PercentConstraint(0.51,
                ms -> ms.getEquipmentNames().stream().map(EquipmentType::get)
                .anyMatch(et -> et instanceof TAGWeapon ||  
                        (et instanceof MiscType &&
                            (et.hasFlag(MiscType.F_BAP) || et.hasFlag(MiscType.F_ECM)))),
                "Probe, ECM, TAG"));
        ft.groupingCriteria = new GroupingConstraint(FLAG_FIGHTER, 2, 0,
                ms -> true,
                (ms0, ms1) -> ms0.getChassis().equals(ms1.getChassis()),
                "Same chassis");
        ft.reportMetrics.put("Probe/ECM/TAG", ms -> ft.otherCriteria.get(0).criterion.test(ms));
        allFormationTypes.put(ft.name, ft);                
    }

    private static void createFireSupportSquadron() {
        FormationType ft = new FormationType("Fire Support Squadron");
        ft.allowedUnitTypes = FLAG_FIGHTER;
        ft.mainCriteria = ms -> EnumSet.of(UnitRole.FIRE_SUPPORT,
                UnitRole.DOGFIGHTER).contains(UnitRoleHandler.getRoleFor(ms));
        ft.mainDescription = "Fire Support, Dogfighter";
        ft.otherCriteria.add(new PercentConstraint(0.5,
                ms -> UnitRoleHandler.getRoleFor(ms).equals(UnitRole.FIRE_SUPPORT),
                "Fire Support"));
        ft.groupingCriteria = new GroupingConstraint(FLAG_FIGHTER, 2, 0,
                ms -> true,
                (ms0, ms1) -> ms0.getChassis().equals(ms1.getChassis()),
                "Same chassis");
        allFormationTypes.put(ft.name, ft);                
    }

    private static void createInterceptorSquadron() {
        FormationType ft = new FormationType("Interceptor Squadron");
        ft.allowedUnitTypes = FLAG_FIGHTER;
        ft.otherCriteria.add(new PercentConstraint(0.51,
                ms -> UnitRoleHandler.getRoleFor(ms).equals(UnitRole.INTERCEPTOR),
                "Interceptor"));
        ft.groupingCriteria = new GroupingConstraint(FLAG_FIGHTER, 2, 0,
                ms -> true,
                (ms0, ms1) -> ms0.getChassis().equals(ms1.getChassis()),
                "Same chassis");
        allFormationTypes.put(ft.name, ft);                
    }

    private static void createStrikeSquadron() {
        FormationType ft = new FormationType("Strike Squadron");
        ft.allowedUnitTypes = FLAG_FIGHTER;
        ft.otherCriteria.add(new PercentConstraint(0.51,
                ms -> EnumSet.of(UnitRole.ATTACK_FIGHTER,
                        UnitRole.DOGFIGHTER).contains(UnitRoleHandler.getRoleFor(ms)), "Attack, Dogfighter"));
        ft.groupingCriteria = new GroupingConstraint(FLAG_FIGHTER, 2, 0,
                ms -> true,
                (ms0, ms1) -> ms0.getChassis().equals(ms1.getChassis()),
                "Same chassis");
        allFormationTypes.put(ft.name, ft);                
    }

    private static void createTransportSquadron() {
        FormationType ft = new FormationType("Transport Squadron");
        ft.allowedUnitTypes = FLAG_FIGHTER | FLAG_SMALL_CRAFT | FLAG_DROPSHIP;
        ft.otherCriteria.add(new PercentConstraint(0.5,
                ms -> UnitRoleHandler.getRoleFor(ms).equals(UnitRole.TRANSPORT), "Transport"));
        ft.groupingCriteria = new GroupingConstraint(FLAG_FIGHTER, 2, Integer.MAX_VALUE,
                ms -> true,
                (ms0, ms1) -> ms0.getChassis().equals(ms1.getChassis()),
                "Same chassis");
        allFormationTypes.put(ft.name, ft);                
    }

    /**
     * Helper function used by some grouping constraints to compare units. Units are considered to match
     * if they are the same model, but omnis can match with different configurations. This is used primarily
     * for ground units; aerospace units match based on chassis.
     * 
     * @param ms0
     * @param ms1
     * @return    Whether the two units are considered the same for grouping considerations.
     */
    private static boolean checkUnitMatch(final MechSummary ms0, final MechSummary ms1) {
        final ModelRecord mRec = RATGenerator.getInstance().getModelRecord(ms0.getName());
        if (null != mRec && mRec.isOmni()) {
            return ms0.getChassis().equals(ms1.getChassis());
        } else {
            return ms0.getName().equals(ms1.getName());
        }
    }
    
    /**
     * base class for limitations on formation type 
     */
    public static abstract class Constraint {
        Predicate<MechSummary> criterion;
        String description;
        boolean pairedWithNext;
        boolean pairedWithPrevious;
        
        protected Constraint(Predicate<MechSummary> criterion, String description) {
            this.criterion = criterion;
            this.description = description;
        }
        
        public abstract int getMinimum(int unitSize);
        
        public String getDescription() {
            return description;
        }

        public boolean matches(MechSummary ms) {
            return criterion.test(ms);
        }
        
        /* In cases where a constraint has multiple possible fulfillments requiring different
         * numbers of units (e.g. Assault requires one juggernaut or two snipers), they must
         * be assigned to separate Constraints consecutively in the list and marked with the
         * appropriate flag.
         */
        public boolean isPairedWithPrevious() {
            return pairedWithPrevious;
        }
        
        public void setPairedWithPrevious(boolean paired) {
            pairedWithPrevious = paired;
        }

        public boolean isPairedWithNext() {
            return pairedWithNext;
        }
        
        public void setPairedWithNext(boolean paired) {
            pairedWithNext = paired;
        }
    }
    
    public static class CountConstraint extends Constraint {
        int count;
        
        public CountConstraint(int min, Predicate<MechSummary> criterion, String description) {
            super(criterion, description);
            count = min;
        }
        
        @Override
        public int getMinimum(int unitSize) {
            return count;
        }
    }
    
    private static class MaxCountConstraint extends CountConstraint {
        
        public MaxCountConstraint(int max, Predicate<MechSummary> criterion, String description) {
            super(max, criterion.negate(), description);
        }
        
        @Override
        public int getMinimum(int unitSize) {
            return unitSize - count;
        }
    }
    
    private static class PercentConstraint extends Constraint {
        double pct;
        
        public PercentConstraint(double min, Predicate<MechSummary> criterion, String description) {
            super(criterion, description);
            pct = min;
        }
        
        @Override
        public int getMinimum(int unitSize) {
            return (int) Math.ceil(pct * unitSize);
        }
    }
    
    /*
     * Permits additional constraints applied to a specific subset of the units.
     * Used to force pairs (or larger groups) of units that are identical or have the same base
     * chassis.
     */
    public static class GroupingConstraint extends Constraint {
        int unitTypes = FLAG_ALL;
        int groupSize = 2;
        int numGroups = 1;
        BiFunction<MechSummary,MechSummary,Boolean> groupConstraint;
        String description;
        
        public GroupingConstraint(Predicate<MechSummary> generalConstraint,
                BiFunction<MechSummary,MechSummary,Boolean> groupConstraint,
                String description) {
            super(generalConstraint, description);
            this.groupConstraint = groupConstraint;
        }
        
        public GroupingConstraint(int unitTypes,
                Predicate<MechSummary> generalConstraint,
                BiFunction<MechSummary,MechSummary,Boolean> groupConstraint,
                String description) {
            this(generalConstraint, groupConstraint, description);
            this.unitTypes = unitTypes;
        }
        
        public GroupingConstraint(int unitTypes, int groupSize, int numGroups,
                Predicate<MechSummary> generalConstraint,
                BiFunction<MechSummary,MechSummary,Boolean> groupConstraint,
                String description) {
            this(generalConstraint, groupConstraint, description);
            this.unitTypes = unitTypes;
            this.groupSize = groupSize;
            this.numGroups = numGroups;
        }
        
        public boolean appliesTo(int unitType) {
            return ((1 << unitType) & unitTypes) != 0;
        }

        public int getNumGroups() {
            return numGroups;
        }
        
        public int getGroupSize() {
            return groupSize;
        }
        
        @Override
        public boolean matches(MechSummary ms) {
            return criterion == null || criterion.test(ms);
        }
        
        public boolean matches(MechSummary ms1, MechSummary ms2) {
            return groupConstraint.apply(ms1,  ms2);
        }

        @Override
        public int getMinimum(int unitSize) {
            int gs = Math.min(groupSize, unitSize);
            int ng = numGroups;
            if (gs > 0) {
                ng = Math.min(ng, unitSize / gs);
            }
            return gs * ng;
        }
        
        public boolean hasGeneralCriteria() {
            return criterion != null;
        }
        
        public GroupingConstraint copy() {
            return new GroupingConstraint(this.unitTypes, this.groupSize, this.numGroups,
                this.criterion, this.groupConstraint, this.description);
        }
    }
}

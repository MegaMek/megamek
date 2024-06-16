/*
* MegaMek -
* Copyright (C) 2016 The MegaMek Team
*
* This program is free software; you can redistribute it and/or modify it under
* the terms of the GNU General Public License as published by the Free Software
* Foundation; either version 2 of the License, or (at your option) any later
* version.
*
* This program is distributed in the hope that it will be useful, but WITHOUT
* ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
* FOR A PARTICULAR PURPOSE. See the GNU General Public License for more
* details.
*/
package megamek.client.ratgenerator;

import megamek.client.ratgenerator.Ruleset.ProgressListener;
import megamek.common.*;
import megamek.common.loaders.EntityLoadingException;
import org.apache.logging.log4j.LogManager;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Describes the characteristics of a force. May be changed during generation.
 *
 * @author Neoancient
 */
public class ForceDescriptor {

    public static final int REINFORCED = 1;
    public static final int UNDERSTRENGTH = -1;

    public static final int EXP_GREEN = 0;
    public static final int EXP_REGULAR = 1;
    public static final int EXP_VETERAN = 2;

    public static final String[] ORDINALS = {
            "First", "Second", "Third", "Fourth", "Fifth",
            "Sixth", "Seventh", "Eighth", "Ninth", "Tenth"
    };

    public static final String[] PHONETIC = {
            "Alpha", "Bravo", "Charlie", "Delta", "Echo",
            "Foxtrot", "Golf", "Hotel", "India", "Juliett",
            "Kilo", "Lima", "Mike", "November", "Oscar",
            "Papa", "Quebec", "Romeo", "Sierra", "Tango",
            "Uniform", "Victor", "Whiskey", "X-ray", "Yankee",
            "Zulu"
    };

    public static final String[] GREEK = {
            "Alpha", "Beta", "Gamma", "Delta", "Epsilon",
            "Zeta", "Eta", "Theta", "Iota", "Kappa",
            "Lambda", "Mu", "Nu", "Xi", "Omicron", "Pi",
            "Rho", "Sigma", "Tau", "Upsilon", "Phi",
            "Chi", "Psi", "Omega"
    };

    public static final String[] LATIN = {
            "Prima", "Secunda", "Tertia", "Quarta", "Quinta",
            "Sexta", "Septima", "Octava", "Nona", "Decima"
    };

    public static final String[] ROMAN = {
            "I", "II", "III", "IV", "V", "VI", "VII", "VIII", "IX", "X",
            "XI", "XII", "XIII", "XIV", "XV", "XVI", "XVII", "XVIII", "XIX", "XX"
    };

    private int index;
    private String name;
    private String faction;
    private Integer year;
    private Integer eschelon;
    private int sizeMod;
    private boolean augmented;
    private Integer weightClass;
    private Integer unitType;
    private HashSet<EntityMovementMode> movementModes;
    private HashSet<MissionRole> roles;
    private String rating;
    private Integer experience;
    private Integer rankSystem;
    private Integer coRank;
    private HashSet<String> models;
    private HashSet<String> chassis;
    private HashSet<String> variants;
    private CrewDescriptor co;
    private CrewDescriptor xo;
    private String camo;

    private HashSet<String> flags;

    private FormationType formationType;
    private String generationRule;
    private boolean topLevel;
    private boolean element;
    private int positionIndex;
    private int nameIndex;
    private String fluffName;
    private Entity entity;
    private List<ValueNode> nameNodes;

    private boolean generateAttachments;
    private ForceDescriptor parent;
    private ArrayList<ForceDescriptor> subforces;
    private ArrayList<ForceDescriptor> attached;
    private double dropshipPct = 0.0;
    private double jumpshipPct = 0.0;
    private double cargo = 0.0;

    public ForceDescriptor() {
        faction = "IS";
        year = 3067;
        movementModes = new HashSet<>();
        roles = new HashSet<>();
        formationType = null;
        experience = EXP_REGULAR;
        models = new HashSet<>();
        chassis = new HashSet<>();
        variants = new HashSet<>();
        parent = null;
        subforces = new ArrayList<>();
        attached = new ArrayList<>();
        flags = new HashSet<>();
        topLevel = false;
        element = false;
        positionIndex = -1;
        nameIndex = -1;
        fluffName = null;
    }

    /**
     * Checks whether the chassis matches the unit type for this node of the force tree.
     * If a list of acceptable chassis has been assigned, checks whether the chassis is in the list.
     * unit type.
     *
     * @param cRec A unit chassis record
     * @return     Whether the chassis is of the correct unit type and is on the list of acceptable
     *             chassis if it exists.
     *
     */
    public boolean matches(ChassisRecord cRec) {
        if (cRec.getUnitType() != unitType) {
            return false;
        } else if (!chassis.isEmpty() && !chassis.contains(cRec.getChassis())) {
            return false;
        } else {
            return true;
        }
    }

    /**
     * If a list of acceptable chassis, models, or variants has been assigned, checks whether
     * the model is among them.
     *
     * @param mRec A unit model record
     * @return     Whether the model is on the list of acceptable chassis, variants, or models.
     */
    public boolean matches(ModelRecord mRec) {
        if (!chassis.isEmpty() && !chassis.contains(mRec.getChassis())) {
            return false;
        } else if (!variants.isEmpty() && !variants.contains(mRec.getModel())) {
            return false;
        } else if (!models.isEmpty() && !models.contains(mRec.getKey())) {
            return false;
        } else {
            return true;
        }
    }

    /**
     * Goes through the force tree structure and generates units for all leaf nodes.
     */
    public void generateUnits(ProgressListener l, double progress) {
        // If the parent node has a chassis or model assigned, it carries through to the children.
        if (null != parent) {
            chassis.addAll(parent.getChassis());
            models.addAll(parent.getModels());
        }
        // First see if a formation has been assigned. If unable to fulfill the formation requirements, generate using default parameters.
        if (subforces.isEmpty()) {
            ModelRecord mr = generate();
            if (null == mr && !models.isEmpty()) {
                mr = RATGenerator.getInstance().getModelRecord(getModelName());
            }
            if (null != mr) {
                setUnit(mr);
            } else {
                LogManager.getLogger().error("Could not generate unit");
            }
        } else {
            if (null != formationType) {
                //Simple leaf node (Lance, Star, etc.
                if (null != generationRule) {
                    //In cases like Novas and air lances the formation rules only apply to some of the units
                    if (!generateAndAssignFormation(subforces, generationRule.equals("chassis"), 0)) {
                        generateLance(subforces);
                        formationType = null;
                    }
                } else {
                    //If group generation is not set, then either this is a compound formation (e.g. squadron,
                    // aero/vehicle Point) or we are generating uniform subforces such as companies in SL line units
                    try {
                        Map<String,List<ForceDescriptor>> byGenRule = subforces.stream()
                                .collect(Collectors.groupingBy(ForceDescriptor::getGenerationRule));
                        if (byGenRule.containsKey("group")) {
                            if (!generateAndAssignFormation(byGenRule.get("group").stream()
                                    .map(ForceDescriptor::getSubforces).flatMap(Collection::stream)
                                    .collect(Collectors.toList()), false, byGenRule.get("group").size())) {
                                formationType = null;
                            }
                        } else if (byGenRule.containsKey("model")) {
                            generateAndAssignFormation(byGenRule.get("model"), false, 0);
                        } else if (byGenRule.containsKey("chassis")) {
                            generateAndAssignFormation(byGenRule.get("chassis"), true, 0);
                        }
                    } catch (NullPointerException ex) {
                        LogManager.getLogger().error("Found null generation rule in force node with formation set.");
                    }
                }
            } else {
                if (null != generationRule) {
                    switch (generationRule) {
                        case "chassis":
                            if (getChassis().isEmpty()) {
                                generate(generationRule);
                            }
                            break;
                        case "model":
                            if (getModels().isEmpty()) {
                                generate(generationRule);
                            }
                            break;
                        case "group":
                            generateLance(subforces);
                            break;
                    }
                }
            }
        }
        int count = subforces.size() + attached.size();
        subforces.forEach(fd -> fd.generateUnits(l, progress / count));
        attached.forEach(fd -> fd.generateUnits(l, progress / count));
        if (count == 0 && null != l) {
            l.updateProgress(progress, "Populating force tree");
        }
    }

    /**
     * Sorts out all subforce nodes eligible for the <code>FormationType</code> and attempts to generate
     * a formation based on their parameters. If the formation is successfully generated, it is distributed
     * to the subforces in the order provided. For leaf node, the unit is set. For non-final nodes,
     * the unit is added to either the model or chassis list depending on the provided grouping rule.
     * Any subforces that are not eligible for the formation are then generated.
     *
     * @param subs             The subforces to generate unit for. These need not be direct children of
     *                         <code>this</code>.
     * @param chassis          If true, any non-final subforce node will have the generated unit added to the
     *                         chassis list instead of the model list.
     * @param numGroups        The number of groups to pass on to formation generation; used to override
     *                         standard grouping constraints (e.g. matched pairs in fighter squadrons).
     * @return                 Whether the formation was successfully generated.
     */
    private boolean generateAndAssignFormation(List<ForceDescriptor> subs, boolean chassis, int numGroups) {
        Map<Boolean,List<ForceDescriptor>> eligibleSubs = subs.stream()
                .collect(Collectors.groupingBy(fd -> null != fd.getUnitType()
                && (formationType.isAllowedUnitType(fd.getUnitType()))
                || (augmented
                        && (fd.getUnitType() == UnitType.BATTLE_ARMOR)
                        || fd.getUnitType() == UnitType.INFANTRY)));
        if (eligibleSubs.containsKey(true)) {
            if (eligibleSubs.get(true).isEmpty()) {
                return false;
            } else {
                List<ModelRecord> list = null;
                if (augmented) {
                    list = generateNovaFormation(eligibleSubs.get(true), ModelRecord.NETWORK_NONE, numGroups);
                } else {
                    list = generateFormation(eligibleSubs.get(true), ModelRecord.NETWORK_NONE, numGroups);
                }
                if (list.isEmpty()) {
                    return false;
                } else {
                    for (int i = 0; i < list.size(); i++) {
                        //The formation requirements do not apply to the infantry part of a nova, and
                        //those units have already been generated by generateNovaFormation.
                        if (augmented
                                && (eligibleSubs.get(true).get(i).getUnitType() == UnitType.BATTLE_ARMOR
                                || eligibleSubs.get(true).get(i).getUnitType() == UnitType.INFANTRY)) {
                            continue;
                        }
                        if (eligibleSubs.get(true).get(i).getSubforces().isEmpty()) {
                            eligibleSubs.get(true).get(i).setUnit(list.get(i));
                        } else if (chassis) {
                            eligibleSubs.get(true).get(i).getChassis().add(list.get(i).getChassis());
                        } else {
                            eligibleSubs.get(true).get(i).getModels().add(list.get(i).getKey());
                        }
                    }
                }
            }
            if (eligibleSubs.containsKey(false)) {
                generateLance(eligibleSubs.get(false));
            }
        }
        return true;
    }

    /**
     * Translates <code>ForceDescriptor</code> list into parameters to pass to the formation builder.
     *
     * @param subs         A list of <ForceDescriptor</code> nodes.
     * @param networkMask  The type of C3 network that should be used in generating the formation.
     * @param numGroups    Overrides the default value for formation grouping constraints (e.g. some
     *                     Capellan squadrons have two groups of three instead of the standard three groups
     *                     of two).
     * @return             The list of units that make up the formation, or an empty list if a formation
     *                     could not be generated with the given parameters.
     */
    private List<ModelRecord> generateFormation (List<ForceDescriptor> subs, int networkMask, int numGroups) {
        Map<UnitTable.Parameters, Integer> paramCount = new HashMap<>();
        for (ForceDescriptor sub : subs) {
            paramCount.merge(new UnitTable.Parameters(sub.getFactionRec(),
                    sub.getUnitType(), sub.getYear(), sub.ratGeneratorRating(), null, networkMask,
                    sub.getMovementModes(), sub.getRoles(), 0, sub.getFactionRec()), 1, Integer::sum);
        }

        List<UnitTable.Parameters> params = new ArrayList<>();
        List<Integer> numUnits = new ArrayList<>();
        for (Map.Entry<UnitTable.Parameters, Integer> e : paramCount.entrySet()) {
            params.add(e.getKey());
            numUnits.add(e.getValue());
        }
        // Check for amount of C3 equipment generated and if certain thresholds are met regenerate the unit
        // with a valid network.
        List<MechSummary> unitList = formationType.generateFormation(params, numUnits, networkMask, false, 0, numGroups);
        if (networkMask == ModelRecord.NETWORK_NONE) {
            int c3m = 0;
            int c3s = 0;
            int c3i = 0;
            int nova = 0;
            for (MechSummary ms : unitList) {
                ModelRecord mRec = RATGenerator.getInstance().getModelRecord(ms.getName());
                int mask = mRec == null? ModelRecord.NETWORK_NONE : mRec.getNetworkMask();

                if ((mask & ModelRecord.NETWORK_C3_MASTER) != 0) {
                    c3m++;
                }
                if ((mask & ModelRecord.NETWORK_C3_SLAVE) != 0) {
                    c3s++;
                }
                if ((mask & ModelRecord.NETWORK_C3I) != 0) {
                    c3i++;
                }
                if ((mask & ModelRecord.NETWORK_NOVA) != 0) {
                    nova++;
                }
            }
            // Any lance with a C3 master should have three slave units (or the remainder of the unit, if smaller)
            if (c3m > 0) {
                if ((c3m > 1) || (c3s < Math.max(3, unitList.size() - 1))) {
                    networkMask = ModelRecord.NETWORK_C3_MASTER;
                } else {
                    flags.add("c3");
                }
            } else {
                // If no master was generated, each slave unit gives a cumulative 1/3 chance of a network. Isolated
                // C3 slaves will be encountered, but not usually more than one or maybe two in a lance.
                if (c3s > Compute.randomInt(3)) {
                    networkMask = ModelRecord.NETWORK_C3_MASTER;
                } else if (c3i > Compute.randomInt(5)) {
                    // Each C3i gives a 1/5 chance of a full C3i network. A network is still useful if not full.
                    networkMask = ModelRecord.NETWORK_C3I;
                } else if (nova > 0) {
                    // The Nova CEWS is specialized enough to add a complete network if any is present.
                    networkMask = ModelRecord.NETWORK_NOVA;
                }
            }
            if (networkMask != ModelRecord.NETWORK_NONE) {
                List<MechSummary> netList = formationType.generateFormation(params, numUnits, networkMask, false, 0, numGroups);
                // Attempt to create the type of network indicated. If no unit can be created that fits the
                // criteria, fall back on the unit that was originally generated.
                if (!netList.isEmpty()) {
                    unitList = netList;
                    if (networkMask == ModelRecord.NETWORK_C3I) {
                        flags.add("c3i");
                    } else if (networkMask == ModelRecord.NETWORK_NOVA) {
                        flags.add("novacews");
                    } else {
                        flags.add("c3");
                    }
                }
            }
        }
        return unitList.stream().map(ms -> RATGenerator.getInstance().getModelRecord(ms.getName()))
                .collect(Collectors.toList());
    }

    /**
     * The Nova formation is a composite of base type and battle armor. The formationType only applies to the
     * base unit type (Mek, vehicle, fighter). The BA must be eligible for mechanized and have at least
     * one omni among the base units per BA squad/point, excepting any BA with magnetic clamps.
     *
     * Though the rules in Campaign Operations only cover BA novas, the Hell's Horses vehicle/conventional infantry
     * nova formations require an adapted version of the Nova formation rules to work.
     *
     * This method generates and assigns infantry elements and returns the list of base elements.
     *
     * @param subs         A list of <ForceDescriptor</code> nodes.
     * @param networkMask  The type of C3 network that should be used in generating the formation.
     * @param numGroups    Overrides the default value for formation grouping constraints (e.g. some
     *                     Capellan squadrons have two groups of three instead of the standard three groups
     *                     of two).
     * @return             The list of units that make up the base formation, or an empty list if a formation
     *                     could not be generated with the given parameters.
     */
    private List<ModelRecord> generateNovaFormation(List<ForceDescriptor> subs, int networkMask, int numGroups) {
        //Split base and infantry units
        List<ForceDescriptor> baseSubs = new ArrayList<>();
        List<ForceDescriptor> baSubs = new ArrayList<>();
        List<ForceDescriptor> infSubs = new ArrayList<>();
        for (ForceDescriptor sub : subs) {
            if (sub.getUnitType() == UnitType.BATTLE_ARMOR) {
                baSubs.add(sub);
            } else if (sub.getUnitType() == UnitType.INFANTRY) {
                infSubs.add(sub);
            } else {
                baseSubs.add(sub);
            }
        }
        // If there is any conventional infantry we'll generate it first, then assign the APC role
        // to as many vehicles (if any) in the base units as we have foot infantry. Any remaining vehicles
        // will get the infantry support role.
        if (!infSubs.isEmpty()) {
            generateLance(infSubs);
            int footCount = (int) infSubs.stream().filter(fd -> fd.getMovementModes()
                    .contains(EntityMovementMode.INF_LEG)).count();
            for (int i = 0; i < baseSubs.size(); i++) {
                if (baseSubs.get(i).getUnitType() == UnitType.TANK
                        || baseSubs.get(i).getUnitType() == UnitType.VTOL) {
                    if (footCount > 0) {
                        baseSubs.get(i).getRoles().add(MissionRole.APC);
                        footCount--;
                    } else {
                        baseSubs.get(i).getRoles().add(MissionRole.INF_SUPPORT);
                    }
                }
            }
        }
        //Generate the base units according to the formation type.
        List<ModelRecord> baseUnitList = null;
        if (!baseSubs.isEmpty()) {
            baseUnitList = generateFormation(baseSubs, networkMask, numGroups);
        }
        if (null == baseUnitList) {
            generateLance(baseSubs);
            baseUnitList = baseSubs.stream().map(ForceDescriptor::getModelName)
                    .map(m -> RATGenerator.getInstance().getModelRecord(m))
                    .filter(Objects::nonNull).collect(Collectors.toList());
        }

        //Any BA in exceess of the number of omni base units will require mag clamps, up to the number of base units.
        int magReq = Math.min((int) (baSubs.size() - baseUnitList.stream().filter(AbstractUnitRecord::isOmni).count()),
                baSubs.size());
        for (int i = 0; i < baSubs.size(); i++) {
            if (i < magReq) {
                baSubs.get(i).getRoles().add(MissionRole.MAG_CLAMP);
            } else {
                baSubs.get(i).getRoles().add(MissionRole.MECHANIZED_BA);
            }
        }
        generateLance(baSubs);

        return baseUnitList;
    }

    /**
     * Generates a lance or other end-level unit (star, level ii) by individual units rather than
     * as an entire formation. Some unit cohesion is attempted for certain unit types, such as building
     * vehicle lances out of the same model or pairing fighter chassis. The equipment rating has an
     * effect on unit cohesion, such that lower rated units are more likely to have mismatched equipment.
     *
     * @param subs The subforces that describe the individual elements of the lance
     */
    public void generateLance(List<ForceDescriptor> subs) {
        if (subs.isEmpty()) {
            return;
        }
        ModelRecord unit;
        if (!chassis.isEmpty() || !models.isEmpty()) {
            for (ForceDescriptor sub : subs) {
                unit = sub.generate();
                if (unit != null) {
                    sub.setUnit(unit);
                }
            }
            return;
        }

        /* This method can be used to generate pieces of a combined arms
         * unit, so we need to get the unit type from one of the subforces
         * rather than the current. */

        Integer ut = subs.get(0).getUnitType();

        boolean useWeights = useWeightClass(ut);
        ArrayList<Integer> weights = new ArrayList<>();
        if (useWeights) {
            for (ForceDescriptor sub : subs) {
                weights.add(sub.getWeightClass());
            }
        } else {
            weights.add(-1);
            weights.add(0);
            weights.add(1);
            weights.add(2);
            weights.add(3);
            weights.add(4);
            weights.add(5);
            weights.add(null);
        }
        int ratingLevel = getRatingLevel();
        int totalLevels = 5;
        /*
         * Using the rating level relative to the total number of levels throws the results
         * off for ComStar, which should behave as A-B out of A-F rather than A-B out of A-B.
         *
         *  int totalLevels = RATGenerator.getInstance().getFaction(faction.split(",")[0]).getRatingLevels().size();
         */
        int target = 12 - ratingLevel;
        if (ratingLevel < 0) {
            target = 10;
        }
        int era = RATGenerator.getInstance().eraForYear(getYear());
        AvailabilityRating av;
        ModelRecord baseModel = null;
        /* Generate base model using weight class of entire formation */
        if (ut != null) {
            if (!(ut == UnitType.MEK || (ut == UnitType.AEROSPACEFIGHTER && subs.size() > 3))) {
                baseModel = subs.get(0).generate();
            }
            if (ut == UnitType.AEROSPACEFIGHTER || ut == UnitType.CONV_FIGHTER || ut == UnitType.AERO ) {
                target -= 3;
            }
            if (roles.contains(MissionRole.ARTILLERY)) {
                if (baseModel != null &&
                        baseModel.getRoles().contains(MissionRole.MISSILE_ARTILLERY)) {
                    roles.remove(MissionRole.ARTILLERY);
                    roles.add(MissionRole.MISSILE_ARTILLERY);
                } else {
                    target -= 4;
                }
            }
        }

        for (ForceDescriptor sub : subs) {
            boolean foundUnit = false;
            if (baseModel == null || (ut != null && !ut.equals(sub.getUnitType()))) {
                unit = sub.generate();
                if (unit != null) {
                    sub.setUnit(unit);
                    baseModel = unit;
                    if (useWeights) {
                        weights.remove(sub.getWeightClass());
                    }
                    foundUnit = true;
                }
            } else {
                for (String model : baseModel.getDeployedWith()) {
                    String chassisKey = model + "[" + ut + "]";
                    ChassisRecord cRec = RATGenerator.getInstance().getChassisRecord(chassisKey);
                    if (cRec == null) {
                        cRec = RATGenerator.getInstance().getChassisRecord(chassisKey + "Omni");
                    }
                    if (cRec != null) {
                        av = RATGenerator.getInstance().
                                findChassisAvailabilityRecord(era, model, faction, getYear());
                        if (av == null) {
                            for (String alt : RATGenerator.getInstance().getFaction(faction).getParentFactions()) {
                                av = RATGenerator.getInstance().findChassisAvailabilityRecord(era, model, alt, getYear());
                                if (av != null) {
                                    break;
                                }
                            }
                        }
                        if (Compute.d6(2) >= target - ((av == null) ? 0 : av.adjustForRating(ratingLevel, totalLevels))) {
                            sub.getChassis().clear();
                            sub.getChassis().add(model);
                            int oldWt = sub.getWeightClass();
                            sub.setWeightClass(-1);
                            unit = sub.generate();
                            if (unit != null && weights.contains(unit.getWeightClass())) {
                                sub.setUnit(unit);
                                if (useWeights) {
                                    weights.remove(sub.getWeightClass());
                                }
                                foundUnit = true;
                                break;
                            } else {
                                sub.setWeightClass(oldWt);
                            }
                        }
                    } else {
                        ModelRecord mRec = RATGenerator.getInstance().getModelRecord(model);
                        if (mRec != null && weights.contains(mRec.getWeightClass())
                                && RATGenerator.getInstance().findModelAvailabilityRecord(era, model, faction) != null) {
                            av = RATGenerator.getInstance().findChassisAvailabilityRecord(era, mRec.getChassisKey(), faction, getYear());
                            if (av == null) {
                                for (String alt : RATGenerator.getInstance().getFaction(faction).getParentFactions()) {
                                    av = RATGenerator.getInstance().findChassisAvailabilityRecord(era, mRec.getChassisKey(), alt, getYear());
                                    if (av != null) {
                                        break;
                                    }
                                }
                            }
                            if (Compute.d6(2) >= target - ((av == null) ? 0 : av.adjustForRating(ratingLevel, totalLevels))) {
                                sub.setUnit(mRec);
                                if (useWeights) {
                                    weights.remove((Object) mRec.getWeightClass());
                                }
                                foundUnit = true;
                                break;
                            }
                        }
                    }
                }
                if (!foundUnit && weights.contains(baseModel.getWeightClass())) {
                    av = RATGenerator.getInstance().findChassisAvailabilityRecord(era, baseModel.getChassisKey(), faction, getYear());
                    if (av == null) {
                        for (String alt : RATGenerator.getInstance().getFaction(faction).getParentFactions()) {
                            av = RATGenerator.getInstance().findChassisAvailabilityRecord(era, baseModel.getChassisKey(), alt, getYear());
                            if (av != null) {
                                break;
                            }
                        }
                    }
                    if (Compute.d6(2) >= target - ((av == null) ? 0 : av.adjustForRating(ratingLevel, totalLevels))) {
                        sub.getChassis().add(baseModel.getChassis());
                        sub.setWeightClass(-1);
                        unit = sub.generate();
                        if (unit != null) {
                            sub.setUnit(unit);
                            if (useWeights) {
                                weights.remove(sub.getWeightClass());
                            }
                            foundUnit = true;
                        }
                    } else if (ut == UnitType.TANK && Compute.d6(2) >= target - 6) {
                        if (useWeights) {
                            switch (baseModel.getMechSummary().getUnitSubType()) {
                                case "Hover":
                                    if (weights.contains(EntityWeightClass.WEIGHT_HEAVY)) {
                                        break;
                                    }
                                    /* fall through */
                                case "Wheeled":
                                    if (weights.contains(EntityWeightClass.WEIGHT_ASSAULT)) {
                                        break;
                                    }
                                    sub.getMovementModes().add(baseModel.getMovementMode());
                            }
                        }
                    } else if (ut == UnitType.INFANTRY) {
                        sub.getMovementModes().add(baseModel.getMovementMode());
                    }
                }
            }
            if (!foundUnit) {
                if (!weights.contains(sub.getWeightClass())) {
                    sub.setWeightClass(weights.get(0));
                }
                unit = sub.generate();
                if (unit == null) {
                    sub.getMovementModes().clear();
                    unit = sub.generate();
                }
                if (unit != null) {
                    sub.setUnit(unit);
                    if (useWeights) {
                        weights.remove(sub.getWeightClass());
                    }
                }
            }
            if (ut == null || ut == UnitType.MEK) {
                baseModel = null;
            }
        }
    }

    /**
     * Assigns a specific model to this node of the force tree. If this is a leaf node it will be flagged
     * as an element. If it has child nodes, they will all be made up of the same model unless changed by
     * a rule at a lower level of organization.
     *
     * @param unit The unit to assign to this node.
     */
    public void setUnit(ModelRecord unit) {
        chassis.clear();
        variants.clear();
        models.clear();
        models.add(unit.getKey());
        if (useWeightClass()) {
            weightClass = unit.getWeightClass();
        }
        if (subforces.isEmpty()) {
            element = true;
            movementModes.clear();
            movementModes.add(unit.getMovementMode());
            if (null == unitType) {
                unitType = unit.getUnitType();
            }
            if (((unitType == UnitType.MEK) || (unitType == UnitType.AEROSPACEFIGHTER)
                    || (unitType == UnitType.TANK))
                    && unit.isOmni()) {
                flags.add("omni");
            }
            if (unit.getRoles().contains(MissionRole.ARTILLERY)) {
                roles.add(MissionRole.ARTILLERY);
            }
            if (unit.getRoles().contains(MissionRole.MISSILE_ARTILLERY)) {
                roles.add(MissionRole.MISSILE_ARTILLERY);
            }
            if (unit.getRoles().contains(MissionRole.ANTI_MEK)) {
                roles.add(MissionRole.ANTI_MEK);
            }
            if (unit.getRoles().contains(MissionRole.FIELD_GUN)) {
                roles.add(MissionRole.FIELD_GUN);
            }
        }
    }

    public void generate(String level) {
        ModelRecord mRec = generate();
        if (mRec != null) {
            if (level.equals("chassis")) {
                getChassis().add(mRec.getChassis());
            } else {
                getModels().add(mRec.getKey());
            }
        }
    }

    public ModelRecord generate() {
        /* If the criteria cannot be matched, first try the next closest weight class,
         * then ignore mission role, then the next weight class, then ignore motive types,
         * then remaining weight classes.
         */
        final int[][] altWeights = {
                {1, 2, 3, 4, 5}, //UL
                {2, 0, 3, 4, 5}, //L
                {3, 1, 4, 0, 5}, //M
                {2, 4, 1, 5, 0}, //H
                {3, 2, 5, 1, 0}, //A
                {4, 3, 2, 1, 0}  //SH
        };
        /* Work with a copy */
        ForceDescriptor fd = createChild(index);
        fd.setEschelon(eschelon);
        fd.setCoRank(coRank);
        fd.getRoles().clear();
        fd.getRoles().addAll(roles.stream().filter(r -> r.fitsUnitType(unitType)).collect(Collectors.toList()));

        int wtIndex = (useWeightClass() && weightClass != null && weightClass != -1) ? 0 : 4;

        while (wtIndex < 5) {
            for (int roleStrictness = 3; roleStrictness >= 0; roleStrictness--) {
                List<Integer> wcs = new ArrayList<>();
                if (useWeightClass() && null != fd.getWeightClass() && fd.getWeightClass() >= 0) {
                    wcs.add(fd.getWeightClass());
                }
                String ratGenRating = ratGeneratorRating();
                UnitTable table = UnitTable.findTable(fd.getFactionRec(), fd.getUnitType(),
                        fd.getYear(), ratGenRating, wcs, ModelRecord.NETWORK_NONE,
                        fd.getMovementModes(), fd.getRoles(), roleStrictness);
                MechSummary ms = null;
                if (!fd.getModels().isEmpty()) {
                    ms = table.generateUnit(u -> fd.getModels().contains(u.getName()));
                } else if (!fd.getChassis().isEmpty()) {
                    ms = table.generateUnit(u -> fd.getChassis().contains(u.getChassis()));
                } else {
                    ms = table.generateUnit();
                }
                if (ms != null && RATGenerator.getInstance().getModelRecord(ms.getName()) != null) {
                    return RATGenerator.getInstance().getModelRecord(ms.getName());
                }

                if ((!useWeightClass() || wtIndex == 2) && !fd.getRoles().isEmpty()) {
                    fd.getRoles().clear();
                } else if ((!useWeightClass() || wtIndex == 1)
                        && !fd.getMovementModes().isEmpty()) {
                    fd.getMovementModes().clear();
                } else {
                    if (useWeightClass() && null != weightClass && weightClass != -1 && weightClass < altWeights.length && wtIndex < altWeights[weightClass].length) {
                        fd.setWeightClass(altWeights[weightClass][wtIndex]);
                    }
                    wtIndex++;
                }
            }
        }

        LogManager.getLogger().debug("Could not find unit for " + UnitType.getTypeDisplayableName(unitType));
        return null;
    }

    public void loadEntities(Ruleset.ProgressListener l, double progress) {
        if (element) {
            MechSummary ms = MechSummaryCache.getInstance().getMech(getModelName());
            if (ms != null) {
                try {
                    entity = new MechFileParser(ms.getSourceFile(), ms.getEntryName()).getEntity();
                    entity.setCrew(getCo().createCrew(entity.defaultCrewType()));
                    entity.setExternalIdAsString(UUID.randomUUID().toString());
                    String forceString = getForceString();
                    entity.setForceString(forceString);
                } catch (EntityLoadingException ex) {
                    LogManager.getLogger().error("Error loading " + ms.getName() + " from file " + ms.getSourceFile().getPath(), ex);
                }
            }
        }
        int count = subforces.size() + attached.size();
        subforces.forEach(fd -> fd.loadEntities(l, progress / count));
        attached.forEach(fd -> fd.loadEntities(l, progress / count));
        if (count == 0 && null != l) {
            l.updateProgress(progress, "Loading entities");
        }
    }

    /** Generates a force string for exporting these units to MUL / adding to the game. */
    private String getForceString() {
        var ancestors = new ArrayList<ForceDescriptor>();
        ForceDescriptor p = parent;
        while (p != null) {
            ancestors.add(p);
            p = p.parent;
        }

        StringBuilder result = new StringBuilder();
        int id = 0;
        for (int i = ancestors.size() - 1; i >= 0; i--) {
            ForceDescriptor ancestor = ancestors.get(i);
            id = 17 * id + ancestor.index + 1;
            result.append(ancestor.parseName()).append("|").append(id).append("||");
        }
        return result.toString();
    }

    public void assignCommanders() {
        subforces.forEach(ForceDescriptor::assignCommanders);

        Ruleset rules = Ruleset.findRuleset(this);
        CommanderNode coNode = null;
        CommanderNode xoNode = null;

        while (coNode == null && rules != null) {
            coNode = rules.getCoNode(this);
            xoNode = rules.getXoNode(this);
            if (coNode == null) {
                if (rules.getParent() == null) {
                    setCo(new CrewDescriptor(this));
                    return;
                }
                rules = Ruleset.findRuleset(rules.getParent());
            }
        }
        //If none is found, assign crew without assigning rank or title.
        if (coNode == null) {
            setCo(new CrewDescriptor(this));
            return;
        }

        if (!subforces.isEmpty()) {
            int coPos = 0;
            if (coNode != null) {
                coPos = (coNode.getPosition() == null) ? 1 : Math.min(coNode.getPosition(), 1);
            }
            int xoPos = 0;
            if (xoNode != null && (xoNode.getPosition() == null || xoNode.getPosition() > 0)) {
                xoPos = (xoNode.getPosition() == null) ? coPos + 1 : Math.max(coPos, xoNode.getPosition());
            }
            if (coPos + xoPos > 0) {
                ForceDescriptor[] forces = subforces.toArray(new ForceDescriptor[0]);
                Arrays.sort(forces, forceSorter);
                if (coPos != 0) {
                    ForceDescriptor coFound = null;
                    if (coNode.getUnitType() != null) {
                        for (ForceDescriptor fd : forces) {
                            if (fd.getUnitType() != null && fd.getUnitTypeName().equals(coNode.getUnitType())) {
                                coFound = fd;
                            }
                        }
                    }
                    if (coFound == null) {
                        coFound = forces[0];
                    }
                    setCo(coFound.getCo());
                    subforces.remove(coFound);
                    subforces.add(0, coFound);
                }
                if (xoPos != 0) {
                    /* If the XO is a field officer, the position is assigned to the first subforce that doesn't contain the CO
                     * (which is the first if the CO is not a field officer). If the CO and XO positions are the same, the
                     * XO is assigned to the same subforce as the CO, but the second subforce of that one.
                     */
                    ForceDescriptor xoFound = null;
                    ArrayList<ForceDescriptor> subforces = this.subforces;
                    if (coPos == xoPos) {
                        subforces = this.subforces.get(0).getSubforces();
                    }
                    if (subforces.size() > coPos) {
                        if (xoNode.getUnitType() != null) {
                            for (int i = coPos; i < subforces.size(); i++) {
                                if (subforces.get(i).getUnitType() != null &&
                                        (xoNode.getUnitType().equals(subforces.get(i).getUnitTypeName())
                                                || (xoNode.getUnitType().equals("other")
                                                        && !subforces.get(i).getUnitType().equals(co.getAssignment().getUnitType())))) {
                                    xoFound = subforces.get(i);
                                    break;
                                }
                            }
                        }
                        if (xoFound == null) {
                            xoFound = subforces.get(1);
                        }
                    }

                    if (xoFound != null) {
                        setXo(xoFound.getCo());
                        getXo().setRank(xoNode.getRank());
                    }
                }
            }
        }

        if (getCo() == null) {
            setCo(new CrewDescriptor(this));
        }
        getCo().setRank(coNode.getRank());
        getCo().setTitle(coNode.getTitle());

        if (xoNode != null) {
            if (getXo() == null) {
                setXo(new CrewDescriptor(this));
            }
            getXo().setRank(xoNode.getRank());
            getXo().setTitle(xoNode.getTitle());
        }
        if (!element && !subforces.isEmpty()) {
            movementModes.clear();
            boolean isOmni = true;
            boolean isArtillery = true;
            boolean isMissileArtillery = true;
            boolean isFieldGun = true;
            for (ForceDescriptor fd : subforces) {
                movementModes.addAll(fd.getMovementModes());
                if ((fd.getUnitType() == null ||
                        !((UnitType.MEK == fd.getUnitType()) || (UnitType.AEROSPACEFIGHTER == fd.getUnitType())
                                || (UnitType.TANK == fd.getUnitType()))) ||
                        !fd.getFlags().contains("omni")) {
                    isOmni = false;
                }
                if (!fd.getRoles().contains(MissionRole.MISSILE_ARTILLERY)) {
                    isMissileArtillery = false;
                }
                if (!fd.getRoles().contains(MissionRole.ARTILLERY)
                        && !fd.getRoles().contains(MissionRole.MISSILE_ARTILLERY)) {
                    isArtillery = false;
                }
                if (!fd.getRoles().contains(MissionRole.FIELD_GUN)) {
                    isFieldGun = false;
                }
            }
            if (isOmni) {
                flags.add("omni");
            }
            if (isArtillery) {
                roles.add(MissionRole.ARTILLERY);
            }
            if (isMissileArtillery) {
                roles.add(MissionRole.MISSILE_ARTILLERY);
            }
            if (isFieldGun) {
                roles.add(MissionRole.FIELD_GUN);
            }

            float wt = 0;
            int c = 0;
            for (ForceDescriptor sub : subforces) {
                if (sub.useWeightClass()) {
                    if (sub.getWeightClass() == null) {
                        LogManager.getLogger().error("Weight class == null for "
                                + sub.getUnitType() + " with " + sub.getSubforces().size() + " subforces.");
                    } else {
                        wt += sub.getWeightClass();
                        c++;
                    }
                }
            }
            if (c > 0) {
                weightClass = (int) (wt / c + 0.5);
            }
        }

        attached.forEach(ForceDescriptor::assignCommanders);
    }

    public void assignPositions() {
        int index = 0;
        HashMap<String,Integer> uniqueCount = new HashMap<>();
        for (int i = 0; i < subforces.size(); i++) {
            subforces.get(i).positionIndex = i + 1;
            if (subforces.get(i).name == null) {
                continue;
            }
            if (subforces.get(i).name.contains(":distinct}")) {
                if (uniqueCount.containsKey(subforces.get(i).name)) {
                    uniqueCount.put(subforces.get(i).name, uniqueCount.get(subforces.get(i).name) + 1);
                } else {
                    uniqueCount.put(subforces.get(i).name, 1);
                }
            } else if (subforces.get(i).name.matches(".*\\{[^:]*\\}.*")) {
                subforces.get(i).nameIndex = index++;
            }
        }
        HashMap<String,Integer> indexCount = new HashMap<>();
        for (ForceDescriptor sub : subforces) {
            if (uniqueCount.containsKey(sub.name)) {
                if (uniqueCount.get(sub.name) > 1) {
                    if (indexCount.containsKey(sub.name)) {
                        indexCount.put(sub.name, indexCount.get(sub.name) + 1);
                    } else {
                        indexCount.put(sub.name, 1);
                    }
                    sub.nameIndex = indexCount.get(sub.name) - 1;
                } else {
                    sub.nameIndex = -1;
                }
                sub.name = sub.name.replace(":distinct", "");
            }
            sub.assignPositions();
        }
        attached.forEach(ForceDescriptor::assignPositions);
    }

    private Comparator<? super ForceDescriptor> forceSorter = new Comparator<>() {
        /* Rank by difference in experience + difference in unit/eschelon weights */
        private int rank(ForceDescriptor fd) {
            int retVal = 0;
            if (fd.getWeightClass() != null) {
                retVal += fd.getWeightClass();
            }
            if (fd.getUnitType() != null) {
                switch (fd.getUnitType()) {
                    case UnitType.MEK:
                        retVal += 2;
                        break;
                    case UnitType.INFANTRY:
                        retVal -= 2;
                }
            }
            if (fd.getCo() != null) {
                retVal -= fd.getCo().getGunnery() + fd.getCo().getPiloting();
                ModelRecord mRec = RATGenerator.getInstance().getModelRecord(fd.getCo().getAssignment().getModelName());
                if (mRec != null) {
                    if (mRec.isSL()) {
                        retVal += 2;
                    }
                    if (mRec.isClan()) {
                        retVal += 5;
                    }
                }
            }
            return retVal;
        }

        @Override
        public int compare(ForceDescriptor arg0, ForceDescriptor arg1) {
            if (arg0.getRoles().contains(MissionRole.COMMAND) && !arg1.getRoles().contains(MissionRole.COMMAND)) {
                return -1;
            }
            if (!arg0.getRoles().contains(MissionRole.COMMAND) && arg1.getRoles().contains(MissionRole.COMMAND)) {
                return 1;
            }
            if (arg0.getRatingLevel() != arg1.getRatingLevel()) {
                return arg1.getRatingLevel() - arg0.getRatingLevel();
            }
            return rank(arg1) - rank(arg0);
        }
    };

    /**
     * Calculates transport needs of the unit and generates enough dropships and jumpships to carry
     * the indicated portion of the unit.
     */
    public ForceDescriptor assignTransport() {
        if ((getDropshipPct() <= 0) && (getJumpshipPct() <= 0) && (getCargo() <= 0)) {
            return null;
        }
        TransportCalculator tp = new TransportCalculator(this);
        List<MechSummary> dropships = tp.calcDropships(getDropshipPct());
        ForceDescriptor transports = createChild(subforces.size() + attached.size());
        transports.setUnitType(null);
        transports.setName("Transport");
        // TODO: put this in the faction files
        transports.setEschelon(3);
        transports.setCoRank(35);

        List<MechSummary> shipList = tp.calcJumpships(getJumpshipPct(), dropships.size());
        shipList.addAll(dropships);
        for (MechSummary ms : shipList) {
            ForceDescriptor sub = transports.createChild(transports.getSubforces().size());
            sub.setUnit(RATGenerator.getInstance().getModelRecord(ms.getName()));
            sub.setEschelon(1);
            sub.setCoRank(34);
            transports.addSubforce(sub);
        }
        transports.assignCommanders();
        transports.assignPositions();

        return transports;
    }

    /*
    public void assignBloodnames() {
        assignBloodnames(getFactionRec());
    }

    public void assignBloodnames(FactionRecord fRec) {
        if (fRec != null && fRec.isClan()) {
            for (ForceDescriptor fd : subforces) {
                fd.assignBloodnames();
            }
            for (ForceDescriptor fd : attached) {
                fd.assignBloodnames();
            }

            if (co != null && (element || !(co.getAssignment() != null && co.getAssignment().isElement()))) {
                co.assignBloodname();
            }
            if (xo != null && !(xo.getAssignment() != null && xo.getAssignment().isElement())) {
                xo.assignBloodname();
            }
        }
    }
     */

    public static int decodeWeightClass(String code) {
        switch (code) {
            case "UL":
                return EntityWeightClass.WEIGHT_ULTRA_LIGHT;
            case "L":
                return EntityWeightClass.WEIGHT_LIGHT;
            case "M":
                return EntityWeightClass.WEIGHT_MEDIUM;
            case "H":
                return EntityWeightClass.WEIGHT_HEAVY;
            case "A":
                return EntityWeightClass.WEIGHT_ASSAULT;
            case "SH":
            case "C":
                return EntityWeightClass.WEIGHT_COLOSSAL;
            default:
                return -1;
        }
    }

    public String getWeightClassCode() {
        final String[] codes = { "UL", "L", "M", "H", "A", "SH" };
        if (weightClass == null || weightClass == -1) {
            return "";
        }
        return codes[weightClass];
    }
    // AeroSpace Units
    public static final int WEIGHT_SMALL_CRAFT = 6; // Only a single weight class for Small Craft
    public static final int WEIGHT_SMALL_DROP = 7;
    public static final int WEIGHT_MEDIUM_DROP = 8;
    public static final int WEIGHT_LARGE_DROP = 9;
    public static final int WEIGHT_SMALL_WAR = 10;
    public static final int WEIGHT_LARGE_WAR = 11;

    // Support Vehicles
    public static final int WEIGHT_SMALL_SUPPORT = 12;
    public static final int WEIGHT_MEDIUM_SUPPORT = 13;
    public static final int WEIGHT_LARGE_SUPPORT = 14;

    public boolean useWeightClass() {
        return useWeightClass(unitType);
    }

    private boolean useWeightClass(Integer ut) {
        return ut != null &&
                !(roles.contains(MissionRole.ARTILLERY) || roles.contains(MissionRole.MISSILE_ARTILLERY)) &&
                (ut == UnitType.MEK ||
                ut == UnitType.AEROSPACEFIGHTER ||
                ut == UnitType.TANK ||
                ut == UnitType.BATTLE_ARMOR);
    }

    /**
     * Weight class can differ from the target once units are generated. Weight class is recalculated
     * based on actual units present and eschelon name is set.
     *
     * @return The weight class of this force node
     */
    public double recalcWeightClass() {
        double wc;
        if (!subforces.isEmpty()) {
            wc = subforces.stream().mapToDouble(ForceDescriptor::recalcWeightClass).sum() / subforces.size();
        } else if (null != weightClass && weightClass >= 0) {
            wc = weightClass;
        } else {
            wc = EntityWeightClass.WEIGHT_MEDIUM;
        }
        weightClass = (int) Math.round(wc);

        //Some names require knowing the weight class first.
        if (null != nameNodes) {
            for (ValueNode n : nameNodes) {
                if (n.matches(this)) {
                    setName(n.getContent());
                    break;
                }
            }
        }
        attached.forEach(ForceDescriptor::recalcWeightClass);

        return wc;
    }

    public ArrayList<Object> getAllChildren() {
        ArrayList<Object> retVal = new ArrayList<>();
        retVal.addAll(subforces);
        retVal.addAll(attached);
        return retVal;
    }

    public int getIndex() {
        return index;
    }

    public void setIndex(int index) {
        this.index = index;
    }

    public String getName() {
        return name;
    }

    public String parseName() {
        String retVal = name;
        if (name == null) {
            String eschName = Ruleset.findRuleset(this).getEschelonName(this);
            if (eschName == null) {
                return "";
            }
            retVal = "{ordinal} " + eschName;
        }
        if (getParent() != null && getParent().getNameIndex() >= 0) {
            retVal = retVal.replace("{ordinal:parent}", ORDINALS[getParent().getNameIndex()]);
            retVal = retVal.replace("{greek:parent}", GREEK[getParent().getNameIndex()]);
            retVal = retVal.replace("{phonetic:parent}", PHONETIC[getParent().getNameIndex()]);
            retVal = retVal.replace("{latin:parent}", LATIN[getParent().getNameIndex()]);
            retVal = retVal.replace("{roman:parent}", ROMAN[getParent().getNameIndex()]);
            retVal = retVal.replace("{cardinal:parent}", Integer.toString(getParent().getNameIndex() + 1));
            retVal = retVal.replace("{alpha:parent}", Character.toString((char) (getParent().getNameIndex() + 'A')));
        }
        if (getParent() != null && retVal.contains("{name:parent}")) {
            String parentName = getParent().getName().replaceAll(".*\\[", "").replaceAll("\\].*", "");
            retVal = retVal.replace("{name:parent}", parentName);
        }
        if (nameIndex < 0) {
            retVal = retVal.replaceAll("\\{.*?\\}\\s?", "");
        } else {
            retVal = retVal.replace("{ordinal}", ORDINALS[getNameIndex()]);
            retVal = retVal.replace("{greek}", GREEK[getNameIndex()]);
            retVal = retVal.replace("{phonetic}", PHONETIC[getNameIndex()]);
            retVal = retVal.replace("{latin}", LATIN[getNameIndex()]);
            retVal = retVal.replace("{roman}", ROMAN[getNameIndex()]);
            retVal = retVal.replace("{cardinal}", Integer.toString(getNameIndex() + 1));
            retVal = retVal.replace("{alpha}", Character.toString((char) (getNameIndex() + 'A')));
            if (retVal.contains("{formation}")) {
                if (null != formationType && null != formationType.getCategory()) {
                    retVal = retVal.replace("{formation}", formationType.getCategory()
                            .replace("Striker/Cavalry", "Striker").replaceAll(" Squadron", ""));
                } else {
                    retVal = retVal.replace("{formation} ", "");
                }
            }
        }
        retVal = retVal.replaceAll("\\{.*?\\}", "");
        retVal = retVal.replaceAll("[\\[\\]]", "").replaceAll("\\s+", " ");
        return retVal.trim();
    }

    public String getDescription() {
        StringBuilder retVal = new StringBuilder();
        if (unitType != null) {
            if (weightClass != null && weightClass >= 0) {
                retVal.append(EntityWeightClass.getClassName(weightClass)).append(" ");
            }

            if (roles.contains(MissionRole.ARTILLERY) || roles.contains(MissionRole.MISSILE_ARTILLERY)) {
                retVal.append(getUnitTypeName().equals("Infantry") ? "Field" : "Mobile").append(" ");
            } else {
                retVal.append(UnitType.getTypeName(unitType)).append(" ");
            }
        }

        if (roles.contains(MissionRole.RECON)) {
            retVal.append("Recon");
        } else if (roles.contains(MissionRole.FIRE_SUPPORT)) {
            retVal.append("Fire Support");
        } else if (roles.contains(MissionRole.ARTILLERY)) {
            retVal.append("Artillery");
        } else if (roles.contains(MissionRole.URBAN)) {
            retVal.append("Urban");
        }
        if (flags.contains("c3")) {
            retVal.append(" (C3)");
        } else if (flags.contains("c3i")) {
            retVal.append(" (C3I)");
        }
        Ruleset rules = Ruleset.findRuleset(this);
        String eschName = null;

        while (eschName == null && rules != null) {
            eschName = rules.getEschelonName(this);
            if (eschName == null) {
                if (rules.getParent() == null) {
                    rules = null;
                } else {
                    rules = Ruleset.findRuleset(rules.getParent());
                }
            }
        }

        if (eschName != null) {
            retVal.append(" ").append(eschName);
        }
        if (null != formationType) {
            retVal.append(" (").append(formationType.getName()).append(")");
        }
        return retVal.toString();
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getFaction() {
        return faction;
    }

    public void setFaction(String faction) {
        this.faction = faction;
    }

    public FactionRecord getFactionRec() {
        return RATGenerator.getInstance().getFaction(faction.split(",")[0]);
    }

    public Integer getYear() {
        return year;
    }

    public void setYear(Integer year) {
        this.year = year;
    }

    public Integer getEschelon() {
        return eschelon;
    }

    public String getEschelonCode() {
        String retVal = eschelon.toString();
        if (augmented) {
            retVal += "^";
        }
        return retVal;
    }

    public void setEschelon(Integer eschelon) {
        this.eschelon = eschelon;
    }

    public int getSizeMod() {
        return sizeMod;
    }

    public void setSizeMod(int sizeMod) {
        this.sizeMod = sizeMod;
    }

    public boolean isAugmented() {
        return augmented;
    }

    public void setAugmented(boolean augmented) {
        this.augmented = augmented;
    }

    public Integer getWeightClass() {
        return weightClass;
    }

    public void setWeightClass(Integer weightClass) {
        this.weightClass = weightClass;
    }

    public Integer getUnitType() {
        return unitType;
    }

    public void setUnitType(Integer unitType) {
        this.unitType = unitType;
    }

    public String getUnitTypeName() {
        if (null != unitType) {
            return UnitType.getTypeDisplayableName(unitType);
        }
        return "";
    }

    public HashSet<EntityMovementMode> getMovementModes() {
        return movementModes;
    }

    public String getRating() {
        return rating;
    }

    public void setRating(String rating) {
        this.rating = rating;
    }

    /**
     * Translates between the rating codes used by the force generator and those used by the
     * RAT Generator. The force generator uses abbreviations to make the formation rules more
     * concise.
     *
     * @return The RATGenerator rating code corresponding to the same index as the force generator
     *         rating code.
     */
    public String ratGeneratorRating() {
        FactionRecord fRec = getFactionRec();
        if ((null != fRec)
                && !fRec.getRatingLevels().contains(rating)
                && (getRatingLevel() >= 0)
                && !fRec.getRatingLevels().isEmpty()) {
            return fRec.getRatingLevels().get(Math.min(getRatingLevel(), fRec.getRatingLevels().size() - 1));
        }
        return rating;
    }

    public int getRatingLevel() {
        if (rating != null) {
            Ruleset rs = Ruleset.findRuleset(this);
            if (rs != null) {
                return rs.getRatingIndex(rating);
            }
        }
        return -1;
    }

    public FormationType getFormation() {
        return formationType;
    }

    public void setFormationType(FormationType ft) {
        formationType = ft;
    }

    public String getGenerationRule() {
        return generationRule;
    }

    public void setGenerationRule(String rule) {
        generationRule = rule;
    }

    public Set<MissionRole> getRoles() {
        return roles;
    }

    public Set<String> getModels() {
        return models;
    }

    public String getModelName() {
        if (models.size() != 1) {
            return "";
        }
        return models.iterator().next();
    }

    public Set<String> getChassis() {
        return chassis;
    }

    public Set<String> getVariants() {
        return variants;
    }

    public Integer getExperience() {
        return experience;
    }

    public void setExperience(Integer experience) {
        this.experience = experience;
    }

    public Integer getCoRank() {
        return coRank;
    }

    public void setCoRank(Integer coRank) {
        this.coRank = coRank;
    }

    public Integer getRankSystem() {
        return rankSystem;
    }

    public void setRankSystem(Integer rankSystem) {
        this.rankSystem = rankSystem;
    }

    public CrewDescriptor getCo() {
        return co;
    }

    public void setCo(CrewDescriptor co) {
        this.co = co;
    }

    public CrewDescriptor getXo() {
        return xo;
    }

    public void setXo(CrewDescriptor xo) {
        this.xo = xo;
    }

    public String getCamo() {
        return camo;
    }

    public void setCamo(String camo) {
        this.camo = camo;
    }

    /**
     * Because some eschelon names depend on knowing the actual weight class, we save a copy
     * of the possibilities for this node and defer selection until after the final weight class
     * determination.
     *
     * @param nameNodes
     */
    public void setNameNodes(List<ValueNode> nameNodes) {
        this.nameNodes = nameNodes;
    }

    public ForceDescriptor getParent() {
        return parent;
    }

    public void setParent(ForceDescriptor parent) {
        this.parent = parent;
    }

    public boolean shouldGenerateAttachments() {
        return generateAttachments;
    }

    public void setAttachments(boolean attachments) {
        generateAttachments = attachments;
    }

    public ArrayList<ForceDescriptor> getSubforces() {
        return subforces;
    }

    public void setSubforces(ArrayList<ForceDescriptor> subforces) {
        this.subforces = subforces;
    }

    public void addSubforce(ForceDescriptor fd) {
        subforces.add(fd);
        fd.setParent(this);
    }

    public ArrayList<ForceDescriptor> getAttached() {
        return attached;
    }

    public void setAttached(ArrayList<ForceDescriptor> attached) {
        this.attached = attached;
    }

    public void addAttached(ForceDescriptor fd) {
        attached.add(fd);
    }

    public double getDropshipPct() {
        return dropshipPct;
    }

    public void setDropshipPct(double dropshipPct) {
        this.dropshipPct = dropshipPct;
    }

    public double getJumpshipPct() {
        return jumpshipPct;
    }

    public void setJumpshipPct(double jumpshipPct) {
        this.jumpshipPct = jumpshipPct;
    }

    public double getCargo() {
        return cargo;
    }

    public void setCargo(double cargo) {
        this.cargo = cargo;
    }

    public Set<String> getFlags() {
        return flags;
    }

    public boolean isTopLevel() {
        return topLevel;
    }

    public void setTopLevel(boolean topLevel) {
        this.topLevel = topLevel;
    }

    public boolean isElement() {
        return element;
    }

    public void setElement(boolean element) {
        this.element = element;
    }

    public int getPositionIndex() {
        return positionIndex;
    }

    public int getNameIndex() {
        return nameIndex;
    }

    public String getFluffName() {
        return fluffName;
    }

    public void setFluffName(String fluffName) {
        this.fluffName = fluffName;
    }

    public Entity getEntity() {
        return entity;
    }

    public void addAllEntities(List<Entity> list) {
        if (isElement()) {
            if (entity != null) {
                list.add(entity);
            }
        }
        subforces.stream().forEach(sf -> sf.addAllEntities(list));
        attached.stream().forEach(sf -> sf.addAllEntities(list));
    }

    public ForceDescriptor createChild(int index) {
        ForceDescriptor retVal = new ForceDescriptor();
        retVal.index = index;
        retVal.name = null;
        retVal.faction = faction;
        retVal.year = year;
        retVal.weightClass = weightClass;
        retVal.unitType = unitType;
        retVal.movementModes.addAll(movementModes);
        retVal.roles.addAll(roles);
        retVal.roles.remove(MissionRole.COMMAND);
        retVal.models.addAll(models);
        retVal.chassis.addAll(chassis);
        retVal.variants.addAll(variants);
        retVal.augmented = augmented;
        retVal.rating = rating;
        retVal.experience = experience;
        retVal.camo = camo;
        retVal.flags.addAll(flags);
        retVal.topLevel = false;
        retVal.rankSystem = rankSystem;
        retVal.generateAttachments = generateAttachments;

        return retVal;
    }
}

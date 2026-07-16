/*
 * Copyright (C) 2016-2025 The MegaMek Team. All Rights Reserved.
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

import java.util.*;
import java.util.stream.Collectors;

import megamek.client.ratgenerator.Ruleset.ProgressListener;
import megamek.common.annotations.Nullable;
import megamek.common.compute.Compute;
import megamek.common.loaders.EntityLoadingException;
import megamek.common.loaders.MekFileParser;
import megamek.common.loaders.MekSummary;
import megamek.common.loaders.MekSummaryCache;
import megamek.common.units.Entity;
import megamek.common.units.EntityMovementMode;
import megamek.common.units.EntityWeightClass;
import megamek.common.units.UnitType;
import megamek.logging.MMLogger;

/**
 * Describes the characteristics of a force. May be changed during generation.
 *
 * @author Neoancient
 */
public class ForceDescriptor {
    private final static MMLogger LOGGER = MMLogger.create(ForceDescriptor.class);

    public static final int REINFORCED = 1;
    public static final int UNDERSTRENGTH = -1;

    public static final int EXP_GREEN = 0;
    public static final int EXP_REGULAR = 1;
    public static final int EXP_VETERAN = 2;

    // Mapped to Dragoon Rating in MHQ
    public static final int RATING_0 = 0;
    public static final int RATING_1 = 1;
    public static final int RATING_2 = 2;
    public static final int RATING_3 = 3;
    public static final int RATING_4 = 4;
    public static final int RATING_5 = 5;

    public static final String[] ORDINALS = { "First", "Second", "Third", "Fourth", "Fifth", "Sixth", "Seventh",
                                              "Eighth", "Ninth", "Tenth" };

    public static final String[] PHONETIC = { "Alpha", "Bravo", "Charlie", "Delta", "Echo", "Foxtrot", "Golf", "Hotel",
                                              "India", "Juliett", "Kilo", "Lima", "Mike", "November", "Oscar", "Papa",
                                              "Quebec", "Romeo", "Sierra", "Tango", "Uniform", "Victor", "Whiskey",
                                              "X-ray", "Yankee", "Zulu" };

    public static final String[] GREEK = { "Alpha", "Beta", "Gamma", "Delta", "Epsilon", "Zeta", "Eta", "Theta", "Iota",
                                           "Kappa", "Lambda", "Mu", "Nu", "Xi", "Omicron", "Pi", "Rho", "Sigma", "Tau",
                                           "Upsilon", "Phi", "Chi", "Psi", "Omega" };

    public static final String[] LATIN = { "Prima", "Secunda", "Tertia", "Quarta", "Quinta", "Sexta", "Septima",
                                           "Octava", "Nona", "Decima" };

    public static final String[] ROMAN = { "I", "II", "III", "IV", "V", "VI", "VII", "VIII", "IX", "X", "XI", "XII",
                                           "XIII", "XIV", "XV", "XVI", "XVII", "XVIII", "XIX", "XX" };

    private int index;
    /**
     * Unique id of this node within the generated force, assigned by {@link #assignForceIds(int)} and
     * emitted by {@link #getForceString()}. Must be unique across the whole generated force so the
     * server does not merge distinct forces that would otherwise share an id. -1 means unassigned.
     */
    private int forceId = -1;
    private String name;
    private String faction;
    private Integer year;
    private Integer echelon;
    private int sizeMod;
    private boolean augmented;
    private Integer weightClass;
    private Integer unitType;
    // Per-cluster-type weight budget parsed from <weightTarget> blocks, keyed by unit type. Set only on
    // the cluster node (deliberately NOT copied to children in createChild) and consumed by
    // WeightBudgetAllocator after the tree is built. Null means no budget for this node.
    private Map<Integer, WeightTarget> weightTargets;
    private final HashSet<EntityMovementMode> movementModes;
    private final HashSet<MissionRole> roles;
    private String rating;
    private Integer experience;
    private Integer rankSystem;
    private Integer coRank;
    private final HashSet<String> models;
    private final HashSet<String> chassis;
    private final HashSet<String> variants;
    private CrewDescriptor co;
    private CrewDescriptor xo;
    private String camo;

    private final HashSet<String> flags;

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
    private ArrayList<ForceDescriptor> subForces;
    private ArrayList<ForceDescriptor> attached;
    private double dropshipPct = 0.0;
    private double jumpshipPct = 0.0;
    private double warshipPct = 0.0;
    private double cargo = 0.0;
    private boolean fighterComplement = false;

    public ForceDescriptor() {
        faction = FactionRecord.IS_GENERAL_KEY;
        year = 3067;
        movementModes = new HashSet<>();
        roles = new HashSet<>();
        formationType = null;
        experience = EXP_REGULAR;
        models = new HashSet<>();
        chassis = new HashSet<>();
        variants = new HashSet<>();
        parent = null;
        subForces = new ArrayList<>();
        attached = new ArrayList<>();
        flags = new HashSet<>();
        topLevel = false;
        element = false;
        positionIndex = -1;
        nameIndex = -1;
        fluffName = null;
    }

    /**
     * Checks whether the chassis matches the unit type for this node of the force tree. If a list of acceptable chassis
     * has been assigned, checks whether the chassis is in the list. unit type.
     *
     * @param cRec A unit chassis record
     *
     * @return Whether the chassis is of the correct unit type and is on the list of acceptable chassis if it exists.
     */
    public boolean matches(ChassisRecord cRec) {
        if (cRec.getUnitType() != unitType) {
            return false;
        } else {
            return chassis.isEmpty() || chassis.contains(cRec.getChassis());
        }
    }

    /**
     * If a list of acceptable chassis, models, or variants has been assigned, checks whether the model is among them.
     *
     * @param mRec A unit model record
     *
     * @return Whether the model is on the list of acceptable chassis, variants, or models.
     */
    public boolean matches(ModelRecord mRec) {
        if (!chassis.isEmpty() && !chassis.contains(mRec.getChassis())) {
            return false;
        } else if (!variants.isEmpty() && !variants.contains(mRec.getModel())) {
            return false;
        } else {
            return models.isEmpty() || models.contains(mRec.getKey());
        }
    }

    /**
     * Goes through the force tree structure and generates units for all leaf nodes.
     */
    public void generateUnits(ProgressListener l, double progress) {
        // If the parent node has a chassis or model assigned, it carries through to the
        // children.
        if (null != parent) {
            chassis.addAll(parent.getChassis());
            models.addAll(parent.getModels());
        }
        // Artillery is built by unit selection, not by FormationType. The "Mobile Artillery"
        // formation backfills non-artillery units when it cannot fill, which is exactly what we
        // want to avoid. Clearing the formation and group rule routes each element through
        // generate() individually (via the subforce recursion below), where the artillery ladder
        // (artillery Mek -> artillery Vehicle -> other Mek) applies.
        if (roles.contains(MissionRole.ARTILLERY)) {
            formationType = null;
            generationRule = null;
            // The formation pick stamps combat roles (Recon, Fire Support, Urban, etc.) on these
            // nodes during the build. With the formation now cleared, those roles are spurious -
            // they would mislabel an artillery star as "Mobile Recon" (getDescription builds the
            // name from roles) and could skew unit selection. Keep only the artillery roles.
            roles.removeIf(r -> (r != MissionRole.ARTILLERY)
                  && (r != MissionRole.MISSILE_ARTILLERY)
                  && (r != MissionRole.MIXED_ARTILLERY));
            // Battery uniformity: an artillery formation fields one gun type. The first artillery
            // node with children picks a single artillery unit and pins it via setUnit, which on a
            // non-leaf node propagates the model to every descendant. Each element then resolves
            // that one model by name (generateUnits' getModelRecord rescue), so the whole battery
            // comes out identical even when the Mek->vehicle fallback changes the unit type.
            if (models.isEmpty() && chassis.isEmpty() && !subForces.isEmpty()) {
                ModelRecord artilleryUnitRecord = generateArtilleryPreferred();
                if (artilleryUnitRecord != null) {
                    setUnit(artilleryUnitRecord);
                }
            }
        }
        // First see if a formation has been assigned. If unable to fulfill the
        // formation requirements, generate using default parameters.
        if (subForces.isEmpty()) {
            ModelRecord modelRecord = generate();
            if (null == modelRecord && !models.isEmpty()) {
                modelRecord = RATGenerator.getInstance().getModelRecord(getModelName());
            }
            if (null != modelRecord) {
                setUnit(modelRecord);
            } else if (models.isEmpty() && chassis.size() == 1) {
                // Chassis-only element (e.g. a named WarShip referenced by chassis for a faction with
                // no warship availability table): generate() found no ModelRecord, so setUnit - which
                // is what normally flags a leaf as an element - was never called. Mark it an element
                // here so loadEntities resolves it by chassis name (see getModelName) and the warship
                // CSV records it (both gate on isElement()).
                element = true;
                LOGGER.debug("[ForceGen][ChassisOnly] generateUnits leaf: RAT gave no unit; marked"
                            + " element=true, will load by chassis name. chassis={} unitType={} faction={} year={}",
                      chassis, unitType, faction, year);
            } else {
                LOGGER.error("[ForceGen] Could not generate unit: RAT returned no model and no chassis/model " +
                                  "fallback applied. unitType={} faction={} year={} weightClass={} roles={} " +
                                  "models={} chassis={}",
                      describeUnitType(unitType), faction, year, weightClass, roles, models, chassis);
            }
        } else {
            if (null != formationType) {
                // Simple leaf node (Lance, Star, etc.
                if (null != generationRule) {
                    // In cases like Novas and air lances the formation rules only apply to some of
                    // the units
                    if (!generateAndAssignFormation(subForces, generationRule.equals("chassis"), 0)) {
                        generateLance(subForces);
                        formationType = null;
                    }
                } else {
                    // If group generation is not set, then either this is a compound formation (e.g. squadron,
                    // aero/vehicle Point) or we are generating uniform sub forces such as companies in SL line units
                    try {
                        Map<String, List<ForceDescriptor>> byGenRule = subForces.stream()
                              .collect(Collectors.groupingBy(
                                    ForceDescriptor::getGenerationRule));
                        if (byGenRule.containsKey("group")) {
                            if (!generateAndAssignFormation(byGenRule.get("group")
                                        .stream()
                                        .map(ForceDescriptor::getSubForces)
                                        .flatMap(Collection::stream)
                                        .collect(Collectors.toList()),
                                  false,
                                  byGenRule.get("group").size())) {
                                formationType = null;
                            }
                        } else if (byGenRule.containsKey("model")) {
                            generateAndAssignFormation(byGenRule.get("model"), false, 0);
                        } else if (byGenRule.containsKey("chassis")) {
                            generateAndAssignFormation(byGenRule.get("chassis"), true, 0);
                        }
                    } catch (NullPointerException ex) {
                        LOGGER.error(ex, "Found null generation rule in force node with formation set.");
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
                            generateLance(subForces);
                            break;
                    }
                }
            }
        }
        int count = subForces.size() + attached.size();
        subForces.forEach(fd -> fd.generateUnits(l, progress / count));
        attached.forEach(fd -> fd.generateUnits(l, progress / count));
        if (count == 0 && null != l) {
            l.updateProgress(progress, "Populating force tree");
        }
    }

    /**
     * Sorts out all sub force nodes eligible for the <code>FormationType</code> and attempts to generate a formation
     * based on their parameters. If the formation is successfully generated, it is distributed to the sub forces in the
     * order provided. For leaf node, the unit is set. For non-final nodes, the unit is added to either the model or
     * chassis list depending on the provided grouping rule. Any sub forces that are not eligible for the formation are
     * then generated.
     *
     * @param subs      The sub forces to generate unit for. These need not be direct children of
     *                  <code>this</code>.
     * @param chassis   If true, any non-final sub force node will have the generated unit added to the chassis list
     *                  instead of the model list.
     * @param numGroups The number of groups to pass on to formation generation; used to override standard grouping
     *                  constraints (e.g. matched pairs in fighter squadrons).
     *
     * @return Whether the formation was successfully generated.
     */
    private boolean generateAndAssignFormation(List<ForceDescriptor> subs, boolean chassis, int numGroups) {
        Map<Boolean, List<ForceDescriptor>> eligibleSubs = subs.stream()
              .collect(Collectors.groupingBy(fd -> null !=
                    fd.getUnitType() &&
                    (formationType.isAllowedUnitType(
                          fd.getUnitType())) ||
                    (augmented &&
                          (fd.getUnitType() ==
                                UnitType.BATTLE_ARMOR) ||
                          fd.getUnitType() ==
                                UnitType.INFANTRY)));
        if (eligibleSubs.containsKey(true)) {
            if (eligibleSubs.get(true).isEmpty()) {
                return false;
            } else {
                List<ModelRecord> list;
                if (augmented) {
                    list = generateNovaFormation(eligibleSubs.get(true), ModelRecord.NETWORK_NONE, numGroups);
                } else {
                    list = generateFormation(eligibleSubs.get(true), ModelRecord.NETWORK_NONE, numGroups);
                }
                if (list.isEmpty()) {
                    return false;
                } else {
                    for (int i = 0; i < list.size(); i++) {
                        // The formation requirements do not apply to the infantry part of a nova, and
                        // those units have already been generated by generateNovaFormation.
                        if (augmented &&
                              (eligibleSubs.get(true).get(i).getUnitType() == UnitType.BATTLE_ARMOR ||
                                    eligibleSubs.get(true).get(i).getUnitType() == UnitType.INFANTRY)) {
                            continue;
                        }
                        ForceDescriptor target = eligibleSubs.get(true).get(i);
                        ModelRecord picked = list.get(i);
                        if (target.getSubForces().isEmpty()) {
                            target.setUnit(picked);
                            LOGGER.debug("[ForceGen][Formation]   assign LEAF '{}' <- setUnit {}(wc={})",
                                  target.parseName(), picked.getKey(), picked.getWeightClass());
                        } else if (chassis) {
                            target.getChassis().add(picked.getChassis());
                            LOGGER.debug("[ForceGen][Formation]   assign NON-LEAF '{}' <- PIN chassis '{}' (children"
                                  + " will regenerate against this)", target.parseName(), picked.getChassis());
                        } else {
                            target.getModels().add(picked.getKey());
                            LOGGER.debug("[ForceGen][Formation]   assign NON-LEAF '{}' <- PIN model '{}' (children"
                                        + " will regenerate against this; Task #2 failure point if unavailable)",
                                  target.parseName(), picked.getKey());
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
     * @param subs        A list of <ForceDescriptor</code> nodes.
     * @param networkMask The type of C3 network that should be used in generating the formation.
     * @param numGroups   Overrides the default value for formation grouping constraints (e.g. some Capellan squadrons
     *                    have two groups of three instead of the standard three groups of two).
     *
     * @return The list of units that make up the formation, or an empty list if a formation could not be generated with
     *       the given parameters.
     */
    private List<ModelRecord> generateFormation(List<ForceDescriptor> subs, int networkMask, int numGroups) {
        // Collect the weight classes the force tree assigned to this formation's elements. Passing
        // them to the formation builder keeps it within the lance's intended weight profile; left
        // null it would pick any weight the FormationType itself allows (e.g. a light Mek in a
        // Heavy/Assault Hunter lance).
        Set<Integer> formationWeightClasses = new TreeSet<>();
        for (ForceDescriptor sub : subs) {
            if (sub.useWeightClass() && (null != sub.getWeightClass())
                  && (sub.getWeightClass() >= EntityWeightClass.WEIGHT_ULTRA_LIGHT)) {
                formationWeightClasses.add(sub.getWeightClass());
            }
        }
        Map<Parameters, Integer> paramCount = new HashMap<>();
        for (ForceDescriptor sub : subs) {
            paramCount.merge(new Parameters(sub.getFactionRec(),
                  sub.getUnitType(),
                  sub.getYear(),
                  sub.ratGeneratorRating(),
                  formationWeightClasses.isEmpty() ? null : formationWeightClasses,
                  networkMask,
                  sub.getMovementModes(),
                  sub.getRoles(),
                  0,
                  sub.getFactionRec()), 1, Integer::sum);
        }

        List<Parameters> params = new ArrayList<>();
        List<Integer> numUnits = new ArrayList<>();
        for (Map.Entry<Parameters, Integer> e : paramCount.entrySet()) {
            params.add(e.getKey());
            numUnits.add(e.getValue());
        }
        // Check for amount of C3 equipment generated and if certain thresholds are met
        // regenerate the unit
        // with a valid network.
        List<MekSummary> unitList = formationType.generateFormation(params, numUnits, networkMask, false, 0, numGroups);
        LOGGER.debug(
              "[ForceGen][Formation] CALLER name='{}' formation='{}' subWeightClasses={} requested={} -> got {} units: {}",
              parseName(),
              formationType.getName(),
              formationWeightClasses,
              subs.size(),
              unitList.size(),
              unitList.stream().map(mekSummary -> mekSummary.getName() + "(" + mekSummary.getWeightClass() + ")")
                    .collect(java.util.stream.Collectors.joining(", ")));
        if (networkMask == ModelRecord.NETWORK_NONE) {
            int c3m = 0;
            int c3s = 0;
            int c3i = 0;
            int nova = 0;
            for (MekSummary ms : unitList) {
                ModelRecord mRec = RATGenerator.getInstance().getModelRecord(ms.getName());
                int mask = mRec == null ? ModelRecord.NETWORK_NONE : mRec.getNetworkMask();

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
            // Any lance with a C3 master should have three slave units (or the remainder of
            // the unit, if smaller)
            if (c3m > 0) {
                if ((c3m > 1) || (c3s < Math.max(3, unitList.size() - 1))) {
                    networkMask = ModelRecord.NETWORK_C3_MASTER;
                } else {
                    flags.add("c3");
                }
            } else {
                // If no master was generated, each slave unit gives a cumulative 1/3 chance of
                // a network. Isolated
                // C3 slaves will be encountered, but not usually more than one or maybe two in
                // a lance.
                if (c3s > Compute.randomInt(3)) {
                    networkMask = ModelRecord.NETWORK_C3_MASTER;
                } else if (c3i > Compute.randomInt(5)) {
                    // Each C3i gives a 1/5 chance of a full C3i network. A network is still useful
                    // if not full.
                    networkMask = ModelRecord.NETWORK_C3I;
                } else if (nova > 0) {
                    // The Nova CEWS is specialized enough to add a complete network if any is
                    // present.
                    networkMask = ModelRecord.NETWORK_NOVA;
                }
            }
            if (networkMask != ModelRecord.NETWORK_NONE) {
                List<MekSummary> netList = formationType.generateFormation(params,
                      numUnits,
                      networkMask,
                      false,
                      0,
                      numGroups);
                // Attempt to create the type of network indicated. If no unit can be created
                // that fits the
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
        return unitList.stream()
              .map(mekSummary -> RATGenerator.getInstance().getModelRecord(mekSummary.getName()))
              .collect(Collectors.toList());
    }

    /**
     * The Nova formation is a composite of base type and battle armor. The formationType only applies to the base unit
     * type (Mek, vehicle, fighter). The BA must be eligible for mechanized and have at least one omni among the base
     * units per BA squad/point, excepting any BA with magnetic clamps.
     * <p>
     * Though the rules in Campaign Operations only cover BA novas, the Hell's Horses vehicle/conventional infantry nova
     * formations require an adapted version of the Nova formation rules to work.
     * <p>
     * This method generates and assigns infantry elements and returns the list of base elements.
     *
     * @param subs        A list of <ForceDescriptor</code> nodes.
     * @param networkMask The type of C3 network that should be used in generating the formation.
     * @param numGroups   Overrides the default value for formation grouping constraints (e.g. some Capellan squadrons
     *                    have two groups of three instead of the standard three groups of two).
     *
     * @return The list of units that make up the base formation, or an empty list if a formation could not be generated
     *       with the given parameters.
     */
    private List<ModelRecord> generateNovaFormation(List<ForceDescriptor> subs, int networkMask, int numGroups) {
        // Split base and infantry units
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
        // If there is any conventional infantry we'll generate it first, then assign
        // the APC role
        // to as many vehicles (if any) in the base units as we have foot infantry. Any
        // remaining vehicles
        // will get the infantry support role.
        if (!infSubs.isEmpty()) {
            generateLance(infSubs);
            int footCount = (int) infSubs.stream()
                  .filter(fd -> fd.getMovementModes().contains(EntityMovementMode.INF_LEG))
                  .count();
            for (ForceDescriptor baseSub : baseSubs) {
                if (baseSub.getUnitType() == UnitType.TANK || baseSub.getUnitType() == UnitType.VTOL) {
                    if (footCount > 0) {
                        baseSub.getRoles().add(MissionRole.APC);
                        footCount--;
                    } else {
                        baseSub.getRoles().add(MissionRole.INF_SUPPORT);
                    }
                }
            }
        }
        // Generate the base units according to the formation type.
        List<ModelRecord> baseUnitList = null;
        if (!baseSubs.isEmpty()) {
            baseUnitList = generateFormation(baseSubs, networkMask, numGroups);
        }
        if (null == baseUnitList) {
            generateLance(baseSubs);
            baseUnitList = baseSubs.stream()
                  .map(ForceDescriptor::getModelName)
                  .map(m -> RATGenerator.getInstance().getModelRecord(m))
                  .filter(Objects::nonNull)
                  .collect(Collectors.toList());
        }

        // Any BA in excess of the number of omni base units will require mag clamps, up to the number of base units.
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
     * Generates a lance or other end-level unit (star, level ii) by individual units rather than as an entire
     * formation. Some unit cohesion is attempted for certain unit types, such as building vehicle lances out of the
     * same model or pairing fighter chassis. The equipment rating has an effect on unit cohesion, such that lower rated
     * units are more likely to have mismatched equipment.
     *
     * @param subs The sub forces that describe the individual elements of the lance
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

        /*
         * This method can be used to generate pieces of a combined arms unit, so we need to get the unit type from
         * one of the sub forces rather than the current.
         */

        Integer ut = subs.getFirst().getUnitType();

        boolean useWeights = useWeightClass(ut);
        ArrayList<Integer> weights = new ArrayList<>();
        if (useWeights) {
            for (ForceDescriptor sub : subs) {
                weights.add(sub.getWeightClass());
            }
            LOGGER.debug("[ForceGen][Weight] generateLance: unitType={} faction={} parentWeightClass={} " +
                        "element target weights={}",
                  UnitType.getTypeName(ut), faction, getWeightClassCode(), weights);
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
         * Using the rating level relative to the total number of levels throws the
         * results
         * off for ComStar, which should behave as A-B out of A-F rather than A-B out of
         * A-B.
         *
         * int totalLevels =
         * RATGenerator.getInstance().getFaction(faction.split(",")[0]).getRatingLevels(
         * ).size();
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
            if (!(ut == UnitType.MEK || (ut == UnitType.AEROSPACE_FIGHTER && subs.size() > 3))) {
                baseModel = subs.getFirst().generate();
            }
            if (ut == UnitType.AEROSPACE_FIGHTER || ut == UnitType.CONV_FIGHTER || ut == UnitType.AERO) {
                target -= 3;
            }
            if (roles.contains(MissionRole.ARTILLERY)) {
                if (baseModel != null && baseModel.getRoles().contains(MissionRole.MISSILE_ARTILLERY)) {
                    roles.remove(MissionRole.ARTILLERY);
                    roles.add(MissionRole.MISSILE_ARTILLERY);
                } else {
                    target -= 4;
                }
            }
        }

        for (ForceDescriptor sub : subs) {
            boolean foundUnit = false;
            if (baseModel == null || !ut.equals(sub.getUnitType())) {
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
                        av = RATGenerator.getInstance().findChassisAvailabilityRecord(era, model, faction, getYear());
                        if (av == null) {
                            for (String alt : RATGenerator.getInstance().getFaction(faction).getParentFactions()) {
                                av = RATGenerator.getInstance()
                                      .findChassisAvailabilityRecord(era, model, alt, getYear());
                                if (av != null) {
                                    break;
                                }
                            }
                        }
                        if (Compute.d6(2) >=
                              target - ((av == null) ? 0 : av.adjustForRating(ratingLevel, totalLevels))) {
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
                        if (mRec != null &&
                              weights.contains(mRec.getWeightClass()) &&
                              RATGenerator.getInstance().findModelAvailabilityRecord(era, model, faction, getYear())
                                    != null) {
                            av = RATGenerator.getInstance()
                                  .findChassisAvailabilityRecord(era, mRec.getChassisKey(), faction, getYear());
                            if (av == null) {
                                for (String alt : RATGenerator.getInstance().getFaction(faction).getParentFactions()) {
                                    av = RATGenerator.getInstance()
                                          .findChassisAvailabilityRecord(era,
                                                mRec.getChassisKey(),
                                                alt,
                                                getYear());
                                    if (av != null) {
                                        break;
                                    }
                                }
                            }
                            if (Compute.d6(2) >=
                                  target - ((av == null) ? 0 : av.adjustForRating(ratingLevel, totalLevels))) {
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
                    av = RATGenerator.getInstance()
                          .findChassisAvailabilityRecord(era, baseModel.getChassisKey(), faction, getYear());
                    if (av == null) {
                        for (String alt : RATGenerator.getInstance().getFaction(faction).getParentFactions()) {
                            av = RATGenerator.getInstance()
                                  .findChassisAvailabilityRecord(era, baseModel.getChassisKey(), alt, getYear());
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
                            switch (baseModel.getMekSummary().getUnitSubType()) {
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
                    sub.setWeightClass(weights.getFirst());
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
     * Assigns a specific model to this node of the force tree. If this is a leaf node it will be flagged as an element.
     * If it has child nodes, they will all be made up of the same model unless changed by a rule at a lower level of
     * organization.
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
        if (subForces.isEmpty()) {
            element = true;
            movementModes.clear();
            movementModes.add(unit.getMovementMode());
            if (null == unitType) {
                unitType = unit.getUnitType();
            }
            if (((unitType == UnitType.MEK) ||
                  (unitType == UnitType.AEROSPACE_FIGHTER) ||
                  (unitType == UnitType.TANK)) && unit.isOmni()) {
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

    public @Nullable ModelRecord generate() {
        // A null unit type means there is no concrete element to generate here (e.g. a
        // subforce that failed to inherit a unitType). Bail out gracefully instead of NPEing
        // in the failure-logging path below, which would abort the entire force generation.
        if (unitType == null) {
            return null;
        }
        // Artillery preference: before the rating ladder below relaxes the mission role and
        // backfills a non-artillery unit, try a real artillery unit. Front-line (Mek) prefers an
        // artillery BattleMek then drops to an artillery combat vehicle; second-line (Tank) stays
        // vehicle. Only if no artillery unit exists for this faction and year do we fall through and
        // let the normal ladder field a non-artillery unit of the original type as a last resort.
        // Skipped when a model is already pinned (battery uniformity), so the pinned gun wins and
        // every element resolves to the same unit instead of re-picking its own.
        if (models.isEmpty() && ((unitType == UnitType.MEK) || (unitType == UnitType.TANK))
              && roles.contains(MissionRole.ARTILLERY)) {
            ModelRecord artilleryUnitRecord = generateArtilleryPreferred();
            if (artilleryUnitRecord != null) {
                return artilleryUnitRecord;
            }
        }
        // Equipment-rating fallback ladder: try the force's own rating first and, only when
        // generation comes up empty, step down to progressively worse ratings (never better).
        // A rating-C force may field C/D/F equipment when nothing matches at its own rating,
        // but never the A/B grades reserved for better-equipped commands.
        List<String> failureTrace = new ArrayList<>();
        for (String ratGenRating : ratingFallbackList()) {
            ModelRecord modelRecord = generateAtRating(ratGenRating, failureTrace);
            if (modelRecord != null) {
                return modelRecord;
            }
        }

        // Ladder exhausted: no unit found at any rating. Emit the diagnostic for EVERY unit type, not
        // just Meks - a combined-arms force fails just as often on tanks, aero, infantry and vessels,
        // and those were previously logged only by a terse one-liner with no context.
        if (models.isEmpty()) {
            // Genuine failure: no pinned model to fall back on, so the caller's
            // getModelRecord(getModelName()) rescue (generateUnits) cannot recover. Log the full trace,
            // joined into a single record rather than one line per attempt.
            LOGGER.debug("[ForceGen][Weight] generate() FAILED for {} requestedWeight={} -> no unit found."
                        + " element: faction={} year={} echelon={} roles={} movementModes={}"
                        + " models={} chassis={}{}",
                  describeUnitType(unitType), weightClass, faction, year, echelon,
                  roles, movementModes, models, chassis, formatFailureTrace(failureTrace));
        } else {
            // Not a real failure: a formation already pinned this model (setUnit) but it is not in the
            // element's own faction/year/role/weight table. The caller resolves it by name via the
            // getModelRecord fallback, so emit one concise line instead of the full FAILED + attempt trace.
            LOGGER.debug("[ForceGen][Weight] generate() table-miss for pinned model(s) {} (unitType={} faction={}"
                        + " year={} weightClass={} roles={}); resolving by name via fallback",
                  models, describeUnitType(unitType), faction, year, weightClass, roles);
        }
        return null;
    }

    /**
     * Renders a unit type for diagnostic messages without unboxing a {@code null}.
     *
     * <p>{@link #unitType} is a nullable {@link Integer} - a subforce can spawn child nodes without
     * propagating a unit type (see {@link #generateAtRating(String, List)}) - while
     * {@link UnitType#getTypeDisplayableName(int)} takes a primitive. Passing the field straight through
     * throws a {@link NullPointerException} on unboxing, and because logger arguments are evaluated
     * eagerly it throws even when {@code DEBUG} is disabled.</p>
     *
     * @param unitType the unit type constant to describe, or {@code null} if this element has none
     *
     * @return the displayable name of the unit type, or {@code "unspecified"} when {@code unitType} is
     *       {@code null}
     */
    private static String describeUnitType(@Nullable Integer unitType) {
        return (unitType == null) ? "unspecified" : UnitType.getTypeDisplayableName(unitType);
    }

    /**
     * Formats the collected generation attempts as a single indented block appended to the failure message.
     *
     * <p>Emitted as one log record rather than one record per attempt: {@link #generate()} is called for
     * every leaf of the force tree, so a per-attempt loop floods the log and violates the project rule
     * against logging inside loops.</p>
     *
     * @param failureTrace the attempt descriptions gathered by {@link #generateAtRating(String, List)}; may
     *                     be empty when {@code DEBUG} is disabled, in which case nothing is appended
     *
     * @return a newline-prefixed block of indented attempt lines, or the empty string when there are none
     */
    private static String formatFailureTrace(List<String> failureTrace) {
        if (failureTrace.isEmpty()) {
            return "";
        }
        StringBuilder formattedTrace = new StringBuilder();
        for (String failureTraceLine : failureTrace) {
            formattedTrace.append("\n[ForceGen][Weight]   attempt: ").append(failureTraceLine);
        }
        return formattedTrace.toString();
    }

    /**
     * Front-line artillery fallback used by {@link #generate()}: looks for a true artillery unit, preferring an
     * artillery BattleMek and dropping to an artillery combat vehicle, across the equipment-rating ladder. Weight class
     * is intentionally NOT constrained - artillery hulls have fixed tonnages, so the artillery role takes priority over
     * the star's rolled weight. Returns {@code null} when no artillery unit of either type exists for this faction and
     * year, leaving {@link #generate()} to relax the role and field a non-artillery Mek as a last resort.
     *
     * @return an artillery unit of a preferred type, or {@code null} if none is available
     */
    private @Nullable ModelRecord generateArtilleryPreferred() {
        // Front-line (Mek) prefers an artillery Mek, then an artillery vehicle. Second-line (Tank)
        // stays vehicle, honoring the "front line = Mek, otherwise = vehicle" rule.
        int[] preferredTypes = (unitType == UnitType.TANK)
              ? new int[] { UnitType.TANK }
              : new int[] { UnitType.MEK, UnitType.TANK };
        for (int candidateType : preferredTypes) {
            for (String ratGenRating : ratingFallbackList()) {
                ModelRecord artilleryUnitRecord = generateArtilleryUnit(candidateType, ratGenRating);
                if (artilleryUnitRecord != null) {
                    return artilleryUnitRecord;
                }
            }
        }
        return null;
    }

    /**
     * Generates a single artillery unit of the given unit type at a fixed equipment rating, keeping the artillery
     * mission role strict so non-artillery units are never substituted. Returns {@code null} if no qualifying artillery
     * unit exists.
     */
    private @Nullable ModelRecord generateArtilleryUnit(int candidateType, String ratGenRating) {
        UnitTable table = UnitTable.findTable(getFactionRec(),
              candidateType,
              getYear(),
              ratGenRating,
              new ArrayList<>(),
              ModelRecord.NETWORK_NONE,
              movementModes,
              EnumSet.of(MissionRole.ARTILLERY),
              2);
        MekSummary mekSummary = table.generateUnit();
        if (mekSummary == null) {
            return null;
        }
        return RATGenerator.getInstance().getModelRecord(mekSummary.getName());
    }

    /**
     * Builds the equipment-rating fallback ladder for {@link #generate()}: the force's own resolved rating followed by
     * each progressively worse rating in the faction's rating system. Generation tries each in order and stops at the
     * first that yields a unit, so worse ratings act only as a safety net - the force never fields equipment better
     * than its assigned rating.
     */
    private List<String> ratingFallbackList() {
        String startRating = ratGeneratorRating();
        Ruleset ruleset = Ruleset.findRuleset(this);
        if (ruleset != null) {
            List<String> ladder = ruleset.getRatingsAtOrWorseThan(startRating);
            if (!ladder.isEmpty()) {
                return ladder;
            }
        }
        return Collections.singletonList(startRating);
    }

    /**
     * Generates a single unit for this descriptor at a fixed equipment rating. If the criteria cannot be matched, first
     * tries the next closest weight class, then ignores mission role, then the next weight class, then ignores motive
     * types, then the remaining weight classes. Returns {@code null} if no unit could be generated at the given
     * rating.
     */
    private @Nullable ModelRecord generateAtRating(String ratGenRating, List<String> failureTrace) {
        final int[][] alternateWeights = { { 1, 2, 3, 4, 5 }, // UL
                                           { 2, 0, 3, 4, 5 }, // L
                                           { 3, 1, 4, 0, 5 }, // M
                                           { 2, 4, 1, 5, 0 }, // H
                                           { 3, 2, 5, 1, 0 }, // A
                                           { 4, 3, 2, 1, 0 } // SH
        };
        /* Work with a copy */
        ForceDescriptor workingCopy = createChild(index);
        workingCopy.setEchelon(echelon);
        workingCopy.setCoRank(coRank);
        workingCopy.getRoles().clear();
        workingCopy.getRoles().addAll(roles.stream().filter(role -> role.fitsUnitType(unitType)).toList());

        // Without a unit type there is no table to draw from. This can happen when a subforce
        // spawns child nodes without propagating a unit type (e.g. a Solahma star group). Treat it
        // as a generation failure rather than letting UnitTable.findTable NPE on the unboxed int.
        if (workingCopy.getUnitType() == null) {
            return null;
        }

        int weightTierIndex = (useWeightClass() && weightClass != null && weightClass != -1) ? 0 : 4;

        while (weightTierIndex < 5) {
            for (int roleStrictness = 3; roleStrictness >= 0; roleStrictness--) {
                List<Integer> weightClasses = new ArrayList<>();
                if (useWeightClass() && null != workingCopy.getWeightClass()
                      && workingCopy.getWeightClass() >= EntityWeightClass.WEIGHT_ULTRA_LIGHT) {
                    weightClasses.add(workingCopy.getWeightClass());
                }
                UnitTable table = UnitTable.findTable(workingCopy.getFactionRec(),
                      workingCopy.getUnitType(),
                      workingCopy.getYear(),
                      ratGenRating,
                      weightClasses,
                      ModelRecord.NETWORK_NONE,
                      workingCopy.getMovementModes(),
                      workingCopy.getRoles(),
                      roleStrictness);
                MekSummary mekSummary;
                if (!workingCopy.getModels().isEmpty()) {
                    mekSummary = table.generateUnit(unit -> workingCopy.getModels().contains(unit.getName()));
                } else if (!workingCopy.getChassis().isEmpty()) {
                    mekSummary = table.generateUnit(unit -> workingCopy.getChassis().contains(unit.getChassis()));
                } else {
                    mekSummary = table.generateUnit();
                }
                // Gate on the log level, not on the unit type: the force generator serves combined-arms
                // forces, so tanks, aero, infantry and vessels need this trace as much as Meks do. The
                // check is still required because String.format runs on every rung of the rating ladder
                // for every leaf of the force tree, and that cost must not be paid when DEBUG is off.
                if (LOGGER.isDebugEnabled()) {
                    failureTrace.add(String.format(
                          "unitType=%s rating=%s weightTierIndex=%d weightClass=%s roleStrictness=%d roles=%s"
                                + " moves=%s models=%s chassis=%s tableEntries=%d unit=%s",
                          describeUnitType(unitType), ratGenRating, weightTierIndex, workingCopy.getWeightClass(),
                          roleStrictness, workingCopy.getRoles(), workingCopy.getMovementModes(),
                          workingCopy.getModels(), workingCopy.getChassis(),
                          table.getNumEntries(), (mekSummary == null) ? "null" : mekSummary.getName()));
                }
                if (mekSummary != null) {
                    // Looked up once: the previous form called getModelRecord in both the condition and
                    // the return, so the returned record was not guaranteed to be the one just checked.
                    ModelRecord selectedModel = RATGenerator.getInstance().getModelRecord(mekSummary.getName());
                    if (selectedModel != null) {
                        LOGGER.debug("[ForceGen][Weight] generate() unitType={} requestedWeight={} weightTierIndex={}"
                                    + " tableWeight={} rating={} -> {} (unitWeightClass={})",
                              describeUnitType(unitType), weightClass, weightTierIndex,
                              workingCopy.getWeightClass(), ratGenRating, mekSummary.getName(),
                              mekSummary.getWeightClass());
                        return selectedModel;
                    }
                }

                if ((!useWeightClass() || weightTierIndex == 2) && !workingCopy.getRoles().isEmpty()) {
                    workingCopy.getRoles().clear();
                } else if ((!useWeightClass() || weightTierIndex == 1) && !workingCopy.getMovementModes().isEmpty()) {
                    workingCopy.getMovementModes().clear();
                } else {
                    if (useWeightClass() &&
                          null != weightClass &&
                          weightClass != -1 &&
                          weightClass < alternateWeights.length &&
                          weightTierIndex < alternateWeights[weightClass].length) {
                        workingCopy.setWeightClass(alternateWeights[weightClass][weightTierIndex]);
                    }
                    weightTierIndex++;
                }
            }
        }
        return null;
    }

    public void loadEntities(Ruleset.ProgressListener l, double progress) {
        if (element) {
            String modelName = getModelName();
            MekSummary ms = MekSummaryCache.getInstance().getMek(modelName);
            if (!chassis.isEmpty()) {
                // Chassis-only element (no model pinned via setUnit): resolved by chassis name.
                LOGGER.debug("[ForceGen][ChassisOnly] loadEntities chassis-only element: modelName='{}'"
                            + " chassis={} unitType={} -> getMek {}",
                      modelName, chassis, unitType,
                      (ms == null) ? "= NULL (NOT FOUND in cache)" : "= '" + ms.getName() + "'");
            }
            if (ms != null) {
                try {
                    entity = new MekFileParser(ms.getSourceFile(), ms.getEntryName()).getEntity();
                    entity.setCrew(getCo().createCrew(entity.defaultCrewType()));
                    entity.setExternalIdAsString(UUID.randomUUID().toString());
                    String forceString = getForceString();
                    entity.setForceString(forceString);
                    if (forceString.isBlank()) {
                        LOGGER.warn("[ForceGen][ToE] leaf '{}' has a BLANK force string; it will lose its "
                              + "ToE position (parent={})", entity.getShortName(), (parent == null ? "null" : "set"));
                    } else {
                        LOGGER.debug("[ForceGen][ToE] leaf '{}' forceString='{}'", entity.getShortName(), forceString);
                    }
                } catch (EntityLoadingException ex) {
                    LOGGER.error(ex, "Error loading {} from file {}", ms.getName(), ms.getSourceFile().getPath());
                }
            }
        }
        int count = subForces.size() + attached.size();
        subForces.forEach(fd -> fd.loadEntities(l, progress / count));
        attached.forEach(fd -> fd.loadEntities(l, progress / count));
        if (count == 0 && null != l) {
            l.updateProgress(progress, "Loading entities");
        }
    }

    /**
     * Generates a force string for exporting these units to MUL / adding to the game. The string is the
     * chain of ancestor forces, each as {@code name|id}, ordered from the top-level force down.
     *
     * <p>The id of each ancestor is its {@link #forceId}, a value made unique across the whole
     * generated force by {@link #assignForceIds(int)}. A previous implementation derived the id from
     * {@code 17 * id + index}, but {@code index} is not unique among siblings created by different
     * {@code <subforce>} / {@code <attachedForces>} blocks, so distinct forces collided on the same id
     * and the server merged them — armor / infantry / VTOL support detachments ended up inside the
     * wrong battalion.</p>
     */
    private String getForceString() {
        var ancestors = new ArrayList<ForceDescriptor>();
        ForceDescriptor p = parent;
        while (p != null) {
            ancestors.add(p);
            p = p.parent;
        }

        StringBuilder result = new StringBuilder();
        for (int i = ancestors.size() - 1; i >= 0; i--) {
            ForceDescriptor ancestor = ancestors.get(i);
            result.append(ancestor.getCombinedDisplayName()).append('|').append(ancestor.forceId).append("||");
        }
        return result.toString();
    }

    /**
     * Builds the display label for this force, combining the formal name (e.g. "A Company") with the weight + unit-type
     * + formation-type descriptor (e.g. "Heavy Mek Company") into a single string.
     *
     * <p>Matches the layout the {@code ForceGeneratorViewUi} tree renderer shows for the same node, so
     * what the user sees in the Force Generator preview matches what the lobby Force View shows after transfer. For
     * lances that lack an explicit name this is the only label available — without it the lobby would render them with
     * a blank name.</p>
     *
     * @return "Name (Descriptor)" when both sides are populated, otherwise whichever side is non-blank, or an empty
     *       string if neither is set.
     */
    public String getCombinedDisplayName() {
        String name = parseName();
        String description = getDescription();
        boolean hasName = name != null && !name.isBlank();
        boolean hasDescription = description != null && !description.isBlank();
        if (hasName && hasDescription) {
            return name + " (" + description + ")";
        }
        if (hasName) {
            return name;
        }
        if (hasDescription) {
            return description;
        }
        return "";
    }

    /**
     * Assigns a unique {@link #forceId} to every formation node in this subtree - that is, every
     * node from the lance/star/point level up. Leaf element nodes (the individual units) are skipped:
     * they become entities in the game, not forces, so they need no force id. Must be run after the
     * force tree is fully built and before {@link #loadEntities} so the force strings stamped onto
     * entities are collision-free.
     *
     * @param nextId the first id to assign
     *
     * @return the next unused id, so a caller can continue numbering a later subtree (e.g. transports)
     */
    public int assignForceIds(int nextId) {
        if (!element) {
            forceId = nextId++;
        }
        for (ForceDescriptor sub : subForces) {
            nextId = sub.assignForceIds(nextId);
        }
        for (ForceDescriptor attachedForce : attached) {
            nextId = attachedForce.assignForceIds(nextId);
        }
        return nextId;
    }

    public void assignCommanders() {
        subForces.forEach(ForceDescriptor::assignCommanders);

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
        // If none is found, assign crew without assigning rank or title.
        if (coNode == null) {
            setCo(new CrewDescriptor(this));
            return;
        }

        if (!subForces.isEmpty()) {
            int coPos = (coNode.getPosition() == null) ? 1 : Math.min(coNode.getPosition(), 1);
            int xoPos = 0;
            if (xoNode != null && (xoNode.getPosition() == null || xoNode.getPosition() > 0)) {
                xoPos = (xoNode.getPosition() == null) ? coPos + 1 : Math.max(coPos, xoNode.getPosition());
            }
            if (coPos + xoPos > 0) {
                ForceDescriptor[] forces = subForces.toArray(new ForceDescriptor[0]);
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
                    subForces.remove(coFound);
                    subForces.addFirst(coFound);
                }
                if (xoPos != 0) {
                    /*
                     * If the XO is a field officer, the position is assigned to the first sub force that doesn't
                     * contain the CO (which is the first if the CO is not a field officer). If the CO and XO
                     * positions are the same, the XO is assigned to the same sub force as the CO, but the second sub
                     * force of that one.
                     */
                    ForceDescriptor xoFound = getForceDescriptor(coPos, xoPos, xoNode);

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
        if (!element && !subForces.isEmpty()) {
            movementModes.clear();
            boolean isOmni = true;
            boolean isArtillery = true;
            boolean isMissileArtillery = true;
            boolean isFieldGun = true;
            for (ForceDescriptor fd : subForces) {
                movementModes.addAll(fd.getMovementModes());
                if ((fd.getUnitType() == null ||
                      !((UnitType.MEK == fd.getUnitType()) ||
                            (UnitType.AEROSPACE_FIGHTER == fd.getUnitType()) ||
                            (UnitType.TANK == fd.getUnitType()))) || !fd.getFlags().contains("omni")) {
                    isOmni = false;
                }
                if (!fd.getRoles().contains(MissionRole.MISSILE_ARTILLERY)) {
                    isMissileArtillery = false;
                }
                if (!fd.getRoles().contains(MissionRole.ARTILLERY) &&
                      !fd.getRoles().contains(MissionRole.MISSILE_ARTILLERY)) {
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
            for (ForceDescriptor sub : subForces) {
                if (sub.useWeightClass()) {
                    if (sub.getWeightClass() == null) {
                        LOGGER.error("Weight class == null for {} with {} sub-forces",
                              sub.getUnitType(),
                              sub.getSubForces().size());
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

    private @Nullable ForceDescriptor getForceDescriptor(int coPos, int xoPos, CommanderNode xoNode) {
        ForceDescriptor xoFound = null;
        ArrayList<ForceDescriptor> subForces = this.subForces;
        if (coPos == xoPos) {
            subForces = this.subForces.getFirst().getSubForces();
        }
        if (subForces.size() > coPos) {
            if (xoNode.getUnitType() != null) {
                for (int i = coPos; i < subForces.size(); i++) {
                    if (subForces.get(i).getUnitType() != null &&
                          (xoNode.getUnitType().equals(subForces.get(i).getUnitTypeName()) ||
                                (xoNode.getUnitType().equals("other") &&
                                      !subForces.get(i)
                                            .getUnitType()
                                            .equals(co.getAssignment().getUnitType())))) {
                        xoFound = subForces.get(i);
                        break;
                    }
                }
            }
            if (xoFound == null) {
                xoFound = subForces.get(1);
            }
        }
        return xoFound;
    }

    public void assignPositions() {
        int index = 0;
        HashMap<String, Integer> uniqueCount = new HashMap<>();
        for (int i = 0; i < subForces.size(); i++) {
            subForces.get(i).positionIndex = i + 1;
            if (subForces.get(i).name == null) {
                continue;
            }
            if (subForces.get(i).name.contains(":distinct}")) {
                if (uniqueCount.containsKey(subForces.get(i).name)) {
                    uniqueCount.put(subForces.get(i).name, uniqueCount.get(subForces.get(i).name) + 1);
                } else {
                    uniqueCount.put(subForces.get(i).name, 1);
                }
            } else if (subForces.get(i).name.matches(".*\\{[^:]*}.*")) {
                subForces.get(i).nameIndex = index++;
            }
        }
        HashMap<String, Integer> indexCount = new HashMap<>();
        for (ForceDescriptor sub : subForces) {
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

    /**
     * Divisor that turns a large craft's tonnage into a naval ranking term. Large craft span hundreds to millions of
     * tons, so the raw tonnage would swamp the single-digit experience and weight-class terms in {@code rank()}.
     * Bucketing by thousands keeps the term on the same scale while still ordering vessels heaviest-first. Sub-1000-ton
     * craft (small DropShips) collapse to 0 on purpose - they are never chosen as the command vessel.
     */
    private static final int TONS_PER_NAVAL_RANK_POINT = 1000;

    private final Comparator<? super ForceDescriptor> forceSorter = new Comparator<>() {
        /* Rank by difference in experience + difference in unit/echelon weights */
        private int rank(ForceDescriptor fd) {
            int retVal = 0;
            if (fd.getWeightClass() != null) {
                retVal += fd.getWeightClass();
            }
            // Large craft (WarShips/DropShips/JumpShips/Space Stations) have no L/M/H/A weight
            // class, so rank them by tonnage: the heaviest vessel in a naval star becomes its
            // command vessel (assignCommanders assigns the CO to forces[0]). The entity is not
            // loaded yet when commanders are assigned, so read tonnage from the model record.
            Integer largeCraftType = fd.getUnitType();
            if ((largeCraftType != null) && ((largeCraftType == UnitType.WARSHIP)
                  || (largeCraftType == UnitType.DROPSHIP) || (largeCraftType == UnitType.JUMPSHIP)
                  || (largeCraftType == UnitType.SPACE_STATION))) {
                ModelRecord modelRecord = RATGenerator.getInstance().getModelRecord(fd.getModelName());
                if ((modelRecord != null) && (modelRecord.getMekSummary() != null)) {
                    retVal += (int) (modelRecord.getMekSummary().getTons() / TONS_PER_NAVAL_RANK_POINT);
                }
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
     * Calculates transport needs of the unit and generates enough drop-ships and jump ships to carry the indicated
     * portion of the unit.
     */
    public ForceDescriptor assignTransport() {
        if ((getDropshipPct() <= 0) && (getJumpshipPct() <= 0)
              && (getWarshipPct() <= 0) && (getCargo() <= 0)) {
            return null;
        }
        TransportCalculator tp = new TransportCalculator(this);
        List<MekSummary> dropships = tp.calcDropships(getDropshipPct());
        List<MekSummary> warships = tp.calcWarShips(getWarshipPct(), dropships.size());
        List<MekSummary> jumpships = tp.calcJumpShips(getJumpshipPct(), dropships.size());

        FactionRecord factionRec = getFactionRec();
        boolean isClan = (factionRec != null) && factionRec.isClan();

        ForceDescriptor transports = createChild(subForces.size() + attached.size());
        transports.setUnitType(null);
        // "Naval Units" is the top container in both Clan and IS trees. Under it, each ship type
        // gets a category node (WarShip Stars / WarShips, etc.) holding the per-type hierarchy:
        //   Clan: Stars of 5 vessels each
        //   IS/Periphery/SLDF: Strategic Operations hierarchy — Flotilla (2) / Division (3 Flotillas) / Squadron (3 Divisions)
        transports.setName("Naval Units");
        // TODO: put this in the faction files
        transports.setEchelon(isClan ? 7 : 9);
        transports.setCoRank(35);

        // Always render the categories in the canonical order: WarShips first, then JumpShips, then DropShips.
        if (isClan) {
            addClanCategory(transports, warships, "WarShip Stars");
            addClanCategory(transports, jumpships, "JumpShip Stars");
            addClanCategory(transports, dropships, "DropShip Stars");
        } else {
            addISCategory(transports, warships, "WarShips");
            addISCategory(transports, jumpships, "JumpShips");
            addISCategory(transports, dropships, "DropShips");
        }

        transports.assignCommanders();
        transports.assignPositions();

        return transports;
    }

    /**
     * Creates a Clan category wrapper node (e.g., "WarShip Stars") and populates it with Stars of 5 vessels each.
     * If the ship list is empty the category is skipped entirely so the tree stays clean.
     */
    private void addClanCategory(ForceDescriptor parent, List<MekSummary> ships, String categoryName) {
        if (ships.isEmpty()) {
            return;
        }
        ForceDescriptor category = createGroupNode(parent, categoryName,
              /* echelon = CLUSTER */ 6, /* coRank = STAR_COL */ 38);
        addClanStars(category, ships);
    }

    /**
     * Creates an IS category wrapper node (e.g., "WarShips") and populates it with the Strategic Operations naval
     * hierarchy. If the ship list is empty the category is skipped entirely so the tree stays clean.
     */
    private void addISCategory(ForceDescriptor parent, List<MekSummary> ships, String categoryName) {
        if (ships.isEmpty()) {
            return;
        }
        ForceDescriptor category = createGroupNode(parent, categoryName,
              /* echelon = DIVISION */ 8, /* coRank = MAJ_GENERAL */ 42);
        addNavalHierarchy(category, ships);
    }

    /**
     * Adds Clan-style Star groupings (5 vessels per Star) under the given parent category node. Stars are named
     * "Alpha Star", "Bravo Star", … per Clan convention (phonetic identifier precedes the formation level). When
     * only a single Star is generated, the phonetic is omitted and the node is simply named "Star".
     *
     * @param parent The category node that will receive the Star(s) as subforces
     * @param ships  The ships to add to this category (must be non-empty; callers should pre-filter)
     */
    private void addClanStars(ForceDescriptor parent, List<MekSummary> ships) {
        final int starSize = 5;
        int totalStars = (ships.size() + starSize - 1) / starSize;
        for (int starIndex = 0; starIndex < totalStars; starIndex++) {
            String groupName = (totalStars > 1)
                  ? PHONETIC[Math.min(starIndex, PHONETIC.length - 1)] + " Star"
                  : "Star";
            ForceDescriptor star = createGroupNode(parent, groupName, /* echelon = STAR */ 3,
                  /* coRank = STAR_CMDR */ 32);
            int start = starIndex * starSize;
            int end = Math.min(start + starSize, ships.size());
            for (int shipIndex = start; shipIndex < end; shipIndex++) {
                addShipElement(star, ships.get(shipIndex));
            }
        }
    }

    /**
     * Adds Inner Sphere / SLDF naval hierarchy groupings per Strategic Operations under the given category node:
     * <ul>
     *   <li>Flotilla = 2 vessels</li>
     *   <li>Division = 3 Flotillas (6 vessels)</li>
     *   <li>Squadron = 3 Divisions (18 vessels)</li>
     * </ul>
     * Picks the minimum nesting depth that fits the ship count: 1-2 ships render as a single Flotilla,
     * 3-6 as Flotillas under one Division, 7-18 as Divisions under one or more Squadrons, 19+ as multiple Squadrons.
     * The ship type is conveyed by the parent category node, so inner-level names are unprefixed
     * (e.g., "Flotilla Alpha" rather than "WarShip Flotilla Alpha").
     *
     * @param parent The category node ("WarShips" / "JumpShips" / "DropShips") that receives the hierarchy
     * @param ships  The ships to add (must be non-empty; callers should pre-filter)
     */
    private void addNavalHierarchy(ForceDescriptor parent, List<MekSummary> ships) {
        // Slice into Flotillas of 2.
        List<List<MekSummary>> flotillas = new ArrayList<>();
        for (int i = 0; i < ships.size(); i += 2) {
            flotillas.add(ships.subList(i, Math.min(i + 2, ships.size())));
        }

        if (flotillas.size() <= 1) {
            // 1-2 ships: one Flotilla directly under category
            addFlotilla(parent, flotillas.get(0), null);
        } else if (flotillas.size() <= 3) {
            // 3-6 ships: one Division of Flotillas
            ForceDescriptor division = createGroupNode(parent, "Division",
                  /* echelon = BRIGADE */ 7, /* coRank = LT_COLONEL */ 37);
            for (int f = 0; f < flotillas.size(); f++) {
                addFlotilla(division, flotillas.get(f), PHONETIC[f]);
            }
        } else if (flotillas.size() <= 9) {
            // 7-18 ships: multiple Divisions, no Squadron wrapper needed
            int numDivisions = (flotillas.size() + 2) / 3;
            for (int d = 0; d < numDivisions; d++) {
                ForceDescriptor division = createGroupNode(parent, "Division " + PHONETIC[d],
                      /* echelon = BRIGADE */ 7, /* coRank = LT_COLONEL */ 37);
                int startF = d * 3;
                int endF = Math.min(startF + 3, flotillas.size());
                for (int f = startF; f < endF; f++) {
                    addFlotilla(division, flotillas.get(f), PHONETIC[f - startF]);
                }
            }
        } else {
            // 19+ ships: full hierarchy with Squadrons of Divisions of Flotillas
            int numSquadrons = (flotillas.size() + 8) / 9;
            for (int s = 0; s < numSquadrons; s++) {
                String squadronName = (numSquadrons > 1) ? "Squadron " + PHONETIC[s] : "Squadron";
                ForceDescriptor squadron = createGroupNode(parent, squadronName,
                      /* echelon = DIVISION */ 8, /* coRank = COLONEL */ 38);
                int startF = s * 9;
                int endF = Math.min(startF + 9, flotillas.size());
                int divisionsInSquadron = (endF - startF + 2) / 3;
                for (int d = 0; d < divisionsInSquadron; d++) {
                    ForceDescriptor division = createGroupNode(squadron, "Division " + PHONETIC[d],
                          /* echelon = BRIGADE */ 7, /* coRank = LT_COLONEL */ 37);
                    int divStartF = startF + d * 3;
                    int divEndF = Math.min(divStartF + 3, flotillas.size());
                    for (int f = divStartF; f < divEndF; f++) {
                        addFlotilla(division, flotillas.get(f), PHONETIC[f - divStartF]);
                    }
                }
            }
        }
    }

    /**
     * Creates an intermediate force-tree node for a transport sub-grouping (Star, Squadron, Division, Flotilla)
     * and attaches it to the parent.
     */
    private ForceDescriptor createGroupNode(ForceDescriptor parent, String name, int echelon, int coRank) {
        ForceDescriptor group = parent.createChild(parent.getSubForces().size());
        group.setUnitType(null);
        group.setName(name);
        group.setEchelon(echelon);
        group.setCoRank(coRank);
        parent.addSubForce(group);
        return group;
    }

    /**
     * Adds a Flotilla node (2 vessels) under the given parent and appends its vessels as element children.
     * Suffix is appended to the Flotilla name only when non-null (e.g., when there are multiple Flotillas at the
     * same level under the same Division).
     */
    private void addFlotilla(ForceDescriptor parent, List<MekSummary> flotillaShips, @Nullable String suffix) {
        String name = "Flotilla" + (suffix != null ? " " + suffix : "");
        ForceDescriptor flotilla = createGroupNode(parent, name,
              /* echelon = REGIMENT */ 6, /* coRank = MAJOR */ 35);
        for (MekSummary ms : flotillaShips) {
            addShipElement(flotilla, ms);
        }
    }

    /**
     * Adds an element-level (echelon 1) child for an individual vessel.
     */
    private void addShipElement(ForceDescriptor parent, MekSummary ms) {
        ForceDescriptor sub = parent.createChild(parent.getSubForces().size());
        sub.setUnit(RATGenerator.getInstance().getModelRecord(ms.getName()));
        sub.setEchelon(1);
        sub.setCoRank(33);
        parent.addSubForce(sub);
    }

    public static int decodeWeightClass(String code) {
        return switch (code) {
            case "UL" -> EntityWeightClass.WEIGHT_ULTRA_LIGHT;
            case "L" -> EntityWeightClass.WEIGHT_LIGHT;
            case "M" -> EntityWeightClass.WEIGHT_MEDIUM;
            case "H" -> EntityWeightClass.WEIGHT_HEAVY;
            case "A" -> EntityWeightClass.WEIGHT_ASSAULT;
            case "SH", "C" -> EntityWeightClass.WEIGHT_COLOSSAL;
            default -> -1;
        };
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
                    ut == UnitType.AEROSPACE_FIGHTER ||
                    ut == UnitType.TANK ||
                    ut == UnitType.BATTLE_ARMOR);
    }

    /**
     * Weight class can differ from the target once units are generated. Weight class is recalculated based on actual
     * units present and echelon name is set.
     *
     * @return The weight class of this force node
     */
    public double recalcWeightClass() {
        double wc;
        if (!subForces.isEmpty()) {
            wc = subForces.stream().mapToDouble(ForceDescriptor::recalcWeightClass).sum() / subForces.size();
        } else if (null != weightClass && weightClass >= 0) {
            wc = weightClass;
        } else {
            wc = EntityWeightClass.WEIGHT_MEDIUM;
        }
        Integer rolledWeightClass = weightClass; // the picker's intended weight, before the overwrite
        weightClass = (int) Math.round(wc);

        // Resolve the name against the INTENDED (rolled) weight class, not the recalculated average,
        // so a formation keeps its doctrinal type: an Assault Cluster whose units average out to
        // Heavy is still named "Assault Cluster" rather than "Battle Cluster". This matters for
        // weight-skewed factions (e.g. Clan Coyote) where every cluster averages Heavy and the
        // recalculated weight would collapse all names to one type. Falls back to the recalculated
        // weight when the picker never set one (rolledWeightClass null/unset).
        if (null != nameNodes) {
            int recalculatedWeightClass = weightClass;
            if ((rolledWeightClass != null) && (rolledWeightClass >= 0)) {
                weightClass = rolledWeightClass;
            }
            for (ValueNode n : nameNodes) {
                if (n.matches(this)) {
                    setName(n.getContent());
                    break;
                }
            }
            weightClass = recalculatedWeightClass;
        }
        attached.forEach(ForceDescriptor::recalcWeightClass);

        return wc;
    }

    public ArrayList<Object> getAllChildren() {
        ArrayList<Object> retVal = new ArrayList<>();
        retVal.addAll(subForces);
        retVal.addAll(attached);
        return retVal;
    }

    /**
     * Recursively counts the weight class of every BattleMek leaf element in this descriptor, its
     * subforces, and its attachments. Diagnostic helper for verifying that a requested force
     * weight (e.g. an Assault regiment) actually produced the expected unit mix — compare the
     * returned counts against the per-faction subforce tables in the ruleset XML.
     *
     * <p>LandAirMeks are counted as Meks ({@code Entity.isMek()} is true for them). Non-Mek
     * elements (vehicles, infantry, fighters) are ignored.</p>
     *
     * @return an int array indexed by {@link EntityWeightClass} constant
     *       ({@code 0 = WEIGHT_ULTRA_LIGHT} … {@code 5 = WEIGHT_SUPER_HEAVY}); each slot holds the
     *       number of Mek elements at that weight class
     */
    public int[] tallyMekWeightClasses() {
        int[] counts = new int[EntityWeightClass.WEIGHT_SUPER_HEAVY + 1];
        tallyMekWeightClasses(counts);
        return counts;
    }

    private void tallyMekWeightClasses(int[] counts) {
        Entity leafEntity = getEntity();
        if (leafEntity != null && leafEntity.isMek()) {
            int leafWeightClass = leafEntity.getWeightClass();
            if (leafWeightClass >= 0 && leafWeightClass < counts.length) {
                counts[leafWeightClass]++;
            }
        }
        for (ForceDescriptor sub : subForces) {
            sub.tallyMekWeightClasses(counts);
        }
        for (ForceDescriptor attachedForce : attached) {
            attachedForce.tallyMekWeightClasses(counts);
        }
    }

    /**
     * Tallies every generated element's weight class, grouped by unit type. Like {@link #tallyMekWeightClasses()} but
     * for all of the weight-classed types the budget allocator governs (Mek, aerospace fighter, vehicle, battle armor),
     * so each type's achieved mix can be measured and tuned independently.
     *
     * @return a map from {@link UnitType} constant to a per-weight-class count array, indexed by
     *       {@link EntityWeightClass} ({@code 0 = WEIGHT_ULTRA_LIGHT} ... {@code 5 = WEIGHT_SUPER_HEAVY})
     */
    public Map<Integer, int[]> tallyWeightClassesByType() {
        Map<Integer, int[]> byType = new HashMap<>();
        tallyWeightClassesByType(byType);
        return byType;
    }

    private void tallyWeightClassesByType(Map<Integer, int[]> byType) {
        Entity leafEntity = getEntity();
        if (leafEntity != null) {
            int leafUnitType = leafEntity.getUnitType();
            if ((leafUnitType == UnitType.MEK) || (leafUnitType == UnitType.AEROSPACE_FIGHTER)
                  || (leafUnitType == UnitType.TANK) || (leafUnitType == UnitType.BATTLE_ARMOR)) {
                int leafWeightClass = leafEntity.getWeightClass();
                if ((leafWeightClass >= 0) && (leafWeightClass <= EntityWeightClass.WEIGHT_SUPER_HEAVY)) {
                    byType.computeIfAbsent(leafUnitType,
                          key -> new int[EntityWeightClass.WEIGHT_SUPER_HEAVY + 1])[leafWeightClass]++;
                }
            }
        }
        for (ForceDescriptor sub : subForces) {
            sub.tallyWeightClassesByType(byType);
        }
        for (ForceDescriptor attachedForce : attached) {
            attachedForce.tallyWeightClassesByType(byType);
        }
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
            String echelonName = Ruleset.findRuleset(this).getEschelonName(this);
            if (echelonName == null) {
                return "";
            }
            retVal = "{ordinal} " + echelonName;
        }
        if (getParent() != null && getParent().getNameIndex() >= 0) {
            retVal = retVal.replace("{ordinal:parent}", ORDINALS[getParent().getNameIndex()]);
            retVal = retVal.replace("{greek:parent}", GREEK[getParent().getNameIndex()]);
            retVal = retVal.replace("{phonetic:parent}", PHONETIC[getParent().getNameIndex()]);
            retVal = retVal.replace("{latin:parent}", LATIN[getParent().getNameIndex()]);
            retVal = retVal.replace("{roman:parent}", ROMAN[getParent().getNameIndex()]);
            retVal = retVal.replace("{cardinal:parent}", Integer.toString(getParent().getNameIndex() + 1));
            retVal = retVal.replace("{cardinalOrdinal:parent}", cardinalOrdinal(getParent().getNameIndex() + 1));
            retVal = retVal.replace("{alpha:parent}", Character.toString((char) (getParent().getNameIndex() + 'A')));
        }
        if (getParent() != null && retVal.contains("{name:parent}")) {
            String parentName = getParent().getName().replaceAll(".*\\[", "").replaceAll("].*", "");
            retVal = retVal.replace("{name:parent}", parentName);
        }
        if (nameIndex < 0) {
            retVal = retVal.replaceAll("\\{.*?}\\s?", "");
        } else {
            retVal = retVal.replace("{ordinal}", ORDINALS[getNameIndex()]);
            retVal = retVal.replace("{greek}", GREEK[getNameIndex()]);
            retVal = retVal.replace("{phonetic}", PHONETIC[getNameIndex()]);
            retVal = retVal.replace("{latin}", LATIN[getNameIndex()]);
            retVal = retVal.replace("{roman}", ROMAN[getNameIndex()]);
            retVal = retVal.replace("{cardinal}", Integer.toString(getNameIndex() + 1));
            retVal = retVal.replace("{cardinalOrdinal}", cardinalOrdinal(getNameIndex() + 1));
            retVal = retVal.replace("{alpha}", Character.toString((char) (getNameIndex() + 'A')));
            if (retVal.contains("{formation}")) {
                if (null != formationType && null != formationType.getCategory()) {
                    retVal = retVal.replace("{formation}",
                          formationType.getCategory()
                                .replace("Striker/Cavalry", "Striker")
                                .replace(" Squadron", ""));
                } else {
                    retVal = retVal.replace("{formation} ", "");
                }
            }
        }
        retVal = retVal.replaceAll("\\{.*?}", "");
        retVal = retVal.replaceAll("[\\[\\]]", "").replaceAll("\\s+", " ");
        return retVal.trim();
    }

    /**
     * Formats a positive integer as a numeric ordinal with the correct English suffix:
     * 1 -> "1st", 2 -> "2nd", 3 -> "3rd", 4 -> "4th", 11/12/13 -> "th", 21 -> "21st", etc.
     * Used by the {@code {cardinalOrdinal}} name token so cluster names read like the canon
     * Touman ("38th Assault Cluster", "202nd Battle Cluster") with no upper bound, unlike the
     * spelled {@code {ordinal}} token which stops at "Tenth".
     */
    public static String cardinalOrdinal(int n) {
        int mod100 = n % 100;
        String suffix;
        if (mod100 >= 11 && mod100 <= 13) {
            suffix = "th";
        } else {
            suffix = switch (n % 10) {
                case 1 -> "st";
                case 2 -> "nd";
                case 3 -> "rd";
                default -> "th";
            };
        }
        return n + suffix;
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
        String echelonName = null;

        while (echelonName == null && rules != null) {
            echelonName = rules.getEschelonName(this);
            if (echelonName == null) {
                if (rules.getParent() == null) {
                    rules = null;
                } else {
                    rules = Ruleset.findRuleset(rules.getParent());
                }
            }
        }

        if (echelonName != null) {
            retVal.append(" ").append(echelonName);
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

    public Integer getEchelon() {
        return echelon;
    }

    public String getEchelonCode() {
        String retVal = echelon.toString();
        if (augmented) {
            retVal += "^";
        }
        return retVal;
    }

    public void setEchelon(Integer echelon) {
        this.echelon = echelon;
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

    /** Per-cluster-type weight budget for this node, keyed by unit type, or {@code null} if none. */
    public Map<Integer, WeightTarget> getWeightTargets() {
        return weightTargets;
    }

    public void setWeightTargets(Map<Integer, WeightTarget> weightTargets) {
        this.weightTargets = weightTargets;
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
     * Translates between the rating codes used by the force generator and those used by the RAT Generator. The force
     * generator uses abbreviations to make the formation rules more concise.
     *
     * @return The RATGenerator rating code corresponding to the same index as the force generator rating code.
     */
    public String ratGeneratorRating() {
        FactionRecord fRec = getFactionRec();
        if ((null != fRec) &&
              !fRec.getRatingLevels().contains(rating) &&
              (getRatingLevel() >= 0) &&
              !fRec.getRatingLevels().isEmpty()) {
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
        if (models.size() == 1) {
            return models.iterator().next();
        }
        // Chassis-only fallback: a unit pinned to a single chassis with no model resolved - e.g. a
        // named WarShip referenced by chassis for a faction that has no warship availability table,
        // so the RAT ladder in generate() cannot supply a model. The chassis of a unique hull (like
        // a WarShip) is its full unit name, so loadEntities can resolve it directly from the cache.
        if (models.isEmpty() && chassis.size() == 1) {
            return chassis.iterator().next();
        }
        return "";
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
     * Because some echelon names depend on knowing the actual weight class, we save a copy of the possibilities for
     * this node and defer selection until after the final weight class determination.
     *
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

    public ArrayList<ForceDescriptor> getSubForces() {
        return subForces;
    }

    public void setSubForces(ArrayList<ForceDescriptor> subForces) {
        this.subForces = subForces;
    }

    public void addSubForce(ForceDescriptor fd) {
        subForces.add(fd);
        fd.setParent(this);
    }

    public ArrayList<ForceDescriptor> getAttached() {
        return attached;
    }

    public void setAttached(ArrayList<ForceDescriptor> attached) {
        this.attached = attached;
    }

    public void addAttached(ForceDescriptor forceDescriptor) {
        attached.add(forceDescriptor);
        // Set the back-reference so getForceString() walks an attached support force up through its
        // parent force; without this the attached force restarts the force string at the top level
        // and is rendered as a separate force instead of nesting under its parent.
        forceDescriptor.setParent(this);
    }

    public boolean isFighterComplement() {
        return fighterComplement;
    }

    public void setFighterComplement(boolean fighterComplement) {
        this.fighterComplement = fighterComplement;
    }

    /**
     * Generates the carried Aerospace Fighter complement of every large craft (WarShip, DropShip, JumpShip, Space
     * Station) in this force and nests it under the carrying ship, so a generated force that includes a carrier also
     * includes the fighters it carries. Each carrier is filled to its ASF bay capacity.
     *
     * <p>Run after unit generation but BEFORE commander/force-id/entity assignment, so the normal passes give the new
     * fighters crews, ids, and entities. Fighters are added via {@link #addAttached(ForceDescriptor)} rather than as
     * subforces so the carrier keeps its own crew (assignCommanders only reassigns from subforces), while the ToE still
     * nests them under the ship.</p>
     */
    public void addFighterComplement() {
        List<ForceDescriptor> carriers = new ArrayList<>();
        collectCarriers(carriers);
        for (ForceDescriptor carrier : carriers) {
            MekSummary carrierSummary = MekSummaryCache.getInstance().getMek(carrier.getModelName());
            if (carrierSummary == null) {
                continue;
            }
            int capacity = TransportCalculator.fighterBayCapacity(carrierSummary);
            if (capacity <= 0) {
                continue;
            }
            UnitTable table = UnitTable.findTable(carrier.getFactionRec(),
                  UnitType.AEROSPACE_FIGHTER,
                  carrier.getYear(),
                  carrier.getRating(),
                  null,
                  ModelRecord.NETWORK_NONE,
                  EnumSet.noneOf(EntityMovementMode.class),
                  EnumSet.noneOf(MissionRole.class),
                  0);
            // Organize the complement into Clan Stars of Points / IS Squadrons of Flights, each
            // attached under the carrier so the ToE reads: Ship -> Star/Squadron -> Point/Flight ->
            // fighters. The two fighters in a Point (Clan) or Flight (IS) are the SAME model: a Point
            // is a matched pair. Different Points within a Star may differ. A Clan aero Star is 5
            // Points of 2 = 10; an IS aero Squadron is 3 Flights of 2 = 6.
            boolean clan = (carrier.getFactionRec() != null) && carrier.getFactionRec().isClan();
            int pointSize = 2;
            int pointsPerGroup = clan ? 5 : 3;
            int groupSize = pointsPerGroup * pointSize;
            String groupLabel = clan ? "Star" : "Squadron";
            String pointLabel = clan ? "Point" : "Flight";
            int groupEchelon = clan ? 3 : 4;
            int pointEchelon = clan ? 2 : 3;
            int totalGroups = (capacity + groupSize - 1) / groupSize;

            int generated = 0;
            boolean exhausted = false;
            for (int groupIndex = 0; (groupIndex < totalGroups) && !exhausted; groupIndex++) {
                String groupName = (totalGroups > 1)
                      ? PHONETIC[Math.min(groupIndex, PHONETIC.length - 1)] + " " + groupLabel
                      : groupLabel;
                ForceDescriptor group = carrier.createChild(carrier.getAttached().size());
                group.getModels().clear();
                group.getChassis().clear();
                group.setUnitType(UnitType.AEROSPACE_FIGHTER);
                group.setName(groupName);
                group.setEchelon(groupEchelon);
                group.setCoRank(32);
                carrier.addAttached(group);

                int groupTarget = Math.min(groupSize, capacity - generated);
                int producedInGroup = 0;
                int pointIndex = 0;
                while (producedInGroup < groupTarget) {
                    // One model per Point: both fighters in the Point share it.
                    MekSummary fighterSummary = table.generateUnit();
                    if (fighterSummary == null) {
                        exhausted = true;
                        break;
                    }
                    // Keep the FIRST Point as a subForce so the Star inherits its commander
                    // (assignCommanders sets a force's CO from its lead subForce). Attach the rest:
                    // the CO-reorder only sorts SUBFORCES, so with one subForce nothing scrambles, and
                    // getAllChildren() (subForces + attached) still nests every Point in creation order.
                    ForceDescriptor point = group.createChild(pointIndex);
                    point.getModels().clear();
                    point.getChassis().clear();
                    point.setUnitType(UnitType.AEROSPACE_FIGHTER);
                    point.setName(pointLabel + " " + (pointIndex + 1));
                    point.setEchelon(pointEchelon);
                    point.setCoRank(16);
                    if (pointIndex == 0) {
                        group.addSubForce(point);
                    } else {
                        group.addAttached(point);
                    }
                    pointIndex++;

                    int pointTarget = Math.min(pointSize, groupTarget - producedInGroup);
                    for (int fighterIndex = 0; fighterIndex < pointTarget; fighterIndex++) {
                        ForceDescriptor fighter = point.createChild(point.getSubForces().size());
                        fighter.setUnitType(UnitType.AEROSPACE_FIGHTER);
                        fighter.setUnit(RATGenerator.getInstance().getModelRecord(fighterSummary.getName()));
                        fighter.setEchelon(1);
                        fighter.setCoRank(31);
                        point.addSubForce(fighter);
                        producedInGroup++;
                        generated++;
                    }
                }
                // Drop a group that produced nothing because the table dried up. Points live in
                // both lists now (first = subForce, rest = attached), so check both.
                if (group.getSubForces().isEmpty() && group.getAttached().isEmpty()) {
                    carrier.getAttached().remove(group);
                }
            }
        }
    }

    /** Recursively collects every large-craft element (carrier) in the tree. */
    private void collectCarriers(List<ForceDescriptor> carriers) {
        if (isElement() && (unitType != null) && ((unitType == UnitType.WARSHIP)
              || (unitType == UnitType.DROPSHIP) || (unitType == UnitType.JUMPSHIP)
              || (unitType == UnitType.SPACE_STATION))) {
            carriers.add(this);
            return;
        }
        for (ForceDescriptor sub : subForces) {
            sub.collectCarriers(carriers);
        }
        for (ForceDescriptor attachedForce : attached) {
            attachedForce.collectCarriers(carriers);
        }
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

    public double getWarshipPct() {
        return warshipPct;
    }

    public void setWarshipPct(double warshipPct) {
        this.warshipPct = warshipPct;
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
        subForces.forEach(sf -> sf.addAllEntities(list));
        attached.forEach(sf -> sf.addAllEntities(list));
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

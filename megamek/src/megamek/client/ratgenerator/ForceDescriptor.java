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

    /**
     * The {@code generate} rules that ask for one unit across a whole {@code <subforces>} block rather
     * than a unit chosen for each child, which is what makes a matched pair or a uniform company.
     */
    private static final Set<String> SHARED_UNIT_RULES = Set.of("model", "chassis");

    public static final int EXP_GREEN = 0;
    public static final int EXP_REGULAR = 1;
    public static final int EXP_VETERAN = 2;
    /**
     * Elite. Declared for completeness: the value was already produced by the Experience Target picker
     * and indexed the elite row of the skill tables, but had no constant, so the range these values
     * cover could only be discovered by reading {@link CrewDescriptor}'s tables.
     */
    public static final int EXP_ELITE = 3;

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

    /**
     * Echelons for the naval hierarchy, nesting Flotilla &lt; Division &lt; Squadron &lt; Fleet under a
     * ship-type category. They deliberately sit above the ground echelons a combat force uses, so a
     * naval formation is never mistaken for a ground one of the same depth - a Flotilla used to be
     * declared at the Regiment echelon, which had it picked up by rules meant for ground regiments.
     */
    private static final int ECHELON_NAVAL_FLOTILLA = 5;
    private static final int ECHELON_NAVAL_DIVISION = 6;
    private static final int ECHELON_NAVAL_SQUADRON = 7;
    private static final int ECHELON_NAVAL_FLEET = 8;
    private static final int ECHELON_NAVAL_CATEGORY = 9;
    private static final int ECHELON_NAVAL_ROOT = 10;

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
    // The formation types this node's rule actually offered, mapped to the weight the ruleset gave each. Recorded
    // when the formation rule fires, before the weighted pick, so it describes the choice rather than its outcome.
    // A node with one entry had no choice to make (a command lance, say); a node with none was never offered one.
    private Map<String, Integer> eligibleFormations = Map.of();
    // The requested distribution of formation types, set on the root from the Force Generator's controls. Empty means
    // no override, which reproduces generation exactly as it was before the mix existed. Deliberately not copied to
    // children: the allocator reads it once from the root and works down from there.
    private FormationMix formationMix = FormationMix.EMPTY;
    // What the mix actually achieved, set on the root by the allocator. Null when no mix was applied.
    private FormationMixReport formationMixReport;
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
    /**
     * How much of the command's cargo requirement to provision hauling for, as a percentage.
     * 100 covers everything it has to carry; above 100 buys headroom for cargo picked up later.
     * The ships themselves are generated by the consumer once the force exists and its real cargo
     * load is known, so this is carried through generation rather than acted on during it.
     */
    private double cargoPct = 100.0;
    private boolean fighterComplement = false;

    // Preview-time flag (not persisted): hosts such as MekHQ let the user exclude nodes from the
    // generated force in the preview tree. Defaults to included; see setIncludedRecursively.
    private boolean included = true;

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
                generateFormationByBlock();
            } else {
                // Each <subforces> block tagged the children it produced with its own generate rule,
                // so a node holding several of them honours each in turn. A node with one block yields
                // one group and behaves exactly as it did when the rule was read off the node itself.
                Map<String, List<ForceDescriptor>> byBlockRule = subForces.stream()
                      .filter(sub -> null != sub.getGenerationRule())
                      .collect(Collectors.groupingBy(ForceDescriptor::getGenerationRule));
                if (!byBlockRule.isEmpty()) {
                    byBlockRule.forEach(this::generateByRule);
                } else if (null != generationRule) {
                    // No child carries a rule, so fall back to the node's own - an older shape, and
                    // what an attached force still looks like.
                    generateByRule(generationRule, subForces);
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

    /**
     * Generates one group of children under the rule their {@code <subforces>} block declared.
     *
     * <p>Applied per block rather than per node so a mixed formation can build its groups by different
     * rules - a Level II generating its Meks as a group while its aerospace pair share one model, which
     * is what makes the two fighters identical.</p>
     *
     * @param rule    the block's generate rule
     * @param members the children that block produced
     */
    /**
     * Generates a node that has been given a formation type, letting each {@code <subforces>} block
     * that asked for a shared unit be built by its own rule.
     *
     * <p>A formation is by nature a set of different units chosen to work together, so its members are
     * picked individually. A block asking for {@code model} or {@code chassis} is asking for the
     * opposite - one unit across the block - and is therefore built separately and left out of the
     * formation, which is what lets a Level II field a Mek formation and a matched fighter pair at
     * once. Blocks asking for {@code group}, and blocks asking for nothing, are the formation.</p>
     *
     * <p>Where every block asks for a shared unit there is no formation left to build. That is a node
     * whose single block carries the rule, which has always meant "build the formation and pin its
     * pick to each child", so it goes on meaning that.</p>
     */
    private void generateFormationByBlock() {
        FormationSplit split = splitForFormation(subForces);
        if (split.formationMembers().isEmpty()) {
            buildFormation(subForces, "chassis".equals(generationRule));
            return;
        }
        split.sharedUnitBlocks().forEach((rule, members) -> {
            LOGGER.debug("[ForceGen][GenRule] '{}': {} child(ren) generate by '{}', outside the {}"
                        + " formation", parseName(), members.size(), rule, formationType);
            shareOneUnitAcross(rule, members);
        });
        buildFormation(split.formationMembers(), false);
    }

    /**
     * How a formation-typed node's children divide between the formation and the blocks generated
     * apart from it.
     *
     * @param sharedUnitBlocks the children of each block asking for one unit across the block, keyed by
     *                         that block's rule
     * @param formationMembers the children the formation itself is built from
     */
    record FormationSplit(Map<String, List<ForceDescriptor>> sharedUnitBlocks,
                          List<ForceDescriptor> formationMembers) {
    }

    /**
     * Divides a formation-typed node's children by what their {@code <subforces>} block asked for.
     *
     * <p>Package-private so the split can be tested on its own: which child goes where is the whole of
     * the decision, and testing it through generation would need the unit tables and a die roll.</p>
     *
     * @param subs the node's children, each tagged with its block's rule or with none
     *
     * @return the division of those children
     */
    static FormationSplit splitForFormation(List<ForceDescriptor> subs) {
        Map<String, List<ForceDescriptor>> sharedUnitBlocks = new LinkedHashMap<>();
        List<ForceDescriptor> formationMembers = new ArrayList<>();
        for (ForceDescriptor sub : subs) {
            String rule = sub.getGenerationRule();
            // A block declaring no rule leaves its children with a null one, and an immutable Set
            // throws rather than answering contains(null), so the null case is settled first.
            boolean sharesOneUnit = (rule != null) && SHARED_UNIT_RULES.contains(rule);
            if (sharesOneUnit) {
                sharedUnitBlocks.computeIfAbsent(rule, key -> new ArrayList<>()).add(sub);
            } else {
                formationMembers.add(sub);
            }
        }
        return new FormationSplit(sharedUnitBlocks, formationMembers);
    }

    /**
     * Builds the node's formation from the given members, falling back to an ordinary lance when the
     * formation's requirements cannot be met.
     *
     * @param members    the children the formation is to be made of
     * @param pinChassis whether a non-leaf child is pinned to the chassis rather than the exact model
     */
    private void buildFormation(List<ForceDescriptor> members, boolean pinChassis) {
        // In cases like Novas and air lances the formation rules only apply to some of the units.
        if (!generateAndAssignFormation(members, pinChassis, 0)) {
            LOGGER.debug("[ForceGen][GenRule] '{}': {} could not be fulfilled by {} child(ren);"
                        + " generating them as an ordinary lance", parseName(), formationType,
                  members.size());
            generateLance(members);
            formationType = null;
        }
    }

    private void generateByRule(String rule, List<ForceDescriptor> members) {
        if (members.isEmpty()) {
            return;
        }
        LOGGER.debug("[ForceGen][GenRule] '{}': generating {} child(ren) by '{}'",
              parseName(), members.size(), rule);
        switch (rule) {
            case "group" -> generateLance(members);
            // One unit picked for the whole block and pinned to every member, so the block comes out
            // uniform. Members that already carry a pick are left alone, an ancestor having set it.
            case "model", "chassis" -> shareOneUnitAcross(rule, members);
            default -> LOGGER.warn("[ForceGen][GenRule] '{}': unknown generate rule '{}'; ignored",
                  parseName(), rule);
        }
    }

    /**
     * Picks a single unit for the group and pins it to every member.
     *
     * @param rule    {@code chassis} to share only the chassis, otherwise the exact model
     * @param members the children to make uniform
     */
    private void shareOneUnitAcross(String rule, List<ForceDescriptor> members) {
        boolean shareChassis = rule.equals("chassis");
        // Only the members without a pick are given one. Testing that they all lack one would let a
        // partly-picked block through and add a second model to those that already had theirs, which
        // an ancestor had set deliberately.
        List<ForceDescriptor> unpicked = members.stream()
                                              .filter(member -> shareChassis
                                                    ? member.getChassis().isEmpty()
                                                    : member.getModels().isEmpty())
                                              .toList();
        if (unpicked.isEmpty()) {
            return;
        }
        ModelRecord shared = unpicked.getFirst().generate();
        if (shared == null) {
            LOGGER.debug("[ForceGen][GenRule] '{}': no unit available to share across {} child(ren)",
                  parseName(), unpicked.size());
            return;
        }
        for (ForceDescriptor member : unpicked) {
            if (shareChassis) {
                member.getChassis().add(shared.getChassis());
            } else {
                member.getModels().add(shared.getKey());
            }
        }
        LOGGER.debug("[ForceGen][GenRule] '{}': {} of {} child(ren) share {} '{}'",
              parseName(), unpicked.size(), members.size(), rule, shared.getKey());
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
        boolean hasNoPinnedModel = models.isEmpty();
        boolean carriesArtillery = roles.contains(MissionRole.ARTILLERY);
        boolean canMountArtillery = isUnitType(UnitType.MEK) || isUnitType(UnitType.TANK);
        if (hasNoPinnedModel && carriesArtillery && canMountArtillery) {
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
        int[] preferredTypes = isUnitType(UnitType.TANK)
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
            } else if (subForces.get(i).name.matches(".*\\{(?!echelon})[^:]*}.*")) {
                // {echelon} is excluded on purpose: it describes the node's size rather than its
                // position, so a name carrying only that token must not consume a sequence index and
                // shift its siblings' ordinals.
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
              && (getWarshipPct() <= 0)) {
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
        transports.setEchelon(isClan ? 7 : ECHELON_NAVAL_ROOT);
        transports.setCoRank(35);

        // Always render the categories in the canonical order: WarShips first, then JumpShips, then DropShips.
        if (isClan) {
            addClanCategory(transports, warships, "WarShip Stars");
            addClanCategory(transports, jumpships, "JumpShip Stars");
            // Named for what they are for: these hulls are generated to carry the command's
            // combat units, and are kept distinct from the cargo hulls a consumer adds later to
            // haul supplies.
            addClanCategory(transports, dropships, "Troopship Stars");
        } else {
            addISCategory(transports, warships, "WarShips");
            addISCategory(transports, jumpships, "JumpShips");
            // See the Clan branch above: these carry units, not cargo.
            addISCategory(transports, dropships, "Troopships");
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
              ECHELON_NAVAL_CATEGORY, /* coRank = MAJ_GENERAL */ 42);
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

    /** Ships in a Flotilla: small or medium vessels operating alone or in pairs. */
    private static final int SHIPS_PER_FLOTILLA = 2;
    /** Flotillas in a Division: six vessels. */
    private static final int FLOTILLAS_PER_DIVISION = 3;
    /** Divisions in a Squadron: up to eighteen vessels. */
    private static final int DIVISIONS_PER_SQUADRON = 3;
    private static final int FLOTILLAS_PER_SQUADRON = FLOTILLAS_PER_DIVISION * DIVISIONS_PER_SQUADRON;

    /**
     * Adds the Inner Sphere / SLDF naval hierarchy under the given category node, following the
     * organisation described for the Star League Navy after Commanding Admiral David Peterson's
     * reforms:
     * <ul>
     *   <li><b>Flotilla</b> - one or two vessels, small or medium ships operating alone or in pairs</li>
     *   <li><b>Division</b> - six vessels (three Flotillas)</li>
     *   <li><b>Squadron</b> - up to eighteen vessels (three Divisions), capable of independent
     *       operation</li>
     *   <li><b>Fleet</b> - a number of Squadrons combined</li>
     * </ul>
     *
     * <p>The shallowest structure that fits the ship count is used, so a pair of DropShips is a single
     * Flotilla rather than a Fleet of one Squadron of one Division. Nodes are named for their echelon
     * only ("Flotilla", "Division"); the designator that distinguishes siblings is applied downstream
     * by the consumer's naming convention, so naval formations follow the same scheme as the rest of
     * the force instead of a hardcoded one of their own.</p>
     *
     * @param parent The category node ("WarShips" / "JumpShips" / "Troopships") that receives the hierarchy
     * @param ships  The ships to add (must be non-empty; callers should pre-filter)
     */
    private void addNavalHierarchy(ForceDescriptor parent, List<MekSummary> ships) {
        List<List<MekSummary>> flotillas = new ArrayList<>();
        for (int i = 0; i < ships.size(); i += SHIPS_PER_FLOTILLA) {
            flotillas.add(ships.subList(i, Math.min(i + SHIPS_PER_FLOTILLA, ships.size())));
        }

        if (flotillas.size() <= 1) {
            // Up to 2 ships: a lone Flotilla, directly under the category.
            addFlotilla(parent, flotillas.get(0));
        } else if (flotillas.size() <= FLOTILLAS_PER_DIVISION) {
            // Up to 6 ships: one Division of Flotillas.
            addDivision(parent, flotillas, 0, flotillas.size());
        } else if (flotillas.size() <= FLOTILLAS_PER_SQUADRON) {
            // Up to 18 ships: one Squadron of Divisions.
            addSquadron(parent, flotillas, 0, flotillas.size());
        } else {
            // More than a Squadron's worth: Squadrons combine into a Fleet.
            ForceDescriptor fleet = createGroupNode(parent, "Fleet",
                  ECHELON_NAVAL_FLEET, /* coRank = LT_GENERAL */ 39);
            for (int start = 0; start < flotillas.size(); start += FLOTILLAS_PER_SQUADRON) {
                addSquadron(fleet, flotillas,
                      start, Math.min(start + FLOTILLAS_PER_SQUADRON, flotillas.size()));
            }
        }
    }

    /** Adds one Squadron covering {@code flotillas[startIndex, endIndex)}, split into Divisions. */
    private void addSquadron(ForceDescriptor parent, List<List<MekSummary>> flotillas,
          int startIndex, int endIndex) {
        ForceDescriptor squadron = createGroupNode(parent, "Squadron",
              ECHELON_NAVAL_SQUADRON, /* coRank = COLONEL */ 38);
        for (int start = startIndex; start < endIndex; start += FLOTILLAS_PER_DIVISION) {
            addDivision(squadron, flotillas, start, Math.min(start + FLOTILLAS_PER_DIVISION, endIndex));
        }
    }

    /** Adds one Division covering {@code flotillas[startIndex, endIndex)}. */
    private void addDivision(ForceDescriptor parent, List<List<MekSummary>> flotillas,
          int startIndex, int endIndex) {
        ForceDescriptor division = createGroupNode(parent, "Division",
              ECHELON_NAVAL_DIVISION, /* coRank = LT_COLONEL */ 37);
        for (int index = startIndex; index < endIndex; index++) {
            addFlotilla(division, flotillas.get(index));
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
    private void addFlotilla(ForceDescriptor parent, List<MekSummary> flotillaShips) {
        ForceDescriptor flotilla = createGroupNode(parent, "Flotilla",
              ECHELON_NAVAL_FLOTILLA, /* coRank = MAJOR */ 35);
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
        // Parent tokens that could not be resolved above (the node is the root of the generated
        // force, or its parent carries no name index) are dropped together with a directly attached
        // "/" or "-" separator, so a template like "{cardinal:parent}/{alpha} Company" degrades to
        // "A Company" for a bare company instead of "/A Company".
        retVal = retVal.replaceAll("\\{[^}]*:parent}[/-]?", "");
        // {echelon} names the formation's own size ("Company", "Battalion", "Squadron"). Unlike the
        // sequence tokens below it does not depend on nameIndex - it describes what this node IS, not
        // where it sits among its siblings - so it is resolved here, before the nameIndex branch that
        // would otherwise strip it from an unindexed node.
        if (retVal.contains("{echelon}")) {
            String echelonName = findEschelonName();
            retVal = (echelonName == null)
                  ? retVal.replaceAll("\\{echelon}\\s?", "")
                  : retVal.replace("{echelon}", echelonName);
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

    /**
     * The display name of this node's echelon - "Company", "Battalion", "Trinary", "Level III" - taken
     * from the {@code eschName} of the first matching {@code <force>} rule, walking up the ruleset
     * parent chain until one supplies it.
     *
     * @return the echelon name, or {@code null} when no ruleset in the chain names this echelon
     */
    public @Nullable String findEschelonName() {
        Ruleset rules = Ruleset.findRuleset(this);
        while (rules != null) {
            String echelonName = rules.getEschelonName(this);
            if (echelonName != null) {
                return echelonName;
            }
            rules = (rules.getParent() == null) ? null : Ruleset.findRuleset(rules.getParent());
        }
        return null;
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
        String echelonName = findEschelonName();

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

    /**
     * Null-safe test of this descriptor's unit type. {@code unitType} is a boxed {@link Integer} and
     * is legitimately {@code null} whenever the ruleset places no restriction on unit type - the
     * ComStar and Word of Blake tables of contents both declare {@code <unitType>null</unitType>}.
     * Comparing the field to a {@link UnitType} constant directly unboxes it, so an unrestricted
     * descriptor throws a {@link NullPointerException} rather than simply failing the test.
     *
     * @param candidateUnitType the {@link UnitType} constant to test against
     *
     * @return {@code true} if this descriptor has a unit type and it is the given one; {@code false}
     *       when the descriptor carries no unit type at all
     */
    private boolean isUnitType(int candidateUnitType) {
        return (unitType != null) && (unitType == candidateUnitType);
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

    /**
     * Discards the units drawn for this force and everything under it, leaving the structure intact.
     *
     * <p>The tree survives - the lances, their positions and their rules are all untouched - so the same node can be
     * generated again against a different formation. Without this, a second pass through
     * {@link #generateUnits(Ruleset.ProgressListener, double)} inherits the chassis and models left behind by the
     * first and picks against a list already narrowed by units the player has just discarded.</p>
     *
     * <p>The element flag is deliberately kept: a leaf is still a leaf, and clearing it would tell the next pass that
     * this node has units to distribute among children it does not have.</p>
     */
    public void clearGeneratedUnits() {
        entity = null;
        chassis.clear();
        models.clear();
        variants.clear();
        // The weight class goes with them. Generation overwrites it with the weight of the units it drew, so
        // leaving it behind makes the next pass pick against the weight of the units just discarded: a Heavy Recon
        // formation asked for inside a medium company was refused outright and fell back to an ordinary lance.
        // Cleared, the formation's own requirements decide the weight, which is what asking for it meant.
        weightClass = null;
        subForces.forEach(ForceDescriptor::clearGeneratedUnits);
        attached.forEach(ForceDescriptor::clearGeneratedUnits);
    }

    public void setFormationType(FormationType ft) {
        formationType = ft;
    }

    /**
     * The formation types this node's rule offered, mapped to the weight the ruleset gave each.
     *
     * <p>Recorded before the weighted pick is made, so it describes what could have been chosen rather than what
     * was. Size tells the caller what kind of node this is: more than one entry means a genuine choice was made and
     * could be made differently, exactly one means the rule narrowed to a single formation - a command lance, say -
     * and none means the node was never offered a formation at all.</p>
     *
     * @return the offered formations and their weights, never {@code null}
     */
    public Map<String, Integer> getEligibleFormations() {
        return (eligibleFormations == null) ? Map.of() : eligibleFormations;
    }

    public void setEligibleFormations(Map<String, Integer> eligibleFormations) {
        this.eligibleFormations = (eligibleFormations == null) ? Map.of() : eligibleFormations;
    }

    /**
     * The requested distribution of formation types across this force, set on the root node only.
     *
     * @return the requested mix, never {@code null}
     */
    public FormationMix getFormationMix() {
        return (formationMix == null) ? FormationMix.EMPTY : formationMix;
    }

    public void setFormationMix(@Nullable FormationMix formationMix) {
        this.formationMix = (formationMix == null) ? FormationMix.EMPTY : formationMix;
    }

    /**
     * What the formation mix achieved on this force, requested against placed.
     *
     * @return the report, or {@code null} when no mix was applied
     */
    public @Nullable FormationMixReport getFormationMixReport() {
        return formationMixReport;
    }

    public void setFormationMixReport(@Nullable FormationMixReport formationMixReport) {
        this.formationMixReport = formationMixReport;
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

    /**
     * Whether this descriptor is included in the generated force. Preview-only state (not persisted):
     * a host UI can mark nodes excluded so they are struck out in the preview and skipped when the
     * force is committed.
     *
     * @return {@code true} if this node is included (the default)
     */
    public boolean isIncluded() {
        return included;
    }

    /**
     * Sets whether this descriptor alone is included. Use {@link #setIncludedRecursively(boolean)} to
     * cascade the value to the whole subtree.
     *
     * @param included {@code true} to include this node, {@code false} to exclude it
     */
    public void setIncluded(boolean included) {
        this.included = included;
    }

    /**
     * Sets this descriptor's included flag and cascades the same value to every subforce and attached
     * descriptor beneath it, so excluding (or re-including) a formation applies to all its units.
     *
     * @param included {@code true} to include the subtree, {@code false} to exclude it
     */
    public void setIncludedRecursively(boolean included) {
        this.included = included;
        if (subForces != null) {
            for (ForceDescriptor subForce : subForces) {
                subForce.setIncludedRecursively(included);
            }
        }
        if (attached != null) {
            for (ForceDescriptor attachedForce : attached) {
                attachedForce.setIncludedRecursively(included);
            }
        }
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
        if (carriers.isEmpty()) {
            // The single most common reason a player sees no fighters: the option is on, but this
            // subtree holds no large craft at all.
            LOGGER.debug("[ForceGen][Fighters] no large craft in this subtree; nothing to fill");
            return;
        }
        LOGGER.debug("[ForceGen][Fighters] filling ASF bays for {} carrier(s)", carriers.size());
        int carriersSkippedUnknownModel = 0;
        int carriersSkippedNoBays = 0;
        for (ForceDescriptor carrier : carriers) {
            MekSummary carrierSummary = MekSummaryCache.getInstance().getMek(carrier.getModelName());
            if (carrierSummary == null) {
                carriersSkippedUnknownModel++;
                continue;
            }
            int capacity = TransportCalculator.fighterBayCapacity(carrierSummary);
            if (capacity <= 0) {
                carriersSkippedNoBays++;
                continue;
            }
            UnitTable table = UnitTable.findTable(carrier.getFactionRec(),
                  UnitType.AEROSPACE_FIGHTER,
                  carrier.getYear(),
                  carrier.ratGeneratorRating(),
                  null,
                  ModelRecord.NETWORK_NONE,
                  EnumSet.noneOf(EntityMovementMode.class),
                  EnumSet.noneOf(MissionRole.class),
                  0);
            // Organize the complement into the shallowest formation that actually fits the bay
            // capacity, so a Leopard's two fighter bays produce a single Flight rather than a Squadron
            // wrapping one Flight. The two fighters in a Point (Clan) or Flight (IS) are the SAME
            // model - a Point is a matched pair - while different Points within a Star may differ.
            boolean clan = (carrier.getFactionRec() != null) && carrier.getFactionRec().isClan();
            int pointSize = 2;
            int pointsPerGroup = clan ? 5 : 3;
            int groupSize = pointsPerGroup * pointSize;
            String groupLabel = clan ? "Star" : "Squadron";
            String pointLabel = clan ? "Point" : "Flight";
            int groupEchelon = clan ? 3 : 4;
            int pointEchelon = clan ? 2 : 3;
            int totalGroups = (capacity + groupSize - 1) / groupSize;

            // A capacity of one Point/Flight or less needs no wrapper at all: the Flight hangs
            // directly off the ship.
            boolean wrapInGroups = capacity > pointSize;
            // With more than one group the complement has outgrown a Squadron/Star, so those in turn
            // nest under a Group (IS) or Binary/Trinary (Clan) to keep the ToE readable.
            ForceDescriptor groupParent = carrier;
            if (wrapInGroups && (totalGroups > 1)) {
                groupParent = carrier.createChild(carrier.getAttached().size());
                groupParent.getModels().clear();
                groupParent.getChassis().clear();
                groupParent.setUnitType(UnitType.AEROSPACE_FIGHTER);
                groupParent.setName(clan ? "Binary" : "Group");
                groupParent.setEchelon(clan ? 4 : 5);
                groupParent.setCoRank(34);
                carrier.addAttached(groupParent);
            }

            int generated = 0;
            boolean exhausted = false;
            for (int groupIndex = 0; (groupIndex < totalGroups) && !exhausted; groupIndex++) {
                ForceDescriptor group;
                if (wrapInGroups) {
                    group = groupParent.createChild(groupParent.getAttached().size());
                    group.getModels().clear();
                    group.getChassis().clear();
                    group.setUnitType(UnitType.AEROSPACE_FIGHTER);
                    group.setName((totalGroups > 1)
                          ? PHONETIC[Math.min(groupIndex, PHONETIC.length - 1)] + " " + groupLabel
                          : groupLabel);
                    group.setEchelon(groupEchelon);
                    group.setCoRank(32);
                    groupParent.addAttached(group);
                } else {
                    // No wrapper: the Flight itself is attached straight to the ship below.
                    group = carrier;
                }

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
                    if (wrapInGroups && (pointIndex == 0)) {
                        group.addSubForce(point);
                    } else {
                        // Without a wrapper the "group" IS the carrier, and a ship must stay a leaf
                        // element - adding a subforce would stop it being recognised as a carrier and
                        // break entity loading. Attach in that case.
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
                        // Crew the fighter here rather than relying on the force-wide commander pass.
                        // These groups hang off the carrier via addAttached, which that pass does not
                        // descend into, and when this runs for transport-stage carriers the pass has
                        // already finished. Without a crew, loadEntities NPEs on getCo().
                        fighter.assignCommanders();
                        point.addSubForce(fighter);
                        producedInGroup++;
                        generated++;
                    }
                }
                // Drop a wrapper that produced nothing because the table dried up. Points live in both
                // lists (first = subForce, rest = attached), so check both. Skipped when there is no
                // wrapper, since "group" is then the carrier itself.
                if (wrapInGroups && group.getSubForces().isEmpty() && group.getAttached().isEmpty()) {
                    groupParent.getAttached().remove(group);
                }
            }
            LOGGER.debug("[ForceGen][Fighters] {} (capacity {}): generated {} fighter(s)",
                  carrier.getModelName(), capacity, generated);
        }
        // Summarised after the loop rather than logged per carrier, so a WarShip fleet does not spam
        // the log; these two counts are what explain an empty result when the option is on.
        if ((carriersSkippedUnknownModel > 0) || (carriersSkippedNoBays > 0)) {
            LOGGER.debug("[ForceGen][Fighters] skipped {} carrier(s) with an unresolvable model and"
                        + " {} with no fighter bays", carriersSkippedUnknownModel, carriersSkippedNoBays);
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

    public double getCargoPct() {
        return cargoPct;
    }

    public void setCargoPct(double cargoPct) {
        this.cargoPct = cargoPct;
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

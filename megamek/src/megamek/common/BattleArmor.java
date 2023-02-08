/*
 * MegaMek - Copyright (C) 2002-2004 Ben Mazur (bmazur@sev.org)
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
package megamek.common;

import megamek.client.ui.swing.calculationReport.CalculationReport;
import megamek.client.ui.swing.calculationReport.DummyCalculationReport;
import megamek.common.battlevalue.BattleArmorBVCalculator;
import megamek.common.cost.BattleArmorCostCalculator;
import megamek.common.enums.AimingMode;
import megamek.common.options.OptionsConstants;
import megamek.common.weapons.InfantryAttack;
import megamek.common.weapons.infantry.InfantryWeapon;
import org.apache.logging.log4j.LogManager;

import java.text.NumberFormat;
import java.util.*;

/**
 * This class represents a squad or point of battle armor equipped infantry,
 * sometimes referred to as "Elementals". Much of the behaviour of a battle
 * armor unit is identical to that of an infantry platoon, and is rather
 * different from that of a Mek or Tank.
 *
 * This was originally coded using the legacy programming style of putting constants first in tests
 * so the compiler catches the "= for ==" errors.
 * @author Suvarov454@sourceforge.net (James A. Damour)
 */
public class BattleArmor extends Infantry {
    private static final long serialVersionUID = 4594311535026187825L;
    /*
     * Infantry have no critical slot limitations. IS squads usually have 4 men,
     * Clan points usually have 5. Have a location that represents the entire
     * squad.
     */
    private static final int[] IS_NUM_OF_SLOTS = { 20, 2, 2, 2, 2, 2, 2 };
    private static final String[] IS_LOCATION_ABBRS = { "Squad", "Trooper 1",
        "Trooper 2", "Trooper 3", "Trooper 4", "Trooper 5", "Trooper 6" };
    private static final String[] IS_LOCATION_NAMES = { "Squad", "Trooper 1",
        "Trooper 2", "Trooper 3", "Trooper 4", "Trooper 5", "Trooper 6" };
    private static final int[] CLAN_NUM_OF_SLOTS = { 20, 2, 2, 2, 2, 2, 2 };
    private static final String[] CLAN_LOCATION_ABBRS = { "Point", "Trooper 1",
        "Trooper 2", "Trooper 3", "Trooper 4", "Trooper 5", "Trooper 6" };
    private static final String[] CLAN_LOCATION_NAMES = { "Point", "Trooper 1",
        "Trooper 2", "Trooper 3", "Trooper 4", "Trooper 5", "Trooper 6" };

    public static final int MANIPULATOR_NONE = 0;
    public static final int MANIPULATOR_ARMORED_GLOVE = 1;
    public static final int MANIPULATOR_BASIC = 2;
    public static final int MANIPULATOR_BASIC_MINE_CLEARANCE = 3;
    public static final int MANIPULATOR_BATTLE = 4;
    public static final int MANIPULATOR_BATTLE_MAGNET = 5;
    public static final int MANIPULATOR_BATTLE_VIBRO = 6;
    public static final int MANIPULATOR_HEAVY_BATTLE = 7;
    public static final int MANIPULATOR_HEAVY_BATTLE_MAGNET = 8;
    public static final int MANIPULATOR_HEAVY_BATTLE_VIBRO = 9;
    public static final int MANIPULATOR_SALVAGE_ARM = 10;
    public static final int MANIPULATOR_CARGO_LIFTER = 11;
    public static final int MANIPULATOR_INDUSTRIAL_DRILL = 12;

    /**
     * A list of the internal names for the different manipulator types.
     * The indices in this collection correspond to the MANIPULATOR defines
     * in <code>BattleArmor</code>.  These names should match the internal
     * name for the manipulator's MiscType entry.
     */
    public static final String[] MANIPULATOR_TYPE_STRINGS = { "None",
        "BAArmoredGlove", "BABasicManipulator",
        "BABasicManipulatorMineClearance", "BABattleClaw",
        "BABattleClawMagnets", "BABattleClawVibro",
        "BAHeavyBattleClaw", "BAHeavyBattleClawMagnets",
        "BAHeavyBattleClawVibro",
        "BASalvageArm", "BACargoLifter", "BAIndustrialDrill" };

    /**
     * A list of the display names for the different manipulator types.
     * The indices in this collection correspond to the MANIPULATOR defines
     * in <code>BattleArmor</code>.  These names should match the
     * name for the manipulator's MiscType entry.
     */
    public static final String[] MANIPULATOR_NAME_STRINGS = { "None",
        "BA Manipulators [Armored Gloves]", "BA Manipulators [Manipulator (Basic)]",
        "BA Manipulator Adaptation [Mine Clearance Equipment]", "BA Manipulators [Battle Claw]",
        "BA Manipulator Adaptation [Magnetic Battle Claw]", "BA Manipulator Adaptation [Vibro-Claw]",
        "BA Manipulators [Heavy Battle Claw]", "BA Manipulator Adaptation [Heavy Magnetic Battle Claw]",
        "BA Manipulator Adaptation [Heavy Vibro-Claw]", "BA Manipulators [Salvage Arm]", "BA Manipulators [Cargo Lifter]",
        "BA Manipulators [Industrial Drill]" };

    public static final int CHASSIS_TYPE_BIPED = 0;
    public static final int CHASSIS_TYPE_QUAD = 1;

    /**
     * The number of men alive in this unit at the beginning of the phase,
     * before it begins to take damage.
     */
    private int troopersShooting = 0;

    /**
     * the number of troopers of this squad, dead or alive
     */
    private int troopers = -1;

    /**
     * The cost of this unit. This value should be set when the unit's file is
     * read.
     */
    protected int myCost = -1;
    /**
     * This unit's weight class
     */
    private int weightClass = -1;
    /**
     * this unit's chassis type, should be BattleArmor.CHASSIS_TYPE_BIPED or
     * BattleArmor.CHASSIS_TYPE_QUAD
     */
    private int chassisType = -1;

    /**
     * Flag that is <code>true</code> when this object's constructor has
     * completed.
     */
    private boolean isInitialized = false;

    /**
     * Flag that is <code>true</code> when this unit is equipped with stealth.
     */
    private boolean isStealthy = false;

    /**
     * Flag that is <code>true</code> when this unit is equipped with mimetic
     * armor.
     */
    private boolean isMimetic = false;

    /**
     * Flag that is <code>true</code> when this unit is equipped with a camo
     * system.
     */
    private boolean hasCamoSystem = false;

    /**
     * Modifiers to <code>ToHitData</code> for stealth.
     */
    private int shortStealthMod = 0;
    private int mediumStealthMod = 0;
    private int longStealthMod = 0;
    private String stealthName = null;
    private String camoName = null;

    // Public and Protected constants, constructors, and methods.

    /**
     * Internal name of the Inner disposable SRM2 ammo pack.
     */
    public static final String DISPOSABLE_SRM2_AMMO = "BA-SRM2 (one shot) Ammo";

    /**
     * Internal name of the disposable NARC ammo pack.
     */
    public static final String DISPOSABLE_NARC_AMMO = "BA-Compact Narc Ammo";

    /**
     * The internal name for the Mine Launcher weapon.
     */
    public static final String MINE_LAUNCHER = "BAMineLauncher";

    /**
     * The internal name for advanced.
     */
    public static final String ADVANCED_ARMOR = EquipmentType
            .getArmorTypeName(EquipmentType.T_ARMOR_BA_STANDARD_ADVANCED);

    /**
     * The internal name for standard Prototype.
     */
    public static final String STANDARD_PROTOTYPE = EquipmentType
            .getArmorTypeName(EquipmentType.T_ARMOR_BA_STANDARD_PROTOTYPE);

    /**
     * The internal name for stealth Prototype.
     */
    public static final String STEALTH_PROTOTYPE = EquipmentType
            .getArmorTypeName(EquipmentType.T_ARMOR_BA_STEALTH_PROTOTYPE);

    /**
     * The internal name for basic Stealth armor.
     */
    public static final String BASIC_STEALTH_ARMOR = EquipmentType
            .getArmorTypeName(EquipmentType.T_ARMOR_BA_STEALTH_BASIC);

    /**
     * The internal name for standard Stealth armor.
     */
    public static final String STANDARD_STEALTH_ARMOR = EquipmentType
            .getArmorTypeName(EquipmentType.T_ARMOR_BA_STEALTH);

    /**
     * The internal name for improved Stealth armor.
     */
    public static final String IMPROVED_STEALTH_ARMOR = EquipmentType
            .getArmorTypeName(EquipmentType.T_ARMOR_BA_STEALTH_IMP);

    /**
     * The internal name for Mimetic armor.
     */
    public static final String MIMETIC_ARMOR = EquipmentType
            .getArmorTypeName(EquipmentType.T_ARMOR_BA_MIMETIC);

    /**
     * The internal name for fire-resistant armor.
     */
    public static final String FIRE_RESISTANT = EquipmentType
            .getArmorTypeName(EquipmentType.T_ARMOR_BA_FIRE_RESIST);

    /**
     * The internal name for Simple Camo equipment.
     */
    public static final String CAMO_SYSTEM = "Camo System";

    /**
     * The internal name for Single-Hex ECM equipment.
     */
    public static final String SINGLE_HEX_ECM = "Single-Hex ECM";

    /**
     * /** The maximum number of men in a battle armor squad.
     */
    public static final int BA_MAX_MEN = 6;

    /**
     * The location for infantry equipment.
     */
    public static final int LOC_SQUAD = 0;

    public static final int LOC_TROOPER_1 = 1;
    public static final int LOC_TROOPER_2 = 2;
    public static final int LOC_TROOPER_3 = 3;
    public static final int LOC_TROOPER_4 = 4;
    public static final int LOC_TROOPER_5 = 5;
    public static final int LOC_TROOPER_6 = 6;

    /**
     * The location for mounted equipment on BA
     */
    public static final int MOUNT_LOC_NONE   = -1;
    public static final int MOUNT_LOC_BODY   = 0;
    public static final int MOUNT_LOC_RARM   = 1;
    public static final int MOUNT_LOC_LARM   = 2;
    public static final int MOUNT_LOC_TURRET = 3;

    public static final String[] MOUNT_LOC_NAMES = { "Body", "Right Arm",
            "Left Arm", "Turret" };

    /**
     * How many mount locations are possible?
     */
    public static final int MOUNT_NUM_LOCS = 4;
    
    // Quad BA can add critical space by adding a turret mount.
    private int turretSize = 0;
    private boolean modularTurret = false;

    private boolean exoskeleton = false;

    /**
     * Clan industrial exoskeletons can opt to not use Harjel, to allow them to
     * use IS chassis weight; this flag indicates whether or not this is the
     * case.
     */
    private boolean clanExoWithoutHarjel = false;

    @Override
    public String[] getLocationAbbrs() {
        if (!isInitialized || isClan()) {
            return CLAN_LOCATION_ABBRS;
        }
        return IS_LOCATION_ABBRS;
    }

    public String[] getBaMountLocAbbr() {
        return MOUNT_LOC_NAMES;
    }

    public static String getBaMountLocAbbr(int loc) {
        if (loc == MOUNT_LOC_NONE) {
            return "None";
        }
        return MOUNT_LOC_NAMES[loc];
    }

    @Override
    public String[] getLocationNames() {
        if (!isInitialized || isClan()) {
            return CLAN_LOCATION_NAMES;
        }
        return IS_LOCATION_NAMES;
    }

    /**
     * Returns the number of Troopers in the BattleArmor squad, since locations
     * for BattleArmor correspond to the different suits instead of the actual
     * mount locations for equipment.
     */
    @Override
    public int locations() {
        int retVal = getTroopers();
        if (retVal == 0) {
            // Return one more than the maximum number of men in the unit.
            if (!isInitialized) {
                retVal = 6 + 1;
            } else if (isClan()) {
                retVal = 5 + 1;
            } else {
                retVal = 4 + 1;
            }
        } else {
            retVal++;
        }
        return retVal;
    }

    /**
     * Generate a new, blank, battle armor unit. Hopefully, we'll be loaded from
     * somewhere.
     */
    public BattleArmor() {
        // Instantiate the superclass.
        super();

        setArmorType(EquipmentType.T_ARMOR_BA_STANDARD);

        // BA are always one squad
        squadCount = 1;

        // All Battle Armor squads are Clan until specified otherwise.
        setTechLevel(TechConstants.T_CLAN_TW);

        // Construction complete.
        isInitialized = true;
    }

    @Override
    public int getUnitType() {
        return UnitType.BATTLE_ARMOR;
    }

    protected static final TechAdvancement[] TA_BATTLEARMOR = {
            new TechAdvancement(TECH_BASE_ALL).setISAdvancement(2710, DATE_NONE, 3058, 2766, 2905)
                .setClanAdvancement(2710, DATE_NONE, 3058).setPrototypeFactions(F_TH)
                .setReintroductionFactions(F_CS).setTechRating(RATING_D)
                .setAvailability(RATING_F, RATING_X, RATING_E, RATING_D)
                .setStaticTechLevel(SimpleTechLevel.STANDARD), //ultralight
            new TechAdvancement(TECH_BASE_ALL).setISAdvancement(DATE_NONE, 3050, 3050)
                .setClanAdvancement(2865, 2870, 2900).setPrototypeFactions(F_CWF)
                .setProductionFactions(F_CIH, F_FS, F_LC).setClanApproximate(true, false, false)
                .setTechRating(RATING_E)
                .setAvailability(RATING_X, RATING_F, RATING_E, RATING_D)
                .setStaticTechLevel(SimpleTechLevel.STANDARD), //light
            new TechAdvancement(TECH_BASE_ALL).setISAdvancement(2864, 3052, 3052)
                .setClanAdvancement(2840, 2868, 2875)
                .setClanApproximate(true, false, false).setPrototypeFactions(F_CGS)
                .setProductionFactions(F_CWF, F_FS, F_LC, F_CS).setTechRating(RATING_E)
                .setAvailability(RATING_X, RATING_D, RATING_D, RATING_D)
                .setStaticTechLevel(SimpleTechLevel.STANDARD), //medium
            new TechAdvancement(TECH_BASE_ALL).setISAdvancement(DATE_NONE, 3050, 3058)
                .setClanAdvancement(2867, 2875, 3058)
                .setClanApproximate(true, false, false).setPrototypeFactions(F_CWF)
                .setProductionFactions(F_CHH, F_FS, F_LC).setTechRating(RATING_E)
                .setAvailability(RATING_X, RATING_F, RATING_E, RATING_D)
                .setStaticTechLevel(SimpleTechLevel.STANDARD), // heavy
            new TechAdvancement(TECH_BASE_ALL).setISAdvancement(DATE_NONE, 3058, 3060)
                .setClanAdvancement(2870, 2877, 3060)
                .setClanApproximate(true, false, false).setPrototypeFactions(F_CNC)
                .setProductionFactions(F_CGB, F_DC).setTechRating(RATING_E)
                .setAvailability(RATING_X, RATING_F, RATING_E, RATING_D)
                .setStaticTechLevel(SimpleTechLevel.STANDARD) // assault
    };
    
    public static TechAdvancement getConstructionTechAdvancement(int weightClass) {
        return new TechAdvancement(TA_BATTLEARMOR[weightClass]);
    }
    
    @Override
    public TechAdvancement getConstructionTechAdvancement() {
        int index = getWeightClass();
        if (index < 0 || index >= TA_BATTLEARMOR.length) {
            index = EntityWeightClass.WEIGHT_MEDIUM;
        }
        return TA_BATTLEARMOR[index];
    }

    /**
     * Returns this entity's original jumping mp.
     */
    @Override
    public int getOriginalJumpMP() {
        return jumpMP;
    }

    /**
     * Returns this entity's walking mp, factored for extreme temperatures and
     * gravity.
     */
    @Override
    public int getWalkMP(boolean gravity, boolean ignoreheat,
            boolean ignoremodulararmor) {
        return getWalkMP(gravity, ignoreheat, ignoremodulararmor, false, false);
    }

    public int getWalkMP(boolean gravity, boolean ignoreheat,
            boolean ignoremodulararmor, boolean ignoreDWP,
            boolean ignoreMyomerBooster) {
        int j = getOriginalWalkMP();
        if (hasMyomerBooster()) {
            if (!ignoreMyomerBooster) {
                if (getWeightClass() >= EntityWeightClass.WEIGHT_HEAVY) {
                    j++;
                } else {
                    j += 2;
                }
            }
        } else if (hasWorkingMisc(MiscType.F_MECHANICAL_JUMP_BOOSTER)) {
            // mechanical jump booster gives an extra MP
            j++;
        }
        if (hasDWP() && !ignoreDWP) {
            if (getWeightClass() == EntityWeightClass.WEIGHT_MEDIUM) {
                j -= 3;
            } else if (getWeightClass() >= EntityWeightClass.WEIGHT_HEAVY) {
                j -= 2;
            }
            if (j == 0) {
                j++;
            }
        }
        if (null != game) {
            int weatherMod = game.getPlanetaryConditions()
                    .getMovementMods(this);
            if (weatherMod != 0) {
                j = Math.max(j + weatherMod, 0);
            }
        }
        if (null != game) {
            if ((game.getPlanetaryConditions().getWeather() == PlanetaryConditions.WE_GUSTING_RAIN) && getCrew().getOptions().stringOption(OptionsConstants.MISC_ENV_SPECIALIST).equals(Crew.ENVSPC_RAIN)) {
                j += 1;
            }

            if (getCrew().getOptions().stringOption(OptionsConstants.MISC_ENV_SPECIALIST).equals(Crew.ENVSPC_WIND)
                    && ((game.getPlanetaryConditions().getWeather() == PlanetaryConditions.WI_STRONG_GALE)
                    || (game.getPlanetaryConditions().getWeather() == PlanetaryConditions.WI_STORM))) {
                j += 1;
            }
        }
        if (gravity) {
            j = applyGravityEffectsOnMP(j);
        }
        return j;
    }

    @Override
    public int getRunMP(boolean gravity, boolean ignoreheat,
            boolean ignoremodulararmor) {
        boolean fastMove = (game != null) &&
                game.getOptions().booleanOption(OptionsConstants.ADVGRNDMOV_TACOPS_FAST_INFANTRY_MOVE);
        if (fastMove) {
            return getWalkMP(gravity, ignoreheat, ignoremodulararmor, false,
                    false) + 1;
        }
        return getWalkMP(gravity, ignoreheat, ignoremodulararmor, false, false);
    }

    /**
     * does this ba mount a myomer booster?
     *
     * @return
     */
    public boolean hasMyomerBooster() {
        for (Mounted mEquip : getMisc()) {
            MiscType mtype = (MiscType) mEquip.getType();
            if (mtype.hasFlag(MiscType.F_MASC) && !mEquip.isInoperable()) {
                return true;
            }
        }
        return false;
    }

    /*
     * (non-Javadoc)
     *
     * @see megamek.common.Infantry#getJumpMP(boolean)
     */
    @Override
    public int getJumpMP(boolean gravity) {
        return getJumpMP(gravity, false, false);
    }

    /**
     * get this BA's jump MP, possibly ignoring gravity and burden
     *
     * @param gravity
     * @param ignoreBurden
     * @return
     */
    public int getJumpMP(boolean gravity, boolean ignoreBurden,
            boolean ignoreDWP) {
        if (isBurdened() && !ignoreBurden) {
            return 0;
        }
        if (hasDWP() && !ignoreDWP) {
            return 0;
        }
        if (null != game) {
            int windCond = game.getPlanetaryConditions().getWindStrength();
            if (windCond >= PlanetaryConditions.WI_STORM) {
                return 0;
            }
        }
        int mp = 0;
        if (getMovementMode() != EntityMovementMode.INF_UMU) {
            mp = getOriginalJumpMP();
        }
        // if we have no normal jump jets, we get 1 jump MP from mechanical jump
        // boosters, if we have them.
        if ((mp == 0) && hasWorkingMisc(MiscType.F_MECHANICAL_JUMP_BOOSTER)) {
            mp++;
        }
        // partial wing gives extra MP in atmosphere
        if ((mp > 0)
                && hasWorkingMisc(MiscType.F_PARTIAL_WING)
                && ((game == null) || !game.getPlanetaryConditions().isVacuum())) {
            mp++;
        }
        if ((mp > 0) && hasWorkingMisc(MiscType.F_JUMP_BOOSTER)) {
            // jump booster gives an extra MP
            mp++;
        }
        if (gravity) {
            mp = applyGravityEffectsOnMP(mp);
        }
        return mp;
    }

    /**
     * Returns the name of the type of movement used. This is Infantry-specific.
     */
    @Override
    public String getMovementString(EntityMovementType mtype) {
        switch (mtype) {
            case MOVE_NONE:
                return "None";
            case MOVE_WALK:
            case MOVE_RUN:
                return "Walked";
            case MOVE_VTOL_WALK:
            case MOVE_VTOL_RUN:
                return "Flew";
            case MOVE_JUMP:
                return "Jumped";
            default:
                return "Unknown!";
        }
    }

    /**
     * Returns the abbreviation of the type of movement used. This is
     * Infantry-specific.
     */
    @Override
    public String getMovementAbbr(EntityMovementType mtype) {
        switch (mtype) {
            case MOVE_NONE:
                return "N";
            case MOVE_WALK:
                return "W";
            case MOVE_RUN:
                return "R";
            case MOVE_JUMP:
                return "J";
            case MOVE_VTOL_WALK:
            case MOVE_VTOL_RUN:
                return "F";
            default:
                return "?";
        }
    }

    /**
     * Battle Armor units can only get hit in undestroyed troopers.
     */
    @Override
    public HitData rollHitLocation(int table, int side, int aimedLocation, AimingMode aimingMode,
                                   int cover) {
        return rollHitLocation(side, aimedLocation, aimingMode, false);
    }

    /**
     * Battle Armor units can only get hit in undestroyed troopers.
     *
     * @param isAttackingConvInfantry Set to true when attacked by CI, as these cannot score TacOps crits
     */
    public HitData rollHitLocation(int side, int aimedLocation, AimingMode aimingMode,
                                   boolean isAttackingConvInfantry) {
        // If this squad was killed, target trooper 1 (just because).
        if (isDoomed()) {
            return new HitData(1);
        }

        if ((aimedLocation != LOC_NONE) && !aimingMode.isNone()) {
            int roll = Compute.d6(2);
            if ((5 < roll) && (roll < 9)) {
                return new HitData(aimedLocation, side == ToHitData.SIDE_REAR, true);
            }
        }

        // Pick a random number between 1 and 6.
        int loc = Compute.d6();

        // Pick a new random number if that trooper is dead or never existed.
        // Remember that there's one more location than the number of troopers.
        // In http://forums.classicbattletech.com/index.php/topic,43203.0.html,
        // "previously destroyed includes the current phase" for rolling hits on
        // a squad, modifying previous ruling in the AskThePM FAQ.
        while ((loc >= locations())
                || (IArmorState.ARMOR_NA == this.getInternal(loc))
                || (IArmorState.ARMOR_DESTROYED == this.getInternal(loc))
                || ((IArmorState.ARMOR_DOOMED == this.getInternal(loc)) && !isDoomed())) {
            loc = Compute.d6();
        }

        // TO:AR p.108: Trooper takes a crit if a second roll is the same location as the first.
        if (game.getOptions().booleanOption(OptionsConstants.ADVCOMBAT_TACOPS_BA_CRITICALS)
                && (loc == Compute.d6()) && !isAttackingConvInfantry) {
            return new HitData(loc, false, HitData.EFFECT_CRITICAL);
        }
        // Hit that trooper.
        return new HitData(loc);
    }

    @Override
    public HitData rollHitLocation(int table, int side) {
        return rollHitLocation(table, side, LOC_NONE, AimingMode.NONE, LosEffects.COVER_NONE);
    }

    /**
     * For level 3 rules, each trooper occupies a specific location
     * precondition: hit is a location covered by BA
     */
    @Override
    public HitData getTrooperAtLocation(HitData hit, Entity transport) {
        if (transport instanceof Mech) {
            int loc = 99;
            switch (hit.getLocation()) {
                case Mech.LOC_RT:
                    if (hit.isRear()) {
                        loc = 3;
                    } else {
                        loc = 1;
                    }
                    break;
                case Mech.LOC_LT:
                    if (hit.isRear()) {
                        loc = 4;
                    } else {
                        loc = 2;
                    }
                    break;
                case Mech.LOC_CT:
                    if (hit.isRear()) {
                        loc = 5;
                    } else {
                        loc = 6;
                    }
                    break;
            }
            if (loc < locations()) {
                return new HitData(loc);
            }
        } else if (transport instanceof Tank) {
            int loc = 99;
            switch (hit.getLocation()) {
                case Tank.LOC_RIGHT:
                    // There are 2 troopers on each location, so pick
                    // one randomly if both are alive.
                    if ((getInternal(1) > 0) && (getInternal(2) > 0)) {
                        loc = Compute.randomInt(2) + 1;
                    } else if (getInternal(1) > 0) {
                        loc = 1;
                    } else {
                        loc = 2;
                    }
                    break;
                case Tank.LOC_LEFT:
                    if ((getInternal(3) > 0) && (getInternal(4) > 0)) {
                        loc = Compute.randomInt(2) + 3;
                    } else if (getInternal(3) > 0) {
                        loc = 3;
                    } else {
                        loc = 4;
                    }
                    break;
                case Tank.LOC_REAR:
                    //Troopers 5 and 6 only exist when you have Clan and CS/WoB units, so we need
                    //to ensure the array is large enough before checking their status.
                    if (locations() >= 7 && ((getInternal(5) > 0) && (getInternal(6) > 0))) {
                        //If we have a live trooper 5 and 6, randomize who gets hit
                        loc = Compute.randomInt(2) + 5;
                    } else if ((locations() >= 7) && (getInternal(5) <= 0)) {
                        //If trooper 5 is dead, hit trooper 6
                        loc = 6;
                    } else {
                        loc = 5;
                    }
                    break;
            }
            // If we get here with an invalid loc for this squad, this next test should fail
            if (loc < locations()) {
                return new HitData(loc);
            }
        }
        // otherwise roll a random location
        return rollHitLocation(ToHitData.HIT_NORMAL, ToHitData.SIDE_FRONT);
    }

    /**
     * Battle Armor units don't transfer damage.
     */
    @Override
    public HitData getTransferLocation(HitData hit) {

        // If any trooper lives, the unit isn't destroyed.
        for (int loop = 1; loop < locations(); loop++) {
            if (0 < this.getInternal(loop)) {
                return new HitData(Entity.LOC_NONE);
            }
        }

        // No surviving troopers, so we're toast.
        return new HitData(Entity.LOC_DESTROYED);
    }

    /**
     * Battle Armor units have no armor on their squad location.
     *
     * @see megamek.common.Infantry#getArmor(int, boolean )
     */
    @Override
    public int getArmor(int loc, boolean rear) {
        if (BattleArmor.LOC_SQUAD != loc) {
            return super.getArmor(loc, rear);
        }
        return IArmorState.ARMOR_NA;
    }

    /**
     * Battle Armor units have no armor on their squad location.
     *
     * @see megamek.common.Infantry#getOArmor(int, boolean )
     */
    @Override
    public int getOArmor(int loc, boolean rear) {
        if (BattleArmor.LOC_SQUAD != loc) {
            return super.getOArmor(loc, rear);
        }
        return IArmorState.ARMOR_NA;
    }

    /**
     * Battle Armor units have no internals on their squad location.
     *
     * @see megamek.common.Infantry#getInternal(int)
     */
    @Override
    public int getInternal(int loc) {
        if (BattleArmor.LOC_SQUAD != loc) {
            return super.getInternal(loc);
        }
        return IArmorState.ARMOR_NA;
    }

    /**
     * Battle Armor units have no internals on their squad location.
     *
     * @see megamek.common.Infantry#getOInternal(int)
     */
    @Override
    public int getOInternal(int loc) {
        if (BattleArmor.LOC_SQUAD != loc) {
            return super.getOInternal(loc);
        }
        return IArmorState.ARMOR_NA;
    }

    /**
     * Set the troopers in the unit to the appropriate values.
     */
    @Override
    public void autoSetInternal() {
        // No troopers in the squad location.
        initializeInternal(IArmorState.ARMOR_NA, LOC_SQUAD);

        // Initialize the troopers.
        for (int loop = 1; loop < locations(); loop++) {
            initializeInternal(1, loop);
        }

        // Set the initial number of troopers that can shoot
        // to one less than the number of locations in the unit.
        troopersShooting = locations() - 1;
    }

    /**
     * Set the troopers in the unit to the given values.
     */
    public void setInternal(int value) {
        // Initialize the troopers.
        for (int loop = 1; loop < locations(); loop++) {
            initializeInternal(value, loop);
        }
    }

    /**
     * Mounts the specified equipment in the specified location.
     */
    @Override
    public void addEquipment(Mounted mounted, int loc, boolean rearMounted)
            throws LocationFullException {
        // Implement parent's behavior.
        super.addEquipment(mounted, loc, rearMounted);

        // Is the item a camo system equipment?
        String name = mounted.getType().getInternalName();
        if (BattleArmor.CAMO_SYSTEM.equals(name)) {
            hasCamoSystem = true;
            camoName = name;
        }
    }

    /**
     * Battle Armor units have as many critical slots as they need to hold their
     * equipment.
     */
    @Override
    protected int[] getNoOfSlots() {
        if (!isInitialized) {
            return CLAN_NUM_OF_SLOTS;
        }
        return Arrays.copyOf(isClan() ? CLAN_NUM_OF_SLOTS : IS_NUM_OF_SLOTS, troopers + 1);
    }

    /**
     * Trooper's equipment dies when they do.
     */
    @Override
    public boolean hasHittableCriticals(int loc) {
        if (LOC_SQUAD == loc) {
            return false;
        }
        return super.hasHittableCriticals(loc);
    }

    @Override
    public int doBattleValueCalculation(boolean ignoreC3, boolean ignoreSkill, CalculationReport calculationReport) {
        return BattleArmorBVCalculator.calculateBV(this, ignoreC3, ignoreSkill, calculationReport);
    }

    /**
     * Calculates the Battle Value of this BattleArmor. When singleTrooper is true the Battle Value of one
     * trooper is returned, otherwise the BV of the whole unit. Calling this method with the value of
     * singleTrooper being false is equivalent to calling the standard calculateBattleValue methods.
     *
     * <P> As in all other BV calculations, the other parameters can be used to ignore C3 / skill-based changes
     * to the BV. When both are true, the "base" BV of the unit is calculated. Note that when a unit has a manual BV
     * value set in its definition file, this manual BV value is returned instead of a calculated BV value.
     *
     * @param ignoreC3    When true, the BV contributions of any C3 computers are not added
     * @param ignoreSkill When true, the skill of the crew / pilot is not taken into account for BV
     * @param singleTrooper When true, returns the BV of a single trooper of this BA, otherwise of the full unit
     * @return The Battle Value of this BattleArmor
     */
    public int calculateBattleValue(boolean ignoreC3, boolean ignoreSkill, boolean singleTrooper) {
        if (useManualBV) {
            return manualBV; // TODO : divide by the number of troopers when singleTrooper is true?
        }
        return BattleArmorBVCalculator.calculateBV(this, ignoreC3, ignoreSkill,
                new DummyCalculationReport(), singleTrooper);
    }

    /**
     * Prepare the entity for a new round of action.
     */
    @Override
    public void newRound(int roundNumber) {
        // Perform all base-class behavior.
        super.newRound(roundNumber);

        // If we're equipped with a Magnetic Mine
        // launcher, turn it to single shot mode.
        for (Mounted m : getMisc()) {
            EquipmentType equip = m.getType();
            if (BattleArmor.MINE_LAUNCHER.equals(equip.getInternalName())) {
                m.setMode("Single");
            }
        }
    }

    /**
     * Update the unit to reflect damages taken in this phase.
     */
    @Override
    public void applyDamage() {
        super.applyDamage();
        int troopersAlive = 0;
        for (int i = 0; i < locations(); i++) {
            if (getInternal(i) > 0) {
                troopersAlive++;
            }
        }
        troopersShooting = troopersAlive;
    }

    /**
     * Get the number of men in the unit (before damage is applied).
     *
     * @see megamek.common.Infantry#getShootingStrength
     */
    @Override
    public int getShootingStrength() {
        return troopersShooting;
    }

    public void setCost(int inC) {
        myCost = inC;
    }

    /**
     * Determines if the battle armor unit is burdened with un-jettisoned
     * equipment. This can prevent the unit from jumping or using their special
     * Anti-Mek attacks.
     *
     * @return <code>true</code> if the unit hasn't jettisoned its equipment
     *         yet, <code>false</code> if it has.
     */
    public boolean isBurdened() {

        // Clan Elemental points are never burdened by equipment.
        if (!isClan()) {

            // if we have ammo left for a body mounted missile launcher,
            // we are burdened
            for (Mounted mounted : getAmmo()) {
                if (mounted.getUsableShotsLeft() == 0) {
                    // no shots left, we don't count
                    continue;
                }
                // first get the weapon we are linked by
                // (so we basically only check the currently loaded
                // ammo, but if the weapon has no currently loaded ammo, we're
                // fine
                Mounted weapon = mounted.getLinkedBy();
                if ((weapon != null) && weapon.isBodyMounted()
                        && weapon.getType().hasFlag(WeaponType.F_MISSILE)) {
                    return true;
                }
            } // Check the next piece of equipment

        } // End is-inner-sphere-squad

        // Unit isn't burdened.
        return false;
    }

    @Override
    public boolean canMakeAntiMekAttacks() {
        return !isBurdened() && canDoMechanizedBA()
                && (getWeightClass() < EntityWeightClass.WEIGHT_HEAVY)
                && (getMovementMode() != EntityMovementMode.INF_UMU);
    }

    /**
     * does this BA have an unjettisoned DWP?
     *
     * @return
     */
    public boolean hasDWP() {
        for (Mounted mounted : getWeaponList()) {
            if (mounted.isDWPMounted()) {
                if (mounted.isMissing()) {
                    continue;
                } else if ((mounted.getLinked() != null)
                        && (mounted.getLinked().getUsableShotsLeft() > 0)) {
                    return true;
                } else if ((mounted.getLinked() == null)
                        && !mounted.isMissing()) {
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * Returns true if this <code>BattleArmor</code> can use a detachable weapon
     * pack. A <code>BattleArmor</code> must have 2 or more walking MP and be
     * Medium or heavier to mount DWP.
     *
     * @return
     */
    public boolean canMountDWP() {
        return (getOriginalWalkMP() >= 2)
                && (getWeightClass() >= EntityWeightClass.WEIGHT_MEDIUM);
    }

    /**
     * Returns the name of the stealth Armor used by the BA. Mostly for
     * MegaMekLab Usage.
     *
     * @return name of the stealth armor.
     */
    public String getStealthName() {
        return stealthName;
    }

    public String getCamoName() {
        return camoName;
    }

    /**
     * Public interface to the BattleArmors short range stealth modifier
     *
     * @return shortStealthMod
     */
    public int getShortStealthMod() {
        return shortStealthMod;
    }

    /**
     * Public interface to the BattleArmors medium range stealth modifier
     *
     * @return mediumStealthMod
     */
    public int getMediumStealthMod() {
        return mediumStealthMod;
    }

    /**
     * Public interface to the BattleArmors long range stealth modifier
     *
     * @return longStealthMod
     */
    public int getLongStealthMod() {
        return longStealthMod;
    }

    // Only for ground vehicles and certain infantry
    @Override
    public boolean isEligibleForPavementBonus() {
        return false;
    }

    /**
     * Determine if this unit has an active stealth system.
     * <p>
     * Sub-classes are encouraged to override this method.
     *
     * @return <code>true</code> if this unit has a stealth system that is
     *         currently active, <code>false</code> if there is no stealth
     *         system or if it is inactive.
     */
    @Override
    public boolean isStealthActive() {
        return (isStealthy || isMimetic || hasCamoSystem);
    }

    public boolean isMimetic() {
        return isMimetic;
    }

    public boolean hasCamoSystem() {
        return hasCamoSystem;
    }

    @Override
    public boolean isStealthy() {
        return isStealthy;
    }

    /**
     * Determine the stealth modifier for firing at this unit from the given
     * range. If the value supplied for <code>range</code> is not one of the
     * <code>Entity</code> class range constants, an
     * <code>IllegalArgumentException</code> will be thrown.
     * <p>
     * Sub-classes are encouraged to override this method.
     *
     * @param range
     *            - an <code>int</code> value that must match one of the
     *            <code>Compute</code> class range constants.
     * @param ae
     *            - the entity making the attack.
     * @return a <code>TargetRoll</code> value that contains the stealth
     *         modifier for the given range.
     */
    @Override
    public TargetRoll getStealthModifier(int range, Entity ae) {
        TargetRoll result = null;

        // Note: infantry are immune to stealth, but not camoflage
        // or mimetic armor

        // Mimetic armor modifier is based upon the number of hexes moved,
        // and adds to existing movement modifier (Total Warfare p228):
        // 0 hexes moved +3 movement modifier
        // 1 hex moved +2 movement modifier
        // 2 hexes moved +1 movement modifier
        // 3+ hexes moved +0 movement modifier
        if (isMimetic && !hasMyomerBooster()) {
            int mmod = 3 - delta_distance;
            mmod = Math.max(0, mmod);
            result = new TargetRoll(mmod, "mimetic armor");
        }

        // Stealthy units alreay have their to-hit mods defined.
        if (isStealthy && !ae.isConventionalInfantry() && !hasMyomerBooster()) {
            switch (range) {
                case RangeType.RANGE_MINIMUM:
                case RangeType.RANGE_SHORT:
                    result = new TargetRoll(shortStealthMod, stealthName);
                    break;
                case RangeType.RANGE_MEDIUM:
                    result = new TargetRoll(mediumStealthMod, stealthName);
                    break;
                case RangeType.RANGE_LONG:
                case RangeType.RANGE_EXTREME:
                case RangeType.RANGE_LOS:
                    result = new TargetRoll(longStealthMod, stealthName);
                    break;
                case RangeType.RANGE_OUT:
                    break;
                default:
                    throw new IllegalArgumentException("Unknown range constant: " + range);
            }
        }

        // Simple camo modifier is on top of the movement modifier
        // 0 hexes moved +2 movement modifier
        // 1 hexes moved +1 movement modifier
        // 2+ hexes moved no modifier
        // This can also be in addition to any armor except Mimetic!
        if (hasCamoSystem && (delta_distance < 2)) {
            int mod = Math.max(2 - delta_distance, 0);
            if (result == null) {
                result = new TargetRoll(mod, "camoflage");
            } else {
                result.append(new TargetRoll(mod, "camoflage"));
            }
        }

        if (result == null) {
            result = new TargetRoll(0, "stealth not active");
        }

        // Return the result.
        return result;
    } // End public TargetRoll getStealthModifier( char )

    @Override
    public double getCost(CalculationReport calcReport, boolean ignoreAmmo) {
        return BattleArmorCostCalculator.calculateCost(this, calcReport, ignoreAmmo, true);
    }

    @Override
    public double getAlternateCost() {
        return BattleArmorCostCalculator.calculateCost(this, new DummyCalculationReport(), false,
                false);
    }

    @Override
    public boolean hasEiCockpit() {
        return true;
    }

    public void setWeightClass(int inWC) {
        switch (inWC) {
            case 0:
                weightClass = EntityWeightClass.WEIGHT_ULTRA_LIGHT;
                break;
            case 1:
                weightClass = EntityWeightClass.WEIGHT_LIGHT;
                break;
            case 2:
                weightClass = EntityWeightClass.WEIGHT_MEDIUM;
                break;
            case 3:
                weightClass = EntityWeightClass.WEIGHT_HEAVY;
                break;
            case 4:
                weightClass = EntityWeightClass.WEIGHT_ASSAULT;
                break;
        }
    }

    public double getTrooperWeight() {
        return EntityWeightClass.getClassLimit(getWeightClass(), this);
    }

    @Override
    public int getWeightClass() {
        return weightClass;
    }

    public int getTroopers() {
        return troopers;
    }

    public void setTroopers(int troopers) {
        this.troopers = troopers;
        // this is also squad size
        setSquadSize(troopers);
    }

    public void setChassisType(int inCT) {
        chassisType = inCT;
    }

    public int getChassisType() {
        return chassisType;
    }

    @Override
    public boolean canAssaultDrop() {
        return true;
    }

    @Override
    public boolean isNuclearHardened() {
        return true;
    }

    public boolean isTrooperActive(int trooperNum) {
        return (getInternal(trooperNum) > 0);
    }

    public int getNumberActiverTroopers() {
        int count = 0;
        // Initialize the troopers.
        for (int loop = 1; loop < locations(); loop++) {
            if (isTrooperActive(loop)) {
                count++;
            }
        }
        return count;
    }

    public int getRandomTrooper() {
        Vector<Integer> activeTroops = new Vector<>();
        for (int loop = 1; loop < locations(); loop++) {
            if (isTrooperActive(loop)) {
                activeTroops.add(loop);
            }
        }
        int locInt = Compute.randomInt(activeTroops.size());
        return activeTroops.elementAt(locInt);
    }

    @Override
    public boolean loadWeapon(Mounted mounted, Mounted mountedAmmo) {
        // BA must carry the ammo in same location as the weapon.
        // except for mine launcher mines
        // This allows for squad weapons and individual trooper weapons
        // such as NARC and the support weapons in TW/TO
        AmmoType at = (AmmoType) mountedAmmo.getType();
        if (!(at.getAmmoType() == AmmoType.T_MINE)
                && (mounted.getLocation() != mountedAmmo.getLocation())) {
            return false;
        }
        return super.loadWeapon(mounted, mountedAmmo);
    }

    @Override
    public boolean loadWeaponWithSameAmmo(Mounted mounted, Mounted mountedAmmo) {
        // BA must carry the ammo in same location as the weapon.
        // except for mine launcher mines
        // This allows for squad weapons and individual trooper weapons
        // such as NARC and the support weapons in TW/TO
        AmmoType at = (AmmoType) mountedAmmo.getType();
        if (!(at.getAmmoType() == AmmoType.T_MINE)
                && (mounted.getLocation() != mountedAmmo.getLocation())) {
            return false;
        }
        return super.loadWeaponWithSameAmmo(mounted, mountedAmmo);
    }

    public final String getBLK() {
        String newline = "\r\n";
        StringBuffer buff = new StringBuffer();
        buff.append("<BlockVersion>");
        buff.append(newline);
        buff.append("1");
        buff.append(newline);
        buff.append("</BlockVersion>");
        buff.append(newline);

        buff.append("<UnitType>");
        buff.append(newline);
        buff.append("BattleArmor");
        buff.append(newline);
        buff.append("</UnitType>");
        buff.append(newline);

        buff.append("<name>");
        buff.append(newline);
        buff.append(getChassis());
        buff.append(newline);
        buff.append("</name>");
        buff.append(newline);

        buff.append("<model>");
        buff.append(newline);
        buff.append(getModel());
        buff.append(newline);
        buff.append("</model>");
        buff.append(newline);

        buff.append("<year>");
        buff.append(newline);
        buff.append(getYear());
        buff.append(newline);
        buff.append("</year>");
        buff.append(newline);

        buff.append("<type>");
        buff.append(newline);
        switch (getTechLevel()) {
            case TechConstants.T_INTRO_BOXSET:
                buff.append("IS Level 1");
                break;
            case TechConstants.T_IS_TW_NON_BOX:
                buff.append("IS Level 2");
                break;
            case TechConstants.T_IS_ADVANCED:
                buff.append("IS Level 3");
                break;
            case TechConstants.T_IS_EXPERIMENTAL:
                buff.append("IS Level 4");
                break;
            case TechConstants.T_IS_UNOFFICIAL:
                buff.append("IS Level 5");
                break;
            case TechConstants.T_CLAN_TW:
                buff.append("Clan Level 2");
                break;
            case TechConstants.T_CLAN_ADVANCED:
                buff.append("Clan Level 3");
                break;
            case TechConstants.T_CLAN_EXPERIMENTAL:
                buff.append("Clan Level 4");
                break;
            case TechConstants.T_CLAN_UNOFFICIAL:
                buff.append("Clan Level 5");
                break;
        }
        buff.append(newline);
        buff.append("</type>");
        buff.append(newline);

        buff.append("<trooper count>");
        buff.append(newline);
        buff.append(getWeight());
        buff.append(newline);
        buff.append("</trooper count>");
        buff.append(newline);

        buff.append("<weightclass>");
        buff.append(newline);
        switch (getWeightClass()) {
            case EntityWeightClass.WEIGHT_ULTRA_LIGHT:
                buff.append("0");
                break;
            case EntityWeightClass.WEIGHT_LIGHT:
                buff.append("1");
                break;
            case EntityWeightClass.WEIGHT_MEDIUM:
                buff.append("2");
                break;
            case EntityWeightClass.WEIGHT_HEAVY:
                buff.append("3");
                break;
            case EntityWeightClass.WEIGHT_ASSAULT:
                buff.append("4");
                break;
        }
        buff.append(newline);
        buff.append("</weightclass>");
        buff.append(newline);

        buff.append("<motion_type>");
        buff.append(newline);
        switch (getMovementMode()) {
            case INF_JUMP:
                buff.append("jump");
                break;
            case INF_LEG:
                buff.append("leg");
                break;
            case VTOL:
                buff.append("vtol");
                break;
            case INF_UMU:
                buff.append("submarine");
                break;
            default:
                buff.append("none");
                break;
        }
        buff.append(newline);
        buff.append("</motion_type>");
        buff.append(newline);

        buff.append("<chassis>");
        buff.append(newline);
        switch (getChassisType()) {
            case BattleArmor.CHASSIS_TYPE_BIPED:
                buff.append("biped");
                break;
            case BattleArmor.CHASSIS_TYPE_QUAD:
                buff.append("quad");
                break;
        }
        buff.append("</chassis>");
        buff.append(newline);

        buff.append("<cruiseMP>");
        buff.append(newline);
        buff.append(getOriginalRunMP());
        buff.append(newline);
        buff.append("</cruiseMP>");
        buff.append(newline);

        buff.append("<jumpMP>");
        buff.append(newline);
        buff.append(getOriginalJumpMP());
        buff.append(newline);
        buff.append("</jumpMP>");
        buff.append(newline);

        buff.append("<armor>");
        buff.append(newline);
        buff.append(getOArmor(LOC_TROOPER_1));
        buff.append(newline);
        buff.append("</armor>");
        buff.append(newline);
        
        if (getTurretCapacity() > 0) {
            buff.append("<turret>");
            buff.append(newline);
            if (hasModularTurretMount()) {
                buff.append("Modular:");
            } else {
                buff.append("Standard:");
            }
            buff.append(getTurretCapacity());
            buff.append("</turret>");
            buff.append(newline);
        }

        buff.append("<armor_type>");
        buff.append(newline);
        buff.append(getArmorType(LOC_SQUAD));
        buff.append(newline);
        buff.append("</armor_type>");
        buff.append(newline);

        buff.append("<armor_tech>");
        buff.append(newline);
        buff.append(getArmorTechLevel(LOC_SQUAD));
        buff.append(newline);
        buff.append("</armor_tech>");
        buff.append(newline);

        for (int i = 0; i < locations(); i++) {
            boolean found = false;
            for (Mounted m : getEquipment()) {
                // don't write out swarm and leg attack, those get added
                // dynamically
                if ((m.getType() instanceof WeaponType)
                        && m.getType().hasFlag(WeaponType.F_INFANTRY_ATTACK)) {
                    continue;
                }
                if (m.getLocation() == i) {
                    if (!found) {
                        found = true;
                        buff.append("<");
                        buff.append(getLocationName(i));
                        buff.append(" equipment>");
                        buff.append(newline);
                    }
                    buff.append(m.getType().getInternalName());
                    buff.append(newline);
                }
            }
            if (found) {
                buff.append("</");
                buff.append(getLocationName(i));
                buff.append(" equipment>");
                buff.append(newline);
            }
        }

        return buff.toString();
    }

    /*
     * (non-Javadoc)
     *
     * @see megamek.common.Entity#getVibroClaws()
     */
    @Override
    public int getVibroClaws() {
        int claws = 0;
        for (Mounted mounted : getMisc()) {
            if (mounted.getType().hasFlag(MiscType.F_VIBROCLAW)) {
                claws++;
            }
        }
        return claws;
    }

    /**
     * return if this BA has fire resistant armor
     *
     * @return
     */
    public boolean isFireResistant() {
        for (Mounted equip : getMisc()) {
            if (equip.getType().hasFlag(MiscType.F_FIRE_RESISTANT)) {
                return true;
            }
        }
        return false;
    }

    /**
     * return if this BA has laser reflective armor
     *
     * @return
     */
    public boolean isReflective() {
        for (Mounted equip : getMisc()) {
            if (equip.getType().hasFlag(MiscType.F_REFLECTIVE)) {
                return true;
            }
        }
        return false;
    }

    /**
     * return if this BA has reactive armor
     * @return
     */
    public boolean isReactive() {
        for (Mounted equip : getMisc()) {
            if (equip.getType().hasFlag(MiscType.F_REACTIVE)) {
                return true;
            }
        }
        return false;
    }

    /**
     * return if this BA has improved sensors
     *
     * @return
     */
    public boolean hasImprovedSensors() {
        for (Mounted equip : getMisc()) {
            if (equip.getType().hasFlag(MiscType.F_BAP)) {
                if (equip.getType().getInternalName().equals(Sensor.ISIMPROVED)
                        || equip.getType().getInternalName()
                        .equals(Sensor.CLIMPROVED)) {
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * return if the BA has any kind of active probe
     *
     * @return
     */
    public boolean hasActiveProbe() {
        for (Mounted equip : getMisc()) {
            if (equip.getType().hasFlag(MiscType.F_BAP)
                    && !(equip.getType().getInternalName()
                            .equals(Sensor.ISIMPROVED) || equip.getType()
                            .getInternalName().equals(Sensor.CLIMPROVED))) {
                return true;
            }
        }
        return false;
    }

    /*
     * (non-Javadoc)
     *
     * @see megamek.common.Entity#canTransferCriticals(int)
     */
    @Override
    public boolean canTransferCriticals(int loc) {
        // BAs can never transfer crits
        return false;
    }

    /**
     * can this BattleArmor ride as Mechanized BA?
     *
     * @return
     */
    public boolean canDoMechanizedBA() {
        if (getChassisType() != CHASSIS_TYPE_QUAD) {
            if (hasWorkingMisc(MiscType.F_MAGNETIC_CLAMP)) {
                return true;
            }
            int tBasicManipulatorCount = countWorkingMisc(MiscType.F_BASIC_MANIPULATOR);
            int tArmoredGloveCount = countWorkingMisc(MiscType.F_ARMORED_GLOVE);
            int tBattleClawCount = countWorkingMisc(MiscType.F_BATTLE_CLAW);
            switch (getWeightClass()) {
                case EntityWeightClass.WEIGHT_ULTRA_LIGHT:
                case EntityWeightClass.WEIGHT_LIGHT:
                    if ((tArmoredGloveCount > 1)
                            || (tBasicManipulatorCount > 0)
                            || (tBattleClawCount > 0)) {
                        return true;
                    }
                    break;
                case EntityWeightClass.WEIGHT_MEDIUM:
                case EntityWeightClass.WEIGHT_HEAVY:
                    if ((tBasicManipulatorCount > 0) || (tBattleClawCount > 0)) {
                        return true;
                    }
                    break;
                case EntityWeightClass.WEIGHT_ASSAULT:
                default:
                    return false;
            }
        }
        return false;
    }

    @Override
    public double getWeight() {
        // If following Total Warfare rules each BA trooper will weigh a ton
        // for transport purposes. Following Tactical Operations gives us a
        // more realistic weight per trooper
        if ((game != null)
                && game.getOptions().booleanOption(OptionsConstants.ADVANCED_TACOPS_BA_WEIGHT)) {
            double troopton;
            switch (getWeightClass()) {
                case EntityWeightClass.WEIGHT_ULTRA_LIGHT:
                    troopton = troopers * 0.25;
                    break;
                case EntityWeightClass.WEIGHT_LIGHT:
                    troopton = troopers * 0.5;
                    break;
                case EntityWeightClass.WEIGHT_MEDIUM:
                    troopton = troopers * 1.0;
                    break;
                case EntityWeightClass.WEIGHT_HEAVY:
                    troopton = troopers * 1.5;
                    break;
                case EntityWeightClass.WEIGHT_ASSAULT:
                    troopton = troopers * 2.0;
                    break;
                default:
                    troopton = troopers;
                    break;
            }
            return troopton;
        } else {
            return troopers;
        }
    }

    public double getAlternateWeight() {
        double troopton;
        switch (getWeightClass()) {
            case EntityWeightClass.WEIGHT_ULTRA_LIGHT:
                troopton = troopers * 0.25;
                break;
            case EntityWeightClass.WEIGHT_LIGHT:
                troopton = troopers * 0.5;
                break;
            case EntityWeightClass.WEIGHT_MEDIUM:
                troopton = troopers * 1.0;
                break;
            case EntityWeightClass.WEIGHT_HEAVY:
                troopton = troopers * 1.5;
                break;
            case EntityWeightClass.WEIGHT_ASSAULT:
                troopton = troopers * 2.0;
                break;
            default:
                troopton = troopers;
                break;
        }
        return troopton;
    }

    @Override
    public int getWeaponArc(int wn) {
        return Compute.ARC_360;
    }

    @Override
    public boolean isHardenedArmorDamaged(HitData hit) {
        return false;
    }

    @Override
    public boolean hasPatchworkArmor() {
        return false;
    }

    public void setIsExoskeleton(boolean exoskeleton) {
        this.exoskeleton = exoskeleton;
    }

    public boolean isExoskeleton() {
        return exoskeleton;
    }

    @Override
    public boolean isCrippled() {
        double activeTroopPercent = (double) getNumberActiverTroopers() / getSquadSize();
        if (activeTroopPercent < 0.5) {
            LogManager.getLogger().debug(getDisplayName() + " CRIPPLED: Only "
                    + NumberFormat.getPercentInstance().format(activeTroopPercent) + " troops remaining.");
            return true;
        } else {
            return false;
        }
    }

    @Override
    public boolean isDmgHeavy() {
        return (((double) getNumberActiverTroopers() / getSquadSize()) < 0.67);
    }

    @Override
    public boolean isDmgModerate() {
        return (((double) getNumberActiverTroopers() / getSquadSize()) < 0.75);
    }

    @Override
    public boolean isDmgLight() {
        return (((double) getNumberActiverTroopers() / getSquadSize()) < 0.9);
    }

    public int calculateSwarmDamage() {
        int damage = 0;
        for (Mounted m : getWeaponList()) {
            WeaponType wtype;
            if (m.getType() instanceof WeaponType) {
                wtype = (WeaponType) m.getType();
                if (wtype.hasFlag(WeaponType.F_MISSILE)) {
                    continue;
                }
                if (m.isBodyMounted()) {
                    continue;
                }
                if (wtype instanceof InfantryWeapon) {
                    continue;
                }
                if (wtype instanceof InfantryAttack) {
                    continue;
                }
                int addToDamage = wtype.getDamage(0);
                if (addToDamage < 0) {
                    continue;
                }
                // if it's a squad mounted weapon, each trooper hits
                if (m.getLocation() == BattleArmor.LOC_SQUAD) {
                    addToDamage *= getShootingStrength();
                }
                damage += addToDamage;
            }
        }
        if (hasMyomerBooster()) {
            damage += getTroopers() * 2;
        }
        
        // we only track vibro claws at the squad level, so we have either 0, 1 or 2. See TW page 223.
        damage += getVibroClaws();
        
        return damage;
    }

    @Override
    public boolean isConventionalInfantry() {
        return false;
    }

    @Override
    public long getEntityType() {
        return Entity.ETYPE_INFANTRY | Entity.ETYPE_BATTLEARMOR;
    }

    public int getMaximumJumpMP() {
        return getMaximumJumpMP(false);
    }

    /**
     * Returns the maximum jump MP that this BA can have.
     *
     * @param ignoreEquipment
     *            If true, bonuses from equipment like partial wing and jump
     *            booster are ignored. This is important for construction
     *            purposes, where we shouldn't allow the JSpinner to select
     *            these values.
     * @return
     */
    public int getMaximumJumpMP(boolean ignoreEquipment) {
        if (chassisType == CHASSIS_TYPE_QUAD) {
            return 0;
        }
        int max = 2;
        if (getMovementMode() == EntityMovementMode.INF_UMU) {
            if (getWeightClass() <= EntityWeightClass.WEIGHT_LIGHT) {
                max = 5;
            } else if (getWeightClass() == EntityWeightClass.WEIGHT_MEDIUM) {
                max = 4;
            } else if (getWeightClass() == EntityWeightClass.WEIGHT_HEAVY) {
                max = 3;
            } else {
                max = 2;
            }
        }
        else if (getMovementMode() == EntityMovementMode.VTOL) {
            if (getWeightClass() == EntityWeightClass.WEIGHT_ULTRA_LIGHT) {
                max = 7;
            } else if (getWeightClass() == EntityWeightClass.WEIGHT_LIGHT) {
                max = 6;
            } else if (getWeightClass() == EntityWeightClass.WEIGHT_MEDIUM) {
                max = 5;
            } else {
                max = 0;
            }
        } else {
            if (getWeightClass() < EntityWeightClass.WEIGHT_HEAVY) {
                max = 3;
            }
        }

        // Partial wings and jump boosters add 1 jump MP and can increase it
        //  over the max and they cannot be used together
        if (!ignoreEquipment
                && (hasWorkingMisc(MiscType.F_JUMP_BOOSTER)
                        || hasWorkingMisc(MiscType.F_PARTIAL_WING))) {
            max++;
        }

        return max;
    }

    public int getMaximumWalkMP() {
        int max = 2;
        if (getWeightClass() < EntityWeightClass.WEIGHT_HEAVY) {
            max = 3;
        }

        if (chassisType == CHASSIS_TYPE_QUAD) {
            max += 2;
        }

        // Mechanical jump boosters add 1 MP and can increase it over the max
        if (hasWorkingMisc(MiscType.F_MECHANICAL_JUMP_BOOSTER)) {
            max++;
        }

        if (hasMyomerBooster()) {
            switch (getWeightClass()) {
                case EntityWeightClass.WEIGHT_ULTRA_LIGHT:
                case EntityWeightClass.WEIGHT_LIGHT:
                case EntityWeightClass.WEIGHT_MEDIUM:
                    max += 2;
                    break;
                case EntityWeightClass.WEIGHT_HEAVY:
                case EntityWeightClass.WEIGHT_ASSAULT:
                    max++;
                    break;
            }
        }

        return max;
    }

    public int getMaximumArmorPoints() {
        switch (getWeightClass()) {
            case EntityWeightClass.WEIGHT_ULTRA_LIGHT:
                return 2;
            case EntityWeightClass.WEIGHT_LIGHT:
                return 6;
            case EntityWeightClass.WEIGHT_MEDIUM:
                return 10;
            case EntityWeightClass.WEIGHT_HEAVY:
                return 14;
            case EntityWeightClass.WEIGHT_ASSAULT:
                return 18;
            default:
                return 0;
        }
    }

    @Override
    public void setArmorType(int armType) {
        for (int i = 0; i < 7; i++) {
            armorType[i] = armType;
        }

        // Set some special state for certain armor types
        if (armType == EquipmentType.T_ARMOR_BA_STEALTH_BASIC) {
            isStealthy = true;
            shortStealthMod = 0;
            mediumStealthMod = 1;
            longStealthMod = 2;
            stealthName = BattleArmor.BASIC_STEALTH_ARMOR;
        } else if (armType == EquipmentType.T_ARMOR_BA_STEALTH_PROTOTYPE) {
            isStealthy = true;
            shortStealthMod = 0;
            mediumStealthMod = 1;
            longStealthMod = 2;
            stealthName = BattleArmor.STEALTH_PROTOTYPE;
        } else if (armType == EquipmentType.T_ARMOR_BA_STEALTH) {
            isStealthy = true;
            shortStealthMod = 1;
            mediumStealthMod = 1;
            longStealthMod = 2;
            stealthName = BattleArmor.STANDARD_STEALTH_ARMOR;
        } else if (armType == EquipmentType.T_ARMOR_BA_STEALTH_IMP) {
            isStealthy = true;
            shortStealthMod = 1;
            mediumStealthMod = 2;
            longStealthMod = 3;
            stealthName = BattleArmor.IMPROVED_STEALTH_ARMOR;
        } else if (armType == EquipmentType.T_ARMOR_BA_MIMETIC) {
            isMimetic = true;
            stealthName = BattleArmor.MIMETIC_ARMOR;
        }
    }

    public int getArmCrits() {
        if (getChassisType() == CHASSIS_TYPE_QUAD) {
            return 0;
        }

        switch (getWeightClass()) {
            case EntityWeightClass.WEIGHT_ULTRA_LIGHT:
            case EntityWeightClass.WEIGHT_LIGHT:
                return 2;
            case EntityWeightClass.WEIGHT_MEDIUM:
            case EntityWeightClass.WEIGHT_HEAVY:
                return 3;
            default:
                return 4;
        }
    }
    
    public int getBodyCrits() {
        if (getChassisType() == CHASSIS_TYPE_QUAD) {
            int turret = 0;
            if (getTurretCapacity() > 0) {
                turret = 1;
                if (hasModularTurretMount()) {
                    turret++;
                }
            }

            switch (getWeightClass()) {
                case EntityWeightClass.WEIGHT_ULTRA_LIGHT:
                    return 0;
                case EntityWeightClass.WEIGHT_LIGHT:
                    return 5 - turret;
                case EntityWeightClass.WEIGHT_MEDIUM:
                    return 7 - turret;
                case EntityWeightClass.WEIGHT_HEAVY:
                    return 9 - turret;
                default:
                    return 11 - turret;
            }
        } else {
            switch (getWeightClass()) {
                case EntityWeightClass.WEIGHT_ULTRA_LIGHT:
                    return 2;
                case EntityWeightClass.WEIGHT_LIGHT:
                case EntityWeightClass.WEIGHT_MEDIUM:
                    return 4;
                default:
                    return 6;
            }
        }
    }
    
    public int getTurretCapacity() {
        return turretSize;
    }
    
    public void setTurretSize(int capacity) {
        turretSize = capacity;
    }
    
    public boolean hasModularTurretMount() {
        return modularTurret;
    }
    
    public void setModularTurret(boolean modular) {
        modularTurret = modular;
    }

    public int getTotalCrits() {
        return (getArmCrits() * 2) + getBodyCrits() + getTurretCapacity();
    }

    public int getNumCrits(int loc) {
        if (loc == MOUNT_LOC_BODY) {
            return getBodyCrits();
        } else if ((loc == MOUNT_LOC_LARM) || (loc == MOUNT_LOC_RARM)) {
            return getArmCrits();
        } else if (loc == MOUNT_LOC_TURRET) {
            return getTurretCapacity();
        } else {
            return 0;
        }
    }

    /**
     * Returns the number of allowed anti-mech weapons the supplied location
     * can mount.  The body can mount a set number of anti-mech weapons and a
     * set number of anti-personnel, however for the arms can mount 2 AP or
     * 1 AP and 1 AM.
     *
     * @param loc
     * @return
     */
    public int getNumAllowedAntiMechWeapons(int loc) {
        if ((loc == MOUNT_LOC_LARM) || (loc == MOUNT_LOC_RARM)) {
            return 1;
        } else if ((loc == MOUNT_LOC_BODY) || (loc == MOUNT_LOC_TURRET)) {
            if (getChassisType() == CHASSIS_TYPE_QUAD) {
                return 4;
            } else {
                return 2;
            }
        } else {
            return 0;
        }
    }

    /**
     * Returns the number of allowed anti-personnel weapons the location can
     * mount. The body can mount a set number of anti-mech weapons and a set
     * number of anti-personnel, however the arms can mount 2 AP or 1 AP and 1
     * AM.
     *
     * @param loc
     * @return
     */
    public int getNumAllowedAntiPersonnelWeapons(int loc, int trooper) {
        if ((loc == MOUNT_LOC_LARM) || (loc == MOUNT_LOC_RARM)) {
            boolean hasAntiMech = false;
            for (Mounted m : getWeaponList()) {
                if (!m.getType().hasFlag(WeaponType.F_INFANTRY)
                        && (m.getBaMountLoc() == loc)
                        && ((m.getLocation() == LOC_SQUAD) || (m.getLocation() == trooper))) {
                    hasAntiMech = true;
                }
            }
            if (hasAntiMech) {
                return 1;
            } else {
                return 2;
            }
        } else if (loc == MOUNT_LOC_BODY) {
            if (getChassisType() == CHASSIS_TYPE_QUAD) {
                return 4;
            } else {
                return 2;
            }
        } else {
            return 0;
        }
    }

    @Override
    public boolean doomedInExtremeTemp() {
        return false;
    }

    @Override
    public boolean doomedInVacuum() {
        return false;
    }

    /**
     * Convenience method for determining if the BA has magnetic clamps.
     *
     * @return true if the unit has at least one magnetic clamp, else false
     */
    public boolean hasMagneticClamps() {
        return countWorkingMisc(MiscType.F_MAGNETIC_CLAMP) > 0;
    }

    /**
     * Returns the <code>EquipmentType</code> internal name for the manipulator
     * mounted in the left arm of this <code>BattleArmor</code> squad.
     *
     * @return
     */
    public String getLeftManipulatorName() {
        Mounted m = getLeftManipulator();
        if (m == null) {
            return MANIPULATOR_TYPE_STRINGS[MANIPULATOR_NONE];
        } else {
            return m.getType().getInternalName();
        }
    }

    /**
     * Returns the <code>EquipmentType</code> internal name for the manipulator
     * mounted in the right arm of this <code>BattleArmor</code> squad.
     *
     * @return
     */
    public String getRightManipulatorName() {
        Mounted m = getRightManipulator();
        if (m == null) {
            return MANIPULATOR_TYPE_STRINGS[MANIPULATOR_NONE];
        } else {
            return m.getType().getInternalName();
        }
    }

    /**
     * Returns the <code>Mounted</code> for the manipulator
     * mounted in the left arm of this <code>BattleArmor</code> squad.
     *
     * @return
     */
    public Mounted getLeftManipulator() {
        for (Mounted m : getMisc()) {
            if (m.getType().hasFlag(MiscType.F_BA_MANIPULATOR)
                    && (m.getBaMountLoc() == MOUNT_LOC_LARM)) {
                return m;
            }
        }
        return null;
    }

    /**
     * Returns the <code>Mounted</code> for the manipulator
     * mounted in the right arm of this <code>BattleArmor</code> squad.
     *
     * @return
     */
    public Mounted getRightManipulator() {
        for (Mounted m : getMisc()) {
            if (m.getType().hasFlag(MiscType.F_BA_MANIPULATOR)
                    && (m.getBaMountLoc() == MOUNT_LOC_RARM)) {
                return m;
            }
        }
        return null;
    }

    public boolean isClanExoWithoutHarjel() {
        return clanExoWithoutHarjel;
    }

    public void setClanExoWithoutHarjel(boolean clanExoWithoutHarjel) {
        this.clanExoWithoutHarjel = clanExoWithoutHarjel;
    }

    @Override
    public String getLocationDamage(int loc) {
        String toReturn = "";
        if (getInternal(loc) < 0) {
            return toReturn;
        }
        boolean first = true;
        for (Mounted m : getEquipment()) {
            if (m.isMissingForTrooper(loc)) {
                if (!first) {
                    toReturn += ", ";
                }
                toReturn += m.getName();
                first = false;
            }
        }
        return toReturn;
    }

    /**
     * Used to determine the draw priority of different Entity subclasses.
     * This allows different unit types to always be draw above/below other
     * types.
     *
     * @return
     */
    @Override
    public int getSpriteDrawPriority() {
        return 2;
    }

    @Override
    protected boolean isFieldWeapon(Mounted equipment) {
        return false;
    }

    @Override
    public boolean isBattleArmor() {
        return true;
    }
}

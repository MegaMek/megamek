/*
 * MegaMek - Copyright (C) 2002,2003,2004 Ben Mazur (bmazur@sev.org)
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

import java.util.Vector;

import megamek.common.weapons.InfantryAttack;
import megamek.common.weapons.infantry.InfantryWeapon;

/**
 * This class represents a squad or point of battle armor equiped infantry,
 * sometimes referred to as "Elementals". Much of the behaviour of a battle
 * armor unit is identical to that of an infantry platoon, and is rather
 * different than that of a Mek or Tank.
 *
 * @author Suvarov454@sourceforge.net (James A. Damour )
 * @version $revision:$
 */
/*
 * PLEASE NOTE!!! My programming style is to put constants first in tests so the
 * compiler catches my "= for ==" errors.
 */
public class BattleArmor extends Infantry {
    /**
     *
     */
    private static final long serialVersionUID = 4594311535026187825L;
    /*
     * Infantry have no critical slot limitations. IS squads usually have 4 men,
     * Clan points usually have 5. Have a location that represents the entire
     * squad.
     */
    private static final int[] IS_NUM_OF_SLOTS =
        { 7, 2, 2, 2, 2, 2, 2 };
    private static final String[] IS_LOCATION_ABBRS =
        { "Squad", "Trooper 1", "Trooper 2", "Trooper 3", "Trooper 4", "Trooper 5", "Trooper 6" };
    private static final String[] IS_LOCATION_NAMES =
        { "Squad", "Trooper 1", "Trooper 2", "Trooper 3", "Trooper 4", "Trooper 5", "Trooper 6" };
    private static final int[] CLAN_NUM_OF_SLOTS =
        { 10, 2, 2, 2, 2, 2, 2 };
    private static final String[] CLAN_LOCATION_ABBRS =
        { "Point", "Trooper 1", "Trooper 2", "Trooper 3", "Trooper 4", "Trooper 5", "Trooper 6" };
    private static final String[] CLAN_LOCATION_NAMES =
        { "Point", "Trooper 1", "Trooper 2", "Trooper 3", "Trooper 4", "Trooper 5", "Trooper 6" };

    // these only used by custom ba dialog
    public static final int MANIPULATOR_NONE = 0;
    public static final int MANIPULATOR_ARMORED_GLOVE = 1;
    public static final int MANIPULATOR_BASIC = 2;
    public static final int MANIPULATOR_BASIC_MINE_CLEARANCE = 3;
    public static final int MANIPULATOR_BATTLE = 4;
    public static final int MANIPULATOR_BATTLE_MAGNET = 5;
    public static final int MANIPULATOR_BATTLE_VIBRO = 6;
    public static final int MANIPULATOR_HEAVY_BATTLE = 7;
    public static final int MANIPULATOR_HEAVY_BATTLE_VIBRO = 8;
    public static final int MANIPULATOR_SALVAGE_ARM = 9;
    public static final int MANIPULATOR_CARGO_LIFTER = 10;
    public static final int MANIPULATOR_INDUSTRIAL_DRILL = 11;

    // these only used by custom ba dialog
    public static final String[] MANIPULATOR_TYPE_STRINGS =
        { "None", "Armored Glove", "Basic Manipulator", "Basic Manipulator (Mine Clearance)", "Battle Claw", "Battle Claw (Magnets)", "Battle Claw (Vibro-Claws)", "Heavy Battle Claw", "Heavy Battle Claw (Vibro-Claws)", "Salvage Arm", "Cargo Lifter", "Industrial Drill" };

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
     * The weight of a single trooper
     */
    private float trooperWeight = -1.0f;

    /**
     * The cost of this unit. This value should be set when the unit's file is
     * read.
     */
    private int myCost = -1;
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
     * The internal name for basic Stealth armor.
     */
    public static final String BASIC_STEALTH_ARMOR = "Basic Stealth";

    /**
     * The internal name for standard Stealth armor.
     */
    public static final String STANDARD_STEALTH_ARMOR = "Standard Stealth";

    /**
     * The internal name for improved Stealth armor.
     */
    public static final String IMPROVED_STEALTH_ARMOR = "Improved Stealth";

    /**
     * The internal name for Mimetic armor.
     */
    public static final String MIMETIC_ARMOR = "Mimetic Armor";

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

    private boolean exoskeleton = false;

    @Override
    public String[] getLocationAbbrs() {
        if (!isInitialized || isClan()) {
            return CLAN_LOCATION_ABBRS;
        }
        return IS_LOCATION_ABBRS;
    }

    @Override
    public String[] getLocationNames() {
        if (!isInitialized || isClan()) {
            return CLAN_LOCATION_NAMES;
        }
        return IS_LOCATION_NAMES;
    }

    /**
     * Returns the number of locations in this unit.
     */
    @Override
    public int locations() {
        int retVal = Math.round(getTroopers());
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

        // BA are always one squad
        squadn = 1;

        // All Battle Armor squads are Clan until specified otherwise.
        setTechLevel(TechConstants.T_CLAN_TW);

        // Construction complete.
        isInitialized = true;
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
    public int getWalkMP(boolean gravity, boolean ignoreheat, boolean ignoremodulararmor) {
        return getWalkMP(gravity, ignoreheat, ignoremodulararmor, false, false);
    }

    public int getWalkMP(boolean gravity, boolean ignoreheat, boolean ignoremodulararmor, boolean ignoreDWP, boolean ignoreMyomerBooster) {
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
            int weatherMod = game.getPlanetaryConditions().getMovementMods(this);
            if (weatherMod != 0) {
                j = Math.max(j + weatherMod, 0);
            }
        }
        if (gravity) {
            j = applyGravityEffectsOnMP(j);
        }
        return j;
    }

    @Override
    public int getRunMP(boolean gravity, boolean ignoreheat, boolean ignoremodulararmor) {
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
    public int getJumpMP(boolean gravity, boolean ignoreBurden, boolean ignoreDWP) {
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
        int mp = getOriginalJumpMP();

        // partial wing gives extra MP in atmosphere
        if ((mp > 0) && hasWorkingMisc(MiscType.F_PARTIAL_WING) && ((game == null) || !game.getPlanetaryConditions().isVacuum())) {
            mp++;
        } else if ((mp > 0) && hasWorkingMisc(MiscType.F_JUMP_BOOSTER)) {
            // jump booster gives an extra MP
            mp++;
        }
        if (getMovementMode() == EntityMovementMode.INF_UMU) {
            mp = 0;
        }
        // if we have no normal jump jets, we get 1 jump MP from mechanical jump
        // boosters, if we have them.
        // we do this after the UMU check, because jump boosters work underwater
        if ((mp == 0) && hasWorkingMisc(MiscType.F_MECHANICAL_JUMP_BOOSTER)) {
            mp++;
            // partial wing gives extra MP in atmosphere
            if (hasWorkingMisc(MiscType.F_PARTIAL_WING) && ((game == null) || (!game.getPlanetaryConditions().isVacuum()))) {
                mp++;
            }
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
    public HitData rollHitLocation(int table, int side, int aimedLocation, int aimingMode, int cover) {

        // If this squad was killed, target trooper 1 (just because).
        if (isDoomed()) {
            return new HitData(1);
        }

        if ((aimedLocation != LOC_NONE) && (aimingMode != IAimingModes.AIM_MODE_NONE)) {

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
        // a squad,
        // modifying previous ruling in the AskThePM FAQ.
        while ((loc >= locations()) || (IArmorState.ARMOR_NA == this.getInternal(loc)) || (IArmorState.ARMOR_DESTROYED == this.getInternal(loc)) || ((IArmorState.ARMOR_DOOMED == this.getInternal(loc)) && !isDoomed())) {
            loc = Compute.d6();
        }

        int critLocation = Compute.d6();
        // TacOps p. 108 Trooper takes a crit if a second roll is the same
        // location as the first.
        if (game.getOptions().booleanOption("tacops_ba_criticals") && (loc == critLocation)) {
            return new HitData(loc, false, HitData.EFFECT_CRITICAL);
        }
        // Hit that trooper.
        return new HitData(loc);

    }

    @Override
    public HitData rollHitLocation(int table, int side) {
        return rollHitLocation(table, side, LOC_NONE, IAimingModes.AIM_MODE_NONE, LosEffects.COVER_NONE);
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
                    if ((getInternal(5) > 0) && (getInternal(6) > 0)) {
                        loc = Compute.randomInt(2) + 5;
                    } else if (getInternal(5) > 0) {
                        loc = 5;
                    } else {
                        loc = 6;
                    }
                    break;
            }
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
     * Battle Armor units use default behavior for armor and internals.
     *
     * @see megamek.common.Infantry#isPlatoon()
     */
    @Override
    protected boolean isPlatoon() {
        return false;
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
     * @see megamek.common.Infantry#getInternal(int )
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
     * @see megamek.common.Infantry#getOInternal(int )
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
    public void addEquipment(Mounted mounted, int loc, boolean rearMounted) throws LocationFullException {
        // Implement parent's behavior.
        super.addEquipment(mounted, loc, rearMounted);

        // Is the item a stealth equipment?
        // TODO: what's the *real* extreme range modifier?
        String name = mounted.getType().getInternalName();
        if (BattleArmor.BASIC_STEALTH_ARMOR.equals(name)) {
            isStealthy = true;
            shortStealthMod = 0;
            mediumStealthMod = 1;
            longStealthMod = 2;
            stealthName = name;
        } else if (BattleArmor.STANDARD_STEALTH_ARMOR.equals(name)) {
            isStealthy = true;
            shortStealthMod = 1;
            mediumStealthMod = 1;
            longStealthMod = 2;
            stealthName = name;
        } else if (BattleArmor.IMPROVED_STEALTH_ARMOR.equals(name)) {
            isStealthy = true;
            shortStealthMod = 1;
            mediumStealthMod = 2;
            longStealthMod = 3;
            stealthName = name;
        } else if (BattleArmor.MIMETIC_ARMOR.equals(name)) {
            isMimetic = true;
            stealthName = name;
        } else if (BattleArmor.CAMO_SYSTEM.equals(name)) {
            hasCamoSystem = true;
            camoName = name;
        }

        // If the BA can swarm, they're anti-mek.
        if (Infantry.SWARM_MEK.equals(name)) {
            setAntiMek(true);
        }
    }

    /**
     * Battle Armor units have as many critical slots as they need to hold their
     * equipment.
     */
    @Override
    protected int[] getNoOfSlots() {
        if (!isInitialized || isClan()) {
            return CLAN_NUM_OF_SLOTS;
        }
        return IS_NUM_OF_SLOTS;
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

    /**
     * Calculates the battle value of this platoon.
     */
    @Override
    public int calculateBattleValue(boolean ignoreC3, boolean ignorePilot) {
        if (useManualBV) {
            return manualBV;
        }
        return calculateBattleValue(ignoreC3, ignorePilot, false);
    }

    /**
     * Calculates the battle value of this platoon.
     *
     * @param ignoreC3
     *            ignore C3 linkage
     * @param ignorePilot
     *            ignore the skill of the pilot
     * @param singleTrooper
     *            calculate just the BV of a single trooper
     * @return the battlevalue
     */
    public int calculateBattleValue(boolean ignoreC3, boolean ignorePilot, boolean singleTrooper) {
        if (useManualBV) {
            return manualBV;
        }
        // we do this per trooper, then add up
        double squadBV = 0;
        for (int i = 1; i < locations(); i++) {
            if (this.getInternal(i) <= 0) {
                continue;
            }
            double dBV = 0;
            double armorBV = 2.5;
            if (isFireResistant()) {
                armorBV = 3.5;
            }
            dBV += (getArmor(i) * armorBV) + 1;
            // improved sensors add 1
            if (hasImprovedSensors()) {
                dBV += 1;
            }
            // active probes add 1
            if (hasActiveProbe()) {
                dBV += 1;
            }
            // ECM adds 1
            for (Mounted mounted : getMisc()) {
                if (mounted.getType().hasFlag(MiscType.F_ECM)) {
                    dBV += 1;
                    break;
                }
            }
            int runMP = getWalkMP(false, false, true, false, true);
            int tmmRan = Compute.getTargetMovementModifier(runMP, false, false).getValue();
            // get jump MP, ignoring burden
            int rawJump = getJumpMP(false, true, true);
            int tmmJumped = Compute.getTargetMovementModifier(rawJump, true, false).getValue();
            double targetMovementModifier = Math.max(tmmRan, tmmJumped);
            double tmmFactor = 1 + (targetMovementModifier / 10) + 0.1;
            if (hasCamoSystem) {
                tmmFactor += 0.2;
            }
            if (isStealthy) {
                tmmFactor += 0.2;
            }
            // improved stealth get's an extra 0.1, for 0.3 total
            if ((stealthName != null) && stealthName.equals(BattleArmor.IMPROVED_STEALTH_ARMOR)) {
                tmmFactor += 0.1;
            }
            if (isMimetic) {
                tmmFactor += 0.3;
            }

            dBV *= tmmFactor;
            double oBV = 0;
            for (Mounted weapon : getWeaponList()) {
                // infantry weapons don't count at all
                if (weapon.getType().hasFlag(WeaponType.F_INFANTRY)) {
                    continue;
                }
                if (weapon.getLocation() == LOC_SQUAD) {
                    oBV += weapon.getType().getBV(this);
                } else {
                    // squad support, count at 1/troopercount
                    oBV += weapon.getType().getBV(this) / getTotalOInternal();
                }
            }

            for (Mounted misc : getMisc()) {
                if (misc.getType().hasFlag(MiscType.F_MINE)) {
                    if (misc.getLocation() == LOC_SQUAD) {
                        oBV += misc.getType().getBV(this);
                    } else {
                        oBV += misc.getType().getBV(this) / getTotalOInternal();
                    }
                }
            }
            for (Mounted ammo : getAmmo()) {
                int loc = ammo.getLocation();
                // don't count oneshot ammo
                if (loc == LOC_NONE) {
                    continue;
                }
                if ((loc == LOC_SQUAD) || (loc == i)) {
                    double ammoBV = ((AmmoType) ammo.getType()).getBABV();
                    oBV += ammoBV;
                }
            }
            if (isAntiMek()) {
                // all non-missile and non-body mounted direct fire weapons
                // counted again
                for (Mounted weapon : getWeaponList()) {
                    // infantry weapons don't count at all
                    if (weapon.getType().hasFlag(WeaponType.F_INFANTRY)) {
                        continue;
                    }
                    if (weapon.getLocation() == LOC_SQUAD) {
                        if (!weapon.getType().hasFlag(WeaponType.F_MISSILE) && !weapon.isBodyMounted()) {
                            oBV += weapon.getType().getBV(this);
                        }
                    } else {
                        // squad support, count at 1/troopercount
                        oBV += weapon.getType().getBV(this) / getTotalOInternal();
                    }
                }
                // magnetic claws and vibro claws counted again
                for (Mounted misc : getMisc()) {
                    if ((misc.getLocation() == LOC_SQUAD) || (misc.getLocation() == i)) {
                        if (misc.getType().hasFlag(MiscType.F_MAGNET_CLAW) || misc.getType().hasFlag(MiscType.F_VIBROCLAW)) {
                            oBV += misc.getType().getBV(this);
                        }
                    }
                }
            }
            int movement = Math.max(getWalkMP(false, false, true, false, true), getJumpMP(false, true, true));
            double speedFactor = Math.pow(1 + ((double) (movement - 5) / 10), 1.2);
            speedFactor = Math.round(speedFactor * 100) / 100.0;
            oBV *= speedFactor;
            squadBV += dBV;

            if (i == 1) {
                System.out.println(getChassis()+getModel());
                System.out.println(dBV);
                System.out.println(oBV);
                System.out.println((oBV+dBV));
            }
            squadBV += oBV;
        }
        // we have now added all troopers, divide by current strength to then
        // multiply by the unit size mod
        squadBV /= getShootingStrength();
        // we might want to get just the BV of a single trooper
        if (singleTrooper) {
            return (int) Math.round(squadBV);
        }
        switch (getShootingStrength()) {
            case 1:
                break;
            case 2:
                squadBV *= 2.2;
                break;
            case 3:
                squadBV *= 3.6;
                break;
            case 4:
                squadBV *= 5.2;
                break;
            case 5:
                squadBV *= 7;
                break;
            case 6:
                squadBV *= 9;
                break;
        }

        if (!ignoreC3) {
            squadBV += getExtraC3BV((int) Math.round(squadBV));
        }

        // Adjust BV for crew skills.
        double pilotFactor = 1;
        if (!ignorePilot) {
            pilotFactor = getCrew().getBVSkillMultiplier(isAntiMek());
        }

        int retVal = (int) Math.round(squadBV * pilotFactor);
        return retVal;
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
                if ((weapon != null) && weapon.isBodyMounted() && weapon.getType().hasFlag(WeaponType.F_MISSILE)) {
                    return true;
                }
            } // Check the next piece of equipment

        } // End is-inner-sphere-squad

        // Unit isn't burdened.
        return false;
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
                } else if ((mounted.getLinked() != null) && (mounted.getLinked().getUsableShotsLeft() > 0)) {
                    return true;
                } else if ((mounted.getLinked() == null) && !mounted.isMissing()){
                    return true;
                }
            }
        }
        return false;
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

    /**
     * Determine if this unit has an active stealth system.
     * <p/>
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

    public boolean isStealthy() {
        return isStealthy;
    }

    /**
     * Determine the stealth modifier for firing at this unit from the given
     * range. If the value supplied for <code>range</code> is not one of the
     * <code>Entity</code> class range constants, an
     * <code>IllegalArgumentException</code> will be thrown.
     * <p/>
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
        if (isStealthy && !((ae instanceof Infantry) && !(ae instanceof BattleArmor)) && !hasMyomerBooster()) {
            switch (range) {
                case RangeType.RANGE_MINIMUM:
                case RangeType.RANGE_SHORT:
                    result = new TargetRoll(shortStealthMod, stealthName);
                    break;
                case RangeType.RANGE_MEDIUM:
                    result = new TargetRoll(mediumStealthMod, stealthName);
                    break;
                case RangeType.RANGE_LONG:
                case RangeType.RANGE_EXTREME: // TODO : what's the *real*
                    // modifier?
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
    public double getCost(boolean ignoreAmmo) {
        // TODO: do this correctly
        // Hopefully the cost is correctly set.
        if (myCost > 0) {
            return myCost;
        }

        // If it's not, I guess we default to the book values...
        if (chassis.equals("Clan Elemental")) {
            return 3500000;
        }
        if (chassis.equals("Clan Gnome")) {
            return 5250000;
        }
        if (chassis.equals("Clan Salamander")) {
            return 3325000;
        }
        if (chassis.equals("Clan Sylph")) {
            return 3325000;
        }
        if (chassis.equals("Clan Undine")) {
            return 3500000;
        }
        if (chassis.equals("IS Standard")) {
            return 2400000;
        }
        if (chassis.equals("Achileus")) {
            return 1920000;
        }
        if (chassis.equals("Cavalier")) {
            return 2400000;
        }
        if (chassis.equals("Fa Shih")) {
            return 2250000;
        }
        if (chassis.equals("Fenrir")) {
            return 2250000;
        }
        if (chassis.equals("Gray Death Light Scout")) {
            return 1650000;
        }
        if (chassis.equals("Gray Death Standard")) {
            return 2400000;
        }
        if (chassis.equals("Infiltrator")) {
            if (model.equals("Mk I")) {
                return 1800000;
            }
            return 2400000; // Mk II
        }
        if (chassis.equals("Kage")) {
            return 1850000;
        }
        if (chassis.equals("Kanazuchi")) {
            return 3300000;
        }
        if (chassis.equals("Longinus")) {
            return 2550000;
        }
        if (chassis.equals("Purifier")) {
            return 2400000;
        }
        if (chassis.equals("Raiden")) {
            return 2400000;
        }
        if (chassis.equals("Sloth")) {
            return 1800000;
        }

        return 0;
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

    public float getTrooperWeight() {
        return trooperWeight;
    }

    public void setTrooperWeight(float trooperWeight) {
        this.trooperWeight = trooperWeight;
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
        Vector<Integer> activeTroops = new Vector<Integer>();
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
        if (!(at.getAmmoType() == AmmoType.T_MINE) && (mounted.getLocation() != mountedAmmo.getLocation())) {
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
        if (!(at.getAmmoType() == AmmoType.T_MINE) && (mounted.getLocation() != mountedAmmo.getLocation())) {
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

        for (int i = 0; i < locations(); i++) {
            boolean found = false;
            for (Mounted m : getEquipment()) {
                // don't write out swarm and leg attack, those get added
                // dynamically
                if ((m.getType() instanceof WeaponType) && m.getType().hasFlag(WeaponType.F_INFANTRY_ATTACK)) {
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
     * return if this BA has improved sensors
     *
     * @return
     */
    public boolean hasImprovedSensors() {
        for (Mounted equip : getMisc()) {
            if (equip.getType().hasFlag(MiscType.F_BAP)) {
                if (equip.getType().getInternalName().equals(Sensor.ISIMPROVED) || equip.getType().getInternalName().equals(Sensor.CLIMPROVED)) {
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
            if (equip.getType().hasFlag(MiscType.F_BAP) && !(equip.getType().getInternalName().equals(Sensor.ISIMPROVED) || equip.getType().getInternalName().equals(Sensor.CLIMPROVED))) {
                return true;
            }
        }
        return false;
    }

    /*
     * (non-Javadoc)
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
                    if ((tArmoredGloveCount > 1) || (tBasicManipulatorCount > 0) || (tBattleClawCount > 0)) {
                        return true;
                    }
                    break;
                case EntityWeightClass.WEIGHT_MEDIUM:
                    if ((tBasicManipulatorCount > 0) || (tBattleClawCount > 0)) {
                        return true;
                    }
                    break;
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
    public float getWeight() {
        // this is for loading into transportes, for that purpose, each trooper
        // weighs a ton
        return troopers;
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

    @Override
    /**
     * Each BA squad has 2 structure points
     */
    public int getBattleForceStructurePoints() {
        return 2;
    }

    public void setIsExoskeleton(boolean exoskeleton) {
        this.exoskeleton = exoskeleton;
    }

    public boolean isExoskeleton() {
        return exoskeleton;
    }

    @Override
    public boolean isCrippled() {
        return (((double)getNumberActiverTroopers() / getSquadSize()) < 0.5);
    }

    @Override
    public boolean isDmgHeavy() {
        return (((double)getNumberActiverTroopers() / getSquadSize()) < 0.67);
    }

    @Override
    public boolean isDmgModerate() {
        return (((double)getNumberActiverTroopers() / getSquadSize()) < 0.75);
    }

    @Override
    public boolean isDmgLight() {
        return (((double)getNumberActiverTroopers() / getSquadSize()) < 0.9);
    }

    public int calculateSwarmDamage() {
        int damage = 0;
        for(Mounted m : getWeaponList()) {
            WeaponType wtype;
            if(m.getType() instanceof WeaponType) {
                wtype = (WeaponType)m.getType();
                if (wtype.hasFlag(WeaponType.F_MISSILE)) {
                    continue;
                }
                if (m.isBodyMounted()) {
                    continue;
                }
                if(wtype instanceof InfantryWeapon) {
                    continue;
                }
                if(wtype instanceof InfantryAttack) {
                    continue;
                }
                int addToDamage = wtype.getDamage(0);
                if(addToDamage < 0) {
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
        return damage;
    }
} // End public class BattleArmor extends Infantry implements Serializable

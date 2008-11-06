/*
 * MegaMek - Copyright (C) 2002,2003,2004 Ben Mazur (bmazur@sev.org)
 * 
 *  This program is free software; you can redistribute it and/or modify it 
 *  under the terms of the GNU General Public License as published by the Free 
 *  Software Foundation; either version 2 of the License, or (at your option) 
 *  any later version.
 * 
 *  This program is distributed in the hope that it will be useful, but 
 *  WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY 
 *  or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License 
 *  for more details.
 */

package megamek.common;

import java.io.Serializable;
import java.util.Vector;

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
public class BattleArmor extends Infantry implements Serializable {
    /**
     * 
     */
    private static final long serialVersionUID = 4594311535026187825L;
    /*
     * Infantry have no critical slot limitations. IS squads usually have 4 men,
     * Clan points usually have 5. Have a location that represents the entire
     * squad.
     */
    private static final int[] IS_NUM_OF_SLOTS = { 7, 2, 2, 2, 2, 2, 2 };
    private static final String[] IS_LOCATION_ABBRS = { "Squad", "Trooper 1",
            "Trooper 2", "Trooper 3", "Trooper 4", "Trooper 5", "Trooper 6" };
    public static final String[] IS_LOCATION_NAMES = { "Squad", "Trooper 1",
            "Trooper 2", "Trooper 3", "Trooper 4", "Trooper 5", "Trooper 6" };
    private static final int[] CLAN_NUM_OF_SLOTS = { 10, 2, 2, 2, 2, 2, 2 };
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
    public static final int MANIPULATOR_HEAVY_BATTLE_VIBRO = 8;
    public static final int MANIPULATOR_SALVAGE_ARM = 9;
    public static final int MANIPULATOR_CARGO_LIFTER = 10;
    public static final int MANIPULATOR_INDUSTRIAL_DRILL = 11;

    public static final String[] MANIPULATOR_TYPE_STRINGS = { "None",
            "Armored Glove", "Basic Manipulator",
            "Basic Manipulator (Mine Clearance)", "Battle Claw",
            "Battle Claw (Magnets)", "Battle Claw (Vibro-Claws)",
            "Heavy Battle Claw", "Heavy Battle Claw (Vibro-Claws)",
            "Salvage Arm", "Cargo Lifter", "Industrial Drill" };

    /**
     * The number of men alive in this unit at the beginning of the phase,
     * before it begins to take damage.
     */
    private int troopersShooting = 0;

    /**
     * The battle value of this unit. This value should be set when the unit's
     * file is read.
     */
    private int myBV = 0;
    private int myCost = -1;
    private int weightClass = -1;
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
     * Camo.
     */
    private boolean isMimetic = false;

    /**
     * Flag that is <code>true</code> when this unit is equipped with simple
     * Camo.
     */
    private boolean isSimpleCamo = false;

    /**
     * Modifiers to <code>ToHitData</code> for stealth.
     */
    private int shortStealthMod = 0;
    private int mediumStealthMod = 0;
    private int longStealthMod = 0;
    private String stealthName = null;

    private int vibroClawDamage = -1;

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
     * The internal name for Boarding Claw equipment.
     */
    public static final String BOARDING_CLAW = "BA-Boarding Claw";

    /**
     * The internal name for Assault Claw equipment.
     */
    public static final String ASSAULT_CLAW = "BA-Assault Claws";

    /**
     * The internal name for Fire Protection equipment.
     */
    public static final String FIRE_PROTECTION = "BA-Fire Resistant Armor";

    /**
     * The internal name for Magnetic Clamp equipment.
     */
    public static final String MAGNETIC_CLAMP = "BA-Magnetic Clamp";

    /**
     * The internal name for the Mine Launcher weapon.
     */
    public static final String MINE_LAUNCHER = "BAMineLauncher";

    /**
     * The internal name for Stealth equipment.
     */
    public static final String STEALTH = "Basic Stealth";

    /**
     * The internal name for Advanced Stealth equipment.
     */
    public static final String ADVANCED_STEALTH = "Standard Stealth";

    /**
     * The internal name for Expert Stealth equipment.
     */
    public static final String EXPERT_STEALTH = "Improved Stealth";

    /**
     * The internal name for Mimetic Camo equipment.
     */
    public static final String MIMETIC_CAMO = "Mimetic Armor";

    /**
     * The internal name for Simple Camo equipment.
     */
    public static final String SIMPLE_CAMO = "Simple Camo";

    /**
     * The internal name for Single-Hex ECM equipment.
     */
    public static final String SINGLE_HEX_ECM = "Single-Hex ECM";

    /**
     * The name for Longinus squads.
     */
    public static final String LONGINUS_SQUAD = "Longinus";

    /**
     * The name for Purifier squads.
     */
    public static final String PURIFIER_SQUAD = "Purifier";

    /**
     * The maximum number of men in a battle armor squad.
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

    public String[] getLocationAbbrs() {
        if (!this.isInitialized || this.isClan()) {
            return CLAN_LOCATION_ABBRS;
        }
        return IS_LOCATION_ABBRS;
    }

    public String[] getLocationNames() {
        if (!this.isInitialized || this.isClan()) {
            return CLAN_LOCATION_NAMES;
        }
        return IS_LOCATION_NAMES;
    }

    /**
     * Returns the number of locations in this unit.
     */
    public int locations() {
        int retVal = Math.round(getWeight());
        if (retVal == 0) {
            // Return one more than the maximum number of men in the unit.
            if (!this.isInitialized) {
                retVal = 6 + 1;
            } else if (this.isClan()) {
                retVal = 5 + 1;
            }
            retVal = 4 + 1;
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

        // All Battle Armor squads are Clan until specified otherwise.
        this.setTechLevel(TechConstants.T_CLAN_TW);

        // Construction complete.
        this.isInitialized = true;
    }

    /**
     * Returns this entity's original jumping mp.
     */
    public int getOriginalJumpMP() {
        return jumpMP;
    }

    public int getWalkMP() {
        return getWalkMP(true, true);
    }
    
    /**
     * Returns this entity's walking mp, factored for extreme temperatures and
     * gravity.
     */
    public int getWalkMP(boolean gravity, boolean ignoreheat) {
        int j = getOriginalWalkMP();
        if(null != game) {
            int weatherMod = game.getPlanetaryConditions().getMovementMods(this);
            if(weatherMod != 0) {
                j = Math.max(j + weatherMod, 0);
            } 
        }      
        if (gravity)
            j = applyGravityEffectsOnMP(j);
        return j;
    }

    /**
     * Returns this entity's running mp, factored for extreme temperatures and
     * gravity.
     */
    public int getRunMP(boolean gravity, boolean ignoreheat) {
        return getWalkMP(gravity, ignoreheat);
    }
    
    public int getJumpMP(boolean gravity) {
        return getJumpMP();
    }

    /**
     * Returns this entity's current jumping MP, not effected by terrain,
     * factored for Gravity. Certain types of equipment prevent a squad from
     * jumping.
     */
    public int getJumpMP() {
        if (this.isBurdened()) {
            return 0;
        }      
        if(null != game) {
            int windCond = game.getPlanetaryConditions().getWindStrength();
            if(windCond >= PlanetaryConditions.WI_STORM) {
                return 0;
            }
        }
        int mp = applyGravityEffectsOnMP(getOriginalJumpMP());
        return mp;
    }

    /**
     * Returns the name of the type of movement used. This is Infantry-specific.
     */
    public String getMovementString(int mtype) {
        switch (mtype) {
            case IEntityMovementType.MOVE_NONE:
                return "None";
            case IEntityMovementType.MOVE_WALK:
            case IEntityMovementType.MOVE_RUN:
                return "Walked";
            case IEntityMovementType.MOVE_VTOL_WALK:
            case IEntityMovementType.MOVE_VTOL_RUN:
                return "Flew";
            case IEntityMovementType.MOVE_JUMP:
                return "Jumped";
            default:
                return "Unknown!";
        }
    }

    /**
     * Returns the abbreviation of the type of movement used. This is
     * Infantry-specific.
     */
    public String getMovementAbbr(int mtype) {
        switch (mtype) {
            case IEntityMovementType.MOVE_NONE:
                return "N";
            case IEntityMovementType.MOVE_WALK:
                return "W";
            case IEntityMovementType.MOVE_RUN:
                return "R";
            case IEntityMovementType.MOVE_JUMP:
                return "J";
            case IEntityMovementType.MOVE_VTOL_WALK:
            case IEntityMovementType.MOVE_VTOL_RUN:
                return "F";
            default:
                return "?";
        }
    }

    /**
     * Battle Armor units can only get hit in undestroyed troopers.
     */
    public HitData rollHitLocation(int table, int side, int aimedLocation,
            int aimingMode) {

        // If this squad was killed, target trooper 1 (just because).
        if (this.isDoomed())
            return new HitData(1);
        
        if ((aimedLocation != LOC_NONE)
                && (aimingMode != IAimingModes.AIM_MODE_NONE)) {
            
            int roll = Compute.d6(2);

            if ((5 < roll) && (roll < 9)) {
                return new HitData(aimedLocation, side == ToHitData.SIDE_REAR,
                        true);
            }
        }

        // Pick a random number between 1 and 6.
        int loc = Compute.d6();

        
/*        if ((aimedLocation != LOC_NONE)
                && (aimingMode != IAimingModes.AIM_MODE_NONE)) {
            
            int roll = Compute.d6(2);

            if ((5 < roll) && (roll < 9)) {
                return new HitData(aimedLocation, side == ToHitData.SIDE_REAR,
                        true);
            }
        }*/

        // Pick a new random number if that trooper is dead or never existed.
        // Remember that there's one more location than the number of troopers.
        // In http://forums.classicbattletech.com/index.php/topic,43203.0.html,
        // "previously destroyed includes the current phase" for rolling hits on
        // a squad,
        // modifying previous ruling in the AskThePM FAQ.
        while (loc >= this.locations()
                || IArmorState.ARMOR_NA == this.getInternal(loc)
                || IArmorState.ARMOR_DESTROYED == this.getInternal(loc)
                || (IArmorState.ARMOR_DOOMED == this.getInternal(loc) && !isDoomed())) {
            loc = Compute.d6();
        }

        int critLocation = Compute.d6();
        //TacOps p. 108 Trooper takes a crit if a second roll is the same location as the first. 
        if (game.getOptions().booleanOption("tacops_ba_criticals") && loc == critLocation) {
            return new HitData(loc, false, HitData.EFFECT_CRITICAL);
        }
        // Hit that trooper.
        return new HitData(loc);

    }

    public HitData rollHitLocation(int table, int side) {
        return rollHitLocation(table, side, LOC_NONE, IAimingModes.AIM_MODE_NONE);
    }

    /**
     * For level 3 rules, each trooper occupies a specific location
     * precondition: hit is a location covered by BA
     */
    public HitData getTrooperAtLocation(HitData hit, Entity transport) {
        if (transport instanceof Mech) {
            int loc = 99;
            switch (hit.getLocation()) {
                case Mech.LOC_RT:
                    if (hit.isRear())
                        loc = 3;
                    else
                        loc = 1;
                    break;
                case Mech.LOC_LT:
                    if (hit.isRear())
                        loc = 4;
                    else
                        loc = 2;
                    break;
                case Mech.LOC_CT:
                    if (hit.isRear())
                        loc = 5;
                    else
                        loc = 6;
                    break;
            }
            if (loc < locations())
                return new HitData(loc);
        } else if (transport instanceof Tank) {
            int loc = 99;
            switch (hit.getLocation()) {
                case Tank.LOC_RIGHT:
                    // There are 2 troopers on each location, so pick
                    // one randomly if both are alive.
                    if (getInternal(1) > 0 && getInternal(2) > 0)
                        loc = Compute.randomInt(2) + 1;
                    else if (getInternal(1) > 0)
                        loc = 1;
                    else
                        loc = 2;
                    break;
                case Tank.LOC_LEFT:
                    if (getInternal(3) > 0 && getInternal(4) > 0)
                        loc = Compute.randomInt(2) + 3;
                    else if (getInternal(3) > 0)
                        loc = 3;
                    else
                        loc = 4;
                    break;
                case Tank.LOC_REAR:
                    if (getInternal(5) > 0 && getInternal(6) > 0)
                        loc = Compute.randomInt(2) + 5;
                    else if (getInternal(5) > 0)
                        loc = 5;
                    else
                        loc = 6;
                    break;
            }
            if (loc < locations())
                return new HitData(loc);
        }
        // otherwise roll a random location
        return rollHitLocation(ToHitData.HIT_NORMAL, ToHitData.SIDE_FRONT);
    }

    /**
     * Battle Armor units don't transfer damage.
     */
    public HitData getTransferLocation(HitData hit) {

        // If any trooper lives, the unit isn't destroyed.
        for (int loop = 1; loop < this.locations(); loop++) {
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
    protected boolean isPlatoon() {
        return false;
    }

    /**
     * Battle Armor units have no armor on their squad location.
     * 
     * @see megamek.common.Infantry#getArmor( int, boolean )
     */
    public int getArmor(int loc, boolean rear) {
        if (BattleArmor.LOC_SQUAD != loc) {
            return super.getArmor(loc, rear);
        }
        return IArmorState.ARMOR_NA;
    }

    /**
     * Battle Armor units have no armor on their squad location.
     * 
     * @see megamek.common.Infantry#getOArmor( int, boolean )
     */
    public int getOArmor(int loc, boolean rear) {
        if (BattleArmor.LOC_SQUAD != loc) {
            return super.getOArmor(loc, rear);
        }
        return IArmorState.ARMOR_NA;
    }

    /**
     * Battle Armor units have no internals on their squad location.
     * 
     * @see megamek.common.Infantry#getInternal( int )
     */
    public int getInternal(int loc) {
        if (BattleArmor.LOC_SQUAD != loc) {
            return super.getInternal(loc);
        }
        return IArmorState.ARMOR_NA;
    }

    /**
     * Battle Armor units have no internals on their squad location.
     * 
     * @see megamek.common.Infantry#getOInternal( int )
     */
    public int getOInternal(int loc) {
        if (BattleArmor.LOC_SQUAD != loc) {
            return super.getOInternal(loc);
        }
        return IArmorState.ARMOR_NA;
    }

    /**
     * Set the troopers in the unit to the appropriate values.
     */
    public void autoSetInternal() {
        // No troopers in the squad location.
        this.initializeInternal(IArmorState.ARMOR_NA, LOC_SQUAD);

        // Initialize the troopers.
        for (int loop = 1; loop < this.locations(); loop++) {
            this.initializeInternal(1, loop);
        }

        // Set the initial number of troopers that can shoot
        // to one less than the number of locations in the unit.
        this.troopersShooting = this.locations() - 1;
    }

    /**
     * Mounts the specified equipment in the specified location.
     */
    protected void addEquipment(Mounted mounted, int loc, boolean rearMounted)
            throws LocationFullException {
        // Implement parent's behavior.
        super.addEquipment(mounted, loc, rearMounted);

        // Add the piece equipment to our slots.
        addCritical(loc, new CriticalSlot(CriticalSlot.TYPE_EQUIPMENT,
                getEquipmentNum(mounted), true));

        // Is the item a stealth equipment?
        // TODO: what's the *real* extreme range modifier?
        String name = mounted.getType().getInternalName();
        if (BattleArmor.STEALTH.equals(name)) {
            this.isStealthy = true;
            this.shortStealthMod = 0;
            this.mediumStealthMod = 1;
            this.longStealthMod = 2;
            this.stealthName = "basic stealth";
        } else if (BattleArmor.ADVANCED_STEALTH.equals(name)) {
            this.isStealthy = true;
            this.shortStealthMod = 1;
            this.mediumStealthMod = 1;
            this.longStealthMod = 2;
            this.stealthName = "standard stealth";
        } else if (BattleArmor.EXPERT_STEALTH.equals(name)) {
            this.isStealthy = true;
            this.shortStealthMod = 1;
            this.mediumStealthMod = 2;
            this.longStealthMod = 3;
            this.stealthName = "improved stealth";
        } else if (BattleArmor.MIMETIC_CAMO.equals(name)) {
            this.isMimetic = true;
        } else if (BattleArmor.SIMPLE_CAMO.equals(name)) {
            this.isSimpleCamo = true;
        }
    }

    /**
     * Battle Armor units have as many critical slots as they need to hold their
     * equipment.
     */
    protected int[] getNoOfSlots() {
        if (!this.isInitialized || this.isClan()) {
            return CLAN_NUM_OF_SLOTS;
        }
        return IS_NUM_OF_SLOTS;
    }

    /**
     * Trooper's equipment dies when they do.
     */
    public boolean hasHittableCriticals(int loc) {
        if (LOC_SQUAD == loc)
            return false;
        return super.hasHittableCriticals(loc);
    }

    /**
     * Calculates the battle value of this platoon.
     */
    public int calculateBattleValue() {
        double bv = myBV;

        // Adjust for missing troopers
        bv = bv * getInternalRemainingPercent();

        // Adjust BV for crew skills.
        double pilotFactor = crew.getBVSkillMultiplier();

        int finalBV = (int) Math.round(bv);

        int retVal = (int) Math.round(finalBV * pilotFactor);
        return retVal;
    }

    /**
     * Prepare the entity for a new round of action.
     */
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
    public void applyDamage() {
        super.applyDamage();
        troopersShooting = this.getTotalInternal();
    }

    /**
     * Get the number of men in the unit (before damage is applied).
     * 
     * @see megamek.common.Infantry#getShootingStrength
     */
    public int getShootingStrength() {
        return troopersShooting;
    }

    /**
     * Sets the battle value of this unit. Please note that the BV of all Battle
     * Armor units is dictated by the BMRr, page 155.
     * 
     * @param bv - the <code>int</code> battle value of this unit.
     */
    public void setBattleValue(int bv) {
        myBV = bv;
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
        if (!this.isClan()) {

            // As of 2006-10-10 any squad can be burdened.

            // As of 2003-01-03, only ammo burdens the jump
            // Loop through the squad's equipment.
            for (Mounted mounted : this.getAmmo()) {
                EquipmentType type = mounted.getType();

                // Un-jettisoned ammo packs burden squads.
                if (mounted.getShotsLeft() > 0
                        && (type.hasFlag(AmmoType.F_ENCUMBERING))) {
                    return true;
                }

            } // Check the next piece of equipment

        } // End is-inner-sphere-squad

        // Unit isn't burdened.
        return false;
    }

    /**
     * Determine if this unit has an active stealth system. <p/> Sub-classes are
     * encouraged to override this method.
     * 
     * @return <code>true</code> if this unit has a stealth system that is
     *         currently active, <code>false</code> if there is no stealth
     *         system or if it is inactive.
     */
    public boolean isStealthActive() {
        return (isStealthy || isMimetic || isSimpleCamo);
    }

    /**
     * does this BA have fire resistant armor?
     * 
     * @return
     */
    public boolean hasFireresistantArmor() {
        return armorType == 7;
    }

    /**
     * Determine the stealth modifier for firing at this unit from the given
     * range. If the value supplied for <code>range</code> is not one of the
     * <code>Entity</code> class range constants, an
     * <code>IllegalArgumentException</code> will be thrown. <p/> Sub-classes
     * are encouraged to override this method.
     * 
     * @param range - an <code>int</code> value that must match one of the
     *            <code>Compute</code> class range constants.
     * @param ae - the entity making the attack.
     * @return a <code>TargetRoll</code> value that contains the stealth
     *         modifier for the given range.
     */
    public TargetRoll getStealthModifier(int range, Entity ae) {
        TargetRoll result = null;

        // Note: infantry are immune to stealth, but not camoflage
        // or mimetic armor

        // TODO: eliminate duplicate code
        if (armorType != -1) {
            /*
             * Here, in order, are the armor types used by custom Battle Armor
             * at this point: 0: "Standard", 1: "Advanced", 2: "Prototype", 3:
             * "Basic Stealth", 4: "Prototype Stealth", 5: "Standard Stealth",
             * 6: "Improved Stealth", 7: "Fire Resistant", 8: "Mimetic"
             */
            if (armorType == 3
                    && !(ae instanceof Infantry && !(ae instanceof BattleArmor))) {
                // Basic Stealth
                switch (range) {
                    case RangeType.RANGE_MINIMUM:
                    case RangeType.RANGE_SHORT:
                        // At short range, basic stealth doesn't get a mod!
                        break;
                    case RangeType.RANGE_MEDIUM:
                        result = new TargetRoll(+1, "Basic Stealth Armor");
                        break;
                    case RangeType.RANGE_LONG:
                    case RangeType.RANGE_EXTREME: // TODO : what's the *real*
                                                    // modifier?
                        result = new TargetRoll(+2, "Basic Stealth Armor");
                        break;
                    default:
                        throw new IllegalArgumentException(
                                "Unknown range constant: " + range);
                }
            } else if (armorType == 4
                    && !(ae instanceof Infantry && !(ae instanceof BattleArmor))) {
                // Prototype Stealth
                switch (range) {
                    case RangeType.RANGE_MINIMUM:
                    case RangeType.RANGE_SHORT:
                        // At short range, prototype stealth doesn't get a mod!
                        break;
                    case RangeType.RANGE_MEDIUM:
                        result = new TargetRoll(+1, "Prototype Stealth Armor");
                        break;
                    case RangeType.RANGE_LONG:
                    case RangeType.RANGE_EXTREME: // TODO : what's the *real*
                                                    // modifier?
                        result = new TargetRoll(+2, "Prototype Stealth Armor");
                        break;
                    default:
                        throw new IllegalArgumentException(
                                "Unknown range constant: " + range);
                }
            } else if (armorType == 5
                    && !(ae instanceof Infantry && !(ae instanceof BattleArmor))) {
                // Standard Stealth
                switch (range) {
                    case RangeType.RANGE_MINIMUM:
                    case RangeType.RANGE_SHORT:
                        result = new TargetRoll(+1, "Standard Stealth Armor");
                        break;
                    case RangeType.RANGE_MEDIUM:
                        result = new TargetRoll(+1, "Standard Stealth Armor");
                        break;
                    case RangeType.RANGE_LONG:
                    case RangeType.RANGE_EXTREME: // TODO : what's the *real*
                                                    // modifier?
                        result = new TargetRoll(+2, "Standard Stealth Armor");
                        break;
                    default:
                        throw new IllegalArgumentException(
                                "Unknown range constant: " + range);
                }
            } else if (armorType == 6
                    && !(ae instanceof Infantry && !(ae instanceof BattleArmor))) {
                // Improved Stealth
                switch (range) {
                    case RangeType.RANGE_MINIMUM:
                    case RangeType.RANGE_SHORT:
                        result = new TargetRoll(+1, "Improved Stealth Armor");
                        break;
                    case RangeType.RANGE_MEDIUM:
                        result = new TargetRoll(+2, "Improved Stealth Armor");
                        break;
                    case RangeType.RANGE_LONG:
                    case RangeType.RANGE_EXTREME: // TODO : what's the *real*
                                                    // modifier?
                        result = new TargetRoll(+3, "Improved Stealth Armor");
                        break;
                    default:
                        throw new IllegalArgumentException(
                                "Unknown range constant: " + range);
                }
            } else if (armorType == 8) {
                int mmod = 3 - delta_distance;
                mmod = Math.max(0, mmod);
                result = new TargetRoll(mmod, "mimetic armor");
            }
        } else {
            // Mimetic armor modifier is based upon the number of hexes moved,
            // and adds to existing movement modifier (Total Warfare p228):
            // 0 hexes moved +3 movement modifier
            // 1 hex moved +2 movement modifier
            // 2 hexes moved +1 movement modifier
            // 3+ hexes moved +0 movement modifier
            if (isMimetic) {
                int mmod = 3 - delta_distance;
                mmod = Math.max(0, mmod);
                result = new TargetRoll(mmod, "mimetic armor");
            }

            // Stealthy units alreay have their to-hit mods defined.
            if (isStealthy
                    && !(ae instanceof Infantry && !(ae instanceof BattleArmor))) {
                switch (range) {
                    case RangeType.RANGE_MINIMUM:
                    case RangeType.RANGE_SHORT:
                        result = new TargetRoll(this.shortStealthMod,
                                this.stealthName);
                        break;
                    case RangeType.RANGE_MEDIUM:
                        result = new TargetRoll(this.mediumStealthMod,
                                this.stealthName);
                        break;
                    case RangeType.RANGE_LONG:
                    case RangeType.RANGE_EXTREME: // TODO : what's the *real*
                                                    // modifier?
                        result = new TargetRoll(this.longStealthMod,
                                this.stealthName);
                        break;
                    default:
                        throw new IllegalArgumentException(
                                "Unknown range constant: " + range);
                }
            }
        }

        // Simple camo modifier is on top of the movement modifier
        // 0 hexes moved +2 movement modifier
        // 1 hexes moved +1 movement modifier
        // 2+ hexes moved no modifier
        // This can also be in addition to any armor except Mimetic!
        if (isSimpleCamo && delta_distance < 2) {
            int mod = Math.max(2 - delta_distance, 0);
            if (result == null)
                result = new TargetRoll(mod, "camoflage");
            else
                result.append(new TargetRoll(mod, "camoflage"));
        }

        if (result == null)
            result = new TargetRoll(0, "stealth not active");

        // Return the result.
        return result;
    } // End public TargetRoll getStealthModifier( char )

    public int getVibroClawDamage() {
        if (vibroClawDamage < 0) {
            vibroClawDamage = 0;
            for (Mounted mounted : getWeaponList()) {
                if (mounted.getType().hasFlag(WeaponType.F_BOOST_SWARM)) {
                    vibroClawDamage = ((WeaponType) (mounted.getType()))
                            .getRackSize();
                    break;
                }
            }
        }
        return vibroClawDamage;
    }

    public double getCost() {
        // Hopefully the cost is correctly set.
        if (myCost > 0)
            return myCost;

        // If it's not, I guess we default to the book values...
        if (chassis.equals("Clan Elemental"))
            return 3500000;
        if (chassis.equals("Clan Gnome"))
            return 5250000;
        if (chassis.equals("Clan Salamander"))
            return 3325000;
        if (chassis.equals("Clan Sylph"))
            return 3325000;
        if (chassis.equals("Clan Undine"))
            return 3500000;
        if (chassis.equals("IS Standard"))
            return 2400000;
        if (chassis.equals("Achileus"))
            return 1920000;
        if (chassis.equals("Cavalier"))
            return 2400000;
        if (chassis.equals("Fa Shih"))
            return 2250000;
        if (chassis.equals("Fenrir"))
            return 2250000;
        if (chassis.equals("Gray Death Light Scout"))
            return 1650000;
        if (chassis.equals("Gray Death Standard"))
            return 2400000;
        if (chassis.equals("Infiltrator")) {
            if (model.equals("Mk I"))
                return 1800000;
            return 2400000; // Mk II
        }
        if (chassis.equals("Kage"))
            return 1850000;
        if (chassis.equals("Kanazuchi"))
            return 3300000;
        if (chassis.equals("Longinus"))
            return 2550000;
        if (chassis.equals("Purifier"))
            return 2400000;
        if (chassis.equals("Raiden"))
            return 2400000;
        if (chassis.equals("Sloth"))
            return 1800000;

        return 0;
    }

    public boolean hasEiCockpit() {
        return true;
    }

    public void setWeightClass(int inWC) {
        weightClass = inWC;
    }

    public int getWeightClass() {
        return weightClass;
    }

    public void setChassisType(int inCT) {
        chassisType = inCT;
    }

    public int getChassisType() {
        return chassisType;
    }

    public boolean canAssaultDrop() {
        return true;
    }

    public boolean isNuclearHardened() {
        return true;
    }

    public boolean isTrooperActive(int trooperNum) {
        return (getInternal(trooperNum) > 0);
    }

    public int getNumberActiverTroopers() {
        int count = 0;
        // Initialize the troopers.
        for (int loop = 1; loop < this.locations(); loop++)
            if (isTrooperActive(loop))
                count++;
        return count;
    }

    public int getRandomTrooper() {
        Vector<Integer> activeTroops = new Vector<Integer>();
        for (int loop = 1; loop < this.locations(); loop++)
            if (isTrooperActive(loop))
                activeTroops.add(loop);
        int locInt = Compute.randomInt(activeTroops.size());
        return activeTroops.elementAt(locInt);
    }

    public boolean loadWeapon(Mounted mounted, Mounted mountedAmmo) {
        // BA must carry the ammo in same location as the weapon.
        // except for mine launcher mines
        // This allows for squad weapons and individual trooper weapons
        // such as NARC and the support weapons in TW/TO
        AmmoType at = (AmmoType) mountedAmmo.getType();
        if (!(at.getAmmoType() == AmmoType.T_MINE)
                && mounted.getLocation() != mountedAmmo.getLocation())
            return false;
        return super.loadWeapon(mounted, mountedAmmo);
    }

    public boolean loadWeaponWithSameAmmo(Mounted mounted, Mounted mountedAmmo) {
        // BA must carry the ammo in same location as the weapon.
        // except for mine launcher mines
        // This allows for squad weapons and individual trooper weapons
        // such as NARC and the support weapons in TW/TO
        AmmoType at = (AmmoType) mountedAmmo.getType();
        if (!(at.getAmmoType() == AmmoType.T_MINE)
                && mounted.getLocation() != mountedAmmo.getLocation())
            return false;
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
            case TechConstants.T_CLAN_TW:
                buff.append("Clan Level 2");
                break;
            case TechConstants.T_CLAN_ADVANCED:
                buff.append("Clan Level 3");
                break;
        }
        buff.append(newline);
        buff.append("</type>");
        buff.append(newline);

        buff.append("<tonnage>");
        buff.append(newline);
        buff.append(getWeight());
        buff.append(newline);
        buff.append("</tonnage>");
        buff.append(newline);

        buff.append("<bv>");
        buff.append(newline);
        buff.append(calculateBattleValue());
        buff.append(newline);
        buff.append("</bv>");
        buff.append(newline);

        buff.append("<motion_type>");
        buff.append(newline);
        switch (getMovementMode()) {
            case IEntityMovementMode.INF_JUMP:
                buff.append("jump");
                break;
            case IEntityMovementMode.INF_LEG:
                buff.append("leg");
                break;
            case IEntityMovementMode.VTOL:
                buff.append("vtol");
                break;
            case IEntityMovementMode.INF_UMU:
                buff.append("submarine");
                break;
        }
        buff.append(newline);
        buff.append("</motion_type>");
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
    public int getVibroClaws() {
        int claws = 0;
        for (Mounted mounted : getMisc()) {
            if (mounted.getType().hasFlag(MiscType.F_VIBROCLAW)) {
                claws++;
            }
        }
        return claws;
    }

} // End public class BattleArmor extends Infantry implements Serializable

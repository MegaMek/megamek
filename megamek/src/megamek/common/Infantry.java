/*
 * MegaMek - Copyright (C) 2000-2002 Ben Mazur (bmazur@sev.org)
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

import java.text.NumberFormat;
import java.util.Enumeration;
import java.util.Vector;

import megamek.common.preference.PreferenceManager;
import megamek.common.verifier.TestEntity;
import megamek.common.weapons.infantry.InfantryWeapon;

/**
 * This class represents the lowest of the low, the ground pounders, the city
 * rats, the PBI (Poor Bloody Infantry). <p/> PLEASE NOTE!!! This class just
 * represents unarmored infantry platoons as described by CitiTech (c) 1986.
 * I've never seen the rules for powered armor, "anti-mech" troops, or
 * Immortals.
 *
 * @author Suvarov454@sourceforge.net (James A. Damour )
 * @version $revision:$
 */
/*
 * PLEASE NOTE!!! My programming style is to put constants first in tests so the
 * compiler catches my "= for ==" errors.
 */
public class Infantry extends Entity {
    // Private attributes and helper functions.

    /**
     *
     */
    private static final long serialVersionUID = -8706716079307721282L;

    /**
     * squad size and number
     */
    protected int squadn = 1;
    private int squadsize = 1;

    /**
     * The number of men originally in this platoon.
     */
    protected int menStarting = 0;

    /**
     * The number of men alive in this platoon at the beginning of the phase,
     * before it begins to take damage.
     */
    private int menShooting = 0;

    /**
     * The number of men left alive in this platoon.
     */
    private int men = 0;

    /**
     * Information on primary and secondary weapons
     * This must be kept separate from the equipment array
     * because they are not fired as separate weapons
     */
    private transient InfantryWeapon primaryW;
    private String primaryName;
    private transient InfantryWeapon secondW;
    private String secondName;
    private int secondn = 0;


    /**
     * Infantry armor
     */
    private double damageDivisor = 1.0;
    private boolean encumbering = false;
    private boolean spaceSuit = false;
    private boolean dest = false;
    private boolean sneak_camo = false;
    private boolean sneak_ir = false;
    private boolean sneak_ecm = false;
    private boolean mountain = false;

    /**
     * The location for infantry equipment.
     */
    public static final int LOC_INFANTRY = 0;
    public static final int LOC_FIELD_GUNS = 1;

    /**
     * Infantry only have critical slots for field gun ammo
     */
    private static final int[] NUM_OF_SLOTS = { 20, 20 };
    private static final String[] LOCATION_ABBRS = { "MEN", "FGUN" };
    private static final String[] LOCATION_NAMES = { "Men" , "Field Guns"};

    public int turnsLayingExplosives = -1;

    public static final int DUG_IN_NONE = 0;
    public static final int DUG_IN_WORKING = 1; // no protection, can't attack
    public static final int DUG_IN_COMPLETE = 2; // protected, restricted arc
    public static final int DUG_IN_FORTIFYING1 = 3; // no protection, can't
    // attack
    public static final int DUG_IN_FORTIFYING2 = 4; // no protection, can't
    // attack
    private int dugIn = DUG_IN_NONE;
    
    private boolean isTakingCover = false;

    // Public and Protected constants, constructors, and methods.

    /**
     * The maximum number of men in an infantry platoon.
     */
    public static final int INF_PLT_MAX_MEN = 30;

    /**
     * The internal names of the anti-Mek attacks.
     */
    public static final String LEG_ATTACK = "LegAttack";
    public static final String SWARM_MEK = "SwarmMek";
    public static final String SWARM_WEAPON_MEK = "SwarmWeaponMek";
    public static final String STOP_SWARM = "StopSwarm";
    
    public static final int ANTI_MECH_SKILL_UNTRAINED = 8;
    public static final int ANTI_MECH_SKILL_FOOT = 5;
    public static final int ANTI_MECH_SKILL_JUMP = 6;

    @Override
    public String[] getLocationAbbrs() {
        return LOCATION_ABBRS;
    }

    @Override
    public String[] getLocationNames() {
        return LOCATION_NAMES;
    }

    /**
     * Returns the number of locations in this platoon
     */
    @Override
    public int locations() {
        return 2;
    }

    /**
     * Generate a new, blank, infantry platoon. Hopefully, we'll be loaded from
     * somewhere.
     */
    public Infantry() {
        // Instantiate the superclass.
        super();
        // Create a "dead" leg rifle platoon.
        menStarting = 0;
        menShooting = 0;
        men = 0;
        setMovementMode(EntityMovementMode.INF_LEG);
        // Determine the number of MPs.
        setOriginalWalkMP(1);
    }

    /**
     * Infantry can face freely (except when dug in)
     */
    @Override
    public boolean canChangeSecondaryFacing() {
        return !hasActiveFieldArtillery();
    }

    /**
     * Infantry can face freely
     */
    @Override
    public boolean isValidSecondaryFacing(int dir) {
        return true;
    }

    /**
     * Infantry can face freely
     */
    @Override
    public int clipSecondaryFacing(int dir) {
        return dir;
    }

    /**
     * return this infantry's walk mp, adjusted for planetary conditions
     */
    @Override
    public int getWalkMP(boolean gravity, boolean ignoreheat, boolean ignoremodulararmor) {
        int mp = getOriginalWalkMP();
        //encumbering armor reduces MP by 1 to a minimum of one (TacOps, pg. 318)
        if(encumbering) {
            mp = Math.max(mp - 1, 1);
        }
        if((getSecondaryN() > 1)
                && ((null == getCrew()) || !getCrew().getOptions().booleanOption("tsm_implant"))
                && ((null == getCrew()) || !getCrew().getOptions().booleanOption("dermal_armor"))
                && (null != secondW) && secondW.hasFlag(WeaponType.F_INF_SUPPORT)
                && (getMovementMode() != EntityMovementMode.TRACKED)
                && (getMovementMode() != EntityMovementMode.INF_JUMP)) {
            mp = Math.max(mp - 1, 0);
        }
        if((null != getCrew())
                && getCrew().getOptions().booleanOption("pl_masc")
                && ((getMovementMode() == EntityMovementMode.INF_LEG)
                    || (getMovementMode() == EntityMovementMode.INF_JUMP))) {
            mp += 1;
        }
        if(hasActiveFieldArtillery()) {
            //mp of 1 at the most
            mp = Math.min(mp, 1);
        }
        if(null != game) {
            int weatherMod = game.getPlanetaryConditions().getMovementMods(this);
            if(weatherMod != 0) {
                mp = Math.max(mp + weatherMod, 0);
            }
        }
        if (gravity) {
            mp = applyGravityEffectsOnMP(mp);
        }
        return mp;
    }

    /**
     * Return this Infantry's run MP, which is identical to its walk MP
     */
    @Override
    public int getRunMP(boolean gravity, boolean ignoreheat, boolean ignoremodulararmor) {
        if( (game != null)
                && game.getOptions().booleanOption("tacops_fast_infantry_move") ) {
            if(getWalkMP(gravity, ignoreheat, ignoremodulararmor) > 0) {
                return getWalkMP(gravity, ignoreheat, ignoremodulararmor) + 1;
            }
            return getWalkMP(gravity, ignoreheat, ignoremodulararmor) + 2;
        }
        return getWalkMP(gravity, ignoreheat, ignoremodulararmor);
    }

    /**
     * Infantry don't have MASC
     */
    @Override
    public int getRunMPwithoutMASC(boolean gravity, boolean ignoreheat, boolean ignoremodulararmor) {
        return getRunMP(gravity, ignoreheat, ignoremodulararmor);
    }


    /*
     * (non-Javadoc)
     * @see megamek.common.Entity#getJumpMP(boolean)
     */
    @Override
    public int getJumpMP(boolean gravity) {
        int mp = getOriginalJumpMP();
        if((getSecondaryN() > 1)
                && ((null == getCrew()) || !getCrew().getOptions().booleanOption("tsm_implant"))
                && ((null == getCrew()) || !getCrew().getOptions().booleanOption("dermal_armor"))
                && (null != secondW) && secondW.hasFlag(WeaponType.F_INF_SUPPORT)) {
            mp = Math.max(mp - 1, 0);
        }
        if (gravity) {
            mp = applyGravityEffectsOnMP(mp);
        }
        int windP = 0;
        if(null != game) {
            int windCond = game.getPlanetaryConditions().getWindStrength();
            if(windCond == PlanetaryConditions.WI_MOD_GALE) {
                windP++;
            }
            if(windCond >= PlanetaryConditions.WI_STRONG_GALE) {
                return 0;
            }
        }
        mp = Math.max(mp - windP, 0);
        return mp;
    }

    /**
     * Infantry can not enter water unless they have UMU mp or hover.
     */
    @Override
    public boolean isLocationProhibited(Coords c, int currElevation) {
        // Coords off the board aren't legal
        if (!game.getBoard().contains(c)) {
            return true;
        }
        IHex hex = game.getBoard().getHex(c);
        // Taharqa: waiting to hear back from Welshie but I am going to assume
        // that units pulling artillery
        // should be treated as wheeled rather than motorized because otherwise
        // mechanized units face fewer
        // terrain restrictions when pulling field artillery

        if (hex.containsTerrain(Terrains.IMPASSABLE)) {
            return true;
        }
        if (hex.containsTerrain(Terrains.MAGMA)) {
            return true;
        }
        if(hex.containsTerrain(Terrains.SPACE) && doomedInSpace()) {
            return true;
        }

        if (hex.terrainLevel(Terrains.WOODS) > 0) {
            if ((hex.terrainLevel(Terrains.WOODS) > 1)
                    && (getMovementMode() == EntityMovementMode.TRACKED)) {
                return true;
            }
            if ((getMovementMode() == EntityMovementMode.HOVER)
                    || (getMovementMode() == EntityMovementMode.WHEELED)
                    || hasActiveFieldArtillery()) {
                return true;
            }
        }

        if (hex.containsTerrain(Terrains.ROUGH)
                || hex.containsTerrain(Terrains.RUBBLE)) {
            if ((getMovementMode() == EntityMovementMode.WHEELED)
                    || hasActiveFieldArtillery()) {
                return true;
            }
        }

        if ((hex.terrainLevel(Terrains.WATER) > 0)
                && !hex.containsTerrain(Terrains.ICE)) {
            if ((getMovementMode() == EntityMovementMode.HOVER)
                    || (getMovementMode() == EntityMovementMode.INF_UMU)
                    || (getMovementMode() == EntityMovementMode.VTOL)) {
                return false;
            }
            return true;
        }
        return false;
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
                switch (getMovementMode()) {
                    case INF_LEG:
                        return "Walked";
                    case INF_MOTORIZED:
                        return "Biked";
                    case HOVER:
                    case TRACKED:
                    case WHEELED:
                        return "Drove";
                    case INF_JUMP:
                    default:
                        return "Unknown!";
                }
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
                switch (getMovementMode()) {
                    case INF_LEG:
                        return "R";
                    case INF_MOTORIZED:
                        return "B";
                    case HOVER:
                    case TRACKED:
                    case WHEELED:
                        return "D";
                    default:
                        return "?";
                }
            case MOVE_JUMP:
                return "J";
            default:
                return "?";
        }
    }

    /**
     * Infantry only have one hit location.
     */
    @Override
    public HitData rollHitLocation(int table, int side, int aimedLocation,
            int aimingMode, int cover) {
        return rollHitLocation(table, side);
    }

    @Override
    public HitData rollHitLocation(int table, int side) {
        return new HitData(LOC_INFANTRY);
    }

    /**
     * Infantry only have one hit location.
     */
    @Override
    public HitData getTransferLocation(HitData hit) {
        return new HitData(Entity.LOC_DESTROYED);
    }

    /**
     * Gets the location that is destroyed recursively.
     */
    @Override
    public int getDependentLocation(int loc) {
        return Entity.LOC_NONE;
    }

    /**
     * Infantry have no rear armor.
     */
    @Override
    public boolean hasRearArmor(int loc) {
        return false;
    }

    /**
     * Infantry platoons do wierd and wacky things with armor and internals, but
     * not all Infantry objects are platoons.
     *
     * @see megamek.common.BattleArmor#isPlatoon()
     */
    protected boolean isPlatoon() {
        return true;
    }

    /**
     * Returns the number of men left in the platoon, or
     * IArmorState.ARMOR_DESTROYED.
     */
    @Override
    public int getInternal(int loc) {
        if (!isPlatoon()) {
            return super.getInternal(loc);
        }
        if(loc != LOC_INFANTRY) {
            return 0;
        }
        return (men > 0 ? men : IArmorState.ARMOR_DESTROYED);
    }

    /**
     * Returns the number of men originally the platoon.
     */
    @Override
    public int getOInternal(int loc) {
        if (!isPlatoon()) {
            return super.getOInternal(loc);
        }
        return menStarting;
    }

    /**
     * Sets the amount of men remaining in the platoon.
     */
    @Override
    public void setInternal(int val, int loc) {
        super.setInternal(val, loc);
        if(loc == LOC_INFANTRY) {
            men = val;
        }
    }

    /**
     * Returns the percent of the men remaining in the platoon.
     */
    @Override
    public double getInternalRemainingPercent() {
        if (!isPlatoon()) {
            return super.getInternalRemainingPercent();
        }
        int menTotal = men > 0 ? men : 0; // Handle "DESTROYED"
        return ((double) menTotal / menStarting);
    }

    /**
     * Initializes the number of men in the platoon. Sets the original and
     * starting point of the platoon to the same number.
     */
    @Override
    public void initializeInternal(int val, int loc) {
        menStarting = val;
        menShooting = val;
        super.initializeInternal(val, loc);
    }

    /**
     * Set the men in the platoon based on squad size and number
     */
    @Override
    public void autoSetInternal() {
        //TODO: put checks here on size
        initializeInternal(squadsize*squadn, LOC_INFANTRY);
    }

    /**
     * Infantry can fire all around themselves. But field guns are set up to a
     * vehicular turret facing
     */
    @Override
    public int getWeaponArc(int wn) {
        Mounted mounted = getEquipment(wn);
        if(mounted.getLocation() == LOC_FIELD_GUNS) {
            if (game.getOptions().booleanOption("tacops_vehicle_arcs")) {
                return Compute.ARC_TURRET;
            }
            return Compute.ARC_FORWARD;
        }
        //This is interesting, according to TacOps rules, Dug in units no longer
        //have to declare a facing
        return Compute.ARC_360;
    }

    /**
     * Infantry can fire all around themselves. But field guns act like turret
     * mounted on a tank
     */
    @Override
    public boolean isSecondaryArcWeapon(int wn) {
        if ((getEquipment(wn).getLocation() == LOC_FIELD_GUNS) && !hasActiveFieldArtillery()) {
            return true;
        }
        return false;
    }

    /**
     * Infantry build no heat.
     */
    @Override
    public int getHeatCapacity() {
        return 999;
    }

    /**
     * Infantry build no heat.
     */
    @Override
    public int getHeatCapacityWithWater() {
        return getHeatCapacity();
    }

    /**
     * Infantry build no heat.
     */
    @Override
    public int getEngineCritHeat() {
        return 0;
    }

    /**
     * Infantry have no critical slots.
     */
    @Override
    protected int[] getNoOfSlots() {
        return NUM_OF_SLOTS;
    }

    /**
     * Infantry criticals can't be hit.
     */
    public boolean hasHittableCriticals(int loc) {
        return false;
    }

    /*
     * (non-Javadoc)
     * @see megamek.common.Entity#calculateBattleValue()
     */
    @Override
    public int calculateBattleValue() {
        return calculateBattleValue(false, false);
    }

    /**
     * Calculates the battle value of this platoon.
     */
    @Override
    public int calculateBattleValue(boolean ignoreC3, boolean ignorePilot) {
        double dbv;

        dbv = men * 1.5 * getDamageDivisor();
        int tmmRan = Compute.getTargetMovementModifier(getRunMP(false, true, true), false, false, game)
                .getValue();

        final int jumpMP = getJumpMP(false);
        final int tmmJumped = (jumpMP > 0) ? Compute.
                getTargetMovementModifier(jumpMP, true, false, game).getValue()
                : 0;

        final int umuMP = getActiveUMUCount();
        final int tmmUMU = (umuMP > 0) ? Compute.
                getTargetMovementModifier(umuMP, false, false, game).getValue()
                : 0;

        double targetMovementModifier = Math.max(tmmRan, Math.max(tmmJumped,
                tmmUMU));
        double tmmFactor = 1 + (targetMovementModifier / 10);
        if(hasDEST()) {
            tmmFactor += 0.1;
        }
        if(hasSneakCamo()) {
            tmmFactor += 0.2;
        }
        if(hasSneakIR()) {
            tmmFactor += 0.2;
        }
        if(hasSneakECM()) {
            tmmFactor += 0.1;
        }
        dbv *= tmmFactor;
        // double weaponbv;
        double obv;
        // adjust further for speed factor
        // this is a bit weird, because the formula gives
        // a different result than the table, because MASC/TSM
        // is handled differently (page 315, TM, compare
        // http://forums.classicbattletech.com/index.php/topic,20468.0.html
        double speedFactor;
        double speedFactorTableLookup = getRunMP(false, true, true)
                + Math.round(Math.max(jumpMP, umuMP) / 2.0);
        if (speedFactorTableLookup > 25) {
            speedFactor = Math.pow(1 + ((((double) walkMP
                + (Math.round(Math.max(jumpMP, umuMP) / 2.0))) - 5) / 10), 1.2);
        } else {
            speedFactor = Math
                    .pow(1 + ((speedFactorTableLookup - 5) / 10), 1.2);
        }
        speedFactor = Math.round(speedFactor * 100) / 100.0;
        double wbv = 0;
        if(null != primaryW) {
            wbv += primaryW.getBV(this) * (squadsize - secondn);
        }
        if(null != secondW) {
            wbv += secondW.getBV(this) * (secondn);
        }
        wbv = wbv * (men/squadsize);
        //if anti-mek then double this
        //TODO: need to factor archaic weapons out of this
        if(isAntiMekTrained()) {
            wbv *= 2;
        }
        //add in field gun BV
        for (Mounted mounted : getEquipment()) {
            if(mounted.getLocation() == LOC_FIELD_GUNS) {
                wbv += mounted.getType().getBV(this);
            }
        }
        obv = wbv * speedFactor;
        int bv;
        if (useGeometricMeanBV()) {
            bv = (int)Math.round(2 * Math.sqrt(obv * dbv));
            if (bv == 0) {
                bv = (int)Math.round(dbv + obv);
            }
        } else {
            bv = (int) Math.round(obv + dbv);
        }
        // and then factor in pilot
        double pilotFactor = 1;
        if (!ignorePilot) {
            pilotFactor = getCrew().getBVSkillMultiplier(isAntiMekTrained(), game);
        }
        return (int) Math.round((bv) * pilotFactor);

    } // End public int calculateBattleValue()

    @Override
    public Vector<Report> victoryReport() {
        Vector<Report> vDesc = new Vector<Report>();

        Report r = new Report(7025);
        r.type = Report.PUBLIC;
        r.addDesc(this);
        vDesc.addElement(r);

        r = new Report(7040);
        r.type = Report.PUBLIC;
        r.newlines = 0;
        vDesc.addElement(r);
        vDesc.addAll(getCrew().getDescVector(true));
        r = new Report(7070, Report.PUBLIC);
        r.add(getKillNumber());
        vDesc.addElement(r);

        if (isDestroyed()) {
            Entity killer = game.getEntity(killerId);
            if (killer == null) {
                killer = game.getOutOfGameEntity(killerId);
            }
            if (killer != null) {
                r = new Report(7072, Report.PUBLIC);
                r.addDesc(killer);
            } else {
                r = new Report(7073, Report.PUBLIC);
            }
            vDesc.addElement(r);
        }
        r.newlines = 2;

        return vDesc;
    }

    /**
     * Infantry don't need piloting rolls.
     */
    @Override
    public PilotingRollData addEntityBonuses(PilotingRollData prd) {
        return prd;
    }

    /**
     * Infantry can only change 1 elevation level at a time unless Mountain Inf which is 3.
     */
    @Override
    public int getMaxElevationChange() {
        if (hasMountain()) {
        return 3;
        }
        return 1;
    }

    /**
     * Update the platoon to reflect damages taken in this phase.
     */
    @Override
    public void applyDamage() {
        super.applyDamage();
        menShooting = men;
    }

    // The methods below aren't in the Entity interface.

    /**
     * Get the number of men in the platoon (before damage is applied).
     */
    public int getShootingStrength() {
        return menShooting;
    }

    @Override
    public boolean canCharge() {
        // Infantry can't Charge
        return false;
    }

    @Override
    public boolean canDFA() {
        // Infantry can't DFA
        return false;
    }

    /**
     * Checks if the entity is moving into a swamp. If so, returns the target
     * roll for the piloting skill check. now includes the level 3 terains which
     * can bog down
     */
    public PilotingRollData checkBogDown(MoveStep step, IHex curHex,
            Coords lastPos, Coords curPos, boolean isPavementStep) {
        PilotingRollData roll = new PilotingRollData(getId(), 5,
                "entering boggy terrain");
        int bgMod = curHex.getBogDownModifier(getMovementMode(), false);
        final boolean onBridge = (curHex.terrainLevel(Terrains.BRIDGE) > 0)
                && (getElevation() == curHex.terrainLevel(Terrains.BRIDGE_ELEV));
        if (!lastPos.equals(curPos) && (bgMod != TargetRoll.AUTOMATIC_SUCCESS)
                && (step.getMovementType(false) != EntityMovementType.MOVE_JUMP)
                && (getMovementMode() != EntityMovementMode.HOVER)
                && (getMovementMode() != EntityMovementMode.VTOL)
                && (getMovementMode() != EntityMovementMode.WIGE)
                && (step.getElevation() == 0) && !isPavementStep && !onBridge) {
            roll.append(new PilotingRollData(getId(), bgMod,
                    "avoid bogging down"));
        } else {
            roll.addModifier(TargetRoll.CHECK_FALSE,
                    "Check false: Not entering bog-down terrain, "
                            + "or jumping/hovering over such terrain");
        }
        return roll;
    }

     
  /**
  * This combines the old getCost and getAlternativeCost methods into a revised getCost Method.  
    *this better considers AntiMek training and Weapons and armor costs.  
  */
    @Override
    public double getCost(boolean ignoreAmmo) {
        double multiplier = 1;     //Cost Multiplier per TM
        double pweaponCost = 0;  //Primary Weapon Cost
        double sweaponCost = 0; // Secondary Weapon Cost
        double armorcost = 0; //Armor Cost
        double cost = 0; //Total Final Cost of Platoon or Squad.
        double primarySquad = 0; //Number of Troopers with Primary Weapon Only
        double secondSquad = 0; //Number oif Troopers with Secondary Weapon Only.
        
        //Weapon Cost Calculation
        if(null != primaryW) {
            pweaponCost += Math.sqrt(primaryW.getCost(this, false, -1)) * 2000;
        }
        if(null != secondW) {
            sweaponCost += Math.sqrt(secondW.getCost(this, false, -1)) * 2000;  
        }
       
        //Determining Break down of who would have primary and secondary weapons.
        primarySquad = (squadsize - secondn) * squadn;
        secondSquad = menStarting - primarySquad;
        
        //Squad Cost with just the weapons.
        cost = (primarySquad * pweaponCost) + (secondSquad * sweaponCost);
        
        
        //add in infantry armor cost
        if(damageDivisor > 1) {
            if(isArmorEncumbering()) {
                armorcost += 1600;
            } else {
                armorcost += 4300;
            }
        }
        int nSneak = 0;
        if(hasSneakCamo()) {
            nSneak++;
        }
        if(hasSneakECM()) {
            nSneak++;
        }
        if(hasSneakIR()) {
            nSneak++;
        }

        if(hasDEST()) {
            armorcost += 50000;
        }
        else if(nSneak == 1) {
            armorcost += 7000;
        }
        else if(nSneak == 2) {
            armorcost += 21000;
        }
        else if(nSneak == 3) {
            armorcost += 28000;
        }

        if(hasSpaceSuit()) {
            armorcost += 5000;
        }
        
        //Cost of armor on a per man basis added
        cost += (armorcost * menStarting);
        

        //Anti-Mek Trained Multiplier
        if (isAntiMekTrained()) {
            multiplier = 1;
        }

        //Add in motive type costs
        switch (getMovementMode()){
            case INF_UMU:
                multiplier *= 2.0;
            case INF_LEG:
                multiplier *= 1.0;
                break;
            case INF_MOTORIZED:
                multiplier *= 1.6;
                break;
            case INF_JUMP:
                multiplier *= 2.6;
                break;
            case HOVER:
                multiplier *= 3.2;
                break;
            case WHEELED:
                multiplier *= 3.2;
                break;
            case TRACKED:
                multiplier *= 3.2;
                break;
            default:
                break;
        }

        cost = cost * multiplier;

        //add in field gun costs
        for (Mounted mounted : getEquipment()) {
            if(mounted.getLocation() == LOC_FIELD_GUNS) {
                cost += Math.floor(mounted.getType().getCost(this, false, mounted.getLocation()));
            }
        }
        return cost;
    }
    
    /**
     * The alternate cost here is used by MekHQ to create costs that reflect just the cost of 
     * equipment. The motive costs here are based on the costs associated with an auto-rifle
     * platoon.
     */
    @Override
    public double getAlternateCost() {
        double cost = 0;
        if(null != primaryW) {
            cost += primaryW.getCost(this, false, -1) * (squadsize - secondn);
        }
        if(null != secondW) {
            cost += secondW.getCost(this, false, -1) * secondn;
        }
        cost = cost / squadsize;
        //Add in motive type costs
        switch (getMovementMode()){
            case INF_UMU:
                cost += 17888 * 1;
                break;
            case INF_LEG:
                break;
            case INF_MOTORIZED:
                cost += 17888 * 0.6;
                break;
            case INF_JUMP:
                cost += 17888 * 1.6;
                break;
            case HOVER:
            case WHEELED:
            case TRACKED:
                cost += 17888 * 2.2;
                break;
            default:
                break;
        }
        cost *= menStarting;
        //add in field gun costs
        for (Mounted mounted : getEquipment()) {
            if(mounted.getLocation() == LOC_FIELD_GUNS) {
                cost += mounted.getType().getCost(this, false, -1);
            }
        }
        return cost;
    }


    @Override
    public boolean doomedInVacuum() {
        return !hasSpaceSuit();
    }

    @Override
    public boolean doomedOnGround() {
        return false;
    }

    @Override
    public boolean doomedInAtmosphere() {
        return true;
    }

    @Override
    public boolean doomedInSpace() {
        return true;
    }
    @Override
    public boolean canAssaultDrop() {
        return game.getOptions().booleanOption("paratroopers");
    }

    @Override
    public boolean isEligibleFor(IGame.Phase phase) {
        if ((turnsLayingExplosives > 0) && (phase != IGame.Phase.PHASE_PHYSICAL)) {
            return false;
        }
        if ((dugIn != DUG_IN_COMPLETE) && (dugIn != DUG_IN_NONE)) {
            return false;
        }
        return super.isEligibleFor(phase);
    }

    @Override
    public boolean isEligibleForFiring() {
        if(game.getOptions().booleanOption("tacops_fast_infantry_move")) {
            if(moved == EntityMovementType.MOVE_RUN) {
                return false;
            }
        }
        return super.isEligibleForFiring();
    }

    @Override
    public void newRound(int roundNumber) {
        if (turnsLayingExplosives >= 0) {
            turnsLayingExplosives++;
            if (!(Compute.isInBuilding(game, this))) {
                turnsLayingExplosives = -1; // give up if no longer in a
                // building
            }
        }
        if ((dugIn != DUG_IN_COMPLETE) && (dugIn != DUG_IN_NONE)) {
            dugIn++;
            if (dugIn > DUG_IN_FORTIFYING2) {
                dugIn = DUG_IN_NONE;
            }
        }
        
        setTakingCover(false);
        super.newRound(roundNumber);
    }

    public void setDugIn(int i) {
        dugIn = i;
    }

    public int getDugIn() {
        return dugIn;
    }

    @Override
    public boolean isNuclearHardened() {
        return false;
    }

    /**
     * This function is called when loading a unit into a transport. This is
     * overridden to ensure infantry are no longer considered dug in when they
     * are being transported.
     *
     * @param transportID
     */
    public void setTransportID(int transportID) {
        super.setTransportId(transportID);

        setDugIn(DUG_IN_NONE);
    }

    /**
     * Convenience method for setting the anti-mek skill of the unit based on
     * whether or not they have anti-mek training.  If the input is false, the
     * anti-mek skill is set to the default untrained value, otherwise it's
     * set to the default value based on motive type.
     * 
     * @param amTraining
     */
    public void setAntiMekSkill(boolean amTraining) {
        if (getCrew() == null) {
            return;
        }
        if (amTraining) {
            if ((getMovementMode() == EntityMovementMode.INF_MOTORIZED)
                    || getMovementMode() == EntityMovementMode.INF_JUMP) {
                getCrew().setPiloting(ANTI_MECH_SKILL_JUMP);
            } else {
                getCrew().setPiloting(ANTI_MECH_SKILL_FOOT);
            }
        } else {
            getCrew().setPiloting(ANTI_MECH_SKILL_UNTRAINED);            
        }
    }
    
    /**
     * Set the anti-mek skill for this unit.  Since Infantry don't have piloting
     * the crew's piloting skill is treated as the anti-mek skill.  This is
     * largely just a convenience method for setting the Crew's piloting skill.
     * @param amSkill
     */
    public void setAntiMekSkill(int amSkill) {
        if (getCrew() == null) {
            return;
        }
        getCrew().setPiloting(amSkill);
    }
    
    /**
     * Returns the anti-mek skill for this unit.  Since Infantry don't have 
     * piloting the crew's piloting skill is treated as the anti-mek skill.  
     * This is largely just a convenience method for setting the Crew's piloting
     * skill.
     * @return
     */
    public int getAntiMekSkill() {
        if (getCrew() == null) {
            return ANTI_MECH_SKILL_UNTRAINED;
        } else {
            return getCrew().getPiloting();
        }
    }

    /**
     * Returns true if this unit has anti-mek training.  According to TM pg 155,
     * any unit that has less than 8 anti-mek skill is assumed to have anti-mek
     * training.  This implies that the unit carries the requisite equipment for
     * properly performing anti-mek attacks (and the weight and cost that goes
     * along with that).
     * @return
     */
    public boolean isAntiMekTrained() {
        // Anything below the antimech skill default is considered to be AM
        // trained.  See TM pg 155
        return getAntiMekSkill() < ANTI_MECH_SKILL_UNTRAINED;
    }

    public boolean isMechanized() {
        if ((getMovementMode() == EntityMovementMode.WHEELED) ||
                (getMovementMode() == EntityMovementMode.HOVER) ||
                (getMovementMode() == EntityMovementMode.TRACKED)) {
            return true;
        }
        return false;
    }

    /*
     * (non-Javadoc)
     * @see megamek.common.Entity#getTotalCommGearTons()
     */
    @Override
    public int getTotalCommGearTons() {
        return 0;
    }

    public double getDamageDivisor() {
        return damageDivisor;
    }

    public void setDamageDivisor(double d) {
        damageDivisor = d;
    }

    public boolean isArmorEncumbering() {
        return encumbering;
    }

    public void setArmorEncumbering(boolean b) {
        encumbering = b;
    }

    public boolean hasSpaceSuit() {
        return spaceSuit;
    }

    public void setSpaceSuit(boolean b) {
        spaceSuit = b;
    }

    public boolean hasDEST() {
        return dest;
    }

    public void setDEST(boolean b) {
        dest = b;
    }
    
    public boolean hasMountain() {
        return mountain;
    }
    
    public void setMountain(boolean b) {
        mountain = b;
    }
    
    public boolean hasSneakCamo() {
        return sneak_camo;
    }

    public void setSneakCamo(boolean b) {
        sneak_camo = b;
    }

    public boolean hasSneakIR() {
        return sneak_ir;
    }

    public void setSneakIR(boolean b) {
        sneak_ir = b;
    }

    public boolean hasSneakECM() {
        return sneak_ecm;
    }

    public void setSneakECM(boolean b) {
        sneak_ecm = b;
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
    @Override
    public TargetRoll getStealthModifier(int range, Entity ae) {
        TargetRoll result = null;

        // Note: infantry are immune to stealth, but not camoflage
        // or mimetic armor

        if ((sneak_ir || dest)
                && !(ae instanceof Infantry)) {
            switch (range) {
                case RangeType.RANGE_MINIMUM:
                case RangeType.RANGE_SHORT:
                case RangeType.RANGE_MEDIUM:
                    result = new TargetRoll(+1, "Sneak, IR/DEST suit");
                    break;
                case RangeType.RANGE_LONG:
                case RangeType.RANGE_EXTREME:
                case RangeType.RANGE_LOS:
                    result = new TargetRoll(+2, "Sneak, IR/DEST suit");
                    break;
                case RangeType.RANGE_OUT:
                    break;
                default:
                    throw new IllegalArgumentException(
                            "Unknown range constant: " + range);
            }
        }

        // Simple camo modifier is on top of the movement modifier
        // This can also be in addition to IR/DEST stealth mods!
        if (sneak_camo && (delta_distance < 3)) {
            int mod = Math.max(3 - delta_distance, 0);
            if (result == null) {
                result = new TargetRoll(mod, "Sneak, Camo");
            } else {
                result.append(new TargetRoll(mod, "Sneak, Camo"));
            }
        }

        if (dest && (delta_distance == 0)) {
            if (result == null) {
                result = new TargetRoll(1, "DEST suit");
            } else {
                result.append(new TargetRoll(1, "DEST Suit"));
            }
        }


        if (result == null) {
            result = new TargetRoll(0, "no sneak mods");
        }

        // Return the result.
        return result;
    } // End public TargetRoll getStealthModifier( char )

    public void setPrimaryWeapon(InfantryWeapon w) {
        primaryW = w;
        primaryName = w.getName();
    }

    public InfantryWeapon getPrimaryWeapon() {
        return primaryW;
    }

    public void setSecondaryWeapon(InfantryWeapon w) {
        secondW = w;
        if(null == w) {
            secondName = null;
        } else {
            secondName = w.getName();
        }
    }

    public InfantryWeapon getSecondaryWeapon() {
        return secondW;
    }

    public void setSquadSize(int size) {
        squadsize = size;
    }

    public int getSquadSize() {
        return squadsize;
    }

    public void setSquadN(int n) {
        squadn = n;
    }

    public int getSquadN() {
        return squadn;
    }

    public void setSecondaryN(int n) {
        secondn = n;
    }

    public int getSecondaryN() {
        return secondn;
    }

    public double getDamagePerTrooper() {

        if(null == primaryW) {
            return 0;
        }

        double damage = primaryW.getInfantryDamage() * (squadsize - secondn);
        if(null != secondW) {
            damage += secondW.getInfantryDamage() * secondn;
        }
        return damage/squadsize;
    }

    public boolean isSquad() {
        return (squadn == 1);
    }

    /**
     * Set the movement type of the entity
     */
    @Override
    public void setMovementMode(EntityMovementMode movementMode) {
        super.setMovementMode(movementMode);
        //movement mode will determine base mp
        if (!(this instanceof BattleArmor)) {
            setOriginalJumpMP(0);
            switch (getMovementMode()) {
                case INF_MOTORIZED:
                    setOriginalWalkMP(3);
                    break;
                case HOVER:
                    setOriginalWalkMP(5);
                    break;
                case TRACKED:
                    setOriginalWalkMP(3);
                    break;
                case WHEELED:
                    setOriginalWalkMP(4);
                    break;
                case INF_JUMP:
                    //fall through to get the original Walk MP is deliberate
                    setOriginalJumpMP(3);
                case INF_LEG:
                    setOriginalWalkMP(1);
                    break;
                default:
                    setOriginalWalkMP(1);
            }
        }
    }


    public boolean canMakeAntiMekAttacks() {
        return !isMechanized();
    }

    @Override
    public float getWeight() {
        float ton;
        switch (getMovementMode()) {
            case INF_MOTORIZED:
                ton = (float) (men * 0.195);
                break;
            case HOVER:
            case TRACKED:
            case WHEELED:
                ton = (float) (men * 1);
                break;
            case INF_JUMP:
                ton = (float) (men * 0.165);
                break;
            case INF_LEG:
            default:
                ton = (float) (men * 0.085);
        }
        
        if(isAntiMekTrained()) {
                ton += (float) (men * .015);
                        
        }

        //add in field gun weight
        for (Mounted mounted : getEquipment()) {
            if(mounted.getLocation() == LOC_FIELD_GUNS) {
                ton += mounted.getType().getTonnage(this);
            }
        }
        return TestEntity.round(ton, TestEntity.CEIL_QUARTERTON);

    }
    public String getArmorDesc() {
        StringBuffer sArmor = new StringBuffer();
        sArmor.append(getDamageDivisor());
        if(isArmorEncumbering()) {
            sArmor.append("E");
        }

        if(hasDEST()) {
            sArmor.append(" (DEST) ");
        }

        if(hasSneakCamo()) {
            sArmor.append(" (Camo) ");
        }

        if(hasSneakIR()) {
            sArmor.append(" (IR) ");
        }

        if(hasSneakECM()) {
            sArmor.append(" (ECM) ");
        }


        return sArmor.toString();

    }

    /**
     * Restores the entity after serialization
     */
    @Override
    public void restore() {
        super.restore();

        if (null != primaryName) {
            primaryW = (InfantryWeapon)EquipmentType.get(primaryName);
        }

        if(null != secondName) {
            secondW = (InfantryWeapon)EquipmentType.get(secondName);
        }
    }

    public boolean hasActiveFieldArtillery() {
        boolean hasArtillery = false;
        float smallestGun = 100.0f;
        for(Mounted wpn : getWeaponList()) {
            if(wpn.getLocation() != LOC_FIELD_GUNS) {
                continue;
            }
            if(wpn.getType().hasFlag(WeaponType.F_ARTILLERY)) {
                hasArtillery = true;
                if(wpn.getType().getTonnage(this) < smallestGun) {
                    smallestGun = wpn.getType().getTonnage(this);
                }
            }
        }

        //you must have enough men to fire at least the smallest piece
        return hasArtillery && (getShootingStrength() >= smallestGun);

    }

    /**
     * Infantry don't use MP to change facing, and don't
     * do PSRs, so just don't let them use maneuvering ace
     * otherwise, their movement gets screwed up
     */
    @Override
    public boolean isUsingManAce() {
        return false;
    }

    @Override
    public int getBattleForceSize() {
        //The tables are on page 356 of StartOps
        return 1;
    }

    @Override
    public int getBattleForceArmorPoints() {
        // Infantry armor points is # of men / 15
        return (int) Math.ceil(getArmor(0)/15.0);
    }

    @Override
    /**
     * Each squad has 1 structure point
     */
    public int getBattleForceStructurePoints() {
        return 1;
    }

    @Override
    public int getEngineHits() {
        return 0;
    }

    @Override
    public String getLocationDamage(int loc) {
        return "";
    }

    @Override
    public boolean isCrippled() {
        double activeTroopPercent = (double)getInternal(LOC_INFANTRY) / getOInternal(LOC_INFANTRY);
        if (activeTroopPercent < 0.25) {
            if (PreferenceManager.getClientPreferences().debugOutputOn()) {
                System.out.println(getDisplayName()
                        + " CRIPPLED: only "
                        + NumberFormat.getPercentInstance().format(
                                activeTroopPercent) + " troops remaining.");
            }
            return true;
        }
        return false;
    }
    
    @Override
    public boolean isCrippled(boolean checkCrew) {
        return isCrippled();
    }

    @Override
    public boolean isDmgHeavy() {
        return (((double)getInternal(LOC_INFANTRY) / getOInternal(LOC_INFANTRY)) < 0.5);
    }

    @Override
    public boolean isDmgModerate() {
        return (((double)getInternal(LOC_INFANTRY) / getOInternal(LOC_INFANTRY)) < 0.75);
    }

    @Override
    public boolean isDmgLight() {
        return (((double)getInternal(LOC_INFANTRY) / getOInternal(LOC_INFANTRY)) < 0.9);
    }

    public boolean hasFieldGun() {
        for (Mounted m : getWeaponList()) {
            if (m.getLocation() == Infantry.LOC_FIELD_GUNS) {
                return true;
            }
        }
        return false;
    }

    /**
     * Mounts the specified equipment in the specified location.
     */
    @Override
    public void addEquipment(Mounted mounted, int loc, boolean rearMounted) throws LocationFullException {
        // Implement parent's behavior.
        super.addEquipment(mounted, loc, rearMounted);

        //we do need to equipment slots for ammo switching of field guns and field artillery
        // Add the piece equipment to our slots.
        addCritical(loc, new CriticalSlot(mounted));

    }

    @Override
    public long getEntityType(){
        return Entity.ETYPE_INFANTRY;
    }
    
    public PilotingRollData checkLandingInHeavyWoods(
            EntityMovementType overallMoveType, IHex curHex) {
        PilotingRollData roll = getBasePilotingRoll(overallMoveType);
        roll.addModifier(TargetRoll.CHECK_FALSE,
                         "Infantry cannot fall");
        return roll;
    }
    
    /**
     * Determines if there is valid cover for an infantry unit to utilize the
     * Using Non-Infantry as Cover rules (TO pg 108).
     * @param game
     * @param position
     * @return
     */
    public static boolean hasValidCover(IGame game, Coords pos, int elevation) {
        // Can't do anything if we don't have a position
        // If elevation > 0, we're either flying, or in a building
        // In either case, we shouldn't be allowed to take cover
        if ((pos == null) || (elevation > 0)) {
            return false;
        }
        boolean hasMovedEntity = false;
        // First, look for ground untis in the same hex that have already moved
        for (Entity e : game.getEntitiesVector(pos)) {
            if (e.isDone() && !(e instanceof Infantry) 
                    && (e.getElevation() == elevation)) {
                hasMovedEntity = true;
                break;
            }
        }
        // If we didn't find anything, check for wrecks
        // The rules don't explicitly cover this, but it makes sense
        if (!hasMovedEntity) {
            Enumeration<Entity> wrecks = game.getWreckedEntities();
            while (wrecks.hasMoreElements()) {
                Entity e = wrecks.nextElement();
                if (pos.equals(e.getPosition()) 
                        && !(e instanceof Infantry)) {
                    hasMovedEntity = true;
                }
            }
        }
        return hasMovedEntity;
    }

    public boolean isTakingCover() {
        return isTakingCover;
    }

    public void setTakingCover(boolean isTakingCover) {
        this.isTakingCover = isTakingCover;
    }
    
} // End class Infantry

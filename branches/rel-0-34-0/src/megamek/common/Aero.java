/*
 * MegaMek - Copyright (C) 2000-2003 Ben Mazur (bmazur@sev.org)
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
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.Vector;

import megamek.common.weapons.EnergyWeapon;
import megamek.common.weapons.GaussWeapon;

/**
 * Taharqa's attempt at creating an Aerospace entity
 */
public class Aero
    extends Entity
    implements Serializable
{
    /**
     *
     */
    private static final long serialVersionUID = 7196307097459255187L;

    //these should probably go at some point
    private boolean m_bImmobile = false;
    private boolean m_bImmobileHit = false;
    protected int movementDamage = 0;

    // locations
    public static final int        LOC_NOSE               = 0;
    public static final int        LOC_LWING              = 1;
    public static final int        LOC_RWING              = 2;
    public static final int        LOC_AFT                = 3;
    public static final int        LOC_WINGS              = 4;

    //ramming angles
    public static final int        RAM_TOWARD_DIR         = 0;
    public static final int        RAM_TOWARD_OBL         = 1;
    public static final int        RAM_AWAY_OBL           = 2;
    public static final int        RAM_AWAY_DIR           = 3;

    //heat type
    public static final int           HEAT_SINGLE              = 0;
    public static final int           HEAT_DOUBLE              = 1;

    //cockpit types
    public static final int COCKPIT_STANDARD = 0;
    public static final int COCKPIT_SMALL = 1;
    public static final int COCKPIT_COMMAND_CONSOLE = 2;
    public static final String[] COCKPIT_STRING = { "Standard Cockpit", "Small Cockpit", "Command Console"};
    public static final String[] COCKPIT_SHORT_STRING = { "Standard","Small", "Command Console"};


    //critical hits
    public static final int CRIT_NONE             = -1;
    public static final int CRIT_CREW             = 0;
    public static final int CRIT_FCS              = 1;
    public static final int CRIT_WEAPON           = 2;
    public static final int CRIT_CONTROL          = 3;
    public static final int CRIT_SENSOR           = 4;
    public static final int CRIT_BOMB             = 5;
    public static final int CRIT_ENGINE           = 6;
    public static final int CRIT_FUEL_TANK        = 7;
    public static final int CRIT_AVIONICS         = 8;
    public static final int CRIT_GEAR             = 9;
    public static final int CRIT_HEATSINK         = 10;
    public static final int CRIT_CARGO             = 11;
    public static final int CRIT_DOCK_COLLAR       = 12;
    public static final int CRIT_DOOR              = 13;
    public static final int CRIT_KF_BOOM           = 14;
    public static final int CRIT_LIFE_SUPPORT      = 15;
    public static final int CRIT_LEFT_THRUSTER     = 16;
    public static final int CRIT_RIGHT_THRUSTER    = 17;
    public static final int CRIT_CIC               = 18;
    public static final int CRIT_KF_DRIVE          = 19;
    public static final int CRIT_GRAV_DECK         = 20;
    public static final int CRIT_WEAPON_BROAD      = 21;

    // aeros have no critical slot limitations
    //this needs to be larger, it is too easy to go over when you get to warships
    //and bombs and such
    private static final int[] NUM_OF_SLOTS = {100, 100, 100, 100, 100, 100};

    protected static String[] LOCATION_ABBRS = { "NOS", "LWG", "RWG", "AFT", "WNG" };
    protected static String[] LOCATION_NAMES = { "Nose", "Left Wing", "Right Wing", "Aft", "Wings" };

    @Override
    public String[] getLocationAbbrs() { return LOCATION_ABBRS; }
    @Override
    public String[] getLocationNames() { return LOCATION_NAMES; }

    private int structureType = 0;
    private int sensorHits = 0;
    private int fcsHits = 0;
    private int engineHits = 0;
    private int avionicsHits = 0;
    private int cicHits = 0;
    private boolean gearHit = false;
    private int structIntegrity;
    private int orig_structIntegrity;
    //set up damage threshold
    private int damThresh[] = {0,0,0,0,0};
    //set up an int for what the critical effect would be
    private int potCrit = CRIT_NONE;
    //need to set up standard damage here
    private int[] standard_damage = {0,0,0,0,0};
    //ignored crew hit for harjel
    private int ignoredCrewHits = 0;
    private int cockpitType = COCKPIT_STANDARD;

    //track straight movement from last turn
    private int straightMoves = 0;

    private boolean spheroid = false;

    //deal with heat
    private int heatSinks;
    private int heatType = HEAT_SINGLE;

    //bombs
    public static final String  SPACE_BOMB_ATTACK      = "SpaceBombAttack";

    private int maxBombPoints = 0;
    private int[] bombChoices = new int[BombType.B_NUM];

    //fuel
    private int fuel = 0;

    //these are used by more advanced aeros
    private boolean lifeSupport = true;
    private int leftThrustHits = 0;
    private int rightThrustHits = 0;

    //out of control
    private boolean outControl = false;
    private boolean outCtrlHeat = false;
    private boolean randomMove = false;

    //set up movement
    private int currentVelocity = 0;
    private int nextVelocity = currentVelocity;
    private boolean accLast = false;
    private boolean rolled = false;
    private boolean failedManeuver = false;
    private boolean accDecNow = false;

    //was the damage threshold exceeded this turn
    boolean critThresh = false;

    //vstol status
    boolean vstol = false;

    //Capital Fighter stuff
    private int capitalArmor = 0;
    private int capitalArmor_orig = 0;
    private int fatalThresh = 2;
    private int currentDamage = 0;
    private boolean wingsHit = false;
    //a hash map of the current weapon groups - the key is the location:internal name, and the value is the weapon id
    Map<String, Integer> weaponGroups = new HashMap<String, Integer>();

    /*
     * According to the rules if two units of the same type and with the
     * same velocity are in the same hex, you roll 2d6 randomly to see who
     * is considered back one step for purposes of targeting.  THis is a bitch
     * to do, so instead we assign a large random variable to each aero unit at the start
     * of the round and we use that. It works out similarly except that you don't roll
     * separately for each pair of possibilities.  That should work well enough for our
     * purposes.
     */
    private int whoFirst = 0;

    private int eccmRoll = 0;

    /**
    * Returns this entity's safe thrust, factored
    * for heat, extreme temperatures, gravity, and bomb load.
    */
    @Override
    public int getWalkMP(boolean gravity, boolean ignoreheat) {
        int j = getOriginalWalkMP();
        j = Math.max(0, j - getCargoMpReduction());
        if(null != game) {
            int weatherMod = game.getPlanetaryConditions().getMovementMods(this);
            if(weatherMod != 0) {
                j = Math.max(j + weatherMod, 0);
            }
        }
        //get bomb load
        j = Math.max(0, j - (int)Math.ceil(getBombPoints()/5.0));

        if ( hasModularArmor() ) {
            j--;
        }

        return j;
    }

    /**
     * Returns the number of locations in the entity
     */
    @Override
    public int locations() {
         return 5;
    }

    @Override
    public boolean canChangeSecondaryFacing() {
        return false;
    }

    @Override
    public boolean isValidSecondaryFacing(int n) {
        return false;
    }

    @Override
    public int clipSecondaryFacing(int n) {
        return n;
    }

    public boolean isOutControlTotal() {
        //due to control roll, heat, shut down, or crew unconscious
        return (outControl || shutDown || crew.isUnconscious());
    }

    public boolean isOutControl() {
        return outControl;
    }

    public boolean isOutCtrlHeat() {
        return outCtrlHeat;
    }

    public boolean isRandomMove() {
        return randomMove;
    }

    public boolean didAccLast() {
        return accLast;
    }

    public boolean hasLifeSupport() {
        return lifeSupport;
    }

    public void setLifeSupport(boolean b) {
        lifeSupport = b;
    }

    public boolean isRolled() {
        return rolled;
    }

    public void setOutControl(boolean ocontrol) {
        outControl = ocontrol;
    }

    public void setOutCtrlHeat(boolean octrlheat) {
        outCtrlHeat = octrlheat;
    }

    public void setRandomMove(boolean randmove) {
        randomMove = randmove;
    }

    public void setRolled(boolean roll) {
        rolled = roll;
    }

    public void setAccLast(boolean b) {
        accLast = b;
    }

    public int getBombPoints() {
        int points = 0;
        for(Mounted bomb : getBombs()) {
            if(bomb.getShotsLeft() > 0) {
                points += BombType.getBombCost(((BombType)bomb.getType()).getBombType());
            }
        }
        return points;
    }

    public int getMaxBombPoints() {
        return maxBombPoints;
    }

    public void autoSetMaxBombPoints() {
        maxBombPoints = Math.round(getWeight()/5);
    }

    public int[] getBombChoices() {
        return bombChoices.clone();
    }

    public void setBombChoices(int[] bc) {
        if(bc.length == bombChoices.length) {
            bombChoices = bc;
        }
    }

    public void setWhoFirst() {
        whoFirst = Compute.randomInt(500);
    }

    public int getWhoFirst() {
        return whoFirst;
    }

    public int getCurrentVelocity() {
        //if using advanced movement then I just want to sum up
        //the different vectors
        if(game.useVectorMove()) {
            return getVelocity();
        }
        return currentVelocity;
    }

    public void setCurrentVelocity(int velocity) {
        currentVelocity = velocity;
    }

    public int getNextVelocity() {
        return nextVelocity;
    }

    public void setNextVelocity(int velocity) {
        nextVelocity = velocity;
    }

    //need some way of retrieving true current velocity
    //even when using advanced movement
    public int getCurrentVelocityActual() {
        return currentVelocity;
    }

    public int getPotCrit() {
        return potCrit;
    }

    public void setPotCrit(int crit) {
        potCrit = crit;
    }

    public int getSI() {
        return structIntegrity;
    }

    public int get0SI() {
        return orig_structIntegrity;
    }

    public int getCapArmor() {
        return capitalArmor;
    }

    public void setCapArmor(int i) {
        capitalArmor = i;
    }

    public int getCap0Armor() {
        return capitalArmor_orig;
    }

    public int getFatalThresh() {
        return fatalThresh;
    }

    public int getCurrentDamage() {
        return currentDamage;
    }

    public void setCurrentDamage(int i) {
        currentDamage = i;
    }

    public void set0SI(int si) {
        orig_structIntegrity = si;
        structIntegrity = si;
    }


    public void autoSetSI() {
        int siweight = (int)Math.floor(weight / 10.0);
        int sithrust = getOriginalWalkMP();
        initializeSI(Math.max(siweight,sithrust));
    }

    public void autoSetCapArmor() {
        capitalArmor_orig = (int)Math.round(getTotalOArmor() / 10.0);
        capitalArmor = (int)Math.round(getTotalArmor() / 10.0);
    }

    public void autoSetFatalThresh() {
        fatalThresh = Math.max(2, (int)Math.ceil(capitalArmor/4.0));
    }

    public void initializeSI(int val) {
        orig_structIntegrity = val;
        setSI(val);
    }

    public void setSI(int si) {
        structIntegrity = si;
    }

    public int getSensorHits() {
        return sensorHits;
    }

    public void setSensorHits(int hits) {
        if(hits > 3) {
            hits = 3;
        }
        sensorHits = hits;
    }

    public int getFCSHits() {
        return fcsHits;
    }

    public void setFCSHits(int hits) {
        if(hits > 3) {
            hits = 3;
        }
        fcsHits = hits;
    }

    public void setCICHits(int hits) {
        cicHits = hits;
    }

    public int getCICHits() {
        return cicHits;
    }

    public void setIgnoredCrewHits(int hits) {
        ignoredCrewHits = hits;
    }

    public int getIgnoredCrewHits() {
        return ignoredCrewHits;
    }

    public int getEngineHits() {
        return engineHits;
    }

    public void setEngineHits(int hits) {
        engineHits = hits;
    }

    public int getAvionicsHits() {
        return avionicsHits;
    }

    public void setAvionicsHits(int hits) {
        avionicsHits = hits;
    }

    public boolean isGearHit() {
        return gearHit;
    }

    public void setGearHit(boolean hit) {
        gearHit = hit;
    }

    public void setHeatSinks(int hs) {
        heatSinks = hs;
    }

    public int getHeatSinks() {
        return heatSinks;
    }

    public void setHeatType(int hstype) {
        heatType = hstype;
    }

    public void setLeftThrustHits(int hits) {
        leftThrustHits = hits;
    }

    public int getLeftThrustHits() {
        return leftThrustHits;
    }

    public void setRightThrustHits(int hits) {
        rightThrustHits = hits;
    }

    public int getRightThrustHits() {
        return rightThrustHits;
    }

    public int getFuel() {
        return fuel;
    }

    public void setFuel(int gas) {
        fuel = gas;
    }

    public int getHeatType() {
        return heatType;
    }

    public boolean wasCritThresh() {
        return critThresh;
    }

    public void setCritThresh(boolean b) {
        critThresh = b;
    }

    public void immobilize()
    {
        m_bImmobileHit = true;
        setOriginalWalkMP(0);
    }

    @Override
    public boolean isImmobile()
    {
        //aeros never go "immobile"
        return false;
    }

    @Override
    public void applyDamage() {
        m_bImmobile |= m_bImmobileHit;
    }

    @Override
    public void newRound(int roundNumber) {
        super.newRound(roundNumber);

        //reset threshold critted
        setCritThresh(false);

        //reset maneuver status
        setFailedManeuver(false);
        //reset acc/dec this turn
        setAccDecNow(false);

        updateBays();

        //update recovery turn if in recovery
        if(getRecoveryTurn() > 0) {
            setRecoveryTurn(getRecoveryTurn() - 1);
        }

        //if in atmosphere, then halve next turn's velocity
        if(game.getBoard().inAtmosphere() && isDeployed() && (roundNumber > 0)) {
            setNextVelocity((int)Math.floor(getNextVelocity() / 2.0));
        }

        //update velocity
        setCurrentVelocity(getNextVelocity());

        //if using variable damage thresholds then autoset them
        if(game.getOptions().booleanOption("variable_damage_thresh")) {
            autoSetThresh();
        }

        //if they are out of control due to heat, then apply this and reset
        if(isOutCtrlHeat()) {
            setOutControl(true);
            setOutCtrlHeat(false);
        }


        //reset eccm bonus
        setECCMRoll(Compute.d6(2));

        //get new random whofirst
        setWhoFirst();

    }

    /**
     * Returns the name of the type of movement used.
     * This is tank-specific.
     */
    @Override
    public String getMovementString(int mtype) {
        switch(mtype) {
        case IEntityMovementType.MOVE_SKID :
            return "Skidded";
        case IEntityMovementType.MOVE_NONE :
            return "None";
        case IEntityMovementType.MOVE_WALK :
            return "Cruised";
        case IEntityMovementType.MOVE_RUN :
            return "Flanked";
        case IEntityMovementType.MOVE_JUMP :
            return "Jumped";
        case IEntityMovementType.MOVE_SAFE_THRUST :
            return "Safe Thrust";
        case IEntityMovementType.MOVE_OVER_THRUST :
            return "Over Thrust";
        default :
            return "Unknown!";
        }
    }

    /**
     * Returns the name of the type of movement used.
     * This is tank-specific.
     */
    @Override
    public String getMovementAbbr(int mtype) {
        switch(mtype) {
        case IEntityMovementType.MOVE_NONE :
            return "N";
        case IEntityMovementType.MOVE_SAFE_THRUST :
            return "S";
        case IEntityMovementType.MOVE_OVER_THRUST :
            return "O";
        default :
            return "?";
        }
    }

    @Override
    public boolean hasRearArmor(int loc) {
        return false;
    }

    /**
     * Returns the Compute.ARC that the weapon fires into.
     */
    //need to figure out aft-pointed wing weapons
    //need to figure out new arcs
    @Override
    public int getWeaponArc(int wn) {
        final Mounted mounted = getEquipment(wn);
        if(mounted.getType().hasFlag(WeaponType.F_SPACE_BOMB)) {
            return Compute.ARC_360;
        }
        int arc = Compute.ARC_NOSE;
        switch (mounted.getLocation()) {
            case LOC_NOSE:
                 arc = Compute.ARC_NOSE;
                 break;
            case LOC_RWING:
                if(mounted.isRearMounted()) {
                    arc = Compute.ARC_RWINGA;
                } else {
                    arc = Compute.ARC_RWING;
                }
                break;
            case LOC_LWING:
                if(mounted.isRearMounted()) {
                    arc = Compute.ARC_LWINGA;
                } else {
                    arc = Compute.ARC_LWING;
                }
                break;
            case LOC_AFT:
                arc = Compute.ARC_AFT;
                break;
            case LOC_WINGS:
                arc = Compute.ARC_NOSE;
                break;
            default:
                arc = Compute.ARC_360;
        }

        return rollArcs(arc);
    }

    /**
     * switches certain arcs due to rolling
     */
    public int rollArcs(int arc) {
        if(isRolled()) {
            if(arc == Compute.ARC_LWING) {
                return Compute.ARC_RWING;
            } else if( arc == Compute.ARC_RWING) {
                return Compute.ARC_LWING;
            } else if( arc == Compute.ARC_LWINGA) {
                return Compute.ARC_RWINGA;
            } else if( arc == Compute.ARC_RWINGA) {
                return Compute.ARC_LWINGA;
            } else if( arc == Compute.ARC_LEFTSIDE_SPHERE) {
                return Compute.ARC_RIGHTSIDE_SPHERE;
            } else if( arc == Compute.ARC_RIGHTSIDE_SPHERE) {
                return Compute.ARC_LEFTSIDE_SPHERE;
            } else if( arc == Compute.ARC_LEFTSIDEA_SPHERE) {
                return Compute.ARC_RIGHTSIDEA_SPHERE;
            } else if( arc == Compute.ARC_RIGHTSIDEA_SPHERE) {
                return Compute.ARC_LEFTSIDEA_SPHERE;
            } else if( arc == Compute.ARC_LEFT_BROADSIDE) {
                return Compute.ARC_RIGHT_BROADSIDE;
            } else if( arc == Compute.ARC_RIGHT_BROADSIDE) {
                return Compute.ARC_LEFT_BROADSIDE;
            }
        }
        return arc;
    }

    /**
     * Returns true if this weapon fires into the secondary facing arc.  If
     * false, assume it fires into the primary.
     */
    @Override
    public boolean isSecondaryArcWeapon(int weaponId) {
        //just leave true for now in case we implement rolls or
        //newtonian movement this way
        return true;
    }
    /**
     * Rolls up a hit location
     */
    @Override
    public HitData rollHitLocation(int table, int side, int aimedLocation, int aimingMode) {
        return rollHitLocation(table, side);
    }

    @Override
    public HitData rollHitLocation(int table, int side) {

    /*
     * Unlike other units, ASFs determine potential crits based on the to-hit roll
     * so I need to set this potential value as well as return the to hit data
     */

    int roll = Compute.d6(2);

    //first check for above/below
    if((table == ToHitData.HIT_ABOVE) || (table == ToHitData.HIT_BELOW)) {

        //have to decide which wing
        int wingloc = LOC_RWING;
        int wingroll = Compute.d6(1);
        if(wingroll > 3) {
            wingloc = LOC_LWING;
        }
        switch( roll ) {
        case 2:
            setPotCrit(CRIT_WEAPON);
            return new HitData(LOC_NOSE, false, HitData.EFFECT_NONE);
        case 3:
            setPotCrit(CRIT_GEAR);
            return new HitData(wingloc, false, HitData.EFFECT_NONE);
        case 4:
            setPotCrit(CRIT_SENSOR);
            return new HitData(LOC_NOSE, false, HitData.EFFECT_NONE);
        case 5:
            setPotCrit(CRIT_CREW);
            return new HitData(LOC_NOSE, false, HitData.EFFECT_NONE);
        case 6:
            setPotCrit(CRIT_WEAPON);
            return new HitData(wingloc, false, HitData.EFFECT_NONE);
        case 7:
            setPotCrit(CRIT_AVIONICS);
            return new HitData(LOC_NOSE, false, HitData.EFFECT_NONE);
        case 8:
            setPotCrit(CRIT_WEAPON);
            return new HitData(wingloc, false, HitData.EFFECT_NONE);
        case 9:
            setPotCrit(CRIT_CONTROL);
            return new HitData(LOC_AFT, false, HitData.EFFECT_NONE);
        case 10:
            setPotCrit(CRIT_ENGINE);
            return new HitData(LOC_AFT, false, HitData.EFFECT_NONE);
        case 11:
            setPotCrit(CRIT_GEAR);
            return new HitData(wingloc, false, HitData.EFFECT_NONE);
        case 12:
            setPotCrit(CRIT_WEAPON);
            return new HitData(LOC_AFT, false, HitData.EFFECT_NONE);
        }
    }

    if(side == ToHitData.SIDE_FRONT) {
        // normal front hits
        switch( roll ) {
        case 2:
            setPotCrit(CRIT_WEAPON);
            return new HitData(LOC_NOSE, false, HitData.EFFECT_NONE);
        case 3:
            setPotCrit(CRIT_SENSOR);
            return new HitData(LOC_NOSE, false, HitData.EFFECT_NONE);
        case 4:
            setPotCrit(CRIT_HEATSINK);
            return new HitData(LOC_RWING, false, HitData.EFFECT_NONE);
        case 5:
            setPotCrit(CRIT_WEAPON);
            return new HitData(LOC_RWING, false, HitData.EFFECT_NONE);
        case 6:
            setPotCrit(CRIT_AVIONICS);
            return new HitData(LOC_NOSE, false, HitData.EFFECT_NONE);
        case 7:
            setPotCrit(CRIT_CONTROL);
            return new HitData(LOC_NOSE, false, HitData.EFFECT_NONE);
        case 8:
            setPotCrit(CRIT_FCS);
            return new HitData(LOC_NOSE, false, HitData.EFFECT_NONE);
        case 9:
            setPotCrit(CRIT_WEAPON);
            return new HitData(LOC_LWING, false, HitData.EFFECT_NONE);
        case 10:
            setPotCrit(CRIT_HEATSINK);
            return new HitData(LOC_LWING, false, HitData.EFFECT_NONE);
        case 11:
            setPotCrit(CRIT_GEAR);
            return new HitData(LOC_NOSE, false, HitData.EFFECT_NONE);
        case 12:
            setPotCrit(CRIT_WEAPON);
            return new HitData(LOC_NOSE, false, HitData.EFFECT_NONE);
        }
    }
    else if(side == ToHitData.SIDE_LEFT) {
        // normal left-side hits
        switch( roll ) {
        case 2:
            setPotCrit(CRIT_WEAPON);
            return new HitData(LOC_NOSE, false, HitData.EFFECT_NONE);
        case 3:
            setPotCrit(CRIT_GEAR);
            return new HitData(LOC_LWING, false, HitData.EFFECT_NONE);
        case 4:
            setPotCrit(CRIT_SENSOR);
            return new HitData(LOC_NOSE, false, HitData.EFFECT_NONE);
        case 5:
            setPotCrit(CRIT_CREW);
            return new HitData(LOC_NOSE, false, HitData.EFFECT_NONE);
        case 6:
            setPotCrit(CRIT_WEAPON);
            return new HitData(LOC_LWING, false, HitData.EFFECT_NONE);
        case 7:
            setPotCrit(CRIT_AVIONICS);
            return new HitData(LOC_LWING, false, HitData.EFFECT_NONE);
        case 8:
            setPotCrit(CRIT_BOMB);
            return new HitData(LOC_LWING, false, HitData.EFFECT_NONE);
        case 9:
            setPotCrit(CRIT_CONTROL);
            return new HitData(LOC_AFT, false, HitData.EFFECT_NONE);
        case 10:
            setPotCrit(CRIT_ENGINE);
            return new HitData(LOC_AFT, false, HitData.EFFECT_NONE);
        case 11:
            setPotCrit(CRIT_GEAR);
            return new HitData(LOC_LWING, false, HitData.EFFECT_NONE);
        case 12:
            setPotCrit(CRIT_WEAPON);
            return new HitData(LOC_AFT, false, HitData.EFFECT_NONE);
        }
    }
    else if(side == ToHitData.SIDE_RIGHT) {
        // normal right-side hits
        switch( roll ) {
        case 2:
            setPotCrit(CRIT_WEAPON);
            return new HitData(LOC_NOSE, false, HitData.EFFECT_NONE);
        case 3:
            setPotCrit(CRIT_GEAR);
            return new HitData(LOC_RWING, false, HitData.EFFECT_NONE);
        case 4:
            setPotCrit(CRIT_SENSOR);
            return new HitData(LOC_NOSE, false, HitData.EFFECT_NONE);
        case 5:
            setPotCrit(CRIT_CREW);
            return new HitData(LOC_NOSE, false, HitData.EFFECT_NONE);
        case 6:
            setPotCrit(CRIT_WEAPON);
            return new HitData(LOC_RWING, false, HitData.EFFECT_NONE);
        case 7:
            setPotCrit(CRIT_AVIONICS);
            return new HitData(LOC_RWING, false, HitData.EFFECT_NONE);
        case 8:
            setPotCrit(CRIT_BOMB);
            return new HitData(LOC_RWING, false, HitData.EFFECT_NONE);
        case 9:
            setPotCrit(CRIT_CONTROL);
            return new HitData(LOC_AFT, false, HitData.EFFECT_NONE);
        case 10:
            setPotCrit(CRIT_ENGINE);
            return new HitData(LOC_AFT, false, HitData.EFFECT_NONE);
        case 11:
            setPotCrit(CRIT_GEAR);
            return new HitData(LOC_RWING, false, HitData.EFFECT_NONE);
        case 12:
            setPotCrit(CRIT_WEAPON);
            return new HitData(LOC_AFT, false, HitData.EFFECT_NONE);
        }
    }
    else if(side == ToHitData.SIDE_REAR) {
        // normal aft hits
        switch( roll ) {
        case 2:
            setPotCrit(CRIT_WEAPON);
            return new HitData(LOC_AFT, false, HitData.EFFECT_NONE);
        case 3:
            setPotCrit(CRIT_HEATSINK);
            return new HitData(LOC_AFT, false, HitData.EFFECT_NONE);
        case 4:
            setPotCrit(CRIT_FUEL_TANK);
            return new HitData(LOC_RWING, false, HitData.EFFECT_NONE);
        case 5:
            setPotCrit(CRIT_WEAPON);
            return new HitData(LOC_RWING, false, HitData.EFFECT_NONE);
        case 6:
            setPotCrit(CRIT_ENGINE);
            return new HitData(LOC_AFT, false, HitData.EFFECT_NONE);
        case 7:
            setPotCrit(CRIT_CONTROL);
            return new HitData(LOC_AFT, false, HitData.EFFECT_NONE);
        case 8:
            setPotCrit(CRIT_ENGINE);
            return new HitData(LOC_AFT, false, HitData.EFFECT_NONE);
        case 9:
            setPotCrit(CRIT_WEAPON);
            return new HitData(LOC_LWING, false, HitData.EFFECT_NONE);
        case 10:
            setPotCrit(CRIT_FUEL_TANK);
            return new HitData(LOC_LWING, false, HitData.EFFECT_NONE);
        case 11:
            setPotCrit(CRIT_HEATSINK);
            return new HitData(LOC_AFT, false, HitData.EFFECT_NONE);
        case 12:
            setPotCrit(CRIT_WEAPON);
            return new HitData(LOC_AFT, false, HitData.EFFECT_NONE);
        }
    }
        return new HitData(LOC_NOSE, false, HitData.EFFECT_NONE);
    }


    /**
     * Gets the location that excess damage transfers to
     */
    @Override
    public HitData getTransferLocation(HitData hit) {
        return new HitData(LOC_DESTROYED);
    }

    /**
     * Gets the location that is destroyed recursively
     */
    @Override
    public int getDependentLocation(int loc) {
        return LOC_NONE;
    }

    /*
     * (non-Javadoc)
     * @see megamek.common.Entity#calculateBattleValue()
     */
    @Override
    public int calculateBattleValue() {
        return calculateBattleValue(false, false);
    }

    /*
     * (non-Javadoc)
     * @see megamek.common.Entity#calculateBattleValue(boolean, boolean)
     */
    @Override
    public int calculateBattleValue(boolean ignoreC3, boolean ignorePilot) {
        double dbv = 0; // defensive battle value
        double obv = 0; // offensive bv

        int modularArmor = 0;
        for (Mounted mounted : getEquipment()) {
            if ((mounted.getType() instanceof MiscType) && mounted.getType().hasFlag(MiscType.F_MODULAR_ARMOR)) {
                modularArmor += mounted.getBaseDamageCapacity() - mounted.getDamageTaken();
            }
        }

        dbv += (getTotalArmor()+modularArmor) * 2.5;

        dbv += getSI() * 2.0;

        // add defensive equipment
        double dEquipmentBV = 0;
        for (Mounted mounted : getEquipment()) {
            EquipmentType etype = mounted.getType();

            // don't count destroyed equipment
            if (mounted.isDestroyed()) {
                continue;
            }

            if (((etype instanceof WeaponType) && (etype.hasFlag(WeaponType.F_AMS)))
                    || ((etype instanceof AmmoType) && (((AmmoType) etype).getAmmoType() == AmmoType.T_AMS))
                    || ((etype instanceof AmmoType) && (((AmmoType) etype).getAmmoType() == AmmoType.T_SCREEN_LAUNCHER))
                    || ((etype instanceof WeaponType) && (((WeaponType) etype).getAtClass() == WeaponType.CLASS_SCREEN))) {
                dEquipmentBV += etype.getBV(this);
            }
        }
        dbv += dEquipmentBV;

        // subtract for explosive ammo
        double ammoPenalty = 0;
        //need to keep track of any ammo type already used
        boolean[][] ammoTypesUsed = new boolean[AmmoType.NUM_TYPES][41];
        for (Mounted mounted : getEquipment()) {
            int loc = mounted.getLocation();
            int toSubtract = 15;
            EquipmentType etype = mounted.getType();

            // only count explosive ammo
            if (!etype.isExplosive()) {
                continue;
            }

            // CASE means no subtraction
            // clan aeros automatically have CASE
            if (hasCase() || isClan()) {
                continue;
            }

            // don't count oneshot ammo
            if (loc == LOC_NONE) {
                continue;
            }

            // gauss rifles only subtract 1 point per slot
            if (etype instanceof GaussWeapon) {
                toSubtract = 1;
            }


            //only ammo counts from here on out
            if(!(etype instanceof AmmoType)) {
                continue;
            }

            AmmoType aType = (AmmoType)etype;
            // empty ammo shouldn't count
            if (mounted.getShotsLeft() == 0) {
                continue;
            }
            // only subtract once for each weapon
            // we identify by matching via the ammoType var and the racksize
            if (ammoTypesUsed[aType.ammoType][aType.getRackSize()]) {
                continue;
            }

            ammoPenalty += toSubtract;

            ammoTypesUsed[aType.ammoType][aType.getRackSize()] = true;
        }
        dbv = Math.max(1, dbv - ammoPenalty);

        //unit type multiplier
        dbv *= getBVTypeModifier();

        // calculate heat efficiency
        int aeroHeatEfficiency = 6 + getHeatCapacity();

        // total up maximum heat generated
        // and add up BVs for ammo-using weapon types for excessive ammo rule
        Map<String, Double> weaponsForExcessiveAmmo = new HashMap<String, Double>();
        double maximumHeat = 0;
        for (Mounted mounted : getTotalWeaponList()) {
            WeaponType wtype = (WeaponType) mounted.getType();
            double weaponHeat = wtype.getHeat();

            // only count non-damaged equipment
            if (mounted.isMissing() || mounted.isHit() || mounted.isDestroyed() || mounted.isBreached()) {
                continue;
            }

            //do not count weapon groups
            if(mounted.isWeaponGroup()) {
                continue;
            }

            // one shot weapons count 1/4
            if ((wtype.getAmmoType() == AmmoType.T_ROCKET_LAUNCHER) || wtype.hasFlag(WeaponType.F_ONESHOT)) {
                weaponHeat *= 0.25;
            }

            // double heat for ultras
            if ((wtype.getAmmoType() == AmmoType.T_AC_ULTRA) || (wtype.getAmmoType() == AmmoType.T_AC_ULTRA_THB)) {
                weaponHeat *= 2;
            }

            // Six times heat for RAC
            if (wtype.getAmmoType() == AmmoType.T_AC_ROTARY) {
                weaponHeat *= 6;
            }

            // half heat for streaks
            if ((wtype.getAmmoType() == AmmoType.T_SRM_STREAK) || (wtype.getAmmoType() == AmmoType.T_MRM_STREAK) || (wtype.getAmmoType() == AmmoType.T_LRM_STREAK)) {
                weaponHeat *= 0.5;
            }
            maximumHeat += weaponHeat;
            // add up BV of ammo-using weapons for each type of weapon,
            // to compare with ammo BV later for excessive ammo BV rule
            if (!((wtype.hasFlag(WeaponType.F_ENERGY) && !(wtype.getAmmoType() == AmmoType.T_PLASMA)) || wtype.hasFlag(WeaponType.F_ONESHOT) || wtype.hasFlag(WeaponType.F_INFANTRY) || (wtype.getAmmoType() == AmmoType.T_NA))) {
                String key = wtype.getAmmoType() + ":" + wtype.getRackSize();
                if (!weaponsForExcessiveAmmo.containsKey(key)) {
                    weaponsForExcessiveAmmo.put(key, wtype.getBV(this));
                } else {
                    weaponsForExcessiveAmmo.put(key, wtype.getBV(this) + weaponsForExcessiveAmmo.get(key));
                }
            }
        }

        double weaponBV = 0;
        boolean hasTargComp = hasTargComp();
        // first, add up front-faced and rear-faced unmodified BV,
        // to know wether front- or rear faced BV should be halved
        double bvFront = 0, bvRear = 0;
        ArrayList<Mounted> weapons = getTotalWeaponList();
        for (Mounted weapon : weapons) {
            WeaponType wtype = (WeaponType) weapon.getType();
            double dBV = wtype.getBV(this);
            // don't count destroyed equipment
            if (weapon.isDestroyed()) {
                continue;
            }
            // don't count AMS, it's defensive
            if (wtype.hasFlag(WeaponType.F_AMS)) {
                continue;
            }
            // don't count screen launchers, they are defensive
            if(wtype.getAtClass() == WeaponType.CLASS_SCREEN) {
                continue;
            }
            //do not count weapon groups
            if(weapon.isWeaponGroup()) {
                continue;
            }
            // calc MG Array here:
            if (wtype.hasFlag(WeaponType.F_MGA)) {
                double mgaBV = 0;
                for (Mounted possibleMG : getTotalWeaponList()) {
                    if (possibleMG.getType().hasFlag(WeaponType.F_MG)
                            && (possibleMG.getLocation() == weapon
                                    .getLocation())) {
                        mgaBV += possibleMG.getType().getBV(this);
                    }
                }
                dBV = mgaBV * 0.67;
            }
            if (weapon.isRearMounted() || (weapon.getLocation() == LOC_AFT)) {
                bvRear += dBV;
            } else {
                bvFront += dBV;
            }
        }
        boolean halveRear = true;
        if (bvFront <= bvRear) {
            halveRear = false;
        }

        if (maximumHeat <= aeroHeatEfficiency) {
            // count all weapons equal, adjusting for rear-firing and excessive
            // ammo
            for (Mounted weapon : getTotalWeaponList()) {
                WeaponType wtype = (WeaponType) weapon.getType();
                double dBV = wtype.getBV(this);

                // don't count destroyed equipment
                if (weapon.isDestroyed()) {
                    continue;
                }

                // don't count AMS, it's defensive
                if (wtype.hasFlag(WeaponType.F_AMS)) {
                    continue;
                }
                //don't count screen launchers, they are defensive
                if(wtype.getAtClass() == WeaponType.CLASS_SCREEN) {
                    continue;
                }
                //do not count weapon groups
                if(weapon.isWeaponGroup()) {
                    continue;
                }
                // calc MG Array here:
                if (wtype.hasFlag(WeaponType.F_MGA)) {
                    double mgaBV = 0;
                    for (Mounted possibleMG : getTotalWeaponList()) {
                        if (possibleMG.getType().hasFlag(WeaponType.F_MG)
                                && (possibleMG.getLocation() == weapon
                                        .getLocation())) {
                            mgaBV += possibleMG.getType().getBV(this);
                        }
                    }
                    dBV = mgaBV * 0.67;
                }

                // and we'll add the tcomp here too
                if (wtype.hasFlag(WeaponType.F_DIRECT_FIRE)) {
                    if (hasTargComp) {
                        dBV *= 1.25;
                    }
                }
                // artemis bumps up the value
                if (weapon.getLinkedBy() != null) {
                    Mounted mLinker = weapon.getLinkedBy();
                    if ((mLinker.getType() instanceof MiscType) && mLinker.getType().hasFlag(MiscType.F_ARTEMIS)) {
                        dBV *= 1.2;
                    }
                    if ((mLinker.getType() instanceof MiscType) && mLinker.getType().hasFlag(MiscType.F_APOLLO)) {
                        dBV *= 1.15;
                    }
                }
                // half for being rear mounted (or front mounted, when more rear-
                // than front-mounted un-modded BV
                if (((weapon.isRearMounted() || (weapon.getLocation() == LOC_AFT))&& halveRear) || (!(weapon.isRearMounted() || (weapon.getLocation() == LOC_AFT)) && !halveRear)) {
                    dBV /= 2;
                }
                weaponBV += dBV;
            }
        } else {
            // this will count heat-generating weapons at full modified BV until
            // heatefficiency is reached or passed with one weapon

            // here we store the modified BV and heat of all heat-using weapons,
            // to later be sorted by BV
            ArrayList<double[]> heatBVs = new ArrayList<double[]>();
            // BVs of non-heat-using weapons
            ArrayList<Double> nonHeatBVs = new ArrayList<Double>();
            // loop through weapons, calc their modified BV
            for (Mounted weapon : weapons) {
                WeaponType wtype = (WeaponType) weapon.getType();
                double dBV = wtype.getBV(this);
                // don't count destroyed equipment
                if (weapon.isDestroyed()) {
                    continue;
                }
                // don't count AMS, it's defensive
                if (wtype.hasFlag(WeaponType.F_AMS)) {
                    continue;
                }
                //don't count screen launchers, they are defensive
                if(wtype.getAtClass() == WeaponType.CLASS_SCREEN) {
                    continue;
                }
                //do not count weapon groups
                if(weapon.isWeaponGroup()) {
                    continue;
                }
                // calc MG Array here:
                if (wtype.hasFlag(WeaponType.F_MGA)) {
                    double mgaBV = 0;
                    for (Mounted possibleMG : getTotalWeaponList()) {
                        if (possibleMG.getType().hasFlag(WeaponType.F_MG) && (possibleMG.getLocation() == weapon.getLocation())) {
                            mgaBV += possibleMG.getType().getBV(this);
                        }
                    }
                    dBV = mgaBV * 0.67;
                }
                // and we'll add the tcomp here too
                if (wtype.hasFlag(WeaponType.F_DIRECT_FIRE) && hasTargComp) {
                    dBV *= 1.25;
                }
                // artemis bumps up the value
                if (weapon.getLinkedBy() != null) {
                    Mounted mLinker = weapon.getLinkedBy();
                    if ((mLinker.getType() instanceof MiscType) && mLinker.getType().hasFlag(MiscType.F_ARTEMIS)) {
                        dBV *= 1.2;
                    }
                    if ((mLinker.getType() instanceof MiscType) && mLinker.getType().hasFlag(MiscType.F_APOLLO)) {
                        dBV *= 1.15;
                    }
                }
                // half for being rear mounted (or front mounted, when more rear-
                // than front-mounted un-modded BV
                if (((weapon.isRearMounted() || (weapon.getLocation() == LOC_AFT))&& halveRear) || (!(weapon.isRearMounted() || (weapon.getLocation() == LOC_AFT)) && !halveRear)) {
                    dBV /= 2;
                }
                int heat = ((WeaponType)weapon.getType()).getHeat();
                double[] weaponValues = new double[2];
                weaponValues[0] = dBV;
                weaponValues[1] = heat;
                if (heat > 0) {
                    // store heat and BV, for sorting a few lines down
                    weaponValues[0] = dBV;
                    weaponValues[1] = heat;
                    heatBVs.add(weaponValues);
                }
                else {
                    nonHeatBVs.add(dBV);
                }
            }
            // sort the heat-using weapons by modified BV
            Collections.sort(heatBVs, new Comparator<double[]>() {
                public int compare(double[] obj1, double[] obj2) {
                    // if same BV, lower heat first
                    if (obj1[0] == obj2[0]) {
                        return (int)Math.ceil(obj1[1] - obj2[1]);
                    }
                    // higher BV first
                    return (int)Math.ceil(obj2[0] - obj1[0]);
                }
            });
            // count heat-free weapons at full modified BV
            for (double bv : nonHeatBVs) {
                weaponBV += bv;
            }
            // count heat-generating weapons at full modified BV until heatefficiency is reached or
            // passed with one weapon
            double heatAdded = 0;
            for (double[] weaponValues : heatBVs) {
                double dBV = weaponValues[0];
                if (heatAdded >= aeroHeatEfficiency) {
                    dBV /= 2;
                }
                heatAdded += weaponValues[1];
                weaponBV += dBV;
            }
        }

        // add offensive misc. equipment BV
        double oEquipmentBV = 0;
        for (Mounted mounted : getMisc()) {
            MiscType mtype = (MiscType) mounted.getType();

            // don't count destroyed equipment
            if (mounted.isDestroyed()) {
                continue;
            }

            if (mtype.hasFlag(MiscType.F_TARGCOMP)) {
                continue;
            }
            double bv = mtype.getBV(this);
            oEquipmentBV += bv;
        }
        weaponBV += oEquipmentBV;

        // add ammo bv
        double ammoBV = 0;
        // extra BV for when we have semiguided LRMs and someone else has TAG on
        // our team
        double tagBV = 0;
        Map<String, Double> ammo = new HashMap<String, Double>();
        ArrayList<String> keys = new ArrayList<String>();
        for (Mounted mounted : getAmmo()) {
            AmmoType atype = (AmmoType) mounted.getType();

            // don't count depleted ammo
            if (mounted.getShotsLeft() == 0) {
                continue;
            }

            // don't count AMS, it's defensive
            if (atype.getAmmoType() == AmmoType.T_AMS) {
                continue;
            }
            //don't count screen launchers, they are defensive
            if(atype.getAmmoType() == AmmoType.T_SCREEN_LAUNCHER) {
                continue;
            }

            // don't count oneshot ammo, it's considered part of the launcher.
            if (mounted.getLocation() == Entity.LOC_NONE) {
                // assumption: ammo without a location is for a oneshot weapon
                continue;
            }
            // semiguided or homing ammo might count double
            if ((atype.getMunitionType() == AmmoType.M_SEMIGUIDED)
                    || (atype.getMunitionType() == AmmoType.M_HOMING)) {
                Player tmpP = getOwner();

                if (tmpP != null) {
                    // Okay, actually check for friendly TAG.
                    if (tmpP.hasTAG()) {
                        tagBV += atype.getBV(this);
                    } else if ((tmpP.getTeam() != Player.TEAM_NONE) && (game != null)) {
                        for (Enumeration<Team> e = game.getTeams(); e.hasMoreElements();) {
                            Team m = e.nextElement();
                            if (m.getId() == tmpP.getTeam()) {
                                if (m.hasTAG(game)) {
                                    tagBV += atype.getBV(this);
                                }
                                // A player can't be on two teams.
                                // If we check his team and don't give the
                                // penalty, that's it.
                                break;
                            }
                        }
                    }
                }
            }
            String key = atype.getAmmoType() + ":" + atype.getRackSize();
            if (!keys.contains(key)) {
                keys.add(key);
            }
            if (!ammo.containsKey(key)) {
                ammo.put(key, atype.getBV(this));
            } else {
                ammo.put(key, atype.getBV(this) + ammo.get(key));
            }
        }

        // Excessive ammo rule:
        // Only count BV for ammo for a weapontype until the BV of all weapons
        // of that
        // type on the mech is reached.
        for (String key : keys) {
            if (weaponsForExcessiveAmmo.get(key) != null) {
                if (ammo.get(key) > weaponsForExcessiveAmmo.get(key)) {
                    ammoBV += weaponsForExcessiveAmmo.get(key);
                } else {
                    ammoBV += ammo.get(key);
                }
            }
        }
        weaponBV += ammoBV;

        // adjust further for speed factor
        double speedFactor = Math.pow(1 + (((double) getRunMP()  - 5) / 10), 1.2);
        speedFactor = Math.round(speedFactor * 100) / 100.0;

        obv = weaponBV * speedFactor;

        double finalBV = dbv + obv;
        if ( getCockpitType() == Aero.COCKPIT_SMALL ) {
            finalBV *= 0.95;
        }
        finalBV = Math.round(finalBV);

        // we get extra bv from some stuff
        double xbv = 0.0;
        // extra BV for semi-guided lrm when TAG in our team
        xbv += tagBV;
        // extra from c3 networks. a valid network requires at least 2 members
        // some hackery and magic numbers here. could be better
        // also, each 'has' loops through all equipment. inefficient to do it 3
        // times
        if (((hasC3MM() && (calculateFreeC3MNodes() < 2)) || (hasC3M() && (calculateFreeC3Nodes() < 3)) || (hasC3S() && (c3Master > NONE)) || (hasC3i() && (calculateFreeC3Nodes() < 5))) && !ignoreC3 && (game != null)) {
            int totalForceBV = 0;
            totalForceBV += this.calculateBattleValue(true, true);
            for (Entity e : game.getC3NetworkMembers(this)) {
                if (!equals(e) && onSameC3NetworkAs(e)) {
                    totalForceBV += e.calculateBattleValue(true, true);
                }
            }
            xbv += totalForceBV *= 0.05;
        }
        finalBV += xbv;


        // and then factor in pilot
        double pilotFactor = 1;
        if (!ignorePilot) {
            pilotFactor = crew.getBVSkillMultiplier();
        }

        int retVal = (int) Math.round((finalBV) * pilotFactor);

        // don't factor pilot in if we are just calculating BV for C3 extra BV
        if (ignoreC3) {
            return (int)finalBV;
        }
        return retVal;
    }

    public double getBVTypeModifier() {
        return 1.2;
    }

    @Override
    public PilotingRollData addEntityBonuses(PilotingRollData prd)
    {
        //this is a control roll. Affected by:
        //avionics damage
        //pilot damage
        //current velocity
        int avihits = getAvionicsHits();
        int pilothits = getCrew().getHits();

        if((avihits > 0) && (avihits<3)) {
            prd.addModifier(avihits, "Avionics Damage");
        }

        //this should probably be replaced with some kind of AVI_DESTROYED boolean
        if(avihits >= 3) {
            prd.addModifier(5, "Avionics Destroyed");
        }

        if(pilothits>0) {
            prd.addModifier(pilothits, "Pilot Hits");
        }

        //movement effects
        //some question as to whether "above safe thrust" applies to thrust or velocity
        //I will treat it as thrust until it is resolved
        if(moved == IEntityMovementType.MOVE_OVER_THRUST) {
            prd.addModifier(+1, "Used more than safe thrust");
        }
        int vel = getCurrentVelocity();
        int vmod = vel - (2*getWalkMP());
        if(vmod > 0) {
            prd.addModifier(vmod, "Velocity greater than 2x safe thrust");
        }

        //add in atmospheric effects later
        if(game.getBoard().inAtmosphere()) {
            prd.addModifier(+2, "Atmospheric operations");

            //check type
            if(this instanceof Dropship) {
                if(isSpheroid()) {
                    prd.addModifier(-1,"spheroid dropship");
                } else {
                    prd.addModifier(0,"aerodyne dropship");
                }
            } else {
                prd.addModifier(-1,"fighter/small craft");
            }
        }

        //life support (only applicable to non-ASFs
        if(!hasLifeSupport()) {
            prd.addModifier(+2,"No life support");
        }

        if ( hasModularArmor() ) {
            prd.addModifier(1,"Modular Armor");
        }
        //VDNI bonus?
        if (getCrew().getOptions().booleanOption("vdni") && !getCrew().getOptions().booleanOption("bvdni")) {
            prd.addModifier(-1, "VDNI");
        }

        // Small/torso-mounted cockpit penalty?
        if ((getCockpitType() == Aero.COCKPIT_SMALL) && !getCrew().getOptions().booleanOption("bvdni")) {
            prd.addModifier(1, "Small Cockpit");
        }

        return prd;
    }

    @Override
    public Vector<Report> victoryReport() {
        Vector<Report> vDesc = new Vector<Report>();

        Report r = new Report(7025);
        r.type = Report.PUBLIC;
        r.addDesc(this);
        vDesc.addElement(r);

        r = new Report(7035);
        r.type = Report.PUBLIC;
        r.newlines = 0;
        vDesc.addElement(r);
        vDesc.addAll(crew.getDescVector(false));
        r = new Report(7070, Report.PUBLIC);
        r.add(getKillNumber());
        vDesc.addElement(r);

        if(isDestroyed()) {
            Entity killer = game.getEntity(killerId);
            if(killer == null) {
                killer = game.getOutOfGameEntity(killerId);
            }
            if(killer != null) {
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

    @Override
    public int[] getNoOfSlots()
    {
        return NUM_OF_SLOTS;
    }

    /**
     * Tanks don't have MASC
     */
    @Override
    public int getRunMPwithoutMASC(boolean gravity, boolean ignoreheat) {
        return getRunMP(gravity, ignoreheat);
    }

    @Override
    public int getHeatCapacity() {
        return (getHeatSinks()*(getHeatType()+1));
    }

    //If the aero is in the water, it is dead so no worries
    @Override
    public int getHeatCapacityWithWater() {
        return getHeatCapacity();
    }

    @Override
    public int getEngineCritHeat() {
        return 0;
    }

    @Override
    public void autoSetInternal()
    {
        //should be no internals because only one SI
        //It doesn't seem to be screwing anything up yet.
        //Need to figure out how destruction of entity is determined
        int nInternal = (int)Math.ceil(weight / 10.0);
        nInternal = 0;
        //I need to look at safe thrust as well at some point

        for (int x = 0; x < locations(); x++) {
            initializeInternal(nInternal, x);
        }
    }

    //initialize the Damage threshold
    public void autoSetThresh()
    {
        for(int x = 0; x < locations(); x++)
        {
            initializeThresh(x);
        }
    }

    public void setThresh(int val, int loc) {
        damThresh[loc] = val;
    }

    public void initializeThresh(int loc)
    {
        int nThresh = (int)Math.ceil(getArmor(loc) / 10.0);
        setThresh(nThresh,loc);
    }

    public int getThresh(int loc) {
        return damThresh[loc];
    }

    /**
     * Determine if the unit can be repaired, or only harvested for spares.
     *
     * @return  A <code>boolean</code> that is <code>true</code> if the unit
     *          can be repaired (given enough time and parts); if this value
     *          is <code>false</code>, the unit is only a source of spares.
     * @see     Entity#isSalvage()
     */
    @Override
    public boolean isRepairable() {
    return true; //deal with this later
    }

    /**
     * Restores the entity after serialization
     */
    @Override
    public void restore() {
        super.restore();
    //not sure what to put here

    }


    @Override
    public boolean canCharge() {
        //ramming is resolved differently than chargin
        return false;
    }

    @Override
    public boolean canDFA() {
        // Aero can't DFA
        return false;
    }

    @Override
    public boolean canRam() {
        return !isImmobile() && (getWalkMP() > 0);
    }

    @Override
    public int getArmorType()
    {
        return armorType;
    }

    @Override
    public void setArmorType(int type)
    {
        armorType = type;
    }

    @Override
    public int getStructureType()
    {
        return structureType;
    }

    @Override
    public void setStructureType(int type)
    {
        structureType = type;
    }

    /**
     * @return suspension factor of vehicle
     */
    //Doesn't really do anything so just return 0
    public int getSuspensionFactor () {
        return 0;
    }

    /*There is a mistake in some of the AT2r costs
     * for some reason they added ammo twice for a lot of the
     * level 2 designs, leading to costs that are too high
     */
    @Override
    public double getCost() {

        double cost = 0;

        //add in cockpit
        cost += 200000 + 50000 + 2000 * weight;

        //Structural integrity
        cost += 50000 * getSI();

        //additional flight systems (attitude thruster and landing gear)
        cost += 25000 + 10 * getWeight();

        //engine
        Engine engine = getEngine();
        cost += engine.getBaseCost() * engine.getRating() * weight / 75.0;

        //fuel tanks
        cost += 200 * getFuel() / 80.0;

        //armor
        cost += getArmorWeight()*EquipmentType.getArmorCost(armorType);

        //heat sinks
        int sinkCost = 2000 + 4000 * getHeatType();// == HEAT_DOUBLE ? 6000: 2000;
        cost += sinkCost*getHeatSinks();

        //weapons
        cost += getWeaponsAndEquipmentCost();

        //omni multiplier
        double omniMultiplier = 1;
        if (isOmni()) {
            omniMultiplier = 1.25f;
        }

        double weightMultiplier = 1 + (weight / 200f);

        return Math.round(cost * omniMultiplier * weightMultiplier);

    }

    @Override
    public boolean doomedInVacuum() {
        return false;
    }

    @Override
    public boolean doomedOnGround() {
        return true;
    }

    @Override
    public boolean doomedInAtmosphere() {
        return false;
    }

    @Override
    public boolean doomedInSpace() {
        return false;
    }

    @Override
    public boolean canGoHullDown () {
        return false;
    }

  /*
    public void addMovementDamage(int level) {
        movementDamage += level;
    }
*/

    public void setEngine(Engine e) {
        engine = e;
        if (e.engineValid) {
            setOriginalWalkMP(calculateWalk());
        }
    }


    /**
     * Returns the percent of the SI remaining
     */
    @Override
    public double getInternalRemainingPercent() {
        return ((double)getSI() / (double)get0SI());
    }

    protected int calculateWalk() {
        return (getEngine().getRating()  / (int)weight) + 2;
    }

    @Override
    public boolean isNuclearHardened() {
        return true;
    }

    @Override
    protected void addEquipment(Mounted mounted, int loc, boolean rearMounted)
    throws LocationFullException {
        super.addEquipment(mounted,loc, rearMounted);
        // Add the piece equipment to our slots.
        addCritical(loc, new CriticalSlot(CriticalSlot.TYPE_EQUIPMENT,
                                           getEquipmentNum(mounted),
                                           true, mounted));
    }

    /** get the type of critical caused by a critical roll,
     * taking account of existing damage
     * @param roll the final dice roll
     * @param loc  the hit location
     * @return     a critical type
     */
    public int getCriticalEffect(int roll, int target) {
        //just grab the latest potential crit
        if(roll < target) {
            return CRIT_NONE;
        }

        int critical = getPotCrit();
        return critical;
    }

    public PilotingRollData checkThrustSI(int thrust, int overallMoveType) {
        PilotingRollData roll = getBasePilotingRoll(overallMoveType);

        if(thrust > getSI()) {
            // append the reason modifier
            roll.append(new PilotingRollData(getId(), thrust-getSI(), "Thrust exceeds current SI in a single hex"));
        } else {
            roll.addModifier(TargetRoll.CHECK_FALSE,"Check false: Entity is not exceeding SI");
        }
        return roll;
    }

    public PilotingRollData checkThrustSITotal(int thrust, int overallMoveType) {
        PilotingRollData roll = getBasePilotingRoll(overallMoveType);

        if(thrust > getSI()) {
            // append the reason modifier
            roll.append(new PilotingRollData(getId(), 0, "Thrust spent this turn exceeds current SI"));
        } else {
            roll.addModifier(TargetRoll.CHECK_FALSE,"Check false: Entity is not exceeding SI");
        }
        return roll;
    }

    public PilotingRollData checkVelocityDouble(int velocity, int overallMoveType) {
        PilotingRollData roll = getBasePilotingRoll(overallMoveType);

        if((velocity > (2 * getWalkMP())) && game.getBoard().inAtmosphere()) {
            // append the reason modifier
            roll.append(new PilotingRollData(getId(), 0, "Velocity greater than 2x safe thrust"));
        } else {
            roll.addModifier(TargetRoll.CHECK_FALSE,"Check false: Entity is not exceeding 2x safe thrust");
        }
        return roll;
    }

    public PilotingRollData checkDown(int drop, int overallMoveType) {
        PilotingRollData roll = getBasePilotingRoll(overallMoveType);

        if( drop > 2 ) {
            // append the reason modifier
            roll.append(new PilotingRollData(getId(), drop, "lost more than two altitudes"));
        } else {
            roll.addModifier(TargetRoll.CHECK_FALSE,"Check false: entity did not drop more than two altitudes");
        }
        return roll;
    }

    public PilotingRollData checkHover(MovePath md) {
        PilotingRollData roll = getBasePilotingRoll(md.getLastStepMovementType());

        if( md.contains(MovePath.STEP_HOVER) ) {
            // append the reason modifier
            roll.append(new PilotingRollData(getId(), 0, "hovering"));
        } else {
            roll.addModifier(TargetRoll.CHECK_FALSE,"Check false: entity did not hover");
        }
        return roll;
    }

    public PilotingRollData checkStall(int velocity, int overallMoveType) {
        PilotingRollData roll = getBasePilotingRoll(overallMoveType);

        if( velocity == 0 ) {
            // append the reason modifier
            roll.append(new PilotingRollData(getId(), 0, "stalled out"));
        } else {
            roll.addModifier(TargetRoll.CHECK_FALSE,"Check false: entity not stalled out");
        }
        return roll;
    }

    public PilotingRollData checkRolls(MoveStep step, int overallMoveType) {
        PilotingRollData roll = getBasePilotingRoll(overallMoveType);

        if(((step.getType() == MovePath.STEP_ROLL) || (step.getType() == MovePath.STEP_YAW))
                && (step.getNRolls() > 1)) {
            // append the reason modifier
            roll.append(new PilotingRollData(getId(), 0, "More than one roll in the same turn"));
        } else {
            roll.addModifier(TargetRoll.CHECK_FALSE,"Check false: Entity is not rolling more than once");
        }
        return roll;
    }

    /**
     * Checks if a maneuver requires a control roll
     */
    public PilotingRollData checkManeuver(MoveStep step, int overallMoveType) {
        PilotingRollData roll = getBasePilotingRoll(overallMoveType);

        if ((step == null) || (step.getType() != MovePath.STEP_MANEUVER)) {
            roll.addModifier(TargetRoll.CHECK_FALSE,
                    "Check false: Entity is not attempting to get up.");
            return roll;
        }

        roll.append(new PilotingRollData(getId(),
                    ManeuverType.getMod(step.getManeuverType(), isVSTOL()),
                    ManeuverType.getTypeName(step.getManeuverType()) + " maneuver"));

        return roll;

    }


    /**

     */
    @Override
    public void setOmni( boolean omni ) {

        // Perform the superclass' action.
        super.setOmni( omni );

    }

    /**
     * Adds clan CASE in every location
     */
    public void addClanCase() {
        boolean explosiveFound=false;
        EquipmentType clCase = EquipmentType.get("CLCASE");
        for (int i = 0; i < locations(); i++) {
            explosiveFound=false;
            for (Mounted m : getEquipment()){
                if(m.getType().isExplosive() && (m.getLocation()==i)) {
                    explosiveFound=true;
                }
            }
            if(explosiveFound) {
                try {
                    addEquipment(new Mounted(this, clCase), i, false);
                    } catch (LocationFullException ex) {
                        // um, that's impossible.
                        }
                    }
         }

    }

    /**
     * check to see if case is available anywhere
     * @return
     */
    public boolean hasCase() {

        boolean hasCase = false;

        for (int x = 0; x < locations(); x++) {
            if(!hasCase) {
                hasCase = locationHasCase(x);
            }
        }
        return hasCase;
    }

    /**
     * Used to determine net velocity of ramming attack
     *
     */
    public int sideTableRam(Coords src) {

        int side = sideTableRam(src, facing);
        //if using advanced movement, then I need heading, but
        //in cases of ties, I use the least damaging option
        if(game.useVectorMove()) {
            int newside = chooseSideRam(src);
            if(newside != -1) {
                side = newside;
            }
        }

        return side;

    }

    public int sideTableRam(Coords src, int face) {

        int fa = (getPosition().degree(src) + (6 - face) * 60) % 360;
        if (((fa > 30) && (fa <= 90)) || ((fa < 330) && (fa >= 270))) {
            return Aero.RAM_TOWARD_OBL;
        } else if ((fa > 150) && (fa < 210)) {
            return Aero.RAM_AWAY_DIR;
        } else if (((fa > 90) && (fa <= 150)) || ((fa < 270) && (fa >= 210))) {
            return Aero.RAM_AWAY_OBL;
        } else {
            return Aero.RAM_TOWARD_DIR;
        }
    }

    public int chooseSideRam(Coords src) {
        //loop through directions and if we have a non-zero vector, then compute
        //the targetsidetable. If we come to a higher vector, then replace.  If
        //we come to an equal vector then take it if it is better
        int thrust = 0;
        int high = -1;
        int side = -1;
        for(int dir = 0; dir < 6; dir++) {
            thrust = getVector(dir);
            if(thrust == 0) {
                continue;
            }

            if(thrust > high) {
                high = thrust;
                side = sideTableRam(src, dir);
            }

            //what if they tie
            if(thrust == high) {
                int newside = sideTableRam(src, dir);
                //choose the better
                if(newside > side) {
                    newside = side;
                }
                //that should be the only case, because it can't shift you from front
                //to aft or vice-versa
            }

        }
        return side;
    }

    public int getStandardDamage(int loc) {
        return standard_damage[loc];
    }

    public void resetStandardDamage() {
        for(int i = 0; i < locations(); i++) {
            standard_damage[i] = 0;
        }
    }

    public void addStandardDamage(int damage, HitData hit) {
        standard_damage[hit.getLocation()] = standard_damage[hit.getLocation()] + damage;
    }

    public int getMaxEngineHits() {
        return 3;
    }

    @Override
    public int getMaxElevationChange() {
        return 999;
    }

    @Override
    public boolean isHexProhibited(IHex hex) {
        if(hex.containsTerrain(Terrains.IMPASSABLE)) {
            return true;
        }
        return false;
    }

    public boolean isSpheroid() {
        return spheroid;
    }

    public void setSpheroid(boolean b) {
        spheroid = b;
    }

    @Override
    public int height() {
        return 0;
    }

    /**
     * Returns true if the entity has an RAC which is jammed and not destroyed
     */
    //different from base bacause I have to look beyond weapon bays
    @Override
    public boolean canUnjamRAC() {
        for (Mounted mounted : getTotalWeaponList()) {
            WeaponType wtype = (WeaponType)mounted.getType();
            if ((wtype.getAmmoType() == AmmoType.T_AC_ROTARY) && mounted.isJammed() && !mounted.isDestroyed()) {
                return true;
            }
        }
        return false;
    }

    //I need a function that takes the bombChoices variable and uses it to produce bombs
    public void applyBombs() {
        int loc = LOC_NOSE;
        for(int type = 0; type < BombType.B_NUM; type++) {
            for(int i = 0; i<bombChoices[type]; i++) {
                if((type == BombType.B_ALAMO) && !game.getOptions().booleanOption("at2_nukes")) {
                    continue;
                }
                if((type > BombType.B_TAG) && !game.getOptions().booleanOption("allow_advanced_ammo")) {
                    continue;
                }

                //some bombs need an associated weapon and if so
                //they need a weapon for each bomb
                if((null != BombType.getBombWeaponName(type)) && (type != BombType.B_ARROW) && (type != BombType.B_HOMING)) {
                    try{
                        addBomb(EquipmentType.get(BombType.getBombWeaponName(type)),loc);
                    } catch (LocationFullException ex) {
                        //throw new LocationFullException(ex.getMessage());
                    }
                }
                if(type != BombType.B_TAG) {
                    try{
                        addEquipment(EquipmentType.get(BombType.getBombInternalName(type)),loc,false);
                    } catch (LocationFullException ex) {
                        //throw new LocationFullException(ex.getMessage());
                    }
                }
            }
        }
        //add the space bomb attack
        //TODO: I don't know where else to put this (where do infantry attacks get added)
        if(game.getOptions().booleanOption("stratops_space_bomb") && (getSpaceBombs().size() > 0)) {
            try{
                addEquipment(EquipmentType.get(SPACE_BOMB_ATTACK),LOC_NOSE,false);
            } catch (LocationFullException ex) {
                //throw new LocationFullException(ex.getMessage());
            }
        }
        updateWeaponGroups();
        loadAllWeapons();
    }

    public Vector<Mounted> getSpaceBombs() {
        Vector<Mounted> bombs = new Vector<Mounted>();
        for(Mounted bomb : getBombs()) {
            BombType btype = (BombType)bomb.getType();
            if(!bomb.isInoperable() && (bomb.getShotsLeft() > 0)
                    && ((btype.getBombType() == BombType.B_HE) || (btype.getBombType() == BombType.B_CLUSTER)
                            || (btype.getBombType() == BombType.B_LG) || (btype.getBombType() == BombType.B_ARROW))) {
                bombs.add(bomb);
            }
        }
        return bombs;
    }

    @Override
    public int getExtremeRangeModifier() {
        return 6;
    }

    public int getStraightMoves() {
        return straightMoves;
    }

    public void setStraightMoves(int i) {
        straightMoves = i;
    }

    public boolean isVSTOL() {
        return vstol;
    }

    public void setVSTOL(boolean b) {
        vstol = b;
    }

    public int getFuelUsed(int thrust) {
        int overThrust =  Math.max(thrust - getWalkMP(), 0);
        int safeThrust = thrust - overThrust;
        int used = safeThrust + 2 * overThrust;
        return used;
    }

    public boolean didFailManeuver() {
        return failedManeuver;
    }

    public void setFailedManeuver(boolean b) {
        failedManeuver = b;
    }

    public void setAccDecNow(boolean b) {
        accDecNow = b;
    }

    public boolean didAccDecNow() {
        return accDecNow;
    }

    @Override
    public void setGameOptions() {
        super.setGameOptions();

        for (Mounted mounted : getWeaponList()) {
            if ((mounted.getType() instanceof EnergyWeapon)
                    && (((WeaponType) mounted.getType()).getAmmoType() == AmmoType.T_NA)
                    && (game != null) && game.getOptions().booleanOption("tacops_energy_weapons")) {

                ArrayList<String> modes = new ArrayList<String>();
                String[] stringArray = {};
                int damage = ((WeaponType) mounted.getType()).getDamage();

                if ( damage == WeaponType.DAMAGE_VARIABLE ) {
                    damage = ((WeaponType) mounted.getType()).damageShort;
                }

                for (; damage >= 0; damage--) {
                    modes.add("Damage " + damage);
                }
                if ( ((WeaponType)mounted.getType()).hasFlag(WeaponType.F_FLAMER) ){
                    modes.add("Heat");
                }
                ((WeaponType) mounted.getType()).setModes(modes.toArray(stringArray));
            }

        }

    }

    @Override
    public boolean hasModularArmor() {

        for (Mounted mount : this.getEquipment()) {
            if (!mount.isDestroyed()
                    && (mount.getType() instanceof MiscType)
                    && ((MiscType) mount.getType()).hasFlag(MiscType.F_MODULAR_ARMOR)) {
                return true;
            }
        }

        return false;

    }

    @Override
    public boolean hasModularArmor(int loc) {

        for (Mounted mount : this.getEquipment()) {
            if ((mount.getLocation() == loc)
                    && (mount.getType() instanceof MiscType)
                    && ((MiscType) mount.getType()).hasFlag(MiscType.F_MODULAR_ARMOR)) {
                return true;
            }
        }

        return false;
    }

    /*
     * (non-Javadoc)
     * @see megamek.common.Entity#getTotalCommGearTons()
     */
    @Override
    public int getTotalCommGearTons() {
        return 1 + getExtraCommGearTons();
    }

    /**
     * The number of critical slots that are destroyed in the component.
     */
    @Override
    public int getBadCriticals(int type, int index, int loc) {
        return 0;
    }

    public int getCockpitType() {
        return cockpitType;
    }

    public void setCockpitType(int type) {
        cockpitType = type;
    }

    public String getCockpitTypeString() {
        return Aero.getCockpitTypeString(getCockpitType());
    }

    public static String getCockpitTypeString(int inCockpitType) {
        if ((inCockpitType < 0) || (inCockpitType >= COCKPIT_STRING.length)) {
            return "Unknown";
        }
        return COCKPIT_STRING[inCockpitType];
    }

    @Override
    public double getArmorRemainingPercent() {
        int armor0 = getTotalOArmor();
        int armor = getTotalArmor();
        if(isCapitalFighter()) {
            armor0 = getCap0Armor();
            armor = getCapArmor();
        }
        if(armor0 == 0) {
            return IArmorState.ARMOR_NA;
        }
        return ((double)armor / (double)armor0);
    }

    /**
     * keep track of whether the wings have suffered a weapon critical hit
     */
    public boolean areWingsHit() {
        return wingsHit;
    }

    public void setWingsHit(boolean b) {
        wingsHit = b;
    }

    /**
     * what location is opposite the given one
     */
    public int getOppositeLocation(int loc) {
        switch(loc) {
        case Aero.LOC_NOSE:
            return Aero.LOC_AFT;
        case Aero.LOC_LWING:
            return Aero.LOC_RWING;
        case Aero.LOC_RWING:
            return Aero.LOC_LWING;
        case Aero.LOC_AFT:
            return Aero.LOC_NOSE;
        default:
            return Aero.LOC_NOSE;
        }
    }

    /**
     * get modifications to the cluster hit table for critical hits
     */
    public int getClusterMods() {
        return -1*(getFCSHits() + getSensorHits());
    }

    /**
     * What's the range of the ECM equipment?
     *
     * @return the <code>int</code> range of this unit's ECM. This value will
     *         be <code>Entity.NONE</code> if no ECM is active.
     */
    @Override
    public int getECMRange() {
        if(!game.getOptions().booleanOption("stratops_ecm") || !game.getBoard().inSpace()) {
            return super.getECMRange();
        }
        return Math.min(super.getECMRange(), 0);
    }

    /**
     * @return the strength of the ECCM field this unit emits
     */
    @Override
    public double getECCMStrength() {
        if(!game.getOptions().booleanOption("stratops_ecm") || !game.getBoard().inSpace()) {
            return super.getECCMStrength();
        }
        if(hasActiveECCM()) {
            return 1;
        }
        return 0;
    }

    public void setECCMRoll(int i) {
        eccmRoll = i;
    }

    public int getECCMRoll() {
        return eccmRoll;
    }

    public int getECCMTarget() {
        return crew.getPiloting() + getSensorHits() + getCICHits() + getFCSHits();
    }

    public int getECCMBonus() {
        return Math.max(0, eccmRoll - getECCMTarget());
    }

    /**
     * @return is  the crew of this vessel protected from gravitational effects, see StratOps, pg. 36
     */
    public boolean isCrewProtected() {
        return true;
    }

    public int getGravSecondaryThreshold() {
        int thresh = 6;
        if(isCrewProtected()) {
            thresh = 12;
        }
        //TODO: clan phenotypes
        return thresh;
    }

    public int getGravPrimaryThreshold() {
        int thresh = 12;
        if(isCrewProtected()) {
            thresh = 22;
        }
        //TODO: clan phenotypes
        return thresh;
    }

    /**
     * Determines if this object can accept the given unit.  The unit may
     * not be of the appropriate type or there may be no room for the unit.
     *
     * @param   unit - the <code>Entity</code> to be loaded.
     * @return  <code>true</code> if the unit can be loaded,
     *          <code>false</code> otherwise.
     */
    @Override
    public boolean canLoad( Entity unit ) {
        // capital fighters can load other capital fighters (becoming squadrons)
        //but not in the deployment phase
        if ( isCapitalFighter() && !unit.isEnemyOf(this) && unit.isCapitalFighter()
                && (getId() != unit.getId()) && (game.getPhase() != IGame.Phase.PHASE_DEPLOYMENT)) {
            return true;
        }

        return super.canLoad(unit);
    }

    /***
     * use the specified amount of fuel for this Aero. The amount may be adjusted by certain game options
     */
    public void useFuel(int fuel) {
        setFuel(Math.max(0,getFuel()- fuel));
    }


    public void updateWeaponGroups() {
        //first we need to reset all the weapons in our existing mounts to zero until proven otherwise
        Set<String> set= weaponGroups.keySet();
        Iterator<String> iter = set.iterator();
        while(iter.hasNext()) {
            String key = iter.next();
            this.getEquipment(weaponGroups.get(key)).setNWeapons(0);
        }
        //now collect a hash of all the same weapons in each location by id
        Map<String, Integer> groups = new HashMap<String, Integer>();
        for (Mounted mounted : getTotalWeaponList()) {
            int loc = mounted.getLocation();
            if((loc == Aero.LOC_RWING) || (loc == Aero.LOC_LWING)) {
                loc = Aero.LOC_WINGS;
            }
            if(mounted.isRearMounted()) {
                loc = Aero.LOC_AFT;
            }
            String key = mounted.getType().getInternalName() + ":" + loc;
            if(null == groups.get(key)) {
                groups.put(key, mounted.getNWeapons());
            } else {
                    groups.put(key, groups.get(key) + mounted.getNWeapons());
            }
        }
        //now we just need to traverse the hash and either update our existing equipment or add new ones if there is none
        Set<String> newSet = groups.keySet();
        Iterator<String> newIter = newSet.iterator();
        while(newIter.hasNext()) {
            String key = newIter.next();
            if(null != weaponGroups.get(key)) {
                //then this equipment is already loaded, so we just need to correctly update the number of weapons
                this.getEquipment(weaponGroups.get(key)).setNWeapons(groups.get(key));
            } else {
                //need to add a new weapon
                String name = key.split(":")[0];
                int loc = Integer.parseInt(key.split(":")[1]);
                EquipmentType etype = EquipmentType.get(name);
                Mounted newmount;
                if (etype != null) {
                    try {
                        newmount = addWeaponGroup(etype, loc);
                        newmount.setNWeapons(groups.get(key));
                        weaponGroups.put(key, getEquipmentNum(newmount));
                    } catch (LocationFullException ex) {
                        System.out.println("Unable to compile weapon groups"); //$NON-NLS-1$
                        ex.printStackTrace();
                        return;
                    }
                }
                else if(name != "0"){
                    addFailedEquipment(name);
                }
            }
        }
    }

    /**
     * In cases where another unit occupies the same hex, determine if this Aero should be moved
     * back a hex for targeting purposes
     * @param otherPos
     * @return
     */
    public boolean shouldMoveBackHex(Aero other) {
        if(null == getPosition()) {
            return false;
        }
        if(null == other.getPosition()) {
            return false;
        }
        if(!getPosition().equals(other.getPosition())) {
            return false;
        }
        int type = UnitType.determineUnitTypeCode(this);
        int otherType = UnitType.determineUnitTypeCode(other);
        int vel = getCurrentVelocity();
        int otherVel = other.getCurrentVelocity();
        if(type > otherType) {
            return false;
        } else if(type < otherType) {
            return true;
        }
        //if we are still here then type is the same so compare velocity
        if(vel < otherVel) {
            return false;
        } else if(vel > otherVel) {
            return true;
        }
        //if we are still here then type and velocity same, so roll for it
        if(getWhoFirst() < other.getWhoFirst()) {
            return false;
        } else {
            return true;
        }
    }

    @Override
    public boolean hasArmoredEngine() {
        for (int slot = 0; slot < getNumberOfCriticals(LOC_AFT); slot++) {
            CriticalSlot cs = getCritical(LOC_AFT, slot);
            if ((cs != null) && (cs.getType() == CriticalSlot.TYPE_SYSTEM) && (cs.getIndex() == Mech.SYSTEM_ENGINE)) {
                return cs.isArmored();
            }
        }
        return false;
    }

}
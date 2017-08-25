/*
 * MegaMek - Copyright (C) 2000-2003 Ben Mazur (bmazur@sev.org)
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

import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Vector;

import megamek.common.options.OptionsConstants;
import megamek.common.weapons.BayWeapon;
import megamek.common.weapons.EnergyWeapon;
import megamek.common.weapons.PPCWeapon;

/**
 * Taharqa's attempt at creating an Aerospace entity
 */
public class Aero extends Entity implements IAero, IBomber {
    /**
     *
     */
    private static final long serialVersionUID = 7196307097459255187L;

    // locations
    public static final int LOC_NOSE = 0;
    public static final int LOC_LWING = 1;
    public static final int LOC_RWING = 2;
    public static final int LOC_AFT = 3;
    public static final int LOC_WINGS = 4;

    // ramming angles
    public static final int RAM_TOWARD_DIR = 0;
    public static final int RAM_TOWARD_OBL = 1;
    public static final int RAM_AWAY_OBL = 2;
    public static final int RAM_AWAY_DIR = 3;

    // heat type
    public static final int HEAT_SINGLE = 0;
    public static final int HEAT_DOUBLE = 1;

    // cockpit types
    public static final int COCKPIT_STANDARD = 0;
    public static final int COCKPIT_SMALL = 1;
    public static final int COCKPIT_COMMAND_CONSOLE = 2;
    public static final int COCKPIT_PRIMITIVE = 3;
    public static final String[] COCKPIT_STRING = { "Standard Cockpit", "Small Cockpit", "Command Console",
            "Primitive Cockpit" };
    public static final String[] COCKPIT_SHORT_STRING = { "Standard", "Small", "Command Console", "Primitive" };

    // critical hits
    public static final int CRIT_NONE = -1;
    public static final int CRIT_CREW = 0;
    public static final int CRIT_FCS = 1;
    public static final int CRIT_WEAPON = 2;
    public static final int CRIT_CONTROL = 3;
    public static final int CRIT_SENSOR = 4;
    public static final int CRIT_BOMB = 5;
    public static final int CRIT_ENGINE = 6;
    public static final int CRIT_FUEL_TANK = 7;
    public static final int CRIT_AVIONICS = 8;
    public static final int CRIT_GEAR = 9;
    public static final int CRIT_HEATSINK = 10;
    public static final int CRIT_CARGO = 11;
    public static final int CRIT_DOCK_COLLAR = 12;
    public static final int CRIT_DOOR = 13;
    public static final int CRIT_KF_BOOM = 14;
    public static final int CRIT_LIFE_SUPPORT = 15;
    public static final int CRIT_LEFT_THRUSTER = 16;
    public static final int CRIT_RIGHT_THRUSTER = 17;
    public static final int CRIT_CIC = 18;
    public static final int CRIT_KF_DRIVE = 19;
    public static final int CRIT_GRAV_DECK = 20;
    public static final int CRIT_WEAPON_BROAD = 21;

    // aeros have no critical slot limitations
    // this needs to be larger, it is too easy to go over when you get to
    // warships
    // and bombs and such
    private static final int[] NUM_OF_SLOTS = { 100, 100, 100, 100, 100, 100 };

    private static String[] LOCATION_ABBRS = { "NOS", "LWG", "RWG", "AFT", "WNG" };
    private static String[] LOCATION_NAMES = { "Nose", "Left Wing", "Right Wing", "Aft", "Wings" };

    @Override
    public String[] getLocationAbbrs() {
        return LOCATION_ABBRS;
    }

    @Override
    public String[] getLocationNames() {
        return LOCATION_NAMES;
    }

    private int sensorHits = 0;
    private int fcsHits = 0;
    private int engineHits = 0;
    private int avionicsHits = 0;
    private int cicHits = 0;
    private boolean gearHit = false;
    private int structIntegrity;
    private int orig_structIntegrity;
    // set up damage threshold
    protected int damThresh[] = { 0, 0, 0, 0, 0 };
    // set up an int for what the critical effect would be
    private int potCrit = CRIT_NONE;

    // ignored crew hit for harjel
    private int ignoredCrewHits = 0;
    private int cockpitType = COCKPIT_STANDARD;

    // track straight movement from last turn
    private int straightMoves = 0;

    // are we tracking any altitude loss due to air-to-ground assaults
    private int altLoss = 0;

    /**
     * Track how much altitude has been lost this turn. This is important for
     * properly making weapon attacks, so WeaponAttackActions knows what the
     * altitude was before the attack happened, since the altitude lose is
     * applied before the attack resolves.
     */
    private int altLossThisRound = 0;

    private boolean spheroid = false;

    // deal with heat
    private int heatSinksOriginal;
    private int heatSinks;
    private int heatType = HEAT_SINGLE;

    // Track how many heat sinks are pod-mounted for omnifighters; these are
    // included in the total
    // This is provided for campaign use; MM does not distribute damage between
    // fixed and pod-mounted.
    private int podHeatSinks;

    protected int maxBombPoints = 0;
    protected int[] bombChoices = new int[BombType.B_NUM];

    // fuel - number of fuel points
    private int fuel = 0;

    // these are used by more advanced aeros
    private boolean lifeSupport = true;
    private int leftThrustHits = 0;
    private int rightThrustHits = 0;

    // out of control
    private boolean outControl = false;
    private boolean outCtrlHeat = false;
    private boolean randomMove = false;

    // set up movement
    private int currentVelocity = 0;
    private int nextVelocity = currentVelocity;
    private boolean accLast = false;
    private boolean rolled = false;
    private boolean failedManeuver = false;
    private boolean accDecNow = false;

    // was the damage threshold exceeded this turn
    boolean critThresh = false;

    // vstol status
    boolean vstol = false;

    // Capital Fighter stuff
    private int capitalArmor = 0;
    private int capitalArmor_orig = 0;
    private int fatalThresh = 2;
    private int currentDamage = 0;
    private boolean wingsHit = false;
    // a hash map of the current weapon groups - the key is the
    // location:internal name, and the value is the weapon id
    Map<String, Integer> weaponGroups = new HashMap<String, Integer>();

    /*
     * According to the rules if two units of the same type and with the same
     * velocity are in the same hex, you roll 2d6 randomly to see who is
     * considered back one step for purposes of targeting. THis is a bitch to
     * do, so instead we assign a large random variable to each aero unit at the
     * start of the round and we use that. It works out similarly except that
     * you don't roll separately for each pair of possibilities. That should
     * work well enough for our purposes.
     */
    private int whoFirst = 0;

    private int eccmRoll = 0;

    public Aero() {
        super();
        // need to set altitude to something different than entity
        altitude = 5;
    }

    /**
     * Returns this entity's safe thrust, factored for heat, extreme
     * temperatures, gravity, and bomb load.
     */
    @Override
    public int getWalkMP(boolean gravity, boolean ignoreheat, boolean ignoremodulararmor) {
        int j = getOriginalWalkMP();
        // adjust for engine hits
        if (engineHits >= getMaxEngineHits()) {
            return 0;
        }
        int engineLoss = 2;
        if ((this instanceof SmallCraft) || (this instanceof Jumpship)) {
            engineLoss = 1;
        }
        j = Math.max(0, j - (engineHits * engineLoss));
        j = Math.max(0, j - getCargoMpReduction());
        if ((null != game) && gravity) {
            int weatherMod = game.getPlanetaryConditions().getMovementMods(this);
            if (weatherMod != 0) {
                j = Math.max(j + weatherMod, 0);
            }
        }
        // get bomb load
        j = Math.max(0, j - (int) Math.ceil(getBombPoints() / 5.0));

        if (hasModularArmor()) {
            j--;
        }

        // if they are not airborne, then they get MP halved (aerodyne) or no MP
        if (!isAirborne()) {
            j = j / 2;
            if (isSpheroid()) {
                j = 0;
            }
        }

        return j;
    }

    /**
     * Thi is the same as getWalkMP, but does not divide by 2 when grounded
     *
     * @return
     */
    @Override
    public int getCurrentThrust() {
        int j = getOriginalWalkMP();
        j = Math.max(0, j - getCargoMpReduction());
        if (null != game) {
            int weatherMod = game.getPlanetaryConditions().getMovementMods(this);
            if (weatherMod != 0) {
                j = Math.max(j + weatherMod, 0);
            }
        }
        // get bomb load
        j = Math.max(0, j - (int) Math.ceil(getBombPoints() / 5.0));

        if (hasModularArmor()) {
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

    @Override
    public boolean isOutControlTotal() {
        // due to control roll, heat, shut down, or crew unconscious
        return (outControl || shutDown || getCrew().isUnconscious());
    }

    @Override
    public boolean isOutControl() {
        return outControl;
    }

    @Override
    public boolean isOutCtrlHeat() {
        return outCtrlHeat;
    }

    @Override
    public boolean isRandomMove() {
        return randomMove;
    }

    @Override
    public boolean didAccLast() {
        return accLast;
    }

    @Override
    public boolean hasLifeSupport() {
        return lifeSupport;
    }

    public void setLifeSupport(boolean b) {
        lifeSupport = b;
    }

    @Override
    public boolean isRolled() {
        return rolled;
    }

    @Override
    public void setOutControl(boolean ocontrol) {
        outControl = ocontrol;
    }

    @Override
    public void setOutCtrlHeat(boolean octrlheat) {
        outCtrlHeat = octrlheat;
    }

    @Override
    public void setRandomMove(boolean randmove) {
        randomMove = randmove;
    }

    @Override
    public void setRolled(boolean roll) {
        rolled = roll;
    }

    @Override
    public void setAccLast(boolean b) {
        accLast = b;
    }

    @Override
    public int getMaxBombPoints() {
        return maxBombPoints;
    }

    public void autoSetMaxBombPoints() {
        maxBombPoints = (int) Math.round(getWeight() / 5);
    }

    @Override
    public int[] getBombChoices() {
        return bombChoices.clone();
    }

    @Override
    public void setBombChoices(int[] bc) {
        if (bc.length == bombChoices.length) {
            bombChoices = bc;
        }
    }

    public void setWhoFirst() {
        whoFirst = Compute.randomInt(500);
    }

    public int getWhoFirst() {
        return whoFirst;
    }

    @Override
    public int getCurrentVelocity() {
        // if using advanced movement then I just want to sum up
        // the different vectors
        if ((game != null) && game.useVectorMove()) {
            return getVelocity();
        }
        return currentVelocity;
    }

    @Override
    public void setCurrentVelocity(int velocity) {
        currentVelocity = velocity;
    }

    @Override
    public int getNextVelocity() {
        return nextVelocity;
    }

    @Override
    public void setNextVelocity(int velocity) {
        nextVelocity = velocity;
    }

    // need some way of retrieving true current velocity
    // even when using advanced movement
    @Override
    public int getCurrentVelocityActual() {
        return currentVelocity;
    }

    public int getPotCrit() {
        return potCrit;
    }

    public void setPotCrit(int crit) {
        potCrit = crit;
    }

    @Override
    public int getSI() {
        return structIntegrity;
    }

    @Override
    public int get0SI() {
        return orig_structIntegrity;
    }

    /**
     * Used to determine modifier for landing; different for Aero and LAM.
     */
    @Override
    public int getNoseArmor() {
        return getArmor(LOC_NOSE);
    }

    @Override
    public int getCapArmor() {
        return capitalArmor;
    }

    @Override
    public void setCapArmor(int i) {
        capitalArmor = i;
    }

    @Override
    public int getCap0Armor() {
        return capitalArmor_orig;
    }

    @Override
    public int getFatalThresh() {
        return fatalThresh;
    }

    @Override
    public int getCurrentDamage() {
        return currentDamage;
    }

    @Override
    public void setCurrentDamage(int i) {
        currentDamage = i;
    }

    public void set0SI(int si) {
        orig_structIntegrity = si;
        structIntegrity = si;
    }

    public void autoSetSI() {
        int siweight = (int) Math.floor(weight / 10.0);
        int sithrust = getOriginalWalkMP();
        initializeSI(Math.max(siweight, sithrust));
    }

    @Override
    public void autoSetCapArmor() {
        double divisor = 10.0;
        if ((null != game) && game.getOptions().booleanOption(OptionsConstants.ADVAERORULES_AERO_SANITY)) {
            divisor = 1.0;
        }
        capitalArmor_orig = (int) Math.round(getTotalOArmor() / divisor);
        capitalArmor = (int) Math.round(getTotalArmor() / divisor);
    }

    @Override
    public void autoSetFatalThresh() {
        int baseThresh = 2;
        if ((null != game) && game.getOptions().booleanOption(OptionsConstants.ADVAERORULES_AERO_SANITY)) {
            baseThresh = 20;
        }
        fatalThresh = Math.max(baseThresh, (int) Math.ceil(capitalArmor / 4.0));
    }

    public void initializeSI(int val) {
        orig_structIntegrity = val;
        setSI(val);
    }

    @Override
    public void setSI(int si) {
        structIntegrity = si;
    }

    @Override
    public int getSensorHits() {
        return sensorHits;
    }

    public void setSensorHits(int hits) {
        if (hits > 3) {
            hits = 3;
        }
        sensorHits = hits;
    }

    @Override
    public int getFCSHits() {
        return fcsHits;
    }

    public void setFCSHits(int hits) {
        if (hits > 3) {
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

    @Override
    public int getEngineHits() {
        return engineHits;
    }

    public void setEngineHits(int hits) {
        engineHits = hits;
    }

    @Override
    public int getAvionicsHits() {
        return avionicsHits;
    }

    public void setAvionicsHits(int hits) {
        avionicsHits = hits;
    }

    public boolean isGearHit() {
        return gearHit;
    }

    @Override
    public void setGearHit(boolean hit) {
        gearHit = hit;
    }

    /**
     * Modifier to landing or vertical takeoff roll for landing gear damage.
     *
     * @param vTakeoff
     *            true if this is for a vertical takeoff, false if for a landing
     * @return the control roll modifier
     */
    @Override
    public int getLandingGearMod(boolean vTakeoff) {
        if (gearHit) {
            return vTakeoff ? 1 : 5;
        } else {
            return 0;
        }
    }

    public void setOHeatSinks(int hs) {
        heatSinksOriginal = hs;
    }

    public int getOHeatSinks() {
        return heatSinksOriginal;
    }

    public void setHeatSinks(int hs) {
        heatSinks = hs;
    }

    @Override
    public int getHeatSinks() {
        return heatSinks;
    }

    public int getHeatSinkHits() {
        return heatSinksOriginal - heatSinks;
    }

    public void setHeatType(int hstype) {
        heatType = hstype;
    }

    public int getPodHeatSinks() {
        return podHeatSinks;
    }

    public void setPodHeatSinks(int hs) {
        podHeatSinks = hs;
    }

    public void setLeftThrustHits(int hits) {
        leftThrustHits = hits;
    }

    @Override
    public int getLeftThrustHits() {
        return leftThrustHits;
    }

    public void setRightThrustHits(int hits) {
        rightThrustHits = hits;
    }

    @Override
    public int getRightThrustHits() {
        return rightThrustHits;
    }

    @Override
    public int getFuel() {
        return fuel;
    }

    /**
     * Sets the number of fuel points.
     * 
     * @param gas
     *            Number of fuel points.
     */
    @Override
    public void setFuel(int gas) {
        fuel = gas;
    }

    @Override
    public double getFuelPointsPerTon() {
        if (getEntityType() == Entity.ETYPE_CONV_FIGHTER) {
            return 160;
        } else if (getEntityType() == Entity.ETYPE_DROPSHIP) {
            if (getWeight() < 400) {
                return 80;
            } else if (getWeight() < 800) {
                return 70;
            } else if (getWeight() < 1200) {
                return 60;
            } else if (getWeight() < 1900) {
                return 50;
            } else if (getWeight() < 3000) {
                return 40;
            } else if (getWeight() < 20000) {
                return 30;
            } else if (getWeight() < 40000) {
                return 20;
            } else {
                return 10;
            }
        } else if ((getEntityType() == Entity.ETYPE_WARSHIP) || (getEntityType() == Entity.ETYPE_JUMPSHIP)
                || (getEntityType() == Entity.ETYPE_SPACE_STATION)) {
            if (getWeight() < 110000) {
                return 10;
            } else if (getWeight() < 250000) {
                return 5;
            } else {
                return 2.5;
            }
        } else if (getEntityType() == Entity.ETYPE_SMALL_CRAFT) {
            return 80;
        } else { // Entity.ETYPE_AERO
            return 80;
        }
    }

    /**
     * Set number of fuel points based on fuel tonnage.
     *
     * @param fuelTons
     *            The number of tons of fuel
     */
    @Override
    public void setFuelTonnage(double fuelTons) {
        double pointsPerTon = getFuelPointsPerTon();
        fuel = (int) Math.ceil(pointsPerTon * fuelTons);
    }

    /**
     * Gets the fuel for this Aero in terms of tonnage.
     *
     * @return The number of tons of fuel on this Aero.
     */
    @Override
    public double getFuelTonnage() {
        return fuel / getFuelPointsPerTon();
    }

    public int getHeatType() {
        return heatType;
    }

    @Override
    public boolean wasCritThresh() {
        return critThresh;
    }

    @Override
    public void setCritThresh(boolean b) {
        critThresh = b;
    }

    @Override
    public boolean isImmobile() {
        // aeros are never immobile when in the air or space
        if (isAirborne() || isSpaceborne()) {
            return false;
        }
        return super.isImmobile();
    }

    @Override
    public void newRound(int roundNumber) {
        super.newRound(roundNumber);

        // reset threshold critted
        setCritThresh(false);

        // reset maneuver status
        setFailedManeuver(false);
        // reset acc/dec this turn
        setAccDecNow(false);

        updateBays();

        // update recovery turn if in recovery
        if (getRecoveryTurn() > 0) {
            setRecoveryTurn(getRecoveryTurn() - 1);
        }

        // if in atmosphere, then halve next turn's velocity
        if (!game.getBoard().inSpace() && isDeployed() && (roundNumber > 0)) {
            setNextVelocity((int) Math.floor(getNextVelocity() / 2.0));
        }

        // update velocity
        setCurrentVelocity(getNextVelocity());

        // if using variable damage thresholds then autoset them
        if (game.getOptions().booleanOption(OptionsConstants.ADVAERORULES_VARIABLE_DAMAGE_THRESH)) {
            autoSetThresh();
            autoSetFatalThresh();
        }

        // if they are out of control due to heat, then apply this and reset
        if (isOutCtrlHeat()) {
            setOutControl(true);
            setOutCtrlHeat(false);
        }

        // reset eccm bonus
        setECCMRoll(Compute.d6(2));

        // get new random whofirst
        setWhoFirst();

        resetAltLossThisRound();
    }

    /**
     * Returns the name of the type of movement used. This is tank-specific.
     */
    @Override
    public String getMovementString(EntityMovementType mtype) {
        switch (mtype) {
        case MOVE_SKID:
            return "Skidded";
        case MOVE_NONE:
            return "None";
        case MOVE_WALK:
            return "Cruised";
        case MOVE_RUN:
            return "Flanked";
        case MOVE_SAFE_THRUST:
            return "Safe Thrust";
        case MOVE_OVER_THRUST:
            return "Over Thrust";
        default:
            return "Unknown!";
        }
    }

    /**
     * Returns the name of the type of movement used. This is tank-specific.
     */
    @Override
    public String getMovementAbbr(EntityMovementType mtype) {
        switch (mtype) {
        case MOVE_NONE:
            return "N";
        case MOVE_SAFE_THRUST:
            return "S";
        case MOVE_OVER_THRUST:
            return "O";
        default:
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
    // need to figure out aft-pointed wing weapons
    // need to figure out new arcs
    @Override
    public int getWeaponArc(int wn) {
        final Mounted mounted = getEquipment(wn);
        if (mounted.getType().hasFlag(WeaponType.F_SPACE_BOMB) || mounted.getType().hasFlag(WeaponType.F_DIVE_BOMB)
                || mounted.getType().hasFlag(WeaponType.F_ALT_BOMB)) {
            return Compute.ARC_360;
        }
        int arc = Compute.ARC_NOSE;
        switch (mounted.getLocation()) {
        case LOC_NOSE:
            arc = Compute.ARC_NOSE;
            break;
        case LOC_RWING:
            if (mounted.isRearMounted()) {
                arc = Compute.ARC_RWINGA;
            } else {
                arc = Compute.ARC_RWING;
            }
            break;
        case LOC_LWING:
            if (mounted.isRearMounted()) {
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
     * Returns true if this weapon fires into the secondary facing arc. If
     * false, assume it fires into the primary.
     */
    @Override
    public boolean isSecondaryArcWeapon(int weaponId) {
        // just leave true for now in case we implement rolls or
        // newtonian movement this way
        return true;
    }

    /**
     * Rolls up a hit location
     */
    @Override
    public HitData rollHitLocation(int table, int side, int aimedLocation, int aimingMode, int cover) {
        return rollHitLocation(table, side);
    }

    @Override
    public HitData rollHitLocation(int table, int side) {

        /*
         * Unlike other units, ASFs determine potential crits based on the
         * to-hit roll so I need to set this potential value as well as return
         * the to hit data
         */

        int roll = Compute.d6(2);

        // first check for above/below
        if ((table == ToHitData.HIT_ABOVE) || (table == ToHitData.HIT_BELOW)) {

            // have to decide which wing
            int wingloc = LOC_RWING;
            int wingroll = Compute.d6(1);
            if (wingroll > 3) {
                wingloc = LOC_LWING;
            }
            switch (roll) {
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

        if (side == ToHitData.SIDE_FRONT) {
            // normal front hits
            switch (roll) {
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
        } else if (side == ToHitData.SIDE_LEFT) {
            // normal left-side hits
            switch (roll) {
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
        } else if (side == ToHitData.SIDE_RIGHT) {
            // normal right-side hits
            switch (roll) {
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
        } else if (side == ToHitData.SIDE_REAR) {
            // normal aft hits
            switch (roll) {
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
     * 
     * @see megamek.common.Entity#calculateBattleValue()
     */
    @Override
    public int calculateBattleValue() {
        if (useManualBV) {
            return manualBV;
        }

        return calculateBattleValue(false, false);
    }

    /*
     * (non-Javadoc)
     * 
     * @see megamek.common.Entity#calculateBattleValue(boolean, boolean)
     */
    @Override
    public int calculateBattleValue(boolean ignoreC3, boolean ignorePilot) {
        if (useManualBV) {
            return manualBV;
        }

        bvText = new StringBuffer("<HTML><BODY><CENTER><b>Battle Value Calculations For ");

        bvText.append(getChassis());
        bvText.append(" ");
        bvText.append(getModel());
        bvText.append("</b></CENTER>");
        bvText.append(nl);

        bvText.append("<b>Defensive Battle Rating Calculation:</b>");
        bvText.append(nl);

        double dbv = 0; // defensive battle value
        double obv = 0; // offensive bv

        double armorMultiplier = 1.0;

        boolean blueShield = hasWorkingMisc(MiscType.F_BLUE_SHIELD);

        for (int loc = 0; loc < (this instanceof SmallCraft ? locations() : (locations() - 1)); loc++) {

            int modularArmor = 0;
            for (Mounted mounted : getEquipment()) {
                if ((mounted.getType() instanceof MiscType) && mounted.getType().hasFlag(MiscType.F_MODULAR_ARMOR)
                        && (mounted.getLocation() == loc)) {
                    modularArmor += mounted.getBaseDamageCapacity() - mounted.getDamageTaken();
                }
            }
            // total armor points

            switch (getArmorType(loc)) {
            case EquipmentType.T_ARMOR_COMMERCIAL:
                armorMultiplier = 0.5;
                break;
            case EquipmentType.T_ARMOR_HARDENED:
                armorMultiplier = 2.0;
                break;
            case EquipmentType.T_ARMOR_REACTIVE:
            case EquipmentType.T_ARMOR_REFLECTIVE:
            case EquipmentType.T_ARMOR_BALLISTIC_REINFORCED:
                armorMultiplier = 1.5;
                break;
            case EquipmentType.T_ARMOR_LAMELLOR_FERRO_CARBIDE:
            case EquipmentType.T_ARMOR_FERRO_LAMELLOR:
            case EquipmentType.T_ARMOR_ANTI_PENETRATIVE_ABLATION:
                armorMultiplier = 1.2;
                break;
            default:
                armorMultiplier = 1.0;
                break;
            }

            if (blueShield) {
                armorMultiplier += 0.2;
            }
            bvText.append(startRow);
            bvText.append(startColumn);

            int armor = getArmor(loc) + modularArmor;
            bvText.append("Total Armor " + this.getLocationAbbr(loc) + " (" + armor + ") x ");
            bvText.append(armorMultiplier);
            bvText.append(endColumn);
            bvText.append(startColumn);
            bvText.append(endColumn);
            bvText.append(startColumn);
            double armorBV = (getArmor(loc) + modularArmor) * armorMultiplier;
            bvText.append(armorBV);
            dbv += armorBV;
            bvText.append(endColumn);
        }
        bvText.append(startRow);
        bvText.append(startColumn);
        bvText.append("Total modified armor BV x 2.5 ");
        bvText.append(endColumn);
        bvText.append(startColumn);
        bvText.append(endColumn);
        bvText.append(startColumn);
        bvText.append("= ");
        dbv *= 2.5;
        bvText.append(dbv);
        bvText.append(endColumn);
        bvText.append(endRow);

        bvText.append(startRow);
        bvText.append(startColumn);

        bvText.append("Total SI x 2 x SI modifier");
        bvText.append(endColumn);
        bvText.append(startColumn);

        double dbvSI = getSI() * 2.0 * (blueShield ? 1.2 : 1);
        dbv += dbvSI;

        bvText.append(getSI());
        bvText.append(" x 2 x ");
        bvText.append(blueShield ? 1.2 : 1);
        bvText.append(endColumn);
        bvText.append(startColumn);
        bvText.append("= ");
        bvText.append(dbvSI);
        bvText.append(endColumn);
        bvText.append(endRow);

        // add defensive equipment
        double amsBV = 0;
        double amsAmmoBV = 0;
        double screenBV = 0;
        double screenAmmoBV = 0;
        double defEqBV = 0;
        for (Mounted mounted : getEquipment()) {
            EquipmentType etype = mounted.getType();

            // don't count destroyed equipment
            if (mounted.isDestroyed()) {
                continue;
            }
            // don't count weapon groups
            if (mounted.isWeaponGroup()) {
                continue;
            }
            if (((etype instanceof WeaponType) && (etype.hasFlag(WeaponType.F_AMS)))) {
                amsBV += etype.getBV(this);
                bvText.append(startRow);
                bvText.append(startColumn);
                bvText.append(etype.getName());
                bvText.append(endColumn);
                bvText.append(startColumn);
                bvText.append("+");
                bvText.append(etype.getBV(this));
                bvText.append(endColumn);
                bvText.append(startColumn);
                bvText.append(endColumn);
                bvText.append(endRow);
            } else if ((etype instanceof AmmoType) && (((AmmoType) etype).getAmmoType() == AmmoType.T_AMS)) {
                amsAmmoBV += etype.getBV(this);
                bvText.append(startRow);
                bvText.append(startColumn);
                bvText.append(etype.getName());
                bvText.append(endColumn);
                bvText.append(startColumn);
                bvText.append("+");
                bvText.append(etype.getBV(this));
                bvText.append(endColumn);
                bvText.append(startColumn);
                bvText.append(endColumn);
                bvText.append(endRow);
            } else if ((etype instanceof AmmoType)
                    && (((AmmoType) etype).getAmmoType() == AmmoType.T_SCREEN_LAUNCHER)) {
                screenAmmoBV += etype.getBV(this);
                bvText.append(startRow);
                bvText.append(startColumn);
                bvText.append(etype.getName());
                bvText.append(endColumn);
                bvText.append(startColumn);
                bvText.append("+");
                bvText.append(etype.getBV(this));
                bvText.append(endColumn);
                bvText.append(startColumn);
                bvText.append(endColumn);
                bvText.append(endRow);
            } else if ((etype instanceof WeaponType)
                    && (((WeaponType) etype).getAtClass() == WeaponType.CLASS_SCREEN)) {
                screenBV += etype.getBV(this);
                bvText.append(startRow);
                bvText.append(startColumn);
                bvText.append(etype.getName());
                bvText.append(endColumn);
                bvText.append(startColumn);
                bvText.append("+");
                bvText.append(etype.getBV(this));
                bvText.append(endColumn);
                bvText.append(startColumn);
                bvText.append(endColumn);
                bvText.append(endRow);
            } else if ((etype instanceof MiscType) && (etype.hasFlag(MiscType.F_ECM) || etype.hasFlag(MiscType.F_BAP)
                    || etype.hasFlag(MiscType.F_CHAFF_POD))) {
                defEqBV += etype.getBV(this);
                bvText.append(startRow);
                bvText.append(startColumn);
                bvText.append(etype.getName());
                bvText.append(endColumn);
                bvText.append(startColumn);
                bvText.append("+");
                bvText.append(etype.getBV(this));
                bvText.append(endColumn);
                bvText.append(startColumn);
                bvText.append(endColumn);
                bvText.append(endRow);
            }
        }
        if (amsBV > 0) {
            bvText.append(startRow);
            bvText.append(startColumn);
            bvText.append("Total AMS BV:");
            bvText.append(endColumn);
            bvText.append(startColumn);
            bvText.append(endColumn);
            bvText.append(startColumn);
            bvText.append(amsBV);
            dbv += amsBV;
            bvText.append(endColumn);
            bvText.append(endRow);
        }
        if (screenBV > 0) {
            bvText.append(startRow);
            bvText.append(startColumn);
            bvText.append("Total Screen BV:");
            bvText.append(endColumn);
            bvText.append(startColumn);
            bvText.append(endColumn);
            bvText.append(startColumn);
            bvText.append(screenBV);
            dbv += screenBV;
            bvText.append(endColumn);
            bvText.append(endRow);
        }
        if (amsAmmoBV > 0) {
            bvText.append(startRow);
            bvText.append(startColumn);
            bvText.append("Total AMS Ammo BV (to a maximum of AMS BV):");
            bvText.append(endColumn);
            bvText.append(startColumn);
            bvText.append(endColumn);
            bvText.append(startColumn);
            bvText.append(Math.min(amsBV, amsAmmoBV));
            dbv += Math.min(amsBV, amsAmmoBV);
            bvText.append(endColumn);
            bvText.append(endRow);
        }
        if (screenAmmoBV > 0) {
            bvText.append(startRow);
            bvText.append(startColumn);
            bvText.append("Total Screen Ammo BV (to a maximum of Screen BV):");
            bvText.append(endColumn);
            bvText.append(startColumn);
            bvText.append(endColumn);
            bvText.append(startColumn);
            bvText.append(Math.min(screenBV, screenAmmoBV));
            dbv += Math.min(screenBV, screenAmmoBV);
            bvText.append(endColumn);
            bvText.append(endRow);
        }
        if (defEqBV > 0) {
            bvText.append(startRow);
            bvText.append(startColumn);
            bvText.append("Total misc defensive equipment BV:");
            bvText.append(endColumn);
            bvText.append(startColumn);
            bvText.append(endColumn);
            bvText.append(startColumn);
            bvText.append(defEqBV);
            dbv += defEqBV;
            bvText.append(endColumn);
            bvText.append(endRow);
        }

        bvText.append("-------------");
        bvText.append(endColumn);
        bvText.append(endRow);

        bvText.append(startRow);
        bvText.append(startColumn);

        bvText.append(endColumn);
        bvText.append(startColumn);
        bvText.append(endColumn);
        bvText.append(startColumn);
        bvText.append(dbv);
        bvText.append(endColumn);
        bvText.append(endRow);

        // subtract for explosive ammo
        double explosivePenalty = 0;
        Map<AmmoType, Boolean> ammos = new HashMap<AmmoType, Boolean>();
        for (Mounted mounted : getEquipment()) {
            int loc = mounted.getLocation();
            int toSubtract = 1;
            EquipmentType etype = mounted.getType();

            if (mounted.isWeaponGroup()) {
                continue;
            }

            // only count explosive ammo
            if (!etype.isExplosive(mounted, true)) {
                continue;
            }
            // PPCs with capacitors subtract 1
            if (etype instanceof PPCWeapon) {
                if (mounted.getLinkedBy() == null) {
                    continue;
                }
            }

            // PPC capacitor does not count separately, it's already counted for
            // with the PPC
            if ((etype instanceof MiscType) && etype.hasFlag(MiscType.F_PPC_CAPACITOR)) {
                continue;
            }

            // don't count oneshot ammo
            if (loc == LOC_NONE) {
                continue;
            }

            // CASE means no subtraction
            if (hasWorkingMisc(MiscType.F_CASE) || isClan()) {
                continue;
            }

            // RACs, LACs and ACs don't really count
            if ((etype instanceof WeaponType) && ((((WeaponType) etype).getAmmoType() == AmmoType.T_AC_ROTARY)
                    || (((WeaponType) etype).getAmmoType() == AmmoType.T_AC)
                    || (((WeaponType) etype).getAmmoType() == AmmoType.T_LAC))) {
                toSubtract = 0;
            }

            // empty ammo shouldn't count
            if ((etype instanceof AmmoType) && (mounted.getUsableShotsLeft() == 0)) {
                continue;
            }
            if (etype instanceof AmmoType) {
                ammos.put((AmmoType) etype, true);
            } else {
                explosivePenalty += toSubtract;
            }
        }
        explosivePenalty += ammos.size() * 15;
        dbv = Math.max(1, dbv - explosivePenalty);

        bvText.append(startRow);
        bvText.append(startColumn);

        bvText.append("Explosive Weapons/Equipment Penalty ");
        bvText.append(endColumn);
        bvText.append(startColumn);
        bvText.append(endColumn);
        bvText.append(startColumn);

        bvText.append("= -");
        bvText.append(explosivePenalty);
        bvText.append(endColumn);
        bvText.append(endRow);

        bvText.append(startRow);
        bvText.append(startColumn);
        bvText.append(endColumn);
        bvText.append(startColumn);
        bvText.append(endColumn);
        bvText.append(startColumn);

        bvText.append("-------------");
        bvText.append(endColumn);
        bvText.append(endRow);

        bvText.append(startRow);
        bvText.append(startColumn);

        bvText.append(endColumn);
        bvText.append(startColumn);
        bvText.append(endColumn);
        bvText.append(startColumn);
        bvText.append(dbv);
        bvText.append(endColumn);
        bvText.append(endRow);

        bvText.append(startRow);
        bvText.append(startColumn);
        bvText.append("Multiply by Unit Type Modifier");
        bvText.append(endColumn);
        bvText.append(startColumn);
        bvText.append(getBVTypeModifier());
        if (hasStealth()) {
            bvText.append("+ 0.3 for Stealth");
        }
        bvText.append(endColumn);
        // unit type multiplier
        dbv *= (getBVTypeModifier() + (hasStealth() ? 0.3 : 0));
        bvText.append(startColumn);
        bvText.append("x" + (getBVTypeModifier() + (hasStealth() ? 0.3 : 0)));
        bvText.append(endColumn);
        bvText.append(endRow);

        bvText.append(startRow);
        bvText.append(startColumn);
        bvText.append(endColumn);
        bvText.append(startColumn);
        bvText.append(endColumn);
        bvText.append(startColumn);
        bvText.append("-------------");
        bvText.append(endColumn);
        bvText.append(endRow);

        bvText.append(startRow);
        bvText.append(startColumn);
        bvText.append(endColumn);
        bvText.append(startColumn);
        bvText.append(endColumn);
        bvText.append(startColumn);
        bvText.append(dbv);
        bvText.append(endColumn);
        bvText.append(endRow);

        bvText.append(startRow);
        bvText.append(startColumn);

        bvText.append("<b>Offensive Battle Rating Calculation:</b>");
        bvText.append(endColumn);
        bvText.append(startColumn);
        bvText.append(endColumn);
        bvText.append(startColumn);
        bvText.append(endColumn);
        bvText.append(endRow);

        // calculate heat efficiency
        int aeroHeatEfficiency = 6 + getHeatCapacity();

        bvText.append(startRow);
        bvText.append(startColumn);

        bvText.append("Base Heat Efficiency ");

        bvText.append(endColumn);
        bvText.append(startColumn);
        bvText.append(aeroHeatEfficiency);

        bvText.append(endColumn);
        bvText.append(endRow);

        bvText.append(startRow);
        bvText.append(startColumn);
        bvText.append("Unmodified Weapon BV:");
        bvText.append(endColumn);
        bvText.append(startColumn);
        bvText.append(endColumn);
        bvText.append(startColumn);
        bvText.append(endColumn);
        bvText.append(endRow);

        double weaponBV = 0;
        boolean hasTargComp = hasTargComp();

        double targetingSystemBVMod = 1.0;

        if (this instanceof FixedWingSupport) {
            if (hasWorkingMisc(MiscType.F_ADVANCED_FIRECONTROL)) {
                targetingSystemBVMod = 1.0;
            } else if (hasWorkingMisc(MiscType.F_BASIC_FIRECONTROL)) {
                targetingSystemBVMod = .9;
            } else {
                targetingSystemBVMod = .8;
            }
        }

        // first, add up front-faced and rear-faced unmodified BV,
        // to know wether front- or rear faced BV should be halved
        double bvFront = 0, bvRear = 0;
        List<Mounted> weapons = getTotalWeaponList();
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
            if (wtype.getAtClass() == WeaponType.CLASS_SCREEN) {
                continue;
            }
            // do not count weapon groups
            if (weapon.isWeaponGroup()) {
                continue;
            }

            String name = wtype.getName();
            // PPC caps bump up the value
            if (weapon.getLinkedBy() != null) {
                // check to see if the weapon is a PPC and has a Capacitor
                // attached to it
                if (wtype.hasFlag(WeaponType.F_PPC)) {
                    dBV += ((MiscType) weapon.getLinkedBy().getType()).getBV(this, weapon);
                    name = name.concat(" with Capacitor");
                }
            }
            // calc MG Array here:
            if (wtype.hasFlag(WeaponType.F_MGA)) {
                double mgaBV = 0;
                for (Mounted possibleMG : getTotalWeaponList()) {
                    if (possibleMG.getType().hasFlag(WeaponType.F_MG)
                            && (possibleMG.getLocation() == weapon.getLocation())) {
                        mgaBV += possibleMG.getType().getBV(this);
                    }
                }
                dBV = mgaBV * 0.67;
            }
            bvText.append(startRow);
            bvText.append(startColumn);

            bvText.append(name);
            if (weapon.isRearMounted() || (weapon.getLocation() == LOC_AFT)) {
                bvRear += dBV;
                bvText.append(" (R)");
            } else {
                bvFront += dBV;
            }
            bvText.append(endColumn);
            bvText.append(startColumn);
            bvText.append(endColumn);
            bvText.append(startColumn);
            bvText.append(dBV);
            bvText.append(endColumn);
            bvText.append(endRow);
        }
        boolean halveRear = true;
        if (bvFront <= bvRear) {
            halveRear = false;

            bvText.append(startRow);
            bvText.append(startColumn);

            bvText.append("halving front instead of rear weapon BVs");
            bvText.append(endColumn);
            bvText.append(endRow);
            bvText.append(startRow);
            bvText.append(startColumn);
        }

        bvText.append(startRow);
        bvText.append(startColumn);
        bvText.append("Weapon Heat:");
        bvText.append(endColumn);
        bvText.append(startColumn);
        bvText.append(endColumn);
        bvText.append(startColumn);
        bvText.append(endColumn);
        bvText.append(endRow);

        // here we store the modified BV and heat of all heat-using weapons,
        // to later be sorted by BV
        ArrayList<ArrayList<Object>> heatBVs = new ArrayList<ArrayList<Object>>();
        // BVs of non-heat-using weapons
        ArrayList<ArrayList<Object>> nonHeatBVs = new ArrayList<ArrayList<Object>>();
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

            // do not count weapon groups
            if (mounted.isWeaponGroup()) {
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

            // laser insulator reduce heat by 1, to a minimum of 1
            if (wtype.hasFlag(WeaponType.F_LASER) && (mounted.getLinkedBy() != null)
                    && !mounted.getLinkedBy().isInoperable()
                    && mounted.getLinkedBy().getType().hasFlag(MiscType.F_LASER_INSULATOR)) {
                weaponHeat -= 1;
                if (weaponHeat == 0) {
                    weaponHeat++;
                }
            }

            // half heat for streaks
            if ((wtype.getAmmoType() == AmmoType.T_SRM_STREAK) || (wtype.getAmmoType() == AmmoType.T_MRM_STREAK)
                    || (wtype.getAmmoType() == AmmoType.T_LRM_STREAK)) {
                weaponHeat *= 0.5;
            }
            String name = wtype.getName();

            // check to see if the weapon is a PPC and has a Capacitor attached
            // to it
            if (wtype.hasFlag(WeaponType.F_PPC) && (mounted.getLinkedBy() != null)) {
                name = name.concat(" with Capacitor");
                weaponHeat += 5;
            }

            bvText.append(startRow);
            bvText.append(startColumn);

            bvText.append(name);
            bvText.append(endColumn);
            bvText.append(startColumn);
            bvText.append(endColumn);
            bvText.append(startColumn);
            bvText.append("+ ");
            bvText.append(weaponHeat);
            bvText.append(endColumn);
            bvText.append(endRow);

            double dBV = wtype.getBV(this);

            if (hasWorkingMisc(MiscType.F_DRONE_OPERATING_SYSTEM)) {
                dBV *= 0.8;
            }

            String weaponName = mounted.getName() + (mounted.isRearMounted() ? "(R)" : "");

            // don't count destroyed equipment
            if (mounted.isDestroyed()) {
                continue;
            }

            // don't count AMS, it's defensive
            if (wtype.hasFlag(WeaponType.F_AMS)) {
                continue;
            }
            // don't count screen launchers, they are defensive
            if (wtype.getAtClass() == WeaponType.CLASS_SCREEN) {
                continue;
            }
            // do not count weapon groups
            if (mounted.isWeaponGroup()) {
                continue;
            }
            // calc MG Array here:
            if (wtype.hasFlag(WeaponType.F_MGA)) {
                double mgaBV = 0;
                for (Mounted possibleMG : getTotalWeaponList()) {
                    if (possibleMG.getType().hasFlag(WeaponType.F_MG)
                            && (possibleMG.getLocation() == mounted.getLocation())) {
                        mgaBV += possibleMG.getType().getBV(this);
                    }
                }
                dBV = mgaBV * 0.67;
            }
            // artemis bumps up the value
            // PPC caps do, too
            if (mounted.getLinkedBy() != null) {
                // check to see if the weapon is a PPC and has a Capacitor
                // attached to it
                if (wtype.hasFlag(WeaponType.F_PPC)) {
                    dBV += ((MiscType) mounted.getLinkedBy().getType()).getBV(this, mounted);
                    name = name.concat(" with Capacitor");
                }
                Mounted mLinker = mounted.getLinkedBy();
                if ((mLinker.getType() instanceof MiscType) && mLinker.getType().hasFlag(MiscType.F_ARTEMIS)) {
                    dBV *= 1.2;
                    name = name.concat(" with Artemis IV");
                }
                if ((mLinker.getType() instanceof MiscType) && mLinker.getType().hasFlag(MiscType.F_ARTEMIS_V)) {
                    dBV *= 1.3;
                    name = name.concat(" with Artemis V");
                }
                if ((mLinker.getType() instanceof MiscType) && mLinker.getType().hasFlag(MiscType.F_APOLLO)) {
                    dBV *= 1.15;
                    name = name.concat(" with Apollo");
                }
                if ((mLinker.getType() instanceof MiscType)
                        && mLinker.getType().hasFlag(MiscType.F_RISC_LASER_PULSE_MODULE)) {
                    dBV *= 1.25;
                    name = name.concat(" with RISC Laser Pulse Module");
                }
            }

            // and we'll add the tcomp here too
            if (wtype.hasFlag(WeaponType.F_DIRECT_FIRE) && hasTargComp) {
                dBV *= 1.25;
            } else if ((this instanceof FixedWingSupport) && !wtype.hasFlag(WeaponType.F_INFANTRY)) {
                dBV *= targetingSystemBVMod;
            }

            // half for being rear mounted (or front mounted, when more rear-
            // than front-mounted un-modded BV
            if (((mounted.isRearMounted() || (mounted.getLocation() == LOC_AFT)) && halveRear)
                    || (!(mounted.isRearMounted() || (mounted.getLocation() == LOC_AFT)) && !halveRear)) {
                dBV /= 2;
            }

            // ArrayList that stores weapon values
            // stores a double first (BV), then an Integer (heat),
            // then a String (weapon name)
            // for 0 heat weapons, just stores BV and name
            ArrayList<Object> weaponValues = new ArrayList<Object>();
            if (weaponHeat > 0) {
                // store heat and BV, for sorting a few lines down;
                weaponValues.add(dBV);
                weaponValues.add(weaponHeat);
                weaponValues.add(weaponName);
                heatBVs.add(weaponValues);
            } else {
                weaponValues.add(dBV);
                weaponValues.add(weaponName);
                nonHeatBVs.add(weaponValues);
            }

            maximumHeat += weaponHeat;
            // add up BV of ammo-using weapons for each type of weapon,
            // to compare with ammo BV later for excessive ammo BV rule
            if (!((wtype.hasFlag(WeaponType.F_ENERGY) && !(wtype.getAmmoType() == AmmoType.T_PLASMA))
                    || wtype.hasFlag(WeaponType.F_ONESHOT) || wtype.hasFlag(WeaponType.F_INFANTRY)
                    || (wtype.getAmmoType() == AmmoType.T_NA))) {
                String key = wtype.getAmmoType() + ":" + wtype.getRackSize();
                if (!weaponsForExcessiveAmmo.containsKey(key)) {
                    weaponsForExcessiveAmmo.put(key, wtype.getBV(this));
                } else {
                    weaponsForExcessiveAmmo.put(key, wtype.getBV(this) + weaponsForExcessiveAmmo.get(key));
                }
            }
        }

        bvText.append(startRow);
        bvText.append(startColumn);
        bvText.append(endColumn);
        bvText.append(startColumn);
        bvText.append(endColumn);
        bvText.append(startColumn);

        bvText.append("-------------");
        bvText.append(endColumn);
        bvText.append(endRow);

        bvText.append(startRow);
        bvText.append(startColumn);

        bvText.append("Total Heat:");
        bvText.append(endColumn);
        bvText.append(startColumn);
        bvText.append(endColumn);
        bvText.append(startColumn);
        bvText.append("= ");
        bvText.append(maximumHeat);
        bvText.append(endColumn);
        bvText.append(endRow);

        bvText.append(startRow);
        bvText.append(startColumn);
        bvText.append("Weapons with no heat at full BV:");
        bvText.append(endColumn);
        bvText.append(endRow);
        // count heat-free weapons always at full modified BV
        for (ArrayList<Object> nonHeatWeapon : nonHeatBVs) {
            weaponBV += (Double) nonHeatWeapon.get(0);

            bvText.append(startRow);
            bvText.append(startColumn);
            bvText.append(nonHeatWeapon.get(1));
            if (nonHeatWeapon.get(1).toString().length() < 8) {
                bvText.append("\t");
            }
            bvText.append(endColumn);
            bvText.append(startColumn);
            bvText.append(startColumn);
            bvText.append(endColumn);
            bvText.append(nonHeatWeapon.get(0));
            bvText.append(endColumn);
            bvText.append(endRow);
        }

        bvText.append(startRow);
        bvText.append(startColumn);
        bvText.append("Heat Modified Weapons BV: ");
        bvText.append(endColumn);
        bvText.append(startColumn);
        bvText.append(endColumn);
        bvText.append(startColumn);
        bvText.append(endColumn);
        bvText.append(endRow);

        if (maximumHeat > aeroHeatEfficiency) {

            bvText.append(startRow);
            bvText.append(startColumn);

            bvText.append("(Heat Exceeds Aero Heat Efficiency) ");

            bvText.append(endColumn);
            bvText.append(startColumn);
            bvText.append(endColumn);
            bvText.append(startColumn);
            bvText.append(endColumn);
            bvText.append(endRow);
        }

        if (maximumHeat <= aeroHeatEfficiency) {
            // count all weapons equal, adjusting for rear-firing and excessive
            // ammo
            for (ArrayList<Object> weaponValues : heatBVs) {
                bvText.append(startRow);
                bvText.append(startColumn);

                bvText.append(weaponValues.get(2));
                weaponBV += (Double) weaponValues.get(0);
                bvText.append(endColumn);
                bvText.append(startColumn);
                bvText.append(endColumn);
                bvText.append(startColumn);

                bvText.append(weaponValues.get(0));
                bvText.append(endColumn);
                bvText.append(endRow);
            }
        } else {
            // this will count heat-generating weapons at full modified BV until
            // heatefficiency is reached or passed with one weapon

            // sort the heat-using weapons by modified BV
            Collections.sort(heatBVs, (obj1, obj2) -> {
                // first element in the the ArrayList is BV, second is heat
                // if same BV, lower heat first
                if (obj1.get(0).equals(obj2.get(0))) {
                    return (int) Math.ceil((Double) obj1.get(1) - (Double) obj2.get(1));
                }
                // higher BV first
                return (int) Math.ceil((Double) obj2.get(0) - (Double) obj1.get(0));
            });

            // count heat-generating weapons at full modified BV until
            // heatefficiency is reached or
            // passed with one weapon
            double heatAdded = 0;
            for (ArrayList<Object> weaponValues : heatBVs) {
                bvText.append(startRow);
                bvText.append(startColumn);

                bvText.append(weaponValues.get(2));
                bvText.append(endColumn);
                bvText.append(startColumn);

                double dBV = (Double) weaponValues.get(0);
                if (heatAdded >= aeroHeatEfficiency) {
                    dBV /= 2;
                }
                if (heatAdded >= aeroHeatEfficiency) {
                    bvText.append("Heat efficiency reached, half BV");
                }
                heatAdded += (Double) weaponValues.get(1);
                weaponBV += dBV;
                bvText.append(endColumn);
                bvText.append(startColumn);
                bvText.append(dBV);
                bvText.append(endColumn);
                bvText.append(endRow);
                bvText.append(startRow);
                bvText.append(startColumn);
                bvText.append("Heat count: " + heatAdded);
                bvText.append(endColumn);
                bvText.append(startColumn);
                bvText.append(endColumn);
                bvText.append(startColumn);
                bvText.append(endColumn);
                bvText.append(endRow);
            }
        }
        bvText.append(startRow);
        bvText.append(startColumn);
        bvText.append(endColumn);
        bvText.append(startColumn);
        bvText.append(endColumn);
        bvText.append(startColumn);

        bvText.append("-------------");
        bvText.append(endColumn);
        bvText.append(endRow);

        bvText.append(startRow);
        bvText.append(startColumn);

        bvText.append("Total Weapons BV Adjusted For Heat:");
        bvText.append(endColumn);
        bvText.append(startColumn);
        bvText.append(endColumn);
        bvText.append(startColumn);
        bvText.append(weaponBV);
        bvText.append(endColumn);
        bvText.append(endRow);

        bvText.append(startRow);
        bvText.append(startColumn);

        bvText.append("Misc Offensive Equipment: ");
        bvText.append(endColumn);
        bvText.append(startColumn);
        bvText.append(endColumn);
        bvText.append(startColumn);
        bvText.append(endColumn);
        bvText.append(endRow);

        // add offensive misc. equipment BV
        double oEquipmentBV = 0;
        for (Mounted mounted : getMisc()) {
            MiscType mtype = (MiscType) mounted.getType();

            // don't count destroyed equipment
            if (mounted.isDestroyed()) {
                continue;
            }

            if (mtype.hasFlag(MiscType.F_TARGCOMP) || mtype.hasFlag(MiscType.F_ECM) || mtype.hasFlag(MiscType.F_BAP)
                    || mtype.hasFlag(MiscType.F_CHAFF_POD)) {
                continue;
            }
            double bv = mtype.getBV(this);
            if (bv > 0) {
                bvText.append(startRow);
                bvText.append(startColumn);

                bvText.append(mtype.getName());
                bvText.append(endColumn);
                bvText.append(startColumn);
                bvText.append(endColumn);
                bvText.append(startColumn);
                bvText.append(bv);
                bvText.append(endColumn);
                bvText.append(endRow);
            }
            oEquipmentBV += bv;
        }
        bvText.append(startRow);
        bvText.append(startColumn);

        bvText.append("Total Misc Offensive Equipment BV: ");
        bvText.append(endColumn);
        bvText.append(startColumn);
        bvText.append(endColumn);
        bvText.append(startColumn);
        bvText.append(oEquipmentBV);
        bvText.append(endColumn);
        bvText.append(endRow);

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
            if (mounted.getUsableShotsLeft() == 0) {
                continue;
            }

            // don't count AMS, it's defensive
            if (atype.getAmmoType() == AmmoType.T_AMS) {
                continue;
            }
            // don't count screen launchers, they are defensive
            if (atype.getAmmoType() == AmmoType.T_SCREEN_LAUNCHER) {
                continue;
            }

            // don't count oneshot ammo, it's considered part of the launcher.
            if (mounted.getLocation() == Entity.LOC_NONE) {
                // assumption: ammo without a location is for a oneshot weapon
                continue;
            }
            // semiguided or homing ammo might count double
            if ((atype.getMunitionType() == AmmoType.M_SEMIGUIDED) || (atype.getMunitionType() == AmmoType.M_HOMING)) {
                IPlayer tmpP = getOwner();

                if (tmpP != null) {
                    // Okay, actually check for friendly TAG.
                    if (tmpP.hasTAG()) {
                        tagBV += atype.getBV(this);
                    } else if ((tmpP.getTeam() != IPlayer.TEAM_NONE) && (game != null)) {
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

        bvText.append(startRow);
        bvText.append(startColumn);

        bvText.append("Total Ammo BV: ");
        bvText.append(endColumn);
        bvText.append(startColumn);
        bvText.append(endColumn);
        bvText.append(startColumn);

        bvText.append(ammoBV);
        bvText.append(endColumn);
        bvText.append(endRow);

        // adjust further for speed factor
        double speedFactor = Math.pow(1 + (((double) getRunMP() - 5) / 10), 1.2);
        speedFactor = Math.round(speedFactor * 100) / 100.0;

        bvText.append(startRow);
        bvText.append(startColumn);

        bvText.append("Final Speed Factor: ");
        bvText.append(endColumn);
        bvText.append(startColumn);
        bvText.append(endColumn);
        bvText.append(startColumn);
        bvText.append(speedFactor);
        bvText.append(endColumn);
        bvText.append(endRow);

        obv = weaponBV * speedFactor;

        bvText.append(startRow);
        bvText.append(startColumn);

        bvText.append("Weapons BV * Speed Factor ");
        bvText.append(endColumn);
        bvText.append(startColumn);

        bvText.append(weaponBV);
        bvText.append(" * ");
        bvText.append(speedFactor);
        bvText.append(endColumn);
        bvText.append(startColumn);
        bvText.append(" = ");
        bvText.append(obv);
        bvText.append(endColumn);
        bvText.append(endRow);

        bvText.append(startRow);
        bvText.append(startColumn);

        if (useGeometricMeanBV()) {
            bvText.append("2 * sqrt(Offensive BV * Defensive BV");
        } else {
            bvText.append("Offensive BV + Defensive BV");
        }
        bvText.append(endColumn);
        bvText.append(startColumn);

        double finalBV;
        if (useGeometricMeanBV()) {
            finalBV = 2 * Math.sqrt(obv * dbv);
            if (finalBV == 0) {
                finalBV = dbv + obv;
            }
            bvText.append("2 * sqrt(");
            bvText.append(obv);
            bvText.append(" + ");
            bvText.append(dbv);
            bvText.append(")");
        } else {
            finalBV = dbv + obv;
            bvText.append(dbv);
            bvText.append(" + ");
            bvText.append(obv);
        }
        double totalBV = finalBV;

        bvText.append(endColumn);
        bvText.append(startColumn);
        bvText.append(" = ");
        bvText.append(finalBV);
        bvText.append(endColumn);
        bvText.append(endRow);

        bvText.append(startRow);
        bvText.append(startColumn);

        double cockpitMod = 1;
        if (getCockpitType() == Aero.COCKPIT_SMALL) {
            cockpitMod = 0.95;
            finalBV *= cockpitMod;
        } else if (hasWorkingMisc(MiscType.F_DRONE_OPERATING_SYSTEM)) {
            finalBV *= 0.95;
        }
        finalBV = Math.round(finalBV);
        bvText.append("Total BV * Cockpit Modifier");
        bvText.append(endColumn);
        bvText.append(startColumn);
        bvText.append(totalBV);
        bvText.append(" * ");
        bvText.append(cockpitMod);
        bvText.append(endColumn);
        bvText.append(startColumn);
        bvText.append(" = ");
        bvText.append(finalBV);
        bvText.append(endColumn);
        bvText.append(endRow);

        bvText.append(startRow);
        bvText.append(startColumn);
        bvText.append(endColumn);
        bvText.append(startColumn);
        bvText.append(endColumn);
        bvText.append(startColumn);

        bvText.append("-------------");
        bvText.append(endColumn);
        bvText.append(endRow);

        bvText.append(startRow);
        bvText.append(startColumn);
        bvText.append("Final BV");
        bvText.append(endColumn);
        bvText.append(startColumn);
        bvText.append(endColumn);
        bvText.append(startColumn);

        bvText.append(finalBV);
        bvText.append(endColumn);
        bvText.append(endRow);

        // We need to consider external stores. Per TechManual Pg314,
        // TM BV Errata Pg23, the external stores BV is added to the
        // units base BV
        boolean hasBombs = false;
        double bombBV = 0;
        for (int bombType = 0; bombType < BombType.B_NUM; bombType++) {
            BombType bomb = BombType.createBombByType(bombType);
            bombBV += bomb.bv * bombChoices[bombType];
            if (bombChoices[bombType] > 0) {
                hasBombs = true;
            }
        }
        finalBV += bombBV;
        if (hasBombs) {
            bvText.append(startRow);
            bvText.append(startColumn);
            bvText.append("External Stores BV");
            bvText.append(endColumn);
            bvText.append(startColumn);
            bvText.append(endColumn);
            bvText.append(startColumn);

            bvText.append(bombBV);
            bvText.append(endColumn);
            bvText.append(endRow);

            bvText.append(startRow);
            bvText.append(startColumn);
            bvText.append(endColumn);
            bvText.append(startColumn);
            bvText.append(endColumn);
            bvText.append(startColumn);

            bvText.append("-------------");
            bvText.append(endColumn);
            bvText.append(endRow);

            bvText.append(startRow);
            bvText.append(startColumn);
            bvText.append("Final BV");
            bvText.append(endColumn);
            bvText.append(startColumn);
            bvText.append(endColumn);
            bvText.append(startColumn);

            bvText.append(finalBV);
            bvText.append(endColumn);
            bvText.append(endRow);
        }

        bvText.append(endTable);
        bvText.append("</BODY></HTML>");

        // we get extra bv from some stuff
        double xbv = 0.0;
        // extra BV for semi-guided lrm when TAG in our team
        xbv += tagBV;
        // extra from c3 networks. a valid network requires at least 2 members
        // some hackery and magic numbers here. could be better
        // also, each 'has' loops through all equipment. inefficient to do it 3
        // times
        if (!ignoreC3 && (game != null)) {

            xbv += getExtraC3BV((int) Math.round(finalBV));
        }
        finalBV += xbv;

        // and then factor in pilot
        double pilotFactor = 1;
        if (!ignorePilot) {
            pilotFactor = getCrew().getBVSkillMultiplier(game);
        }

        int retVal = (int) Math.round((finalBV) * pilotFactor);

        return retVal;
    }

    public double getBVTypeModifier() {
        return 1.2;
    }

    @Override
    public PilotingRollData addEntityBonuses(PilotingRollData prd) {
        // this is a control roll. Affected by:
        // avionics damage
        // pilot damage
        // current velocity
        int avihits = getAvionicsHits();
        int pilothits = getCrew().getHits();

        if ((avihits > 0) && (avihits < 3)) {
            prd.addModifier(avihits, "Avionics Damage");
        }

        // this should probably be replaced with some kind of AVI_DESTROYED
        // boolean
        if (avihits >= 3) {
            prd.addModifier(5, "Avionics Destroyed");
        }

        if (pilothits > 0) {
            prd.addModifier(pilothits, "Pilot Hits");
        }

        // movement effects
        // some question as to whether "above safe thrust" applies to thrust or
        // velocity
        // I will treat it as thrust until it is resolved
        if (moved == EntityMovementType.MOVE_OVER_THRUST) {
            prd.addModifier(+1, "Used more than safe thrust");
        }
        int vel = getCurrentVelocity();
        int vmod = vel - (2 * getWalkMP());
        if (vmod > 0) {
            prd.addModifier(vmod, "Velocity greater than 2x safe thrust");
        }

        int atmoCond = game.getPlanetaryConditions().getAtmosphere();
        // add in atmospheric effects later
        if (!(game.getBoard().inSpace() || (atmoCond == PlanetaryConditions.ATMO_VACUUM)) && isAirborne()) {
            prd.addModifier(+2, "Atmospheric operations");

            // check type
            if (this instanceof Dropship) {
                if (isSpheroid()) {
                    prd.addModifier(+1, "spheroid dropship");
                } else {
                    prd.addModifier(0, "aerodyne dropship");
                }
            } else {
                prd.addModifier(-1, "fighter/small craft");
            }
        }

        // life support (only applicable to non-ASFs
        if (!hasLifeSupport()) {
            prd.addModifier(+2, "No life support");
        }

        if (hasModularArmor()) {
            prd.addModifier(1, "Modular Armor");
        }
        // VDNI bonus?
        if (getCrew().getOptions().booleanOption(OptionsConstants.MD_VDNI)
                && !getCrew().getOptions().booleanOption(OptionsConstants.MD_BVDNI)) {
            prd.addModifier(-1, "VDNI");
        }

        // Small/torso-mounted cockpit penalty?
        if ((getCockpitType() == Aero.COCKPIT_SMALL)
                && !getCrew().getOptions().booleanOption(OptionsConstants.MD_BVDNI)) {
            prd.addModifier(1, "Small Cockpit");
        }

        // quirks?
        if (hasQuirk(OptionsConstants.QUIRK_POS_ATMO_FLYER) && !game.getBoard().inSpace()) {
            prd.addModifier(-1, "atmospheric flyer");
        }
        if (hasQuirk(OptionsConstants.QUIRK_NEG_ATMO_INSTABILITY) && !game.getBoard().inSpace()) {
            prd.addModifier(+1, "atmospheric flight instability");
        }
        if (hasQuirk(OptionsConstants.QUIRK_NEG_CRAMPED_COCKPIT)) {
            prd.addModifier(1, "cramped cockpit");
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

        if (((getEntityType() & Entity.ETYPE_DROPSHIP) == 0) || ((getEntityType() & Entity.ETYPE_SMALL_CRAFT) == 0)
                || ((getEntityType() & Entity.ETYPE_FIGHTER_SQUADRON) == 0)
                || ((getEntityType() & Entity.ETYPE_JUMPSHIP) == 0)
                || ((getEntityType() & Entity.ETYPE_SPACE_STATION) == 0)) {
            r = new Report(7036);
        } else {
            r = new Report(7030);
        }
        r.type = Report.PUBLIC;
        r.newlines = 0;
        vDesc.addElement(r);
        vDesc.addAll(getCrew().getDescVector(false));
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
                if (this instanceof FighterSquadron) {
                    r = new Report(7076, Report.PUBLIC);
                } else {
                    r = new Report(7073, Report.PUBLIC);
                }
            }
            vDesc.addElement(r);
        } else if (getCrew().isEjected()) {
            r = new Report(7074, Report.PUBLIC);
            vDesc.addElement(r);
        }
        r.newlines = 2;

        return vDesc;
    }

    @Override
    public int[] getNoOfSlots() {
        return NUM_OF_SLOTS;
    }

    /**
     * Tanks don't have MASC
     */
    @Override
    public int getRunMPwithoutMASC(boolean gravity, boolean ignoreheat, boolean ignoremodulararmor) {
        return getRunMP(gravity, ignoreheat, ignoremodulararmor);
    }

    @Override
    public int getRunMP(boolean gravity, boolean ignoreheat, boolean ignoremodulararmor) {
        // if aeros are on the ground, they can only move at cruising speed
        if (!isAirborne()) {
            return getWalkMP(gravity, ignoreheat, ignoremodulararmor);
        }
        return super.getRunMP(gravity, ignoreheat, ignoremodulararmor);
    }

    @Override
    public int getHeatCapacity(boolean includeRadicalHeatSink) {
        int capacity = (getHeatSinks() * (getHeatType() + 1));
        if (includeRadicalHeatSink && hasWorkingMisc(MiscType.F_RADICAL_HEATSINK)) {
            capacity += Math.ceil(getHeatSinks() * 0.4);
        }
        return capacity;
    }

    // If the aero is in the water, it is dead so no worries
    @Override
    public int getHeatCapacityWithWater() {
        return getHeatCapacity(false);
    }

    @Override
    public int getEngineCritHeat() {
        // Engine hits cause excess heat for fighters, TW pg 240
        if (!((this instanceof SmallCraft) || (this instanceof Jumpship))) {
            return 2 * getEngineHits();
        } else {
            return 0;
        }
    }

    @Override
    public void autoSetInternal() {
        // should be no internals because only one SI
        // It doesn't seem to be screwing anything up yet.
        // Need to figure out how destruction of entity is determined
        int nInternal = (int) Math.ceil(weight / 10.0);
        nInternal = 0;
        // I need to look at safe thrust as well at some point

        for (int x = 0; x < locations(); x++) {
            initializeInternal(nInternal, x);
        }
    }

    // initialize the Damage threshold
    public void autoSetThresh() {
        for (int x = 0; x < locations(); x++) {
            initializeThresh(x);
        }
    }

    public void setThresh(int val, int loc) {
        damThresh[loc] = val;
    }

    public void initializeThresh(int loc) {
        int nThresh = (int) Math.ceil(getArmor(loc) / 10.0);
        setThresh(nThresh, loc);
    }

    @Override
    public int getThresh(int loc) {
        if (isCapitalFighter()) {
            if ((null != game) && game.getOptions().booleanOption(OptionsConstants.ADVAERORULES_AERO_SANITY)) {
                if (game.getOptions().booleanOption(OptionsConstants.ADVAERORULES_VARIABLE_DAMAGE_THRESH)) {
                    return (int) Math.round(getCapArmor() / 40.0) + 1;
                } else {
                    return (int) Math.round(getCap0Armor() / 40.0) + 1;
                }
            } else {
                return 2;
            }
        }
        return damThresh[loc];
    }

    /**
     * Determine if the unit can be repaired, or only harvested for spares.
     *
     * @return A <code>boolean</code> that is <code>true</code> if the unit can
     *         be repaired (given enough time and parts); if this value is
     *         <code>false</code>, the unit is only a source of spares.
     * @see Entity#isSalvage()
     */
    @Override
    public boolean isRepairable() {
        return true; // deal with this later
    }

    @Override
    public boolean canCharge() {
        // ramming is resolved differently than chargin
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

    /**
     * @return suspension factor of vehicle
     */
    // Doesn't really do anything so just return 0
    public int getSuspensionFactor() {
        return 0;
    }

    /**
     * There is a mistake in some of the AT2r costs for some reason they added
     * ammo twice for a lot of the level 2 designs, leading to costs that are
     * too high
     */
    @Override
    public double getCost(boolean ignoreAmmo) {

        double cost = 0;

        // add in cockpit
        cost += 200000 + 50000 + (2000 * weight);

        // Structural integrity
        cost += 50000 * getSI();

        // additional flight systems (attitude thruster and landing gear)
        cost += 25000 + (10 * getWeight());

        // engine
        if (hasEngine()) {
            cost += (getEngine().getBaseCost() * getEngine().getRating() * weight) / 75.0;
        }

        // fuel tanks
        cost += (200 * getFuel()) / 80.0;

        // armor
        if (hasPatchworkArmor()) {
            for (int loc = 0; loc < locations(); loc++) {
                cost += getArmorWeight(loc) * EquipmentType.getArmorCost(armorType[loc]);
            }

        } else {
            cost += getArmorWeight() * EquipmentType.getArmorCost(armorType[0]);
        }

        // heat sinks
        int sinkCost = 2000 + (4000 * getHeatType());// == HEAT_DOUBLE ? 6000:
        // 2000;
        cost += sinkCost * getHeatSinks();

        // weapons
        cost += getWeaponsAndEquipmentCost(ignoreAmmo);

        // omni multiplier
        double omniMultiplier = 1;
        if (isOmni()) {
            omniMultiplier = 1.25f;
        }

        double weightMultiplier = 1 + (weight / 200f);

        return Math.round(cost * omniMultiplier * weightMultiplier);

    }

    @Override
    public boolean doomedInExtremeTemp() {
        return false;
    }

    @Override
    public boolean doomedInVacuum() {
        return false;
    }

    @Override
    public boolean doomedOnGround() {
        return !game.getOptions().booleanOption(OptionsConstants.ADVAERORULES_AERO_GROUND_MOVE);
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
    public boolean canGoHullDown() {
        return false;
    }

    /*
     * public void addMovementDamage(int level) { movementDamage += level; }
     */

    @Override
    public void setEngine(Engine e) {
        super.setEngine(e);
        if (hasEngine() && getEngine().engineValid) {
            setOriginalWalkMP(calculateWalk());
        }
    }

    /**
     * Returns the percent of the SI remaining
     */
    @Override
    public double getInternalRemainingPercent() {
        return ((double) getSI() / (double) get0SI());
    }

    protected int calculateWalk() {
        if (!hasEngine()) {
            return 0;
        }
        if (isPrimitive()) {
            double rating = getEngine().getRating();
            rating /= 1.2;
            if ((rating % 5) != 0) {
                return (int) (((rating - (rating % 5)) + 5) / (int) weight) + 2;
            }
            return (int) (rating / (int) weight) + 2;
        }
        return (getEngine().getRating() / (int) weight) + 2;
    }

    @Override
    public boolean isNuclearHardened() {
        return true;
    }

    @Override
    public void addEquipment(Mounted mounted, int loc, boolean rearMounted) throws LocationFullException {
        if (getEquipmentNum(mounted) == -1) {
            super.addEquipment(mounted, loc, rearMounted);
        }
        // Add the piece equipment to our slots.
        addCritical(loc, new CriticalSlot(mounted));
    }

    /**
     * get the type of critical caused by a critical roll, taking account of
     * existing damage
     *
     * @param roll
     *            the final dice roll
     * @param target
     *            the hit location
     * @return a critical type
     */
    public int getCriticalEffect(int roll, int target) {
        // just grab the latest potential crit
        if (roll < target) {
            return CRIT_NONE;
        }

        int critical = getPotCrit();
        return critical;
    }

    /**
    
     */
    @Override
    public void setOmni(boolean omni) {

        // Perform the superclass' action.
        super.setOmni(omni);

    }

    /**
     * Adds clan CASE in every location
     */
    public void addClanCase() {
        boolean explosiveFound = false;
        EquipmentType clCase = EquipmentType.get("CLCASE");
        for (int i = 0; i < locations(); i++) {
            // Ignore wings location: it's not a valid loc to put equipment in
            if (i == LOC_WINGS) {
                continue;
            }
            explosiveFound = false;
            for (Mounted m : getEquipment()) {
                if (m.getType().isExplosive(m) && (m.getLocation() == i)) {
                    explosiveFound = true;
                }
            }
            if (explosiveFound) {
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
     *
     * @return
     */
    @Override
    public boolean hasCase() {

        boolean hasCase = false;

        for (int x = 0; x < locations(); x++) {
            if (!hasCase) {
                hasCase = locationHasCase(x);
            }
        }
        return hasCase;
    }

    /**
     * Used to determine net velocity of ramming attack
     *
     */
    @Override
    public int sideTableRam(Coords src) {
        int side = super.sideTableRam(src);
        if (game.useVectorMove() && game.getBoard().inSpace()) {
            int newside = chooseSideRam(src);
            if (newside != -1) {
                side = newside;
            }
        }
        return side;

    }

    public int chooseSideRam(Coords src) {
        // loop through directions and if we have a non-zero vector, then
        // compute
        // the targetsidetable. If we come to a higher vector, then replace. If
        // we come to an equal vector then take it if it is better
        int thrust = 0;
        int high = -1;
        int side = -1;
        for (int dir = 0; dir < 6; dir++) {
            thrust = getVector(dir);
            if (thrust == 0) {
                continue;
            }

            if (thrust > high) {
                high = thrust;
                side = sideTableRam(src, dir);
            }

            // what if they tie
            if (thrust == high) {
                int newside = sideTableRam(src, dir);
                // choose the better
                if (newside > side) {
                    newside = side;
                }
                // that should be the only case, because it can't shift you from
                // front
                // to aft or vice-versa
            }

        }
        return side;
    }

    public int getMaxEngineHits() {
        return 3;
    }

    @Override
    public int getMaxElevationChange() {
        if (isAirborne()) {
            return 999;
        }
        return 1;
    }

    /**
     * Determine if this unit has an active and working stealth system. (stealth
     * can be active and not working when under ECCM)
     * <p/>
     * Sub-classes are encouraged to override this method.
     *
     * @return <code>true</code> if this unit has a stealth system that is
     *         currently active, <code>false</code> if there is no stealth
     *         system or if it is inactive.
     */
    @Override
    public boolean isStealthActive() {
        // Try to find a Mek Stealth system.
        for (Mounted mEquip : getMisc()) {
            MiscType mtype = (MiscType) mEquip.getType();
            if (mtype.hasFlag(MiscType.F_STEALTH)) {

                if (mEquip.curMode().equals("On") && hasActiveECM()) {
                    // Return true if the mode is "On" and ECM is working
                    return true;
                }
            }
        }
        // No Mek Stealth or system inactive. Return false.
        return false;
    }

    /**
     * Determine if this unit has an active and working stealth system. (stealth
     * can be active and not working when under ECCM)
     * <p/>
     * Sub-classes are encouraged to override this method.
     *
     * @return <code>true</code> if this unit has a stealth system that is
     *         currently active, <code>false</code> if there is no stealth
     *         system or if it is inactive.
     */
    @Override
    public boolean isStealthOn() {
        // Try to find a Mek Stealth system.
        for (Mounted mEquip : getMisc()) {
            MiscType mtype = (MiscType) mEquip.getType();
            if (mtype.hasFlag(MiscType.F_STEALTH)) {
                if (mEquip.curMode().equals("On")) {
                    // Return true if the mode is "On"
                    return true;
                }
            }
        }
        // No Mek Stealth or system inactive. Return false.
        return false;
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
     *            - entity making the attack
     * @return a <code>TargetRoll</code> value that contains the stealth
     *         modifier for the given range.
     */
    @Override
    public TargetRoll getStealthModifier(int range, Entity ae) {
        TargetRoll result = null;

        boolean isInfantry = (ae instanceof Infantry) && !(ae instanceof BattleArmor);
        // Stealth or null sig must be active.
        if (!isStealthActive()) {
            result = new TargetRoll(0, "stealth not active");
        }
        // Determine the modifier based upon the range.
        // Infantry do not ignore Chameleon LPS!!!
        else {
            switch (range) {
            case RangeType.RANGE_MINIMUM:
            case RangeType.RANGE_SHORT:
                if (!isInfantry) {
                    result = new TargetRoll(0, "stealth");
                } else {
                    result = new TargetRoll(0, "infantry ignore stealth");
                }
                break;
            case RangeType.RANGE_MEDIUM:
                if (!isInfantry) {
                    result = new TargetRoll(1, "stealth");
                } else {
                    result = new TargetRoll(0, "infantry ignore stealth");
                }
                break;
            case RangeType.RANGE_LONG:
            case RangeType.RANGE_EXTREME:
            case RangeType.RANGE_LOS:
                if (!isInfantry) {
                    result = new TargetRoll(2, "stealth");
                } else {
                    result = new TargetRoll(0, "infantry ignore stealth");
                }
                break;
            case RangeType.RANGE_OUT:
                break;
            default:
                throw new IllegalArgumentException("Unknown range constant: " + range);
            }
        }

        // Return the result.
        return result;

    } // End public TargetRoll getStealthModifier( char )

    @Override
    public void setArmorType(int armType) {
        setArmorType(armType, true);
    }

    public void setArmorType(int armType, boolean addMount) {
        super.setArmorType(armType);
        if ((armType == EquipmentType.T_ARMOR_STEALTH_VEHICLE) && addMount) {
            try {
                this.addEquipment(
                        EquipmentType.get(EquipmentType.getArmorTypeName(EquipmentType.T_ARMOR_STEALTH_VEHICLE, false)),
                        LOC_AFT);
            } catch (LocationFullException e) {
                // this should never happen
            }
        }
    }

    @Override
    public boolean isLocationProhibited(Coords c, int currElevation) {
        IHex hex = game.getBoard().getHex(c);
        if (isAirborne()) {
            if (hex.containsTerrain(Terrains.IMPASSABLE)) {
                return true;
            }
            return false;
        }

        // Additional restrictions for hidden units
        if (isHidden()) {
            // Can't deploy in paved hexes
            if (hex.containsTerrain(Terrains.PAVEMENT) || hex.containsTerrain(Terrains.ROAD)) {
                return true;
            }
            // Can't deploy on a bridge
            if ((hex.terrainLevel(Terrains.BRIDGE_ELEV) == currElevation) && hex.containsTerrain(Terrains.BRIDGE)) {
                return true;
            }
            // Can't deploy on the surface of water
            if (hex.containsTerrain(Terrains.WATER) && (currElevation == 0)) {
                return true;
            }
        }

        // grounded aeros have the same prohibitions as wheeled tanks
        return hex.containsTerrain(Terrains.WOODS) || hex.containsTerrain(Terrains.ROUGH)
                || ((hex.terrainLevel(Terrains.WATER) > 0) && !hex.containsTerrain(Terrains.ICE))
                || hex.containsTerrain(Terrains.RUBBLE) || hex.containsTerrain(Terrains.MAGMA)
                || hex.containsTerrain(Terrains.JUNGLE) || (hex.terrainLevel(Terrains.SNOW) > 1)
                || (hex.terrainLevel(Terrains.GEYSER) == 2);
    }

    @Override
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

    @Override
    public int getStraightMoves() {
        return straightMoves;
    }

    @Override
    public void setStraightMoves(int i) {
        straightMoves = i;
    }

    @Override
    public boolean isVSTOL() {
        return vstol;
    }

    @Override
    public boolean isSTOL() {
        return false;
    }

    public void setVSTOL(boolean b) {
        vstol = b;
    }

    @Override
    public boolean didFailManeuver() {
        return failedManeuver;
    }

    @Override
    public void setFailedManeuver(boolean b) {
        failedManeuver = b;
    }

    @Override
    public void setAccDecNow(boolean b) {
        accDecNow = b;
    }

    @Override
    public boolean didAccDecNow() {
        return accDecNow;
    }

    @Override
    public void setGameOptions() {
        super.setGameOptions();

        for (Mounted mounted : getWeaponList()) {
            if ((mounted.getType() instanceof EnergyWeapon)
                    && (((WeaponType) mounted.getType()).getAmmoType() == AmmoType.T_NA) && (game != null)
                    && game.getOptions().booleanOption(OptionsConstants.ADVCOMBAT_TACOPS_ENERGY_WEAPONS)) {

                ArrayList<String> modes = new ArrayList<String>();
                String[] stringArray = {};
                int damage = ((WeaponType) mounted.getType()).getDamage();

                if (damage == WeaponType.DAMAGE_VARIABLE) {
                    damage = ((WeaponType) mounted.getType()).damageShort;
                }

                for (; damage >= 0; damage--) {
                    modes.add("Damage " + damage);
                }
                if (((WeaponType) mounted.getType()).hasFlag(WeaponType.F_FLAMER)) {
                    modes.add("Heat");
                }
                ((WeaponType) mounted.getType()).setModes(modes.toArray(stringArray));
            }

        }

    }

    /*
     * (non-Javadoc)
     * 
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
        if (type == COCKPIT_COMMAND_CONSOLE) {
            setCrew(new Crew(CrewType.COMMAND_CONSOLE));
        } else {
            setCrew(new Crew(CrewType.SINGLE));
        }
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
    public boolean hasCommandConsoleBonus() {
        return getCockpitType() == COCKPIT_COMMAND_CONSOLE && getCrew().hasActiveCommandConsole()
                && getWeightClass() >= EntityWeightClass.WEIGHT_HEAVY;
    }

    @Override
    public double getArmorRemainingPercent() {
        int armor0 = getTotalOArmor();
        int armor = getTotalArmor();
        if (isCapitalFighter()) {
            armor0 = getCap0Armor();
            armor = getCapArmor();
        }
        if (armor0 == 0) {
            return IArmorState.ARMOR_NA;
        }
        return ((double) armor / (double) armor0);
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
        switch (loc) {
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
    @Override
    public int getClusterMods() {
        return -1 * (getFCSHits() + getSensorHits());
    }

    /**
     * What's the range of the ECM equipment?
     *
     * @return the <code>int</code> range of this unit's ECM. This value will be
     *         <code>Entity.NONE</code> if no ECM is active.
     */
    @Override
    public int getECMRange() {
        if (!game.getOptions().booleanOption(OptionsConstants.ADVAERORULES_STRATOPS_ECM)
                || !game.getBoard().inSpace()) {
            return super.getECMRange();
        }
        return Math.min(super.getECMRange(), 0);
    }

    /**
     * @return the strength of the ECCM field this unit emits
     */
    @Override
    public double getECCMStrength() {
        if (!game.getOptions().booleanOption(OptionsConstants.ADVAERORULES_STRATOPS_ECM)
                || !game.getBoard().inSpace()) {
            return super.getECCMStrength();
        }
        if (hasActiveECCM()) {
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
        return getCrew().getPiloting() + getSensorHits() + getCICHits() + getFCSHits();
    }

    public int getECCMBonus() {
        return Math.max(0, eccmRoll - getECCMTarget());
    }

    /**
     * @return is the crew of this vessel protected from gravitational effects,
     *         see StratOps, pg. 36
     */
    public boolean isCrewProtected() {
        return true;
    }

    public int getGravSecondaryThreshold() {
        int thresh = 6;
        if (isCrewProtected()) {
            thresh = 12;
        }
        // TODO: clan phenotypes
        return thresh;
    }

    public int getGravPrimaryThreshold() {
        int thresh = 12;
        if (isCrewProtected()) {
            thresh = 22;
        }
        // TODO: clan phenotypes
        return thresh;
    }

    /**
     * Determines if this object can accept the given unit. The unit may not be
     * of the appropriate type or there may be no room for the unit.
     *
     * @param unit
     *            - the <code>Entity</code> to be loaded.
     * @return <code>true</code> if the unit can be loaded, <code>false</code>
     *         otherwise.
     */
    @Override
    public boolean canLoad(Entity unit, boolean checkFalse) {
        // capital fighters can load other capital fighters (becoming squadrons)
        // but not in the deployment phase
        if (isCapitalFighter() && !unit.isEnemyOf(this) && unit.isCapitalFighter() && (getId() != unit.getId())
                && (game.getPhase() != IGame.Phase.PHASE_DEPLOYMENT)) {
            return true;
        }

        return super.canLoad(unit, checkFalse);
    }

    @Override
    public Map<String, Integer> getWeaponGroups() {
        return weaponGroups;
    }

    /**
     * Iterate through current weapons and count the number in each capital
     * fighter location.
     *
     * @return A map with keys in the format "weaponName:loc", with the number
     *         of weapons of that type in that location as the value.
     */
    @Override
    public Map<String, Integer> groupWeaponsByLocation() {
        Map<String, Integer> groups = new HashMap<>();
        for (Mounted mounted : getTotalWeaponList()) {
            int loc = mounted.getLocation();
            if (isFighter() && ((loc == Aero.LOC_RWING) || (loc == Aero.LOC_LWING))) {
                loc = Aero.LOC_WINGS;
            }
            if (mounted.isRearMounted()) {
                loc = Aero.LOC_AFT;
            }
            String key = mounted.getType().getInternalName() + ":" + loc;
            if (null == groups.get(key)) {
                groups.put(key, mounted.getNWeapons());
            } else {
                groups.put(key, groups.get(key) + mounted.getNWeapons());
            }
        }
        return groups;
    }

    /**
     * In cases where another unit occupies the same hex, determine if this Aero
     * should be moved back a hex for targeting purposes
     *
     * @param other
     * @return
     */
    public boolean shouldMoveBackHex(Aero other) {
        if (null == getPosition()) {
            return false;
        }
        if (null == other.getPosition()) {
            return false;
        }
        if (!getPosition().equals(other.getPosition())) {
            return false;
        }
        int type = UnitType.determineUnitTypeCode(this);
        int otherType = UnitType.determineUnitTypeCode(other);
        int vel = getCurrentVelocity();
        int otherVel = other.getCurrentVelocity();
        if (type > otherType) {
            return false;
        } else if (type < otherType) {
            return true;
        }
        // if we are still here then type is the same so compare velocity
        if (vel < otherVel) {
            return false;
        } else if (vel > otherVel) {
            return true;
        }
        // if we are still here then type and velocity same, so roll for it
        if (getWhoFirst() < other.getWhoFirst()) {
            return false;
        }
        return true;
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

    /**
     * see {@link Entity#getForwardArc()}
     */
    @Override
    public int getForwardArc() {
        return Compute.ARC_NOSE;
    }

    /**
     * see {@link Entity#getRearArc()}
     */
    @Override
    public int getRearArc() {
        return Compute.ARC_AFT;
    }

    @Override
    public int getAltLoss() {
        return altLoss;
    }

    @Override
    public void setAltLoss(int i) {
        altLoss = i;
    }

    @Override
    public void resetAltLoss() {
        altLoss = 0;
    }

    @Override
    public int getAltLossThisRound() {
        return altLossThisRound;
    }

    @Override
    public void setAltLossThisRound(int i) {
        altLossThisRound = i;
    }

    @Override
    public void resetAltLossThisRound() {
        altLossThisRound = 0;
    }

    @Override
    public int getElevation() {
        if ((game != null) && game.getBoard().inSpace()) {
            return 0;
        }
        // Altitude is not the same as elevation. If an aero is at 0 altitude,
        // then it is
        // grounded and uses elevation normally. Otherwise, just set elevation
        // to a very
        // large number so that a flying aero won't interact with the ground
        // maps in any way
        if (isAirborne()) {
            return 999;
        }
        return super.getElevation();
    }

    @Override
    public boolean canGoDown() {
        return canGoDown(altitude, getPosition());
    }

    @Override
    public void setAlphaStrikeMovement(Map<String, Integer> moves) {
        moves.put(getMovementModeAsBattleForceString(), getWalkMP());
    }

    @Override
    public int getBattleForceArmorPoints() {
        if (isCapitalFighter()) {
            return (int) Math.round(getCapArmor() / 3.0);
        }
        return super.getBattleForceArmorPoints();
    }

    @Override
    public String getBattleForceDamageThresholdString() {
        return "-" + (int) Math.ceil(getBattleForceArmorPoints() / 10.0);
    }

    @Override
    public int getBattleForceStructurePoints() {
        return (int) Math.ceil(getSI() * 0.50);
    }

    @Override
    public int getNumBattleForceWeaponsLocations() {
        return 2;
    }

    @Override
    public double getBattleForceLocationMultiplier(int index, int location, boolean rearMounted) {
        if ((index == 0 && location != LOC_AFT && !rearMounted)
                || (index == 1 && (location == LOC_AFT || rearMounted))) {
            return 1.0;
        }
        return 0;
    }

    @Override
    public String getBattleForceLocationName(int index) {
        if (index == 1) {
            return "REAR";
        }
        return "";
    }

    /**
     * We need to check whether the weapon is mounted in LOC_AFT in addition to
     * isRearMounted()
     */
    @Override
    public int getBattleForceTotalHeatGeneration(boolean allowRear) {
        int totalHeat = 0;

        for (Mounted mount : getWeaponList()) {
            WeaponType weapon = (WeaponType) mount.getType();
            if (weapon instanceof BayWeapon) {
                for (int index : mount.getBayWeapons()) {
                    totalHeat += ((WeaponType) (getEquipment(index).getType())).getHeat();
                }
            }
            if (weapon.hasFlag(WeaponType.F_ONESHOT)
                    || (allowRear && !mount.isRearMounted() && mount.getLocation() != LOC_AFT)
                    || (!allowRear && (mount.isRearMounted() || mount.getLocation() == LOC_AFT))) {
                continue;
            }
            totalHeat += weapon.getHeat();
        }

        return totalHeat;
    }

    @Override
    public int getBattleForceTotalHeatGeneration(int location) {
        int totalHeat = 0;

        for (Mounted mount : getWeaponList()) {
            WeaponType weapon = (WeaponType) mount.getType();
            if (weapon.hasFlag(WeaponType.F_ONESHOT)
                    || getBattleForceLocationMultiplier(location, mount.getLocation(), mount.isRearMounted()) == 0) {
                continue;
            }
            totalHeat += weapon.getHeat();
        }

        return totalHeat;
    }

    @Override
    public void addBattleForceSpecialAbilities(Map<BattleForceSPA, Integer> specialAbilities) {
        super.addBattleForceSpecialAbilities(specialAbilities);
        for (Mounted m : getEquipment()) {
            if (m.getType().hasFlag(MiscType.F_SPACE_MINE_DISPENSER)) {
                specialAbilities.merge(BattleForceSPA.MDS, 1, Integer::sum);
            }
        }
        if ((getEntityType() & (ETYPE_SMALL_CRAFT | ETYPE_JUMPSHIP | ETYPE_FIXED_WING_SUPPORT)) == 0) {
            specialAbilities.put(BattleForceSPA.BOMB, getWeightClass() + 1);
        }
        if ((getEntityType() & (ETYPE_JUMPSHIP | ETYPE_CONV_FIGHTER)) == 0) {
            specialAbilities.put(BattleForceSPA.SPC, null);
        }
        if (isVSTOL()) {
            specialAbilities.put(BattleForceSPA.VSTOL, null);
        }
    }

    /**
     * Is this a primitive ASF?
     *
     * @return
     */
    public boolean isPrimitive() {
        return (getCockpitType() == Aero.COCKPIT_PRIMITIVE);
    }

    @Override
    public String getLocationDamage(int loc) {
        return "";
    }

    public String getCritDamageString() {
        String toReturn = "";
        boolean first = true;
        if (getSensorHits() > 0) {
            if (!first) {
                toReturn += ", ";
            }
            toReturn += "Sensors (" + getSensorHits() + ")";
            first = false;
        }
        if (getAvionicsHits() > 0) {
            if (!first) {
                toReturn += ", ";
            }
            toReturn += "Avionics (" + getAvionicsHits() + ")";
            first = false;
        }
        if (getFCSHits() > 0) {
            if (!first) {
                toReturn += ", ";
            }
            toReturn += "FCS (" + getFCSHits() + ")";
            first = false;
        }
        if (isGearHit()) {
            if (!first) {
                toReturn += ", ";
            }
            toReturn += "Landing Gear";
            first = false;
        }
        if (getLeftThrustHits() > 0) {
            if (!first) {
                toReturn += ", ";
            }
            toReturn += "Left Thruster (" + getLeftThrustHits() + ")";
            first = false;
        }
        if (getRightThrustHits() > 0) {
            if (!first) {
                toReturn += ", ";
            }
            toReturn += "Right Thruster (" + getRightThrustHits() + ")";
            first = false;
        }
        return toReturn;
    }

    @Override
    public boolean isCrippled() {
        double internalPercent = getInternalRemainingPercent();
        String msg = getDisplayName() + " CRIPPLED: ";
        if (internalPercent < 0.5) {
            System.out.println(msg + "only " + NumberFormat.getPercentInstance().format(internalPercent)
                    + " internals remaining.");
            return true;
        }
        if (getEngineHits() > 0) {
            System.out.println(msg + engineHits + " Engine Hits.");
            return true;
        }
        if (getPotCrit() == CRIT_FUEL_TANK) {
            System.out.println(msg + " Fuel Tank Hit.");
            return true;
        }
        if ((getCrew() != null) && (getCrew().getHits() >= 4)) {
            System.out.println(msg + getCrew().getHits() + " Crew Hits.");
            return true;
        }

        // If this is not a military unit, we don't care about weapon status.
        if (!isMilitary()) {
            return false;
        }

        if (!hasViableWeapons()) {
            System.out.println(msg + " no more viable weapons.");
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
        if (getArmorRemainingPercent() <= 0.33) {
            return true;
        }

        if (getInternalRemainingPercent() < 0.67) {
            return true;
        }
        if ((getCrew() != null) && (getCrew().getHits() == 3)) {
            return true;
        }

        // If this is not a military unit, we don't care about weapon status.
        if (!isMilitary()) {
            return false;
        }

        List<Mounted> weaponList = getTotalWeaponList();
        int totalWeapons = weaponList.size();
        int totalInoperable = 0;
        for (Mounted weap : weaponList) {
            if (weap.isCrippled()) {
                totalInoperable++;
            }
        }
        return ((double) totalInoperable / totalWeapons) >= 0.75;
    }

    @Override
    public boolean isDmgModerate() {
        if (getArmorRemainingPercent() <= 0.5) {
            return true;
        }

        if (getInternalRemainingPercent() < 0.75) {
            return true;
        }

        if ((getCrew() != null) && (getCrew().getHits() == 2)) {
            return true;
        }

        // If this is not a military unit, we don't care about weapon status.
        if (!isMilitary()) {
            return false;
        }

        int totalWeapons = getTotalWeaponList().size();
        int totalInoperable = 0;
        for (Mounted weap : getTotalWeaponList()) {
            if (weap.isCrippled()) {
                totalInoperable++;
            }
        }
        return ((double) totalInoperable / totalWeapons) >= 0.5;
    }

    @Override
    public boolean isDmgLight() {
        if (getArmorRemainingPercent() <= 0.75) {
            return true;
        }

        if (getInternalRemainingPercent() < 0.9) {
            return true;
        }

        if ((getCrew() != null) && (getCrew().getHits() == 1)) {
            return true;
        }

        // If this is not a military unit, we don't care about weapon status.
        if (!isMilitary()) {
            return false;
        }

        int totalWeapons = getTotalWeaponList().size();
        int totalInoperable = 0;
        for (Mounted weap : getTotalWeaponList()) {
            if (weap.isCrippled()) {
                totalInoperable++;
            }
        }
        return ((double) totalInoperable / totalWeapons) >= 0.25;
    }

    @Override
    public boolean canSpot() {
        // per a recent ruling on the official forums, aero units can't spot
        // for indirect LRM fire, unless they have a recon cam, an infrared or
        // hyperspec imager, or a high-res imager and it's not night
        if (!isAirborne() || hasWorkingMisc(MiscType.F_RECON_CAMERA) || hasWorkingMisc(MiscType.F_INFRARED_IMAGER)
                || hasWorkingMisc(MiscType.F_HYPERSPECTRAL_IMAGER)
                || (hasWorkingMisc(MiscType.F_HIRES_IMAGER)
                        && ((game.getPlanetaryConditions().getLight() == PlanetaryConditions.L_DAY)
                                || (game.getPlanetaryConditions().getLight() == PlanetaryConditions.L_DUSK)))) {
            return true;
        } else {
            return false;
        }
    }

    // Damage a fighter that was part of a squadron when splitting it. Per
    // StratOps pg. 32 & 34
    @Override
    public void doDisbandDamage() {

        int dealt = 0;

        // Check for critical threshold and if so damage all armor on one facing
        // of the fighter completely,
        // reduce SI by half, and mark three engine hits.
        if (isDestroyed() || isDoomed()) {
            int loc = Compute.randomInt(4);
            dealt = getArmor(loc);
            setArmor(0, loc);
            int finalSI = Math.min(getSI(), getSI() / 2);
            dealt += getSI() - finalSI;
            setSI(finalSI);
            setEngineHits(Math.max(3, getEngineHits()));
        }

        // Move on to actual damage...
        int damage = getCap0Armor() - getCapArmor();
        if ((null != game) || !game.getOptions().booleanOption(OptionsConstants.ADVAERORULES_AERO_SANITY)) {
            damage *= 10;
        }
        damage -= dealt; // We already dealt a bunch of damage, move on.
        if (damage < 1) {
            return;
        }
        int hits = (int) Math.ceil(damage / 5.0);
        int damPerHit = 5;
        for (int i = 0; i < hits; i++) {
            int loc = Compute.randomInt(4);
            setArmor(getArmor(loc) - Math.max(damPerHit, damage), loc);
            // We did too much damage, so we need to damage the SI, but we wont
            // reduce the SI below 1 here
            // unless the fighter is destroyed.
            if (getArmor(loc) < 0) {
                if (getSI() > 1) {
                    int si = getSI() + (getArmor(loc) / 2);
                    si = Math.max(si, isDestroyed() || isDoomed() ? 0 : 1);
                    setSI(si);
                }
                setArmor(0, loc);
            }
            damage -= damPerHit;
        }
    }

    public int getNCrew() {
        return 1;
    }

    public int getNPassenger() {
        return 0;
    }

    @Override
    public long getEntityType() {
        return Entity.ETYPE_AERO;
    }

    public boolean isInASquadron() {
        return game.getEntity(getTransportId()) instanceof FighterSquadron;
    }

    @Override
    public boolean isAero() {
        return true;
    }

    @Override
    public boolean isBomber() {
        return isFighter();
    }

    @Override
    public int availableBombLocation(int cost) {
        return LOC_NOSE;
    }

    /**
     * Used to determine the draw priority of different Entity subclasses. This
     * allows different unit types to always be draw above/below other types.
     *
     * @return
     */
    @Override
    public int getSpriteDrawPriority() {
        return 10;
    }
}

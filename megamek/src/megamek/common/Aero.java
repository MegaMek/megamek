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

import megamek.client.ui.swing.calculationReport.CalculationReport;
import megamek.client.ui.swing.calculationReport.DummyCalculationReport;
import megamek.common.battlevalue.AeroBVCalculator;
import megamek.common.enums.AimingMode;
import megamek.common.enums.GamePhase;
import megamek.common.options.OptionsConstants;
import megamek.common.weapons.bayweapons.BayWeapon;
import org.apache.logging.log4j.LogManager;

import java.text.NumberFormat;
import java.util.*;

/**
 * Taharqa's attempt at creating an Aerospace entity
 */
public class Aero extends Entity implements IAero, IBomber {
    private static final long serialVersionUID = 7196307097459255187L;

    // locations
    public static final int LOC_NOSE = 0;
    public static final int LOC_LWING = 1;
    public static final int LOC_RWING = 2;
    public static final int LOC_AFT = 3;
    /** Location used for capital fighters and squadrons **/
    public static final int LOC_WINGS = 4;
    /** Location used for equipment not allocated to a firing arc **/
    public static final int LOC_FUSELAGE = 5;

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
    private static final int[] NUM_OF_SLOTS = { 100, 100, 100, 100, 100, 100, 100 };

    private static String[] LOCATION_ABBRS = { "NOS", "LWG", "RWG", "AFT", "WNG", "FSLG" };
    private static String[] LOCATION_NAMES = { "Nose", "Left Wing", "Right Wing", "Aft", "Wings", "Fuselage" };

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
    private boolean fuelTankHit = false;
    private boolean gearHit = false;
    private int structIntegrity;
    private int orig_structIntegrity;
    // set up damage threshold
    protected int[] damThresh = { 0, 0, 0, 0, 0, 0 };
    // set up an int for what the critical effect would be
    private int potCrit = CRIT_NONE;

    // ignored crew hit for harjel
    private int ignoredCrewHits = 0;
    private int cockpitType = COCKPIT_STANDARD;
    
    //Autoejection
    private boolean autoEject = true;
    private boolean condEjectAmmo = true;
    private boolean condEjectFuel = true;
    private boolean condEjectSIDest = true;
    
    private boolean ejecting = false;

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
    private int currentfuel = 0;

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
    Map<String, Integer> weaponGroups = new HashMap<>();

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
    
    //List of escape craft used by this ship
    private Set<String> escapeCraftList = new HashSet<>();
    
    //Maps unique id of each assigned marine to marine point value
    private Map<UUID,Integer> marines;

    public Aero() {
        super();
        // need to set altitude to something different than entity
        altitude = 5;
    }

    @Override
    public int getUnitType() {
        return UnitType.AERO;
    }

    protected static final TechAdvancement TA_ASF = new TechAdvancement(TECH_BASE_ALL)
            .setAdvancement(DATE_NONE, 2470, 2490).setProductionFactions(F_TH)
            .setTechRating(RATING_D).setAvailability(RATING_C, RATING_E, RATING_D, RATING_C)
            .setStaticTechLevel(SimpleTechLevel.STANDARD);
    protected static final TechAdvancement TA_ASF_PRIMITIVE = new TechAdvancement(TECH_BASE_IS)
            .setISAdvancement(DATE_ES, 2200, DATE_NONE, 2520)
            .setISApproximate(false, true, false).setProductionFactions(F_TA)
            .setTechRating(RATING_D).setAvailability(RATING_D, RATING_X, RATING_F, RATING_F)
            .setStaticTechLevel(SimpleTechLevel.ADVANCED);

    @Override
    public TechAdvancement getConstructionTechAdvancement() {
        if (isPrimitive()) {
            return TA_ASF_PRIMITIVE;
        } else {
            return TA_ASF;
        }
    }

    protected static final TechAdvancement[] COCKPIT_TA = {
            new TechAdvancement(TECH_BASE_ALL).setAdvancement(2460, 2470, 2491)
                .setApproximate(true, false, false).setPrototypeFactions(F_TH)
                .setPrototypeFactions(F_TH).setTechRating(RATING_C)
                .setAvailability(RATING_C, RATING_C, RATING_C, RATING_C)
                .setStaticTechLevel(SimpleTechLevel.STANDARD), //Standard
            new TechAdvancement(TECH_BASE_IS).setISAdvancement(3065, 3070, 3080)
                .setClanAdvancement(DATE_NONE, DATE_NONE, 3080)
                .setISApproximate(true, false, false).setPrototypeFactions(F_WB)
                .setPrototypeFactions(F_WB, F_CSR).setTechRating(RATING_E)
                .setAvailability(RATING_X, RATING_X, RATING_E, RATING_D)
                .setStaticTechLevel(SimpleTechLevel.STANDARD), //Small
            new TechAdvancement(TECH_BASE_ALL).setISAdvancement(2625, 2631, DATE_NONE, 2850, 3030)
                .setISApproximate(true, false, false, true, true)
                .setClanAdvancement(2625, 2631).setClanApproximate(true, false)
                .setClanApproximate(true, false).setPrototypeFactions(F_TH)
                .setPrototypeFactions(F_TH).setReintroductionFactions(F_FS).setTechRating(RATING_D)
                .setAvailability(RATING_C, RATING_F, RATING_E, RATING_D)
                .setStaticTechLevel(SimpleTechLevel.ADVANCED), //Cockpit command console
            new TechAdvancement(TECH_BASE_ALL).setAdvancement(DATE_ES, 2300, DATE_NONE, 2520)
                .setISApproximate(false, true, false, false)
                .setPrototypeFactions(F_TA).setTechRating(RATING_C)
                .setAvailability(RATING_D, RATING_X, RATING_X, RATING_F)
                .setStaticTechLevel(SimpleTechLevel.STANDARD), //Primitive
    };

    public static TechAdvancement getCockpitTechAdvancement(int cockpitType) {
        if (cockpitType >= 0 && cockpitType < COCKPIT_TA.length) {
            return new TechAdvancement(COCKPIT_TA[cockpitType]);
        }
        return null;
    }

    public TechAdvancement getCockpitTechAdvancement() {
        return getCockpitTechAdvancement(getCockpitType());
    }

    @Override
    protected void addSystemTechAdvancement(CompositeTechLevel ctl) {
        super.addSystemTechAdvancement(ctl);
        if (isFighter() && (getCockpitTechAdvancement() != null)) {
            ctl.addComponent(getCockpitTechAdvancement());
        }
    }

    // Is it Civilian or Military
    public static final int CIVILIAN = 0;
    public static final int MILITARY = 1;
    protected int designType = MILITARY;

    /**
     * Sets the unit as either a civilian or military design
     */
    public void setDesignType(int design) {
        designType = design;
    }

    /**
     * Returns the unit's design type
     */
    public int getDesignType() {
        return designType;
    }

    /**
     * A method to determine if an aero has suffered 3 sensor hits.
     * When double-blind is on, this affects both standard visibility and sensor rolls
     */
    @Override
    public boolean isAeroSensorDestroyed() {
        return getSensorHits() >= 3;
    }

    /**
     * Returns this entity's safe thrust, factored for heat, extreme
     * temperatures, gravity, partial repairs and bomb load.
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
        j = Math.max(0, j - getCargoMpReduction(this));
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
        // partially repaired engine
        if (getPartialRepairs().booleanOption("aero_engine_crit")) {
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
     * This is the same as getWalkMP, but does not divide by 2 when grounded
     *
     * @return
     */
    @Override
    public int getCurrentThrust() {
        int j = getOriginalWalkMP();
        j = Math.max(0, j - getCargoMpReduction(this));
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
        return 6;
    }

    @Override
    public int getBodyLocation() {
        return LOC_FUSELAGE;
    }

    @Override
    public boolean canChangeSecondaryFacing() {
        return false;
    }

    @Override
    public boolean isValidSecondaryFacing(int n) {
        return false;
    }

    /**
     * Aeros really can't torso twist?
     */
    @Override
    public int clipSecondaryFacing(int n) {
        return getFacing();
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

    @Override
    public void clearBombChoices() {
        Arrays.fill(bombChoices, 0);
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
    
    public boolean fuelTankHit() {
        return fuelTankHit;
    }
    
    public void setFuelTankHit(boolean value) {
        fuelTankHit = value;
    }

    public void setCICHits(int hits) {
        if (hits > 3) {
            hits = 3;
        }
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

    //Landing mods for partial repairs
    @Override
    public int getLandingGearPartialRepairs() {
        if (getPartialRepairs().booleanOption("aero_gear_crit")) {
            return 2;
        } else if (getPartialRepairs().booleanOption("aero_gear_replace")) {
            return 1;
        } else {
            return 0;
        }
    }

    // Avionics mods for partial repairs
    @Override
    public int getAvionicsMisreplaced() {
        if (getPartialRepairs().booleanOption("aero_avionics_replace")) {
        return 1;
        } else {
        return 0;
        }
    }

    @Override
    public int getAvionicsMisrepaired() {
        if (getPartialRepairs().booleanOption("aero_avionics_crit")) {
            return 1;
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

    @Override
    public boolean tracksHeat() {
        return true;
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

    public int getOriginalFuel() {
        return fuel;
    }

    @Override
    public int getFuel() {
        if ((getPartialRepairs().booleanOption("aero_asf_fueltank_crit"))
                || (getPartialRepairs().booleanOption("aero_fueltank_crit"))) {
            return (int) (fuel * 0.9);
        } else {
            return fuel;
        }
    }

    @Override
    public int getCurrentFuel() {
        if ((getPartialRepairs().booleanOption("aero_asf_fueltank_crit"))
                || (getPartialRepairs().booleanOption("aero_fueltank_crit"))) {
            return (int) (currentfuel * 0.9);
        } else {
            return currentfuel;
        }
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
        currentfuel = gas;
    }

    @Override
    public void setCurrentFuel(int gas) {
        currentfuel = gas;
    }

    @Override
    public double getFuelPointsPerTon() {
        if (isPrimitive()) {
            return 80 / primitiveFuelFactor();
        }
        return 80;
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
        fuel = (int) Math.floor(pointsPerTon * fuelTons + 0.001);
    }

    /**
     * Gets the fuel for this Aero in terms of tonnage.
     *
     * @return The number of tons of fuel on this Aero.
     */
    @Override
    public double getFuelTonnage() {
        // Rounding required for primitive small craft/dropship fuel multipliers.
        // The reason this is rounded normally instead of up is that the fuel points are actually calculated
        // from the tonnage and rounded down.
        return Math.round(2.0 * fuel / getFuelPointsPerTon()) / 2.0;
    }

    /**
     * Used by SmallCraft and Jumpship and their child classes.
     *
     * @return The tons of fuel burned in a day at 1G using strategic movement.
     */
    public double getStrategicFuelUse() {
        return 0.0;
    }

    /**
     * Some primitve aerospace units have their fuel efficiency reduced by a factor based
     * on construction year.
     *
     * @return The primitive fuel factor for the build year.
     */
    public double primitiveFuelFactor() {
        return 1.0;
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
        int arc;
        switch (mounted.getLocation()) {
            case LOC_NOSE:
            case LOC_WINGS:
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
            default:
                arc = Compute.ARC_360;
                break;
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
    public HitData rollHitLocation(int table, int side, int aimedLocation, AimingMode aimingMode,
                                   int cover) {
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

    @Override
    public int doBattleValueCalculation(boolean ignoreC3, boolean ignoreSkill, CalculationReport calculationReport) {
        return AeroBVCalculator.calculateBV(this, ignoreC3, ignoreSkill, calculationReport);
    }

    public double getBVTypeModifier() {
        return 1.2;
    }

    @Override
    public PilotingRollData addEntityBonuses(PilotingRollData prd) {
        // this is a control roll. Affected by:
        // avionics damage
        // partial repairs
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

        // partial repairs to avionics system, but only if the avionics aren't already destroyed
        if ((getPartialRepairs() != null) && (avihits < 3)) {
            if (getPartialRepairs().booleanOption("aero_avionics_crit")) {
                prd.addModifier(1, "Partial repair of Avionics");
            }
            if (getPartialRepairs().booleanOption("aero_avionics_replace")) {
                prd.addModifier(1, "Misreplaced Avionics");
            }
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
        if (!getGame().getBoard().inSpace() && (vmod > 0)) {
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
        if (hasAbility(OptionsConstants.MD_VDNI)
                && !hasAbility(OptionsConstants.MD_BVDNI)) {
            prd.addModifier(-1, "VDNI");
        }

        // Small/torso-mounted cockpit penalty?
        if ((getCockpitType() == Aero.COCKPIT_SMALL)
                && !hasAbility(OptionsConstants.MD_BVDNI)
                && !hasAbility(OptionsConstants.UNOFF_SMALL_PILOT)) {
            prd.addModifier(1, "Small Cockpit");
        }

        // quirks?
        if (hasQuirk(OptionsConstants.QUIRK_POS_ATMO_FLYER) && !game.getBoard().inSpace()) {
            prd.addModifier(-1, "atmospheric flyer");
        }
        if (hasQuirk(OptionsConstants.QUIRK_NEG_ATMO_INSTABILITY) && !game.getBoard().inSpace()) {
            prd.addModifier(+1, "atmospheric flight instability");
        }
        if (hasQuirk(OptionsConstants.QUIRK_NEG_CRAMPED_COCKPIT) && !hasAbility(OptionsConstants.UNOFF_SMALL_PILOT)) {
            prd.addModifier(1, "cramped cockpit");
        }

        return prd;
    }

    @Override
    public Vector<Report> victoryReport() {
        Vector<Report> vDesc = new Vector<>();

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
     * Fighters don't have MASC
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
        if (loc < damThresh.length) {
            damThresh[loc] = val;
        }
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
        } else if (loc < damThresh.length) {
            return damThresh[loc];
        }
        return 0;
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
        // ramming is resolved differently than charging
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

        return Math.round(cost * getPriceMultiplier());
    }

    @Override
    public double getPriceMultiplier() {
        double priceModifier = 1.0;
        if (isOmni()) {
            priceModifier *= 1.25f;
        }
        priceModifier *= 1 + (weight / 200f);
        return priceModifier;
    }

    @Override
    protected int implicitClanCASE() {
        if (!isClan() || !isFighter()) {
            return 0;
        }
        // Ammo is actually supposed to be assigned to a fuselage location rather than one of the four
        // weapon arcs. We will use LOC_NONE to record the existence of non-weapon explosive equipment.
        Set<Integer> caseLocations = new HashSet<>();
        int explicit = 0;
        for (Mounted m : getEquipment()) {
            if ((m.getType() instanceof MiscType) && (m.getType().hasFlag(MiscType.F_CASE))) {
                explicit++;
            } else if (m.getType().isExplosive(m)) {
                if (m.getType() instanceof WeaponType) {
                    caseLocations.add(m.getLocation());
                } else {
                    caseLocations.add(LOC_NONE);
                }
            }
        }
        return Math.max(0, caseLocations.size() - explicit);
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
        EquipmentType clCase = EquipmentType.get(EquipmentTypeLookup.CLAN_CASE);
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
            return UNLIMITED_JUMP_DOWN;
        }
        return 1;
    }

    /**
     * Determine if this unit has an active and working stealth system. (stealth
     * can be active and not working when under ECCM)
     * <p>
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
     * <p>
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
     * <p>
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
                    if (!ae.isConventionalInfantry()) {
                        result = new TargetRoll(0, "stealth");
                    } else {
                        result = new TargetRoll(0, "infantry ignore stealth");
                    }
                    break;
                case RangeType.RANGE_MEDIUM:
                    if (!ae.isConventionalInfantry()) {
                        result = new TargetRoll(1, "stealth");
                    } else {
                        result = new TargetRoll(0, "infantry ignore stealth");
                    }
                    break;
                case RangeType.RANGE_LONG:
                case RangeType.RANGE_EXTREME:
                case RangeType.RANGE_LOS:
                    if (!ae.isConventionalInfantry()) {
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
        if (isAirborne()) {
            return false;
        }

        Hex hex = game.getBoard().getHex(c);

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
                && (game.getPhase() != GamePhase.DEPLOYMENT)) {
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
        int type = this.getUnitType();
        int otherType = other.getUnitType();
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
        // Altitude is not the same as elevation. If an aero is at 0 altitude, then it is grounded
        // and uses elevation normally. Otherwise, just set elevation to a very large number so that
        // a flying aero won't interact with the ground maps in any way
        return isAirborne() ? 999 : super.getElevation();
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

    @Override
    public boolean isPrimitive() {
        return (getCockpitType() == Aero.COCKPIT_PRIMITIVE);
    }

    @Override
    public String getLocationDamage(int loc) {
        return "";
    }

    public String getCritDamageString() {
        StringBuilder toReturn = new StringBuilder();
        boolean first = true;
        if (getSensorHits() > 0) {
            if (!first) {
                toReturn.append(", ");
            }
            toReturn.append(String.format(Messages.getString("Aero.sensorDamageString"), getSensorHits()));
            first = false;
        }
        if (getAvionicsHits() > 0) {
            if (!first) {
                toReturn.append(", ");
            }
            toReturn.append(String.format(Messages.getString("Aero.avionicsDamageString"), getAvionicsHits()));
            first = false;
        }
        if (getFCSHits() > 0) {
            if (!first) {
                toReturn.append(", ");
            }
            toReturn.append(String.format(Messages.getString("Aero.fcsDamageString"), getFCSHits()));
            first = false;
        }
        if (getCICHits() > 0) {
            if (!first) {
                toReturn.append(", ");
            }
            toReturn.append(String.format(Messages.getString("Aero.cicDamageString"), getCICHits()));
            first = false;
        }
        if (isGearHit()) {
            if (!first) {
                toReturn.append(", ");
            }
            toReturn.append(Messages.getString("Aero.landingGearDamageString"));
            first = false;
        }
        if (!hasLifeSupport()) {
            if (!first) {
                toReturn.append(", ");
            }
            toReturn.append(Messages.getString("Aero.lifeSupportDamageString"));
            first = false;
        }
        if (getLeftThrustHits() > 0) {
            if (!first) {
                toReturn.append(", ");
            }
            toReturn.append(String.format(Messages.getString("Aero.leftThrusterDamageString"), getLeftThrustHits()));
            first = false;
        }
        if (getRightThrustHits() > 0) {
            if (!first) {
                toReturn.append(", ");
            }
            toReturn.append(String.format(Messages.getString("Aero.rightThrusterDamageString"), getRightThrustHits()));
            first = false;
        }
        // Cargo bays and bay doors for large craft
        for (Bay next : getTransportBays()) {
            if (next.getBayDamage() > 0) {
                if (!first) {
                    toReturn.append(", ");
                }
            toReturn.append(String.format(Messages.getString("Aero.bayDamageString"), next.getType(), next.getBayNumber()));
            first = false;
            }
            if (next.getCurrentDoors() < next.getDoors()) {
                if (!first) {
                    toReturn.append(", ");
                }
            toReturn.append(String.format(Messages.getString("Aero.bayDoorDamageString"), next.getType(), next.getBayNumber(), (next.getDoors() - next.getCurrentDoors())));
            first = false;
            }
        }
        return toReturn.toString();
    }

    @Override
    public boolean isCrippled() {
        return isCrippled(true);
    }

    @Override
    public boolean isCrippled(boolean checkCrew) {
        if (isEjecting()) {
            LogManager.getLogger().debug(getDisplayName() + " CRIPPLED: The crew is currently ejecting.");
            return true;
        } else if (getInternalRemainingPercent() < 0.5) {
            LogManager.getLogger().debug(getDisplayName() + " CRIPPLED: Only "
                    + NumberFormat.getPercentInstance().format(getInternalRemainingPercent()) + " internals remaining.");
            return true;
        } else if (getEngineHits() > 0) {
            LogManager.getLogger().debug(getDisplayName() + " CRIPPLED: " + engineHits + " Engine Hits.");
            return true;
        } else if (fuelTankHit()) {
            LogManager.getLogger().debug(getDisplayName() + " CRIPPLED: Fuel Tank Hit");
            return true;
        } else if (checkCrew && (getCrew() != null) && (getCrew().getHits() >= 4)) {
            LogManager.getLogger().debug(getDisplayName() + " CRIPPLED: " + getCrew().getHits() + " Crew Hits taken.");
            return true;
        } else if (getFCSHits() >= 3) {
            LogManager.getLogger().debug(getDisplayName() + " CRIPPLED: Fire Control Destroyed by taking " + fcsHits);
            return true;
        } else if (getCICHits() >= 3) {
            LogManager.getLogger().debug(getDisplayName() + " CRIPPLED: Combat Information Center Destroyed by taking " + cicHits);
            return true;
        }

        // If this is not a military unit, we don't care about weapon status.
        if (!isMilitary()) {
            return false;
        }

        if (!hasViableWeapons()) {
            LogManager.getLogger().debug(getDisplayName() + " CRIPPLED: No more viable weapons.");
            return true;
        } else {
            return false;
        }
    }

    @Override
    public boolean isDmgHeavy() {
        if (getArmorRemainingPercent() <= 0.33) {
            LogManager.getLogger().debug(getDisplayName()
                    + " Heavily Damaged: Armour Remaining percent of " + getArmorRemainingPercent()
                    + " is less than or equal to 0.33.");
            return true;
        } else if (getInternalRemainingPercent() < 0.67) {
            LogManager.getLogger().debug(getDisplayName()
                    + " Heavily Damaged: Internal Structure Remaining percent of " + getInternalRemainingPercent()
                    + " is less than 0.67.");
            return true;
        } else if ((getCrew() != null) && (getCrew().getHits() == 3)) {
            LogManager.getLogger().debug(getDisplayName()
                    + "Moderately Damaged: The crew has taken a minimum of three hits.");
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
            LogManager.getLogger().debug(getDisplayName()
                    + " Moderately Damaged: Armour Remaining percent of " + getArmorRemainingPercent()
                    + " is less than or equal to 0.50.");
            return true;
        } else if (getInternalRemainingPercent() < 0.75) {
            LogManager.getLogger().debug(getDisplayName()
                    + " Moderately Damaged: Internal Structure Remaining percent of " + getInternalRemainingPercent()
                    + " is less than 0.75.");
            return true;
        } else if ((getCrew() != null) && (getCrew().getHits() == 2)) {
            LogManager.getLogger().debug(getDisplayName()
                    + " Moderately Damaged: The crew has taken a minimum of two hits.");
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
            LogManager.getLogger().debug(getDisplayName()
                    + " Lightly Damaged: Armour Remaining percent of " + getArmorRemainingPercent()
                    + " is less than or equal to 0.75.");
            return true;
        } else if (getInternalRemainingPercent() < 0.9) {
            LogManager.getLogger().debug(getDisplayName()
                    + " Lightly Damaged: Internal Structure Remaining percent of " + getInternalRemainingPercent()
                    + " is less than 0.9.");
            return true;
        } else if ((getCrew() != null) && (getCrew().getHits() == 1)) {
            LogManager.getLogger().debug(getDisplayName()
                    + " Lightly Damaged: The crew has taken a minimum of one hit.");
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
        // Fix for #587. Only multiply if Aero Sanity is off
        if ((null != game) && !game.getOptions().booleanOption(OptionsConstants.ADVAERORULES_AERO_SANITY)) {
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
            // Fix for #587. Apply in 5 point groups unless damage remainder is less.
            setArmor(getArmor(loc) - Math.min(damPerHit, damage), loc);
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

    /**
     * Damage a capital fighter's weapons. WeaponGroups are damaged by critical hits.
     * This matches up the individual fighter's weapons and critical slots and damages those
     * for MHQ resolution
     * @param loc - Int corresponding to the location struck
     */
    public void damageCapFighterWeapons(int loc) {
        for (Mounted weapon : weaponList) {
            if (weapon.getLocation() == loc) {
                //Damage the weapon
                weapon.setHit(true);
                //Damage the critical slot
                for (int i = 0; i < getNumberOfCriticals(loc); i++) {
                    CriticalSlot slot1 = getCritical(loc, i);
                    if ((slot1 == null) ||
                            (slot1.getType() == CriticalSlot.TYPE_SYSTEM)) {
                        continue;
                    }
                    Mounted mounted = slot1.getMount();
                    if (mounted.equals(weapon)) {
                        hitAllCriticals(loc, i);
                        break;
                    }
                }
            }
        }
    }

    /**
     * @return The total number of crew available to supplement marines on boarding actions.
     *         Includes officers, enlisted, and bay personnel, but not marines/ba or passengers.
     */
    @Override
    public int getNCrew() {
        return 1;
    }
    
    @Override
    public void setNCrew(int crew) {
    }

    /**
     * @return The total number of officers for vessels.
     */
    public int getNOfficers() {
        return 0;
    }

    /**
     * @return The total number of gunners for vessels.
     */
    public int getNGunners() {
        return 0;
    }

    /**
     * Returns the number of passengers on this unit
     * Intended for spacecraft, where we want to get the crews of transported units
     * plus actual passengers assigned to quarters
     * @return
     */
    @Override
    public int getNPassenger() {
        return 0;
    }
    
    @Override
    public void setNPassenger(int pass) {
    }
    
    /**
     * Returns the list of Entity IDs used by this ship as escape craft
     * @return
     */
    public Set<String> getEscapeCraft() {
        return escapeCraftList;
    }
    
    /**
     * Adds an Escape Craft. Used by MHQ to track where escaped crew and passengers end up.
     * @param id The Entity ID of the ship to add.
     */
    public void addEscapeCraft(String id) {
        escapeCraftList.add(id);
    }
    
    /**
     * Removes an Escape Craft. Used by MHQ to track where escaped crew and passengers end up.
     * @param id The Entity ID of the ship to remove.
     */
    public void removeEscapeCraft(String id) {
        escapeCraftList.remove(id);
    }

    /**
     * @return The number battlearmored marines available to vessels for boarding actions.
     */
    public int getNBattleArmor() {
        return 0;
    }

    /**
     * @return The number conventional marines available to vessels for boarding actions.
     */
    @Override
    public int getNMarines() {
        return 0;
    }
    
    /**
     * Updates the number of marines aboard
     * @param marines The number of marines to add/subtract
     */
    @Override
    public void setNMarines(int marines) {
    }
    
    /**
     * Returns our list of unique individuals being transported as marines
     * @return
     */
    public Map<UUID,Integer> getMarines() {
        return marines;
    }
    
    /**
     * Adds a marine. Used by MHQ to track where a given person ends up. 
     * Also used by MM to move marines around between ships
     * @param personId The unique ID of the person to add.
     * @param pointValue The marine point value of the person being added
     */
    public void addMarine(UUID personId, int pointValue) {
        marines.put(personId, pointValue);
    }
    
    /**
     * Removes a marine. Used by MHQ to track where a given person ends up.
     * Also used by MM to move marines around between ships
     * @param personId The unique ID of the person to remove.
     */
    public void removeMarine(UUID personId) {
        marines.remove(personId);
    }
    
    /**
     * Returns the number of marines assigned to a unit
     * Used for abandoning a unit
     * @return
     */
    public int getMarineCount() {
        return 0;
    }
    
    /**
     * Convenience method that compiles the total number of people aboard a ship - Crew, Marines, Passengers...
     * @return An integer representing everyone aboard
     */
    public int getTotalAboard() {
        return (getNCrew() + getNPassenger() + getMarineCount());
    }

    /**
     * @return The number of escape pods carried by the unit
     */
    public int getEscapePods() {
        return 0;
    }
    
    /**
     * Convenience method to return the number of escape pods remaining
     * @return
     */
    public int getPodsLeft() {
        return getEscapePods() - getLaunchedEscapePods();
    }

    /**
     * @return The number of lifeboats carried by the unit
     */
    public int getLifeBoats() {
        return 0;
    }
    
    /**
     * Returns the total number of escape pods launched so far
     */
    public int getLaunchedEscapePods() {
        return 0;
    }
    
    /**
     * Updates the total number of escape pods launched so far
     * @param n The number to change
     */
    public void setLaunchedEscapePods(int n) {
    }
    
    /**
     * Returns the total number of life boats launched so far
     */
    public int getLaunchedLifeBoats() {
        return 0;
    }
    
    /**
     * Convenience method to return the number of life boats remaining
     * @return
     */
    public int getLifeBoatsLeft() {
        return getLifeBoats() - getLaunchedLifeBoats();
    }
    
    /**
     * Updates the total number of life boats launched so far
     * @param n The number to change
     */
    public void setLaunchedLifeBoats(int n) {
    }
    
    /**
     * Calculates whether this ship has any available escape systems remaining
     * return
     */
    public boolean hasEscapeSystemsLeft() {
        return ((getLaunchedLifeBoats() < getLifeBoats()) 
                || (getLaunchedEscapePods() < getEscapePods())
                || !getLaunchableSmallCraft().isEmpty());
    }
    
    /**
     * Calculates the total number of people that can be carried in this unit's escape systems
     * 6 people per lifeboat/escape pod + troop capacity of any small craft
     * Most small craft use cargo space instead of infantry bays, so we'll assume 0.1 tons/person
     * (Taken from Infantry.getWeight() - foot trooper + .015t for the spacesuit everyone aboard is wearing ;) )
     * @return The total escape count for the unit
     */
    public int getEscapeCapacity() {
        int people = 0;
        // We can cram 6 people in an escape pod
        people += getEscapePods() * 6;
        // Lifeboats hold 6 comfortably
        people += getLifeBoats() * 6;
        
        // Any small craft aboard and able to launch?
        for (Entity sc : getLaunchableSmallCraft()) {
            // There could be an ASF in the bay...
            if (sc instanceof SmallCraft) {
                for (Bay b : sc.getTransportBays()) {
                    if (b instanceof InfantryBay || b instanceof BattleArmorBay || b instanceof CargoBay) {
                        // Use the available tonnage
                        people += (b.getCapacity() / 0.1);
                    }
                }
            }
        }
        return people;
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

    @Override
    public List<Mounted> getActiveAMS() {
        //Large craft use AMS and Point Defense bays
        if ((this instanceof Dropship)
                || (this instanceof Jumpship)
                || (this instanceof Warship)
                || (this instanceof SpaceStation)) {

            ArrayList<Mounted> ams = new ArrayList<>();
            for (Mounted weapon : getWeaponBayList()) {
                // Skip anything that's not an AMS, AMS Bay or Point Defense Bay
                if (!weapon.getType().hasFlag(WeaponType.F_AMS)
                        && !weapon.getType().hasFlag(WeaponType.F_AMSBAY)
                        && !weapon.getType().hasFlag(WeaponType.F_PDBAY))  {
                    continue;
                }

                // Make sure the AMS is good to go
                if (!weapon.isReady() || weapon.isMissing()
                        || weapon.curMode().equals("Off")
                        || weapon.curMode().equals("Normal")) {
                    continue;
                }

                // AMS blocked by transported units can not fire
                if (isWeaponBlockedAt(weapon.getLocation(),
                        weapon.isRearMounted())) {
                    continue;
                }

                // Make sure ammo is loaded
                for (int wId : weapon.getBayWeapons()) {
                    Mounted bayW = getEquipment(wId);
                    Mounted bayWAmmo = bayW.getLinked();
                    if (!(weapon.getType().hasFlag(WeaponType.F_ENERGY))
                            && ((bayWAmmo == null) || (bayWAmmo.getUsableShotsLeft() == 0)
                                    || bayWAmmo.isDumping())) {
                        loadWeapon(weapon);
                        bayWAmmo = weapon.getLinked();
                    }

                    // try again
                    if (!(weapon.getType().hasFlag(WeaponType.F_ENERGY))
                            && ((bayWAmmo == null) || (bayWAmmo.getUsableShotsLeft() == 0)
                                    || bayWAmmo.isDumping())) {
                        // No ammo for this AMS.
                        continue;
                    }
                }
                ams.add(weapon);
            }
            return ams;
        }
        //ASFs and Small Craft should use regular old AMS...
        return super.getActiveAMS();
    }

    /**
     * A method to add/remove sensors that only work in space as we transition in and out of an atmosphere
     */
    @Override
    public void updateSensorOptions() {
        //Prevent adding duplicates
        boolean hasSpacecraftThermal = false;
        boolean hasAeroThermal = false;
        boolean hasESM = false;
        for (Sensor sensor : getSensors()) {
            if (sensor.getType() == Sensor.TYPE_SPACECRAFT_THERMAL) {
                hasSpacecraftThermal = true;
            }
            if (sensor.getType() == Sensor.TYPE_AERO_THERMAL) {
                hasAeroThermal = true;
            }
            if (sensor.getType() == Sensor.TYPE_SPACECRAFT_ESM) {
                hasESM = true;
            }
        }
        //Remove everything but Radar if we're not in space
        if (!isSpaceborne()) {
            Vector<Sensor> sensorsToRemove = new Vector<>();
            if (hasETypeFlag(Entity.ETYPE_DROPSHIP)) {
                for (Sensor sensor : getSensors()) {
                    if (sensor.getType() == Sensor.TYPE_SPACECRAFT_ESM) {
                        hasESM = false;
                        sensorsToRemove.add(sensor);
                    }
                    if (sensor.getType() == Sensor.TYPE_SPACECRAFT_THERMAL) {
                        hasSpacecraftThermal = false;
                        sensorsToRemove.add(sensor);
                    }
                }
            } else if (hasETypeFlag(Entity.ETYPE_AERO)) {
                for (Sensor sensor : getSensors()) {
                    if (sensor.getType() == Sensor.TYPE_AERO_THERMAL) {
                        hasAeroThermal = false;
                        sensorsToRemove.add(sensor);
                    }
                }
            }
            getSensors().removeAll(sensorsToRemove);
            if (sensorsToRemove.size() >= 1) {
            setNextSensor(getSensors().firstElement());
            }
        }
        //If we are in space, add them back...
        if (isSpaceborne()) {
            if (hasETypeFlag(Entity.ETYPE_DROPSHIP)
                    || hasETypeFlag(Entity.ETYPE_SPACE_STATION)
                    || hasETypeFlag(Entity.ETYPE_JUMPSHIP)
                    || hasETypeFlag(Entity.ETYPE_WARSHIP)) {
                //Large craft get thermal/optical sensors
                if (!hasSpacecraftThermal) {
                    getSensors().add(new Sensor(Sensor.TYPE_SPACECRAFT_THERMAL));
                    hasSpacecraftThermal = true;
                }
                //Only military craft get ESM, which detects active radar
                if (getDesignType() == Aero.MILITARY) {
                    if (!hasESM) {
                        getSensors().add(new Sensor(Sensor.TYPE_SPACECRAFT_ESM));
                        hasESM = true;
                    }
                }
            } else if (hasETypeFlag(Entity.ETYPE_AERO)
                        || hasETypeFlag(Entity.ETYPE_SMALL_CRAFT)) {
                //ASFs and small craft get thermal/optical sensors
                if (!hasAeroThermal) {
                    getSensors().add(new Sensor(Sensor.TYPE_AERO_THERMAL));
                    hasAeroThermal = true;
                }
            }
        }
    }
    
    // autoejection methods
    /**
     * @return Returns the autoEject.
     */
    public boolean isAutoEject() {
        boolean hasEjectSeat = !hasQuirk(OptionsConstants.QUIRK_NEG_NO_EJECT);

        return autoEject && hasEjectSeat;
    }

    /**
     * @param autoEject
     *            Turn the master autoejection system on or off
     */
    public void setAutoEject(boolean autoEject) {
        this.autoEject = autoEject;
    }
    
    /**
     * Is autoejection enabled for ammo explosions?
     * @return
     */
    public boolean isCondEjectAmmo() {
        return condEjectAmmo;
    }

    /**
     * Used by Conditional Auto Ejection - will we eject when an ammo explosion is triggered?
     * @param  condEjectAmmo  Sets autoejection for ammo explosions
     */
    public void setCondEjectAmmo(boolean condEjectAmmo) {
        this.condEjectAmmo = condEjectAmmo;
    }

    /**
     * Is autoejection enabled for fuel explosions?
     * @return
     */
    public boolean isCondEjectFuel() {
        return condEjectFuel;
    }

    /**
     * Used by Conditional Auto Ejection - will we eject when a fuel explosion is triggered?
     * @param  condEjectFuel   Sets autoejection for fuel tank explosions
     */
    public void setCondEjectFuel(boolean condEjectFuel) {
        this.condEjectFuel = condEjectFuel;
    }

    /**
     * Is autoejection enabled for SI destruction (Fighter only)?
     * @return
     */
    public boolean isCondEjectSIDest() {
        return condEjectSIDest;
    }

    /**
     * Used by Conditional Auto Ejection - will we eject when structural integrity is reduced to 0?
     * @param  condEjectSIDest   Sets autoejection for structural integrity destruction
     */
    public void setCondEjectSIDest(boolean condEjectSIDest) {
        this.condEjectSIDest = condEjectSIDest;
    }
    
    /**
     * Intended for large craft. Indicates that the ship is being abandoned.
     * @return
     */
    public boolean isEjecting() {
        return ejecting;
    }

    /**
     * Changes the ejecting flag when the order to abandon ship is given
     * @param ejecting Change to the ejecting status of this ship
     */
    public void setEjecting(boolean ejecting) {
        this.ejecting = ejecting;
    }
}

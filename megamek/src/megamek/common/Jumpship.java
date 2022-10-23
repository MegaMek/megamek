/*
 * MegaAero - Copyright (C) 2007 Jay Lawson This program is free software; you
 * can redistribute it and/or modify it under the terms of the GNU General
 * Public License as published by the Free Software Foundation; either version 2
 * of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. See the GNU General Public License for more
 * details.
 */
package megamek.common;

import megamek.client.ui.swing.calculationReport.CalculationReport;
import megamek.common.cost.JumpShipCostCalculator;
import megamek.common.options.OptionsConstants;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author Jay Lawson
 * @since Jun 17, 2007
 */
public class Jumpship extends Aero {
    private static final long serialVersionUID = 9154398176617208384L;
    // Additional Jumpship locations (FLS, FRS and ALS override Aero locations)
    public static final int LOC_FLS = 1;
    public static final int LOC_FRS = 2;
    public static final int LOC_ALS = 4;
    public static final int LOC_ARS = 5;
    public static final int LOC_HULL = 6;

    public static final int GRAV_DECK_STANDARD_MAX = 100;
    public static final int GRAV_DECK_LARGE_MAX = 250;
    public static final int GRAV_DECK_HUGE_MAX = 1500;
    
    public static final int DRIVE_CORE_STANDARD    = 0;
    public static final int DRIVE_CORE_COMPACT     = 1;
    public static final int DRIVE_CORE_SUBCOMPACT  = 2;
    public static final int DRIVE_CORE_NONE        = 3;
    public static final int DRIVE_CORE_PRIMITIVE   = 4;
    
    // The percentage of the total unit weight taken up by the drive core. The value
    // given for primitive assumes a 30ly range, but the final value has to be computed.
    private static double[] DRIVE_CORE_WEIGHT_PCT = { 0.95, 0.4525, 0.5, 0.0, 0.95 };

    private static String[] LOCATION_ABBRS = { "NOS", "FLS", "FRS", "AFT", "ALS", "ARS", "HULL" };
    private static String[] LOCATION_NAMES = { "Nose", "Left Front Side", "Right Front Side",
            "Aft", "Aft Left Side", "Aft Right Side", "Hull" };

    // K-F Drive Stuff
    private int original_kf_integrity = 0;
    private int kf_integrity = 0;
    private int original_sail_integrity = 0;
    private int sail_integrity = 0;
    private int helium_tankage = 0;
    private boolean heliumTankHit = false;
    private boolean driveCoilHit = false;
    private boolean fieldInitiatorHit = false;
    private boolean chargingSystemHit = false;
    private boolean driveControllerHit = false;
    private boolean lfBatteryHit = false;
    private boolean sail = true;
    private int driveCoreType = DRIVE_CORE_STANDARD;
    private int jumpRange = 30; // Primitive JumpShips can have a reduced range
    
    // lithium fusion
    boolean hasLF = false;

    // crew and passengers
    private int nBattleArmor = 0;
    private int nOtherCrew = 0;
    private int nOfficers = 0;
    private int nGunners = 0;
    
    // lifeboats and escape pods
    private int lifeBoats = 0;
    private int escapePods = 0;
    private int escapePodsLaunched = 0;
    private int lifeBoatsLaunched = 0;

    // Battlestation
    private boolean isBattleStation = false;

    // HPG
    private boolean hasHPG = false;

    /**
     * Keep track of all of the grav decks and their sizes.
     *
     * This is a new approach for storing grav decks, which allows the size of each deck to be stored.  Previously,
     * we just stored the number of standard, large and huge grav decks, and could not specify the exact size of the
     * deck.
     */
    private List<Integer> gravDecks = new ArrayList<>();
    
    /**
     * Keep track of all of the grav decks and their damage status
     *
     * Stores the number of hits on each grav deck by the index value from the list gravDecks
     */
    private Map<Integer,Integer> damagedGravDecks = new HashMap<>();

    // station-keeping thrust and accumulated thrust
    private double stationThrust = 0.2;
    private double accumulatedThrust = 0.0;

    public Jumpship() {
        super();
        damThresh = new int[] { 0, 0, 0, 0, 0, 0, 0 };
    }

    @Override
    public boolean tracksHeat() {
        return false;
    }

    @Override
    public int getUnitType() {
        // While large craft perform heat calculations, they are not considered heat-tracking units
        // because they cannot generate more heat than they can dissipate in the same turn.
        return UnitType.JUMPSHIP;
    }

    // ASEW Missile Effects, per location
    // Values correspond to Locations: NOS, FLS, FRS, AFT, ALS, ARS
    private int[] asewAffectedTurns = { 0, 0, 0, 0, 0, 0 };
    
    /*
     * Accessor for the asewAffectedTurns array, which may be different for inheriting classes.
     */
    protected int[] getAsewAffectedTurns() {
        return asewAffectedTurns;
    }
    
    /*
     * Sets the number of rounds a specified firing arc is affected by an ASEW missile
     * @param arc - integer representing the desired firing arc
     * @param turns - integer specifying the number of end phases that the effects last through
     * Technically, about 1.5 turns elapse per the rules for ASEW missiles in TO
     */
    public void setASEWAffected(int arc, int turns) {
        if (arc < getAsewAffectedTurns().length) {
            getAsewAffectedTurns()[arc] = turns;
        }
    }
    
    /*
     * Returns the number of rounds a specified firing arc is affected by an ASEW missile
     * @param arc - integer representing the desired firing arc
     */
    public int getASEWAffected(int arc) {
        if (arc < getAsewAffectedTurns().length) {
            return getAsewAffectedTurns()[arc];
        }
        return 0;
    }
    
    /**
     * Primitive JumpShips may be constructed with standard docking collars, or with pre-boom collars.
     */
    public static final int COLLAR_STANDARD  = 0;
    public static final int COLLAR_NO_BOOM = 1;

    protected static final TechAdvancement TA_JUMPSHIP = new TechAdvancement(TECH_BASE_ALL)
            .setAdvancement(DATE_NONE, 2300).setISApproximate(false, true)
            .setProductionFactions(F_TA).setTechRating(RATING_D)
            .setAvailability(RATING_D, RATING_E, RATING_D, RATING_F)
            .setStaticTechLevel(SimpleTechLevel.ADVANCED);
    protected static final TechAdvancement TA_JUMPSHIP_PRIMITIVE = new TechAdvancement(TECH_BASE_IS)
            .setISAdvancement(2100, 2200, DATE_NONE, 2500)
            .setISApproximate(true, true, false, false)
            .setProductionFactions(F_TA).setTechRating(RATING_D)
            .setAvailability(RATING_D, RATING_X, RATING_X, RATING_X)
            .setStaticTechLevel(SimpleTechLevel.ADVANCED);
    
    @Override
    public TechAdvancement getConstructionTechAdvancement() {
        return isPrimitive() ? TA_JUMPSHIP_PRIMITIVE : TA_JUMPSHIP;
    }
    
    /**
     * Tech advancement data for lithium fusion batteries
     */
    public static TechAdvancement getLFBatteryTA() {
        return new TechAdvancement(TECH_BASE_ALL)
                .setISAdvancement(2520, 2529, DATE_NONE, 2819, 3043)
                .setISApproximate(true, false, false, false, false)
                .setPrototypeFactions(F_TH).setProductionFactions(F_TH).setReintroductionFactions(F_FS)
                .setClanAdvancement(2520, 2529)
                .setTechRating(RATING_E)
                .setAvailability(RATING_E, RATING_F, RATING_E, RATING_E)
                .setStaticTechLevel(SimpleTechLevel.ADVANCED);
    }
    
    /**
     * Tech advancement data for the jump sail
     */
    public static TechAdvancement getJumpSailTA() {
        return new TechAdvancement(TECH_BASE_ALL)
                .setAdvancement(2200, 2300, 2325)
                .setPrototypeFactions(F_TA).setProductionFactions(F_TA)
                .setTechRating(RATING_D)
                .setAvailability(RATING_E, RATING_E, RATING_D, RATING_D)
                .setStaticTechLevel(SimpleTechLevel.ADVANCED);
    }
    
    /**
     * @return Returns the autoEject setting (always off for large craft)
     */
    @Override
    public boolean isAutoEject() {
        return false;
    }

    @Override
    public String getCritDamageString() {
        StringBuilder toReturn = new StringBuilder(super.getCritDamageString());
        boolean first = toReturn.length() == 0;
        if (getTotalDamagedGravDeck() > 0) {
            if (!first) {
                toReturn.append(", ");
            }
            toReturn.append(String.format(Messages.getString("Jumpship.gravDeckDamageString"), getTotalDamagedGravDeck()));
            first = false;
        }
        if (getTotalDamagedDockingCollars() > 0) {
            if (!first) {
                toReturn.append(", ");
            }
            toReturn.append(String.format(Messages.getString("Jumpship.dockingCollarsDamageString"), getTotalDamagedDockingCollars()));
            first = false;
        }
        if (getKFDriveCoilHit()) {
            if (!first) {
                toReturn.append(", ");
            }
            toReturn.append(Messages.getString("Jumpship.driveCoilDamageString"));
            first = false;
        }
        if (getKFDriveControllerHit()) {
            if (!first) {
                toReturn.append(", ");
            }
            toReturn.append(Messages.getString("Jumpship.driveControllerDamageString"));
            first = false;
        }
        if (getKFHeliumTankHit()) {
            if (!first) {
                toReturn.append(", ");
            }
            toReturn.append(Messages.getString("Jumpship.heliumTankDamageString"));
            first = false;
        }
        if (getKFFieldInitiatorHit()) {
            if (!first) {
                toReturn.append(", ");
            }
            toReturn.append(Messages.getString("Jumpship.fieldInitiatorDamageString"));
            first = false;
        }
        if (getKFChargingSystemHit()) {
            if (!first) {
                toReturn.append(", ");
            }
            toReturn.append(Messages.getString("Jumpship.chargingSystemDamageString"));
            first = false;
        }
        if (getLFBatteryHit()) {
            if (!first) {
                toReturn.append(", ");
            }
            toReturn.append(Messages.getString("Jumpship.lfBatteryDamageString"));
            first = false;
        }
        return toReturn.toString();
    }

    @Override
    public CrewType defaultCrewType() {
        return CrewType.VESSEL;
    }
    
    @Override
    public int locations() {
        return 7;
    }

    @Override
    public int getBodyLocation() {
        return LOC_HULL;
    }
    
    /**
     * Get the docking collar type used by the ship.
     *
     * @return the docking collar type
     */
    public int getDockingCollarType() {
        return (isPrimitive() ? Jumpship.COLLAR_NO_BOOM : Jumpship.COLLAR_STANDARD);
    }
    
    /** Returns the number of free Docking Collars. */
    public int getFreeDockingCollars() {
        return getDockingCollars().stream().mapToInt(dc -> (int) dc.getUnused()).sum();
    }
    
    /**
     * Get the number of damaged docking collars on the ship.
     * Used by crit damage string on unit display
     *
     * @return the number of damaged docking collars
     */
    public int getTotalDamagedDockingCollars() {
        int count = 0;
        for (DockingCollar collar : getDockingCollars()) {
            if (collar.isDamaged()) {
                count++;
            }
        }
        return count;
    }

    /**
     * Get the number of grav decks on the ship.
     *
     * @return the total number of grav decks
     */
    public int getTotalGravDeck() {
        return gravDecks.size();
    }
    
    /**
     * Get the number of damaged grav decks on the ship.
     * Used by JS/WS MapSet widget to display critical hits
     *
     * @return the number of damaged grav decks
     */
    public int getTotalDamagedGravDeck() {
        int count = 0;
        for (int hits : damagedGravDecks.values()) {
            if (hits == 1) {
                count++;
            }
        }
        return count;
    }

    /**
     * Adds a grav deck whose size in meters is specified.
     *
     * @param size  The size in meters of the grav deck.
     */
    public void addGravDeck(int size) {
        gravDecks.add(size);
    }

    /**
     * Get a list of all grav decks mounted on this ship. Returns the size in meters of the deck
     *
     * @return a list of grav deck diameters, in meters
     */
    public List<Integer> getGravDecks() {
        return gravDecks;
    }
    
    /**
     * Adds a grav deck damage value that maps to the index of each deck size in meters
     *
     */
    public void initializeGravDeckDamage(int index) {
        damagedGravDecks.put(index, 0);
    }

    /**
     * Gets the damage flag for the grav deck with the specified key
     *
     * @return the damage status for the deck 0 (undamaged) or 1 (damaged)
     */
    public int getGravDeckDamageFlag(int key) {
        return damagedGravDecks.get(key);
    }
    
    /**
     * Sets the damage flag for the grav deck with the specified key to the specified value
     *
     * @param key - the id of the deck to affect
     * @param damaged - 0 (undamaged), 1 (damaged)
     */
    public void setGravDeckDamageFlag(int key, int damaged) {
        damagedGravDecks.replace(key, damaged);
    }

    /**
     * Old style for setting the number of grav decks.  This allows the user to specify N standard grav decks, which
     * will get added at a default value.
     *
     * @param n
     */
    public void setGravDeck(int n) {
        for (int i = 0; i < n; i++) {
            addGravDeck(GRAV_DECK_STANDARD_MAX / 2);
        }
    }

    /**
     * Get the number of standard grav decks
     * @return the number of 0-99 meter grav decks installed
     */
    public int getGravDeck() {
        int count = 0;
        for (int deck : gravDecks) {
            if (deck < GRAV_DECK_STANDARD_MAX) {
                count++;
            }
        }
        return count;
    }

    /**
     * Old style method for adding N large grav decks. A default value is chosen that is half-way
     * between the standard and huge sizes.
     *
     * @param n
     */
    public void setGravDeckLarge(int n) {
        for (int i = 0; i < n; i++) {
            addGravDeck(GRAV_DECK_STANDARD_MAX + (GRAV_DECK_LARGE_MAX - GRAV_DECK_STANDARD_MAX) / 2);
        }
    }

    /**
     * Get the number of large grav decks.
     *
     * @return the number of 100-249 meter grav decks installed
     */
    public int getGravDeckLarge() {
        int count = 0;
        for (int deck : gravDecks) {
            if (deck >= GRAV_DECK_STANDARD_MAX && deck <= GRAV_DECK_LARGE_MAX) {
                count++;
            }
        }
        return count;
    }

    /**
     * Old style method for adding N huge grav decks. A default value is chosen that is the current
     * large maximum plus half that value.
     *
     * @param n
     */
    public void setGravDeckHuge(int n) {
        for (int i = 0; i < n; i++) {
            addGravDeck(GRAV_DECK_LARGE_MAX + (GRAV_DECK_LARGE_MAX) / 2);
        }
    }

    /**
     * Get the number of huge grav decks.
     *
     * @return the number of 250 meter and larger grav decks installed
     */
    public int getGravDeckHuge() {
        int count = 0;
        for (int deck : gravDecks) {
            if (deck > GRAV_DECK_LARGE_MAX) {
                count++;
            }
        }
        return count;
    }

    public void setHPG(boolean b) {
        hasHPG = b;
    }

    public boolean hasHPG() {
        return hasHPG;
    }

    public void setBattleStation(boolean b) {
        isBattleStation = b;

    }

    public boolean isBattleStation() {
        return isBattleStation;
    }

    public void setLF(boolean b) {
        hasLF = b;
    }

    public boolean hasLF() {
        return hasLF;
    }

    public void setEscapePods(int n) {
        escapePods = n;
    }

    @Override
    public int getEscapePods() {
        return escapePods;
    }
    
    /**
     * Returns the total number of escape pods launched so far
     */
    @Override
    public int getLaunchedEscapePods() {
        return escapePodsLaunched;
    }
    
    /**
     * Updates the total number of escape pods launched so far
     * @param n The number to change
     */
    @Override
    public void setLaunchedEscapePods(int n) {
        escapePodsLaunched = n;
    }

    public void setLifeBoats(int n) {
        lifeBoats = n;
    }

    @Override
    public int getLifeBoats() {
        return lifeBoats;
    }
    
    /**
     * Returns the total number of life boats launched so far
     */
    @Override
    public int getLaunchedLifeBoats() {
        return lifeBoatsLaunched;
    }
    
    /**
     * Updates the total number of life boats launched so far
     * @param n The number to change
     */
    @Override
    public void setLaunchedLifeBoats(int n) {
        lifeBoatsLaunched = n;
    }

    @Override
    public void setNCrew(int crew) {
        nCrew = crew;
    }

    @Override
    public int getNCrew() {
        return nCrew;
    }

    @Override
    public void setNPassenger(int pass) {
        nPassenger = pass;
    }

    public void setNOfficers(int officer) {
        nOfficers = officer;
    }
    
    @Override
    public int getNOfficers() {
        return nOfficers;
    }
    
    public void setNGunners(int gunners) {
        nGunners = gunners;
    }
    
    @Override
    public int getNGunners() {
        return nGunners;
    }
    
    @Override
    public int getNPassenger() {
        return nPassenger;
    }

    @Override
    public void setNMarines(int m) {
        nMarines = m;
    }

    /**
     * Returns the number of marines assigned to a unit
     * Used for abandoning a unit
     * @return
     */
    @Override
    public int getNMarines() {
        return nMarines;
    }

    public void setNBattleArmor(int m) {
        nBattleArmor = m;
    }

    @Override
    public int getNBattleArmor() {
        return nBattleArmor;
    }

    public void setNOtherCrew(int m) {
        nOtherCrew = m;
    }

    public int getNOtherCrew() {
        return nOtherCrew;
    }
    
    @Override
    public double getFuelPointsPerTon() {
        double ppt;
        if (getWeight() < 110000) {
            ppt = 10;
        } else if (getWeight() < 250000) {
            ppt = 5;
        } else {
            ppt = 2.5;
        }
        if (isPrimitive()) {
            return ppt / primitiveFuelFactor();
        }
        return ppt;
    }

    @Override
    public double getStrategicFuelUse() {
        double fuelUse;
        if (weight >= 200000) {
            fuelUse = 39.52;
        } else if (weight >= 100000) {
            fuelUse = 19.75;
        } else if (weight >= 50000) {
            fuelUse = 9.77;
        } else {
            fuelUse = 2.82;
        }
        if (isPrimitive()) {
            return fuelUse * primitiveFuelFactor();
        }
        // JS and SS (and WS without transit drives) use fuel at 10% the rate.
        if (hasStationKeepingDrive()) {
            fuelUse *= 0.1;
        }
        return fuelUse;
    }

    @Override
    public double primitiveFuelFactor() {
        int year = getOriginalBuildYear();
        if (year >= 2300) {
            return 1.0;
        } else if (year >= 2251) {
            return 1.1;
        } else if (year >= 2201) {
            return 1.4;
        } else if (year >= 2151) {
            return 1.7;
        } else {
            return 2.0;
        }
    }

    @Override
    public String[] getLocationAbbrs() {
        return LOCATION_ABBRS;
    }

    @Override
    public String[] getLocationNames() {
        return LOCATION_NAMES;
    }
    
    //Methods for dealing with the K-F Drive, Sail and L-F Battery
    
    //Set the current KF Drive integrity
    public void setKFIntegrity(int kf) {
        kf_integrity = kf;
    }
    
    //Return the current KF Drive integrity
    public int getKFIntegrity() {
        return kf_integrity;
    }
    
    //Set the original/undamaged KF Drive integrity
    public void setOKFIntegrity(int kf) {
        original_kf_integrity = kf;
    }
    
    //Return the original/undamaged KF Drive integrity
    public int getOKFIntegrity() {
        return original_kf_integrity;
    }
    
    //Return the damage taken to the KF Drive
    public int getKFDriveDamage() {
        return (getOKFIntegrity() - getKFIntegrity());
    }
    
    //Is any part of the KF Drive damaged?  Used by MHQ for repairs.
    public boolean isKFDriveDamaged() {
        return (getKFHeliumTankHit() 
                || getKFDriveCoilHit() 
                || getKFDriveControllerHit() 
                || getLFBatteryHit() 
                || getKFChargingSystemHit()
                || getKFFieldInitiatorHit());
    }
    
    //Set the portion of the total drive integrity represented by the helium tanks
    public void setKFHeliumTankIntegrity(int ht) {
        helium_tankage = ht;
    }
    
    //Used by MHQ when repairing the helium tanks. Allows restoration of up to 2/3 of the total drive integrity
    public int getKFHeliumTankIntegrity() {
        return helium_tankage;
    }
    
    //Record a hit on the KF Drive Helium Tank
    public void setKFHeliumTankHit(boolean hit) {
        heliumTankHit = hit;
    }
    
    //Return the status of the KF Drive Helium Tank
    public boolean getKFHeliumTankHit() {
        return heliumTankHit;
    }
    
    //Record a hit on the KF Drive Coil
    public void setKFDriveCoilHit(boolean hit) {
        driveCoilHit = hit;
    }
    
    //Return the status of the KF Drive Coil
    public boolean getKFDriveCoilHit() {
        return driveCoilHit;
    }
    
    //Record a hit on the KF Field Initiator
    public void setKFFieldInitiatorHit(boolean hit) {
        fieldInitiatorHit = hit;
    }
    
    //Return the status of the KF Field Initiator
    public boolean getKFFieldInitiatorHit() {
        return fieldInitiatorHit;
    }
    
    //Record a hit on the KF Charging System
    public void setKFChargingSystemHit(boolean hit) {
        chargingSystemHit = hit;
    }
    
    //Return the status of the KF Charging System
    public boolean getKFChargingSystemHit() {
        return chargingSystemHit;
    }
    
    //Record a hit on the KF Drive Controller
    public void setKFDriveControllerHit(boolean hit) {
        driveControllerHit = hit;
    }
    
    //Return the status of the KF Drive Controller
    public boolean getKFDriveControllerHit() {
        return driveControllerHit;
    }
    
    //Return the status of the LF Battery
    public boolean getLFBatteryHit() {
        return lfBatteryHit;
    }
    
    //Record a hit on the LF Battery
    public void setLFBatteryHit(boolean hit) {
        lfBatteryHit = hit;
    }
    
    //Set the original/undamaged Jump Sail integrity
    public void setOSailIntegrity(int sail) {
        original_sail_integrity = sail;
    }
    
    //Return the original/undamaged Jump Sail integrity
    public int getOSailIntegrity() {
        return original_sail_integrity;
    }
    
    //Return the damage taken to the Jump Sail
    public int getSailDamage() {
        return (getOSailIntegrity() - getSailIntegrity());
    }

    //Set the current integrity of the jump sail
    public void setSailIntegrity(int sail) {
        sail_integrity = sail;
    }
    
    //Return the current integrity of the jump sail
    public int getSailIntegrity() {
        return sail_integrity;
    }
    
    /**
     * @return Whether this ship has a jump sail (optional on space stations and primitive jumpships)
     */
    public boolean hasSail() {
        return sail;
    }
    
    /**
     * @param sail Whether this ship has an energy collection sail
     */
    public void setSail(boolean sail) {
        this.sail = sail;
    }

    public void initializeSailIntegrity() {
        int integrity = 1 + (int) Math.ceil((30.0 + (weight / 7500.0)) / 20.0);
        setOSailIntegrity(integrity);
        setSailIntegrity(integrity);
    }

    public void initializeKFIntegrity() {
        int integrity = (int) Math.ceil(1.2 + (getJumpDriveWeight() / 60000.0));
        setOKFIntegrity(integrity);
        setKFIntegrity(integrity);
        //Helium Tanks make up about 2/3 of the drive core. 
        setKFHeliumTankIntegrity((int) (integrity * 0.67));
    }

    public boolean canJump() {
        return kf_integrity > 0;
    }
    
    public int getDriveCoreType() {
        return driveCoreType;
    }
    
    public void setDriveCoreType(int driveCoreType) {
        this.driveCoreType = driveCoreType;
    }

    /**
     * Get maximum range of a jump
     */
    public int getJumpRange() {
        return jumpRange;
    }
    
    /**
     * Set maximum jump range (used for primitive jumpships)
     */
    public void setJumpRange(int range) {
        jumpRange = range;
    }
    
    /**
     * @return The weight of the jump drive core for this unit
     */
    public double getJumpDriveWeight() {
        double pct = DRIVE_CORE_WEIGHT_PCT[driveCoreType];
        if (driveCoreType == DRIVE_CORE_PRIMITIVE) {
            pct = 0.05 + 0.03 * jumpRange;
        }
        return Math.ceil(getWeight() * pct); 
    }

    // different firing arcs
    // different firing arcs
    @Override
    public int getWeaponArc(int wn) {
        final Mounted mounted = getEquipment(wn);

        int arc = Compute.ARC_NOSE;
        switch (mounted.getLocation()) {
            case LOC_NOSE:
                if (mounted.isInWaypointLaunchMode()) {
                    arc = Compute.ARC_NOSE_WPL;
                    break;
                }
                arc = Compute.ARC_NOSE;
                break;
            case LOC_FRS:
                if (mounted.isInWaypointLaunchMode()) {
                    arc = Compute.ARC_RIGHTSIDE_SPHERE_WPL;
                    break;
                }
                arc = Compute.ARC_RIGHTSIDE_SPHERE;
                break;
            case LOC_FLS:
                if (mounted.isInWaypointLaunchMode()) {
                    arc = Compute.ARC_LEFTSIDE_SPHERE_WPL;
                    break;
                }
                arc = Compute.ARC_LEFTSIDE_SPHERE;
                break;
            case LOC_ARS:
                if (mounted.isInWaypointLaunchMode()) {
                    arc = Compute.ARC_RIGHTSIDEA_SPHERE_WPL;
                    break;
                }
                arc = Compute.ARC_RIGHTSIDEA_SPHERE;
                break;
            case LOC_ALS:
                if (mounted.isInWaypointLaunchMode()) {
                    arc = Compute.ARC_LEFTSIDEA_SPHERE_WPL;
                    break;
                }
                arc = Compute.ARC_LEFTSIDEA_SPHERE;
                break;
            case LOC_AFT:
                if (mounted.isInWaypointLaunchMode()) {
                    arc = Compute.ARC_AFT_WPL;
                    break;
                }
                arc = Compute.ARC_AFT;
                break;
            default:
                arc = Compute.ARC_360;
                break;
        }
        return rollArcs(arc);
    }

    // different hit locations
    @Override
    public HitData rollHitLocation(int table, int side) {

        /*
         * Unlike other units, ASFs determine potential crits based on the
         * to-hit roll so I need to set this potential value as well as return
         * the to hit data
         */

        int roll = Compute.d6(2);
        if (side == ToHitData.SIDE_FRONT) {
            // normal front hits
            switch (roll) {
                case 2:
                    setPotCrit(CRIT_LIFE_SUPPORT);
                    return new HitData(LOC_NOSE, false, HitData.EFFECT_NONE);
                case 3:
                    setPotCrit(CRIT_CONTROL);
                    return new HitData(LOC_NOSE, false, HitData.EFFECT_NONE);
                case 4:
                    setPotCrit(CRIT_WEAPON);
                    return new HitData(LOC_FRS, false, HitData.EFFECT_NONE);
                case 5:
                    setPotCrit(CRIT_RIGHT_THRUSTER);
                    return new HitData(LOC_FRS, false, HitData.EFFECT_NONE);
                case 6:
                    setPotCrit(CRIT_CIC);
                    return new HitData(LOC_NOSE, false, HitData.EFFECT_NONE);
                case 7:
                    setPotCrit(CRIT_WEAPON);
                    return new HitData(LOC_NOSE, false, HitData.EFFECT_NONE);
                case 8:
                    setPotCrit(CRIT_SENSOR);
                    return new HitData(LOC_NOSE, false, HitData.EFFECT_NONE);
                case 9:
                    setPotCrit(CRIT_LEFT_THRUSTER);
                    return new HitData(LOC_FLS, false, HitData.EFFECT_NONE);
                case 10:
                    setPotCrit(CRIT_WEAPON);
                    return new HitData(LOC_FLS, false, HitData.EFFECT_NONE);
                case 11:
                    setPotCrit(CRIT_CREW);
                    return new HitData(LOC_NOSE, false, HitData.EFFECT_NONE);
                case 12:
                    setPotCrit(CRIT_KF_DRIVE);
                    return new HitData(LOC_NOSE, false, HitData.EFFECT_NONE);
            }
        } else if (side == ToHitData.SIDE_LEFT) {
            // normal left-side hits
            switch (roll) {
                case 2:
                    setPotCrit(CRIT_AVIONICS);
                    return new HitData(LOC_NOSE, false, HitData.EFFECT_NONE);
                case 3:
                    setPotCrit(CRIT_SENSOR);
                    return new HitData(LOC_FLS, false, HitData.EFFECT_NONE);
                case 4:
                    setPotCrit(CRIT_WEAPON);
                    return new HitData(LOC_FLS, false, HitData.EFFECT_NONE);
                case 5:
                    setPotCrit(CRIT_DOCK_COLLAR);
                    return new HitData(LOC_FLS, false, HitData.EFFECT_NONE);
                case 6:
                    setPotCrit(CRIT_KF_DRIVE);
                    return new HitData(LOC_FLS, false, HitData.EFFECT_NONE);
                case 7:
                    setPotCrit(CRIT_WEAPON_BROAD);
                    return new HitData(LOC_ALS, false, HitData.EFFECT_NONE);
                case 8:
                    setPotCrit(CRIT_GRAV_DECK);
                    return new HitData(LOC_ALS, false, HitData.EFFECT_NONE);
                case 9:
                    setPotCrit(CRIT_DOOR);
                    return new HitData(LOC_ALS, false, HitData.EFFECT_NONE);
                case 10:
                    setPotCrit(CRIT_WEAPON);
                    return new HitData(LOC_ALS, false, HitData.EFFECT_NONE);
                case 11:
                    setPotCrit(CRIT_CARGO);
                    return new HitData(LOC_AFT, false, HitData.EFFECT_NONE);
                case 12:
                    setPotCrit(CRIT_ENGINE);
                    return new HitData(LOC_AFT, false, HitData.EFFECT_NONE);
            }
        } else if (side == ToHitData.SIDE_RIGHT) {
            // normal left-side hits
            switch (roll) {
                case 2:
                    setPotCrit(CRIT_AVIONICS);
                    return new HitData(LOC_NOSE, false, HitData.EFFECT_NONE);
                case 3:
                    setPotCrit(CRIT_SENSOR);
                    return new HitData(LOC_FRS, false, HitData.EFFECT_NONE);
                case 4:
                    setPotCrit(CRIT_WEAPON);
                    return new HitData(LOC_FRS, false, HitData.EFFECT_NONE);
                case 5:
                    setPotCrit(CRIT_DOCK_COLLAR);
                    return new HitData(LOC_FRS, false, HitData.EFFECT_NONE);
                case 6:
                    setPotCrit(CRIT_KF_DRIVE);
                    return new HitData(LOC_FRS, false, HitData.EFFECT_NONE);
                case 7:
                    setPotCrit(CRIT_WEAPON_BROAD);
                    return new HitData(LOC_ARS, false, HitData.EFFECT_NONE);
                case 8:
                    setPotCrit(CRIT_GRAV_DECK);
                    return new HitData(LOC_ARS, false, HitData.EFFECT_NONE);
                case 9:
                    setPotCrit(CRIT_DOOR);
                    return new HitData(LOC_ARS, false, HitData.EFFECT_NONE);
                case 10:
                    setPotCrit(CRIT_WEAPON);
                    return new HitData(LOC_ARS, false, HitData.EFFECT_NONE);
                case 11:
                    setPotCrit(CRIT_CARGO);
                    return new HitData(LOC_AFT, false, HitData.EFFECT_NONE);
                case 12:
                    setPotCrit(CRIT_ENGINE);
                    return new HitData(LOC_AFT, false, HitData.EFFECT_NONE);
            }
        } else if (side == ToHitData.SIDE_REAR) {
            // normal aft hits
            switch (roll) {
                case 2:
                    setPotCrit(CRIT_FUEL_TANK);
                    return new HitData(LOC_AFT, false, HitData.EFFECT_NONE);
                case 3:
                    setPotCrit(CRIT_AVIONICS);
                    return new HitData(LOC_AFT, false, HitData.EFFECT_NONE);
                case 4:
                    setPotCrit(CRIT_WEAPON);
                    return new HitData(LOC_ARS, false, HitData.EFFECT_NONE);
                case 5:
                    setPotCrit(CRIT_RIGHT_THRUSTER);
                    return new HitData(LOC_ARS, false, HitData.EFFECT_NONE);
                case 6:
                    setPotCrit(CRIT_ENGINE);
                    return new HitData(LOC_AFT, false, HitData.EFFECT_NONE);
                case 7:
                    setPotCrit(CRIT_WEAPON);
                    return new HitData(LOC_AFT, false, HitData.EFFECT_NONE);
                case 8:
                    setPotCrit(CRIT_ENGINE);
                    return new HitData(LOC_AFT, false, HitData.EFFECT_NONE);
                case 9:
                    setPotCrit(CRIT_LEFT_THRUSTER);
                    return new HitData(LOC_ALS, false, HitData.EFFECT_NONE);
                case 10:
                    setPotCrit(CRIT_WEAPON);
                    return new HitData(LOC_ALS, false, HitData.EFFECT_NONE);
                case 11:
                    setPotCrit(CRIT_CONTROL);
                    return new HitData(LOC_AFT, false, HitData.EFFECT_NONE);
                case 12:
                    setPotCrit(CRIT_KF_DRIVE);
                    return new HitData(LOC_AFT, false, HitData.EFFECT_NONE);
            }
        }
        return new HitData(LOC_NOSE, false, HitData.EFFECT_NONE);
    }

    @Override
    public int getMaxEngineHits() {
        return 6;
    }

    public int getArcswGuns() {
        // return the number
        int nArcs = 0;
        for (int i = 0; i < locations(); i++) {
            if (hasWeaponInArc(i)) {
                nArcs++;
            }
        }
        return nArcs;
    }

    public boolean hasWeaponInArc(int loc) {
        boolean hasWeapons = false;
        for (Mounted weap : getWeaponList()) {
            if (weap.getLocation() == loc) {
                hasWeapons = true;
            }
        }
        return hasWeapons;
    }

    public double getFuelPerTon() {

        double points = 10.0;

        if (weight >= 250000) {
            points = 2.5;
            return points;
        } else if (weight >= 110000) {
            points = 5.0;
            return points;
        }

        return points;
    }
    
    @Override
    public double getArmorWeight() {
        return getArmorWeight(locations() - 1); // no armor in hull location
    }
    
    @Override
    public double getArmorWeight(int locCount) {
        double armorPoints = getTotalOArmor();

        if (!isPrimitive()) {
            armorPoints -= Math.round(get0SI() / 10.0) * locCount;
        } else {
            armorPoints -= Math.floor(Math.round(get0SI() / 10.0) * locCount * 0.66);
            armorPoints = Math.ceil(armorPoints / 0.66);
        }

        // now I need to determine base armor points by type and weight
        boolean clan = TechConstants.isClan(getArmorTechLevel(firstArmorIndex()));
        double baseArmor = clan ? 1.0 : 0.8;

        if (weight >= 250000) {
            baseArmor = clan ? 0.5 : 0.4;
        } else if (weight >= 150000) {
            baseArmor = clan ? 0.7 : 0.6;
        }

        if (armorType[0] == EquipmentType.T_ARMOR_LC_FERRO_IMP) {
            baseArmor += 0.2;
        } else if (armorType[0] == EquipmentType.T_ARMOR_LC_FERRO_CARBIDE) {
            baseArmor += 0.4;
        } else if (armorType[0] == EquipmentType.T_ARMOR_LC_LAMELLOR_FERRO_CARBIDE) {
            baseArmor += 0.6;
        }

        return RoundWeight.standard(armorPoints / baseArmor, this);
    }

    @Override
    public double getCost(CalculationReport calcReport, boolean ignoreAmmo) {
        return JumpShipCostCalculator.calculateCost(this, calcReport, ignoreAmmo);
    }

    @Override
    public double getPriceMultiplier() {
        return 1.25;
    }


    @Override
    public boolean doomedOnGround() {
        return true;
    }

    @Override
    public boolean doomedInAtmosphere() {
        return true;
    }

    @Override
    public boolean doomedInSpace() {
        return false;
    }

    /**
     * need to check bay location before loading ammo
     */
    @Override
    public boolean loadWeapon(Mounted mounted, Mounted mountedAmmo) {
        boolean success = false;
        WeaponType wtype = (WeaponType) mounted.getType();
        AmmoType atype = (AmmoType) mountedAmmo.getType();

        if (mounted.getLocation() != mountedAmmo.getLocation()) {
            return success;
        }

        // for large craft, ammo must be in the same ba
        Mounted bay = whichBay(getEquipmentNum(mounted));
        if ((bay != null) && !bay.ammoInBay(getEquipmentNum(mountedAmmo))) {
            return success;
        }

        if (mountedAmmo.isAmmoUsable() && !wtype.hasFlag(WeaponType.F_ONESHOT)
                && (atype.getAmmoType() == wtype.getAmmoType()) && (atype.getRackSize() == wtype.getRackSize())) {
            mounted.setLinked(mountedAmmo);
            success = true;
        }
        return success;
    }

    /*
     * (non-Javadoc)
     *
     * @see megamek.common.Entity#getIniBonus()
     */
    @Override
    public int getHQIniBonus() {
        // large craft are considered to have > 7 tons comm equipment
        // hence they get +2 ini bonus as a mobile hq
        return 2;
    }

    /**
     * what location is opposite the given one
     */
    @Override
    public int getOppositeLocation(int loc) {
        switch (loc) {
            case LOC_NOSE:
                return LOC_AFT;
            case LOC_FLS:
                return LOC_ARS;
            case LOC_FRS:
                return LOC_ALS;
            case LOC_ALS:
                return LOC_FRS;
            case LOC_ARS:
                return LOC_FLS;
            case LOC_AFT:
            default:
                return LOC_NOSE;
        }
    }

    /**
     * All military jumpships automatically have ECM if in space
     */
    @Override
    public boolean hasActiveECM() {
        if (!game.getOptions().booleanOption(OptionsConstants.ADVAERORULES_STRATOPS_ECM)
                || !game.getBoard().inSpace()) {
            return super.hasActiveECM();
        }
        return getECMRange() >= 0;
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
        if (!isMilitary()) {
            return Entity.NONE;
        }
        int range = 1;
        // the range might be affected by sensor/FCS damage
        range = range - getSensorHits() - getCICHits();
        return range;
    }

    /**
     * @return is the crew of this vessel protected from gravitational effects,
     *         see StratOps, pg. 36
     */
    @Override
    public boolean isCrewProtected() {
        return isMilitary() && (getOriginalWalkMP() > 4);
    }

    public double getAccumulatedThrust() {
        return accumulatedThrust;
    }

    public void setAccumulatedThrust(double d) {
        accumulatedThrust = d;
    }

    public double getStationKeepingThrust() {
        return stationThrust;
    }

    @Override
    public void newRound(int roundNumber) {
        super.newRound(roundNumber);

        // accumulate some more 
        // We assume that  will be accumulated. If this is proven wrong by
        // the movement
        // then we make the proper adjustments in server#processMovement
        // until I hear from Welshman, I am assuming that you cannot "hold back"
        // thrust. So once you
        // get 1 thrust point, you have to spend it before you can accumulate
        // more
        if (isDeployed() && (isBattleStation() == true)) {
            setAccumulatedThrust(1);
        }

        if (isDeployed() && (getAccumulatedThrust() < 1.0)) {
            setAccumulatedThrust(getAccumulatedThrust() + stationThrust);
        }
    }

    @Override
    public int getRunMP(boolean gravity, boolean ignoreheat, boolean ignoremodulararmor) {
        if (!hasStationKeepingDrive()) {
            return super.getRunMP(gravity, ignoreheat, ignoremodulararmor);
        }
        return (int) Math.floor(getAccumulatedThrust());
    }
    
    /**
     * @return Whether this ship has station-keeping drive instead of transit drive.
     */
    public boolean hasStationKeepingDrive() {
        return walkMP == 0;
    }

    /**
     * find the adjacent firing arc on this vessel clockwise
     */
    public int getAdjacentArcCW(int arc) {
        switch (arc) {
            case Compute.ARC_NOSE:
                return Compute.ARC_RIGHTSIDE_SPHERE;
            case Compute.ARC_LEFTSIDE_SPHERE:
                return Compute.ARC_NOSE;
            case Compute.ARC_RIGHTSIDE_SPHERE:
                return Compute.ARC_RIGHTSIDEA_SPHERE;
            case Compute.ARC_LEFTSIDEA_SPHERE:
                return Compute.ARC_LEFTSIDE_SPHERE;
            case Compute.ARC_RIGHTSIDEA_SPHERE:
                return Compute.ARC_AFT;
            case Compute.ARC_AFT:
                return Compute.ARC_LEFTSIDEA_SPHERE;
            default:
                return Integer.MIN_VALUE;
        }
    }

    /**
     * find the adjacent firing arc on this vessel counter-clockwise
     */
    public int getAdjacentArcCCW(int arc) {
        switch (arc) {
            case Compute.ARC_NOSE:
                return Compute.ARC_LEFTSIDE_SPHERE;
            case Compute.ARC_RIGHTSIDE_SPHERE:
                return Compute.ARC_NOSE;
            case Compute.ARC_LEFTSIDE_SPHERE:
                return Compute.ARC_LEFTSIDEA_SPHERE;
            case Compute.ARC_LEFTSIDEA_SPHERE:
                return Compute.ARC_AFT;
            case Compute.ARC_RIGHTSIDEA_SPHERE:
                return Compute.ARC_RIGHTSIDE_SPHERE;
            case Compute.ARC_AFT:
                return Compute.ARC_RIGHTSIDEA_SPHERE;
            default:
                return Integer.MIN_VALUE;
        }
    }

    /**
     * Finds the arc on the opposite side of the ship. Used in BV calculations.
     * 
     * @param arc A firing arc constant from <code>Compute</code>
     * @return    The arc on the opposite side of the ship.
     */
    public int getOppositeArc(int arc) {
        switch (arc) {
            case Compute.ARC_NOSE:
                return Compute.ARC_AFT;
            case Compute.ARC_LEFTSIDE_SPHERE:
                return Compute.ARC_RIGHTSIDEA_SPHERE;
            case Compute.ARC_RIGHTSIDE_SPHERE:
                return Compute.ARC_LEFTSIDEA_SPHERE;
            case Compute.ARC_LEFTSIDEA_SPHERE:
                return Compute.ARC_RIGHTSIDE_SPHERE;
            case Compute.ARC_RIGHTSIDEA_SPHERE:
                return Compute.ARC_LEFTSIDE_SPHERE;
            case Compute.ARC_LEFT_BROADSIDE:
                return Compute.ARC_RIGHT_BROADSIDE;
            case Compute.ARC_RIGHT_BROADSIDE:
                return Compute.ARC_LEFT_BROADSIDE;
            case Compute.ARC_AFT:
                return Compute.ARC_NOSE;
            default:
                return Integer.MIN_VALUE;
        }
    }

    @Override
    public double getBVTypeModifier() {
        return 0.75;
    }

    @Override
    public boolean usesWeaponBays() {
        return true;
    }

    @Override
    public boolean isFighter() {
        return false;
    }
    
    @Override
    public boolean isPrimitive() {
        return getDriveCoreType() == DRIVE_CORE_PRIMITIVE;
    }

    @Override
    public long getEntityType() {
        return Entity.ETYPE_AERO | Entity.ETYPE_JUMPSHIP;
    }

    /*
     * Do not recalculate walkMP when adding engine.
     */
    @Override
    protected int calculateWalk() {
        return walkMP;
    }
}

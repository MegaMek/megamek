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
import megamek.common.cost.DropShipCostCalculator;
import megamek.common.options.OptionsConstants;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Vector;

/**
 * @author Jay Lawson
 * @since Jun 17, 2007
 */
public class Dropship extends SmallCraft {
    private static final long serialVersionUID = 1528728632696989565L;

    // ASEW Missile Effects, per location
    // Values correspond to Locations: NOS, Left, Right, AFT
    private int[] asewAffectedTurns = { 0, 0, 0, 0 };

    /**
     * Sets the number of rounds a specified firing arc is affected by an ASEW missile
     * @param arc - integer representing the desired firing arc
     * @param turns - integer specifying the number of end phases that the effects last through
     * Technically, about 1.5 turns elapse per the rules for ASEW missiles in TO
     */
    public void setASEWAffected(int arc, int turns) {
        if (arc < asewAffectedTurns.length) {
            asewAffectedTurns[arc] = turns;
        }
    }

    /**
     * Returns the number of rounds a specified firing arc is affected by an ASEW missile
     * @param arc - integer representing the desired firing arc
     */
    public int getASEWAffected(int arc) {
        if (arc < asewAffectedTurns.length) {
            return asewAffectedTurns[arc];
        }
        return 0;
    }

    /**
     * Primitive DropShips may be constructed with no docking collar, or with a pre-boom collar.
     */
    public static final int COLLAR_STANDARD  = 0;
    public static final int COLLAR_PROTOTYPE = 1;
    public static final int COLLAR_NO_BOOM   = 2;

    private static final String[] COLLAR_NAMES = {
            "KF-Boom", "Prototype KF-Boom", "No Boom"
    };

    // Likewise, you can have a prototype or standard K-F Boom
    public static final int BOOM_STANDARD  = 0;
    public static final int BOOM_PROTOTYPE = 1;

    // what needs to go here?
    // loading and unloading of units?
    private boolean dockCollarDamaged = false;
    private boolean kfBoomDamaged = false;
    private int collarType = COLLAR_STANDARD;
    private int boomType = BOOM_STANDARD;

    @Override
    public boolean tracksHeat() {
        // While large craft perform heat calculations, they are not considered heat-tracking units
        // because they cannot generate more heat than they can dissipate in the same turn.
        return false;
    }

    @Override
    public int getUnitType() {
        return UnitType.DROPSHIP;
    }

    @Override
    public boolean isSmallCraft() {
        return false;
    }

    @Override
    public boolean isDropShip() {
        return true;
    }

    @Override
    public CrewType defaultCrewType() {
        return CrewType.VESSEL;
    }

    //Docking Collar Stuff
    public boolean isDockCollarDamaged() {
        return dockCollarDamaged;
    }

    public int getCollarType() {
        return collarType;
    }

    public void setCollarType(int collarType) {
        this.collarType = collarType;
    }

    public String getCollarName() {
        return COLLAR_NAMES[collarType];
    }

    public static String getCollarName(int type) {
        return COLLAR_NAMES[type];
    }

    public static TechAdvancement getCollarTA() {
        return new TechAdvancement(TECH_BASE_ALL).setAdvancement(2458, 2470, 2500)
                .setPrototypeFactions(F_TH).setProductionFactions(F_TH).setTechRating(RATING_C)
                .setAvailability(RATING_C, RATING_C, RATING_C, RATING_C)
                .setStaticTechLevel(SimpleTechLevel.STANDARD);
    }

    //KF Boom Stuff
    public boolean isKFBoomDamaged() {
        return kfBoomDamaged;
    }

    public int getBoomType() {
        return boomType;
    }

    public void setBoomType(int boomType) {
        this.boomType = boomType;
    }

    @Override
    public String getCritDamageString() {
        StringBuilder toReturn = new StringBuilder(super.getCritDamageString());
        boolean first = toReturn.length() == 0;
        if (isDockCollarDamaged()) {
            if (!first) {
                toReturn.append(", ");
            }
            toReturn.append(Messages.getString("Dropship.collarDamageString"));
            first = false;
        }
        if (isKFBoomDamaged()) {
            if (!first) {
                toReturn.append(", ");
            }
            toReturn.append(Messages.getString("Dropship.kfBoomDamageString"));
            first = false;
        }
        return toReturn.toString();
    }

    @Override
    public boolean isLocationProhibited(Coords c, int currElevation) {
        Hex hex = game.getBoard().getHex(c);
        if (isAirborne()) {
            return hex.containsTerrain(Terrains.IMPASSABLE);
        }
        // Check prohibited terrain
        // treat grounded Dropships like wheeled tanks,
        // plus buildings are prohibited
        boolean isProhibited = hexContainsProhibitedTerrain(hex);

        HashMap<Integer, Integer> elevations = new HashMap<>();
        elevations.put(hex.getLevel(), 1);
        for (int dir = 0; dir < 6; dir++) {
            Coords secondaryCoord = c.translated(dir);
            Hex secondaryHex = game.getBoard().getHex(secondaryCoord);
            if (secondaryHex == null) {
                // Don't allow landed dropships to hang off the board
                isProhibited = true;
            } else {
                isProhibited |= hexContainsProhibitedTerrain(secondaryHex);

                int elev = secondaryHex.getLevel();
                if (elevations.containsKey(elev)) {
                    elevations.put(elev, elevations.get(elev) + 1);
                } else {
                    elevations.put(elev, 1);
                }
            }
        }
        /*
         * As of 8/2013 there aren't clear restrictions for landed dropships. We
         * are going to assume that Dropships need to be on fairly level
         * terrain. This means, it can only be on at most 2 different elevations
         * that are at most 1 elevation apart. Additionally, at least half of
         * the dropships hexes round down must be on one elevation
         */
        // Whole DS is on one elevation
        if (elevations.size() == 1) {
            return isProhibited;
        }
        // DS on more than 2 different elevations
        // or not on an elevation, what?
        if ((elevations.size() > 2) || elevations.isEmpty()) {
            return true;
        }

        Object[] elevs = elevations.keySet().toArray();
        int elev1 = (Integer) elevs[0];
        int elev2 = (Integer) elevs[1];
        int elevDifference = Math.abs(elev1 - elev2);
        int elevMinCount = 2;
        // Check elevation difference and make sure that the counts of different
        // elevations will allow for a legal deployment to exist
        if ((elevDifference > 1) || (elevations.get(elevs[0]) < elevMinCount)
                || (elevations.get(elevs[1]) < elevMinCount)) {
            return true;
        }

        // It's still possible we have a legal deployment, we now have to check
        // the arrangement of hexes
        // The way this is done is we start at the hex directly above the
        // central hex and then move around clockwise and compare the two hexes
        // to see if they share an elevation. We need to have a number of these
        // adjacencies equal to the number of secondary elevation hexes - 1.
        int numAdjacencies = 0;
        int centralElev = hex.getLevel();
        int secondElev = centralElev;
        Hex currHex = game.getBoard().getHex(c.translated(5));
        // Ensure we aren't trying to deploy off the board
        if (currHex == null) {
            return true;
        }
        for (int dir = 0; dir < 6; dir++) {
            if (currHex.getLevel() != centralElev) {
                secondElev = currHex.getLevel();
            }
            Hex nextHex = game.getBoard().getHex(c.translated(dir));
            // Ensure we aren't trying to deploy off the board
            if (nextHex == null) {
                return true;
            }
            if ((currHex.getLevel() != centralElev) && (currHex.getLevel() == nextHex.getLevel())) {
                numAdjacencies++;
            }
            currHex = nextHex;
        }
        if (numAdjacencies < (elevations.get(secondElev) - 1)) {
            return true;
        }

        return isProhibited;
    }

    /**
     * Worker function that checks if a given hex contains terrain onto which a grounded dropship
     * cannot deploy.
     */
    private boolean hexContainsProhibitedTerrain(Hex hex) {
        return hex.containsTerrain(Terrains.WOODS) || hex.containsTerrain(Terrains.ROUGH)
                || ((hex.terrainLevel(Terrains.WATER) > 0) && !hex.containsTerrain(Terrains.ICE))
                || hex.containsTerrain(Terrains.RUBBLE) || hex.containsTerrain(Terrains.MAGMA)
                || hex.containsTerrain(Terrains.JUNGLE) || (hex.terrainLevel(Terrains.SNOW) > 1)
                || (hex.terrainLevel(Terrains.GEYSER) == 2)
                || hex.containsTerrain(Terrains.BUILDING) || hex.containsTerrain(Terrains.IMPASSABLE)
                || hex.containsTerrain(Terrains.BRIDGE);

    }

    public void setDamageDockCollar(boolean b) {
        dockCollarDamaged = b;
    }

    public void setDamageKFBoom(boolean b) {
        kfBoomDamaged = b;
    }

    @Override
    public double getFuelPointsPerTon() {
        double ppt;
        if (getWeight() < 400) {
            ppt = 80;
        } else if (getWeight() < 800) {
            ppt = 70;
        } else if (getWeight() < 1200) {
            ppt = 60;
        } else if (getWeight() < 1900) {
            ppt = 50;
        } else if (getWeight() < 3000) {
            ppt = 40;
        } else if (getWeight() < 20000) {
            ppt = 30;
        } else if (getWeight() < 40000) {
            ppt = 20;
        } else {
            ppt = 10;
        }
        if (isPrimitive()) {
            return ppt / primitiveFuelFactor();
        }
        return ppt;
    }

    @Override
    public double getStrategicFuelUse() {
        double fuelUse = 1.84; // default for military designs and civilian < 1000
        if ((getDesignType() == CIVILIAN) || isPrimitive()) {
            if (getWeight() >= 70000) {
                fuelUse = 8.83;
            } else if (getWeight() >= 50000) {
                fuelUse = 8.37;
            } else if (getWeight() >= 40000) {
                fuelUse = 7.71;
            } else if (getWeight() >= 30000) {
                fuelUse = 6.52;
            } else if (getWeight() >= 20000) {
                fuelUse = 5.19;
            } else if (getWeight() >= 9000) {
                fuelUse = 4.22;
            } else if (getWeight() >= 4000) {
                fuelUse = 2.82;
            }
        }
        if (isPrimitive()) {
            return fuelUse * primitiveFuelFactor();
        }
        return fuelUse;
    }

    @Override
    public double primitiveFuelFactor() {
        int year = getOriginalBuildYear();
        if (year >= 2500) {
            return 1.0;
        } else if (year >= 2400) {
            return 1.1;
        } else if (year >= 2351) {
            return 1.3;
        } else if (year >= 2251) {
            return 1.4;
        } else if (year >= 2201) {
            return 1.6;
        } else if (year >= 2151) {
            return 1.8;
        } else {
            return 2.0;
        }
    }

    protected static final TechAdvancement TA_DROPSHIP = new TechAdvancement(TECH_BASE_ALL)
            .setAdvancement(DATE_NONE, 2470, 2490).setISApproximate(false, true, false)
            .setProductionFactions(F_TH).setTechRating(RATING_D)
            .setAvailability(RATING_D, RATING_E, RATING_D, RATING_D)
            .setStaticTechLevel(SimpleTechLevel.STANDARD);
    protected static final TechAdvancement TA_DROPSHIP_PRIMITIVE = new TechAdvancement(TECH_BASE_IS)
            .setISAdvancement(DATE_ES, 2200, DATE_NONE, 2500)
            .setISApproximate(false, true, false, false)
            .setProductionFactions(F_TA).setTechRating(RATING_D)
            .setAvailability(RATING_D, RATING_X, RATING_X, RATING_X)
            .setStaticTechLevel(SimpleTechLevel.STANDARD);

    @Override
    public TechAdvancement getConstructionTechAdvancement() {
        return isPrimitive() ? TA_DROPSHIP_PRIMITIVE : TA_DROPSHIP;
    }

    @Override
    protected void addSystemTechAdvancement(CompositeTechLevel ctl) {
        super.addSystemTechAdvancement(ctl);
        if (collarType != COLLAR_NO_BOOM) {
            ctl.addComponent(getCollarTA());
        }
    }

    @Override
    public double getCost(CalculationReport calcReport, boolean ignoreAmmo) {
        return DropShipCostCalculator.calculateCost(this, calcReport, ignoreAmmo);
    }

    @Override
    public double getPriceMultiplier() {
        return isSpheroid() ? 28.0 : 36.0;
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

        // for large craft, ammo must be in the same bay
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
     * All military dropships automatically have ECM if in space
     */
    @Override
    public boolean hasActiveECM() {
        if (!game.getOptions().booleanOption(OptionsConstants.ADVAERORULES_STRATOPS_ECM)
                || !game.getBoard().inSpace()) {
            return super.hasActiveECM();
        }
        return getECMRange() > Entity.NONE;
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
        range = range - getFCSHits() - getSensorHits();
        return range;
    }

    /**
     * Return the height of this dropship above the terrain.
     */
    @Override
    public int height() {
        if (isAirborne()) {
            return 0;
        }
        if (isSpheroid()) {
            return 9;
        }
        return 4;
    }

    @Override
    public int getWalkMP(MPCalculationSetting mpCalculationSetting) {
        // A grounded dropship with the center hex in level 1 water is immobile.
        if ((game != null) && !game.getBoard().inSpace() && !isAirborne()) {
            Hex hex = game.getBoard().getHex(getPosition());
            if ((hex != null) && (hex.containsTerrain(Terrains.WATER, 1) && !hex.containsTerrain(Terrains.ICE))) {
                return 0;
            }
        }
        return super.getWalkMP(mpCalculationSetting);
    }

    /*
     * (non-Javadoc)
     *
     * @see megamek.common.Entity#setPosition(megamek.common.Coords)
     */
    @Override

    public void setPosition(Coords position) {
        HashSet<Coords> oldPositions = getOccupiedCoords();
        super.setPosition(position, false);
        if ((getAltitude() == 0) && (null != game) && !game.getBoard().inSpace() && (position != null)) {
            secondaryPositions.put(0, position);
            secondaryPositions.put(1, position.translated(getFacing()));
            secondaryPositions.put(2, position.translated((getFacing() + 1) % 6));
            secondaryPositions.put(3, position.translated((getFacing() + 2) % 6));
            secondaryPositions.put(4, position.translated((getFacing() + 3) % 6));
            secondaryPositions.put(5, position.translated((getFacing() + 4) % 6));
            secondaryPositions.put(6, position.translated((getFacing() + 5) % 6));
        }
        if (game != null) {
            game.updateEntityPositionLookup(this, oldPositions);
        }
    }

    @Override
    public void setAltitude(int altitude) {
        super.setAltitude(altitude);
        if ((getAltitude() == 0) && (game != null) && !game.getBoard().inSpace() && (getPosition() != null)) {
            secondaryPositions.put(0, getPosition());
            secondaryPositions.put(1, getPosition().translated(getFacing()));
            secondaryPositions.put(2, getPosition().translated((getFacing() + 1) % 6));
            secondaryPositions.put(3, getPosition().translated((getFacing() + 2) % 6));
            secondaryPositions.put(4, getPosition().translated((getFacing() + 3) % 6));
            secondaryPositions.put(5, getPosition().translated((getFacing() + 4) % 6));
            secondaryPositions.put(6, getPosition().translated((getFacing() + 5) % 6));
        }
    }

    /*
     * (non-Javadoc)
     *
     * @see megamek.common.Entity#setFacing(int)
     */
    @Override
    public void setFacing(int facing) {
        super.setFacing(facing);
        setPosition(getPosition());
    }

    @Override
    public int getLandingLength() {
        return 15;
    }

    @Override
    public String hasRoomForVerticalLanding() {
        // dropships can land just about anywhere they want, unless it is off
        // the map
        Vector<Coords> positions = new Vector<>();
        positions.add(getPosition());
        for (int i = 0; i < 6; i++) {
            positions.add(getPosition().translated(i));
        }
        for (Coords pos : positions) {
            Hex hex = game.getBoard().getHex(getPosition());
            hex = game.getBoard().getHex(pos);
            // if the hex is null, then we are offboard. Don't let units
            // land offboard.
            if (null == hex) {
                return "landing area not on the map";
            }
            if (hex.containsTerrain(Terrains.WATER)) {
                return "cannot land on water";
            }
        }
        // TODO: what about other terrain (like jungles)?
        return null;
    }

    @Override
    public boolean usesWeaponBays() {
        if (null == game) {
            return true;
        }
        return (isAirborne() || isSpaceborne() || game.getPhase().isLounge());
    }

    @Override
    public HitData rollHitLocation(int table, int side) {
        if ((table == ToHitData.HIT_KICK) || (table == ToHitData.HIT_PUNCH)) {
            // we don't really have any good rules on how to apply this,
            // I have a rules question posted about it:
            // http://bg.battletech.com/forums/index.php/topic,24077.new.html#new
            // in the meantime lets make up our own hit table (fun!)
            int roll = Compute.d6(2);
            if (side == ToHitData.SIDE_LEFT) {
                // normal left-side hits
                switch (roll) {
                    case 2:
                        setPotCrit(CRIT_GEAR);
                        return new HitData(LOC_AFT, false, HitData.EFFECT_NONE);
                    case 3:
                        setPotCrit(CRIT_LIFE_SUPPORT);
                        return new HitData(LOC_AFT, false, HitData.EFFECT_NONE);
                    case 4:
                        setPotCrit(CRIT_DOCK_COLLAR);
                        return new HitData(LOC_AFT, false, HitData.EFFECT_NONE);
                    case 5:
                        setPotCrit(CRIT_LEFT_THRUSTER);
                        return new HitData(LOC_LWING, false, HitData.EFFECT_NONE);
                    case 6:
                        setPotCrit(CRIT_CARGO);
                        return new HitData(LOC_LWING, false, HitData.EFFECT_NONE);
                    case 7:
                        setPotCrit(CRIT_WEAPON);
                        return new HitData(LOC_LWING, false, HitData.EFFECT_NONE);
                    case 8:
                        setPotCrit(CRIT_DOOR);
                        return new HitData(LOC_LWING, false, HitData.EFFECT_NONE);
                    case 9:
                        setPotCrit(CRIT_LEFT_THRUSTER);
                        return new HitData(LOC_LWING, false, HitData.EFFECT_NONE);
                    case 10:
                        setPotCrit(CRIT_AVIONICS);
                        return new HitData(LOC_AFT, false, HitData.EFFECT_NONE);
                    case 11:
                        setPotCrit(CRIT_ENGINE);
                        return new HitData(LOC_AFT, false, HitData.EFFECT_NONE);
                    case 12:
                        setPotCrit(CRIT_WEAPON);
                        return new HitData(LOC_AFT, false, HitData.EFFECT_NONE);
                }
            } else {
                switch (roll) {
                    case 2:
                        setPotCrit(CRIT_GEAR);
                        return new HitData(LOC_AFT, false, HitData.EFFECT_NONE);
                    case 3:
                        setPotCrit(CRIT_LIFE_SUPPORT);
                        return new HitData(LOC_AFT, false, HitData.EFFECT_NONE);
                    case 4:
                        setPotCrit(CRIT_DOCK_COLLAR);
                        return new HitData(LOC_AFT, false, HitData.EFFECT_NONE);
                    case 5:
                        setPotCrit(CRIT_RIGHT_THRUSTER);
                        return new HitData(LOC_RWING, false, HitData.EFFECT_NONE);
                    case 6:
                        setPotCrit(CRIT_CARGO);
                        return new HitData(LOC_RWING, false, HitData.EFFECT_NONE);
                    case 7:
                        setPotCrit(CRIT_WEAPON);
                        return new HitData(LOC_RWING, false, HitData.EFFECT_NONE);
                    case 8:
                        setPotCrit(CRIT_DOOR);
                        return new HitData(LOC_RWING, false, HitData.EFFECT_NONE);
                    case 9:
                        setPotCrit(CRIT_RIGHT_THRUSTER);
                        return new HitData(LOC_RWING, false, HitData.EFFECT_NONE);
                    case 10:
                        setPotCrit(CRIT_AVIONICS);
                        return new HitData(LOC_AFT, false, HitData.EFFECT_NONE);
                    case 11:
                        setPotCrit(CRIT_ENGINE);
                        return new HitData(LOC_AFT, false, HitData.EFFECT_NONE);
                    case 12:
                        setPotCrit(CRIT_WEAPON);
                        return new HitData(LOC_AFT, false, HitData.EFFECT_NONE);
                }
            }
            return new HitData(LOC_AFT, false, HitData.EFFECT_NONE);
        } else {
            return super.rollHitLocation(table, side);
        }
    }

    @Override
    public String getLocationAbbr(int loc) {
        if (loc == Entity.LOC_NONE) {
            return "System Wide";
        } else {
            return super.getLocationAbbr(loc);
        }
    }

    @Override
    public long getEntityType() {
        return Entity.ETYPE_AERO | Entity.ETYPE_SMALL_CRAFT | Entity.ETYPE_DROPSHIP;
    }

    @Override
    public boolean canChangeSecondaryFacing() {
        // flying dropships can execute the "ECHO" maneuver (stratops 113), aka a torso twist,
        // if they have the MP for it
        return isAirborne() && !isEvading() && (mpUsed <= getRunMP() - 2);
    }

    /**
     * Can this dropship "torso twist" in the given direction?
     */
    @Override
    public boolean isValidSecondaryFacing(int dir) {
        int rotate = dir - getFacing();
        if (canChangeSecondaryFacing()) {
            return (rotate == 0) || (rotate == 1) || (rotate == -1)
                    || (rotate == -5) || (rotate == 5);
        }
        return rotate == 0;
    }

    /**
     * Return the nearest valid direction to "torso twist" in
     */
    @Override
    public int clipSecondaryFacing(int dir) {
        if (isValidSecondaryFacing(dir)) {
            return dir;
        }

        // can't twist without enough MP
        if (!canChangeSecondaryFacing()) {
            return getFacing();
        }

        // otherwise, twist once in the appropriate direction
        final int rotate = (dir + (6 - getFacing())) % 6;

        return rotate >= 3 ? (getFacing() + 5) % 6 : (getFacing() + 1) % 6;
    }

    @Override
    public void newRound(int roundNumber) {
        super.newRound(roundNumber);

        if (getGame().useVectorMove()) {
            setFacing(getSecondaryFacing());
        }

        setSecondaryFacing(getFacing());
    }

    /**
     * Utility function that handles situations where a facing change
     * has some kind of permanent effect on the entity.
     */
    @Override
    public void postProcessFacingChange() {
        mpUsed += 2;
    }

    /**
     * Depsite being VSTOL in other respects, aerodyne dropships are
     * explicitely forbidden from vertical landings in atmosphere.
     */
    @Override
    public boolean canLandVertically() {
        return isSpheroid() || game.getPlanetaryConditions().isVacuum();
    }

    /**
     * Depsite being VSTOL in other respects, aerodyne dropships are
     * explicitely forbidden from vertical takeoff in atmosphere.
     */
    @Override
    public boolean canTakeOffVertically() {
        return (isSpheroid() || game.getPlanetaryConditions().isVacuum()) && (getCurrentThrust() > 2);
    }

    @Override
    public void autoSetMaxBombPoints() {
        // Only count free whole tons per bay
        maxBombPoints = getTransportBays().stream().mapToInt(
                tb -> (tb instanceof CargoBay) ? (int) Math.floor(tb.getUnused() / 5) : 0
        ).sum();
    }
}

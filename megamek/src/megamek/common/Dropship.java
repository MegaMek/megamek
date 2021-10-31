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
/*
 * Created on Jun 17, 2007
 */
package megamek.common;

import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.TreeMap;
import java.util.Vector;

import megamek.common.enums.GamePhase;
import megamek.common.options.OptionsConstants;
import megamek.common.weapons.bayweapons.BayWeapon;

/**
 * @author Jay Lawson
 */
public class Dropship extends SmallCraft {

    /**
     *
     */
    private static final long serialVersionUID = 1528728632696989565L;
    
    
    //ASEW Missile Effects, per location
    //Values correspond to Locations: NOS,Left,Right,AFT
    private int asewAffectedTurns[] = {0,0,0,0};
    
    /*
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
    
    /*
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
     * Primitive Dropships may be constructed with no docking collar, or with a pre-boom collar. 
     * 
     */
    public static final int COLLAR_STANDARD  = 0;
    public static final int COLLAR_PROTOTYPE = 1;
    public static final int COLLAR_NO_BOOM   = 2;
    
    private static final String[] COLLAR_NAMES = {
            "KF-Boom", "Prototype KF-Boom", "No Boom"
    };
    
    //Likewise, you can have a prototype or standard K-F Boom
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
        IHex hex = game.getBoard().getHex(c);
        if (isAirborne()) {
            if (hex.containsTerrain(Terrains.IMPASSABLE)) {
                return true;
            }
            return false;
        }
        // Check prohibited terrain
        // treat grounded Dropships like wheeled tanks,
        // plus buildings are prohibited
        boolean isProhibited = hexContainsProhibitedTerrain(hex);

        HashMap<Integer, Integer> elevations = new HashMap<Integer, Integer>();
        elevations.put(hex.getLevel(), 1);
        for (int dir = 0; dir < 6; dir++) {
            Coords secondaryCoord = c.translated(dir);
            IHex secondaryHex = game.getBoard().getHex(secondaryCoord);
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
        if ((elevations.size() > 2) || (elevations.size() == 0)) {
            return true;
        }

        Object elevs[] = elevations.keySet().toArray();
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
        IHex currHex = game.getBoard().getHex(c.translated(5));
        // Ensure we aren't trying to deploy off the board
        if (currHex == null) {
            return true;
        }
        for (int dir = 0; dir < 6; dir++) {
            if (currHex.getLevel() != centralElev) {
                secondElev = currHex.getLevel();
            }
            IHex nextHex = game.getBoard().getHex(c.translated(dir));
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
    private boolean hexContainsProhibitedTerrain(IHex hex) {
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
        return isPrimitive()? TA_DROPSHIP_PRIMITIVE : TA_DROPSHIP;
    }
    
    @Override
    protected void addSystemTechAdvancement(CompositeTechLevel ctl) {
        super.addSystemTechAdvancement(ctl);
        if (collarType != COLLAR_NO_BOOM) {
            ctl.addComponent(getCollarTA());
        }
    }
    
    @Override
    public double getCost(boolean ignoreAmmo) {
        double[] costs = new double[20];
        int costIdx = 0;
        double cost = 0;

        // Control Systems
        // Bridge
        costs[costIdx++] += 200000 + (10 * weight);
        // Computer
        costs[costIdx++] += 200000;
        // Life Support
        costs[costIdx++] += 5000 * (getNCrew() + getNPassenger());
        // Sensors
        costs[costIdx++] += 80000;
        // Fire Control Computer
        costs[costIdx++] += 100000;
        // Gunnery Control Systems
        costs[costIdx++] += 10000 * getArcswGuns();

        // Structural Integrity
        costs[costIdx++] += 100000 * getSI();

        // Additional Flight Systems
        // Attitude Thruster
        costs[costIdx++] += 25000;
        // Landing Gear
        costs[costIdx++] += 10 * getWeight();
        // Docking Collar
        if (collarType == COLLAR_STANDARD) {
            costs[costIdx++] += 10000;
        } else if (collarType == COLLAR_PROTOTYPE) {
            costs[costIdx++] += 1010000;
        }

        // Engine
        double engineMultiplier = 0.065;
        if (isClan()) {
            engineMultiplier = 0.061;
        }
        double engineWeight = getOriginalWalkMP() * weight * engineMultiplier;
        costs[costIdx++] += engineWeight * 1000;
        // Drive Unit
        costs[costIdx++] += (500 * getOriginalWalkMP() * weight) / 100.0;

        // Fuel Tanks
        costs[costIdx++] += (200 * getFuel()) / getFuelPointsPerTon() * 1.02;

        // Armor
        costs[costIdx++] += getArmorWeight() * EquipmentType.getArmorCost(armorType[0]);

        // Heat Sinks
        int sinkCost = 2000 + (4000 * getHeatType());// == HEAT_DOUBLE ? 6000:
        // 2000;
        costs[costIdx++] += sinkCost * getHeatSinks();

        // Weapons and Equipment
        costs[costIdx++] += getWeaponsAndEquipmentCost(ignoreAmmo);

        // Transport Bays
        int baydoors = 0;
        long bayCost = 0;
        long quartersCost = 0;
        // Passenger and crew quarters and infantry bays are considered part of the structure
        // and don't add to the cost
        for (Bay next : getTransportBays()) {
            baydoors += next.getDoors();
            if (!next.isQuarters() && !(next instanceof InfantryBay) && !(next instanceof BattleArmorBay)) {
                bayCost += next.getCost();
            }
        }

        costs[costIdx++] += bayCost + (baydoors * 1000L);
        costs[costIdx++] = quartersCost;

        // Life Boats and Escape Pods
        costs[costIdx++] += 5000 * (getLifeBoats() + getEscapePods());

        // TODO Decouple cost calculation from addCostDetails and eliminate duplicate code in getPriceMultiplier
        double weightMultiplier = 36.0;
        if (isSpheroid()) {
            weightMultiplier = 28.0;
        }

        // Sum Costs
        for (int i = 0; i < costIdx; i++) {
            cost += costs[i];
        }

        costs[costIdx++] = -weightMultiplier; // Negative indicates multiplier
        cost = Math.round(cost * weightMultiplier);
        addCostDetails(cost, costs);
        return cost;
    }

    @Override
    public double getPriceMultiplier() {
        return isSpheroid() ? 28.0 : 36.0;
    }

    private void addCostDetails(double cost, double[] costs) {
        bvText = new StringBuffer();
        String[] left = { "Bridge", "Computer", "Life Support", "Sensors", "FCS", "Gunnery Control Systems",
                "Structural Integrity", "Attitude Thruster", "Landing Gear", "Docking Collar",
                "Engine", "Drive Unit", "Fuel Tanks", "Armor", "Heat Sinks", "Weapons/Equipment", "Bays",
                "Quarters", "Life Boats/Escape Pods", "Weight Multiplier" };

        NumberFormat commafy = NumberFormat.getInstance();

        bvText.append("<HTML><BODY><CENTER><b>Cost Calculations For ");
        bvText.append(getChassis());
        bvText.append(" ");
        bvText.append(getModel());
        bvText.append("</b></CENTER>");
        bvText.append(nl);

        bvText.append(startTable);
        // find the maximum length of the columns.
        for (int l = 0; l < left.length; l++) {

            if (l == 14) {
                getWeaponsAndEquipmentCost(true);
            } else {
                bvText.append(startRow);
                bvText.append(startColumn);
                bvText.append(left[l]);
                bvText.append(endColumn);
                bvText.append(startColumn);

                if (costs[l] == 0) {
                    bvText.append("N/A");
                } else if (costs[l] < 0) {
                    bvText.append("x ");
                    bvText.append(commafy.format(-costs[l]));
                } else {
                    bvText.append(commafy.format(costs[l]));

                }
                bvText.append(endColumn);
                bvText.append(endRow);
            }
        }
        bvText.append(startRow);
        bvText.append(startColumn);
        bvText.append(endColumn);
        bvText.append(startColumn);
        bvText.append("-------------");
        bvText.append(endColumn);
        bvText.append(endRow);

        bvText.append(startRow);
        bvText.append(startColumn);
        bvText.append("Total Cost:");
        bvText.append(endColumn);
        bvText.append(startColumn);
        bvText.append(commafy.format(cost));
        bvText.append(endColumn);
        bvText.append(endRow);

        bvText.append(endTable);
        bvText.append("</BODY></HTML>");
    }

    private String getArcName(int loc) {
        if (loc < locations()) {
            return getLocationName(loc);
        }
        return getLocationName(loc - 3) + " (R)";
    }

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

        int modularArmor = 0;
        for (Mounted mounted : getEquipment()) {
            if ((mounted.getType() instanceof MiscType) && mounted.getType().hasFlag(MiscType.F_MODULAR_ARMOR)) {
                modularArmor += mounted.getBaseDamageCapacity() - mounted.getDamageTaken();
            }
        }

        // no use for armor mods right now but in case we need one later
        double armorMod = 1.0;

        bvText.append(startTable);
        bvText.append(startRow);
        bvText.append(startColumn);

        bvText.append("Total Armor Factor x 2.5 x");
        bvText.append(armorMod);
        bvText.append(endColumn);
        bvText.append(startColumn);

        dbv += (getTotalArmor() + modularArmor);

        bvText.append(dbv);
        bvText.append(" x 2.5 x ");
        bvText.append(armorMod);
        bvText.append(endColumn);
        bvText.append(startColumn);
        bvText.append("= ");

        dbv *= 2.5 * armorMod;

        bvText.append(dbv);
        bvText.append(endColumn);
        bvText.append(endRow);

        bvText.append(startRow);
        bvText.append(startColumn);

        bvText.append("Total SI x 2");
        bvText.append(endColumn);
        bvText.append(startColumn);

        double dbvSI = getSI() * 2.0;
        dbv += dbvSI;

        bvText.append(getSI());
        bvText.append(" x 2");
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
            if (etype instanceof BayWeapon) {
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
                // we need to deal with cases where ammo is loaded in multi-ton
                // increments
                // (on dropships and jumpships) - lets take the ratio of shots
                // to shots left
                double ratio = mounted.getUsableShotsLeft() / ((AmmoType) etype).getShots();

                // if the ratio is less than one, we will treat as a full ton
                // since
                // we don't make that adjustment elsewhere
                if (ratio < 1.0) {
                    ratio = 1.0;
                }
                amsAmmoBV += ratio * etype.getBV(this);
                bvText.append(startRow);
                bvText.append(startColumn);
                bvText.append(etype.getName());
                bvText.append(endColumn);
                bvText.append(startColumn);
                bvText.append("+");
                bvText.append(ratio * etype.getBV(this));
                bvText.append(endColumn);
                bvText.append(startColumn);
                bvText.append(endColumn);
                bvText.append(endRow);
            } else if ((etype instanceof AmmoType)
                    && (((AmmoType) etype).getAmmoType() == AmmoType.T_SCREEN_LAUNCHER)) {
                // we need to deal with cases where ammo is loaded in multi-ton
                // increments
                // (on dropships and jumpships) - lets take the ratio of shots
                // to shots left
                double ratio = mounted.getUsableShotsLeft() / ((AmmoType) etype).getShots();

                // if the ratio is less than one, we will treat as a full ton
                // since
                // we don't make that adjustment elsewhere
                if (ratio < 1.0) {
                    ratio = 1.0;
                }
                screenAmmoBV += ratio * etype.getBV(this);
                bvText.append(startRow);
                bvText.append(startColumn);
                bvText.append(etype.getName());
                bvText.append(endColumn);
                bvText.append(startColumn);
                bvText.append("+");
                bvText.append(ratio * etype.getBV(this));
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
            } else if ((etype instanceof MiscType)
                    && (etype.hasFlag(MiscType.F_ECM) || etype.hasFlag(MiscType.F_BAP))) {
                defEqBV += etype.getBV(this);
                bvText.append(startRow);
                bvText.append(startColumn);
                bvText.append(mounted.getName());
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

        // unit type multiplier
        bvText.append(startRow);
        bvText.append(startColumn);
        bvText.append("Multiply by Unit type Modifier");
        bvText.append(endColumn);
        bvText.append(startColumn);
        bvText.append(getBVTypeModifier());
        dbv *= getBVTypeModifier();
        bvText.append(endColumn);
        bvText.append(startColumn);
        bvText.append("x" + getBVTypeModifier());
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
        int aeroHeatEfficiency = getHeatCapacity();

        bvText.append(startRow);
        bvText.append(startColumn);

        bvText.append("Base Heat Efficiency ");

        bvText.append(endColumn);
        bvText.append(startColumn);
        bvText.append(aeroHeatEfficiency);

        bvText.append(endColumn);
        bvText.append(endRow);

        // get arc BV and heat
        // I am going to redo how I do this to cycle through locations, so I can
        // spit it out
        // to bvText. It will require a little trickery for rear LS/RS since
        // those technically are not
        // locations
        double[] arcBVs = new double[locations() + 2];
        double[] arcHeats = new double[locations() + 2];
        double[] ammoBVs = new double[locations() + 2];

        bvText.append(startRow);
        bvText.append(startColumn);
        bvText.append("Arc BV and Heat");
        bvText.append(endColumn);
        bvText.append(endRow);

        // cycle through locations
        for (int loc = 0; loc < (locations() + 2); loc++) {
            int l = loc;
            boolean isRear = (loc >= locations());
            String rear = "";
            if (isRear) {
                l = l - 3;
                rear = " (R)";
            }
            this.getLocationName(l);

            bvText.append(startRow);
            bvText.append(startColumn);
            bvText.append("<i>" + getLocationName(l) + rear + "</i>");
            bvText.append(endColumn);
            bvText.append(startColumn);
            bvText.append("<i>BV</i>");
            bvText.append(endColumn);
            bvText.append(startColumn);
            bvText.append("<i>Heat</i>");
            bvText.append(endColumn);
            bvText.append(endRow);
            double arcBV = 0.0;
            double arcHeat = 0.0;
            double arcAmmoBV = 0.0;
            TreeMap<String, Double> weaponsForExcessiveAmmo = new TreeMap<String, Double>();
            for (Mounted mounted : getTotalWeaponList()) {
                if (mounted.getLocation() != l) {
                    continue;
                }
                if (mounted.isRearMounted() != isRear) {
                    continue;
                }
                // only count non-damaged equipment
                if (mounted.isMissing() || mounted.isHit() || mounted.isDestroyed() || mounted.isBreached()) {
                    continue;
                }
                WeaponType wtype = (WeaponType) mounted.getType();
                double weaponHeat = wtype.getHeat();
                double dBV = wtype.getBV(this);
                // skip bays
                if (wtype instanceof BayWeapon) {
                    continue;
                }
                // don't count defensive weapons
                if (wtype.hasFlag(WeaponType.F_AMS)) {
                    continue;
                }
                // don't count screen launchers, they are defensive
                if (wtype.getAtClass() == WeaponType.CLASS_SCREEN) {
                    continue;
                }
                // double heat for ultras
                if ((wtype.getAmmoType() == AmmoType.T_AC_ULTRA) || (wtype.getAmmoType() == AmmoType.T_AC_ULTRA_THB)) {
                    weaponHeat *= 2;
                }
                // Six times heat for RAC
                if (wtype.getAmmoType() == AmmoType.T_AC_ROTARY) {
                    weaponHeat *= 6;
                }
                // calc MG Array here:
                if (wtype.hasFlag(WeaponType.F_MGA)) {
                    double mgBV = 0;
                    for (int eqNum : mounted.getBayWeapons()) {
                        Mounted mg = getEquipment(eqNum);
                        if ((mg != null) && (!mg.isDestroyed())) {
                            mgBV += mg.getType().getBV(this);
                        }
                    }
                    dBV = mgBV * 0.67;
                }
                // and we'll add the tcomp here too
                if (wtype.hasFlag(WeaponType.F_DIRECT_FIRE)) {
                    if (hasTargComp()) {
                        dBV *= 1.25;
                    }
                }
                // artemis bumps up the value
                if (mounted.getLinkedBy() != null) {
                    Mounted mLinker = mounted.getLinkedBy();
                    if ((mLinker.getType() instanceof MiscType) && mLinker.getType().hasFlag(MiscType.F_ARTEMIS)) {
                        dBV *= 1.2;
                    }
                    if ((mLinker.getType() instanceof MiscType) && mLinker.getType().hasFlag(MiscType.F_ARTEMIS_PROTO)) {
                        dBV *= 1.1;
                    }
                    
                    if ((mLinker.getType() instanceof MiscType) && mLinker.getType().hasFlag(MiscType.F_ARTEMIS_V)) {
                        dBV *= 1.3;
                    }
                    if ((mLinker.getType() instanceof MiscType) && mLinker.getType().hasFlag(MiscType.F_APOLLO)) {
                        dBV *= 1.15;
                    }
                    if ((mLinker.getType() instanceof MiscType)
                            && mLinker.getType().hasFlag(MiscType.F_RISC_LASER_PULSE_MODULE)) {
                        dBV *= 1.15;
                    }
                }
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
                bvText.append(startRow);
                bvText.append(startColumn);
                bvText.append(wtype.getName());
                bvText.append(endColumn);
                bvText.append(startColumn);
                bvText.append("+" + dBV);
                bvText.append(endColumn);
                bvText.append(startColumn);
                bvText.append("+" + weaponHeat);
                bvText.append(endColumn);
                bvText.append(endRow);
                arcBV += dBV;
                arcHeat += weaponHeat;
            }
            // now ammo
            Map<String, Double> ammo = new HashMap<String, Double>();
            ArrayList<String> keys = new ArrayList<String>();
            for (Mounted mounted : getAmmo()) {
                if (mounted.getLocation() != l) {
                    continue;
                }
                if (mounted.isRearMounted() != isRear) {
                    continue;
                }
                AmmoType atype = (AmmoType) mounted.getType();
                // we need to deal with cases where ammo is loaded in multi-ton
                // increments
                // (on dropships and jumpships) - lets take the ratio of shots
                // to
                // shots left
                double ratio = mounted.getUsableShotsLeft() / atype.getShots();

                // if the ratio is less than one, we will treat as a full ton
                // since
                // we don't make that adjustment elsewhere
                if (ratio < 1.0) {
                    ratio = 1.0;
                }

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

                // don't count oneshot ammo, it's considered part of the
                // launcher.
                if (mounted.getLocation() == Entity.LOC_NONE) {
                    // assumption: ammo without a location is for a oneshot
                    // weapon
                    continue;
                }
                double abv = ratio * atype.getBV(this);
                String key = atype.getAmmoType() + ":" + atype.getRackSize();
                String key2 = atype.getName() + ";" + key;
                // MML needs special casing so they don't count double
                if (atype.getAmmoType() == AmmoType.T_MML) {
                    key2 = "MML " + atype.getRackSize() + " Ammo;" + key;
                }
                // same for the different AR10 ammos
                if (atype.getAmmoType() == AmmoType.T_AR10) {
                    key2 = "AR10 Ammo;" + key;
                }
                if (!keys.contains(key2)) {
                    keys.add(key2);
                }
                if (!ammo.containsKey(key)) {
                    ammo.put(key, abv);
                } else {
                    ammo.put(key, abv + ammo.get(key));
                }
            }
            // now cycle through ammo hash and deal with excessive ammo issues
            for (String fullkey : keys) {
                String[] k = fullkey.split(";");
                String key = k[1];
                bvText.append(startRow);
                bvText.append(startColumn);
                bvText.append(k[0]);
                bvText.append(endColumn);
                bvText.append(startColumn);
                if (weaponsForExcessiveAmmo.get(key) != null) {
                    if (ammo.get(key) > weaponsForExcessiveAmmo.get(key)) {
                        bvText.append("+" + weaponsForExcessiveAmmo.get(key) + "*");
                        arcAmmoBV += weaponsForExcessiveAmmo.get(key);
                    } else {
                        bvText.append("+" + ammo.get(key));
                        arcAmmoBV += ammo.get(key);
                    }
                }
                bvText.append(endColumn);
                bvText.append(startColumn);
                bvText.append("");
                bvText.append(endColumn);
                bvText.append(endRow);
            }
            bvText.append(startRow);
            bvText.append(startColumn);
            bvText.append("<b>" + getLocationName(l) + rear + " Weapon Totals</b>");
            bvText.append(endColumn);
            bvText.append(startColumn);
            bvText.append("<b>" + arcBV + "</b>");
            bvText.append(endColumn);
            bvText.append(startColumn);
            bvText.append("<b>" + arcHeat + "</b>");
            bvText.append(endColumn);
            bvText.append(endRow);
            bvText.append(startRow);
            bvText.append(startColumn);
            bvText.append("<b>" + getLocationName(l) + rear + " Ammo Totals</b>");
            bvText.append(endColumn);
            bvText.append(startColumn);
            bvText.append("<b>" + arcAmmoBV + "</b>");
            bvText.append(endColumn);
            bvText.append(startColumn);
            bvText.append("");
            bvText.append(endColumn);
            bvText.append(endRow);
            bvText.append(startRow);
            bvText.append(startColumn);
            bvText.append("<b>" + getLocationName(l) + rear + " Totals</b>");
            bvText.append(endColumn);
            bvText.append(startColumn);
            double tempBV = arcBV + arcAmmoBV;
            bvText.append("<b>" + tempBV + "</b>");
            bvText.append(endColumn);
            bvText.append(startColumn);
            bvText.append("");
            bvText.append(endColumn);
            bvText.append(endRow);
            arcBVs[loc] = arcBV;
            arcHeats[loc] = arcHeat;
            ammoBVs[loc] = arcAmmoBV;
        }

        double weaponBV = 0.0;
        // ok, now lets loop through the arcs and find the highest value BV arc
        int highArc = Integer.MIN_VALUE;
        int adjArcH = Integer.MIN_VALUE;
        int adjArcL = Integer.MIN_VALUE;
        double adjArcHMult = 1.0;
        double adjArcLMult = 0.5;
        double highBV = 0.0;
        double heatUsed = 0.0;
        for (int loc = 0; loc < arcBVs.length; loc++) {
            if (arcBVs[loc] > highBV) {
                highArc = loc;
                highBV = arcBVs[loc];
            }
        }
        // now lets identify the adjacent arcs
        if (highArc > Integer.MIN_VALUE) {
            heatUsed += arcHeats[highArc];
            // now get the BV and heat for the two adjacent arcs
            int adjArcCW = getAdjacentLocCW(highArc);
            int adjArcCCW = getAdjacentLocCCW(highArc);
            double adjArcCWBV = 0.0;
            double adjArcCWHeat = 0.0;
            if (adjArcCW > Integer.MIN_VALUE) {
                adjArcCWBV = arcBVs[adjArcCW];
                adjArcCWHeat = arcHeats[adjArcCW];
            }
            double adjArcCCWBV = 0.0;
            double adjArcCCWHeat = 0.0;
            if (adjArcCCW > Integer.MIN_VALUE) {
                adjArcCCWBV = arcBVs[adjArcCCW];
                adjArcCCWHeat = arcHeats[adjArcCCW];
            }
            if (adjArcCWBV > adjArcCCWBV) {
                adjArcH = adjArcCW;
                if ((heatUsed + adjArcCWHeat) > aeroHeatEfficiency) {
                    adjArcHMult = 0.5;
                }
                heatUsed += adjArcCWHeat;
                adjArcL = adjArcCCW;
                if ((heatUsed + adjArcCCWHeat) > aeroHeatEfficiency) {
                    adjArcLMult = 0.25;
                }
                heatUsed += adjArcCCWHeat;
            } else {
                adjArcH = adjArcCCW;
                if ((heatUsed + adjArcCCWHeat) > aeroHeatEfficiency) {
                    adjArcHMult = 0.5;
                }
                heatUsed += adjArcCCWHeat;
                adjArcL = adjArcCW;
                if ((heatUsed + adjArcCWHeat) > aeroHeatEfficiency) {
                    adjArcLMult = 0.25;
                }
                heatUsed += adjArcCWHeat;
            }
        }

        // ok now add in ammo to arc bvs
        for (int i = 0; i < arcBVs.length; i++) {
            arcBVs[i] = arcBVs[i] + ammoBVs[i];
        }

        // ok now lets go in and add the arcs
        double totalHeat = 0.0;
        if (highArc > Integer.MIN_VALUE) {
            // ok now add the BV from this arc and reset to zero
            bvText.append(startRow);
            bvText.append(startColumn);
            bvText.append("Highest BV Arc (" + getArcName(highArc) + ")" + arcBVs[highArc] + "*1.0");
            bvText.append(endColumn);
            bvText.append(startColumn);
            bvText.append("+" + arcBVs[highArc]);
            bvText.append(endColumn);
            bvText.append(endRow);
            bvText.append(startColumn);
            totalHeat = arcHeats[highArc];
            bvText.append("Total Heat: " + totalHeat);
            bvText.append(endColumn);
            bvText.append(endRow);
            weaponBV += arcBVs[highArc];
            arcBVs[highArc] = 0.0;
            if (adjArcH > Integer.MIN_VALUE) {
                bvText.append(startRow);
                bvText.append(startColumn);
                bvText.append(
                        "Adjacent High BV Arc (" + getArcName(adjArcH) + ") " + arcBVs[adjArcH] + "*" + adjArcHMult);
                bvText.append(endColumn);
                bvText.append(startColumn);
                bvText.append("+" + (arcBVs[adjArcH] * adjArcHMult));
                bvText.append(endColumn);
                bvText.append(endRow);
                bvText.append(startRow);
                bvText.append(startColumn);
                totalHeat += arcHeats[adjArcH];
                String over = "";
                if (totalHeat > aeroHeatEfficiency) {
                    over = " (Greater than heat efficiency)";
                }
                bvText.append("Total Heat: " + totalHeat + over);
                bvText.append(endColumn);
                bvText.append(endRow);
                weaponBV += adjArcHMult * arcBVs[adjArcH];
                arcBVs[adjArcH] = 0.0;
            }
            if (adjArcL > Integer.MIN_VALUE) {
                bvText.append(startRow);
                bvText.append(startColumn);
                bvText.append(
                        "Adjacent Low BV Arc (" + getArcName(adjArcL) + ") " + arcBVs[adjArcL] + "*" + adjArcLMult);
                bvText.append(endColumn);
                bvText.append(startColumn);
                bvText.append("+" + (adjArcLMult * arcBVs[adjArcL]));
                bvText.append(endColumn);
                bvText.append(endRow);
                bvText.append(startRow);
                bvText.append(startColumn);
                totalHeat += arcHeats[adjArcL];
                String over = "";
                if (totalHeat > aeroHeatEfficiency) {
                    over = " (Greater than heat efficiency)";
                }
                bvText.append("Total Heat: " + totalHeat + over);
                bvText.append(endColumn);
                bvText.append(endRow);
                weaponBV += adjArcLMult * arcBVs[adjArcL];
                arcBVs[adjArcL] = 0.0;
            }
            // ok now we can cycle through the rest and add 25%
            bvText.append(startRow);
            bvText.append(startColumn);
            bvText.append("Remaining Arcs");
            bvText.append(endColumn);
            bvText.append(endRow);
            for (int loc = 0; loc < arcBVs.length; loc++) {
                if (arcBVs[loc] <= 0) {
                    continue;
                }
                bvText.append(startRow);
                bvText.append(startColumn);
                bvText.append(getArcName(loc) + " " + arcBVs[loc] + "*0.25");
                bvText.append(endColumn);
                bvText.append(startColumn);
                bvText.append("+" + (0.25 * arcBVs[loc]));
                bvText.append(endColumn);
                bvText.append(endRow);
                weaponBV += (0.25 * arcBVs[loc]);
            }
        }

        bvText.append("Total Weapons BV Adjusted For Heat:");
        bvText.append(endColumn);
        bvText.append(startColumn);
        bvText.append(endColumn);
        bvText.append(startColumn);
        bvText.append(weaponBV);
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

            if (mtype.hasFlag(MiscType.F_TARGCOMP)) {
                continue;
            }
            double bv = mtype.getBV(this);
            if (bv > 0) {
                bvText.append(startRow);
                bvText.append(startColumn);

                bvText.append(mounted.getName());
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

        // adjust

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

        bvText.append(endColumn);
        bvText.append(startColumn);
        bvText.append(" = ");
        bvText.append(finalBV);
        bvText.append(endColumn);
        bvText.append(endRow);

        finalBV = Math.round(finalBV);

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

        bvText.append(endTable);
        bvText.append("</BODY></HTML>");

        // we get extra bv from some stuff
        double xbv = 0.0;

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
     * find the adjacent firing arc on this vessel clockwise
     */
    public int getAdjacentArcCW(int arc) {
        switch (arc) {
        case Compute.ARC_NOSE:
            if (isSpheroid()) {
                return Compute.ARC_RIGHTSIDE_SPHERE;
            }
            return Compute.ARC_RWING;
        case Compute.ARC_LWING:
            return Compute.ARC_NOSE;
        case Compute.ARC_RWING:
            return Compute.ARC_RWINGA;
        case Compute.ARC_LWINGA:
            return Compute.ARC_LWING;
        case Compute.ARC_RWINGA:
            return Compute.ARC_AFT;
        case Compute.ARC_LEFTSIDE_SPHERE:
            return Compute.ARC_NOSE;
        case Compute.ARC_RIGHTSIDE_SPHERE:
            return Compute.ARC_RIGHTSIDEA_SPHERE;
        case Compute.ARC_LEFTSIDEA_SPHERE:
            return Compute.ARC_LEFTSIDE_SPHERE;
        case Compute.ARC_RIGHTSIDEA_SPHERE:
            return Compute.ARC_AFT;
        case Compute.ARC_AFT:
            if (isSpheroid()) {
                return Compute.ARC_LEFTSIDEA_SPHERE;
            }
            return Compute.ARC_LWINGA;
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
            if (isSpheroid()) {
                return Compute.ARC_LEFTSIDE_SPHERE;
            }
            return Compute.ARC_LWING;
        case Compute.ARC_LWING:
            return Compute.ARC_LWINGA;
        case Compute.ARC_RWING:
            return Compute.ARC_NOSE;
        case Compute.ARC_LWINGA:
            return Compute.ARC_AFT;
        case Compute.ARC_RWINGA:
            return Compute.ARC_RWING;
        case Compute.ARC_LEFTSIDE_SPHERE:
            return Compute.ARC_LEFTSIDEA_SPHERE;
        case Compute.ARC_RIGHTSIDE_SPHERE:
            return Compute.ARC_NOSE;
        case Compute.ARC_LEFTSIDEA_SPHERE:
            return Compute.ARC_AFT;
        case Compute.ARC_RIGHTSIDEA_SPHERE:
            return Compute.ARC_RWING;
        case Compute.ARC_AFT:
            if (isSpheroid()) {
                return Compute.ARC_RIGHTSIDEA_SPHERE;
            }
            return Compute.ARC_RWINGA;
        default:
            return Integer.MIN_VALUE;
        }
    }

    /**
     * find the adjacent firing arc location on this vessel clockwise
     */
    public int getAdjacentLocCW(int loc) {
        switch (loc) {
        case LOC_NOSE:
            return LOC_RWING;
        case LOC_LWING:
            return LOC_NOSE;
        case LOC_RWING:
            return (LOC_RWING + 3);
        case LOC_AFT:
            return (LOC_LWING + 3);
        case 4:
            return LOC_LWING;
        case 5:
            return LOC_AFT;
        default:
            return Integer.MIN_VALUE;
        }
    }

    /**
     * find the adjacent firing arc on this vessel counter-clockwise
     */
    public int getAdjacentLocCCW(int loc) {
        switch (loc) {
        case LOC_NOSE:
            return LOC_LWING;
        case LOC_LWING:
            return (LOC_LWING + 3);
        case LOC_RWING:
            return LOC_NOSE;
        case LOC_AFT:
            return (LOC_RWING + 3);
        case 4:
            return LOC_AFT;
        case 5:
            return LOC_RWING;
        default:
            return Integer.MIN_VALUE;
        }
    }

    /**
     * find the adjacent firing arc location on this vessel clockwise
     */
    public int getOppositeLoc(int loc) {
        switch (loc) {
        case LOC_NOSE:
            return LOC_AFT;
        case LOC_LWING:
            return (LOC_RWING + 3);
        case LOC_RWING:
            return (LOC_LWING + 3);
        case LOC_AFT:
            return LOC_NOSE;
        case 4:
            return LOC_RWING;
        case 5:
            return LOC_LWING;
        default:
            return Integer.MIN_VALUE;
        }
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
        Vector<Coords> positions = new Vector<Coords>();
        positions.add(getPosition());
        for (int i = 0; i < 6; i++) {
            positions.add(getPosition().translated(i));
        }
        for (Coords pos : positions) {
            IHex hex = game.getBoard().getHex(getPosition());
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
        return (isAirborne() || isSpaceborne() || game.getPhase() == GamePhase.LOUNGE);
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
    public void addBattleForceSpecialAbilities(Map<BattleForceSPA,Integer> specialAbilities) {
        super.addBattleForceSpecialAbilities(specialAbilities);
        if (getNCrew() >= 30) {
            specialAbilities.put(BattleForceSPA.CRW, (int)Math.round(getNCrew() / 60.0));
        }
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
}

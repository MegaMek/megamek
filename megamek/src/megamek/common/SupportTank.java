/*
 * MegaMek - Copyright (C) 2000-2003 Ben Mazur (bmazur@sev.org)
 *
 * This program is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License as published by the Free
 * Software Foundation; either version 2 of the License, or (at your option)
 * any later version.
 *
 * This program is distributed in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 * or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License
 * for more details.
 */
package megamek.common;

import java.util.Map;

/**
 * This is a support vehicle
 *
 * @author beerockxs
 */
public class SupportTank extends Tank {
    private static final long serialVersionUID = -9028127010133768714L;
    private int[] barRating;
    private double fuelTonnage = 0;

    public SupportTank() {
        super();
        barRating = new int[locations()];
    }

    public void setBARRating(int rating, int loc) {
        barRating[loc] = rating;
    }

    @Override
    public void setBARRating(int rating) {
        for (int i = 0; i < locations(); i++) {
            barRating[i] = rating;
        }
    }


    /*
     * (non-Javadoc)
     *
     * @see megamek.common.Entity#getBARRating()
     */
    @Override
    public int getBARRating(int loc) {
        return barRating[loc];
    }

    /*
     * (non-Javadoc)
     *
     * @see megamek.common.Entity#hasBARArmor()
     */
    @Override
    public boolean hasBARArmor(int loc) {
        return true;
    }

    /*
     * (non-Javadoc)
     *
     * @see megamek.common.Entity#hasArmoredChassis()
     */
    @Override
    public boolean hasArmoredChassis() {
        for (Mounted misc : miscList) {
            if (misc.getType().hasFlag(MiscType.F_ARMORED_CHASSIS)) {
                return true;
            }
        }
        return false;
    }
    
    private static final TechAdvancement TA_HOVER = new TechAdvancement(TECH_BASE_ALL)
            .setTechRating(RATING_C).setAdvancement(DATE_PS, DATE_ES, DATE_ES)
            .setAvailability(RATING_A, RATING_B, RATING_A, RATING_A)
            .setStaticTechLevel(SimpleTechLevel.STANDARD);
    private static final TechAdvancement TA_HOVER_LARGE = new TechAdvancement(TECH_BASE_ALL)
            .setTechRating(RATING_C).setAdvancement(DATE_PS, DATE_ES, DATE_ES)
            .setAvailability(RATING_B, RATING_C, RATING_B, RATING_B)
            .setStaticTechLevel(SimpleTechLevel.STANDARD);
    private static final TechAdvancement TA_NAVAL = new TechAdvancement(TECH_BASE_ALL)
            .setAdvancement(DATE_PS, DATE_PS, DATE_PS).setTechRating(RATING_A)
            .setAvailability(RATING_C, RATING_D, RATING_C, RATING_C)
            .setStaticTechLevel(SimpleTechLevel.STANDARD);
    private static final TechAdvancement TA_NAVAL_LARGE = new TechAdvancement(TECH_BASE_ALL)
            .setAdvancement(DATE_PS, DATE_PS, DATE_PS).setTechRating(RATING_A)
            .setAvailability(RATING_C, RATING_E, RATING_D, RATING_D)
            .setStaticTechLevel(SimpleTechLevel.ADVANCED);
    private static final TechAdvancement TA_TRACKED = new TechAdvancement(TECH_BASE_ALL)
            .setAdvancement(DATE_PS, DATE_PS, DATE_PS).setTechRating(RATING_B)
            .setAvailability(RATING_B, RATING_C, RATING_B, RATING_B)
            .setStaticTechLevel(SimpleTechLevel.STANDARD);
    private static final TechAdvancement TA_TRACKED_LARGE = new TechAdvancement(TECH_BASE_ALL)
            .setAdvancement(DATE_PS, DATE_PS, DATE_PS).setTechRating(RATING_B)
            .setAvailability(RATING_C, RATING_D, RATING_C, RATING_C)
            .setStaticTechLevel(SimpleTechLevel.STANDARD);
    private static final TechAdvancement TA_WHEELED_SMALL = new TechAdvancement(TECH_BASE_ALL)
            .setAdvancement(DATE_PS, DATE_PS, DATE_PS).setTechRating(RATING_A)
            .setAvailability(RATING_A, RATING_A, RATING_A, RATING_A)
            .setStaticTechLevel(SimpleTechLevel.STANDARD);
    private static final TechAdvancement TA_WHEELED_MEDIUM = new TechAdvancement(TECH_BASE_ALL)
            .setAdvancement(DATE_PS, DATE_PS, DATE_PS).setTechRating(RATING_A)
            .setAvailability(RATING_A, RATING_B, RATING_A, RATING_A)
            .setStaticTechLevel(SimpleTechLevel.STANDARD);
    private static final TechAdvancement TA_WHEELED_LARGE = new TechAdvancement(TECH_BASE_ALL)
            .setAdvancement(DATE_PS, DATE_PS, DATE_PS).setTechRating(RATING_A)
            .setAvailability(RATING_B, RATING_C, RATING_B, RATING_B)
            .setStaticTechLevel(SimpleTechLevel.STANDARD);
    private static final TechAdvancement TA_WIGE = new TechAdvancement(TECH_BASE_ALL)
            .setAdvancement(DATE_ES, DATE_ES, DATE_ES).setTechRating(RATING_C)
            .setAvailability(RATING_B, RATING_C, RATING_B, RATING_B)
            .setStaticTechLevel(SimpleTechLevel.STANDARD);
    private static final TechAdvancement TA_WIGE_LARGE = new TechAdvancement(TECH_BASE_ALL)
            .setAdvancement(DATE_ES, DATE_ES, DATE_ES).setTechRating(RATING_C)
            .setAvailability(RATING_C, RATING_D, RATING_C, RATING_C)
            .setStaticTechLevel(SimpleTechLevel.STANDARD);
    private static final TechAdvancement TA_RAIL = new TechAdvancement(TECH_BASE_ALL)
            .setAdvancement(DATE_PS, DATE_PS, DATE_PS).setTechRating(RATING_A)
            .setAvailability(RATING_C, RATING_C, RATING_C, RATING_C)
            .setStaticTechLevel(SimpleTechLevel.ADVANCED);
    private static final TechAdvancement TA_RAIL_LARGE = new TechAdvancement(TECH_BASE_ALL)
            .setAdvancement(DATE_PS, DATE_PS, DATE_PS).setTechRating(RATING_A)
            .setAvailability(RATING_C, RATING_D, RATING_D, RATING_D)
            .setStaticTechLevel(SimpleTechLevel.ADVANCED);

    @Override
    public TechAdvancement getConstructionTechAdvancement() {
        return getConstructionTechAdvancement(getMovementMode(), getWeightClass());
    }

    public static TechAdvancement getConstructionTechAdvancement(EntityMovementMode movementMode, int weightClass) {
        /* Support vehicle dates and tech ratings are found in TM 120, 122. DA availability is assumed to
         * be the same as Clan invasion era. */
        switch (movementMode) {
            case HOVER:
                if (weightClass == EntityWeightClass.WEIGHT_LARGE_SUPPORT) {
                    return TA_HOVER_LARGE;
                } else {
                    return TA_HOVER;
                }
            case NAVAL:
            case HYDROFOIL:
            case SUBMARINE:
                if (weightClass == EntityWeightClass.WEIGHT_LARGE_SUPPORT) {
                    return TA_NAVAL_LARGE;
                } else {
                    return TA_NAVAL;
                }
            case TRACKED:
                if (weightClass == EntityWeightClass.WEIGHT_LARGE_SUPPORT) {
                    return TA_TRACKED_LARGE;
                } else {
                    return TA_TRACKED;
                }
            case WHEELED:
                if (weightClass == EntityWeightClass.WEIGHT_LARGE_SUPPORT) {
                    return TA_WHEELED_LARGE;
                } else if (weightClass == EntityWeightClass.WEIGHT_MEDIUM_SUPPORT) {
                    return TA_WHEELED_MEDIUM;
                } else {
                    return TA_WHEELED_SMALL;
                }
            case WIGE:
                if (weightClass == EntityWeightClass.WEIGHT_LARGE_SUPPORT) {
                    return TA_WIGE_LARGE;
                } else {
                    return TA_WIGE;
                }
            case RAIL:
            case MAGLEV:
                if (weightClass == EntityWeightClass.WEIGHT_LARGE_SUPPORT) {
                    return TA_RAIL_LARGE;
                } else {
                    return TA_RAIL;
                }
            default:
                return TA_TRACKED; // average
        }
    }

    /**
     * Tanks have all sorts of prohibited terrain.
     */
    @Override
    public boolean isLocationProhibited(Coords c, int currElevation) {
        Hex hex = game.getBoard().getHex(c);
        if (hex.containsTerrain(Terrains.IMPASSABLE)) {
            return true;
        }

        // Additional restrictions for hidden units
        if (isHidden()) {
            // Can't deploy in paved hexes
            if (hex.containsTerrain(Terrains.PAVEMENT)
                    || hex.containsTerrain(Terrains.ROAD)) {
                return true;
            }
            // Can't deploy on a bridge
            if ((hex.terrainLevel(Terrains.BRIDGE_ELEV) == currElevation)
                    && hex.containsTerrain(Terrains.BRIDGE)) {
                return true;
            }
            // Can't deploy on the surface of water
            if (hex.containsTerrain(Terrains.WATER) && (currElevation == 0)) {
                return true;
            }
        }

        switch (movementMode) {
            case TRACKED:
                return (hex.terrainLevel(Terrains.WOODS) > 1)
                        || ((hex.terrainLevel(Terrains.WATER) > 0)
                                && !hex.containsTerrain(Terrains.ICE) 
                                && !hasEnvironmentalSealing())
                        || hex.containsTerrain(Terrains.JUNGLE)
                        || (hex.terrainLevel(Terrains.MAGMA) > 1)
                        || (hex.terrainLevel(Terrains.ROUGH) > 1)
                        || (hex.terrainLevel(Terrains.RUBBLE) > 5);
            case WHEELED:
                return hex.containsTerrain(Terrains.WOODS)
                        || hex.containsTerrain(Terrains.ROUGH)
                        || ((hex.terrainLevel(Terrains.WATER) > 0)
                                && !hex.containsTerrain(Terrains.ICE) 
                                && !hasEnvironmentalSealing())
                        || hex.containsTerrain(Terrains.RUBBLE)
                        || hex.containsTerrain(Terrains.MAGMA)
                        || hex.containsTerrain(Terrains.JUNGLE)
                        || (hex.terrainLevel(Terrains.SNOW) > 1)
                        || (hex.terrainLevel(Terrains.GEYSER) == 2);
            case HOVER:
                return hex.containsTerrain(Terrains.WOODS)
                        || hex.containsTerrain(Terrains.JUNGLE)
                        || (hex.terrainLevel(Terrains.MAGMA) > 1)
                        || (hex.terrainLevel(Terrains.ROUGH) > 1)
                        || (hex.terrainLevel(Terrains.RUBBLE) > 5);
            case NAVAL:
            case HYDROFOIL:
                return (hex.terrainLevel(Terrains.WATER) <= 0)
                        || hex.containsTerrain(Terrains.ICE);
            case SUBMARINE:
                return (hex.terrainLevel(Terrains.WATER) <= 0);
            case WIGE:
                return (hex.containsTerrain(Terrains.WOODS) || (hex
                        .containsTerrain(Terrains.BUILDING)))
                        && !(currElevation > hex
                                .maxTerrainFeatureElevation(game.getBoard()
                                        .inAtmosphere()));
            default:
                return false;
        }
    }

    /*
     * (non-Javadoc)
     *
     * @see megamek.common.Entity#getTotalCommGearTons()
     */
    @Override
    public int getTotalCommGearTons() {
        return getExtraCommGearTons();
    }

    @Override
    public int getWalkMP(boolean gravity, boolean ignoreheat,
                         boolean ignoremodulararmor) {
        int mp = getOriginalWalkMP();
        if (engineHit || isImmobile()) {
            return 0;
        }
        if (hasWorkingMisc(MiscType.F_HYDROFOIL)) {
            mp = (int) Math.round(mp * 1.25);
        }
        mp = Math.max(0, mp - motiveDamage);
        mp = Math.max(0, mp - getCargoMpReduction(this));
        if (null != game) {
            int weatherMod = game.getPlanetaryConditions()
                    .getMovementMods(this);
            if (weatherMod != 0) {
                mp = Math.max(mp + weatherMod, 0);
            }
        }

        if (!ignoremodulararmor && hasModularArmor()) {
            mp--;
        }
        if (hasWorkingMisc(MiscType.F_DUNE_BUGGY) && (game != null)) {
            mp--;
        }

        if (gravity) {
            mp = applyGravityEffectsOnMP(mp);
        }

        //If the unit is towing trailers, adjust its walkMP, TW p205
        if ((null != game) && !getAllTowedUnits().isEmpty()) {
            double trainWeight = getWeight();
            int lowestSuspensionFactor = getSuspensionFactor();
            //Add up the trailers
            for (int id : getAllTowedUnits()) {
                Entity tr = game.getEntity(id);
                if (tr == null) {
                    //this isn't supposed to happen, but it can in rare cases when tr is destroyed
                    continue;
                }
                if (tr instanceof Tank) {
                    Tank trailer = (Tank) tr;
                    if (trailer.getSuspensionFactor() < lowestSuspensionFactor) {
                        lowestSuspensionFactor = trailer.getSuspensionFactor();
                    }
                }
                trainWeight += tr.getWeight();
            }
            mp = (int) ((getEngine().getRating() + lowestSuspensionFactor) / trainWeight);
        }

        return mp;

    }
    
    // CONSTRUCTION INFORMATION
    //Support Vee Engine Information
    @Override
    public double getBaseChassisValue() {
        switch (movementMode) {
            case HOVER:
                if (getWeight() < 5) {
                    return 0.2;
                } else if (!isSuperHeavy()) {
                    return 0.25;
                } else {
                    return 0.3;
                }
            case NAVAL:
            case HYDROFOIL:
            case SUBMARINE:
                if (getWeight() < 5) {
                    return 0.12;
                } else {
                    return 0.15;
                }
            case TRACKED:
                if (getWeight() < 5) {
                    return 0.13;
                } else if (!isSuperHeavy()) {
                    return 0.15;
                } else {
                    return 0.25;
                }
            case WHEELED:
                if (getWeight() < 5) {
                    return 0.12;
                } else if (!isSuperHeavy()) {
                    return 0.15;
                } else {
                    return 0.18;
                }
            case WIGE:
                if (getWeight() < 5) {
                    return 0.12;
                } else if (!isSuperHeavy()) {
                    return 0.15;
                } else {
                    return 0.17;
                }
           default:
               return 0;
        }
    }

    //Support Vee Engine Information
    @Override
    public double getBaseEngineValue() {
        switch (movementMode) {
            case AIRSHIP:
                if (getWeight() < 5) {
                    return 0.005;
                } else {
                    return 0.008;
                }
            case HOVER:
                if (getWeight() < 5) {
                    return 0.0025;
                } else if (!isSuperHeavy()) {
                    return 0.004;
                } else {
                    return 0.008;
                }
            case NAVAL:
            case HYDROFOIL:
            case SUBMARINE:
                if (getWeight() < 5) {
                    return 0.004;
                } else {
                    return 0.007;
                }
            case TRACKED:
                if (getWeight() < 5) {
                    return 0.006;
                } else if (!isSuperHeavy()) {
                    return 0.013;
                } else {
                    return 0.025;
                }
            case WHEELED:
                if (getWeight() < 5) {
                    return 0.0025;
                } else if (!isSuperHeavy()) {
                    return 0.0075;
                } else {
                    return 0.015;
                }
            case WIGE:
                if (getWeight() < 5) {
                    return 0.003;
                } else if (!isSuperHeavy()) {
                    return 0.005;
                } else {
                    return 0.006;
                }
            case RAIL:
            case MAGLEV:
                if (getWeight() < 5) {
                    return 0.003;
                } else if (!isSuperHeavy()) {
                    return 0.004;
                } else {
                    return 0.005;
                }
           default:
               return 0;
        }
    }

    //FUEL CAPACITY TM 128
    @Override
    public double getFuelTonnage() {
        return fuelTonnage;
    }

    @Override
    public void setFuelTonnage(double fuel) {
        fuelTonnage = fuel;
    }

    //DETERMINE SI TM 130
    //ADD LIFT/DIVE EQUIPMENT TM 131
    //ADD CONTROL AND CREW STUFF TM 131
    //ADD HEAT SINKS TM 133
    //ADD ARMOR
    //ADD WEAPONS AMMO and EQUIPMENT
      
        
    @Override
    public int getTotalSlots() {
        return 5 + (int) Math.floor(getWeight() / 10);
    }

    @Override
    public long getEntityType() {
        return Entity.ETYPE_TANK | Entity.ETYPE_SUPPORT_TANK;
    }
    
    
    //START OF BATTLEFORCE STUFF.
    @Override
    public int getBattleForceSize() {
        //The tables are on page 356 of StartOps
        if (getWeight() < 5) {
            return 1;
        }
        int mediumCeil= 0;
        int largeCeil=0;
        int veryLargeCeil = 0;
        switch (movementMode) {
            case TRACKED:
                mediumCeil = 100;
                largeCeil = 200;
                break;
            case WHEELED:
                mediumCeil = 80;
                largeCeil = 160;
                break;
            case HOVER:
                mediumCeil = 50;
                largeCeil = 100;
                break;
            case NAVAL:
            case HYDROFOIL:
            case SUBMARINE:
                mediumCeil = 300;
                largeCeil = 6000;
                veryLargeCeil = 30000;
                break;
            case WIGE:
                mediumCeil = 80;
                largeCeil = 240;
                break;
            default:
                break;
        }
        if (getWeight() < mediumCeil) {
            return 2;
        }
        if (getWeight() < largeCeil) {
            return 3;
        }
        if ((getWeight() < veryLargeCeil) || (veryLargeCeil == 0)) {
            return 4;
        }
        return 5;
    }

    @Override
    /*
     * returns the battle force structure points for a mech
     */
    public int getBattleForceStructurePoints() {
        switch (movementMode) {
            case NAVAL:
            case HYDROFOIL:
            case SUBMARINE:
                if (getWeight() <= 300) {
                    return 10;
                }
                if (getWeight() <= 500) {
                    return 15;
                }
                if (getWeight() <= 6000) {
                    return 20;
                }
                if (getWeight() <= 12000) {
                    return 25;
                }
                if (getWeight() <= 30000) {
                    return 30;
                }
                if (getWeight() <= 100000) {
                    return 35;
                }
            default:
                //TODO add rail in here when ready
                return super.getBattleForceStructurePoints();
        }
    }
    
    @Override
    public void addBattleForceSpecialAbilities(Map<BattleForceSPA,Integer> specialAbilities) {
        super.addBattleForceSpecialAbilities(specialAbilities);
        switch (getBattleForceSize()) {
            case 3:
                specialAbilities.put(BattleForceSPA.LG, null);
                break;
            case 4:
                specialAbilities.put(BattleForceSPA.VLG, null);
                break;
            case 5:
                specialAbilities.put(BattleForceSPA.SLG, null);
                break;
        }
    }
    
    @Override
    public boolean isSupportVehicle() {
        return true;
    }

    @Override
    public boolean isTractor() {
        return hasWorkingMisc(MiscType.F_TRACTOR_MODIFICATION);
    }

    @Override
    public boolean isTrailer() {
        return hasWorkingMisc(MiscType.F_TRAILER_MODIFICATION);
    }
}

/*
* MegaAero - Copyright (C) 2007 Jay Lawson
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
/*
 * Created on Jun 17, 2007
 *
 */
package megamek.common;

import megamek.common.options.OptionsConstants;

/**
 * @author Jay Lawson
 */
public class SpaceStation extends Jumpship {

    /**
     *
     */
    private static final long serialVersionUID = -3160156173650960985L;
    
    
    //ASEW Missile Effects, per location
    //Values correspond to Locations, inherited from Jumpship: NOS,FLS,FRS,AFT,ALS,ARS
    private int asewAffectedTurns[] = { 0, 0, 0, 0, 0, 0};
    
    
    /*
     * Sets the number of rounds a specified firing arc is affected by an ASEW missile
     * @param arc - integer representing the desired firing arc
     * @param turns - integer specifying the number of end phases that the effects last through
     * Technically, about 1.5 turns elapse per the rules for ASEW missiles in TO
     * Space Stations should use the same method as Jumpships, but because Warships have a different number of arcs
     * we run into problems if this isn't explicitly specified here.
     */
    @Override
    public void setASEWAffected(int arc, int turns) {
        asewAffectedTurns[arc] = turns;
    }
    
    /*
     * Returns the number of rounds a specified firing arc is affected by an ASEW missile
     * @param arc - integer representing the desired firing arc
     * Also an override to prevent issues with Warships having a different number of arcs
     */
    @Override
    public int getASEWAffected(int arc) {
        return asewAffectedTurns[arc];
    }
    
    public SpaceStation() {
        super();
        setDriveCoreType(DRIVE_CORE_NONE);
    }
    
    private static final TechAdvancement TA_SPACE_STATION = new TechAdvancement(TECH_BASE_ALL)
            .setAdvancement(DATE_ES, DATE_ES)
            .setTechRating(RATING_D)
            .setAvailability(RATING_C, RATING_D, RATING_C, RATING_C)
            .setStaticTechLevel(SimpleTechLevel.ADVANCED);
    /*
    private static final TechAdvancement TA_SPACE_STATION_MODULAR = new TechAdvancement(TECH_BASE_ALL)
            .setISAdvancement(2565, 2585, DATE_NONE, 2790, 3090).setClanAdvancement(2565, 2585)
            .setPrototypeFactions(F_TH).setProductionFactions(F_TH)
            .setReintroductionFactions(F_RS).setTechRating(RATING_D)
            .setAvailability(RATING_F, RATING_F, RATING_F, RATING_F)
            .setStaticTechLevel(SimpleTechLevel.ADVANCED);
            */

    @Override
    public TechAdvancement getConstructionTechAdvancement() {
        return TA_SPACE_STATION;
    }

    @Override
    public double getCost(boolean ignoreAmmo) {
        double[] costs = new double[20];
        int costIdx = 0;
        double cost = 0;

        // Control Systems
        // Bridge
        costs[costIdx++] += 200000 + 10 * weight;
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

        // Station-Keeping Drive
        // Engine
        costs[costIdx++] += 1000 * weight * 0.012;
        // Engine Control Unit
        costs[costIdx++] += 1000;

        // Additional Ships Systems
        // Attitude Thrusters
        costs[costIdx++] += 25000;
        // Docking Collars
        costs[costIdx++] += 100000 * getDocks();
        // Fuel Tanks
        costs[costIdx++] += (200 * getFuel()) / getFuelPerTon() * 1.02;

        // Armor
        costs[costIdx++] += getArmorWeight(locations()) * EquipmentType.getArmorCost(armorType[0]);

        // Heat Sinks
        int sinkCost = 2000 + (4000 * getHeatType());
        costs[costIdx++] += sinkCost * getHeatSinks();

        // Escape Craft
        costs[costIdx++] += 5000 * (getLifeBoats() + getEscapePods());

        // Grav Decks
        double deckCost = 0;
        deckCost += 5000000 * getGravDeck();
        deckCost += 10000000 * getGravDeckLarge();
        deckCost += 40000000 * getGravDeckHuge();
        costs[costIdx++] += deckCost;

        // Transport Bays
        int baydoors = 0;
        int bayCost = 0;
        for (Bay next : getTransportBays()) {
            baydoors += next.getDoors();
            if ((next instanceof MechBay) || (next instanceof ASFBay) || (next instanceof SmallCraftBay)) {
                bayCost += 20000 * next.totalSpace;
            }
            if ((next instanceof LightVehicleBay) || (next instanceof HeavyVehicleBay)) {
                bayCost += 20000 * next.totalSpace;
            }
        }

        costs[costIdx++] += bayCost + (baydoors * 1000);

        // Weapons and Equipment
        // HPG
        if (hasHPG()) {
            costs[costIdx++] += 1000000000;
        } else {
            costs[costIdx++] += 0;
        }
        // Weapons and Equipment
        costs[costIdx++] += getWeaponsAndEquipmentCost(ignoreAmmo);

        double weightMultiplier = 5.00f;

        // Sum Costs
        for (int i = 0; i < costIdx; i++) {
            cost += costs[i];
        }

        costs[costIdx++] = -weightMultiplier; // Negative indicates multiplier
        cost = Math.round(cost * weightMultiplier);

        return cost;

    }

    /**
     * All military space stations automatically have ECM if in space
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
        int range = 2;
        // the range might be affected by sensor/FCS damage
        range = range - getSensorHits() - getCICHits();
        return range;
    }

    @Override
    public double getBVTypeModifier() {
        return 0.7;
    }

    public long getEntityType() {
        return Entity.ETYPE_AERO | Entity.ETYPE_JUMPSHIP | Entity.ETYPE_SPACE_STATION;
    }

}
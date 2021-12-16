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

import java.text.NumberFormat;

import megamek.common.options.OptionsConstants;

/**
 * @author Jay Lawson
 */
public class SpaceStation extends Jumpship {

    /**
     *
     */
    private static final long serialVersionUID = -3160156173650960985L;
    
    // This only affects cost, but may have an effect in a large-scale strategic setting.
    private boolean modular = false;
    
    @Override
    public int getUnitType() {
        return UnitType.SPACE_STATION;
    }
    
    public SpaceStation() {
        super();
        setDriveCoreType(DRIVE_CORE_NONE);
        setSail(false);
    }
    
    private static final TechAdvancement TA_SPACE_STATION = new TechAdvancement(TECH_BASE_ALL)
            .setAdvancement(DATE_ES, DATE_ES)
            .setTechRating(RATING_D)
            .setAvailability(RATING_C, RATING_D, RATING_C, RATING_C)
            .setStaticTechLevel(SimpleTechLevel.ADVANCED);

    private static final TechAdvancement TA_SPACE_STATION_MODULAR = new TechAdvancement(TECH_BASE_ALL)
            .setISAdvancement(2565, 2585, DATE_NONE, 2790, 3090).setClanAdvancement(2565, 2585)
            .setPrototypeFactions(F_TH).setProductionFactions(F_TH)
            .setReintroductionFactions(F_RS).setTechRating(RATING_D)
            .setAvailability(RATING_F, RATING_F, RATING_F, RATING_F)
            .setStaticTechLevel(SimpleTechLevel.ADVANCED);

    @Override
    public TechAdvancement getConstructionTechAdvancement() {
        return modular? TA_SPACE_STATION_MODULAR : TA_SPACE_STATION;
    }
    
    public static TechAdvancement getModularTA() {
        return TA_SPACE_STATION_MODULAR;
    }
    
    /**
     * Designates whether this is a modular space station
     * @param modular
     */
    public void setModular(boolean modular) {
        this.modular = modular;
    }
    
    /**
     * @return True if this space station has a modular construction, otherwise false.
     */
    public boolean isModular() {
        return modular;
    }

    @Override
    public double getCost(boolean ignoreAmmo) {
        double[] costs = new double[21];
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
        costs[costIdx++] += getArmorWeight() * EquipmentType.getArmorCost(armorType[0]);

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

        // Weapons and Equipment
        // HPG
        if (hasHPG()) {
            costs[costIdx++] += 1000000000;
        } else {
            costs[costIdx++] += 0;
        }
        // Weapons and Equipment
        costs[costIdx++] += getWeaponsAndEquipmentCost(ignoreAmmo);

        // Sum Costs
        for (int i = 0; i < costIdx; i++) {
            cost += costs[i];
        }

        costs[costIdx++] = -getPriceMultiplier(); // Negative indicates multiplier
        cost = Math.round(cost * getPriceMultiplier());
        addCostDetails(cost, costs);
        return cost;

    }

    @Override
    public double getPriceMultiplier() {
        return modular ? 50.0 : 5.0; // weight multiplier
    }

    private void addCostDetails(double cost, double[] costs) {
        bvText = new StringBuffer();
        String[] left = { "Bridge", "Computer", "Life Support", "Sensors", "FCS", "Gunnery Control Systems",
                "Structural Integrity", "Engine", "Engine Control Unit",
                "Attitude Thrusters", "Docking Collars",
                "Fuel Tanks", "Armor", "Heat Sinks", "Life Boats/Escape Pods", "Grav Decks",
                "Bays", "Quarters", "HPG", "Weapons/Equipment", "Weight Multiplier" };

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

            if (l == 18) {
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

    @Override
    public long getEntityType() {
        return Entity.ETYPE_AERO | Entity.ETYPE_JUMPSHIP | Entity.ETYPE_SPACE_STATION;
    }

}
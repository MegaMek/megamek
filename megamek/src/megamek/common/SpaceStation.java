/*
* MegaAero - Copyright (C) 2007 Jay Lawson
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

import megamek.client.ui.swing.calculationReport.CalculationReport;
import megamek.common.cost.SpaceStationCostCalculator;
import megamek.common.options.OptionsConstants;

/**
 * @author Jay Lawson
 * @since Jun 17, 2007
 */
public class SpaceStation extends Jumpship {
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
    public double getCost(CalculationReport calcReport, boolean ignoreAmmo) {
        return SpaceStationCostCalculator.calculateCost(this, calcReport, ignoreAmmo);
    }

    @Override
    public double getPriceMultiplier() {
        return modular ? 50.0 : 5.0;
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
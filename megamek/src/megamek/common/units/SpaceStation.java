/*
 * Copyright (C) 2007 Jay Lawson
 * Copyright (C) 2025 The MegaMek Team. All Rights Reserved.
 *
 * This file is part of MegaMek.
 *
 * MegaMek is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License (GPL),
 * version 3 or (at your option) any later version,
 * as published by the Free Software Foundation.
 *
 * MegaMek is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty
 * of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU General Public License for more details.
 *
 * A copy of the GPL should have been included with this project;
 * if not, see <https://www.gnu.org/licenses/>.
 *
 * NOTICE: The MegaMek organization is a non-profit group of volunteers
 * creating free software for the BattleTech community.
 *
 * MechWarrior, BattleMech, `Mech and AeroTech are registered trademarks
 * of The Topps Company, Inc. All Rights Reserved.
 *
 * Catalyst Game Labs and the Catalyst Game Labs logo are trademarks of
 * InMediaRes Productions, LLC.
 *
 * MechWarrior Copyright Microsoft Corporation. MegaMek was created under
 * Microsoft's "Game Content Usage Rules"
 * <https://www.xbox.com/en-US/developers/rules> and it is not endorsed by or
 * affiliated with Microsoft.
 */

package megamek.common.units;

import java.io.Serial;

import megamek.client.ui.clientGUI.calculationReport.CalculationReport;
import megamek.common.SimpleTechLevel;
import megamek.common.TechAdvancement;
import megamek.common.cost.SpaceStationCostCalculator;
import megamek.common.enums.AvailabilityValue;
import megamek.common.enums.Faction;
import megamek.common.enums.TechBase;
import megamek.common.enums.TechRating;
import megamek.common.options.OptionsConstants;

/**
 * @author Jay Lawson
 * @since Jun 17, 2007
 */
public class SpaceStation extends Jumpship {
    @Serial
    private static final long serialVersionUID = -3160156173650960985L;

    /** A station over this weight in may built as a modular station. */
    public static final double MODULAR_MININUM_WEIGHT = 100000.0;

    // This only affects cost, but may have an effect in a large-scale strategic setting.
    private boolean modularOrKFAdapter = false;

    @Override
    public int getUnitType() {
        return UnitType.SPACE_STATION;
    }

    public SpaceStation() {
        super();
        setDriveCoreType(DRIVE_CORE_NONE);
        setSail(false);
    }

    private static final TechAdvancement TA_SPACE_STATION = new TechAdvancement(TechBase.ALL)
          .setAdvancement(DATE_ES, DATE_ES)
          .setTechRating(TechRating.D)
          .setAvailability(AvailabilityValue.C, AvailabilityValue.D, AvailabilityValue.C, AvailabilityValue.C)
          .setStaticTechLevel(SimpleTechLevel.ADVANCED);

    private static final TechAdvancement TA_SPACE_STATION_KF_ADAPTER = new TechAdvancement(TechBase.ALL)
          .setISAdvancement(2350, 2375, DATE_NONE, 2850, 3048).setClanAdvancement(2350, 2375)
          .setPrototypeFactions(Faction.TH).setProductionFactions(Faction.TH)
          // The adapter itself is tech rating C, but this is the base for a station with an adapter.
          .setReintroductionFactions(Faction.FS).setTechRating(TechRating.D)
          .setAvailability(AvailabilityValue.D, AvailabilityValue.F, AvailabilityValue.D, AvailabilityValue.D)
          .setStaticTechLevel(SimpleTechLevel.ADVANCED);

    private static final TechAdvancement TA_SPACE_STATION_MODULAR = new TechAdvancement(TechBase.ALL)
          .setISAdvancement(2565, 2585, DATE_NONE, 2790, 3090).setClanAdvancement(2565, 2585)
          .setPrototypeFactions(Faction.TH).setProductionFactions(Faction.TH)
          .setReintroductionFactions(Faction.RS).setTechRating(TechRating.D)
          .setAvailability(AvailabilityValue.F, AvailabilityValue.F, AvailabilityValue.F, AvailabilityValue.F)
          .setStaticTechLevel(SimpleTechLevel.ADVANCED);

    @Override
    public TechAdvancement getConstructionTechAdvancement() {
        if (isModular()) {
            return TA_SPACE_STATION_MODULAR;
        } else if (hasKFAdapter()) {
            return TA_SPACE_STATION_KF_ADAPTER;
        } else {
            return TA_SPACE_STATION;
        }
    }

    public static TechAdvancement getKFAdapterTA() {return TA_SPACE_STATION_KF_ADAPTER;}

    public static TechAdvancement getModularTA() {
        return TA_SPACE_STATION_MODULAR;
    }

    /**
     * Designates whether this is a modular space station
     *
     * @param modularOrKFAdapter Whether the space station can be transported by jumpship.
     */
    public void setModularOrKFAdapter(boolean modularOrKFAdapter) {
        this.modularOrKFAdapter = modularOrKFAdapter;
    }

    /**
     * @return True if this space station has a modular construction (or has a KF adapter for stations less than 100kt,
     *       otherwise false.
     */
    public boolean isModularOrKFAdapter() {
        return modularOrKFAdapter;
    }

    public boolean isModular() {
        return modularOrKFAdapter && getWeight() > MODULAR_MININUM_WEIGHT;
    }

    public boolean hasKFAdapter() {
        return modularOrKFAdapter && getWeight() <= MODULAR_MININUM_WEIGHT;
    }

    @Override
    public double getCost(CalculationReport calcReport, boolean ignoreAmmo) {
        return SpaceStationCostCalculator.calculateCost(this, calcReport, ignoreAmmo);
    }

    @Override
    public double getPriceMultiplier() {
        if (isModular()) {
            return 50.0;
        } else if (hasKFAdapter()) {
            return 20.0;
        } else {
            return 5.0;
        }
    }

    @Override
    public int getECMRange() {
        if (!isActiveOption(OptionsConstants.ADVANCED_AERO_RULES_STRATOPS_ECM) || !isSpaceborne()) {
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
    public boolean isBattleStation() {
        return designType == MILITARY;
    }

    @Override
    public double getBVTypeModifier() {
        return 0.7;
    }

    @Override
    public long getEntityType() {
        return Entity.ETYPE_AERO | Entity.ETYPE_JUMPSHIP | Entity.ETYPE_SPACE_STATION;
    }

    @Override
    public boolean isJumpShip() {
        return false;
    }

    @Override
    public boolean isSpaceStation() {
        return true;
    }

    @Override
    public int getGenericBattleValue() {
        return (int) Math.round(Math.exp(5.1322 + 0.2384 * Math.log(getWeight())));
    }
}

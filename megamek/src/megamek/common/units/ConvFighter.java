/*
 * Copyright (C) 2007 Jay Lawson
 * Copyright (C) 2008-2025 The MegaMek Team. All Rights Reserved.
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
import megamek.common.cost.ConvFighterCostCalculator;
import megamek.common.enums.AvailabilityValue;
import megamek.common.enums.Faction;
import megamek.common.enums.TechBase;
import megamek.common.enums.TechRating;
import megamek.common.interfaces.ITechnology;
import megamek.common.options.OptionsConstants;

/**
 * @author Jay Lawson
 * @since Jun 12, 2008
 */
public class ConvFighter extends AeroSpaceFighter {
    @Serial
    private static final long serialVersionUID = 6297668284292929409L;

    @Override
    public int getUnitType() {
        return UnitType.CONV_FIGHTER;
    }

    @Override
    public boolean isConventionalFighter() {
        return true;
    }

    @Override
    public boolean isAerospaceFighter() {
        return false;
    }

    @Override
    public boolean doomedInVacuum() {
        return true;
    }

    @Override
    public boolean doomedInSpace() {
        return true;
    }

    @Override
    public int getHeatCapacity() {
        return DOES_NOT_TRACK_HEAT;
    }

    @Override
    public boolean tracksHeat() {
        return false;
    }

    @Override
    public double getFuelPointsPerTon() {
        return 160;
    }

    @Override
    public int getFuelUsed(int thrust) {
        if (!hasEngine() || !requiresFuel()) {
            return 0;
        }
        int overThrust = Math.max(thrust - getWalkMP(), 0);
        int safeThrust = thrust - overThrust;
        int used = safeThrust + (2 * overThrust);
        if (!getEngine().isFusion()) {
            used = (int) Math.floor(safeThrust * 0.5) + overThrust;
        } else if (gameOptions().booleanOption(OptionsConstants.ADVANCED_AERO_RULES_STRATOPS_CONV_FUSION_BONUS)) {
            used = (int) Math.floor(safeThrust * 0.5) + (2 * overThrust);
        }
        return used;
    }

    protected static final TechAdvancement TA_CONV_FIGHTER = new TechAdvancement(TechBase.ALL)
          .setAdvancement(ITechnology.DATE_NONE, 2470, 2490)
          .setProductionFactions(Faction.TH)
          .setTechRating(TechRating.D)
          .setAvailability(AvailabilityValue.C,
                AvailabilityValue.D,
                AvailabilityValue.C,
                AvailabilityValue.B)
          .setStaticTechLevel(SimpleTechLevel.STANDARD);

    @Override
    public TechAdvancement getConstructionTechAdvancement() {
        return TA_CONV_FIGHTER;
    }

    @Override
    public double getBVTypeModifier() {
        return 1.1;
    }

    @Override
    public double getCost(CalculationReport calcReport, boolean ignoreAmmo) {
        return ConvFighterCostCalculator.calculateCost(this, calcReport, ignoreAmmo);
    }

    @Override
    public double getPriceMultiplier() {
        double priceMultiplier = 1.0;
        // omni multiplier (leaving this in for now even though conventional fighters
        // don't make for legal OmniMeks)
        if (isOmni()) {
            priceMultiplier *= 1.25f;
        }
        priceMultiplier *= 1 + (weight / 200.0); // weight multiplier
        return priceMultiplier;
    }

    @Override
    protected int calculateWalk() {
        if (!hasEngine()) {
            return 0;
        }
        if (isPrimitive()) {
            double rating = getEngine().getRating();
            rating /= 1.2;
            if ((rating % 5) != 0) {
                return (int) (((rating - (rating % 5)) + 5) / (int) weight);
            }
            return (int) (rating / (int) weight);
        }
        return (getEngine().getRating() / (int) weight);
    }

    @Override
    public long getEntityType() {
        return Entity.ETYPE_AERO | Entity.ETYPE_CONV_FIGHTER;
    }

    @Override
    public int getGenericBattleValue() {
        return (int) Math.round(Math.exp(2.943 + 0.795 * Math.log(getWeight())));
    }

    @Override
    public int getRecoveryTime() {
        return 60;
    }
}

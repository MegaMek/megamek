/*
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

package megamek.common.equipment.enums;


import megamek.common.equipment.Engine;

import java.io.Serializable;

/**
 * {@link megamek.common.units.MobileStructure} and {@link megamek.common.units.BuildingEntity} both have some unique
 * and shared rules for Engines.
 * <ul>
 *     <li>Building Entity weight multiplier and daily fuel weight from TO:AR 132</li>
 *     <li>Base Cost for Mobile Structure and Building Entity engine/generator from TO:AUE 208</li>
 *     <li>Mobile Structure power and motive system multipliers and fuel multiplier from TO:AUE 78</li>
 * </ul>
 */
public enum StructureEngine implements Serializable {

    STEAM(Engine.STEAM, 3.0, 1.5, 4000.0, 6.0, 6.0, 6.0, 7.0, 7.0, 7.0, 7.0, 8.0, 4.0),
    /**
     * {@link megamek.common.units.BuildingEntity} only, also prohibits mounting equipment on the roof
     */
    SOLAR(Engine.SOLAR, 3.0, 0.0, 8000.0, -1.0, -1.0, -1.0, -1.0, -1.0, -1.0, -1.0, -1.0, -1.0),
    FISSION(Engine.FISSION, 1.5, 0.0, 15000.0, 3.0, 3.0, 3.0, 3.0, 4.0, 4.0, 4.0, 4.0, 0.0),
    FUSION(Engine.NORMAL_ENGINE, 1.0, 0.0, 10000.0, 2.0, 2.0, 2.0, 2.2, 1.8, 1.8, 1.8, 2.0, 0.0),
    COMBUSTION_LIQUID(Engine.COMBUSTION_ENGINE, 1.5, 1.0, 5000.0, 3.0, 3.0, 3.0, 3.2, 3.0, 3.0, 3.0, 3.0, 2.0),
    COMBUSTION_SOLID(Engine.COMBUSTION_ENGINE, 2.0, 2.0, 5000.0, 3.0, 3.0, 3.0, 3.2, 3.0, 3.0, 3.0, 3.0, 2.0),
    FUEL_CELL(Engine.FUEL_CELL, 1.0, 1.2, 7000.0, 4.0, 4.4, 4.0, 5.0, 4.0, 4.2, 4.0, 4.4, 2.0),
    /**
     * {@link megamek.common.units.BuildingEntity} only.
     */
    EXTERNAL_PCMT(Engine.EXTERNAL, 0.5, 0.0, 5000.0, -1.0, -1.0, -1.0, -1.0, -1.0, -1.0, -1.0, -1.0, -1.0),
    /**
     * {@link megamek.common.units.BuildingEntity} only.
     */
    EXTERNAL(Engine.EXTERNAL, 0.5, 0.0, 5000.0, -1.0, -1.0, -1.0, -1.0, -1.0, -1.0, -1.0, -1.0, -1.0);


    /**
     * Corresponds to one of the types in {@link Engine}
     */
    final int engineType;

    /**
     * Power Generators Table, TO:AR 132
     */
    final double buildingWeightMultiplier;
    /**
     * Power Generators Table, TO:AR 132
     */
    final double buildingDailyFuelWeight;
    /**
     * Building and Mobile Structure Costs, TO:AUE 208
     */
    final double baseCost;

    /**
     * Mobile Structure Power and Motive System Tables, TO:AUE 78
     */
    final double groundMobileStructurePowerSystemWeightMultiplierIS;
    /**
     * Mobile Structure Power and Motive System Tables, TO:AUE 78
     */
    final double airMobileStructurePowerSystemWeightMultiplierIS;
    /**
     * Mobile Structure Power and Motive System Tables, TO:AUE 78
     */
    final double surfaceNavalMobileStructurePowerSystemWeightMultiplierIS;
    /**
     * Mobile Structure Power and Motive System Tables, TO:AUE 78
     */
    final double submarineNavalMobileStructurePowerSystemWeightMultiplierIS;
    /**
     * Mobile Structure Power and Motive System Tables, TO:AUE 78
     */
    final double groundMobileStructurePowerSystemWeightMultiplierClan;
    /**
     * Mobile Structure Power and Motive System Tables, TO:AUE 78
     */
    final double airMobileStructurePowerSystemWeightMultiplierClan;
    /**
     * Mobile Structure Power and Motive System Tables, TO:AUE 78
     */
    final double surfaceNavalMobileStructurePowerSystemWeightMultiplierClan;
    /**
     * Mobile Structure Power and Motive System Tables, TO:AUE 78
     */
    final double submarineNavalMobileStructurePowerSystemWeightMultiplierClan;

    /**
     * Mobile Structure Power and Motive System Tables, TO:AUE 78
     */
    final double mobileStructureFuelMultiplier;

    StructureEngine(int engineType, double buildingWeightMultiplier, double buildingDailyFuelWeight, double baseCost,
          double groundMobileStructurePowerSystemWeightMultiplierIS,
          double airMobileStructurePowerSystemWeightMultiplierIS,
          double surfaceNavalMobileStructurePowerSystemWeightMultiplierIS,
          double submarineNavalMobileStructurePowerSystemWeightMultiplierIS,
          double groundMobileStructurePowerSystemWeightMultiplierClan,
          double airMobileStructurePowerSystemWeightMultiplierClan,
          double surfaceNavalMobileStructurePowerSystemWeightMultiplierClan,
          double submarineNavalMobileStructurePowerSystemWeightMultiplierClan, double mobileStructureFuelMultiplier) {
        this.engineType = engineType;
        this.buildingWeightMultiplier = buildingWeightMultiplier;
        this.buildingDailyFuelWeight = buildingDailyFuelWeight;
        this.baseCost = baseCost;
        this.groundMobileStructurePowerSystemWeightMultiplierIS = groundMobileStructurePowerSystemWeightMultiplierIS;
        this.airMobileStructurePowerSystemWeightMultiplierIS =  airMobileStructurePowerSystemWeightMultiplierIS;
        this.surfaceNavalMobileStructurePowerSystemWeightMultiplierIS = surfaceNavalMobileStructurePowerSystemWeightMultiplierIS;
        this.submarineNavalMobileStructurePowerSystemWeightMultiplierIS = submarineNavalMobileStructurePowerSystemWeightMultiplierIS;
        this.groundMobileStructurePowerSystemWeightMultiplierClan = groundMobileStructurePowerSystemWeightMultiplierClan;
        this.airMobileStructurePowerSystemWeightMultiplierClan = airMobileStructurePowerSystemWeightMultiplierClan;
        this.surfaceNavalMobileStructurePowerSystemWeightMultiplierClan =  surfaceNavalMobileStructurePowerSystemWeightMultiplierClan;
        this.submarineNavalMobileStructurePowerSystemWeightMultiplierClan =  submarineNavalMobileStructurePowerSystemWeightMultiplierClan;
        this.mobileStructureFuelMultiplier = mobileStructureFuelMultiplier;
    }

    public int getEngineType() {
        return engineType;
    }

    public double getBuildingWeightMultiplier() {
        return buildingWeightMultiplier;
    }
}

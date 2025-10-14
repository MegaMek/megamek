/*
 * Copyright (c) 2000-2003 - Ben Mazur (bmazur@sev.org)
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

import megamek.common.SimpleTechLevel;
import megamek.common.TechAdvancement;
import megamek.common.enums.AvailabilityValue;
import megamek.common.enums.TechBase;
import megamek.common.enums.TechRating;
import megamek.common.equipment.ArmorType;
import megamek.common.equipment.MiscType;

/**
 * This is a support vehicle VTOL
 *
 * @author beerockxs
 */
public class SupportVTOL extends VTOL {
    @Serial
    private static final long serialVersionUID = 2771230410747098997L;
    private final int[] barRating;
    private double fuelTonnage = 0;

    public SupportVTOL() {
        super();
        barRating = new int[locations()];
    }

    @Override
    public void setBARRating(int rating, int loc) {
        barRating[loc] = rating;
    }

    @Override
    public void setBARRating(int rating) {
        for (int i = 0; i < locations(); i++) {
            barRating[i] = rating;
        }
    }

    @Override
    public int getBARRating(int loc) {
        return (barRating == null) ? 0 : barRating[loc];
    }

    @Override
    public boolean hasBARArmor(int loc) {
        return ArmorType.forEntity(this, loc).hasFlag(MiscType.F_SUPPORT_VEE_BAR_ARMOR);
    }

    @Override
    public boolean hasArmoredChassis() {
        return hasMisc(MiscType.F_ARMORED_CHASSIS);
    }

    private static final TechAdvancement TA_VTOL = new TechAdvancement(TechBase.ALL)
          .setAdvancement(DATE_PS, DATE_ES, DATE_ES)
          .setTechRating(TechRating.C)
          .setAvailability(AvailabilityValue.D, AvailabilityValue.E, AvailabilityValue.D, AvailabilityValue.D)
          .setStaticTechLevel(SimpleTechLevel.STANDARD);

    private static final TechAdvancement TA_VTOL_LARGE = new TechAdvancement(TechBase.ALL)
          .setAdvancement(DATE_PS, DATE_ES, DATE_ES)
          .setTechRating(TechRating.C)
          .setAvailability(AvailabilityValue.C, AvailabilityValue.D, AvailabilityValue.C, AvailabilityValue.C)
          .setStaticTechLevel(SimpleTechLevel.STANDARD);

    @Override
    public TechAdvancement getConstructionTechAdvancement() {
        return getConstructionTechAdvancement(getWeightClass());
    }

    public static TechAdvancement getConstructionTechAdvancement(int weightClass) {
        /* Support vehicle dates and tech ratings are found in TM 120, 122. DA availability is assumed to
         * be the same as Clan invasion era. */
        if (weightClass == EntityWeightClass.WEIGHT_LARGE_SUPPORT) {
            return TA_VTOL_LARGE;
        } else {
            return TA_VTOL;
        }
    }

    @Override
    public int getTotalCommGearTons() {
        return getExtraCommGearTons();
    }

    @Override
    public double getBaseEngineValue() {
        if (getWeight() < 5) {
            return 0.002;
        } else if (!isSuperHeavy()) {
            return 0.0025;
        } else {
            return 0.004;
        }
    }

    @Override
    public double getBaseChassisValue() {
        if (getWeight() < 5) {
            return 0.2;
        } else if (!isSuperHeavy()) {
            return 0.25;
        } else {
            return 0.3;
        }
    }

    @Override
    public double getFuelTonnage() {
        return fuelTonnage;
    }

    @Override
    public void setFuelTonnage(double fuel) {
        fuelTonnage = fuel;
    }

    @Override
    public int getTotalSlots() {
        return 5 + (int) Math.floor(getWeight() / 10);
    }

    @Override
    public long getEntityType() {
        return Entity.ETYPE_TANK | Entity.ETYPE_VTOL | Entity.ETYPE_SUPPORT_VTOL;
    }

    @Override
    public boolean isSupportVehicle() {
        return true;
    }

    @Override
    public int getGenericBattleValue() {
        return (int) Math.round(Math.exp(3.336 + 0.451 * Math.log(getWeight())));
    }
}

/*
 * Copyright (c) 2000-2003 - Ben Mazur (bmazur@sev.org)
 * Copyright (c) 2023 - The MegaMek Team. All Rights Reserved.
 *
 * This file is part of MegaMek.
 *
 * MegaMek is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * MegaMek is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with MegaMek. If not, see <http://www.gnu.org/licenses/>.
 */
package megamek.common;

import megamek.common.equipment.ArmorType;

/**
 * This is a support vehicle VTOL
 * @author beerockxs
 */
public class SupportVTOL extends VTOL {
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
    
    private static final TechAdvancement TA_VTOL = new TechAdvancement(TECH_BASE_ALL)
            .setAdvancement(DATE_PS, DATE_ES, DATE_ES)
            .setTechRating(RATING_C).setAvailability(RATING_D, RATING_E, RATING_D, RATING_D)
            .setStaticTechLevel(SimpleTechLevel.STANDARD);

    private static final TechAdvancement TA_VTOL_LARGE = new TechAdvancement(TECH_BASE_ALL)
            .setAdvancement(DATE_PS, DATE_ES, DATE_ES)
            .setTechRating(RATING_C).setAvailability(RATING_C, RATING_D, RATING_C, RATING_C)
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
    protected int getGenericBattleValue() {
        return (int) Math.round(Math.exp(3.336 + 0.451*Math.log(getWeight())));
    }
}
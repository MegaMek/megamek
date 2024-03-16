/*
* MegaMek -
* Copyright (C) 2018 The MegaMek Team
*
* This program is free software; you can redistribute it and/or modify it under
* the terms of the GNU General Public License as published by the Free Software
* Foundation; either version 2 of the License, or (at your option) any later
* version.
*
* This program is distributed in the hope that it will be useful, but WITHOUT
* ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
* FOR A PARTICULAR PURPOSE. See the GNU General Public License for more
* details.
*/

package megamek.common;

import java.text.DecimalFormat;

/**
 * Reinforced naval repair facility allows ship to expend thrust with docked unit. Only available
 * unpressurized. See TacOps 334-5 for rules.
 *
 * @author Neoancient
 *
 */
public class ReinforcedRepairFacility extends NavalRepairFacility {

    private static final long serialVersionUID = -3474202393188929092L;

    /**
     * The default constructor is only for serialization.
     */

    protected ReinforcedRepairFacility() {
        totalSpace = 0;
        currentSpace = 0;
    }

    /**
     * Create a new repair facility
     *
     * @param size   Maximum capacity in tons
     * @param doors
     * @param bayNumber
     * @param facing The armor facing of the facility
     */
    public ReinforcedRepairFacility(double size, int doors, int bayNumber, int facing) {
        super(size, doors, bayNumber, facing, false);
    }

    @Override
    public boolean isPressurized() {
        return false;
    }

    @Override
    public String getType() {
        return "Naval Repair Facility (Reinforced)";
    }

    @Override
    public double getWeight() {
        return RoundWeight.nextHalfTon(totalSpace * 0.1);
    }

    @Override
    public String toString() {
        return "reinforcedrepairfacility:"
                + totalSpace + FIELD_SEPARATOR
                + doors + FIELD_SEPARATOR
                + bayNumber + FIELD_SEPARATOR
                + FACING_PREFIX + getFacing();
    }

    @Override
    public String getUnusedString(boolean showRecovery) {
        StringBuilder sb = new StringBuilder("Reinforced Naval Repair Facility: ");
        sb.append(DecimalFormat.getInstance().format(totalSpace)).append(" tons");
        return sb.toString();
    }

    public static TechAdvancement techAdvancement() {
        return new TechAdvancement(TECH_BASE_IS).setAdvancement(2750, DATE_NONE, DATE_NONE, 2766, 3065)
                .setISApproximate(true, false, false, false, false)
                .setPrototypeFactions(F_TH).setReintroductionFactions(F_WB).setTechRating(RATING_C)
                .setAvailability(RATING_F, RATING_X, RATING_F, RATING_F)
                .setStaticTechLevel(SimpleTechLevel.EXPERIMENTAL);
    }

    @Override
    public long getCost() {
        return 30000L * (long) totalSpace;
    }

}

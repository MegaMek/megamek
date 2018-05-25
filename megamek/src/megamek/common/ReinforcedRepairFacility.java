/*
 * MegaMek - Copyright (C) 2018 - The MegaMek Team
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
     * @param facing The armor facing of the facility
     */
    public ReinforcedRepairFacility(double size, int facing) {
        super(size, false, facing);
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
    public String toString() {
        return "reinforcedrepairfacility:" + totalSpace + ":1:" + bayNumber;
    }
    
    public static TechAdvancement techAdvancement() {
        return new TechAdvancement(TECH_BASE_IS).setAdvancement(2750, DATE_NONE, DATE_NONE, 2766, 3065)
                .setISApproximate(true, false, false, false, false)
                .setPrototypeFactions(F_TH).setReintroductionFactions(F_WB).setTechRating(RATING_C)
                .setAvailability(RATING_F, RATING_X, RATING_F, RATING_F)
                .setStaticTechLevel(SimpleTechLevel.EXPERIMENTAL);
    }


}

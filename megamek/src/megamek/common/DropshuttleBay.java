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
 * Implements internal bays for dropships used by primitive jumpships.
 * See rules IO, p. 119.
 * 
 * @author Neoancient
 *
 */
public class DropshuttleBay extends Bay {
    
    /**
     * 
     */
    private static final long serialVersionUID = -6910402023514976670L;
    
    // No more than one bay is allowed per armor facing
    private int facing = Entity.LOC_NONE;

    /**
     * The default constructor is only for serialization.
     */

    protected DropshuttleBay() {
        totalSpace = 0;
        currentSpace = 0;
    }

    /**
     * Create a new dropshuttle bay
     *
     * @param doors     The number of bay doors
     * @param bayNumber The bay index, unique to the Entity 
     * @param facing    The armor facing of the bay
     */
    public DropshuttleBay(int doors, int bayNumber, int facing) {
        totalSpace = 2;
        currentSpace = 2;
        this.doors = doors;
        doorsNext = doors;
        this.bayNumber = bayNumber;
        currentdoors = doors;
        this.facing = facing;
    }

    // Type is Dropshuttle Bay
    @Override
    public String getType() {
        return "Dropshuttle Bay";
    }

    @Override
    public boolean canLoad(Entity unit) {
        
        return unit.hasETypeFlag(Entity.ETYPE_DROPSHIP)
                && (unit.getWeight() <= 5000)
                && (currentSpace >= 1);
    }
    
    @Override
    public double getWeight() {
        return 11000;
    }
    
    @Override
    public int getFacing() {
        return facing;
    }
    
    /**
     * Sets the bay location
     * @param facing The armor facing (location) of the bay
     */
    @Override
    public void setFacing(int facing) {
        this.facing = facing;
    }
    
    @Override
    public String toString() {
        String bayType = "dropshuttlebay";
        return this.bayString(
                bayType,
                totalSpace,
                doors,
                bayNumber,
                "",
                facing,
                0
        );
    }
    
    @Override
    public int hardpointCost() {
        return 2;
    }
    
    public static TechAdvancement techAdvancement() {
        return new TechAdvancement(TECH_BASE_IS).setISAdvancement(2110, 2120, DATE_NONE, 2500)
                .setISApproximate(true, false).setTechRating(RATING_C)
                .setProductionFactions(F_TA).setProductionFactions(F_TA)
                .setAvailability(RATING_C, RATING_X, RATING_X, RATING_X)
                .setStaticTechLevel(SimpleTechLevel.ADVANCED);
    }

    @Override
    public long getCost() {
        // Set cost for 2-capacity bay
        return 150000000;
    }

}

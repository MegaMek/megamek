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

import java.text.DecimalFormat;

/**
 * Standard naval repair facilities for space stations (jumpships and warships can also carry a single facility).
 * See TacOps 334-5 for rules.
 * 
 * @author Neoancient
 *
 */
public class NavalRepairFacility extends Bay {
    
    private static final long serialVersionUID = -5983197195382933671L;
    
    // No more than one bay is allowed per armor facing
    private int facing = Entity.LOC_NONE;
    private boolean pressurized = false;

    /**
     * The default constructor is only for serialization.
     */

    protected NavalRepairFacility() {
        totalSpace = 0;
        currentSpace = 0;
    }

    /**
     * Create a new repair facility
     *
     * @param size   Maximum capacity in tons
     * @param pressurized Whether the facility is pressurized
     * @param facing The armor facing of the facility
     */
    public NavalRepairFacility(double size, int doors, int bayNumber, int facing, boolean pressurized) {
        totalSpace = size;
        currentSpace = size;
        this.doors = doors;
        doorsNext = doors;
        this.bayNumber = bayNumber;
        currentdoors = doors;
        this.facing = facing;
        this.pressurized = pressurized;
    }
    
    /**
     * Pressurized facility allows crew to work without encumbrance of life support gear. No game effect
     * that I could find.
     * @return Whether the facility is pressurized.
     */
    public boolean isPressurized() {
        return pressurized;
    }
    
    public void setPressurized(boolean pressurized) {
        this.pressurized = pressurized;
    }

    public String getType() {
        return "Naval Repair Facility " + (pressurized? "(Pressurized)" : "Unpressurized");
    }

    public boolean canLoad(Entity unit) {
        if (unit.getWeight() > currentSpace) {
            return false;
        }
        // We can carry two dropships or one JS/WS/SS.
        if (unit.hasETypeFlag(Entity.ETYPE_DROPSHIP)) {
            return troops.isEmpty()
                    || ((troops.size() == 1)
                            && (null != unit.getGame().getEntity(troops.get(0)))
                            && (unit.getGame().getEntity(troops.get(0)).hasETypeFlag(Entity.ETYPE_DROPSHIP)));
        } else if (unit.hasETypeFlag(Entity.ETYPE_JUMPSHIP)) {
            return troops.isEmpty();
        } else {
            return false;
        }
    }
    
    @Override
    public double getWeight() {
        return Math.ceil(2 * totalSpace * (pressurized? 0.075 : 0.025)) * 0.5;
    }
    
    @Override
    public int getFacing() {
        return facing;
    }
    
    /**
     * Sets the bay location
     * @param facing The armor facing (location) of the bay
     */
    public void setFacing(int facing) {
        this.facing = facing;
    }
    
    @Override
    public String toString() {
        return "navalrepair" + (pressurized? "pressurized:" : "unpressurized:")
                + totalSpace + FIELD_SEPARATOR
                + doors + FIELD_SEPARATOR
                + bayNumber + FIELD_SEPARATOR
                + FACING_PREFIX + getFacing();
    }
    
    @Override
    public String getUnusedString(boolean showrecovery) {
        StringBuilder sb = new StringBuilder("Standard ");
        if (pressurized) {
            sb.append("Pressurized");
        } else {
            sb.append("Unpressurized");
        }
        sb.append(" Naval Repair Facility: ");
        sb.append(DecimalFormat.getInstance().format(totalSpace)).append(" tons");
        return sb.toString();
    }
    
    @Override
    public int hardpointCost() {
        return (int) Math.ceil(totalSpace / 50000);
    }
    
    public static TechAdvancement techAdvancement() {
        return new TechAdvancement(TECH_BASE_ALL).setAdvancement(DATE_ES, DATE_ES, DATE_ES)
                .setTechRating(RATING_C)
                .setAvailability(RATING_C, RATING_E, RATING_D, RATING_D)
                .setStaticTechLevel(SimpleTechLevel.ADVANCED);
    }


}

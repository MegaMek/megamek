/*
 * MegaMek - Copyright (C) 2003, 2004 Ben Mazur (bmazur@sev.org)
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
 * Represtents a volume of space set aside for carrying ASFs and Small Craft
 * aboard DropShips
 */

public final class BattleArmorBay extends Bay {

    /**
     *
     */
    private static final long serialVersionUID = 7091227399812361916L;

    private boolean isClan = false;
    private boolean isComStar = false;

    /**
     * The default constructor is only for serialization.
     */
    protected BattleArmorBay() {
        totalSpace = 0;
        currentSpace = 0;
    }

    // Public constructors and methods.

    /**
     * Create a space for the given tonnage of troops. For this class, only the
     * weight of the troops (and their equipment) are considered; if you'd like
     * to think that they are stacked like lumber, be my guest.
     *
     * @param space
     *            - The weight of troops (in tons) this space can carry.
     * @param bayNumber
     * @param carrier
     */
    public BattleArmorBay(double space, int doors, int bayNumber, boolean isClan, boolean isComStar) {
        totalSpace = space;
        currentSpace = space;
        this.doors = doors;
        doorsNext = doors;
        this.bayNumber = bayNumber;
        this.isClan = isClan;
        this.isComStar = isComStar;
    }

    /**
     * Determines if this object can accept the given unit. The unit may not be
     * of the appropriate type or there may be no room for the unit.
     *
     * @param unit
     *            - the <code>Entity</code> to be loaded.
     * @return <code>true</code> if the unit can be loaded, <code>false</code>
     *         otherwise.
     */
    @Override
    public boolean canLoad(Entity unit) {
        // Assume that we cannot carry the unit.
        boolean result = false;

        // Only smallcraft
        if (unit instanceof BattleArmor) {
            result = true;
        }

        // We must have enough space for the new troops.
        // POSSIBLE BUG: we may have to take the Math.ceil() of the weight.
        if (currentSpace < 1) {
            result = false;
        }

        // is the door functional
        if (doors < loadedThisTurn) {
            result = false;
        }

        // Return our result.
        return result;
    }

    @Override
    public String getUnusedString(boolean showrecovery) {
        return "Battle Armor Bay - " + String.format("%1$,.0f", currentSpace) + (currentSpace > 1 ? isClan?" Points":isComStar?" Level I":" Squads" : isClan?" Point":isComStar?" Level I":" Squad");
    }

    @Override
    public String getType() {
        return "Battle Armor";
    }

    @Override
    public float getWeight() {
        return (float) totalSpace * 2 * (isClan?5:isComStar?6:4);
    }

    @Override
    public String toString() {
        return "battlearmorbay:" + totalSpace + ":" + doors + ":"+bayNumber+(isComStar?":C*":"");
    }

}

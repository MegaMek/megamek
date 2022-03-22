/*
* MegaMek -
* Copyright (C) 2003, 2004 Ben Mazur (bmazur@sev.org)
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

/**
 * Represents a volume of space set aside for carrying Battle Armor squads
 * aboard large spacecraft and mobile structures
 */
public final class BattleArmorBay extends Bay {
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
     * @param doors
     * @param bayNumber
     * @param isClan
     * @param isComStar
     */
    public BattleArmorBay(double space, int doors, int bayNumber, boolean isClan, boolean isComStar) {
        totalSpace = space;
        currentSpace = space;
        this.doors = doors;
        doorsNext = doors;
        this.bayNumber = bayNumber;
        this.isClan = isClan;
        this.isComStar = isComStar;
        currentdoors = doors;
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

        // Only Battle Armor squads
        if (unit instanceof BattleArmor) {
            result = true;
        }

        // We must have enough space for the new troops.
        // POSSIBLE BUG: we may have to take the Math.ceil() of the weight.
        if (getUnused() < 1) {
            result = false;
        }

        // is the door functional
        if (currentdoors < loadedThisTurn) {
            result = false;
        }

        // Return our result.
        return result;
    }

    @Override
    public String getUnusedString(boolean showrecovery) {
        return "Battle Armor Bay " + numDoorsString() + " - "
                + String.format("%1$,.0f", getUnused())
                + (getUnused() > 1 ? isClan ? " Points"
                        : isComStar ? " Level I" : " Squads"
                        : isClan ? " Point" : isComStar ? " Level I" : " Squad");
    }

    @Override
    public String getType() {
        return "Battle Armor";
    }

    @Override
    public double getWeight() {
        return totalSpace * 2 * (isClan ? 5 : (isComStar ? 6 : 4));
    }

    @Override
    public int getPersonnel(boolean clan) {
        return (int) totalSpace * 6;
    }

    @Override
    public String toString() {
        return "battlearmorbay:" + totalSpace + ":" + doors + ":"+bayNumber+(isComStar?":C*":"");
    }

    public static TechAdvancement techAdvancement() {
        return new TechAdvancement(TECH_BASE_ALL)
                .setClanAdvancement(2867, 2868, 2870,DATE_NONE,DATE_NONE )
                .setClanApproximate(true, false, false, false, false)
                .setISAdvancement(DATE_NONE, DATE_NONE, 3050,DATE_NONE,DATE_NONE)
                .setPrototypeFactions(F_CWF).setTechRating(RATING_D)
                .setAvailability(RATING_X, RATING_X, RATING_C, RATING_B)
                .setStaticTechLevel(SimpleTechLevel.STANDARD);
    }

    @Override
    public TechAdvancement getTechAdvancement() {
        return BattleArmorBay.techAdvancement();
    }

    @Override
    public boolean isClan() {
        return isClan;
    }

    @Override
    public long getCost() {
        // Based on the weight of the equipment (not capacity), rounded up to the whole ton
        return 15000L * (long) Math.ceil(getWeight());
    }

}

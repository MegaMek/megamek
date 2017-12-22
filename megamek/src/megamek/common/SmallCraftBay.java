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

import java.util.Vector;

/**
 * Represents a volume of space set aside for carrying ASFs and Small Craft
 * aboard large spacecraft and mobile structures
 */

public final class SmallCraftBay extends Bay {

    /**
     *
     */
    private static final long serialVersionUID = -8275147432497460821L;

    /**
     * The default constructor is only for serialization.
     */
    protected SmallCraftBay() {
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
     */
    public SmallCraftBay(double space, int doors, int bayNumber) {
        totalSpace = space;
        currentSpace = space;
        this.doors = doors;
        doorsNext = doors;
        this.currentdoors = doors;
        recoverySlots = initializeRecoverySlots();
        this.bayNumber = bayNumber;
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

        // Only ASFs, Fighter-mode LAMs or Small Craft
        if (((unit.isAero()) 
        		&& !(unit instanceof FighterSquadron) 
        		&& !(unit instanceof Dropship) 
        		&& !(unit instanceof Jumpship)) 
        		|| ((unit instanceof LandAirMech) && (unit.getConversionMode() == LandAirMech.CONV_MODE_FIGHTER))) {
            result = true;
        }

        // System.err.print("Current space to load " + unit.getShortName() +
        // " is " + this.currentSpace + "\n");
        if (getUnused() < 1) {
            result = false;
        }

        // is there at least one recovery slot available?
        if (getRecoverySlots() < 1) {
            result = false;
        }
        
        // Return our result.
        return result;
    }

    /**
     * Load the given unit.
     *
     * @param unit
     *            - the <code>Entity</code> to be loaded.
     * @exception - If the unit can't be loaded, an
     *            <code>IllegalArgumentException</code> exception will be
     *            thrown.
     */
    @Override
    public void load(Entity unit) throws IllegalArgumentException {
        // If we can't load the unit, throw an exception.
        if (!canLoad(unit)) {
            throw new IllegalArgumentException("Can not load " + unit.getShortName() + " into this bay. " + getUnused());
        }

        currentSpace -= 1;

        // Add the unit to our list of troops.
        troops.addElement(unit.getId());
    }

    // Recovery is different from loading in that it uses up a recovery slot
    // load is only used in deployment phase
    public void recover(Entity unit) throws IllegalArgumentException {
        // If we can't load the unit, throw an exception.
        if (!canLoad(unit)) {
            throw new IllegalArgumentException("Can not recover " + unit.getShortName() + " into this bay. " + getUnused());
        }

        currentSpace -= 1;

        // get the closest open recovery slot and make it unavailable
        closeSlot();

        // Add the unit to our list of troops.
        troops.addElement(unit.getId());
    }

    @Override
    public String getUnusedString(boolean showrecovery) {
        if (showrecovery) {
            return "Small Craft " + numDoorsString() + " - "
                    + String.format("%1$,.0f", getUnused())
                    + (getUnused() > 1 ? " units (" : " unit (")
                    + getRecoverySlots() + " recovery open)";
        } else {
            return "Small Craft " + numDoorsString() + " - "
                    + String.format("%1$,.0f", getUnused())
                    + (getUnused() > 1 ? " units" : " unit");
        }
    }

    @Override
    public String getType() {
        return "Small Craft";
    }

    // update the time remaining in recovery slots
    public void updateSlots() {
        if (recoverySlots.size() < 1) {
            return;
        }

        for (int i = recoverySlots.size() - 1; i >= 0; i--) {
            if (recoverySlots.elementAt(i) > 0) {
                int temp = recoverySlots.elementAt(i) - 1;
                recoverySlots.remove(i);
                recoverySlots.add(temp);
            }
        }
    }

    public Vector<Integer> initializeRecoverySlots() {

        Vector<Integer> slots = new Vector<Integer>();
        // We have to account for changes in the number of doors, so remove all slots first.
    	slots.removeAllElements();
    	//now add 2 slots back on for each functional door.
        for (int i = 0; i < currentdoors; i++) {
            slots.add(0);
            slots.add(0);
        }
        recoverySlots = slots;
        return slots;
    }

    // check how many available slots we have
    public int getRecoverySlots() {
        // a zero means it is available
        int avail = 0;
        if (null == recoverySlots) {
            return avail;
        }
        for (int i = 0; i < recoverySlots.size(); i++) {
            if (recoverySlots.elementAt(i) == 0) {
                avail++;
            }
        }
        return avail;
    }

    public void closeSlot() {
        for (int i = 0; i < recoverySlots.size(); i++) {
            if (recoverySlots.elementAt(i) == 0) {
                recoverySlots.remove(i);
                recoverySlots.add(5);
                break;
            }
        }
    }

    // destroy a door
    @Override
    public void destroyDoorNext() {

        setDoorsNext(getDoorsNext() - 1);

        // get rid of two empty recovery slots
        // it doesn't matter which ones
        if (recoverySlots.size() > 0) {
            recoverySlots.remove(0);
        }
        if (recoverySlots.size() > 0) {
            recoverySlots.remove(0);
        }
    }

    // destroy a door
    @Override
    public void destroyDoor() {
    	if (getCurrentDoors() > 0) {
    		setCurrentDoors(getCurrentDoors() - 1);
    	}
    
        // get rid of two empty recovery slots
        // it doesn't matter which ones
        if (recoverySlots.size() > 0) {
            recoverySlots.remove(0);
        }
        if (recoverySlots.size() > 0) {
            recoverySlots.remove(0);
        }
    }

    @Override
    public double getWeight() {
        return totalSpace * 200;
    }

    @Override
    public int getPersonnel(boolean clan) {
        return (int)totalSpace * 5;
    }

    @Override
    public String toString() {
        return "smallcraftbay:" + totalSpace + ":" + doors + ":"+ bayNumber;
    }

    public static TechAdvancement techAdvancement() {
        return new TechAdvancement(TECH_BASE_ALL).setAdvancement(DATE_ES, DATE_ES, DATE_ES)
                .setTechRating(RATING_C)
                .setAvailability(RATING_B, RATING_B, RATING_B, RATING_B)
                .setStaticTechLevel(SimpleTechLevel.STANDARD);
    }
    
    public TechAdvancement getTechAdvancement() {
        return SmallCraftBay.techAdvancement();
    }

} // End package class TroopSpace implements Transporter

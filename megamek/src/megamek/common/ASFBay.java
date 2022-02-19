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
 * Represents a volume of space set aside for carrying and launching ASFs
 * aboard large spacecraft and mobile structures
 */

public final class ASFBay extends Bay {

    private static final long serialVersionUID = -4110012474950158433L;

    private final boolean arts;

    /**
     * The default constructor is only for serialization.
     */
    protected ASFBay() {
        totalSpace = 0;
        currentSpace = 0;
        arts = false;
    }

    // Public constructors and methods.

    /**
     * Create a space for the given number of fighters.
     *
     * @param space      The number of cubicles
     * @param doors      The number of bay doors
     * @param bayNumber  The id number for the bay
     */
    public ASFBay(double space, int doors, int bayNumber) {
        this(space, doors, bayNumber, false);
    }
        /**
         * Create a space for the given number of fighters.
         *
         * @param space      The number of cubicles
         * @param doors      The number of bay doors
         * @param bayNumber  The id number for the bay
         * @param arts       Whether the bay has the advanced robotic transport system
         */
    public ASFBay(double space, int doors, int bayNumber, boolean arts) {
        totalSpace = space;
        currentSpace = space;
        this.doors = doors;
        doorsNext = doors;
        this.currentdoors = doors;
        recoverySlots = initializeRecoverySlots();
        this.bayNumber = bayNumber;
        this.arts = arts;
    }

    /**
     * Advanced Robotic Transport System (ARTS). See IO, p. 147
     *
     * @return Whether the bay has the ARTS automated system
     */
    public boolean hasARTS() {
        return arts;
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

        // Only ASFs or Fighter-Mode LAMs
        // (See IO Battleforce section for the rules that allow converted QVs and LAMs to use other bay types)
        if ((unit.isFighter() && !(unit instanceof FighterSquadron)) || ((unit instanceof LandAirMech) && (unit.getConversionMode() == LandAirMech.CONV_MODE_FIGHTER))) {
            result = true;
        }

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
     * @throws IllegalArgumentException- If the unit can't be loaded, an
     *            <code>IllegalArgumentException</code> exception will be
     *            thrown.
     */
    @Override
    public void load(Entity unit) throws IllegalArgumentException {
        // If we can't load the unit, throw an exception.
        if (!canLoad(unit)) {
            throw new IllegalArgumentException("Can not load "
                    + unit.getShortName() + " into this bay. " + getUnused());
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
            throw new IllegalArgumentException("Can not recover "
                    + unit.getShortName() + " into this bay. " + getUnused());
        }

        currentSpace -= 1;

        // get the closest open recovery slot and make it unavailable
        closeSlot();

        // Add the unit to our list of troops.
        troops.addElement(unit.getId());
    }

    @Override
    public String getUnusedString(boolean showrecovery) {
        StringBuilder sb = new StringBuilder();
        if (arts) {
            sb.append("ARTS ");
        }
        sb.append("Aerospace Fighter ");
        if (showrecovery) {
            sb.append(numDoorsString()).append(" - ")
                .append(String.format("%1$,.0f", getUnused()))
                .append(" units (").append(getRecoverySlots()).append(" recovery open)");
        } else {
            sb.append(String.format(" Bay %s - %2$,.0f", numDoorsString(), getUnused()))
                    .append(getUnused() > 1 ? " units" : " unit");
        }
        return sb.toString();
    }

    @Override
    public String getType() {
        return arts ? "ARTS Fighter" : "Fighter";
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

        Vector<Integer> slots = new Vector<>();
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
    
    // destroy a door for next turn
    @Override
    public void destroyDoorNext() {

    	if (getDoorsNext() > 0) {
    		setDoorsNext(getDoorsNext() - 1);
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
        return totalSpace * (arts ? 187.5 : 150);
    }

    @Override
    public int getPersonnel(boolean clan) {
        return arts ? 0 : (int) totalSpace * 2;
    }

    @Override
    public String toString() {
        return (arts ? "artsasfbay:" : "asfbay:") + totalSpace + ":" + doors + ":" + bayNumber;
    }

    public static TechAdvancement techAdvancement() {
        return new TechAdvancement(TECH_BASE_ALL).setAdvancement(DATE_ES, DATE_ES, DATE_ES)
                .setTechRating(RATING_C)
                .setAvailability(RATING_C, RATING_C, RATING_C, RATING_C)
                .setStaticTechLevel(SimpleTechLevel.STANDARD);
    }
    
    @Override
    public TechAdvancement getTechAdvancement() {
        if (arts) {
            return Bay.artsTechAdvancement();
        } else {
            return ASFBay.techAdvancement();
        }
    }

    @Override
    public long getCost() {
        // Based on the number of cubicles
        long cost = 20000L * (long) totalSpace;
        if (arts) {
            cost += 1000000L;
        }
        return cost;
    }

}

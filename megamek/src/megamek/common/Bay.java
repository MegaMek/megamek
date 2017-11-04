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

import java.util.ArrayList;
import java.util.List;
import java.util.Vector;

/**
 * Represents a volume of space set aside for carrying cargo of some sort
 * aboard large spacecraft and mobile structures
 */

public class Bay implements Transporter, ITechnology {

    // Private attributes and helper functions.

    /**
     *
     */
    private static final long serialVersionUID = -9056450317468016272L;
    int doors = 1;
    int doorsNext = 1;
    int currentdoors = doors;
    protected int unloadedThisTurn = 0;
    protected int loadedThisTurn = 0;
    Vector<Integer> recoverySlots = new Vector<Integer>();
    int bayNumber = 0;
    transient IGame game = null;
    private double damage;

    /**
     * The troops being carried.
     */
    /* package */Vector<Integer> troops = new Vector<Integer>();

    /**
     * The total amount of space available for troops.
     */
    /* package */double totalSpace;

    /**
     * The current amount of space not occupied by troops or cargo.
     */
    /* package */double currentSpace;

    // Protected constructors and methods.

    /**
     * The default constructor is only for serialization.
     */
    protected Bay() {
        totalSpace = 0;
        currentSpace = 0;
        damage = 0;
    }

    // Public constructors and methods.

    /**
     * Create a space for the given tonnage of troops. For this class, only the
     * weight of the troops (and their equipment) are considered; if you'd like
     * to think that they are stacked like lumber, be my guest.
     *
     * @param space
     *            - The weight of troops (in tons) this space can carry.
     * @param bayNumber2
     */
    public Bay(double space, int doors, int bayNumber) {
        totalSpace = space;
        currentSpace = space;
        this.doors = doors;
        doorsNext = currentdoors;
        this.bayNumber = bayNumber;
        damage = 0;
    }
    
    /**
     * Bay damage to unit transport bays is tracked by number of cubicles/units. Damage
     * to cargo bays is tracked by cargo tonnage.
     * 
     * @return The reduction of bay capacity due to damage.
     */
    public double getBayDamage() {
    	return damage;
    }
    
    /**
     * Bay damage to unit transport bays is tracked by number of cubicles/units. Damage
     * to cargo bays is tracked by cargo tonnage.
     *
     * @param damage The total amount of bay capacity reduced due to damage.
     */
    public void setBayDamage(double damage) {
    	this.damage = Math.min(damage, totalSpace);
    }
    
    // the starting number of doors for the bay.
    public int getDoors() {
        return doors;
    }

    public void setDoors(int d) {
        doors = d;
        doorsNext = d;
        currentdoors = d;
    }

    public int getCurrentDoors() {
        return currentdoors;
    }
    
    public void setCurrentDoors(int d) {
    	currentdoors = d;
    }
    
    // for setting doors after this launch
    public void setDoorsNext(int d) {
        doorsNext = d;
    }

    public int getDoorsNext() {
        return doorsNext;
    }

    public void resetDoors() {
        doorsNext = currentdoors;
    }

    public void resetCounts() {
        unloadedThisTurn = 0;
        loadedThisTurn = 0;
    }
    
    /**
     * Most bay types track space by individual units. Infantry bays have variable space requirements
     * and must track by cubicle tonnage.
     * 
     * @param unit The unit to load/unload.
     * @return     The amount of bay space taken up by the unit.
     */
    public double spaceForUnit(Entity unit) {
        return 1;
    }

    @Override
    public void resetTransporter() {
        troops = new Vector<Integer>();
        currentSpace = totalSpace;
        resetCounts();
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
        boolean result = true;

        // We must have enough space for the new troops.
        if (getUnused() < spaceForUnit(unit)) {
            result = false;
        }

        // more doors than units loaded
        if (currentdoors <= loadedThisTurn) {
            result = false;
        }
        
        // Return our result.
        return result;
    }

    /**
     * to unload units, a bay must have more doors available than units unloaded
     * this turn
     *
     * @return
     */
    
    // can't load, launch or recover into a damaged bay, but you can unload it
    public boolean canUnloadUnits() {
        return currentdoors > unloadedThisTurn;
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

        currentSpace -= spaceForUnit(unit);
        if((unit.game.getPhase() != IGame.Phase.PHASE_DEPLOYMENT) && (unit.game.getPhase() != IGame.Phase.PHASE_LOUNGE)) {
                loadedThisTurn += 1;
        }

        // Add the unit to our list of troops.
        troops.addElement(unit.getId());
    }

    /**
     * Get a <code>List</code> of the units currently loaded into this payload.
     *
     * @return A <code>List</code> of loaded <code>Entity</code> units. This
     *         list will never be <code>null</code>, but it may be empty. The
     *         returned <code>List</code> is independant from the under- lying
     *         data structure; modifying one does not affect the other.
     */
    @Override
    public Vector<Entity> getLoadedUnits() {
        // Return a copy of our list of troops.
        Vector<Entity> loaded = new Vector<Entity>();
        for (int unit : troops) {
            loaded.add(game.getEntity(unit));
        }
        return loaded;
    }

    /**
     * get a vector of launchable units. This is different from loaded in that
     * units in recovery cannot launch
     */
    public Vector<Entity> getLaunchableUnits() {

        Vector<Entity> launchable = new Vector<Entity>();

        for (int i = 0; i < troops.size(); i++) {
            Entity nextUnit = game.getEntity(troops.elementAt(i));
            if (nextUnit.getRecoveryTurn() == 0) {
                launchable.add(nextUnit);
            }
        }

        return launchable;
    }

    /**
     * get a vector of droppable units.
     */
    public Vector<Entity> getDroppableUnits() {

        Vector<Entity> droppable = new Vector<Entity>();

        for (int i = 0; i < troops.size(); i++) {
            Entity nextUnit = game.getEntity(troops.elementAt(i));
            if (nextUnit.canAssaultDrop()) {
                droppable.add(nextUnit);
            }
        }

        return droppable;
    }

    /***
     * get a vector of units that are unloadable on the ground
     */
    public Vector<Entity> getUnloadableUnits() {

        Vector<Entity> unloadable = new Vector<Entity>();

        // TODO: we need to handle aeros and VTOLs differently
        for (int i = 0; i < troops.size(); i++) {
            Entity nextUnit = game.getEntity(troops.elementAt(i));
            unloadable.add(nextUnit);
        }

        return unloadable;
    }

    /**
     * Unload the given unit.
     *
     * @param unit
     *            - the <code>Entity</code> to be unloaded.
     * @return <code>true</code> if the unit was contained in this space,
     *         <code>false</code> otherwise.
     */
    @Override
    public boolean unload(Entity unit) {

        // Remove the unit if we are carrying it.
        boolean retval = troops.removeElement(unit.getId());

        // If we removed it, restore our space.
        if (retval) {
            currentSpace += spaceForUnit(unit);
            unloadedThisTurn += 1;
        }

        // Return our status
        return retval;
    }

    /**
     * Return a string that identifies the unused capacity of this transporter.
     *
     * @return A <code>String</code> meant for a human.
     */
    public String getUnusedString(boolean showrecovery) {
        return numDoorsString() + "  - " + getUnused()
                + (getUnused() > 1 ? " units" : " unit");
    }
    
    protected String numDoorsString() {
        return "(" + getCurrentDoors()
            + ((getCurrentDoors() == 1)?" door":" doors") + ")";
    }

    @Override
    public String getUnusedString() {
        return getUnusedString(true);
    }

    /**
     * @return The amount of unused space in the bay.
     */
    @Override
    public double getUnused() {
        return currentSpace - damage;
    }
    
    /**
     * @return The amount of unused space in the bay expressed in slots. For most bays this is the
     *         same as the the unused space, but bays for units that can take up a variable amount
     *         of space (such as infantry bays) this calculates the number of the default unit size
     *         that can fit into the remaining space.
     */
    public double getUnusedSlots() {
        return currentSpace;
    }
    
    /**
     * @return A String that describes the default slot type. Only meaningful for bays with variable
     *         space requirements (like infantry).
     */
    public String getDefaultSlotDescription() {
        return "";
    }

    /**
     * Determine if transported units prevent a weapon in the given location
     * from firing.
     *
     * @param loc
     *            - the <code>int</code> location attempting to fire.
     * @param isRear
     *            - a <code>boolean</code> value stating if the given location
     *            is rear facing; if <code>false</code>, the location is front
     *            facing.
     * @return <code>true</code> if a transported unit is in the way,
     *         <code>false</code> if the weapon can fire.
     */
    @Override
    public boolean isWeaponBlockedAt(int loc, boolean isRear) {
        return false;
    }

    /**
     * If a unit is being transported on the outside of the transporter, it can
     * suffer damage when the transporter is hit by an attack. Currently, no
     * more than one unit can be at any single location; that same unit can be
     * "spread" over multiple locations.
     *
     * @param loc
     *            - the <code>int</code> location hit by attack.
     * @param isRear
     *            - a <code>boolean</code> value stating if the given location
     *            is rear facing; if <code>false</code>, the location is front
     *            facing.
     * @return The <code>Entity</code> being transported on the outside at that
     *         location. This value will be <code>null</code> if no unit is
     *         transported on the outside at that location.
     */
    @Override
    public Entity getExteriorUnitAt(int loc, boolean isRear) {
        return null;
    }

    @Override
    public final List<Entity> getExternalUnits() {
        ArrayList<Entity> rv = new ArrayList<Entity>(1);
        return rv;
    }

    @Override
    public int getCargoMpReduction() {
        return 0;
    }

    public String getType() {
        return "Unknown";
    }

    // destroy a door for next turn
    public void destroyDoorNext() {

        if (getDoorsNext() > 0) {
            setDoorsNext(getDoorsNext() - 1);
        }

    }

    // destroy a door
    public void destroyDoor() {
    	if (getCurrentDoors() > 0) 
    		setCurrentDoors(getCurrentDoors() - 1);
    }
    
    // restore a door
    public void restoreDoor() {
    	if (getCurrentDoors() < getDoors()) {
    		setCurrentDoors(getCurrentDoors() + 1);
    	}
    }
    
    // restore all doors
    public void restoreAllDoors() {
    	setCurrentDoors(getDoors());
    }
    
    public int getNumberUnloadedThisTurn() {
        return unloadedThisTurn;
    }

    public int getNumberLoadedThisTurn() {
        return unloadedThisTurn;
    }

    /** Return the tonnage of the bay, not the actual mass or weight */
    public double getWeight() {
        return totalSpace;
    }
    
    /**
     * @param clan  Whether the bay is installed in a Clan unit. Needed for infantry bays.
     * @return      The number of additional crew provided by the bay. This includes transport bays only;
     *              crew quarters are already accounted for in the crew total.
     */
    public int getPersonnel(boolean clan) {
        return 0;
    }

    @Override
    public String toString() {
        return "bay:" + totalSpace + ":" + doors + ":"+ bayNumber;
    }

    /**
     * @return The total size of the bay.
     */
    public double getCapacity() {
        return totalSpace;
    }
    
    public int getBayNumber() {
        return bayNumber;
    }

    @Override
    public void setGame(IGame game) {
        this.game = game;
    }
    
    // Use cargo/infantry for default tech advancement
    public static TechAdvancement techAdvancement() {
        return new TechAdvancement(TECH_BASE_ALL).setAdvancement(DATE_PS, DATE_PS, DATE_PS)
                .setTechRating(RATING_A)
                .setAvailability(RATING_A, RATING_A, RATING_A, RATING_A)
                .setStaticTechLevel(SimpleTechLevel.STANDARD);
    }
    
    public TechAdvancement getTechAdvancement() {
        return Bay.techAdvancement();
    }

    @Override
    public boolean isClan() {
        return getTechAdvancement().isClan();
    }

    @Override
    public boolean isMixedTech() {
        return getTechAdvancement().isMixedTech();
    }

    @Override
    public int getTechBase() {
        return getTechAdvancement().getTechBase();
    }

    @Override
    public int getIntroductionDate() {
        return getTechAdvancement().getIntroductionDate();
    }

    @Override
    public int getPrototypeDate() {
        return getTechAdvancement().getPrototypeDate();
    }

    @Override
    public int getProductionDate() {
        return getTechAdvancement().getProductionDate();
    }

    @Override
    public int getCommonDate() {
        return getTechAdvancement().getCommonDate();
    }

    @Override
    public int getExtinctionDate() {
        return getTechAdvancement().getExtinctionDate();
    }

    @Override
    public int getReintroductionDate() {
        return getTechAdvancement().getReintroductionDate();
    }

    @Override
    public int getTechRating() {
        return getTechAdvancement().getTechRating();
    }

    @Override
    public int getBaseAvailability(int era) {
        return getTechAdvancement().getBaseAvailability(era);
    }

    @Override
    public int getIntroductionDate(boolean clan, int faction) {
        return getTechAdvancement().getIntroductionDate(clan, faction);
    }

    @Override
    public int getPrototypeDate(boolean clan, int faction) {
        return getTechAdvancement().getPrototypeDate(clan, faction);
    }

    @Override
    public int getProductionDate(boolean clan, int faction) {
        return getTechAdvancement().getProductionDate(clan, faction);
    }

    @Override
    public int getExtinctionDate(boolean clan, int faction) {
        return getTechAdvancement().getExtinctionDate(clan, faction);
    }

    @Override
    public int getReintroductionDate(boolean clan, int faction) {
        return getTechAdvancement().getReintroductionDate(clan, faction);
    }

    @Override
    public SimpleTechLevel getStaticTechLevel() {
        return getTechAdvancement().getStaticTechLevel();
    }

    /**
     * @return true if this bay represents crew quarters or seating rather than a unit transport bay.
     */
    public boolean isQuarters() {
        return false;
    }
    
    /**
     * @return true if this bay represents cargo capacity rather than unit transport or crew quarters.
     */
    public boolean isCargo() {
        return false;
    }

} // End package class TroopSpace implements Transporter

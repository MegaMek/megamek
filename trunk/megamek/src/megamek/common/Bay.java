/*
 * MegaMek - Copyright (C) 2003, 2004 Ben Mazur (bmazur@sev.org)
 * 
 *  This program is free software; you can redistribute it and/or modify it 
 *  under the terms of the GNU General Public License as published by the Free 
 *  Software Foundation; either version 2 of the License, or (at your option) 
 *  any later version.
 * 
 *  This program is distributed in the hope that it will be useful, but 
 *  WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY 
 *  or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License 
 *  for more details.
 */

package megamek.common;

import java.util.ArrayList;
import java.util.List;
import java.util.Vector;

/**
 * Represtents a volume of space set aside for carrying ASFs and Small Craft aboard DropShips
 */

public class Bay implements Transporter {

    // Private attributes and helper functions.

    /**
     * 
     */
    private static final long serialVersionUID = -9056450317468016272L;
    int doors = 1;
    int doorsNext = 1;
    Vector<Integer> recoverySlots = new Vector<Integer>();
    
    /**
     * The troops being carried.
     */
    /* package */ Vector<Entity> troops = new Vector<Entity>();

    /**
     * The total amount of space available for troops.
     */
    /* package */ int totalSpace;

    /**
     * The current amount of space available for troops.
     */
    /* package */ int currentSpace;

    /* ### I don't think that I need this code ### *#/
    /**
     * Write our current state into the output stream.
     *
     * @param   out - the <code>ObjectOutputStream</code> we write to.
     * @exception - This method does not catch <code>IOException</code>s
     *          thrown during the write.
     *#/
    private void writeObject(java.io.ObjectOutputStream out)
        throws IOException {
        // Write our total and current space, followed by our troops.
        out.writeInt( this.totalSpace );
        out.writeInt( this.currentSpace );
        out.writeObject( this.troops );
    }

    /**
     * Read our state from the input stream.
     *
     * @param   out - the <code>ObjectInputStream</code> we read from.
     * @exception - This method does not catch <code>IOException</code>s
     *          or <code>ClassNotFoundException</code>s thrown during the read.
     *#/
    private void readObject(java.io.ObjectInputStream in)
        throws IOException, ClassNotFoundException {
        // Read our total and current space, followed by our troops.
        this.totalSpace = in.readInt();
        this.currentSpace = in.readInt();
        this.troops = (List)in.readObject();
    }
    /* ### I don't think that I need this code ### */

    // Protected constructors and methods.

    /**
     * The default constructor is only for serialization.
     */
    protected Bay() {
        this.totalSpace = 0;
        this.currentSpace = 0;
    }

    // Public constructors and methods.

    /**
     * Create a space for the given tonnage of troops. For this class, only
     * the weight of the troops (and their equipment) are considered; if
     * you'd like to think that they are stacked like lumber, be my guest.
     *
     * @param   space - The weight of troops (in tons) this space can carry.
     */
    public Bay( int space, int doors ) {
        this.totalSpace = space;
        this.currentSpace = space;
        this.doors = doors;
        this.doorsNext = doors;
    }

    public int getDoors() {
        return doors;
    }
    
    public void setDoors(int d) {
        this.doors = d;
        this.doorsNext = d;
    }

    //for setting doors after this launch
    public void setDoorsNext(int d) {
        this.doorsNext = d;
    }
    
    public int getDoorsNext() {
        return doorsNext;
    }

    public void resetDoors() {
        this.doors = this.doorsNext;
    }
    
    /**
     * Determines if this object can accept the given unit.  The unit may
     * not be of the appropriate type or there may be no room for the unit.
     *
     * @param   unit - the <code>Entity</code> to be loaded.
     * @return  <code>true</code> if the unit can be loaded,
     *          <code>false</code> otherwise.
     */
    public boolean canLoad( Entity unit ) {
        // Assume that we cannot carry the unit.
        boolean result = true;


        // We must have enough space for the new troops.
        if ( this.currentSpace < 1 ) {
            result = false;
        }
        
        //is there at least one door available
        if(this.doors < 1) {
            result = false;
        }
        
        // Return our result.
        return result;
    }

    /**
     * Load the given unit.  
     *
     * @param   unit - the <code>Entity</code> to be loaded.
     * @exception - If the unit can't be loaded, an
     *          <code>IllegalArgumentException</code> exception will be thrown.
     */
    public void load( Entity unit ) throws IllegalArgumentException {
        // If we can't load the unit, throw an exception.
        if ( !this.canLoad(unit) ) {
            throw new IllegalArgumentException( "Can not load " +
                        unit.getShortName() +
                        " into this bay. " + this.currentSpace);
        }

        this.currentSpace -= 1;
        
        // Add the unit to our list of troops.
        this.troops.addElement( unit );
    }

    /**
     * Get a <code>List</code> of the units currently loaded into this payload.
     *
     * @return  A <code>List</code> of loaded <code>Entity</code> units.
     *          This list will never be <code>null</code>, but it may be empty.
     *          The returned <code>List</code> is independant from the under-
     *          lying data structure; modifying one does not affect the other.
     */
    @SuppressWarnings("unchecked")
    public Vector<Entity> getLoadedUnits() {
        // Return a copy of our list of troops.
        return (Vector<Entity>)this.troops.clone();
    }
    
    /**
     * get a vector of launchable units. This is different from loaded in that
     * units in recovery cannot launch
     */
    public Vector<Entity> getLaunchableUnits() {
        
        Vector<Entity> launchable = new Vector<Entity>();
        
        for(int i = 0; i < this.troops.size(); i++) {
            Entity nextUnit = this.troops.elementAt(i);
            if(nextUnit.getRecoveryTurn() == 0) {
                launchable.add(nextUnit);
            }
        }
        
        return launchable;
    }

    /**
     * Unload the given unit.
     *
     * @param   unit - the <code>Entity</code> to be unloaded.
     * @return  <code>true</code> if the unit was contained in this space,
     *          <code>false</code> otherwise.
     */
    public boolean unload( Entity unit ) {
        
        //check to see if unloading possible
        //      is the door functional
        if( this.doors < 1 ) {
            return false;
        }
        
        // Remove the unit if we are carrying it.
        boolean retval = this.troops.removeElement( unit );

        // If we removed it, restore our space.
        if ( retval ) {
            this.currentSpace += 1;
        }

        // Return our status
        return retval;
    }

    /**
     * Return a string that identifies the unused capacity of this transporter.
     *
     * @return A <code>String</code> meant for a human.
     */
    public String getUnusedString() {
        return " - " + this.currentSpace + " units";
    }

    /**
     * Determine if transported units prevent a weapon in the given location
     * from firing.
     *
     * @param   loc - the <code>int</code> location attempting to fire.
     * @param   isRear - a <code>boolean</code> value stating if the given
     *          location is rear facing; if <code>false</code>, the location
     *          is front facing.
     * @return  <code>true</code> if a transported unit is in the way, 
     *          <code>false</code> if the weapon can fire.
     */
    public boolean isWeaponBlockedAt( int loc, boolean isRear ) {return false;}

    /**
     * If a unit is being transported on the outside of the transporter, it
     * can suffer damage when the transporter is hit by an attack.  Currently,
     * no more than one unit can be at any single location; that same unit
     * can be "spread" over multiple locations.
     *
     * @param   loc - the <code>int</code> location hit by attack.
     * @param   isRear - a <code>boolean</code> value stating if the given
     *          location is rear facing; if <code>false</code>, the location
     *          is front facing.
     * @return  The <code>Entity</code> being transported on the outside
     *          at that location.  This value will be <code>null</code>
     *          if no unit is transported on the outside at that location.
     */
    public Entity getExteriorUnitAt( int loc, boolean isRear ) { return null; }

    public final List<Entity> getExternalUnits() {
        ArrayList<Entity> rv = new ArrayList<Entity>(1);
        return rv;
    }

    public int getCargoMpReduction() {
        return 0;
    }
    
    public String getType() {
        return "Unknown";
    }
    
    //destroy a door for next turn
    public void destroyDoorNext() {
        
        setDoorsNext(getDoorsNext() - 1);
        
    }
    
    //  destroy a door
    public void destroyDoor() {
        
        setDoors(getDoors() - 1);
        
    }
    
} // End package class TroopSpace implements Transporter

/* MegaMek - Copyright (C) 2004 Ben Mazur (bmazur@sev.org)
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

import java.io.Serializable;
import java.util.Hashtable;
import java.util.Vector;
import java.util.Enumeration;

/**
 * TeleMissile Tracker  - holds a list of tele-missiles controled by this entity and
 * information on what particular weapon controls them
 */
public class TeleMissileTracker implements Serializable {

    /**
     * 
     */
    private static final long serialVersionUID = -6913144265531983734L;
    /**
     * map the entity id of the missile to the weapon that it came from
     */
    private Hashtable<Integer, Integer> missiles;

    /**
     * Creates new instance of the tracker
     */
    public TeleMissileTracker() {
        missiles = new Hashtable<Integer, Integer>();
    }

    public void addMissile(int wId, int missileId) {
        missiles.put(wId, missileId);
    }
    
    public void removeMissile(int wId) {
        missiles.remove(wId);
    }
    
    public int getMissile(int wId) {
        return missiles.get(wId);
    }
    
    public boolean containsLauncher(int wId) {
        return missiles.containsKey(wId);
    }
    
    public Vector<Integer> getMissiles() {
        //I could probably do this more directly with a Collection
        //but I don't know how to work with collections
        Vector<Integer> m = new Vector<Integer>();
        for (Enumeration<Integer> k = missiles.keys() ; k.hasMoreElements();) {
            int wId = k.nextElement();
            int missileId = missiles.get(wId);
            m.add(missileId);
        }
        return m;
    }
}
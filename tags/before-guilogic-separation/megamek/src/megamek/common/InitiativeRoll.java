/*
 * MegaMek - Copyright (C) 2000-2003 Ben Mazur (bmazur@sev.org)
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

/*
 * InitaitiveRoll.java
 *
 * Created on April 25, 2002, 12:21 PM
 */

package megamek.common;

import java.util.*;
import java.io.*;

/**
 * A roll, or sequence of rolls, made by the player to determine initiative 
 * order.  Also contains some methods for ordering players by initiative.
 *
 * @author  Ben
 * @version 
 */
public class InitiativeRoll implements com.sun.java.util.collections.Comparable, Serializable {
    
    private Vector rolls = new Vector();
    private Vector originalRolls = new Vector();
    private Vector wasRollReplaced = new Vector(); //booleans

    /** Creates new InitaitiveRoll */
    public InitiativeRoll() {
    }

    public void clear() {
       rolls.removeAllElements();
       originalRolls.removeAllElements();
       wasRollReplaced.removeAllElements();
    }

    public void addRoll() {
        Integer roll = new Integer(Compute.d6(2));
        rolls.addElement(roll);
        originalRolls.addElement(roll);
        wasRollReplaced.addElement(new Boolean(false));
    }

    // Replace the previous init roll with a new one, and make a note
    //  that it was replaced.  Used for Tactical Genius special
    //  pilot ability (lvl 3).
    public void replaceRoll() {
        Integer roll = new Integer(Compute.d6(2));
        rolls.setElementAt(roll, size() - 1);
        wasRollReplaced.setElementAt(new Boolean(true), size() - 1);
    }

    public int size() {
        return rolls.size();
    }
    
    public int getRoll(int index) {
        return ((Integer)rolls.elementAt(index)).intValue();
    }
    
    
    /**
     * Two initiative rolls are equal if they match, roll by roll
     */
    public boolean equals(Object object) {
        if (this == object) {
            return true;
        } else if (object == null || getClass() != object.getClass()) {
            return false;
        }
        InitiativeRoll other = (InitiativeRoll)object;
        if (size() != other.size()) {
            return false;
        }
        for (int i = 0; i < size(); i++) {
            if (getRoll(i) != other.getRoll(i)) {
                return false;
            }
        }
        return true;
    }
    
    public int compareTo(Object object) {
        if (getClass() != object.getClass()) {
            throw new IllegalArgumentException("InitiativeRoll must be compared to InitiativeRoll");
        }
        InitiativeRoll other = (InitiativeRoll)object;
        int minSize = Math.min(size(), other.size());
        int compare = 0;
        for (int i = 0; i < minSize; i++) {
            compare = getRoll(i) - other.getRoll(i);
            if (compare != 0) {
                return compare;
            }
        }
        return compare;
    }

    public String toString()
    {
        StringBuffer buff = new StringBuffer();

        boolean tacticalGenius = false;
        for (int i = 0; i < rolls.size(); i++) {
            Integer r = (Integer)rolls.elementAt(i);
            Integer o = (Integer)originalRolls.elementAt(i);

            if (((Boolean)wasRollReplaced.elementAt(i)).booleanValue()) {
                buff.append(o.toString()).append("(").append(r.toString()).append(")");
                tacticalGenius = true;
            } else {
                buff.append( r.toString() );
            }
            if (i != rolls.size() - 1) {
                buff.append( " / " );
            }
        }
        if (tacticalGenius) {
            buff.append("  (Tactical Genius ability used)");
        }
        return buff.toString();
    }

}

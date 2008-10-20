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

import java.io.Serializable;
import java.util.Vector;

/**
 * A roll, or sequence of rolls, made by the player to determine initiative
 * order. Also contains some methods for ordering players by initiative.
 * 
 * @author Ben
 * @version
 */
public class InitiativeRoll implements Comparable<InitiativeRoll>, Serializable {

    private static final long serialVersionUID = -1850190415242027657L;
    private Vector<Integer> rolls = new Vector<Integer>();
    private Vector<Integer> originalRolls = new Vector<Integer>();
    private Vector<Boolean> wasRollReplaced = new Vector<Boolean>(); // booleans
    private Vector<Integer> bonuses = new Vector<Integer>();
    
    /** Creates new InitaitiveRoll */
    public InitiativeRoll() {
    }

    public void clear() {
        rolls.removeAllElements();
        originalRolls.removeAllElements();
        bonuses.removeAllElements();
        wasRollReplaced.removeAllElements();
    }

    public void addRoll(int bonus) {
        Integer roll = new Integer(Compute.d6(2));
        rolls.addElement(roll);
        originalRolls.addElement(roll);
        bonuses.addElement(bonus);
        wasRollReplaced.addElement(new Boolean(false));
    }

    /**
     * Replace the previous init roll with a new one, and make a note that it
     * was replaced. Used for Tactical Genius special pilot ability (lvl 3).
     */
    public void replaceRoll(int bonus) {
        Integer roll = new Integer(Compute.d6(2));
        rolls.setElementAt(roll, size() - 1);
        bonuses.setElementAt(bonus, size()-1);
        wasRollReplaced.setElementAt(new Boolean(true), size() - 1);
    }

    public int size() {
        return rolls.size();
    }

    public int getRoll(int index) {
        return rolls.elementAt(index).intValue() + bonuses.elementAt(index).intValue();
    }

    /**
     * Two initiative rolls are equal if they match, roll by roll
     */
    public boolean equals(InitiativeRoll other) {
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

    public int compareTo(InitiativeRoll other) {
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

    public String toString() {
        StringBuffer buff = new StringBuffer();

        boolean tacticalGenius = false;
        for (int i = 0; i < rolls.size(); i++) {
            Integer r = rolls.elementAt(i);
            Integer o = originalRolls.elementAt(i);
            Integer b = bonuses.elementAt(i);
            Integer t = r+b;
            Integer to = o+b;
            
            if (wasRollReplaced.elementAt(i).booleanValue()) {
                buff.append(to.toString()).append("[").append(o.toString()).append("+").append(b.toString()).append("]").append("(").append(t.toString()).append("[").append(r.toString()).append("+").append(b.toString()).append("]")
                        .append(")");
                tacticalGenius = true;
            } else {
                buff.append(t.toString()).append("[").append(r.toString()).append("+").append(b.toString()).append("]");
            }
            if (i != rolls.size() - 1) {
                buff.append(" / ");
            }
        }
        if (tacticalGenius) {
            buff.append("  (Tactical Genius ability used)");
        }
        return buff.toString();
    }
}

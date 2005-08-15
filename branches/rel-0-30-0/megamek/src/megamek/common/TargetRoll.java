/*
 * MegaMek - Copyright (C) 2000-2002 Ben Mazur (bmazur@sev.org)
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
 * TargetRoll.java
 *
 * Created on April 19, 2002, 1:05 AM
 */

package megamek.common;

import java.util.Vector;
import java.util.Enumeration;
import java.io.Serializable;

/**
 * Keeps track of a target for a roll.  Allows adding modifiers with 
 * descriptions, including appending the modifiers in another TargetRoll.
 * Intended for rolls like a to-hit roll or a piloting skill check.
 *
 * @author  Ben
 * @version 
 */
public class TargetRoll implements Serializable {
    
    public final static int IMPOSSIBLE          = Integer.MAX_VALUE;
    public final static int AUTOMATIC_FAIL      = Integer.MAX_VALUE - 1;
    public final static int AUTOMATIC_SUCCESS   = Integer.MIN_VALUE;
    /* The CHECK_FALSE value is returned when a function that normally
       would return a target roll number determines that the roll
       wasn't needed after all. */
    public final static int CHECK_FALSE         = Integer.MIN_VALUE + 1;
    
    private Vector modifiers = new Vector();
    
    private int total;

    /** Creates new TargetRoll */
    public TargetRoll() {

    }
    
    /** 
     * Creates a new TargetRoll with a base value & desc
     */
    public TargetRoll(int value, String desc) {
        addModifier(value, desc);
    }
    
    /**
     * Returns the total value of all modifiers
     */
    public int getValue() {
        return total;
    }
    
    /**
     * Returns the total value of all modifiers
     */
    public String getValueAsString() {
        switch (total) {
            case IMPOSSIBLE : 
                return "Impossible";
            case AUTOMATIC_FAIL :
                return "Automatic Failure";
            case AUTOMATIC_SUCCESS :
                return "Automatic Success";
            case CHECK_FALSE :
                return "Did not need to roll";
            default :
                return Integer.toString(total);
        }
    }
    
    /**
     * Returns a description of all applicable modifiers
     */
    public String getDesc() {
        boolean first = true;
        StringBuffer allDesc = new StringBuffer();
        
        for (Enumeration e = getModifiers(); e.hasMoreElements();) {
            Modifier modifier = (Modifier)e.nextElement();
            
            // check for break condition
            if (modifier.value == IMPOSSIBLE
                || modifier.value == AUTOMATIC_FAIL 
                || modifier.value == AUTOMATIC_SUCCESS
                || modifier.value == CHECK_FALSE) {
                return modifier.desc;
            }
            
            // add desc
            if (first) {
                first = false;
            } else {
                allDesc.append((modifier.value < 0 ? " - " : " + "));
            }
            allDesc.append(Math.abs(modifier.value));
            allDesc.append(" (");
            allDesc.append(modifier.desc);
            allDesc.append(")");
        }
        
        return allDesc.toString();
    }
    
    /**
     * Returns the first description found
     */
    public String getPlainDesc() {
        for (Enumeration e = getModifiers(); e.hasMoreElements();) {
            return ((Modifier)e.nextElement()).desc;
        }
        return "";
    }
    
    /**
     * Returns the last description found
     */
    public String getLastPlainDesc() {
        Modifier last = (Modifier)modifiers.elementAt(modifiers.size() - 1);
        return last.desc;
    }
    
    public Enumeration getModifiers() {
        return modifiers.elements();
    }
    
    public void addModifier(int value, String desc) {
        addModifier(new Modifier(value, desc));
    }

    public void addModifier(Modifier modifier) {
        if (modifier.value == CHECK_FALSE) {
            removeAutos(true);
        }
        modifiers.addElement(modifier);
        recalculate();
    }
    
    /**
     * Append another TargetRoll to the end of this one
     */
    public void append(TargetRoll other) {
        if (other == null) {
            return;
        }
        for (Enumeration e = other.getModifiers(); e.hasMoreElements();) {
            addModifier((Modifier)e.nextElement());
        }
    }
    
    /**
     * Remove all automatic failures or successes, but leave impossibles intact
     */
    
    public void removeAutos() {
        removeAutos(false);
    }
    
    /**
     * Remove all automatic failures or successes, and possibly also remove
     * impossibles
     * @param removeImpossibles <code>boolean</code> value wether or not 
     *                          impossibles should be removed
     */
    
    public void removeAutos(boolean removeImpossibles) {
        Vector toKeep = new Vector();
        for (Enumeration e = getModifiers(); e.hasMoreElements();) {
            Modifier modifier = (Modifier)e.nextElement();
            if (!removeImpossibles) {
                if (modifier.value != AUTOMATIC_FAIL && modifier.value != AUTOMATIC_SUCCESS) {
                    toKeep.addElement(modifier);
                }
            } else if (modifier.value != AUTOMATIC_FAIL &&
                       modifier.value != AUTOMATIC_SUCCESS &&
                       modifier.value != IMPOSSIBLE) {
                toKeep.addElement(modifier);
            }
        }
        modifiers = toKeep;
        recalculate();
    }
    
    /**
     * Recalculate the target number & desc for all modifiers.  If any of them
     * indicates an automatic result, stop and just return that modifier.  Treat
     * the first modifier listed as a base
     */
    private void recalculate() {
        total = 0;
        
        for (Enumeration e = getModifiers(); e.hasMoreElements();) {
            Modifier modifier = (Modifier)e.nextElement();
            
            // check for break condition
            if (modifier.value == IMPOSSIBLE
                || modifier.value == AUTOMATIC_FAIL 
                || modifier.value == AUTOMATIC_SUCCESS
                || modifier.value == CHECK_FALSE) {
                total = modifier.value;
                break;
            }
            
            // add modifier
            total += modifier.value;
        }
    }
    
    private class Modifier implements Serializable {
        int value;
        String desc;
        
        public Modifier(int value, String desc) {
            this.value = value;
            this.desc = desc;
        }
    }
}

/**
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

import java.util.Enumeration;
import java.util.Vector;

/**
 * A handy utility class for collecting <code>Vectors</code> of
 * <code>TurnOrdered</code> markers and then walking through them.
 */
public class TurnVectors implements Enumeration<TurnOrdered> {
    private final int numEven;
    private final int numNormal;
    //need to keep an enumeration of all non-even turns
    private final int numTotal;
    private final int numSS;
    private final int numJS;
    private final int numWS;
    private final int numDS;
    private final int numSC;
    private final Vector<TurnOrdered> even_turns;
    private final Vector<TurnOrdered> normal_turns;
    private final Vector<TurnOrdered> total_turns;
    private final Vector<TurnOrdered> space_station_turns;
    private final Vector<TurnOrdered> jumpship_turns;
    private final Vector<TurnOrdered> warship_turns;
    private final Vector<TurnOrdered> dropship_turns;
    private final Vector<TurnOrdered> small_craft_turns;

    private Enumeration<TurnOrdered> turnNormalEnum = null;
    private Enumeration<TurnOrdered> evenEnum = null;
    private Enumeration<TurnOrdered> turnTotalEnum = null;
    private Enumeration<TurnOrdered> turnSSEnum = null;
    private Enumeration<TurnOrdered> turnJSEnum = null;
    private Enumeration<TurnOrdered> turnWSEnum = null;
    private Enumeration<TurnOrdered> turnDSEnum = null;
    private Enumeration<TurnOrdered> turnSCEnum = null;
    private final int min;

    /**
     * Helper function to access the <code>Enumeration</code> through our
     * recorded markers.
     */
    private synchronized Enumeration<TurnOrdered> getTurnNormalEnum() {
        if (null == turnNormalEnum) {
            // Only walk through "normal" turns.
            turnNormalEnum = normal_turns.elements();
        }
        return turnNormalEnum;
    }
    
    /**
     * Helper function to access the <code>Enumeration</code> through our
     * recorded markers.
     */
    private synchronized Enumeration<TurnOrdered> getTurnTotalEnum() {
        if (null == turnTotalEnum) {
            turnTotalEnum = total_turns.elements();
        }
        return turnTotalEnum;
    }
    
    /**
     * Helper function to access the <code>Enumeration</code> through our
     * recorded markers.
     */
    private synchronized Enumeration<TurnOrdered> getTurnSSEnum() {
        if (null == turnSSEnum) {
            // Only walk through "normal" turns.
            turnSSEnum = space_station_turns.elements();
        }
        return turnSSEnum;
    }
    
    /**
     * Helper function to access the <code>Enumeration</code> through our
     * recorded markers.
     */
    private synchronized Enumeration<TurnOrdered> getTurnJSEnum() {
        if (null == turnJSEnum) {
            // Only walk through "normal" turns.
            turnJSEnum = jumpship_turns.elements();
        }
        return turnJSEnum;
    }
    
    /**
     * Helper function to access the <code>Enumeration</code> through our
     * recorded markers.
     */
    private synchronized Enumeration<TurnOrdered> getTurnWSEnum() {
        if (null == turnWSEnum) {
            // Only walk through "normal" turns.
            turnWSEnum = warship_turns.elements();
        }
        return turnWSEnum;
    }
    
    /**
     * Helper function to access the <code>Enumeration</code> through our
     * recorded markers.
     */
    private synchronized Enumeration<TurnOrdered> getTurnDSEnum() {
        if (null == turnDSEnum) {
            turnDSEnum = dropship_turns.elements();
        }
        return turnDSEnum;
    }
    
    /**
     * Helper function to access the <code>Enumeration</code> through our
     * recorded markers.
     */
    private synchronized Enumeration<TurnOrdered> getTurnSCEnum() {
        if (null == turnSCEnum) {
            // Only walk through "normal" turns.
            turnSCEnum = small_craft_turns.elements();
        }
        return turnSCEnum;
    }

    /**
     * Helper function to access the <code>Enumeration</code> through our
     * recorded markers for "even" turns.
     */
    private synchronized Enumeration<TurnOrdered> getEvenEnum() {
        if (null == evenEnum) {
            evenEnum = even_turns.elements();
        }
        return evenEnum;
    }

    /**
     * Construct empty <code>Vectors</code> with the given capacities.
     * 
     * @param normalCount the <code>int</code> count of
     *            <code>TurnOrdered</code> object markers for normal turns
     * @param evenCount the <code>int</code> count of markers for turns that
     *            occur after all normal turns.
     * @param min the smallest <code>int</code> number of times one of the
     *            <code>TurnOrdered</code> objects appears in the normal list.
     */
    public TurnVectors(int normalCount, int totalCount, int  ssCount, int jsCount,
                       int wsCount, int dsCount, int scCount, int evenCount, int min) {
        this.numEven = evenCount;
        this.numNormal = normalCount;
        this.numTotal = totalCount;
        this.numSS = ssCount;
        this.numJS = jsCount;
        this.numWS = wsCount;
        this.numDS = dsCount;
        this.numSC = scCount;
        this.normal_turns = new Vector<TurnOrdered>(normalCount);
        this.total_turns = new Vector<TurnOrdered>(this.numTotal);
        this.even_turns = new Vector<TurnOrdered>(evenCount);
        this.space_station_turns = new Vector<TurnOrdered>(ssCount);
        this.jumpship_turns = new Vector<TurnOrdered>(jsCount);
        this.warship_turns = new Vector<TurnOrdered>(wsCount);
        this.dropship_turns = new Vector<TurnOrdered>(dsCount);
        this.small_craft_turns = new Vector<TurnOrdered>(scCount);
        this.min = min;
    }

    /**
     * Get the number of turns that must occur after all normal turns.
     * 
     * @return the <code>int</code> count of even turns.
     */
    public int getEvenTurns() {
        return even_turns.size();
    }

    /**
     * Get the number of normal turns.
     * 
     * @return the <code>int</code> count of normal turns.
     */
    public int getTotalTurns() {
        return total_turns.size();
    }
    
    public int getNormalTurns() {
        return normal_turns.size();
    }
    
    public int getSpaceStationTurns() {
        return space_station_turns.size();
    }
    
    public int getJumpshipTurns() {
        return jumpship_turns.size();
    }
    
    public int getWarshipTurns() {
        return warship_turns.size();
    }
    
    public int getDropshipTurns() {
        return dropship_turns.size();
    }
    
    public int getSmallCraftTurns() {
        return small_craft_turns.size();
    }

    /**
     * Add a <code>TurnOrdered</code> marker for a turn that must occur after
     * all normal turns.
     * 
     * @param marker the <code>TurnOrdered</code> marker for a even turn.
     * @exception IllegalStateException if as many markers have been added for
     *                even turns as indicated at construction.
     */
    public void addEven(TurnOrdered marker) {
        if (this.numEven == even_turns.size()) {
            throw new IllegalStateException("Have already added "
                    + this.numEven + " even turns.");
        }
        even_turns.addElement(marker);
    }

    /**
     * Add a <code>TurnOrdered</code> marker for a "normal" turn.
     * 
     * @param marker the <code>TurnOrdered</code> marker for a "normal" turn.
     * @exception IllegalStateException if as many markers have been added for
     *                normal turns as indicated at construction.
     */
    public void addNormal(TurnOrdered marker) {
        if (this.numNormal == normal_turns.size()) {
            throw new IllegalStateException("Have already added "
                    + this.numNormal + " normal turns.");
        }
        normal_turns.addElement(marker);
        total_turns.addElement(marker);
    }
    
    public void addSpaceStation(TurnOrdered marker) {
        if (this.numSS == space_station_turns.size()) {
            throw new IllegalStateException("Have already added "
                    + this.numSS + " space station turns.");
        }
        space_station_turns.addElement(marker);
        total_turns.addElement(marker);
    }
    
    public void addWarship(TurnOrdered marker) {
        if (this.numWS == warship_turns.size()) {
            throw new IllegalStateException("Have already added "
                    + this.numWS + " warship turns.");
        }
        warship_turns.addElement(marker);
        total_turns.addElement(marker);
    }
    
    public void addJumpship(TurnOrdered marker) {
        if (this.numJS == jumpship_turns.size()) {
            throw new IllegalStateException("Have already added "
                    + this.numJS + " jumpship turns.");
        }
        jumpship_turns.addElement(marker);
        total_turns.addElement(marker);
    }
    
    public void addDropship(TurnOrdered marker) {
        if (this.numDS == dropship_turns.size()) {
            throw new IllegalStateException("Have already added "
                    + this.numDS + " dropship turns.");
        }
        dropship_turns.addElement(marker);
        total_turns.addElement(marker);
    }

    public void addSmallCraft(TurnOrdered marker) {
        if (this.numSC == small_craft_turns.size()) {
            throw new IllegalStateException("Have already added "
                    + this.numSC + " small craft turns.");
        }
        small_craft_turns.addElement(marker);
        total_turns.addElement(marker);
    }
    
    /**
     * Determine if we've iterated to the end of our turn markers.
     * 
     * @return <code>true</code> if we've read all turn markers.
     */
    public boolean hasMoreNormalElements() {
        return this.getTurnNormalEnum().hasMoreElements();
    }

    /**
     * Get the next <code>TurnOrdered</code> marker.
     * 
     * @return the <code>TurnOrdered</code> marker; all "even" markers will be
     *         returned after all "normal" markers.
     */
    public TurnOrdered nextNormalElement() {
        return this.getTurnNormalEnum().nextElement();
    }

    /**
     * Determine if we've iterated to the end of our even turn markers.
     * 
     * @return <code>true</code> if we've read all turn markers.
     */
    public boolean hasMoreEvenElements() {
        return this.getEvenEnum().hasMoreElements();
    }
    
    /**
     * Get the next "total" <code>TurnOrdered</code> marker.
     * 
     * @return the "total" <code>TurnOrdered</code> marker.
     */
    public TurnOrdered nextElement() {
        return this.getTurnTotalEnum().nextElement();
    }
    
    /**
     * Determine if we've iterated to the end of our total turn markers.
     * 
     * @return <code>true</code> if we've read all turn markers.
     */
    public boolean hasMoreElements() {
        return this.getTurnTotalEnum().hasMoreElements();
    }

    /**
     * Get the next "space station" <code>TurnOrdered</code> marker.
     * 
     * @return the "space staion" <code>TurnOrdered</code> marker.
     */
    public TurnOrdered nextSpaceStationElement() {
        return this.getTurnSSEnum().nextElement();
    }
    
    /**
     * Determine if we've iterated to the end of our space station turn markers.
     * 
     * @return <code>true</code> if we've read all turn markers.
     */
    public boolean hasMoreSpaceStationElements() {
        return this.getTurnSSEnum().hasMoreElements();
    }
    
    /**
     * Get the next "jumpship" <code>TurnOrdered</code> marker.
     * 
     * @return the "jumpship" <code>TurnOrdered</code> marker.
     */
    public TurnOrdered nextJumpshipElement() {
        return this.getTurnJSEnum().nextElement();
    }
    
    /**
     * Determine if we've iterated to the end of our jumpship turn markers.
     * 
     * @return <code>true</code> if we've read all turn markers.
     */
    public boolean hasMoreJumpshipElements() {
        return this.getTurnJSEnum().hasMoreElements();
    }

    /**
     * Get the next "warship" <code>TurnOrdered</code> marker.
     * 
     * @return the "warship" <code>TurnOrdered</code> marker.
     */
    public TurnOrdered nextWarshipElement() {
        return this.getTurnWSEnum().nextElement();
    }
    
    /**
     * Determine if we've iterated to the end of our warship turn markers.
     * 
     * @return <code>true</code> if we've read all turn markers.
     */
    public boolean hasMoreWarshipElements() {
        return this.getTurnWSEnum().hasMoreElements();
    }
    
    /**
     * Get the next "dropship" <code>TurnOrdered</code> marker.
     * 
     * @return the "dropship" <code>TurnOrdered</code> marker.
     */
    public TurnOrdered nextDropshipElement() {
        return this.getTurnDSEnum().nextElement();
    }
    
    /**
     * Determine if we've iterated to the end of our dropship turn markers.
     * 
     * @return <code>true</code> if we've read all turn markers.
     */
    public boolean hasMoreDropshipElements() {
        return this.getTurnDSEnum().hasMoreElements();
    }
    
    /**
     * Get the next "small craft" <code>TurnOrdered</code> marker.
     * 
     * @return the "small craft" <code>TurnOrdered</code> marker.
     */
    public TurnOrdered nextSmallCraftElement() {
        return this.getTurnSCEnum().nextElement();
    }
    
    /**
     * Determine if we've iterated to the end of our small craft turn markers.
     * 
     * @return <code>true</code> if we've read all turn markers.
     */
    public boolean hasMoreSmallCraftElements() {
        return this.getTurnSCEnum().hasMoreElements();
    }
    
    /**
     * Get the next "even" <code>TurnOrdered</code> marker.
     * 
     * @return the "even" <code>TurnOrdered</code> marker.
     */
    public TurnOrdered nextEvenElement() {
        return this.getEvenEnum().nextElement();
    }

    /**
     * Return the smallest number of times one of the <code>TurnOrdered</code>
     * objects is present in the list of "normal" turns.
     * 
     * @return the <code>int</code> number of times.
     */
    public int getMin() {
        return this.min;
    }
    

}

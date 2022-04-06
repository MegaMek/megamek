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
public class TurnVectors implements Enumeration<ITurnOrdered> {
    private final int numEven;
    private final int numNormal;
    //need to keep an enumeration of all non-even turns
    private final int numTotal;
    private final int numSS;
    private final int numJS;
    private final int numWS;
    private final int numDS;
    private final int numSC;
    private final int numTM;
    private final int numAero;
    private final Vector<ITurnOrdered> even_turns;
    private final Vector<ITurnOrdered> normal_turns;
    private final Vector<ITurnOrdered> total_turns;
    private final Vector<ITurnOrdered> space_station_turns;
    private final Vector<ITurnOrdered> jumpship_turns;
    private final Vector<ITurnOrdered> warship_turns;
    private final Vector<ITurnOrdered> dropship_turns;
    private final Vector<ITurnOrdered> small_craft_turns;
    private final Vector<ITurnOrdered> telemissile_turns;
    private final Vector<ITurnOrdered> aero_turns;

    private Enumeration<ITurnOrdered> turnNormalEnum = null;
    private Enumeration<ITurnOrdered> evenEnum = null;
    private Enumeration<ITurnOrdered> turnTotalEnum = null;
    private Enumeration<ITurnOrdered> turnSSEnum = null;
    private Enumeration<ITurnOrdered> turnJSEnum = null;
    private Enumeration<ITurnOrdered> turnWSEnum = null;
    private Enumeration<ITurnOrdered> turnDSEnum = null;
    private Enumeration<ITurnOrdered> turnSCEnum = null;
    private Enumeration<ITurnOrdered> turnTelemissileEnum = null;
    private Enumeration<ITurnOrdered> turnAeroEnum = null;
    private final int min;

    /**
     * Helper function to access the <code>Enumeration</code> through our
     * recorded markers.
     */
    private synchronized Enumeration<ITurnOrdered> getTurnNormalEnum() {
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
    private synchronized Enumeration<ITurnOrdered> getTurnTotalEnum() {
        if (null == turnTotalEnum) {
            turnTotalEnum = total_turns.elements();
        }
        return turnTotalEnum;
    }
    
    /**
     * Helper function to access the <code>Enumeration</code> through our
     * recorded markers.
     */
    private synchronized Enumeration<ITurnOrdered> getTurnSSEnum() {
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
    private synchronized Enumeration<ITurnOrdered> getTurnJSEnum() {
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
    private synchronized Enumeration<ITurnOrdered> getTurnWSEnum() {
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
    private synchronized Enumeration<ITurnOrdered> getTurnDSEnum() {
        if (null == turnDSEnum) {
            turnDSEnum = dropship_turns.elements();
        }
        return turnDSEnum;
    }
    
    /**
     * Helper function to access the <code>Enumeration</code> through our
     * recorded markers.
     */
    private synchronized Enumeration<ITurnOrdered> getTurnSCEnum() {
        if (null == turnSCEnum) {
            // Only walk through "normal" turns.
            turnSCEnum = small_craft_turns.elements();
        }
        return turnSCEnum;
    }
    
    /**
     * Helper function to access the <code>Enumeration</code> through our
     * recorded markers.
     */
    private synchronized Enumeration<ITurnOrdered> getTurnTelemissileEnum() {
        if (null == turnTelemissileEnum) {
            // Only walk through "normal" turns.
            turnTelemissileEnum = telemissile_turns.elements();
        }
        return turnTelemissileEnum;
    }
    
    /**
     * Helper function to access the <code>Enumeration</code> through our
     * recorded markers.
     */
    private synchronized Enumeration<ITurnOrdered> getTurnAeroEnum() {
        if (null == turnAeroEnum) {
            // Only walk through "normal" turns.
            turnAeroEnum = aero_turns.elements();
        }
        return turnAeroEnum;
    }

    /**
     * Helper function to access the <code>Enumeration</code> through our
     * recorded markers for "even" turns.
     */
    private synchronized Enumeration<ITurnOrdered> getEvenEnum() {
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
                       int wsCount, int dsCount, int scCount, int tmCount, int aeroCount, int evenCount, int min) {
        this.numEven = evenCount;
        this.numNormal = normalCount;
        this.numTotal = totalCount;
        this.numSS = ssCount;
        this.numJS = jsCount;
        this.numWS = wsCount;
        this.numDS = dsCount;
        this.numSC = scCount;
        this.numTM = tmCount;
        this.numAero = aeroCount;
        this.normal_turns = new Vector<>(normalCount);
        this.total_turns = new Vector<>(this.numTotal);
        this.even_turns = new Vector<>(evenCount);
        this.space_station_turns = new Vector<>(ssCount);
        this.jumpship_turns = new Vector<>(jsCount);
        this.warship_turns = new Vector<>(wsCount);
        this.dropship_turns = new Vector<>(dsCount);
        this.small_craft_turns = new Vector<>(scCount);
        this.telemissile_turns = new Vector<>(tmCount);
        this.aero_turns = new Vector<>(aeroCount);
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
    
    public int getTelemissileTurns() {
        return telemissile_turns.size();
    }
    
    public int getAeroTurns() {
        return aero_turns.size();
    }

    /**
     * Add a <code>TurnOrdered</code> marker for a turn that must occur after
     * all normal turns.
     * 
     * @param marker the <code>TurnOrdered</code> marker for a even turn.
     * @throws IllegalStateException if as many markers have been added for
     *                even turns as indicated at construction.
     */
    public void addEven(ITurnOrdered marker) {
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
     * @throws IllegalStateException if as many markers have been added for
     *                normal turns as indicated at construction.
     */
    public void addNormal(ITurnOrdered marker) {
        if (this.numNormal == normal_turns.size()) {
            throw new IllegalStateException("Have already added "
                    + this.numNormal + " normal turns.");
        }
        normal_turns.addElement(marker);
        total_turns.addElement(marker);
    }
    
    public void addSpaceStation(ITurnOrdered marker) {
        if (this.numSS == space_station_turns.size()) {
            throw new IllegalStateException("Have already added "
                    + this.numSS + " space station turns.");
        }
        space_station_turns.addElement(marker);
        total_turns.addElement(marker);
    }
    
    public void addWarship(ITurnOrdered marker) {
        if (this.numWS == warship_turns.size()) {
            throw new IllegalStateException("Have already added "
                    + this.numWS + " warship turns.");
        }
        warship_turns.addElement(marker);
        total_turns.addElement(marker);
    }
    
    public void addJumpship(ITurnOrdered marker) {
        if (this.numJS == jumpship_turns.size()) {
            throw new IllegalStateException("Have already added "
                    + this.numJS + " jumpship turns.");
        }
        jumpship_turns.addElement(marker);
        total_turns.addElement(marker);
    }
    
    public void addDropship(ITurnOrdered marker) {
        if (this.numDS == dropship_turns.size()) {
            throw new IllegalStateException("Have already added "
                    + this.numDS + " dropship turns.");
        }
        dropship_turns.addElement(marker);
        total_turns.addElement(marker);
    }

    public void addSmallCraft(ITurnOrdered marker) {
        if (this.numSC == small_craft_turns.size()) {
            throw new IllegalStateException("Have already added "
                    + this.numSC + " small craft turns.");
        }
        small_craft_turns.addElement(marker);
        total_turns.addElement(marker);
    }
    
    public void addTelemissile(ITurnOrdered marker) {
        if (this.numTM == telemissile_turns.size()) {
            throw new IllegalStateException("Have already added "
                    + this.numTM + " telemissile turns.");
        }
        telemissile_turns.addElement(marker);
        total_turns.addElement(marker);
    }
    
    public void addAero(ITurnOrdered marker) {
        if (this.numAero == aero_turns.size()) {
            throw new IllegalStateException("Have already added "
                    + this.numAero + " aero turns.");
        }
        aero_turns.addElement(marker);
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
    public ITurnOrdered nextNormalElement() {
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
    @Override
    public ITurnOrdered nextElement() {
        return this.getTurnTotalEnum().nextElement();
    }
    
    /**
     * Determine if we've iterated to the end of our total turn markers.
     * 
     * @return <code>true</code> if we've read all turn markers.
     */
    @Override
    public boolean hasMoreElements() {
        return this.getTurnTotalEnum().hasMoreElements();
    }

    /**
     * Get the next "space station" <code>TurnOrdered</code> marker.
     * 
     * @return the "space staion" <code>TurnOrdered</code> marker.
     */
    public ITurnOrdered nextSpaceStationElement() {
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
    public ITurnOrdered nextJumpshipElement() {
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
    public ITurnOrdered nextWarshipElement() {
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
    public ITurnOrdered nextDropshipElement() {
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
    public ITurnOrdered nextSmallCraftElement() {
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
     * Get the next "telemissile" <code>TurnOrdered</code> marker.
     * 
     * @return the "telemissile" <code>TurnOrdered</code> marker.
     */
    public ITurnOrdered nextTelemissileElement() {
        return this.getTurnTelemissileEnum().nextElement();
    }
    
    /**
     * Determine if we've iterated to the end of our telemissile turn markers.
     * 
     * @return <code>true</code> if we've read all turn markers.
     */
    public boolean hasMoreTelemissileElements() {
        return this.getTurnTelemissileEnum().hasMoreElements();
    }
    
    /**
     * Get the next "aero" <code>TurnOrdered</code> marker.
     * 
     * @return the "aero" <code>TurnOrdered</code> marker.
     */
    public ITurnOrdered nextAeroElement() {
        return this.getTurnAeroEnum().nextElement();
    }
    
    /**
     * Determine if we've iterated to the end of our aero turn markers.
     * 
     * @return <code>true</code> if we've read all turn markers.
     */
    public boolean hasMoreAeroElements() {
        return this.getTurnAeroEnum().hasMoreElements();
    }
    
    /**
     * Get the next "even" <code>TurnOrdered</code> marker.
     * 
     * @return the "even" <code>TurnOrdered</code> marker.
     */
    public ITurnOrdered nextEvenElement() {
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

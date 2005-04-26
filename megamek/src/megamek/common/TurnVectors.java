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

import com.sun.java.util.collections.Vector;

/**
 * A handy utility class for collecting <code>Vectors</code> of
 * <code>TurnOrdered</code> markers and then walking through them.
 */
public class TurnVectors implements Enumeration {
    private final int numEven;
    private final int numNormal;
    private final Vector even_turns;
    private final Vector normal_turns;

    private Enumeration turnEnum = null;
    private Enumeration evenEnum = null;
    private final int min;

    /**
     * Helper function to access the <code>Enumeration</code> through
     * our recorded markers.
     */
    private synchronized Enumeration getTurnEnum() {
        if ( null == turnEnum ) {
            // Only walk through "normal" turns.
            turnEnum = normal_turns.elements();
        }
        return turnEnum;
    }

    /**
     * Helper function to access the <code>Enumeration</code> through
     * our recorded markers for "even" turns.
     */
    private synchronized Enumeration getEvenEnum() {
        if ( null == evenEnum ) {
            evenEnum = even_turns.elements();
        }
        return evenEnum;
    }

    /**
     * Construct empty <code>Vectors</code> with the given capacities.
     *
     * @param   normalCount the <code>int</code> count of 
     *          <code>TurnOrdered</code> object markers for normal turns
     * @param   evenCount the <code>int</code> count of markers
     *          for turns that occur after all normal turns.
     * @param   order the array of <code>TurnOrdered</code> objects, sorted
     *          in order of appearance.
     * @param   min the smallest <code>int</code> number of times one of the
     *          <code>TurnOrdered</code> objects appears in the normal list.
     */    
    public TurnVectors( int normalCount, int evenCount, int min )
    {
        this.numEven = evenCount;
        this.numNormal = normalCount;
    this.normal_turns = new Vector( normalCount );
    this.even_turns  = new Vector( evenCount );
        this.min = min;
    }

    /**
     * Get the number of turns that must occur after all normal turns.
     *
     * @return  the <code>int</code> count of even turns.
     */
    public int getEvenTurns() {
        return even_turns.size();
    }

    /**
     * Get the number of normal turns.
     *
     * @return  the <code>int</code> count of normal turns.
     */
    public int getNormalTurns() {
        return normal_turns.size();
    }

    /**
     * Add a <code>TurnOrdered</code> marker for a turn that must occur
     * after all normal turns.
     *
     * @param   marker the <code>TurnOrdered</code> marker for a even turn.
     * @exception <code>IllegalStateException</code> if as many markers
     *          have been added for even turns as indicated at construction.
     */
    public void addEven( TurnOrdered marker ) {
        if ( this.numEven == even_turns.size() ) {
            throw new IllegalStateException
                ( "Have already added " + this.numEven + " even turns." );
        }
        even_turns.addElement( marker );
    }

    /**
     * Add a <code>TurnOrdered</code> marker for a "normal" turn.
     *
     * @param   marker the <code>TurnOrdered</code> marker for a "normal" turn.
     * @exception <code>IllegalStateException</code> if as many markers
     *          have been added for normal turns as indicated at construction.
     */
    public void addNormal( TurnOrdered marker ) {
        if ( this.numNormal == normal_turns.size() ) {
            throw new IllegalStateException
                ( "Have already added " + this.numNormal + " normal turns." );
        }
        normal_turns.addElement( marker );
    }

    /**
     * Determine if we've iterated to the end of our turn markers.
     *
     * @return  <code>true</code> if we've read all turn markers.
     */
    public boolean hasMoreElements() {
        return this.getTurnEnum().hasMoreElements();
    }

    /**
     * Get the next <code>TurnOrdered</code> marker.
     *
     * @return  the <code>TurnOrdered</code> marker; all "even" markers
     *          will be returned after all "normal" markers.
     */
    public Object nextElement() {
        return this.getTurnEnum().nextElement();
    }

    /**
     * Determine if we've iterated to the end of our even turn markers.
     *
     * @return  <code>true</code> if we've read all turn markers.
     */
    public boolean hasMoreEvenElements() {
        return this.getEvenEnum().hasMoreElements();
    }

    /**
     * Get the next "even" <code>TurnOrdered</code> marker.
     *
     * @return  the "even" <code>TurnOrdered</code> marker.
     */
    public Object nextEvenElement() {
        return this.getEvenEnum().nextElement();
    }

    /**
     * Return the smallest number of times one of the <code>TurnOrdered</code>
     * objects is present in the list of "normal" turns.
     *
     * @param   the <code>int</code> number of times.
     */
    public int getMin() {
        return this.min;
    }

}

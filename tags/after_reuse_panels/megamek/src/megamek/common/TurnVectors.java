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

import java.io.Serializable;
import java.util.Enumeration;
import java.util.Vector;
import megamek.common.util.SequenceEnumeration;

/**
 * A handy utility class for collecting <code>Vectors</code> of
 * <code>TurnOrdered</code> markers and then walking through them.
 */
public class TurnVectors implements Enumeration {
    private final int numLast;
    private final int numNormal;
    private final Vector last_turns;
    private final Vector normal_turns;

    private Enumeration turnEnum = null;

    /**
     * Helper function to access the <code>Enumeration</code> through
     * our recorded markers.
     */
    private synchronized Enumeration getTurnEnum() {
        if ( null == turnEnum ) {
            turnEnum = new SequenceEnumeration( normal_turns.elements(),
                                                last_turns.elements() );
        }
        return turnEnum;
    }

    /**
     * Construct empty <code>Vectors</code> with the given capacities.
     *
     * @param   normalCount the <code>int</code> count of 
     *          <code>TurnOrdered</code> object markers for normal turns
     * @param   lastCount the <code>int</code> count of markers
     *          for turns that occur after all normal turns.
     */    
    public TurnVectors( int normalCount, int lastCount )
    {
        this.numLast = lastCount;
        this.numNormal = normalCount;
	this.normal_turns = new Vector( normalCount );
	this.last_turns  = new Vector( lastCount );
    }

    /**
     * Get the number of turns that must occur after all normal turns.
     *
     * @return  the <code>int</code> count of last turns.
     */
    public int getLastTurns() {
        return last_turns.size();
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
     * @param   marker the <code>TurnOrdered</code> marker for a last turn.
     * @exception <code>IllegalStateException</code> if as many markers
     *          have been added for last turns as indicated at construction.
     */
    public void addLast( TurnOrdered marker ) {
        if ( this.numLast == last_turns.size() ) {
            throw new IllegalStateException
                ( "Have already added " + this.numLast + " last turns." );
        }
        last_turns.addElement( marker );
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
     * @return  the <code>TurnOrdered</code> marker; all "last" markers
     *          will be returned after all "normal" markers.
     */
    public Object nextElement() {
        return this.getTurnEnum().nextElement();
    }

}

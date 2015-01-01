/*
 * MegaMek - Copyright (C) 2004 Ben Mazur (bmazur@sev.org)
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

package megamek.common.util;

import java.util.Enumeration;

/**
 * This class is an <code>Enumeration</code> of <code>Enumeration</code>s.
 * That is, it takes multiple <code>Enumeration</code>s, and walks through
 * the elements of each in turn; when all the elements of one are exhaused,
 * it starts iterating through the elements of the next, similar to the
 * behavior of a <code>SequenceInputStream</code>.
 *
 * Created on January 17, 2004
 *
 * @author  James Damour
 * @version 1
 */
public final class SequenceEnumeration implements Enumeration {

    /**
     * The items to be iterated through.
     */
    private final Enumeration[] items;

    /**
     * The element currently being iterated through.
     */
    private int curItem = 0;

    /**
     * Walk through all elements in the first <code>Enumeration</code>,
     * followed by all elements in the second.  The parameters must not be
     * modified after being passed to this constructor.
     *
     * @param   first - the first <code>Enumeration</code> to walk through.
     *          This value must not be <code>null</code>.
     * @param   second - the second <code>Enumeration</code> to walk through.
     *          This value must not be <code>null</code>.
     * @throws  <code>IllegalArgumentException</code> if any argument is null.
     */
    public SequenceEnumeration( Enumeration first, Enumeration second ) {
        // Validate the input.
        if ( null == first ) {
            throw new IllegalArgumentException
                ( "The first enumeration is null.");
        }
        if ( null == second ) {
            throw new IllegalArgumentException
                ( "The second enumeration is null.");
        }

        // We have an array of two items.
        items = new Enumeration[2];
        items[0] = first;
        items[1] = second;
    }

    /**
     * Walk through all elements of the <code>Enumeration</code>s in the
     * order they occur in the given array.  The parameters must not be
     * modified after being passed to this constructor.
     *
     * @param   array - the array of <code>Enumeration</code>s.  Neither the
     *          array nor its elements may be <code>null</code>.  
     * @throws  <code>IllegalArgumentException</code> if any argument is null.
     */
    public SequenceEnumeration( Enumeration[] array ) {
        // Validate the input.
        if ( null == array ) {
            throw new IllegalArgumentException( "A null array was passed." );
        }
        for ( int loop = 0; loop < array.length; loop++ ) {
            if ( null == array[loop] ) {
                throw new IllegalArgumentException
                    ( "Array element #" + loop + " is null." );
            }
        }

        // We will walk through this array.
        items = array;
    }

    /**
     * Determine if there are any more elements to be returned.
     *
     * @return  <code>true</code> if there are more elements.
     *          <code>false</code> if all elements have been returned.
     */
    public boolean hasMoreElements() {
        // Walk through the array of items looking for more elements.
        for ( ; curItem < items.length; curItem++ ) {
            if ( items[curItem].hasMoreElements() ) {
                return true;
            }
        }

        // We didn't find any more elements.
        return false;
    }

    /**
     * Return the next element.
     *
     * @return  the next <code>Object</code> in the sequence.  If there
     *          are no more elements, this will be <code>null</code>.
     */
    public Object nextElement() {
        // Walk through the array of items looking for more elements.
        for ( ; curItem < items.length; curItem++ ) {
            if ( items[curItem].hasMoreElements() ) {
                return items[curItem].nextElement();
            }
        }

        // We didn't find any more elements.
        return null;
    }

}

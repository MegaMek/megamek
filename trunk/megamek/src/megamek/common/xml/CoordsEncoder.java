/*
 * MegaMek - Copyright (C) 2003 Ben Mazur (bmazur@sev.org)
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

package megamek.common.xml;

import java.io.Writer;
import java.io.IOException;
import java.util.Enumeration;
import gd.xml.tiny.ParsedXML;
import megamek.common.*;

/**
 * Objects of this class can encode a <code>Coords</code> object as XML
 * into an output writer and decode one from a parsed XML node.  It is used
 * when saving games into a version- neutral format.
 *
 * @author      James Damour <suvarov454@users.sourceforge.net>
 */
public class CoordsEncoder {

    /**
     * Encode a <code>Coords</code> object to an output writer.
     *
     * @param   coords - the <code>Coords</code> to be encoded.
     *          This value must not be <code>null</code>.
     * @param   out - the <code>Writer</code> that will receive the XML.
     *          This value must not be <code>null</code>.
     * @throws  <code>IllegalArgumentException</code> if the node is
     *          <code>null</code>.
     * @throws  <code>IOException</code> if there's any error on write.
     */
    public static void encode( Coords coords, Writer out )
        throws IOException
    {
        // First, validate our input.
        if ( null == coords ) {
            throw new IllegalArgumentException( "The coords is null." );
        }
        if ( null == out ) {
            throw new IllegalArgumentException( "The writer is null." );
        }

        // Encode the coords object to the stream.
        out.write( "<coords version=\"1.0\" hash=\"" );
        out.write( coords.hashCode() );
        out.write( "\" />" );
    }

    /**
     * Decode a <code>Coords</code> object from the passed node.
     *
     * @param   node - the <code>ParsedXML</code> node for this object.
     *          This value must not be <code>null</code>.
     * @param   game - the <code>Game</code> the decoded object belongs to.
     * @return  the <code>Coords</code> object based on the node.
     * @throws  <code>IllegalArgumentException</code> if the node is
     *          <code>null</code>.
     * @throws  <code>IllegalStateException</code> if the node does not
     *          contain a valid <code>Coords</code>.
     */
    public static Coords decode( ParsedXML node, Game game ) {

        if ( 1 == 0 ) {
            // Try to pull the hash code from the node.
            int hash = 0;

            Coords coords = Coords.getFromHashCode( hash );
        }
        return null;
    }

}


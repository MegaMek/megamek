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
 * Objects of this class can encode a <code>Building</code> object as XML
 * into an output writer and decode one from a parsed XML node.  It is used
 * when saving games into a version- neutral format.
 *
 * @author      James Damour <suvarov454@users.sourceforge.net>
 */
public class BuildingEncoder {

    /**
     * Encode a <code>Building</code> object to an output writer.
     *
     * @param   bldg - the <code>Building</code> to be encoded.
     *          This value must not be <code>null</code>.
     * @param   out - the <code>Writer</code> that will receive the XML.
     *          This value must not be <code>null</code>.
     * @throws  <code>IllegalArgumentException</code> if the node is
     *          <code>null</code>.
     * @throws  <code>IOException</code> if there's any error on write.
     */
    public static void encode( Building bldg, Writer out )
        throws IOException
    {
        Enumeration iter; // used when marching through a list of sub-elements
        Coords coords;

        // First, validate our input.
        if ( null == bldg ) {
            throw new IllegalArgumentException( "The board is null." );
        }
        if ( null == out ) {
            throw new IllegalArgumentException( "The writer is null." );
        }

        // Start the XML stream for this building
        out.write( "<building version=\"1.0\" >" );

        // Write the hex array to the stream.
        out.write( "<buildingData id=\"" );
        out.write( bldg.getId() );
        out.write( "\" type=\"" );
        out.write( bldg.getType() );
        out.write( "\" currentCF=\"" );
        out.write( bldg.getCurrentCF() );
        out.write( "\" phaseCF=\"" );
        out.write( bldg.getPhaseCF() );
        out.write( "\" name=\"" );
        out.write( bldg.getName() );
        out.write( "\" isBurning=\"" );
        out.write( bldg.isBurning() ? "true" : "false" );
        out.write( "\" >" );

        // Write the coordinate of the building.
        iter = bldg.getCoords();
        if ( iter.hasMoreElements() ) {
            while ( iter.hasMoreElements() ) {
                // Encode the infernos as these coordinates.
                coords = (Coords) iter.nextElement();
                CoordsEncoder.encode( coords, out );
            }
        }
        out.write( "</buildingData>" );

        // Finish the XML stream for this building.
        out.write( "</building>" );
    }

    /**
     * Decode a <code>Building</code> object from the passed node.
     *
     * @param   node - the <code>ParsedXML</code> node for this object.
     *          This value must not be <code>null</code>.
     * @param   game - the <code>Game</code> the decoded object belongs to.
     * @return  the <code>Building</code> object based on the node.
     * @throws  <code>IllegalArgumentException</code> if the node is
     *          <code>null</code>.
     * @throws  <code>IllegalStateException</code> if the node does not
     *          contain a valid <code>Building</code>.
     */
    public static Building decode( ParsedXML node, Game game ) {
        return null;
    }

}


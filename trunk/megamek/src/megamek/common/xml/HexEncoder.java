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
 * Objects of this class can encode a <code>Hex</code> object as XML
 * into an output writer and decode one from a parsed XML node.  It is used
 * when saving games into a version- neutral format.
 *
 * @author      James Damour <suvarov454@users.sourceforge.net>
 */
public class HexEncoder {

    /**
     * Encode a <code>Hex</code> object to an output writer.
     *
     * @param   hex - the <code>Hex</code> to be encoded.
     *          This value must not be <code>null</code>.
     * @param   out - the <code>Writer</code> that will receive the XML.
     *          This value must not be <code>null</code>.
     * @throws  <code>IllegalArgumentException</code> if the node is
     *          <code>null</code>.
     * @throws  <code>IOException</code> if there's any error on write.
     */
    public static void encode( Hex hex, Writer out )
        throws IOException
    {
        Terrain terrain = null;
        int loop = 0;

        // First, validate our input.
        if ( null == hex ) {
            throw new IllegalArgumentException( "The hex is null." );
        }
        if ( null == out ) {
            throw new IllegalArgumentException( "The writer is null." );
        }

        // Start the XML stream for this hex
        out.write( "<hex version=\"1.0\" >" );

        // OK, so elevation and theme aren't *strictly speaking* part of
        // terrain, but it's convenient to store this info here.  Cope.
        out.write( "<terrains count=\"" );
        out.write( Integer.toString(hex.terrainsPresent()) );
        out.write( "\" elevation=\"" );
        out.write( Integer.toString(hex.getElevation()) );
        out.write( "\" theme=\"" );
        out.write( hex.getTheme() );
        out.write( "\" >" );
        for ( loop = 0; loop < Terrain.SIZE; loop++ ) {
            // If the hex has this kind of terrain, encode it.
            if ( hex.contains(loop) ) {
                terrain = hex.getTerrain(loop);
                out.write( "<terrain type=\"" );
                out.write( Integer.toString(terrain.getType()) );
                out.write( "\" level=\"" );
                out.write( Integer.toString(terrain.getLevel()) );
                out.write( "\" exits=\"" );
                out.write( Integer.toString(terrain.getExits()) );
                out.write( "\" exitsSpecified=\"" );
                out.write( terrain.hasExitsSpecified() ? "true" : "false" );
                out.write( "\" />" );
            }
        }
        out.write( "</terrains>" );
        out.write( "</hex>" );
    }

    /**
     * Decode a <code>Hex</code> object from the passed node.
     *
     * @param   node - the <code>ParsedXML</code> node for this object.
     *          This value must not be <code>null</code>.
     * @param   game - the <code>Game</code> the decoded object belongs to.
     * @return  the <code>Hex</code> object based on the node.
     * @throws  <code>IllegalArgumentException</code> if the node is
     *          <code>null</code>.
     * @throws  <code>IllegalStateException</code> if the node does not
     *          contain a valid <code>Hex</code>.
     */
    public static Hex decode( ParsedXML node, Game game ) {
        return null;
    }

}


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
 * Objects of this class can encode a <code>Board</code> object as XML
 * into an output writer and decode one from a parsed XML node.  It is used
 * when saving games into a version- neutral format.
 *
 * @author      James Damour <suvarov454@users.sourceforge.net>
 */
public class BoardEncoder {

    /**
     * Encode a <code>Board</code> object to an output writer.
     *
     * @param   board - the <code>Board</code> to be encoded.
     *          This value must not be <code>null</code>.
     * @param   out - the <code>Writer</code> that will receive the XML.
     *          This value must not be <code>null</code>.
     * @throws  <code>IllegalArgumentException</code> if the node is
     *          <code>null</code>.
     * @throws  <code>IOException</code> if there's any error on write.
     */
    public static void encode( Board board, Writer out )
        throws IOException
    {
        Enumeration iter; // used when marching through a list of sub-elements
        Coords coords;
        int x;
        int y;
        int turns;

        // First, validate our input.
        if ( null == board ) {
            throw new IllegalArgumentException( "The board is null." );
        }
        if ( null == out ) {
            throw new IllegalArgumentException( "The writer is null." );
        }

        // Start the XML stream for this board
        out.write( "<board version=\"1.0\" >" );

        // Write the hex array to the stream.
        out.write( "<boardData width=\"" );
        out.write( board.width );
        out.write( "\" height=\"" );
        out.write( board.height );
        out.write( "\" roadsAutoExit=\"" );
        out.write( board.getRoadsAutoExit() ? "true" : "false" );
        out.write( "\" >" );
        for ( x = 0; x < board.width; x++ ) {
            for ( y = 0; y < board.height; y++ ) {
                HexEncoder.encode( board.getHex(x,y), out );
            }
        }
        out.write( "</boardData>" );

        // Write out the buildings (if any).
        iter = board.getBuildings();
        if ( iter.hasMoreElements() ) {
            out.write( "<buildings>" );
            while ( iter.hasMoreElements() ) {
                BuildingEncoder.encode( (Building) iter.nextElement(), out );
            }
            out.write( "</buildings>" );
        }

        // Write out the infernos (if any).
        iter = board.getInfernoBurningCoords();
        if ( iter.hasMoreElements() ) {
            out.write( "<infernos>" );
            while ( iter.hasMoreElements() ) {
                // Encode the infernos as these coordinates.
                coords = (Coords) iter.nextElement();
                out.write( "<inferno>" );
                CoordsEncoder.encode( coords, out );
                turns = board.getInfernoIVBurnTurns( coords );
                // This value may be zero.
                if ( turns > 0 ) {
                    out.write( "<arrowiv turns=\"" );
                    out.write( turns );
                    out.write( "\" />" );
                }
                // -(Arrow IV turns - All Turns) = Standard Turns.
                turns -= board.getInfernoBurnTurns( coords );
                turns = -turns;
                if ( turns > 0 ) {
                    out.write( "<standard turns=\"" );
                    out.write( turns );
                    out.write( "\" />" );
                }
                out.write( "</inferno>" );
            }
            out.write( "</infernos>" );
        }

        // Finish the XML stream for this board.
        out.write( "</board>" );
    }

    /**
     * Decode a <code>Board</code> object from the passed node.
     *
     * @param   node - the <code>ParsedXML</code> node for this object.
     *          This value must not be <code>null</code>.
     * @param   game - the <code>Game</code> the decoded object belongs to.
     * @return  the <code>Board</code> object based on the node.
     * @throws  <code>IllegalArgumentException</code> if the node is
     *          <code>null</code>.
     * @throws  <code>IllegalStateException</code> if the node does not
     *          contain a valid <code>Board</code>.
     */
    public static Board decode( ParsedXML node, Game game ) {
        return null;
    }

}


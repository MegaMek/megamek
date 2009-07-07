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

import gd.xml.tiny.ParsedXML;

import java.io.IOException;
import java.io.Writer;

import megamek.common.IGame;
import megamek.common.Minefield;

/**
 * Objects of this class can encode a <code>Minefield</code> object as XML
 * into an output writer and decode one from a parsed XML node. It is used when
 * saving games into a version- neutral format.
 * 
 * @author James Damour <suvarov454@users.sourceforge.net>
 */
public class MinefieldEncoder {

    /**
     * Encode a <code>Minefield</code> object to an output writer.
     * 
     * @param minefield - the <code>Minefield</code> to be encoded. This value
     *            must not be <code>null</code>.
     * @param out - the <code>Writer</code> that will receive the XML. This
     *            value must not be <code>null</code>.
     * @throws <code>IllegalArgumentException</code> if the node is
     *             <code>null</code>.
     * @throws <code>IOException</code> if there's any error on write.
     */
    public static void encode(Minefield minefield, Writer out)
            throws IOException {
        // First, validate our input.
        if (null == minefield) {
            throw new IllegalArgumentException("The minefield is null.");
        }
        if (null == out) {
            throw new IllegalArgumentException("The writer is null.");
        }

        // Start the XML stream for this minefield.
        out.write("<minefield version=\"1.0\">");

        // Encode the game's data.
        out.write("<minefieldData type=\"");
        out.write(minefield.getType());
        out.write("\" playerId=\"");
        out.write(minefield.getPlayerId());
        out.write("\" setting=\"");
        out.write(minefield.getSetting());
        out.write("\" damage=\"");
        out.write(minefield.getDensity());
        out.write("\" >");
        CoordsEncoder.encode(minefield.getCoords(), out);
        out.write("</minefieldData>");

        // Finish the XML stream for this game.
        out.write("</game>");
    }

    /**
     * Decode a <code>Minefield</code> object from the passed node.
     * 
     * @param node - the <code>ParsedXML</code> node for this object. This
     *            value must not be <code>null</code>.
     * @param game - the <code>IGame</code> the decoded object belongs to.
     * @return the <code>Minefield</code> object based on the node.
     * @throws <code>IllegalArgumentException</code> if the node is
     *             <code>null</code>.
     * @throws <code>IllegalStateException</code> if the node does not contain
     *             a valid <code>Minefield</code>.
     */
    public static Minefield decode(ParsedXML node, IGame game) {
        return null;
    }

}

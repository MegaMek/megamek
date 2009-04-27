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

import megamek.common.Coords;
import megamek.common.IGame;

/**
 * Objects of this class can encode a <code>Coords</code> object as XML into
 * an output writer and decode one from a parsed XML node. It is used when
 * saving games into a version- neutral format.
 * 
 * @author James Damour <suvarov454@users.sourceforge.net>
 */
public class CoordsEncoder {

    /**
     * Encode a <code>Coords</code> object to an output writer.
     * 
     * @param coords - the <code>Coords</code> to be encoded. This value must
     *            not be <code>null</code>.
     * @param out - the <code>Writer</code> that will receive the XML. This
     *            value must not be <code>null</code>.
     * @throws <code>IllegalArgumentException</code> if the node is
     *             <code>null</code>.
     * @throws <code>IOException</code> if there's any error on write.
     */
    public static void encode(Coords coords, Writer out) throws IOException {
        // First, validate our input.
        if (null == coords) {
            throw new IllegalArgumentException("The coords is null.");
        }
        if (null == out) {
            throw new IllegalArgumentException("The writer is null.");
        }

        // Encode the coords object to the stream.
        out.write("<coords version=\"1.0\" hash=\"");
        out.write(Integer.toString(coords.hashCode()));
        out.write("\" />");
    }

    /**
     * Decode a <code>Coords</code> object from the passed node.
     * 
     * @param node - the <code>ParsedXML</code> node for this object. This
     *            value must not be <code>null</code>.
     * @param game - the <code>IGame</code> the decoded object belongs to.
     * @return the <code>Coords</code> object based on the node.
     * @throws <code>IllegalArgumentException</code> if the node is
     *             <code>null</code>.
     * @throws <code>IllegalStateException</code> if the node does not contain
     *             a valid <code>Coords</code>.
     */
    public static Coords decode(ParsedXML node, IGame game) {

        // Did we get a null node?
        if (null == node) {
            throw new IllegalArgumentException("The coords is null.");
        }

        // Make sure that the node is for a Coords object.
        if (!node.getName().equals("coords")) {
            throw new IllegalStateException("Not passed a coords node.");
        }

        // TODO : perform version checking.

        // Get the hash code.
        String hashStr = node.getAttribute("hash");
        if (null == hashStr) {
            throw new IllegalStateException("Couldn't decode the coords node.");
        }
        // Try to pull the hash code from the string
        int hash = 0;
        try {
            hash = Integer.parseInt(hashStr);
        } catch (NumberFormatException exp) {
            throw new IllegalStateException("Couldn't find coords for "
                    + hashStr);
        }

        // Return the coords for this hash code.
        return Coords.getFromHashCode(hash);
    }

}

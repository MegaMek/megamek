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
import java.util.Enumeration;

import megamek.common.Hex;
import megamek.common.IGame;
import megamek.common.IHex;
import megamek.common.ITerrain;
import megamek.common.ITerrainFactory;
import megamek.common.Terrains;
import megamek.common.util.StringUtil;

/**
 * Objects of this class can encode a <code>Hex</code> object as XML into an
 * output writer and decode one from a parsed XML node. It is used when saving
 * games into a version- neutral format.
 * 
 * @author James Damour <suvarov454@users.sourceforge.net>
 */
public class HexEncoder {

    /**
     * Encode a <code>Hex</code> object to an output writer.
     * 
     * @param hex - the <code>Hex</code> to be encoded. This value must not be
     *            <code>null</code>.
     * @param out - the <code>Writer</code> that will receive the XML. This
     *            value must not be <code>null</code>.
     * @throws <code>IllegalArgumentException</code> if the node is
     *             <code>null</code>.
     * @throws <code>IOException</code> if there's any error on write.
     */
    public static void encode(IHex hex, Writer out) throws IOException {
        ITerrain terrain = null;
        int loop = 0;

        // First, validate our input.
        if (null == hex) {
            throw new IllegalArgumentException("The hex is null.");
        }
        if (null == out) {
            throw new IllegalArgumentException("The writer is null.");
        }

        // Start the XML stream for this hex
        out.write("<hex version=\"1.0\" >");

        // OK, so elevation and theme aren't *strictly speaking* part of
        // terrain, but it's convenient to store this info here. Cope.
        out.write("<terrains count=\"");
        out.write(Integer.toString(hex.terrainsPresent()));
        out.write("\" elevation=\"");
        out.write(Integer.toString(hex.getElevation()));
        if (null != hex.getTheme()) {
            out.write("\" theme=\"");
            out.write(hex.getTheme());
        }
        out.write("\" >");
        for (loop = 0; loop < Terrains.SIZE; loop++) {
            // If the hex has this kind of terrain, encode it.
            if (hex.containsTerrain(loop)) {
                terrain = hex.getTerrain(loop);
                out.write("<terrain type=\"");
                out.write(Integer.toString(terrain.getType()));
                out.write("\" level=\"");
                out.write(Integer.toString(terrain.getLevel()));
                out.write("\" exits=\"");
                out.write(Integer.toString(terrain.getExits()));
                out.write("\" exitsSpecified=\"");
                out.write(terrain.hasExitsSpecified() ? "true" : "false");
                out.write("\" />");
            }
        }
        out.write("</terrains>");
        out.write("</hex>");
    }

    /**
     * Decode a <code>Hex</code> object from the passed node.
     * 
     * @param node - the <code>ParsedXML</code> node for this object. This
     *            value must not be <code>null</code>.
     * @param game - the <code>IGame</code> the decoded object belongs to.
     * @return the <code>Hex</code> object based on the node.
     * @throws <code>IllegalArgumentException</code> if the node is
     *             <code>null</code>.
     * @throws <code>IllegalStateException</code> if the node does not contain
     *             a valid <code>Hex</code>.
     */
    public static IHex decode(ParsedXML node, IGame game) {
        String attrStr = null;
        int attrVal = 0;
        IHex retVal = null;
        ITerrainFactory f = Terrains.getTerrainFactory();

        // Did we get a null node?
        if (null == node) {
            throw new IllegalArgumentException("The hex is null.");
        }

        // Make sure that the node is for a Hex object.
        if (!node.getName().equals("hex")) {
            throw new IllegalStateException("Not passed a hex node.");
        }

        // TODO : perform version checking.

        // Find the terrains node.
        Enumeration<?> children = node.elements();
        while (children.hasMoreElements()) {
            ParsedXML child = (ParsedXML) children.nextElement();
            if (child.getName().equals("terrains")) {

                // There should be only one terrains node.
                if (null != retVal) {
                    throw new IllegalStateException(
                            "More than one 'terrains' node in a hex node.");
                }

                // Create an array to hold all the terrains.
                ITerrain[] terrains = new ITerrain[Terrains.SIZE];

                // Walk through the subnodes, parsing out terrain nodes.
                Enumeration<?> subnodes = child.elements();
                while (subnodes.hasMoreElements()) {

                    // Is this a "terrain" node?
                    ParsedXML subnode = (ParsedXML) subnodes.nextElement();
                    if (subnode.getName().equals("terrain")) {

                        // Try to parse the terrain node.
                        try {
                            final int type = Integer.parseInt(subnode
                                    .getAttribute("type"));
                            final boolean exitsSpecified = StringUtil
                                    .parseBoolean(subnode
                                            .getAttribute("exitsSpecified"));
                            final int level = Integer.parseInt(subnode
                                    .getAttribute("level"));
                            final int exits = Integer.parseInt(subnode
                                    .getAttribute("exits"));
                            terrains[type] = f.createTerrain(type, level,
                                    exitsSpecified, exits);
                        } catch (Throwable thrown) {
                            throw new IllegalStateException(
                                    "Couldn't parse a terrain from a hex node.");
                        }

                    } // End found-"terrain"-node

                } // Look at the next subnode.

                // Get the elevation of the hex.
                attrStr = child.getAttribute("elevation");
                if (null == attrStr) {
                    throw new IllegalStateException(
                            "Couldn't decode the terrains for a hex node.");
                }

                // Try to pull the elevation from the attribute string
                try {
                    attrVal = Integer.parseInt(attrStr);
                } catch (NumberFormatException exp) {
                    throw new IllegalStateException(
                            "Couldn't get an integer from " + attrStr);
                }

                // Get the theme of the hex (if any).
                attrStr = child.getAttribute("theme");

                // Construct the hex.
                retVal = new Hex(attrVal, terrains, attrStr);

            } // End found-"terrains"-node

        } // Look at the next child.

        // Return the hex for this node.
        return retVal;
    }

}

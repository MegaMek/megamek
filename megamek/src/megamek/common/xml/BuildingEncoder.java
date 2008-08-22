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
import java.util.HashMap;
import java.util.Map;
import java.util.Vector;

import megamek.common.Building;
import megamek.common.Coords;
import megamek.common.IGame;
import megamek.common.util.StringUtil;

/**
 * Objects of this class can encode a <code>Building</code> object as XML into
 * an output writer and decode one from a parsed XML node. It is used when
 * saving games into a version- neutral format.
 * 
 * @author James Damour <suvarov454@users.sourceforge.net>
 */
public class BuildingEncoder {

    /**
     * Encode a <code>Building</code> object to an output writer.
     * 
     * @param bldg - the <code>Building</code> to be encoded. This value must
     *            not be <code>null</code>.
     * @param out - the <code>Writer</code> that will receive the XML. This
     *            value must not be <code>null</code>.
     * @throws <code>IllegalArgumentException</code> if the node is
     *             <code>null</code>.
     * @throws <code>IOException</code> if there's any error on write.
     */
    public static void encode(Building bldg, Writer out) throws IOException {
        Enumeration<?> iter; // used when marching through a list of sub-elements
        Coords coords;

        // First, validate our input.
        if (null == bldg) {
            throw new IllegalArgumentException("The board is null.");
        }
        if (null == out) {
            throw new IllegalArgumentException("The writer is null.");
        }

        // Start the XML stream for this building
        out.write("<building version=\"1.0\" >");

        // Write the hex array to the stream.
        out.write("<buildingData id=\"");
        out.write(Integer.toString(bldg.getId()));
        out.write("\" type=\"");
        out.write(Integer.toString(bldg.getType()));
        out.write("\" name=\"");
        out.write(bldg.getName());
        out.write("\" >");

        // Write the coordinate of the building.
        iter = bldg.getCoords();
        if (iter.hasMoreElements()) {
            while (iter.hasMoreElements()) {
                // Encode the infernos as these coordinates.
                coords = (Coords) iter.nextElement();
                CoordsEncoder.encode(coords, out);
                out.write("\" currentCF=\"");
                out.write(Integer.toString(bldg.getCurrentCF(coords)));
                out.write("\" phaseCF=\"");
                out.write(Integer.toString(bldg.getPhaseCF(coords)));
                out.write("\" isBurning=\"");
                out.write(bldg.isBurning(coords) ? "true" : "false");
            }
        }
        out.write("</buildingData>");

        // Finish the XML stream for this building.
        out.write("</building>");
    }

    /**
     * Decode a <code>Building</code> object from the passed node.
     * 
     * @param node - the <code>ParsedXML</code> node for this object. This
     *            value must not be <code>null</code>.
     * @param game - the <code>IGame</code> the decoded object belongs to.
     * @return the <code>Building</code> object based on the node.
     * @throws <code>IllegalArgumentException</code> if the node is
     *             <code>null</code>.
     * @throws <code>IllegalStateException</code> if the node does not contain
     *             a valid <code>Building</code>.
     */
    public static Building decode(ParsedXML node, IGame game) {
        String attrStr = null;
        int attrVal = 0;
        Building retVal = null;
        Vector<Coords> coordVec = new Vector<Coords>();
        Map<Coords, Boolean> burning = new HashMap<Coords, Boolean>();
        Map<Coords, Integer> curCF = new HashMap<Coords, Integer>();
        Map<Coords, Integer> phaseCF = new HashMap<Coords, Integer>();
        Enumeration<?> subnodes = null;
        ParsedXML subnode = null;
        Coords coords = null;
        int type = -1;
        int id = -1;
        String name = null;

        // Walk the building node's children.
        Enumeration<?> children = node.elements();
        while (children.hasMoreElements()) {
            ParsedXML child = (ParsedXML) children.nextElement();
            String childName = child.getName();

            // Handle null child names.
            if (null == childName) {

                // No-op.
            }

            // Did we find the buildingData node?
            else if (childName.equals("buildingData")) {

                // There should be only one buildingData node.
                if (null != retVal) {
                    throw new IllegalStateException(
                            "More than one 'buildingData' node in a building node.");
                }

                // Read the coords of the building.
                subnodes = child.elements();
                while (subnodes.hasMoreElements()) {
                    subnode = (ParsedXML) subnodes.nextElement();

                    // Have we found the coords subnode?
                    if (subnode.getName().equals("coords")) {
                        coords = CoordsEncoder.decode(subnode, game);
                        if (null != coords) {
                            coordVec.addElement(coords);
                        }
                        // Do we have a 'currentCF' attribute?
                        attrStr = subnode.getAttribute("currentCF");
                        if (null != attrStr) {

                            // Try to pull the currentCF from the attribute string
                            try {
                                attrVal = Integer.parseInt(attrStr);
                            } catch (NumberFormatException nfexp) {
                                throw new IllegalStateException(
                                        "Couldn't get an integer from " + attrStr);
                            }

                            // Do we have a valid value?
                            if (0 > attrVal) {
                                throw new IllegalStateException(
                                        "Illegal value for currentCF: " + attrStr);
                            }
                            curCF.put(coords, attrVal);
                        }

                        // Do we have a 'phaseCF' attribute?
                        attrStr = subnode.getAttribute("phaseCF");
                        if (null != attrStr) {

                            // Try to pull the phaseCF from the attribute string
                            try {
                                attrVal = Integer.parseInt(attrStr);
                            } catch (NumberFormatException nfexp) {
                                throw new IllegalStateException(
                                        "Couldn't get an integer from " + attrStr);
                            }

                            // Do we have a valid value?
                            if (0 >= attrVal) {
                                throw new IllegalStateException(
                                        "Illegal value for phaseCF: " + attrStr);
                            }
                            phaseCF.put(coords, attrVal);
                        }
                        // Set the building's 'isBurning' flag.
                        burning.put(coords,StringUtil.parseBoolean(child
                                .getAttribute("isBurning")));
                    }

                } // Check the next subnode

                // We *did* find at least one coords for the building, right?
                if (0 >= coordVec.size()) {
                    throw new IllegalStateException(
                            "Couldn't decode the coords for the building.");
                }

                // Get the building type.
                attrStr = child.getAttribute("type");
                if (null == attrStr) {
                    throw new IllegalStateException(
                            "Couldn't decode the buildingData for a building node.");
                }

                // Try to pull the type from the attribute string
                try {
                    attrVal = Integer.parseInt(attrStr);
                } catch (NumberFormatException exp) {
                    throw new IllegalStateException(
                            "Couldn't get an integer from " + attrStr);
                }
                type = attrVal;

                // Do we have a valid value?
                if (type < 0 || type > Building.WALL) {
                    throw new IllegalStateException("Illegal value for type: "
                            + attrStr);
                }

                // Get the building id.
                attrStr = child.getAttribute("id");
                if (null == attrStr) {
                    throw new IllegalStateException(
                            "Couldn't decode the buildingData for a building node.");
                }

                // Try to pull the id from the attribute string
                try {
                    attrVal = Integer.parseInt(attrStr);
                } catch (NumberFormatException nfexp) {
                    throw new IllegalStateException(
                            "Couldn't get an integer from " + attrStr);
                }
                id = attrVal;

                // Do we have a valid value?
                if (0 >= id) {
                    throw new IllegalStateException("Illegal value for id: "
                            + attrStr);
                }

                // Get the building name.
                attrStr = child.getAttribute("name");
                if (null == attrStr) {
                    throw new IllegalStateException(
                            "Couldn't decode the buildingData for a building node.");
                }
                name = attrStr;

                // Try to create the building.
                try {
                    retVal = new Building(type, id, name, coordVec);
                } catch (IllegalArgumentException iaexp) {
                    throw new IllegalStateException(iaexp.getMessage());
                }
                for (Coords coord : burning.keySet()) {
                    retVal.setBurning(burning.get(coord), coord);
                }
                for (Coords coord : curCF.keySet()) {
                    retVal.setCurrentCF(curCF.get(coord), coord);
                }
                for (Coords coord : phaseCF.keySet()) {
                    retVal.setPhaseCF(phaseCF.get(coord), coord);
                }

            } // End found-"buildingData"-child

        } // Look at the next child.

        return retVal;
    }

}

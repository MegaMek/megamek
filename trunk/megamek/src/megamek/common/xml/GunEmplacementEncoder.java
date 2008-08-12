/*
 * MegaMek - Copyright (C) 2003 Ben Mazur (bmazur@sev.org)
 *           Copyright (C) 2005 Mike Gratton <mike@vee.net>
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

import megamek.common.Entity;
import megamek.common.GunEmplacement;
import megamek.common.IGame;

/**
 * Objects of this class can encode a <code>Entity</code> object as XML into
 * an output writer and decode one from a parsed XML node. It is used when
 * saving games into a version- neutral format.
 * 
 * @author Mike Gratton <mike@vee.net>
 */
public class GunEmplacementEncoder {

    /**
     * Encode a <code>Entity</code> object to an output writer.
     * 
     * @param entity - the <code>Entity</code> to be encoded. This value must
     *            not be <code>null</code>.
     * @param out - the <code>Writer</code> that will receive the XML. This
     *            value must not be <code>null</code>.
     * @throws <code>IOException</code> if there's any error on write.
     */
    public static void encode(Entity entity, Writer out) throws IOException {
        GunEmplacement ge = (GunEmplacement) entity;
        // save this so that the GE has the correct building type when decoded
        out.write("<cf>");
        out.write(Integer.toString(ge.getConstructionFactor()));
        out.write("<cf/>");

        if (ge.hasTurret() && ge.isTurretLocked()) {
            out.write("<turretLocked facing=\"");
            out.write(Integer.toString(ge.getSecondaryFacing()));
            out.write("\"/>");
        }
    }

    /**
     * Decode a <code>Entity</code> object from the passed node.
     * 
     * @param node - the <code>ParsedXML</code> node for this object. This
     *            value must not be <code>null</code>.
     * @param game - the <code>IGame</code> the decoded object belongs to.
     * @return the <code>Entity</code> object based on the node.
     * @throws <code>IllegalStateException</code> if the node does not contain
     *             a valid <code>Entity</code>.
     */
    public static Entity decode(ParsedXML node, IGame game) {
        if (!node.getName().equals("class")
                || !"GunEmplacement".equals(node.getAttribute("name"))) {
            throw new IllegalStateException(
                    "Not passed an GunEmplacement node.");
        }

        GunEmplacement ge = new GunEmplacement();

        // TODO : perform version checking.

        Enumeration<?> children = node.elements();
        while (children.hasMoreElements()) {
            ParsedXML child = (ParsedXML) children.nextElement();
            String childName = child.getName();

            if (childName.equals("cf")) {
                String cf = child.getContent().trim();
                try {
                    ge.setConstructionFactor(Integer.parseInt(cf));
                } catch (NumberFormatException nfe) {
                    throw new IllegalStateException(
                            "Invalid integer value for cf element: " + cf);
                }
            } else if (childName.equals("turretLocked")) {
                String facing = child.getAttribute("facing");
                ge.setTurretLocked(true);
                try {
                    ge.setSecondaryFacing(Integer.parseInt(facing));
                } catch (NumberFormatException nfe) {
                    throw new IllegalStateException(
                            "Invalid integer value for facing attribute: "
                                    + facing);
                }
            }
        }

        return ge;
    }

}

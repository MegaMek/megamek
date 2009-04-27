/*
 * MegaMek - Copyright (C) 2003, 2004 Ben Mazur (bmazur@sev.org)
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
import megamek.common.IGame;
import megamek.common.Infantry;

/**
 * Objects of this class can encode a <code>Entity</code> object as XML into
 * an output writer and decode one from a parsed XML node. It is used when
 * saving games into a version- neutral format.
 * 
 * @author James Damour <suvarov454@users.sourceforge.net>
 */
public class InfantryEncoder {

    /**
     * Encode a <code>Entity</code> object to an output writer.
     * 
     * @param entity - the <code>Entity</code> to be encoded. This value must
     *            not be <code>null</code>.
     * @param out - the <code>Writer</code> that will receive the XML. This
     *            value must not be <code>null</code>.
     * @throws <code>IllegalArgumentException</code> if the entity is
     *             <code>null</code>.
     * @throws <code>IOException</code> if there's any error on write.
     */
    public static void encode(Entity entity, Writer out) throws IOException {
        Infantry inf = (Infantry) entity;
        int value;

        // First, validate our input.
        if (null == entity) {
            throw new IllegalArgumentException("The entity is null.");
        }
        if (null == out) {
            throw new IllegalArgumentException("The writer is null.");
        }

        // Our EntityEncoder already gave us our root element.
        out.write("<shootingStrength value=\"");
        value = inf.getShootingStrength();
        out.write(String.valueOf(value));
        out.write("\" />");
    }

    /**
     * Decode a <code>Entity</code> object from the passed node.
     * 
     * @param node - the <code>ParsedXML</code> node for this object. This
     *            value must not be <code>null</code>.
     * @param game - the <code>IGame</code> the decoded object belongs to.
     * @return the <code>Entity</code> object based on the node.
     * @throws <code>IllegalArgumentException</code> if the node is
     *             <code>null</code>.
     * @throws <code>IllegalStateException</code> if the node does not contain
     *             a valid <code>Entity</code>.
     */
    public static Entity decode(ParsedXML node, IGame game) {
        Infantry entity = null;
        String attrStr;
        int attrVal;

        // Did we get a null node?
        if (null == node) {
            throw new IllegalArgumentException("The Infantry node is null.");
        }

        // Make sure that the node is for an Infantry unit.
        attrStr = node.getAttribute("name");
        if (!node.getName().equals("class") || null == attrStr
                || !attrStr.equals("Infantry")) {
            throw new IllegalStateException("Not passed an Infantry node.");
        }

        // TODO : perform version checking.

        // Create the entity.
        entity = new Infantry();

        // Walk the board node's children.
        Enumeration<?> children = node.elements();
        while (children.hasMoreElements()) {
            ParsedXML child = (ParsedXML) children.nextElement();
            String childName = child.getName();

            // Handle null child names.
            if (null == childName) {

                // No-op.
            }

            // Did we find the shootingStrength node?
            else if (childName.equals("shootingStrength")) {

                // Get the number of men shooting.
                attrStr = child.getAttribute("value");
                if (null == attrStr) {
                    throw new IllegalStateException(
                            "Couldn't decode the shootingStrength for an Infantry unit.");
                }

                // Try to pull the number from the attribute string
                try {
                    attrVal = Integer.parseInt(attrStr);
                } catch (NumberFormatException exp) {
                    throw new IllegalStateException(
                            "Couldn't get an integer from " + attrStr);
                }

                // Shooting strength is set... oddly.
                entity.setInternal(Infantry.LOC_INFANTRY, attrVal);
                entity.applyDamage();
            }

        } // Handle the next element.

        // Return the entity.
        return entity;
    }

}

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

import megamek.common.Entity;
import megamek.common.IGame;
import megamek.common.Tank;

/**
 * Objects of this class can encode a <code>Entity</code> object as XML into
 * an output writer and decode one from a parsed XML node. It is used when
 * saving games into a version- neutral format.
 * 
 * @author James Damour <suvarov454@users.sourceforge.net>
 */
public class TankEncoder {

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
        Tank tank = (Tank) entity;
        int value;

        // First, validate our input.
        if (null == entity) {
            throw new IllegalArgumentException("The entity is null.");
        }
        if (null == out) {
            throw new IllegalArgumentException("The writer is null.");
        }

        // Our EntityEncoder already gave us our root element.
        out.write("<hasNoTurret value=\"");
        out.write(tank.hasNoTurret() ? "true" : "false");
        out.write("\" /><stunnedTurns value=\"");
        value = tank.getStunnedTurns();
        out.write(String.valueOf(value));
        out.write("\" /><moveHit value=\"");
        out.write(tank.isMovementHit() ? "true" : "false");
        out.write("\" /><moveHitPending value=\"");
        out.write(tank.isMovementHitPending() ? "true" : "false");

        // Is the turret locked?
        if (tank.isTurretLocked()) {
            // Yup. Record the current facing of the tank and the turret.
            out.write("\" /><facing value=\"");
            value = tank.getFacing();
            out.write(String.valueOf(value));
            out.write("\" /><turretFacing value=\"");
            value = tank.getSecondaryFacing();
            out.write(String.valueOf(value));
            out.write("\" /><turretLocked value=\"true");
        }
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
        Tank entity = null;
        String attrStr;
        int attrVal;

        // Did we get a null node?
        if (null == node) {
            throw new IllegalArgumentException("The Tank node is null.");
        }

        // Make sure that the node is for an Tank unit.
        attrStr = node.getAttribute("name");
        if (!node.getName().equals("class") || null == attrStr
                || !attrStr.equals("Tank")) {
            throw new IllegalStateException("Not passed an Tank node.");
        }

        // TODO : perform version checking.

        // Create the entity.
        entity = new Tank();

        // Walk the board node's children.
        Enumeration<?> children = node.elements();
        while (children.hasMoreElements()) {
            ParsedXML child = (ParsedXML) children.nextElement();
            String childName = child.getName();

            // Handle null child names.
            if (null == childName) {

                // No-op.
            }

            // Did we find the stunnedTurns node?
            else if (childName.equals("stunnedTurns")) {

                // Get the Tank's stunned turns.
                attrStr = child.getAttribute("value");
                if (null == attrStr) {
                    throw new IllegalStateException(
                            "Couldn't decode the stunnedTurns for a Tank unit.");
                }

                // Try to pull the number from the attribute string
                try {
                    attrVal = Integer.parseInt(attrStr);
                } catch (NumberFormatException exp) {
                    throw new IllegalStateException(
                            "Couldn't get an integer from " + attrStr);
                }
                entity.setStunnedTurns(attrVal);
            }

            // Did we find the hasNoTurret node?
            else if (childName.equals("hasNoTurret")) {

                // See if the Tank has a no turret.
                attrStr = child.getAttribute("value");
                if (null == attrStr) {
                    throw new IllegalStateException(
                            "Couldn't decode hasNoTurret for a Tank unit.");
                }

                // If the value is "true", the Tank has a no turret.
                if (attrStr.equals("true")) {
                    entity.setHasNoTurret(true);
                } else {
                    entity.setHasNoTurret(false);
                }
            }

            // Did we find the moveHit node?
            else if (childName.equals("moveHit")) {

                // See if the Tank has a move hit.
                attrStr = child.getAttribute("value");
                if (null == attrStr) {
                    throw new IllegalStateException(
                            "Couldn't decode moveHit for a Tank unit.");
                }

                // If the value is "true", the Tank move a hit pending.
                if (attrStr.equals("true")) {
                    entity.immobilize();
                    entity.applyDamage();
                }
            }

            // Did we find the moveHitPending node?
            else if (childName.equals("moveHitPending")) {

                // See if the Tank has a move hit pending.
                attrStr = child.getAttribute("value");
                if (null == attrStr) {
                    throw new IllegalStateException(
                            "Couldn't decode moveHitPending for a Tank unit.");
                }

                // If the value is "true", the Tank move a hit pending.
                if (attrStr.equals("true")) {
                    entity.immobilize();
                }
            }

            // Did we find the facing node?
            else if (childName.equals("facing")) {

                // Get the Tank's facing.
                attrStr = child.getAttribute("value");
                if (null == attrStr) {
                    throw new IllegalStateException(
                            "Couldn't decode the facing for a Tank unit.");
                }

                // Try to pull the number from the attribute string
                try {
                    attrVal = Integer.parseInt(attrStr);
                } catch (NumberFormatException exp) {
                    throw new IllegalStateException(
                            "Couldn't get an integer from " + attrStr);
                }
                entity.setFacing(attrVal);
            }

            // Did we find the turret's secondaryFacing node?
            else if (childName.equals("turretFacing")) {

                // Get the Tank's turret's facing.
                attrStr = child.getAttribute("value");
                if (null == attrStr) {
                    throw new IllegalStateException(
                            "Couldn't decode the turret's secondaryFacing for a Tank unit.");
                }

                // Try to pull the number from the attribute string
                try {
                    attrVal = Integer.parseInt(attrStr);
                } catch (NumberFormatException exp) {
                    throw new IllegalStateException(
                            "Couldn't get an integer from " + attrStr);
                }
                entity.setSecondaryFacing(attrVal);
            }

            // Did we find the turretLocked node?
            else if (childName.equals("turretLocked")) {

                // See if the Tank move a hit pending.
                attrStr = child.getAttribute("value");
                if (null == attrStr) {
                    throw new IllegalStateException(
                            "Couldn't decode turretLocked for a Tank unit.");
                }

                // If the value is "true", the Tank move a hit pending.
                if (attrStr.equals("true")) {
                    entity.lockTurret();
                }
            }

        } // Handle the next element.

        // Return the entity.
        return entity;

    }

}

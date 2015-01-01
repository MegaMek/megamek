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
import java.util.Iterator;
import java.util.Vector;

import megamek.common.AmmoType;
import megamek.common.BattleArmor;
import megamek.common.BipedMech;
import megamek.common.Coords;
import megamek.common.CriticalSlot;
import megamek.common.Entity;
import megamek.common.GunEmplacement;
import megamek.common.IArmorState;
import megamek.common.IGame;
import megamek.common.Infantry;
import megamek.common.InfernoTracker;
import megamek.common.Mech;
import megamek.common.Mounted;
import megamek.common.Pilot;
import megamek.common.Player;
import megamek.common.Protomech;
import megamek.common.QuadMech;
import megamek.common.Tank;
import megamek.common.TechConstants;

/**
 * Objects of this class can encode a <code>Entity</code> object as XML into
 * an output writer and decode one from a parsed XML node. It is used when
 * saving games into a version- neutral format.
 *
 * @author James Damour <suvarov454@users.sourceforge.net>
 */
public class EntityEncoder {

    /**
     * Encode a <code>Entity</code> object to an output writer.
     *
     * @param entity - the <code>Entity</code> to be encoded. This value must
     *            not be <code>null</code>.
     * @param out - the <code>Writer</code> that will receive the XML. This
     *            value must not be <code>null</code>.
     * @throws IllegalArgumentException if the entity is <code>null</code>.
     * @throws IOException if there's any error on write.
     */
    public static void encode(Entity entity, Writer out) throws IOException {
        Enumeration<Entity> iter; // used when marching through a list of sub-elements
        Coords coords;
        int turns;
        String substr;

        // First, validate our input.
        if (null == entity) {
            throw new IllegalArgumentException("The entity is null.");
        }
        if (null == out) {
            throw new IllegalArgumentException("The writer is null.");
        }

        // Make sure any transported entities are written first.
        iter = entity.getLoadedUnits().elements();
        while (iter.hasMoreElements()) {
            EntityEncoder.encode(iter.nextElement(), out);
        }

        // Start writing this entity to the file.
        out.write("<entity chassis=\"");
        out.write(entity.getChassis());
        out.write("\" model=\"");
        out.write(entity.getModel());
        out.write("\" type=\"");
        out.write(entity.getMovementModeAsString());
        out.write("\" typeVal=\"");
        out.write(String.valueOf(entity.getMovementMode()));
        out.write("\" techBase=\"");
        out.write(entity.getTechLevel() + ":"
                + TechConstants.getLevelName(entity.getTechLevel()));
        out.write("\" year=\"");
        out.write(String.valueOf(entity.getYear()));
        out.write("\" mass=\"");
        out.write(String.valueOf(entity.getWeight()));
        out.write("\" walkMp=\"");
        out.write(String.valueOf(entity.getOriginalWalkMP()));
        out.write("\" jumpMp=\"");
        out.write(String.valueOf(entity.getOriginalJumpMP()));
        out.write("\">");

        // Add the crew this entity.
        final Pilot crew = entity.getCrew();
        out.write("<pilot name=\"");
        out.write(crew.getName());
        out.write("\" gunnery=\"");
        out.write(String.valueOf(crew.getGunnery()));
        out.write("\" piloting=\"");
        out.write(String.valueOf(crew.getPiloting()));
        if (crew.isDead() || (crew.getHits() > 5)) {
            out.write("\" hits=\"Dead");
        } else if (crew.getHits() > 0) {
            out.write("\" hits=\"");
            out.write(String.valueOf(crew.getHits()));
        }
        if (crew.countAdvantages() > 0) {
            out.write("\" advantages=\"");
            out.write(String.valueOf(crew.getAdvantageList(" ")));
        }
        if (crew.countMDImplants() > 0) {
            out.write("\" implants=\"");
            out.write(String.valueOf(crew.getImplantList(" ")));
        }
        out.write("\"/>");

        // Write the game-specific data.
        out.write("<entityData gameId=\"");
        out.write(String.valueOf(entity.getId()));
        out.write("\" externalId=\"");
        out.write(String.valueOf(entity.getExternalId()));
        out.write("\" ownerId=\"");
        out.write(String.valueOf(entity.getOwnerId()));
        out.write("\" facing=\"");
        out.write(String.valueOf(entity.getFacing()));
        out.write("\" secondaryFacing=\"");
        out.write(String.valueOf(entity.getSecondaryFacing()));
        out.write("\" walkMpCurrent=\"");
        out.write(String.valueOf(entity.getWalkMP()));
        out.write("\" isOmni=\"");
        out.write(entity.isOmni() ? "true" : "false");
        out.write("\" jumpMpCurrent=\"");
        out.write(String.valueOf(entity.getJumpMP()));
        out.write("\" C3MasterId=\"");
        out.write(String.valueOf(entity.getC3MasterId()));
        out.write("\" transportId=\"");
        out.write(String.valueOf(entity.getTransportId()));
        out.write("\" swarmTargetId=\"");
        out.write(String.valueOf(entity.getSwarmTargetId()));
        out.write("\" swarmAttackerId=\"");
        out.write(String.valueOf(entity.getSwarmAttackerId()));
        out.write("\" removalCondition=\"");
        out.write(String.valueOf(entity.getRemovalCondition()));
        out.write("\" deployRound=\"");
        out.write(String.valueOf(entity.getDeployRound()));

        out.write("\" isShutDown=\"");
        out.write(entity.isShutDown() ? "true" : "false");
        out.write("\" isDoomed=\"");
        out.write(entity.isDoomed() ? "true" : "false");
        out.write("\" isDestroyed=\"");
        out.write(entity.isDestroyed() ? "true" : "false");
        out.write("\" isDone=\"");
        out.write(entity.isDone() ? "true" : "false");
        out.write("\" isProne=\"");
        out.write(entity.isProne() ? "true" : "false");
        out.write("\" isFindingClub=\"");
        out.write(entity.isFindingClub() ? "true" : "false");
        out.write("\" isArmsFlipped=\"");
        out.write(entity.getArmsFlipped() ? "true" : "false");
        out.write("\" isUnjammingRAC=\"");
        out.write(entity.isUnjammingRAC() ? "true" : "false");
        out.write("\" isSpotting=\"");
        out.write(entity.isSpotting() ? "true" : "false");
        out.write("\" isClearingMinefield=\"");
        out.write(entity.isClearingMinefield() ? "true" : "false");
        out.write("\" isSalvage=\"");
        out.write(entity.isSalvage() ? "true" : "false");
        out.write("\" isDeployed=\"");
        out.write(entity.isDeployed() ? "true" : "false");
        out.write("\" isUnloadedThisTurn=\"");
        out.write(entity.isUnloadedThisTurn() ? "true" : "false");

        out.write("\" heat=\"");
        out.write(String.valueOf(entity.heat));
        out.write("\" heatBuildup=\"");
        out.write(String.valueOf(entity.heatBuildup));
        out.write("\" heatFromExternal=\"");
        out.write(String.valueOf(entity.heatFromExternal));
        out.write("\" delta_distance=\"");
        out.write(String.valueOf(entity.delta_distance));
        out.write("\" mpUsed=\"");
        out.write(String.valueOf(entity.mpUsed));
        out.write("\" moved=\"");
        out.write(String.valueOf(entity.moved));
        out.write("\" damageThisPhase=\"");
        out.write(String.valueOf(entity.damageThisPhase));
        out.write("\" engineHitsThisRound=\"");
        out.write(String.valueOf(entity.engineHitsThisRound));
        out.write("\" rolledForEngineExplosion=\"");
        out.write(entity.rolledForEngineExplosion ? "true" : "false");
        out.write("\" dodging=\"");
        out.write(entity.dodging ? "true" : "false");
        out.write("\" >");

        // Now save the entity's coordinates.
        coords = entity.getPosition();
        if (null != coords) {
            CoordsEncoder.encode(coords, out);
        }

        // Is the entity performing a displacement attack?
        if (entity.hasDisplacementAttack()) {
            EntityActionEncoder.encode(entity.getDisplacementAttack(), out);
        }

        // Add the narc pods attached to this entity (if any are needed).
        substr = getNarcString(entity);
        if (null != substr) {
            out.write(substr);
        }

        // Encode the infernos burning on this entity.
        if (entity.infernos.isStillBurning()) {
            // Encode the infernos on this entity.
            out.write("<inferno>");
            turns = entity.infernos.getArrowIVTurnsLeftToBurn();
            // This value may be zero.
            if (turns > 0) {
                out.write("<arrowiv turns=\"");
                out.write(String.valueOf(turns));
                out.write("\" />");
            }
            // -(Arrow IV turns - All Turns) = Standard Turns.
            turns -= entity.infernos.getTurnsLeftToBurn();
            turns = -turns;
            if (turns > 0) {
                out.write("<standard turns=\"");
                out.write(String.valueOf(turns));
                out.write("\" />");
            }
            out.write("</inferno>");
        }

        // Do we have any transporters?
        String transporters = Entity.encodeTransporters(entity);
        if ((null != transporters) && (0 == transporters.length())) {
            out.write("<transporters value=\"");
            out.write(transporters);
            out.write("\" />");
        }

        // Record the IDs of all transported units (if any).
        iter = entity.getLoadedUnits().elements();
        if (iter.hasMoreElements()) {
            out.write("<loadedUnits>");
            while (iter.hasMoreElements()) {
                Entity loaded = iter.nextElement();
                out.write("<entityRef gameId=\"");
                out.write(String.valueOf(loaded.getId()));
                out.write("\" />");
            }
            out.write("</loadedUnits>");
        }

        // Handle sub-classes of Entity.
        out.write("<class name=\"");
        if (entity instanceof BipedMech) {
            out.write("BipedMech\">");
            BipedMechEncoder.encode(entity, out);
        } else if (entity instanceof QuadMech) {
            out.write("QuadMech\">");
            QuadMechEncoder.encode(entity, out);
        } else if (entity instanceof Tank) {
            out.write("Tank\">");
            TankEncoder.encode(entity, out);
        } else if (entity instanceof BattleArmor) {
            out.write("BattleArmor\">");
            BattleArmorEncoder.encode(entity, out);
        } else if (entity instanceof Infantry) {
            out.write("Infantry\">");
            InfantryEncoder.encode(entity, out);
        } else if (entity instanceof Protomech) {
            out.write("Protomech\">");
            ProtomechEncoder.encode(entity, out);
        } else if (entity instanceof GunEmplacement) {
            out.write("GunEmplacement\">");
            GunEmplacementEncoder.encode(entity, out);
        } else {
            throw new IllegalStateException("Unexpected entity type "
                    + entity.getClass().getName());
        }
        out.write("</class>");

        // Finish the game-specific data.
        out.write("</entityData>");

        // Encode this unit's equipment.
        Iterator<Mounted> iter2 = entity.getEquipment().iterator();
        if (iter2.hasNext()) {
            out.write("<entityEquipment>");
            int index = 0;
            while (iter2.hasNext()) {
                substr = EntityEncoder.formatEquipment(index,iter2.next(), entity);
                if (null != substr) {
                    out.write(substr);
                }
                index++;
            }
            out.write("</entityEquipment>");
        }

        // Add the locations of this entity (if any are needed).
        substr = getLocString(entity);
        if (null != substr) {
            out.write(substr);
        }

        // Finish the XML stream for this entity.
        out.write("</entity>");
    }

    /**
     * Produce a string describing all NARC pods on the entity.
     *
     * @param entity - the <code>Entity</code> being examined. This value may
     *            be <code>null</code>.
     * @return a <code>String</code> describing the equipment. This value may
     *         be <code>null</code>.
     */
    private static String getNarcString(Entity entity) {
        // null in, null out
        if (null == entity) {
            return null;
        }

        // Show all teams that have NARCed the entity.
        StringBuffer output = new StringBuffer();
        boolean narced = false;
        for (int team = Player.TEAM_NONE; team < Player.MAX_TEAMS; team++) {
            if (entity.isNarcedBy(team)) {

                // Is this the first narc on the entity?
                if (!narced) {
                    output.append("<narcs>");
                    narced = true;
                }

                // Add this team to the NARC pods.
                output.append("<narc type=\"Standard\" team=\"");
                output.append(String.valueOf(team));
                output.append("\" />");
            }
        }

        // If the entity wasn't narced, return a null.
        if (!narced) {
            return null;
        }

        // Finish off this section, and return the string.
        output.append("</narcs>");
        return output.toString();
    }

    /**
     * Produce a string describing a piece of equipment.
     *
     * @param index - the <code>int</code> index of this equipment on the
     *            given entity.
     * @param mount - the <code>Mounted</code> object of the equipment. This
     *            value should not be <code>null</code>.
     * @param entity - the <code>Entity</code> that has this mount.
     * @return a <code>String</code> describing the equipment. This value will
     *         be <code>null</code> if a <code>null</code> was passed.
     */
    private static String formatEquipment(int index, Mounted mount,
            Entity entity) {
        StringBuffer output = new StringBuffer();

        // null in, null out.
        if (null == mount) {
            return null;
        }

        // Format this piece of equipment.
        output.append("<equipment index=\"");
        output.append(String.valueOf(index));
        output.append("\" type=\"");
        output.append(mount.getType().getInternalName());
        output.append("\" location=\"");
        output.append(String.valueOf(mount.getLocation()));
        output.append("\" isRear=\"");
        output.append(mount.isRearMounted() ? "true" : "false");
        if (mount.getType() instanceof AmmoType) {
            output.append("\" shots=\"");
            output.append(String.valueOf(mount.getShotsLeft()));
        }
        output.append("\" curMode=\"");
        output.append(mount.curMode().getName());
        output.append("\" pendingMode=\"");
        output.append(mount.pendingMode().getName());
        output.append("\" linkedRef=\"");
        if (null == mount.getLinked()) {
            output.append("N/A");
        } else {
            output.append(String.valueOf(entity.getEquipmentNum(mount
                    .getLinked())));
        }
        output.append("\" foundCrits=\"");
        output.append(String.valueOf(mount.getFoundCrits()));

        output.append("\" isUsedThisRound=\"");
        output.append(mount.isUsedThisRound() ? "true" : "false");
        output.append("\" isBreached=\"");
        output.append(mount.isBreached() ? "true" : "false");
        output.append("\" isHit=\"");
        output.append(mount.isHit() ? "true" : "false");
        output.append("\" isDestroyed=\"");
        output.append(mount.isDestroyed() ? "true" : "false");
        output.append("\" isMissing=\"");
        output.append(mount.isMissing() ? "true" : "false");
        output.append("\" isJammed=\"");
        output.append(mount.isJammed() ? "true" : "false");
        output.append("\" isPendingDump=\"");
        output.append(mount.isPendingDump() ? "true" : "false");
        output.append("\" isDumping=\"");
        output.append(mount.isDumping() ? "true" : "false");
        output.append("\" isSplit=\"");
        output.append(mount.isSplit() ? "true" : "false");
        output.append("\" isFired=\"");
        output.append(mount.isFired() ? "true" : "false");
        output.append("\"/>");

        // Return a String.
        return output.toString();
    }

    /**
     * Produce a string describing the equipment in a critical slot.
     *
     * @param slot - the <code>CriticalSlot</code> being encoded.
     * @param mount - the <code>Mounted</code> object of the equipment. This
     *            value should be <code>null</code> for a slot with system
     *            equipment.
     * @return a <code>String</code> describing the slot.
     */
    private static String formatSlot(CriticalSlot slot, Mounted mount) {
        StringBuffer output = new StringBuffer();

        // Don't forget... slots start at index 1.
        output.append("<slot index=\"");
        output.append(String.valueOf(slot.getIndex() + 1));
        output.append("\" type=\"");
        if (mount == null) {
            output.append("System");
        } else {
            output.append(mount.getType().getInternalName());
            if (mount.isRearMounted()) {
                output.append("\" isRear=\"true");
            }
            if (mount.getType() instanceof AmmoType) {
                output.append("\" shots=\"");
                output.append(String.valueOf(mount.getShotsLeft()));
            }
        }
        output.append("\" isHit=\"");
        output.append(slot.isHit() ? "true" : "false");
        output.append("\" isDestroyed=\"");
        output.append(slot.isDestroyed() ? "true" : "false");
        output.append("\" isMissing=\"");
        output.append(slot.isMissing() ? "true" : "false");
        output.append("\" isBreached=\"");
        output.append(slot.isBreached() ? "true" : "false");
        output.append("\" isHittable=\"");
        output.append(slot.isEverHittable() ? "true" : "false");
        output.append("\"/>");

        // Return a String.
        return output.toString();
    }

    /**
     * Helper function that generates a string identifying the state of the
     * locations for an entity.
     *
     * @param entity - the <code>Entity</code> whose location state is needed
     */
    private static String getLocString(Entity entity) {
        boolean isMech = entity instanceof Mech;
        StringBuffer output = new StringBuffer();

        // Walk through the locations for the entity,
        // and only record damage and ammo.
        for (int loc = 0; loc < entity.locations(); loc++) {

            // Add this location to the output string.
            output.append("<location index=\"");
            output.append(String.valueOf(loc));
            output.append("\"> ");

            // Record values of armor and internal structure,
            // unless the section never has armor.
            if (entity.getOInternal(loc) != IArmorState.ARMOR_NA) {
                output.append("<armor points=\"");
                output.append(String.valueOf(entity.getArmor(loc)));
                output.append("\"/>");
                output.append("<armor points=\"");
                output.append(String.valueOf(entity.getInternal(loc)));
                output.append("\" type=\"Internal\"/>");
                if (entity.hasRearArmor(loc)) {
                    output.append("<armor points=\"");
                    output.append(String.valueOf(entity.getArmor(loc, true)));
                    output.append("\" type=\"Rear\"/>");
                }
            }

            // Walk through the slots in this location.
            for (int loop = 0; loop < entity.getNumberOfCriticals(loc); loop++) {

                // Get this slot.
                CriticalSlot slot = entity.getCritical(loc, loop);

                // Did we get a slot?
                if (null == slot) {

                    // Nope. Record missing actuators on Biped Mechs.
                    if (isMech && !entity.entityIsQuad()
                            && ((loc == Mech.LOC_RARM) || (loc == Mech.LOC_LARM))
                            && ((loop == 2) || (loop == 3))) {
                        output.append("<slot index=\"");
                        output.append(String.valueOf(loop + 1));
                        output.append("\" type=\"Empty\"/>");
                    }

                } else {

                    // Yup. If the equipment isn't a system, get it.
                    Mounted mount = null;
                    if (CriticalSlot.TYPE_EQUIPMENT == slot.getType()) {
                        mount = entity.getEquipment(slot.getIndex());
                    }

                    // Format the slot.
                    output.append(formatSlot(slot, mount));

                } // End have-slot

            } // Check the next slot in this location

            // Tanks don't have slots, and Protomechs only have
            // system slots, so we have to handle their ammo specially.
            if ((entity instanceof Tank) || (entity instanceof Protomech)) {
                for (Mounted mount : entity.getAmmo()) {

                    // Is this ammo in the current location?
                    if (mount.getLocation() == loc) {
                        output.append("<slot index=\"N/A\" type=\"");
                        output.append(mount.getType().getInternalName());
                        output.append("\" shots=\"");
                        output.append(String.valueOf(mount.getShotsLeft()));
                        output.append("\"/>");
                    }

                } // Check the next ammo.

            } // End is-tank-or-proto

            // Finish off the location
            output.append("</location>");

        } // Handle the next location

        // Convert the output into a String and return it.
        return output.toString();

    } // End private static String getLocString( Entity )

    /**
     * Helper function to decode the pilot (crew) of an <code>Entity</code>
     * object from the passed node.
     *
     * @param node - the <code>ParsedXML</code> node for this object. This
     *            value must not be <code>null</code>.
     * @param entity - the <code>Entity</code> the decoded object belongs to.
     * @throws IllegalArgumentException if the node is <code>null</code>.
     * @throws IllegalStateException if the node does not contain a valid
     *             <code>Entity</code>.
     */
    private static void decodePilot(ParsedXML node, Entity entity) {
        // TODO : implement me
    }

    /**
     * Helper function to decode the equipment of an <code>Entity</code>
     * object from the passed node.
     *
     * @param node - the <code>ParsedXML</code> node for this object. This
     *            value must not be <code>null</code>.
     * @param entity - the <code>Entity</code> the decoded object belongs to.
     * @throws IllegalArgumentException if the node is <code>null</code>.
     * @throws IllegalStateException if the node does not contain a valid
     *             <code>Entity</code>.
     */
    private static void decodeEntityEquipment(ParsedXML node, Entity entity) {
        // TODO : implement me
    }

    /**
     * Helper function to decode a location of an <code>Entity</code> object
     * from the passed node.
     *
     * @param node - the <code>ParsedXML</code> node for this object. This
     *            value must not be <code>null</code>.
     * @param entity - the <code>Entity</code> the decoded object belongs to.
     * @throws IllegalArgumentException if the node is <code>null</code>.
     * @throws IllegalStateException if the node does not contain a valid
     *             <code>Entity</code>.
     */
    private static void decodeLocation(ParsedXML node, Entity entity) {
        // TODO : implement me
    }

    /**
     * Helper function to decode the inferno rounds on an <code>Entity</code>
     * object from the passed node.
     *
     * @param node - the <code>ParsedXML</code> node for this object. This
     *            value must not be <code>null</code>.
     * @param entity - the <code>Entity</code> the decoded object belongs to.
     * @throws IllegalArgumentException if the node is <code>null</code>.
     * @throws IllegalStateException if the node does not contain a valid
     *             <code>Entity</code>.
     */
    private static void decodeInferno(ParsedXML node, Entity entity) {
        String attrStr = null;
        int attrVal = 0;

        // Did we get a null node?
        if (null == node) {
            throw new IllegalArgumentException("The inferno is null.");
        }

        // Make sure that the node is for a EntityData object.
        if (!node.getName().equals("inferno")) {
            throw new IllegalStateException("Not passed a inferno node.");
        }

        // Try to find the inferno detail nodes.
        Enumeration<?> details = node.elements();
        while (details.hasMoreElements()) {
            ParsedXML detail = (ParsedXML) details.nextElement();

            // Have we found the Arrow IV inferno detail?
            if (detail.getName().equals("arrowiv")) {

                // Get the burn turns attribute.
                attrStr = detail.getAttribute("turns");
                if (null == attrStr) {
                    throw new IllegalStateException(
                            "Couldn't decode the burn turns for an Arrow IV inferno round.");
                }

                // Try to pull the value from the string
                try {
                    attrVal = Integer.parseInt(attrStr);
                } catch (NumberFormatException exp) {
                    throw new IllegalStateException(
                            "Couldn't get an integer from " + attrStr);
                }

                // Add the number of Arrow IV burn turns.
                entity.infernos.add(InfernoTracker.INFERNO_IV_TURN, attrVal);

            } // End found-arrowiv-detail

            // Have we found the standard inferno entry?
            else if (detail.getName().equals("standard")) {

                // Get the burn turns attribute.
                attrStr = detail.getAttribute("turns");
                if (null == attrStr) {
                    throw new IllegalStateException(
                            "Couldn't decode the burn turns for a standard inferno round.");
                }

                // Try to pull the value from the string
                try {
                    attrVal = Integer.parseInt(attrStr);
                } catch (NumberFormatException exp) {
                    throw new IllegalStateException(
                            "Couldn't get an integer from " + attrStr);
                }

                // Add the number of standard burn turns.
                entity.infernos.add(InfernoTracker.STANDARD_TURN, attrVal);

            } // End found-standard-detail

        } // Handle the next detail node.

    }

    /**
     * Helper function to decode a <code>Entity</code> object from the passed
     * node.
     *
     * @param node - the <code>ParsedXML</code> node for this object. This
     *            value must not be <code>null</code>.
     * @param game - the <code>IGame</code> the decoded object belongs to.
     * @return the <code>Entity</code> object based on the node.
     * @throws IllegalArgumentException if the node is <code>null</code>.
     * @throws IllegalStateException if the node does not contain a valid
     *             <code>Entity</code>.
     */
    @SuppressWarnings("unused")
    // supressing unused warnings.
    private static Entity decodeEntityData(ParsedXML node, IGame game) {
        String attrStr = null;
        int attrVal = 0;
        boolean attrTrue = false;
        Entity entity = null;
        Coords coords = null;
        ParsedXML actionNode = null;
        ParsedXML narcNode = null;
        ParsedXML infernoNode = null;
        ParsedXML loadedUnitsNode = null;

        // Did we get a null node?
        if (null == node) {
            throw new IllegalArgumentException("The entityData is null.");
        }

        // Make sure that the node is for a EntityData object.
        if (!node.getName().equals("entityData")) {
            throw new IllegalStateException("Not passed a entityData node.");
        }

        // TODO : perform version checking.

        // Walk the entityData node's children.
        Enumeration<?> children = node.elements();
        while (children.hasMoreElements()) {
            ParsedXML child = (ParsedXML) children.nextElement();
            String childName = child.getName();

            // Handle null child names.
            if (null == childName) {

                // No-op.
            }

            // Did we find the coords node?
            else if (childName.equals("coords")) {

                // We can decode the coords immediately.
                coords = CoordsEncoder.decode(child, game);

            } // End found-"coords"-child

            // Did we find the action node?
            // TODO : rename me
            else if (childName.equals("action")) {

                // Save the action node for later decoding.
                actionNode = child;

            } // End found-"action"-child

            // Did we find the narcs node?
            else if (childName.equals("narcs")) {

                // Save the narc node for later decoding.
                narcNode = child;

            } // End found-"narc"-child

            // Did we find the inferno node?
            else if (childName.equals("inferno")) {

                // Save the inferno node for later decoding.
                infernoNode = child;

            } // End found-"inferno"-child

            // Did we find the loadedUnits node?
            else if (childName.equals("loadedUnits")) {

                // Save the loadedUnits node for later decoding.
                loadedUnitsNode = child;

            } // End found-"loadedUnits"-child

            // Did we find the class node?
            else if (childName.equals("class")) {

                // Create the appropriate sub-class of Entity.
                attrStr = child.getAttribute("name");
                if (null == attrStr) {
                    throw new IllegalStateException(
                            "Couldn't decode the name of a class node.");
                } else if (attrStr.equals("BipedMech")) {
                    entity = BipedMechEncoder.decode(child, game);
                } else if (attrStr.equals("QuadMech")) {
                    entity = QuadMechEncoder.decode(child, game);
                } else if (attrStr.equals("Tank")) {
                    entity = TankEncoder.decode(child, game);
                } else if (attrStr.equals("BattleArmor")) {
                    entity = BattleArmorEncoder.decode(child, game);
                } else if (attrStr.equals("Infantry")) {
                    entity = InfantryEncoder.decode(child, game);
                } else if (attrStr.equals("Protomech")) {
                    entity = ProtomechEncoder.decode(child, game);
                } else if (attrStr.equals("GunEmplacement")) {
                    entity = GunEmplacementEncoder.decode(child, game);
                } else {
                    throw new IllegalStateException(
                            "Unexpected name for a class node: " + attrStr);
                }

            } // End found-"class"-child

        } // Look at the next child.

        // Did we find the entity yet?
        if (null == entity) {
            throw new IllegalStateException(
                    "Couldn't locate the class for an entityData node.");
        }

        // Decode the inferno node.
        EntityEncoder.decodeInferno(infernoNode, entity);

        // TODO : a whole lot more decoding needed.

        return entity;
    }

    /**
     * Decode a <code>Entity</code> object from the passed node.
     *
     * @param node - the <code>ParsedXML</code> node for this object. This
     *            value must not be <code>null</code>.
     * @param game - the <code>IGame</code> the decoded object belongs to.
     * @return the <code>Entity</code> object based on the node.
     * @throws IllegalArgumentException if the node is <code>null</code>.
     * @throws IllegalStateException if the node does not contain a valid
     *             <code>Entity</code>.
     */
    public static Entity decode(ParsedXML node, IGame game) {
        String attrStr = null;
        int attrVal = 0;
        Entity entity = null;
        Vector<ParsedXML> locations = new Vector<ParsedXML>();
        ParsedXML pilotNode = null;
        ParsedXML equipNode = null;
        Enumeration<?> children = null;
        ParsedXML child = null;
        String childName;

        // Did we get a null node?
        if (null == node) {
            throw new IllegalArgumentException("The entity is null.");
        }

        // Make sure that the node is for a Entity object.
        if (!node.getName().equals("entity")) {
            throw new IllegalStateException("Not passed a entity node.");
        }

        // TODO : perform version checking.

        // Walk the entity node's children, finding bits for later parsing..
        children = node.elements();
        while (children.hasMoreElements()) {
            child = (ParsedXML) children.nextElement();
            childName = child.getName();

            // Handle null child childNames.
            if (null == childName) {

                // No-op.
            }

            // Did we find the pilot node?
            else if (childName.equals("pilot")) {

                // Save the entity's pilot for later decoding.
                pilotNode = child;

            } // End found-"entityEquipment"-node

            // Did we find the entityData node?
            else if (childName.equals("entityData")) {

                // Did we find the entity already?
                if (null != entity) {
                    throw new IllegalStateException(
                            "Found two entityData nodes for an Entity node.");
                }

                // Decode the entity data.
                entity = EntityEncoder.decodeEntityData(child, game);

            } // End found-"entityData"-node

            // Did we find the entityEquipment node?
            else if (childName.equals("entityEquipment")) {

                // Save the entity equipment for later decoding.
                equipNode = child;

            } // End found-"entityEquipment"-node

            // Did we find the location node?
            else if (childName.equals("location")) {

                // Save this location for later decoding.
                locations.addElement(child);

            } // End found-"location"-node

        } // Look at the next child.

        // Did we find the needed elements?
        if (null == entity) {
            throw new IllegalStateException(
                    "Couldn't locate the entityData for an Entity node.");
        } else if (null == pilotNode) {
            throw new IllegalStateException(
                    "Couldn't locate the pilot for an Entity node.");
        } else if (null == equipNode) {
            throw new IllegalStateException(
                    "Couldn't locate the entityEquipment for an Entity node.");
        } else if (locations.size() != entity.locations()) {
            StringBuffer msgBuf = new StringBuffer();
            msgBuf.append("Found ").append(locations.size()).append(
                    " locations for an Entity node. ").append(
                    "Was expecting to find ").append(entity.locations())
                    .append(".");
            throw new IllegalStateException(msgBuf.toString());
        }

        // Decode the entity node's chassis.
        attrStr = node.getAttribute("chassis");
        if (null == attrStr) {
            throw new IllegalStateException(
                    "Couldn't decode the chassis from an Entity node.");
        }
        entity.setChassis(attrStr);

        // Decode the entity node's model.
        attrStr = node.getAttribute("model");
        if (null == attrStr) {
            throw new IllegalStateException(
                    "Couldn't decode the model from an Entity node.");
        }
        entity.setModel(attrStr);

        // Decode the entity node's movement type.
        attrStr = node.getAttribute("typeVal");
        if (null == attrStr) {
            throw new IllegalStateException(
                    "Couldn't decode the typeVal from an Entity node.");
        }

        // Try to pull the value from the string
        try {
            attrVal = Integer.parseInt(attrStr);
        } catch (NumberFormatException exp) {
            throw new IllegalStateException("Couldn't get an integer from "
                    + attrStr);
        }
        entity.setMovementMode(attrVal);

        // Decode the entity node's year.
        attrStr = node.getAttribute("year");
        if (null == attrStr) {
            throw new IllegalStateException(
                    "Couldn't decode the year from an Entity node.");
        }

        // Try to pull the value from the string
        try {
            attrVal = Integer.parseInt(attrStr);
        } catch (NumberFormatException exp) {
            throw new IllegalStateException("Couldn't get an integer from "
                    + attrStr);
        }
        entity.setYear(attrVal);

        // Decode the entity node's techBase.
        attrStr = node.getAttribute("techBase");
        if (null == attrStr) {
            throw new IllegalStateException(
                    "Couldn't decode the techBase from an Entity node.");
        }

        // Try to pull the value from the string
        try {
            attrVal = Integer.parseInt(attrStr.substring(0, 1));
        } catch (NumberFormatException exp) {
            throw new IllegalStateException("Couldn't get an integer from "
                    + attrStr.substring(0, 1));
        }
        entity.setTechLevel(attrVal);

        // Decode the entity node's mass.
        attrStr = node.getAttribute("mass");
        if (null == attrStr) {
            throw new IllegalStateException(
                    "Couldn't decode the mass from an Entity node.");
        }

        // Try to pull the value from the string
        try {
            attrVal = Integer.parseInt(attrStr);
        } catch (NumberFormatException exp) {
            throw new IllegalStateException("Couldn't get an integer from "
                    + attrStr);
        }
        entity.setWeight(attrVal);

        // Decode the entity node's walkMp.
        attrStr = node.getAttribute("walkMp");
        if (null == attrStr) {
            throw new IllegalStateException(
                    "Couldn't decode the walkMp from an Entity node.");
        }

        // Try to pull the value from the string
        try {
            attrVal = Integer.parseInt(attrStr);
        } catch (NumberFormatException exp) {
            throw new IllegalStateException("Couldn't get an integer from "
                    + attrStr);
        }
        entity.setOriginalWalkMP(attrVal);

        // Decode the entity node's jumpMp.
        attrStr = node.getAttribute("jumpMp");
        if (null == attrStr) {
            throw new IllegalStateException(
                    "Couldn't decode the jumpMp from an Entity node.");
        }

        // Try to pull the value from the string
        try {
            attrVal = Integer.parseInt(attrStr);
        } catch (NumberFormatException exp) {
            throw new IllegalStateException("Couldn't get an integer from "
                    + attrStr);
        }
        entity.setOriginalJumpMP(attrVal);

        // Try to pull the value from the string
        try {
            attrVal = Integer.parseInt(attrStr);
        } catch (NumberFormatException exp) {
            throw new IllegalStateException("Couldn't get an integer from "
                    + attrStr);
        }

        // Decode the entity's pilot.
        EntityEncoder.decodePilot(pilotNode, entity);

        // Decode the entity's equipment.
        EntityEncoder.decodeEntityEquipment(equipNode, entity);

        // Decode the entity's locations.
        children = locations.elements();
        while (children.hasMoreElements()) {
            child = (ParsedXML) children.nextElement();
            EntityEncoder.decodeLocation(child, entity);
        }

        return entity;
    }

}

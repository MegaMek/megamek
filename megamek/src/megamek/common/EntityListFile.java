/*
 * MegaMek - Copyright (C) 2003, 2004, 2005 Ben Mazur (bmazur@sev.org)
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

package megamek.common;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.Vector;

import megamek.MegaMek;
import megamek.client.Client;
import megamek.common.options.PilotOptions;

/**
 * This class provides static methods to save a list of <code>Entity</code>s to,
 * and load a list of <code>Entity</code>s from a file.
 */
public class EntityListFile {

    /**
     * Produce a string describing this armor value. Valid output values are any
     * integer from 0 to 100, N/A, or Destroyed.
     *
     * @param points
     *            - the <code>int</code> value of the armor. This value may be
     *            any valid value of entity armor (including NA, DOOMED, and
     *            DESTROYED).
     * @return a <code>String</code> that matches the armor value.
     */
    private static String formatArmor(int points) {
        // Is the armor destroyed or doomed?
        if ((points == IArmorState.ARMOR_DOOMED)
                || (points == IArmorState.ARMOR_DESTROYED)) {
            return "Destroyed";
        }

        // Was there armor to begin with?
        if (points == IArmorState.ARMOR_NA) {
            return "N/A";
        }

        // Translate the int to a String.
        return String.valueOf(points);
    }

    /**
     * Produce a string describing the equipment in a critical slot.
     *
     * @param index
     *            - the <code>String</code> index of the slot. This value should
     *            be a positive integer or "N/A".
     * @param mount
     *            - the <code>Mounted</code> object of the equipment. This value
     *            should be <code>null</code> for a slot with system equipment.
     * @param isHit
     *            - a <code>boolean</code> that identifies this slot as having
     *            taken a hit.
     * @param isDestroyed
     *            - a <code>boolean</code> that identifies the equipment as
     *            having been destroyed. Note that a single slot in a multi-slot
     *            piece of equipment can be destroyed but not hit; it is still
     *            available to absorb additional critical hits.
     * @return a <code>String</code> describing the slot.
     */
    private static String formatSlot(String index, Mounted mount,
            boolean isHit, boolean isDestroyed, boolean isRepairable,
            boolean isMissing, int indentLvl) {
        StringBuffer output = new StringBuffer();

        output.append("         <slot index=\"");
        output.append(index);
        output.append("\" type=\"");
        if (mount == null) {
            output.append("System");
        } else {
            output.append(mount.getType().getInternalName());
            if (mount.isRearMounted()) {
                output.append("\" isRear=\"true");
            }
            if (mount.isMechTurretMounted()) {
                output.append("\" isTurreted=\"true");
            }
            if (mount.getType() instanceof AmmoType) {
                output.append("\" shots=\"");
                output.append(String.valueOf(mount.getBaseShotsLeft()));
            }
            if ((mount.getType() instanceof WeaponType)
                    && (mount.getType()).hasFlag(WeaponType.F_ONESHOT)) {
                output.append("\" munition=\"");
                output.append(mount.getLinked().getType().getInternalName());
            }
            if (mount.isRapidfire()) {
                output.append("\" rfmg=\"true");
            }
            if (mount.countQuirks() > 0) {
                output.append("\" quirks=\"");
                output.append(String.valueOf(mount.getQuirkList("::")));
            }
            if(mount.isAnyMissingTroopers()) {
                output.append("\" trooperMiss=\"");
                output.append(String.valueOf(mount.getMissingTrooperString()));
            }
        }
        if (isHit) {
            output.append("\" isHit=\"");
            output.append(String.valueOf(isHit));
        }
        if (!isRepairable && (isHit || isDestroyed)) {
            output.append("\" isRepairable=\"");
            output.append(String.valueOf(isRepairable));
        }
        if (isMissing) {
            output.append("\" isMissing=\"");
            output.append(String.valueOf(isMissing));
        }
        output.append("\" isDestroyed=\"");
        output.append(String.valueOf(isDestroyed));
        output.append("\"/>");
        output.append(CommonConstants.NL);

        // Return a String.
        return output.toString();
    }

    /**
     * Helper function to indent based on an indent level
     * @param level
     * @return
     */
    public static String indentStr(int level) {
        String retVal = "";

        for (int x=0; x<level; x++)
            retVal += "\t";

        return retVal;
    }

    /**
     * Helper function that generates a string identifying the state of the
     * locations for an entity.
     *
     * @param entity
     *            - the <code>Entity</code> whose location state is needed
     */
    public static String getLocString(Entity entity, int indentLvl) {
        boolean isMech = entity instanceof Mech;
        boolean haveSlot = false;
        StringBuffer output = new StringBuffer();
        StringBuffer thisLoc = new StringBuffer();
        boolean isDestroyed = false;

        // Walk through the locations for the entity,
        // and only record damage and ammo.
        for (int loc = 0; loc < entity.locations(); loc++) {

            // if the location is blown off, remove it so we can get the real
            // values
            boolean blownOff = entity.isLocationBlownOff(loc);

            // Record destroyed locations.
            if (!(entity instanceof Aero)
                    && !((entity instanceof Infantry) && !(entity instanceof BattleArmor))
                    && (entity.getOInternal(loc) != IArmorState.ARMOR_NA)
                    && (entity.getInternalForReal(loc) <= 0)) {
                isDestroyed = true;
            }

            //exact zeroes for BA should not be treated as destroyed as MHQ uses this to signify
            //suits without pilots
            if(entity instanceof BattleArmor && entity.getInternalForReal(loc) >= 0) {
                isDestroyed = false;
            }

            // Record damage to armor and internal structure.
            // Destroyed locations have lost all their armor and IS.
            if (!isDestroyed) {
                int currentArmor;
                if (entity instanceof BattleArmor){
                    currentArmor = entity.getArmor(loc);
                } else {
                    currentArmor = entity.getArmorForReal(loc);
                }
                if (entity.getOArmor(loc) != currentArmor) {
                    thisLoc.append(indentStr(indentLvl+1) + "<armor points=\"");
                    thisLoc.append(EntityListFile.formatArmor(entity
                            .getArmorForReal(loc)));
                    thisLoc.append("\"/>");
                    thisLoc.append(CommonConstants.NL);
                }
                if (entity.getOInternal(loc) != entity.getInternalForReal(loc)) {
                    thisLoc.append(indentStr(indentLvl+1) + "<armor points=\"");
                    thisLoc.append(EntityListFile.formatArmor(entity
                            .getInternalForReal(loc)));
                    thisLoc.append("\" type=\"Internal\"/>");
                    thisLoc.append(CommonConstants.NL);
                }
                if (entity.hasRearArmor(loc)
                        && (entity.getOArmor(loc, true) != entity
                                .getArmorForReal(loc, true))) {
                    thisLoc.append(indentStr(indentLvl+1) + "<armor points=\"");
                    thisLoc.append(EntityListFile.formatArmor(entity
                            .getArmorForReal(loc, true)));
                    thisLoc.append("\" type=\"Rear\"/>");
                    thisLoc.append(CommonConstants.NL);
                }
                if (entity.getLocationStatus(loc) == ILocationExposureStatus.BREACHED) {
                    thisLoc.append(indentStr(indentLvl+1) + "<breached/>");
                    thisLoc.append(CommonConstants.NL);
                }
                if (blownOff) {
                    thisLoc.append(indentStr(indentLvl+1) + "<blownOff/>");
                    thisLoc.append(CommonConstants.NL);
                }
            }

            // Walk through the slots in this location.
            for (int loop = 0; loop < entity.getNumberOfCriticals(loc); loop++) {

                // Get this slot.
                CriticalSlot slot = entity.getCritical(loc, loop);

                // Did we get a slot?
                if (null == slot) {

                    // Nope. Record missing actuators on Biped Mechs.
                    if (isMech
                            && !entity.entityIsQuad()
                            && ((loc == Mech.LOC_RARM) || (loc == Mech.LOC_LARM))
                            && ((loop == 2) || (loop == 3))) {
                        thisLoc.append(indentStr(indentLvl+1) + "<slot index=\"");
                        thisLoc.append(String.valueOf(loop + 1));
                        thisLoc.append("\" type=\"Empty\"/>");
                        thisLoc.append(CommonConstants.NL);
                        haveSlot = true;
                    }

                } else {

                    // Yup. If the equipment isn't a system, get it.
                    Mounted mount = null;
                    if (CriticalSlot.TYPE_EQUIPMENT == slot.getType()) {
                        mount = slot.getMount();
                    }

                    if (mount != null && mount.getType() instanceof BombType) {
                        continue;
                    }

                    // Destroyed locations on Mechs that contain slots
                    // that are missing but not hit or destroyed must
                    // have been blown off.
                    if (!isDestroyed && isMech && slot.isMissing()
                            && !slot.isHit() && !slot.isDestroyed()) {
                        thisLoc.append(EntityListFile.formatSlot(
                                String.valueOf(loop + 1), mount, slot.isHit(),
                                slot.isDestroyed(), slot.isRepairable(),
                                slot.isMissing(), indentLvl+1));
                        haveSlot = true;
                    }

                    // Record damaged slots in undestroyed locations.
                    else if (!isDestroyed && slot.isDamaged()) {
                        thisLoc.append(EntityListFile.formatSlot(
                                String.valueOf(loop + 1), mount, slot.isHit(),
                                slot.isDestroyed(), slot.isRepairable(),
                                slot.isMissing(), indentLvl+1));
                        haveSlot = true;
                    }

                    // record any quirks
                    else if ((null != mount) && (mount.countQuirks() > 0)) {
                        thisLoc.append(EntityListFile.formatSlot(
                                String.valueOf(loop + 1), mount, slot.isHit(),
                                slot.isDestroyed(), slot.isRepairable(),
                                slot.isMissing(), indentLvl+1));
                        haveSlot = true;
                    }

                    // Record Rapid Fire Machine Guns
                    else if ((mount != null) && (mount.isRapidfire())) {
                        thisLoc.append(EntityListFile.formatSlot(
                                String.valueOf(loop + 1), mount, slot.isHit(),
                                slot.isDestroyed(), slot.isRepairable(),
                                slot.isMissing(), indentLvl+1));
                        haveSlot = true;
                    }

                    // Record ammunition slots in undestroyed locations.
                    // N.B. the slot CAN\"T be damaged at this point.
                    else if (!isDestroyed && (mount != null)
                            && (mount.getType() instanceof AmmoType)) {
                        thisLoc.append(indentStr(indentLvl+1) + "<slot index=\"");
                        thisLoc.append(String.valueOf(loop + 1));
                        thisLoc.append("\" type=\"");
                        thisLoc.append(mount.getType().getInternalName());
                        thisLoc.append("\" shots=\"");
                        thisLoc.append(String.valueOf(mount.getBaseShotsLeft()));
                        thisLoc.append("\"/>");
                        thisLoc.append(CommonConstants.NL);
                        haveSlot = true;
                    }

                    // Record the munition type of oneshot launchers
                    else if (!isDestroyed && (mount != null)
                            && (mount.getType() instanceof WeaponType)
                            && (mount.getType()).hasFlag(WeaponType.F_ONESHOT)) {
                        thisLoc.append(EntityListFile.formatSlot(
                                String.valueOf(loop + 1), mount, slot.isHit(),
                                slot.isDestroyed(), slot.isRepairable(),
                                slot.isMissing(), indentLvl+1));
                        haveSlot = true;
                    }

                    // Record trooper missing equipment on BattleArmor
                    else if(null != mount && mount.isAnyMissingTroopers()) {
                        thisLoc.append(EntityListFile.formatSlot(
                                String.valueOf(loop + 1), mount, slot.isHit(),
                                slot.isDestroyed(), slot.isRepairable(),
                                slot.isMissing(), indentLvl+1));
                        haveSlot = true;
                    }

                } // End have-slot

            } // Check the next slot in this location

            // Stabilizer hit
            if ((entity instanceof Tank)
                    && ((Tank) entity).isStabiliserHit(loc)) {
                thisLoc.append(indentStr(indentLvl+1) + "<stabilizer isHit=\"true\"/>\n");
            }

            // Protomechs only have system slots,
            //  so we have to handle the ammo specially.
            if (entity instanceof Protomech) {
                for (Mounted mount : entity.getAmmo()) {
                    // Is this ammo in the current location?
                    if (mount.getLocation() == loc) {
                        thisLoc.append(EntityListFile.formatSlot("N/A", mount,
                                false, false, false, false, indentLvl+1));
                        haveSlot = true;
                    }
                } // Check the next ammo.
                // TODO: handle slotless equipment.
            } // End is-proto

            // Did we record information for this location?
            if (thisLoc.length() > 0) {

                // Add this location to the output string.
                output.append(indentStr(indentLvl) + "<location index=\"");
                output.append(String.valueOf(loc));
                if (isDestroyed) {
                    output.append("\" isDestroyed=\"true");
                }
                output.append("\"> ");
                output.append(entity.getLocationName(loc));
                if (blownOff) {
                    output.append(" has been blown off.");
                }
                output.append(CommonConstants.NL);
                output.append(thisLoc.toString());
                output.append(indentStr(indentLvl) + "</location>");
                output.append(CommonConstants.NL);

                // Reset the location buffer.
                thisLoc = new StringBuffer();
                blownOff = false;

            } // End output-location

            // If the location is completely destroyed, log it anyway.
            else if (isDestroyed) {

                // Add this location to the output string.
                output.append(indentStr(indentLvl) + "<location index=\"");
                output.append(String.valueOf(loc));
                output.append("\" isDestroyed=\"true\" /> ");
                output.append(entity.getLocationName(loc));
                output.append(CommonConstants.NL);

            } // End location-completely-destroyed

            // Reset the "location is destroyed" flag.
            isDestroyed = false;

        } // Handle the next location

        // If there is no location string, return a null.
        if (output.length() == 0) {
            return null;
        }

        // If we recorded a slot, remind the player that slots start at 1.
        if (haveSlot) {
            output.insert(0, CommonConstants.NL);
            output.insert(0,
                    "      The first slot in a location is at index=\"1\".");

            // Tanks do wierd things with ammo.
            if (entity instanceof Tank) {
                output.insert(0, CommonConstants.NL);
                output.insert(0,
                        "      Tanks have special needs, so don't delete any ammo slots.");
            }
        }

        // Convert the output into a String and return it.
        return output.toString();

    } // End private static String getLocString( Entity )

    /**
     * Save the <code>Entity</code>s in the list to the given file.
     * <p/>
     * The <code>Entity</code>s\" pilots, damage, ammo loads, ammo usage, and
     * other campaign-related information are retained but data specific to a
     * particular game is ignored.
     *
     * @param file
     *            - The current contents of the file will be discarded and all
     *            <code>Entity</code>s in the list will be written to the file.
     * @param list
     *            - a <code>Vector</code> containing <code>Entity</code>s to be
     *            stored in a file.
     * @throws IOException
     *             is thrown on any error.
     */
    public static void saveTo(File file, ArrayList<Entity> list)
            throws IOException {

        // Open up the file. Produce UTF-8 output.
        Writer output = new BufferedWriter(new OutputStreamWriter(
                new FileOutputStream(file), "UTF-8"));

        // Output the doctype and header stuff.
        output.write("<?xml version=\"1.0\" encoding=\"UTF-8\"?>");
        output.write(CommonConstants.NL);
        output.write(CommonConstants.NL);
        output.write("<unit version=\"" + MegaMek.VERSION + "\" >");
        output.write(CommonConstants.NL);
        output.write(CommonConstants.NL);

        try {
            writeEntityList(output, list);
        } catch(IOException exception) {
            throw exception;
        }


        // Finish writing.
        output.write("</unit>");
        output.write(CommonConstants.NL);
        output.flush();
        output.close();
    }

    /**
     * Save the entities from the game of client to the given file. This will create
     * separate sections for salvage, devastated, and ejected crews in addition
     * to the surviving units
     * <p/>
     * The <code>Entity</code>s\" pilots, damage, ammo loads, ammo usage, and
     * other campaign-related information are retained but data specific to a
     * particular game is ignored.
     *
     * @param file
     *            - The current contents of the file will be discarded and all
     *            <code>Entity</code>s in the list will be written to the file.
     * @param client
     *            - a <code>Client</code> containing the <code>Game</code>s to be used
     * @throws IOException
     *             is thrown on any error.
     */
    public static void saveTo(File file, Client client)
            throws IOException {

        if(null == client.getGame()) {
            return;
        }

        // Open up the file. Produce UTF-8 output.
        Writer output = new BufferedWriter(new OutputStreamWriter(
                new FileOutputStream(file), "UTF-8"));

        // Output the doctype and header stuff.
        output.write("<?xml version=\"1.0\" encoding=\"UTF-8\"?>");
        output.write(CommonConstants.NL);
        output.write(CommonConstants.NL);
        output.write("<record version=\"" + MegaMek.VERSION + "\" >");

        // Make a list of the player's surviving units
        ArrayList<Entity> living = client.getGame().getPlayerEntities(client.getLocalPlayer(), false);

        // Be sure to include all units that have retreated.
        for (Enumeration<Entity> iter = client.getGame().getRetreatedEntities(); iter.hasMoreElements(); ) {
            Entity ent = iter.nextElement();
            if (ent.getOwnerId() == client.getLocalPlayer().getId()) {
                living.add(ent);
            }
        }

        // separate out salvage and devastated units and record kills if possible
        ArrayList<Entity> salvage = new ArrayList<Entity>();
        ArrayList<Entity> devastated = new ArrayList<Entity>();
        Hashtable<String, String> kills = new Hashtable<String, String>();

        //salvageable stuff
        Enumeration<Entity> graveyard = client.getGame().getGraveyardEntities();
        while (graveyard.hasMoreElements()) {
            Entity entity = graveyard.nextElement();
            if(entity.getOwner().isEnemyOf(client.getLocalPlayer())) {
                Entity killer = client.getGame().getEntityFromAllSources(entity.getKillerId());
                if(null != killer
                        && !killer.getExternalIdAsString().equals("-1")
                        && killer.getOwnerId() == client.getLocalPlayer().getId()) {
                    kills.put(entity.getDisplayName(), killer.getExternalIdAsString());
                } else {
                    kills.put(entity.getDisplayName(), "None");
                }
            }
            salvage.add(entity);
        }

        //look for surviving enemy entities and add them to the possible salvage
        Iterator<Entity> entities = client.getGame().getEntities();
        while(entities.hasNext()) {
            Entity entity = entities.next();
            if(entity.getOwner().isEnemyOf(client.getLocalPlayer())) {
                 if(!entity.canEscape()) {
                     kills.put(entity.getDisplayName(), "None");
                 }
                 salvage.add(entity);
            }
        }

        //devastated units
        Enumeration<Entity> devastation = client.getGame().getDevastatedEntities();
        while (devastation.hasMoreElements()) {
            Entity entity = devastation.nextElement();
            if(entity.getOwner().isEnemyOf(client.getLocalPlayer())) {
                Entity killer = client.getGame().getEntityFromAllSources(entity.getKillerId());
                if(null != killer
                        && !killer.getExternalIdAsString().equals("-1")
                        && killer.getOwnerId() == client.getLocalPlayer().getId()) {
                    kills.put(entity.getDisplayName(), killer.getExternalIdAsString());
                } else {
                    kills.put(entity.getDisplayName(), "None");
                }
            }
            devastated.add(entity);
        }

        if(!living.isEmpty()) {
            output.write(CommonConstants.NL);
            output.write(indentStr(1) + "<survivors>");
            output.write(CommonConstants.NL);
            output.write(CommonConstants.NL);
            try {
                writeEntityList(output, living);
            } catch(IOException exception) {
                throw exception;
            }
            // Finish writing.
            output.write(indentStr(1) + "</survivors>");
            output.write(CommonConstants.NL);
        }

        if(!salvage.isEmpty()) {
            output.write(CommonConstants.NL);
            output.write(indentStr(1) + "<salvage>");
            output.write(CommonConstants.NL);
            output.write(CommonConstants.NL);
            try {
                writeEntityList(output, salvage);
            } catch(IOException exception) {
                throw exception;
            }
            // Finish writing.
            output.write(indentStr(1) + "</salvage>");
            output.write(CommonConstants.NL);
        }

        if(!devastated.isEmpty()) {
            output.write(CommonConstants.NL);
            output.write(indentStr(1) + "<devastated>");
            output.write(CommonConstants.NL);
            output.write(CommonConstants.NL);
            try {
                writeEntityList(output, devastated);
            } catch(IOException exception) {
                throw exception;
            }
            // Finish writing.
            output.write(indentStr(1) + "</devastated>");
            output.write(CommonConstants.NL);
        }

        if(!kills.isEmpty()) {
            output.write(CommonConstants.NL);
            output.write(indentStr(1) + "<kills>");
            output.write(CommonConstants.NL);
            output.write(CommonConstants.NL);
            try {
                writeKills(output, kills);
            } catch(IOException exception) {
                throw exception;
            }
             // Finish writing.
            output.write(indentStr(1) + "</kills>");
            output.write(CommonConstants.NL);
        }

        // Finish writing.
        output.write("</record>");
        output.write(CommonConstants.NL);
        output.flush();
        output.close();
    }

    private static void writeKills(Writer output, Hashtable<String,String> kills) throws IOException {
        int indentLvl = 2;
        for(String killed : kills.keySet()) {
            output.write(indentStr(indentLvl) + "<kill killed=\"");
            output.write(killed.replaceAll("\"", "&quot;"));
            output.write("\" killer=\"");
            output.write(kills.get(killed));
            output.write("\"/>");
            output.write(CommonConstants.NL);
        }
    }

    private static void writeEntityList(Writer output, ArrayList<Entity> list) throws IOException {
        // Walk through the list of entities.
        Iterator<Entity> items = list.iterator();
        while (items.hasNext()) {
            final Entity entity = items.next();

            if (entity instanceof FighterSquadron) {
                continue;
            }
            int indentLvl = 2;

            // Start writing this entity to the file.
            output.write(indentStr(indentLvl) + "<entity chassis=\"");
            output.write(entity.getChassis().replaceAll("\"", "&quot;"));
            output.write("\" model=\"");
            output.write(entity.getModel().replaceAll("\"", "&quot;"));
            output.write("\" type=\"");
            output.write(entity.getMovementModeAsString());
            output.write("\" commander=\"");
            output.write(String.valueOf(entity.isCommander()));
            output.write("\" offboard=\"");
            output.write(String.valueOf(entity.isOffBoard()));
            if (entity.isOffBoard()) {
                output.write("\" offboard_distance=\"");
                output.write(String.valueOf(entity.getOffBoardDistance()));
                output.write("\" offboard_direction=\"");
                output.write(String.valueOf(entity.getOffBoardDirection()
                        .getValue()));
            }
            output.write("\" hidden=\"");
            output.write(String.valueOf(entity.isHidden()));
            output.write("\" deployment=\"");
            output.write(String.valueOf(entity.getDeployRound()));
            output.write("\" deploymentZone=\"");
            output.write(String.valueOf(entity.getStartingPos(false)));
            output.write("\" neverDeployed=\"");
            output.write(String.valueOf(entity.wasNeverDeployed()));
            if (entity instanceof Aero){
                output.write("\" velocity=\"");
                output.write(((Aero)entity).getCurrentVelocity() + "");
                output.write("\" altitude=\"");
                output.write(((Aero)entity).getAltitude() + "");
            }
            if (!entity.getExternalIdAsString().equals("-1")) {
                output.write("\" externalId=\"");
                output.write(entity.getExternalIdAsString());
            }
            if (entity.countQuirks() > 0) {
                output.write("\" quirks=\"");
                output.write(String.valueOf(entity.getQuirkList("::")));
            }
            if (entity.getC3Master() != null) {
                output.write("\" c3MasterIs=\"");
                output.write(entity.getGame()
                        .getEntity(entity.getC3Master().getId())
                        .getC3UUIDAsString());
            }
            if (entity.hasC3() || entity.hasC3i()) {
                output.write("\" c3UUID=\"");
                output.write(entity.getC3UUIDAsString());
            }
            if (null != entity.getCamoCategory()) {
                output.write("\" camoCategory=\"");
                output.write(entity.getCamoCategory());
            }
            if (null != entity.getCamoFileName()) {
                output.write("\" camoFileName=\"");
                output.write(entity.getCamoFileName());
            }
            if(entity instanceof MechWarrior && !((MechWarrior)entity).getPickedUpByExternalIdAsString().equals("-1")) {
                output.write("\" pickUpId=\"");
                output.write(((MechWarrior)entity).getPickedUpByExternalIdAsString());
            }
            output.write("\">");
            output.write(CommonConstants.NL);

            // Add the crew this entity.
            final Crew crew = entity.getCrew();
            output.write(indentStr(indentLvl+1) + "<pilot name=\"");
            output.write(crew.getName().replaceAll("\"", "&quot;"));
            output.write("\" size=\"");
            output.write(String.valueOf(crew.getSize()));
            output.write("\" nick=\"");
            output.write(crew.getNickname().replaceAll("\"", "&quot;"));
            output.write("\" gunnery=\"");
            output.write(String.valueOf(crew.getGunnery()));
            if ((null != entity.getGame())
                    && entity.getGame().getOptions()
                            .booleanOption("rpg_gunnery")) {
                output.write("\" gunneryL=\"");
                output.write(String.valueOf(crew.getGunneryL()));
                output.write("\" gunneryM=\"");
                output.write(String.valueOf(crew.getGunneryM()));
                output.write("\" gunneryB=\"");
                output.write(String.valueOf(crew.getGunneryB()));
            }
            output.write("\" piloting=\"");
            output.write(String.valueOf(crew.getPiloting()));
            if ((null != entity.getGame())
                    && entity.getGame().getOptions()
                            .booleanOption("artillery_skill")) {
                output.write("\" artillery=\"");
                output.write(String.valueOf(crew.getArtillery()));
            }
            if (crew.getToughness() != 0) {
                output.write("\" toughness=\"");
                output.write(String.valueOf(crew.getToughness()));
            }
            if (crew.getInitBonus() != 0) {
                output.write("\" initB=\"");
                output.write(String.valueOf(crew.getInitBonus()));
            }
            if (crew.getCommandBonus() != 0) {
                output.write("\" commandB=\"");
                output.write(String.valueOf(crew.getCommandBonus()));
            }
            if (crew.isDead() || (crew.getHits() > 5)) {
                output.write("\" hits=\"Dead");
            } else if (crew.getHits() > 0) {
                output.write("\" hits=\"");
                output.write(String.valueOf(crew.getHits()));
            }
            output.write("\" ejected=\"");
            output.write(String.valueOf(crew.isEjected()));
            if (crew.countOptions(PilotOptions.LVL3_ADVANTAGES) > 0) {
                output.write("\" advantages=\"");
                output.write(String.valueOf(crew.getOptionList("::",
                        PilotOptions.LVL3_ADVANTAGES)));
            }
            if (crew.countOptions(PilotOptions.EDGE_ADVANTAGES) > 0) {
                output.write("\" edge=\"");
                output.write(String.valueOf(crew.getOptionList("::",
                        PilotOptions.EDGE_ADVANTAGES)));
            }
            if (crew.countOptions(PilotOptions.MD_ADVANTAGES) > 0) {
                output.write("\" implants=\"");
                output.write(String.valueOf(crew.getOptionList("::",
                        PilotOptions.MD_ADVANTAGES)));
            }
            if (entity instanceof Mech) {
                if (((Mech) entity).isAutoEject()) {
                    output.write("\" autoeject=\"true");
                } else {
                    output.write("\" autoeject=\"false");
                }
                if (entity.game.getOptions().booleanOption(
                        "conditional_ejection")) {
                    if (((Mech) entity).isCondEjectAmmo()) {
                        output.write("\" condejectammo=\"true");
                    } else {
                        output.write("\" condejectammo=\"false");
                    }
                    if (((Mech) entity).isCondEjectEngine()) {
                        output.write("\" condejectengine=\"true");
                    } else {
                        output.write("\" condejectengine=\"false");
                    }
                    if (((Mech) entity).isCondEjectCTDest()) {
                        output.write("\" condejectctdest=\"true");
                    } else {
                        output.write("\" condejectctdest=\"false");
                    }
                    if (((Mech) entity).isCondEjectHeadshot()) {
                        output.write("\" condejectctheadshot=\"true");
                    } else {
                        output.write("\" condejectctheadshot=\"false");
                    }
                }
            }
            if (!Crew.ROOT_PORTRAIT.equals(crew.getPortraitCategory())) {
                output.write("\" portraitCat=\"");
                output.write(crew.getPortraitCategory());
            }
            if (!Crew.PORTRAIT_NONE.equals(crew.getPortraitFileName())) {
                output.write("\" portraitFile=\"");
                output.write(crew.getPortraitFileName());
            }
            if (!crew.getExternalIdAsString().equals("-1")) {
                output.write("\" externalId=\"");
                output.write(crew.getExternalIdAsString());
            }
            output.write("\"/>");
            output.write(CommonConstants.NL);

            // If it's a tank, add a movement tag.
            if (entity instanceof Tank) {
                Tank tentity = (Tank) entity;
                output.write(EntityListFile.getMovementString(tentity));
                if (tentity.isTurretLocked(tentity.getLocTurret())) {
                    output.write(EntityListFile.getTurretLockedString(tentity));
                }
                // crits
                output.write(EntityListFile.getTankCritString(tentity));
            }

            // add a bunch of stuff for aeros
            if (entity instanceof Aero) {
                Aero a = (Aero) entity;

                // SI
                output.write(indentStr(indentLvl+1) + "<structural integrity=\"");
                output.write(String.valueOf(a.getSI()));
                output.write("\"/>");
                output.write(CommonConstants.NL);

                // heat sinks
                output.write(indentStr(indentLvl+1) + "<heat sinks=\"");
                output.write(String.valueOf(a.getHeatSinks()));
                output.write("\"/>");
                output.write(CommonConstants.NL);

                // fuel
                output.write(indentStr(indentLvl+1) + "<fuel left=\"");
                output.write(String.valueOf(a.getFuel()));
                output.write("\"/>");
                output.write(CommonConstants.NL);

                // Write the Bomb Data if needed
                int[] bombChoices = new int[BombType.B_NUM];
                bombChoices = a.getBombChoices();
                if (bombChoices.length > 0) {
                    output.write(indentStr(indentLvl+1) + "<bombs>");
                    output.write(CommonConstants.NL);
                    for (int type = 0; type < BombType.B_NUM; type++) {
                        String typeName = BombType.getBombInternalName(type);
                        if (bombChoices[type] > 0) {
                            output.write(indentStr(indentLvl+2) + "<bomb type=\"");
                            output.write(typeName);
                            output.write("\" load=\"");
                            output.write(String.valueOf(bombChoices[type]));
                            output.write("\"/>");
                            output.write(CommonConstants.NL);
                        }
                    }
                    for (Mounted m : a.getBombs()) {
                        if (!(m.getType() instanceof BombType)) {
                            continue;
                        }
                        output.write(indentStr(indentLvl+2) + "<bomb type=\"");
                        output.write(m.getType().getShortName());
                        output.write("\" load=\"");
                        output.write(String.valueOf(m.getBaseShotsLeft()));
                        output.write("\"/>");
                        output.write(CommonConstants.NL);
                    }
                    output.write(indentStr(indentLvl+1) + "</bombs>");
                    output.write(CommonConstants.NL);
                }

                // TODO: dropship docking collars, bays

                // large craft stuff
                if (a instanceof Jumpship) {
                    Jumpship j = (Jumpship) a;

                    // kf integrity
                    output.write(indentStr(indentLvl+1) + "<KF integrity=\"");
                    output.write(String.valueOf(j.getKFIntegrity()));
                    output.write("\"/>");
                    output.write(CommonConstants.NL);

                    // kf sail integrity
                    output.write(indentStr(indentLvl+1) + "<sail integrity=\"");
                    output.write(String.valueOf(j.getSailIntegrity()));
                    output.write("\"/>");
                    output.write(CommonConstants.NL);
                }

                // crits
                output.write(EntityListFile.getAeroCritString(a));

            }

            if (entity instanceof BattleArmor) {
                BattleArmor ba = (BattleArmor) entity;
                for (Mounted m : entity.getEquipment()){
                    if (m.getType().hasFlag(MiscType.F_BA_MEA)){
                        Mounted manipulator = null;
                        if (m.getBaMountLoc() == BattleArmor.MOUNT_LOC_LARM){
                            manipulator = ba.getLeftManipulator();
                        } else if (m.getBaMountLoc()
                                == BattleArmor.MOUNT_LOC_RARM){
                            manipulator = ba.getRightManipulator();
                        }
                        output.write(indentStr(indentLvl+1) + "<modularEquipmentMount ");
                        output.write("baMEAMountLoc=\"" + m.getBaMountLoc()
                                + "\" ");
                        if (manipulator != null){
                            output.write("baMEATypeName=\""
                                    + manipulator.getType().getInternalName()
                                    + "\" ");
                        }
                        output.write("/>");
                        output.write(CommonConstants.NL);
                    } else if (m.getType().hasFlag(MiscType.F_AP_MOUNT)){
                        int mountIdx = entity.getEquipmentNum(m);
                        EquipmentType apType = null;
                        if (m.getLinked() != null){
                            apType = m.getLinked().getType();
                        }
                        output.write(indentStr(indentLvl+1) + "<antiPersonnelMount ");
                        output.write("baAPMMountNum=\"" + mountIdx + "\" ");
                        if (apType != null){
                            output.write("baAPMTypeName=\""
                                    + apType.getInternalName() + "\" ");
                        }
                        output.write("/>");
                        output.write(CommonConstants.NL);
                    }
                }
            }

            // Add the locations of this entity (if any are needed).
            String loc = EntityListFile.getLocString(entity, indentLvl+1);
            if (null != loc) {
                output.write(loc);
            }

            // Write the C3i Data if needed
            if (entity.hasC3i()) {
                output.write(indentStr(indentLvl+1) + "<c3iset>");
                output.write(CommonConstants.NL);
                Iterator<Entity> c3iList = list.iterator();
                while (c3iList.hasNext()) {
                    final Entity C3iEntity = c3iList.next();

                    if (C3iEntity.onSameC3NetworkAs(entity, true)) {
                        output.write(indentStr(indentLvl+1) + "<c3i_link link=\"");
                        output.write(C3iEntity.getC3UUIDAsString());
                        output.write("\"/>");
                        output.write(CommonConstants.NL);
                    }
                }
                output.write(indentStr(indentLvl+1) + "</c3iset>");
                output.write(CommonConstants.NL);
            }

            // Finish writing this entity to the file.
            output.write(indentStr(indentLvl) + "</entity>");
            output.write(CommonConstants.NL);
            output.write(CommonConstants.NL);

        } // Handle the next entity
    }

    private static String getTurretLockedString(Tank e) {
        String retval = "      <turretlock direction=\"";
        retval = retval.concat(Integer.toString(e.getSecondaryFacing()));
        retval = retval.concat("\"/>\n");
        return retval;
    }

    private static String getMovementString(Tank e) {
        String retVal = "      <motive damage=\"";
        retVal = retVal.concat(Integer.toString(e.getMotiveDamage()));
        retVal = retVal.concat("\" penalty=\"");
        retVal = retVal.concat(Integer.toString(e.getMotivePenalty()));
        retVal = retVal.concat("\"/>\n");
        return retVal;
    }

    // Aero crits
    private static String getAeroCritString(Aero a) {

        String retVal = "      <acriticals";
        String critVal = "";

        // crits
        if (a.getAvionicsHits() > 0) {
            critVal = critVal.concat(" avionics=\"");
            critVal = critVal.concat(Integer.toString(a.getAvionicsHits()));
            critVal = critVal.concat("\"");
        }
        if (a.getSensorHits() > 0) {
            critVal = critVal.concat(" sensors=\"");
            critVal = critVal.concat(Integer.toString(a.getSensorHits()));
            critVal = critVal.concat("\"");
        }
        if (a.getEngineHits() > 0) {
            critVal = critVal.concat(" engine=\"");
            critVal = critVal.concat(Integer.toString(a.getEngineHits()));
            critVal = critVal.concat("\"");
        }
        if (a.getFCSHits() > 0) {
            critVal = critVal.concat(" fcs=\"");
            critVal = critVal.concat(Integer.toString(a.getFCSHits()));
            critVal = critVal.concat("\"");
        }
        if (a.getCICHits() > 0) {
            critVal = critVal.concat(" cic=\"");
            critVal = critVal.concat(Integer.toString(a.getCICHits()));
            critVal = critVal.concat("\"");
        }
        if (a.getLeftThrustHits() > 0) {
            critVal = critVal.concat(" leftThrust=\"");
            critVal = critVal.concat(Integer.toString(a.getLeftThrustHits()));
            critVal = critVal.concat("\"");
        }
        if (a.getRightThrustHits() > 0) {
            critVal = critVal.concat(" rightThrust=\"");
            critVal = critVal.concat(Integer.toString(a.getRightThrustHits()));
            critVal = critVal.concat("\"");
        }
        if (!a.hasLifeSupport()) {
            critVal = critVal.concat(" lifeSupport=\"none\"");
        }
        if (a.isGearHit()) {
            critVal = critVal.concat(" gear=\"none\"");
        }

        if (!critVal.equals("")) {
            // then add beginning and end
            retVal = retVal.concat(critVal);
            retVal = retVal.concat("/>\n");
        } else {
            return critVal;
        }

        return retVal;

    }

    // Aero crits
    private static String getTankCritString(Tank t) {

        String retVal = "      <tcriticals";
        String critVal = "";

        // crits
        if (t.getSensorHits() > 0) {
            critVal = critVal.concat(" sensors=\"");
            critVal = critVal.concat(Integer.toString(t.getSensorHits()));
            critVal = critVal.concat("\"");
        }
        if (t.isEngineHit()) {
            critVal = critVal.concat(" engine=\"");
            critVal = critVal.concat("hit");
            critVal = critVal.concat("\"");
        }

        if (t.isDriverHit()) {
            critVal = critVal.concat(" driver=\"");
            critVal = critVal.concat("hit");
            critVal = critVal.concat("\"");
        }

        if (t.isCommanderHit()) {
            critVal = critVal.concat(" commander=\"");
            critVal = critVal.concat("hit");
            critVal = critVal.concat("\"");
        }

        if (!critVal.equals("")) {
            // then add beginning and end
            retVal = retVal.concat(critVal);
            retVal = retVal.concat("/>\n");
        } else {
            return critVal;
        }

        return retVal;

    }

    /**
     * Load a list of <code>Entity</code>s from the given file.
     * <p/>
     * The <code>Entity</code>s\" pilots, damage, ammo loads, ammo usage, and
     * other campaign-related information are retained but data specific to a
     * particular game is ignored.
     *
     * @param file
     *            - the <code>File</code> to load from.
     * @return A <code>Vector</code> containing <code>Entity</code>s loaded from
     *         the file. This vector may be empty, but it will not be
     *         <code>null</code>.
     * @throws IOException
     *             is thrown on any error.
     */
    public static Vector<Entity> loadFrom(File file) throws IOException {

        // Create an empty parser.
        MULParser parser = new MULParser();

        // Open up the file.
        InputStream listStream = new FileInputStream(file);

        // Read a Vector from the file.
        parser.parse(listStream);
        listStream.close();


        // Was there any error in parsing?
        if (parser.hasWarningMessage()) {
            System.out.println(parser.getWarningMessage());
        }

        // Return the entities.
        return parser.getEntities();
    }

}

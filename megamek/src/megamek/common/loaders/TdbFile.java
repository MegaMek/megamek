/*
 * MegaMek - Copyright (C) 2000-2004 Ben Mazur (bmazur@sev.org)
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

/*
 * TdbFile.java
 *  -based on MtfFile.java, modifications by Ryan McConnell
 * Created on April 1, 2003, 2:48 PM
 */

package megamek.common.loaders;

import java.io.*;
import java.util.Hashtable;
import java.util.Vector;
import java.util.Enumeration;

import megamek.common.BipedMech;
import megamek.common.CriticalSlot;
import megamek.common.Entity;
import megamek.common.EquipmentType;
import megamek.common.LocationFullException;
import megamek.common.Mech;
import megamek.common.Mounted;
import megamek.common.QuadMech;
import megamek.common.TechConstants;

import gd.xml.*;
import gd.xml.tiny.*;

public class TdbFile implements MechLoader {

    private ParsedXML root = null;

    /**
     * The names of the various elements recognized by this parser.
     */
    private static final String  CREATOR_SECTION    = "creator";
    private static final String  BASICS_SECTION    = "basics";
    private static final String  ITEM_DEFS_SECTION    = "itemdefs";
    private static final String  MOUNTED_ITEMS_SECTION    = "mounteditems";
    private static final String  CRIT_DEFS_SECTION    = "critdefs";

    private static final String  NAME    = "name";
    private static final String  VERSION    = "version";
    private static final String  MODEL    = "model";
    private static final String  VARIANT    = "variant";
    private static final String  TECHNOLOGY    = "technology";
    private static final String  MOVEMECHMOD    = "movemechmod";
    private static final String  TONNAGE    = "tonnage"; // also attribute
    private static final String  TYPE    = "type";
    private static final String  OMNI    = "isomni";
    private static final String  WALK   = "walk";
    private static final String  JUMP    = "jump";
    private static final String  HEAT_SINKS    = "heatsinks";
    private static final String  ARMOR    = "armor"; // also attribute
    private static final String  MOUNTED_ITEM    = "mounteditem";
    private static final String  LOCATION= "location";

    /**
     * The names of the attributes recognized by this parser.
     */
    private static final String  LEVEL    = "level";
    private static final String  COUNT    = "count";
    private static final String  POINTS    = "points";
    private static final String  REAR_MOUNTED    = "rearmounted";
    private static final String  IS_SPREAD    = "isspread";
    private static final String  ITEM_INDEX    = "itemindex";
    private static final String  REAR_ARMOR    = "reararmor";

    /**
     * Special values recognized by this parser.
     */
    private static final String  TRUE    = "True";
    private static final String  FALSE   = "False";
    private static final String  DOUBLE   = "Double";
    private static final String  TRUE_LOWER    = "true";

    private String creatorName = "Unknown";
    private String creatorVersion = "Unknown";
    private String name;
    private boolean isOmni = false;
    private String model;
    private String variant;

    private String chassisConfig;
    private String techBase;
    private static final String techYear = "3068"; // TDB doesn't have era
    private String rulesLevel;
    private String LAMTonnage;

    private String tonnage;

    private String heatSinks;
    private boolean dblSinks;
    private String walkMP;
    private String jumpMP;

    private int larmArmor;
    private int rarmArmor;
    private int ltArmor;
    private int rtArmor;
    private int ctArmor;
    private int headArmor;
    private int llegArmor;
    private int rlegArmor;
    private int ltrArmor;
    private int rtrArmor;
    private int ctrArmor;

    private String[][][] critData;
    private boolean isRearMounted[];
    private boolean isSplit[];

    private Hashtable hSharedEquip = new Hashtable();
    private Vector vSplitWeapons = new Vector();

    /** Creates new TdbFile */
    public TdbFile(InputStream is) throws EntityLoadingException {
        try {
            root = TinyParser.parseXML(is);
        }
        catch (ParseException e) {
            throw new EntityLoadingException("   Failure to parse XML (TinyParser exception)");
        }
        // Arbitrarily sized static arrays suck, or so a computer
        //  science teacher once told me.
        isRearMounted = new boolean[256];
        isSplit = new boolean[256];

        critData = new String[8][12][2];
        parseNode((ParsedXML)root.elements().nextElement());
    }

    private void parseNode(ParsedXML node) throws EntityLoadingException {
        if (!node.getTypeName().equals("tag")) {
            // We only want to parse element nodes, text nodes
            //  are implicitly parsed when needed.
            return;
        }

        Enumeration children = node.elements();

        if (node.getName().equals(CREATOR_SECTION)) {
            parseCreatorNode(node);
        } else if (node.getName().equals(BASICS_SECTION)) {
            parseBasicsNode(node);
        } else if (node.getName().equals(ITEM_DEFS_SECTION)) {
            return; // don't need item defs section of xml
        } else if (node.getName().equals(MOUNTED_ITEMS_SECTION)) {
            parseMountedNode(node);
        } else if (node.getName().equals(CRIT_DEFS_SECTION)) {
            parseCritNode(node);
        } else if (children != null) {
            // Use recursion to process all the children
            while (children.hasMoreElements()) {
                parseNode((ParsedXML)children.nextElement());
            }
        }
    }

    private void parseCreatorNode(ParsedXML node) throws EntityLoadingException {
        if (!node.getTypeName().equals("tag")) {
            // We only want to parse element nodes, text nodes
            //  are directly parsed below.
            return;
        }

        Enumeration children = node.elements();

        if (node.getName().equals(NAME)) {
            creatorName = ((ParsedXML)children.nextElement()).getContent();
        } else if (node.getName().equals(VERSION)) {
            creatorVersion = ((ParsedXML)children.nextElement()).getContent();
        } else if (children != null) {
            // Use recursion to process all the children
            while (children.hasMoreElements()) {
                parseCreatorNode((ParsedXML)children.nextElement());
            }
        }
        // Other tags (that don't match any if blocks above)
        //  are simply ignored.
    }

    private void parseBasicsNode(ParsedXML node) throws EntityLoadingException {
        if (!node.getTypeName().equals("tag")) {
            // We only want to parse element nodes, text nodes
            //  are directly parsed below.
            return;
        }

        Enumeration children = node.elements();

        if (node.getName().equals(NAME)) {
            name = ((ParsedXML)children.nextElement()).getContent();
        } else if (node.getName().equals(MODEL)) {
            model = ((ParsedXML)children.nextElement()).getContent();
        } else if (node.getName().equals(OMNI)) {
            isOmni = ((ParsedXML)children.nextElement()).getContent().equals(TRUE_LOWER);
        } else if (node.getName().equals(VARIANT)) {
            variant = ((ParsedXML)children.nextElement()).getContent();
        } else if (node.getName().equals(TECHNOLOGY)) {
            techBase = ((ParsedXML)children.nextElement()).getContent();
            rulesLevel = node.getAttribute(LEVEL);
        } else if (node.getName().equals(TONNAGE)) {
            tonnage = ((ParsedXML)children.nextElement()).getContent();
        } else if (node.getName().equals(TYPE)) {
            chassisConfig = ((ParsedXML)children.nextElement()).getContent();
        } else if (node.getName().equals(MOVEMECHMOD)) {
            // This tag seems to indicate the pod space on an omnimech
            //  or the tonnage of the conversion equipment for
            //  a LAM (Land Air Mech).
            LAMTonnage = node.getAttribute(TONNAGE);
        } else if (node.getName().equals(WALK)) {
            walkMP = ((ParsedXML)children.nextElement()).getContent();
        } else if (node.getName().equals(JUMP)) {
            jumpMP = ((ParsedXML)children.nextElement()).getContent();
        } else if (node.getName().equals(HEAT_SINKS)) {
            if (((ParsedXML)children.nextElement()).getContent().indexOf(DOUBLE) != -1) {
                dblSinks = true;
            } else {
                dblSinks = false;
            }
            heatSinks = node.getAttribute(COUNT);
        } else if (children != null) {
            // Use recursion to process all the children
            while (children.hasMoreElements()) {
                parseBasicsNode((ParsedXML)children.nextElement());
            }
        }
        // Other tags (that don't match any if blocks above)
        //  are simply ignored.
    }

    private void parseMountedNode(ParsedXML node) throws EntityLoadingException {
        if (!node.getTypeName().equals("tag")) {
            // We only want to parse element nodes, text nodes
            //  are directly parsed below.
            return;
        }

        Enumeration children = node.elements();

        if (node.getName().equals(MOUNTED_ITEM)) {
            if (node.getAttribute(REAR_MOUNTED).equals(TRUE)) {
                isRearMounted[Integer.parseInt(node.getAttribute(ITEM_INDEX))] = true;
            } else {
                isRearMounted[Integer.parseInt(node.getAttribute(ITEM_INDEX))] = false;
            }
            if (node.getAttribute(IS_SPREAD).equals(TRUE)) {
                isSplit[Integer.parseInt(node.getAttribute(ITEM_INDEX))] = true;
            } else {
                isSplit[Integer.parseInt(node.getAttribute(ITEM_INDEX))] = false;
            }
        } else if (children != null) {
            // Use recursion to process all the children
            while (children.hasMoreElements()) {
                parseMountedNode((ParsedXML)children.nextElement());
            }
        }
        // Other tags (that don't match any if blocks above)
        //  are simply ignored.
    }

    private void parseCritNode(ParsedXML node) throws EntityLoadingException {
        if (!node.getTypeName().equals("tag")) {
            // We only want to parse element nodes, text nodes
            //  are directly parsed below.
            return;
        }

        Enumeration children = node.elements();

        if (node.getName().equals(LOCATION)) {
            int loc = -1;
            int i = 0;
            int armor = -1;
            int rearArmor = -1;
            if (node.getAttribute(ARMOR) != null) {
                armor = Integer.parseInt(node.getAttribute(ARMOR));
            }
            if (node.getAttribute(REAR_ARMOR) != null) {
                rearArmor = Integer.parseInt(node.getAttribute(REAR_ARMOR));
            }
            if (node.getAttribute(NAME).equals("LA") || node.getAttribute(NAME).equals("FLL")) {
                loc = Mech.LOC_LARM;
                larmArmor = armor;
            } else if (node.getAttribute(NAME).equals("RA") || node.getAttribute(NAME).equals("FRL")) {
                loc = Mech.LOC_RARM;
                rarmArmor = armor;
            } else if (node.getAttribute(NAME).equals("LT")) {
                loc = Mech.LOC_LT;
                ltArmor = armor;
                ltrArmor = rearArmor;
            } else if (node.getAttribute(NAME).equals("RT")) {
                loc = Mech.LOC_RT;
                rtArmor = armor;
                rtrArmor = rearArmor;
            } else if (node.getAttribute(NAME).equals("CT")) {
                loc = Mech.LOC_CT;
                ctArmor = armor;
                ctrArmor = rearArmor;
            } else if (node.getAttribute(NAME).equals("H")) {
                loc = Mech.LOC_HEAD;
                headArmor = armor;
            } else if (node.getAttribute(NAME).equals("LL") || node.getAttribute(NAME).equals("RLL")) {
                loc = Mech.LOC_LLEG;
                llegArmor = armor;
            } else if (node.getAttribute(NAME).equals("RL") || node.getAttribute(NAME).equals("RRL")) {
                loc = Mech.LOC_RLEG;
                rlegArmor = armor;
            }

            if (loc == -1) {
                throw new EntityLoadingException("   Bad Mech location: " + node.getAttribute(NAME));
            }
            while (children.hasMoreElements()) {
                ParsedXML critSlotNode = (ParsedXML)children.nextElement();
                critData[loc][i][0] = ((ParsedXML)critSlotNode.elements().nextElement()).getContent();
                critData[loc][i++][1] = critSlotNode.getAttribute(ITEM_INDEX);
            }
        } else if (children != null) {
            // Use recursion to process all the children
            while (children.hasMoreElements()) {
                parseCritNode((ParsedXML)children.nextElement());
            }
        }
        // Other tags (that don't match any if blocks above)
        //  are simply ignored.
    }

    public Entity getEntity() throws EntityLoadingException {
        try {
            Mech mech;

            if (creatorName == "Unknown"
                || !creatorName.equals("The Drawing Board")
                || Integer.parseInt(creatorVersion) != 2) {
                // MegaMek no longer supports older versions of The
                //  Drawing Board (pre 2.0.23) due to incomplete xml
                //  file information in those versions.
                throw new EntityLoadingException("This xml file is not a valid Drawing Board mech.  Make sure you are using version 2.0.23 or later of The Drawing Board.");
            }

            if (chassisConfig.equals("Quad")) {
                mech = new QuadMech();
            } else {
                mech = new BipedMech();
            }

            // aarg!  those stupid sub-names in parenthesis screw everything up
            // we may do something different in the future, but for now, I'm
            // going to strip them out
            int pindex = name.indexOf("(");
            if (pindex == -1) {
                mech.setChassis(name);
            } else {
                mech.setChassis(name.substring(0, pindex - 1));
            }

            if (variant != null) {
                mech.setModel(variant);
            } else if (model != null) {
                mech.setModel(model);
            } else {
                // Do mechs need a model?
                mech.setModel("");
            }
            mech.setYear(Integer.parseInt(techYear));
            mech.setOmni(isOmni);

            if (LAMTonnage != null) {
                throw new EntityLoadingException("Unsupported tech: LAM?");
            }
            if (techBase.equals("Inner Sphere")) {
                switch (Integer.parseInt(rulesLevel)) {
                case 1 :
                    mech.setTechLevel(TechConstants.T_IS_LEVEL_1);
                    break;
                case 2 :
                    mech.setTechLevel(TechConstants.T_IS_LEVEL_2);
                    break;
                default :
                    throw new EntityLoadingException("Unsupported tech level: " + rulesLevel);
                }
            } else if (techBase.equals("Clan")) {
                if (Integer.parseInt(rulesLevel) == 2)
                    mech.setTechLevel(TechConstants.T_CLAN_LEVEL_2);
                else
                    throw new EntityLoadingException("Unsupported tech level: " + rulesLevel);
            } else if (techBase.equals("Mixed (IS Chassis)") ||
                       techBase.equals("Inner Sphere 'C'")) {
                mech.setTechLevel(TechConstants.T_MIXED_BASE_IS_LEVEL_2);
            } else if (techBase.equals("Mixed (Clan Chassis)")) {
                mech.setTechLevel(TechConstants.T_MIXED_BASE_CLAN_LEVEL_2);
            } else {
                throw new EntityLoadingException("Unsupported tech base: " + techBase);
            }
            mech.setWeight((float)Integer.parseInt(tonnage));
            mech.setOriginalWalkMP(Integer.parseInt(walkMP));
            if (jumpMP != null)
                mech.setOriginalJumpMP(Integer.parseInt(jumpMP));
            int expectedSinks = Integer.parseInt(heatSinks);

            mech.autoSetInternal();

            mech.initializeArmor(larmArmor, Mech.LOC_LARM);
            mech.initializeArmor(rarmArmor, Mech.LOC_RARM);
            mech.initializeArmor(ltArmor, Mech.LOC_LT);
            mech.initializeArmor(rtArmor, Mech.LOC_RT);
            mech.initializeArmor(ctArmor, Mech.LOC_CT);
            mech.initializeArmor(headArmor, Mech.LOC_HEAD);
            mech.initializeArmor(llegArmor, Mech.LOC_LLEG);
            mech.initializeArmor(rlegArmor, Mech.LOC_RLEG);
            mech.initializeRearArmor(ltrArmor, Mech.LOC_LT);
            mech.initializeRearArmor(rtrArmor, Mech.LOC_RT);
            mech.initializeRearArmor(ctrArmor, Mech.LOC_CT);

            // oog, crits.
            compactCriticals(mech);
            // we do these in reverse order to get the outermost
            //  locations first, which is necessary for split crits to work
            for (int i = mech.locations() - 1; i >= 0; i--) {
                parseCrits(mech, i);
            }

            if (mech.isClan()) {
                mech.addClanCase();
            }

            // add any heat sinks not allocated
            mech.addEngineSinks(expectedSinks - mech.heatSinks(), dblSinks);

            return mech;
        } catch (NumberFormatException ex) {
            throw new EntityLoadingException("NumberFormatException parsing file");
        } catch (NullPointerException ex) {
            throw new EntityLoadingException("NullPointerException parsing file");
        } catch (StringIndexOutOfBoundsException ex) {
            throw new EntityLoadingException("StringIndexOutOfBoundsException parsing file");
        }
    }

    private void parseCrits(Mech mech, int loc) throws EntityLoadingException {
        // check for removed arm actuators
        if (!(mech instanceof QuadMech)) {
            if (loc == Mech.LOC_LARM || loc == Mech.LOC_RARM) {
                if (!critData[loc][3][0].equals("Hand Actuator")) {
                    mech.setCritical(loc, 3, null);
                }
                if (!critData[loc][2][0].equals("Lower Arm Actuator")) {
                    mech.setCritical(loc, 2, null);
                }
            }
        }

        // go thru file, add weapons
        for (int i = 0; i < mech.getNumberOfCriticals(loc); i++) {

            // if the slot's full already, skip it.
            if (mech.getCritical(loc, i) != null) {
                continue;
            }

            // parse out and add the critical
            String critName = critData[loc][i][0];
            boolean rearMounted = true;
            if (critData[loc][i][1] == null || !isRearMounted[Integer.parseInt(critData[loc][i][1])]) {
                rearMounted = false;
            }
            boolean split = true;
            if (critData[loc][i][1] == null || !isSplit[Integer.parseInt(critData[loc][i][1])]) {
                split = false;
            }

            if (critName.indexOf("Engine") != -1) {
                mech.setCritical(loc,i, new CriticalSlot(CriticalSlot.TYPE_SYSTEM, Mech.SYSTEM_ENGINE));
                continue;
            }
            if (critName.endsWith("[LRM]") || critName.endsWith("[SRM]")) {
                // This is a lame kludge for The Drawing Board, which
                //  identifies which type of missle weapon an
                //  Artemis IV system goes with.
                critName = critName.substring(0,14);
            }
            if (critName.endsWith("- Artemis IV")) {
                // Ugh, another lame kludge to allow for loading of
                //  The Drawing Board's specially marked Artemis IV
                //  missle ammo.  The only real game difference is
                //  c-bill cost, which we don't care about anyway.
                critName = critName.substring(0,critName.indexOf(" - Artemis IV"));
            }
            if (critName.endsWith("- Narc")) {
                // Yet another lame kludge to allow for loading of
                //  The Drawing Board's specially marked Narc
                //  missle ammo.
                critName = critName.substring(0,critName.indexOf(" - Narc"));
            }
            if (critName.equals("(C) Endosteel")) {
                // MegaMek determines whether Endo Steel is IS or Clan
                //  type by techbase of mech.
                critName = critName.substring(4);
            }
            if (critName.equals("(C) Ferro-Fibrous Armor")) {
                // MegaMek determines whether FF Armor is IS or Clan
                //  type by techbase of mech.
                critName = critName.substring(4);
            }
            try {
                String hashPrefix;
                if (critName.startsWith("(C)")) {
                    //equipment specifically marked as clan
                    hashPrefix = "Clan ";
                    critName = critName.substring(4);
                } else if (critName.startsWith("(IS)")) {
                    //equipment specifically marked as inner sphere
                    hashPrefix = "IS ";
                    critName = critName.substring(5);
                } else if (mech.isClan()) {
                    //assume equipment is clan because mech is clan
                    hashPrefix = "Clan ";
                } else {
                    //assume equipment is inner sphere
                    hashPrefix = "IS ";
                }
                EquipmentType etype = EquipmentType.get(hashPrefix + critName);
                if (etype == null) {
                    //try without prefix
                    etype = EquipmentType.get(critName);
                }
                if (etype != null) {
                    if (etype.isSpreadable()) {
                        // do we already have one of these?  Key on Type
                        Mounted m = (Mounted)hSharedEquip.get(etype);
                        if (m != null) {
                            // use the existing one
                            mech.addCritical(loc, new CriticalSlot(CriticalSlot.TYPE_EQUIPMENT,
                                                                   mech.getEquipmentNum(m), etype.isHittable()));
                            continue;
                        }
                        else {
                            m = mech.addEquipment(etype, loc, rearMounted);
                            hSharedEquip.put(etype, m);
                        }
                    }
                    else if (split) {
                        // do we already have this one in this or an outer location?
                        Mounted m = null;
                        boolean bFound = false;
                        for (int x = 0, n = vSplitWeapons.size(); x < n; x++) {
                            m = (Mounted)vSplitWeapons.elementAt(x);
                            int nLoc = m.getLocation();
                            if ((nLoc == loc || loc == Mech.getInnerLocation(nLoc))
                                && m.getType() == etype) {
                                bFound = true;
                                break;
                            }
                        }
                        if (bFound) {
                            m.setFoundCrits(m.getFoundCrits() + 1);
                            if (m.getFoundCrits() >= etype.getCriticals(mech)) {
                                vSplitWeapons.removeElement(m);
                            }
                            // give the most restrictive location for arcs
                            m.setLocation(Mech.mostRestrictiveLoc(loc, m.getLocation()));
                        }
                        else {
                            // make a new one
                            m = new Mounted(mech, etype);
                            m.setSplit(true);
                            m.setFoundCrits(1);
                            mech.addEquipment(m, loc, rearMounted);
                            vSplitWeapons.addElement(m);
                        }
                        mech.addCritical(loc, new CriticalSlot(CriticalSlot.TYPE_EQUIPMENT,
                                                               mech.getEquipmentNum(m), etype.isHittable()));
                    }
                    else {
                        mech.addEquipment(etype, loc, rearMounted);
                    }
                } else {
                    if (!critName.equals("Empty")) {
                        //Can't load this piece of equipment!
                        // Add it to the list so we can show the user.
                        mech.addFailedEquipment(critName);
                        // Make the failed equipment an empty slot
                        critData[loc][i][0] = "Empty";
                        critData[loc][i][1] = null;
                        // Compact criticals again
                        compactCriticals(mech, loc);
                        // Re-parse the same slot, since the compacting
                        //  could have moved new equipment to this slot
                        i--;
                    }
                }
            } catch (LocationFullException ex) {
                throw new EntityLoadingException(ex.getMessage());
            }
        }
    }

    /**
     * This function moves all "empty" slots to the end of a location's
     * critical list.
     *
     * MegaMek adds equipment to the first empty slot available in a
     * location.  This means that any "holes" (empty slots not at the
     * end of a location), will cause the file crits and MegaMek's crits
     * to become out of sync.
     */
    private void compactCriticals(Mech mech) {
        for (int loc = 0; loc < mech.locations(); loc++) {
            compactCriticals(mech, loc);
        }
    }

    private void compactCriticals(Mech mech, int loc) {
        if (loc == Mech.LOC_HEAD) {
            //This location has an empty slot inbetween systems crits
            // which will mess up parsing if compacted.
            return;
        }
        int firstEmpty = -1;
        for (int slot = 0; slot < mech.getNumberOfCriticals(loc); slot++) {
            if (critData[loc][slot][0].equals("Empty")) {
                firstEmpty = slot;
            }
            if (firstEmpty != -1 && !critData[loc][slot][0].equals("Empty")) {
                //move this to the first empty slot
                critData[loc][firstEmpty][0] = critData[loc][slot][0];
                critData[loc][firstEmpty][1] = critData[loc][slot][1];
                //mark the old slot empty
                critData[loc][slot][0] = "Empty";
                critData[loc][slot][1] = null;
                //restart just after the moved slot's new location
                slot = firstEmpty;
                firstEmpty = -1;
            }
        }
    }

}

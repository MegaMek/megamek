/*
 * MegaMek - Copyright (C) 2000-2002 Ben Mazur (bmazur@sev.org)
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

package megamek.common;

import java.io.*;
import java.util.Hashtable;
import java.util.Vector;
import java.util.Enumeration;

import gd.xml.*;
import gd.xml.tiny.*;

public class TdbFile implements MechLoader {

    private ParsedXML root = null;

    /**
     * The names of the various elements recognized by this parser.
     */
    private static final String  NAME    = "name";
    private static final String  MODEL    = "model";
    private static final String  VARIANT    = "variant";
    private static final String  TECHNOLOGY    = "technology";
    private static final String  TONNAGE    = "tonnage";
    private static final String  TYPE    = "type";
    private static final String  OMNI    = "movemechmod";
    private static final String  WALK   = "walk";
    private static final String  JUMP    = "jump";
    private static final String  HEAT_SINKS    = "heatsinks";
    private static final String  MOUNTED_ITEM    = "mounteditem";
    private static final String  ITEM_DEFS    = "itemdefs";
    private static final String  LOCATION= "location";

    /**
     * The names of the attributes recognized by this parser.
     */
    private static final String  LEVEL    = "level";
    private static final String  POD_SPACE    = "podspace";
    private static final String  COUNT    = "count";
    private static final String  REAR_MOUNTED    = "rearmounted";
    private static final String  IS_SPREAD    = "isspread";
    private static final String  ITEM_INDEX    = "itemindex";


    /**
     * Special values recognized by this parser.
     */
    private static final String  TRUE    = "True";
    private static final String  FALSE   = "False";
    private static final String  DOUBLE   = "Double";

    
    private String name;
    private String model;
    private String variant;
    
    private String chassisConfig;
    private String podSpace;
    private String techBase;
    private static final String techYear = "3068"; // TDB doesn't have era
    private String rulesLevel;
    
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
    public TdbFile(InputStream is, String fileName) throws EntityLoadingException {
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

        /** Ack!  There is no armor allocation information inside
            a Drawing Board xml file.  We will have to pull this
            information from a binary Drawing board (.dbm) save
            file.  This means that both the .xml file and the .dbm
            files have to be present (with the same name).
        */

        Vector armorValues = readArmor(fileName);
        try {
            larmArmor = ((Integer)armorValues.elementAt(0)).intValue();
            rarmArmor = ((Integer)armorValues.elementAt(10)).intValue();
            ltArmor = ((Integer)armorValues.elementAt(2)).intValue();
            rtArmor = ((Integer)armorValues.elementAt(8)).intValue();
            ctArmor = ((Integer)armorValues.elementAt(12)).intValue();
            headArmor = ((Integer)armorValues.elementAt(14)).intValue();
            llegArmor = ((Integer)armorValues.elementAt(4)).intValue();
            rlegArmor = ((Integer)armorValues.elementAt(6)).intValue();
            ltrArmor = ((Integer)armorValues.elementAt(16)).intValue();
            rtrArmor = ((Integer)armorValues.elementAt(18)).intValue();
            ctrArmor = ((Integer)armorValues.elementAt(20)).intValue();
        } catch (Exception e) {
            throw new EntityLoadingException("Could not parse armor from Drawing Board (.dbm) file: " + e.getMessage());
        }

        // The "if" block below should be removed eventually,
        //  it is basically a debuging tool.
        if (larmArmor != rarmArmor || ltArmor != rtArmor
            || llegArmor != rlegArmor || ltrArmor != rtrArmor) {
            System.out.println("  -Warning: This mech appears to have asymetrical armor.  If that is the case, then ignore this message.  If this mech's armor is supposed to be symetrical, then we have failed to read the armor from the '.dbm' file correctly");
        }

    }

    private void parseNode(ParsedXML node) throws EntityLoadingException {
        if (!node.getTypeName().equals("tag")) {
            // We only want to parse element nodes, text nodes
            //  are implicitly parsed when needed.
            return;
        }

        Enumeration children = node.elements();

        if (node.getName().equals(NAME)) {
            name = ((ParsedXML)children.nextElement()).getContent();
        } else if (node.getName().equals(MODEL)) {
            model = ((ParsedXML)children.nextElement()).getContent();
        } else if (node.getName().equals(VARIANT)) {
            variant = ((ParsedXML)children.nextElement()).getContent();
        } else if (node.getName().equals(TECHNOLOGY)) {
            techBase = ((ParsedXML)children.nextElement()).getContent();
            rulesLevel = node.getAttribute(LEVEL);
        } else if (node.getName().equals(TONNAGE)) {
            tonnage = ((ParsedXML)children.nextElement()).getContent();
        } else if (node.getName().equals(TYPE)) {
            chassisConfig = ((ParsedXML)children.nextElement()).getContent();
        } else if (node.getName().equals(OMNI)) {
            podSpace = node.getAttribute(POD_SPACE);
        } else if (node.getName().equals(WALK)) {
            walkMP = ((ParsedXML)children.nextElement()).getContent();
        } else if (node.getName().equals(JUMP)) {
            jumpMP = ((ParsedXML)children.nextElement()).getContent();
        } else if (node.getName().equals(HEAT_SINKS)) {
            if (((ParsedXML)children.nextElement()).getContent().equals(DOUBLE)) {
                dblSinks = true;
            } else {
                dblSinks = false;
            }
            heatSinks = node.getAttribute(COUNT);
        } else if (node.getName().equals(MOUNTED_ITEM)) {
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
        } else if (node.getName().equals(LOCATION)) {
            int loc = -1;
            int i = 0;
            if (node.getAttribute(NAME).equals("LA"))
                loc = Mech.LOC_LARM;
            else if (node.getAttribute(NAME).equals("RA"))
                loc = Mech.LOC_RARM;
            else if (node.getAttribute(NAME).equals("LT"))
                loc = Mech.LOC_LT;
            else if (node.getAttribute(NAME).equals("RT"))
                loc = Mech.LOC_RT;
            else if (node.getAttribute(NAME).equals("CT"))
                loc = Mech.LOC_CT;
            else if (node.getAttribute(NAME).equals("H"))
                loc = Mech.LOC_HEAD;
            else if (node.getAttribute(NAME).equals("LL"))
                loc = Mech.LOC_LLEG;
            else if (node.getAttribute(NAME).equals("RL"))
                loc = Mech.LOC_RLEG;
            if (loc == -1)
                throw new EntityLoadingException("   Bad Mech location");
            while (children.hasMoreElements()) {
                ParsedXML critSlotNode = (ParsedXML)children.nextElement();
                critData[loc][i][0] = ((ParsedXML)critSlotNode.elements().nextElement()).getContent();
                critData[loc][i++][1] = critSlotNode.getAttribute(ITEM_INDEX);
            }
        } else if (node.getName().equals(ITEM_DEFS)) {
            return; // don't need item defs section of xml
        } else if (children != null) {
            // Use recursion to process all the children
            while (children.hasMoreElements()) {
                parseNode((ParsedXML)children.nextElement());
            } 
        }
    }
    
    private static Vector readArmor(String fileName) throws EntityLoadingException {
        /**
           In a Drawing Board .dbm file, the bytes that hold the armor
           distribution for each location seem to always be followed by
           the following string of bytes (in hex): 0,4D,0,8,0,53.  That
           is how we find them in the binary file.
           Note: The above method for finding the armor is invalid for
           clan omni mechs.  In the case of a clan omni mech, only the
           following couple of bytes follow the armor (in hex): 0,4D.
           This seems like a much more error prone method, but we'll
           use it until the bug reports start rolling in :)
           One place where (0,4D) can be found is the name/model area
           when a mechs name/model/etc starts with "M".  So we won't
           start looking for armor until address 100h or so in the
           file.
        */
        try {
            File BASE = new File(Settings.mechDirectory);
            File armorFile = new File(BASE, fileName.substring(0,fileName.lastIndexOf(".")) + ".dbm");
            FileInputStream fStream = new FileInputStream(armorFile);
            DataInputStream dStream = new DataInputStream(fStream);

            Vector armorValues = new Vector(28);
            int a = 0;

            for (int i = 0; i < 27; i++) {
                armorValues.addElement(new Integer(dStream.readUnsignedByte()));
                a++;
            }

             while (true) {
                 armorValues.addElement(new Integer(dStream.readUnsignedByte()));
                 a++;
                 armorValues.removeElementAt(0);
                 
                 //if (((Integer)armorValues.elementAt(26)).intValue() == 83 &&
                 //    ((Integer)armorValues.elementAt(25)).intValue() == 0 &&
                 //    ((Integer)armorValues.elementAt(24)).intValue() == 8 &&
                 //    ((Integer)armorValues.elementAt(23)).intValue() == 0 &&
                 //    ((Integer)armorValues.elementAt(22)).intValue() == 77 &&
                 //    ((Integer)armorValues.elementAt(21)).intValue() == 0) {
                 if (((Integer)armorValues.elementAt(22)).intValue() == 77 &&
                     ((Integer)armorValues.elementAt(21)).intValue() == 0 && a > 256) {
                     break;
                 }
             }

            return armorValues;

        } catch (EOFException ex) {
            throw new EntityLoadingException("Could not parse armor from Drawing Board (.dbm) file.  Premature end of file.");
        } catch (IOException ex) {
            throw new EntityLoadingException("Could not parse armor from Drawing Board (.dbm) file.  Note that both Drawing Board save files (.xml and .dbm) must be present for each Drawing Board mech.");
        }
    }
    
    public Entity getEntity() throws EntityLoadingException {
        try {
            Mech mech;
            
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
            if (podSpace != null && podSpace.length() > 0) {
                mech.setOmni(true);
            }

            //TODO: this ought to be a better test
            if (techBase.equals("Inner Sphere")) {
                switch (Integer.parseInt(rulesLevel)) {
                case 1 :
                    mech.setTechLevel(TechConstants.T_IS_LEVEL_1);
                    break;
                case 2 :
                    mech.setTechLevel(TechConstants.T_IS_LEVEL_2);
                    break;
                default :
                    throw new EntityLoadingException("Unsupported tech level");
                }
            } else if (techBase.equals("Clan")) {
                if (Integer.parseInt(rulesLevel) == 2)
                    mech.setTechLevel(TechConstants.T_CLAN_LEVEL_2);
                else
                    throw new EntityLoadingException("Unsupported tech level");
            } else {
                throw new EntityLoadingException("Unsupported tech base");
            }
            mech.weight = (float)Integer.parseInt(tonnage);
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
            // we do these in reverse order to get the outermost locations first,
            // which is necessary for split crits to work
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
            try {
                EquipmentType etype = EquipmentType.getByTdbName(critName,mech.isClan());
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
                            if ((nLoc == loc || loc == getInnerLocation(nLoc)) 
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
                            m.setLocation(mostRestrictiveLoc(loc, m.getLocation()));
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
                        // Can't load this piece of equipment!
                        System.out.println("              ***Cannot load equipment:" + critName);
                    }
                }
            } catch (LocationFullException ex) {
                throw new EntityLoadingException(ex.getMessage());
            }
        }
    }
    
    private int getInnerLocation(int n)
    {
        switch(n) {
            case Mech.LOC_RT :
            case Mech.LOC_LT :
                return Mech.LOC_CT;
            case Mech.LOC_LLEG :
            case Mech.LOC_LARM :
                return Mech.LOC_LT;
            case Mech.LOC_RLEG :
            case Mech.LOC_RARM :
                return Mech.LOC_RT;
            default:
                return n;
        }
    }
    
    private int mostRestrictiveLoc(int n1, int n2)
    {
        if (n1 == n2) {
            return n1;
        }
        else if (restrictScore(n1) >= restrictScore(n2)) {
            return n1;
        }
        else {
            return n2;
        }
    }
    
    private int restrictScore(int n)
    {
        switch(n) {
            case Mech.LOC_RARM :
            case Mech.LOC_LARM :
                return 0;
            case Mech.LOC_RT :
            case Mech.LOC_LT :
                return 1;
            case Mech.LOC_CT :
                return 2;
            default :
                return 3;
        }
    }
}

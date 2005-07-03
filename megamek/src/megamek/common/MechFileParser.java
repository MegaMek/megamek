/*
 * MechFileParser.java - Copyright (C) 2002,2003,2004 Josh Yockey
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

import java.io.*;
import java.util.Enumeration;
import java.util.Vector;
import java.util.zip.*;
import megamek.common.preference.PreferenceManager;

import megamek.common.loaders.*;
import megamek.common.util.BuildingBlock;

 /*
  * Switches between the various type-specific parsers depending on suffix
  */

public class MechFileParser {
    private Entity m_entity = null;
    private static Vector canonUnitNames = null;
    private static final File ROOT = new File(PreferenceManager.getClientPreferences().getMechDirectory());
    private static final File OFFICIALUNITS = new File(ROOT, "OfficialUnitList.txt");

    public MechFileParser(File f) throws EntityLoadingException {
        this(f, null);
    }

    public MechFileParser(File f, String entryName) throws EntityLoadingException {
        if (entryName == null) {
            // try normal file
            try {
                parse(new FileInputStream(f), f.getName());
            } catch (Exception ex) {
                if (ex instanceof EntityLoadingException) {
                    throw new EntityLoadingException("While parsing file " + f.getName() + ", " + ex.getMessage());
                } else {
                    throw new EntityLoadingException("Exception from " + ex.getClass() + ": " + ex.getMessage());
                }
            }
        } else {
            // try zip file
            ZipFile zFile;
            try {
                zFile = new ZipFile(f);
                parse(zFile.getInputStream(zFile.getEntry(entryName)), entryName);
            } catch (Exception ex) {
                if (ex instanceof EntityLoadingException) {
                    throw new EntityLoadingException(ex.getMessage());
                } else {
                    throw new EntityLoadingException("Exception from " + ex.getClass() + ": " + ex.getMessage());
                }
            }
        }
    }

    public MechFileParser(InputStream is, String fileName) throws EntityLoadingException {
        try {
            parse(is, fileName);
        } catch (Exception ex) {
            if (ex instanceof EntityLoadingException) {
                throw new EntityLoadingException(ex.getMessage());
            } else {
                throw new EntityLoadingException("Exception from " + ex.getClass() + ": " + ex.getMessage());
            }
        }
    }

    public Entity getEntity() { return m_entity; }

    public void parse(InputStream is, String fileName) throws EntityLoadingException {
        String lowerName = fileName.toLowerCase();
        IMechLoader loader;

        if (lowerName.endsWith(".mep")) {
            loader = new MepFile(is);
        } else if (lowerName.endsWith(".mtf")) {
            loader = new MtfFile(is);
        } else if (lowerName.endsWith(".hmp")) {
            loader = new HmpFile(is);
        } else if (lowerName.endsWith(".hmv")) {
            loader = new HmvFile(is);
        } else if (lowerName.endsWith(".xml")) {
            loader = new TdbFile(is);
        } else if (lowerName.endsWith(".blk")) {
            BuildingBlock bb = new BuildingBlock(is);
            if (bb.exists("UnitType")) {
                String sType = bb.getDataAsString("UnitType")[0];
                if (sType.equals("Tank")) {
                    loader = new BLKTankFile(bb);
                }
                else if (sType.equals("Infantry")) {
                    loader = new BLKInfantryFile(bb);
                }
                else if (sType.equals("BattleArmor")) {
                    loader = new BLKBattleArmorFile(bb);
                }
                else if (sType.equals("ProtoMech")) {
                    loader = new BLKProtoFile(bb);
                }
                else if (sType.equals("Mech")) {
                    loader = new BLKMechFile(bb);
                }
                else if (sType.equals("VTOL")) {
                    loader = new BLKVTOLFile(bb);
                }
                else {
                    throw new EntityLoadingException("Unknown UnitType: " + sType);
                }
            }
            else {
                loader = new BLKMechFile(bb);
            }
        } else if (lowerName.endsWith(".dbm")) {
            throw new EntityLoadingException("In order to use mechs from The Drawing Board with MegaMek, you must save your mech as an XML file (look in the 'File' menu of TDB.)  Then use the resulting '.xml' file instead of the '.dbm' file.  Note that only version 2.0.23 or later of TDB is compatible with MegaMek.");
        } else {
            throw new EntityLoadingException("Unsupported file suffix");
        }

        m_entity = loader.getEntity();
        postLoadInit(m_entity);
    }

    /**
     * File-format agnostic location to do post-load initialization on a unit.
     * Automatically add BattleArmorHandles to all OmniMechs.
     */
    private void postLoadInit(Entity ent) throws EntityLoadingException {

        // Walk through the list of equipment.
        for (Enumeration e = ent.getMisc(); e.hasMoreElements(); ) {
            Mounted m = (Mounted)e.nextElement();

            // Link Artemis IV fire-control systems to their missle racks.
            if (m.getType().hasFlag(MiscType.F_ARTEMIS) && m.getLinked() == null) {

                // link up to a weapon in the same location
                for (Enumeration e2 = ent.getWeapons(); e2.hasMoreElements(); ) {
                    Mounted mWeapon = (Mounted)e2.nextElement();
                    WeaponType wtype = (WeaponType)mWeapon.getType();

                    // only srm and lrm are valid for artemis
                    if (wtype.getAmmoType() != AmmoType.T_LRM && wtype.getAmmoType() != AmmoType.T_SRM) {
                        continue;
                    }

                    // already linked?
                    if (mWeapon.getLinkedBy() != null) {
                        continue;
                    }

                    // check location
                    if (mWeapon.getLocation() == m.getLocation()) {
                        m.setLinked(mWeapon);
                        break;
                    }
                    // also, mechs have a special location rule
                    else if (ent instanceof Mech && m.getLocation() == Mech.LOC_HEAD &&
                                mWeapon.getLocation() == Mech.LOC_CT) {
                        m.setLinked(mWeapon);
                        break;
                    }
                }

                if (m.getLinked() == null) {
                    // huh.  this shouldn't happen
                    throw new EntityLoadingException("Unable to match Artemis to launcher");
                }
            } // End link-Artemis
            else if ( Mech.STEALTH.equals(m.getType().getInternalName()) &&
                      m.getLinked() == null ) {
                // Find an ECM suite to link to the stealth system.
                // Stop looking after we find the first ECM suite.
                for ( Enumeration equips = ent.getMisc(); equips.hasMoreElements(); ) {
                    Mounted mEquip = (Mounted) equips.nextElement();
                    MiscType mtype = (MiscType) mEquip.getType();
                    if ( mtype.hasFlag(MiscType.F_ECM) ) {
                        m.setLinked( mEquip );
                        break;
                    }
                }

                if (m.getLinked() == null) {
                    // This mech has stealth armor but no ECM.  Probably
                    //  an improperly created custom.
                    throw new EntityLoadingException("Unable to find an ECM Suite.  Mechs with Stealth Armor must also be equipped with an ECM Suite.");
                }
            } // End link-Stealth

        } // Check the next piece of equipment.
        
        //Check if it's canon; if it is, mark it as such.
        ent.setCanon(false);//Guilty until proven innocent
        try {
            if(canonUnitNames==null) {
                canonUnitNames=new Vector();
                //init the list.
                BufferedReader br = null;
                try {
                    br = new BufferedReader(new FileReader(OFFICIALUNITS));
                } catch (FileNotFoundException e) {
                }
                String s;
                String name;
                while ((s = br.readLine()) != null) {
                    int nIndex1 = s.indexOf('|');
                    name=s.substring(0, nIndex1);
                    canonUnitNames.addElement(name);
                }
            }
        } catch (IOException e) {
            }
        for(Enumeration i = canonUnitNames.elements(); i.hasMoreElements();) {
            String s = (String)i.nextElement();
            if(s.equals(ent.getShortName())) {
                ent.setCanon(true);
            }
        }
        
            

    } // End private void postLoadInit(Entity) throws EntityLoadingException


}

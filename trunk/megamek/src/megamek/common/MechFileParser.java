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

import megamek.common.loaders.BLKBattleArmorFile;
import megamek.common.loaders.BLKGunEmplacementFile;
import megamek.common.loaders.BLKInfantryFile;
import megamek.common.loaders.BLKMechFile;
import megamek.common.loaders.BLKProtoFile;
import megamek.common.loaders.BLKTankFile;
import megamek.common.loaders.BLKVTOLFile;
import megamek.common.loaders.EntityLoadingException;
import megamek.common.loaders.HmpFile;
import megamek.common.loaders.HmvFile;
import megamek.common.loaders.IMechLoader;
import megamek.common.loaders.MepFile;
import megamek.common.loaders.MtfFile;
import megamek.common.loaders.TdbFile;
import megamek.common.preference.PreferenceManager;
import megamek.common.util.BuildingBlock;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Enumeration;
import java.util.Vector;
import java.util.zip.ZipFile;

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
                ex.printStackTrace();
                if (ex instanceof EntityLoadingException) {
                    throw new EntityLoadingException("While parsing file " + f.getName() + ", " + ex.getMessage());
                }
                throw new EntityLoadingException("Exception from " + ex.getClass() + ": " + ex.getMessage());
            }
        } else {
            // try zip file
            ZipFile zFile;
            try {
                zFile = new ZipFile(f);
                parse(zFile.getInputStream(zFile.getEntry(entryName)), entryName);
            } catch(EntityLoadingException ele ){
                throw new EntityLoadingException(ele.getMessage());
            } catch (NullPointerException npe){
                throw new NullPointerException();
            }
            catch (Exception ex) {
                ex.printStackTrace();
                throw new EntityLoadingException("Exception from " + ex.getClass() + ": " + ex.getMessage());
            }
        }
    }

    public MechFileParser(InputStream is, String fileName) throws EntityLoadingException {
        try {
            parse(is, fileName);
        } catch (Exception ex) {
            ex.printStackTrace();
            if (ex instanceof EntityLoadingException) {
                throw new EntityLoadingException(ex.getMessage());
            }
            throw new EntityLoadingException("Exception from " + ex.getClass() + ": " + ex.getMessage());
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
                if (sType.equals("Tank")
                        || sType.equals("Naval")
                        || sType.equals("Surface")
                        || sType.equals("Hydrofoil")) {
                    loader = new BLKTankFile(bb);
                } else if (sType.equals("Infantry")) {
                    loader = new BLKInfantryFile(bb);
                } else if (sType.equals("BattleArmor")) {
                    loader = new BLKBattleArmorFile(bb);
                } else if (sType.equals("ProtoMech")) {
                    loader = new BLKProtoFile(bb);
                } else if (sType.equals("Mech")) {
                    loader = new BLKMechFile(bb);
                } else if (sType.equals("VTOL")) {
                    loader = new BLKVTOLFile(bb);
                } else if (sType.equals("GunEmplacement")) {
                    loader = new BLKGunEmplacementFile(bb);
                } else {
                    throw new EntityLoadingException("Unknown UnitType: " + sType);
                }
            } else {
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
        for (Mounted m : ent.getMisc()) {

            // Link Artemis IV fire-control systems to their missle racks.
            if (m.getType().hasFlag(MiscType.F_ARTEMIS) && m.getLinked() == null) {

                // link up to a weapon in the same location
                for (Mounted mWeapon : ent.getWeaponList()) {
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
                for (Mounted mEquip : ent.getMisc()) {
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
            
            if(ent instanceof Mech && m.getType().hasFlag(MiscType.F_CASE)) {
                ((Mech)ent).setAutoEject(false);
            }

        } // Check the next piece of equipment.
        
        //Check if it's canon; if it is, mark it as such.
        ent.setCanon(false);//Guilty until proven innocent
        try {
            if (canonUnitNames==null) {
                canonUnitNames=new Vector();
                //init the list.
                BufferedReader br = null;
                try {
                    br = new BufferedReader(new FileReader(OFFICIALUNITS));
                    String s;
                    String name;
                    while ((s = br.readLine()) != null) {
                        int nIndex1 = s.indexOf('|');
                        name=s.substring(0, nIndex1);
                        canonUnitNames.addElement(name);
                    }
                } catch (FileNotFoundException e) {
                }
            }
        } catch (IOException e) {
        }
        for (Enumeration i = canonUnitNames.elements(); i.hasMoreElements();) {
            String s = (String)i.nextElement();
            if(s.equals(ent.getShortNameRaw())) {
                ent.setCanon(true);
                break;
            }
        }
    } // End private void postLoadInit(Entity) throws EntityLoadingException

    public static void main(String[] args) {
        if (args.length == 0) {
            System.out.println("Files in a supported MegaMek file format can be specified on\nthe command line.  Multiple files may be processed at once.\nThe supported formats are:\n\t.mtf    The native MegaMek format that your file will be converted into\n\n\t.blk    Another native MegaMek format\n\t.hmp    Heavy Metal Pro (c)RCW Enterprises\n\t.mep    MechEngineer Pro (c)Howling Moon SoftWorks\n\t.xml    The Drawing Board (c)Blackstone Interactive\n\nNote: If you are using the MtfConvert utility, you may also drag and drop files onto it for conversion.\n");
            MechFileParser.getResponse("Press <enter> to exit...");
            return;
        }
        for (int i = 0; i < args.length; i++) {
            String filename = args[i];
            File file = new File(filename);
            String outFilename = filename.substring(0, filename.lastIndexOf("."));
            outFilename += ".mtf";
            File outFile = new File(outFilename);
            if (outFile.exists()) {
                if (!MechFileParser.getResponse("File already exists, overwrite? "))
                    return;
            }
            BufferedWriter out = null;
            try {
                MechFileParser mfp = new MechFileParser(file);
                out = new BufferedWriter(new FileWriter(outFile));
                out.write(((Mech)mfp.getEntity()).getMtf());
            } catch (Exception e) {
                System.out.println(e.getMessage());
                MechFileParser.getResponse("Press <enter> to exit...");
            } finally {
                if (out != null) {
                    try {
                        out.close();
                    } catch (IOException ex) {
                        //ignore
                    }
                }
            }
        }
    }

    private static boolean getResponse(String prompt) {
        String response = null;
        BufferedReader in = new BufferedReader(new InputStreamReader(System.in));
        System.out.print(prompt);
        try {
            response = in.readLine();
        } catch (IOException ioe) {
        }
        if (response != null && response.toLowerCase().indexOf("y") == 0)
            return true;
        return false;
    }
    
    public static void dispose() {
        canonUnitNames = null;
    }
}

/*
 * MechFileParser.java - Copyright (C) 2002 Josh Yockey
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
import java.util.zip.*;
import megamek.common.util.BuildingBlock;

 /*
  * Switches between the various type-specific parsers depending on suffix
  */

public class MechFileParser {
    private Entity m_entity = null;
    
    public MechFileParser(File f) throws EntityLoadingException {
        this(f, null);
    }
    
    public MechFileParser(File f, String entryName) throws EntityLoadingException {
        if (entryName == null) {
            // try normal file
            try {
                parse(new FileInputStream(f), f.getName());
            } catch (FileNotFoundException ex) {
                throw new EntityLoadingException(ex.getMessage());
            }
        } else {
            // try zip file
            ZipFile zFile;
            try {
                zFile = new ZipFile(f);
                parse(zFile.getInputStream(zFile.getEntry(entryName)), entryName);
            } catch (Exception ex) {
                throw new EntityLoadingException(ex.getMessage());
            }
        }
    }
    
    public MechFileParser(InputStream is, String fileName) throws EntityLoadingException {
        parse(is, fileName);
    }
    
    public Entity getEntity() { return m_entity; }
    
    public void parse(InputStream is, String fileName) throws EntityLoadingException {
        String lowerName = fileName.toLowerCase();
        MechLoader loader;

        if (lowerName.endsWith(".mep")) {
            loader = new MepFile(is);
        } else if (lowerName.endsWith(".mtf")) {
            loader = new MtfFile(is);
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
                else if (sType.equals("Mech")) {
                    loader = new BLKMechFile(bb);
                }
                else {
                    throw new EntityLoadingException("Unknown UnitType: " + sType);
                }
            }
            else {
                loader = new BLKMechFile(bb);
            }
        } else {
            throw new EntityLoadingException("Unsupported file suffix");
        }
        
        m_entity = loader.getEntity();
    }
}
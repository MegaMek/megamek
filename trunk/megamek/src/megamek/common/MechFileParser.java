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

import java.io.File;
import megamek.common.util.BuildingBlock;

 /*
  * Switches between the various type-specific parsers depending on suffix
  */

public class MechFileParser {
    private File m_file;
    private Entity m_entity = null;
    
    public MechFileParser(File f) throws EntityLoadingException {
        parse(f);
    }
    
    public Entity getEntity() { return m_entity; }
    
    private void parse(File f) throws EntityLoadingException {
        m_file = f;
        MechLoader loader;
        
        if (f.getName().toLowerCase().endsWith(".mep")) {
            loader = new MepFile(f);
        } else if (f.getName().toLowerCase().endsWith(".mtf")) {
            loader = new MtfFile(f);
        } else if (f.getName().toLowerCase().endsWith(".blk")) {
            BuildingBlock bb = new BuildingBlock(f.getPath());
            if (bb.exists("UnitType")) {
                String sType = bb.getDataAsString("UnitType")[0];
                if (sType.equals("Tank")) {
                    loader = new BLKTankFile(bb);
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
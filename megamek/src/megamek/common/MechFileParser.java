/*
 * MechSelectorDialog.java - Copyright (C) 2002 Josh Yockey
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
  * It also contains info not currently stored in the mech object, including
  * year and techtype
  */

public class MechFileParser {
    private File m_file;
    private Mech m_mech = null;
    
    public MechFileParser(File f) {
        parse(f);
    }
    
    public Mech getMech() { return m_mech; }
    
    private void parse(File f) {
        m_file = f;
        if (f.getName().toLowerCase().endsWith(".mep")) {
            MepFile mf = new MepFile(f);
            m_mech = mf.getMech();
        }
        else if (f.getName().toLowerCase().endsWith(".mtf")) {
            MtfFile mf = new MtfFile(f);
            m_mech = mf.getMech();
        }
        else if (f.getName().toLowerCase().endsWith(".blk")) {
            BLKMechFile mf = new BLKMechFile(f);
            m_mech = mf.getMech();
            BuildingBlock bb = mf.dataFile;
        }
    }
}
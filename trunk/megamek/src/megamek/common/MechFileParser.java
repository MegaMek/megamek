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
 
 public class MechFileParser
 {
 	private File m_file;
 	private int m_nYear;
 	private String m_sTechType;
 	private Mech m_mech = null;
 	
 	public MechFileParser(File f)
 	{
 		parse(f);
 	}
 	
 	public int getYear() { return m_nYear; }
 	public String getTechType() { return m_sTechType; }
 	public Mech getMech() { return m_mech; }
 	
 	private void parse(File f) {
 		m_file = f;
		if (f.getName().toLowerCase().endsWith(".mep")) {
        	MepFile mf = new MepFile(f);
            m_mech = mf.getMech();
            m_nYear = Integer.parseInt(mf.techYear);
            m_sTechType = mf.innerSphere;
        } 
        else if (f.getName().toLowerCase().endsWith(".mtf")) {
            MtfFile mf = new MtfFile(f);
            m_mech = mf.getMech();
            m_nYear = Integer.parseInt(mf.techYear);
            m_sTechType = mf.techBase;
        } 
        else if (f.getName().toLowerCase().endsWith(".blk")) {
            BLKMechFile mf = new BLKMechFile(f);
            m_mech = mf.getMech();
            BuildingBlock bb = mf.dataFile;
            m_nYear = bb.getDataAsInt("year")[0];
            m_sTechType = bb.getDataAsString("type")[0];
        }
    }
 }
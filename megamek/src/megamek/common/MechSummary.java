/*
 * MechSummary.java - Copyright (C) 2002 Josh Yockey
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
 
 /* 
  * Contains minimal information about a single mech
  */
 
 public class MechSummary
 {
 	private String m_sName;
 	private String m_sRef;
 	private File m_sSourceFile;
 	private int m_nYear;
 	private int m_nType;
 	private int m_nTons;
 	private int m_nBV;

	
	public String getName() { return (this.m_sName); }
	public String getRef() { return (this.m_sRef); }
	public File getSourceFile() { return (this.m_sSourceFile); }
	public int getYear() { return (this.m_nYear); }
	public int getType() { return (this.m_nType); }
	public int getTons() { return (this.m_nTons); }
	public int getBV() { return (this.m_nBV); }
	
	public void setName(String m_sName) { this.m_sName = m_sName; }
	public void setRef(String m_sRef) { this.m_sRef = m_sRef; }
	public void setSourceFile(File m_sSourceFile) { this.m_sSourceFile = m_sSourceFile; }
	public void setYear(int m_nYear) { this.m_nYear = m_nYear; }
	public void setType(int m_nType) { this.m_nType = m_nType; }
	public void setTons(int m_nTons) { this.m_nTons = m_nTons; }
	public void setBV(int m_nBV) { this.m_nBV = m_nBV; }
}

 	
 	
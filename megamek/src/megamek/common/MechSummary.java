/*
 * MechSummary.java - Copyright (C) 2002,2003,2004 Josh Yockey
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
 import java.io.Serializable;
 
 /* 
  * Contains minimal information about a single entity
  */
 
 public class MechSummary implements Serializable
 {
    private String m_sName;
    private String m_sChassis;
    private String m_sModel;
    private String m_sUnitType;
    private File m_sSourceFile;
    private String m_sEntryName; // for files in zips
    private int m_nYear;
    private int m_nType;
    private int m_nTons;
    private int m_nBV;
    private int m_nCost;
    private long m_lModified; // for comparison when loading
    private String m_sLevel;
    
    public String getName() { return (this.m_sName); }
    public String getChassis() { return (this.m_sChassis); }
    public String getModel() { return (this.m_sModel); }
    public String getUnitType() { return (this.m_sUnitType); }
    
    public static String determineUnitType(Entity e) {
        int mm = e.getMovementMode();
        if (e instanceof Infantry) {
            return "Infantry";
        } else if (e instanceof VTOL) { //for now
            return "VTOL";
        } else if ((mm == IEntityMovementMode.NAVAL) || (mm == IEntityMovementMode.HYDROFOIL) || (mm == IEntityMovementMode.SUBMARINE)) {
            return "Naval";
        } else if (e instanceof Tank) {
            return "Tank";
        } else if (e instanceof Mech) {
            return "Mek";
        } else if (e instanceof Protomech) {
            return "ProtoMek";
        } else {
            //Hmm...this is not a good case, should throw excep. instead?
            return "Unknown";
        }
    }
    public File getSourceFile() { return (this.m_sSourceFile); }
    public String getEntryName() { return (this.m_sEntryName); }
    public int getYear() { return (this.m_nYear); }
    public int getType() { return (this.m_nType); }
    public int getTons() { return (this.m_nTons); }
    public int getBV() { return (this.m_nBV); }
    public int getCost() { return (this.m_nCost); }
    public long getModified() { return (this.m_lModified); }
    public String getLevel() { return (this.m_sLevel); }
    
    public void setName(String m_sName) { this.m_sName = m_sName; }
    public void setChassis(String m_sChassis) { this.m_sChassis = m_sChassis; }
    public void setModel(String m_sModel) { this.m_sModel = m_sModel; }
    public void setUnitType(String m_sUnitType) { this.m_sUnitType = m_sUnitType; }
    public void setSourceFile(File m_sSourceFile) { this.m_sSourceFile = m_sSourceFile; }
    public void setEntryName(String m_sEntryName) { this.m_sEntryName = m_sEntryName; }
    public void setYear(int m_nYear) { this.m_nYear = m_nYear; }
    public void setType(int m_nType) { this.m_nType = m_nType; }
    public void setTons(int m_nTons) { this.m_nTons = m_nTons; }
    public void setCost(int m_nCost) { this.m_nCost = m_nCost; }
    public void setBV(int m_nBV) { this.m_nBV = m_nBV; }
    public void setModified(long m_lModified) { this.m_lModified = m_lModified; }
    public void setLevel(String level) { this.m_sLevel = level; }
    
    public int getWeightClass() {
        return EntityWeightClass.getWeightClass(m_nTons);
    }
}

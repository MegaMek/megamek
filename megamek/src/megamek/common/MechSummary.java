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
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Vector;


/**
 * Contains minimal information about a single entity
 */

public class MechSummary implements Serializable {
    /**
     *
     */
    private static final long serialVersionUID = -6635709122122038237L;
    private String m_sName;
    private String m_sChassis;
    private String m_sModel;
    private String m_sUnitType;
    private String m_sUnitSubType;
    private File m_sSourceFile;
    private String m_sEntryName; // for files in zips
    private int m_nYear;
    private int m_nType;
    private float m_nTons;
    private int m_nBV;
    /**
     * Stores the BV of the unit computed using the geometric mean method.
     */
    private int m_gmBV;
    private int m_rhBV;
    private int m_rhgmBV;
    private long m_nCost;
    private long m_nUnloadedCost;
    private long m_aCost;
    private long m_lModified; // for comparison when loading
    private String m_sLevel;
    private boolean canon;
    private boolean clan;
    private boolean support;
    private int walkMp;
    private int runMp;
    private int jumpMp;
    private int totalArmor;
    private int totalInternal;
    private int cockpitType;
    private String engineName;
    private int gyroType;
    private String myomerName;
    /**
     * For BattleArmor, we want to know the weight of an individual suit.
     */
    private float m_TWsuitTons;
    private float m_TOsuitTons;
    private float suitWeight;
    

    /** Stores the type of internal structure on this unit **/
    private int internalsType;
    
    /**
     * Each location can have a separate armor type, but this is used for 
     * search purposes we we really only care about which types are present.
     */
    private HashSet<Integer> armorTypeSet;
    
    /**
     * Stores the armor type for each location.
     */
    private int[] armorLoc;
    
    /**
     * Stores the armor tech type for each location.
     */
    private int[] armorLocTech;
    
    
    public MechSummary(){
        armorTypeSet = new HashSet<Integer>();
    }
    
    /**
     * Store a unique list of the names of the equipment mounted on this unit.
     */
    private Vector<String> equipmentNames;
    
    /**
     * The number of times the piece of equipment in the corresponding 
     * <code>equipmentNames</code> list appears.
     */
    private Vector<Integer> equipmentQuantities;

    public String getName() {
        return (m_sName);
    }

    public String getChassis() {
        return (m_sChassis);
    }

    public String getModel() {
        return (m_sModel);
    }

    public String getUnitType() {
        return (m_sUnitType);
    }

    public boolean isCanon() {
        return canon;
    }

    public boolean isClan() {
        return clan;
    }

    public boolean isSupport() {
        return support;
    }

    public String getUnitSubType() {
        return m_sUnitSubType;
    }

    public static String determineUnitType(Entity e) {
        EntityMovementMode mm = e.getMovementMode();
        if (e instanceof BattleArmor) {
            return "BattleArmor";
        } else if (e instanceof Infantry) {
            return "Infantry";
        } else if (e instanceof VTOL) { // for now
            return "VTOL";
        } else if ((mm == EntityMovementMode.NAVAL)
                || (mm == EntityMovementMode.HYDROFOIL)
                || (mm == EntityMovementMode.SUBMARINE)) {
            return "Naval";
        } else if (e instanceof GunEmplacement) {
            return "Gun Emplacement";
        } else if (e instanceof Tank) {
            return "Tank";
        } else if (e instanceof Mech) {
            return "Mek";
        } else if (e instanceof Protomech) {
            return "ProtoMek";
        } else if (e instanceof SpaceStation) {
            return "Space Station";
        } else if (e instanceof Warship) {
            return "Warship";
        } else if (e instanceof Jumpship) {
            return "Jumpship";
        } else if (e instanceof Dropship) {
            return "Dropship";
        } else if (e instanceof SmallCraft) {
            return "Small Craft";
        } else if (e instanceof ConvFighter) {
            return "Conventional Fighter";
        } else if (e instanceof Aero) {
            return "Aero";
        } else {
            // Hmm...this is not a good case, should throw excep. instead?
            return "Unknown";
        }
    }

    public File getSourceFile() {
        return (m_sSourceFile);
    }

    public String getEntryName() {
        return (m_sEntryName);
    }

    public int getYear() {
        return (m_nYear);
    }

    public int getType() {
        return (m_nType);
    }

    public float getTons() {
        return (m_nTons);
    }

    public float getTOweight() {
        return (m_TOsuitTons);
    }

    public float getTWweight() {
        return (m_TWsuitTons);
    }

    public int getBV() {
        return (m_nBV);
    }

    public long getCost() {
        return (m_nCost);
    }

    public long getUnloadedCost() {
        return (m_nUnloadedCost);
    }

    public long getAlternateCost() {
        return (m_aCost);
    }

    public long getModified() {
        return (m_lModified);
    }

    public String getLevel() {
        return (m_sLevel);
    }

    public void setName(String m_sName) {
        this.m_sName = m_sName;
    }

    public void setChassis(String m_sChassis) {
        this.m_sChassis = m_sChassis;
    }

    public void setModel(String m_sModel) {
        this.m_sModel = m_sModel;
    }

    public void setUnitType(String m_sUnitType) {
        this.m_sUnitType = m_sUnitType;
    }

    public void setSourceFile(File m_sSourceFile) {
        this.m_sSourceFile = m_sSourceFile;
    }

    public void setEntryName(String m_sEntryName) {
        this.m_sEntryName = m_sEntryName;
    }

    public void setYear(int m_nYear) {
        this.m_nYear = m_nYear;
    }

    public void setType(int m_nType) {
        this.m_nType = m_nType;
    }

    public void setTons(float m_nTons) {
        this.m_nTons = m_nTons;
    }

    public void setTOweight(float m_TOsuitTons) {
        this.m_TOsuitTons = m_TOsuitTons;
    }

    public void setTWweight(float m_TWsuitTons) {
        this.m_TWsuitTons = m_TWsuitTons;
    }

    public void setCost(long m_nCost) {
        this.m_nCost = m_nCost;
    }

    public void setUnloadedCost(long m_nCost) {
        m_nUnloadedCost = m_nCost;
    }

    public void setAlternateCost(long m_aCost) {
        this.m_aCost = m_aCost;
    }

    public void setBV(int m_nBV) {
        this.m_nBV = m_nBV;
    }

    public void setModified(long m_lModified) {
        this.m_lModified = m_lModified;
    }

    public void setLevel(String level) {
        m_sLevel = level;
    }

    public void setCanon(boolean canon) {
        this.canon = canon;
    }

    public void setClan(boolean clan) {
        this.clan = clan;
    }

    public void setSupport(boolean support) {
        this.support = support;
    }

    public void setUnitSubType(String subType) {
        m_sUnitSubType = subType;
    }

    public int getWeightClass() {
        float tons;
        if (getUnitType().equals("BattleArmor")){
            tons = getSuitWeight();
        } else {
            tons = getTons();
        }
        if (isSupport()) {
            return EntityWeightClass.getSupportWeightClass(m_nTons, m_sUnitSubType);
        }
        return EntityWeightClass.getWeightClass(tons, getUnitType());
    }

    public int getWalkMp() {
        return walkMp;
    }

    public void setWalkMp(int walkMp) {
        this.walkMp = walkMp;
    }

    public int getRunMp() {
        return runMp;
    }

    public void setRunMp(int runMp) {
        this.runMp = runMp;
    }

    public int getJumpMp() {
        return jumpMp;
    }

    public void setJumpMp(int jumpMp) {
        this.jumpMp = jumpMp;
    }
    
    /**
     * Given the list of equipment mounted on this unit, parse it into a unique
     * list of names and the number of times that name appears.
     * 
     * @param mountedList A collection of <code>Mounted</code> equipment
     */
    public void setEquipment(List<Mounted> mountedList)
    {
        equipmentNames = new Vector<String>(mountedList.size());
        equipmentQuantities = new Vector<Integer>(mountedList.size());
        for (Mounted mnt : mountedList)
        {
            String eqName = mnt.getType().getInternalName();
            int index = equipmentNames.indexOf(eqName);
            if (index == -1){ //We haven't seen this piece of equipment before
                equipmentNames.add(eqName);
                equipmentQuantities.add(1);
            }else{ //We've seen this before, update count
                equipmentQuantities.set(index, equipmentQuantities.get(index)+1);
            }               
        }
        
    }
    
    public Vector<String> getEquipmentNames()
    {
        return equipmentNames;
    }
    
    public Vector<Integer> getEquipmentQuantities()
    {
        return equipmentQuantities;
    }

    public void setTotalArmor(int totalArmor) {
        this.totalArmor = totalArmor;
    }

    public int getTotalArmor() {
        return totalArmor;
    }

    public void setTotalInternal(int totalInternal) {
        this.totalInternal = totalInternal;
    }

    public int getTotalInternal() {
        return totalInternal;
    }

    public void setInternalsType(int internalsType) {
        this.internalsType = internalsType;
    }

    public int getInternalsType() {
        return internalsType;
    }

    /**
     * Takes the armor type at all locations and creates a set of the armor 
     * types.
     * 
     * @param locsArmor  An array that stores the armor type at each location.
     */
    public void setArmorType(int[] locsArmor) {
        armorTypeSet.clear();
        for (int i = 0; i < locsArmor.length; i++){
            armorTypeSet.add(locsArmor[i]);
        }
        
    }

    public HashSet<Integer> getArmorType() {
        return armorTypeSet;
    }
    
    public int [] getArmorTypes(){
        return armorLoc;
    }
    
    public void setArmorTypes(int [] al){
        armorLoc = al;
    }
    
    public int [] getArmorTechTypes(){
        return armorLocTech;
    }
    
    public void setArmorTechTypes(int [] att){
        armorLocTech = att;
    }

    public void setCockpitType(int cockpitType) {
        this.cockpitType = cockpitType;
    }

    public int getCockpitType() {
        return cockpitType;
    }


    public String getEngineName() {
        return engineName;
    }

    public void setEngineName(String engineName) {
        this.engineName = engineName;
    }

    public int getGyroType() {
        return gyroType;
    }

    public void setGyroType(int gyroType) {
        this.gyroType = gyroType;
    }

    public String getMyomerName() {
        return myomerName;
    }

    public void setMyomerName(String myomerName) {
        this.myomerName = myomerName;
    }

    public float getSuitWeight() {
        return suitWeight;
    }

    public void setSuitWeight(float suitWeight) {
        this.suitWeight = suitWeight;
    }

    public int getGMBV() {
        return m_gmBV;
    }

    public void setGMBV(int m_gmBV) {
        this.m_gmBV = m_gmBV;
    }

    public int getRHBV() {
        return m_rhBV;
    }

    public void setRHBV(int m_rhBV) {
        this.m_rhBV = m_rhBV;
    }

    public int getRHGMBV() {
        return m_rhgmBV;
    }

    public void setRHGMBV(int m_rhgmBV) {
        this.m_rhgmBV = m_rhgmBV;
    }

    @Override
    public boolean equals(Object obj) {
        if(this == obj) {
            return true;
        }
        if((null == obj) || (getClass() != obj.getClass())) {
            return false;
        }
        final MechSummary other = (MechSummary) obj;
        // we match on chassis + model + unittype + sourcefile
        return Objects.equals(m_sChassis, other.m_sChassis) && Objects.equals(m_sModel, other.m_sModel)
                && Objects.equals(m_sUnitType, other.m_sUnitType) && Objects.equals(m_sSourceFile, other.m_sSourceFile);
    }
    
    @Override
    public int hashCode() {
        return Objects.hash(m_sChassis, m_sModel, m_sUnitType, m_sSourceFile);
    }
}

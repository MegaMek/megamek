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
 * EquipmentType.java
 *
 * Created on April 1, 2002, 1:35 PM
 */

package megamek.common;

import java.util.*;

/**
 * Represents any type of equipment mounted on a mechs, excluding systems and 
 * actuators.
 *
 * @author  Ben
 * @version 
 */
public class EquipmentType {
    
    public static final float TONNAGE_VARIABLE = Float.MIN_VALUE;
    public static final int CRITICALS_VARIABLE = Integer.MIN_VALUE;
    public static final int BV_VARIABLE = Integer.MIN_VALUE;
    
    
    protected String    name = null;

    protected String    internalName = null;
    protected String    mepName = null;
    protected String    mtfName = null;
    protected String    tdbName = null;

    protected float     tonnage = 0;
    protected int       criticals = 0;
    protected int       techType = TechConstants.T_IS_LEVEL_1;
    
    protected boolean   explosive = false;
    protected boolean   hittable = true; // if false, reroll critical hits
    // can the crits for this be spread over locations?
    protected boolean   spreadable = false;
    protected int       toHitModifier = 0;

    protected int       flags = 0;
      
    protected double     bv = 0; // battle value point system
    
    // what modes can this equipment be in?
    protected String[] m_saModes = null;
    
    // can modes be switched instantly, or at end of turn?
    protected boolean m_bInstantModeSwitch = true;
    
    // static list of eq
    protected static Vector allTypes;
    protected static Hashtable internalNameHash;
    protected static Hashtable mepNameHash;
    protected static Hashtable mtfNameHash;
    protected static Hashtable tdbNameHash;

    /** Creates new EquipmentType */
    public EquipmentType() {
        ;
    }

    public String getName() {
        return name;
    }
    
    public String getDesc() {
        return name;
    }
    
    public String getInternalName() {
        return internalName;
    }
    
    public String getMepName() {
        return mepName;
    }
    
    public String getMtfName() {
        return mtfName;
    }
    
    public String getTdbName() {
        return tdbName;
    }
    
    public float getTonnage(Entity entity) {
        return tonnage;
    }

    public int getCriticals(Entity entity) {
        return criticals;
    }
    
    public int getTechType() {
        return techType;
    }
    
    public boolean isExplosive() {
        return explosive;
    }
    
    public boolean isHittable() {
        return hittable;
    }
    
    // like margarine!
    public boolean isSpreadable() { 
        return spreadable;
    }
    
    public int getToHitModifier() {
        return toHitModifier;
    }
    
    public int getFlags() {
        return flags;
    }
    
    public boolean hasFlag(int flag) {
        return (flags & flag) != 0;
    }

    public double getBV(Entity entity) {
        return bv;
    }
    
    public String[] getModes() {
        return m_saModes;
    }
    
    public void setModes(String[] sa) {
        m_saModes = sa;
    }
    
    public boolean hasModes() {
        return m_saModes != null;
    }
    
    public void setInstantModeSwitch(boolean b) {
        m_bInstantModeSwitch = b;
    }
    
    public boolean hasInstantModeSwitch() {
        return m_bInstantModeSwitch;
    }

    public static void initializeTypes() {
        allTypes = new Vector();
        // will I need any others?
        WeaponType.initializeTypes();
        AmmoType.initializeTypes();
        MiscType.initializeTypes();
    }
    
    public static Enumeration getAllTypes() {
        return allTypes.elements();
    }
    
    protected static void addType(EquipmentType type) {
        allTypes.addElement(type);
    }
    
    public static EquipmentType getByInternalName(String key) {
        if (internalNameHash == null) {
            initializeInternalNameHash();
        }
        
        return (EquipmentType)internalNameHash.get(key);
    }
    
    public static void initializeInternalNameHash() {
        if (allTypes == null) {
            initializeTypes();
        }
        
        internalNameHash = new Hashtable(allTypes.size());
        
        for (Enumeration i = allTypes.elements(); i.hasMoreElements();) {
            EquipmentType type = (EquipmentType)i.nextElement();
            
            internalNameHash.put(type.getInternalName(), type);
        }
    }
    
    public static EquipmentType getByMepName(String key) {
        if (mepNameHash == null) {
            initializeMepNameHash();
        }
        
        return (EquipmentType)mepNameHash.get(key);
    }
    
    public static void initializeMepNameHash() {
        if (allTypes == null) {
            initializeTypes();
        }
        
        mepNameHash = new Hashtable(allTypes.size());
        
        for (Enumeration i = allTypes.elements(); i.hasMoreElements();) {
            EquipmentType type = (EquipmentType)i.nextElement();
            
            mepNameHash.put(type.getMepName(), type);
        }
    }
    
    public static EquipmentType getByMtfName(String key) {
        if (mtfNameHash == null) {
            initializeMtfNameHash();
        }
        
        return (EquipmentType)mtfNameHash.get(key);
    }
    
    public static void initializeMtfNameHash() {
        if (allTypes == null) {
            initializeTypes();
        }
        
        mtfNameHash = new Hashtable(allTypes.size());
        
        for (Enumeration i = allTypes.elements(); i.hasMoreElements();) {
            EquipmentType type = (EquipmentType)i.nextElement();
            
            mtfNameHash.put(type.getMtfName(), type);
        }
    }

    public static EquipmentType getByTdbName(String key, boolean isClan) {
        if (tdbNameHash == null) {
            initializeTdbNameHash();
        }

        EquipmentType equip = (EquipmentType)tdbNameHash.get(key);
        if (equip == null) {
            if (isClan)
                key = "Clan " + key;
            else
                key = "IS " + key;
            return (EquipmentType)tdbNameHash.get(key);
        } else {
            return equip;
        }
    }
    
    public static void initializeTdbNameHash() {
        if (allTypes == null) {
            initializeTypes();
        }
        
        tdbNameHash = new Hashtable(allTypes.size());
        
        for (Enumeration i = allTypes.elements(); i.hasMoreElements();) {
            EquipmentType type = (EquipmentType)i.nextElement();
            
            tdbNameHash.put(type.getTdbName(), type);
        }
    }
}

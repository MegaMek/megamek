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
    
    
    protected String    name;

    protected String    internalName;
    protected String    mepName;
    protected String    mtfName;

    protected float     tonnage;
    protected int       criticals;
      
    protected int       bv; // battle value point system
    
    // static list of eq
    protected static Vector allTypes;
    protected static Hashtable internalNameHash;
    protected static Hashtable mepNameHash;
    protected static Hashtable mtfNameHash;

    /** Creates new EquipmentType */
    public EquipmentType() {
        ;
    }
    
    public static void initializeTypes() {
        allTypes = new Vector();
        // will I need any others?
        WeaponType.initializeTypes();
        AmmoType.initializeTypes();
        MiscType.initializeTypes();
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
    
    
    
    
    
    
    
    public String getName() {
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
    
    public float getTonnage(Entity entity) {
        return tonnage;
    }

    public int getCriticals(Entity entity) {
        return criticals;
    }

    public int getBV(Entity entity) {
        return bv;
    }

}

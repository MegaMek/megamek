/**
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

package megamek.common;

import java.util.*;

/**
 * A type of mech or vehicle weapon.  There is only one instance of this
 * weapon for all weapons of this type.
 */
public class Weapon
{
    public static final int     DAMAGE_MISSILE = -2;
    public static final int     WEAPON_NA = Integer.MIN_VALUE;

    private String  name;
    private String  mepName;
    	
    private int     heat;
    private int     damage;
    private int     rackSize; // or AC size, or whatever
    private int     ammoType;
    	
    private int     minimumRange;
    private int     shortRange;
    private int     mediumRange;
    private int     longRange;
    	
    private float   tonnage;
    private int     criticals;
      
    private int     bv; // battle value point system
    
    // static stuff:
    private static Vector allWeapons = new Vector();
    private static Hashtable weaponsByName = new Hashtable();
    private static Hashtable weaponsByMepName = new Hashtable();
	
	private Weapon() {
        ;
	}
    
    public String getName() {
        return name;
    }
  
    public String getMepName() {
        return mepName;
    }
  
    public int getHeat() {
        return heat;
    }
  
    public int getDamage() {
        return damage;
    }
  
    public int getRackSize() {
        return rackSize;
    }
    
    public int getAmmoType() {
        return ammoType;
    }
    
    public int getMinimumRange() {
        return minimumRange;
    }
  
    public int getShortRange() {
        return shortRange;
    }
  
    public int getMediumRange() {
        return mediumRange;
    }
  
    public int getLongRange() {
        return longRange;
    }
    
    public int getCriticals() {
        return criticals;
    }
  
    public int getBV() {
        return bv;
    }
    
    
    /**
     * Add all the types of weapons we can create to the allWeapons list
     */
    private static void initializeAllWeapons() {
        allWeapons.removeAllElements();
        
        // all tech level 1 weapons
        allWeapons.addElement(createFlamer());
        allWeapons.addElement(createSmallLaser());
        allWeapons.addElement(createMediumLaser());
        allWeapons.addElement(createLargeLaser());
        allWeapons.addElement(createPPC());
        allWeapons.addElement(createAC2());
        allWeapons.addElement(createAC5());
        allWeapons.addElement(createAC10());
        allWeapons.addElement(createAC20());
        allWeapons.addElement(createMG());
        allWeapons.addElement(createLRM5());
        allWeapons.addElement(createLRM10());
        allWeapons.addElement(createLRM15());
        allWeapons.addElement(createLRM20());
        allWeapons.addElement(createSRM2());
        allWeapons.addElement(createSRM4());
        allWeapons.addElement(createSRM6());
    }
    
    /**
     * Initialize the table of weapons keyed by name
     */
    private static void initializeWeaponsByName() {
        if (allWeapons.size() == 0) {
            initializeAllWeapons();
        }
        
        weaponsByName.clear();
        
        for (Enumeration i = allWeapons.elements(); i.hasMoreElements();) {
            Weapon weapon = (Weapon)i.nextElement();
            
            weaponsByName.put(weapon.getName(), weapon);
        }
    }
    
    /**
     * Gets the weapon by its name
     */
    public static Weapon getWeaponByName(String name) {
        if (weaponsByName.size() == 0) {
            initializeWeaponsByName();
        }
        
        return (Weapon)weaponsByName.get(name);
    }
  
    /**
     * Initialize the table of weapons keyed by .mep name
     */
    private static void initializeWeaponsByMepName() {
        if (allWeapons.size() == 0) {
            initializeAllWeapons();
        }
        
        weaponsByMepName.clear();
        
        for (Enumeration i = allWeapons.elements(); i.hasMoreElements();) {
            Weapon weapon = (Weapon)i.nextElement();
            
            weaponsByMepName.put(weapon.getMepName(), weapon);
        }
    }
    
    /**
     * Gets the weapon by its .MEP name
     */
    public static Weapon getWeaponByMepName(String mepName) {
        if (weaponsByMepName.size() == 0) {
            initializeWeaponsByMepName();
        }
        
        return (Weapon)weaponsByMepName.get(mepName);
    }
  
	
	public static Weapon createFlamer() {
		Weapon weapon = new Weapon();
		
		weapon.name = "Flamer";
		weapon.mepName = weapon.name;
		weapon.heat = 3;
		weapon.damage = 2;
		weapon.ammoType = Ammo.TYPE_NA;
		weapon.minimumRange = WEAPON_NA;
		weapon.shortRange = 1;
		weapon.mediumRange = 2;
		weapon.longRange = 3;
		weapon.tonnage = 1.0f;
		weapon.criticals = 1;
        weapon.bv = 9;
		
		return weapon;
	}
	
	public static Weapon createLargeLaser() {
		Weapon weapon = new Weapon();
		
		weapon.name = "Large Laser";
		weapon.mepName = weapon.name;
		weapon.heat = 8;
		weapon.damage = 8;
		weapon.ammoType = Ammo.TYPE_NA;
		weapon.minimumRange = WEAPON_NA;
		weapon.shortRange = 5;
		weapon.mediumRange = 10;
		weapon.longRange = 15;
		weapon.tonnage = 5.0f;
		weapon.criticals = 2;
        weapon.bv = 124;
		
		return weapon;
	}
	
	public static Weapon createMediumLaser() {
		Weapon weapon = new Weapon();
		
		weapon.name = "Medium Laser";
		weapon.mepName = weapon.name;
		weapon.heat = 3;
		weapon.damage = 5;
		weapon.ammoType = Ammo.TYPE_NA;
		weapon.minimumRange = WEAPON_NA;
		weapon.shortRange = 3;
		weapon.mediumRange = 6;
		weapon.longRange = 9;
		weapon.tonnage = 1.0f;
		weapon.criticals = 1;
        weapon.bv = 46;
		
		return weapon;
	}
	
	public static Weapon createSmallLaser() {
		Weapon weapon = new Weapon();
		
		weapon.name = "Small Laser";
        weapon.mepName = weapon.name;
		weapon.heat = 1;
		weapon.damage = 3;
		weapon.ammoType = Ammo.TYPE_NA;
		weapon.minimumRange = WEAPON_NA;
		weapon.shortRange = 1;
		weapon.mediumRange = 2;
		weapon.longRange = 3;
		weapon.tonnage = 0.5f;
		weapon.criticals = 1;
        weapon.bv = 9;
		
		return weapon;
	}
	
 	public static Weapon createPPC() {
		Weapon weapon = new Weapon();
		
		weapon.name = "Particle Cannon";
        weapon.mepName = "PPC";
		weapon.heat = 10;
		weapon.damage = 10;
		weapon.ammoType = Ammo.TYPE_NA;
		weapon.minimumRange = 3;
		weapon.shortRange = 6;
		weapon.mediumRange = 12;
		weapon.longRange = 18;
		weapon.tonnage = 7.0f;
		weapon.criticals = 3;
        weapon.bv = 176;
		
		return weapon;
	}
	
	public static Weapon createMG() {
		Weapon weapon = new Weapon();
		
		weapon.name = "Machine Gun";
        weapon.mepName = weapon.name;
		weapon.heat = 0;
		weapon.damage = 2;
		weapon.rackSize = 2;
		weapon.ammoType = Ammo.TYPE_MG;
		weapon.minimumRange = WEAPON_NA;
		weapon.shortRange = 1;
		weapon.mediumRange = 2;
		weapon.longRange = 3;
		weapon.tonnage = 0.5f;
		weapon.criticals = 1;
        weapon.bv = 5;
		
		return weapon;
	}
	
	public static Weapon createAC2() {
		Weapon weapon = new Weapon();
		
		weapon.name = "Auto Cannon/2";
        weapon.mepName = weapon.name;
		weapon.heat = 1;
		weapon.damage = 2;
		weapon.rackSize = 2;
		weapon.ammoType = Ammo.TYPE_AC;
		weapon.minimumRange = 4;
		weapon.shortRange = 8;
		weapon.mediumRange = 16;
		weapon.longRange = 24;
		weapon.tonnage = 6.0f;
		weapon.criticals = 1;
        weapon.bv = 37;
		
		return weapon;
	}
	
	public static Weapon createAC5() {
		Weapon weapon = new Weapon();
		
		weapon.name = "Auto Cannon/5";
        weapon.mepName = weapon.name;
		weapon.heat = 1;
		weapon.damage = 5;
		weapon.rackSize = 5;
		weapon.ammoType = Ammo.TYPE_AC;
		weapon.minimumRange = 3;
		weapon.shortRange = 6;
		weapon.mediumRange = 12;
		weapon.longRange = 18;
		weapon.tonnage = 8.0f;
		weapon.criticals = 4;
        weapon.bv = 70;
		
		return weapon;
	}
	
	public static Weapon createAC10() {
		Weapon weapon = new Weapon();
		
		weapon.name = "Auto Cannon/10";
        weapon.mepName = weapon.name;
		weapon.heat = 3;
		weapon.damage = 10;
		weapon.rackSize = 10;
		weapon.ammoType = Ammo.TYPE_AC;
		weapon.minimumRange = WEAPON_NA;
		weapon.shortRange = 5;
		weapon.mediumRange = 10;
		weapon.longRange = 15;
		weapon.tonnage = 12.0f;
		weapon.criticals = 7;
        weapon.bv = 124;
		
		return weapon;
	}
	
	public static Weapon createAC20() {
		Weapon weapon = new Weapon();
		
		weapon.name = "Auto Cannon/20";
        weapon.mepName = weapon.name;
		weapon.heat = 7;
		weapon.damage = 20;
		weapon.rackSize = 20;
		weapon.ammoType = Ammo.TYPE_AC;
		weapon.minimumRange = 0;
		weapon.shortRange = 3;
		weapon.mediumRange = 6;
		weapon.longRange = 9;
		weapon.tonnage = 14.0f;
		weapon.criticals = 10;
        weapon.bv = 178;
		
		return weapon;
	}
	
	public static Weapon createLRM5() {
		Weapon weapon = new Weapon();
		
		weapon.name = "LRM 5";
        weapon.mepName = "LRM-5";
		weapon.heat = 2;
		weapon.damage = DAMAGE_MISSILE;
		weapon.rackSize = 5;
		weapon.ammoType = Ammo.TYPE_LRM;
		weapon.minimumRange = 6;
		weapon.shortRange = 7;
		weapon.mediumRange = 14;
		weapon.longRange = 21;
		weapon.tonnage = 2.0f;
		weapon.criticals = 1;
        weapon.bv = 45;
		
		return weapon;
	}
	
	public static Weapon createLRM10() {
		Weapon weapon = new Weapon();
		
		weapon.name = "LRM 10";
        weapon.mepName = "LRM-10";
		weapon.heat = 4;
		weapon.damage = DAMAGE_MISSILE;
		weapon.rackSize = 10;
		weapon.ammoType = Ammo.TYPE_LRM;
		weapon.minimumRange = 6;
		weapon.shortRange = 7;
		weapon.mediumRange = 14;
		weapon.longRange = 21;
		weapon.tonnage = 5.0f;
		weapon.criticals = 2;
        weapon.bv = 90;
		
		return weapon;
	}
	
	public static Weapon createLRM15() {
		Weapon weapon = new Weapon();
		
		weapon.name = "LRM 15";
        weapon.mepName = "LRM-15";
		weapon.heat = 5;
		weapon.damage = DAMAGE_MISSILE;
		weapon.rackSize = 15;
		weapon.ammoType = Ammo.TYPE_LRM;
		weapon.minimumRange = 6;
		weapon.shortRange = 7;
		weapon.mediumRange = 14;
		weapon.longRange = 21;
		weapon.tonnage = 7.0f;
		weapon.criticals = 3;
        weapon.bv = 136;
		
		return weapon;
	}
	
	public static Weapon createLRM20() {
		Weapon weapon = new Weapon();
		
		weapon.name = "LRM 20";
        weapon.mepName = "LRM-20";
		weapon.heat = 6;
		weapon.damage = DAMAGE_MISSILE;
		weapon.rackSize = 20;
		weapon.ammoType = Ammo.TYPE_LRM;
		weapon.minimumRange = 6;
		weapon.shortRange = 7;
		weapon.mediumRange = 14;
		weapon.longRange = 21;
		weapon.tonnage = 10.0f;
		weapon.criticals = 5;
        weapon.bv = 181;
		
		return weapon;
	}
	
	public static Weapon createSRM2() {
		Weapon weapon = new Weapon();
		
		weapon.name = "SRM 2";
        weapon.mepName = "SRM-2";
		weapon.heat = 2;
		weapon.damage = DAMAGE_MISSILE;
		weapon.rackSize = 2;
		weapon.ammoType = Ammo.TYPE_SRM;
		weapon.minimumRange = 0;
		weapon.shortRange = 3;
		weapon.mediumRange = 6;
		weapon.longRange = 9;
		weapon.tonnage = 1.0f;
		weapon.criticals = 1;
        weapon.bv = 21;
		
		return weapon;
	}
	
	public static Weapon createSRM4() {
		Weapon weapon = new Weapon();
		
		weapon.name = "SRM 4";
        weapon.mepName = "SRM-4";
		weapon.heat = 3;
		weapon.damage = DAMAGE_MISSILE;
		weapon.rackSize = 4;
		weapon.ammoType = Ammo.TYPE_SRM;
		weapon.minimumRange = 0;
		weapon.shortRange = 3;
		weapon.mediumRange = 6;
		weapon.longRange = 9;
		weapon.tonnage = 2.0f;
		weapon.criticals = 1;
        weapon.bv = 39;
		
		return weapon;
	}
	
	public static Weapon createSRM6() {
		Weapon weapon = new Weapon();
		
		weapon.name = "SRM 6";
        weapon.mepName = "SRM-6";
		weapon.heat = 4;
		weapon.damage = DAMAGE_MISSILE;
		weapon.rackSize = 6;
		weapon.ammoType = Ammo.TYPE_SRM;
		weapon.minimumRange = 0;
		weapon.shortRange = 3;
		weapon.mediumRange = 6;
		weapon.longRange = 9;
		weapon.tonnage = 3.0f;
		weapon.criticals = 2;
        weapon.bv = 59;
		
		return weapon;
	}
	
  
  public String toString() {
        return "Weapon: " + name;
  }
}

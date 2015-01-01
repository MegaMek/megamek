package megamek.common;

import java.util.*;

/**
 * Keeps information about a single type of type.  Only one instance
 * exists for all weapons of that type
 */
public class WeaponType
{
    public static final int     DAMAGE_MISSILE = -2;
    public static final int     NONE = Integer.MIN_VALUE;
    
    private String  name;
    	
    private int     heat;
    private int     damage;
    private int     rackSize; // or AC size, or whatever
    private int     ammoType;
    	
    private int     rangeMinimum;
    private int     rangeShort;
    private int     rangeMedium;
    private int     rangeLong;
    	
    private float   tonnage;
    private int     criticals;
      
    private int     bv; // battle value point system
    
    private static Hashtable weaponTypes = new Hashtable();
    
    /**
     * No public constructor.  Call getTypeFor().
     */
    private WeaponType() {
        ;
    }
    
    private void initializeTypes() {
    }
    
	public static WeaponType createFlamer() {
		WeaponType type = new WeaponType();
		
		type.name = "Flamer";
		type.heat = 3;
		type.damage = 2;
		type.ammoType = Ammo.TYPE_NA;
		type.rangeMinimum = NONE;
		type.rangeShort = 1;
		type.rangeMedium = 2;
		type.rangeLong = 3;
		type.tonnage = 1.0f;
		type.criticals = 1;
        type.bv = 9;
		
		return type;
	}
	
	public static WeaponType createLargeLaser() {
		WeaponType type = new WeaponType();
		
		type.name = "Large Laser";
		type.heat = 8;
		type.damage = 8;
		type.ammoType = Ammo.TYPE_NA;
		type.rangeMinimum = NONE;
		type.rangeShort = 5;
		type.rangeMedium = 10;
		type.rangeLong = 15;
		type.tonnage = 5.0f;
		type.criticals = 2;
        type.bv = 124;
		
		return type;
	}
	
	public static WeaponType createMediumLaser() {
		WeaponType type = new WeaponType();
		
		type.name = "Medium Laser";
		type.heat = 3;
		type.damage = 5;
		type.ammoType = Ammo.TYPE_NA;
		type.rangeMinimum = NONE;
		type.rangeShort = 3;
		type.rangeMedium = 6;
		type.rangeLong = 9;
		type.tonnage = 1.0f;
		type.criticals = 1;
        type.bv = 46;
		
		return type;
	}
	
	public static WeaponType createSmallLaser() {
		WeaponType type = new WeaponType();
		
		type.name = "Small Laser";
		type.heat = 1;
		type.damage = 3;
		type.ammoType = Ammo.TYPE_NA;
		type.rangeMinimum = NONE;
		type.rangeShort = 1;
		type.rangeMedium = 2;
		type.rangeLong = 3;
		type.tonnage = 0.5f;
		type.criticals = 1;
        type.bv = 9;
		
		return type;
	}
	
 	public static WeaponType createPPC() {
		WeaponType type = new WeaponType();
		
		type.name = "Particle Cannon";
		type.heat = 10;
		type.damage = 10;
		type.ammoType = Ammo.TYPE_NA;
		type.rangeMinimum = 3;
		type.rangeShort = 6;
		type.rangeMedium = 12;
		type.rangeLong = 18;
		type.tonnage = 7.0f;
		type.criticals = 3;
        type.bv = 176;
		
		return type;
	}
	
	public static WeaponType createMG() {
		WeaponType type = new WeaponType();
		
		type.name = "Machine Gun";
		type.heat = 0;
		type.damage = 2;
		type.rackSize = 2;
		type.ammoType = Ammo.TYPE_MG;
		type.rangeMinimum = NONE;
		type.rangeShort = 1;
		type.rangeMedium = 2;
		type.rangeLong = 3;
		type.tonnage = 0.5f;
		type.criticals = 1;
        type.bv = 5;
		
		return type;
	}
	
	public static WeaponType createAC2() {
		WeaponType type = new WeaponType();
		
		type.name = "Auto Cannon/2";
		type.heat = 1;
		type.damage = 2;
		type.rackSize = 2;
		type.ammoType = Ammo.TYPE_AC;
		type.rangeMinimum = 4;
		type.rangeShort = 8;
		type.rangeMedium = 16;
		type.rangeLong = 24;
		type.tonnage = 6.0f;
		type.criticals = 1;
        type.bv = 37;
		
		return type;
	}
	
	public static WeaponType createAC5() {
		WeaponType type = new WeaponType();
		
		type.name = "Auto Cannon/5";
		type.heat = 1;
		type.damage = 5;
		type.rackSize = 5;
		type.ammoType = Ammo.TYPE_AC;
		type.rangeMinimum = 3;
		type.rangeShort = 6;
		type.rangeMedium = 12;
		type.rangeLong = 18;
		type.tonnage = 8.0f;
		type.criticals = 4;
        type.bv = 70;
		
		return type;
	}
	
	public static WeaponType createAC10() {
		WeaponType type = new WeaponType();
		
		type.name = "Auto Cannon/10";
		type.heat = 3;
		type.damage = 10;
		type.rackSize = 10;
		type.ammoType = Ammo.TYPE_AC;
		type.rangeMinimum = NONE;
		type.rangeShort = 5;
		type.rangeMedium = 10;
		type.rangeLong = 15;
		type.tonnage = 12.0f;
		type.criticals = 7;
        type.bv = 124;
		
		return type;
	}
	
	public static WeaponType createAC20() {
		WeaponType type = new WeaponType();
		
		type.name = "Auto Cannon/20";
		type.heat = 7;
		type.damage = 20;
		type.rackSize = 20;
		type.ammoType = Ammo.TYPE_AC;
		type.rangeMinimum = 0;
		type.rangeShort = 3;
		type.rangeMedium = 6;
		type.rangeLong = 9;
		type.tonnage = 14.0f;
		type.criticals = 10;
        type.bv = 178;
		
		return type;
	}
	
	public static WeaponType createLRM5() {
		WeaponType type = new WeaponType();
		
		type.name = "LRM 5";
		type.heat = 2;
		type.damage = DAMAGE_MISSILE;
		type.rackSize = 5;
		type.ammoType = Ammo.TYPE_LRM;
		type.rangeMinimum = 6;
		type.rangeShort = 7;
		type.rangeMedium = 14;
		type.rangeLong = 21;
		type.tonnage = 2.0f;
		type.criticals = 1;
        type.bv = 45;
		
		return type;
	}
	
	public static WeaponType createLRM10() {
		WeaponType type = new WeaponType();
		
		type.name = "LRM 10";
		type.heat = 4;
		type.damage = DAMAGE_MISSILE;
		type.rackSize = 10;
		type.ammoType = Ammo.TYPE_LRM;
		type.rangeMinimum = 6;
		type.rangeShort = 7;
		type.rangeMedium = 14;
		type.rangeLong = 21;
		type.tonnage = 5.0f;
		type.criticals = 2;
        type.bv = 90;
		
		return type;
	}
	
	public static WeaponType createLRM15() {
		WeaponType type = new WeaponType();
		
		type.name = "LRM 15";
		type.heat = 5;
		type.damage = DAMAGE_MISSILE;
		type.rackSize = 15;
		type.ammoType = Ammo.TYPE_LRM;
		type.rangeMinimum = 6;
		type.rangeShort = 7;
		type.rangeMedium = 14;
		type.rangeLong = 21;
		type.tonnage = 7.0f;
		type.criticals = 3;
        type.bv = 136;
		
		return type;
	}
	
	public static WeaponType createLRM20() {
		WeaponType type = new WeaponType();
		
		type.name = "LRM 20";
		type.heat = 6;
		type.damage = DAMAGE_MISSILE;
		type.rackSize = 20;
		type.ammoType = Ammo.TYPE_LRM;
		type.rangeMinimum = 6;
		type.rangeShort = 7;
		type.rangeMedium = 14;
		type.rangeLong = 21;
		type.tonnage = 10.0f;
		type.criticals = 5;
        type.bv = 181;
		
		return type;
	}
	
	public static WeaponType createSRM2() {
		WeaponType type = new WeaponType();
		
		type.name = "SRM 2";
		type.heat = 2;
		type.damage = DAMAGE_MISSILE;
		type.rackSize = 2;
		type.ammoType = Ammo.TYPE_SRM;
		type.rangeMinimum = 0;
		type.rangeShort = 3;
		type.rangeMedium = 6;
		type.rangeLong = 9;
		type.tonnage = 1.0f;
		type.criticals = 1;
        type.bv = 21;
		
		return type;
	}
	
	public static WeaponType createSRM4() {
		WeaponType type = new WeaponType();
		
		type.name = "SRM 4";
		type.heat = 3;
		type.damage = DAMAGE_MISSILE;
		type.rackSize = 4;
		type.ammoType = Ammo.TYPE_SRM;
		type.rangeMinimum = 0;
		type.rangeShort = 3;
		type.rangeMedium = 6;
		type.rangeLong = 9;
		type.tonnage = 2.0f;
		type.criticals = 1;
        type.bv = 39;
		
		return type;
	}
	
	public static WeaponType createSRM6() {
		WeaponType type = new WeaponType();
		
		type.name = "SRM 6";
		type.heat = 4;
		type.damage = DAMAGE_MISSILE;
		type.rackSize = 6;
		type.ammoType = Ammo.TYPE_SRM;
		type.rangeMinimum = 0;
		type.rangeShort = 3;
		type.rangeMedium = 6;
		type.rangeLong = 9;
		type.tonnage = 3.0f;
		type.criticals = 2;
        type.bv = 59;
		
		return type;
	}}

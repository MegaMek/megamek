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

import java.io.Serializable;

public class Ammo
  implements Serializable
{
    public static final int    TYPE_NA = 0;
    public static final int    TYPE_MG = 1;
    public static final int    TYPE_AC = 2;
    public static final int    TYPE_LRM = 3;
    public static final int    TYPE_SRM = 4;
  
  private String name;
  
  public int damagePerShot;
  public int rackSize;
  public int type;
  public int shots;
  public float weight = 1.0f;
  public int location;
  
  public int bv; // batle value
  
  public transient boolean exploded = false; // server only
  
  public Ammo() {
    ;
  }
  
  /**
   * Return the name of this ammunition
   */
  public String getName() {
    return name + " (" + shots + ")";
  }
  
  public int getBV() {
    return bv;
  }
  
  /**
   * Tries to make the ammo specified by the string.
   * 
   * @return the weapon, or null if unrecognized
   */
  public static Ammo makeAmmo(String s) {
    if(s.equalsIgnoreCase("Ammo AC/2")) {
      return makeAC2Ammo();
    } else if(s.equalsIgnoreCase("Ammo AC/5")) {
      return makeAC5Ammo();
    } else if(s.equalsIgnoreCase("Ammo AC/10")) {
      return makeAC10Ammo();
    } else if(s.equalsIgnoreCase("Ammo AC/20")) {
      return makeAC20Ammo();
    } else if(s.equalsIgnoreCase("Ammo MG - Full")) {
      return makeMGAmmo();
    } else if(s.equalsIgnoreCase("Ammo MG - Half")) {
      return makeMGAmmoHalf();
    } else if(s.equalsIgnoreCase("Ammo LRM-5")) {
      return makeLRM5Ammo();
    } else if(s.equalsIgnoreCase("Ammo LRM-10")) {
      return makeLRM10Ammo();
    } else if(s.equalsIgnoreCase("Ammo LRM-15")) {
      return makeLRM15Ammo();
    } else if(s.equalsIgnoreCase("Ammo LRM-20")) {
      return makeLRM20Ammo();
    } else if(s.equalsIgnoreCase("Ammo SRM-2")) {
      return makeSRM2Ammo();
    } else if(s.equalsIgnoreCase("Ammo SRM-4")) {
      return makeSRM4Ammo();
    } else if(s.equalsIgnoreCase("Ammo SRM-6")) {
      return makeSRM6Ammo();
    } else {
      return null;
    }
  }
    
  public static Ammo makeMGAmmo() {
    Ammo ammo = new Ammo();
    
    ammo.name = "Machine Gun Ammo";
    ammo.damagePerShot = 1;
    ammo.rackSize = 2;
    ammo.type = Ammo.TYPE_MG;
    ammo.shots = 200;
    ammo.bv = 1;
    
    return ammo;
  }
  
  public static Ammo makeMGAmmoHalf() {
    Ammo ammo = new Ammo();
    
    ammo.name = "Machine Gun Ammo";
    ammo.damagePerShot = 1;
    ammo.rackSize = 2;
    ammo.type = Ammo.TYPE_MG;
    ammo.shots = 100;
    ammo.bv = 1;
    ammo.weight = 0.5f;
    
    return ammo;
  }
  
  public static Ammo makeAC2Ammo() {
    Ammo ammo = new Ammo();
    
    ammo.name = "AC/2 Ammo";
    ammo.damagePerShot = 1;
    ammo.rackSize = 2;
    ammo.type = Ammo.TYPE_AC;
    ammo.shots = 45;
    ammo.bv = 5;
    
    return ammo;
  }
  
  public static Ammo makeAC5Ammo() {
    Ammo ammo = new Ammo();
    
    ammo.name = "AC/5 Ammo";
    ammo.damagePerShot = 1;
    ammo.rackSize = 5;
    ammo.type = Ammo.TYPE_AC;
    ammo.shots = 20;
    ammo.bv = 9;
    
    return ammo;
  }
  
  public static Ammo makeAC10Ammo() {
    Ammo ammo = new Ammo();
    
    ammo.name = "AC/10 Ammo";
    ammo.damagePerShot = 1;
    ammo.rackSize = 10;
    ammo.type = Ammo.TYPE_AC;
    ammo.shots = 10;
    ammo.bv = 15;
    
    return ammo;
  }
  
  public static Ammo makeAC20Ammo() {
    Ammo ammo = new Ammo();
    
    ammo.name = "AC/20 Ammo";
    ammo.damagePerShot = 1;
    ammo.rackSize = 20;
    ammo.type = Ammo.TYPE_AC;
    ammo.shots = 5;
    ammo.bv = 20;
    
    return ammo;
  }
  
  public static Ammo makeLRM5Ammo() {
    Ammo ammo = new Ammo();
    
    ammo.name = "LRM 5 Ammo";
    ammo.damagePerShot = 1;
    ammo.rackSize = 5;
    ammo.type = Ammo.TYPE_LRM;
    ammo.shots = 24;
    ammo.bv = 6;
    
    return ammo;
  }
  
  public static Ammo makeLRM10Ammo() {
    Ammo ammo = new Ammo();
    
    ammo.name = "LRM 10 Ammo";
    ammo.damagePerShot = 1;
    ammo.rackSize = 10;
    ammo.type = Ammo.TYPE_LRM;
    ammo.shots = 12;
    ammo.bv = 11;
    
    return ammo;
  }
  
  public static Ammo makeLRM15Ammo() {
    Ammo ammo = new Ammo();
    
    ammo.name = "LRM 15 Ammo";
    ammo.damagePerShot = 1;
    ammo.rackSize = 15;
    ammo.type = Ammo.TYPE_LRM;
    ammo.shots = 8;
    ammo.bv = 17;
    
    return ammo;
  }
  
  public static Ammo makeLRM20Ammo() {
    Ammo ammo = new Ammo();
    
    ammo.name = "LRM 20 Ammo";
    ammo.damagePerShot = 1;
    ammo.rackSize = 20;
    ammo.type = Ammo.TYPE_LRM;
    ammo.shots = 6;
    ammo.bv = 23;
    
    return ammo;
  }
  
  public static Ammo makeSRM2Ammo() {
    Ammo ammo = new Ammo();
    
    ammo.name = "SRM 2 Ammo";
    ammo.damagePerShot = 2;
    ammo.rackSize = 2;
    ammo.type = Ammo.TYPE_SRM;
    ammo.shots = 50;
    ammo.bv = 3;
    
    return ammo;
  }
  
  public static Ammo makeSRM4Ammo() {
    Ammo ammo = new Ammo();
    
    ammo.name = "SRM 4 Ammo";
    ammo.damagePerShot = 2;
    ammo.rackSize = 4;
    ammo.type = Ammo.TYPE_SRM;
    ammo.shots = 25;
    ammo.bv = 5;
    
    return ammo;
  }
  
  public static Ammo makeSRM6Ammo() {
    Ammo ammo = new Ammo();
    
    ammo.name = "SRM 6 Ammo";
    ammo.damagePerShot = 2;
    ammo.rackSize = 6;
    ammo.type = Ammo.TYPE_SRM;
    ammo.shots = 15;
    ammo.bv = 7;
    
    return ammo;
  }
  
  public String toString() {
    return "Ammo: " + name;
  }
}

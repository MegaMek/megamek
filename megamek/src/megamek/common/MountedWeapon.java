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

import java.io.*;

public class MountedWeapon
    implements Serializable 
{
    private boolean             ready = true;
    private boolean             destroyed = false;
    private int                 location = Entity.LOC_NONE;
    private boolean             rearMounted = false;
    private Ammo                ammoFeed = null;
    private boolean             firedThisRound = false;
    
    private transient Weapon    weapon;
    private String              weaponName;
    
    /**
     * Create w/ weapon.  Makes a front-mounted weapon
     */
    public MountedWeapon(Weapon weapon) {
        this(weapon, false);
    }
    
    /**
     * Create with weapon, rear specified
     */
    public MountedWeapon(Weapon weapon, boolean rearMounted) {
        this.weapon = weapon;
        this.weaponName = weapon.getName();
        this.rearMounted = rearMounted;
    }
    
    /**
     * Restores the weapon from the name
     */
    public void restore() {
        this.weapon = Weapon.getWeaponByName(weaponName);
        
        if (this.weapon == null) {
            System.err.println("MountedWeapon.restore: could not restore weapon \"" + weaponName + "\"");
        }
    }
    
    public String getName() {
        return weapon.getName() + (rearMounted ? " (R)" : "");
    }
    
    public Weapon getType() {
        return weapon;
    }
    
    public boolean isReady() {
        return ready;
    }
    
    public void setReady(boolean ready) {
        this.ready = ready;
    }
    
    public boolean isDestroyed() {
        return destroyed;
    }
    
    public void setDestroyed(boolean destroyed) {
        this.destroyed = destroyed;
    }
    
    public int getLocation() {
        return location;
    }
    
    public void setLocation(int location) {
        this.location = location;
    }
    
    public boolean isRearMounted() {
        return rearMounted;
    }
    
    public void setRearMounted(boolean rearMounted) {
        this.rearMounted = rearMounted;
    }
    
    public Ammo getAmmoFeed() {
        return ammoFeed;
    }
    
    public void setAmmoFeed(Ammo ammoFeed) {
        this.ammoFeed = ammoFeed;
    }
    
    public boolean isFiredThisRound() {
        return firedThisRound;
    }
    public void setFiredThisRound(boolean firedThisRound) {
        this.firedThisRound = firedThisRound;
    }
}

/*
 * MegaMek -
 * Copyright (C) 2000,2001,2002,2003,2004,2005 Ben Mazur (bmazur@sev.org)
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
 * Mounted.java
 *
 * Created on April 1, 2002, 1:29 PM
 */

package megamek.common;

import java.io.Serializable;

/**
 * This describes equipment mounted on a mech.
 *
 * @author  Ben
 * @version
 */
public class Mounted implements Serializable, RoundUpdated {

    private boolean usedThisRound = false;
    private boolean destroyed = false;
    private boolean hit = false;
    private boolean missing = false;
    private boolean jammed = false;
    private boolean useless = false;
    private boolean fired = false; //Only true for used OS stuff.
    private boolean rapidfire = false; //MGs in rapid-fire mode

    private int mode; //Equipment's current state.  On or Off.  Sixshot or Fourshot, etc
    private int pendingMode = -1; // if mode changes happen at end of turn

    private int location;
    private boolean rearMounted;

    private Mounted linked = null; // for ammo, or artemis
    private Mounted linkedBy = null; // reverse link for convenience

    private Entity entity; // what I'm mounted on

    private transient EquipmentType type;
    private String typeName;

    // ammo-specific stuff.  Probably should be a subclass
    private int shotsLeft;
    private boolean m_bPendingDump;
    private boolean m_bDumping;

    // handle split weapons
    private boolean bSplit = false;
    private int nFoundCrits = 0;


    /** Creates new Mounted */
    public Mounted(Entity entity, EquipmentType type) {
        this.entity = entity;
        this.type = type;
        this.typeName = type.getInternalName();

        if (type instanceof AmmoType) {
            shotsLeft = ((AmmoType)type).getShots();
        }
    }

    /**
     * Changing ammo loadouts allows updating AmmoTypes of existing bins.
     * This is the only circumstance under which this should happen.
     */

    public void changeAmmoType(AmmoType at) {
        if ( !(type instanceof AmmoType)) {
            System.out.println("Attempted to change ammo type of non-ammo");
            return;
        }
        this.type = at;
        this.typeName = at.getInternalName();
        if (this.location == Entity.LOC_NONE) {
            //Oneshot launcher
            shotsLeft = 1;
        } else {
            //Regular launcher
            shotsLeft = at.getShots();
        }
    }

    /**
     * Restores the equipment from the name
     */
    public void restore() {
        this.type = EquipmentType.get(typeName);

        if (this.type == null) {
            System.err.println("Mounted.restore: could not restore equipment type \"" + typeName + "\"");
        }
    }

    public EquipmentType getType() {
        return type;
    }

    /**
     * 
     * @return the current mode of the equipment, or <code>null</code> 
     * if it's not available.
     */
    public EquipmentMode curMode() {
        if (mode >= 0 && mode < type.getModesCount())
            return type.getMode(mode);
        else
            return null;
    }

    /**
     * 
     * @return the pending mode of the equipment.
     */
    public EquipmentMode pendingMode() {
        if (pendingMode < 0 || pendingMode >= type.getModesCount()) {
            return EquipmentMode.getMode("None");
        } else {        
            return type.getMode(pendingMode);
        }
    }

    /**
     * Switches the equipment mode to the next available.
     * @return new mode number, or <code>-1</code> if it's not available.
     */
    public int switchMode() {
        if (type.hasModes()) {
            int nMode = 0;
            if (pendingMode > -1) {
                nMode = (pendingMode + 1) % type.getModesCount();
            }
            else {
                nMode = (mode + 1) % type.getModesCount();
            }
            setMode(nMode);
            return nMode;
        }
        return -1;
    }

    /**
     * Sets the equipment mode to the mode denoted by the given mode name  
     * @param newMode the name of the desired new mode
     * @return new mode number on success, <code>-1<code> otherwise.
     */
    public int setMode(String newMode) {
        for (int x = 0, e = type.getModesCount(); x < e; x++) {
            if (type.getMode(x).equals(newMode)) {
                setMode(x);
                return x;
            }
        }
        return -1;
    }

    /**
     * Sets the equipment mode to the mode denoted by the given mode number
     * @param newMode the number of the desired new mode
     */
    public void setMode(int newMode) {
        if (type.hasModes()) {
            megamek.debug.Assert.assertTrue(newMode >= 0 && newMode < type.getModesCount(), 
            "Invalid mode, mode="+newMode+", modesCount="+type.getModesCount());            
            if (type.hasInstantModeSwitch()) {
                mode = newMode;
            }
            else if (pendingMode != newMode) {
                pendingMode = newMode;
            }
        }
    }

    public void newRound(int roundNumber) {
        setUsedThisRound(false);
        if (type.hasModes() && pendingMode != -1) {
            mode = pendingMode;
            pendingMode = -1;
        }
    }


    /**
     * Shortcut to type.getName()
     */
    public String getName() {
        return type.getName();
    }

    public String getDesc() {
        StringBuffer desc = new StringBuffer(type.getDesc());
        if (destroyed) {
            desc.insert(0, "*");
        } else if (useless) {
            desc.insert(0, "x ");
        } else if (usedThisRound) {
            desc.insert(0, "+");
        } else if (jammed) {
            desc.insert(0, "j ");
        } else if (fired) {
            desc.insert(0, "x ");
        }
        if (rearMounted) {
            desc.append(" (R)");
        }
        if (type instanceof AmmoType &&
            location != Entity.LOC_NONE) {

            desc.append(" (");
            desc.append(shotsLeft);
            desc.append(")");
        }
        return desc.toString();
    }

    public boolean isReady() {
        return !usedThisRound && !destroyed && !jammed && !useless;
    }

    public boolean isUsedThisRound() {
        return usedThisRound;
    }

    public void setUsedThisRound(boolean usedThisRound) {
        this.usedThisRound = usedThisRound;
    }

    public boolean isBreached() {
        return useless;
    }

    public void setBreached(boolean breached) {
        this.useless = breached;
    }

    public boolean isDestroyed() {
        return destroyed;
    }

    public void setDestroyed(boolean destroyed) {
        this.destroyed = destroyed;
    }

    public boolean isHit() {
        return hit;
    }

    public void setHit(boolean hit) {
        this.hit = hit;
    }

    public boolean isMissing() {
        return missing;
    }

    public void setMissing(boolean missing) {
        this.missing = missing;
    }

    public boolean isJammed() {
        return jammed;
    }

    public void setJammed(boolean j) {
        jammed = j;
    }

    public int getShotsLeft() {
        return shotsLeft;
    }

    public void setShotsLeft(int shotsLeft) {
        this.shotsLeft = shotsLeft;
    }

    /**
     * Returns how many shots the weapon is using
     */
    public int howManyShots() {
        final WeaponType wtype = (WeaponType)this.getType();
        int nShots = 1;
        // figure out # of shots for variable-shot weapons
        if (wtype.getAmmoType() == AmmoType.T_AC_ULTRA
            && this.curMode().equals("Ultra")) {
            nShots = 2;
        }
        else if (wtype.getAmmoType() == AmmoType.T_AC_ROTARY ||
                 wtype.getInternalName().equals(BattleArmor.MINE_LAUNCHER)) {
            if (this.curMode().equals("2-shot")) {
                nShots = 2;
            } else if (this.curMode().equals("4-shot")) {
                nShots = 4;
            } else if (this.curMode().equals("6-shot")) {
                nShots = 6;
            }
        }
        return nShots;
    }

    public boolean isPendingDump() {
        return m_bPendingDump;
    }

    public void setPendingDump(boolean b) {
        m_bPendingDump = b;
    }

    public boolean isDumping() {
        return m_bDumping;
    }

    public void setDumping(boolean b) {
        m_bDumping = b;
    }
    
    public boolean isRapidfire() {
        return rapidfire;
    }
    
    public void setRapidfire(boolean rapidfire) {
        this.rapidfire = rapidfire;
    }


    public int getLocation() {
        return location;
    }

    public boolean isRearMounted() {
        return rearMounted;
    }

    public void setLocation(int location) {
        setLocation(location, false);
    }

    public void setLocation(int location, boolean rearMounted) {
        this.location = location;
        this.rearMounted = rearMounted;
    }

    public Mounted getLinked() {
        return linked;
    }

    public Mounted getLinkedBy() {
        return linkedBy;
    }

    public void setLinked(Mounted linked) {
        this.linked = linked;
        linked.setLinkedBy(this);
    }

    // should only be called by setLinked()
    // in the case of a many-to-one relationship (like ammo) this is meaningless
    protected void setLinkedBy(Mounted linker) {
        if (linker.getLinked() != this) {
            // liar
            return;
        }
        linkedBy = linker;
    }

    public int getFoundCrits() {
        return nFoundCrits;
    }

    public void setFoundCrits(int n) {
        nFoundCrits = n;
    }

    public boolean isSplit() {
        return bSplit;
    }

    public void setSplit(boolean b) {
        bSplit = b;
    }

    public int getExplosionDamage() {
        if (type instanceof AmmoType) {
            AmmoType atype = (AmmoType)type;
            return atype.getDamagePerShot() * atype.getRackSize() * shotsLeft;
        } else if (type instanceof WeaponType) {
            WeaponType wtype = (WeaponType)type;
            //HACK: gauss rifle damage hardcoding
            if (wtype.getAmmoType() == AmmoType.T_GAUSS) {
                return 20;
            } else if (wtype.getAmmoType() == AmmoType.T_GAUSS_LIGHT) {
                return 16;
            } else if (wtype.getAmmoType() == AmmoType.T_GAUSS_HEAVY) {
                return 25;
            } else if (wtype.getAmmoType() == AmmoType.T_AC_ROTARY ||
                       wtype.getAmmoType() == AmmoType.T_AC) {
                return wtype.getDamage();
            }
        }
        // um, otherwise, I'm not sure
        System.err.println("mounted: unable to determine explosion damage for "
                            + getName());
        return 0;
    }
    public boolean isFired() { //has a oneshot weapon been fired?
      return fired;
    }
    public void setFired(boolean val) {
      fired=val;
    }

    /**
     * Confirm that the given entity can fire the indicated equipment.
     *
     * @return  <code>true</code> if the equipment can be fired by the
     *          entity; <code>false</code> otherwise.
     */
    public boolean canFire() {
    
        // Equipment operational?
        if ( !isReady() || isBreached() || isMissing() ) {
            return false;
        }
    
        // Is the entity even active?
        if ( entity.isShutDown() || !entity.getCrew().isActive() ) {
            return false;
        }
    
        // Otherwise, the equipment can be fired.
        return true;
    }

    /*
     * Returns false if this ammo should not be loaded.  Checks if the
     *    ammo is already destroyed, is being dumped, has been breached, is
     *   already used up, or is locationless (oneshot ammo).
     */
    public boolean isAmmoUsable() {
        if (this.destroyed || this.m_bDumping ||
            this.useless || this.shotsLeft <= 0 ||
            this.location == Entity.LOC_NONE) {
            return false;
        } else {
            return true;
        }
    }

}

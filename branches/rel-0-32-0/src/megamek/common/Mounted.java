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
    private boolean hotloaded = false; //Hotloading for ammoType
    
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
    private int secondLocation = 0;
    
    // mine type
    private int mineType = MINE_NONE;
    // vibrabomb mine setting
    private int vibraSetting = 20;
    
    private int phase = IGame.PHASE_UNKNOWN;
    
    public static final int MINE_NONE              = -1;
    public static final int MINE_CONVENTIONAL      = 0;
    public static final int MINE_VIBRABOMB         = 1;
    public static final int MINE_COMMAND_DETONATED = 2;    

    //New stuff for shields
    protected int baseDamageAbsorptionRate = 0;
    protected int baseDamageCapacity = 0;
    protected int damageTaken = 0;
        

    /** Creates new Mounted */
    public Mounted(Entity entity, EquipmentType type) {
        this.entity = entity;
        this.type = type;
        this.typeName = type.getInternalName();

        if (type instanceof AmmoType) {
            shotsLeft = ((AmmoType)type).getShots();
        }
        if (type instanceof MiscType && type.hasFlag(MiscType.F_MINE)) {
            this.mineType = MINE_CONVENTIONAL; 
        }
        if ( type instanceof MiscType && ((MiscType)type).isShield() ){
                MiscType shield = (MiscType)type;
                baseDamageAbsorptionRate = shield.baseDamageAbsorptionRate;
                baseDamageCapacity = shield.baseDamageCapacity;
                damageTaken = shield.damageTaken;
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
        if (typeName == null)
            typeName = type.getName();
        else
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
		return null;
    }

    /**
     * 
     * @return the pending mode of the equipment.
     */
    public EquipmentMode pendingMode() {
        if (pendingMode < 0 || pendingMode >= type.getModesCount()) {
            return EquipmentMode.getMode("None");
        }
		return type.getMode(pendingMode);
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
        if ((type != null) && (type.hasModes() && pendingMode != -1)) {
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
        StringBuffer desc;
        switch (getMineType()) {
            case 0:
                desc = new StringBuffer(Messages.getString("Mounted.ConventionalMine"));
                break;
            case 1:
                desc = new StringBuffer(Messages.getString("Mounted.VibraBombMine"));
                break;
            case 2:
                desc = new StringBuffer(Messages.getString("Mounted.CommandDetonatedMine"));
                break;
            case -1:
            default:
                desc = new StringBuffer(type.getDesc());
        }
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
        if (usedThisRound) {
            phase = entity.game.getPhase();
        } else {
            phase = IGame.PHASE_UNKNOWN;
        }
    }

    public int usedInPhase () {
        if (usedThisRound) {
            return phase;
        }
		return IGame.PHASE_UNKNOWN;
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
        if (shotsLeft < 0) shotsLeft = 0;
        this.shotsLeft = shotsLeft;
    }

    /**
     * Returns how many shots the weapon is using
     */
    public int howManyShots() {
        final WeaponType wtype = (WeaponType)this.getType();
        int nShots = 1;
        // figure out # of shots for variable-shot weapons
        if (((wtype.getAmmoType() == AmmoType.T_AC_ULTRA)
                || (wtype.getAmmoType() == AmmoType.T_AC_ULTRA_THB))
                && this.curMode().equals("Ultra")) {
            nShots = 2;
        }
        //sets number of shots for AC rapid mode
        else if ( (wtype.getAmmoType() == AmmoType.T_AC || (wtype.getAmmoType() == AmmoType.T_LAC))
                    && wtype.hasModes() && this.curMode().equals("Rapid")) {
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

    /**
     * Checks to see if the current ammo for this weapon is hotloaded
     * @return <code>true</code> if ammo is hotloaded or <code>false</code> if not
     */
    public boolean isHotLoaded(){
     
        boolean isHotLoaded = false;
        
        if ( this.getType() instanceof WeaponType ){
            Mounted link = this.getLinked();
            if ( link == null || !(link.getType() instanceof AmmoType) || link.getShotsLeft() <= 0)
                return false;
            
                
            isHotLoaded = this.getLinked().hotloaded;
            
            //Check to see if the ammo has its mode set to hotloaded.
            //This is for vehicles that can change hotload status during combat.
            if ( !isHotLoaded && this.getLinked().getType().hasModes() && this.getLinked().curMode().equals("HotLoad") )
                isHotLoaded = true;
            
            return isHotLoaded;
        }
    
        if ( this.getType() instanceof AmmoType && this.getShotsLeft() > 0){
            isHotLoaded = this.hotloaded;
            
            //Check to see if the ammo has its mode set to hotloaded.
            //This is for vehicles that can change hotload status during combat.
            if ( !isHotLoaded && this.getType().hasModes() && this.curMode().equals("HotLoad") )
                isHotLoaded = true;
            
            return isHotLoaded;
        }
        
        return false;
    }
    
    /**
     * Sets the hotloading parameter for this weapons ammo.
     * @param hotload
     */
    public void setHotLoad(boolean hotload){
        
        if ( this.getType() instanceof WeaponType ){
            Mounted link = this.getLinked();
            if ( link == null ||  !(link.getType() instanceof AmmoType) )
                return;
            if ( ((AmmoType)link.getType()).hasFlag(AmmoType.F_HOTLOAD) )
                link.hotloaded = hotload;
        }
        if ( this.getType() instanceof AmmoType ){
            if ( ((AmmoType)this.getType()).hasFlag(AmmoType.F_HOTLOAD) )
                this.hotloaded = hotload;
        }
            
    }
    

    public int getLocation() {
        return location;
    }

    public int getSecondLocation() {
        if (bSplit) {
            return secondLocation;
        }
        return -1;
    }
    
    public boolean isRearMounted() {
        return rearMounted;
    }

    public void setLocation(int location) {
        setLocation(location, false);
    }

    public void setSecondLocation(int location) {
        setSecondLocation(location, false);
    }
    
    public void setLocation(int location, boolean rearMounted) {
        this.location = location;
        this.rearMounted = rearMounted;
    }

    public void setSecondLocation(int location, boolean rearMounted) {
        this.secondLocation = location;
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
    
    public boolean isSplitable() {
        return (this.getType() instanceof WeaponType &&
            this.getType().hasFlag(WeaponType.F_SPLITABLE));
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
            } else if (wtype.getAmmoType() == AmmoType.T_RAIL_GUN) {
                return 22;
            } else if (wtype.getAmmoType() == AmmoType.T_GAUSS_HEAVY) {
                return 25;
            } else if (wtype.getAmmoType() == AmmoType.T_AC_ROTARY ||
                       wtype.getAmmoType() == AmmoType.T_AC ||
                       wtype.getAmmoType() == AmmoType.T_LAC) {
                return wtype.getDamage();
            } else if (wtype.getAmmoType() == AmmoType.T_MAGSHOT) {
                return 3;
            }else if ( this.isHotLoaded() ){
                Mounted link = this.getLinked();
                AmmoType atype = ((AmmoType) link.getType());
                int damage = wtype.getRackSize() * atype.getDamagePerShot();
                return damage;
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

    /**
     * Returns false if this ammo should not be loaded.  Checks if the
     *    ammo is already destroyed, is being dumped, has been breached, is
     *   already used up, or is locationless (oneshot ammo).
     */
    public boolean isAmmoUsable() {
        if (this.destroyed || this.m_bDumping ||
            this.useless || this.shotsLeft <= 0 ||
            this.location == Entity.LOC_NONE) {
            return false;
        }
		return true;
    }
    
    /**
     * @return the type of mine this mounted is, or
     * <code>-1</code> if it isn't a mine
     * 
     */
    public int getMineType() {
        return this.mineType;
    }
    
    /**
     * set the type of mine this should be
     * @param mineType
     */
    public void setMineType(int mineType) {
        this.mineType = mineType;
    }
    
    /**
     * set the vibrabomb sensitivity
     * @param vibraSetting the <code>int</code> sensitivity to set
     */
    public void setVibraSetting(int vibraSetting) {
        this.vibraSetting = vibraSetting;
    }
    
    /**
     * get the vibrabomb sensitivity
     * @return the <code>int</code> vibrabomb sensitity this mine is set to.
     */
    public int getVibraSetting() {
        return vibraSetting;
    }

    public String toString() {
        return "megamek.common.Mounted (" + typeName + ")";
    }

    /**
     * Rules state that every time the shield takes a crit its damage absorption
     * for each attack is reduced by 1. 
     * Also for every Arm actuator critted damage absorption is reduced by 1
     * and finally if the should is hit the damage absorption is reduced by 2
     * making it possble to kill a shield before its gone through its full
     * damage capacity.
     * @param entity
     * @param location
     * @return
     */
    public int getDamageAbsorption(Entity entity, int location){
        //Shields can only be used in arms so if you've got a shield in a location
        //other then an arm your SOL --Torren.
        if ( location != Mech.LOC_RARM && location != Mech.LOC_LARM )
            return 0;
        
        int base = baseDamageAbsorptionRate;
        
        for ( int slot = 0; slot < entity.getNumberOfCriticals(location); slot++){
            CriticalSlot cs = entity.getCritical(location,slot);
            
            if ( cs == null )
                continue;
            
            if ( cs.getType() != CriticalSlot.TYPE_EQUIPMENT )
                continue;
            
            Mounted m = entity.getEquipment(cs.getIndex());
            EquipmentType type = m.getType();
            if (type instanceof MiscType &&((MiscType)type).isShield()) {
                if ( cs.isDamaged() )
                    base--;
            }
        }
    
        //Only damaged Actuators should effect the shields absorption rate
        //Not missing ones.
        if ( entity.hasSystem(Mech.ACTUATOR_SHOULDER, location)
                && !entity.hasWorkingSystem(Mech.ACTUATOR_SHOULDER, location) )
            base -= 2;
    
        if ( entity.hasSystem(Mech.ACTUATOR_LOWER_ARM, location) 
                && !entity.hasWorkingSystem(Mech.ACTUATOR_LOWER_ARM, location) )
            base--;
        if ( entity.hasSystem(Mech.ACTUATOR_UPPER_ARM, location)
                && !entity.hasWorkingSystem(Mech.ACTUATOR_UPPER_ARM, location) )
            base--;
        
        return Math.max(0,base);
    }
    
    /**
     * Rules say every time a shield is critted it loses 5 points from its
     * Damage Capacity. basically count down from the top then subtract the amount
     * of damage its already take.
     * The damage capacity is used to determine if the shield is still viable.
     * @param entity
     * @param location
     * @return damage capacity(no less then 0)
     */
    public int getCurrentDamageCapacity(Entity entity, int location){
        //Shields can only be used in arms so if you've got a shield in a location
        //other then an arm your SOL --Torren.
        if ( location != Mech.LOC_RARM && location != Mech.LOC_LARM )
            return 0;
        
        int base = baseDamageCapacity;
    
        for ( int slot = 0; slot < entity.getNumberOfCriticals(location); slot++){
            CriticalSlot cs = entity.getCritical(location,slot);
            
            if ( cs == null )
                continue;
            
            if ( cs.getType() != CriticalSlot.TYPE_EQUIPMENT )
                continue;
            
            Mounted m = entity.getEquipment(cs.getIndex());
            EquipmentType type = m.getType();
            if (type instanceof MiscType && ((MiscType)type).isShield()) {
                if ( cs.isDamaged() )
                    base -= 5;
            }
        }
        return Math.max(0,base-damageTaken);
    }
    
}

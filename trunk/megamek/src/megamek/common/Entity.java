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

package megamek.common;

import java.io.Serializable;
import java.util.*;

/**
 * Entity is a master class for basically anything on the board except
 * terrain.
 */
public abstract class Entity 
    implements Serializable 
{
    public static final int        NONE                = -1;

    public static final int        MOVE_ILLEGAL        = -1;
    public static final int        MOVE_NONE           = 0;
    public static final int        MOVE_WALK           = 1;
    public static final int        MOVE_RUN            = 2;
    public static final int        MOVE_JUMP           = 3;

    public static final int        ARMOR_NA            = -1;
    public static final int        ARMOR_DOOMED        = -2;
    public static final int        ARMOR_DESTROYED     = -3;

    public static final int        LOC_NONE            = -1;
    public static final int        LOC_DESTROYED       = -2;

    protected int               id;

    protected float             weight;
    protected String            model;
    protected String            name;

    protected transient Player  owner;
    protected int               ownerId;

    public Pilot                crew = new Pilot();

    protected boolean           shutDown = false;
    protected boolean           doomed = false;
    protected boolean           destroyed = false;

    private Coords              position = new Coords();

    protected int               facing = 0;
    protected int               sec_facing = 0;

    protected int               walkMP = 0;
    protected int               jumpMP = 0;

    public boolean              ready = false;    

    protected boolean           prone = false;
    protected boolean           charging = false;
    protected boolean           makingDfa = false;
    protected boolean           findingClub = false;  //TODO: put this somehwere else, maybe
    
    public int                  heat = 0;
    public int                  heatBuildup = 0;
    public int                  delta_distance = 0;
    public int                  moved = MOVE_NONE;

    private int[]               armor;
    private int[]               internal;
    private int[]               orig_armor;
    private int[]               orig_internal;
    public int                  damageThisPhase;

    protected Vector               equipmentList = new Vector();
    protected Vector               weaponList = new Vector();
    protected Vector               ammoList = new Vector();
    protected Vector               miscList = new Vector();

    protected int               heatSinks = 10;

    protected CriticalSlot[][]  crits; // [loc][slot]

    
    /**
     * Generates a new, blank, entity.
     */
    public Entity() {
        this.armor = new int[locations()];
        this.internal = new int[locations()];
        this.orig_armor = new int[locations()];
        this.orig_internal = new int[locations()];

        this.crits = new CriticalSlot[locations()][];
        for (int i = 0; i < locations(); i++) {
            this.crits[i] = new CriticalSlot[getNumberOfCriticals(i)];
        }
    }
    
    /**
     * Restores the entity after serialization
     */
    public void restore() {
        // restore all mounted equipments
        for (Enumeration i = equipmentList.elements(); i.hasMoreElements();) {
            Mounted mounted = (Mounted)i.nextElement();
            mounted.restore();
        }   
    }
  
    public int getId() {
        return id;
    }
  
    public void setId(int id) {
        this.id = id;
    }
    
    /**
     * Returns the unit code for this entity.
     */
    public String getModel() {
        return model;
    }
  
    public void setModel(String model) {
        this.model = model;
    }
    
    /**
     * Returns the unit name for this entity.
     */
    public String getName() {
        return name;
    }
  
    protected void setName(String name) {
        this.name = name;
    }
    
    public float getWeight() {
        return weight;
    }
    
    protected void setWeight(float weight) {
        this.weight = weight;
    }
    
    /**
     * Returns the number of locations in the entity
     */
    public abstract int locations();
    
    /**
     * Returns the player that "owns" this entity.
     */
    public Player getOwner() {
        return owner;
    }
  
    public void setOwner(Player player) {
        this.owner = player;
        this.ownerId = player.getId();
    }
  
    public int getOwnerId() {
        return ownerId;
    }
    
    public boolean isEnemyOf(Entity other) {
        return owner.isEnemyOf(other.getOwner());
    }
  
    public Pilot getCrew() {
        return crew;
    }
  
    protected void setCrew(Pilot crew) {
        this.crew = crew;
    }
  
    public boolean isShutDown() {
        return shutDown;
    }
  
    public void setShutDown(boolean shutDown) {
        this.shutDown = shutDown;
    }
    
    public boolean isDoomed() {
        return doomed;
    }
  
    public void setDoomed(boolean doomed) {
        this.doomed = doomed;
    }
    
    public boolean isDestroyed() {
        return destroyed;
    }
  
    public void setDestroyed(boolean destroyed) {
        this.destroyed = destroyed;
    }
    
    /**
     * Is this entity not shut down, not destroyed and has an active
     * crew?
     */
    public boolean isActive() {
        return !shutDown && !destroyed && getCrew().isActive();
    }
  
    /**
     * Returns true if this entity is selectable for action
     */
    public boolean isSelectable() {
        return ready && isActive();
    }

    /**
     * Returns true if this entity is targetable for attacks
     */
    public boolean isTargetable() {
        return !destroyed && !doomed && !crew.isDead();
    }
    
    public boolean isProne() {
        return prone;
    }
  
    public void setProne(boolean prone) {
        this.prone = prone;
    }
    
    /**
     * Is this entity shut down or is the crew unconcious?
     */
    public boolean isImmobile() {
        return shutDown || crew.isUnconcious();
    }
    
    public boolean isCharging() {
        return charging;
    }

    public void setCharging(boolean charging) {
        this.charging = charging;
    }
        
    public boolean isMakingDfa() {
        return makingDfa;
    }

    public void setMakingDfa(boolean makingDfa) {
        this.makingDfa = makingDfa;
    }
        
    public boolean isFindingClub() {
        return findingClub;
    }

    public void setFindingClub(boolean findingClub) {
        this.findingClub = findingClub;
    }
        
    /**
     * Returns the current position of this entity on
     * the board.
     */
    public Coords getPosition() {
        return position;
    }

    /**
     * Sets the current position of this entity on the board.
     * 
     * @param position the new position.
     */
    public void setPosition(Coords position) {
        this.position = position;
    }

    /**
     * Returns the elevation of the hex that this entity is standing on
     */
    public int getElevation(Board b) {
        return b.getHex(getPosition()).getElevation();
    }
    
    /**
    * Returns the display name for this entity.
    * 
    * The display name is in the format [Model] [Name] ([Player Name]).
    */
    public String getDisplayName() {
        if (getOwner() != null) {
            return getModel() + " " + getName() + " (" + getOwner().getName() + ")";
        } else {
            return getModel() + " " + getName() + " (<NULL>)";
        }

    }
    
    /**
     * Returns the primary facing, or -1 if n/a
     */
    public int getFacing() {
        return facing;
    }

    /**
     * Sets the primary facing.
     */
    public void setFacing(int facing) {
        this.facing = facing;
    }

    /**
     * Returns the secondary facing, or -1 if n/a
     */
    public int getSecondaryFacing() {
        return sec_facing;
    }

    /**
     * Sets the secondary facing.
     */
    public void setSecondaryFacing(int sec_facing) {
        this.sec_facing = sec_facing;
    }
    
    /**
     * Can this entity change secondary facing at all?
     */
    public abstract boolean canChangeSecondaryFacing();
  
    /**
     * Can this entity torso/turret twist the given direction?
     */
    public abstract boolean isValidSecondaryFacing(int dir);
    
    /**
     * Returns the closest valid secondary facing to the given direction.
     *
     * @returns the the closest valid secondary facing.
     */
    public abstract int clipSecondaryFacing(int dir);
    
    /**
     * Returns this entity's original walking movement points
     */
    protected int getOriginalWalkMP() {
        return walkMP;
    }

    /**
     * Sets this entity's original walking movement points
     */
    public void setOriginalWalkMP(int walkMP) {
        this.walkMP = walkMP;
    }

    /**
     * Returns this entity's walking/cruising mp, factored
     * for heat.
     */
    public int getWalkMP() {
        return Math.max(walkMP - (int)(heat / 5), 0);
    }

    /**
     * Returns this entity's unmodified running/flank mp.
     */
    protected int getOriginalRunMP() {
        return (int)Math.ceil(getOriginalWalkMP() * 1.5);
    }
    
    /**
     * Returns this entity's running/flank mp modified for heat.
     */
    public int getRunMP() {
        return (int)Math.ceil(getWalkMP() * 1.5);
    }

    /**
     * Returns this entity's original jumping mp.
     */
    protected int getOriginalJumpMP() {
        return jumpMP;
    }

    /**
     * Sets this entity's original jump movement points
     */
    public void setOriginalJumpMP(int jumpMP) {
        this.jumpMP = jumpMP;
    }

    /**
     * Returns this entity's jumping mp, modified for any factors
     * that affect them.
     */
    public int getJumpMP() {
        return jumpMP;
    }

    /**
     * Returns the name of the type of movement used.
     */
    public abstract String getMovementString(int mtype);

    /**
     * Returns the abbreviation of the name of the type of movement used.
     */
    public abstract String getMovementAbbr(int mtype);

    /**
     * Returns the name of the location specified.
     */
    public abstract String getLocationName(HitData hit);

    /**
     * Returns the abbreviated name of the location specified.
     */
    public abstract String getLocationAbbr(HitData hit);

    /**
     * Returns the name of the location specified.
     */
    public abstract String getLocationName(int loc);

    /**
     * Returns the abbreviated name of the location specified.
     */
    public abstract String getLocationAbbr(int loc);

    /**
     * Rolls the to-hit number 
     */
    public abstract HitData rollHitLocation(int table, int side);

    /**
     * Gets the location that excess damage transfers to
     */
    public abstract HitData getTransferLocation(HitData hit);
                                                    
    /**
     * Gets the location that is destroyed recursively
     */
    public abstract int getDependentLocation(int loc);
    
    /**
     * Does this location have rear armor?
     */
    public abstract boolean hasRearArmor(int loc);

    /**
     * Returns the amount of armor in the location specified,
     * or ARMOR_NA, or ARMOR_DESTROYED.  Only works on front locations.
     */
    public int getArmor(int loc) {
        return getArmor(loc, false);
    }

    /**
     * Returns the amount of armor in the location hit.
     */
    public int getArmor(HitData hit) {
        return getArmor(hit.getLocation(), hit.isRear());
    }

    /**
     * Returns the amount of armor in the location specified,
     * or ARMOR_NA, or ARMOR_DESTROYED.
     */
    public int getArmor(int loc, boolean rear) {
        return armor[loc];
    }

    /**
     * Returns the original amount of armor in the location specified,
     * or ARMOR_NA, or ARMOR_DESTROYED.  Only works on front locations.
     */
    public int getOArmor(int loc) {
        return getOArmor(loc, false);
    }

    /**
     * Returns the original amount of armor in the location hit.
     */
    public int getOArmor(HitData hit) {
        return getOArmor(hit.getLocation(), hit.isRear());
    }

    /**
     * Returns the original amount of armor in the location specified,
     * or ARMOR_NA, or ARMOR_DESTROYED.
     */
    public int getOArmor(int loc, boolean rear) {
        return orig_armor[loc];
    }

    /**
     * Sets the amount of armor in the location specified.
     */
    public void setArmor(int val, HitData hit) {
        setArmor(val, hit.getLocation(), hit.isRear());
    }

    /**
     * Sets the amount of armor in the front location specified.
     */
    public void setArmor(int val, int loc) {
        setArmor(val, loc, false);
    }

    /**
     * Sets the amount of armor in the location specified.
     */
    public void setArmor(int val, int loc, boolean rear) {
        armor[loc] = val;
    }

    /**
     * Initializes the armor on the unit. Sets the original and starting point
     * of the armor to the same number.
     */
      public void initializeArmor(int val, int loc) {
        orig_armor[loc] = val;
        setArmor(val, loc);
      }
      
    /**
    * Returns the total amount of armor on the entity.
    */
    public int getTotalArmor() {
        int totalArmor = 0;
        for (int i = 0; i < locations(); i++) {
            if (getArmor(i) > 0) {
                totalArmor += getArmor(i);
            }
            if (hasRearArmor(i) && getArmor(i, true) > 0) {
                totalArmor += getArmor(i, true);
            }
        }
        return totalArmor;
    }
    
    /**
    * Returns the total amount of armor on the entity.
    */
    public int getTotalOArmor() {
        int totalArmor = 0;
        for (int i = 0; i < locations(); i++) {
            if (getOArmor(i) > 0) {
                totalArmor += getOArmor(i);
            }
            if (hasRearArmor(i) && getOArmor(i, true) > 0) {
                totalArmor += getOArmor(i, true);
            }
        }
        return totalArmor;
    }
    
    /**
     * Returns the percent of the armor remaining
     */
      public double getArmorRemainingPercent() {
        return ((double)getTotalArmor() / (double)getTotalOArmor());
      }
      
    /**
     * Returns the amount of internal structure in the location hit.
     */
    public int getInternal(HitData hit) {
        return getInternal(hit.getLocation());
    }

    /**
     * Returns the amount of internal structure in the 
     * location specified, or ARMOR_NA, or ARMOR_DESTROYED.
     */
    public int getInternal(int loc) {
        return internal[loc];
    }
    
    /**
     * Returns the original amount of internal structure in the location hit.
     */
    public int getOInternal(HitData hit) {
        return getOInternal(hit.getLocation());
    }

    /**
     * Returns the original amount of internal structure in the 
     * location specified, or ARMOR_NA, or ARMOR_DESTROYED.
     */
    public int getOInternal(int loc) {
        return orig_internal[loc];
    }
    
    /**
     * Sets the amount of armor in the location specified.
     */
    public void setInternal(int val, HitData hit) {
        setInternal(val, hit.getLocation());
    }
  
    /**
     * Sets the amount of armor in the location specified.
     */
    public void setInternal(int val, int loc) {
        internal[loc] = val;
    }
    
    /**
     * Initializes the internal structure on the unit. Sets the original and starting point
     * of the internal structure to the same number.
     */
      public void initializeInternal(int val, int loc) {
        orig_internal[loc] = val;
        setInternal(val, loc);
      }
      
    /**
     * Set the internal structure to the appropriate value for the mech's
     * weight class
     */
    public abstract void autoSetInternal();
  
    /**
     * Returns the total amount of internal structure on the entity.
     */
    public int getTotalInternal() {
        int totalInternal = 0;
        for (int i = 0; i < locations(); i++) {
            if (getInternal(i) > 0) {
                totalInternal += getInternal(i);
            }
        }
        return totalInternal;
    }
    
    /**
     * Returns the total original amount of internal structure on the entity.
     */
    public int getTotalOInternal() {
        int totalInternal = 0;
        for (int i = 0; i < locations(); i++) {
            if (getOInternal(i) > 0) {
                totalInternal += getOInternal(i);
            }
        }
        return totalInternal;
    }
    
    /**
     * Returns the percent of the armor remaining
     */
      public double getInternalRemainingPercent() {
        return ((double)getTotalInternal() / (double)getTotalOInternal());
      }
      
    /**
    * Is this location destroyed?
    */
    public boolean isLocationDestroyed(int loc) {
        return getInternal(loc) == ARMOR_DESTROYED;
    }
    
    /**
     * Returns a string representing the armor in the location
     */
    public String getArmorString(int loc) {
        return getArmorString(loc, false);
    }
    
    /**
     * Returns a string representing the armor in the location
     */
    public String getArmorString(int loc, boolean rear) {
        if (getArmor(loc, rear) == ARMOR_NA) {
            return "N/A";
        } else if (getArmor(loc, rear) == ARMOR_DESTROYED) {
            return "***";
        } else {
            return Integer.toString(getArmor(loc, rear));
        }
    }
    
    /**
     * Returns a string representing the internal structure in the location
     */
    public String getInternalString(int loc) {
        if (getInternal(loc) == ARMOR_NA) {
            return "N/A";
        } else if (getInternal(loc) == ARMOR_DESTROYED) {
            return "***";
        } else {
            return Integer.toString(getInternal(loc));
        }
    }
    
    /**
     * Returns the modifier to weapons fire due to heat.
     */
    public int getHeatFiringModifier() {
        int mod = 0;
        if (heat >= 8) {
            mod++;
        }
        if (heat >= 13) {
            mod++;
        }
        if (heat >= 17) {
            mod++;
        }
        if (heat >= 24) {
            mod++;
        }
        return mod;
    }
    
    /**
     * Creates a new mount for this equipment and adds it in.
     */
    public void addEquipment(EquipmentType etype, int loc) {
        addEquipment(etype, loc, false);
    }
    
    /**
     * Creates a new mount for this equipment and adds it in.
     */
    public void addEquipment(EquipmentType etype, int loc, boolean rearMounted) {
        Mounted mounted = new Mounted(this, etype);
        addEquipment(mounted, loc, rearMounted);
    }
    
    protected void addEquipment(Mounted mounted, int loc, boolean rearMounted) {
        mounted.setLocation(loc, rearMounted);
        equipmentList.addElement(mounted);
        
        // add it to the proper sub-list
        if (mounted.getType() instanceof WeaponType) {
            weaponList.addElement(mounted);
        }
        if (mounted.getType() instanceof AmmoType) {
            ammoList.addElement(mounted);
        }
        if (mounted.getType() instanceof MiscType) {
            miscList.addElement(mounted);
        }
    }
    
    /**
     * Returns the equipment number of the specified equipment, or
     * -1 if equipment is not present.
     */
    public int getEquipmentNum(Mounted mounted) {
        return equipmentList.indexOf(mounted);
    }
    
    /**
     * Returns an enumeration of all equipment
     */
    public Enumeration getEquipment() {
        return equipmentList.elements();
    }
    
    /**
     * Returns the equipment, specified by number
     */
    public Mounted getEquipment(int index) {
        try {
            return (Mounted)equipmentList.elementAt(index);
        } catch (ArrayIndexOutOfBoundsException ex) {
            return null;
        }
    }
    
    /**
     * Returns the Rules.ARC that the weapon, specified by 
     * number, fires into.
     */
    public abstract int getWeaponArc(int wn);
    
    /**
     * Returns true if this weapon fires into the secondary facing arc.  If
     * false, assume it fires into the primary.
     */
    public abstract boolean isSecondaryArcWeapon(int weaponId);
    

    public Enumeration getWeapons() {
        return weaponList.elements();
    }

    public Vector getWeaponList() {
        return weaponList;
    }

    /**
     * Returns the first ready weapon
     * 
     * @return the index number of the first available weapon, or -1 if none are ready.
     */
    public int getFirstWeapon() {
        for (Enumeration i = weaponList.elements(); i.hasMoreElements();) {
            Mounted mounted = (Mounted)i.nextElement();
            if (mounted.isReady()) {
                return getEquipmentNum(mounted);
            }
        }
        return -1;
    }
  
    /**
     * Returns the next ready weapon, starting at the specified index
     */
    public int getNextWeapon(int start) {
        boolean past = false;
        for (Enumeration i = weaponList.elements(); i.hasMoreElements();) {
            Mounted mounted = (Mounted)i.nextElement();
            if (past && mounted.isReady()) {
                return getEquipmentNum(mounted);
            }
            if (getEquipmentNum(mounted) == start) {
                past = true;
                continue;
            }
        }
        return getFirstWeapon();
    }
  
    /**
     * Loads all weapons with ammo
     */
    public void loadAllWeapons() {
        for (Enumeration i = weaponList.elements(); i.hasMoreElements();) {
            Mounted mounted = (Mounted)i.nextElement();
            WeaponType wtype = (WeaponType)mounted.getType();
            if (wtype.getAmmoType() != AmmoType.TYPE_NA) {
                loadWeapon(mounted);
            }
        }
    }
  
    /**
     * Tries to load the specified weapon with the first available ammo
     */
    public void loadWeapon(Mounted mounted) {
        WeaponType wtype = (WeaponType)mounted.getType();
        for (Enumeration i = getAmmo(); i.hasMoreElements();) {
            Mounted mountedAmmo = (Mounted)i.nextElement();
            AmmoType atype = (AmmoType)mountedAmmo.getType();
            if (mountedAmmo.isDestroyed() || mountedAmmo.getShotsLeft() <= 0) {
                continue;
            }
            if (atype.getAmmoType() == wtype.getAmmoType() && atype.getRackSize() == wtype.getRackSize()) {
                mounted.setLinked(mountedAmmo);
            }
        }
    }
    
    /**
     * Checks whether a weapon has been fired from the specified location this
     * turn
     */
    public boolean weaponFiredFrom(int loc) {
        for (Enumeration i = weaponList.elements(); i.hasMoreElements();) {
            Mounted mounted = (Mounted)i.nextElement();
            if (mounted.getLocation() == loc && mounted.isUsedThisRound()) {
                return true;
            }
        }
        return false;
    }
    
    public Enumeration getAmmo() {
        return ammoList.elements();
    }
    
    public Enumeration getMisc() {
        return miscList.elements();
    }
    
    /**
     * Removes the first misc eq. whose name equals the specified string.  Used
     * for removing broken tree clubs.
     */
    public void removeMisc(String toRemove) {
        for (Enumeration i = miscList.elements(); i.hasMoreElements();) {
            Mounted mounted = (Mounted)i.nextElement();
            if (mounted.getName().equals(toRemove)) {
                miscList.removeElement(mounted);
                equipmentList.removeElement(mounted);
                break;
            }
        }
    }
    
    
    /**
     * Returns the about of heat that the entity can sink each 
     * turn.
     */
    public int getHeatCapacity() {
        return 10 + heatSinks;
    }
  
    /**
     * Returns the about of heat that the entity can sink each 
     * turn, factoring in whether the entity is standing in water.
     */
    public abstract int getHeatCapacityWithWater(Game game);
  
    /**
     * Returns a critical hit slot
     */
    public CriticalSlot getCritical(int loc, int slot) {
        return crits[loc][slot];
    }
    
    /**
     * Sets a critical hit slot
     */
    public void setCritical(int loc, int slot, CriticalSlot cs) {
        crits[loc][slot] = cs;
    }
    
    /**
     * Removes all matching critical slots from the location
     */
    public void removeCriticals(int loc, CriticalSlot cs) {
        for (int i = 0; i < getNumberOfCriticals(loc); i++) {
            if (getCritical(loc, i) != null && getCritical(loc, i).equals(cs)) {
                setCritical(loc, i, null);
            }
        }
    }
    
    /**
     * Returns the number of total critical slots in a location
     */
    public abstract int getNumberOfCriticals(int loc);
    
    /**
     * Returns the number of empty critical slots in a location
     */
    public int getEmptyCriticals(int loc) {
        int empty = 0;
        
        for (int i = 0; i < getNumberOfCriticals(loc); i++) {
            if (getCritical(loc, i) == null) {
                empty++;
            }
        }
        
        return empty;        
    }
    
    /**
     * Returns the number of operational critical slots remaining in a location
     */
    public int getHitableCriticals(int loc) {
        int empty = 0;
        
        for (int i = 0; i < getNumberOfCriticals(loc); i++) {
            if (getCritical(loc, i) != null && getCritical(loc, i).isHitable()) {
                empty++;
            }
        }
        
        return empty;        
    }
    
    /**
     * Slightly different from getHitableCriticals; returns true if this 
     * location can be critically hit this phase, false if criticals should
     * transfer.
     */
    public boolean hasHitableCriticals(int loc) {
        for (int i = 0; i < getNumberOfCriticals(loc); i++) {
            if (getCritical(loc, i) != null 
               && getCritical(loc, i).isDestroyed() == false) {
                return true;
            }
        }
        return false;
    }
    
    /**
     * Returns the number of operational critical slots of the specified type
     * in the location
     */
    public int getGoodCriticals(CriticalSlot cs, int loc) {
        return getGoodCriticals(cs.getType(), cs.getIndex(), loc);
    }
    
    /**
     * Returns the number of operational critical slots of the specified type
     * in the location
     */
    public int getGoodCriticals(int type, int index, int loc) {
        int operational = 0;
        
        for (int i = 0; i < getNumberOfCriticals(loc); i++) {
            CriticalSlot ccs = getCritical(loc, i);
            
            if (ccs != null && ccs.getType() == type && ccs.getIndex() == index
                && !ccs.isDestroyed()) {
                operational++;
            }
            
        }
        
        return operational;
    }
    
    /**
     * The number of critical slots that are destroyed in the component.
     */
    public int getDestroyedCriticals(int type, int index, int loc) {
        int hits = 0;
        
        for (int i = 0; i < getNumberOfCriticals(loc); i++) {
            CriticalSlot ccs = getCritical(loc, i);
            
            if (ccs != null && ccs.getType() == type && ccs.getIndex() == index) {
                if (ccs.isDestroyed()) {
                    hits++;
                }
            }
            
        }
        
        return hits;
    }
    
    /**
     * Number of slots doomed, missing or destroyed
     */
    public int getHitCriticals(int type, int index, int loc) {
        int hits = 0;
        
        for (int i = 0; i < getNumberOfCriticals(loc); i++) {
            CriticalSlot ccs = getCritical(loc, i);
            
            if (ccs != null && ccs.getType() == type && ccs.getIndex() == index) {
                if (ccs.isDamaged()) {
                    hits++;
                }
            }
            
        }
        
        return hits;
    }
    
    /**
     * Returns the number of critical slots present in the section, destroyed
     * or not.
     */
    public int getNumberOfCriticals(int type, int index, int loc) {
        int num = 0;
        
        for (int i = 0; i < getNumberOfCriticals(loc); i++) {
            CriticalSlot ccs = getCritical(loc, i);
            
            if (ccs != null && ccs.getType() == type && ccs.getIndex() == index) {
                num++;
            }
            
        }
        
        return num;
    }
    
    /**
     * Returns true if there is at least 1 functional system of the type
     * specified in the location
     */
    public boolean hasWorkingSystem(int system, int loc) {
        for (int i = 0; i < getNumberOfCriticals(loc); i++) {
            CriticalSlot ccs = getCritical(loc, i);
            if (ccs != null && ccs.getType() == CriticalSlot.TYPE_SYSTEM
            && ccs.getIndex() == system && !ccs.isDestroyed()) {
                return true;
            }
        }
        return false;
    }
    
    /**
     * Adds a CriticalSlot into the first empty slot
     * 
     * TODO: throw exception if full, maybe?
     */
    public void addCritical(int loc, CriticalSlot cs) {
        for (int i = 0; i < getNumberOfCriticals(loc); i++) {
            if (getCritical(loc, i) == null) {
                setCritical(loc, i, cs);
                return;
            }
        }
    }
  
    /**
     * Calculates the battle value of this entity
     */
    public abstract int calculateBattleValue();
    
    
    /**
     * Two entities are equal if their ids are equal
     */
    public boolean equals(Object object) {
        if (this == object) {
            return true;
        } else if (object == null || getClass() != object.getClass()) {
            return false;
        }
        Entity other = (Entity)object;
        return other.getId() == this.id;
    }
}


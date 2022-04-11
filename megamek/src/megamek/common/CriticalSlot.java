/*
 * MegaMek - Copyright (C) 2000-2002 Ben Mazur (bmazur@sev.org)
 *
 * This program is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License as published by the Free
 * Software Foundation; either version 2 of the License, or (at your option)
 * any later version.
 *
 * This program is distributed in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 * or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License
 * for more details.
 */
package megamek.common;

import java.io.Serializable;
import java.util.Objects;

public class CriticalSlot implements Serializable {
    private static final long serialVersionUID = -8744251501251495923L;
    public static final int TYPE_SYSTEM = 0;
    public static final int TYPE_EQUIPMENT = 1;

    /**
     * Determines what the type of this CriticalSlot is, either system or
     * equipment. Systems represent core components of a unit that are located
     * in specific criticals, such as gyros, engines, and cockpits.  Equipment
     * represents everything else, and will have an entry in an EquipmentType
     * subclass.
     */
    private int type;

    /**
     * Index is only used for system type critical slots. It is used as an
     * index into a collection to determine what the system actually is.
     */
    private int index = -1;
    private Mounted mount;
    private Mounted mount2;

    private boolean hit = false; // hit
    private boolean missing = false; // location destroyed
    private boolean destroyed = false;
    private boolean hittable = true; // false = hits rerolled
    private boolean breached = false; // true = breached
    private boolean repairing = false; // true = currently being repaired
    private boolean repairable = true; // true = can be repaired

    private boolean armored = false; // Armored Component Rule

    public CriticalSlot(int type, int index) {
        this(type, index, true, false);
    }

    public CriticalSlot(int type, int index, boolean hittable, boolean armored) {
        this.type = type;
        this.index = index;
        this.hittable = hittable;
        // non-hittable crits cannot be armored.
        if (hittable) {
            this.armored = armored;
        }
    }

    public CriticalSlot(Mounted mount) {
        this(TYPE_EQUIPMENT, -1, mount.getType().isHittable(), mount.isArmored());
        this.mount = mount;
    }

    public int getType() {
        return type;
    }

    public int getIndex() {
        return index;
    }

    public void setIndex(int i) {
        index = i;
    }

    public boolean isHit() {
        return hit;
    }

    /**
     * set that this CriticalSlot was or was not hit with a crit this phase
     * Note: stuff that was hit in a phase can still be used in that phase, if
     * that's not desired, use setDestroyed instead
     *
     * @param hit
     * @see #setDestroyed(boolean)
     */
    public void setHit(boolean hit) {
        this.hit = hit;
    }

    public boolean isDestroyed() {
        return destroyed;
    }

    /**
     * Set this Mounted's destroyed status NOTE: only set this if this Mounted
     * cannot be used in the current phase anymore. If it still can, use setHit
     * instead
     *
     * @param destroyed
     * @see #setHit(boolean)
     */
    public void setDestroyed(boolean destroyed) {
        this.destroyed = destroyed;
    }

    public boolean isMissing() {
        return missing;
    }

    public void setMissing(boolean missing) {
        this.missing = missing;
    }

    public boolean isBreached() {
        return breached;
    }

    public void setBreached(boolean breached) {
        this.breached = breached;
    }

    /**
     * Has this slot been damaged?
     */
    public boolean isDamaged() {
        return hit || destroyed;
    }

    /**
     * Can this slot be hit by a critical hit roll?
     */
    public boolean isHittable() {
        return hittable && !hit && !destroyed && !missing;
    }

    /**
     * Was this critical slot ever hittable?
     */
    public boolean isEverHittable() {
        return hittable;
    }

    /**
     * is the slot being repaired?
     */
    public boolean isRepairing() {
        return repairing;
    }

    public void setRepairing(boolean repairing) {
        this.repairing = repairing;
    }

    /**
     * Two CriticalSlots are equal if their type, index and mount are equal
     */
    @Override
    public boolean equals(Object object) {
        if (this == object) {
            return true;
        } else if ((object == null) || (getClass() != object.getClass())) {
            return false;
        }
        CriticalSlot other = (CriticalSlot) object;
        return ((other.getType() == type) && (other.getIndex() == index) && (((other
                .getMount() != null) && (mount != null) && other.getMount()
                .equals(mount)) || ((mount == null) && (other.getMount() == null))));
    }

    @Override
    public int hashCode() {
        return Objects.hash(type, index, mount);
    }

    /**
     * @param mount the mount to set
     */
    public void setMount(Mounted mount) {
        this.mount = mount;
    }

    /**
     * @return the mount
     */
    public Mounted getMount() {
        return mount;
    }

    /**
     * @param mount the mount to set
     */
    public void setMount2(Mounted mount) {
        mount2 = mount;
    }

    /**
     * @return the mount
     */
    public Mounted getMount2() {
        return mount2;
    }

    public void setArmored(boolean armored) {
        this.armored = armored;
    }

    public boolean isArmored() {
        return armored;
    }

    public void setRepairable(boolean repair) {
        repairable = repair;
    }

    public boolean isRepairable() {
        return repairable;
    }
}

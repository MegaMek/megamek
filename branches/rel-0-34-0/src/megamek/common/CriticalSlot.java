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

public class CriticalSlot implements Serializable {
    /**
     *
     */
    private static final long serialVersionUID = -8744251501251495923L;
    public final static int TYPE_SYSTEM = 0;
    public final static int TYPE_EQUIPMENT = 1;

    private int type;
    private int index;
    private Mounted mount;

    private boolean hit; // hit
    private boolean missing; // location destroyed
    private boolean destroyed;
    private boolean hittable; // false = hits rerolled
    private boolean breached; // true = breached
    private boolean repairing = false; // true = currently being repaired

    private boolean armored = false; // Armored Component Rule

    public CriticalSlot(int type, int index) {
        this(type, index, true, null);
    }

    public CriticalSlot(int type, int index, boolean hittable, Mounted mount) {
        this(type, index, hittable, mount != null?mount.isArmored():false, mount);
    }

    public CriticalSlot(int type, int index, boolean hittable, boolean armored,
            Mounted mount) {
        this.type = type;
        this.index = index;
        this.hittable = hittable;
        // non-hittable crits cannot be armored.
        if (hittable) {
            this.armored = armored;
        }
        this.mount = mount;
    }

    public CriticalSlot(int type, Mounted mount) {
        this(type,mount,true);
    }

    public CriticalSlot(int type, Mounted mount, boolean hittable) {
        this.type = type;
        index = -1;
        this.hittable = hittable;
        setMount(mount);
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

    public void setHit(boolean hit) {
        this.hit = hit;
    }

    public boolean isDestroyed() {
        return destroyed;
    }

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
        return hit || missing || destroyed;
    }

    /**
     * Can this slot be hit by a critical hit roll?
     */
    public boolean isHittable() {
        return hittable && !hit && !destroyed;
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
     * Two CriticalSlots are equal if their type and index are equal
     */
    @Override
    public boolean equals(Object object) {
        if (this == object) {
            return true;
        } else if ((object == null) || (getClass() != object.getClass())) {
            return false;
        }
        CriticalSlot other = (CriticalSlot) object;
        return (other.getType() == type) && (other.getIndex() == index);
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

    public void setArmored(boolean armored) {
        this.armored = armored;
    }

    public boolean isArmored() {
        return armored;
    }
}

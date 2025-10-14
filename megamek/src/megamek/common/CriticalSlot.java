/*
 * Copyright (C) 2000-2002 Ben Mazur (bmazur@sev.org)
 * Copyright (C) 2002-2025 The MegaMek Team. All Rights Reserved.
 *
 * This file is part of MegaMek.
 *
 * MegaMek is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License (GPL),
 * version 3 or (at your option) any later version,
 * as published by the Free Software Foundation.
 *
 * MegaMek is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty
 * of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU General Public License for more details.
 *
 * A copy of the GPL should have been included with this project;
 * if not, see <https://www.gnu.org/licenses/>.
 *
 * NOTICE: The MegaMek organization is a non-profit group of volunteers
 * creating free software for the BattleTech community.
 *
 * MechWarrior, BattleMech, `Mech and AeroTech are registered trademarks
 * of The Topps Company, Inc. All Rights Reserved.
 *
 * Catalyst Game Labs and the Catalyst Game Labs logo are trademarks of
 * InMediaRes Productions, LLC.
 *
 * MechWarrior Copyright Microsoft Corporation. MegaMek was created under
 * Microsoft's "Game Content Usage Rules"
 * <https://www.xbox.com/en-US/developers/rules> and it is not endorsed by or
 * affiliated with Microsoft.
 */

package megamek.common;

import java.io.Serial;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import megamek.common.equipment.Mounted;

public class CriticalSlot implements Serializable {
    @Serial
    private static final long serialVersionUID = -8744251501251495923L;
    public static final int TYPE_SYSTEM = 0;
    public static final int TYPE_EQUIPMENT = 1;

    /**
     * Determines what the type of this CriticalSlot is, either system or equipment. Systems represent core components
     * of a unit that are located in specific criticalSlots, such as gyros, engines, and cockpits. Equipment represents
     * everything else, and will have an entry in an EquipmentType subclass.
     */
    private final int type;

    /**
     * Index is only used for system type critical slots. It is used as an index into a collection to determine what the
     * system actually is.
     */
    private int index;
    private Mounted<?> mount;
    private Mounted<?> mount2;

    private boolean hit = false; // hit
    private boolean missing = false; // location destroyed
    private boolean destroyed = false;
    private final boolean hittable; // false = hits rerolled
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

    public CriticalSlot(Mounted<?> mount) {
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
     * set that this CriticalSlot was or was not hit with a crit this phase Note: stuff that was hit in a phase can
     * still be used in that phase, if that's not desired, use setDestroyed instead
     *
     * @see #setDestroyed(boolean)
     */
    public void setHit(boolean hit) {
        this.hit = hit;
    }

    public boolean isDestroyed() {
        return destroyed;
    }

    /**
     * Set this Mounted's destroyed status NOTE: only set this if this Mounted cannot be used in the current phase
     * anymore. If it still can, use setHit instead
     *
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
        return ((other.getType() == type) && (other.getIndex() == index) && (((other.getMount() != null)
              && other.getMount().equals(mount))
              || ((mount == null) && (other.getMount() == null))));
    }

    @Override
    public int hashCode() {
        return Objects.hash(type, index, mount);
    }

    /**
     * @param mount the mount to set
     */
    public void setMount(Mounted<?> mount) {
        this.mount = mount;
    }

    /**
     * @return the mount
     */
    public Mounted<?> getMount() {
        return mount;
    }

    /**
     * @param mount the mount to set
     */
    public void setMount2(Mounted<?> mount) {
        mount2 = mount;
    }

    /**
     * @return the mount
     */
    public Mounted<?> getMount2() {
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

    @Override
    public String toString() {
        String typeString = type == 0 ? "System Slot" : "Equipment Slot";
        List<String> state = new ArrayList<>();
        if (type == 0) {state.add("System No: " + index);}
        if (mount != null) {
            state.add("[" + mount.equipmentIndex() + "] " + mount.getType().getInternalName()
                  + (mount.isWeaponGroup() ? " -Group-" : ""));
        }
        if (mount2 != null) {
            state.add("Mount 2: [" + mount2.equipmentIndex() + "] " + mount2.getType().getInternalName()
                  + (mount2.isWeaponGroup() ? " -Group-" : ""));
        }
        if (destroyed) {state.add("Destroyed");}
        if (hit) {state.add("Hit");}
        if (!hittable) {state.add("Not hittable");}
        if (breached) {state.add("Breached");}
        if (missing) {state.add("Missing");}
        if (armored) {state.add("Armored");}
        if (repairing) {state.add("Repairing");}
        if (!repairable) {state.add("Not repairable");}
        return typeString + " { " + String.join(", ", state) + " }";
    }

    /**
     * @return True if this crit slot is eligible for being an armored component, TO:AUE p.95
     */
    public boolean isArmorable() {
        return ((getType() == CriticalSlot.TYPE_SYSTEM) || ((getMount() != null) && getMount().getType()
              .isEligibleForBeingArmored()));
    }
}

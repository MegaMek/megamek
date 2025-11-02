/*
 * Copyright (C) 2025 The MegaMek Team. All Rights Reserved.
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

package megamek.common.equipment;

import java.util.ArrayList;
import java.util.List;
import java.util.Vector;

import megamek.common.game.Game;
import megamek.common.units.Entity;

/**
 * Unprotected cargo transporter. Miscellaneous transporters that don't properly protect their cargo like a bay or
 * infantry compartment, but support cargo unlike clamp mounts or BA Handles.
 */
public abstract class ExternalCargo implements Transporter {

    protected transient Game game;

    /** The total amount of space available for objects. */
    protected double totalSpace;

    /** The current amount of space not occupied by objects. */
    protected double currentSpace;

    protected Vector<ICarryable> carriedObjects = new Vector<>();

    protected ExternalCargo(double tonnage) {
        totalSpace = tonnage;
        currentSpace = totalSpace;
    }

    /**
     * Determines if this object can accept the given unit. The unit may not be of the appropriate type or there may be
     * no room for the unit.
     *
     * @param unit - the <code>Entity</code> to be loaded.
     *
     * @return <code>true</code> if the unit can be loaded, <code>false</code>
     *       otherwise.
     */
    @Override
    public boolean canLoad(Entity unit) {
        return false; //TODO: Support loading cargo in game
    }

    /**
     * Load the given unit.
     *
     * @param unit the <code>Entity</code> to be loaded.
     *
     * @throws IllegalArgumentException If the unit can't be loaded
     */
    @Override
    public void load(Entity unit) throws IllegalArgumentException {
        throw new IllegalArgumentException("Non-Functional Feature");
    }

    /**
     * Get a <code>Vector</code> of the units currently loaded into this payload.
     *
     * @return A <code>List</code> of loaded <code>Entity</code> units. This list will never be <code>null</code>, but
     *       it may be empty. The returned <code>List</code> is independent from the underlying data structure;
     *       modifying one does not affect the other.
     */
    @Override
    public List<Entity> getLoadedUnits() {
        if (carriedObjects.isEmpty()) {
            return List.of();
        }
        List<Entity> retList = new ArrayList<Entity>();
        for (ICarryable carriedObject : carriedObjects) {
            if (carriedObject instanceof Entity) {
                retList.add((Entity) carriedObject);
            }
        }

        return retList;
    }

    /**
     * Unload the given unit.
     *
     * @param unit - the <code>Entity</code> to be unloaded.
     *
     * @return <code>true</code> if the unit was contained in this space,
     *       <code>false</code> otherwise.
     */
    @Override
    public boolean unload(Entity unit) {
        boolean wasCarried = carriedObjects.removeElement(unit);
        if (wasCarried) {
            currentSpace += unit.getTonnage();
        }
        return wasCarried;
    }

    /**
     * @return the number of unused spaces in this transporter.
     */
    @Override
    public double getUnused() {
        return currentSpace;
    }

    public double getCarriedTonnage() {
        return currentSpace - totalSpace;
    }

    /**
     * Return a string that identifies the unused capacity of this transporter.
     *
     * @return A <code>String</code> meant for a human.
     */
    @Override
    public String getUnusedString() {
        return "";
    }

    /**
     * Determine if transported units prevent a weapon in the given location from firing.
     *
     * @param loc    the location attempting to fire.
     * @param isRear true if the weapon is rear-facing
     *
     * @return True if a transported unit is in the way, false if the weapon can fire.
     */
    @Override
    public boolean isWeaponBlockedAt(int loc, boolean isRear) {
        return false;
    }

    /**
     * If a unit is being transported on the outside of the transporter, it can suffer damage when the transporter is
     * hit by an attack. Currently, no more than one unit can be at any single location; that same unit can be "spread"
     * over multiple locations.
     *
     * @param loc    - the <code>int</code> location hit by attack.
     * @param isRear - a <code>boolean</code> value stating if the given location is rear facing; if <code>false</code>,
     *               the location is front facing.
     *
     * @return The <code>Entity</code> being transported on the outside at that location. This value will be
     *       <code>null</code> if no unit is transported on the outside at that location.
     */
    @Override
    public Entity getExteriorUnitAt(int loc, boolean isRear) {
        return null;
    }

    /**
     * @return list of all units carried externally by this transporter
     */
    @Override
    public List<Entity> getExternalUnits() {
        return getLoadedUnits();
    }

    /**
     * @param carrier
     *
     * @return the MP reduction due to cargo carried by this transporter
     */
    @Override
    public int getCargoMpReduction(Entity carrier) {
        return 0;
    }

    @Override
    public void setGame(Game game) {
        this.game = game;
    }

    /**
     * clear out all troops listed in the transporter. Used by MHQ to reset units after game
     */
    @Override
    public void resetTransporter() {

    }
}

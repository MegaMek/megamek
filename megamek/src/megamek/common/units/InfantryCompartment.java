/*
 * Copyright (c) 2003-2004 Ben Mazur (bmazur@sev.org)
 * Copyright (C) 2003-2025 The MegaMek Team. All Rights Reserved.
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

package megamek.common.units;

import static java.util.stream.Collectors.toList;

import java.io.Serial;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Vector;

import megamek.common.equipment.Transporter;
import megamek.common.game.Game;

/**
 * Represents a volume of space set aside for carrying troops and their equipment under battle conditions. Typically, a
 * component of an APC.
 */
public final class InfantryCompartment implements Transporter, InfantryTransporter {
    @Serial
    private static final long serialVersionUID = 7837499891552862932L;

    /**
     * The troops being carried.
     */
    Map<Integer, Double> troops = new HashMap<>();

    /**
     * The total amount of space available for troops.
     */
    double totalSpace;

    /**
     * The current amount of space available for troops.
     */
    double currentSpace;

    transient Game game;

    /**
     * The default constructor is only for serialization.
     */
    private InfantryCompartment() {
        totalSpace = 0;
        currentSpace = 0;
    }

    /**
     * Create a space for the given tonnage of troops. For this class, only the weight of the troops (and their
     * equipment) are considered; if you'd like to think that they are stacked like lumber, be my guest.
     *
     * @param space The weight of troops (in tons) this space can carry.
     */
    public InfantryCompartment(double space) {
        totalSpace = space;
        currentSpace = space;
    }

    /**
     * Determines if this object can accept the given unit. The unit may not be of the appropriate type or there may be
     * no room for the unit.
     *
     * @param unit the <code>Entity</code> to be loaded.
     *
     * @return <code>true</code> if the unit can be loaded, <code>false</code> otherwise.
     */
    @Override
    public boolean canLoad(Entity unit) {
        // Assume that we can carry the unit.
        boolean result = true;

        // Only Infantry and BattleArmor can be carried in TroopSpace.
        if (!(unit instanceof Infantry)) {
            result = false;
        }

        // We must have enough space for the new troops.
        // POSSIBLE BUG: we may have to take the Math.ceil() of the weight.
        else if (currentSpace < unit.getWeight()) {
            result = false;
        }

        // Return our result.
        return result;
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
        // If we can't load the unit, throw an exception.
        if (!canLoad(unit)) {
            throw new IllegalArgumentException("Can not load " + unit.getShortName() + " into this troop space.");
        }

        // Decrement the available space.
        // POSSIBLE BUG: we may have to take the Math.ceil() of the weight.
        currentSpace -= unit.getWeight();

        // Add the unit to our list of troops.
        troops.put(unit.getId(), unit.getWeight());

    }

    /**
     * Get a <code>List</code> of the units currently loaded into this payload.
     *
     * @return A <code>List</code> of loaded <code>Entity</code> units. This list will never be
     *       <code>null</code>, but it may be empty. The returned <code>List</code> is independent of
     *       the underlying data structure; modifying one does not affect the other.
     */
    @Override
    public Vector<Entity> getLoadedUnits() {
        Vector<Entity> loaded = new Vector<>();
        for (Map.Entry<Integer, Double> entry : troops.entrySet()) {
            int key = entry.getKey();
            Entity entity = game.getEntity(key);

            if (entity != null) {
                loaded.add(entity);
            }
        }

        return loaded;
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
        // If this unit isn't loaded, nothing to do
        if (!troops.containsKey(unit.getId())) {
            return false;
        }

        // Remove the unit if we are carrying it.
        boolean retVal = false;
        double unloadWeight = 0;

        if (unit != null) {
            unloadWeight = troops.get(unit.getId());
        }

        // If we removed it, restore our space.
        if (troops.remove(unit.getId()) != null) {
            retVal = true;
            currentSpace += unloadWeight;
        }

        // Return our status
        return retVal;
    }

    /**
     * Return a string that identifies the unused capacity of this transporter.
     *
     * @return A <code>String</code> meant for a human.
     */
    @Override
    public String getUnusedString() {
        return "Infantry Compartment - " + currentSpace + " tons";
    }

    @Override
    public double getUnused() {
        return currentSpace;
    }

    /**
     * Determine if transported units prevent a weapon in the given location from firing.
     *
     * @param loc    - the <code>int</code> location attempting to fire.
     * @param isRear - a <code>boolean</code> value stating if the given location is rear facing; if <code>false</code>,
     *               the location is front facing.
     *
     * @return <code>true</code> if a transported unit is in the way,
     *       <code>false</code> if the weapon can fire.
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
     * @return The amount of unused space in the bay expressed in slots. For most bays this is the same as the unused
     *       space, but bays for units that can take up a variable amount of space (such as infantry bays) this
     *       calculates the number of the default unit size that can fit into the remaining space.
     */
    @Override
    public double getUnusedSlots() {
        return currentSpace;
    }

    /** @return A (possibly empty) list of units from this bay that can be assault-dropped. */
    @Override
    public List<Entity> getDroppableUnits() {
        return troops.keySet()
              .stream()
              .map(game::getEntity)
              .filter(Objects::nonNull)
              .filter(Entity::canAssaultDrop)
              .collect(toList());
    }

    // Use the calculated original weight for infantry in cargo bays
    @Override
    public double spaceForUnit(Entity unit) {
        return Math.round(unit.getWeight() / unit.getInternalRemainingPercent());
    }

    @Override
    public List<Entity> getExternalUnits() {
        return new ArrayList<>(1);
    }

    @Override
    public int getCargoMpReduction(Entity carrier) {
        return 0;
    }

    @Override
    public String toString() {
        return "troopspace:" + totalSpace;
    }

    @Override
    public String getTransporterType() {
        return "Infantry Compartment";
    }

    @Override
    public void setGame(Game game) {
        this.game = game;
    }

    @Override
    public void resetTransporter() {
        troops = new HashMap<>();
        currentSpace = totalSpace;
    }
}

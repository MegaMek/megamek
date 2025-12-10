/*
  Copyright (C) 2000-2002 Ben Mazur (bmazur@sev.org)
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

package megamek.common.equipment;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import megamek.common.annotations.Nullable;
import megamek.common.game.Game;
import megamek.common.units.Entity;

/**
 * Classes that implement this interface have the ability to load, carry, and unload units in the game. It is
 * anticipated that classes will exist for passenger compartments, battle armor steps, Mek bays, Aerospace hangers, and
 * vehicle garages. Other possible classes include cargo bays and Dropship docks.
 */
public interface Transporter extends Serializable {

    /**
     * Determines if this object can accept the given unit. The unit may not be of the appropriate type or there may be
     * no room for the unit.
     *
     * @param unit - the <code>Entity</code> to be loaded.
     *
     * @return <code>true</code> if the unit can be loaded, <code>false</code>
     *       otherwise.
     */
    boolean canLoad(Entity unit);

    /**
     * Determines if this transporter can tow the given unit. By default, no.
     */
    default boolean canTow(Entity unit) {return false;}

    /**
     * Load the given unit.
     *
     * @param unit the <code>Entity</code> to be loaded.
     *
     * @throws IllegalArgumentException If the unit can't be loaded
     */
    void load(Entity unit) throws IllegalArgumentException;

    /**
     * Get a <code>Vector</code> of the units currently loaded into this payload.
     *
     * @return A <code>List</code> of loaded <code>Entity</code> units. This list will never be <code>null</code>, but
     *       it may be empty. The returned <code>List</code> is independent of the underlying data structure; modifying
     *       one does not affect the other.
     */
    List<Entity> getLoadedUnits();

    /**
     * Unload the given unit.
     *
     * @param unit - the <code>Entity</code> to be unloaded.
     *
     * @return <code>true</code> if the unit was contained in this space,
     *       <code>false</code> otherwise.
     */
    boolean unload(Entity unit);

    /**
     * @return the number of unused spaces in this transporter.
     */
    double getUnused();

    /**
     * Return a string that identifies the unused capacity of this transporter.
     *
     * @return A <code>String</code> meant for a human.
     */
    String getUnusedString();

    /**
     * Determine if transported units prevent a weapon in the given location from firing.
     *
     * @param loc    the location attempting to fire.
     * @param isRear true if the weapon is rear-facing
     *
     * @return True if a transported unit is in the way, false if the weapon can fire.
     */
    boolean isWeaponBlockedAt(int loc, boolean isRear);

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
    @Nullable
    Entity getExteriorUnitAt(int loc, boolean isRear);

    /**
     * @return list of all units carried externally by this transporter
     */
    List<Entity> getExternalUnits();

    /**
     * @return the MP reduction due to cargo carried by this transporter
     */
    int getCargoMpReduction(Entity carrier);

    void setGame(Game game);

    /**
     * clear out all troops listed in the transporter. Used by MHQ to reset units after game
     */
    void resetTransporter();

    /**
     * Returns the number of Docking Collars (hardpoints) this transporter counts as toward the maximum that a JumpShip
     * (or WS, SS) may carry. TO:AUE p.146
     *
     * @return The number of docking hardpoints this transporter counts as toward the limit.
     */
    default int hardpointCost() {
        return 0;
    }

    /**
     * Returns the number of units loaded on to this transporter this turn. Used to determine loading eligibility
     *
     * @return the number of units loaded
     */
    default int getNumberLoadedThisTurn() {
        return 0;
    }

    /**
     * Returns the number of units loaded on to this transporter this turn. Used to determine loading eligibility
     *
     * @return the number of units loaded
     */
    default int getNumberUnloadedThisTurn() {
        return 0;
    }


    default String getTransporterType() {
        return "Unknown";
    }

    default String getNameForRecordSheets() {
        return getTransporterType();
    }

    /**
     * Sets the specified entity to the transporter. Not implemented by default, only implemented for Transporters that
     * need it (like {@link ExternalCargo})
     *
     * @param entity the {@code Entity} to be set for the transporter
     */
    default void setEntity(Entity entity) {}

    /**
     * Returns true if the transporter can pick up ground objects
     */
    default boolean canPickupGroundObject() {
        return false;
    }

    /**
     * Returns true if the transporter damages its cargo if the transport is hit, otherwise false.
     */
    default boolean alwaysDamageCargoIfTransportHit() {
        return false;
    }

    /**
     * Retrieves a list of all {@link ICarryable} objects currently carried by this transporter. If no objects are being
     * carried, the returned list will be empty.
     * <p>
     * The returned list is a separate instance and modifying it will not affect the underlying data structure of
     * carried objects.
     *
     * @return a list of {@link ICarryable} objects currently carried. Never null, but may be empty.
     */
    default List<ICarryable> getCarryables() {
        return new ArrayList<>(getLoadedUnits());
    }
}

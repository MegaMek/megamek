/*
 * Copyright (c) 2002-2018 - Ben Mazur (bmazur@sev.org).
 * Copyright (C) 2018-2025 The MegaMek Team. All Rights Reserved.
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

import java.io.Serial;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Vector;

import megamek.common.game.Game;
import megamek.common.loaders.MekFileParser;
import megamek.common.units.Entity;

/**
 * Represents a trailer hitch that allows a wheeled or tracked vehicle to tow trailers.
 *
 * @see MekFileParser#postLoadInit
 */
public class TankTrailerHitch implements Transporter {
    @Serial
    private static final long serialVersionUID = 1193349063084937973L;

    /**
     * Is this transporter associated with a front or rear-mounted hitch equipment?
     */
    private final boolean rearMounted;

    public boolean getRearMounted() {
        return rearMounted;
    }

    /**
     * The entity being towed by this hitch.
     */
    protected int towed = Entity.NONE;
    private transient Game game;

    /**
     * The <code>String</code> reported when the hitch is in use.
     */
    private static final String NO_VACANCY_STRING = "A trailer is attached";

    /**
     * The <code>String</code> reported when the hitch is available.
     */
    private static final String HAVE_VACANCY_STRING = "One trailer";

    /**
     * Get the <code>String</code> to report the presence (or lack thereof) of a towed trailer.
     * <p>
     * Subclasses are encouraged to override this method.
     *
     * @param isLoaded - a <code>boolean</code> that indicates a trailer is currently loaded (if the value is
     *                 <code>true</code>) or not (if the value is <code>false</code>).
     *
     * @return a <code>String</code> describing the occupancy state of this transporter.
     */
    protected String getVacancyString(boolean isLoaded) {
        if (isLoaded) {
            return TankTrailerHitch.NO_VACANCY_STRING;
        }
        return TankTrailerHitch.HAVE_VACANCY_STRING;
    }

    /**
     * Create a new hitch, specified as a (front) or rear mount.
     */
    public TankTrailerHitch(boolean rear) {
        rearMounted = rear;
    }

    /**
     * Determines if this object can accept the given unit. The unit may not be of the appropriate type or there may be
     * no room for the unit.
     * <p>
     *
     * @param unit - the <code>Entity</code> to be loaded.
     *
     * @return <code>true</code> if the unit can be loaded, <code>false</code>
     *       otherwise.
     */
    @Override
    public boolean canLoad(Entity unit) {
        return false;
    }

    @Override
    public boolean canTow(Entity unit) {
        // Only trailers can be towed.
        if (!unit.isTrailer()) {
            return false;
        }

        // We must have enough space for the trailer.
        return towed == Entity.NONE;
    }

    /**
     * Load the given unit.
     *
     * @param unit the <code>Entity</code> to be loaded.
     *
     * @throws IllegalArgumentException If the unit can't be loaded
     */
    @Override
    public final void load(Entity unit) throws IllegalArgumentException {
        // If we can't load the unit, throw an exception.
        if (!canTow(unit)) {
            throw new IllegalArgumentException("Can not load " + unit.getShortName() + " onto this hitch.");
        }

        // Assign the unit as our carried troopers.
        towed = unit.getId();
    }

    /**
     * Get a <code>List</code> of the units currently loaded into this payload.
     *
     * @return A <code>Vector</code> of loaded <code>Entity</code> units. This list will never be <code>null</code>, but
     *       it may be empty. The returned <code>List</code> is independent of the under-lying data structure; modifying
     *       one does not affect the other.
     */
    @Override
    public final Vector<Entity> getLoadedUnits() {
        // Return a list of our carried troopers.
        Vector<Entity> units = new Vector<>(1);
        if (towed != Entity.NONE) {
            Entity entity = game.getEntity(towed);

            if (entity != null) {
                units.addElement(entity);
            }
        }
        return units;
    }

    /**
     * Unload the given unit.
     *
     * @param unit - the <code>Entity</code> to be unloaded.
     *
     * @return <code>true</code> if the unit was contained is loaded in this space,
     *       <code>false</code> otherwise.
     */
    @Override
    public final boolean unload(Entity unit) {
        // Are we carrying the unit?
        Entity trailer = game.getEntity(towed);
        if ((trailer == null) || !trailer.equals(unit)) {
            // Nope.
            return false;
        }

        // Remove the troopers.
        towed = Entity.NONE;
        return true;
    }

    /**
     * Return a string that identifies the unused capacity of this transporter.
     * <p>
     * Subclasses should override the <code>getVacancyString</code> method.
     *
     * @return A <code>String</code> meant for a human.
     *
     * @see TankTrailerHitch#getUnusedString()
     */
    @Override
    public final String getUnusedString() {
        return getVacancyString(towed != Entity.NONE);
    }

    @Override
    public double getUnused() {
        if (towed == Entity.NONE) {
            return 1;
        } else {
            return 0;
        }
    }

    @Override
    public void resetTransporter() {
        towed = Entity.NONE;
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
        // Assume that the weapon is not blocked. See Entity.isWeaponBlockedByTowing() instead.
        return false;
    }

    /**
     * If a unit is being transported on the outside of the transporter, it can suffer damage when the transporter is
     * hit by an attack. Currently, no more than one unit can be at any single location; that same unit can be "spread"
     * over multiple locations.
     * <p>
     *
     * @param loc    - the <code>int</code> location hit by attack.
     * @param isRear - a <code>boolean</code> value stating if the given location is rear facing; if <code>false</code>,
     *               the location is front facing.
     *
     * @return The <code>Entity</code> being transported on the outside at that location. This value will be
     *       <code>null</code> if no unit is transported on the outside at that location.
     */
    @Override
    public final Entity getExteriorUnitAt(int loc, boolean isRear) {
        return null;
    }

    @Override
    public final List<Entity> getExternalUnits() {
        return Collections.emptyList();
    }

    @Override
    public int getCargoMpReduction(Entity carrier) {
        return 0;
    }

    @Override
    public String toString() {
        return "Trailer Hitch:" + getUnused();
    }

    @Override
    public void setGame(Game game) {
        this.game = game;
    }
}

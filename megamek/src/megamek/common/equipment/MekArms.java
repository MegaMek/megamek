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

import java.util.List;

import megamek.common.units.Entity;
import megamek.common.units.MekWithArms;
import megamek.logging.MMLogger;

public class MekArms extends ExternalCargo {
    private final static MMLogger logger = MMLogger.create(MekArms.class);

    public MekArms(MekWithArms mek) {
        // FIXME #7640: Split MekArms into individual transporters once there is support for picking up cargo into
        //  multiple locations.
        this(mek.unmodifiedMaxGroundObjectTonnage(), mek.getDefaultPickupLocations());
        this.entity = mek;
        this.entityId = mek.getId();
    }

    public MekArms(double tonnage, List<Integer> validPickupLocations) {
        super(tonnage, validPickupLocations);
    }

    /**
     * If this specific transporter is capable of loading regardless of what the object is.
     *
     * @return <code>true</code> if the transporter is capable of loading, <code>false</code> otherwise.
     */
    @Override
    protected boolean canLoad() {
        for (int location : getLocations()) {
            if (getCarryables(location).isEmpty() && super.canLoad()) {
                return true;
            }
        }
        return false;
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
        return unit.getTonnage() <= getUnused();
    }

    /**
     * Load the given carryable into the given location
     *
     * @param carryable the {@link ICarryable} to be loaded
     * @param location  the location it should be loaded into
     *
     * @throws IllegalArgumentException If the unit can't be loaded
     */
    @Override
    public void loadCarryable(ICarryable carryable, int location) throws IllegalArgumentException {
        super.loadCarryable(carryable, location);
        if (entity != null) {
            // FIXME #7640: Once we have transporters for each location that could carry an object, we can update the entity
            //  logic to just be disabling additional loading
            entity.pickupCarryableObject(carryable, location);
        }
    }

    /**
     * @return the number of unused spaces in this transporter.
     */
    @Override
    public double getUnused() {
        if (entity instanceof MekWithArms mek) {
            return (totalSpace * mek.getTSMPickupModifier()) - getCarriedTonnage();
        }

        return super.getUnused();
    }

    @Override
    public String getTransporterType() {
        return "Arms";
    }
}

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
import megamek.common.units.Mek;

/**
 * Transporter for Lift Hoists as described TW p. 136
 */
public class LiftHoist extends ExternalCargo {
    private int mountedId;

    public LiftHoist(Mounted<?> mounted, double tonnage) {
        super(tonnage, List.of(Entity.LOC_NONE));
        entity = mounted.getEntity();
        if (entity != null) {
            entityId = mounted.getEntity().getId();
        }
        mountedId = mounted.getEquipmentNum();
    }

    @Override
    public double getUnused() {
        if (isOperable()) {
            if (entity != null && entity instanceof Mek mek) {
                return (totalSpace * mek.getTSMPickupModifier()) - getCarriedTonnage();
            }
            return super.getUnused();
        }
        return 0;
    }

    /**
     * If this specific transporter is capable of loading regardless of what the object is. LiftHoists are only able to
     * load if they are operable and not carrying anything else.
     *
     * @return <code>true</code> if the transporter is capable of loading, <code>false</code> otherwise.
     */
    @Override
    protected boolean canLoad() {
        return super.canLoad() && isOperable() && getCarryables().isEmpty();
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
        // Lift Hoists can't load units in combat; It can load HHWs though. Can only carry one item at a time in a
        // lift hoist!
        return unit instanceof HandheldWeapon && super.canLoadCarryable(unit);
    }

    /**
     * Determines if this object can accept the given {@link ICarryable}. The carryable may not be of the appropriate
     * type or there may be no room for the unit.
     *
     * @param carryable the {@link ICarryable} to be loaded
     *
     * @return <code>true</code> if the carryable can be loaded, <code>false</code> otherwise.
     */
    @Override
    public boolean canLoadCarryable(ICarryable carryable) {
        return super.canLoadCarryable(carryable);
    }

    @Override
    public String getTransporterType() {
        return "Lift Hoist";
    }

    /**
     * Returns true if the transporter can pick up ground objects
     */
    @Override
    public boolean canPickupGroundObject() {
        return canLoad();
    }

    private boolean isOperable() {
        Mounted<?> mounted = getMounted();
        return mounted != null && !mounted.getEntity().isLocationBad(mounted.getLocation()) && mounted.isOperable();
    }

    private Mounted<?> getMounted() {
        Entity entity = game.getEntity(entityId);
        if (entity == null) {
            return null;
        }
        return entity.getEquipment(mountedId);
    }

    /**
     * Returns true if the transporter damages its cargo if the transport is hit, otherwise false.
     */
    @Override
    public boolean alwaysDamageCargoIfTransportHit() {
        // TW p. 137 - Resolve damage per the Cargo Carrier rules (TW p. 261)
        return true;
    }
}

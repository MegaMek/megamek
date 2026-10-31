/*
 * Copyright (C) 2026 The MegaMek Team. All Rights Reserved.
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
package megamek.server.totalWarfare;

import java.util.Map;

import megamek.common.equipment.ExternalCargo;
import megamek.common.equipment.ICarryable;
import megamek.common.equipment.Transporter;
import megamek.common.game.Game;
import megamek.common.units.Entity;

/**
 * Keeps carriers pointed at the game's own copy of the units they carry. A unit in a Mek's arms or other external
 * cargo is stored as an object, not an id. When the game swaps in a new copy of that unit (a lobby edit arrives from
 * a client), the carrier would keep the old copy, and dropping it would place that copy while the game's unit stays
 * carried, off the board (#9335).
 */
final class CarriedUnitRelinker {

    private CarriedUnitRelinker() {
    }

    /**
     * Points carried-unit references at the game's current units after a client's copy of a unit has been swapped in.
     * Works both ways: a carried unit that was updated, and an updated carrier whose own copy brought copies of what
     * it carries.
     *
     * @param game        the game the unit was swapped into
     * @param updatedUnit the unit that was just swapped in
     */
    static void relinkAfterUpdate(Game game, Entity updatedUnit) {
        if (updatedUnit.getTransportId() != Entity.NONE) {
            Entity carrier = game.getEntity(updatedUnit.getTransportId());
            if (carrier != null) {
                relinkCarriedUnit(carrier, updatedUnit);
            }
        }
        for (Transporter transporter : updatedUnit.getTransports()) {
            if (transporter instanceof ExternalCargo externalCargo) {
                for (ICarryable carryable : externalCargo.getCarryables()) {
                    if (carryable instanceof Entity carriedCopy) {
                        Entity carriedUnit = game.getEntity(carriedCopy.getId());
                        if (carriedUnit != null) {
                            relinkCarriedUnit(updatedUnit, carriedUnit);
                        }
                    }
                }
            }
        }
    }

    /**
     * Returns the game's own instance of a carried unit, which a carrier may hold an older copy of. Cargo that is not
     * a unit, or a unit the game does not know, comes back unchanged.
     *
     * @param game  the game the cargo belongs to
     * @param cargo the cargo as the carrier holds it
     *
     * @return the game's instance of that unit, or the cargo itself
     */
    static ICarryable currentInstanceOf(Game game, ICarryable cargo) {
        if (cargo instanceof Entity carriedCopy) {
            Entity carriedUnit = game.getEntity(carriedCopy.getId());
            if (carriedUnit != null) {
                return carriedUnit;
            }
        }
        return cargo;
    }

    /**
     * Points every reference the carrier holds to the carried unit at the given instance, both in its carried objects
     * and in its external cargo transporters.
     */
    private static void relinkCarriedUnit(Entity carrier, Entity carriedUnit) {
        for (Map.Entry<Integer, ICarryable> entry : carrier.getCarriedObjects().entrySet()) {
            if (carriedUnit.equals(entry.getValue())) {
                entry.setValue(carriedUnit);
            }
        }
        for (Transporter transporter : carrier.getTransports()) {
            if (transporter instanceof ExternalCargo externalCargo) {
                externalCargo.relinkCarriedUnit(carriedUnit);
            }
        }
    }
}

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

package megamek.common.units;

import java.util.HashSet;
import java.util.Map;
import java.util.Vector;

import megamek.common.board.Coords;
import megamek.common.moves.MobileStructureAirMovement;

/** Airborne mobile carriers use the normal fighter bay launch rules (TO:AUE p.37, TW pp.86–87). */
public final class MobileStructureBayLaunch {
    private MobileStructureBayLaunch() { }

    public static boolean available(MobileStructure carrier, Coords position, int facing, int elevation) {
        if (!MobileStructureAirMovement.isAir(carrier) || carrier.isDestroyed() || carrier.isDoomed()
              || carrier.getGame() == null || position == null) {
            return false;
        }
        var footprint = carrier.computeBuildingCoordsForPositionAndFacing(position, facing);
        return MobileStructureAirMovement.absoluteBase(carrier, position, elevation)
              > MobileStructureAirMovement.landingSurface(carrier, footprint);
    }

    /** Bay keys are indices in the ordinary launch dialog's fighter-bay list; reject forged/duplicate cargo ids. */
    public static boolean valid(MobileStructure carrier, Coords position, int facing, int elevation,
          Map<Integer, Vector<Integer>> launches) {
        if (!available(carrier, position, facing, elevation) || launches == null || launches.isEmpty()) {
            return false;
        }
        var bays = carrier.getFighterBays();
        var seen = new HashSet<Integer>();
        for (var entry : launches.entrySet()) {
            if (entry.getKey() == null || entry.getKey() < 0 || entry.getKey() >= bays.size()
                  || entry.getValue() == null || entry.getValue().isEmpty()) {
                return false;
            }
            var bay = bays.get(entry.getKey());
            if (bay.getUsableDoors() <= 0) {
                return false;
            }
            var eligible = bay.getLaunchableUnits();
            for (Integer id : entry.getValue()) {
                var unit = id == null ? null : carrier.getGame().getEntity(id);
                if (unit == null || !seen.add(id) || !eligible.contains(unit) || !carrier.getLoadedUnits().contains(unit)
                      || unit.isDestroyed() || unit.isDoomed() || unit.isShutDown() || unit.isManualShutdown()
                      || !unit.getCrew().isActive() || unit.wasLoadedThisTurn()
                      || !(unit instanceof IAero) || unit instanceof Jumpship
                      || ((IAero) unit).isLaunchProhibitedByAtmosphere()) {
                    return false;
                }
            }
        }
        return true;
    }
}

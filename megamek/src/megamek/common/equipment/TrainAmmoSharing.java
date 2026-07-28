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

package megamek.common.equipment;

import java.util.ArrayList;
import java.util.List;

import megamek.common.annotations.Nullable;
import megamek.common.game.Game;
import megamek.common.units.Entity;

/**
 * Stateless rules helper for ammunition shared along a tractor-and-trailer train. A unit may feed its weapons from the
 * ammo bins of the unit it tows and the unit towing it, so an ammo bin is not necessarily owned by the unit firing it.
 * <p>
 * This is the single definition of which units may supply each other. The client offers exactly this set in the ammo
 * dropdown and the server validates against it, so the two cannot drift apart.
 * </p>
 * Extracted from {@link Entity} and {@code TWGameManager} so the rule does not add to either of those already very
 * large classes; this mirrors the codebase's other Entity-operating utilities ({@link BridgeLayerLogic},
 * {@code megamek.common.compute.Compute}).
 *
 * @author Claude Code (Opus 5)
 */
public final class TrainAmmoSharing {

    private TrainAmmoSharing() {
    }

    /**
     * @param shooter the unit whose weapons are being loaded
     *
     * @return every ammo bin the unit may draw from, in display order: its own bins first, then those of the unit
     *       towing it, then those of the unit it tows. Units not in a train return only their own bins.
     */
    public static List<AmmoMounted> getSharedAmmo(Entity shooter) {
        List<AmmoMounted> sharedAmmo = new ArrayList<>(shooter.getAmmo());
        Game game = shooter.getGame();
        if (game == null) {
            return sharedAmmo;
        }
        if (shooter.getTowedBy() != Entity.NONE) {
            Entity ahead = game.getEntity(shooter.getTowedBy());
            if (ahead != null) {
                sharedAmmo.addAll(ahead.getAmmo());
            }
        }
        if (shooter.getTowing() != Entity.NONE) {
            Entity behind = game.getEntity(shooter.getTowing());
            if (behind != null) {
                sharedAmmo.addAll(behind.getAmmo());
            }
        }
        return sharedAmmo;
    }

    /**
     * Whether one unit may fire another's ammo. The server must check this because the ammo bin named in an ammo
     * change packet arrives from the client.
     *
     * @param shooter     the unit firing the weapon
     * @param ammoCarrier the unit that owns the ammo bin
     *
     * @return {@code true} when the carrier is the shooter itself or a unit directly connected to it in the same
     *       train, {@code false} otherwise
     */
    public static boolean canShareAmmoWith(Entity shooter, Entity ammoCarrier) {
        if (shooter.equals(ammoCarrier)) {
            return true;
        }
        int carrierId = ammoCarrier.getId();
        return (shooter.getTowedBy() == carrierId) || (shooter.getTowing() == carrierId);
    }

    /**
     * Reconnects weapons linked to another unit's ammo bin after the owning unit has been transferred.
     * <p>
     * Packets are Java-serialized object graphs and a {@link Mounted} holds a hard reference to its owning entity, so a
     * weapon linked to a trailer's bin drags a copy of that trailer along with it. The receiving side would otherwise
     * hold a detached duplicate rather than the unit the game knows about. Links to bins the unit owns itself are left
     * alone, and a link whose carrier has left the game is cleared so nothing keeps firing a detached copy.
     * </p>
     *
     * @param entity the unit whose weapon links should be checked
     * @param game   the game holding the canonical units, or {@code null} to do nothing
     */
    public static void relinkExternalAmmo(Entity entity, @Nullable Game game) {
        if (game == null) {
            return;
        }
        for (WeaponMounted weapon : entity.getTotalWeaponList()) {
            if (!(weapon.getLinked() instanceof AmmoMounted linkedAmmo)) {
                continue;
            }
            Entity staleCarrier = linkedAmmo.getEntity();
            if ((staleCarrier == null) || staleCarrier.equals(entity)) {
                continue;
            }
            Entity canonicalCarrier = game.getEntity(staleCarrier.getId());
            if (canonicalCarrier == null) {
                weapon.setLinked(null);
                continue;
            }
            weapon.setLinked(canonicalCarrier.getAmmo(staleCarrier.getEquipmentNum(linkedAmmo)));
        }
    }
}

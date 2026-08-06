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
 * Sharing reaches one coupling in each direction and no further; see {@link #canShareAmmoWith} for the ruling.
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
     * <p>
     * Only the units on either side of a coupling share, not the whole train. Xotl ruled on this in the official
     * rules Q&amp;A (battletech.com forums, topic 74296): "Only vehicles directly coupled can share ammo", and in a
     * trailer-tractor-trailer arrangement "the tractor may pull from either trailer, but one trailer may not pull
     * from the other".
     * </p>
     * This makes a Mobile Long Tom convoy weaker than its fluff: three carriages hold about 75 rounds between them,
     * but the gun only ever reaches the 25 in the carriage hitched to it. That is the ruling, not an oversight. Do
     * not widen this to the whole train on the strength of the convoy description or of TM's "Trailers act as part
     * of the Tractor for movement, stacking and firing" - the Q&amp;A was asked about exactly this case.
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
     * Drops any weapon link pointing at ammo the unit may no longer fire, and reloads the weapon from a bin it may
     * still use.
     * <p>
     * A weapon linked to a trailer's ammo bin keeps that link when the train uncouples, and the firing path never
     * re-checks the coupling: {@code AmmoWeaponHandler.checkAmmo} takes whatever {@code getLinked} returns, and the
     * server only consults {@link #canShareAmmoWith} when the player changes ammo. Left alone, a tractor goes on
     * firing a detached trailer's ammo from any distance. Call this on every unit whose train membership changed.
     * </p>
     * The replacement is chosen before the stale link is cleared, so a unit that carries the same munition itself
     * keeps firing that munition rather than falling back to whatever sits in its first bin.
     *
     * @param entity the unit whose weapon links should be re-checked
     */
    public static void dropUncoupledAmmoLinks(Entity entity) {
        for (WeaponMounted weapon : entity.getTotalWeaponList()) {
            if (!(weapon.getLinked() instanceof AmmoMounted linkedAmmo)) {
                continue;
            }
            Entity ammoCarrier = linkedAmmo.getEntity();
            if ((ammoCarrier == null) || canShareAmmoWith(entity, ammoCarrier)) {
                continue;
            }
            entity.loadWeaponWithSameAmmo(weapon);
            if (weapon.getLinked() == linkedAmmo) {
                // Nothing legal to fall back on. Leaving the weapon unlinked is safe: the handler reloads it from
                // this unit's own bins when it next fires.
                weapon.setLinked(null);
            }
        }
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

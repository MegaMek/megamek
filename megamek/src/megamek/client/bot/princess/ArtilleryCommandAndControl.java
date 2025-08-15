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
package megamek.client.bot.princess;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;
import java.util.Vector;

import megamek.common.Coords;
import megamek.common.Entity;

/**
 * ArtilleryCommandAndControl class represents the artillery command and control for the bot. It keeps track of the
 * artillery order given to it.
 *
 * @author Luana Coppio
 */
public class ArtilleryCommandAndControl {

    public enum ArtilleryOrder {
        HALT,
        AUTO,
        BARRAGE,
        VOLLEY,
        SINGLE,
    }

    public enum SpecialAmmo {
        NONE,
        SMOKE,
        FLARE,
        MINE
    }

    private ArtilleryOrder artilleryOrder = ArtilleryOrder.AUTO;
    private final Vector<Coords> artilleryTargets = new Vector<>();
    private final Set<Integer> shooterUnits = new HashSet<>();
    private SpecialAmmo ammo = SpecialAmmo.NONE;

    public void addArtilleryTargets(Collection<Coords> targets) {
        artilleryTargets.addAll(targets);
    }

    public boolean contains(Coords position) {
        return artilleryTargets.contains(position);
    }

    public void removeArtilleryTarget(Coords coords) {
        artilleryTargets.remove(coords);
    }

    public void removeArtilleryTargets() {
        artilleryTargets.clear();
    }

    public Set<Coords> getArtilleryTargets() {
        return new HashSet<>(artilleryTargets);
    }

    public boolean isArtilleryHalted() {
        return artilleryOrder == ArtilleryOrder.HALT;
    }

    public boolean isArtilleryAuto() {
        return artilleryOrder == ArtilleryOrder.AUTO;
    }

    public boolean isArtilleryBarrage() {
        return artilleryOrder == ArtilleryOrder.BARRAGE;
    }

    public boolean isArtilleryVolley() {
        return artilleryOrder == ArtilleryOrder.VOLLEY;
    }

    public boolean isArtillerySingle() {
        return artilleryOrder == ArtilleryOrder.SINGLE;
    }

    public void setArtilleryOrder(ArtilleryOrder order) {
        setArtilleryOrder(order, SpecialAmmo.NONE);
    }

    public void setArtilleryOrder(ArtilleryOrder order, SpecialAmmo ammo) {
        artilleryOrder = order;
        this.ammo = ammo;
        shooterUnits.clear();
    }

    public boolean isSmokeAmmo() {
        return ammo == SpecialAmmo.SMOKE;
    }

    public boolean isFlareAmmo() {
        return ammo == SpecialAmmo.FLARE;
    }

    public boolean isMineAmmo() {
        return ammo == SpecialAmmo.MINE;
    }

    public boolean isSpecialAmmo() {
        return ammo != SpecialAmmo.NONE;
    }

    /**
     * Add the shooter is already in the list of shooters. Used to make sure that in case we are doing a volley we know
     * which units are already shooting.
     *
     * @param shooter the shooter unit
     *
     * @return false if the shooter is already in the list of shooters, true otherwise
     */
    public boolean setShooter(Entity shooter) {
        return shooterUnits.add(shooter.getId());
    }

    public void reset() {
        shooterUnits.clear();
        artilleryTargets.clear();
        ammo = SpecialAmmo.NONE;
        artilleryOrder = ArtilleryOrder.AUTO;
    }
}

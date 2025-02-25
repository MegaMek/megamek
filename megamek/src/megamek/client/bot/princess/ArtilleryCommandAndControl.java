/*
 * Copyright (c) 2025 - The MegaMek Team. All Rights Reserved.
 *
 * This program is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License as published by the Free
 * Software Foundation; either version 2 of the License, or (at your option)
 * any later version.
 *
 * This program is distributed in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 * or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License
 * for more details.
 *
 */

package megamek.client.bot.princess;

import megamek.common.Coords;
import megamek.common.Entity;

import java.util.*;

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
        MINE,
        HOMING
    }

    private ArtilleryOrder artilleryOrder = ArtilleryOrder.AUTO;
    private final Vector<Coords> artilleryTargets = new Vector<>();
    private final Set<Integer> shooterUnits = new HashSet<>();
    private SpecialAmmo ammo = SpecialAmmo.NONE;
    private int roundOrder = -1;

    public void addArtilleryTarget(Coords target) {
        artilleryTargets.add(target);
    }

    public void addArtilleryTargets(Collection<Coords> targets) {
        artilleryTargets.addAll(targets);
    }

    public boolean contains(Coords position) {
        return artilleryTargets.contains(position);
    }

    public Optional<Coords> peekTarget() {
        return artilleryTargets.isEmpty() ? Optional.empty() : Optional.of(artilleryTargets.get(artilleryTargets.size() - 1));
    }

    public boolean hasTargets() {
        return !artilleryTargets.isEmpty();
    }

    public int getTargetCount() {
        return artilleryTargets.size();
    }

    public Optional<Coords> poolArtilleryTarget() {
        int size = artilleryTargets.size();
        if (size == 0) {
            return Optional.empty();
        }
        return Optional.of(artilleryTargets.remove(size - 1));
    }

    public void removeArtilleryTarget(Coords coords) {
        artilleryTargets.remove(coords);
    }

    public Coords getArtilleryTarget() {
        return artilleryTargets.stream().findAny().orElse(null);
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

    public int getRoundOrder() {
        return roundOrder;
    }

    public void setArtilleryAmmo(SpecialAmmo ammo) {
        this.ammo = ammo;
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

    public boolean isHomingAmmo() {
        return ammo == SpecialAmmo.HOMING;
    }

    public boolean isSpecialAmmo() {
        return ammo != SpecialAmmo.NONE;
    }

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

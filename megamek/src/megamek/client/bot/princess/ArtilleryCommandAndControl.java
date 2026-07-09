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

import java.util.Arrays;
import java.util.Collection;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.Set;
import java.util.Vector;

import megamek.common.board.Coords;
import megamek.common.equipment.AmmoType;
import megamek.common.units.Entity;

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
        COUNTER_BATTERY,
    }

    /**
     * The ammo type a fire mission should use, mapped to the artillery munitions it covers. STANDARD is a plain
     * high-explosive round; the {@code utility} flag marks zero-damage munitions (smoke, flare, mines and variants)
     * that the bot only fires when explicitly ordered at a hex.
     */
    public enum SpecialAmmo {
        STANDARD(false),
        SMOKE(true, AmmoType.SMOKE_MUNITIONS),
        FLARE(true, AmmoType.FLARE_MUNITIONS),
        MINE(true, AmmoType.MINE_MUNITIONS),
        LASER_INHIBITING(true, AmmoType.Munitions.M_LASER_INHIB),
        VIBRABOMB(true, AmmoType.Munitions.M_VIBRABOMB_IV),
        FAE(false, AmmoType.Munitions.M_FAE),
        CLUSTER(false, AmmoType.Munitions.M_CLUSTER),
        INFERNO(false, AmmoType.Munitions.M_INFERNO_IV),
        FLECHETTE(false, AmmoType.Munitions.M_FLECHETTE),
        ADA(false, AmmoType.Munitions.M_ADA),
        DAVY_CROCKETT(false, AmmoType.Munitions.M_DAVY_CROCKETT_M),
        HOMING(false, AmmoType.Munitions.M_HOMING);

        private final boolean utility;
        private final Set<AmmoType.Munitions> munitions;

        SpecialAmmo(boolean utility, AmmoType.Munitions... munitions) {
            this.utility = utility;
            this.munitions = (munitions.length == 0)
                  ? EnumSet.noneOf(AmmoType.Munitions.class)
                  : EnumSet.copyOf(Arrays.asList(munitions));
        }

        SpecialAmmo(boolean utility, Set<AmmoType.Munitions> munitions) {
            this.utility = utility;
            this.munitions = EnumSet.copyOf(munitions);
        }

        /**
         * @return {@code true} for zero-damage utility munitions (smoke, flare, mines and their variants), which the bot only
         *       fires when the player explicitly orders them at a hex
         */
        public boolean isUtility() {
            return utility;
        }

        /**
         * @return The munition types this category covers (empty for STANDARD)
         */
        public Set<AmmoType.Munitions> getMunitions() {
            return munitions;
        }

        /**
         * Classifies a loaded ammo bin's munition set into a fire-mission ammo category, or STANDARD when it is a plain
         * high-explosive round not matched by any special category.
         *
         * @param ammoMunitions The bin's munition types
         *
         * @return The matching category
         */
        public static SpecialAmmo forMunitions(Set<AmmoType.Munitions> ammoMunitions) {
            for (SpecialAmmo candidate : values()) {
                if ((candidate != STANDARD) && !candidate.munitions.isEmpty()
                      && candidate.munitions.containsAll(ammoMunitions)) {
                    return candidate;
                }
            }
            return STANDARD;
        }
    }

    private ArtilleryOrder artilleryOrder = ArtilleryOrder.AUTO;
    private final Vector<Coords> artilleryTargets = new Vector<>();
    private final Set<Integer> shooterUnits = new HashSet<>();
    private SpecialAmmo ammo = SpecialAmmo.STANDARD;

    public void addArtilleryTargets(Collection<Coords> targets) {
        artilleryTargets.addAll(targets);
    }

    public boolean contains(Coords position) {
        return artilleryTargets.contains(position);
    }

    @Deprecated(since = "0.51.0", forRemoval = true)
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

    @Deprecated(since = "0.51.0", forRemoval = true)
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

    /**
     * @return {@code true} if the bot is in forced counter-battery mode: its artillery prioritizes firing the selected
     *       ammo back at any observed off-board enemy battery.
     */
    public boolean isCounterBattery() {
        return artilleryOrder == ArtilleryOrder.COUNTER_BATTERY;
    }

    public void setArtilleryOrder(ArtilleryOrder order) {
        setArtilleryOrder(order, SpecialAmmo.STANDARD);
    }

    public void setArtilleryOrder(ArtilleryOrder order, SpecialAmmo ammo) {
        artilleryOrder = order;
        this.ammo = ammo;
        shooterUnits.clear();
    }

    /**
     * @return The special-ammo type the current fire mission should use
     */
    public SpecialAmmo getAmmo() {
        return ammo;
    }

    public boolean isHomingAmmo() {
        return ammo == SpecialAmmo.HOMING;
    }

    @Deprecated(since = "0.51.0", forRemoval = true)
    public boolean isSpecialAmmo() {
        return ammo != SpecialAmmo.STANDARD;
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

    /**
     * Checks whether the given unit has already taken its shot during the current volley fire mission.
     *
     * @param shooter the shooter unit
     *
     * @return {@code true} if the shooter has already fired during this volley
     */
    public boolean hasAlreadyFired(Entity shooter) {
        return shooterUnits.contains(shooter.getId());
    }

    public void reset() {
        shooterUnits.clear();
        artilleryTargets.clear();
        ammo = SpecialAmmo.STANDARD;
        artilleryOrder = ArtilleryOrder.AUTO;
    }
}

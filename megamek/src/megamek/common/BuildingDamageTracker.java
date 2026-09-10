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

package megamek.common;

import java.io.Serial;
import java.io.Serializable;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import megamek.common.board.BoardLocation;
import megamek.common.board.Coords;
import megamek.common.enums.GamePhase;
import megamek.common.units.Entity;
import megamek.common.units.IBuilding;

/**
 * Rounds Castles Brian damage once per attacker, hex and phase (TO:AR p. 124; TW p. 238). The signed remainders
 * also retain damage already rounded up: two five-point hits must not remove two points of capital armor.
 * Stored on Game so saving during a phase does not discard an attacker's fractional damage.
 */
public class BuildingDamageTracker implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;

    private final Map<DamageKey, Remainders> remainders = new HashMap<>();
    private final Set<BoardLocation> changedHexes = new HashSet<>();
    private int round = -1;
    private GamePhase phase;

    private record DamageKey(int attackerId, int boardId, int buildingId, Coords coords) implements Serializable {}

    private static class Remainders implements Serializable {
        @Serial
        private static final long serialVersionUID = 1L;

        private long armor;
        private long cf;
    }

    /** Damage to stored capital points, and the part of this individual standard-scale hit that reached the CF. */
    public record Damage(int armor, int cf, int throughArmor) {}

    /**
     * Convert standard-scale damage without rounding individual weapons or missile clusters independently.
     * Armor uses 10:1 conversion; the errata-corrected CF rule uses 20:1. Attacks originating inside the structure
     * bypass armor. Unattributed environmental damage is resolved as a separate event rather than a shared attacker.
     */
    public Damage resolve(IBuilding building, Coords coords, int damage, int attackerId, boolean ignoreArmor,
          int currentRound, GamePhase currentPhase) {
        if (round != currentRound || phase != currentPhase) {
            clear();
            round = currentRound;
            phase = currentPhase;
        }
        if (damage <= 0) {
            return new Damage(0, 0, 0);
        }
        Remainders remainder = attackerId == Entity.NONE ? new Remainders()
              : remainders.computeIfAbsent(new DamageKey(attackerId, building.getBoardId(), building.getId(), coords),
                    key -> new Remainders());

        int armorDamage = 0;
        long toCF = damage;
        if (!ignoreArmor) {
            int armor = Math.max(0, building.getArmor(coords));
            long pending = damage + remainder.armor;
            toCF = Math.max(0, pending - armor * 10L);
            armorDamage = (int) Math.min(armor, (pending + 5) / 10);
            remainder.armor = toCF > 0 ? 0 : pending - armorDamage * 10L;
        }
        // Historical fractions affect the CF total, but cannot make this individual hit breach the threshold.
        int throughArmor = (int) Math.min(damage, toCF);
        long pendingCF = toCF + remainder.cf;
        int cfDamage = (int) ((pendingCF + 10) / 20);
        remainder.cf = pendingCF - cfDamage * 20L;
        if (armorDamage > 0 || cfDamage > 0) {
            changedHexes.add(BoardLocation.of(coords, building.getBoardId()));
        }
        return new Damage(armorDamage, cfDamage, throughArmor);
    }

    /** Armor-only hits also require a building update at the end of the phase. */
    public boolean hasChanges(IBuilding building, Coords coords) {
        return changedHexes.contains(BoardLocation.of(coords, building.getBoardId()));
    }

    public void clearChanges() {
        changedHexes.clear();
    }

    public void clear() {
        remainders.clear();
        clearChanges();
        round = -1;
        phase = null;
    }
}

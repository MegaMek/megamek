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
package megamek.common;

import java.util.HashMap;

import megamek.common.BombType.BombTypeEnum;
/**
 * Represents a collection of bombs with their quantities.
 */
public class BombLoadout extends HashMap<BombTypeEnum, Integer> {

    public BombLoadout() {
        super();
    }
        
    public BombLoadout(BombLoadout bombs) {
        super();
        if (bombs != null) {
            bombs.entrySet().stream()
                .filter(entry -> entry.getKey() != BombTypeEnum.NONE && entry.getValue() > 0)
                .forEach(entry -> this.put(entry.getKey(), entry.getValue()));
        }
    }

    @Override
    public Integer put(BombTypeEnum key, Integer count) {
        if (key == BombTypeEnum.NONE) {
            throw new IllegalArgumentException("Cannot add NONE bomb type");
        }
        // If count is zero or negative, remove the bomb type from the loadout
        if (count <= 0) {
            return remove(key);
        }
        return super.put(key, count);
    }

    /**
     * Returns the count of bombs of a given type in this loadout.
     * If the bomb type does not exist, returns 0.
     * @param bombType the type of bomb to check
     * @return the count of bombs of the specified type
     */
    public int getCount(BombTypeEnum bombType) {
        return getOrDefault(bombType, 0);
    }

    /**
     * Adds a specified number of bombs of a given type to this loadout.
     * If the bomb type does not exist, it will be added with the specified count.
     * If the count is zero or negative, the bomb type will be removed from the loadout.
     * @param bombType the type of bomb to add
     * @param count the number of bombs to add
     */
    public void addBombs(BombTypeEnum bombType, int count) {
        put(bombType, getCount(bombType) + count);
    }

    /**
     * Returns the total number of bombs in this loadout.
     * @return total number of bombs
     */
    public int getTotalBombs() {
        return values().stream().mapToInt(Integer::intValue).sum();
    }

    /**
     * Returns the total cost of all bombs in this loadout.
     * @return total cost of bombs
     */
    public int getTotalBombCost() {
        return entrySet().stream()
            .mapToInt(entry -> entry.getKey().getCost() * entry.getValue())
            .sum();
    }

    /**
     * Checks if this loadout contains any guided ordnance that requires TAG
     * @return true if loadout contains ordnance that requires TAG
     */
    public boolean hasGuidedOrdnance() {
        return keySet().stream().anyMatch(BombTypeEnum::isGuided);
    }

    /**
     * Checks if this loadout contains bombs capable of ground attack
     * @return true if loadout contains any ground-capable bombs
     */
    public boolean canGroundBomb() {
        return keySet().stream().anyMatch(BombTypeEnum::canGroundBomb);
    }

    /**
     * Checks if this loadout contains bombs capable of space attack
     * @return true if loadout contains any space-capable bombs
     */
    public boolean canSpaceBomb() {
        return keySet().stream().anyMatch(BombTypeEnum::canSpaceBomb);
    }
}

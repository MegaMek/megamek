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
package megamek.client.ratgenerator;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * How likely a unit drawn for a faction is to be salvage, and whose salvage it is.
 *
 * <p>A unit table holds only the faction's own units. After each unit is drawn, it is replaced by another
 * faction's unit with this chance. The other faction is picked by its weight here.</p>
 *
 * @param percent       the chance, from 0 to 100, that a drawn unit is salvage
 * @param factionWeights the factions salvage comes from, with their relative weights; never {@code null}
 */
public record SalvageChance(double percent, Map<FactionRecord, Double> factionWeights) {

    /** No salvage at all. */
    public static final SalvageChance NONE = new SalvageChance(0.0, Map.of());

    /**
     * Keeps only factions with a positive weight, in the order given, and clamps the chance to 0-100.
     */
    public SalvageChance {
        percent = Math.clamp(percent, 0.0, 100.0);
        Map<FactionRecord, Double> kept = new LinkedHashMap<>();
        for (Map.Entry<FactionRecord, Double> entry : factionWeights.entrySet()) {
            if ((entry.getValue() != null) && (entry.getValue() > 0.0)) {
                kept.put(entry.getKey(), entry.getValue());
            }
        }
        factionWeights = Collections.unmodifiableMap(kept);
    }

    /** @return {@code true} if a drawn unit can be salvage */
    public boolean hasSalvage() {
        return (percent > 0.0) && !factionWeights.isEmpty();
    }

    /**
     * @param faction a salvage source
     *
     * @return that faction's share of all salvage, from 0 to 1; 0 for a faction that is not a source
     */
    public double shareOf(FactionRecord faction) {
        double total = 0.0;
        for (double weight : factionWeights.values()) {
            total += weight;
        }
        Double weight = factionWeights.get(faction);
        return ((weight == null) || (total <= 0.0)) ? 0.0 : weight / total;
    }
}

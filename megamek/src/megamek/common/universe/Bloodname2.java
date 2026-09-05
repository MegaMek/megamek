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
package megamek.common.universe;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * A Bloodname, together with the Houses that descend from it.
 *
 * <p>Named Bloodname2 to sit alongside MekHQ's Bloodname class, which is the game-facing view of the
 * same legacy; this is the data as it is stored.</p>
 */
@SuppressWarnings("unused") // Fields are assigned when factions are loaded from YAML
public class Bloodname2 {

    @JsonProperty("clan")
    private String clan;

    @JsonProperty("name")
    private String name;

    @JsonProperty("houses")
    private List<BloodnameHouse> houses = new ArrayList<>();

    /**
     * The Clan that founded this Bloodname, which is not necessarily the only Clan whose warriors may
     * hold it - a name that is not exclusive can be granted elsewhere, and the Wars of Reaving moved
     * many legacies between Clans.
     *
     * @return the founding Clan's faction key
     */
    public String getClan() {
        return clan;
    }

    /**
     * @return the Bloodname itself, as a warrior would carry it
     */
    public String getName() {
        return name;
    }

    /**
     * @return every House descending from this Bloodname; usually one, occasionally more. The list is
     *       read-only - use {@link #addHouses(List)} to record another House against this name.
     */
    public List<BloodnameHouse> getHouses() {
        return Collections.unmodifiableList(houses);
    }

    /**
     * Records further Houses against this Bloodname.
     *
     * <p>The same Bloodname can be filed under more than one Clan, and each file names its own Houses.
     * Merging them here keeps a shared legacy whole instead of reducing it to whichever file loaded
     * last.</p>
     *
     * @param additionalHouses the Houses to add
     */
    public void addHouses(List<BloodnameHouse> additionalHouses) {
        houses.addAll(additionalHouses);
    }

    @Override
    public String toString() {
        return "[Bloodname2] " + name + " of " + clan + " (" + houses.size() + " house(s))";
    }
}

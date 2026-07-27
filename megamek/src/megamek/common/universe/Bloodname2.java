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
import java.util.List;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * A Bloodname, together with the Houses that descend from it.
 *
 * <p>Named Bloodname2 to sit alongside MekHQ's older Bloodname class while the two coexist; that one
 * loads from bloodnames.xml, this one from the Clan faction files.</p>
 */
@SuppressWarnings("unused") // Fields are assigned when factions are loaded from YAML
public class Bloodname2 {

    @JsonProperty("name")
    private String name;

    @JsonProperty("houses")
    private List<BloodnameHouse> houses = new ArrayList<>();

    /**
     * @return the Bloodname itself, as a warrior would carry it
     */
    public String getName() {
        return name;
    }

    /**
     * @return every House descending from this Bloodname; usually one, occasionally more
     */
    public List<BloodnameHouse> getHouses() {
        return houses;
    }

    @Override
    public String toString() {
        return "[Bloodname2] " + name + " (" + houses.size() + " house(s))";
    }
}

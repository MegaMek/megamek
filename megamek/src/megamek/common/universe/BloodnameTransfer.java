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

import com.fasterxml.jackson.annotation.JsonProperty;
import megamek.common.annotations.Nullable;

/**
 * A Bloodname House passing from one Clan to another, and the year it happened.
 *
 * <p>Covers both directions the data records: absorbed, where another Clan took the legacy over, and
 * acquired, where this Clan took one on.</p>
 */
@SuppressWarnings("unused") // Fields are assigned when factions are loaded from YAML
public class BloodnameTransfer {

    @JsonProperty("clan")
    private String clan;

    @JsonProperty("date")
    private Integer date;

    /**
     * @return the faction key of the Clan on the other side of the transfer
     */
    public String getClan() {
        return clan;
    }

    /**
     * @return the year of the transfer, or {@code null} if the data does not record one
     */
    public @Nullable Integer getDate() {
        return date;
    }

    @Override
    public String toString() {
        return "[BloodnameTransfer] " + clan + (date == null ? "" : " (" + date + ")");
    }
}

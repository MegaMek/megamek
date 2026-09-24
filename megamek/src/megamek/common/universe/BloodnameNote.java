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
 * Something recorded about a Bloodname House that only became true in a particular year.
 *
 * <p>A House's summary describes it as it has always been. Anything that happened to it at a point
 * in time belongs here instead, so a campaign is not told about events still in its future - a
 * warrior in 3050 should not read that their name was noted as Limited in 3085.</p>
 */
@SuppressWarnings("unused") // Fields are assigned when Bloodnames are loaded from YAML
public class BloodnameNote {

    @JsonProperty("year")
    private Integer year;

    @JsonProperty("text")
    private String text;

    /**
     * @return the year this became true, or {@code null} if the data does not record one, in which
     *       case it is shown regardless of the campaign year
     */
    public @Nullable Integer getYear() {
        return year;
    }

    /**
     * @return what happened, as a sentence
     */
    public String getText() {
        return text;
    }

    /**
     * @param campaignYear the year the campaign has reached
     *
     * @return {@code true} once the campaign has reached the year this note describes
     */
    public boolean hasHappenedBy(int campaignYear) {
        return (year == null) || (campaignYear >= year);
    }

    @Override
    public String toString() {
        return "[BloodnameNote] " + (year == null ? "" : year + ": ") + text;
    }
}

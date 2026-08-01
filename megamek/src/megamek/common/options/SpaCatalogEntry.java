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

package megamek.common.options;

import megamek.common.annotations.Nullable;

/**
 * One Campaign Operations Special Pilot Ability in the {@link SpaCatalog}: its implementation status, CamOps page,
 * point cost, and localized abbreviated rules text.
 *
 * <p>This record is static reference data. It is never part of a serialized object graph (saved games, MUL files),
 * so it deliberately has no {@code SerializationHelper} converter - do not add it to a serialized class without
 * one.</p>
 *
 * @param key        for implemented SPAs, the {@link IOption} name (an {@link OptionsConstants} value); for
 *                   placeholders, a stable lower_snake_case key chosen to match the natural option name a future
 *                   implementation would use
 * @param status     how completely MegaMek implements the ability
 * @param camOpsPage the page in Campaign Operations (2024 Rev 5th Printing) describing the ability
 * @param costText   the ability's XP cost as printed; a plain number for most ("2"), a range for some ("1-4")
 */
public record SpaCatalogEntry(String key, SpaImplementationStatus status, int camOpsPage, String costText) {

    /**
     * @return {@code true} if this ability has no {@link IOption} in MegaMek and exists only as a grayed-out
     *       catalog row in the pilot options UI
     */
    public boolean isPlaceholder() {
        return status == SpaImplementationStatus.NOT_IMPLEMENTED;
    }

    /**
     * @return the localized display name for a placeholder ability. Implemented abilities take their name from
     *       their {@link IOption#getDisplayableName()} instead; calling this for one returns a missing-resource
     *       marker.
     */
    public String getDisplayableName() {
        return Messages.getString("SpaCatalog." + key + ".displayableName");
    }

    /** @return the localized one-line summary of the ability's book effect */
    public String getAbbreviatedEffect() {
        return Messages.getString("SpaCatalog." + key + ".effect");
    }

    /**
     * @return the audit's note on which parts of the book rule MegaMek does not honor, or {@code null} when there
     *       is no such note (fully implemented abilities and placeholders)
     */
    public @Nullable String getMissingText() {
        String missingText = Messages.getString("SpaCatalog." + key + ".missing");
        return missingText.startsWith("!") ? null : missingText;
    }
}

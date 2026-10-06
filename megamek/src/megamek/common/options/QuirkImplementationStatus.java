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
 * How completely MegaMek implements a quirk, per the audit of the quirk tables in Campaign Operations (2024 Rev 5th
 * Printing) pp. 228 and 232 and the BattleMech Manual (7th Printing) pp. 82-89 against the MegaMek engine.
 *
 * <p>The status of an implemented quirk is stored alongside its name and description in
 * {@code common/options/messages.properties}, under the option's {@code .working} key.</p>
 *
 * @see QuirkCatalog
 */
public enum QuirkImplementationStatus {
    /** The quirk's rules take effect in play. */
    IMPLEMENTED("QuirkImplementationStatus.implemented", "1"),
    /**
     * The quirk has some effect, but not its full rule - for example one that changes only a unit's cost and never
     * its behaviour in a game.
     */
    PARTIAL("QuirkImplementationStatus.partial", "partial"),
    /**
     * The quirk can be set on a unit and is saved with it, but the MegaMek engine ignores it. It may still matter to
     * MekHQ (maintenance and repair rolls) or serve as a record of the unit's design.
     */
    NOT_IMPLEMENTED("QuirkImplementationStatus.notImplemented", "0"),
    /**
     * The quirk exists in the rule books but has no MegaMek option at all. It cannot be set on a unit and appears
     * only as a grayed-out catalog placeholder.
     */
    NOT_IN_MEGAMEK("QuirkImplementationStatus.notInMegaMek", null);

    private final String messageKey;
    /** The {@code .working} resource value this status is written as, or {@code null} if it has none. */
    private final String resourceValue;

    QuirkImplementationStatus(String messageKey, @Nullable String resourceValue) {
        this.messageKey = messageKey;
        this.resourceValue = resourceValue;
    }

    /** @return the localized, player-facing name of this status (e.g. "Not implemented") */
    public String getDisplayableName() {
        return Messages.getString(messageKey);
    }

    /**
     * @return {@code true} if a quirk with this status has no effect at all on play. Both the quirks MegaMek ignores
     *       and those it has no option for are inert in a game.
     */
    public boolean hasNoGameEffect() {
        return (this == NOT_IMPLEMENTED) || (this == NOT_IN_MEGAMEK);
    }

    /**
     * Parses the value of an option's {@code .working} resource key.
     *
     * @param workingValue the raw resource value, or {@code null} when the key is absent
     *
     * @return the matching status, or {@code null} if the value is absent or not recognized. A {@code null} return
     *       means the quirk's status is simply unknown, and callers should show no status rather than guess.
     */
    public static @Nullable QuirkImplementationStatus parse(@Nullable String workingValue) {
        if (workingValue == null) {
            return null;
        }
        String trimmedValue = workingValue.trim();
        for (QuirkImplementationStatus status : values()) {
            if ((status.resourceValue != null) && trimmedValue.equalsIgnoreCase(status.resourceValue)) {
                return status;
            }
        }
        return null;
    }
}

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

/**
 * How completely MegaMek implements a Campaign Operations Special Pilot Ability, per the audit of CamOps (2024 Rev
 * 5th Printing) pp. 71-82 against the MegaMek engine.
 *
 * @see SpaCatalog
 */
public enum SpaImplementationStatus {
    /** The SPA's rules are implemented as written. */
    FULL("SpaImplementationStatus.full"),
    /** The SPA works in play, with small deviations from the book (see the audit for specifics). */
    FULL_MINOR_GAPS("SpaImplementationStatus.fullMinorGaps"),
    /** Some of the SPA's rules work; significant parts are missing. */
    PARTIAL("SpaImplementationStatus.partial"),
    /** The SPA has no game effect in MegaMek; it exists only as a catalog placeholder. */
    NOT_IMPLEMENTED("SpaImplementationStatus.notImplemented");

    private final String messageKey;

    SpaImplementationStatus(String messageKey) {
        this.messageKey = messageKey;
    }

    /** @return the localized, player-facing name of this status (e.g. "Partial") */
    public String getDisplayableName() {
        return Messages.getString(messageKey);
    }
}

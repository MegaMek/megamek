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
 * Which of MegaMek's two quirk option sets a quirk belongs to: the chassis quirks held by the unit ({@link Quirks})
 * or the per-weapon quirks held by each mount ({@link WeaponQuirks}).
 *
 * <p>This is not a cosmetic distinction. The two sets share several option codes - {@code fast_reload} and
 * {@code mod_weapons} each name both a chassis quirk and a weapon quirk - so a quirk is only uniquely identified by
 * its kind together with its code. Each kind also names its own resource bundle prefix, which is how
 * {@link QuirkCatalog} finds a quirk's implementation status and rules reference.</p>
 */
public enum QuirkKind {
    /** A chassis quirk, registered in {@link Quirks} and held by the unit as a whole. */
    UNIT("QuirksInfo"),
    /** A weapon quirk, registered in {@link WeaponQuirks} and held by an individual weapon mount. */
    WEAPON("WeaponQuirksInfo");

    private final String optionsInfoName;

    QuirkKind(String optionsInfoName) {
        this.optionsInfoName = optionsInfoName;
    }

    /**
     * @param code   the quirk's option name (an {@link OptionsConstants} {@code QUIRK_} value)
     * @param suffix the metadata key suffix, without its leading dot (for example {@code "working"})
     *
     * @return the full resource key for that piece of quirk metadata, in the
     *       {@code megamek.common.options.messages} bundle
     */
    public String resourceKey(String code, String suffix) {
        return optionsInfoName + ".option." + code + "." + suffix;
    }
}

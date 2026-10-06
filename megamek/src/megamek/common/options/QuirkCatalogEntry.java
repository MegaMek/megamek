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
 * One real quirk option in the {@link QuirkCatalog}: how completely MegaMek implements it and where its rule is
 * printed.
 *
 * <p>This record is static reference data assembled from the resource bundle at class load. It is never part of a
 * serialized object graph (saved games, MUL files), so it deliberately has no {@code SerializationHelper} converter -
 * do not add it to a serialized class without one.</p>
 *
 * @param kind      whether this is a chassis quirk or a weapon quirk. Needed to identify the quirk, because
 *                  {@code fast_reload} and {@code mod_weapons} each name both a chassis and a weapon quirk.
 * @param code      the quirk's {@link IOption} name (an {@link OptionsConstants} {@code QUIRK_} value)
 * @param status    how completely MegaMek implements the quirk
 * @param rulesBook the abbreviated rule book the quirk is printed in (e.g. {@code "BMM"}), or {@code null} when the
 *                  bundle carries no reference for it
 * @param rulesPage the page in that book, as printed, or {@code null} when the bundle carries no reference
 */
public record QuirkCatalogEntry(QuirkKind kind, String code, QuirkImplementationStatus status,
                                @Nullable String rulesBook, @Nullable String rulesPage) {

    /**
     * @return the localized "Book p.Page" citation for this quirk, or {@code null} when the bundle carries no rules
     *       reference for it
     */
    public @Nullable String getRulesReference() {
        if ((rulesBook == null) || (rulesPage == null)) {
            return null;
        }
        return Messages.getString("QuirkCatalog.rulesReference", rulesBook, rulesPage);
    }
}

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
 * A quirk that exists in the rule books but has no MegaMek option at all.
 *
 * <p>Unlike a {@link QuirkCatalogEntry}, a placeholder has no {@link IOption} behind it: it cannot be set on a unit,
 * never serializes into a saved game or MUL file, and never reaches MekHQ. It exists so the quirks UI can list the
 * book quirk as a grayed-out, searchable row - telling the player the quirk is missing rather than leaving them to
 * wonder where it went.</p>
 *
 * <p>Keeping placeholders out of {@link Quirks} and {@link WeaponQuirks} is what makes them free of side effects. Do
 * not "promote" one by adding it here and to the option list; when a quirk is implemented, delete its placeholder and
 * give the new option a {@code .working} resource entry instead.</p>
 *
 * @param key       a stable lower_snake_case key, chosen to match the natural option name a future implementation
 *                  would use
 * @param groupKey  the quirk group the placeholder belongs in, either {@link Quirks#POS_QUIRKS} or
 *                  {@link Quirks#NEG_QUIRKS}; the UI lists it in that column
 * @param rulesBook the abbreviated rule book the quirk is printed in (e.g. {@code "CO"})
 * @param rulesPage the page in that book, as printed
 */
public record QuirkPlaceholder(String key, String groupKey, String rulesBook, String rulesPage) {

    /** @return the localized display name of the missing quirk */
    public String getDisplayableName() {
        return Messages.getString("QuirkCatalog." + key + ".displayableName");
    }

    /** @return the localized one-line summary of the quirk's book effect */
    public String getDescription() {
        return Messages.getString("QuirkCatalog." + key + ".description");
    }

    /** @return the localized "Book p.Page" citation for this quirk */
    public String getRulesReference() {
        return Messages.getString("QuirkCatalog.rulesReference", rulesBook, rulesPage);
    }
}

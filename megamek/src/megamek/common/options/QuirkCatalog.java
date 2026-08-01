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

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.Enumeration;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import megamek.common.annotations.Nullable;
import megamek.logging.MMLogger;

/**
 * Catalog of every MegaMek quirk with its implementation status and rules reference, plus the book quirks MegaMek has
 * no option for at all.
 *
 * <p>The status of a real quirk is not listed here. It is read from the quirk's own resource entries in
 * {@code common/options/messages.properties}, which already carry {@code .working}, {@code .rulesBook} and
 * {@code .rulesPage} next to the quirk's name and description. Keeping the data there means a quirk's status lives
 * with the rest of its text, and adding a quirk to {@link Quirks} or {@link WeaponQuirks} needs no edit here.</p>
 *
 * <p>{@link QuirkPlaceholder}s are the exception: a quirk with no option has no resource entry to hang status on, so
 * the small list of them is declared below. They have no {@link IOption}, so they never serialize into saved games or
 * MUL files and never surface in MekHQ.</p>
 *
 * <p>Source: {@code docs/MegaMek_Quirks_Reference.xlsx}, reconciled against the engine on 2026-08-01. When the audit
 * is revised, update the {@code .working} resource values rather than adding data here.</p>
 */
public final class QuirkCatalog {

    private static final MMLogger LOGGER = MMLogger.create(QuirkCatalog.class);

    /** Resource suffix holding a quirk's implementation status; see {@link QuirkImplementationStatus#parse}. */
    private static final String WORKING_SUFFIX = "working";
    private static final String RULES_BOOK_SUFFIX = "rulesBook";
    private static final String RULES_PAGE_SUFFIX = "rulesPage";

    /**
     * Book quirks with no MegaMek option. Slow Traverse (CamOps p.232) is the only quirk in the CamOps tables that
     * MegaMek has no option for; every other book quirk can at least be set on a unit.
     */
    private static final List<QuirkPlaceholder> PLACEHOLDERS = List.of(
          new QuirkPlaceholder("slow_traverse", Quirks.NEG_QUIRKS, "CO", "232"));

    private static final Map<QuirkKind, Map<String, QuirkCatalogEntry>> ENTRIES_BY_KIND = loadEntries();

    private QuirkCatalog() {
    }

    /**
     * Reads the status and rules reference of every registered quirk out of the resource bundle, once. A quirk whose
     * bundle entry has no usable {@code .working} value is left out of the catalog entirely, so the UI shows it with
     * no status rather than claiming one.
     */
    private static Map<QuirkKind, Map<String, QuirkCatalogEntry>> loadEntries() {
        Map<QuirkKind, Map<String, QuirkCatalogEntry>> entriesByKind = new LinkedHashMap<>();
        entriesByKind.put(QuirkKind.UNIT, loadEntries(QuirkKind.UNIT, new Quirks()));
        entriesByKind.put(QuirkKind.WEAPON, loadEntries(QuirkKind.WEAPON, new WeaponQuirks()));
        return Map.copyOf(entriesByKind);
    }

    private static Map<String, QuirkCatalogEntry> loadEntries(QuirkKind kind, AbstractOptions quirkOptions) {
        Map<String, QuirkCatalogEntry> entries = new LinkedHashMap<>();
        List<String> uncataloguedCodes = new ArrayList<>();
        for (Enumeration<IOption> options = quirkOptions.getOptions(); options.hasMoreElements(); ) {
            String code = options.nextElement().getName();
            QuirkImplementationStatus status =
                  QuirkImplementationStatus.parse(resourceValue(kind, code, WORKING_SUFFIX));
            if (status == null) {
                // Summarized after the loop rather than logged per quirk
                uncataloguedCodes.add(code);
                continue;
            }
            entries.put(code, new QuirkCatalogEntry(kind, code, status,
                  resourceValue(kind, code, RULES_BOOK_SUFFIX),
                  resourceValue(kind, code, RULES_PAGE_SUFFIX)));
        }

        // Answers "why does this quirk show no implementation status?" from megamek.log alone
        if (uncataloguedCodes.isEmpty()) {
            LOGGER.debug("[QuirkCatalog] Loaded {} {} quirks, all with an implementation status",
                  entries.size(), kind);
        } else {
            LOGGER.warn("[QuirkCatalog] Loaded {} {} quirks; {} have no usable '{}' resource value and will show "
                        + "no status: {}",
                  entries.size(), kind, uncataloguedCodes.size(), WORKING_SUFFIX, uncataloguedCodes);
        }
        return Map.copyOf(entries);
    }

    /**
     * @return the value of one of a quirk's metadata resource keys, or {@code null} when the bundle has no such key.
     *       {@link Messages} reports a missing key by returning it wrapped in exclamation marks.
     */
    private static @Nullable String resourceValue(QuirkKind kind, String code, String suffix) {
        String value = Messages.getString(kind.resourceKey(code, suffix));
        return value.startsWith("!") ? null : value;
    }

    /**
     * @param kind whether to look among the chassis quirks or the weapon quirks. The two sets share some codes, so
     *             the kind is part of the lookup.
     * @param code the quirk's {@link IOption} name
     *
     * @return the catalog entry for that quirk, or empty when the resource bundle records no status for it
     */
    public static Optional<QuirkCatalogEntry> getEntry(QuirkKind kind, String code) {
        return Optional.ofNullable(ENTRIES_BY_KIND.get(kind).get(code));
    }

    /**
     * @param groupKey a quirk group key, either {@link Quirks#POS_QUIRKS} or {@link Quirks#NEG_QUIRKS}
     *
     * @return the book quirks MegaMek has no option for that belong in that group, sorted by display name. The quirks
     *       UI shows these as grayed-out placeholder rows.
     */
    public static List<QuirkPlaceholder> getPlaceholders(String groupKey) {
        return PLACEHOLDERS.stream()
              .filter(placeholder -> placeholder.groupKey().equals(groupKey))
              .sorted(Comparator.comparing(QuirkPlaceholder::getDisplayableName))
              .toList();
    }

    /** @return every book quirk MegaMek has no option for, in no particular order */
    public static List<QuirkPlaceholder> getAllPlaceholders() {
        return PLACEHOLDERS;
    }

    /**
     * @param kind whether to list the chassis quirks or the weapon quirks
     *
     * @return every catalog entry of that kind, in no particular order
     */
    public static Collection<QuirkCatalogEntry> getEntries(QuirkKind kind) {
        return ENTRIES_BY_KIND.get(kind).values();
    }
}

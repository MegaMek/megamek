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

import static megamek.common.options.SpaImplementationStatus.FULL;
import static megamek.common.options.SpaImplementationStatus.FULL_MINOR_GAPS;
import static megamek.common.options.SpaImplementationStatus.NOT_IMPLEMENTED;
import static megamek.common.options.SpaImplementationStatus.PARTIAL;

import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Catalog of every Special Pilot Ability in Campaign Operations (2024 Rev 5th Printing) pp. 71-82, with its MegaMek
 * implementation status, page reference, XP cost, and an abbreviated effect line.
 *
 * <p>Implemented abilities are keyed by their {@link IOption} name and enrich the pilot options UI with status and
 * book data. Abilities MegaMek does not implement have no {@link IOption} at all - they appear here only so the UI
 * can show them as grayed-out, searchable placeholder rows. Keeping placeholders out of {@link PilotOptions} means
 * they never serialize into saved games or MUL files and never surface in MekHQ (whose {@code PersonnelOptions}
 * extends {@link PilotOptions}).</p>
 *
 * <p>MegaMek abilities that are not CamOps SPAs (AToW, MaxTech, unofficial) have no catalog entry and render
 * without status decoration.</p>
 *
 * <p>Source: {@code docs/Pilot SPAs - CampOps vs MegaMek.xlsx} (audit of 2026-07-22, MegaMek development main).
 * When the audit is revised, regenerate the entry list and the {@code SpaCatalog.*} keys in
 * {@code common/options/messages.properties} rather than editing them by hand.</p>
 */
public final class SpaCatalog {

    private static final Map<String, SpaCatalogEntry> ENTRIES_BY_KEY = List.of(
          entry(OptionsConstants.PILOT_ANIMAL_MIMIC, PARTIAL, 72, "2"),
          entry("antagonizer", NOT_IMPLEMENTED, 73, "3"),
          entry(OptionsConstants.GUNNERY_BLOOD_STALKER, FULL, 73, "2"),
          entry(OptionsConstants.GUNNERY_CLUSTER_HITTER, PARTIAL, 73, "2"),
          entry("combat_intuition", NOT_IMPLEMENTED, 73, "3"),
          entry(OptionsConstants.PILOT_CROSS_COUNTRY, PARTIAL, 73, "2"),
          entry("demoralizer", NOT_IMPLEMENTED, 74, "3"),
          entry(OptionsConstants.PILOT_DODGE_MANEUVER, PARTIAL, 74, "2"),
          entry("dust_off", NOT_IMPLEMENTED, 74, "2"),
          entry(OptionsConstants.MISC_EAGLE_EYES, PARTIAL, 74, "2"),
          entry(OptionsConstants.MISC_ENV_SPECIALIST, PARTIAL, 74, "2"),
          entry("fist_fire", NOT_IMPLEMENTED, 75, "2"),
          entry(OptionsConstants.INFANTRY_FOOT_CAV, PARTIAL, 75, "1"),
          entry(OptionsConstants.MISC_FORWARD_OBSERVER, PARTIAL, 75, "1"),
          entry(OptionsConstants.GUNNERY_GOLDEN_GOOSE, FULL, 75, "3"),
          entry("ground_hugger", NOT_IMPLEMENTED, 75, "2"),
          entry("heavy_horse", NOT_IMPLEMENTED, 75, "2"),
          entry(OptionsConstants.PILOT_HVY_LIFTER, PARTIAL, 76, "1"),
          entry("hopper", NOT_IMPLEMENTED, 76, "1"),
          entry(OptionsConstants.PILOT_HOT_DOG, FULL, 76, "2"),
          entry(OptionsConstants.MISC_HUMAN_TRO, FULL, 76, "1"),
          entry("iron_will", NOT_IMPLEMENTED, 76, "1"),
          entry(OptionsConstants.PILOT_JUMPING_JACK, PARTIAL, 76, "2"),
          entry("light_horseman", NOT_IMPLEMENTED, 76, "2"),
          entry("lucky", NOT_IMPLEMENTED, 77, "1-4"),
          entry(OptionsConstants.PILOT_MANEUVERING_ACE, PARTIAL, 77, "2"),
          entry("marksman", NOT_IMPLEMENTED, 77, "2"),
          entry(OptionsConstants.PILOT_MELEE_MASTER, FULL_MINOR_GAPS, 77, "2"),
          entry(OptionsConstants.PILOT_MELEE_SPECIALIST, FULL_MINOR_GAPS, 77, "1"),
          entry(OptionsConstants.GUNNERY_MULTI_TASKER, FULL, 78, "2"),
          entry("natural_grace", NOT_IMPLEMENTED, 78, "3"),
          entry(OptionsConstants.GUNNERY_OBLIQUE_ARTILLERY, PARTIAL, 78, "1"),
          entry(OptionsConstants.GUNNERY_OBLIQUE_ATTACKER, FULL, 78, "1"),
          entry(OptionsConstants.GUNNERY_RANGE_MASTER, FULL, 78, "2"),
          entry("ride_the_wash", NOT_IMPLEMENTED, 79, "4"),
          entry(OptionsConstants.GUNNERY_SANDBLASTER, FULL_MINOR_GAPS, 79, "2"),
          entry(OptionsConstants.PILOT_SHAKY_STICK, FULL, 79, "2"),
          entry("sharpshooter", NOT_IMPLEMENTED, 79, "4"),
          entry("slugger", NOT_IMPLEMENTED, 80, "1"),
          entry(OptionsConstants.GUNNERY_SNIPER, FULL, 80, "3"),
          entry("speed_demon", NOT_IMPLEMENTED, 80, "2"),
          entry("stand_aside", NOT_IMPLEMENTED, 80, "1"),
          entry("street_fighter", NOT_IMPLEMENTED, 80, "2"),
          entry("swordsman", NOT_IMPLEMENTED, 80, "2"),
          entry(OptionsConstants.MISC_TACTICAL_GENIUS, PARTIAL, 80, "3"),
          entry("terrain_master_drag_racer", NOT_IMPLEMENTED, 81, "1"),
          entry(OptionsConstants.PILOT_TM_FOREST_RANGER, FULL, 81, "3"),
          entry(OptionsConstants.PILOT_TM_FROGMAN, PARTIAL, 81, "3"),
          entry(OptionsConstants.PILOT_TM_MOUNTAINEER, PARTIAL, 81, "3"),
          entry(OptionsConstants.PILOT_TM_NIGHTWALKER, PARTIAL, 81, "3"),
          entry(OptionsConstants.PILOT_TM_SWAMP_BEAST, PARTIAL, 81, "3"),
          entry(OptionsConstants.INFANTRY_URBAN_GUERRILLA, FULL_MINOR_GAPS, 82, "1"),
          entry(OptionsConstants.GUNNERY_WEAPON_SPECIALIST, FULL, 82, "3"),
          entry(OptionsConstants.PILOT_WIND_WALKER, PARTIAL, 82, "2"),
          entry(OptionsConstants.PILOT_ZWEIHANDER, FULL, 82, "2"))
          .stream()
          .collect(Collectors.toUnmodifiableMap(SpaCatalogEntry::key, Function.identity()));

    private static final List<SpaCatalogEntry> PLACEHOLDERS = ENTRIES_BY_KEY.values().stream()
          .filter(SpaCatalogEntry::isPlaceholder)
          .sorted(Comparator.comparing(SpaCatalogEntry::getDisplayableName))
          .toList();

    private SpaCatalog() {
    }

    private static SpaCatalogEntry entry(String key, SpaImplementationStatus status, int camOpsPage,
          String costText) {
        return new SpaCatalogEntry(key, status, camOpsPage, costText);
    }

    /**
     * @param optionName an {@link IOption} name (or placeholder key)
     *
     * @return the catalog entry for that ability, or empty for abilities that are not CamOps SPAs
     */
    public static Optional<SpaCatalogEntry> getEntry(String optionName) {
        return Optional.ofNullable(ENTRIES_BY_KEY.get(optionName));
    }

    /**
     * @return the CamOps SPAs MegaMek does not implement, sorted by display name. These have no {@link IOption};
     *       the pilot options UI shows them as grayed-out placeholder rows.
     */
    public static List<SpaCatalogEntry> getPlaceholders() {
        return PLACEHOLDERS;
    }

    /** @return every catalog entry, in no particular order */
    public static Collection<SpaCatalogEntry> getAllEntries() {
        return ENTRIES_BY_KEY.values();
    }
}

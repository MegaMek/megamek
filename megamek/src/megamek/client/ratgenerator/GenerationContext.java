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
package megamek.client.ratgenerator;

import megamek.common.annotations.Nullable;

/**
 * What a player chose when they generated a force: the faction and command it was rolled for, the
 * year, and the equipment rating. The army generators put all of this on screen and then drop it the
 * moment the units are added, which leaves the lobby holding a pile of units with no idea what they
 * were meant to be. Capturing it lets later work organize those units the way the faction actually
 * organizes its units.
 *
 * <p>The faction key carries the command as well: a sub-command is written as a dotted key -
 * {@code "FS.CH"} is the Ceti Hussars of the Federated Suns, {@code "CC.SIJ"} the St. Ives Janissaries
 * - so one string answers both "which faction" and "which command". The plain key ({@code "FS"}) is
 * the faction at large.</p>
 *
 * @param faction the {@link FactionRecord} key, dotted for a sub-command; never blank
 * @param year    the year the units were rolled for
 * @param rating  the equipment rating ("A" to "F", "Keshik", "SL"), or {@code null} when none chosen
 * @param source  which generator produced the units, so a report can say where this came from
 */
public record GenerationContext(String faction, int year, @Nullable String rating, Source source) {

    /** Which generator a force came from. */
    public enum Source {
        /** The RAT Generator tab: knows faction, command, year and rating. */
        RAT_GENERATOR,
        /** The Formation tab: knows faction, command, year and rating, plus a formation type. */
        FORMATION_BUILDER,
        /** The Force Generator tab: knows everything and builds its own structure. */
        FORCE_GENERATOR,
        /** A generator that knows none of this - the context is a default, not a choice. */
        UNSPECIFIED
    }

    /**
     * The fallback when a player generated units without choosing a faction: the Inner Sphere at
     * large, which is what every faction lookup in this package already falls back to.
     *
     * @param year the year to record
     *
     * @return a context naming no particular command
     */
    public static GenerationContext defaultFor(int year) {
        return new GenerationContext(FactionRecord.IS_GENERAL_KEY, year, null, Source.UNSPECIFIED);
    }

    /**
     * Builds a context from what a generator tab holds, falling back to the Inner Sphere when the tab
     * has no faction selected.
     *
     * @param factionRecord the chosen faction, may be {@code null}
     * @param year          the chosen year
     * @param rating        the chosen equipment rating, may be {@code null} or blank
     * @param source        which generator this is
     *
     * @return the context, never {@code null}
     */
    public static GenerationContext of(@Nullable FactionRecord factionRecord, int year,
          @Nullable String rating, Source source) {
        if (factionRecord == null) {
            return new GenerationContext(FactionRecord.IS_GENERAL_KEY, year,
                  ((rating == null) || rating.isBlank()) ? null : rating, source);
        }
        return new GenerationContext(factionRecord.getKey(), year,
              ((rating == null) || rating.isBlank()) ? null : rating, source);
    }

    /** @return {@code true} when the faction key names a command rather than a faction at large */
    public boolean hasSubCommand() {
        return faction.contains(".");
    }

    /** @return the parent faction key: {@code "FS"} for {@code "FS.CH"}, unchanged when there is none */
    public String parentFaction() {
        return hasSubCommand() ? faction.substring(0, faction.indexOf('.')) : faction;
    }

    /**
     * @return the faction's display name for the recorded year, falling back to the raw key when the
     *       generator data does not know it
     */
    public String factionDisplayName() {
        FactionRecord factionRecord = RATGenerator.getInstance().getFaction(faction);
        return (factionRecord == null) ? faction : factionRecord.getName(year);
    }

    /**
     * A one-line summary for logs and diagnostics, e.g. {@code "Ceti Hussars, 3067, rating A"}. Not
     * for display: anything a player reads is built from the parts through {@code Messages} so it can
     * be translated.
     *
     * @return the summary
     */
    public String describe() {
        StringBuilder description = new StringBuilder(factionDisplayName());
        description.append(", ").append(year);
        if (rating != null) {
            description.append(", rating ").append(rating);
        }
        return description.toString();
    }
}

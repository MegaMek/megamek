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

import java.util.ArrayList;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonProperty;
import megamek.common.annotations.Nullable;

/**
 * One Bloodname House: a single genetic legacy descending from one founder.
 *
 * <p>A Bloodname can have more than one House. Sixteen do - Kerensky descends separately from Andery
 * and from Nicholas - which is why a Bloodname holds a list of these rather than carrying the founder
 * itself.</p>
 *
 * <p>The fields fall into two groups. The first describes what the game needs to decide whether a
 * warrior can hold this name in a given year: the phenotype it breeds for, whether it is exclusive to
 * its Clan, and what became of it during and after the Wars of Reaving. The second is descriptive -
 * who founded it, who has held it, where it is written about - and exists to be shown to the player.</p>
 */
@SuppressWarnings("unused") // Fields are assigned when factions are loaded from YAML
public class BloodnameHouse {

    // region Identity
    @JsonProperty("founder")
    private String founder;

    @JsonProperty("founderFullName")
    private String founderFullName;

    @JsonProperty("founderRank")
    private String founderRank;

    @JsonProperty("founderAffiliation")
    private String founderAffiliation;
    // endregion

    // region Mechanical - consulted when choosing a Bloodname for a warrior
    @JsonProperty("phenotype")
    private String phenotype;

    /** Exclusive to its origin Clan; other Clans do not grant it. */
    @JsonProperty("exclusive")
    private boolean exclusive;

    /**
     * Year exclusivity ended, or {@code null} while it still holds.
     *
     * <p>Exclusivity is not permanent. The Council of Six Clans fought Trials in 3084 that turned
     * several exclusive names into shared ones, and a Trial of Possession can end it at any time.</p>
     */
    @JsonProperty("exclusiveUntil")
    private Integer exclusiveUntil;

    /** Held by only a handful of warriors; a Bloodcount of five or fewer active Bloodheritages. */
    @JsonProperty("limited")
    private boolean limited;

    /**
     * Year the name became Limited, or {@code null} when the data does not say.
     *
     * <p>A name is not Limited from its founding - it becomes so as its Bloodcount falls. Without a
     * year the restriction reads as having always applied, which puts a thirty-second-century state
     * of affairs in front of a campaign centuries earlier.</p>
     */
    @JsonProperty("limitedSince")
    private Integer limitedSince;

    /** Year the founding Clan was abjured, after which only that Clan grants the name. */
    @JsonProperty("abjured")
    private Integer abjured;

    /** Year the legacy fell dormant, or {@code null} if it never did. */
    @JsonProperty("dormant")
    private Integer dormant;

    /** Year the legacy was reaved during the Wars of Reaving. */
    @JsonProperty("reaved")
    private Integer reaved;

    @JsonProperty("reactivated")
    private Integer reactivated;

    /** Year the legacy was created, for names that postdate the founding. */
    @JsonProperty("created")
    private Integer created;

    /** Clans that inherited the legacy after the Wars of Reaving. */
    @JsonProperty("postReaving")
    private List<String> postReaving = new ArrayList<>();

    /** The Clan that took this legacy over, if one did. */
    @JsonProperty("absorbed")
    private BloodnameTransfer absorbed;

    /**
     * Clans that took the legacy on, and when.
     *
     * <p>A list because a single record can name several Clans at once, and because a legacy can be
     * taken on more than once. Both of these weight the draw, so a Clan lost here is a Clan that never
     * gets offered the name.</p>
     */
    @JsonProperty("acquired")
    private List<BloodnameTransfer> acquired = new ArrayList<>();

    /** Clans the legacy is shared with, and when. As {@link #acquired}, but granted rather than taken. */
    @JsonProperty("shared")
    private List<BloodnameTransfer> shared = new ArrayList<>();

    /**
     * Things recorded about this House that only became true in a given year.
     *
     * <p>Kept apart from {@link #summary}, which describes the House as it has always been, so a
     * campaign is not told about events still in its future.</p>
     */
    @JsonProperty("datedNotes")
    private List<BloodnameNote> datedNotes = new ArrayList<>();
    // endregion

    // region Descriptive - shown to the player, never consulted by the rules
    @JsonProperty("summary")
    private String summary;

    @JsonProperty("notableHolders")
    private List<BloodnameHolder> notableHolders = new ArrayList<>();

    @JsonProperty("sources")
    private List<BloodnameSource> sources = new ArrayList<>();

    /** Free prose about the House. Null until the histories are written. */
    @JsonProperty("history")
    private String history;
    // endregion

    /**
     * @return the founder's given name, which together with the Bloodname identifies this House
     */
    public String getFounder() {
        return founder;
    }

    public @Nullable String getFounderFullName() {
        return founderFullName;
    }

    public @Nullable String getFounderRank() {
        return founderRank;
    }

    public @Nullable String getFounderAffiliation() {
        return founderAffiliation;
    }

    /**
     * @return the phenotype this legacy breeds for, or {@code null} if it is not tied to one
     */
    public @Nullable String getPhenotype() {
        return phenotype;
    }

    public boolean isExclusive() {
        return exclusive;
    }

    /**
     * Whether the name is still exclusive to its origin Clan in the given year.
     *
     * @param year the year to judge it in
     *
     * @return {@code true} if exclusivity is recorded and has not yet ended
     */
    public boolean isExclusive(int year) {
        return exclusive && ((exclusiveUntil == null) || (year < exclusiveUntil));
    }

    /**
     * @return the year exclusivity ended, or {@code null} while it still holds
     */
    public @Nullable Integer getExclusiveUntil() {
        return exclusiveUntil;
    }

    public boolean isLimited() {
        return limited;
    }

    /**
     * Whether the name counts as Limited in the given year.
     *
     * <p>A name with no recorded year is treated as having always been Limited, which is the older
     * behaviour and the safer reading when the data does not say.</p>
     *
     * @param year the year to judge it in
     *
     * @return {@code true} if the name is Limited and the campaign has reached the year it became so
     */
    public boolean isLimited(int year) {
        return limited && ((limitedSince == null) || (year >= limitedSince));
    }

    /**
     * @return the year the name became Limited, or {@code null} when the data does not say
     */
    public @Nullable Integer getLimitedSince() {
        return limitedSince;
    }

    /**
     * @return everything recorded about this House that carries a date; never {@code null}
     */
    public List<BloodnameNote> getDatedNotes() {
        return datedNotes;
    }

    /**
     * The dated notes a campaign has reached, in the order the data records them.
     *
     * @param year the year the campaign has reached
     *
     * @return the notes describing events that have already happened
     */
    public List<BloodnameNote> getDatedNotesBy(int year) {
        return datedNotes.stream().filter(note -> note.hasHappenedBy(year)).toList();
    }

    public @Nullable Integer getAbjured() {
        return abjured;
    }

    public @Nullable Integer getDormant() {
        return dormant;
    }

    public @Nullable Integer getReaved() {
        return reaved;
    }

    public @Nullable Integer getReactivated() {
        return reactivated;
    }

    public @Nullable Integer getCreated() {
        return created;
    }

    public List<String> getPostReaving() {
        return postReaving;
    }

    public @Nullable BloodnameTransfer getAbsorbed() {
        return absorbed;
    }

    public List<BloodnameTransfer> getAcquired() {
        return acquired;
    }

    public List<BloodnameTransfer> getShared() {
        return shared;
    }

    public @Nullable String getSummary() {
        return summary;
    }

    public List<BloodnameHolder> getNotableHolders() {
        return notableHolders;
    }

    public List<BloodnameSource> getSources() {
        return sources;
    }

    public @Nullable String getHistory() {
        return history;
    }

    @Override
    public String toString() {
        return "[BloodnameHouse] founder: " + founder;
    }
}

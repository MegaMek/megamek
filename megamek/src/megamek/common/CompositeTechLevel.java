/*
 * Copyright (C) 2017-2025 The MegaMek Team. All Rights Reserved.
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

package megamek.common;

import java.io.Serial;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumMap;
import java.util.List;
import java.util.stream.Collectors;

import megamek.common.enums.AvailabilityValue;
import megamek.common.enums.Era;
import megamek.common.enums.Faction;
import megamek.common.enums.FactionAffiliation;
import megamek.common.enums.TechBase;
import megamek.common.enums.TechRating;
import megamek.common.eras.Eras;
import megamek.common.interfaces.ITechnology;
import megamek.common.units.Entity;

/**
 * Determines tech level dates based on tech progression of components.
 *
 * @author Neoancient
 */
public class CompositeTechLevel implements ITechnology, Serializable {
    @Serial
    private static final long serialVersionUID = -2591881133085092725L;

    private final boolean clan;
    private final boolean mixed;
    private final int introYear;
    private final Faction techFaction;
    private Integer experimental;
    private Integer advanced;
    private Integer standard;
    private List<DateRange> extinct;
    private TechRating techRating;
    private final EnumMap<Era, AvailabilityValue> availability;
    private int earliest;

    // Provides a set tech level for non-era-based use.
    private SimpleTechLevel staticTechLevel = SimpleTechLevel.INTRO;

    /**
     * @param initialTA - the base tech advancement for the composite equipment
     * @param clan      - whether the equipment tech base is Clan
     * @param mixed     - whether the equipment contains a mix of Clan and IS equipment
     * @param introYear - the year the composite equipment is first available - Prototype production common extinction
     *                  reintroduction They want to use eras - this unit is prototype - new availability type - TN to
     *                  find a prototype table -
     */
    public CompositeTechLevel(TechAdvancement initialTA, boolean clan, boolean mixed, int introYear,
          Faction techFaction) {
        this.techFaction = techFaction;
        this.clan = clan;
        this.mixed = mixed;
        this.introYear = introYear;
        earliest = initialTA.getIntroductionDate(clan, techFaction);
        extinct = new ArrayList<>();
        int protoDate = mixed ? initialTA.getPrototypeDate() : initialTA.getPrototypeDate(clan);
        int prodDate = mixed ? initialTA.getProductionDate() : initialTA.getProductionDate(clan);
        int commonDate = mixed ? initialTA.getCommonDate() : initialTA.getCommonDate(clan);
        if (commonDate == DATE_NONE) {
            standard = null;
        } else {
            standard = Math.max(commonDate, introYear);
        }
        if (prodDate == DATE_NONE || (standard != null && standard <= introYear)) {
            advanced = null;
        } else {
            advanced = Math.max(prodDate, introYear);
        }
        if (protoDate == DATE_NONE ||
              (advanced != null && advanced <= introYear) ||
              (standard != null && standard <= introYear)) {
            experimental = null;
        } else {
            experimental = Math.max(protoDate, introYear);
        }
        addExtinctionRange(mixed ? initialTA.getExtinctionDate() : initialTA.getExtinctionDate(clan),
              mixed ? initialTA.getReintroductionDate() : initialTA.getReintroductionDate(clan));
        techRating = initialTA.getTechRating();
        availability = new EnumMap<>(Era.class);
        for (Era era : Era.values()) {
            availability.put(era, initialTA.getBaseAvailability(era));
        }
        staticTechLevel = initialTA.getStaticTechLevel();
    }

    /**
     * @param entity {@link Entity} to check.
     */
    public CompositeTechLevel(Entity entity, Faction techFaction) {
        this(entity.getConstructionTechAdvancement(),
              entity.isClan(),
              entity.isMixedTech(),
              entity.getYear(),
              techFaction);
        // If the entity has the Obsolete quirk, add extinction ranges for all obsolete periods
        List<Integer> obsoleteYears = entity.getObsoleteYears();
        if (!obsoleteYears.isEmpty()) {
            setObsoleteYears(obsoleteYears);
        }
    }

    /**
     * @return - the experimental tech date range, formatted as a string
     */
    @Override
    public String getExperimentalRange() {
        if (experimental == null) {
            return "-";
        }
        int end = DATE_NONE;
        if (advanced != null) {
            end = Math.max(experimental, advanced - 1);
        } else if (standard != null) {
            end = Math.max(experimental, standard - 1);
        }
        return new DateRange(experimental, end).toString();
    }

    /**
     * @return - the advanced tech date range, formatted as a string
     */
    @Override
    public String getAdvancedRange() {
        if (advanced == null) {
            return "-";
        }
        int end = DATE_NONE;
        if (standard != null) {
            end = Math.max(advanced, standard - 1);
        }
        return new DateRange(advanced, end).toString();
    }

    /**
     * @return - the standard tech date range, formatted as a string
     */
    @Override
    public String getStandardRange() {
        if (standard == null) {
            return "-";
        }
        return new DateRange(standard).toString();
    }

    /**
     * @return - the range(s) of dates when the tech is extinct
     */
    @Override
    public String getExtinctionRange() {
        if (extinct.isEmpty()) {
            return "-";
        }
        return extinct.stream().map(DateRange::toString).collect(Collectors.joining(", "));
    }

    /**
     * Adjust the dates for various tech levels to account for the tech advancement of a new component.
     *
     * @param tech - the advancement for the new component
     */
    public void addComponent(ITechnology tech) {
        int protoDate = mixed ? tech.getPrototypeDate() : tech.getPrototypeDate(clan, techFaction);
        int prodDate = mixed ? tech.getProductionDate() : tech.getProductionDate(clan, techFaction);
        int commonDate = mixed ? tech.getCommonDate() : tech.getCommonDate(clan);
        earliest = Math.max(earliest, tech.getIntroductionDate(clan, techFaction));

        staticTechLevel = SimpleTechLevel.max(staticTechLevel, tech.getStaticTechLevel());
        //If this record is blank we ignore it
        if (protoDate == DATE_NONE && prodDate == DATE_NONE && commonDate == DATE_NONE) {
            return;
        }

        //No common date means minimum advanced; no common or production date means only experimental
        if (commonDate == DATE_NONE) {
            if (standard != null) {
                if (advanced == null) {
                    advanced = standard;
                } else {
                    advanced = Math.min(advanced, standard);
                }
                standard = null;
            }
            if (prodDate == DATE_NONE) {
                if (advanced != null) {
                    if (experimental == null) {
                        experimental = advanced;
                    } else {
                        experimental = Math.min(experimental, advanced);
                    }
                    advanced = null;
                }
            }
        }

        if (protoDate != DATE_NONE) {
            /* If there was no previous prototype stage, part of either the advanced or standard
             * tech ranges may need to be converted to experimental
             */
            if (experimental == null) {
                if (advanced != null && prodDate > advanced) {
                    experimental = advanced;
                } else if (advanced != null && prodDate < advanced) {
                    experimental = protoDate;
                } else if (standard != null) {
                    advanced = null;
                    if (prodDate > standard) {
                        experimental = standard;
                    } else {
                        // Tech never went into production, experimental straight to common
                        experimental = protoDate;
                    }
                }
            } else {
                experimental = Math.max(experimental, protoDate);
            }
        }

        if (prodDate != DATE_NONE) {
            /*If all previous tech had no advanced date but had a common date (either started common or
             * went straight from prototype to common), a chunk of the previous standard range can
             * become advanced.
             */

            if (advanced == null) {
                if (standard != null && commonDate > standard) {
                    advanced = standard;
                }
            } else {
                advanced = Math.max(prodDate, advanced);
            }
        }

        //Unless previously set to minimum advanced, check to see if the common date needs to be later.
        if (standard != null) {
            standard = Math.max(standard, commonDate);
        }

        // Sanity check
        if (experimental != null) {
            if (experimental != introYear) {
                experimental = introYear;
            }
        } else if (advanced != null) {
            if (advanced > introYear) {
                advanced = introYear;
            }
        } else if (standard != null) {
            if (standard > introYear) {
                standard = introYear;
            }
        }

        if (experimental != null) {
            if (experimental.equals(advanced) || experimental.equals(standard)) {
                experimental = null;
            }
        }

        addExtinctionRange(mixed ? tech.getExtinctionDate() : tech.getExtinctionDate(clan, techFaction),
              mixed ? tech.getReintroductionDate() : tech.getReintroductionDate(clan, techFaction));

        techRating = TechRating.fromIndex(Math.max(techRating.getIndex(), tech.getTechRating().getIndex()));

        for (Era era : Era.values()) {
            AvailabilityValue av = tech.getBaseAvailability(era);
            // Clan mixed tech units cannot use IS tech introduced during SW until 3050.
            if (clan && era == Era.SW && !tech.isClan()
                  && !techFaction.getAffiliation().equals(FactionAffiliation.CLAN)
                  && (techFaction != Faction.CS)
                  && ITechnology.getTechEra(tech.getIntroductionDate()).equals(Era.SW)) {
                av = AvailabilityValue.X;
            }
            // IS base cannot include Clan tech before 3050; after 3050 av is +1.
            if (!clan && tech.isClan()) {
                if (era == Era.SW) {
                    av = AvailabilityValue.X;
                } else {
                    int harder = Math.min(av.getIndex() + 1, AvailabilityValue.X.getIndex());
                    av = AvailabilityValue.fromIndex(harder);
                }
            }
            if (availability.get(era) == null || av.isBetterThan(availability.get(era))) {
                availability.put(era, av);
            }
        }
    }

    /**
     * @param year Year to check Tech Level for.
     *
     * @return the TechConstants tech level for a particular year
     */
    @Override
    public int getTechLevel(int year) {
        if (getStaticTechLevel() == SimpleTechLevel.UNOFFICIAL) {
            return clan ? TechConstants.T_CLAN_UNOFFICIAL : TechConstants.T_IS_UNOFFICIAL;
        }
        if (standard != null && year >= standard) {
            if (clan) {
                return TechConstants.T_CLAN_TW;
            } else if (getStaticTechLevel() == SimpleTechLevel.INTRO) {
                return TechConstants.T_INTRO_BOX_SET;
            } else {
                return TechConstants.T_IS_TW_NON_BOX;
            }
        } else if (advanced != null && year >= advanced) {
            return clan ? TechConstants.T_CLAN_ADVANCED : TechConstants.T_IS_ADVANCED;
        } else if (experimental != null && year >= experimental) {
            return clan ? TechConstants.T_CLAN_EXPERIMENTAL : TechConstants.T_IS_EXPERIMENTAL;
        }
        return TechConstants.T_TECH_UNKNOWN;
    }

    /**
     * Sets the obsolete year for this unit, adding an extinction range from that year onwards. This should be called
     * after quirks are loaded if the unit has the Obsolete quirk.
     *
     * @param obsoleteYear - the year when production of this obsolete unit ceased
     */
    public void setObsoleteYear(int obsoleteYear) {
        if (obsoleteYear > 0) {
            addExtinctionRange(obsoleteYear, DATE_NONE);
        }
    }

    /**
     * Sets multiple obsolete/reintroduction cycles for this unit based on the Obsolete quirk. The years list should be
     * in pairs: obsoleteYear, reintroYear, obsoleteYear2, reintroYear2, ... An odd number of years means the final
     * obsolete period extends indefinitely.
     *
     * @param years - list of years in order: obsolete, reintro, obsolete, reintro, ...
     */
    public void setObsoleteYears(List<Integer> years) {
        if (years == null || years.isEmpty()) {
            return;
        }

        // Process pairs of years
        for (int i = 0; i < years.size(); i += 2) {
            int obsoleteYear = years.get(i);
            // Extinction ends the year before reintroduction (e.g., reintro 3146 means extinct until 3145)
            int extinctionEnd = (i + 1 < years.size()) ? years.get(i + 1) - 1 : DATE_NONE;
            if (obsoleteYear > 0) {
                addExtinctionRange(obsoleteYear, extinctionEnd);
            }
        }
    }

    /**
     * Adds new range to collection of extinction ranges then checks for overlapping ranges and merges them.
     *
     * @param start - first year of new extinction range
     * @param end   - reintroduction date of new extinction range, or DATE_NONE if never reintroduced
     */
    private void addExtinctionRange(int start, int end) {
        if (start == DATE_NONE || (end != DATE_NONE && end <= introYear)) {
            return;
        }
        start = Math.max(introYear, start);
        extinct.add(new DateRange(start, end));
        Collections.sort(extinct);
        List<DateRange> merged = new ArrayList<>();
        DateRange previous = null;
        for (DateRange dr : extinct) {
            if (previous == null) {
                merged.add(dr);
                previous = dr;
            } else if (previous.end == null) {
                break;
            } else if (dr.start <= previous.end) {
                if ((null == dr.end) || (previous.end < dr.end)) {
                    previous.end = dr.end;
                }
            } else {
                previous = dr;
                merged.add(dr);
            }
        }
        extinct = merged;
    }

    public static class DateRange implements Serializable, Comparable<DateRange> {
        @Serial
        private static final long serialVersionUID = 3144194494591950878L;

        Integer start;
        Integer end;
        boolean startApproximate = false;
        boolean endApproximate = false;

        DateRange(int start, int end) {
            this.start = start;
            this.end = end == DATE_NONE ? null : end;
        }

        DateRange(int start) {
            this.start = start;
            this.end = null;
        }

        String formatYear(int year, boolean approximate) {
            if (year == DATE_PS) {
                return "PS";
            } else if (year == DATE_ES) {
                return "ES";
            }
            if (approximate) {
                return "~" + year;
            } else {
                return Integer.toString(year);
            }
        }

        @Override
        public String toString() {
            if (start == null) {
                return "-";
            }
            StringBuilder sb = new StringBuilder();
            sb.append(formatYear(start, startApproximate));
            if (end == null) {
                sb.append("+");
            } else {
                if (!end.equals(start)) {
                    sb.append("-").append(formatYear(end, endApproximate));
                }
            }
            return sb.toString();
        }

        @Override
        public int compareTo(DateRange other) {
            return start.compareTo(other.start);
        }
    }

    @Override
    public TechBase getTechBase() {
        return isClan() ? TechBase.CLAN : TechBase.IS;
    }

    @Override
    public boolean isClan() {
        return clan;
    }

    @Override
    public boolean isMixedTech() {
        return mixed;
    }

    @Override
    public int getIntroductionDate() {
        return introYear;
    }

    public int getEarliestTechDate() {
        return earliest;
    }

    /**
     * Returns the prototype/experimental date ranges, accounting for extinction periods
     *
     * @return List of DateRange objects representing when the unit is in prototype phase
     */
    public String getPrototypeDateRange() {
        if (experimental == null) {
            return formatDateRanges(new ArrayList<>());
        }

        int endDate = DATE_NONE;
        if (advanced != null) {
            endDate = advanced - 1;
        } else if (standard != null) {
            endDate = standard - 1;
        }

        return formatDateRanges(splitRangeByExtinctions(experimental, endDate));
    }

    /**
     * Returns the production/advanced date ranges, accounting for extinction periods
     *
     * @return List of DateRange objects representing when the unit is in production phase
     */
    public String getProductionDateRange() {
        if (advanced == null) {
            return formatDateRanges(new ArrayList<>());
        }

        int endDate = DATE_NONE;
        if (standard != null) {
            endDate = standard - 1;
        }

        return formatDateRanges(splitRangeByExtinctions(advanced, endDate));
    }

    /**
     * Returns the common/standard date ranges, accounting for extinction periods
     *
     * @return List of DateRange objects representing when the unit is in common phase
     */
    public String getCommonDateRange() {
        if (standard == null) {
            return formatDateRanges(new ArrayList<>());
        }

        return formatDateRanges(splitRangeByExtinctions(standard, DATE_NONE));
    }

    /**
     * Helper method to split a date range by extinction periods
     *
     * @param startDate The start date of the range
     * @param endDate   The end date of the range (DATE_NONE for open-ended)
     *
     * @return List of DateRange objects with extinction periods removed
     */
    private List<DateRange> splitRangeByExtinctions(int startDate, int endDate) {
        List<DateRange> result = new ArrayList<>();

        if (extinct.isEmpty()) {
            // No extinctions, return single range
            result.add(new DateRange(startDate, endDate));
            return result;
        }

        int currentStart = startDate;

        for (DateRange extinctionRange : extinct) {
            int extinctStart = extinctionRange.start;
            Integer extinctEnd = extinctionRange.end;

            // If extinction starts after our range ends, we're done
            if (endDate != DATE_NONE && extinctStart > endDate) {
                break;
            }

            // If extinction ends before our range starts, skip it
            if (extinctEnd != null && extinctEnd < currentStart) {
                continue;
            }

            // If there's a gap before the extinction, add it
            if (currentStart < extinctStart) {
                int gapEnd = Math.min(extinctStart - 1, endDate == DATE_NONE ? extinctStart - 1 : endDate);
                result.add(new DateRange(currentStart, gapEnd));
            }

            // Move past the extinction period
            if (extinctEnd != null) {
                currentStart = Math.max(currentStart, extinctEnd + 1);
            } else {
                // Extinction goes to the end of time, nothing more to add
                return result;
            }

            // If we've moved past our end date, we're done
            if (endDate != DATE_NONE && currentStart > endDate) {
                return result;
            }
        }

        // Add any remaining range after all extinctions
        if (endDate == DATE_NONE || currentStart <= endDate) {
            result.add(new DateRange(currentStart, endDate));
        }

        return result;
    }

    /**
     * Formats a list of DateRange objects as a string for display, including era information
     *
     * @param ranges List of DateRange objects
     *
     * @return Formatted string representation with era metadata
     */
    public String formatDateRanges(List<DateRange> ranges) {
        if (ranges.isEmpty()) {
            return "-";
        }

        return ranges.stream()
              .map(this::formatDateRangeWithEra)
              .collect(Collectors.joining(", "));
    }

    /**
     * Formats a single DateRange with era information
     *
     * @param range The DateRange to format
     *
     * @return Formatted string with era metadata
     */
    private String formatDateRangeWithEra(DateRange range) {
        StringBuilder result = new StringBuilder();
        String eraText = Eras.getEraText(range.start, range.end);
        return result.append(range)
              .append(eraText)
              .toString();
    }

    @Override
    public int getPrototypeDate() {
        return experimental == null ? DATE_NONE : experimental;
    }

    @Override
    public int getProductionDate() {
        return advanced == null ? DATE_NONE : advanced;
    }

    @Override
    public int getCommonDate() {
        return standard == null ? DATE_NONE : standard;
    }

    @Override
    public int getExtinctionDate() {
        if (extinct.isEmpty()) {
            return DATE_NONE;
        } else {
            return extinct.get(0).start;
        }
    }

    @Override
    public int getReintroductionDate() {
        if (extinct.isEmpty()) {
            return DATE_NONE;
        } else {
            return extinct.get(0).end == null ? DATE_NONE : extinct.get(0).end;
        }
    }

    @Override
    public TechRating getTechRating() {
        return techRating;
    }

    @Override
    public AvailabilityValue getBaseAvailability(Era era) {
        if (era == null || (ITechnology.getTechEra(introYear).getIndex() > era.getIndex())) {
            return AvailabilityValue.X;
        }
        return availability.get(era);
    }

    @Override
    public SimpleTechLevel getStaticTechLevel() {
        return staticTechLevel;
    }

    @Override
    public int getIntroductionDate(boolean clan, Faction faction) {
        return getIntroductionDate();
    }

    @Override
    public int getPrototypeDate(boolean clan, Faction faction) {
        return getPrototypeDate();
    }

    @Override
    public int getProductionDate(boolean clan, Faction faction) {
        return getProductionDate();
    }

    @Override
    public int getExtinctionDate(boolean clan, Faction faction) {
        return getExtinctionDate();
    }

    @Override
    public int getReintroductionDate(boolean clan, Faction faction) {
        return getReintroductionDate();
    }
}

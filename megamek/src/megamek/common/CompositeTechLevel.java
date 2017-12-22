/*
 * MegaMek - Copyright (C) 2017 - The MegaMek Team
 *
 * This program is free software; you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the Free Software
 * Foundation; either version 2 of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR
 * A PARTICULAR PURPOSE. See the GNU General Public License for more details.
 */
package megamek.common;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Determines tech level dates based on tech progression of components.
 * 
 * @author Neoancient
 *
 */
public class CompositeTechLevel implements ITechnology, Serializable {
    private static final long serialVersionUID = -2591881133085092725L;    

    private final boolean clan;
    private final boolean mixed;
    private final int introYear;
    private final int techFaction;
    private Integer experimental;
    private Integer advanced;
    private Integer standard;
    private List<DateRange> extinct;
    private int techRating;
    private int[] availability;
    private int earliest;
    
    // Provides a set tech level for non-era-based use.
    private SimpleTechLevel staticTechLevel = SimpleTechLevel.INTRO;
    
    /**
     * @param initialTA - the base tech advancement for the composite equipment
     * @param clan - whether the equipment tech base is Clan
     * @param mixed - whether the equipment contains a mix of Clan and IS equipment
     * @param introYear - the year the composite equipment is first available
     */
    public CompositeTechLevel(TechAdvancement initialTA,
            boolean clan, boolean mixed, int introYear, int techFaction) {
        this.techFaction = techFaction;
        this.clan = clan;
        this.mixed = mixed;
        this.introYear = introYear;
        earliest = initialTA.getIntroductionDate(clan, techFaction);
        extinct = new ArrayList<>();
        int protoDate = mixed?initialTA.getPrototypeDate() : initialTA.getPrototypeDate(clan);
        int prodDate = mixed?initialTA.getProductionDate() : initialTA.getProductionDate(clan);
        int commonDate = mixed?initialTA.getCommonDate() : initialTA.getCommonDate(clan);
        if (commonDate == DATE_NONE) {
            standard = null;
        } else {
            standard = Math.max(commonDate, introYear);
        }
        if (prodDate == DATE_NONE
                || (standard != null && standard <= introYear)) {
            advanced = null;
        } else {
            advanced = Math.max(prodDate, introYear);
        }
        if (protoDate == DATE_NONE
                || (advanced != null && advanced <= introYear)
                || (standard != null && standard <= introYear)) {
            experimental = null;
        } else {
            experimental = Math.max(protoDate, introYear);
        }
        addExtinctionRange(mixed? initialTA.getExtinctionDate() : initialTA.getExtinctionDate(clan),
                mixed? initialTA.getReintroductionDate() : initialTA.getReintroductionDate(clan));
        techRating = initialTA.getTechRating();
        availability = new int[ERA_NUM];
        for (int era = 0; era < ERA_NUM; era++) {
            availability[era] = initialTA.getBaseAvailability(era);
        }
        staticTechLevel = initialTA.getStaticTechLevel();
    }
    
    /**
     * @param en 
     */
    public CompositeTechLevel(Entity en, int techFaction) {
        this(en.getConstructionTechAdvancement(), en.isClan(), en.isMixedTech(), en.getYear(), techFaction);
    }
    
    /**
     * @return - the experimental tech date range, formatted as a string
     */
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
     * @param tech - the advancement for the new component
     */
    public void addComponent(ITechnology tech) {
        int protoDate = mixed?tech.getPrototypeDate() : tech.getPrototypeDate(clan, techFaction);
        int prodDate = mixed?tech.getProductionDate() : tech.getProductionDate(clan, techFaction);
        int commonDate = mixed?tech.getCommonDate() : tech.getCommonDate(clan);
        earliest = Math.max(earliest, tech.getIntroductionDate(clan, techFaction));
        
        staticTechLevel = SimpleTechLevel.max(staticTechLevel, tech.getStaticTechLevel());
        //If this record is blank we ignore it
        if (protoDate == DATE_NONE
                && prodDate == DATE_NONE
                && commonDate == DATE_NONE) {
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
                } else if (standard != null && prodDate > standard){
                    experimental = standard;
                    advanced = null;
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
        
        addExtinctionRange(mixed?tech.getExtinctionDate() : tech.getExtinctionDate(clan, techFaction),
                mixed?tech.getReintroductionDate() : tech.getReintroductionDate(clan, techFaction));
        
        techRating = Math.max(techRating, tech.getTechRating());
        for (int era = 0; era < ERA_NUM; era++) {
            int av = tech.getBaseAvailability(era);
            // Clan mixed tech units cannot use IS tech introduced during SW until 3050.
            if (clan && era == ERA_SW
                    && !tech.isClan()
                    && (techFaction < F_CLAN)
                    && (techFaction != F_CS)
                    && ITechnology.getTechEra(tech.getIntroductionDate()) == ERA_SW) {
                av = RATING_X; 
            }
            // IS base cannot include Clan tech before 3050; after 3050 av is +1.
            if (!clan && tech.isClan()) {
                if (era == ERA_SW) {
                    av = RATING_X;
                } else {
                    av = Math.min(av + 1, RATING_X);
                }
            }
            availability[era] = Math.max(availability[era], av);
        }
    }
    
    /**
     * @param year
     * @return - the TechConstants tech level for a particular year
     */
    public int getTechLevel(int year) {
        if (getStaticTechLevel() == SimpleTechLevel.UNOFFICIAL) {
            return clan? TechConstants.T_CLAN_UNOFFICIAL : TechConstants.T_IS_UNOFFICIAL;
        }
        if (standard != null && year >= standard) {
            if (clan) {
                return TechConstants.T_CLAN_TW;
            } else if (getStaticTechLevel() == SimpleTechLevel.INTRO) {
                return TechConstants.T_INTRO_BOXSET;
            } else {
                return TechConstants.T_IS_TW_NON_BOX;
            }
        } else if (advanced != null && year >= advanced) {
            return clan? TechConstants.T_CLAN_ADVANCED : TechConstants.T_IS_ADVANCED;
        } else if (experimental != null && year >= experimental) {
            return clan? TechConstants.T_CLAN_EXPERIMENTAL : TechConstants.T_IS_EXPERIMENTAL;
        }
        return TechConstants.T_TECH_UNKNOWN;
    }
    
    /**
     * Adds new range to collection of extinction ranges then checks for overlapping ranges
     * and merges them.
     * 
     * @param start - first year of new extinction range
     * @param end - reintroduction date of new extinction range, or DATE_NONE if never reintroduced
     */
    private void addExtinctionRange(int start, int end) {
        if (start == DATE_NONE
                || (end != DATE_NONE && end <= introYear)) {
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
    
    private static class DateRange implements Serializable, Comparable<DateRange> {
        private static final long serialVersionUID = 3144194494591950878L;
        
        Integer start = null;
        Integer end = null;
        boolean startApproximate = false;
        boolean endApproximate = false;
        
        DateRange(int start, int end) {
            this.start = start;
            this.end = end == DATE_NONE? null : end;
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
            if (start != null) {
                sb.append(formatYear(start, startApproximate));
            } else {
                sb.append("?");
            }

            if (end == null) {
                sb.append("+");
            } else {
                if (end != start) {
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
    public int getTechBase() {
        return isClan()? TECH_BASE_CLAN : TECH_BASE_IS;
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

    @Override
    public int getPrototypeDate() {
        return experimental == null? DATE_NONE : experimental;
    }

    @Override
    public int getProductionDate() {
        return advanced == null? DATE_NONE : advanced;
    }

    @Override
    public int getCommonDate() {
        return standard == null? DATE_NONE : standard;
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
            return extinct.get(0).end == null? DATE_NONE : extinct.get(0).end;
        }
    }

    @Override
    public int getTechRating() {
        return techRating;
    }

    @Override
    public int getBaseAvailability(int era) {
        if (era < 0 || era > availability.length) {
            return RATING_X;
        }
        return availability[era];
    }
    
    @Override
    public SimpleTechLevel getStaticTechLevel() {
        return staticTechLevel;
    }

    @Override
    public int getIntroductionDate(boolean clan, int faction) {
        return getIntroductionDate();
    }

    @Override
    public int getPrototypeDate(boolean clan, int faction) {
        return getPrototypeDate();
    }

    @Override
    public int getProductionDate(boolean clan, int faction) {
        return getProductionDate();
    }

    @Override
    public int getExtinctionDate(boolean clan, int faction) {
        return getExtinctionDate();
    }

    @Override
    public int getReintroductionDate(boolean clan, int faction) {
        return getReintroductionDate();
    }
}

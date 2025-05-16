/*
* MegaMek -
* Copyright (C) 2017 The MegaMek Team
*
* This program is free software; you can redistribute it and/or modify it under
* the terms of the GNU General Public License as published by the Free Software
* Foundation; either version 2 of the License, or (at your option) any later
* version.
*
* This program is distributed in the hope that it will be useful, but WITHOUT
* ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
* FOR A PARTICULAR PURPOSE. See the GNU General Public License for more
* details.
*/
package megamek.common;

import java.util.Map;
import java.util.HashMap;

/**
 * Implemented by any class that is subject to tech advancement (entities, equipment, systems, etc.)
 *
 * @author Neoancient
 */
public interface ITechnology {

    // --- Tech Base Enum ---
    enum TechBase {
        UNKNOWN(-1),
        ALL(0),
        IS(1),
        CLAN(2);

        private final int index;
        private static final Map<Integer, TechBase> INDEX_LOOKUP = new HashMap<>();
        static {
            for (TechBase tb : values()) {
                INDEX_LOOKUP.put(tb.index, tb);
            }
        }
        TechBase(int idx) { this.index = idx; }
        public int getIndex() { return index; }
        public static TechBase fromIndex(int idx) {
            TechBase tb = INDEX_LOOKUP.get(idx);
            if (tb == null) throw new IllegalArgumentException("Invalid TechBase index: " + idx);
            return tb;
        }
    }

    // --- Availability Enum ---
    enum AvailabilityValue {
        A(0, "A"),
        B(1, "B"),
        C(2, "C"),
        D(3, "D"),
        E(4, "E"),
        F(5, "F"),
        X(7, "X");

        private final int index;
        private final String name;
        private static final Map<Integer, AvailabilityValue> INDEX_LOOKUP = new HashMap<>();
        private static final Map<String, AvailabilityValue> NAME_LOOKUP = new HashMap<>();
        static {
            for (AvailabilityValue tr : values()) {
                INDEX_LOOKUP.put(tr.index, tr);
                NAME_LOOKUP.put(tr.name, tr);
            }
        }
        AvailabilityValue(int idx, String name) { this.index = idx; this.name = name; }
        public int getIndex() { return index; }
        public String getName() { return name; }
        public boolean isBetterThan(AvailabilityValue other) {
            return this.index > other.index;
        }
        public boolean isBetterOrEqualThan(AvailabilityValue other) {
            return this.index >= other.index;
        }
        public static AvailabilityValue fromIndex(int idx) {
            AvailabilityValue tr = INDEX_LOOKUP.get(idx);
            if (tr == null) throw new IllegalArgumentException("Invalid AvailabilityValue index: " + idx);
            return tr;
        }
        public static AvailabilityValue fromName(String name) {
            AvailabilityValue tr = NAME_LOOKUP.get(name);
            if (tr == null) throw new IllegalArgumentException("Invalid AvailabilityValue name: " + name);
            return tr;
        }
    }


    // --- Tech Rating Enum ---
    enum TechRating {
        A(0, "A"),
        B(1, "B"),
        C(2, "C"),
        D(3, "D"),
        E(4, "E"),
        F(5, "F"),
        FSTAR(6, "F*"),
        X(7, "X");

        private final int index;
        private final String name;
        private static final Map<Integer, TechRating> INDEX_LOOKUP = new HashMap<>();
        private static final Map<String, TechRating> NAME_LOOKUP = new HashMap<>();
        static {
            for (TechRating tr : values()) {
                INDEX_LOOKUP.put(tr.index, tr);
                NAME_LOOKUP.put(tr.name, tr);
            }
        }
        TechRating(int idx, String name) { this.index = idx; this.name = name; }
        public int getIndex() { return index; }
        public String getName() { return name; }
        public boolean isBetterThan(TechRating other) {
            return this.index > other.index;
        }
        public boolean isBetterOrEqualThan(TechRating other) {
            return this.index >= other.index;
        }
        public static TechRating fromIndex(int idx) {
            TechRating tr = INDEX_LOOKUP.get(idx);
            if (tr == null) throw new IllegalArgumentException("Invalid TechRating index: " + idx);
            return tr;
        }
        public static TechRating fromName(String name) {
            TechRating tr = NAME_LOOKUP.get(name);
            if (tr == null) throw new IllegalArgumentException("Invalid TechRating name: " + name);
            return tr;
        }
    }

    enum Era {
        SL(0),
        SW(1),
        CLAN(2),
        DA(3);

        private final int index;
        private static final Map<Integer, Era> INDEX_LOOKUP = new HashMap<>();
        static {
            for (Era era : values()) {
                INDEX_LOOKUP.put(era.index, era);
            }
        }
        Era(int idx) { this.index = idx; }
        public int getIndex() { return index; }
        public static Era fromIndex(int idx) {
            Era era = INDEX_LOOKUP.get(idx);
            if (era == null) throw new IllegalArgumentException("Invalid Era index: " + idx);
            return era;
        }
    }

    // --- Faction Affiliation Enum ---
    enum FactionAffiliation {
        NONE(0, "NONE"),
        IS(1, "IS"),
        CLAN(2, "CLAN"),
        MIXED(3, "MIXED");

        private final int index;
        private final String name;
        private static final Map<Integer, FactionAffiliation> INDEX_LOOKUP = new HashMap<>();
        static {
            for (FactionAffiliation fa : values()) {
                INDEX_LOOKUP.put(fa.index, fa);
            }
        }
        FactionAffiliation(int idx, String name) { this.index = idx; this.name = name; }
        public int getIndex() { return index; }
        public String getName() { return name; }
    }

    // --- Faction Enum ---
    enum Faction {
        NONE(-1, FactionAffiliation.NONE, "None", "None"),
        IS(0, FactionAffiliation.IS, "IS", "IS"),
        CC(1, FactionAffiliation.IS, "CC", "CC"),
        CF(2, FactionAffiliation.IS, "CF","CIR"),
        CP(3, FactionAffiliation.IS, "CP","CDP"),
        CS(4, FactionAffiliation.IS, "CS","CS"),
        DC(5, FactionAffiliation.IS, "DC", "DC"),
        EI(6, FactionAffiliation.IS, "EI", "CEI"),
        FC(7, FactionAffiliation.IS, "FC", "FC"),
        FR(8, FactionAffiliation.IS, "FR", "FRR"),
        FS(9, FactionAffiliation.IS, "FS", "FS"),
        FW(10, FactionAffiliation.IS, "FW", "FWL"),
        LC(11, FactionAffiliation.IS, "LC", "LA"),
        MC(12, FactionAffiliation.IS, "MC", "MOC"),
        MH(13, FactionAffiliation.IS, "MH", "MH"),
        OA(14, FactionAffiliation.IS, "OA", "OA"),
        TA(15, FactionAffiliation.IS, "TA", "TA"),
        TC(16, FactionAffiliation.IS, "TC", "TC"),
        TH(17, FactionAffiliation.IS, "TH", "TH"),
        RD(18, FactionAffiliation.IS, "RD", "RD"),
        RS(19, FactionAffiliation.IS, "RS", "ROS"),
        RA(20, FactionAffiliation.IS, "RA", "RA"),
        RW(21, FactionAffiliation.IS, "RW", "RWR"),
        WB(22, FactionAffiliation.IS, "WB", "WOB"),
        MERC(23, FactionAffiliation.IS, "Merc", "MERC"),
        PER(24, FactionAffiliation.IS, "Per", "Periphery"),
        CLAN(25, FactionAffiliation.CLAN, "Clan", "CLAN"),
        CBR(26, FactionAffiliation.CLAN, "CBR", "CB"),
        CBS(27, FactionAffiliation.CLAN, "CBS", "CBS"),
        CCY(28, FactionAffiliation.CLAN, "CCY", "CCO"),
        CCC(29, FactionAffiliation.CLAN, "CCC", "CCC"),
        CFM(30, FactionAffiliation.CLAN, "CFM", "CFM"),
        CGB(31, FactionAffiliation.CLAN, "CGB", "CGB"),
        CGS(32, FactionAffiliation.CLAN, "CGS", "CGS"),
        CHH(33, FactionAffiliation.CLAN, "CHH", "CHH"),
        CIH(34, FactionAffiliation.CLAN, "CIH", "CIH"),
        CJF(35, FactionAffiliation.CLAN, "CJF", "CJF"),
        CMN(36, FactionAffiliation.CLAN, "CMN", "CMG"),
        CNC(37, FactionAffiliation.CLAN, "CNC", "CNC"),
        CSF(38, FactionAffiliation.CLAN, "CSF", "CDS"),
        CSJ(39, FactionAffiliation.CLAN, "CSJ", "CSJ"),
        CSR(40, FactionAffiliation.CLAN, "CSR", "CSR"),
        CSV(41, FactionAffiliation.CLAN, "CSV", "CSV"),
        CSA(42, FactionAffiliation.CLAN, "CSA", "CSA"),
        CWM(43, FactionAffiliation.CLAN, "CWM", "CWI"),
        CWF(44, FactionAffiliation.CLAN, "CWF", "CW"),
        CWX(45, FactionAffiliation.CLAN, "CWX", "CWIE"),
        CWV(46, FactionAffiliation.CLAN, "CWV", "CWOV");

        private final int index;
        private final FactionAffiliation affiliation;
        private final String codeMM;
        private final String codeIO;
        private static final Map<Integer, Faction> INDEX_LOOKUP = new HashMap<>();
        private static final Map<String, Faction> IO_ABBR_LOOKUP = new HashMap<>();
        private static final Map<String, Faction> MM_ABBR_LOOKUP = new HashMap<>();
    
        static {
            for (Faction f : values()) {
                INDEX_LOOKUP.put(f.index, f);
                MM_ABBR_LOOKUP.put(f.codeMM, f);
                IO_ABBR_LOOKUP.put(f.codeIO, f);
            }
        }
        Faction(int idx, FactionAffiliation affiliation, String codeMM, String codeIO) {
            this.index = idx;
            this.affiliation = affiliation;
            this.codeMM = codeMM;
            this.codeIO = codeIO;
        }
        public int getIndex() { return index; }
        public FactionAffiliation getAffiliation() { return affiliation; }
        public String getCode() { return codeMM; }
        public String getCodeMM() { return codeMM; }
        public String getCodeIO() { return codeIO; }
        public static Faction fromIndex(int idx) {
            Faction f = INDEX_LOOKUP.get(idx);
            if (f == null) throw new IllegalArgumentException("Invalid Faction index: " + idx);
            return f;
        }
        public static Faction fromMMAbbr(String abbr) {
            // These abbreviations may have sub-faction dot codes; strip them.
            String baseAbbr = abbr.split("\\.")[0];
            return MM_ABBR_LOOKUP.getOrDefault(baseAbbr, NONE);
        }
        public static Faction fromIOAbbr(String abbr) {
            return IO_ABBR_LOOKUP.getOrDefault(abbr, NONE);
        }
    }
    
    // --- Constants ---
    int DATE_NONE = -1;
    int DATE_PS = 1950;
    int DATE_ES = 2100;

    boolean isClan();
    boolean isMixedTech();
    TechBase getTechBase();

    int getIntroductionDate();
    int getPrototypeDate();
    int getProductionDate();
    int getCommonDate();
    int getExtinctionDate();
    int getReintroductionDate();

    TechRating getTechRating();
    AvailabilityValue getBaseAvailability(Era era);

    default int getIntroductionDate(boolean clan) {
        return getIntroductionDate();
    }
    int getIntroductionDate(boolean clan, Faction faction);
    default int getPrototypeDate(boolean clan) {
        return getPrototypeDate();
    }
    int getPrototypeDate(boolean clan, Faction faction);
    default int getProductionDate(boolean clan) {
        return getProductionDate();
    }
    int getProductionDate(boolean clan, Faction faction);
    default int getCommonDate(boolean clan) {
        return getCommonDate();
    }
    int getExtinctionDate(boolean clan, Faction faction);
    default int getExtinctionDate(boolean clan) {
        return getExtinctionDate();
    }
    int getReintroductionDate(boolean clan, Faction faction);
    default int getReintroductionDate(boolean clan) {
        return getReintroductionDate();
    }

    static Era getTechEra(int year) {
        if (year < 2780) {
            return Era.SL;
        } else if (year < 3050) {
            return Era.SW;
        } else if (year < 3130) {
            return Era.CLAN;
        } else {
            return Era.DA;
        }
    }

    default int getTechLevel(int year, boolean clan) {
        return getSimpleLevel(year, clan).getCompoundTechLevel(clan);
    }

    default int getTechLevel(int year) {
        return getTechLevel(year, isClan());
    }

    default SimpleTechLevel getSimpleLevel(int year) {
        if (getSimpleLevel(year, true).compareTo(getSimpleLevel(year, false)) < 0) {
            return getSimpleLevel(year, true);
        }
        return getSimpleLevel(year, false);
    }

    default SimpleTechLevel getSimpleLevel(int year, boolean clan) {
        return getSimpleLevel(year, clan, Faction.NONE);
    }

    default SimpleTechLevel getSimpleLevel(int year, boolean clan, Faction faction) {
        if (isUnofficial()) {
            return SimpleTechLevel.UNOFFICIAL;
        } else if (year >= getCommonDate(clan) && getCommonDate(clan) != DATE_NONE) {
            if (isIntroLevel()) {
                return SimpleTechLevel.INTRO;
            } else {
                return SimpleTechLevel.STANDARD;
            }
        } else if (year >= getProductionDate(clan, faction) && getProductionDate(clan, faction) != DATE_NONE) {
            return SimpleTechLevel.ADVANCED;
        } else if (year >= getPrototypeDate(clan, faction) && getPrototypeDate(clan, faction) != DATE_NONE) {
            return SimpleTechLevel.EXPERIMENTAL;
        } else {
            return SimpleTechLevel.UNOFFICIAL;
        }
    }

    /**
     * For non-era-based usage, provide a single tech level that does not vary with date.
     *
     * @return The base rules level of the equipment or unit.
     */
    SimpleTechLevel getStaticTechLevel();

    default boolean isIntroLevel() {
        return getStaticTechLevel() == SimpleTechLevel.INTRO;
    }

    default boolean isUnofficial() {
        return getStaticTechLevel() == SimpleTechLevel.UNOFFICIAL;
    }


    /**
     * Finds the lowest rules level the equipment qualifies for, for either IS or Clan faction
     * using it.
     *
     * @param clan - whether tech level is being calculated for a Clan faction
     * @return - the lowest tech level available to the item
     */
    default SimpleTechLevel findMinimumRulesLevel(boolean clan) {
        if (getCommonDate(clan) != DATE_NONE) {
            return (getStaticTechLevel() == SimpleTechLevel.INTRO)
                    ? SimpleTechLevel.INTRO : SimpleTechLevel.STANDARD;
        }
        if (getProductionDate(clan) != DATE_NONE) {
            return SimpleTechLevel.ADVANCED;
        }
        if (getPrototypeDate(clan) != DATE_NONE) {
            return SimpleTechLevel.EXPERIMENTAL;
        }
        return SimpleTechLevel.UNOFFICIAL;
    }

    /**
     * Finds the lowest rules level the equipment qualifies for regardless of faction using it.
     *
     * @return - the lowest tech level available to the item
     */
    default SimpleTechLevel findMinimumRulesLevel() {
        if (getCommonDate() != DATE_NONE) {
            return (getStaticTechLevel() == SimpleTechLevel.INTRO)
                    ? SimpleTechLevel.INTRO : SimpleTechLevel.STANDARD;
        }
        if (getProductionDate() != DATE_NONE) {
            return SimpleTechLevel.ADVANCED;
        }
        if (getPrototypeDate() != DATE_NONE) {
            return SimpleTechLevel.EXPERIMENTAL;
        }
        return SimpleTechLevel.UNOFFICIAL;
    }

    default boolean isExtinct(int year, boolean clan) {
        return getExtinctionDate(clan) != DATE_NONE
                && getExtinctionDate(clan) < year
                && (getReintroductionDate(clan) == DATE_NONE
                || year < getReintroductionDate(clan));
    }

    default boolean isExtinct(int year, boolean clan, Faction faction) {
        // Tech that is lost but later recovered in the IS is not lost to ComStar.
        if ((Faction.CS == faction) && (getReintroductionDate(false) != DATE_NONE)) {
            return false;
        }
        return getExtinctionDate(clan) != DATE_NONE
                && getExtinctionDate(clan) < year
                && (getReintroductionDate(clan) == DATE_NONE
                || year < getReintroductionDate(clan, faction));
    }

    default boolean isExtinct(int year) {
        return getExtinctionDate() != DATE_NONE
                && getExtinctionDate() < year
                && (getReintroductionDate() == DATE_NONE
                || year < getReintroductionDate());
    }

    default boolean isAvailableIn(int year, boolean clan, boolean ignoreExtinction) {
        return year >= getIntroductionDate(clan) && (getIntroductionDate(clan) != DATE_NONE)
                && (ignoreExtinction || !isExtinct(year, clan));
    }

    default boolean isAvailableIn(int year, boolean ignoreExtinction) {
        return year >= getIntroductionDate() && (getIntroductionDate() != DATE_NONE)
                && (ignoreExtinction || !isExtinct(year));
    }

    default boolean isAvailableIn(int year, boolean clan, Faction faction) {
        return year >= getIntroductionDate(clan, faction)
                && getIntroductionDate(clan, faction) != DATE_NONE  && !isExtinct(year, clan, faction);
    }

    default boolean isLegal(int year, int techLevel, boolean mixedTech) {
        return isLegal(year, SimpleTechLevel.convertCompoundToSimple(techLevel),
                TechConstants.isClan(techLevel), mixedTech, false);
    }

    default boolean isLegal(int year, SimpleTechLevel simpleRulesLevel, boolean clanBase, boolean mixedTech, boolean ignoreExtinct) {
        if (mixedTech) {
            if (!isAvailableIn(year, ignoreExtinct)) {
                return false;
            } else {
                return getSimpleLevel(year).ordinal() <= simpleRulesLevel.ordinal();
            }
        } else {
            if (getTechBase() != TechBase.ALL
                    && clanBase != isClan()) {
                return false;
            }
            if (!isAvailableIn(year, clanBase, ignoreExtinct)) {
                return false;
            }
            return getSimpleLevel(year, clanBase).ordinal() <= simpleRulesLevel.ordinal();
        }
    }

    /**
     * Adjusts availability for certain combinations of era and IS/Clan use
     * @param era - one of the tech eras
     * @param clanUse - whether the faction trying to obtain the tech is IS or Clan
     * @return - the adjusted availability code
     */
    default AvailabilityValue calcEraAvailability(Era era, boolean clanUse) {
        if (clanUse) {
            if (!isClan()
                    && era.getIndex() < Era.CLAN.getIndex()
                    && getPrototypeDate(false) >= 2780) {
                return AvailabilityValue.X;
            } else {
                return getBaseAvailability(era);
            }
        } else {
            if (isClan()) {
                if (era.getIndex() < Era.CLAN.getIndex()) {
                    return AvailabilityValue.X;
                } else {
                    // For Clan items in IS eras, availability is one step harder, but not above X
                    AvailabilityValue base = getBaseAvailability(era);
                    int harder = Math.min(AvailabilityValue.X.getIndex(), base.getIndex() + 1);
                    return AvailabilityValue.fromIndex(harder);
                }
            } else {
                return getBaseAvailability(era);
            }
        }
    }

    default AvailabilityValue calcYearAvailability(int year, boolean clanUse) {
        return calcYearAvailability(year, clanUse, Faction.NONE);
    }

    default AvailabilityValue calcYearAvailability(int year, boolean clanUse, Faction faction) {
        Era era = getTechEra(year);
        if (!clanUse && !isClan() && (faction != Faction.CS) && (era == Era.SW)
            && getBaseAvailability(Era.SW).isBetterOrEqualThan(AvailabilityValue.E)
            && getExtinctionDate(false) != DATE_NONE
            && getExtinctionDate(false) <= year
            && (getReintroductionDate(false) == DATE_NONE || getReintroductionDate(false) > year)) {
            int harder = Math.min(getBaseAvailability(Era.SW).getIndex() + 1, AvailabilityValue.X.getIndex());
            return AvailabilityValue.fromIndex(harder);
        }
        return calcEraAvailability(era, clanUse);
    }

    /**
     * Adjusts base availability code for IS/Clan and IS extinction
     *
     * @param era - one of the Era enums
     * @param clanUse - whether this should be calculated for a Clan faction rather than IS
     * @return - The availability code for the faction in the era. The code for an IS faction
     *           during the SW era may be two values indicating availability before and after
     *           the extinction date.
     */
    default String getEraAvailabilityName(Era era, boolean clanUse) {
        if (!clanUse && !isClan() && (era == Era.SW)
            && getBaseAvailability(Era.SW).isBetterOrEqualThan(AvailabilityValue.E)
            && !getBaseAvailability(Era.SW).equals(AvailabilityValue.X)
            && (getExtinctionDate(false) != DATE_NONE)
            && getTechEra(getExtinctionDate(false)) == Era.SW) {
            AvailabilityValue base = getBaseAvailability(Era.SW);
            int harderIdx = Math.min(base.getIndex() + 1, AvailabilityValue.X.getIndex());
            return base.getName() + "(" + AvailabilityValue.fromIndex(harderIdx).getName() + ")";
        }
        return calcEraAvailability(era, clanUse).getName();
    }

    default String getTechRatingName() {
        return getRatingName(getTechRating());
    }

    default String getEraAvailabilityName(Era era) {
        return getEraAvailabilityName(era, isClan());
    }

    default String getFullRatingName(boolean clanUse) {
        String rating = getRatingName(getTechRating());
        rating += "/";
        rating += getEraAvailabilityName(Era.SL, clanUse);
        rating += "-";
        rating += getEraAvailabilityName(Era.SW, clanUse);
        rating += "-";
        rating += getEraAvailabilityName(Era.CLAN, clanUse);
        rating += "-";
        rating += getEraAvailabilityName(Era.DA, clanUse);
        return rating;
    }

    default String getFullRatingName() {
        return getFullRatingName(isClan());
    }

    default AvailabilityValue calcEraAvailability(Era era) {
        return calcEraAvailability(era, isClan());
    }

    default AvailabilityValue calcYearAvailability(int year) {
        return calcYearAvailability(year, isClan());
    }

    /**
     * @deprecated Use {@link TechRating#getName()} instead.
     * @param rating
     * @return
     */
    @Deprecated
    static String getRatingName(TechRating rating) {
        return rating.getName();
    }

    static String getDateRange(int startIncl, int endNonIncl) {
        if (startIncl == DATE_NONE) {
            return "-";
        }
        StringBuilder sb = new StringBuilder();
        sb.append(startIncl);
        if (endNonIncl == DATE_NONE) {
            sb.append("+");
        } else if (endNonIncl > startIncl + 1) {
            sb.append("-").append(endNonIncl - 1);
        }
        return sb.toString();
    }

    static Faction getFactionFromIOAbbr(String abbr) {
        return Faction.fromIOAbbr(abbr);
    }

    static Faction getFactionFromMMAbbr(String abbr) {
        return Faction.fromMMAbbr(abbr);
    }

    default String getExperimentalRange(boolean clan) {
        return getDateRange(getPrototypeDate(clan), getProductionDate(clan));
    }
    default String getAdvancedRange(boolean clan) {
        return getDateRange(getProductionDate(clan), getCommonDate(clan));
    }
    default String getStandardRange(boolean clan) {
        return getDateRange(getCommonDate(clan), DATE_NONE);
    }
    default String getExtinctionRange(boolean clan) {
        return getDateRange(getExtinctionDate(clan), getReintroductionDate(clan));
    }

    default String getExperimentalRange() {
        return getDateRange(getPrototypeDate(), getProductionDate());
    }
    default String getAdvancedRange() {
        return getDateRange(getProductionDate(), getCommonDate());
    }
    default String getStandardRange() {
        return getDateRange(getCommonDate(), DATE_NONE);
    }
    default String getExtinctionRange() {
        return getDateRange(getExtinctionDate(), getReintroductionDate());
    }

    /**
     * @deprecated Use getFactionFromMMAbbr instead.
     */
    @Deprecated
    default int getCodeFromMMAbbr(String abbr) {
        return getFactionFromMMAbbr(abbr).getIndex();
    }
    /**
     * @deprecated Use getFactionFromIOAbbr instead.
     */
    @Deprecated
    default int getCodeFromIOAbbr(String abbr) {
        return getFactionFromIOAbbr(abbr).getIndex();
    }
    
}

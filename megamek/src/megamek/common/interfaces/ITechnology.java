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

package megamek.common.interfaces;

import megamek.common.SimpleTechLevel;
import megamek.common.TechConstants;
import megamek.common.enums.AvailabilityValue;
import megamek.common.enums.Era;
import megamek.common.enums.Faction;
import megamek.common.enums.TechBase;
import megamek.common.enums.TechRating;

/**
 * Implemented by any class that is subject to tech advancement (entities, equipment, systems, etc.)
 *
 * @author Neoancient
 */
public interface ITechnology {


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
     * Finds the lowest rules level the equipment qualifies for, for either IS or Clan faction using it.
     *
     * @param clan - whether tech level is being calculated for a Clan faction
     *
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
              && getIntroductionDate(clan, faction) != DATE_NONE && !isExtinct(year, clan, faction);
    }

    default boolean isLegal(int year, int techLevel, boolean mixedTech) {
        return isLegal(year, SimpleTechLevel.convertCompoundToSimple(techLevel),
              TechConstants.isClan(techLevel), mixedTech, false);
    }

    default boolean isLegal(int year, SimpleTechLevel simpleRulesLevel, boolean clanBase, boolean mixedTech,
          boolean ignoreExtinct) {
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
     *
     * @param era     - one of the tech eras
     * @param clanUse - whether the faction trying to obtain the tech is IS or Clan
     *
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
     * @param era     - one of the Era enums
     * @param clanUse - whether this should be calculated for a Clan faction rather than IS
     *
     * @return - The availability code for the faction in the era. The code for an IS faction during the SW era may be
     *       two values indicating availability before and after the extinction date.
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
     * @param rating the TechRating
     *
     * @return the name of the TechRating
     *
     * @deprecated Use {@link TechRating#getName()} instead.
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

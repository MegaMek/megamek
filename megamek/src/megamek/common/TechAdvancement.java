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

import java.util.Collections;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.StringJoiner;

import megamek.common.enums.AvailabilityValue;
import megamek.common.enums.Era;
import megamek.common.enums.Faction;
import megamek.common.enums.FactionAffiliation;
import megamek.common.enums.TechBase;
import megamek.common.enums.TechRating;
import megamek.common.interfaces.ITechnology;

/**
 * Handles the progression of technology through prototype, production, extinction and reintroduction phases. Calculates
 * current rules level for IS or Clan.
 *
 * @author Neoancient
 */
public class TechAdvancement implements ITechnology {

    public enum AdvancementPhase {
        PROTOTYPE(0),
        PRODUCTION(1),
        COMMON(2),
        EXTINCT(3),
        REINTRODUCED(4);

        private final int index;
        private static final Map<Integer, AdvancementPhase> INDEX_LOOKUP = new HashMap<>();

        static {
            for (AdvancementPhase phase : values()) {
                INDEX_LOOKUP.put(phase.index, phase);
            }
        }

        AdvancementPhase(int index) {
            this.index = index;
        }

        public int getIndex() {
            return index;
        }

        public static AdvancementPhase fromIndex(int idx) {
            AdvancementPhase phase = INDEX_LOOKUP.get(idx);
            if (phase == null) {
                throw new IllegalArgumentException("Invalid AdvancementPhase index: " + idx);
            }
            return phase;
        }
    }

    //Dates that are approximate can be pushed this many years earlier (or later for extinctions).
    public static final int APPROXIMATE_MARGIN = 5;
    // Per IO p. 34, tech with only a prototype date becomes available to
    // other factions after 3d6+5 years if it hasn't gone extinct by then.
    // Using the minimum value here.
    private static final int PROTOTYPE_DATE_OFFSET_FOR_OTHER_FACTIONS = 8;
    // Per IO p. 34, tech with no common date becomes available to other factions after 10 years if not extinct
    private static final int PRODUCTION_DATE_OFFSET_FOR_OTHER_FACTIONS = 10;
    private static final int REINTRODUCTION_DATE_OFFSET = 10;

    private TechBase techBase = TechBase.ALL;
    private EnumMap<AdvancementPhase, Integer> isAdvancement = new EnumMap<>(AdvancementPhase.class);
    private EnumMap<AdvancementPhase, Integer> clanAdvancement = new EnumMap<>(AdvancementPhase.class);
    private EnumMap<AdvancementPhase, Boolean> isApproximate = new EnumMap<>(AdvancementPhase.class);
    private EnumMap<AdvancementPhase, Boolean> clanApproximate = new EnumMap<>(AdvancementPhase.class);
    private Set<Faction> prototypeFactions = EnumSet.noneOf(Faction.class);
    private Set<Faction> productionFactions = EnumSet.noneOf(Faction.class);
    private Set<Faction> extinctionFactions = EnumSet.noneOf(Faction.class);
    private Set<Faction> reintroductionFactions = EnumSet.noneOf(Faction.class);
    private SimpleTechLevel staticTechLevel = SimpleTechLevel.STANDARD;
    private TechRating techRating = TechRating.C;
    private EnumMap<Era, AvailabilityValue> availability = new EnumMap<>(Era.class);

    public TechAdvancement() {
        for (AdvancementPhase phase : AdvancementPhase.values()) {
            isAdvancement.put(phase, DATE_NONE);
            clanAdvancement.put(phase, DATE_NONE);
            isApproximate.put(phase, false);
            clanApproximate.put(phase, false);
        }
        for (Era era : Era.values()) {
            availability.put(era, AvailabilityValue.A);
        }
    }

    public TechAdvancement(TechBase techBase) {
        this();
        this.techBase = techBase;
    }

    /**
     * Copy constructor
     */
    public TechAdvancement(TechAdvancement ta) {
        this(ta.techBase);
        this.isAdvancement = new EnumMap<>(ta.isAdvancement);
        this.clanAdvancement = new EnumMap<>(ta.clanAdvancement);
        this.isApproximate = new EnumMap<>(ta.isApproximate);
        this.clanApproximate = new EnumMap<>(ta.clanApproximate);
        prototypeFactions = EnumSet.copyOf(ta.prototypeFactions);
        productionFactions = EnumSet.copyOf(ta.productionFactions);
        extinctionFactions = EnumSet.copyOf(ta.extinctionFactions);
        reintroductionFactions = EnumSet.copyOf(ta.reintroductionFactions);
        this.staticTechLevel = ta.staticTechLevel;
        this.techRating = ta.techRating;
        availability = new EnumMap<>(ta.availability);
    }

    public TechAdvancement setTechBase(TechBase base) {
        techBase = base;
        return this;
    }

    @Override
    public TechBase getTechBase() {
        return techBase;
    }

    /**
     * Provide years for prototype, production, common, extinction, and reintroduction for IS factions.
     *
     * @param prog A map of tech progression years.
     *
     * @return a reference to this object
     */

    public TechAdvancement setISAdvancement(Map<AdvancementPhase, Integer> prog) {
        for (AdvancementPhase phase : AdvancementPhase.values()) {
            isAdvancement.put(phase, prog.getOrDefault(phase, DATE_NONE));
        }
        return this;
    }

    /**
     * Provide years for prototype, production, common, extinction, and reintroduction for IS factions.
     *
     * @param prog Up to five tech progression years. Missing levels should be marked by DATE_NONE.
     *
     * @return a reference to this object
     */
    public TechAdvancement setISAdvancement(int... prog) {
        int i = 0;
        for (AdvancementPhase phase : AdvancementPhase.values()) {
            if (i < prog.length) {
                isAdvancement.put(phase, prog[i]);
            } else {
                isAdvancement.put(phase, DATE_NONE);
            }
            i++;
        }
        return this;
    }

    public TechAdvancement setISAdvancement(AdvancementPhase phase, int prog) {
        isAdvancement.put(phase, prog);
        return this;
    }

    public TechAdvancement setClanAdvancement(AdvancementPhase phase, int prog) {
        clanAdvancement.put(phase, prog);
        return this;
    }


    public TechAdvancement setISApproximate(AdvancementPhase phase, boolean approximate) {
        isApproximate.put(phase, approximate);
        return this;
    }


    public TechAdvancement setClanApproximate(AdvancementPhase phase, boolean approximate) {
        clanApproximate.put(phase, approximate);
        return this;
    }

    /**
     * Indicate whether the years for prototype, production, common, extinction, and reintroduction for IS factions
     * should be considered approximate.
     *
     * @param approx A map of tech progression years.
     *
     * @return a reference to this object
     */

    public TechAdvancement setISApproximate(Map<AdvancementPhase, Boolean> approx) {
        for (AdvancementPhase phase : AdvancementPhase.values()) {
            isApproximate.put(phase, approx.getOrDefault(phase, false));
        }
        return this;
    }

    /**
     * Indicate whether the years for prototype, production, common, extinction, and reintroduction for IS factions
     * should be considered approximate.
     *
     * @param approx Up to five tech progression years.
     *
     * @return a reference to this object
     */
    public TechAdvancement setISApproximate(boolean... approx) {
        int i = 0;
        for (AdvancementPhase phase : AdvancementPhase.values()) {
            if (i < approx.length) {
                isApproximate.put(phase, approx[i]);
            } else {
                isApproximate.put(phase, false);
            }
            i++;
        }
        return this;
    }

    /**
     * Indicate whether the years for prototype, production, common, extinction, and reintroduction for IS factions
     * should be considered approximate.
     *
     * @param phases the phases to set as approximate
     *
     * @return a reference to this object
     */
    public TechAdvancement setISApproximate(AdvancementPhase... phases) {
        for (AdvancementPhase phase : AdvancementPhase.values()) {
            isApproximate.put(phase, false);
        }
        for (AdvancementPhase phase : phases) {
            isApproximate.put(phase, true);
        }
        return this;
    }

    /**
     * Provide years for prototype, production, common, extinction, and reintroduction for Clan factions.
     *
     * @param prog A map of tech progression years.
     *
     * @return a reference to this object
     */

    public TechAdvancement setClanAdvancement(Map<AdvancementPhase, Integer> prog) {
        for (AdvancementPhase phase : AdvancementPhase.values()) {
            clanAdvancement.put(phase, prog.getOrDefault(phase, DATE_NONE));
        }
        return this;
    }

    /**
     * Provide years for prototype, production, common, extinction, and reintroduction for Clan factions.
     *
     * @param prog Up to five tech progression years. Missing levels should be marked by DATE_NONE.
     *
     * @return a reference to this object
     */
    public TechAdvancement setClanAdvancement(int... prog) {
        int i = 0;
        for (AdvancementPhase phase : AdvancementPhase.values()) {
            if (i < prog.length) {
                clanAdvancement.put(phase, prog[i]);
            } else {
                clanAdvancement.put(phase, DATE_NONE);
            }
            i++;
        }
        return this;
    }


    /**
     * Indicate whether the years for prototype, production, common, extinction, and reintroduction for Clan factions
     * should be considered approximate.
     *
     * @param approx A map of tech progression years.
     *
     * @return a reference to this object
     */

    public TechAdvancement setClanApproximate(Map<AdvancementPhase, Boolean> approx) {
        for (AdvancementPhase phase : AdvancementPhase.values()) {
            clanApproximate.put(phase, approx.getOrDefault(phase, false));
        }
        return this;
    }

    /**
     * Indicate whether the years for prototype, production, common, extinction, and reintroduction for Clan factions
     * should be considered approximate.
     *
     * @param approx Up to five tech progression years.
     *
     * @return a reference to this object
     */
    public TechAdvancement setClanApproximate(boolean... approx) {
        int i = 0;
        for (AdvancementPhase phase : AdvancementPhase.values()) {
            if (i < approx.length) {
                clanApproximate.put(phase, approx[i]);
            } else {
                clanApproximate.put(phase, false);
            }
            i++;
        }
        return this;
    }

    /**
     * Indicate whether the years for prototype, production, common, extinction, and reintroduction for Clan factions
     * should be considered approximate.
     *
     * @param phases the phases to set as approximate
     *
     * @return a reference to this object
     */
    public TechAdvancement setClanApproximate(AdvancementPhase... phases) {
        for (AdvancementPhase phase : AdvancementPhase.values()) {
            clanApproximate.put(phase, false);
        }
        for (AdvancementPhase phase : phases) {
            clanApproximate.put(phase, true);
        }
        return this;
    }

    /**
     * A convenience method that will set identical values for IS and Clan factions. Prototype, Production, Common,
     * Extinct, Reintroduced
     *
     * @param prog A map of tech progression years.
     *
     */
    public TechAdvancement setAdvancement(Map<AdvancementPhase, Integer> prog) {
        setISAdvancement(prog);
        setClanAdvancement(prog);
        return this;
    }

    /**
     * A convenience method that will set identical values for IS and Clan factions. Prototype, Production, Common,
     * Extinct, Reintroduced
     *
     */
    public TechAdvancement setAdvancement(int... prog) {
        setISAdvancement(prog);
        setClanAdvancement(prog);
        return this;
    }

    public Integer getISAdvancement(AdvancementPhase phase) {
        return isAdvancement.get(phase);
    }

    public Integer getClanAdvancement(AdvancementPhase phase) {
        return clanAdvancement.get(phase);
    }

    public boolean getISApproximate(AdvancementPhase phase) {
        return isApproximate.get(phase);
    }

    public boolean getClanApproximate(AdvancementPhase phase) {
        return clanApproximate.get(phase);
    }

    /**
     * A convenience method that will set identical values for IS and Clan factions.
     *
     * @param approx A map of tech progression years.
     *
     */
    public TechAdvancement setApproximate(Map<AdvancementPhase, Boolean> approx) {
        setISApproximate(approx);
        setClanApproximate(approx);
        return this;
    }

    /**
     * A convenience method that will set identical values for IS and Clan factions.
     *
     */
    public TechAdvancement setApproximate(boolean... approx) {
        setISApproximate(approx);
        setClanApproximate(approx);
        return this;
    }

    /**
     * A convenience method that will set identical values for IS and Clan factions.
     *
     * @param phases the phases to set as approximate
     *
     */
    public TechAdvancement setApproximate(AdvancementPhase... phases) {
        setISApproximate(phases);
        setClanApproximate(phases);
        return this;
    }

    private TechAdvancement setFactionsAdvancement(Set<Faction> factionAdvancement, Faction... factions) {
        factionAdvancement = EnumSet.noneOf(Faction.class);
        Collections.addAll(factionAdvancement, factions);
        return this;
    }

    /**
     * Sets which factions developed a prototype.
     *
     * @param factions A list of Faction enums
     *
     */
    public TechAdvancement setPrototypeFactions(Faction... factions) {
        return setFactionsAdvancement(prototypeFactions, factions);
    }

    /**
     * @return A set of Faction enums that indicate which factions started prototype development.
     */
    public Set<Faction> getPrototypeFactions() {
        return prototypeFactions;
    }

    /**
     * Sets which factions started production before the technology was commonly available.
     *
     * @param factions A list of Faction enums
     *
     * @return A reference to this object.
     */
    public TechAdvancement setProductionFactions(Faction... factions) {
        return setFactionsAdvancement(productionFactions, factions);
    }

    /**
     * @return A set of Faction enums that indicate which factions started production before the technology was commonly
     *       available.
     */
    public Set<Faction> getProductionFactions() {
        return productionFactions;
    }

    /**
     * Sets the factions for which the technology became extinct.
     *
     * @param factions A list of Faction enums
     *
     * @return A reference to this object.
     */
    public TechAdvancement setExtinctionFactions(Faction... factions) {
        return setFactionsAdvancement(extinctionFactions, factions);
    }

    /**
     * @return A set of Faction enums that indicate the factions for which the technology became extinct.
     */
    public Set<Faction> getExtinctionFactions() {
        return extinctionFactions;
    }

    /**
     * Sets the factions which reintroduced technology that had been extinct.
     *
     * @param factions A list of Faction enums
     *
     * @return A reference to this object.
     */
    public TechAdvancement setReintroductionFactions(Faction... factions) {
        return setFactionsAdvancement(reintroductionFactions, factions);
    }

    /**
     * @return A set of Faction enums that indicate the factions that reintroduced extinct technology. became extinct.
     */
    public Set<Faction> getReintroductionFactions() {
        return reintroductionFactions;
    }

    /**
     * The prototype date for either Clan or IS factions. If the date is flagged as approximate, the date returned will
     * be earlier by the value of APPROXIMATE_MARGIN.
     */
    @Override
    public int getPrototypeDate(boolean clan) {
        return getDate(AdvancementPhase.PROTOTYPE, clan);
    }

    /**
     * The prototype date for a particular faction. If there are prototype factions and the given faction is not among
     * them, the prototype date is DATE_NONE.
     *
     * @param clan    Whether to use Clan or IS progression dates
     * @param faction The index of the faction (F_* constant). If &lt; 0, the prototype factions are ignored.
     */
    @Override
    public int getPrototypeDate(boolean clan, Faction faction) {
        int protoDate = getDate(AdvancementPhase.PROTOTYPE, clan);
        if (protoDate == DATE_NONE) {
            return DATE_NONE;
        }
        if (!prototypeFactions.isEmpty() && faction != null && faction != Faction.NONE) {
            if (prototypeFactions.contains(faction)
                  || (prototypeFactions.contains(Faction.IS) && !clan)
                  || (prototypeFactions.contains(Faction.CLAN) && clan)) {
                return protoDate;
            }
            // Per IO p. 34, tech with only a prototype date becomes available to
            // other factions after 3d6+5 years if it hasn't gone extinct by then.
            // Using the minimum value here.
            final int date = protoDate + PROTOTYPE_DATE_OFFSET_FOR_OTHER_FACTIONS;
            int prodDate = getDate(AdvancementPhase.PRODUCTION, clan);
            int commonDate = getDate(AdvancementPhase.COMMON, clan);
            if ((prodDate != DATE_NONE && prodDate < date)
                  || (commonDate != DATE_NONE && commonDate < date)
                  || isExtinct(date, clan)) {
                return DATE_NONE;
            }
            return date;
        }
        return getDate(AdvancementPhase.PROTOTYPE, clan);
    }

    /**
     * The production date for either Clan or IS factions. If the date is flagged as approximate, the date returned will
     * be earlier by the value of APPROXIMATE_MARGIN.
     */
    @Override
    public int getProductionDate(boolean clan) {
        return getDate(AdvancementPhase.PRODUCTION, clan);
    }

    /**
     * The production date for a particular faction. If there are production factions and the given faction is not among
     * them, the production date is DATE_NONE.
     *
     * @param clan    Whether to use Clan or IS progression dates
     * @param faction The index of the faction (F_* constant). If &lt; 0, the production factions are ignored.
     */
    @Override
    public int getProductionDate(boolean clan, Faction faction) {
        int prodDate = getDate(AdvancementPhase.PRODUCTION, clan);
        if (prodDate == DATE_NONE) {
            return DATE_NONE;
        }
        if (!productionFactions.isEmpty() && faction != null && faction != Faction.NONE) {
            if (productionFactions.contains(faction)
                  || (productionFactions.contains(Faction.IS) && !clan)
                  || (productionFactions.contains(Faction.CLAN) && clan)) {
                return prodDate;
            }
            // Per IO p. 34, tech with no common date becomes available to other factions after 10 years if not extinct
            int date = prodDate + PRODUCTION_DATE_OFFSET_FOR_OTHER_FACTIONS;
            int commonDate = getDate(AdvancementPhase.COMMON, clan);
            if ((commonDate != DATE_NONE && commonDate <= date) || isExtinct(date, clan)) {
                return DATE_NONE;
            }
            return date;
        }
        return getDate(AdvancementPhase.PRODUCTION, clan);
    }

    /**
     * The common date for either Clan or IS factions. If the date is flagged as approximate, the date returned will be
     * earlier by the value of APPROXIMATE_MARGIN.
     */
    @Override
    public int getCommonDate(boolean clan) {
        return getDate(AdvancementPhase.COMMON, clan);
    }

    /**
     * The extinction date for either Clan or IS factions. If the date is flagged as approximate, the date returned will
     * be later by the value of APPROXIMATE_MARGIN.
     */
    @Override
    public int getExtinctionDate(boolean clan) {
        return getDate(AdvancementPhase.EXTINCT, clan);
    }

    /**
     * The extinction date for a particular faction. If there are extinction factions and the given faction is not among
     * them, the extinction date is DATE_NONE.
     *
     * @param clan    Whether to use Clan or IS progression dates
     * @param faction The index of the faction (F_* constant). If &lt; 0, the extinction factions are ignored.
     */
    @Override
    public int getExtinctionDate(boolean clan, Faction faction) {
        int extinctionDate = getDate(AdvancementPhase.EXTINCT, clan);
        if (extinctionDate == DATE_NONE) {
            return DATE_NONE;
        }
        if (!extinctionFactions.isEmpty() && faction != null && faction != Faction.NONE) {
            if (extinctionFactions.contains(faction)
                  || (extinctionFactions.contains(Faction.IS) && !clan)
                  || (extinctionFactions.contains(Faction.CLAN) && clan)) {
                return extinctionDate;
            }
            return DATE_NONE;
        }
        return getDate(AdvancementPhase.EXTINCT, clan);
    }

    /**
     * The reintroduction date for either Clan or IS factions. If the date is flagged as approximate, the date returned
     * will be earlier by the value of APPROXIMATE_MARGIN.
     */
    @Override
    public int getReintroductionDate(boolean clan) {
        return getDate(AdvancementPhase.REINTRODUCED, clan);
    }

    /**
     * The reintroduction date for a particular faction. If there are reintroduction factions and the given faction is
     * not among them, the reintroduction date is DATE_NONE.
     *
     * @param clan    Whether to use Clan or IS progression dates
     * @param faction The Faction. If null the reintroduction factions are ignored.
     */
    @Override
    public int getReintroductionDate(boolean clan, Faction faction) {
        int reIntroDate = getDate(AdvancementPhase.REINTRODUCED, clan);
        if (reIntroDate == DATE_NONE) {
            return DATE_NONE;
        }
        if (!reintroductionFactions.isEmpty() && faction != null && faction != Faction.NONE) {
            if (reintroductionFactions.contains(faction)
                  || (reintroductionFactions.contains(Faction.IS) && !clan)
                  || (reintroductionFactions.contains(Faction.CLAN) && clan)) {
                return reIntroDate;
            }
            // If the production or common date is later than the reintroduction date, that is
            // when it becomes available to other factions. Otherwise, we use reintro + 10 as with
            // production date.
            final int prodDate = getProductionDate(clan, faction);
            final int commonDate = getDate(AdvancementPhase.COMMON, clan);
            if (prodDate > reIntroDate) {
                return prodDate;
            } else if (commonDate > reIntroDate) {
                return commonDate;
            } else {
                return reIntroDate + REINTRODUCTION_DATE_OFFSET;
            }
        }
        return getDate(AdvancementPhase.REINTRODUCED, clan);
    }

    /**
     * The year the technology first became available for Clan or IS factions, regardless of production level, or
     * APPROXIMATE_MARGIN years earlier if marked as approximate.
     */
    @Override
    public int getIntroductionDate(boolean clan) {
        if (getPrototypeDate(clan) > 0) {
            return getPrototypeDate(clan);
        }
        if (getProductionDate(clan) > 0) {
            return getProductionDate(clan);
        }
        return getCommonDate(clan);
    }

    /**
     * The year the technology first became available for the given faction, regardless of production level, or
     * APPROXIMATE_MARGIN years earlier if marked as approximate.
     */
    @Override
    public int getIntroductionDate(boolean clan, Faction faction) {
        int date = getReintroductionDate(clan, faction);
        if (getPrototypeDate(clan, faction) > 0) {
            return earliestDate(date, getPrototypeDate(clan, faction));
        }
        if (getProductionDate(clan, faction) > 0) {
            return earliestDate(date, getProductionDate(clan, faction));
        }
        return earliestDate(date, getCommonDate(clan));
    }

    /**
     * Convenience method for calculating approximations.
     */
    private int getDate(AdvancementPhase phase, boolean clan) {
        Integer date;
        Boolean approx;
        if (clan) {
            date = clanAdvancement.get(phase);
            approx = clanApproximate.get(phase);
        } else {
            date = isAdvancement.get(phase);
            approx = isApproximate.get(phase);
        }
        if (Boolean.TRUE.equals(approx) && date != null && date > 0) {
            return date + ((phase == AdvancementPhase.EXTINCT) ? APPROXIMATE_MARGIN : -APPROXIMATE_MARGIN);
        } else {
            return date != null ? date : DATE_NONE;
        }
    }

    /*
     * Methods which return universe-wide dates
     */

    @Override
    public int getPrototypeDate() {
        return earliestDate(getDate(AdvancementPhase.PROTOTYPE, false), getDate(AdvancementPhase.PROTOTYPE, true));
    }

    @Override
    public int getProductionDate() {
        return earliestDate(getDate(AdvancementPhase.PRODUCTION, false), getDate(AdvancementPhase.PRODUCTION, true));
    }

    @Override
    public int getCommonDate() {
        return earliestDate(getDate(AdvancementPhase.COMMON, false), getDate(AdvancementPhase.COMMON, true));
    }

    /**
     * If the tech base is IS or Clan, returns the extinction date that matches the tech base. Otherwise, returns the
     * latter of the IS and Clan dates, or DATE_NONE if the tech has not gone extinct for both.
     *
     * @return Universe-wide extinction date.
     */
    @Override
    public int getExtinctionDate() {
        if (getTechBase() != TechBase.ALL) {
            return getDate(AdvancementPhase.EXTINCT, getTechBase() == TechBase.CLAN);
        }
        if (isAdvancement.get(AdvancementPhase.EXTINCT) == DATE_NONE
              || clanAdvancement.get(AdvancementPhase.EXTINCT) == DATE_NONE) {
            return DATE_NONE;
        }
        return Math.max(getDate(AdvancementPhase.EXTINCT, false), getDate(AdvancementPhase.EXTINCT, true));
    }

    @Override
    public int getReintroductionDate() {
        if (getTechBase() != TechBase.ALL) {
            return getDate(AdvancementPhase.REINTRODUCED, getTechBase() == TechBase.CLAN);
        }
        return earliestDate(getDate(AdvancementPhase.REINTRODUCED, false),
              getDate(AdvancementPhase.REINTRODUCED, true));
    }

    @Override
    public int getIntroductionDate() {
        if (getPrototypeDate() > 0) {
            return getPrototypeDate();
        }
        if (getProductionDate() > 0) {
            return getProductionDate();
        }
        return getCommonDate();
    }

    /**
     * Formats the date at an index for display in a table, showing DATE_NONE as "-" and prepending "~" to approximate
     * dates.
     *
     * @param phase    PROTOTYPE, PRODUCTION, COMMON, EXTINCT, or REINTRODUCED
     * @param clan     Use the Clan progression
     * @param factions A list of factions to include in parentheses after the date.
     *
     */
    private String formatDate(AdvancementPhase phase, boolean clan, Set<Faction> factions) {
        Integer date = clan ? clanAdvancement.get(phase) : isAdvancement.get(phase);
        if (date == null || date == DATE_NONE) {
            return "-";
        }
        Boolean approx = clan ? clanApproximate.get(phase) : isApproximate.get(phase);
        StringBuilder sb = new StringBuilder();
        if (Boolean.TRUE.equals(approx)) {
            sb.append("~");
        }
        if (date == DATE_PS) {
            sb.append("PS");
        } else if (date == DATE_ES) {
            sb.append("ES");
        } else {
            sb.append(date);
        }
        if (factions != null && !factions.isEmpty()) {
            StringJoiner sj = new StringJoiner(",");
            for (Faction f : factions) {
                if ((clan && f.getAffiliation().equals(FactionAffiliation.CLAN))
                      || (!clan && !f.getAffiliation().equals(FactionAffiliation.CLAN))) {
                    sj.add(f.getCodeIO());
                }
            }
            if (sj.length() > 0) {
                sb.append("(").append(sj).append(")");
            }
        }
        return sb.toString();
    }

    /**
     * Formats introduction date indicating approximate when appropriate, and prototype faction if any for either IS or
     * Clan use tech base.
     */
    public String getIntroductionDateName() {
        if (getPrototypeDate() > 0) {
            return getPrototypeDateName();
        }
        if (getProductionDate() > 0) {
            return getProductionDateName();
        }
        return getCommonDateName();
    }

    /**
     * Formats prototype date indicating approximate when appropriate, and prototype faction if any for either IS or
     * Clan use tech base.
     */
    public String getPrototypeDateName(boolean clan) {
        return formatDate(AdvancementPhase.PROTOTYPE, clan, prototypeFactions);
    }

    private boolean useClanDate(AdvancementPhase phase) {
        Integer isDate = isAdvancement.get(phase);
        Integer clanDate = clanAdvancement.get(phase);
        return (isDate == null || isDate == DATE_NONE)
              || (clanDate != null && clanDate != DATE_NONE && clanDate < isDate);
    }

    /**
     * Formats earliest of Clan or IS prototype date indicating approximate when appropriate, and prototype faction if
     * any for mixed tech.
     */
    public String getPrototypeDateName() {
        return formatDate(AdvancementPhase.PROTOTYPE, useClanDate(AdvancementPhase.PROTOTYPE), prototypeFactions);
    }

    /**
     * Formats production date indicating approximate when appropriate, and production faction if any for either IS or
     * Clan use tech base.
     */
    public String getProductionDateName(boolean clan) {
        return formatDate(AdvancementPhase.PRODUCTION, clan, productionFactions);
    }

    /**
     * Formats earliest of Clan or IS production date indicating approximate when appropriate, and production faction if
     * any for mixed tech.
     */
    public String getProductionDateName() {
        return formatDate(AdvancementPhase.PRODUCTION, useClanDate(AdvancementPhase.PRODUCTION), productionFactions);
    }

    /**
     * Formats common date indicating approximate when appropriate.
     */
    public String getCommonDateName(boolean clan) {
        return formatDate(AdvancementPhase.COMMON, clan, null);
    }

    /**
     * Formats earliest of Clan or IS common date indicating approximate when appropriate for mixed tech.
     */
    public String getCommonDateName() {
        return formatDate(AdvancementPhase.COMMON, useClanDate(AdvancementPhase.COMMON), null);
    }

    /**
     * Formats extinction date indicating approximate when appropriate, and extinction faction if any for either IS or
     * Clan use tech base.
     */
    public String getExtinctionDateName(boolean clan) {
        return formatDate(AdvancementPhase.EXTINCT, clan, extinctionFactions);
    }

    /**
     * Formats latest of Clan or IS extinction date indicating approximate when appropriate, and extinction faction if
     * any for mixed tech.
     */
    public String getExtinctionDateName() {
        Integer isDate = isAdvancement.get(AdvancementPhase.EXTINCT);
        Integer clanDate = clanAdvancement.get(AdvancementPhase.EXTINCT);
        if (techBase == TechBase.ALL) {
            if (isDate == null || isDate == DATE_NONE) {
                // If there is no IS date, choose the Clan date
                return getExtinctionDateName(true);
            } else if (clanDate == null || clanDate == DATE_NONE) {
                // If there is no Clan date, choose the IS date
                return getExtinctionDateName(false);
            } else {
                // Pick the latter of the two dates for extinction
                boolean useClan = clanDate > isDate;
                return formatDate(AdvancementPhase.EXTINCT, useClan, extinctionFactions);
            }
        } else {
            return getExtinctionDateName(techBase == TechBase.CLAN);
        }
    }

    /**
     * Formats reintroduction date indicating approximate when appropriate, and reintroduction faction if any for either
     * IS or Clan use tech base.
     */
    public String getReintroductionDateName(boolean clan) {
        return formatDate(AdvancementPhase.REINTRODUCED, clan, reintroductionFactions);
    }

    /**
     * Formats earliest of Clan or IS reintroduction date indicating approximate when appropriate, and reintroduction
     * faction if any for mixed tech.
     */
    public String getReintroductionDateName() {
        return formatDate(AdvancementPhase.REINTRODUCED,
              useClanDate(AdvancementPhase.REINTRODUCED),
              reintroductionFactions);
    }

    /**
     * Finds the earliest of two dates, ignoring DATE_NA unless both values are set to DATE_NA
     */
    public static int earliestDate(int d1, int d2) {
        if (d1 < 0) {
            return d2;
        }
        if (d2 < 0) {
            return d1;
        }
        return Math.min(d1, d2);
    }

    public TechAdvancement setIntroLevel(boolean intro) {
        if (intro) {
            staticTechLevel = SimpleTechLevel.INTRO;
        } else if (staticTechLevel == SimpleTechLevel.INTRO) {
            staticTechLevel = SimpleTechLevel.STANDARD;
        }
        return this;
    }

    public TechAdvancement setUnofficial(boolean unofficial) {
        if (unofficial) {
            staticTechLevel = SimpleTechLevel.UNOFFICIAL;
        } else if (staticTechLevel == SimpleTechLevel.UNOFFICIAL) {
            staticTechLevel = null;
        }
        return this;
    }

    @Override
    public SimpleTechLevel getStaticTechLevel() {
        return staticTechLevel;
    }

    /**
     * Set RULES LEVEL
     *
     * @param level can be any of {@link SimpleTechLevel}
     *
     * @return itself
     */
    public TechAdvancement setStaticTechLevel(SimpleTechLevel level) {
        staticTechLevel = level;
        return this;
    }

    public SimpleTechLevel guessStaticTechLevel(String rulesRefs) {
        if (rulesRefs.contains("TW") || rulesRefs.contains("TM")) {
            return SimpleTechLevel.STANDARD;
        } else if (getProductionDate() != DATE_NONE) {
            return SimpleTechLevel.ADVANCED;
        } else {
            return SimpleTechLevel.EXPERIMENTAL;
        }
    }

    public TechAdvancement setTechRating(TechRating rating) {
        techRating = rating;
        return this;
    }

    @Override
    public TechRating getTechRating() {
        return techRating;
    }

    public TechAdvancement setAvailability(AvailabilityValue sl, AvailabilityValue sw,
          AvailabilityValue clan, AvailabilityValue da) {
        if (sl == null || sw == null || clan == null || da == null) {
            throw new IllegalArgumentException("Availability values cannot be null");
        }
        availability.put(Era.SL, sl);
        availability.put(Era.SW, sw);
        availability.put(Era.CLAN, clan);
        availability.put(Era.DA, da);
        return this;
    }

    public TechAdvancement setAvailability(Era era, AvailabilityValue av) {
        availability.put(era, av);
        return this;
    }

    @Override
    public AvailabilityValue getBaseAvailability(Era era) {
        return availability.get(era);
    }

    @Override
    public boolean isClan() {
        return techBase == TechBase.CLAN;
    }

    @Override
    public boolean isMixedTech() {
        return techBase == TechBase.ALL;
    }
}

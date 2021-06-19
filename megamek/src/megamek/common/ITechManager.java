/*
 * MegaMekLab - Copyright (C) 2017 - The MegaMek Team
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

/**
 * Determines whether a piece of technology (a part, construction option, or entire unit) meets
 * certain constraints such as intro year, tech base, or tech level. Provides methods to define
 * the constraints and options.
 * 
 * @author Neoancient
 *
 */
public interface ITechManager {
    
    /**
     * @return The maximimum intro date for the tech; any tech that appears after this date
     *         will be excluded.
     */
    int getTechIntroYear();
    
    /**
     * @return The date to use in determining the current tech level if {@link #useVariableTechLevel()}
     *         is true.
     */
    int getGameYear();
    
    /**
     * Indicates which faction should be used in determining intro and tech level dates. Not all
     * tech has faction-specific dates. Special consideration is given to F_COMSTAR, which ignores
     * extinction dates of Inner Sphere tech that is later reintroduced. Clan factions treat
     * Star-League Era tech that later goes extinct for the Clans as Clan tech up to the extinction
     * date, providing a transitional stage from the formation of the Clans until early in the Golden
     * Century.
     * 
     * @return One of the F_* faction constants defined in {@link ITechnology}. If < 0, faction
     *         variations will be ignored.
     */
    int getTechFaction();
    
    /**
     * @return True if the tech should have a Clan tech base, or false for Inner Sphere/Periphery
     */
    boolean useClanTechBase();
    
    /**
     * @return True if both Inner Sphere and Clan tech bases are acceptable.
     */
    boolean useMixedTech();
    
    /**
     * @return The maximum allowable tech level.
     */
    SimpleTechLevel getTechLevel();
    
    /**
     * @return If true and {@link #getTechLevel()} is <code>UNOFFICIAL</code>, intro dates are ignored.
     */
    boolean unofficialNoYear();
    
    /**
     * @return If true, the rules level of a piece of tech will vary as it moves through production
     *         stages per the rules in IO, pp. 33-4.
     */
    boolean useVariableTechLevel();
    
    /**
     * @return Whether tech that is no longer in production should be included.
     */
    boolean showExtinct();
    
    default boolean isLegal(ITechnology tech) {
        // Unofficial tech has the option to ignore year availability
        if ((getTechLevel() == SimpleTechLevel.UNOFFICIAL)
                && unofficialNoYear()) {
            return useMixedTech() || (tech.getTechBase() == ITechnology.TECH_BASE_ALL)
                    || (useClanTechBase() == tech.isClan());
        }

        int faction = getTechFaction();
        boolean clanTech = useClanTechBase();
        
        int isIntroDate = tech.getIntroductionDate(false);
        int clanIntroDate = tech.getIntroductionDate(true);
        boolean introducedIS = (isIntroDate != ITechnology.DATE_NONE) && (isIntroDate <= getTechIntroYear());
        boolean introducedClan = (clanIntroDate != ITechnology.DATE_NONE) && (clanIntroDate <= getTechIntroYear());
        boolean extinctIS = tech.isExtinct(getTechIntroYear(), false);
        boolean extinctClan = tech.isExtinct(getTechIntroYear(), true);
        // A little bit of hard-coded universe detail
        if ((faction == ITechnology.F_CS)
                && extinctIS && (isIntroDate != ITechnology.DATE_NONE)
                && (tech.getBaseAvailability(ITechnology.getTechEra(getTechIntroYear())) < ITechnology.RATING_X)
                && isIntroDate <= getTechIntroYear()) {
            // ComStar has access to Star League tech that is otherwise extinct in the Inner Sphere as if TH,
            // unless it has an availability of X (which is SLDF Royal equipment).
            extinctIS = false;
            faction = ITechnology.F_TH;
        } else if (useClanTechBase() && !introducedClan
                && tech.isAvailableIn(2787, false, ITechnology.F_TH)
                && !extinctClan && (tech.getExtinctionDate(false) > getGameYear())
                && (tech.getExtinctionDate(false) != ITechnology.DATE_NONE)) {
            // Transitional period: Clans can treat IS tech as Clan if it was available to TH and
            // has an extinction date that it hasn't reached yet (using specific Clan date if given).
            faction = ITechnology.F_TH;
            clanTech = false;
        }
        if (useMixedTech()) {
            if ((!introducedIS && !introducedClan) 
                    || (!showExtinct()
                            && (tech.isExtinct(getTechIntroYear())))) {
                return false;
            } else if (useVariableTechLevel()) {
                // If using tech progression with mixed tech, we pass if either IS or Clan meets the required level
                return tech.getSimpleLevel(getGameYear(), true, faction).compareTo(getTechLevel()) <= 0
                        || tech.getSimpleLevel(getGameYear(), false, faction).compareTo(getTechLevel()) <= 0;
            }
        } else {
            if (tech.getTechBase() != ITechnology.TECH_BASE_ALL
                    && clanTech != tech.isClan()) {
                return false;
            } else if (clanTech && (!introducedClan || (!showExtinct() && extinctClan))) {
                return false;
            } else if (!clanTech && (!introducedIS || (!showExtinct() && extinctIS))) {
                return false;
            } else if (useVariableTechLevel()) {
                return tech.getSimpleLevel(getGameYear(), clanTech, faction).compareTo(getTechLevel()) <= 0;
            }
        }
        // It's available in the year and we're not using tech progression, so just check the tech level.
        return tech.getStaticTechLevel().compareTo(getTechLevel()) <= 0;
    }
}


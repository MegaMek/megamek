/*
 * Copyright (C) 2025 The MegaMek Team. All Rights Reserved.
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
package megamek.common.enums;

import static megamek.common.interfaces.ITechnology.DATE_ES;
import static megamek.common.interfaces.ITechnology.DATE_NONE;
import static megamek.common.interfaces.ITechnology.DATE_PS;

import java.util.HashMap;
import java.util.Map;

import megamek.common.SimpleTechLevel;
import megamek.common.TechAdvancement;
import megamek.common.options.OptionsConstants;

/**
 * Defines MD (cybernetic) augmentation types with their associated tech progression and cost data. This enum maps pilot
 * option constants to their BattleTech tech base, tech rating, availability, introduction dates, and C-bill costs per
 * IO (Interstellar Operations) rules.
 *
 * <p>Tech data sourced from IO: Alternate Eras equipment tables.</p>
 *
 * @author MegaMek Team
 * @since 0.50.07
 */
public enum MDAugmentationType {
    // @formatter:off

    // Basic Implants
    PAIN_SHUNT(
          OptionsConstants.MD_PAIN_SHUNT,
          "Artificial Pain Shunt",
          500000,
          new TechAdvancement(TechBase.ALL)
                .setAdvancement(DATE_ES, DATE_ES, DATE_NONE)
                .setTechRating(TechRating.C)
                .setAvailability(AvailabilityValue.F, AvailabilityValue.F, AvailabilityValue.F, AvailabilityValue.F)
                .setStaticTechLevel(SimpleTechLevel.ADVANCED)),

    COMM_IMPLANT(
          OptionsConstants.MD_COMM_IMPLANT,
          "Communications Implant",
          8000,
          new TechAdvancement(TechBase.ALL)
                .setAdvancement(2600, 2600, DATE_NONE)
                .setTechRating(TechRating.E)
                .setAvailability(AvailabilityValue.E, AvailabilityValue.F, AvailabilityValue.D, AvailabilityValue.D)
                .setStaticTechLevel(SimpleTechLevel.ADVANCED)),

    BOOSTED_COMM_IMPLANT(
          OptionsConstants.MD_BOOST_COMM_IMPLANT,
          "Boosted Communications Implant",
          8000,
          new TechAdvancement(TechBase.IS)
                .setAdvancement(3060, DATE_NONE, DATE_NONE)
                .setTechRating(TechRating.E)
                .setAvailability(AvailabilityValue.X, AvailabilityValue.X, AvailabilityValue.D, AvailabilityValue.D)
                .setStaticTechLevel(SimpleTechLevel.ADVANCED)),

    // Sensory Implants - Basic types (infantry only for BV)
    CYBER_IMP_AUDIO(
          OptionsConstants.MD_CYBER_IMP_AUDIO,
          "Sensory Implants (Audio)",
          650000,
          new TechAdvancement(TechBase.ALL)
                .setAdvancement(2610, 2610, DATE_NONE)
                .setTechRating(TechRating.E)
                .setAvailability(AvailabilityValue.E, AvailabilityValue.F, AvailabilityValue.C, AvailabilityValue.C)
                .setStaticTechLevel(SimpleTechLevel.ADVANCED)),

    CYBER_IMP_VISUAL(
          OptionsConstants.MD_CYBER_IMP_VISUAL,
          "Sensory Implants (IR/EM)",
          650000,
          new TechAdvancement(TechBase.ALL)
                .setAdvancement(2610, 2610, DATE_NONE)
                .setTechRating(TechRating.E)
                .setAvailability(AvailabilityValue.E, AvailabilityValue.F, AvailabilityValue.C, AvailabilityValue.C)
                .setStaticTechLevel(SimpleTechLevel.ADVANCED)),

    CYBER_IMP_LASER(
          OptionsConstants.MD_CYBER_IMP_LASER,
          "Sensory Implants (Laser)",
          600000,
          new TechAdvancement(TechBase.ALL)
                .setAdvancement(2610, 2610, DATE_NONE)
                .setTechRating(TechRating.E)
                .setAvailability(AvailabilityValue.D, AvailabilityValue.E, AvailabilityValue.C, AvailabilityValue.C)
                .setStaticTechLevel(SimpleTechLevel.ADVANCED)),

    CYBER_IMP_TELE(
          OptionsConstants.MD_CYBER_IMP_TELE,
          "Sensory Implants (Telescopic)",
          600000,
          new TechAdvancement(TechBase.ALL)
                .setAdvancement(2610, 2610, DATE_NONE)
                .setTechRating(TechRating.E)
                .setAvailability(AvailabilityValue.D, AvailabilityValue.E, AvailabilityValue.C, AvailabilityValue.C)
                .setStaticTechLevel(SimpleTechLevel.ADVANCED)),

    // Multi-Modal Sensory Implants
    MM_IMPLANTS(
          OptionsConstants.MD_MM_IMPLANTS,
          "Multi-Modal Sensory Implants",
          900000,
          new TechAdvancement(TechBase.IS)
                .setAdvancement(3055, DATE_NONE, DATE_NONE)
                .setTechRating(TechRating.E)
                .setAvailability(AvailabilityValue.X, AvailabilityValue.X, AvailabilityValue.D, AvailabilityValue.D)
                .setStaticTechLevel(SimpleTechLevel.ADVANCED)),

    ENH_MM_IMPLANTS(
          OptionsConstants.MD_ENH_MM_IMPLANTS,
          "Enhanced Multi-Modal Sensory Implants",
          1600000,
          new TechAdvancement(TechBase.IS)
                .setAdvancement(3060, DATE_NONE, DATE_NONE)
                .setTechRating(TechRating.E)
                .setAvailability(AvailabilityValue.X, AvailabilityValue.X, AvailabilityValue.E, AvailabilityValue.E)
                .setStaticTechLevel(SimpleTechLevel.ADVANCED)),

    // Filtration Implants
    FILTRATION(
          OptionsConstants.MD_FILTRATION,
          "Filtration Implants",
          60000,
          new TechAdvancement(TechBase.ALL)
                .setAdvancement(2580, 2580, DATE_NONE)
                .setTechRating(TechRating.C)
                .setAvailability(AvailabilityValue.A, AvailabilityValue.A, AvailabilityValue.A, AvailabilityValue.A)
                .setStaticTechLevel(SimpleTechLevel.ADVANCED)),

    // Gas Effuser Implants
    GAS_EFFUSER_PHEROMONE(
          OptionsConstants.MD_GAS_EFFUSER_PHEROMONE,
          "Gas Effuser Implant (Pheromone)",
          40000,
          new TechAdvancement(TechBase.IS)
                .setAdvancement(3060, DATE_NONE, DATE_NONE)
                .setTechRating(TechRating.E)
                .setAvailability(AvailabilityValue.X, AvailabilityValue.X, AvailabilityValue.F, AvailabilityValue.F)
                .setStaticTechLevel(SimpleTechLevel.EXPERIMENTAL)),

    GAS_EFFUSER_TOXIN(
          OptionsConstants.MD_GAS_EFFUSER_TOXIN,
          "Gas Effuser Implant (Toxin)",
          10000,
          new TechAdvancement(TechBase.IS)
                .setAdvancement(3060, DATE_NONE, DATE_NONE)
                .setTechRating(TechRating.F)
                .setAvailability(AvailabilityValue.X, AvailabilityValue.X, AvailabilityValue.F, AvailabilityValue.F)
                .setStaticTechLevel(SimpleTechLevel.EXPERIMENTAL)),

    // Dermal Implants
    DERMAL_ARMOR(
          OptionsConstants.MD_DERMAL_ARMOR,
          "Dermal Armor Implants",
          1500000,
          new TechAdvancement(TechBase.IS)
                .setAdvancement(2950, DATE_NONE, DATE_NONE)
                .setTechRating(TechRating.E)
                .setAvailability(AvailabilityValue.X, AvailabilityValue.E, AvailabilityValue.E, AvailabilityValue.E)
                .setStaticTechLevel(SimpleTechLevel.ADVANCED)),

    DERMAL_CAMO_ARMOR(
          OptionsConstants.MD_DERMAL_CAMO_ARMOR,
          "Dermal Camouflage Implants",
          1100000,
          new TechAdvancement(TechBase.IS)
                .setAdvancement(3065, DATE_NONE, DATE_NONE)
                .setTechRating(TechRating.F)
                .setAvailability(AvailabilityValue.X, AvailabilityValue.X, AvailabilityValue.F, AvailabilityValue.F)
                .setStaticTechLevel(SimpleTechLevel.EXPERIMENTAL)),

    // Myomer and Processor Implants
    TSM_IMPLANT(
          OptionsConstants.MD_TSM_IMPLANT,
          "Triple-Strength Myomer Implants",
          2500000,
          new TechAdvancement(TechBase.IS)
                .setAdvancement(3060, DATE_NONE, DATE_NONE)
                .setTechRating(TechRating.F)
                .setAvailability(AvailabilityValue.X, AvailabilityValue.X, AvailabilityValue.E, AvailabilityValue.E)
                .setStaticTechLevel(SimpleTechLevel.ADVANCED)),

    TRIPLE_CORE_PROCESSOR(
          OptionsConstants.MD_TRIPLE_CORE_PROCESSOR,
          "Triple-Core Processor Implant",
          3000000,
          new TechAdvancement(TechBase.IS)
                .setAdvancement(3068, DATE_NONE, DATE_NONE)
                .setTechRating(TechRating.E)
                .setAvailability(AvailabilityValue.X, AvailabilityValue.X, AvailabilityValue.F, AvailabilityValue.F)
                .setStaticTechLevel(SimpleTechLevel.EXPERIMENTAL)),

    // DNI Implants
    VDNI(
          OptionsConstants.MD_VDNI,
          "Vehicular Direct Neural Interface",
          1400000,
          new TechAdvancement(TechBase.IS)
                .setAdvancement(3055, DATE_NONE, DATE_NONE)
                .setTechRating(TechRating.F)
                .setAvailability(AvailabilityValue.X, AvailabilityValue.X, AvailabilityValue.E, AvailabilityValue.E)
                .setStaticTechLevel(SimpleTechLevel.ADVANCED)),

    BVDNI(
          OptionsConstants.MD_BVDNI,
          "Buffered VDNI",
          2000000,
          new TechAdvancement(TechBase.IS)
                .setAdvancement(3065, DATE_NONE, DATE_NONE)
                .setTechRating(TechRating.F)
                .setAvailability(AvailabilityValue.X, AvailabilityValue.X, AvailabilityValue.F, AvailabilityValue.F)
                .setStaticTechLevel(SimpleTechLevel.EXPERIMENTAL)),

    PROTO_DNI(
          OptionsConstants.MD_PROTO_DNI,
          "Prototype Direct Neural Interface",
          2500000,
          new TechAdvancement(TechBase.IS)
                .setAdvancement(3052, DATE_NONE, DATE_NONE)
                .setPrototypeFactions(Faction.WB)
                .setTechRating(TechRating.F)
                .setAvailability(AvailabilityValue.X, AvailabilityValue.X, AvailabilityValue.F, AvailabilityValue.F)
                .setStaticTechLevel(SimpleTechLevel.EXPERIMENTAL)),

    // Explosive Suicide Implant
    SUICIDE_IMPLANTS(
          OptionsConstants.MD_SUICIDE_IMPLANTS,
          "Explosive Suicide Implant",
          250,
          new TechAdvancement(TechBase.ALL)
                .setAdvancement(DATE_PS, DATE_PS, DATE_NONE)
                .setTechRating(TechRating.C)
                .setAvailability(AvailabilityValue.B, AvailabilityValue.B, AvailabilityValue.B, AvailabilityValue.B)
                .setStaticTechLevel(SimpleTechLevel.ADVANCED)),

    // Prosthetic Limbs
    PL_MASC(
          OptionsConstants.MD_PL_MASC,
          "Prosthetic Leg MASC",
          255000,
          new TechAdvancement(TechBase.IS)
                .setAdvancement(3065, DATE_NONE, DATE_NONE)
                .setTechRating(TechRating.E)
                .setAvailability(AvailabilityValue.X, AvailabilityValue.X, AvailabilityValue.F, AvailabilityValue.F)
                .setStaticTechLevel(SimpleTechLevel.EXPERIMENTAL)),

    PL_ENHANCED(
          OptionsConstants.MD_PL_ENHANCED,
          "Prosthetic Limb, Enhanced",
          100000,
          new TechAdvancement(TechBase.ALL)
                .setAdvancement(DATE_ES, DATE_ES, DATE_NONE)
                .setTechRating(TechRating.E)
                .setAvailability(AvailabilityValue.F, AvailabilityValue.F, AvailabilityValue.E, AvailabilityValue.E)
                .setStaticTechLevel(SimpleTechLevel.ADVANCED)),

    PL_I_ENHANCED(
          OptionsConstants.MD_PL_I_ENHANCED,
          "Prosthetic Limb, Improved Enhanced",
          202000,
          new TechAdvancement(TechBase.ALL)
                .setAdvancement(2650, 2650, DATE_NONE)
                .setTechRating(TechRating.E)
                .setAvailability(AvailabilityValue.F, AvailabilityValue.F, AvailabilityValue.F, AvailabilityValue.F)
                .setStaticTechLevel(SimpleTechLevel.ADVANCED)),

    PL_EXTRA_LIMBS(
          OptionsConstants.MD_PL_EXTRA_LIMBS,
          "Prosthetic Limb, Extraneous",
          400000,
          new TechAdvancement(TechBase.IS)
                .setAdvancement(3068, DATE_NONE, DATE_NONE)
                .setTechRating(TechRating.F)
                .setAvailability(AvailabilityValue.X, AvailabilityValue.X, AvailabilityValue.F, AvailabilityValue.F)
                .setStaticTechLevel(SimpleTechLevel.EXPERIMENTAL)),

    PL_TAIL(
          OptionsConstants.MD_PL_TAIL,
          "Prosthetic Tail, Enhanced",
          60000,
          new TechAdvancement(TechBase.IS)
                .setAdvancement(3068, DATE_NONE, DATE_NONE)
                .setTechRating(TechRating.E)
                .setAvailability(AvailabilityValue.X, AvailabilityValue.X, AvailabilityValue.F, AvailabilityValue.F)
                .setStaticTechLevel(SimpleTechLevel.EXPERIMENTAL)),

    PL_GLIDER(
          OptionsConstants.MD_PL_GLIDER,
          "Prosthetic Glider Wings",
          90000,
          new TechAdvancement(TechBase.IS)
                .setAdvancement(3069, DATE_NONE, DATE_NONE)
                .setTechRating(TechRating.E)
                .setAvailability(AvailabilityValue.X, AvailabilityValue.X, AvailabilityValue.E, AvailabilityValue.E)
                .setStaticTechLevel(SimpleTechLevel.ADVANCED)),

    PL_FLIGHT(
          OptionsConstants.MD_PL_FLIGHT,
          "Prosthetic Powered Flight Wings",
          120000,
          new TechAdvancement(TechBase.IS)
                .setAdvancement(3070, DATE_NONE, DATE_NONE)
                .setTechRating(TechRating.F)
                .setAvailability(AvailabilityValue.X, AvailabilityValue.X, AvailabilityValue.F, AvailabilityValue.F)
                .setStaticTechLevel(SimpleTechLevel.EXPERIMENTAL));

    // @formatter:on

    private final String optionName;
    private final String displayName;
    private final long cost;
    private final TechAdvancement techAdvancement;

    private static final Map<String, MDAugmentationType> OPTION_MAP = new HashMap<>();

    static {
        for (MDAugmentationType type : values()) {
            OPTION_MAP.put(type.optionName, type);
        }
    }

    /**
     * Creates an MD augmentation type.
     *
     * @param optionName      The OptionsConstants.MD_* constant name
     * @param displayName     Human-readable name for the augmentation
     * @param cost            Cost in C-bills
     * @param techAdvancement Tech progression data (dates, availability, tech rating)
     */
    MDAugmentationType(String optionName, String displayName, long cost, TechAdvancement techAdvancement) {
        this.optionName = optionName;
        this.displayName = displayName;
        this.cost = cost;
        this.techAdvancement = techAdvancement;
    }

    /**
     * @return The OptionsConstants.MD_* constant name for this augmentation
     */
    public String getOptionName() {
        return optionName;
    }

    /**
     * @return Human-readable display name for the augmentation
     */
    public String getDisplayName() {
        return displayName;
    }

    /**
     * @return Cost in C-bills for the augmentation
     */
    public long getCost() {
        return cost;
    }

    /**
     * @return The TechAdvancement data for this augmentation
     */
    public TechAdvancement getTechAdvancement() {
        return techAdvancement;
    }

    /**
     * @return The tech base (IS, Clan, or ALL) for this augmentation
     */
    public TechBase getTechBase() {
        return techAdvancement.getTechBase();
    }

    /**
     * @return The tech rating for this augmentation
     */
    public TechRating getTechRating() {
        return techAdvancement.getTechRating();
    }

    /**
     * Gets the availability for a specific era.
     *
     * @param era The era to check
     *
     * @return The availability rating for the specified era
     */
    public AvailabilityValue getAvailability(Era era) {
        return techAdvancement.getBaseAvailability(era);
    }

    /**
     * Gets the introduction date for this augmentation.
     *
     * @param isClan True to get Clan intro date, false for IS
     *
     * @return The introduction year, or DATE_NONE if not available for that faction
     */
    public int getIntroductionDate(boolean isClan) {
        return techAdvancement.getIntroductionDate(isClan);
    }

    /**
     * Checks if this augmentation is available in a given year.
     *
     * @param year   The year to check
     * @param isClan True if checking for Clan availability
     *
     * @return True if the augmentation is available
     */
    public boolean isAvailableIn(int year, boolean isClan) {
        // Must use the 3-param version: isAvailableIn(year, clan, ignoreExtinction)
        // The 2-param version treats the second param as ignoreExtinction, not clan
        return techAdvancement.isAvailableIn(year, isClan, false);
    }

    /**
     * Looks up an augmentation type by its option constant name.
     *
     * @param optionName The OptionsConstants.MD_* constant name
     *
     * @return The matching MDAugmentationType, or null if not found
     */
    public static MDAugmentationType getByOptionName(String optionName) {
        return OPTION_MAP.get(optionName);
    }

    /**
     * Calculates the total cost of all augmentations for a given set of option names.
     *
     * @param optionNames Collection of OptionsConstants.MD_* names that are enabled
     *
     * @return Total cost in C-bills
     */
    public static long calculateTotalCost(Iterable<String> optionNames) {
        long total = 0;
        for (String name : optionNames) {
            MDAugmentationType type = OPTION_MAP.get(name);
            if (type != null) {
                total += type.cost;
            }
        }
        return total;
    }

    @Override
    public String toString() {
        return displayName;
    }
}

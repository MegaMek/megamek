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
package megamek.common.enums;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import megamek.common.annotations.Nullable;
import megamek.common.options.IOption;
import megamek.common.options.OptionsConstants;
import megamek.common.options.PilotOptions;
import megamek.logging.MMLogger;

/**
 * Chooses the cybernetics a Manei Domini warrior of a given rank receives.
 *
 * <p>Applies the availability chart in <i>Jihad Hot Spots: 3072</i>, pp. 121, 123-124 (Rules Annex:
 * Manei Domini Classes / Manei Domini Nomenclature). The rank sets how many implants a warrior carries
 * and how advanced they may be; this picks them.</p>
 *
 * <p>Only the implants MegaMek models are issued. The chart lists several the game has no equivalent
 * for - cosmetic enhancements, a secondary power supply, the separate recorder, receiver and
 * transmitter units - and issuing those would record implants that do nothing in play, so the count
 * reflects what a warrior actually fields. Where the source is finer-grained than the game, the game's
 * option stands in: one multi-modal sensory implant covers its separate eyes, ears and speech.</p>
 *
 * <p>Which implants do anything depends on how the warrior fights. Most of the catalogue is explicitly
 * conventional-infantry-only - the effusers, the sensory and optical implants, the enhanced
 * prosthetics and prosthetic leg MASC all say so in their own descriptions, and dermal armour and the
 * triple-strength myomer implant are read only by the infantry and BattleArmor calculators. The neural
 * interfaces are the reverse, being what lets a warrior drive the unit they sit in.</p>
 *
 * <p><b>Six entries are placed by inference, not by the chart.</b> They are Manei Domini
 * augmentations the game implements and the availability chart does not list, so it gives them no
 * level and one had to be chosen. This matters if the chart is ever revisited: every other entry here
 * can be pointed at a line in it, and these cannot.</p>
 *
 * <ul>
 *   <li>Level 3 - extraneous limbs, the prosthetic tail and both sets of prosthetic wings
 *       (<i>Interstellar Operations</i>, pp. 84-85), placed with the other limb work.</li>
 *   <li>Level 4 - the dermal armour camouflage (IO p. 80), placed with the dermal armour it varies,
 *       and the triple-core processor (IO p. 81), placed with the other significant augmentations.</li>
 * </ul>
 *
 * <p>Deliberately left out: the prototype direct neural interface, being the crude forerunner of the
 * interface these warriors already carry, and the filtration implants, which the game declares
 * unimplemented.</p>
 *
 * @see ManeiDominiAugmentationRank
 */
public final class ManeiDominiImplants {

    private static final MMLogger LOGGER = MMLogger.create(ManeiDominiImplants.class);

    /**
     * Who an implant actually does something for, according to the effect MegaMek gives it.
     */
    public enum ImplantAudience {
        /** Does something only for a warrior fighting on foot. */
        ON_FOOT,
        /** Does something only for a warrior piloting a unit. */
        PILOTING,
        /** Useful whoever carries it. */
        ANYONE;

        /**
         * @param unitType the kind of unit the warrior serves in
         *
         * @return {@code true} if an implant for this audience does something for such a warrior
         */
        public boolean servesA(AugmentedUnitType unitType) {
            return switch (this) {
                case ANYONE -> true;
                case ON_FOOT -> unitType.fightsOnFoot();
                // Anyone who operates a machine, which a battle armour trooper does in their suit -
                // the construction rules put battle armour on every neural interface's unit list and
                // leave only conventional infantry off it.
                case PILOTING -> unitType != AugmentedUnitType.CONVENTIONAL_INFANTRY;
            };
        }
    }

    /**
     * One issuable implant: the game options it may be satisfied by, the level it sits at, and who it
     * benefits.
     *
     * <p>Most entries name a single option. The source's "Cybernetic Eye Implants" is one entry that
     * MegaMek splits into three optical implants, so that entry carries all three and one is rolled -
     * a formation fields a mix of optics rather than every warrior carrying identical eyes.</p>
     *
     * @param level         the implant level, which the warrior's rank caps
     * @param audience      who this implant actually does something for
     * @param factionCodes  the factions the construction rules give it to, empty for any
     * @param optionChoices the game options this entry may be satisfied by; one is chosen at random
     */
    public record ImplantEntry(int level, ImplantAudience audience, List<String> factionCodes,
                               List<String> optionChoices) {

        private ImplantEntry(int level, ImplantAudience audience, String singleOption) {
            this(level, audience, ANY_FACTION, List.of(singleOption));
        }

        private ImplantEntry(int level, ImplantAudience audience, List<String> optionChoices) {
            this(level, audience, ANY_FACTION, optionChoices);
        }

        private ImplantEntry(int level, ImplantAudience audience, List<String> factionCodes,
              String singleOption) {
            this(level, audience, factionCodes, List.of(singleOption));
        }

        /**
         * @param factionCode the faction the force is being generated for
         *
         * @return {@code true} if that faction fields this augmentation
         */
        boolean isFieldedBy(@Nullable String factionCode) {
            if (factionCodes.isEmpty()) {
                return true;
            }
            if (factionCode == null) {
                return false;
            }
            // A sub-command carries its parent's key followed by a dot, as in WOB.SD.
            String primary = factionCode.split("\\.")[0];
            return factionCodes.stream().anyMatch(primary::equalsIgnoreCase);
        }
    }

    /** Used by an entry the construction rules place with no faction restriction. */
    private static final List<String> ANY_FACTION = List.of();

    /** The Capellan Confederation, whose Thuggee Phansigars field the limb prosthetics. */
    private static final List<String> CAPELLAN_ONLY = List.of("CC");

    /**
     * The issuable catalogue, in source order. Level 0 contributes nothing: both of its entries
     * (cosmetic enhancements, and type 4 and 5 prosthetic limbs) are among those MegaMek does not
     * model.
     */
    private static final List<ImplantEntry> CATALOGUE = List.of(
          new ImplantEntry(1, ImplantAudience.ON_FOOT, OptionsConstants.MD_PL_ENHANCED),
          new ImplantEntry(2, ImplantAudience.ANYONE, OptionsConstants.MD_PAIN_SHUNT),
          new ImplantEntry(2, ImplantAudience.ON_FOOT, OptionsConstants.MD_CYBER_IMP_AUDIO),
          new ImplantEntry(2, ImplantAudience.ON_FOOT, List.of(OptionsConstants.MD_CYBER_IMP_VISUAL,
                OptionsConstants.MD_CYBER_IMP_LASER,
                OptionsConstants.MD_CYBER_IMP_TELE)),
          new ImplantEntry(2, ImplantAudience.ANYONE, OptionsConstants.MD_COMM_IMPLANT),
          new ImplantEntry(3, ImplantAudience.ON_FOOT, OptionsConstants.MD_PL_I_ENHANCED),
          new ImplantEntry(3, ImplantAudience.ON_FOOT, OptionsConstants.MD_PL_MASC),
          new ImplantEntry(3, ImplantAudience.ON_FOOT, OptionsConstants.MD_GAS_EFFUSER_PHEROMONE),
          // Capellan, not Word of Blake: the construction rules give all four to the Confederation
          // alone, whose Thuggee Phansigars field them. Their level is still inferred - see the class
          // comment - but the faction is the source's.
          new ImplantEntry(3, ImplantAudience.ON_FOOT, CAPELLAN_ONLY, OptionsConstants.MD_PL_EXTRA_LIMBS),
          new ImplantEntry(3, ImplantAudience.ON_FOOT, CAPELLAN_ONLY, OptionsConstants.MD_PL_TAIL),
          new ImplantEntry(3, ImplantAudience.ON_FOOT, CAPELLAN_ONLY, OptionsConstants.MD_PL_GLIDER),
          new ImplantEntry(3, ImplantAudience.ON_FOOT, CAPELLAN_ONLY, OptionsConstants.MD_PL_FLIGHT),
          new ImplantEntry(3, ImplantAudience.PILOTING, OptionsConstants.MD_VDNI),
          new ImplantEntry(3, ImplantAudience.ANYONE, OptionsConstants.MD_BOOST_COMM_IMPLANT),
          new ImplantEntry(3, ImplantAudience.ANYONE, OptionsConstants.MD_MM_IMPLANTS),
          new ImplantEntry(4, ImplantAudience.ON_FOOT, OptionsConstants.MD_GAS_EFFUSER_TOXIN),
          new ImplantEntry(4, ImplantAudience.ON_FOOT, OptionsConstants.MD_DERMAL_ARMOR),
          new ImplantEntry(4, ImplantAudience.ON_FOOT, OptionsConstants.MD_TSM_IMPLANT),
          // Also placed by inference - see the class comment. The camouflage belongs with the dermal
          // armour it varies; the processor's work is all done through the unit, which is why it
          // serves a warrior in a cockpit and needs a neural interface to reach one.
          new ImplantEntry(4, ImplantAudience.ON_FOOT, OptionsConstants.MD_DERMAL_CAMO_ARMOR),
          new ImplantEntry(4, ImplantAudience.PILOTING, OptionsConstants.MD_TRIPLE_CORE_PROCESSOR),
          new ImplantEntry(5, ImplantAudience.ANYONE, OptionsConstants.MD_ENH_MM_IMPLANTS),
          new ImplantEntry(5, ImplantAudience.PILOTING, OptionsConstants.MD_BVDNI));

    /**
     * Implants that supersede a lesser version of themselves. Holding both is meaningless, so taking
     * the improved one rules the basic one out and vice versa.
     */
    private static final Map<String, String> SUPERSEDED_BY = Map.of(
          OptionsConstants.MD_PL_ENHANCED, OptionsConstants.MD_PL_I_ENHANCED,
          OptionsConstants.MD_COMM_IMPLANT, OptionsConstants.MD_BOOST_COMM_IMPLANT,
          OptionsConstants.MD_MM_IMPLANTS, OptionsConstants.MD_ENH_MM_IMPLANTS,
          OptionsConstants.MD_VDNI, OptionsConstants.MD_BVDNI,
          // Powered flight wings carry every glider benefit and the game forbids the pair outright,
          // so a warrior is issued one or the other.
          OptionsConstants.MD_PL_GLIDER, OptionsConstants.MD_PL_FLIGHT);

    /**
     * Implants that reach the unit through a neural interface and do nothing without one. The
     * multi-modal sensory implants need it to sync with the unit's sensors; the triple-core processor
     * says outright that it requires a VDNI or a buffered VDNI.
     */
    private static final List<String> REQUIRE_NEURAL_INTERFACE = List.of(
          OptionsConstants.MD_MM_IMPLANTS, OptionsConstants.MD_ENH_MM_IMPLANTS,
          OptionsConstants.MD_TRIPLE_CORE_PROCESSOR);

    /** The neural interfaces that satisfy that requirement. */
    private static final List<String> NEURAL_INTERFACES = List.of(
          OptionsConstants.MD_VDNI, OptionsConstants.MD_BVDNI);

    /** The implant level above which a rank must reach to be issued a buffered neural interface. */
    private static final int BUFFERED_INTERFACE_LEVEL = 5;

    private ManeiDominiImplants() {
    }

    /**
     * The explosive charge every Manei Domini implant carries.
     *
     * <p>Fitted to every Manei Domini and not counted against their allowance: the source describes it
     * as a property of their augmentation rather than an implant issued in place of another. It is
     * useful whoever carries it - any entity may trigger it, and only its reactive detonation is
     * specific to infantry.</p>
     *
     * @return the game option for the explosive charge
     */
    public static String getExplosiveCharge() {
        return OptionsConstants.MD_SUICIDE_IMPLANTS;
    }

    /**
     * Chooses the implants a warrior of the given rank receives.
     *
     * <p>Implants that serve the warrior are drawn first and at least one is guaranteed, so nobody
     * comes out carrying only implants that do nothing for the way they fight. The rest make up the
     * numbers, because drawing strictly would leave junior ranks short of the chart's minimum: a
     * warrior in a cockpit has only two implants that serve them at the level 2 ceiling, against a
     * stated minimum of three at Beta.</p>
     *
     * @param rank                the rank whose allowance governs the selection
     * @param unitType            the kind of unit the warrior serves in
     *
     * @return the game options to fit, excluding the explosive charge every Manei Domini receives
     */
    public static List<String> selectFor(ManeiDominiAugmentationRank rank, AugmentedUnitType unitType,
          @Nullable String factionCode) {
        List<ImplantEntry> withinLevel = CATALOGUE.stream()
                                               .filter(entry -> entry.level() <= rank.getMaximumImplantLevel())
                                               .filter(entry -> entry.isFieldedBy(factionCode))
                                               .toList();
        List<ImplantEntry> useful = new ArrayList<>(withinLevel.stream()
              .filter(entry -> entry.audience().servesA(unitType))
              .toList());
        List<ImplantEntry> remainder = new ArrayList<>(withinLevel.stream()
              .filter(entry -> !entry.audience().servesA(unitType))
              .toList());

        int target = randomBetween(rank.getMinimumImplants(), rank.getMaximumImplants());
        List<String> issued = new ArrayList<>();
        // Guarantee the first one is useful, so nobody comes out carrying nothing but implants that
        // do nothing for the way they fight.
        drawInto(issued, useful, 1);
        drawInto(issued, useful, target);
        drawInto(issued, remainder, target);

        ensureNeuralInterface(issued, rank, unitType);
        return issued;
    }

    /**
     * Selects a warrior's implants and fits them.
     *
     * <p>Takes {@link PilotOptions} rather than anything more specific so that both generators can
     * call it: MekHQ's {@code PersonnelOptions} extends it, and MegaMek's crews carry it directly. A
     * Shadow Division raised in either therefore ends up augmented the same way.</p>
     *
     * <p>The explosive charge every Manei Domini implant carries is fitted here too, and is not
     * counted against the allowance - the source describes it as a property of their augmentation
     * rather than an implant issued in place of another.</p>
     *
     * @param options             the warrior's options, which this sets
     * @param rank                the rank whose allowance governs the selection
     * @param unitType            the kind of unit the warrior serves in
     *
     * @return the implants fitted, excluding the explosive charge
     */
    public static List<String> fitTo(PilotOptions options, ManeiDominiAugmentationRank rank,
          AugmentedUnitType unitType, @Nullable String factionCode) {
        setOption(options, getExplosiveCharge());
        List<String> issued = selectFor(rank, unitType, factionCode);
        for (String implant : issued) {
            setOption(options, implant);
        }
        return issued;
    }

    /**
     * Switches one option on, if the warrior's options carry it at all.
     *
     * @return {@code true} if the option was found and set
     */
    private static boolean setOption(PilotOptions options, String optionName) {
        IOption option = options.getOption(optionName);
        if (option == null) {
            LOGGER.warn("[ManeiDomini] '{}' is not an option these warriors carry; not fitted",
                  optionName);
            return false;
        }
        option.setValue(true);
        return true;
    }

    /**
     * @param option              a game option from the catalogue
     * @param unitType            the kind of unit the warrior serves in
     *
     * @return {@code true} if this implant does something for such a warrior
     */
    public static boolean servesWarrior(String option, AugmentedUnitType unitType) {
        return CATALOGUE.stream()
                     .filter(entry -> entry.optionChoices().contains(option))
                     .anyMatch(entry -> entry.audience().servesA(unitType));
    }

    /**
     * @param option a game option
     *
     * @return the implant level of that option, or 0 if it is not part of the catalogue
     */
    public static int levelOf(String option) {
        return CATALOGUE.stream()
                     .filter(entry -> entry.optionChoices().contains(option))
                     .mapToInt(ImplantEntry::level)
                     .findFirst()
                     .orElse(0);
    }

    /**
     * Draws at random from {@code available} until the issued list reaches {@code target}, skipping
     * anything ruled out by an implant already issued. Drawn entries are removed, so a later call
     * cannot re-offer them.
     */
    private static void drawInto(List<String> issued, List<ImplantEntry> available, int target) {
        while ((issued.size() < target) && !available.isEmpty()) {
            ImplantEntry entry = available.remove((int) (Math.random() * available.size()));
            String option = entry.optionChoices()
                                  .get((int) (Math.random() * entry.optionChoices().size()));
            if (isRuledOutBySupersession(option, issued)) {
                continue;
            }
            issued.add(option);
        }
    }

    /**
     * @return {@code true} if this option is the lesser or greater half of a pair already issued
     */
    private static boolean isRuledOutBySupersession(String option, List<String> issued) {
        String supersedes = SUPERSEDED_BY.get(option);
        if ((supersedes != null) && issued.contains(supersedes)) {
            return true;
        }
        return SUPERSEDED_BY.entrySet()
                     .stream()
                     .anyMatch(pair -> pair.getValue().equals(option) && issued.contains(pair.getKey()));
    }

    /**
     * Multi-modal sensory implants only sync with a vehicle's sensors through a neural interface, so a
     * warrior issued one without the other would carry an implant that does nothing.
     *
     * <p>Only non-infantry need it: a warrior on foot carries the sensors on their own body, so a
     * multi-modal implant works for them with nothing to sync it to. The interface takes the place of
     * another implant rather than being added on top, the rank's maximum being what the source allows
     * the warrior to carry - going past it to satisfy a prerequisite would grant an allowance the
     * chart does not.</p>
     */
    private static void ensureNeuralInterface(List<String> issued, ManeiDominiAugmentationRank rank,
          AugmentedUnitType unitType) {
        if (unitType == AugmentedUnitType.CONVENTIONAL_INFANTRY) {
            return;
        }
        boolean needsInterface = issued.stream().anyMatch(REQUIRE_NEURAL_INTERFACE::contains);
        boolean hasInterface = issued.stream().anyMatch(NEURAL_INTERFACES::contains);
        if (!needsInterface || hasInterface) {
            return;
        }
        String neuralInterface = (rank.getMaximumImplantLevel() >= BUFFERED_INTERFACE_LEVEL)
              ? OptionsConstants.MD_BVDNI
              : OptionsConstants.MD_VDNI;

        String surrendered = issued.stream()
                                   .filter(option -> !REQUIRE_NEURAL_INTERFACE.contains(option))
                                   .findFirst()
                                   .orElseGet(issued::getFirst);
        issued.remove(surrendered);
        issued.add(neuralInterface);
    }

    /**
     * @return a value between {@code minimum} and {@code maximum}, both inclusive
     */
    private static int randomBetween(int minimum, int maximum) {
        return minimum + (int) (Math.random() * ((maximum - minimum) + 1));
    }
}

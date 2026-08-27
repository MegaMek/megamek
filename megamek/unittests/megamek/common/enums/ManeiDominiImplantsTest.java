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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import megamek.common.options.OptionsConstants;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

/**
 * Covers the Manei Domini implant availability rules from <i>Jihad Hot Spots: 3072</i>, pp. 121,
 * 123-124.
 *
 * <p>Selection is random, so each case runs many times: a rule that only usually holds is not a rule,
 * and a single draw would let a violation through most runs. Each case also runs for both kinds of
 * warrior, because which implants do anything depends on whether they fight on foot or from a
 * cockpit.</p>
 */
class ManeiDominiImplantsTest {

    /** Enough draws that a rule broken on an uncommon path still shows up. */
    private static final int DRAWS = 400;

    /** Both kinds of warrior: {@code true} fights on foot, {@code false} fights from a cockpit. */
    private static final AugmentedUnitType[] BOTH_AUDIENCES = {
          AugmentedUnitType.CONVENTIONAL_INFANTRY, AugmentedUnitType.BATTLE_MEK };

    /** The faction these rules are generated for unless a case says otherwise. */
    private static final String WORD_OF_BLAKE = "WOB.SD";

    private static String describe(boolean fightsOnFoot) {
        return fightsOnFoot ? "on foot" : "from a cockpit";
    }

    @ParameterizedTest
    @EnumSource(ManeiDominiAugmentationRank.class)
    void implantCountStaysWithinTheRanksAllowance(ManeiDominiAugmentationRank maneiDominiRank) {
        int minimum = maneiDominiRank.getMinimumImplants();
        int maximum = maneiDominiRank.getMaximumImplants();

        for (AugmentedUnitType fightsOnFoot : BOTH_AUDIENCES) {
            for (int draw = 0; draw < DRAWS; draw++) {
                List<String> issued = ManeiDominiImplants.selectFor(maneiDominiRank, fightsOnFoot, WORD_OF_BLAKE);
                assertTrue(issued.size() >= minimum,
                      maneiDominiRank + " drew " + issued.size() + ", fewer than the minimum " + minimum);
                // The chart's maximum is a hard ceiling. A neural interface needed by a multi-modal
                // implant takes another implant's place rather than being granted on top of it.
                assertTrue(issued.size() <= maximum,
                      maneiDominiRank + " drew " + issued.size() + ", beyond the maximum " + maximum);
            }
        }
    }

    @ParameterizedTest
    @EnumSource(ManeiDominiAugmentationRank.class)
    void noImplantExceedsTheRanksLevelCeiling(ManeiDominiAugmentationRank maneiDominiRank) {
        int maximumLevel = maneiDominiRank.getMaximumImplantLevel();

        for (AugmentedUnitType fightsOnFoot : BOTH_AUDIENCES) {
            for (int draw = 0; draw < DRAWS; draw++) {
                for (String option : ManeiDominiImplants.selectFor(maneiDominiRank, fightsOnFoot, WORD_OF_BLAKE)) {
                    assertTrue(ManeiDominiImplants.levelOf(option) <= maximumLevel,
                          maneiDominiRank + " was issued " + option + " at level "
                                + ManeiDominiImplants.levelOf(option)
                                + ", above its ceiling " + maximumLevel);
                }
            }
        }
    }

    @ParameterizedTest
    @EnumSource(ManeiDominiAugmentationRank.class)
    void anImplantIsNeverIssuedTwice(ManeiDominiAugmentationRank maneiDominiRank) {
        for (AugmentedUnitType fightsOnFoot : BOTH_AUDIENCES) {
            for (int draw = 0; draw < DRAWS; draw++) {
                List<String> issued = ManeiDominiImplants.selectFor(maneiDominiRank, fightsOnFoot, WORD_OF_BLAKE);
                assertTrue(issued.stream().distinct().count() == issued.size(),
                      maneiDominiRank + " was issued a duplicate implant: " + issued);
            }
        }
    }

    /**
     * The point of matching implants to the warrior: a MekWarrior has only two implants that do
     * anything for them at the level 2 ceiling, so the numbers are made up from the rest - but never
     * to the point of issuing somebody nothing useful at all.
     */
    @ParameterizedTest
    @EnumSource(ManeiDominiAugmentationRank.class)
    void everyWarriorGetsAtLeastOneImplantThatServesThem(ManeiDominiAugmentationRank maneiDominiRank) {
        for (AugmentedUnitType fightsOnFoot : BOTH_AUDIENCES) {
            for (int draw = 0; draw < DRAWS; draw++) {
                List<String> issued = ManeiDominiImplants.selectFor(maneiDominiRank, fightsOnFoot, WORD_OF_BLAKE);
                boolean anyUseful = issued.stream()
                                          .anyMatch(option ->
                                                ManeiDominiImplants.servesWarrior(option, fightsOnFoot));
                assertTrue(anyUseful,
                      maneiDominiRank + " fighting " + fightsOnFoot
                            + " got nothing useful: " + issued);
            }
        }
    }

    /**
     * Beta is the tight case: up to four implants against a level 2 ceiling. For a MekWarrior only two
     * of those are useful, so this is the rank that proves the fallback is pulling its weight.
     */
    @Test
    void betaCanStillFillItsAllowanceForEitherKindOfWarrior() {
        for (AugmentedUnitType fightsOnFoot : BOTH_AUDIENCES) {
            boolean everFilledToMaximum = false;
            for (int draw = 0; draw < DRAWS; draw++) {
                List<String> issued =
                      ManeiDominiImplants.selectFor(ManeiDominiAugmentationRank.BETA, fightsOnFoot, WORD_OF_BLAKE);
                assertTrue(issued.size() >= 3, "Beta must always reach its minimum of 3, drew " + issued);
                everFilledToMaximum |= (issued.size() >= 4);
            }
            assertTrue(everFilledToMaximum,
                  "Beta must be able to reach its maximum of 4 fighting " + fightsOnFoot);
        }
    }

    @ParameterizedTest
    @EnumSource(ManeiDominiAugmentationRank.class)
    void anImprovedImplantIsNeverHeldAlongsideTheOneItSupersedes(ManeiDominiAugmentationRank maneiDominiRank) {
        for (AugmentedUnitType fightsOnFoot : BOTH_AUDIENCES) {
            for (int draw = 0; draw < DRAWS; draw++) {
                List<String> issued = ManeiDominiImplants.selectFor(maneiDominiRank, fightsOnFoot, WORD_OF_BLAKE);
                assertFalse(issued.contains(OptionsConstants.MD_PL_ENHANCED)
                            && issued.contains(OptionsConstants.MD_PL_I_ENHANCED),
                      "both basic and improved prosthetics issued: " + issued);
                assertFalse(issued.contains(OptionsConstants.MD_COMM_IMPLANT)
                            && issued.contains(OptionsConstants.MD_BOOST_COMM_IMPLANT),
                      "both basic and boosted comm implants issued: " + issued);
                assertFalse(issued.contains(OptionsConstants.MD_MM_IMPLANTS)
                            && issued.contains(OptionsConstants.MD_ENH_MM_IMPLANTS),
                      "both basic and enhanced multi-modal implants issued: " + issued);
                assertFalse(issued.contains(OptionsConstants.MD_VDNI)
                            && issued.contains(OptionsConstants.MD_BVDNI),
                      "both plain and buffered neural interfaces issued: " + issued);
            }
        }
    }

    /**
     * A multi-modal sensory implant only syncs with a vehicle's sensors through a neural interface, so
     * issuing one without the other would fit an implant that does nothing for a MekWarrior.
     */
    @ParameterizedTest
    @EnumSource(ManeiDominiAugmentationRank.class)
    void multiModalImplantsAlwaysComeWithANeuralInterface(ManeiDominiAugmentationRank maneiDominiRank) {
        for (AugmentedUnitType fightsOnFoot : BOTH_AUDIENCES) {
            for (int draw = 0; draw < DRAWS; draw++) {
                List<String> issued = ManeiDominiImplants.selectFor(maneiDominiRank, fightsOnFoot, WORD_OF_BLAKE);
                boolean hasMultiModal = issued.contains(OptionsConstants.MD_MM_IMPLANTS)
                                              || issued.contains(OptionsConstants.MD_ENH_MM_IMPLANTS);
                // A warrior on foot carries the sensors themselves and needs nothing to sync them to,
                // so the requirement is only on those fighting from a cockpit.
                if (!hasMultiModal || (fightsOnFoot == AugmentedUnitType.CONVENTIONAL_INFANTRY)) {
                    continue;
                }
                boolean hasInterface = issued.contains(OptionsConstants.MD_VDNI)
                                             || issued.contains(OptionsConstants.MD_BVDNI);
                assertTrue(hasInterface,
                      maneiDominiRank + " got a multi-modal implant with no neural interface: " + issued);
            }
        }
    }

    /**
     * The explosive charge is a property of being Manei Domini rather than an implant chosen in place
     * of another, so it is fitted separately and must not appear in the selection.
     */
    @ParameterizedTest
    @EnumSource(ManeiDominiAugmentationRank.class)
    void theExplosiveChargeIsNotDrawnFromTheAllowance(ManeiDominiAugmentationRank maneiDominiRank) {
        for (AugmentedUnitType fightsOnFoot : BOTH_AUDIENCES) {
            for (int draw = 0; draw < DRAWS; draw++) {
                assertFalse(ManeiDominiImplants.selectFor(maneiDominiRank, fightsOnFoot, WORD_OF_BLAKE)
                                  .contains(OptionsConstants.MD_SUICIDE_IMPLANTS),
                      "the suicide charge must be fitted separately, not drawn as an implant");
            }
        }
    }


    /** The audience split, taken from the effects MegaMek actually gives these implants. */
    @Test
    void implantsAreMatchedToHowTheWarriorFights() {
        assertTrue(ManeiDominiImplants.servesWarrior(OptionsConstants.MD_VDNI, AugmentedUnitType.BATTLE_MEK),
              "a neural interface is what lets a warrior drive the unit they sit in");
        assertFalse(ManeiDominiImplants.servesWarrior(OptionsConstants.MD_VDNI, AugmentedUnitType.CONVENTIONAL_INFANTRY));

        assertTrue(ManeiDominiImplants.servesWarrior(OptionsConstants.MD_DERMAL_ARMOR, AugmentedUnitType.CONVENTIONAL_INFANTRY),
              "dermal armour is read only by the infantry and BattleArmor calculators");
        assertFalse(ManeiDominiImplants.servesWarrior(OptionsConstants.MD_DERMAL_ARMOR, AugmentedUnitType.BATTLE_MEK));

        assertTrue(ManeiDominiImplants.servesWarrior(OptionsConstants.MD_GAS_EFFUSER_TOXIN, AugmentedUnitType.CONVENTIONAL_INFANTRY),
              "the effusers are conventional infantry only");
        assertFalse(ManeiDominiImplants.servesWarrior(OptionsConstants.MD_GAS_EFFUSER_TOXIN, AugmentedUnitType.BATTLE_MEK));

        for (AugmentedUnitType fightsOnFoot : BOTH_AUDIENCES) {
            assertTrue(ManeiDominiImplants.servesWarrior(OptionsConstants.MD_PAIN_SHUNT, fightsOnFoot),
                  "a pain shunt serves whoever carries it");
        }
    }

    /**
     * Powered flight wings carry every glider benefit and the game forbids the pair outright, so a
     * warrior gets one or the other.
     */
    @ParameterizedTest
    @EnumSource(ManeiDominiAugmentationRank.class)
    void gliderAndPoweredFlightWingsAreNeverBothIssued(ManeiDominiAugmentationRank maneiDominiRank) {
        for (AugmentedUnitType fightsOnFoot : BOTH_AUDIENCES) {
            for (int draw = 0; draw < DRAWS; draw++) {
                List<String> issued = ManeiDominiImplants.selectFor(maneiDominiRank, fightsOnFoot, WORD_OF_BLAKE);
                assertFalse(issued.contains(OptionsConstants.MD_PL_GLIDER)
                            && issued.contains(OptionsConstants.MD_PL_FLIGHT),
                      "both sets of wings issued: " + issued);
            }
        }
    }

    /**
     * The limb prosthetics the chart does not list are placed at level 3 by inference, and serve only
     * a warrior fighting on foot.
     */
    @Test
    void theExtraLimbProstheticsServeOnlyFootTroopers() {
        for (String prosthetic : List.of(OptionsConstants.MD_PL_EXTRA_LIMBS,
              OptionsConstants.MD_PL_TAIL, OptionsConstants.MD_PL_GLIDER,
              OptionsConstants.MD_PL_FLIGHT)) {
            assertEquals(3, ManeiDominiImplants.levelOf(prosthetic), prosthetic + " sits at level 3");
            assertTrue(ManeiDominiImplants.servesWarrior(prosthetic, AugmentedUnitType.CONVENTIONAL_INFANTRY), prosthetic + " serves infantry");
            assertFalse(ManeiDominiImplants.servesWarrior(prosthetic, AugmentedUnitType.BATTLE_MEK),
                  prosthetic + " does nothing for a warrior in a cockpit");
        }
    }

    /**
     * The triple-core processor states outright that it requires a VDNI or a buffered VDNI, so it is
     * never issued alone - the same rule the multi-modal implants follow.
     */
    @ParameterizedTest
    @EnumSource(ManeiDominiAugmentationRank.class)
    void theTripleCoreProcessorAlwaysComesWithANeuralInterface(
          ManeiDominiAugmentationRank maneiDominiRank) {
        for (int draw = 0; draw < DRAWS; draw++) {
            List<String> issued = ManeiDominiImplants.selectFor(maneiDominiRank, AugmentedUnitType.BATTLE_MEK, WORD_OF_BLAKE);
            if (!issued.contains(OptionsConstants.MD_TRIPLE_CORE_PROCESSOR)) {
                continue;
            }
            assertTrue(issued.contains(OptionsConstants.MD_VDNI)
                        || issued.contains(OptionsConstants.MD_BVDNI),
                  maneiDominiRank + " got a triple-core processor with nothing to run it through: "
                        + issued);
        }
    }

    /**
     * The processor's work - initiative, aimed shots, shutdown avoidance - is all done through the
     * unit, so it serves a warrior in a cockpit. The camouflage varies the dermal armour and is read
     * only for infantry.
     */
    @Test
    void theInferredLevelFourEntriesServeWhoTheyShould() {
        assertEquals(4, ManeiDominiImplants.levelOf(OptionsConstants.MD_TRIPLE_CORE_PROCESSOR));
        assertTrue(ManeiDominiImplants.servesWarrior(OptionsConstants.MD_TRIPLE_CORE_PROCESSOR, AugmentedUnitType.BATTLE_MEK),
              "the processor serves a warrior in a cockpit");
        assertFalse(ManeiDominiImplants.servesWarrior(OptionsConstants.MD_TRIPLE_CORE_PROCESSOR, AugmentedUnitType.CONVENTIONAL_INFANTRY));

        assertEquals(4, ManeiDominiImplants.levelOf(OptionsConstants.MD_DERMAL_CAMO_ARMOR));
        assertTrue(ManeiDominiImplants.servesWarrior(OptionsConstants.MD_DERMAL_CAMO_ARMOR, AugmentedUnitType.CONVENTIONAL_INFANTRY),
              "the camouflage serves a foot trooper");
        assertFalse(ManeiDominiImplants.servesWarrior(OptionsConstants.MD_DERMAL_CAMO_ARMOR, AugmentedUnitType.BATTLE_MEK));
    }

    /**
     * The construction rules give the extraneous limbs, the tail and both sets of wings to the
     * Capellan Confederation alone - they are Thuggee Phansigar kit, not Manei Domini. A Word of Blake
     * warrior must never be issued them.
     */
    @ParameterizedTest
    @EnumSource(ManeiDominiAugmentationRank.class)
    void theCapellanProstheticsNeverReachTheWordOfBlake(ManeiDominiAugmentationRank maneiDominiRank) {
        for (int draw = 0; draw < DRAWS; draw++) {
            List<String> issued = ManeiDominiImplants.selectFor(maneiDominiRank, AugmentedUnitType.CONVENTIONAL_INFANTRY, WORD_OF_BLAKE);
            for (String capellanOnly : List.of(OptionsConstants.MD_PL_EXTRA_LIMBS,
                  OptionsConstants.MD_PL_TAIL, OptionsConstants.MD_PL_GLIDER,
                  OptionsConstants.MD_PL_FLIGHT)) {
                assertFalse(issued.contains(capellanOnly),
                      "the Word of Blake was issued Capellan kit: " + capellanOnly);
            }
        }
    }

    /** A Capellan force does field them, which is the point of restricting rather than removing them. */
    @Test
    void theCapellanProstheticsReachACapellanForce() {
        boolean everIssued = false;
        for (int draw = 0; draw < DRAWS; draw++) {
            List<String> issued =
                  ManeiDominiImplants.selectFor(ManeiDominiAugmentationRank.OMICRON, AugmentedUnitType.CONVENTIONAL_INFANTRY, "CC");
            everIssued |= issued.contains(OptionsConstants.MD_PL_TAIL)
                              || issued.contains(OptionsConstants.MD_PL_EXTRA_LIMBS)
                              || issued.contains(OptionsConstants.MD_PL_GLIDER)
                              || issued.contains(OptionsConstants.MD_PL_FLIGHT);
        }
        assertTrue(everIssued, "a Capellan force must be able to draw its own prosthetics");
    }

    /** Unrestricted entries are issued whatever the faction, including when none is known. */
    @Test
    void unrestrictedImplantsAreIssuedToAnyone() {
        for (String faction : new String[] { "WOB.SD", "CC", "FS", null }) {
            boolean everIssued = false;
            for (int draw = 0; draw < DRAWS; draw++) {
                everIssued |= ManeiDominiImplants
                                    .selectFor(ManeiDominiAugmentationRank.OMICRON, AugmentedUnitType.BATTLE_MEK, faction)
                                    .contains(OptionsConstants.MD_PAIN_SHUNT);
            }
            assertTrue(everIssued, "the pain shunt is unrestricted and must reach " + faction);
        }
    }

    /**
     * The construction rules put battle armour on the neural interface's unit list and leave
     * conventional infantry off it - a suit is a machine to interface with, a foot trooper has none.
     * Reading both as simply "on foot" denied battle armour the interface, and with it the multi-modal
     * implants and the processor that need one.
     */
    @Test
    void battleArmourMayCarryANeuralInterfaceAndFootInfantryMayNot() {
        assertTrue(ManeiDominiImplants.servesWarrior(OptionsConstants.MD_VDNI,
                    AugmentedUnitType.BATTLE_ARMOR),
              "a battle armour trooper interfaces with their suit");
        assertFalse(ManeiDominiImplants.servesWarrior(OptionsConstants.MD_VDNI,
                    AugmentedUnitType.CONVENTIONAL_INFANTRY),
              "a foot trooper has no machine to interface with");

        boolean everIssued = false;
        for (int draw = 0; draw < DRAWS; draw++) {
            List<String> issued = ManeiDominiImplants.selectFor(ManeiDominiAugmentationRank.OMICRON,
                  AugmentedUnitType.BATTLE_ARMOR, WORD_OF_BLAKE);
            assertFalse(issued.isEmpty());
            everIssued |= issued.contains(OptionsConstants.MD_VDNI)
                              || issued.contains(OptionsConstants.MD_BVDNI);
        }
        assertTrue(everIssued, "battle armour must actually be issued an interface");
    }

    /** Battle armour keeps the implants that act on the trooper's own body as well. */
    @Test
    void battleArmourStillGetsTheImplantsThatActOnTheBody() {
        assertTrue(ManeiDominiImplants.servesWarrior(OptionsConstants.MD_DERMAL_ARMOR,
                    AugmentedUnitType.BATTLE_ARMOR),
              "the infantry calculators read dermal armour for battle armour too");
    }
}

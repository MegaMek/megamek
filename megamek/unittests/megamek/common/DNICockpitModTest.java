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
package megamek.common;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import megamek.common.equipment.EquipmentType;
import megamek.common.equipment.MiscMounted;
import megamek.common.equipment.MiscType;
import megamek.common.game.Game;
import megamek.common.options.OptionsConstants;
import megamek.common.rolls.PilotingRollData;
import megamek.common.units.BipedMek;
import megamek.common.units.Crew;
import megamek.common.units.CrewType;
import megamek.common.units.Entity;
import megamek.common.units.Mek;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Tests for Direct Neural Interface (DNI) Cockpit Modification functionality per IO p.83.
 * <p>
 * Rules Summary:
 * </p>
 * <ul>
 *     <li>DNI Cockpit Mod: 0 weight, 0 crits, +250,000 C-bills</li>
 *     <li>Available to: BM, IM, BA, CV, SV, AF, CF</li>
 *     <li>Required for DNI-implanted pilots to use their benefits (when tracking hardware)</li>
 *     <li>Without DNI mod: Pilot operates manually/neurohelmet only</li>
 *     <li>Hard to Pilot: Adds +1 PSR for pilots WITHOUT compatible implants</li>
 *     <li>Compatible Implants: VDNI, BVDNI, Proto DNI</li>
 * </ul>
 */
public class DNICockpitModTest {

    private Game game;

    @BeforeAll
    static void initializeEquipment() {
        EquipmentType.initializeTypes();
    }

    @BeforeEach
    void setUp() {
        game = new Game();
        game.addPlayer(0, new Player(0, "Test Player"));
    }

    /**
     * Creates a BipedMek for testing with optional DNI cockpit mod and pilot implant.
     */
    private Mek createMek(boolean withDNICockpitMod, String implantType) {
        BipedMek mek = new BipedMek();
        mek.setGame(game);
        mek.setId(1);
        mek.setChassis("Test Mek");
        mek.setModel("Standard");
        mek.setWeight(50);

        // Initialize crew
        Crew crew = new Crew(CrewType.SINGLE);
        mek.setCrew(crew);

        // Set pilot implant if specified
        if (implantType != null) {
            switch (implantType) {
                case "VDNI":
                    crew.getOptions().getOption(OptionsConstants.MD_VDNI).setValue(true);
                    break;
                case "BVDNI":
                    crew.getOptions().getOption(OptionsConstants.MD_VDNI).setValue(true);
                    crew.getOptions().getOption(OptionsConstants.MD_BVDNI).setValue(true);
                    break;
                case "PROTO_DNI":
                    crew.getOptions().getOption(OptionsConstants.MD_PROTO_DNI).setValue(true);
                    break;
                case "EI":
                    crew.getOptions().getOption(OptionsConstants.MD_EI_IMPLANT).setValue(true);
                    break;
                default:
                    break;
            }
        }

        mek.setOwner(game.getPlayer(0));
        mek.autoSetInternal();

        // Add DNI cockpit modification if requested
        if (withDNICockpitMod) {
            try {
                MiscType dniMod = (MiscType) EquipmentType.get("DNICockpitModification");
                if (dniMod != null) {
                    mek.addEquipment(dniMod, Entity.LOC_NONE);
                }
            } catch (Exception e) {
                // Equipment addition failed
            }
        }

        return mek;
    }

    /**
     * Sets the game option for tracking neural interface hardware.
     */
    private void setTrackingOption(boolean enabled) {
        game.getOptions().getOption(OptionsConstants.ADVANCED_TRACK_NEURAL_INTERFACE_HARDWARE).setValue(enabled);
    }

    /**
     * Enables the Hard to Pilot quirk on an entity. Also enables the stratops_quirks game option which is required for
     * quirks to take effect.
     */
    private void enableHardToPilotQuirk(Mek mek) {
        game.getOptions().getOption(OptionsConstants.ADVANCED_STRATOPS_QUIRKS).setValue(true);
        mek.getQuirks().getOption(OptionsConstants.QUIRK_NEG_HARD_PILOT).setValue(true);
    }

    /**
     * Checks if a piloting roll contains a specific modifier description.
     */
    private boolean hasModifier(PilotingRollData roll, String modifierDesc) {
        return roll.getDesc().contains(modifierDesc);
    }

    /**
     * Adds EI cockpit equipment to a mek and sets it to "On" mode. EI Interface has modes ["Off", "Initiate enhanced
     * imaging"] where index 0 is Off. Since EI Interface uses non-instant mode switching, we need to call newRound to
     * apply the mode.
     */
    private void addEiCockpit(Mek mek) {
        try {
            MiscType eiInterface = (MiscType) EquipmentType.get("EIInterface");
            if (eiInterface != null) {
                mek.addEquipment(eiInterface, Entity.LOC_NONE);
                // Set to mode 1 ("On") - mode 0 is "Off"
                for (MiscMounted miscMounted : mek.getMisc()) {
                    if (miscMounted.getType().hasFlag(MiscType.F_EI_INTERFACE)) {
                        miscMounted.setMode(1);
                        // Mode switch is not instant, so call newRound to apply the pending mode
                        miscMounted.newRound(1);
                        break;
                    }
                }
            }
        } catch (Exception e) {
            // Equipment addition failed
        }
    }

    @Nested
    @DisplayName("Equipment Definition Tests")
    class EquipmentDefinitionTests {

        @Test
        @DisplayName("DNI Cockpit Mod equipment exists")
        void dniCockpitModEquipmentExists() {
            EquipmentType dniMod = EquipmentType.get("DNICockpitModification");
            assertNotNull(dniMod, "DNI Cockpit Mod equipment should exist");
        }

        @Test
        @DisplayName("DNI Cockpit Mod has F_DNI_COCKPIT_MOD flag")
        void dniCockpitModHasCorrectFlag() {
            MiscType dniMod = (MiscType) EquipmentType.get("DNICockpitModification");
            assertTrue(dniMod.hasFlag(MiscType.F_DNI_COCKPIT_MOD),
                  "DNI Cockpit Mod should have F_DNI_COCKPIT_MOD flag");
        }

        @Test
        @DisplayName("DNI Cockpit Mod costs 250,000 C-bills")
        void dniCockpitModCostIsCorrect() {
            MiscType dniMod = (MiscType) EquipmentType.get("DNICockpitModification");
            assertEquals(250000, dniMod.getCost(null, false, Entity.LOC_NONE),
                  "DNI Cockpit Mod should cost 250,000 C-bills");
        }

        @Test
        @DisplayName("EI Interface equipment exists")
        void eiInterfaceEquipmentExists() {
            EquipmentType eiInterface = EquipmentType.get("EIInterface");
            assertNotNull(eiInterface, "EI Interface equipment should exist");
        }

        @Test
        @DisplayName("EI Interface has F_EI_INTERFACE flag")
        void eiInterfaceHasCorrectFlag() {
            MiscType eiInterface = (MiscType) EquipmentType.get("EIInterface");
            assertNotNull(eiInterface, "EI Interface should exist");
            assertTrue(eiInterface.hasFlag(MiscType.F_EI_INTERFACE),
                  "EI Interface should have F_EI_INTERFACE flag");
        }

        @Test
        @DisplayName("EI Interface can be added to mek and detected")
        void eiInterfaceCanBeAddedToMek() {
            Mek mek = createMek(false, null);
            assertFalse(mek.hasEiCockpit(), "Mek should not have EI cockpit initially");

            addEiCockpit(mek);
            assertTrue(mek.hasEiCockpit(), "Mek should have EI cockpit after adding");
        }

        @Test
        @DisplayName("DNI Cockpit Mod has zero tonnage")
        void dniCockpitModHasZeroTonnage() {
            MiscType dniMod = (MiscType) EquipmentType.get("DNICockpitModification");
            assertEquals(0.0, dniMod.getTonnage(null), 0.001,
                  "DNI Cockpit Mod should have zero tonnage");
        }

        @Test
        @DisplayName("DNI Cockpit Mod is available for Meks")
        void dniCockpitModAvailableForMeks() {
            MiscType dniMod = (MiscType) EquipmentType.get("DNICockpitModification");
            assertTrue(dniMod.hasFlag(MiscType.F_MEK_EQUIPMENT),
                  "DNI Cockpit Mod should be available for Meks");
        }

        @Test
        @DisplayName("DNI Cockpit Mod is available for Tanks")
        void dniCockpitModAvailableForTanks() {
            MiscType dniMod = (MiscType) EquipmentType.get("DNICockpitModification");
            assertTrue(dniMod.hasFlag(MiscType.F_TANK_EQUIPMENT),
                  "DNI Cockpit Mod should be available for Tanks");
        }

        @Test
        @DisplayName("DNI Cockpit Mod is available for Fighters")
        void dniCockpitModAvailableForFighters() {
            MiscType dniMod = (MiscType) EquipmentType.get("DNICockpitModification");
            assertTrue(dniMod.hasFlag(MiscType.F_FIGHTER_EQUIPMENT),
                  "DNI Cockpit Mod should be available for Fighters");
        }
    }

    @Nested
    @DisplayName("hasDNICockpitMod() Tests")
    class HasDNICockpitModTests {

        @Test
        @DisplayName("Mek with DNI cockpit mod returns true")
        void mekWithDniCockpitModReturnsTrue() {
            Mek mek = createMek(true, null);
            assertTrue(mek.hasDNICockpitMod(),
                  "Mek with DNI cockpit mod should return true for hasDNICockpitMod()");
        }

        @Test
        @DisplayName("Mek without DNI cockpit mod returns false")
        void mekWithoutDniCockpitModReturnsFalse() {
            Mek mek = createMek(false, null);
            assertFalse(mek.hasDNICockpitMod(),
                  "Mek without DNI cockpit mod should return false for hasDNICockpitMod()");
        }
    }

    @Nested
    @DisplayName("hasDNIImplant() Tests")
    class HasDNIImplantTests {

        @Test
        @DisplayName("Pilot with VDNI returns true")
        void pilotWithVdniReturnsTrue() {
            Mek mek = createMek(false, "VDNI");
            assertTrue(mek.hasDNIImplant(),
                  "Pilot with VDNI should return true for hasDNIImplant()");
        }

        @Test
        @DisplayName("Pilot with BVDNI returns true")
        void pilotWithBvdniReturnsTrue() {
            Mek mek = createMek(false, "BVDNI");
            assertTrue(mek.hasDNIImplant(),
                  "Pilot with BVDNI should return true for hasDNIImplant()");
        }

        @Test
        @DisplayName("Pilot with Proto DNI returns true")
        void pilotWithProtoDniReturnsTrue() {
            Mek mek = createMek(false, "PROTO_DNI");
            assertTrue(mek.hasDNIImplant(),
                  "Pilot with Proto DNI should return true for hasDNIImplant()");
        }

        @Test
        @DisplayName("Pilot without DNI implant returns false")
        void pilotWithoutDniImplantReturnsFalse() {
            Mek mek = createMek(false, null);
            assertFalse(mek.hasDNIImplant(),
                  "Pilot without DNI implant should return false for hasDNIImplant()");
        }

        @Test
        @DisplayName("Pilot with EI implant (not DNI) returns false")
        void pilotWithEiImplantReturnsFalse() {
            Mek mek = createMek(false, "EI");
            assertFalse(mek.hasDNIImplant(),
                  "Pilot with EI implant (not DNI) should return false for hasDNIImplant()");
        }
    }

    @Nested
    @DisplayName("hasActiveDNI() - Tracking OFF (Default)")
    class HasActiveDNITrackingOffTests {

        @BeforeEach
        void disableTracking() {
            setTrackingOption(false);
        }

        @Test
        @DisplayName("VDNI pilot without equipment gets benefits (tracking OFF)")
        void vdniPilotWithoutEquipmentGetsBenefits() {
            Mek mek = createMek(false, "VDNI");
            assertTrue(mek.hasActiveDNI(),
                  "VDNI pilot should get DNI benefits without equipment when tracking is OFF");
        }

        @Test
        @DisplayName("VDNI pilot with equipment gets benefits (tracking OFF)")
        void vdniPilotWithEquipmentGetsBenefits() {
            Mek mek = createMek(true, "VDNI");
            assertTrue(mek.hasActiveDNI(),
                  "VDNI pilot should get DNI benefits with equipment when tracking is OFF");
        }

        @Test
        @DisplayName("Pilot without implant has no active DNI (tracking OFF)")
        void pilotWithoutImplantNoActiveDni() {
            Mek mek = createMek(true, null);
            assertFalse(mek.hasActiveDNI(),
                  "Pilot without implant should not have active DNI even with equipment");
        }
    }

    @Nested
    @DisplayName("hasActiveDNI() - Tracking ON")
    class HasActiveDNITrackingOnTests {

        @BeforeEach
        void enableTracking() {
            setTrackingOption(true);
        }

        @Test
        @DisplayName("VDNI pilot with equipment gets benefits (tracking ON)")
        void vdniPilotWithEquipmentGetsBenefits() {
            Mek mek = createMek(true, "VDNI");
            assertTrue(mek.hasActiveDNI(),
                  "VDNI pilot with equipment should get DNI benefits when tracking is ON");
        }

        @Test
        @DisplayName("VDNI pilot without equipment does NOT get benefits (tracking ON)")
        void vdniPilotWithoutEquipmentNoBenefits() {
            Mek mek = createMek(false, "VDNI");
            assertFalse(mek.hasActiveDNI(),
                  "VDNI pilot without equipment should NOT get DNI benefits when tracking is ON");
        }

        @Test
        @DisplayName("Equipment without implant does NOT provide benefits (tracking ON)")
        void equipmentWithoutImplantNoBenefits() {
            Mek mek = createMek(true, null);
            assertFalse(mek.hasActiveDNI(),
                  "Equipment without DNI implant should NOT provide benefits");
        }

        @Test
        @DisplayName("BVDNI pilot with equipment gets benefits (tracking ON)")
        void bvdniPilotWithEquipmentGetsBenefits() {
            Mek mek = createMek(true, "BVDNI");
            assertTrue(mek.hasActiveDNI(),
                  "BVDNI pilot with equipment should get DNI benefits when tracking is ON");
        }

        @Test
        @DisplayName("Proto DNI pilot with equipment gets benefits (tracking ON)")
        void protoDniPilotWithEquipmentGetsBenefits() {
            Mek mek = createMek(true, "PROTO_DNI");
            assertTrue(mek.hasActiveDNI(),
                  "Proto DNI pilot with equipment should get DNI benefits when tracking is ON");
        }

        @Test
        @DisplayName("BVDNI pilot without equipment does NOT get benefits (tracking ON)")
        void bvdniPilotWithoutEquipmentNoBenefits() {
            Mek mek = createMek(false, "BVDNI");
            assertFalse(mek.hasActiveDNI(),
                  "BVDNI pilot without equipment should NOT get DNI benefits when tracking is ON");
        }
    }

    @Nested
    @DisplayName("hasActiveEiCockpit() Tests")
    class HasActiveEiCockpitTests {

        @Test
        @DisplayName("Tracking OFF: EI cockpit alone is sufficient")
        void trackingOff_eiCockpitAloneSufficient() {
            setTrackingOption(false);

            // Mek with EI cockpit but no EI implant
            Mek mek = createMek(false, null);
            addEiCockpit(mek);

            assertTrue(mek.hasActiveEiCockpit(),
                  "EI cockpit alone should provide benefits when tracking is OFF");
        }

        @Test
        @DisplayName("Tracking OFF: No EI cockpit means no benefits")
        void trackingOff_noEiCockpitNoBenefits() {
            setTrackingOption(false);

            // Mek with EI implant but no EI cockpit
            Mek mek = createMek(false, "EI");

            assertFalse(mek.hasActiveEiCockpit(),
                  "Without EI cockpit, should NOT have active EI even with implant");
        }

        @Test
        @DisplayName("Tracking ON: EI cockpit + implant required")
        void trackingOn_eiCockpitAndImplantRequired() {
            setTrackingOption(true);

            // Mek with both EI cockpit and EI implant
            Mek mek = createMek(false, "EI");
            addEiCockpit(mek);

            assertTrue(mek.hasActiveEiCockpit(),
                  "EI cockpit + implant should provide benefits when tracking is ON");
        }

        @Test
        @DisplayName("Tracking ON: EI cockpit without implant fails")
        void trackingOn_eiCockpitWithoutImplantFails() {
            setTrackingOption(true);

            // Mek with EI cockpit but no EI implant
            Mek mek = createMek(false, null);
            addEiCockpit(mek);

            assertFalse(mek.hasActiveEiCockpit(),
                  "EI cockpit without implant should NOT have active EI when tracking is ON");
        }

        @Test
        @DisplayName("Tracking ON: EI implant without cockpit fails")
        void trackingOn_eiImplantWithoutCockpitFails() {
            setTrackingOption(true);

            // Mek with EI implant but no EI cockpit
            Mek mek = createMek(false, "EI");

            assertFalse(mek.hasActiveEiCockpit(),
                  "EI implant without cockpit should NOT have active EI when tracking is ON");
        }
    }

    @Nested
    @DisplayName("Game Option Tests")
    class GameOptionTests {

        @Test
        @DisplayName("Track Neural Interface Hardware option exists")
        void trackNeuralInterfaceHardwareOptionExists() {
            assertNotNull(game.getOptions().getOption(OptionsConstants.ADVANCED_TRACK_NEURAL_INTERFACE_HARDWARE),
                  "Track Neural Interface Hardware game option should exist");
        }

        @Test
        @DisplayName("Track Neural Interface Hardware option defaults to false")
        void trackNeuralInterfaceHardwareDefaultsFalse() {
            Game freshGame = new Game();
            assertFalse(freshGame.getOptions().booleanOption(OptionsConstants.ADVANCED_TRACK_NEURAL_INTERFACE_HARDWARE),
                  "Track Neural Interface Hardware option should default to false");
        }
    }

    @Nested
    @DisplayName("Hard to Pilot Quirk - Tracking OFF")
    class HardToPilotTrackingOffTests {

        @BeforeEach
        void disableTracking() {
            setTrackingOption(false);
        }

        @Test
        @DisplayName("No implant - HTP quirk applies")
        void noImplant_htpQuirkApplies() {
            Mek mek = createMek(false, null);
            enableHardToPilotQuirk(mek);

            PilotingRollData roll = mek.getBasePilotingRoll();

            assertTrue(hasModifier(roll, "hard to pilot"),
                  "Without implant, Hard to Pilot quirk should apply");
        }

        @Test
        @DisplayName("With VDNI - HTP quirk ignored (implant implies hardware)")
        void withVdni_htpQuirkIgnored() {
            Mek mek = createMek(false, "VDNI");
            enableHardToPilotQuirk(mek);

            PilotingRollData roll = mek.getBasePilotingRoll();

            assertFalse(hasModifier(roll, "hard to pilot"),
                  "With VDNI implant (tracking OFF), Hard to Pilot quirk should be ignored");
        }
    }

    @Nested
    @DisplayName("Hard to Pilot Quirk - Tracking ON")
    class HardToPilotTrackingOnTests {

        @BeforeEach
        void enableTracking() {
            setTrackingOption(true);
        }

        @Test
        @DisplayName("No implant, no DNI - HTP quirk applies")
        void noImplant_noDni_htpQuirkApplies() {
            Mek mek = createMek(false, null);
            enableHardToPilotQuirk(mek);

            PilotingRollData roll = mek.getBasePilotingRoll();

            assertTrue(hasModifier(roll, "hard to pilot"),
                  "Without implant or DNI, Hard to Pilot quirk should apply");
        }

        @Test
        @DisplayName("VDNI without DNI - HTP quirk applies (no hardware = no exemption)")
        void vdniWithoutDni_htpQuirkApplies() {
            Mek mek = createMek(false, "VDNI");
            enableHardToPilotQuirk(mek);

            PilotingRollData roll = mek.getBasePilotingRoll();

            assertTrue(hasModifier(roll, "hard to pilot"),
                  "VDNI without DNI hardware should NOT ignore Hard to Pilot quirk");
        }

        @Test
        @DisplayName("No implant with DNI - gains Hard to Pilot quirk (IO p.83)")
        void noImplant_withDni_gainsHardToPilotQuirk() {
            // DNI cockpit induces Hard to Pilot quirk when Design Quirks rules are in use (IO p.83)
            game.getOptions().getOption(OptionsConstants.ADVANCED_STRATOPS_QUIRKS).setValue(true);
            Mek mek = createMek(true, null);

            // Should now have the Hard to Pilot quirk via DNI
            assertTrue(mek.hasQuirk(OptionsConstants.QUIRK_NEG_HARD_PILOT),
                  "DNI without implant should induce Hard to Pilot quirk");

            PilotingRollData roll = mek.getBasePilotingRoll();
            assertTrue(hasModifier(roll, "hard to pilot"),
                  "DNI without implant should apply 'hard to pilot' modifier");
        }

        @Test
        @DisplayName("No implant with DNI - no quirk when quirks OFF")
        void noImplant_withDni_noQuirkWhenQuirksOff() {
            // DNI-induced quirk should NOT apply when Design Quirks rules are off
            game.getOptions().getOption(OptionsConstants.ADVANCED_STRATOPS_QUIRKS).setValue(false);
            Mek mek = createMek(true, null);

            assertFalse(mek.hasQuirk(OptionsConstants.QUIRK_NEG_HARD_PILOT),
                  "DNI without implant should NOT have quirk when quirks disabled");

            PilotingRollData roll = mek.getBasePilotingRoll();
            assertFalse(hasModifier(roll, "hard to pilot"),
                  "DNI without implant should NOT apply penalty when quirks disabled");
        }

        @Test
        @DisplayName("No implant with DNI and existing HTP quirk - no double penalty")
        void noImplant_withDni_htpQuirk_noDoublePenalty() {
            Mek mek = createMek(true, null);
            enableHardToPilotQuirk(mek);

            PilotingRollData roll = mek.getBasePilotingRoll();

            // Should only have ONE "hard to pilot" modifier, not two
            assertTrue(hasModifier(roll, "hard to pilot"),
                  "Should show 'hard to pilot'");
            // Count occurrences - should only appear once
            String desc = roll.getDesc();
            int count = desc.split("hard to pilot", -1).length - 1;
            assertEquals(1, count, "Should only have one 'hard to pilot' modifier, not stacked");
        }

        @Test
        @DisplayName("VDNI with DNI - HTP quirk ignored")
        void vdniWithDni_htpQuirkIgnored() {
            Mek mek = createMek(true, "VDNI");
            enableHardToPilotQuirk(mek);

            PilotingRollData roll = mek.getBasePilotingRoll();

            assertFalse(hasModifier(roll, "hard to pilot"),
                  "VDNI with DNI hardware should ignore Hard to Pilot quirk");
            assertFalse(hasModifier(roll, "DNI cockpit (no implant)"),
                  "VDNI with DNI hardware should not have DNI cockpit penalty");
        }

        @Test
        @DisplayName("BVDNI with DNI - HTP quirk ignored")
        void bvdniWithDni_htpQuirkIgnored() {
            Mek mek = createMek(true, "BVDNI");
            enableHardToPilotQuirk(mek);

            PilotingRollData roll = mek.getBasePilotingRoll();

            assertFalse(hasModifier(roll, "hard to pilot"),
                  "BVDNI with DNI hardware should ignore Hard to Pilot quirk");
        }
    }

    @Nested
    @DisplayName("isNeuralInterfaceActive() Helper Tests")
    class NeuralInterfaceActiveHelperTests {

        @Test
        @DisplayName("No implant returns false regardless of hardware")
        void noImplant_returnsFalse() {
            setTrackingOption(true);
            Mek mekWithHardware = createMek(true, null);
            Mek mekWithoutHardware = createMek(false, null);

            assertFalse(mekWithHardware.hasActiveDNI(),
                  "No implant should return false even with hardware");
            assertFalse(mekWithoutHardware.hasActiveDNI(),
                  "No implant should return false without hardware");
        }

        @Test
        @DisplayName("Tracking OFF - implant alone returns true")
        void trackingOff_implantAlone_returnsTrue() {
            setTrackingOption(false);
            Mek mek = createMek(false, "VDNI");

            assertTrue(mek.hasActiveDNI(),
                  "With tracking OFF, implant alone should return true");
        }

        @Test
        @DisplayName("Tracking ON - implant without hardware returns false")
        void trackingOn_implantWithoutHardware_returnsFalse() {
            setTrackingOption(true);
            Mek mek = createMek(false, "VDNI");

            assertFalse(mek.hasActiveDNI(),
                  "With tracking ON, implant without hardware should return false");
        }

        @Test
        @DisplayName("Tracking ON - implant with hardware returns true")
        void trackingOn_implantWithHardware_returnsTrue() {
            setTrackingOption(true);
            Mek mek = createMek(true, "VDNI");

            assertTrue(mek.hasActiveDNI(),
                  "With tracking ON, implant with hardware should return true");
        }
    }

    @Nested
    @DisplayName("hasDNIInducedHardToPilot() Tests")
    class HasDNIInducedHardToPilotTests {

        @Test
        @DisplayName("Tracking OFF - never induces quirk")
        void trackingOff_neverInducesQuirk() {
            setTrackingOption(false);
            game.getOptions().getOption(OptionsConstants.ADVANCED_STRATOPS_QUIRKS).setValue(true);
            Mek mek = createMek(true, null); // DNI hardware, no implant

            assertFalse(mek.hasDNIInducedHardToPilot(),
                  "Should not induce HTP quirk when tracking is OFF");
        }

        @Test
        @DisplayName("Tracking ON, no DNI hardware - no quirk")
        void trackingOn_noDniHardware_noQuirk() {
            setTrackingOption(true);
            game.getOptions().getOption(OptionsConstants.ADVANCED_STRATOPS_QUIRKS).setValue(true);
            Mek mek = createMek(false, null); // No DNI hardware

            assertFalse(mek.hasDNIInducedHardToPilot(),
                  "Should not induce HTP quirk without DNI hardware");
        }

        @Test
        @DisplayName("Tracking ON, DNI hardware, no implant - induces quirk")
        void trackingOn_dniHardware_noImplant_inducesQuirk() {
            setTrackingOption(true);
            game.getOptions().getOption(OptionsConstants.ADVANCED_STRATOPS_QUIRKS).setValue(true);
            Mek mek = createMek(true, null); // DNI hardware, no implant

            assertTrue(mek.hasDNIInducedHardToPilot(),
                  "Should induce HTP quirk with DNI hardware but no implant");
        }

        @Test
        @DisplayName("Tracking ON, DNI hardware, with VDNI - no quirk")
        void trackingOn_dniHardware_withVdni_noQuirk() {
            setTrackingOption(true);
            game.getOptions().getOption(OptionsConstants.ADVANCED_STRATOPS_QUIRKS).setValue(true);
            Mek mek = createMek(true, "VDNI"); // DNI hardware + VDNI implant

            assertFalse(mek.hasDNIInducedHardToPilot(),
                  "Should not induce HTP quirk with compatible implant");
        }

        @Test
        @DisplayName("Tracking ON, DNI hardware, with BVDNI - no quirk")
        void trackingOn_dniHardware_withBvdni_noQuirk() {
            setTrackingOption(true);
            game.getOptions().getOption(OptionsConstants.ADVANCED_STRATOPS_QUIRKS).setValue(true);
            Mek mek = createMek(true, "BVDNI"); // DNI hardware + BVDNI implant

            assertFalse(mek.hasDNIInducedHardToPilot(),
                  "Should not induce HTP quirk with BVDNI implant");
        }

        @Test
        @DisplayName("Tracking ON, DNI hardware, with Proto DNI - no quirk")
        void trackingOn_dniHardware_withProtoDni_noQuirk() {
            setTrackingOption(true);
            game.getOptions().getOption(OptionsConstants.ADVANCED_STRATOPS_QUIRKS).setValue(true);
            Mek mek = createMek(true, "PROTO_DNI"); // DNI hardware + Proto DNI implant

            assertFalse(mek.hasDNIInducedHardToPilot(),
                  "Should not induce HTP quirk with Proto DNI implant");
        }

        @Test
        @DisplayName("Tracking ON, DNI hardware, with EI implant (wrong type) - induces quirk")
        void trackingOn_dniHardware_withEiImplant_inducesQuirk() {
            setTrackingOption(true);
            game.getOptions().getOption(OptionsConstants.ADVANCED_STRATOPS_QUIRKS).setValue(true);
            Mek mek = createMek(true, "EI"); // DNI hardware + EI implant (not compatible)

            assertTrue(mek.hasDNIInducedHardToPilot(),
                  "EI implant is not compatible with DNI - should still induce HTP quirk");
        }
    }

    @Nested
    @DisplayName("Crew Option Update Tests - Simulates Dialog Behavior")
    class CrewOptionUpdateTests {

        /**
         * Simulates what CustomMekDialog.optionClicked() does when VDNI is selected: directly updating the entity's
         * crew options.
         */
        private void setCrewOption(Mek mek, String optionName, boolean value) {
            mek.getCrew().getOptions().getOption(optionName).setValue(value);
        }

        @Test
        @DisplayName("Tracking ON: Setting VDNI option updates hasDNIImplant()")
        void trackingOn_settingVdniUpdatesHasDniImplant() {
            setTrackingOption(true);
            Mek mek = createMek(false, null);

            // Initially no implant
            assertFalse(mek.hasDNIImplant(), "Should not have DNI implant initially");

            // Simulate dialog setting VDNI (like optionClicked does)
            setCrewOption(mek, OptionsConstants.MD_VDNI, true);

            // Now should detect implant
            assertTrue(mek.hasDNIImplant(), "Should detect DNI implant after setting VDNI");
        }

        @Test
        @DisplayName("Tracking ON: Setting BVDNI option updates hasDNIImplant()")
        void trackingOn_settingBvdniUpdatesHasDniImplant() {
            setTrackingOption(true);
            Mek mek = createMek(false, null);

            assertFalse(mek.hasDNIImplant(), "Should not have DNI implant initially");

            // BVDNI requires VDNI to be set first
            setCrewOption(mek, OptionsConstants.MD_VDNI, true);
            setCrewOption(mek, OptionsConstants.MD_BVDNI, true);

            assertTrue(mek.hasDNIImplant(), "Should detect DNI implant after setting BVDNI");
        }

        @Test
        @DisplayName("Tracking ON: Setting Proto DNI option updates hasDNIImplant()")
        void trackingOn_settingProtoDniUpdatesHasDniImplant() {
            setTrackingOption(true);
            Mek mek = createMek(false, null);

            assertFalse(mek.hasDNIImplant(), "Should not have DNI implant initially");

            setCrewOption(mek, OptionsConstants.MD_PROTO_DNI, true);

            assertTrue(mek.hasDNIImplant(), "Should detect DNI implant after setting Proto DNI");
        }

        @Test
        @DisplayName("Tracking OFF: Setting VDNI option updates hasDNIImplant()")
        void trackingOff_settingVdniUpdatesHasDniImplant() {
            setTrackingOption(false);
            Mek mek = createMek(false, null);

            assertFalse(mek.hasDNIImplant(), "Should not have DNI implant initially");

            setCrewOption(mek, OptionsConstants.MD_VDNI, true);

            assertTrue(mek.hasDNIImplant(), "Should detect DNI implant after setting VDNI");
        }

        @Test
        @DisplayName("Tracking ON: VDNI set then hasActiveDNI requires hardware")
        void trackingOn_vdniSetThenHasActiveDniRequiresHardware() {
            setTrackingOption(true);
            Mek mek = createMek(false, null);

            setCrewOption(mek, OptionsConstants.MD_VDNI, true);

            // Has implant but no hardware
            assertTrue(mek.hasDNIImplant(), "Should have DNI implant");
            assertFalse(mek.hasDNICockpitMod(), "Should not have DNI hardware");
            assertFalse(mek.hasActiveDNI(), "Should NOT have active DNI without hardware");
        }

        @Test
        @DisplayName("Tracking ON: VDNI set with hardware then hasActiveDNI is true")
        void trackingOn_vdniSetWithHardwareThenHasActiveDniTrue() {
            setTrackingOption(true);
            Mek mek = createMek(true, null); // Has DNI hardware

            setCrewOption(mek, OptionsConstants.MD_VDNI, true);

            assertTrue(mek.hasDNIImplant(), "Should have DNI implant");
            assertTrue(mek.hasDNICockpitMod(), "Should have DNI hardware");
            assertTrue(mek.hasActiveDNI(), "Should have active DNI with implant AND hardware");
        }

        @Test
        @DisplayName("Tracking OFF: VDNI set then hasActiveDNI is true (no hardware needed)")
        void trackingOff_vdniSetThenHasActiveDniTrue() {
            setTrackingOption(false);
            Mek mek = createMek(false, null); // No DNI hardware

            setCrewOption(mek, OptionsConstants.MD_VDNI, true);

            assertTrue(mek.hasDNIImplant(), "Should have DNI implant");
            assertFalse(mek.hasDNICockpitMod(), "Should not have DNI hardware");
            assertTrue(mek.hasActiveDNI(), "Should have active DNI with just implant when tracking OFF");
        }

        @Test
        @DisplayName("Unsetting VDNI removes implant detection")
        void unsettingVdniRemovesImplantDetection() {
            setTrackingOption(true);
            Mek mek = createMek(false, "VDNI");

            assertTrue(mek.hasDNIImplant(), "Should have DNI implant initially");

            // Simulate dialog unchecking VDNI
            setCrewOption(mek, OptionsConstants.MD_VDNI, false);

            assertFalse(mek.hasDNIImplant(), "Should not have DNI implant after unsetting VDNI");
        }

        @Test
        @DisplayName("Tracking ON: EI implant set updates hasAbility correctly")
        void trackingOn_eiImplantSetUpdatesHasAbility() {
            setTrackingOption(true);
            Mek mek = createMek(false, null);

            assertFalse(mek.hasAbility(OptionsConstants.MD_EI_IMPLANT), "Should not have EI implant initially");

            setCrewOption(mek, OptionsConstants.MD_EI_IMPLANT, true);

            assertTrue(mek.hasAbility(OptionsConstants.MD_EI_IMPLANT), "Should have EI implant after setting");
        }

        @Test
        @DisplayName("Tracking ON: EI implant with EI cockpit gives active EI")
        void trackingOn_eiImplantWithCockpitGivesActiveEi() {
            setTrackingOption(true);
            Mek mek = createMek(false, null);
            addEiCockpit(mek);

            assertFalse(mek.hasActiveEiCockpit(), "Should not have active EI without implant");

            setCrewOption(mek, OptionsConstants.MD_EI_IMPLANT, true);

            assertTrue(mek.hasActiveEiCockpit(), "Should have active EI with implant AND cockpit");
        }

        @Test
        @DisplayName("Tracking OFF: EI cockpit alone gives active EI (no implant needed)")
        void trackingOff_eiCockpitAloneGivesActiveEi() {
            setTrackingOption(false);
            Mek mek = createMek(false, null);
            addEiCockpit(mek);

            // No implant, but tracking is off so cockpit alone should work
            assertTrue(mek.hasActiveEiCockpit(), "Should have active EI with just cockpit when tracking OFF");
        }
    }
}

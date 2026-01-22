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
import megamek.common.equipment.MiscType;
import megamek.common.game.Game;
import megamek.common.options.OptionsConstants;
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
                MiscType dniMod = (MiscType) EquipmentType.get("BABattleMechNIU");
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

    @Nested
    @DisplayName("Equipment Definition Tests")
    class EquipmentDefinitionTests {

        @Test
        @DisplayName("DNI Cockpit Mod equipment exists")
        void dniCockpitModEquipmentExists() {
            EquipmentType dniMod = EquipmentType.get("BABattleMechNIU");
            assertNotNull(dniMod, "DNI Cockpit Mod equipment should exist");
        }

        @Test
        @DisplayName("DNI Cockpit Mod has F_BATTLEMEK_NIU flag")
        void dniCockpitModHasCorrectFlag() {
            MiscType dniMod = (MiscType) EquipmentType.get("BABattleMechNIU");
            assertTrue(dniMod.hasFlag(MiscType.F_BATTLEMEK_NIU),
                  "DNI Cockpit Mod should have F_BATTLEMEK_NIU flag");
        }

        @Test
        @DisplayName("DNI Cockpit Mod costs 250,000 C-bills")
        void dniCockpitModCostIsCorrect() {
            MiscType dniMod = (MiscType) EquipmentType.get("BABattleMechNIU");
            assertEquals(250000, dniMod.getCost(null, false, Entity.LOC_NONE),
                  "DNI Cockpit Mod should cost 250,000 C-bills");
        }

        @Test
        @DisplayName("DNI Cockpit Mod has zero tonnage")
        void dniCockpitModHasZeroTonnage() {
            MiscType dniMod = (MiscType) EquipmentType.get("BABattleMechNIU");
            assertEquals(0.0, dniMod.getTonnage(null), 0.001,
                  "DNI Cockpit Mod should have zero tonnage");
        }

        @Test
        @DisplayName("DNI Cockpit Mod is available for Meks")
        void dniCockpitModAvailableForMeks() {
            MiscType dniMod = (MiscType) EquipmentType.get("BABattleMechNIU");
            assertTrue(dniMod.hasFlag(MiscType.F_MEK_EQUIPMENT),
                  "DNI Cockpit Mod should be available for Meks");
        }

        @Test
        @DisplayName("DNI Cockpit Mod is available for Tanks")
        void dniCockpitModAvailableForTanks() {
            MiscType dniMod = (MiscType) EquipmentType.get("BABattleMechNIU");
            assertTrue(dniMod.hasFlag(MiscType.F_TANK_EQUIPMENT),
                  "DNI Cockpit Mod should be available for Tanks");
        }

        @Test
        @DisplayName("DNI Cockpit Mod is available for Fighters")
        void dniCockpitModAvailableForFighters() {
            MiscType dniMod = (MiscType) EquipmentType.get("BABattleMechNIU");
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
        @DisplayName("EI pilot with EI cockpit option gets benefits")
        void eiPilotWithEiCockpitOptionGetsBenefits() {
            setTrackingOption(false);
            // Enable the all_have_ei_cockpit option for non-Clan Meks
            game.getOptions().getOption(OptionsConstants.ADVANCED_ALL_HAVE_EI_COCKPIT).setValue(true);

            Mek mek = createMek(false, "EI");

            assertTrue(mek.hasActiveEiCockpit(),
                  "EI pilot should get benefits when tracking is OFF and all_have_ei option is ON");
        }

        @Test
        @DisplayName("Non-EI pilot with EI cockpit option has no benefits")
        void nonEiPilotWithEiCockpitOptionNoBenefits() {
            setTrackingOption(false);
            game.getOptions().getOption(OptionsConstants.ADVANCED_ALL_HAVE_EI_COCKPIT).setValue(true);

            // Mek without EI implant should not have active EI even with EI cockpit option
            Mek mek = createMek(false, null);

            assertFalse(mek.hasActiveEiCockpit(),
                  "Pilot without EI implant should NOT have active EI even with EI cockpit option");
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
}

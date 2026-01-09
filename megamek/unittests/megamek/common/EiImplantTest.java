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
import static org.junit.jupiter.api.Assertions.assertTrue;

import megamek.common.battleArmor.BattleArmor;
import megamek.common.equipment.EquipmentType;
import megamek.common.equipment.MiscType;
import megamek.common.equipment.Sensor;
import megamek.common.game.Game;
import megamek.common.options.OptionsConstants;
import megamek.common.units.BipedMek;
import megamek.common.units.Crew;
import megamek.common.units.CrewType;
import megamek.common.units.EntityWeightClass;
import megamek.common.units.ProtoMek;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Tests for Enhanced Imaging (EI) Interface and Implant functionality.
 * <p>
 * TableTop Rules (IO p.77):
 * <ul>
 *     <li>EI Interface: Equipment that provides neural connection to unit</li>
 *     <li>EI Implant: Pilot augmentation that activates full EI benefits</li>
 *     <li>Aimed Shots: +2 (EI only), -1 (EI with TC) vs +3 (normal TC)</li>
 *     <li>IS Damage: TN 7 roll when taking internal damage, fail = 1 pilot damage</li>
 *     <li>Darkness: Ignores darkness modifiers</li>
 *     <li>Active Probe: 1-hex active probe from neural interface</li>
 *     <li>ProtoMeks: Always have EI built-in</li>
 * </ul>
 */
public class EiImplantTest {

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
     * Creates a test Mek with optional EI Interface equipment and pilot implant.
     */
    private BipedMek createMek(boolean withEiInterface, boolean withEiImplant) {
        BipedMek mek = new BipedMek();
        mek.setGame(game);
        mek.setId(1);
        mek.setChassis("Test Mek");
        mek.setModel(withEiInterface ? "EI" : "Standard");
        mek.setWeight(50);

        // Initialize crew
        Crew crew = new Crew(CrewType.SINGLE);
        mek.setCrew(crew);

        // Set EI implant option on crew
        if (withEiImplant) {
            crew.getOptions().getOption(OptionsConstants.MD_EI_IMPLANT).setValue(true);
        }

        mek.setOwner(game.getPlayer(0));
        mek.autoSetInternal();

        // Add EI Interface equipment if requested
        if (withEiInterface) {
            try {
                MiscType eiInterface = (MiscType) EquipmentType.get("EIInterface");
                if (eiInterface != null) {
                    mek.addEquipment(eiInterface, BipedMek.LOC_HEAD);
                }
            } catch (Exception ignored) {
                // Equipment addition may fail in test context
            }
        }

        return mek;
    }

    /**
     * Creates a Battle Armor unit with optional EI Interface and pilot implant.
     */
    private BattleArmor createBattleArmor(int troopers, boolean withEiInterface, boolean withEiImplant) {
        BattleArmor ba = new BattleArmor();
        ba.setGame(game);
        ba.setId(1);
        ba.setChassis("Test BA");
        ba.setModel(withEiInterface ? "EI" : "Standard");
        ba.setTroopers(troopers);
        ba.setWeightClass(EntityWeightClass.WEIGHT_MEDIUM);

        // Initialize crew
        Crew crew = new Crew(CrewType.INFANTRY_CREW);
        ba.setCrew(crew);

        // Set EI implant option on crew
        if (withEiImplant) {
            crew.getOptions().getOption(OptionsConstants.MD_EI_IMPLANT).setValue(true);
        }

        ba.setOwner(game.getPlayer(0));

        // Set armor values for each trooper
        for (int i = 1; i <= troopers; i++) {
            ba.initializeArmor(4, i);
        }

        ba.autoSetInternal();

        // Add EI Interface equipment if requested
        if (withEiInterface) {
            try {
                MiscType eiInterface = (MiscType) EquipmentType.get("EIInterface");
                if (eiInterface != null) {
                    ba.addEquipment(eiInterface, BattleArmor.LOC_SQUAD);
                }
            } catch (Exception ignored) {
                // Equipment addition may fail in test context
            }
        }

        return ba;
    }

    /**
     * Creates a ProtoMek for testing.
     */
    private ProtoMek createProtoMek() {
        ProtoMek proto = new ProtoMek();
        proto.setGame(game);
        proto.setId(1);
        proto.setChassis("Test ProtoMek");
        proto.setModel("Standard");
        proto.setWeight(5);

        // Initialize crew
        Crew crew = new Crew(CrewType.SINGLE);
        proto.setCrew(crew);

        proto.setOwner(game.getPlayer(0));
        proto.autoSetInternal();

        return proto;
    }

    @Nested
    @DisplayName("EI Interface Equipment Tests")
    class EiInterfaceEquipmentTests {

        @Test
        @DisplayName("EI Interface equipment exists in equipment database")
        void eiInterfaceEquipmentExists() {
            EquipmentType eiInterface = EquipmentType.get("EIInterface");
            assertTrue(eiInterface != null, "EI Interface should exist in equipment database");
        }

        @Test
        @DisplayName("EI Interface has correct flag")
        void eiInterfaceHasCorrectFlag() {
            EquipmentType eiInterface = EquipmentType.get("EIInterface");
            assertTrue(eiInterface != null && eiInterface instanceof MiscType,
                  "EI Interface should be a MiscType");
            MiscType miscType = (MiscType) eiInterface;
            assertTrue(miscType.hasFlag(MiscType.F_EI_INTERFACE),
                  "EI Interface should have F_EI_INTERFACE flag");
        }

        @Test
        @DisplayName("EI Interface is zero weight")
        void eiInterfaceIsZeroWeight() {
            EquipmentType eiInterface = EquipmentType.get("EIInterface");
            assertTrue(eiInterface != null, "EI Interface should exist");
            assertEquals(0, eiInterface.getTonnage(null), 0.001,
                  "EI Interface should be zero weight");
        }
    }

    @Nested
    @DisplayName("hasEiCockpit() Tests")
    class HasEiCockpitTests {

        @Test
        @DisplayName("ProtoMek always has EI cockpit")
        void protoMekAlwaysHasEiCockpit() {
            ProtoMek proto = createProtoMek();
            assertTrue(proto.hasEiCockpit(),
                  "ProtoMeks should always have EI cockpit (integral to design)");
        }

        @Test
        @DisplayName("Mek without EI Interface does not have EI cockpit")
        void mekWithoutEiInterfaceDoesNotHaveEiCockpit() {
            BipedMek mek = createMek(false, false);
            assertFalse(mek.hasEiCockpit(),
                  "Mek without EI Interface equipment should not have EI cockpit");
        }

        @Test
        @DisplayName("BA without EI Interface does not have EI cockpit")
        void baWithoutEiInterfaceDoesNotHaveEiCockpit() {
            BattleArmor ba = createBattleArmor(4, false, false);
            assertFalse(ba.hasEiCockpit(),
                  "BA without EI Interface equipment should not have EI cockpit");
        }
    }

    @Nested
    @DisplayName("hasActiveEiCockpit() Tests")
    class HasActiveEiCockpitTests {

        @Test
        @DisplayName("ProtoMek always has active EI cockpit (built-in per IO p.77)")
        void protoMekAlwaysHasActiveEiCockpit() {
            ProtoMek proto = createProtoMek();
            // ProtoMeks have built-in EI that doesn't require crew implant option
            // per IO p.77 - they are neurally connected by design
            assertTrue(proto.hasActiveEiCockpit(),
                  "ProtoMek should always have active EI cockpit (built-in, no crew option needed)");
        }

        @Test
        @DisplayName("ProtoMek EI active without MD_EI_IMPLANT option")
        void protoMekEiActiveWithoutImplantOption() {
            ProtoMek proto = createProtoMek();
            // Explicitly verify crew does NOT have MD_EI_IMPLANT set
            assertFalse(proto.getCrew().getOptions().booleanOption(OptionsConstants.MD_EI_IMPLANT),
                  "Test setup: crew should not have MD_EI_IMPLANT option");
            // ProtoMek EI should still be active
            assertTrue(proto.hasActiveEiCockpit(),
                  "ProtoMek EI should be active even without crew MD_EI_IMPLANT option");
        }

        @Test
        @DisplayName("Unit with EI Interface but no implant does not have active EI")
        void unitWithInterfaceButNoImplantDoesNotHaveActiveEi() {
            // EI Interface alone doesn't provide full benefits - need implant too
            BattleArmor ba = createBattleArmor(4, true, false);
            // Note: hasEiCockpit checks for equipment, hasActiveEiCockpit needs both
            // BA will have equipment but no implant
            assertFalse(ba.hasActiveEiCockpit(),
                  "Unit with EI Interface but no implant should not have active EI");
        }
    }

    @Nested
    @DisplayName("EI Shutdown Tests")
    class EiShutdownTests {

        @Test
        @DisplayName("ProtoMek cannot shut down EI")
        void protoMekCannotShutdownEi() {
            ProtoMek proto = createProtoMek();
            assertFalse(proto.canShutdownEi(),
                  "ProtoMeks cannot shut down EI (integral to design)");
        }

        @Test
        @DisplayName("setEiShutdown does not affect ProtoMek")
        void setEiShutdownDoesNotAffectProtoMek() {
            ProtoMek proto = createProtoMek();
            // ProtoMek EI is built-in - no crew option needed

            assertTrue(proto.hasActiveEiCockpit(), "ProtoMek should have active EI before shutdown attempt");

            proto.setEiShutdown(true);  // Should have no effect

            assertTrue(proto.hasActiveEiCockpit(),
                  "ProtoMek EI should still be active after shutdown attempt (cannot shutdown)");
        }

        @Test
        @DisplayName("isEiShutdown returns false by default")
        void isEiShutdownReturnsFalseByDefault() {
            ProtoMek proto = createProtoMek();
            assertFalse(proto.isEiShutdown(),
                  "EI should not be shutdown by default");
        }
    }

    @Nested
    @DisplayName("EI Sensor/Probe Tests")
    class EiSensorTests {

        @Test
        @DisplayName("TYPE_EI_PROBE sensor type exists")
        void eiProbeSensorTypeExists() {
            assertEquals(23, Sensor.TYPE_EI_PROBE,
                  "TYPE_EI_PROBE should be defined as type 23");
        }

        @Test
        @DisplayName("TYPE_EI_PROBE is classified as BAP")
        void eiProbeIsClassifiedAsBap() {
            Sensor eiProbe = new Sensor(Sensor.TYPE_EI_PROBE);
            assertTrue(eiProbe.isBAP(),
                  "EI Probe should be classified as an active probe (BAP)");
        }

        @Test
        @DisplayName("TYPE_EI_PROBE has 1-hex range")
        void eiProbeHasOneHexRange() {
            Sensor eiProbe = new Sensor(Sensor.TYPE_EI_PROBE);
            assertEquals(1, eiProbe.getRangeByBracket(),
                  "EI Probe should have 1-hex range per IO p.77");
        }

        @Test
        @DisplayName("ProtoMek with EI has BAP capability")
        void protoMekWithEiHasBapCapability() {
            ProtoMek proto = createProtoMek();
            // ProtoMeks always have EI, so they should report having BAP capability
            assertTrue(proto.hasBAP(),
                  "ProtoMek should have BAP capability from EI Interface");
        }

        @Test
        @DisplayName("ProtoMek EI provides 1-hex BAP range")
        void protoMekEiProvidesOneHexBapRange() {
            ProtoMek proto = createProtoMek();
            assertEquals(1, proto.getBAPRange(),
                  "ProtoMek EI should provide 1-hex BAP range per IO p.77");
        }
    }
}

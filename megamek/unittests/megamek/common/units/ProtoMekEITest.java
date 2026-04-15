/*
 * Copyright (C) 2025-2026 The MegaMek Team. All Rights Reserved.
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
package megamek.common.units;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import megamek.common.CriticalSlot;
import megamek.common.GameBoardTestCase;
import megamek.common.Player;
import megamek.common.equipment.EquipmentType;
import megamek.common.game.Game;
import megamek.common.options.GameOptions;
import megamek.common.options.IOption;
import megamek.common.options.OptionsConstants;
import megamek.common.options.PilotOptions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Tests for ProtoMek Enhanced Imaging (EI) implementation per IO:AE p.69. ProtoMeks have EI
 * built-in. The {@code neural_interface_mode} game option controls whether EI is active and
 * how it affects tech level.
 *
 * <p>Off: No EI bonuses, Standard tech. Pilot Only: EI active, Standard tech.
 * Full Tracking: EI active, Experimental tech.</p>
 */
@DisplayName("ProtoMek EI Tests")
class ProtoMekEITest extends GameBoardTestCase {

    private static final String BOARD_DATA = """
          size 3 3
          hex 0101 0 "" ""
          hex 0201 0 "" ""
          hex 0301 0 "" ""
          hex 0102 0 "" ""
          hex 0202 0 "" ""
          hex 0302 0 "" ""
          hex 0103 0 "" ""
          hex 0203 0 "" ""
          hex 0303 0 "" ""
          end""";

    private static Player player;
    private GameOptions mockGameOptions;
    private Game game;
    private ProtoMek protoMek;

    static {
        initializeBoard("FLAT_3X3", BOARD_DATA);
    }

    @BeforeAll
    static void setUpAll() {
        EquipmentType.initializeTypes();
        player = new Player(0, "TestPlayer");
    }

    @BeforeEach
    void setUp() {
        mockGameOptions = mock(GameOptions.class);
        game = getGame();
        game.setOptions(mockGameOptions);

        IOption mockOption = mock(IOption.class);
        when(mockOption.booleanValue()).thenReturn(false);
        when(mockOption.stringValue()).thenReturn("");
        when(mockGameOptions.getOption(org.mockito.ArgumentMatchers.anyString())).thenReturn(mockOption);
        // Default: neural interface mode OFF
        when(mockGameOptions.stringOption(OptionsConstants.ADVANCED_NEURAL_INTERFACE_MODE))
              .thenReturn(OptionsConstants.NEURAL_INTERFACE_MODE_OFF);

        game.addPlayer(0, player);
        setBoard("FLAT_3X3");

        protoMek = createProtoMek("Basilisk", "Standard", "TestPilot");
        protoMek.setOwnerId(player.getId());
        protoMek.setId(1);
        game.addEntity(protoMek);
    }

    private ProtoMek createProtoMek(String chassis, String model, String pilotName) {
        ProtoMek proto = new ProtoMek();
        proto.setGame(game);
        proto.setChassis(chassis);
        proto.setModel(model);
        proto.setWeight(5);

        // Mock crew without EI implant option - ProtoMeks don't need it
        Crew mockCrew = mock(Crew.class);
        PilotOptions pilotOptions = new PilotOptions();
        when(mockCrew.getName(anyInt())).thenReturn(pilotName);
        when(mockCrew.getNames()).thenReturn(new String[] { pilotName });
        when(mockCrew.getOptions()).thenReturn(pilotOptions);
        when(mockCrew.isActive()).thenReturn(true);
        proto.setCrew(mockCrew);

        // Initialize internal structure for head damage tests
        proto.initializeInternal(1, ProtoMek.LOC_HEAD);
        proto.initializeInternal(3, ProtoMek.LOC_TORSO);
        proto.initializeInternal(2, ProtoMek.LOC_LEG);

        return proto;
    }

    private void setNeuralInterfaceMode(String mode) {
        when(mockGameOptions.stringOption(OptionsConstants.ADVANCED_NEURAL_INTERFACE_MODE))
              .thenReturn(mode);
    }

    @Nested
    @DisplayName("Off Mode - No EI Bonuses")
    class OffModeTests {

        @Test
        @DisplayName("ProtoMek does NOT have EI cockpit when mode is Off")
        void protoMekNoEiCockpitWhenOff() {
            setNeuralInterfaceMode(OptionsConstants.NEURAL_INTERFACE_MODE_OFF);
            assertFalse(protoMek.hasEiCockpit(),
                  "ProtoMeks should NOT have EI cockpit when neural interface mode is Off");
        }

        @Test
        @DisplayName("ProtoMek does NOT have active EI when mode is Off")
        void protoMekNoActiveEiWhenOff() {
            setNeuralInterfaceMode(OptionsConstants.NEURAL_INTERFACE_MODE_OFF);
            assertFalse(protoMek.hasActiveEiCockpit(),
                  "ProtoMeks should NOT have active EI when neural interface mode is Off");
        }
    }

    @Nested
    @DisplayName("Pilot Only Mode - EI Active, Standard Tech")
    class PilotOnlyModeTests {

        @BeforeEach
        void setMode() {
            setNeuralInterfaceMode(OptionsConstants.NEURAL_INTERFACE_MODE_PILOT_ONLY);
        }

        @Test
        @DisplayName("ProtoMek has EI cockpit in Pilot Only mode")
        void protoMekHasEiCockpit() {
            assertTrue(protoMek.hasEiCockpit(),
                  "ProtoMeks should have EI cockpit in Pilot Only mode per IO:AE p.69");
        }

        @Test
        @DisplayName("ProtoMek EI is active when head undamaged - no crew implant required")
        void protoMekEiActiveWhenHeadUndamaged() {
            assertTrue(protoMek.hasActiveEiCockpit(),
                  "ProtoMek EI should be active when head is undamaged, without crew implant option");
        }

        @Test
        @DisplayName("ProtoMek EI is disabled when head has critical damage")
        void protoMekEiDisabledWhenHeadDamaged() {
            CriticalSlot headCrit = protoMek.getCritical(ProtoMek.LOC_HEAD, 0);
            headCrit.setHit(true);

            assertFalse(protoMek.hasActiveEiCockpit(),
                  "ProtoMek EI should be disabled when head has critical damage");
        }
    }

    @Nested
    @DisplayName("Full Tracking Mode - EI Active, Experimental Tech")
    class FullTrackingModeTests {

        @BeforeEach
        void setMode() {
            setNeuralInterfaceMode(OptionsConstants.NEURAL_INTERFACE_MODE_FULL_TRACKING);
        }

        @Test
        @DisplayName("ProtoMek has EI cockpit in Full Tracking mode")
        void protoMekHasEiCockpit() {
            assertTrue(protoMek.hasEiCockpit(),
                  "ProtoMeks should have EI cockpit in Full Tracking mode per IO:AE p.69");
        }

        @Test
        @DisplayName("ProtoMek EI is active when head undamaged - no crew implant required")
        void protoMekEiActiveWhenHeadUndamaged() {
            assertTrue(protoMek.hasActiveEiCockpit(),
                  "ProtoMek EI should be active when head is undamaged, without crew implant option");
        }

        @Test
        @DisplayName("ProtoMek EI is disabled when head has critical damage")
        void protoMekEiDisabledWhenHeadDamaged() {
            CriticalSlot headCrit = protoMek.getCritical(ProtoMek.LOC_HEAD, 0);
            headCrit.setHit(true);

            assertFalse(protoMek.hasActiveEiCockpit(),
                  "ProtoMek EI should be disabled when head has critical damage");
        }

        @Test
        @DisplayName("ProtoMek EI works without MD_EI_IMPLANT pilot option")
        void protoMekEiWorksWithoutCrewOption() {
            ProtoMek protoWithoutImplant = createProtoMek("Test", "NoImplant", "Pilot");
            protoWithoutImplant.setId(2);
            game.addEntity(protoWithoutImplant);

            assertTrue(protoWithoutImplant.hasActiveEiCockpit(),
                  "ProtoMek EI should work even without crew having EI implant option");
        }

        @Test
        @DisplayName("Regular Mek requires crew EI implant - ProtoMek does not")
        void regularMekRequiresCrewOptionProtoMekDoesNot() {
            Mek mek = new BipedMek();
            mek.setGame(game);
            mek.setChassis("Atlas");
            mek.setModel("AS7-D");

            Crew mockCrew = mock(Crew.class);
            PilotOptions pilotOptions = new PilotOptions();
            when(mockCrew.getOptions()).thenReturn(pilotOptions);
            mek.setCrew(mockCrew);

            assertFalse(mek.hasActiveEiCockpit(),
                  "Regular Mek should NOT have active EI without crew implant option");

            assertTrue(protoMek.hasActiveEiCockpit(),
                  "ProtoMek should have active EI regardless of crew options");
        }
    }
}

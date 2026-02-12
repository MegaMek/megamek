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
import megamek.common.options.PilotOptions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Tests for ProtoMek Enhanced Imaging (EI) implementation per IO p.77. ProtoMeks have EI built-in and cannot disable
 * it.
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

    private static GameOptions mockGameOptions;
    private static Player player;
    private Game game;
    private ProtoMek protoMek;

    static {
        initializeBoard("FLAT_3X3", BOARD_DATA);
    }

    @BeforeAll
    static void setUpAll() {
        EquipmentType.initializeTypes();
        mockGameOptions = mock(GameOptions.class);
        player = new Player(0, "TestPlayer");
    }

    @BeforeEach
    void setUp() {
        game = getGame();
        game.setOptions(mockGameOptions);

        IOption mockOption = mock(IOption.class);
        when(mockOption.booleanValue()).thenReturn(false);
        when(mockGameOptions.getOption(org.mockito.ArgumentMatchers.anyString())).thenReturn(mockOption);

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

    @Nested
    @DisplayName("ProtoMek Built-in EI Tests")
    class BuiltInEITests {

        @Test
        @DisplayName("ProtoMek always has EI cockpit (built-in per IO p.77)")
        void protoMekAlwaysHasEiCockpit() {
            assertTrue(protoMek.hasEiCockpit(),
                  "ProtoMeks should always have EI cockpit built-in per IO p.77");
        }

        @Test
        @DisplayName("ProtoMek EI is active when head undamaged - no crew implant required")
        void protoMekEiActiveWhenHeadUndamaged() {
            // ProtoMeks don't need crew EI implant option - they're neurally connected by default
            // This differs from regular Meks which require the pilot to have EI implants
            assertTrue(protoMek.hasActiveEiCockpit(),
                  "ProtoMek EI should be active when head is undamaged, without crew implant option");
        }

        @Test
        @DisplayName("ProtoMek EI is disabled when head has critical damage")
        void protoMekEiDisabledWhenHeadHasCriticalDamage() {
            // ProtoMek head has 2 critical slots (SYSTEM_HEAD_CRIT)
            // Damage one of them to simulate sensor damage that disables EI
            CriticalSlot headCrit = protoMek.getCritical(ProtoMek.LOC_HEAD, 0);
            headCrit.setHit(true);

            assertFalse(protoMek.hasActiveEiCockpit(),
                  "ProtoMek EI should be disabled when head has critical damage");
        }

        @Test
        @DisplayName("ProtoMek EI remains active when head crits are undamaged")
        void protoMekEiActiveWhenHeadCritsUndamaged() {
            // Verify the head critical slots exist and are undamaged
            CriticalSlot headCrit0 = protoMek.getCritical(ProtoMek.LOC_HEAD, 0);
            CriticalSlot headCrit1 = protoMek.getCritical(ProtoMek.LOC_HEAD, 1);

            // Both crits should be undamaged initially
            assertFalse(headCrit0.isDamaged(), "Head crit 0 should be undamaged initially");
            assertFalse(headCrit1.isDamaged(), "Head crit 1 should be undamaged initially");

            // EI should be active with undamaged head
            assertTrue(protoMek.hasActiveEiCockpit(),
                  "ProtoMek EI should remain active with undamaged head crits");
        }
    }

    @Nested
    @DisplayName("ProtoMek EI Independence from Crew Options")
    class CrewIndependenceTests {

        @Test
        @DisplayName("ProtoMek EI works without MD_EI_IMPLANT pilot option")
        void protoMekEiWorksWithoutCrewOption() {
            // Create a ProtoMek with crew that explicitly does NOT have EI implant option
            ProtoMek protoWithoutImplant = createProtoMek("Test", "NoImplant", "Pilot");
            protoWithoutImplant.setId(2);
            game.addEntity(protoWithoutImplant);

            // The crew's PilotOptions is empty (no MD_EI_IMPLANT set)
            // But ProtoMek EI should still work because it's built-in
            assertTrue(protoWithoutImplant.hasActiveEiCockpit(),
                  "ProtoMek EI should work even without crew having EI implant option");
        }

        @Test
        @DisplayName("Regular Mek requires crew EI implant option - ProtoMek does not")
        void regularMekRequiresCrewOptionProtoMekDoesNot() {
            // Create a regular Mek with crew that does NOT have EI implant option
            Mek mek = new BipedMek();
            mek.setGame(game);
            mek.setChassis("Atlas");
            mek.setModel("AS7-D");

            Crew mockCrew = mock(Crew.class);
            PilotOptions pilotOptions = new PilotOptions();
            // Not setting MD_EI_IMPLANT - crew doesn't have implant
            when(mockCrew.getOptions()).thenReturn(pilotOptions);
            mek.setCrew(mockCrew);

            // Regular Mek should NOT have active EI without crew implant
            assertFalse(mek.hasActiveEiCockpit(),
                  "Regular Mek should NOT have active EI without crew implant option");

            // But ProtoMek should still have active EI
            assertTrue(protoMek.hasActiveEiCockpit(),
                  "ProtoMek should have active EI regardless of crew options");
        }
    }
}

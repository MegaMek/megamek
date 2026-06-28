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

import megamek.common.Player;
import megamek.common.equipment.EquipmentType;
import megamek.common.game.Game;
import megamek.common.options.GameOptions;
import megamek.common.options.IOption;
import megamek.common.options.OptionsConstants;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Tests for {@link Entity#isCapitalFighter(boolean)} behavior with game options. These tests document the interaction
 * between StratOps Capital Fighter rules and the "Single Fighters Not Capital" unofficial option (see GitHub issue
 * #7935).
 */
class CapitalFighterOptionsTest {

    private Game game;
    private AeroSpaceFighter fighter;

    @BeforeAll
    static void beforeAll() {
        EquipmentType.initializeTypes();
    }

    @BeforeEach
    void setUp() {
        game = new Game();
        GameOptions gameOptions = new GameOptions();
        game.setOptions(gameOptions);

        Player player = new Player(1, "Test");
        game.addPlayer(1, player);

        fighter = new AeroSpaceFighter();
        fighter.setId(game.getNextEntityId());
        game.addEntity(fighter);
        fighter.setOwner(player);
    }

    private void setCapitalFighterOption(boolean enabled) {
        IOption option = game.getOptions()
              .getOption(OptionsConstants.ADVANCED_AERO_RULES_STRATOPS_CAPITAL_FIGHTER);
        option.setValue(enabled);
    }

    private void setSingleFightersNotCapitalOption(boolean enabled) {
        IOption option = game.getOptions()
              .getOption(OptionsConstants.ADVANCED_AERO_RULES_SINGLE_NO_CAP);
        option.setValue(enabled);
    }

    @Nested
    @DisplayName("isCapitalFighter() with default lounge=false")
    class IsCapitalFighterDefaultTests {

        @Test
        @DisplayName("Fighter is not capital when StratOps Capital Fighter option is OFF")
        void notCapitalWhenOptionOff() {
            setCapitalFighterOption(false);

            assertFalse(fighter.isCapitalFighter(),
                  "Fighter should not be capital when StratOps Capital Fighter is disabled");
        }

        @Test
        @DisplayName("Fighter is capital when StratOps Capital Fighter option is ON")
        void isCapitalWhenOptionOn() {
            setCapitalFighterOption(true);

            assertTrue(fighter.isCapitalFighter(),
                  "Fighter should be capital when StratOps Capital Fighter is enabled");
        }

        @Test
        @DisplayName("Single fighter is NOT capital when both options are ON (Issue #7935)")
        void singleFighterNotCapitalWithBothOptionsOn() {
            // This test verifies the fix for issue #7935:
            // When both "StratOps Capital Fighters" AND "Single Fighters Not Capital" are enabled,
            // a single fighter (not in a squadron) should NOT be treated as a capital fighter.
            setCapitalFighterOption(true);
            setSingleFightersNotCapitalOption(true);

            assertFalse(fighter.isCapitalFighter(),
                  "Single fighter should NOT be capital when 'Single Fighters Not Capital' is enabled");
        }

        @Test
        @DisplayName("Single Fighters Not Capital has no effect when StratOps Capital Fighter is OFF")
        void singleNoCapHasNoEffectWhenCapitalOff() {
            setCapitalFighterOption(false);
            setSingleFightersNotCapitalOption(true);

            assertFalse(fighter.isCapitalFighter(),
                  "Fighter should not be capital regardless of Single No Cap when Capital Fighter is off");
        }
    }

    @Nested
    @DisplayName("isCapitalFighter(true) with lounge=true (legacy behavior)")
    class IsCapitalFighterLoungeTests {

        @Test
        @DisplayName("Fighter is capital with lounge=true even when Single Fighters Not Capital is ON")
        void loungeIgnoresSingleNoCapOption() {
            // This documents the legacy behavior where lounge=true skips the Single No Cap check.
            // This was causing issue #7935 when lobby code used isCapitalFighter(true).
            setCapitalFighterOption(true);
            setSingleFightersNotCapitalOption(true);

            assertTrue(fighter.isCapitalFighter(true),
                  "isCapitalFighter(true) ignores Single Fighters Not Capital option");
        }

        @Test
        @DisplayName("Comparing lounge=true vs lounge=false with both options ON")
        void compareLoungeParameterBehavior() {
            // This test documents the difference between lounge=true and lounge=false
            // which was the root cause of issue #7935
            setCapitalFighterOption(true);
            setSingleFightersNotCapitalOption(true);

            // lounge=false respects "Single Fighters Not Capital" option
            assertFalse(fighter.isCapitalFighter(false),
                  "lounge=false should respect Single Fighters Not Capital option");

            // lounge=true ignores "Single Fighters Not Capital" option
            assertTrue(fighter.isCapitalFighter(true),
                  "lounge=true should ignore Single Fighters Not Capital option");
        }
    }

    @Nested
    @DisplayName("Lobby Loading Eligibility (Issue #7935)")
    class LobbyLoadingEligibilityTests {

        @Test
        @DisplayName("Fighter with transport bays should be eligible loader when Single No Cap is ON")
        void fighterEligibleForLoadingWithSingleNoCap() {
            // This test verifies that with the fix for #7935, single fighters
            // will pass the !isCapitalFighter() check used in LobbyMekPopup
            // when "Single Fighters Not Capital" is enabled.
            setCapitalFighterOption(true);
            setSingleFightersNotCapitalOption(true);

            // The lobby filter is: .filter(e -> !e.isCapitalFighter())
            // With the fix, this uses isCapitalFighter() which returns false for single fighters
            // when Single No Cap is enabled, so they pass the filter.
            boolean passesLobbyFilter = !fighter.isCapitalFighter();

            assertTrue(passesLobbyFilter,
                  "Single fighter should pass lobby loading filter when Single No Cap is enabled");
        }

        @Test
        @DisplayName("Fighter should NOT be eligible loader when only StratOps Capital Fighter is ON")
        void fighterNotEligibleWhenOnlyCapitalOn() {
            // When only StratOps Capital Fighter is on (without Single No Cap),
            // fighters should still be filtered out as capital fighters
            setCapitalFighterOption(true);
            setSingleFightersNotCapitalOption(false);

            boolean passesLobbyFilter = !fighter.isCapitalFighter();

            assertFalse(passesLobbyFilter,
                  "Fighter should NOT pass lobby filter when it's a capital fighter");
        }
    }
}

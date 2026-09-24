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

package megamek.common.equipment;

import static org.junit.jupiter.api.Assertions.assertInstanceOf;

import megamek.common.game.Game;
import megamek.common.options.OptionsConstants;
import megamek.common.rules.RulesScanning;
import megamek.common.rules.core.CoreRulesScanning;
import megamek.common.rules.tacops.TacOpsScanning;
import org.junit.jupiter.api.Test;

/**
 * Which scanning rules a game resolves a scan by. The Core Rulebook's mission scanning check is the baseline; the
 * optional TacOps: Advanced Rules scanning rule replaces it when a game switches that rule on.
 */
class ScanMissionRulesTest {

    @Test
    void testAGameUsesTheCoreRulebookCheckByDefault() {
        Game game = new Game();

        RulesScanning rules = ScanMission.scanningRules(game);

        assertInstanceOf(CoreRulesScanning.class, rules, "the mission scanning check is the baseline");
    }

    @Test
    void testSwitchingOnTheTacOpsRuleReplacesTheCheck() {
        Game game = new Game();
        game.getOptions().getOption(OptionsConstants.ADVANCED_TAC_OPS_SCANNING).setValue(true);

        RulesScanning rules = ScanMission.scanningRules(game);

        assertInstanceOf(TacOpsScanning.class, rules, "the optional TacOps rule takes over when it is switched on");
    }

    @Test
    void testAUnitWithNoGameYetFallsBackToTheRulesetsCheck() {
        RulesScanning rules = ScanMission.scanningRules(null);

        assertInstanceOf(CoreRulesScanning.class, rules, "no game means no optional rules to read");
    }
}

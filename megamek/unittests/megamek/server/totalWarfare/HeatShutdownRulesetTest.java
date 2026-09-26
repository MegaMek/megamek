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
package megamek.server.totalWarfare;

import static org.junit.jupiter.api.Assertions.assertEquals;

import megamek.common.Player;
import megamek.common.equipment.EquipmentType;
import megamek.common.game.Game;
import megamek.common.options.OptionsConstants;
import megamek.common.rules.RulesManager;
import megamek.common.rules.core.CoreRulesManager;
import megamek.common.rules.totalwarfare.TWRulesManager;
import megamek.common.units.BipedMek;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Tests the server's shutdown roll against the game options and the ruleset, issue #9025: the Expanded Heat Scale no
 * longer brings in the Avoiding Shutdown rule, which has its own option and only the Total Warfare ruleset offers.
 */
class HeatShutdownRulesetTest {

    private static final int HEAT_AT_FIRST_SHUTDOWN_CHECK = 14;
    private static final int REGULAR_PILOTING = 5;

    private RulesManager previousRulesManager;
    private Game game;
    private HeatResolver heatResolver;
    private BipedMek mek;

    @BeforeAll
    static void initializeEquipment() {
        EquipmentType.initializeTypes();
    }

    @BeforeEach
    void setUp() {
        previousRulesManager = Game.rulesManager;
        TWGameManager gameManager = new TWGameManager();
        game = gameManager.getGame();
        game.addPlayer(0, new Player(0, "Player"));
        heatResolver = new HeatResolver(gameManager);

        mek = new BipedMek();
        mek.setOwner(game.getPlayer(0));
        mek.getCrew().setPiloting(REGULAR_PILOTING, 0);
        mek.heat = HEAT_AT_FIRST_SHUTDOWN_CHECK;
    }

    @AfterEach
    void restoreRulesManager() {
        Game.rulesManager = previousRulesManager;
    }

    private void setOption(String name, boolean value) {
        game.getOptions().getOption(name).setValue(value);
    }

    private int shutdownTarget() {
        return heatResolver.shutdownAvoidanceTarget(mek, 0).getValue();
    }

    @Test
    void theExpandedHeatScaleAloneUsesThePlainAvoidNumber() {
        // The reported case: expanded heat on, and the roll at 14 heat came out as an automatic success.
        Game.rulesManager = new TWRulesManager();
        setOption(OptionsConstants.ADVANCED_COMBAT_TAC_OPS_HEAT, true);

        assertEquals(4, shutdownTarget());
    }

    @Test
    void theAvoidingShutdownOptionAppliesUnderTotalWarfare() {
        Game.rulesManager = new TWRulesManager();
        setOption(OptionsConstants.ADVANCED_COMBAT_TAC_OPS_HEAT, true);
        setOption(OptionsConstants.ADVANCED_COMBAT_TAC_OPS_AVOIDING_SHUTDOWN, true);

        assertEquals(-1, shutdownTarget());
    }

    @Test
    void theAvoidingShutdownOptionAppliesWithoutTheExpandedScale() {
        // The book allows Avoiding Shutdown in any game, not only with the expanded scale.
        Game.rulesManager = new TWRulesManager();
        setOption(OptionsConstants.ADVANCED_COMBAT_TAC_OPS_AVOIDING_SHUTDOWN, true);

        assertEquals(-1, shutdownTarget());
    }

    @Test
    void theCoreRulesIgnoreTheAvoidingShutdownOption() {
        Game.rulesManager = new CoreRulesManager();
        setOption(OptionsConstants.ADVANCED_COMBAT_TAC_OPS_AVOIDING_SHUTDOWN, true);

        assertEquals(4, shutdownTarget());
    }
}

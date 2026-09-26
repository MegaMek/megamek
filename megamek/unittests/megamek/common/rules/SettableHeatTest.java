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
package megamek.common.rules;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.List;

import megamek.common.game.Game;
import megamek.common.options.OptionsConstants;
import megamek.common.units.Entity;
import org.junit.jupiter.api.Test;

/**
 * Tests the most heat the lobby's heat menu and the gamemaster's unit editor offer: the top of the scale in play plus
 * the unit's dissipation, which the Heat Phase takes off before it checks heat effects.
 */
class SettableHeatTest {

    /** A Mek with 30 points of heat dissipation, which sinks 30 heat each Heat Phase. */
    private static final int DISSIPATION = 30;

    private static Game gameWithExpandedHeat(boolean isExpandedHeatScale) {
        Game game = new Game();
        game.getOptions().getOption(OptionsConstants.ADVANCED_COMBAT_TAC_OPS_HEAT).setValue(isExpandedHeatScale);
        return game;
    }

    private static Entity unit(int heat, int dissipation) {
        Entity unit = mock(Entity.class);
        unit.heat = heat;
        when(unit.getHeatCapacityWithWater()).thenReturn(dissipation);
        return unit;
    }

    @Test
    void theStandardScaleOffersFortyPlusDissipation() {
        assertEquals(70, SettableHeat.maximum(gameWithExpandedHeat(false), 0, DISSIPATION));
    }

    @Test
    void theExpandedScaleOffersFiftyPlusDissipation() {
        // TO:AR p.102: the scale runs to 50. A Mek sinking 30 set to 50 would be checked at 20, so it takes 80.
        assertEquals(80, SettableHeat.maximum(gameWithExpandedHeat(true), 0, DISSIPATION));
    }

    @Test
    void outsideAGameTheStandardScaleApplies() {
        // MekHQ and MegaMekLab open the unit editor on a unit with no game.
        assertEquals(70, SettableHeat.maximum(null, 0, DISSIPATION));
    }

    @Test
    void aUnitAlreadyPastTheLimitCanStillBeEdited() {
        // A spinner refuses a starting value above its maximum, so a unit at 95 heat must raise the limit to 95.
        assertEquals(95, SettableHeat.maximum(gameWithExpandedHeat(true), 95, DISSIPATION));
    }

    @Test
    void aLobbyGroupReachesTheTopForItsBestCooledUnit() {
        // Two units given the same heat: the one sinking 30 needs 80 to be checked at 50; the one sinking 10 is
        // simply checked hotter.
        List<Entity> units = List.of(unit(0, 10), unit(0, DISSIPATION));

        assertEquals(80, SettableHeat.maximum(gameWithExpandedHeat(true), units));
    }

    @Test
    void anEmptyLobbySelectionStillOffersTheScale() {
        assertEquals(50, SettableHeat.maximum(gameWithExpandedHeat(true), List.of()));
    }
}

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
package megamek.common.actions.compute;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import megamek.common.game.Game;
import megamek.common.planetaryConditions.EMI;
import megamek.common.units.BipedMek;
import megamek.common.units.ConvInfantry;
import megamek.common.units.Entity;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * A unit left suffering EMI by a nuclear strike takes +2 to hit (IO:AE p.166), once only when planetary EMI applies
 * too.
 */
class EmiToHitModifierTest {
    private Game game;
    private Entity mek;
    private Entity platoon;

    @BeforeEach
    void beforeEach() {
        game = new Game();
        mek = new BipedMek();
        mek.setGame(game);
        platoon = new ConvInfantry();
        platoon.setGame(game);
    }

    @Test
    void unitWithoutEmiTakesNothing() {
        assertFalse(ComputeAttackerToHitMods.takesUnitEmiModifier(game, mek));
    }

    @Test
    void nukedUnitTakesTheModifier() {
        mek.setEMI(true);

        assertTrue(ComputeAttackerToHitMods.takesUnitEmiModifier(game, mek));
    }

    @Test
    void nukedUnitUnderPlanetaryEmiTakesItOnlyOnce() {
        // The planetary +2 already applies, so the unit's own EMI adds nothing more: +2, not +4
        game.getPlanetaryConditions().setEMI(EMI.EMI);
        mek.setEMI(true);

        assertFalse(ComputeAttackerToHitMods.takesUnitEmiModifier(game, mek));
    }

    @Test
    void nukedInfantryUnderPlanetaryEmiStillTakesIt() {
        // Planetary EMI exempts conventional infantry; nuclear EMI does not (HammerGS, 2026-10-26)
        game.getPlanetaryConditions().setEMI(EMI.EMI);
        platoon.setEMI(true);

        assertTrue(ComputeAttackerToHitMods.takesUnitEmiModifier(game, platoon));
    }
}

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
package megamek.common.units;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

/**
 * The vertical-liftoff capability rule (SO): "Aerospace fighters, aerodyne small craft, and
 * VSTOL-equipped conventional fighters may lift off or land vertically... all spheroid DropShips
 * and spheroid small craft lift off vertically. A unit must spend 2 Thrust Points to lift off."
 * The old test allowed only VSTOL and spheroids, contradicting the liftoff roll's own "+2, Fighter
 * making vertical liftoff" modifier. Aerodyne DropShips keep their vacuum-only override.
 */
class VerticalLiftoffRulesTest {

    @Test
    void anAerospaceFighterMayLiftOffVertically() {
        AeroSpaceFighter fighter = new AeroSpaceFighter();
        fighter.setOriginalWalkMP(4);

        assertTrue(fighter.canTakeOffVertically(),
              "the liftoff roll's fighter modifier exists because fighters may lift vertically");
    }

    @Test
    void aConventionalFighterNeedsVstolGear() {
        ConvFighter conventional = new ConvFighter();
        conventional.setOriginalWalkMP(4);

        assertFalse(conventional.canTakeOffVertically(),
              "a conventional fighter without VSTOL gear needs a runway");

        conventional.setVSTOL(true);
        assertTrue(conventional.canTakeOffVertically(), "VSTOL gear is the conventional exception");
    }

    @Test
    void anAerodyneSmallCraftMayLiftOffVertically() {
        SmallCraft smallCraft = new SmallCraft();
        smallCraft.setOriginalWalkMP(4);
        smallCraft.setSpheroid(false);

        assertTrue(smallCraft.canTakeOffVertically(), "aerodyne small craft lift vertically per SO");
    }

    @Test
    void aSpheroidSmallCraftLiftsOffVertically() {
        SmallCraft spheroid = new SmallCraft();
        spheroid.setOriginalWalkMP(4);
        spheroid.setSpheroid(true);

        assertTrue(spheroid.canTakeOffVertically(), "spheroids always lift vertically");
    }

    @Test
    void liftingOffSpendsTwoThrustPoints() {
        AeroSpaceFighter winded = new AeroSpaceFighter();
        winded.setOriginalWalkMP(1);
        assertFalse(winded.canTakeOffVertically(),
              "one thrust point cannot pay the two-point liftoff cost");

        AeroSpaceFighter exact = new AeroSpaceFighter();
        exact.setOriginalWalkMP(2);
        assertTrue(exact.canTakeOffVertically(),
              "exactly two thrust points is exactly the liftoff cost");
    }
}

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
package megamek.common.planetaryConditions;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

/**
 * Regression tests for issue #8827: shifting wind strength went past the minimum and maximum limits, because
 * {@link Wind#raiseWind()} and {@link Wind#lowerWind()} skipped strengths (Moderate Gale rose to Storm, Storm fell to
 * Calm).
 */
class WindTest {

    @ParameterizedTest
    @EnumSource(value = Wind.class, names = "TORNADO_F4", mode = EnumSource.Mode.EXCLUDE)
    void raiseWindMovesUpOneStrength(Wind wind) {
        assertEquals(Wind.values()[wind.ordinal() + 1], wind.raiseWind());
    }

    @ParameterizedTest
    @EnumSource(value = Wind.class, names = "CALM", mode = EnumSource.Mode.EXCLUDE)
    void lowerWindMovesDownOneStrength(Wind wind) {
        assertEquals(Wind.values()[wind.ordinal() - 1], wind.lowerWind());
    }

    @Test
    void raiseWindStaysAtStrongest() {
        assertEquals(Wind.TORNADO_F4, Wind.TORNADO_F4.raiseWind());
    }

    @Test
    void lowerWindStaysAtCalm() {
        assertEquals(Wind.CALM, Wind.CALM.lowerWind());
    }
}

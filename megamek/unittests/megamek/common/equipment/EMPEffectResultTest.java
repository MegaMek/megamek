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
package megamek.common.equipment;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import megamek.MMConstants;
import org.junit.jupiter.api.Test;

/**
 * Tests for {@link EMPEffectResult} factory methods and state queries.
 */
class EMPEffectResultTest {

    @Test
    void testNoEffectFactory() {
        EMPEffectResult result = EMPEffectResult.noEffect(5, 0);

        assertEquals(MMConstants.EMP_EFFECT_NONE, result.effect());
        assertEquals(0, result.durationTurns());
        assertEquals(5, result.rollValue());
        assertEquals(0, result.modifier());
        assertTrue(result.isNoEffect());
        assertFalse(result.isInterference());
        assertFalse(result.isShutdown());
    }

    @Test
    void testInterferenceFactory() {
        EMPEffectResult result = EMPEffectResult.interference(4, 7, 0);

        assertEquals(MMConstants.EMP_EFFECT_INTERFERENCE, result.effect());
        assertEquals(4, result.durationTurns());
        assertEquals(7, result.rollValue());
        assertEquals(0, result.modifier());
        assertFalse(result.isNoEffect());
        assertTrue(result.isInterference());
        assertFalse(result.isShutdown());
    }

    @Test
    void testShutdownFactory() {
        EMPEffectResult result = EMPEffectResult.shutdown(3, 10, 2);

        assertEquals(MMConstants.EMP_EFFECT_SHUTDOWN, result.effect());
        assertEquals(3, result.durationTurns());
        assertEquals(10, result.rollValue());
        assertEquals(2, result.modifier());
        assertFalse(result.isNoEffect());
        assertFalse(result.isInterference());
        assertTrue(result.isShutdown());
    }

    @Test
    void testDroneModifierIncluded() {
        EMPEffectResult result = EMPEffectResult.interference(5, 9, 2);

        assertEquals(2, result.modifier());
        assertEquals(9, result.rollValue()); // Modified roll value
    }
}

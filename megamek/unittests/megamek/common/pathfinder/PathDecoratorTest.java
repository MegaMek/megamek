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
package megamek.common.pathfinder;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import megamek.common.game.Game;
import megamek.common.moves.MovePath;
import org.junit.jupiter.api.Test;

/**
 * Tests for {@link PathDecorator#capForLowGravity(MovePath, int, int)}: the bot's decorated paths stop at the
 * gravity-free MP on a low gravity world (issue #9265) and are unchanged everywhere else.
 */
class PathDecoratorTest {

    private static MovePath pathAtGravity(float gravity) {
        Game game = new Game();
        game.getPlanetaryConditions().setGravity(gravity);
        MovePath path = mock(MovePath.class);
        when(path.getGame()).thenReturn(game);
        return path;
    }

    @Test
    void lowGravityCapsTheMPAtTheGravityFreeLimit() {
        // a Firestarter's 9 run MP is 225 at 0.04 g
        assertEquals(9, PathDecorator.capForLowGravity(pathAtGravity(0.04f), 225, 9));
    }

    @Test
    void lowGravityKeepsAnMPAlreadyUnderTheLimit() {
        assertEquals(6, PathDecorator.capForLowGravity(pathAtGravity(0.5f), 6, 9));
    }

    @Test
    void standardGravityIsUnchanged() {
        // sprinting can exceed the running gravity limit at 1 g; that must not be cut
        assertEquals(12, PathDecorator.capForLowGravity(pathAtGravity(1.0f), 12, 9));
    }

    @Test
    void highGravityIsUnchanged() {
        assertEquals(4, PathDecorator.capForLowGravity(pathAtGravity(1.5f), 4, 6));
    }
}

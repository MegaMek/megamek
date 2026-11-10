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
package megamek.client.bot.princess;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.List;
import java.util.TreeSet;

import megamek.common.board.Coords;
import megamek.common.game.Game;
import megamek.common.moves.MovePath;
import megamek.common.units.Entity;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.core.config.Configurator;
import org.junit.jupiter.api.Test;

/**
 * Ranking paths must not post progress lines to chat. The shipped log configuration runs the bot loggers at
 * DEBUG, and the old "... N% complete." chat line was gated on the log level, so it flooded the chat window
 * with twenty lines per unit (issue #9510).
 */
class PathRankerProgressChatTest {

    private static final int PATH_COUNT = 40;

    /** A ranker that accepts every path and scores each one with some expected damage. */
    private static final class StubPathRanker extends PathRanker {
        StubPathRanker(Princess owner) {
            super(owner);
        }

        @Override
        List<MovePath> validatePaths(List<MovePath> startingPathList, Game game, int maxRange,
              double fallTolerance) {
            return startingPathList;
        }

        @Override
        protected RankedPath rankPath(MovePath path, Game game, int maxRange, double fallTolerance,
              List<Entity> enemies, Coords friendsCoords) {
            RankedPath rankedPath = new RankedPath(path.getHexesMoved(), path, "stub");
            rankedPath.setExpectedDamage(1.0);
            return rankedPath;
        }

        @Override
        public double distanceToClosestEnemy(Entity entity, Coords position, Game game) {
            return 0;
        }
    }

    @Test
    void rankPathsSendsNoProgressChatAtShippedDebugLevel() {
        Princess princess = mock(Princess.class);
        when(princess.getName()).thenReturn("Princess");
        when(princess.getPathRankerState()).thenReturn(new PathRankerState());
        Game game = mock(Game.class);
        Entity mover = mock(Entity.class);

        List<MovePath> paths = new ArrayList<>();
        for (int pathIndex = 0; pathIndex < PATH_COUNT; pathIndex++) {
            MovePath path = mock(MovePath.class);
            when(path.getEntity()).thenReturn(mover);
            when(path.getHexesMoved()).thenReturn(pathIndex);
            paths.add(path);
        }

        final Level previousLevel = LogManager.getLogger(PathRanker.class).getLevel();
        Configurator.setLevel(PathRanker.class.getName(), Level.DEBUG);
        try {
            TreeSet<RankedPath> rankedPaths = new StubPathRanker(princess).rankPaths(paths, game, 17, 0.0,
                  List.of(), List.of());

            assertEquals(PATH_COUNT, rankedPaths.size());
            verify(princess, never()).sendChat(anyString());
        } finally {
            Configurator.setLevel(PathRanker.class.getName(), previousLevel);
        }
    }
}

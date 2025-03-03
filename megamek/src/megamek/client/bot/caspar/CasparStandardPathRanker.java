/*
 * Copyright (C) 2025 The MegaMek Team. All Rights Reserved.
 *
 * This file is part of MegaMek.
 *
 * MegaMek is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License (GPL),
 * version 2 or (at your option) any later version,
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
 */

package megamek.client.bot.caspar;

import megamek.client.bot.princess.PathEnumerator;
import megamek.client.bot.princess.PathRanker;
import megamek.client.bot.princess.Princess;
import megamek.client.bot.princess.RankedPath;
import megamek.common.Coords;
import megamek.common.Entity;
import megamek.common.Game;
import megamek.common.MovePath;
import megamek.logging.MMLogger;

import java.util.List;

/**
 * The path ranker for CASPAR (Combat Algorithmic System for Predictive Analysis and Response).
 * @author Luana Coppio
 */
public class CasparStandardPathRanker extends PathRanker {
    private final static MMLogger logger = MMLogger.create(CasparStandardPathRanker.class);

    private PathEnumerator pathEnumerator;

    public CasparStandardPathRanker(Princess princess) {
        super(princess);
    }

    @Override
    protected RankedPath rankPath(MovePath path, Game game, int maxRange, double fallTolerance, List<Entity> enemies, Coords friendsCoords) {
        return null;
    }

    @Override
    public double distanceToClosestEnemy(Entity entity, Coords position, Game game) {
        return 0;
    }

    public void setPathEnumerator(PathEnumerator pathEnumerator) {
        this.pathEnumerator = pathEnumerator;
    }
}

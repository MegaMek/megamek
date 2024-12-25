/*
 * Copyright (c) 2024 - The MegaMek Team. All Rights Reserved.
 *
 *  This program is free software; you can redistribute it and/or modify it
 *  under the terms of the GNU General Public License as published by the Free
 *  Software Foundation; either version 2 of the License, or (at your option)
 *  any later version.
 *
 *  This program is distributed in the hope that it will be useful, but
 *  WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 *  or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License
 *  for more details.
 */

package megamek.client.bot.duchess;

import megamek.ai.utility.Agent;
import megamek.ai.utility.Intelligence;
import megamek.client.bot.duchess.ai.utility.tw.context.TWWorld;
import megamek.client.bot.princess.Princess;
import megamek.common.Entity;
import megamek.common.Game;
import megamek.logging.MMLogger;

public class Duchess implements Agent<Entity, Entity> {
    private static final MMLogger logger = MMLogger.create(Duchess.class);

    private final Intelligence<Entity, Entity> intelligence;
    private final TWWorld world;
    private final Princess princess;

    public Duchess(Game game, Intelligence<Entity, Entity> intelligence, Princess princess) {
        this.world = new TWWorld(game);
        this.intelligence = intelligence;
        this.princess = princess;
    }

    @Override
    public int getId() {
        return princess.getLocalPlayerNumber();
    }

    @Override
    public TWWorld getContext() {
        return world;
    }

    @Override
    public Intelligence<Entity, Entity> getIntelligence() {
        return intelligence;
    }

    public TWWorld getWorld() {
        return world;
    }

    @Override
    public Princess getClient() {
        return princess;
    }
}

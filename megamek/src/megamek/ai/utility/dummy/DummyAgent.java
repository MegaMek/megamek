/*
 * Copyright (c) 2024 - The MegaMek Team. All Rights Reserved.
 *
 * This program is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License as published by the Free
 * Software Foundation; either version 2 of the License, or (at your option)
 * any later version.
 *
 * This program is distributed in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 * or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License
 * for more details.
 *
 */

package megamek.ai.utility.dummy;

import megamek.ai.utility.Agent;
import megamek.ai.utility.Intelligence;
import megamek.ai.utility.World;
import megamek.client.IClient;
import megamek.client.bot.princess.Princess;
import megamek.common.Entity;
import megamek.logging.MMLogger;

/**
 * Dummy agent for testing purposes.
 */
public class DummyAgent implements Agent<Entity, Entity> {
    private static final MMLogger logger = MMLogger.create(DummyAgent.class);

    private final World<Entity, Entity> world;
    private final IClient client;
    private final Intelligence<Entity, Entity> intelligence;

    public DummyAgent(World<Entity, Entity> world, IClient client, Intelligence<Entity, Entity> intelligence) {
        this.world = world;
        this.client = client;
        this.intelligence = intelligence;
    }

    @Override
    public int getId() {
        return client.getLocalPlayerNumber();
    }

    @Override
    public World<Entity, Entity> getWorld() {
        return world;
    }

    @Override
    public Intelligence<Entity, Entity> getIntelligence() {
        return intelligence;
    }

    @Override
    public IClient getClient() {
        return client;
    }

}

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
import megamek.client.bot.duchess.ai.utility.tw.ClusteringService;
import megamek.client.bot.duchess.ai.utility.tw.context.TWWorld;
import megamek.client.bot.duchess.ai.utility.tw.intelligence.SimpleIntelligence;
import megamek.client.bot.duchess.ai.utility.tw.profile.TWProfile;
import megamek.client.bot.princess.*;
import megamek.common.Entity;
import megamek.logging.MMLogger;

import java.util.HashMap;

public class Duchess extends Princess implements Agent<Entity, Entity> {
    private static final MMLogger logger = MMLogger.create(Duchess.class);

    private final SimpleIntelligence intelligence;
    private final TWWorld world;

    public Duchess(String name, String host, int port, TWProfile profile) {
        super(name, host, port);
        this.intelligence = new SimpleIntelligence(this, profile);
        this.world = new TWWorld(this.getGame(), this, new ClusteringService(15d, 6));
    }

    @Override
    public void initializePathRankers() {
        super.initializePathRankers();
        pathRankers.put(PathRanker.PathRankerType.Basic, intelligence);
    }

    @Override
    public int getId() {
        return getLocalPlayerNumber();
    }

    @Override
    public Intelligence<Entity, Entity> getIntelligence() {
        return intelligence;
    }

    public TWWorld getWorld() {
        return world;
    }

    @Override
    public Duchess getClient() {
        return this;
    }
}

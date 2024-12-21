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

import megamek.client.bot.PhysicalOption;
import megamek.client.bot.princess.Princess;
import megamek.common.Coords;
import megamek.common.Entity;
import megamek.common.Minefield;
import megamek.common.MovePath;
import megamek.common.event.GamePlayerChatEvent;
import megamek.logging.MMLogger;

import java.util.Vector;

public class Duchess extends Princess{
    private static final MMLogger logger = MMLogger.create(Duchess.class);

    /**
     * Constructor - initializes a new instance of the Princess bot.
     *
     * @param name The display name.
     * @param host The host address to which to connect.
     * @param port The port on the host where to connect.
     */
    public Duchess(String name, String host, int port) {
        super(name, host, port);
    }


    @Override
    public void initialize() {

    }

    @Override
    protected void processChat(GamePlayerChatEvent ge) {

    }

    @Override
    protected void initMovement() {

    }

    @Override
    protected void initFiring() {

    }

    @Override
    protected MovePath calculateMoveTurn() {
        return null;
    }

    @Override
    protected void calculateFiringTurn() {

    }

    @Override
    protected void calculateDeployment() {

    }

    @Override
    protected PhysicalOption calculatePhysicalTurn() {
        return null;
    }

    @Override
    protected MovePath continueMovementFor(Entity entity) {
        return null;
    }

    @Override
    protected Vector<Minefield> calculateMinefieldDeployment() {
        return null;
    }

    @Override
    protected Vector<Coords> calculateArtyAutoHitHexes() {
        return null;
    }

    @Override
    protected void checkMorale() {

    }
}

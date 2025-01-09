/*
 * Copyright (c) 2025 - The MegaMek Team. All Rights Reserved.
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
package megamek.common.autoresolve.acar;

import megamek.client.AbstractClient;
import megamek.client.IClient;
import megamek.common.IGame;
import megamek.common.Player;

import java.util.Map;

public class SimulatedClient implements IClient {

    private final IGame game;
    private final Player localPlayer;

    public SimulatedClient(IGame game) {
        this.game = game;
        this.localPlayer = game.getPlayer(0);
    }

    @Override
    public IGame getGame() {
        return game;
    }

    @Override
    public int getLocalPlayerNumber() {
        return localPlayer.getId();
    }

    @Override
    public Player getLocalPlayer() {
        return localPlayer;
    }


    // The following methods are not used in the context of the Abstract Combat Auto Resolve
    @Override
    public boolean connect() {
        return false;
    }

    @Override
    public String getName() {
        return "";
    }

    @Override
    public int getPort() {
        return 0;
    }

    @Override
    public String getHost() {
        return "";
    }

    @Override
    public boolean isMyTurn() {
        return false;
    }

    @Override
    public void setLocalPlayerNumber(int localPlayerNumber) {}

    @Override
    public Map<String, AbstractClient> getBots() {
        return Map.of();
    }

    @Override
    public void sendDone(boolean done) {}

    @Override
    public void sendChat(String message) {}

    @Override
    public void die() {}
}

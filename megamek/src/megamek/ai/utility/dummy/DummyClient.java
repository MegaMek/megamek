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

import megamek.client.AbstractClient;
import megamek.client.IClient;
import megamek.common.IGame;
import megamek.logging.MMLogger;

import java.util.Map;

/**
 * Dummy client for testing purposes.
 */
public class DummyClient implements IClient {
    private static final MMLogger logger = MMLogger.create(DummyClient.class);
    private final IGame game;
    private int localPlayerNumber = -1;
    private boolean myTurn;

    public DummyClient(IGame game) {
        this.game = game;
    }

    @Override
    public String getName() {
        return "DummyClient";
    }

    @Override
    public IGame getGame() {
        return game;
    }

    @Override
    public int getLocalPlayerNumber() {
        return localPlayerNumber;
    }

    public void setMyTurn(boolean myTurn) {
        this.myTurn = myTurn;
    }

    @Override
    public boolean isMyTurn() {
        return myTurn;
    }

    @Override
    public void setLocalPlayerNumber(int localPlayerNumber) {
        this.localPlayerNumber = localPlayerNumber;
    }

    @Override
    public void sendChat(String message) {
        logger.debug("Chat: {}", message);
    }

    @Override
    public int getPort() {
        return 0;
    }

    @Override
    public String getHost() {
        return "0.0.0.0";
    }

    @Override
    public void die() {
        // NOT IMPLEMENTED
    }

    @Override
    public boolean connect() {
        return false;
    }

    @Override
    public Map<String, AbstractClient> getBots() {
        // NOT IMPLEMENTED
        return Map.of();
    }

    @Override
    public void sendDone(boolean done) {
        // NOT IMPLEMENTED
    }

}

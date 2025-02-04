/*
 * Copyright (c) 2025 - The MegaMek Team. All Rights Reserved.
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

package megamek.client;


import io.sentry.Sentry;
import io.sentry.transport.ReusableCountLatch;
import megamek.client.bot.princess.Princess;
import megamek.common.Game;
import megamek.common.MMRandom;
import megamek.common.Player;
import megamek.common.event.GameListenerAdapter;
import megamek.common.options.IBasicOption;
import megamek.common.options.OptionsConstants;
import megamek.logging.MMLogger;
import megamek.server.Server;

import java.io.File;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;
import java.util.Vector;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

class GameThread extends Thread implements CloseClientListener {
    private static final MMLogger logger = MMLogger.create(GameThread.class);

    private final Server server;
    protected Client watcher;
    private final int timeout;
    private final File saveFile;
    private final CountDownLatch gameIsLive;
    private final ReusableCountLatch reusableCountdownLatch = new ReusableCountLatch(0);
    private final Map<String, AbstractClient> localBots = new HashMap<>();
    /**
     * GameThread
     * <p>
     *     Initializes a new thread for a game.
     * </p>
     * @param timeout    Timeout in seconds
     * @param watcher    The client that will watch the game
     */
    public GameThread(int timeout, Server server, Client watcher, File saveFile) {
        this.watcher = watcher;
        this.timeout = timeout;
        this.server = server;
        this.saveFile = saveFile;
        this.gameIsLive = new CountDownLatch(timeout);
    }
    // endregion Constructors

    public Client getClient() {
        return watcher;
    }

    protected Map<String, AbstractClient> getLocalBots() {
        return localBots;
    }

    private void sendAction(Runnable runnable) {
        runnable.run();
        try {
            Thread.sleep(1000);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

    private boolean isSetup() {
        return (watcher.getGame() != null) && watcher.getGame().getPhase().isLounge();
    }

    @Override
    public void run() {
        watcher.addCloseClientListener(this);
        initializeListeners();
        connectToServer();
        loop();
    }

    private void loop() {
        try {
            if (isSetup()) {
                initializeWatcher();
                initializePlayers();
            }
            boolean result = gameIsLive.await(timeout + 2, TimeUnit.SECONDS);
            if (!result) {
                logger.error("Game timed out");
            }
        } catch (Exception e) {
            Sentry.captureException(e);
            logger.error("", e);
        } finally {
            die();
        }
    }

    private void initializeListeners() {
        watcher.getGame().addGameListener(new GameListenerAdapter() {

        });
    }

    private void initializeWatcher() {
        sendAction(watcher::sendPlayerInfo);
    }

    private void initializePlayers() {
        var ghosts = server.getGame().getPlayersList().stream()
            .filter(Player::isBot)
            .sorted(Comparator.comparingInt(Player::getId)).toList();

        for (var ghost : ghosts) {
            var behavior = ((Game) server.getGame()).getBotSettings().get(ghost.getName());
            Princess botClient = Princess.createPrincess(ghost.getName(), server.getHost(), server.getPort(), behavior);
            if (botClient.connect()) {
                getLocalBots().put(botClient.getName(), botClient);
                int retryCount = 0;
                while ((botClient.getLocalPlayer() == null)
                    && (retryCount++ < 250)) {
                    try {
                        Thread.sleep(50);
                    } catch (Exception ignored) {

                    }
                }

                sendAction(botClient::sendPlayerInfo);
            } else {
                throw new RuntimeException("Failed to connect to server");
            }
        }

        for (var bot : getLocalBots().values()) {
            bot.sendDone(true);
        }
        sendAction(() -> getClient().sendDone(true));
    }

    private void connectToServer() {
        try {
            watcher.connect();
        } catch (Exception ex) {
            logger.error(ex, "MegaMek client failed to connect to server");
            throw new RuntimeException("Failed to connect to server");
        }
        int safetyLatch = 200;
        while (watcher.getLocalPlayer() == null || watcher.getGame().getPhase().isUnknown()) {
            try {
                Thread.sleep(50);
            } catch (InterruptedException e) {
                throw new RuntimeException("Failed to connect to server");
            }
            safetyLatch--;
            if (safetyLatch <= 0) {
                logger.error("Failed to connect to server");
                throw new RuntimeException("Failed to connect to server");
            }
        }
        if (!server.loadGame(saveFile)) {
            throw new RuntimeException("Failed to load game");
        }
    }

    @Override
    public void clientClosed() {
        requestStop();
    }

    public void requestStop() {
        while(gameIsLive.getCount() > 0) {
            gameIsLive.countDown();
        }
    }

    public void die() {
        if (watcher != null) {
            watcher.die();
            watcher = null;
        }
        System.gc();
    }
}

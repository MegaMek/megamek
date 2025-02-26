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

package megamek.utilities;

import io.sentry.Sentry;
import megamek.client.AbstractClient;
import megamek.client.Client;
import megamek.client.CloseClientListener;
import megamek.client.HeadlessClient;
import megamek.client.bot.princess.Princess;
import megamek.client.ui.swing.*;
import megamek.client.ui.swing.util.MegaMekController;
import megamek.common.Game;
import megamek.common.MMRandom;
import megamek.common.MekSummaryCache;
import megamek.common.Player;
import megamek.common.enums.GamePhase;
import megamek.common.event.GameListenerAdapter;
import megamek.common.event.GamePhaseChangeEvent;
import megamek.common.net.marshalling.SanityInputFilter;
import megamek.common.options.IBasicOption;
import megamek.common.options.OptionsConstants;
import megamek.common.preference.PreferenceManager;
import megamek.logging.MMLogger;
import megamek.server.Server;
import megamek.server.totalwarfare.TWGameManager;

import javax.swing.*;
import java.io.File;
import java.io.IOException;
import java.io.ObjectInputFilter;
import java.util.*;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static megamek.MMConstants.*;

/**
 * QuickGameRunner
 * This class is used to run a game without minimal preparation
 * all it needs is a game file to load and it will populate the bots in the game
 * and run the game the predefined number of turns, or until the game ends or until
 * it times out.
 */
public class QuickGameRunner {
    private static final MMLogger logger = MMLogger.create(QuickGameRunner.class);

    static {
        MekSummaryCache.getInstance();
        ObjectInputFilter.Config.setSerialFilter(new SanityInputFilter());
    }

    private Server server;
    private Client client;
    private final Random random = new Random();
    private final GUIType guiType;

    public enum GUIType {
        DEFAULT,
        COMMANDER,
        NONE,
    }

    /**
     * QuickGameRunner, initializes the game and runs it
     * @param gameFile The game file to load
     * @param GUIType The GUI type to use, Default is ClientGUI, Commander is CommanderGUI, None does not load a GUI at all.
     * @param roundsLimit  The number of rounds to run the game
     * @param timeoutMinutes  The maximum number of minutes to wait for the game to complete
     */
    public QuickGameRunner(File gameFile, GUIType GUIType, int roundsLimit, int timeoutMinutes) {
        this.guiType = GUIType;
        try {
            initialize();
            if (gameFile != null) {
                run(gameFile, roundsLimit, timeoutMinutes);
            }
        } catch (Exception e) {
            logger.fatal(e, "Failed to start game");
        }
        cleanup();
    }

    private void initialize() throws Exception {
        TWGameManager gameManager = new TWGameManager();
        server = new Server(null, random.nextInt(MIN_PORT, MAX_PORT), gameManager, false, "", null, true);
        Thread.sleep(1000);
        PreferenceManager.getClientPreferences().setStampFilenames(true);
    }

    private void cleanup() {
        System.exit(0);
    }

    private void run(File gameFile, int roundsLimit, int timeoutMinutes) throws Exception {
        CountDownLatch roundCounter = startGame(gameFile, new GameListenerAdapter(), roundsLimit);
        if (roundCounter.await(timeoutMinutes, TimeUnit.MINUTES)) {
            logger.info("Game completed successfully");
        } else {
            logger.error("Game timed out");
        }
        PreferenceManager.getClientPreferences().setAskForVictoryList(true);
    }

    private CountDownLatch startGame(File saveFile, GameListenerAdapter gameListenerAdapter, int rounds) throws IOException {
        assert rounds > 0;
        switch (guiType) {
            case COMMANDER, NONE:
                client = new HeadlessClient("watcher", LOCALHOST_IP, server.getPort());
                break;
            default:
                client = new Client("watcher", LOCALHOST_IP, server.getPort());
                break;
        }

        CountDownLatch roundCounter = new CountDownLatch(rounds);
        client.getGame().addGameListener(gameListenerAdapter);
        client.getGame().addGameListener(new GameListenerAdapter() {
            @Override
            public void gamePhaseChange(GamePhaseChangeEvent e) {
                if (e.getNewPhase() == GamePhase.END_REPORT) {
                    roundCounter.countDown();
                    if (roundCounter.getCount() == 1) {
                        client.sendChat("/victory");
                    }
                } else if (e.getNewPhase() == GamePhase.VICTORY) {
                    while (roundCounter.getCount() > 0) {
                        roundCounter.countDown();
                    }
                }
            }
        });
        GameThread gameThread = new GameThread(600, server, client, saveFile, guiType);
        gameThread.start();
        return roundCounter;
    }

    /**
     * GameThread
     * Based on the GameThread from MekHQ, this thread serves to separate the place where we setup some parts of the game
     * from the place where we actually run the game.
     */
    private static class GameThread extends Thread implements CloseClientListener {
        private static final MMLogger logger = MMLogger.create(GameThread.class);

        private final Server server;
        protected Client watcher;
        protected IClientGUI gui;
        private final int timeout;
        private final File saveFile;
        private final CountDownLatch gameIsLive;
        private final Map<String, AbstractClient> localBots = new HashMap<>();
        private MegaMekController controller;
        private final GUIType guiType;
        /**
         * GameThread
         * <p>
         *     Initializes a new thread for a game.
         * </p>
         * @param timeout    Timeout in seconds
         * @param watcher    The client that will watch the game
         */
        public GameThread(int timeout, Server server, Client watcher, File saveFile, GUIType guiType) {
            this.watcher = watcher;
            this.timeout = timeout;
            this.server = server;
            this.saveFile = saveFile;
            this.gameIsLive = new CountDownLatch(timeout);
            this.guiType = guiType;
        }
        // endregion Constructors

        private Client getClient() {
            return watcher;
        }

        private Map<String, AbstractClient> getLocalBots() {
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

        }

        private void createController() {
            MegaMekGUI megaMekGUI = new MegaMekGUI();
            megaMekGUI.createController();
            controller = MegaMekGUI.getKeyDispatcher();
        }

        private void initializeWatcher() {
            createController();
            watcher.addCloseClientListener(this);
            watcher.getGame().addGameListener(new GameListenerAdapter());

            switch (guiType) {
                case COMMANDER:
                    createController();
                    gui = new CommanderGUI(watcher, controller);
                    controller.clientgui = gui;
                    gui.initialize();
                    break;
                case NONE:
                    break;
                default:
                    gui = new ClientGUI(watcher, controller);
                    controller.clientgui = gui;
                    gui.initialize();
                    break;
            }

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

        private void disconnectGuiSilently() {
            if (gui instanceof IDisconnectSilently disconnectSilently) {
                disconnectSilently.setDisconnectQuietly(true);
            }
        }

        public void die() {
            try {
                if (gui != null) {
                    disconnectGuiSilently();
                    SwingUtilities.invokeLater(() -> gui.die());
                }
            } catch (Exception e) {
                logger.error(e, "Failed to close GUI");
                gui = null;
            }
        }
    }

    /**
     * Main
     * @param args The command line arguments
     */
    public static void main(String[] args) {
        if (args.length < 1) {
            System.out.println("Usage: QuickGameRunner <gameFile> [guiType] [rounds] [timeout]: GUITypes are 0=DEFAULT, 1=COMMANDER, 2=NONE");
            System.out.println(" - The game to be loaded need to have all the players with units set as bots in the save file");
            System.out.println(" - rounds is a limit of rounds to run the game until it stops");
            System.out.println(" - timeout is a number of minutes to wait the game completion, it will kill the game if it takes longer than this");
            System.exit(1);
        }

        File gameFile = new File(args[0]);
        int rounds = 2;
        int timeout = 5;
        GUIType guiType1 = GUIType.DEFAULT;
        if (args.length > 1) {
            guiType1 = GUIType.values()[Integer.parseInt(args[1])];
        }
        if (args.length > 2) {
            rounds = Integer.parseInt(args[2]);
        }
        if (args.length > 3) {
            timeout = Integer.parseInt(args[3]);
        }
        PreferenceManager.getClientPreferences().setAskForVictoryList(false);
        new QuickGameRunner(gameFile, guiType1, rounds, timeout);
    }
}

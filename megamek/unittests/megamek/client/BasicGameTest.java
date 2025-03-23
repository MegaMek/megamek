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

import megamek.MMConstants;
import megamek.common.*;
import megamek.common.enums.GamePhase;
import megamek.common.event.*;
import megamek.common.net.marshalling.SanityInputFilter;
import megamek.server.Server;
import megamek.server.totalwarfare.TWGameManager;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;

import java.io.File;
import java.io.IOException;
import java.io.ObjectInputFilter;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import static megamek.MMConstants.*;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

@EnabledIfEnvironmentVariable(named = "ENABLE_TW_TESTS", matches = "true")
public class BasicGameTest {

    private TWGameManager gameManager;
    private Server server;
    private HeadlessClient client;
    private Random random = new Random();
    private GameThread gameThread;

    private static Map<String, File> testSaves = Map.of(
        "one mek each", new File("testresources/data/scenarios/testbot/lounge_1x1_grasslands.sav.gz"));

    @BeforeAll
    public static void setUpClass() {
        MekSummaryCache.getInstance();
        ObjectInputFilter.Config.setSerialFilter(new SanityInputFilter());
    }

    @BeforeEach
    public void setUp() throws Exception {
        Compute.d6();
        gameManager = new TWGameManager();
        server = new Server(null, random.nextInt(MMConstants.MIN_PORT_FOR_QUICK_GAME, MMConstants.MAX_PORT),
            gameManager, false, "", null, true);
        Thread.sleep(1000);
        client = new HeadlessClient("watcher", LOCALHOST_IP, server.getPort());
    }

    @AfterEach
    public void tearDown() {
        if (client != null) {
            client.die();
            client = null;
        }
        if (server != null) {
            server.die();
            server = null;
        }
        if (gameThread != null) {
            gameThread.requestStop();
            gameThread.die();
            gameThread = null;
        }
        gameManager = null;
    }

    private CountDownLatch startGame(File saveFile, GameListenerAdapter gameListenerAdapter, int rounds)
        throws IOException {
        assert rounds > 0;
        assertTrue(saveFile.exists() && saveFile.canRead());

        CountDownLatch roundCounter = new CountDownLatch(rounds);
        client.getGame().addGameListener(gameListenerAdapter);
        client.getGame().addGameListener(new GameListenerAdapter() {
            @Override
            public void gamePhaseChange(GamePhaseChangeEvent e) {
                if (e.getNewPhase() == GamePhase.END_REPORT) {
                    roundCounter.countDown();
                    if (roundCounter.getCount() == 0) {
                        gameThread.requestStop();
                    }
                }
            }
        });
        gameThread = new GameThread(600, server, client, saveFile);
        gameThread.start();
        return roundCounter;
    }

    @Test
    public void testGameRun() throws Exception {
        AtomicBoolean gameComplete = new AtomicBoolean(false);
        int turns = 6;
        AtomicReference<CountDownLatch> roundCounterRef = new AtomicReference<>();
        CountDownLatch roundCounter = startGame(testSaves.get("one mek each"), new GameListenerAdapter() {
            @Override
            public void gamePhaseChange(GamePhaseChangeEvent e) {
                if (e.getNewPhase() == GamePhase.INITIATIVE_REPORT) {
                    if (client.getGame().getCurrentRound() == 2) {
                        client.sendChat("/victory");
                    }

                } else if (e.getOldPhase() == GamePhase.LOUNGE) {
                    System.out.println("Game started");
                } else if (e.getNewPhase() == GamePhase.VICTORY) {
                    System.out.println("Game Finished");
                }
            }

            @Override
            public void gamePlayerChat(GamePlayerChatEvent e) {
                System.out.println(">> " + e.getMessage());
            }

            @Override
            public void gameVictory(PostGameResolution e) {
                gameComplete.set(true);
                if (roundCounterRef.get() != null) {
                    while (roundCounterRef.get().getCount() > 0) {
                        roundCounterRef.get().countDown();
                    }
                }
            }
        }, turns);
        roundCounterRef.set(roundCounter);
        if (!roundCounter.await(1, TimeUnit.MINUTES)) {
            fail();
        }
        assertTrue(gameComplete.get());
    }
}

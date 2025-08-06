/*
 * Copyright (C) 2025 The MegaMek Team. All Rights Reserved.
 *
 * This file is part of MegaMek.
 *
 * MegaMek is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License (GPL),
 * version 3 or (at your option) any later version,
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
 *
 * MechWarrior Copyright Microsoft Corporation. MegaMek was created under
 * Microsoft's "Game Content Usage Rules"
 * <https://www.xbox.com/en-US/developers/rules> and it is not endorsed by or
 * affiliated with Microsoft.
 */

package megamek.client;

import static megamek.MMConstants.LOCALHOST_IP;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import java.io.File;
import java.io.IOException;
import java.io.ObjectInputFilter;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import megamek.common.Compute;
import megamek.common.MekSummaryCache;
import megamek.common.enums.GamePhase;
import megamek.common.event.GameListenerAdapter;
import megamek.common.event.GamePhaseChangeEvent;
import megamek.common.event.GamePlayerChatEvent;
import megamek.common.event.PostGameResolution;
import megamek.common.net.marshalling.SanityInputFilter;
import megamek.server.Server;
import megamek.server.totalwarfare.TWGameManager;
import megamek.utils.ServerFactory;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;

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
        server = ServerFactory.createServer(gameManager);
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

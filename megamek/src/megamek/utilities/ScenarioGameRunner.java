/*
 * Copyright (C) 2026 The MegaMek Team. All Rights Reserved.
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
package megamek.utilities;

import static megamek.MMConstants.LOCALHOST_IP;

import java.io.File;
import java.io.ObjectInputFilter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.function.BooleanSupplier;

import megamek.MMConstants;
import megamek.client.HeadlessClient;
import megamek.client.bot.AIType;
import megamek.client.bot.BotClient;
import megamek.client.bot.BotFactory;
import megamek.client.bot.princess.BehaviorSettings;
import megamek.client.bot.princess.BehaviorSettingsFactory;
import megamek.common.Player;
import megamek.common.enums.GamePhase;
import megamek.common.event.GameListenerAdapter;
import megamek.common.event.GamePhaseChangeEvent;
import megamek.common.game.Game;
import megamek.common.game.IGame;
import megamek.common.jacksonAdapters.BotParser;
import megamek.common.loaders.MekSummaryCache;
import megamek.common.net.marshalling.SanityInputFilter;
import megamek.common.preference.PreferenceManager;
import megamek.common.scenario.Scenario;
import megamek.common.scenario.ScenarioLoader;
import megamek.common.units.Entity;
import megamek.logging.MMLogger;
import megamek.server.Server;
import megamek.server.totalWarfare.TWGameManager;

/**
 * Runs a Scenario file headless as a fully automated bot-vs-bot game, without any GUI or human interaction.
 *
 * <p>The first faction in the scenario is claimed by a headless watcher client that automatically acknowledges
 * report phases; every other faction is played by a Princess bot, using the behavior settings declared in the
 * scenario's {@code bot:} block when present, or default behavior otherwise. The game runs until victory, the given
 * round limit, or the timeout, whichever comes first.</p>
 *
 * <p>Intended for AI testing and decision-log generation (see {@code docs/issues/princess-work-tracker.md}):
 * bot decision data is written to the BotLogger TSV and the standard logs while the game runs.</p>
 *
 * <p>Usage: {@code ScenarioGameRunner <scenarioFile> [roundsLimit] [timeoutMinutes]}</p>
 */
public class ScenarioGameRunner {
    private static final MMLogger logger = MMLogger.create(ScenarioGameRunner.class);

    private static final int DEFAULT_ROUNDS_LIMIT = 6;
    private static final int DEFAULT_TIMEOUT_MINUTES = 10;
    private static final int CONNECT_RETRY_LIMIT = 250;
    private static final int CONNECT_RETRY_SLEEP_MILLIS = 50;

    static {
        MekSummaryCache.getInstance();
        ObjectInputFilter.Config.setSerialFilter(new SanityInputFilter());
    }

    private final Server server;
    private final Scenario scenario;
    private final Game game;

    public ScenarioGameRunner(File scenarioFile) throws Exception {
        TWGameManager gameManager = new TWGameManager();
        Random random = new Random();
        server = new Server(null,
              random.nextInt(MMConstants.MIN_PORT_FOR_QUICK_GAME, MMConstants.MAX_PORT),
              gameManager, false, "", null, true);

        // The Server has already opened its socket and started a non-daemon thread; if the rest of construction
        // fails, tear it down so a failed runner cannot leak the port/thread and keep the JVM alive.
        try {
            ScenarioLoader scenarioLoader = new ScenarioLoader(scenarioFile);
            scenario = scenarioLoader.load();
            IGame loadedGame = scenario.createGame();
            if (!(loadedGame instanceof Game totalWarfareGame)) {
                throw new IllegalArgumentException("Only Total Warfare scenarios are supported: " + scenarioFile);
            }
            game = totalWarfareGame;

            server.setGame(game);
            scenario.applyDamage(gameManager);
            gameManager.calculatePlayerInitialCounts();
        } catch (Exception constructionFailure) {
            server.die();
            throw constructionFailure;
        }
    }

    /**
     * The result of a single scenario game: whether it finished (rather than timing out) and which team won,
     * defined as the sole surviving combatant team. The unit-less headless watcher is ignored, and a game that
     * ends with more than one (or no) combatant team still standing is a draw.
     *
     * @param finished    whether the game finished within the timeout
     * @param winningTeam the sole surviving combatant team, or {@link Player#TEAM_NONE} for a draw
     */
    public record GameResult(boolean finished, int winningTeam) {}

    /**
     * Connects the watcher and bots, then runs the game.
     *
     * @param roundsLimit    maximum number of rounds before the game is ended via /victory
     * @param timeoutMinutes wall-clock limit; the game is abandoned when exceeded
     *
     * @return the {@link GameResult} for this game
     */
    public GameResult runGame(int roundsLimit, int timeoutMinutes) throws Exception {
        List<Player> players = new ArrayList<>(game.getPlayersList());
        if (players.isEmpty()) {
            throw new IllegalStateException("Scenario defines no players");
        }
        players.sort(Comparator.comparingInt(Player::getId));

        Player watcherSlot = players.getFirst();
        CountDownLatch roundCounter = new CountDownLatch(roundsLimit);

        HeadlessClient watcher = new HeadlessClient(watcherSlot.getName(), LOCALHOST_IP, server.getPort());
        watcher.getGame().addGameListener(new GameListenerAdapter() {
            @Override
            public void gamePhaseChange(GamePhaseChangeEvent event) {
                GamePhase newPhase = event.getNewPhase();
                if (newPhase == GamePhase.END_REPORT) {
                    roundCounter.countDown();
                    if (roundCounter.getCount() == 1) {
                        watcher.sendChat("/victory");
                    }
                } else if (newPhase == GamePhase.VICTORY) {
                    while (roundCounter.getCount() > 0) {
                        roundCounter.countDown();
                    }
                }

                // the watcher has no units; acknowledge every report phase so the game never waits on it
                if (newPhase.isReport()) {
                    watcher.sendDone(true);
                }
            }
        });

        if (!watcher.connect()) {
            throw new IllegalStateException("Watcher client failed to connect to the local server");
        }
        waitForLocalPlayer(watcher.getName(), () -> watcher.getLocalPlayer() != null);

        for (Player botSlot : players.subList(1, players.size())) {
            BotClient botClient = BotFactory.createBot(aiTypeFor(botSlot.getName()),
                  botSlot.getName(),
                  LOCALHOST_IP,
                  server.getPort(),
                  behaviorFor(botSlot.getName()));
            if (!botClient.connect()) {
                throw new IllegalStateException("Bot failed to connect for player " + botSlot.getName());
            }
            waitForLocalPlayer(botClient.getName(), () -> botClient.getLocalPlayer() != null);
            botClient.sendPlayerInfo();
            logger.info("Connected bot for {}", botSlot.getName());
        }

        logger.info("Running scenario '{}' for up to {} rounds ({} minute timeout)",
              scenario.getName(), roundsLimit, timeoutMinutes);
        boolean finished = roundCounter.await(timeoutMinutes, TimeUnit.MINUTES);
        if (finished) {
            logger.info("Scenario game completed");
        } else {
            logger.error("Scenario game timed out");
        }
        return new GameResult(finished, determineWinningTeam(watcherSlot.getTeam()));
    }

    /**
     * Determines the winner as the sole combatant team with surviving units, ignoring the unit-less watcher.
     *
     * @param watcherTeam the team of the headless watcher, which is excluded
     *
     * @return the sole surviving combatant team, or {@link Player#TEAM_NONE} if zero or more than one remain
     */
    private int determineWinningTeam(int watcherTeam) {
        Set<Integer> survivingTeams = new TreeSet<>();
        for (Entity entity : game.getEntitiesVector()) {
            Player owner = entity.getOwner();
            if (!entity.isDestroyed() && (owner != null) && (owner.getTeam() != watcherTeam)) {
                survivingTeams.add(owner.getTeam());
            }
        }
        return (survivingTeams.size() == 1) ? survivingTeams.iterator().next() : Player.TEAM_NONE;
    }

    /**
     * Shuts down this runner's server, releasing its port and connections. Call between games when running many
     * in one process.
     */
    public void shutdown() {
        server.die();
    }

    /**
     * Returns the behavior declared for the named player in the scenario, or default behavior.
     */
    private BehaviorSettings behaviorFor(String playerName) {
        if (scenario.hasBotInfo(playerName)
              && scenario.getBotInfo(playerName) instanceof BotParser.PrincessRecord record) {
            return record.behaviorSettings();
        }
        return BehaviorSettingsFactory.getInstance().DEFAULT_BEHAVIOR;
    }

    /**
     * Returns the {@link AIType} declared for the named player in the scenario's {@code ai:} key, or
     * {@link AIType#PRINCESS} if none is declared.
     */
    public AIType aiTypeFor(String playerName) {
        if (scenario.hasBotInfo(playerName)
              && scenario.getBotInfo(playerName) instanceof BotParser.PrincessRecord record) {
            return record.aiType();
        }
        return AIType.PRINCESS;
    }

    /**
     * Maps each team to the {@link AIType}s of its bot players. The first player slot (by id) is the headless
     * watcher and is excluded, so only the competing bot teams are reported.
     *
     * @return team id to the set of bot {@link AIType}s on that team
     */
    public Map<Integer, Set<AIType>> getBotTeamAITypes() {
        Map<Integer, Set<AIType>> teamAITypes = new TreeMap<>();
        List<Player> players = new ArrayList<>(game.getPlayersList());
        if (players.isEmpty()) {
            return teamAITypes;
        }
        players.sort(Comparator.comparingInt(Player::getId));
        for (Player botSlot : players.subList(1, players.size())) {
            teamAITypes.computeIfAbsent(botSlot.getTeam(), team -> new TreeSet<>())
                  .add(aiTypeFor(botSlot.getName()));
        }
        return teamAITypes;
    }

    private void waitForLocalPlayer(String clientName, BooleanSupplier connected) throws InterruptedException {
        int retryCount = 0;
        while (!connected.getAsBoolean() && (retryCount++ < CONNECT_RETRY_LIMIT)) {
            Thread.sleep(CONNECT_RETRY_SLEEP_MILLIS);
        }
        if (!connected.getAsBoolean()) {
            throw new IllegalStateException("Client " + clientName + " failed to receive its player slot");
        }
    }

    public static void main(String[] args) {
        if (args.length < 1) {
            System.out.println("Usage: ScenarioGameRunner <scenarioFile> [roundsLimit] [timeoutMinutes]");
            System.out.println(" - scenarioFile: an MMS scenario; first faction is the headless watcher,");
            System.out.println("   all other factions are played by Princess bots");
            System.out.println(" - roundsLimit: stop the game after this many rounds (default "
                  + DEFAULT_ROUNDS_LIMIT + ")");
            System.out.println(" - timeoutMinutes: abandon the game after this long (default "
                  + DEFAULT_TIMEOUT_MINUTES + ")");
            System.exit(1);
        }

        File scenarioFile = new File(args[0]);
        int roundsLimit = DEFAULT_ROUNDS_LIMIT;
        int timeoutMinutes = DEFAULT_TIMEOUT_MINUTES;
        try {
            if (args.length > 1) {
                roundsLimit = Integer.parseInt(args[1]);
            }
            if (args.length > 2) {
                timeoutMinutes = Integer.parseInt(args[2]);
            }
        } catch (NumberFormatException e) {
            System.out.println("roundsLimit and timeoutMinutes must be whole numbers, but got: "
                  + String.join(" ", args));
            System.out.println("Usage: ScenarioGameRunner <scenarioFile> [roundsLimit] [timeoutMinutes]");
            System.exit(1);
        }

        PreferenceManager.getClientPreferences().setAskForVictoryList(false);
        // stamp gamelog.html and game_actions TSV filenames with date+time so consecutive runs
        // accumulate instead of overwriting each other
        PreferenceManager.getClientPreferences().setStampFilenames(true);

        int exitCode = 0;
        ScenarioGameRunner runner = null;
        try {
            runner = new ScenarioGameRunner(scenarioFile);
            if (!runner.runGame(roundsLimit, timeoutMinutes).finished()) {
                exitCode = 2;
            }
        } catch (Exception exception) {
            logger.fatal(exception, "Failed to run scenario game");
            exitCode = 1;
        } finally {
            if (runner != null) {
                runner.shutdown();
            }
        }
        System.exit(exitCode);
    }
}

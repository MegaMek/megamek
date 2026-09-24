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
import java.util.HashMap;
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
import megamek.client.AbstractClient;
import megamek.client.HeadlessClient;
import megamek.client.bot.AIType;
import megamek.client.bot.BotClient;
import megamek.client.bot.BotFactory;
import megamek.client.bot.princess.BehaviorSettings;
import megamek.client.bot.princess.BehaviorSettingsFactory;
import megamek.common.Player;
import megamek.common.annotations.Nullable;
import megamek.common.enums.GamePhase;
import megamek.common.event.GameListenerAdapter;
import megamek.common.event.GamePhaseChangeEvent;
import megamek.common.game.Game;
import megamek.common.game.IGame;
import megamek.common.interfaces.IEntityRemovalConditions;
import megamek.common.jacksonAdapters.BotParser;
import megamek.common.loaders.MekSummaryCache;
import megamek.common.net.marshalling.SanityInputFilter;
import megamek.common.preference.PreferenceManager;
import megamek.common.scenario.Scenario;
import megamek.common.scenario.ScenarioLoader;
import megamek.common.units.EjectedCrew;
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

    /**
     * Every client this runner has connected, so they can be disconnected when the game ends.
     *
     * <p>This matters far more than it looks when many games run in one JVM. Each client holds its own copy of the
     * {@link Game} - every entity, the board, and for a bot its path caches and precognition thread. Leaving them
     * connected leaks all of that per game, and a batch runner plays hundreds. The symptom is not a crash: the heap
     * fills, the JVM spends its time collecting garbage instead of playing, and a later game slows to a stop while
     * still technically running.</p>
     */
    private final List<AbstractClient> connectedClients = new ArrayList<>();

    /**
     * The players as loaded from the scenario, captured before the game runs. The end-of-game standings are
     * collected from this snapshot rather than from the live player list, because every bot disconnects itself
     * when the VICTORY phase is announced ({@code BotClient#changePhase}) and the server then removes its player
     * from the game entirely ({@code TWGameManager#disconnect}). That removal races the standings collection, so
     * by the time standings are read a wiped (or even winning) team's player may already be gone - see issue #8700.
     * {@code Game#removePlayer} only drops the list entry; these Player objects stay valid, and the server reuses
     * them when the bot clients connect to claim their slots.
     */
    private final List<Player> playersAtGameStart;

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
            playersAtGameStart = List.copyOf(game.getPlayersList());
        } catch (Exception constructionFailure) {
            server.die();
            throw constructionFailure;
        }
    }

    /**
     * The result of a single scenario game: whether it finished (rather than timing out), which team won -
     * defined as the sole surviving combatant team - and the end-of-game standing of every combatant team.
     * The unit-less headless watcher is ignored, and a game that ends with more than one (or no) combatant
     * team still standing is a draw.
     *
     * @param finished      whether the game finished within the timeout
     * @param winningTeam   the sole surviving combatant team, or {@link Player#TEAM_NONE} for a draw
     * @param rounds        the round the game ended on
     * @param teamStandings the end-of-game standing of each combatant team, ordered by team id
     */
    public record GameResult(boolean finished, int winningTeam, int rounds, List<TeamStanding> teamStandings) {}

    /**
     * The end-of-game standing of one combatant team. Ejected crew entities are not counted as units. Units
     * whose removal condition is neither destruction nor retreat (for example never-deployed units) appear in
     * {@code unitsFielded} only.
     *
     * @param team              the team id
     * @param unitsFielded      units the team started the game with
     * @param survivors         units still on the board and not destroyed at game end
     * @param crippledSurvivors survivors that report {@link Entity#isCrippled()}
     * @param destroyed         units destroyed, whether salvageable, devastated, or lost with their crew
     * @param fled              units that left the board in retreat, were captured, or were pushed off
     * @param bvInitial         the team's total Battle Value at game start
     * @param bvRemaining       the team's total Battle Value of usable assets still in play at game end,
     *                          per {@link Player#getBV()} (units counting toward the strength sum)
     */
    public record TeamStanding(int team, int unitsFielded, int survivors, int crippledSurvivors, int destroyed,
          int fled, int bvInitial, int bvRemaining) {}

    /** Mutable accumulator behind {@link TeamStanding}, keyed by team while walking players and entities. */
    private static final class TeamTally {
        private int unitsFielded;
        private int survivors;
        private int crippledSurvivors;
        private int destroyed;
        private int fled;
        private int bvInitial;
        private int bvRemaining;
    }

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

        connectedClients.add(watcher);
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
            connectedClients.add(botClient);
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
        return new GameResult(finished,
              determineWinningTeam(game, playersAtGameStart, watcherSlot.getTeam()),
              game.getCurrentRound(),
              collectTeamStandings(game, playersAtGameStart, watcherSlot.getTeam()));
    }

    /**
     * Collects the end-of-game standing of every combatant team: units fielded, survivors (and how many of them
     * are crippled), destroyed and fled counts, and Battle Value at start versus game end. Ejected crew entities
     * are skipped so a unit and its ejected pilot are not counted twice.
     *
     * <p>Teams and entity ownership are resolved through the game-start player snapshot, not the live player
     * list: a bot whose side was wiped disconnects at VICTORY and its player is removed from the game, which
     * would otherwise silently drop that team's standing entirely (issue #8700).</p>
     *
     * <p>Package-private and static so the wiped-team regression test can drive it without a live server.</p>
     *
     * @param game               the game to collect standings from
     * @param playersAtGameStart every player as of game start, including any since removed
     * @param watcherTeam        the team of the headless watcher, which is excluded
     *
     * @return one {@link TeamStanding} per combatant team, ordered by team id
     */
    static List<TeamStanding> collectTeamStandings(Game game, List<Player> playersAtGameStart, int watcherTeam) {
        Map<Integer, Integer> teamByPlayerId = teamByPlayerId(playersAtGameStart);
        Map<Integer, TeamTally> tallies = new TreeMap<>();

        for (Player player : playersAtGameStart) {
            if (player.getTeam() == watcherTeam) {
                continue;
            }
            TeamTally tally = tallies.computeIfAbsent(player.getTeam(), team -> new TeamTally());
            tally.unitsFielded += player.getInitialEntityCount();
            tally.bvInitial += player.getInitialBV();
            tally.bvRemaining += player.getBV();
        }

        for (Entity entity : game.getEntitiesVector()) {
            TeamTally tally = tallyFor(tallies, entity, teamByPlayerId, watcherTeam);
            if (tally == null) {
                continue;
            }
            if (entity.isDestroyed()) {
                tally.destroyed++;
            } else {
                tally.survivors++;
                if (entity.isCrippled()) {
                    tally.crippledSurvivors++;
                }
            }
        }

        for (Entity entity : game.getOutOfGameEntitiesVector()) {
            TeamTally tally = tallyFor(tallies, entity, teamByPlayerId, watcherTeam);
            if (tally == null) {
                continue;
            }
            switch (entity.getRemovalCondition()) {
                case IEntityRemovalConditions.REMOVE_SALVAGEABLE,
                     IEntityRemovalConditions.REMOVE_EJECTED,
                     IEntityRemovalConditions.REMOVE_DEVASTATED -> tally.destroyed++;
                case IEntityRemovalConditions.REMOVE_IN_RETREAT,
                     IEntityRemovalConditions.REMOVE_CAPTURED,
                     IEntityRemovalConditions.REMOVE_PUSHED -> tally.fled++;
                default -> { } // never deployed or unknown; counted in unitsFielded only
            }
        }

        List<TeamStanding> standings = new ArrayList<>();
        for (Map.Entry<Integer, TeamTally> entry : tallies.entrySet()) {
            TeamTally tally = entry.getValue();
            standings.add(new TeamStanding(entry.getKey(), tally.unitsFielded, tally.survivors,
                  tally.crippledSurvivors, tally.destroyed, tally.fled, tally.bvInitial, tally.bvRemaining));
        }
        return standings;
    }

    /**
     * Returns the tally the given entity counts toward, or {@code null} when it should not be counted:
     * watcher-team entities and entities of unknown ownership are not combatants, and ejected crew are not
     * units. Ownership is resolved by owner id against the game-start snapshot, because the owner's player
     * may already have been removed from the game (see {@link #playersAtGameStart}).
     */
    private static @Nullable TeamTally tallyFor(Map<Integer, TeamTally> tallies, Entity entity,
          Map<Integer, Integer> teamByPlayerId, int watcherTeam) {
        Integer team = teamByPlayerId.get(entity.getOwnerId());
        if ((team == null) || (team == watcherTeam) || (entity instanceof EjectedCrew)) {
            return null;
        }
        return tallies.computeIfAbsent(team, teamId -> new TeamTally());
    }

    /**
     * Determines the winner as the sole combatant team with surviving units, ignoring the unit-less watcher.
     * Entity ownership is resolved by owner id against the game-start snapshot, so a winner whose bot has
     * already disconnected (and had its player removed) is not misreported as a draw.
     *
     * <p>Package-private and static so the wiped-team regression test can drive it without a live server.</p>
     *
     * @param game               the game to determine the winner of
     * @param playersAtGameStart every player as of game start, including any since removed
     * @param watcherTeam        the team of the headless watcher, which is excluded
     *
     * @return the sole surviving combatant team, or {@link Player#TEAM_NONE} if zero or more than one remain
     */
    static int determineWinningTeam(Game game, List<Player> playersAtGameStart, int watcherTeam) {
        Map<Integer, Integer> teamByPlayerId = teamByPlayerId(playersAtGameStart);
        Set<Integer> survivingTeams = new TreeSet<>();
        for (Entity entity : game.getEntitiesVector()) {
            Integer team = teamByPlayerId.get(entity.getOwnerId());
            if (!entity.isDestroyed() && (team != null) && (team != watcherTeam)) {
                survivingTeams.add(team);
            }
        }
        return (survivingTeams.size() == 1) ? survivingTeams.iterator().next() : Player.TEAM_NONE;
    }

    /**
     * Maps each game-start player's id to their team, for resolving entity ownership after a player has been
     * removed from the game.
     */
    private static Map<Integer, Integer> teamByPlayerId(List<Player> playersAtGameStart) {
        Map<Integer, Integer> teamByPlayerId = new HashMap<>();
        for (Player player : playersAtGameStart) {
            teamByPlayerId.put(player.getId(), player.getTeam());
        }
        return teamByPlayerId;
    }

    /**
     * Disconnects every client this runner connected, then shuts down its server, releasing the port. Call between
     * games when running many in one process.
     *
     * <p>Clients are disconnected before the server dies so each one closes its socket and, for a bot, stops its
     * precognition thread. Skipping this is what makes a long batch degrade: see {@link #connectedClients}.</p>
     */
    public void shutdown() {
        for (AbstractClient client : connectedClients) {
            try {
                client.die();
            } catch (Exception exception) {
                // A client that fails to shut down cleanly must not stop the others being released.
                logger.warn(exception, "Failed to disconnect client " + client.getName());
            }
        }
        connectedClients.clear();
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

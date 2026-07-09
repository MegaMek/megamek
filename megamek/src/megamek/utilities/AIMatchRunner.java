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

import java.io.File;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

import megamek.client.bot.AIType;
import megamek.common.Player;
import megamek.common.preference.PreferenceManager;
import megamek.logging.MMLogger;

/**
 * Runs a scenario headlessly many times and reports the win rate per team (and the {@link AIType}s on each team),
 * so two bot AIs assigned via the scenario {@code ai:} key - for example Princess versus CASPAR - can be compared
 * over a batch of games. The BotLogger TSVs from every game are kept, so the battle-analyzer tooling can also
 * score decision quality per side, not just wins.
 *
 * <p>Usage: {@code AIMatchRunner <scenarioFile> [repetitions] [roundsLimit] [timeoutMinutes]}</p>
 */
public final class AIMatchRunner {
    private static final MMLogger logger = MMLogger.create(AIMatchRunner.class);

    private static final int DEFAULT_REPETITIONS = 10;
    private static final int DEFAULT_ROUNDS_LIMIT = 12;
    private static final int DEFAULT_TIMEOUT_MINUTES = 10;

    private AIMatchRunner() {}

    public static void main(String[] args) {
        if (args.length < 1) {
            System.out.println("Usage: AIMatchRunner <scenarioFile> [repetitions] [roundsLimit] [timeoutMinutes]");
            System.out.println(" - assign AIs to the scenario's bot factions with the 'ai:' key (e.g. ai: caspar)");
            System.exit(1);
        }

        File scenarioFile = new File(args[0]);
        int repetitions = parseIntArg(args, 1, DEFAULT_REPETITIONS, "repetitions");
        int roundsLimit = parseIntArg(args, 2, DEFAULT_ROUNDS_LIMIT, "roundsLimit");
        int timeoutMinutes = parseIntArg(args, 3, DEFAULT_TIMEOUT_MINUTES, "timeoutMinutes");

        // Keep each game's logs instead of overwriting them, so per-game decision data survives the batch.
        PreferenceManager.getClientPreferences().setStampFilenames(true);

        Map<Integer, Integer> teamWins = new TreeMap<>();
        Map<Integer, Set<AIType>> teamAITypes = new TreeMap<>();
        int draws = 0;
        int unfinished = 0;

        for (int gameNumber = 1; gameNumber <= repetitions; gameNumber++) {
            ScenarioGameRunner runner = null;
            try {
                runner = new ScenarioGameRunner(scenarioFile);
                if (teamAITypes.isEmpty()) {
                    teamAITypes = runner.getBotTeamAITypes();
                }
                ScenarioGameRunner.GameResult result = runner.runGame(roundsLimit, timeoutMinutes);
                if (!result.finished()) {
                    unfinished++;
                    logger.warn("Game {}/{} did not finish within the timeout", gameNumber, repetitions);
                } else if (result.winningTeam() == Player.TEAM_NONE) {
                    draws++;
                    logger.info("Game {}/{}: draw (no sole surviving team)", gameNumber, repetitions);
                } else {
                    teamWins.merge(result.winningTeam(), 1, Integer::sum);
                    logger.info("Game {}/{}: team {} {} wins", gameNumber, repetitions, result.winningTeam(),
                          teamAITypes.getOrDefault(result.winningTeam(), Set.of()));
                }
            } catch (Exception exception) {
                logger.error(exception, "Game " + gameNumber + "/" + repetitions + " failed to run");
            } finally {
                if (runner != null) {
                    runner.shutdown();
                }
            }
        }

        logger.info(formatSummary(repetitions, teamWins, teamAITypes, draws, unfinished));
        System.exit(0);
    }

    /**
     * Parses an optional integer command-line argument, or returns the default when it is not supplied. Exits
     * with a usage-style message rather than throwing if the argument is present but not an integer.
     *
     * @param args         the command-line arguments
     * @param index        the index of the argument to parse
     * @param defaultValue the value to use when the argument is not supplied
     * @param argumentName the argument name, for the error message
     *
     * @return the parsed value, or {@code defaultValue} when the argument is absent
     */
    private static int parseIntArg(String[] args, int index, int defaultValue, String argumentName) {
        if (args.length <= index) {
            return defaultValue;
        }
        try {
            return Integer.parseInt(args[index]);
        } catch (NumberFormatException exception) {
            System.out.println("Invalid " + argumentName + ": '" + args[index] + "' is not an integer");
            System.exit(1);
            return defaultValue; // unreachable: System.exit does not return
        }
    }

    private static String formatSummary(int repetitions, Map<Integer, Integer> teamWins,
          Map<Integer, Set<AIType>> teamAITypes, int draws, int unfinished) {
        StringBuilder summary = new StringBuilder(
              System.lineSeparator() + "=== AI match results over " + repetitions + " game(s) ===");
        for (Map.Entry<Integer, Set<AIType>> entry : teamAITypes.entrySet()) {
            summary.append(System.lineSeparator())
                  .append("  Team ").append(entry.getKey())
                  .append(' ').append(entry.getValue())
                  .append(": ").append(teamWins.getOrDefault(entry.getKey(), 0)).append(" win(s)");
        }
        summary.append(System.lineSeparator()).append("  Draws: ").append(draws);
        if (unfinished > 0) {
            summary.append(System.lineSeparator()).append("  Unfinished (timeout): ").append(unfinished);
        }
        return summary.toString();
    }
}

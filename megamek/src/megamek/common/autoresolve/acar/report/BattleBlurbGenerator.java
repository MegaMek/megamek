package megamek.common.autoresolve.acar.report;

import java.util.Random;

import megamek.common.Board;
import megamek.common.autoresolve.acar.SimulationContext;
import megamek.common.autoresolve.acar.SimulationManager;
import megamek.common.autoresolve.component.Formation;
import megamek.utilities.BoardsTagger;


/**
 * Generates narrative summary text for battle reports based on the context and outcome.
 * Uses localized text templates from properties files.
 * @author Luana Coppio
 */
public class BattleBlurbGenerator {

    // Intensity descriptors
    private static final String[] INTENSITY_KEYS = {
          "acar.{blurb}.intensity.fierce",
          "acar.{blurb}.intensity.intense",
          "acar.{blurb}.intensity.brutal",
          "acar.{blurb}.intensity.tactical",
          "acar.{blurb}.intensity.calculated"
    };

    // Short battle length descriptors
    private static final String[] SHORT_LENGTH_KEYS = {
          "acar.{blurb}.length.short.decisive",
          "acar.{blurb}.length.short.lightning",
          "acar.{blurb}.length.short.brief",
          "acar.{blurb}.length.short.flash",
          "acar.{blurb}.length.short.swift"
    };

    // Medium battle length descriptors
    private static final String[] MEDIUM_LENGTH_KEYS = {
          "acar.{blurb}.length.medium.methodical",
          "acar.{blurb}.length.medium.steady",
          "acar.{blurb}.length.medium.balanced",
          "acar.{blurb}.length.medium.moderate",
          "acar.{blurb}.length.medium.average"
    };

    // Long battle length descriptors
    private static final String[] LONG_LENGTH_KEYS = {
          "acar.{blurb}.length.long.drawn",
          "acar.{blurb}.length.long.grueling",
          "acar.{blurb}.length.long.exhausting",
          "acar.{blurb}.length.long.endless",
          "acar.{blurb}.length.long.attrition"
    };

    // Terrain descriptors
    private static final String[] TERRAIN_KEYS = {
          "acar.{blurb}.terrain.rugged",
          "acar.{blurb}.terrain.urban",
          "acar.{blurb}.terrain.plains",
          "acar.{blurb}.terrain.forest",
          "acar.{blurb}.terrain.highlands"
    };

    // Tactical descriptors
    private static final String[] TACTICAL_KEYS = {
          "acar.{blurb}.tactical.precise",
          "acar.{blurb}.tactical.bold",
          "acar.{blurb}.tactical.cautious",
          "acar.{blurb}.tactical.aggressive",
          "acar.{blurb}.tactical.defensive"
    };

    // Standard outcome descriptors
    private static final String[] STANDARD_OUTCOME_KEYS = {
          "acar.{blurb}.outcome.standard.clear",
          "acar.{blurb}.outcome.standard.strategic",
          "acar.{blurb}.outcome.standard.tactical",
          "acar.{blurb}.outcome.standard.successful",
          "acar.{blurb}.outcome.standard.decisive"
    };

    // High casualty outcome descriptors
    private static final String[] HIGH_CASUALTY_OUTCOME_KEYS = {
          "acar.{blurb}.outcome.highCasualty.significant",
          "acar.{blurb}.outcome.highCasualty.heavy",
          "acar.{blurb}.outcome.highCasualty.devastating",
          "acar.{blurb}.outcome.highCasualty.pyrrhic",
          "acar.{blurb}.outcome.highCasualty.aftermath"
    };

    // Narrow victory outcome descriptors
    private static final String[] NARROW_VICTORY_OUTCOME_KEYS = {
          "acar.{blurb}.outcome.narrow.margin",
          "acar.{blurb}.outcome.narrow.contested",
          "acar.{blurb}.outcome.narrow.thin",
          "acar.{blurb}.outcome.narrow.hardWon",
          "acar.{blurb}.outcome.narrow.almost"
    };

    // Short battle second paragraph starters
    private static final String[] SHORT_SECOND_PARA_KEYS = {
          "acar.{blurb}.secondPara.short.showcased",
          "acar.{blurb}.secondPara.short.confrontation",
          "acar.{blurb}.secondPara.short.highlighted",
          "acar.{blurb}.secondPara.short.demonstrated",
          "acar.{blurb}.secondPara.short.revealed"
    };

    // Medium battle second paragraph starters
    private static final String[] MEDIUM_SECOND_PARA_KEYS = {
          "acar.{blurb}.secondPara.medium.throughout",
          "acar.{blurb}.secondPara.medium.unfolded",
          "acar.{blurb}.secondPara.medium.course",
          "acar.{blurb}.secondPara.medium.balanced",
          "acar.{blurb}.secondPara.medium.progressed"
    };

    // Long battle second paragraph starters
    private static final String[] LONG_SECOND_PARA_KEYS = {
          "acar.{blurb}.secondPara.long.extended",
          "acar.{blurb}.secondPara.long.course",
          "acar.{blurb}.secondPara.long.protracted",
          "acar.{blurb}.secondPara.long.grueling",
          "acar.{blurb}.secondPara.long.endurance"
    };

    // Key factors descriptors
    private static final String[] KEY_FACTORS_KEYS = {
          "acar.{blurb}.keyFactors.key",
          "acar.{blurb}.keyFactors.critical",
          "acar.{blurb}.keyFactors.decisive",
          "acar.{blurb}.keyFactors.important",
          "acar.{blurb}.keyFactors.significant"
    };

    // Specific tactics descriptors
    private static final String[] TACTICS_KEYS = {
          "acar.{blurb}.tactics.flanking",
          "acar.{blurb}.tactics.firepower",
          "acar.{blurb}.tactics.coordinated",
          "acar.{blurb}.tactics.cover",
          "acar.{blurb}.tactics.precision"
    };

    // Unit performance descriptors
    private static final String[] PERFORMANCE_KEYS = {
          "acar.{blurb}.performance.notable",
          "acar.{blurb}.performance.exceptional",
          "acar.{blurb}.performance.strategic",
          "acar.{blurb}.performance.adaptability",
          "acar.{blurb}.performance.cohesion"
    };

    // Standard conclusion descriptors
    private static final String[] STANDARD_CONCLUSION_KEYS = {
          "acar.{blurb}.conclusion.standard.tactical",
          "acar.{blurb}.conclusion.standard.coordinated",
          "acar.{blurb}.conclusion.standard.awareness",
          "acar.{blurb}.conclusion.standard.disciplined",
          "acar.{blurb}.conclusion.standard.resource"
    };

    // Short battle conclusion descriptors
    private static final String[] SHORT_CONCLUSION_KEYS = {
          "acar.{blurb}.conclusion.short.decisive",
          "acar.{blurb}.conclusion.short.precision",
          "acar.{blurb}.conclusion.short.initiative",
          "acar.{blurb}.conclusion.short.readiness",
          "acar.{blurb}.conclusion.short.preparation"
    };

    // Long battle conclusion descriptors
    private static final String[] LONG_CONCLUSION_KEYS = {
          "acar.{blurb}.conclusion.long.endurance",
          "acar.{blurb}.conclusion.long.adaptability",
          "acar.{blurb}.conclusion.long.logistical",
          "acar.{blurb}.conclusion.long.persistence",
          "acar.{blurb}.conclusion.long.reserves"
    };

    // High casualty conclusion descriptors
    private static final String[] HIGH_CASUALTY_CONCLUSION_KEYS = {
          "acar.{blurb}.conclusion.highCasualty.pyrrhic",
          "acar.{blurb}.conclusion.highCasualty.reminder",
          "acar.{blurb}.conclusion.highCasualty.thin",
          "acar.{blurb}.conclusion.highCasualty.depleted",
          "acar.{blurb}.conclusion.highCasualty.threshold"
    };

    // Format templates
    private static final String SHORT_BATTLE_FORMAT = "acar.blurb.format.short";
    private static final String MEDIUM_BATTLE_FORMAT = "acar.blurb.format.medium";
    private static final String LONG_BATTLE_FORMAT = "acar.blurb.format.long";

    // Team-related keys
    private static final String TEAM_WINNING_KEY = "acar.blurb.team.winning";
    private static final String DEFAULT_DRAW_KEY = "acar.blurb.default.draw";
    public static final String BLURB_LOCATION_PLACEHOLDER = "{blurb}";

    // The context and manager instances
    private final SimulationContext context;
    private final SimulationManager simulationManager;
    private final Board board;
    private final Random rand;

    /**
     * Constructor for BattleSummaryGenerator
     *
     * @param simulationManager The simulation manager with access to victory results
     */
    public BattleBlurbGenerator(SimulationManager simulationManager) {
        this.simulationManager = simulationManager;
        this.context = simulationManager.getGame();
        this.rand = new Random(context.getSeed());
        this.board = simulationManager.getGame().getBoard();
    }

    /**
     * Generates a narrative battle summary based on the simulation results.
     *
     * @return Formatted battle summary text with appropriate variations
     */
    public PublicReportEntry generateBlurb() {
        String location;
        if (board.onGround()) {
            location = "ground";
        } else if (board.inAtmosphere()) {
            location = "atmosphere";
        } else if (board.inSpace()) {
            location = "space";
        } else {
            throw new RuntimeException("unknown board type");
        }
        // Get context information to determine which variations to use
        int rounds = context.getCurrentRound();
        boolean isLongBattle = rounds > 5;
        boolean isVeryShortBattle = rounds <= 2;
        int destroyedUnits = (int) context.getGraveyard().stream().filter(e -> e instanceof Formation).count();
        int remainingUnits = context.getActiveFormations().size();
        int totalUnits = remainingUnits + destroyedUnits;

        double casualtyRate = 1.0 - ((double) remainingUnits / totalUnits);
        boolean isHighCasualty = casualtyRate > 0.5;

        // Select descriptors based on battle characteristics
        String intensityKey = selectRandom(INTENSITY_KEYS).replace(BLURB_LOCATION_PLACEHOLDER, location);
        String lengthKey = selectBattleLengthKey(isVeryShortBattle, isLongBattle).replace(BLURB_LOCATION_PLACEHOLDER, location);
        String terrainKey = selectRandom(TERRAIN_KEYS).replace(BLURB_LOCATION_PLACEHOLDER, location);
        String tacticalKey = selectRandom(TACTICAL_KEYS).replace(BLURB_LOCATION_PLACEHOLDER, location);

        // Calculate if it was a close battle
        boolean isCloseVictory = isCloseVictory();

        // Select outcome descriptor
        String outcomeKey = selectOutcomeKey(isHighCasualty, isCloseVictory).replace(BLURB_LOCATION_PLACEHOLDER, location);

        // Select second paragraph starter based on battle length
        String secondParaKey = selectSecondParaKey(isVeryShortBattle, isLongBattle).replace(BLURB_LOCATION_PLACEHOLDER, location);

        // Select second tactical descriptor for variety
        String secondTacticalKey = selectRandom(TACTICAL_KEYS).replace(BLURB_LOCATION_PLACEHOLDER, location);

        // Select additional descriptors
        String keyFactorsKey = selectRandom(KEY_FACTORS_KEYS).replace(BLURB_LOCATION_PLACEHOLDER, location);
        String tacticsKey = selectRandom(TACTICS_KEYS).replace(BLURB_LOCATION_PLACEHOLDER, location);
        String performanceKey = selectRandom(PERFORMANCE_KEYS).replace(BLURB_LOCATION_PLACEHOLDER, location);

        // Determine winning team description
        String teamDescription = determineTeamDescription();

        // Select conclusion descriptor based on battle characteristics
        String conclusionKey = selectConclusionKey(isHighCasualty, isVeryShortBattle, isLongBattle).replace(
              BLURB_LOCATION_PLACEHOLDER, location);

        // Create the formatted summary using the appropriate template based on battle type
        return createFormattedSummary(
              isVeryShortBattle, isLongBattle,
              intensityKey, lengthKey, terrainKey, tacticalKey, outcomeKey,
              secondParaKey, secondTacticalKey, keyFactorsKey, tacticsKey,
              performanceKey, teamDescription, conclusionKey
        );
    }

    /**
     * Selects a random key from the provided array.
     *
     * @param keys Array of property keys
     * @return A randomly selected key
     */
    private String selectRandom(String[] keys) {
        var tags = BoardsTagger.tagsFor(board);

        return keys[rand.nextInt(keys.length)];
    }

    /**
     * Selects appropriate battle length key based on duration.
     */
    private String selectBattleLengthKey(boolean isVeryShortBattle, boolean isLongBattle) {
        if (isVeryShortBattle) {
            return selectRandom(SHORT_LENGTH_KEYS);
        } else if (isLongBattle) {
            return selectRandom(LONG_LENGTH_KEYS);
        } else {
            return selectRandom(MEDIUM_LENGTH_KEYS);
        }
    }

    /**
     * Determines if the battle was a close victory.
     */
    private boolean isCloseVictory() {
        int winnerUnitCount = 0;
        int loserUnitCount = 0;
        var victoryResult = simulationManager.getCurrentVictoryResult();

        for (var team : context.getTeams()) {
            int teamRemainingUnits = team.players().stream().mapToInt(e -> context.getActiveFormations(e).size()).sum();

            if (team.getId() == victoryResult.getWinningTeam()) {
                winnerUnitCount += teamRemainingUnits;
            } else {
                loserUnitCount += teamRemainingUnits;
            }
        }

        return winnerUnitCount > 0 && loserUnitCount > 0 &&
                     ((double)winnerUnitCount / (winnerUnitCount + loserUnitCount)) < 0.6;
    }

    /**
     * Selects appropriate outcome key based on battle characteristics.
     */
    private String selectOutcomeKey(boolean isHighCasualty, boolean isCloseVictory) {
        if (isHighCasualty) {
            return selectRandom(HIGH_CASUALTY_OUTCOME_KEYS);
        } else if (isCloseVictory) {
            return selectRandom(NARROW_VICTORY_OUTCOME_KEYS);
        } else {
            return selectRandom(STANDARD_OUTCOME_KEYS);
        }
    }

    /**
     * Selects appropriate second paragraph starter based on battle length.
     */
    private String selectSecondParaKey(boolean isVeryShortBattle, boolean isLongBattle) {
        if (isVeryShortBattle) {
            return selectRandom(SHORT_SECOND_PARA_KEYS);
        } else if (isLongBattle) {
            return selectRandom(LONG_SECOND_PARA_KEYS);
        } else {
            return selectRandom(MEDIUM_SECOND_PARA_KEYS);
        }
    }

    /**
     * Determines team description based on victory results.
     */
    private String determineTeamDescription() {
        var victoryResult = simulationManager.getCurrentVictoryResult();

        if (!context.getTeams().isEmpty()) {
            for (var team : context.getTeams()) {
                if (team.getId() == victoryResult.getWinningTeam()) {
                    return new PublicReportEntry(TEAM_WINNING_KEY).add(team.toString()).noNL().text();
                }
            }
        }
        return new PublicReportEntry(DEFAULT_DRAW_KEY).noNL().text();
    }

    /**
     * Selects appropriate conclusion key based on battle characteristics.
     */
    private String selectConclusionKey(boolean isHighCasualty, boolean isVeryShortBattle, boolean isLongBattle) {
        if (isHighCasualty) {
            return selectRandom(HIGH_CASUALTY_CONCLUSION_KEYS);
        } else if (isVeryShortBattle) {
            return selectRandom(SHORT_CONCLUSION_KEYS);
        } else if (isLongBattle) {
            return selectRandom(LONG_CONCLUSION_KEYS);
        } else {
            return selectRandom(STANDARD_CONCLUSION_KEYS);
        }
    }

    /**
     * Creates the formatted battle summary using the appropriate template.
     */
    private PublicReportEntry createFormattedSummary(
          boolean isVeryShortBattle, boolean isLongBattle,
          String intensityKey, String lengthKey, String terrainKey, String tacticalKey,
          String outcomeKey, String secondParaKey, String secondTacticalKey,
          String keyFactorsKey, String tacticsKey, String performanceKey,
          String teamDescription, String conclusionKey) {

        PublicReportEntry summary;
        if (isVeryShortBattle) {
            summary = new PublicReportEntry(SHORT_BATTLE_FORMAT)
                            .add(new PublicReportEntry(intensityKey).noNL().text())
                            .add(new PublicReportEntry(lengthKey).noNL().text())
                            .add(new PublicReportEntry(terrainKey).noNL().text())
                            .add(new PublicReportEntry(tacticalKey).noNL().text())
                            .add(new PublicReportEntry(outcomeKey).noNL().text())
                            .add(new PublicReportEntry(secondParaKey).noNL().text())
                            .add(new PublicReportEntry(tacticsKey).noNL().text())
                            .add(new PublicReportEntry(performanceKey).noNL().text())
                            .add(new PublicReportEntry(keyFactorsKey).noNL().text())
                            .add(new PublicReportEntry(conclusionKey).noNL().text());
        } else if (isLongBattle) {
            summary = new PublicReportEntry(LONG_BATTLE_FORMAT)
                            .add(new PublicReportEntry(intensityKey).noNL().text())
                            .add(new PublicReportEntry(lengthKey).noNL().text())
                            .add(new PublicReportEntry(terrainKey).noNL().text())
                            .add(new PublicReportEntry(tacticalKey).noNL().text())
                            .add(new PublicReportEntry(outcomeKey).noNL().text())
                            .add(new PublicReportEntry(secondParaKey).noNL().text())
                            .add(new PublicReportEntry(secondTacticalKey).noNL().text())
                            .add(new PublicReportEntry(keyFactorsKey).noNL().text())
                            .add(new PublicReportEntry(tacticsKey).noNL().text())
                            .add(new PublicReportEntry(performanceKey).noNL().text())
                            .add(teamDescription)
                            .add(new PublicReportEntry(conclusionKey).noNL().text());
        } else {
            summary = new PublicReportEntry(MEDIUM_BATTLE_FORMAT)
                            .add(new PublicReportEntry(intensityKey).noNL().text())
                            .add(new PublicReportEntry(lengthKey).noNL().text())
                            .add(new PublicReportEntry(terrainKey).noNL().text())
                            .add(new PublicReportEntry(tacticalKey).noNL().text())
                            .add(new PublicReportEntry(outcomeKey).noNL().text())
                            .add(new PublicReportEntry(secondParaKey).noNL().text())
                            .add(new PublicReportEntry(secondTacticalKey).noNL().text())
                            .add(new PublicReportEntry(keyFactorsKey).noNL().text())
                            .add(new PublicReportEntry(tacticsKey).noNL().text())
                            .add(new PublicReportEntry(performanceKey).noNL().text())
                            .add(teamDescription)
                            .add(new PublicReportEntry(conclusionKey).noNL().text());
        }

        return summary;
    }
}

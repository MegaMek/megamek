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

package megamek.common.autoresolve.acar.report;

import megamek.common.autoresolve.acar.SimulationContext;
import megamek.common.autoresolve.acar.SimulationManager;
import megamek.common.autoresolve.component.Formation;
import megamek.logging.MMLogger;

import java.io.*;
import java.util.Random;
import java.util.function.Consumer;

public class PhaseEndReporter implements IPhaseEndReporter {
    private static final MMLogger logger = MMLogger.create(PhaseEndReporter.class);

    private final HtmlGameLogger gameLogger;
    private final Consumer<PublicReportEntry> reportConsumer;
    private final SimulationContext context;

    private PhaseEndReporter(SimulationContext context, Consumer<PublicReportEntry> reportConsumer, HtmlGameLogger gameLogger) {
        this.reportConsumer = reportConsumer;
        this.gameLogger = gameLogger;
        this.context = context;
    }

    public static IPhaseEndReporter create(SimulationManager manager) {
        if (manager.isLogSuppressed()) {
            return DummyPhaseEndReporter.instance();
        }
        return new PhaseEndReporter(manager.getGame(), manager::addReport, manager.getGameLogger());
    }

    @Override
    public void movementPhaseHeader() {
        reportConsumer.accept(new ReportEntryWithAnchor("acar.movementPhase.header", "round-" + context.getCurrentRound() + "-movement").noNL());
        reportConsumer.accept(new LinkEntry("acar.link.backRef", "summary-round-" + context.getCurrentRound() + "-movement"));
    }

    @Override
    public void firingPhaseHeader() {
        reportConsumer.accept(new ReportEntryWithAnchor("acar.firingPhase.header", "round-" + context.getCurrentRound() + "-firing").noNL());
        reportConsumer.accept(new LinkEntry("acar.link.backRef", "summary-round-" + context.getCurrentRound() + "-firing"));
    }

    private void modifyFile(File file, Consumer<StringBuilder> modifier) throws IOException {
        // Read the file content into a StringBuilder
        StringBuilder content = new StringBuilder();
        try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
            String line;
            while ((line = reader.readLine()) != null) {
                content.append(line).append(System.lineSeparator());
            }
        }

        // Modify the content
        modifier.accept(content);

        // Write the updated content back to the file
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(file))) {
            writer.write(content.toString());
        }
    }

    private String blurbSummary(SimulationManager simulationManager) {
        // Get context information to determine which variations to use
        int rounds = context.getCurrentRound();
        boolean isLongBattle = rounds > 5;
        boolean isVeryShortBattle = rounds <= 2;
        int destroyedUnits = (int) context.getGraveyard().stream().filter(e -> e instanceof Formation).count();
        int remainingUnits = context.getActiveFormations().size();
        int totalUnits = remainingUnits + destroyedUnits;

        double casualtyRate = 1.0 - ((double) remainingUnits / totalUnits);
        boolean isHighCasualty = casualtyRate > 0.5;

        // Setup randomization
        Random rand = new Random(context.getSeed());

        // Select intensity descriptor
        String[] intensityKeys = {
              "acar.{blurb}.intensity.fierce",
              "acar.{blurb}.intensity.intense",
              "acar.{blurb}.intensity.brutal",
              "acar.{blurb}.intensity.tactical",
              "acar.{blurb}.intensity.calculated"
        };
        String intensityKey = intensityKeys[rand.nextInt(intensityKeys.length)];

        // Select length descriptor based on battle duration
        String lengthKey;
        if (isVeryShortBattle) {
            String[] shortLengthKeys = {
                  "acar.{blurb}.length.short.decisive",
                  "acar.{blurb}.length.short.lightning",
                  "acar.{blurb}.length.short.brief",
                  "acar.{blurb}.length.short.flash",
                  "acar.{blurb}.length.short.swift"
            };
            lengthKey = shortLengthKeys[rand.nextInt(shortLengthKeys.length)];
        } else if (isLongBattle) {
            String[] longLengthKeys = {
                  "acar.{blurb}.length.long.drawn",
                  "acar.{blurb}.length.long.grueling",
                  "acar.{blurb}.length.long.exhausting",
                  "acar.{blurb}.length.long.endless",
                  "acar.{blurb}.length.long.attrition"
            };
            lengthKey = longLengthKeys[rand.nextInt(longLengthKeys.length)];
        } else {
            String[] mediumLengthKeys = {
                  "acar.{blurb}.length.medium.methodical",
                  "acar.{blurb}.length.medium.steady",
                  "acar.{blurb}.length.medium.balanced",
                  "acar.{blurb}.length.medium.moderate",
                  "acar.{blurb}.length.medium.average"
            };
            lengthKey = mediumLengthKeys[rand.nextInt(mediumLengthKeys.length)];
        }

        // Select terrain descriptor
        String[] terrainKeys = {
              "acar.{blurb}.terrain.rugged",
              "acar.{blurb}.terrain.urban",
              "acar.{blurb}.terrain.plains",
              "acar.{blurb}.terrain.forest",
              "acar.{blurb}.terrain.highlands"
        };
        String terrainKey = terrainKeys[rand.nextInt(terrainKeys.length)];

        // Select tactical descriptor
        String[] tacticalKeys = {
              "acar.{blurb}.tactical.precise",
              "acar.{blurb}.tactical.bold",
              "acar.{blurb}.tactical.cautious",
              "acar.{blurb}.tactical.aggressive",
              "acar.{blurb}.tactical.defensive"
        };
        String tacticalKey = tacticalKeys[rand.nextInt(tacticalKeys.length)];

        // Calculate if it was a close battle
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

        boolean isCloseVictory = winnerUnitCount > 0 && loserUnitCount > 0 &&
                                       ((double)winnerUnitCount / (winnerUnitCount + loserUnitCount)) < 0.6;

        // Select outcome descriptor
        String outcomeKey;
        if (isHighCasualty) {
            String[] highCasualtyKeys = {
                  "acar.{blurb}.outcome.highCasualty.significant",
                  "acar.{blurb}.outcome.highCasualty.heavy",
                  "acar.{blurb}.outcome.highCasualty.devastating",
                  "acar.{blurb}.outcome.highCasualty.pyrrhic",
                  "acar.{blurb}.outcome.highCasualty.aftermath"
            };
            outcomeKey = highCasualtyKeys[rand.nextInt(highCasualtyKeys.length)];
        } else if (isCloseVictory) {
            String[] narrowVictoryKeys = {
                  "acar.{blurb}.outcome.narrow.margin",
                  "acar.{blurb}.outcome.narrow.contested",
                  "acar.{blurb}.outcome.narrow.thin",
                  "acar.{blurb}.outcome.narrow.hardWon",
                  "acar.{blurb}.outcome.narrow.almost"
            };
            outcomeKey = narrowVictoryKeys[rand.nextInt(narrowVictoryKeys.length)];
        } else {
            String[] standardOutcomeKeys = {
                  "acar.{blurb}.outcome.standard.clear",
                  "acar.{blurb}.outcome.standard.strategic",
                  "acar.{blurb}.outcome.standard.tactical",
                  "acar.{blurb}.outcome.standard.successful",
                  "acar.{blurb}.outcome.standard.decisive"
            };
            outcomeKey = standardOutcomeKeys[rand.nextInt(standardOutcomeKeys.length)];
        }

        // Select second paragraph starter based on battle length
        String secondParaKey;
        if (isVeryShortBattle) {
            String[] shortSecondParaKeys = {
                  "acar.{blurb}.secondPara.short.showcased",
                  "acar.{blurb}.secondPara.short.confrontation",
                  "acar.{blurb}.secondPara.short.highlighted",
                  "acar.{blurb}.secondPara.short.demonstrated",
                  "acar.{blurb}.secondPara.short.revealed"
            };
            secondParaKey = shortSecondParaKeys[rand.nextInt(shortSecondParaKeys.length)];
        } else if (isLongBattle) {
            String[] longSecondParaKeys = {
                  "acar.{blurb}.secondPara.long.extended",
                  "acar.{blurb}.secondPara.long.course",
                  "acar.{blurb}.secondPara.long.protracted",
                  "acar.{blurb}.secondPara.long.grueling",
                  "acar.{blurb}.secondPara.long.endurance"
            };
            secondParaKey = longSecondParaKeys[rand.nextInt(longSecondParaKeys.length)];
        } else {
            String[] mediumSecondParaKeys = {
                  "acar.{blurb}.secondPara.medium.throughout",
                  "acar.{blurb}.secondPara.medium.unfolded",
                  "acar.{blurb}.secondPara.medium.course",
                  "acar.{blurb}.secondPara.medium.balanced",
                  "acar.{blurb}.secondPara.medium.progressed"
            };
            secondParaKey = mediumSecondParaKeys[rand.nextInt(mediumSecondParaKeys.length)];
        }

        // Select second tactical descriptor for long battles
        String secondTacticalKey = tacticalKeys[rand.nextInt(tacticalKeys.length)];

        // Select key factors descriptor
        String[] keyFactorsKeys = {
              "acar.{blurb}.keyFactors.key",
              "acar.{blurb}.keyFactors.critical",
              "acar.{blurb}.keyFactors.decisive",
              "acar.{blurb}.keyFactors.important",
              "acar.{blurb}.keyFactors.significant"
        };
        String keyFactorsKey = keyFactorsKeys[rand.nextInt(keyFactorsKeys.length)];

        // Select specific tactics descriptor
        String[] specificTacticsKeys = {
              "acar.{blurb}.tactics.flanking",
              "acar.{blurb}.tactics.firepower",
              "acar.{blurb}.tactics.coordinated",
              "acar.{blurb}.tactics.cover",
              "acar.{blurb}.tactics.precision"
        };
        String tacticsKey = specificTacticsKeys[rand.nextInt(specificTacticsKeys.length)];

        // Select unit performance descriptor
        String[] performanceKeys = {
              "acar.{blurb}.performance.notable",
              "acar.{blurb}.performance.exceptional",
              "acar.{blurb}.performance.strategic",
              "acar.{blurb}.performance.adaptability",
              "acar.{blurb}.performance.cohesion"
        };
        String performanceKey = performanceKeys[rand.nextInt(performanceKeys.length)];

        // Determine winning team description
        String teamDescription = "";
        if (!context.getTeams().isEmpty()) {
            for (var team : context.getTeams()) {
                if (team.getId() == victoryResult.getWinningTeam()) {
                    teamDescription = new PublicReportEntry("acar.{blurb}.team.winning").add(team.toString()).text();
                    break;
                }
            }

            if (teamDescription.isEmpty()) {
                teamDescription = new PublicReportEntry("acar.{blurb}.default.resourceManagement").text();
            }
        } else {
            teamDescription = new PublicReportEntry("acar.{blurb}.default.resourceManagement").text();
        }

        // Select conclusion descriptor based on battle characteristics
        String conclusionKey;
        if (isHighCasualty) {
            String[] highCasualtyConclusionKeys = {
                  "acar.{blurb}.conclusion.highCasualty.pyrrhic",
                  "acar.{blurb}.conclusion.highCasualty.reminder",
                  "acar.{blurb}.conclusion.highCasualty.thin",
                  "acar.{blurb}.conclusion.highCasualty.depleted",
                  "acar.{blurb}.conclusion.highCasualty.threshold"
            };
            conclusionKey = highCasualtyConclusionKeys[rand.nextInt(highCasualtyConclusionKeys.length)];
        } else if (isVeryShortBattle) {
            String[] shortConclusionKeys = {
                  "acar.{blurb}.conclusion.short.decisive",
                  "acar.{blurb}.conclusion.short.precision",
                  "acar.{blurb}.conclusion.short.initiative",
                  "acar.{blurb}.conclusion.short.readiness",
                  "acar.{blurb}.conclusion.short.preparation"
            };
            conclusionKey = shortConclusionKeys[rand.nextInt(shortConclusionKeys.length)];
        } else if (isLongBattle) {
            String[] longConclusionKeys = {
                  "acar.{blurb}.conclusion.long.endurance",
                  "acar.{blurb}.conclusion.long.adaptability",
                  "acar.{blurb}.conclusion.long.logistical",
                  "acar.{blurb}.conclusion.long.persistence",
                  "acar.{blurb}.conclusion.long.reserves"
            };
            conclusionKey = longConclusionKeys[rand.nextInt(longConclusionKeys.length)];
        } else {
            String[] standardConclusionKeys = {
                  "acar.{blurb}.conclusion.standard.tactical",
                  "acar.{blurb}.conclusion.standard.coordinated",
                  "acar.{blurb}.conclusion.standard.awareness",
                  "acar.{blurb}.conclusion.standard.disciplined",
                  "acar.{blurb}.conclusion.standard.resource"
            };
            conclusionKey = standardConclusionKeys[rand.nextInt(standardConclusionKeys.length)];
        }

        // Create the formatted summary using the appropriate template based on battle type
        PublicReportEntry summary;
        if (isVeryShortBattle) {
            summary = new PublicReportEntry("acar.{blurb}.format.short")
                            .add(new PublicReportEntry(intensityKey).text())
                            .add(new PublicReportEntry(lengthKey).text())
                            .add(new PublicReportEntry(terrainKey).text())
                            .add(new PublicReportEntry(tacticalKey).text())
                            .add(new PublicReportEntry(outcomeKey).text())
                            .add(new PublicReportEntry(secondParaKey).text())
                            .add(new PublicReportEntry(tacticsKey).text())
                            .add(new PublicReportEntry(performanceKey).text())
                            .add(new PublicReportEntry(keyFactorsKey).text())
                            .add(new PublicReportEntry(conclusionKey).text());
        } else if (isLongBattle) {
            summary = new PublicReportEntry("acar.{blurb}.format.long")
                            .add(new PublicReportEntry(intensityKey).text())
                            .add(new PublicReportEntry(lengthKey).text())
                            .add(new PublicReportEntry(terrainKey).text())
                            .add(new PublicReportEntry(tacticalKey).text())
                            .add(new PublicReportEntry(outcomeKey).text())
                            .add(new PublicReportEntry(secondParaKey).text())
                            .add(new PublicReportEntry(secondTacticalKey).text())
                            .add(new PublicReportEntry(keyFactorsKey).text())
                            .add(new PublicReportEntry(tacticsKey).text())
                            .add(new PublicReportEntry(performanceKey).text())
                            .add(teamDescription)
                            .add(new PublicReportEntry(conclusionKey).text());
        } else {
            summary = new PublicReportEntry("acar.{blurb}.format.medium")
                            .add(new PublicReportEntry(intensityKey).text())
                            .add(new PublicReportEntry(lengthKey).text())
                            .add(new PublicReportEntry(terrainKey).text())
                            .add(new PublicReportEntry(tacticalKey).text())
                            .add(new PublicReportEntry(outcomeKey).text())
                            .add(new PublicReportEntry(secondParaKey).text())
                            .add(new PublicReportEntry(secondTacticalKey).text())
                            .add(new PublicReportEntry(keyFactorsKey).text())
                            .add(new PublicReportEntry(tacticsKey).text())
                            .add(new PublicReportEntry(performanceKey).text())
                            .add(teamDescription)
                            .add(new PublicReportEntry(conclusionKey).text());
        }

        return summary.text();
    }

    @Override
    public void addSummary(SimulationManager simulationManager) {
        var logFile = gameLogger.getLogFile();
        var rounds = context.getCurrentRound();

        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("<p>").append(new BattleBlurbGenerator(simulationManager).generateBlurb().text())
              .append("</p><br/>");
        stringBuilder.append("<a name='summary'>").append(new PublicReportEntry("acar.header.summary").text()).append("</a></p>")
            .append("<ul>");
        for (int i = 0; i <= rounds; i++) {
            stringBuilder.append("<li><a href='#round-").append(i).append("' name='summary-round-").append(i).append("'>")
                    .append(new PublicReportEntry("acar.summary.roundCount").add(i).text()).append("</a>\n")
                .append("<ul>")
                .append("<li><a href='#round-").append(i).append("-movement' name='summary-round-").append(i).append("-movement'>")
                    .append(new PublicReportEntry("acar.summary.movementPhase").add(i).text()).append("</a></li>\n")
                .append("<li><a href='#round-").append(i).append("-firing' name='summary-round-").append(i).append("-firing'>")
                    .append(new PublicReportEntry("acar.summary.firingPhase").add(i).text()).append("</a></li>\n")
                .append("<li><a href='#round-").append(i).append("-end' name='summary-round-").append(i).append("-end'>")
                    .append(new PublicReportEntry("acar.summary.endPhase").add(i).text()).append("</a></li>\n")
                .append("</ul>\n")
                .append("</li>\n");
        }

        stringBuilder.append("<li><a name='summary-end-of-combat' href='#end-of-combat'>").append(new PublicReportEntry("acar.summary.endOfCombat").text()).append("</a></li>\n");
        stringBuilder.append("<li><a name='summary-end-of-combat' href='#victory'>").append(new PublicReportEntry("acar.summary.scenarioResult").text()).append("</a></li>\n");

        for (var team : context.getTeams()) {
            stringBuilder.append("<li><a href='#end-team-").append(team.getId()).append("' id=>")
                .append(new PublicReportEntry("acar.summary.teamReport").add(team.toString()).text()).append("</a>\n")
                .append("<ul>\n");

            for (var player : team.players()) {
                stringBuilder
                    .append("<li>")
                    .append(new PublicReportEntry("acar.summary.playerReport").add(new PlayerNameReportEntry(player).reportText()).text()).append("\n")
                    .append("<ul>")
                    .append("<li><a name='remaining-").append(player.getId()).append("' href='#end-player-").append(player.getId()).append("-remaining'>")
                    .append(new PublicReportEntry("acar.summary.player.remainingUnits").text()).append("</a></li>\n")
                    .append("<li><a name='destroyed-").append(player.getId()).append("' href='#end-player-").append(player.getId()).append("-destroyed'>")
                    .append(new PublicReportEntry("acar.summary.player.destroyedUnits").text()).append("</a></li>\n")
                    .append("<li><a name='retreating-").append(player.getId()).append("' href='#end-player-").append(player.getId()).append("-retreating'>")
                    .append(new PublicReportEntry("acar.summary.player.retreatingUnits").text()).append("</a></li>\n")
                    .append("</ul>\n")
                    .append("</li>\n");
            }
            stringBuilder.append("</ul>\n")
                .append("</li>\n");
        }

        stringBuilder.append("</ul>\n");

        try {
            modifyFile(logFile, content -> {
                var newString = stringBuilder.toString();
                int index = content.indexOf(SummaryPlaceholderEntry.PLACEHOLDER);
                while (index != -1) {
                    content.replace(index, index + SummaryPlaceholderEntry.PLACEHOLDER.length(), newString);
                    index = content.indexOf(SummaryPlaceholderEntry.PLACEHOLDER, index + newString.length());
                }
            });
        } catch (IOException e) {
            logger.error(e);
        }
    }

    @Override
    public void closeTheFile() {
        gameLogger.addRaw("</body>")
            .addRaw("<footer>")
            .addRaw("</footer>")
            .addRaw("</html>");

        try {
            gameLogger.close();
        } catch (Exception e) {
            logger.warn(e);
        }
    }
}

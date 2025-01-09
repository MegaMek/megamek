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
import megamek.logging.MMLogger;

import java.io.*;
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

    @Override
    public void addSummary() {
        var logFile = gameLogger.getLogFile();
        var rounds = context.getCurrentRound();

        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("<p>").append(new PublicReportEntry("acar.shortBlurb").text()).append("</p><br/>");
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

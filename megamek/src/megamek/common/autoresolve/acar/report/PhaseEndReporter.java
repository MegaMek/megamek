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
        reportConsumer.accept(new ReportEntryWithAnchor(2201, "round-" + context.getCurrentRound() + "-movement").noNL());
        reportConsumer.accept(new LinkEntry(301, "summary-round-" + context.getCurrentRound() + "-movement"));
    }

    @Override
    public void firingPhaseHeader() {
        reportConsumer.accept(new ReportEntryWithAnchor(2002, "round-" + context.getCurrentRound() + "-firing").noNL());
        reportConsumer.accept(new LinkEntry(301, "summary-round-" + context.getCurrentRound() + "-firing"));
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
        stringBuilder.append("<p>").append(new PublicReportEntry(110).text()).append("</p><br/>");
        stringBuilder.append("<a name='summary'>").append(new PublicReportEntry(300).text()).append("</a></p>")
            .append("<ul>");
        for (int i = 0; i <= rounds; i++) {
            stringBuilder.append("<li><a href='#round-").append(i).append("' name='summary-round-").append(i).append("'>")
                    .append(new PublicReportEntry(302).add(i).text()).append("</a>\n")
                .append("<ul>")
                .append("<li><a href='#round-").append(i).append("-movement' name='summary-round-").append(i).append("-movement'>")
                    .append(new PublicReportEntry(303).add(i).text()).append("</a></li>\n")
                .append("<li><a href='#round-").append(i).append("-firing' name='summary-round-").append(i).append("-firing'>")
                    .append(new PublicReportEntry(304).add(i).text()).append("</a></li>\n")
                .append("<li><a href='#round-").append(i).append("-end' name='summary-round-").append(i).append("-end'>")
                    .append(new PublicReportEntry(305).add(i).text()).append("</a></li>\n")
                .append("</ul>\n")
                .append("</li>\n");
        }

        stringBuilder.append("<li><a name='summary-end-of-combat' href='#end-of-combat'>").append(new PublicReportEntry(306).text()).append("</a></li>\n");
        stringBuilder.append("<li><a name='summary-end-of-combat' href='#victory'>").append(new PublicReportEntry(312).text()).append("</a></li>\n");

        for (var team : context.getTeams()) {
            stringBuilder.append("<li><a href='#end-team-").append(team.getId()).append("' id=>")
                .append(new PublicReportEntry(307).add(team.toString()).text()).append("</a>\n")
                .append("<ul>\n");

            for (var player : team.players()) {
                stringBuilder
                    .append("<li>")
                    .append(new PublicReportEntry(308).add(new PlayerNameReportEntry(player).reportText()).text()).append("\n")
                    .append("<ul>")
                    .append("<li><a name='remaining-").append(player.getId()).append("' href='#end-player-").append(player.getId()).append("-remaining'>")
                    .append(new PublicReportEntry(309).text()).append("</a></li>\n")
                    .append("<li><a name='destroyed-").append(player.getId()).append("' href='#end-player-").append(player.getId()).append("-destroyed'>")
                    .append(new PublicReportEntry(310).text()).append("</a></li>\n")
                    .append("<li><a name='retreating-").append(player.getId()).append("' href='#end-player-").append(player.getId()).append("-retreating'>")
                    .append(new PublicReportEntry(311).text()).append("</a></li>\n")
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

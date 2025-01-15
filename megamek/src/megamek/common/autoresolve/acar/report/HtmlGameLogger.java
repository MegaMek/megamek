/*
 * Copyright (c) 2025 - The MegaMek Team. All Rights Reserved.
 *
 *  This program is free software; you can redistribute it and/or modify it
 *  under the terms of the GNU General Public License as published by the Free
 *  Software Foundation; either version 2 of the License, or (at your option)
 *  any later version.
 *
 *  This program is distributed in the hope that it will be useful, but
 *  WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 *  or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License
 *  for more details.
 */
package megamek.common.autoresolve.acar.report;

import megamek.common.GameLog;
import megamek.common.Report;
import megamek.logging.MMLogger;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.List;

/**
 * @author Luana Coppio
 */
public class HtmlGameLogger {

    private static final MMLogger logger = MMLogger.create(HtmlGameLogger.class);
    private final GameLog gameLog;

    private static class LocalGameLog extends GameLog {

        /**
         * Creates GameLog named
         *
         * @param filename the name of the file
         */
        public LocalGameLog(String filename) {
            super(filename);
        }

        @Override
        protected void initialize() {
            String cssContent = loadCssFromResources();

            appendRaw("""
            <!DOCTYPE html>
            <html lang="en">
            <head>
                <meta charset="utf-8">
                <meta name="viewport" content="width=device-width, initial-scale=1.0">
                <meta name="description" content="A layout example that shows off a blog page with a list of posts.">
                <title>Simulation Game Log</title>
                <style>
            """ + cssContent + """
                </style>
            </head>
            <body>
            """
            );
            appendRaw("<p class=\"datetimelog\">Log file opened " + LocalDateTime.now() + "</p>");
        }

    }

    private static String loadCssFromResources() {
        var cssFile = new File("data/css/acarCssFile.css");
        if (cssFile.exists()) {
            try (InputStream inputStream = Files.newInputStream(Paths.get(cssFile.toURI()))) {
                return new String(inputStream.readAllBytes(), StandardCharsets.UTF_8);

            } catch (IOException e) {
                logger.error("Error reading CSS file", e);
            }
        } else {
            logger.error("CSS file not found " + cssFile);
        }
        return "";
    }
    /**
     * Creates GameLog named
     *
     * @param filename the name of the file
     */
    private HtmlGameLogger(String filename) {
        gameLog = new LocalGameLog(filename);
    }

    public static HtmlGameLogger create(String filename) {
        return new HtmlGameLogger(filename);
    }

    public HtmlGameLogger add(List<Report> reports) {
        for (var report : reports) {
            add(report.text());
        }
        return this;
    }

    public HtmlGameLogger add(Report report) {
        add(report.text());
        return this;
    }

    public HtmlGameLogger addRaw(String message) {
        gameLog.appendRaw(message);
        gameLog.appendRaw("\n");
        return this;
    }

    public HtmlGameLogger add(String message) {
        gameLog.append(message);
        return this;
    }

    public File getLogFile() {
        return gameLog.getLogFile();
    }

    public void close() throws Exception {
        gameLog.close();
    }

}

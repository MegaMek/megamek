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

import java.io.File;
import java.time.LocalDateTime;
import java.util.List;

/**
 * @author Luana Coppio
 */
public class HtmlGameLogger {

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
            appendRaw("""
            <!DOCTYPE html>
            <html lang="en">
            <head>
                <title>Simulation Game Log</title>
                <meta charset="UTF-8">
                <!-- CSS -->
                <style>
                .datetimelog {
                    font-size: 0.6em;
                    color: #666;
                }
                body {
                    padding: 10px;
                }
                </style>
            </head>
            <body>
            """
            );
            appendRaw("<p class=\"datetimelog\">Log file opened " + LocalDateTime.now() + "</p>");
        }

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

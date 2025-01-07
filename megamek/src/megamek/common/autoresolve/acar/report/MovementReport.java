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

import megamek.common.IGame;
import megamek.common.alphaStrike.ASRange;
import megamek.common.autoresolve.component.Formation;

import java.util.Map;
import java.util.function.Consumer;

public class MovementReport {

    private final IGame game;
    private final Consumer<PublicReportEntry> reportConsumer;

    private static final Map<ASRange, Integer> reportRangeClosingIn = Map.of(
        ASRange.SHORT, 2204,
        ASRange.MEDIUM, 2204,
        ASRange.LONG, 2204,
        ASRange.EXTREME, 2203,
        ASRange.HORIZON, 2203
    );

    private static final Map<ASRange, Integer> rangeNames = Map.of(
        ASRange.SHORT, 2301,
        ASRange.MEDIUM, 2302,
        ASRange.LONG, 2303,
        ASRange.EXTREME, 2304,
        ASRange.HORIZON, 2305
    );

    public MovementReport(IGame game, Consumer<PublicReportEntry> reportConsumer) {
        this.reportConsumer = reportConsumer;
        this.game = game;
    }

    public void reportMovement(Formation mover, Formation target, int direction) {
        int reportId;
        var distance = mover.getPosition().coords().distance(target.getPosition().coords());
        var range = ASRange.fromDistance(distance);
        if (direction > 0) {
            reportId = reportRangeClosingIn.get(range);
        } else {
            reportId = 2205;
        }

        var rangeName = distance < 3 ? 2300 : rangeNames.get(range);

        var report = new PublicReportEntry(reportId).noNL();
        report.add(new FormationReportEntry(mover, game).text());
        report.add(new FormationReportEntry(target, game).text());
        report.add(new PublicReportEntry(rangeName).reportText());

        reportConsumer.accept(report);
    }

    public void reportRetreatMovement(Formation mover) {
        var report = new PublicReportEntry(2206).noNL();
        report.add(new FormationReportEntry(mover, game).text());
        reportConsumer.accept(report);
    }

    public void reportMovement(Formation mover) {
        var report = new PublicReportEntry(2200).noNL();
        report.add(new FormationReportEntry(mover, game).text());
        reportConsumer.accept(report);
    }
}

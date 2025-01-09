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
import megamek.common.autoresolve.acar.SimulationManager;
import megamek.common.autoresolve.component.Formation;

import java.util.Map;
import java.util.function.Consumer;

public class MovementReport implements IMovementReport {

    private final IGame game;
    private final Consumer<PublicReportEntry> reportConsumer;

    private static final Map<ASRange, String> reportRangeClosingIn = Map.of(
        ASRange.SHORT, "acar.movementPhase.advancingDetailed",
        ASRange.MEDIUM, "acar.movementPhase.advancingDetailed",
        ASRange.LONG, "acar.movementPhase.advancingDetailed",
        ASRange.EXTREME, "acar.movementPhase.advancingTarget",
        ASRange.HORIZON, "acar.movementPhase.advancingTarget"
    );

    private static final Map<ASRange, String> rangeNames = Map.of(
        ASRange.SHORT, "acar.range.close",
        ASRange.MEDIUM, "acar.range.skirmish",
        ASRange.LONG, "acar.range.bombardment",
        ASRange.EXTREME, "acar.range.extreme",
        ASRange.HORIZON, "acar.range.beyondVisual"
    );

    private MovementReport(IGame game, Consumer<PublicReportEntry> reportConsumer) {
        this.reportConsumer = reportConsumer;
        this.game = game;
    }

    public static IMovementReport create(SimulationManager manager) {
        if (manager.isLogSuppressed()) {
            return DummyMovementReport.instance();
        }
        return new MovementReport(manager.getGame(), manager::addReport);
    }

    @Override
    public void reportMovement(Formation mover, Formation target, int direction) {
        var distance = mover.getPosition().coords().distance(target.getPosition().coords());
        var range = ASRange.fromDistance(distance);
        String reportId = direction > 0 ? reportRangeClosingIn.get(range) : "acar.movementPhase.movingAway";
        var rangeName = distance >= 3 ? rangeNames.get(range) :  "acar.range.pointBlank";

        var report = new PublicReportEntry(reportId);
        report.add(new FormationReportEntry(mover, game).text());
        report.add(new FormationReportEntry(target, game).text());
        report.add(new PublicReportEntry(rangeName).reportText());

        reportConsumer.accept(report);
    }

    @Override
    public void reportRetreatMovement(Formation mover) {
        var report = new PublicReportEntry("acar.movementPhase.disengaging");
        report.add(new FormationReportEntry(mover, game).text());
        reportConsumer.accept(report);
    }

    @Override
    public void reportMovement(Formation mover) {
        var report = new PublicReportEntry("acar.movementPhase.advancing");
        report.add(new FormationReportEntry(mover, game).text());
        reportConsumer.accept(report);
    }
}

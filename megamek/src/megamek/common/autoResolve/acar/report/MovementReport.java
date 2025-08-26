/*
 * Copyright (C) 2025 The MegaMek Team. All Rights Reserved.
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

package megamek.common.autoResolve.acar.report;

import java.util.Map;
import java.util.function.Consumer;

import megamek.common.alphaStrike.ASRange;
import megamek.common.autoResolve.acar.SimulationManager;
import megamek.common.autoResolve.component.Formation;
import megamek.common.game.IGame;

public record MovementReport(IGame game, Consumer<PublicReportEntry> reportConsumer) implements IMovementReport {

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
        var rangeName = distance >= 3 ? rangeNames.get(range) : "acar.range.pointBlank";

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

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
import megamek.common.Roll;
import megamek.common.autoresolve.component.EngagementControl;
import megamek.common.autoresolve.component.Formation;

import java.util.function.Consumer;

public class EngagementAndControlReporter {
    private final IGame game;
    private final Consumer<PublicReportEntry> reportConsumer;

    public EngagementAndControlReporter(IGame game, Consumer<PublicReportEntry> reportConsumer) {
        this.game = game;
        this.reportConsumer = reportConsumer;
    }

    public void reportEngagementStart(Formation attacker, Formation target, EngagementControl control) {
        PublicReportEntry report = new PublicReportEntry(2200)
            .add(new FormationReportEntry(attacker, game).text())
            .add(new FormationReportEntry(target, game).text())
            .add(control.name());
        reportConsumer.accept(report);
    }

    public void reportAttackerToHitValue(int toHitValue) {
        reportConsumer.accept(new PublicReportEntry(2203).indent().add(toHitValue));
    }

    public void reportAttackerRoll(Formation attacker, Roll attackerRoll) {
        PublicReportEntry report = new PublicReportEntry(2202);
        report.add(new PlayerNameReportEntry(game.getPlayer(attacker.getOwnerId())).text());
        report.add(new RollReportEntry(attackerRoll).reportText()).indent();
        reportConsumer.accept(report);
    }

    public void reportDefenderRoll(Formation target, Roll defenderRoll) {
        PublicReportEntry report = new PublicReportEntry(2202).indent();
        report.add(new PlayerNameReportEntry(game.getPlayer(target.getOwnerId())).text());
        report.add(new RollReportEntry(defenderRoll).reportText());
        reportConsumer.accept(report);
    }

    public void reportAttackerWin(Formation attacker) {
        PublicReportEntry report = new PublicReportEntry(2204).indent()
            .add(new PlayerNameReportEntry(game.getPlayer(attacker.getOwnerId())).text());
        reportConsumer.accept(report);
    }

    public void reportAttackerLose(Formation attacker) {
        reportConsumer.accept(new PublicReportEntry(2205)
            .add(new PlayerNameReportEntry(game.getPlayer(attacker.getOwnerId())).text())
            .indent());
    }
}

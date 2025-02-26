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
import megamek.common.autoresolve.acar.SimulationManager;
import megamek.common.autoresolve.component.Formation;

import java.util.function.Consumer;

public class MoraleReporter implements IMoraleReporter {

    private final IGame game;
    private final Consumer<PublicReportEntry> reportConsumer;

    private MoraleReporter(IGame game, Consumer<PublicReportEntry> reportConsumer) {
        this.game = game;
        this.reportConsumer = reportConsumer;
    }

    public static IMoraleReporter create(SimulationManager manager) {
        if (manager.isLogSuppressed()) {
            return DummyMoraleReporter.instance();
        }
        return new MoraleReporter(manager.getGame(), manager::addReport);
    }

    @Override
    public void reportMoraleCheckStart(Formation formation, int toHitValue) {
        var startReport = new PublicReportEntry("acar.morale.checkAttempt")
            .add(new FormationReportEntry(formation, game).reportText())
            .add(toHitValue);
        reportConsumer.accept(startReport);
    }

    @Override
    public void reportMoraleCheckRoll(Formation formation, Roll roll) {
        var rollReport = new PublicReportEntry("acar.morale.checkRolled")
            .add(new FormationReportEntry(formation, game).reportText())
            .add(new RollReportEntry(roll).reportText());
        reportConsumer.accept(rollReport);
    }

    @Override
    public void reportMoraleCheckSuccess(Formation formation) {
        var successReport = new PublicReportEntry("acar.morale.checkSuccess")
            .add(new FormationReportEntry(formation, game).text());
        reportConsumer.accept(successReport);
    }

    @Override
    public void reportMoraleCheckFailure(Formation formation, Formation.MoraleStatus oldStatus, Formation.MoraleStatus newStatus) {
        var failReport = new PublicReportEntry("acar.morale.checkFail")
            .add(new FormationReportEntry(formation, game).text())
            .add(oldStatus.name())
            .add(newStatus.name());

        reportConsumer.accept(failReport);
    }
}

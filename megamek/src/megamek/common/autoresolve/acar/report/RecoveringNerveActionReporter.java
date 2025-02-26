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
import megamek.common.strategicBattleSystems.SBFFormation;

import java.util.function.Consumer;

public class RecoveringNerveActionReporter implements IRecoveringNerveActionReporter {

    private final IGame game;
    private final Consumer<PublicReportEntry> reportConsumer;

    private RecoveringNerveActionReporter(IGame game, Consumer<PublicReportEntry> reportConsumer) {
        this.reportConsumer = reportConsumer;
        this.game = game;
    }

    public static IRecoveringNerveActionReporter create(SimulationManager manager) {
        if (manager.isLogSuppressed()) {
            return DummyRecoveringNerveActionReporter.instance();
        }
        return new RecoveringNerveActionReporter(manager.getGame(), manager::addReport);
    }

    @Override
    public void reportRecoveringNerveStart(Formation formation, int toHitValue) {
        reportConsumer.accept(new PublicReportEntry("acar.morale.recoveryAttempt")
            .add(new FormationReportEntry(formation, game).reportText())
            .add(toHitValue)
        );
    }

    @Override
    public void reportMoraleStatusChange(Formation.MoraleStatus newMoraleStatus, Roll roll) {
        reportConsumer.accept(new PublicReportEntry("acar.morale.recoveryRoll")
            .indent().add(new RollReportEntry(roll).reportText()).add(newMoraleStatus.name().toLowerCase()));
    }

    @Override
    public void reportFailureRoll(Roll roll, SBFFormation.MoraleStatus moraleStatus) {
        reportConsumer.accept(new PublicReportEntry("acar.morale.recoveryFail")
            .add(roll.toString()).add(moraleStatus.name().toLowerCase()));
    }
}

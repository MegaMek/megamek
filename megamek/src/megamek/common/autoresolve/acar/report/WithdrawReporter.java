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
import megamek.common.autoresolve.acar.SimulationManager;
import megamek.common.autoresolve.component.Formation;

import java.util.function.Consumer;

public class WithdrawReporter implements IWithdrawReporter {

    private final IGame game;
    private final Consumer<PublicReportEntry> reportConsumer;

    public WithdrawReporter(IGame game, Consumer<PublicReportEntry> reportConsumer) {
        this.game = game;
        this.reportConsumer = reportConsumer;
    }

    public static IWithdrawReporter create(SimulationManager manager) {
        if (manager.isLogSuppressed()) {
            return DummyWithdrawReporter.instance();
        }
        return new WithdrawReporter(manager.getGame(), manager::addReport);
    }

    @Override
    public void reportSuccessfulWithdraw(Formation withdrawingFormation) {
        reportConsumer.accept(
            new PublicReportEntry(3333)
            .add(new FormationReportEntry(withdrawingFormation, game).reportText())
            .indent(2));
    }
}

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

import megamek.common.Coords;
import megamek.common.IGame;
import megamek.common.autoresolve.acar.SimulationManager;
import megamek.common.autoresolve.component.Formation;

import java.util.function.Consumer;

public class DeploymentReport implements IDeploymentReport {

    private final IGame game;
    private final Consumer<PublicReportEntry> reportConsumer;

    private DeploymentReport(IGame game, Consumer<PublicReportEntry> reportConsumer) {
        this.reportConsumer = reportConsumer;
        this.game = game;
    }

    public static IDeploymentReport create(SimulationManager manager) {
        if (manager.isLogSuppressed()) {
            return DummyDeploymentReport.instance();
        }
        return new DeploymentReport(manager.getGame(), manager::addReport);
    }

    @Override
    public void reportDeployment(Formation deployed, Coords position) {
        var report = new PublicReportEntry(2202);
        report.add(new FormationReportEntry(deployed, game).text());
        report.add(position.getX() + 1);
        reportConsumer.accept(report);
    }
}

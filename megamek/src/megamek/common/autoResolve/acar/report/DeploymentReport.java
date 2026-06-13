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

import java.util.function.Consumer;

import megamek.common.autoResolve.acar.SimulationManager;
import megamek.common.autoResolve.component.Formation;
import megamek.common.board.Coords;
import megamek.common.game.IGame;

public record DeploymentReport(IGame game, Consumer<PublicReportEntry> reportConsumer) implements IDeploymentReport {

    public static IDeploymentReport create(SimulationManager manager) {
        if (manager.isLogSuppressed()) {
            return DummyDeploymentReport.instance();
        }
        return new DeploymentReport(manager.getGame(), manager::addReport);
    }

    @Override
    public void deploymentRoundHeader(int currentRound) {
        reportConsumer.accept(new PublicReportEntry("acar.initiative.deploymentHeader").add(currentRound));
    }

    @Override
    public void reportDeployment(Formation deployed, Coords position) {
        var report = new PublicReportEntry("acar.movementPhase.deployed");
        report.add(new FormationReportEntry(deployed, game).text());
        report.add(position.getX() + 1);
        reportConsumer.accept(report);
    }
}

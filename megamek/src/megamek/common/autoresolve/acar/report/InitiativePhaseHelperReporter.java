/*
 * Copyright (c) 2025 - The MegaMek Team. All Rights Reserved.
 *
 * This program is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License as published by the Free
 * Software Foundation; either version 2 of the License, or (at your option)
 * any later version.
 *
 * This program is distributed in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 * or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License
 * for more details.
 *
 */

package megamek.common.autoresolve.acar.report;

import megamek.common.Player;
import megamek.common.Team;
import megamek.common.autoresolve.acar.SimulationContext;
import megamek.common.autoresolve.acar.SimulationManager;

import java.util.function.Consumer;

public class InitiativePhaseHelperReporter implements IInitiativePhaseHelperReporter {

    private final Consumer<PublicReportEntry> reportConsumer;
    private final SimulationContext context;

    private InitiativePhaseHelperReporter(SimulationContext context, Consumer<PublicReportEntry> reportConsumer) {
        this.reportConsumer = reportConsumer;
        this.context = context;
    }

    public static IInitiativePhaseHelperReporter create(SimulationManager manager) {
        if (manager.isLogSuppressed()) {
            return DummyInitiativePhaseHelperReporter.instance();
        }
        return new InitiativePhaseHelperReporter(manager.getGame(), manager::addReport);
    }

    @Override
    public void writeInitiativeRolls() {
        for (Team team : context.getTeams()) {
            // Teams with no active players can be ignored
            if (team.isObserverTeam()) {
                continue;
            }
            reportConsumer.accept(new PublicReportEntry(1015).add(Player.TEAM_NAMES[team.getId()])
                .add(team.getInitiative().toString()));

            // Multiple players. List the team, then break it down.
            for (Player player : team.nonObserverPlayers()) {
                reportConsumer.accept(new PublicReportEntry(2020)
                    .indent()
                    .add(player.getName())
                    .add(player.getInitiative().toString())
                );
            }
        }
    }

    @Override
    public void writeInitiativeHeader() {
        var currentRound = context.getCurrentRound();
        reportConsumer.accept(new DividerEntry());
        if (context.getLastPhase().isDeployment() || context.isDeploymentComplete()
            || !context.shouldDeployThisRound()) {
            reportConsumer.accept(new ReportEntryWithAnchor(1000, "round-" + currentRound).add(currentRound));
            reportConsumer.accept(new LinkEntry(301, "summary-round-" + currentRound));
        } else {
            if (currentRound == 0) {
                reportConsumer.accept(new ReportEntryWithAnchor(1005, "round-" + currentRound));
                reportConsumer.accept(new LinkEntry(301, "summary-round-" + currentRound));
            } else {
                reportConsumer.accept(new ReportEntryWithAnchor(1000, "round-" + currentRound).add(currentRound));
                reportConsumer.accept(new LinkEntry(301, "summary-round-" + currentRound));
            }
        }
    }
}

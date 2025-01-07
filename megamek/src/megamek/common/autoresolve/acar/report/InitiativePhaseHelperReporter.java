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

import megamek.common.Deployable;
import megamek.common.Player;
import megamek.common.Team;
import megamek.common.autoresolve.acar.SimulationContext;
import megamek.common.autoresolve.acar.SimulationManager;
import megamek.common.autoresolve.component.Formation;

import java.util.Comparator;
import java.util.List;
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
    public void deploymentRoundHeader(int round) {
        reportConsumer.accept(new PublicReportEntry(1065).add(round));
    }

    @Override
    public void addCurrentDeployment(Formation deployable) {
        reportConsumer.accept(new PublicReportEntry(1066)
            .add(new FormationReportEntry(deployable, context).text())
            .add(deployable.getId())
            .add(deployable.getDeployRound())
            .indent(2));
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

    @Override
    public void writeFutureDeployment() {
        // remaining deployments
        Comparator<Deployable> comp = Comparator.comparingInt(Deployable::getDeployRound);
        List<Deployable> futureDeployments = context.getInGameObjects().stream()
            .filter(Formation.class::isInstance)
            .map(Deployable.class::cast)
            .filter(unit -> !unit.isDeployed())
            .sorted(comp)
            .toList();

        if (!futureDeployments.isEmpty()) {
            int round = -1;
            for (Deployable deployable : futureDeployments) {
                if (round != deployable.getDeployRound()) {
                    round = deployable.getDeployRound();
                    deploymentRoundHeader(round);
                }
                addCurrentDeployment((Formation) deployable);
            }
        }
    }
}

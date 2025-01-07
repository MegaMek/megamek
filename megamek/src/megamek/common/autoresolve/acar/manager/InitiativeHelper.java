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
package megamek.common.autoresolve.acar.manager;

import megamek.common.*;
import megamek.common.autoresolve.acar.SimulationManager;
import megamek.common.autoresolve.acar.report.FormationReportEntry;
import megamek.common.autoresolve.acar.report.PublicReportEntry;
import megamek.common.autoresolve.acar.report.ReportHeader;
import megamek.common.autoresolve.component.AcTurn;
import megamek.common.autoresolve.component.Formation;
import megamek.common.autoresolve.component.FormationTurn;
import megamek.common.enums.GamePhase;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

/**
 * @author Luana Coppio
 */
public record InitiativeHelper(SimulationManager simulationManager) implements SimulationManagerHelper {

    public void resetInitiative() {
        for (var player : game().getPlayersList()) {
            player.getInitiative().clear();
        }
    }

    private record InitiativeFormationTurn(int initiativeValue, AcTurn acTurn)  implements Comparable<InitiativeFormationTurn> {
        @Override
        public int compareTo(InitiativeFormationTurn o) {
            if (initiativeValue > o.initiativeValue) {
                return -1;
            } else if (initiativeValue < o.initiativeValue) {
                return 1;
            }
            return 0;
        }
    };

    /**
     * Determines the turn order for a given phase, setting the game's turn list and sending it to the
     * Clients. Also resets the turn index.
     *
     * @param phase The phase to find the turns for
     * @see AbstractGame#resetTurnIndex()
     */
    void determineTurnOrder(GamePhase phase) {
        List<InitiativeFormationTurn> formationTurns = new ArrayList<>();
        if (phase.isFiring() || phase.isMovement()) {
            for (var player : game().getPlayersList()) {
                var actionsForThisTurn = game().getActiveFormations(player).stream()
                    .filter(Formation::isDeployed)
                    .filter(unit -> unit.isEligibleForPhase(phase))
                    .count();
                var turnsForPlayer = Math.min(actionsForThisTurn, player.getInitiative().size());
                for (int i = 0; i < turnsForPlayer; i++) {
                    var initiative = player.getInitiative().getRoll(i);
                    formationTurns.add(new InitiativeFormationTurn(initiative, new FormationTurn(player.getId())));
                }
            }
        } else if (phase.isDeployment()) {
            for (var player : game().getPlayersList()) {
                var actionsForThisTurn = game().getActiveFormations(player).stream()
                    .filter(Formation::isDeployed)
                    .filter(unit -> !unit.isDeployed())
                    .count();
                var turnsForPlayer = Math.min(actionsForThisTurn, player.getInitiative().size());
                for (int i = 0; i < turnsForPlayer; i++) {
                    var initiative = player.getInitiative().getRoll(i);
                    formationTurns.add(new InitiativeFormationTurn(initiative, new FormationTurn(player.getId())));
                }
            }
        } else {
            for (var player : game().getPlayersList()) {
                var actionsForThisTurn = game().getActiveFormations(player).stream()
                    .filter(unit -> unit.isEligibleForPhase(phase))
                    .filter(Formation::isDeployed)
                    .count();
                var turnsForPlayer = Math.min(actionsForThisTurn, player.getInitiative().size());
                for (int i = 0; i < turnsForPlayer; i++) {
                    var initiative = player.getInitiative().getRoll(i);
                    formationTurns.add(new InitiativeFormationTurn(initiative, new FormationTurn(player.getId())));
                }
            }
        }

        final List<AcTurn> turns = formationTurns.stream()
            .sorted()
            .map(InitiativeFormationTurn::acTurn)
            .collect(Collectors.toList());

        game().setTurns(turns);
        game().resetTurnIndex();
    }

    public void writeInitiativeReport() {
        writeHeader();
        writeInitiativeRolls();
        writeFutureDeployment();
    }

    private void writeFutureDeployment() {
        // remaining deployments
        Comparator<Deployable> comp = Comparator.comparingInt(Deployable::getDeployRound);
        List<Deployable> futureDeployments = game().getInGameObjects().stream()
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
                    addReport(new PublicReportEntry(1065).add(round));
                }

                var r = new PublicReportEntry(1066)
                    .add(new FormationReportEntry((Formation) deployable, game()).text())
                    .add(((InGameObject) deployable).getId())
                    .add(deployable.getDeployRound())
                    .indent(2);
                addReport(r);
            }
        }
    }

    private void writeInitiativeRolls() {
        for (Team team : game().getTeams()) {
            // Teams with no active players can be ignored
            if (team.isObserverTeam()) {
                continue;
            }
            addReport(new PublicReportEntry(1015).add(Player.TEAM_NAMES[team.getId()])
                .add(team.getInitiative().toString()));

            // Multiple players. List the team, then break it down.
            for (Player player : team.nonObserverPlayers()) {
                addReport(new PublicReportEntry(2020)
                    .indent()
                    .add(player.getName())
                    .add(player.getInitiative().toString())
                );
            }
        }
    }

    private Team getTeamForPlayerId(int id) {
        return game().getTeams().stream()
            .filter(t -> t.players().stream().anyMatch(p -> p.getId() == id))
            .findFirst()
            .orElse(null);
    }

    private Player getPlayerForFormation(Formation formation) {
        return game().getPlayer(formation.getOwnerId());
    }

    public void rollInitiativeForFormations(List<Formation> formations) {
        for (var formation : formations) {
            int bonus = 0;
            final Team team = getTeamForPlayerId(formation.getOwnerId());
            if (team != null) {
                bonus = team.getTotalInitBonus(false);
            }
            var player = this.getPlayerForFormation(formation);
            player.getInitiative().addRoll(bonus);
        }
    }

    private void writeHeader() {
        addReport(new ReportHeader(1200));
        if (game().getLastPhase().isDeployment() || game().isDeploymentComplete()
            || !game().shouldDeployThisRound()) {
            addReport(new ReportHeader(1000).add(game().getCurrentRound()));
        } else {
            if (game().getCurrentRound() == 0) {
                addReport(new ReportHeader(1005));
            } else {
                addReport(new ReportHeader(1000).add(game().getCurrentRound()));
            }
        }
    }
}

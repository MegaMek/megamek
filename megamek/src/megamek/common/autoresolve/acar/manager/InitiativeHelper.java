/*
 * Copyright (c) 2024 - The MegaMek Team. All Rights Reserved.
 *
 *  This file is part of MekHQ.
 *
 *  MekHQ is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 *
 *  MekHQ is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with MekHQ. If not, see <http://www.gnu.org/licenses/>.
 */

package megamek.common.autoresolve.acar.manager;

import megamek.common.*;
import megamek.common.enums.GamePhase;
import megamek.common.strategicBattleSystems.SBFFormation;
import mekhq.campaign.autoresolve.acar.SimulationManager;
import mekhq.campaign.autoresolve.acar.report.FormationReportEntry;
import mekhq.campaign.autoresolve.acar.report.PlayerNameReportEntry;
import mekhq.campaign.autoresolve.acar.report.PublicReportEntry;
import mekhq.campaign.autoresolve.acar.report.ReportHeader;
import mekhq.campaign.autoresolve.component.AcTurn;
import mekhq.campaign.autoresolve.component.Formation;
import mekhq.campaign.autoresolve.component.FormationTurn;

import java.util.*;
import java.util.stream.Collectors;

/**
 * @author Luana Coppio
 */
public record InitiativeHelper(SimulationManager simulationManager) implements SimulationManagerHelper {

    /**
     * Determines the turn order for a given phase, setting the game's turn list and sending it to the
     * Clients. Also resets the turn index.
     *
     * @param phase The phase to find the turns for
     * @see AbstractGame#resetTurnIndex()
     */
    void determineTurnOrder(GamePhase phase) {
        final List<AcTurn> turns;
        if (phase.isFiring() || phase.isMovement()) {
            turns = game().getInGameObjects().stream()
                .filter(Formation.class::isInstance)
                .map(Formation.class::cast)
                .filter(Formation::isDeployed)
                .filter(unit -> unit.isEligibleForPhase(phase))
                .map(InGameObject::getOwnerId)
                .map(FormationTurn::new)
                .sorted(Comparator.comparing(t -> game().getPlayer(t.playerId()).getInitiative()))
                .collect(Collectors.toList());
        } else if (phase.isDeployment()) {
            // Deployment phase: sort by initiative
            turns = game().getInGameObjects().stream()
                .filter(Formation.class::isInstance)
                .map(Formation.class::cast)
                .filter(unit -> !unit.isDeployed())
                .map(InGameObject::getOwnerId)
                .map(FormationTurn::new)
                .sorted(Comparator.comparing(t -> game().getPlayer(t.playerId()).getInitiative()))
                .collect(Collectors.toList());

        } else {
            // As a fallback, provide unsorted turns
            turns = game().getInGameObjects().stream()
                .filter(Formation.class::isInstance)
                .map(Formation.class::cast)
                .filter(SBFFormation::isDeployed)
                .filter(unit -> unit.isEligibleForPhase(phase))
                .map(InGameObject::getOwnerId)
                .map(FormationTurn::new)
                .collect(Collectors.toList());

            // Now, assemble formations and sort by initiative and relative formation count
            Map<Integer, Long> unitCountsByPlayer = game().getInGameObjects().stream()
                .filter(Formation.class::isInstance)
                .map(Formation.class::cast)
                .filter(SBFFormation::isDeployed)
                .filter(unit -> unit.isEligibleForPhase(phase))
                .collect(Collectors.groupingBy(InGameObject::getOwnerId, Collectors.counting()));

            if (!unitCountsByPlayer.isEmpty()) {
                final long lowestUnitCount = Collections.min(unitCountsByPlayer.values());

                int playerWithLowestUnitCount = unitCountsByPlayer.entrySet().stream()
                    .filter(e -> e.getValue() == lowestUnitCount)
                    .map(Map.Entry::getKey)
                    .findAny().orElse(Player.PLAYER_NONE);

                List<Integer> playersByInitiative = new ArrayList<>(unitCountsByPlayer.keySet());
                playersByInitiative.sort(Comparator.comparing(id -> game().getPlayer(id).getInitiative()));

                if ((playerWithLowestUnitCount != Player.PLAYER_NONE) && (lowestUnitCount > 0)) {
                    List<AcTurn> sortedTurns = new ArrayList<>();
                    for (int initCycle = 0; initCycle < lowestUnitCount; initCycle++) {
                        long currentLowestUnitCount = Collections.min(unitCountsByPlayer.values());
                        for (int playerId : playersByInitiative) {
                            long unitsToMove = unitCountsByPlayer.get(playerId) / currentLowestUnitCount;
                            long remainingUnits = unitCountsByPlayer.get(playerId);
                            unitsToMove = Math.min(unitsToMove, remainingUnits);
                            for (int i = 0; i < unitsToMove; i++) {
                                sortedTurns.add(new FormationTurn(playerId));
                            }
                            unitCountsByPlayer.put(playerId, remainingUnits - unitsToMove);
                        }
                    }
                    // When here, sorting has been successful; replace the unsorted turns
                    turns.clear();
                    turns.addAll(sortedTurns);
                }
            }
        }

        game().setTurns(turns);
        game().resetTurnIndex();
    }

    public void writeInitiativeReport() {
        writeHeader();
        writeInitiativeRolls();
        writeTurnOrder();
        writeFutureDeployment();
    }

    private void writeTurnOrder() {
        addReport(new PublicReportEntry(1020));

        for (var turn : game().getTurnsList()) {
            Player player = game().getPlayer(turn.playerId());
            addReport(new PlayerNameReportEntry(player).indent().addNL());
        }

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
            addReport(new PublicReportEntry(1060));
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
                    .indent();
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

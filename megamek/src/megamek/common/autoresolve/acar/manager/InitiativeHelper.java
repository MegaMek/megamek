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

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import megamek.common.AbstractGame;
import megamek.common.Player;
import megamek.common.Team;
import megamek.common.autoresolve.acar.SimulationManager;
import megamek.common.autoresolve.component.AcTurn;
import megamek.common.autoresolve.component.Formation;
import megamek.common.autoresolve.component.FormationTurn;
import megamek.common.enums.GamePhase;

/**
 * @author Luana Coppio
 */
public record InitiativeHelper(SimulationManager simulationManager) implements SimulationManagerHelper {

    public void resetInitiative() {
        for (var player : game().getPlayersList()) {
            player.getInitiative().clear();
        }
    }

    private record InitiativeFormationTurn(int initiativeValue, AcTurn acTurn)
          implements Comparable<InitiativeFormationTurn> {
        @Override
        public int compareTo(InitiativeFormationTurn o) {
            if (initiativeValue > o.initiativeValue) {
                return -1;
            } else if (initiativeValue < o.initiativeValue) {
                return 1;
            }
            return 0;
        }
    }

    /**
     * Determines the turn order for a given phase, setting the game's turn list and sending it to the Clients. Also
     * resets the turn index.
     *
     * @param phase The phase to find the turns for
     *
     * @see AbstractGame#resetTurnIndex()
     */
    void determineTurnOrder(GamePhase phase) {
        List<InitiativeFormationTurn> formationTurns = new ArrayList<>();
        if (phase.isFiring() || phase.isMovement()) {
            for (var player : game().getPlayersList()) {
                var actionsForThisTurn = game().getActiveFormations(player)
                                               .stream()
                                               .filter(Formation::isDeployed)
                                               .filter(unit -> unit.isEligibleForPhase(phase))
                                               .count();
                var turnsForPlayer = (int) Math.min(actionsForThisTurn, player.getInitiative().size());
                for (int i = 0; i < turnsForPlayer; i++) {
                    var initiative = player.getInitiative().getRoll(i);
                    formationTurns.add(new InitiativeFormationTurn(initiative, new FormationTurn(player.getId())));
                }
            }
        } else if (phase.isDeployment()) {
            for (var player : game().getPlayersList()) {
                var actionsForThisTurn = game().getActiveFormations(player)
                                               .stream()
                                               .filter(Formation::isDeployed)
                                               .filter(unit -> !unit.isDeployed())
                                               .count();
                var turnsForPlayer = (int) Math.min(actionsForThisTurn, player.getInitiative().size());
                for (int i = 0; i < turnsForPlayer; i++) {
                    var initiative = player.getInitiative().getRoll(i);
                    formationTurns.add(new InitiativeFormationTurn(initiative, new FormationTurn(player.getId())));
                }
            }
        } else {
            for (var player : game().getPlayersList()) {
                var actionsForThisTurn = game().getActiveFormations(player)
                                               .stream()
                                               .filter(unit -> unit.isEligibleForPhase(phase))
                                               .filter(Formation::isDeployed)
                                               .count();
                var turnsForPlayer = (int) Math.min(actionsForThisTurn, player.getInitiative().size());
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

    private Team getTeamForPlayerId(int id) {
        return game().getTeams()
                     .stream()
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
            // Due to the abstract nature of the simulation, we don't track initiative aptitude SPAs
            player.getInitiative().addRoll(bonus, "");
        }
    }
}

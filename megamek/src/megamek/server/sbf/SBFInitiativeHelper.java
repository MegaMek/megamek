/*
 * Copyright (C) 2024-2025 The MegaMek Team. All Rights Reserved.
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

package megamek.server.sbf;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import megamek.common.AbstractGame;
import megamek.common.Deployable;
import megamek.common.InGameObject;
import megamek.common.Player;
import megamek.common.Team;
import megamek.common.enums.GamePhase;
import megamek.common.planetaryconditions.PlanetaryConditions;
import megamek.common.strategicBattleSystems.SBFFormation;
import megamek.common.strategicBattleSystems.SBFFormationTurn;
import megamek.common.strategicBattleSystems.SBFInitiativeRollReportEntry;
import megamek.common.strategicBattleSystems.SBFPlayerNameReportEntry;
import megamek.common.strategicBattleSystems.SBFPlayerTurn;
import megamek.common.strategicBattleSystems.SBFPublicReportEntry;
import megamek.common.strategicBattleSystems.SBFReportEntry;
import megamek.common.strategicBattleSystems.SBFReportHeader;
import megamek.common.strategicBattleSystems.SBFTurn;

public record SBFInitiativeHelper(SBFGameManager gameManager) implements SBFGameManagerHelper {

    /**
     * Determines the turn order for a given phase, setting the game's turn list and sending it to the Clients. Also
     * resets the turn index.
     *
     * @param phase The phase to find the turns for
     *
     * @see AbstractGame#resetTurnIndex()
     */
    void determineTurnOrder(GamePhase phase) {
        final List<SBFTurn> turns;
        if (phase.isDeployMinefields()) {
            turns = game().getPlayersList()
                  .stream()
                  .filter(Player::hasMinefields)
                  .map(p -> new SBFPlayerTurn(p.getId()))
                  .collect(Collectors.toList());

        } else if (phase.isFiring()) {
            turns = game().getInGameObjects()
                  .stream()
                  .filter(unit -> unit instanceof SBFFormation)
                  .filter(unit -> ((SBFFormation) unit).isDeployed()) //TODO roll into eligible!!! may be off board
                  .filter(unit -> ((SBFFormation) unit).isEligibleForPhase(phase))
                  .map(InGameObject::getOwnerId)
                  .map(SBFFormationTurn::new)
                  .collect(Collectors.toList());

            turns.sort(Comparator.comparing(t -> game().getPlayer(t.playerId()).getInitiative()));

        } else {
            // As a fallback, provide unsorted turns
            turns = game().getInGameObjects()
                  .stream()
                  .filter(unit -> unit instanceof SBFFormation)
                  .filter(unit -> ((SBFFormation) unit).isDeployed())
                  .filter(unit -> ((SBFFormation) unit).isEligibleForPhase(phase))
                  .map(InGameObject::getOwnerId)
                  .map(SBFFormationTurn::new)
                  .collect(Collectors.toList());

            // Now, assemble formations and sort by initiative and relative formation count
            Map<Integer, Long> unitCountsByPlayer = game().getInGameObjects()
                  .stream()
                  .filter(unit -> unit instanceof SBFFormation)
                  .filter(unit -> ((SBFFormation) unit).isDeployed())
                  .filter(unit -> ((SBFFormation) unit).isEligibleForPhase(phase))
                  .collect(Collectors.groupingBy(InGameObject::getOwnerId,
                        Collectors.counting()));

            if (!unitCountsByPlayer.isEmpty()) {
                final long lowestUnitCount = Collections.min(unitCountsByPlayer.values());

                int playerWithLowestUnitCount = unitCountsByPlayer.entrySet()
                      .stream()
                      .filter(e -> e.getValue() == lowestUnitCount)
                      .map(Map.Entry::getKey)
                      .findAny()
                      .orElse(Player.PLAYER_NONE);

                List<Integer> playersByInitiative = new ArrayList<>(unitCountsByPlayer.keySet());
                playersByInitiative.sort(Comparator.comparing(id -> game().getPlayer(id).getInitiative()));

                if ((playerWithLowestUnitCount != Player.PLAYER_NONE) && (lowestUnitCount > 0)) {
                    List<SBFTurn> sortedTurns = new ArrayList<>();

                    for (long initCycle = 0; initCycle < lowestUnitCount; initCycle++) {
                        long currentLowestUnitCount = Collections.min(unitCountsByPlayer.values());

                        for (int playerId : playersByInitiative) {
                            long unitsToMove = unitCountsByPlayer.get(playerId) / currentLowestUnitCount;
                            long remainingUnits = unitCountsByPlayer.get(playerId);
                            unitsToMove = Math.min(unitsToMove, remainingUnits);

                            for (long i = 0; i < unitsToMove; i++) {
                                sortedTurns.add(new SBFFormationTurn(playerId));
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
        send(packetHelper().createTurnListPacket());
    }

    void writeInitiativeReport() {
        writeHeader();
        writeInitiativeRolls();
        writeTurnOrder();
        writeFutureDeployment();
        writeWeatherReport();
    }

    private void writeTurnOrder() {
        if (!gameManager.usesDoubleBlind()) {
            addReport(new SBFReportEntry(1020));

            for (SBFTurn turn : game().getTurnsList()) {
                Player player = game().getPlayer(turn.playerId());
                addReport(new SBFPlayerNameReportEntry(player).indent().addNL());
            }
        }
    }

    private void writeFutureDeployment() {
        // remaining deployments
        Comparator<Deployable> comp = Comparator.comparingInt(Deployable::getDeployRound);
        List<Deployable> futureDeployments = game().getInGameObjects()
              .stream()
              .filter(unit -> unit instanceof Deployable)
              .map(unit -> (Deployable) unit)
              .filter(unit -> !unit.isDeployed())
              .sorted(comp)
              .toList();

        if (!futureDeployments.isEmpty()) {
            addReport(new SBFPublicReportEntry(1060));
            int round = -1;

            for (Deployable deployable : futureDeployments) {
                if (round != deployable.getDeployRound()) {
                    round = deployable.getDeployRound();
                    addReport(new SBFPublicReportEntry(1065).add(round));
                }

                SBFReportEntry r = new SBFReportEntry(1066).subject(((InGameObject) deployable).getId());
                r.add(((InGameObject) deployable).generalName());
                r.add("1");
                r.add("2");
                addReport(r);
            }
            addReport(new SBFPublicReportEntry(1210).newLines(2));
        }
    }

    private void writeWeatherReport() {
        PlanetaryConditions conditions = game().getPlanetaryConditions();
        addReport(new SBFPublicReportEntry(1025).add(conditions.getWindDirection().toString()));
        addReport(new SBFPublicReportEntry(1030).add(conditions.getWind().toString()));
        addReport(new SBFPublicReportEntry(1031).add(conditions.getWeather().toString()));
        addReport(new SBFPublicReportEntry(1032).add(conditions.getLight().toString()));
        addReport(new SBFPublicReportEntry(1033).add(conditions.getFog().toString()));
    }

    private void writeInitiativeRolls() {
        for (Team team : game().getTeams()) {
            // Teams with no active players can be ignored
            if (team.isObserverTeam()) {
                continue;
            }

            // If there is only one non-observer player, list them as the 'team', and use the team initiative
            if (team.getNonObserverSize() == 1) {
                final Player player = team.nonObserverPlayers().get(0);
                addReport(new SBFPlayerNameReportEntry(player));
                addReport(new SBFPublicReportEntry(1015).noNL());
                addReport(new SBFInitiativeRollReportEntry(team.getInitiative()));
            } else {
                // Multiple players. List the team, then break it down.
                SBFReportEntry r = new SBFPublicReportEntry(1015).add(Player.TEAM_NAMES[team.getId()]);
                r.add(team.getInitiative().toString());
                addReport(r);
                for (Player player : team.nonObserverPlayers()) {
                    addReport(new SBFPublicReportEntry(1015).indent()
                          .add(player.getName())
                          .add(player.getInitiative().toString()));
                }
            }
        }
    }

    private void writeHeader() {
        if (game().getLastPhase().isDeployment() || game().isDeploymentComplete() || !game().shouldDeployThisRound()) {
            addReport(new SBFReportHeader(1000).add(game().getCurrentRound()));
        } else {
            if (game().getCurrentRound() == 0) {
                addReport(new SBFReportHeader(1005));
            } else {
                addReport(new SBFReportHeader(1010).add(game().getCurrentRound()));
            }
        }
    }
}

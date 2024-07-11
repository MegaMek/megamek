/*
 * Copyright (c) 2024 - The MegaMek Team. All Rights Reserved.
 *
 * This file is part of MegaMek.
 *
 * MegaMek is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * MegaMek is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with MegaMek. If not, see <http://www.gnu.org/licenses/>.
 */
package megamek.server.sbf;

import megamek.common.*;
import megamek.common.enums.GamePhase;
import megamek.common.planetaryconditions.PlanetaryConditions;
import megamek.common.strategicBattleSystems.SBFFormation;
import megamek.common.strategicBattleSystems.SBFFormationTurn;
import megamek.common.strategicBattleSystems.SBFPlayerTurn;
import megamek.common.strategicBattleSystems.SBFTurn;

import java.util.*;
import java.util.stream.Collectors;

import static megamek.common.Report.publicReport;

public record SBFInitiativeHelper(SBFGameManager gameManager) implements SBFGameManagerHelper {

    /**
     * Determines the turn order for a given phase, setting the game's turn list and sending it to the
     * Clients. Also resets the turn index.
     *
     * @param phase The phase to find the turns for
     * @see AbstractGame#resetTurnIndex()
     */
    void determineTurnOrder(GamePhase phase) {
        final List<SBFTurn> turns;
        if (phase.isDeployMinefields()) {
            turns = game().getPlayersList().stream()
                    .filter(Player::hasMinefields)
                    .map(p -> new SBFPlayerTurn(p.getId()))
                    .collect(Collectors.toList());
        } else {
            turns = game().getInGameObjects().stream()
                    .filter(unit -> unit instanceof SBFFormation)
                    .filter(unit -> ((SBFFormation) unit).isDeployed())
                    .filter(unit -> ((SBFFormation) unit).isEligibleForPhase(phase))
                    .map(InGameObject::getOwnerId)
                    .map(SBFFormationTurn::new)
                    .collect(Collectors.toList());
        }
        //TODO sort by init and uneven count

        if (gameManager.usesAdvancedInitiative()) {
            //TODO ...
        }

        if (game().usesBattlefieldInt()) {
            //TODO ...
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
            // The turn order is different in movement phase
            // if a player has any "even" moving units. ???????????????????????????????????? SBF?
            Report r = new Report(1020, Report.PUBLIC);

            boolean hasEven = false;
            for (SBFTurn turn : game().getTurnsList()) {
                Player player = game().getPlayer(turn.playerId());
                if (null != player) {
                    r.add(player.getName());
//                        if (player.getEvenTurns() > 0) {
//                            hasEven = true;
//                        }
                }
            }
            r.newlines = 2;
            addReport(r);
//                if (hasEven) {
//                    r = new Report(1021, Report.PUBLIC);
//                    if ((game().getOptions().booleanOption(OptionsConstants.INIT_INF_DEPLOY_EVEN)
//                            || game().getOptions().booleanOption(OptionsConstants.INIT_PROTOS_MOVE_EVEN))
//                            && !game().getLastPhase().isEndReport()) {
//                        r.choose(true);
//                    } else {
//                        r.choose(false);
//                    }
//                    r.indent();
//                    r.newlines = 2;
//                    addReport(r);
//                }
        }
    }

    private void writeFutureDeployment() {
        // remaining deployments
        Comparator<Deployable> comp = Comparator.comparingInt(Deployable::getDeployRound);
//        comp = comp.thenComparingInt(InGameObject::getOwnerId);
//        comp = comp.thenComparingInt(Entity::getStartingPos);
//        List<Entity> ue = game().getEntitiesVector().stream().filter(e -> e.getDeployRound() > game().getCurrentRound()).sorted(comp).collect(Collectors.toList());
        List<Deployable> futureDeployments = game().getInGameObjects().stream()
                .filter(unit -> unit instanceof Deployable)
                .map(unit -> (Deployable) unit)
                .filter(unit -> !unit.isDeployed())
                .sorted(comp)
                .collect(Collectors.toList());

        if (!futureDeployments.isEmpty()) {
            addReport(new Report(1060, Report.PUBLIC));
            int round = -1;

            for (Deployable deployable : futureDeployments) {
                if (round != deployable.getDeployRound()) {
                    round = deployable.getDeployRound();
                    addReport(publicReport(1065).add(round));
                }

                Report r = new Report(1066).subject(((InGameObject) deployable).getId());
                r.add(((InGameObject) deployable).generalName());
                r.add("1");
                r.add("2");
                //TODO
//                r.addDesc(entity);
//                String s = IStartingPositions.START_LOCATION_NAMES[entity.getStartingPos()];
//                r.add(s);
                addReport(r);
            }
            addReport(publicReport(1210).newLines(2));
        }
    }

    private void writeWeatherReport() {
        PlanetaryConditions conditions = game().getPlanetaryConditions();
        addReport(publicReport(1025).add(conditions.getWindDirection().toString()).noNL());
        addReport(publicReport(1030).add(conditions.getWind().toString()).noNL());
        addReport(publicReport(1031).add(conditions.getWeather().toString()).noNL());
        addReport(publicReport(1032).add(conditions.getLight().toString()));
        addReport(publicReport(1033).add(conditions.getFog().toString()));
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
                Report r = publicReport(1015).add(player.getColorForPlayer());
                r.add(team.getInitiative().toString());
                addReport(r);
            } else {
                // Multiple players. List the team, then break it down.
                Report r = publicReport(1015).add(Player.TEAM_NAMES[team.getId()]);
                r.add(team.getInitiative().toString());
                addReport(r);
                for (Player player : team.nonObserverPlayers()) {
                    addReport(publicReport(1015).indent().add(player.getName()).add(player.getInitiative().toString()));
                }
            }
        }
    }

    private void writeHeader() {
        if (game().getLastPhase().isDeployment() || game().isDeploymentComplete()
                || !game().shouldDeployThisRound()) {
            addReport(publicReport(1000).add(game().getCurrentRound()));
        } else {
//            deployment = true;
            if (game().getCurrentRound() == 0) {
                addReport(publicReport(1005));
            } else {
                addReport(publicReport(1010).add(game().getCurrentRound()));
            }
        }
        // write separator
        addReport(new Report(1200, Report.PUBLIC));
    }
}
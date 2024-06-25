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
import megamek.common.options.OptionsConstants;
import megamek.common.planetaryconditions.PlanetaryConditions;
import megamek.common.strategicBattleSystems.SBFGame;
import megamek.server.SBFGameManager;

import java.util.Comparator;
import java.util.Enumeration;
import java.util.List;
import java.util.stream.Collectors;

public class SBFInitiativeHelper {

    private final SBFGameManager gameManager;

    public SBFInitiativeHelper(SBFGameManager gameManager) {
        this.gameManager = gameManager;
    }

    private SBFGame game() {
        return gameManager.getGame();
    } 
    
    private void addReport(ReportEntry reportEntry) {
        gameManager.addReport(reportEntry);
    }

    public void writeInitiativeReport() {
        boolean deployment = false;
        if (game().getLastPhase().isDeployment() || game().isDeploymentComplete()
                || !game().shouldDeployThisRound()) {
            addReport(Report.publicReport(1000).add(game().getCurrentRound()));
        } else {
//            deployment = true;
            if (game().getCurrentRound() == 0) {
                addReport(Report.publicReport(1005));
            } else {
                addReport(Report.publicReport(1010).add(game().getCurrentRound()));
            }
        }
        // write separator
        addReport(new Report(1200, Report.PUBLIC));


//            for (Team team : game().getTeams()) {
//                // Teams with no active players can be ignored
//                if (team.isObserverTeam()) {
//                    continue;
//                }
//
//                // If there is only one non-observer player, list
//                // them as the 'team', and use the team initiative
//                if (team.getNonObserverSize() == 1) {
//                    final Player player = team.nonObserverPlayers().get(0);
//                    r = new Report(1015, Report.PUBLIC);
//                    r.add(player.getColorForPlayer());
//                    r.add(team.getInitiative().toString());
//                    addReport(r);
//                } else {
//                    // Multiple players. List the team, then break it down.
//                    r = new Report(1015, Report.PUBLIC);
//                    r.add(Player.TEAM_NAMES[team.getId()]);
//                    r.add(team.getInitiative().toString());
//                    addReport(r);
//                    for (Player player : team.nonObserverPlayers()) {
//                        r = new Report(1015, Report.PUBLIC);
//                        r.indent();
//                        r.add(player.getName());
//                        r.add(player.getInitiative().toString());
//                        addReport(r);
//                    }
//                }
//            }
//
//            if (!doBlind()) {
//                // The turn order is different in movement phase
//                // if a player has any "even" moving units.
//                r = new Report(1020, Report.PUBLIC);
//
//                boolean hasEven = false;
//                for (Enumeration<GameTurn> i = game().getTurns(); i.hasMoreElements(); ) {
//                    GameTurn turn = i.nextElement();
//                    Player player = game().getPlayer(turn.playerId());
//                    if (null != player) {
//                        r.add(player.getName());
//                        if (player.getEvenTurns() > 0) {
//                            hasEven = true;
//                        }
//                    }
//                }
//                r.newlines = 2;
//                addReport(r);
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
//            }
//
////        addNewLines();
//
//            // remaining deployments
//            Comparator<Entity> comp = Comparator.comparingInt(Entity::getDeployRound);
//            comp = comp.thenComparingInt(Entity::getOwnerId);
//            comp = comp.thenComparingInt(Entity::getStartingPos);
//            List<Entity> ue = game().getEntitiesVector().stream().filter(e -> e.getDeployRound() > game().getCurrentRound()).sorted(comp).collect(Collectors.toList());
//            if (!ue.isEmpty()) {
//                r = new Report(1060, Report.PUBLIC);
//                addReport(r);
//                int round = -1;
//
//                for (Entity entity : ue) {
//                    if (round != entity.getDeployRound()) {
//                        round = entity.getDeployRound();
//                        r = new Report(1065, Report.PUBLIC);
//                        r.add(round);
//                        addReport(r);
//                    }
//
//                    r = new Report(1066);
//                    r.subject = entity.getId();
//                    r.addDesc(entity);
//                    String s = IStartingPositions.START_LOCATION_NAMES[entity.getStartingPos()];
//                    r.add(s);
//                    addReport(r);
//                }
//
//                r = new Report(1210, Report.PUBLIC);
//                r.newlines = 2;
//                addReport(r);
//            }
//
//
//            if (deployment) {
////                addNewLines();
//            }
    }

    /**
     * Write the initiative results to the report
     */
//    void writeInitiativeReport(boolean abbreviatedReport) {
//        // write to report
//        Report r;
//        boolean deployment = false;
//        if (!abbreviatedReport) {
//            r = new Report(1210);
//            r.type = Report.PUBLIC;
//            if (game().getLastPhase().isDeployment() || game().isDeploymentComplete()
//                    || !game().shouldDeployThisRound()) {
//                r.messageId = 1000;
//                r.add(game().getCurrentRound());
//            } else {
//                deployment = true;
//                if (game().getCurrentRound() == 0) {
//                    r.messageId = 1005;
//                } else {
//                    r.messageId = 1010;
//                    r.add(game().getCurrentRound());
//                }
//            }
//            addReport(r);
//            // write separator
//            addReport(new Report(1200, Report.PUBLIC));
//        } else {
//            addReport(new Report(1210, Report.PUBLIC));
//        }
//
//        if (game().getOptions().booleanOption(OptionsConstants.RPG_INDIVIDUAL_INITIATIVE)) {
//            r = new Report(1040, Report.PUBLIC);
//            addReport(r);
//            for (Enumeration<GameTurn> e = game().getTurns(); e.hasMoreElements(); ) {
//                GameTurn t = e.nextElement();
//                if (t instanceof SpecificEntityTurn) {
//                    Entity entity = game().getEntity(((SpecificEntityTurn) t).getEntityNum());
//                    if (entity.getDeployRound() <= game().getCurrentRound()) {
//                        r = new Report(1045);
//                        r.subject = entity.getId();
//                        r.addDesc(entity);
//                        r.add(entity.getInitiative().toString());
//                        addReport(r);
//                    }
//                } else {
//                    Player player = game().getPlayer(t.playerId());
//                    if (null != player) {
//                        r = new Report(1050, Report.PUBLIC);
//                        r.add(player.getColorForPlayer());
//                        addReport(r);
//                    }
//                }
//            }
//        } else {
//            for (Team team : game().getTeams()) {
//                // Teams with no active players can be ignored
//                if (team.isObserverTeam()) {
//                    continue;
//                }
//
//                // If there is only one non-observer player, list
//                // them as the 'team', and use the team initiative
//                if (team.getNonObserverSize() == 1) {
//                    final Player player = team.nonObserverPlayers().get(0);
//                    r = new Report(1015, Report.PUBLIC);
//                    r.add(player.getColorForPlayer());
//                    r.add(team.getInitiative().toString());
//                    addReport(r);
//                } else {
//                    // Multiple players. List the team, then break it down.
//                    r = new Report(1015, Report.PUBLIC);
//                    r.add(Player.TEAM_NAMES[team.getId()]);
//                    r.add(team.getInitiative().toString());
//                    addReport(r);
//                    for (Player player : team.nonObserverPlayers()) {
//                        r = new Report(1015, Report.PUBLIC);
//                        r.indent();
//                        r.add(player.getName());
//                        r.add(player.getInitiative().toString());
//                        addReport(r);
//                    }
//                }
//            }
//
//            if (!doBlind()) {
//                // The turn order is different in movement phase
//                // if a player has any "even" moving units.
//                r = new Report(1020, Report.PUBLIC);
//
//                boolean hasEven = false;
//                for (Enumeration<GameTurn> i = game().getTurns(); i.hasMoreElements(); ) {
//                    GameTurn turn = i.nextElement();
//                    Player player = game().getPlayer(turn.playerId());
//                    if (null != player) {
//                        r.add(player.getName());
//                        if (player.getEvenTurns() > 0) {
//                            hasEven = true;
//                        }
//                    }
//                }
//                r.newlines = 2;
//                addReport(r);
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
//            }
//        }
//
//        addNewLines();
//
//        if (!abbreviatedReport) {
//            // remaining deployments
//            Comparator<Entity> comp = Comparator.comparingInt(Entity::getDeployRound);
//            comp = comp.thenComparingInt(Entity::getOwnerId);
//            comp = comp.thenComparingInt(Entity::getStartingPos);
//            List<Entity> ue = game().getEntitiesVector().stream().filter(e -> e.getDeployRound() > game().getCurrentRound()).sorted(comp).collect(Collectors.toList());
//            if (!ue.isEmpty()) {
//                r = new Report(1060, Report.PUBLIC);
//                addReport(r);
//                int round = -1;
//
//                for (Entity entity : ue) {
//                    if (round != entity.getDeployRound()) {
//                        round = entity.getDeployRound();
//                        r = new Report(1065, Report.PUBLIC);
//                        r.add(round);
//                        addReport(r);
//                    }
//
//                    r = new Report(1066);
//                    r.subject = entity.getId();
//                    r.addDesc(entity);
//                    String s = IStartingPositions.START_LOCATION_NAMES[entity.getStartingPos()];
//                    r.add(s);
//                    addReport(r);
//                }
//
//                r = new Report(1210, Report.PUBLIC);
//                r.newlines = 2;
//                addReport(r);
//            }
//
//            // we don't much care about wind direction and such in a hard vacuum
//            if (!game().getBoard().inSpace()) {
//                // Wind direction and strength
//                PlanetaryConditions conditions = game().getPlanetaryConditions();
//                Report rWindDir = new Report(1025, Report.PUBLIC);
//                rWindDir.add(conditions.getWindDirection().toString());
//                rWindDir.newlines = 0;
//                Report rWindStr = new Report(1030, Report.PUBLIC);
//                rWindStr.add(conditions.getWind().toString());
//                rWindStr.newlines = 0;
//                Report rWeather = new Report(1031, Report.PUBLIC);
//                rWeather.add(conditions.getWeather().toString());
//                rWeather.newlines = 0;
//                Report rLight = new Report(1032, Report.PUBLIC);
//                rLight.add(conditions.getLight().toString());
//                Report rVis = new Report(1033, Report.PUBLIC);
//                rVis.add(conditions.getFog().toString());
//                addReport(rWindDir);
//                addReport(rWindStr);
//                addReport(rWeather);
//                addReport(rLight);
//                addReport(rVis);
//            }
//
//            if (deployment) {
//                addNewLines();
//            }
//        }
//    }
}

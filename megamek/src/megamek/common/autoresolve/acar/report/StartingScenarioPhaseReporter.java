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

import megamek.client.ui.swing.util.UIUtil;
import megamek.common.Entity;
import megamek.common.IGame;
import megamek.common.Player;
import megamek.common.autoresolve.acar.SimulationContext;
import megamek.common.autoresolve.acar.SimulationManager;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.function.Consumer;

import static megamek.client.ui.swing.tooltip.SBFInGameObjectTooltip.ownerColor;

public class StartingScenarioPhaseReporter implements IStartingScenarioPhaseReporter {

    private final IGame game;
    private final Consumer<PublicReportEntry> reportConsumer;

    private StartingScenarioPhaseReporter(IGame game, Consumer<PublicReportEntry> reportConsumer) {
        this.reportConsumer = reportConsumer;
        this.game = game;
    }

    public static IStartingScenarioPhaseReporter create(SimulationManager manager) {
        if (manager.isLogSuppressed()) {
            return DummyStartingScenarioPhaseReporter.instance();
        }
        return new StartingScenarioPhaseReporter(manager.getGame(), manager::addReport);
    }

    @Override
    public void header() {
        reportConsumer.accept(new PublicReportEntry("acar.header"));
        reportConsumer.accept(new SummaryPlaceholderEntry());
        reportConsumer.accept(new PublicReportEntry("acar.header.startingScenario"));
    }


    @Override
    public void formationsSetup(SimulationManager gameManager) {
        var players = gameManager.getGame().getPlayersList();
        var teamMap = new HashMap<Integer, List<Player>>();

        for (var player : players) {
            var team = player.getTeam();
            if (!teamMap.containsKey(team)) {
                teamMap.put(team, new ArrayList<>());
            }
            teamMap.get(team).add(player);
        }

        for (var team : teamMap.keySet()) {
            var teamPlayers = teamMap.get(team);
            var teamReport = new PublicReportEntry("acar.header.teamFormationsHeader").add(team);
            reportConsumer.accept(teamReport);
            for (var player : teamPlayers) {
                playerFinalReport(player);
            }
        }
    }

    private void playerFinalReport(Player player) {
        var formations = ((SimulationContext) game).getActiveFormations(player);

        reportConsumer.accept(new PublicReportEntry("acar.header.teamFormations")
            .add(new PlayerNameReportEntry(player).reportText())
            .add(formations.size()).indent());

        for (var formation : formations) {
            var color = ownerColor(formation, game);
            if (!formation.isSingleEntity()) {
                reportConsumer.accept(new PublicReportEntry("acar.startingScenario.formation.numberOfUnits")
                    .add(new FormationReportEntry(
                        formation.getName(), "", UIUtil.hexColor(color)).reportText())
                    .add(formation.getUnits().size())
                    .indent(1));
            }

            for (var unit : formation.getUnits()) {
                if (!formation.isSingleEntity()) {
                    reportConsumer.accept(new PublicReportEntry("acar.startingScenario.unit.numberOfElements")
                        .add(new UnitReportEntry(unit, ownerColor(formation, game)).reportText())
                        .add(unit.getElements().size())
                        .indent(2));
                }
                for (var element : unit.getElements()) {
                    var entity = (Entity) game.getInGameObject(element.getId()).orElseThrow();
                    var armor = entity.getArmorRemainingPercent();
                    var internal = entity.getInternalRemainingPercent();
                    var crew = entity.getCrew();
                    reportConsumer.accept(new PublicReportEntry("acar.startingScenario.unitStats")
                        .add(new EntityNameReportEntry(entity).reportText())
                        .add(String.format("%.2f%%", armor * 100))
                        .add(String.format("%.2f%%", internal * 100))
                        .add(crew.getName())
                        .add(crew.getHits())
                        .indent(3));
                }
            }
        }
    }

}

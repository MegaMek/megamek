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

import megamek.common.Entity;
import megamek.common.IGame;
import megamek.common.Player;
import megamek.common.autoresolve.acar.SimulationManager;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.function.Consumer;

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
            var teamReport = new PublicReportEntry("acar.header.teamFormations").add(team);
            reportConsumer.accept(teamReport);
            for (var player : teamPlayers) {
                playerFinalReport(player);
            }
        }
    }

    private void playerFinalReport(Player player) {
        var playerEntities = game.getInGameObjects().stream()
            .filter(e -> e.getOwnerId() == player.getId())
            .filter(Entity.class::isInstance)
            .map(Entity.class::cast)
            .toList();

        reportConsumer.accept(new PublicReportEntry("acar.startingScenario.numberOfUnits").add(new PlayerNameReportEntry(player).reportText())
            .add(playerEntities.size()).indent());

        for (var entity : playerEntities) {
            var armor = entity.getArmorRemainingPercent();
            if (armor < 0d) {
                armor = 0d;
            }
            reportConsumer.accept(new PublicReportEntry("acar.startingScenario.unitStats")
                .add(new EntityNameReportEntry(entity).reportText())
                .add(String.format("%.2f%%", armor * 100))
                .add(String.format("%.2f%%", entity.getInternalRemainingPercent() * 100))
                .add(entity.getCrew().getName())
                .add(entity.getCrew().getHits())
                .indent(2));
        }
    }

}

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
import megamek.common.IAero;
import megamek.common.IGame;
import megamek.common.Player;
import megamek.common.autoresolve.acar.SimulationContext;
import megamek.common.autoresolve.acar.SimulationManager;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.function.Consumer;

public class VictoryPhaseReporter implements IVictoryPhaseReporter {

    private final IGame game;
    private final Consumer<PublicReportEntry> reportConsumer;

    private VictoryPhaseReporter(IGame game, Consumer<PublicReportEntry> reportConsumer) {
        this.reportConsumer = reportConsumer;
        this.game = game;
    }

    public static IVictoryPhaseReporter create(SimulationManager manager) {
        if (manager.isLogSuppressed()) {
            return DummyVictoryPhaseReporter.instance();
        }
        return new VictoryPhaseReporter(manager.getGame(), manager::addReport);
    }


    @Override
    public void victoryHeader() {
        reportConsumer.accept(new DividerEntry());
        reportConsumer.accept(new ReportEntryWithAnchor("acar.endOfCombat.header", "end-of-combat").noNL());
        reportConsumer.accept(new LinkEntry("acar.link.backRef", "summary-end-of-combat"));
    }

    @Override
    public void victoryResult(SimulationManager gameManager) {
        var players = gameManager.getGame().getPlayersList();
        var teamMap = new HashMap<Integer, List<Player>>();

        for (var player : players) {
            var team = player.getTeam();
            if (!teamMap.containsKey(team)) {
                teamMap.put(team, new ArrayList<>());
            }
            teamMap.get(team).add(player);
        }

        var victoryResult = gameManager.getCurrentVictoryResult();

        reportConsumer.accept(new ReportEntryWithAnchor("acar.victory.teamVictorious", "victory").add(victoryResult.getWinningTeam()));
        reportConsumer.accept(new DividerEntry());
        for (var team : teamMap.keySet()) {
            var teamPlayers = teamMap.get(team);
            reportConsumer.accept(new ReportEntryWithAnchor("acar.endOfCombat.teamReportHeader", "end-team-" + team).add(team).noNL());
            for (var player : teamPlayers) {
                playerFinalReport(player);
            }
            reportConsumer.accept(new DividerEntry());
        }
    }

    @Override
    public void playerFinalReport(Player player) {
        var playerEntities = game.getInGameObjects().stream()
            .filter(e -> e.getOwnerId() == player.getId())
            .filter(Entity.class::isInstance)
            .map(Entity.class::cast)
            .toList();

        reportConsumer.accept(new LinkEntry("acar.link.backRef", "remaining-" + player.getId()).noNL());
        reportConsumer.accept(new ReportEntryWithAnchor("acar.endOfCombat.teamRemainingUnits", "end-player-" + player.getId() + "-remaining").noNL()
            .add(new PlayerNameReportEntry(player).reportText())
            .add(playerEntities.size()).indent());


        for (var entity : playerEntities) {
            double armor = Math.max(entity.getArmorRemainingPercent(), 0d);
            double internal = entity instanceof IAero ? ((IAero) entity).getSI() / (double) ((IAero) entity).get0SI()
                : entity.getInternalRemainingPercent();
            reportConsumer.accept(new PublicReportEntry("acar.endOfCombat.teamUnitStats")
                .add(new EntityNameReportEntry(entity).reportText())
                .add(String.format("%.2f%%", armor * 100))
                .add(String.format("%.2f%%", internal * 100))
                .add(entity.getCrew().getName())
                .add(entity.getCrew().getHits())
                .indent(2));
        }

        var deadEntities = game.getGraveyard().stream()
            .filter(e -> e.getOwnerId() == player.getId())
            .filter(Entity.class::isInstance)
            .map(Entity.class::cast)
            .toList();

        reportConsumer.accept(new LinkEntry("acar.link.backRef", "destroyed-" + player.getId()).noNL());
        reportConsumer.accept(new ReportEntryWithAnchor("acar.endOfCombat.teamDestroyedUnits", "end-player-" + player.getId() + "-destroyed")
            .add(new PlayerNameReportEntry(player).reportText())
            .add(deadEntities.size())
            .indent(1));

        for (var entity : deadEntities) {
            double armor = Math.max(entity.getArmorRemainingPercent(), 0d);
            double internal = entity instanceof IAero ? ((IAero) entity).getSI() / (double) ((IAero) entity).get0SI()
                : entity.getInternalRemainingPercent();

            reportConsumer.accept(new PublicReportEntry("acar.endOfCombat.teamUnitStats")
                .add(new EntityNameReportEntry(entity).reportText())
                .add(String.format("%.2f%%", armor * 100))
                .add(String.format("%.2f%%", internal * 100))
                .add(entity.getCrew().getName())
                .add(entity.getCrew().getHits())
                .indent(2));
        }

        var retreatingEntities = ((SimulationContext) game).getRetreatingUnits().stream()
            .filter(e -> e.getOwnerId() == player.getId())
            .toList();

        reportConsumer.accept(new LinkEntry("acar.link.backRef", "retreating-" + player.getId()).noNL());
        reportConsumer.accept(new ReportEntryWithAnchor("acar.endOfCombat.teamRetreatingUnits", "end-player-" + player.getId() + "-retreating")
            .add(new PlayerNameReportEntry(player).reportText())
            .add(retreatingEntities.size())
            .indent());


        for (var entity : retreatingEntities) {
            double armor = Math.max(entity.getArmorRemainingPercent(), 0d);
            double internal = entity instanceof IAero ? ((IAero) entity).getSI() / (double) ((IAero) entity).get0SI()
                : entity.getInternalRemainingPercent();
            reportConsumer.accept(new PublicReportEntry("acar.endOfCombat.teamUnitStats")
                .add(new EntityNameReportEntry(entity).reportText())
                .add(String.format("%.2f%%", armor * 100))
                .add(String.format("%.2f%%", internal * 100))
                .add(entity.getCrew().getName())
                .add(entity.getCrew().getHits())
                .indent(2)
            );
        }
    }
}

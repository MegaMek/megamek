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

package megamek.common.autoresolve.acar.report;

import megamek.common.Entity;
import megamek.common.IGame;
import megamek.common.Player;
import mekhq.campaign.autoresolve.acar.SimulationContext;
import mekhq.campaign.autoresolve.acar.SimulationManager;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.function.Consumer;

public class VictoryPhaseReporter {

    private final IGame game;
    private final Consumer<PublicReportEntry> reportConsumer;

    public VictoryPhaseReporter(IGame game, Consumer<PublicReportEntry> reportConsumer) {
        this.reportConsumer = reportConsumer;
        this.game = game;
    }

    public void victoryHeader() {
        reportConsumer.accept(new PublicReportEntry(999));
        reportConsumer.accept(new PublicReportEntry(5000));
    }

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

        for (var team : teamMap.keySet()) {
            var teamPlayers = teamMap.get(team);
            var teamReport = new PublicReportEntry(5002).add(team);
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

        reportConsumer.accept(new PublicReportEntry(5003).add(new PlayerNameReportEntry(player).reportText())
            .add(playerEntities.size()).indent());

        for (var entity : playerEntities) {
            reportConsumer.accept(new PublicReportEntry(5004)
                .add(new EntityNameReportEntry(entity).reportText())
                .add(String.format("%.2f%%", (entity).getArmorRemainingPercent() * 100))
                .add(String.format("%.2f%%", (entity).getInternalRemainingPercent() * 100))
                .add(entity.getCrew().getName())
                .add(entity.getCrew().getHits())
                .indent(2));
        }

        var deadEntities = game.getGraveyard().stream()
            .filter(e -> e.getOwnerId() == player.getId())
            .filter(Entity.class::isInstance)
            .map(Entity.class::cast)
            .toList();

        reportConsumer.accept(new PublicReportEntry(5006).add(new PlayerNameReportEntry(player).reportText())
            .add(deadEntities.size()).indent());

        for (var entity : deadEntities) {
            reportConsumer.accept(new PublicReportEntry(5004)
                .add(new EntityNameReportEntry(entity).reportText())
                .add(String.format("%.2f%%", entity.getArmorRemainingPercent() * 100))
                .add(String.format("%.2f%%", entity.getInternalRemainingPercent() * 100))
                .add(entity.getCrew().getName())
                .add(entity.getCrew().getHits())
                .indent(2));
        }

        var retreatingEntities = ((SimulationContext) game).getRetreatingUnits().stream()
            .filter(e -> e.getOwnerId() == player.getId())
            .toList();

        reportConsumer.accept(new PublicReportEntry(5007).add(new PlayerNameReportEntry(player).reportText())
            .add(retreatingEntities.size()).indent());

        for (var entity : retreatingEntities) {
            reportConsumer.accept(new PublicReportEntry(5004)
                .add(new EntityNameReportEntry(entity).reportText())
                .add(String.format("%.2f%%", entity.getArmorRemainingPercent() * 100))
                .add(String.format("%.2f%%", entity.getInternalRemainingPercent() * 100))
                .add(entity.getCrew().getName())
                .add(entity.getCrew().getHits())
                .indent(2));
        }
    }
}

/*
 * Copyright (C) 2025 The MegaMek Team. All Rights Reserved.
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

package megamek.common.autoResolve.acar.report;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.function.Consumer;

import megamek.common.Player;
import megamek.common.autoResolve.acar.SimulationContext;
import megamek.common.autoResolve.acar.SimulationManager;
import megamek.common.game.IGame;
import megamek.common.units.Entity;
import megamek.common.units.IAero;

public record VictoryPhaseReporter(IGame game, Consumer<PublicReportEntry> reportConsumer)
      implements IVictoryPhaseReporter {

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

        reportConsumer.accept(new ReportEntryWithAnchor("acar.victory.teamVictorious",
              "victory").add(victoryResult.getWinningTeam()));
        reportConsumer.accept(new DividerEntry());
        for (var team : teamMap.keySet()) {
            var teamPlayers = teamMap.get(team);
            reportConsumer.accept(new ReportEntryWithAnchor("acar.endOfCombat.teamReportHeader",
                  "end-team-" + team).add(team).noNL());
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
        reportConsumer.accept(new ReportEntryWithAnchor("acar.endOfCombat.teamRemainingUnits",
              "end-player-" + player.getId() + "-remaining").noNL()
              .add(new PlayerNameReportEntry(player).reportText())
              .add(playerEntities.size()).indent());


        for (var entity : playerEntities) {
            double armor = Math.max(entity.getArmorRemainingPercent(), 0d);
            double internal = entity instanceof IAero ? ((IAero) entity).getSI() / (double) ((IAero) entity).getOSI()
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
        reportConsumer.accept(new ReportEntryWithAnchor("acar.endOfCombat.teamDestroyedUnits",
              "end-player-" + player.getId() + "-destroyed")
              .add(new PlayerNameReportEntry(player).reportText())
              .add(deadEntities.size())
              .indent(1));

        for (var entity : deadEntities) {
            double armor = Math.max(entity.getArmorRemainingPercent(), 0d);
            double internal = entity instanceof IAero ? ((IAero) entity).getSI() / (double) ((IAero) entity).getOSI()
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
        reportConsumer.accept(new ReportEntryWithAnchor("acar.endOfCombat.teamRetreatingUnits",
              "end-player-" + player.getId() + "-retreating")
              .add(new PlayerNameReportEntry(player).reportText())
              .add(retreatingEntities.size())
              .indent());


        for (var entity : retreatingEntities) {
            double armor = Math.max(entity.getArmorRemainingPercent(), 0d);
            double internal = entity instanceof IAero ? ((IAero) entity).getSI() / (double) ((IAero) entity).getOSI()
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

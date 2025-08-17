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

package megamek.common.autoresolve.acar.report;

import static megamek.client.ui.clientGUI.tooltip.SBFInGameObjectTooltip.ownerColor;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.function.Consumer;

import megamek.client.ui.util.UIUtil;
import megamek.common.Player;
import megamek.common.autoresolve.acar.SimulationContext;
import megamek.common.autoresolve.acar.SimulationManager;
import megamek.common.game.IGame;
import megamek.common.units.Entity;
import megamek.common.units.IAero;

public record StartingScenarioPhaseReporter(IGame game, Consumer<PublicReportEntry> reportConsumer)
      implements IStartingScenarioPhaseReporter {

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
                    var crew = entity.getCrew();
                    var armor = Math.max(entity.getArmorRemainingPercent(), 0d);
                    var internal = entity instanceof IAero ?
                          ((IAero) entity).getSI() / (double) ((IAero) entity).getOSI()
                          :
                          entity.getInternalRemainingPercent();
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

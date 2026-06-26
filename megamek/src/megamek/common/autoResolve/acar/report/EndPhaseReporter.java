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

import static megamek.client.ui.clientGUI.tooltip.SBFInGameObjectTooltip.ownerColor;

import java.util.Map;
import java.util.function.Consumer;

import megamek.common.autoResolve.acar.SimulationManager;
import megamek.common.autoResolve.component.Formation;
import megamek.common.game.IGame;
import megamek.common.interfaces.IEntityRemovalConditions;
import megamek.common.strategicBattleSystems.SBFUnit;
import megamek.common.units.Entity;

public record EndPhaseReporter(IGame game, Consumer<PublicReportEntry> reportConsumer) implements IEndPhaseReporter {

    private static final Map<Integer, String> reportIdForEachRemovalCondition = Map.of(
          IEntityRemovalConditions.REMOVE_DEVASTATED, "acar.endPhase.devastated",
          IEntityRemovalConditions.REMOVE_EJECTED, "acar.endPhase.destroyedPilot",
          IEntityRemovalConditions.REMOVE_PUSHED, "acar.endPhase.destroyedOffBoard",
          IEntityRemovalConditions.REMOVE_CAPTURED, "acar.endPhase.captured",
          IEntityRemovalConditions.REMOVE_IN_RETREAT, "acar.endPhase.retreat",
          IEntityRemovalConditions.REMOVE_NEVER_JOINED, "acar.endPhase.missing",
          IEntityRemovalConditions.REMOVE_SALVAGEABLE, "acar.endPhase.destroyedSalvage");

    private static final String MSG_ID_UNIT_DESTROYED_UNKNOWINGLY = "acar.endPhase.destroyedSurprise";

    public static IEndPhaseReporter create(SimulationManager manager) {
        if (manager.isLogSuppressed()) {
            return DummyEndPhaseReporter.instance();
        }
        return new EndPhaseReporter(manager.getGame(), manager::addReport);
    }


    @Override
    public void endPhaseHeader() {
        reportConsumer.accept(new ReportEntryWithAnchor("acar.endPhase.header",
              "round-" + game.getCurrentRound() + "-end").noNL());
        reportConsumer.accept(new LinkEntry("acar.link.backRef", "summary-round-" + game.getCurrentRound() + "-end"));
    }

    @Override
    public void reportUnitDestroyed(Formation formation, SBFUnit unit) {
        var names = unit.getElements().stream().map(e -> e.getName() + " ID:" + e.getId()).toList();
        var r = new PublicReportEntry("acar.endPhase.unitDestroyed")
              .add(new UnitReportEntry(formation, unit, ownerColor(formation, game)).reportText())
              .add(String.join(", ", names));
        reportConsumer.accept(r);
    }

    @Override
    public void reportElementDestroyed(Formation formation, SBFUnit unit, Entity entity) {
        var removalCondition = entity.getRemovalCondition();
        var reportId = reportIdForEachRemovalCondition.getOrDefault(removalCondition,
              MSG_ID_UNIT_DESTROYED_UNKNOWINGLY);

        var r = new PublicReportEntry(reportId)
              .add(new UnitReportEntry(formation, unit, ownerColor(formation, game)).reportText());
        reportConsumer.accept(r);
    }
}

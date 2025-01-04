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
import megamek.common.IEntityRemovalConditions;
import megamek.common.IGame;

import java.util.Map;
import java.util.function.Consumer;

public class EndPhaseReporter {

    private final Consumer<PublicReportEntry> reportConsumer;
    private static final Map<Integer, Integer> reportIdForEachRemovalCondition = Map.of(
        IEntityRemovalConditions.REMOVE_DEVASTATED, 3337,
        IEntityRemovalConditions.REMOVE_EJECTED, 3338,
        IEntityRemovalConditions.REMOVE_PUSHED, 3339,
        IEntityRemovalConditions.REMOVE_CAPTURED, 3340,
        IEntityRemovalConditions.REMOVE_IN_RETREAT, 3341,
        IEntityRemovalConditions.REMOVE_NEVER_JOINED, 3342,
        IEntityRemovalConditions.REMOVE_SALVAGEABLE, 3343);

    private static final int MSG_ID_UNIT_DESTROYED_UNKNOWINGLY = 3344;

    public EndPhaseReporter(IGame game, Consumer<PublicReportEntry> reportConsumer) {
        this.reportConsumer = reportConsumer;
    }

    public void endPhaseHeader() {
        reportConsumer.accept(new PublicReportEntry(999));
        reportConsumer.accept(new PublicReportEntry(3299));
    }

    public void reportUnitDestroyed(Entity entity) {
        var removalCondition = entity.getRemovalCondition();
        var reportId = reportIdForEachRemovalCondition.getOrDefault(removalCondition, MSG_ID_UNIT_DESTROYED_UNKNOWINGLY);

        var r = new PublicReportEntry(reportId)
            .add(new EntityNameReportEntry(entity).reportText());
        reportConsumer.accept(r);
    }

    public void destroyedUnitsHeader() {
        reportConsumer.accept(new PublicReportEntry(3298).indent());
    }
}

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
package megamek.server.trigger;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import megamek.common.Entity;
import megamek.common.Game;
import megamek.common.IGame;
import megamek.common.InGameObject;
import megamek.common.Player;
import megamek.common.annotations.Nullable;
import megamek.logging.MMLogger;

/**
 * This Trigger reacts when the count of units that were killed is in a given
 * range.
 *
 * @see Entity#isDestroyed()
 * @see Entity#isCarcass()
 */
public class KilledUnitsTrigger implements Trigger {
    private static final MMLogger logger = MMLogger.create(KilledUnitsTrigger.class);

    private final String playerName;
    private final int minUnitCount;
    private final int maxUnitCount;
    private final List<Integer> unitIds;

    /**
     * Creates a Trigger that reacts when the count of units that were killed is
     * between the given
     * min and maxUnitCount.
     * When the playerName is blank, units of all players are counted, otherwise
     * only
     * those of the given player.
     * When a list of unit IDs is given, only those units will be considered.
     * Note that destroyed units can spawn pilots, so providing an ID list to
     * prevent counting the wrong
     * units usually makes sense.
     * Note that this trigger will react multiple times. Use {@link OnceTrigger} to
     * limit it to one-time-only.
     *
     * @param playerName   A player name to limit the checked units to; may be null
     * @param unitIds      A list of Ids to limit the checked units to; when empty,
     *                     all units are considered
     * @param minUnitCount the minimum unit count to react to
     * @param maxUnitCount the maximum unit count to react to
     */
    public KilledUnitsTrigger(@Nullable String playerName, List<Integer> unitIds, int minUnitCount, int maxUnitCount) {
        this.playerName = Objects.requireNonNullElse(playerName, "");
        this.minUnitCount = minUnitCount;
        this.maxUnitCount = maxUnitCount;
        this.unitIds = (unitIds == null) ? new ArrayList<>() : new ArrayList<>(unitIds);
    }

    /**
     * Creates a Trigger that reacts when the given unit is killed.
     * Note that this trigger will react multiple times. Use {@link OnceTrigger} to
     * limit it to one-time-only.
     *
     * @param unitId The unit to look at
     */
    public KilledUnitsTrigger(int unitId) {
        this(null, List.of(unitId), 1, 1);
    }

    /**
     * Creates a Trigger that reacts when the count of units that were killed is
     * equal to the given count.
     * When a list of unit IDs is given, only those units will be considered. Note
     * that destroyed units can
     * spawn pilots, so providing an ID list to prevent counting the wrong units
     * usually makes sense.
     * Note that this trigger will react multiple times. Use {@link OnceTrigger} to
     * limit it to one-time-only.
     *
     * @param unitIds         A list of Ids to limit the checked units to; when
     *                        empty, all units are considered
     * @param killedUnitCount The count of killed units to react to
     */
    public KilledUnitsTrigger(List<Integer> unitIds, int killedUnitCount) {
        this(null, unitIds, killedUnitCount, killedUnitCount);
    }

    @Override
    public boolean isTriggered(IGame game, TriggerSituation event) {
        if (game instanceof Game) {
            List<InGameObject> allUnits = game.getInGameObjects();
            allUnits.addAll(game.getGraveyard());
            long killedUnitCount = allUnits.stream()
                    .filter(this::matchesIdList)
                    .filter(e -> matchesPlayerName(game.getPlayer(e.getOwnerId())))
                    .filter(e -> e instanceof Entity)
                    .map(e -> (Entity) e)
                    .filter(e -> e.isDestroyed() || e.isCarcass())
                    .count();
            return (killedUnitCount >= minUnitCount) && (killedUnitCount <= maxUnitCount);
        } else {
            logger.warn("UnitKilledTrigger is currently only available for TW games.");
            return false;
        }
    }

    private boolean matchesPlayerName(Player player) {
        return playerName.isBlank() || playerName.equals(player.getName());
    }

    private boolean matchesIdList(InGameObject unit) {
        return unitIds.isEmpty() || unitIds.contains(unit.getId());
    }
}

/*
 * Copyright (C) 2024-2025 The MegaMek Team. All Rights Reserved.
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

package megamek.server.trigger;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import megamek.common.units.Entity;
import megamek.common.interfaces.IEntityRemovalConditions;
import megamek.common.game.IGame;
import megamek.common.game.InGameObject;
import megamek.common.Player;
import megamek.common.annotations.Nullable;

/**
 * This Trigger reacts when the count of units that fled the battlefield is equal to the given count. When the
 * playerName is blank, units of all players are counted, otherwise only those of the given player. When a list of unit
 * IDs is given, only those units will be considered. This Trigger will only ever trigger once. Note that destroyed
 * units can spawn pilots that in turn can flee, so providing an ID list can make sense. Note that this trigger will
 * react multiple times. Use {@link OnceTrigger} to limit it to one-time-only.
 */
public class FledUnitsTrigger implements Trigger {

    private final String playerName;
    private final int minUnitCount;
    private final int maxUnitCount;
    private final List<Integer> unitIds;

    public FledUnitsTrigger(@Nullable String playerName, List<Integer> unitIds, int minUnitCount, int maxUnitCount) {
        this.playerName = Objects.requireNonNullElse(playerName, "");
        this.minUnitCount = minUnitCount;
        this.maxUnitCount = maxUnitCount;
        this.unitIds = (unitIds == null) ? new ArrayList<>() : new ArrayList<>(unitIds);
    }

    public FledUnitsTrigger(@Nullable String playerName, List<Integer> unitIds, int fledUnitCount) {
        this(playerName, unitIds, fledUnitCount, fledUnitCount);
    }

    public FledUnitsTrigger(@Nullable String playerName, int fledUnitCount) {
        this(playerName, new ArrayList<>(), fledUnitCount);
    }

    public FledUnitsTrigger(@Nullable String playerName, int minUnitCount, int maxUnitCount) {
        this(playerName, new ArrayList<>(), minUnitCount, maxUnitCount);
    }

    @Override
    public boolean isTriggered(IGame game, TriggerSituation event) {
        long fledUnitCount = game.getGraveyard().stream()
              .filter(this::matchesIdList)
              .filter(e -> matchesPlayerName(game.getPlayer(e.getOwnerId())))
              .filter(e -> e instanceof Entity)
              .map(e -> (Entity) e)
              .filter(e -> e.getRemovalCondition() == IEntityRemovalConditions.REMOVE_IN_RETREAT)
              .count();

        return (fledUnitCount >= minUnitCount) && (fledUnitCount <= maxUnitCount);
    }

    private boolean matchesPlayerName(Player player) {
        return playerName.isBlank() || playerName.equals(player.getName());
    }

    private boolean matchesIdList(InGameObject unit) {
        return unitIds.isEmpty() || unitIds.contains(unit.getId());
    }

    @Override
    public String toString() {
        String result = "FledUnits: ";
        result += (minUnitCount >= 0 ? minUnitCount : "0") + (maxUnitCount < Integer.MAX_VALUE ?
              "-" + maxUnitCount :
              "+") + " of ";
        result += playerName.isBlank() ? "" : playerName + "/";
        result += unitIds.toString();
        return result;
    }
}

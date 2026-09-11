/*
 * Copyright (C) 2025-2026 The MegaMek Team. All Rights Reserved.
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
package megamek.common.actions;


import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import megamek.common.game.Game;
import megamek.common.units.Entity;

/**
 * Action to INITIATE new infantry vs. infantry combat in a building. Only valid when NO combat currently exists in the
 * target building.
 */
public class InitiateInfantryCombatAction extends InfantryCombatAction {

    /** The friendly units in the building that attack alongside the initiator, by id; never {@code null}. */
    private final List<Integer> committedUnitIds;

    /**
     * Creates a new initiate infantry combat action.
     *
     * @param entityId the attacking infantry entity ID
     * @param targetId the target entity ID (AbstractBuildingEntity)
     */
    public InitiateInfantryCombatAction(int entityId, int targetId) {
        this(entityId, targetId, List.of());
    }

    /**
     * Creates an initiation that commits other friendly units in the building with the initiator (TO:AR p. 169: the
     * whole force present attacks, less what the player holds back).
     *
     * @param entityId         the initiating infantry entity ID
     * @param targetId         the target entity ID (AbstractBuildingEntity)
     * @param committedUnitIds the other units' ids; copied
     */
    public InitiateInfantryCombatAction(int entityId, int targetId, List<Integer> committedUnitIds) {
        super(entityId, targetId, false);
        this.committedUnitIds = new ArrayList<>(committedUnitIds);
    }

    /**
     * @return the ids of the friendly units committed with the initiator, empty when it attacks alone
     */
    public List<Integer> getCommittedUnitIds() {
        return Collections.unmodifiableList(committedUnitIds);
    }

    @Override
    public String toSummaryString(Game game) {
        Entity target = game.getEntity(getTargetId());
        String targetName = (target != null) ? target.getDisplayName() : "Unknown";
        return "Initiate combat at " + targetName;
    }
}

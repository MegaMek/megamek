/*
 * Copyright (C) 2000-2002 Ben Mazur (bmazur@sev.org)
 * Copyright (C) 2003-2025 The MegaMek Team. All Rights Reserved.
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

import java.io.Serial;

import jakarta.annotation.Nullable;
import megamek.client.ui.Messages;
import megamek.common.game.Game;
import megamek.common.units.Entity;
import megamek.common.units.Targetable;

public class SpotAction extends AbstractEntityAction {

    /**
     *
     */
    @Serial
    private static final long serialVersionUID = 3629300334304478911L;
    private final int targetId;
    private @Nullable Integer targetType;

    public SpotAction(int entityId, int targetId) {
        super(entityId);
        this.targetId = targetId;
    }

    public SpotAction(int entityId, Targetable target) {
        this(entityId, target.getId());
        targetType = (target.getTargetType() == Targetable.TYPE_ENTITY) ? null : target.getTargetType();
    }

    public int getTargetId() {
        return targetId;
    }

    public int getTargetType() {
        return (targetType == null) ? Targetable.TYPE_ENTITY : targetType;
    }

    public @Nullable Targetable getTarget(Game game) {
        return (targetType == null) ? game.getEntity(targetId) : game.getTarget(targetType, targetId);
    }

    @Override
    public String toSummaryString(final Game game) {
        Targetable target = getTarget(game);
        String name = (target instanceof Entity entity) ? entity.getShortName()
              : (target == null) ? "" : target.getDisplayName();
        return Messages.getString("BoardView1.SpotAction", name);
    }
}

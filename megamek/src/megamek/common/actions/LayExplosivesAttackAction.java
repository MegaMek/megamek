/*
 * Copyright (C) 2000-2004 Ben Mazur (bmazur@sev.org)
 * Copyright (C) 2005-2025 The MegaMek Team. All Rights Reserved.
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

import megamek.common.ToHitData;
import megamek.common.equipment.EquipmentType;
import megamek.common.equipment.MiscType;
import megamek.common.equipment.Mounted;
import megamek.common.equipment.enums.MiscTypeFlag;
import megamek.common.game.Game;
import megamek.common.rolls.TargetRoll;
import megamek.common.units.Entity;
import megamek.common.units.Infantry;
import megamek.common.units.Targetable;
import megamek.common.weapons.infantry.InfantryWeapon;

public class LayExplosivesAttackAction extends AbstractAttackAction {
    @Serial
    private static final long serialVersionUID = -8799415934269686590L;

    public LayExplosivesAttackAction(int entityId, int targetId) {
        super(entityId, targetId);
    }

    public LayExplosivesAttackAction(int entityId, int targetType, int targetId) {
        super(entityId, targetType, targetId);
    }

    /**
     * Damage that the specified platoon does with explosives
     */
    public static int getDamageFor(Entity entity) {
        if (!(entity instanceof Infantry inf)) {
            return 0;
        }
        InfantryWeapon srmWeapon = (InfantryWeapon) EquipmentType
              .get("SRM Launcher (Std, Two-Shot)");
        int dmg = (int) Math.round(srmWeapon.getInfantryDamage()
              * inf.getShootingStrength());
        int numTurns = Math.min(6, inf.turnsLayingExplosives);
        return dmg * numTurns;
    }

    public ToHitData toHit(Game game) {
        Targetable target = game.getTarget(getTargetType(), getTargetId());

        if (target == null) {
            return null;
        }

        return toHit(game, getEntityId(), target);
    }

    /**
     * To-hit number, i.e. is the action possible
     */
    public static ToHitData toHit(Game game, int attackerId, Targetable target) {
        final Entity ae = game.getEntity(attackerId);
        if ((target.getTargetType() != Targetable.TYPE_BUILDING)
              && (target.getTargetType() != Targetable.TYPE_FUEL_TANK)) {
            return new ToHitData(TargetRoll.IMPOSSIBLE, "You can only target buildings");
        } else if (ae == null) {
            return new ToHitData(TargetRoll.IMPOSSIBLE, "You can't attack from a null entity!");
        } else if (!(ae instanceof Infantry)) {
            return new ToHitData(TargetRoll.IMPOSSIBLE, "Attacker is not infantry");
        }
        Infantry inf = (Infantry) ae;
        if (inf.turnsLayingExplosives > 0) {
            return new ToHitData(TargetRoll.AUTOMATIC_SUCCESS, "STOP: Expected Damage: " + getDamageFor(ae));
        }
        boolean ok = false;
        for (Mounted<?> m : ae.getMisc()) {
            if (m.getType().hasFlag(MiscType.F_TOOLS)
                  && m.getType().hasFlag(MiscTypeFlag.S_DEMOLITION_CHARGE)) {
                ok = true;
                break;
            }
        }
        if (!ok) {
            return new ToHitData(TargetRoll.IMPOSSIBLE, "No explosives carried");
        }
        return new ToHitData(TargetRoll.AUTOMATIC_SUCCESS,
              "START: Can't move or fire while laying explosives");
    }
}

/*
 * Copyright (C) 2004 Ben Mazur (bmazur@sev.org)
 * Copyright (C) 2007-2025 The MegaMek Team. All Rights Reserved.
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

package megamek.common.weapons.autoCannons;

import static megamek.common.game.IGame.LOGGER;

import java.io.Serial;

import megamek.common.ToHitData;
import megamek.common.actions.WeaponAttackAction;
import megamek.common.alphaStrike.AlphaStrikeElement;
import megamek.common.annotations.Nullable;
import megamek.common.enums.TechBase;
import megamek.common.equipment.AmmoType;
import megamek.common.equipment.Mounted;
import megamek.common.game.Game;
import megamek.common.loaders.EntityLoadingException;
import megamek.common.units.Entity;
import megamek.common.weapons.handlers.AttackHandler;
import megamek.common.weapons.handlers.RACHandler;
import megamek.common.weapons.handlers.UltraWeaponHandler;
import megamek.server.totalwarfare.TWGameManager;

/**
 * @author Andrew Hunter
 * @since Oct 19, 2004
 */
public abstract class RACWeapon extends UACWeapon {
    @Serial
    private static final long serialVersionUID = 659000035767322660L;

    public RACWeapon() {
        super();
        ammoType = AmmoType.AmmoTypeEnum.AC_ROTARY;
        String[] modeStrings = { MODE_AC_SINGLE, MODE_RAC_TWO_SHOT, MODE_RAC_THREE_SHOT,
                                 MODE_RAC_FOUR_SHOT, MODE_RAC_FIVE_SHOT, MODE_RAC_SIX_SHOT };
        setModes(modeStrings);
        // explosive when jammed
        explosive = true;
        explosionDamage = damage;
        atClass = CLASS_AC;
    }

    /*
     * (non-Javadoc)
     *
     * @see
     * megamek.common.weapons.Weapon#getCorrectHandler(megamek.common.ToHitData,
     * megamek.common.actions.WeaponAttackAction, megamek.common.game.Game,
     * megamek.server.Server)
     */
    @Override
    @Nullable
    public AttackHandler getCorrectHandler(ToHitData toHit, WeaponAttackAction waa, Game game, TWGameManager manager) {
        try {
            Entity entity = game.getEntity(waa.getEntityId());

            if (entity != null) {
                Mounted<?> weapon = entity.getEquipment(waa.getWeaponId());
                if (weapon.curMode().equals(MODE_RAC_SIX_SHOT)
                      || weapon.curMode().equals(MODE_RAC_FIVE_SHOT)
                      || weapon.curMode().equals(MODE_RAC_FOUR_SHOT)
                      || weapon.curMode().equals(MODE_RAC_THREE_SHOT)) {
                    return new RACHandler(toHit, waa, game, manager);
                }
            }

            return new UltraWeaponHandler(toHit, waa, game, manager);
        } catch (EntityLoadingException ignored) {
            LOGGER.warn("Get Correct Handler - Attach Handler Received Null Entity.");
        }
        return null;
    }

    @Override
    public double getBattleForceDamage(int range, Mounted<?> disregard) {
        if (techAdvancement.getTechBase() == TechBase.IS) {
            if (rackSize == 2) {
                return (range <= AlphaStrikeElement.LONG_RANGE) ? 0.8 : 0;
            } else {
                return (range <= AlphaStrikeElement.MEDIUM_RANGE) ? 2 : 0;
            }
        } else {
            if (rackSize == 2) {
                return 0.8;
            } else {
                return (range <= AlphaStrikeElement.LONG_RANGE) ? 2 : 0;
            }
        }
    }
}

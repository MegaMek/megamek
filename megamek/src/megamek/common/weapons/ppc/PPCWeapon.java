/*
  Copyright (C) 2004, 2005 Ben Mazur (bmazur@sev.org)
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

package megamek.common.weapons.ppc;

import static megamek.common.game.IGame.LOGGER;

import megamek.common.ToHitData;
import megamek.common.actions.WeaponAttackAction;
import megamek.common.alphaStrike.AlphaStrikeElement;
import megamek.common.annotations.Nullable;
import megamek.common.equipment.MiscType;
import megamek.common.equipment.Mounted;
import megamek.common.game.Game;
import megamek.common.loaders.EntityLoadingException;
import megamek.common.options.IGameOptions;
import megamek.common.options.OptionsConstants;
import megamek.common.weapons.handlers.AttackHandler;
import megamek.common.weapons.handlers.PPCHandler;
import megamek.common.weapons.lasers.EnergyWeapon;
import megamek.server.totalwarfare.TWGameManager;

/**
 * @author Andrew Hunter
 * @since Sep 13, 2004
 */
public abstract class PPCWeapon extends EnergyWeapon {
    private static final long serialVersionUID = -8128018700095507410L;

    public PPCWeapon() {
        super();
        flags = flags.or(F_DIRECT_FIRE).or(F_PPC);
        atClass = CLASS_PPC;
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
    public AttackHandler getCorrectHandler(ToHitData toHit, WeaponAttackAction waa, Game game,
          TWGameManager manager) {
        try {
            return new PPCHandler(toHit, waa, game, manager);
        } catch (EntityLoadingException ignored) {
            LOGGER.warn("Get Correct Handler - Attach Handler Received Null Entity.");
        }
        return null;
    }

    @Override
    public double getBattleForceDamage(int range, Mounted<?> capacitor) {
        double damage = 0;
        if (range <= getLongRange()) {
            // Variable damage weapons that cannot reach into the BF long range band use LR
            // damage for the MR band
            if ((getDamage() == DAMAGE_VARIABLE)
                  && (range == AlphaStrikeElement.MEDIUM_RANGE)
                  && (getLongRange() < AlphaStrikeElement.LONG_RANGE)) {
                damage = getDamage(AlphaStrikeElement.LONG_RANGE);
            } else {
                damage = getDamage(range);
            }
            if ((capacitor != null) && (capacitor.getType() instanceof MiscType)
                  && capacitor.getType().hasFlag(MiscType.F_PPC_CAPACITOR)) {
                damage = (damage + 5) / 2;
            }
            if ((range == AlphaStrikeElement.SHORT_RANGE) && (getMinimumRange() > 0)) {
                damage = adjustBattleForceDamageForMinRange(damage);
            }
            if (getToHitModifier(null) != 0) {
                damage -= damage * getToHitModifier(null) * 0.05;
            }
        }
        return damage / 10.0;
    }

    @Override
    public void adaptToGameOptions(IGameOptions gameOptions) {
        super.adaptToGameOptions(gameOptions);

        // Modes for disengaging PPC field inhibitors according to TacOps, p.103.
        // The benefit is removing the minimum range, so only PPCs with a minimum range
        // get the modes.
        if (minimumRange > 0) {
            if (gameOptions.booleanOption(OptionsConstants.ADVANCED_COMBAT_TAC_OPS_PPC_INHIBITORS)) {
                addMode("Field Inhibitor ON");
                addMode("Field Inhibitor OFF");
            } else {
                removeMode("Field Inhibitor ON");
                removeMode("Field Inhibitor OFF");
            }
        }
    }
}

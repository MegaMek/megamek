/*
 * Copyright (c) 2005 - Ben Mazur (bmazur@sev.org)
 * Copyright (C) 2008-2025 The MegaMek Team. All Rights Reserved.
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

package megamek.common.weapons.handlers;

import java.io.Serial;

import megamek.common.RangeType;
import megamek.common.ToHitData;
import megamek.common.actions.WeaponAttackAction;
import megamek.common.compute.Compute;
import megamek.common.game.Game;
import megamek.common.loaders.EntityLoadingException;
import megamek.common.options.OptionsConstants;
import megamek.common.units.Infantry;
import megamek.server.totalWarfare.TWGameManager;

public class VariableSpeedPulseLaserWeaponHandler extends EnergyWeaponHandler {
    @Serial
    private static final long serialVersionUID = -5701939682138221449L;

    public VariableSpeedPulseLaserWeaponHandler(ToHitData toHit, WeaponAttackAction waa, Game g,
          TWGameManager m) throws EntityLoadingException {
        super(toHit, waa, g, m);
    }

    /*
     * (non-Javadoc)
     *
     * @see megamek.common.weapons.handlers.WeaponHandler#calcDamagePerHit()
     */
    @Override
    protected int calcDamagePerHit() {
        int[] nRanges = weaponType.getRanges(weapon);
        double toReturn = weaponType.getDamage(nRange);

        if (game.getOptions().booleanOption(OptionsConstants.ADVANCED_COMBAT_TAC_OPS_ENERGY_WEAPONS)
              && weapon.hasModes()) {
            toReturn = Compute.dialDownDamage(weapon, weaponType, nRange);
        }

        // Check for Altered Damage from Energy Weapons (TacOp, pg.83)
        if (game.getOptions().booleanOption(OptionsConstants.ADVANCED_COMBAT_TAC_OPS_ALTERNATIVE_DAMAGE)) {
            if (nRange <= 1) {
                toReturn++;
            } else if (nRange > weaponType.getMediumRange() && nRange <= weaponType.getLongRange()) {
                toReturn--;
            }
        }

        if (target.isConventionalInfantry()) {
            toReturn = Compute.directBlowInfantryDamage(toReturn,
                  bDirect ? toHit.getMoS() / 3 : 0,
                  weaponType.getInfantryDamageClass(),
                  ((Infantry) target).isMechanized(),
                  toHit.getThruBldg() != null, attackingEntity.getId(), calcDmgPerHitReport);
            if (nRange <= nRanges[RangeType.RANGE_SHORT]) {
                toReturn += 3;
            } else if (nRange <= nRanges[RangeType.RANGE_MEDIUM]) {
                toReturn += 2;
            } else {
                toReturn++;
            }
        } else if (bDirect) {
            toReturn = Math.min(toReturn + (toHit.getMoS() / 3.0), toReturn * 2);
        }

        if (game.getOptions().booleanOption(OptionsConstants.ADVANCED_COMBAT_TAC_OPS_RANGE)
              && (nRange > nRanges[RangeType.RANGE_LONG])) {
            // Against conventional infantry, treat as direct fire energy
            if (target.isConventionalInfantry()) {
                toReturn -= 1;
            } else { // Else, treat as pulse weapon
                toReturn = (int) Math.floor(toReturn / 2.0);
            }
        }
        if (game.getOptions().booleanOption(OptionsConstants.ADVANCED_COMBAT_TAC_OPS_LOS_RANGE)
              && (nRange > nRanges[RangeType.RANGE_EXTREME])) {
            // Against conventional infantry, treat as direct fire energy
            if (target.isConventionalInfantry()) {
                toReturn = (int) Math.floor(toReturn / 2.0);
            } else { // Else, treat as pulse weapon
                toReturn = (int) Math.floor(toReturn / 3.0);
            }

        }

        toReturn = applyGlancingBlowModifier(toReturn, target.isConventionalInfantry());
        return (int) Math.ceil(toReturn);
    }
}

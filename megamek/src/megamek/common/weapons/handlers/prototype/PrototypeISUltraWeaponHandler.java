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

package megamek.common.weapons.handlers.prototype;

import java.io.Serial;
import java.util.Vector;

import megamek.common.Report;
import megamek.common.ToHitData;
import megamek.common.actions.WeaponAttackAction;
import megamek.common.equipment.AmmoType;
import megamek.common.game.Game;
import megamek.common.loaders.EntityLoadingException;
import megamek.common.weapons.handlers.UltraWeaponHandler;
import megamek.server.totalwarfare.TWGameManager;

/**
 * @author Andrew Hunter
 * @since Sept 29, 2004
 */
public class PrototypeISUltraWeaponHandler extends UltraWeaponHandler {
    @Serial
    private static final long serialVersionUID = 6441106275439235564L;

    /**
     *
     */
    public PrototypeISUltraWeaponHandler(ToHitData t, WeaponAttackAction w, Game g, TWGameManager m)
          throws EntityLoadingException {
        super(t, w, g, m);
    }

    /*
     * (non-Javadoc)
     *
     * @see megamek.common.weapons.handlers.UltraWeaponHandler#doChecks(java.util.Vector)
     */
    @Override
    protected boolean doChecks(Vector<Report> vPhaseReport) {
        if (doAmmoFeedProblemCheck(vPhaseReport)) {
            return true;
        } else if (attackingEntity.isConventionalInfantry()) {
            return false;
        }

        if (((roll.getIntValue() <= 4) && (howManyShots == 2))
              || ((roll.getIntValue() == 2) && (howManyShots == 1))) {
            Report r = new Report();
            r.subject = subjectId;
            weapon.setJammed(true);
            if (weaponType.getAmmoType() == AmmoType.AmmoTypeEnum.AC_ULTRA) {
                r.messageId = 3160;
                weapon.setHit(true);
            } else {
                r.messageId = 3170;
            }
            vPhaseReport.addElement(r);
            return true;
        }
        return false;
    }
}

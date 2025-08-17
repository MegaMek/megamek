/*
 * Copyright (C) 2005 Ben Mazur (bmazur@sev.org)
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

package megamek.common.weapons.handlers.ac;

import java.util.Vector;

import megamek.common.equipment.AmmoType;
import megamek.common.game.Game;
import megamek.common.Report;
import megamek.common.ToHitData;
import megamek.common.equipment.WeaponType;
import megamek.common.actions.WeaponAttackAction;
import megamek.common.equipment.WeaponMounted;
import megamek.common.weapons.handlers.AmmoBayWeaponHandler;
import megamek.server.totalwarfare.TWGameManager;

/**
 * @author Jay Lawson
 */
public class ACBayHandler extends AmmoBayWeaponHandler {

    private static final long serialVersionUID = -1618484541772117621L;

    /**
     * @param t
     * @param w
     * @param g
     * @param m
     */
    public ACBayHandler(ToHitData t, WeaponAttackAction w, Game g, TWGameManager m) {
        super(t, w, g, m);
    }

    /*
     * (non-Javadoc)
     *
     * @see megamek.common.weapons.handlers.UltraWeaponHandler#doChecks(java.util.Vector)
     */
    @Override
    protected boolean doChecks(Vector<Report> vPhaseReport) {
        for (WeaponMounted bayW : weapon.getBayWeapons()) {
            WeaponType bayWType = bayW.getType();
            int ammoUsed = bayW.getCurrentShots();
            if (bayWType.getAmmoType() == AmmoType.AmmoTypeEnum.AC_ROTARY) {
                boolean jams = false;
                switch (ammoUsed) {
                    case 6:
                        if (roll.getIntValue() <= 4) {
                            jams = true;
                        }
                        break;
                    case 5:
                    case 4:
                        if (roll.getIntValue() <= 3) {
                            jams = true;
                        }
                        break;
                    case 3:
                    case 2:
                        if (roll.getIntValue() <= 2) {
                            jams = true;
                        }
                        break;
                    default:
                        break;
                }
                if (jams) {
                    Report r = new Report(3170);
                    r.subject = subjectId;
                    r.add(" shot(s)");
                    r.newlines = 0;
                    vPhaseReport.addElement(r);
                    bayW.setJammed(true);
                }
            } else if (bayWType.getAmmoType() == AmmoType.AmmoTypeEnum.AC_ULTRA) {
                if (roll.getIntValue() == 2 && ammoUsed == 2) {
                    Report r = new Report();
                    r.subject = subjectId;
                    r.messageId = 3160;
                    r.newlines = 0;
                    bayW.setJammed(true);
                    bayW.setHit(true);
                    vPhaseReport.addElement(r);
                }
            }
        }

        return false;
    }
}

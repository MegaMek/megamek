/*
 * Copyright (C) 2005 Ben Mazur (bmazur@sev.org)
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

package megamek.common.weapons.handlers.lrm;

import java.io.Serial;
import java.util.EnumSet;
import java.util.Vector;

import megamek.common.Report;
import megamek.common.ToHitData;
import megamek.common.actions.WeaponAttackAction;
import megamek.common.board.Coords;
import megamek.common.compute.Compute;
import megamek.common.equipment.AmmoType;
import megamek.common.game.Game;
import megamek.common.loaders.EntityLoadingException;
import megamek.common.rolls.TargetRoll;
import megamek.common.units.Entity;
import megamek.common.weapons.handlers.MissileWeaponHandler;
import megamek.server.totalWarfare.TWGameManager;

/**
 * @author Sebastian Brocks
 */
public class LRMScatterableHandler extends MissileWeaponHandler {
    @Serial
    private static final long serialVersionUID = -3661776853552779877L;

    public LRMScatterableHandler(ToHitData t, WeaponAttackAction w, Game g, TWGameManager m)
          throws EntityLoadingException {
        super(t, w, g, m);
    }

    @Override
    protected boolean specialResolution(Vector<Report> vPhaseReport,
          Entity entityTarget) {
        Coords coords = target.getPosition();
        AmmoType ammoType = ammo.getType();
        EnumSet<AmmoType.Munitions> amType = ammoType.getMunitionType();
        boolean mineDelivery = amType.contains(AmmoType.Munitions.M_THUNDER)
              || amType.contains(AmmoType.Munitions.M_THUNDER_ACTIVE)
              || amType.contains(AmmoType.Munitions.M_THUNDER_AUGMENTED)
              || amType.contains(AmmoType.Munitions.M_THUNDER_INFERNO)
              || amType.contains(AmmoType.Munitions.M_THUNDER_VIBRABOMB);
        int whoReport = Report.PUBLIC;
        // only report to player if mine delivery
        if (mineDelivery) {
            whoReport = Report.HIDDEN;
        }
        int density = ammoType.getRackSize();
        if (amType.contains(AmmoType.Munitions.M_THUNDER_AUGMENTED)) {
            density = density / 2 + density % 2;
        }
        if (!bMissed) {
            Report r = new Report(3190, whoReport);
            r.subject = subjectId;
            r.player = attackingEntity.getOwnerId();
            r.add(coords.getBoardNum());
            vPhaseReport.addElement(r);
        } else {
            // Per TacOps errata 3.4, thunder munitions scatter like artillery,
            // i.e. by MoF; for simplicity's sake, we'll for now treat other
            // LRM attacks using this handler the same.
            coords = Compute.scatter(coords, -toHit.getMoS());
            if (mineDelivery) {
                density -= 5;
                // If density drops to 0 or less, we're done here.
                if (density <= 0) {
                    Report r = new Report(3198, whoReport);
                    r.subject = subjectId;
                    r.player = attackingEntity.getOwnerId();
                    vPhaseReport.addElement(r);
                    return true;
                }
            }
            if (game.getBoard().contains(coords)) {
                // misses and scatters to another hex
                int reportNr = mineDelivery ? 3197 : 3195;
                Report r = new Report(reportNr, whoReport);
                r.subject = subjectId;
                r.player = attackingEntity.getOwnerId();
                r.add(coords.getBoardNum());
                vPhaseReport.addElement(r);
            } else {
                // misses and scatters off-board
                Report r = new Report(3200);
                r.subject = subjectId;
                r.player = attackingEntity.getOwnerId();
                vPhaseReport.addElement(r);
                return true;
            }
        }

        // Handle the thunder munitions.
        if (ammoType.getMunitionType().contains(AmmoType.Munitions.M_THUNDER_AUGMENTED)) {
            gameManager.deliverThunderAugMinefield(coords,
                  attackingEntity.getOwner().getId(),
                  density,
                  attackingEntity.getId());
        } else if (ammoType.getMunitionType().contains(AmmoType.Munitions.M_THUNDER)) {
            gameManager.deliverThunderMinefield(coords,
                  attackingEntity.getOwner().getId(),
                  density,
                  attackingEntity.getId());
        } else if (ammoType.getMunitionType().contains(AmmoType.Munitions.M_THUNDER_INFERNO)) {
            gameManager.deliverThunderInfernoMinefield(coords,
                  attackingEntity.getOwner().getId(),
                  density,
                  attackingEntity.getId());
        } else if (ammoType.getMunitionType().contains(AmmoType.Munitions.M_THUNDER_VIBRABOMB)) {
            gameManager.deliverThunderVibraMinefield(coords,
                  attackingEntity.getOwner().getId(),
                  density,
                  weaponAttackAction.getOtherAttackInfo(),
                  attackingEntity.getId());
        } else if (ammoType.getMunitionType().contains(AmmoType.Munitions.M_THUNDER_ACTIVE)) {
            gameManager.deliverThunderActiveMinefield(coords,
                  attackingEntity.getOwner().getId(),
                  density,
                  attackingEntity.getId());
        }

        // Per TO:AUE pg 181, incendiary LRM mixed with THUNDER also attempts to set fires
        if (ammoType.getMunitionType().contains(AmmoType.Munitions.M_INCENDIARY_LRM)) {
            Report r = new Report(3329);
            r.subject = subjectId;
            vPhaseReport.addElement(r);
            TargetRoll targetRoll = new TargetRoll(-4, "Incendiary LRM");
            gameManager.tryIgniteHex(coords, target.getBoardId(), subjectId, false, false,
                  targetRoll, true, -1, vPhaseReport);
        }

        return true;
    }
}

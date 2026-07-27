package megamek.common.weapons.handlers.mrm;

import megamek.common.Report;
import megamek.common.ToHitData;
import megamek.common.actions.WeaponAttackAction;
import megamek.common.battleArmor.BattleArmor;
import megamek.common.board.Coords;
import megamek.common.compute.Compute;
import megamek.common.compute.ComputeECM;
import megamek.common.enums.GamePhase;
import megamek.common.equipment.AmmoType;
import megamek.common.equipment.MiscType;
import megamek.common.equipment.Mounted;
import megamek.common.equipment.WeaponType;
import megamek.common.game.Game;
import megamek.common.loaders.EntityLoadingException;
import megamek.common.options.OptionsConstants;
import megamek.common.rolls.Roll;
import megamek.common.rolls.TargetRoll;
import megamek.common.units.Entity;
import megamek.common.units.IBuilding;
import megamek.common.units.Infantry;
import megamek.common.units.Mek;
import megamek.common.units.Tank;
import megamek.common.units.Targetable;
import megamek.common.weapons.Weapon;
import megamek.common.weapons.handlers.AttackHandler;
import megamek.common.weapons.handlers.WeaponHandler;
import megamek.server.totalWarfare.TWGameManager;

import java.util.EnumSet;
import java.util.Enumeration;
import java.util.Vector;

import static megamek.common.equipment.AmmoType.INCENDIARY_MOD;

/*
 * Copyright (C) 2026 The MegaMek Team. All Rights Reserved.
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
public class MRMSaturationHandler extends MRMHandler {
    public MRMSaturationHandler(ToHitData t, WeaponAttackAction w, Game g, TWGameManager m) throws EntityLoadingException {
        super(t, w, g, m);
    }

    @Override
    protected boolean specialResolution(Vector<Report> vPhaseReport,
          Entity entityTarget) {

        Coords coords = target.getPosition();
        AmmoType ammoType = ammo.getType();
                
        int size = ammoType.getRackSize();
        
        if (!bMissed) {
            Report r = new Report(3190);
            r.subject = subjectId;
            r.player = attackingEntity.getOwnerId();
            r.add(coords.getBoardNum());
            vPhaseReport.addElement(r);
        } else {
            coords = Compute.scatter(coords, -toHit.getMoS());
            if (game.getBoard().contains(coords)) {
                // misses and scatters to another hex
                Report r = new Report(3195);
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

        gameManager.deliverSaturationMRM(coords,
              attackingEntity.getOwner().getId(),
              size,
              attackingEntity.getId());
        return true;
    }
        
    @Override
    protected int calcHits(Vector<Report> vPhaseReport) {
        
        /*
        * This is very experimental. It may not be needed, and instead go into the special handler.
         */
        
        int damage = weaponType.getRackSize();
        // Saturation mode reduces by AMS first.
        // add AMS mods
        int nMissilesModifier = getClusterModifiers(true);
        nMissilesModifier += getAMSHitsMod(vPhaseReport);
        int missilesHit;
        
        if (allShotsHit()) {
            // We want buildings and large craft to be able to affect this number with AMS
            // treat as a Streak launcher (cluster roll 11) to make this happen
            missilesHit = Compute.missilesHit(weaponType.getRackSize(), nMissilesModifier,
                  weapon.isHotLoaded(), true, isAdvancedAMS());
        } else {
            missilesHit = Compute.missilesHit(weaponType.getRackSize(), nMissilesModifier,
                      weapon.isHotLoaded(), false, isAdvancedAMS());
        }
        
        // Core rules p.197 does not list a minumum damage amount. Only 1 missile hitting will be negated
        damage = (int) Math.round(missilesHit / 3);
        
        return missilesHit;
    }
}

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

package megamek.common.weapons.handlers;

import java.io.Serial;
import java.util.Vector;
import javax.swing.JOptionPane;

import megamek.common.Report;
import megamek.common.ToHitData;
import megamek.common.actions.WeaponAttackAction;
import megamek.common.battleArmor.BattleArmor;
import megamek.common.compute.Compute;
import megamek.common.compute.ComputeECM;
import megamek.common.equipment.AmmoMounted;
import megamek.common.equipment.WeaponType;
import megamek.common.game.Game;
import megamek.common.loaders.EntityLoadingException;
import megamek.common.options.OptionsConstants;
import megamek.common.rolls.TargetRoll;
import megamek.common.units.Entity;
import megamek.common.units.IBuilding;
import megamek.common.units.Infantry;
import megamek.common.units.Targetable;
import megamek.logging.MMLogger;
import megamek.server.totalWarfare.TWGameManager;

/**
 * @author Sebastian Brocks
 */
public class StreakHandler extends MissileWeaponHandler {
    private static final MMLogger logger = MMLogger.create(StreakHandler.class);

    @Serial
    private static final long serialVersionUID = 4122111574368642492L;
    boolean isAngelECMAffected = ComputeECM.isAffectedByAngelECM(attackingEntity,
          attackingEntity.getPosition(),
          target.getPosition());

    /**
     *
     */
    public StreakHandler(ToHitData t, WeaponAttackAction w, Game g, TWGameManager m) throws EntityLoadingException {
        super(t, w, g, m);
    }

    @Override
    protected int calcDamagePerHit() {
        if (target.isConventionalInfantry()) {
            return Compute.directBlowInfantryDamage(
                  weaponType.getRackSize() * 2, bDirect ? toHit.getMoS() / 3 : 0,
                  weaponType.getInfantryDamageClass(),
                  ((Infantry) target).isMechanized(),
                  toHit.getThruBldg() != null, attackingEntity.getId(), calcDmgPerHitReport);
        }
        return 2;
    }

    @Override
    protected int calculateNumCluster() {
        return 1;
    }

    @Override
    protected int calcHits(Vector<Report> vPhaseReport) {
        // conventional infantry gets hit in one lump
        // BAs do one lump of damage per BA suit
        if (target.isConventionalInfantry()) {
            if (attackingEntity instanceof BattleArmor) {
                bSalvo = true;
                return ((BattleArmor) attackingEntity).getShootingStrength();
            }
            return 1;
        }
        // no AMS when streak misses
        if (bMissed) {
            return 0;
        }
        int nMissilesModifier = getClusterModifiers(true);

        int missilesHit;
        int amsMod = getAMSHitsMod(vPhaseReport);

        if (game.getOptions().booleanOption(OptionsConstants.ADVANCED_AERO_RULES_AERO_SANITY)) {
            Entity entityTarget = (target.getTargetType() == Targetable.TYPE_ENTITY) ? (Entity) target
                  : null;
            if (entityTarget != null && entityTarget.isLargeCraft()) {
                amsMod = (int) -getAeroSanityAMSHitsMod();
            }
        }

        if (amsMod == 0 && allShotsHit()) {
            missilesHit = weaponType.getRackSize();
        } else {
            missilesHit = Compute.missilesHit(weaponType.getRackSize(), amsMod + nMissilesModifier,
                  weapon.isHotLoaded(), allShotsHit(), isAdvancedAMS());
            if (amsMod != 0) {
                Report r;
                if (amsMod > 0) {
                    r = new Report(3340);
                } else {
                    r = new Report(3341);
                }
                r.subject = subjectId;
                r.add(amsMod);
                r.newlines = 0;
                vPhaseReport.addElement(r);
            }
        }

        if (missilesHit > 0) {
            Report r = new Report(3325);
            r.subject = subjectId;
            r.add(missilesHit);
            r.add(sSalvoType);
            r.add(toHit.getTableDesc());
            r.newlines = 0;
            vPhaseReport.addElement(r);
        }
        Report r = new Report(3345);
        r.subject = subjectId;
        vPhaseReport.addElement(r);
        bSalvo = true;
        return missilesHit;
    }

    @Override
    protected void useAmmo() {
        checkAmmo();
        if (ammo == null) {
            final String message = "Handler can't find any ammo! This should be impossible!";
            logger.error(message, new Exception());
            JOptionPane.showMessageDialog(null, message, "Unknown Ammo Exception",
                  JOptionPane.ERROR_MESSAGE);
        }

        if (ammo.getUsableShotsLeft() <= 0) {
            attackingEntity.loadWeaponWithSameAmmo(weapon);
            ammo = (AmmoMounted) weapon.getLinked();
        }

        if (roll.getIntValue() >= toHit.getValue()) {
            ammo.setShotsLeft(ammo.getBaseShotsLeft() - 1);
            if (weaponType.hasFlag(WeaponType.F_ONE_SHOT)) {
                weapon.setFired(true);
            }
        }

        // Always mark weapon as used even if lock-on fails. Per official ruling,
        // attempting to fire a Streak counts as "firing" for physical attack restrictions.
        // See: https://battletech.com/forums/index.php?topic=34443.msg803644#msg803644
        setDone();
    }

    @Override
    protected void reportMiss(Vector<Report> vPhaseReport) {
        Report r = new Report(3215);
        r.subject = subjectId;
        vPhaseReport.addElement(r);
    }

    @Override
    protected void addHeat() {
        if ((toHit.getValue() != TargetRoll.IMPOSSIBLE) && (roll.getIntValue() >= toHit.getValue())) {
            super.addHeat();
        }
    }

    @Override
    protected boolean allShotsHit() {
        return super.allShotsHit() || !isAngelECMAffected;
    }

    @Override
    protected boolean handleSpecialMiss(Entity entityTarget, boolean bldgDamagedOnMiss,
          IBuilding bldg, Vector<Report> vPhaseReport) {
        return false;
    }
}

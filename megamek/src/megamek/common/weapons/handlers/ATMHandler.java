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
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.Vector;

import megamek.common.RangeType;
import megamek.common.Report;
import megamek.common.ToHitData;
import megamek.common.actions.WeaponAttackAction;
import megamek.common.battleArmor.BattleArmor;
import megamek.common.board.Coords;
import megamek.common.compute.Compute;
import megamek.common.compute.ComputeECM;
import megamek.common.equipment.AmmoType;
import megamek.common.equipment.Minefield;
import megamek.common.equipment.MiscType;
import megamek.common.equipment.Mounted;
import megamek.common.equipment.WeaponType;
import megamek.common.game.Game;
import megamek.common.loaders.EntityLoadingException;
import megamek.common.options.OptionsConstants;
import megamek.common.units.Entity;
import megamek.common.units.Infantry;
import megamek.common.units.Mek;
import megamek.common.units.Tank;
import megamek.common.units.Targetable;
import megamek.server.totalWarfare.TWGameManager;

/**
 * @author Sebastian Brocks
 */
public class ATMHandler extends MissileWeaponHandler {
    @Serial
    private static final long serialVersionUID = -2536312899803153911L;

    public ATMHandler(ToHitData t, WeaponAttackAction w, Game g, TWGameManager m) throws EntityLoadingException {
        super(t, w, g, m);
    }

    @Override
    protected int calcDamagePerHit() {
        double toReturn;
        AmmoType ammoType = ammo.getType();
        if (ammoType.getMunitionType().contains(AmmoType.Munitions.M_HIGH_EXPLOSIVE)) {
            sSalvoType = " high-explosive missile(s) ";
            toReturn = 3;
        } else if (ammoType.getMunitionType().contains(AmmoType.Munitions.M_EXTENDED_RANGE)) {
            sSalvoType = " extended-range missile(s) ";
            toReturn = 1;
        } else {
            toReturn = 2;
        }
        if (target.isConventionalInfantry()) {
            toReturn = Compute.directBlowInfantryDamage(
                  weaponType.getRackSize(), bDirect ? toHit.getMoS() / 3 : 0,
                  weaponType.getInfantryDamageClass(),
                  ((Infantry) target).isMechanized(),
                  toHit.getThruBldg() != null, weaponEntity.getId(), calcDmgPerHitReport);
            toReturn = applyGlancingBlowModifier(toReturn, true);
        }

        return (int) toReturn;
    }

    @Override
    protected int calcHits(Vector<Report> vPhaseReport) {
        // conventional infantry gets hit in one lump
        // don't need to check for BAs, because BA can't mount ATMs
        if (target.isConventionalInfantry()) {
            return 1;
        }
        int hits;
        AmmoType ammoType = ammo.getType();
        // TacOPs p.84 Cluster Hit Penalties will only affect ATM HE
        if (ammoType.getMunitionType().contains(AmmoType.Munitions.M_HIGH_EXPLOSIVE)) {
            hits = super.calcHits(vPhaseReport);
        } else {
            hits = calcStandardAndExtendedAmmoHits(vPhaseReport);
        }
        // change to 5 damage clusters here, after AMS has been done
        hits = nDamPerHit * hits;
        nDamPerHit = 1;
        return hits;
    }

    // PLAYTEST3 ATMs now cluster in 6s
    @Override
    protected int calculateNumCluster() {
        if (game.getOptions().booleanOption(OptionsConstants.PLAYTEST_3)) {
            return 6;
        } else {
            return 5;
        }
    }

    /**
     * Calculate the attack value based on range
     *
     * @return an <code>int</code> representing the attack value at that range.
     */
    @Override
    protected int calcAttackValue() {
        int av = 0;
        int counterAV;
        int range = RangeType.rangeBracket(nRange, weaponType.getATRanges(), true, false);
        AmmoType ammoType = ammo.getType();
        if (ammoType.getMunitionType().contains(AmmoType.Munitions.M_HIGH_EXPLOSIVE)) {
            if (range == WeaponType.RANGE_SHORT) {
                av = weaponType.getRoundShortAV();
                av = av + (int) Math.ceil(av / 2.0);
            }
        } else if (ammoType.getMunitionType().contains(AmmoType.Munitions.M_EXTENDED_RANGE)) {
            av = (int) Math.ceil(weaponType.getRoundMedAV() / 2.0);
        } else {
            if (range == WeaponType.RANGE_SHORT) {
                av = weaponType.getRoundShortAV();
            } else if (range == WeaponType.RANGE_MED) {
                av = weaponType.getRoundMedAV();
            } else if (range == WeaponType.RANGE_LONG) {
                av = weaponType.getRoundLongAV();
            } else if (range == WeaponType.RANGE_EXT) {
                av = weaponType.getRoundExtAV();
            }
        }

        // Point Defenses engage the missiles still aimed at us
        counterAV = calcCounterAV();
        av = av - counterAV;

        if (bDirect) {
            av = Math.min(av + (toHit.getMoS() / 3), av * 2);
        }
        av = applyGlancingBlowModifier(av, false);
        av = (int) Math.floor(getBracketingMultiplier() * av);
        return av;
    }

    protected int calcStandardAndExtendedAmmoHits(Vector<Report> vPhaseReport) {
        // conventional infantry gets hit in one lump
        // BAs do one lump of damage per BA suit
        if (target.isConventionalInfantry()) {
            if (attackingEntity instanceof BattleArmor) {
                bSalvo = true;
                Report r = new Report(3325);
                r.subject = subjectId;
                r.add(weaponType.getRackSize()
                      * ((BattleArmor) attackingEntity).getShootingStrength());
                r.add(sSalvoType);
                r.add(toHit.getTableDesc());
                vPhaseReport.add(r);
                return ((BattleArmor) attackingEntity).getShootingStrength();
            }
            Report r = new Report(3325);
            r.subject = subjectId;
            r.add(weaponType.getRackSize());
            r.add(sSalvoType);
            r.add(toHit.getTableDesc());
            vPhaseReport.add(r);
            return 1;
        }
        Entity entityTarget = (target.getTargetType() == Targetable.TYPE_ENTITY) ? (Entity) target
              : null;
        int missilesHit;

        boolean bMekTankStealthActive = false;
        if ((attackingEntity instanceof Mek) || (attackingEntity instanceof Tank)) {
            bMekTankStealthActive = attackingEntity.isStealthActive();
        }
        Mounted<?> mLinker = weapon.getLinkedBy();
        AmmoType ammoType = ammo.getType();

        int nMissilesModifier = getClusterModifiers(
              ammoType.getMunitionType().contains(AmmoType.Munitions.M_HIGH_EXPLOSIVE));

        // is any hex in the flight path of the missile ECM affected?
        boolean bECMAffected = ComputeECM.isAffectedByECM(attackingEntity,
              attackingEntity.getPosition(),
              target.getPosition());
        // if the attacker is affected by ECM or the target is protected by ECM
        // then act as if affected.

        if (((mLinker != null) && (mLinker.getType() instanceof MiscType)
              && !mLinker.isDestroyed() && !mLinker.isMissing()
              && !mLinker.isBreached() && mLinker.getType().hasFlag(
              MiscType.F_ARTEMIS))
              && (ammoType.getMunitionType().contains(AmmoType.Munitions.M_ARTEMIS_CAPABLE))) {
            if (bECMAffected) {
                // ECM prevents bonus
                Report r = new Report(3330);
                r.subject = subjectId;
                r.newlines = 0;
                vPhaseReport.addElement(r);
            } else if (bMekTankStealthActive) {
                // stealth prevents bonus
                Report r = new Report(3335);
                r.subject = subjectId;
                r.newlines = 0;
                vPhaseReport.addElement(r);
            } else {
                nMissilesModifier += 2;
            }
        } else if (ammoType.getAmmoType() == AmmoType.AmmoTypeEnum.ATM) {
            if (bECMAffected) {
                // ECM prevents bonus
                Report r = new Report(3330);
                r.subject = subjectId;
                r.newlines = 0;
                vPhaseReport.addElement(r);
            } else if (bMekTankStealthActive) {
                // stealth prevents bonus
                Report r = new Report(3335);
                r.subject = subjectId;
                r.newlines = 0;
                vPhaseReport.addElement(r);
            } else {
                nMissilesModifier += 2;
            }
        } else if ((entityTarget != null)
              && (entityTarget.isNarcedBy(attackingEntity.getOwner().getTeam()) || entityTarget
              .isINarcedBy(attackingEntity.getOwner().getTeam()))) {
            // only apply Narc bonus if we're not suffering ECM effect,
            // and we are using narc ammo, and we're not firing indirectly.
            // narc capable missiles are only affected if the narc pod, which
            // sits on the target, is ECM affected
            boolean bTargetECMAffected = ComputeECM.isAffectedByECM(attackingEntity,
                  target.getPosition(), target.getPosition());
            if (((ammoType.getAmmoType() == AmmoType.AmmoTypeEnum.LRM) || (ammoType
                  .getAmmoType() == AmmoType.AmmoTypeEnum.SRM))
                  || ((ammoType.getAmmoType() == AmmoType.AmmoTypeEnum.MML)
                  && (ammoType.getMunitionType().contains(AmmoType.Munitions.M_NARC_CAPABLE)) && ((weapon
                  .curMode() == null) || !weapon.curMode().equals(
                  "Indirect")))) {
                if (bTargetECMAffected) {
                    // ECM prevents bonus
                    Report r = new Report(3330);
                    r.subject = subjectId;
                    r.newlines = 0;
                    vPhaseReport.addElement(r);
                } else {
                    nMissilesModifier += 2;
                }
            }
        }

        // add AMS mods
        nMissilesModifier += getAMSHitsMod(vPhaseReport);

        if (game.getOptions().booleanOption(OptionsConstants.ADVANCED_AERO_RULES_AERO_SANITY)
              && entityTarget != null && entityTarget.isLargeCraft()) {
            nMissilesModifier -= (int) Math.floor(getAeroSanityAMSHitsMod());
        }

        if (allShotsHit()) {
            // We want buildings and large craft to be able to affect this number with AMS
            // treat as a Streak launcher (cluster roll 11) to make this happen
            missilesHit = Compute.missilesHit(weaponType.getRackSize(),
                  nMissilesModifier, weapon.isHotLoaded(), true,
                  isAdvancedAMS());
        } else {
            if (attackingEntity instanceof BattleArmor) {
                missilesHit = Compute.missilesHit(weaponType.getRackSize()
                            * ((BattleArmor) attackingEntity).getShootingStrength(),
                      nMissilesModifier, weapon.isHotLoaded(), false,
                      isAdvancedAMS());
            } else {
                missilesHit = Compute.missilesHit(weaponType.getRackSize(),
                      nMissilesModifier, weapon.isHotLoaded(), false,
                      isAdvancedAMS());
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
            if (nMissilesModifier != 0) {
                if (nMissilesModifier > 0) {
                    r = new Report(3340);
                } else {
                    r = new Report(3341);
                }
                r.subject = subjectId;
                r.add(nMissilesModifier);
                r.newlines = 0;
                vPhaseReport.addElement(r);
            }
        }
        Report r = new Report(3345);
        r.subject = subjectId;
        vPhaseReport.addElement(r);
        bSalvo = true;
        return missilesHit;
    }

    @Override
    protected boolean specialResolution(Vector<Report> vPhaseReport, Entity entityTarget) {
        if (!bMissed
              && (target.getTargetType() == Targetable.TYPE_MINEFIELD_CLEAR)) {
            Report r = new Report(3255);
            r.indent(1);
            r.subject = subjectId;
            vPhaseReport.addElement(r);
            Coords coords = target.getPosition();

            Enumeration<Minefield> minefields = game.getMinefields(coords).elements();
            ArrayList<Minefield> mfRemoved = new ArrayList<>();
            while (minefields.hasMoreElements()) {
                Minefield mf = minefields.nextElement();
                if (gameManager.clearMinefield(mf, attackingEntity,
                      Minefield.CLEAR_NUMBER_WEAPON, vPhaseReport)) {
                    mfRemoved.add(mf);
                }
            }
            // we have to do it this way to avoid a concurrent error problem
            for (Minefield mf : mfRemoved) {
                gameManager.removeMinefield(mf);
            }
            return true;
        }
        return false;
    }

}

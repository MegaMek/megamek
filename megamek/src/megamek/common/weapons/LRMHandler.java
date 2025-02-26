/*
 * MegaMek - Copyright (C) 2007 Ben Mazur (bmazur@sev.org)
 * Copyright (c) 2024 - The MegaMek Team. All Rights Reserved.
 *
 * This file is part of MegaMek.
 *
 * MegaMek is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * MegaMek is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with MegaMek. If not, see <http://www.gnu.org/licenses/>.
 */
package megamek.common.weapons;

import java.io.Serial;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.Vector;

import megamek.common.AmmoType;
import megamek.common.BattleArmor;
import megamek.common.Compute;
import megamek.common.ComputeECM;
import megamek.common.Coords;
import megamek.common.Entity;
import megamek.common.Game;
import megamek.common.Mek;
import megamek.common.Minefield;
import megamek.common.MiscType;
import megamek.common.Mounted;
import megamek.common.Report;
import megamek.common.Tank;
import megamek.common.Targetable;
import megamek.common.ToHitData;
import megamek.common.actions.WeaponAttackAction;
import megamek.common.options.OptionsConstants;
import megamek.common.weapons.lrms.ExtendedLRMWeapon;
import megamek.server.totalwarfare.TWGameManager;

/**
 * @author Sebastian Brocks
 */
public class LRMHandler extends MissileWeaponHandler {
    @Serial
    private static final long serialVersionUID = -9160255801810263821L;

    public LRMHandler(ToHitData t, WeaponAttackAction w, Game g, TWGameManager m) {
        this(t, w, g, m, 0);
    }

    public LRMHandler(ToHitData t, WeaponAttackAction w, Game g, TWGameManager m, int salvoMod) {
        super(t, w, g, m);
        nSalvoBonus = salvoMod;
    }

    @Override
    protected boolean specialResolution(Vector<Report> vPhaseReport,
            Entity entityTarget) {
        if (!bMissed
                && (target.getTargetType() == Targetable.TYPE_MINEFIELD_CLEAR)) {
            // minefield clearance attempt
            Report r = new Report(3255);
            r.indent(1);
            r.subject = subjectId;
            vPhaseReport.addElement(r);
            Coords coords = target.getPosition();

            Enumeration<Minefield> minefields = game.getMinefields(coords)
                    .elements();
            ArrayList<Minefield> mfRemoved = new ArrayList<>();
            while (minefields.hasMoreElements()) {
                Minefield mf = minefields.nextElement();
                if (gameManager.clearMinefield(mf, ae,
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

    @Override
    protected int calcHits(Vector<Report> vPhaseReport) {
        // conventional infantry gets hit in one lump
        // BAs do one lump of damage per BA suit
        if (target.isConventionalInfantry()) {
            if (ae instanceof BattleArmor) {
                bSalvo = true;
                Report r = new Report(3325);
                r.subject = subjectId;
                r.add(wtype.getRackSize()
                        * ((BattleArmor) ae).getShootingStrength());
                r.add(sSalvoType);
                r.add(toHit.getTableDesc());
                vPhaseReport.add(r);
                return ((BattleArmor) ae).getShootingStrength();
            }
            Report r = new Report(3326);
            r.newlines = 0;
            r.subject = subjectId;
            r.add(wtype.getRackSize());
            r.add(sSalvoType);
            vPhaseReport.add(r);
            return 1;
        }
        Entity entityTarget = (target.getTargetType() == Targetable.TYPE_ENTITY) ? (Entity) target
                : null;
        int missilesHit;
        int nMissilesModifier = getClusterModifiers(false);

        boolean bMekTankStealthActive = false;
        if ((ae instanceof Mek) || (ae instanceof Tank)) {
            bMekTankStealthActive = ae.isStealthActive();
        }
        Mounted<?> mLinker = weapon.getLinkedBy();
        AmmoType atype = (AmmoType) ammo.getType();
        // is any hex in the flight path of the missile ECM affected?
        boolean bECMAffected = false;
        // if the attacker is affected by ECM or the target is protected by ECM
        // then act as if affected.
        if (ComputeECM.isAffectedByECM(ae, ae.getPosition(), target.getPosition())) {
            bECMAffected = true;
        }

        if (!weapon.curMode().equals("Indirect")) {
            if (((mLinker != null) && (mLinker.getType() instanceof MiscType)
                && !mLinker.isDestroyed() && !mLinker.isMissing()
                && !mLinker.isBreached() && mLinker.getType().hasFlag(
                MiscType.F_ARTEMIS))
                && (atype.getMunitionType().contains(AmmoType.Munitions.M_ARTEMIS_CAPABLE))) {
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
            } else if (((mLinker != null)
                && (mLinker.getType() instanceof MiscType)
                && !mLinker.isDestroyed() && !mLinker.isMissing()
                && !mLinker.isBreached() && mLinker.getType().hasFlag(
                MiscType.F_ARTEMIS_PROTO))
                && (atype.getMunitionType().contains(AmmoType.Munitions.M_ARTEMIS_CAPABLE))) {
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
                    nMissilesModifier += 1;
                }
            } else if (((mLinker != null)
                && (mLinker.getType() instanceof MiscType)
                && !mLinker.isDestroyed() && !mLinker.isMissing()
                && !mLinker.isBreached() && mLinker.getType().hasFlag(
                MiscType.F_ARTEMIS_V))
                && (atype.getMunitionType().contains(AmmoType.Munitions.M_ARTEMIS_V_CAPABLE))) {
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
                    nMissilesModifier += 3;
                }
            } else if (atype.getAmmoType() == AmmoType.T_ATM) {
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
                && (entityTarget.isNarcedBy(ae.getOwner().getTeam()) || entityTarget
                .isINarcedBy(ae.getOwner().getTeam()))) {
                // only apply Narc bonus if we're not suffering ECM effect
                // and we are using narc ammo, and we're not firing indirectly.
                // narc capable missiles are only affected if the narc pod, which
                // sits on the target, is ECM affected
                boolean bTargetECMAffected = false;
                bTargetECMAffected = ComputeECM.isAffectedByECM(ae,
                    target.getPosition(), target.getPosition());
                if (((atype.getAmmoType() == AmmoType.T_LRM)
                    || (atype.getAmmoType() == AmmoType.T_LRM_IMP)
                    || (atype.getAmmoType() == AmmoType.T_SRM)
                    || (atype.getAmmoType() == AmmoType.T_SRM_IMP)
                    || (atype.getAmmoType() == AmmoType.T_MML)
                    || (atype.getAmmoType() == AmmoType.T_NLRM))
                    && (atype.getMunitionType().contains(AmmoType.Munitions.M_NARC_CAPABLE))) {
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
        }

        // add AMS mods
        nMissilesModifier += getAMSHitsMod(vPhaseReport);

        if (game.getOptions().booleanOption(OptionsConstants.ADVAERORULES_AERO_SANITY)
                && entityTarget != null && entityTarget.isLargeCraft()) {
            nMissilesModifier -= getAeroSanityAMSHitsMod();
        }

        int rackSize = wtype.getRackSize();
        boolean minRangeELRMAttack = false;

        // ELRMs only hit with half their rack size rounded up at minimum range.
        // Ignore this for space combat. 1 hex is 18km across.
        if (wtype instanceof ExtendedLRMWeapon
                && !game.getBoard().inSpace()
                && (nRange <= wtype.getMinimumRange())) {
            rackSize = rackSize / 2 + rackSize % 2;
            minRangeELRMAttack = true;
        }

        if (allShotsHit()) {
            // We want buildings and large craft to be able to affect this number with AMS
            // treat as a Streak launcher (cluster roll 11) to make this happen
            missilesHit = Compute.missilesHit(rackSize,
                    nMissilesModifier, weapon.isHotLoaded(), true,
                    isAdvancedAMS());
        } else {
            if (ae instanceof BattleArmor) {
                missilesHit = Compute.missilesHit(rackSize
                        * ((BattleArmor) ae).getShootingStrength(),
                        nMissilesModifier, weapon.isHotLoaded(), false,
                        isAdvancedAMS());
            } else {
                missilesHit = Compute.missilesHit(rackSize,
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
            if (minRangeELRMAttack) {
                r = new Report(3342);
                r.subject = subjectId;
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
}

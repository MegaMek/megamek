/*
 * Copyright (C) 2007 Ben Mazur (bmazur@sev.org)
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
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.Vector;

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
import megamek.common.game.Game;
import megamek.common.loaders.EntityLoadingException;
import megamek.common.options.OptionsConstants;
import megamek.common.rolls.TargetRoll;
import megamek.common.units.Entity;
import megamek.common.units.IBuilding;
import megamek.common.units.Infantry;
import megamek.common.units.Mek;
import megamek.common.units.Tank;
import megamek.common.units.Targetable;
import megamek.common.weapons.handlers.MissileWeaponHandler;
import megamek.common.weapons.lrms.ExtendedLRMWeapon;
import megamek.server.totalWarfare.TWGameManager;

/**
 * @author Sebastian Brocks
 */
public class LRMHandler extends MissileWeaponHandler {
    @Serial
    private static final long serialVersionUID = -9160255801810263821L;

    public LRMHandler(ToHitData t, WeaponAttackAction w, Game g, TWGameManager m) throws EntityLoadingException {
        this(t, w, g, m, 0);
    }

    public LRMHandler(ToHitData t, WeaponAttackAction w, Game g, TWGameManager m, int salvoMod)
          throws EntityLoadingException {
        super(t, w, g, m);
        nSalvoBonus = salvoMod;
    }

    /**
     * Checks if the ammo is mixed with incendiary rounds. Per TO:AUE pg 181, incendiary rounds can be mixed with LRM
     * ammo at 1 in 5 missiles.
     *
     * @return true if the ammo has incendiary munition type
     */
    protected boolean isIncendiaryMixed() {
        if (ammo == null) {
            return false;
        }
        AmmoType ammoType = ammo.getType();
        if (!ammoType.getMunitionType().contains(AmmoType.Munitions.M_INCENDIARY_LRM)) {
            return false;
        }
        // For MML, incendiary only applies in LRM mode
        if (ammoType.getAmmoType() == AmmoType.AmmoTypeEnum.MML) {
            return ammoType.hasFlag(AmmoType.F_MML_LRM);
        }
        return true;
    }

    /**
     * Returns the number of incendiary missiles in a mixed salvo. Per TO:AUE pg 181, 1 in 5 missiles are incendiary
     * (minimum 1).
     *
     * @return the number of incendiary missiles
     */
    protected int getIncendiaryMissileCount() {
        if (!isIncendiaryMixed()) {
            return 0;
        }
        return (int) Math.ceil(weaponType.getRackSize() / 5.0);
    }

    /**
     * Returns the effective rack size after removing incendiary missiles. Per TO:AUE pg 181, 1 in 5 missiles are
     * incendiary, reducing the damage-dealing missiles.
     *
     * @return the effective rack size for damage calculation
     */
    protected int getEffectiveRackSize() {
        int baseRackSize = weaponType.getRackSize();
        if (!isIncendiaryMixed()) {
            return baseRackSize;
        }
        int incendiaryCount = getIncendiaryMissileCount();
        return Math.max(1, baseRackSize - incendiaryCount);
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

    @Override
    protected int calcHits(Vector<Report> vPhaseReport) {
        // Use effective rack size (reduced by 20% when incendiary mixed)
        int effectiveRackSize = getEffectiveRackSize();

        // conventional infantry gets hit in one lump
        // BAs do one lump of damage per BA suit
        if (target.isConventionalInfantry()) {
            if (attackingEntity instanceof BattleArmor) {
                bSalvo = true;
                Report r = new Report(3325);
                r.subject = subjectId;
                r.add(effectiveRackSize
                      * ((BattleArmor) attackingEntity).getShootingStrength());
                r.add(sSalvoType);
                r.add(toHit.getTableDesc());
                vPhaseReport.add(r);
                addIncendiaryMixedReport(vPhaseReport);
                return ((BattleArmor) attackingEntity).getShootingStrength();
            }
            Report r = new Report(3326);
            r.newlines = 0;
            r.subject = subjectId;
            r.add(effectiveRackSize);
            r.add(sSalvoType);
            vPhaseReport.add(r);
            addIncendiaryMixedReport(vPhaseReport);
            return 1;
        }
        Entity entityTarget = (target.getTargetType() == Targetable.TYPE_ENTITY) ? (Entity) target
              : null;
        int missilesHit;
        int nMissilesModifier = getClusterModifiers(false);

        boolean bMekTankStealthActive = false;
        if ((attackingEntity instanceof Mek) || (attackingEntity instanceof Tank)) {
            bMekTankStealthActive = attackingEntity.isStealthActive();
        }
        Mounted<?> mLinker = weapon.getLinkedBy();
        AmmoType ammoType = ammo.getType();
        // is any hex in the flight path of the missile ECM affected?
        // if the attacker is affected by ECM or the target is protected by ECM
        // then act as if affected.

        boolean bECMAffected = ComputeECM.isAffectedByECM(attackingEntity,
              attackingEntity.getPosition(),
              target.getPosition());

        if (!weapon.curMode().equals("Indirect")) {
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
            } else if (((mLinker != null)
                  && (mLinker.getType() instanceof MiscType)
                  && !mLinker.isDestroyed() && !mLinker.isMissing()
                  && !mLinker.isBreached() && mLinker.getType().hasFlag(
                  MiscType.F_ARTEMIS_PROTO))
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
                    nMissilesModifier += 1;
                }
            } else if (((mLinker != null)
                  && (mLinker.getType() instanceof MiscType)
                  && !mLinker.isDestroyed() && !mLinker.isMissing()
                  && !mLinker.isBreached() && mLinker.getType().hasFlag(
                  MiscType.F_ARTEMIS_V))
                  && (ammoType.getMunitionType().contains(AmmoType.Munitions.M_ARTEMIS_V_CAPABLE))) {
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
                      target.getPosition(),
                      target.getPosition());
                if (((ammoType.getAmmoType() == AmmoType.AmmoTypeEnum.LRM)
                      || (ammoType.getAmmoType() == AmmoType.AmmoTypeEnum.LRM_IMP)
                      || (ammoType.getAmmoType() == AmmoType.AmmoTypeEnum.SRM)
                      || (ammoType.getAmmoType() == AmmoType.AmmoTypeEnum.SRM_IMP)
                      || (ammoType.getAmmoType() == AmmoType.AmmoTypeEnum.MML)
                      || (ammoType.getAmmoType() == AmmoType.AmmoTypeEnum.NLRM))
                      && (ammoType.getMunitionType().contains(AmmoType.Munitions.M_NARC_CAPABLE))) {
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

        if (game.getOptions().booleanOption(OptionsConstants.ADVANCED_AERO_RULES_AERO_SANITY)
              && entityTarget != null && entityTarget.isLargeCraft()) {
            nMissilesModifier -= (int) Math.floor(getAeroSanityAMSHitsMod());
        }

        // Use effective rack size (already calculated at method start)
        int rackSize = effectiveRackSize;
        boolean minRangeELRMAttack = false;

        // ELRMs only hit with half their rack size rounded up at minimum range.
        // Ignore this for space combat. 1 hex is 18km across.
        // PLAYTEST3 this is now ignored completely
        if (!game.getOptions().booleanOption(OptionsConstants.PLAYTEST_3)) {
            if (weaponType instanceof ExtendedLRMWeapon
                  && !game.getBoard().isSpace()
                  && (nRange <= weaponType.getMinimumRange())) {
                rackSize = rackSize / 2 + rackSize % 2;
                minRangeELRMAttack = true;
            }
        }

        if (allShotsHit()) {
            // We want buildings and large craft to be able to affect this number with AMS
            // treat as a Streak launcher (cluster roll 11) to make this happen
            missilesHit = Compute.missilesHit(rackSize,
                  nMissilesModifier, weapon.isHotLoaded(), true,
                  isAdvancedAMS());
        } else {
            if (attackingEntity instanceof BattleArmor) {
                missilesHit = Compute.missilesHit(rackSize
                            * ((BattleArmor) attackingEntity).getShootingStrength(),
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
            // Add incendiary mixed indicator
            addIncendiaryMixedReport(vPhaseReport);
        }
        Report r = new Report(3345);
        r.subject = subjectId;
        vPhaseReport.addElement(r);
        bSalvo = true;
        return missilesHit;
    }

    /**
     * Adds a report indicating incendiary missiles are mixed with the salvo. Per TO:AUE pg 181, shows "X of Y are
     * incendiary".
     *
     * @param vPhaseReport the report vector to add to
     */
    protected void addIncendiaryMixedReport(Vector<Report> vPhaseReport) {
        if (!isIncendiaryMixed()) {
            return;
        }
        Report r = new Report(3327);
        r.subject = subjectId;
        r.add(getIncendiaryMissileCount());
        r.add(weaponType.getRackSize());
        r.newlines = 0;
        vPhaseReport.addElement(r);
    }

    @Override
    protected int calcDamagePerHit() {
        // For infantry targets with incendiary mixed, apply +1 damage per 5 missiles bonus
        if (isIncendiaryMixed() && target.isConventionalInfantry()) {
            int effectiveRack = getEffectiveRackSize();
            // Calculate bonus: +1 per 5 missiles in the volley (round up), per TO:AUE pg 181
            int bonusDamage = getIncendiaryMissileCount();

            // Get base infantry damage
            double toReturn = Compute.directBlowInfantryDamage(
                  effectiveRack, bDirect ? toHit.getMoS() / 3 : 0,
                  weaponType.getInfantryDamageClass(),
                  ((Infantry) target).isMechanized(),
                  toHit.getThruBldg() != null, attackingEntity.getId(), calcDmgPerHitReport);

            // Add incendiary bonus damage and report it
            toReturn += bonusDamage;

            // Set the previous report to not add a newline so incendiary bonus appears inline
            if (!calcDmgPerHitReport.isEmpty()) {
                calcDmgPerHitReport.lastElement().newlines = 0;
            }

            Report r = new Report(3328);
            r.subject = subjectId;
            r.add(bonusDamage);
            calcDmgPerHitReport.addElement(r);

            toReturn = applyGlancingBlowModifier(toReturn, false);
            return (int) toReturn;
        }

        // Battle Armor also gets bonus damage when incendiary mixed
        if (isIncendiaryMixed() && target instanceof BattleArmor) {
            // Calculate bonus: +1 per 5 missiles in the volley (round up), per TO:AUE pg 181
            int bonusDamage = getIncendiaryMissileCount();
            Report r = new Report(3328);
            r.subject = subjectId;
            r.add(bonusDamage);
            calcDmgPerHitReport.addElement(r);
            return 1 + bonusDamage;
        }

        // Standard damage for non-infantry or non-incendiary
        return super.calcDamagePerHit();
    }

    /**
     * Returns the fire target number modifier for incendiary LRM. Per TO:AUE pg 181, incendiary rounds apply a -4
     * modifier to fire ignition rolls.
     *
     * @return TargetRoll with -4 modifier if incendiary mixed, or the weapon's standard fire TN
     */
    protected TargetRoll getIncendiaryFireTN() {
        if (isIncendiaryMixed()) {
            return new TargetRoll(-4, "Incendiary LRM");
        }
        return new TargetRoll(weaponType.getFireTN(), weaponType.getName());
    }

    @Override
    protected void handleIgnitionDamage(Vector<Report> vPhaseReport, IBuilding bldg, int hits) {
        if (!bSalvo) {
            // hits!
            Report r = new Report(2270);
            r.subject = subjectId;
            r.newlines = 0;
            vPhaseReport.addElement(r);
        }

        TargetRoll targetRoll = getIncendiaryFireTN();
        if (targetRoll.getValue() != TargetRoll.IMPOSSIBLE) {
            Report.addNewline(vPhaseReport);
            gameManager.tryIgniteHex(target.getPosition(), target.getBoardId(), subjectId, false, false,
                  targetRoll, true, -1, vPhaseReport);
        }
    }

    @Override
    protected void handleClearDamage(Vector<Report> vPhaseReport, IBuilding bldg, int nDamage) {
        if (!bSalvo) {
            // hits!
            Report r = new Report(2270);
            r.subject = subjectId;
            vPhaseReport.addElement(r);
        }
        // report that damage was "applied" to terrain
        Report r = new Report(3385);
        r.indent(2);
        r.subject = subjectId;
        r.add(nDamage);
        vPhaseReport.addElement(r);

        // Per TO:AUE pg 181, incendiary LRM applies -4 modifier to fire ignition
        TargetRoll targetRoll = getIncendiaryFireTN();

        // Try to ignite the hex (buildings handled separately)
        if ((bldg != null)
              && gameManager.tryIgniteHex(target.getPosition(), target.getBoardId(), subjectId, false,
              false,
              targetRoll, 5,
              vPhaseReport)) {
            return;
        }

        // Try to clear the hex
        Vector<Report> clearReports = gameManager.tryClearHex(target.getPosition(), target.getBoardId(),
              nDamage, subjectId);
        if (!clearReports.isEmpty()) {
            vPhaseReport.lastElement().newlines = 0;
        }
        vPhaseReport.addAll(clearReports);
    }

    @Override
    protected void handleEntityDamage(Entity entityTarget, Vector<Report> vPhaseReport, IBuilding bldg, int hits,
          int nCluster, int bldgAbsorbs) {
        // Apply standard entity damage first
        super.handleEntityDamage(entityTarget, vPhaseReport, bldg, hits, nCluster, bldgAbsorbs);

        // Per TO:AUE pg 181, incendiary missiles attempt to ignite the target hex
        if (isIncendiaryMixed() && !bMissed) {
            tryIncendiaryIgnition(vPhaseReport);
        }
    }

    @Override
    protected void handleBuildingDamage(Vector<Report> vPhaseReport, IBuilding bldg, int nDamage, Coords coords) {
        // Apply standard building damage first
        super.handleBuildingDamage(vPhaseReport, bldg, nDamage, coords);

        // Per TO:AUE pg 181, incendiary missiles attempt to ignite buildings
        if (isIncendiaryMixed() && !bMissed) {
            tryIncendiaryIgnition(vPhaseReport);
        }
    }

    /**
     * Attempts to ignite the target hex with incendiary missiles. Per TO:AUE pg 181, incendiary LRM rounds apply -4 to
     * fire ignition rolls.
     *
     * @param vPhaseReport the report vector to add results to
     */
    protected void tryIncendiaryIgnition(Vector<Report> vPhaseReport) {
        TargetRoll targetRoll = getIncendiaryFireTN();
        if (targetRoll.getValue() != TargetRoll.IMPOSSIBLE) {
            Report r = new Report(3329);
            r.subject = subjectId;
            r.newlines = 0;
            vPhaseReport.addElement(r);
            gameManager.tryIgniteHex(target.getPosition(), target.getBoardId(), subjectId, false, false,
                  targetRoll, true, -1, vPhaseReport);
        }
    }
}

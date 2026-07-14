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
import java.util.Vector;

import megamek.common.Report;
import megamek.common.ToHitData;
import megamek.common.actions.WeaponAttackAction;
import megamek.common.battleArmor.BattleArmor;
import megamek.common.compute.Compute;
import megamek.common.compute.ComputeECM;
import megamek.common.enums.GamePhase;
import megamek.common.equipment.Mounted;
import megamek.common.equipment.WeaponType;
import megamek.common.game.Game;
import megamek.common.loaders.EntityLoadingException;
import megamek.common.options.OptionsConstants;
import megamek.common.rolls.TargetRoll;
import megamek.common.units.Entity;
import megamek.common.units.IBuilding;
import megamek.common.units.Infantry;
import megamek.common.units.Targetable;
import megamek.common.weapons.Weapon;
import megamek.common.weapons.handlers.AttackHandler;
import megamek.server.totalWarfare.TWGameManager;

/**
 * @author Sebastian Brocks
 */
public class LRMSwarmHandler extends LRMHandler {
    @Serial
    private static final long serialVersionUID = 7962873403915683220L;

    private int swarmMissilesNowLeft = 0;
    private boolean handledHeat = false;

    public LRMSwarmHandler(ToHitData t, WeaponAttackAction w, Game g, TWGameManager m) throws EntityLoadingException {
        super(t, w, g, m);
        sSalvoType = " swarm missile(s) ";
    }

    @Override
    public boolean handle(GamePhase phase, Vector<Report> vPhaseReport) {
        if (!cares(phase)) {
            return true;
        }
        Entity entityTarget = (target.getTargetType() == Targetable.TYPE_ENTITY) ? (Entity) target
              : null;
        final boolean targetInBuilding = Compute.isInBuilding(game,
              entityTarget);
        final boolean bldgDamagedOnMiss = targetInBuilding
              && !(target instanceof Infantry)
              && attackingEntity.getPosition().distance(target.getPosition()) <= 1;

        if (entityTarget != null) {
            attackingEntity.setLastTarget(entityTarget.getId());
            attackingEntity.setLastTargetDisplayName(entityTarget.getDisplayName());
        }

        // Which building takes the damage?
        IBuilding bldg = game.getBoard().getBuildingAt(target.getPosition());

        // Report weapon attack and its to-hit value.
        Report report = new Report(3115);
        report.indent();
        report.newlines = 0;
        report.subject = subjectId;
        report.add(weaponType.getName() + " (" + ammoType.getShortName() + ")");
        if (entityTarget != null) {
            report.addDesc(entityTarget);
            // record which launcher targeted the target
            entityTarget.addTargetedBySwarm(attackingEntity.getId(), weaponAttackAction.getWeaponId());
        } else {
            report.messageId = 3120;
            report.add(target.getDisplayName(), true);
        }
        vPhaseReport.addElement(report);
        if (toHit.getValue() == TargetRoll.IMPOSSIBLE) {
            report = new Report(3135);
            report.subject = subjectId;
            report.add(toHit.getDesc());
            vPhaseReport.addElement(report);
            return false;
        } else if (toHit.getValue() == TargetRoll.AUTOMATIC_FAIL) {
            report = new Report(3140);
            report.newlines = 0;
            report.subject = subjectId;
            report.add(toHit.getDesc());
            vPhaseReport.addElement(report);
        } else if (toHit.getValue() == TargetRoll.AUTOMATIC_SUCCESS) {
            report = new Report(3145);
            report.newlines = 0;
            report.subject = subjectId;
            report.add(toHit.getDesc());
            vPhaseReport.addElement(report);
        } else {
            // roll to hit
            report = new Report(3150);
            report.newlines = 0;
            report.subject = subjectId;
            report.add(toHit);
            vPhaseReport.addElement(report);
        }

        // dice have been rolled, thanks
        report = new Report(3155);
        report.newlines = 0;
        report.subject = subjectId;
        report.add(roll);
        vPhaseReport.addElement(report);

        // do we hit?
        bMissed = roll.getIntValue() < toHit.getValue();

        // are we a glancing hit?
        setGlancingBlowFlags(entityTarget);
        addGlancingBlowReports(vPhaseReport);

        // Set Margin of Success/Failure.
        toHit.setMoS(roll.getIntValue() - Math.max(2, toHit.getValue()));
        bDirect = game.getOptions().booleanOption(OptionsConstants.ADVANCED_COMBAT_TAC_OPS_DIRECT_BLOW)
              && ((toHit.getMoS() / 3) >= 1) && (entityTarget != null);
        if (bDirect) {
            report = new Report(3189);
            report.subject = attackingEntity.getId();
            report.newlines = 0;
            vPhaseReport.addElement(report);
        }

        // Do this stuff first, because some weapon's miss report reference the
        // amount of shots fired and stuff.
        if (!handledHeat) {
            addHeat();
            handledHeat = true;
        }

        // Any necessary PSRs, jam checks, etc.
        // If this boolean is true, don't report
        // the miss later, as we already reported
        // it in doChecks
        boolean missReported = doChecks(vPhaseReport);

        nDamPerHit = calcDamagePerHit();

        // Do we need some sort of special resolution (minefields, artillery,
        if (specialResolution(vPhaseReport, entityTarget)) {
            return false;
        }

        if (bMissed && !missReported) {
            reportMiss(vPhaseReport);

            // Works out fire setting, AMS shots, and whether continuation is
            // necessary.
            if (!handleSpecialMiss(entityTarget, bldgDamagedOnMiss, bldg,
                  vPhaseReport, phase)) {
                return false;
            }
        }

        // handle damage. . different weapons do this in very different
        // ways
        int hits = calcHits(vPhaseReport), nCluster = calculateNumCluster();

        if (bMissed) {
            return false;

        } // End missed-target

        // Buildings shield all units from a certain amount of damage.
        // Amount is based upon the building's CF at the phase's start.
        int bldgAbsorbs = 0;
        if (targetInBuilding && (bldg != null)
              && (toHit.getThruBldg() == null)) {
            bldgAbsorbs = bldg.getAbsorption(target.getPosition());
        }

        // Attacking infantry in buildings from same building
        if (targetInBuilding && (bldg != null)
              && (toHit.getThruBldg() != null)
              && (entityTarget instanceof Infantry)) {
            // If elevation is the same, building doesn't absorb
            if (attackingEntity.getElevation() != entityTarget.getElevation()) {
                int dmgClass = weaponType.getInfantryDamageClass();
                int nDamage;
                if (dmgClass < WeaponType.WEAPON_BURST_1D6) {
                    nDamage = nDamPerHit * Math.min(nCluster, hits);
                } else {
                    // Need to indicate to handleEntityDamage that the
                    // absorbed damage shouldn't reduce incoming damage,
                    // since the incoming damage was reduced in
                    // Compute.directBlowInfantryDamage
                    nDamage = -weaponType.getDamage(nRange)
                          * Math.min(nCluster, hits);
                }
                bldgAbsorbs = (int) Math.round(nDamage
                      * bldg.getInfDmgFromInside());
            } else {
                // Used later to indicate a special report
                bldgAbsorbs = Integer.MIN_VALUE;
            }
        }

        // Make sure the player knows when his attack causes no damage.
        if (hits == 0) {
            report = new Report(3365);
            report.subject = subjectId;
            vPhaseReport.addElement(report);
        }

        // Handle full-salvo hit on initial attack so we don't accidentally spawn another full shot
        if (hits == weaponType.getRackSize()) {
            swarmMissilesNowLeft = 0;
        }
        // for each cluster of hits, do a chunk of damage
        while (hits > 0) {
            int nDamage;
            // targeting a hex for igniting
            if ((target.getTargetType() == Targetable.TYPE_HEX_IGNITE)
                  || (target.getTargetType() == Targetable.TYPE_BLDG_IGNITE)) {
                handleIgnitionDamage(vPhaseReport, bldg, hits);
                hits = 0;
            }
            // targeting a hex for clearing
            if (target.getTargetType() == Targetable.TYPE_HEX_CLEAR) {
                nDamage = nDamPerHit * hits;
                handleClearDamage(vPhaseReport, bldg, nDamage);
                hits = 0;
            }
            // Targeting a building.
            if (target.getTargetType() == Targetable.TYPE_BUILDING) {
                // The building takes the full brunt of the attack.
                nDamage = nDamPerHit * hits;
                handleBuildingDamage(vPhaseReport, bldg, nDamage,
                      target.getPosition());
                hits = 0;
            }
            if (entityTarget != null) {
                handleEntityDamage(entityTarget, vPhaseReport, bldg, hits,
                      nCluster, bldgAbsorbs);
                gameManager.creditKill(entityTarget, attackingEntity);
                hits -= nCluster;
                firstHit = false;
            }
        } // Handle the next cluster.

        Report.addNewline(vPhaseReport);
        if (swarmMissilesNowLeft > 0) {
            Entity swarmTarget = Compute.getSwarmMissileTarget(game,
                  attackingEntity.getId(), target.getPosition(), weaponAttackAction.getWeaponId());
            boolean stoppedByECM = ComputeECM.isAffectedByECM(attackingEntity,
                  target.getPosition(), target.getPosition())
                  && !(this instanceof LRMSwarmIHandler);
            if (swarmTarget != null && !stoppedByECM) {
                report = new Report(3420, Report.HIDDEN);
                report.subject = subjectId;
                report.indent(2);
                report.add(swarmMissilesNowLeft);
                vPhaseReport.addElement(report);
                weapon.setUsedThisRound(false);
                WeaponAttackAction newWaa = getWeaponAttackAction(swarmTarget, swarmMissilesNowLeft);
                Mounted<?> m = attackingEntity.getEquipment(weaponAttackAction.getWeaponId());
                Weapon w = (Weapon) m.getType();
                // increase ammo by one, we'll use one that we shouldn't use
                // in the next line
                weapon.getLinked().setShotsLeft(
                      weapon.getLinked().getBaseShotsLeft() + 1);
                AttackHandler ah = w.fire(newWaa, game, gameManager);
                LRMSwarmHandler wh = (LRMSwarmHandler) ah;
                // attack the new target
                wh.handledHeat = true;
                wh.handle(phase, vPhaseReport);
            } else {
                if (swarmTarget == null) {
                    report = new Report(3425);
                } else {
                    report = new Report(3426);
                }
                report.add(swarmMissilesNowLeft);
                report.subject = subjectId;
                report.indent(2);
                vPhaseReport.addElement(report);
            }

            // Reset the Swarm Missiles variable to 0 because if we don't it
            // thinks we've already computed this when we fire the next rack.
            swarmMissilesNowLeft = 0;

        }
        return false;
    }

    private WeaponAttackAction getWeaponAttackAction(Entity swarmTarget, int swarmMissilesNowLeft) {
        WeaponAttackAction weaponAttackAction = new WeaponAttackAction(attackingEntity.getId(),
              swarmTarget.getId(),
              this.weaponAttackAction.getWeaponId());
        weaponAttackAction.setSwarmingMissiles(true);
        weaponAttackAction.setSwarmMissiles(swarmMissilesNowLeft);
        weaponAttackAction.setOldTargetId(target.getId());
        weaponAttackAction.setOldTargetType(target.getTargetType());
        weaponAttackAction.setOriginalTargetId(this.weaponAttackAction.getOriginalTargetId());
        weaponAttackAction.setOriginalTargetType(this.weaponAttackAction.getOriginalTargetType());
        weaponAttackAction.setAmmoId(this.weaponAttackAction.getAmmoId());
        weaponAttackAction.setAmmoMunitionType(this.weaponAttackAction.getAmmoMunitionType());
        weaponAttackAction.setAmmoCarrier(this.weaponAttackAction.getAmmoCarrier());
        return weaponAttackAction;
    }

    @Override
    protected int calcDamagePerHit() {
        // This needs to override the superclass method because in case of swarm
        // the damage to adjacent infantry should be based on the missiles left over,
        // not the total rack size.
        if (target.isConventionalInfantry()) {
            int missiles = weaponAttackAction.isSwarmingMissiles() ? weaponAttackAction.getSwarmMissiles()
                  : weaponType.getRackSize();
            double toReturn = Compute.directBlowInfantryDamage(
                  missiles, bDirect ? toHit.getMoS() / 3 : 0,
                  weaponType.getInfantryDamageClass(),
                  ((Infantry) target).isMechanized(),
                  toHit.getThruBldg() != null, attackingEntity.getId(), calcDmgPerHitReport);

            toReturn = applyGlancingBlowModifier(toReturn, true);
            return (int) toReturn;
        }
        return 1;
    }

    protected boolean handleSpecialMiss(Entity entityTarget,
          boolean bldgDamagedOnMiss, IBuilding bldg,
          Vector<Report> vPhaseReport, GamePhase phase) {
        super.handleSpecialMiss(entityTarget, bldgDamagedOnMiss, bldg,
              vPhaseReport);
        int swarmMissilesNowLeft = weaponAttackAction.getSwarmMissiles();
        if (swarmMissilesNowLeft == 0) {
            swarmMissilesNowLeft = weaponType.getRackSize();
        }
        if (entityTarget != null) {
            attackingEntity.setLastTarget(entityTarget.getId());
            attackingEntity.setLastTargetDisplayName(entityTarget.getDisplayName());
        }

        Entity swarmTarget = Compute.getSwarmMissileTarget(game, attackingEntity.getId(),
              target.getPosition(), weaponAttackAction.getWeaponId());
        boolean stoppedByECM = ComputeECM.isAffectedByECM(attackingEntity,
              target.getPosition(), target.getPosition())
              && !(this instanceof LRMSwarmIHandler);
        if (swarmTarget != null && !stoppedByECM) {
            Report r = new Report(3420, Report.HIDDEN);
            r.subject = subjectId;
            r.indent(2);
            r.add(swarmMissilesNowLeft);
            vPhaseReport.addElement(r);
            weapon.setUsedThisRound(false);
            WeaponAttackAction newWaa = getWeaponAttackAction(swarmTarget, swarmMissilesNowLeft);
            Mounted<?> m = attackingEntity.getEquipment(weaponAttackAction.getWeaponId());
            Weapon w = (Weapon) m.getType();
            // increase ammo by one, we'll use one that we shouldn't use
            // in the next line
            weapon.getLinked().setShotsLeft(
                  weapon.getLinked().getBaseShotsLeft() + 1);
            AttackHandler ah = w.fire(newWaa, game, gameManager);
            LRMSwarmHandler wh = (LRMSwarmHandler) ah;
            // attack the new target
            wh.handledHeat = true;
            wh.handle(phase, vPhaseReport);
        } else {
            Report report;
            if (swarmTarget == null) {
                report = new Report(3425);
            } else {
                report = new Report(3426);
            }
            report.add(swarmMissilesNowLeft);
            report.subject = subjectId;
            report.indent(2);
            vPhaseReport.addElement(report);
        }
        return false;
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

        int missilesHit;
        int nMissilesModifier = getClusterModifiers(false);

        // add AMS mods
        int amsMod = getAMSHitsMod(vPhaseReport);
        if (game.getOptions().booleanOption(OptionsConstants.ADVANCED_AERO_RULES_AERO_SANITY)) {
            Entity entityTarget = (target.getTargetType() == Targetable.TYPE_ENTITY) ? (Entity) target
                  : null;
            if (entityTarget != null && entityTarget.isLargeCraft()) {
                amsMod = (int) -getAeroSanityAMSHitsMod();
            }
        }
        nMissilesModifier += amsMod;

        int swarmMissilesLeft = weaponAttackAction.getSwarmMissiles();
        // swarm or swarm-I shots may just hit with the remaining missiles
        if (swarmMissilesLeft > 0) {
            if (allShotsHit()) {
                missilesHit = (swarmMissilesLeft - amsMod);
            } else {
                missilesHit = Compute.missilesHit(swarmMissilesLeft,
                      nMissilesModifier, weapon.isHotLoaded(), false,
                      isAdvancedAMS());
            }
        } else {
            missilesHit = allShotsHit() ? weaponType.getRackSize()
                  : Compute
                  .missilesHit(weaponType.getRackSize(), nMissilesModifier,
                        weapon.isHotLoaded(), false, isAdvancedAMS());
            swarmMissilesLeft = weaponType.getRackSize();
        }
        swarmMissilesNowLeft = swarmMissilesLeft - missilesHit;
        Report r = new Report(3325, Report.HIDDEN);
        r.subject = subjectId;
        r.add(missilesHit);
        r.add(sSalvoType);
        r.add(toHit.getTableDesc());
        r.newlines = 0;
        vPhaseReport.addElement(r);
        r = new Report(3345, Report.HIDDEN);
        r.subject = subjectId;
        vPhaseReport.addElement(r);
        bSalvo = true;
        return missilesHit;
    }
}

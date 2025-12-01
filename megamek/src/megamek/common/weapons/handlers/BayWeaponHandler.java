/*
 * Copyright (C) 2005 Ben Mazur (bmazur@sev.org)
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

import static java.lang.Math.floor;

import java.io.Serial;
import java.util.Vector;

import megamek.common.RangeType;
import megamek.common.Report;
import megamek.common.ToHitData;
import megamek.common.actions.WeaponAttackAction;
import megamek.common.compute.Compute;
import megamek.common.enums.GamePhase;
import megamek.common.equipment.AmmoMounted;
import megamek.common.equipment.WeaponMounted;
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
import megamek.logging.MMLogger;
import megamek.server.totalWarfare.TWGameManager;

/**
 * @author Jay Lawson
 */
public class BayWeaponHandler extends WeaponHandler {
    private static final MMLogger LOGGER = MMLogger.create(BayWeaponHandler.class);

    @Serial
    private static final long serialVersionUID = -1618484541772117621L;
    protected AmmoMounted ammo;

    protected BayWeaponHandler() {
        // deserialization only
    }

    public BayWeaponHandler(ToHitData t, WeaponAttackAction w, Game g, TWGameManager m) throws EntityLoadingException {
        super(t, w, g, m);
    }

    /**
     * Calculate the attack value based on range
     *
     * @return an <code>int</code> representing the attack value at that range.
     */
    @Override
    protected int calcAttackValue() {
        double av = 0;
        int range = RangeType.rangeBracket(nRange, weaponType.getATRanges(), true, false);

        for (WeaponMounted m : weapon.getBayWeapons()) {
            if (!m.isBreached() && !m.isDestroyed() && !m.isJammed()) {
                WeaponType bayWType = m.getType();
                // need to cycle through weapons and add av
                if (range == WeaponType.RANGE_SHORT) {
                    av = av + bayWType.getShortAV();
                } else if (range == WeaponType.RANGE_MED) {
                    av = av + bayWType.getMedAV();
                } else if (range == WeaponType.RANGE_LONG) {
                    av = av + bayWType.getLongAV();
                } else if (range == WeaponType.RANGE_EXT) {
                    av = av + bayWType.getExtAV();
                }
            }
        }
        if (bDirect) {
            av = Math.min(av + (int) floor(toHit.getMoS() / 3.0), av * 2);
        }
        av = applyGlancingBlowModifier(av, false);
        av = (int) Math.floor(getBracketingMultiplier() * av);
        return (int) Math.ceil(av);
    }

    @Override
    protected void addHeat() {
        // Only add heat for first shot in strafe
        if (isStrafing && !isStrafingFirstShot()) {
            return;
        }
        if (!(toHit.getValue() == TargetRoll.IMPOSSIBLE)) {
            if (game.getOptions().booleanOption(OptionsConstants.ADVANCED_AERO_RULES_HEAT_BY_BAY)) {
                for (WeaponMounted m : weapon.getBayWeapons()) {
                    attackingEntity.heatBuildup += m.getCurrentHeat();
                }
            } else {
                int loc = weapon.getLocation();
                boolean rearMount = weapon.isRearMounted();
                if (!attackingEntity.hasArcFired(loc, rearMount)) {
                    attackingEntity.heatBuildup += attackingEntity.getHeatInArc(loc, rearMount);
                    attackingEntity.setArcFired(loc, rearMount);
                }
            }
        }
    }

    /**
     * Sigh, according to the ruling linked below, when weapon bays are fired at ground targets, they should make one
     * to-hit roll, but the AV of each weapon should be applied separately as damage - that needs a special handler
     *
     * @return a <code>boolean</code> value indicating whether this should be kept or not
     */
    @Override
    public boolean handle(GamePhase phase, Vector<Report> vPhaseReport) {

        if (game.getOptions().booleanOption(OptionsConstants.ADVANCED_AERO_RULES_AERO_SANITY)) {
            return handleAeroSanity(phase, vPhaseReport);
        }

        Entity entityTarget = (target.getTargetType() == Targetable.TYPE_ENTITY) ? (Entity) target
              : null;

        if ((((null == entityTarget) || entityTarget.isAirborne())
              && (target != null)
              && (target.getTargetType() != Targetable.TYPE_HEX_CLEAR
              && target.getTargetType() != Targetable.TYPE_HEX_IGNITE
              && target.getTargetType() != Targetable.TYPE_BUILDING))
              || game.getBoard().isSpace()
              // Capital missile launchers should return the root handler...
              || (weaponType.getAtClass() == (WeaponType.CLASS_CAPITAL_MISSILE))
              || (weaponType.getAtClass() == (WeaponType.CLASS_AR10))) {
            return super.handle(phase, vPhaseReport);
        }

        // then we have a ground target, so we need to handle it in a special
        // way
        insertAttacks(phase, vPhaseReport);

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
        String number = numWeapons > 1 ? " (" + numWeapons + ")" : "";

        // Report weapon attack and its to-hit value.
        Report r = new Report(3115);
        r.indent();
        r.newlines = 0;
        r.subject = subjectId;
        r.add(weaponType.getName() + number);
        if (entityTarget != null) {
            r.addDesc(entityTarget);
        } else {
            r.messageId = 3120;
            r.add(target.getDisplayName(), true);
        }

        vPhaseReport.addElement(r);
        if (toHit.getValue() == TargetRoll.IMPOSSIBLE) {
            r = new Report(3135);
            r.subject = subjectId;
            r.add(toHit.getDesc());
            vPhaseReport.addElement(r);
            return false;
        } else if (toHit.getValue() == TargetRoll.AUTOMATIC_FAIL) {
            r = new Report(3140);
            r.newlines = 0;
            r.subject = subjectId;
            r.add(toHit.getDesc());
            vPhaseReport.addElement(r);
        } else if (toHit.getValue() == TargetRoll.AUTOMATIC_SUCCESS) {
            r = new Report(3145);
            r.newlines = 0;
            r.subject = subjectId;
            r.add(toHit.getDesc());
            vPhaseReport.addElement(r);
        } else {
            // roll to hit
            r = new Report(3150);
            r.newlines = 0;
            r.subject = subjectId;
            r.add(toHit);
            vPhaseReport.addElement(r);
        }

        // dice have been rolled, thanks
        r = new Report(3155);
        r.newlines = 0;
        r.subject = subjectId;
        r.add(roll);
        vPhaseReport.addElement(r);

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
            r = new Report(3189);
            r.subject = attackingEntity.getId();
            r.newlines = 0;
            vPhaseReport.addElement(r);
        }

        // Do this stuff first, because some weapon's miss report reference the
        // amount of shots fired and stuff.
        nDamPerHit = calcAttackValue();
        addHeat();

        // Any necessary PSRs, jam checks, etc.
        // If this boolean is true, don't report
        // the miss later, as we already reported
        // it in doChecks
        boolean missReported = doChecks(vPhaseReport);
        if (missReported) {
            bMissed = true;
        }

        // Do we need some sort of special resolution (minefields, artillery,
        if (specialResolution(vPhaseReport, entityTarget)) {
            return false;
        }

        if (bMissed && !missReported) {
            reportMiss(vPhaseReport);

            // Works out fire setting, AMS shots, and whether continuation is
            // necessary.
            if (!handleSpecialMiss(entityTarget, bldgDamagedOnMiss, bldg, vPhaseReport)) {
                return false;
            }
        }

        if (bMissed) {
            return false;

        } // End missed-target

        if ((target.getTargetType() == Targetable.TYPE_HEX_IGNITE)
              || (target.getTargetType() == Targetable.TYPE_BLDG_IGNITE)) {
            handleIgnitionDamage(vPhaseReport, bldg, 1);
            return false;
        }
        if (target.getTargetType() == Targetable.TYPE_HEX_CLEAR) {
            handleClearDamage(vPhaseReport, bldg, nDamPerHit);
            return false;
        }
        // Targeting a building.
        if (target.getTargetType() == Targetable.TYPE_BUILDING) {
            // The building takes the full brunt of the attack
            handleBuildingDamage(vPhaseReport, bldg, nDamPerHit, target.getPosition());
            return false;
        }

        Report.addNewline(vPhaseReport);
        // loop through weapons in bay and do damage
        int range = RangeType.rangeBracket(nRange, weaponType.getATRanges(), true, false);
        int hits = 1;
        int nCluster = 1;
        for (WeaponMounted m : weapon.getBayWeapons()) {
            double av = getAv(m, range);
            nDamPerHit = (int) Math.ceil(av);

            if (nDamPerHit <= 0) {
                continue;
            }

            bSalvo = true;

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
                        nDamage = -weaponType.getDamage(nRange) * Math.min(nCluster, hits);
                    }
                    bldgAbsorbs = (int) Math.round(nDamage * bldg.getInfDmgFromInside());
                } else {
                    // Used later to indicate a special report
                    bldgAbsorbs = Integer.MIN_VALUE;
                }
            }

            handleEntityDamage(entityTarget, vPhaseReport, bldg, hits, nCluster, bldgAbsorbs);
            gameManager.creditKill(entityTarget, attackingEntity);
        } // Handle the next weapon in the bay
        Report.addNewline(vPhaseReport);
        return false;
    }

    private static double getAv(WeaponMounted m, int range) {
        double av = 0;
        if (!m.isBreached() && !m.isDestroyed() && !m.isJammed()) {
            WeaponType bayWType = m.getType();
            // need to cycle through weapons and add av
            if (range == WeaponType.RANGE_SHORT) {
                av = bayWType.getShortAV();
            } else if (range == WeaponType.RANGE_MED) {
                av = bayWType.getMedAV();
            } else if (range == WeaponType.RANGE_LONG) {
                av = bayWType.getLongAV();
            } else if (range == WeaponType.RANGE_EXT) {
                av = bayWType.getExtAV();
            }
        }
        return av;
    }

    /**
     * Calculate the starting armor value of a flight of Capital Missiles Used for Aero Sanity. This is done in
     * calcAttackValue() otherwise
     */
    protected int initializeCapMissileArmor() {
        return 0;
    }

    public boolean handleAeroSanity(GamePhase phase, Vector<Report> vPhaseReport) {
        if (!cares(phase)) {
            return true;
        }

        insertAttacks(phase, vPhaseReport);

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
        report.add(weaponType.getName());
        if (entityTarget != null) {
            report.addDesc(entityTarget);
        } else {
            report.messageId = 3120;
            report.add(target.getDisplayName(), true);
        }
        vPhaseReport.addElement(report);

        // Handle point defense fire. For cluster hit missile launchers, we'll report
        // later.
        CounterAV = calcCounterAV();

        // We need this for thunderbolt bays
        CapMissileAMSMod = calcCapMissileAMSMod();

        // Set up Capital Missile (thunderbolt) armor
        CapMissileArmor = initializeCapMissileArmor();

        // and now damage it
        CapMissileArmor = (CapMissileArmor - CounterAV);

        // Only do this if the missile wasn't destroyed
        if (CapMissileAMSMod > 0 && CapMissileArmor > 0) {
            toHit.addModifier(CapMissileAMSMod, "Damage from Point Defenses");
            if (roll.getIntValue() < toHit.getValue()) {
                CapMissileMissed = true;
            }
        }

        // Report any AMS bay action against Capital missiles that doesn't destroy them
        // all.
        if (amsBayEngagedCap && CapMissileArmor > 0) {
            report = new Report(3358);
            report.add(CapMissileAMSMod);
            report.subject = subjectId;
            vPhaseReport.addElement(report);

            // Report any PD bay action against Capital missiles that doesn't destroy them
            // all.
        } else if (pdBayEngagedCap && CapMissileArmor > 0) {
            report = new Report(3357);
            report.add(CapMissileAMSMod);
            report.subject = subjectId;
            vPhaseReport.addElement(report);
        }

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

        // Don't add heat here, because that will be handled by individual weapons (even
        // if heat by arc)

        // Any necessary PSRs, jam checks, etc.
        // If this boolean is true, don't report
        // the miss later, as we already reported
        // it in doChecks
        boolean missReported = doChecks(vPhaseReport);
        if (missReported) {
            bMissed = true;
        }

        // Do we need some sort of special resolution (minefields,
        // artillery,
        if (specialResolution(vPhaseReport, entityTarget)) {
            return false;
        }

        // Large missiles
        // use this if AMS counterfire destroys all the missiles
        if (amsBayEngagedCap && (CapMissileArmor <= 0)) {
            report = new Report(3356);
            report.indent();
            report.subject = subjectId;
            vPhaseReport.addElement(report);
            return false;
        }
        // use this if PD counterfire destroys all the Capital missiles
        if (pdBayEngagedCap && (CapMissileArmor <= 0)) {
            report = new Report(3355);
            report.indent();
            report.subject = subjectId;
            vPhaseReport.addElement(report);
            return false;
        }

        // We have to adjust the reports on a miss, so they line up
        if (bMissed) {
            reportMiss(vPhaseReport);
            if (!handleSpecialMiss(entityTarget, bldgDamagedOnMiss, bldg, vPhaseReport)) {
                return false;
            }
        }

        // Report point defense effects
        // Set up a cluster hits table modifier
        double counterAVMod = getCounterAV();
        // Report a failure due to overheating
        if (pdOverheated
              && (!(amsBayEngaged
              || amsBayEngagedCap
              || amsBayEngagedMissile
              || pdBayEngaged
              || pdBayEngagedCap
              || pdBayEngagedMissile))) {
            report = new Report(3359);
            report.subject = subjectId;
            report.indent();
            vPhaseReport.addElement(report);
        } else if (pdOverheated) {
            // Report a partial failure
            report = new Report(3361);
            report.subject = subjectId;
            report.indent();
            vPhaseReport.addElement(report);
            // Halve the effectiveness of cluster hits modification
            counterAVMod /= 2.0;
        }
        // Now report the effects, if any
        // Missiles using the cluster hits table
        if (amsBayEngaged || pdBayEngaged) {
            report = new Report(3366);
            report.indent();
            report.subject = subjectId;
            report.add((int) counterAVMod);
            report.newlines = 0;
            vPhaseReport.addElement(report);
        }

        Report.addNewline(vPhaseReport);
        // New toHit data to hold our bay auto hit. We want to be able to get
        // glancing/direct blow
        // data from the 'real' toHit data of this bay handler
        ToHitData autoHit = new ToHitData();
        autoHit.addModifier(TargetRoll.AUTOMATIC_SUCCESS, "if the bay hits, all bay weapons hit");
        int replaceReport;
        for (WeaponMounted m : weapon.getBayWeapons()) {
            if (!m.isBreached() && !m.isDestroyed() && !m.isJammed()) {
                WeaponType bayWType = m.getType();
                if (bayWType instanceof Weapon) {
                    replaceReport = vPhaseReport.size();
                    WeaponAttackAction bayWaa = new WeaponAttackAction(weaponAttackAction.getEntityId(),
                          weaponAttackAction.getTargetType(),
                          weaponAttackAction.getTargetId(),
                          m.getEquipmentNum());
                    AttackHandler weaponBayHandler = ((Weapon) bayWType).getCorrectHandler(autoHit, bayWaa, game,
                          gameManager);
                    weaponBayHandler.setAnnouncedEntityFiring(false);
                    // This should always be true
                    if (weaponBayHandler instanceof WeaponHandler wHandler) {
                        wHandler.setParentBayHandler(this);
                    } else {
                        LOGGER.error("weaponBayHandler {} is not a weapon handler! Cannot set parent bay handler.",
                              weaponBayHandler.getClass());
                        continue;
                    }

                    weaponBayHandler.handle(phase, vPhaseReport);

                    if (vPhaseReport.size() > replaceReport) {
                        // fix the reporting - is there a better way to do this
                        Report currentReport = vPhaseReport.get(replaceReport);
                        while (null != currentReport) {
                            vPhaseReport.remove(replaceReport);
                            if ((currentReport.newlines > 0) || (vPhaseReport.size() <= replaceReport)) {
                                currentReport = null;
                            } else {
                                currentReport = vPhaseReport.get(replaceReport);
                            }
                        }
                        report = new Report(3115);
                        report.indent(2);
                        report.newlines = 1;
                        report.subject = subjectId;
                        report.add(bayWType.getName());
                        if (entityTarget != null) {
                            report.addDesc(entityTarget);
                        } else {
                            report.messageId = 3120;
                            report.add(target.getDisplayName(), true);
                        }
                        vPhaseReport.add(replaceReport, report);
                    }
                }
            }
        }

        Report.addNewline(vPhaseReport);
        return false;
    }
}

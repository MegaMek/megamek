/*
 * Copyright (C) 2005 Ben Mazur (bmazur@sev.org)
 * Copyright (C) 2017-2025 The MegaMek Team. All Rights Reserved.
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
import megamek.common.equipment.AmmoType;
import megamek.common.equipment.MiscType;
import megamek.common.equipment.Mounted;
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
import megamek.server.totalWarfare.TWGameManager;

/**
 * @author Jay Lawson
 */
public class MissileBayWeaponHandler extends AmmoBayWeaponHandler {

    @Serial
    private static final long serialVersionUID = -1618484541772117621L;

    protected MissileBayWeaponHandler() {
        // deserialization only
    }

    /**
     *
     */
    public MissileBayWeaponHandler(ToHitData t, WeaponAttackAction w, Game g,
          TWGameManager m) throws EntityLoadingException {
        super(t, w, g, m);
    }

    /**
     * Calculate the attack value based on range This needs to do additional work for Weapon Bays with ammo. I need to
     * use the ammo within this function because I may run out of ammo while going through the loop Sine this function
     * is called in the WeaponHandler constructor it should be ok to use the ammo here
     *
     * @return an <code>int</code> representing the attack value at that range.
     */
    @Override
    protected int calcAttackValue() {

        double av = 0;
        double counterAV;
        int weaponArmor = 0;
        int range = RangeType.rangeBracket(nRange, weaponType.getATRanges(), true, false);

        for (WeaponMounted bayW : weapon.getBayWeapons()) {
            // check the currently loaded ammo
            AmmoMounted bayWAmmo = bayW.getLinkedAmmo();
            if (null == bayWAmmo || bayWAmmo.getUsableShotsLeft() < 1) {
                // try loading something else
                attackingEntity.loadWeaponWithSameAmmo(bayW);
                bayWAmmo = bayW.getLinkedAmmo();
            }
            if (!bayW.isBreached()
                  && !bayW.isDestroyed()
                  && !bayW.isJammed()
                  && bayWAmmo != null
                  && attackingEntity.getTotalAmmoOfType(bayWAmmo.getType()) >= bayW
                  .getCurrentShots()) {
                WeaponType bayWType = bayW.getType();
                // need to cycle through weapons and add av
                double current_av = 0;
                AmmoType ammoType = bayWAmmo.getType();

                if (range == WeaponType.RANGE_SHORT) {
                    current_av = bayWType.getShortAV();
                } else if (range == WeaponType.RANGE_MED) {
                    current_av = bayWType.getMedAV();
                } else if (range == WeaponType.RANGE_LONG) {
                    current_av = bayWType.getLongAV();
                } else if (range == WeaponType.RANGE_EXT) {
                    current_av = bayWType.getExtAV();
                }
                current_av = updateAVForAmmo(current_av, ammoType, bayWType,
                      range, bayW.getEquipmentNum());
                av = av + current_av;
                // If these are thunderbolts, they'll have missile armor
                weaponArmor += bayWType.getMissileArmor();
                // now use the ammo that we had loaded
                if (current_av > 0) {
                    int shots = bayW.getCurrentShots();
                    for (int i = 0; i < shots; i++) {
                        if (null == bayWAmmo
                              || bayWAmmo.getUsableShotsLeft() < 1) {
                            // try loading something else
                            attackingEntity.loadWeaponWithSameAmmo(bayW);
                            bayWAmmo = bayW.getLinkedAmmo();
                        }
                        if (null != bayWAmmo) {
                            bayWAmmo.setShotsLeft(bayWAmmo.getBaseShotsLeft() - 1);
                        }
                    }
                }
            }
        }
        // Bracketing fire reduces the number of missiles that hit
        av = (int) Math.floor(getBracketingMultiplier() * av);

        // Point Defenses engage the missiles still aimed at us
        counterAV = calcCounterAV();
        if (isThunderBolt()) {
            CapMissileArmor = weaponArmor - (int) counterAV;
            CapMissileAMSMod = calcCapMissileAMSMod();
        } else {
            av = av - counterAV;
        }

        // Apply direct/glancing blow modifiers to the survivors
        if (bDirect) {
            av = Math.min(av + (int) floor(toHit.getMoS() / 3.0), av * 2);
        }

        av = applyGlancingBlowModifier(av, false);

        return (int) Math.ceil(av);

    }

    /**
     * Sets the appropriate AMS Bay reporting flag depending on what type of missile this is
     */
    @Override
    protected void setAMSBayReportingFlag() {
        if (isThunderBolt()) {
            amsBayEngagedCap = true;
        } else {
            amsBayEngaged = true;
        }
    }

    /**
     * Sets the appropriate PD Bay reporting flag depending on what type of missile this is
     */
    @Override
    protected void setPDBayReportingFlag() {
        if (isThunderBolt()) {
            pdBayEngagedCap = true;
        } else {
            pdBayEngaged = true;
        }
    }

    // Check for Thunderbolt. We'll use this for single AMS resolution
    @Override
    protected boolean isThunderBolt() {
        return weaponType.hasFlag(WeaponType.F_LARGE_MISSILE);
    }

    /**
     * Calculate the starting armor value of a flight of thunderbolts Used for Aero Sanity. This is done in
     * calcAttackValue() otherwise
     */
    @Override
    protected int initializeCapMissileArmor() {
        int armor = 0;
        for (WeaponMounted bayW : weapon.getBayWeapons()) {
            armor += bayW.getType().getMissileArmor();
        }
        return armor;
    }

    @Override
    protected int calcCapMissileAMSMod() {
        CapMissileAMSMod = 0;
        if (isThunderBolt()) {
            CapMissileAMSMod = (int) Math.ceil(CounterAV / 10.0);
        }
        return CapMissileAMSMod;
    }

    /*
     * check for special munitions and their effect on av
     *
     */
    @Override
    protected double updateAVForAmmo(double current_av, AmmoType ammoType,
          WeaponType bayWType, int range, int wId) {
        Mounted<?> bayW = attackingEntity.getEquipment(wId);
        Mounted<?> mLinker = bayW.getLinkedBy();
        int bonus;
        if ((mLinker != null && mLinker.getType() instanceof MiscType
              && !mLinker.isDestroyed() && !mLinker.isMissing()
              && !mLinker.isBreached() && mLinker.getType().hasFlag(
              MiscType.F_ARTEMIS))
              && ammoType.getMunitionType().contains(AmmoType.Munitions.M_ARTEMIS_CAPABLE)) {
            bonus = (int) Math.ceil(ammoType.getRackSize() / 5.0);
            if ((ammoType.getAmmoType() == AmmoType.AmmoTypeEnum.SRM) || (ammoType.getAmmoType()
                  == AmmoType.AmmoTypeEnum.SRM_IMP)) {
                bonus = 2;
            }
            current_av = current_av + bonus;
        }
        // check for Artemis V
        if (((mLinker != null) && (mLinker.getType() instanceof MiscType)
              && !mLinker.isDestroyed() && !mLinker.isMissing()
              && !mLinker.isBreached() && mLinker.getType().hasFlag(
              MiscType.F_ARTEMIS_V))) {
            ammoType.getMunitionType();
        }// MML3 WOULD get a bonus from Artemis V, if you were crazy enough
        // to cross-tech it

        if (ammoType.getMunitionType().contains(AmmoType.Munitions.M_CLUSTER)) {
            current_av = Math.floor(0.6 * current_av);
        } else if (AmmoType.AmmoTypeEnum.ATM == ammoType.getAmmoType()) {
            if (ammoType.getMunitionType().contains(AmmoType.Munitions.M_EXTENDED_RANGE)) {
                current_av = bayWType.getShortAV() / 2;
            } else if (ammoType.getMunitionType().contains(AmmoType.Munitions.M_HIGH_EXPLOSIVE)) {
                current_av = 1.5 * current_av;
                if (range > WeaponType.RANGE_SHORT) {
                    current_av = 0.0;
                }
            }
        } else if (ammoType.getAmmoType() == AmmoType.AmmoTypeEnum.MML
              && !ammoType.hasFlag(AmmoType.F_MML_LRM)) {
            current_av = 2 * current_av;
            if (range > WeaponType.RANGE_SHORT) {
                current_av = 0;
            }
        }
        return current_av;

    }

    @Override
    public boolean handle(GamePhase phase, Vector<Report> vPhaseReport) {

        if (game.getOptions().booleanOption(OptionsConstants.ADVANCED_AERO_RULES_AERO_SANITY)) {
            return handleAeroSanity(phase, vPhaseReport);
        }

        Entity entityTarget = (target.getTargetType() == Targetable.TYPE_ENTITY) ? (Entity) target
              : null;

        if ((((null == entityTarget) || entityTarget.isAirborne())
              && target != null
              && (target.getTargetType() != Targetable.TYPE_HEX_CLEAR
              && target.getTargetType() != Targetable.TYPE_HEX_IGNITE
              && target.getTargetType() != Targetable.TYPE_BUILDING))
              || game.getBoard().isSpace()) {
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

        // Report any AMS bay action against standard missiles.
        // This only gets used in atmosphere/ground battles
        // Non AMS point defenses only work in space
        CounterAV = getCounterAV();
        // use this if counterfire destroys all the missiles
        if (amsBayEngaged && (attackValue <= 0)) {
            r = new Report(3356);
            r.indent();
            r.subject = subjectId;
            vPhaseReport.addElement(r);
        } else if (amsBayEngaged) {
            r = new Report(3354);
            r.indent();
            r.add(CounterAV);
            r.subject = subjectId;
            vPhaseReport.addElement(r);
        }

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
            if (!handleSpecialMiss(entityTarget, bldgDamagedOnMiss, bldg,
                  vPhaseReport)) {
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
            handleBuildingDamage(vPhaseReport, bldg, nDamPerHit,
                  target.getPosition());
            return false;
        }

        Report.addNewline(vPhaseReport);
        // loop through weapons in bay and do damage
        int range = RangeType.rangeBracket(nRange, weaponType.getATRanges(), true, false);
        int hits = 1;
        int nCluster = 1;
        for (WeaponMounted m : weapon.getBayWeapons()) {
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

            nDamPerHit = (int) (Math.ceil(av) - CounterAV);
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

            handleEntityDamage(entityTarget, vPhaseReport, bldg, hits,
                  nCluster, bldgAbsorbs);
            gameManager.creditKill(entityTarget, attackingEntity);
        } // Handle the next weapon in the bay
        Report.addNewline(vPhaseReport);
        return false;
    }
}

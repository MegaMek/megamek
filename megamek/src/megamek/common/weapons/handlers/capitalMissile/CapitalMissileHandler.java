/*
 * Copyright (c) 2005 - Ben Mazur (bmazur@sev.org)
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

package megamek.common.weapons.handlers.capitalMissile;

import static java.lang.Math.floor;

import java.io.Serial;
import java.util.Vector;

import megamek.common.RangeType;
import megamek.common.Report;
import megamek.common.ToHitData;
import megamek.common.actions.WeaponAttackAction;
import megamek.common.enums.GamePhase;
import megamek.common.equipment.AmmoType;
import megamek.common.equipment.WeaponMounted;
import megamek.common.equipment.WeaponType;
import megamek.common.game.Game;
import megamek.common.loaders.EntityLoadingException;
import megamek.common.options.OptionsConstants;
import megamek.common.rolls.TargetRoll;
import megamek.common.units.Entity;
import megamek.common.units.IBuilding;
import megamek.common.units.Targetable;
import megamek.common.weapons.handlers.AmmoWeaponHandler;
import megamek.common.weapons.handlers.WeaponHandler;
import megamek.server.totalWarfare.TWGameManager;

/**
 * @author Jay Lawson
 */
public class CapitalMissileHandler extends AmmoWeaponHandler {
    @Serial
    private static final long serialVersionUID = -1618484541772117621L;
    boolean advancedPD;

    /**
     *
     */
    public CapitalMissileHandler(ToHitData t, WeaponAttackAction w, Game g, TWGameManager m)
          throws EntityLoadingException {
        super(t, w, g, m);
        advancedPD = g.getOptions().booleanOption(OptionsConstants.ADVANCED_AERO_RULES_STRATOPS_ADV_POINT_DEFENSE);
    }

    /*
     * (non-Javadoc)
     *
     * @see megamek.common.weapons.handlers.AttackHandler#handle(int, java.util.Vector)
     */
    @Override
    public boolean handle(GamePhase phase, Vector<Report> vPhaseReport) {
        if (!cares(phase)) {
            return true;
        }

        int numAttacks = 1;

        Entity entityTarget = (target.getTargetType() == Targetable.TYPE_ENTITY) ? (Entity) target
              : null;

        if (entityTarget != null) {
            attackingEntity.setLastTarget(entityTarget.getId());
            attackingEntity.setLastTargetDisplayName(entityTarget.getDisplayName());
        }
        // Which building takes the damage?
        IBuilding bldg = game.getBoard().getBuildingAt(target.getPosition());
        String number = numWeapons > 1 ? " (" + numWeapons + ")" : "";
        for (int i = numAttacks; i > 0; i--) {
            // Report weapon attack and its to-hit value.
            Report report = new Report(3115);
            report.indent();
            report.newlines = 0;
            report.subject = subjectId;
            report.add(weaponType.getName() + number);
            if (entityTarget != null) {
                if ((weaponType.getAmmoType() != AmmoType.AmmoTypeEnum.NA)
                      && (weapon.getLinked() != null)
                      && (weapon.getLinked().getType() instanceof AmmoType ammoType)) {
                    if (!ammoType.getMunitionType().contains(AmmoType.Munitions.M_STANDARD)) {
                        report.messageId = 3116;
                        report.add(ammoType.getSubMunitionName());
                    }
                }
                report.addDesc(entityTarget);
            } else {
                report.messageId = 3120;
                report.add(target.getDisplayName(), true);
            }
            vPhaseReport.addElement(report);

            // are we a glancing hit?  Check for this here, report it later
            if (game.getOptions().booleanOption(OptionsConstants.ADVANCED_COMBAT_TAC_OPS_GLANCING_BLOWS)) {
                if (game.getOptions().booleanOption(OptionsConstants.ADVANCED_AERO_RULES_AERO_SANITY)) {
                    if (getParentBayHandler() != null) {
                        //Use the to-hit value for the bay handler, otherwise toHit is set to Automatic Success
                        WeaponHandler bayHandler = getParentBayHandler();
                        bGlancing = (roll.getIntValue() == bayHandler.toHit.getValue());
                        bLowProfileGlancing = isLowProfileGlancingBlow(entityTarget, bayHandler.toHit);
                    }
                } else {
                    setGlancingBlowFlags(entityTarget);
                }
            }

            // Set Margin of Success/Failure and check for Direct Blows
            if (game.getOptions().booleanOption(OptionsConstants.ADVANCED_AERO_RULES_AERO_SANITY)
                  && getParentBayHandler() != null) {
                //Use the to-hit value for the bay handler, otherwise toHit is set to Automatic Success
                WeaponHandler bayHandler = getParentBayHandler();
                toHit.setMoS(roll.getIntValue() - Math.max(2, bayHandler.toHit.getValue()));
            } else {
                toHit.setMoS(roll.getIntValue() - Math.max(2, toHit.getValue()));
            }
            bDirect = game.getOptions().booleanOption(OptionsConstants.ADVANCED_COMBAT_TAC_OPS_DIRECT_BLOW)
                  && ((toHit.getMoS() / 3) >= 1) && (entityTarget != null);

            // Used when using a grounded DropShip with individual weapons
            // or a fighter squadron loaded with ASM or Alamo bombs.
            nDamPerHit = calcDamagePerHit();

            // Point Defense fire vs Capital Missiles
            if (game.getOptions().booleanOption(OptionsConstants.ADVANCED_AERO_RULES_AERO_SANITY)
                  && getParentBayHandler() != null) {
                WeaponHandler bayHandler = getParentBayHandler();
                CounterAV = bayHandler.getCounterAV();
            } else {
                // This gets used if you're shooting at an airborne DropShip. It can defend with PD bays.
                attackValue = calcAttackValue();
            }
            // CalcAttackValue triggers counterfire, so now we can safely get this
            CapMissileAMSMod = getCapMissileAMSMod();

            // Only do this if the missile wasn't destroyed
            if (CapMissileAMSMod > 0 && CapMissileArmor > 0) {
                toHit.addModifier(CapMissileAMSMod, "Damage from Point Defenses");
                if (roll.getIntValue() < toHit.getValue()) {
                    CapMissileMissed = true;
                }
            }

            // Report any AMS bay action against Capital missiles that doesn't destroy them all.
            if (amsBayEngagedCap && CapMissileArmor > 0) {
                report = new Report(3358);
                report.add(CapMissileAMSMod);
                report.subject = subjectId;
                vPhaseReport.addElement(report);

                // Report any PD bay action against Capital missiles that doesn't destroy them all.
            } else if (pdBayEngagedCap && CapMissileArmor > 0) {
                report = new Report(3357);
                report.add(CapMissileAMSMod);
                report.subject = subjectId;
                vPhaseReport.addElement(report);
            }

            if (toHit.getValue() == TargetRoll.IMPOSSIBLE) {
                report = new Report(3135);
                report.subject = subjectId;
                report.add(" " + target.getPosition(), true);
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

            //Report Glancing/Direct Blow here because of Capital Missile weirdness
            if (!(amsBayEngagedCap || pdBayEngagedCap)) {
                addGlancingBlowReports(vPhaseReport);

                if (bDirect) {
                    report = new Report(3189);
                    report.subject = attackingEntity.getId();
                    report.newlines = 0;
                    vPhaseReport.addElement(report);
                }
            }

            CounterAV = getCounterAV();
            //use this if AMS counterfire destroys all the Capital missiles
            if (amsBayEngagedCap && (CapMissileArmor <= 0)) {
                report = new Report(3356);
                report.indent();
                report.subject = subjectId;
                vPhaseReport.addElement(report);
                nDamPerHit = 0;
            }
            //use this if PD counterfire destroys all the Capital missiles
            if (pdBayEngagedCap && (CapMissileArmor <= 0)) {
                report = new Report(3355);
                report.indent();
                report.subject = subjectId;
                vPhaseReport.addElement(report);
                nDamPerHit = 0;
            }

            // Any necessary PSRs, jam checks, etc.
            // If this boolean is true, don't report
            // the miss later, as we already reported
            // it in doChecks
            boolean missReported = doChecks(vPhaseReport);
            if (missReported) {
                bMissed = true;
            }
            if (bMissed && !missReported) {
                reportMiss(vPhaseReport);
            }
            // Handle damage.
            int nCluster = calculateNumCluster();
            int id = vPhaseReport.size();
            int hits = calcHits(vPhaseReport);

            if (target.isAirborne() || game.getBoard().isSpace() || attackingEntity.usesWeaponBays()) {
                // if we added a line to the phase report for calc hits, remove
                // it now
                while (vPhaseReport.size() > id) {
                    vPhaseReport.removeElementAt(vPhaseReport.size() - 1);
                }
                int[] aeroResults = calcAeroDamage(entityTarget, vPhaseReport);
                hits = aeroResults[0];
                // If our capital missile was destroyed, it shouldn't hit
                if ((amsBayEngagedCap || pdBayEngagedCap) && (CapMissileArmor <= 0)) {
                    hits = 0;
                }
                nCluster = aeroResults[1];
            }

            //Capital missiles shouldn't be able to target buildings, being space-only weapons
            // but if they aren't defined, handleEntityDamage() doesn't work.
            int bldgAbsorbs = 0;

            // We have to adjust the reports on a miss, so they line up
            if (bMissed && id != vPhaseReport.size()) {
                vPhaseReport.get(id - 1).newlines--;
                vPhaseReport.get(id).indent(2);
                vPhaseReport.get(vPhaseReport.size() - 1).newlines++;
            }

            // Make sure the player knows when his attack causes no damage.
            if (nDamPerHit == 0) {
                report = new Report(3365);
                report.subject = subjectId;
                vPhaseReport.addElement(report);
                return false;
            }

            if (!bMissed) {
                // for each cluster of hits, do a chunk of damage
                while (hits > 0) {
                    int nDamage;
                    // targeting a hex for igniting
                    if ((target.getTargetType() == Targetable.TYPE_HEX_IGNITE)
                          || (target.getTargetType() == Targetable.TYPE_BLDG_IGNITE)) {
                        handleIgnitionDamage(vPhaseReport, bldg, hits);
                        return false;
                    }
                    // targeting a hex for clearing
                    if (target.getTargetType() == Targetable.TYPE_HEX_CLEAR) {
                        nDamage = nDamPerHit * hits;
                        handleClearDamage(vPhaseReport, bldg, nDamage);
                        return false;
                    }
                    // Targeting a building.
                    if (target.getTargetType() == Targetable.TYPE_BUILDING) {
                        // The building takes the full brunt of the attack.
                        nDamage = nDamPerHit * hits;
                        handleBuildingDamage(vPhaseReport, bldg, nDamage,
                              target.getPosition());
                        // And we're done!
                        return false;
                    }
                    if (entityTarget != null) {
                        handleEntityDamage(entityTarget, vPhaseReport, bldg, hits,
                              nCluster, bldgAbsorbs);
                        gameManager.creditKill(entityTarget, attackingEntity);
                        hits -= nCluster;
                        firstHit = false;
                    }
                } // Handle the next cluster.
            } else { // Hex is targeted, need to report a hit
                report = new Report(3390);
                report.subject = subjectId;
                vPhaseReport.addElement(report);
            }
        }
        Report.addNewline(vPhaseReport);
        return false;
    }

    /**
     * Calculate the attack value based on range
     *
     * @return an <code>int</code> representing the attack value at that range.
     */
    @Override
    protected int calcAttackValue() {
        AmmoType ammoType = ammo.getType();
        int av = 0;
        double counterAV = calcCounterAV();
        int armor = weaponType.getMissileArmor();
        //AR10 munitions
        if (ammoType != null) {
            if (ammoType.getAmmoType() == AmmoType.AmmoTypeEnum.AR10) {
                if (ammoType.hasFlag(AmmoType.F_AR10_KILLER_WHALE)) {
                    av = 4;
                    armor = 40;
                } else if (ammoType.hasFlag(AmmoType.F_AR10_WHITE_SHARK)) {
                    av = 3;
                    armor = 30;
                } else if (ammoType.hasFlag(AmmoType.F_PEACEMAKER)) {
                    av = 1000;
                    armor = 40;
                } else if (ammoType.hasFlag(AmmoType.F_SANTA_ANNA)) {
                    av = 100;
                    armor = 30;
                } else {
                    av = 2;
                    armor = 20;
                }
            } else {
                int range = RangeType.rangeBracket(nRange, weaponType.getATRanges(),
                      true, false);
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
            //Nuclear Warheads for non-AR10 missiles
            if (ammoType.hasFlag(AmmoType.F_SANTA_ANNA)) {
                av = 100;
            } else if (ammoType.hasFlag(AmmoType.F_PEACEMAKER)) {
                av = 1000;
            }
            nukeS2S = ammoType.hasFlag(AmmoType.F_NUCLEAR);
        }
        // For squadrons, total the missile armor for the launched volley
        if (attackingEntity.isCapitalFighter()) {
            armor = armor * numWeapons;
        }
        CapMissileArmor = armor - (int) counterAV;
        CapMissileAMSMod = calcCapMissileAMSMod();

        if (bDirect) {
            av = Math.min(av + (toHit.getMoS() / 3), av * 2);
        }

        av = applyGlancingBlowModifier(av, false);
        av = (int) Math.floor(getBracketingMultiplier() * av);

        return av;
    }

    /**
     * Calculate the damage per hit.
     *
     * @return an <code>int</code> representing the damage dealt per hit.
     */
    @Override
    protected int calcDamagePerHit() {
        AmmoType ammoType = ammo.getType();
        double toReturn = weaponType.getDamage(nRange);

        //AR10 munitions
        if (ammoType != null) {
            if (ammoType.getAmmoType() == AmmoType.AmmoTypeEnum.AR10) {
                if (ammoType.hasFlag(AmmoType.F_AR10_KILLER_WHALE)) {
                    toReturn = 4;
                } else if (ammoType.hasFlag(AmmoType.F_AR10_WHITE_SHARK)) {
                    toReturn = 3;
                } else if (ammoType.hasFlag(AmmoType.F_PEACEMAKER)) {
                    toReturn = 1000;
                } else if (ammoType.hasFlag(AmmoType.F_SANTA_ANNA)) {
                    toReturn = 100;
                } else {
                    toReturn = 2;
                }
            }
            //Nuclear Warheads for non-AR10 missiles
            if (ammoType.hasFlag(AmmoType.F_SANTA_ANNA)) {
                toReturn = 100;
            } else if (ammoType.hasFlag(AmmoType.F_PEACEMAKER)) {
                toReturn = 1000;
            }
            nukeS2S = ammoType.hasFlag(AmmoType.F_NUCLEAR);
        }

        // we default to direct fire weapons for anti-infantry damage
        if (bDirect) {
            toReturn = Math.min(toReturn + (int) floor(toHit.getMoS() / 3.0), toReturn * 2);
        }

        toReturn = applyGlancingBlowModifier(toReturn, false);

        return (int) toReturn;
    }

    @Override
    protected int calcCapMissileAMSMod() {
        CapMissileAMSMod = (int) Math.ceil(CounterAV / 10.0);
        return CapMissileAMSMod;
    }

    @Override
    protected int getCapMisMod() {
        AmmoType ammoType = ammo.getType();
        return getCritMod(ammoType);
    }

    /*
     * get the cap mis mod given a single ammo type
     */
    protected int getCritMod(AmmoType ammoType) {
        if (ammoType == null || ammoType.getAmmoType() == AmmoType.AmmoTypeEnum.PIRANHA
              || ammoType.getAmmoType() == AmmoType.AmmoTypeEnum.AAA_MISSILE
              || ammoType.getAmmoType() == AmmoType.AmmoTypeEnum.ASEW_MISSILE
              || ammoType.getAmmoType() == AmmoType.AmmoTypeEnum.LAA_MISSILE) {
            return 0;
        }
        if (ammoType.getAmmoType() == AmmoType.AmmoTypeEnum.WHITE_SHARK
              || ammoType.getAmmoType() == AmmoType.AmmoTypeEnum.WHITE_SHARK_T
              || ammoType.hasFlag(AmmoType.F_AR10_WHITE_SHARK)
              // Santa Anna, per IO rules
              || ammoType.hasFlag(AmmoType.F_SANTA_ANNA)) {
            return 9;
        } else if (ammoType.getAmmoType() == AmmoType.AmmoTypeEnum.KRAKEN_T
              || ammoType.getAmmoType() == AmmoType.AmmoTypeEnum.KRAKENM
              // Peacemaker, per IO rules
              || ammoType.hasFlag(AmmoType.F_PEACEMAKER)) {
            return 8;
        } else if (ammoType.getAmmoType() == AmmoType.AmmoTypeEnum.KILLER_WHALE
              || ammoType.getAmmoType() == AmmoType.AmmoTypeEnum.KILLER_WHALE_T
              || ammoType.hasFlag(AmmoType.F_AR10_KILLER_WHALE)
              || ammoType.getAmmoType() == AmmoType.AmmoTypeEnum.MANTA_RAY
              || ammoType.getAmmoType() == AmmoType.AmmoTypeEnum.ALAMO) {
            return 10;
        } else if (ammoType.getAmmoType() == AmmoType.AmmoTypeEnum.STINGRAY) {
            return 12;
        } else {
            return 11;
        }
    }

    /**
     * Checks to see if this point defense/AMS bay can engage a capital missile This should return true. Only when
     * handling capital missile attacks can this be false.
     */
    @Override
    protected boolean canEngageCapitalMissile(WeaponMounted counter) {
        return counter.getBayWeapons().size() >= 2;
    }

    /**
     * Sets the appropriate AMS Bay reporting flag depending on what type of missile this is
     */
    @Override
    protected void setAMSBayReportingFlag() {
        amsBayEngagedCap = true;
    }

    /**
     * Sets the appropriate PD Bay reporting flag depending on what type of missile this is
     */
    @Override
    protected void setPDBayReportingFlag() {
        pdBayEngagedCap = true;
    }
}

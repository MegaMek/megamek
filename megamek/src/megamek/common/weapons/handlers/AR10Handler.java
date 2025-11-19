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

import java.io.Serial;
import java.util.Vector;

import megamek.common.Report;
import megamek.common.ToHitData;
import megamek.common.actions.WeaponAttackAction;
import megamek.common.enums.GamePhase;
import megamek.common.equipment.AmmoType;
import megamek.common.game.Game;
import megamek.common.loaders.EntityLoadingException;
import megamek.common.options.OptionsConstants;
import megamek.common.rolls.TargetRoll;
import megamek.common.units.Entity;
import megamek.common.units.IBuilding;
import megamek.common.units.Targetable;
import megamek.server.totalWarfare.TWGameManager;

/**
 * @author Jay Lawson
 */
public class AR10Handler extends AmmoWeaponHandler {

    /**
     *
     */
    @Serial
    private static final long serialVersionUID = -2536312899803153911L;

    /**
     *
     */
    public AR10Handler(ToHitData t, WeaponAttackAction w, Game g, TWGameManager m) throws EntityLoadingException {
        super(t, w, g, m);
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
            Report r = new Report(3115);
            r.indent();
            r.newlines = 0;
            r.subject = subjectId;
            r.add(weaponType.getName() + number);
            if (entityTarget != null) {
                if ((weaponType.getAmmoType() != AmmoType.AmmoTypeEnum.NA)
                      && (weapon.getLinked() != null)
                      && (weapon.getLinked().getType() instanceof AmmoType ammoType)) {
                    if (!ammoType.getMunitionType().contains(AmmoType.Munitions.M_STANDARD)) {
                        r.messageId = 3116;
                        r.add(ammoType.getSubMunitionName());
                    }
                }
                r.addDesc(entityTarget);
            } else {
                r.messageId = 3120;
                r.add(target.getDisplayName(), true);
            }
            vPhaseReport.addElement(r);

            //Point Defense fire vs Capital Missiles

            // are we a glancing hit?  Check for this here, report it later
            setGlancingBlowFlags(entityTarget);

            // Set Margin of Success/Failure and check for Direct Blows
            toHit.setMoS(roll.getIntValue() - Math.max(2, toHit.getValue()));
            bDirect = game.getOptions().booleanOption(OptionsConstants.ADVANCED_COMBAT_TAC_OPS_DIRECT_BLOW)
                  && ((toHit.getMoS() / 3) >= 1) && (entityTarget != null);

            //This has to be up here so that we don't screw up glancing/direct blow reports
            attackValue = calcAttackValue();

            //CalcAttackValue triggers counterfire, so now we can safely get this
            CapMissileAMSMod = getCapMissileAMSMod();

            //Only do this if the missile wasn't destroyed
            if (CapMissileAMSMod > 0 && CapMissileArmor > 0) {
                toHit.addModifier(CapMissileAMSMod, "Damage from Point Defenses");
                if (roll.getIntValue() < toHit.getValue()) {
                    CapMissileMissed = true;
                }
            }

            // Report any AMS bay action against Capital missiles that doesn't destroy them all.
            if (amsBayEngagedCap && CapMissileArmor > 0) {
                r = new Report(3358);
                r.add(CapMissileAMSMod);
                r.subject = subjectId;
                vPhaseReport.addElement(r);

                // Report any PD bay action against Capital missiles that doesn't destroy them all.
            } else if (pdBayEngagedCap && CapMissileArmor > 0) {
                r = new Report(3357);
                r.add(CapMissileAMSMod);
                r.subject = subjectId;
                vPhaseReport.addElement(r);
            }

            if (toHit.getValue() == TargetRoll.IMPOSSIBLE) {
                r = new Report(3135);
                r.subject = subjectId;
                r.add(" " + target.getPosition(), true);
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

            //Report Glancing/Direct Blow here because of Capital Missile weirdness
            if (!(amsBayEngagedCap || pdBayEngagedCap)) {
                addGlancingBlowReports(vPhaseReport);

                if (bDirect) {
                    r = new Report(3189);
                    r.subject = attackingEntity.getId();
                    r.newlines = 0;
                    vPhaseReport.addElement(r);
                }
            }

            CounterAV = getCounterAV();
            //use this if AMS counterfire destroys all the Capital missiles
            if (amsBayEngagedCap && (CapMissileArmor <= 0)) {
                r = new Report(3356);
                r.indent();
                r.subject = subjectId;
                vPhaseReport.addElement(r);
            }
            //use this if PD counterfire destroys all the Capital missiles
            if (pdBayEngagedCap && (CapMissileArmor <= 0)) {
                r = new Report(3355);
                r.indent();
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
                r = new Report(3365);
                r.subject = subjectId;
                vPhaseReport.addElement(r);
                return false;
            }
            if (!bMissed && (entityTarget != null)) {
                handleEntityDamage(entityTarget, vPhaseReport, bldg, hits,
                      nCluster, bldgAbsorbs);
                gameManager.creditKill(entityTarget, attackingEntity);
            } else if (!bMissed) { // Hex is targeted, need to report a hit
                r = new Report(3390);
                r.subject = subjectId;
                vPhaseReport.addElement(r);
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
        int av;
        AmmoType ammoType = ammo.getType();
        if (ammoType.hasFlag(AmmoType.F_AR10_KILLER_WHALE)) {
            av = 4;
        } else if (ammoType.hasFlag(AmmoType.F_AR10_WHITE_SHARK)) {
            av = 3;
        } else {
            av = 2;
        }
        if (bDirect) {
            av = Math.min(av + (toHit.getMoS() / 3), av * 2);
        }
        av = applyGlancingBlowModifier(av, false);
        av = (int) Math.floor(getBracketingMultiplier() * av);
        return av;
    }

    @Override
    protected int getCapMisMod() {
        int mod;
        AmmoType ammoType = ammo.getType();
        if (ammoType.hasFlag(AmmoType.F_AR10_KILLER_WHALE)) {
            mod = 10;
        } else if (ammoType.hasFlag(AmmoType.F_AR10_WHITE_SHARK)) {
            mod = 9;
        } else {
            mod = 11;
        }
        return mod;
    }

}

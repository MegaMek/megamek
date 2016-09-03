/**
 * MegaMek - Copyright (C) 2005 Ben Mazur (bmazur@sev.org)
 *
 *  This program is free software; you can redistribute it and/or modify it
 *  under the terms of the GNU General Public License as published by the Free
 *  Software Foundation; either version 2 of the License, or (at your option)
 *  any later version.
 *
 *  This program is distributed in the hope that it will be useful, but
 *  WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 *  or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License
 *  for more details.
 */
/*
 * Created on Sept 5, 2005
 *
 */
package megamek.common.weapons;

import java.util.Vector;

import megamek.common.BattleArmor;
import megamek.common.Compute;
import megamek.common.CriticalSlot;
import megamek.common.Entity;
import megamek.common.HitData;
import megamek.common.IGame;
import megamek.common.Infantry;
import megamek.common.Mounted;
import megamek.common.RangeType;
import megamek.common.Report;
import megamek.common.ToHitData;
import megamek.common.actions.WeaponAttackAction;
import megamek.common.options.OptionsConstants;
import megamek.server.Server;
import megamek.server.Server.DamageType;

/**
 * @author Sebastian Brocks
 */
public class PPCHandler extends EnergyWeaponHandler {
    /**
     *
     */
    private static final long serialVersionUID = 5545991061428671743L;
    private int chargedCapacitor = 0;

    /**
     * @param t
     * @param w
     * @param g
     * @param s
     */
    public PPCHandler(ToHitData t, WeaponAttackAction w, IGame g, Server s) {
        super(t, w, g, s);
        // remember capacitor state and turn it off here,
        // so a crit in the firing phase does not cause an explosion, per the
        // rules in TO
        if (weapon.hasChargedCapacitor() != 0) {
            if (weapon.hasChargedCapacitor() == 2) {
                chargedCapacitor = 2;
                weapon.getCrossLinkedBy().setMode("Off");
            }
            if (weapon.hasChargedCapacitor() == 1) {
                chargedCapacitor = 1;
                weapon.getLinkedBy().setMode("Off");
            }
        }
    }

    /*
     * (non-Javadoc)
     * 
     * @see megamek.common.weapons.EnergyWeaponHandler#calcDamagePerHit()
     */
    @Override
    protected int calcDamagePerHit() {
        float toReturn = wtype.getDamage(nRange);

        if (game.getOptions().booleanOption(OptionsConstants.ADVCOMBAT_TACOPS_ENERGY_WEAPONS)
                && wtype.hasModes()) {
            toReturn = Compute.dialDownDamage(weapon, wtype, nRange);
        }

        if (chargedCapacitor != 0) {
            toReturn += 5;
        }
        // during a swarm, all damage gets applied as one block to one location
        if ((ae instanceof BattleArmor)
                && (weapon.getLocation() == BattleArmor.LOC_SQUAD)
                && !(weapon.isSquadSupportWeapon())
                && (ae.getSwarmTargetId() == target.getTargetId())) {
            toReturn *= ((BattleArmor) ae).getShootingStrength();
        }

        // Check for Altered Damage from Energy Weapons (TacOps, pg.83)
        if (game.getOptions().booleanOption(OptionsConstants.ADVCOMBAT_TACOPS_ALTDMG)) {
            if (nRange <= 1) {
                toReturn++;
            } else if (nRange <= wtype.getMediumRange()) {
                // Do Nothing for Short and Medium Range
            } else if (nRange <= wtype.getLongRange()) {
                toReturn--;
            }
        }

        if (game.getOptions().booleanOption(OptionsConstants.ADVCOMBAT_TACOPS_RANGE)
                && (nRange > wtype.getRanges(weapon)[RangeType.RANGE_LONG])) {
            toReturn -= 1;
        }
        if (game.getOptions().booleanOption(
                OptionsConstants.ADVCOMBAT_TACOPS_LOS_RANGE)
                && (nRange > wtype.getRanges(weapon)[RangeType.RANGE_EXTREME])) {
            toReturn = (int) Math.floor(toReturn * .75);
        }

        if ((target instanceof Entity)
                && ((Entity) target).hasActiveBlueShield()) {
            toReturn = (int) Math.max(Math.floor(toReturn / 2.0), 1);
        }

        if ((target instanceof Infantry) && !(target instanceof BattleArmor)) {
            toReturn = Compute.directBlowInfantryDamage(toReturn,
                    bDirect ? toHit.getMoS() / 3 : 0,
                    wtype.getInfantryDamageClass(),
                    ((Infantry) target).isMechanized(),
                    toHit.getThruBldg() != null, ae.getId(), calcDmgPerHitReport);
        } else if (bDirect) {
            toReturn = Math.min(toReturn + (toHit.getMoS() / 3), toReturn * 2);
        }
        if (bGlancing) {
            toReturn = (int) Math.floor(toReturn / 2.0);
        }

        return (int) Math.ceil(toReturn);
    }

    /*
     * (non-Javadoc)
     * 
     * @see megamek.common.weapons.WeaponHandler#doChecks(java.util.Vector)
     */
    @Override
    protected boolean doChecks(Vector<Report> vPhaseReport) {
        // Resolve roll for disengaged field inhibitors on PPCs, if needed
        if (game.getOptions().booleanOption(OptionsConstants.ADVCOMBAT_TACOPS_PPC_INHIBITORS)
                && wtype.hasModes()
                && weapon.curMode().equals("Field Inhibitor OFF")) {
            int rollTarget = 0;
            int dieRoll = Compute.d6(2);
            int distance = Compute.effectiveDistance(game, ae, target);

            if (distance >= 3) {
                rollTarget = 3;
            } else if (distance == 2) {
                rollTarget = 6;
            } else if (distance == 1) {
                rollTarget = 10;
            }
            // roll to avoid damage
            Report.addNewline(vPhaseReport);
            Report r = new Report(3175);
            r.subject = subjectId;
            r.indent(2);
            vPhaseReport.addElement(r);
            r = new Report(3180);
            r.subject = subjectId;
            r.indent();
            r.add(rollTarget);
            r.add(dieRoll);
            if (dieRoll < rollTarget) {
                // Oops, we ruined our day...
                int wlocation = weapon.getLocation();
                weapon.setHit(true);
                // Destroy the first unmarked critical for the PPC
                for (int i = 0; i < ae.getNumberOfCriticals(wlocation); i++) {
                    CriticalSlot slot1 = ae.getCritical(wlocation, i);
                    if ((slot1 == null)
                            || (slot1.getType() == CriticalSlot.TYPE_SYSTEM)
                            || slot1.isHit()) {
                        continue;
                    }
                    Mounted mounted = slot1.getMount();
                    if (mounted.equals(weapon)) {
                        slot1.setHit(true);
                        break;
                    }
                }
                // Bug 1066147 : damage is *not* like an ammo explosion,
                // but it *does* get applied directly to the IS.
                r.choose(false);
                r.indent(2);
                vPhaseReport.addElement(r);
                Vector<Report> newReports = server.damageEntity(ae,
                        new HitData(wlocation), 10, false, DamageType.NONE,
                        true);
                for (Report rep : newReports) {
                    rep.indent(2);
                }
                vPhaseReport.addAll(newReports);
                // Deal 2 damage to the pilot
                vPhaseReport.addAll(server.damageCrew(ae, 2));
                r = new Report(3185);
                r.subject = subjectId;
                vPhaseReport.addElement(r);
            } else {
                r.choose(true);
                vPhaseReport.addElement(r);
            }
        }
        // resolve roll for charged capacitor
        if (chargedCapacitor != 0) {
            if (roll == 2) {
                Report r = new Report(3178);
                r.subject = ae.getId();
                r.indent();
                vPhaseReport.add(r);
                // Oops, we ruined our day...
                int wlocation = weapon.getLocation();
                weapon.setHit(true);
                for (int i = 0; i < ae.getNumberOfCriticals(wlocation); i++) {
                    CriticalSlot slot = ae.getCritical(wlocation, i);
                    if ((slot == null)
                            || (slot.getType() == CriticalSlot.TYPE_SYSTEM)) {
                        continue;
                    }
                    // Only one Crit needs to be damaged.
                    Mounted mounted = slot.getMount();
                    if (mounted.equals(weapon)) {
                        slot.setDestroyed(true);
                        break;
                    }
                }
            }
        }
        return false;
    }
}

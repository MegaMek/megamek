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
package megamek.common.weapons;

import java.util.Vector;

import megamek.common.Aero;
import megamek.common.BattleArmor;
import megamek.common.Building;
import megamek.common.Compute;
import megamek.common.Coords;
import megamek.common.Entity;
import megamek.common.EquipmentType;
import megamek.common.HitData;
import megamek.common.IGame;
import megamek.common.Infantry;
import megamek.common.Mech;
import megamek.common.RangeType;
import megamek.common.Report;
import megamek.common.TargetRoll;
import megamek.common.ToHitData;
import megamek.common.actions.WeaponAttackAction;
import megamek.common.options.OptionsConstants;
import megamek.server.Server;

public class PlasmaRifleHandler extends AmmoWeaponHandler {
    /**
     *
     */
    private static final long serialVersionUID = -2092721653693187140L;

    /**
     * @param toHit
     * @param waa
     * @param g
     */
    public PlasmaRifleHandler(ToHitData toHit, WeaponAttackAction waa, IGame g,
            Server s) {
        super(toHit, waa, g, s);
        generalDamageType = HitData.DAMAGE_ENERGY;

    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * megamek.common.weapons.WeaponHandler#handleEntityDamage(megamek.common
     * .Entity, java.util.Vector, megamek.common.Building, int, int, int, int)
     */
    @Override
    protected void handleEntityDamage(Entity entityTarget,
            Vector<Report> vPhaseReport, Building bldg, int hits, int nCluster,
            int bldgAbsorbs) {
        super.handleEntityDamage(entityTarget, vPhaseReport, bldg, hits,
                nCluster, bldgAbsorbs);
        if (!missed
                && ((entityTarget instanceof Mech) || (entityTarget instanceof Aero))) {
            Report r = new Report(3400);
            r.subject = subjectId;
            r.indent(2);
            int extraHeat = 0;
            // if this is a fighter squadron, we need to account for number of
            // weapons
            // should default to one for non squadrons
            for (int i = 0; i < nweaponsHit; i++) {
                extraHeat += Compute.d6();
            }
            if (entityTarget.getArmor(hit) > 0
                    && (entityTarget.getArmorType(hit.getLocation()) == EquipmentType.T_ARMOR_REFLECTIVE)) {
                entityTarget.heatFromExternal += Math.max(1, extraHeat / 2);
                r.add(Math.max(1, extraHeat / 2));
                r.choose(true);
                r.messageId = 3406;
                r.add(extraHeat);
                r.add(EquipmentType.armorNames[entityTarget.getArmorType(hit
                        .getLocation())]);
            } else if (entityTarget.getArmor(hit) > 0
                    && (entityTarget.getArmorType(hit.getLocation()) == EquipmentType.T_ARMOR_HEAT_DISSIPATING)) {
                entityTarget.heatFromExternal += extraHeat / 2;
                r.add(extraHeat / 2);
                r.choose(true);
                r.messageId = 3406;
                r.add(extraHeat);
                r.add(EquipmentType.armorNames[entityTarget.getArmorType(hit
                        .getLocation())]);
            } else {
                entityTarget.heatFromExternal += extraHeat;
                r.add(extraHeat);
                r.choose(true);
            }
            vPhaseReport.addElement(r);
        }
    }

    /*
     * (non-Javadoc)
     * 
     * @see megamek.common.weapons.WeaponHandler#calcDamagePerHit()
     */
    @Override
    protected int calcDamagePerHit() {
        if ((target instanceof Mech) || (target instanceof Aero)) {
            int toReturn = 10;
            if (bGlancing) {
                toReturn = (int) Math.floor(toReturn / 2.0);
            }
            if (game.getOptions().booleanOption(
                    OptionsConstants.ADVCOMBAT_TACOPS_RANGE)
                    && (nRange > wtype.getRanges(weapon)[RangeType.RANGE_LONG])) {
                toReturn -= 1;
            }
            if (game.getOptions().booleanOption(
                    OptionsConstants.ADVCOMBAT_TACOPS_LOS_RANGE)
                    && (nRange > wtype.getRanges(weapon)[RangeType.RANGE_EXTREME])) {
                toReturn = (int) Math.floor(toReturn / 2.0);
            }
            return toReturn;
        }
        return 1;
    }

    /*
     * (non-Javadoc)
     * 
     * @see megamek.common.weapons.WeaponHandler#calcnCluster()
     */
    @Override
    protected int calcnCluster() {
        if ((target instanceof Mech) || (target instanceof Aero)) {
            bSalvo = false;
            return 1;
        }
        int toReturn = 5;
        if ((target instanceof Infantry) && !(target instanceof BattleArmor)) {
            toReturn = Compute.d6(2);
        }
        bSalvo = true;
        // pain shunted infantry get half damage
        if ((target instanceof Infantry)
                && ((Entity) target).getCrew().getOptions()
                        .booleanOption(OptionsConstants.MD_PAIN_SHUNT)) {
            toReturn = Math.max(toReturn / 2, 1);
        }
        return toReturn;
    }

    /*
     * (non-Javadoc)
     * 
     * @see megamek.common.weapons.WeaponHandler#calcHits(java.util.Vector)
     */
    @Override
    protected int calcHits(Vector<Report> vPhaseReport) {
        int toReturn;
        // against mechs, 1 hit with 10 damage, plus heat
        if ((target instanceof Mech) || (target instanceof Aero)) {
            toReturn = 1;
            // otherwise, 10+2d6 damage
            // but fireresistant BA armor gets no damage from heat, and half the
            // normal one, so only 5 damage
        } else {
            if ((target instanceof BattleArmor)
                    && ((BattleArmor) target).isFireResistant()) {
                toReturn = 5;
            } else {
                toReturn = 10 + Compute.d6(2);
            }
            if (bGlancing) {
                toReturn = (int) Math.floor(toReturn / 2.0);
            }
        }
        return toReturn;
    }

    @Override
    protected void handleIgnitionDamage(Vector<Report> vPhaseReport,
            Building bldg, int hits) {
        if (!bSalvo) {
            // hits!
            Report r = new Report(2270);
            r.subject = subjectId;
            r.newlines = 0;
            vPhaseReport.addElement(r);
        }
        TargetRoll tn = new TargetRoll(wtype.getFireTN(), wtype.getName());
        if (tn.getValue() != TargetRoll.IMPOSSIBLE) {
            Report.addNewline(vPhaseReport);
            server.tryIgniteHex(target.getPosition(), subjectId, true, false,
                    tn, true, -1, vPhaseReport);
        }
    }

    @Override
    protected void handleClearDamage(Vector<Report> vPhaseReport,
            Building bldg, int nDamage) {
        if (!bSalvo) {
            // hits!
            Report r = new Report(2270);
            r.subject = subjectId;
            vPhaseReport.addElement(r);
        }

        nDamage *= 2; // Plasma weapons deal double damage to woods.

        // report that damage was "applied" to terrain
        Report r = new Report(3385);
        r.indent(2);
        r.subject = subjectId;
        r.add(nDamage);
        vPhaseReport.addElement(r);

        // Any clear attempt can result in accidental ignition, even
        // weapons that can't normally start fires. that's weird.
        // Buildings can't be accidentally ignited.
        // TODO: change this for TacOps - now you roll another 2d6 first and on
        // a 5 or less
        // you do a normal ignition as though for intentional fires
        if ((bldg != null)
                && server.tryIgniteHex(target.getPosition(), subjectId, true,
                        false,
                        new TargetRoll(wtype.getFireTN(), wtype.getName()), 5,
                        vPhaseReport)) {
            return;
        }
        Vector<Report> clearReports = server.tryClearHex(target.getPosition(),
                nDamage, subjectId);
        if (clearReports.size() > 0) {
            vPhaseReport.lastElement().newlines = 0;
        }
        vPhaseReport.addAll(clearReports);
        return;
    }

    @Override
    protected void handleBuildingDamage(Vector<Report> vPhaseReport,
            Building bldg, int nDamage, Coords coords) {
        // Plasma weapons deal double damage to buildings.
        super.handleBuildingDamage(vPhaseReport, bldg, nDamage * 2, coords);
    }

}

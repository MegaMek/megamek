/*
 * MegaMek - Copyright (C) 2005 Ben Mazur (bmazur@sev.org)
 *
 * This program is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License as published by the Free
 * Software Foundation; either version 2 of the License, or (at your option)
 * any later version.
 *
 * This program is distributed in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 * or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License
 * for more details.
 */
package megamek.common.weapons;

import megamek.common.*;
import megamek.common.actions.WeaponAttackAction;
import megamek.common.options.OptionsConstants;
import megamek.server.GameManager;
import org.apache.logging.log4j.LogManager;

import javax.swing.*;
import java.util.Vector;

/**
 * @author Sebastian Brocks
 */
public class StreakHandler extends MissileWeaponHandler {
    private static final long serialVersionUID = 4122111574368642492L;
    boolean isAngelECMAffected = ComputeECM.isAffectedByAngelECM(ae, ae.getPosition(), target.getPosition());

    /**
     * @param t
     * @param w
     * @param g
     * @param m
     */
    public StreakHandler(ToHitData t, WeaponAttackAction w, Game g, GameManager m) {
        super(t, w, g, m);
    }

    @Override
    protected int calcDamagePerHit() {
        if (target.isConventionalInfantry()) {
            return Compute.directBlowInfantryDamage(
                    wtype.getRackSize() * 2, bDirect ? toHit.getMoS() / 3 : 0,
                    wtype.getInfantryDamageClass(),
                    ((Infantry) target).isMechanized(),
                    toHit.getThruBldg() != null, ae.getId(), calcDmgPerHitReport);
        }
        return 2;
    }

    @Override
    protected int calcnCluster() {
        return 1;
    }

    @Override
    protected int calcHits(Vector<Report> vPhaseReport) {
        // conventional infantry gets hit in one lump
        // BAs do one lump of damage per BA suit
        if (target.isConventionalInfantry()) {
            if (ae instanceof BattleArmor) {
                bSalvo = true;
                return ((BattleArmor) ae).getShootingStrength();
            }
            return 1;
        }
        // no AMS when streak misses
        if (bMissed) {
            return 0;
        }
        int nMissilesModifier = getClusterModifiers(true);

        int missilesHit;
        int amsMod = getAMSHitsMod(vPhaseReport);
        
        if (game.getOptions().booleanOption(OptionsConstants.ADVAERORULES_AERO_SANITY)) {
            Entity entityTarget = (target.getTargetType() == Targetable.TYPE_ENTITY) ? (Entity) target
                    : null;
            if (entityTarget != null && entityTarget.isLargeCraft()) {
                amsMod = (int) -getAeroSanityAMSHitsMod();
            }
        }

        if (amsMod == 0 && allShotsHit()) {
            missilesHit = wtype.getRackSize();
        } else {
            missilesHit = Compute.missilesHit(wtype.getRackSize(), amsMod+nMissilesModifier,
                    weapon.isHotLoaded(), allShotsHit(), isAdvancedAMS());
            if (amsMod != 0) {
                Report r;
                if (amsMod > 0) {
                    r = new Report(3340);
                } else {
                    r = new Report(3341);
                }
                r.subject = subjectId;
                r.add(amsMod);
                r.newlines = 0;
                vPhaseReport.addElement(r);
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
        }
        Report r = new Report(3345);
        r.subject = subjectId;
        vPhaseReport.addElement(r);
        bSalvo = true;
        return missilesHit;
    }

    @Override
    protected void useAmmo() {
        checkAmmo();
        if (ammo == null) {
            final String message = "Handler can't find any ammo! This should be impossible!";
            LogManager.getLogger().error(message, new Exception());
            JOptionPane.showMessageDialog(null, message, "Unknown Ammo Exception",
                    JOptionPane.ERROR_MESSAGE);
        }

        if (ammo.getUsableShotsLeft() <= 0) {
            ae.loadWeaponWithSameAmmo(weapon);
            ammo = weapon.getLinked();
        }

        if (roll.getIntValue() >= toHit.getValue()) {
            ammo.setShotsLeft(ammo.getBaseShotsLeft() - 1);
            if (wtype.hasFlag(WeaponType.F_ONESHOT)) {
                weapon.setFired(true);
            }
            setDone();
        }
    }

    @Override
    protected void reportMiss(Vector<Report> vPhaseReport) {
        Report r = new Report(3215);
        r.subject = subjectId;
        vPhaseReport.addElement(r);
    }

    @Override
    protected void addHeat() {
        if ((toHit.getValue() != TargetRoll.IMPOSSIBLE) && (roll.getIntValue() >= toHit.getValue())) {
            super.addHeat();
        }
    }

    @Override
    protected boolean allShotsHit() {
        return super.allShotsHit() || !isAngelECMAffected;
    }

    @Override
    protected boolean handleSpecialMiss(Entity entityTarget, boolean bldgDamagedOnMiss,
                                        Building bldg, Vector<Report> vPhaseReport) {
        return false;
    }
}

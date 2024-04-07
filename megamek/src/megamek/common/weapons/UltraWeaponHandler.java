/*
 * Copyright (c) 2004 - Ben Mazur (bmazur@sev.org).
 * Copyright (c) 2022 - The MegaMek Team. All Rights Reserved.
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

import java.util.Vector;

import megamek.common.AmmoType;
import megamek.common.Compute;
import megamek.common.Entity;
import megamek.common.Game;
import megamek.common.Infantry;
import megamek.common.RangeType;
import megamek.common.Report;
import megamek.common.ToHitData;
import megamek.common.WeaponType;
import megamek.common.actions.WeaponAttackAction;
import megamek.common.options.OptionsConstants;
import megamek.server.GameManager;
import megamek.server.Server;

/**
 * @author Andrew Hunter
 * @since Sept 29, 2004
 */
public class UltraWeaponHandler extends AmmoWeaponHandler {
    private static final long serialVersionUID = 7551194199079004134L;
    int howManyShots;
    private final boolean twoRollsUltra; // Tracks whether this is an

    // ultra AC using the unofficial "two rolls" rule. Can be final because
    // this isn't really going to change over the course of a game.

    /**
     * @param t
     * @param w
     * @param g
     */
    public UltraWeaponHandler(ToHitData t, WeaponAttackAction w, Game g, GameManager m) {
        super(t, w, g, m);
        twoRollsUltra = game.getOptions().booleanOption(OptionsConstants.ADVCOMBAT_UAC_TWOROLLS)
                && ((wtype.getAmmoType() == AmmoType.T_AC_ULTRA)
                        || (wtype.getAmmoType() == AmmoType.T_AC_ULTRA_THB));
    }

    /*
     * (non-Javadoc)
     *
     * @see megamek.common.weapons.WeaponHandler#addHeatUseAmmo()
     */
    @Override
    protected void useAmmo() {
        setDone();
        checkAmmo();
        howManyShots = (weapon.curMode().equals(Weapon.MODE_AC_SINGLE) ? 1 : 2);
        int total = ae.getTotalAmmoOfType(ammo.getType());
        if (total > 1 ) {
            // No need to change howManyShots
        } else if (total == 1) {
            howManyShots = 1;
        } else {
            howManyShots = 0;
        }

        // Handle bins that are empty or will be emptied by this attack
        attemptToReloadWeapon();
        reduceShotsLeft(howManyShots);
    }

    protected void attemptToReloadWeapon() {
        // We _may_ be able to reload from another ammo source, but in case
        // a previous attack burned through all the ammo, this attack may be SOL.
        if (ammo.getUsableShotsLeft() == 0) {
            ae.loadWeapon(weapon);
            ammo = weapon.getLinked();
        }
    }

    protected void reduceShotsLeft(int shotsNeedFiring) {
        while (shotsNeedFiring > ammo.getUsableShotsLeft()) {
            shotsNeedFiring -= ammo.getBaseShotsLeft();
            ammo.setShotsLeft(0);
            attemptToReloadWeapon();
        }
        ammo.setShotsLeft(ammo.getBaseShotsLeft() - shotsNeedFiring);
    }

    /*
     * (non-Javadoc)
     *
     * @see megamek.common.weapons.WeaponHandler#calcHits(java.util.Vector)
     */
    @Override
    protected int calcHits(Vector<Report> vPhaseReport) {
        // conventional infantry gets hit in one lump BAs can't mount UACS/RACs
        if (target.isConventionalInfantry()) {
            return 1;
        }

        if (howManyShots == 1 || twoRollsUltra) {
            return 1;
        }

        bSalvo = true;

        int nMod = getClusterModifiers(true);

        int shotsHit = allShotsHit() ? howManyShots : Compute.missilesHit(howManyShots, nMod);

        // report number of shots that hit only when weapon doesn't jam
        if (!weapon.isJammed()) {
            Report r = new Report(3325);
            r.subject = subjectId;
            r.add(shotsHit);
            r.add(sSalvoType);
            r.add(toHit.getTableDesc());
            r.newlines = 0;
            vPhaseReport.addElement(r);
            if (nMod != 0) {
                if (nMod > 0) {
                    r = new Report(3340);
                } else {
                    r = new Report(3341);
                }
                r.subject = subjectId;
                r.add(nMod);
                r.newlines = 0;
                vPhaseReport.addElement(r);
            }
            r = new Report(3345);
            r.subject = subjectId;
            vPhaseReport.addElement(r);
        }
        return shotsHit;
    }

    /*
     * (non-Javadoc)
     *
     * @see megamek.common.weapons.WeaponHandler#doChecks(java.util.Vector)
     */
    @Override
    protected boolean doChecks(Vector<Report> vPhaseReport) {
        if (super.doChecks(vPhaseReport)) {
            return true;
        }

        if ((roll.getIntValue() == 2) && (howManyShots == 2) && !(ae instanceof Infantry)) {
            Report r = new Report();
            r.subject = subjectId;
            weapon.setJammed(true);
            isJammed = true;
            if ((wtype.getAmmoType() == AmmoType.T_AC_ULTRA)
                    || (wtype.getAmmoType() == AmmoType.T_AC_ULTRA_THB)) {
                r.messageId = 3160;
            } else {
                r.messageId = 3170;
            }
            vPhaseReport.addElement(r);
        }
        return false;
    }

    /*
     * (non-Javadoc)
     *
     * @see megamek.common.weapons.WeaponHandler#calcDamagePerHit()
     */
    @Override
    protected int calcDamagePerHit() {
        double toReturn = wtype.getDamage();
        // infantry get hit by all shots
        if (target.isConventionalInfantry()) {
            if (howManyShots > 1) { // Is this a cluster attack?
                // Compute maximum damage potential for cluster weapons
                toReturn = howManyShots * wtype.getDamage();
                toReturn = Compute.directBlowInfantryDamage(toReturn,
                        bDirect ? toHit.getMoS() / 3 : 0,
                        WeaponType.WEAPON_CLUSTER_BALLISTIC, // treat as cluster
                        ((Infantry) target).isMechanized(),
                        toHit.getThruBldg() != null, ae.getId(),
                        calcDmgPerHitReport);
            } else { // No - only one shot fired
                toReturn = Compute.directBlowInfantryDamage(wtype.getDamage(),
                        bDirect ? toHit.getMoS() / 3 : 0,
                        wtype.getInfantryDamageClass(),
                        ((Infantry) target).isMechanized(),
                        toHit.getThruBldg() != null, ae.getId(),
                        calcDmgPerHitReport);
            }
        // Cluster bonuses or penalties can't apply to "two rolls" UACs, so
        // if we have one, modify the damage per hit directly.
        } else if (bDirect && (howManyShots == 1 || twoRollsUltra)) {
            toReturn = Math.min(toReturn + (toHit.getMoS() / 3), toReturn * 2);
        }

        if (howManyShots == 1 || twoRollsUltra) {
            toReturn = applyGlancingBlowModifier(toReturn, false);
        }

        if (game.getOptions().booleanOption(OptionsConstants.ADVCOMBAT_TACOPS_RANGE)
                && (nRange > wtype.getRanges(weapon)[RangeType.RANGE_LONG])) {
            toReturn = (int) Math.floor(toReturn * .75);
        }
        if (game.getOptions().booleanOption(OptionsConstants.ADVCOMBAT_TACOPS_LOS_RANGE)
                && (nRange > wtype.getRanges(weapon)[RangeType.RANGE_EXTREME])) {
            toReturn = (int) Math.floor(toReturn * .5);
        }
        return (int) toReturn;
    }

    @Override
    protected boolean usesClusterTable() {
        return !game.getOptions().booleanOption(OptionsConstants.ADVCOMBAT_UAC_TWOROLLS);
    }

    @Override
    protected int calcnClusterAero(Entity entityTarget) {
        if (usesClusterTable() && !ae.isCapitalFighter() && (entityTarget != null)
                && !entityTarget.isCapitalScale()) {
            return (int) Math.ceil(attackValue / 2.0);
        } else {
            return 1;
        }
    }
}

/**
 * MegaMek - Copyright (C) 2004,2005 Ben Mazur (bmazur@sev.org)
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
 * Created on Sep 24, 2004
 *
 */
package megamek.common.weapons.infantry;

import java.util.Vector;

import megamek.common.BattleArmor;
import megamek.common.Building;
import megamek.common.Compute;
import megamek.common.Entity;
import megamek.common.IGame;
import megamek.common.Infantry;
import megamek.common.Report;
import megamek.common.ToHitData;
import megamek.common.WeaponType;
import megamek.common.actions.WeaponAttackAction;
import megamek.common.options.OptionsConstants;
import megamek.common.weapons.WeaponHandler;
import megamek.server.Server;
import megamek.server.Server.DamageType;

/**
 * @author Sebastian Brocks
 */
public class InfantryWeaponHandler extends WeaponHandler {

    /**
     *
     */
    private static final long serialVersionUID = 1425176802065536326L;

    /**
     * @param t
     * @param w
     * @param g
     */
    public InfantryWeaponHandler(ToHitData t, WeaponAttackAction w, IGame g,
            Server s) {
        super(t, w, g, s);
        bSalvo = true;
    }

    /*
     * (non-Javadoc)
     *
     * @see megamek.common.weapons.WeaponHandler#calcDamagePerHit()
     */
    @Override
    protected int calcDamagePerHit() {
        return 1;
    }

    /*
     * (non-Javadoc)
     *
     * @see megamek.common.weapons.WeaponHandler#calcnCluster()
     */
    @Override
    protected int calcnCluster() {
        return 2;
    }

    /*
     * (non-Javadoc)
     *
     * @see megamek.common.weapons.WeaponHandler#calcHits(java.util.Vector)
     */
    @Override
    protected int calcHits(Vector<Report> vPhaseReport) {
        int nHitMod = 0;
        if (bGlancing) {
            nHitMod -= 4;
        }
        int troopersHit = 0;
        //when swarming all troopers hit
        if (ae.getSwarmTargetId() == target.getTargetId()) {
            troopersHit = ((Infantry)ae).getShootingStrength();
        } else if (!(ae instanceof Infantry)) {
            troopersHit = 1;
        } else {
            troopersHit = Compute.missilesHit(((Infantry) ae)
                .getShootingStrength(), nHitMod, bGlancing);
        }
        double damage = ((InfantryWeapon)wtype).getInfantryDamage();
        if((ae instanceof Infantry) && !(ae instanceof BattleArmor)) {
            //for conventional infantry, we have to calculate primary and secondary weapons
            //to get damage per trooper
            damage = ((Infantry)ae).getDamagePerTrooper();
        }
        if((ae instanceof Infantry)
                && nRange == 0
                && ae.getCrew().getOptions().booleanOption(OptionsConstants.MD_TSM_IMPLANT)) {
            damage += 0.14;
        }
        int damageDealt = (int) Math.round(damage * troopersHit);
        if((target instanceof Infantry) && !(target instanceof BattleArmor) && wtype.hasFlag(WeaponType.F_INF_BURST)) {
            damageDealt += Compute.d6();
        }
        if((ae instanceof Infantry)
                && nRange == 0
                && ae.getCrew().getOptions().booleanOption(OptionsConstants.MD_TSM_IMPLANT)) {

        }
        if ((target instanceof Infantry) && ((Infantry)target).isMechanized()) {
            damageDealt /= 2;
        }
        // this doesn't work...
        if ((target instanceof Building) && (wtype.hasFlag(WeaponType.F_INF_NONPENETRATING))) {
            damageDealt = 0;
        }
        if (wtype.hasFlag(WeaponType.F_INF_NONPENETRATING)) {
            damageType = DamageType.NONPENETRATING;
        }
        Report r = new Report(3325);
        r.subject = subjectId;
        if (ae instanceof Infantry) {
            r.add(troopersHit);
            r.add(" troopers ");
        } else { // Needed for support tanks with infantry weapons
            r.add("");
            r.add("");
        }
        r.add(toHit.getTableDesc() + ", causing " + damageDealt
                + " damage.");
        r.newlines = 0;
        vPhaseReport.addElement(r);
        if((target instanceof Infantry) && !(target instanceof BattleArmor)) {
            //this is a little strange, but I can't just do this in calcDamagePerHit because
            //that is called up before misses are determined and will lead to weird reporting
            nDamPerHit = damageDealt;
            return 1;
        }
        return damageDealt;
    }

    //we need to figure out AV damage to aeros for AA weapons
    protected int calcnClusterAero(Entity entityTarget) {
        return 5;
    }

    protected int calcAttackValue() {
        int av = 0;
        //Sigh, another rules oversight - nobody bothered to figure this out
        //To be consistent with other cluster weapons we will assume 60% hit
        double damage = ((InfantryWeapon)wtype).getInfantryDamage();
        if((ae instanceof Infantry) && !(ae instanceof BattleArmor)) {
            damage = ((Infantry)ae).getDamagePerTrooper();
            av = (int) Math.round(damage * 0.6 * ((Infantry)ae).getShootingStrength());
        }
        if(bDirect) {
            av = Math.min(av+(toHit.getMoS()/3), av*2);
        }
        if(bGlancing) {
            av = (int) Math.floor(av / 2.0);
        }
        return av;
    }

}

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

import java.math.BigInteger;
import java.util.Vector;

import megamek.common.Aero;
import megamek.common.BattleArmor;
import megamek.common.Building;
import megamek.common.Compute;
import megamek.common.ConvFighter;
import megamek.common.Dropship;
import megamek.common.IGame;
import megamek.common.Infantry;
import megamek.common.LandAirMech;
import megamek.common.LargeSupportTank;
import megamek.common.Mech;
import megamek.common.Protomech;
import megamek.common.QuadMech;
import megamek.common.Report;
import megamek.common.SmallCraft;
import megamek.common.SupportTank;
import megamek.common.SupportVTOL;
import megamek.common.Tank;
import megamek.common.ToHitData;
import megamek.common.VTOL;
import megamek.common.WeaponType;
import megamek.common.actions.WeaponAttackAction;
import megamek.common.weapons.WeaponHandler;
import megamek.server.Server;

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
        int troopersHit = Compute.missilesHit(((Infantry) ae)
                .getShootingStrength(), nHitMod, bGlancing);
        double damage = ((InfantryWeapon)wtype).getInfantryDamage();
        if((ae instanceof Infantry) && !(ae instanceof BattleArmor)) {
            //for conventional infantry, we have to calculate primary and secondary weapons
            //to get damage per trooper
            damage = ((Infantry)ae).getDamagePerTrooper();
        }
        int damageDealt = (int) Math.round(damage * troopersHit);
        if((target instanceof Infantry) && !(target instanceof BattleArmor) && wtype.hasFlag(WeaponType.F_INF_BURST)) {
            damageDealt += Compute.d6();
        }
        if ((target instanceof Infantry) && ((Infantry)target).isMechanized()) {
            damageDealt /= 2;
        }
        // not terribly graceful, but does keep Non Penetrating weapons frame affecting non-conventional Infantry
        if((target instanceof BattleArmor) && wtype.hasFlag(WeaponType.F_INF_NONPENETRATING)) {
            damageDealt = 0;
        }
        if(target instanceof Mech && wtype.hasFlag(WeaponType.F_INF_NONPENETRATING)) {
            damageDealt = 0;
        }
        if((target instanceof Tank) && wtype.hasFlag(WeaponType.F_INF_NONPENETRATING)) {
            damageDealt = 0;
        }
        if((target instanceof Aero) && wtype.hasFlag(WeaponType.F_INF_NONPENETRATING)) {
            damageDealt = 0;
        }
        if((target instanceof VTOL) && wtype.hasFlag(WeaponType.F_INF_NONPENETRATING)) {
            damageDealt = 0;
        }
        if((target instanceof SmallCraft) && wtype.hasFlag(WeaponType.F_INF_NONPENETRATING)) {
            damageDealt = 0;
        }
        if((target instanceof Protomech) && wtype.hasFlag(WeaponType.F_INF_NONPENETRATING)) {
            damageDealt = 0;
        }
        if((target instanceof QuadMech) && wtype.hasFlag(WeaponType.F_INF_NONPENETRATING)) {
            damageDealt = 0;
        }
        if((target instanceof SupportVTOL) && wtype.hasFlag(WeaponType.F_INF_NONPENETRATING)) {
            damageDealt = 0;
        }
        if((target instanceof SupportTank) && wtype.hasFlag(WeaponType.F_INF_NONPENETRATING)) {
            damageDealt = 0;
        }
        if((target instanceof ConvFighter) && wtype.hasFlag(WeaponType.F_INF_NONPENETRATING)) {
            damageDealt = 0;
        }
        if((target instanceof Dropship) && wtype.hasFlag(WeaponType.F_INF_NONPENETRATING)) {
            damageDealt = 0;
        }
        if((target instanceof LandAirMech) && wtype.hasFlag(WeaponType.F_INF_NONPENETRATING)) {
            damageDealt = 0;
        }
        if((target instanceof LargeSupportTank) && wtype.hasFlag(WeaponType.F_INF_NONPENETRATING)) {
            damageDealt = 0;
        }
        if((target instanceof Building) && wtype.hasFlag(WeaponType.F_INF_NONPENETRATING)) {
            damageDealt = 0;
        }
        Report r = new Report(3325);
        r.subject = subjectId;
        r.add(troopersHit);
        r.add(" troopers ");
        r.add(toHit.getTableDesc() + ", causing " + damageDealt
                + " damage.");
        r.newlines = 0;
        vPhaseReport.addElement(r);
        if((target instanceof Infantry) && !(target instanceof BattleArmor)) {
            //this is a little strange, but I cant just do this in calcDamagePerHit because
            //that is called up before misses are determined and will lead to weird reporting
            nDamPerHit = damageDealt;
            return 1;
        }
        return damageDealt;
   // }

    // private BigInteger (boolean b) {
        // TODO Auto-generated method stub
        //return null;
    }
}

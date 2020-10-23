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

import megamek.MegaMek;
import megamek.common.*;
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
        
        if (this.bLowProfileGlancing) {
            nHitMod -= 4;
        }
        
        int troopersHit = 0;
        //when swarming all troopers hit
        if (ae.getSwarmTargetId() == target.getTargetId()) {
            troopersHit = ((Infantry) ae).getShootingStrength();
        } else if (!(ae instanceof Infantry)) {
            troopersHit = 1;
        } else {
            troopersHit = Compute.missilesHit(((Infantry) ae)
                .getShootingStrength(), nHitMod);
        }
        double damage;
        if (ae.isConventionalInfantry()) {
            //for conventional infantry, we have to calculate primary and secondary weapons
            //to get damage per trooper
            damage = ((Infantry) ae).getDamagePerTrooper();
        } else if (ae.isSupportVehicle()) {
            // Damage for some weapons depends on what type of ammo is being used
            if (((AmmoType) weapon.getLinked().getType()).getMunitionType() == AmmoType.M_INFERNO) {
                damage = ((InfantryWeapon) wtype).getInfernoVariant().getInfantryDamage();
            } else {
                damage = ((InfantryWeapon) wtype).getNonInfernoVariant().getInfantryDamage();
            }
        } else {
            damage = ((InfantryWeapon) wtype).getInfantryDamage();
        }
        if ((ae instanceof Infantry)
                && nRange == 0
                && ae.hasAbility(OptionsConstants.MD_TSM_IMPLANT)) {
            damage += 0.14;
        }
        int damageDealt = (int) Math.round(damage * troopersHit);
        if ((target instanceof Infantry) && !(target instanceof BattleArmor) && wtype.hasFlag(WeaponType.F_INF_BURST)) {
            damageDealt += Compute.d6();
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
        int av;
        //Sigh, another rules oversight - nobody bothered to figure this out
        //To be consistent with other cluster weapons we will assume 60% hit
        if (ae.isConventionalInfantry()) {
            double damage = ((Infantry) ae).getDamagePerTrooper();
            av = (int) Math.round(damage * 0.6 * ((Infantry) ae).getShootingStrength());
        } else {
            // Small fixed wing support
            av = super.calcAttackValue();
        }
        if (bDirect) {
            av = Math.min(av + (toHit.getMoS() / 3), av * 2);
        }
        av = applyGlancingBlowModifier(av, false);
        return av;
    }

    @Override
    public void useAmmo() {
        if (ae.isSupportVehicle()) {
            Mounted ammo = weapon.getLinked();
            if (ammo == null) {
                ae.loadWeapon(weapon);
                ammo = weapon.getLinked();
            }
            if (ammo == null) {// Can't happen. w/o legal ammo, the weapon
                // *shouldn't* fire.
                MegaMek.getLogger().error(String.format("Handler can't find any ammo for %s firing %s",
                                ae.getShortName(), weapon.getName()));
                return;
            }

            ammo.setShotsLeft(ammo.getBaseShotsLeft() - 1);
            // Swap between standard and inferno if the unit has some left of the other type
            if ((ammo.getUsableShotsLeft() <= 0)
                    && (ammo.getLinked() != null)
                    && (ammo.getLinked().getUsableShotsLeft() > 0)) {
                weapon.setLinked(ammo.getLinked());
                weapon.getLinked().setLinked(ammo);
            }
            super.useAmmo();
        }
    }
}

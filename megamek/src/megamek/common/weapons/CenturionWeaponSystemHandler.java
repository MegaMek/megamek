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
import megamek.common.Building;
import megamek.common.Entity;
import megamek.common.IGame;
import megamek.common.Infantry;
import megamek.common.Report;
import megamek.common.ToHitData;
import megamek.common.actions.WeaponAttackAction;
import megamek.common.options.OptionsConstants;
import megamek.server.Server;

/**
 * Weaponhandler for the Centurion Weapon System weapon, 
 * which is found in Jihad Conspiracies Interstellar Players 2, pg 127.
 * 
 * @author arlith
 *
 */ 
class CenturionWeaponSystemHandler extends EnergyWeaponHandler {

    /**
     * 
     */
    private static final long serialVersionUID = -5226841653686213141L;

    /**
     * @param t
     * @param w
     * @param g
     * @param s
     */
    public CenturionWeaponSystemHandler(ToHitData t, WeaponAttackAction w, IGame g, Server s) {
        super(t, w, g, s);
    }

    /*
     * (non-Javadoc)
     * 
     * @see megamek.common.weapons.EnergyWeaponHandler#calcDamagePerHit()
     */
    @Override
    protected int calcDamagePerHit() {
        return 0;
    }

    protected void handleEntityDamage(Entity entityTarget,
            Vector<Report> vPhaseReport, Building bldg, int hits, int nCluster,
            int bldgAbsorbs) {
        super.handleEntityDamage(entityTarget, vPhaseReport, bldg, hits, 
                nCluster, bldgAbsorbs);

        // Report that this unit has been hit by CWS
        Report r = new Report(7510);
        r.subject = entityTarget.getId();
        r.addDesc(entityTarget);
        r.indent(2);
        vPhaseReport.add(r);

        // CWS has no effect against infantry
        if ((entityTarget instanceof Infantry) 
                && !(entityTarget instanceof BattleArmor)){
            // No Effect
            r = new Report(7515);
            r.subject = entityTarget.getId();
            r.indent(3);
            vPhaseReport.add(r);
            return;
        }
        
        // If the Entity is shutdown, it will remain shutdown next turn
        if (entityTarget.isShutDown()){
            r = new Report(7511);
            r.subject = entityTarget.getId();
            r.addDesc(entityTarget);
            r.indent(3);
            vPhaseReport.add(r);
            if (entityTarget.getTaserShutdownRounds() < 1){
                entityTarget.setTaserShutdownRounds(1);
            }
        } else { // Otherwise, there's a shutdown check
            boolean mtHeat = game.getOptions().booleanOption(OptionsConstants.ADVCOMBAT_TACOPS_HEAT);
            int hotDogMod = 0;
            if (entityTarget.getCrew().getOptions().booleanOption(OptionsConstants.PILOT_HOT_DOG)) {
                hotDogMod = 1;
            }
            int shutdown = (4 + (((entityTarget.heat) / 4) * 2))
                    - hotDogMod;
            if (mtHeat) {
                shutdown -= 5;
                switch (entityTarget.getCrew().getPiloting()) {
                    case 0:
                    case 1:
                        shutdown -= 2;
                        break;
                    case 2:
                    case 3:
                        shutdown -= 1;
                        break;
                    case 6:
                    case 7:
                        shutdown += 1;
                        break;
                }
            }
            int sdroll = entityTarget.getCrew().rollPilotingSkill();
            r = new Report(5060);
            r.subject = entityTarget.getId();
            r.indent(3);
            r.addDesc(entityTarget);
            r.add(shutdown);
            r.add(sdroll);
            if (sdroll >= shutdown) {
                // avoided
                r.choose(true);
                vPhaseReport.add(r);
            } else {
                // shutting down...
                r.choose(false);
                vPhaseReport.add(r);
                // okay, now mark shut down
                if (entityTarget instanceof BattleArmor){
                    r = new Report(3706);
                    r.addDesc(entityTarget);
                    r.indent(4);
                    // shut down for rest of scenario, treat as blown off loc
                    r.add(entityTarget.getLocationAbbr(hit));
                    vPhaseReport.add(r);
                    // TODO: fix for salvage purposes
                    entityTarget.destroyLocation(hit.getLocation());
                    // Check to see if the squad has been eliminated
                    if (entityTarget.getTransferLocation(hit).getLocation() == 
                            Entity.LOC_DESTROYED) {
                        vPhaseReport.addAll(server.destroyEntity(entityTarget,
                                "all troopers eliminated", false));
                    }
                } else {
                    entityTarget.setShutDown(true);
                }
            }            
        }
    }
}

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

import java.util.Vector;

import megamek.common.Aero;
import megamek.common.BattleArmor;
import megamek.common.Building;
import megamek.common.Compute;
import megamek.common.ConvFighter;
import megamek.common.Engine;
import megamek.common.Entity;
import megamek.common.IGame;
import megamek.common.Mech;
import megamek.common.Protomech;
import megamek.common.Report;
import megamek.common.SupportTank;
import megamek.common.Tank;
import megamek.common.ToHitData;
import megamek.common.actions.WeaponAttackAction;
import megamek.common.weapons.other.TSEMPWeapon;
import megamek.server.Server;

/**
 * Weaponhandler for the Tight-Stream Electro-Magnetic Pulse (TSEMP) weapon, 
 * which is found in FM:3145 pg 255.
 * 
 * @author arlith
 * Created on Sept 5, 2005
 */ 
public class TSEMPHandler extends EnergyWeaponHandler {
    private static final long serialVersionUID = 5545991061428671743L;

    /**
     * @param t
     * @param w
     * @param g
     * @param s
     */
    public TSEMPHandler(ToHitData t, WeaponAttackAction w, IGame g, Server s) {
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
    
    public boolean handle(IGame.Phase phase, Vector<Report> vPhaseReport) {
        weapon.setFired(true);

        ae.setFiredTsempThisTurn(true);
        ae.setHasFiredTsemp(true);

        if (ae.getTsempEffect() == TSEMPWeapon.TSEMP_EFFECT_NONE){
            ae.setTsempEffect(TSEMPWeapon.TSEMP_EFFECT_INTERFERENCE);
        }

        return super.handle(phase, vPhaseReport);
    }

    protected void handleEntityDamage(Entity entityTarget,
            Vector<Report> vPhaseReport, Building bldg, int hits, int nCluster,
            int bldgAbsorbs) {
        super.handleEntityDamage(entityTarget, vPhaseReport, bldg, hits, 
                nCluster, bldgAbsorbs);
        
        // Increment the TSEMP hit counter
        entityTarget.addTsempHitThisTurn();

        // Report that this unit has been hit by TSEMP
        Report r = new Report(7410);
        r.subject = entityTarget.getId();
        r.addDesc(entityTarget);
        r.add(entityTarget.getTsempHitsThisTurn());
        r.indent(2);
        vPhaseReport.add(r);

        // TSEMP has no effect against infantry
        if (entityTarget.isConventionalInfantry()) {
            // No Effect
            r = new Report(7415);
            r.subject = entityTarget.getId();
            r.indent(3);
            vPhaseReport.add(r);
            return;
        }
        
        // Determine roll modifiers
        int tsempModifiers = 0;
        if (entityTarget.getWeight() >= 200){
            // No Effect
            r = new Report(7416);
            r.subject = entityTarget.getId();
            r.indent(3);
            vPhaseReport.add(r);
            return;
        } else if (entityTarget.getWeight() >= 100){
            tsempModifiers -= 2;
        }
        
        if (entityTarget.getEngine() != null &&
                entityTarget.getEngine().getEngineType() == 
                    Engine.COMBUSTION_ENGINE){
            tsempModifiers -= 1;
        } else if (entityTarget.getEngine() != null &&
                entityTarget.getEngine().getEngineType() == 
                Engine.STEAM){
            tsempModifiers -= 2;
        }
        
        tsempModifiers += Math.min(4, entityTarget.getTsempHitsThisTurn() - 1);
        // Multiple hits add a +1 for each hit after the first, 
        //  up to a max of 4                   
        int tsempRoll = Math.max(2, Compute.d6(2) + tsempModifiers);
        
        // Ugly code to set the target rolls
        int shutdownTarget = 13;
        int interferenceTarget = 13;
        if (entityTarget instanceof Mech){
            if (((Mech) entityTarget).isIndustrial()){
                interferenceTarget = 6;
                shutdownTarget = 8;
            } else {
                interferenceTarget = 7;
                shutdownTarget = 9;
            }            
        } else if (entityTarget instanceof SupportTank){
            interferenceTarget = 5;
            shutdownTarget = 7;
        } else if (entityTarget instanceof Tank){
            interferenceTarget = 6;
            shutdownTarget = 8;
        } else if (entityTarget instanceof BattleArmor){
            interferenceTarget = 6;
            shutdownTarget = 8;
        } else if (entityTarget instanceof Protomech){
            interferenceTarget = 6;
            shutdownTarget = 9;
        } else if (entityTarget instanceof ConvFighter){
            interferenceTarget = 6;
            shutdownTarget = 8;
        } else if (entityTarget instanceof Aero){
            interferenceTarget = 7;
            shutdownTarget = 9;
        }

        // Create the effect report
        if (tsempModifiers == 0){
            r = new Report(7411);
        } else {
            r = new Report(7412);
            if (tsempModifiers >= 0){
                r.add("+" + tsempModifiers);
            } else {
                r.add(tsempModifiers);
            }
        }
        r.indent(3);
        r.add(tsempRoll);
        r.subject = entityTarget.getId();
        String tsempEffect;

        // Determine the effect
        Report baShutdownReport = null;
        if (tsempRoll >= shutdownTarget){
            entityTarget.setTsempEffect(TSEMPWeapon.TSEMP_EFFECT_SHUTDOWN);
            tsempEffect = 
                    "<font color='C00000'><b>Shutdown!</b></font>";
            if (entityTarget instanceof BattleArmor){
                baShutdownReport = new Report(3706);
                baShutdownReport.addDesc(entityTarget);
                baShutdownReport.indent(4);
                baShutdownReport.add(entityTarget.getLocationAbbr(hit));
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
        } else if (tsempRoll >= interferenceTarget){
            int targetEffect = entityTarget.getTsempEffect();
            if (targetEffect != TSEMPWeapon.TSEMP_EFFECT_SHUTDOWN) {
                entityTarget.setTsempEffect(
                        TSEMPWeapon.TSEMP_EFFECT_INTERFERENCE);
            }
            tsempEffect = "<b>Interference!</b>";
        } else {
            // No effect roll
            tsempEffect = "No Effect!";
        }
        r.add(tsempEffect);
        vPhaseReport.add(r); 
        if (baShutdownReport != null){
            vPhaseReport.add(baShutdownReport);
        }
    }
}

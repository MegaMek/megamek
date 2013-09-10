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

import megamek.common.BattleArmor;
import megamek.common.Building;
import megamek.common.Compute;
import megamek.common.Entity;
import megamek.common.IGame;
import megamek.common.Infantry;
import megamek.common.Mounted;
import megamek.common.RangeType;
import megamek.common.Report;
import megamek.common.TargetRoll;
import megamek.common.Targetable;
import megamek.common.ToHitData;
import megamek.common.actions.WeaponAttackAction;
import megamek.server.Server;

/**
 * @author Sebastian Brocks
 */
public class LRMSwarmHandler extends LRMHandler {

    /**
     *
     */
    private static final long serialVersionUID = 7962873403915683220L;
    private int swarmMissilesNowLeft = 0;
    private boolean handledHeat = false;

    /**
     * @param t
     * @param w
     * @param g
     * @param s
     */
    public LRMSwarmHandler(ToHitData t, WeaponAttackAction w, IGame g, Server s) {
        super(t, w, g, s);
        sSalvoType = " swarm missile(s) ";
    }

    /*
     * (non-Javadoc)
     * 
     * @see megamek.common.weapons.AttackHandler#handle(int, java.util.Vector)
     */
    @Override
    public boolean handle(IGame.Phase phase, Vector<Report> vPhaseReport) {
        if (!cares(phase)) {
            return true;
        }
        Entity entityTarget = (target.getTargetType() == Targetable.TYPE_ENTITY) ? (Entity) target
                : null;
        final boolean targetInBuilding = Compute.isInBuilding(game,
                entityTarget);

        if (entityTarget != null) {
            ae.setLastTarget(entityTarget.getId());
        }

        // Which building takes the damage?
        Building bldg = game.getBoard().getBuildingAt(target.getPosition());

        // Report weapon attack and its to-hit value.
        Report r = new Report(3115);
        r.indent();
        r.newlines = 0;
        r.subject = subjectId;
        r.add(wtype.getName());
        if (entityTarget != null) {
            r.addDesc(entityTarget);
            // record which launcher targeted the target
            entityTarget.addTargetedBySwarm(ae.getId(), waa.getWeaponId());
        } else {
            r.messageId = 3120;
            r.add(target.getDisplayName(), true);
        }
        vPhaseReport.addElement(r);
        if (toHit.getValue() == TargetRoll.IMPOSSIBLE) {
            r = new Report(3135);
            r.subject = subjectId;
            r.add(toHit.getDesc());
            vPhaseReport.addElement(r);
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
            r.add(toHit.getValue());
            vPhaseReport.addElement(r);
        }

        // dice have been rolled, thanks
        r = new Report(3155);
        r.newlines = 0;
        r.subject = subjectId;
        r.add(roll);
        vPhaseReport.addElement(r);

        // do we hit?
        bMissed = roll < toHit.getValue();

        // are we a glancing hit?
        if (game.getOptions().booleanOption("tacops_glancing_blows")) {
            if (roll == toHit.getValue()) {
                bGlancing = true;
                r = new Report(3186);
                r.subject = subjectId;
                r.newlines = 0;
                vPhaseReport.addElement(r);
            } else {
                bGlancing = false;
            }
        } else {
            bGlancing = false;
        }

        // Set Margin of Success/Failure.
        toHit.setMoS(roll - Math.max(2, toHit.getValue()));
        bDirect = game.getOptions().booleanOption("tacops_direct_blow")
                && ((toHit.getMoS() / 3) >= 1) && (entityTarget != null);
        if (bDirect) {
            r = new Report(3189);
            r.subject = ae.getId();
            r.newlines = 0;
            vPhaseReport.addElement(r);
        }

        // Do this stuff first, because some weapon's miss report reference the
        // amount of shots fired and stuff.
        if (!handledHeat) {
            addHeat();
            handledHeat = true;
        }

        // Any necessary PSRs, jam checks, etc.
        // If this boolean is true, don't report
        // the miss later, as we already reported
        // it in doChecks
        boolean missReported = doChecks(vPhaseReport);

        nDamPerHit = calcDamagePerHit();

        // Do we need some sort of special resolution (minefields, artillery,
        if (specialResolution(vPhaseReport, entityTarget)) {
            return false;
        }

        if (bMissed && !missReported) {
            reportMiss(vPhaseReport);

            // Works out fire setting, AMS shots, and whether continuation is
            // necessary.
            if (!handleSpecialMiss(entityTarget, targetInBuilding, bldg,
                    vPhaseReport, phase)) {
                return false;
            }
        }

        // yeech. handle damage. . different weapons do this in very different
        // ways
        int hits = calcHits(vPhaseReport), nCluster = calcnCluster();

        if (bMissed) {
            return false;

        } // End missed-target

        // The building shields all units from a certain amount of damage.
        // The amount is based upon the building's CF at the phase's start.
        int bldgAbsorbs = 0;
        if (targetInBuilding && (bldg != null)) {
            bldgAbsorbs = bldg.getAbsorbtion(target.getPosition());
        }

        // Make sure the player knows when his attack causes no damage.
        if (hits == 0) {
            r = new Report(3365);
            r.subject = subjectId;
            vPhaseReport.addElement(r);
        }

        // for each cluster of hits, do a chunk of damage
        while (hits > 0) {
            int nDamage;
            // targeting a hex for igniting
            if ((target.getTargetType() == Targetable.TYPE_HEX_IGNITE)
                    || (target.getTargetType() == Targetable.TYPE_BLDG_IGNITE)) {
                handleIgnitionDamage(vPhaseReport, bldg, hits);
                return false;
            }
            // targeting a hex for clearing
            if (target.getTargetType() == Targetable.TYPE_HEX_CLEAR) {
                nDamage = nDamPerHit * hits;
                handleClearDamage(vPhaseReport, bldg, nDamage);
                return false;
            }
            // Targeting a building.
            if (target.getTargetType() == Targetable.TYPE_BUILDING) {
                // The building takes the full brunt of the attack.
                nDamage = nDamPerHit * hits;
                handleBuildingDamage(vPhaseReport, bldg, nDamage,
                        target.getPosition());
                // And we're done!
                return false;
            }
            if (entityTarget != null) {
                handleEntityDamage(entityTarget, vPhaseReport, bldg, hits,
                        nCluster, bldgAbsorbs);
                server.creditKill(entityTarget, ae);
                hits -= nCluster;
            }
        } // Handle the next cluster.
        Report.addNewline(vPhaseReport);
        if (swarmMissilesNowLeft > 0) {
            Entity swarmTarget = Compute.getSwarmMissileTarget(game,
                    ae.getId(), entityTarget, waa.getWeaponId());
            boolean stoppedByECM = Compute.isAffectedByECM(ae,
                    target.getPosition(), target.getPosition())
                    && !(this instanceof LRMSwarmIHandler);
            if (swarmTarget != null && !stoppedByECM) {
                r = new Report(3420);
                r.subject = subjectId;
                r.indent(2);
                r.add(swarmMissilesNowLeft);
                vPhaseReport.addElement(r);
                weapon.setUsedThisRound(false);
                WeaponAttackAction newWaa = new WeaponAttackAction(ae.getId(),
                        swarmTarget.getTargetId(), waa.getWeaponId());
                newWaa.setSwarmingMissiles(true);
                newWaa.setSwarmMissiles(swarmMissilesNowLeft);
                newWaa.setOldTargetId(target.getTargetId());
                newWaa.setAmmoId(waa.getAmmoId());
                Mounted m = ae.getEquipment(waa.getWeaponId());
                Weapon w = (Weapon) m.getType();
                // increase ammo by one, we'll use one that we shouldn't use
                // in the next line
                weapon.getLinked().setShotsLeft(
                        weapon.getLinked().getBaseShotsLeft() + 1);
                AttackHandler ah = w.fire(newWaa, game, server);
                LRMSwarmHandler wh = (LRMSwarmHandler) ah;
                // attack the new target
                wh.handledHeat = true;
                wh.handle(phase, vPhaseReport);
            } else {
                if (swarmTarget == null) {
                    r = new Report(3425);
                    r.add(swarmMissilesNowLeft);
                    r.subject = subjectId;
                    r.indent(2);
                    vPhaseReport.addElement(r);
                } else {
                    r = new Report(3426);
                    r.add(swarmMissilesNowLeft);
                    r.subject = subjectId;
                    r.indent(2);
                    vPhaseReport.addElement(r);
                }
            }

            // Reset the Swarm Missiles variable to 0 because if we don't it
            // thinks we've already computed this when we fire the next rack.
            swarmMissilesNowLeft = 0;

        }
        return false;
    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * megamek.common.weapons.WeaponHandler#handleSpecialMiss(megamek.common
     * .Entity, boolean, megamek.common.Building, java.util.Vector)
     */
    protected boolean handleSpecialMiss(Entity entityTarget,
            boolean targetInBuilding, Building bldg,
            Vector<Report> vPhaseReport, IGame.Phase phase) {
        super.handleSpecialMiss(entityTarget, targetInBuilding, bldg,
                vPhaseReport);
        int swarmMissilesNowLeft = waa.getSwarmMissiles();
        if (swarmMissilesNowLeft == 0) {
            swarmMissilesNowLeft = wtype.getRackSize();
        }
        if (entityTarget != null) {
            ae.setLastTarget(entityTarget.getId());
        }

        Entity swarmTarget = Compute.getSwarmMissileTarget(game, ae.getId(),
                entityTarget, waa.getWeaponId());
        boolean stoppedByECM = Compute.isAffectedByECM(ae,
                target.getPosition(), target.getPosition())
                && !(this instanceof LRMSwarmIHandler);
        if (swarmTarget != null && !stoppedByECM) {
            Report r = new Report(3420);
            r.subject = subjectId;
            r.indent(2);
            r.add(swarmMissilesNowLeft);
            vPhaseReport.addElement(r);
            weapon.setUsedThisRound(false);
            WeaponAttackAction newWaa = new WeaponAttackAction(ae.getId(),
                    swarmTarget.getTargetId(), waa.getWeaponId());
            newWaa.setSwarmingMissiles(true);
            newWaa.setSwarmMissiles(swarmMissilesNowLeft);
            newWaa.setOldTargetId(target.getTargetId());
            newWaa.setAmmoId(waa.getAmmoId());
            Mounted m = ae.getEquipment(waa.getWeaponId());
            Weapon w = (Weapon) m.getType();
            // increase ammo by one, we'll use one that we shouldn't use
            // in the next line
            weapon.getLinked().setShotsLeft(
                    weapon.getLinked().getBaseShotsLeft() + 1);
            AttackHandler ah = w.fire(newWaa, game, server);
            LRMSwarmHandler wh = (LRMSwarmHandler) ah;
            // attack the new target
            wh.handledHeat = true;
            wh.handle(phase, vPhaseReport);
        } else {
            if (swarmTarget == null) {
                Report r = new Report(3425);
                r.add(swarmMissilesNowLeft);
                r.subject = subjectId;
                r.indent(2);
                vPhaseReport.addElement(r);
            } else {
                Report r = new Report(3426);
                r.add(swarmMissilesNowLeft);
                r.subject = subjectId;
                r.indent(2);
                vPhaseReport.addElement(r);
            }
        }
        return false;
    }

    /*
     * (non-Javadoc)
     * 
     * @see megamek.common.weapons.WeaponHandler#calcHits(java.util.Vector)
     */
    @Override
    protected int calcHits(Vector<Report> vPhaseReport) {
        // conventional infantry gets hit in one lump
        // BAs do one lump of damage per BA suit
        if ((target instanceof Infantry) && !(target instanceof BattleArmor)) {
            if (ae instanceof BattleArmor) {
                bSalvo = true;
                return ((BattleArmor) ae).getShootingStrength();
            }
            return 1;
        }

        int missilesHit;
        int nMissilesModifier = 0;

        // add AMS mods
        nMissilesModifier += getAMSHitsMod(vPhaseReport);

        if (bGlancing) {
            nMissilesModifier -= 4;
        }
        if (game.getOptions().booleanOption("tacops_range")
                && (nRange > wtype.getRanges(weapon)[RangeType.RANGE_LONG])) {
            nMissilesModifier -= 2;
        }

        if (bDirect) {
            nMissilesModifier += (toHit.getMoS() / 3) * 2;
        }

        if (game.getPlanetaryConditions().hasEMI()) {
            nMissilesModifier -= 2;
        }

        int swarmMissilesLeft = waa.getSwarmMissiles();
        // swarm or swarm-I shots may just hit with the remaining missiles
        if (swarmMissilesLeft > 0) {
            if (allShotsHit()) {
                missilesHit = swarmMissilesLeft;
            } else {
                int swarmsForHitTable = 5;
                if ((swarmMissilesLeft > 5) && (swarmMissilesLeft <= 10)) {
                    swarmsForHitTable = 10;
                } else if ((swarmMissilesLeft > 10)
                        && (swarmMissilesLeft <= 15)) {
                    swarmsForHitTable = 15;
                } else if ((swarmMissilesLeft > 15)
                        && (swarmMissilesLeft <= 20)) {
                    swarmsForHitTable = 20;
                }
                missilesHit = Compute.missilesHit(swarmsForHitTable,
                        nMissilesModifier, weapon.isHotLoaded(), false,
                        advancedAMS && amsEnganged);
                if (missilesHit > swarmMissilesLeft) {
                    missilesHit = swarmMissilesLeft;
                }
            }
        } else {
            missilesHit = allShotsHit() ? wtype.getRackSize() : Compute
                    .missilesHit(wtype.getRackSize(), nMissilesModifier,
                            weapon.isHotLoaded(), false, advancedAMS
                                    && amsEnganged);
            swarmMissilesLeft = wtype.getRackSize();
        }
        swarmMissilesNowLeft = swarmMissilesLeft - missilesHit;
        Report r = new Report(3325);
        r.subject = subjectId;
        r.add(missilesHit);
        r.add(sSalvoType);
        r.add(toHit.getTableDesc());
        vPhaseReport.addElement(r);
        r = new Report(3345);
        r.subject = subjectId;
        vPhaseReport.addElement(r);
        bSalvo = true;
        return missilesHit;
    }
}

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
package megamek.common.weapons;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.util.Vector;

import megamek.common.BattleArmor;
import megamek.common.Building;
import megamek.common.Compute;
import megamek.common.Entity;
import megamek.common.HitData;
import megamek.common.IGame;
import megamek.common.Infantry;
import megamek.common.Mech;
import megamek.common.Mounted;
import megamek.common.Report;
import megamek.common.TargetRoll;
import megamek.common.Targetable;
import megamek.common.ToHitData;
import megamek.common.WeaponType;
import megamek.common.actions.WeaponAttackAction;
import megamek.server.Server;
import megamek.server.Server.DamageType;

/**
 * @author Andrew Hunter A basic, simple attack handler. May or may not work for
 *         any particular weapon; must be overloaded to support special rules.
 */
public class WeaponHandler implements AttackHandler, Serializable {

    /**
     * 
     */
    private static final long serialVersionUID = 7137408139594693559L;
    public ToHitData toHit;
    public WeaponAttackAction waa;
    public int roll;

    protected IGame game;
    protected transient Server server; // must not save the server
    protected Report r;
    protected boolean bMissed;
    protected boolean bSalvo = false;
    protected boolean bGlancing = false;
    protected WeaponType wtype;
    protected Mounted weapon;
    protected Entity ae;
    protected Targetable target;
    protected int subjectId;
    protected int nRange;
    protected int nDamPerHit;
    protected boolean throughFront;
    protected boolean announcedEntityFiring = false;
    protected boolean missed = false;
    protected DamageType damageType;

    /**
     * return the <code>int</code> Id of the attacking <code>Entity</code>
     */
    public int getAttackerId() {
        return ae.getId();
    }

    /**
     * Do we care about the specified phase?
     */
    public boolean cares(int phase) {
        if (phase == IGame.PHASE_FIRING) {
            return true;
        }
        return false;
    }

    /**
     * @param vPhaseReport - A <code>Vector</code> containing the phasereport.
     * @return a <code>boolean</code> value indicating wether or not the
     *         attack misses because of a failed check.
     */
    protected boolean doChecks(Vector<Report> vPhaseReport) {
        return false;
    }

    private void writeObject(ObjectOutputStream out) throws IOException {
        out.defaultWriteObject();
    }

    private void readObject(ObjectInputStream in) throws IOException,
            ClassNotFoundException {
        in.defaultReadObject();

        server = Server.getServerInstance();
    }

    /**
     * @return a <code>boolean</code> value indicating wether or not this
     *         attack needs further calculating, like a missed shot hitting a
     *         building, or an AMS only shooting down some missiles.
     */
    protected boolean handleSpecialMiss(Entity entityTarget,
            boolean targetInBuilding, Building bldg, Vector<Report> vPhaseReport) {
        // Shots that miss an entity can set fires.
        // Buildings can't be accidentally ignited,
        // and some weapons can't ignite fires.
        if (entityTarget != null
                && (bldg == null && wtype.getFireTN() != TargetRoll.IMPOSSIBLE)) {
            server.tryIgniteHex(target.getPosition(), subjectId, false, 11,
                    vPhaseReport);
        }

        // BMRr, pg. 51: "All shots that were aimed at a target inside
        // a building and miss do full damage to the building instead."
        if (!targetInBuilding || toHit.getValue() == TargetRoll.AUTOMATIC_FAIL) {
            return false;
        }
        return true;
    }

    /**
     * Calculate the number of hits
     * 
     * @param vPhaseReport - the <code>Vector</code> containing the phase
     *            report.
     * @return an <code>int</code> containing the number of hits.
     */
    protected int calcHits(Vector<Report> vPhaseReport) {
        // normal BA attacks (non-swarm, non single-trooper weapons)
        // do more than 1 hit
        if (ae instanceof BattleArmor
                && weapon.getLocation() == BattleArmor.LOC_SQUAD
                && !(ae.getSwarmTargetId() == target.getTargetId())) {
            bSalvo = true;
            int toReturn = allShotsHit() ? ((BattleArmor) ae).getShootingStrength()
                    : Compute.missilesHit(((BattleArmor) ae)
                            .getShootingStrength()); 
            r = new Report(3325);
            r.subject = subjectId;
            r.add(toReturn);
            r.add(" troopers ");
            r.add(toHit.getTableDesc());
            r.newlines = 0;
            vPhaseReport.add(r);
            return toReturn;
        }
        return 1;
    }

    /**
     * Calculate the clustering of the hits
     * 
     * @return a <code>int</code> value saying how much hits are in each
     *         cluster of damage.
     */
    protected int calcnCluster() {
        return 1;
    }

    /**
     * handle this weapons firing
     * 
     * @return a <code>boolean</code> value indicating wether this should be
     *         kept or not
     */
    public boolean handle(int phase, Vector<Report> vPhaseReport) {
        if (!this.cares(phase)) {
            return true;
        }
        Entity entityTarget = (target.getTargetType() == Targetable.TYPE_ENTITY) ? (Entity) target
                : null;
        final boolean targetInBuilding = Compute.isInBuilding(game,
                entityTarget);

        if (entityTarget != null)
            ae.setLastTarget(entityTarget.getId());
        // Which building takes the damage?
        Building bldg = game.getBoard().getBuildingAt(target.getPosition());

        // Report weapon attack and its to-hit value.
        r = new Report(3115);
        r.indent();
        r.newlines = 0;
        r.subject = subjectId;
        r.add(wtype.getName());
        if (entityTarget != null) {
            r.addDesc(entityTarget);
        } else {
            r.messageId = 3120;
            r.add(target.getDisplayName(), true);
        }
        vPhaseReport.addElement(r);
        if (toHit.getValue() == ToHitData.IMPOSSIBLE) {
            r = new Report(3135);
            r.subject = subjectId;
            r.add(toHit.getDesc());
            vPhaseReport.addElement(r);
            return false;
        } else if (toHit.getValue() == ToHitData.AUTOMATIC_FAIL) {
            r = new Report(3140);
            r.newlines = 0;
            r.subject = subjectId;
            r.add(toHit.getDesc());
            vPhaseReport.addElement(r);
        } else if (toHit.getValue() == ToHitData.AUTOMATIC_SUCCESS) {
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
        if (game.getOptions().booleanOption("maxtech_glancing_blows")) {
            if (roll == toHit.getValue()) {
                bGlancing = true;
                r = new Report(3186);
                r.subject = ae.getId();
                r.newlines = 0;
                vPhaseReport.addElement(r);
            } else {
                bGlancing = false;
            }
        } else {
            bGlancing = false;
        }

        // Do this stuff first, because some weapon's miss report reference the
        // amount of shots fired and stuff.
        nDamPerHit = calcDamagePerHit();
        addHeat();

        // Any necessary PSRs, jam checks, etc.
        // If this boolean is true, don't report
        // the miss later, as we already reported
        // it in doChecks
        boolean missReported = doChecks(vPhaseReport);
        if (missReported) {
            bMissed = true;
        }

        // Do we need some sort of special resolution (minefields, artillery,
        if (specialResolution(vPhaseReport, entityTarget, bMissed)) {
            return false;
        }

        if (bMissed && !missReported) {
            reportMiss(vPhaseReport);

            // Works out fire setting, AMS shots, and whether continuation is
            // necessary.
            if (!handleSpecialMiss(entityTarget, targetInBuilding, bldg,
                    vPhaseReport)) {
                return false;
            }
        }

        // yeech. handle damage. . different weapons do this in very different
        // ways
        int hits = calcHits(vPhaseReport), nCluster = calcnCluster();

        // We've calculated how many hits. At this point, any missed
        // shots damage the building instead of the target.
        if (bMissed) {
            if (targetInBuilding && bldg != null) {
                handleAccidentalBuildingDamage(vPhaseReport, bldg, hits,
                        nDamPerHit);
            } // End missed-target-in-building
            return false;

        } // End missed-target

        // The building shields all units from a certain amount of damage.
        // The amount is based upon the building's CF at the phase's start.
        int bldgAbsorbs = 0;
        if (targetInBuilding && bldg != null) {
            bldgAbsorbs = (int) Math.ceil(bldg.getPhaseCF() / 10.0);
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
            if (target.getTargetType() == Targetable.TYPE_HEX_IGNITE
                    || target.getTargetType() == Targetable.TYPE_BLDG_IGNITE) {
                handleIgnitionDamage(vPhaseReport, bldg, bSalvo, hits);
                return false;
            }
            // targeting a hex for clearing
            if (target.getTargetType() == Targetable.TYPE_HEX_CLEAR) {
                nDamage = nDamPerHit * hits;
                handleClearDamage(vPhaseReport, bldg, nDamage, bSalvo);
                return false;
            }
            // Targeting a building.
            if (target.getTargetType() == Targetable.TYPE_BUILDING) {
                // The building takes the full brunt of the attack.
                nDamage = nDamPerHit * hits;
                handleBuildingDamage(vPhaseReport, bldg, nDamage, bSalvo);
                // And we're done!
                return false;
            }
            if (entityTarget != null) {
                handleEntityDamage(entityTarget, vPhaseReport, bldg, hits,
                        nCluster, nDamPerHit, bldgAbsorbs);
                server.creditKill(entityTarget, ae);
                hits -= nCluster;
            }
        } // Handle the next cluster.
        Report.addNewline(vPhaseReport);
        return false;
    }

    /**
     * Calculate the damage per hit.
     * 
     * @return an <code>int</code> representing the damage dealt per hit.
     */
    protected int calcDamagePerHit() {
        double toReturn = wtype.getDamage();
        // during a swarm, all damage gets applied as one block to one location
        if (ae instanceof BattleArmor
                && weapon.getLocation() == BattleArmor.LOC_SQUAD
                && (ae.getSwarmTargetId() == target.getTargetId())) {
            toReturn *= ((BattleArmor) ae).getShootingStrength();
        }
        // we default to direct fire weapons for anti-infantry damage
        if (target instanceof Infantry && !(target instanceof BattleArmor)) {
            toReturn = Math.ceil(toReturn/10);
        }
        if (bGlancing) {
            toReturn = ((int)toReturn) / 2;
        }
        return (int) toReturn;
    }

    /**
     * Handle damage against an entity, called once per hit by default.
     * 
     * @param entityTarget
     * @param vPhaseReport
     * @param bldg
     * @param hits
     * @param nCluster
     * @param nDamPerHit
     * @param bldgAbsorbs
     */
    protected void handleEntityDamage(Entity entityTarget,
            Vector<Report> vPhaseReport, Building bldg, int hits, int nCluster,
            int nDamPerHit, int bldgAbsorbs) {
        int nDamage;
        missed = false;

        HitData hit = entityTarget.rollHitLocation(toHit.getHitTable(), toHit
                .getSideTable(), waa.getAimedLocation(), waa.getAimingMode());

        if (entityTarget.removePartialCoverHits(hit.getLocation(), toHit
                .getCover(), Compute.targetSideTable(ae, entityTarget))) {
            // Weapon strikes Partial Cover.
            r = new Report(3460);
            r.subject = subjectId;
            r.add(entityTarget.getShortName());
            r.add(entityTarget.getLocationAbbr(hit));
            r.newlines = 0;
            r.indent(2);
            vPhaseReport.addElement(r);
            nDamage = 0;
            missed = true;
            return;
        }

        if (!bSalvo) {
            // Each hit in the salvo get's its own hit location.
            r = new Report(3405);
            r.subject = subjectId;
            r.add(toHit.getTableDesc());
            r.add(entityTarget.getLocationAbbr(hit));
            r.newlines = 0;
            vPhaseReport.addElement(r);
        }

        if (hit.hitAimedLocation()) {
            r = new Report(3410);
            r.subject = subjectId;
            r.newlines = 0;
            vPhaseReport.addElement(r);
        }
        // Resolve damage normally.
        nDamage = nDamPerHit * Math.min(nCluster, hits);

        // A building may be damaged, even if the squad is not.
        if (bldgAbsorbs > 0) {
            int toBldg = Math.min(bldgAbsorbs, nDamage);
            nDamage -= toBldg;
            Report.addNewline(vPhaseReport);
            Vector<Report> buildingReport = server.damageBuilding(bldg, toBldg);
            for (Report report : buildingReport) {
                report.subject = subjectId;
            }
            vPhaseReport.addAll(buildingReport);
        }

        // A building may absorb the entire shot.
        if (nDamage == 0) {
            r = new Report(3415);
            r.subject = subjectId;
            r.indent(2);
            r.addDesc(entityTarget);
            r.newlines = 0;
            vPhaseReport.addElement(r);
            missed = true;
        } else {
            if (bGlancing) {
                hit.makeGlancingBlow();
            }
            vPhaseReport
                    .addAll(server.damageEntity(entityTarget, hit, nDamage,
                            false, ae.getSwarmTargetId() == entityTarget
                                    .getId() ? DamageType.IGNORE_PASSENGER
                                    : damageType, false, false, throughFront));
        }
    }

    protected void handleAccidentalBuildingDamage(Vector<Report> vPhaseReport,
            Building bldg, int hits, int nDamPerHit) {
        // Damage the building in one big lump.
        // Only report if damage was done to the building.
        int toBldg = hits * nDamPerHit;
        if (toBldg > 0) {
            Vector<Report> buildingReport = server.damageBuilding(bldg, toBldg);
            for (Report report : buildingReport) {
                report.subject = subjectId;
            }
            vPhaseReport.addAll(buildingReport);
        }
    }

    protected void handleIgnitionDamage(Vector<Report> vPhaseReport,
            Building bldg, boolean bSalvo, int hits) {
        if (!bSalvo) {
            // hits!
            r = new Report(2270);
            r.subject = subjectId;
            r.newlines = 0;
            vPhaseReport.addElement(r);
        }
        int tn = wtype.getFireTN();
        if (tn != TargetRoll.IMPOSSIBLE) {
            if (bldg != null) {
                tn += bldg.getType() - 1;
            }
            Report.addNewline(vPhaseReport);
            server.tryIgniteHex(target.getPosition(), subjectId, false, tn,
                    true, vPhaseReport);
        }
    }

    protected void handleClearDamage(Vector<Report> vPhaseReport,
            Building bldg, int nDamage, boolean bSalvo) {
        if (!bSalvo) {
            // hits!
            r = new Report(2270);
            r.subject = subjectId;
            r.newlines = 0;
            vPhaseReport.addElement(r);
        }
        // report that damage was "applied" to terrain
        r = new Report(3385);
        r.indent();
        r.subject = subjectId;
        r.add(nDamage);
        vPhaseReport.addElement(r);

        // Any clear attempt can result in accidental ignition, even
        // weapons that can't normally start fires. that's weird.
        // Buildings can't be accidentally ignited.
        if (bldg != null
                && server.tryIgniteHex(target.getPosition(), subjectId, false,
                        9, vPhaseReport)) {
            return;
        }
        server.tryClearHex(target.getPosition(), nDamage, subjectId);
        return;
    }

    protected void handleBuildingDamage(Vector<Report> vPhaseReport,
            Building bldg, int nDamage, boolean bSalvo) {
        if (!bSalvo) {
            // hits!
            r = new Report(3390);
            r.subject = subjectId;
            vPhaseReport.addElement(r);
        }
        Report.addNewline(vPhaseReport);
        Vector<Report> buildingReport = server.damageBuilding(bldg, nDamage);
        for (Report report : buildingReport) {
            report.subject = subjectId;
        }
        vPhaseReport.addAll(buildingReport);

        // Damage any infantry in the hex.
        server.damageInfantryIn(bldg, nDamage);
    }

    protected boolean allShotsHit() {
        if (((target.getTargetType() == Targetable.TYPE_BLDG_IGNITE || target
                .getTargetType() == Targetable.TYPE_BUILDING) && nRange <= 1)
                || target.getTargetType() == Targetable.TYPE_HEX_CLEAR) {
            return true;
        }
        return false;
    }

    protected void reportMiss(Vector<Report> vPhaseReport) {
        // Report the miss.
        r = new Report(3220);
        r.subject = subjectId;
        vPhaseReport.addElement(r);
    }

    /**
     * Used for deserialization. DO NOT USE OTHERWISE.
     */
    protected WeaponHandler() {
    }

    // Among other things, basically a refactored Server#preTreatWeaponAttack
    public WeaponHandler(ToHitData t, WeaponAttackAction w, IGame g, Server s) {
        damageType = DamageType.NONE;
        toHit = t;
        waa = w;
        game = g;
        ae = game.getEntity(waa.getEntityId());
        weapon = ae.getEquipment(waa.getWeaponId());
        wtype = (WeaponType) weapon.getType();
        target = game.getTarget(waa.getTargetType(), waa.getTargetId());
        server = s;
        subjectId = getAttackerId();
        nRange = ae.getPosition().distance(target.getPosition());
        if (target instanceof Mech) {
            throughFront = Compute.isThroughFrontHex(game, ae.getPosition(),
                    (Entity) target);
        } else {
            throughFront = true;
        }
        roll = Compute.d6(2);
        // use ammo when creating this, so it works when shooting the last shot
        // a unit has and we fire multiple weapons of the same type
        useAmmo();
    }

    protected void useAmmo() {
        setDone();
    }

    protected void setDone() {
        weapon.setUsedThisRound(true);
    }

    protected void addHeat() {
        if (!(toHit.getValue() == TargetRoll.IMPOSSIBLE)) {
            ae.heatBuildup += (wtype.getHeat());
        }
    }

    /**
     * special resolution, like minefields and arty
     * 
     * @param vPhaseReport - a <code>Vector</code> containing the phase report
     * @param entityTarget - the <code>Entity</code> targeted, or
     *            <code>null</code>, if no Entity targeted
     * @param bMissed - a <code>boolean</code> value indicating wether the
     *            attack missed or hit
     * @return true when done with processing, false when not
     */
    protected boolean specialResolution(Vector<Report> vPhaseReport,
            Entity entityTarget, boolean bMissed) {
        return false;
    }

    public boolean announcedEntityFiring() {
        return announcedEntityFiring;
    }

    public void setAnnouncedEntityFiring(boolean announcedEntityFiring) {
        this.announcedEntityFiring = announcedEntityFiring;
    }

    public WeaponAttackAction getWaa() {
        return waa;
    }

}

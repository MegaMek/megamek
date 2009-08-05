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

import megamek.common.Aero;
import megamek.common.BattleArmor;
import megamek.common.Building;
import megamek.common.Compute;
import megamek.common.Coords;
import megamek.common.Entity;
import megamek.common.HitData;
import megamek.common.IGame;
import megamek.common.ITerrain;
import megamek.common.Infantry;
import megamek.common.Mech;
import megamek.common.Mounted;
import megamek.common.RangeType;
import megamek.common.Report;
import megamek.common.TargetRoll;
import megamek.common.Targetable;
import megamek.common.Terrains;
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
    protected boolean bDirect = false;
    protected boolean nukeS2S = false;
    protected WeaponType wtype;
    protected Mounted weapon;
    protected Entity ae;
    protected Targetable target;
    protected int subjectId;
    protected int nRange;
    protected int nDamPerHit;
    protected int attackValue;
    protected boolean throughFront;
    protected boolean underWater;
    protected boolean announcedEntityFiring = false;
    protected boolean missed = false;
    protected DamageType damageType;
    protected int generalDamageType = HitData.DAMAGE_NONE;
    protected Vector<Integer> insertedAttacks = new Vector<Integer>();
    protected int nweapons; //for capital fighters/fighter squadrons


    /**
     * return the <code>int</code> Id of the attacking <code>Entity</code>
     */
    public int getAttackerId() {
        return ae.getId();
    }

    /**
     * Do we care about the specified phase?
     */
    public boolean cares(IGame.Phase phase) {
        if (phase == IGame.Phase.PHASE_FIRING) {
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
        if ((entityTarget != null)
                && ((bldg == null) && (wtype.getFireTN() != TargetRoll.IMPOSSIBLE))) {
            server.tryIgniteHex(target.getPosition(), subjectId, false, false, new TargetRoll(wtype.getFireTN(), wtype.getName()),
                    3, vPhaseReport);
        }

        //shots that miss an entity can also potential cause explosions in a heavy industrial hex
        server.checkExplodeIndustrialZone(target.getPosition(), vPhaseReport);

        // BMRr, pg. 51: "All shots that were aimed at a target inside
        // a building and miss do full damage to the building instead."
        if (!targetInBuilding || (toHit.getValue() == TargetRoll.AUTOMATIC_FAIL)) {
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
        if ((ae instanceof BattleArmor)
                && (weapon.getLocation() == BattleArmor.LOC_SQUAD)
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
        // HACK: during a swarm, when they are not already resolved,
        // get any other attacks by this unit at the target that
        // are also automatic successes, and add their damage
        // if they have already been resolved, just report how much they added
        // to the damage
        if (ae instanceof BattleArmor) {
            BattleArmor ba = (BattleArmor)ae;
            if (!ba.isAttacksDuringSwarmResolved()) {
                for (AttackHandler ah : server.getGame().getAttacksVector()) {
                    if ((ah.getAttackerId() == subjectId)
                            && (ah.getWaa().getWeaponId() != waa.getWeaponId())
                            && (ah.getWaa().getTargetId() == target.getTargetId())
                            && (((WeaponHandler)ah).toHit.getValue() == TargetRoll.AUTOMATIC_SUCCESS)) {
                        WeaponType wtype = (WeaponType)ba.getEquipment(ah.getWaa().getWeaponId()).getType();
                        // damage to add to the original attack's damage,
                        // so we apply one block of damage to the target
                        int addToDamage = wtype.getDamage(nRange);
                        // if it's a squad mounted weapon, each trooper hits
                        if (ba.getEquipment().get(ah.getWaa().getWeaponId()).getLocation() == BattleArmor.LOC_SQUAD) {
                            addToDamage *= ba.getShootingStrength();
                        }
                        nDamPerHit += addToDamage;
                    }
                }
                ba.setAttacksDuringSwarmResolved(true);
            } else {
                r = new Report(3375);
                r.subject = subjectId;
                r.add(wtype.getDamage(nRange));
                r.indent(2);
                vPhaseReport.add(r);
                return 0;
            }
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
    public boolean handle(IGame.Phase phase, Vector<Report> vPhaseReport) {
        if (!cares(phase)) {
            return true;
        }

        insertAttacks(phase, vPhaseReport);

        Entity entityTarget = (target.getTargetType() == Targetable.TYPE_ENTITY) ? (Entity) target
                : null;
        final boolean targetInBuilding = Compute.isInBuilding(game,
                entityTarget);

        if (entityTarget != null) {
            ae.setLastTarget(entityTarget.getId());
        }
        // Which building takes the damage?
        Building bldg = game.getBoard().getBuildingAt(target.getPosition());
        String number = nweapons > 1 ? " (" + nweapons + ")" : "";
        // Report weapon attack and its to-hit value.
        r = new Report(3115);
        r.indent();
        r.newlines = 0;
        r.subject = subjectId;
        r.add(wtype.getName() + number);
        if (entityTarget != null) {
            r.addDesc(entityTarget);
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
            return false;
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
                r.subject = ae.getId();
                r.newlines = 0;
                vPhaseReport.addElement(r);
            } else {
                bGlancing = false;
            }
        } else {
            bGlancing = false;
        }

        //Set Margin of Success/Failure.
        toHit.setMoS(roll-Math.max(2,toHit.getValue()));
        bDirect = game.getOptions().booleanOption("tacops_direct_blow") && ((toHit.getMoS()/3) >= 1) && (entityTarget != null);
        if (bDirect) {
            r = new Report(3189);
            r.subject = ae.getId();
            r.newlines = 0;
            vPhaseReport.addElement(r);
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
        int hits = 1;
        if(!(ae instanceof Aero)) {
            hits = calcHits(vPhaseReport);
        }
        int nCluster = calcnCluster();

        //Now I need to adjust this for air-to-air attacks because they use attack values and different rules
        if((ae instanceof Aero) && (target instanceof Aero)) {
            //this will work differently for cluster and non-cluster weapons, and differently for capital fighter/fighter squadrons
            if(wtype.hasFlag(WeaponType.F_SPACE_BOMB)) {
                bSalvo = true;
                nDamPerHit = 1;
                hits = attackValue;
                nCluster = 5;
            } else if(ae.isCapitalFighter()) {
                bSalvo = true;
                int nhit = 1;
                if(nweapons > 1) {
                    nhit = Compute.missilesHit(nweapons, ((Aero)ae).getClusterMods());
                    r = new Report(3325);
                    r.subject = subjectId;
                    r.add(nhit);
                    r.add(" weapon(s) ");
                    r.add(" ");
                    r.newlines = 0;
                    vPhaseReport.add(r);
                }
                nDamPerHit = attackValue * nhit;
                hits = 1;
                nCluster = 1;
            } else if(usesClusterTable() && (entityTarget != null) && !entityTarget.isCapitalScale()) {
                bSalvo = true;
                nDamPerHit = 1;
                hits = attackValue;
                nCluster = 5;
            } else {
                nDamPerHit = attackValue;
                hits = 1;
                nCluster = 1;
            }
        }

        if (bMissed) {
            return false;

        } // End missed-target

        // The building shields all units from a certain amount of damage.
        // The amount is based upon the building's CF at the phase's start.
        int bldgAbsorbs = 0;
        if (targetInBuilding && (bldg != null)) {
            bldgAbsorbs = (int) Math.ceil(bldg.getPhaseCF(target.getPosition()) / 10.0);
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
                handleBuildingDamage(vPhaseReport, bldg, nDamage, bSalvo, target.getPosition());
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
        int nRange = ae.getPosition().distance(target.getPosition());
        double toReturn = wtype.getDamage(nRange);
        // during a swarm, all damage gets applied as one block to one location
        if ((ae instanceof BattleArmor)
                && (ae.getSwarmTargetId() == target.getTargetId())) {
            BattleArmor ba = (BattleArmor)ae;
            if (weapon.getLocation() == BattleArmor.LOC_SQUAD) {
                toReturn *= ba.getShootingStrength();
            }
        }
        // we default to direct fire weapons for anti-infantry damage
        if ((target instanceof Infantry) && !(target instanceof BattleArmor)) {
            toReturn = Compute.directBlowInfantryDamage(toReturn, bDirect ? toHit.getMoS()/3 : 0, Compute.WEAPON_DIRECT_FIRE, ((Infantry)target).isMechanized());
        } else if ( bDirect ){
            toReturn = Math.min(toReturn+(toHit.getMoS()/3), toReturn*2);
        }

        if (bGlancing) {
            toReturn = (int) Math.floor(toReturn / 2.0);
        }

        if (game.getOptions().booleanOption("tacops_range") && (nRange > wtype.getRanges(weapon)[RangeType.RANGE_LONG])) {
            toReturn = (int) Math.floor(toReturn * .75);
        }
        return (int) toReturn;
    }

    /**
     * Calculate the attack value based on range
     *
     * @return an <code>int</code> representing the attack value at that range.
     */
    protected int calcAttackValue() {
        int distance = ae.getPosition().distance(target.getPosition());
        int av = 0;
        int range = RangeType.rangeBracket(distance, wtype.getATRanges(), true);
        if(range == WeaponType.RANGE_SHORT) {
            av = wtype.getRoundShortAV();
        } else if(range == WeaponType.RANGE_MED) {
            av = wtype.getRoundMedAV();
        } else if (range == WeaponType.RANGE_LONG) {
            av = wtype.getRoundLongAV();
        } else if (range == WeaponType.RANGE_EXT) {
            av = wtype.getRoundExtAV();
        }
        return av;
    }

    /****
     * adjustment factor on attack value for fighter squadrons
     */
    protected double getBracketingMultiplier() {
        double mult = 1.0;
        if(wtype.hasModes() && weapon.curMode().equals("Bracket 80%")) {
            mult = 0.8;
        }
        if(wtype.hasModes() && weapon.curMode().equals("Bracket 60%")) {
            mult = 0.6;
        }
        if(wtype.hasModes() && weapon.curMode().equals("Bracket 40%")) {
            mult = 0.4;
        }
        return mult;
    }

    /*
     * Return the capital missile target for criticals. Zero if not a capital missile
     */
    protected int getCapMisMod() {
        return 0;
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
        hit.setGeneralDamageType(generalDamageType);
        hit.setCapital(wtype.isCapital());
        hit.setBoxCars(roll == 12);
        hit.setCapMisCritMod(getCapMisMod());
        if(weapon.isWeaponGroup()) {
            hit.setSingleAV(attackValue);
        }
        boolean isIndirect = wtype.hasModes() && weapon.curMode().equals("Indirect");

        if (!isIndirect && entityTarget.removePartialCoverHits(hit.getLocation(), toHit
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

        if ( bDirect ){
            hit.makeDirectBlow(toHit.getMoS()/3);
        }
        // A building may be damaged, even if the squad is not.
        if (bldgAbsorbs > 0) {
            int toBldg = Math.min(bldgAbsorbs, nDamage);
            nDamage -= toBldg;
            Report.addNewline(vPhaseReport);
            Vector<Report> buildingReport = server.damageBuilding(bldg, toBldg, entityTarget.getPosition());
            for (Report report : buildingReport) {
                report.subject = subjectId;
            }
            vPhaseReport.addAll(buildingReport);
        }

        nDamage = checkTerrain(nDamage, entityTarget, vPhaseReport);

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
                                    : damageType, false, false, throughFront, underWater, nukeS2S));
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
        TargetRoll tn = new TargetRoll(wtype.getFireTN(), wtype.getName());
        if (tn.getValue() != TargetRoll.IMPOSSIBLE) {
            Report.addNewline(vPhaseReport);
            server.tryIgniteHex(target.getPosition(), subjectId, false, false, tn,
                    true, -1, vPhaseReport);
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
        //TODO: change this for TacOps - now you roll another 2d6 first and on a 5 or less
        //you do a normal ignition as though for intentional fires
        if ((bldg != null)
                && server.tryIgniteHex(target.getPosition(), subjectId, false, false,
                        new TargetRoll(wtype.getFireTN(), wtype.getName()), 5, vPhaseReport)) {
            return;
        }
        vPhaseReport.addAll(server.tryClearHex(target.getPosition(), nDamage, subjectId));
        return;
    }

    protected void handleBuildingDamage(Vector<Report> vPhaseReport,
            Building bldg, int nDamage, boolean bSalvo, Coords coords) {
        if (!bSalvo) {
            // hits!
            r = new Report(3390);
            r.subject = subjectId;
            vPhaseReport.addElement(r);
        }
        Report.addNewline(vPhaseReport);
        Vector<Report> buildingReport = server.damageBuilding(bldg, nDamage, coords);
        for (Report report : buildingReport) {
            report.subject = subjectId;
        }
        vPhaseReport.addAll(buildingReport);

        // Damage any infantry in the hex.
        server.damageInfantryIn(bldg, nDamage, coords);
    }

    protected boolean allShotsHit() {
        if ((((target.getTargetType() == Targetable.TYPE_BLDG_IGNITE) || (target
                .getTargetType() == Targetable.TYPE_BUILDING)) && (nRange <= 1))
                || (target.getTargetType() == Targetable.TYPE_HEX_CLEAR)) {
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
        //is this an underwater attack on a surface naval vessel?
        underWater = toHit.getHitTable() == ToHitData.HIT_UNDERWATER;
        roll = Compute.d6(2);
        nweapons = getNumberWeapons();
        // use ammo when creating this, so it works when shooting the last shot
        // a unit has and we fire multiple weapons of the same type
        //TODO: need to adjust this for cases where not all the ammo is available
        for(int i=0;i<nweapons;i++) {
            useAmmo();
        }
        attackValue = (int)Math.floor(getBracketingMultiplier() * calcAttackValue());
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
     * Does this attack use the cluster hit table?
     * necessary to determine how Aero damage should be applied
     */
    protected boolean usesClusterTable() {
        return false;
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

    public int checkTerrain(int nDamage, Entity entityTarget, Vector<Report>vPhaseReport){
        if ( game.getOptions().booleanOption("tacops_woods_cover") &&
                (game.getBoard().getHex(entityTarget.getPosition()).containsTerrain(Terrains.WOODS)
                || game.getBoard().getHex(entityTarget.getPosition()).containsTerrain(Terrains.JUNGLE))
                && !(entityTarget.getSwarmAttackerId() == ae.getId())) {
            ITerrain woodHex = game.getBoard().getHex(entityTarget.getPosition()).getTerrain(Terrains.WOODS);
            ITerrain jungleHex = game.getBoard().getHex(entityTarget.getPosition()).getTerrain(Terrains.JUNGLE);
            int treeAbsorbs = 0;
            String hexType = "";
            if ( woodHex != null ){
                treeAbsorbs = woodHex.getLevel() * 2;
                hexType = "wooded";
            }else if (jungleHex != null){
                treeAbsorbs = jungleHex.getLevel() * 2;
                hexType = "jungle";
            }

            //Do not absorb more damage then the weapon can do.
            treeAbsorbs = Math.min(nDamage, treeAbsorbs);

            nDamage = Math.max(0, nDamage-treeAbsorbs);
            server.tryClearHex(entityTarget.getPosition(), treeAbsorbs, ae.getId());
            Report.addNewline(vPhaseReport);
            Report r = new Report(6427);
            r.subject = entityTarget.getId();
            r.add(hexType);
            r.add(treeAbsorbs);
            r.indent(2);
            r.newlines = 0;
            vPhaseReport.add(r);
        }
        return nDamage;
    }

    protected boolean canDoDirectBlowDamage(){
        return true;
    }

    /**
     * Insert any additionaly attacks that should occur before this attack
     */
    protected void insertAttacks(IGame.Phase phase, Vector<Report> vPhaseReport) {
        return;
    }

    /**
     * @return the number of weapons of this type firing (for squadron weapon groups)
     */
    protected int getNumberWeapons() {
        return weapon.getNWeapons();
    }
}

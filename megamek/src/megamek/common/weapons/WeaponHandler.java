/*
 * MegaMek
 * Copyright (c) 2004-2005 - Ben Mazur (bmazur@sev.org)
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

import megamek.common.*;
import megamek.common.actions.TeleMissileAttackAction;
import megamek.common.actions.WeaponAttackAction;
import megamek.common.enums.AimingMode;
import megamek.common.enums.GamePhase;
import megamek.common.options.OptionsConstants;
import megamek.server.Server;
import megamek.server.Server.DamageType;
import megamek.server.SmokeCloud;
import org.apache.logging.log4j.LogManager;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.Vector;

/**
 * A basic, simple attack handler. May or may not work for any particular weapon; must be overloaded
 * to support special rules.
 * @author Andrew Hunter
 */
public class WeaponHandler implements AttackHandler, Serializable {

    private static final long serialVersionUID = 7137408139594693559L;
    public ToHitData toHit;
    protected HitData hit;
    public WeaponAttackAction waa;
    public int roll;
    protected boolean isJammed = false;

    protected Game game;
    protected transient Server server; // must not save the server
    protected boolean bMissed;
    protected boolean bSalvo = false;
    protected boolean bGlancing = false;
    protected boolean bDirect = false;
    protected boolean bLowProfileGlancing = false;
    protected boolean nukeS2S = false;
    protected WeaponType wtype;
    protected String typeName;
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
    protected Vector<Integer> insertedAttacks = new Vector<>();
    protected int nweapons; // for capital fighters/fighter squadrons
    protected int nweaponsHit; // for capital fighters/fighter squadrons
    protected boolean secondShot = false;
    protected int numRapidFireHits;
    protected String sSalvoType = " shot(s) ";
    protected int nSalvoBonus = 0;
    /**
     * Keeps track of whether we are processing the first hit in a series of
     * hits (like for cluster weapons)
     */
    protected boolean firstHit = true;

    /**
     * Boolean flag that determines whether or not this attack is part of a
     * strafing run.
     */
    protected boolean isStrafing = false;

    /**
     * Boolean flag that determiens if this shot was the first one by a
     * particular weapon in a strafing run. Used to ensure that heat is only
     * added once.
     */
    protected boolean isStrafingFirstShot = false;
    
    // Large Craft Point Defense/AMS Bay Stuff
    protected int CounterAV; // the combined attack value of all point defenses used against this weapon attack
    protected int CapMissileArmor; // the standard scale armor points of a capital missile bay
    protected int CapMissileAMSMod; // the to-hit mod inflicted against a capital missile attack if it isn't completely destroyed
    protected boolean CapMissileMissed = false; //true if the AMSmod causes a capital missile attack to miss. Used for reporting.
    protected boolean amsBayEngaged = false; //true if one or more AMS bays engages this attack. Used for reporting if this is a standard missile (LRM, MRM, etc) attack.
    protected boolean pdBayEngaged = false; // true if one or more point defense bays engages this attack. Used for reporting if this is a standard missile (LRM, MRM, etc) attack.
    protected boolean pdOverheated = false; // true if counterfire + offensive weapon attacks made this round cause the defending unit to overheat. Used for reporting.
    protected boolean amsBayEngagedCap = false; //true if one or more AMS bays engages this attack. Used for reporting if this is a capital missile attack.
    protected boolean pdBayEngagedCap = false; // true if one or more point defense bays engages this attack. Used for reporting if this is a capital missile attack.
    protected boolean amsBayEngagedMissile = false; // true if one or more AMS bays engages this attack. Used for reporting if this is a single large missile (thunderbolt, etc) attack.
    protected boolean pdBayEngagedMissile = false; // true if one or more point defense bays engages this attack. Used for reporting if this is a single large missile (thunderbolt, etc) attack.
    protected boolean advancedPD = false; // true if advanced StratOps game rule is on
    protected WeaponHandler parentBayHandler = null; //Used for weapons bays when Aero Sanity is on
    protected int originalAV = 0; // Used to handle AMS damage to standard missile flights fired by capital fighters
    
    protected boolean amsEngaged = false;
    protected boolean apdsEngaged = false;
    
    /**
     * Returns the heat generated by a large craft's weapons fire declarations during the round
     * Used to determine whether point defenses can engage.
     * @param e the entity you wish to get heat data from
     * @see TeleMissileAttackAction which contains a modified version of this to work against a
     * TeleMissile entity in the physical phase
     */
    protected int getLargeCraftHeat(Entity e) {
        int totalheat = 0;
        if (e.hasETypeFlag(Entity.ETYPE_DROPSHIP) 
                || e.hasETypeFlag(Entity.ETYPE_JUMPSHIP)) {
            if (e.usesWeaponBays()) {
                for (Enumeration<AttackHandler> i = game.getAttacks(); i.hasMoreElements();) {
                    AttackHandler ah = i.nextElement();
                    WeaponAttackAction prevAttack = ah.getWaa();
                    if (prevAttack.getEntityId() == e.getId()) {
                        Mounted prevWeapon = e.getEquipment(prevAttack.getWeaponId());
                        for (int wId : prevWeapon.getBayWeapons()) {
                            Mounted bayW = e.getEquipment(wId);
                            totalheat += bayW.getCurrentHeat();
                        }
                    }
                }
            } else {
                for (Enumeration<AttackHandler> i = game.getAttacks(); i.hasMoreElements();) {
                    AttackHandler ah = i.nextElement();
                    WeaponAttackAction prevAttack = ah.getWaa();
                    if (prevAttack.getEntityId() == e.getId()) {
                        Mounted prevWeapon = e.getEquipment(prevAttack.getWeaponId());
                        totalheat += prevWeapon.getCurrentHeat();
                    }
                }
            }
        }
        return totalheat;
    }
    
    /**
     * Checks to see if the basic conditions needed for point defenses to work are in place
     * Artillery weapons need to change this slightly
     * See also TeleMissileAttackAction, which contains a modified version of this to work against 
     * a TeleMissile entity in the physical phase
     */
    protected boolean checkPDConditions() {
        advancedPD = game.getOptions().booleanOption(OptionsConstants.ADVAERORULES_STRATOPS_ADV_POINTDEF);
        if ((target == null)
                || (target.getTargetType() != Targetable.TYPE_ENTITY)
                || !advancedPD
                //Don't defend against ground fire with bay fire unless attacked by capital missile fire
                //Prevents ammo and heat being used twice for dropships defending here and with getAMSHitsMod()
                || (waa.isGroundToAir(game) && (!(wtype.isSubCapital() || wtype.isCapital())))) {
            return false;
        }
        if (target instanceof Dropship 
                && waa.isAirToGround(game)
                && !ae.usesWeaponBays()) {
            //Prevents a grounded dropship using individual weapons from engaging with AMSBays unless attacked by a dropship or capital fighter
            //You can get some blank missile weapons fire reports due to the attackvalue / ndamageperhit conversion if this isn't done
            return false;
        }
        return true;
    }
    
    /**
     * Checks to see if this point defense/AMS bay can engage a capital missile
     * This should return true. Only when handling capital missile attacks can this be false.
     * See also TeleMissileAttackAction, which contains a modified version of this to work against 
     * a TeleMissile entity in the physical phase
     */
    protected boolean canEngageCapitalMissile(Mounted counter) {
        return true;
    }
    
    /**
     * Sets the appropriate AMS Bay reporting flag depending on what type of missile this is
     * See also TeleMissileAttackAction, which contains a modified version of this to work against 
     * a TeleMissile entity in the physical phase
     */
    protected void setAMSBayReportingFlag() {
    }
    
    /**
     * Sets the appropriate PD Bay reporting flag depending on what type of missile this is
     * See also TeleMissileAttackAction, which contains a modified version of this to work against 
     * a TeleMissile entity in the physical phase
     */
    protected void setPDBayReportingFlag() {
    }
    
    /**
     * Sets whether or not this weapon is considered a single, large missile for AMS resolution
     */
    protected boolean isTbolt() {
        return false;
    }
    
    /**
     * Calculates the attack value of point defense weapons used against a missile bay attack
     * This is the main large craft point defense method
     * See also TeleMissileAttackAction, which contains a modified version of this to work against 
     * a TeleMissile entity in the physical phase
     */    
    protected int calcCounterAV() {
        if (!checkPDConditions()) {
            return 0;
        }
        int counterAV = 0;
        int amsAV = 0;
        double pdAV = 0;
        Entity entityTarget = (Entity) target;
        // any AMS bay attacks by the target?
        ArrayList<Mounted> lCounters = waa.getCounterEquipment();
        // We need to know how much heat has been assigned to offensive weapons fire by the defender this round
        int weaponHeat = getLargeCraftHeat(entityTarget) + entityTarget.heatBuildup;
        if (null != lCounters) {
            for (Mounted counter : lCounters) {
                // Point defenses only fire vs attacks against the arc they protect
                Entity pdEnt = counter.getEntity();
                boolean isInArc;
                // If the defending unit is the target, use attacker for arc
                if (entityTarget.equals(pdEnt)) {
                    isInArc = Compute.isInArc(game, pdEnt.getId(), pdEnt.getEquipmentNum(counter), ae);
                } else { // Otherwise, the attack must pass through an escort unit's hex
                    // TODO: We'll get here, eventually
                    isInArc = Compute.isInArc(game, pdEnt.getId(),
                            pdEnt.getEquipmentNum(counter),
                            entityTarget);
                }
                
                if (!isInArc) {
                    continue;
                }
                // Point defenses can't fire if they're not ready for any other reason
                if (!(counter.getType() instanceof WeaponType)
                         || !counter.isReady() || counter.isMissing()
                            // shutdown means no Point defenses
                            || pdEnt.isShutDown()) {
                        continue;
                }
                // Point defense/AMS bays with less than 2 weapons cannot engage capital missiles
                if (!canEngageCapitalMissile(counter)) {
                    continue;
                }
                
                // Set up differences between point defense and AMS bays
                boolean isAMSBay = counter.getType().hasFlag(WeaponType.F_AMSBAY);
                boolean isPDBay = counter.getType().hasFlag(WeaponType.F_PDBAY);
                
                // Point defense bays can only fire at one attack per round
                if (isPDBay) {
                    if (counter.isUsedThisRound()) {
                        continue;
                    }
                }
                
                // Now for heat, damage and ammo we need the individual weapons in the bay
                // First, reset the temporary damage counters
                amsAV = 0;
                pdAV = 0;
                for (int wId : counter.getBayWeapons()) {
                    Mounted bayW = pdEnt.getEquipment(wId);
                    Mounted bayWAmmo = bayW.getLinked();
                    WeaponType bayWType = ((WeaponType) bayW.getType());
                    
                    // build up some heat
                    // First Check to see if we have enough heat capacity to fire
                    if ((weaponHeat + bayW.getCurrentHeat()) > pdEnt.getHeatCapacity()) {
                        pdOverheated = true;
                        break;
                    }
                    if (counter.getType().hasFlag(WeaponType.F_HEATASDICE)) {
                        int heatDice = Compute.d6(bayW
                                .getCurrentHeat());
                        pdEnt.heatBuildup += heatDice;
                        weaponHeat += heatDice;
                    } else {
                        pdEnt.heatBuildup += bayW.getCurrentHeat();
                        weaponHeat += bayW.getCurrentHeat();
                    }
                    
                    // Bays use lots of ammo. Check to make sure we haven't run out
                    if (bayWAmmo != null) {
                        if (bayWAmmo.getBaseShotsLeft() == 0) {
                            continue;
                        }
                        // decrement the ammo
                        bayWAmmo.setShotsLeft(Math.max(0,
                            bayWAmmo.getBaseShotsLeft() - 1));
                    }

                    if (isAMSBay) {
                        // get the attack value
                        amsAV += (int) Math.round(bayWType.getShortAV());
                        // set the ams as having fired, if it did
                        setAMSBayReportingFlag();
                    }

                    if (isPDBay) {
                        // get the attack value
                        pdAV += bayWType.getShortAV();
                        // set the pdbay as having fired, if it was able to
                        counter.setUsedThisRound(true); 
                        setPDBayReportingFlag();
                    }
                }
                // non-AMS only add half their damage, rounded up
                counterAV += (int) Math.ceil(pdAV / 2.0); 
                // AMS add their full damage
                counterAV += amsAV;
            }
        }

        CounterAV = counterAV;
        return counterAV;
    }

    
    /**
     * Return the attack value of point defense weapons used against a missile bay attack
     */ 
    protected int getCounterAV() {
        return CounterAV;
    }
    
    /**
     * Used with Aero Sanity mod
     * Returns the handler for the BayWeapon this individual weapon belongs to
     */ 
    protected WeaponHandler getParentBayHandler() {
        return parentBayHandler;
    }
    
    /**
     * Sets the parent handler for each sub-weapon handler called when looping through bay weapons
     * Used with Aero Sanity to pass counterAV through to the individual missile handler from the bay handler
     * 
     * @param bh - The <code>AttackHandler</code> for the BayWeapon this individual weapon belongs to
     */ 
    protected void setParentBayHandler(WeaponHandler bh) {
        parentBayHandler = bh;
    }
    
    /**
     * Calculates the to-hit penalty inflicted on a capital missile attack by point defense fire
     * this should return 0 unless this is a capital missile attack (otherwise, reporting and to-hit get screwed up)
     */    
    protected int calcCapMissileAMSMod() {
        return 0;
    }
    
    /**
     * Return the to-hit penalty inflicted on a capital missile attack by point defense fire
     */ 
    protected int getCapMissileAMSMod() {
        return CapMissileAMSMod;
    }
    
    //End of Large Craft Point Defense Methods and Variables
    
    /**
     * Used to store reports from calls to <code>calcDamagePerHit</code>.  This
     * is necessary because the method is called before the report needs to be
     * added.
     */
    protected Vector<Report> calcDmgPerHitReport = new Vector<>();

    /**
     * return the <code>int</code> Id of the attacking <code>Entity</code>
     */
    @Override
    public int getAttackerId() {
        return ae.getId();
    }

    @Override
    public Entity getAttacker() {
        return ae;
    }
    
    /**
     * Do we care about the specified phase?
     */
    @Override
    public boolean cares(GamePhase phase) {
        if (phase == GamePhase.FIRING) {
            return true;
        }
        return false;
    }

    /**
     * @param vPhaseReport
     *            - A <code>Vector</code> containing the phasereport.
     * @return a <code>boolean</code> value indicating wether or not the attack
     *         misses because of a failed check.
     */
    protected boolean doChecks(Vector<Report> vPhaseReport) {
        return false;
    }
    
    /**
     * Carries out a check to see if the weapon in question explodes due to the 'ammo feed problem' quirk
     * Not the case for weapons without ammo
     */
    protected boolean doAmmoFeedProblemCheck(Vector<Report> vPhaseReport) {
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
     * @return a <code>boolean</code> value indicating wether or not this attack
     *         needs further calculating, like a missed shot hitting a building,
     *         or an AMS only shooting down some missiles.
     */
    protected boolean handleSpecialMiss(Entity entityTarget,
            boolean bldgDamagedOnMiss, Building bldg,
            Vector<Report> vPhaseReport) {
        // Shots that miss an entity can set fires.
        // Buildings can't be accidentally ignited,
        // and some weapons can't ignite fires.
        if ((entityTarget != null)
                && !entityTarget.isAirborne()
                && !entityTarget.isAirborneVTOLorWIGE()
                && ((bldg == null) && (wtype.getFireTN() != TargetRoll.IMPOSSIBLE))) {
            server.tryIgniteHex(target.getPosition(), subjectId, false, false,
                    new TargetRoll(wtype.getFireTN(), wtype.getName()), 3,
                    vPhaseReport);
        }

        // shots that miss an entity can also potential cause explosions in a
        // heavy industrial hex
        server.checkExplodeIndustrialZone(target.getPosition(), vPhaseReport);

        // TW, pg. 171 - shots that miss a target in a building don't damage the
        // building, unless the attacker is adjacent
        if (!bldgDamagedOnMiss
                || (toHit.getValue() == TargetRoll.AUTOMATIC_FAIL)) {
            return false;
        }
        return true;
    }

    /**
     * Calculate the number of hits
     *
     * @param vPhaseReport
     *            - the <code>Vector</code> containing the phase report.
     * @return an <code>int</code> containing the number of hits.
     */
    protected int calcHits(Vector<Report> vPhaseReport) {
        // normal BA attacks (non-swarm, non single-trooper weapons)
        // do more than 1 hit
        if ((ae instanceof BattleArmor)
                && (weapon.getLocation() == BattleArmor.LOC_SQUAD)
                && !(weapon.isSquadSupportWeapon())
                && !(ae.getSwarmTargetId() == target.getTargetId())) {
            bSalvo = true;
            int toReturn = allShotsHit() ? ((BattleArmor) ae)
                    .getShootingStrength() : Compute
                    .missilesHit(((BattleArmor) ae).getShootingStrength());
            Report r = new Report(3325);
            r.newlines = 0;
            r.subject = subjectId;
            r.add(toReturn);
            r.add(" troopers ");
            r.add(toHit.getTableDesc());
            vPhaseReport.add(r);
            return toReturn;
        }
        return 1;
    }

    /**
     * Calculate the clustering of the hits
     *
     * @return a <code>int</code> value saying how much hits are in each cluster
     *         of damage.
     */
    protected int calcnCluster() {
        return 1;
    }

    protected int calcnClusterAero(Entity entityTarget) {
        if (usesClusterTable() && !ae.isCapitalFighter()
                && (entityTarget != null) && !entityTarget.isCapitalScale()) {
            return 5;
        } else {
            return 1;
        }
    }

    protected int[] calcAeroDamage(Entity entityTarget,
            Vector<Report> vPhaseReport) {
        // Now I need to adjust this for attacks on aeros because they use
        // attack values and different rules
        // this will work differently for cluster and non-cluster
        // weapons, and differently for capital fighter/fighter
        // squadrons
        if (game.getOptions().booleanOption(OptionsConstants.ADVAERORULES_AERO_SANITY)) {
            // everything will use the normal hits and clusters for hits weapon
            // unless
            // we have a squadron or capital scale entity
            int reportSize = vPhaseReport.size();
            int hits = calcHits(vPhaseReport);
            int nCluster = calcnCluster();
            int AMSHits = 0;
            if (ae.isCapitalFighter()) {
                Vector<Report> throwAwayReport = new Vector<>();
                // for capital scale fighters, each non-cluster weapon hits a
                // different location
                bSalvo = true;
                hits = 1;
                if (nweapons > 1) {
                    if (allShotsHit()) {
                        nweaponsHit = nweapons;
                    } else {
                        nweaponsHit = Compute.missilesHit(nweapons,
                                ((Aero) ae).getClusterMods());
                    }
                    if (usesClusterTable()) {
                        // remove the last reports because they showed the
                        // number of shots that hit
                        while (vPhaseReport.size() > reportSize) {
                            vPhaseReport.remove(vPhaseReport.size() - 1);
                        }
                        hits = 0;
                        for (int i = 0; i < nweaponsHit; i++) {
                            hits += calcHits(throwAwayReport);
                        }
                        //Report and apply point defense fire
                        if (pdBayEngaged || amsBayEngaged) {
                            Report r = new Report(3367);
                            r.indent();
                            r.subject = subjectId;
                            r.add(getCounterAV());
                            r.newlines = 0;
                            vPhaseReport.addElement(r);
                            hits -= (CounterAV / nDamPerHit);
                        } else if (amsEngaged) {
                            Report r = new Report(3350);
                            r.subject = entityTarget.getId();
                            r.newlines = 0;
                            vPhaseReport.add(r);
                        }
                        Report r = new Report(3325);
                        r.subject = subjectId;
                        r.add(hits);
                        r.add(sSalvoType);
                        r.add(toHit.getTableDesc());
                        r.newlines = 0;
                        vPhaseReport.add(r);
                    } else {
                        //If point defenses engage Large, single missiles
                        if (pdBayEngagedMissile || amsBayEngagedMissile) {
                            // remove the last reports because they showed the
                            // number of shots that hit
                            while (vPhaseReport.size() > reportSize) {
                                vPhaseReport.remove(vPhaseReport.size() - 1);
                            }
                            AMSHits = 0;
                            Report r = new Report(3236);
                            r.subject = subjectId;
                            r.add(nweaponsHit);
                            vPhaseReport.add(r);
                            r = new Report(3230);
                            r.indent(1);
                            r.subject = subjectId;
                            vPhaseReport.add(r);
                            for (int i = 0; i < nweaponsHit; i++) {
                                int destroyRoll = Compute.d6();
                                if (destroyRoll <= 3) {
                                    r = new Report(3240);
                                    r.subject = subjectId;
                                    r.add("missile");
                                    r.add(destroyRoll);
                                    vPhaseReport.add(r);
                                    AMSHits += 1;
                                } else {
                                    r = new Report(3241);
                                    r.add("missile");
                                    r.add(destroyRoll);
                                    r.subject = subjectId;
                                    vPhaseReport.add(r);                                
                                }
                            }
                            nweaponsHit = nweaponsHit - AMSHits;
                        } else if (amsEngaged || apdsEngaged) {
                            // remove the last reports because they showed the
                            // number of shots that hit
                            while (vPhaseReport.size() > reportSize) {
                                vPhaseReport.remove(vPhaseReport.size() - 1);
                            }
                            //If you're shooting at a target using single AMS
                            //Too many variables here as far as AMS numbers
                            //Just allow 1 missile to be shot down
                            AMSHits = 0;
                            Report r = new Report(3236);
                            r.subject = subjectId;
                            r.add(nweaponsHit);
                            vPhaseReport.add(r);
                            if (amsEngaged) {
                                r = new Report(3230);
                                r.indent(1);
                                r.subject = subjectId;
                                vPhaseReport.add(r);
                            }
                            if (apdsEngaged) {
                                r = new Report(3231);
                                r.indent(1);
                                r.subject = subjectId;
                                vPhaseReport.add(r);
                            }
                            int destroyRoll = Compute.d6();
                            if (destroyRoll <= 3) {
                                r = new Report(3240);
                                r.subject = subjectId;
                                r.add("missile");
                                r.add(destroyRoll);
                                vPhaseReport.add(r);
                                AMSHits = 1;
                            } else {
                                r = new Report(3241);
                                r.add("missile");
                                r.add(destroyRoll);
                                r.subject = subjectId;
                                vPhaseReport.add(r);                                
                            }
                            nweaponsHit = nweaponsHit - AMSHits;
                        }
                        nCluster = 1;
                        if (!bMissed) {
                            Report r = new Report(3325);
                            r.subject = subjectId;
                            r.add(nweaponsHit);
                            r.add(" weapon(s) ");
                            r.add(" ");
                            r.newlines = 0;
                            hits = nweaponsHit;
                            vPhaseReport.add(r);
                        }
                    }
                }
            }
            int[] results = new int[2];
            results[0] = hits;
            results[1] = nCluster;
            return results;
        } else {
            int hits = 1;
            int nCluster = calcnClusterAero(entityTarget);
            if (ae.isCapitalFighter()) {
                bSalvo = false;
                if (nweapons > 1) {
                    nweaponsHit = Compute.missilesHit(nweapons,
                            ((IAero) ae).getClusterMods());
                    if (pdBayEngaged || amsBayEngaged) {
                        //Point Defenses engage standard (cluster) missiles
                        int counterAV = 0;
                        counterAV = getCounterAV();
                        nDamPerHit = originalAV * nweaponsHit - counterAV;
                        hits = 1;
                        nCluster = 1;
                    } else {
                        //If multiple large missile or non-missile weapons hit
                        Report r = new Report(3325);
                        r.subject = subjectId;
                        r.add(nweaponsHit);
                        r.add(" weapon(s) ");
                        r.add(" ");
                        r.newlines = 1;
                        vPhaseReport.add(r);
                        nDamPerHit = attackValue * nweaponsHit;
                        hits = 1;
                        nCluster = 1;
                    }
                } 
            } else if (nCluster > 1) {
                bSalvo = true;
                nDamPerHit = 1;
                hits = attackValue;
            } else {
                //If we're not a capital fighter / squadron
                //Point Defenses engage any Large, single missiles
                getCounterAV();
                if (pdBayEngagedMissile || amsBayEngagedMissile) {
                    bSalvo = false;
                    Report r = new Report(3235);
                    r.subject = subjectId;
                    vPhaseReport.add(r);
                    r = new Report(3230);
                    r.indent(1);
                    r.subject = subjectId;
                    vPhaseReport.add(r);
                    for (int i = 0; i < nweaponsHit; i++) {
                        int destroyRoll = Compute.d6();
                        if (destroyRoll <= 3) {
                            r = new Report(3240);
                            r.subject = subjectId;
                            r.add("missile");
                            r.add(destroyRoll);
                            vPhaseReport.add(r);
                            hits = 0;
                        } else {
                            r = new Report(3241);
                            r.add("missile");
                            r.add(destroyRoll);
                            r.subject = subjectId;
                            vPhaseReport.add(r);
                            hits = 1;
                        }
                    }
                } else {
                bSalvo = false;
                nDamPerHit = attackValue;
                hits = 1;
                nCluster = 1;
                }
            }
            int[] results = new int[2];
            results[0] = hits;
            results[1] = nCluster;
            return results;
        }
    }

    /**
     * handle this weapons firing
     *
     * @return a <code>boolean</code> value indicating whether this should be
     *         kept or not
     */
    @Override
    public boolean handle(GamePhase phase, Vector<Report> returnedReports) {
        if (!cares(phase)) {
            return true;
        }
        Vector<Report> vPhaseReport = new Vector<>();

        boolean heatAdded = false;
        int numAttacks = 1;
        if (game.getOptions().booleanOption(OptionsConstants.ADVCOMBAT_UAC_TWOROLLS)
                && ((wtype.getAmmoType() == AmmoType.T_AC_ULTRA) || (wtype
                        .getAmmoType() == AmmoType.T_AC_ULTRA_THB))
                && !weapon.curMode().equals("Single")) {
            numAttacks = 2;
        }

        Entity entityTarget = (target.getTargetType() == Targetable.TYPE_ENTITY) ? (Entity) target
                : null;
        final boolean targetInBuilding = Compute.isInBuilding(game,
                entityTarget);
        final boolean bldgDamagedOnMiss = targetInBuilding
                && !(target instanceof Infantry)
                && ae.getPosition().distance(target.getPosition()) <= 1;

        if (entityTarget != null) {
            ae.setLastTarget(entityTarget.getId());
            ae.setLastTargetDisplayName(entityTarget.getDisplayName());
        }
        // Which building takes the damage?
        Building bldg = game.getBoard().getBuildingAt(target.getPosition());
        String number = nweapons > 1 ? " (" + nweapons + ")" : "";
        for (int i = numAttacks; i > 0; i--) {
            // Report weapon attack and its to-hit value.
            Report r = new Report(3115);
            r.indent();
            r.newlines = 0;
            r.subject = subjectId;
            r.add(wtype.getName() + number);
            if (entityTarget != null) {
                if ((wtype.getAmmoType() != AmmoType.T_NA)
                        && (weapon.getLinked() != null)
                        && (weapon.getLinked().getType() instanceof AmmoType)) {
                    AmmoType atype = (AmmoType) weapon.getLinked().getType();
                    if (atype.getMunitionType() != AmmoType.M_STANDARD) {
                        r.messageId = 3116;
                        r.add(atype.getSubMunitionName());
                    }
                }
                r.addDesc(entityTarget);
            } else {
                r.messageId = 3120;
                r.add(target.getDisplayName(), true);
            }
            vPhaseReport.addElement(r);
            
            //Point Defense fire vs Capital Missiles
            
            // are we a glancing hit?  Check for this here, report it later
            setGlancingBlowFlags(entityTarget);
            
            // Set Margin of Success/Failure and check for Direct Blows
            toHit.setMoS(roll - Math.max(2, toHit.getValue()));
            bDirect = game.getOptions().booleanOption(OptionsConstants.ADVCOMBAT_TACOPS_DIRECT_BLOW)
                    && ((toHit.getMoS() / 3) >= 1) && (entityTarget != null);

            //This has to be up here so that we don't screw up glancing/direct blow reports
            attackValue = calcAttackValue();

            //CalcAttackValue triggers counterfire, so now we can safely get this
            CapMissileAMSMod = getCapMissileAMSMod();

            //Only do this if the missile wasn't destroyed
            if (CapMissileAMSMod > 0 && CapMissileArmor > 0) {
                toHit.addModifier(CapMissileAMSMod, "Damage from Point Defenses");
                if (roll < toHit.getValue()) {
                    CapMissileMissed = true;
                }
            }

            // Report any AMS bay action against Capital missiles that doesn't destroy them all.
            if (amsBayEngagedCap && CapMissileArmor > 0) {
                r = new Report(3358);
                r.add(CapMissileAMSMod);
                r.subject = subjectId;
                vPhaseReport.addElement(r);

            // Report any PD bay action against Capital missiles that doesn't destroy them all.
            } else if (pdBayEngagedCap && CapMissileArmor > 0) {
                r = new Report(3357);
                r.add(CapMissileAMSMod);
                r.subject = subjectId;
                vPhaseReport.addElement(r);
            }

            // Report AMS/Pointdefense failure due to Overheating.
            if (pdOverheated 
                    && (!(amsBayEngaged
                            || amsBayEngagedCap
                            || amsBayEngagedMissile
                            || pdBayEngaged
                            || pdBayEngagedCap
                            || pdBayEngagedMissile))) {
                r = new Report (3359);
                r.subject = subjectId;
                r.indent();
                vPhaseReport.addElement(r);
            } else if (pdOverheated) {
                //Report a partial failure
                r = new Report (3361);
                r.subject = subjectId;
                r.indent();
                vPhaseReport.addElement(r); 
            }

            if (toHit.getValue() == TargetRoll.IMPOSSIBLE) {
                r = new Report(3135);
                r.subject = subjectId;
                r.add(toHit.getDesc());
                vPhaseReport.addElement(r);
                returnedReports.addAll(vPhaseReport);
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
                r.add(toHit);
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
       

            //Report Glancing/Direct Blow here because of Capital Missile weirdness
            //TODO: Can't figure out a good way to make Capital Missile bays report direct/glancing blows
            //when Advanced Point Defense is on, but they work correctly.
            if (!(amsBayEngagedCap || pdBayEngagedCap)) {
                addGlancingBlowReports(vPhaseReport);
    
                if (bDirect) {
                    r = new Report(3189);
                    r.subject = ae.getId();
                    r.newlines = 0;
                    vPhaseReport.addElement(r);
                }
            }

            // Do this stuff first, because some weapon's miss report reference
            // the amount of shots fired and stuff.
            Vector<Report> dmgPerHitReport = new Vector<>();
            nDamPerHit = calcDamagePerHit();
            if (!heatAdded) {
                addHeat();
                heatAdded = true;
            }

            
            
            // Report any AMS bay action against standard missiles.
            CounterAV = getCounterAV();
            //use this if counterfire destroys all the missiles
            if (amsBayEngaged && (attackValue <= 0)) {
                r = new Report(3356);
                r.indent();
                r.subject = subjectId;
                vPhaseReport.addElement(r);
            } else if (amsBayEngaged) {
                r = new Report(3354);
                r.indent();
                r.add(CounterAV);
                r.subject = subjectId;
                vPhaseReport.addElement(r);
            }

            //use this if AMS counterfire destroys all the Capital missiles
            if (amsBayEngagedCap && (CapMissileArmor <= 0)) {
                r = new Report(3356);
                r.indent();
                r.subject = subjectId;
                vPhaseReport.addElement(r);
            } 

            // Report any Point Defense bay action against standard missiles.
            if (pdBayEngaged && (attackValue <= 0)) {
                r = new Report(3355);
                r.subject = subjectId;
                vPhaseReport.addElement(r);
            } else if (pdBayEngaged) {
                r = new Report(3353);
                r.add(CounterAV);
                r.subject = subjectId;
                vPhaseReport.addElement(r);
            }

            //use this if PD counterfire destroys all the Capital missiles
            if (pdBayEngagedCap && (CapMissileArmor <= 0)) {
                r = new Report(3355);
                r.indent();
                r.subject = subjectId;
                vPhaseReport.addElement(r);
            }

            // Any necessary PSRs, jam checks, etc.
            // If this boolean is true, don't report
            // the miss later, as we already reported
            // it in doChecks
            boolean missReported = doChecks(vPhaseReport);
            if (missReported) {
                bMissed = true;
            }

            // Do we need some sort of special resolution (minefields,
            // artillery,
            if (specialResolution(vPhaseReport, entityTarget) && (i < 2)) {
                returnedReports.addAll(vPhaseReport);
                return false;
            }

            if (bMissed && !missReported) {
                if (game.getOptions().booleanOption(OptionsConstants.ADVCOMBAT_UAC_TWOROLLS)
                        && ((wtype.getAmmoType() == AmmoType.T_AC_ULTRA) || (wtype
                                .getAmmoType() == AmmoType.T_AC_ULTRA_THB))
                        && (i == 2)) {
                    reportMiss(vPhaseReport, true);
                } else {
                    reportMiss(vPhaseReport);
                }

                // Works out fire setting, AMS shots, and whether continuation
                // is necessary.
                if (!handleSpecialMiss(entityTarget, bldgDamagedOnMiss, bldg,
                        vPhaseReport) && (i < 2)) {
                    returnedReports.addAll(vPhaseReport);
                    return false;
                }
            }

            // yeech. handle damage. . different weapons do this in very
            // different
            // ways
            int nCluster = calcnCluster();
            int id = vPhaseReport.size();
            int hits = calcHits(vPhaseReport);
            if ((target.isAirborne() && !waa.isGroundToAir(game)) || game.getBoard().inSpace() || ae.usesWeaponBays()) {
                // if we added a line to the phase report for calc hits, remove
                // it now
                while (vPhaseReport.size() > id) {
                    vPhaseReport.removeElementAt(vPhaseReport.size() - 1);
                }
                int[] aeroResults = calcAeroDamage(entityTarget, vPhaseReport);
                hits = aeroResults[0];
                // If our capital missile was destroyed, it shouldn't hit
                if ((amsBayEngagedCap || pdBayEngagedCap) && (CapMissileArmor <= 0)) {
                    hits = 0;
                }
                nCluster = aeroResults[1];
            }

            // We have to adjust the reports on a miss, so they line up
            if (bMissed && id != vPhaseReport.size()) {
                vPhaseReport.get(id - 1).newlines--;
                vPhaseReport.get(id).indent(2);
                vPhaseReport.get(vPhaseReport.size() - 1).newlines++;
            }

            if (!bMissed) {
                vPhaseReport.addAll(dmgPerHitReport);
                // Buildings shield all units from a certain amount of damage.
                // Amount is based upon the building's CF at the phase's start.
                int bldgAbsorbs = 0;
                if (targetInBuilding && (bldg != null)
                        && (toHit.getThruBldg() == null)) {
                    bldgAbsorbs = bldg.getAbsorbtion(target.getPosition());
                }
                
                // Attacking infantry in buildings from same building
                if (targetInBuilding && (bldg != null)
                        && (toHit.getThruBldg() != null)
                        && (entityTarget instanceof Infantry)) {
                    // If elevation is the same, building doesn't absorb
                    if (ae.getElevation() != entityTarget.getElevation()) {
                        int dmgClass = wtype.getInfantryDamageClass();
                        int nDamage;
                        if (dmgClass < WeaponType.WEAPON_BURST_1D6) {
                            nDamage = nDamPerHit * Math.min(nCluster, hits);
                        } else {
                            // Need to indicate to handleEntityDamage that the
                            // absorbed damage shouldn't reduce incoming damage,
                            // since the incoming damage was reduced in
                            // Compute.directBlowInfantryDamage
                            nDamage = -wtype.getDamage(nRange)
                                    * Math.min(nCluster, hits);
                        }
                        bldgAbsorbs = (int) Math.round(nDamage
                                * bldg.getInfDmgFromInside());
                    } else {
                        // Used later to indicate a special report
                        bldgAbsorbs = Integer.MIN_VALUE;
                    }
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
                    if ((target.getTargetType() == Targetable.TYPE_HEX_TAG)
                            || (target.getTargetType() == Targetable.TYPE_BLDG_TAG)) {
                        int priority = 1;
                        EquipmentMode mode = (weapon.curMode());
                        if (mode != null) {
                            if (mode.getName() == "1-shot") {
                                priority = 1;
                            } else if (mode.getName() == "2-shot") {
                                priority = 2;
                            } else if (mode.getName() == "3-shot") {
                                priority = 3;
                            } else if (mode.getName() == "4-shot") {
                                priority = 4;
                            }
                        }
                        TagInfo info = new TagInfo(ae.getId(),
                                target.getTargetType(), target, priority, false);
                        game.addTagInfo(info);
                        
                        ae.setSpotting(true);
                        ae.setSpotTargetId(target.getTargetId());
                        
                        r = new Report(3390);
                        r.subject = subjectId;
                        vPhaseReport.addElement(r);
                        hits = 0;
                    // targeting a hex for igniting    
                    } else if ((target.getTargetType() == Targetable.TYPE_HEX_IGNITE)
                            || (target.getTargetType() == Targetable.TYPE_BLDG_IGNITE)) {
                        handleIgnitionDamage(vPhaseReport, bldg, hits);
                        hits = 0;
                    // targeting a hex for clearing
                    } else if (target.getTargetType() == Targetable.TYPE_HEX_CLEAR) {
                        nDamage = nDamPerHit * hits;
                        handleClearDamage(vPhaseReport, bldg, nDamage);
                        hits = 0;
                    // Targeting a building.
                    } else if (target.getTargetType() == Targetable.TYPE_BUILDING) {
                        // The building takes the full brunt of the attack.
                        nDamage = nDamPerHit * hits;
                        handleBuildingDamage(vPhaseReport, bldg, nDamage,
                                target.getPosition());
                        hits = 0;
                    } else if (entityTarget != null) {
                        handleEntityDamage(entityTarget, vPhaseReport, bldg,
                                hits, nCluster, bldgAbsorbs);
                        server.creditKill(entityTarget, ae);
                        hits -= nCluster;
                        firstHit = false;
                    } else {
                        // we shouldn't be here, but if we get here, let's set hits to 0
                        // to avoid infinite loops
                        hits = 0;
                        LogManager.getLogger().error("Unexpected target type: " + target.getTargetType());
                    }
                } // Handle the next cluster.
            } else { // We missed, but need to handle special miss cases

                // When shooting at a non-infantry unit in a building and the
                // shot misses, the building is damaged instead, TW pg 171
                if (bldgDamagedOnMiss) {
                    r = new Report(6429);
                    r.indent(2);
                    r.subject = ae.getId();
                    r.newlines--;
                    vPhaseReport.add(r);
                    int nDamage = nDamPerHit * hits;
                    // We want to set bSalvo to true to prevent
                    // handleBuildingDamage from reporting a hit
                    boolean savedSalvo = bSalvo;
                    bSalvo = true;
                    handleBuildingDamage(vPhaseReport, bldg, nDamage,
                            target.getPosition());
                    bSalvo = savedSalvo;
                    hits = 0;
                }
            }
            if (game.getOptions().booleanOption(OptionsConstants.ADVCOMBAT_UAC_TWOROLLS)
                    && ((wtype.getAmmoType() == AmmoType.T_AC_ULTRA) || (wtype
                            .getAmmoType() == AmmoType.T_AC_ULTRA_THB))
                    && (i == 2)) {
                // Jammed weapon doesn't get 2nd shot...
                if (isJammed) {
                    r = new Report(9905);
                    r.indent();
                    r.subject = ae.getId();
                    vPhaseReport.addElement(r);
                    i--;
                } else { // If not jammed, it gets the second shot...
                    r = new Report(9900);
                    r.indent();
                    r.subject = ae.getId();
                    vPhaseReport.addElement(r);
                    if (null != ae.getCrew()) {
                        roll = ae.getCrew().rollGunnerySkill();
                    } else {
                        roll = Compute.d6(2);
                    }
                }
            }
        }
        Report.addNewline(vPhaseReport);

        insertAttacks(phase, vPhaseReport);

        returnedReports.addAll(vPhaseReport);
        return false;
    }

    /**
     * Calculate the damage per hit.
     *
     * @return an <code>int</code> representing the damage dealt per hit.
     */
    protected int calcDamagePerHit() {
        double toReturn = wtype.getDamage(nRange);

        // Check for BA vs BA weapon effectiveness, if option is on
        if (game.getOptions().booleanOption(OptionsConstants.ADVCOMBAT_TACOPS_BA_VS_BA)
                && (target instanceof BattleArmor)) {
            // We don't check to make sure the attacker is BA, as most weapons
            // will return their normal damage.
            toReturn = Compute.directBlowBADamage(toReturn,
                    wtype.getBADamageClass(), (BattleArmor) target);
        }

        // we default to direct fire weapons for anti-infantry damage
        if (target.isConventionalInfantry()) {
            toReturn = Compute.directBlowInfantryDamage(toReturn,
                    bDirect ? toHit.getMoS() / 3 : 0,
                    wtype.getInfantryDamageClass(),
                    ((Infantry) target).isMechanized(),
                    toHit.getThruBldg() != null, ae.getId(), calcDmgPerHitReport);
        } else if (bDirect) {
            toReturn = Math.min(toReturn + (toHit.getMoS() / 3.0), toReturn * 2);
        }

        toReturn = applyGlancingBlowModifier(toReturn, target.isConventionalInfantry());

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

    /**
     * Calculate the attack value based on range
     *
     * @return an <code>int</code> representing the attack value at that range.
     */
    protected int calcAttackValue() {
        int av = 0;
        // if we have a ground firing unit, then AV should not be determined by
        // aero range brackets
        if (!ae.isAirborne() || game.getOptions().booleanOption(OptionsConstants.ADVCOMBAT_UAC_TWOROLLS)) {
            if (usesClusterTable()) {
                // for cluster weapons just use the short range AV
                av = wtype.getRoundShortAV();
            } else {
                // otherwise just use the full weapon damage by range
                av = wtype.getDamage(nRange);
            }
        } else {
            // we have an airborne attacker, so we need to use aero range
            // brackets
            int range = RangeType.rangeBracket(nRange, wtype.getATRanges(),
                    true, false);
            if (range == WeaponType.RANGE_SHORT) {
                av = wtype.getRoundShortAV();
            } else if (range == WeaponType.RANGE_MED) {
                av = wtype.getRoundMedAV();
            } else if (range == WeaponType.RANGE_LONG) {
                av = wtype.getRoundLongAV();
            } else if (range == WeaponType.RANGE_EXT) {
                av = wtype.getRoundExtAV();
            }
        }
        if (bDirect) {
            av = Math.min(av + (toHit.getMoS() / 3), av * 2);
        }
        
        av = applyGlancingBlowModifier(av, false);
        
        av = (int) Math.floor(getBracketingMultiplier() * av);

        return av;
    }

    /**
     * * adjustment factor on attack value for fighter squadrons
     */
    protected double getBracketingMultiplier() {
        double mult = 1.0;
        if (wtype.hasModes() && weapon.curMode().equals("Bracket 80%")) {
            mult = 0.8;
        }
        if (wtype.hasModes() && weapon.curMode().equals("Bracket 60%")) {
            mult = 0.6;
        }
        if (wtype.hasModes() && weapon.curMode().equals("Bracket 40%")) {
            mult = 0.4;
        }
        return mult;
    }

    /*
     * Return the capital missile target for criticals. Zero if not a capital
     * missile
     */
    protected int getCapMisMod() {
        return 0;
    }

    /**
     * Handles potential damage to partial cover that absorbs a shot. The
     * <code>ToHitData</code> is checked to what if there is any damagable cover
     * to be hit, and if so which cover gets hit (there are two possibilities in
     * some cases, such as 75% partial cover). The method then takes care of
     * assigning damage to the cover. Buildings are damaged directly, while
     * dropships call the <code>handleEntityDamage</code> method.
     *
     * @param entityTarget
     *            The target Entity
     * @param vPhaseReport
     * @param pcHit
     * @param bldg
     * @param hits
     * @param nCluster
     * @param bldgAbsorbs
     */
    protected void handlePartialCoverHit(Entity entityTarget,
            Vector<Report> vPhaseReport, HitData pcHit, Building bldg,
            int hits, int nCluster, int bldgAbsorbs) {

        // Report the hit and table description, if this isn't part of a salvo
        Report r;
        if (!bSalvo) {
            r = new Report(3405);
            r.subject = subjectId;
            r.add(toHit.getTableDesc());
            r.add(entityTarget.getLocationAbbr(pcHit));
            vPhaseReport.addElement(r);
            if (weapon.isRapidfire()) {
                r.newlines = 0;
                r = new Report(3225);
                r.subject = subjectId;
                r.add(numRapidFireHits * 3);
                vPhaseReport.add(r);
            }
        } else {
            // Keep spacing consistent
            Report.addNewline(vPhaseReport);
        }

        r = new Report(3460);
        r.subject = subjectId;
        r.add(entityTarget.getShortName());
        r.add(entityTarget.getLocationAbbr(pcHit));
        r.indent(2);
        vPhaseReport.addElement(r);

        int damageableCoverType = LosEffects.DAMAGABLE_COVER_NONE;
        Building coverBuilding = null;
        Entity coverDropShip = null;
        Coords coverLoc = null;

        // Determine if there is primary and secondary cover,
        // and then determine which one gets hit
        if ((toHit.getCover() == LosEffects.COVER_75RIGHT || toHit.getCover() == LosEffects.COVER_75LEFT)
                ||
                // 75% cover has a primary and secondary
                (toHit.getCover() == LosEffects.COVER_HORIZONTAL && toHit
                        .getDamagableCoverTypeSecondary() != LosEffects.DAMAGABLE_COVER_NONE)) {
            // Horizontal cover provided by two 25%'s, so primary and secondary
            int hitLoc = pcHit.getLocation();
            // Primary stores the left side, from the perspective of the
            // attacker
            if (hitLoc == Mech.LOC_RLEG || hitLoc == Mech.LOC_RT
                    || hitLoc == Mech.LOC_RARM) {
                // Left side is primary
                damageableCoverType = toHit.getDamagableCoverTypePrimary();
                coverBuilding = toHit.getCoverBuildingPrimary();
                coverDropShip = toHit.getCoverDropshipPrimary();
                coverLoc = toHit.getCoverLocPrimary();
            } else {
                // If not left side, then right side, which is secondary
                damageableCoverType = toHit.getDamagableCoverTypeSecondary();
                coverBuilding = toHit.getCoverBuildingSecondary();
                coverDropShip = toHit.getCoverDropshipSecondary();
                coverLoc = toHit.getCoverLocSecondary();
            }
        } else { // Only primary cover exists
            damageableCoverType = toHit.getDamagableCoverTypePrimary();
            coverBuilding = toHit.getCoverBuildingPrimary();
            coverDropShip = toHit.getCoverDropshipPrimary();
            coverLoc = toHit.getCoverLocPrimary();
        }
        // Check if we need to damage the cover that absorbed the hit.
        if (damageableCoverType == LosEffects.DAMAGABLE_COVER_DROPSHIP) {
            // We need to adjust some state and then restore it later
            // This allows us to make a call to handleEntityDamage
            ToHitData savedToHit = toHit;
            AimingMode savedAimingMode = waa.getAimingMode();
            waa.setAimingMode(AimingMode.NONE);
            int savedAimedLocation = waa.getAimedLocation();
            waa.setAimedLocation(Entity.LOC_NONE);
            boolean savedSalvo = bSalvo;
            bSalvo = true;
            // Create new toHitData
            toHit = new ToHitData(0, "", ToHitData.HIT_NORMAL,
                    Compute.targetSideTable(ae, coverDropShip));
            // Report cover was damaged
            int sizeBefore = vPhaseReport.size();
            r = new Report(3465);
            r.subject = subjectId;
            r.add(coverDropShip.getShortName());
            vPhaseReport.add(r);
            // Damage the DropShip
            handleEntityDamage(coverDropShip, vPhaseReport, bldg, hits, nCluster, bldgAbsorbs);
            // Remove a blank line in the report list
            if (vPhaseReport.elementAt(sizeBefore).newlines > 0) {
                vPhaseReport.elementAt(sizeBefore).newlines--;
            }
            // Indent reports related to the damage absorption
            while (sizeBefore < vPhaseReport.size()) {
                vPhaseReport.elementAt(sizeBefore).indent(3);
                sizeBefore++;
            }
            // Restore state
            toHit = savedToHit;
            waa.setAimingMode(savedAimingMode);
            waa.setAimedLocation(savedAimedLocation);
            bSalvo = savedSalvo;
            // Damage a building that blocked a shot
        } else if (damageableCoverType == LosEffects.DAMAGABLE_COVER_BUILDING) {
            // Normal damage
            int nDamage = nDamPerHit * Math.min(nCluster, hits);
            Vector<Report> buildingReport = server.damageBuilding(
                    coverBuilding, nDamage, " blocks the shot and takes ",
                    coverLoc);
            for (Report report : buildingReport) {
                report.subject = subjectId;
                report.indent();
            }
            vPhaseReport.addAll(buildingReport);
            // Damage any infantry in the building.
            Vector<Report> infantryReport = server.damageInfantryIn(
                    coverBuilding, nDamage, coverLoc,
                    wtype.getInfantryDamageClass());
            for (Report report : infantryReport) {
                report.indent(2);
            }
            vPhaseReport.addAll(infantryReport);
        }
        missed = true;
    }

    /**
     * Handle damage against an entity, called once per hit by default.
     */
    protected void handleEntityDamage(Entity entityTarget,
            Vector<Report> vPhaseReport, Building bldg, int hits, int nCluster,
            int bldgAbsorbs) {
        missed = false;

        initHit(entityTarget);
        
        boolean isIndirect = wtype.hasModes() && weapon.curMode().equals("Indirect");
        
        Hex targetHex = game.getBoard().getHex(target.getPosition());
        boolean mechPokingOutOfShallowWater = unitGainsPartialCoverFromWater(targetHex, entityTarget);
        
        // a very specific situation where a mech is standing in a height 1 building
        // or its upper torso is otherwise somehow poking out of said building 
        boolean targetInShortBuilding = WeaponAttackAction.targetInShortCoverBuilding(target);
        boolean legHit = entityTarget.locationIsLeg(hit.getLocation());
        boolean shortBuildingBlocksLegHit = targetInShortBuilding && legHit;
        
        boolean partialCoverForIndirectFire = 
                isIndirect && (mechPokingOutOfShallowWater || shortBuildingBlocksLegHit);

        //For indirect fire, remove leg hits only if target is in water partial cover
        //Per TW errata for indirect fire
        if ((!isIndirect || partialCoverForIndirectFire) 
                && entityTarget.removePartialCoverHits(hit.getLocation(), toHit
                        .getCover(), Compute.targetSideTable(ae, entityTarget,
                        weapon.getCalledShot().getCall()))) {
            // Weapon strikes Partial Cover.
            handlePartialCoverHit(entityTarget, vPhaseReport, hit, bldg, hits,
                    nCluster, bldgAbsorbs);
            return;
        }

        if (!bSalvo) {
            // Each hit in the salvo get's its own hit location.
            Report r = new Report(3405);
            r.subject = subjectId;
            r.add(toHit.getTableDesc());
            r.add(entityTarget.getLocationAbbr(hit));
            vPhaseReport.addElement(r);
            if (weapon.isRapidfire()) {
                r.newlines = 0;
                r = new Report(3225);
                r.subject = subjectId;
                r.add(numRapidFireHits * 3);
                vPhaseReport.add(r);
            }
        } else {
            Report.addNewline(vPhaseReport);
        }

        // for non-salvo shots, report that the aimed shot was successful
        // before applying damage
        if (hit.hitAimedLocation() && !bSalvo) {
            Report r = new Report(3410);
            r.subject = subjectId;
            vPhaseReport.lastElement().newlines = 0;
            vPhaseReport.addElement(r);
        }
        // Resolve damage normally.
        int nDamage = nDamPerHit * Math.min(nCluster, hits);

        if (bDirect) {
            hit.makeDirectBlow(toHit.getMoS() / 3);
        }
        
        // Report calcDmgPerHitReports here
        if (!calcDmgPerHitReport.isEmpty()) {
            vPhaseReport.addAll(calcDmgPerHitReport);
        }
        
        // if the target was in partial cover, then we already handled
        // damage absorption by the partial cover, if it would have happened
        boolean targetStickingOutOfBuilding = unitStickingOutOfBuilding(targetHex, entityTarget);
                
        nDamage = absorbBuildingDamage(nDamage, entityTarget, bldgAbsorbs, 
                vPhaseReport, bldg, targetStickingOutOfBuilding);

        nDamage = checkTerrain(nDamage, entityTarget, vPhaseReport);
        nDamage = checkLI(nDamage, entityTarget, vPhaseReport);

        // some buildings scale remaining damage that is not absorbed
        // TODO: this isn't quite right for castles brian
        if ((null != bldg) && !targetStickingOutOfBuilding) {
            nDamage = (int) Math.floor(bldg.getDamageToScale() * nDamage);
        }

        // A building may absorb the entire shot.
        if (nDamage == 0) {
            Report r = new Report(3415);
            r.subject = subjectId;
            r.indent(2);
            r.addDesc(entityTarget);
            vPhaseReport.addElement(r);
            missed = true;
        } else {
            if (bGlancing) {
                hit.makeGlancingBlow();
            }
            
            if (bLowProfileGlancing) {
                hit.makeGlancingBlow();
            }
            
            vPhaseReport
                    .addAll(server.damageEntity(entityTarget, hit, nDamage,
                            false, ae.getSwarmTargetId() == entityTarget
                                    .getId() ? DamageType.IGNORE_PASSENGER
                                    : damageType, false, false, throughFront,
                            underWater, nukeS2S));
            if (damageType.equals(DamageType.ANTI_TSM) && (target instanceof Mech) && entityTarget.antiTSMVulnerable()) {
                vPhaseReport.addAll(server.doGreenSmokeDamage(entityTarget));
            }
            // for salvo shots, report that the aimed location was hit after
            // applying damage, because the location is first reported when
            // dealing the damage
            if (hit.hitAimedLocation() && bSalvo) {
                Report r = new Report(3410);
                r.subject = subjectId;
                vPhaseReport.lastElement().newlines = 0;
                vPhaseReport.addElement(r);
            }
        }
        // If a BA squad is shooting at infantry, damage may be random and need
        // to be rerolled for the next hit (if any) from the same attack.
        if ((ae instanceof BattleArmor) && (target instanceof Infantry)) {
            nDamPerHit = calcDamagePerHit();
        }
    }
    
    /**
     * Worker function - does the entity gain partial cover from shallow water?
     */
    protected boolean unitGainsPartialCoverFromWater(Hex targetHex, Entity entityTarget) {
        return (targetHex != null) && 
                targetHex.containsTerrain(Terrains.WATER) &&
                (entityTarget.relHeight() == targetHex.getLevel());
    }
    
    /**
     * Worker function - is a part of this unit inside the hex's terrain features, 
     * but part sticking out?
     */
    protected boolean unitStickingOutOfBuilding(Hex targetHex, Entity entityTarget) {
        // target needs to be on the board,
        // be tall enough for it to make a difference,
        // target "feet" are below the "ceiling"
        // target "head" is above the "ceiling"
        return (targetHex != null) &&
                (entityTarget.getHeight() > 0) &&
                (entityTarget.getElevation() < targetHex.ceiling()) &&
                (entityTarget.relHeight() >= targetHex.ceiling());
    }
    
    /**
     * Worker function to (maybe) have a building absorb damage meant for the entity
     */
    protected int absorbBuildingDamage(int nDamage, Entity entityTarget, int bldgAbsorbs, 
            Vector<Report> vPhaseReport, Building bldg, boolean targetStickingOutOfBuilding) {

        // if the building will absorb some damage and the target is actually
        // entirely inside the building:
        if ((bldgAbsorbs > 0) && !targetStickingOutOfBuilding) {            
            int toBldg = Math.min(bldgAbsorbs, nDamage);
            nDamage -= toBldg;
            Report.addNewline(vPhaseReport);
            Vector<Report> buildingReport = server.damageBuilding(bldg, toBldg,
                    entityTarget.getPosition());
            for (Report report : buildingReport) {
                report.subject = subjectId;
            }
            vPhaseReport.addAll(buildingReport);
        // Units on same level, report building absorbs no damage
        } else if (bldgAbsorbs == Integer.MIN_VALUE) {
            Report.addNewline(vPhaseReport);
            Report r = new Report(9976);
            r.subject = ae.getId();
            r.indent(2);
            vPhaseReport.add(r);
        // Cases where absorbed damage doesn't reduce incoming damage
        } else if ((bldgAbsorbs < 0) && !targetStickingOutOfBuilding) {
            int toBldg = -bldgAbsorbs;
            Report.addNewline(vPhaseReport);
            Vector<Report> buildingReport = server.damageBuilding(bldg, toBldg,
                    entityTarget.getPosition());
            for (Report report : buildingReport) {
                report.subject = subjectId;
            }
            vPhaseReport.addAll(buildingReport);
        }
        
        return nDamage;
    }

    protected void handleIgnitionDamage(Vector<Report> vPhaseReport,
            Building bldg, int hits) {
        if (!bSalvo) {
            // hits!
            Report r = new Report(2270);
            r.subject = subjectId;
            r.newlines = 0;
            vPhaseReport.addElement(r);
        }
        TargetRoll tn = new TargetRoll(wtype.getFireTN(), wtype.getName());
        if (tn.getValue() != TargetRoll.IMPOSSIBLE) {
            Report.addNewline(vPhaseReport);
            server.tryIgniteHex(target.getPosition(), subjectId, false, false,
                    tn, true, -1, vPhaseReport);
        }
    }

    protected void handleClearDamage(Vector<Report> vPhaseReport,
            Building bldg, int nDamage) {
        handleClearDamage(vPhaseReport, bldg, nDamage, true);
    }

    protected void handleClearDamage(Vector<Report> vPhaseReport,
            Building bldg, int nDamage, boolean hitReport) {
        if (!bSalvo && hitReport) {
            // hits!
            Report r = new Report(2270);
            r.subject = subjectId;
            vPhaseReport.addElement(r);
        }
        // report that damage was "applied" to terrain
        Report r = new Report(3385);
        r.indent(2);
        r.subject = subjectId;
        r.add(nDamage);
        vPhaseReport.addElement(r);

        // Any clear attempt can result in accidental ignition, even
        // weapons that can't normally start fires. that's weird.
        // Buildings can't be accidentally ignited.
        // TODO: change this for TacOps - now you roll another 2d6 first and on
        // a 5 or less
        // you do a normal ignition as though for intentional fires
        if ((bldg != null)
                && server.tryIgniteHex(target.getPosition(), subjectId, false,
                        false,
                        new TargetRoll(wtype.getFireTN(), wtype.getName()), 5,
                        vPhaseReport)) {
            return;
        }
        Vector<Report> clearReports = server.tryClearHex(target.getPosition(),
                nDamage, subjectId);
        if (!clearReports.isEmpty()) {
            vPhaseReport.lastElement().newlines = 0;
        }
        vPhaseReport.addAll(clearReports);
    }

    protected void handleBuildingDamage(Vector<Report> vPhaseReport, Building bldg, int nDamage,
                                        Coords coords) {
        if (!bSalvo) {
            // hits!
            Report r = new Report(3390);
            r.subject = subjectId;
            vPhaseReport.addElement(r);
        }
        Report.addNewline(vPhaseReport);
        Vector<Report> buildingReport = server.damageBuilding(bldg, nDamage, coords);
        for (Report report : buildingReport) {
            report.subject = subjectId;
        }
        vPhaseReport.addAll(buildingReport);

        // Damage any infantry in hex, unless attack between units in same bldg
        if (toHit.getThruBldg() == null) {
            vPhaseReport.addAll(server.damageInfantryIn(bldg, nDamage, coords,
                    wtype.getInfantryDamageClass()));
        }
    }

    protected boolean allShotsHit() {
        if ((((target.getTargetType() == Targetable.TYPE_BLDG_IGNITE) || (target
                .getTargetType() == Targetable.TYPE_BUILDING)) && (nRange <= 1))
                || (target.getTargetType() == Targetable.TYPE_HEX_CLEAR)) {
            return true;
        }

        if (game.getOptions().booleanOption(OptionsConstants.ADVAERORULES_AERO_SANITY)
                && target.getTargetType() == Targetable.TYPE_ENTITY
                && ((Entity) target).isCapitalScale()
                && !((Entity) target).isCapitalFighter()
                && !ae.isCapitalFighter()) {
            return true;
        }
        return false;
    }

    protected void reportMiss(Vector<Report> vPhaseReport) {
        reportMiss(vPhaseReport, false);
    }

    protected void reportMiss(Vector<Report> vPhaseReport, boolean singleNewline) {
        // Report the miss.
        Report r = new Report(3220);
        r.subject = subjectId;
        if (singleNewline) {
            r.newlines = 1;
        } else {
            r.newlines = 2;
        }
        vPhaseReport.addElement(r);
    }

    protected WeaponHandler() {
        // deserialization only
    }

    // Among other things, basically a refactored Server#preTreatWeaponAttack
    public WeaponHandler(ToHitData t, WeaponAttackAction w, Game g, Server s) {
        damageType = DamageType.NONE;
        toHit = t;
        waa = w;
        game = g;
        ae = game.getEntity(waa.getEntityId());
        weapon = ae.getEquipment(waa.getWeaponId());
        wtype = (WeaponType) weapon.getType();
        typeName = wtype.getInternalName();
        target = game.getTarget(waa.getTargetType(), waa.getTargetId());
        server = s;
        subjectId = getAttackerId();
        nRange = Compute.effectiveDistance(game, ae, target);
        if (target instanceof Mech) {
            throughFront = Compute.isThroughFrontHex(game, ae.getPosition(), (Entity) target);
        } else {
            throughFront = true;
        }
        // is this an underwater attack on a surface naval vessel?
        underWater = toHit.getHitTable() == ToHitData.HIT_UNDERWATER;
        if (null != ae.getCrew()) {
            roll = ae.getCrew().rollGunnerySkill();
        } else {
            roll = Compute.d6(2);
        }
        
        nweapons = getNumberWeapons();
        nweaponsHit = 1;
        // use ammo when creating this, so it works when shooting the last shot
        // a unit has and we fire multiple weapons of the same type
        // TODO : need to adjust this for cases where not all the ammo is available
        for (int i = 0; i < nweapons; i++) {
            useAmmo();
        }

        if (target instanceof Entity) {
            ((Entity) target).addAttackedByThisTurn(w.getEntityId());
        }
    }

    /**
     * Worker function that initializes the actual hit, including a hit location and various other properties.
     * @param entityTarget Entity being hit.
     */
    protected void initHit(Entity entityTarget) {
        hit = entityTarget.rollHitLocation(toHit.getHitTable(),
                toHit.getSideTable(), waa.getAimedLocation(),
                waa.getAimingMode(), toHit.getCover());
        hit.setGeneralDamageType(generalDamageType);
        hit.setCapital(wtype.isCapital());
        hit.setBoxCars(roll == 12);
        hit.setCapMisCritMod(getCapMisMod());
        hit.setFirstHit(firstHit);
        hit.setAttackerId(getAttackerId());

        if (weapon.isWeaponGroup()) {
            hit.setSingleAV(attackValue);
        }
    }

    protected void useAmmo() {
        if (wtype.hasFlag(WeaponType.F_DOUBLE_ONESHOT)) {
            ArrayList<Mounted> chain = new ArrayList<>();
            for (Mounted current = weapon.getLinked(); current != null; current = current.getLinked()) {
                chain.add(current);
            }

            if (!chain.isEmpty()) {
                chain.sort((m1, m2) -> Integer.compare(m2.getUsableShotsLeft(), m1.getUsableShotsLeft()));
                weapon.setLinked(chain.get(0));
                for (int i = 0; i < chain.size() - 1; i++) {
                    chain.get(i).setLinked(chain.get(i + 1));
                }
                chain.get(chain.size() - 1).setLinked(null);
                if (weapon.getLinked().getUsableShotsLeft() == 0) {
                    weapon.setFired(true);
                }
            }
        } else if (wtype.hasFlag(WeaponType.F_ONESHOT)) {
            weapon.setFired(true);
        }
        setDone();
    }

    protected void setDone() {
        weapon.setUsedThisRound(true);
    }

    protected void addHeat() {
        // Only add heat for first shot in strafe
        if (isStrafing && !isStrafingFirstShot()) {
            return;
        }
        if (!(toHit.getValue() == TargetRoll.IMPOSSIBLE)) {
            if (ae.usesWeaponBays() && !game.getOptions().booleanOption(OptionsConstants.ADVAERORULES_HEAT_BY_BAY)) {
                int loc = weapon.getLocation();
                boolean rearMount = weapon.isRearMounted();
                if (!ae.hasArcFired(loc, rearMount)) {
                    ae.heatBuildup += ae.getHeatInArc(loc, rearMount);
                    ae.setArcFired(loc, rearMount);
                }
            } else {
                ae.heatBuildup += (weapon.getCurrentHeat());
            }
        }
    }
    
    /**
     * Does this attack use the cluster hit table? necessary to determine how
     * Aero damage should be applied
     */
    protected boolean usesClusterTable() {
        return false;
    }

    /**
     * special resolution, like minefields and arty
     *
     * @param vPhaseReport - a <code>Vector</code> containing the phase report
     * @param entityTarget - the <code>Entity</code> targeted, or <code>null</code>, if
     *                     no Entity targeted
     * @return true when done with processing, false when not
     */
    protected boolean specialResolution(Vector<Report> vPhaseReport, Entity entityTarget) {
        return false;
    }

    @Override
    public boolean announcedEntityFiring() {
        return announcedEntityFiring;
    }

    @Override
    public void setAnnouncedEntityFiring(boolean announcedEntityFiring) {
        this.announcedEntityFiring = announcedEntityFiring;
    }

    @Override
    public WeaponAttackAction getWaa() {
        return waa;
    }

    public int checkTerrain(int nDamage, Entity entityTarget, Vector<Report> vPhaseReport) {
        if (entityTarget == null) {
            return nDamage;
        }
        Hex hex = game.getBoard().getHex(entityTarget.getPosition());
        boolean hasWoods = hex.containsTerrain(Terrains.WOODS) || hex.containsTerrain(Terrains.JUNGLE);
        boolean isAboveWoods = (entityTarget.relHeight() + 1 > hex.terrainLevel(Terrains.FOLIAGE_ELEV)) 
                || entityTarget.isAirborne() || !hasWoods;
        
        if (game.getOptions().booleanOption(OptionsConstants.ADVCOMBAT_TACOPS_WOODS_COVER)
                && hasWoods && !isAboveWoods
                && !(entityTarget.getSwarmAttackerId() == ae.getId())) {
            Terrain woodHex = hex.getTerrain(Terrains.WOODS);
            Terrain jungleHex = hex.getTerrain(Terrains.JUNGLE);
            int treeAbsorbs = 0;
            String hexType = "";
            if (woodHex != null) {
                treeAbsorbs = woodHex.getLevel() * 2;
                hexType = "wooded";
            } else if (jungleHex != null) {
                treeAbsorbs = jungleHex.getLevel() * 2;
                hexType = "jungle";
            }

            // Do not absorb more damage than the weapon can do.
            treeAbsorbs = Math.min(nDamage, treeAbsorbs);

            nDamage = Math.max(0, nDamage - treeAbsorbs);
            server.tryClearHex(entityTarget.getPosition(), treeAbsorbs, ae.getId());
            Report.addNewline(vPhaseReport);
            Report terrainReport = new Report(6427);
            terrainReport.subject = entityTarget.getId();
            terrainReport.add(hexType);
            terrainReport.add(treeAbsorbs);
            terrainReport.indent(2);
            terrainReport.newlines = 0;
            vPhaseReport.add(terrainReport);
        }
        return nDamage;
    }

    /**
     * Check for Laser Inhibiting smoke clouds
     */
    public int checkLI(int nDamage, Entity entityTarget, Vector<Report> vPhaseReport) {
        weapon = ae.getEquipment(waa.getWeaponId());
        wtype = (WeaponType) weapon.getType();

        ArrayList<Coords> coords = Coords.intervening(ae.getPosition(), entityTarget.getPosition());
        int refrac = 0;
        double travel = 0;
        double range = ae.getPosition().distance(target.getPosition());
        double atkLev = ae.relHeight();
        double tarLev = entityTarget.relHeight();
        double levDif = Math.abs(atkLev - tarLev);
        String hexType = "LASER inhibiting smoke";

        // loop through all intervening coords.
        // If you could move this to compute.java, then remove - import
        // java.util.ArrayList;
        for (Coords curr : coords) {
            // skip hexes not actually on the board
            if (!game.getBoard().contains(curr)) {
                continue;
            }
            Terrain smokeHex = game.getBoard().getHex(curr).getTerrain(Terrains.SMOKE);
            if (game.getBoard().getHex(curr).containsTerrain(Terrains.SMOKE)
                    && wtype.hasFlag(WeaponType.F_ENERGY)
                    && ((smokeHex.getLevel() == SmokeCloud.SMOKE_LI_LIGHT) || (smokeHex
                            .getLevel() == SmokeCloud.SMOKE_LI_HEAVY))) {

                int levit = ((game.getBoard().getHex(curr).getLevel()) + 2);

                // does the hex contain LASER inhibiting smoke?
                if ((tarLev > atkLev)
                        && (levit >= ((travel * (levDif / range)) + atkLev))) {
                    refrac++;
                } else if ((atkLev > tarLev)
                        && (levit >= (((range - travel) * (levDif / range)) + tarLev))) {
                    refrac++;
                } else if ((atkLev == tarLev) && (levit >= 0)) {
                    refrac++;
                }
                travel++;
            }
        }
        if (refrac != 0) {
            // Damage reduced by 2 for each intervening smoke.
            refrac = (refrac * 2);

            // Do not absorb more damage than the weapon can do. (Are both of
            // these really necessary?)
            refrac = Math.min(nDamage, refrac);
            nDamage = Math.max(0, (nDamage - refrac));

            Report.addNewline(vPhaseReport);
            Report fogReport = new Report(6427);
            fogReport.subject = entityTarget.getId();
            fogReport.add(hexType);
            fogReport.add(refrac);
            fogReport.indent(2);
            fogReport.newlines = 0;
            vPhaseReport.add(fogReport);
        }
        return nDamage;
    }

    /**
     * Insert any additional attacks that should occur before this attack
     */
    protected void insertAttacks(GamePhase phase, Vector<Report> vPhaseReport) {

    }

    /**
     * @return the number of weapons of this type firing (for squadron weapon groups)
     */
    protected int getNumberWeapons() {
        return weapon.getNWeapons();
    }

    /**
     * Restores the equipment from the name
     */
    public void restore() {
        if (typeName == null) {
            typeName = wtype.getName();
        } else {
            wtype = (WeaponType) EquipmentType.get(typeName);
        }

        if (wtype == null) {
            System.err
                    .println("WeaponHandler.restore: could not restore equipment type \""
                            + typeName + "\"");
        }
    }

    protected int getClusterModifiers(boolean clusterRangePenalty) {
        int nMissilesModifier = nSalvoBonus;

        int[] ranges = wtype.getRanges(weapon);
        if (clusterRangePenalty && game.getOptions().booleanOption(OptionsConstants.ADVCOMBAT_TACOPS_CLUSTERHITPEN)) {
            if (nRange <= 1) {
                nMissilesModifier += 1;
            } else if (nRange <= ranges[RangeType.RANGE_MEDIUM]) {
                nMissilesModifier += 0;
            } else {
                nMissilesModifier -= 1;
            }
        }

        if (game.getOptions().booleanOption(OptionsConstants.ADVCOMBAT_TACOPS_RANGE)
                && (nRange > ranges[RangeType.RANGE_LONG])) {
            nMissilesModifier -= 2;
        }
        if (game.getOptions().booleanOption(OptionsConstants.ADVCOMBAT_TACOPS_LOS_RANGE)
                && (nRange > ranges[RangeType.RANGE_EXTREME])) {
            nMissilesModifier -= 3;
        }

        if (bGlancing) {
            nMissilesModifier -= 4;
        }
        
        if (bLowProfileGlancing) {
            nMissilesModifier -= 4;
        }

        if (bDirect) {
            nMissilesModifier += (toHit.getMoS() / 3) * 2;
        }

        if (game.getPlanetaryConditions().hasEMI()) {
            nMissilesModifier -= 2;
        }

        if (null != ae.getCrew()) {
            if (ae.hasAbility(OptionsConstants.GUNNERY_SANDBLASTER, wtype.getName())) {
                if (nRange > ranges[RangeType.RANGE_MEDIUM]) {
                    nMissilesModifier += 2;
                } else if (nRange > ranges[RangeType.RANGE_SHORT]) {
                    nMissilesModifier += 3;
                } else {
                    nMissilesModifier += 4;
                }
            } else if (ae.hasAbility(OptionsConstants.GUNNERY_CLUSTER_MASTER)) {
                nMissilesModifier += 2;
            } else if (ae.hasAbility(OptionsConstants.GUNNERY_CLUSTER_HITTER)) {
                nMissilesModifier += 1;
            }
        }
        return nMissilesModifier;
    }

    @Override
    public boolean isStrafing() {
        return isStrafing;
    }

    @Override
    public void setStrafing(boolean isStrafing) {
        this.isStrafing = isStrafing;
    }

    @Override
    public boolean isStrafingFirstShot() {
        return isStrafingFirstShot;
    }

    @Override
    public void setStrafingFirstShot(boolean isStrafingFirstShot) {
        this.isStrafingFirstShot = isStrafingFirstShot;
    }

    /**
     * Determine the "glancing blow" divider.
     * 2 if the shot is "glancing" or "glancing due to low profile"
     * 4 if both
     * int version
     */
    protected int applyGlancingBlowModifier(int initialValue, boolean roundup) {
        return (int) applyGlancingBlowModifier((double) initialValue, roundup);
    }
    
    /**
     * Determine the "glancing blow" divider.
     * 2 if the shot is "glancing" or "glancing due to low profile"
     * 4 if both
     * double version
     */
    protected double applyGlancingBlowModifier(double initialValue, boolean roundup) {
        // if we're not going to be applying any glancing blow modifiers, just return what we came in with
        if (!bGlancing && !bLowProfileGlancing) {
            return initialValue;
        }
        
        double divisor = getTotalGlancingBlowFactor();        
        double intermediateValue = initialValue / divisor;
        return roundup ? Math.ceil(intermediateValue) : Math.floor(intermediateValue);
    }
    
    /**
     * Logic to determine the glancing blow multiplier:
     * 1 if no glancing blow
     * 2 if one type of glancing blow (either usual or narrow/low profile)
     * 4 if both types of glancing blow
     */
    protected double getTotalGlancingBlowFactor() {
        return (bGlancing ? 2.0 : 1.0) * (bLowProfileGlancing ? 2.0 : 1.0);
    }
    
    /**
     * Worker function that sets the glancing blow flags for this attack for the target when appropriate
     */
    protected void setGlancingBlowFlags(Entity entityTarget) {
        // are we a glancing hit?  Check for this here, report it later
        if (game.getOptions().booleanOption(OptionsConstants.ADVCOMBAT_TACOPS_GLANCING_BLOWS)) {
            if (roll == toHit.getValue()) {
                bGlancing = true;
            } else {
                bGlancing = false;
            }
        }
        
        // low profile glancing blows are triggered on roll = toHit or toHit - 1
        bLowProfileGlancing = isLowProfileGlancingBlow(entityTarget, toHit);
    }
    
    /**
     * Worker function that determines if the given hit on the given entity is a glancing blow
     * as per narrow/low profile quirk rules
     */
    protected boolean isLowProfileGlancingBlow(Entity entityTarget, ToHitData hitData) {
        return (entityTarget != null) &&
                entityTarget.hasQuirk(OptionsConstants.QUIRK_POS_LOW_PROFILE) &&
                ((roll == hitData.getValue()) || (roll == hitData.getValue() + 1));
    }
    
    /**
     * Worker function that adds the 'glancing blow' reports
     */
    protected void addGlancingBlowReports(Vector<Report> vPhaseReport) {
        Report r;
        
        if (bGlancing) {
            r = new Report(3186);
            r.subject = ae.getId();
            r.newlines = 0;
            vPhaseReport.addElement(r);
        }
        
        if (bLowProfileGlancing) {
            r = new Report(9985);
            r.subject = ae.getId();
            r.newlines = 0;
            vPhaseReport.addElement(r);
        }
    }
}

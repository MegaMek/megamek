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
import java.util.ArrayList;
import java.util.Vector;

import megamek.common.Aero;
import megamek.common.AmmoType;
import megamek.common.BattleArmor;
import megamek.common.Building;
import megamek.common.Compute;
import megamek.common.Coords;
import megamek.common.Entity;
import megamek.common.EquipmentType;
import megamek.common.HitData;
import megamek.common.IAimingModes;
import megamek.common.IGame;
import megamek.common.ITerrain;
import megamek.common.Infantry;
import megamek.common.LosEffects;
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

    private static final long serialVersionUID = 7137408139594693559L;
    public ToHitData toHit;
    public WeaponAttackAction waa;
    public int roll;
    protected boolean isJammed = false;

    protected IGame game;
    protected transient Server server; // must not save the server
    protected boolean bMissed;
    protected boolean bSalvo = false;
    protected boolean bGlancing = false;
    protected boolean bDirect = false;
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
    protected Vector<Integer> insertedAttacks = new Vector<Integer>();
    protected int nweapons; //for capital fighters/fighter squadrons
    protected int nweaponsHit; //for capital fighters/fighter squadrons
    protected boolean secondShot = false;


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
            Report r = new Report(3325);
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
     * @return a <code>int</code> value saying how much hits are in each
     *         cluster of damage.
     */
    protected int calcnCluster() {
        return 1;
    }

    protected int calcnClusterAero(Entity entityTarget) {
    	if(usesClusterTable() && !ae.isCapitalFighter() && (entityTarget != null) && !entityTarget.isCapitalScale()) {
    		return 5;
    	} else {
    		return 1;
    	}
    }

    /**
     * handle this weapons firing
     *
     * @return a <code>boolean</code> value indicating whether this should be
     *         kept or not
     */
    public boolean handle(IGame.Phase phase, Vector<Report> vPhaseReport) {
        if (!cares(phase)) {
            return true;
        }

        boolean heatAdded = false;
        int numAttacks = 1;
        if (game.getOptions().booleanOption("uac_tworolls") && ((wtype.getAmmoType() == AmmoType.T_AC_ULTRA) || (wtype.getAmmoType() == AmmoType.T_AC_ULTRA_THB)) && !weapon.curMode().equals("Single")) {
            numAttacks = 2;
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
        for (int i = numAttacks;i > 0;i--) {
            // Report weapon attack and its to-hit value.
            Report r = new Report(3115);
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
            if (!heatAdded) {
                addHeat();
                heatAdded = true;
            }

            attackValue = calcAttackValue();

            // Any necessary PSRs, jam checks, etc.
            // If this boolean is true, don't report
            // the miss later, as we already reported
            // it in doChecks
            boolean missReported = doChecks(vPhaseReport);
            if (missReported) {
                bMissed = true;
            }

            // Do we need some sort of special resolution (minefields, artillery,
            if (specialResolution(vPhaseReport, entityTarget) && (i < 2)) {
                return false;
            }

            if (bMissed && !missReported) {
                if (game.getOptions().booleanOption("uac_tworolls") && ((wtype.getAmmoType() == AmmoType.T_AC_ULTRA) || (wtype.getAmmoType() == AmmoType.T_AC_ULTRA_THB)) && (i == 2)) {
                    reportMiss(vPhaseReport, true);
                } else {
                reportMiss(vPhaseReport);
                }

                // Works out fire setting, AMS shots, and whether continuation is
                // necessary.
                if (!handleSpecialMiss(entityTarget, targetInBuilding, bldg,
                        vPhaseReport) && (i < 2)) {
                    return false;
                }
            }

            // yeech. handle damage. . different weapons do this in very different
            // ways
            int hits = 1;
            if(!(target.isAirborne())) {
                hits = calcHits(vPhaseReport);
            }
            int nCluster = calcnCluster();

            //Now I need to adjust this for attacks on aeros because they use attack values and different rules
            if(target.isAirborne() || game.getBoard().inSpace()) {
            	//this will work differently for cluster and non-cluster weapons, and differently for capital fighter/fighter squadrons
                nCluster = calcnClusterAero(entityTarget);
                if(nCluster > 1) {
                	bSalvo = true;
                    nDamPerHit = 1;
                    hits = attackValue;
                } else {
	                if(ae.isCapitalFighter()) {
	                    bSalvo = true;
	                    if(nweapons > 1) {
	                        nweaponsHit = Compute.missilesHit(nweapons, ((Aero)ae).getClusterMods());
	                        r = new Report(3325);
	                        r.subject = subjectId;
	                        r.add(nweaponsHit);
	                        r.add(" weapon(s) ");
	                        r.add(" ");
	                        r.newlines = 0;
	                        vPhaseReport.add(r);
	                    }
	                    nDamPerHit = attackValue * nweaponsHit;
	                    hits = 1;
	                    nCluster = 1;
	                } else {
	                    bSalvo = false;
	                    nDamPerHit = attackValue;
	                    hits = 1;
	                    nCluster = 1;
	                }
                }
            }

            if (!bMissed) {
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
                        hits = 0;
                    }
                    // targeting a hex for clearing
                    if (target.getTargetType() == Targetable.TYPE_HEX_CLEAR) {
                        nDamage = nDamPerHit * hits;
                        handleClearDamage(vPhaseReport, bldg, nDamage);
                        hits = 0;
                    }
                    // Targeting a building.
                    if (target.getTargetType() == Targetable.TYPE_BUILDING) {
                        // The building takes the full brunt of the attack.
                        nDamage = nDamPerHit * hits;
                        handleBuildingDamage(vPhaseReport, bldg, nDamage, target.getPosition());
                        hits = 0;
                    }
                    if (entityTarget != null) {
                        handleEntityDamage(entityTarget, vPhaseReport, bldg, hits,
                                nCluster, bldgAbsorbs);
                        server.creditKill(entityTarget, ae);
                        hits -= nCluster;
                    }
                } // Handle the next cluster.
            } // End hit target
            if (game.getOptions().booleanOption("uac_tworolls") && ((wtype.getAmmoType() == AmmoType.T_AC_ULTRA) || (wtype.getAmmoType() == AmmoType.T_AC_ULTRA_THB)) && (i == 2)) {
                // Jammed weapon doesn't get 2nd shot...
                if (isJammed) {
                    r = new Report(9905);
                    r.indent();
                    r.subject = ae.getId();
                    vPhaseReport.addElement(r);
                    i--;
                } else {  // If not jammed, it gets the second shot...
                    r = new Report(9900);
                    r.indent();
                    r.subject = ae.getId();
                    vPhaseReport.addElement(r);
                    if(null != ae.getCrew()) {
                        roll = ae.getCrew().rollGunnerySkill();
                    } else {
                        roll = Compute.d6(2);
                    }
                }
            }
        }
        Report.addNewline(vPhaseReport);
        return false;
    }

    /**
     * Calculate the damage per hit.
     *
     * @return an <code>int</code> representing the damage dealt per hit.
     */
    protected int calcDamagePerHit() {
        double toReturn = wtype.getDamage(nRange);
        // we default to direct fire weapons for anti-infantry damage
        if ((target instanceof Infantry) && !(target instanceof BattleArmor)) {
            toReturn = Compute.directBlowInfantryDamage(toReturn, bDirect ? toHit.getMoS()/3 : 0, wtype.getInfantryDamageClass(), ((Infantry)target).isMechanized());
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
        int av = 0;
        //if we have a ground firing unit, then AV should not be determined by aero range brackets
        if(!ae.isAirborne()) {
            if(usesClusterTable()) {
                //for cluster weapons just use the short range AV
                av = wtype.getRoundShortAV();
            } else {
                //otherwise just use the full weapon damage by range
                av = wtype.getDamage(nRange);
            }
        } else {
            //we have an airborne attacker, so we need to use aero range brackets
            int range = RangeType.rangeBracket(nRange, wtype.getATRanges(), true);
            if(range == WeaponType.RANGE_SHORT) {
                av = wtype.getRoundShortAV();
            } else if(range == WeaponType.RANGE_MED) {
                av = wtype.getRoundMedAV();
            } else if (range == WeaponType.RANGE_LONG) {
                av = wtype.getRoundLongAV();
            } else if (range == WeaponType.RANGE_EXT) {
                av = wtype.getRoundExtAV();
            }
        }
        if(bDirect) {
            av = Math.min(av+(toHit.getMoS()/3), av*2);
        }
        if(bGlancing) {
            av = (int) Math.floor(av / 2.0);

        }
        av = (int)Math.floor(getBracketingMultiplier() * av);

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

    protected void handlePartialCoverHit(Entity entityTarget,
            Vector<Report> vPhaseReport, HitData hit, Building bldg, int hits, int nCluster,
            int bldgAbsorbs){
        
        //Report the hit and table description, if this isn't part of a salvo
        Report r;
        if (!bSalvo) {
            r = new Report(3405);
            r.subject = subjectId;
            r.add(toHit.getTableDesc());
            r.add(entityTarget.getLocationAbbr(hit));
            vPhaseReport.addElement(r);
        }else{        
            //Keep spacing consistent
            Report.addNewline(vPhaseReport);
        }
        
        r = new Report(3460);
        r.subject = subjectId;
        r.add(entityTarget.getShortName());
        r.add(entityTarget.getLocationAbbr(hit));
        r.indent(2);
        vPhaseReport.addElement(r);
        
        int damagableCoverType = LosEffects.DAMAGABLE_COVER_NONE;
        Building coverBuilding = null;
        Entity coverDropship = null;
        Coords coverLoc = null;
        
        //Determine if there is primary and secondary cover, 
        // and then determine which one gets hit
        if ((toHit.getCover() == LosEffects.COVER_75RIGHT || 
                toHit.getCover() == LosEffects.COVER_75LEFT) ||
            //75% cover has a primary and secondary           
                (toHit.getCover() == LosEffects.COVER_HORIZONTAL &&
                 toHit.getDamagableCoverTypeSecondary() != 
                    LosEffects.DAMAGABLE_COVER_NONE)){
            //Horiztonal cover provided by two 25%'s, so primary and secondary
            int hitLoc = hit.getLocation();
            //Primary stores the left side, from the perspective of the attacker
            if (hitLoc == Mech.LOC_RLEG || hitLoc == Mech.LOC_RT || 
                    hitLoc == Mech.LOC_RARM){
                //Left side is primary
                damagableCoverType = toHit.getDamagableCoverTypePrimary();
                coverBuilding = toHit.getCoverBuildingPrimary();
                coverDropship = toHit.getCoverDropshipPrimary();
                coverLoc = toHit.getCoverLocPrimary();
            }else{
                //If not left side, then right side, which is secondary
                damagableCoverType = toHit.getDamagableCoverTypeSecondary();
                coverBuilding = toHit.getCoverBuildingSecondary();
                coverDropship = toHit.getCoverDropshipSecondary();
                coverLoc = toHit.getCoverLocSecondary();
            }            
        } else{ //Only primary cover exists
            damagableCoverType = toHit.getDamagableCoverTypePrimary();
            coverBuilding = toHit.getCoverBuildingPrimary();
            coverDropship = toHit.getCoverDropshipPrimary();
            coverLoc = toHit.getCoverLocPrimary();                
        }
        //Check if we need to damage the cover that absorbed the hit.
        if (damagableCoverType == LosEffects.DAMAGABLE_COVER_DROPSHIP){
          //We need to adjust some state and then restore it later
            // This allows us to make a call to handleEntityDamage
            ToHitData savedToHit = toHit;
            int savedAimingMode = waa.getAimingMode();
            waa.setAimingMode(IAimingModes.AIM_MODE_NONE);
            int savedAimedLocation = waa.getAimedLocation();
            waa.setAimedLocation(Entity.LOC_NONE);
            boolean savedSalvo = bSalvo;
            bSalvo = true;
            //Create new toHitData
            toHit = new ToHitData(0,"",ToHitData.HIT_NORMAL,
                    Compute.targetSideTable(ae,coverDropship));
            //Report cover was damaged
            int sizeBefore = vPhaseReport.size();
            r = new Report(3465);
            r.subject = subjectId;
            r.add(coverDropship.getShortName());
            vPhaseReport.add(r);
            //Damage the dropship
            handleEntityDamage(coverDropship,vPhaseReport,
                    bldg,hits,nCluster,bldgAbsorbs);
            //Remove a blank line in the report list
            if (vPhaseReport.elementAt(sizeBefore).newlines > 0)
                vPhaseReport.elementAt(sizeBefore).newlines--;
            //Indent reports related to the damage absorption
            while (sizeBefore < vPhaseReport.size()){
                vPhaseReport.elementAt(sizeBefore).indent(3);
                sizeBefore++;
            }
            //Restore state                
            toHit = savedToHit;
            waa.setAimingMode(savedAimingMode);
            waa.setAimedLocation(savedAimedLocation);
            bSalvo = savedSalvo;
        //Damage a building that blocked a shot
        } else if (damagableCoverType == LosEffects.DAMAGABLE_COVER_BUILDING){
            //Normal damage
            int nDamage = nDamPerHit * Math.min(nCluster, hits);           
            Vector<Report> buildingReport = 
                server.damageBuilding(coverBuilding, nDamage, 
                        " blocks the shot and takes ",coverLoc);
            for (Report report : buildingReport) {
                report.subject = subjectId;
                report.indent();
            }
            vPhaseReport.addAll(buildingReport);
            // Damage any infantry in the building.
            Vector<Report> infantryReport =
                    server.damageInfantryIn(coverBuilding,
                            nDamage, coverLoc, 
                            wtype.getInfantryDamageClass());                
            for (Report report : infantryReport){
                report.indent(2);
            }
            vPhaseReport.addAll(infantryReport);                                
        }
        missed = true; 
    }
    
    /**
     * Handle damage against an entity, called once per hit by default.
     *
     * @param entityTarget
     * @param vPhaseReport
     * @param bldg
     * @param hits
     * @param nCluster
     * @param bldgAbsorbs
     */
    protected void handleEntityDamage(Entity entityTarget,
            Vector<Report> vPhaseReport, Building bldg, int hits, int nCluster,
            int bldgAbsorbs) {
        int nDamage;
        missed = false;

        HitData hit = entityTarget.rollHitLocation(toHit.getHitTable(), toHit
                .getSideTable(), waa.getAimedLocation(), waa.getAimingMode(), toHit.getCover());
        hit.setGeneralDamageType(generalDamageType);
        hit.setCapital(wtype.isCapital());
        hit.setBoxCars(roll == 12);
        hit.setCapMisCritMod(getCapMisMod());
        if(weapon.isWeaponGroup()) {
            hit.setSingleAV(attackValue);
        }
        boolean isIndirect = wtype.hasModes() && weapon.curMode().equals("Indirect");

        if (!isIndirect && entityTarget.removePartialCoverHits(hit.getLocation(), toHit
                .getCover(), Compute.targetSideTable(ae, entityTarget, weapon.getCalledShot().getCall()))) {
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
        } else {
            Report.addNewline(vPhaseReport);
        }

        // for non-salvo shots, report that the aimed shot was successfull
        // before applying damage
        if (hit.hitAimedLocation() && !bSalvo) {
            Report r = new Report(3410);
            r.subject = subjectId;
            vPhaseReport.lastElement().newlines = 0;
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
        nDamage = checkLI(nDamage, entityTarget, vPhaseReport);

        //some buildings scale remaining damage that is not absorbed
        //TODO: this isn't quite right for castles brian
        if(null != bldg) {
            nDamage = (int) Math.floor(bldg.getDamageToScale() * nDamage);
        }

        // A building may absorb the entire shot.
        if (nDamage == 0) {
            Report r = new Report(3415);
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
        // If a BA squad is shooting at conventional infantry, damage may need to be
        // rerolled for the next attack (if any).
        if ((ae instanceof BattleArmor) && (target instanceof Infantry)
                && !(target instanceof BattleArmor)) {
            nDamPerHit = calcDamagePerHit();
        }
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
            server.tryIgniteHex(target.getPosition(), subjectId, false, false, tn,
                    true, -1, vPhaseReport);
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
        //TODO: change this for TacOps - now you roll another 2d6 first and on a 5 or less
        //you do a normal ignition as though for intentional fires
        if ((bldg != null)
                && server.tryIgniteHex(target.getPosition(), subjectId, false, false,
                        new TargetRoll(wtype.getFireTN(), wtype.getName()), 5, vPhaseReport)) {
            return;
        }
        Vector<Report> clearReports = server.tryClearHex(target.getPosition(), nDamage, subjectId);
        if (clearReports.size() > 0) {
            vPhaseReport.lastElement().newlines = 0;
        }
        vPhaseReport.addAll(clearReports);
        return;
    }

    protected void handleBuildingDamage(Vector<Report> vPhaseReport,
            Building bldg, int nDamage, Coords coords) {
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

        // Damage any infantry in the hex.
        vPhaseReport.addAll(server.damageInfantryIn(bldg, nDamage, coords, wtype.getInfantryDamageClass()));
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
        //deserialization only
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
        typeName = wtype.getInternalName();
        target = game.getTarget(waa.getTargetType(), waa.getTargetId());
        server = s;
        subjectId = getAttackerId();
        nRange = Compute.effectiveDistance(game, ae, target);
        if (target instanceof Mech) {
            throughFront = Compute.isThroughFrontHex(game, ae.getPosition(),
                    (Entity) target);
        } else {
            throughFront = true;
        }
        //is this an underwater attack on a surface naval vessel?
        underWater = toHit.getHitTable() == ToHitData.HIT_UNDERWATER;
        if(null != ae.getCrew()) {
            roll = ae.getCrew().rollGunnerySkill();
        } else {
            roll = Compute.d6(2);
        }
        nweapons = getNumberWeapons();
        nweaponsHit = 1;
        // use ammo when creating this, so it works when shooting the last shot
        // a unit has and we fire multiple weapons of the same type
        //TODO: need to adjust this for cases where not all the ammo is available
        for(int i=0;i<nweapons;i++) {
            useAmmo();
        }
    }

    protected void useAmmo() {
        if (wtype.hasFlag(WeaponType.F_ONESHOT)) {
            weapon.setFired(true);
        }
        setDone();
    }

    protected void setDone() {
        weapon.setUsedThisRound(true);
    }

    protected void addHeat() {
        if (!(toHit.getValue() == TargetRoll.IMPOSSIBLE)) {
            ae.heatBuildup += (weapon.getCurrentHeat());
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
     * @return true when done with processing, false when not
     */
    protected boolean specialResolution(Vector<Report> vPhaseReport,
            Entity entityTarget) {
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
        boolean isAboveWoods = ((entityTarget != null) && ((entityTarget.absHeight() >= 2) || (entityTarget.isAirborne())));
        if ( game.getOptions().booleanOption("tacops_woods_cover") && !isAboveWoods
                && (game.getBoard().getHex(entityTarget.getPosition()).containsTerrain(Terrains.WOODS)
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

            //Do not absorb more damage than the weapon can do.
            treeAbsorbs = Math.min(nDamage, treeAbsorbs);

            nDamage = Math.max(0, nDamage-treeAbsorbs);
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
    public int checkLI(int nDamage, Entity entityTarget,
            Vector<Report> vPhaseReport) {

        weapon = ae.getEquipment(waa.getWeaponId());
        wtype = (WeaponType) weapon.getType();

        ArrayList<Coords> coords = Coords.intervening(ae.getPosition(),
                entityTarget.getPosition());
        int refrac = 0;
        double travel = 0;
        double range = ae.getPosition().distance(target.getPosition());
        double atkLev = ae.absHeight();
        double tarLev = entityTarget.absHeight();
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
            ITerrain smokeHex = game.getBoard().getHex(curr)
                    .getTerrain(Terrains.SMOKE);
            if (game.getBoard().getHex(curr).containsTerrain(Terrains.SMOKE)
                    && wtype.hasFlag(WeaponType.F_ENERGY)
                    && ((smokeHex.getLevel() == 3) || (smokeHex.getLevel() == 4))) {

                int levit = ((game.getBoard().getHex(curr).getElevation()) + 2);

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
            // Damage reduced by 2 for each interviening smoke.
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

    /**
     * Restores the equipment from the name
     */
    public void restore() {
        if (typeName == null) {
            typeName = wtype.getName();
        } else {
            wtype = (WeaponType)EquipmentType.get(typeName);
        }

        if (wtype == null) {
            System.err
                    .println("WeaponHandler.restore: could not restore equipment type \""
                            + typeName + "\"");
        }
    }
}

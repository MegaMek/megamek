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

import java.util.Enumeration;
import java.util.Vector;

import megamek.common.AmmoType;
import megamek.common.Building;
import megamek.common.Compute;
import megamek.common.Entity;
import megamek.common.IGame;
import megamek.common.Mech;
import megamek.common.MiscType;
import megamek.common.Mounted;
import megamek.common.Report;
import megamek.common.TargetRoll;
import megamek.common.Targetable;
import megamek.common.ToHitData;
import megamek.common.WeaponType;
import megamek.common.actions.WeaponAttackAction;
import megamek.server.Server;

/**
 * @author Sebastian Brocks
 *
 */
public class MissileWeaponHandler extends AmmoWeaponHandler {

    boolean bECMAffected = Compute.isAffectedByECM(ae, ae.getPosition(), target.getPosition());
    String sSalvoType = " missile(s) ";
    int nSalvoBonus = 0;
    int[] amsShotDown = new int[0];
    int amsShotDownTotal = 0;
    
    /**
     * @param t
     * @param w
     * @param g
     * @param s
     */
    public MissileWeaponHandler(ToHitData t, WeaponAttackAction w, IGame g,
            Server s) {
        super(t, w, g, s);
    }
    
    /*
     *  (non-Javadoc)
     * @see megamek.common.weapons.WeaponHandler#calcHits(java.util.Vector)
     */
    protected int calcHits(Vector vPhaseReport) {
        Entity entityTarget = (target.getTargetType() == Targetable.TYPE_ENTITY) ? (Entity) target
                : null;
        int missilesHit;
        int nGlancing = 0;
        int nMissilesModifier = 0;
        boolean maxtechmissiles = game.getOptions().booleanOption("maxtech_mslhitpen");
        if (maxtechmissiles) {
            if (nRange<=1) {
                nMissilesModifier += 1;
            } else if (nRange <= wtype.getShortRange()) {
                nMissilesModifier += 0;
            } else if (nRange <= wtype.getMediumRange()) {
                nMissilesModifier -= 1;
            } else {
                nMissilesModifier -= 2;
            }
        }
        boolean bMekStealthActive = false;
        if (ae instanceof Mech) {
            bMekStealthActive = ae.isStealthActive();
        }
        Mounted mLinker = weapon.getLinkedBy();
        AmmoType atype = (AmmoType)ammo.getType();
        if ( (mLinker != null && mLinker.getType() instanceof MiscType &&
                !mLinker.isDestroyed() && !mLinker.isMissing() &&
                !mLinker.isBreached() && 
                mLinker.getType().hasFlag(MiscType.F_ARTEMIS) ) &&
                atype.getMunitionType() == AmmoType.M_ARTEMIS_CAPABLE &&
                !bECMAffected) {
            nSalvoBonus += 2;
        } else if (!bECMAffected && atype.getAmmoType() == AmmoType.T_ATM) {
            nSalvoBonus += 2; 
        } else if (entityTarget != null &&
                (entityTarget.isNarcedBy(ae.getOwner().getTeam()) || 
                 entityTarget.isINarcedBy(ae.getOwner().getTeam()))) {
            // only apply Narc bonus if we're not suffering ECM effect
            // and we are using narc ammo.
            if (!bECMAffected
                    && !bMekStealthActive
                    && ((atype.getAmmoType() == AmmoType.T_LRM) || (atype.getAmmoType() == AmmoType.T_SRM))
                    && atype.getMunitionType() == AmmoType.M_NARC_CAPABLE) {
                nSalvoBonus += 2;
            }
        }
        if (bGlancing) {
            nGlancing -=4;
        }
        missilesHit = Compute.missilesHit(wtype.getRackSize(), nSalvoBonus + nGlancing + nMissilesModifier , bGlancing || maxtechmissiles);
        int newMissilesHit = missilesHit - getAMSShotDown(vPhaseReport);
        if (amsShotDownTotal > 0) {
            for (int i=0; i < amsShotDown.length; i++) {
                int shotDown = Math.min(amsShotDown[i], missilesHit);
                r = new Report(3350);
                r.indent();
                r.subject = subjectId;
                r.add(amsShotDown[i]);
                r.add(shotDown);
                r.newlines = 0;
                vPhaseReport.addElement(r);
            }
            Report.addNewline(vPhaseReport);
            if (newMissilesHit < 1) {
                //all missiles shot down
                r = new Report(3355);
                r.indent();
                r.subject = subjectId;
                r.newlines = 0;
                vPhaseReport.addElement(r);
            } else {
                r = new Report(3360);
                r.indent();
                r.subject = subjectId;
                r.add(newMissilesHit);
                r.newlines = 0;
                vPhaseReport.addElement(r);
            }
        }
        if (newMissilesHit > 0) {
            r = new Report(3325);
            r.subject = subjectId;
            r.add(newMissilesHit);
            r.add(sSalvoType);
            r.add(toHit.getTableDesc());
            r.newlines = 0;
            vPhaseReport.addElement(r);
            if (bECMAffected) {
                //ECM prevents bonus
                r = new Report(3330);
                r.subject = subjectId;
                r.newlines = 0;
                vPhaseReport.addElement(r);
            }
            else if (bMekStealthActive) {
                //stealth prevents bonus
                r = new Report(3335);
                r.subject = subjectId;
                r.newlines = 0;
                vPhaseReport.addElement(r);
            }
            if (nSalvoBonus > 0) {
                r = new Report(3340);
                r.subject = subjectId;
                r.add(nSalvoBonus);
                r.newlines = 0;
                vPhaseReport.addElement(r);
            }
        }
        r = new Report(3345);
        r.newlines = 0;
        vPhaseReport.addElement(r);
        bSalvo = true;
        return newMissilesHit;
    }
    
    /*
     *  (non-Javadoc)
     * @see megamek.common.weapons.WeaponHandler#calcnCluster()
     */
    protected int calcnCluster() {
        return 5;
    }
    
    /*
     *  (non-Javadoc)
     * @see megamek.common.weapons.WeaponHandler#calcDamagePerHit()
     */
    protected int calcDamagePerHit() {
        return 1;
    }
    
    /*
     *  (non-Javadoc)
     * @see megamek.common.weapons.WeaponHandler#handleSpecialMiss(megamek.common.Entity, boolean, megamek.common.Building)
     */
    protected boolean handleSpecialMiss(Entity entityTarget,
            boolean targetInBuilding, Building bldg, Vector vPhaseReport) {
        // Shots that miss an entity can set fires.
        // Buildings can't be accidentally ignited,
        // and some weapons can't ignite fires.
        if (entityTarget != null
                && (bldg == null && wtype.getFireTN() != TargetRoll.IMPOSSIBLE)) {
            server.tryIgniteHex(target.getPosition(), subjectId, false, 11);
        }
        final Entity te = game.getEntity(waa.getTargetId());
        // Report any AMS action.
        for (int i=0; i < amsShotDown.length; i++) {
            if (amsShotDown[i] > 0) {
                r = new Report(3230);
                r.indent();
                r.subject = subjectId;
                r.add(amsShotDown[i]);
                vPhaseReport.addElement(r);
            }
        }

        // BMRr, pg. 51: "All shots that were aimed at a target inside
        // a building and miss do full damage to the building instead."
        if (!targetInBuilding || toHit.getValue() == TargetRoll.AUTOMATIC_FAIL) {
            return false;
        }
        return true;
    }
    
    protected int getAMSShotDown(Vector vPhaseReport) {
        Entity entityTarget = (target.getTargetType() == Targetable.TYPE_ENTITY) ? (Entity) target
                : null;
        if (entityTarget != null) {
            //any AMS attacks by the target?
            Vector vCounters = waa.getCounterEquipment();
            if (null != vCounters) {
                // resolve AMS counter-fire
                amsShotDown = new int[vCounters.size()];
                for (int x = 0; x < vCounters.size(); x++) {
                    amsShotDown[x] = 0;

                    Mounted counter = (Mounted)vCounters.elementAt(x);
                    Mounted mAmmo = counter.getLinked();
                    if ((!(counter.getType() instanceof WeaponType))
                            || (!(counter.getType().hasFlag(WeaponType.F_AMS)))
                            || (!counter.isReady())
                            || (counter.isMissing())) {
                        continue;
                    }
                    // roll hits
                    int amsHits = Compute.d6(((WeaponType)counter.getType()).getDamage());

                    // build up some heat (assume target is ams owner)
                    if (counter.getType().hasFlag(WeaponType.F_HEATASDICE))
                        entityTarget.heatBuildup += Compute.d6(((WeaponType)counter.getType()).getHeat());
                    else
                        entityTarget.heatBuildup += ((WeaponType)counter.getType()).getHeat();

                    // decrement the ammo
                    if (mAmmo != null)
                        mAmmo.setShotsLeft(Math.max(0, mAmmo.getShotsLeft() - amsHits));

                    // set the ams as having fired
                    counter.setUsedThisRound(true);

                    amsShotDown[x]    = amsHits;
                    amsShotDownTotal += amsHits;
                }
            }
        }
        return amsShotDownTotal;
    }
    
    /*
     *  (non-Javadoc)
     * @see megamek.common.weapons.AttackHandler#handle(int, java.util.Vector)
     */
    public boolean handle(int phase, Vector vPhaseReport) {
        if (!this.cares(phase)) {
            return true;
        }
        Entity entityTarget = (target.getTargetType() == Targetable.TYPE_ENTITY) ? (Entity) target
                : null;
        final boolean targetInBuilding = Compute.isInBuilding(game,
                entityTarget);
        boolean bNemesisConfusable = isNemesisConfusable();

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
        // check for nemesis
        if (bNemesisConfusable && !waa.isNemesisConfused()) {
            boolean shotAtNemesisTarget = false;
            // loop through nemesis targets
            for (Enumeration e = game.getNemesisTargets(ae, target.getPosition());e.hasMoreElements();) {
                Entity entity = (Entity)e.nextElement();
                //friendly unit with attached iNarc Nemesis pod standing in the way
                r = new Report(3125);
                r.subject = subjectId;
                vPhaseReport.addElement(r);
                weapon.setUsedThisRound(false);
                WeaponAttackAction newWaa = new WeaponAttackAction(ae.getId(),
                    entity.getTargetId(), waa.getWeaponId());
                newWaa.setNemesisConfused(true);
                Entity ae = game.getEntity(waa.getEntityId());
                Mounted m = ae.getEquipment(waa.getWeaponId());
                Weapon w = (Weapon)m.getType();
                AttackHandler ah = w.fire(newWaa, game, server);
                WeaponHandler wh = (WeaponHandler)ah;
                // attack the new target, and if we hit it, return;
                wh.handle(phase, vPhaseReport);
                // if the new attack hit, we are finished.
                System.err.println(!wh.bMissed);
                if (!wh.bMissed) return false;
                shotAtNemesisTarget = true;
            }
            if (shotAtNemesisTarget) {
                //back to original target
                r = new Report(3130);
                r.subject = subjectId;
                r.newlines = 0;
                r.indent();
                vPhaseReport.addElement(r);
            }
        }
        if (toHit.getValue() == ToHitData.IMPOSSIBLE) {
            r = new Report(3135);
            r.subject = subjectId;
            r.add(toHit.getDesc());
            vPhaseReport.addElement(r);
            return false;
        }
        else if (toHit.getValue() == ToHitData.AUTOMATIC_FAIL) {
            r = new Report(3140);
            r.newlines = 0;
            r.subject = subjectId;
            r.add(toHit.getDesc());
            vPhaseReport.addElement(r);
        }
        else if (toHit.getValue() == ToHitData.AUTOMATIC_SUCCESS) {
            r = new Report(3145);
            r.newlines = 0;
            r.subject = subjectId;
            r.add(toHit.getDesc());
            vPhaseReport.addElement(r);
        }
        else {
            //roll to hit
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

        // Any necessary PSRs, jam checks, etc.
        // If this boolean is true, don't report
        // the miss later, as we already reported
        // it in doChecks
        boolean missReported = doChecks(vPhaseReport);

        // do we hit?
        bMissed = roll < toHit.getValue();
        
        // are we a glancing hit?
        if (game.getOptions().booleanOption("maxtech_glancing_blows")) {
            if (roll == toHit.getValue()) {
                bGlancing = true;
                r = new  Report(3186);
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
        useAmmo();
        addHeat();
        nDamPerHit = calcDamagePerHit();
        boolean bAllShotsHit = allShotsHit();
        
        //Do we need some sort of special resolution (minefields, artillery,
        if (specialResolution(vPhaseReport,entityTarget,bMissed)) {
            return false;
        }
        
        if (bMissed && !missReported) {
            reportMiss(vPhaseReport);

            // Works out fire setting, AMS shots, and whether continuation is
            // necessary.
            if (!handleSpecialMiss(entityTarget, targetInBuilding, bldg, vPhaseReport)) {
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
        if ( hits == 0 ) {
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
    
    protected boolean isNemesisConfusable() {
        // Are we iNarc Nemesis Confusable?
        boolean isNemesisConfusable = false;
        AmmoType atype = (AmmoType)weapon.getLinked().getType();
        Mounted mLinker = weapon.getLinkedBy();
        if ( wtype.getAmmoType() == AmmoType.T_ATM ||
             ( mLinker != null &&
               mLinker.getType() instanceof MiscType &&
               !mLinker.isDestroyed() && !mLinker.isMissing() && !mLinker.isBreached() &&
               mLinker.getType().hasFlag(MiscType.F_ARTEMIS) ) ) {
            if ((!weapon.getType().hasModes() ||
                 !weapon.curMode().equals("Indirect")) &&
                ( ((atype.getAmmoType() == AmmoType.T_ATM) &&
                   (atype.getMunitionType() == AmmoType.M_STANDARD ||
                    atype.getMunitionType() == AmmoType.M_EXTENDED_RANGE ||
                    atype.getMunitionType() == AmmoType.M_HIGH_EXPLOSIVE) ) ||
                  ((atype.getAmmoType() == AmmoType.T_LRM ||
                   atype.getAmmoType() == AmmoType.T_SRM) &&
                  atype.getMunitionType() == AmmoType.M_ARTEMIS_CAPABLE))) {
                isNemesisConfusable = true;
            }
        } else if (wtype.getAmmoType() == AmmoType.T_LRM ||
                   wtype.getAmmoType() == AmmoType.T_SRM) {
            if (atype.getMunitionType() == AmmoType.M_NARC_CAPABLE) {
                isNemesisConfusable = true;
            }
        }
        return isNemesisConfusable;
    }
}

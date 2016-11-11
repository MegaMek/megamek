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

import java.util.ArrayList;
import java.util.Enumeration;
import java.util.Vector;

import megamek.common.Aero;
import megamek.common.AmmoType;
import megamek.common.BattleArmor;
import megamek.common.Building;
import megamek.common.Compute;
import megamek.common.ComputeECM;
import megamek.common.Coords;
import megamek.common.Entity;
import megamek.common.IGame;
import megamek.common.Infantry;
import megamek.common.Mech;
import megamek.common.Minefield;
import megamek.common.Mounted;
import megamek.common.RangeType;
import megamek.common.Report;
import megamek.common.Tank;
import megamek.common.TargetRoll;
import megamek.common.Targetable;
import megamek.common.ToHitData;
import megamek.common.WeaponType;
import megamek.common.actions.WeaponAttackAction;
import megamek.common.options.OptionsConstants;
import megamek.server.Server;

/**
 * @author Sebastian Brocks, modified by Greg
 */
public class CLIATMHandler extends ATMHandler {

    /**
     *
     */
    private static final long serialVersionUID = 5476183194060709574L;
    boolean isAngelECMAffected = false;

    /**
     * @param t
     * @param w
     * @param g
     * @param s
     */
    public CLIATMHandler(ToHitData t, WeaponAttackAction w, IGame g, Server s) {
        super(t, w, g, s);
        isAngelECMAffected = ComputeECM.isAffectedByAngelECM(ae, ae.getPosition(),
                                                             target.getPosition());
    }

    /*
     * (non-Javadoc)
     *
     * @see megamek.common.weapons.WeaponHandler#calcDamagePerHit()
     */
    @Override
    protected int calcDamagePerHit() {
        double toReturn;
        AmmoType atype = (AmmoType) ammo.getType();
        if (atype.getMunitionType() == AmmoType.M_HIGH_EXPLOSIVE) {
            sSalvoType = " high-explosive missile(s) ";
            toReturn = 3;
        } else if (atype.getMunitionType() == AmmoType.M_EXTENDED_RANGE) {
            sSalvoType = " extended-range missile(s) ";
            toReturn = 1;
        } else if (atype.getMunitionType() == AmmoType.M_IATM_IMP) {
            sSalvoType = " IMP missile(s) ";
            toReturn = 1;
        } else if (atype.getMunitionType() == AmmoType.M_IATM_IIW) {
            sSalvoType = " IIW missile(s) ";
            toReturn = 2;
        } else {
            toReturn = 2;
        }

        if ((target instanceof Infantry) && !(target instanceof BattleArmor)) {
            toReturn = Compute.directBlowInfantryDamage(
                    wtype.getRackSize(), bDirect ? toHit.getMoS() / 3 : 0,
                    wtype.getInfantryDamageClass(),
                    ((Infantry) target).isMechanized(),
                    toHit.getThruBldg() != null, ae.getId(), calcDmgPerHitReport);
            if (bGlancing) {
                toReturn /= 2; // Is this correct for partial streak missiles??
                // it seems as if this only affects infantry -
                // I'm going to ignore this.
            }
        }

        return (int) toReturn;
    }

    /*
     * (non-Javadoc)
     *
     * @see megamek.common.weapons.WeaponHandler#calcHits(java.util.Vector)
     */
    @Override
    protected int calcHits(Vector<Report> vPhaseReport) {
        // conventional infantry gets hit in one lump - gets calculated in the
        // sub functions
        // don't need to check for BAs, because BA can't mount ATMs
        int hits;
        AmmoType atype = (AmmoType) ammo.getType();
        // TacOPs p.84 Cluster Hit Penalites will only effect ATM HE
        // I'm doing my own hit calcs here. Special ammo gets its own method.

        // compute ammount of missiles hit - this is the same for all ATM ammo
        // types.
        hits = calcMissileHits(vPhaseReport);

        // If we use IIW or IMP we are done.
        if ((atype.getMunitionType() == AmmoType.M_IATM_IIW)
            || (atype.getMunitionType() == AmmoType.M_IATM_IMP)) {
            return hits;
        }

        // Normalize into clusters (for standard and HE)
        if (atype.getMunitionType() != AmmoType.M_EXTENDED_RANGE) {
            // change to 5 damage clusters here, after AMS has been done
            hits = nDamPerHit * hits;
            nDamPerHit = 1;
        }

        return hits;
    }

    /**
     * Calculate the attack value based on range
     *
     * @return an <code>int</code> representing the attack value at that range.
     */
    @Override
    protected int calcAttackValue() {
        // TODO: Should handle speical munitions AV
        return super.calcAttackValue();
    }

    /*
     * (non-Javadoc)
     *
     * @see megamek.common.weapons.WeaponHandler#calcHits(java.util.Vector)
     */
    protected int calcMissileHits(Vector<Report> vPhaseReport) {

        AmmoType atype = (AmmoType) ammo.getType();

        // conventional infantry gets hit in one lump
        // BAs do one lump of damage per BA suit
        if ((target instanceof Infantry) && !(target instanceof BattleArmor)) {
            if (ae instanceof BattleArmor) {
                bSalvo = true;
                Report r = new Report(3325);
                r.newlines = 0;
                r.subject = subjectId;
                r.add(wtype.getRackSize()
                      * ((BattleArmor) ae).getShootingStrength());
                r.add(sSalvoType);
                r.add(toHit.getTableDesc());
                vPhaseReport.add(r);
                return ((BattleArmor) ae).getShootingStrength();
            }
            Report r = new Report(3325);
            r.subject = subjectId;
            r.newlines = 0;
            r.add(wtype.getRackSize());
            r.add(sSalvoType);
            r.add(toHit.getTableDesc());
            vPhaseReport.add(r);
            return 1;
        }

        int missilesHit;
        int nMissilesModifier = nSalvoBonus;

        // If we are in streak mode and miss, don't fire AMS! - However, AMS
        // shouldn't fire if we miss anyway, right?
        /*
         * if(bMissed && allShotsHit()) { return 0; }
         */
        // //////
        // TacOPs p.84 Cluster Hit Penalites will only effect ATM HE.
        // Since the IMP ammo has the same ranges as the ATM HE I assume it also
        // gets affected by this rule.
        // However, IMP is done in its own function - i think. Also if we have
        // the streak system enabled, this is not used
        int[] ranges = wtype.getRanges(weapon);
        boolean tacopscluster = game.getOptions().booleanOption(
                OptionsConstants.ADVCOMBAT_TACOPS_CLUSTERHITPEN);

        // Only apply if not all shots hit. IATM IMP have HE ranges and thus
        // suffer from spread too
        if (((atype.getMunitionType() == AmmoType.M_HIGH_EXPLOSIVE) || (atype
                .getMunitionType() == AmmoType.M_IATM_IMP))
            && tacopscluster
            && !allShotsHit()) {
            if (nRange <= 1) {
                nMissilesModifier += 1;
            } else if (nRange <= ranges[RangeType.RANGE_MEDIUM]) {
                nMissilesModifier += 0;
            } else {
                nMissilesModifier -= 1;
            }
        }
        // //////
        // This applies even with streaks.
        if (game.getOptions().booleanOption(OptionsConstants.ADVCOMBAT_TACOPS_RANGE)
            && (nRange > wtype.getRanges(weapon)[RangeType.RANGE_LONG])) {
            nMissilesModifier -= 2;
        }
        if (game.getOptions().booleanOption(OptionsConstants.ADVCOMBAT_TACOPS_LOS_RANGE)
                && (nRange > ranges[RangeType.RANGE_EXTREME])) {
            nMissilesModifier -= 3;
        }
        

        // Don't need to check for ECM here since we can't have artemis boni.
        // And Streak bonus is allready handled.

        // In theory there is no direct statement that says iATM doesn't have
        // the Artemis bonus too, but since it also has Streak and Artemis
        // doesn't work with IDF, I can skip the Artemis part here.
        // Also, I don't think iATM missiles are narc enabled.

        // we can only do glancing blows if we IDF. They don't occur even if
        // streak is deactivated by AECM - at least if the Streak Handler is
        // correct.
        if (bGlancing && weapon.curMode().equals("Indirect")) {
            nMissilesModifier -= 4;
        }

        // Seems to affect even streak, potentially countering the AMS penalty?
        if (bDirect) {
            nMissilesModifier += (toHit.getMoS() / 3) * 2;
        }

        // Affects streak too.
        if (game.getPlanetaryConditions().hasEMI()) {
            nMissilesModifier -= 2;
        }

        // add AMS mods
        int amsMod = getAMSHitsMod(vPhaseReport);
        nMissilesModifier += amsMod;

        if (allShotsHit()) {
            if (amsMod == 0) {
                missilesHit = wtype.getRackSize();
            } else {
                missilesHit = Compute.missilesHit(wtype.getRackSize(), amsMod,
                        weapon.isHotLoaded(), allShotsHit(), isAdvancedAMS());
            }
        } else {
            if (ae instanceof BattleArmor) {
                missilesHit = Compute.missilesHit(wtype.getRackSize()
                        * ((BattleArmor) ae).getShootingStrength(),
                        nMissilesModifier, weapon.isHotLoaded(), false,
                        isAdvancedAMS());
            } else {
                missilesHit = Compute.missilesHit(wtype.getRackSize(),
                        nMissilesModifier, weapon.isHotLoaded(), false,
                        isAdvancedAMS());
            }
        }

        if (missilesHit > 0) {
            Report r = new Report(3325);
            r.subject = subjectId;
            r.add(missilesHit);
            r.add(sSalvoType);
            r.add(toHit.getTableDesc());
            r.newlines = 0;
            vPhaseReport.addElement(r);
            if (nMissilesModifier != 0) {
                if (nMissilesModifier > 0) {
                    r = new Report(3340);
                } else {
                    r = new Report(3341);
                }
                r.subject = subjectId;
                r.add(nMissilesModifier);
                r.newlines = 0;
                vPhaseReport.addElement(r);
            }
        }
        Report r = new Report(3345);
        r.subject = subjectId;
        vPhaseReport.addElement(r);
        bSalvo = true;
        return missilesHit;
    }

    // I don't think i need to change anything here for iATMs. Seems just to
    // handle Minefield clearance
    @Override
    protected boolean specialResolution(Vector<Report> vPhaseReport,
                                        Entity entityTarget) {
        if (!bMissed
            && (target.getTargetType() == Targetable.TYPE_MINEFIELD_CLEAR)) {
            Report r = new Report(3255);
            r.indent(1);
            r.subject = subjectId;
            vPhaseReport.addElement(r);
            Coords coords = target.getPosition();

            Enumeration<Minefield> minefields = game.getMinefields(coords)
                                                    .elements();
            ArrayList<Minefield> mfRemoved = new ArrayList<Minefield>();
            while (minefields.hasMoreElements()) {
                Minefield mf = minefields.nextElement();
                if (server.clearMinefield(mf, ae,
                        Minefield.CLEAR_NUMBER_WEAPON, vPhaseReport)) {
                    mfRemoved.add(mf);
                }
            }
            // we have to do it this way to avoid a concurrent error problem
            for (Minefield mf : mfRemoved) {
                server.removeMinefield(mf);
            }
            return true;
        }

        return false;
    }

    /*
     * (non-Javadoc)
     *
     * @see megamek.common.weapons.WeaponHandler#allShotsHit()
     */
    @Override
    protected boolean allShotsHit() {
        // If we IDF, we don't get the streak bonus
        if (weapon.curMode().equals("Indirect")) {
            return super.allShotsHit();
        }
        // If we DF, we get the streak bonus if not in AECM
        return super.allShotsHit() || !isAngelECMAffected;
    }

    /*
     * (non-Javadoc)
     *
     * @see megamek.common.weapons.WeaponHandler#addHeat()
     */
    @Override
    protected void addHeat() {
        // call super function if we are in IDF mode since we don't have streak
        // there.
        if (weapon.curMode().equals("Indirect")) {
            super.addHeat();
            return;
        }

        if (!(toHit.getValue() == TargetRoll.IMPOSSIBLE)
            && (roll >= toHit.getValue())) {
            super.addHeat();
        }
    }

    /*
     * (non-Javadoc)
     *
     * @see megamek.common.weapons.WeaponHandler#UseAmmo()
     */
    @Override
    protected void useAmmo() {
        // call super function if we are in IDF mode, since we don't have streak
        // there.
        if (weapon.curMode().equals("Indirect")) {
            super.useAmmo();
            return;
        }
        checkAmmo();
        if (ammo == null) {// Can't happen. w/o legal ammo, the weapon
            // *shouldn't* fire.
            System.out.println("Handler can't find any ammo!  Oh no!");
        }
        if (ammo.getUsableShotsLeft() <= 0) {
            ae.loadWeaponWithSameAmmo(weapon);
            ammo = weapon.getLinked();
        }
        if (roll >= toHit.getValue()) {
            ammo.setShotsLeft(ammo.getBaseShotsLeft() - 1);
            if (wtype.hasFlag(WeaponType.F_ONESHOT)) {
                weapon.setFired(true);
            }
            setDone();
        }
    }

    /*
     * (non-Javadoc)
     *
     * @see megamek.common.weapons.WeaponHandler#reportMiss(java.util.Vector)
     */
    @Override
    protected void reportMiss(Vector<Report> vPhaseReport) {
        // again, call super if we are in IDF mode.
        if (weapon.curMode().equals("Indirect")) {
            super.reportMiss(vPhaseReport);
            return;
        }
        // if (!isAngelECMAffected) {
        // no lock
        Report r = new Report(3215);
        r.subject = subjectId;
        vPhaseReport.addElement(r);
        /*
         * } else { super.reportMiss(vPhaseReport); }
         */
    }

    /*
     * (non-Javadoc)
     *
     * @see
     * megamek.common.weapons.MissileWeaponHandler#handleSpecialMiss(megamek
     * .common.Entity, boolean, megamek.common.Building, java.util.Vector) TODO:
     * Greg: Handle special misses for IMP and IIW warheads.
     */
    @Override
    protected boolean handleSpecialMiss(Entity entityTarget,
            boolean bldgDamagedOnMiss, Building bldg,
            Vector<Report> vPhaseReport) {
        if (weapon.curMode().equals("Indirect")) {
            return super.handleSpecialMiss(entityTarget, bldgDamagedOnMiss,
                    bldg, vPhaseReport);
        }
        return false;
    }

    /*
     * (non-Javadoc) Override the handle function to handle IIW and IMP warheads
     * here. Call super function for the regular ATM ammo.
     */

    @Override
    public boolean handle(IGame.Phase phase, Vector<Report> vPhaseReport) {
        AmmoType atype = (AmmoType) ammo.getType();
        if (atype.getMunitionType() == AmmoType.M_IATM_IIW) {
            if (!cares(phase)) {
                return true;
            }
            sSalvoType = " IIW missile(s) ";
            Entity entityTarget = (target.getTargetType() == Targetable.TYPE_ENTITY) ? (Entity) target
                    : null;
            final boolean targetInBuilding = Compute.isInBuilding(game,
                    entityTarget);
            final boolean bldgDamagedOnMiss = targetInBuilding
                    && !(target instanceof Infantry)
                    && ae.getPosition().distance(target.getPosition()) <= 1;

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
            if (game.getOptions().booleanOption(OptionsConstants.ADVCOMBAT_TACOPS_GLANCING_BLOWS)) {
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
            bDirect = game.getOptions().booleanOption(OptionsConstants.ADVCOMBAT_TACOPS_DIRECT_BLOW)
                      && ((toHit.getMoS() / 3) >= 1) && (entityTarget != null);
            if (bDirect) {
                r = new Report(3189);
                r.subject = ae.getId();
                r.newlines = 0;
                vPhaseReport.addElement(r);
            }

            // Do this stuff first, because some weapon's miss report reference
            // the
            // amount of shots fired and stuff.
            addHeat();

            // Any necessary PSRs, jam checks, etc.
            // If this boolean is true, don't report
            // the miss later, as we already reported
            // it in doChecks
            boolean missReported = doChecks(vPhaseReport);
            if (missReported) {
                bMissed = true;
            }

            if (bMissed && !missReported) {
                reportMiss(vPhaseReport);
                // Works out fire setting, AMS shots, and whether continuation
                // is
                // necessary.
                if (!handleSpecialMiss(entityTarget, bldgDamagedOnMiss,
                        bldg, vPhaseReport)) {
                    return false;
                }
            }

            // yeech. handle damage. . different weapons do this in very
            // different
            // ways
            int hits = calcHits(vPhaseReport);
            Report.addNewline(vPhaseReport);

            if (bMissed) {
                return false;
            } // End missed-target

            // light inferno missiles all at once, if not missed
            if (!bMissed) {
                vPhaseReport.addAll(server.deliverInfernoMissiles(ae, target,
                        hits, weapon.getCalledShot().getCall()));
            }
            return false;
        } else if (atype.getMunitionType() == AmmoType.M_IATM_IMP) {
            if (!cares(phase)) {
                return true;
            }
            Entity entityTarget = (target.getTargetType() == Targetable.TYPE_ENTITY) ? (Entity) target
                    : null;
            final boolean targetInBuilding = Compute.isInBuilding(game,
                    entityTarget);
            final boolean bldgDamagedOnMiss = targetInBuilding
                    && !(target instanceof Infantry)
                    && ae.getPosition().distance(target.getPosition()) <= 1;
            boolean bNemesisConfusable = isNemesisConfusable();

            if (entityTarget != null) {
                ae.setLastTarget(entityTarget.getId());
                ae.setLastTargetDisplayName(entityTarget.getDisplayName());
            }

            // Which building takes the damage?
            Building bldg = game.getBoard().getBuildingAt(target.getPosition());
            String number = nweapons > 1 ? " (" + nweapons + ")" : "";
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
            // check for nemesis
            boolean shotAtNemesisTarget = false;
            if (bNemesisConfusable && !waa.isNemesisConfused()) {
                // loop through nemesis targets
                for (Enumeration<Entity> e = game.getNemesisTargets(ae,
                        target.getPosition()); e.hasMoreElements();) {
                    Entity entity = e.nextElement();
                    // friendly unit with attached iNarc Nemesis pod standing in
                    // the
                    // way
                    r = new Report(3125);
                    r.subject = subjectId;
                    vPhaseReport.addElement(r);
                    weapon.setUsedThisRound(false);
                    WeaponAttackAction newWaa = new WeaponAttackAction(
                            ae.getId(), entity.getTargetId(), waa.getWeaponId());
                    newWaa.setNemesisConfused(true);
                    Mounted m = ae.getEquipment(waa.getWeaponId());
                    Weapon w = (Weapon) m.getType();
                    AttackHandler ah = w.fire(newWaa, game, server);
                    // increase ammo by one, becaues we just incorrectly used
                    // one up
                    weapon.getLinked().setShotsLeft(
                            weapon.getLinked().getBaseShotsLeft() + 1);
                    // if the new attack has an impossible to-hit, go on to next
                    // entity
                    if (ah == null) {
                        continue;
                    }
                    WeaponHandler wh = (WeaponHandler) ah;
                    // attack the new target, and if we hit it, return;
                    wh.handle(phase, vPhaseReport);
                    // if the new attack hit, we are finished.
                    if (!wh.bMissed) {
                        return false;
                    }
                    shotAtNemesisTarget = true;
                }
                if (shotAtNemesisTarget) {
                    // back to original target
                    r = new Report(3130);
                    r.subject = subjectId;
                    r.newlines = 0;
                    r.indent();
                    vPhaseReport.addElement(r);
                }
            }
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
            if (game.getOptions().booleanOption(OptionsConstants.ADVCOMBAT_TACOPS_GLANCING_BLOWS)) {
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
            bDirect = game.getOptions().booleanOption(OptionsConstants.ADVCOMBAT_TACOPS_DIRECT_BLOW)
                      && ((toHit.getMoS() / 3) >= 1) && (entityTarget != null);
            if (bDirect) {
                r = new Report(3189);
                r.subject = ae.getId();
                r.newlines = 0;
                vPhaseReport.addElement(r);
            }

            // Do this stuff first, because some weapon's miss report reference
            // the
            // amount of shots fired and stuff.
            if (!shotAtNemesisTarget) {
                addHeat();
            }
            // Any necessary PSRs, jam checks, etc.
            // If this boolean is true, don't report
            // the miss later, as we already reported
            // it in doChecks
            boolean missReported = doChecks(vPhaseReport);

            nDamPerHit = calcDamagePerHit();

            // Do we need some sort of special resolution (minefields,
            // artillery,
            if (specialResolution(vPhaseReport, entityTarget)) {
                return false;
            }

            if (bMissed && !missReported) {
                reportMiss(vPhaseReport);

                // Works out fire setting, AMS shots, and whether continuation
                // is necessary.
                if (!handleSpecialMiss(entityTarget, bldgDamagedOnMiss,
                        bldg, vPhaseReport)) {
                    return false;
                }
            }

            // yeech. handle damage. . different weapons do this in very
            // different
            // ways
            int hits = 1;
            if (!(target.isAirborne())) {
                hits = calcHits(vPhaseReport);
            }
            int nCluster = calcnCluster();

            // Now I need to adjust this for attacks on aeros because they use
            // attack values and different rules
            if (target.isAirborne() || game.getBoard().inSpace()) {
                // this will work differently for cluster and non-cluster
                // weapons, and differently for capital fighter/fighter
                // squadrons
                nCluster = calcnClusterAero(entityTarget);
                if (nCluster > 1) {
                    bSalvo = true;
                    nDamPerHit = 1;
                    hits = attackValue;
                } else {
                    if (ae.isCapitalFighter()) {
                        bSalvo = true;
                        if (nweapons > 1) {
                            nweaponsHit = Compute.missilesHit(nweapons,
                                    ((Aero) ae).getClusterMods());
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

            if (bMissed) {
                return false;

            } // End missed-target

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
                    firstHit = false;
                    // do IMP stuff here!
                    if ((entityTarget instanceof Mech)
                            || (entityTarget instanceof Aero)
                            || (entityTarget instanceof Tank)) {
                        entityTarget.addIMPHits(Math.max(0,
                                hits - Math.max(0, bldgAbsorbs)));
                    }
                }
            } // Handle the next cluster.
            Report.addNewline(vPhaseReport);
            return false;
        } else {
            return super.handle(phase, vPhaseReport);
        }
    }
}

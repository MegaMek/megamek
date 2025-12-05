/*
 * Copyright (c) 2005 - Ben Mazur (bmazur@sev.org).
 * Copyright (C) 2013-2025 The MegaMek Team. All Rights Reserved.
 *
 * This file is part of MegaMek.
 *
 * MegaMek is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License (GPL),
 * version 3 or (at your option) any later version,
 * as published by the Free Software Foundation.
 *
 * MegaMek is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty
 * of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU General Public License for more details.
 *
 * A copy of the GPL should have been included with this project;
 * if not, see <https://www.gnu.org/licenses/>.
 *
 * NOTICE: The MegaMek organization is a non-profit group of volunteers
 * creating free software for the BattleTech community.
 *
 * MechWarrior, BattleMech, `Mech and AeroTech are registered trademarks
 * of The Topps Company, Inc. All Rights Reserved.
 *
 * Catalyst Game Labs and the Catalyst Game Labs logo are trademarks of
 * InMediaRes Productions, LLC.
 *
 * MechWarrior Copyright Microsoft Corporation. MegaMek was created under
 * Microsoft's "Game Content Usage Rules"
 * <https://www.xbox.com/en-US/developers/rules> and it is not endorsed by or
 * affiliated with Microsoft.
 */

package megamek.common.weapons.handlers;

import java.io.Serial;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.Vector;

import megamek.common.RangeType;
import megamek.common.Report;
import megamek.common.ToHitData;
import megamek.common.actions.WeaponAttackAction;
import megamek.common.battleArmor.BattleArmor;
import megamek.common.board.Coords;
import megamek.common.compute.Compute;
import megamek.common.compute.ComputeECM;
import megamek.common.enums.GamePhase;
import megamek.common.equipment.AmmoMounted;
import megamek.common.equipment.AmmoType;
import megamek.common.equipment.Minefield;
import megamek.common.equipment.Mounted;
import megamek.common.equipment.WeaponType;
import megamek.common.game.Game;
import megamek.common.loaders.EntityLoadingException;
import megamek.common.options.OptionsConstants;
import megamek.common.planetaryConditions.PlanetaryConditions;
import megamek.common.rolls.TargetRoll;
import megamek.common.units.Aero;
import megamek.common.units.Entity;
import megamek.common.units.IBuilding;
import megamek.common.units.Infantry;
import megamek.common.units.Mek;
import megamek.common.units.Tank;
import megamek.common.units.Targetable;
import megamek.common.weapons.Weapon;
import megamek.server.totalWarfare.TWGameManager;

/**
 * @author Sebastian Brocks, modified by Greg
 */
public class CLIATMHandler extends ATMHandler {
    @Serial
    private static final long serialVersionUID = 5476183194060709574L;
    boolean isAngelECMAffected;

    public CLIATMHandler(ToHitData t, WeaponAttackAction w, Game g, TWGameManager m) throws EntityLoadingException {
        super(t, w, g, m);
        isAngelECMAffected = ComputeECM.isAffectedByAngelECM(attackingEntity,
              attackingEntity.getPosition(),
              target.getPosition());
    }

    @Override
    protected int calcDamagePerHit() {
        double toReturn;
        AmmoType ammoType = ammo.getType();
        if (ammoType.getMunitionType().contains(AmmoType.Munitions.M_HIGH_EXPLOSIVE)) {
            sSalvoType = " high-explosive missile(s) ";
            toReturn = 3;
        } else if (ammoType.getMunitionType().contains(AmmoType.Munitions.M_EXTENDED_RANGE)) {
            sSalvoType = " extended-range missile(s) ";
            toReturn = 1;
        } else if (ammoType.getMunitionType().contains(AmmoType.Munitions.M_IATM_IMP)) {
            sSalvoType = " IMP missile(s) ";
            toReturn = 1;
        } else if (ammoType.getMunitionType().contains(AmmoType.Munitions.M_IATM_IIW)) {
            sSalvoType = " IIW missile(s) ";
            toReturn = 2;
        } else {
            toReturn = 2;
        }

        if (target.isConventionalInfantry()) {
            toReturn = Compute.directBlowInfantryDamage(
                  weaponType.getRackSize(), bDirect ? toHit.getMoS() / 3 : 0,
                  weaponType.getInfantryDamageClass(),
                  ((Infantry) target).isMechanized(),
                  toHit.getThruBldg() != null, weaponEntity.getId(), calcDmgPerHitReport);

            // some question here about "partial streak missiles"
            if (streakInactive()) {
                toReturn = applyGlancingBlowModifier(toReturn, true);
            }
        }

        return (int) toReturn;
    }

    @Override
    protected int calcHits(Vector<Report> vPhaseReport) {
        // conventional infantry gets hit in one lump - gets calculated in the sub
        // functions
        // don't need to check for BAs, because BA can't mount ATMs
        AmmoType ammoType = ammo.getType();
        // TacOPs p.84 Cluster Hit Penalties will only affect ATM HE
        // I'm doing my own hit calculations here. Special ammo gets its own method.

        // compute amount of missiles hit - this is the same for all ATM ammo types.
        int hits = calcMissileHits(vPhaseReport);

        // If we use IIW or IMP we are done.
        if ((ammoType.getMunitionType().contains(AmmoType.Munitions.M_IATM_IIW))
              || (ammoType.getMunitionType().contains(AmmoType.Munitions.M_IATM_IMP))) {
            return hits;
        }

        // Normalize into clusters (for standard and HE)
        if (!ammoType.getMunitionType().contains(AmmoType.Munitions.M_EXTENDED_RANGE)) {
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
        // TODO: Should handle special munitions AV
        return super.calcAttackValue();
    }

    protected int calcMissileHits(Vector<Report> vPhaseReport) {
        AmmoType ammoType = ammo.getType();

        // conventional infantry gets hit in one lump
        // BAs do one lump of damage per BA suit
        if (target.isConventionalInfantry()) {
            if (attackingEntity instanceof BattleArmor) {
                bSalvo = true;
                Report report = new Report(3325);
                report.newlines = 0;
                report.subject = subjectId;
                report.add(weaponType.getRackSize() * ((BattleArmor) attackingEntity).getShootingStrength());
                report.add(sSalvoType);
                report.add(toHit.getTableDesc());
                vPhaseReport.add(report);
                return ((BattleArmor) attackingEntity).getShootingStrength();
            }
            Report report = new Report(3325);
            report.subject = subjectId;
            report.newlines = 0;
            report.add(weaponType.getRackSize());
            report.add(sSalvoType);
            report.add(toHit.getTableDesc());
            vPhaseReport.add(report);
            return 1;
        }

        int missilesHit;
        int nMissilesModifier = nSalvoBonus;

        // If we are in streak mode and miss, don't fire AMS! - However, AMS
        // shouldn't fire if we miss anyway, right?
        /*
         * if (bMissed && allShotsHit()) { return 0; }
         */
        // //////
        // TacOPs p.84 Cluster Hit Penalties will only affect ATM HE.
        // Since the IMP ammo has the same ranges as the ATM HE I assume it also
        // gets affected by this rule.
        // However, IMP is done in its own function - I think. Also, if we have
        // the streak system enabled, this is not used
        int[] ranges = weaponType.getRanges(weapon);
        boolean tacOpsCluster = game.getOptions()
              .booleanOption(OptionsConstants.ADVANCED_COMBAT_TAC_OPS_CLUSTER_HIT_PEN);

        // Only apply if not all shots hit. IATM IMP have HE ranges and thus
        // suffer from spread too
        if (((ammoType.getMunitionType().contains(AmmoType.Munitions.M_HIGH_EXPLOSIVE))
              || (ammoType.getMunitionType().contains(AmmoType.Munitions.M_IATM_IMP)))
              && tacOpsCluster && !allShotsHit()) {
            if (nRange <= 1) {
                nMissilesModifier += 1;
            } else if (nRange > ranges[RangeType.RANGE_MEDIUM]) {
                nMissilesModifier -= 1;
            }
        }

        // //////
        // This applies even with streaks.
        if (game.getOptions().booleanOption(OptionsConstants.ADVANCED_COMBAT_TAC_OPS_RANGE)
              && (nRange > weaponType.getRanges(weapon)[RangeType.RANGE_LONG])) {
            nMissilesModifier -= 2;
        }
        if (game.getOptions().booleanOption(OptionsConstants.ADVANCED_COMBAT_TAC_OPS_LOS_RANGE)
              && (nRange > ranges[RangeType.RANGE_EXTREME])) {
            nMissilesModifier -= 3;
        }

        // Don't need to check for ECM here since we can't have artemis boni.
        // And Streak bonus is already handled.

        // In theory there is no direct statement that says iATM doesn't have
        // the Artemis bonus too, but since it also has Streak and Artemis
        // doesn't work with IDF, I can skip the Artemis part here.
        // Also, I don't think iATM missiles are narc enabled.

        // Fusillade doesn't have streak effect, but has the built-in Artemis IV of the
        // ATM.
        if (weapon.getType().hasFlag(WeaponType.F_PROTO_WEAPON)) {
            if (ComputeECM.isAffectedByECM(attackingEntity, attackingEntity.getPosition(), target.getPosition())) {
                Report r = new Report(3330);
                r.subject = subjectId;
                r.newlines = 0;
                vPhaseReport.addElement(r);
            } else {
                nMissilesModifier += 2;
            }
        }

        // we can only do glancing blows if we IDF. They don't occur even if
        // streak is deactivated by AECM - at least if the Streak Handler is
        // correct.
        if (bGlancing && streakInactive()) {
            nMissilesModifier -= 4;
        }

        if (bLowProfileGlancing && streakInactive()) {
            nMissilesModifier -= 4;
        }

        // Seems to affect even streak, potentially countering the AMS penalty?
        if (bDirect) {
            nMissilesModifier += (toHit.getMoS() / 3) * 2;
        }

        // Affects streak too.
        PlanetaryConditions conditions = game.getPlanetaryConditions();
        if (conditions.getEMI().isEMI()) {
            nMissilesModifier -= 2;
        }

        // add AMS mods
        int amsMod = getAMSHitsMod(vPhaseReport);
        nMissilesModifier += amsMod;

        if (game.getOptions().booleanOption(OptionsConstants.ADVANCED_AERO_RULES_AERO_SANITY)) {
            Entity entityTarget = (target.getTargetType() == Targetable.TYPE_ENTITY) ? (Entity) target
                  : null;
            if (entityTarget != null && entityTarget.isLargeCraft()) {
                nMissilesModifier -= (int) Math.floor(getAeroSanityAMSHitsMod());
            }
        }

        if (allShotsHit()) {
            // We want buildings and large craft to be able to affect this number with AMS
            // treat as a Streak launcher (cluster roll 11) to make this happen
            missilesHit = Compute.missilesHit(weaponType.getRackSize(),
                  nMissilesModifier, weapon.isHotLoaded(), true,
                  isAdvancedAMS());
        } else {
            if (attackingEntity instanceof BattleArmor) {
                missilesHit = Compute.missilesHit(weaponType.getRackSize()
                            * ((BattleArmor) attackingEntity).getShootingStrength(),
                      nMissilesModifier, weapon.isHotLoaded(), false,
                      isAdvancedAMS());
            } else {
                missilesHit = Compute.missilesHit(weaponType.getRackSize(),
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

    // I don't think I need to change anything here for iATMs. Seems just to
    // handle Minefield clearance
    @Override
    protected boolean specialResolution(Vector<Report> vPhaseReport, Entity entityTarget) {
        if (!bMissed
              && (target.getTargetType() == Targetable.TYPE_MINEFIELD_CLEAR)) {
            Report report = new Report(3255);
            report.indent(1);
            report.subject = subjectId;
            vPhaseReport.addElement(report);
            Coords coords = target.getPosition();

            Enumeration<Minefield> minefields = game.getMinefields(coords).elements();
            ArrayList<Minefield> mfRemoved = new ArrayList<>();
            while (minefields.hasMoreElements()) {
                Minefield mf = minefields.nextElement();
                if (gameManager.clearMinefield(mf, attackingEntity, Minefield.CLEAR_NUMBER_WEAPON, vPhaseReport)) {
                    mfRemoved.add(mf);
                }
            }
            // we have to do it this way to avoid a concurrent error problem
            for (Minefield mf : mfRemoved) {
                gameManager.removeMinefield(mf);
            }
            return true;
        }

        return false;
    }

    @Override
    protected boolean allShotsHit() {
        // If we IDF, we don't get the streak bonus
        if (streakInactive()) {
            return super.allShotsHit();
        }
        // If we DF, we get the streak bonus if not in AECM
        return super.allShotsHit() || !isAngelECMAffected;
    }

    @Override
    protected void addHeat() {
        // call super function if we are in IDF mode since we don't have streak
        // there.
        if (streakInactive()) {
            super.addHeat();
            return;
        }

        if (!(toHit.getValue() == TargetRoll.IMPOSSIBLE)
              && (roll.getIntValue() >= toHit.getValue())) {
            super.addHeat();
        }
    }

    @Override
    protected void useAmmo() {
        // call super function if we are in IDF mode, since we don't have streak
        // there.
        if (streakInactive()) {
            super.useAmmo();
            return;
        }
        checkAmmo();
        if (ammo == null) {// Can't happen. w/o legal ammo, the weapon *shouldn't* fire.
            System.out.println("Handler can't find any ammo!  Oh no!");
            return;
        }

        if (ammo.getUsableShotsLeft() <= 0) {
            weaponEntity.loadWeaponWithSameAmmo(weapon);
            if (weapon.getLinked() instanceof AmmoMounted ammoMounted) {
                ammo = ammoMounted;
            } else {
                throw new IllegalStateException(
                      "Weapon " + weapon.getType().getName()
                            + " has no linked ammo!");
            }
        }
        if (roll.getIntValue() >= toHit.getValue()) {
            ammo.setShotsLeft(ammo.getBaseShotsLeft() - 1);
            if (weaponType.hasFlag(WeaponType.F_ONE_SHOT)) {
                weapon.setFired(true);
            }
            setDone();
        }
    }

    @Override
    protected void reportMiss(Vector<Report> vPhaseReport) {
        // again, call super if we are in IDF mode.
        if (streakInactive()) {
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
     * megamek.common.weapons.handlers.MissileWeaponHandler#handleSpecialMiss(megamek
     * .common.Entity, boolean, megamek.common.units.Building, java.util.Vector) TODO:
     * Greg: Handle special misses for IMP and IIW warheads.
     */
    @Override
    protected boolean handleSpecialMiss(Entity entityTarget,
          boolean bldgDamagedOnMiss, IBuilding bldg,
          Vector<Report> vPhaseReport) {
        if (streakInactive()) {
            return super.handleSpecialMiss(entityTarget, bldgDamagedOnMiss,
                  bldg, vPhaseReport);
        }
        return false;
    }

    /**
     * Streak effect only works for iATM when not firing indirectly, and not at all for fusillade.
     */
    private boolean streakInactive() {
        return weapon.curMode().equals("Indirect")
              || weapon.getType().hasFlag(WeaponType.F_PROTO_WEAPON);
    }

    /*
     * (non-Javadoc) Override the handle function to handle IIW and IMP warheads
     * here. Call super function for the regular ATM ammo.
     */

    @Override
    public boolean handle(GamePhase phase, Vector<Report> vPhaseReport) {
        AmmoType ammoType = ammo.getType();
        if (ammoType.getMunitionType().contains(AmmoType.Munitions.M_IATM_IIW)) {
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
                  && attackingEntity.getPosition().distance(target.getPosition()) <= 1;

            // Which building takes the damage?
            IBuilding bldg = game.getBoard().getBuildingAt(target.getPosition());

            // Report weapon attack and its to-hit value.
            Report r = new Report(3115);
            r.indent();
            r.newlines = 0;
            r.subject = subjectId;
            r.add(weaponType.getName() + " (" + ammoType.getShortName() + ")");
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
            bMissed = roll.getIntValue() < toHit.getValue();

            // are we a glancing hit?
            setGlancingBlowFlags(entityTarget);
            addGlancingBlowReports(vPhaseReport);

            // Set Margin of Success/Failure.
            toHit.setMoS(roll.getIntValue() - Math.max(2, toHit.getValue()));
            bDirect = game.getOptions().booleanOption(OptionsConstants.ADVANCED_COMBAT_TAC_OPS_DIRECT_BLOW)
                  && ((toHit.getMoS() / 3) >= 1) && (entityTarget != null);
            if (bDirect) {
                r = new Report(3189);
                r.subject = attackingEntity.getId();
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

            //  handle damage. . different weapons do this in very
            // different
            // ways
            int hits = calcHits(vPhaseReport);
            Report.addNewline(vPhaseReport);

            if (!bMissed) {
                // light inferno missiles all at once, if not missed
                vPhaseReport.addAll(gameManager.deliverInfernoMissiles(weaponEntity, target,
                      hits, weapon.getCalledShot().getCall()));
            }
            return false;
        } else if (ammoType.getMunitionType().contains(AmmoType.Munitions.M_IATM_IMP)) {
            if (!cares(phase)) {
                return true;
            }
            Entity entityTarget = (target.getTargetType() == Targetable.TYPE_ENTITY) ? (Entity) target
                  : null;
            final boolean targetInBuilding = Compute.isInBuilding(game,
                  entityTarget);
            final boolean bldgDamagedOnMiss = targetInBuilding
                  && !(target instanceof Infantry)
                  && attackingEntity.getPosition().distance(target.getPosition()) <= 1;
            boolean bNemesisConfusable = isNemesisConfusable();

            if (entityTarget != null) {
                attackingEntity.setLastTarget(entityTarget.getId());
                attackingEntity.setLastTargetDisplayName(entityTarget.getDisplayName());
            }

            // Which building takes the damage?
            IBuilding bldg = game.getBoard().getBuildingAt(target.getPosition());
            String number = numWeapons > 1 ? " (" + numWeapons + ")" : "";
            // Report weapon attack and its to-hit value.
            Report report = new Report(3115);
            report.indent();
            report.newlines = 0;
            report.subject = subjectId;
            report.add(weaponType.getName() + number);
            if (entityTarget != null) {
                report.addDesc(entityTarget);
            } else {
                report.messageId = 3120;
                report.add(target.getDisplayName(), true);
            }
            vPhaseReport.addElement(report);
            // check for nemesis
            boolean shotAtNemesisTarget = false;
            if (bNemesisConfusable && !weaponAttackAction.isNemesisConfused()) {
                // loop through nemesis targets
                for (Enumeration<Entity> e = game.getNemesisTargets(attackingEntity,
                      target.getPosition()); e.hasMoreElements(); ) {
                    Entity entity = e.nextElement();
                    // friendly unit with attached iNarc Nemesis pod standing in
                    // the
                    // way
                    report = new Report(3125);
                    report.subject = subjectId;
                    vPhaseReport.addElement(report);
                    weapon.setUsedThisRound(false);
                    WeaponAttackAction newWaa = new WeaponAttackAction(
                          weaponEntity.getId(), entity.getId(), weaponAttackAction.getWeaponId());
                    newWaa.setNemesisConfused(true);
                    Mounted<?> m = weaponEntity.getEquipment(weaponAttackAction.getWeaponId());
                    Weapon w = (Weapon) m.getType();
                    AttackHandler attackHandler = w.fire(newWaa, game, gameManager);
                    // increase ammo by one, because we just incorrectly used
                    // one up
                    weapon.getLinked().setShotsLeft(
                          weapon.getLinked().getBaseShotsLeft() + 1);
                    // if the new attack has an impossible to-hit, go on to next
                    // entity
                    if (attackHandler == null) {
                        continue;
                    }
                    WeaponHandler wh = (WeaponHandler) attackHandler;
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
                    report = new Report(3130);
                    report.subject = subjectId;
                    report.newlines = 0;
                    report.indent();
                    vPhaseReport.addElement(report);
                }
            }
            if (toHit.getValue() == TargetRoll.IMPOSSIBLE) {
                report = new Report(3135);
                report.subject = subjectId;
                report.add(toHit.getDesc());
                vPhaseReport.addElement(report);
                return false;
            } else if (toHit.getValue() == TargetRoll.AUTOMATIC_FAIL) {
                report = new Report(3140);
                report.newlines = 0;
                report.subject = subjectId;
                report.add(toHit.getDesc());
                vPhaseReport.addElement(report);
            } else if (toHit.getValue() == TargetRoll.AUTOMATIC_SUCCESS) {
                report = new Report(3145);
                report.newlines = 0;
                report.subject = subjectId;
                report.add(toHit.getDesc());
                vPhaseReport.addElement(report);
            } else {
                // roll to hit
                report = new Report(3150);
                report.newlines = 0;
                report.subject = subjectId;
                report.add(toHit);
                vPhaseReport.addElement(report);
            }

            // dice have been rolled, thanks
            report = new Report(3155);
            report.newlines = 0;
            report.subject = subjectId;
            report.add(roll);
            vPhaseReport.addElement(report);

            // do we hit?
            bMissed = roll.getIntValue() < toHit.getValue();

            // are we a glancing hit?
            setGlancingBlowFlags(entityTarget);
            addGlancingBlowReports(vPhaseReport);

            // Set Margin of Success/Failure.
            toHit.setMoS(roll.getIntValue() - Math.max(2, toHit.getValue()));
            bDirect = game.getOptions().booleanOption(OptionsConstants.ADVANCED_COMBAT_TAC_OPS_DIRECT_BLOW)
                  && ((toHit.getMoS() / 3) >= 1) && (entityTarget != null);
            if (bDirect) {
                report = new Report(3189);
                report.subject = attackingEntity.getId();
                report.newlines = 0;
                vPhaseReport.addElement(report);
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

            //  handle damage. . different weapons do this in very
            // different
            // ways
            int hits = 1;
            if (!(target.isAirborne())) {
                hits = calcHits(vPhaseReport);
            }
            int nCluster = calculateNumCluster();

            // Now I need to adjust this for attacks on aerospace because they use
            // attack values and different rules
            if (target.isAirborne() || game.getBoard().isSpace()) {
                // this will work differently for cluster and non-cluster
                // weapons, and differently for capital fighter/fighter
                // squadrons
                nCluster = calculateNumClusterAero(entityTarget);
                if (nCluster > 1) {
                    bSalvo = true;
                    nDamPerHit = 1;
                    hits = attackValue;
                } else {
                    if (attackingEntity.isCapitalFighter()) {
                        bSalvo = true;
                        if (numWeapons > 1) {
                            numWeaponsHit = Compute.missilesHit(numWeapons,
                                  ((Aero) attackingEntity).getClusterMods());
                            report = new Report(3325);
                            report.subject = subjectId;
                            report.add(numWeaponsHit);
                            report.add(" weapon(s) ");
                            report.add(" ");
                            report.newlines = 0;
                            vPhaseReport.add(report);
                        }
                        nDamPerHit = attackValue * numWeaponsHit;
                    } else {
                        bSalvo = false;
                        nDamPerHit = attackValue;
                    }
                    hits = 1;
                    nCluster = 1;
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
                bldgAbsorbs = bldg.getAbsorption(target.getPosition());
            }

            // Attacking infantry in buildings from same building
            if (targetInBuilding && (bldg != null)
                  && (toHit.getThruBldg() != null)
                  && (entityTarget instanceof Infantry)) {
                // If elevation is the same, building doesn't absorb
                if (attackingEntity.getElevation() != entityTarget.getElevation()) {
                    int dmgClass = weaponType.getInfantryDamageClass();
                    int nDamage;
                    if (dmgClass < WeaponType.WEAPON_BURST_1D6) {
                        nDamage = nDamPerHit * Math.min(nCluster, hits);
                    } else {
                        // Need to indicate to handleEntityDamage that the
                        // absorbed damage shouldn't reduce incoming damage,
                        // since the incoming damage was reduced in
                        // Compute.directBlowInfantryDamage
                        nDamage = -weaponType.getDamage(nRange)
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
                report = new Report(3365);
                report.subject = subjectId;
                vPhaseReport.addElement(report);
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
                    gameManager.creditKill(entityTarget, attackingEntity);
                    hits -= nCluster;
                    firstHit = false;
                    // do IMP stuff here!
                    if ((entityTarget instanceof Mek)
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

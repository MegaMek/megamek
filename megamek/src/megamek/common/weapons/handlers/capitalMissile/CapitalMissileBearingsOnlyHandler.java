/*
 * Copyright (C) 2005 Ben Mazur (bmazur@sev.org)
 * Copyright (C) 2017-2025 The MegaMek Team. All Rights Reserved.
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

package megamek.common.weapons.handlers.capitalMissile;

import static java.lang.Math.floor;

import java.io.Serial;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Vector;

import megamek.common.RangeType;
import megamek.common.Report;
import megamek.common.ToHitData;
import megamek.common.actions.ArtilleryAttackAction;
import megamek.common.actions.WeaponAttackAction;
import megamek.common.board.Coords;
import megamek.common.compute.Compute;
import megamek.common.compute.ComputeArc;
import megamek.common.compute.ComputeECM;
import megamek.common.enums.GamePhase;
import megamek.common.equipment.AmmoMounted;
import megamek.common.equipment.AmmoType;
import megamek.common.equipment.WeaponMounted;
import megamek.common.equipment.WeaponType;
import megamek.common.game.Game;
import megamek.common.loaders.EntityLoadingException;
import megamek.common.net.packets.InvalidPacketDataException;
import megamek.common.options.OptionsConstants;
import megamek.common.rolls.TargetRoll;
import megamek.common.units.Aero;
import megamek.common.units.Entity;
import megamek.common.units.IAero;
import megamek.common.units.IBuilding;
import megamek.common.units.Targetable;
import megamek.common.weapons.Weapon;
import megamek.common.weapons.bayWeapons.TeleOperatedMissileBayWeapon;
import megamek.common.weapons.handlers.AmmoBayWeaponHandler;
import megamek.common.weapons.handlers.AttackHandler;
import megamek.common.weapons.handlers.WeaponHandler;
import megamek.logging.MMLogger;
import megamek.server.totalWarfare.TWGameManager;

/**
 * @author MKerensky
 * @since Sep 24, 2004
 */
public class CapitalMissileBearingsOnlyHandler extends AmmoBayWeaponHandler {
    private static final MMLogger LOGGER = MMLogger.create(CapitalMissileBearingsOnlyHandler.class);

    @Serial
    private static final long serialVersionUID = -1277549123532227298L;
    boolean handledAmmoAndReport = false;
    boolean detRangeShort = (weapon.curMode().equals(Weapon.MODE_CAP_MISSILE_BEARING_SHORT) ||
          weapon.curMode().equals(Weapon.MODE_CAP_MISSILE_WAYPOINT_BEARING_SHORT));
    boolean detRangeMedium = (weapon.curMode().equals(Weapon.MODE_CAP_MISSILE_BEARING_MED) ||
          weapon.curMode().equals(Weapon.MODE_CAP_MISSILE_WAYPOINT_BEARING_MED));
    boolean detRangeLong = (weapon.curMode().equals(Weapon.MODE_CAP_MISSILE_BEARING_LONG) ||
          weapon.curMode().equals(Weapon.MODE_CAP_MISSILE_WAYPOINT_BEARING_LONG));
    boolean detRangeExtreme = (weapon.curMode().equals(Weapon.MODE_CAP_MISSILE_BEARING_EXT) ||
          weapon.curMode().equals(Weapon.MODE_CAP_MISSILE_WAYPOINT_BEARING_EXT));

    // Defined here so we can use it in multiple methods
    AmmoMounted bayWAmmo;
    int range;
    Coords targetCoords;

    /**
     * This constructor can only be used for deserialization.
     */
    protected CapitalMissileBearingsOnlyHandler() {
        super();
    }

    public CapitalMissileBearingsOnlyHandler(ToHitData t, WeaponAttackAction w, Game g, TWGameManager m)
          throws EntityLoadingException {
        super(t, w, g, m);
    }

    @Override
    public boolean cares(GamePhase phase) {
        return phase.isFiring() || phase.isTargeting();
    }

    protected void getMountedAmmo() {
        for (WeaponMounted bayW : weapon.getBayWeapons()) {
            // check the currently loaded ammo
            bayWAmmo = bayW.getLinkedAmmo();

            if (bayWAmmo == null) {// Can't happen. w/o legal ammo, the weapon
                // *shouldn't* fire.
                LOGGER.debug("Handler can't find any ammo! Oh no!");
            }
        }
    }

    @Override
    protected void useAmmo() {
        getMountedAmmo();
        WeaponMounted bayW = (WeaponMounted) bayWAmmo.getLinkedBy();
        int shots = (bayW.getCurrentShots() * weapon.getBayWeapons().size());
        for (int i = 0; i < shots; i++) {
            if ((null == bayWAmmo) || bayWAmmo.getUsableShotsLeft() < 1) {
                // try loading something else
                attackingEntity.loadWeaponWithSameAmmo(bayW);
                bayWAmmo = bayW.getLinkedAmmo();
            }
            if (null != bayWAmmo) {
                bayWAmmo.setShotsLeft(bayWAmmo.getBaseShotsLeft() - 1);
            }
        }
    }

    @Override
    public boolean handle(GamePhase phase, Vector<Report> vPhaseReport) {
        if (!cares(phase)) {
            return true;
        }
        ArtilleryAttackAction aaa = (ArtilleryAttackAction) weaponAttackAction;
        if (phase.isTargeting()) {
            if (!handledAmmoAndReport) {
                addHeat();
                // Report the firing itself
                Report r = new Report(3122);
                r.indent();
                r.newlines = 0;
                r.subject = subjectId;
                r.add(weaponType.getName());
                r.add(aaa.getTurnsTilHit());
                vPhaseReport.addElement(r);
                Report.addNewline(vPhaseReport);
                handledAmmoAndReport = true;
            }
            // if this is the last targeting phase before we hit,
            // make it so the firing entity is announced in the
            // off-board attack phase that follows.
            if (aaa.getTurnsTilHit() == 0) {
                setAnnouncedEntityFiring(false);
            }
            return true;
        }
        if (aaa.getTurnsTilHit() > 0) {
            aaa.decrementTurnsTilHit();
            return true;
        }
        Entity entityTarget = (aaa.getTargetType() == Targetable.TYPE_ENTITY) ? (Entity) aaa.getTarget(game) : null;
        if (game.getPhase().isFiring() && entityTarget == null) {
            convertHexTargetToEntityTarget();
            entityTarget = (aaa.getTargetType() == Targetable.TYPE_ENTITY) ? (Entity) aaa.getTarget(game) : null;
        }

        // Report weapon attack and its to-hit value.
        Report r = new Report(3118);
        r.newlines = 0;
        r.subject = subjectId;
        vPhaseReport.addElement(r);

        if (toHit.getValue() == TargetRoll.IMPOSSIBLE) {
            r = new Report(3123);
            r.subject = subjectId;
            r.add(" " + target.getPosition(), true);
            vPhaseReport.addElement(r);
        } else {
            r = new Report(3119);
            r.indent();
            r.newlines = 1;
            r.subject = subjectId;
            r.addDesc(entityTarget);
            vPhaseReport.addElement(r);
        }

        // are we a glancing hit? Check for this here, report it later
        setGlancingBlowFlags(entityTarget);

        // Point Defense fire vs Capital Missiles

        // Set Margin of Success/Failure and check for Direct Blows
        toHit.setMoS(roll.getIntValue() - Math.max(2, toHit.getValue()));
        bDirect = game.getOptions().booleanOption(OptionsConstants.ADVANCED_COMBAT_TAC_OPS_DIRECT_BLOW) &&
              ((toHit.getMoS() / 3) >= 1) &&
              (entityTarget != null);

        // This has to be up here so that we don't screw up glancing/direct blow reports
        attackValue = calcAttackValue();
        nDamPerHit = attackValue;

        // CalcAttackValue triggers counterfire, so now we can safely get this
        CapMissileAMSMod = getCapMissileAMSMod();

        // Only do this if the missile wasn't destroyed
        if (CapMissileAMSMod > 0 && CapMissileArmor > 0) {
            toHit.addModifier(CapMissileAMSMod, "Damage from Point Defenses");
            if (roll.getIntValue() < toHit.getValue()) {
                CapMissileMissed = true;
            }
        }

        // Report any AMS bay action against Capital missiles that doesn't destroy them
        // all.
        if (amsBayEngagedCap && CapMissileArmor > 0) {
            r = new Report(3358);
            r.add(CapMissileAMSMod);
            r.subject = subjectId;
            vPhaseReport.addElement(r);

            // Report any PD bay action against Capital missiles that doesn't destroy them
            // all.
        } else if (pdBayEngagedCap && CapMissileArmor > 0) {
            r = new Report(3357);
            r.add(CapMissileAMSMod);
            r.subject = subjectId;
            vPhaseReport.addElement(r);
        }

        if (toHit.getValue() == TargetRoll.IMPOSSIBLE) {
            // This is reported elsewhere
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

        // Report Glancing/Direct Blow here because of Capital Missile weirdness
        // TODO : Can't figure out a good way to make Capital Missile bays report
        // direct/glancing
        // TODO : blows when Advanced Point Defense is on, but they work correctly.
        if (!(amsBayEngagedCap || pdBayEngagedCap)) {
            addGlancingBlowReports(vPhaseReport);

            if (bDirect) {
                r = new Report(3189);
                r.subject = attackingEntity.getId();
                r.newlines = 0;
                vPhaseReport.addElement(r);
            }
        }

        // we may still have to use ammo, if direct fire
        if (!handledAmmoAndReport) {
            addHeat();
        }

        CounterAV = getCounterAV();
        // use this if AMS counterfire destroys all the Capital missiles
        if (amsBayEngagedCap && (CapMissileArmor <= 0)) {
            r = new Report(3356);
            r.indent();
            r.subject = subjectId;
            vPhaseReport.addElement(r);
            return false;
        }
        // use this if PD counterfire destroys all the Capital missiles
        if (pdBayEngagedCap && (CapMissileArmor <= 0)) {
            r = new Report(3355);
            r.indent();
            r.subject = subjectId;
            vPhaseReport.addElement(r);
            return false;
        }

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
        }
        // Aero Sanity Handling
        if (game.getOptions().booleanOption(OptionsConstants.ADVANCED_AERO_RULES_AERO_SANITY) && !bMissed) {
            // New toHit data to hold our bay auto hit. We want to be able to get glancing/direct blow data from the
            // 'real' toHit data of this bay handler
            ToHitData autoHit = new ToHitData();
            autoHit.addModifier(TargetRoll.AUTOMATIC_SUCCESS, "if the bay hits, all bay weapons hit");
            int replaceReport;
            for (WeaponMounted m : weapon.getBayWeapons()) {
                if (!m.isBreached() && !m.isDestroyed() && !m.isJammed()) {
                    WeaponType bayWType = m.getType();
                    if (bayWType instanceof Weapon) {
                        replaceReport = vPhaseReport.size();
                        WeaponAttackAction bayWaa = new WeaponAttackAction(weaponAttackAction.getEntityId(),
                              weaponAttackAction.getTargetType(),
                              weaponAttackAction.getTargetId(),
                              bayWAmmo.getEquipmentNum());
                        AttackHandler bayWHandler = ((Weapon) bayWType).getCorrectHandler(autoHit,
                              bayWaa,
                              game,
                              gameManager);
                        bayWHandler.setAnnouncedEntityFiring(false);
                        // This should always be true. Maybe there's a better way to write this?
                        if (bayWHandler instanceof WeaponHandler wHandler) {
                            wHandler.setParentBayHandler(this);
                        }
                        bayWHandler.handle(phase, vPhaseReport);
                        if (vPhaseReport.size() > replaceReport) {
                            // fix the reporting - is there a better way to do this
                            Report currentReport = vPhaseReport.get(replaceReport);
                            while (null != currentReport) {
                                vPhaseReport.remove(replaceReport);
                                if ((currentReport.newlines > 0) || (vPhaseReport.size() <= replaceReport)) {
                                    currentReport = null;
                                } else {
                                    currentReport = vPhaseReport.get(replaceReport);
                                }
                            }
                            r = new Report(3115);
                            r.indent(2);
                            r.newlines = 1;
                            r.subject = subjectId;
                            r.add(bayWType.getName());
                            if (entityTarget != null) {
                                r.addDesc(entityTarget);
                            } else {
                                r.messageId = 3120;
                                r.add(target.getDisplayName(), true);
                            }
                            vPhaseReport.add(replaceReport, r);
                        }
                    }
                }
            } // Handle the next weapon in the bay
            Report.addNewline(vPhaseReport);
            return false;
        }

        // Handle damage.
        int nCluster = calculateNumCluster();
        int id = vPhaseReport.size();
        int hits = calcHits(vPhaseReport);
        // Set the hit location table based on where the missile goes active
        if (entityTarget != null) {
            if (aaa.getOldTargetCoords() != null) {
                toHit.setSideTable(entityTarget.sideTable(aaa.getOldTargetCoords()));
            }
        }
        if (target.isAirborne() || game.getBoard().isSpace() || attackingEntity.usesWeaponBays()) {
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

        // Bearings-only missiles shouldn't be able to target buildings, being
        // space-only weapons
        // but if these two things aren't defined, handleEntityDamage() doesn't work.
        IBuilding bldg = game.getBoard().getBuildingAt(target.getPosition());
        int bldgAbsorbs = 0;

        // We have to adjust the reports on a miss, so they line up
        if (bMissed && id != vPhaseReport.size()) {
            vPhaseReport.get(id - 1).newlines--;
            vPhaseReport.get(id).indent(2);
            vPhaseReport.get(vPhaseReport.size() - 1).newlines++;
        }

        // Make sure the player knows when his attack causes no damage.
        if (nDamPerHit == 0) {
            r = new Report(3365);
            r.subject = subjectId;
            vPhaseReport.addElement(r);
            return false;
        }
        if (!bMissed && (entityTarget != null)) {
            handleEntityDamage(entityTarget, vPhaseReport, bldg, hits, nCluster, bldgAbsorbs);
            gameManager.creditKill(entityTarget, attackingEntity);
        } else if (!bMissed) { // Hex is targeted, need to report a hit
            r = new Report(3390);
            r.subject = subjectId;
            vPhaseReport.addElement(r);
        }
        Report.addNewline(vPhaseReport);
        return false;
    }

    /**
     * Find the available targets within sensor range. Bearings-only missiles scan within the nose arc and target the
     * closest large craft within the preset range band. If none are found, it targets the closest small craft.
     */
    public void convertHexTargetToEntityTarget() {
        ArtilleryAttackAction aaa = (ArtilleryAttackAction) weaponAttackAction;

        final Coords tc = target.getPosition();
        targetCoords = tc;
        // Set the original missile target data. AMS and to-hit table calculations need
        // this.
        aaa.setOldTargetCoords(tc);
        aaa.setOriginalTargetId(target.getId());
        aaa.setOriginalTargetType(target.getTargetType());
        int missileFacing = attackingEntity.getPosition().direction(tc);
        Targetable newTarget = null;
        Vector<Aero> targets = new Vector<>();

        // get all entities on the opposing side
        for (Iterator<Entity> enemies = game.getAllEnemyEntities(attackingEntity); enemies.hasNext(); ) {
            Entity e = enemies.next();
            // Narrow the list to small craft and larger
            if (((e.getEntityType() & (Entity.ETYPE_SMALL_CRAFT)) != 0)) {
                Aero a = (Aero) e;
                targets.add(a);
            } else if (((e.getEntityType() & (Entity.ETYPE_JUMPSHIP)) != 0)) {
                Aero a = (Aero) e;
                targets.add(a);
            }
        }

        if (targets.isEmpty()) {
            // We're not dealing with targets in arc or in range yet.
            toHit = new ToHitData(TargetRoll.IMPOSSIBLE, "no valid targets in play");
            return;
        }

        // Add only targets in arc
        Vector<Aero> inArc = new Vector<>();
        for (Aero a : targets) {
            boolean isInArc = ComputeArc.isInArc(tc, missileFacing, a, Compute.ARC_NOSE);
            if (isInArc) {
                inArc.add(a);
            }
        }

        if (inArc.isEmpty()) {
            toHit = new ToHitData(TargetRoll.IMPOSSIBLE, "no targets detected within the missile's nose arc");
            return;
        }

        // Empty out the targets vector and only put valid targets in arc back in
        targets.removeAllElements();
        targets.addAll(inArc);

        // Detection range for targets is based on the range set at firing
        Vector<Aero> detected = new Vector<>();
        if (detRangeExtreme) {
            for (Aero a : targets) {
                if (tc.distance(a.getPosition()) <= 25) {
                    detected.add(a);
                }
            }
        } else if (detRangeLong) {
            for (Aero a : targets) {
                if (tc.distance(a.getPosition()) <= 20) {
                    detected.add(a);
                }
            }
        } else if (detRangeMedium) {
            for (Aero a : targets) {
                if (tc.distance(a.getPosition()) <= 12) {
                    detected.add(a);
                }
            }
        } else if (detRangeShort) {
            for (Aero a : targets) {
                if (tc.distance(a.getPosition()) <= 6) {
                    detected.add(a);
                }
            }
        }

        if (detected.isEmpty()) {
            toHit = new ToHitData(TargetRoll.IMPOSSIBLE, "no targets detected within the missile's detection range");
            return;
        }

        // Empty out the targets vector and only put valid targets in range back in
        targets.removeAllElements();
        targets.addAll(detected);

        // If we're using teleoperated missiles, the player gets to select the target
        if (weapon.getType() instanceof TeleOperatedMissileBayWeapon) {
            List<Integer> targetIds = new ArrayList<>();
            List<Integer> toHitValues = new ArrayList<>();
            for (Aero target : targets) {
                setToHit(target);
                targetIds.add(target.getId());
                toHitValues.add(toHit.getValue());
            }
            int choice = 0;
            try {
                choice = gameManager.processTeleguidedMissileCFR(attackingEntity.getOwnerId(), targetIds, toHitValues);
            } catch (InvalidPacketDataException e) {
                LOGGER.error("Invalid packet data:", e);
            }
            newTarget = targets.get(choice);
            target = newTarget;
            aaa.setTargetId(target.getId());
            aaa.setTargetType(target.getTargetType());
            // Run this again, otherwise toHit is left set to the value for the last target n the list...
            setToHit(target);
            gameManager.assignAMS();
        } else {
            // Otherwise, find the largest and closest target of those available
            int bestDistance = Integer.MAX_VALUE;
            int bestTonnage = 0;
            Aero currTarget = targets.firstElement();
            // Target the closest large craft
            for (Aero a : targets) {
                // Ignore small craft for now
                if (((a.getEntityType() & Entity.ETYPE_SMALL_CRAFT) > 0) &&
                      ((a.getEntityType() & Entity.ETYPE_DROPSHIP) == 0)) {
                    continue;
                }
                int distance = tc.distance(a.getPosition());
                if (distance < bestDistance) {
                    bestDistance = distance;
                    currTarget = a;
                    newTarget = currTarget;
                    continue;
                }
                // same distance
                int tonnage = (int) a.getWeight();
                if (distance == bestDistance) {
                    // Find the largest target
                    if (tonnage > bestTonnage) {
                        bestTonnage = tonnage;
                        currTarget = a;
                        newTarget = currTarget;
                        continue;
                    }
                }
                // same distance and tonnage? Roll randomly
                if (distance == bestDistance && tonnage == bestTonnage) {
                    int tiebreaker = Compute.d6();
                    if (tiebreaker < 4) {
                        newTarget = a;
                    } else {
                        newTarget = currTarget;
                    }
                }
            }

            // Repeat the process for small craft if no large craft are found
            if (newTarget == null) {
                for (Aero a : targets) {
                    int distance = tc.distance(a.getPosition());
                    if (distance < bestDistance) {
                        bestDistance = distance;
                        currTarget = a;
                        newTarget = currTarget;
                        continue;
                    }
                    // same distance
                    int tonnage = (int) a.getWeight();
                    if (distance == bestDistance) {
                        // Find the largest target
                        if (tonnage > bestTonnage) {
                            bestTonnage = tonnage;
                            currTarget = a;
                            newTarget = currTarget;
                            continue;
                        }
                    }
                    // same distance and tonnage? Roll randomly
                    if (distance == bestDistance && tonnage == bestTonnage) {
                        int tiebreaker = Compute.d6();
                        if (tiebreaker < 4) {
                            newTarget = a;
                        } else {
                            newTarget = currTarget;
                        }
                    }
                }
            }

            if (newTarget != null) {
                aaa.setTargetId(newTarget.getId());
                aaa.setTargetType(newTarget.getTargetType());
                setToHit(newTarget);
                gameManager.assignAMS();
            }
        }
    }

    private void setToHit(Targetable target) {
        if (!(target instanceof Aero targetShip)) {
            return;
        }

        toHit = new ToHitData(4, "Base");
        if (range > 20 && range <= 25) {
            toHit.addModifier(6, "extreme range");
        } else if (range > 12 && range <= 20) {
            toHit.addModifier(4, "long range");
        } else if (range > 6 && range <= 12) {
            toHit.addModifier(2, "medium range");
        } else if (range <= 6) {
            toHit.addModifier(0, "short range");
        }
        // If the target is closer than the set range band, add a +1 modifier
        if ((detRangeExtreme && range <= 20) || (detRangeLong && range <= 12) || (detRangeMedium && range <= 6)) {
            toHit.addModifier(1, "target closer than range setting");
        }

        // evading bonuses
        if (targetShip.isEvading()) {
            toHit.addModifier(2, "target is evading");
        }

        // is the target at zero velocity
        if ((targetShip.getCurrentVelocity() == 0) && !(targetShip.isSpheroid() && !game.getBoard().isSpace())) {
            toHit.addModifier(-2, "target is not moving");
        }

        // Barracuda Missile Modifier
        getMountedAmmo();
        AmmoType bayAType = bayWAmmo.getType();
        if ((bayWAmmo.getType().hasFlag(AmmoType.F_AR10_BARRACUDA)) ||
              (bayAType.getAmmoType() == AmmoType.AmmoTypeEnum.BARRACUDA)) {
            toHit.addModifier(-2, "Barracuda Missile");
        }

        if (target.isAirborne() && target.isAero()) {
            if (!(((IAero) target).isSpheroid() && !game.getBoard().isSpace())) {
                // get mods for direction of attack
                int side = toHit.getSideTable();
                // if this is an aero attack using advanced movement rules then determine side differently
                if (game.useVectorMove()) {
                    boolean usePrior = false;
                    side = ((Entity) target).chooseSide(targetCoords, usePrior);
                }
                if (side == ToHitData.SIDE_FRONT) {
                    toHit.addModifier(+1, "attack against nose");
                }
                if ((side == ToHitData.SIDE_LEFT) || (side == ToHitData.SIDE_RIGHT)) {
                    toHit.addModifier(+2, "attack against side");
                }
            }
        }

        // Space ECM
        if (game.getBoard().isSpace() && game.getOptions()
              .booleanOption(OptionsConstants.ADVANCED_AERO_RULES_STRATOPS_ECM)) {
            int ecm = ComputeECM.getLargeCraftECM(attackingEntity, targetCoords, target.getPosition());
            ecm = Math.min(4, ecm);
            if (ecm > 0) {
                toHit.addModifier(ecm, "ECM");
            }
        }
    }

    @Override
    protected boolean handleSpecialMiss(Entity entityTarget, boolean bldgDamagedOnMiss, IBuilding bldg,
          Vector<Report> vPhaseReport) {
        return true;
    }

    /**
     * Checks to see if this point defense/AMS bay can engage a capital missile This should return true. Only when
     * handling capital missile attacks can this be false.
     */
    @Override
    protected boolean canEngageCapitalMissile(WeaponMounted counter) {
        return counter.getBayWeapons().size() >= 2;
    }

    /**
     * Sets the appropriate AMS Bay reporting flag depending on what type of missile this is
     */
    @Override
    protected void setAMSBayReportingFlag() {
        amsBayEngagedCap = true;
    }

    /**
     * Sets the appropriate PD Bay reporting flag depending on what type of missile this is
     */
    @Override
    protected void setPDBayReportingFlag() {
        pdBayEngagedCap = true;
    }

    @Override
    protected int calcAttackValue() {
        double attackValue = 0;
        double counterAV = calcCounterAV();
        int armor = 0;
        int weaponArmor;
        // A bearings-only shot is, by definition, always going to be at extreme range...
        int range = RangeType.RANGE_EXTREME;

        for (WeaponMounted bayWeaponMounted : weapon.getBayWeapons()) {
            // check the currently loaded ammo
            AmmoMounted bayWeaponAmmoMounted = bayWeaponMounted.getLinkedAmmo();

            if (null == bayWeaponAmmoMounted || bayWeaponAmmoMounted.getUsableShotsLeft() < 1) {
                // try loading something else
                attackingEntity.loadWeaponWithSameAmmo(bayWeaponMounted);
                bayWeaponAmmoMounted = bayWeaponMounted.getLinkedAmmo();
            }

            if (!bayWeaponMounted.isBreached() &&
                  !bayWeaponMounted.isDestroyed() &&
                  !bayWeaponMounted.isJammed() &&
                  bayWeaponAmmoMounted != null &&
                  attackingEntity.getTotalAmmoOfType(bayWeaponAmmoMounted.getType())
                        >= bayWeaponMounted.getCurrentShots()) {
                WeaponType bayWeaponMountedType = bayWeaponMounted.getType();
                // need to cycle through weapons and add attackValue
                double currentAttackValue = 0;

                AmmoType ammoType = bayWeaponAmmoMounted.getType();
                if (bayWeaponMountedType.getAtClass() == (WeaponType.CLASS_AR10) &&
                      (ammoType.hasFlag(AmmoType.F_AR10_KILLER_WHALE) || ammoType.hasFlag(AmmoType.F_PEACEMAKER))) {
                    weaponArmor = 40;
                } else if (bayWeaponMountedType.getAtClass() == (WeaponType.CLASS_AR10) &&
                      (ammoType.hasFlag(AmmoType.F_AR10_WHITE_SHARK) ||
                            ammoType.hasFlag(AmmoType.F_SANTA_ANNA))) {
                    weaponArmor = 30;
                } else if (bayWeaponMountedType.getAtClass() == (WeaponType.CLASS_AR10) &&
                      ammoType.hasFlag(AmmoType.F_AR10_BARRACUDA)) {
                    weaponArmor = 20;
                } else {
                    weaponArmor = bayWeaponMountedType.getMissileArmor();
                }

                if (ammoType.hasFlag(AmmoType.F_NUCLEAR)) {
                    nukeS2S = true;
                }

                currentAttackValue = updateAVForAmmo(currentAttackValue,
                      ammoType,
                      bayWeaponMountedType,
                      range,
                      bayWeaponMounted.getEquipmentNum());
                attackValue = attackValue + currentAttackValue;
                armor = armor + weaponArmor;
            }
        }

        CapMissileArmor = armor - (int) counterAV;
        CapMissileAMSMod = calcCapMissileAMSMod();

        if (bDirect) {
            attackValue = Math.min(attackValue + (int) floor(toHit.getMoS() / 3.0), attackValue * 2);
        }

        attackValue = applyGlancingBlowModifier(attackValue, false);
        attackValue = (int) Math.floor(getBracketingMultiplier() * attackValue);
        return (int) Math.ceil(attackValue);
    }

    /**
     * Calculate the damage per hit.
     *
     * @return an <code>int</code> representing the damage dealt per hit.
     */
    @Override
    protected int calcDamagePerHit() {
        AmmoType ammoType = ammo.getType();
        double toReturn = weaponType.getDamage(nRange);

        // AR10 munitions
        if (ammoType != null) {
            if (ammoType.getAmmoType() == AmmoType.AmmoTypeEnum.AR10) {
                if (ammoType.hasFlag(AmmoType.F_AR10_KILLER_WHALE)) {
                    toReturn = 4;
                } else if (ammoType.hasFlag(AmmoType.F_AR10_WHITE_SHARK)) {
                    toReturn = 3;
                } else if (ammoType.hasFlag(AmmoType.F_PEACEMAKER)) {
                    toReturn = 1000;
                } else if (ammoType.hasFlag(AmmoType.F_SANTA_ANNA)) {
                    toReturn = 100;
                } else {
                    toReturn = 2;
                }
            }
            // Nuclear Warheads for non-AR10 missiles
            if (ammoType.hasFlag(AmmoType.F_SANTA_ANNA)) {
                toReturn = 100;
            } else if (ammoType.hasFlag(AmmoType.F_PEACEMAKER)) {
                toReturn = 1000;
            }
            nukeS2S = ammoType.hasFlag(AmmoType.F_NUCLEAR);
        }

        // we default to direct fire weapons for anti-infantry damage
        if (bDirect) {
            toReturn = Math.min(toReturn + (int) floor(toHit.getMoS() / 3.0), toReturn * 2);
        }

        toReturn = applyGlancingBlowModifier(toReturn, false);

        return (int) toReturn;
    }

    @Override
    protected int calcCapMissileAMSMod() {
        CapMissileAMSMod = (int) Math.ceil(CounterAV / 10.0);
        return CapMissileAMSMod;
    }

    /**
     * Calculate the starting armor value of a flight of Capital Missiles Used for Aero Sanity. This is done in
     * calcAttackValue() otherwise
     */
    @Override
    protected int initializeCapMissileArmor() {
        int armor = 0;
        for (WeaponMounted bayWeaponMounted : weapon.getBayWeapons()) {
            int currentArmor;
            // check the currently loaded ammo
            AmmoMounted bayWAmmo = bayWeaponMounted.getLinkedAmmo();
            AmmoType ammoType = bayWAmmo.getType();
            WeaponType bayWeaponType = bayWeaponMounted.getType();

            if (bayWeaponType.getAtClass() == (WeaponType.CLASS_AR10) &&
                  (ammoType.hasFlag(AmmoType.F_AR10_KILLER_WHALE) || ammoType.hasFlag(AmmoType.F_PEACEMAKER))) {
                currentArmor = 40;
            } else if (bayWeaponType.getAtClass() == (WeaponType.CLASS_AR10) &&
                  (ammoType.hasFlag(AmmoType.F_AR10_WHITE_SHARK) ||
                        ammoType.hasFlag(AmmoType.F_SANTA_ANNA))) {
                currentArmor = 30;
            } else if (bayWeaponType.getAtClass() == (WeaponType.CLASS_AR10) &&
                  ammoType.hasFlag(AmmoType.F_AR10_BARRACUDA)) {
                currentArmor = 20;
            } else {
                currentArmor = bayWeaponType.getMissileArmor();
            }

            armor = armor + currentArmor;
        }
        return armor;
    }

    @Override
    protected int getCapMisMod() {
        int mod = 0;

        for (WeaponMounted bayWeaponMounted : weapon.getBayWeapons()) {
            int currentMod;
            // check the currently loaded ammo
            AmmoMounted bayWAmmo = bayWeaponMounted.getLinkedAmmo();
            currentMod = getCritMod(bayWAmmo.getType());
            if (currentMod > mod) {
                mod = currentMod;
            }
        }

        return mod;
    }

    /*
     * get the cap mis mod given a single ammo type
     */
    protected int getCritMod(AmmoType ammoType) {
        if (ammoType == null || ammoType.getAmmoType() == AmmoType.AmmoTypeEnum.PIRANHA) {
            return 0;
        }

        if (ammoType.getAmmoType() == AmmoType.AmmoTypeEnum.WHITE_SHARK || ammoType.hasFlag(AmmoType.F_AR10_WHITE_SHARK)
              // Santa Anna, per IO rules
              || ammoType.hasFlag(AmmoType.F_SANTA_ANNA)) {
            return 9;
        } else if (ammoType.getAmmoType() == AmmoType.AmmoTypeEnum.KRAKEN_T
              || ammoType.getAmmoType() == AmmoType.AmmoTypeEnum.KRAKENM
              // Peacemaker, per IO rules
              || ammoType.hasFlag(AmmoType.F_PEACEMAKER)) {
            return 8;
        } else if (ammoType.getAmmoType() == AmmoType.AmmoTypeEnum.KILLER_WHALE ||
              ammoType.hasFlag(AmmoType.F_AR10_KILLER_WHALE) ||
              ammoType.getAmmoType() == AmmoType.AmmoTypeEnum.MANTA_RAY ||
              ammoType.getAmmoType() == AmmoType.AmmoTypeEnum.ALAMO) {
            return 10;
        } else if (ammoType.getAmmoType() == AmmoType.AmmoTypeEnum.STINGRAY) {
            return 12;
        }

        return 11;
    }

    @Override
    protected double updateAVForAmmo(double currentAttackValue, AmmoType ammoType, WeaponType bayWeaponType, int range,
          int wId) {
        // AR10 munitions
        if (ammoType.getAmmoType() == AmmoType.AmmoTypeEnum.AR10) {
            if (ammoType.hasFlag(AmmoType.F_AR10_KILLER_WHALE)) {
                currentAttackValue = 4;
            } else if (ammoType.hasFlag(AmmoType.F_AR10_WHITE_SHARK)) {
                currentAttackValue = 3;
            } else if (ammoType.hasFlag(AmmoType.F_PEACEMAKER)) {
                currentAttackValue = 1000;
            } else if (ammoType.hasFlag(AmmoType.F_SANTA_ANNA)) {
                currentAttackValue = 100;
            } else {
                currentAttackValue = 2;
            }
        }

        // Nuclear Warheads for non-AR10 missiles
        if (ammoType.hasFlag(AmmoType.F_SANTA_ANNA)) {
            currentAttackValue = 100;
        } else if (ammoType.hasFlag(AmmoType.F_PEACEMAKER)) {
            currentAttackValue = 1000;
        }

        return currentAttackValue;
    }

}

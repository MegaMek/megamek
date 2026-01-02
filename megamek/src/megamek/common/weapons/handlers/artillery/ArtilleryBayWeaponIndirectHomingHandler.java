/*
 * Copyright (c) 2005 - Ben Mazur (bmazur@sev.org)
 * Copyright (C) 2012-2025 The MegaMek Team. All Rights Reserved.
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

package megamek.common.weapons.handlers.artillery;

import java.io.Serial;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Vector;

import megamek.common.HitData;
import megamek.common.Report;
import megamek.common.TagInfo;
import megamek.common.ToHitData;
import megamek.common.actions.ArtilleryAttackAction;
import megamek.common.actions.WeaponAttackAction;
import megamek.common.battleArmor.BattleArmor;
import megamek.common.board.Coords;
import megamek.common.compute.Compute;
import megamek.common.enums.GamePhase;
import megamek.common.equipment.AmmoMounted;
import megamek.common.equipment.AmmoType;
import megamek.common.equipment.AmmoType.AmmoTypeEnum;
import megamek.common.equipment.WeaponType;
import megamek.common.game.Game;
import megamek.common.loaders.EntityLoadingException;
import megamek.common.net.packets.InvalidPacketDataException;
import megamek.common.options.OptionsConstants;
import megamek.common.rolls.Roll;
import megamek.common.rolls.TargetRoll;
import megamek.common.units.Entity;
import megamek.common.units.IBuilding;
import megamek.common.units.Infantry;
import megamek.common.units.Targetable;
import megamek.common.weapons.DamageType;
import megamek.logging.MMLogger;
import megamek.server.totalWarfare.TWGameManager;

public class ArtilleryBayWeaponIndirectHomingHandler extends ArtilleryBayWeaponIndirectFireHandler {
    private static final MMLogger LOGGER = MMLogger.create(ArtilleryBayWeaponIndirectHomingHandler.class);
    @Serial
    private static final long serialVersionUID = -7243477723032010917L;
    boolean advancedPD;
    boolean advancedAMS;
    boolean multiAMS;

    /**
     *
     */
    public ArtilleryBayWeaponIndirectHomingHandler(ToHitData t, WeaponAttackAction w, Game g, TWGameManager m)
          throws EntityLoadingException {
        super(t, w, g, m);
        advancedPD = g.getOptions().booleanOption(OptionsConstants.ADVANCED_AERO_RULES_STRATOPS_ADV_POINT_DEFENSE);
        advancedAMS = game.getOptions().booleanOption(OptionsConstants.ADVANCED_COMBAT_TAC_OPS_AMS);
        multiAMS = g.getOptions().booleanOption(OptionsConstants.ADVANCED_COMBAT_MULTI_USE_AMS);
    }

    /*
     * (non-Javadoc)
     *
     * @see megamek.common.weapons.handlers.AttackHandler#handle(int, java.util.Vector)
     */
    @Override
    public boolean handle(GamePhase phase, Vector<Report> vPhaseReport) {
        if (!cares(phase)) {
            return true;
        }

        ArtilleryAttackAction artilleryAttackAction = (ArtilleryAttackAction) weaponAttackAction;
        if (phase.isTargeting()) {
            if (!handledAmmoAndReport) {
                addHeat();
                // Report the firing itself
                Report report = new Report(3121);
                report.indent();
                report.newlines = 0;
                report.subject = subjectId;
                report.add(weaponType.getName() + " (" + ammoType.getShortName() + ")");
                report.add(artilleryAttackAction.getTurnsTilHit());
                vPhaseReport.addElement(report);
                Report.addNewline(vPhaseReport);
                handledAmmoAndReport = true;
            }

            // if this is the last targeting phase before we hit, make it so the firing entity is announced in the
            // off-board attack phase that follows.
            if (artilleryAttackAction.getTurnsTilHit() == 0) {
                setAnnouncedEntityFiring(false);
            }
            return true;
        }
        if (artilleryAttackAction.getTurnsTilHit() > 0) {
            artilleryAttackAction.decrementTurnsTilHit();
            return true;
        }

        Entity entityTarget;
        try {
            convertHomingShotToEntityTarget();
            entityTarget = (artilleryAttackAction.getTargetType() == Targetable.TYPE_ENTITY) ?
                  (Entity) artilleryAttackAction.getTarget(game) :
                  null;
        } catch (InvalidPacketDataException e) {
            LOGGER.error("Invalid packet data:", e);
            return true;
        }

        final boolean targetInBuilding = Compute.isInBuilding(game, entityTarget);
        final boolean bldgDamagedOnMiss = targetInBuilding
              && !(target instanceof Infantry)
              && attackingEntity.getPosition().distance(target.getPosition()) <= 1;

        // Which building takes the damage?
        IBuilding building = game.getBoard().getBuildingAt(target.getPosition());

        // Determine what ammo we're firing for reporting and (later) damage
        AmmoMounted ammoUsed = attackingEntity.getAmmo(artilleryAttackAction.getAmmoId());
        final AmmoType ammoType = ammoUsed.getType();
        // Report weapon attack and its to-hit value.
        Report report = new Report(3124);
        report.indent();
        report.newlines = 0;
        report.subject = subjectId;
        report.add(weaponType.getName());
        report.add(numWeaponsHit);
        report.add(ammoType.getShortName());

        if (entityTarget != null) {
            report.addDesc(entityTarget);
        } else {
            report.messageId = 3126;
            report.add(target.getDisplayName(), true);
        }

        vPhaseReport.addElement(report);

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

        // we may still have to use ammo, if direct fire
        if (!handledAmmoAndReport) {
            addHeat();
        }

        // Any necessary PSRs, jam checks, etc. If this boolean is true, don't report the miss later, as we already
        // reported it in doChecks
        boolean missReported = doChecks(vPhaseReport);
        if (missReported) {
            bMissed = true;
        }

        // Set up the damage
        nDamPerHit = ammoType.getRackSize();

        // copperhead gets 10 damage less than standard
        if (ammoType.getAmmoType() != AmmoTypeEnum.ARROW_IV) {
            nDamPerHit -= 10;
        }

        nDamPerHit = applyGlancingBlowModifier(nDamPerHit, false);

        // Do we need some sort of special resolution (minefields, artillery,
        if (specialResolution(vPhaseReport, entityTarget)) {
            return false;
        }

        // this has to be called here, or it triggers before the TAG shot, and we have no entityTarget
        // mounting AMS
        if (ammoType.getAmmoType() == AmmoTypeEnum.ARROW_IV) {
            gameManager.assignAMS();
        }
        while (numWeaponsHit > 0) {
            int hits = 1;
            int nCluster = 1;
            if ((entityTarget != null) && (entityTarget.getTaggedBy() != -1)) {
                // Do point defenses shoot down this homing missile? (Copperheads don't count)
                hits = handleAMS(vPhaseReport, ammoUsed);

                if (bMissed && !missReported) {
                    reportMiss(vPhaseReport);

                    // Works out fire setting and whether continuation is
                    // necessary.
                    if (!handleSpecialMiss(entityTarget, bldgDamagedOnMiss, building,
                          vPhaseReport)) {
                        return false;
                    }
                }

                if (artilleryAttackAction.getCoords() != null && hits > 0) {
                    toHit.setSideTable(entityTarget.sideTable(artilleryAttackAction.getCoords()));
                }
            }

            // The building shields all units from a certain amount of damage.
            // The amount is based upon the building's CF at the phase's start.
            int bldgAbsorbs = 0;
            if (targetInBuilding && (building != null)) {
                bldgAbsorbs = building.getAbsorption(target.getPosition());
            }
            if ((building != null) && (bldgAbsorbs > 0)) {
                // building absorbs some damage
                report = new Report(6010);
                if (entityTarget != null) {
                    report.subject = entityTarget.getId();
                }
                report.add(bldgAbsorbs);
                vPhaseReport.addElement(report);
                Vector<Report> buildingReports = gameManager.damageBuilding(building, nDamPerHit, target.getPosition());
                if (entityTarget != null) {
                    for (Report buildingReport : buildingReports) {
                        buildingReport.subject = entityTarget.getId();
                    }
                }
                vPhaseReport.addAll(buildingReports);
            }
            nDamPerHit -= bldgAbsorbs;

            // Make sure the player knows when his attack causes no damage.
            if (nDamPerHit == 0) {
                report = new Report(3365);
                report.subject = subjectId;
                vPhaseReport.addElement(report);
                return false;
            }
            if (!bMissed && (entityTarget != null)) {
                handleEntityDamage(entityTarget, vPhaseReport, building, hits,
                      nCluster, bldgAbsorbs);
                gameManager.creditKill(entityTarget, attackingEntity);
            } else if (!bMissed && // The attack is targeting a specific building
                  (target.getTargetType() == Targetable.TYPE_BLDG_TAG)) {
                report = new Report(3390);
                report.subject = subjectId;
                vPhaseReport.addElement(report);
                vPhaseReport.addAll(gameManager.damageBuilding(building,
                      nDamPerHit, target.getPosition()));
            } else if (!bMissed) { // Hex is targeted, need to report a hit
                report = new Report(3390);
                report.subject = subjectId;
                vPhaseReport.addElement(report);
            }

            // Use original target coordinates for splash damage, not current entity position.
            // If target was converted from hex to entity, use saved coords; otherwise use current position.
            Coords coords = (artilleryAttackAction.getOldTargetCoords() != null)
                  ? artilleryAttackAction.getOldTargetCoords()
                  : target.getPosition();
            int ratedDamage = 5; // splash damage is 5 from all launchers

            // If AMS shoots down a missile, it shouldn't deal any splash damage
            if (hits == 0) {
                ratedDamage = 0;
            }

            building = game.getBoard().getBuildingAt(coords);
            bldgAbsorbs = (building != null) ? building.getAbsorption(coords) : 0;
            bldgAbsorbs = Math.min(bldgAbsorbs, ratedDamage);
            // assumption: homing artillery splash damage is area effect.
            // do damage to woods, 2 * normal damage (TW page 112)
            handleClearDamage(vPhaseReport, building, ratedDamage * 2, false);
            ratedDamage -= bldgAbsorbs;
            if (ratedDamage > 0) {
                for (Entity entity : game.getEntitiesVector(coords)) {
                    if (!bMissed) {
                        if (entity == entityTarget) {
                            continue; // don't splash the target unless missile
                            // missed
                        }
                    }
                    toHit.setSideTable(entity.sideTable(artilleryAttackAction.getCoords()));
                    HitData hit = entity.rollHitLocation(toHit.getHitTable(),
                          toHit.getSideTable(), weaponAttackAction.getAimedLocation(),
                          weaponAttackAction.getAimingMode(), toHit.getCover());
                    hit.setAttackerId(getAttackerId());
                    // BA gets damage to all troopers
                    if (entity instanceof BattleArmor ba) {
                        for (int loc = 1; loc <= ba.getTroopers(); loc++) {
                            hit.setLocation(loc);
                            vPhaseReport.addAll(gameManager.damageEntity(entity, hit,
                                  ratedDamage, false, DamageType.NONE, false,
                                  true, throughFront, underWater));
                        }
                    } else {
                        vPhaseReport.addAll(gameManager.damageEntity(entity, hit,
                              ratedDamage, false, DamageType.NONE, false, true,
                              throughFront, underWater));
                    }
                    gameManager.creditKill(entity, attackingEntity);
                }
            }
            Report.addNewline(vPhaseReport);
            numWeaponsHit--;
        }
        return false;
    }

    /**
     * Find the tagged entity for this attack Uses a CFR to let the player choose from eligible TAGs
     */
    public void convertHomingShotToEntityTarget() throws InvalidPacketDataException {
        ArtilleryAttackAction aaa = (ArtilleryAttackAction) weaponAttackAction;

        final Coords tc = target.getPosition();
        // Save original target coordinates before converting to entity target.
        // This ensures splash damage is applied at the original targeted hex,
        // not wherever the entity moved to. (Fix for issue #7274)
        aaa.setOldTargetCoords(tc);
        Targetable newTarget = null;

        Vector<TagInfo> v = game.getTagInfo();
        Vector<TagInfo> allowed = new Vector<>();
        // get only TagInfo on the same side
        for (TagInfo ti : v) {
            switch (ti.targetType) {
                case Targetable.TYPE_BLDG_TAG:
                case Targetable.TYPE_HEX_TAG:
                    allowed.add(ti);
                    break;
                case Targetable.TYPE_ENTITY:
                    if (attackingEntity.isEnemyOf((Entity) ti.target)
                          || game.getOptions().booleanOption(OptionsConstants.BASE_FRIENDLY_FIRE)) {
                        allowed.add(ti);
                    }
                    break;
            }
        }
        if (allowed.isEmpty()) {
            toHit = new ToHitData(TargetRoll.IMPOSSIBLE, "no targets tagged this turn");
            return;
        }

        // get TAGs that hit
        v = new Vector<>();
        for (TagInfo ti : allowed) {
            newTarget = ti.target;
            if (!ti.missed && (newTarget != null)) {
                v.add(ti);
            }
        }

        Objects.requireNonNull(newTarget);
        if (v.isEmpty()) {
            aaa.setTargetId(newTarget.getId());
            aaa.setTargetType(newTarget.getTargetType());
            target = newTarget;
            toHit = new ToHitData(TargetRoll.IMPOSSIBLE, "tag missed the target");
            return;
        }
        // get TAGs that are on the same map
        allowed = new Vector<>();
        for (TagInfo ti : v) {
            newTarget = ti.target;
            // homing target area is 8 hexes
            if (tc.distance(newTarget.getPosition()) <= Compute.HOMING_RADIUS) {
                allowed.add(ti);
            }
        }

        if (allowed.isEmpty()) {
            aaa.setTargetId(newTarget.getId());
            aaa.setTargetType(newTarget.getTargetType());
            target = newTarget;
            toHit = new ToHitData(TargetRoll.IMPOSSIBLE,
                  "no tag in 8 hex radius of target hex");
        } else if (allowed.size() == 1) {
            // Just use target 0...
            newTarget = allowed.get(0).target;
            target = newTarget;
            aaa.setTargetId(target.getId());
            aaa.setTargetType(target.getTargetType());
        } else {
            // The player gets to select the target
            List<Integer> targetIds = new ArrayList<>();
            List<Integer> targetTypes = new ArrayList<>();
            for (TagInfo target : allowed) {
                targetIds.add(target.target.getId());
                targetTypes.add(target.target.getTargetType());
            }
            int choice = gameManager.processTAGTargetCFR(attackingEntity.getOwnerId(), targetIds, targetTypes);
            newTarget = allowed.get(choice).target;
            target = newTarget;
            aaa.setTargetId(target.getId());
            aaa.setTargetType(target.getTargetType());
        }
    }

    /*
     * (non-Javadoc)
     *
     * @see
     * megamek.common.weapons.handlers.WeaponHandler#handleSpecialMiss(megamek.common
     * .Entity, boolean, megamek.common.units.Building, java.util.Vector)
     */
    @Override
    protected boolean handleSpecialMiss(Entity entityTarget,
          boolean bldgDamagedOnMiss, IBuilding bldg,
          Vector<Report> vPhaseReport) {
        return true;
    }

    /**
     * This is a unified method that handles single AMS and AMS Bay counterfire against Arrow IV homing missiles
     * Artillery bays resolve each weapon individually and don't use Aero AV, so we can safely do this
     *
     * @param vPhaseReport The report for this game phase, be it offboard (Indirect) or firing (Direct)
     * @param ammoUsed     The ammoType used by this bay - as only homing shots can be intercepted by AMS
     *
     * @return 1 hit if this missile survives any AMS fire, 0 if it is destroyed
     */
    protected int handleAMS(Vector<Report> vPhaseReport, AmmoMounted ammoUsed) {

        int hits = 1;
        boolean isArrowIV = ammoUsed.getType().getAmmoType() == AmmoTypeEnum.ARROW_IV;
        boolean isHoming = ammoUsed.isHomingAmmoInHomingMode();

        // TODO: this logic seems to be a bit off, rules need to be checked.
        if (!isArrowIV && !isHoming) {
            // If this is not an Arrow IV or a homing shot, we don't care about AMS
            return hits;
        }

        // this has to be called here, or it fires before the TAG shot and we have no
        // target
        gameManager.assignAMS();
        calcCounterAV();
        // Report AMS/Point defense failure due to Overheating.
        if (pdOverheated
              && (!(amsBayEngaged
              || amsBayEngagedCap
              || amsBayEngagedMissile
              || pdBayEngaged
              || pdBayEngagedCap
              || pdBayEngagedMissile))) {
            Report r = new Report(3359);
            r.subject = subjectId;
            r.indent();
            vPhaseReport.addElement(r);
        }
        // PD/AMS bays should engage using AV and missile armor per SO Errata
        if (amsBayEngagedCap || pdBayEngagedCap) {
            CapMissileArmor = ((WeaponType) ammoUsed.getLinkedBy().getType()).getMissileArmor() - CounterAV;
            CapMissileAMSMod = calcCapMissileAMSMod();
            Report report = new Report(3235);
            report.subject = subjectId;
            report.indent(1);
            vPhaseReport.add(report);
            if (CapMissileArmor <= 0) {
                report = new Report(3356);
                report.subject = subjectId;
                vPhaseReport.add(report);
                nDamPerHit = 0;
                hits = 0;
            } else {
                report = new Report(3358);
                report.subject = subjectId;
                report.add(CapMissileAMSMod);
                vPhaseReport.add(report);
                toHit.addModifier(CapMissileAMSMod, "damage from AMS");
                // If the damage was enough to make us miss, record it for reporting and set 0
                // hits
                if (roll.getIntValue() < toHit.getValue()) {
                    bMissed = true;
                    nDamPerHit = 0;
                    hits = 0;
                }
            }
        } else if (amsEngaged || apdsEngaged) {
            // Single AMS/APDS should continue to engage per TW rules, which have not
            // changed
            bSalvo = true;
            Report report = new Report(3235);
            report.subject = subjectId;
            vPhaseReport.add(report);
            report = new Report(3230);
            report.indent(1);
            report.subject = subjectId;
            vPhaseReport.add(report);
            Roll diceRoll = Compute.rollD6(1);

            if (diceRoll.getIntValue() <= 3) {
                report = new Report(3240);
                report.subject = subjectId;
                report.add("missile");
                report.add(diceRoll);
                vPhaseReport.add(report);
                nDamPerHit = 0;
                hits = 0;

            } else {
                report = new Report(3241);
                report.add("missile");
                report.add(diceRoll);
                report.subject = subjectId;
                vPhaseReport.add(report);
            }
        }
        return hits;
    }

    /**
     * Checks to see if the basic conditions needed for point defenses to work are in place Artillery weapons need to
     * change this slightly compared to other types of missiles
     */
    @Override
    protected boolean checkPDConditions() {
        return (target != null)
              && target.getTargetType() == Targetable.TYPE_ENTITY
              && advancedPD
              && advancedAMS
              && weaponAttackAction.getCounterEquipment() != null;
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
    protected int calcCapMissileAMSMod() {
        CapMissileAMSMod = (int) Math.ceil(CounterAV / 10.0);
        return CapMissileAMSMod;
    }

}

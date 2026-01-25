/*
 * Copyright (c) 2005 - Ben Mazur (bmazur@sev.org)
 * Copyright (C) 2007-2025 The MegaMek Team. All Rights Reserved.
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

import megamek.common.Hex;
import megamek.common.Messages;
import megamek.common.Report;
import megamek.common.TagInfo;
import megamek.common.ToHitData;
import megamek.common.actions.ArtilleryAttackAction;
import megamek.common.actions.WeaponAttackAction;
import megamek.common.board.Coords;
import megamek.common.compute.Compute;
import megamek.common.enums.GamePhase;
import megamek.common.equipment.AmmoType;
import megamek.common.equipment.AmmoType.AmmoTypeEnum;
import megamek.common.game.Game;
import megamek.common.loaders.EntityLoadingException;
import megamek.common.net.packets.InvalidPacketDataException;
import megamek.common.options.OptionsConstants;
import megamek.common.rolls.TargetRoll;
import megamek.common.units.Entity;
import megamek.common.units.IBuilding;
import megamek.common.units.Infantry;
import megamek.common.units.Targetable;
import megamek.common.weapons.handlers.AreaEffectHelper;
import megamek.logging.MMLogger;
import megamek.server.totalWarfare.TWGameManager;

public class ArtilleryWeaponIndirectHomingHandler extends ArtilleryWeaponIndirectFireHandler {
    private static final MMLogger LOGGER = MMLogger.create(ArtilleryWeaponIndirectHomingHandler.class);

    @Serial
    private static final long serialVersionUID = -7243477723032010917L;
    boolean advancedAMS;
    boolean advancedPD;

    /**
     *
     */
    public ArtilleryWeaponIndirectHomingHandler(ToHitData toHitData, WeaponAttackAction weaponAttackAction, Game game,
          TWGameManager gameManager) throws EntityLoadingException {
        super(toHitData, weaponAttackAction, game, gameManager);
        // PLAYTEST3 AMS below 0 is enabled
        advancedAMS =
              game.getOptions().booleanOption(OptionsConstants.ADVANCED_COMBAT_TAC_OPS_AMS) || game.getOptions()
                    .booleanOption(OptionsConstants.PLAYTEST_3);
        advancedPD = game.getOptions().booleanOption(OptionsConstants.ADVANCED_AERO_RULES_STRATOPS_ADV_POINT_DEFENSE);
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

        ArtilleryAttackAction aaa = (ArtilleryAttackAction) weaponAttackAction;
        if (phase.isTargeting()) {
            if (!handledAmmoAndReport) {
                addHeat();
                // Report the firing itself
                Report report = new Report(3121);
                report.indent();
                report.newlines = 0;
                report.subject = subjectId;
                report.add(weaponType.getName() + " (" + ammoType.getShortName() + ")");
                report.add(aaa.getTurnsTilHit());
                vPhaseReport.addElement(report);
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
        Entity entityTarget;
        try {
            convertHomingShotToEntityTarget();
            entityTarget = (aaa.getTargetType() == Targetable.TYPE_ENTITY) ? (Entity) aaa
                  .getTarget(game) : null;
        } catch (InvalidPacketDataException e) {
            LOGGER.error("Invalid packet data:", e);
            return false;
        }

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

        // we may still have to use ammo, if direct fire
        if (!handledAmmoAndReport) {
            addHeat();
        }

        // Any necessary PSRs, jam checks, etc.
        // If this boolean is true, don't report
        // the miss later, as we already reported
        // it in doChecks
        boolean missReported = doChecks(vPhaseReport);
        if (missReported) {
            bMissed = true;
        }
        nDamPerHit = weaponType.getRackSize();

        AmmoType ammoType = ammo.getType();

        // copperhead gets 10 damage less than standard
        if (!(ammoType.getAmmoType() == AmmoType.AmmoTypeEnum.ARROW_IV
              || ammoType.getAmmoType() == AmmoType.AmmoTypeEnum.ARROW_IV_BOMB)) {
            nDamPerHit -= 10;
        }

        // Do we need some sort of special resolution (minefields, artillery,
        if (specialResolution(vPhaseReport, entityTarget)) {
            return false;
        }

        // Any AMS/Point Defense fire against homing rounds?
        int hits = handleAMS(vPhaseReport);

        if (bMissed && !missReported) {
            // Notify player of last-second miss that hits the hex instead
            r = new Report(3201);
            r.subject = attackingEntity.getId();
            r.newlines = 0;
            vPhaseReport.addElement(r);

            reportMiss(vPhaseReport);

            // Works out fire setting, AMS shots, and whether continuation is
            // necessary.
            if (!handleSpecialMiss(entityTarget, bldgDamagedOnMiss, bldg,
                  vPhaseReport)) {
                return false;
            }
        }
        int nCluster = 1;
        if ((entityTarget != null) && (entityTarget.getTaggedBy() != -1)) {
            if (aaa.getCoords() != null && hits > 0) {
                toHit.setSideTable(entityTarget.sideTable(aaa.getCoords()));
            }

        }

        // The building shields all units from a certain amount of damage.
        // The amount is based upon the building's CF at the phase's start.
        int bldgAbsorbs = 0;
        if (targetInBuilding && (bldg != null)) {
            bldgAbsorbs = bldg.getAbsorption(target.getPosition());
        }
        if ((bldg != null) && (bldgAbsorbs > 0)) {
            // building absorbs some damage
            r = new Report(6010);
            r.subject = entityTarget.getId();
            r.add(bldgAbsorbs);
            vPhaseReport.addElement(r);
            Vector<Report> buildingReport = gameManager.damageBuilding(bldg,
                  nDamPerHit, target.getPosition());
            for (Report report : buildingReport) {
                report.subject = entityTarget.getId();
            }
            vPhaseReport.addAll(buildingReport);
        }
        nDamPerHit -= bldgAbsorbs;

        // Make sure the player knows when his attack causes no damage.
        if (nDamPerHit <= 0) {
            r = new Report(3365);
            r.subject = subjectId;
            vPhaseReport.addElement(r);
            return false;
        }

        boolean targetingHex = false;

        if (!bMissed && (entityTarget != null)) {
            handleEntityDamage(entityTarget, vPhaseReport, bldg, hits, nCluster, bldgAbsorbs);
            gameManager.creditKill(entityTarget, attackingEntity);
        } else if (!bMissed && // The attack is targeting a specific building
              (target.getTargetType() == Targetable.TYPE_BLDG_TAG)) {
            r = new Report(3390);
            r.subject = subjectId;
            vPhaseReport.addElement(r);
            vPhaseReport.addAll(gameManager.damageBuilding(bldg,
                  nDamPerHit, target.getPosition()));
        } else if (!bMissed) { // Hex is targeted, need to report a hit
            r = new Report(3390);
            r.subject = subjectId;
            vPhaseReport.addElement(r);
            targetingHex = true;
        }

        // Use original target coordinates for splash damage, not current entity position.
        // If target was converted from hex to entity, use saved coords; otherwise use current position.
        Coords coords = (aaa.getOldTargetCoords() != null) ? aaa.getOldTargetCoords() : target.getPosition();
        int ratedDamage = 5; // splash damage is 5 from all launchers

        // If AMS shoots down a missile, it shouldn't deal any splash damage
        if (hits == 0) {
            ratedDamage = 0;
        }

        // homing artillery splash damage is area effect.
        // do damage to woods, 2 * normal damage (TW page 112)
        // on the other hand, if the hex *is* the target, do full damage
        int hexDamage = targetingHex ? weaponType.getRackSize() : ratedDamage * 2;

        bldg = game.getBoard().getBuildingAt(coords);
        bldgAbsorbs = (bldg != null) ? bldg.getAbsorption(coords) : 0;
        bldgAbsorbs = Math.min(bldgAbsorbs, ratedDamage);
        handleClearDamage(vPhaseReport, bldg, hexDamage, false);
        ratedDamage -= bldgAbsorbs;

        if (ratedDamage > 0) {
            Hex hex = game.getBoard().getHex(coords);

            for (Entity entity : game.getEntitiesVector(coords)) {
                if (!bMissed && (entity == entityTarget)) {
                    continue; // don't splash the original target unless it's a miss
                }

                AreaEffectHelper.artilleryDamageEntity(
                      entity, ratedDamage, bldg, bldgAbsorbs,
                      false, false, false, 0,
                      coords, ammoType, coords, false,
                      attackingEntity, hex, attackingEntity.getId(), vPhaseReport, gameManager);
            }
        }
        Report.addNewline(vPhaseReport);
        return false;
    }

    /**
     * Find the tagged entity for this attack Uses a CFR to let the player choose from eligible TAG
     */
    public void convertHomingShotToEntityTarget() throws InvalidPacketDataException {
        LOGGER.debug("convertHomingShotToEntityTarget: processing homing shot for attacker {}",
              attackingEntity != null ? attackingEntity.getDisplayName() : "null");
        ArtilleryAttackAction aaa = (ArtilleryAttackAction) weaponAttackAction;

        final Coords tc = target.getPosition();
        // Save original target coordinates before converting to entity target.
        // This ensures splash damage is applied at the original targeted hex,
        // not wherever the entity moved to. (Fix for issue #7274)
        aaa.setOldTargetCoords(tc);
        Targetable newTarget = null;

        Vector<TagInfo> v = game.getTagInfo();
        Vector<TagInfo> allowed = new Vector<>();
        Entity attacker = game.getEntityFromAllSources(getAttackerId());

        // get only TagInfo on the same side
        for (TagInfo ti : v) {
            Entity tagger = game.getEntityFromAllSources(ti.attackerId);
            if (attacker.getOwner().isEnemyOf(tagger.getOwner())) {
                continue;
            }

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

        LOGGER.debug("convertHomingShotToEntityTarget: found {} allowed TAG targets", allowed.size());
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
                LOGGER.debug("Found valid TAG on target {}; Range to original target is {}",
                      ti.target.getDisplayName(), tc.distance(ti.target.getPosition()));
            }
        }

        Objects.requireNonNull(newTarget);
        if (v.isEmpty()) {
            LOGGER.debug("convertHomingShotToEntityTarget: all TAGs missed");
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
        LOGGER.debug("convertHomingShotToEntityTarget: {} TAGs within homing radius", allowed.size());
        if (allowed.isEmpty()) {
            aaa.setTargetId(newTarget.getId());
            aaa.setTargetType(newTarget.getTargetType());
            target = newTarget;
            toHit = new ToHitData(TargetRoll.IMPOSSIBLE,
                  "no tag in 8 hex radius of target hex");
        } else if (allowed.size() == 1) {
            // Just use target 0...
            LOGGER.debug("convertHomingShotToEntityTarget: single target, auto-selecting");
            newTarget = allowed.get(0).target;
            target = newTarget;
            aaa.setTargetId(target.getId());
            aaa.setTargetType(target.getTargetType());
            toHit = new ToHitData(4, Messages.getString("ArtilleryIndirectHomingHandler.HomingArtyMissChance"));
        } else {
            // The player gets to select the target
            LOGGER.debug("convertHomingShotToEntityTarget: {} targets available, requesting player selection",
                  allowed.size());
            List<Integer> targetIds = new ArrayList<>();
            List<Integer> targetTypes = new ArrayList<>();
            for (TagInfo target : allowed) {
                targetIds.add(target.target.getId());
                targetTypes.add(target.target.getTargetType());
            }
            int choice = gameManager.processTAGTargetCFR(attackingEntity.getOwnerId(), targetIds, targetTypes);
            LOGGER.debug("convertHomingShotToEntityTarget: player selected target index {}", choice);
            newTarget = allowed.get(choice).target;
            target = newTarget;
            aaa.setTargetId(target.getId());
            aaa.setTargetType(target.getTargetType());
            toHit = new ToHitData(4, Messages.getString("ArtilleryIndirectHomingHandler.HomingArtyMissChance"));
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
     * Checks to see if the basic conditions needed for point defenses to work are in place Artillery weapons need to
     * change this slightly
     */
    @Override
    protected boolean checkPDConditions() {
        advancedPD = game.getOptions().booleanOption(OptionsConstants.ADVANCED_AERO_RULES_STRATOPS_ADV_POINT_DEFENSE);
        return (target != null) && advancedPD && (target.getTargetType() == Targetable.TYPE_ENTITY);
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

    protected int handleAMS(Vector<Report> vPhaseReport) {

        int hits = 1;
        boolean isArrowIV = ammo.getType().getAmmoType() == AmmoTypeEnum.ARROW_IV;
        boolean isHoming = ammo.isHomingAmmoInHomingMode();
        // TODO: this logic seems to be a bit off, rules need to be checked.
        if (!isArrowIV && !isHoming) {
            // If this is not an Arrow IV or homing shot, we don't need to do AMS/PD
            return hits;
        }
        // this has to be called here, or it fires before the TAG shot and we have no
        // target
        gameManager.assignAMS();
        calcCounterAV();
        // Report AMS/Point-defense failure due to Overheating.
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
            CapMissileArmor = weaponType.getMissileArmor() - CounterAV;
            CapMissileAMSMod = calcCapMissileAMSMod();
            Report r = new Report(3235);
            r.subject = subjectId;
            r.indent(1);
            vPhaseReport.add(r);
            if (CapMissileArmor <= 0) {
                r = new Report(3356);
                r.subject = subjectId;
                vPhaseReport.add(r);
                nDamPerHit = 0;
                hits = 0;
            } else {
                r = new Report(3358);
                r.subject = subjectId;
                r.add(CapMissileAMSMod);
                vPhaseReport.add(r);
                toHit.addModifier(CapMissileAMSMod, "damage from AMS");
                // If the damage was enough to make us miss, record it for reporting and set 0
                // hits
                if (roll.getIntValue() < toHit.getValue()) {
                    bMissed = true;
                    nDamPerHit = 0;
                    hits = 0;
                }
            }
        }
        return hits;
    }
}

/*
 * Copyright (C) 2026 The MegaMek Team. All Rights Reserved.
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

import static java.lang.Math.floor;

import java.io.Serial;
import java.util.Vector;

import megamek.common.HitData;
import megamek.common.Report;
import megamek.common.ToHitData;
import megamek.common.actions.WeaponAttackAction;
import megamek.common.compute.Compute;
import megamek.common.game.Game;
import megamek.common.loaders.EntityLoadingException;
import megamek.common.options.OptionsConstants;
import megamek.common.rolls.Roll;
import megamek.common.units.Entity;
import megamek.common.units.IBuilding;
import megamek.common.units.Infantry;
import megamek.common.units.Mek;
import megamek.common.units.Tank;
import megamek.common.units.Targetable;
import megamek.logging.MMLogger;
import megamek.server.totalWarfare.TWGameManager;

/**
 * Resolves an attack by a Flamer, Fluid Gun or Sprayer firing Water Ammo (TO:AUE p.174). Water douses
 * fires, reduces a heat-tracking target's heat by 1 (max 6 per turn), and against conventional infantry
 * inflicts a 1D6/2 (round up) burst-fire attack whose casualties are knocked out rather than killed and
 * therefore recoverable.
 *
 * @author The MegaMek Team
 */
public class WaterHandler extends AmmoWeaponHandler {
    private static final MMLogger LOGGER = MMLogger.create(WaterHandler.class);

    @Serial
    private static final long serialVersionUID = 8267429735710097183L;

    public WaterHandler(ToHitData toHit, WeaponAttackAction waa, Game game, TWGameManager manager)
          throws EntityLoadingException {
        super(toHit, waa, game, manager);
        generalDamageType = HitData.DAMAGE_BALLISTIC;
    }

    @Override
    protected boolean specialResolution(Vector<Report> vPhaseReport, Entity entityTarget) {
        if (!bMissed && (target.getTargetType() == Targetable.TYPE_HEX_EXTINGUISH)) {
            // Water douses an ordinary hex fire on a 2D6 roll of 3+ (12 for an Inferno fire) (TO:AUE p.174).
            FluidFireSuppression.extinguishHex(game, gameManager, target, subjectId, 3, vPhaseReport);
            return true;
        }
        return false;
    }

    @Override
    protected int calcDamagePerHit() {
        if (!target.isConventionalInfantry()) {
            return 0;
        }
        // Water inflicts a 1D6/2 (round up) burst-fire attack against conventional infantry (TO:AUE p.174).
        double damage = Math.ceil(Compute.d6() / 2.0);
        if (bDirect) {
            damage += floor(toHit.getMoS() / 3.0);
        }
        if (((Entity) target).hasAbility(OptionsConstants.MD_PAIN_SHUNT)) {
            damage /= 2;
        }
        damage = applyGlancingBlowModifier(damage, true);
        return (int) damage;
    }

    @Override
    protected void handleEntityDamage(Entity entityTarget, Vector<Report> vPhaseReport, IBuilding building,
          int hits, int nCluster, int bldgAbsorbs) {
        if (entityTarget.isConventionalInfantry()) {
            // Apply the burst-fire damage, then record the resulting casualties as knocked-out (recoverable)
            // troopers rather than dead (TO:AUE p.174). For infantry, internal structure is the trooper count.
            int troopersBefore = entityTarget.getTotalInternal();
            super.handleEntityDamage(entityTarget, vPhaseReport, building, hits, nCluster, bldgAbsorbs);
            int knockedOut = troopersBefore - entityTarget.getTotalInternal();
            if ((knockedOut > 0) && (entityTarget instanceof Infantry infantry)) {
                infantry.addKnockedOutTroopers(knockedOut);
            }
            LOGGER.debug("[Fluid:Water] {}: {} trooper(s) knocked out (recoverable), {} now have {} total",
                  entityTarget.getShortName(), knockedOut, entityTarget.getShortName(),
                  entityTarget.getTotalInternal());
            return;
        }

        // Water does no damage to anything but infantry; its remaining effects are firefighting and cooling.
        Report report = new Report(3390);
        report.subject = subjectId;
        vPhaseReport.addElement(report);

        if (entityTarget.infernos.isStillBurning()
              || ((target instanceof Tank tank) && tank.isOnFire() && tank.isInfernoFire())) {
            // Inferno-fuelled fires are only put out by Water on a roll of 12 (TO:AUE p.174).
            report = new Report(3545);
            report.subject = subjectId;
            report.addDesc(entityTarget);
            report.indent(3);
            Roll diceRoll = Compute.rollD6(2);
            report.add(diceRoll);
            boolean doused = diceRoll.getIntValue() == 12;
            if (doused) {
                report.choose(true);
                entityTarget.infernos.clear();
            } else {
                report.choose(false);
            }
            LOGGER.debug("[Fluid:Water] {}: inferno-fire douse rolled {} vs 12 -> {}",
                  entityTarget.getShortName(), diceRoll.getIntValue(), doused ? "OUT" : "still burning");
            vPhaseReport.add(report);
        } else if ((target instanceof Tank tank) && tank.isOnFire()) {
            // Ordinary (non-Inferno) fires are doused by Water on a roll of 3+ (TO:AUE p.174).
            report = new Report(3550);
            report.subject = subjectId;
            report.addDesc(entityTarget);
            report.indent(3);
            Roll diceRoll = Compute.rollD6(2);
            report.add(diceRoll);
            boolean doused = diceRoll.getIntValue() >= 3;
            if (doused) {
                report.choose(true);
                tank.extinguishAll();
            } else {
                report.choose(false);
            }
            LOGGER.debug("[Fluid:Water] {}: vehicle-fire douse rolled {} vs 3 -> {}",
                  entityTarget.getShortName(), diceRoll.getIntValue(), doused ? "OUT" : "still burning");
            vPhaseReport.add(report);
        }

        // Water also reduces the heat level of a heat-tracking target by 1 point per hit (TO:AUE p.174).
        if (target instanceof Mek) {
            int cooling = 1;
            report = new Report(3400);
            report.subject = subjectId;
            report.indent(2);
            report.add(cooling);
            report.choose(false);
            vPhaseReport.add(report);
            entityTarget.coolFromExternal += cooling;
            LOGGER.debug("[Fluid:Water] cooled {} by {} heat this turn (pending external cooling now {})",
                  entityTarget.getShortName(), cooling, entityTarget.coolFromExternal);
        }

        // Water can wash Paint/Obscurant sensor fouling off a unit on a 2D6 roll of 9+ (TO:AUE p.174).
        if (entityTarget.getObscurantToHitPenalty() > 0) {
            Roll washRoll = Compute.rollD6(2);
            Report washReport = new Report(3397);
            washReport.subject = subjectId;
            washReport.addDesc(entityTarget);
            washReport.add(washRoll);
            washReport.indent(2);
            vPhaseReport.add(washReport);
            boolean washedOff = washRoll.getIntValue() >= 9;
            if (washedOff) {
                entityTarget.clearObscurantToHitPenalty();
                Report cleared = new Report(3399);
                cleared.subject = subjectId;
                cleared.indent(3);
                vPhaseReport.add(cleared);
            }
            LOGGER.debug("[Fluid:Water] {}: obscurant wash rolled {} vs 9 -> {}",
                  entityTarget.getShortName(), washRoll.getIntValue(), washedOff ? "CLEARED" : "still fouled");
        }
    }
}

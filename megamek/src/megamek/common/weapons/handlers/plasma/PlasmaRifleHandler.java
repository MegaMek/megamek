/*
 * Copyright (C) 2005 Ben Mazur (bmazur@sev.org)
 * Copyright (C) 2007-2026 The MegaMek Team. All Rights Reserved.
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

package megamek.common.weapons.handlers.plasma;

import java.io.Serial;
import java.util.Vector;

import jakarta.annotation.Nullable;
import megamek.common.RangeType;
import megamek.common.Report;
import megamek.common.ToHitData;
import megamek.common.actions.WeaponAttackAction;
import megamek.common.battleArmor.BattleArmor;
import megamek.common.board.Coords;
import megamek.common.compute.Compute;
import megamek.common.enums.HitDamageType;
import megamek.common.equipment.WeaponType;
import megamek.common.game.Game;
import megamek.common.loaders.EntityLoadingException;
import megamek.common.options.OptionsConstants;
import megamek.common.rolls.TargetRoll;
import megamek.common.units.Entity;
import megamek.common.units.IBuilding;
import megamek.common.weapons.handlers.AmmoWeaponHandler;
import megamek.common.weapons.ppc.clan.CLPlasmaCannon;
import megamek.common.weapons.ppc.innerSphere.ISHeavyPlasmaRifle;
import megamek.common.weapons.ppc.innerSphere.ISLightPlasmaRifle;
import megamek.server.totalWarfare.TWGameManager;

public class PlasmaRifleHandler extends AmmoWeaponHandler {
    @Serial
    private static final long serialVersionUID = -2092721653693187140L;

    /**
     * @param toHitData          The {@link ToHitData} to use.
     * @param weaponAttackAction The {@link WeaponAttackAction} to use.
     * @param game               The {@link Game} object to use.
     * @param twGameManager      A {@link TWGameManager} to use.
     */
    public PlasmaRifleHandler(ToHitData toHitData, WeaponAttackAction weaponAttackAction, Game game,
          TWGameManager twGameManager) throws EntityLoadingException {
        super(toHitData, weaponAttackAction, game, twGameManager);
        generalDamageType = HitDamageType.DAMAGE_HEAT;
    }

    @Override
    protected @Nullable Report applyHeatOnStrike(Entity entityTarget, int damageThatStrikes) {
        if (!entityTarget.tracksHeat()) {
            return null;
        }
        // Squadrons roll separately for each weapon that hit
        int extraHeat = 0;
        for (int i = 0; i < numWeaponsHit; i++) {
            extraHeat += rollPlasmaHeat(weapon.getType());
        }
        return applyExternalHeat(entityTarget, extraHeat);
    }

    /** @return The heat a plasma weapon of the type deals to a target it strikes (Core p. 189) */
    static int rollPlasmaHeat(WeaponType type) {
        if (type instanceof ISLightPlasmaRifle) {
            return (int) Math.ceil(Compute.d6() / 2.0);
        }
        return ((type instanceof ISHeavyPlasmaRifle) || (type instanceof CLPlasmaCannon)) ? Compute.d6(2)
              : Compute.d6();
    }

    @Override
    protected int calcDamagePerHit() {
        if (target.tracksHeat()) {
            int toReturn = weapon.getType().getDamage();
            toReturn = applyGlancingBlowModifier(toReturn, false);
            if (game.getOptions().booleanOption(OptionsConstants.ADVANCED_COMBAT_TAC_OPS_RANGE) &&
                  (nRange > weaponType.getRanges(weapon)[RangeType.RANGE_LONG])) {
                toReturn -= 1;
            }
            if (game.getOptions().booleanOption(OptionsConstants.ADVANCED_COMBAT_TAC_OPS_LOS_RANGE) &&
                  (nRange > weaponType.getRanges(weapon)[RangeType.RANGE_EXTREME])) {
                toReturn = (int) Math.floor(toReturn / 2.0);
            }
            return toReturn;
        }
        return 1;
    }

    @Override
    protected int calculateNumCluster() {
        if (target.tracksHeat()) {
            bSalvo = false;
            return 1;
        }

        int toReturn = 5;

        if (target.isConventionalInfantry()) {
            if (weapon.getType() instanceof ISLightPlasmaRifle) {
                toReturn = Compute.d6();
            } else if (weapon.getType() instanceof ISHeavyPlasmaRifle) {
                toReturn = Compute.d6(3);
            } else {
                toReturn = Compute.d6(2);
            }
        }

        bSalvo = true;
        return toReturn;
    }

    @Override
    protected int calcHits(Vector<Report> vPhaseReport) {
        int toReturn;
        // against meks, 1 hit with 10 damage, plus heat
        if (target.tracksHeat()) {
            toReturn = 1;
            // otherwise, 10+2d6 damage but fire-resistant BA armor gets no damage from heat, and half the normal
            // one, so only 5 damage
        } else {
            WeaponType plasmaWeapon = weapon.getType();
            if ((target instanceof BattleArmor) && ((BattleArmor) target).isFireResistant()) {
                toReturn = plasmaWeapon.getDamage() / 2;
            } else {
                int damage = plasmaWeapon.getDamage();
                if (plasmaWeapon instanceof ISLightPlasmaRifle) {
                    damage += Compute.d6();
                } else if (plasmaWeapon instanceof ISHeavyPlasmaRifle) {
                    damage += Compute.d6(3);
                } else {
                    damage += Compute.d6(2);
                }
                toReturn = damage;
            }
            toReturn = applyGlancingBlowModifier(toReturn, false);
            // Conventional infantry and battle armor with an Artificial Pain Shunt halve flame damage (IO p. 78).
            // Plasma delivers that damage as 1-point hits, so the hit count is what carries the damage total.
            // Halving the cluster size instead, as this handler used to, only regroups the same total.
            toReturn = (int) applyPainShuntModifier(toReturn);
        }
        return toReturn;
    }

    @Override
    protected void handleIgnitionDamage(Vector<Report> vPhaseReport, IBuilding bldg, int hits) {
        if (!bSalvo) {
            // hits!
            Report report = new Report(2270);
            report.subject = subjectId;
            report.newlines = 0;
            vPhaseReport.addElement(report);
        }

        TargetRoll targetRoll = new TargetRoll(weaponType.getFireTN(), weaponType.getName());
        if (targetRoll.getValue() != TargetRoll.IMPOSSIBLE) {
            Report.addNewline(vPhaseReport);
            gameManager.tryIgniteHex(target.getPosition(),
                  target.getBoardId(),
                  subjectId,
                  true,
                  false,
                  targetRoll,
                  true,
                  -1,
                  vPhaseReport);
        }
    }

    @Override
    protected void handleClearDamage(Vector<Report> vPhaseReport, IBuilding bldg, int nDamage) {
        if (!bSalvo) {
            // hits!
            Report report = new Report(2270);
            report.subject = subjectId;
            vPhaseReport.addElement(report);
        }

        nDamage *= 2; // Plasma weapons deal double damage to woods.

        // report that damage was "applied" to terrain
        Report report = new Report(3385);
        report.indent(2);
        report.subject = subjectId;
        report.add(nDamage);
        vPhaseReport.addElement(report);

        // Any clear attempt can result in accidental ignition, even weapons that can't normally start fires. that's
        // weird. Buildings can't be accidentally ignited.
        // TODO: change this for TacOps - now you roll another 2d6 first and on a 5 or less you do a normal ignition
        //  as though for intentional fires
        if ((bldg != null) &&
              gameManager.tryIgniteHex(target.getPosition(), target.getBoardId(), subjectId,
                    true,
                    false,
                    new TargetRoll(weaponType.getFireTN(), weaponType.getName()),
                    5,
                    vPhaseReport)) {
            return;
        }

        Vector<Report> clearReports = gameManager.tryClearHex(target.getPosition(),
              target.getBoardId(),
              nDamage,
              subjectId);
        if (!clearReports.isEmpty()) {
            vPhaseReport.lastElement().newlines = 0;
        }

        vPhaseReport.addAll(clearReports);
    }

    @Override
    protected void handleBuildingDamage(Vector<Report> vPhaseReport, IBuilding bldg, int nDamage, Coords coords) {
        // Plasma weapons deal double damage to buildings.
        super.handleBuildingDamage(vPhaseReport, bldg, nDamage * 2, coords);
    }
}

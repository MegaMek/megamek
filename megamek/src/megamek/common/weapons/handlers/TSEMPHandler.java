/*
 * Copyright (C) 2005 Ben Mazur (bmazur@sev.org)
 * Copyright (C) 2014-2025 The MegaMek Team. All Rights Reserved.
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
import java.util.Vector;

import megamek.MMConstants;
import megamek.common.CriticalSlot;
import megamek.common.EMPEffectFormatter;
import megamek.common.Report;
import megamek.common.ToHitData;
import megamek.common.actions.WeaponAttackAction;
import megamek.common.battleArmor.BattleArmor;
import megamek.common.compute.Compute;
import megamek.common.enums.GamePhase;
import megamek.common.equipment.Engine;
import megamek.common.equipment.Mounted;
import megamek.common.game.Game;
import megamek.common.loaders.EntityLoadingException;
import megamek.common.rolls.Roll;
import megamek.common.units.Aero;
import megamek.common.units.ConvFighter;
import megamek.common.units.Entity;
import megamek.common.units.IBuilding;
import megamek.common.units.Mek;
import megamek.common.units.ProtoMek;
import megamek.common.units.SupportTank;
import megamek.common.units.Tank;
import megamek.server.totalWarfare.TWGameManager;

/**
 * Weapon handler for the Tight-Stream Electro-Magnetic Pulse (TSEMP) weapon, which is found in FM:3145 pg 255.
 *
 * @author arlith Created on Sept 5, 2005
 */
public class TSEMPHandler extends EnergyWeaponHandler {
    @Serial
    private static final long serialVersionUID = 5545991061428671743L;

    /**
     *
     */
    public TSEMPHandler(ToHitData t, WeaponAttackAction w, Game g, TWGameManager m) throws EntityLoadingException {
        super(t, w, g, m);
    }

    /*
     * (non-Javadoc)
     *
     * @see megamek.common.weapons.handlers.EnergyWeaponHandler#calcDamagePerHit()
     */
    @Override
    protected int calcDamagePerHit() {
        return 0;
    }

    // Copied from
    // megamek.common.weapons.handlers.HVACWeaponHandler#doChecks(java.util.Vector)
    /*
     * (non-Javadoc)
     *
     * @see megamek.common.weapons.handlers.WeaponHandler#doChecks(java.util.Vector)
     */
    @Override
    protected boolean doChecks(Vector<Report> vPhaseReport) {
        if (roll.getIntValue() == 2 && weapon.is("TSEMP Repeating Cannon")) {
            Report report = new Report(3162);
            report.subject = subjectId;
            weapon.setHit(true);
            int weaponLocation = weapon.getLocation();
            for (int i = 0; i < attackingEntity.getNumberOfCriticalSlots(weaponLocation); i++) {
                CriticalSlot slot1 = attackingEntity.getCritical(weaponLocation, i);
                if ((slot1 == null) ||
                      (slot1.getType() == CriticalSlot.TYPE_SYSTEM)) {
                    continue;
                }
                Mounted<?> mounted = slot1.getMount();
                if (mounted.equals(weapon)) {
                    attackingEntity.hitAllCriticalSlots(weaponLocation, i);
                    break;
                }
            }
            vPhaseReport.addAll(gameManager.explodeEquipment(attackingEntity, weaponLocation, weapon));
            report.choose(false);
            vPhaseReport.addElement(report);
            return true;
        } else {
            return super.doChecks(vPhaseReport);
        }
    }

    @Override
    public boolean handle(GamePhase phase, Vector<Report> vPhaseReport) {
        weapon.setFired(true);

        attackingEntity.setFiredTsempThisTurn(true);
        attackingEntity.setHasFiredTsemp(true);

        if (attackingEntity.getTsempEffect() == MMConstants.TSEMP_EFFECT_NONE) {
            attackingEntity.setTsempEffect(MMConstants.TSEMP_EFFECT_INTERFERENCE);
        }

        return super.handle(phase, vPhaseReport);
    }

    @Override
    protected void handleEntityDamage(Entity entityTarget, Vector<Report> vPhaseReport,
          IBuilding bldg, int hits, int nCluster, int bldgAbsorbs) {
        super.handleEntityDamage(entityTarget, vPhaseReport, bldg, hits, nCluster, bldgAbsorbs);

        // Increment the TSEMP hit counter
        entityTarget.addTsempHitThisTurn();

        // Report that this unit has been hit by TSEMP
        Report r = new Report(7410);
        r.subject = entityTarget.getId();
        r.addDesc(entityTarget);
        r.add(entityTarget.getTsempHitsThisTurn());
        r.indent(2);
        vPhaseReport.add(r);

        // TSEMP has no effect against infantry
        if (entityTarget.isConventionalInfantry()) {
            // No Effect
            r = new Report(7415);
            r.subject = entityTarget.getId();
            r.indent(3);
            vPhaseReport.add(r);
            return;
        }

        // Determine roll modifiers
        int tsempModifiers = 0;
        if (entityTarget.getWeight() >= 200) {
            // No Effect
            r = new Report(7416);
            r.subject = entityTarget.getId();
            r.indent(3);
            vPhaseReport.add(r);
            return;
        } else if (entityTarget.getWeight() >= 100) {
            tsempModifiers -= 2;
        }

        if (entityTarget.getEngine() != null &&
              entityTarget.getEngine().getEngineType() == Engine.COMBUSTION_ENGINE) {
            tsempModifiers -= 1;
        } else if (entityTarget.getEngine() != null &&
              entityTarget.getEngine().getEngineType() == Engine.STEAM) {
            tsempModifiers -= 2;
        }

        tsempModifiers += Math.min(4, entityTarget.getTsempHitsThisTurn() - 1);
        // Multiple hits add a +1 for each hit after the first,
        // up to a max of 4
        Roll diceRoll = Compute.rollD6(2);
        int rollValue = Math.max(2, diceRoll.getIntValue() + tsempModifiers);
        String rollCalc = rollValue + " [" + diceRoll.getIntValue() + " + " + tsempModifiers + "] max 2";

        // Ugly code to set the target rolls
        int shutdownTarget = 13;
        int interferenceTarget = 13;
        if (entityTarget instanceof Mek) {
            if (((Mek) entityTarget).isIndustrial()) {
                interferenceTarget = 6;
                shutdownTarget = 8;
            } else {
                interferenceTarget = 7;
                shutdownTarget = 9;
            }
        } else if (entityTarget instanceof SupportTank) {
            interferenceTarget = 5;
            shutdownTarget = 7;
        } else if (entityTarget instanceof Tank) {
            interferenceTarget = 6;
            shutdownTarget = 8;
        } else if (entityTarget instanceof BattleArmor) {
            interferenceTarget = 6;
            shutdownTarget = 8;
        } else if (entityTarget instanceof ProtoMek) {
            interferenceTarget = 6;
            shutdownTarget = 9;
        } else if (entityTarget instanceof ConvFighter) {
            interferenceTarget = 6;
            shutdownTarget = 8;
        } else if (entityTarget instanceof Aero) {
            interferenceTarget = 7;
            shutdownTarget = 9;
        }

        // Create the effect report
        if (tsempModifiers == 0) {
            r = new Report(7411);
        } else {
            r = new Report(7412);
            if (tsempModifiers >= 0) {
                r.add("+" + tsempModifiers);
            } else {
                r.add(tsempModifiers);
            }
        }
        r.indent(3);
        r.addDataWithTooltip(rollCalc, diceRoll.getReport());
        r.subject = entityTarget.getId();
        String tsempEffect;

        // Determine the effect - use shared formatter for consistent display
        Report baShutdownReport = null;
        if (rollValue >= shutdownTarget) {
            entityTarget.setTsempEffect(MMConstants.TSEMP_EFFECT_SHUTDOWN);
            tsempEffect = EMPEffectFormatter.formatEffect(MMConstants.TSEMP_EFFECT_SHUTDOWN);
            if (entityTarget instanceof BattleArmor) {
                baShutdownReport = new Report(3706);
                baShutdownReport.addDesc(entityTarget);
                baShutdownReport.indent(4);
                baShutdownReport.add(entityTarget.getLocationAbbr(hit));
                // TODO : fix for salvage purposes
                entityTarget.destroyLocation(hit.getLocation());
                // Check to see if the squad has been eliminated
                if (entityTarget.getTransferLocation(hit).getLocation() == Entity.LOC_DESTROYED) {
                    vPhaseReport.addAll(gameManager.destroyEntity(entityTarget,
                          "all troopers eliminated", false));
                }
            } else {
                entityTarget.setShutDown(true);
            }
        } else if (rollValue >= interferenceTarget) {
            int targetEffect = entityTarget.getTsempEffect();
            if (targetEffect != MMConstants.TSEMP_EFFECT_SHUTDOWN) {
                entityTarget.setTsempEffect(MMConstants.TSEMP_EFFECT_INTERFERENCE);
            }
            tsempEffect = EMPEffectFormatter.formatEffect(MMConstants.TSEMP_EFFECT_INTERFERENCE);
        } else {
            // No effect roll
            tsempEffect = EMPEffectFormatter.formatEffect(MMConstants.TSEMP_EFFECT_NONE);
        }
        r.add(tsempEffect);
        vPhaseReport.add(r);
        if (baShutdownReport != null) {
            vPhaseReport.add(baShutdownReport);
        }
    }
}

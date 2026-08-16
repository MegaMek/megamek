package megamek.common.weapons.handlers.mrm;

import megamek.common.Hex;
import megamek.common.Report;
import megamek.common.ToHitData;
import megamek.common.actions.WeaponAttackAction;
import megamek.common.battleArmor.BattleArmor;
import megamek.common.board.Coords;
import megamek.common.compute.Compute;
import megamek.common.compute.ComputeArc;
import megamek.common.enums.GamePhase;
import megamek.common.equipment.AmmoMounted;
import megamek.common.equipment.Mounted;
import megamek.common.equipment.WeaponMounted;
import megamek.common.equipment.WeaponType;
import megamek.common.game.Game;
import megamek.common.loaders.EntityLoadingException;
import megamek.common.units.Entity;
import megamek.common.units.Targetable;
import megamek.common.weapons.handlers.DamageFalloff;
import megamek.server.totalWarfare.TWGameManager;

import java.util.List;
import java.util.Vector;

import static megamek.common.equipment.AmmoType.INCENDIARY_MOD;

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
public class MRMSaturationHandler extends MRMHandler {
    public MRMSaturationHandler(ToHitData t, WeaponAttackAction w, Game g, TWGameManager m) throws EntityLoadingException {
        super(t, w, g, m);
    }

    /**
     * Overridden AttackHandler to skip a lot of extraneous missile prep work, report the Saturation attack, prep
     * AMS for this attack (Saturation mode attacks don't target entities so don't trigger AMS / APDS normally),
     * and finally execute the AE damage allocation in the targeted hex (if a hit is rolled).
     *
     * @param phase         The present game phase
     * @param vPhaseReport The reports list to add new reports to
     *
     * @return boolean      Does this attack need to be kept for later processing?  False in most cases.
     */
    @Override
    public boolean handle(GamePhase phase, Vector<Report> vPhaseReport) {
        // Using handle instead of specialResolution skips a bunch of missile-related stuff that doesn't apply.
        if (!cares(phase)) {
            return true;
        }

        Coords targetPos = target.getPosition();
        Hex targetHex = game.getHexOf(target);

        Report r = new Report(9941);
        r.subject = subjectId;
        r.player = attackingEntity.getOwnerId();
        r.add(attackingEntity.getShortName());
        r.add(targetPos.getBoardNum());
        vPhaseReport.addElement(r);

        // do we hit?
        bMissed = roll.getIntValue() < toHit.getValue();
        if (bMissed) {
            // Report the miss; nothing happens.
            // Also, how did you miss the ground?!
            r = new Report(3196);
            r.subject = subjectId;
            r.player = attackingEntity.getOwnerId();
            r.add(targetPos.getBoardNum());
            vPhaseReport.addElement(r);

            // Don't keep this handler around any longer.
            return false;
        }

        // First, calc number of missiles on the cluster table.  Note: do not apply MRM standard -1 penalty here!
        // This handles cluster mods and AMS/APDS
        int count = calcHits(vPhaseReport);

        // Core rules p.197 does not list a minumum damage amount. If only 1 missile hits, no damage will be dealt.
        int damage = (int) Math.round(count / 3.0);

        // Note: MRM Saturation does not scatter!
        if (!bMissed) {
            r = new Report(3190);
            r.subject = subjectId;
            r.player = attackingEntity.getOwnerId();
            r.add(targetPos.getBoardNum());
            vPhaseReport.addElement(r);

            // Set blast damage and falloff (this represents a single-hex AE damage volume
            DamageFalloff damageFalloff = new DamageFalloff();
            damageFalloff.radius = 0;
            damageFalloff.damage = damage;
            damageFalloff.falloff = damage;
            damageFalloff.clusterMunitionsFlag = false;

            gameManager.artilleryDamageArea(targetPos, target.getBoardId(), ammoType,
                  subjectId, attackingEntity, damageFalloff, false, targetHex.getLevel(), vPhaseReport,
                  false
            );
        }

        return false;
    }

    /**
     * We have to override this because MRMHandler falls through to MissileWeaponHandler, but that requires an
     * entity target, whereas we need to deal with a hex possibly full of enemies.
     *
     * @param vPhaseReport
     * @return missile count mod (0 or negative)
     */
    @Override
    protected int getAMSHitsMod(Vector<Report> vPhaseReport) {
        // No mod for no target!
        if (target == null || target.getTargetType() != Targetable.TYPE_SATURATION) {
            return 0;
        }

        int apdsMod = 0;
        int amsMod = 0;

        // any AMS attacks by enemies in the target hex?
        List<WeaponMounted> lCounters = weaponAttackAction.getCounterEquipment();
        if (null != lCounters) {
            // Track firing for each counter, as this attack can be engaged multiple times
            // (unlike other missile attacks)
            boolean localAMSEngaged, localAPDSEngaged;
            int localAMSMod, localAPDSMod;

            // resolve AMS counter-fire
            for (WeaponMounted counter : lCounters) {
                localAMSEngaged = false;
                localAPDSEngaged = false;
                localAMSMod = 0;
                localAPDSMod = 0;

                // Set up differences between different types of AMS
                boolean isAPDS = counter.isAPDS();
                boolean isAMS = counter.getType().hasFlag(WeaponType.F_AMS) && !isAPDS;
                boolean isAMSBay = counter.getType().hasFlag(WeaponType.F_AMS_BAY);

                // Check the firing arc, even though this was done when the AMS was assigned
                Entity pdEnt = counter.getEntity();
                boolean isInArc;

                // For MRM Saturation, we only need to consider if the counter carrier has arc to the firing
                // unit, as every possible counter should also be considered a defender.
                isInArc = ComputeArc.isInArc(game, pdEnt.getId(), pdEnt.getEquipmentNum(counter), attackingEntity);

                if (!isInArc) {
                    continue;
                }

                // Point defenses can't fire if they're not ready for any other reason
                if (counter.getType() == null
                      || !counter.isReady() || counter.isMissing()
                      // no AMS when a shield in the AMS location
                      || (pdEnt.hasShield() && pdEnt.hasRaisedShield(counter.getLocation(), false))
                      // shutdown means no AMS
                      || pdEnt.isShutDown()) {
                    continue;
                }

                // If we're an AMSBay, heat and ammo must be calculated differently
                if (isAMSBay) {
                    for (WeaponMounted bayW : counter.getBayWeapons()) {
                        AmmoMounted bayWAmmo = bayW.getLinkedAmmo();
                        // For AMS bays, continue until we find an individual AMS that hasn't shot yet
                        if (bayW.isUsedThisRound()) {
                            continue;
                        }

                        // build up some heat (assume target is ams owner)
                        if (bayW.getType().hasFlag(WeaponType.F_HEAT_AS_DICE)) {
                            pdEnt.heatBuildup += Compute.d6(bayW.getCurrentHeat());
                        } else {
                            pdEnt.heatBuildup += bayW.getCurrentHeat();
                        }

                        // decrement the ammo
                        if (bayWAmmo != null) {
                            bayWAmmo.setShotsLeft(Math.max(0, bayWAmmo.getBaseShotsLeft() - 1));
                        }

                        // Optional rule to allow multiple AMS shots per round
                        if (!multiAMS) {
                            // set the ams as having fired, which is checked by isReady()
                            bayW.setUsedThisRound(true);
                        }
                        localAMSEngaged = true;
                    }
                } else {
                    // build up some heat
                    if (counter.getType().hasFlag(WeaponType.F_HEAT_AS_DICE)) {
                        pdEnt.heatBuildup += Compute.d6(counter.getCurrentHeat());
                    } else {
                        pdEnt.heatBuildup += counter.getCurrentHeat();
                    }

                    // decrement the ammo
                    Mounted<?> mAmmo = counter.getLinked();
                    if (mAmmo != null) {
                        mAmmo.setShotsLeft(Math.max(0, mAmmo.getBaseShotsLeft() - 1));
                    }

                    // Can we fire the AMS multiple times?
                    if (!multiAMS && !Game.rulesManager.getRulesEquipment().getAMSMultiShot()) {
                        // set the ams as having fired
                        counter.setUsedThisRound(true);
                    }

                    if (Game.rulesManager.getRulesEquipment().getAMSMultiShot()) {
                        if (!multiAMS && !isAMS) {
                            counter.setUsedThisRound(true);
                        }
                        if (isAMS && counter.isAMSused()) {
                            // Second AMS shot
                            counter.setUsedThisRound(true);
                        } else if (isAMS && !counter.isAMSused()) {
                            // First AMS shot, set it to used.
                            counter.setAMSused(true);
                        }
                    }

                    if (isAMS) {
                        localAMSEngaged = true;
                    }

                    if (isAPDS) {
                        localAPDSEngaged = true;
                    }
                }
                // Determine APDS mod
                if (localAPDSEngaged) {
                    int dist = target.getPosition().distance(pdEnt.getPosition());
                    int minApdsMod = -4;
                    if (pdEnt instanceof BattleArmor) {
                        int numTroopers = ((BattleArmor) pdEnt).getNumberActiveTroopers();
                        minApdsMod = switch (numTroopers) {
                            case 1 -> -2;
                            case 2, 3 -> -3;
                            default -> // 4+
                                  -4;
                        };
                    }
                    localAPDSMod = Math.min(minApdsMod + dist, 0);
                }
                // Determine AMS modifier and report
                if (localAMSEngaged) {
                    Report r = new Report(3350);
                    r.subject = pdEnt.getId();
                    r.newlines = 0;
                    vPhaseReport.add(r);
                    localAMSMod = -4;
                }

                // Report APDS fire. Effect relies on internal variables and must be separated
                // above
                if (localAPDSEngaged) {
                    Report r = new Report(3351);
                    r.subject = pdEnt.getId();
                    r.add(localAPDSMod);
                    r.newlines = 0;
                    vPhaseReport.add(r);
                }
                // Accumulate AMS / APDS engagement; count any number above zero as "engaged"
                amsEngaged |= localAMSEngaged;
                apdsEngaged |= localAPDSEngaged;
                amsMod += localAMSMod;
                apdsMod += localAPDSMod;
            }
        }
        return apdsMod + amsMod;
    }

    /**
     * Overridden calcHits to ensure assignAMS() gets called with this as the only attack to check.
     *
     * @param vPhaseReport - the <code>Vector</code> containing the phase report.
     *
     * @return int      number of missiles that will hit (total AE damage calculated subsequently)
     */
    @Override
    protected int calcHits(Vector<Report> vPhaseReport) {
        // add Cluster mods, depending on options
        int nMissilesModifier = getClusterModifiers(true);

        // Explicitly call assignAMS now to force assignments, as this attack won't be picked up normally
        gameManager.assignAMS(new Vector<>(List.of(this)), false);

        // Saturation mode reduces by AMS before calculating AE damage.
        nMissilesModifier += getAMSHitsMod(vPhaseReport);

        // No need for artificial Streak calcs as this type of attack can only target a hex
        int missilesHit = Compute.missilesHit(weaponType.getRackSize(), nMissilesModifier,
              weapon.isHotLoaded(), false, isAdvancedAMS());

        return missilesHit;
    }
}

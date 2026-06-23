/*
 * Copyright (C) 2025 The MegaMek Team. All Rights Reserved.
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

package megamek.server.totalWarfare;

import java.awt.Color;
import java.util.Vector;

import megamek.MMConstants;
import megamek.client.ui.clientGUI.GUIPreferences;
import megamek.common.CriticalSlot;
import megamek.common.Hex;
import megamek.common.Report;
import megamek.common.compute.Compute;
import megamek.common.equipment.AmmoType;
import megamek.common.equipment.ArmorType;
import megamek.common.equipment.EquipmentMode;
import megamek.common.equipment.MiscType;
import megamek.common.equipment.Mounted;
import megamek.common.options.OptionsConstants;
import megamek.common.rolls.PilotingRollData;
import megamek.common.rolls.Roll;
import megamek.common.rolls.TargetRoll;
import megamek.common.units.Aero;
import megamek.common.units.ConvFighter;
import megamek.common.units.Dropship;
import megamek.common.units.Entity;
import megamek.common.units.FighterSquadron;
import megamek.common.units.Jumpship;
import megamek.common.units.Mek;
import megamek.common.units.Terrains;
import megamek.logging.MMLogger;
import megamek.server.ServerHelper;

class HeatResolver extends AbstractTWRuleHandler {

    private static final MMLogger LOGGER = MMLogger.create(HeatResolver.class);

    HeatResolver(TWGameManager gameManager) {
        super(gameManager);
    }

    /**
     * Each mek sinks the amount of heat appropriate to its current heat capacity.
     */
    void resolveHeat() {
        Report report;
        // Heat phase header
        addReport(new Report(5000, Report.PUBLIC));
        for (Entity entity : getGame().inGameTWEntities()) {
            if (!getGame().hasBoardLocation(entity.getPosition(), entity.getBoardId()) && !entity.isAero()) {
                continue;
            }

            int hotDogMod = 0;
            if (entity.hasAbility(OptionsConstants.PILOT_HOT_DOG)) {
                hotDogMod = 1;
            }
            if (entity.getTaserInterferenceHeat()) {
                entity.heatBuildup += 5;
            }
            boolean hasEMPInterferenceHeat = entity.hasEMPInterferenceHeat();
            if (hasEMPInterferenceHeat) {
                entity.heatBuildup += 5;
            }
            if (entity.hasDamagedRHS() && entity.weaponFired()) {
                entity.heatBuildup += 1;
            }
            if ((entity instanceof Mek) && ((Mek) entity).hasDamagedCoolantSystem() && entity.weaponFired()) {
                entity.heatBuildup += 1;
            }

            int radicalHSBonus = 0;
            Vector<Report> rhsReports = new Vector<>();
            Vector<Report> heatEffectsReports = new Vector<>();

            // Report EMP interference heat and duration (must be after heatEffectsReports is created)
            // Only for Meks/Aero - non-Meks handle this separately before their continue
            if (hasEMPInterferenceHeat) {
                report = new Report(2574);
                report.subject = entity.getId();
                heatEffectsReports.add(report);

                // Also report interference duration (only when heat applies, i.e., Meks/Aero)
                if (entity.getEMPInterferenceRounds() > 0) {
                    report = new Report(2575);
                    report.subject = entity.getId();
                    report.add(entity.getShortName(), true);
                    report.add(entity.getEMPInterferenceRounds());
                    heatEffectsReports.add(report);
                }
            }

            if (entity.hasActivatedRadicalHS()) {
                if (entity instanceof Mek) {
                    radicalHSBonus = ((Mek) entity).getActiveSinks();
                } else if (entity instanceof Aero) {
                    radicalHSBonus = ((Aero) entity).getHeatSinks();
                } else {
                    LOGGER.error("Radical heat sinks mounted on non-mek, non-aero Entity!");
                }

                // Mark that RHS was used this turn (for tracking consecutive uses in newRound)
                entity.setUsedRHSLastTurn(true);

                // RHS activation report
                report = new Report(5540);
                report.subject = entity.getId();
                report.indent();
                report.addDesc(entity);
                report.add(radicalHSBonus);
                rhsReports.add(report);

                Roll diceRoll = Compute.rollD6(2);
                // Increment consecutive RHS uses for this activation attempt (success or failure),
                // then look up the target number based on the updated count.
                entity.setConsecutiveRHSUses(entity.getConsecutiveRHSUses() + 1);
                int targetNumber = ServerHelper.radicalHeatSinkSuccessTarget(entity.getConsecutiveRHSUses());
                boolean rhsFailure = diceRoll.getIntValue() < targetNumber;

                report = new Report(5541);
                report.indent(2);
                report.subject = entity.getId();
                report.add(targetNumber);
                report.add(diceRoll);
                report.choose(rhsFailure);
                rhsReports.add(report);

                // Show RHS stress level and next activation TN (only if RHS didn't fail)
                if (!rhsFailure) {
                    int nextTargetNumber = ServerHelper.radicalHeatSinkSuccessTarget(entity.getConsecutiveRHSUses()
                          + 1);
                    report = new Report(5547);
                    report.indent(2);
                    report.subject = entity.getId();
                    report.add(entity.getConsecutiveRHSUses());
                    report.add(nextTargetNumber);
                    rhsReports.add(report);
                }

                if (rhsFailure) {
                    entity.setHasDamagedRHS(true);
                    int loc = Entity.LOC_NONE;
                    for (Mounted<?> m : entity.getEquipment()) {
                        if (m.getType().hasFlag(MiscType.F_RADICAL_HEATSINK)) {
                            loc = m.getLocation();
                            m.setDestroyed(true);
                            break;
                        }
                    }
                    if (loc == Entity.LOC_NONE) {
                        throw new IllegalStateException("Server.resolveHeat(): " +
                              "Could not find Radical Heat Sink mount on unit that used RHS!");
                    }
                    for (int s = 0; s < entity.getNumberOfCriticalSlots(loc); s++) {
                        CriticalSlot slot = entity.getCritical(loc, s);
                        if ((slot.getType() == CriticalSlot.TYPE_EQUIPMENT) &&
                              slot.getMount().getType().hasFlag(MiscType.F_RADICAL_HEATSINK)) {
                            slot.setHit(true);
                            break;
                        }
                    }
                }
            } else if (entity.hasWorkingRadicalHS() && (entity.getConsecutiveRHSUses() > 0)) {
                // RHS not activated this turn, but has stress - show settling message
                int currentStress = entity.getConsecutiveRHSUses();
                // Calculate reduced stress: single decrement, plus extra if rhsWentUp (double decrement)
                int decrement = entity.hasRHSWentUp() ? 2 : 1;
                int reducedStress = Math.max(0, currentStress - decrement);
                // If activated next turn, stress will increment from reduced level
                int nextActivationTN = ServerHelper.radicalHeatSinkSuccessTarget(reducedStress + 1);
                report = new Report(5548);
                report.indent();
                report.subject = entity.getId();
                report.add(currentStress);
                report.add(reducedStress);
                report.add(nextActivationTN);
                rhsReports.add(report);
            }

            Hex entityHex = getGame().getHex(entity.getPosition(), entity.getBoardId());
            if (entity.tracksHeat() &&
                  (entityHex != null) &&
                  entityHex.containsTerrain(Terrains.FIRE) &&
                  (entityHex.getFireTurn() > 0) &&
                  (entity.getElevation() <= 1) &&
                  (entity.getAltitude() == 0)) {
                int heatToAdd = 5;
                boolean isMekWithHeatDissipatingArmor = (entity instanceof Mek) &&
                      ((Mek) entity).hasIntactHeatDissipatingArmor();
                if (isMekWithHeatDissipatingArmor) {
                    heatToAdd /= 2;
                }
                entity.heatFromExternal += heatToAdd;
                report = new Report(5030);
                report.add(heatToAdd);
                report.subject = entity.getId();
                heatEffectsReports.add(report);
                if (isMekWithHeatDissipatingArmor) {
                    report = new Report(5550);
                    heatEffectsReports.add(report);
                }
            }

            // put in ASF heat build-up first because there are few differences
            if (entity instanceof Aero && !(entity instanceof ConvFighter)) {
                resolveAeroHeat(entity,
                      gameManager.getMainPhaseReport(),
                      rhsReports,
                      radicalHSBonus,
                      hotDogMod);
                continue;
            }

            // heat doesn't matter for non-meks
            if (!(entity instanceof Mek mek)) {
                entity.heat = 0;
                entity.heatBuildup = 0;
                entity.heatFromExternal = 0;
                entity.coolFromExternal = 0;

                if (entity.infernos.isStillBurning()) {
                    gameManager.doFlamingDamage(entity, entity.getPosition());
                }
                if (entity.getTaserShutdownRounds() == 0) {
                    entity.setBATaserShutdown(false);
                    if (entity.isShutDown() &&
                          !entity.isManualShutdown() &&
                          (entity.getTsempEffect() != MMConstants.TSEMP_EFFECT_SHUTDOWN) &&
                          (entity.getEMPShutdownRounds() == 0)) {
                        entity.setShutDown(false);
                        report = new Report(5045);
                        report.subject = entity.getId();
                        report.addDesc(entity);
                        heatEffectsReports.add(report);
                    }
                } else if (entity.isBATaserShutdown()) {
                    // if we're shutdown by a BA taser, we might activate again
                    int roll = Compute.d6(2);
                    if (roll >= 8) {
                        entity.setTaserShutdownRounds(0);
                        if (!(entity.isManualShutdown())) {
                            entity.setShutDown(false);
                        }
                        entity.setBATaserShutdown(false);
                    }
                }

                // Report remaining EMP shutdown turns (from EMP mine effect)
                if (entity.isShutDown() && (entity.getEMPShutdownRounds() > 0)) {
                    report = new Report(2573);
                    report.subject = entity.getId();
                    report.add(entity.getShortName(), true);
                    report.add(entity.getEMPShutdownRounds());
                    heatEffectsReports.add(report);
                }

                // Report remaining EMP interference turns (from EMP mine effect)
                if (entity.getEMPInterferenceRounds() > 0) {
                    report = new Report(2575);
                    report.subject = entity.getId();
                    report.add(entity.getShortName(), true);
                    report.add(entity.getEMPInterferenceRounds());
                    heatEffectsReports.add(report);
                }

                // Add any heat effects reports for non-Meks before continuing
                gameManager.getMainPhaseReport().addAll(heatEffectsReports);

                continue;
            }

            // Only Meks after this point

            // Meks gain heat from inferno hits.
            if (entity.infernos.isStillBurning()) {
                int infernoHeat = entity.infernos.getHeat();
                entity.heatFromExternal += infernoHeat;
                report = new Report(5010);
                report.subject = entity.getId();
                report.add(infernoHeat);
                heatEffectsReports.add(report);
            }

            // should we even bother for this mek?
            if (entity.isDestroyed() || entity.isDoomed() || entity.getCrew().isDoomed() || entity.getCrew().isDead()) {
                continue;
            }

            // engine hits add a lot of heat, provided the engine is on
            entity.heatBuildup += entity.getEngineCritHeat();

            // If a Mek had an active Stealth suite, add 10 heat.
            if (entity.isStealthOn()) {
                entity.heatBuildup += ArmorType.STEALTH_ARMOR_HEAT;
                report = new Report(5015);
                report.subject = entity.getId();
                heatEffectsReports.add(report);
            }

            // Greg: Nova CEWS If a Mek had an active Nova suite, add 2 heat.
            if (entity.hasActiveNovaCEWS()) {
                entity.heatBuildup += 2;
                report = new Report(5013);
                report.subject = entity.getId();
                heatEffectsReports.add(report);
            }

            // void sig adds 10 heat
            if (entity.isVoidSigOn()) {
                entity.heatBuildup += 10;
                report = new Report(5016);
                report.subject = entity.getId();
                heatEffectsReports.add(report);
            }

            // null sig adds 10 heat
            if (entity.isNullSigOn()) {
                entity.heatBuildup += 10;
                report = new Report(5017);
                report.subject = entity.getId();
                heatEffectsReports.add(report);
            }

            // chameleon polarization field adds 6
            if (entity.isChameleonShieldOn()) {
                entity.heatBuildup += 6;
                report = new Report(5014);
                report.subject = entity.getId();
                heatEffectsReports.add(report);
            }

            // If a Mek is in extreme Temperatures, add or subtract one
            // heat per 10 degrees (or fraction of 10 degrees) above or
            // below 50 or -30 degrees Celsius
            adjustHeatExtremeTemp(entity, gameManager.getMainPhaseReport());

            // Add +5 Heat if the hex you're in is on fire
            // and was on fire for the full round.
            if (entityHex != null) {
                int magma = entityHex.terrainLevel(Terrains.MAGMA);
                if ((magma > 0) && (entity.getElevation() == 0)) {
                    int heatToAdd = 5 * magma;
                    if (mek.hasIntactHeatDissipatingArmor()) {
                        heatToAdd /= 2;
                    }
                    entity.heatFromExternal += heatToAdd;
                    report = new Report(5032);
                    report.subject = entity.getId();
                    report.add(heatToAdd);
                    heatEffectsReports.add(report);
                    if (mek.hasIntactHeatDissipatingArmor()) {
                        report = new Report(5550);
                        heatEffectsReports.add(report);
                    }
                }
            }

            // Check the mek for vibroblades if so then check to see if any
            // are active and what heat they will produce.
            if (entity.hasVibroblades()) {
                int vibroHeat;

                vibroHeat = entity.getActiveVibrobladeHeat(Mek.LOC_RIGHT_ARM);
                vibroHeat += entity.getActiveVibrobladeHeat(Mek.LOC_LEFT_ARM);

                if (vibroHeat > 0) {
                    report = new Report(5018);
                    report.subject = entity.getId();
                    report.add(vibroHeat);
                    heatEffectsReports.add(report);
                    entity.heatBuildup += vibroHeat;
                }
            }

            int capHeat = 0;
            for (Mounted<?> m : entity.getEquipment()) {
                if ((m.hasChargedOrChargingCapacitor() == 1) && !m.isUsedThisRound()) {
                    capHeat += 5;
                }
                if ((m.hasChargedOrChargingCapacitor() == 2) && !m.isUsedThisRound()) {
                    capHeat += 10;
                }
            }
            if (capHeat > 0) {
                report = new Report(5019);
                report.subject = entity.getId();
                report.add(capHeat);
                heatEffectsReports.add(report);
                entity.heatBuildup += capHeat;
            }

            // Add heat from external sources to the heat buildup
            int max_ext_heat = getGame().getOptions().intOption(OptionsConstants.ADVANCED_COMBAT_MAX_EXTERNAL_HEAT);
            // Check Game Options
            if (max_ext_heat < 0) {
                max_ext_heat = 15; // standard value specified in TW p.159
            }
            entity.heatBuildup += Math.min(max_ext_heat, entity.heatFromExternal);
            entity.heatFromExternal = 0;
            // remove heat we cooled down
            entity.heatBuildup -= Math.min(9, entity.coolFromExternal);
            entity.coolFromExternal = 0;

            // Combat computers help manage heat
            if (entity.hasQuirk(OptionsConstants.QUIRK_POS_COMBAT_COMPUTER)) {
                int reduce = Math.min(entity.heatBuildup, 4);
                report = new Report(5026);
                report.subject = entity.getId();
                report.add(reduce);
                heatEffectsReports.add(report);
                entity.heatBuildup -= reduce;
            }

            if (entity.hasQuirk(OptionsConstants.QUIRK_NEG_FLAWED_COOLING) && mek.isCoolingFlawActive()) {
                int flaw = 5;
                report = new Report(5021);
                report.subject = entity.getId();
                report.add(flaw);
                heatEffectsReports.add(report);
                entity.heatBuildup += flaw;
            }
            // if heat build up is negative due to temperature, set it to 0
            // for prettier turn reports
            if (entity.heatBuildup < 0) {
                entity.heatBuildup = 0;
            }

            // add the heat we've built up so far.
            entity.heat += entity.heatBuildup;

            // how much heat can we sink?
            int toSink = entity.getHeatCapacityWithWater() + radicalHSBonus;

            if (getGame().getOptions().booleanOption(OptionsConstants.ADVANCED_COMBAT_TAC_OPS_COOLANT_FAILURE) &&
                  entity.getCoolantFailureAmount() > 0) {
                int failureAmount = entity.getCoolantFailureAmount();
                report = new Report(5520);
                report.subject = entity.getId();
                report.add(failureAmount);
                heatEffectsReports.add(report);
            }

            // should we use a coolant pod?
            int safeHeat = entity.hasInfernoAmmo() ? 9 : 13;
            int possibleSinkage = mek.getNumberOfSinks();
            for (Mounted<?> m : entity.getEquipment()) {
                if (m.getType() instanceof AmmoType at) {
                    if ((at.getAmmoType() == AmmoType.AmmoTypeEnum.COOLANT_POD) && m.isAmmoUsable()) {
                        EquipmentMode mode = m.curMode();
                        if (mode.equals("dump")) {
                            report = new Report(5260);
                            report.subject = entity.getId();
                            heatEffectsReports.add(report);
                            m.setShotsLeft(0);
                            toSink += possibleSinkage;
                            break;
                        }
                        if (mode.equals("safe") && ((entity.heat - toSink) > safeHeat)) {
                            report = new Report(5265);
                            report.subject = entity.getId();
                            heatEffectsReports.add(report);
                            m.setShotsLeft(0);
                            toSink += possibleSinkage;
                            break;
                        }
                        if (mode.equals("efficient") && ((entity.heat - toSink) >= possibleSinkage)) {
                            report = new Report(5270);
                            report.subject = entity.getId();
                            heatEffectsReports.add(report);
                            m.setShotsLeft(0);
                            toSink += possibleSinkage;
                            break;
                        }
                    }
                }
            }

            toSink = Math.min(toSink, entity.heat);
            entity.heat -= toSink;
            report = new Report(5035);
            report.subject = entity.getId();
            report.addDesc(entity);
            report.add(entity.heatBuildup);
            report.add(toSink);
            Color color = GUIPreferences.getInstance().getColorForHeat(entity.heat, Color.BLACK);
            report.add(Report.bold(report.fgColor(color, String.valueOf(entity.heat))));
            addReport(report);
            entity.heatBuildup = 0;
            gameManager.getMainPhaseReport().addAll(rhsReports);
            gameManager.getMainPhaseReport().addAll(heatEffectsReports);

            // Does the unit have inferno ammo?
            if (entity.hasInfernoAmmo()) {

                // Roll for possible inferno ammo explosion.
                if (entity.heat >= 10) {
                    int boom = (4 +
                          (entity.heat >= 14 ? 2 : 0) +
                          (entity.heat >= 19 ? 2 : 0) +
                          (entity.heat >= 23 ? 2 : 0) +
                          (entity.heat >= 28 ? 2 : 0)) - hotDogMod;
                    Roll diceRoll = Compute.rollD6(2);
                    int rollValue = diceRoll.getIntValue();
                    report = new Report(5040);
                    report.subject = entity.getId();
                    report.addDesc(entity);
                    report.add(boom);
                    if (entity.getCrew().hasActiveTechOfficer()) {
                        rollValue += 2;
                        String rollCalc = rollValue + " [" + diceRoll.getIntValue() + " + 2]";
                        report.addDataWithTooltip(rollCalc, diceRoll.getReport());
                    } else {
                        report.add(diceRoll);
                    }

                    if (rollValue >= boom) {
                        // avoided
                        report.choose(true);
                        addReport(report);
                    } else {
                        report.choose(false);
                        addReport(report);
                        addReport(gameManager.explodeInfernoAmmoFromHeat(entity));
                    }
                }
            } // End avoid-inferno-explosion
            int autoShutDownHeat;
            boolean mtHeat;

            if (getGame().getOptions().booleanOption(OptionsConstants.ADVANCED_COMBAT_TAC_OPS_HEAT)) {
                autoShutDownHeat = 50;
                mtHeat = true;
            } else {
                autoShutDownHeat = 30;
                mtHeat = false;
            }
            // heat effects: start up
            if ((entity.heat < autoShutDownHeat) && entity.isShutDown() && !entity.isStalled()) {
                if ((entity.getTaserShutdownRounds() == 0) &&
                      (entity.getTsempEffect() != MMConstants.TSEMP_EFFECT_SHUTDOWN) &&
                      (entity.getEMPShutdownRounds() == 0)) {
                    if ((entity.heat < 14) && !(entity.isManualShutdown())) {
                        // automatically starts up again
                        entity.setShutDown(false);
                        report = new Report(5045);
                        report.subject = entity.getId();
                        report.addDesc(entity);
                        addReport(report);
                    } else if (!(entity.isManualShutdown())) {
                        // If the pilot is KO, and we need to roll, auto-fail.
                        if (!entity.getCrew().isActive()) {
                            report = new Report(5049);
                            report.subject = entity.getId();
                            report.addDesc(entity);
                        } else {
                            // roll for startup
                            int startup = (4 + (((entity.heat - 14) / 4) * 2)) - hotDogMod;
                            if (mtHeat) {
                                startup -= 5;
                                switch (entity.getCrew().getPiloting()) {
                                    case 0:
                                    case 1:
                                        startup -= 2;
                                        break;
                                    case 2:
                                    case 3:
                                        startup -= 1;
                                        break;
                                    case 6:
                                    case 7:
                                        startup += 1;
                                }
                            }
                            Roll diceRoll = Compute.rollD6(2);
                            report = new Report(5050);
                            report.subject = entity.getId();
                            report.addDesc(entity);
                            report.add(startup);
                            report.add(diceRoll);

                            if (diceRoll.getIntValue() >= startup) {
                                // start 'er back up
                                entity.setShutDown(false);
                                report.choose(true);
                            } else {
                                report.choose(false);
                            }
                        }
                        addReport(report);
                    }
                } else {
                    // if we're shutdown by a BA taser, we might activate
                    // again
                    if (entity.isBATaserShutdown()) {
                        int roll = Compute.d6(2);
                        if (roll >= 7) {
                            entity.setTaserShutdownRounds(0);
                            if (!(entity.isManualShutdown())) {
                                entity.setShutDown(false);
                            }
                            entity.setBATaserShutdown(false);
                        }
                    }

                    // Report remaining EMP shutdown turns (from EMP mine effect)
                    if (entity.isShutDown() && (entity.getEMPShutdownRounds() > 0)) {
                        report = new Report(2573);
                        report.subject = entity.getId();
                        report.add(entity.getShortName(), true);
                        report.add(entity.getEMPShutdownRounds());
                        addReport(report);
                    }
                    // Note: EMP interference duration is reported earlier with heat effects
                }
            }

            // heat effects: shutdown!
            // Don't shut down if you just restarted.
            else if ((entity.heat >= 14) && !entity.isShutDown()) {
                if (entity.heat >= autoShutDownHeat) {
                    report = new Report(5055);
                    report.subject = entity.getId();
                    report.addDesc(entity);
                    addReport(report);
                    // add a piloting roll and resolve immediately
                    if (entity.canFall()) {
                        getGame().addPSR(new PilotingRollData(entity.getId(), 3, "reactor shutdown"));
                        addReport(gameManager.resolvePilotingRolls());
                    }
                    // okay, now mark shut down
                    entity.setShutDown(true);
                } else {
                    // Again, pilot KO means shutdown is automatic.
                    if (!entity.getCrew().isActive()) {
                        report = new Report(5056);
                        report.subject = entity.getId();
                        report.addDesc(entity);
                        addReport(report);
                        entity.setShutDown(true);
                    } else if ((report = createTCPShutdownAvoidanceReport(entity)) != null) {
                        addReport(report);
                        // No shutdown - TCP automatically avoids
                    } else {
                        int shutdown = (4 + (((entity.heat - 14) / 4) * 2)) - hotDogMod;
                        TargetRoll target;
                        if (mtHeat) {
                            shutdown -= 5;
                            target = new TargetRoll(shutdown, "Base TacOps shutdown TN");
                            switch (entity.getCrew().getPiloting()) {
                                case 0:
                                case 1:
                                    target.addModifier(-2, "Piloting skill");
                                    break;
                                case 2:
                                case 3:
                                    target.addModifier(-1, "Piloting skill");
                                    break;
                                case 6:
                                case 7:
                                    target.addModifier(+1, "Piloting skill");
                            }
                        } else {
                            target = new TargetRoll(shutdown, "Base shutdown TN");
                        }
                        if (mek.hasRiscHeatSinkOverrideKit()) {
                            target.addModifier(-2, "RISC Heat Sink Override Kit");
                        }
                        Roll diceRoll = Compute.rollD6(2);
                        int rollValue = diceRoll.getIntValue();
                        report = new Report(5060);
                        report.subject = entity.getId();
                        report.addDesc(entity);
                        report.add(target);

                        if (entity.getCrew().hasActiveTechOfficer()) {
                            rollValue += 2;
                            String rollCalc = rollValue + " [" + diceRoll.getIntValue() + "]";
                            report.addDataWithTooltip(rollCalc, diceRoll.getReport());
                        } else {
                            report.add(diceRoll);
                        }

                        if (rollValue >= target.getValue()) {
                            // avoided
                            report.choose(true);
                            addReport(report);
                        } else {
                            // shutting down...
                            report.choose(false);
                            addReport(report);
                            // add a piloting roll and resolve immediately
                            if (entity.canFall()) {
                                getGame().addPSR(new PilotingRollData(entity.getId(), 3, "reactor shutdown"));
                                addReport(gameManager.resolvePilotingRolls());
                            }
                            // okay, now mark shut down
                            entity.setShutDown(true);
                        }

                        if (diceRoll.getIntValue() == 2 && mek.hasRiscHeatSinkOverrideKit()) {
                            report = new Report(5545);
                            report.subject(entity.getId());
                            addReport(report);

                            int hits = 0;
                            Roll diceRoll2 = Compute.rollD6(2);
                            report = new Report(6310);
                            report.subject = entity.getId();
                            report.add(diceRoll2);
                            report.newlines = 0;
                            addReport(report);

                            if ((diceRoll2.getIntValue() == 8) || (diceRoll2.getIntValue() == 9)) {
                                hits = 1;
                            } else if ((diceRoll2.getIntValue() == 10) || (diceRoll2.getIntValue() == 11)) {
                                hits = 2;
                            } else if (diceRoll2.getIntValue() == 12) {
                                hits = 3;
                            }

                            report = new Report(6328);
                            report.subject = entity.getId();
                            report.add("%d+1=%d".formatted(hits, hits + 1));
                            addReport(report);

                            hits++;

                            for (int j = 0; (j < 12) && (hits > 0); j++) {
                                var crit = mek.getCritical(Mek.LOC_CENTER_TORSO, j);
                                if ((crit != null) &&
                                      (crit.getType() == CriticalSlot.TYPE_SYSTEM) &&
                                      (crit.getIndex() == Mek.SYSTEM_ENGINE) &&
                                      crit.isHittable()) {
                                    addReport(gameManager.applyCriticalHit(entity,
                                          Mek.LOC_CENTER_TORSO,
                                          crit,
                                          true,
                                          0,
                                          false));
                                    hits--;
                                }
                            }
                        }
                    }
                }
            }

            // LAMs in fighter mode need to check for random movement due to heat
            gameManager.checkRandomAeroMovement(entity, hotDogMod);

            // heat effects: ammo explosion!
            if (entity.heat >= 19) {
                int boom = (4 + (entity.heat >= 23 ? 2 : 0) + (entity.heat >= 28 ? 2 : 0)) - hotDogMod;
                if (mtHeat) {
                    boom += (entity.heat >= 35 ? 2 : 0) + (entity.heat >= 40 ? 2 : 0) + (entity.heat >= 45 ? 2 : 0);
                    // Last line is a crutch; 45 heat should be no roll
                    // but automatic explosion.
                }
                Roll diceRoll = Compute.rollD6(2);
                int rollValue = diceRoll.getIntValue();
                report = new Report(5065);
                report.subject = entity.getId();
                report.addDesc(entity);
                report.add(boom);
                if (entity.getCrew().hasActiveTechOfficer() && mek.hasLaserHeatSinks()) {
                    rollValue += 3;
                    String rollCalc = rollValue + " [" + diceRoll.getIntValue() + " + 2 + 1]";
                    report.addDataWithTooltip(rollCalc, diceRoll.getReport());
                } else if (entity.getCrew().hasActiveTechOfficer()) {
                    rollValue += 2;
                    String rollCalc = rollValue + " [" + diceRoll.getIntValue() + " + 2]";
                    report.addDataWithTooltip(rollCalc, diceRoll.getReport());
                } else if (mek.hasLaserHeatSinks()) {
                    rollValue += 1;
                    String rollCalc = rollValue + " [" + diceRoll.getIntValue() + " + 1]";
                    report.addDataWithTooltip(rollCalc, diceRoll.getReport());
                } else {
                    report.add(diceRoll);
                }
                if (rollValue >= boom) {
                    // mek is ok
                    report.choose(true);
                    addReport(report);
                } else {
                    // boom!
                    report.choose(false);
                    addReport(report);
                    addReport(gameManager.explodeAmmoFromHeat(entity));
                }
            }

            // heat effects: mekwarrior damage
            // N.B. The pilot may already be dead.
            int lifeSupportCritCount;
            boolean torsoMountedCockpit = mek.getCockpitType() == Mek.COCKPIT_TORSO_MOUNTED;
            if (torsoMountedCockpit) {
                lifeSupportCritCount = entity.getHitCriticalSlots(CriticalSlot.TYPE_SYSTEM,
                      Mek.SYSTEM_LIFE_SUPPORT,
                      Mek.LOC_RIGHT_TORSO);
                lifeSupportCritCount += entity.getHitCriticalSlots(CriticalSlot.TYPE_SYSTEM,
                      Mek.SYSTEM_LIFE_SUPPORT,
                      Mek.LOC_LEFT_TORSO);
            } else {
                lifeSupportCritCount = entity.getHitCriticalSlots(CriticalSlot.TYPE_SYSTEM,
                      Mek.SYSTEM_LIFE_SUPPORT,
                      Mek.LOC_HEAD);
            }
            int damageHeat = entity.heat;
            if (entity.hasQuirk(OptionsConstants.QUIRK_POS_IMP_LIFE_SUPPORT)) {
                damageHeat -= 5;
            }
            if (entity.hasQuirk(OptionsConstants.QUIRK_NEG_POOR_LIFE_SUPPORT)) {
                damageHeat += 5;
            }
            if ((lifeSupportCritCount > 0) &&
                  ((damageHeat >= 15) || (torsoMountedCockpit && (damageHeat > 0))) &&
                  !entity.getCrew().isDead() &&
                  !entity.getCrew().isDoomed() &&
                  !entity.getCrew().isEjected()) {
                int heatLimitDesc = 1;
                int damageToCrew = 0;
                if ((damageHeat >= 47) && mtHeat) {
                    // mekwarrior takes 5 damage
                    heatLimitDesc = 47;
                    damageToCrew = 5;
                } else if ((damageHeat >= 39) && mtHeat) {
                    // mekwarrior takes 4 damage
                    heatLimitDesc = 39;
                    damageToCrew = 4;
                } else if ((damageHeat >= 32) && mtHeat) {
                    // mekwarrior takes 3 damage
                    heatLimitDesc = 32;
                    damageToCrew = 3;
                } else if (damageHeat >= 25) {
                    // mekwarrior takes 2 damage
                    heatLimitDesc = 25;
                    damageToCrew = 2;
                } else if (damageHeat >= 15) {
                    // mekwarrior takes 1 damage
                    heatLimitDesc = 15;
                    damageToCrew = 1;
                }
                if ((mek.getCockpitType() == Mek.COCKPIT_TORSO_MOUNTED) &&
                      !entity.hasAbility(OptionsConstants.MD_PAIN_SHUNT)) {
                    damageToCrew += 1;
                }
                report = new Report(5070);
                report.subject = entity.getId();
                report.addDesc(entity);
                report.add(heatLimitDesc);
                report.add(damageToCrew);
                addReport(report);
                addReport(gameManager.damageCrew(entity, damageToCrew));
            } else if (mtHeat &&
                  (entity.heat >= 32) &&
                  !entity.getCrew().isDead() &&
                  !entity.getCrew().isDoomed() &&
                  !entity.hasAbility(OptionsConstants.MD_PAIN_SHUNT)) {
                // Crew may take damage from heat if MaxTech option is set
                Roll diceRoll = Compute.rollD6(2);
                int avoidNumber;
                if (entity.heat >= 47) {
                    avoidNumber = 12;
                } else if (entity.heat >= 39) {
                    avoidNumber = 10;
                } else {
                    avoidNumber = 8;
                }
                avoidNumber -= hotDogMod;
                report = new Report(5075);
                report.subject = entity.getId();
                report.addDesc(entity);
                report.add(avoidNumber);
                report.add(diceRoll);

                if (diceRoll.getIntValue() >= avoidNumber) {
                    // damage avoided
                    report.choose(true);
                    addReport(report);
                } else {
                    report.choose(false);
                    addReport(report);
                    addReport(gameManager.damageCrew(entity, 1));
                }
            }

            // The pilot may have just expired.
            if ((entity.getCrew().isDead() || entity.getCrew().isDoomed()) && !entity.getCrew().isEjected()) {
                report = new Report(5080);
                report.subject = entity.getId();
                report.addDesc(entity);
                addReport(report);
                addReport(gameManager.destroyEntity(entity, "crew death", true));
            }

            // With MaxTech Heat Scale, there may occur critical damage
            if (mtHeat) {
                if (entity.heat >= 36) {
                    Roll diceRoll = Compute.rollD6(2);
                    int damageNumber;
                    if (entity.heat >= 44) {
                        damageNumber = 10;
                    } else {
                        damageNumber = 8;
                    }
                    damageNumber -= hotDogMod;
                    report = new Report(5085);
                    report.subject = entity.getId();
                    report.addDesc(entity);
                    report.add(damageNumber);
                    report.add(diceRoll);
                    report.newlines = 0;

                    if (diceRoll.getIntValue() >= damageNumber) {
                        report.choose(true);
                    } else {
                        report.choose(false);
                        addReport(report);
                        addReport(gameManager.oneCriticalEntity(entity, Compute.randomInt(8), false, 0));
                        // add an empty report, for line breaking
                        report = new Report(1210, Report.PUBLIC);
                    }
                    addReport(report);
                }
            }

            // If the TacOps coolant failure rule is in use, check for coolant failure. If
            // the
            // heat sink capacity has already been offset by previous coolant failures,
            // further
            // reductions in capacity will have no effect.
            if (getGame().getOptions().booleanOption(OptionsConstants.ADVANCED_COMBAT_TAC_OPS_COOLANT_FAILURE) &&
                  (entity.heat >= 5) &&
                  (entity.getCoolantFailureAmount() < mek.getNumberOfSinks() * (mek.hasDoubleHeatSinks() ? 2 : 1))) {
                Roll diceRoll = Compute.rollD6(2);
                int hitNumber = 10;
                hitNumber -= Math.max(0, (int) Math.ceil(entity.heat / 5.0) - 2);
                report = new Report(5525);
                report.subject = entity.getId();
                report.add(entity.getShortName());
                report.add(hitNumber);
                report.add(diceRoll);
                report.newlines = 0;
                addReport(report);

                if (diceRoll.getIntValue() >= hitNumber) {
                    report = new Report(5052);
                    report.subject = entity.getId();
                    addReport(report);
                    report = new Report(5526);
                    report.subject = entity.getId();
                    report.add(entity.getShortNameRaw());
                    addReport(report);
                    entity.addCoolantFailureAmount(1);
                } else {
                    report = new Report(5041);
                    report.subject = entity.getId();
                    addReport(report);
                }
            }
        }

        if (gameManager.getMainPhaseReport().size() == 1) {
            // I guess nothing happened...
            addReport(new Report(1205, Report.PUBLIC));
        }
    }

    /**
     * Worker function that handles heat as applied to aerospace fighter
     */
    void resolveAeroHeat(Entity entity, Vector<Report> vPhaseReport, Vector<Report> rhsReports,
          int radicalHSBonus, int hotDogMod) {
        Report report;

        // If this aero is part of a squadron, we will deal with its
        // heat with the fighter squadron
        if (getGame().getEntity(entity.getTransportId()) instanceof FighterSquadron) {
            return;
        }

        // should we even bother?
        if (entity.isDestroyed() || entity.isDoomed()
              || entity.getCrew().isDoomed()
              || entity.getCrew().isDead()) {
            return;
        }

        // engine hits add a lot of heat, provided the engine is on
        entity.heatBuildup += entity.getEngineCritHeat();

        // If an Aero had an active Stealth suite, add 10 heat.
        if (entity.isStealthOn()) {
            entity.heatBuildup += 10;
            report = new Report(5015);
            report.subject = entity.getId();
            vPhaseReport.add(report);
        }

        // Report EMP interference heat (heat was already added in main loop)
        if (entity.hasEMPInterferenceHeat()) {
            report = new Report(2574);
            report.subject = entity.getId();
            vPhaseReport.add(report);
        }

        // Add or subtract heat due to extreme temperatures TO:AR p60
        adjustHeatExtremeTemp(entity, vPhaseReport);

        // Combat computers help manage heat
        if (entity.hasQuirk(OptionsConstants.QUIRK_POS_COMBAT_COMPUTER)) {
            int reduce = Math.min(entity.heatBuildup, 4);
            report = new Report(5026);
            report.subject = entity.getId();
            report.add(reduce);
            vPhaseReport.add(report);
            entity.heatBuildup -= reduce;
        }

        // Add heat from external sources to the heat buildup
        int max_ext_heat = getGame().getOptions().intOption(
              OptionsConstants.ADVANCED_COMBAT_MAX_EXTERNAL_HEAT); // Check Game Options
        if (max_ext_heat < 0) {
            max_ext_heat = 15; // standard value specified in TW p.159
        }
        entity.heatBuildup += Math.min(max_ext_heat, entity.heatFromExternal);
        entity.heatFromExternal = 0;
        // remove heat we cooled down
        entity.heatBuildup -= Math.min(9, entity.coolFromExternal);
        entity.coolFromExternal = 0;

        // add the heat we've built up so far.
        entity.heat += entity.heatBuildup;

        // how much heat can we sink?
        int toSink = entity.getHeatCapacityWithWater() + radicalHSBonus;

        // should we use a coolant pod?
        int safeHeat = entity.hasInfernoAmmo() ? 9 : 13;
        int possibleSinkage = ((Aero) entity).getHeatSinks();
        for (Mounted<?> mounted : entity.getEquipment()) {
            if (mounted.getType() instanceof AmmoType at) {
                if ((at.getAmmoType() == AmmoType.AmmoTypeEnum.COOLANT_POD) && mounted.isAmmoUsable()) {
                    EquipmentMode mode = mounted.curMode();
                    if (mode.equals("dump")) {
                        report = new Report(5260);
                        report.subject = entity.getId();
                        vPhaseReport.add(report);
                        mounted.setShotsLeft(0);
                        toSink += possibleSinkage;
                        break;
                    }
                    if (mode.equals("safe") && ((entity.heat - toSink) > safeHeat)) {
                        report = new Report(5265);
                        report.subject = entity.getId();
                        vPhaseReport.add(report);
                        mounted.setShotsLeft(0);
                        toSink += possibleSinkage;
                        break;
                    }
                    if (mode.equals("efficient")
                          && ((entity.heat - toSink) >= possibleSinkage)) {
                        report = new Report(5270);
                        report.subject = entity.getId();
                        vPhaseReport.add(report);
                        mounted.setShotsLeft(0);
                        toSink += possibleSinkage;
                        break;
                    }
                }
            }
        }

        toSink = Math.min(toSink, entity.heat);
        entity.heat -= toSink;
        report = new Report(5035);
        report.subject = entity.getId();
        report.addDesc(entity);
        report.add(entity.heatBuildup);
        report.add(toSink);
        report.add(entity.heat);
        vPhaseReport.add(report);
        entity.heatBuildup = 0;
        vPhaseReport.addAll(rhsReports);

        // add in the effects of heat

        if ((entity instanceof Dropship) || (entity instanceof Jumpship)) {
            // only check for a possible control roll
            if (entity.heat > 0) {
                int bonus = (int) Math.ceil(entity.heat / 100.0);
                getGame().addControlRoll(new PilotingRollData(
                      entity.getId(), bonus, "used too much heat"));
                entity.heat = 0;
            }
            return;
        }

        // Capital fighters can overheat and require control rolls
        if (entity.isCapitalFighter() && (entity.heat > 0)) {
            int penalty = (int) Math.ceil(entity.heat / 15.0);
            getGame().addControlRoll(new PilotingRollData(entity.getId(),
                  penalty, "used too much heat"));
        }

        // Like other large craft, the rest of these rules don't apply
        // to capital fighters
        if (entity.isCapitalFighter()) {
            return;
        }

        int autoShutDownHeat = 30;
        boolean mtHeat = getGame().getOptions().booleanOption(OptionsConstants.ADVANCED_COMBAT_TAC_OPS_HEAT);
        if (mtHeat) {
            autoShutDownHeat = 50;
        }

        // heat effects: start up
        if ((entity.heat < autoShutDownHeat) && entity.isShutDown()) {
            // only start up if not shut down by taser, TSEMP, or EMP mine
            if ((entity.getTaserShutdownRounds() == 0)
                  && (entity.getTsempEffect() != MMConstants.TSEMP_EFFECT_SHUTDOWN)
                  && (entity.getEMPShutdownRounds() == 0)) {
                if ((entity.heat < 14) && !entity.isManualShutdown()) {
                    // automatically starts up again
                    entity.setShutDown(false);
                    report = new Report(5045);
                    report.subject = entity.getId();
                    report.addDesc(entity);
                    vPhaseReport.add(report);
                } else if (!entity.isManualShutdown()) {
                    // If the pilot is KO, and we need to roll, auto-fail.
                    if (!entity.getCrew().isActive()) {
                        report = new Report(5049);
                        report.subject = entity.getId();
                        report.addDesc(entity);
                    } else {
                        // roll for startup
                        int startup = (4 + (((entity.heat - 14) / 4) * 2)) - hotDogMod;
                        if (mtHeat) {
                            startup -= 5;
                            switch (entity.getCrew().getPiloting()) {
                                case 0:
                                case 1:
                                    startup -= 2;
                                    break;
                                case 2:
                                case 3:
                                    startup -= 1;
                                    break;
                                case 6:
                                case 7:
                                    startup += 1;
                                    break;
                            }
                        }
                        Roll diceRoll = entity.getCrew().rollPilotingSkill();
                        report = new Report(5050);
                        report.subject = entity.getId();
                        report.addDesc(entity);
                        report.add(startup);
                        report.add(diceRoll);

                        if (diceRoll.getIntValue() >= startup) {
                            // start 'er back up
                            entity.setShutDown(false);
                            report.choose(true);
                        } else {
                            report.choose(false);
                        }
                    }
                    vPhaseReport.add(report);
                }
            } else {
                // if we're shutdown by a BA taser, we might activate
                // again
                if (entity.isBATaserShutdown()) {
                    int roll = Compute.d6(2);
                    if (roll >= 8) {
                        entity.setTaserShutdownRounds(0);
                        if (!(entity.isManualShutdown())) {
                            entity.setShutDown(false);
                        }
                        entity.setBATaserShutdown(false);
                    }
                }

                // Report remaining EMP shutdown turns (from EMP mine effect)
                if (entity.isShutDown() && (entity.getEMPShutdownRounds() > 0)) {
                    report = new Report(2573);
                    report.subject = entity.getId();
                    report.add(entity.getShortName(), true);
                    report.add(entity.getEMPShutdownRounds());
                    vPhaseReport.add(report);
                }

                // Report remaining EMP interference turns (from EMP mine effect)
                if (entity.getEMPInterferenceRounds() > 0) {
                    report = new Report(2575);
                    report.subject = entity.getId();
                    report.add(entity.getShortName(), true);
                    report.add(entity.getEMPInterferenceRounds());
                    vPhaseReport.add(report);
                }
            }
        }
        // heat effects: shutdown!
        else if ((entity.heat >= 14) && !entity.isShutDown()) {
            if (entity.heat >= autoShutDownHeat) {
                report = new Report(5055);
                report.subject = entity.getId();
                report.addDesc(entity);
                vPhaseReport.add(report);
                // okay, now mark shut down
                entity.setShutDown(true);
            } else {
                // Again, pilot KO means shutdown is automatic.
                if (!entity.getCrew().isActive()) {
                    report = new Report(5056);
                    report.subject = entity.getId();
                    report.addDesc(entity);
                    vPhaseReport.add(report);
                    entity.setShutDown(true);
                } else if ((report = createTCPShutdownAvoidanceReport(entity)) != null) {
                    vPhaseReport.add(report);
                    // No shutdown - TCP automatically avoids
                } else {
                    int shutdown = (4 + (((entity.heat - 14) / 4) * 2)) - hotDogMod;
                    if (mtHeat) {
                        shutdown -= 5;
                        switch (entity.getCrew().getPiloting()) {
                            case 0:
                            case 1:
                                shutdown -= 2;
                                break;
                            case 2:
                            case 3:
                                shutdown -= 1;
                                break;
                            case 6:
                            case 7:
                                shutdown += 1;
                                break;
                        }
                    }
                    Roll diceRoll = Compute.rollD6(2);
                    report = new Report(5060);
                    report.subject = entity.getId();
                    report.addDesc(entity);
                    report.add(shutdown);
                    report.add(diceRoll);

                    if (diceRoll.getIntValue() >= shutdown) {
                        // avoided
                        report.choose(true);
                        vPhaseReport.add(report);
                    } else {
                        // shutting down...
                        report.choose(false);
                        vPhaseReport.add(report);
                        // okay, now mark shut down
                        entity.setShutDown(true);
                    }
                }
            }
        }

        gameManager.checkRandomAeroMovement(entity, hotDogMod);

        // heat effects: ammo explosion!
        if (entity.heat >= 19) {
            int boom = (4 + (entity.heat >= 23 ? 2 : 0) + (entity.heat >= 28 ? 2 : 0))
                  - hotDogMod;
            if (mtHeat) {
                boom += (entity.heat >= 35 ? 2 : 0)
                      + (entity.heat >= 40 ? 2 : 0)
                      + (entity.heat >= 45 ? 2 : 0);
                // Last line is a crutch; 45 heat should be no roll
                // but automatic explosion.
            }
            report = new Report(5065);
            report.subject = entity.getId();
            report.addDesc(entity);
            report.add(boom);

            Roll diceRoll = Compute.rollD6(2);
            int rollValue = diceRoll.getIntValue();
            if (entity.getCrew().hasActiveTechOfficer()) {
                rollValue += 2;
                String rollCalc = rollValue + " [" + diceRoll.getIntValue() + " + 2]";
                report.addDataWithTooltip(rollCalc, diceRoll.getReport());
            } else {
                report.add(diceRoll);
            }

            if (rollValue >= boom) {
                // no ammo explosion
                report.choose(true);
                vPhaseReport.add(report);
            } else {
                // boom!
                report.choose(false);
                vPhaseReport.add(report);
                vPhaseReport.addAll(gameManager.explodeAmmoFromHeat(entity));
            }
        }

        // heat effects: pilot damage
        if (entity.heat >= 21) {
            int ouch = (6 + (entity.heat >= 27 ? 3 : 0)) - hotDogMod;
            Roll diceRoll = Compute.rollD6(2);
            report = new Report(5075);
            report.subject = entity.getId();
            report.addDesc(entity);
            report.add(ouch);
            report.add(diceRoll);

            if (diceRoll.getIntValue() >= ouch) {
                // pilot is ok
                report.choose(true);
                vPhaseReport.add(report);
            } else {
                // pilot is hurting
                report.choose(false);
                vPhaseReport.add(report);
                vPhaseReport.addAll(gameManager.damageCrew(entity, 1));
            }
        }

        // The pilot may have just expired.
        if ((entity.getCrew().isDead() || entity.getCrew().isDoomed())
              && !entity.getCrew().isEjected()) {
            report = new Report(5080);
            report.subject = entity.getId();
            report.addDesc(entity);
            vPhaseReport.add(report);
            vPhaseReport.addAll(gameManager.destroyEntity(entity, "pilot death", true));
        }
    }

    void adjustHeatExtremeTemp(Entity entity, Vector<Report> vPhaseReport) {
        Report report;
        int tempDiff = getGame().getPlanetaryConditions().getTemperatureDifference(50, -30);
        boolean heatArmor = false;

        if (entity instanceof Mek) {
            heatArmor = ((Mek) entity).hasIntactHeatDissipatingArmor();
        }

        if (entity.isSpaceborne() || (tempDiff == 0)) {
            return;
        } else {
            if (getGame().getPlanetaryConditions().getTemperature() > 50) {
                int heatToAdd = tempDiff;
                if (heatArmor) {
                    heatToAdd /= 2;
                }
                entity.heatFromExternal += heatToAdd;
                report = new Report(5020);
                report.subject = entity.getId();
                report.add(heatToAdd);
                vPhaseReport.add(report);
                if (heatArmor) {
                    report = new Report(5550);
                    vPhaseReport.add(report);
                }
            } else {
                entity.heatFromExternal -= tempDiff;
                report = new Report(5025);
                report.subject = entity.getId();
                report.add(tempDiff);
                vPhaseReport.add(report);
            }
        }
    }

    /**
     * Creates a report for TCP automatic shutdown avoidance if the entity has a Triple-Core Processor. Per IO pg 81,
     * TCP-implanted MechWarriors and fighter pilots automatically succeed at all shutdown-avoidance checks for extreme
     * heat, except for those described as "automatic shutdown". Note: Unlike aimed shots, TCP heat shutdown avoidance
     * does NOT require VDNI - the implant works standalone for this benefit.
     *
     * @param entity The entity to check
     *
     * @return A Report for TCP shutdown avoidance, or null if entity doesn't have TCP
     */
    private Report createTCPShutdownAvoidanceReport(Entity entity) {
        if (!entity.hasAbility(OptionsConstants.MD_TRIPLE_CORE_PROCESSOR)) {
            return null;
        }
        Report report = new Report(5057);
        report.subject = entity.getId();
        report.addDesc(entity);
        return report;
    }

}

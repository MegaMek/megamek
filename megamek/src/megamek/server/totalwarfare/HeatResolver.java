/*
 * Copyright (c) 2025 - The MegaMek Team. All Rights Reserved.
 *
 * This file is part of MegaMek.
 *
 * MegaMek is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * MegaMek is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with MegaMek. If not, see <http://www.gnu.org/licenses/>.
 */
package megamek.server.totalwarfare;

import megamek.MMConstants;
import megamek.client.ui.swing.GUIPreferences;
import megamek.common.*;
import megamek.common.equipment.ArmorType;
import megamek.common.options.OptionsConstants;
import megamek.logging.MMLogger;
import megamek.server.ServerHelper;

import java.awt.Color;
import java.util.Vector;

public class HeatResolver extends AbstractTWRuleHandler {

    private static final MMLogger LOGGER = MMLogger.create(HeatResolver.class);

    HeatResolver(TWGameManager gameManager) {
        super(gameManager);
    }

    /**
     * Each mek sinks the amount of heat appropriate to its current heat capacity.
     */
    void resolveHeat() {
        Report r;
        // Heat phase header
        addReport(new Report(5000, Report.PUBLIC));
        for (Entity entity : getGame().inGameTWEntities()) {
            if (!getGame().hasBoardLocation(entity.getPosition(),entity.getBoardId()) && !entity.isAero()) {
                continue;
            }

            int hotDogMod = 0;
            if (entity.hasAbility(OptionsConstants.PILOT_HOT_DOG)) {
                hotDogMod = 1;
            }
            if (entity.getTaserInterferenceHeat()) {
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
            if (entity.hasActivatedRadicalHS()) {
                if (entity instanceof Mek) {
                    radicalHSBonus = ((Mek) entity).getActiveSinks();
                } else if (entity instanceof Aero) {
                    radicalHSBonus = ((Aero) entity).getHeatSinks();
                } else {
                    LOGGER.error("Radical heat sinks mounted on non-mek, non-aero Entity!");
                }

                // RHS activation report
                r = new Report(5540);
                r.subject = entity.getId();
                r.indent();
                r.addDesc(entity);
                r.add(radicalHSBonus);
                rhsReports.add(r);

                Roll diceRoll = Compute.rollD6(2);
                entity.setConsecutiveRHSUses(entity.getConsecutiveRHSUses() + 1);
                int targetNumber = ServerHelper.radicalHeatSinkSuccessTarget(entity.getConsecutiveRHSUses());
                boolean rhsFailure = diceRoll.getIntValue() < targetNumber;

                r = new Report(5541);
                r.indent(2);
                r.subject = entity.getId();
                r.add(targetNumber);
                r.add(diceRoll);
                r.choose(rhsFailure);
                rhsReports.add(r);

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
                    for (int s = 0; s < entity.getNumberOfCriticals(loc); s++) {
                        CriticalSlot slot = entity.getCritical(loc, s);
                        if ((slot.getType() == CriticalSlot.TYPE_EQUIPMENT) &&
                                  slot.getMount().getType().hasFlag(MiscType.F_RADICAL_HEATSINK)) {
                            slot.setHit(true);
                            break;
                        }
                    }
                }
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
                r = new Report(5030);
                r.add(heatToAdd);
                r.subject = entity.getId();
                heatEffectsReports.add(r);
                if (isMekWithHeatDissipatingArmor) {
                    r = new Report(5550);
                    heatEffectsReports.add(r);
                }
            }

            // put in ASF heat build-up first because there are few differences
            if (entity instanceof Aero && !(entity instanceof ConvFighter)) {
                ServerHelper.resolveAeroHeat(getGame(),
                      entity,
                      gameManager.getMainPhaseReport(),
                      rhsReports,
                      radicalHSBonus,
                      hotDogMod,
                      gameManager);
                continue;
            }

            // heat doesn't matter for non-meks
            if (!(entity instanceof Mek)) {
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
                              (entity.getTsempEffect() != MMConstants.TSEMP_EFFECT_SHUTDOWN)) {
                        entity.setShutDown(false);
                        r = new Report(5045);
                        r.subject = entity.getId();
                        r.addDesc(entity);
                        heatEffectsReports.add(r);
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

                continue;
            }

            // Only Meks after this point

            // Meks gain heat from inferno hits.
            if (entity.infernos.isStillBurning()) {
                int infernoHeat = entity.infernos.getHeat();
                entity.heatFromExternal += infernoHeat;
                r = new Report(5010);
                r.subject = entity.getId();
                r.add(infernoHeat);
                heatEffectsReports.add(r);
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
                r = new Report(5015);
                r.subject = entity.getId();
                heatEffectsReports.add(r);
            }

            // Greg: Nova CEWS If a Mek had an active Nova suite, add 2 heat.
            if (entity.hasActiveNovaCEWS()) {
                entity.heatBuildup += 2;
                r = new Report(5013);
                r.subject = entity.getId();
                heatEffectsReports.add(r);
            }

            // void sig adds 10 heat
            if (entity.isVoidSigOn()) {
                entity.heatBuildup += 10;
                r = new Report(5016);
                r.subject = entity.getId();
                heatEffectsReports.add(r);
            }

            // null sig adds 10 heat
            if (entity.isNullSigOn()) {
                entity.heatBuildup += 10;
                r = new Report(5017);
                r.subject = entity.getId();
                heatEffectsReports.add(r);
            }

            // chameleon polarization field adds 6
            if (entity.isChameleonShieldOn()) {
                entity.heatBuildup += 6;
                r = new Report(5014);
                r.subject = entity.getId();
                heatEffectsReports.add(r);
            }

            // If a Mek is in extreme Temperatures, add or subtract one
            // heat per 10 degrees (or fraction of 10 degrees) above or
            // below 50 or -30 degrees Celsius
            ServerHelper.adjustHeatExtremeTemp(getGame(), entity, gameManager.getMainPhaseReport());

            // Add +5 Heat if the hex you're in is on fire
            // and was on fire for the full round.
            if (entityHex != null) {
                int magma = entityHex.terrainLevel(Terrains.MAGMA);
                if ((magma > 0) && (entity.getElevation() == 0)) {
                    int heatToAdd = 5 * magma;
                    if (((Mek) entity).hasIntactHeatDissipatingArmor()) {
                        heatToAdd /= 2;
                    }
                    entity.heatFromExternal += heatToAdd;
                    r = new Report(5032);
                    r.subject = entity.getId();
                    r.add(heatToAdd);
                    heatEffectsReports.add(r);
                    if (((Mek) entity).hasIntactHeatDissipatingArmor()) {
                        r = new Report(5550);
                        heatEffectsReports.add(r);
                    }
                }
            }

            // Check the mek for vibroblades if so then check to see if any
            // are active and what heat they will produce.
            if (entity.hasVibroblades()) {
                int vibroHeat;

                vibroHeat = entity.getActiveVibrobladeHeat(Mek.LOC_RARM);
                vibroHeat += entity.getActiveVibrobladeHeat(Mek.LOC_LARM);

                if (vibroHeat > 0) {
                    r = new Report(5018);
                    r.subject = entity.getId();
                    r.add(vibroHeat);
                    heatEffectsReports.add(r);
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
                r = new Report(5019);
                r.subject = entity.getId();
                r.add(capHeat);
                heatEffectsReports.add(r);
                entity.heatBuildup += capHeat;
            }

            // Add heat from external sources to the heat buildup
            int max_ext_heat = getGame().getOptions().intOption(OptionsConstants.ADVCOMBAT_MAX_EXTERNAL_HEAT);
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
                r = new Report(5026);
                r.subject = entity.getId();
                r.add(reduce);
                heatEffectsReports.add(r);
                entity.heatBuildup -= reduce;
            }

            if (entity.hasQuirk(OptionsConstants.QUIRK_NEG_FLAWED_COOLING) && ((Mek) entity).isCoolingFlawActive()) {
                int flaw = 5;
                r = new Report(5021);
                r.subject = entity.getId();
                r.add(flaw);
                heatEffectsReports.add(r);
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

            if (getGame().getOptions().booleanOption(OptionsConstants.ADVCOMBAT_TACOPS_COOLANT_FAILURE) &&
                      entity.getCoolantFailureAmount() > 0) {
                int failureAmount = entity.getCoolantFailureAmount();
                r = new Report(5520);
                r.subject = entity.getId();
                r.add(failureAmount);
                heatEffectsReports.add(r);
            }

            // should we use a coolant pod?
            int safeHeat = entity.hasInfernoAmmo() ? 9 : 13;
            int possibleSinkage = ((Mek) entity).getNumberOfSinks();
            for (Mounted<?> m : entity.getEquipment()) {
                if (m.getType() instanceof AmmoType at) {
                    if ((at.getAmmoType() == AmmoType.T_COOLANT_POD) && m.isAmmoUsable()) {
                        EquipmentMode mode = m.curMode();
                        if (mode.equals("dump")) {
                            r = new Report(5260);
                            r.subject = entity.getId();
                            heatEffectsReports.add(r);
                            m.setShotsLeft(0);
                            toSink += possibleSinkage;
                            break;
                        }
                        if (mode.equals("safe") && ((entity.heat - toSink) > safeHeat)) {
                            r = new Report(5265);
                            r.subject = entity.getId();
                            heatEffectsReports.add(r);
                            m.setShotsLeft(0);
                            toSink += possibleSinkage;
                            break;
                        }
                        if (mode.equals("efficient") && ((entity.heat - toSink) >= possibleSinkage)) {
                            r = new Report(5270);
                            r.subject = entity.getId();
                            heatEffectsReports.add(r);
                            m.setShotsLeft(0);
                            toSink += possibleSinkage;
                            break;
                        }
                    }
                }
            }

            toSink = Math.min(toSink, entity.heat);
            entity.heat -= toSink;
            r = new Report(5035);
            r.subject = entity.getId();
            r.addDesc(entity);
            r.add(entity.heatBuildup);
            r.add(toSink);
            Color color = GUIPreferences.getInstance().getColorForHeat(entity.heat, Color.BLACK);
            r.add(Report.bold(r.fgColor(color, String.valueOf(entity.heat))));
            addReport(r);
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
                    r = new Report(5040);
                    r.subject = entity.getId();
                    r.addDesc(entity);
                    r.add(boom);
                    if (entity.getCrew().hasActiveTechOfficer()) {
                        rollValue += 2;
                        String rollCalc = rollValue + " [" + diceRoll.getIntValue() + " + 2]";
                        r.addDataWithTooltip(rollCalc, diceRoll.getReport());
                    } else {
                        r.add(diceRoll);
                    }

                    if (rollValue >= boom) {
                        // avoided
                        r.choose(true);
                        addReport(r);
                    } else {
                        r.choose(false);
                        addReport(r);
                        addReport(gameManager.explodeInfernoAmmoFromHeat(entity));
                    }
                }
            } // End avoid-inferno-explosion
            int autoShutDownHeat;
            boolean mtHeat;

            if (getGame().getOptions().booleanOption(OptionsConstants.ADVCOMBAT_TACOPS_HEAT)) {
                autoShutDownHeat = 50;
                mtHeat = true;
            } else {
                autoShutDownHeat = 30;
                mtHeat = false;
            }
            // heat effects: start up
            if ((entity.heat < autoShutDownHeat) && entity.isShutDown() && !entity.isStalled()) {
                if ((entity.getTaserShutdownRounds() == 0) &&
                          (entity.getTsempEffect() != MMConstants.TSEMP_EFFECT_SHUTDOWN)) {
                    if ((entity.heat < 14) && !(entity.isManualShutdown())) {
                        // automatically starts up again
                        entity.setShutDown(false);
                        r = new Report(5045);
                        r.subject = entity.getId();
                        r.addDesc(entity);
                        addReport(r);
                    } else if (!(entity.isManualShutdown())) {
                        // If the pilot is KO and we need to roll, auto-fail.
                        if (!entity.getCrew().isActive()) {
                            r = new Report(5049);
                            r.subject = entity.getId();
                            r.addDesc(entity);
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
                            r = new Report(5050);
                            r.subject = entity.getId();
                            r.addDesc(entity);
                            r.add(startup);
                            r.add(diceRoll);

                            if (diceRoll.getIntValue() >= startup) {
                                // start 'er back up
                                entity.setShutDown(false);
                                r.choose(true);
                            } else {
                                r.choose(false);
                            }
                        }
                        addReport(r);
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
                }
            }

            // heat effects: shutdown!
            // Don't shut down if you just restarted.
            else if ((entity.heat >= 14) && !entity.isShutDown()) {
                if (entity.heat >= autoShutDownHeat) {
                    r = new Report(5055);
                    r.subject = entity.getId();
                    r.addDesc(entity);
                    addReport(r);
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
                        r = new Report(5056);
                        r.subject = entity.getId();
                        r.addDesc(entity);
                        addReport(r);
                        entity.setShutDown(true);
                    } else {
                        int shutdown = (4 + (((entity.heat - 14) / 4) * 2)) - hotDogMod;
                        TargetRoll target;
                        if (mtHeat) {
                            shutdown -= 5;
                            target = new TargetRoll(shutdown, "Base tacops shutdown TN");
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
                        Mek mek = (Mek) entity;
                        if (mek.hasRiscHeatSinkOverrideKit()) {
                            target.addModifier(-2, "RISC Heat Sink Override Kit");
                        }
                        Roll diceRoll = Compute.rollD6(2);
                        int rollValue = diceRoll.getIntValue();
                        r = new Report(5060);
                        r.subject = entity.getId();
                        r.addDesc(entity);
                        r.add(target);

                        if (entity.getCrew().hasActiveTechOfficer()) {
                            rollValue += 2;
                            String rollCalc = rollValue + " [" + diceRoll.getIntValue() + "]";
                            r.addDataWithTooltip(rollCalc, diceRoll.getReport());
                        } else {
                            r.add(diceRoll);
                        }

                        if (rollValue >= target.getValue()) {
                            // avoided
                            r.choose(true);
                            addReport(r);
                        } else {
                            // shutting down...
                            r.choose(false);
                            addReport(r);
                            // add a piloting roll and resolve immediately
                            if (entity.canFall()) {
                                getGame().addPSR(new PilotingRollData(entity.getId(), 3, "reactor shutdown"));
                                addReport(gameManager.resolvePilotingRolls());
                            }
                            // okay, now mark shut down
                            entity.setShutDown(true);
                        }

                        if (diceRoll.getIntValue() == 2 && mek.hasRiscHeatSinkOverrideKit()) {
                            r = new Report(5545);
                            r.subject(entity.getId());
                            addReport(r);

                            int hits = 0;
                            Roll diceRoll2 = Compute.rollD6(2);
                            r = new Report(6310);
                            r.subject = entity.getId();
                            r.add(diceRoll2);
                            r.newlines = 0;
                            addReport(r);

                            if ((diceRoll2.getIntValue() == 8) || (diceRoll2.getIntValue() == 9)) {
                                hits = 1;
                            } else if ((diceRoll2.getIntValue() == 10) || (diceRoll2.getIntValue() == 11)) {
                                hits = 2;
                            } else if (diceRoll2.getIntValue() == 12) {
                                hits = 3;
                            }

                            r = new Report(6328);
                            r.subject = entity.getId();
                            r.add("%d+1=%d".formatted(hits, hits + 1));
                            addReport(r);

                            hits++;

                            for (int j = 0; (j < 12) && (hits > 0); j++) {
                                var crit = mek.getCritical(Mek.LOC_CT, j);
                                if ((crit != null) &&
                                          (crit.getType() == CriticalSlot.TYPE_SYSTEM) &&
                                          (crit.getIndex() == Mek.SYSTEM_ENGINE) &&
                                          crit.isHittable()) {
                                    addReport(gameManager.applyCriticalHit(entity, Mek.LOC_CT, crit, true, 0, false));
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
                if (((Mek) entity).hasLaserHeatSinks()) {
                    boom--;
                }
                Roll diceRoll = Compute.rollD6(2);
                int rollValue = diceRoll.getIntValue();
                r = new Report(5065);
                r.subject = entity.getId();
                r.addDesc(entity);
                r.add(boom);
                if (entity.getCrew().hasActiveTechOfficer()) {
                    rollValue += 2;
                    String rollCalc = rollValue + " [" + diceRoll.getIntValue() + " + 2]";
                    r.addDataWithTooltip(rollCalc, diceRoll.getReport());
                } else {
                    r.add(diceRoll);
                }
                if (rollValue >= boom) {
                    // mek is ok
                    r.choose(true);
                    addReport(r);
                } else {
                    // boom!
                    r.choose(false);
                    addReport(r);
                    addReport(gameManager.explodeAmmoFromHeat(entity));
                }
            }

            // heat effects: mekwarrior damage
            // N.B. The pilot may already be dead.
            int lifeSupportCritCount;
            boolean torsoMountedCockpit = ((Mek) entity).getCockpitType() == Mek.COCKPIT_TORSO_MOUNTED;
            if (torsoMountedCockpit) {
                lifeSupportCritCount = entity.getHitCriticals(CriticalSlot.TYPE_SYSTEM,
                      Mek.SYSTEM_LIFE_SUPPORT,
                      Mek.LOC_RT);
                lifeSupportCritCount += entity.getHitCriticals(CriticalSlot.TYPE_SYSTEM,
                      Mek.SYSTEM_LIFE_SUPPORT,
                      Mek.LOC_LT);
            } else {
                lifeSupportCritCount = entity.getHitCriticals(CriticalSlot.TYPE_SYSTEM,
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
                if ((((Mek) entity).getCockpitType() == Mek.COCKPIT_TORSO_MOUNTED) &&
                          !entity.hasAbility(OptionsConstants.MD_PAIN_SHUNT)) {
                    damageToCrew += 1;
                }
                r = new Report(5070);
                r.subject = entity.getId();
                r.addDesc(entity);
                r.add(heatLimitDesc);
                r.add(damageToCrew);
                addReport(r);
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
                r = new Report(5075);
                r.subject = entity.getId();
                r.addDesc(entity);
                r.add(avoidNumber);
                r.add(diceRoll);

                if (diceRoll.getIntValue() >= avoidNumber) {
                    // damage avoided
                    r.choose(true);
                    addReport(r);
                } else {
                    r.choose(false);
                    addReport(r);
                    addReport(gameManager.damageCrew(entity, 1));
                }
            }

            // The pilot may have just expired.
            if ((entity.getCrew().isDead() || entity.getCrew().isDoomed()) && !entity.getCrew().isEjected()) {
                r = new Report(5080);
                r.subject = entity.getId();
                r.addDesc(entity);
                addReport(r);
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
                    r = new Report(5085);
                    r.subject = entity.getId();
                    r.addDesc(entity);
                    r.add(damageNumber);
                    r.add(diceRoll);
                    r.newlines = 0;

                    if (diceRoll.getIntValue() >= damageNumber) {
                        r.choose(true);
                    } else {
                        r.choose(false);
                        addReport(r);
                        addReport(gameManager.oneCriticalEntity(entity, Compute.randomInt(8), false, 0));
                        // add an empty report, for line breaking
                        r = new Report(1210, Report.PUBLIC);
                    }
                    addReport(r);
                }
            }

            // If the TacOps coolant failure rule is in use, check for coolant failure. If
            // the
            // heat sink capacity has already been offset by previous coolant failures,
            // further
            // reductions in capacity will have no effect.
            if (getGame().getOptions().booleanOption(OptionsConstants.ADVCOMBAT_TACOPS_COOLANT_FAILURE) &&
                      (entity.heat >= 5) &&
                      (entity.getCoolantFailureAmount() <
                             ((Mek) entity).getNumberOfSinks() * (((Mek) entity).hasDoubleHeatSinks() ? 2 : 1))) {
                Roll diceRoll = Compute.rollD6(2);
                int hitNumber = 10;
                hitNumber -= Math.max(0, (int) Math.ceil(entity.heat / 5.0) - 2);
                r = new Report(5525);
                r.subject = entity.getId();
                r.add(entity.getShortName());
                r.add(hitNumber);
                r.add(diceRoll);
                r.newlines = 0;
                addReport(r);

                if (diceRoll.getIntValue() >= hitNumber) {
                    r = new Report(5052);
                    r.subject = entity.getId();
                    addReport(r);
                    r = new Report(5526);
                    r.subject = entity.getId();
                    r.add(entity.getShortNameRaw());
                    addReport(r);
                    entity.addCoolantFailureAmount(1);
                } else {
                    r = new Report(5041);
                    r.subject = entity.getId();
                    addReport(r);
                }
            }
        }

        if (gameManager.getMainPhaseReport().size() == 1) {
            // I guess nothing happened...
            addReport(new Report(1205, Report.PUBLIC));
        }
    }
}

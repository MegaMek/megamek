/*
 * Copyright (c) 2021 - The MegaMek Team. All Rights Reserved.
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
package megamek.server;

import java.util.*;
import megamek.common.*;
import megamek.common.options.OptionsConstants;
import megamek.common.weapons.other.TSEMPWeapon;

/**
 * This class contains computations carried out by the Server class.
 * Methods put in here should be static and self-contained. 
 * @author NickAragua
 *
 */
public class ServerHelper {
    /**
     * Determines if the given entity is an infantry unit in the given hex is "in the open" 
     * (and thus subject to double damage from attacks)
     * @param te Target entity.
     * @param te_hex Hex where target entity is located.
     * @param game Game being played.
     * @param isPlatoon Whether the target unit is a platoon.
     * @param ammoExplosion Whether we're considering a "big boom" ammo explosion from tacops.
     * @param ignoreInfantryDoubleDamage Whether we should ignore double damage to infantry.
     * @return Whether the infantry unit can be considered to be "in the open"
     */
    public static boolean infantryInOpen(Entity te, IHex te_hex, Game game, 
            boolean isPlatoon, boolean ammoExplosion, boolean ignoreInfantryDoubleDamage) {
        
        if (isPlatoon && !te.isDestroyed() && !te.isDoomed() && !ignoreInfantryDoubleDamage
                && (((Infantry) te).getDugIn() != Infantry.DUG_IN_COMPLETE)) {
        	
        	if(te_hex == null) {
        		te_hex = game.getBoard().getHex(te.getPosition());
        	}
        	
            if ((te_hex != null) && !te_hex.containsTerrain(Terrains.WOODS) && !te_hex.containsTerrain(Terrains.JUNGLE)
                    && !te_hex.containsTerrain(Terrains.ROUGH) && !te_hex.containsTerrain(Terrains.RUBBLE)
                    && !te_hex.containsTerrain(Terrains.SWAMP) && !te_hex.containsTerrain(Terrains.BUILDING)
                    && !te_hex.containsTerrain(Terrains.FUEL_TANK) && !te_hex.containsTerrain(Terrains.FORTIFIED)
                    && (!te.hasAbility(OptionsConstants.INFANTRY_URBAN_GUERRILLA))
                    && (!te_hex.containsTerrain(Terrains.PAVEMENT) || !te_hex.containsTerrain(Terrains.ROAD))
                    && !ammoExplosion) {
                return true;
            }
        }
        
        return false;
    }
    
    /**
     * Worker function that handles heat as applied to aerospace fighter
     */
    public static void resolveAeroHeat(Game game, Entity entity, Vector<Report> vPhaseReport, Vector<Report> rhsReports, 
            int radicalHSBonus, int hotDogMod, Server s) {
        Report r;
        
        // If this aero is part of a squadron, we will deal with its
        // heat with the fighter squadron
        if (game.getEntity(entity.getTransportId()) instanceof FighterSquadron) {
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
            r = new Report(5015);
            r.subject = entity.getId();
            vPhaseReport.add(r);
        }

        // Combat computers help manage heat
        if (entity.hasQuirk(OptionsConstants.QUIRK_POS_COMBAT_COMPUTER)) {
            int reduce = Math.min(entity.heatBuildup, 4);
            r = new Report(5026);
            r.subject = entity.getId();
            r.add(reduce);
            vPhaseReport.add(r);
            entity.heatBuildup -= reduce;
        }

        // Add heat from external sources to the heat buildup
        int max_ext_heat = game.getOptions().intOption(
                OptionsConstants.ADVCOMBAT_MAX_EXTERNAL_HEAT); // Check Game Options
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
        int tosink = entity.getHeatCapacityWithWater() + radicalHSBonus;

        // should we use a coolant pod?
        int safeHeat = entity.hasInfernoAmmo() ? 9 : 13;
        int possibleSinkage = ((Aero) entity).getHeatSinks()
                - entity.getCoolantFailureAmount();
        for (Mounted m : entity.getEquipment()) {
            if (m.getType() instanceof AmmoType) {
                AmmoType at = (AmmoType) m.getType();
                if ((at.getAmmoType() == AmmoType.T_COOLANT_POD) && m.isAmmoUsable()) {
                    EquipmentMode mode = m.curMode();
                    if (mode.equals("dump")) {
                        r = new Report(5260);
                        r.subject = entity.getId();
                        vPhaseReport.add(r);
                        m.setShotsLeft(0);
                        tosink += possibleSinkage;
                        break;
                    }
                    if (mode.equals("safe") && ((entity.heat - tosink) > safeHeat)) {
                        r = new Report(5265);
                        r.subject = entity.getId();
                        vPhaseReport.add(r);
                        m.setShotsLeft(0);
                        tosink += possibleSinkage;
                        break;
                    }
                    if (mode.equals("efficient")
                            && ((entity.heat - tosink) >= possibleSinkage)) {
                        r = new Report(5270);
                        r.subject = entity.getId();
                        vPhaseReport.add(r);
                        m.setShotsLeft(0);
                        tosink += possibleSinkage;
                        break;
                    }
                }
            }
        }

        tosink = Math.min(tosink, entity.heat);
        entity.heat -= tosink;
        r = new Report(5035);
        r.subject = entity.getId();
        r.addDesc(entity);
        r.add(entity.heatBuildup);
        r.add(tosink);
        r.add(entity.heat);
        vPhaseReport.add(r);
        entity.heatBuildup = 0;
        vPhaseReport.addAll(rhsReports);

        // add in the effects of heat

        if ((entity instanceof Dropship) || (entity instanceof Jumpship)) {
            // only check for a possible control roll
            if (entity.heat > 0) {
                int bonus = (int) Math.ceil(entity.heat / 100.0);
                game.addControlRoll(new PilotingRollData(
                        entity.getId(), bonus, "used too much heat"));
                entity.heat = 0;
            }
            return;
        }

        // Capital fighters can overheat and require control rolls
        if (entity.isCapitalFighter() && (entity.heat > 0)) {
            int penalty = (int) Math.ceil(entity.heat / 15.0);
            game.addControlRoll(new PilotingRollData(entity.getId(),
                    penalty, "used too much heat"));
        }

        // Like other large craft, the rest of these rules don't apply
        // to capital fighters
        if (entity.isCapitalFighter()) {
            return;
        }

        int autoShutDownHeat = 30;
        boolean mtHeat = game.getOptions().booleanOption(OptionsConstants.ADVCOMBAT_TACOPS_HEAT);
        if (mtHeat) {
            autoShutDownHeat = 50;
        }

        // heat effects: start up
        if ((entity.heat < autoShutDownHeat) && entity.isShutDown()) {
            // only start up if not shut down by taser or a TSEMP
            if ((entity.getTaserShutdownRounds() == 0)
                    && (entity.getTsempEffect() != TSEMPWeapon.TSEMP_EFFECT_SHUTDOWN)) {
                if ((entity.heat < 14) && !entity.isManualShutdown()) {
                    // automatically starts up again
                    entity.setShutDown(false);
                    r = new Report(5045);
                    r.subject = entity.getId();
                    r.addDesc(entity);
                    vPhaseReport.add(r);
                } else if (!entity.isManualShutdown()) {
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
                                    break;
                            }
                        }
                        int startupRoll = entity.getCrew().rollPilotingSkill();
                        r = new Report(5050);
                        r.subject = entity.getId();
                        r.addDesc(entity);
                        r.add(startup);
                        r.add(startupRoll);
                        if (startupRoll >= startup) {
                            // start 'er back up
                            entity.setShutDown(false);
                            r.choose(true);
                        } else {
                            r.choose(false);
                        }
                    }
                    vPhaseReport.add(r);
                }
            } else {
                // if we're shutdown by a BA taser, we might activate
                // again
                if (entity.isBATaserShutdown()) {
                    int roll = Compute.d6(2);
                    if (roll >= 8) {
                        entity.setTaserShutdownRounds(0);
                        if (!(game.getOptions().booleanOption(
                                OptionsConstants.RPG_MANUAL_SHUTDOWN)
                                && entity.isManualShutdown())) {
                            entity.setShutDown(false);
                        }
                        entity.setBATaserShutdown(false);
                    }
                }
            }
        }
        // heat effects: shutdown!
        else if ((entity.heat >= 14) && !entity.isShutDown()) {
            if (entity.heat >= autoShutDownHeat) {
                r = new Report(5055);
                r.subject = entity.getId();
                r.addDesc(entity);
                vPhaseReport.add(r);
                // okay, now mark shut down
                entity.setShutDown(true);
            } else {
                // Again, pilot KO means shutdown is automatic.
                if (!entity.getCrew().isActive()) {
                    r = new Report(5056);
                    r.subject = entity.getId();
                    r.addDesc(entity);
                    vPhaseReport.add(r);
                    entity.setShutDown(true);
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
                    int shutdownRoll = Compute.d6(2);
                    r = new Report(5060);
                    r.subject = entity.getId();
                    r.addDesc(entity);
                    r.add(shutdown);
                    r.add(shutdownRoll);
                    if (shutdownRoll >= shutdown) {
                        // avoided
                        r.choose(true);
                        vPhaseReport.add(r);
                    } else {
                        // shutting down...
                        r.choose(false);
                        vPhaseReport.add(r);
                        // okay, now mark shut down
                        entity.setShutDown(true);
                    }
                }
            }
        }

        s.checkRandomAeroMovement(entity, hotDogMod);

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
            r = new Report(5065);
            r.subject = entity.getId();
            r.addDesc(entity);
            r.add(boom);
            
            int roll = Compute.d6(2);
            r.add(roll);
            
            if (roll >= boom) {
                // no ammo explosion
                r.choose(true);
                vPhaseReport.add(r);
            } else {
                // boom!
                r.choose(false);
                vPhaseReport.add(r);
                vPhaseReport.addAll(s.explodeAmmoFromHeat(entity));
            }
        }

        // heat effects: pilot damage
        if (entity.heat >= 21) {
            int ouch = (6 + (entity.heat >= 27 ? 3 : 0)) - hotDogMod;
            int ouchRoll = Compute.d6(2);
            r = new Report(5075);
            r.subject = entity.getId();
            r.addDesc(entity);
            r.add(ouch);
            r.add(ouchRoll);
            if (ouchRoll >= ouch) {
                // pilot is ok
                r.choose(true);
                vPhaseReport.add(r);
            } else {
                // pilot is hurting
                r.choose(false);
                vPhaseReport.add(r);
                vPhaseReport.addAll(s.damageCrew(entity, 1));
            }
        }

        // The pilot may have just expired.
        if ((entity.getCrew().isDead() || entity.getCrew().isDoomed())
                && !entity.getCrew().isEjected()) {
            r = new Report(5080);
            r.subject = entity.getId();
            r.addDesc(entity);
            vPhaseReport.add(r);
            vPhaseReport.addAll(s.destroyEntity(entity, "pilot death", true));
        }
    }
    
    /**
     * Helper function that causes an entity to sink to the bottom of the water
     * hex it's currently in.
     */
    public static void sinkToBottom(Entity entity) {
        if((entity == null) || !entity.getGame().getBoard().contains(entity.getPosition())) {
            return;
        }
        
        IHex fallHex = entity.getGame().getBoard().getHex(entity.getPosition());
        int waterDepth = 0;
        
        // we're going hull down, we still sink to the bottom if appropriate
        if (fallHex.containsTerrain(Terrains.WATER)) {
            boolean hexHasBridge = fallHex.containsTerrain(Terrains.BRIDGE_CF);
            boolean entityOnTopOfBridge = hexHasBridge && (entity.getElevation() == fallHex.ceiling());
            
            if (!entityOnTopOfBridge) {
                // *Only* use this if there actually is water in the hex, otherwise
                // we get ITerrain.LEVEL_NONE, i.e. Integer.minValue...
                waterDepth = fallHex.terrainLevel(Terrains.WATER);
                entity.setElevation(-waterDepth);
            }
        }
    }
    
    public static void checkAndApplyMagmaCrust(IHex hex, int elevation, Entity entity, Coords curPos,
            boolean jumpLanding, Vector<Report> vPhaseReport, Server server) {
        
        if ((hex.terrainLevel(Terrains.MAGMA) == 1) && (elevation == 0) && (entity.getMovementMode() != EntityMovementMode.HOVER)) {
            int reportID = jumpLanding ? 2396 : 2395;
            
            int roll = Compute.d6();
            Report r = new Report(reportID);
            r.addDesc(entity);
            r.add(roll);
            r.subject = entity.getId();
            vPhaseReport.add(r);
            
            int rollTarget = jumpLanding ? 4 : 6;
            
            if (roll >= rollTarget) {
                hex.removeTerrain(Terrains.MAGMA);
                hex.addTerrain(Terrains.getTerrainFactory().createTerrain(Terrains.MAGMA, 2));
                server.sendChangedHex(curPos);
                for (Entity en : entity.getGame().getEntitiesVector(curPos)) {
                    server.doMagmaDamage(en, false);
                }
            }
        }
    }

    /**
     * Loops through all active entities in the game and performs mine detection
     */
    public static void detectMinefields(Game game, Vector<Report> vPhaseReport, Server server) {
        boolean tacOpsBap = game.getOptions().booleanOption(OptionsConstants.ADVANCED_TACOPS_BAP);
        
        // if the entity is on the board
        // and it either a) hasn't moved or b) we're not using TacOps BAP rules
        // if we are not using the TacOps BAP rules, that means we only check the entity's final hex
        // if we are using TacOps BAP rules, all moved entities have made all their checks already
        // so we just need to do the unmoved entities
        for (Entity entity : game.getEntitiesVector()) {
            if (!entity.isOffBoard() && entity.isDeployed() &&
                    ((entity.delta_distance == 0) || !tacOpsBap)) {
                detectMinefields(game, entity, entity.getPosition(), vPhaseReport, server);
            }
        }
    }
    
    /**
     * Checks for minefields within the entity's active probe range.
     * @return True if any minefields have been detected.
     */
    public static boolean detectMinefields(Game game, Entity entity, Coords coords, 
            Vector<Report> vPhaseReport, Server server) {
        if (!game.getOptions().booleanOption(OptionsConstants.ADVANCED_MINEFIELDS)) {
            return false;
        }
        
        int probeRange = entity.getBAPRange();
        if (probeRange <= 0) {
            return false;
        }
        
        boolean minefieldDetected = false;
        
        for (int distance = 1; distance <= probeRange; distance++) {      
            for (Coords potentialMineCoords : coords.allAtDistance(distance)) {
                if (!game.getBoard().contains(potentialMineCoords)) {
                    continue;
                }
                
                for (Minefield minefield : game.getMinefields(potentialMineCoords)) {
                    // no need to roll for already revealed minefields
                    if (entity.getOwner().containsMinefield(minefield)) {
                        continue;
                    }
                    
                    int roll = Compute.d6(2);
                    
                    if (roll >= minefield.getBAPDetectionTarget()) {
                        minefieldDetected = true;
                        
                        Report r = new Report(2163);
                        r.subject = entity.getId();
                        r.add(entity.getShortName(), true);
                        r.add(potentialMineCoords.toFriendlyString());
                        vPhaseReport.add(r);
                        
                        server.revealMinefield(entity.getOwner(), minefield);
                    }
                }
            }
        }
        
        return minefieldDetected;
    }  
}

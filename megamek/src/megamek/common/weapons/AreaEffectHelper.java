/*  
* MegaMek - Copyright (C) 2020 - The MegaMek Team  
*  
* This program is free software; you can redistribute it and/or modify it under  
* the terms of the GNU General Public License as published by the Free Software  
* Foundation; either version 2 of the License, or (at your option) any later  
* version.  
*  
* This program is distributed in the hope that it will be useful, but WITHOUT  
* ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS  
* FOR A PARTICULAR PURPOSE. See the GNU General Public License for more  
* details.  
*/  

package megamek.common.weapons;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.Vector;

import megamek.client.bot.princess.BotGeometry;
import megamek.common.AmmoType;
import megamek.common.BattleArmor;
import megamek.common.BombType;
import megamek.common.Compute;
import megamek.common.Coords;
import megamek.common.Entity;
import megamek.common.EquipmentType;
import megamek.common.IGame;
import megamek.common.Infantry;
import megamek.common.PlanetaryConditions;
import megamek.common.Report;
import megamek.common.TargetRoll;
import megamek.server.Server;

/**
 * Class containing functionality that helps out with area effect weapons. 
 * @author NickAragua
 *
 */
public class AreaEffectHelper {
    // maps equipment name to blast radius index for fuel-air ordnance
    public static Map<String, Integer> fuelAirBlastRadiusIndex;
    public static final int[] fuelAirDamage = { 5, 10, 20, 30 };
    
    /**
     * Worker funciton that initializes blast radius data for fuel-air explosives of various types.
     */
    private static void initializeFuelAirBlastRadiusIndexData() {
        fuelAirBlastRadiusIndex = new HashMap<>();
        
        fuelAirBlastRadiusIndex.put(BombType.getBombInternalName(BombType.B_FAE_SMALL), 2);
        fuelAirBlastRadiusIndex.put(BombType.getBombInternalName(BombType.B_FAE_LARGE), 3);
        
        // the following ammo types have the capability to load FAE munitions:
        // Arrow IV, Thumper, Sniper, Long Tom        
        addFuelAirBlastRadiusIndex(AmmoType.T_ARROW_IV, 2);
        addFuelAirBlastRadiusIndex(AmmoType.T_THUMPER, 1);
        addFuelAirBlastRadiusIndex(AmmoType.T_THUMPER_CANNON, 1);
        addFuelAirBlastRadiusIndex(AmmoType.T_SNIPER, 2);
        addFuelAirBlastRadiusIndex(AmmoType.T_SNIPER_CANNON, 2);
        addFuelAirBlastRadiusIndex(AmmoType.T_LONG_TOM_CANNON, 3);
        addFuelAirBlastRadiusIndex(AmmoType.T_LONG_TOM, 3);
    }
    
    /**
     * Helper function that adds elements to the fuel blast radius index
     */
    private static void addFuelAirBlastRadiusIndex(int ammoType, int blastRadius) {
        // this is relatively inefficient, but probably the least inefficient of the options
        // to acquire a list of the ammo types
        for(AmmoType at : AmmoType.getMunitionsFor(ammoType)) {
            if(at.getMunitionType() == AmmoType.M_FAE) {
                fuelAirBlastRadiusIndex.put(at.getInternalName(), blastRadius);
            }
        }
    }
    
    /**
     * Get the blast radius of a particular bomb type, given the internal name.
     */
    public static int getFuelAirBlastRadiusIndex(String name) {
        if(fuelAirBlastRadiusIndex == null) {
            initializeFuelAirBlastRadiusIndexData();
        }
        
        return fuelAirBlastRadiusIndex.getOrDefault(name, 0);
    }
    
    /**
     * Helper function that processes damage for fuel-air explosives.
     */
    public static void processFuelAirDamage(Coords center, EquipmentType ordnanceType, Entity attacker, Vector<Report> vPhaseReport, Server server) {
        IGame game = attacker.getGame();
        // sanity check: if this attack is happening in vacuum through very thin atmo, add that to the phase report and terminate early 
        boolean notEnoughAtmo = game.getBoard().inSpace() ||
                game.getPlanetaryConditions().getAtmosphere() <= PlanetaryConditions.ATMO_TRACE;
        
        if(notEnoughAtmo) {
            Report r = new Report(9986);
            r.indent(1);
            r.subject = attacker.getId();
            r.newlines = 1;
            vPhaseReport.addElement(r);
            return;
        }
        
        boolean thinAtmo = game.getPlanetaryConditions().getAtmosphere() == PlanetaryConditions.ATMO_THIN;
        int bombRadius = getFuelAirBlastRadiusIndex(ordnanceType.getInternalName());
        
        if(thinAtmo) {
            Report r = new Report(9990);
            r.indent(1);
            r.subject = attacker.getId();
            r.newlines = 1;
            vPhaseReport.addElement(r);
        }
        
        Vector<Integer> alreadyHit = new Vector<>();
        
        // assemble collection of hexes at ranges 0 to radius
        // for each hex, invoke artilleryDamageHex, with the damage set according to this:
        //      radius chart 
        //      (divided by half, round up for thin atmo)
        //      not here, but in artilleryDamageHex, make sure to 2x damage for infantry outside of building
        //      not here, but in artilleryDamageHex, make sure to 1.5x damage for light building or unit with armor BAR < 10
        //      not here, but in artilleryDamageHex, make sure to .5x damage for "castle brian" or "armored" building
        // if any attacked unit is infantry or BA, roll 2d6 + current distance. Inf dies on 9-, BA dies on 7-
        for(int damageBracket = bombRadius, distFromCenter = 0; damageBracket >= 0; damageBracket--, distFromCenter++) {
            Set<Coords> donut = BotGeometry.getHexDonut(center, distFromCenter);
            for(Coords coords : donut) {
                int damage = AreaEffectHelper.fuelAirDamage[damageBracket];
                if(thinAtmo) {
                    damage = (int) Math.ceil(damage / 2.0);
                }
                
                checkInfantryDestruction(coords, distFromCenter, attacker, alreadyHit, vPhaseReport, game, server);
                
                server.artilleryDamageHex(coords, center, damage, (AmmoType) ordnanceType, attacker.getId(), attacker, null, false, 0, vPhaseReport, false,
                        alreadyHit, false);
                
                TargetRoll fireRoll = new TargetRoll(7, "fuel-air ordnance");
                server.tryIgniteHex(coords, attacker.getId(), false, false, fireRoll, true, -1, vPhaseReport);
            }
        }
    }
    
    /**
     * Worker function that checks for and implements instant infantry destruction due to fuel air ordnance, if necessary
     */
    public static void checkInfantryDestruction(Coords coords, int distFromCenter, Entity attacker, Vector<Integer> alreadyHit,
            Vector<Report> vPhaseReport, IGame game, Server server) {
        for(Entity entity : game.getEntitiesVector(coords)) {
            int rollTarget = -1;
            if(entity instanceof BattleArmor) {
                rollTarget = 7;
            } else if(entity instanceof Infantry) {
                rollTarget = 9;
            } else {
                continue;
            }
            
            int roll = Compute.d6(2);
            int result = roll + distFromCenter;
            boolean destroyed = result > rollTarget;
            
            Report r = new Report(9987);
            r.indent(1);
            r.subject = attacker.getId();
            r.newlines = 1;
            r.add(rollTarget);
            r.add(roll);
            r.add(distFromCenter);
            r.choose(destroyed);
            vPhaseReport.addElement(r);
            
            if(destroyed) {
                vPhaseReport.addAll(server.destroyEntity(entity, "fuel-air bomb detonation", false, false));
                alreadyHit.add(entity.getId());
            }
            return;
        }
    }
}

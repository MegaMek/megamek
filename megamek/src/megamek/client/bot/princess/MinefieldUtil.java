/*
* MegaMek -
* Copyright (C) 2021 The MegaMek Team
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

package megamek.client.bot.princess;

import megamek.common.Compute;
import megamek.common.Entity;
import megamek.common.EntityMovementMode;
import megamek.common.Mech;
import megamek.common.Minefield;
import megamek.common.MovePath;
import megamek.common.MoveStep;
import megamek.common.annotations.Nullable;

/**
 * This class contains logic to evaluate the damage a unit could sustain from
 * moving a long a move path containing minefields
 * @author NickAragua
 */
public class MinefieldUtil {
    /**
     * Calculate how much damage we'll take from stepping on mines over a particular path
     */
    public static double checkPathForMinefieldHazards(MovePath path) {
        double hazardAccumulator = 0;
                
        for (MoveStep step : path.getStepVector()) {
            hazardAccumulator += calcMinefieldHazardForHex(step, path.getEntity(),
                    path.isJumping(), step.equals(path.getLastStep())); 
        }
        
        //TODO: Teach bot to activate minesweepers
        
        return hazardAccumulator;
    }
    
    /**
     * Calculate how much damage we'll take from stepping on mines in a particular hex
     */
    public static double calcMinefieldHazardForHex(@Nullable MoveStep step, Entity movingUnit, 
            boolean isJumping, boolean lastStep) {
        // if we're not actually taking a step, no minefield hazard
        if ((step == null) || !step.getType().entersNewHex()) {
            return 0;
        }
        
        // if our movement mode does not result in minefield detonation, no minefield hazard
        if (!movingUnit.getMovementMode().detonatesGroundMinefields()) {
            return 0;
        }
        
        double hazardAccumulator = 0;
        // hovercraft and WIGEs grinding along the ground detonate minefields on a 12
        boolean hoverMovement = (movingUnit.getMovementMode() == EntityMovementMode.HOVER) ||
                ((movingUnit.getMovementMode() == EntityMovementMode.WIGE) && (movingUnit.getElevation() == 0));
        double hoverMovementMultiplier = hoverMovement ?
                Compute.oddsAbove(Minefield.HOVER_WIGE_DETONATION_TARGET) : 1;
        
        // only mechs interact with vibrabombs
        boolean unitIsMech = movingUnit instanceof Mech;
        
        for (Minefield minefield : movingUnit.getGame().getMinefields(step.getPosition())) {
            switch (minefield.getType()) {
                case Minefield.TYPE_CONVENTIONAL:
                case Minefield.TYPE_INFERNO:
                    // if we're either not jumping or it's the last step
                    if (!isJumping || lastStep) {
                        hazardAccumulator += minefield.getDensity() * hoverMovementMultiplier;
                    }
                    break;
                case Minefield.TYPE_ACTIVE:
                    hazardAccumulator += minefield.getDensity() * hoverMovementMultiplier;
                    break;
                case Minefield.TYPE_VIBRABOMB:
                    // mechs >10 tons the "setting" will set off vibrabombs before they 
                    // get to them so we don't particularly care 
                    if (unitIsMech && (!isJumping || lastStep) &&
                            (movingUnit.getWeight() >= minefield.getSetting()) &&
                            (movingUnit.getWeight() <= minefield.getSetting() + 10)) {
                        hazardAccumulator += minefield.getDensity();
                    }
                    break;
            }
        }
        
        return hazardAccumulator;
    }
}

package megamek.client.bot.princess;

import megamek.common.IAero;
import megamek.common.MovePath;
import megamek.common.UnitType;
import megamek.common.MovePath.MoveStepType;

// Helper class that contains functionality relating exclusively to aero units
public class AeroPathUtil 
{	
	/**
	 * Determines if the aircraft undertaking the given path will stall at the end of the turn. 
	 * Only relevant for aerodyne units 
	 * @param movePath the path to check
	 * @return whether the aircraft will stall at the end of the path
	 */
	public static boolean WillStall(MovePath movePath)
	{
		// Stalling only happens in atmospheres on ground maps
		if(!movePath.isOnAtmosphericGroundMap()) {
			return false;
		}
		
		// aircraft that are not vtols or spheroids will stall if the final velocity is zero after all acc/dec
		// aerodyne units can actually land or "vertical land" and it's ok to do so (even though you're unlikely to find the 20 clear spaces)
		// spheroids will stall if they don't move or land
		
		boolean isAirborne = movePath.getEntity().isAirborne();
		boolean isSpheroid = UnitType.isSpheroidDropship(movePath.getEntity());
		
        if ((movePath.getFinalVelocity() == 0) && isAirborne && !isSpheroid) {
            return true;
        }
        
        if (isSpheroid && (movePath.getFinalNDown() == 0) 
        		&& (movePath.getMpUsed() == 0) 
        		&& !movePath.contains(MoveStepType.VLAND)) {
            return true;
        }
        
        return false;
	}
	
	/**
     * Determines if the aircraft undertaking the given path will explicitly go off the board
     * @param movePath the path to check
     * @return True or false
     */
	public static boolean WillCrash(MovePath movePath)
	{
		return movePath.getEntity().isAero() && 
		        (movePath.getFinalAltitude() < 1) && 
		        !movePath.contains(MoveStepType.VLAND) && 
		        !movePath.contains(MoveStepType.LAND);
	}
	
	/**
	 * A quick determination that checks the given path for the most common causes of a PSR and whether it leads us off board.
	 * The idea being that a safe path off board should not include any PSRs.
	 * @param movePath The path to check
	 * @return True or false
	 */
	public static boolean IsSafePathOffBoard(MovePath movePath)
	{
	    // common causes of PSR include, but are not limited to:
	    // stalling your aircraft
	    // crashing your aircraft into the ground
	    // executing maneuvers
	    // thrusting too hard 
	    // see your doctor if you experience any of these symptoms as it may lead to your aircraft transforming into a lawn dart
		return !WillStall(movePath) && !WillCrash(movePath) && movePath.fliesOffBoard() && !movePath.contains(MoveStepType.MANEUVER) &&
		        (movePath.getMpUsed() <= movePath.getEntity().getWalkMP()) && 
		        (movePath.getEntity().isAero() && (movePath.getMpUsed() <= ((IAero) movePath.getEntity()).getSI()));
	}

}

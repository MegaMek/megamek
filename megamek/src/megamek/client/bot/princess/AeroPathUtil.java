package megamek.client.bot.princess;

import megamek.common.MovePath;
import megamek.common.UnitType;
import megamek.common.MovePath.MoveStepType;

// Helper class that contains functionality relating exclusively to aero units
public class AeroPathUtil 
{	
	// Determines if the aircraft undertaking the given path will stall at the end of the turn. 
	public static boolean WillStall(MovePath movePath)
	{
		// aircraft that are not vtols or spheroids will stall if the final velocity is zero
		// aerodyne units can actually land or "vertical land" and it's ok to do so (even though you're unlikely to find the 20 clear spaces)
		// spheroids will stall if they don't move or land
		
		boolean isVTOL = UnitType.isVTOL(movePath.getEntity());
		boolean isSpheroid = UnitType.isSpheroidDropship(movePath.getEntity());
		
        if (movePath.getFinalVelocity() == 0 
        		&& !isVTOL 
        		&& !isSpheroid 
        		&& !movePath.contains(MoveStepType.VLAND)
        		&& !movePath.contains(MoveStepType.LAND)) {
            return true;
        }
        
        if (isSpheroid && (movePath.getFinalNDown() == 0) 
        		&& (movePath.getMpUsed() == 0) 
        		&& !movePath.contains(MoveStepType.VLAND)) {
            return true;
        }
        
        return false;
	}
	
	// Determines if the aircraft undertaking the given path will crash
	public static boolean WillCrash(MovePath movePath)
	{
		return movePath.getFinalAltitude() < 1 && !movePath.contains(MoveStepType.VLAND) && !movePath.contains(MoveStepType.LAND);
	}
	
	// Determines if the aircraft undertaking the given path will go off the board at any point in time
	public static boolean WillGoOffBoard(MovePath movePath)
	{
		return movePath.contains(MoveStepType.RETURN);
	}
	
	public static boolean IsSafePathOffBoard(MovePath movePath)
	{
		// also need to include whether the path has any piloting rolls involved
		return !WillStall(movePath) && !WillCrash(movePath) && WillGoOffBoard(movePath) && !movePath.contains(MoveStepType.MANEUVER);
	}

}

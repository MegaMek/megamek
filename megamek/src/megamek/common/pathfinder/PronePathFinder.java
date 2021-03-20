package megamek.common.pathfinder;

import java.util.ArrayList;
import java.util.List;

import megamek.client.bot.princess.AeroPathUtil;
import megamek.common.MovePath;
import megamek.common.MovePath.MoveStepType;
import megamek.common.options.OptionsConstants;

/**
 * This class handles pathfinding for situations where the unit is
 * prone and wants to remain prone for whatever reason
 * (no leg, getting up will result in exposure to fire, etc) 
 * @author NickAragua
 */
public class PronePathFinder {
	private List<MovePath> pronePaths;
	
	public List<MovePath> getPronePaths() {
		return pronePaths;
	}
	
	public void run(MovePath startingEdge) {
		pronePaths = new ArrayList<>();
		
		// if we're prone, consider staying that way
		if (startingEdge.getFinalProne()) {
			pronePaths.add(startingEdge);
			pronePaths.addAll(AeroPathUtil.generateValidRotations(startingEdge));
			
			// if we can go hull down, consider doing so - going "hull down" from prone
			// doesn't require a PSR.
			if (startingEdge.getGame().getOptions().booleanOption(OptionsConstants.ADVGRNDMOV_TACOPS_HULL_DOWN) &&
					startingEdge.getEntity().canGoHullDown()) {
				MovePath hullDown = startingEdge.clone().addStep(MoveStepType.HULL_DOWN);
				
				if (hullDown.isMoveLegal()) {
					pronePaths.add(hullDown);
					pronePaths.addAll(AeroPathUtil.generateValidRotations(hullDown));
				}
			}
		}
	}
}

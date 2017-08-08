package megamek.client.bot.princess;

import java.util.Map;

import megamek.common.Entity;
import megamek.common.Mounted;
import megamek.common.Targetable;
import megamek.common.annotations.Nullable;

// This data structure contains parameters that may be passed to the "determineBestFiringPlan()"
public class FiringPlanCalculationParameters
{
	//The type of firing plan calculation to carry out
	public enum FiringPlanCalculationType
	{
		// We're guessing the firing plan based on our estimate of enemy movement
		Guess,
		// We're getting a firing plan based on exact known enemy movement results
		Get
	}
	
	private Entity shooter;
	private EntityState shooterState; 
	private Targetable target;
	private EntityState targetState; 
	private int maxHeat; 
	private Map<Mounted, Double> ammoConservation;
	private FiringPlanCalculationType calculationType;
    
	public Entity getShooter() { return shooter; }
	public EntityState getShooterState() { return shooterState; } 
	public Targetable getTarget() { return target; }
	public EntityState getTargetState() { return targetState; } 
	public int getMaxHeat() { return maxHeat; } 
	public Map<Mounted, Double> getAmmoConservation() { return ammoConservation; }
	public FiringPlanCalculationType getCalculationType() { return calculationType; }
	
    /**
     * Creates an instance of this data structure for when we're guessing the firing plan based on estimated movement
     *
     * @param shooter      The unit doing the shooting.
     * @param shooterState The current state of the shooting unit.
     * @param target       The unit being shot at.
     * @param targetState  The current state of the target unit.
     * @param maxHeat      How much heat we're willing to tolerate.
     * @return the 'best' firing plan under a certain heat, includes the option of twisting.
     */
    public FiringPlanCalculationParameters(Entity shooter,
        @Nullable EntityState shooterState, Targetable target,
        @Nullable EntityState targetState, @Nullable Integer maxHeat)
    {
    	this.shooter = shooter;
    	this.shooterState = shooterState;
    	this.target = target;
    	this.targetState = targetState;
    	this.maxHeat = maxHeat != null ? maxHeat : FireControl.DOES_NOT_TRACK_HEAT;
    	if(this.maxHeat < 0)
    		this.maxHeat = 0;
    	calculationType = FiringPlanCalculationType.Guess;
    }
    
    /**
     * Creates an instance of this data structure for when we're getting the firing plan based on known movement results
     *
     * @param shooter      		The unit doing the shooting.
     * @param target       		The unit being shot at.
     * @param ammoConservation  Ammo conservation biases of the unit's mounted weapons.
     * @return the 'best' firing plan under a certain heat, includes the option of twisting.
     */
    public FiringPlanCalculationParameters(Entity shooter, Targetable target, Map<Mounted, Double> ammoConservation)
    {
    	this.shooter = shooter;
    	this.target = target;
    	this.ammoConservation = ammoConservation;
    	calculationType = FiringPlanCalculationType.Get;
    }
}

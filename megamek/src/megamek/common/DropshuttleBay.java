/**
 * 
 */
package megamek.common;

/**
 * Implements internal bays for dropships used by primitive jumpships.
 * See rules IO, p. 119.
 * 
 * @author Neoancient
 *
 */
public class DropshuttleBay extends Bay {
    
    /**
     * 
     */
    private static final long serialVersionUID = -6910402023514976670L;
    
    // No more than one bay is allowed per armor facing
    private int facing = Entity.LOC_NONE;

    /**
     * The default constructor is only for serialization.
     */

    protected DropshuttleBay() {
        totalSpace = 0;
        currentSpace = 0;
    }

    /**
     * Create a new dropshuttle bay
     *
     * @param facing The armor facing of the bay
     */
    public DropshuttleBay(int facing) {
        totalSpace = 2;
        currentSpace = 2;
        this.facing = facing;
    }

    // Type is Dropshuttle Bay
    public String getType() {
        return "Dropshuttle Bay";
    }

    public boolean canLoad(Entity unit) {
        
        return unit.hasETypeFlag(Entity.ETYPE_DROPSHIP)
                && (unit.getWeight() <= 5000)
                && (currentSpace >= 1);
    }
    
    @Override
    public int getFacing() {
        return facing;
    }
    
    /**
     * Sets the bay location
     * @param facing The armor facing (location) of the bay
     */
    public void setFacing(int facing) {
        this.facing = facing;
    }
    
    @Override
    public String toString() {
        return "dropshuttlebay:" + facing;
    }
    
    @Override
    public int hardpointCost() {
        return 2;
    }
    
    public static TechAdvancement techAdvancement() {
        return new TechAdvancement(TECH_BASE_IS).setISAdvancement(2110, 2120, DATE_NONE, 2500)
                .setISApproximate(true, false).setTechRating(RATING_C)
                .setProductionFactions(F_TA).setProductionFactions(F_TA)
                .setAvailability(RATING_C, RATING_X, RATING_X, RATING_X)
                .setStaticTechLevel(SimpleTechLevel.ADVANCED);
    }

}

package megamek.common;

public class BattleArmorHandlesTank extends BattleArmorHandles {

    // Private attributes, constants and helper functions.

    /**
     * The set of front locations blocked by loaded troopers.
     */
    private static final int[] BLOCKED_LOCATIONS = { Tank.LOC_RIGHT,
                                                     Tank.LOC_LEFT,
                                                     Tank.LOC_REAR };

    /**
     * The set of front locations that load troopers externally.
     */
    private static final int[] EXTERIOR_LOCATIONS = { Tank.LOC_RIGHT,
                                                      Tank.LOC_LEFT,
                                                      Tank.LOC_REAR };

    // Protected constructors and methods.

    /**
     * Get the locations blocked when a squad is loaded.
     * <p/>
     * Sub-classes are encouraged to override this method.
     *
     * @param   isRear - a <code>boolean</code> value stating if the given
     *          location is rear facing; if <code>false</code>, the location
     *          is front facing.
     * @return  an array of <code>int</code> listing the blocked locations.
     */
    protected int[] getBlockedLocs( boolean isRear ) {
        return BattleArmorHandlesTank.BLOCKED_LOCATIONS;
    }

    /**
     * Get the exterior locations that a loaded squad covers.
     * <p/>
     * Sub-classes are encouraged to override this method.
     *
     * @param   isRear - a <code>boolean</code> value stating if the given
     *          location is rear facing; if <code>false</code>, the location
     *          is front facing.
     * @return  an array of <code>int</code> listing the exterior locations.
     */
    protected int[] getExteriorLocs( boolean isRear ) {
        return BattleArmorHandlesTank.EXTERIOR_LOCATIONS;
    }

}

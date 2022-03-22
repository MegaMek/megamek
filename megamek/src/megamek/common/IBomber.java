/**
 * 
 */
package megamek.common;

import java.util.Arrays;
import java.util.List;

import megamek.common.options.OptionsConstants;

/**
 * Common interface for all entities capable of carrying bombs and making bomb attacks, includig Aero,
 * LandAirMech, and VTOL.
 * 
 * @author Neoancient
 *
 */
public interface IBomber {
    
    public static final String SPACE_BOMB_ATTACK = "SpaceBombAttack";
    public static final String DIVE_BOMB_ATTACK = "DiveBombAttack";
    public static final String ALT_BOMB_ATTACK = "AltBombAttack";

    /**
     * @return The total number of bomb points that the bomber can carry.
     */
    int getMaxBombPoints();
    
    /**
     * Fighters and VTOLs can carry any size bomb up to the maximum number of points, but LAMs are limited
     * to the number of bays in a single location.
     * 
     * @return The largest single bomb that can be carried
     */
    default int getMaxBombSize() {
        return getMaxBombPoints();
    }
    
    /**
     * @return The number of each bomb type that was selected prior to deployment
     */
    int[] getBombChoices();
    
    /**
     * Sets the bomb type selections prior to deployment.
     * 
     * @param bc An array with the count of each bomb type as the value of the bomb type's index
     */
    void setBombChoices(int[] bc);
    
    /**
     * Sets the count of each bomb to zero
     */
    void clearBombChoices();
    
    /**
     * @param cost The cost of the bomb to be mounted
     * @return A location with sufficient space to mount the bomb, or Entity.LOC_NONE if the unit does not have the space.
     */
    int availableBombLocation(int cost);

    /**
     * Used by VTOLs and LAMs in airmech mode to declare the target hex for a bomb attack during the movement
     * phase.
     */
    
    default void setVTOLBombTarget(Targetable target) {}
    default Targetable getVTOLBombTarget() {
        return null;
    }
    default boolean isVTOLBombing() {
        return getVTOLBombTarget() != null;
    }

    // For convenience
    List<Mounted> getBombs();

    /**
     * @return The number of points taken up by all mounted bombs or other external stores.
     */
    default int getBombPoints() {
        int points = 0;
        for (Mounted bomb : getBombs()) {
            if (bomb.getUsableShotsLeft() > 0) {
                points += BombType.getBombCost(((BombType) bomb.getType()).getBombType());
            }
        }
        return points;
    }

    /**
     * Iterate through the bomb choices that were configured prior to deployment and add the corresponding
     * equipment.
     */
    default void applyBombs() {
        Game game = ((Entity) this).getGame();
        int gameTL = TechConstants.getSimpleLevel(game.getOptions().stringOption("techlevel"));
        Integer[] sorted = new Integer[BombType.B_NUM];
        // Apply the largest bombs first because we need to fit larger bombs into a single location
        // in LAMs.
        for (int i = 0; i < sorted.length; i++) {
            sorted[i] = i;
        }
        Arrays.sort(sorted, (a, b) -> BombType.bombCosts[b] - BombType.bombCosts[a]);
        for (int type : sorted) {
            for (int i = 0; i < getBombChoices()[type]; i++) {
                int loc = availableBombLocation(BombType.bombCosts[type]);
                if ((type == BombType.B_ALAMO)
                        && !game.getOptions().booleanOption(OptionsConstants.ADVAERORULES_AT2_NUKES)) {
                    continue;
                }
                if ((type > BombType.B_TAG)
                        && (gameTL < TechConstants.T_SIMPLE_ADVANCED)) {
                    continue;
                }

                // some bombs need an associated weapon and if so
                // they need a weapon for each bomb
                if (null != BombType.getBombWeaponName(type)) {
                    Mounted m = null;
                    try {
                        m = ((Entity) this).addBomb(EquipmentType.get(BombType
                                .getBombWeaponName(type)), loc);
                        // Add bomb itself as single-shot ammo.
                        if (type != BombType.B_TAG) {
                            Mounted ammo = new Mounted((Entity) this,
                                    EquipmentType.get(BombType.getBombInternalName(type)));
                            ammo.setShotsLeft(1);
                            m.setLinked(ammo);
                            ((Entity) this).addEquipment(ammo, loc, false);
                                                        
                        }
                    } catch (LocationFullException ignored) {

                    }
                } else {
                    try {
                        ((Entity) this).addEquipment(EquipmentType.get(BombType.getBombInternalName(type)),
                                loc, false);
                    } catch (LocationFullException ignored) {

                    }
                }
            }
        }
        clearBombChoices();
    }

    void clearBombs();
}

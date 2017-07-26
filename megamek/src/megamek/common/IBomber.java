/**
 * 
 */
package megamek.common;

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

    int getMaxBombPoints();
    int[] getBombChoices();
    void setBombChoices(int[] bc);
    int availableBombLocation();
            
    List<Mounted> getBombs();

    // I need a function that takes the bombChoices variable and uses it to
    // produce bombs
    default void applyBombs() {
        IGame game = ((Entity)this).getGame();
        int gameTL = TechConstants.getSimpleLevel(game.getOptions()
                .stringOption("techlevel"));
        for (int type = 0; type < BombType.B_NUM; type++) {
            for (int i = 0; i < getBombChoices()[type]; i++) {
                int loc = availableBombLocation();
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
                if ((null != BombType.getBombWeaponName(type))
                        && (type != BombType.B_ARROW)
                        && (type != BombType.B_HOMING)) {
                    try {
                        ((Entity)this).addBomb(EquipmentType.get(BombType
                                .getBombWeaponName(type)), loc);
                    } catch (LocationFullException ex) {
                        // throw new LocationFullException(ex.getMessage());
                    }
                }
                if (type != BombType.B_TAG) {
                    try {
                        ((Entity)this).addEquipment(EquipmentType.get(BombType
                                .getBombInternalName(type)), loc, false);
                    } catch (LocationFullException ex) {
                        // throw new LocationFullException(ex.getMessage());
                    }
                }
            }
            // Clear out the bomb choice once the bombs are loaded
            getBombChoices()[type] = 0;
        }

        if (this instanceof Aero) {
            ((Aero)this).updateWeaponGroups();
        }
        ((Entity)this).loadAllWeapons();
    }

}

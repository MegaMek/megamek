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

    int getMaxBombPoints();
    int[] getBombChoices();
    void setBombChoices(int[] bc);
    int availableBombLocation(int cost);
            
    List<Mounted> getBombs();

    default int getBombPoints() {
        int points = 0;
        for (Mounted bomb : getBombs()) {
            if (bomb.getUsableShotsLeft() > 0) {
                points += BombType.getBombCost(((BombType) bomb.getType()).getBombType());
            }
        }
        return points;
    }

    // I need a function that takes the bombChoices variable and uses it to
    // produce bombs
    default void applyBombs() {
        IGame game = ((Entity)this).getGame();
        int gameTL = TechConstants.getSimpleLevel(game.getOptions()
                .stringOption("techlevel"));
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
                if ((null != BombType.getBombWeaponName(type))
                        && (type != BombType.B_ARROW)
                        && (type != BombType.B_HOMING)) {
                    Mounted m = null;
                    try {
                        m = ((Entity)this).addBomb(EquipmentType.get(BombType
                                .getBombWeaponName(type)), loc);
                        // Add bomb itself as single-shot ammo.
                        if (type != BombType.B_TAG) {
                            Mounted ammo = new Mounted((Entity)this,
                                    EquipmentType.get(BombType.getBombInternalName(type)));
                            ammo.setShotsLeft(1);
                            m.setLinked(ammo);
                            // Oneshot ammo will be identified by having a location
                            // of null. Other areas in the code will rely on this.
                            ((Entity)this).addEquipment(ammo, Entity.LOC_NONE, false);
                            
                        }
                    } catch (LocationFullException ex) {
                        // throw new LocationFullException(ex.getMessage());
                    }
                } else {
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

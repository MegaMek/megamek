package megamek.common.util;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Iterator;

import megamek.common.Building;
import megamek.common.Coords;

/**
 * Building template, for placing on the map during map generation. Currently
 * used by mekwars to place objective buildings. Could also be used by an RMG
 * town builder
 * 
 * @author coelocanth
 */
public class BuildingTemplate implements Serializable {
    /**
     * 
     */
    private static final long serialVersionUID = -911419490135815472L;

    public static final int BASEMENT_RANDOM = -1;

    private ArrayList<Coords> coordsList = new ArrayList<Coords>();
    private int type = Building.LIGHT;
    private int CF = 15;
    private int height = 2;
    private int basement = BASEMENT_RANDOM;

    public BuildingTemplate(int type, ArrayList<Coords> coords) {
        this.type = type;
        coordsList = coords;
        CF = Building.getDefaultCF(type);
    }

    public BuildingTemplate(int type, ArrayList<Coords> coords, int CF,
            int height, int basement) {
        this.type = type;
        this.coordsList = coords;
        this.CF = CF;
        this.height = height;
        this.basement = basement;
    }

    /**
     * @return vector containing Coords of all hexes the building covers
     */
    public Iterator<Coords> getCoords() {
        return coordsList.iterator();
    }

    /**
     * @return type of the building (Building.LIGHT - Building.HARDENED)
     */
    public int getType() {
        return type;
    }

    /**
     * @return construction factor, used to initialise BLDG_CF
     */
    public int getCF() {
        return CF;
    }

    /**
     * @return height of the building, used to initialise BLDG_ELEV
     */
    public int getHeight() {
        return height;
    }

    /**
     * @return basement settings - basements arent implemented yet
     */
    public int getBasement() {
        return basement;
    }

    public boolean containsCoords(Coords c) {
        return coordsList.contains(c);
    }
}

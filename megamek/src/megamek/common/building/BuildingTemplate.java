package megamek.common.building;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import megamek.common.Coords;

// LEGAL (giorgiga) I'm not sure the above copyright is the correct one
//
// This file was originally in megamek.common.util, and didn't include a
// license header.

/**
 * Building template, for placing on the map during map generation. Currently
 * used by mekwars to place objective buildings. Could also be used by an RMG
 * town builder
 * 
 * @author coelocanth
 */
public class BuildingTemplate implements Serializable {

    private static final long serialVersionUID = -911419490135815472L;

    public BuildingTemplate(int type, ArrayList<Coords> coords, int cf, int height) {
        this.type = type;
        this.coordsList = coords;
        this.cf = cf;
        this.height = height;
    }

    private List<Coords> coordsList;
    private int type;
    private int cf;
    private int height;

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
        return cf;
    }

    /**
     * @return height of the building, used to initialise BLDG_ELEV
     */
    public int getHeight() {
        return height;
    }

    public boolean containsCoords(Coords c) {
        return coordsList.contains(c);
    }

}

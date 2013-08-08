/*
 * MegaMek - Copyright (C) 2000-2003 Ben Mazur (bmazur@sev.org)
 *
 *  This program is free software; you can redistribute it and/or modify it
 *  under the terms of the GNU General Public License as published by the Free
 *  Software Foundation; either version 2 of the License, or (at your option)
 *  any later version.
 *
 *  This program is distributed in the hope that it will be useful, but
 *  WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 *  or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License
 *  for more details.
 */

package megamek.common;

/**
 * This is a support vehicle
 *
 * @author beerockxs
 */
public class SupportTank extends Tank {

    /**
     *
     */
    private static final long serialVersionUID = -9028127010133768714L;
    private int[] barRating;

    public SupportTank() {
        super();
        barRating = new int[locations()];
    }

    public void setBARRating(int rating, int loc) {
        barRating[loc] = rating;
    }

    public void setBARRating(int rating) {
        for (int i = 0; i < locations(); i++) {
            barRating[i] = rating;
        }
    }


    /*
     * (non-Javadoc)
     *
     * @see megamek.common.Entity#getBARRating()
     */
    @Override
    public int getBARRating(int loc) {
        return barRating[loc];
    }

    /*
     * (non-Javadoc)
     *
     * @see megamek.common.Entity#hasBARArmor()
     */
    @Override
    public boolean hasBARArmor(int loc) {
        return true;
    }

    /*
     * (non-Javadoc)
     *
     * @see megamek.common.Entity#hasArmoredChassis()
     */
    @Override
    public boolean hasArmoredChassis() {
        for (Mounted misc : miscList) {
            if (misc.getType().hasFlag(MiscType.F_ARMORED_CHASSIS)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Tanks have all sorts of prohibited terrain.
     */
    @Override
    public boolean isHexProhibited(IHex hex) {
        if (hex.containsTerrain(Terrains.IMPASSABLE)) {
            return true;
        }
        switch (movementMode) {
            case TRACKED:
                return (hex.terrainLevel(Terrains.WOODS) > 1) || ((hex.terrainLevel(Terrains.WATER) > 0) && !hex.containsTerrain(Terrains.ICE) && !hasEnvironmentalSealing()) || hex.containsTerrain(Terrains.JUNGLE) || (hex.terrainLevel(Terrains.MAGMA) > 1);
            case WHEELED:
                return hex.containsTerrain(Terrains.WOODS) || hex.containsTerrain(Terrains.ROUGH) || ((hex.terrainLevel(Terrains.WATER) > 0) && !hex.containsTerrain(Terrains.ICE) && !hasEnvironmentalSealing()) || hex.containsTerrain(Terrains.RUBBLE) || hex.containsTerrain(Terrains.MAGMA) || hex.containsTerrain(Terrains.JUNGLE) || (hex.terrainLevel(Terrains.SNOW) > 1) || (hex.terrainLevel(Terrains.GEYSER) == 2);
            case HOVER:
                return hex.containsTerrain(Terrains.WOODS) || hex.containsTerrain(Terrains.JUNGLE) || (hex.terrainLevel(Terrains.MAGMA) > 1);
            case NAVAL:
            case HYDROFOIL:
                return (hex.terrainLevel(Terrains.WATER) <= 0) || hex.containsTerrain(Terrains.ICE);
            case SUBMARINE:
                return (hex.terrainLevel(Terrains.WATER) <= 0);
            case WIGE:
                return (hex.containsTerrain(Terrains.WOODS) || hex.containsTerrain(Terrains.BUILDING));
            default:
                return false;
        }
    }

    /*
     * (non-Javadoc)
     *
     * @see megamek.common.Entity#getTotalCommGearTons()
     */
    @Override
    public int getTotalCommGearTons() {
        return getExtraCommGearTons();
    }

    @Override
    public int getBattleForceSize() {
        //The tables are on page 356 of StartOps
        if (getWeight() < 5) {
            return 1;
        }
        int mediumCeil= 0;
        int largeCeil=0;
        int veryLargeCeil = 0;
        switch (movementMode) {
        case TRACKED:
            mediumCeil = 100;
            largeCeil = 200;
            break;
        case WHEELED:
            mediumCeil = 80;
            largeCeil = 160;
            break;
        case HOVER:
            mediumCeil = 50;
            largeCeil = 100;
            break;
        case NAVAL:
        case HYDROFOIL:
        case SUBMARINE:
            mediumCeil = 300;
            largeCeil = 6000;
            veryLargeCeil = 30000;
            break;
        case WIGE:
            mediumCeil = 80;
            largeCeil = 240;
            break;
        default:
            break;
        }
        if (getWeight() < mediumCeil) {
            return 2;
        }
        if (getWeight() < largeCeil) {
            return 3;
        }
        if ((getWeight() < veryLargeCeil) || (veryLargeCeil == 0)) {
            return 4;
        }
        return 5;
    }

    @Override
    /*
     * returns the battle force structure points for a mech
     */
    public int getBattleForceStructurePoints() {
        switch (movementMode) {
        case NAVAL:
        case HYDROFOIL:
        case SUBMARINE:
            if (getWeight() <= 300) {
                return 10;
            }
            if (getWeight() <= 500) {
                return 15;
            }
            if (getWeight() <= 6000) {
                return 20;
            }
            if (getWeight() <= 12000) {
                return 25;
            }
            if (getWeight() <= 30000) {
                return 30;
            }
            if (getWeight() <= 100000) {
                return 35;
            }
        default:
            //TODO add rail in here when ready
            return super.getBattleForceStructurePoints();
        }
    }

    public float getBaseEngineValue() {
        switch (movementMode) {
            /*case AIRSHIP:
                if (getWeight() < 5) {
                    return 0.005f;
                } else {
                    return 0.008f;
                }*/
            case HOVER:
                if (getWeight() < 5) {
                    return 0.0025f;
                } else if (!isSuperHeavy()) {
                    return 0.004f;
                } else {
                    return 0.008f;
                }
            case NAVAL:
            case HYDROFOIL:
            case SUBMARINE:
                if (getWeight() < 5) {
                    return 0.004f;
                } else {
                    return 0.007f;
                }
            case TRACKED:
                if (getWeight() < 5) {
                    return 0.006f;
                } else if (!isSuperHeavy()) {
                    return 0.013f;
                } else {
                    return 0.025f;
                }
            case WHEELED:
                if (getWeight() < 5) {
                    return 0.0025f;
                } else if (!isSuperHeavy()) {
                    return 0.0075f;
                } else {
                    return 0.015f;
                }
            case WIGE:
                if (getWeight() < 5) {
                    return 0.003f;
                } else if (!isSuperHeavy()) {
                    return 0.005f;
                } else {
                    return 0.006f;
                }
           default:
               return 0f;
        }
    }

    public float getBaseChassisValue() {
        switch (movementMode) {
            /*case AIRSHIP:
                if (getWeight() < 5) {
                    return 0.2f;
                } else {
                    return 0.25f;
                }*/
            case HOVER:
                if (getWeight() < 5) {
                    return 0.2f;
                } else if (!isSuperHeavy()) {
                    return 0.25f;
                } else {
                    return 0.3f;
                }
            case NAVAL:
            case HYDROFOIL:
            case SUBMARINE:
                if (getWeight() < 5) {
                    return 0.12f;
                } else {
                    return 0.15f;
                }
            case TRACKED:
                if (getWeight() < 5) {
                    return 0.13f;
                } else if (!isSuperHeavy()) {
                    return 0.15f;
                } else {
                    return 0.25f;
                }
            case WHEELED:
                if (getWeight() < 5) {
                    return 0.12f;
                } else if (!isSuperHeavy()) {
                    return 0.15f;
                } else {
                    return 0.18f;
                }
            case WIGE:
                if (getWeight() < 5) {
                    return 0.12f;
                } else if (!isSuperHeavy()) {
                    return 0.15f;
                } else {
                    return 0.17f;
                }
           default:
               return 0f;
        }
    }

    @Override
    public int getTotalSlots() {
        return 5 + (int) Math.floor(getWeight() / 10);
    }

    public long getEntityType(){
        return Entity.ETYPE_TANK | Entity.ETYPE_SUPPORT_TANK;
    }

}

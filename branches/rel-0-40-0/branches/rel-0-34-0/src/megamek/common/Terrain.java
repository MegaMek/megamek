/*
 * MegaMek - Copyright (C) 2000-2002 Ben Mazur (bmazur@sev.org)
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

import java.io.Serializable;

/**
 * Represents a single type of terrain or condition in a hex. The type of a
 * terrain is immutable, once created, but the level and exits are changable.
 * Each type of terrain should only be represented once in a hex.
 *
 * @author Ben
 */
public class Terrain implements ITerrain, Serializable {

    /**
     *
     */
    private static final long serialVersionUID = -7624691566755134033L;
    private final int type;
    private int level;
    private boolean exitsSpecified = false;
    private int exits;
    private int terrainFactor;

    /**
     * Terrain constructor
     */
    public Terrain(int type, int level) {
        this(type, level, false, 0);
    }

    public Terrain(int type, int level, boolean exitsSpecified, int exits) {
        this.type = type;
        this.level = level;
        this.exitsSpecified = exitsSpecified;
        this.exits = exits;
        terrainFactor = Terrains.getTerrainFactor(type, level);
    }

    public Terrain(ITerrain other) {
        type = other.getType();
        level = other.getLevel();
        exitsSpecified = other.hasExitsSpecified();
        exits = other.getExits();
        terrainFactor = other.getTerrainFactor();
    }

    /**
     * Parses a string containing terrain info into the actual terrain
     */
    public Terrain(String terrain) {
        // should have at least one colon, maybe two
        int firstColon = terrain.indexOf(':');
        int lastColon = terrain.lastIndexOf(':');
        String name = terrain.substring(0, firstColon);

        type = Terrains.getType(name);
        if (firstColon == lastColon) {
            level = levelFor(terrain.substring(firstColon + 1));
            exitsSpecified = false;

            // Buildings *never* use implicit exits.
            if ((type == Terrains.BUILDING)
                    || (type == Terrains.FUEL_TANK)) {
                exitsSpecified = true;
            }
        } else {
            level = levelFor(terrain.substring(firstColon + 1, lastColon));
            exitsSpecified = true;
            exits = levelFor(terrain.substring(lastColon + 1));
        }
        terrainFactor = Terrains.getTerrainFactor(type, level);
    }

    public static int levelFor(String string) {
        if (string.equals("*")) {
            return WILDCARD;
        }
        return Integer.parseInt(string);
    }

    public int getType() {
        return type;
    }

    public int getLevel() {
        return level;
    }

    public int getTerrainFactor() {
        return terrainFactor;
    }

    public void setTerrainFactor(int tf) {
        terrainFactor = tf;
    }

    public int getExits() {
        return exits;
    }

    public boolean hasExitsSpecified() {
        return exitsSpecified;
    }

    public void setExits(int exits) {
        this.exits = exits;
    }

    public void setExit(int direction, boolean connection) {
        int mask = (int) Math.pow(2, direction);
        if (connection) {
            exits |= mask;
        } else {
            exits &= (63 ^ mask);
        }
    }

    /**
     * Flips the exits around the vertical axis (North-for-South) and/or the
     * horizontal axis (East-for-West).
     *
     * @param horiz - a <code>boolean</code> value that, if <code>true</code>,
     *            indicates that the exits are being flipped North-for-South.
     * @param vert - a <code>boolean</code> value that, if <code>true</code>,
     *            indicates that the exits are being flipped East-for-West.
     */
    public void flipExits(boolean horiz, boolean vert) {
        // Do nothing if no flips are defined.
        if (!horiz && !vert) {
            return;
        }

        // Determine the new exits.
        int newExits = 0;

        // Is there a North exit?
        if (0 != (exits & 0x0001)) {
            if (vert) {
                // Becomes South.
                newExits |= 0x08;
            }
        }
        // Is there a North-East exit?
        if (0 != (exits & 0x0002)) {
            if (vert && horiz) {
                // Becomes South-West
                newExits |= 0x10;
            } else if (horiz) {
                // Becomes North-West.
                newExits |= 0x20;
            } else if (vert) {
                // Becomes South-East.
                newExits |= 0x04;
            }
        }
        // Is there a South-East exit?
        if (0 != (exits & 0x0004)) {
            if (vert && horiz) {
                // Becomes North-West
                newExits |= 0x20;
            } else if (horiz) {
                // Becomes South-West.
                newExits |= 0x10;
            } else if (vert) {
                // Becomes North-East.
                newExits |= 0x02;
            }
        }
        // Is there a South exit?
        if (0 != (exits & 0x0008)) {
            if (vert) {
                // Becomes North.
                newExits |= 0x01;
            }
        }
        // Is there a South-West exit?
        if (0 != (exits & 0x0010)) {
            if (vert && horiz) {
                // Becomes North-East
                newExits |= 0x02;
            } else if (horiz) {
                // Becomes South-East.
                newExits |= 0x04;
            } else if (vert) {
                // Becomes North-West.
                newExits |= 0x20;
            }
        }
        // Is there a North-West exit?
        if (0 != (exits & 0x0020)) {
            if (vert && horiz) {
                // Becomes South-East
                newExits |= 0x04;
            } else if (horiz) {
                // Becomes North-East.
                newExits |= 0x02;
            } else if (vert) {
                // Becomes South-West.
                newExits |= 0x10;
            }
        }

        // Update the exits.
        setExits(newExits);

    }

    /**
     * Returns true if the terrain in this hex exits to the terrain in the other
     * hex.
     */
    public boolean exitsTo(ITerrain other) {
        if (other == null) {
            return false;
        }
        return (type == other.getType()) && (level == other.getLevel());
    }

    /**
     * Terrains are equal if their types and levels are equal. Does not pay
     * attention to exits.
     */
    @Override
    public boolean equals(Object object) {
        if (this == object) {
            return true;
        } else if ((object == null) || !(object instanceof ITerrain)) {
            return false;
        }
        ITerrain other = (ITerrain) object;
        return (type == other.getType()) && (level == other.getLevel());
    }

    @Override
    public String toString() {
        return Terrains.getName(type) + ":" + level
                + (exitsSpecified ? ":" + exits : "");
    }

    public int pilotingModifier(int moveType) {
        switch (type) {
            case Terrains.JUNGLE:
                return level;
            case Terrains.MAGMA:
                return (level == 2) ? 4 : 1;
            case Terrains.TUNDRA:
            case Terrains.SAND:
                return 1;
            case Terrains.SNOW:
                return (level == 2) ? 1 : 0;
            case Terrains.SWAMP:
                if ((moveType == IEntityMovementMode.HOVER) || (moveType == IEntityMovementMode.WIGE)) {
                    return 0;
                } else if((moveType == IEntityMovementMode.BIPED) || (moveType== IEntityMovementMode.QUAD)) {
                    return 1;
                } else {
                    return 2;
                }
            case Terrains.MUD:
                if((moveType == IEntityMovementMode.BIPED) || (moveType== IEntityMovementMode.QUAD)
                        || (moveType == IEntityMovementMode.HOVER) || (moveType == IEntityMovementMode.WIGE)) {
                    return 0;
                } else {
                    return 1;
                }
            case Terrains.GEYSER:
            case Terrains.RUBBLE:
                if (level == 2) {
                    return 1;
                }
                return 0;
            case Terrains.RAPIDS:
                if(level == 2) {
                    return 3;
                } else {
                    return 2;
                }
            case Terrains.ICE:
                if ((moveType == IEntityMovementMode.HOVER) || (moveType == IEntityMovementMode.WIGE)) {
                    return 0;
                } else {
                    return 4;
                }
            case Terrains.INDUSTRIAL:
                return 1;
            default:
                return 0;
        }
    }

    public int movementCost(int moveType) {
        switch (type) {
            case Terrains.MAGMA:
                return level - 1;
            case Terrains.GEYSER:
                if (level == 2) {
                    return 1;
                }
                return 0;
            case Terrains.RUBBLE:
                return 1;
            case Terrains.WOODS:
                return level;
            case Terrains.JUNGLE:
                return level + 1;
            case Terrains.SNOW:
                if(level == 2) {
                    if((moveType == IEntityMovementMode.HOVER) || (moveType == IEntityMovementMode.WIGE)) {
                        return 0;
                    } else {
                        return 1;
                    }
                }
                if ((moveType == IEntityMovementMode.WHEELED)
                        || (moveType == IEntityMovementMode.INF_JUMP)
                        || (moveType == IEntityMovementMode.INF_LEG)
                        || (moveType == IEntityMovementMode.INF_MOTORIZED)) {
                    return 1;
                }
                return 0;
            case Terrains.MUD:
                if((moveType == IEntityMovementMode.BIPED) || (moveType== IEntityMovementMode.QUAD)
                        || (moveType == IEntityMovementMode.HOVER) || (moveType == IEntityMovementMode.WIGE)) {
                    return 0;
                } else {
                    return 1;
                }
            case Terrains.SWAMP:
                if ((moveType == IEntityMovementMode.HOVER) || (moveType == IEntityMovementMode.WIGE)) {
                    return 0;
                } else if((moveType == IEntityMovementMode.BIPED) || (moveType== IEntityMovementMode.QUAD)) {
                    return 1;
                } else {
                    return 2;
                }
            case Terrains.ICE:
                if ((moveType == IEntityMovementMode.HOVER) || (moveType == IEntityMovementMode.WIGE)) {
                    return 0;
                } else {
                    return 1;
                }
            case Terrains.RAPIDS:
            case Terrains.ROUGH:
                if(level == 2) {
                    return 2;
                } else {
                    return 1;
                }
            case Terrains.SAND:
                if ((moveType == IEntityMovementMode.WHEELED)
                        || (moveType == IEntityMovementMode.INF_JUMP)
                        || (moveType == IEntityMovementMode.INF_LEG)
                        || (moveType == IEntityMovementMode.INF_MOTORIZED)) {
                    return 1;
                }
                return 0;
            case Terrains.INDUSTRIAL:
                if((moveType == IEntityMovementMode.BIPED) || (moveType== IEntityMovementMode.QUAD)) {
                    return 1;
                } else {
                    return 0;
                }
            default:
                return 0;
        }
    }

    public int ignitionModifier() {
        switch (type) {
            case Terrains.JUNGLE:
                return 1;
            case Terrains.SNOW:
                if(level == 2) {
                    return 2;
                } else {
                    return 0;
                }
            case Terrains.FIELDS:
                return -1;
            default:
                return 0;
        }
    }

    public int getBogDownModifier(int moveType, boolean largeVee) {
        if((moveType == IEntityMovementMode.HOVER) || (moveType == IEntityMovementMode.WIGE)) {
            return TargetRoll.AUTOMATIC_SUCCESS;
        }
        switch (type) {
        case(Terrains.SWAMP):
            //if this is quicksand, then you automatically fail
            if(level > 1) {
                return TargetRoll.AUTOMATIC_FAIL;
            }
            if(moveType == IEntityMovementMode.VTOL) {
                return TargetRoll.AUTOMATIC_FAIL;
            } else {
                return 0;
            }
        case(Terrains.MAGMA):
            if(level == 2) {
                return 0;
            } else {
                return TargetRoll.AUTOMATIC_SUCCESS;
            }
        case(Terrains.MUD):
            if((moveType == IEntityMovementMode.BIPED) || (moveType== IEntityMovementMode.QUAD)) {
                return TargetRoll.AUTOMATIC_SUCCESS;
            }
        case(Terrains.TUNDRA):
            return -1;
        case(Terrains.SNOW):
            if(level == 2) {
                return -1;
            } else {
                return TargetRoll.AUTOMATIC_SUCCESS;
            }
        case(Terrains.SAND):
            if(largeVee) {
                return 0;
            } else {
                return TargetRoll.AUTOMATIC_SUCCESS;
            }
        default:
            return TargetRoll.AUTOMATIC_SUCCESS;
        }
    }

    public int getUnstuckModifier(int elev) {
        switch (type) {
        case(Terrains.SWAMP):
            if(level > 1) {
                return 3 + ((-3)*elev);
            } else {
                return 0;
            }
        case(Terrains.TUNDRA):
            return -1;
        case(Terrains.SNOW):
                return -1;
        default:
            return 0;
        }
    }

}

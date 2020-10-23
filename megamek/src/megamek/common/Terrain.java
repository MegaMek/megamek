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
import java.util.Objects;

import megamek.common.options.OptionsConstants;

/**
 * Represents a single type of terrain or condition in a hex. The type of a
 * terrain is immutable, once created, but the level and exits are changeable.
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
    /**
     * Terrain level, which is used to indicate varying severity of terrain
     * types (ie, Light Woods vs Heavy woods). Not to be confused with Hex
     * levels.
     */
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
            if ((type == Terrains.BUILDING) || (type == Terrains.FUEL_TANK)) {
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

    public int getTerrainElevation(boolean inAtmosphere) {
        return Terrains.getTerrainElevation(type, level, inAtmosphere);
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
     * @param horiz
     *            - a <code>boolean</code> value that, if <code>true</code>,
     *            indicates that the exits are being flipped North-for-South.
     * @param vert
     *            - a <code>boolean</code> value that, if <code>true</code>,
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
        // Check to see if we've got a type that can have exits
        boolean exitableTerrainType = Terrains.exitableTerrain(type);
        return (type == other.getType()) && exitableTerrainType && (level == other.getLevel());
    }

    @Override
    public int hashCode() {
        return Objects.hash(level, type);
    }

    /**
     * Terrains are equal if their types and levels are equal. Does not pay
     * attention to exits.
     */
    @Override
    public boolean equals(Object object) {
        if (this == object) {
            return true;
        } else if ((object == null) || (getClass() != object.getClass())) {
            return false;
        }
        final Terrain other = (Terrain) object;
        return (type == other.type) && (level == other.level);
        // Ints don't need special handling. For more complex objects use:
        // return Objects.equals(level, other.level) && Objects.equals(type,
        // other.type);
    }

    @Override
    public String toString() {
        return Terrains.getName(type) + ":" + level + (exitsSpecified ? ":" + exits : "");
    }

    public void pilotingModifier(EntityMovementMode moveMode, PilotingRollData roll, boolean enteringRubble) {
        switch (type) {
        case Terrains.JUNGLE:
            if (level == 3) {
                roll.addModifier(level, "Ultra Jungle");
            }
            if (level == 2) {
                roll.addModifier(level, "Heavy Jungle");
            }
            if (level == 1) {
                roll.addModifier(level, "Jungle");
            }
            break;
        case Terrains.MAGMA:
            if (level == 2) {
                roll.addModifier(4, "Liquid Magma");
            }
            if (level == 1) {
                roll.addModifier(1, "Magma Crust");
            }
            break;
        case Terrains.TUNDRA:
            roll.addModifier(1, "Tundra");
            break;
        case Terrains.SAND:
            roll.addModifier(1, "Sand");
            break;
        case Terrains.SNOW:
            if (level == 2) {
                roll.addModifier(1, "Deep Snow");
            }
            break;
        case Terrains.SWAMP:
            if ((moveMode == EntityMovementMode.BIPED) || (moveMode == EntityMovementMode.QUAD)) {
                roll.addModifier(1, "Swamp");
            } else {
                roll.addModifier(2, "Swamp");
            }
            break;
        case Terrains.MUD:
            if ((moveMode != EntityMovementMode.HOVER) && (moveMode != EntityMovementMode.WIGE)) {
                roll.addModifier(1, "Mud");
            }
            break;
        case Terrains.GEYSER:
            if (level == 2) {
                roll.addModifier(1, "Active Geyser");
                break;
            }
        case Terrains.RUBBLE:
            if (level == 6) {
                if (enteringRubble) {
                    roll.addModifier(1, "entering Ultra Rubble");
                } else {
                    roll.addModifier(1, "Ultra Rubble");

                }
            }
            if (level < 6) {
                if (enteringRubble) {
                    roll.addModifier(0, "entering Rubble");
                } else {
                    roll.addModifier(0, "Rubble");
                }
            }
            break;
        case Terrains.RAPIDS:
            if (level == 2) {
                roll.addModifier(3, "Torrent");
            } else {
                roll.addModifier(2, "Rapids");
            }
            break;
        case Terrains.ICE:
            if ((moveMode != EntityMovementMode.HOVER) && (moveMode != EntityMovementMode.WIGE)) {
                roll.addModifier(4, "Ice");
            }
            break;
        case Terrains.INDUSTRIAL:
            roll.addModifier(1, "Industrial Zone");
            break;
        default:
            break;
        }
    }

    public int movementCost(Entity e) {
        EntityMovementMode moveMode = e.getMovementMode();
        int mp = 0;
        switch (type) {
            case Terrains.MAGMA:
                return level - 1;
            case Terrains.GEYSER:
                if (level == 2) {
                    return 1;
                }
                return 0;
            case Terrains.RUBBLE:
                if (level == 6) {
                    mp = 2;
                } else {
                    mp = 1;
                }
                if ((e instanceof Mech) && ((Mech)e).isSuperHeavy()) {
                    mp -= 1;
                }
                if (e.hasAbility(OptionsConstants.PILOT_TM_MOUNTAINEER)) {
                    mp -= 1;
                }
                if ((e.hasAbility(OptionsConstants.INFANTRY_FOOT_CAV)
                                && (moveMode == EntityMovementMode.INF_LEG))) {
                    mp -= 1;
                }
                return Math.max(0, mp);
            case Terrains.WOODS:
                mp = level;
                if ((e instanceof Mech) && ((Mech)e).isSuperHeavy()) {
                    mp -= 1;
                }
                if (e.hasAbility(OptionsConstants.PILOT_TM_FOREST_RANGER)) {
                    mp -= 1;
                }
                if ((e.hasAbility(OptionsConstants.INFANTRY_FOOT_CAV)
                                && (moveMode == EntityMovementMode.INF_LEG))) {
                    mp -= 1;
                }
                if (e.hasAbility(OptionsConstants.PILOT_ANIMAL_MIMIC)) {
                    if ((e.entityIsQuad()) || ((moveMode == EntityMovementMode.BIPED) && e.hasQuirk("animalistic"))) {
                        mp -= 1;
                    }
                }
                return Math.max(0, mp);
            case Terrains.JUNGLE:
                mp = level +1;
                if ((e instanceof Mech) && ((Mech)e).isSuperHeavy()) {
                    mp -= 1;
                }
                if (e.hasAbility(OptionsConstants.PILOT_TM_FOREST_RANGER)) {
                    mp -= 1;
                }
                if ((e.hasAbility(OptionsConstants.INFANTRY_FOOT_CAV)
                                && (moveMode == EntityMovementMode.INF_LEG))) {
                    mp -= 1;
                }
                if (e.hasAbility(OptionsConstants.PILOT_ANIMAL_MIMIC)) {
                    if ((e.entityIsQuad()) || ((moveMode == EntityMovementMode.BIPED) && e.hasQuirk("animalistic"))) {
                        mp -= 1;
                    }
                }
                return Math.max(0, mp);
            case Terrains.SNOW:
                if (level == 2) {
                    if ((moveMode == EntityMovementMode.HOVER) || (moveMode == EntityMovementMode.WIGE)) {
                        return 0;
                    }
                    return 1;
                }
                if ((moveMode == EntityMovementMode.WHEELED) || (moveMode == EntityMovementMode.INF_JUMP)
                        || (moveMode == EntityMovementMode.INF_LEG) || (moveMode == EntityMovementMode.INF_MOTORIZED)) {
                    return 1;
                }
                return 0;
            case Terrains.MUD:
                if ((moveMode == EntityMovementMode.HOVER) || (moveMode == EntityMovementMode.WIGE)) {
                    return 0;
                }
                if (e.hasAbility(OptionsConstants.PILOT_TM_SWAMP_BEAST)) {
                    return 0;
                }
                return 1;
            case Terrains.SWAMP:
                mp = 2;
                if ((moveMode == EntityMovementMode.HOVER) || (moveMode == EntityMovementMode.WIGE)) {
                    return 0;
                }
                if (e.hasAbility(OptionsConstants.PILOT_TM_SWAMP_BEAST)) {
                    mp -= 1;
                }
                if ((moveMode == EntityMovementMode.BIPED) || (moveMode == EntityMovementMode.QUAD)) {
                    mp -= 1;
                }
                return Math.max(0, mp);
            case Terrains.ICE:
                if ((moveMode == EntityMovementMode.HOVER) || (moveMode == EntityMovementMode.WIGE)) {
                    return 0;
                }
                return 1;
            case Terrains.RAPIDS:
                // Doesn't apply to Hover, or airborne WiGE or VTOL
                if (e.isAirborneVTOLorWIGE() || (e.getMovementMode() == EntityMovementMode.HOVER)) {
                    return 0;
                }
                if (level == 2) {
                    mp = 2;
                } else {
                    mp = 1;
                }
                if ((e instanceof Mech) && ((Mech) e).isSuperHeavy()) {
                    mp -= 1;
                }
                return Math.max(0, mp);
            case Terrains.ROUGH:
                if (level == 2) {
                    mp = 2;
                } else {
                    mp = 1;
                }
                if ((e instanceof Mech) && ((Mech)e).isSuperHeavy()) {
                    mp -= 1;
                }
                if (e.hasAbility(OptionsConstants.PILOT_TM_MOUNTAINEER)) {
                    mp -= 1;
                }
                if ((e.hasAbility(OptionsConstants.INFANTRY_FOOT_CAV)
                                && (moveMode == EntityMovementMode.INF_LEG))) {
                    mp -= 1;
                }
                return Math.max(0, mp);
            case Terrains.SAND:
                if (((moveMode == EntityMovementMode.WHEELED) && !e.hasWorkingMisc(MiscType.F_DUNE_BUGGY))
                        || (moveMode == EntityMovementMode.INF_JUMP) || (moveMode == EntityMovementMode.INF_LEG)
                        || (moveMode == EntityMovementMode.INF_MOTORIZED)) {
                    return 1;
                }
                return 0;
            case Terrains.INDUSTRIAL:
                if ((moveMode == EntityMovementMode.BIPED) || (moveMode == EntityMovementMode.QUAD)) {
                    return 1;
                }
                return 0;
            default:
                return 0;
        }
    }

    public int ignitionModifier() {
        switch (type) {
        case Terrains.JUNGLE:
            return 1;
        case Terrains.SNOW:
            if (level == 2) {
                return 2;
            }
            return 0;
        case Terrains.FIELDS:
            return -1;
        default:
            return 0;
        }
    }

    public int getBogDownModifier(EntityMovementMode moveMode, boolean largeVee) {
        if ((moveMode == EntityMovementMode.HOVER) || (moveMode == EntityMovementMode.WIGE)) {
            return TargetRoll.AUTOMATIC_SUCCESS;
        }
        switch (type) {
        case (Terrains.SWAMP):
            // if this is quicksand, then you automatically fail
            if (level > 1) {
                return TargetRoll.AUTOMATIC_FAIL;
            }
            if (moveMode == EntityMovementMode.VTOL) {
                return TargetRoll.AUTOMATIC_FAIL;
            }
            return 0;
        case (Terrains.MAGMA):
            if (level == 2) {
                return 0;
            }
            return TargetRoll.AUTOMATIC_SUCCESS;
        case (Terrains.MUD):
            if ((moveMode == EntityMovementMode.BIPED) || (moveMode == EntityMovementMode.QUAD)) {
                return TargetRoll.AUTOMATIC_SUCCESS;
                // any kind of infantry just gets a flat roll
            } else if(moveMode == EntityMovementMode.INF_LEG || moveMode == EntityMovementMode.INF_MOTORIZED ||
                    moveMode == EntityMovementMode.INF_JUMP || moveMode == EntityMovementMode.INF_UMU) {
                return 0;
            }
            return -1;
        case (Terrains.TUNDRA):
            return -1;
        case (Terrains.SNOW):
            if (level == 2) {
                return -1;
            }
            return TargetRoll.AUTOMATIC_SUCCESS;
        case (Terrains.SAND):
            if (largeVee) {
                return 0;
            }
            return TargetRoll.AUTOMATIC_SUCCESS;
        default:
            return TargetRoll.AUTOMATIC_SUCCESS;
        }
    }

    public void getUnstuckModifier(int elev, PilotingRollData rollTarget) {
        switch (type) {
        case (Terrains.SWAMP):
            if (level > 1) {
                rollTarget.addModifier((3 + ((-3) * elev)), "Quicksand");
                break;
            }
            rollTarget.addModifier(0, "Swamp");
            break;
        case (Terrains.MAGMA):
            if (level == 2) {
                rollTarget.addModifier(0, "Liquid Magma");
            }
            break;
        case (Terrains.MUD):
            rollTarget.addModifier(-1, "Mud");
            break;
        case (Terrains.TUNDRA):
            rollTarget.addModifier(-1, "Tundra");
            break;
        case (Terrains.SNOW):
            rollTarget.addModifier(-1, "Deep Snow");
            break;
        default:
            break;
        }
    }

    public boolean isValid(StringBuffer errBuff) {
        boolean rv = true;
        if (type == Terrains.WOODS && (level < 1 || level > 3)) {
            rv = false;
        } else if (type == Terrains.SWAMP && (level < 1 || level > 3)) {
            rv = false;
        } else if (type == Terrains.ROUGH && (level < 1 || level > 2)) {
            rv = false;
        } else if (type == Terrains.JUNGLE && (level < 1 || level > 3)) {
            rv = false;
        } else if (type == Terrains.WATER && (level < 0)) {
            rv = false;
        } else if (type == Terrains.RAPIDS && (level < 1 || level > 2)) {
            rv = false;
        } else if (type == Terrains.ICE && level != 1) {
            rv = false;
        } else if (type == Terrains.GEYSER && (level < 1 || level > 3)) {
            rv = false;
        } else if (type == Terrains.FORTIFIED && level != 1) {
            rv = false;
        } else if (type == Terrains.RUBBLE && (level < 1 || level > 6)) {
            rv = false;
        } else if (type == Terrains.FIRE && (level < 1 || level > 4)) {
            rv = false;
        } else if (type == Terrains.SMOKE && (level < 1 || level > 5)) {
            rv = false;
        } else if (type == Terrains.MAGMA && (level < 1 || level > 2)) {
            rv = false;
        } else if (type == Terrains.MUD && (level < 1 || level > 2)) {
            rv = false;
        } else if (type == Terrains.PAVEMENT && level < 1) {
            rv = false;
        } else if (type == Terrains.SNOW && (level < 1 || level > 2)) {
            rv = false;
        } else if (type == Terrains.TUNDRA && level != 1) {
            rv = false;
        } else if (type == Terrains.BRIDGE && level < 1) {
            rv = false;
        } else if (type == Terrains.FOLIAGE_ELEV && (level < 1 || level > 3)) {
            rv = false;
        } else if ((type == Terrains.BLDG_ELEV) && (level < 1)) {
            rv = false;
        } else if ((type == Terrains.BRIDGE_ELEV) && (level < 0)) {
            rv = false;
        }

        if (!rv && (errBuff != null)) {
            errBuff.append("Illegal level! For " + toString() + "\n");
        }

        return rv;
    }
}

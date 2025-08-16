/*
 * Copyright (C) 2005 Ben Mazur (bmazur@sev.org)
 * Copyright (C) 2025 The MegaMek Team. All Rights Reserved.
 *
 * This file is part of MegaMek.
 *
 * MegaMek is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License (GPL),
 * version 3 or (at your option) any later version,
 * as published by the Free Software Foundation.
 *
 * MegaMek is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty
 * of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU General Public License for more details.
 *
 * A copy of the GPL should have been included with this project;
 * if not, see <https://www.gnu.org/licenses/>.
 *
 * NOTICE: The MegaMek organization is a non-profit group of volunteers
 * creating free software for the BattleTech community.
 *
 * MechWarrior, BattleMech, `Mech and AeroTech are registered trademarks
 * of The Topps Company, Inc. All Rights Reserved.
 *
 * Catalyst Game Labs and the Catalyst Game Labs logo are trademarks of
 * InMediaRes Productions, LLC.
 *
 * MechWarrior Copyright Microsoft Corporation. MegaMek was created under
 * Microsoft's "Game Content Usage Rules"
 * <https://www.xbox.com/en-US/developers/rules> and it is not endorsed by or
 * affiliated with Microsoft.
 */

package megamek.common.net.packets;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Vector;

import megamek.common.Building;
import megamek.common.Coords;
import megamek.common.Entity;
import megamek.common.Minefield;
import megamek.common.UnitLocation;
import megamek.common.annotations.Nullable;
import megamek.common.force.Force;
import megamek.common.force.Forces;
import megamek.common.net.enums.PacketCommand;

/**
 * Application layer data packet used to exchange information between client and server.
 */
public record Packet(PacketCommand command, Object... data) implements Serializable {
    /**
     * Creates a <code>Packet</code> with a command and an array of objects
     *
     */
    public Packet {
    }

    /**
     * @return the command associated.
     */
    @Override
    public PacketCommand command() {
        return command;
    }

    /**
     * @return the data in the packet
     */
    @Override
    public Object[] data() {
        return data;
    }

    /**
     * @param index the index of the desired object
     *
     * @return the object at the specified index
     */
    public @Nullable Object getObject(final int index) {
        return (index >= 0 && index < data.length) ? data[index] : null;
    }

    /**
     * @param index the index of the desired object
     *
     * @return the <code>int</code> value of the object at the specified index
     */
    public int getIntValue(int index) {
        Object object = getObject(index);

        if (object instanceof Integer integer) {
            return integer;
        }

        return 0;
    }

    /**
     * @param index the index of the desired object
     *
     * @return a List of {@link Integer}'s value of the object at the specified index
     */
    public List<Integer> getIntList(int index) {
        Object object = getObject(index);

        ArrayList<Integer> result = new ArrayList<>();

        if (object instanceof List<?> list) {
            for (Object integer : list) {
                if (integer instanceof Integer verifiedInt) {
                    result.add(verifiedInt);
                }
            }
        }

        return result;
    }

    /**
     * @param index the index of the desired object
     *
     * @return the <code>boolean</code> value of the object at the specified index
     */
    public boolean getBooleanValue(int index) {
        Object object = getObject(index);

        if (object instanceof Boolean bool) {
            return bool;
        }

        return false;
    }

    /**
     * @param index the index of the desired object
     *
     * @return the <code>String</code> value of the object at the specified index
     */
    public String getStringValue(int index) {
        Object object = getObject(index);

        if (object instanceof String string) {
            return string;
        }

        return "";
    }

    /**
     * @param index the index of the desired object
     *
     * @return a List of {@link megamek.common.Building}'s value of the object at the specified index
     */
    public List<Building> getBuildingList(int index) {
        Object object = getObject(index);

        ArrayList<Building> result = new ArrayList<>();

        if (object instanceof Collection<?> collection) {
            for (Object building : collection) {
                if (building instanceof Building verifiedBuilding) {
                    result.add(verifiedBuilding);
                }
            }
        }

        return result;
    }

    /**
     * @param index the index of the desired object
     *
     * @return a Vector of {@link megamek.common.Coords}'s value of the object at the specified index
     */
    public Vector<Coords> getCoordsVector(int index) {
        Object object = getObject(index);

        ArrayList<Coords> result = new ArrayList<>();

        if (object instanceof Collection<?> collection) {
            for (Object coord : collection) {
                if (coord instanceof Coords verifiedCoord) {
                    result.add(verifiedCoord);
                }
            }
        }

        return result;
    }

    /**
     * @param index the index of the desired object
     *
     * @return the {@link Entity} value of the object at the specified index
     */
    public @Nullable Entity getEntity(int index) {
        Object object = getObject(index);

        if (object instanceof Entity entity) {
            return entity;
        }

        return null;
    }

    /**
     * @param index the index of the desired object
     *
     * @return a List of {@link Entity}'s value of the object at the specified index
     */
    public List<Entity> getEntityList(int index) {
        Object object = getObject(index);

        ArrayList<Entity> result = new ArrayList<>();

        if (object instanceof List<?> list) {
            for (Object entity : list) {
                if (entity instanceof Entity verifiedEntity) {
                    result.add(verifiedEntity);
                }
            }
        }

        return result;
    }

    /**
     * @param index the index of the desired object
     *
     * @return the {@link megamek.common.force.Forces} value of the object at the specified index
     */
    public @Nullable Forces getForces(int index) {
        Object object = getObject(index);

        if (object instanceof Forces force) {
            return force;
        }

        return null;
    }

    /**
     * @param index the index of the desired object
     *
     * @return a List of {@link Force}'s value of the object at the specified index
     */
    public List<Force> getForceList(int index) {
        Object object = getObject(index);

        ArrayList<Force> result = new ArrayList<>();

        if (object instanceof Collection<?> collection) {
            for (Object forces : collection) {
                if (forces instanceof Force verifiedForce) {
                    result.add(verifiedForce);
                }
            }
        }

        return result;
    }

    /**
     * @param index the index of the desired object
     *
     * @return a Vector of {@link UnitLocation}'s value of the object at the specified index
     */
    public Vector<UnitLocation> getUnitLocationVector(int index) {
        Object object = getObject(index);

        Vector<UnitLocation> result = new Vector<>();

        if (object instanceof Vector<?> vector) {
            for (Object unitLocation : vector) {
                if (unitLocation instanceof UnitLocation verifiedLocation) {
                    result.add(verifiedLocation);
                }
            }
        }

        return result;
    }

    /**
     * @param index the index of the desired object
     *
     * @return a Vector of {@link Minefield}'s value of the object at the specified index
     */
    public Vector<Minefield> getMinefieldVector(int index) {
        Object object = getObject(index);

        Vector<Minefield> result = new Vector<>();

        if (object instanceof Vector<?> vector) {
            for (Object minefield : vector) {
                if (minefield instanceof Minefield verifiedMinefield) {
                    result.add(verifiedMinefield);
                }
            }
        }

        return result;
    }

    @Override
    public String toString() {
        return "Packet [" + command + "] - " + Arrays.toString(data);
    }
}

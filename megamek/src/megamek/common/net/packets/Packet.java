/*
 * MegaMek - Copyright (C) 2005 Ben Mazur (bmazur@sev.org)
 *
 * This program is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License as published by the Free
 * Software Foundation; either version 2 of the License, or (at your option)
 * any later version.
 *
 * This program is distributed in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 * or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License
 * for more details.
 */
package megamek.common.net.packets;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Vector;

import megamek.Version;
import megamek.client.ui.Base64Image;
import megamek.common.*;
import megamek.common.actions.EntityAction;
import megamek.common.annotations.Nullable;
import megamek.common.enums.GamePhase;
import megamek.common.force.Force;
import megamek.common.force.Forces;
import megamek.common.net.enums.PacketCommand;
import megamek.common.strategicBattleSystems.SBFMovePath;
import megamek.server.SmokeCloud;

/**
 * Application layer data packet used to exchange information between client and server.
 */
public record Packet(PacketCommand command, Object... data) implements Serializable {
    /**
     * Creates a <code>Packet</code> with a command and an array of objects
     *
     * @param command a {@link PacketCommand} describing the kind of data.
     * @param data    an Object Array of data.
     */
    public Packet {
    }

    /**
     * @return the command associated.
     */
    public PacketCommand getCommand() {
        return command();
    }

    /**
     * @return the data in the packet
     */
    public Object[] getData() {
        return data;
    }

    /**
     * @param index the index of the desired object
     *
     * @return The {@link megamek.client.ui.Base64Image} value of the object at the specified index
     */
    public Base64Image getBase64Image(int index) {
        Object o = getObject(index);

        if (o instanceof Base64Image image) {
            return image;
        }

        throw new InvalidPacketObjectException("Invalid Base64Image Object Received");
    }

    /**
     * @param index the index of the desired object
     *
     * @return the <code>boolean</code> value of the object at the specified index
     *
     * @deprecated Use {@link #getBoolean(int)} instead.
     */
    public boolean getBooleanValue(int index) {
        return getBoolean(index);
    }

    /**
     * @param index the index of the desired object
     *
     * @return the <code>boolean</code> value of the object at the specified index. Defaults to false.
     */
    public boolean getBoolean(int index) {
        Object o = getObject(index);

        if (o instanceof Boolean bool) {
            return bool;
        }

        return false;
    }

    /**
     * @param index the index of the desired object
     *
     * @return List of <code>boolean</code> value of the object at the specified index
     */
    public List<Boolean> getBooleanList(int index) {
        Object o = getObject(index);

        List<Boolean> booleans = new ArrayList<>();

        if (o instanceof List<?> list) {
            for (Object o1 : list) {
                if (o1 instanceof Boolean bool) {
                    booleans.add(bool);
                }
            }
        }

        return booleans;
    }

    /**
     * @param index the index of the desired object
     *
     * @return List of <code>String</code> value of the object at the specified index
     */
    public Vector<Building> getBuildingVector(int index) {
        Object o = getObject(index);

        Vector<Building> buildings = new Vector<>();

        if (o instanceof List<?> objects) {
            for (Object o1 : objects) {
                if (o1 instanceof Building building) {
                    buildings.add(building);
                }
            }
        }

        return buildings;
    }

    /**
     * @param index the index of the desired object
     *
     * @return the <code>String</code> value of the object at the specified index
     */
    public Coords getCoords(int index) {
        Object o = getObject(index);

        if (o instanceof Coords coords) {
            return coords;
        }

        throw new InvalidPacketObjectException("Invalid Coords Object Received");
    }

    /**
     * @param index the index of the desired object
     *
     * @return Set of <code>String</code> value of the object at the specified index
     */
    public Set<Coords> getCoordsSet(int index) {
        Object o = getObject(index);

        List<Coords> coordsSet = new ArrayList<>();

        if (o instanceof List<?> objects) {
            for (Object o1 : objects) {
                if (o1 instanceof Coords coords) {
                    coordsSet.add(coords);
                }
            }
        }

        return Set.copyOf(coordsSet);
    }

    /**
     * @param index the index of the desired object
     *
     * @return the <code>String</code> value of the object at the specified index
     */
    public Entity getEntity(int index) {
        Object o = getObject(index);

        if (o instanceof Entity entity) {
            return entity;
        }

        throw new InvalidPacketObjectException("Invalid Entity Object Received");
    }

    /**
     * @param index the index of the desired object
     *
     * @return List of <code>String</code> value of the object at the specified index
     */
    public List<Entity> getEntityList(int index) {
        Object o = getObject(index);

        List<Entity> entityList = new ArrayList<>();

        if (o instanceof List<?> objects) {
            for (Object o1 : objects) {
                if (o1 instanceof Entity entity) {
                    entityList.add(entity);
                }
            }
        }

        return entityList;
    }

    /**
     * @param index the index of the desired object
     *
     * @return List of <code>String</code> value of the object at the specified index
     */
    public List<EntityAction> getEntityActionList(int index) {
        Object o = getObject(index);

        List<EntityAction> entityActionList = new ArrayList<>();

        if (o instanceof List<?> objects) {
            for (Object o1 : objects) {
                if (o1 instanceof EntityAction entityAction) {
                    entityActionList.add(entityAction);
                }
            }
        }

        return entityActionList;
    }

    /**
     * @param index the index of the desired object
     *
     * @return the {@link Force} value of the object at the specified index
     */
    public Force getForce(int index) {
        Object o = getObject(index);

        if (o instanceof Force force) {
            return force;
        }

        throw new InvalidPacketObjectException("Invalid Force Object Received");
    }

    /**
     * @param index the index of the desired object
     *
     * @return A List of {@link Force} value of the object at the specified index
     */
    public List<Force> getForceList(int index) {
        Object o = getObject(index);

        List<Force> verifiedForces = new ArrayList<>();

        if (o instanceof List<?> forces) {
            for (Object force : forces) {
                if (force instanceof Force verifiedForce) {
                    verifiedForces.add(verifiedForce);
                }
            }
        }

        return verifiedForces;
    }

    /**
     * @param index the index of the desired object
     *
     * @return the <code>String</code> value of the object at the specified index
     */
    public Forces getForces(int index) {
        Object o = getObject(index);

        if (o instanceof Forces forces) {
            return forces;
        }

        throw new InvalidPacketObjectException("Invalid Forces Object Received");
    }

    /**
     * @param index the index of the desired object
     *
     * @return List of <code>String</code> value of the object at the specified index
     */
    public List<Forces> getForcesList(int index) {
        Object o = getObject(index);

        List<Forces> forcesList = new ArrayList<>();

        if (o instanceof List<?> objects) {
            for (Object o1 : objects) {
                if (o1 instanceof Forces forces) {
                    forcesList.add(forces);
                }
            }
        }

        return forcesList;
    }

    /**
     * @param index the index of the desired object
     *
     * @return The {@link GamePhase} value of the object at the specified index
     */
    public GamePhase getGamePhase(int index) {
        Object o = getObject(index);

        if (o instanceof GamePhase gamePhase) {
            return gamePhase;
        }

        throw new InvalidPacketObjectException("Invalid GamePhase Object Received");
    }

    /**
     * @param index the index of the desired object
     *
     * @return the <code>String</code> value of the object at the specified index
     */
    public GameTurn getGameTurn(int index) {
        Object o = getObject(index);

        if (o instanceof GameTurn gameTurn) {
            return gameTurn;
        }

        throw new InvalidPacketObjectException("Invalid GameTurn Object Received");
    }

    /**
     * @param index the index of the desired object
     *
     * @return List of <code>String</code> value of the object at the specified index
     */
    public List<GameTurn> getGameTurnList(int index) {
        Object o = getObject(index);

        List<GameTurn> gameTurns = new ArrayList<>();

        if (o instanceof List<?> objects) {
            for (Object o1 : objects) {
                if (o1 instanceof GameTurn gameTurn) {
                    gameTurns.add(gameTurn);
                }
            }
        }

        return gameTurns;
    }

    /**
     * @param index the index of the desired object
     *
     * @return the <code>String</code> value of the object at the specified index
     */
    public Hex getHex(int index) {
        Object o = getObject(index);

        if (o instanceof Hex hex) {
            return hex;
        }

        throw new InvalidPacketObjectException("Invalid Hex Object Received");
    }


    /**
     * @param index the index of the desired object
     *
     * @return Set of <code>String</code> value of the object at the specified index
     */
    public Set<Hex> getHexSet(int index) {
        Object o = getObject(index);

        List<Hex> hexSet = new ArrayList<>();

        if (o instanceof List<?> objects) {
            for (Object o1 : objects) {
                if (o1 instanceof Hex hex) {
                    hexSet.add(hex);
                }
            }
        }

        return Set.copyOf(hexSet);
    }

    /**
     * @param index the index of the desired object
     *
     * @return A List of {@link InGameObject} value of the object at the specified index
     */
    public List<InGameObject> getInGameObjectList(int index) {
        Object o = getObject(index);

        List<InGameObject> verifiedInGameObjects = new ArrayList<>();

        if (o instanceof List<?> inGameObjects) {
            for (Object force : inGameObjects) {
                if (force instanceof InGameObject verifiedInGameObject) {
                    verifiedInGameObjects.add(verifiedInGameObject);
                }
            }
        }

        return verifiedInGameObjects;
    }

    /**
     * @param index the index of the desired object
     *
     * @return the <code>int</code> value of the object at the specified index
     *
     * @deprecated use {@link #getInt(int)} instead.
     */
    @Deprecated(since = "0.50.05")
    public int getIntValue(int index) {
        return getInt(index);
    }

    /**
     * @param index the index of the desired object
     *
     * @return the <code>int</code> value of the object at the specified index
     */
    public int getInt(int index) {
        Object o = getObject(index);

        if (o instanceof Integer integer) {
            return integer;
        }

        return 0;
    }

    /**
     * @param index the index of the desired object
     *
     * @return the List of <code>int</code> value of the object at the specified index
     */
    public List<Integer> getIntList(int index) {
        Object o = getObject(index);

        List<Integer> integers = new ArrayList<>();

        if (o instanceof List<?> list) {
            for (Object o1 : list) {
                if (o1 instanceof Integer integer) {
                    integers.add(integer);
                }
            }
        }

        return integers;
    }

    /**
     * @param index the index of the desired object
     *
     * @return A Map of {@link Board} keyed to Integers value of the object at the specified index
     */
    public Map<Integer, Board> getMapOfIntegerAndBoard(int index) {
        Object o = getObject(index);

        Map<Integer, Board> mapOfIntegerAndBoard = new HashMap<>();

        if (o instanceof Map<?, ?> mapOfInteger) {
            for (Object key : mapOfInteger.keySet()) {
                Object value = mapOfInteger.get(key);

                if (key instanceof Integer integer && value instanceof Board board) {
                    mapOfIntegerAndBoard.put(integer, board);
                }
            }
        }

        return mapOfIntegerAndBoard;
    }

    /**
     * @param index the index of the desired object
     *
     * @return the <code>String</code> value of the object at the specified index
     */
    public Minefield getMinefield(int index) {
        Object o = getObject(index);

        if (o instanceof Minefield minefield) {
            return minefield;
        }

        throw new InvalidPacketObjectException("Invalid Minefield Object Received");
    }

    /**
     * @param index the index of the desired object
     *
     * @return List of <code>String</code> value of the object at the specified index
     */
    public Vector<Minefield> getMinefieldVector(int index) {
        Object o = getObject(index);

        Vector<Minefield> minefields = new Vector<>();

        if (o instanceof List<?> objects) {
            for (Object o1 : objects) {
                if (o1 instanceof Minefield minefield) {
                    minefields.add(minefield);
                }
            }
        }

        return minefields;
    }

    /**
     * @param index the index of the desired object
     *
     * @return the object at the specified index
     *
     * @deprecated Use/create methods to handle conversion and enforce types and convert this to a private method
     */
    @Deprecated(since = "0.50.05")
    public @Nullable Object getObject(final int index) {
        return (index >= 0 && index < data.length) ? data[index] : null;
    }

    /**
     * @param index the index of the desired object
     *
     * @return A List of {@link Packet} value of the object at the specified index
     */
    public List<Packet> getPackteList(int index) {
        Object o = getObject(index);

        List<Packet> verifiedPackets = new ArrayList<>();

        if (o instanceof List<?> packets) {
            for (Object force : packets) {
                if (force instanceof Packet verifiedPacket) {
                    verifiedPackets.add(verifiedPacket);
                }
            }
        }

        return verifiedPackets;
    }

    /**
     * @param index the index of the desired object
     *
     * @return the {@link Player} value of the object at the specified index
     */
    public Player getPlayer(int index) {
        Object o = getObject(index);

        if (o instanceof Player player) {
            return player;
        }

        throw new InvalidPacketObjectException("Invalid Player Object Received");
    }

    /**
     * @param index the index of the desired object
     *
     * @return List of {@link Player} value of the object at the specified index
     */
    public Vector<Player> getPlayerVector(int index) {
        Object o = getObject(index);

        Vector<Player> players = new Vector<>();

        if (o instanceof List<?> objects) {
            for (Object o1 : objects) {
                if (o1 instanceof Player player) {
                    players.add(player);
                }
            }
        }

        return players;
    }

    /**
     * @param index the index of the desired object
     *
     * @return the <code>String</code> value of the object at the specified index
     */
    public SBFMovePath getSBFMovePath(int index) {
        Object o = getObject(index);

        if (o instanceof SBFMovePath sbfMovePath) {
            return sbfMovePath;
        }

        throw new InvalidPacketObjectException("Invalid SBFMovePath Object Received");
    }

    /**
     * @param index the index of the desired object
     *
     * @return the <code>String</code> value of the object at the specified index
     */
    public SmokeCloud getSmokeCloud(int index) {
        Object o = getObject(index);

        if (o instanceof SmokeCloud smokeCloud) {
            return smokeCloud;
        }

        throw new InvalidPacketObjectException("Invalid SmokeCloud Object Received");
    }

    /**
     * @param index the index of the desired object
     *
     * @return the <code>String</code> value of the object at the specified index
     *
     * @deprecated use {@link #getString(int)} instead
     */
    @Deprecated(since = "0.50.05")
    public String getStringValue(int index) {
        return getString(index);
    }

    /**
     * @param index the index of the desired object
     *
     * @return the <code>String</code> value of the object at the specified index. Defaults to empty string.
     */
    public String getString(int index) {
        Object o = getObject(index);

        if (o instanceof String string) {
            return string;
        }

        return "";
    }

    /**
     * @param index the index of the desired object
     *
     * @return List of <code>String</code> value of the object at the specified index
     */
    public List<String> getStringList(int index) {
        Object o = getObject(index);

        List<String> strings = new ArrayList<>();

        if (o instanceof List<?> objects) {
            for (Object o1 : objects) {
                if (o1 instanceof String string) {
                    strings.add(string);
                }
            }
        }

        return strings;
    }


    /**
     * @param index the index of the desired object
     *
     * @return the <code>String</code> value of the object at the specified index
     */
    public UnitLocation getUnitLocation(int index) {
        Object o = getObject(index);

        if (o instanceof UnitLocation unitLocation) {
            return unitLocation;
        }

        throw new InvalidPacketObjectException("Invalid UnitLocation Object Received");
    }

    /**
     * @param index the index of the desired object
     *
     * @return List of <code>String</code> value of the object at the specified index
     */
    public Vector<UnitLocation> getUnitLocationVector(int index) {
        Object o = getObject(index);

        Vector<UnitLocation> unitLocations = new Vector<>();

        if (o instanceof List<?> objects) {
            for (Object o1 : objects) {
                if (o1 instanceof UnitLocation unitLocation) {
                    unitLocations.add(unitLocation);
                }
            }
        }

        return unitLocations;
    }

    /**
     * @param index the index of the desired object
     *
     * @return The {@link Version} value of the object at the specified index
     */
    public Version getVersion(int index) {
        Object o = getObject(index);

        if (o instanceof Version version) {
            return version;
        }

        throw new InvalidPacketObjectException("Invalid Version Object Received");
    }

    @Override
    public String toString() {
        return "Packet [" + command + "] - " + Arrays.toString(data);
    }
}

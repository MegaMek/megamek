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
import java.util.Vector;

import megamek.Version;
import megamek.client.ui.Base64Image;
import megamek.common.Board;
import megamek.common.Entity;
import megamek.common.GameTurn;
import megamek.common.InGameObject;
import megamek.common.Player;
import megamek.common.UnitLocation;
import megamek.common.annotations.Nullable;
import megamek.common.enums.GamePhase;
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
     * @return the <code>int</code> value of the object at the specified index
     */
    public int getIntValue(int index) {
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
    public List<Integer> getIntListValue(int index) {
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
     * @return the <code>boolean</code> value of the object at the specified index
     */
    public boolean getBooleanValue(int index) {
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
    public List<Boolean> getBooleanListValue(int index) {
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
     * @return the <code>String</code> value of the object at the specified index
     */
    public String getStringValue(int index) {
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
    public List<String> getStringListValue(int index) {
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
     * @return the {@link Player} value of the object at the specified index
     */
    public @Nullable Player getPlayerValue(int index) {
        Object o = getObject(index);

        if (o instanceof Player player) {
            return player;
        }

        return null;
    }

    /**
     * @param index the index of the desired object
     *
     * @return List of <code>String</code> value of the object at the specified index
     */
    public Vector<Player> getPlayerVectorValue(int index) {
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
     * @return A List of {@link Force} value of the object at the specified index
     */
    public List<Force> getForceListValue(int index) {
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
     * @return A List of {@link Force} value of the object at the specified index
     */
    public List<InGameObject> getInGameObjectListValue(int index) {
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
     * @return A List of {@link Packet} value of the object at the specified index
     */
    public List<Packet> getPackteListValue(int index) {
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
     * @return The {@link Version} value of the object at the specified index
     */
    public @Nullable Version getVersionValue(int index) {
        Object o = getObject(index);

        if (o instanceof Version version) {
            return version;
        }

        return null;
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
     * @return The {@link megamek.client.ui.Base64Image} value of the object at the specified index
     */
    public @Nullable Base64Image getBase64ImageValue(int index) {
        Object o = getObject(index);

        if (o instanceof Base64Image image) {
            return image;
        }

        return null;
    }

    /**
     * @param index the index of the desired object
     *
     * @return The {@link GamePhase} value of the object at the specified index
     */
    public @Nullable GamePhase getGamePhaseValue(int index) {
        Object o = getObject(index);

        if (o instanceof GamePhase gamePhase) {
            return gamePhase;
        }

        return null;
    }

    /**
     * @param index the index of the desired object
     *
     * @return the <code>String</code> value of the object at the specified index
     */
    public @Nullable GameTurn getGameTurnValue(int index) {
        Object o = getObject(index);

        if (o instanceof GameTurn gameTurn) {
            return gameTurn;
        }

        return null;
    }

    /**
     * @param index the index of the desired object
     *
     * @return List of <code>String</code> value of the object at the specified index
     */
    public List<GameTurn> getGameTurnListValue(int index) {
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
    public @Nullable Entity getEntityValue(int index) {
        Object o = getObject(index);

        if (o instanceof Entity entity) {
            return entity;
        }

        return null;
    }

    /**
     * @param index the index of the desired object
     *
     * @return List of <code>String</code> value of the object at the specified index
     */
    public List<Entity> getEntityListValue(int index) {
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
     * @return the <code>String</code> value of the object at the specified index
     */
    public @Nullable Forces getForcesValue(int index) {
        Object o = getObject(index);

        if (o instanceof Forces forces) {
            return forces;
        }

        return null;
    }

    /**
     * @param index the index of the desired object
     *
     * @return List of <code>String</code> value of the object at the specified index
     */
    public List<Forces> getForcesListValue(int index) {
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
     * @return the <code>String</code> value of the object at the specified index
     */
    public @Nullable UnitLocation getUnitLocationValue(int index) {
        Object o = getObject(index);

        if (o instanceof UnitLocation unitLocation) {
            return unitLocation;
        }

        return null;
    }

    /**
     * @param index the index of the desired object
     *
     * @return List of <code>String</code> value of the object at the specified index
     */
    public Vector<UnitLocation> getUnitLocationVectorValue(int index) {
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

    @Override
    public String toString() {
        return "Packet [" + command + "] - " + Arrays.toString(data);
    }
}

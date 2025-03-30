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
import java.util.*;

import megamek.Version;
import megamek.client.bot.princess.BehaviorSettings;
import megamek.client.ui.Base64Image;
import megamek.common.*;
import megamek.common.actions.ArtilleryAttackAction;
import megamek.common.actions.EntityAction;
import megamek.common.actions.WeaponAttackAction;
import megamek.common.annotations.Nullable;
import megamek.common.enums.GamePhase;
import megamek.common.force.Force;
import megamek.common.force.Forces;
import megamek.common.net.enums.PacketCommand;
import megamek.common.options.GameOptions;
import megamek.common.planetaryconditions.PlanetaryConditions;
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

    /*
     * Everything above here are items to help deal with migrations to the naming below as well as allow direct
     * access to the data.
     *
     * Everything BELOW here are methods meant to validate the data that is coming in is actually to be what it is
     * supposed to be, provide defaults for lower level types (int, boolean, etc.) and to throw errors for all others
     * . When adding new methods, follow the patter of <code>get`Type`</code> or <code>get`TypeStorage`</code> or in
     * the cases of Maps, <code>getMapOfFirstTypeAndSecondType</code> and so on.
     *
     * Any single `gets` should throw if the value can't be correctly determined. For all other Collection types,
     * return an empty version of them.
     */

    /**
     * @param index the index of the desired object
     *
     * @return Vector of {@link ArtilleryAttackAction} Objects
     */
    public Vector<ArtilleryAttackAction> getArtilleryAttackActionVector(int index) {
        Object o = getObject(index);

        Vector<ArtilleryAttackAction> artilleryAttackActions = new Vector<>();

        if (o instanceof List<?> objects) {
            for (Object o1 : objects) {
                if (o1 instanceof ArtilleryAttackAction artilleryAttackAction) {
                    artilleryAttackActions.add(artilleryAttackAction);
                }
            }
        }

        return artilleryAttackActions;
    }

    /**
     * @param index the index of the desired object
     *
     * @return {@link Base64Image} Object
     */
    public Base64Image getBase64Image(int index) throws InvalidPacketObjectException {
        Object o = getObject(index);

        if (o instanceof Base64Image image) {
            return image;
        }

        throw new InvalidPacketObjectException("Invalid Base64Image Object Received");
    }

    /**
     * @param index the index of the desired object
     *
     * @return Set of {@link BoardDimensions} Objects
     */
    public Set<BoardDimensions> getBoardDimensionsSet(int index) {
        Object o = getObject(index);

        List<BoardDimensions> dataSet = new ArrayList<>();

        if (o instanceof List<?> objects) {
            for (Object o1 : objects) {
                if (o1 instanceof BoardDimensions boardDimensions) {
                    dataSet.add(boardDimensions);
                }
            }
        }

        return Set.copyOf(dataSet);
    }

    /**
     * @param index the index of the desired object
     *
     * @return the <code>boolean</code> value or false
     *
     * @deprecated Use {@link #getBoolean(int)} instead.
     */
    @Deprecated(since = "0.50.05")
    public boolean getBooleanValue(int index) {
        return getBoolean(index);
    }

    /**
     * @param index the index of the desired object
     *
     * @return the <code>boolean</code> value or false
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
     * @return List of <code>boolean</code> values
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
     * @return Vector of {@link Building} Objects
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
     * @return {@link Coords} Object
     */
    public Coords getCoords(int index) throws InvalidPacketObjectException {
        Object o = getObject(index);

        if (o instanceof Coords coords) {
            return coords;
        }

        throw new InvalidPacketObjectException("Invalid Coords Object Received");
    }

    /**
     * @param index the index of the desired object
     *
     * @return HashSet of {@link Coords} Objects
     */
    public HashSet<Coords> getCoordsHashSet(int index) {
        Object o = getObject(index);

        HashSet<Coords> coordsSet = new HashSet<>();

        if (o instanceof List<?> objects) {
            for (Object o1 : objects) {
                if (o1 instanceof Coords coords) {
                    coordsSet.add(coords);
                }
            }
        }

        return coordsSet;
    }

    /**
     * @param index the index of the desired object
     *
     * @return Set of {@link Coords} Objects
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
     * @return Vector of {@link Coords} Objects
     */
    public Vector<Coords> getCoordsVector(int index) {
        Object o = getObject(index);

        Vector<Coords> coordsVector = new Vector<>();

        if (o instanceof List<?> objects) {
            for (Object o1 : objects) {
                if (o1 instanceof Coords coords) {
                    coordsVector.add(coords);
                }
            }
        }

        return coordsVector;
    }

    /**
     * @param index the index of the desired object
     *
     * @return {@link Entity} Object
     */
    public Entity getEntity(int index) throws InvalidPacketObjectException {
        Object o = getObject(index);

        if (o instanceof Entity entity) {
            return entity;
        }

        throw new InvalidPacketObjectException("Invalid Entity Object Received");
    }

    /**
     * @param index the index of the desired object
     *
     * @return List of {@link Entity} Objects
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
     * @return List of {@link EntityAction} Objects
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
     * @return Vector of {@link Flare} Objects
     */
    public Vector<Flare> getFlareVector(int index) {
        Object o = getObject(index);

        Vector<Flare> flares = new Vector<>();

        if (o instanceof List<?> objects) {
            for (Object o1 : objects) {
                if (o1 instanceof Flare flare) {
                    flares.add(flare);
                }
            }
        }

        return flares;
    }

    /**
     * @param index the index of the desired object
     *
     * @return {@link Force} Object
     */
    public Force getForce(int index) throws InvalidPacketObjectException {
        Object o = getObject(index);

        if (o instanceof Force force) {
            return force;
        }

        throw new InvalidPacketObjectException("Invalid Force Object Received");
    }

    /**
     * @param index the index of the desired object
     *
     * @return A List of {@link Force} Objects
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
     * @return {@link Forces} Object
     */
    public Forces getForces(int index) throws InvalidPacketObjectException {
        Object o = getObject(index);

        if (o instanceof Forces forces) {
            return forces;
        }

        throw new InvalidPacketObjectException("Invalid Forces Object Received");
    }

    /**
     * @param index the index of the desired object
     *
     * @return List of {@link Forces} Objects.
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
     * @return {@link GameOptions} object
     */
    public GameOptions getGameOptions(int index) throws InvalidPacketObjectException {
        Object o = getObject(index);

        if (o instanceof GameOptions gameOptions) {
            return gameOptions;
        }

        throw new InvalidPacketObjectException("Invalid GameOptions Object Received");
    }

    /**
     * @param index the index of the desired object
     *
     * @return {@link GamePhase} Object.
     */
    public GamePhase getGamePhase(int index) throws InvalidPacketObjectException {
        Object o = getObject(index);

        if (o instanceof GamePhase gamePhase) {
            return gamePhase;
        }

        throw new InvalidPacketObjectException("Invalid GamePhase Object Received");
    }

    /**
     * @param index the index of the desired object
     *
     * @return {@link GameTurn} Object
     */
    public GameTurn getGameTurn(int index) throws InvalidPacketObjectException {
        Object o = getObject(index);

        if (o instanceof GameTurn gameTurn) {
            return gameTurn;
        }

        throw new InvalidPacketObjectException("Invalid GameTurn Object Received");
    }

    /**
     * @param index the index of the desired object
     *
     * @return List of {@link GameTurn} Objects
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
     * @return HashTable of a Collection of {@link SpecialHexDisplay} Objects Keyed by {@link Coords}
     */
    public Hashtable<Coords, Collection<SpecialHexDisplay>> getHashTableOfCoordsAndCollectionOfSpecialHexDisplay(
          int index) {
        Object o = getObject(index);

        Hashtable<Coords, Collection<SpecialHexDisplay>> mapData = new Hashtable<>();

        if (o instanceof Hashtable<?, ?> mapOfCoords) {
            for (Object key : mapOfCoords.keySet()) {
                if (key instanceof Coords coords) {
                    Object value = mapOfCoords.get(key);

                    if (value instanceof Collection<?> collection) {
                        List<SpecialHexDisplay> values = new ArrayList<>();

                        for (Object object : collection) {
                            if (object instanceof SpecialHexDisplay specialHexDisplay) {
                                values.add(specialHexDisplay);
                            }
                        }

                        mapData.put(coords, values);
                    }
                }
            }
        }

        return mapData;
    }

    /**
     * @param index the index of the desired object
     *
     * @return {@link Hex} Object.
     */
    public Hex getHex(int index) throws InvalidPacketObjectException {
        Object o = getObject(index);

        if (o instanceof Hex hex) {
            return hex;
        }

        throw new InvalidPacketObjectException("Invalid Hex Object Received");
    }

    /**
     * @param index the index of the desired object
     *
     * @return Set of {@link Hex} Objects
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
     * @return A List of {@link InGameObject} objects
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
     * @return the <code>int</code> value or 0
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
     * @return the <code>int</code> value or 0
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
     * @return A Mapping of {@link Coords} to a listing of {@link ICarryable} Objects
     */
    public Map<Coords, List<ICarryable>> getMapOfCoordsAndListICarryables(int index) {
        Object o = getObject(index);

        Map<Coords, List<ICarryable>> verifiedObjects = new HashMap<>();

        if (o instanceof Map<?, ?> outterMap) {
            for (Object o1 : outterMap.keySet()) {
                if (o1 instanceof Coords coords) {
                    Object value = outterMap.get(coords);

                    if (value instanceof List<?> list) {
                        List<ICarryable> carryables = new ArrayList<>();

                        for (Object o2 : list) {
                            if (o2 instanceof ICarryable carryable) {
                                carryables.add(carryable);
                            }
                        }

                        verifiedObjects.put(coords, carryables);
                    }
                }
            }
        }

        return verifiedObjects;
    }

    /**
     * @param index the index of the desired object
     *
     * @return A Map of {@link Board}'s keyed to Integers value
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
     * @return A Map of {@link BehaviorSettings} keyed with <code>String</code>'s.
     */
    public Map<String, BehaviorSettings> getMapOfStringAndBehaviorSettings(int index) {
        Object o = getObject(index);

        Map<String, BehaviorSettings> mapData = new HashMap<>();

        if (o instanceof Map<?, ?> mapOfString) {
            for (Object key : mapOfString.keySet()) {
                Object value = mapOfString.get(key);

                if (key instanceof String string && value instanceof BehaviorSettings settings) {
                    mapData.put(string, settings);
                }
            }
        }

        return mapData;
    }

    /**
     * @param index the index of the desired object
     *
     * @return {@link MapSettings} Object
     */
    public MapSettings getMapSettings(int index) throws InvalidPacketObjectException {
        Object o = getObject(index);

        if (o instanceof MapSettings mapSettings) {
            return mapSettings;
        }

        throw new InvalidPacketObjectException("Invalid MapSettings Object Received");
    }

    /**
     * @param index the index of the desired object
     *
     * @return {@link Minefield} Object
     */
    public Minefield getMinefield(int index) throws InvalidPacketObjectException {
        Object o = getObject(index);

        if (o instanceof Minefield minefield) {
            return minefield;
        }

        throw new InvalidPacketObjectException("Invalid Minefield Object Received");
    }

    /**
     * @param index the index of the desired object
     *
     * @return Vector of {@link Minefield} Objects
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
     * @return A List of {@link Packet} objects
     */
    public List<Packet> getPackteList(int index) throws InvalidPacketObjectException {
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
     * @return {@link PlanetaryConditions} object
     */
    public PlanetaryConditions getPlanetaryConditions(int index) throws InvalidPacketObjectException {
        Object o = getObject(index);

        if (o instanceof PlanetaryConditions planetaryConditions) {
            return planetaryConditions;
        }

        throw new InvalidPacketObjectException("Invalid PlanetaryConditions Object Received");
    }

    /**
     * @param index the index of the desired object
     *
     * @return {@link Player} object
     */
    public Player getPlayer(int index) throws InvalidPacketObjectException {
        Object o = getObject(index);

        if (o instanceof Player player) {
            return player;
        }

        throw new InvalidPacketObjectException("Invalid Player Object Received");
    }

    /**
     * @param index the index of the desired object
     *
     * @return Vector of {@link Player} objects
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
     * @return A List of {@link Report} objects
     */
    public List<Report> getReportList(int index) {
        Object o = getObject(index);

        List<Report> reports = new ArrayList<>();

        if (o instanceof List<?> packets) {
            for (Object force : packets) {
                if (force instanceof Report report) {
                    reports.add(report);
                }
            }
        }

        return reports;
    }

    /**
     * @param index the index of the desired object
     *
     * @return A List of... List of {@link Report} objects (yea, 2 lists deep)
     */
    public List<List<Report>> getReportListOfAList(int index) {
        Object o = getObject(index);

        List<List<Report>> reports = new ArrayList<>();

        if (o instanceof List<?> outer) {
            for (Object o1 : outer) {
                if (o1 instanceof List<?> list) {
                    List<Report> innerReports = new ArrayList<>();

                    for (Object o2 : list) {
                        if (o2 instanceof Report report) {
                            innerReports.add(report);
                        }
                    }

                    reports.add(innerReports);
                }
            }
        }

        return reports;
    }

    /**
     * @param index the index of the desired object
     *
     * @return {@link SBFMovePath} Object
     */
    public SBFMovePath getSBFMovePath(int index) throws InvalidPacketObjectException {
        Object o = getObject(index);

        if (o instanceof SBFMovePath sbfMovePath) {
            return sbfMovePath;
        }

        throw new InvalidPacketObjectException("Invalid SBFMovePath Object Received");
    }

    /**
     * @param index the index of the desired object
     *
     * @return {@link SmokeCloud} Object
     */
    public SmokeCloud getSmokeCloud(int index) throws InvalidPacketObjectException {
        Object o = getObject(index);

        if (o instanceof SmokeCloud smokeCloud) {
            return smokeCloud;
        }

        throw new InvalidPacketObjectException("Invalid SmokeCloud Object Received");
    }

    /**
     * @param index the index of the desired object
     *
     * @return the <code>String</code> value at index or an empty string.
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
     * @return the <code>String</code> value at index or an empty string.
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
     * @return List of <code>String</code>'s
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
     * @return Vector of {@link TagInfo} Objects
     */
    public Vector<TagInfo> getTagInfoVector(int index) {
        Object o = getObject(index);

        Vector<TagInfo> tagInfos = new Vector<>();

        if (o instanceof List<?> objects) {
            for (Object o1 : objects) {
                if (o1 instanceof TagInfo tagInfo) {
                    tagInfos.add(tagInfo);
                }
            }
        }

        return tagInfos;
    }

    /**
     * @param index the index of the desired object
     *
     * @return {@link UnitLocation} Object
     */
    public UnitLocation getUnitLocation(int index) throws InvalidPacketObjectException {
        Object o = getObject(index);

        if (o instanceof UnitLocation unitLocation) {
            return unitLocation;
        }

        throw new InvalidPacketObjectException("Invalid UnitLocation Object Received");
    }

    /**
     * @param index the index of the desired object
     *
     * @return Vector of {@link UnitLocation} data
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
     * @return {@link Version} Object
     */
    public Version getVersion(int index) throws InvalidPacketObjectException {
        Object o = getObject(index);

        if (o instanceof Version version) {
            return version;
        }

        throw new InvalidPacketObjectException("Invalid Version Object Received");
    }

    /**
     * @param index the index of the desired object
     *
     * @return A List of {@link WeaponAttackAction} Objects
     */
    public List<WeaponAttackAction> getWeaponAttackActionList(int index) {
        Object o = getObject(index);

        List<WeaponAttackAction> verifiedData = new ArrayList<>();

        if (o instanceof List<?> inGameObjects) {
            for (Object force : inGameObjects) {
                if (force instanceof WeaponAttackAction verifiedObject) {
                    verifiedData.add(verifiedObject);
                }
            }
        }

        return verifiedData;
    }

    @Override
    public String toString() {
        return "Packet [" + command + "] - " + Arrays.toString(data);
    }
}

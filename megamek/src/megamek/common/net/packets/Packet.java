/*
 * Copyright (C) 2005 Ben Mazur (bmazur@sev.org)
 * Copyright (C) 2002-2025 The MegaMek Team. All Rights Reserved.
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
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Vector;

import jakarta.annotation.Nonnull;
import megamek.client.bot.princess.BehaviorSettings;
import megamek.common.Hex;
import megamek.common.Player;
import megamek.common.Report;
import megamek.common.SpecialHexDisplay;
import megamek.common.TagInfo;
import megamek.common.actions.ArtilleryAttackAction;
import megamek.common.actions.EntityAction;
import megamek.common.actions.WeaponAttackAction;
import megamek.common.annotations.Nullable;
import megamek.common.board.BoardDimensions;
import megamek.common.board.BoardLocation;
import megamek.common.board.Coords;
import megamek.common.containers.PlayerIDAndList;
import megamek.common.enums.WeaponSortOrder;
import megamek.common.equipment.Flare;
import megamek.common.equipment.GroundObject;
import megamek.common.equipment.ICarryable;
import megamek.common.equipment.Minefield;
import megamek.common.force.Force;
import megamek.common.force.Forces;
import megamek.common.game.GameTurn;
import megamek.common.game.InGameObject;
import megamek.common.loaders.MapSettings;
import megamek.common.moves.MovePath;
import megamek.common.net.enums.PacketCommand;
import megamek.common.options.GameOptions;
import megamek.common.options.IBasicOption;
import megamek.common.planetaryConditions.PlanetaryConditions;
import megamek.common.strategicBattleSystems.SBFMovePath;
import megamek.common.strategicBattleSystems.SBFReportEntry;
import megamek.common.strategicBattleSystems.SBFTurn;
import megamek.common.units.Entity;
import megamek.common.units.FighterSquadron;
import megamek.common.units.IBuilding;
import megamek.common.units.UnitLocation;
import megamek.server.SmokeCloud;

/**
 * Application layer data packet used to exchange information between client and server.
 */
public record Packet(PacketCommand command, Object... data) implements Serializable {

    /**
     * Creates a <code>Packet</code> with a command and an array of objects
     */
    public Packet(PacketCommand command, Object... data) {
        this.command = command;
        this.data = data;
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
    public int getIntValue(int index) throws InvalidPacketDataException {
        Object object = getObject(index);

        if (object instanceof Integer integer) {
            return integer;
        }

        throw new InvalidPacketDataException("int", object, index);
    }

    /**
     * @param index the index of the desired object
     *
     * @return a List of {@link Integer}'s value of the object at the specified index
     */
    public List<Integer> getIntList(int index) throws InvalidPacketDataException {
        Object object = getObject(index);

        ArrayList<Integer> result = new ArrayList<>();

        if (object instanceof List<?> list) {
            for (Object integer : list) {
                if (integer instanceof Integer verifiedInt) {
                    result.add(verifiedInt);
                }
            }
            return result;
        }

        throw new InvalidPacketDataException("List", object, index);
    }

    /**
     * @param index the index of the desired object
     *
     * @return a Map of {@link Integer} to {@link Integer}'s value of the object at the specified index
     */
    public Map<Integer, Integer> getIntMapToInt(int index) throws InvalidPacketDataException {
        Object object = getObject(index);

        Map<Integer, Integer> result = new HashMap<>();

        if (object instanceof Map<?, ?> collection) {
            collection.forEach((key, value) -> {
                if (key instanceof Integer verifiedKey) {
                    if (value instanceof Integer verifiedValue) {
                        result.put(verifiedKey, verifiedValue);
                    }
                }
            });
            return result;
        }

        throw new InvalidPacketDataException("Map<Integer, Integer>", object, index);
    }


    /**
     * @param index the index of the desired object
     *
     * @return a Map of {@link Coords} key's with a Collection of {@link SpecialHexDisplay} value's of the object at the
     *       specified index
     */
    public Map<Integer, List<SBFReportEntry>> getIntegerWithSBFReportEntryList(int index)
          throws InvalidPacketDataException {
        Object object = getObject(index);

        Map<Integer, List<SBFReportEntry>> result = new HashMap<>();

        if (object instanceof Map<?, ?> collection) {
            collection.forEach((key, value) -> {
                if (key instanceof Integer verifiedKey) {
                    if (value instanceof List<?> verifieList) {
                        List<SBFReportEntry> verifiedList = new ArrayList<>();
                        for (Object collectionValue : verifieList) {
                            if (collectionValue instanceof SBFReportEntry verifiedSBFReportEntry) {
                                verifiedList.add(verifiedSBFReportEntry);
                            }
                        }

                        result.put(verifiedKey, verifiedList);
                    }
                }
            });
            return result;
        }

        throw new InvalidPacketDataException("Map<Integer, List>", object, index);
    }

    /**
     * @param index the index of the desired object
     *
     * @return the <code>boolean</code> value of the object at the specified index
     */
    public boolean getBooleanValue(int index) throws InvalidPacketDataException {
        Object object = getObject(index);

        if (object instanceof Boolean bool) {
            return bool;
        }

        throw new InvalidPacketDataException("Boolean", object, index);
    }

    /**
     * @param index the index of the desired object
     *
     * @return the <code>String</code> value of the object at the specified index
     */
    public @Nullable String getStringValue(int index) throws InvalidPacketDataException {
        Object object = getObject(index);

        if (object instanceof String value) {
            return value;
        }

        throw new InvalidPacketDataException("String", object, index);
    }

    /**
     * @param index the index of the desired object
     *
     * @return a Map of {@link String} key's with {@link BehaviorSettings} value's of the object at the specified index
     */
    public Map<String, BehaviorSettings> getStringWIthBehaviorSettingsMap(int index) throws InvalidPacketDataException {
        Object object = getObject(index);

        Map<String, BehaviorSettings> result = new HashMap<>();

        if (object instanceof Map<?, ?> collection) {
            collection.forEach((key, value) -> {
                if (key instanceof String verifiedKey) {
                    if (value instanceof BehaviorSettings verifiedValue) {
                        result.put(verifiedKey, verifiedValue);
                    }
                }
            });
            return result;
        }

        throw new InvalidPacketDataException("Map<String, BehaviorSettings>", object, index);
    }

    /**
     * @param index the index of the desired object
     *
     * @return a Vector of {@link ArtilleryAttackAction}'s value of the object at the specified index
     */
    public Vector<ArtilleryAttackAction> getArtilleryAttackAction(int index) throws InvalidPacketDataException {
        Object object = getObject(index);

        Vector<ArtilleryAttackAction> result = new Vector<>();

        if (object instanceof Vector<?> vector) {
            for (Object action : vector) {
                if (action instanceof ArtilleryAttackAction value) {
                    result.add(value);
                }
            }
            return result;
        }

        throw new InvalidPacketDataException("Vector", object, index);
    }

    /**
     * @param index the index of the desired object
     *
     * @return a Set of {@link BoardDimensions}'s value of the object at the specified index
     */
    public Set<BoardDimensions> getBoardDimensionsSet(int index) throws InvalidPacketDataException {
        Object object = getObject(index);

        Set<BoardDimensions> result = new HashSet<>();

        if (object instanceof Set<?> vector) {
            for (Object action : vector) {
                if (action instanceof BoardDimensions value) {
                    result.add(value);
                }
            }
            return result;
        }

        throw new InvalidPacketDataException("Set", object, index);
    }

    /**
     * @param index the index of the desired object
     *
     * @return a Map of {@link BoardLocation} key's with {@link Hex} value's of the object at the specified index
     */
    public Map<BoardLocation, Hex> getBoardLocationHexMap(int index) throws InvalidPacketDataException {
        Object object = getObject(index);

        Map<BoardLocation, Hex> result = new HashMap<>();

        if (object instanceof Map<?, ?> collection) {
            collection.forEach((key, value) -> {
                if (key instanceof BoardLocation verifiedBoardLocation) {
                    if (value instanceof Hex verifiedHex) {
                        result.put(verifiedBoardLocation, verifiedHex);
                    }
                }
            });
            return result;
        }

        throw new InvalidPacketDataException("Map<Location, Hex>", object, index);
    }

    /**
     * @param index the index of the desired object
     *
     * @return a List of {@link IBuilding}'s value of the object at the specified index
     */
    public List<IBuilding> getBuildingList(int index) throws InvalidPacketDataException {
        Object object = getObject(index);

        ArrayList<IBuilding> result = new ArrayList<>();

        if (object instanceof Vector<?> vector) {
            for (Object building : vector) {
                if (building instanceof IBuilding verifiedBuilding) {
                    result.add(verifiedBuilding);
                }
            }
            return result;
        }

        throw new InvalidPacketDataException("Vector", object, index);
    }

    /**
     * @param index the index of the desired object
     *
     * @return the {@link Coords} value of the object at the specified index
     */
    public @Nullable Coords getCoords(int index) throws InvalidPacketDataException {
        Object object = getObject(index);

        if (object instanceof Coords coords) {
            return coords;
        }

        throw new InvalidPacketDataException("Coords", object, index);
    }

    /**
     * @param index the index of the desired object
     *
     * @return a HashSet of {@link Coords}'s value of the object at the specified index
     */
    public HashSet<Coords> getCoordsHashSet(int index) throws InvalidPacketDataException {
        Object object = getObject(index);

        HashSet<Coords> result = new HashSet<>();

        if (object instanceof HashSet<?> hashSet) {
            for (Object coord : hashSet) {
                if (coord instanceof Coords verifiedCoord) {
                    result.add(verifiedCoord);
                }
            }
            return result;
        }

        throw new InvalidPacketDataException("HashSet", object, index);
    }

    /**
     * @param index the index of the desired object
     *
     * @return a Vector of {@link Coords}'s value of the object at the specified index
     */
    public Vector<Coords> getCoordsVector(int index) throws InvalidPacketDataException {
        Object object = getObject(index);

        Vector<Coords> result = new Vector<>();

        if (object instanceof Vector<?> vector) {
            for (Object coord : vector) {
                if (coord instanceof Coords verifiedCoord) {
                    result.add(verifiedCoord);
                }
            }
            return result;
        }

        throw new InvalidPacketDataException("Vector", object, index);
    }

    /**
     * @param index the index of the desired object
     *
     * @return a Map of {@link Coords} key's with a List of {@link GroundObject} {@link GroundObject} value's of the
     *       object at the specified index
     */
    public Map<Coords, List<ICarryable>> getCoordsWithGroundObjectListMap(int index) throws InvalidPacketDataException {
        Object object = getObject(index);

        Map<Coords, List<ICarryable>> result = new HashMap<>();

        if (object instanceof Map<?, ?> collection) {
            collection.forEach((key, value) -> {
                if (key instanceof Coords verifiedCoords) {
                    if (value instanceof List<?> valueList) {
                        List<ICarryable> verifiedList = new ArrayList<>();
                        for (Object listValue : valueList) {
                            if (listValue instanceof GroundObject verifiedGroundObject) {
                                verifiedList.add(verifiedGroundObject);
                            }
                        }

                        result.put(verifiedCoords, verifiedList);
                    }
                }
            });
            return result;
        }

        throw new InvalidPacketDataException("Map<Coords, List>", object, index);
    }

    /**
     * @param index the index of the desired object
     *
     * @return a Map of {@link Coords} key's with a Collection of {@link SpecialHexDisplay} value's of the object at the
     *       specified index
     */
    public Map<Coords, Collection<SpecialHexDisplay>> getCoordsWithSpecialHexDisplayCollectionMap(int index)
          throws InvalidPacketDataException {
        Object object = getObject(index);

        Map<Coords, Collection<SpecialHexDisplay>> result = new HashMap<>();

        if (object instanceof Map<?, ?> collection) {
            collection.forEach((key, value) -> {
                if (key instanceof Coords verifiedCoords) {
                    if (value instanceof Collection<?> verifiedCollection) {
                        List<SpecialHexDisplay> verifiedList = new ArrayList<>();
                        for (Object collectionValue : verifiedCollection) {
                            if (collectionValue instanceof SpecialHexDisplay verifiedSpecialHexDisplay) {
                                verifiedList.add(verifiedSpecialHexDisplay);
                            }
                        }

                        result.put(verifiedCoords, verifiedList);
                    }
                }
            });
            return result;
        }

        throw new InvalidPacketDataException("Map<Coords, Collection>", object, index);
    }

    /**
     * @param index the index of the desired object
     *
     * @return the {@link Entity} value of the object at the specified index
     */
    public @Nullable Entity getEntity(int index) throws InvalidPacketDataException {
        Object object = getObject(index);

        if (object instanceof Entity entity) {
            return entity;
        }

        throw new InvalidPacketDataException("Entity", object, index);
    }

    /**
     * @param index the index of the desired object
     *
     * @return a List of {@link Entity}'s value of the object at the specified index
     */
    public List<Entity> getEntityList(int index) throws InvalidPacketDataException {
        Object object = getObject(index);

        ArrayList<Entity> result = new ArrayList<>();

        // Some consumers require empty ArrayLists on null
        if (object == null) {
            return result;
        }

        // Must accept Collections, at least until other changes are made
        if (object instanceof Collection<?> collection) {
            for (Object entity : collection) {
                if (entity instanceof Entity verifiedEntity) {
                    result.add(verifiedEntity);
                }
            }
            return result;
        }

        throw new InvalidPacketDataException("Collection<?>", object, index);
    }

    /**
     * @param index the index of the desired object
     *
     * @return a List of {@link megamek.common.actions.EntityAction}'s value of the object at the specified index
     */
    public List<EntityAction> getEntityActionList(int index) throws InvalidPacketDataException {
        Object object = getObject(index);

        ArrayList<EntityAction> result = new ArrayList<>();

        // Found users are List<> and Vector<> so this works.
        if (object instanceof List<?> list) {
            for (Object entityAction : list) {
                if (entityAction instanceof EntityAction verifiedEntityAction) {
                    result.add(verifiedEntityAction);
                }
            }
            return result;
        }

        throw new InvalidPacketDataException("List<?>", object, index);
    }

    /**
     * @param index the index of the desired object
     *
     * @return the {@link FighterSquadron} value of the object at the specified index
     */
    public @Nullable FighterSquadron getFighterSquadron(int index) throws InvalidPacketDataException {
        Object object = getObject(index);

        if (object instanceof FighterSquadron value) {
            return value;
        }

        throw new InvalidPacketDataException("FighterSquadron", object, index);
    }

    /**
     * @param index the index of the desired object
     *
     * @return a Vector of {@link Flare}'s value of the object at the specified index
     */
    public Vector<Flare> getFlareVector(int index) throws InvalidPacketDataException {
        Object object = getObject(index);

        Vector<Flare> result = new Vector<>();

        if (object instanceof Vector<?> vector) {
            for (Object action : vector) {
                if (action instanceof Flare value) {
                    result.add(value);
                }
            }
            return result;
        }

        throw new InvalidPacketDataException("Vector", object, index);
    }

    /**
     * @param index the index of the desired object
     *
     * @return the {@link Force} value of the object at the specified index or NULL
     */
    public @Nullable Force getForce(int index) throws InvalidPacketDataException {
        Object object = getObject(index);

        if (object instanceof Force value) {
            return value;
        }

        throw new InvalidPacketDataException("Force", object, index);
    }


    /**
     * @param index the index of the desired object
     *
     * @return the {@link Forces} value of the object at the specified index
     */
    public @Nullable Forces getForces(int index) throws InvalidPacketDataException {
        Object object = getObject(index);

        // Some consumers allow or require null return when Forces not found.
        if (object == null) {
            return null;
        }
        if (object instanceof Forces force) {
            return force;
        }

        throw new InvalidPacketDataException("Forces", object, index);
    }

    /**
     * @param index the index of the desired object
     *
     * @return a List of {@link Force}'s value of the object at the specified index
     */
    public List<Force> getForceList(int index) throws InvalidPacketDataException {
        Object object = getObject(index);

        ArrayList<Force> result = new ArrayList<>();

        // Users should now only use HashSet.
        if (object instanceof HashSet<?> set) {
            for (Object forces : set) {
                if (forces instanceof Force verifiedForce) {
                    result.add(verifiedForce);
                }
            }
            return result;
        }

        throw new InvalidPacketDataException("HashSet", object, index);
    }

    /**
     * @param index the index of the desired object
     *
     * @return the {@link GameOptions} value of the object at the specified index
     */
    public @Nullable GameOptions getGameOptions(int index) throws InvalidPacketDataException {
        Object object = getObject(index);

        if (object instanceof GameOptions gameOptions) {
            return gameOptions;
        }

        throw new InvalidPacketDataException("GameOptions", object, index);
    }

    /**
     * @param index the index of the desired object
     *
     * @return a List of {@link GameTurn}'s value of the object at the specified index
     */
    public List<GameTurn> getGameTurnList(int index) throws InvalidPacketDataException {
        Object object = getObject(index);

        ArrayList<GameTurn> result = new ArrayList<>();

        if (object instanceof List<?> list) {
            for (Object gameTurns : list) {
                if (gameTurns instanceof GameTurn verifiedGameTurn) {
                    result.add(verifiedGameTurn);
                }
            }
            return result;
        }

        throw new InvalidPacketDataException("List", object, index);
    }

    /**
     * @param index the index of the desired object
     *
     * @return the {@link Hex} value of the object at the specified index
     */
    public @Nullable Hex getHex(int index) throws InvalidPacketDataException {
        Object object = getObject(index);

        if (object instanceof Hex hex) {
            return hex;
        }

        throw new InvalidPacketDataException("Hex", object, index);
    }

    /**
     * @param index the index of the desired object
     *
     * @return a Vector of {@link Player}'s value of the object at the specified index
     */
    public Vector<IBasicOption> getIBasicOptionVector(int index) throws InvalidPacketDataException {
        Object object = getObject(index);

        Vector<IBasicOption> result = new Vector<>();

        if (object instanceof Vector<?> vector) {
            for (Object option : vector) {
                if (option instanceof IBasicOption verifiedValue) {
                    result.add(verifiedValue);
                }
            }
            return result;
        }

        throw new InvalidPacketDataException("Vector", object, index);
    }

    /**
     * @param index the index of the desired object
     *
     * @return the {@link InGameObject} value of the object at the specified index
     */
    public @Nullable InGameObject getInGameObject(int index) throws InvalidPacketDataException {
        Object object = getObject(index);

        if (object instanceof InGameObject value) {
            return value;
        }

        throw new InvalidPacketDataException("InGameObject", object, index);
    }

    /**
     * @param index the index of the desired object
     *
     * @return a List of {@link InGameObject}'s value of the object at the specified index
     */
    public List<InGameObject> getInGameObjectList(int index) throws InvalidPacketDataException {
        Object object = getObject(index);

        ArrayList<InGameObject> result = new ArrayList<>();

        if (object instanceof List<?> list) {
            for (Object report : list) {
                if (report instanceof InGameObject value) {
                    result.add(value);
                }
            }
            return result;
        }

        throw new InvalidPacketDataException("List", object, index);
    }

    /**
     * @param index the index of the desired object
     *
     * @return the {@link MapSettings} value of the object at the specified index
     */
    public @Nullable MapSettings getMapSettings(int index) throws InvalidPacketDataException {
        Object object = getObject(index);

        if (object instanceof MapSettings mapSettings) {
            return mapSettings;
        }

        throw new InvalidPacketDataException("MapSettings", object, index);
    }

    /**
     * @param index the index of the desired object
     *
     * @return the {@link Minefield} value of the object at the specified index
     */
    public @Nullable Minefield getMinefield(int index) throws InvalidPacketDataException {
        Object object = getObject(index);

        if (object instanceof Minefield minefield) {
            return minefield;
        }

        throw new InvalidPacketDataException("Minefield", object, index);
    }

    /**
     * @param index the index of the desired object
     *
     * @return a Vector of {@link Minefield}'s value of the object at the specified index
     */
    public Vector<Minefield> getMinefieldVector(int index) throws InvalidPacketDataException {
        Object object = getObject(index);

        Vector<Minefield> result = new Vector<>();

        if (object instanceof Vector<?> vector) {
            for (Object minefield : vector) {
                if (minefield instanceof Minefield verifiedMinefield) {
                    result.add(verifiedMinefield);
                }
            }
            return result;
        }

        throw new InvalidPacketDataException("Vector", object, index);
    }

    /**
     * @param index the index of the desired object
     *
     * @return the {@link MovePath} value of the object at the specified index
     */
    public @Nullable MovePath getMovePath(int index) throws InvalidPacketDataException {
        Object object = getObject(index);

        if (object instanceof MovePath movePath) {
            return movePath;
        }

        throw new InvalidPacketDataException("MovePath", object, index);
    }

    /**
     * @param index the index of the desired object
     *
     * @return the {@link PacketCommand} value of the object at the specified index
     */
    public @Nullable PacketCommand getPacketCommand(int index) {
        Object object = getObject(index);

        if (object instanceof PacketCommand value) {
            return value;
        }

        return null;
    }

    /**
     * @param index the index of the desired object
     *
     * @return the {@link PlanetaryConditions} value of the object at the specified index
     */
    public @Nullable PlanetaryConditions getPlanetaryConditions(int index) throws InvalidPacketDataException {
        Object object = getObject(index);

        if (object instanceof PlanetaryConditions planetaryConditions) {
            return planetaryConditions;
        }

        throw new InvalidPacketDataException("PlanetaryConditions", object, index);
    }

    /**
     * @param index the index of the desired object
     *
     * @return the {@link Player} value of the object at the specified index
     */
    public @Nullable Player getPlayer(int index) throws InvalidPacketDataException {
        Object object = getObject(index);

        if (object instanceof Player value) {
            return value;
        }

        throw new InvalidPacketDataException("Player", object, index);
    }

    /**
     * @param index the index of the desired object
     *
     * @return a Vector of {@link Player}'s value of the object at the specified index
     */
    public PlayerIDAndList<BoardLocation> getPlayerIDAndListWithBoardLocation(int index)
          throws InvalidPacketDataException {
        Object object = getObject(index);

        PlayerIDAndList<BoardLocation> result = new PlayerIDAndList<>();

        if (object instanceof PlayerIDAndList<?> collection) {
            result.setPlayerID(collection.getPlayerID());

            for (Object player : collection) {
                if (player instanceof BoardLocation value) {
                    result.add(value);
                }
            }
            return result;
        }

        throw new InvalidPacketDataException("PlayerIDAndList", object, index);
    }

    /**
     * @param index the index of the desired object
     *
     * @return a Vector of {@link Player}'s value of the object at the specified index
     */
    public Vector<Player> getPlayerVector(int index) throws InvalidPacketDataException {
        Object object = getObject(index);

        Vector<Player> result = new Vector<>();

        if (object instanceof Vector<?> vector) {
            for (Object player : vector) {
                if (player instanceof Player verifiedPlayer) {
                    result.add(verifiedPlayer);
                }
            }
            return result;
        }

        throw new InvalidPacketDataException("Vector", object, index);
    }

    /**
     * @param index the index of the desired object
     *
     * @return a List of {@link Report}'s value of the object at the specified index
     */
    public List<Report> getReportList(int index) throws InvalidPacketDataException {
        Object object = getObject(index);

        ArrayList<Report> result = new ArrayList<>();

        if (object instanceof Vector<?> vector) {
            for (Object report : vector) {
                if (report instanceof Report verifiedReport) {
                    result.add(verifiedReport);
                }
            }
            return result;
        }

        throw new InvalidPacketDataException("Vector", object, index);
    }

    /**
     * @param index the index of the desired object
     *
     * @return a List of Lists of {@link Report}'s value of the object at the specified index
     */
    public List<List<Report>> getReportListOfList(int index) throws InvalidPacketDataException {
        Object object = getObject(index);

        ArrayList<List<Report>> result = new ArrayList<>();

        // TODO: refactor SBFFullGameReport.java:createFilteredReport to _not_ return a HashMap
        // but instead a list of lists, or refactor all other creators of data objects for this
        // method to also create HashMaps (this may reduce bandwidth usage slightly).
        if (object instanceof Collection<?> collection) {
            for (Object reportList : collection) {
                if (reportList instanceof List<?> verifiedReportList) {
                    ArrayList<Report> verifiedReports = new ArrayList<>();

                    for (Object report : verifiedReportList) {
                        if (report instanceof Report verifiedReport) {
                            verifiedReports.add(verifiedReport);
                        }
                    }

                    result.add(verifiedReports);
                }
            }
            return result;
        }

        throw new InvalidPacketDataException("Collection<?>", object, index);
    }

    /**
     * @param index the index of the desired object
     *
     * @return the {@link megamek.common.strategicBattleSystems.SBFMovePath} value of the object at the specified index
     */
    public @Nullable SBFMovePath getSBFMovePath(int index) throws InvalidPacketDataException {
        Object object = getObject(index);

        if (object instanceof SBFMovePath sbfMovePath) {
            return sbfMovePath;
        }

        throw new InvalidPacketDataException("SBFMovePath", object, index);
    }

    /**
     * @param index the index of the desired object
     *
     * @return a List of {@link SBFReportEntry}'s value of the object at the specified index
     */
    public List<SBFReportEntry> getSBFReportEntryList(int index) throws InvalidPacketDataException {
        Object object = getObject(index);

        ArrayList<SBFReportEntry> result = new ArrayList<>();

        if (object instanceof Collection<?> collection) {
            for (Object report : collection) {
                if (report instanceof SBFReportEntry value) {
                    result.add(value);
                }
            }
            return result;
        }

        throw new InvalidPacketDataException("Collection<?>", object, index);
    }

    /**
     * @param index the index of the desired object
     *
     * @return a List of {@link SBFTurn}'s value of the object at the specified index
     */
    public List<SBFTurn> getSBFTurnList(int index) throws InvalidPacketDataException {
        Object object = getObject(index);

        ArrayList<SBFTurn> result = new ArrayList<>();

        if (object instanceof List<?> list) {
            for (Object report : list) {
                if (report instanceof SBFTurn value) {
                    result.add(value);
                }
            }
            return result;
        }

        throw new InvalidPacketDataException("List", object, index);
    }

    /**
     * @param index the index of the desired object
     *
     * @return the {@link SmokeCloud} value of the object at the specified index
     */
    public @Nullable SmokeCloud getSmokeCloud(int index) throws InvalidPacketDataException {
        Object object = getObject(index);

        if (object instanceof SmokeCloud smokeCloud) {
            return smokeCloud;
        }

        throw new InvalidPacketDataException("SmokeCloud", object, index);
    }

    /**
     * @param index the index of the desired object
     *
     * @return the {@link SpecialHexDisplay} value of the object at the specified index
     */
    public @Nullable SpecialHexDisplay getSpecialHexDisplay(int index) throws InvalidPacketDataException {
        Object object = getObject(index);

        if (object instanceof SpecialHexDisplay value) {
            return value;
        }

        throw new InvalidPacketDataException("SpecialHexDisplay", object, index);
    }


    /**
     * @param index the index of the desired object
     *
     * @return a Vector of {@link TagInfo}'s value of the object at the specified index
     */
    public Vector<TagInfo> getTagInfoVector(int index) throws InvalidPacketDataException {
        Object object = getObject(index);

        Vector<TagInfo> result = new Vector<>();

        if (object instanceof Vector<?> vector) {
            for (Object tagInfo : vector) {
                if (tagInfo instanceof TagInfo verifiedTagInfo) {
                    result.add(verifiedTagInfo);
                }
            }
            return result;
        }

        throw new InvalidPacketDataException("List", object, index);
    }

    /**
     * @param index the index of the desired object
     *
     * @return a Vector of {@link UnitLocation}'s value of the object at the specified index
     */
    public @Nullable Vector<UnitLocation> getUnitLocationVector(int index) throws InvalidPacketDataException {
        Object object = getObject(index);

        Vector<UnitLocation> result = new Vector<>();

        // In some instances we may get a null entry; this is valid and handled in Game
        if (object == null) {
            return null;
        }
        if (object instanceof Vector<?> vector) {
            for (Object unitLocation : vector) {
                if (unitLocation instanceof UnitLocation verifiedLocation) {
                    result.add(verifiedLocation);
                }
            }
            return result;
        }

        throw new InvalidPacketDataException("Vector", object, index);

    }

    /**
     * @param index the index of the desired object
     *
     * @return a List of {@link WeaponAttackAction}'s value of the object at the specified index
     */
    public List<WeaponAttackAction> getWeaponAttackActionList(int index) throws InvalidPacketDataException {
        Object object = getObject(index);

        ArrayList<WeaponAttackAction> result = new ArrayList<>();

        if (object instanceof List<?> list) {
            for (Object value : list) {
                if (value instanceof WeaponAttackAction verifiedValue) {
                    result.add(verifiedValue);
                }
            }
            return result;
        }

        throw new InvalidPacketDataException("List", object, index);
    }

    /**
     * @param index the index of the desired object
     *
     * @return the {@link megamek.common.enums.WeaponSortOrder} value of the object at the specified index
     */
    public @Nullable WeaponSortOrder getWeaponSortOrder(int index) throws InvalidPacketDataException {
        Object object = getObject(index);

        if (object instanceof WeaponSortOrder value) {
            return value;
        }

        throw new InvalidPacketDataException("WeaponSortOrder", object, index);
    }

    @Override
    @Nonnull
    public String toString() {
        return "Packet [" + command + "] - " + Arrays.toString(data);
    }
}

/*
 * Copyright (c) 2024 - The MegaMek Team. All Rights Reserved.
 *
 * This file is part of MegaMek.
 *
 * MegaMek is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * MegaMek is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with MegaMek. If not, see <http://www.gnu.org/licenses/>.
 */
package megamek.common.jacksonadapters;

import com.fasterxml.jackson.core.JacksonException;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.deser.std.StdDeserializer;
import megamek.common.Board;
import megamek.common.Configuration;

import java.io.File;
import java.io.IOException;

public class BoardDeserializer extends StdDeserializer<Board> {

    private static final String TYPE = "type";
    private static final String LOW_ALTITUDE = "lowaltitude";
    private static final String ATMOSPHERIC = "atmospheric";
    private static final String SPACE = "space";
    private static final String HIGH_ALTITUDE = "highaltitude";
    private static final String RADAR = "radar";
    private static final String CAP_RADAR = "capitalradar";
    private static final String WIDTH = "width";
    private static final String HEIGHT = "height";
    private static final String COLS = "cols";
    private static final String ROWS = "rows";
    private static final String ID = "id";

    protected BoardDeserializer(Class<?> vc) {
        super(vc);
    }

    protected BoardDeserializer(JavaType valueType) {
        super(valueType);
    }

    protected BoardDeserializer(StdDeserializer<?> src) {
        super(src);
    }

    public static Board parseBoard(JsonNode mapNode, File basePath) {

        // "map: Xyz.board" will directly load that board with no modifiers
        if (!mapNode.isContainerNode()) {
            return loadBoard(mapNode.textValue(), basePath);
        }

        //TODO: Board handling - this is incomplete, compare ScenarioV1

        // more complex map setup
        int mapWidth = mapNode.has(WIDTH) ? mapNode.get(WIDTH).intValue() : 16;
        int mapHeight = mapNode.has(HEIGHT) ? mapNode.get(HEIGHT).intValue() : 17;
        int columns = mapNode.has(COLS) ? mapNode.get(COLS).intValue() : 1;
        int rows = mapNode.has(ROWS) ? mapNode.get(ROWS).intValue() : 1;
        // TODO: board ID

        Board board;
        if (mapNode.has(TYPE)) {
            String type = mapNode.get(TYPE).asText();
            board = new Board(mapWidth, mapHeight);
            if (type.equals(ATMOSPHERIC) || type.equals(LOW_ALTITUDE)) {
                return Board.newAtmosphericBoard(mapWidth, mapHeight);
            } else if (type.equals(SPACE)) {
                board.setType(Board.T_SPACE);
            } else if (type.equals(HIGH_ALTITUDE)) {
                //TODO: dont have that type yet
                board.setType(Board.T_SPACE);
            }
            return board;
        }


//
//        // load available boards
//        // basically copied from Server.java. Should get moved somewhere neutral
//        List<String> boards = new ArrayList<>();
//
//        // Find subdirectories given in the scenario file
//        List<String> allDirs = new LinkedList<>();
//        // "" entry stands for the boards base directory
//        allDirs.add("");
//
        return null;
//        Entity entity = loadEntity(node);
//        assignPosition(entity, node);
//        assignFacing(entity, node);
//        assignDeploymentRound(entity, node);
//        assignStatus(entity, node);
//        assignIndividualCamo(entity, node);
//        assignElevation(entity, node);
//        assignAltitude(entity, node);
//        assignVelocity(entity, node);
//        CrewDeserializer.parseCrew(node, entity);
//        return entity;
    }

    private static Board loadBoard(String fileName, File basePath) {
        File boardFile = new File(basePath, fileName);
        if (!boardFile.exists()) {
            boardFile = new File(Configuration.boardsDir(), fileName);
            if (!boardFile.exists()) {
                throw new IllegalArgumentException("Board file does not exist: " + boardFile + " in " + basePath);
            }
        }
        Board result = new Board();
        result.load(boardFile);
        return result;
    }

    @Override
    public Board deserialize(JsonParser p, DeserializationContext ctxt) throws IOException, JacksonException {
        return parseBoard(p.getCodec().readTree(p), new File(""));
    }

}

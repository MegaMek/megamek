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

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.deser.std.StdDeserializer;

import megamek.common.Board;
import megamek.common.BoardType;
import megamek.common.Compute;
import megamek.common.Configuration;
import megamek.common.Coords;
import megamek.common.MapSettings;
import megamek.common.board.postprocess.BoardProcessor;
import megamek.common.util.BoardUtilities;
import megamek.logging.MMLogger;

public class BoardDeserializer extends StdDeserializer<Board> {
    private static final MMLogger logger = MMLogger.create(BoardDeserializer.class);

    private static final String TYPE = "type";
    private static final String LOW_ALTITUDE = "lowaltitude";
    private static final String ATMOSPHERIC = "atmospheric";
    private static final String SKY = "sky";
    private static final String SPACE = "space";
    private static final String HIGH_ALTITUDE = "highaltitude";
    private static final String WIDTH = "width";
    private static final String HEIGHT = "height";
    private static final String COLS = "cols";
    private static final String BOARDS = "boards";
    private static final String FILE = "file";
    private static final String ROTATE = "rotate";
    private static final String FLIP_H = "fliph"; // scrambles the map
    private static final String FLIP_V = "flipv"; // scrambles the map
    private static final String MODIFY = "modify";
    private static final String SURPRISE = "surprise";
    private static final String RADAR = "radar";
    private static final String CAP_RADAR = "capitalradar";
    private static final String ID = "id";
    private static final String PROCESS = "postprocess";
    private static final String AT = "at";
    private static final String NAME = "name";
    private static final String EMBED = "embed";

    protected BoardDeserializer(Class<?> vc) {
        super(vc);
    }

    /**
     * Parses the given map: or maps: node to return a list of one or more boards (the list should ideally never be
     * empty, an exception being thrown instead). Board files are tried first in the given basePath; if not found there,
     * MM's data/boards/ is tried instead.
     *
     * @param mapNode  a map: or maps: node from a YAML definition file
     * @param basePath a path to search board files in (e.g. scenario path)
     *
     * @return a list of parsed boards
     *
     * @throws IllegalArgumentException for illegal node combinations and other errors
     */
    public static List<Board> parse(JsonNode mapNode, File basePath) {
        // the map node is
        // - textual, giving the board file directly
        // - one board node, or
        // - an array of board nodes

        List<Board> result = new ArrayList<>();
        if (!mapNode.isContainerNode()) {
            // "map: xyz.board" will directly load that board with no modifiers
            result.add(loadBoard(mapNode.textValue(), basePath));

        } else if (mapNode.isArray()) {
            // as an array of multiple boards, it cannot be a simple string; each entry must
            // be a board node
            result.addAll(parseMultipleBoards(mapNode, basePath));

        } else {
            Board board = parseSingleBoard(mapNode, basePath);
            if (board != null) {
                result.add(board);
            }
        }
        return result;
    }

    private static List<Board> parseMultipleBoards(JsonNode node, File basePath) {
        List<Board> result = new ArrayList<>();
        if (!node.isArray()) {
            logger.error("Called parseMultipleBoards with non-array node!");
            return result;
        }
        node.elements().forEachRemaining(n -> result.add(parseSingleBoard(n, basePath)));
        return result;
    }

    public static Board parseSingleBoard(JsonNode mapNode, File basePath) {
        testBoardNodeFields(mapNode);

        if (mapNode.has(FILE)) {
            // map: as node with file: and optional modify:
            String fileName = mapNode.get(FILE).textValue();
            Board board = parseBoardFileNode(mapNode, basePath, fileName);
            board.setBoardType(BoardType.GROUND);
            board.setMapName(processName(mapNode).orElse(fileName));
            board.setBoardId(parseId(mapNode));
            return board;

        } else if (mapNode.has(SURPRISE)) {
            // map: as node with surprise: filelist and optional modify:
            if (!mapNode.get(SURPRISE).isArray()) {
                throw new IllegalArgumentException("Surprise keyword without boards list!");
            }
            List<Board> surpriseBoardsList = parseMultipleBoards(mapNode.get(SURPRISE), basePath);
            Board board = Compute.randomListElement(surpriseBoardsList);
            parseBoardModifiers(board, mapNode);
            parseBoardProcessors(board, mapNode);
            board.setBoardType(BoardType.GROUND);
            board.setMapName(processName(mapNode).orElse("Surprise Map"));
            board.setBoardId(parseId(mapNode));
            return board;
        }

        // more complex map setup
        int mapWidth = mapNode.has(WIDTH) ? mapNode.get(WIDTH).intValue() : 16;
        int mapHeight = mapNode.has(HEIGHT) ? mapNode.get(HEIGHT).intValue() : 17;
        int columns = mapNode.has(COLS) ? mapNode.get(COLS).intValue() : 1;

        Board board;
        if (mapNode.has(TYPE)) {
            String type = mapNode.get(TYPE).asText();
            board = new Board(mapWidth, mapHeight);
            parseEmbeddedBoards(board, mapNode);
            switch (type) {
                case SKY:
                    board = Board.getSkyBoard(mapWidth, mapHeight);
                    board.setBoardType(BoardType.SKY);
                    board.setMapName(processName(mapNode).orElse("Atmospheric Map"));
                    board.setBoardId(parseId(mapNode));
                    parseEmbeddedBoards(board, mapNode);
                    return board;
                case ATMOSPHERIC:
                case LOW_ALTITUDE:
                    board = Board.getSkyBoard(mapWidth, mapHeight);
                    board.setBoardType(BoardType.SKY_WITH_TERRAIN);
                    board.setMapName(processName(mapNode).orElse("Atmospheric Map"));
                    board.setBoardId(parseId(mapNode));
                    parseEmbeddedBoards(board, mapNode);
                    return board;
                case SPACE:
                    board = Board.getSpaceBoard(mapWidth, mapHeight);
                    board.setBoardType(BoardType.FAR_SPACE);
                    board.setMapName(processName(mapNode).orElse("Space Map"));
                    board.setBoardId(parseId(mapNode));
                    parseEmbeddedBoards(board, mapNode);
                    return board;
                case HIGH_ALTITUDE:
                    // TODO: dont have that type yet
                    board.setBoardType(BoardType.NEAR_SPACE);
                    board.setMapName(processName(mapNode).orElse("High-Atmosphere Map"));
                    board.setBoardId(parseId(mapNode));
                    break;
            }
        } else {
            // ground map
            // this is the only map type that allows combining multiple board files
            JsonNode boardsNode = mapNode.get(BOARDS);
            if (!boardsNode.isArray()) {
                throw new IllegalArgumentException("Must give multiple boards!");
            }

            List<Board> boardsList = parseMultipleBoards(boardsNode, basePath);
            mapWidth = boardsList.get(0).getWidth();
            mapHeight = boardsList.get(0).getHeight();
            int rows = boardsList.size() / columns;
            if (boardsList.size() != columns * rows) {
                throw new IllegalArgumentException("The number of given boards must give full rows!");
            }
            List<Boolean> isRotatedList = new ArrayList<>();
            Collections.fill(isRotatedList, Boolean.FALSE);
            board = BoardUtilities.combine(mapWidth, mapHeight, columns, rows, boardsList, isRotatedList,
                  MapSettings.MEDIUM_GROUND);
        }
        parseBoardProcessors(board, mapNode);
        parseEmbeddedBoards(board, mapNode);
        board.setBoardType(BoardType.GROUND);
        board.setMapName(processName(mapNode).orElse("Ground Map"));
        board.setBoardId(parseId(mapNode));
        return board;
    }

    private static Optional<String> processName(JsonNode boardNode) {
        return Optional.ofNullable(boardNode.has(NAME) ? boardNode.get(NAME).asText() : null);
    }

    private static int parseId(JsonNode boardNode) {
        return boardNode.has(ID) ? boardNode.get(ID).intValue() : 0;
    }

    private static void parseEmbeddedBoards(Board board, JsonNode boardNode) {
        if (boardNode.has(EMBED) && boardNode.get(EMBED).isArray()) {
            boardNode.get(EMBED).iterator().forEachRemaining(n -> parseSingleEmbeddedBoard(board, n));
        }
    }

    private static void parseSingleEmbeddedBoard(Board board, JsonNode embedNode) {
        MMUReader.requireFields("Board", embedNode, AT, ID);
        Coords coords = CoordsDeserializer.parseNode(embedNode.get(AT));
        board.setEmbeddedBoard(embedNode.get(ID).intValue(), coords);
    }

    private static Board parseBoardFileNode(JsonNode boardNode, File basePath, String fileName) {
        // map: as node with file: and optional rotate:
        Board board = loadBoard(fileName, basePath);
        parseBoardModifiers(board, boardNode);
        parseBoardProcessors(board, boardNode);
        return board;
    }

    private static void parseBoardProcessors(Board board, JsonNode boardNode) {
        if (boardNode.has(PROCESS)) {
            List<BoardProcessor> processors = BoardProcessorDeserializer.parseNode(boardNode.get(PROCESS));
            for (BoardProcessor processor : processors) {
                processor.processBoard(board);
            }
        }
    }

    private static void parseBoardModifiers(Board board, JsonNode boardNode) {
        if (boardNode.has(MODIFY)) {
            JsonNode modifierNode = boardNode.get(MODIFY);
            if (modifierNode.isArray()) {
                modifierNode.iterator().forEachRemaining(n -> parseSingleBoardModifier(board, n.textValue()));
            } else if (modifierNode.isTextual()) {
                parseSingleBoardModifier(board, modifierNode.asText());
            }
        }
    }

    private static void parseSingleBoardModifier(Board board, String modifier) {
        switch (modifier) {
            case ROTATE:
                BoardUtilities.flip(board, true, true);
                break;
            case FLIP_H:
                BoardUtilities.flip(board, true, false);
                break;
            case FLIP_V:
                BoardUtilities.flip(board, false, true);
                break;
            default:
                throw new IllegalArgumentException("Unknown modifier " + modifier);
        }
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
    public Board deserialize(JsonParser p, DeserializationContext ctxt) throws IOException {
        return parse(p.getCodec().readTree(p), new File("")).get(0);
    }

    private static void testBoardNodeFields(JsonNode mapNode) {
        MMUReader.disallowCombinedFields("Board", mapNode, FILE, SURPRISE);
        MMUReader.disallowCombinedFields("Board", mapNode, FILE, WIDTH);
        MMUReader.disallowCombinedFields("Board", mapNode, FILE, HEIGHT);
        MMUReader.disallowCombinedFields("Board", mapNode, FILE, BOARDS);
        MMUReader.disallowCombinedFields("Board", mapNode, FILE, COLS);

        MMUReader.disallowCombinedFields("Board", mapNode, TYPE, COLS);
        MMUReader.disallowCombinedFields("Board", mapNode, TYPE, FILE);
        MMUReader.disallowCombinedFields("Board", mapNode, TYPE, SURPRISE);
        MMUReader.disallowCombinedFields("Board", mapNode, TYPE, MODIFY);
        MMUReader.disallowCombinedFields("Board", mapNode, TYPE, BOARDS);
    }
}

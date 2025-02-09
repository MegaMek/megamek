/*
 * Copyright (c) 2025 - The MegaMek Team. All Rights Reserved.
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
 *
 */
package megamek.ai.dataset;

import megamek.common.MovePath;

import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;


/**
 * serializer and deserializer for game dataset UnitAction
 * @author Luana Coppio
 */
public class UnitActionSerde extends TsvSerde<UnitAction> {

    private final DecimalFormat LOG_DECIMAL =
        new DecimalFormat("0.00", DecimalFormatSymbols.getInstance());

    @Override
    public String toTsv(UnitAction obj) {
        // Create an array with one slot per column defined in the enum.
        String[] row = new String[UnitActionField.values().length];

        row[UnitActionField.ENTITY_ID.ordinal()] = String.valueOf(obj.id());
        row[UnitActionField.PLAYER_ID.ordinal()] = String.valueOf(obj.playerId());
        row[UnitActionField.TEAM_ID.ordinal()] = String.valueOf(obj.teamId());
        row[UnitActionField.FACING.ordinal()] = String.valueOf(obj.facing());
        row[UnitActionField.FROM_X.ordinal()] = String.valueOf(obj.fromX());
        row[UnitActionField.FROM_Y.ordinal()] = String.valueOf(obj.fromY());
        row[UnitActionField.TO_X.ordinal()] = String.valueOf(obj.toX());
        row[UnitActionField.TO_Y.ordinal()] = String.valueOf(obj.toY());
        row[UnitActionField.HEXES_MOVED.ordinal()] = String.valueOf(obj.hexesMoved());
        row[UnitActionField.DISTANCE.ordinal()] = String.valueOf(obj.distance());
        row[UnitActionField.MP_USED.ordinal()] = String.valueOf(obj.mpUsed());
        row[UnitActionField.MAX_MP.ordinal()] = String.valueOf(obj.maxMp());
        row[UnitActionField.MP_P.ordinal()] = LOG_DECIMAL.format(obj.mpP());
        row[UnitActionField.HEAT_P.ordinal()] = LOG_DECIMAL.format(obj.heatP());
        row[UnitActionField.ARMOR_P.ordinal()] = LOG_DECIMAL.format(obj.armorP());
        row[UnitActionField.INTERNAL_P.ordinal()] = LOG_DECIMAL.format(obj.internalP());
        row[UnitActionField.JUMPING.ordinal()] = obj.jumping() ? "1" : "0";
        row[UnitActionField.PRONE.ordinal()] = obj.prone() ? "1" : "0";
        row[UnitActionField.LEGAL.ordinal()] = obj.legal() ? "1" : "0";

        // For STEPS, join the list of MoveStepType values with a space.
        row[UnitActionField.STEPS.ordinal()] = obj.steps().stream()
            .map(Enum::name)
            .collect(Collectors.joining(" "));

        row[UnitActionField.CHANCE_OF_FAILURE.ordinal()] =
            LOG_DECIMAL.format(obj.chanceOfFailure());

        return String.join("\t", row);
    }

    public UnitAction fromTsv(String line, int idOffset) {
        String[] parts = line.split("\t", -1); // include trailing empty strings
        // For the fields that are not part of UnitAction (PLAYER_ID, CHASSIS, MODEL), we ignore the values.
        int entityId = Integer.parseInt(parts[UnitActionField.ENTITY_ID.ordinal()]) + idOffset;
        int playerId = Integer.parseInt(parts[UnitActionField.PLAYER_ID.ordinal()]);
        int teamId = Integer.parseInt(parts[UnitActionField.TEAM_ID.ordinal()]);
        int facing = Integer.parseInt(parts[UnitActionField.FACING.ordinal()]);
        int fromX = Integer.parseInt(parts[UnitActionField.FROM_X.ordinal()]);
        int fromY = Integer.parseInt(parts[UnitActionField.FROM_Y.ordinal()]);
        int toX = Integer.parseInt(parts[UnitActionField.TO_X.ordinal()]);
        int toY = Integer.parseInt(parts[UnitActionField.TO_Y.ordinal()]);
        int hexesMoved = Integer.parseInt(parts[UnitActionField.HEXES_MOVED.ordinal()]);
        int distance = Integer.parseInt(parts[UnitActionField.DISTANCE.ordinal()]);
        int mpUsed = Integer.parseInt(parts[UnitActionField.MP_USED.ordinal()]);
        int maxMp = Integer.parseInt(parts[UnitActionField.MAX_MP.ordinal()]);
        double mpP = Double.parseDouble(parts[UnitActionField.MP_P.ordinal()]);
        double heatP = Double.parseDouble(parts[UnitActionField.HEAT_P.ordinal()]);
        double armorP = Double.parseDouble(parts[UnitActionField.ARMOR_P.ordinal()]);
        double internalP = Double.parseDouble(parts[UnitActionField.INTERNAL_P.ordinal()]);
        boolean jumping = "1".equals(parts[UnitActionField.JUMPING.ordinal()]);
        boolean prone = "1".equals(parts[UnitActionField.PRONE.ordinal()]);
        boolean legal = "1".equals(parts[UnitActionField.LEGAL.ordinal()]);
        double chanceOfFailure = Double.parseDouble(parts[UnitActionField.CHANCE_OF_FAILURE.ordinal()]);

        // Convert the steps field (a space-separated list) back to a List of MoveStepType.
        List<MovePath.MoveStepType> steps = Arrays.stream(
                parts[UnitActionField.STEPS.ordinal()].split(" "))
            .filter(s -> !s.isEmpty())
            .map(MovePath.MoveStepType::valueOf)
            .collect(Collectors.toList());

        return new UnitAction(
            entityId,
            teamId,
            playerId,
            facing,
            fromX,
            fromY,
            toX,
            toY,
            hexesMoved,
            distance,
            mpUsed,
            maxMp,
            mpP,
            heatP,
            armorP,
            internalP,
            jumping,
            prone,
            legal,
            chanceOfFailure,
            steps
        );
    }

    @Override
    public String getHeaderLine() {
        return UnitActionField.getHeaderLine();
    }

    private enum UnitActionField {
        ENTITY_ID("ENTITY_ID"),
        TEAM_ID("TEAM_ID"),
        PLAYER_ID("PLAYER_ID"),
        FACING("FACING"),
        FROM_X("FROM_X"),
        FROM_Y("FROM_Y"),
        TO_X("TO_X"),
        TO_Y("TO_Y"),
        HEXES_MOVED("HEXES_MOVED"),
        DISTANCE("DISTANCE"),
        MP_USED("MP_USED"),
        MAX_MP("MAX_MP"),
        MP_P("MP_P"),
        HEAT_P("HEAT_P"),
        ARMOR_P("ARMOR_P"),
        INTERNAL_P("INTERNAL_P"),
        JUMPING("JUMPING"),
        PRONE("PRONE"),
        LEGAL("LEGAL"),
        STEPS("STEPS"),
        CHANCE_OF_FAILURE("CHANCE_OF_FAILURE");

        private final String headerName;

        UnitActionField(String headerName) {
            this.headerName = headerName;
        }

        public String getHeaderName() {
            return headerName;
        }

        /**
         * Builds the TSV header line (joined by tabs) by iterating over all enum constants.
         */
        public static String getHeaderLine() {
            return Arrays.stream(values())
                .map(UnitActionField::getHeaderName)
                .collect(Collectors.joining("\t"));
        }
    }
}

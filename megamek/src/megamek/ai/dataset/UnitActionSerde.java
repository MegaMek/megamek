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
 * <p>serializer and deserializer for UnitAction to/from TSV format.</p>
 * @author Luana Coppio
 */
public class UnitActionSerde extends TsvSerde<UnitAction> {

    private final DecimalFormat LOG_DECIMAL =
        new DecimalFormat("0.00", DecimalFormatSymbols.getInstance());

    @Override
    public String toTsv(UnitAction obj) {
        String[] row = new String[UnitActionField.values().length];
        row[UnitActionField.ENTITY_ID.ordinal()] = String.valueOf(obj.id());
        row[UnitActionField.PLAYER_ID.ordinal()] = String.valueOf(obj.playerId());
        row[UnitActionField.TEAM_ID.ordinal()] = String.valueOf(obj.teamId());
        row[UnitActionField.CHASSIS.ordinal()] = obj.chassis();
        row[UnitActionField.MODEL.ordinal()] = obj.model();
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
        row[UnitActionField.CHANCE_OF_FAILURE.ordinal()] = LOG_DECIMAL.format(obj.chanceOfFailure());
        row[UnitActionField.IS_BOT.ordinal()] = obj.bot() ? "1" : "0";

        // For STEPS, join the list of MoveStepType values with a space.
        row[UnitActionField.STEPS.ordinal()] = obj.steps().stream()
            .map(Enum::name)
            .collect(Collectors.joining(" "));

        return String.join("\t", row);
    }

    public UnitAction fromTsv(String line, int idOffset) throws NumberFormatException {
        String[] parts = line.split("\t", -1);
        int entityId = Integer.parseInt(parts[UnitActionField.ENTITY_ID.ordinal()]) + idOffset;
        int playerId = Integer.parseInt(parts[UnitActionField.PLAYER_ID.ordinal()]);
        String chassis = parts[UnitActionField.CHASSIS.ordinal()];
        String model = parts[UnitActionField.MODEL.ordinal()];
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
        boolean bot = false;
        int teamId = -1;
        double chanceOfFailure = 0.0;
        if (parts.length >= 23) {
            teamId = Integer.parseInt(parts[UnitActionField.TEAM_ID.ordinal()]);
            chanceOfFailure = Double.parseDouble(parts[UnitActionField.CHANCE_OF_FAILURE.ordinal()]);
            bot = "1".equals(parts[UnitActionField.IS_BOT.ordinal()]);
        }
        // Convert the steps field (a space-separated list) back to a List of MoveStepType.
        List<MovePath.MoveStepType> steps = Arrays.stream(
                parts[UnitActionField.STEPS.ordinal()].split(" "))
            .filter(s -> !s.isEmpty())
            .map(MovePath.MoveStepType::fromLabel)
            .collect(Collectors.toList());

        return new UnitAction(
            entityId,
            teamId,
            playerId,
            chassis,
            model,
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
            steps,
            bot
        );
    }

    @Override
    public String getHeaderLine() {
        return UnitActionField.getHeaderLine();
    }
}

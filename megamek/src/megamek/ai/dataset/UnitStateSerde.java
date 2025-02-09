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

import megamek.common.Compute;
import megamek.common.Entity;
import megamek.common.MekSummary;
import megamek.common.UnitRole;
import megamek.common.enums.GamePhase;

import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.Arrays;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * serializer and deserializer for game dataset UnitState
 * @author Luana Coppio
 */
public class UnitStateSerde extends TsvSerde<UnitState> {

    private final DecimalFormat LOG_DECIMAL = new DecimalFormat("0.00", DecimalFormatSymbols.getInstance());

    @Override
    public String toTsv(UnitState obj) {
        String[] row = new String[UnitStateSerde.UnitStateField.values().length];
        row[UnitStateSerde.UnitStateField.ROUND.ordinal()] = String.valueOf(obj.round());
        row[UnitStateSerde.UnitStateField.PHASE.ordinal()] = obj.phase().name();
        row[UnitStateSerde.UnitStateField.PLAYER_ID.ordinal()] = String.valueOf(obj.playerId());
        row[UnitStateSerde.UnitStateField.ENTITY_ID.ordinal()] = String.valueOf(obj.id());
        row[UnitStateSerde.UnitStateField.CHASSIS.ordinal()] = obj.chassis();
        row[UnitStateSerde.UnitStateField.MODEL.ordinal()] = obj.chassis();
        row[UnitStateSerde.UnitStateField.TYPE.ordinal()] = obj.type();
        row[UnitStateSerde.UnitStateField.ROLE.ordinal()] = obj.role() == null ? UnitRole.NONE.name() : obj.role().name();
        row[UnitStateSerde.UnitStateField.X.ordinal()] = String.valueOf(obj.x());
        row[UnitStateSerde.UnitStateField.Y.ordinal()] = String.valueOf(obj.y());
        row[UnitStateSerde.UnitStateField.FACING.ordinal()] = String.valueOf(obj.facing());
        row[UnitStateSerde.UnitStateField.MP.ordinal()] = LOG_DECIMAL.format(obj.mp());
        row[UnitStateSerde.UnitStateField.HEAT.ordinal()] = LOG_DECIMAL.format(obj.heat());
        row[UnitStateSerde.UnitStateField.PRONE.ordinal()] = obj.prone() ? "1" : "0";
        row[UnitStateSerde.UnitStateField.AIRBORNE.ordinal()] = obj.airborne() ? "1" : "0";
        row[UnitStateSerde.UnitStateField.OFF_BOARD.ordinal()] = obj.offBoard() ? "1" : "0";
        row[UnitStateSerde.UnitStateField.CRIPPLED.ordinal()] = obj.crippled() ? "1" : "0";
        row[UnitStateSerde.UnitStateField.DESTROYED.ordinal()] = obj.destroyed() ? "1" : "0";
        row[UnitStateSerde.UnitStateField.ARMOR_P.ordinal()] = LOG_DECIMAL.format(obj.armorP());
        row[UnitStateSerde.UnitStateField.INTERNAL_P.ordinal()] = LOG_DECIMAL.format(obj.internalP());
        row[UnitStateSerde.UnitStateField.DONE.ordinal()] = obj.done() ? "1" : "0";
        row[UnitStateSerde.UnitStateField.TEAM_ID.ordinal()] = String.valueOf(obj.teamId());
        row[UnitStateSerde.UnitStateField.MAX_RANGE.ordinal()] = String.valueOf(obj.maxRange());
        row[UnitStateSerde.UnitStateField.TOTAL_DAMAGE.ordinal()] = String.valueOf(obj.totalDamage());

        return String.join("\t", row);
    }

    public UnitState fromTsv(String line, Map<Integer, Entity> entities, int idOffset) {
        String[] parts = line.split("\t", -1);

        int id = Integer.parseInt(parts[UnitStateField.ENTITY_ID.ordinal()]) + idOffset;
        GamePhase phase = GamePhase.valueOf(parts[UnitStateField.PHASE.ordinal()]);
        int teamId = Integer.parseInt(parts[UnitStateField.TEAM_ID.ordinal()]);
        int round = Integer.parseInt(parts[UnitStateField.ROUND.ordinal()]);
        int playerId = Integer.parseInt(parts[UnitStateField.PLAYER_ID.ordinal()]);
        String chassis = parts[UnitStateField.CHASSIS.ordinal()];
        String model = parts[UnitStateField.MODEL.ordinal()];
        String type = parts[UnitStateField.TYPE.ordinal()];
        UnitRole role = UnitRole.valueOf(parts[UnitStateField.ROLE.ordinal()]);
        int x = Integer.parseInt(parts[UnitStateField.X.ordinal()]);
        int y = Integer.parseInt(parts[UnitStateField.Y.ordinal()]);
        int facing = Integer.parseInt(parts[UnitStateField.FACING.ordinal()]);
        double mp = Double.parseDouble(parts[UnitStateField.MP.ordinal()]);
        double heat = Double.parseDouble(parts[UnitStateField.HEAT.ordinal()]);
        boolean prone = "1".equals(parts[UnitStateField.PRONE.ordinal()]);
        boolean airborne = "1".equals(parts[UnitStateField.AIRBORNE.ordinal()]);
        boolean offBoard = "1".equals(parts[UnitStateField.OFF_BOARD.ordinal()]);
        boolean crippled = "1".equals(parts[UnitStateField.CRIPPLED.ordinal()]);
        boolean destroyed = "1".equals(parts[UnitStateField.DESTROYED.ordinal()]);
        double armorP = Double.parseDouble(parts[UnitStateField.ARMOR_P.ordinal()]);
        double internalP = Double.parseDouble(parts[UnitStateField.INTERNAL_P.ordinal()]);
        boolean done = "1".equals(parts[UnitStateField.DONE.ordinal()]);
        int maxRange = Integer.parseInt(parts[UnitStateField.MAX_RANGE.ordinal()]);
        int totalDamage = Integer.parseInt(parts[UnitStateField.TOTAL_DAMAGE.ordinal()]);

        Entity entity = null;

        if (!type.equals("MekWarrior") && !type.equals("EjectedCrew")) {
            if (entities != null) {
                entity = entities.computeIfAbsent(id, i -> MekSummary.loadEntity(chassis + " " + model));
            } else {
                entity = MekSummary.loadEntity(chassis + " " + model);
            }

            if (entity != null) {
                maxRange = entity.getMaxWeaponRange();
                totalDamage = Compute.computeTotalDamage(entity.getWeaponList());
                entity.setInitialBV(entity.calculateBattleValue(true, true));
                entity.setId(id);
            }
        }

        return new UnitState(
            id,
            phase,
            teamId,
            round,
            playerId,
            chassis,
            model,
            type,
            role,
            x,
            y,
            facing,
            mp,
            heat,
            prone,
            airborne,
            offBoard,
            crippled,
            destroyed,
            armorP,
            internalP,
            done,
            maxRange,
            totalDamage,
            entity);
    }

    @Override
    public String getHeaderLine() {
        return UnitStateField.getHeaderLine();
    }

    public enum UnitStateField {
        ENTITY_ID("ENTITY_ID"),
        PHASE("PHASE"),
        TEAM_ID("TEAM_ID"),
        ROUND("ROUND"),
        PLAYER_ID("PLAYER_ID"),
        CHASSIS("CHASSIS"),
        MODEL("MODEL"),
        TYPE("TYPE"),
        ROLE("ROLE"),
        X("X"),
        Y("Y"),
        FACING("FACING"),
        MP("MP"),
        HEAT("HEAT"),
        PRONE("PRONE"),
        AIRBORNE("AIRBORNE"),
        OFF_BOARD("OFF_BOARD"),
        CRIPPLED("CRIPPLED"),
        DESTROYED("DESTROYED"),
        ARMOR_P("ARMOR_P"),
        INTERNAL_P("INTERNAL_P"),
        DONE("DONE"),
        MAX_RANGE("MAX_RANGE"),
        TOTAL_DAMAGE("TOTAL_DAMAGE");

        private final String headerName;

        UnitStateField(String headerName) {
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
                .map(UnitStateField::getHeaderName)
                .collect(Collectors.joining("\t"));
        }
    }
}

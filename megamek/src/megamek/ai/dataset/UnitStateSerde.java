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
import java.util.Map;

/**
 * <h1>TSV Serializer Deserializer</h1>
 * <p>serializer and deserializer for UnitState to/from TSV format.</p>
 * @author Luana Coppio
 */
public class UnitStateSerde extends TsvSerde<UnitState> {

    private final DecimalFormat LOG_DECIMAL = new DecimalFormat("0.00", DecimalFormatSymbols.getInstance());

    @Override
    public String toTsv(UnitState obj) {
        String[] row = new String[UnitStateField.values().length];
        row[UnitStateField.ROUND.ordinal()] = String.valueOf(obj.round());
        row[UnitStateField.PHASE.ordinal()] = obj.phase().name();
        row[UnitStateField.PLAYER_ID.ordinal()] = String.valueOf(obj.playerId());
        row[UnitStateField.ENTITY_ID.ordinal()] = String.valueOf(obj.id());
        row[UnitStateField.CHASSIS.ordinal()] = obj.chassis();
        row[UnitStateField.MODEL.ordinal()] = obj.chassis();
        row[UnitStateField.TYPE.ordinal()] = obj.type();
        row[UnitStateField.ROLE.ordinal()] = obj.role() == null ? UnitRole.NONE.name() : obj.role().name();
        row[UnitStateField.X.ordinal()] = String.valueOf(obj.x());
        row[UnitStateField.Y.ordinal()] = String.valueOf(obj.y());
        row[UnitStateField.FACING.ordinal()] = String.valueOf(obj.facing());
        row[UnitStateField.MP.ordinal()] = LOG_DECIMAL.format(obj.mp());
        row[UnitStateField.HEAT.ordinal()] = LOG_DECIMAL.format(obj.heat());
        row[UnitStateField.PRONE.ordinal()] = obj.prone() ? "1" : "0";
        row[UnitStateField.AIRBORNE.ordinal()] = obj.airborne() ? "1" : "0";
        row[UnitStateField.OFF_BOARD.ordinal()] = obj.offBoard() ? "1" : "0";
        row[UnitStateField.CRIPPLED.ordinal()] = obj.crippled() ? "1" : "0";
        row[UnitStateField.DESTROYED.ordinal()] = obj.destroyed() ? "1" : "0";
        row[UnitStateField.ARMOR_P.ordinal()] = LOG_DECIMAL.format(obj.armorP());
        row[UnitStateField.INTERNAL_P.ordinal()] = LOG_DECIMAL.format(obj.internalP());
        row[UnitStateField.DONE.ordinal()] = obj.done() ? "1" : "0";
        row[UnitStateField.TEAM_ID.ordinal()] = String.valueOf(obj.teamId());
        row[UnitStateField.MAX_RANGE.ordinal()] = String.valueOf(obj.maxRange());
        row[UnitStateField.TOTAL_DAMAGE.ordinal()] = String.valueOf(obj.totalDamage());

        return String.join("\t", row);
    }

    public UnitState fromTsv(String line, Map<Integer, Entity> entities, int idOffset) throws NumberFormatException {
        String[] parts = line.split("\t", -1);

        int id = Integer.parseInt(parts[UnitStateField.ENTITY_ID.ordinal()]) + idOffset;
        GamePhase phase = GamePhase.valueOf(parts[UnitStateField.PHASE.ordinal()]);
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
        int maxRange = 0;
        int totalDamage = 0;
        int teamId = -1;
        if (parts.length == UnitStateField.values().length) {
            maxRange = Integer.parseInt(parts[UnitStateField.MAX_RANGE.ordinal()]);
            totalDamage = Integer.parseInt(parts[UnitStateField.TOTAL_DAMAGE.ordinal()]);
            teamId = Integer.parseInt(parts[UnitStateField.TEAM_ID.ordinal()]);
        }

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
}

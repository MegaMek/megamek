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

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.deser.std.StdDeserializer;
import megamek.common.BoardLocation;
import megamek.common.Coords;
import megamek.common.alphaStrike.BattleForceSUA;
import megamek.common.strategicBattleSystems.SBFFormation;
import megamek.common.strategicBattleSystems.SBFFormationConverter;
import megamek.common.strategicBattleSystems.SBFUnit;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import static megamek.common.jacksonadapters.MMUReader.*;
import static megamek.common.jacksonadapters.SBFFormationSerializer.UNITS;

/**
 * This Jackson deserializer reads an SBFFormation from an MMU file. As a formation must have its units
 * fully listed and the stats can then be converted, any given stats in the MMU file are (currently)
 * ignored.
 */
public class SBFFormationDeserializer extends StdDeserializer<SBFFormation> {

    private static final String ID = "id";
    private static final String AT = "at";
    private static final String X = "x";
    private static final String Y = "y";
    private static final String DEPLOYMENTROUND = "deploymentround";

    public SBFFormationDeserializer() {
        this(null);
    }

    public SBFFormationDeserializer(Class<?> vc) {
        super(vc);
    }

    @Override
    public SBFFormation deserialize(JsonParser jp, DeserializationContext ctxt) throws IOException {
        JsonNode node = jp.getCodec().readTree(jp);
        if (!node.has(TYPE) || !node.get(TYPE).textValue().equalsIgnoreCase(SBF_FORMATION)) {
            throw new IllegalArgumentException("SBFFormationDeserializer: Wrong Deserializer chosen!");
        }

        requireFields(SBF_FORMATION, node, GENERAL_NAME, UNITS);
        SBFFormation formation = new SBFFormation();
        formation.setName(node.get(GENERAL_NAME).textValue());
        new MMUReader().read(node.get(UNITS)).stream()
                .filter(o -> o instanceof SBFUnit)
                .map(o -> (SBFUnit) o)
                .forEach(formation::addUnit);
        SBFFormationConverter.calculateStatsFromUnits(formation);
        validateCOM(formation);
        validateLEAD(formation);

        if (node.has(ID)) {
            formation.setId(node.get(ID).intValue());
        }
        assignPosition(formation, node);
        assignDeploymentRound(formation, node);
        return formation;
    }

    private void assignPosition(SBFFormation formation, JsonNode node) {
        try {
            if (node.has(AT)) {
                List<Integer> xyList = new ArrayList<>();
                node.get(AT).elements().forEachRemaining(n -> xyList.add(n.asInt()));
                setDeployedPosition(formation, new Coords(xyList.get(0), xyList.get(1)));

            } else if (node.has(X) || node.has(Y)) {
                requireFields("SBFFormation", node, X, Y);
                setDeployedPosition(formation, new Coords(node.get(X).asInt(), node.get(Y).asInt()));
            }
        } catch (Exception e) {
            throw new IllegalArgumentException("Illegal position information for formation " + formation, e);
        }
    }

    private void setDeployedPosition(SBFFormation formation, Coords coords) {
        formation.setDeployed(true);
        // translate the position so "at: 2, 3" will place a unit on 0203 (instead of 0102)
        formation.setPosition(new BoardLocation(new Coords(coords.getX() - 1, coords.getY() - 1), 0));
    }

    private void assignDeploymentRound(SBFFormation formation, JsonNode node) {
        if (node.has(DEPLOYMENTROUND)) {
            formation.setDeployRound(node.get(DEPLOYMENTROUND).asInt());
        }
    }

    private void validateCOM(SBFFormation formation) {
        long comCount = formation.getUnits().stream().filter(u -> u.hasSUA(BattleForceSUA.COM)).count();
        if (comCount > 1) {
            throw new IllegalArgumentException("Only one Unit of a Formation may have COM");
        } else if (comCount == 1) {
            formation.getSpecialAbilities().setSUA(BattleForceSUA.COM);
        }
    }

    private void validateLEAD(SBFFormation formation) {
        long leadCount = formation.getUnits().stream().filter(u -> u.hasSUA(BattleForceSUA.LEAD)).count();
        if (leadCount > 1) {
            throw new IllegalArgumentException("Only one Unit of a Formation may have LEAD");
        } else if ((leadCount == 0) && (formation.getUnits().size() > 1)) {
            formation.getUnits().get(0).getSpecialAbilities().setSUA(BattleForceSUA.LEAD);
        }
    }
}
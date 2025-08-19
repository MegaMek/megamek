/*
 * Copyright (C) 2024-2025 The MegaMek Team. All Rights Reserved.
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

package megamek.common.jacksonAdapters;

import static megamek.common.jacksonAdapters.MMUReader.GENERAL_NAME;
import static megamek.common.jacksonAdapters.MMUReader.SBF_FORMATION;
import static megamek.common.jacksonAdapters.MMUReader.TYPE;
import static megamek.common.jacksonAdapters.MMUReader.requireFields;
import static megamek.common.jacksonAdapters.SBFFormationSerializer.UNITS;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.deser.std.StdDeserializer;
import megamek.common.alphaStrike.BattleForceSUA;
import megamek.common.board.BoardLocation;
import megamek.common.board.Coords;
import megamek.common.strategicBattleSystems.SBFFormation;
import megamek.common.strategicBattleSystems.SBFFormationConverter;
import megamek.common.strategicBattleSystems.SBFUnit;

/**
 * This Jackson deserializer reads an SBFFormation from an MMU file. As a formation must have its units fully listed and
 * the stats can then be converted, any given stats in the MMU file are (currently) ignored.
 */
public class SBFFormationDeserializer extends StdDeserializer<SBFFormation> {

    private static final String ID = "id";
    private static final String AT = "at";
    private static final String X = "x";
    private static final String Y = "y";
    private static final String DEPLOYMENT_ROUND = "deploymentround";

    public SBFFormationDeserializer() {
        this(null);
    }

    public SBFFormationDeserializer(Class<?> vc) {
        super(vc);
    }

    @Override
    public SBFFormation deserialize(JsonParser jp, DeserializationContext context) throws IOException {
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
        formation.setPosition(BoardLocation.of(new Coords(coords.getX() - 1, coords.getY() - 1), 0));
    }

    private void assignDeploymentRound(SBFFormation formation, JsonNode node) {
        if (node.has(DEPLOYMENT_ROUND)) {
            formation.setDeployRound(node.get(DEPLOYMENT_ROUND).asInt());
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

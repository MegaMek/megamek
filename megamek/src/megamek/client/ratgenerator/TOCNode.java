/*
 * Copyright (C) 2016-2025 The MegaMek Team. All Rights Reserved.
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
package megamek.client.ratgenerator;

import java.util.ArrayList;

import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

/**
 * Contains details about which units and echelon levels are available to this unit in given eras.
 *
 * @author Neoancient
 */
public class TOCNode extends RulesetNode {
    protected ArrayList<ValueNode> unitTypeNodes;
    protected ArrayList<ValueNode> echelonNodes;
    protected ArrayList<ValueNode> ratingNodes;
    protected ArrayList<ValueNode> flagNodes;

    protected TOCNode() {
        unitTypeNodes = new ArrayList<>();
        echelonNodes = new ArrayList<>();
        ratingNodes = new ArrayList<>();
        flagNodes = new ArrayList<>();
    }

    public ValueNode findUnitTypes(ForceDescriptor fd) {
        for (ValueNode n : unitTypeNodes) {
            if (n.matches(fd)) {
                return n;
            }
        }
        return null;
    }

    public ValueNode findEchelons(ForceDescriptor fd) {
        for (ValueNode n : echelonNodes) {
            if (n.matches(fd)) {
                return n;
            }
        }
        return null;
    }

    public ValueNode findRatings(ForceDescriptor fd) {
        for (ValueNode n : ratingNodes) {
            if (n.matches(fd)) {
                return n;
            }
        }
        return null;
    }

    public ValueNode findFlags(ForceDescriptor fd) {
        for (ValueNode n : flagNodes) {
            if (n.matches(fd)) {
                return n;
            }
        }
        return null;
    }

    public static TOCNode createFromXml(Node node) {
        TOCNode retVal = new TOCNode();
        retVal.loadFromXml(node);
        return retVal;
    }

    @Override
    protected void loadFromXml(Node node) {
        super.loadFromXml(node);

        NodeList nl = node.getChildNodes();
        for (int x = 0; x < nl.getLength(); x++) {
            Node wn = nl.item(x);

            switch (wn.getNodeName()) {
                case "unitType":
                    for (int y = 0; y < wn.getChildNodes().getLength(); y++) {
                        Node wn2 = wn.getChildNodes().item(y);
                        if (wn2.getNodeName().equals("option")) {
                            unitTypeNodes.add(ValueNode.createFromXml(wn2));
                        }
                    }
                    break;
                case "echelon":
                    for (int y = 0; y < wn.getChildNodes().getLength(); y++) {
                        Node wn2 = wn.getChildNodes().item(y);
                        if (wn2.getNodeName().equals("option")) {
                            echelonNodes.add(ValueNode.createFromXml(wn2));
                        }
                    }
                    break;
                case "rating":
                    for (int y = 0; y < wn.getChildNodes().getLength(); y++) {
                        Node wn2 = wn.getChildNodes().item(y);
                        if (wn2.getNodeName().equals("option")) {
                            ratingNodes.add(ValueNode.createFromXml(wn2));
                        }
                    }
                    break;
                case "flags":
                    for (int y = 0; y < wn.getChildNodes().getLength(); y++) {
                        Node wn2 = wn.getChildNodes().item(y);
                        if (wn2.getNodeName().equals("option")) {
                            flagNodes.add(ValueNode.createFromXml(wn2));
                        }
                    }
                    break;
            }
        }
    }
}

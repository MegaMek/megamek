/*
 * MegaMek - Copyright (C) 2016 The MegaMek Team
 *
 *  This program is free software; you can redistribute it and/or modify it
 *  under the terms of the GNU General Public License as published by the Free
 *  Software Foundation; either version 2 of the License, or (at your option)
 *  any later version.
 *
 *  This program is distributed in the hope that it will be useful, but
 *  WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 *  or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License
 *  for more details.
 */
package megamek.client.ratgenerator;

import java.util.ArrayList;

import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

/**
 * Contains details about which units and eschelon levels are available to this unit in given eras.
 * 
 * @author Neoancient
 *
 */
public class TOCNode extends RulesetNode {
    protected ArrayList<ValueNode> unitTypeNodes;
    protected ArrayList<ValueNode> eschelonNodes;
    protected ArrayList<ValueNode> ratingNodes;
    protected ArrayList<ValueNode> flagNodes;

    protected TOCNode() {
        unitTypeNodes = new ArrayList<>();
        eschelonNodes = new ArrayList<>();
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

    public ValueNode findEschelons(ForceDescriptor fd) {
        for (ValueNode n : eschelonNodes) {
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
                case "eschelon":
                    for (int y = 0; y < wn.getChildNodes().getLength(); y++) {
                        Node wn2 = wn.getChildNodes().item(y);
                        if (wn2.getNodeName().equals("option")) {
                            eschelonNodes.add(ValueNode.createFromXml(wn2));
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

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
 * The Defaults node is at the beginning of a ruleset file and provides the default values for
 * that faction.
 * 
 * @author Neoancient
 *
 */
public class DefaultsNode extends RulesetNode {
    protected ArrayList<ValueNode> unitTypeNodes;
    protected ArrayList<ValueNode> eschelonNodes;
    protected ArrayList<ValueNode> rankSystemNodes;
    protected ArrayList<ValueNode> ratingNodes;

    protected DefaultsNode() {
        unitTypeNodes = new ArrayList<>();
        eschelonNodes = new ArrayList<>();
        rankSystemNodes = new ArrayList<>();
        ratingNodes = new ArrayList<>();
    }

    public void apply(ForceDescriptor fd) {
        ValueNode ut = null;
        ValueNode e = null;
        ValueNode rs = null;
        ValueNode r = null;
        for (ValueNode n : unitTypeNodes) {
            if ((fd.getUnitType() == null || n.predicates.containsKey("ifUnitType"))
                    && n.matches(fd)) {
                ut = n;
                break;
            }
        }
        for (ValueNode n : eschelonNodes) {
            if ((fd.getEschelon() == null || n.predicates.containsKey("ifEschelon"))
                    && n.matches(fd)) {
                e = n;
                break;
            }
        }
        for (ValueNode n : rankSystemNodes) {
            if ((fd.getRankSystem() == null || n.predicates.containsKey("ifRankSystem"))
                    && n.matches(fd)) {
                rs = n;
                break;
            }
        }
        for (ValueNode n : ratingNodes) {
            if ((fd.getRating() == null || n.predicates.containsKey("ifRating"))
                    && n.matches(fd)) {
                r = n;
                break;
            }
        }
        if (ut != null) {
            fd.setUnitType(ModelRecord.parseUnitType(ut.getContent()));
        }
        if (e != null) {
            fd.setEschelon(Integer.parseInt(e.getContent()));
        }
        if (rs != null) {
            fd.setRankSystem(Integer.parseInt(rs.getContent()));
        }
        if (r != null) {
            fd.setRating(r.getContent());
        }
    }

    public String getUnitType(ForceDescriptor fd) {
        for (ValueNode n : unitTypeNodes) {
            if (n.matches(fd)) {
                return n.getContent();
            }
        }
        return null;
    }

    public String getEschelon(ForceDescriptor fd) {
        for (ValueNode n : eschelonNodes) {
            if (n.matches(fd)) {
                return n.getContent();
            }
        }
        return null;
    }

    public String getRating(ForceDescriptor fd) {
        for (ValueNode n : ratingNodes) {
            if (n.matches(fd)) {
                return n.getContent();
            }
        }
        return null;
    }

    public static DefaultsNode createFromXml(Node node) {
        DefaultsNode retVal = new DefaultsNode();
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
                    unitTypeNodes.add(ValueNode.createFromXml(wn));
                    break;
                case "eschelon":
                    eschelonNodes.add(ValueNode.createFromXml(wn));
                    break;
                case "rankSystem":
                    rankSystemNodes.add(ValueNode.createFromXml(wn));
                    break;
                case "rating":
                    ratingNodes.add(ValueNode.createFromXml(wn));
                    break;
            }
        }
    }
}

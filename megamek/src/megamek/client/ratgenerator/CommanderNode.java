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

import org.w3c.dom.Node;

/**
 * A commander node gives the rank to assign the force's commander, an optional title, and the
 * position within the force structure (typically in the first unit/subforce for a field officer).
 * 
 * @author Neoancient
 *
 */
public class CommanderNode extends RulesetNode {
    protected String title;
    protected Integer position;
    protected int rank;
    protected String unitType;

    protected CommanderNode() {
        title = null;
        position = null;
        rank = 0;
        unitType = null;
    }

    public String getTitle() {
        return title;
    }

    public Integer getPosition() {
        return position;
    }

    public Integer getRank() {
        return rank;
    }

    public String getUnitType() {
        return unitType;
    }

    public static CommanderNode createFromXml(Node node) {
        CommanderNode retVal = new CommanderNode();
        retVal.loadFromXml(node);
        return retVal;
    }

    @Override
    protected void loadFromXml(Node node) {
        super.loadFromXml(node);
        if (assertions.containsKey("title")) {
            title = assertions.getProperty("title");
            assertions.remove("title");
        }
        if (assertions.containsKey("position")) {
            position = Integer.parseInt(assertions.getProperty("position"));
            assertions.remove("position");
        }
        if (assertions.containsKey("unitType")) {
            unitType = assertions.getProperty("unitType");
            assertions.remove("unitType");
        }
        rank = Integer.parseInt(Ruleset.substituteConstants(node.getTextContent().trim()));
    }
}

/*
 * Copyright (c) 2016-2022 - The MegaMek Team. All Rights Reserved.
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
 */
package megamek.client.ratgenerator;

import java.util.ArrayList;

import megamek.common.Compute;

import megamek.common.annotations.Nullable;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

/**
 * An OptionGroupNode is an adapter that allows two or more alternative ValueNodes to be
 * used when one is expected, and provides information on how to make the selection.
 * 
 * @author Neoancient
 */
public class OptionGroupNode extends RulesetNode {
    protected ArrayList<ValueNode> options; 

    protected OptionGroupNode() {
        options = new ArrayList<>();
    }

    public @Nullable ValueNode selectOption(ForceDescriptor fd) {
        return selectOption(fd, false);
    }

    public @Nullable ValueNode selectOption(ForceDescriptor fd, boolean apply) {
        ArrayList<ValueNode> list = new ArrayList<>();
        for (ValueNode o : options) {
            if (o.matches(fd)) {
                for (int i = 0; i < o.getWeight(); i++) {
                    list.add(o);
                }
            }
        }

        if (!list.isEmpty()) {
            ValueNode n = list.get(Compute.randomInt(list.size()));
            if (apply) {
                n.apply(fd);
            }
            if (n.getContent() == null) {
                return null;
            }
            return n;
        }
        return null;
    }

    public static OptionGroupNode createFromXml(Node node) {
        OptionGroupNode retVal = new OptionGroupNode();
        retVal.loadFromXml(node);
        return retVal;
    }

    @Override
    protected void loadFromXml(Node node) {
        super.loadFromXml(node);
        NodeList nl = node.getChildNodes();
        for (int x = 0; x < nl.getLength(); x++) {
            Node wn = nl.item(x);
            if (wn.getNodeName().equals("option")) {
                options.add(ValueNode.createFromXml(wn));
            }
        }
    }
}

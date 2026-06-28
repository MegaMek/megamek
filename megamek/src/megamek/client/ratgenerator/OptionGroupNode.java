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

import megamek.common.annotations.Nullable;
import megamek.common.compute.Compute;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

/**
 * An OptionGroupNode is an adapter that allows two or more alternative ValueNodes to be used when one is expected, and
 * provides information on how to make the selection.
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
        ArrayList<ValueNode> matching = new ArrayList<>();
        for (ValueNode option : options) {
            if (option.matches(fd)) {
                matching.add(option);
            }
        }
        if (matching.isEmpty()) {
            return null;
        }

        ArrayList<ValueNode> weightedOptions = new ArrayList<>();
        for (ValueNode option : matching) {
            for (int i = 0; i < option.getWeight(); i++) {
                weightedOptions.add(option);
            }
        }
        if (weightedOptions.isEmpty()) {
            return null;
        }
        ValueNode selected = weightedOptions.get(Compute.randomInt(weightedOptions.size()));

        if (apply) {
            selected.apply(fd);
        }
        if (selected.getContent() == null) {
            return null;
        }
        return selected;
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

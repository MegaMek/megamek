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

import org.w3c.dom.Node;

/**
 * Generic node which contains a value, a proportional weight to be used in random selection, and the number of times to
 * be applied.
 *
 * @author Neoancient
 */
public class ValueNode extends RulesetNode {
    protected Integer weight;
    protected Integer num;
    protected String content;

    protected ValueNode() {
        weight = 1;
        num = 1;
        content = null;
    }

    public void apply(ForceDescriptor fd) {
        apply(fd, 0);
    }

    public Integer getWeight() {
        return weight;
    }

    public void setWeight(Integer weight) {
        this.weight = weight;
    }

    public Integer getNum() {
        return num;
    }

    public void setNum(Integer num) {
        this.num = num;
    }

    public String getContent() {
        return content;
    }

    public static ValueNode createFromXml(Node node) {
        ValueNode retVal = new ValueNode();
        retVal.loadFromXml(node);
        return retVal;
    }

    @Override
    protected void loadFromXml(Node node) {
        super.loadFromXml(node);
        if (assertions.containsKey("weight")) {
            weight = Integer.valueOf(assertions.getProperty("weight"));
            assertions.remove("weight");
        }
        if (assertions.containsKey("weightClass") && !assertions.getProperty("weightClass").isEmpty()) {
            num = assertions.getProperty("weightClass").split(",").length;
            if (assertions.containsKey("num")) {
                if (Integer.valueOf(assertions.getProperty("num")).equals(num)) {
                    assertions.remove("num");
                } else {
                    throw new IllegalArgumentException("Value of attribute num='"
                          + assertions.getProperty("num")
                          + "' conflicts with value of attribute weightClass='"
                          + assertions.getProperty("weightClass"));
                }
            }
        } else if (assertions.containsKey("num")) {
            num = Integer.valueOf(assertions.getProperty("num"));
            assertions.remove("num");
        }
        content = node.getTextContent().isBlank()
              ? null : Ruleset.substituteConstants(node.getTextContent().trim());
    }
}

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

import org.w3c.dom.Node;

/**
 * Generic node which contains a value, a proportional weight to be used in random selection,
 * and the number of times to be applied.
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

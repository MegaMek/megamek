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
 * Node while contains rules for generating subforces -- either lower eschelons or the actual
 * units if this is a bottom-level eschelon.
 * 
 * @author Neoancient
 *
 */
public class SubforcesNode extends RulesetNode {
    String altFaction;
    boolean parentFaction;
    ArrayList<ValueNode> subforces;
    ArrayList<OptionGroupNode> optionSubforces;

    protected SubforcesNode() {
        altFaction = null;
        parentFaction = false;
        subforces = new ArrayList<>();
        optionSubforces = new ArrayList<>();
    }

    public ArrayList<ForceDescriptor> generateSubforces(ForceDescriptor fd) {
        return generateSubforces(fd, false);
    }

    public ArrayList<ForceDescriptor> generateSubforces(ForceDescriptor fd,
            boolean isAttached) {
        ArrayList<ForceDescriptor> retVal = new ArrayList<>();
        for (ValueNode n : subforces) {
            if (n.matches(fd)) {
                ArrayList<ForceDescriptor> subs = new ArrayList<>();
                for (int i = 0; i < n.getNum(); i++) {
                    /* Remove the middle weight class to keep the overall weight class
                     * roughly the same.
                     */
                    if (!isAttached && fd.getSizeMod() == ForceDescriptor.UNDERSTRENGTH
                            && i == n.getNum() / 2) {
                        continue;
                    }
                    ForceDescriptor sub = fd.createChild(i);
                    sub.setEschelon(Integer.parseInt(n.getContent()));
                    apply(sub, i);
                    n.apply(sub, i);
                    subs.add(sub);
                }
                if (!isAttached && fd.getSizeMod() == ForceDescriptor.REINFORCED) {
                    ForceDescriptor sub = fd.createChild(subs.size());
                    sub.setEschelon(Integer.parseInt(n.getContent()));
                    apply(sub, n.getNum() / 2);
                    n.apply(sub, n.getNum() / 2);
                    subs.add(sub);

                }
                retVal.addAll(subs);
                if (!isAttached && null == fd.getGenerationRule()) {
                    fd.setGenerationRule(findGenerateProperty(n, this));
                }
            }
        }
        for (OptionGroupNode n : optionSubforces) {
            if (n.matches(fd)) {
                ValueNode vn = n.selectOption(fd);
                if (vn != null) {
                    ArrayList<ForceDescriptor> subs = new ArrayList<>();
                    for (int i = 0; i < vn.getNum(); i++) {
                        if (fd.getSizeMod() == ForceDescriptor.UNDERSTRENGTH
                                && i == vn.getNum() / 2) {
                            continue;
                        }
                        ForceDescriptor sub = fd.createChild(i);
                        if (vn.getContent().endsWith("+")) {
                            sub.setSizeMod(ForceDescriptor.REINFORCED);
                            sub.setEschelon(Integer.parseInt(vn.getContent().replace("+", "")));
                        } else if (vn.getContent().endsWith("-")) {
                            sub.setSizeMod(ForceDescriptor.UNDERSTRENGTH);
                            sub.setEschelon(Integer.parseInt(vn.getContent().replace("-", "")));
                        } else  {
                            sub.setEschelon(Integer.parseInt(vn.getContent()));
                        }
                        apply(sub, i);
                        n.apply(sub, i);
                        vn.apply(sub, i);
                        //						if (sub.getEschelon() == 0) {
                        //							sub.generate(false);
                        //						}
                        subs.add(sub);
                    }
                    if (fd.getSizeMod() == ForceDescriptor.REINFORCED) {
                        ForceDescriptor sub = fd.createChild(subs.size());
                        sub.setEschelon(Integer.parseInt(vn.getContent()));
                        apply(sub, vn.getNum() / 2);
                        n.apply(sub, vn.getNum() / 2);
                        subs.add(sub);

                    }
                    retVal.addAll(subs);
                    if (!isAttached && null == fd.getGenerationRule()) {
                        fd.setGenerationRule(findGenerateProperty(vn, n, this));
                    }
                }
            }
        }
        return retVal;
    }

    /**
     * Used to check the element hierarchy looking for a node with a "generate" assertion set.
     * 
     * @param nodes A series of ruleset nodes, sorted from the innermost to the outermost.
     * @return The value of the innermost "generate" property that is set, or null if none are set. 
     */
    private String findGenerateProperty(RulesetNode... nodes) {
        for (RulesetNode n : nodes) {
            final String prop = n.assertions.getProperty("generate");
            if (null != prop) {
                return prop;
            }
        }
        return null;
    }

    public String getAltFaction() {
        return altFaction;
    }

    public boolean useParentFaction() {
        return parentFaction;
    }

    public static SubforcesNode createFromXml(Node node) {
        SubforcesNode retVal = new SubforcesNode();
        retVal.loadFromXml(node);
        return retVal;
    }

    @Override
    protected void loadFromXml(Node node) {
        super.loadFromXml(node);

        NodeList nl = node.getChildNodes();
        for (int x = 0; x < nl.getLength(); x++) {
            Node wn = nl.item(x);

            if (wn.getNodeName().equals("subforce")) {
                subforces.add(ValueNode.createFromXml(wn));
            } else if (wn.getNodeName().equals("subforceOption")) {
                optionSubforces.add(OptionGroupNode.createFromXml(wn));
            } else if (wn.getNodeName().equals("asFaction")) {
                altFaction = wn.getTextContent().trim();
            } else if (wn.getNodeName().equals("asParent")) {
                parentFaction = true;
            }
        }
    }
}

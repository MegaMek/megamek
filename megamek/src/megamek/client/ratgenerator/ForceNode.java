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
import java.util.stream.Collectors;

import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import megamek.MegaMek;
import megamek.common.EntityMovementMode;

/**
 * 
 * A force node contains the rules for generating a force when the ForceDescriptor matches the
 * characteristics defined by the force node.
 *
 * @author Neoancient
 *
 */
public class ForceNode extends RulesetNode {
    protected Integer eschelon;
    protected String eschelonName;
    protected ArrayList<ValueNode> nameNodes;
    protected ArrayList<CommanderNode> coNodes;
    protected ArrayList<CommanderNode> xoNodes;
    protected ArrayList<ArrayList <OptionGroupNode>> ruleGroups;
    protected ArrayList <SubforcesNode> subforces;
    protected ArrayList <SubforcesNode> attached;

    protected String desc;

    private ForceNode() {
        super();
        eschelon = null;
        eschelonName = null;
        nameNodes = new ArrayList<ValueNode>();
        coNodes = new ArrayList<CommanderNode>();
        xoNodes = new ArrayList<CommanderNode>();
        ruleGroups = new ArrayList<ArrayList<OptionGroupNode>>();
        subforces = new ArrayList<SubforcesNode>();
        attached = new ArrayList<SubforcesNode>();
    }

    public boolean apply(ForceDescriptor fd) {
        for (ArrayList<OptionGroupNode> group : ruleGroups) {
            ArrayList<OptionGroupNode> toApply = new ArrayList<OptionGroupNode>();
            for (OptionGroupNode rule : group) {
                if (rule.matches(fd)) {
                    toApply.add(rule);
                }
            }
            for (OptionGroupNode rule : toApply) {
                switch (rule.getName()) {
                    case "weightClass":
                        if (fd.getWeightClass() == null
                        || rule.predicates.containsKey("ifWeightClass")) {
                            ValueNode n = rule.selectOption(fd, true);
                            if (n != null) {
                                fd.setWeightClass(ForceDescriptor.decodeWeightClass(n.getContent()));
                            }
                        }
                        break;
                    case "unitType":
                        if (fd.getUnitType() == null
                        || rule.predicates.containsKey("ifUnitType")) {
                            ValueNode n = rule.selectOption(fd, true);
                            if (n != null) {
                                fd.setUnitType(ModelRecord.parseUnitType(n.getContent()));
                            }
                        }
                        break;
                    case "chassis":
                        if (fd.getChassis().size() == 0
                        || rule.predicates.containsKey("ifChassis")) {
                            ValueNode n = rule.selectOption(fd, true);
                            if (n != null) {
                                for (String c : n.getContent().split(",")) {
                                    fd.getChassis().add(c);
                                }
                            }
                        }
                        break;
                    case "variant":
                        if (fd.getVariants().size() == 0
                        || rule.predicates.containsKey("ifVariant")) {
                            ValueNode n = rule.selectOption(fd, true);
                            if (n != null) {
                                for (String c : n.getContent().split(",")) {
                                    fd.getVariants().add(c);
                                }
                            }
                        }
                        break;
                    case "motive":
                        ValueNode n = rule.selectOption(fd, true);
                        if (n == null) {
                            break;
                        }
                        String content = n.getContent();
                        if (content.startsWith("-")) {
                            for (String p : content.replaceFirst("\\-", "").split(",")) {
                                fd.getMovementModes().remove(EntityMovementMode.parseFromString(p));
                            }
                            break;
                        }
                        if (content.startsWith("+")) {
                            content = content.replace("+", "");
                        } else {
                            fd.getMovementModes().clear();
                        }
                        for (String p : content.split(",")) {
                            fd.getMovementModes().add(EntityMovementMode.parseFromString(p));
                        }
                        break;
                    case "formation":
                        if (null == fd.getFormation()
                        || rule.predicates.containsKey("ifFormation")) {
                            n = rule.selectOption(fd, true);
                            if (n == null) {
                                break;
                            }
                            content = n.getContent();
                            if (content != null) {
                                FormationType ft = FormationType.getFormationType(content);
                                if (null == ft) {
                                    MegaMek.getLogger().error("Could not parse formation type " + content);
                                }
                                fd.setFormationType(ft);
                            }
                        }
                        break;
                    case "role":
                        n = rule.selectOption(fd, true);
                        if (n == null) {
                            break;
                        }
                        content = n.getContent();
                        if (content == null) {
                            break;
                        }
                        if (content.startsWith("-")) {
                            for (String p : content.replaceFirst("\\-", "").split(",")) {
                                fd.getRoles().remove(MissionRole.parseRole(p));
                            }
                            break;
                        }
                        if (content.startsWith("+")) {
                            content = content.replace("+", "");
                        } else {
                            fd.getRoles().clear();
                        }
                        for (String p : content.split(",")) {
                            MissionRole role = MissionRole.parseRole(p);
                            if (role != null) {
                                fd.getRoles().add(role);
                            } else {
                                MegaMek.getLogger().error("Force generator could not parse mission role " + p);
                            }
                        }
                        break;
                    case "flags":
                        n = rule.selectOption(fd, true);
                        if (n == null) {
                            break;
                        }
                        content = n.getContent();
                        if (content == null) {
                            break;
                        }
                        if (content.startsWith("-")) {
                            for (String p : content.replaceFirst("\\-", "").split(",")) {
                                fd.getFlags().remove(p);
                            }
                            break;
                        }
                        if (content.startsWith("+")) {
                            content = content.replace("+", "");
                        } else {
                            fd.getFlags().clear();
                        }
                        for (String p : content.split(",")) {
                            fd.getFlags().add(p);
                        }
                        break;
                    case "changeEschelon":
                        n = rule.selectOption(fd, true);
                        if (n == null) {
                            break;
                        }
                        content = n.getContent();
                        if (content == null) {
                            break;
                        }
                        if (content.endsWith("+")) {
                            fd.setSizeMod(ForceDescriptor.REINFORCED);
                        } else if (content.endsWith("-")) {
                            fd.setSizeMod(ForceDescriptor.UNDERSTRENGTH);
                        } 
                        fd.setEschelon(Integer.parseInt(n.getContent().replaceAll("[\\+\\-]", "")));
                        return false;
                }
            }
        }

        if (fd.getName() == null) {
            fd.setNameNodes(nameNodes);
        }

        String generate = assertions.getProperty("generate");
        if (subforces.size() == 0) {
            generate = "model";
        }

        processSubforces(fd, generate);

        if (fd.shouldGenerateAttachments()) {
            for (SubforcesNode n : attached) {
                if (n.matches(fd)) {
                    ArrayList<ForceDescriptor> subs = n.generateSubforces(fd, true);
                    if (subs != null) {
                        for (ForceDescriptor sub : subs) {
                            fd.addAttached(sub);
                        }
                    }
                }
            }
        }
        return true;
    }

    public void processSubforces(ForceDescriptor fd, String generate) {
        processSubforces(fd, generate, Ruleset.findRuleset(fd.getFaction()));
    }

    public void processSubforces(ForceDescriptor fd, String generate, Ruleset ruleset) {
        for (SubforcesNode n : subforces) {
            if (n.matches(fd)) {
                ArrayList<ForceDescriptor> subs = null;
                if (n.getAltFaction() != null || n.useParentFaction()) {
                    String faction = n.getAltFaction();
                    if (n.useParentFaction()) {
                        faction = ruleset.getParent();
                    }
                    if (faction != null) {
                        Ruleset rs = null;
                        ForceNode fn = null;
                        do {
                            rs = Ruleset.findRuleset(faction);
                            if (rs != null) {
                                fn = rs.findForceNode(fd);
                                if (fn == null) {
                                    faction = rs.getParent();
                                } else {
                                    fn.processSubforces(fd, generate, rs);
                                }
                            }
                        } while (rs != null && fn == null);
                    }
                } else {
                    subs = n.generateSubforces(fd, false);
                }
                if (subs != null) {
                    for (ForceDescriptor sub : subs) {
                        fd.addSubforce(sub);
                    }
                }
            }
        }
    }

    public Integer getEschelon() {
        return eschelon;
    }

    public String getEschelonCode() {
        String retVal = eschelon.toString();
        if (predicates.containsKey("ifAugmented")
                && predicates.getProperty("ifAugmented").equals("1")) {
            retVal += "*";
        }
        return retVal;
    }

    public ArrayList<CommanderNode> getCoNodes() {
        return coNodes;
    }

    public ArrayList<CommanderNode> getXoNodes() {
        return xoNodes;
    }

    public Integer getCoRank(ForceDescriptor fd) {
        for (CommanderNode n : coNodes) {
            if (n.matches(fd)) {
                return n.getRank();
            }
        }
        return null;
    }

    public void setEschelon(Integer eschelon) {
        this.eschelon = eschelon;
    }

    public String getEschelonName() {
        return eschelonName;
    }

    public static ForceNode createFromXml(Node node) {
        ForceNode retVal = new ForceNode();
        retVal.loadFromXml(node);
        return retVal;
    }

    @Override
    protected void loadFromXml(Node node) throws IllegalArgumentException {
        super.loadFromXml(node);

        try {
            eschelon = Integer.parseInt(assertions.getProperty("eschelon"));
            assertions.remove("eschelon");
        } catch (NullPointerException ex) {
            throw new IllegalArgumentException("Force Generator: force node is missing eschelon attribute.");
        } catch (NumberFormatException ex) {
            throw new IllegalArgumentException("Force Generator: force node is missing eschelon attribute.");
        }

        if (assertions.containsKey("eschName")) {
            eschelonName = assertions.getProperty("eschName");
            assertions.remove("eschName");
        }
        ArrayList<OptionGroupNode> currentRuleGroup = null;
        NodeList nl = node.getChildNodes();
        for (int x = 0; x < nl.getLength(); x++) {
            Node wn = nl.item(x);

            switch (wn.getNodeName()) {
                case "name":
                    nameNodes.add(ValueNode.createFromXml(wn));
                    break;
                case "co":
                    coNodes.add(CommanderNode.createFromXml(wn));
                    break;
                case "xo":
                    xoNodes.add(CommanderNode.createFromXml(wn));
                    break;
                case "weightClass":
                case "unitType":
                case "chassis":
                case "variant":
                case "motive":
                case "formation":
                case "role":
                case "flags":
                case "changeEschelon":
                    if (currentRuleGroup == null) {
                        currentRuleGroup = new ArrayList<OptionGroupNode>();
                        ruleGroups.add(currentRuleGroup);
                    }
                    ruleGroups.get(0).add(OptionGroupNode.createFromXml(wn));
                    break;
                case "ruleGroup":
                    currentRuleGroup = new ArrayList<OptionGroupNode>();
                    ruleGroups.add(currentRuleGroup);
                    for (int y = 0; y < wn.getChildNodes().getLength(); y++) {
                        Node wn2 = wn.getChildNodes().item(y);
                        if (wn2.getNodeName().equals("weightClass")
                                || wn2.getNodeName().equals("unitType")
                                || wn2.getNodeName().equals("chassis")
                                || wn2.getNodeName().equals("variant")
                                || wn2.getNodeName().equals("motive")
                                || wn2.getNodeName().equals("formation")
                                || wn2.getNodeName().equals("role")
                                || wn2.getNodeName().equals("flags")
                                || wn2.getNodeName().equals("changeEschelon")) {
                            currentRuleGroup.add(OptionGroupNode.createFromXml(wn2));
                        }
                    }
                    break;
                case "subforces":
                    subforces.add(SubforcesNode.createFromXml(wn));
                    break;
                case "attachedForces":
                    attached.add(SubforcesNode.createFromXml(wn));
                    break;
            }
        }
    }

    /**
     * Used for debugging output
     * 
     * @return A description of the node
     */
    public String show() {
        if (null == desc) {
            StringBuilder sb = new StringBuilder();
            sb.append("Force Node [eschelon:").append(eschelon).append(" predicates:");
            sb.append(predicates.entrySet().stream().map(e -> e.getKey() + "=" + e.getValue())
                    .collect(Collectors.joining(",")));
            sb.append(" assertions:");
            sb.append(assertions.entrySet().stream().map(e -> e.getKey() + "=" + e.getValue())
                    .collect(Collectors.joining(",")));
            sb.append("]");
            desc = sb.toString();
        }
        return desc;
    }
}

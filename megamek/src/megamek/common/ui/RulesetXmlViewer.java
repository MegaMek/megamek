/*
 * Copyright (C) 2026 The MegaMek Team. All Rights Reserved.
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

package megamek.common.ui;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.io.File;
import java.io.IOException;
import java.io.StringWriter;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.function.UnaryOperator;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.JTree;
import javax.swing.SwingUtilities;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeModel;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import megamek.common.Configuration;
import megamek.common.annotations.Nullable;
import megamek.logging.MMLogger;
import megamek.utilities.xml.MMXMLUtility;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

/**
 * Read-only developer tool that browses the force generator faction ruleset XML files
 * ({@code data/forcegenerator/faction_rules/}). The left pane is a tree of every file and the full
 * element hierarchy inside it; the right pane <em>interprets</em> the selected element the way the
 * ratgen engine does — what the node is for, the conditions under which it applies, what it changes
 * on the force being generated, and (for random tables) the roll odds — rather than just echoing the
 * raw XML.
 *
 * <p>The interpretation mirrors {@code RulesetNode} (predicate vs. assertion attribute handling),
 * {@code ForceNode}, {@code SubForcesNode} and {@code ValueNode}. The raw XML is still shown, at the
 * bottom, as a cross-reference.</p>
 *
 * <p>Launched from the command line via the {@code -view_rulesets} flag, mirroring how
 * {@link RATGeneratorEditor} is opened with {@code -edit_rat_gen}.</p>
 */
public class RulesetXmlViewer extends JFrame {
    private static final MMLogger logger = MMLogger.create(RulesetXmlViewer.class);

    private static final String RULES_SUBDIRECTORY = "forcegenerator/faction_rules";

    private final JTree tree = new JTree();
    private final JTextArea detailArea = new JTextArea();
    private final JTextField filterField = new JTextField(16);

    private final File rulesDirectory;
    private final List<File> rulesetFiles = new ArrayList<>();
    private final Map<File, Document> documentCache = new HashMap<>();

    /** Maps faction / command keys (DC, DC.SL, ...) to their display names, loaded from universe data. */
    private static final Map<String, String> factionNames = new HashMap<>();

    public RulesetXmlViewer() {
        this(new File(Configuration.dataDir(), RULES_SUBDIRECTORY));
    }

    public RulesetXmlViewer(File directory) {
        this.rulesDirectory = directory;
        initUI();
        loadFactionNames();
        loadFiles();
        rebuildTree(filterField.getText());
    }

    private void initUI() {
        setTitle("Force Generator Ruleset Viewer");
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setSize(new Dimension(1150, 760));
        setLocationRelativeTo(null);
        setLayout(new BorderLayout());

        JPanel topPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        topPanel.add(new JLabel("Filter files:"));
        topPanel.add(filterField);
        JButton reloadButton = new JButton("Reload");
        reloadButton.setToolTipText("Re-read the ruleset directory from disk");
        reloadButton.addActionListener(event -> {
            documentCache.clear();
            loadFactionNames();
            loadFiles();
            rebuildTree(filterField.getText());
        });
        topPanel.add(reloadButton);
        add(topPanel, BorderLayout.NORTH);

        filterField.getDocument().addDocumentListener(new DocumentListener() {
            @Override
            public void insertUpdate(DocumentEvent event) {
                rebuildTree(filterField.getText());
            }

            @Override
            public void removeUpdate(DocumentEvent event) {
                rebuildTree(filterField.getText());
            }

            @Override
            public void changedUpdate(DocumentEvent event) {
                rebuildTree(filterField.getText());
            }
        });

        tree.setRootVisible(false);
        tree.setShowsRootHandles(true);
        tree.addTreeSelectionListener(event -> showDetail());

        detailArea.setEditable(false);
        detailArea.setLineWrap(true);
        detailArea.setWrapStyleWord(true);
        detailArea.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 12));

        JSplitPane splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT,
              new JScrollPane(tree), new JScrollPane(detailArea));
        splitPane.setDividerLocation(400);
        add(splitPane, BorderLayout.CENTER);
    }

    /**
     * Scans the ruleset directory for XML files and parses each one into the document cache. Files that
     * fail to parse are kept in the list so the tree can surface the error rather than hiding the file.
     */
    private void loadFiles() {
        rulesetFiles.clear();
        if (!rulesDirectory.isDirectory()) {
            logger.warn("Ruleset directory not found: {}", rulesDirectory.getAbsolutePath());
            return;
        }
        File[] found = rulesDirectory.listFiles(
              (directory, name) -> name.toLowerCase(Locale.ROOT).endsWith(".xml"));
        if (found == null) {
            return;
        }
        rulesetFiles.addAll(Arrays.asList(found));
        rulesetFiles.sort(Comparator.comparing(File::getName));
        for (File file : rulesetFiles) {
            if (!documentCache.containsKey(file)) {
                try {
                    DocumentBuilder builder = MMXMLUtility.newSafeDocumentBuilder();
                    Document document = builder.parse(file);
                    stripWhitespace(document.getDocumentElement());
                    documentCache.put(file, document);
                } catch (Exception ex) {
                    logger.error(ex, "Failed to parse ruleset file {}", file.getName());
                    documentCache.put(file, null);
                }
            }
        }
    }

    /**
     * Loads faction and command display names from the universe data so faction keys in the rulesets
     * (DC, DC.SL, ...) can be shown with their names. Major factions live in universe/factions; the
     * sub-commands (Sword of Light, Arkab Legions, ...) live in universe/commands.
     */
    private static void loadFactionNames() {
        factionNames.clear();
        scanFactionDirectory(new File(Configuration.dataDir(), "universe/factions"));
        scanFactionDirectory(new File(Configuration.dataDir(), "universe/commands"));
    }

    /**
     * Scans one universe directory of YAML faction files, extracting each file's {@code key} and
     * {@code name} into {@link #factionNames}.
     */
    private static void scanFactionDirectory(File directory) {
        if (!directory.isDirectory()) {
            logger.warn("Faction name directory not found: {}", directory.getAbsolutePath());
            return;
        }
        File[] files = directory.listFiles(
              (dir, name) -> name.toLowerCase(Locale.ROOT).endsWith(".yml"));
        if (files == null) {
            return;
        }
        for (File file : files) {
            try {
                String key = null;
                String name = null;
                for (String line : Files.readAllLines(file.toPath())) {
                    String trimmed = line.strip();
                    if (trimmed.startsWith("#")) {
                        continue;
                    }
                    if ((key == null) && trimmed.startsWith("key:")) {
                        key = trimmed.substring("key:".length()).strip();
                    } else if ((name == null) && trimmed.startsWith("name:")) {
                        name = trimmed.substring("name:".length()).strip();
                    }
                    if ((key != null) && (name != null)) {
                        break;
                    }
                }
                if ((key != null) && !key.isEmpty() && (name != null) && !name.isEmpty()) {
                    factionNames.putIfAbsent(key, name);
                }
            } catch (IOException ex) {
                logger.warn("Could not read faction file {}", file.getName());
            }
        }
    }

    /**
     * Appends a faction / command key's display name in parentheses, if one is known.
     */
    private static String decodeFaction(String key) {
        String trimmed = key.trim();
        String name = factionNames.get(trimmed);
        return (name == null) ? trimmed : trimmed + " (" + name + ')';
    }

    /**
     * Rebuilds the file/element tree, including only files whose name contains the (case-insensitive)
     * filter text.
     *
     * @param filter the filter text; an empty or blank value includes every file
     */
    private void rebuildTree(@Nullable String filter) {
        String normalizedFilter = (filter == null) ? "" : filter.trim().toLowerCase(Locale.ROOT);
        DefaultMutableTreeNode root = new DefaultMutableTreeNode("rulesets");
        for (File file : rulesetFiles) {
            if (!normalizedFilter.isEmpty()
                  && !file.getName().toLowerCase(Locale.ROOT).contains(normalizedFilter)) {
                continue;
            }
            Document document = documentCache.get(file);
            Node documentElement = (document == null) ? null : document.getDocumentElement();
            DefaultMutableTreeNode fileNode = new DefaultMutableTreeNode(
                  new XmlEntry(documentElement, file.getName(), file));
            if (document == null) {
                fileNode.add(new DefaultMutableTreeNode(
                      new XmlEntry(null, "[parse error - see log]", file)));
            } else {
                appendChildElements(document.getDocumentElement(), fileNode, file);
            }
            root.add(fileNode);
        }
        tree.setModel(new DefaultTreeModel(root));
    }

    /**
     * Recursively adds a tree node for every child element of {@code element}.
     */
    private void appendChildElements(Element element, DefaultMutableTreeNode parentNode, File file) {
        NodeList children = element.getChildNodes();
        for (int i = 0; i < children.getLength(); i++) {
            Node child = children.item(i);
            if (child.getNodeType() != Node.ELEMENT_NODE) {
                continue;
            }
            Element childElement = (Element) child;
            DefaultMutableTreeNode childNode = new DefaultMutableTreeNode(
                  new XmlEntry(childElement, treeLabelFor(childElement), file));
            parentNode.add(childNode);
            appendChildElements(childElement, childNode, file);
        }
    }

    // ------------------------------------------------------------------
    // Tree labels
    // ------------------------------------------------------------------

    /**
     * Builds a readable tree label: a {@code <force>} shows its echelon name and unit-type filter, an
     * {@code <option>} decodes its weight-class content, and everything else falls back to the tag with
     * its key attributes.
     */
    private static String treeLabelFor(Element element) {
        String tag = element.getTagName();
        switch (tag) {
            case "force": {
                String echelonName = attr(element, "eschName");
                String label = echelonName.isEmpty() ? "force" : echelonName;
                String unitType = attr(element, "ifUnitType");
                if (!unitType.isEmpty()) {
                    label += "  (" + unitType.replace("|", "/") + ")";
                }
                return label;
            }
            case "option":
            case "subforce": {
                String text = directText(element).strip();
                StringBuilder label = new StringBuilder(tag);
                String weightClass = attr(element, "weightClass");
                if (!text.isEmpty()) {
                    label.append(": ").append(maybeDecodeWeightList(text));
                } else if (!weightClass.isEmpty()) {
                    label.append(": weights ").append(maybeDecodeWeightList(weightClass));
                }
                String weight = attr(element, "weight");
                if (!weight.isEmpty()) {
                    label.append("  (weight ").append(weight).append(')');
                }
                return label.toString();
            }
            case "faction":
            case "parent": {
                String text = directText(element).strip();
                return text.isEmpty() ? tag : tag + ": " + decodeFaction(text);
            }
            case "ratingSystem": {
                String text = directText(element).strip();
                return text.isEmpty() ? tag : tag + ": " + text;
            }
            default: {
                NamedNodeMap attributes = element.getAttributes();
                if (attributes.getLength() == 0) {
                    return tag;
                }
                List<String> bits = new ArrayList<>();
                for (int i = 0; i < attributes.getLength(); i++) {
                    Node attribute = attributes.item(i);
                    bits.add(attribute.getNodeName() + "=" + attribute.getNodeValue());
                }
                return tag + "  [" + String.join(", ", bits) + ']';
            }
        }
    }

    // ------------------------------------------------------------------
    // Detail pane - semantic interpretation
    // ------------------------------------------------------------------

    private void showDetail() {
        Object selected = tree.getLastSelectedPathComponent();
        if (!(selected instanceof DefaultMutableTreeNode treeNode)
              || !(treeNode.getUserObject() instanceof XmlEntry entry)
              || !(entry.node instanceof Element element)) {
            detailArea.setText("");
            return;
        }
        detailArea.setText(explain(element, entry.file));
        detailArea.setCaretPosition(0);
    }

    /**
     * Produces the full human-readable interpretation of one element.
     */
    private String explain(Element element, File file) {
        String tag = element.getTagName();
        boolean inToc = isDescendantOf(element, "toc");
        StringBuilder out = new StringBuilder();

        out.append(tag);
        if (inToc) {
            out.append("  (inside <toc>)");
        }
        out.append("  -  ").append(elementSummary(tag, inToc)).append('\n');
        out.append("File: ").append(file.getName()).append("\n\n");

        String notes = elementNotes(tag, inToc);
        if (!notes.isEmpty()) {
            out.append(notes).append("\n\n");
        }

        List<String> predicates = explainPredicates(element);
        if (!predicates.isEmpty()) {
            out.append("APPLIES WHEN (all of these must be true):\n");
            for (String line : predicates) {
                out.append("  - ").append(line).append('\n');
            }
            out.append('\n');
        }

        List<String> assertions = explainAssertions(element);
        if (!assertions.isEmpty()) {
            out.append("SETS / DOES:\n");
            for (String line : assertions) {
                out.append("  - ").append(line).append('\n');
            }
            out.append('\n');
        }

        String text = directText(element).strip();
        if (!text.isEmpty()) {
            out.append("VALUE: ").append(text).append('\n');
            String interpreted;
            if (inToc && tag.equals("option")) {
                Node parent = element.getParentNode();
                String parentTag = (parent == null) ? "" : parent.getNodeName();
                interpreted = "menu choices: " + formatChoiceList(parentTag, text);
            } else {
                interpreted = interpretValue(tag, text);
            }
            if (!interpreted.isEmpty()) {
                out.append("  -> ").append(interpreted).append('\n');
            }
            out.append('\n');
        }

        List<Element> options = optionChildren(element);
        if (!options.isEmpty()) {
            out.append(explainOptions(tag, options, inToc)).append('\n');
        }

        List<String> structure = childStructure(element);
        if (!structure.isEmpty() && options.isEmpty()) {
            out.append("CONTAINS: ").append(String.join(", ", structure)).append("\n\n");
        }

        out.append("---- Raw XML ----\n").append(prettyPrint(element));
        return out.toString();
    }

    /**
     * One-line description of what an element type is. The {@code <toc>} children are menu definitions
     * for the UI rather than generation rules, so they get a different summary in that context.
     */
    private static String elementSummary(String tag, boolean inToc) {
        if (inToc) {
            switch (tag) {
                case "unitType":
                    return "the unit-type choices offered in the Force Generator UI";
                case "echelon", "eschelon":
                    return "the echelon (formation size) choices offered in the UI";
                case "rating":
                    return "the equipment-rating choices offered in the UI";
                case "flags":
                    return "the user-settable flag choices offered in the UI";
                case "option":
                    return "one menu entry: a comma-separated list of choices, gated by its conditions";
                default:
                    break;
            }
        }
        return switch (tag) {
            case "ruleset" -> "the complete force-generation ruleset for one faction";
            case "faction" -> "the faction code this ruleset belongs to";
            case "parent" -> "fallback ruleset for rules not defined here";
            case "ratingSystem" -> "the equipment-rating scale this faction uses";
            case "defaults" -> "default values used when the caller leaves a field unspecified";
            case "toc" -> "table of contents: the choices the Force Generator UI offers";
            case "customRanks" -> "a faction-specific rank system for MekHQ";
            case "base" -> "the rank system that supplies any non-overridden rank names";
            case "force" -> "a generation rule for one echelon (organization level)";
            case "co" -> "the commanding officer's rank and title";
            case "xo" -> "the executive officer's rank and title";
            case "name" -> "a formation-naming rule";
            case "weightClass" -> "a random table that picks the force's weight class";
            case "unitType" -> "selects the unit type (Mek, Tank, etc.)";
            case "ruleGroup" -> "a group of option rules applied together";
            case "changeEschelon" -> "re-routes generation to a different echelon node";
            case "subforces" -> "defines the child forces this echelon is built from";
            case "subforce" -> "one child force, always generated";
            case "subforceOption" -> "a weighted random choice between child-force layouts";
            case "option" -> "one entry in a selection group";
            case "attachedForces" -> "support forces attached to this echelon";
            case "attachedForce" -> "one attached support force";
            case "flags" -> "sets generation flags on the force";
            case "role" -> "sets the force's mission role(s)";
            case "motive" -> "sets the force's movement mode(s)";
            case "formation" -> "sets the force's formation type";
            case "chassis" -> "restricts generation to specific chassis";
            case "variant" -> "restricts generation to specific variants";
            case "eschelon", "echelon" -> "an echelon (organization-level) entry";
            case "rank" -> "a rank value";
            case "title" -> "a title string";
            case "rankSystem" -> "the rank system identifier";
            default -> "ruleset node";
        };
    }

    /**
     * Longer explanatory note describing how the ratgen engine consumes this element type. Sourced from
     * docs/RAT and Force Generator Stuff/force-generator.txt.
     */
    private static String elementNotes(String tag, boolean inToc) {
        if (inToc) {
            switch (tag) {
                case "unitType":
                case "echelon":
                case "eschelon":
                case "rating":
                case "flags":
                    return "MENU DEFINITION - not a generation rule. The Force Generator UI shows the "
                          + "<option> whose conditions match the current settings (usually the campaign "
                          + "date). Nothing is rolled here; this only populates a dropdown.";
                case "option":
                    return "One menu entry. Its VALUE is a comma-separated list of choices; the "
                          + "conditions decide when this entry's list is the one shown to the user.";
                default:
                    break;
            }
        }
        return switch (tag) {
            case "ruleset" -> "The engine picks the ruleset whose faction matches the force being built. "
                  + "Any rule not found here is looked up in the <parent> ruleset, then its parent, and "
                  + "so on - generic IS / Periphery / CLAN rulesets sit at the root.";
            case "parent" -> "When no matching <force> rule is found in this file, the engine retries "
                  + "the search in this parent ruleset (and its parents) before giving up.";
            case "ratingSystem" -> "Defines the values the 'rating' property may take: "
                  + "IS = A,B,C,D,F  |  SL = A,B,C  |  CLAN = Keshik,FL,SL,Sol,PG  |  ROS = SB,HS,PG,TP.";
            case "defaults" -> "Fills in unit type / echelon / rank system / rating on the ROOT force "
                  + "ONLY when the caller left them unspecified. It never overrides an explicit choice.";
            case "customRanks" -> "Defines a faction-specific rank system for MekHQ: a <base> system plus "
                  + "per-level <rank> name overrides.";
            case "toc" -> "Drives the Force Generator UI's dropdowns (unit types, echelons, ratings, "
                  + "flags). It only populates the UI - it generates nothing itself.";
            case "force" -> "GENERATION FLOW: when the engine builds a tree node it scans <force> nodes "
                  + "top-to-bottom and applies the FIRST whose APPLIES WHEN conditions all match. It then "
                  + "runs this node's property rules, assigns CO/XO, and creates the child forces under "
                  + "<subforces> (plus <attachedForces> if support generation is on). Every child is then "
                  + "processed the same way, recursively, down to the lance / star level.";
            case "co" -> "Sets the commanding officer's rank (VALUE = a rank level; affects MekHQ "
                  + "personnel only, not MegaMek play). 'position=0' means the CO commands no unit; "
                  + "otherwise the engine walks the first branch of subforces and places the CO there.";
            case "xo" -> "Sets the executive officer's rank. An XO is assigned only if an <xo> element "
                  + "matches the node. By default the XO takes the first subforce that does not contain "
                  + "the CO.";
            case "name" -> "Names this force node. The VALUE may contain {token} expressions evaluated "
                  + "from the node's position in the tree (see VALUE below). If several <name> rules "
                  + "match, the first one wins.";
            case "weightClass" -> "PROPERTY RULE: the engine SKIPS this whole element if the weight "
                  + "class is already set - UNLESS the element also carries an ifWeightClass predicate. "
                  + "So a caller-supplied weight class survives untouched, but a conditional rule can "
                  + "still refine it. One <option> is then chosen by weighted roll.";
            case "unitType" -> "In <toc>: lists the unit types offered in the UI. As a property rule "
                  + "under <force>: rolls/sets the unit type, and is SKIPPED if the unit type is already "
                  + "set unless it also carries an ifUnitType predicate.";
            case "chassis", "variant", "motive", "formation" -> "PROPERTY RULE: rolls/sets the "
                  + tag + " property, and is SKIPPED if that property is already set unless the element "
                  + "also carries the matching if-predicate.";
            case "role", "flags" -> "PROPERTY RULE (set-valued): unlike single-value properties, "
                  + tag + " is a SET and is ALWAYS evaluated even when already populated. In values, "
                  + "'+x' adds, '-x' removes, and a bare list replaces the whole set.";
            case "ruleGroup" -> "Property rules are normally all tested against the node's CURRENT "
                  + "state before any of them run. A <ruleGroup> forces ordering: every matching rule "
                  + "inside one group runs before the next group is tested - so a later rule can react "
                  + "to an earlier rule's changes.";
            case "changeEschelon" -> "PROPERTY RULE that re-routes generation. If a non-empty <option> "
                  + "is chosen, the engine ABANDONS the current <force> rule immediately and searches "
                  + "for a new <force> rule matching the changed echelon. An empty option means "
                  + "'stay at this echelon'.";
            case "subforces" -> "Creates the child force nodes, which inherit this node's properties. "
                  + "The 'generate' attribute can instead invoke the RAT Generator directly: "
                  + "chassis/model make all children share a chassis / exact unit; 'group' builds the "
                  + "children as one cohesive lance via the Campaign Operations formation rules.";
            case "subforce" -> "Always creates child node(s). The VALUE is the child's echelon level. "
                  + "'num' sets how many to create; a 'weightClass' list creates one child per listed "
                  + "weight (e.g. weightClass=\"H,H,M\" => two Heavy children + one Medium).";
            case "subforceOption" -> "Exactly ONE <option> below is chosen by weighted random roll, and "
                  + "that option's child force(s) are created. An empty <option> adds no child at all - "
                  + "that is how the engine models 'maybe a support detachment, maybe not'.";
            case "option" -> "One candidate in a weighted random pick. 'weight' raises its odds "
                  + "proportionally (default 1); predicates filter it out when they do not match. Its "
                  + "assertions apply to the CURRENT node - EXCEPT inside <subforces> / <attachedForces>, "
                  + "where they apply to the newly created child node.";
            case "attachedForces" -> "Like <subforces>, but processed ONLY when the user enabled "
                  + "support-force generation. The CO and XO are never placed in attached forces.";
            case "asParent" -> "Redirects to the parent ruleset: the engine finds a matching <force> "
                  + "rule there and applies only its <subforces> section. Used to reuse a parent's "
                  + "structure while overriding other properties.";
            case "asFaction" -> "Like <asParent>, but redirects to the named faction's ruleset (the "
                  + "faction key is the VALUE).";
            case "eschelon", "echelon" -> "An echelon entry. A trailing '^' = augmented, '+' = "
                  + "reinforced, '-' = understrength.";
            default -> "";
        };
    }

    /**
     * Interprets every {@code if*} attribute (predicate) into a plain-English condition.
     */
    private static List<String> explainPredicates(Element element) {
        List<String> lines = new ArrayList<>();
        NamedNodeMap attributes = element.getAttributes();
        for (int i = 0; i < attributes.getLength(); i++) {
            Node attribute = attributes.item(i);
            String name = attribute.getNodeName();
            if (!name.startsWith("if")) {
                continue;
            }
            String value = attribute.getNodeValue();
            lines.add(switch (name) {
                case "ifUnitType" -> "unit type is " + interpretMatch(value, UnaryOperator.identity());
                case "ifWeightClass" -> "weight class is "
                      + interpretMatch(value, RulesetXmlViewer::decodeWeightClass);
                case "ifRating" -> "equipment rating is " + interpretMatch(value, UnaryOperator.identity());
                case "ifRole" -> "mission roles include " + interpretMatch(value, UnaryOperator.identity());
                case "ifFlags" -> "flags include " + interpretMatch(value, UnaryOperator.identity());
                case "ifMotive" -> "movement mode includes " + interpretMatch(value, UnaryOperator.identity());
                case "ifFaction" -> "faction is " + interpretMatch(value, RulesetXmlViewer::decodeFaction);
                case "ifEschelon" -> "echelon is " + interpretMatch(value, UnaryOperator.identity());
                case "ifName" -> "the formation name matches " + interpretMatch(value, UnaryOperator.identity());
                case "ifIndex" -> "the child index is " + interpretMatch(value, UnaryOperator.identity());
                case "ifAugmented" -> "1".equals(value)
                      ? "the force IS augmented (a Nova / combined-arms force)"
                      : "the force is NOT augmented";
                case "ifTopLevel" -> "1".equals(value)
                      ? "this is the top-level force (the one the user asked for)"
                      : "this is NOT the top-level force";
                case "ifDateBetween", "ifYearBetween" -> "the campaign date is " + interpretDateRange(value);
                default -> name + " = \"" + value + "\"  (unrecognized condition)";
            });
        }
        return lines;
    }

    /**
     * Interprets every non-{@code if} attribute (assertion) into a plain-English effect.
     */
    private static List<String> explainAssertions(Element element) {
        List<String> lines = new ArrayList<>();
        NamedNodeMap attributes = element.getAttributes();
        for (int i = 0; i < attributes.getLength(); i++) {
            Node attribute = attributes.item(i);
            String name = attribute.getNodeName();
            if (name.startsWith("if")) {
                continue;
            }
            String value = attribute.getNodeValue();
            lines.add(switch (name) {
                case "weightClass" -> value.contains(",")
                      ? "weight class per child force: " + perChildWeights(value)
                      : "set weight class to " + decodeWeightClass(value);
                case "unitType" -> "set unit type to " + value;
                case "rating" -> "set equipment rating to " + value;
                case "role" -> "mission role(s): " + addRemoveList(value);
                case "flags" -> "generation flags: " + addRemoveList(value);
                case "motive" -> "movement mode(s): " + addRemoveList(value);
                case "chassis" -> "restrict chassis to: " + addRemoveList(value);
                case "model" -> "restrict model to: " + addRemoveList(value);
                case "variant" -> "restrict variant to: " + addRemoveList(value);
                case "formation" -> "set formation type to " + value;
                case "augmented" -> "1".equals(value)
                      ? "mark the force as augmented" : "mark the force as not augmented";
                case "echelon" -> "set echelon to " + value;
                case "eschName" -> "echelon display name: \"" + value + '"';
                case "eschName2" -> "alternate echelon display name: \"" + value + '"';
                case "faction" -> "switch faction to " + decodeFaction(value)
                      + " (also resets the rank system and marks the force top-level)";
                case "rankSystem" -> "set rank system to " + value;
                case "name" -> "set the formation name to \"" + value + '"';
                case "fluffName" -> "set the fluff name to \"" + value + '"';
                case "weight" -> "selection weight " + value + " (relative odds when this option is rolled)";
                case "num" -> "generate " + value + " cop" + ("1".equals(value) ? "y" : "ies");
                case "generate" -> "generation mode: " + value;
                case "parentFaction", "asParent" -> "use the parent force's faction";
                case "asFaction" -> "generate as faction " + decodeFaction(value);
                default -> name + " = \"" + value + '"';
            });
        }
        return lines;
    }

    /**
     * Renders an option table. Inside {@code <toc>} the options are a UI menu (the dropdown shows
     * whichever entry's conditions match), so no probabilities are shown. For {@code <subforces>} every
     * entry is generated; everywhere else exactly one entry is chosen by weighted random roll, so each
     * line shows its roll probability. Assertions carried by an option are tagged with whether they land
     * on this node or on the new child force.
     */
    private static String explainOptions(String parentTag, List<Element> options, boolean inToc) {
        if (inToc) {
            StringBuilder out = new StringBuilder();
            out.append("AVAILABLE CHOICES (a UI menu - the dropdown shows the entry whose conditions "
                  + "match; nothing is rolled):\n");
            for (Element option : options) {
                List<String> conditions = explainPredicates(option);
                if (conditions.isEmpty()) {
                    out.append("  [always shown]\n");
                } else {
                    out.append("  [shown when ").append(String.join("; ", conditions)).append("]\n");
                }
                out.append("      ")
                      .append(formatChoiceList(parentTag, directText(option).strip()))
                      .append('\n');
            }
            return out.toString();
        }

        boolean allGenerated = parentTag.equals("subforces") || parentTag.equals("attachedForces");
        boolean childContext = allGenerated || parentTag.equals("subforceOption");
        boolean decodeWeights = parentTag.equals("weightClass");

        int totalWeight = 0;
        for (Element option : options) {
            totalWeight += optionWeight(option);
        }
        StringBuilder out = new StringBuilder();
        if (allGenerated) {
            out.append("CHILD FORCES (every entry below is generated):\n");
        } else {
            out.append("ROLLS ONE OF (total weight ").append(totalWeight)
                  .append(" - exactly one is chosen):\n");
        }
        for (Element option : options) {
            String content = directText(option).strip();
            String shown;
            if (content.isEmpty()) {
                shown = childContext ? "(no child force added)" : "(no change)";
            } else if (decodeWeights) {
                shown = decodeWeightClass(content);
            } else {
                shown = content;
            }
            if (allGenerated) {
                out.append(String.format(Locale.ROOT, "  %-32s%n", shown));
            } else {
                int weight = optionWeight(option);
                double percent = (totalWeight > 0) ? (100.0 * weight / totalWeight) : 0.0;
                out.append(String.format(Locale.ROOT, "  %-32s weight %d  (%.1f%%)%n",
                      shown, weight, percent));
            }
            if (!content.isEmpty() && !decodeWeights) {
                String interpreted = interpretValue(option.getTagName(), content);
                if (!interpreted.isEmpty()) {
                    out.append("        => ").append(interpreted).append('\n');
                }
            }
            String target = childContext ? "child force" : "this node";
            for (String effect : explainAssertions(option)) {
                if (effect.startsWith("selection weight")) {
                    continue;
                }
                out.append("        - (").append(target).append(") ").append(effect).append('\n');
            }
            for (String condition : explainPredicates(option)) {
                out.append("        ? only when ").append(condition).append('\n');
            }
        }
        return out.toString();
    }

    /**
     * Context-aware interpretation of an element's text content - the same text means different things
     * depending on the element it sits in.
     */
    private static String interpretValue(String tag, String text) {
        return switch (tag) {
            case "co", "xo" -> "rank level " + text + " (an index into the MekHQ rank system)";
            case "rankSystem" -> "MekHQ rank-system index " + text;
            case "subforce", "subforceOption", "echelon", "eschelon" -> interpretEchelon(text);
            case "option" -> text.matches(".*[%^+\\-].*") ? interpretEchelon(text) : "";
            case "name" -> interpretNameTokens(text);
            case "faction", "asFaction", "parent" -> "faction key " + decodeFaction(text);
            case "weightClass" -> decodeWeightClass(text);
            case "formation" -> "Campaign Operations formation type '" + text + "'";
            default -> "";
        };
    }

    /**
     * Decodes an echelon value and its optional reinforced / understrength / augmented suffix.
     */
    private static String interpretEchelon(String text) {
        String core = text;
        String suffix = "";
        if (core.endsWith("+")) {
            suffix = " - reinforced: the child generates an EXTRA subforce";
            core = core.substring(0, core.length() - 1);
        } else if (core.endsWith("-")) {
            suffix = " - understrength: the child generates ONE FEWER subforce";
            core = core.substring(0, core.length() - 1);
        } else if (core.endsWith("^")) {
            suffix = " - augmented (Nova / combined-arms / choir)";
            core = core.substring(0, core.length() - 1);
        }
        if (core.isBlank()) {
            return "no echelon" + suffix;
        }
        return "echelon " + core + suffix;
    }

    /**
     * Explains the {@code {token}} placeholders used in {@code <name>} content.
     */
    private static String interpretNameTokens(String text) {
        List<String> notes = new ArrayList<>();
        Matcher matcher = Pattern.compile("\\{([^}]*)}").matcher(text);
        while (matcher.find()) {
            String token = matcher.group(1);
            String base = token;
            String modifiers = "";
            int colon = base.indexOf(':');
            if (colon >= 0) {
                modifiers = base.substring(colon + 1);
                base = base.substring(0, colon);
            }
            String meaning = switch (base) {
                case "cardinal" -> "1, 2, 3 ... by sibling position";
                case "ordinal" -> "First, Second, Third ...";
                case "roman" -> "I, II, III ...";
                case "alpha" -> "A, B, C ...";
                case "phonetic" -> "Alpha, Bravo, Charlie ...";
                case "greek" -> "Alpha, Beta, Gamma ...";
                case "latin" -> "Prima, Secunda, Tertia ...";
                case "parent" -> "the parent formation's name";
                case "formation" -> "the formation type's display name";
                case "name" -> "a bracketed portion of the parent's name";
                default -> "(unrecognized token)";
            };
            if (modifiers.contains("parent")) {
                meaning += ", taken from the PARENT's sibling position";
            }
            if (modifiers.contains("distinct")) {
                meaning += ", applied only when needed to keep sibling names unique";
            }
            notes.add('{' + token + "} = " + meaning);
        }
        return String.join("; ", notes);
    }

    /**
     * Lists immediate child element tags with repeat counts, e.g. {@code co, weightClass, subforces (x2)}.
     */
    private static List<String> childStructure(Element element) {
        List<String> order = new ArrayList<>();
        Map<String, Integer> counts = new HashMap<>();
        NodeList children = element.getChildNodes();
        for (int i = 0; i < children.getLength(); i++) {
            Node child = children.item(i);
            if (child.getNodeType() != Node.ELEMENT_NODE) {
                continue;
            }
            String tag = child.getNodeName();
            if (!counts.containsKey(tag)) {
                order.add(tag);
            }
            counts.merge(tag, 1, Integer::sum);
        }
        List<String> result = new ArrayList<>();
        for (String tag : order) {
            int count = counts.get(tag);
            result.add(count > 1 ? tag + " (x" + count + ')' : tag);
        }
        return result;
    }

    // ------------------------------------------------------------------
    // Value interpreters
    // ------------------------------------------------------------------

    /**
     * Interprets the ratgen match syntax: {@code ,} = AND, {@code |} = OR, leading {@code !} = NOT,
     * blank = "the value is unset".
     */
    private static String interpretMatch(String value, UnaryOperator<String> tokenDecoder) {
        if (value.isBlank()) {
            return "unset (blank)";
        }
        boolean negated = value.startsWith("!");
        String body = negated ? value.substring(1) : value;
        List<String> andTerms = new ArrayList<>();
        for (String andPart : body.split(",")) {
            List<String> orTerms = new ArrayList<>();
            for (String orPart : andPart.split("\\|")) {
                orTerms.add(tokenDecoder.apply(orPart));
            }
            andTerms.add(String.join(" or ", orTerms));
        }
        String joined = String.join(" AND ", andTerms);
        return negated ? "NOT (" + joined + ')' : joined;
    }

    /**
     * Interprets the {@code ifDateBetween} syntax: {@code +} = AND, {@code |} = OR, each term is
     * {@code start,end} with either side blank meaning open-ended.
     */
    private static String interpretDateRange(String value) {
        List<String> andTerms = new ArrayList<>();
        for (String andPart : value.split("\\+")) {
            List<String> orTerms = new ArrayList<>();
            for (String orPart : andPart.split("\\|")) {
                String[] dates = orPart.split(",", 2);
                String start = (dates.length > 0) ? dates[0].trim() : "";
                String end = (dates.length > 1) ? dates[1].trim() : "";
                if (start.isEmpty() && end.isEmpty()) {
                    orTerms.add("any date");
                } else if (start.isEmpty()) {
                    orTerms.add(end + " or earlier");
                } else if (end.isEmpty()) {
                    orTerms.add(start + " or later");
                } else {
                    orTerms.add("between " + start + " and " + end);
                }
            }
            andTerms.add(String.join(" or ", orTerms));
        }
        return String.join(" and ", andTerms);
    }

    /**
     * Decodes a single weight-class letter into its full name.
     */
    private static String decodeWeightClass(String code) {
        return switch (code.trim()) {
            case "UL" -> "Ultra-Light";
            case "L" -> "Light";
            case "M" -> "Medium";
            case "H" -> "Heavy";
            case "A" -> "Assault";
            case "SH", "C" -> "Super-Heavy";
            case "" -> "(any)";
            default -> code;
        };
    }

    /**
     * Decodes a comma-separated list of weight-class letters, leaving anything that is not a recognized
     * code untouched.
     */
    private static String maybeDecodeWeightList(String text) {
        if (!text.matches("(UL|L|M|H|A|SH|C)(,(UL|L|M|H|A|SH|C))*")) {
            return text;
        }
        List<String> decoded = new ArrayList<>();
        for (String code : text.split(",")) {
            decoded.add(decodeWeightClass(code));
        }
        return String.join(", ", decoded);
    }

    /**
     * Renders a comma-separated weight-class list as a per-child-index assignment.
     */
    private static String perChildWeights(String value) {
        String[] codes = value.split(",");
        List<String> parts = new ArrayList<>();
        for (int i = 0; i < codes.length; i++) {
            parts.add("child " + i + " -> " + decodeWeightClass(codes[i]));
        }
        return String.join(", ", parts);
    }

    /**
     * Interprets an additive/subtractive list: a {@code +} prefix adds, {@code -} removes, and a list
     * with no prefixes replaces the current value entirely.
     */
    private static String addRemoveList(String value) {
        if (value.isBlank()) {
            return "(clear)";
        }
        boolean additive = value.startsWith("+") || value.startsWith("-");
        List<String> parts = new ArrayList<>();
        for (String token : value.split(",")) {
            if (token.startsWith("+")) {
                parts.add("add " + token.substring(1));
            } else if (token.startsWith("-")) {
                parts.add("remove " + token.substring(1));
            } else {
                parts.add(token);
            }
        }
        return additive ? String.join(", ", parts)
              : "replace all with [" + String.join(", ", parts) + ']';
    }

    private static int optionWeight(Element option) {
        String weight = attr(option, "weight");
        if (weight.isEmpty()) {
            return 1;
        }
        try {
            return Integer.parseInt(weight.trim());
        } catch (NumberFormatException ex) {
            return 1;
        }
    }

    /**
     * Returns the direct {@code <option>} / {@code <subforce>} element children of an element.
     */
    private static List<Element> optionChildren(Element element) {
        List<Element> options = new ArrayList<>();
        NodeList children = element.getChildNodes();
        for (int i = 0; i < children.getLength(); i++) {
            Node child = children.item(i);
            if (child.getNodeType() == Node.ELEMENT_NODE
                  && ("option".equals(child.getNodeName()) || "subforce".equals(child.getNodeName()))) {
                options.add((Element) child);
            }
        }
        return options;
    }

    // ------------------------------------------------------------------
    // DOM helpers
    // ------------------------------------------------------------------

    private static String attr(Element element, String name) {
        return element.hasAttribute(name) ? element.getAttribute(name) : "";
    }

    /**
     * Walks up the DOM looking for an ancestor element with the given tag name.
     */
    private static boolean isDescendantOf(Node node, String ancestorTag) {
        Node parent = node.getParentNode();
        while (parent != null) {
            if ((parent.getNodeType() == Node.ELEMENT_NODE) && ancestorTag.equals(parent.getNodeName())) {
                return true;
            }
            parent = parent.getParentNode();
        }
        return false;
    }

    /**
     * Formats a comma-separated {@code <toc>} option list for display. {@code <flags>} entries use the
     * {@code value:displayName} format and are expanded accordingly; a literal {@code null} unit-type
     * entry is labeled as the no-preference choice.
     */
    private static String formatChoiceList(String parentTag, String content) {
        if (content.isBlank()) {
            return "(empty)";
        }
        List<String> items = new ArrayList<>();
        for (String raw : content.split(",")) {
            String token = raw.trim();
            if (token.isEmpty()) {
                continue;
            }
            if ("flags".equals(parentTag) && token.contains(":")) {
                String[] parts = token.split(":", 2);
                items.add(parts[0] + " (shown as \"" + parts[1] + "\")");
            } else if ("null".equals(token)) {
                items.add("null (no preference / any)");
            } else {
                items.add(token);
            }
        }
        return String.join(", ", items);
    }

    /**
     * Returns the concatenation of the element's direct text-node children (not descendant text).
     */
    private static String directText(Node element) {
        StringBuilder text = new StringBuilder();
        NodeList children = element.getChildNodes();
        for (int i = 0; i < children.getLength(); i++) {
            Node child = children.item(i);
            if ((child.getNodeType() == Node.TEXT_NODE) && (child.getNodeValue() != null)) {
                text.append(child.getNodeValue());
            }
        }
        return text.toString();
    }

    /**
     * Removes whitespace-only text nodes so the pretty-printer can re-indent the subtree cleanly.
     */
    private static void stripWhitespace(Node node) {
        NodeList children = node.getChildNodes();
        for (int i = children.getLength() - 1; i >= 0; i--) {
            Node child = children.item(i);
            if (child.getNodeType() == Node.TEXT_NODE) {
                if ((child.getNodeValue() == null) || child.getNodeValue().isBlank()) {
                    node.removeChild(child);
                }
            } else if (child.getNodeType() == Node.ELEMENT_NODE) {
                stripWhitespace(child);
            }
        }
    }

    /**
     * Serializes a DOM subtree to indented XML for the detail pane's cross-reference section.
     */
    private static String prettyPrint(Node node) {
        try {
            Transformer transformer = TransformerFactory.newInstance().newTransformer();
            transformer.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, "yes");
            transformer.setOutputProperty(OutputKeys.INDENT, "yes");
            transformer.setOutputProperty("{http://xml.apache.org/xslt}indent-amount", "2");
            StringWriter writer = new StringWriter();
            transformer.transform(new DOMSource(node), new StreamResult(writer));
            return writer.toString();
        } catch (Exception ex) {
            logger.error(ex, "Failed to pretty-print XML node");
            return "[unable to render XML]";
        }
    }

    /**
     * Tree user object pairing a DOM node with its display label and originating file.
     *
     * @param node  the DOM node this tree entry represents, or null for an error placeholder
     * @param label the text shown in the tree
     * @param file  the ruleset file this entry came from
     */
    private record XmlEntry(@Nullable Node node, String label, File file) {
        @Override
        public String toString() {
            return label;
        }
    }

    public static void main(String... args) {
        SwingUtilities.invokeLater(() -> {
            RulesetXmlViewer viewer;
            if (args.length > 0) {
                File directory = new File(args[0]);
                if (directory.isDirectory()) {
                    viewer = new RulesetXmlViewer(directory);
                } else {
                    logger.info("{} is not a valid directory name", args[0]);
                    viewer = new RulesetXmlViewer();
                }
            } else {
                viewer = new RulesetXmlViewer();
            }
            viewer.setVisible(true);
        });
    }
}

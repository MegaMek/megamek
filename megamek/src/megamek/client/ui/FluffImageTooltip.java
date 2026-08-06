/*
 * Copyright (C) 2024-2026 The MegaMek Team. All Rights Reserved.
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
package megamek.client.ui;

import java.awt.Color;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;

import megamek.client.ui.clientGUI.GUIPreferences;
import megamek.client.ui.util.FluffImageHelper;
import megamek.client.ui.util.UIUtil;
import megamek.common.annotations.Nullable;
import megamek.logging.MMLogger;

/**
 * This class is very specialized. It provides tooltip information for the fluff image tooltip in the
 * {@link megamek.client.ui.dialogs.unitSelectorDialogs.EntityReadoutPanel}, taken from yaml files that are supplied
 * with painted minis images.
 */
public class FluffImageTooltip {

    private static final MMLogger LOGGER = MMLogger.create(FluffImageTooltip.class);

    private static final ObjectMapper YAML_MAPPER = new ObjectMapper(new YAMLFactory());

    /** The suffix that marks a yaml file as the info sidecar of a fluff image. */
    private static final String YAML_FILE_SUFFIX = "data.yaml";

    /** The width of the tooltip text block, before GUI scaling. */
    private static final int TOOLTIP_WIDTH = 360;

    private static final String NODE_TITLE = "title";
    private static final String NODE_AUTHOR = "author";
    private static final String NODE_CONTENT = "content";
    private static final String NODE_TYPE = "type";
    private static final String VALUE_INSIGNIA = "insignia";

    /**
     * @return The CSS styles used by the fluff image tooltip.
     */
    private static String styles() {
        int labelSize = UIUtil.scaleForGUI(UIUtil.FONT_SCALE1);
        Color color = GUIPreferences.getInstance().getToolTipLightFGColor();
        String styleColor = Integer.toHexString(color.getRGB() & 0xFFFFFF);
        return "span { font-family:Noto Sans; font-size:" + labelSize + "; }"
                + ".label { color:" + styleColor + "; }";
    }

    /**
     * Returns the tooltip text for the supplied FluffImageRecord, if any can be found, {@code null} otherwise.
     *
     * @param record The FluffImageRecord that is currently shown as an image
     *
     * @return A tooltip text, or {@code null} if no yaml info is available
     */
    public static @Nullable String getTooltip(FluffImageHelper.FluffImageRecord record) {
        return findYamlInfo(record).map(FluffImageTooltip::getTooltip).orElse(null);
    }

    private static Optional<File> findYamlInfo(FluffImageHelper.FluffImageRecord record) {
        return (record.file() == null) ? Optional.empty() : getYamlFile(record.file());
    }

    private static @Nullable String getTooltip(File yamlFile) {
        try {
            JsonNode node = YAML_MAPPER.readTree(yamlFile);

            StringBuilder result = new StringBuilder("<HTML><HEAD><STYLE>" + styles() + "</STYLE></HEAD><BODY>");
            int width = UIUtil.scaleForGUI(TOOLTIP_WIDTH);
            result.append("<div width=").append(width).append(">");

            appendNodeValue(result, node, NODE_TITLE, Messages.getString("FluffImageTooltip.unit"), false);
            appendNodeValue(result, node, NODE_AUTHOR, Messages.getString("FluffImageTooltip.artist"), true);

            if (node.has(NODE_CONTENT)) {
                String description = findInsignia(node.get(NODE_CONTENT));
                if (!description.isBlank()) {
                    appendLabelledValue(result, Messages.getString("FluffImageTooltip.insignia"), description, true);
                }
            }
            result.append("</div></BODY></HTML>");
            return result.toString();
        } catch (IOException exception) {
            LOGGER.warn("Could not read fluff image info from {}", yamlFile, exception);
            return null;
        }
    }

    /**
     * Appends a "label value" pair to the tooltip, taking the value from the given yaml node. Does nothing if the
     * node is absent or its value is blank.
     *
     * @param result               The tooltip being assembled
     * @param node                 The yaml root node
     * @param nodeName             The name of the yaml node holding the value
     * @param label                The already localized label to show in front of the value
     * @param precedeWithLineBreak {@code true} to start the entry on a new line
     */
    private static void appendNodeValue(StringBuilder result, JsonNode node, String nodeName, String label,
          boolean precedeWithLineBreak) {
        if (!node.has(nodeName)) {
            return;
        }
        String value = node.get(nodeName).asText();
        if (!value.isBlank()) {
            appendLabelledValue(result, label, value, precedeWithLineBreak);
        }
    }

    /**
     * Appends a "label value" pair to the tooltip. The separating space is added here so that the localized labels
     * do not have to carry a trailing space.
     *
     * @param result               The tooltip being assembled
     * @param label                The already localized label to show in front of the value
     * @param value                The value to show
     * @param precedeWithLineBreak {@code true} to start the entry on a new line
     */
    private static void appendLabelledValue(StringBuilder result, String label, String value,
          boolean precedeWithLineBreak) {
        String lineBreak = precedeWithLineBreak ? "<BR>" : "";
        result.append(UIUtil.spanCSS("label", lineBreak + label + " "))
                .append(UIUtil.spanCSS("value", value));
    }

    private static String findInsignia(JsonNode contentNode) {
        List<JsonNode> nodes = new ArrayList<>();
        contentNode.iterator().forEachRemaining(nodes::add);
        for (JsonNode node : nodes) {
            if (node.has(NODE_TYPE) && node.get(NODE_TYPE).asText().equals(VALUE_INSIGNIA)) {
                return node.get(NODE_CONTENT).asText();
            }
        }
        return "";
    }

    private static Optional<File> getYamlFile(File imageFile) {
        File parent = imageFile.getParentFile();
        if (parent == null) {
            LOGGER.warn("Image file {} has no parent directory; cannot search for YAML.", imageFile);
            return Optional.empty();
        }
        try (Stream<Path> entries = Files.walk(parent.toPath(), 1)) {
            return entries.filter(entry -> isSuitableYamlFile(entry, imageFile)).map(Path::toFile).findFirst();
        } catch (Exception exception) {
            // Deliberately broad: walking a user-supplied directory can also fail with UncheckedIOException
            // or a SecurityException, and a missing tooltip must never break the readout panel.
            LOGGER.warn("Error while reading files from {}", parent, exception);
            return Optional.empty();
        }
    }

    private static boolean isSuitableYamlFile(Path yamlFile, File imageFile) {
        if (Files.isDirectory(yamlFile)) {
            return false;
        }

        Path yamlFileNamePath = yamlFile.getFileName();
        if (yamlFileNamePath == null) {
            return false;
        }

        String yamlFileName = yamlFileNamePath.toString();
        if (!yamlFileName.endsWith(YAML_FILE_SUFFIX)) {
            return false;
        }

        int baseLength = yamlFileName.length() - YAML_FILE_SUFFIX.length();
        if (baseLength <= 0) {
            return false;
        }

        String baseName = yamlFileName.substring(0, baseLength);
        return imageFile.getName().contains(baseName);
    }

    private FluffImageTooltip() {}
}

/*
 * Copyright (c) 2024 - The MegaMek Team. All Rights Reserved.
 *
 * This file is part of MegaMek.
 *
 * MegaMek is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * MegaMek is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with MegaMek. If not, see <http://www.gnu.org/licenses/>.
 */
package megamek.client.ui.swing;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import megamek.client.ui.swing.util.FluffImageHelper;
import megamek.client.ui.swing.util.UIUtil;
import org.apache.logging.log4j.LogManager;

import java.awt.*;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

/**
 * This class is very specialized. It provides tooltip information for the fluff image tooltip in the
 * MechViewPanel, taken from yaml files that are supplied with painted minis images.
 */
public class FluffImageTooltip {

    private static final ObjectMapper yamlMapper = new ObjectMapper(new YAMLFactory());

    public static String styles() {
        int labelSize = UIUtil.scaleForGUI(UIUtil.FONT_SCALE1);
        Color color = GUIPreferences.getInstance().getToolTipLightFGColor();
        String styleColor = Integer.toHexString(color.getRGB() & 0xFFFFFF);
        return "span { font-family:Noto Sans; font-size:" + labelSize + "; }"
                + ".label { color:" + styleColor + "; }";
    }

    /**
     * Returns the tooltip text for the supplied FluffImageRecord, if any can be found, null otherwise.
     *
     * @param record The FluffImageRecord that is currently shown as an image
     * @return A tooltip text or null if no yaml info is available
     */
    public static String getTooltip(FluffImageHelper.FluffImageRecord record) {
        return findYamlInfo(record).map(FluffImageTooltip::getTooltip).orElse(null);
    }

    private static Optional<File> findYamlInfo(FluffImageHelper.FluffImageRecord record) {
        return (record.file() == null) ? Optional.empty() : getYamlFile(record.file());
    }

    private static String getTooltip(File yamlFile) {
        try {
            JsonNode node = yamlMapper.readTree(yamlFile);

            StringBuilder result = new StringBuilder("<HTML><HEAD><STYLE>" + styles() + "</STYLE></HEAD><BODY>");
            int width = UIUtil.scaleForGUI(360);
            result.append("<div width=").append(width).append(">");

            if (node.has("title")) {
                String unit = node.get("title").asText();
                if (!unit.isBlank()) {
                    result.append(UIUtil.spanCSS("label", "Unit: "))
                            .append(UIUtil.spanCSS("value", unit));
                }
            }
            if (node.has("author")) {
                String artist = node.get("author").asText();
                if (!artist.isBlank()) {
                    result.append(UIUtil.spanCSS("label", "<BR>Artist: "))
                            .append(UIUtil.spanCSS("value", artist));
                }
            }
            if (node.has("content")) {
                JsonNode contentNode = node.get("content");
                String description = findInsignia(contentNode);
                if (!description.isBlank()) {
                    result.append(UIUtil.spanCSS("label", "<BR>Insignia: "))
                            .append(UIUtil.spanCSS("value", description));
                }
            }
            result.append("</div></BODY></HTML>");
            return result.toString();
        } catch (IOException e) {
            return null;
        }
    }

    private static String findInsignia(JsonNode contentNode) {
        List<JsonNode> nodes = new ArrayList<>();
        contentNode.iterator().forEachRemaining(nodes::add);
        for (JsonNode node : nodes) {
            if (node.has("type") && node.get("type").asText().equals("insignia")) {
                return node.get("content").asText();
            }
        }
        return "";
    }

    private static Optional<File> getYamlFile(File imageFile) {
        try (Stream<Path> entries = Files.walk(imageFile.getParentFile().toPath())) {
            return entries.filter(p -> isSuitableYamlFile(p, imageFile)).map(Path::toFile).findFirst();
        } catch (Exception e) {
            LogManager.getLogger().warn("Error while reading files from {}", imageFile.getParentFile(), e);
            return Optional.empty();
        }
    }

    private static boolean isSuitableYamlFile(Path yamlFile, File imageFile) {
        String yamlFileName = yamlFile.getFileName().toString();
        return !Files.isDirectory(yamlFile) && yamlFileName.endsWith("data.yaml")
                && imageFile.getName().contains(yamlFileName.substring(0, yamlFileName.length() - 9));
    }
}

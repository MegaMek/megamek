/*
 * Copyright (C) 2020-2025 The MegaMek Team. All Rights Reserved.
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

package megamek.common.icons;

import java.awt.Graphics;
import java.awt.Image;
import java.awt.image.BufferedImage;
import java.io.PrintWriter;
import java.io.Serial;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import javax.swing.ImageIcon;

import megamek.common.annotations.Nullable;
import megamek.common.util.ImageUtil;
import megamek.utilities.xml.MMXMLUtility;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

/**
 * An AbstractIcon is an abstract class that ensures standardized and isolated image code for icons across the suite. It
 * handles creating the images and image icons, scaling and centering them, handles base comparisons and object
 * overrides, and implements basic File I/O. It also gracefully handles any failures to prevent a hard crash when an
 * image or icon cannot be created.
 */
public abstract class AbstractIcon implements Serializable {
    //region Variable Declarations
    @Serial
    private static final long serialVersionUID = 870271199001476289L;

    public static final String ROOT_CATEGORY = "-- General --";
    public static final String DEFAULT_ICON_FILENAME = "None";

    /**
     * The pre-0.51.01 administrator portrait folder, and the sub-folders it was sorted into.
     *
     * @see #migrateAdministratorCategories(String)
     */
    private static final String LEGACY_ADMINISTRATOR_FOLDER = "Admin";
    private static final String ADMINISTRATOR_FOLDER = "Administrator";
    private static final List<String> LEGACY_ADMINISTRATOR_SUB_FOLDERS = List.of("Command",
          "HR",
          "Logistical",
          "Transport");

    private String category;
    protected String filename;
    //endregion Variable Declarations

    //region Constructors
    protected AbstractIcon() {
        this(ROOT_CATEGORY, DEFAULT_ICON_FILENAME);
    }

    protected AbstractIcon(final @Nullable String category) {
        this(category, DEFAULT_ICON_FILENAME);
    }

    protected AbstractIcon(final @Nullable String category, final @Nullable String filename) {
        setCategory(category);
        setFilename(filename);
    }
    //endregion Constructors

    //region Getters/Setters
    public String getCategory() {
        return category;
    }

    public void setCategory(final @Nullable String category) {
        this.category = (category == null) ? ROOT_CATEGORY : category;
    }

    public String getFilename() {
        return filename;
    }

    public void setFilename(final @Nullable String filename) {
        this.filename = (filename == null) ? DEFAULT_ICON_FILENAME : filename;
    }
    //endregion Getters/Setters

    //region Boolean Methods
    public boolean isDefault() {
        return hasDefaultCategory() && hasDefaultFilename();
    }

    public boolean hasDefaultCategory() {
        return ROOT_CATEGORY.equals(getCategory()) || getCategory().isBlank();
    }

    public boolean hasDefaultFilename() {
        return DEFAULT_ICON_FILENAME.equals(getFilename()) || getFilename().isBlank();
    }
    //endregion Boolean Methods

    /**
     * This is used to determine whether the created image should be scaled or not by checking the Height and Width
     * values. If either is -1, then we need to scale the produced image
     *
     * @return whether to scale the image or not
     */
    protected boolean isScaled(final int width, final int height) {
        return (width == -1) || (height == -1);
    }

    /**
     * @return the ImageIcon for the Image stored by the AbstractIcon. May be null for non-existent files
     */
    public @Nullable ImageIcon getImageIcon() {
        final Image image = getImage();
        return (image == null) ? null : new ImageIcon(image);
    }

    /**
     * @param size the width to scale and center the ImageIcon to
     *
     * @return the created ImageIcon, scaled and centered to size width. This may be null for non-existent files
     */
    public @Nullable ImageIcon getImageIcon(final int size) {
        final Image image = getImage(size);
        return (image == null) ? null : new ImageIcon(image);
    }

    /**
     * @return the created image, which may only be null for a UnitIcon with no icon selected
     */
    public @Nullable Image getImage() {
        return getImage(0, 0);
    }

    /**
     * @param size the width to scale and center the image to
     *
     * @return the created image, scaled and centered to size width. This may only be null for a UnitIcon with no icon
     *       selected
     */
    public @Nullable Image getImage(final int size) {
        return getImage(size, -1);
    }

    /**
     * @param width  the width of the image, or -1 if this is the non-scaling factor
     * @param height the height of the image, or -1 if this is the non-scaling factor
     *
     * @return the image, scaled and centered if either the width or height are -1. This may only be null for a UnitIcon
     *       with no icon selected
     */
    public @Nullable Image getImage(final int width, final int height) {
        return getImage(getBaseImage(), width, height);
    }

    /**
     * This is used to create the proper image and scale it if required. It also handles null protection by creating a
     * blank image if required.
     *
     * @return the created image
     */
    protected Image getImage(final @Nullable Image image, final int width, final int height) {
        if (image == null) {
            return ImageUtil.failStandardImage();
        } else if (isScaled(width, height)) {
            return scaleAndCenter(image, (width != -1) ? width : height);
        } else {
            return image;
        }
    }

    /**
     * @return a square BufferedImage of the given size. Scales the given image to fit into the square and centers it on
     *       a transparent background.
     */
    public static BufferedImage scaleAndCenter(Image image, final int size) {
        final BufferedImage result = ImageUtil.createAcceleratedImage(size, size);
        final Graphics graphics = result.getGraphics();
        if (image.getWidth(null) > image.getHeight(null)) {
            image = image.getScaledInstance(size, -1, Image.SCALE_SMOOTH);
            graphics.drawImage(image, 0, (size - image.getHeight(null)) / 2, null);
        } else {
            image = image.getScaledInstance(-1, size, Image.SCALE_SMOOTH);
            graphics.drawImage(image, (size - image.getWidth(null)) / 2, 0, null);
        }
        return result;
    }

    /**
     * This is abstract to allow for different formats for determining the image in question
     *
     * @return the Image stored by the AbstractIcon
     */
    public abstract Image getBaseImage();

    //region File I/O
    public abstract void writeToXML(final PrintWriter pw, final int indent);

    protected void writeToXML(final PrintWriter pw, int indent, final String name) {
        if (isDefault()) {
            return;
        }

        MMXMLUtility.writeSimpleXMLOpenTag(pw, indent++, name);
        writeBodyToXML(pw, indent);
        MMXMLUtility.writeSimpleXMLCloseTag(pw, --indent, name);
    }

    protected void writeBodyToXML(final PrintWriter pw, int indent) {
        if (!hasDefaultCategory()) {
            MMXMLUtility.writeSimpleXMLTag(pw, indent, "category", getCategory());
        }

        if (!hasDefaultFilename()) {
            MMXMLUtility.writeSimpleXMLTag(pw, indent, "filename", getFilename());
        }
    }

    public void parseNodes(final NodeList nl) {
        for (int x = 0; x < nl.getLength(); x++) {
            parseNode(nl.item(x));
        }
    }

    /**
     * Parses a given node and performs actions based on the node name.
     *
     * @param workingNode The node to parse
     */
    protected void parseNode(final Node workingNode) {
        switch (workingNode.getNodeName()) {
            case "category":
                String category = MMXMLUtility.unEscape(workingNode.getTextContent().trim());

                // <50.07 compatibility handlers
                category = category.replace("Mek Tech", "MekTech");
                // <50.10 compatibility handlers
                category = category.replace("Vehicle Gunner", "Vehicle Crew Ground");
                category = category.replace("Vehicle Driver", "Vehicle Crew Ground");
                category = category.replace("Naval Driver", "Vehicle Crew Naval");
                category = category.replace("VTOL Pilot", "Vehicle Crew VTOL");
                category = category.replace("Vehicle Crewmember", "Vehicle Crew/Generic");
                category = category.replace("Combat Technician", "Vehicle Crew/Generic");
                category = category.replace("Conventional Aircraft Pilot", "Conventional Aircraft Crew");
                // <51.01 compatibility handlers
                category = migrateAdministratorCategories(category);

                setCategory(category);
                break;
            case "filename":
                filename = MMXMLUtility.unEscape(workingNode.getTextContent().trim());

                // <50.07 compatibility handlers
                filename = filename.replace("MekTek", "MekTech");

                setFilename(filename);
                break;
            default:
                break;
        }
    }

    /**
     * Rewrites an icon category that still points at one of the pre-0.51.01 administrator portrait folders.
     *
     * <p>The four administrator roles were merged into a single Administrator role in 0.51.01. The portraits that
     * shipped under {@code Admin/Command}, {@code Admin/HR}, {@code Admin/Logistical} and {@code Admin/Transport} were
     * consolidated into {@code Administrator}, so those categories are rewritten to point there. Without this handler
     * every character using one of those portraits would silently fall back to the default portrait, as the category
     * saved against them no longer resolves.</p>
     *
     * <p>The four sub-folder names are used as the signature of a shipped portrait, because no portrait was ever
     * shipped in the {@code Admin} folder itself. A category naming {@code Admin} without one of them - a bare
     * {@code Admin}, or an {@code Admin} sub-folder of the user's own making - therefore refers to a folder the user
     * created, which has not been renamed, and is deliberately left alone. The one case this cannot tell apart is a
     * user folder that happens to share a name with a shipped sub-folder.</p>
     *
     * <p>Matching is by whole path element, so {@code Admin/HR Reserves} is not treated as {@code Admin/HR}.</p>
     *
     * @param category the category to migrate
     *
     * @return the migrated category, or {@code category} unchanged if it does not reference a shipped legacy folder
     */
    private static String migrateAdministratorCategories(final String category) {
        if (!category.contains(LEGACY_ADMINISTRATOR_FOLDER)) {
            return category;
        }

        final List<String> elements = new ArrayList<>(Arrays.asList(category.split("/")));
        boolean migrated = false;

        for (int index = 0; index < elements.size(); index++) {
            final int subFolderIndex = index + 1;

            if (!LEGACY_ADMINISTRATOR_FOLDER.equals(elements.get(index)) ||
                  (subFolderIndex >= elements.size()) ||
                  !LEGACY_ADMINISTRATOR_SUB_FOLDERS.contains(elements.get(subFolderIndex))) {
                continue;
            }

            elements.set(index, ADMINISTRATOR_FOLDER);
            elements.remove(subFolderIndex);
            migrated = true;
        }

        if (!migrated) {
            return category;
        }

        // Categories are normally stored with a trailing separator, so preserve whichever form was saved
        final String joined = String.join("/", elements);
        return category.endsWith("/") ? joined + '/' : joined;
    }
    //endregion File I/O

    @Override
    public String toString() {
        return "[Camouflage] Category: " + category + "; Filename: " + filename;
    }

    @Override
    public boolean equals(final @Nullable Object other) {
        if (other == null) {
            return false;
        } else if (this == other) {
            return true;
        } else if (other instanceof AbstractIcon dOther) {
            return dOther.getCategory().equals(getCategory()) && dOther.getFilename().equals(getFilename());
        } else {
            return false;
        }
    }

    @Override
    public int hashCode() {
        return (getCategory() + getFilename()).hashCode();
    }
}

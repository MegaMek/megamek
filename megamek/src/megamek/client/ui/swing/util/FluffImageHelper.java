/*
 * Copyright (c) 2009 Jay Lawson
 * Copyright (c) 2013 Edward Cullen (eddy@obsessedcomputers.co.uk)
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
package megamek.client.ui.swing.util;

import megamek.common.BTObject;
import megamek.common.Configuration;
import megamek.common.Mech;
import megamek.common.annotations.Nullable;
import megamek.common.preference.PreferenceManager;
import org.apache.logging.log4j.LogManager;

import javax.swing.*;
import java.awt.*;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * This class provides methods for retrieving fluff images, for use in MM, MML and MHQ; also
 * for record sheets (where the fallback image "hud.png" is used).
 */
public final class FluffImageHelper {

    public static final String DIR_NAME_BA = "BattleArmor";
    public static final String DIR_NAME_CONVFIGHTER = "ConvFighter";
    public static final String DIR_NAME_DROPSHIP = "DropShip";
    public static final String DIR_NAME_FIGHTER = "Fighter";
    public static final String DIR_NAME_INFANTRY = "Infantry";
    public static final String DIR_NAME_JUMPSHIP = "JumpShip";
    public static final String DIR_NAME_MECH = "Mech";
    public static final String DIR_NAME_PROTOMEK = "ProtoMek";
    public static final String DIR_NAME_SMALLCRAFT = "Small Craft";
    public static final String DIR_NAME_SPACESTATION = "Space Station";
    public static final String DIR_NAME_VEHICLE = "Vehicle";
    public static final String DIR_NAME_WARSHIP = "WarShip";
    public static final String[] EXTENSIONS_FLUFF_IMAGE_FORMATS = { ".PNG", ".png", ".JPG",
            ".JPEG", ".jpg", ".jpeg", ".GIF", ".gif" };

    /**
     * Returns a fluff image for the given unit/object to be shown e.g. in the unit summary.
     *
     * <P>If a fluff image is stored in the unit/object itself, e.g. if it was part of the
     * unit's file or is created by the unit itself, this is returned. Note that this
     * is not used for canon units, but may be used in custom ones by adding a fluff
     * image to the unit in MML.
     *
     * <P>Otherwise, the fluff images directories are searched. First searches the user dir,
     * then the internal dir. Tries to match the image by chassis + model or chassis alone.
     * Chassis and model names are cleaned from " and / characters before matching. For
     * Meks with clan names, both names and the combinations are searched. The model
     * alone is not used to search.
     *
     * Returns null if no fluff image can be found.
     *
     * @param unit The unit
     * @return a fluff image or null, if no match is found
     */
    public static @Nullable Image getFluffImage(@Nullable BTObject unit) {
        return getFluffImage(unit, false);
    }

    /**
     * Returns a list of all fluff images for the given unit/object to be shown e.g. in the
     * unit summary.
     *
     * <P>If a fluff image is stored in the unit/object itself, e.g. if it was part of the
     * unit's file or is created by the unit itself, only this is returned. Note that this
     * is not used for canon units, but may be used in custom ones by adding a fluff
     * image to the unit in MML.
     *
     * <P>Otherwise, the fluff image directories are searched. First searches the user dir,
     * then the internal dir. Tries to match the image by chassis + model or chassis alone.
     * Chassis and model names are cleaned from " and / characters before matching. For
     * Meks with clan names, both names and the combinations are searched. The model
     * alone is not used to search.
     *
     * Returns null if no fluff image can be found.
     *
     * @param unit The unit
     * @return a fluff image or null, if no match is found
     */
    public static List<Image> getFluffImages(@Nullable BTObject unit) {
        return getFluffImageList(unit, false);
    }

    /**
     * Returns a list of all fluff images for the given unit/object to be shown e.g. in the
     * unit summary.
     *
     * <P>If a fluff image is stored in the unit/object itself, e.g. if it was part of the
     * unit's file or is created by the unit itself, only this is returned. Note that this
     * is not used for canon units, but may be used in custom ones by adding a fluff
     * image to the unit in MML.
     *
     * <P>Otherwise, the fluff image directories are searched. First searches the user dir,
     * then the internal dir. Tries to match the image by chassis + model or chassis alone.
     * Chassis and model names are cleaned from " and / characters before matching. For
     * Meks with clan names, both names and the combinations are searched. The model
     * alone is not used to search.
     *
     * Returns null if no fluff image can be found.
     *
     * @param unit The unit
     * @return a fluff image or null, if no match is found
     */
    public static List<FluffImageRecord> getFluffRecords(@Nullable BTObject unit) {
        return getFluffImageRecords(unit, false);
    }

    /**
     * Returns a fluff image for the given unit for the record sheet, with a fallback
     * file named "hud.png" if that is present in the right fluff directory, or null if nothing
     * can be found. See {@link #getFluffImage(BTObject)} for further comments on how the fluff
     * image is searched.
     *
     * @param unit The unit
     * @return a fluff image or null, if no match is found
     */
    @SuppressWarnings("unused") // used in MML
    public static @Nullable Image getRecordSheetFluffImage(@Nullable BTObject unit) {
        return getFluffImage(unit, true);
    }

    private static @Nullable Image getFluffImage(@Nullable BTObject unit, boolean recordSheet) {
        List<Image> fluffImages = getFluffImageList(unit, recordSheet);
        if (!fluffImages.isEmpty()) {
            //return fluffImages.get(0);
            // TEST --- just choose a random image from the available ones. Should be get(0) instead
            int rndIndex = (int) (Math.random() * fluffImages.size());
            return fluffImages.get(rndIndex);
            // ---
        } else {
            return null;
        }
    }

    /**
     * Returns a list of available fluff images. If a fluff image is embedded in the unit file,
     * only that image is returned, even if others are available from the fluff directories. The returned
     * list may be empty, but not null.
     *
     * @param unit The unit
     * @param recordSheet True if this image search is meant for a record sheet
     * @return Available fluff images or the embedded fluff image
     */
    private static List<FluffImageRecord> getFluffImageRecords(@Nullable BTObject unit, boolean recordSheet) {
        if (unit == null) {
            return new ArrayList<>();
        }
        Image embeddedFluffImage = unit.getFluffImage();
        if (embeddedFluffImage != null) {
            return List.of(new FluffImageRecord(embeddedFluffImage, ""));
        } else {
            return findFluffFiles(unit, recordSheet).stream().map(FluffImageRecord::toRecord).toList();
        }
    }

    /**
     * Returns a list of available fluff images. If a fluff image is embedded in the unit file,
     * only that image is returned, even if others are available from the fluff directories. The returned
     * list may be empty, but not null.
     *
     * @param unit The unit
     * @param recordSheet True if this image search is meant for a record sheet
     * @return Available fluff images or the embedded fluff image
     */
    private static List<Image> getFluffImageList(@Nullable BTObject unit, boolean recordSheet) {
        if (unit == null) {
            return new ArrayList<>();
        }
        Image embeddedFluffImage = unit.getFluffImage();
        if (embeddedFluffImage != null) {
            return List.of(embeddedFluffImage);
        } else {
            return findFluffFiles(unit, recordSheet).stream()
                    .map(File::toString)
                    .map(ImageIcon::new)
                    .map(ImageIcon::getImage)
                    .collect(Collectors.toList());
        }
    }

    private static Set<File> findFluffFiles(BTObject unit, boolean recordSheet) {
        Set<File> fileCandidates = new HashSet<>();
        var fluffDir = new File(Configuration.fluffImagesDir(), FluffImageHelper.getFluffPath(unit));

        List<String> nameCandidates = nameCandidates(unit);

        // UserDir matches
        String userDir = PreferenceManager.getClientPreferences().getUserDir();
        if (!userDir.isBlank() && new File(userDir).isDirectory()) {
            var fluffUserDir = new File(userDir, fluffDir.toString());

            for (String nameCandidate : nameCandidates) {
                for (String ext : EXTENSIONS_FLUFF_IMAGE_FORMATS) {
                    fileCandidates.add(new File(fluffUserDir, nameCandidate + ext));
                }
            }

            fileCandidates.addAll(getFluffInChassisDirs(unit, fluffUserDir));
        }

        // Internal fluff path matches
        for (String nameCandidate : nameCandidates) {
            for (String ext : EXTENSIONS_FLUFF_IMAGE_FORMATS) {
                fileCandidates.add(new File(fluffDir, nameCandidate + ext));
            }
        }

        // Fallback
        if (recordSheet) {
            fileCandidates.add(new File(fluffDir, "hud.png"));
        }

        fileCandidates.addAll(getFluffInChassisDirs(unit, fluffDir));
        fileCandidates.removeIf(f -> !f.exists() || f.isDirectory());
        return fileCandidates;
    }

    /**
     * With the addition of multiple fluff images, file matching depends on the directory a file is in.
     * <BR>- In the main fluff/[unittype]/ directory the old rules apply, i.e. a file is valid if it
     * matches the model exactly or if the filename is only the chassis and matches the unit's chassis.
     * The filename may now contain additional information after an underscore (atlas_xyz.jpg matches for
     * any Atlas mek).
     * <BR>- In a chassis subdirectory fluff/[unittype]/[chassis], all files match if [chassis]
     * matches the unit's chassis (even if the filename has the wrong model) AND if there is no
     * [model] subdirectory matching the unit's model. Empty models match the directory "---empty---".
     * The filename doesn't matter for matching.
     * <BR>- In a model subdirectory fluff/[unittype]/[chassis]/[model], all files match if the
     * unit's chassis and model match [chassis] and [model]. The filename doesn't matter for matching.
     */
    private static List<File> getFluffInChassisDirs(BTObject unit, File unitTypeFluffDir) {
        List<File> result = new ArrayList<>();
        for (String nameCandidate : chassisNameCandidates(unit)) {
            var chassisDir = new File(unitTypeFluffDir, nameCandidate);
            if (chassisDir.exists()) {
                result.addAll(getFluffInChassisDir(unit, chassisDir));
            }
        }
        return result;
    }

    /**
     * @return For the unit, returns the possible chassis lookup strings, which is simply the chassis
     * (the list has only one entry) for all units except Clan Meks with a double name, where the list
     * includes the four variations on Timber Wolf (Mad Cat), Mad Cat (Timber Wolf), Mad Cat and
     * Timber Wolf. Note that a few units have X (Y) chassis that are not clan double names. Those
     * will return only the full chassis X (Y).
     */
    private static List<String> chassisNameCandidates(BTObject unit) {
        List<String> result = new ArrayList<>();
        String sanitizedChassis = sanitize(unit.generalName());
        result.add(sanitizedChassis);
        if ((unit instanceof Mech) && !((Mech) unit).getClanChassisName().isBlank()) {
            String sanitizedClanChassis = sanitize(((Mech) unit).getClanChassisName());
            result.add(sanitizedClanChassis + " (" + sanitizedChassis + ")");
            result.add(sanitizedChassis + " (" + sanitizedClanChassis + ")");
            result.add(sanitizedClanChassis);
        }
        return result;
    }

    private static List<File> getFluffInChassisDir(BTObject unit, File chassisDir) {
        String sanitizedModel = sanitize(unit.specificName());
        if (sanitizedModel.isBlank()) {
            sanitizedModel = "---empty---";
        }
        List<File> result = new ArrayList<>();
        for (String chassisNameCandidate : chassisNameCandidates(unit)) {
            var modelDir = new File(chassisDir, chassisNameCandidate + " " + sanitizedModel);
            if (modelDir.exists()) {
                result.addAll(getFluffInDir(modelDir));
            }
        }
        if (result.isEmpty()) {
            result.addAll(getFluffInDir(chassisDir));
        }
        return result;
    }

    private static List<File> getFluffInDir(File dir) {
        List<File> result = new ArrayList<>();
        try (Stream<Path> entries = Files.walk(dir.toPath())) {
            result.addAll(entries.map(Objects::toString).map(File::new).toList());
        } catch (IOException e) {
            LogManager.getLogger().warn("Error while reading files from " + dir, e);
        }
        return result;
    }

    private static String sanitize(String original) {
        return original.replace("\"", "").replace("/", "");
    }

    private static List<String> nameCandidates(BTObject unit) {
        List<String> nameCandidates = new ArrayList<>();
        String sanitizedModel = sanitize(unit.specificName());
        for (String chassisNameCandidate : chassisNameCandidates(unit)) {
            nameCandidates.add((chassisNameCandidate + " " + sanitizedModel.trim()));
        }
        nameCandidates.addAll(chassisNameCandidates(unit));
        return nameCandidates;
    }

    private FluffImageHelper() { }

    /**
     * Returns the subdirectory in the fluff images directory suitable for the given
     * unit, i.e. "ConvFighter" for CF and FWS units.
     *
     * @param unit The unit
     * @return The unit type subdirectory for fluff images
     */
    public static String getFluffPath(BTObject unit) {
        if (unit.isWarShip()) {
            return DIR_NAME_WARSHIP;
        } else if (unit.isSpaceStation()) {
            return DIR_NAME_SPACESTATION;
        } else if (unit.isJumpShip()) {
            return DIR_NAME_JUMPSHIP;
        } else if (unit.isConventionalFighter() || unit.isFixedWingSupport()) {
            return DIR_NAME_CONVFIGHTER;
        } else if (unit.isDropShip()) {
            return DIR_NAME_DROPSHIP;
        } else if (unit.isSmallCraft()) {
            return DIR_NAME_SMALLCRAFT;
        } else if (unit.isFighter()) {
            return DIR_NAME_FIGHTER;
        } else if (unit.isBattleArmor()) {
            return DIR_NAME_BA;
        } else if (unit.isConventionalInfantry()) {
            return DIR_NAME_INFANTRY;
        } else if (unit.isProtoMek()) {
            return DIR_NAME_PROTOMEK;
        } else if (unit.isVehicle()) {
            return DIR_NAME_VEHICLE;
        } else {
            return DIR_NAME_MECH;
        }
    }

    public record FluffImageRecord(Image image, String fileName) {

        public static FluffImageRecord toRecord(File file) {
            return toRecord(file.toString());
        }

        public static FluffImageRecord toRecord(String fileName) {
            return new FluffImageRecord(new ImageIcon(fileName).getImage(), fileName);
        }
    }
}
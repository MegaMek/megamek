/*
 * Copyright (c) 2016, 2020 MekHQ Team. All rights reserved.
 *
 * This file is part of MekHQ.
 *
 * MekHQ is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * MekHQ is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with MekHQ.  If not, see <http://www.gnu.org/licenses/>.
 */
package megamek.common.util;

import java.io.File;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

import megamek.common.Configuration;
import megamek.common.logging.DefaultMmLogger;
import megamek.common.logging.MMLogger;

/**
 * This is a local MegaMek version of java.io.File and is designed to support
 * having files that could exist in two locations: the MM install location and
 * a userData directory.  When a file is opened, the path is first checked to
 * see if it exists within the userData directory, and if it does, that file is
 * opened.  However, if it doesn't exist, then the file is opened from MM's
 * install directory instead.
 * 
 * @author arlith
 *
 */
public class MegaMekFile {
    private List<File> fileList = new ArrayList<>();
    private List<File> subdirectoryList = new ArrayList<>();
    private boolean isDirectory;
    private static MMLogger logger = DefaultMmLogger.getInstance();

    public MegaMekFile(File parent, String child) {
        this(new File(parent, child).toString());
    }

    public MegaMekFile(String pathName) {
        File standardVersion = new File(pathName);
        File userDataVersion = new File(Configuration.userdataDir(), pathName);
        isDirectory = standardVersion.isDirectory();

        if (isDirectory) {
            // Create an array of files located in the standard file directory
            List<File> standardFiles = splitFilesAndSubdirectories(standardVersion.listFiles(), true);

            if (userDataVersion.exists()) {
                // If we have a user data version of the directory, then we need to do some additional
                // processing based on the files located within it
                List<File> userDataSubdirectories = splitFilesAndSubdirectories(standardVersion.listFiles(), false);

                // Note: subdirectories are always pulled from the standard folder path if possible
                Set<File> fileSet = new TreeSet<>(new FileComparator());

                // Handle FileList
                fileSet.addAll(fileList);
                fileSet.addAll(standardFiles);
                fileList.clear();
                fileList.addAll(fileSet);
                fileSet.clear();

                // Handle subdirectoriesList
                fileSet.addAll(subdirectoryList);
                fileSet.addAll(userDataSubdirectories);
                fileList.clear();
                fileList.addAll(fileSet);
            } else {
                // If we do not, then we just return the standard files list based on the files within
                fileList.addAll(standardFiles);
            }
        } else {
            fileList = new ArrayList<>();
            if (userDataVersion.exists()) {
                fileList.add(userDataVersion);
            } else {
                fileList.add(standardVersion);
            }
        }
    }

    /**
     *
     * @param files an array of files to split between subdirectories and files
     * @param type true if you want to return files, false for subdirectories
     * @return a list of either files or subdirectories based on the value of type
     */
    private List<File> splitFilesAndSubdirectories(File[] files, boolean type) {
        List<File> returnList = new ArrayList<>();

        if (type) {
            // Returning files, and saving subdirectories to the subdirectory list
            for (File file : files) {
                if (file.isFile()) {
                    returnList.add(file);
                } else {
                    subdirectoryList.add(file);
                }
            }
        } else {
            // Returning subdirectories, and saving files to the file list
            for (File file : files) {
                if (file.isFile()) {
                    fileList.add(file);
                } else {
                    returnList.add(file);
                }
            }
        }

        return returnList;
    }


    /**
     * getFile() is used to get a single file, and should not be used for directories
     * @return the first file in the list, or null if the fileList is of size 0
     */
    public File getFile() {
        if (isDirectory) {
            logger.warning(MegaMekFile.class, "getFile",
                    "Called getFile for a directory. Did you mean to call getFiles instead?");
        }
        if (fileList.size() < 1) {
            return null;
        } else {
            return fileList.get(0);
        }
    }

    /**
     * getFiles() is used for directories, and should not be used for single files
     * @return the fileList for the provided directory
     */
    public List<File> getFiles() {
        if (!isDirectory) {
            logger.warning(MegaMekFile.class, "getFiles",
                    "Called getFiles for a non-directory. Did you mean to call getFile instead?");
        }
        return fileList;
    }

    public String toString() {
        if (isDirectory) {
            return fileList.toString();
        } else if (fileList.size() < 1) {
            return "";
        } else {
            return fileList.get(0).toString();
        }
    }

    private static class FileComparator implements Comparator<File> {
        @Override
        public int compare(File f1, File f2) {
            if (Paths.get(f1.getPath()).getFileName().toString()
                    .equals(Paths.get(f2.getPath()).getFileName().toString())) {
                return 0;
            } else {
                return 1;
            }
        }
    }
}

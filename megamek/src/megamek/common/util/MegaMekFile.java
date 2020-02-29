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
import sun.reflect.generics.tree.Tree;

/**
 * This is a local MegaMek version of java.io.File and is designed to support
 * having files that could exist in two locations: the MM install location and
 * a userData directory. When a file is opened, the path is first checked to
 * see if it exists within the userData directory, and if it does, that file is
 * opened. However, if it doesn't exist, then the file is opened from MM's
 * install directory instead.
 * 
 * @author arlith
 *
 */
public class MegaMekFile {
    private List<File> fileList = new ArrayList<>();
    private boolean isDirectory;
    private static MMLogger logger = DefaultMmLogger.getInstance();

    public MegaMekFile(File parent, String child) {
        this(new File(parent, child).toString());
    }

    public MegaMekFile(String pathName) {
        File standardVersion = new File(pathName);
        isDirectory = standardVersion.isDirectory();

        if (isDirectory) {
            recursivelyAddAllFiles(standardVersion);
        } else {
            File userDataVersion = new File(Configuration.userdataDir(), pathName);
            if (userDataVersion.exists()) {
                fileList.add(userDataVersion);
            } else {
                fileList.add(standardVersion);
            }
        }
    }


    private void recursivelyAddAllFiles(File inputDirectory) {
        File[] files = inputDirectory.listFiles();

        File[] userDataFiles = new File(Configuration.userdataDir(), inputDirectory.getPath()).listFiles();
        if ((files != null) && (userDataFiles != null)) {
            // This is the primary and hardest case. We will first divide the files into files and
            // subdirectories, with userData being taken as the primary source for any files while
            // any subdirectories are listed as the standard directory
            Set<File> fileSet = new TreeSet<>(new FileComparator());
            Set<File> subdirectorySet = new TreeSet<>(new FileComparator());
            List<File> userDataSubdirectories = new ArrayList<>();

            // We start with the userData files, and save any files to the fileSet while
            // subdirectories are stored in the userDataSubdirectories to wait for further processing
            for (File file : userDataFiles) {
                if (file.isFile()) {
                    fileSet.add(file);
                } else {
                    userDataSubdirectories.add(file);
                }
            }

            // We can just add everything here to the sets, as userData has been processed already
            for (File file : fileSet) {
                if (file.isFile()) {
                    fileSet.add(file);
                } else {
                    subdirectorySet.add(file);
                }
            }

            // We've removed any duplicate files as they were added to the fileSet, so we can just
            // add all of the files to the fileList
            fileList.addAll(fileSet);

            // Now we add the userData subdirectories to the subdirectory set, to give us a list of
            // unique subdirectories
            subdirectorySet.addAll(userDataSubdirectories);

            // And finally we recursively search through the subdirectories
            for (File file : subdirectorySet) {
                recursivelyAddAllFiles(file);
            }
        } else if (files != null) {
            // This folder is only located in the standard folder path, so we can just recursively
            // add all files under it to the file list without needing to check for whether or not
            // they are in the userData directory path
            recursivelyAddAllFilesSinglePath(files);
        } else if (userDataFiles != null) {
            // This folder is only located in the userData folder path, so we can just recursively
            // add all files under it to the file list without needing to check for whether or not
            // they are in the standard directory path
            recursivelyAddAllFilesSinglePath(userDataFiles);
        }
    }

    /**
     * This recursively searches through a file tree to find all of the files located within it
     * @param files the file array to parse through
     */
    private void recursivelyAddAllFilesSinglePath(File[] files) {
        if (files != null) { //need this null check because listFile returns an array not a list
            for (File file : files) {
                if (file.isFile()) {
                    // Add the file to the file list
                    fileList.add(file);
                } else {
                    // Recursively search and add any found files to the file list
                    recursivelyAddAllFilesSinglePath(file.listFiles());
                }
            }
        }
    }

    /**
     * getFile() is used to get a single file, and should not be used for directories
     * @return the first file in the list, or null if the fileList is of size 0
     */
    public File getFile() {
        if (isDirectory) {
            logger.warning(MegaMekFile.class, "getFile",
                    "Called getFile for a directory, " + fileList.get(0).getParent()
                            + ". Did you mean to call getFiles instead?");
        }
        if (fileList.size() > 0) {
            return fileList.get(0);
        } else {
            return null;
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

    /**
     *
     * @return the paths of the files located within as a string
     */
    public String toString() {
        if (isDirectory) {
            return fileList.toString();
        } else if (fileList.size() > 0) {
            return fileList.get(0).toString();
        } else {
            return "";
        }
    }

    /**
     * This is a custom comparator that compares if two files have the same file name. It returns 0
     * if they do, otherwise it returns 1 (keep the pre-existing key)
     */
    private static class FileComparator implements Comparator<File> {
        @Override
        public int compare(File f1, File f2) {
            if (Paths.get(f1.getPath()).getFileName().toString()
                    .equals(Paths.get(f2.getPath()).getFileName().toString())) {
                return 0;
            } else {
                // This is purposeful, we want it to not add anything new that was
                return 1;
            }
        }
    }
}

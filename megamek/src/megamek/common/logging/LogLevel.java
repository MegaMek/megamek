/*
 * MegaMek - Copyright (C) 2000-2011 Ben Mazur (bmazur@sev.org)
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
package megamek.common.logging;

import java.util.ArrayList;
import java.util.List;

/**
 * Created with IntelliJ IDEA.
 *
 * @version %Id%
 * @author: Deric "Netzilla" Page (deric dot page at usa dot net)
 * @since: 9/7/13 9:31 AM
 */
public enum LogLevel {
    ERROR(0), WARNING(1), INFO(2), DEBUG(3);

    private int level;

    LogLevel(int level) {
        this.level = level;
    }

    public int getLevel() {
        return level;
    }

    public static String[] getLogLevelNames() {
        List<String> out = new ArrayList<String>();
        for (LogLevel l : values()) {
            out.add(l.toString());
        }
        return out.toArray(new String[out.size()]);
    }

    public static Integer[] getLogLevels() {
        List<Integer> out = new ArrayList<Integer>();
        for (LogLevel l : values()) {
            out.add(l.getLevel());
        }
        return out.toArray(new Integer[out.size()]);
    }

    public static LogLevel getLogLevel(String levelName) {
        for (LogLevel l : values()) {
            if (l.toString().equals(levelName)) {
                return l;
            }
        }
        return ERROR;
    }

    public static LogLevel getLogLevel(int level) {
        for (LogLevel l : values()) {
            if (l.getLevel() == level) {
                return l;
            }
        }
        return ERROR;
    }
}

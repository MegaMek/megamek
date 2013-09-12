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
package megamek.common.util;

import java.text.DateFormat;
import java.util.Date;

/**
 * Created with IntelliJ IDEA.
 *
 * @version %Id%
 * @author: Deric "Netzilla" Page (deric dot page at usa dot net)
 * @since: 9/7/13 9:33 AM
 */
public class Logger {

    private static LogLevel verbosity = LogLevel.ERROR;

    public static LogLevel getVerbosity() {
        return verbosity;
    }

    public static void setVerbosity(LogLevel verbosity) {
        Logger.verbosity = verbosity;
    }

    public static void log(Class<?> callingClass, String methodName, LogLevel level,
                           String msg) {
        if (level.getLevel() > verbosity.getLevel()) {
            return;
        }
        StringBuilder out = new StringBuilder(DateFormat.getDateTimeInstance().format(new Date()));
        out.append(" ").append(callingClass.getSimpleName());
        out.append(".").append(methodName);
        out.append(" [").append(level.toString()).append("]");
        out.append(" ").append(msg);
        System.out.println(out);
    }

    public static void log(Class<?> callingClass, String methodName, String msg) {
        log(callingClass, methodName, LogLevel.DEBUG, msg);
    }

    public static void log(Class<?> callingClass, String methodName, LogLevel level, Throwable t) {
        if (t == null) {
            return;
        }
        if (level.getLevel() > verbosity.getLevel()) {
            return;
        }
        StringBuilder msg = new StringBuilder(t.getMessage());
        for (StackTraceElement e : t.getStackTrace()) {
            msg.append("\n").append(e.toString());
        }
        log(callingClass, methodName, level, msg.toString());
    }

    public static void log(Class<?> callingClass, String methodName, Throwable t) {
        log(callingClass, methodName, LogLevel.ERROR, t);
    }

    public static void methodBegin(Class<?> callingClass, String methodName) {
        log(callingClass, methodName, LogLevel.DEBUG, "method begin");
    }

    public static void methodEnd(Class<?> callingClass, String methodName) {
        log(callingClass, methodName, LogLevel.DEBUG, "method end");
    }

}

/*
 * Copyright (c) 2017-2021 - The MegaMek Team. All Rights Reserved.
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
package megamek.common.logging;

import org.apache.log4j.Logger;

/**
 * Fake logger implementation for unit testing.
 * 
 * @author Deric Page (deric.page@nisc.coop) (ext 2335)
 * @version %Id%
 * @since 7/31/2017 2:35 PM
 */
public class FakeLogger implements MMLogger {
    @Override
    public Logger getLogger(String loggerName) {
        return null;
    }

    @Override
    public <T extends Throwable> T log(String className, String methodName, LogLevel logLevel, String message, T throwable) {
        return null;
    }

    @Override
    public boolean willLog(Class<?> callingClass, LogLevel level) {
        return false;
    }

    @Override
    public void setLogLevel(String category, LogLevel level) {

    }

    @Override
    public LogLevel getLogLevel(String category) {
        return null;
    }

    @Override
    public void removeLoggingProperties() {

    }

    @Override
    public void resetLogFile(String logFileName) {

    }

    @Override
    public void debug(String message) {

    }

    @Override
    public <T extends Throwable> T debug(T throwable) {
        return null;
    }

    @Override
    public <T extends Throwable> T debug(String message, T throwable) {
        return null;
    }

    @Override
    public void error(String message) {

    }

    @Override
    public <T extends Throwable> T error(T throwable) {
        return null;
    }

    @Override
    public <T extends Throwable> T error(String message, T throwable) {
        return null;
    }

    @Override
    public void fatal(String message) {

    }

    @Override
    public <T extends Throwable> T fatal(T throwable) {
        return null;
    }

    @Override
    public <T extends Throwable> T fatal(String message, T throwable) {
        return null;
    }

    @Override
    public void info(String message) {

    }

    @Override
    public <T extends Throwable> T info(T throwable) {
        return null;
    }

    @Override
    public <T extends Throwable> T info(String message, T throwable) {
        return null;
    }

    @Override
    public void trace(String message) {

    }

    @Override
    public <T extends Throwable> T trace(T throwable) {
        return null;
    }

    @Override
    public <T extends Throwable> T trace(String message, T throwable) {
        return null;
    }

    @Override
    public void warning(String message) {

    }

    @Override
    public <T extends Throwable> T warning(T throwable) {
        return null;
    }

    @Override
    public <T extends Throwable> T warning(String message, T throwable) {
        return null;
    }

    @Override
    public void methodBegin() {

    }

    @Override
    public void methodEnd() {

    }

    @Override
    public void methodCalled() {

    }

    @Override
    public <T extends Throwable> T log(LogLevel logLevel, String message, T throwable) {
        return null;
    }

    @Override
    public void setLogLevel(LogLevel level) {

    }

    @Override
    public LogLevel getLogLevel() {
        return null;
    }
}

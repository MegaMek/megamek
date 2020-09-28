/*
 * Copyright (c) 2017, 2020 - The MegaMek Team. All Rights Reserved.
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
    public <T extends Throwable> T log(Class<?> callingClass, String methodName, LogLevel logLevel, T throwable) {
        return null;
    }

    @Override
    public <T extends Throwable> T log(Class<?> callingClass, String methodName, LogLevel level, String message, T throwable) {
        return null;
    }

    @Override
    public void log(Class<?> callingClass, String methodName, LogLevel level, String message) {

    }

    @Override
    public <T extends Throwable> T debug(Class<?> callingClass, String methodName, T throwable) {
        return null;
    }

    @Override
    public void debug(Class<?> callingClass, String methodName, String message) {

    }

    @Override
    public void debug(Class<?> callingClass, String message) {

    }

    @Override
    public void debug(Object callingObject, String message) {

    }

    @Override
    public <T extends Throwable> T debug(Class<?> callingClass, T throwable) {
        return null;
    }

    @Override
    public <T extends Throwable> T debug(Object callingObject, T throwable) {
        return null;
    }

    @Override
    public <T extends Throwable> T debug(Object callingObject, String message, T throwable) {
        return null;
    }

    @Override
    public <T extends Throwable> T error(Class<?> callingClass, String methodName, String message, T throwable) {
        return null;
    }

    @Override
    public <T extends Throwable> T error(Class<?> callingClass, String methodName, T throwable) {
        return null;
    }

    @Override
    public void error(Class<?> callingClass, String methodName, String message) {

    }

    @Override
    public void error(Class<?> callingClass, String message) {

    }

    @Override
    public void error(Object callingObject, String message) {

    }

    @Override
    public <T extends Throwable> T error(Class<?> callingClass, T throwable) {
        return null;
    }

    @Override
    public <T extends Throwable> T error(Object callingObject, T throwable) {
        return null;
    }

    @Override
    public <T extends Throwable> T error(Object callingObject, String message, T throwable) {
        return null;
    }

    @Override
    public void fatal(Class<?> callingClass, String message) {

    }

    @Override
    public void fatal(Object callingObject, String message) {

    }

    @Override
    public <T extends Throwable> T fatal(Class<?> callingClass, T throwable) {
        return null;
    }

    @Override
    public <T extends Throwable> T fatal(Object callingObject, T throwable) {
        return null;
    }

    @Override
    public <T extends Throwable> T fatal(Class<?> callingClass, String message, T throwable) {
        return null;
    }

    @Override
    public <T extends Throwable> T fatal(Object callingObject, String message, T throwable) {
        return null;
    }

    @Override
    public <T extends Throwable> T info(Class<?> callingClass, String methodName, T throwable) {
        return null;
    }

    @Override
    public void info(Class<?> callingClass, String methodName, String message) {

    }

    @Override
    public void info(Class<?> callingClass, String message) {

    }

    @Override
    public void info(Object callingObject, String message) {

    }

    @Override
    public <T extends Throwable> T info(Class<?> callingClass, T throwable) {
        return null;
    }

    @Override
    public <T extends Throwable> T info(Object callingObject, T throwable) {
        return null;
    }

    @Override
    public <T extends Throwable> T info(Object callingObject, String message, T throwable) {
        return null;
    }

    @Override
    public void trace(Class<?> callingClass, String message) {

    }

    @Override
    public <T extends Throwable> T trace(Class<?> callingClass, String methodName, T throwable) {
        return null;
    }

    @Override
    public void trace(Object callingObject, String message) {

    }

    @Override
    public <T extends Throwable> T trace(Class<?> callingClass, T throwable) {
        return null;
    }

    @Override
    public <T extends Throwable> T trace(Object callingObject, T throwable) {
        return null;
    }

    @Override
    public <T extends Throwable> T trace(Object callingObject, String message, T throwable) {
        return null;
    }

    @Override
    public <T extends Throwable> T warning(Class<?> callingClass, String methodName, String message, T throwable) {
        return null;
    }

    @Override
    public void warning(Class<?> callingClass, String methodName, String message) {

    }

    @Override
    public void warning(Class<?> callingClass, String message) {

    }

    @Override
    public void warning(Object callingObject, String message) {

    }

    @Override
    public <T extends Throwable> T warning(Class<?> callingClass, T throwable) {
        return null;
    }

    @Override
    public <T extends Throwable> T warning(Object callingObject, T throwable) {
        return null;
    }

    @Override
    public <T extends Throwable> T warning(Class<?> callingClass, String message, T throwable) {
        return null;
    }

    @Override
    public <T extends Throwable> T warning(Object callingObject, String message, T throwable) {
        return null;
    }

    @Override
    public void methodBegin(Class<?> callingClass, String methodName) {

    }

    @Override
    public void methodEnd(Class<?> callingClass, String methodName) {

    }

    @Override
    public void methodCalled(Class<?> callingClass, String methodName) {

    }

    @Override
    public boolean willLog(Class<?> callingClass, LogLevel level) {
        return false;
    }

    @Override
    public void setLogLevel(String category, LogLevel level) {

    }

    @Override
    public void setLogLevel(Object callingObject, LogLevel level) {

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
        // TODO Auto-generated method stub
        
    }

    @Override
    public <T extends Throwable> T debug(T throwable) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public <T extends Throwable> T debug(String message, T throwable) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public void error(String message) {
        // TODO Auto-generated method stub
        
    }

    @Override
    public <T extends Throwable> T error(T throwable) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public <T extends Throwable> T error(String message, T throwable) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public void fatal(String message) {
        // TODO Auto-generated method stub
        
    }

    @Override
    public <T extends Throwable> T fatal(T throwable) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public <T extends Throwable> T fatal(String message, T throwable) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public void info(String message) {
        // TODO Auto-generated method stub
        
    }

    @Override
    public <T extends Throwable> T info(T throwable) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public <T extends Throwable> T info(String message, T throwable) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public void trace(String message) {
        // TODO Auto-generated method stub
        
    }

    @Override
    public <T extends Throwable> T trace(T throwable) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public <T extends Throwable> T trace(String message, T throwable) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public void warning(String message) {
        // TODO Auto-generated method stub
        
    }

    @Override
    public <T extends Throwable> T warning(T throwable) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public <T extends Throwable> T warning(String message, T throwable) {
        // TODO Auto-generated method stub
        return null;
    }
}

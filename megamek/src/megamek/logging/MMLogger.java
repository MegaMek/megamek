/*
 * MegaMek
 * Copyright (c) 2024 - The MegaMek Team. All Rights Reserved.
 *
 * This program is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License as published by the Free
 * Software Foundation; either version 2 of the License, or (at your option)
 * any later version.
 *
 * This program is distributed in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 * or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License
 * for more details.
 */

package megamek.logging;

import javax.swing.JOptionPane;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import io.sentry.Sentry;

public class MMLogger {
  MMLogger() {
    throw new IllegalStateException("MMLogger Utility Class");
  }

  public static void info(String message) {
    Logger logger = LogManager.getLogger();

    if (logger.isInfoEnabled()) {
      logger.info(message);
    }
  }

  public static void warn(String message) {
    Logger logger = LogManager.getLogger();

    if (logger.isWarnEnabled()) {
      logger.warn(message);
    }
  }

  public static void error(Throwable exception, String message) {
    Sentry.captureException(exception);

    Logger logger = LogManager.getLogger();
    if (logger.isErrorEnabled()) {
      logger.error(message, exception);
    }
  }

  public static void error(Throwable exception, String message, String title) {
    MMLogger.error(exception, message);
    JOptionPane.showMessageDialog(null, message, title, JOptionPane.ERROR_MESSAGE);
  }

  public static void fatal(Throwable exception, String message) {
    Sentry.captureException(exception);

    Logger logger = LogManager.getLogger();
    if (logger.isFatalEnabled()) {
      logger.fatal(message, exception);
    }
  }

}

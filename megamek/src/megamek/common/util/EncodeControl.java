/*
 * MegaMek - Copyright (C) 2000,2001,2002,2003,2004 Ben Mazur (bmazur@sev.org)
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

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.net.URL;
import java.net.URLConnection;
import java.security.AccessController;
import java.security.PrivilegedActionException;
import java.security.PrivilegedExceptionAction;
import java.util.Locale;
import java.util.PropertyResourceBundle;
import java.util.ResourceBundle;

/**
 * Created with IntelliJ IDEA.
 *
 * @author Xenon
 * @version 1
 *
 * Borrowed code from http://stackoverflow.com/questions/4659929/how-to-use-utf-8-in-resource-properties-with-resourcebundle
 * The issue was the Resource Bundle was reading properties files as ISO-8859-1 encodings. Thus special characters, like
 * those used in Russian, were being read wrong. The class below allows for a controller to read in any encoding
 * specified.
 * The actual overridden class has been copied here with the encoding change from the borrowed coded added.
 */
public class EncodeControl extends ResourceBundle.Control {
    public ResourceBundle newBundle(String baseName, Locale locale, String format, ClassLoader loader, boolean reload)
            throws IllegalAccessException, InstantiationException, IOException {
        String bundleName = this.toBundleName(baseName, locale);
        Object bundle = null;
        if (format.equals("java.class")) {
            try {
                Class<?> resourceName = loader.loadClass(bundleName);
                if (!ResourceBundle.class.isAssignableFrom(resourceName)) {
                    throw new ClassCastException(resourceName.getName() + " cannot be cast to ResourceBundle");
                }

                bundle = (ResourceBundle) resourceName.newInstance();
            } catch (ClassNotFoundException var19) {
                throw (IOException) var19.getException();
            }
        } else {
            if (!format.equals("java.properties")) {
                throw new IllegalArgumentException("unknown format: " + format);
            }

            final String resourceName1 = this.toResourceName0(bundleName, "properties");
            if (resourceName1 == null) {
                return (ResourceBundle) bundle;
            }

            final ClassLoader classLoader = loader;
            final boolean reloadFlag = reload;
            InputStream stream = null;

            try {
                stream = (InputStream) AccessController.doPrivileged(new PrivilegedExceptionAction<InputStream>() {
                    public InputStream run() throws IOException {
                        InputStream is = null;
                        if (reloadFlag) {
                            URL url = classLoader.getResource(resourceName1);
                            if (url != null) {
                                URLConnection connection = url.openConnection();
                                if (connection != null) {
                                    connection.setUseCaches(false);
                                    is = connection.getInputStream();
                                }
                            }
                        } else {
                            is = classLoader.getResourceAsStream(resourceName1);
                        }

                        return is;
                    }
                });
            } catch (PrivilegedActionException var18) {
                throw (IOException) var18.getException();
            }

            if (stream != null) {
                try(Reader reader = new InputStreamReader(stream, "UTF-8")) { //$NON-NLS-1$
                    // Only this line is changed to make it to read properties files as UTF-8 or other encodings.
                    bundle = new PropertyResourceBundle(reader);
                } finally {
                    stream.close();
                }
            }
        }

        return (ResourceBundle) bundle;
    }

    // Also borrowed from overridden class.
    private String toResourceName0(String bundleName, String suffix) {
        return bundleName.contains("://")?null:this.toResourceName(bundleName, suffix);
    }
}
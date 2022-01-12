/*
 * MegaMek - Copyright (C) 2005 Ben Mazur (bmazur@sev.org)
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

package megamek.common.preference;

import java.util.Enumeration;
import java.util.Properties;
import java.util.Vector;

class PreferenceStore implements IPreferenceStore {

    protected boolean dirty = false;

    protected Properties properties;
    protected Properties defaultProperties;

    protected Vector<IPreferenceChangeListener> listeners = new Vector<>();

    public PreferenceStore() {
        defaultProperties = new Properties();
        properties = new Properties(defaultProperties);
    }

    @Override
    public boolean getDefaultBoolean(String name) {
        return getBoolean(defaultProperties, name);
    }

    @Override
    public int getDefaultInt(String name) {
        return getInt(defaultProperties, name);
    }

    @Override
    public long getDefaultLong(String name) {
        return getLong(defaultProperties, name);
    }

    @Override
    public String getDefaultString(String name) {
        return getString(defaultProperties, name);
    }

    @Override
    public double getDefaultDouble(String name) {
        return getDouble(defaultProperties, name);
    }

    @Override
    public float getDefaultFloat(String name) {
        return getFloat(defaultProperties, name);
    }

    @Override
    public boolean getBoolean(String name) {
        return getBoolean(properties, name);
    }

    private boolean getBoolean(Properties p, String name) {
        final String value = p != null ? p.getProperty(name) : null;
        return (value == null) ? BOOLEAN_DEFAULT : value.equals(IPreferenceStore.TRUE);
    }

    @Override
    public double getDouble(String name) {
        return getDouble(properties, name);
    }

    private double getDouble(Properties p, String name) {
        String value = p != null ? p.getProperty(name) : null;
        if (value == null) {
            return DOUBLE_DEFAULT;
        }
        double ival = DOUBLE_DEFAULT;
        try {
            ival = Double.parseDouble(value);
        } catch (Exception ignored) {

        }
        return ival;
    }

    @Override
    public float getFloat(String name) {
        return getFloat(properties, name);
    }

    private float getFloat(Properties p, String name) {
        final String value = (p != null) ? p.getProperty(name) : null;
        if (value == null) {
            return FLOAT_DEFAULT;
        }
        float ival = FLOAT_DEFAULT;
        try {
            ival = Float.parseFloat(value);
        } catch (Exception ignored) {

        }
        return ival;
    }

    @Override
    public int getInt(String name) {
        return getInt(properties, name);
    }

    private int getInt(Properties p, String name) {
        String value = p != null ? p.getProperty(name) : null;
        if (value == null) {
            return INT_DEFAULT;
        }
        int ival = 0;
        try {
            ival = Integer.parseInt(value);
        } catch (Exception ignored) {

        }
        return ival;
    }

    @Override
    public long getLong(String name) {
        return getLong(properties, name);
    }

    private long getLong(Properties p, String name) {
        String value = (p != null) ? p.getProperty(name) : null;
        if (value == null) {
            return LONG_DEFAULT;
        }
        long ival = LONG_DEFAULT;
        try {
            ival = Long.parseLong(value);
        } catch (Exception ignored) {

        }
        return ival;
    }

    @Override
    public String getString(String name) {
        return getString(properties, name);
    }

    private String getString(Properties p, String name) {
        final String value = (p != null) ? p.getProperty(name) : null;
        return (value == null) ? STRING_DEFAULT : value;
    }

    @Override
    public void setDefault(String name, double value) {
        setValue(defaultProperties, name, value);
    }

    @Override
    public void setDefault(String name, float value) {
        setValue(defaultProperties, name, value);
    }

    @Override
    public void setDefault(String name, int value) {
        setValue(defaultProperties, name, value);
    }

    @Override
    public void setDefault(String name, long value) {
        setValue(defaultProperties, name, value);
    }

    @Override
    public void setDefault(String name, String value) {
        setValue(defaultProperties, name, value);
    }

    @Override
    public void setDefault(String name, boolean value) {
        setValue(defaultProperties, name, value);
    }

    @Override
    public void setValue(String name, double value) {
        double oldValue = getDouble(name);
        if (oldValue != value) {
            setValue(properties, name, value);
            dirty = true;
            firePropertyChangeEvent(name, oldValue, value);
        }
    }

    @Override
    public void setValue(String name, float value) {
        float oldValue = getFloat(name);
        if (oldValue != value) {
            setValue(properties, name, value);
            dirty = true;
            firePropertyChangeEvent(name, oldValue, value);
        }
    }

    @Override
    public void setValue(String name, int value) {
        int oldValue = getInt(name);
        if (oldValue != value) {
            setValue(properties, name, value);
            dirty = true;
            firePropertyChangeEvent(name, oldValue, value);
        }
    }

    @Override
    public void setValue(String name, long value) {
        long oldValue = getLong(name);
        if (oldValue != value) {
            setValue(properties, name, value);
            dirty = true;
            firePropertyChangeEvent(name, oldValue, value);
        }
    }

    @Override
    public void setValue(String name, String value) {
        String oldValue = getString(name);
        if (oldValue == null || !oldValue.equals(value)) {
            setValue(properties, name, value);
            dirty = true;
            firePropertyChangeEvent(name, oldValue, value);
        }
    }

    @Override
    public void setValue(String name, boolean value) {
        boolean oldValue = getBoolean(name);
        if (oldValue != value) {
            setValue(properties, name, value);
            dirty = true;
            firePropertyChangeEvent(name, oldValue, value);
        }
    }

    @Override
    public void putValue(String name, String value) {
        String oldValue = getString(name);
        if (oldValue == null || !oldValue.equals(value)) {
            setValue(properties, name, value);
            dirty = true;
        }
    }

    private void setValue(Properties p, String name, double value) {
        put(p, name, Double.toString(value));
    }

    private void setValue(Properties p, String name, float value) {
        put(p, name, Float.toString(value));
    }

    private void setValue(Properties p, String name, int value) {
        put(p, name, Integer.toString(value));
    }

    private void setValue(Properties p, String name, long value) {
        put(p, name, Long.toString(value));
    }

    private void setValue(Properties p, String name, String value) {
        put(p, name, value);
    }

    private void setValue(Properties p, String name, boolean value) {
        put(p, name, Boolean.toString(value));
    }

    protected void put(Properties p, String name, String value) {
        p.put(name, value);
    }

    @Override
    public void addPreferenceChangeListener(IPreferenceChangeListener listener) {
        if (!listeners.contains((listener))) {
            listeners.addElement(listener);
        }
    }

    @Override
    public void removePreferenceChangeListener(
            IPreferenceChangeListener listener) {
        listeners.removeElement(listener);
    }

    protected void firePropertyChangeEvent(String name, Object oldValue,
            Object newValue) {
        if (listeners.size() > 0
                && (oldValue == null || !oldValue.equals(newValue))) {
            final PreferenceChangeEvent pe = new PreferenceChangeEvent(this,
                    name, oldValue, newValue);
            for (int i = 0; i < listeners.size(); ++i) {
                IPreferenceChangeListener l = listeners.elementAt(i);
                l.preferenceChange(pe);
            }
        }
    }

    @Override
    public String[] getAdvancedProperties() {
        Vector<String> v = new Vector<>();
        String s;
        for (Enumeration<?> e = properties.propertyNames(); e.hasMoreElements();) {
            s = (String) e.nextElement();
            if (s.startsWith("Advanced")) {
                v.addElement(s);
            }
        }
        String[] props = new String[v.size()];
        for (int i = 0; i < v.size(); i++) {
            props[i] = v.elementAt(i);
        }
        return props;
    }
}

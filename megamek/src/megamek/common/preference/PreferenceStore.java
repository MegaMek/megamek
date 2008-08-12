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

    protected Vector<IPreferenceChangeListener> listeners = new Vector<IPreferenceChangeListener>();

    public PreferenceStore() {
        defaultProperties = new Properties();
        properties = new Properties(defaultProperties);
    }

    public boolean getDefaultBoolean(String name) {
        return getBoolean(defaultProperties, name);
    }

    public int getDefaultInt(String name) {
        return getInt(defaultProperties, name);
    }

    public long getDefaultLong(String name) {
        return getLong(defaultProperties, name);
    }

    public String getDefaultString(String name) {
        return getString(defaultProperties, name);
    }

    public double getDefaultDouble(String name) {
        return getDouble(defaultProperties, name);
    }

    public float getDefaultFloat(String name) {
        return getFloat(defaultProperties, name);
    }

    public boolean getBoolean(String name) {
        return getBoolean(properties, name);
    }

    private boolean getBoolean(Properties p, String name) {
        String value = p != null ? p.getProperty(name) : null;
        if (value == null)
            return BOOLEAN_DEFAULT;
        if (value.equals(IPreferenceStore.TRUE))
            return true;
        return false;
    }

    public double getDouble(String name) {
        return getDouble(properties, name);
    }

    private double getDouble(Properties p, String name) {
        String value = p != null ? p.getProperty(name) : null;
        if (value == null)
            return DOUBLE_DEFAULT;
        double ival = DOUBLE_DEFAULT;
        try {
            ival = new Double(value).doubleValue();
        } catch (NumberFormatException e) {
        }
        return ival;
    }

    public float getFloat(String name) {
        return getFloat(properties, name);
    }

    private float getFloat(Properties p, String name) {
        String value = p != null ? p.getProperty(name) : null;
        if (value == null)
            return FLOAT_DEFAULT;
        float ival = FLOAT_DEFAULT;
        try {
            ival = new Float(value).floatValue();
        } catch (NumberFormatException e) {
        }
        return ival;
    }

    public int getInt(String name) {
        return getInt(properties, name);
    }

    private int getInt(Properties p, String name) {
        String value = p != null ? p.getProperty(name) : null;
        if (value == null)
            return INT_DEFAULT;
        int ival = 0;
        try {
            ival = Integer.parseInt(value);
        } catch (NumberFormatException e) {
        }
        return ival;
    }

    public long getLong(String name) {
        return getLong(properties, name);
    }

    private long getLong(Properties p, String name) {
        String value = p != null ? p.getProperty(name) : null;
        if (value == null)
            return LONG_DEFAULT;
        long ival = LONG_DEFAULT;
        try {
            ival = Long.parseLong(value);
        } catch (NumberFormatException e) {
        }
        return ival;
    }

    public String getString(String name) {
        return getString(properties, name);
    }

    private String getString(Properties p, String name) {
        String value = p != null ? p.getProperty(name) : null;
        if (value == null)
            return STRING_DEFAULT;
        return value;
    }

    public void setDefault(String name, double value) {
        setValue(defaultProperties, name, value);
    }

    public void setDefault(String name, float value) {
        setValue(defaultProperties, name, value);
    }

    public void setDefault(String name, int value) {
        setValue(defaultProperties, name, value);
    }

    public void setDefault(String name, long value) {
        setValue(defaultProperties, name, value);
    }

    public void setDefault(String name, String value) {
        setValue(defaultProperties, name, value);
    }

    public void setDefault(String name, boolean value) {
        setValue(defaultProperties, name, value);
    }

    public void setValue(String name, double value) {
        double oldValue = getDouble(name);
        if (oldValue != value) {
            setValue(properties, name, value);
            dirty = true;
            firePropertyChangeEvent(name, new Double(oldValue), new Double(
                    value));
        }
    }

    public void setValue(String name, float value) {
        float oldValue = getFloat(name);
        if (oldValue != value) {
            setValue(properties, name, value);
            dirty = true;
            firePropertyChangeEvent(name, new Float(oldValue), new Float(value));
        }
    }

    public void setValue(String name, int value) {
        int oldValue = getInt(name);
        if (oldValue != value) {
            setValue(properties, name, value);
            dirty = true;
            firePropertyChangeEvent(name, new Integer(oldValue), new Integer(
                    value));
        }
    }

    public void setValue(String name, long value) {
        long oldValue = getLong(name);
        if (oldValue != value) {
            setValue(properties, name, value);
            dirty = true;
            firePropertyChangeEvent(name, new Long(oldValue), new Long(value));
        }
    }

    public void setValue(String name, String value) {
        String oldValue = getString(name);
        if (oldValue == null || !oldValue.equals(value)) {
            setValue(properties, name, value);
            dirty = true;
            firePropertyChangeEvent(name, oldValue, value);
        }
    }

    public void setValue(String name, boolean value) {
        boolean oldValue = getBoolean(name);
        if (oldValue != value) {
            setValue(properties, name, value);
            dirty = true;
            firePropertyChangeEvent(name, new Boolean(oldValue), new Boolean(
                    value));
        }
    }

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
        put(p, name, value == true ? IPreferenceStore.TRUE
                : IPreferenceStore.FALSE);
    }

    protected void put(Properties p, String name, String value) {
        p.put(name, value);
    }

    public void addPreferenceChangeListener(IPreferenceChangeListener listener) {
        if (!listeners.contains((listener))) {
            listeners.addElement(listener);
        }
    }

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

    public String[] getAdvancedProperties() {
        Vector<String> v = new Vector<String>();
        String s;
        for (Enumeration<?> e = properties.propertyNames(); e.hasMoreElements();) {
            s = (String) e.nextElement();
            if (s.startsWith("Advanced")) {
                v.addElement(s);
            }
        }
        String props[] = new String[v.size()];
        for (int i = 0; i < v.size(); i++) {
            props[i] = v.elementAt(i);
        }
        return props;
    }
}

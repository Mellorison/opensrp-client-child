package org.smartregister.child.util;

import java.util.Properties;

/**
 * Created by ndegwamartin on 2019-06-07.
 */
public class AppProperties extends Properties {

    public Boolean getPropertyBoolean(String key) {

        return Boolean.valueOf(getProperty(key));
    }

    public Boolean hasProperty(String key) {

        return getProperty(key) != null;
    }
}

/**
 *   Copyright (C) 2011-2012 Typesafe Inc. <http://typesafe.com>
 */
package com.gu.typesafe.config.impl;

import java.io.ObjectStreamException;
import java.io.Serializable;

import com.gu.typesafe.config.ConfigOrigin;
import com.gu.typesafe.config.ConfigValueType;

final class ConfigInt extends ConfigNumber implements Serializable {

    private static final long serialVersionUID = 2L;

    final private int value;

    ConfigInt(com.gu.typesafe.config.ConfigOrigin origin, int value, String originalText) {
        super(origin, originalText);
        this.value = value;
    }

    @Override
    public com.gu.typesafe.config.ConfigValueType valueType() {
        return ConfigValueType.NUMBER;
    }

    @Override
    public Integer unwrapped() {
        return value;
    }

    @Override
    String transformToString() {
        String s = super.transformToString();
        if (s == null)
            return Integer.toString(value);
        else
            return s;
    }

    @Override
    protected long longValue() {
        return value;
    }

    @Override
    protected double doubleValue() {
        return value;
    }

    @Override
    protected ConfigInt newCopy(ConfigOrigin origin) {
        return new ConfigInt(origin, value, originalText);
    }

    // serialization all goes through SerializedConfigValue
    private Object writeReplace() throws ObjectStreamException {
        return new SerializedConfigValue(this);
    }
}

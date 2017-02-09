// Copyright (C) 2017 Red Hat, Inc. and individual contributors as indicated by the @author tags.
// You may not use this file except in compliance with the Apache License, Version 2.0.

package io.agroal.api.security;

import java.io.Serializable;

/**
 * @author <a href="lbarreiro@redhat.com">Luis Barreiro</a>
 */
public class SimplePassword implements Serializable {

    private static final long serialVersionUID = 1;

    private String word;

    public SimplePassword(String password) {
        this.word = password;
    }

    public String getWord() {
        return word;
    }

    // --- //

    @Override
    public boolean equals(Object o) {
        if ( this == o ) {
            return true;
        }
        if ( o == null || !(o instanceof SimplePassword) ) {
            return false;
        }

        SimplePassword that = (SimplePassword) o;
        return word == null ? that.word == null : word.equals( that.word );
    }

    @Override
    public int hashCode() {
        return word == null ? 7 : word.hashCode();
    }

    @Override
    public String toString() {
        return "*** masked ***";
    }
}

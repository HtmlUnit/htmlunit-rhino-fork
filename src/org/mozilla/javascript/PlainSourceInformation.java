/* -*- Mode: java; tab-width: 8; indent-tabs-mode: nil; c-basic-offset: 4 -*-
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.javascript;

import java.math.BigInteger;

import org.mozilla.javascript.ast.FunctionNode;

/**
 * PlainSourceInformation.
 */
public class PlainSourceInformation implements SourceInformation {

    private String source;
    private int currentFunctionEndPos;

    public PlainSourceInformation(String sourceString) {
        source = sourceString;
    }

    @Override
    public void addToken(int token) {
    }

    @Override
    public void addEOL(int token) {
    }

    @Override
    public void addName(String str) {
    }

    @Override
    public void addString(String str) {
    }

    @Override
    public void addNumber(double n) {
    }

    @Override
    public void addTemplateLiteral(String str) {
    }

    @Override
    public void addRegexp(String regexp, String flags) {
    }

    @Override
    public void addBigInt(BigInteger n) {
    }

    @Override
    public int markFunctionStart(FunctionNode fn) {
        int absolutePosition = fn.getAbsolutePosition();
        currentFunctionEndPos = absolutePosition + fn.getLength();
        return absolutePosition;
    }

    @Override
    public int markFunctionEnd(int functionStart) {
        return currentFunctionEndPos;
    }

    @Override
    public int getCurrentOffset() {
        return 0;
    }

    @Override
    public String getEncodedSource() {
        return source;
    }
}

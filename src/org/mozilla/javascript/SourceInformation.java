/* -*- Mode: java; tab-width: 8; indent-tabs-mode: nil; c-basic-offset: 4 -*-
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.javascript;

import java.math.BigInteger;

import org.mozilla.javascript.ast.FunctionNode;

/**
 * The interface for the {@link Decompiler} and {@link PlainSourceInformation}
 */
public interface SourceInformation {

    void addToken(int token);

    void addEOL(int token);

    void addName(String str);

    void addString(String str);

    void addNumber(double n);

    void addTemplateLiteral(String str);

    void addRegexp(String regexp, String flags);

    void addBigInt(BigInteger n);

    int markFunctionStart(FunctionNode fn);

    int markFunctionEnd(int functionStart);

    int getCurrentOffset();

    String getEncodedSource();
}

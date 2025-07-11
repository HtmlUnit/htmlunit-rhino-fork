/* -*- Mode: java; tab-width: 8; indent-tabs-mode: nil; c-basic-offset: 4 -*-
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.javascript;

import static org.mozilla.javascript.ScriptableObject.DONTENUM;
import static org.mozilla.javascript.ScriptableObject.PERMANENT;
import static org.mozilla.javascript.ScriptableObject.READONLY;

import java.io.Serializable;
import org.mozilla.javascript.xml.XMLLib;

/**
 * This class implements the global native object (function and value properties only).
 *
 * <p>See ECMA 15.1.[12].
 *
 * @author Mike Shaver
 */
public class NativeGlobal implements Serializable {
    static final long serialVersionUID = 6080442165748707530L;

    // HtmlUnit
    private static abstract class SerializableConstructableWithInjectableCtor implements SerializableConstructable {
        // We need a reference to the LambdaFunction, so we use this "lateBound"
        // trick. It ain't great, but it's the only idea I have.
        Function lateBoundCtor;
    }
    // HtmlUnit

    public static void init(Context cx, Scriptable scope, boolean sealed) {
        defineGlobalFunction(
                scope,
                sealed,
                "decodeURI",
                1,
                (callCx, callScope, thisObj, args) -> js_decodeURI(args));
        defineGlobalFunction(
                scope,
                sealed,
                "decodeURIComponent",
                1,
                (callCx, callScope, thisObj, args) -> js_decodeURIComponent(args));
        defineGlobalFunction(
                scope,
                sealed,
                "encodeURI",
                1,
                (callCx, callScope, thisObj, args) -> js_encodeURI(args));
        defineGlobalFunction(
                scope,
                sealed,
                "encodeURIComponent",
                1,
                (callCx, callScope, thisObj, args) -> js_encodeURIComponent(args));
        defineGlobalFunction(
                scope, sealed, "escape", 1, (callCx, callScope, thisObj, args) -> js_escape(args));
        defineGlobalFunction(
                scope,
                sealed,
                "isFinite",
                1,
                (callCx, callScope, thisObj, args) -> js_isFinite(args));
        defineGlobalFunction(
                scope, sealed, "isNaN", 1, (callCx, callScope, thisObj, args) -> js_isNaN(args));
        defineGlobalFunction(
                scope,
                sealed,
                "isXMLName",
                1,
                (callCx, callScope, thisObj, args) -> js_isXMLName(callCx, callScope, args));
        defineGlobalFunction(
                scope,
                sealed,
                "parseFloat",
                1,
                (callCx, callScope, thisObj, args) -> js_parseFloat(args));
        defineGlobalFunction(
                scope,
                sealed,
                "parseInt",
                2,
                (callCx, callScope, thisObj, args) -> js_parseInt(callCx, args));
        defineGlobalFunction(
                scope,
                sealed,
                "unescape",
                1,
                (callCx, callScope, thisObj, args) -> js_unescape(args));
        defineGlobalFunction(
                scope,
                sealed,
                "uneval",
                1,
                (callCx, callScope, thisObj, args) -> js_uneval(callCx, callScope, args));
        defineGlobalFunctionEval(scope, sealed);

        ScriptableObject.defineProperty(
                scope, "NaN", ScriptRuntime.NaNobj, READONLY | DONTENUM | PERMANENT);
        ScriptableObject.defineProperty(
                scope, "Infinity", Double.POSITIVE_INFINITY, READONLY | DONTENUM | PERMANENT);
        ScriptableObject.defineProperty(
                scope, "undefined", Undefined.instance, READONLY | DONTENUM | PERMANENT);
        ScriptableObject.defineProperty(scope, "globalThis", scope, DONTENUM);

        /*
            Each error constructor gets its own Error object as a prototype,
            with the 'name' property set to the name of the error.
        */
        Scriptable nativeError =
                ScriptableObject.ensureScriptable(ScriptableObject.getProperty(scope, "Error"));
        Scriptable nativeErrorProto =
                ScriptableObject.ensureScriptable(
                        ScriptableObject.getProperty(nativeError, "prototype"));

        for (TopLevel.NativeErrors error : TopLevel.NativeErrors.values()) {
            if (error == TopLevel.NativeErrors.Error) {
                // Error is initialized elsewhere and we should not overwrite it.
                continue;
            }
            String name = error.name();
            Scriptable topLevelScope = ScriptableObject.getTopLevelScope(scope);
            Function builtinErrorCtor =
                    TopLevel.getBuiltinCtor(cx, topLevelScope, TopLevel.Builtins.Error);
            ScriptableObject errorProto = NativeError.makeProto(topLevelScope, builtinErrorCtor);
            errorProto.defineProperty("name", name, DONTENUM);
            errorProto.defineProperty("message", "", DONTENUM);

            BaseFunction ctor;

            // Building errors is complex because of the prototype chain requirements. This is a bit
            // arcane, but it's a combination that makes test262 happy.
            if (error == TopLevel.NativeErrors.AggregateError) {
                SerializableConstructableWithInjectableCtor target =
                        new SerializableConstructableWithInjectableCtor() {
                            @Override
                            public Scriptable construct(
                                    Context callCx, Scriptable callScope, Object[] args) {
                                return NativeError.makeAggregate(
                                        callCx, callScope, lateBoundCtor, args);
                            }
                        };
                ctor =
                        new LambdaConstructor(scope, name, 2, target) {
                            // Necessary to make the test262 case
                            // built-ins/NativeErrors/AggregateError/newtarget-proto-custom.js work
                            // correctly
                            @Override
                            public Scriptable createObject(Context cx, Scriptable scope) {
                                return null;
                            }
                        };
                target.lateBoundCtor = ctor;
            } else {
                SerializableConstructableWithInjectableCtor target =
                        new SerializableConstructableWithInjectableCtor() {
                            @Override
                            public Scriptable construct(
                                    Context callCx, Scriptable callScope, Object[] args) {
                                return NativeError.make(callCx, callScope, lateBoundCtor, args);
                            }
                        };
                ctor = new LambdaConstructor(scope, name, 1, target);
                target.lateBoundCtor = ctor;
            }

            ctor.setImmunePrototypeProperty(errorProto);
            ctor.setPrototype(nativeError);
            errorProto.put("constructor", errorProto, ctor);
            errorProto.setAttributes("constructor", ScriptableObject.DONTENUM);
            errorProto.setPrototype(nativeErrorProto);
            ctor.setAttributes("name", DONTENUM | READONLY);
            ctor.setAttributes("length", DONTENUM | READONLY);
            if (sealed) {
                errorProto.sealObject();
                ctor.sealObject();
            }

            ScriptableObject.defineProperty(scope, name, ctor, DONTENUM);
        }
    }

    private static void defineGlobalFunction(
            Scriptable scope,
            boolean sealed,
            String name,
            int length,
            SerializableCallable callable) {
        LambdaFunction fun = new LambdaFunction(scope, name, length, null, callable);
        registerGlobalFunction(scope, sealed, name, fun);
    }

    private static void registerGlobalFunction(
            Scriptable scope, boolean sealed, String name, LambdaFunction fun) {
        ScriptableObject.defineProperty(scope, name, fun, DONTENUM);
        if (sealed) {
            fun.sealObject();
        }
    }

    // Eval is special because we need to "recognize" it in isEvalFunction
    private static void defineGlobalFunctionEval(Scriptable scope, boolean sealed) {
        LambdaFunction evalFun = new EvalLambdaFunction(scope);
        registerGlobalFunction(scope, sealed, "eval", evalFun);
    }

    static boolean isEvalFunction(Object functionObj) {
        return functionObj instanceof EvalLambdaFunction;
    }

    private static String js_uneval(Context cx, Scriptable scope, Object[] args) {
        Object value = (args.length != 0) ? args[0] : Undefined.instance;
        return ScriptRuntime.uneval(cx, scope, value);
    }

    private static Boolean js_isXMLName(Context cx, Scriptable scope, Object[] args) {
        Object name = (args.length == 0) ? Undefined.instance : args[0];
        XMLLib xmlLib = XMLLib.extractFromScope(scope);
        return xmlLib.isXMLName(cx, name);
    }

    private static Boolean js_isNaN(Object[] args) {
        // The global method isNaN, as per ECMA-262 15.1.2.6.
        if (args.length < 1) {
            return true;
        } else {
            double d = ScriptRuntime.toNumber(args[0]);
            return Double.isNaN(d);
        }
    }

    private static Object js_isFinite(Object[] args) {
        if (args.length < 1) {
            return Boolean.FALSE;
        }
        return NativeNumber.isFinite(args[0]);
    }

    private static String js_decodeURI(Object[] args) {
        String str = ScriptRuntime.toString(args, 0);
        return decode(str, true);
    }

    private static String js_decodeURIComponent(Object[] args) {
        String str = ScriptRuntime.toString(args, 0);
        return decode(str, false);
    }

    private static String js_encodeURI(Object[] args) {
        String str = ScriptRuntime.toString(args, 0);
        return encode(str, true);
    }

    private static String js_encodeURIComponent(Object[] args) {
        String str = ScriptRuntime.toString(args, 0);
        return encode(str, false);
    }

    /** The global method parseInt, as per ECMA-262 15.1.2.2. */
    static Object js_parseInt(Context cx, Object[] args) {
        String s = ScriptRuntime.toString(args, 0);
        int radix = ScriptRuntime.toInt32(args, 1);

        int len = s.length();
        if (len == 0) return ScriptRuntime.NaNobj;

        boolean negative = false;
        int start = 0;
        char c;
        do {
            c = s.charAt(start);
            if (!ScriptRuntime.isStrWhiteSpaceChar(c)) break;
            start++;
        } while (start < len);

        if (c == '+' || (negative = (c == '-'))) start++;

        final int NO_RADIX = -1;
        if (radix == 0) {
            radix = NO_RADIX;
        } else if (radix < 2 || radix > 36) {
            return ScriptRuntime.NaNobj;
        } else if (radix == 16 && len - start > 1 && s.charAt(start) == '0') {
            c = s.charAt(start + 1);
            if (c == 'x' || c == 'X') start += 2;
        }

        if (radix == NO_RADIX) {
            radix = 10;
            if (len - start > 1 && s.charAt(start) == '0') {
                c = s.charAt(start + 1);
                if (c == 'x' || c == 'X') {
                    radix = 16;
                    start += 2;
                } else if ('0' <= c && c <= '9') {
                    if (cx == null || cx.getLanguageVersion() < Context.VERSION_1_5) {
                        radix = 8;
                        start++;
                    }
                }
            }
        }

        double d = ScriptRuntime.stringPrefixToNumber(s, start, radix);
        return negative ? -d : d;
    }

    /**
     * The global method parseFloat, as per ECMA-262 15.1.2.3.
     *
     * @param args the arguments to parseFloat, ignoring args[>=1]
     */
    static Object js_parseFloat(Object[] args) {
        if (args.length < 1) return ScriptRuntime.NaNobj;

        String s = ScriptRuntime.toString(args[0]);
        int len = s.length();
        int start = 0;
        // Scan forward to skip whitespace
        char c;
        for (; ; ) {
            if (start == len) {
                return ScriptRuntime.NaNobj;
            }
            c = s.charAt(start);
            if (!ScriptRuntime.isStrWhiteSpaceChar(c)) {
                break;
            }
            ++start;
        }

        int i = start;
        if (c == '+' || c == '-') {
            ++i;
            if (i == len) {
                return ScriptRuntime.NaNobj;
            }
            c = s.charAt(i);
        }

        if (c == 'I') {
            // check for "Infinity"
            if (i + 8 <= len && s.regionMatches(i, "Infinity", 0, 8)) {
                if (s.charAt(start) == '-') {
                    return Double.NEGATIVE_INFINITY;
                } else {
                    return Double.POSITIVE_INFINITY;
                }
            }
            return ScriptRuntime.NaNobj;
        }

        // Find the end of the legal bit
        int decimal = -1;
        int exponent = -1;
        boolean exponentValid = false;
        for (; i < len; i++) {
            switch (s.charAt(i)) {
                case '.':
                    if (decimal != -1) // Only allow a single decimal point.
                    break;
                    decimal = i;
                    continue;

                case 'e':
                case 'E':
                    if (exponent != -1) {
                        break;
                    } else if (i == len - 1) {
                        break;
                    }
                    exponent = i;
                    continue;

                case '+':
                case '-':
                    // Only allow '+' or '-' after 'e' or 'E'
                    if (exponent != i - 1) {
                        break;
                    } else if (i == len - 1) {
                        --i;
                        break;
                    }
                    continue;

                case '0':
                case '1':
                case '2':
                case '3':
                case '4':
                case '5':
                case '6':
                case '7':
                case '8':
                case '9':
                    if (exponent != -1) {
                        exponentValid = true;
                    }
                    continue;

                default:
                    break;
            }
            break;
        }
        if (exponent != -1 && !exponentValid) {
            i = exponent;
        }
        s = s.substring(start, i);
        try {
            return Double.valueOf(s);
        } catch (NumberFormatException ex) {
            return ScriptRuntime.NaNobj;
        }
    }

    /**
     * The global method escape, as per ECMA-262 15.1.2.4.
     *
     * <p>Includes code for the 'mask' argument supported by the C escape method, which used to be
     * part of the browser embedding. Blame for the strange constant names should be directed there.
     */
    private static Object js_escape(Object[] args) {
        final int URL_XALPHAS = 1, URL_XPALPHAS = 2, URL_PATH = 4;

        String s = ScriptRuntime.toString(args, 0);

        int mask = URL_XALPHAS | URL_XPALPHAS | URL_PATH;
        if (args.length > 1) { // the 'mask' argument.  Non-ECMA.
            double d = ScriptRuntime.toNumber(args[1]);
            if (Double.isNaN(d)
                    || ((mask = (int) d) != d)
                    || 0 != (mask & ~(URL_XALPHAS | URL_XPALPHAS | URL_PATH))) {
                throw Context.reportRuntimeErrorById("msg.bad.esc.mask");
            }
        }

        StringBuilder sb = null;
        for (int k = 0, L = s.length(); k != L; ++k) {
            int c = s.charAt(k);
            if (mask != 0
                    && ((c >= '0' && c <= '9')
                            || (c >= 'A' && c <= 'Z')
                            || (c >= 'a' && c <= 'z')
                            || c == '@'
                            || c == '*'
                            || c == '_'
                            || c == '-'
                            || c == '.'
                            || (0 != (mask & URL_PATH) && (c == '/' || c == '+')))) {
                if (sb != null) {
                    sb.append((char) c);
                }
            } else {
                if (sb == null) {
                    sb = new StringBuilder(L + 3);
                    sb.append(s);
                    sb.setLength(k);
                }

                int hexSize;
                if (c < 256) {
                    if (c == ' ' && mask == URL_XPALPHAS) {
                        sb.append('+');
                        continue;
                    }
                    sb.append('%');
                    hexSize = 2;
                } else {
                    sb.append('%');
                    sb.append('u');
                    hexSize = 4;
                }

                // append hexadecimal form of c left-padded with 0
                for (int shift = (hexSize - 1) * 4; shift >= 0; shift -= 4) {
                    int digit = 0xf & (c >> shift);
                    int hc = (digit < 10) ? '0' + digit : 'A' - 10 + digit;
                    sb.append((char) hc);
                }
            }
        }

        return (sb == null) ? s : sb.toString();
    }

    /** The global unescape method, as per ECMA-262 15.1.2.5. */
    private static Object js_unescape(Object[] args) {
        String s = ScriptRuntime.toString(args, 0);
        int firstEscapePos = s.indexOf('%');
        if (firstEscapePos >= 0) {
            int L = s.length();
            char[] buf = s.toCharArray();
            int destination = firstEscapePos;
            for (int k = firstEscapePos; k != L; ) {
                char c = buf[k];
                ++k;
                if (c == '%' && k != L) {
                    int end, start;
                    if (buf[k] == 'u') {
                        start = k + 1;
                        end = k + 5;
                    } else {
                        start = k;
                        end = k + 2;
                    }
                    if (end <= L) {
                        int x = 0;
                        for (int i = start; i != end; ++i) {
                            x = Kit.xDigitToInt(buf[i], x);
                        }
                        if (x >= 0) {
                            c = (char) x;
                            k = end;
                        }
                    }
                }
                buf[destination] = c;
                ++destination;
            }
            s = new String(buf, 0, destination);
        }
        return s;
    }

    /**
     * This is an indirect call to eval, and thus uses the global environment. Direct calls are
     * executed via ScriptRuntime.callSpecial().
     */
    private static Object js_eval(Context cx, Scriptable scope, Object[] args) {
        Scriptable global = ScriptableObject.getTopLevelScope(scope);
        return ScriptRuntime.evalSpecial(cx, global, global, args, "eval code", 1);
    }

    /**
     * @deprecated Use {@link ScriptRuntime#constructError(String,String)} instead.
     */
    @Deprecated
    public static EcmaError constructError(
            Context cx, String error, String message, Scriptable scope) {
        return ScriptRuntime.constructError(error, message);
    }

    /**
     * @deprecated Use {@link ScriptRuntime#constructError(String,String,String,int,String,int)}
     *     instead.
     */
    @Deprecated
    public static EcmaError constructError(
            Context cx,
            String error,
            String message,
            Scriptable scope,
            String sourceName,
            int lineNumber,
            int columnNumber,
            String lineSource) {
        return ScriptRuntime.constructError(
                error, message,
                sourceName, lineNumber,
                lineSource, columnNumber);
    }

    /*
     *   ECMA 3, 15.1.3 URI Handling Function Properties
     *
     *   The following are implementations of the algorithms
     *   given in the ECMA specification for the hidden functions
     *   'Encode' and 'Decode'.
     */
    private static String encode(String str, boolean fullUri) {
        byte[] utf8buf = null;
        StringBuilder sb = null;

        for (int k = 0, length = str.length(); k != length; ++k) {
            char C = str.charAt(k);
            if (encodeUnescaped(C, fullUri)) {
                if (sb != null) {
                    sb.append(C);
                }
            } else {
                if (sb == null) {
                    sb = new StringBuilder(length + 3);
                    sb.append(str);
                    sb.setLength(k);
                    utf8buf = new byte[6];
                }
                if (0xDC00 <= C && C <= 0xDFFF) {
                    throw uriError();
                }
                int V;
                if (C < 0xD800 || 0xDBFF < C) {
                    V = C;
                } else {
                    k++;
                    if (k == length) {
                        throw uriError();
                    }
                    char C2 = str.charAt(k);
                    if (!(0xDC00 <= C2 && C2 <= 0xDFFF)) {
                        throw uriError();
                    }
                    V = ((C - 0xD800) << 10) + (C2 - 0xDC00) + 0x10000;
                }
                int L = oneUcs4ToUtf8Char(utf8buf, V);
                for (int j = 0; j < L; j++) {
                    int d = 0xff & utf8buf[j];
                    sb.append('%');
                    sb.append(toHexChar(d >>> 4));
                    sb.append(toHexChar(d & 0xf));
                }
            }
        }
        return (sb == null) ? str : sb.toString();
    }

    private static char toHexChar(int i) {
        if (i >> 4 != 0) Kit.codeBug();
        return (char) ((i < 10) ? i + '0' : i - 10 + 'A');
    }

    private static int unHex(char c) {
        if ('A' <= c && c <= 'F') {
            return c - 'A' + 10;
        } else if ('a' <= c && c <= 'f') {
            return c - 'a' + 10;
        } else if ('0' <= c && c <= '9') {
            return c - '0';
        } else {
            return -1;
        }
    }

    private static int unHex(char c1, char c2) {
        int i1 = unHex(c1);
        int i2 = unHex(c2);
        if (i1 >= 0 && i2 >= 0) {
            return (i1 << 4) | i2;
        }
        return -1;
    }

    private static String decode(String str, boolean fullUri) {
        char[] buf = null;
        int bufTop = 0;

        for (int k = 0, length = str.length(); k != length; ) {
            char C = str.charAt(k);
            if (C != '%') {
                if (buf != null) {
                    buf[bufTop++] = C;
                }
                ++k;
            } else {
                if (buf == null) {
                    // decode always compress so result can not be bigger then
                    // str.length()
                    buf = new char[length];
                    str.getChars(0, k, buf, 0);
                    bufTop = k;
                }
                int start = k;
                if (k + 3 > length) throw uriError();
                int B = unHex(str.charAt(k + 1), str.charAt(k + 2));
                if (B < 0) throw uriError();
                k += 3;
                if ((B & 0x80) == 0) {
                    C = (char) B;
                } else {
                    // Decode UTF-8 sequence into ucs4Char and encode it into
                    // UTF-16
                    int utf8Tail, ucs4Char, minUcs4Char;
                    if ((B & 0xC0) == 0x80) {
                        // First  UTF-8 should be ouside 0x80..0xBF
                        throw uriError();
                    } else if ((B & 0x20) == 0) {
                        utf8Tail = 1;
                        ucs4Char = B & 0x1F;
                        minUcs4Char = 0x80;
                    } else if ((B & 0x10) == 0) {
                        utf8Tail = 2;
                        ucs4Char = B & 0x0F;
                        minUcs4Char = 0x800;
                    } else if ((B & 0x08) == 0) {
                        utf8Tail = 3;
                        ucs4Char = B & 0x07;
                        minUcs4Char = 0x10000;
                    } else if ((B & 0x04) == 0) {
                        utf8Tail = 4;
                        ucs4Char = B & 0x03;
                        minUcs4Char = 0x200000;
                    } else if ((B & 0x02) == 0) {
                        utf8Tail = 5;
                        ucs4Char = B & 0x01;
                        minUcs4Char = 0x4000000;
                    } else {
                        // First UTF-8 can not be 0xFF or 0xFE
                        throw uriError();
                    }
                    if (k + 3 * utf8Tail > length) throw uriError();
                    for (int j = 0; j != utf8Tail; j++) {
                        if (str.charAt(k) != '%') throw uriError();
                        B = unHex(str.charAt(k + 1), str.charAt(k + 2));
                        if (B < 0 || (B & 0xC0) != 0x80) throw uriError();
                        ucs4Char = (ucs4Char << 6) | (B & 0x3F);
                        k += 3;
                    }
                    // Check for overlongs and other should-not-present codes
                    if (ucs4Char < minUcs4Char || (ucs4Char >= 0xD800 && ucs4Char <= 0xDFFF)) {
                        ucs4Char = INVALID_UTF8;
                    } else if (ucs4Char == 0xFFFE || ucs4Char == 0xFFFF) {
                        ucs4Char = 0xFFFD;
                    }
                    if (ucs4Char >= 0x10000) {
                        ucs4Char -= 0x10000;
                        if (ucs4Char > 0xFFFFF) {
                            throw uriError();
                        }
                        char H = (char) ((ucs4Char >>> 10) + 0xD800);
                        C = (char) ((ucs4Char & 0x3FF) + 0xDC00);
                        buf[bufTop++] = H;
                    } else {
                        C = (char) ucs4Char;
                    }
                }
                if (fullUri && URI_DECODE_RESERVED.indexOf(C) >= 0) {
                    for (int x = start; x != k; x++) {
                        buf[bufTop++] = str.charAt(x);
                    }
                } else {
                    buf[bufTop++] = C;
                }
            }
        }
        return (buf == null) ? str : new String(buf, 0, bufTop);
    }

    private static boolean encodeUnescaped(char c, boolean fullUri) {
        if (('A' <= c && c <= 'Z') || ('a' <= c && c <= 'z') || ('0' <= c && c <= '9')) {
            return true;
        }
        if ("-_.!~*'()".indexOf(c) >= 0) {
            return true;
        }
        if (fullUri) {
            return URI_DECODE_RESERVED.indexOf(c) >= 0;
        }
        return false;
    }

    private static EcmaError uriError() {
        return ScriptRuntime.constructError(
                "URIError", ScriptRuntime.getMessageById("msg.bad.uri"));
    }

    private static final String URI_DECODE_RESERVED = ";/?:@&=+$,#";
    private static final int INVALID_UTF8 = Integer.MAX_VALUE;

    /* Convert one UCS-4 char and write it into a UTF-8 buffer, which must be
     * at least 6 bytes long.  Return the number of UTF-8 bytes of data written.
     */
    private static int oneUcs4ToUtf8Char(byte[] utf8Buffer, int ucs4Char) {
        int utf8Length = 1;

        // JS_ASSERT(ucs4Char <= 0x7FFFFFFF);
        if ((ucs4Char & ~0x7F) == 0) utf8Buffer[0] = (byte) ucs4Char;
        else {
            int i;
            int a = ucs4Char >>> 11;
            utf8Length = 2;
            while (a != 0) {
                a >>>= 5;
                utf8Length++;
            }
            i = utf8Length;
            while (--i > 0) {
                utf8Buffer[i] = (byte) ((ucs4Char & 0x3F) | 0x80);
                ucs4Char >>>= 6;
            }
            utf8Buffer[0] = (byte) (0x100 - (1 << (8 - utf8Length)) + ucs4Char);
        }
        return utf8Length;
    }

    /**
     * A simple subclass of {@link LambdaFunction} used to "tag" eval, so that we can recognize it
     * in {@link NativeGlobal#isEvalFunction}
     */
    private static class EvalLambdaFunction extends LambdaFunction {
        public EvalLambdaFunction(Scriptable scope) {
            super(
                    scope,
                    "eval",
                    1,
                    null,
                    (callCx, callScope, thisObj, args) ->
                            NativeGlobal.js_eval(callCx, callScope, args));
        }
    }
}

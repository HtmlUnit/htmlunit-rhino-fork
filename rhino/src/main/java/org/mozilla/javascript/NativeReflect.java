/* -*- Mode: java; tab-width: 4; indent-tabs-mode: 1; c-basic-offset: 4 -*-
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.javascript;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import org.mozilla.javascript.ScriptRuntime.StringIdOrIndex;

/**
 * This class implements the Reflect object.
 *
 * @author Ronald Brill
 * @author Lai Quang Duong
 */
final class NativeReflect extends ScriptableObject {
    private static final long serialVersionUID = 2920773905356325445L;

    private static final String REFLECT_TAG = "Reflect";

    public static Object init(Context cx, Scriptable scope, boolean sealed) {
        NativeReflect reflect = new NativeReflect();
        reflect.setPrototype(getObjectPrototype(scope));
        reflect.setParentScope(scope);

        reflect.defineBuiltinProperty(scope, "apply", 3, NativeReflect::apply);
        reflect.defineBuiltinProperty(scope, "construct", 2, NativeReflect::construct);
        reflect.defineBuiltinProperty(scope, "defineProperty", 3, NativeReflect::defineProperty);
        reflect.defineBuiltinProperty(scope, "deleteProperty", 2, NativeReflect::deleteProperty);
        reflect.defineBuiltinProperty(scope, "get", 2, NativeReflect::get);
        reflect.defineBuiltinProperty(
                scope, "getOwnPropertyDescriptor", 2, NativeReflect::getOwnPropertyDescriptor);
        reflect.defineBuiltinProperty(scope, "getPrototypeOf", 1, NativeReflect::getPrototypeOf);
        reflect.defineBuiltinProperty(scope, "has", 2, NativeReflect::has);
        reflect.defineBuiltinProperty(scope, "isExtensible", 1, NativeReflect::isExtensible);
        reflect.defineBuiltinProperty(scope, "ownKeys", 1, NativeReflect::ownKeys);
        reflect.defineBuiltinProperty(
                scope, "preventExtensions", 1, NativeReflect::preventExtensions);
        reflect.defineBuiltinProperty(scope, "set", 3, NativeReflect::set);
        reflect.defineBuiltinProperty(scope, "setPrototypeOf", 2, NativeReflect::setPrototypeOf);

        reflect.defineProperty(SymbolKey.TO_STRING_TAG, REFLECT_TAG, DONTENUM | READONLY);
        if (sealed) {
            reflect.sealObject();
        }
        return reflect;
    }

    private NativeReflect() {}

    @Override
    public String getClassName() {
        return "Reflect";
    }

    private static Object apply(Context cx, Scriptable scope, Scriptable thisObj, Object[] args) {
        if (args.length < 3) {
            throw ScriptRuntime.typeErrorById(
                    "msg.method.missing.parameter",
                    "Reflect.apply",
                    "3",
                    Integer.toString(args.length));
        }

        Scriptable callable = ScriptableObject.ensureScriptable(args[0]);

        if (args[1] instanceof Scriptable) {
            thisObj = (Scriptable) args[1];
        } else if (ScriptRuntime.isPrimitive(args[1])) {
            thisObj = cx.newObject(scope, "Object", new Object[] {args[1]});
        }

        if (ScriptRuntime.isSymbol(args[2])) {
            throw ScriptRuntime.typeErrorById("msg.arg.not.object", ScriptRuntime.typeof(args[2]));
        }
        ScriptableObject argumentsList = ScriptableObject.ensureScriptableObject(args[2]);

        return ScriptRuntime.applyOrCall(
                true, cx, scope, callable, new Object[] {thisObj, argumentsList});
    }

    /**
     * see <a href="https://262.ecma-international.org/12.0/#sec-reflect.construct">28.1.2
     * Reflect.construct (target, argumentsList[, newTarget])</a>
     */
    private static Scriptable construct(
            Context cx, Scriptable scope, Scriptable thisObj, Object[] args) {
        /*
         * 1. If IsConstructor(target) is false, throw a TypeError exception.
         * 2. If newTarget is not present, set newTarget to target.
         * 3. Else if IsConstructor(newTarget) is false, throw a TypeError exception.
         * 4. Let args be ? CreateListFromArrayLike(argumentsList).
         * 5. Return ? Construct(target, args, newTarget).
         */
        if (args.length < 1) {
            throw ScriptRuntime.typeErrorById(
                    "msg.method.missing.parameter",
                    "Reflect.construct",
                    "3",
                    Integer.toString(args.length));
        }

        if (!AbstractEcmaObjectOperations.isConstructor(cx, args[0])) {
            throw ScriptRuntime.typeErrorById("msg.not.ctor", ScriptRuntime.typeof(args[0]));
        }

        Constructable ctor = (Constructable) args[0];
        if (args.length < 2) {
            return ctor.construct(cx, scope, ScriptRuntime.emptyArgs);
        }

        if (args.length > 2 && !AbstractEcmaObjectOperations.isConstructor(cx, args[2])) {
            throw ScriptRuntime.typeErrorById("msg.not.ctor", ScriptRuntime.typeof(args[2]));
        }

        Object[] callArgs = ScriptRuntime.getApplyArguments(cx, args[1]);

        Object newTargetPrototype = null;
        if (args.length > 2) {
            Scriptable newTarget = ScriptableObject.ensureScriptable(args[2]);

            if (newTarget instanceof BaseFunction) {
                newTargetPrototype = ((BaseFunction) newTarget).getPrototypeProperty();
            } else {
                newTargetPrototype = newTarget.get("prototype", newTarget);
            }

            if (!(newTargetPrototype instanceof Scriptable)
                    || ScriptRuntime.isSymbol(newTargetPrototype)
                    || Undefined.isUndefined(newTargetPrototype)) {
                newTargetPrototype = null;
            }
        }

        // our Constructable interface does not support the newTarget;
        // therefore we use a cloned implementation that fixes
        // the prototype before executing call(..).
        if (ctor instanceof BaseFunction && newTargetPrototype != null) {
            BaseFunction ctorBaseFunction = (BaseFunction) ctor;
            Scriptable result = ctorBaseFunction.createObject(cx, scope);
            if (result != null) {
                result.setPrototype((Scriptable) newTargetPrototype);

                // LambdaConstructor could be non-callable (requires "new") or
                // non-constructable (no "new"). Check its flag to decide.
                if (ctorBaseFunction instanceof LambdaConstructor
                        && ((LambdaConstructor) ctorBaseFunction).isConstructable()) {
                    Scriptable newScriptable = ctorBaseFunction.construct(cx, scope, callArgs);
                    newScriptable.setPrototype((Scriptable) newTargetPrototype);
                    return newScriptable;
                }

                Object val = ctorBaseFunction.call(cx, scope, result, callArgs);
                if (val instanceof Scriptable) {
                    return (Scriptable) val;
                }

                return result;
            }
        }

        Scriptable newScriptable = ctor.construct(cx, scope, callArgs);
        if (newTargetPrototype != null) {
            newScriptable.setPrototype((Scriptable) newTargetPrototype);
        }

        return newScriptable;
    }

    private static Object defineProperty(
            Context cx, Scriptable scope, Scriptable thisObj, Object[] args) {
        if (args.length < 3) {
            throw ScriptRuntime.typeErrorById(
                    "msg.method.missing.parameter",
                    "Reflect.defineProperty",
                    "3",
                    Integer.toString(args.length));
        }

        ScriptableObject target = checkTarget(args);
        DescriptorInfo desc = new DescriptorInfo(ScriptableObject.ensureScriptableObject(args[2]));

        Object key = args[1];

        try {
            if (key instanceof Symbol) {
                return target.defineOwnProperty(cx, key, desc);
            } else {
                String propertyKey =
                        ScriptRuntime.toString(
                                ScriptRuntime.toPrimitive(key, ScriptRuntime.StringClass));
                return target.defineOwnProperty(cx, propertyKey, desc);
            }

        } catch (EcmaError e) {
            return false;
        }
    }

    private static Object deleteProperty(
            Context cx, Scriptable scope, Scriptable thisObj, Object[] args) {
        ScriptableObject target = checkTarget(args);

        if (args.length > 1) {
            if (ScriptRuntime.isSymbol(args[1])) {
                return ScriptableObject.deleteProperty(target, (Symbol) args[1]);
            }
            return ScriptableObject.deleteProperty(target, ScriptRuntime.toString(args[1]));
        }

        return false;
    }

    /*
     * https://tc39.es/ecma262/#sec-reflect.get
     * 1. If target is not an Object, throw a TypeError exception.
     * 2. Let key be ? ToPropertyKey(propertyKey).
     * 3. If receiver is not present, then
     *        a. Set receiver to target.
     * 4. Return ? target.[[Get]](key, receiver).
     */
    private static Object get(Context cx, Scriptable scope, Scriptable thisObj, Object[] args) {
        final ScriptableObject target = checkTarget(args);
        final Object propertyKey = args.length > 1 ? args[1] : Undefined.instance;
        final Object receiver = args.length > 2 ? args[2] : target;

        // If target is a proxy, delegate to the proxy handler
        if (target instanceof NativeProxy) {
            final NativeProxy proxy = (NativeProxy) target;
            final Function trap = proxy.getTrap("get");
            if (trap != null) {
                final ScriptableObject proxyTarget = proxy.getTargetThrowIfRevoked();
                final Object[] trapArgs = {proxyTarget, propertyKey, receiver};
                final Object trapResult = proxy.callTrap(trap, trapArgs);

                // checks for non-configurable properties
                // https://tc39.es/ecma262/#sec-proxy-object-internal-methods-and-internal-slots-get-p-receiver steps 9
                final DescriptorInfo targetDesc = proxyTarget.getOwnPropertyDescriptor(cx, propertyKey);
                if (targetDesc != null && targetDesc.isConfigurable(false)) {
                    if (targetDesc.isDataDescriptor() && targetDesc.isWritable(false)) {
                        if (!Objects.equals(trapResult, targetDesc.value)) {
                            throw ScriptRuntime.typeError(
                                    "proxy must report the same value for the non-writable,"
                                            + " non-configurable property '\"" + propertyKey + "\"'");
                        }
                    }
                    if (targetDesc.isAccessorDescriptor()
                            && (targetDesc.getter == null
                                    || targetDesc.getter == Scriptable.NOT_FOUND
                                    || Undefined.isUndefined(targetDesc.getter))) {
                        if (!Undefined.isUndefined(trapResult)) {
                            throw ScriptRuntime.typeError(
                                    "proxy must report the same value for the non-writable,"
                                            + " non-configurable property '\"" + propertyKey + "\"'");
                        }
                    }
                }
                return trapResult;
            }
        }

        return internalGet(cx, target, propertyKey, receiver);
    }

    /*
     * https://tc39.es/ecma262/#sec-ordinary-object-internal-methods-and-internal-slots-get-p-receiver
     * 1. Let desc be ? O.[[GetOwnProperty]](P).
     * 2. If desc is undefined, then
     *        a. Let parent be ? O.[[GetPrototypeOf]]().
     *        b. If parent is null, return undefined.
     *        c. Return ? parent.[[Get]](P, Receiver).
     * 3. If IsDataDescriptor(desc) is true, return desc.[[Value]].
     * 4. Assert: IsAccessorDescriptor(desc) is true.
     * 5. Let getter be desc.[[Get]].
     * 6. If getter is undefined, return undefined.
     * 7. Return ? Call(getter, Receiver).
     */
    private static Object internalGet(
            Context cx, ScriptableObject target, Object propertyKey, Object receiver) {
        final DescriptorInfo desc = target.getOwnPropertyDescriptor(cx, propertyKey);
        if (desc == null) {
            final Scriptable parent = target.getPrototype();
            if (parent == null) {
                return Undefined.SCRIPTABLE_UNDEFINED;
            }
            return internalGet(cx, ScriptableObject.ensureScriptableObject(parent), propertyKey, receiver);
        }

        if (desc.isDataDescriptor()) {
            return desc.value == Scriptable.NOT_FOUND
                    ? Undefined.SCRIPTABLE_UNDEFINED
                    : desc.value;
        }

        final Object getter = desc.getter;
        if (getter == null || getter == Scriptable.NOT_FOUND || Undefined.isUndefined(getter)) {
            return Undefined.SCRIPTABLE_UNDEFINED;
        }

        final Scriptable receiverForCall;
        if (receiver == null || Undefined.isUndefined(receiver)) {
            receiverForCall = cx.isStrictMode()
                    ? null
                    : ScriptableObject.getTopLevelScope(target);
        } else {
            receiverForCall = ScriptableObject.ensureScriptable(receiver);
        }
        return ((Function) getter).call(cx, target, receiverForCall, ScriptRuntime.emptyArgs);
    }

    private static Scriptable getOwnPropertyDescriptor(
            Context cx, Scriptable scope, Scriptable thisObj, Object[] args) {
        ScriptableObject target = checkTarget(args);

        if (args.length > 1) {
            if (ScriptRuntime.isSymbol(args[1])) {
                var desc = target.getOwnPropertyDescriptor(cx, args[1]);
                return desc == null ? Undefined.SCRIPTABLE_UNDEFINED : desc.toObject(scope);
            }

            var desc = target.getOwnPropertyDescriptor(cx, ScriptRuntime.toString(args[1]));
            return desc == null ? Undefined.SCRIPTABLE_UNDEFINED : desc.toObject(scope);
        }
        return Undefined.SCRIPTABLE_UNDEFINED;
    }

    private static Scriptable getPrototypeOf(
            Context cx, Scriptable scope, Scriptable thisObj, Object[] args) {
        ScriptableObject target = checkTarget(args);

        return target.getPrototype();
    }

    private static Object has(Context cx, Scriptable scope, Scriptable thisObj, Object[] args) {
        ScriptableObject target = checkTarget(args);

        if (args.length > 1) {
            if (ScriptRuntime.isSymbol(args[1])) {
                return ScriptableObject.hasProperty(target, (Symbol) args[1]);
            }

            return ScriptableObject.hasProperty(target, ScriptRuntime.toString(args[1]));
        }
        return false;
    }

    private static Object isExtensible(
            Context cx, Scriptable scope, Scriptable thisObj, Object[] args) {
        ScriptableObject target = checkTarget(args);
        return target.isExtensible();
    }

    private static Scriptable ownKeys(
            Context cx, Scriptable scope, Scriptable thisObj, Object[] args) {
        ScriptableObject target = checkTarget(args);

        final List<Object> strings = new ArrayList<>();
        final List<Object> symbols = new ArrayList<>();

        Object[] ids;
        try (var map = target.startCompoundOp(false)) {
            ids = target.getIds(map, true, true);
        }
        for (Object o : ids) {
            if (o instanceof Symbol) {
                symbols.add(o);
            } else {
                strings.add(ScriptRuntime.toString(o));
            }
        }

        Object[] keys = new Object[strings.size() + symbols.size()];
        System.arraycopy(strings.toArray(), 0, keys, 0, strings.size());
        System.arraycopy(symbols.toArray(), 0, keys, strings.size(), symbols.size());

        return cx.newArray(scope, keys);
    }

    private static Object preventExtensions(
            Context cx, Scriptable scope, Scriptable thisObj, Object[] args) {
        ScriptableObject target = checkTarget(args);

        return target.preventExtensions();
    }

    /*
     * https://tc39.es/ecma262/#sec-reflect.set
     * 1. If target is not an Object, throw a TypeError exception.
     * 2. Let key be ? ToPropertyKey(propertyKey).
     * 3. If receiver is not present, then
     *        a. Set receiver to target.
     * 4. Return ? target.[[Set]](key, V, receiver).
     */
    private static Object set(Context cx, Scriptable scope, Scriptable thisObj, Object[] args) {
        final ScriptableObject target = checkTarget(args);
        final Object propertyKey = args.length > 1 ? args[1] : Undefined.instance;
        final Object value = args.length > 2 ? args[2] : Undefined.instance;
        final Object receiver = args.length > 3 ? args[3] : target;

        // If target is a proxy, delegate to the proxy handler
        if (target instanceof NativeProxy) {
            final NativeProxy proxy = (NativeProxy) target;
            final Function trap = proxy.getTrap("set");
            if (trap != null) {
                final ScriptableObject proxyTarget = proxy.getTargetThrowIfRevoked();
                final Object[] trapArgs = {proxyTarget, propertyKey, value, receiver};
                final boolean booleanTrapResult = ScriptRuntime.toBoolean(proxy.callTrap(trap, trapArgs));
                if (!booleanTrapResult) {
                    return false;
                }

                // checks for non-configurable properties
                // https://tc39.es/ecma262/#sec-proxy-object-internal-methods-and-internal-slots-set-p-v-receiver steps 10
                final DescriptorInfo targetDesc = proxyTarget.getOwnPropertyDescriptor(cx, propertyKey);
                if (targetDesc != null && targetDesc.isConfigurable(false)) {
                    if (targetDesc.isDataDescriptor() && targetDesc.isWritable(false)) {
                        if (!Objects.equals(value, targetDesc.value)) {
                            throw ScriptRuntime.typeError(
                                    "proxy can't successfully set a non-writable,"
                                            + " non-configurable property '\"" + propertyKey + "\"'");
                        }
                    }
                    if (targetDesc.isAccessorDescriptor()
                            && (targetDesc.setter == null
                                    || targetDesc.setter == Scriptable.NOT_FOUND
                                    || Undefined.isUndefined(targetDesc.setter))) {
                        throw ScriptRuntime.typeError(
                                "proxy can't successfully set a non-writable,"
                                        + " non-configurable property '\"" + propertyKey + "\"'");
                    }
                }
                return true;
            }
        }

        return internalSet(cx, target, propertyKey, value, receiver);
    }

    /*
     * https://tc39.es/ecma262/#sec-ordinary-object-internal-methods-and-internal-slots-set-p-v-receiver
     * 1. Let ownDesc be ? O.[[GetOwnProperty]](P).
     * 2. If ownDesc is undefined, then
     *        a. Let parent be ? O.[[GetPrototypeOf]]().
     *        b. If parent is not null, then
     *               i. Return ? parent.[[Set]](P, V, Receiver).
     *        c. Else,
     *               i. Set ownDesc to the PropertyDescriptor
     *                  { [[Value]]: undefined, [[Writable]]: true,
     *                    [[Enumerable]]: true, [[Configurable]]: true }.
     * 3. If IsDataDescriptor(ownDesc) is true, then
     *        a. If ownDesc.[[Writable]] is false, return false.
     *        b. If Receiver is not an Object, return false.
     *        c. Let existingDescriptor be ? Receiver.[[GetOwnProperty]](P).
     *        d. If existingDescriptor is not undefined, then
     *               i. If IsAccessorDescriptor(existingDescriptor) is true, return false.
     *               ii. If existingDescriptor.[[Writable]] is false, return false.
     *               iii. Let valueDesc be the PropertyDescriptor { [[Value]]: V }.
     *               iv. Return ? Receiver.[[DefineOwnProperty]](P, valueDesc).
     *        e. Else,
     *               i. Assert: Receiver does not currently have a property P.
     *               ii. Return ? CreateDataProperty(Receiver, P, V).
     * 4. Assert: IsAccessorDescriptor(ownDesc) is true.
     * 5. Let setter be ownDesc.[[Set]].
     * 6. If setter is undefined, return false.
     * 7. Perform ? Call(setter, Receiver, « V »).
     * 8. Return true.
     */
    private static boolean internalSet(Context cx, ScriptableObject target, Object propertyKey,
            Object value, Object receiver) {
        try {
            DescriptorInfo ownDesc = target.getOwnPropertyDescriptor(cx, propertyKey);
            if (ownDesc == null) {
                final Scriptable parent = target.getPrototype();
                if (parent != null) {
                    return internalSet(cx, ScriptableObject.ensureScriptableObject(parent), propertyKey, value, receiver);
                }
                ownDesc = new DescriptorInfo(true, true, true, Undefined.instance);
            }

            if (ownDesc.isDataDescriptor()) {
                if (ownDesc.isWritable(false)) {
                    return false;
                }
                if (!ScriptRuntime.isObject(receiver)) {
                    return false;
                }

                final ScriptableObject receiverObj = ScriptableObject.ensureScriptableObject(receiver);
                final DescriptorInfo existingDescriptor = receiverObj.getOwnPropertyDescriptor(cx, propertyKey);
                if (existingDescriptor != null) {
                    if (existingDescriptor.isAccessorDescriptor()) {
                        return false;
                    }
                    if (existingDescriptor.isWritable(false)) {
                        return false;
                    }
                } else if (!receiverObj.isExtensible()) {
                    return false;
                }

                // If receiver is a proxy, set property directly on the proxy's target
                // to avoid recursion (reflect <-> proxy)
                final ScriptableObject realReceiverObj = receiverObj instanceof NativeProxy
                        ? ((NativeProxy) receiverObj).getTargetThrowIfRevoked()
                        : receiverObj;

                if (ScriptRuntime.isSymbol(propertyKey)) {
                    realReceiverObj.put((Symbol) propertyKey, realReceiverObj, value);
                } else {
                    final StringIdOrIndex s = ScriptRuntime.toStringIdOrIndex(propertyKey);
                    if (s.stringId == null) {
                        realReceiverObj.put(s.index, realReceiverObj, value);
                    } else {
                        realReceiverObj.put(s.stringId, realReceiverObj, value);
                    }
                }

                return true;
            }

            if (ownDesc.isAccessorDescriptor()) {
                final Object setter = ownDesc.setter;
                if (setter == null
                        || setter == Scriptable.NOT_FOUND
                        || Undefined.isUndefined(setter)) {
                    return false;
                }
                final Scriptable receiverForCall;
                if (receiver == null || Undefined.isUndefined(receiver)) {
                    receiverForCall = cx.isStrictMode()
                            ? null
                            : ScriptableObject.getTopLevelScope(target);
                } else {
                    receiverForCall = ScriptableObject.ensureScriptable(receiver);
                }

                ((Function) setter).call(cx, target, receiverForCall, new Object[] {value});
            }

            return true;

        } catch (EcmaError e) {
            return false;
        }
    }

    private static Object setPrototypeOf(
            Context cx, Scriptable scope, Scriptable thisObj, Object[] args) {
        if (args.length < 2) {
            throw ScriptRuntime.typeErrorById(
                    "msg.method.missing.parameter",
                    "Reflect.js_setPrototypeOf",
                    "2",
                    Integer.toString(args.length));
        }

        ScriptableObject target = checkTarget(args);

        if (target.getPrototype() == args[1]) {
            return true;
        }

        if (!target.isExtensible()) {
            return false;
        }

        if (args[1] == null) {
            target.setPrototype(null);
            return true;
        }

        if (ScriptRuntime.isSymbol(args[1])) {
            throw ScriptRuntime.typeErrorById("msg.arg.not.object", ScriptRuntime.typeof(args[0]));
        }

        ScriptableObject proto = ScriptableObject.ensureScriptableObject(args[1]);
        if (target.getPrototype() == proto) {
            return true;
        }

        // avoid cycles
        Scriptable p = proto;
        while (p != null) {
            if (target == p) {
                return false;
            }
            p = p.getPrototype();
        }

        target.setPrototype(proto);
        return true;
    }

    private static ScriptableObject checkTarget(Object[] args) {
        if (args.length == 0 || args[0] == null || args[0] == Undefined.instance) {
            Object argument = args.length == 0 ? Undefined.instance : args[0];
            throw ScriptRuntime.typeErrorById(
                    "msg.no.properties", ScriptRuntime.toString(argument));
        }

        if (ScriptRuntime.isSymbol(args[0])) {
            throw ScriptRuntime.typeErrorById("msg.arg.not.object", ScriptRuntime.typeof(args[0]));
        }
        return ScriptableObject.ensureScriptableObject(args[0]);
    }
}

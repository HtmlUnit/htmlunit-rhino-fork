package org.mozilla.javascript.lc.type.impl.factory;

import java.lang.reflect.GenericArrayType;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.lang.reflect.TypeVariable;
import java.lang.reflect.WildcardType;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.mozilla.javascript.lc.type.ParameterizedTypeInfo;
import org.mozilla.javascript.lc.type.TypeInfo;
import org.mozilla.javascript.lc.type.TypeInfoFactory;
import org.mozilla.javascript.lc.type.VariableTypeInfo;
import org.mozilla.javascript.lc.type.impl.ArrayTypeInfo;
import org.mozilla.javascript.lc.type.impl.ParameterizedTypeInfoImpl;

/**
 * @author ZZZank
 */
public interface FactoryBase extends TypeInfoFactory {

    @Override
    default TypeInfo create(GenericArrayType genericArrayType) {
        return toArray(create(genericArrayType.getGenericComponentType()));
    }

    @Override
    default TypeInfo create(ParameterizedType parameterizedType) {
        return attachParam(
                create(parameterizedType.getRawType()),
                createList(parameterizedType.getActualTypeArguments()));
    }

    @Override
    default TypeInfo create(WildcardType wildcardType) {
        Type[] upper = wildcardType.getUpperBounds();
        if (upper.length != 0 && upper[0] != Object.class) {
            return create(upper[0]);
        }

        Type[] lower = wildcardType.getLowerBounds();
        if (lower.length != 0) {
            return create(lower[0]);
        }
        return TypeInfo.NONE;
    }

    @Override
    default TypeInfo toArray(TypeInfo component) {
        return new ArrayTypeInfo(component);
    }

    @Override
    default TypeInfo attachParam(TypeInfo base, List<TypeInfo> params) {
        if (base instanceof ParameterizedTypeInfo) {
            base = ((ParameterizedTypeInfo) base).rawType();
        }
        return new ParameterizedTypeInfoImpl(base, params);
    }

    static Map<VariableTypeInfo, TypeInfo> transformMapping(
            Map<VariableTypeInfo, TypeInfo> mapping, Map<VariableTypeInfo, TypeInfo> transformer) {
        if (mapping.isEmpty()) {
            return new HashMap<>();
        } else if (mapping.size() == 1) {
            Map.Entry<VariableTypeInfo, TypeInfo> entry = mapping.entrySet().iterator().next();

            Map<VariableTypeInfo, TypeInfo> result = new HashMap<>();
            result.put(entry.getKey(), entry.getValue().consolidate(transformer));
            return result;
        }
        HashMap<VariableTypeInfo, TypeInfo> transformed = new HashMap<>(mapping);
        for (Map.Entry<VariableTypeInfo, TypeInfo> entry : transformed.entrySet()) {
            entry.setValue(entry.getValue().consolidate(transformer));
        }
        return transformed;
    }

    /** Used by {@link #getConsolidationMapping(java.lang.Class)} */
    default Map<VariableTypeInfo, TypeInfo> computeConsolidationMapping(Class<?> type) {
        HashMap<VariableTypeInfo, TypeInfo> mapping = new HashMap<VariableTypeInfo, TypeInfo>();

        // in our E.class example, this will collect mapping from B<Te>, forming Tb -> Te
        extractSuperMapping(type.getGenericSuperclass(), mapping);

        // in our E.class example, this will collect mapping from D<String>, forming Td -> String
        for (Type genericInterface : type.getGenericInterfaces()) {
            extractSuperMapping(genericInterface, mapping);
        }

        // extract mappings for superclasses/interfaces
        // in our E.class example, super mapping will include Ta -> Tb
        Map<VariableTypeInfo, TypeInfo> superMapping = getConsolidationMapping(type.getSuperclass());

        // in our E.class example, interface mapping will include Tc -> Td
        Class<?>[] interfaces = type.getInterfaces();
        ArrayList<Map<VariableTypeInfo, TypeInfo>> interfaceMappings = new ArrayList<Map<VariableTypeInfo, TypeInfo>>(interfaces.length);
        for (Class<?> interface_ : interfaces) {
            interfaceMappings.add(getConsolidationMapping(interface_));
        }

        if (superMapping.isEmpty() && interfaceMappings.stream().allMatch(Map::isEmpty)) {
            return new HashMap<>(mapping);
        }

        // transform super mapping to make it able to directly map a type to types used by E.class,
        // then merge them together
        // Example: Ta -> Tb (from `superMapping`) will be transformed by Tb -> Te (from `mapping`),
        // forming Ta -> Te
        HashMap<VariableTypeInfo, TypeInfo> merged = new HashMap<>(transformMapping(superMapping, mapping));
        for (Map<VariableTypeInfo, TypeInfo> interfaceMapping : interfaceMappings) {
            merged.putAll(transformMapping(interfaceMapping, mapping));
        }
        merged.putAll(mapping);

        // Result: `Ta -> Te`, `Tb -> Te`, `Tc -> String`, `Td -> String`
        // This means that all type variables from superclass / interface (Ta, Tb, Tc, Td) can be
        // eliminated by applying the mapping ONCE, which will be important for performance
        return new HashMap<>(merged);
    }

    default void extractSuperMapping(Type superType, Map<VariableTypeInfo, TypeInfo> pushTo) {
        if (!(superType instanceof ParameterizedType)) {
            return;
        }
        ParameterizedType parameterized = (ParameterizedType) superType;
        if (!(parameterized.getRawType() instanceof Class<?>)) {
            return;
        }
        Class<?> parent = (Class<?>) parameterized.getRawType();

        final TypeVariable<? extends Class<?>>[] params = parent.getTypeParameters(); // T
        final Type[] args = parameterized.getActualTypeArguments(); // T is mapped to

        if (params.length != args.length) {
            throw new IllegalArgumentException(
                    String.format(
                            "typeParameters.length != actualTypeArguments.length (%s != %s)",
                            params.length, args.length));
        }

        for (int i = 0; i < args.length; i++) {
            pushTo.put((VariableTypeInfo) create(params[i]), create(args[i]));
        }
    }
}

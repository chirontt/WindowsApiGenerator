//
// Windows API Generator for Java
// Copyright (c) 2026 Manuel Bleichenbacher
// Licensed under MIT License
// https://opensource.org/licenses/MIT
//
package net.codecrete.windowsapi.graalvm;

import net.codecrete.windowsapi.graalvm.json.Downcall;
import net.codecrete.windowsapi.graalvm.json.DowncallLinkerOptions;
import net.codecrete.windowsapi.graalvm.json.ForeignApiConfiguration;
import net.codecrete.windowsapi.graalvm.json.ReachabilityMetadata;
import net.codecrete.windowsapi.graalvm.json.Upcall;
import net.codecrete.windowsapi.metadata.ComInterface;
import net.codecrete.windowsapi.metadata.Delegate;
import net.codecrete.windowsapi.metadata.Method;
import net.codecrete.windowsapi.metadata.Type;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

/**
 * Builds the GraalVM reachability metadata for a given scope.
 * <p>
 * The metadata contains a deduplicated downcall entry for each distinct function
 * and COM interface method signature and an upcall entry for each distinct
 * callback function signature. The function/method names are irrelevant; only
 * the return type, parameter types and linker options matter.
 * </p>
 */
public final class ReachabilityMetadataBuilder {

    private static final Comparator<List<String>> PARAMETER_TYPES_COMPARATOR = (a, b) -> {
        var common = Math.min(a.size(), b.size());
        for (var i = 0; i < common; i += 1) {
            var result = a.get(i).compareTo(b.get(i));
            if (result != 0)
                return result;
        }
        return Integer.compare(a.size(), b.size());
    };

    private static final Comparator<Downcall> DOWNCALL_COMPARATOR =
            Comparator.comparing(Downcall::returnType)
                    .thenComparing(Downcall::parameterTypes, PARAMETER_TYPES_COMPARATOR)
                    .thenComparing(downcall -> downcall.linkerOptions().captureCallState());

    private static final Comparator<Upcall> UPCALL_COMPARATOR =
            Comparator.comparing(Upcall::returnType)
                    .thenComparing(Upcall::parameterTypes, PARAMETER_TYPES_COMPARATOR);

    private final Set<Method> functions;
    private final Set<Type> transitiveTypes;

    /**
     * Creates a new instance.
     *
     * @param functions       the functions in scope
     * @param transitiveTypes the transitive type scope (containing COM interfaces and callback functions)
     */
    public ReachabilityMetadataBuilder(Set<Method> functions, Set<Type> transitiveTypes) {
        this.functions = functions;
        this.transitiveTypes = transitiveTypes;
    }

    /**
     * Builds the reachability metadata.
     *
     * @return the reachability metadata
     */
    public ReachabilityMetadata build() {
        // TreeSet deduplicates by the comparator (which is the dedup key) and sorts for deterministic output.
        var downcalls = new TreeSet<>(DOWNCALL_COMPARATOR);
        for (var function : functions)
            downcalls.add(toDowncall(function, false));
        for (var type : transitiveTypes) {
            if (type instanceof ComInterface comInterface) {
                for (var method : comInterface.methods())
                    downcalls.add(toDowncall(method, true));
            }
        }

        var upcalls = new TreeSet<>(UPCALL_COMPARATOR);
        for (var type : transitiveTypes) {
            if (type instanceof Delegate delegate)
                upcalls.add(toUpcall(delegate.signature()));
        }

        return new ReachabilityMetadata(new ForeignApiConfiguration(List.copyOf(downcalls), List.copyOf(upcalls)), null);
    }

    private static Downcall toDowncall(Method method, boolean comMethod) {
        var parameterTypes = new ArrayList<String>();
        if (comMethod)
            parameterTypes.add(LayoutDescriptor.POINTER); // the leading "this" pointer
        for (var parameter : method.parameters())
            parameterTypes.add(LayoutDescriptor.of(parameter.type()));

        return new Downcall(
                LayoutDescriptor.ofReturnType(method),
                List.copyOf(parameterTypes),
                new DowncallLinkerOptions(method.supportsLastError()));
    }

    private static Upcall toUpcall(Method signature) {
        var parameterTypes = new ArrayList<String>();
        for (var parameter : signature.parameters())
            parameterTypes.add(LayoutDescriptor.of(parameter.type()));

        return new Upcall(LayoutDescriptor.ofReturnType(signature), List.copyOf(parameterTypes));
    }
}

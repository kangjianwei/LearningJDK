// CheckStyle: stop header check
// CheckStyle: stop line length check
// GENERATED CONTENT - DO NOT EDIT
// Source: PostAllocationOptimizationStage.java
package org.graalvm.compiler.lir.phases;

import java.util.*;
import org.graalvm.compiler.options.*;
import org.graalvm.compiler.options.OptionType;

public class PostAllocationOptimizationStage_OptionDescriptors implements OptionDescriptors {
    @Override
    public OptionDescriptor get(String value) {
        switch (value) {
        // CheckStyle: stop line length check
        case "LIROptControlFlowOptimizer": {
            return OptionDescriptor.create(
                /*name*/ "LIROptControlFlowOptimizer",
                /*optionType*/ OptionType.Debug,
                /*optionValueType*/ Boolean.class,
                /*help*/ "",
                /*declaringClass*/ PostAllocationOptimizationStage.Options.class,
                /*fieldName*/ "LIROptControlFlowOptimizer",
                /*option*/ PostAllocationOptimizationStage.Options.LIROptControlFlowOptimizer);
        }
        case "LIROptEdgeMoveOptimizer": {
            return OptionDescriptor.create(
                /*name*/ "LIROptEdgeMoveOptimizer",
                /*optionType*/ OptionType.Debug,
                /*optionValueType*/ Boolean.class,
                /*help*/ "",
                /*declaringClass*/ PostAllocationOptimizationStage.Options.class,
                /*fieldName*/ "LIROptEdgeMoveOptimizer",
                /*option*/ PostAllocationOptimizationStage.Options.LIROptEdgeMoveOptimizer);
        }
        case "LIROptNullCheckOptimizer": {
            return OptionDescriptor.create(
                /*name*/ "LIROptNullCheckOptimizer",
                /*optionType*/ OptionType.Debug,
                /*optionValueType*/ Boolean.class,
                /*help*/ "",
                /*declaringClass*/ PostAllocationOptimizationStage.Options.class,
                /*fieldName*/ "LIROptNullCheckOptimizer",
                /*option*/ PostAllocationOptimizationStage.Options.LIROptNullCheckOptimizer);
        }
        case "LIROptRedundantMoveElimination": {
            return OptionDescriptor.create(
                /*name*/ "LIROptRedundantMoveElimination",
                /*optionType*/ OptionType.Debug,
                /*optionValueType*/ Boolean.class,
                /*help*/ "",
                /*declaringClass*/ PostAllocationOptimizationStage.Options.class,
                /*fieldName*/ "LIROptRedundantMoveElimination",
                /*option*/ PostAllocationOptimizationStage.Options.LIROptRedundantMoveElimination);
        }
        case "LIRProfileMethods": {
            return OptionDescriptor.create(
                /*name*/ "LIRProfileMethods",
                /*optionType*/ OptionType.Debug,
                /*optionValueType*/ Boolean.class,
                /*help*/ "Enables profiling of methods.",
                /*declaringClass*/ PostAllocationOptimizationStage.Options.class,
                /*fieldName*/ "LIRProfileMethods",
                /*option*/ PostAllocationOptimizationStage.Options.LIRProfileMethods);
        }
        case "LIRProfileMoves": {
            return OptionDescriptor.create(
                /*name*/ "LIRProfileMoves",
                /*optionType*/ OptionType.Debug,
                /*optionValueType*/ Boolean.class,
                /*help*/ "Enables profiling of move types on LIR level. Move types are for example stores (register to stack), constant loads (constant to register) or copies (register to register).",
                /*declaringClass*/ PostAllocationOptimizationStage.Options.class,
                /*fieldName*/ "LIRProfileMoves",
                /*option*/ PostAllocationOptimizationStage.Options.LIRProfileMoves);
        }
        // CheckStyle: resume line length check
        }
        return null;
    }

    @Override
    public Iterator<OptionDescriptor> iterator() {
        return new Iterator<OptionDescriptor>() {
            int i = 0;
            @Override
            public boolean hasNext() {
                return i < 6;
            }
            @Override
            public OptionDescriptor next() {
                switch (i++) {
                    case 0: return get("LIROptControlFlowOptimizer");
                    case 1: return get("LIROptEdgeMoveOptimizer");
                    case 2: return get("LIROptNullCheckOptimizer");
                    case 3: return get("LIROptRedundantMoveElimination");
                    case 4: return get("LIRProfileMethods");
                    case 5: return get("LIRProfileMoves");
                }
                throw new NoSuchElementException();
            }
        };
    }
}

// CheckStyle: stop header check
// CheckStyle: stop line length check
// GENERATED CONTENT - DO NOT EDIT
// Source: OptimizingLinearScanWalker.java
package org.graalvm.compiler.lir.alloc.lsra;

import java.util.*;
import org.graalvm.compiler.options.*;
import org.graalvm.compiler.options.OptionType;

public class OptimizingLinearScanWalker_OptionDescriptors implements OptionDescriptors {
    @Override
    public OptionDescriptor get(String value) {
        switch (value) {
        // CheckStyle: stop line length check
        case "LSRAOptSplitOnly": {
            return OptionDescriptor.create(
                /*name*/ "LSRAOptSplitOnly",
                /*optionType*/ OptionType.Debug,
                /*optionValueType*/ Boolean.class,
                /*help*/ "LSRA optimization: Only split but do not reassign",
                /*declaringClass*/ OptimizingLinearScanWalker.Options.class,
                /*fieldName*/ "LSRAOptSplitOnly",
                /*option*/ OptimizingLinearScanWalker.Options.LSRAOptSplitOnly);
        }
        case "LSRAOptimization": {
            return OptionDescriptor.create(
                /*name*/ "LSRAOptimization",
                /*optionType*/ OptionType.Debug,
                /*optionValueType*/ Boolean.class,
                /*help*/ "Enable LSRA optimization",
                /*declaringClass*/ OptimizingLinearScanWalker.Options.class,
                /*fieldName*/ "LSRAOptimization",
                /*option*/ OptimizingLinearScanWalker.Options.LSRAOptimization);
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
                return i < 2;
            }
            @Override
            public OptionDescriptor next() {
                switch (i++) {
                    case 0: return get("LSRAOptSplitOnly");
                    case 1: return get("LSRAOptimization");
                }
                throw new NoSuchElementException();
            }
        };
    }
}

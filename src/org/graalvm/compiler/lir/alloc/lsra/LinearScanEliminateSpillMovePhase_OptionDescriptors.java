// CheckStyle: stop header check
// CheckStyle: stop line length check
// GENERATED CONTENT - DO NOT EDIT
// Source: LinearScanEliminateSpillMovePhase.java
package org.graalvm.compiler.lir.alloc.lsra;

import java.util.*;
import org.graalvm.compiler.options.*;
import org.graalvm.compiler.options.OptionType;

public class LinearScanEliminateSpillMovePhase_OptionDescriptors implements OptionDescriptors {
    @Override
    public OptionDescriptor get(String value) {
        switch (value) {
        // CheckStyle: stop line length check
        case "LIROptLSRAEliminateSpillMoves": {
            return OptionDescriptor.create(
                /*name*/ "LIROptLSRAEliminateSpillMoves",
                /*optionType*/ OptionType.Debug,
                /*optionValueType*/ Boolean.class,
                /*help*/ "Enable spill move elimination.",
                /*declaringClass*/ LinearScanEliminateSpillMovePhase.Options.class,
                /*fieldName*/ "LIROptLSRAEliminateSpillMoves",
                /*option*/ LinearScanEliminateSpillMovePhase.Options.LIROptLSRAEliminateSpillMoves);
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
                return i < 1;
            }
            @Override
            public OptionDescriptor next() {
                switch (i++) {
                    case 0: return get("LIROptLSRAEliminateSpillMoves");
                }
                throw new NoSuchElementException();
            }
        };
    }
}

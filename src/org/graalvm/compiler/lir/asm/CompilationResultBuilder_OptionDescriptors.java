// CheckStyle: stop header check
// CheckStyle: stop line length check
// GENERATED CONTENT - DO NOT EDIT
// Source: CompilationResultBuilder.java
package org.graalvm.compiler.lir.asm;

import java.util.*;
import org.graalvm.compiler.options.*;
import org.graalvm.compiler.options.OptionType;

public class CompilationResultBuilder_OptionDescriptors implements OptionDescriptors {
    @Override
    public OptionDescriptor get(String value) {
        switch (value) {
        // CheckStyle: stop line length check
        case "PrintLIRWithAssembly": {
            return OptionDescriptor.create(
                /*name*/ "PrintLIRWithAssembly",
                /*optionType*/ OptionType.Debug,
                /*optionValueType*/ Boolean.class,
                /*help*/ "Include the LIR as comments with the final assembly.",
                /*declaringClass*/ CompilationResultBuilder.Options.class,
                /*fieldName*/ "PrintLIRWithAssembly",
                /*option*/ CompilationResultBuilder.Options.PrintLIRWithAssembly);
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
                    case 0: return get("PrintLIRWithAssembly");
                }
                throw new NoSuchElementException();
            }
        };
    }
}

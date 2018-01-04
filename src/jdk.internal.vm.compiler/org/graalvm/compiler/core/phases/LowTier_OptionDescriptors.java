// CheckStyle: stop header check
// CheckStyle: stop line length check
// GENERATED CONTENT - DO NOT EDIT
// Source: LowTier.java
package org.graalvm.compiler.core.phases;

import java.util.*;
import org.graalvm.compiler.options.*;
import org.graalvm.compiler.options.OptionType;

public class LowTier_OptionDescriptors implements OptionDescriptors {
    @Override
    public OptionDescriptor get(String value) {
        switch (value) {
        // CheckStyle: stop line length check
        case "ProfileCompiledMethods": {
            return OptionDescriptor.create(
                /*name*/ "ProfileCompiledMethods",
                /*optionType*/ OptionType.Debug,
                /*optionValueType*/ Boolean.class,
                /*help*/ "",
                /*declaringClass*/ LowTier.Options.class,
                /*fieldName*/ "ProfileCompiledMethods",
                /*option*/ LowTier.Options.ProfileCompiledMethods);
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
                    case 0: return get("ProfileCompiledMethods");
                }
                throw new NoSuchElementException();
            }
        };
    }
}

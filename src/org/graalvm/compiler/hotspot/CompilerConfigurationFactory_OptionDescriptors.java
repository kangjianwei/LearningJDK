// CheckStyle: stop header check
// CheckStyle: stop line length check
// GENERATED CONTENT - DO NOT EDIT
// Source: CompilerConfigurationFactory.java
package org.graalvm.compiler.hotspot;

import java.util.*;
import org.graalvm.compiler.options.*;
import org.graalvm.compiler.options.OptionType;

public class CompilerConfigurationFactory_OptionDescriptors implements OptionDescriptors {
    @Override
    public OptionDescriptor get(String value) {
        switch (value) {
        // CheckStyle: stop line length check
        case "CompilerConfiguration": {
            return OptionDescriptor.create(
                /*name*/ "CompilerConfiguration",
                /*optionType*/ OptionType.Expert,
                /*optionValueType*/ String.class,
                /*help*/ "Names the Graal compiler configuration to use. If ommitted, the compiler configuration with the highest auto-selection priority is used. To see the set of available configurations, supply the value 'help' to this option.",
                /*declaringClass*/ CompilerConfigurationFactory.Options.class,
                /*fieldName*/ "CompilerConfiguration",
                /*option*/ CompilerConfigurationFactory.Options.CompilerConfiguration);
        }
        case "ShowConfiguration": {
            return OptionDescriptor.create(
                /*name*/ "ShowConfiguration",
                /*optionType*/ OptionType.User,
                /*optionValueType*/ org.graalvm.compiler.hotspot.CompilerConfigurationFactory.ShowConfigurationLevel.class,
                /*help*/ "Writes to the VM log information about the Graal compiler configuration selected.",
                /*declaringClass*/ CompilerConfigurationFactory.Options.class,
                /*fieldName*/ "ShowConfiguration",
                /*option*/ CompilerConfigurationFactory.Options.ShowConfiguration);
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
                    case 0: return get("CompilerConfiguration");
                    case 1: return get("ShowConfiguration");
                }
                throw new NoSuchElementException();
            }
        };
    }
}

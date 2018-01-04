// CheckStyle: stop header check
// CheckStyle: stop line length check
// GENERATED CONTENT - DO NOT EDIT
// Source: NodeCounterPhase.java
package org.graalvm.compiler.phases.common;

import java.util.*;
import org.graalvm.compiler.options.*;
import org.graalvm.compiler.options.OptionType;

public class NodeCounterPhase_OptionDescriptors implements OptionDescriptors {
    @Override
    public OptionDescriptor get(String value) {
        switch (value) {
        // CheckStyle: stop line length check
        case "NodeCounters": {
            return OptionDescriptor.create(
                /*name*/ "NodeCounters",
                /*optionType*/ OptionType.Debug,
                /*optionValueType*/ Boolean.class,
                /*help*/ "Counts the number of instances of each node class.",
                /*declaringClass*/ NodeCounterPhase.Options.class,
                /*fieldName*/ "NodeCounters",
                /*option*/ NodeCounterPhase.Options.NodeCounters);
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
                    case 0: return get("NodeCounters");
                }
                throw new NoSuchElementException();
            }
        };
    }
}

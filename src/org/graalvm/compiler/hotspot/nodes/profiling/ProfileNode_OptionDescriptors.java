// CheckStyle: stop header check
// CheckStyle: stop line length check
// GENERATED CONTENT - DO NOT EDIT
// Source: ProfileNode.java
package org.graalvm.compiler.hotspot.nodes.profiling;

import java.util.*;
import org.graalvm.compiler.options.*;
import org.graalvm.compiler.options.OptionType;

public class ProfileNode_OptionDescriptors implements OptionDescriptors {
    @Override
    public OptionDescriptor get(String value) {
        switch (value) {
        // CheckStyle: stop line length check
        case "ProbabilisticProfiling": {
            return OptionDescriptor.create(
                /*name*/ "ProbabilisticProfiling",
                /*optionType*/ OptionType.Expert,
                /*optionValueType*/ Boolean.class,
                /*help*/ "Control probabilistic profiling on AMD64",
                /*declaringClass*/ ProfileNode.Options.class,
                /*fieldName*/ "ProbabilisticProfiling",
                /*option*/ ProfileNode.Options.ProbabilisticProfiling);
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
                    case 0: return get("ProbabilisticProfiling");
                }
                throw new NoSuchElementException();
            }
        };
    }
}

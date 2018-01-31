// CheckStyle: stop header check
// CheckStyle: stop line length check
// GENERATED CONTENT - DO NOT EDIT
// Source: PEGraphDecoder.java
package org.graalvm.compiler.replacements;

import java.util.*;
import org.graalvm.compiler.options.*;
import org.graalvm.compiler.options.OptionType;

public class PEGraphDecoder_OptionDescriptors implements OptionDescriptors {
    @Override
    public OptionDescriptor get(String value) {
        switch (value) {
        // CheckStyle: stop line length check
        case "FailedLoopExplosionIsFatal": {
            return OptionDescriptor.create(
                /*name*/ "FailedLoopExplosionIsFatal",
                /*optionType*/ OptionType.Debug,
                /*optionValueType*/ Boolean.class,
                /*help*/ "Do not bail out but throw an exception on failed loop explosion.",
                /*declaringClass*/ PEGraphDecoder.Options.class,
                /*fieldName*/ "FailedLoopExplosionIsFatal",
                /*option*/ PEGraphDecoder.Options.FailedLoopExplosionIsFatal);
        }
        case "InliningDepthError": {
            return OptionDescriptor.create(
                /*name*/ "InliningDepthError",
                /*optionType*/ OptionType.Debug,
                /*optionValueType*/ Integer.class,
                /*help*/ "Maximum inlining depth during partial evaluation before reporting an infinite recursion",
                /*declaringClass*/ PEGraphDecoder.Options.class,
                /*fieldName*/ "InliningDepthError",
                /*option*/ PEGraphDecoder.Options.InliningDepthError);
        }
        case "MaximumLoopExplosionCount": {
            return OptionDescriptor.create(
                /*name*/ "MaximumLoopExplosionCount",
                /*optionType*/ OptionType.Debug,
                /*optionValueType*/ Integer.class,
                /*help*/ "Max number of loop explosions per method.",
                /*declaringClass*/ PEGraphDecoder.Options.class,
                /*fieldName*/ "MaximumLoopExplosionCount",
                /*option*/ PEGraphDecoder.Options.MaximumLoopExplosionCount);
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
                return i < 3;
            }
            @Override
            public OptionDescriptor next() {
                switch (i++) {
                    case 0: return get("FailedLoopExplosionIsFatal");
                    case 1: return get("InliningDepthError");
                    case 2: return get("MaximumLoopExplosionCount");
                }
                throw new NoSuchElementException();
            }
        };
    }
}

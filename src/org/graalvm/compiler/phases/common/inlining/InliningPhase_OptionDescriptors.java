// CheckStyle: stop header check
// CheckStyle: stop line length check
// GENERATED CONTENT - DO NOT EDIT
// Source: InliningPhase.java
package org.graalvm.compiler.phases.common.inlining;

import java.util.*;
import org.graalvm.compiler.options.*;
import org.graalvm.compiler.options.OptionType;

public class InliningPhase_OptionDescriptors implements OptionDescriptors {
    @Override
    public OptionDescriptor get(String value) {
        switch (value) {
        // CheckStyle: stop line length check
        case "AlwaysInlineIntrinsics": {
            return OptionDescriptor.create(
                /*name*/ "AlwaysInlineIntrinsics",
                /*optionType*/ OptionType.Debug,
                /*optionValueType*/ Boolean.class,
                /*help*/ "Unconditionally inline intrinsics",
                /*declaringClass*/ InliningPhase.Options.class,
                /*fieldName*/ "AlwaysInlineIntrinsics",
                /*option*/ InliningPhase.Options.AlwaysInlineIntrinsics);
        }
        case "MethodInlineBailoutLimit": {
            return OptionDescriptor.create(
                /*name*/ "MethodInlineBailoutLimit",
                /*optionType*/ OptionType.Debug,
                /*optionValueType*/ Integer.class,
                /*help*/ "Per-compilation method inlining exploration limit before giving up (use 0 to disable)",
                /*declaringClass*/ InliningPhase.Options.class,
                /*fieldName*/ "MethodInlineBailoutLimit",
                /*option*/ InliningPhase.Options.MethodInlineBailoutLimit);
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
                    case 0: return get("AlwaysInlineIntrinsics");
                    case 1: return get("MethodInlineBailoutLimit");
                }
                throw new NoSuchElementException();
            }
        };
    }
}

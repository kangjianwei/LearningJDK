// CheckStyle: stop header check
// CheckStyle: stop line length check
// GENERATED CONTENT - DO NOT EDIT
// Source: AOTInliningPolicy.java
package org.graalvm.compiler.hotspot.phases.aot;

import java.util.*;
import org.graalvm.compiler.options.*;
import org.graalvm.compiler.options.OptionType;

public class AOTInliningPolicy_OptionDescriptors implements OptionDescriptors {
    @Override
    public OptionDescriptor get(String value) {
        switch (value) {
        // CheckStyle: stop line length check
        case "AOTInliningDepthToSizeRate": {
            return OptionDescriptor.create(
                /*name*/ "AOTInliningDepthToSizeRate",
                /*optionType*/ OptionType.Expert,
                /*optionValueType*/ Double.class,
                /*help*/ "",
                /*declaringClass*/ AOTInliningPolicy.Options.class,
                /*fieldName*/ "AOTInliningDepthToSizeRate",
                /*option*/ AOTInliningPolicy.Options.AOTInliningDepthToSizeRate);
        }
        case "AOTInliningSizeMaximum": {
            return OptionDescriptor.create(
                /*name*/ "AOTInliningSizeMaximum",
                /*optionType*/ OptionType.Expert,
                /*optionValueType*/ Integer.class,
                /*help*/ "",
                /*declaringClass*/ AOTInliningPolicy.Options.class,
                /*fieldName*/ "AOTInliningSizeMaximum",
                /*option*/ AOTInliningPolicy.Options.AOTInliningSizeMaximum);
        }
        case "AOTInliningSizeMinimum": {
            return OptionDescriptor.create(
                /*name*/ "AOTInliningSizeMinimum",
                /*optionType*/ OptionType.Expert,
                /*optionValueType*/ Integer.class,
                /*help*/ "",
                /*declaringClass*/ AOTInliningPolicy.Options.class,
                /*fieldName*/ "AOTInliningSizeMinimum",
                /*option*/ AOTInliningPolicy.Options.AOTInliningSizeMinimum);
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
                    case 0: return get("AOTInliningDepthToSizeRate");
                    case 1: return get("AOTInliningSizeMaximum");
                    case 2: return get("AOTInliningSizeMinimum");
                }
                throw new NoSuchElementException();
            }
        };
    }
}

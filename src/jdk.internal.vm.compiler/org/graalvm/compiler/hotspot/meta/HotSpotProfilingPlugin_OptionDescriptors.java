// CheckStyle: stop header check
// CheckStyle: stop line length check
// GENERATED CONTENT - DO NOT EDIT
// Source: HotSpotProfilingPlugin.java
package org.graalvm.compiler.hotspot.meta;

import java.util.*;
import org.graalvm.compiler.options.*;
import org.graalvm.compiler.options.OptionType;

public class HotSpotProfilingPlugin_OptionDescriptors implements OptionDescriptors {
    @Override
    public OptionDescriptor get(String value) {
        switch (value) {
        // CheckStyle: stop line length check
        case "ProfileBackedges": {
            return OptionDescriptor.create(
                /*name*/ "ProfileBackedges",
                /*optionType*/ OptionType.Expert,
                /*optionValueType*/ Boolean.class,
                /*help*/ "Emit profiling of backedges",
                /*declaringClass*/ HotSpotProfilingPlugin.Options.class,
                /*fieldName*/ "ProfileBackedges",
                /*option*/ HotSpotProfilingPlugin.Options.ProfileBackedges);
        }
        case "ProfileInvokes": {
            return OptionDescriptor.create(
                /*name*/ "ProfileInvokes",
                /*optionType*/ OptionType.Expert,
                /*optionValueType*/ Boolean.class,
                /*help*/ "Emit profiling of invokes",
                /*declaringClass*/ HotSpotProfilingPlugin.Options.class,
                /*fieldName*/ "ProfileInvokes",
                /*option*/ HotSpotProfilingPlugin.Options.ProfileInvokes);
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
                    case 0: return get("ProfileBackedges");
                    case 1: return get("ProfileInvokes");
                }
                throw new NoSuchElementException();
            }
        };
    }
}

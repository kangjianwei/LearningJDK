// CheckStyle: stop header check
// CheckStyle: stop line length check
// GENERATED CONTENT - DO NOT EDIT
// Source: HotSpotTTYStreamProvider.java
package org.graalvm.compiler.hotspot;

import java.util.*;
import org.graalvm.compiler.options.*;
import org.graalvm.compiler.options.OptionType;

public class HotSpotTTYStreamProvider_OptionDescriptors implements OptionDescriptors {
    @Override
    public OptionDescriptor get(String value) {
        switch (value) {
        // CheckStyle: stop line length check
        case "LogFile": {
            return OptionDescriptor.create(
                /*name*/ "LogFile",
                /*optionType*/ OptionType.Expert,
                /*optionValueType*/ String.class,
                /*help*/ "File to which logging is sent.  A %p in the name will be replaced with a string identifying the process, usually the process id and %t will be replaced by System.currentTimeMillis().",
                /*declaringClass*/ HotSpotTTYStreamProvider.Options.class,
                /*fieldName*/ "LogFile",
                /*option*/ HotSpotTTYStreamProvider.Options.LogFile);
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
                    case 0: return get("LogFile");
                }
                throw new NoSuchElementException();
            }
        };
    }
}

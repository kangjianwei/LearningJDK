// CheckStyle: stop header check
// CheckStyle: stop line length check
// GENERATED CONTENT - DO NOT EDIT
// Source: BootstrapWatchDog.java
package org.graalvm.compiler.hotspot;

import java.util.*;
import org.graalvm.compiler.options.*;
import org.graalvm.compiler.options.OptionType;

public class BootstrapWatchDog_OptionDescriptors implements OptionDescriptors {
    @Override
    public OptionDescriptor get(String value) {
        switch (value) {
        // CheckStyle: stop line length check
        case "BootstrapTimeout": {
            return OptionDescriptor.create(
                /*name*/ "BootstrapTimeout",
                /*optionType*/ OptionType.Debug,
                /*optionValueType*/ Double.class,
                /*help*/ "Maximum time in minutes to spend bootstrapping (0 to disable this limit).",
                /*declaringClass*/ BootstrapWatchDog.Options.class,
                /*fieldName*/ "BootstrapTimeout",
                /*option*/ BootstrapWatchDog.Options.BootstrapTimeout);
        }
        case "BootstrapWatchDogCriticalRateRatio": {
            return OptionDescriptor.create(
                /*name*/ "BootstrapWatchDogCriticalRateRatio",
                /*optionType*/ OptionType.Debug,
                /*optionValueType*/ Double.class,
                /*help*/ "Ratio of the maximum compilation rate below which the bootstrap compilation rate must not fall (0 or less disables monitoring).",
                /*declaringClass*/ BootstrapWatchDog.Options.class,
                /*fieldName*/ "BootstrapWatchDogCriticalRateRatio",
                /*option*/ BootstrapWatchDog.Options.BootstrapWatchDogCriticalRateRatio);
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
                    case 0: return get("BootstrapTimeout");
                    case 1: return get("BootstrapWatchDogCriticalRateRatio");
                }
                throw new NoSuchElementException();
            }
        };
    }
}

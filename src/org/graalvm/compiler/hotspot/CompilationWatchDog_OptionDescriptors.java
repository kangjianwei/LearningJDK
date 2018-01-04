// CheckStyle: stop header check
// CheckStyle: stop line length check
// GENERATED CONTENT - DO NOT EDIT
// Source: CompilationWatchDog.java
package org.graalvm.compiler.hotspot;

import java.util.*;
import org.graalvm.compiler.options.*;
import org.graalvm.compiler.options.OptionType;

public class CompilationWatchDog_OptionDescriptors implements OptionDescriptors {
    @Override
    public OptionDescriptor get(String value) {
        switch (value) {
        // CheckStyle: stop line length check
        case "CompilationWatchDogStackTraceInterval": {
            return OptionDescriptor.create(
                /*name*/ "CompilationWatchDogStackTraceInterval",
                /*optionType*/ OptionType.Debug,
                /*optionValueType*/ Double.class,
                /*help*/ "Interval in seconds between a watch dog reporting stack traces for long running compilations.",
                /*declaringClass*/ CompilationWatchDog.Options.class,
                /*fieldName*/ "CompilationWatchDogStackTraceInterval",
                /*option*/ CompilationWatchDog.Options.CompilationWatchDogStackTraceInterval);
        }
        case "CompilationWatchDogStartDelay": {
            return OptionDescriptor.create(
                /*name*/ "CompilationWatchDogStartDelay",
                /*optionType*/ OptionType.Debug,
                /*optionValueType*/ Double.class,
                /*help*/ "Delay in seconds before watch dog monitoring a compilation (0 disables monitoring).",
                /*declaringClass*/ CompilationWatchDog.Options.class,
                /*fieldName*/ "CompilationWatchDogStartDelay",
                /*option*/ CompilationWatchDog.Options.CompilationWatchDogStartDelay);
        }
        case "NonFatalIdenticalCompilationSnapshots": {
            return OptionDescriptor.create(
                /*name*/ "NonFatalIdenticalCompilationSnapshots",
                /*optionType*/ OptionType.Debug,
                /*optionValueType*/ Integer.class,
                /*help*/ "Number of contiguous identical compiler thread stack traces allowed before the VM exits on the basis of a stuck compilation.",
                /*declaringClass*/ CompilationWatchDog.Options.class,
                /*fieldName*/ "NonFatalIdenticalCompilationSnapshots",
                /*option*/ CompilationWatchDog.Options.NonFatalIdenticalCompilationSnapshots);
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
                    case 0: return get("CompilationWatchDogStackTraceInterval");
                    case 1: return get("CompilationWatchDogStartDelay");
                    case 2: return get("NonFatalIdenticalCompilationSnapshots");
                }
                throw new NoSuchElementException();
            }
        };
    }
}

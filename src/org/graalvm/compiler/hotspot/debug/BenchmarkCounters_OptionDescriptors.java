// CheckStyle: stop header check
// CheckStyle: stop line length check
// GENERATED CONTENT - DO NOT EDIT
// Source: BenchmarkCounters.java
package org.graalvm.compiler.hotspot.debug;

import java.util.*;
import org.graalvm.compiler.options.*;
import org.graalvm.compiler.options.OptionType;

public class BenchmarkCounters_OptionDescriptors implements OptionDescriptors {
    @Override
    public OptionDescriptor get(String value) {
        switch (value) {
        // CheckStyle: stop line length check
        case "BenchmarkCountersDumpDynamic": {
            return OptionDescriptor.create(
                /*name*/ "BenchmarkCountersDumpDynamic",
                /*optionType*/ OptionType.Debug,
                /*optionValueType*/ Boolean.class,
                /*help*/ "Dump dynamic counters",
                /*declaringClass*/ BenchmarkCounters.Options.class,
                /*fieldName*/ "BenchmarkCountersDumpDynamic",
                /*option*/ BenchmarkCounters.Options.BenchmarkCountersDumpDynamic);
        }
        case "BenchmarkCountersDumpStatic": {
            return OptionDescriptor.create(
                /*name*/ "BenchmarkCountersDumpStatic",
                /*optionType*/ OptionType.Debug,
                /*optionValueType*/ Boolean.class,
                /*help*/ "Dump static counters",
                /*declaringClass*/ BenchmarkCounters.Options.class,
                /*fieldName*/ "BenchmarkCountersDumpStatic",
                /*option*/ BenchmarkCounters.Options.BenchmarkCountersDumpStatic);
        }
        case "BenchmarkCountersFile": {
            return OptionDescriptor.create(
                /*name*/ "BenchmarkCountersFile",
                /*optionType*/ OptionType.Debug,
                /*optionValueType*/ String.class,
                /*help*/ "File to which benchmark counters are dumped. A CSV format is used if the file ends with .csv otherwise a more human readable format is used. The fields in the CSV format are: category, group, name, value",
                /*declaringClass*/ BenchmarkCounters.Options.class,
                /*fieldName*/ "BenchmarkCountersFile",
                /*option*/ BenchmarkCounters.Options.BenchmarkCountersFile);
        }
        case "BenchmarkDynamicCounters": {
            return OptionDescriptor.create(
                /*name*/ "BenchmarkDynamicCounters",
                /*optionType*/ OptionType.Debug,
                /*optionValueType*/ String.class,
                /*help*/ "Turn on the benchmark counters, and listen for specific patterns on System.out/System.err.",
                /*extraHelp*/ new String[] {
                         "The format of this option is:",
                         "",
                         "  (err|out),start pattern,end pattern",
                         "  ",
                         "You can use \"~\" to match 1 or more digits.",
                         "Examples:",
                         "",
                         "  err, starting =====, PASSED in",
                         "  out,Iteration ~ (~s) begins:,Iteration ~ (~s) ends:",
                         "  ",
                         "The first pattern matches DaCapo output and the second matches SPECjvm2008 output.",
                         "",
                         "As a more detailed example, here are the options to use for getting statistics",
                         "about allocations within the DaCapo pmd benchmark:",
                         "",
                         "  -XX:JVMCICounterSize=<value> -XX:-JVMCICountersExcludeCompiler \\",
                         "  -Dgraal.BenchmarkDynamicCounters=\"err, starting ====, PASSED in \" \\",
                         "  -Dgraal.ProfileAllocations=true",
                         "  ",
                         "The JVMCICounterSize value depends on the granularity of the profiling -",
                         "10000 should be sufficient. Omit JVMCICountersExcludeCompiler to exclude",
                         "counting allocations on the compiler threads.",
                         "The counters can be further configured by the ProfileAllocationsContext option.",
                              },
                /*declaringClass*/ BenchmarkCounters.Options.class,
                /*fieldName*/ "BenchmarkDynamicCounters",
                /*option*/ BenchmarkCounters.Options.BenchmarkDynamicCounters);
        }
        case "DynamicCountersPrintGroupSeparator": {
            return OptionDescriptor.create(
                /*name*/ "DynamicCountersPrintGroupSeparator",
                /*optionType*/ OptionType.Debug,
                /*optionValueType*/ Boolean.class,
                /*help*/ "Use grouping separators for number printing",
                /*declaringClass*/ BenchmarkCounters.Options.class,
                /*fieldName*/ "DynamicCountersPrintGroupSeparator",
                /*option*/ BenchmarkCounters.Options.DynamicCountersPrintGroupSeparator);
        }
        case "GenericDynamicCounters": {
            return OptionDescriptor.create(
                /*name*/ "GenericDynamicCounters",
                /*optionType*/ OptionType.Debug,
                /*optionValueType*/ Boolean.class,
                /*help*/ "Turn on the benchmark counters, and displays the results on VM shutdown",
                /*declaringClass*/ BenchmarkCounters.Options.class,
                /*fieldName*/ "GenericDynamicCounters",
                /*option*/ BenchmarkCounters.Options.GenericDynamicCounters);
        }
        case "TimedDynamicCounters": {
            return OptionDescriptor.create(
                /*name*/ "TimedDynamicCounters",
                /*optionType*/ OptionType.Debug,
                /*optionValueType*/ Integer.class,
                /*help*/ "Turn on the benchmark counters, and displays the results every n milliseconds",
                /*declaringClass*/ BenchmarkCounters.Options.class,
                /*fieldName*/ "TimedDynamicCounters",
                /*option*/ BenchmarkCounters.Options.TimedDynamicCounters);
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
                return i < 7;
            }
            @Override
            public OptionDescriptor next() {
                switch (i++) {
                    case 0: return get("BenchmarkCountersDumpDynamic");
                    case 1: return get("BenchmarkCountersDumpStatic");
                    case 2: return get("BenchmarkCountersFile");
                    case 3: return get("BenchmarkDynamicCounters");
                    case 4: return get("DynamicCountersPrintGroupSeparator");
                    case 5: return get("GenericDynamicCounters");
                    case 6: return get("TimedDynamicCounters");
                }
                throw new NoSuchElementException();
            }
        };
    }
}

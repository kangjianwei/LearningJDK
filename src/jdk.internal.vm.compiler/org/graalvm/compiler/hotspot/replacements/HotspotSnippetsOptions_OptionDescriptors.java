// CheckStyle: stop header check
// CheckStyle: stop line length check
// GENERATED CONTENT - DO NOT EDIT
// Source: HotspotSnippetsOptions.java
package org.graalvm.compiler.hotspot.replacements;

import java.util.*;
import org.graalvm.compiler.options.*;
import org.graalvm.compiler.options.OptionType;

public class HotspotSnippetsOptions_OptionDescriptors implements OptionDescriptors {
    @Override
    public OptionDescriptor get(String value) {
        switch (value) {
        // CheckStyle: stop line length check
        case "LoadExceptionObjectInVM": {
            return OptionDescriptor.create(
                /*name*/ "LoadExceptionObjectInVM",
                /*optionType*/ OptionType.Debug,
                /*optionValueType*/ Boolean.class,
                /*help*/ "Use a VM runtime call to load and clear the exception object from the thread at the start of a compiled exception handler.",
                /*declaringClass*/ HotspotSnippetsOptions.class,
                /*fieldName*/ "LoadExceptionObjectInVM",
                /*option*/ HotspotSnippetsOptions.LoadExceptionObjectInVM);
        }
        case "ProfileAllocations": {
            return OptionDescriptor.create(
                /*name*/ "ProfileAllocations",
                /*optionType*/ OptionType.Debug,
                /*optionValueType*/ Boolean.class,
                /*help*/ "Enable profiling of allocation sites.",
                /*declaringClass*/ HotspotSnippetsOptions.class,
                /*fieldName*/ "ProfileAllocations",
                /*option*/ HotspotSnippetsOptions.ProfileAllocations);
        }
        case "ProfileAllocationsContext": {
            return OptionDescriptor.create(
                /*name*/ "ProfileAllocationsContext",
                /*optionType*/ OptionType.Debug,
                /*optionValueType*/ org.graalvm.compiler.hotspot.replacements.NewObjectSnippets.ProfileContext.class,
                /*help*/ "Control the naming and granularity of the counters when using ProfileAllocations.",
                /*extraHelp*/ new String[] {
                         "The accepted values are:",
                         "        AllocatingMethod - a counter per method",
                         "         InstanceOrArray - one counter for all instance allocations and",
                         "                           one counter for all array allocations ",
                         "           AllocatedType - one counter per allocated type",
                         "  AllocatedTypesInMethod - one counter per allocated type, per method",
                         " ",
                              },
                /*declaringClass*/ HotspotSnippetsOptions.class,
                /*fieldName*/ "ProfileAllocationsContext",
                /*option*/ HotspotSnippetsOptions.ProfileAllocationsContext);
        }
        case "ProfileMonitors": {
            return OptionDescriptor.create(
                /*name*/ "ProfileMonitors",
                /*optionType*/ OptionType.Debug,
                /*optionValueType*/ Boolean.class,
                /*help*/ "Enable profiling of monitor operations.",
                /*declaringClass*/ HotspotSnippetsOptions.class,
                /*fieldName*/ "ProfileMonitors",
                /*option*/ HotspotSnippetsOptions.ProfileMonitors);
        }
        case "SimpleFastInflatedLocking": {
            return OptionDescriptor.create(
                /*name*/ "SimpleFastInflatedLocking",
                /*optionType*/ OptionType.Expert,
                /*optionValueType*/ Boolean.class,
                /*help*/ "Handle simple cases for inflated monitors in the fast-path.",
                /*declaringClass*/ HotspotSnippetsOptions.class,
                /*fieldName*/ "SimpleFastInflatedLocking",
                /*option*/ HotspotSnippetsOptions.SimpleFastInflatedLocking);
        }
        case "TraceMonitorsMethodFilter": {
            return OptionDescriptor.create(
                /*name*/ "TraceMonitorsMethodFilter",
                /*optionType*/ OptionType.Debug,
                /*optionValueType*/ String.class,
                /*help*/ "Trace monitor operations in methods whose fully qualified name contains this substring.",
                /*declaringClass*/ HotspotSnippetsOptions.class,
                /*fieldName*/ "TraceMonitorsMethodFilter",
                /*option*/ HotspotSnippetsOptions.TraceMonitorsMethodFilter);
        }
        case "TraceMonitorsTypeFilter": {
            return OptionDescriptor.create(
                /*name*/ "TraceMonitorsTypeFilter",
                /*optionType*/ OptionType.Debug,
                /*optionValueType*/ String.class,
                /*help*/ "Trace monitor operations on objects whose type contains this substring.",
                /*declaringClass*/ HotspotSnippetsOptions.class,
                /*fieldName*/ "TraceMonitorsTypeFilter",
                /*option*/ HotspotSnippetsOptions.TraceMonitorsTypeFilter);
        }
        case "TypeCheckMaxHints": {
            return OptionDescriptor.create(
                /*name*/ "TypeCheckMaxHints",
                /*optionType*/ OptionType.Expert,
                /*optionValueType*/ Integer.class,
                /*help*/ "The maximum number of profiled types that will be used when compiling a profiled type check. Note that TypeCheckMinProfileHitProbability also influences whether profiling info is used in compiled type checks.",
                /*declaringClass*/ HotspotSnippetsOptions.class,
                /*fieldName*/ "TypeCheckMaxHints",
                /*option*/ HotspotSnippetsOptions.TypeCheckMaxHints);
        }
        case "TypeCheckMinProfileHitProbability": {
            return OptionDescriptor.create(
                /*name*/ "TypeCheckMinProfileHitProbability",
                /*optionType*/ OptionType.Expert,
                /*optionValueType*/ Double.class,
                /*help*/ "If the probability that a type check will hit one the profiled types (up to TypeCheckMaxHints) is below this value, the type check will be compiled without profiling info",
                /*declaringClass*/ HotspotSnippetsOptions.class,
                /*fieldName*/ "TypeCheckMinProfileHitProbability",
                /*option*/ HotspotSnippetsOptions.TypeCheckMinProfileHitProbability);
        }
        case "VerifyBalancedMonitors": {
            return OptionDescriptor.create(
                /*name*/ "VerifyBalancedMonitors",
                /*optionType*/ OptionType.Debug,
                /*optionValueType*/ Boolean.class,
                /*help*/ "Emit extra code to dynamically check monitor operations are balanced.",
                /*declaringClass*/ HotspotSnippetsOptions.class,
                /*fieldName*/ "VerifyBalancedMonitors",
                /*option*/ HotspotSnippetsOptions.VerifyBalancedMonitors);
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
                return i < 10;
            }
            @Override
            public OptionDescriptor next() {
                switch (i++) {
                    case 0: return get("LoadExceptionObjectInVM");
                    case 1: return get("ProfileAllocations");
                    case 2: return get("ProfileAllocationsContext");
                    case 3: return get("ProfileMonitors");
                    case 4: return get("SimpleFastInflatedLocking");
                    case 5: return get("TraceMonitorsMethodFilter");
                    case 6: return get("TraceMonitorsTypeFilter");
                    case 7: return get("TypeCheckMaxHints");
                    case 8: return get("TypeCheckMinProfileHitProbability");
                    case 9: return get("VerifyBalancedMonitors");
                }
                throw new NoSuchElementException();
            }
        };
    }
}

// CheckStyle: stop header check
// CheckStyle: stop line length check
// GENERATED CONTENT - DO NOT EDIT
// Source: HotSpotAOTProfilingPlugin.java
package org.graalvm.compiler.hotspot.meta;

import java.util.*;
import org.graalvm.compiler.options.*;
import org.graalvm.compiler.options.OptionType;

public class HotSpotAOTProfilingPlugin_OptionDescriptors implements OptionDescriptors {
    @Override
    public OptionDescriptor get(String value) {
        switch (value) {
        // CheckStyle: stop line length check
        case "TierABackedgeNotifyFreqLog": {
            return OptionDescriptor.create(
                /*name*/ "TierABackedgeNotifyFreqLog",
                /*optionType*/ OptionType.Expert,
                /*optionValueType*/ Integer.class,
                /*help*/ "Backedge notification frequency",
                /*declaringClass*/ HotSpotAOTProfilingPlugin.Options.class,
                /*fieldName*/ "TierABackedgeNotifyFreqLog",
                /*option*/ HotSpotAOTProfilingPlugin.Options.TierABackedgeNotifyFreqLog);
        }
        case "TierABackedgeProfileProbabilityLog": {
            return OptionDescriptor.create(
                /*name*/ "TierABackedgeProfileProbabilityLog",
                /*optionType*/ OptionType.Expert,
                /*optionValueType*/ Integer.class,
                /*help*/ "Backedge profile probability",
                /*declaringClass*/ HotSpotAOTProfilingPlugin.Options.class,
                /*fieldName*/ "TierABackedgeProfileProbabilityLog",
                /*option*/ HotSpotAOTProfilingPlugin.Options.TierABackedgeProfileProbabilityLog);
        }
        case "TierAInvokeInlineeNotifyFreqLog": {
            return OptionDescriptor.create(
                /*name*/ "TierAInvokeInlineeNotifyFreqLog",
                /*optionType*/ OptionType.Expert,
                /*optionValueType*/ Integer.class,
                /*help*/ "Inlinee invocation notification frequency (-1 means count, but do not notify)",
                /*declaringClass*/ HotSpotAOTProfilingPlugin.Options.class,
                /*fieldName*/ "TierAInvokeInlineeNotifyFreqLog",
                /*option*/ HotSpotAOTProfilingPlugin.Options.TierAInvokeInlineeNotifyFreqLog);
        }
        case "TierAInvokeNotifyFreqLog": {
            return OptionDescriptor.create(
                /*name*/ "TierAInvokeNotifyFreqLog",
                /*optionType*/ OptionType.Expert,
                /*optionValueType*/ Integer.class,
                /*help*/ "Invocation notification frequency",
                /*declaringClass*/ HotSpotAOTProfilingPlugin.Options.class,
                /*fieldName*/ "TierAInvokeNotifyFreqLog",
                /*option*/ HotSpotAOTProfilingPlugin.Options.TierAInvokeNotifyFreqLog);
        }
        case "TierAInvokeProfileProbabilityLog": {
            return OptionDescriptor.create(
                /*name*/ "TierAInvokeProfileProbabilityLog",
                /*optionType*/ OptionType.Expert,
                /*optionValueType*/ Integer.class,
                /*help*/ "Invocation profile probability",
                /*declaringClass*/ HotSpotAOTProfilingPlugin.Options.class,
                /*fieldName*/ "TierAInvokeProfileProbabilityLog",
                /*option*/ HotSpotAOTProfilingPlugin.Options.TierAInvokeProfileProbabilityLog);
        }
        case "TieredAOT": {
            return OptionDescriptor.create(
                /*name*/ "TieredAOT",
                /*optionType*/ OptionType.User,
                /*optionValueType*/ Boolean.class,
                /*help*/ "Do profiling and callbacks to tiered runtime",
                /*declaringClass*/ HotSpotAOTProfilingPlugin.Options.class,
                /*fieldName*/ "TieredAOT",
                /*option*/ HotSpotAOTProfilingPlugin.Options.TieredAOT);
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
                return i < 6;
            }
            @Override
            public OptionDescriptor next() {
                switch (i++) {
                    case 0: return get("TierABackedgeNotifyFreqLog");
                    case 1: return get("TierABackedgeProfileProbabilityLog");
                    case 2: return get("TierAInvokeInlineeNotifyFreqLog");
                    case 3: return get("TierAInvokeNotifyFreqLog");
                    case 4: return get("TierAInvokeProfileProbabilityLog");
                    case 5: return get("TieredAOT");
                }
                throw new NoSuchElementException();
            }
        };
    }
}

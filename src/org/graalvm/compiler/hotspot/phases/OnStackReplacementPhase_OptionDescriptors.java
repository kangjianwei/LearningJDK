// CheckStyle: stop header check
// CheckStyle: stop line length check
// GENERATED CONTENT - DO NOT EDIT
// Source: OnStackReplacementPhase.java
package org.graalvm.compiler.hotspot.phases;

import java.util.*;
import org.graalvm.compiler.options.*;
import org.graalvm.compiler.options.OptionType;

public class OnStackReplacementPhase_OptionDescriptors implements OptionDescriptors {
    @Override
    public OptionDescriptor get(String value) {
        switch (value) {
        // CheckStyle: stop line length check
        case "DeoptAfterOSR": {
            return OptionDescriptor.create(
                /*name*/ "DeoptAfterOSR",
                /*optionType*/ OptionType.Debug,
                /*optionValueType*/ Boolean.class,
                /*help*/ "Deoptimize OSR compiled code when the OSR entry loop is finished if there is no mature profile available for the rest of the method.",
                /*declaringClass*/ OnStackReplacementPhase.Options.class,
                /*fieldName*/ "DeoptAfterOSR",
                /*option*/ OnStackReplacementPhase.Options.DeoptAfterOSR);
        }
        case "SupportOSRWithLocks": {
            return OptionDescriptor.create(
                /*name*/ "SupportOSRWithLocks",
                /*optionType*/ OptionType.Debug,
                /*optionValueType*/ Boolean.class,
                /*help*/ "Support OSR compilations with locks. If DeoptAfterOSR is true we can per definition not have unbalaced enter/extis mappings. If DeoptAfterOSR is false insert artificial monitor enters after the OSRStart to have balanced enter/exits in the graph.",
                /*declaringClass*/ OnStackReplacementPhase.Options.class,
                /*fieldName*/ "SupportOSRWithLocks",
                /*option*/ OnStackReplacementPhase.Options.SupportOSRWithLocks);
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
                    case 0: return get("DeoptAfterOSR");
                    case 1: return get("SupportOSRWithLocks");
                }
                throw new NoSuchElementException();
            }
        };
    }
}

// CheckStyle: stop header check
// CheckStyle: stop line length check
// GENERATED CONTENT - DO NOT EDIT
// Source: GraphUtil.java
package org.graalvm.compiler.nodes.util;

import java.util.*;
import org.graalvm.compiler.options.*;
import org.graalvm.compiler.options.OptionType;

public class GraphUtil_OptionDescriptors implements OptionDescriptors {
    @Override
    public OptionDescriptor get(String value) {
        switch (value) {
        // CheckStyle: stop line length check
        case "VerifyKillCFGUnusedNodes": {
            return OptionDescriptor.create(
                /*name*/ "VerifyKillCFGUnusedNodes",
                /*optionType*/ OptionType.Debug,
                /*optionValueType*/ Boolean.class,
                /*help*/ "Verify that there are no new unused nodes when performing killCFG",
                /*declaringClass*/ GraphUtil.Options.class,
                /*fieldName*/ "VerifyKillCFGUnusedNodes",
                /*option*/ GraphUtil.Options.VerifyKillCFGUnusedNodes);
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
                    case 0: return get("VerifyKillCFGUnusedNodes");
                }
                throw new NoSuchElementException();
            }
        };
    }
}

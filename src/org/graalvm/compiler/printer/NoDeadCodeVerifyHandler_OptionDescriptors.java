// CheckStyle: stop header check
// CheckStyle: stop line length check
// GENERATED CONTENT - DO NOT EDIT
// Source: NoDeadCodeVerifyHandler.java
package org.graalvm.compiler.printer;

import java.util.*;
import org.graalvm.compiler.options.*;
import org.graalvm.compiler.options.OptionType;

public class NoDeadCodeVerifyHandler_OptionDescriptors implements OptionDescriptors {
    @Override
    public OptionDescriptor get(String value) {
        switch (value) {
        // CheckStyle: stop line length check
        case "NDCV": {
            return OptionDescriptor.create(
                /*name*/ "NDCV",
                /*optionType*/ OptionType.Debug,
                /*optionValueType*/ Integer.class,
                /*help*/ "Run level for NoDeadCodeVerifyHandler (0 = off, 1 = info, 2 = verbose, 3 = fatal)",
                /*declaringClass*/ NoDeadCodeVerifyHandler.Options.class,
                /*fieldName*/ "NDCV",
                /*option*/ NoDeadCodeVerifyHandler.Options.NDCV);
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
                    case 0: return get("NDCV");
                }
                throw new NoSuchElementException();
            }
        };
    }
}

// CheckStyle: stop header check
// CheckStyle: stop line length check
// GENERATED CONTENT - DO NOT EDIT
// Source: StubOptions.java
package org.graalvm.compiler.hotspot.stubs;

import java.util.*;
import org.graalvm.compiler.options.*;
import org.graalvm.compiler.options.OptionType;

public class StubOptions_OptionDescriptors implements OptionDescriptors {
    @Override
    public OptionDescriptor get(String value) {
        switch (value) {
        // CheckStyle: stop line length check
        case "ForceUseOfNewInstanceStub": {
            return OptionDescriptor.create(
                /*name*/ "ForceUseOfNewInstanceStub",
                /*optionType*/ OptionType.Debug,
                /*optionValueType*/ Boolean.class,
                /*help*/ "Force non-array object allocation to always use the slow path.",
                /*declaringClass*/ StubOptions.class,
                /*fieldName*/ "ForceUseOfNewInstanceStub",
                /*option*/ StubOptions.ForceUseOfNewInstanceStub);
        }
        case "TraceExceptionHandlerStub": {
            return OptionDescriptor.create(
                /*name*/ "TraceExceptionHandlerStub",
                /*optionType*/ OptionType.Debug,
                /*optionValueType*/ Boolean.class,
                /*help*/ "Trace execution of stub used to handle an exception thrown by a callee.",
                /*declaringClass*/ StubOptions.class,
                /*fieldName*/ "TraceExceptionHandlerStub",
                /*option*/ StubOptions.TraceExceptionHandlerStub);
        }
        case "TraceNewArrayStub": {
            return OptionDescriptor.create(
                /*name*/ "TraceNewArrayStub",
                /*optionType*/ OptionType.Debug,
                /*optionValueType*/ Boolean.class,
                /*help*/ "Trace execution of slow path stub for array allocation.",
                /*declaringClass*/ StubOptions.class,
                /*fieldName*/ "TraceNewArrayStub",
                /*option*/ StubOptions.TraceNewArrayStub);
        }
        case "TraceNewInstanceStub": {
            return OptionDescriptor.create(
                /*name*/ "TraceNewInstanceStub",
                /*optionType*/ OptionType.Debug,
                /*optionValueType*/ Boolean.class,
                /*help*/ "Trace execution of slow path stub for non-array object allocation.",
                /*declaringClass*/ StubOptions.class,
                /*fieldName*/ "TraceNewInstanceStub",
                /*option*/ StubOptions.TraceNewInstanceStub);
        }
        case "TraceUnwindStub": {
            return OptionDescriptor.create(
                /*name*/ "TraceUnwindStub",
                /*optionType*/ OptionType.Debug,
                /*optionValueType*/ Boolean.class,
                /*help*/ "Trace execution of the stub that routes an exception to a handler in the calling frame.",
                /*declaringClass*/ StubOptions.class,
                /*fieldName*/ "TraceUnwindStub",
                /*option*/ StubOptions.TraceUnwindStub);
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
                return i < 5;
            }
            @Override
            public OptionDescriptor next() {
                switch (i++) {
                    case 0: return get("ForceUseOfNewInstanceStub");
                    case 1: return get("TraceExceptionHandlerStub");
                    case 2: return get("TraceNewArrayStub");
                    case 3: return get("TraceNewInstanceStub");
                    case 4: return get("TraceUnwindStub");
                }
                throw new NoSuchElementException();
            }
        };
    }
}

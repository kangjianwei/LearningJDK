// CheckStyle: stop header check
// CheckStyle: stop line length check
// GENERATED CONTENT - DO NOT EDIT
// Source: Graph.java
package org.graalvm.compiler.graph;

import java.util.*;
import org.graalvm.compiler.options.*;
import org.graalvm.compiler.options.OptionType;

public class Graph_OptionDescriptors implements OptionDescriptors {
    @Override
    public OptionDescriptor get(String value) {
        switch (value) {
        // CheckStyle: stop line length check
        case "GraphCompressionThreshold": {
            return OptionDescriptor.create(
                /*name*/ "GraphCompressionThreshold",
                /*optionType*/ OptionType.Debug,
                /*optionValueType*/ Integer.class,
                /*help*/ "Graal graph compression is performed when percent of live nodes falls below this value",
                /*declaringClass*/ Graph.Options.class,
                /*fieldName*/ "GraphCompressionThreshold",
                /*option*/ Graph.Options.GraphCompressionThreshold);
        }
        case "VerifyGraalGraphEdges": {
            return OptionDescriptor.create(
                /*name*/ "VerifyGraalGraphEdges",
                /*optionType*/ OptionType.Debug,
                /*optionValueType*/ Boolean.class,
                /*help*/ "Perform expensive verification of graph inputs, usages, successors and predecessors",
                /*declaringClass*/ Graph.Options.class,
                /*fieldName*/ "VerifyGraalGraphEdges",
                /*option*/ Graph.Options.VerifyGraalGraphEdges);
        }
        case "VerifyGraalGraphs": {
            return OptionDescriptor.create(
                /*name*/ "VerifyGraalGraphs",
                /*optionType*/ OptionType.Debug,
                /*optionValueType*/ Boolean.class,
                /*help*/ "Verify graphs often during compilation when assertions are turned on",
                /*declaringClass*/ Graph.Options.class,
                /*fieldName*/ "VerifyGraalGraphs",
                /*option*/ Graph.Options.VerifyGraalGraphs);
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
                    case 0: return get("GraphCompressionThreshold");
                    case 1: return get("VerifyGraalGraphEdges");
                    case 2: return get("VerifyGraalGraphs");
                }
                throw new NoSuchElementException();
            }
        };
    }
}

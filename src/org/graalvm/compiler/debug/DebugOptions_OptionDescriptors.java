// CheckStyle: stop header check
// CheckStyle: stop line length check
// GENERATED CONTENT - DO NOT EDIT
// Source: DebugOptions.java
package org.graalvm.compiler.debug;

import java.util.*;
import org.graalvm.compiler.options.*;
import org.graalvm.compiler.options.OptionType;

public class DebugOptions_OptionDescriptors implements OptionDescriptors {
    @Override
    public OptionDescriptor get(String value) {
        switch (value) {
        // CheckStyle: stop line length check
        case "AggregatedMetricsFile": {
            return OptionDescriptor.create(
                /*name*/ "AggregatedMetricsFile",
                /*optionType*/ OptionType.Debug,
                /*optionValueType*/ String.class,
                /*help*/ "File to which aggregated metrics are dumped at shutdown. A CSV format is used if the file ends with .csv otherwise a more human readable format is used. If not specified, metrics are dumped to the console.",
                /*declaringClass*/ DebugOptions.class,
                /*fieldName*/ "AggregatedMetricsFile",
                /*option*/ DebugOptions.AggregatedMetricsFile);
        }
        case "BootstrapInitializeOnly": {
            return OptionDescriptor.create(
                /*name*/ "BootstrapInitializeOnly",
                /*optionType*/ OptionType.Debug,
                /*optionValueType*/ Boolean.class,
                /*help*/ "Do not compile anything on bootstrap but just initialize the compiler.",
                /*declaringClass*/ DebugOptions.class,
                /*fieldName*/ "BootstrapInitializeOnly",
                /*option*/ DebugOptions.BootstrapInitializeOnly);
        }
        case "CanonicalGraphStringsCheckConstants": {
            return OptionDescriptor.create(
                /*name*/ "CanonicalGraphStringsCheckConstants",
                /*optionType*/ OptionType.Debug,
                /*optionValueType*/ Boolean.class,
                /*help*/ "Exclude virtual nodes when dumping canonical text for graphs.",
                /*declaringClass*/ DebugOptions.class,
                /*fieldName*/ "CanonicalGraphStringsCheckConstants",
                /*option*/ DebugOptions.CanonicalGraphStringsCheckConstants);
        }
        case "CanonicalGraphStringsExcludeVirtuals": {
            return OptionDescriptor.create(
                /*name*/ "CanonicalGraphStringsExcludeVirtuals",
                /*optionType*/ OptionType.Debug,
                /*optionValueType*/ Boolean.class,
                /*help*/ "Exclude virtual nodes when dumping canonical text for graphs.",
                /*declaringClass*/ DebugOptions.class,
                /*fieldName*/ "CanonicalGraphStringsExcludeVirtuals",
                /*option*/ DebugOptions.CanonicalGraphStringsExcludeVirtuals);
        }
        case "CanonicalGraphStringsRemoveIdentities": {
            return OptionDescriptor.create(
                /*name*/ "CanonicalGraphStringsRemoveIdentities",
                /*optionType*/ OptionType.Debug,
                /*optionValueType*/ Boolean.class,
                /*help*/ "Attempts to remove object identity hashes when dumping canonical text for graphs.",
                /*declaringClass*/ DebugOptions.class,
                /*fieldName*/ "CanonicalGraphStringsRemoveIdentities",
                /*option*/ DebugOptions.CanonicalGraphStringsRemoveIdentities);
        }
        case "ClearMetricsAfterBootstrap": {
            return OptionDescriptor.create(
                /*name*/ "ClearMetricsAfterBootstrap",
                /*optionType*/ OptionType.Debug,
                /*optionValueType*/ Boolean.class,
                /*help*/ "Clear the debug metrics after bootstrap.",
                /*declaringClass*/ DebugOptions.class,
                /*fieldName*/ "ClearMetricsAfterBootstrap",
                /*option*/ DebugOptions.ClearMetricsAfterBootstrap);
        }
        case "Count": {
            return OptionDescriptor.create(
                /*name*/ "Count",
                /*optionType*/ OptionType.Debug,
                /*optionValueType*/ String.class,
                /*help*/ "Pattern for specifying scopes in which counters are enabled. See the Dump option for the pattern syntax. An empty value enables all counters unconditionally.",
                /*declaringClass*/ DebugOptions.class,
                /*fieldName*/ "Count",
                /*option*/ DebugOptions.Count);
        }
        case "Counters": {
            return OptionDescriptor.create(
                /*name*/ "Counters",
                /*optionType*/ OptionType.Debug,
                /*optionValueType*/ String.class,
                /*help*/ "Comma separated names of counters that are enabled irrespective of the value for Count option. An empty value enables all counters unconditionally.",
                /*declaringClass*/ DebugOptions.class,
                /*fieldName*/ "Counters",
                /*option*/ DebugOptions.Counters);
        }
        case "DebugStubsAndSnippets": {
            return OptionDescriptor.create(
                /*name*/ "DebugStubsAndSnippets",
                /*optionType*/ OptionType.Debug,
                /*optionValueType*/ Boolean.class,
                /*help*/ "Enable debug output for stub code generation and snippet preparation.",
                /*declaringClass*/ DebugOptions.class,
                /*fieldName*/ "DebugStubsAndSnippets",
                /*option*/ DebugOptions.DebugStubsAndSnippets);
        }
        case "Dump": {
            return OptionDescriptor.create(
                /*name*/ "Dump",
                /*optionType*/ OptionType.Debug,
                /*optionValueType*/ String.class,
                /*help*/ "Filter pattern for specifying scopes in which dumping is enabled.",
                /*extraHelp*/ new String[] {
                         "",
                         "A filter is a list of comma-separated terms of the form:",
                         " ",
                         "  <pattern>[:<level>]",
                         " ",
                         "If <pattern> contains a \"*\" or \"?\" character, it is interpreted as a glob pattern.",
                         "Otherwise, it is interpreted as a substring. If <pattern> is empty, it",
                         "matches every scope. If :<level> is omitted, it defaults to 1. The term",
                         "~<pattern> is a shorthand for <pattern>:0 to disable a debug facility for a pattern.",
                         "",
                         "The default log level is 0 (disabled). Terms with an empty pattern set",
                         "the default log level to the specified value. The last",
                         "matching term with a non-empty pattern selects the level specified. If",
                         "no term matches, the log level is the default level. A filter with no",
                         "terms matches every scope with a log level of 1.",
                         "",
                         "Examples of debug filters:",
                         "--------- ",
                         "  (empty string)",
                         "",
                         "  Matches any scope with level 1.",
                         "--------- ",
                         "  :1",
                         "",
                         "  Matches any scope with level 1.",
                         "--------- ",
                         "  *",
                         "",
                         "  Matches any scope with level 1.",
                         "--------- ",
                         "  CodeGen,CodeInstall",
                         "",
                         "  Matches scopes containing \"CodeGen\" or \"CodeInstall\", both with level 1.",
                         "--------- ",
                         "  CodeGen:2,CodeInstall:1",
                         "",
                         "  Matches scopes containing \"CodeGen\" with level 2, or \"CodeInstall\" with level 1.",
                         "---------",
                         "  Outer:2,Inner:0}",
                         "",
                         "  Matches scopes containing \"Outer\" with log level 2, or \"Inner\" with log level 0. If the scope",
                         "  name contains both patterns then the log level will be 0. This is useful for silencing subscopes.",
                         "---------",
                         "  :1,Dead:2",
                         "",
                         "  Matches scopes containing \"Dead\" with level 2, and all other scopes with level 1.",
                         "--------- ",
                         "  Dead:0,:1",
                         "",
                         "  Matches all scopes with level 1, except those containing \"Dead\".   Note that the location of",
                         "  the :1 doesn't matter since it's specifying the default log level so it's the same as",
                         "  specifying :1,Dead:0.",
                         "--------- ",
                         "  Code*",
                         "",
                         "  Matches scopes starting with \"Code\" with level 1.",
                         "--------- ",
                         "  Code,~Dead",
                         "",
                         "  Matches scopes containing \"Code\" but not \"Dead\", with level 1.",
                              },
                /*declaringClass*/ DebugOptions.class,
                /*fieldName*/ "Dump",
                /*option*/ DebugOptions.Dump);
        }
        case "DumpOnError": {
            return OptionDescriptor.create(
                /*name*/ "DumpOnError",
                /*optionType*/ OptionType.Debug,
                /*optionValueType*/ Boolean.class,
                /*help*/ "Send Graal compiler IR to dump handlers on error.",
                /*declaringClass*/ DebugOptions.class,
                /*fieldName*/ "DumpOnError",
                /*option*/ DebugOptions.DumpOnError);
        }
        case "DumpOnPhaseChange": {
            return OptionDescriptor.create(
                /*name*/ "DumpOnPhaseChange",
                /*optionType*/ OptionType.Debug,
                /*optionValueType*/ String.class,
                /*help*/ "Dump a before and after graph if the named phase changes the graph.%nThe argument is substring matched against the simple name of the phase class",
                /*declaringClass*/ DebugOptions.class,
                /*fieldName*/ "DumpOnPhaseChange",
                /*option*/ DebugOptions.DumpOnPhaseChange);
        }
        case "DumpPath": {
            return OptionDescriptor.create(
                /*name*/ "DumpPath",
                /*optionType*/ OptionType.Debug,
                /*optionValueType*/ String.class,
                /*help*/ "The directory where various Graal dump files are written.",
                /*declaringClass*/ DebugOptions.class,
                /*fieldName*/ "DumpPath",
                /*option*/ DebugOptions.DumpPath);
        }
        case "DumpingErrorsAreFatal": {
            return OptionDescriptor.create(
                /*name*/ "DumpingErrorsAreFatal",
                /*optionType*/ OptionType.Debug,
                /*optionValueType*/ Boolean.class,
                /*help*/ "Treat any exceptions during dumping as fatal.",
                /*declaringClass*/ DebugOptions.class,
                /*fieldName*/ "DumpingErrorsAreFatal",
                /*option*/ DebugOptions.DumpingErrorsAreFatal);
        }
        case "InterceptBailout": {
            return OptionDescriptor.create(
                /*name*/ "InterceptBailout",
                /*optionType*/ OptionType.Debug,
                /*optionValueType*/ Boolean.class,
                /*help*/ "Intercept also bailout exceptions",
                /*declaringClass*/ DebugOptions.class,
                /*fieldName*/ "InterceptBailout",
                /*option*/ DebugOptions.InterceptBailout);
        }
        case "ListMetrics": {
            return OptionDescriptor.create(
                /*name*/ "ListMetrics",
                /*optionType*/ OptionType.Debug,
                /*optionValueType*/ Boolean.class,
                /*help*/ "Lists on the console at VM shutdown the metric names available to the Timers, Counters and MemUseTrackers options. Note that this only lists the metrics that were initialized during the VM execution and so will not include metrics for compiler code that is not executed.",
                /*declaringClass*/ DebugOptions.class,
                /*fieldName*/ "ListMetrics",
                /*option*/ DebugOptions.ListMetrics);
        }
        case "Log": {
            return OptionDescriptor.create(
                /*name*/ "Log",
                /*optionType*/ OptionType.Debug,
                /*optionValueType*/ String.class,
                /*help*/ "Pattern for specifying scopes in which logging is enabled. See the Dump option for the pattern syntax.",
                /*declaringClass*/ DebugOptions.class,
                /*fieldName*/ "Log",
                /*option*/ DebugOptions.Log);
        }
        case "LogVerbose": {
            return OptionDescriptor.create(
                /*name*/ "LogVerbose",
                /*optionType*/ OptionType.Debug,
                /*optionValueType*/ Boolean.class,
                /*help*/ "Enable more verbose log output when available",
                /*declaringClass*/ DebugOptions.class,
                /*fieldName*/ "LogVerbose",
                /*option*/ DebugOptions.LogVerbose);
        }
        case "MemUseTrackers": {
            return OptionDescriptor.create(
                /*name*/ "MemUseTrackers",
                /*optionType*/ OptionType.Debug,
                /*optionValueType*/ String.class,
                /*help*/ "Comma separated names of memory usage trackers that are enabled irrespective of the value for TrackMemUse option. An empty value enables all memory usage trackers unconditionally.",
                /*declaringClass*/ DebugOptions.class,
                /*fieldName*/ "MemUseTrackers",
                /*option*/ DebugOptions.MemUseTrackers);
        }
        case "MethodFilter": {
            return OptionDescriptor.create(
                /*name*/ "MethodFilter",
                /*optionType*/ OptionType.Debug,
                /*optionValueType*/ String.class,
                /*help*/ "Pattern for filtering debug scope output based on method context.",
                /*extraHelp*/ new String[] {
                         "The syntax for a pattern is:",
                         "",
                         "  SourcePatterns = SourcePattern [\",\" SourcePatterns] .",
                         "  SourcePattern = [ Class \".\" ] method [ \"(\" [ Parameter { \";\" Parameter } ] \")\" ] .",
                         "  Parameter = Class | \"int\" | \"long\" | \"float\" | \"double\" | \"short\" | \"char\" | \"boolean\" .",
                         "  Class = { package \".\" } class .",
                         " ",
                         "Glob pattern matching (*, ?) is allowed in all parts of the source pattern.",
                         "",
                         "Examples of method filters:",
                         "--------- ",
                         "  visit(Argument;BlockScope)  ",
                         "  ",
                         "  Matches all methods named \"visit\", with the first parameter of",
                         "  type \"Argument\", and the second parameter of type \"BlockScope\".",
                         "  The packages of the parameter types are irrelevant.",
                         "---------",
                         "  arraycopy(Object;;;;)",
                         " ",
                         "  Matches all methods named \"arraycopy\", with the first parameter",
                         "  of type \"Object\", and four more parameters of any type. The",
                         "  packages of the parameter types are irrelevant.",
                         "---------    ",
                         "  org.graalvm.compiler.core.graph.PostOrderNodeIterator.*",
                         " ",
                         "  Matches all methods in the class \"org.graalvm.compiler.core.graph.PostOrderNodeIterator\".",
                         "---------    ",
                         "  *",
                         " ",
                         "  Matches all methods in all classes",
                         "---------",
                         "  org.graalvm.compiler.core.graph.*.visit",
                         " ",
                         "  Matches all methods named \"visit\" in classes in the package",
                         "  \"org.graalvm.compiler.core.graph\".",
                         "---------",
                         "  arraycopy,toString",
                         " ",
                         "  Matches all methods named \"arraycopy\" or \"toString\", meaning that ',' acts as an or operator.",
                              },
                /*declaringClass*/ DebugOptions.class,
                /*fieldName*/ "MethodFilter",
                /*option*/ DebugOptions.MethodFilter);
        }
        case "MethodFilterRootOnly": {
            return OptionDescriptor.create(
                /*name*/ "MethodFilterRootOnly",
                /*optionType*/ OptionType.Debug,
                /*optionValueType*/ Boolean.class,
                /*help*/ "Only check MethodFilter against the root method in the context if true, otherwise check all methods",
                /*declaringClass*/ DebugOptions.class,
                /*fieldName*/ "MethodFilterRootOnly",
                /*option*/ DebugOptions.MethodFilterRootOnly);
        }
        case "MetricsFile": {
            return OptionDescriptor.create(
                /*name*/ "MetricsFile",
                /*optionType*/ OptionType.Debug,
                /*optionValueType*/ String.class,
                /*help*/ "File to which metrics are dumped per compilation.",
                /*extraHelp*/ new String[] {
                         "A CSV format is used if the file ends with .csv otherwise a more",
                         "human readable format is used. The fields in the CSV format are:",
                         "           compilable - method being compiled",
                         "  compilable_identity - identity hash code of compilable",
                         "       compilation_nr - where this compilation lies in the ordered",
                         "                        sequence of all compilations identified by",
                         "                        compilable_identity",
                         "       compilation_id - runtime issued identifier for the compilation",
                         "          metric_name - name of metric",
                         "         metric_value - value of metric",
                              },
                /*declaringClass*/ DebugOptions.class,
                /*fieldName*/ "MetricsFile",
                /*option*/ DebugOptions.MetricsFile);
        }
        case "MetricsThreadFilter": {
            return OptionDescriptor.create(
                /*name*/ "MetricsThreadFilter",
                /*optionType*/ OptionType.Debug,
                /*optionValueType*/ String.class,
                /*help*/ "Only report metrics for threads whose name matches the regular expression.",
                /*declaringClass*/ DebugOptions.class,
                /*fieldName*/ "MetricsThreadFilter",
                /*option*/ DebugOptions.MetricsThreadFilter);
        }
        case "PrintBackendCFG": {
            return OptionDescriptor.create(
                /*name*/ "PrintBackendCFG",
                /*optionType*/ OptionType.Debug,
                /*optionValueType*/ Boolean.class,
                /*help*/ "Enable dumping LIR, register allocation and code generation info to the C1Visualizer.",
                /*declaringClass*/ DebugOptions.class,
                /*fieldName*/ "PrintBackendCFG",
                /*option*/ DebugOptions.PrintBackendCFG);
        }
        case "PrintBinaryGraphPort": {
            return OptionDescriptor.create(
                /*name*/ "PrintBinaryGraphPort",
                /*optionType*/ OptionType.Debug,
                /*optionValueType*/ Integer.class,
                /*help*/ "Port part of the address to which graphs are dumped in binary format.",
                /*declaringClass*/ DebugOptions.class,
                /*fieldName*/ "PrintBinaryGraphPort",
                /*option*/ DebugOptions.PrintBinaryGraphPort);
        }
        case "PrintCFG": {
            return OptionDescriptor.create(
                /*name*/ "PrintCFG",
                /*optionType*/ OptionType.Debug,
                /*optionValueType*/ Boolean.class,
                /*help*/ "Enable dumping to the C1Visualizer. Enabling this option implies PrintBackendCFG.",
                /*declaringClass*/ DebugOptions.class,
                /*fieldName*/ "PrintCFG",
                /*option*/ DebugOptions.PrintCFG);
        }
        case "PrintCanonicalGraphStringFlavor": {
            return OptionDescriptor.create(
                /*name*/ "PrintCanonicalGraphStringFlavor",
                /*optionType*/ OptionType.Debug,
                /*optionValueType*/ Integer.class,
                /*help*/ "Choose format used when dumping canonical text for graphs: 0 gives a scheduled graph (better for spotting changes involving the schedule) while 1 gives a CFG containing expressions rooted at fixed nodes (better for spotting small structure differences)",
                /*declaringClass*/ DebugOptions.class,
                /*fieldName*/ "PrintCanonicalGraphStringFlavor",
                /*option*/ DebugOptions.PrintCanonicalGraphStringFlavor);
        }
        case "PrintCanonicalGraphStrings": {
            return OptionDescriptor.create(
                /*name*/ "PrintCanonicalGraphStrings",
                /*optionType*/ OptionType.Debug,
                /*optionValueType*/ Boolean.class,
                /*help*/ "Enable dumping canonical text from for graphs.",
                /*declaringClass*/ DebugOptions.class,
                /*fieldName*/ "PrintCanonicalGraphStrings",
                /*option*/ DebugOptions.PrintCanonicalGraphStrings);
        }
        case "PrintGraph": {
            return OptionDescriptor.create(
                /*name*/ "PrintGraph",
                /*optionType*/ OptionType.Debug,
                /*optionValueType*/ Boolean.class,
                /*help*/ "Enable dumping to the IdealGraphVisualizer.",
                /*declaringClass*/ DebugOptions.class,
                /*fieldName*/ "PrintGraph",
                /*option*/ DebugOptions.PrintGraph);
        }
        case "PrintGraphFile": {
            return OptionDescriptor.create(
                /*name*/ "PrintGraphFile",
                /*optionType*/ OptionType.Debug,
                /*optionValueType*/ Boolean.class,
                /*help*/ "Print graphs to files instead of sending them over the network.",
                /*declaringClass*/ DebugOptions.class,
                /*fieldName*/ "PrintGraphFile",
                /*option*/ DebugOptions.PrintGraphFile);
        }
        case "PrintGraphHost": {
            return OptionDescriptor.create(
                /*name*/ "PrintGraphHost",
                /*optionType*/ OptionType.Debug,
                /*optionValueType*/ String.class,
                /*help*/ "Host part of the address to which graphs are dumped.",
                /*declaringClass*/ DebugOptions.class,
                /*fieldName*/ "PrintGraphHost",
                /*option*/ DebugOptions.PrintGraphHost);
        }
        case "PrintGraphProbabilities": {
            return OptionDescriptor.create(
                /*name*/ "PrintGraphProbabilities",
                /*optionType*/ OptionType.Debug,
                /*optionValueType*/ Boolean.class,
                /*help*/ "Output probabilities for fixed nodes during binary graph dumping.",
                /*declaringClass*/ DebugOptions.class,
                /*fieldName*/ "PrintGraphProbabilities",
                /*option*/ DebugOptions.PrintGraphProbabilities);
        }
        case "PrintGraphWithSchedule": {
            return OptionDescriptor.create(
                /*name*/ "PrintGraphWithSchedule",
                /*optionType*/ OptionType.Debug,
                /*optionValueType*/ Boolean.class,
                /*help*/ "Schedule graphs as they are dumped.",
                /*declaringClass*/ DebugOptions.class,
                /*fieldName*/ "PrintGraphWithSchedule",
                /*option*/ DebugOptions.PrintGraphWithSchedule);
        }
        case "PrintTruffleTrees": {
            return OptionDescriptor.create(
                /*name*/ "PrintTruffleTrees",
                /*optionType*/ OptionType.Debug,
                /*optionValueType*/ Boolean.class,
                /*help*/ "Enable dumping Truffle ASTs to the IdealGraphVisualizer.",
                /*declaringClass*/ DebugOptions.class,
                /*fieldName*/ "PrintTruffleTrees",
                /*option*/ DebugOptions.PrintTruffleTrees);
        }
        case "ShowDumpFiles": {
            return OptionDescriptor.create(
                /*name*/ "ShowDumpFiles",
                /*optionType*/ OptionType.Debug,
                /*optionValueType*/ Boolean.class,
                /*help*/ "Print the name of each dump file path as it's created.",
                /*declaringClass*/ DebugOptions.class,
                /*fieldName*/ "ShowDumpFiles",
                /*option*/ DebugOptions.ShowDumpFiles);
        }
        case "Time": {
            return OptionDescriptor.create(
                /*name*/ "Time",
                /*optionType*/ OptionType.Debug,
                /*optionValueType*/ String.class,
                /*help*/ "Pattern for specifying scopes in which timing is enabled. See the Dump option for the pattern syntax. An empty value enables all timers unconditionally.",
                /*declaringClass*/ DebugOptions.class,
                /*fieldName*/ "Time",
                /*option*/ DebugOptions.Time);
        }
        case "Timers": {
            return OptionDescriptor.create(
                /*name*/ "Timers",
                /*optionType*/ OptionType.Debug,
                /*optionValueType*/ String.class,
                /*help*/ "Comma separated names of timers that are enabled irrespective of the value for Time option. An empty value enables all timers unconditionally.",
                /*declaringClass*/ DebugOptions.class,
                /*fieldName*/ "Timers",
                /*option*/ DebugOptions.Timers);
        }
        case "TrackMemUse": {
            return OptionDescriptor.create(
                /*name*/ "TrackMemUse",
                /*optionType*/ OptionType.Debug,
                /*optionValueType*/ String.class,
                /*help*/ "Pattern for specifying scopes in which memory use tracking is enabled. See the Dump option for the pattern syntax. An empty value enables all memory use trackers unconditionally.",
                /*declaringClass*/ DebugOptions.class,
                /*fieldName*/ "TrackMemUse",
                /*option*/ DebugOptions.TrackMemUse);
        }
        case "Verify": {
            return OptionDescriptor.create(
                /*name*/ "Verify",
                /*optionType*/ OptionType.Debug,
                /*optionValueType*/ String.class,
                /*help*/ "Pattern for specifying scopes in which logging is enabled. See the Dump option for the pattern syntax.",
                /*declaringClass*/ DebugOptions.class,
                /*fieldName*/ "Verify",
                /*option*/ DebugOptions.Verify);
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
                return i < 39;
            }
            @Override
            public OptionDescriptor next() {
                switch (i++) {
                    case 0: return get("AggregatedMetricsFile");
                    case 1: return get("BootstrapInitializeOnly");
                    case 2: return get("CanonicalGraphStringsCheckConstants");
                    case 3: return get("CanonicalGraphStringsExcludeVirtuals");
                    case 4: return get("CanonicalGraphStringsRemoveIdentities");
                    case 5: return get("ClearMetricsAfterBootstrap");
                    case 6: return get("Count");
                    case 7: return get("Counters");
                    case 8: return get("DebugStubsAndSnippets");
                    case 9: return get("Dump");
                    case 10: return get("DumpOnError");
                    case 11: return get("DumpOnPhaseChange");
                    case 12: return get("DumpPath");
                    case 13: return get("DumpingErrorsAreFatal");
                    case 14: return get("InterceptBailout");
                    case 15: return get("ListMetrics");
                    case 16: return get("Log");
                    case 17: return get("LogVerbose");
                    case 18: return get("MemUseTrackers");
                    case 19: return get("MethodFilter");
                    case 20: return get("MethodFilterRootOnly");
                    case 21: return get("MetricsFile");
                    case 22: return get("MetricsThreadFilter");
                    case 23: return get("PrintBackendCFG");
                    case 24: return get("PrintBinaryGraphPort");
                    case 25: return get("PrintCFG");
                    case 26: return get("PrintCanonicalGraphStringFlavor");
                    case 27: return get("PrintCanonicalGraphStrings");
                    case 28: return get("PrintGraph");
                    case 29: return get("PrintGraphFile");
                    case 30: return get("PrintGraphHost");
                    case 31: return get("PrintGraphProbabilities");
                    case 32: return get("PrintGraphWithSchedule");
                    case 33: return get("PrintTruffleTrees");
                    case 34: return get("ShowDumpFiles");
                    case 35: return get("Time");
                    case 36: return get("Timers");
                    case 37: return get("TrackMemUse");
                    case 38: return get("Verify");
                }
                throw new NoSuchElementException();
            }
        };
    }
}

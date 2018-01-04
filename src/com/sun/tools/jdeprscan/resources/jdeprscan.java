package com.sun.tools.jdeprscan.resources;

public final class jdeprscan extends java.util.ListResourceBundle {
    protected final Object[][] getContents() {
        return new Object[][] {
            { "main.help", "Scans each argument for usages of deprecated APIs. An argument\nmay be a directory specifying the root of a package hierarchy,\na JAR file, a class file, or a class name. The class name must be\nspecified using a fully qualified class name using the $ separator\ncharacter for nested classes, for example,\n\n    java.lang.Thread$State\n\nThe --class-path option provides a search path for resolution\nof dependent classes.\n\nThe --for-removal option limits scanning or listing to APIs that are\ndeprecated for removal. Cannot be used with a release value of 6, 7, or 8.\n\nThe --full-version option prints out the full version string of the tool.\n\nThe --help (-? -h) option prints out a full help message.\n\nThe --list (-l) option prints out the set of deprecated APIs. No scanning is done,\nso no directory, jar, or class arguments should be provided.\n\nThe --release option specifies the Java SE release that provides the set\nof deprecated APIs for scanning.\n\nThe --verbose (-v) option enables additional message output during processing.\n\nThe --version option prints out the abbreviated version string of the tool." },
            { "main.usage", "Usage: jdeprscan [options] '{dir|jar|class}' ...\n\noptions:\n        --class-path PATH\n        --for-removal\n        --full-version\n  -? -h --help\n  -l    --list\n        --release {0}\n  -v    --verbose\n        --version" },
            { "main.xhelp", "Unsupported options:\n\n  --Xload-class CLASS\n      Loads deprecation information from the named class.\n  --Xload-csv CSVFILE\n      Loads deprecation information from the named CSV file.\n  --Xload-dir DIR\n      Loads deprecation information from the class hierarchy\n      at the named directory.\n  --Xload-jar JARFILE\n      Loads deprecation information from the named JAR file.\n  --Xload-jdk9 JAVA_HOME\n      Loads deprecation information from the JDK located at\n      JAVA_HOME, which must be a modular JDK.\n  --Xload-old-jdk JAVA_HOME\n      Loads deprecation information from the JDK located at\n      JAVA_HOME, which must not be a modular JDK. Instead, the\n      named JDK must be a \"classic\" JDK with an rt.jar file.\n  --Xload-self\n      Loads deprecation information by traversing the jrt:\n      filesystem of the running JDK image.\n  --Xcompiler-arg ARG\n      Adds ARG to the list of compiler arguments.\n  --Xcsv-comment COMMENT\n      Adds COMMENT as a comment line to the output CSV file.\n      Only effective if -Xprint-csv is also supplied.\n  --Xhelp\n      Prints this message.\n  --Xprint-csv\n      Prints a CSV file containing the loaded deprecation information\n      instead of scanning any classes or JAR files." },
            { "scan.dep.normal", "" },
            { "scan.dep.removal", "(forRemoval=true)" },
            { "scan.err.exception", "error: unexpected exception {0}" },
            { "scan.err.noclass", "error: cannot find class {0}" },
            { "scan.err.nofile", "error: cannot find file {0}" },
            { "scan.err.nomethod", "error: cannot resolve Methodref {0}.{1}:{2}" },
            { "scan.head.dir", "Directory {0}:" },
            { "scan.head.jar", "Jar file {0}:" },
            { "scan.out.extends", "{0} {1} extends deprecated class {2} {3}" },
            { "scan.out.hasfield", "{0} {1} has field named {2} of deprecated type {3} {4}" },
            { "scan.out.implements", "{0} {1} implements deprecated interface {2} {3}" },
            { "scan.out.methodoverride", "{0} {1} overrides deprecated method {2}::{3}{4} {5}" },
            { "scan.out.methodparmtype", "{0} {1} has method named {2} having deprecated parameter type {3} {4}" },
            { "scan.out.methodrettype", "{0} {1} has method named {2} having deprecated return type {3} {4}" },
            { "scan.out.usesclass", "{0} {1} uses deprecated class {2} {3}" },
            { "scan.out.usesfield", "{0} {1} uses deprecated field {2}::{3} {4}" },
            { "scan.out.usesintfmethod", "{0} {1} uses deprecated method {2}::{3}{4} {5}" },
            { "scan.out.usesmethod", "{0} {1} uses deprecated method {2}::{3}{4} {5}" },
            { "scan.process.class", "Processing class {0}..." },
        };
    }
}

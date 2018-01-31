package com.sun.tools.javac.resources;

import java.nio.file.Path;
import com.sun.tools.javac.util.JCDiagnostic.Error;
import com.sun.tools.javac.util.JCDiagnostic.Warning;
import com.sun.tools.javac.util.JCDiagnostic.Note;
import com.sun.tools.javac.util.JCDiagnostic.Fragment;

public class LauncherProperties {
    public static class Errors {
        /**
         * launcher.err.cant.access.main.method=\
         *    can''t access main method in class: {0}
         */
        public static Error CantAccessMainMethod(String arg0) {
            return new Error("launcher", "cant.access.main.method", arg0);
        }
        
        /**
         * launcher.err.cant.find.class=\
         *    can''t find class: {0}
         */
        public static Error CantFindClass(String arg0) {
            return new Error("launcher", "cant.find.class", arg0);
        }
        
        /**
         * launcher.err.cant.find.main.method=\
         *    can''t find main(String[]) method in class: {0}
         */
        public static Error CantFindMainMethod(String arg0) {
            return new Error("launcher", "cant.find.main.method", arg0);
        }
        
        /**
         * launcher.err.cant.read.file=\
         *    error reading file {0}: {1}
         */
        public static Error CantReadFile(Path arg0, Object arg1) {
            return new Error("launcher", "cant.read.file", arg0, arg1);
        }
        
        /**
         * launcher.err.compilation.failed=\
         *    compilation failed
         */
        public static final Error CompilationFailed = new Error("launcher", "compilation.failed");
        
        /**
         * launcher.err.enable.preview.requires.source=\
         *    --enable-preview must be used with --source
         */
        public static final Error EnablePreviewRequiresSource = new Error("launcher", "enable.preview.requires.source");
        
        /**
         * launcher.err.file.not.found=\
         *    file not found: {0}
         */
        public static Error FileNotFound(Path arg0) {
            return new Error("launcher", "file.not.found", arg0);
        }
        
        /**
         * launcher.err.invalid.filename=\
         *    invalid filename: {0}
         */
        public static Error InvalidFilename(String arg0) {
            return new Error("launcher", "invalid.filename", arg0);
        }
        
        /**
         * launcher.err.invalid.value.for.source=\
         *    invalid value for --source option: {0}
         */
        public static Error InvalidValueForSource(String arg0) {
            return new Error("launcher", "invalid.value.for.source", arg0);
        }
        
        /**
         * launcher.err.main.not.public.static=\
         *    ''main'' method is not declared ''public static''
         */
        public static final Error MainNotPublicStatic = new Error("launcher", "main.not.public.static");
        
        /**
         * launcher.err.main.not.void=\
         *    ''main'' method is not declared with a return type of ''void''
         */
        public static final Error MainNotVoid = new Error("launcher", "main.not.void");
        
        /**
         * launcher.err.no.args=\
         *    no filename
         */
        public static final Error NoArgs = new Error("launcher", "no.args");
        
        /**
         * launcher.err.no.class=\
         *    no class declared in file
         */
        public static final Error NoClass = new Error("launcher", "no.class");
        
        /**
         * launcher.err.no.value.for.option=\
         *    no value given for option: {0}
         */
        public static Error NoValueForOption(String arg0) {
            return new Error("launcher", "no.value.for.option", arg0);
        }
        
        /**
         * launcher.err.unexpected.class=\
         *    class found on application class path: {0}
         */
        public static Error UnexpectedClass(String arg0) {
            return new Error("launcher", "unexpected.class", arg0);
        }
    }
}

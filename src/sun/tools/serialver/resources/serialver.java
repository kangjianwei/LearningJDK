package sun.tools.serialver.resources;

public final class serialver extends java.util.ListResourceBundle {
    protected final Object[][] getContents() {
        return new Object[][] {
            { "ClassNotFound", "Class {0} not found." },
            { "NotSerializable", "Class {0} is not Serializable." },
            { "error.missing.classpath", "Missing argument for -classpath option" },
            { "error.parsing.classpath", "Error parsing classpath {0}." },
            { "invalid.flag", "Invalid flag {0}." },
            { "usage", "use: serialver [-classpath classpath] [classname...]" },
        };
    }
}

package jdk.jshell.resources;

public final class l10n extends java.util.ListResourceBundle {
    protected final Object[][] getContents() {
        return new Object[][] {
            { "jshell.diag.modifier.plural.fatal", "Modifiers {0} not permitted in top-level declarations" },
            { "jshell.diag.modifier.plural.ignore", "Modifiers {0} not permitted in top-level declarations, ignored" },
            { "jshell.diag.modifier.single.fatal", "Modifier {0} not permitted in top-level declarations" },
            { "jshell.diag.modifier.single.ignore", "Modifier {0} not permitted in top-level declarations, ignored" },
            { "jshell.diag.object.method.fatal", "JShell method names must not match Object methods: {0}" },
            { "jshell.exc.alien", "Snippet not from this JShell: {0}" },
            { "jshell.exc.closed", "JShell ({0}) has been closed." },
            { "jshell.exc.null", "Snippet must not be null" },
            { "jshell.exc.var.not.valid", "Snippet parameter of varValue() {0} must be VALID, it is: {1}" },
        };
    }
}

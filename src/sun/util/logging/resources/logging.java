package sun.util.logging.resources;

import java.util.ListResourceBundle;

public final class logging extends ListResourceBundle {
    protected final Object[][] getContents() {
        return new Object[][] {
            { "ALL", "All" },
            { "CONFIG", "Config" },
            { "FINE", "Fine" },
            { "FINER", "Finer" },
            { "FINEST", "Finest" },
            { "INFO", "Info" },
            { "OFF", "Off" },
            { "SEVERE", "Severe" },
            { "WARNING", "Warning" },
        };
    }
}

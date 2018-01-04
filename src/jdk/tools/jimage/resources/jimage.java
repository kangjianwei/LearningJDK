package jdk.tools.jimage.resources;

import java.util.ListResourceBundle;

public final class jimage extends ListResourceBundle {
    protected final Object[][] getContents() {
        return new Object[][] {
            { "err.cannot.create.dir", "cannot create directory {0}" },
            { "err.invalid.jimage", "Unable to open {0}: {1}" },
            { "err.missing.arg", "no value given for {0}" },
            { "err.no.jimage", "no jimage provided" },
            { "err.not.a.dir", "not a directory: {0}" },
            { "err.not.a.jimage", "not a jimage file: {0}" },
            { "err.not.a.task", "task must be one of <extract | info | list | verify>: {0}" },
            { "err.option.unsupported", "{0} not supported: {1}" },
            { "err.unknown.option", "unknown option: {0}" },
            { "error.prefix", "Error:" },
            { "main.command.files", "       @<filename>                  Read options from file" },
            { "main.opt.dir", "          --dir                        Target directory for extract directive" },
            { "main.opt.footer", "\nFor options requiring a <pattern-list>, the value will be a comma separated\nlist of elements each using one the following forms:\n  <glob-pattern>\n  glob:<glob-pattern>\n  regex:<regex-pattern>" },
            { "main.opt.full-version", "          --full-version               Print full version information" },
            { "main.opt.help", "  -?, -h, --help                       Print this help message" },
            { "main.opt.include", "          --include <pattern-list>     Pattern list for filtering entries." },
            { "main.opt.verbose", "          --verbose                    Listing prints entry size and offset\n                                       attributes" },
            { "main.opt.version", "          --version                    Print version information" },
            { "main.usage", "Usage: {0} <extract | info | list | verify> <options> jimage...\n\n  extract  - Extract all jimage entries and place in a directory specified\n             by the --dir=<directory> (default='.') option.\n\n  info     - Prints detailed information contained in the jimage header.\n\n  list     - Prints the names of all the entries in the jimage.  When used with\n             --verbose, list will also print entry size and offset attributes.\n\n  verify   - Reports on any .class entries that don't verify as classes.\n\nPossible options include:" },
            { "main.usage.extract", "  extract  - Extract all jimage entries and place in a directory specified\n             by the --dir=<directory> (default='.') option." },
            { "main.usage.info", "  info     - Prints detailed information contained in the jimage header." },
            { "main.usage.list", "  list     - Prints the names of all the entries in the jimage.  When used with\n             --verbose, list will also print entry size and offset attributes." },
            { "main.usage.summary", "Usage: {0} <extract | info | list | verify> <options> jimage...\nuse -h or --help for a list of possible options." },
            { "main.usage.verify", "  verify   - Reports errors on any .class entries that don't verify as classes." },
            { "warn.prefix", "Warning:" },
        };
    }
}

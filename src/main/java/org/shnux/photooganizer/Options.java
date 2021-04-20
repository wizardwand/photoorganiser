package org.shnux.photooganizer;

import com.sampullara.cli.Argument;

public class Options {

    @Argument(alias = "s", description = "source folder path", required = true)
    static String source;

    @Argument(alias = "d", description = "destination folder path", required = true)
    static String destination;


}

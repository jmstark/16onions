/*
 * Copyright (C) 2016 Sree Harsha Totakura <sreeharsha@totakura.in>
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package util.config;

import java.util.logging.Level;
import java.util.logging.Logger;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;

/**
 *
 * @author Sree Harsha Totakura <sreeharsha@totakura.in>
 */
public class CliParser {

    private final String programName;
    private final String helpText;
    private final DefaultParser parser;
    private final Options options;
    private CommandLine commandline;

    public CliParser(String programName, String helpText) {
        this.programName = programName;
        this.helpText = helpText;
        this.parser = new DefaultParser();
        this.options = new Options();
        //add the default options
        options.addOption(Option.builder("c")
                .required(false)
                .longOpt("config")
                .desc("configuration file")
                .optionalArg(false)
                .argName("FILE")
                .hasArg().build());
        options.addOption(Option.builder("h")
                .required(false)
                .hasArg(false)
                .longOpt("help")
                .desc("show usage help")
                .build());
        this.commandline = null;
    }

    public void addOption(Option option) {
        options.addOption(option);
    }

    private void printHelp(HelpFormatter formatter, String header) {
        formatter.printHelp(programName,
                header,
                options,
                "Please report bugs to totakura@net.in.tum.de",
                true);
    }

    public CommandLine parse(String[] arguments) {
        HelpFormatter formatter;
        CommandLine commandline = null;
        formatter = new HelpFormatter();
        try {
            commandline = parser.parse(options, arguments);
        } catch (ParseException ex) {
            printHelp(formatter, "Insufficient or wrong arguments given");
            System.exit(1);
        }
        if (commandline.hasOption('h')) {
            printHelp(formatter, helpText);
            System.exit(1);
        }
        this.commandline = commandline;
        return commandline;
    }

    public String getConfigFilename(String defaultFilename) throws
            IllegalStateException {
        if (null == commandline) {
            throw new IllegalStateException("Arguments are not parsed yet");
        }
        return commandline.getOptionValue('c', defaultFilename);
    }
}

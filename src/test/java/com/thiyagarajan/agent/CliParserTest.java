package com.thiyagarajan.agent;

import com.thiyagarajan.agent.cli.CliParser;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class CliParserTest {
    @Test
    void acceptsTopLevelHelpAndVersionFlags() {
        assertTrue(CliParser.parse(new String[]{"--help"}).helpRequested());
        assertTrue(CliParser.parse(new String[]{"--version"}).versionRequested());
    }

    @Test
    void parsesCommandArgumentsAndOptions() {
        CliParser cli = CliParser.parse(new String[]{"plan", "example.json", "--env", "qa"});
        assertEquals("plan", cli.command());
        assertArrayEquals(new String[]{"example.json"}, cli.positional());
        assertEquals("qa", cli.option("--env"));
    }

    @Test
    void rejectsUnknownAndIncompleteOptions() {
        assertThrows(IllegalArgumentException.class,
                () -> CliParser.parse(new String[]{"plan", "x.json", "--unknown"}));
        assertThrows(IllegalArgumentException.class,
                () -> CliParser.parse(new String[]{"plan", "x.json", "--env"}));
    }

    @Test
    void rejectsDuplicateSingleValueOptions() {
        assertThrows(IllegalArgumentException.class,
                () -> CliParser.parse(new String[]{"plan", "x.json", "--env", "qa", "--env", "prod"}));
    }
}

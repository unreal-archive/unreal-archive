package org.unrealarchive.common;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class CLI {

	private static final Pattern OPTION_PATTERN = Pattern.compile("--([a-zA-Z0-9-_]+)=(.+)?");

	private static final Pattern FLAG_PATTERN = Pattern.compile("--([a-zA-Z0-9-_]+)");

	private static final String PROPERTIES = ".unreal-archive.conf";

	private final String[] commands;
	private final Map<String, String> options;
	private final Set<String> flags;

	public CLI(String[] commands, Map<String, String> options, Set<String> flags) {
		this.commands = commands;
		this.options = options;
		this.flags = flags;
	}

	public static CLI parse(String... args) {
		return parse(Map.of(), args);
	}

	public static CLI parse(Map<String, String> defOptions, String... args) {
		final List<String> commands = new ArrayList<>();
		final Map<String, String> props = new HashMap<>();
		final Set<String> flags = new HashSet<>();

		// populate default options
		props.putAll(defOptions);

		Path confFile = Paths.get(PROPERTIES);
		if (!Files.exists(confFile)) confFile = Paths.get(System.getProperty("user.home")).resolve(PROPERTIES);
		if (Files.exists(confFile)) {
			try {
				Properties fileProps = new Properties();
				fileProps.load(Files.newInputStream(confFile));
				for (String p : fileProps.stringPropertyNames()) {
					props.put(p, fileProps.getProperty(p));
				}
			} catch (IOException e) {
				System.err.println("Failed to read properties from file " + confFile + ": " + e);
			}
		}

		for (String arg : args) {
			Matcher optMatcher = OPTION_PATTERN.matcher(arg);
			Matcher flagMatcher = FLAG_PATTERN.matcher(arg);

			if (optMatcher.matches()) {
				props.put(optMatcher.group(1), optMatcher.group(2) == null ? "" : optMatcher.group(2));
			} else if (flagMatcher.matches()) {
				flags.add(flagMatcher.group(1));
			} else {
				commands.add(arg);
			}
		}

		return new CLI(commands.toArray(new String[0]), props, flags);
	}

	public String option(String key, String defaultValue) {
		return options.getOrDefault(key, defaultValue);
	}

	public boolean flag(String flag) {
		return flags.contains(flag);
	}

	public void putOption(String key, String value) {
		options.put(key, value);
	}

	public void putFlag(String flag) {
		flags.add(flag);
	}

	public String[] commands() {
		return commands;
	}

	public static String userPrompt(String prompt, String defaultValue) {
		System.out.println(prompt);
		System.out.print("> ");
		try (BufferedReader reader = new BufferedReader(new InputStreamReader(System.in))) {
			String in = reader.readLine().trim();
			if (in.isEmpty()) return defaultValue;
			else return in;
		} catch (IOException e) {
			System.err.printf("Failed to read user input: %s", e);
			System.exit(254);
		}
		return defaultValue;
	}
}

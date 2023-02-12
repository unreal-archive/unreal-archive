package net.shrimpworks.unreal.archive.common;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class CLI {

	private static final String OPTION_PATTERN = "--([a-zA-Z0-9-_]+)=(.+)?";

	private static final String PROPERTIES = ".unreal-archive.conf";

	private final String[] commands;
	private final Map<String, String> options;

	public CLI(String[] commands, Map<String, String> options) {
		this.commands = commands;
		this.options = options;
	}

	public static CLI parse(Map<String, String> defOptions, String... args) {
		final List<String> commands = new ArrayList<>();
		final Map<String, String> props = new HashMap<>();

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

		Pattern optPattern = Pattern.compile(OPTION_PATTERN);

		for (String arg : args) {
			Matcher optMatcher = optPattern.matcher(arg);

			if (optMatcher.matches()) {
				props.put(optMatcher.group(1), optMatcher.group(2) == null ? "" : optMatcher.group(2));
			} else {
				commands.add(arg);
			}
		}

		return new CLI(commands.toArray(new String[0]), props);
	}

	public String option(String key, String defaultValue) {
		return options.getOrDefault(key, defaultValue);
	}

	public void putOption(String key, String value) {

		options.put(key, value);
	}

	public String[] commands() {
		return commands;
	}

}

package net.shrimpworks.unreal.archive;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class CLI {

	private static final String OPTION_PATTERN = "--([a-zA-Z0-9-_]+)=(.+)?";

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

		Pattern optPattern = Pattern.compile(OPTION_PATTERN);

		final StringBuilder commandBuilder = new StringBuilder();

		for (String arg : args) {
			Matcher optMatcher = optPattern.matcher(arg);

			if (optMatcher.matches()) {
				props.put(optMatcher.group(1), optMatcher.group(2) == null ? "" : optMatcher.group(2));
			} else {
				commands.add(arg);
			}
		}

		return new CLI(commands.toArray(new String[0]), Collections.unmodifiableMap(props));
	}

	public String option(String key, String defaultValue) {
		return options.getOrDefault(key, defaultValue);
	}

	public String[] commands() {
		return commands;
	}

}

package org.unrealarchive.tools;

import java.io.IOException;
import java.util.Arrays;

import org.unrealarchive.common.CLI;
import org.unrealarchive.common.Version;
import org.unrealarchive.content.RepositoryManager;

public class Main {
	static {
		// prepare the version
		Version.setVersion(Main.class);
	}

	public static void main(String[] args) throws IOException, InterruptedException, ReflectiveOperationException {
		System.err.printf("Unreal Archive Tools version %s%n", Version.version());

		final CLI cli = CLI.parse(args);

		if (cli.commands().length == 0) {
			usage();
			System.exit(1);
		}

		switch (cli.commands()[0].toLowerCase()) {
			case "helper" -> IndexHelper.main(Arrays.copyOfRange(cli.commands(), 1, cli.commands().length));
			case "screenshots" -> MasterServerScreenshots.main(Arrays.copyOfRange(cli.commands(), 1, cli.commands().length));
			default -> {
				System.out.printf("Command \"%s\" does not exist!%n%n", cli.commands()[0]);
				usage();
			}
		}

		System.exit(0);
	}

	private static void usage() {
		System.out.println("Usage: tools <command> [options]");
		System.out.println("Commands:");
		System.out.println("  helper <command> [options]");
		System.out.println("  screenshots <game> <output directory>");
	}
}
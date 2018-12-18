package net.shrimpworks.unreal.archive.managed;

import java.io.IOException;
import java.time.LocalDate;

import net.shrimpworks.unreal.archive.YAML;

import org.junit.Test;

import static org.junit.Assert.*;

public class ManagedContentTest {

	@Test
	public void managedYaml() throws IOException {
		Managed man = new Managed();
		man.createdDate = LocalDate.now().minusDays(3);
		man.updatedDate = LocalDate.now();
		man.game = "General";
		man.path = "Tests";
		man.title = "Testing Things";
		man.author = "Bob";
		man.description = "There is no description";
		man.homepage = "https://unreal.com/";

		Managed.ManagedFile file = new Managed.ManagedFile();
		file.platform = Managed.Platform.WINDOWS;
		file.localFile = "file.exe";
		file.synced = false;
		file.title = "The File";
		file.version = "1.0";

		man.downloads.add(file);

		// serialise and de-serialise between YAML and instance
		String stringMan = YAML.toString(man);
		Managed newMan = YAML.fromString(stringMan, Managed.class);

		assertEquals(man, newMan);
		assertNotSame(man, newMan);
		assertEquals(file, newMan.downloads.get(0));

		// fake syncing the download, should appear as a change
		newMan.downloads.get(0).downloads.add("https://cool-files.dl/file.exe");
		newMan.downloads.get(0).synced = true;

		assertNotEquals(man, newMan);
		assertNotEquals(file, newMan.downloads.get(0));
	}

}

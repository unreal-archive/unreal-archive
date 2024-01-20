package org.unrealarchive.indexing.announcers;

import java.util.Objects;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;

import net.shrimpworks.unreal.packages.IntFile;

import org.unrealarchive.content.FileType;
import org.unrealarchive.indexing.Classifier;
import org.unrealarchive.indexing.Incoming;
import org.unrealarchive.indexing.IndexUtils;

/**
 * An announcer should contain:
 * <p>
 * - At least one .ucl file
 * - At least one .u or .uax file
 * <p>
 * UT 2003/4 format:
 * <pre>
 * Announcer=(ClassName=APackage.$Name,PackageName=APackage,FallbackPackage=Fallback,EnglishOnly=0,FriendlyName=APackage.$Name.AnnouncerName,FallbackName="Voice Name")
 * </pre>
 */
public class AnnouncerClassifier implements Classifier {

	@Override
	public boolean classify(Incoming incoming) {
		Set<Incoming.IncomingFile> uclFiles = incoming.files(FileType.UCL);
		Set<Incoming.IncomingFile> soundFiles = incoming.files(FileType.CODE, FileType.SOUNDS);

		Set<Incoming.IncomingFile> miscFiles = incoming.files(FileType.MAP,
															  FileType.MUSIC,
															  FileType.STATICMESH,
															  FileType.ANIMATION,
															  FileType.PLAYER);

		// if there are other types of files, we can probably assume it's something like a mod
		if (!miscFiles.isEmpty()) return false;

		// there should be a UCL file, along with a sound or code package
		if (uclFiles.isEmpty() || soundFiles.isEmpty()) return false;

		// a UT model should have a "code" package which contains the mesh
		return checkUT2Announcer(incoming, uclFiles);
	}

	private boolean checkUT2Announcer(Incoming incoming, Set<Incoming.IncomingFile> uclFiles) {
		final AtomicBoolean seemsToBeAnnouncer = new AtomicBoolean(false);
		final AtomicBoolean probablyNotAnnouncer = new AtomicBoolean(false);

		// search files for objects describing an announcer, and nothing else
		IndexUtils.readIntFiles(incoming, uclFiles, true)
				  .filter(Objects::nonNull)
				  .forEach(uclFile -> {
					  if (probablyNotAnnouncer.get()) return;

					  IntFile.Section section = uclFile.section("root");
					  if (section == null) return;

					  if (section.keys().contains("Map")
						  || section.keys().contains("Game")
						  || section.keys().contains("Mutator")) {
						  probablyNotAnnouncer.set(true);
						  return;
					  }

					  if (section.keys().contains("Announcer")) {
						  seemsToBeAnnouncer.set(true);
					  }
				  });

		return !probablyNotAnnouncer.get() && seemsToBeAnnouncer.get();
	}

}

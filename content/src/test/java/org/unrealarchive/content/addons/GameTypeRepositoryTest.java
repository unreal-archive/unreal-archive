package org.unrealarchive.content.addons;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;

import org.unrealarchive.common.YAML;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import static org.junit.jupiter.api.Assertions.*;

public class GameTypeRepositoryTest {

    @TempDir
    Path tempDir;

    @Test
    public void forIdLookup() throws IOException {
        GameType gt = new GameType();
        gt.game = "Unreal Tournament";
        gt.name = "Bunny Hunt";
        gt.author = "Tester";
        gt.description = "A test game type";
        gt.titleImage = "title.png";

        // write minimal gametype yaml anywhere under tempDir
        Path out = Files.createDirectories(tempDir.resolve("gt"));
        Files.writeString(out.resolve("gametype.yml"), YAML.toString(gt), StandardOpenOption.CREATE);

        GameTypeRepository repo = new GameTypeRepository.FileRepository(tempDir);

        String rawId = gt.id().id();
        GameType byRaw = repo.forId(rawId);
        assertNotNull(byRaw);
        assertEquals(gt, byRaw);

        String fullId = gt.id().toString();
        GameType byFull = repo.forId(fullId);
        assertNotNull(byFull);
        assertEquals(gt, byFull);
    }
}

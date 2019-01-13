package net.shrimpworks.unreal.archive.content.voices;

import java.util.function.Consumer;

import net.shrimpworks.unreal.archive.content.Content;
import net.shrimpworks.unreal.archive.content.Incoming;
import net.shrimpworks.unreal.archive.content.IndexHandler;
import net.shrimpworks.unreal.archive.content.IndexLog;
import net.shrimpworks.unreal.archive.content.IndexResult;

public class VoiceIndexHandler implements IndexHandler<Voice> {

	public static class ModelIndexHandlerFactory implements IndexHandlerFactory<Voice> {

		@Override
		public IndexHandler<Voice> get() {
			return new VoiceIndexHandler();
		}
	}

	@Override
	public void index(Incoming incoming, Content current, Consumer<IndexResult<Voice>> completed) {
		Voice v = (Voice)current;
		IndexLog log = incoming.log;

	}
}

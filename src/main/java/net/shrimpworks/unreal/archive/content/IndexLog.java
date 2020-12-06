package net.shrimpworks.unreal.archive.content;

import java.util.ArrayList;
import java.util.List;

public class IndexLog {

	public static final IndexLog NOP = new IndexLog() {
		@Override
		public void log(EntryType type, String message) {}

		@Override
		public void log(EntryType type, String message, Throwable e) {}
	};

	public enum EntryType {
		INFO,
		CONTINUE,
		FATAL
	}

	public final List<LogEntry> log;

	public IndexLog() {
		this.log = new ArrayList<>();
	}

	public void log(EntryType type, String message) {
		this.log(type, message, null);
	}

	public void log(EntryType type, String message, Throwable e) {
		this.log.add(new LogEntry(System.currentTimeMillis(), type, message, e));
	}

	public boolean ok() {
		return log.isEmpty() || log.stream().noneMatch(l -> l.type == EntryType.FATAL);
	}

	@Override
	public String toString() {
		return String.format("IndexLog [log=%s]", log);
	}

	public static class LogEntry {

		public final long timestamp;
		public final EntryType type;
		public final String message;
		public final Throwable exception;

		public LogEntry(long timestamp, EntryType type, String message, Throwable exception) {
			this.timestamp = timestamp;
			this.type = type;
			this.message = message;
			this.exception = exception;
		}

		@Override
		public String toString() {
			return String.format("LogEntry [timestamp=%s, type=%s, message=%s, exception=%s]", timestamp, type, message, exception);
		}
	}
}

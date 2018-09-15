package net.shrimpworks.unreal.archive;

import java.util.ArrayList;
import java.util.List;

public class IndexLog {

	public enum EntryType {
		INFO,
		CONTINUE,
		FATAL
	}

	public final ContentSubmission submission;

	public final List<LogEntry> log;

	public IndexLog(ContentSubmission submission) {
		this.submission = submission;
		this.log = new ArrayList<>();
	}

	public void log(EntryType type, String message) {
		this.log(type, message, null);
	}

	public void log(EntryType type, String message, Exception e) {
		this.log.add(new LogEntry(System.currentTimeMillis(), type, message, e));
	}

	@Override
	public String toString() {
		return String.format("IndexLog [submission=%s, log=%s]", submission, log);
	}

	public static class LogEntry {

		public final long timestamp;
		public final EntryType type;
		public final String message;
		public final Exception exception;

		public LogEntry(long timestamp, EntryType type, String message, Exception exception) {
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

module unreal.archive.content {
	requires java.base;
	requires java.desktop;

	requires unreal.archive.common;

	requires com.fasterxml.jackson.annotation;

	exports org.unrealarchive.content;
	exports org.unrealarchive.content.addons;
	exports org.unrealarchive.content.docs;
	exports org.unrealarchive.content.managed;
	exports org.unrealarchive.content.wiki;
}

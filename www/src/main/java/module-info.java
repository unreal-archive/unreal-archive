open module unreal.archive.www {
	requires java.base;
	requires java.desktop;

	requires unreal.archive.common;
	requires unreal.archive.content;

	requires com.fasterxml.jackson.annotation;

	requires freemarker;
	requires org.commonmark;
	requires org.commonmark.ext.gfm.tables;
	requires org.jsoup;
	requires jdk.compiler;

	exports org.unrealarchive.www;
	exports org.unrealarchive.www.content;
	exports org.unrealarchive.www.features;
}

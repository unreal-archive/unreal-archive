open module unreal.archive.www {
	requires java.base;
	requires java.desktop;
	requires java.logging;

	requires unreal.archive.common;
	requires unreal.archive.content;

	requires com.fasterxml.jackson.annotation;

	requires freemarker;
	requires org.commonmark;
	requires org.commonmark.ext.gfm.tables;
	requires org.jsoup;

	exports org.unrealarchive.www;
	exports org.unrealarchive.www.content;
	exports org.unrealarchive.www.features;
}

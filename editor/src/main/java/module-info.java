module org.unrealarchive.editor {
    requires javafx.controls;
    requires javafx.fxml;
    requires java.desktop;

    requires unreal.archive.common;
    requires unreal.archive.content;
	requires jdk.compiler;

	opens org.unrealarchive.editor to javafx.fxml;
    exports org.unrealarchive.editor;
}
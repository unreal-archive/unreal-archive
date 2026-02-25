package org.unrealarchive.editor;

import java.beans.ConstructorProperties;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.GenericArrayType;
import java.lang.reflect.Modifier;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.lang.reflect.TypeVariable;
import java.lang.reflect.WildcardType;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javafx.geometry.Insets;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.control.TitledPane;
import javafx.scene.layout.ColumnConstraints;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;

public class GenericEditor<T> extends VBox {

	private final Class<T> type;
	private T instance;
	private final Map<Field, InputControl> inputs = new HashMap<>();

	public GenericEditor(T instance) {
		this((Class<T>)instance.getClass(), instance);
	}

	public GenericEditor(Class<T> type) {
		this(type, null);
	}

	public GenericEditor(Class<T> type, T instance) {
		this.type = type;
		this.instance = instance;
		this.setSpacing(10);
		this.setPadding(new Insets(10));

		buildUI();
	}

	private void buildUI() {
		GridPane grid = new GridPane();
		grid.setHgap(10);
		grid.setVgap(10);

		ColumnConstraints col1 = new ColumnConstraints(120, 0, 0);
		ColumnConstraints col2 = new ColumnConstraints(25);
		ColumnConstraints col3 = new ColumnConstraints();
		col3.setHgrow(Priority.ALWAYS);

		grid.getColumnConstraints().addAll(col1, col2, col3);
		grid.setMaxWidth(Double.MAX_VALUE);

		int row = 0;
		for (Field field : type.getFields()) {
			int modifiers = field.getModifiers();
			if (Modifier.isTransient(modifiers)) continue;
			if (Modifier.isStatic(modifiers)) continue;
			boolean isFinal = Modifier.isFinal(modifiers);

			Label label = new Label(field.getName());
			CheckBox nullCheck = new CheckBox();
			nullCheck.setTooltip(new javafx.scene.control.Tooltip("Null Value"));

			Object value = null;
			if (instance != null) {
				try {
					value = field.get(instance);
				} catch (IllegalAccessException e) {
					// ignore
				}
			}

			InputControl input = createInputControl(field.getType(), field.getGenericType(), value);
			inputs.put(field, input);

			nullCheck.setSelected(value == null);
			input.getNode().setDisable(value == null || isFinal);
			if (isFinal) nullCheck.setDisable(true);

			nullCheck.selectedProperty().addListener(
				(obs, oldVal, newVal) -> {
					if (newVal) input.getNode().getStyleClass().add("null-value");
					else input.getNode().getStyleClass().remove("null-value");
					input.getNode().setDisable(newVal || isFinal);
				}
			);

			grid.add(label, 0, row);
			grid.add(nullCheck, 1, row);
			grid.add(input.getNode(), 2, row);
			row++;
		}

		this.getChildren().add(grid);
	}

	private static InputControl createInputControl(Type fieldType, Type genericType, Object value) {
		Class<?> fieldTypeClass = toClass(fieldType);

		if (fieldType == String.class) {
			return new StringInput((String)value);
		} else if (fieldType == boolean.class || fieldType == Boolean.class) {
			return new BooleanInput((Boolean)value);
		} else if (fieldType == int.class || fieldType == Integer.class) {
			return new NumberInput(fieldType, value);
		} else if (fieldType == long.class || fieldType == Long.class) {
			return new NumberInput(fieldType, value);
		} else if (fieldType == double.class || fieldType == Double.class) {
			return new NumberInput(fieldType, value);
		} else if (fieldType == LocalDateTime.class) {
			return new DateTimeInput((LocalDateTime)value);
		} else if (fieldType == LocalDate.class) {
			return new DateInput((LocalDate)value);
		} else if (fieldTypeClass.isEnum()) {
			return new EnumInput(fieldTypeClass, value);
		} else if (List.class.isAssignableFrom(fieldTypeClass) || Set.class.isAssignableFrom(fieldTypeClass)) {
			Type elementType = extractTypeArg(genericType, 0);
			return new CollectionInput(fieldType, elementType, (Collection<?>)value);
		} else if (Map.class.isAssignableFrom(fieldTypeClass)) {
			Type keyType = extractTypeArg(genericType, 0);
			Type valType = extractTypeArg(genericType, 1);
			return new MapInput(keyType, valType, (Map<?, ?>)value);
		} else {
			return new ComplexInput(fieldTypeClass, value);
		}
	}

	private static Type extractTypeArg(Type genericType, int index) {
		if (genericType instanceof ParameterizedType pt) {
			Type[] args = pt.getActualTypeArguments();
			if (index >= 0 && index < args.length) return args[index];
		}
		return Object.class;
	}

	private static Class<?> toClass(Type type) {
		if (type instanceof Class<?> c) return c;

		if (type instanceof ParameterizedType pt) {
			Type raw = pt.getRawType();
			if (raw instanceof Class<?> c) return c;
			return Object.class;
		}

		if (type instanceof WildcardType wt) {
			Type[] upper = wt.getUpperBounds();
			return upper.length > 0 ? toClass(upper[0]) : Object.class;
		}

		if (type instanceof TypeVariable<?> tv) {
			Type[] bounds = tv.getBounds();
			return bounds.length > 0 ? toClass(bounds[0]) : Object.class;
		}

		if (type instanceof GenericArrayType gat) {
			// Treat as Object[] for editor purposes
			return Object[].class;
		}

		return Object.class;
	}

	public T value() {
		try {
			if (instance == null) {
				instance = createInstance();
			}

			for (Map.Entry<Field, InputControl> entry : inputs.entrySet()) {
				Field field = entry.getKey();
				InputControl control = entry.getValue();

				if (Modifier.isFinal(field.getModifiers())) continue;

				if (control.getNode().isDisable()) {
					if (!field.getType().isPrimitive()) field.set(instance, null);
				} else {
					field.set(instance, control.getValue());
				}
			}
		} catch (Exception e) {
			throw new RuntimeException("Failed to get value from editor", e);
		}
		return instance;
	}

	private T createInstance() throws Exception {
		// 1. Look for constructor with @ConstructorProperties
		for (Constructor<?> ctor : type.getConstructors()) {
			ConstructorProperties props = ctor.getAnnotation(ConstructorProperties.class);
			if (props != null) {
				Object[] args = new Object[props.value().length];
				for (int i = 0; i < props.value().length; i++) {
					String paramName = props.value()[i];
					Field field = type.getField(paramName);
					InputControl control = inputs.get(field);
					if (control != null && !control.getNode().isDisable()) {
						args[i] = control.getValue();
					} else {
						args[i] = defaultValue(ctor.getParameterTypes()[i]);
					}
				}
				return (T)ctor.newInstance(args);
			}
		}

		// 2. Default constructor
		return type.getDeclaredConstructor().newInstance();
	}

	private Object defaultValue(Class<?> type) {
		if (type == int.class) return 0;
		if (type == long.class) return 0L;
		if (type == double.class) return 0.0;
		if (type == boolean.class) return false;
		return null;
	}

	private interface InputControl {

		Object getValue();

		Node getNode();
	}

	private static class StringInput implements InputControl {

		private final TextField textField;

		public StringInput(String value) {
			textField = new TextField(value != null ? value : "");
			textField.setMaxWidth(Double.MAX_VALUE);
		}

		@Override
		public Object getValue() {return textField.getText();}

		@Override
		public Node getNode() {return textField;}
	}

	private static class BooleanInput implements InputControl {

		private final CheckBox checkBox;

		public BooleanInput(Boolean value) {
			checkBox = new CheckBox();
			checkBox.setSelected(value != null && value);
		}

		@Override
		public Object getValue() {return checkBox.isSelected();}

		@Override
		public Node getNode() {return checkBox;}
	}

	private static class NumberInput implements InputControl {

		private final Type type;
		private final TextField textField;

		public NumberInput(Type type, Object value) {
			this.type = type;
			textField = new TextField(value != null ? value.toString() : "0");
			textField.setMaxWidth(Double.MAX_VALUE);
		}

		@Override
		public Object getValue() {
			String text = textField.getText();
			if (type == int.class || type == Integer.class) return Integer.parseInt(text);
			if (type == long.class || type == Long.class) return Long.parseLong(text);
			if (type == double.class || type == Double.class) return Double.parseDouble(text);
			return null;
		}

		@Override
		public Node getNode() {return textField;}
	}

	private static class DateInput implements InputControl {

		private final DatePicker picker;

		public DateInput(LocalDate value) {
			picker = new DatePicker(value);
			picker.setMaxWidth(Double.MAX_VALUE);
		}

		@Override
		public Object getValue() {return picker.getValue();}

		@Override
		public Node getNode() {return picker;}
	}

	private static class DateTimeInput implements InputControl {

		private final DatePicker picker;

		// Simple implementation, just date part for now as requirement says "picker"
		public DateTimeInput(LocalDateTime value) {
			picker = new DatePicker(value != null ? value.toLocalDate() : null);
			picker.setMaxWidth(Double.MAX_VALUE);
		}

		@Override
		public Object getValue() {
			return picker.getValue() != null ? picker.getValue().atStartOfDay() : null;
		}

		@Override
		public Node getNode() {return picker;}
	}

	private static class EnumInput implements InputControl {

		private final ComboBox<Object> comboBox;

		public EnumInput(Class<?> enumType, Object value) {
			comboBox = new ComboBox<>();
			comboBox.getItems().addAll(enumType.getEnumConstants());
			comboBox.setValue(value);
			comboBox.setMaxWidth(Double.MAX_VALUE);
		}

		@Override
		public Object getValue() {return comboBox.getValue();}

		@Override
		public Node getNode() {return comboBox;}
	}

	private static class ComplexInput implements InputControl {

		private final GenericEditor editor;
		private final TitledPane titledPane;

		public ComplexInput(Class<?> type, Object value) {
			editor = new GenericEditor(type, value);
			titledPane = new TitledPane(type.getSimpleName(), editor);
			titledPane.setExpanded(false);
		}

		@Override
		public Object getValue() {return editor.value();}

		@Override
		public Node getNode() {return titledPane;}
	}

	private static class CollectionInput implements InputControl {

		private final Type collectionType;
		private final Type elementType;
		private final VBox container;
		private final TitledPane titledPane;
		private final List<InputControl> elements = new ArrayList<>();

		public CollectionInput(Type collectionType, Type elementType, Collection<?> value) {
			this.collectionType = collectionType;
			this.elementType = elementType;
			this.container = new VBox(5);
			this.titledPane = new TitledPane("", container);
			this.titledPane.setExpanded(false);

			Button addBtn = new Button("+");
			addBtn.setOnAction(e -> addRow(null));
			container.getChildren().add(addBtn);

			if (value != null) {
				for (Object item : value) addRow(item);
			}

			updateTitle();
		}

		private void updateTitle() {
			Class<?> elementClass = toClass(elementType);
			Class<?> collectionClass = toClass(collectionType);
			titledPane.setText(collectionClass.getSimpleName() + "<" + elementClass.getSimpleName() + "> (" + elements.size() + ")");
		}

		private void addRow(Object value) {
			HBox row = new HBox(5);
			InputControl control = createInputControl(elementType, elementType, value);
			elements.add(control);
			Button removeBtn = new Button("-");
			removeBtn.setOnAction(e -> {
				container.getChildren().remove(row);
				elements.remove(control);
				updateTitle();
			});
			row.getChildren().addAll(control.getNode(), removeBtn);
			container.getChildren().add(container.getChildren().size() - 1, row);
			updateTitle();
		}

		@Override
		public Object getValue() {
			Collection<Object> col = Set.class.isAssignableFrom(toClass(collectionType)) ? new HashSet<>() : new ArrayList<>();
			for (InputControl control : elements) col.add(control.getValue());
			return col;
		}

		@Override
		public Node getNode() {return titledPane;}
	}

	private static class MapInput implements InputControl {

		private final Type keyType;
		private final Type valType;
		private final VBox container;
		private final TitledPane titledPane;
		private final List<MapRow> rows = new ArrayList<>();

		public MapInput(Type keyType, Type valType, Map<?, ?> value) {
			this.keyType = keyType;
			this.valType = valType;
			this.container = new VBox(5);
			this.titledPane = new TitledPane("", container);
			this.titledPane.setExpanded(false);

			Button addBtn = new Button("+");
			addBtn.setOnAction(e -> addRow(null, null));
			container.getChildren().add(addBtn);

			if (value != null) {
				value.forEach(this::addRow);
			}

			updateTitle();
		}

		private void updateTitle() {
			Class<?> keyClass = toClass(keyType);
			Class<?> valClass = toClass(valType);
			titledPane.setText("Map<" + keyClass.getSimpleName() + ", " + valClass.getSimpleName() + "> (" + rows.size() + ")");
		}

		private void addRow(Object k, Object v) {
			HBox rowNode = new HBox(5);

			Class<?> keyClass = toClass(keyType);
			Class<?> valClass = toClass(valType);

			InputControl keyCtrl = createInputControl(keyClass, keyType, k);
			InputControl valCtrl = createInputControl(valClass, valType, v);

			MapRow mapRow = new MapRow(keyCtrl, valCtrl);
			rows.add(mapRow);
			Button removeBtn = new Button("-");
			removeBtn.setOnAction(e -> {
				container.getChildren().remove(rowNode);
				rows.remove(mapRow);
				updateTitle();
			});
			rowNode.getChildren().addAll(new Label("K:"), keyCtrl.getNode(), new Label("V:"), valCtrl.getNode(), removeBtn);
			container.getChildren().add(container.getChildren().size() - 1, rowNode);
			updateTitle();
		}

		@Override
		public Object getValue() {
			Map<Object, Object> map = new LinkedHashMap<>();
			for (MapRow row : rows) map.put(row.key.getValue(), row.val.getValue());
			return map;
		}

		@Override
		public Node getNode() {
			return titledPane;
		}

		private record MapRow(InputControl key, InputControl val) {
		}
	}
}

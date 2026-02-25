We want to implement a simple GUI editor within the `editor` module, for editing various content entities from the various repositories in
the `content` module.

We want to be able to edit the following:

- authors
- addons (SimpleAddonRepository)
- managed
- gametypes
- collections

Each of these has their own Repository implementations (via the RepositoryManager), and are represented by their own content type classes.
Addons have additional subclass implementations.

The basic editor will be simple and generic, and will behave as follows:

- Building blocks:
    - we will need to define a generic and reusable "Editor" type component that can be given an instance of any type
    - it can either be given an instance of that type, where it will populate itself values from the instance, or it can be given a type
      only, which it will use to create the input form. It does not need to create an instance itself.
    - this component will reflect over all public non-transient fields (not functions or getters)
    - renders as a form with labels (field names) on the left and inputs (field values) to the right
    - for simple types like Strings, dates, booleans, numeric types, render an appropriate input (text, picker, checkbox, number field)
    - for Enum types, render a dropdown containing all possible values of the Enum
    - for complex types, render these as nested collapsable/expandable Editor components in place of where the inputs would be
    - for List or Set types, render these as a collapsable/expandable list, and depending on the content type, either include a list of
      inputs (per the simple types above) or nested Editor components (for complex types), with a "+" button to add new items and "-"
      button to remove existing items
    - for Map types, render these as a collapsable/expandable list of key-value pairs, with each value following the same rules as the
      simple or complex types above, including add and removal
    - all fields should be nullable, specified by a checkbox which disables the input. Any existing null values should be rendered as such
      when the form is first loaded

- Editor interaction and data management:
    - for simplicity, the Editor is not re-usable but rather it is created for each instance being edited
    - the parent editor component must be able to scroll. Embedded value or list editors should not scroll independently, and when expanded
      should show their full content
    - changes to form inputs will not be immediately reflected in the underlying data, and adding items to lists or maps will not
      automatically create instances of the underlying types
    - the Editor won't have a Save button embedded but will instead expose a value() or suitably named function that can be called which
      will return the updated instance of the edited type, with all input values recursively applied to it (via public field value setting,
      not calling setters), respecting the null state of each field where set
    - when creating instances of complex types with non-default constructors, the editor should be able to call the appropriate constructor
      with the appropriate arguments, potentially using @ConstructorProperties annotations where appropriate to match argument names and
      order

- Repositories:
    - include a tab across the top of the interface for each repository type
    - for now, we will only support the SimpleAddonRepository (call it "Content")
    - there is no ability to create new instances of addons or other content types, only edit existing ones
    - at the top of the Content tab, include a plain text input box and a "Edit" button, which allows the user to retrieve an addon by
      hash (SimpleAddonRepository.forHash) entered
    - on successful retrieval, the editor will be created in the lower portion of the interface below the input area, for the retrieved
      addon.
    - on failed retrieval, an error message will be shown below the input area instead
    - below the editor, a save or cancel button will be shown, cancel will do nothing but close the editor
    - save will get the updated instance value from the Editor, and call the appropriate repository method to save the edited addon, and
      will show a success or failure message

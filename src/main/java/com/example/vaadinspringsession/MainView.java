package com.example.vaadinspringsession;

import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.router.PreserveOnRefresh;
import com.vaadin.flow.router.Route;

@Route
@PreserveOnRefresh
public class MainView extends VerticalLayout {

    public MainView() {
        TextField labelField = new TextField();
        labelField.setLabel("Label");

        TextField placeholderField = new TextField();
        placeholderField.setPlaceholder("Placeholder");

        TextField valueField = new TextField();
        valueField.setValue("Value");

        add(labelField, placeholderField, valueField);
    }
}

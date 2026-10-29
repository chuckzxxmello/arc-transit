package com.transit.arctransit.common.ui;

import com.vaadin.flow.component.combobox.ComboBox;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

import java.util.List;
import java.util.function.BiFunction;

public final class RecordPicker {
    private RecordPicker() {
    }

    public record Option(Long id, String label) {
    }

    public static ComboBox<Option> create(String label,
            BiFunction<String, Pageable, List<Option>> search) {
        ComboBox<Option> picker = FormInputs.choice(new ComboBox<>(label));
        picker.setItemLabelGenerator(Option::label);
        picker.setPlaceholder("Type a unit number, name or code and select a result");
        picker.setWidthFull();
        picker.setClearButtonVisible(true);
        picker.setItems(query -> search.apply(query.getFilter().orElse(""),
                PageRequest.of(query.getPage(), query.getPageSize(), Sort.by("id"))).stream());
        return picker;
    }
}

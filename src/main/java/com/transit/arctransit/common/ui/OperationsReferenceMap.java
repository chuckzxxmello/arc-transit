package com.transit.arctransit.common.ui;

import com.vaadin.flow.component.html.IFrame;

public class OperationsReferenceMap extends IFrame {
    public OperationsReferenceMap() {
        setSrc("arc-ui/map/index.html");
        setTitle("Cavite road map and weather reference");
        setSizeFull();
        getStyle().set("border", "0").set("min-height", "240px");
    }
}

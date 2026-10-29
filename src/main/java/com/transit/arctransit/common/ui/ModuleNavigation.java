package com.transit.arctransit.common.ui;

import com.vaadin.flow.component.ClientCallable;
import com.vaadin.flow.component.dependency.JavaScript;
import com.vaadin.flow.component.dependency.StyleSheet;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.sidenav.SideNav;
import com.vaadin.flow.component.sidenav.SideNavItem;
import org.vaadin.jchristophe.SortableConfig;
import org.vaadin.jchristophe.SortableLayout;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@JavaScript("context://arc-ui/sidebar-controls.js")
@StyleSheet("context://arc-ui/sidebar-sortable.css")
public class ModuleNavigation extends VerticalLayout {
    private final SideNav navigation;
    private final Map<String, SideNavItem> items = new LinkedHashMap<>();
    private final String storageKey;

    public ModuleNavigation(SideNav navigation, String username) {
        this.navigation = navigation;
        this.storageKey = "arc-transit.module-order." + username;
        navigation.getItems().forEach(item -> items.put(item.getPath(), item));
        setPadding(false);
        setSpacing(false);
        setWidthFull();
        addClassName("arc-module-navigation");
        navigation.setWidthFull();
        items.forEach((path, item) -> {
            item.getElement().setAttribute("data-module-path", path);
            item.getElement().setAttribute("title", "Click to open. Drag up or down to reorder.");
            Span handle = new Span();
            handle.addClassName("arc-module-grip");
            handle.getElement().setAttribute("role", "button");
            handle.getElement().setAttribute("tabindex", "0");
            handle.getElement().setAttribute("aria-label", "Reorder " + item.getLabel());
            handle.getElement().setAttribute("title", "Drag to reorder. Keyboard: Alt + Up or Down.");
            item.setSuffixComponent(handle);
        });
        Span announcement = new Span();
        announcement.addClassName("arc-sort-announcement");
        announcement.getElement().setAttribute("aria-live", "polite");
        SortableConfig config = new SortableConfig() {
            @Override
            public tools.jackson.databind.node.ObjectNode toJson() {
                return super.toJson().put("forceFallback", true).put("fallbackOnBody", true)
                        .put("draggable", "vaadin-side-nav-item").put("direction", "vertical")
                        .put("fallbackTolerance", 5);
            }
        };
        config.setAnimation(150);
        config.setGhostClass("arc-module-placeholder");
        config.setChosenClass("arc-module-chosen");
        config.allowDragIn(false);
        config.allowDragOut(false);
        SortableLayout sortable = new SortableLayout(navigation, config);
        sortable.setWidthFull();
        sortable.getStyle().set("display", "block");
        sortable.addSortableComponentReorderListener(event -> {
            persist();
            announcement.setText("Module moved to position " + (event.getIndex() + 1) + " of " + items.size() + ".");
        });
        Span hint = new Span("Drag modules to reorder");
        hint.addClassName("arc-sort-hint");
        add(hint, sortable, announcement);
        addAttachListener(event -> getElement().executeJs("""
                window.ArcSidebar.install(this);
                try { return localStorage.getItem($0) || ''; } catch (error) { return ''; }
                """, storageKey).then(String.class, saved -> {
                    if (!saved.isEmpty()) applyOrder(List.of(saved.split(",", -1)));
                }));
    }

    List<String> currentOrder() {
        return navigation.getItems().stream().map(SideNavItem::getPath).toList();
    }

    void applyOrder(List<String> requested) {
        List<String> order = new ArrayList<>();
        for (String path : requested) {
            if (items.containsKey(path) && !order.contains(path)) order.add(path);
        }
        items.keySet().stream().filter(path -> !order.contains(path)).forEach(order::add);
        for (int index = 0; index < order.size(); index++) {
            var item = items.get(order.get(index)).getElement();
            if (!navigation.getElement().getChild(index).equals(item)) {
                navigation.getElement().insertChild(index, item);
            }
        }
    }

    @ClientCallable
    public void saveOrder(String paths) {
        if (paths == null || paths.length() > 2000) return;
        applyOrder(List.of(paths.split(",", -1)));
        persist();
    }

    private void persist() {
        getElement().executeJs("""
                try { localStorage.setItem($0, $1); } catch (error) { /* Keep this session's order. */ }
                """, storageKey, String.join(",", currentOrder()));
    }
}

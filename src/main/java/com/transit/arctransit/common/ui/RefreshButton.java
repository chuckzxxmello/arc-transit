package com.transit.arctransit.common.ui;

import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.icon.VaadinIcon;

public class RefreshButton extends Button {
    public RefreshButton(Runnable refresh) {
        super(VaadinIcon.REFRESH.create());
        setAriaLabel("Refresh data");
        setTooltipText("Refresh data");
        addThemeVariants(ButtonVariant.LUMO_SMALL, ButtonVariant.LUMO_TERTIARY);
        addAttachListener(event -> getElement().executeJs("""
                if (!this.arcRefreshAnimation) {
                    this.arcRefreshAnimation = true;
                    this.addEventListener('click', () => {
                        if (this.disabled || matchMedia('(prefers-reduced-motion: reduce)').matches) return;
                        this.querySelector('vaadin-icon')?.animate(
                            [{transform: 'rotate(0deg)'}, {transform: 'rotate(360deg)'}],
                            {duration: 650, easing: 'ease-in-out'});
                    });
                }
                """));
        addClickListener(event -> {
            try {
                refresh.run();
            } catch (RuntimeException exception) {
                ApplicationErrors.show(exception);
            }
        });
    }
}

(() => {
    window.ArcSidebar = {
        install(host) {
            if (host.arcSidebarControlsInstalled) return;
            host.arcSidebarControlsInstalled = true;
            const nav = host.querySelector('vaadin-side-nav');
            const wrapper = host.querySelector('sortable');
            const rows = () => [...nav.children].filter(row => row.hasAttribute('data-module-path'));
            // SideNav's shadow-root links escape Sortable's native-link drag suppression.
            nav.addEventListener('dragstart', event => event.preventDefault(), true);
            let saving = false;
            let dragging = false;
            host.addEventListener('click', event => {
                if (event.composedPath().some(node => node.classList?.contains('arc-module-grip'))) {
                    event.preventDefault();
                    event.stopPropagation();
                }
            }, true);
            host.addEventListener('keydown', event => {
                const handle = event.target.closest('.arc-module-grip');
                if (!handle || !event.altKey || !['ArrowUp', 'ArrowDown'].includes(event.key)) return;
                event.preventDefault();
                event.stopPropagation();
                if (saving || dragging) return;
                const row = handle.closest('[data-module-path]');
                const order = rows();
                const index = order.indexOf(row);
                const next = index + (event.key === 'ArrowUp' ? -1 : 1);
                if (next < 0 || next >= order.length) return;
                [order[index], order[next]] = [order[next], order[index]];
                saving = true;
                const sortable = wrapper.$connector?.sortable;
                sortable?.option('disabled', true);
                host.$server.saveOrder(order.map(item => item.dataset.modulePath).join(',')).then(() => {
                    host.querySelector('.arc-sort-announcement').textContent =
                        `Module moved to position ${next + 1} of ${order.length}.`;
                    handle.focus();
                }).catch(() => {
                    host.querySelector('.arc-sort-announcement').textContent =
                        'Order could not be saved. Reload the page and try again.';
                }).finally(() => {
                    saving = false;
                    sortable?.option('disabled', false);
                });
            });
            wrapper.addEventListener('on-choose', () => {
                const original = rows();
                const sortable = wrapper.$connector.sortable;
                const end = sortable.options.onEnd;
                let cancelled = false;
                let completed = false;
                dragging = true;
                const escape = event => { if (event.key === 'Escape') cancelled = true; };
                const cleanup = () => {
                    document.removeEventListener('keydown', escape, true);
                    wrapper.removeEventListener('on-unchoose', unchoose);
                    dragging = false;
                    sortable.option('onEnd', end);
                };
                const unchoose = () => queueMicrotask(() => { if (!completed) cleanup(); });
                wrapper.addEventListener('on-unchoose', unchoose);
                document.addEventListener('keydown', escape, true);
                sortable.option('onEnd', event => {
                    completed = true;
                    cleanup();
                    if (cancelled) {
                        // The add-on receives a no-op reorder after restoring Flow's known order.
                        original.forEach(row => nav.appendChild(row));
                        event.newIndex = event.oldIndex;
                    }
                    end.call(sortable, event);
                });
            });
        }
    };
})();

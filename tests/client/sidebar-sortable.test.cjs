const {test} = require('node:test');
const assert = require('node:assert/strict');
const fs = require('node:fs');
const path = require('node:path');
const vm = require('node:vm');
const source = fs.readFileSync(path.join(__dirname,
    '../../src/main/resources/META-INF/resources/arc-ui/sidebar-controls.js'), 'utf8');

class Events {
    listeners = new Map();
    addEventListener(type, listener) {
        const entries = this.listeners.get(type) || [];
        entries.push(listener);
        this.listeners.set(type, entries);
    }
    removeEventListener(type, listener) {
        this.listeners.set(type, (this.listeners.get(type) || []).filter(entry => entry !== listener));
    }
    fire(type, event) { [...(this.listeners.get(type) || [])].forEach(listener => listener(event)); }
}

function fixture() {
    const calls = [];
    const drops = [];
    const nav = Object.assign(new Events(), {children: [], appendChild(row) { this.insertBefore(row, null); },
        insertBefore(row, before) {
            const existing = this.children.indexOf(row);
            if (existing >= 0) this.children.splice(existing, 1);
            const index = before ? this.children.indexOf(before) : this.children.length;
            this.children.splice(index, 0, row);
        }});
    for (const name of ['dashboard', 'dispatch', 'fleet']) {
        const row = {dataset: {modulePath: name}, textContent: name,
            hasAttribute: attribute => attribute === 'data-module-path'};
        const handle = {closest: selector => selector === '.arc-module-grip' ? handle : row,
            focus() {}, classList: {contains: value => value === 'arc-module-grip'}};
        row.handle = handle;
        nav.children.push(row);
    }
    const wrapper = new Events();
    wrapper.$connector = {sortable: {
        options: {onEnd(event) { drops.push({oldIndex: event.oldIndex, newIndex: event.newIndex,
            dom: nav.children.map(row => row.dataset.modulePath).join(',')}); }},
        option(name, value) { this.options[name] = value; }
    }};
    const announcement = {textContent: ''};
    const host = new Events();
    host.querySelector = selector => selector === 'vaadin-side-nav' ? nav : selector === 'sortable' ? wrapper : announcement;
    host.$server = {saveOrder(order) {
        calls.push({order, dom: nav.children.map(row => row.dataset.modulePath).join(',')});
        return Promise.resolve();
    }};
    const doc = new Events();
    const win = {};
    vm.runInNewContext(source, {window: win, document: doc, queueMicrotask});
    win.ArcSidebar.install(host);
    const event = (row, extra = {}) => ({target: row.handle,
        preventDefault() {}, stopPropagation() {}, ...extra});
    return {host, doc, nav, calls, event, wrapper, drops};
}
const settle = () => new Promise(resolve => setImmediate(resolve));

test('native link dragging is cancelled so shadow-root links cannot interrupt add-on pointer sorting', () => {
    const ui = fixture();
    let prevented = false;
    ui.nav.fire('dragstart', {preventDefault() { prevented = true; }});
    assert.equal(prevented, true);
    assert.equal(ui.calls.length, 0);
});

test('keyboard movement preserves Flow DOM and briefly disables add-on dragging during its save', async () => {
    const ui = fixture();
    ui.host.fire('keydown', ui.event(ui.nav.children[2], {key: 'ArrowUp', altKey: true}));
    assert.deepEqual(ui.calls, [{order: 'dashboard,fleet,dispatch', dom: 'dashboard,dispatch,fleet'}]);
    assert.equal(ui.wrapper.$connector.sortable.options.disabled, true);
    await settle();
    assert.equal(ui.wrapper.$connector.sortable.options.disabled, false);
});

test('normal pointer drops are handled by the add-on without a second custom save', () => {
    const ui = fixture();
    ui.wrapper.fire('on-choose');
    ui.nav.insertBefore(ui.nav.children[2], ui.nav.children[0]);
    ui.wrapper.$connector.sortable.options.onEnd({oldIndex: 2, newIndex: 0});
    assert.deepEqual(ui.drops, [{oldIndex: 2, newIndex: 0, dom: 'fleet,dashboard,dispatch'}]);
    assert.equal(ui.calls.length, 0);
});

test('Escape restores the original DOM and delegates a no-op reorder to the add-on', () => {
    const ui = fixture();
    ui.wrapper.fire('on-choose');
    ui.nav.insertBefore(ui.nav.children[2], ui.nav.children[0]);
    ui.doc.fire('keydown', {key: 'Escape'});
    ui.wrapper.$connector.sortable.options.onEnd({oldIndex: 2, newIndex: 0});
    assert.deepEqual(ui.drops, [{oldIndex: 2, newIndex: 2, dom: 'dashboard,dispatch,fleet'}]);
    assert.equal(ui.calls.length, 0);
});

test('handle clicks do not navigate while ordinary module label clicks remain untouched', () => {
    const ui = fixture();
    let prevented = 0;
    const handle = ui.nav.children[0].handle;
    ui.host.fire('click', {composedPath: () => [handle], preventDefault() { prevented++; }, stopPropagation() {}});
    assert.equal(prevented, 1);
    ui.host.fire('click', {composedPath: () => [ui.nav.children[0]], preventDefault() { prevented++; }, stopPropagation() {}});
    assert.equal(prevented, 1);
});

test('choosing a handle without actually dragging does not leave keyboard controls locked', async () => {
    const ui = fixture();
    ui.wrapper.fire('on-choose');
    ui.wrapper.fire('on-unchoose');
    await settle();
    ui.host.fire('keydown', ui.event(ui.nav.children[2], {key: 'ArrowUp', altKey: true}));
    assert.equal(ui.calls.length, 1);
});

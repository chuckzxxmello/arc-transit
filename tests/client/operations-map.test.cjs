const {test} = require('node:test');
const assert = require('node:assert/strict');
const fs = require('node:fs');
const vm = require('node:vm');
const path = require('node:path');
const source = fs.readFileSync(path.join(__dirname,
    '../../src/main/resources/META-INF/resources/arc-ui/map/map.js'), 'utf8');

const forecast = temperature => ({
    current: {time: '2026-10-19T14:15', temperature_2m: temperature,
        wind_speed_10m: null, precipitation: 0, weather_code: 3},
    hourly: {time: ['2026-10-19T14:00', '2026-10-19T15:00', '2026-10-19T16:00', '2026-10-19T17:00'],
        precipitation_probability: [99, 20, 40, 30]}
});

function fixture(fetch) {
    const nodes = new Map();
    const node = id => {
        if (!nodes.has(id)) nodes.set(id, {textContent: '', hidden: true, disabled: false, events: {},
            addEventListener(type, listener) { this.events[type] = listener; }});
        return nodes.get(id);
    };
    const map = {events: {}, views: [], setView(...args) { this.views.push(args); return this; },
        on(type, callback) { this.events[type] = callback; }, invalidateSize() {}};
    const tiles = {events: {}, addTo() { return this; }, on(type, callback) { this.events[type] = callback; }};
    const pin = {addTo() { return this; }, bindTooltip() { return this; }, setLatLng() {}};
    const L = {map: () => map, tileLayer: () => tiles, circleMarker: () => pin,
        control: {scale: () => ({addTo() {}})}};
    const context = {window: {L}, L, document: {getElementById: node}, fetch, URLSearchParams,
        AbortController, ResizeObserver: class {observe() {}}, setTimeout: () => 1, clearTimeout() {}};
    vm.runInNewContext(source, context);
    return {node, map, tiles};
}
const settle = () => new Promise(resolve => setImmediate(resolve));

test('weather outage preserves map controls and enables retry without showing invented readings', async () => {
    const ui = fixture(async () => { throw new Error('offline'); });
    await settle();
    assert.match(ui.node('condition').textContent, /Weather unavailable/);
    assert.equal(ui.node('temperature').textContent, '--');
    assert.equal(ui.node('refresh').disabled, false);
    ui.node('home').events.click();
    assert.equal(ui.map.views.at(-1)[1], 10);
    ui.tiles.events.tileerror();
    assert.equal(ui.node('tile-status').hidden, false);
    ui.tiles.events.tileload();
    assert.equal(ui.node('tile-status').hidden, true);
});

test('real model fields retain zero precipitation, identify missing data and cache repeat requests', async () => {
    let calls = 0;
    const ui = fixture(async () => { calls++; return {ok: true, json: async () => forecast(28)}; });
    await settle();
    assert.equal(ui.node('temperature').textContent, '28 \u00b0C');
    assert.equal(ui.node('rain').textContent, '0 mm');
    assert.equal(ui.node('wind').textContent, 'Unavailable');
    assert.equal(ui.node('chance').textContent, '40%');
    assert.match(ui.node('weather-time').textContent, /2026-10-19 14:15 \(Asia\/Manila\)/);
    await ui.node('refresh').events.click();
    assert.equal(calls, 1);
});

test('slow previous location cannot overwrite the newly selected area even if the transport ignores abort', async () => {
    const requests = [];
    const ui = fixture((url, options) => new Promise(resolve => requests.push({url, options, resolve})));
    ui.node('area').events.change({target: {value: '14.1153,120.9621', selectedOptions: [{textContent: 'Tagaytay'}]}});
    assert.equal(requests[0].options.signal.aborted, true);
    assert.match(requests[1].url, /latitude=14.115&longitude=120.962/);
    requests[1].resolve({ok: true, json: async () => forecast(23)});
    await settle();
    requests[0].resolve({ok: true, json: async () => forecast(32)});
    await settle();
    assert.equal(ui.node('place').textContent, 'Tagaytay');
    assert.equal(ui.node('temperature').textContent, '23 \u00b0C');
    assert.equal(ui.node('refresh').disabled, false);
});

test('rate limiting and malformed provider data can be retried and never become valid cached weather', async () => {
    const replies = [{ok: false}, {ok: true, json: async () => ({current: {temperature_2m: null}})},
        {ok: true, json: async () => forecast(27)}];
    const ui = fixture(async () => replies.shift());
    await settle();
    assert.match(ui.node('condition').textContent, /Weather unavailable/);
    await ui.node('refresh').events.click();
    assert.equal(ui.node('temperature').textContent, '--');
    await ui.node('refresh').events.click();
    assert.equal(ui.node('temperature').textContent, '27 \u00b0C');
    assert.equal(replies.length, 0);
});

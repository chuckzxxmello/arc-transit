(() => {
    const element = id => document.getElementById(id);
    const status = element('tile-status');
    if (!window.L) {
        status.hidden = false;
        status.textContent = 'Map could not load. Refresh this page to try again.';
        return;
    }
    const map = L.map('map', {scrollWheelZoom: false}).setView([14.3294, 120.9367], 11);
    const tiles = L.tileLayer('https://tile.openstreetmap.org/{z}/{x}/{y}.png', {
        maxZoom: 19,
        attribution: '&copy; <a href="https://www.openstreetmap.org/copyright" target="_blank" rel="noopener noreferrer">OpenStreetMap contributors</a>'
    }).addTo(map);
    tiles.on('tileerror', () => {
        status.hidden = false;
        status.textContent = 'Some map tiles are unavailable. Check your connection and try another area or reload.';
    });
    tiles.on('tileload', () => { status.hidden = true; });
    new ResizeObserver(() => map.invalidateSize()).observe(element('map'));
    L.control.scale({imperial: false}).addTo(map);
    let selection = {lat: 14.3294, lon: 120.9367, name: 'Dasmarinas'};
    const pin = L.circleMarker([selection.lat, selection.lon], {
        radius: 7, color: '#fff', weight: 2, fillColor: '#3264df', fillOpacity: 1
    }).addTo(map).bindTooltip('Weather location');
    let request;
    let sequence = 0;
    const cache = new Map();
    const conditions = new Map([
        [0, 'Clear sky'], [1, 'Mainly clear'], [2, 'Partly cloudy'], [3, 'Overcast'],
        [45, 'Fog'], [48, 'Rime fog'], [51, 'Light drizzle'], [53, 'Drizzle'], [55, 'Heavy drizzle'],
        [56, 'Freezing drizzle'], [57, 'Heavy freezing drizzle'], [61, 'Light rain'], [63, 'Rain'],
        [65, 'Heavy rain'], [66, 'Freezing rain'], [67, 'Heavy freezing rain'], [71, 'Light snow'],
        [73, 'Snow'], [75, 'Heavy snow'], [77, 'Snow grains'], [80, 'Light showers'], [81, 'Showers'],
        [82, 'Heavy showers'], [85, 'Snow showers'], [86, 'Heavy snow showers'],
        [95, 'Thunderstorm'], [96, 'Thunderstorm with hail'], [99, 'Thunderstorm with heavy hail']
    ]);
    const display = data => {
        const current = data.current;
        if (!current?.time || !Number.isFinite(current.temperature_2m)) throw new Error('Invalid weather');
        const measurement = (id, value, unit) => {
            element(id).textContent = Number.isFinite(value) ? `${value} ${unit}` : 'Unavailable';
        };
        element('condition').textContent = conditions.get(current.weather_code) || 'Weather condition unavailable';
        measurement('temperature', current.temperature_2m, '\u00b0C');
        measurement('wind', current.wind_speed_10m, 'km/h');
        measurement('rain', current.precipitation, 'mm');
        const probabilities = (data.hourly?.time || []).map((time, index) =>
            time >= current.time ? data.hourly.precipitation_probability?.[index] : null)
            .filter(value => Number.isFinite(value)).slice(0, 3);
        element('chance').textContent = probabilities.length === 3 ? `${Math.max(...probabilities)}%` : 'Unavailable';
        element('weather-time').textContent = `Model time: ${current.time.replace('T', ' ')} (Asia/Manila)`;
    };
    async function weather() {
        const version = ++sequence;
        request?.abort();
        element('place').textContent = selection.name;
        element('condition').textContent = 'Loading weather...';
        ['temperature', 'wind', 'rain', 'chance'].forEach(id => { element(id).textContent = '--'; });
        element('weather-time').textContent = '';
        const refresh = element('refresh');
        refresh.disabled = true;
        const controller = new AbortController();
        request = controller;
        const timer = setTimeout(() => controller.abort(), 12000);
        try {
            const key = `${selection.lat.toFixed(3)},${selection.lon.toFixed(3)}`;
            const cached = cache.get(key);
            let data = cached?.expires > Date.now() ? cached.data : null;
            if (!data) {
                const params = new URLSearchParams({
                    latitude: selection.lat.toFixed(3), longitude: selection.lon.toFixed(3),
                    current: 'temperature_2m,precipitation,weather_code,wind_speed_10m',
                    hourly: 'precipitation_probability', timezone: 'Asia/Manila', forecast_days: '2'
                });
                const response = await fetch(`https://api.open-meteo.com/v1/forecast?${params}`, {signal: controller.signal});
                if (!response.ok) throw new Error('Weather unavailable');
                data = await response.json();
                if (!data.current?.time || !Number.isFinite(data.current.temperature_2m)) throw new Error('Invalid weather');
                if (cache.size >= 30) cache.delete(cache.keys().next().value);
                cache.set(key, {data, expires: Date.now() + 600000});
            }
            if (version === sequence) display(data);
        } catch (error) {
            if (version === sequence) element('condition').textContent = 'Weather unavailable. You can still use the map. Try refresh later.';
        } finally {
            clearTimeout(timer);
            if (version === sequence) refresh.disabled = false;
        }
    }
    function choose(lat, lon, name) {
        selection = {lat, lon, name};
        pin.setLatLng([lat, lon]);
        weather();
    }
    element('area').addEventListener('change', event => {
        const [lat, lon] = event.target.value.split(',').map(Number);
        choose(lat, lon, event.target.selectedOptions[0].textContent);
        map.setView([lat, lon], 12);
    });
    map.on('click', event => {
        const lat = Math.max(-90, Math.min(90, event.latlng.lat));
        const lon = ((event.latlng.lng + 180) % 360 + 360) % 360 - 180;
        element('area').selectedIndex = -1;
        choose(lat, lon, `${lat.toFixed(3)}, ${lon.toFixed(3)}`);
    });
    element('home').addEventListener('click', () => map.setView([14.3, 120.9], 10));
    element('refresh').addEventListener('click', weather);
    weather();
})();

// hubitat-api.js — Hubitat Maker API + EventSocket adapter layer
// Translates between Hubitat's data model and the dashboard's stateCache format.
// Loaded before tile-engine.js. Expects globals: stateCache, dimLocks, render()

const MAX_BRIGHTNESS = 255;

const HubitatAPI = {
  // Connection config — set by loadConfig() or manually
  hubUrl: '',
  appId: '',
  token: '',

  // EventSocket state
  _ws: null,
  _wsReconnectTimer: null,
  _wsReconnectMs: 2000,
  _knownDeviceIds: new Set(),

  // Device type map — built at init from config data, maps deviceId -> tile type
  deviceTypeMap: {},

  // --- Capability-to-tile-type mapping (for auto-discovery) ---
  CAPABILITY_TYPE_MAP: {
    'SwitchLevel':                'light',
    'ColorControl':               'light',
    'ColorTemperature':           'light',
    'Switch':                     'switch',
    'FanControl':                 'fan',
    'WindowShade':                'cover',
    'WindowBlind':                'cover',
    'GarageDoorControl':          'cover',
    'ContactSensor':              'door',
    'MotionSensor':               'motion',
    'TemperatureMeasurement':     'sensor',
    'RelativeHumidityMeasurement':'sensor',
    'IlluminanceMeasurement':     'sensor',
    'MusicPlayer':                'media',
    'AudioVolume':                'media',
    'Thermostat':                 'climate',
  },

  // Priority order for capability detection (first match wins)
  CAPABILITY_PRIORITY: [
    'FanControl', 'ColorControl', 'ColorTemperature', 'SwitchLevel',
    'WindowShade', 'WindowBlind', 'GarageDoorControl',
    'Thermostat', 'MusicPlayer', 'AudioVolume',
    'ContactSensor', 'MotionSensor',
    'TemperatureMeasurement', 'RelativeHumidityMeasurement', 'IlluminanceMeasurement',
    'Switch',
  ],

  // --- Speed mapping helpers ---
  speedToPercent(speed) {
    if (!speed || typeof speed !== 'string') return 0;
    const map = {
      'off': 0, 'low': 20, 'medium-low': 40, 'medium': 50,
      'medium-high': 75, 'high': 100, 'on': 50, 'auto': 50
    };
    return map[speed.toLowerCase()] || 0;
  },

  percentToSpeed(pct) {
    if (pct <= 0) return 'off';
    if (pct <= 20) return 'low';
    if (pct <= 40) return 'medium-low';
    if (pct <= 60) return 'medium';
    if (pct <= 80) return 'medium-high';
    return 'high';
  },

  // --- Brightness scale conversion ---
  // Hubitat: 0-100 level, Dashboard: 0-255 brightness
  levelToBrightness(level) {
    if (!level && level !== 0) return 0;
    return Math.round(parseFloat(level) * 2.55);
  },

  brightnessToLevel(brightness) {
    if (!brightness && brightness !== 0) return 0;
    return Math.round(brightness / 2.55);
  },

  // --- API URL builder ---
  apiUrl(path) {
    if (!this.hubUrl || !this.appId || !this.token) return '';
    return `${this.hubUrl}/apps/api/${this.appId}/${path}?access_token=${this.token}`;
  },

  // --- Maker API: send command to device ---
  async sendCommand(deviceId, command, param) {
    if (!deviceId || !command) return;
    let path = `devices/${deviceId}/${command}`;
    if (param !== undefined && param !== null) path += `/${param}`;
    const url = this.apiUrl(path);
    if (!url) return;
    // Fire-and-forget: response intentionally unchecked
    try { await fetch(url); } catch (e) { /* network error */ }
  },

  // --- Maker API: fetch single device ---
  async getDevice(deviceId) {
    if (!deviceId) return null;
    const url = this.apiUrl(`devices/${deviceId}`);
    if (!url) return null;
    try {
      const resp = await fetch(url);
      if (!resp.ok) return null;
      return resp.json();
    } catch (e) { return null; }
  },

  // --- Maker API: fetch all devices ---
  async fetchAllDevices() {
    const url = this.apiUrl('devices/all');
    if (!url) return null;
    try {
      const resp = await fetch(url);
      if (!resp.ok) return null;
      return resp.json();
    } catch (e) { return null; }
  },

  // --- State normalization: Hubitat device -> stateCache entries ---
  getAttrValue(device, attrName) {
    if (!device || !device.attributes) return undefined;
    for (let i = 0; i < device.attributes.length; i++) {
      if (device.attributes[i].name === attrName) return device.attributes[i].currentValue;
    }
    return undefined;
  },

  normalizeDevice(device) {
    if (!device || !device.id) return;
    const id = String(device.id);
    const type = this.deviceTypeMap[id];
    if (!type) return;

    // Primary state based on tile type
    if (type === 'light' || type === 'switch') {
      const sw = this.getAttrValue(device, 'switch');
      if (sw !== undefined && !isDimLocked(id)) stateCache[id] = sw;

      // Brightness / dimmable
      const level = this.getAttrValue(device, 'level');
      if (level !== undefined) {
        stateCache[id + '_dimmable'] = true;
        if (!isDimLocked(id)) {
          const hasAnim = typeof dimAnimations !== 'undefined' && dimAnimations[id];
          if (!hasAnim) {
            stateCache[id + '_brightness'] = this.levelToBrightness(level);
          }
        }
      }
    } else if (type === 'fan') {
      const sw = this.getAttrValue(device, 'switch');
      if (sw !== undefined && !isDimLocked(id)) stateCache[id] = sw;

      const speed = this.getAttrValue(device, 'speed');
      if (speed !== undefined && !isDimLocked(id)) {
        stateCache[id + '_percentage'] = this.speedToPercent(speed);
      }
      // Also store level for fans that support it
      const level = this.getAttrValue(device, 'level');
      if (level !== undefined && !isDimLocked(id)) {
        stateCache[id + '_brightness'] = this.levelToBrightness(level);
      }
    } else if (type === 'cover') {
      const shade = this.getAttrValue(device, 'windowShade');
      if (shade !== undefined && !isDimLocked(id)) {
        stateCache[id] = (shade === 'closed') ? 'closed' : 'open';
      }
      // Fallback: some covers use door/switch attribute
      if (shade === undefined) {
        const door = this.getAttrValue(device, 'door');
        if (door !== undefined && !isDimLocked(id)) {
          stateCache[id] = (door === 'closed') ? 'closed' : 'open';
        }
      }
    } else if (type === 'door') {
      const contact = this.getAttrValue(device, 'contact');
      if (contact !== undefined && !isDimLocked(id)) {
        stateCache[id] = (contact === 'open') ? 'on' : 'off';
      }
    } else if (type === 'motion') {
      const motion = this.getAttrValue(device, 'motion');
      if (motion !== undefined && !isDimLocked(id)) {
        stateCache[id] = (motion === 'active') ? 'on' : 'off';
      }
    } else if (type === 'media') {
      this._normalizeMedia(device, id);
    } else if (type === 'sensor' || type === 'climate' || type === 'temp_humidity' || type === 'ups') {
      // Sensor types: store raw attribute values
      this._normalizeSensor(device, id, type);
    }
  },

  _normalizeMedia(device, id) {
    if (!device || !id) return;
    // Hubitat media players use 'status' attribute for playing/paused/stopped
    const status = this.getAttrValue(device, 'status');
    if (status !== undefined && !isDimLocked(id)) {
      const s = status.toLowerCase();
      if (s === 'playing') stateCache[id] = 'playing';
      else if (s === 'paused') stateCache[id] = 'paused';
      else stateCache[id] = 'off';
    }

    // Volume: Hubitat 0-100, dashboard 0.0-1.0
    const volume = this.getAttrValue(device, 'volume');
    if (volume !== undefined && !isDimLocked(id)) {
      stateCache[id + '_volume'] = parseFloat(volume) / 100;
    }

    // Track info (varies by driver)
    const trackDesc = this.getAttrValue(device, 'trackDescription');
    if (trackDesc) {
      stateCache[id + '_title'] = trackDesc;
      stateCache[id + '_artist'] = '';
    }
    const trackData = this.getAttrValue(device, 'trackData');
    if (trackData && typeof trackData === 'string') {
      try {
        const td = JSON.parse(trackData);
        if (td.title) stateCache[id + '_title'] = td.title;
        if (td.artist) stateCache[id + '_artist'] = td.artist;
        if (td.albumArtUrl) stateCache[id + '_picture'] = td.albumArtUrl;
        if (td.duration) stateCache[id + '_duration'] = td.duration;
        if (td.position) stateCache[id + '_position'] = td.position;
      } catch (e) { /* not JSON */ }
    }

    // Album art from direct attribute
    const art = this.getAttrValue(device, 'albumArtUrl');
    if (art) stateCache[id + '_picture'] = art;
  },

  _normalizeSensor(device, id, type) {
    if (!device || !id) return;
    // For generic sensors, pick the most relevant attribute
    const temp = this.getAttrValue(device, 'temperature');
    if (temp !== undefined) stateCache[id] = String(temp);

    const humidity = this.getAttrValue(device, 'humidity');
    if (humidity !== undefined) {
      // If this is the primary sensor and type is sensor, use humidity as main state
      if (type === 'sensor' && temp === undefined) stateCache[id] = String(humidity);
    }

    const illuminance = this.getAttrValue(device, 'illuminance');
    if (illuminance !== undefined && temp === undefined && humidity === undefined) {
      stateCache[id] = String(illuminance);
    }

    const battery = this.getAttrValue(device, 'battery');
    if (battery !== undefined) stateCache[id + '_battery'] = String(battery);

    // Climate / thermostat
    if (type === 'climate') {
      const mode = this.getAttrValue(device, 'thermostatMode');
      if (mode !== undefined) stateCache[id] = mode;
    }
  },

  // Normalize a single attribute event from EventSocket
  normalizeEvent(msg) {
    if (!msg || !msg.deviceId) return false;
    const id = String(msg.deviceId);
    if (!this._knownDeviceIds.has(id)) return false;
    if (isDimLocked(id)) return false;

    const type = this.deviceTypeMap[id];
    const name = msg.name;
    const value = msg.value;

    if (name === 'switch') {
      stateCache[id] = value;
    } else if (name === 'level') {
      const hasAnim = typeof dimAnimations !== 'undefined' && dimAnimations[id];
      if (!hasAnim) {
        stateCache[id + '_brightness'] = this.levelToBrightness(value);
      }
    } else if (name === 'speed') {
      stateCache[id + '_percentage'] = this.speedToPercent(value);
    } else if (name === 'volume') {
      stateCache[id + '_volume'] = parseFloat(value) / 100;
    } else if (name === 'contact') {
      stateCache[id] = (value === 'open') ? 'on' : 'off';
    } else if (name === 'motion') {
      stateCache[id] = (value === 'active') ? 'on' : 'off';
    } else if (name === 'windowShade') {
      stateCache[id] = (value === 'closed') ? 'closed' : 'open';
    } else if (name === 'door') {
      stateCache[id] = (value === 'closed') ? 'closed' : 'open';
    } else if (name === 'temperature') {
      stateCache[id] = String(value);
    } else if (name === 'humidity') {
      // Only set main state for humidity sensors, not temp+humidity combos
      // The temp-humidity tile reads from id2 separately
    } else if (name === 'battery') {
      stateCache[id + '_battery'] = String(value);
    } else if (name === 'illuminance') {
      stateCache[id] = String(value);
    } else if (name === 'status') {
      const s = (value || '').toLowerCase();
      if (s === 'playing') stateCache[id] = 'playing';
      else if (s === 'paused') stateCache[id] = 'paused';
      else stateCache[id] = 'off';
    } else if (name === 'trackDescription') {
      stateCache[id + '_title'] = value || '';
    } else if (name === 'thermostatMode') {
      stateCache[id] = value;
    } else {
      return false; // Unknown attribute, no update
    }
    return true;
  },

  // --- EventSocket connection ---
  connectEventSocket() {
    if (!this.hubUrl) return;
    const wsUrl = this.hubUrl.replace(/^http/, 'ws') + '/eventsocket';
    this._ws = new WebSocket(wsUrl);

    this._ws.onopen = () => {
      stateCache._live = true;
      render();
    };

    this._ws.onmessage = (evt) => {
      if (!evt || !evt.data) return;
      let msg;
      try { msg = JSON.parse(evt.data); } catch (e) { return; }
      if (!msg) return;

      // Handle location-level events (mode changes, HSM)
      if (msg.source === 'LOCATION') {
        if (msg.name === 'mode') {
          stateCache.__mode__ = msg.value;
          // Update active flag in cached modes list
          if (stateCache.__modes__) {
            for (let i = 0; i < stateCache.__modes__.length; i++) {
              stateCache.__modes__[i].active = (stateCache.__modes__[i].name === msg.value);
            }
          }
          render();
        } else if (msg.name === 'hsmStatus' || msg.name === 'hsmAlert') {
          stateCache.__hsm__ = msg.value;
          render();
        }
        return;
      }

      if (msg.source !== 'DEVICE') return;

      // Handle brightness animation for dimmable lights
      const id = String(msg.deviceId);
      const type = this.deviceTypeMap[id];
      if (type === 'light' && msg.name === 'switch' && stateCache[id + '_dimmable'] && !isDimLocked(id)) {
        this._handleLightStateChange(id, msg);
        return;
      }

      if (this.normalizeEvent(msg)) render();
    };

    this._ws.onclose = () => {
      stateCache._live = false;
      render();
      clearTimeout(this._wsReconnectTimer);
      this._wsReconnectTimer = setTimeout(() => this.connectEventSocket(), this._wsReconnectMs);
    };

    this._ws.onerror = () => {
      if (this._ws) this._ws.close();
    };
  },

  // Handle light on/off transitions with brightness animation
  _handleLightStateChange(id, msg) {
    if (!id || !msg) return;
    const wasOn = stateCache[id] === 'on';
    const nowOn = msg.value === 'on';
    const oldBri = stateCache[id + '_brightness'] || (wasOn ? MAX_BRIGHTNESS : 0);

    if (wasOn && !nowOn) {
      // Turning off — animate to 0
      stateCache[id] = 'off';
      const transition = stateCache[id + '_transition'];
      const ms = (transition && transition > 0 ? transition : TileEngine.defaults.transitionSec) * 1000;
      animateBrightness(id, oldBri, 0, ms, 'off');
    } else if (!wasOn && nowOn) {
      // Turning on — animate from 0 to target
      stateCache[id] = 'on';
      stateCache[id + '_brightness'] = 0;
      render();
      const transition = stateCache[id + '_transition'];
      const ms = (transition && transition > 0 ? transition : TileEngine.defaults.transitionSec) * 1000;
      animateBrightness(id, 0, MAX_BRIGHTNESS, ms);
    } else {
      stateCache[id] = msg.value;
      render();
    }
  },

  // Register group member device IDs as media type
  _registerGroupMembers(entity) {
    if (!entity || !entity.groupMembers) return;
    for (let gi = 0; gi < entity.groupMembers.length; gi++) {
      const mid = entity.groupMembers[gi].id;
      if (mid) {
        this.deviceTypeMap[mid] = 'media';
        this._knownDeviceIds.add(String(mid));
      }
    }
  },

  // --- Build device type map from config data ---
  buildDeviceTypeMap(configData) {
    if (!configData) return;
    this.deviceTypeMap = {};
    this._knownDeviceIds = new Set();
    const floors = Object.values(configData);
    for (let fi = 0; fi < floors.length; fi++) {
      if (!floors[fi].rooms) continue;
      const rooms = Object.values(floors[fi].rooms);
      for (let ri = 0; ri < rooms.length; ri++) {
        const entities = rooms[ri].entities;
        if (!entities) continue;
        for (let ei = 0; ei < entities.length; ei++) {
          const e = entities[ei];
          if (e.id) {
            this.deviceTypeMap[e.id] = e.type;
            this._knownDeviceIds.add(String(e.id));
          }
          if (e.id2) this._knownDeviceIds.add(String(e.id2));
          if (e.idBat) this._knownDeviceIds.add(String(e.idBat));
          if (e.idStatus) this._knownDeviceIds.add(String(e.idStatus));
          this._registerGroupMembers(e);
        }
      }
    }
  },

  // --- Bulk state fetch from Maker API ---
  async fetchStates(configData) {
    if (!this.hubUrl || !this.appId || !this.token) return;
    const devices = await this.fetchAllDevices();
    if (!devices || !Array.isArray(devices)) {
      stateCache._live = false;
      return;
    }

    // Build a device lookup by ID for quick access
    const deviceById = {};
    for (let i = 0; i < devices.length; i++) {
      deviceById[String(devices[i].id)] = devices[i];
    }

    // Normalize each known device
    this._knownDeviceIds.forEach(id => {
      const device = deviceById[id];
      if (!device) return;
      const type = this.deviceTypeMap[id];

      if (type) {
        this.normalizeDevice(device);
      } else {
        // Secondary IDs (id2, idBat, idStatus) — store raw values
        this._normalizeSecondaryDevice(device, id);
      }
    });

    stateCache._live = true;
    render();
  },

  // Normalize secondary device IDs (temp/humidity sensors referenced via id2, idBat, etc.)
  _normalizeSecondaryDevice(device, id) {
    if (!device || !id) return;
    const temp = this.getAttrValue(device, 'temperature');
    if (temp !== undefined) { stateCache[id] = String(temp); return; }

    const humidity = this.getAttrValue(device, 'humidity');
    if (humidity !== undefined) { stateCache[id] = String(humidity); return; }

    const battery = this.getAttrValue(device, 'battery');
    if (battery !== undefined) { stateCache[id] = String(battery); return; }

    const power = this.getAttrValue(device, 'power');
    if (power !== undefined) { stateCache[id] = String(power); return; }

    const energy = this.getAttrValue(device, 'energy');
    if (energy !== undefined) { stateCache[id] = String(energy); return; }

    const voltage = this.getAttrValue(device, 'voltage');
    if (voltage !== undefined) { stateCache[id] = String(voltage); return; }

    // For volume on group members
    const volume = this.getAttrValue(device, 'volume');
    if (volume !== undefined) {
      stateCache[id + '_volume'] = parseFloat(volume) / 100;
    }

    // Generic: take the first non-null attribute value
    if (device.attributes && device.attributes.length > 0) {
      for (let i = 0; i < device.attributes.length; i++) {
        const attr = device.attributes[i];
        if (attr.currentValue !== null && attr.currentValue !== undefined) {
          stateCache[id] = String(attr.currentValue);
          return;
        }
      }
    }
  },

  // --- Auto-detect tile type from Hubitat device capabilities ---
  detectTileType(device) {
    if (!device || !device.capabilities) return 'switch';
    const caps = Array.isArray(device.capabilities)
      ? device.capabilities.map(c => typeof c === 'string' ? c : c.name || '')
      : [];

    for (let i = 0; i < this.CAPABILITY_PRIORITY.length; i++) {
      const cap = this.CAPABILITY_PRIORITY[i];
      if (caps.indexOf(cap) !== -1) {
        return this.CAPABILITY_TYPE_MAP[cap] || 'switch';
      }
    }
    // Fallback: if it has Switch capability at all, it's a switch
    if (caps.indexOf('Switch') !== -1) return 'switch';
    return 'sensor';
  },

  // --- Maker API: fetch all modes ---
  async fetchModes() {
    const url = this.apiUrl('modes');
    if (!url) return null;
    try {
      const resp = await fetch(url);
      if (!resp.ok) return null;
      return resp.json();
    } catch (e) { return null; }
  },

  // --- Maker API: set active mode ---
  async setMode(modeId) {
    if (!modeId) return;
    const url = this.apiUrl(`modes/${modeId}`);
    if (!url) return;
    try { await fetch(url); } catch (e) { /* network error */ }
  },

  // --- Maker API: fetch HSM status ---
  async fetchHSM() {
    const url = this.apiUrl('hsm');
    if (!url) return null;
    try {
      const resp = await fetch(url);
      if (!resp.ok) return null;
      return resp.json();
    } catch (e) { return null; }
  },

  // --- Maker API: set HSM status ---
  async setHSM(command) {
    if (!command) return;
    const url = this.apiUrl(`hsm/${command}`);
    if (!url) return;
    try { await fetch(url); } catch (e) { /* network error */ }
  },

  // --- Fetch hub info (direct endpoint, no auth) ---
  async fetchHubInfo() {
    if (!this.hubUrl) return null;
    try {
      const resp = await fetch(`${this.hubUrl}/hub/details/json`);
      if (!resp.ok) return null;
      return resp.json();
    } catch (e) { return null; }
  },

  // --- Fetch mode + HSM state and populate stateCache ---
  async fetchSystemState() {
    const modes = await this.fetchModes();
    if (modes && Array.isArray(modes)) {
      stateCache.__modes__ = modes;
      const active = modes.find(m => m.active);
      if (active) stateCache.__mode__ = active.name;
    }
    const hsm = await this.fetchHSM();
    if (hsm && hsm.hsm) {
      stateCache.__hsm__ = hsm.hsm;
    }
  },

  // --- Test connection ---
  async testConnection(hubUrl, appId, token) {
    if (!hubUrl || !appId || !token) return { ok: false, error: 'Missing connection parameters' };
    const url = `${hubUrl}/apps/api/${appId}/devices/all?access_token=${token}`;
    try {
      const resp = await fetch(url);
      if (!resp.ok) return { ok: false, error: `HTTP ${resp.status}` };
      const data = await resp.json();
      if (!Array.isArray(data)) return { ok: false, error: 'Invalid response' };
      return { ok: true, deviceCount: data.length, devices: data };
    } catch (e) {
      return { ok: false, error: e.message };
    }
  },

  // --- Initialize API layer ---
  init(hubUrl, appId, token, configData) {
    if (!hubUrl || !appId || !token) return;
    this.hubUrl = hubUrl;
    this.appId = appId;
    this.token = token;
    this.buildDeviceTypeMap(configData);
  }
};

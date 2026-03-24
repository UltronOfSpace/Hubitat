// config-ui.js — Settings panel for Hubitat dashboard configuration
// Provides hub connection setup, device discovery, floor/room/entity management

const ConfigUI = {
  _overlay: null,
  _discoveredDevices: null,

  // --- Open/Close ---
  open() {
    if (this._overlay) return;
    const config = loadConfig() || getDefaultConfig();
    this._overlay = this._buildOverlay(config);
    document.body.appendChild(this._overlay);
  },

  close() {
    if (!this._overlay) return;
    this._overlay.remove();
    this._overlay = null;
  },

  // --- Main overlay ---
  _buildOverlay(config) {
    const overlay = document.createElement('div');
    overlay.className = 'cfg-overlay';

    const panel = document.createElement('div');
    panel.className = 'cfg-panel';

    // Header
    const header = document.createElement('div');
    header.className = 'cfg-header';
    header.innerHTML = '<span class="cfg-title">Settings</span>';
    const closeBtn = document.createElement('button');
    closeBtn.className = 'cfg-close';
    closeBtn.textContent = '\u00d7';
    closeBtn.onclick = () => this.close();
    header.appendChild(closeBtn);
    panel.appendChild(header);

    // Tabs
    const tabs = document.createElement('div');
    tabs.className = 'cfg-tabs';
    const tabNames = ['Connection', 'Devices', 'Layout'];
    const tabPanels = [];
    for (let i = 0; i < tabNames.length; i++) {
      const tab = document.createElement('button');
      tab.className = 'cfg-tab' + (i === 0 ? ' active' : '');
      tab.textContent = tabNames[i];
      tab.onclick = () => this._switchTab(tabs, tabPanels, i);
      tabs.appendChild(tab);
    }
    panel.appendChild(tabs);

    // Tab panels
    const connPanel = this._buildConnectionPanel(config);
    const devicePanel = this._buildDevicePanel(config);
    const layoutPanel = this._buildLayoutPanel(config);
    tabPanels.push(connPanel, devicePanel, layoutPanel);
    devicePanel.style.display = 'none';
    layoutPanel.style.display = 'none';
    panel.appendChild(connPanel);
    panel.appendChild(devicePanel);
    panel.appendChild(layoutPanel);

    overlay.appendChild(panel);
    overlay.onclick = (e) => { if (e.target === overlay) this.close(); };
    return overlay;
  },

  _switchTab(tabs, tabPanels, index) {
    tabs.querySelectorAll('.cfg-tab').forEach(t => t.classList.remove('active'));
    tabs.children[index].classList.add('active');
    tabPanels.forEach((p, idx) => p.style.display = idx === index ? 'block' : 'none');
  },

  // --- Connection Tab ---
  _buildConnectionPanel(config) {
    const div = document.createElement('div');
    div.className = 'cfg-section';

    div.innerHTML = `
      <label class="cfg-label">Hub IP / URL</label>
      <input class="cfg-input" id="cfgHubUrl" type="text" placeholder="http://192.168.1.20" value="${config.hub.url || ''}">
      <label class="cfg-label">Maker API App ID</label>
      <input class="cfg-input" id="cfgAppId" type="text" placeholder="123" value="${config.hub.appId || ''}">
      <label class="cfg-label">Access Token</label>
      <input class="cfg-input" id="cfgToken" type="text" placeholder="xxxxxxxx-xxxx-xxxx-xxxx-xxxxxxxxxxxx" value="${config.hub.token || ''}">
      <div class="cfg-btn-row">
        <button class="cfg-btn" id="cfgTestBtn">Test Connection</button>
        <button class="cfg-btn cfg-btn-primary" id="cfgSaveConnBtn">Save</button>
      </div>
      <div class="cfg-status" id="cfgConnStatus"></div>
    `;

    // Defer to allow innerHTML to flush to DOM
    setTimeout(() => {
      const testBtn = document.getElementById('cfgTestBtn');
      const saveBtn = document.getElementById('cfgSaveConnBtn');
      const status = document.getElementById('cfgConnStatus');

      if (testBtn) testBtn.onclick = async () => {
        const url = document.getElementById('cfgHubUrl').value.trim().replace(/\/+$/, '');
        const appId = document.getElementById('cfgAppId').value.trim();
        const token = document.getElementById('cfgToken').value.trim();
        status.textContent = 'Testing...';
        status.className = 'cfg-status';
        const result = await HubitatAPI.testConnection(url, appId, token);
        if (result.ok) {
          status.textContent = `Connected! Found ${result.deviceCount} devices.`;
          status.className = 'cfg-status cfg-ok';
          this._discoveredDevices = result.devices;
        } else {
          status.textContent = `Failed: ${result.error}`;
          status.className = 'cfg-status cfg-err';
        }
      };

      if (saveBtn) saveBtn.onclick = () => {
        const url = document.getElementById('cfgHubUrl').value.trim().replace(/\/+$/, '');
        const appId = document.getElementById('cfgAppId').value.trim();
        const token = document.getElementById('cfgToken').value.trim();
        const cfg = loadConfig() || getDefaultConfig();
        cfg.hub = { url, appId, token };
        saveConfig(cfg);
        status.textContent = 'Saved! Reloading...';
        status.className = 'cfg-status cfg-ok';
        setTimeout(() => location.reload(), 500); // delay for status message visibility
      };
    }, 0);

    return div;
  },

  // --- Devices Tab ---
  _buildDevicePanel(config) {
    const div = document.createElement('div');
    div.className = 'cfg-section';

    div.innerHTML = `
      <div class="cfg-btn-row">
        <button class="cfg-btn" id="cfgDiscoverBtn">Discover Devices</button>
      </div>
      <input class="cfg-input cfg-search" id="cfgDeviceSearch" type="text" placeholder="Search devices...">
      <div class="cfg-device-list" id="cfgDeviceList"></div>
      <div class="cfg-status" id="cfgDeviceStatus"></div>
    `;

    setTimeout(() => {
      const discoverBtn = document.getElementById('cfgDiscoverBtn');
      const searchInput = document.getElementById('cfgDeviceSearch');

      if (discoverBtn) discoverBtn.onclick = async () => {
        const status = document.getElementById('cfgDeviceStatus');
        const cfg = loadConfig();
        if (!cfg || !cfg.hub || !cfg.hub.url) {
          if (status) status.textContent = 'Configure hub connection first.';
          return;
        }
        if (status) status.textContent = 'Discovering...';
        const result = await HubitatAPI.testConnection(cfg.hub.url, cfg.hub.appId, cfg.hub.token);
        if (result.ok) {
          this._discoveredDevices = result.devices;
          this._renderDeviceList(config);
          if (status) { status.textContent = `Found ${result.deviceCount} devices.`; status.className = 'cfg-status cfg-ok'; }
        } else {
          if (status) { status.textContent = `Failed: ${result.error}`; status.className = 'cfg-status cfg-err'; }
        }
      };

      if (searchInput) searchInput.oninput = () => this._renderDeviceList(config, searchInput.value);

      // If devices were already discovered (from connection test), show them
      if (this._discoveredDevices) this._renderDeviceList(config);
    }, 0);

    return div;
  },

  _renderDeviceList(config, filter) {
    const list = document.getElementById('cfgDeviceList');
    if (!list || !this._discoveredDevices) return;
    const search = (filter || '').toLowerCase();

    // Build set of already-configured device IDs
    const configured = new Set();
    if (config && config.floors) {
      Object.values(config.floors).forEach(floor => {
        if (!floor.rooms) return;
        Object.values(floor.rooms).forEach(room => {
          if (!room.entities) return;
          room.entities.forEach(e => { if (e.id) configured.add(String(e.id)); });
        });
      });
    }

    const devices = this._discoveredDevices
      .filter(d => !search || d.label.toLowerCase().includes(search) || d.name.toLowerCase().includes(search))
      .sort((a, b) => (a.label || a.name).localeCompare(b.label || b.name));

    list.innerHTML = '';
    const MAX_VISIBLE = 100;
    const shown = Math.min(devices.length, MAX_VISIBLE);
    for (let i = 0; i < shown; i++) {
      const d = devices[i];
      const row = document.createElement('div');
      row.className = 'cfg-device-row' + (configured.has(String(d.id)) ? ' cfg-configured' : '');
      const detectedType = HubitatAPI.detectTileType(d);
      row.innerHTML = `
        <span class="cfg-device-name">${d.label || d.name}</span>
        <span class="cfg-device-type">${detectedType}</span>
        <span class="cfg-device-id">#${d.id}</span>
        ${configured.has(String(d.id)) ? '<span class="cfg-device-added">Added</span>' : `<button class="cfg-btn cfg-btn-sm" data-did="${d.id}" data-dname="${d.label || d.name}" data-dtype="${detectedType}">Add</button>`}
      `;
      list.appendChild(row);
    }
    if (devices.length > MAX_VISIBLE) {
      const more = document.createElement('div');
      more.className = 'cfg-more';
      more.textContent = `...and ${devices.length - MAX_VISIBLE} more. Use search to filter.`;
      list.appendChild(more);
    }

    // Wire up Add buttons
    list.querySelectorAll('button[data-did]').forEach(btn => {
      btn.onclick = () => this._addDevicePrompt(btn.dataset.did, btn.dataset.dname, btn.dataset.dtype);
    });
  },

  _addDevicePrompt(deviceId, deviceName, deviceType) {
    const cfg = loadConfig() || getDefaultConfig();
    const floors = cfg.floors || {};
    const floorKeys = Object.keys(floors);

    // If no floors exist, create a default one
    if (floorKeys.length === 0) {
      floors['floor_1'] = { name: '1st Floor', rooms: { room_1: { name: 'Room 1', entities: [] } } };
      cfg.floors = floors;
    }

    // Simple: add to first room of first floor for now (layout tab manages organization)
    const firstFloor = Object.values(floors)[0];
    const firstRoom = Object.values(firstFloor.rooms)[0];
    const MAX_ENTITIES_PER_ROOM = 50;
    if (firstRoom.entities.length >= MAX_ENTITIES_PER_ROOM) return;
    firstRoom.entities.push({ id: String(deviceId), name: deviceName, type: deviceType });
    saveConfig(cfg);

    // Re-render device list to show "Added" state
    this._renderDeviceList(cfg);
    const status = document.getElementById('cfgDeviceStatus');
    if (status) { status.textContent = `Added "${deviceName}" to ${firstRoom.name}.`; status.className = 'cfg-status cfg-ok'; }
  },

  // --- Layout Tab ---
  _buildLayoutPanel(config) {
    const div = document.createElement('div');
    div.className = 'cfg-section';
    div.id = 'cfgLayoutSection';
    this._renderLayoutTree(div, config);
    return div;
  },

  _renderLayoutHTML(container, cfg) {
    if (!container) return;
    const floors = cfg.floors || {};

    container.innerHTML = `
      <div class="cfg-btn-row">
        <button class="cfg-btn" id="cfgAddFloor">+ Floor</button>
        <button class="cfg-btn cfg-btn-primary" id="cfgSaveLayout">Save &amp; Reload</button>
      </div>
      <div id="cfgLayoutTree"></div>
      <label class="cfg-label">Home Room</label>
      <select class="cfg-input" id="cfgHomeRoom"></select>
    `;

    const tree = container.querySelector('#cfgLayoutTree');
    const homeSelect = container.querySelector('#cfgHomeRoom');

    Object.entries(floors).forEach(([floorId, floor]) => {
      const floorDiv = document.createElement('div');
      floorDiv.className = 'cfg-floor';

      const floorHeader = document.createElement('div');
      floorHeader.className = 'cfg-floor-header';
      floorHeader.innerHTML = `
        <input class="cfg-inline-edit" value="${floor.name}" data-floor="${floorId}" data-field="floorName">
        <button class="cfg-btn cfg-btn-sm" data-action="addRoom" data-floor="${floorId}">+ Room</button>
        <button class="cfg-btn cfg-btn-sm cfg-btn-danger" data-action="deleteFloor" data-floor="${floorId}">\u00d7</button>
      `;
      floorDiv.appendChild(floorHeader);

      if (floor.rooms) {
        Object.entries(floor.rooms).forEach(([roomId, room]) => {
          const roomDiv = document.createElement('div');
          roomDiv.className = 'cfg-room';
          roomDiv.innerHTML = `
            <div class="cfg-room-header">
              <input class="cfg-inline-edit" value="${room.name}" data-floor="${floorId}" data-room="${roomId}" data-field="roomName">
              <button class="cfg-btn cfg-btn-sm cfg-btn-danger" data-action="deleteRoom" data-floor="${floorId}" data-room="${roomId}">\u00d7</button>
            </div>
          `;

          const entList = document.createElement('div');
          entList.className = 'cfg-entity-list';
          if (room.entities) {
            room.entities.forEach((ent, idx) => {
              const entDiv = document.createElement('div');
              entDiv.className = 'cfg-entity';
              entDiv.innerHTML = `
                <span class="cfg-entity-name">${ent.name}</span>
                <span class="cfg-entity-type">${ent.type}</span>
                <span class="cfg-entity-id">#${ent.id}</span>
                <button class="cfg-btn cfg-btn-sm cfg-btn-danger" data-action="deleteEntity" data-floor="${floorId}" data-room="${roomId}" data-idx="${idx}">\u00d7</button>
              `;
              entList.appendChild(entDiv);
            });
          }
          roomDiv.appendChild(entList);
          floorDiv.appendChild(roomDiv);

          const opt = document.createElement('option');
          opt.value = roomId;
          opt.textContent = `${floor.name} > ${room.name}`;
          if (cfg.homeRoom === roomId) opt.selected = true;
          homeSelect.appendChild(opt);
        });
      }
      tree.appendChild(floorDiv);
    });
  },

  _wireFloorActions(container) {
    if (!container) return;
    container.querySelector('#cfgAddFloor').onclick = () => {
      const c = loadConfig() || getDefaultConfig();
      const id = 'floor_' + Date.now();
      if (!c.floors) c.floors = {};
      c.floors[id] = { name: 'New Floor', rooms: {} };
      saveConfig(c);
      this._renderLayoutTree(container, c);
    };

    const homeSelect = container.querySelector('#cfgHomeRoom');
    container.querySelector('#cfgSaveLayout').onclick = () => {
      const c = loadConfig() || getDefaultConfig();
      container.querySelectorAll('[data-field="floorName"]').forEach(input => {
        const fid = input.dataset.floor;
        if (c.floors[fid]) c.floors[fid].name = input.value;
      });
      container.querySelectorAll('[data-field="roomName"]').forEach(input => {
        const fid = input.dataset.floor;
        const rid = input.dataset.room;
        if (c.floors[fid] && c.floors[fid].rooms[rid]) c.floors[fid].rooms[rid].name = input.value;
      });
      c.homeRoom = homeSelect.value;
      saveConfig(c);
      location.reload();
    };

    container.querySelectorAll('[data-action="deleteFloor"]').forEach(btn => {
      btn.onclick = () => {
        if (!confirm('Delete this floor and all its rooms?')) return;
        const c = loadConfig() || getDefaultConfig();
        delete c.floors[btn.dataset.floor];
        saveConfig(c);
        this._renderLayoutTree(container, c);
      };
    });
  },

  _wireRoomActions(container) {
    if (!container) return;
    container.querySelectorAll('[data-action="addRoom"]').forEach(btn => {
      btn.onclick = () => {
        const c = loadConfig() || getDefaultConfig();
        const fid = btn.dataset.floor;
        if (!c.floors[fid]) return;
        const rid = 'room_' + Date.now();
        if (!c.floors[fid].rooms) c.floors[fid].rooms = {};
        c.floors[fid].rooms[rid] = { name: 'New Room', entities: [] };
        saveConfig(c);
        this._renderLayoutTree(container, c);
      };
    });

    container.querySelectorAll('[data-action="deleteRoom"]').forEach(btn => {
      btn.onclick = () => {
        if (!confirm('Delete this room and all its devices?')) return;
        const c = loadConfig() || getDefaultConfig();
        const fid = btn.dataset.floor;
        if (c.floors[fid]) delete c.floors[fid].rooms[btn.dataset.room];
        saveConfig(c);
        this._renderLayoutTree(container, c);
      };
    });
  },

  _wireEntityActions(container) {
    if (!container) return;
    container.querySelectorAll('[data-action="deleteEntity"]').forEach(btn => {
      btn.onclick = () => {
        const c = loadConfig() || getDefaultConfig();
        const fid = btn.dataset.floor;
        const rid = btn.dataset.room;
        const idx = parseInt(btn.dataset.idx, 10);
        if (c.floors[fid] && c.floors[fid].rooms[rid]) {
          c.floors[fid].rooms[rid].entities.splice(idx, 1);
        }
        saveConfig(c);
        this._renderLayoutTree(container, c);
      };
    });
  },

  _renderLayoutTree(container, config) {
    if (!container) return;
    const cfg = config || loadConfig() || getDefaultConfig();
    this._renderLayoutHTML(container, cfg);
    // Defer to allow innerHTML to flush to DOM
    setTimeout(() => {
      this._wireFloorActions(container);
      this._wireRoomActions(container);
      this._wireEntityActions(container);
    }, 0);
  },

  // --- CSS ---
  css: `
.cfg-overlay {
  position: fixed; top: 0; left: 0; right: 0; bottom: 0;
  background: rgba(0,0,0,0.7); z-index: 2000;
  display: flex; align-items: center; justify-content: center;
  backdrop-filter: blur(8px); -webkit-backdrop-filter: blur(8px);
  animation: cfgFadeIn 0.2s ease;
}
@keyframes cfgFadeIn { from { opacity: 0; } to { opacity: 1; } }
.cfg-panel {
  background: #1c1c1e; border-radius: 20px; padding: 0;
  width: 90vw; max-width: 520px; max-height: 85vh;
  overflow-y: auto; box-shadow: 0 12px 48px rgba(0,0,0,0.6);
}
.cfg-header {
  display: flex; align-items: center; justify-content: space-between;
  padding: 16px 20px 8px; border-bottom: 1px solid rgba(255,255,255,0.08);
}
.cfg-title { font-size: 18px; font-weight: 700; color: #fff; }
.cfg-close {
  border: none; background: none; color: rgba(255,255,255,0.4);
  font-size: 24px; cursor: pointer; padding: 0 4px; line-height: 1;
}
.cfg-close:hover { color: #fff; }
.cfg-tabs {
  display: flex; gap: 0; border-bottom: 1px solid rgba(255,255,255,0.08);
  padding: 0 16px;
}
.cfg-tab {
  border: none; background: none; color: rgba(255,255,255,0.4);
  font-size: 13px; font-weight: 600; padding: 10px 14px; cursor: pointer;
  border-bottom: 2px solid transparent; font-family: inherit;
}
.cfg-tab.active { color: #0a84ff; border-bottom-color: #0a84ff; }
.cfg-section { padding: 16px 20px; }
.cfg-label {
  display: block; font-size: 12px; font-weight: 600; color: rgba(255,255,255,0.5);
  margin: 12px 0 4px; text-transform: uppercase; letter-spacing: 0.5px;
}
.cfg-input {
  width: 100%; box-sizing: border-box; padding: 10px 12px;
  background: #2c2c2e; border: 1px solid rgba(255,255,255,0.1); border-radius: 10px;
  color: #fff; font-size: 14px; font-family: inherit; outline: none;
}
.cfg-input:focus { border-color: #0a84ff; }
.cfg-search { margin-bottom: 12px; }
.cfg-btn-row { display: flex; gap: 8px; margin: 12px 0; }
.cfg-btn {
  border: none; cursor: pointer; border-radius: 10px;
  padding: 8px 16px; font-size: 13px; font-weight: 600; font-family: inherit;
  background: #2c2c2e; color: rgba(255,255,255,0.7);
  transition: background 0.15s;
}
.cfg-btn:hover { background: #3a3a3c; }
.cfg-btn-primary { background: #0a84ff; color: #fff; }
.cfg-btn-primary:hover { background: #0070e0; }
.cfg-btn-sm { padding: 4px 10px; font-size: 11px; border-radius: 6px; }
.cfg-btn-danger { background: rgba(255,59,48,0.2); color: #ff3b30; }
.cfg-btn-danger:hover { background: rgba(255,59,48,0.4); }
.cfg-status {
  font-size: 12px; color: rgba(255,255,255,0.4); margin-top: 8px;
  min-height: 18px;
}
.cfg-ok { color: #30d158; }
.cfg-err { color: #ff3b30; }
.cfg-device-list { max-height: 300px; overflow-y: auto; }
.cfg-device-row {
  display: flex; align-items: center; gap: 8px; padding: 6px 0;
  border-bottom: 1px solid rgba(255,255,255,0.05); font-size: 13px;
}
.cfg-device-name { flex: 1; color: #fff; min-width: 0; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
.cfg-device-type { color: rgba(255,255,255,0.3); font-size: 11px; min-width: 50px; }
.cfg-device-id { color: rgba(255,255,255,0.2); font-size: 11px; min-width: 40px; }
.cfg-device-added { color: #30d158; font-size: 11px; font-weight: 600; }
.cfg-configured { opacity: 0.5; }
.cfg-more { font-size: 11px; color: rgba(255,255,255,0.3); padding: 8px 0; text-align: center; }
.cfg-floor { margin-bottom: 16px; }
.cfg-floor-header {
  display: flex; align-items: center; gap: 8px; padding: 4px 0;
  border-bottom: 1px solid rgba(255,255,255,0.1);
}
.cfg-inline-edit {
  background: none; border: none; border-bottom: 1px solid transparent;
  color: #fff; font-size: 14px; font-weight: 600; padding: 2px 4px;
  font-family: inherit; outline: none; flex: 1;
}
.cfg-inline-edit:focus { border-bottom-color: #0a84ff; }
.cfg-room { margin-left: 16px; margin-top: 8px; }
.cfg-room-header {
  display: flex; align-items: center; gap: 8px;
  border-bottom: 1px solid rgba(255,255,255,0.05); padding: 2px 0;
}
.cfg-room-header .cfg-inline-edit { font-size: 13px; font-weight: 500; }
.cfg-entity-list { margin-left: 8px; }
.cfg-entity {
  display: flex; align-items: center; gap: 6px; padding: 3px 0; font-size: 12px;
}
.cfg-entity-name { flex: 1; color: rgba(255,255,255,0.7); }
.cfg-entity-type { color: rgba(255,255,255,0.3); font-size: 10px; }
.cfg-entity-id { color: rgba(255,255,255,0.2); font-size: 10px; }
`
};

// Inject CSS once
(function() {
  const style = document.createElement('style');
  style.textContent = ConfigUI.css;
  document.head.appendChild(style);
})();

// Global function for onclick
function openSettings() { ConfigUI.open(); }

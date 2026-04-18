TileEngine.register('mode', {
  icon: '<svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8" stroke-linecap="round" stroke-linejoin="round"><circle cx="12" cy="12" r="5"/><line x1="12" y1="1" x2="12" y2="3"/><line x1="12" y1="21" x2="12" y2="23"/><line x1="4.22" y1="4.22" x2="5.64" y2="5.64"/><line x1="18.36" y1="18.36" x2="19.78" y2="19.78"/><line x1="1" y1="12" x2="3" y2="12"/><line x1="21" y1="12" x2="23" y2="12"/><line x1="4.22" y1="19.78" x2="5.64" y2="18.36"/><line x1="18.36" y1="5.64" x2="19.78" y2="4.22"/></svg>',

  // FontAwesome icon name to simple SVG mapping for mode icons
  MODE_ICONS: {
    'fa-sun': '<svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8"><circle cx="12" cy="12" r="5"/><line x1="12" y1="1" x2="12" y2="3"/><line x1="12" y1="21" x2="12" y2="23"/><line x1="4.22" y1="4.22" x2="5.64" y2="5.64"/><line x1="18.36" y1="18.36" x2="19.78" y2="19.78"/><line x1="1" y1="12" x2="3" y2="12"/><line x1="21" y1="12" x2="23" y2="12"/><line x1="4.22" y1="19.78" x2="5.64" y2="18.36"/><line x1="18.36" y1="5.64" x2="19.78" y2="4.22"/></svg>',
    'fa-sunset': '<svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8" stroke-linecap="round" stroke-linejoin="round"><path d="M17 18a5 5 0 0 0-10 0"/><line x1="12" y1="9" x2="12" y2="2"/><line x1="4.22" y1="10.22" x2="5.64" y2="11.64"/><line x1="1" y1="18" x2="3" y2="18"/><line x1="21" y1="18" x2="23" y2="18"/><line x1="18.36" y1="11.64" x2="19.78" y2="10.22"/><line x1="23" y1="22" x2="1" y2="22"/><polyline points="16 5 12 9 8 5"/></svg>',
    'fa-moon': '<svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8" stroke-linecap="round" stroke-linejoin="round"><path d="M21 12.79A9 9 0 1 1 11.21 3 7 7 0 0 0 21 12.79z"/></svg>',
    'fa-plane-departure': '<svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8" stroke-linecap="round" stroke-linejoin="round"><path d="M17.8 19.2L16 11l3.5-3.5C21 6 21.5 4 21 3c-1-.5-3 0-4.5 1.5L13 8 4.8 6.2c-.5-.1-.9.1-1.1.5l-.3.5 5.1 3.5-3.3 3.3-2.1-.7-.6.5 2.5 2.5 2.5 2.5.5-.6-.7-2.1 3.3-3.3 3.5 5.1.5-.3c.4-.2.6-.6.5-1.1z"/></svg>',
    'fa-flask': '<svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8" stroke-linecap="round" stroke-linejoin="round"><path d="M9 3h6M10 3v7.4L4 20h16l-6-9.6V3"/></svg>',
    'fa-home': '<svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8" stroke-linecap="round" stroke-linejoin="round"><path d="M3 9l9-7 9 7v11a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2z"/><polyline points="9 22 9 12 15 12 15 22"/></svg>',
  },

  _popupOpen: false,

  formatState(entity) {
    if (!entity) return 'Unknown';
    const mode = stateCache.__mode__;
    if (!mode) return 'Loading...';
    return mode;
  },

  isOn() { return true; },
  isSensor: false,
  isAlert() { return false; },
  priority() { return 5; },

  _getModeIcon(iconName) {
    if (!iconName) return this.icon;
    return this.MODE_ICONS[iconName] || this.icon;
  },

  _getCurrentModeIcon() {
    const modes = stateCache.__modes__;
    const current = stateCache.__mode__;
    if (!modes || !current) return this.icon;
    for (let i = 0; i < modes.length; i++) {
      if (modes[i].name === current || modes[i].active) {
        return this._getModeIcon(modes[i].icon);
      }
    }
    return this.icon;
  },

  render(entity) {
    if (!entity) return '';
    if (!TileEngine) return '';
    const mode = stateCache.__mode__ || 'Unknown';
    const modeIcon = this._getCurrentModeIcon();

    return `<div class="tile state-on type-mode" onclick="toggleEntity('${entity.id}')">
      <div class="tile-icon-circle" style="color:#1c1c1e">${modeIcon}</div>
      <div class="tile-bottom">
        <div class="tile-name">${entity.name}</div>
        <div class="tile-state">${mode}</div>
      </div>
    </div>`;
  },

  toggle(entityId) {
    if (!entityId) return;
    const modes = stateCache.__modes__;
    if (!modes || modes.length === 0) return;

    // Build and show mode picker popup
    this._showModePicker(modes);
  },

  _showModePicker(modes) {
    if (this._popupOpen) return;
    this._popupOpen = true;
    const overlay = document.createElement('div');
    overlay.className = 'mode-picker-overlay';

    const popup = document.createElement('div');
    popup.className = 'mode-picker';

    const title = document.createElement('div');
    title.className = 'mode-picker-title';
    title.textContent = 'Select Mode';
    popup.appendChild(title);

    for (let i = 0; i < modes.length; i++) {
      const m = modes[i];
      const btn = document.createElement('button');
      btn.className = 'mode-picker-btn' + (m.active ? ' active' : '');
      const iconHtml = this._getModeIcon(m.icon);
      btn.innerHTML = `<span class="mode-picker-icon">${iconHtml}</span><span>${m.name}</span>`;
      btn.onclick = () => {
        // Optimistic update
        stateCache.__mode__ = m.name;
        for (let j = 0; j < modes.length; j++) {
          modes[j].active = (modes[j].id === m.id);
        }
        render();
        HubitatAPI.setMode(m.id);
        this._closePopup(overlay);
      };
      popup.appendChild(btn);
    }

    overlay.appendChild(popup);
    overlay.onclick = (e) => {
      if (e.target === overlay) this._closePopup(overlay);
    };
    document.body.appendChild(overlay);
  },

  _closePopup(overlay) {
    if (overlay) overlay.remove();
    this._popupOpen = false;
  },

  css: `.tile.type-mode .tile-icon-circle {
  background: linear-gradient(145deg, #ffb347 0%, #ff9500 50%, #e08600 100%);
  box-shadow: var(--neu-icon-glow) rgba(255,149,0,0.4), var(--neu-icon-glow-inset);
}
.mode-picker-overlay {
  position: fixed; top: 0; left: 0; right: 0; bottom: 0;
  background: rgba(0,0,0,0.7); z-index: 3000;
  display: flex; align-items: center; justify-content: center;
  backdrop-filter: blur(8px); -webkit-backdrop-filter: blur(8px);
  animation: cfgFadeIn 0.2s ease;
}
.mode-picker {
  background: #1c1c1e; border-radius: 20px; padding: 20px;
  min-width: 260px; max-width: 340px;
  box-shadow: 0 12px 48px rgba(0,0,0,0.6);
}
.mode-picker-title {
  font-size: 16px; font-weight: 700; color: #fff;
  margin-bottom: 16px; text-align: center;
}
.mode-picker-btn {
  display: flex; align-items: center; gap: 12px;
  width: 100%; padding: 12px 16px; margin-bottom: 6px;
  background: #2c2c2e; border: 2px solid transparent;
  border-radius: 12px; cursor: pointer;
  color: rgba(255,255,255,0.8); font-size: 15px; font-weight: 500;
  font-family: inherit; transition: all 0.15s;
}
.mode-picker-btn:last-child { margin-bottom: 0; }
.mode-picker-btn:hover { background: #3a3a3c; }
.mode-picker-btn.active {
  border-color: #ff9500; background: rgba(255,149,0,0.15);
  color: #ffb347;
}
.mode-picker-icon {
  width: 24px; height: 24px; display: flex;
  align-items: center; justify-content: center;
}
.mode-picker-icon svg { width: 20px; height: 20px; }`
});

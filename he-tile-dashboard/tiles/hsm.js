TileEngine.register('hsm', {
  icon: '<svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8" stroke-linecap="round" stroke-linejoin="round"><path d="M12 22s8-4 8-10V5l-8-3-8 3v7c0 6 8 10 8 10z"/></svg>',

  // HSM status labels
  HSM_LABELS: {
    'disarmed':   'Disarmed',
    'armedAway':  'Armed Away',
    'armedHome':  'Armed Home',
    'armedNight': 'Armed Night',
    'armingAway': 'Arming Away...',
    'armingHome': 'Arming Home...',
    'armingNight':'Arming Night...',
    'allDisarmed':'Disarmed',
    'intrusion':  'INTRUSION',
    'intrusion-home': 'INTRUSION',
    'intrusion-night': 'INTRUSION',
    'smoke':      'SMOKE',
    'water':      'WATER',
    'rule':       'RULE ALERT',
    'cancel':     'Cancelling...',
    'cancelRuleAlerts': 'Cancelling...',
  },

  // HSM commands for the picker
  HSM_COMMANDS: [
    { cmd: 'armAway',  label: 'Arm Away',   icon: 'away' },
    { cmd: 'armHome',  label: 'Arm Home',   icon: 'home' },
    { cmd: 'armNight', label: 'Arm Night',  icon: 'night' },
    { cmd: 'disarm',   label: 'Disarm',     icon: 'disarm' },
  ],

  _popupOpen: false,

  formatState(entity) {
    if (!entity) return 'Unknown';
    const hsm = stateCache.__hsm__;
    if (!hsm) return 'Loading...';
    return this.HSM_LABELS[hsm] || hsm;
  },

  isOn(entity) {
    if (!entity) return false;
    const hsm = stateCache.__hsm__;
    return hsm && hsm !== 'disarmed' && hsm !== 'allDisarmed';
  },

  isSensor: false,

  isAlert(entity) {
    if (!entity) return false;
    const hsm = stateCache.__hsm__;
    if (!hsm) return false;
    return hsm === 'intrusion' || hsm === 'intrusion-home' || hsm === 'intrusion-night' ||
           hsm === 'smoke' || hsm === 'water' || hsm === 'rule';
  },

  priority() { return 4; },

  render(entity) {
    if (!entity) return '';
    if (!TileEngine) return '';
    const hsm = stateCache.__hsm__;
    const isAlert = this.isAlert(entity);
    const isArmed = this.isOn(entity);
    let cls = 'type-hsm';
    if (isAlert) cls += ' state-alert hsm-alert';
    else if (isArmed) cls += ' state-on hsm-armed';
    else cls += ' hsm-disarmed';

    const ic = isArmed || isAlert ? '#1c1c1e' : 'rgba(255,255,255,0.6)';

    return `<div class="tile ${cls}" onclick="toggleEntity('${entity.id}')">
      <div class="tile-icon-circle" style="color:${ic}">${this.icon}</div>
      <div class="tile-bottom">
        <div class="tile-name">${entity.name}</div>
        <div class="tile-state">${this.formatState(entity)}</div>
      </div>
    </div>`;
  },

  toggle(entityId) {
    if (!entityId) return;
    this._showHSMPicker();
  },

  _showHSMPicker() {
    if (this._popupOpen) return;
    this._popupOpen = true;
    const overlay = document.createElement('div');
    overlay.className = 'mode-picker-overlay';

    const popup = document.createElement('div');
    popup.className = 'mode-picker';

    const title = document.createElement('div');
    title.className = 'mode-picker-title';
    title.textContent = 'Home Security';
    popup.appendChild(title);

    const hsm = stateCache.__hsm__ || '';
    const isAlert = hsm === 'intrusion' || hsm === 'intrusion-home' || hsm === 'intrusion-night' ||
                    hsm === 'smoke' || hsm === 'water' || hsm === 'rule';

    // If there's an active alert, show dismiss button
    if (isAlert) {
      const dismissBtn = document.createElement('button');
      dismissBtn.className = 'mode-picker-btn hsm-dismiss-btn';
      dismissBtn.innerHTML = '<span class="mode-picker-icon"><svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8" stroke-linecap="round" stroke-linejoin="round"><circle cx="12" cy="12" r="10"/><line x1="15" y1="9" x2="9" y2="15"/><line x1="9" y1="9" x2="15" y2="15"/></svg></span><span>Dismiss Alert</span>';
      dismissBtn.onclick = () => {
        HubitatAPI.setHSM('cancelAlerts');
        this._closePopup(overlay);
      };
      popup.appendChild(dismissBtn);
    }

    for (let i = 0; i < this.HSM_COMMANDS.length; i++) {
      const c = this.HSM_COMMANDS[i];
      const btn = document.createElement('button');
      const isActive = (hsm === c.cmd.replace('arm', 'armed').replace('Arm', 'Armed')) ||
                       (c.cmd === 'disarm' && (hsm === 'disarmed' || hsm === 'allDisarmed'));
      btn.className = 'mode-picker-btn' + (isActive ? ' active' : '');
      btn.innerHTML = `<span class="mode-picker-icon">${this.icon}</span><span>${c.label}</span>`;
      btn.onclick = () => {
        HubitatAPI.setHSM(c.cmd);
        // Optimistic update
        if (c.cmd === 'disarm') stateCache.__hsm__ = 'disarmed';
        else stateCache.__hsm__ = c.cmd.replace('arm', 'armed');
        render();
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

  css: `.tile.hsm-armed .tile-icon-circle {
  background: linear-gradient(145deg, #4cd964 0%, #30d158 50%, #28b84c 100%);
  box-shadow: var(--neu-icon-glow) rgba(48,209,88,0.4), var(--neu-icon-glow-inset);
}
.tile.hsm-disarmed .tile-icon-circle {
  background: rgba(255,255,255,0.08);
}
.tile.hsm-alert {
  animation: hsmPulse 1s ease-in-out infinite;
}
.tile.hsm-alert .tile-icon-circle {
  background: linear-gradient(145deg, #ff6961 0%, #ff3b30 50%, #d63028 100%);
  box-shadow: var(--neu-icon-glow) rgba(255,59,48,0.5), var(--neu-icon-glow-inset);
}
.tile.hsm-alert .tile-state { color: #ff3b30; font-weight: 700; }
@keyframes hsmPulse {
  0%, 100% { opacity: 1; }
  50% { opacity: 0.7; }
}
.hsm-dismiss-btn {
  border-color: #ff3b30 !important;
  background: rgba(255,59,48,0.15) !important;
  color: #ff6961 !important;
  margin-bottom: 12px !important;
}
.mode-picker-btn.active.hsm-armed-active {
  border-color: #30d158;
  background: rgba(48,209,88,0.15);
  color: #4cd964;
}`
});

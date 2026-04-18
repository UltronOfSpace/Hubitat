const FAN_LOW_MAX = 33;
const FAN_MED_MAX = 66;

function fanSpeedActive(entityId, pct) {
  const cur = TileEngine.attr(entityId, 'percentage') || 0;
  if (pct <= FAN_LOW_MAX) return cur > 0 && cur <= FAN_LOW_MAX;
  if (pct <= FAN_MED_MAX) return cur > FAN_LOW_MAX && cur <= FAN_MED_MAX;
  return cur > FAN_MED_MAX;
}

function updateFanTileInPlace(entityId, pct) {
  const tile = document.querySelector(`.tile[data-entity="${entityId}"]`);
  if (!tile) { render(); return; }
  const on = pct > 0;

  tile.classList.toggle('state-on', on);
  tile.classList.toggle('fan-low', on && pct <= FAN_LOW_MAX);
  tile.classList.toggle('fan-med', on && pct > FAN_LOW_MAX && pct <= FAN_MED_MAX);
  tile.classList.toggle('fan-high', on && pct > FAN_MED_MAX);

  const btns = tile.querySelectorAll('.fan-speed-btn');
  if (btns.length === 3) {
    btns[0].classList.toggle('active', on && pct > FAN_MED_MAX);
    btns[1].classList.toggle('active', on && pct > FAN_LOW_MAX && pct <= FAN_MED_MAX);
    btns[2].classList.toggle('active', on && pct <= FAN_LOW_MAX);
  }

  const circle = tile.querySelector('.tile-icon-circle');
  if (circle) circle.style.color = on ? '#1c1c1e' : 'rgba(255,255,255,0.6)';

  const name = tile.querySelector('.tile-name');
  if (name) name.style.color = on ? '#1c1c1e' : '';

  const state = tile.querySelector('.tile-state');
  if (state) {
    state.textContent = on ? 'On' : 'Off';
    state.style.color = on ? 'rgba(0,0,0,0.4)' : '';
  }
}

function setFanSpeed(entityId, percentage) {
  if (!entityId) return;
  if (percentage < 0 || percentage > 100) return;
  const isOn = TileEngine.state(entityId) === 'on';
  if (isOn && fanSpeedActive(entityId, percentage)) percentage = 0;
  TileEngine.setAttr(entityId, 'percentage', percentage);
  TileEngine.setState(entityId, percentage > 0 ? 'on' : 'off');
  TileEngine.lock(entityId);
  updateFanTileInPlace(entityId, percentage);
  if (percentage === 0) {
    TileEngine.callService(entityId, 'off');
  } else {
    TileEngine.callService(entityId, 'setSpeed', HubitatAPI.percentToSpeed(percentage));
  }
}

TileEngine.register('fan', {
  icon: `<svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8" stroke-linecap="round" stroke-linejoin="round"><path d="M12 12c-1.7-2.4-2-5.8-.3-8.3C13.5 1 16.5.7 18.8 2.4c1.2.9 1.8 2.2 1.2 3.6-.8 1.8-3.2 2.5-5 2"/><path d="M12 12c2.4-1.7 5.8-2 8.3-.3C23 13.5 23.3 16.5 21.6 18.8c-.9 1.2-2.2 1.8-3.6 1.2-1.8-.8-2.5-3.2-2-5"/><path d="M12 12c1.7 2.4 2 5.8.3 8.3-1.8 2.7-4.8 3-7.1 1.3-1.2-.9-1.8-2.2-1.2-3.6.8-1.8 3.2-2.5 5-2"/><path d="M12 12c-2.4 1.7-5.8 2-8.3.3C1 10.5.7 7.5 2.4 5.2c.9-1.2 2.2-1.8 3.6-1.2 1.8.8 2.5 3.2 2 5"/><circle cx="12" cy="12" r="1.5" fill="currentColor"/></svg>`,

  formatState(entity) {
    if (!entity || !entity.id) return 'Unknown';
    const s = TileEngine.state(entity.id);
    if (s === 'unavailable' || s === 'unknown') return 'No Response';
    if (s === 'on') return 'On';
    if (s === 'off') return 'Off';
    return s.charAt(0).toUpperCase() + s.slice(1);
  },

  isOn(entity) {
    if (!entity || !entity.id) return false;
    return TileEngine.state(entity.id) === 'on';
  },

  isSensor: false,

  isAlert() { return false; },

  priority(entity) {
    if (!entity) return 200;
    return entity.name.toLowerCase().includes('ceiling') ? 20 : 40;
  },

  render(entity) {
    if (!entity || !entity.id) return '';
    if (!TileEngine) return '';
    const on = this.isOn(entity);
    const state = this.formatState(entity);
    const cls = TileEngine.baseClass(entity);
    const offline = TileEngine.offlineHtml(entity);
    const ic = TileEngine.iconColor(entity);
    const icon = this.icon;

    const fanPct = TileEngine.attr(entity.id, 'percentage') || 0;
    let speedClass = '';
    if (on && fanPct <= FAN_LOW_MAX) speedClass = ' fan-low';
    else if (on && fanPct > FAN_LOW_MAX && fanPct <= FAN_MED_MAX) speedClass = ' fan-med';
    else if (on && fanPct > FAN_MED_MAX) speedClass = ' fan-high';
    const lowActive = on && fanPct <= FAN_LOW_MAX ? ' active' : '';
    const medActive = on && fanPct > FAN_LOW_MAX && fanPct <= FAN_MED_MAX ? ' active' : '';
    const highActive = on && fanPct > FAN_MED_MAX ? ' active' : '';

    return `<div class="tile ${cls}${speedClass} has-fan-speeds" data-entity="${entity.id}" onclick="if(!event.target.closest('.tile-fan-speeds'))toggleEntity('${entity.id}')">
      ${offline}
      <div class="tile-fan-speeds" onclick="event.stopPropagation()">
        <button class="fan-speed-btn${highActive}" onclick="setFanSpeed('${entity.id}',100)">H</button>
        <button class="fan-speed-btn${medActive}" onclick="setFanSpeed('${entity.id}',${FAN_MED_MAX})">M</button>
        <button class="fan-speed-btn${lowActive}" onclick="setFanSpeed('${entity.id}',${FAN_LOW_MAX})">L</button>
      </div>
      <div class="tile-icon-circle" style="color:${ic}">${icon}</div>
      <div class="tile-bottom">
        <div class="tile-name">${entity.name}</div>
        <div class="tile-state">${state}</div>
      </div>
    </div>`;
  },

  async toggle(entityId) {
    if (!entityId) return;
    if (!TileEngine || !TileEngine.callService) return;
    const wasOn = TileEngine.state(entityId) === 'on';
    if (wasOn) {
      TileEngine.setState(entityId, 'off');
      TileEngine.setAttr(entityId, 'percentage', 0);
      TileEngine.lock(entityId);
      updateFanTileInPlace(entityId, 0);
      TileEngine.callService(entityId, 'off');
    } else {
      TileEngine.setState(entityId, 'on');
      render();
      await TileEngine.callService(entityId, 'on');
      try {
        const device = await TileEngine.getDeviceState(entityId);
        if (device) {
          const speed = HubitatAPI.getAttrValue(device, 'speed');
          const pct = speed ? HubitatAPI.speedToPercent(speed) : 50;
          TileEngine.setAttr(entityId, 'percentage', pct);
          TileEngine.setState(entityId, 'on');
          TileEngine.lock(entityId);
          updateFanTileInPlace(entityId, pct);
        }
      } catch(e) { /* state recovery via EventSocket */ }
    }
  },

  css: `
.tile.state-on.type-fan .tile-icon-circle {
  background: linear-gradient(145deg, #6ee7b7 0%, #34d399 50%, #2ab887 100%);
  box-shadow: var(--neu-icon-glow) rgba(52,211,153,0.4), var(--neu-icon-glow-inset);
}
.tile.type-fan .tile-icon-circle svg { transform-origin: center; }
.tile-fan-speeds {
  position: absolute; right: 8%; top: 10%; bottom: 10%; left: 55%;
  display: flex; flex-direction: column; align-items: center; justify-content: space-between;
  z-index: 2;
}
.fan-speed-btn {
  border: none; cursor: pointer; border-radius: 8px;
  height: 28%; aspect-ratio: 1;
  display: flex; align-items: center; justify-content: center;
  font-size: clamp(10px, 1.2vw, 15px); font-weight: 700;
  font-family: inherit; text-transform: uppercase; letter-spacing: 0;
  background-color: #252527;
  color: rgba(255,255,255,0.4);
  box-shadow: var(--neu-raised-sm);
  transition: background-color 1s ease, color 1s ease, box-shadow 1s ease, transform 0.1s;
}
.fan-speed-btn:active { transform: scale(0.95); }
.fan-speed-btn.active {
  background-color: #34d399;
  color: #0a2e1f; box-shadow: var(--neu-raised-sm), 0 0 8px rgba(52,211,153,0.3);
}
.tile.state-on .fan-speed-btn {
  background-color: #dcdcdc;
  color: rgba(0,0,0,0.3);
  box-shadow: var(--neu-raised-sm-on);
}
.tile.state-on .fan-speed-btn.active {
  background-color: #34d399;
  color: #0a2e1f;
}
.tile.has-fan-speeds { position: relative; }
.tile.has-fan-speeds .tile-bottom { max-width: calc(100% - clamp(30px, 4vw, 50px)); }
`
});

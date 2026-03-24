function setFanSpeed(entityId, percentage) {
  stateCache[entityId + '_percentage'] = percentage;
  stateCache[entityId] = percentage > 0 ? 'on' : 'off';
  dimLocks[entityId] = Date.now();
  render();
  if (percentage === 0) {
    TileEngine.callService('fan', 'turn_off', { entity_id: entityId });
  } else {
    TileEngine.callService('fan', 'set_percentage', { entity_id: entityId, percentage: percentage });
  }
}

TileEngine.register('fan', {
  icon: `<svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8" stroke-linecap="round" stroke-linejoin="round"><path d="M12 12c-1.7-2.4-2-5.8-.3-8.3C13.5 1 16.5.7 18.8 2.4c1.2.9 1.8 2.2 1.2 3.6-.8 1.8-3.2 2.5-5 2"/><path d="M12 12c2.4-1.7 5.8-2 8.3-.3C23 13.5 23.3 16.5 21.6 18.8c-.9 1.2-2.2 1.8-3.6 1.2-1.8-.8-2.5-3.2-2-5"/><path d="M12 12c1.7 2.4 2 5.8.3 8.3-1.8 2.7-4.8 3-7.1 1.3-1.2-.9-1.8-2.2-1.2-3.6.8-1.8 3.2-2.5 5-2"/><path d="M12 12c-2.4 1.7-5.8 2-8.3.3C1 10.5.7 7.5 2.4 5.2c.9-1.2 2.2-1.8 3.6-1.2 1.8.8 2.5 3.2 2 5"/><circle cx="12" cy="12" r="1.5" fill="currentColor"/></svg>`,

  formatState(entity) {
    const s = TileEngine.state(entity.id);
    if (s === 'unavailable') return 'No Response';
    if (s === 'on') return 'On';
    if (s === 'off') return 'Off';
    return s.charAt(0).toUpperCase() + s.slice(1);
  },

  isOn(entity) {
    return TileEngine.state(entity.id) === 'on';
  },

  isSensor: false,

  isAlert() { return false; },

  priority(entity) {
    return entity.name.toLowerCase().includes('ceiling') ? 20 : 40;
  },

  render(entity) {
    const T = TileEngine;
    const on = this.isOn(entity);
    const state = this.formatState(entity);
    const cls = T.baseClass(entity);
    const offline = T.offlineHtml(entity);
    const ic = T.iconColor(entity);
    const icon = this.icon;

    const fanPct = stateCache[entity.id + '_percentage'] || 0;
    let speedClass = '';
    if (on && fanPct > 0 && fanPct <= 33) speedClass = ' fan-low';
    else if (on && fanPct > 33 && fanPct <= 66) speedClass = ' fan-med';
    else if (on && fanPct > 66) speedClass = ' fan-high';
    const lowActive = on && fanPct > 0 && fanPct <= 33 ? ' active' : '';
    const medActive = on && fanPct > 33 && fanPct <= 66 ? ' active' : '';
    const highActive = on && fanPct > 66 ? ' active' : '';

    return `<div class="tile ${cls}${speedClass} has-fan-speeds" data-entity="${entity.id}" onclick="if(!event.target.closest('.tile-fan-speeds'))toggleEntity('${entity.id}')">
      ${offline}
      <div class="tile-fan-speeds" onclick="event.stopPropagation()">
        <button class="fan-speed-btn${highActive}" onclick="setFanSpeed('${entity.id}',100)">H</button>
        <button class="fan-speed-btn${medActive}" onclick="setFanSpeed('${entity.id}',66)">M</button>
        <button class="fan-speed-btn${lowActive}" onclick="setFanSpeed('${entity.id}',33)">L</button>
      </div>
      <div class="tile-icon-circle" style="color:${ic}">${icon}</div>
      <div class="tile-bottom">
        <div class="tile-name">${entity.name}</div>
        <div class="tile-state">${state}</div>
      </div>
    </div>`;
  },

  async toggle(entityId) {
    const wasOn = TileEngine.state(entityId) === 'on';
    if (wasOn) {
      TileEngine.setState(entityId, 'off');
      TileEngine.setAttr(entityId, 'percentage', 0);
    } else {
      TileEngine.setState(entityId, 'on');
    }
    TileEngine.lock(entityId);
    render();

    const service = wasOn ? 'turn_off' : 'turn_on';
    await TileEngine.callService('fan', service, { entity_id: entityId });

    if (!wasOn) {
      try {
        const s = await TileEngine.getEntityState(entityId);
        if (s.attributes && s.attributes.percentage !== undefined) {
          TileEngine.setAttr(entityId, 'percentage', s.attributes.percentage);
          TileEngine.setState(entityId, s.state);
          TileEngine.lock(entityId);
          render();
        }
      } catch(e) {}
    }
  },

  css: `
.tile.state-on.type-fan .tile-icon-circle {
  background: linear-gradient(145deg, #6ee7b7 0%, #34d399 50%, #2ab887 100%);
  box-shadow: 0 2px 6px rgba(52,211,153,0.4), inset 0 1px 0 rgba(255,255,255,0.3);
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
  background: linear-gradient(145deg, #2e2e30 0%, #1c1c1e 100%);
  color: rgba(255,255,255,0.4);
  box-shadow: 2px 2px 4px rgba(0,0,0,0.4), -1px -1px 2px rgba(50,50,52,0.2),
    inset 0 1px 0 rgba(255,255,255,0.06);
  transition: all 0.15s;
}
.fan-speed-btn:active { transform: scale(0.95); }
.fan-speed-btn.active {
  background: linear-gradient(145deg, #6ee7b7 0%, #34d399 50%, #2ab887 100%);
  color: #0a2e1f; box-shadow: 2px 2px 4px rgba(0,0,0,0.3), 0 0 8px rgba(52,211,153,0.3),
    inset 0 1px 0 rgba(255,255,255,0.3);
}
.tile.state-on .fan-speed-btn {
  background: linear-gradient(145deg, #e8e8e8 0%, #d0d0d0 100%);
  color: rgba(0,0,0,0.3);
  box-shadow: 2px 2px 4px rgba(0,0,0,0.15), inset 0 1px 0 rgba(255,255,255,0.5);
}
.tile.state-on .fan-speed-btn.active {
  background: linear-gradient(145deg, #6ee7b7 0%, #34d399 50%, #2ab887 100%);
  color: #0a2e1f;
}
.tile.has-fan-speeds { position: relative; }
.tile.has-fan-speeds .tile-bottom { max-width: calc(100% - clamp(30px, 4vw, 50px)); }
`
});

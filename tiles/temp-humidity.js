TileEngine.register('temp_humidity', {
  isSensor: true,
  isOn(entity) {
    if (!entity || !entity.id) return false;
    return false;
  },
  isAlert(entity) {
    if (!entity || !entity.id) return false;
    return false;
  },
  priority() { return 10; },
  formatState(entity) {
    if (!entity || !entity.id) return 'Unknown';
    return '';
  },

  render(entity) {
    if (!entity || !entity.id) return '';
    if (!TileEngine) return '';
    const T = TileEngine;
    const cls = T.baseClass(entity);
    const offline = T.offlineHtml(entity);
    const ic = T.iconColor(entity);

    const tempRaw = stateCache[entity.id] || '—';
    const humRaw = stateCache[entity.id2] || '—';
    const temp = parseFloat(tempRaw) ? Math.round(parseFloat(tempRaw)) + '°' : tempRaw;
    const hum = parseFloat(humRaw) ? Math.round(parseFloat(humRaw)) + '%' : humRaw;

    const tempSvg = `<svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8" stroke-linecap="round" stroke-linejoin="round"><path d="M14 14.76V3.5a2.5 2.5 0 0 0-5 0v11.26a4.5 4.5 0 1 0 5 0z"/></svg>`;
    const humSvg = `<svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8" stroke-linecap="round" stroke-linejoin="round"><path d="M12 2c-2.5 4-5 7-5 10a5 5 0 0 0 10 0c0-3-2.5-6-5-10z"/></svg>`;

    // Battery warning (show only if <= 20%)
    const batRaw = entity.idBat ? (stateCache[entity.idBat] || '') : '';
    const batLevel = parseFloat(batRaw);
    const LOW_THRESHOLD = 20;
    const CRIT_THRESHOLD = 10;
    const showBat = !isNaN(batLevel) && batLevel <= LOW_THRESHOLD;
    const batPct = showBat ? Math.round(batLevel) : 0;
    const batCritical = batPct <= CRIT_THRESHOLD;
    const batSvg = `<svg viewBox="0 0 24 24" fill="none" stroke="#ff3b30" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><rect x="1" y="6" width="18" height="12" rx="2"/><line x1="23" y1="10" x2="23" y2="14"/><line x1="6" y1="10" x2="6" y2="14" stroke="#ff3b30" stroke-width="2.5"/></svg>`;
    const batHtml = showBat ? `<div class="tile-battery-warn${batCritical ? ' critical' : ''}">${batSvg}<span class="bat-pct">${batPct}%</span></div>` : '';

    return `<div class="tile ${cls} tile-dual">
      ${offline}${batHtml}
      <div class="tile-dual-value">
        <div class="tile-val-group">
          <div class="tile-icon-circle tile-icon-temp" style="color:${ic}">${tempSvg}</div>
          <div class="tile-val-num">${temp}</div>
          <div class="tile-val-label">Temp</div>
        </div>
        <div class="tile-val-group">
          <div class="tile-icon-circle tile-icon-hum" style="color:${ic}">${humSvg}</div>
          <div class="tile-val-num">${hum}</div>
          <div class="tile-val-label">Humidity</div>
        </div>
      </div>
    </div>`;
  },

  css: `
.tile-dual-value { display: flex; width: 100%; height: 100%; }
.tile-dual-value .tile-val-group {
  display: flex; flex-direction: column; align-items: center; flex: 1;
  justify-content: flex-start; gap: clamp(4px, 1vh, 10px);
}
.tile-dual-value .tile-val-group .tile-icon-circle { margin-top: 0; }
.tile-dual-value .tile-val-group .tile-val-num { margin-top: auto; }
.tile-dual-value .tile-val-num {
  font-size: clamp(10px, 2vw, 32px); font-weight: 700; color: #fff;
  letter-spacing: -0.5px; line-height: 1.1;
}
.tile-dual-value .tile-val-label {
  font-size: clamp(6px, 0.7vw, 11px); font-weight: 500; color: rgba(255,255,255,0.35);
  text-transform: uppercase; letter-spacing: 0.5px;
}
.tile-dual { padding: 8%; overflow: hidden !important; }
.tile-dual .tile-dual-top { display: none; }
.tile-dual .tile-val-group .tile-icon-circle {
  margin-bottom: 0;
  width: clamp(var(--icon-min), 44%, var(--icon-max)) !important;
}
.tile-dual .tile-icon-temp {
  background: linear-gradient(145deg, rgba(255,80,60,0.25) 0%, rgba(255,80,60,0.08) 100%) !important;
  box-shadow: 3px 3px 6px rgba(0,0,0,0.5), -2px -2px 4px rgba(255,100,80,0.1), inset 0 1px 1px rgba(255,150,130,0.2) !important;
  color: rgba(255,130,110,0.8) !important;
}
.tile-dual .tile-icon-hum {
  background: linear-gradient(145deg, rgba(10,132,255,0.25) 0%, rgba(10,132,255,0.08) 100%) !important;
  box-shadow: 3px 3px 6px rgba(0,0,0,0.5), -2px -2px 4px rgba(60,140,255,0.1), inset 0 1px 1px rgba(100,180,255,0.2) !important;
  color: rgba(100,180,255,0.8) !important;
}
.tile-battery-warn {
  position: absolute; top: 50%; left: 50%; transform: translate(-50%, -50%);
  display: flex; flex-direction: column; align-items: center; gap: 2px; z-index: 2;
}
.tile-battery-warn svg {
  width: clamp(18px, 2.2vw, 28px); height: clamp(18px, 2.2vw, 28px);
  filter: drop-shadow(0 0 6px rgba(255,59,48,0.6));
}
.tile-battery-warn .bat-pct {
  font-size: clamp(8px, 0.7vw, 11px); font-weight: 700;
  color: #ff3b30; letter-spacing: 0.3px;
  text-shadow: 0 0 6px rgba(255,59,48,0.4);
}
@keyframes bat-pulse {
  0%, 100% { opacity: 1; }
  50% { opacity: 0.5; }
}
.tile-battery-warn.critical { animation: bat-pulse 2s ease-in-out infinite; }
`
});

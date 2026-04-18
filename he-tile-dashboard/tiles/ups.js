TileEngine.register('ups', {
  isSensor: true,
  isOn(entity) {
    if (!entity || !entity.id) return false;
    return false;
  },
  isAlert(entity) {
    if (!entity || !entity.id) return false;
    return false;
  },
  priority() { return 90; },
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

    const loadRaw = stateCache[entity.id] || '—';
    const batRaw = stateCache[entity.id2] || '—';
    const statusRaw = entity.idStatus ? (stateCache[entity.idStatus] || '—') : '—';
    const load = parseFloat(loadRaw) ? Math.round(parseFloat(loadRaw)) + '%' : loadRaw;
    const bat = parseFloat(batRaw) ? Math.round(parseFloat(batRaw)) + '%' : batRaw;
    const loadPct = parseFloat(loadRaw) || 0;
    const batPct = parseFloat(batRaw) || 0;

    // Load gauge SVG
    const loadFillColor = loadPct > 80 ? '#ff3b30' : loadPct > 50 ? '#ff9500' : '#30d158';
    const loadAngle = (loadPct / 100) * 180;
    const startAngle = Math.PI;
    const endAngle = startAngle + (loadAngle * Math.PI / 180);
    const endX = 12 + 8 * Math.cos(endAngle);
    const endY = 16 + 8 * Math.sin(endAngle);
    const loadSvg = `<svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8" stroke-linecap="round" stroke-linejoin="round"><path d="M4 16a8 8 0 0 1 16 0" stroke="rgba(255,255,255,0.2)" stroke-width="3" fill="none"/><path d="M4 16a8 8 0 0 1 16 0" stroke="${loadFillColor}" stroke-width="3" fill="none" stroke-dasharray="${loadPct / 100 * 25.13} 25.13"/><circle cx="12" cy="16" r="2" fill="currentColor" stroke="none"/><line x1="12" y1="16" x2="${endX.toFixed(1)}" y2="${endY.toFixed(1)}" stroke="currentColor" stroke-width="1.5"/></svg>`;

    // Battery SVG
    const batFillColor = batPct < 20 ? '#ff3b30' : batPct < 50 ? '#ff9500' : '#30d158';
    const batFillW = Math.max(1, Math.round(batPct / 100 * 14));
    const batSvg = `<svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8" stroke-linecap="round" stroke-linejoin="round"><rect x="1" y="6" width="18" height="12" rx="2" ry="2"/><line x1="23" y1="10" x2="23" y2="14"/><rect x="3" y="8" width="${batFillW}" height="8" rx="1" fill="${batFillColor}" stroke="none"/></svg>`;

    return `<div class="tile ${cls} tile-dual">
      ${offline}
      <div class="tile-dual-value">
        <div class="tile-val-group">
          <div class="tile-icon-circle tile-icon-temp" style="color:${ic}">${loadSvg}</div>
          <div class="tile-val-num">${load}</div>
          <div class="tile-val-label">Load</div>
        </div>
        <div class="tile-val-group">
          <div class="tile-icon-circle tile-icon-hum" style="color:${ic}">${batSvg}</div>
          <div class="tile-val-num">${bat}</div>
          <div class="tile-val-label">Battery</div>
        </div>
      </div>
      <div class="tile-bottom"><div class="tile-name">${entity.name}</div><div class="tile-state">${statusRaw}</div></div>
    </div>`;
  },

  css: ``
});

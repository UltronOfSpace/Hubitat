TileEngine.register('climate', {
  icon: '<svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8" stroke-linecap="round" stroke-linejoin="round"><path d="M14 14.76V3.5a2.5 2.5 0 0 0-5 0v11.26a4.5 4.5 0 1 0 5 0z"/></svg>',

  formatState(entity) {
    if (!entity || !entity.id) return 'Unknown';
    const s = stateCache[entity.id] || 'unknown';
    if (s === 'unavailable') return 'No Response';
    return s.replace('_', '/').replace(/\b\w/g, c => c.toUpperCase());
  },

  isOn(entity) {
    if (!entity || !entity.id) return false;
    return false;
  },
  isSensor: true,
  isAlert(entity) {
    if (!entity || !entity.id) return false;
    return false;
  },
  priority() { return 11; },

  render(entity) {
    if (!entity || !entity.id) return '';
    if (!TileEngine) return '';
    const T = TileEngine;
    const cls = T.baseClass(entity);
    const offline = T.offlineHtml(entity);
    const ic = T.iconColor(entity);
    const state = this.formatState(entity);

    return `<div class="tile ${cls}">
      ${offline}
      <div class="tile-icon-circle" style="color:${ic}">${this.icon}</div>
      <div class="tile-bottom">
        <div class="tile-value">${state}</div>
        <div class="tile-name">${entity.name}</div>
      </div>
    </div>`;
  },

  css: `.tile.state-on.type-climate .tile-icon-circle {
  background: linear-gradient(145deg, #ff8a80 0%, #ff6961 50%, #e05a53 100%);
  box-shadow: 0 2px 6px rgba(255,105,97,0.4), inset 0 1px 0 rgba(255,255,255,0.3);
}`
});

TileEngine.register('camera', {
  icon: '<svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8" stroke-linecap="round" stroke-linejoin="round"><path d="M23 19a2 2 0 0 1-2 2H3a2 2 0 0 1-2-2V8a2 2 0 0 1 2-2h4l2-3h6l2 3h4a2 2 0 0 1 2 2z"/><circle cx="12" cy="13" r="4"/></svg>',

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

  isAlert(entity) {
    if (!entity || !entity.id) return false;
    return false;
  },

  priority() {
    return 80;
  },

  render(entity) {
    if (!entity || !entity.id) return '';
    if (!TileEngine) return '';
    return TileEngine.renderStandard(entity, {
      icon: this.icon,
      iconColor: TileEngine.iconColor(entity),
      state: this.formatState(entity)
    });
  },

  css: `.tile.state-on.type-camera .tile-icon-circle {
  background: linear-gradient(145deg, #4de670 0%, #30d158 50%, #28b84c 100%);
  box-shadow: 0 2px 6px rgba(48,209,88,0.4), inset 0 1px 0 rgba(255,255,255,0.3);
}`
});

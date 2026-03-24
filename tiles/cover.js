TileEngine.register('cover', {
  icon: '<svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8" stroke-linecap="round" stroke-linejoin="round"><rect x="3" y="3" width="18" height="18" rx="2"/><line x1="3" y1="9" x2="21" y2="9"/><line x1="3" y1="15" x2="21" y2="15"/></svg>',

  formatState(entity) {
    if (!entity || !entity.id) return 'Unknown';
    const s = TileEngine.state(entity.id);
    if (s === 'unavailable' || s === 'unknown') return 'No Response';
    return s === 'closed' ? 'Closed' : 'Open';
  },

  isOn(entity) {
    if (!entity || !entity.id) return false;
    return TileEngine.state(entity.id) === 'open';
  },

  isSensor: false,

  isAlert(entity) {
    if (!entity || !entity.id) return false;
    return false;
  },

  priority() {
    return 70;
  },

  render(entity) {
    if (!entity || !entity.id) return '';
    if (!TileEngine) return '';
    return TileEngine.renderStandard(entity, {
      icon: this.icon,
      iconColor: TileEngine.iconColor(entity),
      state: this.formatState(entity),
      extraCls: '',
      dimStyle: '',
      onclick: ` onclick="toggleEntity('${entity.id}')"`,
      extraAttrs: '',
      sliderHtml: '',
      artHtml: '',
      nameStyle: '',
      stateStyle: '',
      mediaInfoHtml: '',
      transportHtml: '',
      progressHtml: ''
    });
  },

  toggle(entityId) {
    if (!entityId) return;
    if (!TileEngine || !TileEngine.callService) return;
    const wasOn = TileEngine.state(entityId) === 'open';
    TileEngine.setState(entityId, wasOn ? 'closed' : 'open');
    TileEngine.lock(entityId);
    render();
    TileEngine.callService(entityId, wasOn ? 'close' : 'open');
  },

  css: `.tile.state-on.type-cover .tile-icon-circle {
  background: linear-gradient(145deg, #3d9eff 0%, #0a84ff 50%, #0070e0 100%);
  box-shadow: var(--neu-icon-glow) rgba(10,132,255,0.4), var(--neu-icon-glow-inset);
}`
});

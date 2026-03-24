TileEngine.register('cover', {
  icon: '<svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8" stroke-linecap="round" stroke-linejoin="round"><rect x="3" y="3" width="18" height="18" rx="2"/><line x1="3" y1="9" x2="21" y2="9"/><line x1="3" y1="15" x2="21" y2="15"/></svg>',

  formatState(entity) {
    const s = TileEngine.state(entity.id);
    if (s === 'unavailable' || s === 'unknown') return 'No Response';
    return s === 'closed' ? 'Closed' : 'Open';
  },

  isOn(entity) {
    return TileEngine.state(entity.id) === 'open';
  },

  isSensor: false,

  isAlert() {
    return false;
  },

  priority() {
    return 70;
  },

  render(entity) {
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
    const wasOn = TileEngine.state(entityId) === 'open';
    TileEngine.setState(entityId, wasOn ? 'closed' : 'open');
    TileEngine.lock(entityId);
    render();
    TileEngine.callService('cover', wasOn ? 'close_cover' : 'open_cover', { entity_id: entityId });
  },

  css: `.tile.state-on.type-cover .tile-icon-circle {
  background: linear-gradient(145deg, #3d9eff 0%, #0a84ff 50%, #0070e0 100%);
  box-shadow: 0 2px 6px rgba(10,132,255,0.4), inset 0 1px 0 rgba(255,255,255,0.3);
}`
});

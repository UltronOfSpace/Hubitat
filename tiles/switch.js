TileEngine.register('switch', {
  icon: '<svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8" stroke-linecap="round" stroke-linejoin="round"><path d="M13 2L3 14h9l-1 8 10-12h-9l1-8z"/></svg>',

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

  isAlert() {
    return false;
  },

  priority() {
    return 60;
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
    const wasOn = TileEngine.state(entityId) === 'on';
    TileEngine.setState(entityId, wasOn ? 'off' : 'on');
    TileEngine.lock(entityId);
    render();
    TileEngine.callService('switch', 'toggle', { entity_id: entityId });
  },

  css: `.tile.state-on.type-switch .tile-icon-circle {
  background: linear-gradient(145deg, #3d9eff 0%, #0a84ff 50%, #0070e0 100%);
  box-shadow: 0 2px 6px rgba(10,132,255,0.4), inset 0 1px 0 rgba(255,255,255,0.3);
}`
});

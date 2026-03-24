TileEngine.register('door', {
  iconDoor: '<svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8" stroke-linecap="round" stroke-linejoin="round"><rect x="5" y="2" width="14" height="20" rx="2"/><circle cx="15" cy="12" r="1" fill="currentColor"/></svg>',
  iconWindow: '<svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8" stroke-linecap="round" stroke-linejoin="round"><rect x="4" y="2" width="16" height="20" rx="1"/><line x1="12" y1="2" x2="12" y2="22"/><line x1="4" y1="11" x2="20" y2="11"/><line x1="4" y1="12.5" x2="20" y2="12.5"/></svg>',

  formatState(entity) {
    const s = TileEngine.state(entity.id);
    if (s === 'unavailable' || s === 'unknown') return 'No Response';
    return s === 'on' ? 'Open' : 'Closed';
  },

  isOn(entity) {
    return TileEngine.state(entity.id) === 'on';
  },

  isSensor: false,

  isAlert(entity) {
    return TileEngine.state(entity.id) === 'on';
  },

  priority() {
    return 100;
  },

  render(entity) {
    const icon = entity.name.toLowerCase().includes('window') ? this.iconWindow : this.iconDoor;
    return TileEngine.renderStandard(entity, {
      icon: icon,
      iconColor: TileEngine.iconColor(entity),
      state: this.formatState(entity)
    });
  },

  css: ''
});

TileEngine.register('motion', {
  icon: '<svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8" stroke-linecap="round" stroke-linejoin="round"><circle cx="12" cy="5" r="2"/><path d="M10 22V18l-2-4 4-3 4 3-2 4v4"/><path d="M6 11l2-1"/><path d="M18 11l-2-1"/></svg>',

  formatState(entity) {
    const s = TileEngine.state(entity.id);
    if (s === 'unavailable' || s === 'unknown') return 'No Response';
    return s === 'on' ? 'Open' : 'Closed';
  },

  isOn(entity) {
    return TileEngine.state(entity.id) === 'on';
  },

  isSensor: false,

  isAlert(entity) {
    return TileEngine.state(entity.id) === 'on';
  },

  priority() {
    return 110;
  },

  render(entity) {
    return TileEngine.renderStandard(entity, {
      icon: this.icon,
      iconColor: TileEngine.iconColor(entity),
      state: this.formatState(entity)
    });
  },

  css: ''
});

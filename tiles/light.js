TileEngine.register('light', {
  icon: '<svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8" stroke-linecap="round" stroke-linejoin="round"><path d="M9 18h6"/><path d="M10 22h4"/><path d="M12 2a7 7 0 0 0-4 12.7V17h8v-2.3A7 7 0 0 0 12 2z"/></svg>',

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

  priority(entity) {
    if (!entity) return 200;
    const name = (entity.name || '').toLowerCase();
    if (name.includes('ceiling')) return 30;
    return 50;
  },

  render(entity) {
    if (!entity || !entity.id) return '';
    if (!TileEngine) return '';
    const on = TileEngine.state(entity.id) === 'on';
    const dimmable = stateCache[entity.id + '_dimmable'];
    const brightness = stateCache[entity.id + '_brightness'] || (on ? 255 : 0);
    const bPct = Math.round((brightness / 255) * 100);
    let dimStyle = '';
    let nameStyle = '';
    let stateStyle = '';
    let sliderHtml = '';

    if (dimmable) {
      // Slider track background
      const oTrack = bPct / 100;
      const tMix = (off, on) => Math.round(off + (on - off) * oTrack);
      const trackBg = on
        ? `linear-gradient(145deg, rgb(${tMix(46,240)},${tMix(46,240)},${tMix(48,240)}) 0%, rgb(${tMix(28,224)},${tMix(28,224)},${tMix(30,224)}) 50%, rgb(${tMix(19,208)},${tMix(19,208)},${tMix(21,208)}) 100%)`
        : '';
      sliderHtml = `<div class="tile-slider-wrap" onclick="event.stopPropagation()"><div class="tile-slider-track" style="background:${trackBg} !important" data-entity="${entity.id}" data-slider-type="brightness" onmousedown="startDim(event)" ontouchstart="startDim(event)"><div class="tile-slider-fill" style="height:${bPct}%"></div></div></div>`;

      if (on) {
        const o = Math.round((brightness / 255) * 100) / 100;
        const mix = (off, on) => Math.round(off + (on - off) * o);
        const s1 = `rgb(${mix(46,255)},${mix(46,255)},${mix(48,255)})`;
        const s2 = `rgb(${mix(35,245)},${mix(35,245)},${mix(37,245)})`;
        const s3 = `rgb(${mix(28,235)},${mix(28,235)},${mix(30,235)})`;
        const s4 = `rgb(${mix(19,224)},${mix(19,224)},${mix(21,224)})`;
        const bt = 0.08 + (0.9 - 0.08) * o;
        const bl = 0.05 + (0.6 - 0.05) * o;
        const sh = 0.6 - 0.25 * o;
        dimStyle = ` style="background:linear-gradient(145deg, ${s1} 0%, ${s2} 30%, ${s3} 70%, ${s4} 100%);border-top:1px solid rgba(255,255,255,${bt.toFixed(2)});border-left:1px solid rgba(255,255,255,${bl.toFixed(2)});box-shadow:4px 4px 10px rgba(0,0,0,${sh.toFixed(2)}),-2px -2px 6px rgba(255,255,255,${(o*0.08).toFixed(3)}),inset 0 1px 0 rgba(255,255,255,${o})"`;

        if (o < 0.65) {
          const shadowO = (o / 0.65).toFixed(2);
          nameStyle = ` style="color:rgba(255,255,255,0.85);text-shadow:0 1px 3px rgba(0,0,0,${shadowO})"`;
          stateStyle = ` style="color:rgba(255,255,255,0.35);text-shadow:0 1px 3px rgba(0,0,0,${shadowO})"`;
        } else {
          nameStyle = ` style="color:#1c1c1e"`;
          stateStyle = ` style="color:rgba(0,0,0,0.4)"`;
        }
      }
    }

    return TileEngine.renderStandard(entity, {
      icon: this.icon,
      iconColor: TileEngine.iconColor(entity),
      state: this.formatState(entity),
      extraCls: '',
      dimStyle: dimStyle,
      onclick: ` onclick="if(!event.target.closest('.tile-slider-wrap'))toggleEntity('${entity.id}')"`,
      extraAttrs: '',
      sliderHtml: sliderHtml,
      artHtml: '',
      nameStyle: nameStyle,
      stateStyle: stateStyle,
      mediaInfoHtml: '',
      transportHtml: '',
      progressHtml: ''
    });
  },

  toggle(entityId) {
    if (!entityId) return;
    if (!TileEngine || !TileEngine.callService) return;
    const wasOn = TileEngine.state(entityId) === 'on';
    const isDim = TileEngine.attr(entityId, 'dimmable');

    if (isDim) {
      const currentBri = TileEngine.attr(entityId, 'brightness') || (wasOn ? 255 : 0);
      const deviceTransition = TileEngine.attr(entityId, 'transition');
      const transitionMs = ((deviceTransition && deviceTransition > 0) ? deviceTransition : TileEngine.defaults.transitionSec) * 1000;

      if (wasOn) {
        TileEngine.animateBrightness(entityId, currentBri, 0, transitionMs, 'off');
      } else {
        TileEngine.setState(entityId, 'on');
        TileEngine.animateBrightness(entityId, 0, 255, transitionMs);
      }
      render();

      if (wasOn) {
        TileEngine.callService('light', 'turn_off', { entity_id: entityId });
      } else {
        TileEngine.callService('light', 'turn_on', { entity_id: entityId });
      }
      return;
    }

    TileEngine.setState(entityId, wasOn ? 'off' : 'on');
    TileEngine.lock(entityId);
    render();
    TileEngine.callService('light', 'toggle', { entity_id: entityId });
  },

  css: `.tile.state-on.type-light .tile-icon-circle {
  background: linear-gradient(145deg, #ffe066 0%, #ffd60a 50%, #e6c009 100%);
  box-shadow: 0 2px 6px rgba(255,214,10,0.4), inset 0 1px 0 rgba(255,255,255,0.5);
}`
});

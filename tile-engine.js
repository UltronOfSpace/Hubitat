// tile-engine.js — Core tile rendering engine with plugin registry
// Expects globals from dashboard: HA_URL, haToken, stateCache, dimLocks, render(), fetchStates()

const TileEngine = {
  // --- Defaults (single source of truth) ---
  defaults: {
    transitionSec: 1,       // fallback dim/fade transition when device doesn't report one
    dimLockMs: 10000,        // how long to ignore WebSocket updates after local interaction
  },

  registry: {},
  toggleHandlers: {},
  _injectedCSS: new Set(),

  // --- Plugin Registry ---
  register(type, def) {
    this.registry[type] = def;
    if (def.css && !this._injectedCSS.has(type)) {
      const style = document.createElement('style');
      style.textContent = def.css;
      document.head.appendChild(style);
      this._injectedCSS.add(type);
    }
    if (def.toggle) {
      const domainMap = { media: 'media_player', door: 'binary_sensor', motion: 'binary_sensor' };
      this.toggleHandlers[domainMap[type] || type] = def.toggle;
    }
  },

  // --- State Accessors ---
  state(id) { return stateCache[id] || 'unknown'; },
  attr(id, suffix) { return stateCache[id + '_' + suffix]; },
  setState(id, val) { stateCache[id] = val; },
  setAttr(id, suffix, val) { stateCache[id + '_' + suffix] = val; },
  lock(id) { dimLocks[id] = Date.now(); },
  isLocked(id) { return dimLocks[id] && (Date.now() - dimLocks[id] < this.defaults.dimLockMs); },

  // --- HA API ---
  async callService(domain, service, data) {
    try {
      await fetch(`${HA_URL}/api/services/${domain}/${service}`, {
        method: 'POST',
        headers: { 'Authorization': 'Bearer ' + haToken, 'Content-Type': 'application/json' },
        body: JSON.stringify(data)
      });
    } catch(e) { /* silent */ }
  },

  async getEntityState(entityId) {
    const resp = await fetch(`${HA_URL}/api/states/${entityId}`, {
      headers: { 'Authorization': 'Bearer ' + haToken }
    });
    return resp.json();
  },

  // --- Rendering Helpers ---
  baseClass(entity) {
    const s = stateCache[entity.id] || '';
    const unavail = s === 'unavailable';
    const def = this.registry[entity.type];
    const alert = def && def.isAlert ? def.isAlert(entity) : false;
    const sensor = def ? !!def.isSensor : false;
    const on = def && def.isOn ? def.isOn(entity) : false;

    let cls = `type-${entity.type}`;
    if (unavail) cls += ' state-unavailable';
    else if (alert) cls += ' state-alert';
    else if (sensor) cls += ' state-sensor';
    else if (on) cls += ' state-on';
    return cls;
  },

  offlineHtml(entity) {
    if ((stateCache[entity.id] || '') !== 'unavailable') return '';
    const svg = `<svg viewBox="0 0 24 24" fill="none" stroke="#ff453a" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><line x1="1" y1="1" x2="23" y2="23"/><path d="M16.72 11.06A10.94 10.94 0 0 1 19 12.55"/><path d="M5 12.55a10.94 10.94 0 0 1 5.17-2.39"/><path d="M10.71 5.05A16 16 0 0 1 22.56 9"/><path d="M1.42 9a15.91 15.91 0 0 1 4.7-2.88"/><path d="M8.53 16.11a6 6 0 0 1 6.95 0"/><line x1="12" y1="20" x2="12.01" y2="20"/></svg>`;
    return `<div class="tile-offline">${svg}<span>Offline</span></div>`;
  },

  iconColor(entity) {
    const def = this.registry[entity.type];
    const on = def && def.isOn ? def.isOn(entity) : false;
    const sensor = def ? !!def.isSensor : false;
    const alert = def && def.isAlert ? def.isAlert(entity) : false;
    if (on && !sensor && !alert) return '#1c1c1e';
    if (alert) return 'rgba(0,0,0,0.7)';
    return 'rgba(255,255,255,0.6)';
  },

  // Standard tile template — used by simple tile types
  renderStandard(entity, opts = {}) {
    const cls = this.baseClass(entity);
    const offline = this.offlineHtml(entity);
    const ic = opts.iconColor || this.iconColor(entity);
    const icon = opts.icon || '';
    const sliderCls = opts.sliderHtml ? ' has-slider' : '';
    const artCls = opts.artHtml ? ' has-art' : '';

    return `<div class="tile ${cls}${opts.extraCls || ''}${sliderCls}${artCls}"${opts.dimStyle || ''}${opts.onclick || ''}${opts.extraAttrs || ''}>
      ${opts.artHtml || ''}${offline}${opts.sliderHtml || ''}
      <div class="tile-icon-bg">${icon}</div>
      <div class="tile-icon-circle" style="color:${ic}">${icon}</div>
      <div class="tile-bottom tile-bottom-media">
        <div class="tile-media-left">
          <div class="tile-name"${opts.nameStyle || ''}>${entity.name}</div>
          <div class="tile-state"${opts.stateStyle || ''}>${opts.state || ''}</div>
          ${opts.mediaInfoHtml || ''}
        </div>
        ${opts.transportHtml || ''}
      </div>
      ${opts.progressHtml || ''}
    </div>`;
  },

  // --- Main Render Dispatch ---
  renderTile(entity) {
    const def = this.registry[entity.type];
    if (!def) {
      return `<div class="tile type-${entity.type}"><div class="tile-bottom">
        <div class="tile-name">${entity.name}</div>
        <div class="tile-state">Unknown: ${entity.type}</div></div></div>`;
    }
    return def.render(entity);
  },

  formatState(entity) {
    const def = this.registry[entity.type];
    if (def && def.formatState) return def.formatState(entity);
    const s = stateCache[entity.id] || 'unknown';
    if (s === 'unavailable') return 'No Response';
    return s === 'on' ? 'On' : s === 'off' ? 'Off' : s.charAt(0).toUpperCase() + s.slice(1);
  },

  isOn(entity) {
    const def = this.registry[entity.type];
    return def && def.isOn ? def.isOn(entity) : false;
  },

  isSensor(entity) {
    const def = this.registry[entity.type];
    return def ? !!def.isSensor : false;
  },

  isAlert(entity) {
    const def = this.registry[entity.type];
    return def && def.isAlert ? def.isAlert(entity) : false;
  },

  tilePriority(entity) {
    const def = this.registry[entity.type];
    return def && def.priority ? def.priority(entity) : 200;
  },

  // --- Grid ---
  calcGrid(count, containerW, containerH) {
    if (count === 0) return { cols: 1, rows: 1 };
    let bestCols = 1, bestSize = 0;
    for (let cols = 1; cols <= Math.min(count, 8); cols++) {
      const rows = Math.ceil(count / cols);
      const cellW = (containerW - (cols - 1) * 10) / cols;
      const cellH = (containerH - (rows - 1) * 10) / rows;
      const size = Math.min(cellW, cellH);
      if (size > bestSize) { bestSize = size; bestCols = cols; }
    }
    return { cols: bestCols, rows: Math.ceil(count / bestCols) };
  },

  // --- Slider Infrastructure ---
  sliderActiveUntil: 0,
  sliderAnimating: false,
  _dimDebounce: null,
  _volDebounce: null,

  setBrightness(entityId, value) {
    stateCache[entityId + '_brightness'] = value;
    stateCache[entityId + '_percentage'] = Math.round((value / 255) * 100);
    if (value === 0) stateCache[entityId] = 'off';
    else if (stateCache[entityId] === 'off') stateCache[entityId] = 'on';
    dimLocks[entityId] = Date.now();

    clearTimeout(this._dimDebounce);
    this._dimDebounce = setTimeout(() => {
      const domain = entityId.split('.')[0];
      if (domain === 'fan') {
        const pct = Math.round((value / 255) * 100);
        if (pct === 0) this.callService('fan', 'turn_off', { entity_id: entityId });
        else this.callService('fan', 'set_percentage', { entity_id: entityId, percentage: pct });
      } else {
        if (value === 0) this.callService('light', 'turn_off', { entity_id: entityId });
        else this.callService('light', 'turn_on', { entity_id: entityId, brightness: value });
      }
    }, 300);
  },

  _volDebounces: {},
  setVolume(entityId, value, sendNow) {
    const vol = value / 255;
    stateCache[entityId + '_volume'] = vol;
    dimLocks[entityId] = Date.now();

    if (sendNow) {
      clearTimeout(this._volDebounces[entityId]);
      this.callService('media_player', 'volume_set', { entity_id: entityId, volume_level: vol });
    } else {
      clearTimeout(this._volDebounces[entityId]);
      this._volDebounces[entityId] = setTimeout(() => {
        this.callService('media_player', 'volume_set', { entity_id: entityId, volume_level: vol });
      }, 300);
    }
  },

  startDim(e) {
    if (e.button && e.button !== 0) return;
    e.preventDefault(); e.stopPropagation();
    TileEngine.sliderActiveUntil = Date.now() + 60000;
    const track = e.currentTarget;
    const entityId = track.dataset.entity;
    const fill = track.querySelector('.tile-slider-fill');
    const isVolume = track.dataset.sliderType === 'volume';
    const tile = track.closest('.tile');

    function update(clientY) {
      const rect = track.getBoundingClientRect();
      const pct = Math.max(0, Math.min(1, (rect.bottom - clientY) / rect.height));
      const val = Math.round(pct * 255);
      fill.style.height = (pct * 100) + '%';
      if (isVolume) { TileEngine.setVolume(entityId, val); }
      else {
        TileEngine.setBrightness(entityId, val);
        if (tile) {
          const o = pct;
          if (pct > 0) {
            tile.style.background = `linear-gradient(145deg, rgba(255,255,255,${o}) 0%, rgba(245,245,245,${o}) 30%, rgba(235,235,235,${o}) 70%, rgba(224,224,224,${o}) 100%)`;
            tile.style.borderTop = `1px solid rgba(255,255,255,${o*0.9})`;
            tile.style.borderLeft = `1px solid rgba(255,255,255,${o*0.6})`;
            tile.style.boxShadow = `4px 4px 10px rgba(0,0,0,0.35),-2px -2px 6px rgba(255,255,255,${o*0.08}),inset 0 1px 0 rgba(255,255,255,${o})`;
          } else {
            tile.style.background = '';
            tile.style.borderTop = '';
            tile.style.borderLeft = '';
            tile.style.boxShadow = '';
          }
        }
      }
    }

    let dragged = false;

    function onMove(ev) {
      ev.preventDefault();
      dragged = true;
      const y = ev.touches ? ev.touches[0].clientY : ev.clientY;
      update(y);
    }
    function onUp(ev) {
      document.removeEventListener('mousemove', onMove);
      document.removeEventListener('mouseup', onUp);
      document.removeEventListener('touchmove', onMove);
      document.removeEventListener('touchend', onUp);
      TileEngine.sliderActiveUntil = Date.now() + 500;

      if (!dragged) {
        // Click without drag — animate to target using global transition
        const y = ev.changedTouches ? ev.changedTouches[0].clientY : ev.clientY;
        const rect = track.getBoundingClientRect();
        const targetPct = Math.max(0, Math.min(1, (rect.bottom - y) / rect.height));
        const targetVal = Math.round(targetPct * 255);
        const currentPct = parseFloat(fill.style.height) / 100 || 0;
        const duration = TileEngine.defaults.transitionSec * 1000;

        // Frame-by-frame animation for slider fill (same for volume and brightness)
        TileEngine.sliderAnimating = true;
        const steps = Math.max(10, Math.round(duration / 20));
        let step = 0;
        // For volume: schedule stepped HA commands separately from the visual animation
        if (isVolume) {
          const volSteps = 5;
          for (let i = 1; i <= volSteps; i++) {
            setTimeout(() => {
              const t = i / volSteps;
              const pct = currentPct + (targetPct - currentPct) * t;
              TileEngine.setVolume(entityId, Math.round(pct * 255), true);
            }, (duration / volSteps) * i);
          }
        }
        function animateStep() {
          step++;
          const t = step / steps;
          const ease = t < 0.5 ? 2 * t * t : -1 + (4 - 2 * t) * t;
          const pct = currentPct + (targetPct - currentPct) * ease;
          const val = Math.round(pct * 255);
          fill.style.height = (pct * 100) + '%';
          if (isVolume) {
            stateCache[entityId + '_volume'] = pct;
            dimLocks[entityId] = Date.now();
          } else {
            TileEngine.setBrightness(entityId, val);
            if (tile) {
              const o = pct;
              if (o > 0) {
                tile.style.background = `linear-gradient(145deg, rgba(255,255,255,${o}) 0%, rgba(245,245,245,${o}) 30%, rgba(235,235,235,${o}) 70%, rgba(224,224,224,${o}) 100%)`;
                tile.style.borderTop = `1px solid rgba(255,255,255,${o*0.9})`;
                tile.style.borderLeft = `1px solid rgba(255,255,255,${o*0.6})`;
                tile.style.boxShadow = `4px 4px 10px rgba(0,0,0,0.35),-2px -2px 6px rgba(255,255,255,${o*0.08}),inset 0 1px 0 rgba(255,255,255,${o})`;
              } else {
                tile.style.background = '';
                tile.style.borderTop = '';
                tile.style.borderLeft = '';
                tile.style.boxShadow = '';
              }
            }
          }
          if (step >= steps) { TileEngine.sliderAnimating = false; setTimeout(fetchStates, 1000); return; }
          setTimeout(animateStep, duration / steps);
        }
        animateStep();
      } else {
        setTimeout(fetchStates, 1000);
      }
    }

    document.addEventListener('mousemove', onMove);
    document.addEventListener('mouseup', onUp);
    document.addEventListener('touchmove', onMove, { passive: false });
    document.addEventListener('touchend', onUp);
  },

  // --- Brightness Animation ---
  dimAnimations: {},

  animateBrightness(entityId, from, to, durationMs, finalState) {
    this.dimAnimations[entityId] = {
      from, to,
      startTime: performance.now(),
      duration: durationMs,
      finalState: finalState || null
    };
  },

  cancelAnimation(entityId) {
    delete this.dimAnimations[entityId];
  },

  dimAnimLoop() {
    const now = performance.now();
    for (const [entityId, anim] of Object.entries(TileEngine.dimAnimations)) {
      const elapsed = now - anim.startTime;
      const progress = Math.min(1, elapsed / anim.duration);
      const current = Math.round(anim.from + (anim.to - anim.from) * progress);
      stateCache[entityId + '_brightness'] = current;

      if (progress >= 1) {
        if (anim.finalState) stateCache[entityId] = anim.finalState;
        delete TileEngine.dimAnimations[entityId];
        TileEngine.dimAnimUpdateTile(entityId, anim.to);
        setTimeout(() => render(), 50);
      } else {
        TileEngine.dimAnimUpdateTile(entityId, current);
      }
    }
    requestAnimationFrame(TileEngine.dimAnimLoop);
  },

  dimAnimUpdateTile(entityId, brightness) {
    const track = document.querySelector(`[data-entity="${entityId}"][data-slider-type="brightness"]`);
    if (!track) return;
    const tile = track.closest('.tile');
    if (!tile) return;
    const fill = track.querySelector('.tile-slider-fill');
    const bPct = Math.round((brightness / 255) * 100);
    const o = bPct / 100;

    if (fill) fill.style.height = bPct + '%';

    if (!tile.classList.contains('state-on')) {
      const tMix = (off, on) => Math.round(off + (on - off) * o);
      track.style.background = `linear-gradient(145deg, rgb(${tMix(46,240)},${tMix(46,240)},${tMix(48,240)}) 0%, rgb(${tMix(28,224)},${tMix(28,224)},${tMix(30,224)}) 50%, rgb(${tMix(19,208)},${tMix(19,208)},${tMix(21,208)}) 100%)`;
    } else {
      track.style.background = '';
    }

    const mix = (off, on) => Math.round(off + (on - off) * o);
    tile.style.background = `linear-gradient(145deg, rgb(${mix(46,255)},${mix(46,255)},${mix(48,255)}) 0%, rgb(${mix(35,245)},${mix(35,245)},${mix(37,245)}) 30%, rgb(${mix(28,235)},${mix(28,235)},${mix(30,235)}) 70%, rgb(${mix(19,224)},${mix(19,224)},${mix(21,224)}) 100%)`;
    const bt = 0.08 + (0.9 - 0.08) * o;
    const bl = 0.05 + (0.6 - 0.05) * o;
    const sh = 0.6 - 0.25 * o;
    tile.style.borderTop = `1px solid rgba(255,255,255,${bt.toFixed(2)})`;
    tile.style.borderLeft = `1px solid rgba(255,255,255,${bl.toFixed(2)})`;
    tile.style.boxShadow = `4px 4px 10px rgba(0,0,0,${sh.toFixed(2)}),-2px -2px 6px rgba(255,255,255,${(o*0.08).toFixed(3)}),inset 0 1px 0 rgba(255,255,255,${o})`;

    const nameEl = tile.querySelector('.tile-name');
    const stateEl = tile.querySelector('.tile-state');
    if (o < 0.65) {
      const shadowO = (o / 0.65).toFixed(2);
      if (nameEl) { nameEl.style.color = 'rgba(255,255,255,0.85)'; nameEl.style.textShadow = `0 1px 3px rgba(0,0,0,${shadowO})`; }
      if (stateEl) { stateEl.style.color = 'rgba(255,255,255,0.35)'; stateEl.style.textShadow = `0 1px 3px rgba(0,0,0,${shadowO})`; }
    } else {
      if (nameEl) { nameEl.style.color = '#1c1c1e'; nameEl.style.textShadow = ''; }
      if (stateEl) { stateEl.style.color = 'rgba(0,0,0,0.4)'; stateEl.style.textShadow = ''; }
    }
  },

  // --- Fan Animation ---
  _fanState: {},
  fanLoop() {
    document.querySelectorAll('.tile.type-fan').forEach(tile => {
      const svg = tile.querySelector('.tile-icon-circle svg');
      if (!svg) return;
      const entityId = tile.dataset.entity;
      if (!entityId) return;

      if (!TileEngine._fanState[entityId]) TileEngine._fanState[entityId] = { angle: 0, currentSpeed: 0, targetSpeed: 0 };
      const fs = TileEngine._fanState[entityId];

      if (tile.classList.contains('fan-high')) fs.targetSpeed = 18;
      else if (tile.classList.contains('fan-med')) fs.targetSpeed = 10;
      else if (tile.classList.contains('fan-low')) fs.targetSpeed = 5;
      else fs.targetSpeed = 0;

      fs.currentSpeed += (fs.targetSpeed - fs.currentSpeed) * 0.05;
      if (Math.abs(fs.currentSpeed - fs.targetSpeed) < 0.01) fs.currentSpeed = fs.targetSpeed;

      fs.angle = (fs.angle + fs.currentSpeed) % 360;
      svg.style.transform = `rotate(${fs.angle}deg)`;
    });
    requestAnimationFrame(TileEngine.fanLoop);
  },

  // --- UI Helpers ---
  autoSizeNames() {
    // Watermark icons
    document.querySelectorAll('.tile-icon-bg').forEach(bg => {
      const tile = bg.closest('.tile');
      if (!tile) return;
      const tw = tile.clientWidth, th = tile.clientHeight;
      const show = tw >= 40 && th >= 40;
      bg.style.display = show ? 'block' : 'none';
      if (!show) return;

      const tileRect = tile.getBoundingClientRect();
      const iconCircle = tile.querySelector('.tile-icon-circle');
      const slider = tile.querySelector('.tile-slider-wrap');
      const fanBtns = tile.querySelector('.tile-fan-speeds');

      const cs = getComputedStyle(tile);
      const padL = parseFloat(cs.paddingLeft) || 0;
      const padR = parseFloat(cs.paddingRight) || 0;
      const padT = parseFloat(cs.paddingTop) || 0;

      const iconPad = iconCircle ? iconCircle.clientHeight * 0.3 : 5;
      const top0 = iconCircle ? (iconCircle.getBoundingClientRect().bottom - tileRect.top + iconPad) : padT;
      const bot0 = th * 0.75 - iconPad;
      const right0 = slider ? (slider.getBoundingClientRect().left - tileRect.left) :
                     fanBtns ? (fanBtns.getBoundingClientRect().left - tileRect.left) : tw;
      const left0 = 0;
      const deadW = right0;
      const deadH = bot0 - top0;

      const iconEl = tile.querySelector('.tile-icon-circle');
      const iconSize = iconEl ? iconEl.clientWidth : 0;
      const size = Math.min(iconSize * 2.5, deadW * 0.65, deadH * 0.65);

      if (size < 8 || deadW < 20 || deadH < 20) {
        bg.style.display = 'none';
        return;
      }

      const cx = left0 + deadW / 2;
      const cy = top0 + deadH / 2;

      bg.style.width = size + 'px';
      bg.style.height = size + 'px';
      bg.style.left = (cx - size / 2) + 'px';
      bg.style.top = (cy - size / 2) + 'px';
    });

    // Room tabs
    document.querySelectorAll('.room-tab').forEach(el => {
      const text = el.textContent.trim();
      const hasSpaces = text.includes(' ');
      const maxW = el.clientWidth - 4;
      const isActive = el.classList.contains('active');
      let size = Math.min(13, Math.max(7, window.innerWidth * 0.009));
      if (isActive) size *= 1.2;
      el.style.fontSize = size + 'px';

      if (hasSpaces) {
        el.style.whiteSpace = 'normal';
        const words = text.split(/\s+/);
        const span = document.createElement('span');
        span.style.cssText = `font-weight:600;white-space:nowrap;visibility:hidden;position:absolute;`;
        document.body.appendChild(span);
        let tries = 0, fits = false;
        while (!fits && size > 5 && tries < 20) {
          span.style.fontSize = size + 'px';
          fits = true;
          for (const w of words) { span.textContent = w; if (span.offsetWidth > maxW) { fits = false; break; } }
          if (!fits) { size -= 0.5; el.style.fontSize = size + 'px'; }
          tries++;
        }
        span.remove();
      } else {
        el.style.whiteSpace = 'nowrap';
        let tries = 0;
        while (el.scrollWidth > el.clientWidth + 1 && size > 5 && tries < 20) {
          size -= 0.5;
          el.style.fontSize = size + 'px';
          tries++;
        }
      }
    });

    // Floor tabs
    document.querySelectorAll('.floor-tab').forEach(el => {
      const isActive = el.classList.contains('active');
      let size = Math.min(11, Math.max(7, window.innerHeight * 0.012));
      if (isActive) size *= 1.2;
      el.style.fontSize = size + 'px';
    });

    // Tile state text
    document.querySelectorAll('.tile-state').forEach(el => {
      let size = Math.min(13, Math.max(7, window.innerWidth * 0.0085));
      el.style.fontSize = size + 'px';
      let tries = 0;
      while (el.scrollWidth > el.clientWidth + 1 && size > 6 && tries < 8) {
        size -= 0.5;
        el.style.fontSize = size + 'px';
        tries++;
      }
    });

    // Tile names
    document.querySelectorAll('.tile-name').forEach(el => {
      const text = el.textContent.trim();
      const hasSpaces = text.includes(' ');
      const tile = el.closest('.tile');
      const hasSlider = tile && tile.classList.contains('has-slider');
      let size = Math.min(16, Math.max(8, window.innerWidth * 0.011));
      el.style.fontSize = size + 'px';

      const hasControls = hasSlider || (tile && tile.classList.contains('has-fan-speeds'));
      const pad = tile.clientWidth * 0.04;
      const maxW = hasControls
        ? (tile.clientWidth * 0.5 - pad)
        : (tile.clientWidth - tile.clientWidth * 0.16 - pad);

      if (hasSpaces) {
        el.style.whiteSpace = 'normal';
        el.style.display = 'block';
        el.style.maxWidth = maxW + 'px';
        el.style.overflow = 'visible';
        const words = text.split(/\s+/);
        const span = document.createElement('span');
        span.style.cssText = `font-size:${size}px;font-weight:600;white-space:nowrap;visibility:hidden;position:absolute;`;
        document.body.appendChild(span);
        let tries = 0, fits = false;
        while (!fits && size > 5 && tries < 20) {
          span.style.fontSize = size + 'px';
          fits = true;
          for (const w of words) {
            span.textContent = w;
            if (span.offsetWidth > maxW + 1) { fits = false; break; }
          }
          if (!fits) { size -= 0.5; el.style.fontSize = size + 'px'; }
          tries++;
        }
        span.remove();
      } else {
        el.style.whiteSpace = 'nowrap';
        el.style.display = 'block';
        el.style.maxWidth = maxW + 'px';
        el.style.overflow = 'visible';
        let tries = 0;
        while (el.scrollWidth > maxW + 1 && size > 5 && tries < 20) {
          size -= 0.5;
          el.style.fontSize = size + 'px';
          tries++;
        }
      }

      const bottom = el.closest('.tile-bottom') || el.closest('.tile-bottom-media');
      if (bottom && tile) {
        const tileRect = tile.getBoundingClientRect();
        let tries2 = 0;
        while (bottom.getBoundingClientRect().bottom > tileRect.bottom && size > 5 && tries2 < 20) {
          size -= 0.5;
          el.style.fontSize = size + 'px';
          tries2++;
        }
      }
    });
  },

  // --- Engine Init ---
  _origRender: null,

  initTileEngine() {
    this._origRender = render;
    render = function() {
      TileEngine._origRender();
      requestAnimationFrame(() => TileEngine.autoSizeNames());
    };
    window.origRender = this._origRender;

    requestAnimationFrame(TileEngine.dimAnimLoop);
    requestAnimationFrame(TileEngine.fanLoop);

    // Progress bar updater
    setInterval(() => {
      document.querySelectorAll('.tile.tile-wide .tile-progress-fill').forEach(fill => {
        const tile = fill.closest('.tile');
        if (!tile) return;
        const entityId = Object.keys(stateCache).find(k =>
          k.endsWith('_duration') && tile.querySelector(`[onclick*="${k.replace('_duration','')}"]`)
        );
        if (!entityId) return;
        const id = entityId.replace('_duration', '');
        const mediaDef = TileEngine.registry.media;
        if (mediaDef && mediaDef.getMediaPosition) {
          const { pos, dur } = mediaDef.getMediaPosition(id);
          if (dur > 0) fill.style.width = Math.min(100, Math.round((pos / dur) * 100)) + '%';
        }
      });
    }, 1000);
  }
};

// --- Global function bindings (for onclick handlers in tile HTML) ---
function toggleEntity(entityId) {
  if (!haToken || Date.now() < TileEngine.sliderActiveUntil) return;
  const domain = entityId.split('.')[0];
  const handler = TileEngine.toggleHandlers[domain];
  if (handler) handler(entityId);
}
function startDim(e) { TileEngine.startDim(e); }
function setBrightness(id, val) { TileEngine.setBrightness(id, val); }
function setVolume(id, val) { TileEngine.setVolume(id, val); }
function calcGrid(count, w, h) { return TileEngine.calcGrid(count, w, h); }
function renderTile(e) { return TileEngine.renderTile(e); }
function formatState(e) { return TileEngine.formatState(e); }
function isOn(e) { return TileEngine.isOn(e); }
function isSensor(e) { return TileEngine.isSensor(e); }
function isAlert(e) { return TileEngine.isAlert(e); }
function tilePriority(e) { return TileEngine.tilePriority(e); }
function autoSizeNames() { TileEngine.autoSizeNames(); }
function initTileEngine() { TileEngine.initTileEngine(); }
function isDimLocked(id) { return TileEngine.isLocked(id); }
function animateBrightness(a, b, c, d, e) { TileEngine.animateBrightness(a, b, c, d, e); }
function cancelAnimation(id) { TileEngine.cancelAnimation(id); }
// Expose dimAnimations globally for WebSocket handler in dashboard
var dimAnimations = TileEngine.dimAnimations;

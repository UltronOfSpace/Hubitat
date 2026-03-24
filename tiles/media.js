/* media.js – Media player tile plugin */

const mediaIcon = `<svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8" stroke-linecap="round" stroke-linejoin="round"><rect x="2" y="7" width="20" height="15" rx="2" ry="2"/><polyline points="17 2 12 7 7 2"/></svg>`;
const speakerIcon = `<svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8" stroke-linecap="round" stroke-linejoin="round"><rect x="4" y="2" width="16" height="20" rx="2"/><circle cx="12" cy="14" r="4"/><circle cx="12" cy="6" r="1" fill="currentColor"/></svg>`;

// Media press/hold logic
let mediaTimer = null;
let mediaHandled = false;

// Group volume popup
function openGroupVolumePopup(entityId) {
  // Find the entity config to get groupMembers
  let members = null;
  Object.values(data).forEach(floor =>
    Object.values(floor.rooms).forEach(room =>
      room.entities.forEach(e => {
        if (e.id === entityId && e.groupMembers) members = e.groupMembers;
      })
    )
  );
  if (!members || !members.length) return;

  // Remove existing popup
  const old = document.getElementById('group-volume-popup');
  if (old) old.remove();

  const overlay = document.createElement('div');
  overlay.id = 'group-volume-popup';
  overlay.className = 'gvp-overlay';
  overlay.onclick = (e) => { if (e.target === overlay) overlay.remove(); };

  const card = document.createElement('div');
  card.className = 'gvp-card';

  const title = document.createElement('div');
  title.className = 'gvp-title';
  const eName = stateCache[entityId + '_title'] ? stateCache[entityId + '_artist'] + ' — ' + stateCache[entityId + '_title'] : 'Speaker Volumes';
  title.textContent = eName;
  card.appendChild(title);

  const sliders = document.createElement('div');
  sliders.className = 'gvp-sliders';

  members.forEach(m => {
    const col = document.createElement('div');
    col.className = 'gvp-slider-col';

    const label = document.createElement('div');
    label.className = 'gvp-label';
    label.textContent = m.name;

    const trackWrap = document.createElement('div');
    trackWrap.className = 'gvp-track-wrap';

    const track = document.createElement('div');
    track.className = 'tile-slider-track';
    track.dataset.entity = m.id;
    track.dataset.sliderType = 'volume';
    track.onmousedown = (e) => startDim(e);
    track.ontouchstart = (e) => startDim(e);

    const fill = document.createElement('div');
    fill.className = 'tile-slider-fill';
    const vol = stateCache[m.id + '_volume'] || 0;
    fill.style.height = Math.round(vol * 100) + '%';

    const pctLabel = document.createElement('div');
    pctLabel.className = 'gvp-pct';
    pctLabel.textContent = Math.round(vol * 100) + '%';
    pctLabel.dataset.entity = m.id;

    track.appendChild(fill);
    trackWrap.appendChild(track);
    col.appendChild(label);
    col.appendChild(trackWrap);
    col.appendChild(pctLabel);
    sliders.appendChild(col);
  });

  card.appendChild(sliders);
  overlay.appendChild(card);
  document.body.appendChild(overlay);

  // Live-update fills and pct labels
  const updateInterval = setInterval(() => {
    if (!document.getElementById('group-volume-popup')) { clearInterval(updateInterval); return; }
    members.forEach(m => {
      const vol = stateCache[m.id + '_volume'] || 0;
      const pct = Math.round(vol * 100);
      const t = overlay.querySelector(`.tile-slider-track[data-entity="${m.id}"] .tile-slider-fill`);
      if (t) t.style.height = pct + '%';
      const p = overlay.querySelector(`.gvp-pct[data-entity="${m.id}"]`);
      if (p) p.textContent = pct + '%';
    });
  }, 200);
}

function mediaContext(entityId, e) {
  e.preventDefault();
  // Check if this is a group with members
  let hasMembers = false;
  Object.values(data).forEach(floor =>
    Object.values(floor.rooms).forEach(room =>
      room.entities.forEach(ent => {
        if (ent.id === entityId && ent.groupMembers && ent.groupMembers.length) hasMembers = true;
      })
    )
  );
  const isOn = stateCache[entityId] === 'playing' || stateCache[entityId] === 'paused' || stateCache[entityId] === 'idle';
  if (hasMembers && isOn) openGroupVolumePopup(entityId);
}

function mediaDown(entityId, e) {
  if (e.button && e.button !== 0) return;
  if (e.target.closest('.tile-slider-wrap') || e.target.closest('.tile-media-controls') || e.target.closest('.tile-progress-wrap') || e.target.closest('.media-ctrl-btn')) return;
  e.preventDefault();
  mediaHandled = false;

  // Check if this is a group with members
  let hasMembers = false;
  Object.values(data).forEach(floor =>
    Object.values(floor.rooms).forEach(room =>
      room.entities.forEach(ent => {
        if (ent.id === entityId && ent.groupMembers && ent.groupMembers.length) hasMembers = true;
      })
    )
  );
  const isOn = stateCache[entityId] === 'playing' || stateCache[entityId] === 'paused' || stateCache[entityId] === 'idle';

  mediaTimer = setTimeout(() => {
    mediaHandled = true;
    if (hasMembers && isOn) {
      openGroupVolumePopup(entityId);
    } else {
      stateCache[entityId] = 'off';
      dimLocks[entityId] = Date.now();
      render();
      TileEngine.callService('media_player', 'turn_off', { entity_id: entityId });
    }
  }, 600);
}

function mediaUp(entityId, e) {
  if (e.target.closest('.tile-slider-wrap') || e.target.closest('.tile-media-controls') || e.target.closest('.tile-progress-wrap') || e.target.closest('.media-ctrl-btn')) return;
  clearTimeout(mediaTimer);
  if (mediaHandled) return;

  const s = stateCache[entityId] || 'off';
  let service;
  if (s === 'off' || s === 'unavailable') { service = 'turn_on'; stateCache[entityId] = 'on'; }
  else if (s === 'playing') { service = 'media_pause'; stateCache[entityId] = 'paused'; }
  else if (s === 'paused') { service = 'media_play'; stateCache[entityId] = 'playing'; }
  else { service = 'turn_off'; stateCache[entityId] = 'off'; }

  dimLocks[entityId] = Date.now();
  render();
  TileEngine.callService('media_player', service, { entity_id: entityId });
}

function mediaSkip(entityId, direction, e) {
  e.stopPropagation();
  const service = direction === 'next' ? 'media_next_track' : 'media_previous_track';
  TileEngine.callService('media_player', service, { entity_id: entityId });
  setTimeout(fetchStates, 1000);
}

function mediaSeek(entityId, e) {
  e.stopPropagation();
  const track = e.currentTarget;
  const rect = track.getBoundingClientRect();
  const pct = Math.max(0, Math.min(1, (e.clientX - rect.left) / rect.width));
  const duration = stateCache[entityId + '_duration'] || 0;
  if (!duration) return;
  const seekTo = Math.round(pct * duration);
  stateCache[entityId + '_position'] = seekTo;
  stateCache[entityId + '_position_at'] = new Date().toISOString();
  TileEngine.callService('media_player', 'media_seek', { entity_id: entityId, seek_position: seekTo });
}

TileEngine.register('media', {
  icon(entity) {
    const n = entity.name.toLowerCase();
    return (n.includes('tv') || n.includes('chromecast') || n.includes('display')) ? mediaIcon : speakerIcon;
  },

  formatState(entity) {
    const s = TileEngine.state(entity.id);
    if (s === 'unavailable') return 'No Response';
    if (s === 'playing') return '\u25b6 Playing';
    if (s === 'paused') return '\u275a\u275a Paused';
    if (s === 'idle') return 'Idle';
    if (s === 'off') return 'Off';
    return s.charAt(0).toUpperCase() + s.slice(1);
  },

  isOn(entity) {
    const s = TileEngine.state(entity.id);
    return s === 'playing' || s === 'paused' || s === 'idle';
  },

  isSensor: false,

  isAlert() { return false; },

  priority(entity) {
    const n = entity.name.toLowerCase();
    if (n.includes('tv')) return 230;
    if (n.includes('chromecast') || n.includes('display')) return 220;
    return 210;
  },

  getMediaPosition(entityId) {
    const pos = stateCache[entityId + '_position'] || 0;
    const at = stateCache[entityId + '_position_at'] || '';
    const dur = stateCache[entityId + '_duration'] || 0;
    const state = stateCache[entityId] || '';
    if (!at || !dur || state !== 'playing') return { pos, dur };
    const elapsed = (Date.now() - new Date(at).getTime()) / 1000;
    return { pos: Math.min(dur, pos + elapsed), dur };
  },

  toggle(entityId) {
    // Not used directly — mediaDown/mediaUp handles interaction
    // But needed for toggleEntity dispatch
    const s = stateCache[entityId] || 'off';
    if (s === 'playing') { stateCache[entityId] = 'paused'; TileEngine.callService('media_player', 'media_pause', { entity_id: entityId }); }
    else if (s === 'paused') { stateCache[entityId] = 'playing'; TileEngine.callService('media_player', 'media_play', { entity_id: entityId }); }
    else { stateCache[entityId] = 'off'; TileEngine.callService('media_player', 'turn_off', { entity_id: entityId }); }
    dimLocks[entityId] = Date.now();
    render();
  },

  render(entity) {
    const T = TileEngine;
    const on = this.isOn(entity);
    const state = this.formatState(entity);
    const cls = T.baseClass(entity);
    const offline = T.offlineHtml(entity);
    const ic = T.iconColor(entity);
    const n = entity.name.toLowerCase();
    const icon = (n.includes('tv') || n.includes('chromecast') || n.includes('display')) ? mediaIcon : speakerIcon;

    // Volume slider (only when on)
    let sliderHtml = '', sliderCls = '';
    if (on) {
      const vol = stateCache[entity.id + '_volume'] || 0;
      const vPct = Math.round(vol * 100);
      sliderHtml = `<div class="tile-slider-wrap" onclick="event.stopPropagation()"><div class="tile-slider-track" data-entity="${entity.id}" data-slider-type="volume" onmousedown="startDim(event)" ontouchstart="startDim(event)"><div class="tile-slider-fill" style="height:${vPct}%"></div></div></div>`;
      sliderCls = ' has-slider';
    }

    // Album art
    let artHtml = '', artCls = '';
    const pic = stateCache[entity.id + '_picture'] || '';
    const title = stateCache[entity.id + '_title'] || '';
    const artist = stateCache[entity.id + '_artist'] || '';
    if (pic) {
      const imgUrl = pic.startsWith('/') ? HA_URL + pic : pic;
      artHtml = `<div class="tile-album-art"><img src="${imgUrl}" alt=""></div>`;
      artCls = ' has-art';
    }
    let mediaInfoHtml = '';
    if (title) {
      mediaInfoHtml = `<div class="tile-media-info">${artist ? artist + ' \u2014 ' : ''}${title}</div>`;
    }

    // Transport controls
    let transportHtml = '', progressHtml = '';
    const ms = stateCache[entity.id] || 'off';
    const isPlaying = ms === 'playing' || ms === 'paused';
    const { pos, dur } = this.getMediaPosition(entity.id);

    if (isPlaying) {
      const prevSvg = `<svg viewBox="0 0 24 24" fill="currentColor"><path d="M6 6h2v12H6z"/><path d="M18 18L9.5 12 18 6v12z"/></svg>`;
      const nextSvg = `<svg viewBox="0 0 24 24" fill="currentColor"><path d="M16 6h2v12h-2z"/><path d="M6 18l8.5-6L6 6v12z"/></svg>`;
      transportHtml = `<div class="tile-media-controls" onclick="event.stopPropagation()">
        <button class="media-ctrl-btn" onclick="mediaSkip('${entity.id}','prev',event)">${prevSvg}</button>
        <button class="media-ctrl-btn" onclick="mediaSkip('${entity.id}','next',event)">${nextSvg}</button>
      </div>`;
    }

    if (dur > 0) {
      const pct = Math.min(100, Math.round((pos / dur) * 100));
      progressHtml = `<div class="tile-progress-wrap" onclick="mediaSeek('${entity.id}',event)"><div class="tile-progress-track"><div class="tile-progress-fill" style="width:${pct}%"><div class="tile-progress-knob"></div></div></div></div>`;
    }

    return `<div class="tile ${cls}${sliderCls}${artCls} tile-wide" onmousedown="mediaDown('${entity.id}',event)" onmouseup="mediaUp('${entity.id}',event)" ontouchstart="mediaDown('${entity.id}',event)" ontouchend="mediaUp('${entity.id}',event)" oncontextmenu="mediaContext('${entity.id}',event)">
      ${artHtml}${offline}${sliderHtml}
      <div class="tile-icon-bg">${icon}</div>
      <div class="tile-icon-circle" style="color:${ic}">${icon}</div>
      <div class="tile-bottom tile-bottom-media">
        <div class="tile-media-left">
          <div class="tile-name">${entity.name}</div>
          <div class="tile-state">${state}</div>
          ${mediaInfoHtml}
        </div>
        ${transportHtml}
      </div>
      ${progressHtml}
    </div>`;
  },

  css: `
.tile.state-on.type-media .tile-icon-circle {
  background: linear-gradient(145deg, #7b79eb 0%, #5e5ce6 50%, #4e4cd0 100%);
  box-shadow: 0 2px 6px rgba(94,92,230,0.4), inset 0 1px 0 rgba(255,255,255,0.3);
}
.tile-album-art {
  position: absolute; top: 0; left: 0; right: 0; bottom: 0;
  border-radius: 16px; overflow: hidden; z-index: 0;
}
.tile-album-art img {
  width: 100%; height: 100%; object-fit: cover;
  filter: brightness(0.5) saturate(1.3); transition: filter 0.3s;
}
.tile.state-on .tile-album-art img { filter: brightness(0.45) saturate(1.4); }
.tile-album-art::before {
  content: ''; position: absolute; top: 0; left: 0; right: 0; bottom: 0;
  background: rgba(0,0,0,0.15);
  backdrop-filter: blur(1px); -webkit-backdrop-filter: blur(1px); z-index: 1;
}
.tile-album-art::after {
  content: ''; position: absolute; bottom: 0; left: 0; right: 0; height: 65%;
  background: linear-gradient(transparent, rgba(0,0,0,0.85)); z-index: 2;
}
.tile.has-art .tile-bottom { position: relative; z-index: 3; }
.tile.has-art .tile-slider-wrap { z-index: 3; }
.tile.has-art {
  border: none !important; overflow: hidden !important;
  box-shadow: 4px 4px 12px rgba(0,0,0,0.6), -2px -2px 6px rgba(0,0,0,0.2) !important;
}
.tile.has-art .tile-name { color: #fff; }
.tile.has-art .tile-state { color: rgba(255,255,255,0.7); }
.tile.has-art .tile-icon-circle { display: none; }
.tile-media-info {
  font-size: clamp(7px, 0.7vw, 10px); color: rgba(255,255,255,0.6);
  overflow: hidden; text-overflow: ellipsis; white-space: nowrap; margin-top: 2px;
}
.tile-bottom-media {
  display: flex; align-items: center; justify-content: space-between; width: 100%;
}
.tile-wide .tile-bottom-media { max-width: 70% !important; }
.tile-media-left { flex: 1; min-width: 0; overflow: hidden; }
.tile-media-controls {
  display: flex; align-items: center; justify-content: space-evenly;
  flex: 1; margin-left: clamp(8px, 2vw, 20px);
}
.media-ctrl-btn {
  border: none; cursor: pointer; background: transparent;
  color: rgba(255,255,255,0.6); padding: 4px;
  transition: color 0.15s, transform 0.1s;
  display: flex; align-items: center; justify-content: center;
}
.media-ctrl-btn:hover { color: #fff; }
.media-ctrl-btn:active { transform: scale(0.9); }
.media-ctrl-btn svg { width: clamp(16px, 2vw, 28px); height: clamp(16px, 2vw, 28px); }
.tile.state-on .media-ctrl-btn { color: rgba(0,0,0,0.4); }
.tile.state-on .media-ctrl-btn:hover { color: rgba(0,0,0,0.8); }
.tile.has-art .media-ctrl-btn { color: rgba(255,255,255,0.6); }
.tile.has-art .media-ctrl-btn:hover { color: #fff; }
.tile-progress-wrap {
  position: absolute; bottom: 0; left: 0; right: 0; height: 4px; z-index: 3; cursor: pointer;
}
.tile-progress-track {
  position: absolute; bottom: 0; left: 0; right: 0; height: 100%;
  background: rgba(255,255,255,0.1);
}
.tile-progress-fill {
  height: 100%; background: rgba(255,255,255,0.5);
  border-radius: 0 2px 2px 0; transition: width 1s linear; position: relative;
}
.tile-progress-knob {
  position: absolute; right: -5px; top: 50%; transform: translateY(-50%);
  width: 10px; height: 10px; border-radius: 50%; background: #fff;
  box-shadow: 0 0 4px rgba(0,0,0,0.5); opacity: 0; transition: opacity 0.15s;
}
.tile-progress-wrap:hover .tile-progress-knob { opacity: 1; }
.tile.state-on .tile-progress-track { background: rgba(0,0,0,0.08); }
.tile.state-on .tile-progress-fill { background: rgba(0,0,0,0.25); }
.tile.state-on .tile-progress-knob { background: #333; }
.tile.has-art .tile-progress-fill { background: rgba(255,255,255,0.5); }
.tile.has-art .tile-progress-knob { background: #fff; }
.tile-progress-wrap:hover { height: 6px; }
.gvp-overlay {
  position: fixed; top: 0; left: 0; right: 0; bottom: 0;
  background: rgba(0,0,0,0.6); z-index: 1000;
  display: flex; align-items: center; justify-content: center;
  backdrop-filter: blur(6px); -webkit-backdrop-filter: blur(6px);
  animation: gvpFadeIn 0.2s ease;
}
@keyframes gvpFadeIn { from { opacity: 0; } to { opacity: 1; } }
.gvp-card {
  background: #1c1c1e; border-radius: 20px; padding: 24px 28px;
  min-width: 200px; max-width: 90vw;
  box-shadow: 0 8px 32px rgba(0,0,0,0.5);
}
.gvp-title {
  font-size: 13px; color: rgba(255,255,255,0.6);
  text-align: center; margin-bottom: 20px;
  overflow: hidden; text-overflow: ellipsis; white-space: nowrap;
}
.gvp-sliders {
  display: flex; gap: 20px; justify-content: center; align-items: stretch;
}
.gvp-slider-col {
  display: flex; flex-direction: column; align-items: center; gap: 8px;
  width: 44px;
}
.gvp-label {
  font-size: 11px; color: rgba(255,255,255,0.5);
  text-align: center; white-space: nowrap;
}
.gvp-track-wrap {
  height: 160px; width: 44px; position: relative;
}
.gvp-track-wrap .tile-slider-track {
  position: absolute; top: 0; left: 0; right: 0; bottom: 0;
  border-radius: 12px; overflow: hidden; cursor: pointer;
  background: rgba(255,255,255,0.08) !important;
  border: none !important;
}
.gvp-track-wrap .tile-slider-fill {
  position: absolute; bottom: 0; left: 0; right: 0;
  background: linear-gradient(180deg, rgba(94,92,230,0.7) 0%, rgba(94,92,230,0.35) 100%);
  border-radius: 0 0 12px 12px;
  transition: height 0.15s ease;
}
.gvp-pct {
  font-size: 11px; color: rgba(255,255,255,0.7);
  text-align: center;
}
.tile.type-media .tile-slider-track {
  background: rgba(255,255,255,0.08) !important;
  backdrop-filter: blur(12px); -webkit-backdrop-filter: blur(12px);
  box-shadow: inset 0 1px 0 rgba(255,255,255,0.1), 0 0 0 1px rgba(255,255,255,0.05) !important;
  border: none !important;
}
.tile.type-media .tile-slider-fill {
  background: linear-gradient(180deg, rgba(255,255,255,0.35) 0%, rgba(255,255,255,0.15) 100%);
}
.tile.state-on.type-media .tile-slider-track {
  background: rgba(0,0,0,0.1) !important;
}
.tile.state-on.type-media .tile-slider-fill {
  background: linear-gradient(180deg, rgba(94,92,230,0.5) 0%, rgba(94,92,230,0.25) 100%);
}
.tile.tile-wide { grid-column: span 2; overflow: hidden; padding: 4% !important; }
.tile.tile-wide .tile-icon-circle { width: clamp(var(--icon-min), 11%, var(--icon-max)); }
.tile.tile-wide .tile-bottom { max-width: 45%; }
.tile.tile-wide .tile-slider-wrap { left: 75% !important; right: 4% !important; top: 8% !important; bottom: 8% !important; }
`
});

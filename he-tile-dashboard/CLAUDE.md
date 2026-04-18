# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

Hubitat tile dashboard — a standalone, vanilla JavaScript SPA with a HomeKit-style neumorphic UI for controlling smart home devices via the Hubitat Maker API. No build tools, no frameworks, no package manager. Served from Hubitat's File Manager (`http://<hub-ip>/local/dashboard.html`) or any web server.

## Persona — UltronOfSpace voice (scoped)

The parent Hubitat repo uses the **UltronOfSpace** persona for public-facing writing. This sub-project applies it on a **scoped** basis — the codebase enforces NASA Power of 10 + code style via `/review` and `/ship`, which would conflict with voice-in-code.

**Full style guide:** [`../.claude/PERSONA.md`](../.claude/PERSONA.md)

**Scope for this sub-project:**
- **Voiced (10/10):** `README.md` when present — full Ultron voice, same rules as the main repo README
- **Small cameo (2/10):** Settings/About modal in the dashboard UI — persona nod only (e.g., credit line *"Built by Ultronumus Of Space — may he nap in peace"* or a version-string flavor line), never in operational UI strings
- **Neutral (0/10):** this CLAUDE.md, code, comments, variable names, dashboard UI strings (tile labels, error messages, settings panel copy), `manifest.json` display name

**One-line rule:** Ultron lives on the front porch (README + Settings cameo), not inside the house (code, UI, engineering docs).

## Architecture

**Five layers:**

1. **`dashboard-test.html`** — App shell, layout (sidebar + room tabs + tile grid), localStorage config persistence (`loadConfig()`/`saveConfig()`), navigation state, and the `render()` function as a global consumed by all tiles. Contains `stateCache`, `dimLocks`, and `data` (built from config). Inline `<script>` block (lines 21–253) defines globals and core functions; external scripts loaded after.

2. **`hubitat-api.js`** — Hubitat adapter layer. Handles Maker API calls (`sendCommand()`, `fetchAllDevices()`, `getDevice()`), EventSocket real-time connection, state normalization (translates Hubitat device attributes into stateCache format), device type detection from capabilities, and brightness/speed/volume scale conversion.

3. **`tile-engine.js`** — Core engine providing the plugin registry (`TileEngine.register()`), state access helpers (`state()`, `attr()`, `lock()`), Hubitat command calls (`callService(deviceId, command, param)`), standard tile template (`renderStandard()`), slider/brightness/volume infrastructure, animation loops (RequestAnimationFrame), and responsive auto-sizing for icons, text, and room tabs.

4. **`tiles/*.js`** — Plugin modules. Each file registers one or more tile types via `TileEngine.register(type, { icon, priority, formatState, isOn, isSensor, isAlert, render, toggle, css })`.

5. **`config-ui.js`** — Settings panel (gear icon) for hub connection setup, device discovery, and floor/room/entity management. All config stored in localStorage.

**Script load order** (matters — no module system):
`dashboard-test.html` (inline globals) → `hubitat-api.js` → `tile-engine.js` → `tiles/*.js` (any order) → `config-ui.js` → `initTileEngine()` + `init()`

**Tile type categories:**
- **Toggle tiles** (light, switch, cover, camera): On/off with optional brightness slider
- **Control tiles** (fan): Multi-button speed control (H/M/L) with rotation animation
- **Sensor tiles** (sensor, climate, temp-humidity, ups): Display-only, `isSensor: true`
- **Alert tiles** (door, motion): Orange state when triggered, `isAlert` returns true
- **Complex tiles** (media): 2-column span, playback controls, volume slider, group speaker popup

**State flow:** Bulk Maker API fetch on load → EventSocket subscription for device events → `HubitatAPI.normalizeEvent()` → update `stateCache` → re-render. Dim locking (`dimLocks`) prevents EventSocket updates from overriding active user slider interactions for 10 seconds.

**Hubitat API:**
- Bulk state: `GET /apps/api/{appId}/devices/all?access_token=...`
- Commands: `GET /apps/api/{appId}/devices/{deviceId}/{command}/{param}?access_token=...` (GET, not POST)
- Real-time: EventSocket at `ws://<hub>/eventsocket` (no auth, auto-streams all device events)
- Brightness: Hubitat uses 0-100 "level"; adapter converts to/from 0-255 at the boundary
- Volume: Hubitat uses 0-100 integer; adapter converts to/from 0.0-1.0
- Fan speed: Named speeds (low/medium-low/medium/medium-high/high) mapped to/from percentage
- Device IDs: Numeric integers (string keys in stateCache)

**Grid layout:** `calcGrid()` computes optimal columns. Non-media tiles flow normally; media tiles are positioned from the bottom-right corner with spacers filling gaps.

## Key Globals (defined in dashboard-test.html, used everywhere)

- `stateCache` — Entity state object. Keys: `deviceId` (state string), `deviceId_brightness`, `deviceId_volume`, etc.
- `dimLocks` — Tracks locked entities to prevent EventSocket race conditions during slider drags
- `data` — Floor/room/entity configuration object, built from localStorage config
- `render()` — Full dashboard re-render function
- `TileEngine` — Central registry and helper object
- `HubitatAPI` — Hubitat adapter (connection, commands, normalization)

## Config Persistence

All configuration stored in `localStorage` under key `hubitat-dashboard-config`:
```json
{
  "version": 1,
  "hub": { "url": "http://192.168.1.20", "appId": "123", "token": "xxx" },
  "floors": { "floor_id": { "name": "Floor", "rooms": { "room_id": { "name": "Room", "entities": [...] } } } },
  "homeRoom": "room_id",
  "floorNames": {}
}
```

Hub connection can also be passed via URL params: `?hub=http://...&appId=123&token=xxx`

## Development

No build step. Open `dashboard-test.html` in a browser. On first load with no config, the settings panel guides hub connection setup. All JS files are loaded via `<script>` tags. Edit and refresh.

**Deployment:** Upload all files to Hubitat's File Manager (Settings → File Manager). Access at `http://<hub-ip>/local/dashboard.html`. Target runtime includes wall-mounted Android tablets running Fully Kiosk Browser.

## Coding Standards

This project enforces **NASA Power of 10** coding rules and a custom code style. Use `/review` to check compliance before committing, and `/ship` to bump version, review, commit, and push.

## Slash Commands

- `/review` — Review code for NASA Power of 10 compliance and code style
- `/ship` — Bump version, review code for compliance, commit, and push to GitHub
- `/dashboard-architecture` — Load architecture context (state flow, plugin contract, error handling)
- `/dashboard-visuals` — Load visual/animation engineering context (animation system, race conditions, critical pitfalls, design system)

# Hubitat Elevation HTTP Endpoints — Community Reference

> **Tested on:** C-8 Pro (firmware 2.4.4.156), C-4 (firmware 2.4.3.103 and 2.5.0.117 beta)
> **Date:** April 2026
> **Base URL:** `http://<HUB_IP>` — replace `<HUB_IP>` with your hub's IP address (e.g., `http://192.168.1.100`)

## What Is This?

Your Hubitat hub has a built-in web server that powers the admin UI you see in your browser. Behind that UI are hundreds of **HTTP endpoints** — URLs you can call directly to read data from your hub or tell it to do things, without clicking through the web interface.

An **endpoint** is just a URL path on your hub (like `/device/list/data`) that returns data or performs an action when you visit it.

### How to Build a URL

Every endpoint in this document is written as a **path** starting with `/`. To use it, put your hub's address in front:

```
http://<YOUR_HUB_IP>/<endpoint path>
```

For example, if your hub's IP is `192.168.1.100` and the endpoint is `/device/list/data`:
```
http://192.168.1.100/device/list/data
```

You can also use `hubitat.local` instead of the IP address if your network supports mDNS (most do):
```
http://hubitat.local/device/list/data
```

If you have multiple hubs, each has a unique name (e.g., `hubitat-sandbox.local`). You can find your hub's IP on the Settings page or by checking your router's device list.

### Who Is This For?

You can use these endpoints from:
- **Your browser's address bar** — type the full URL to see what comes back
- **Scripts and automations** — use `curl`, Python, Node.js, or any HTTP client
- **Third-party integrations** — tools like Home Assistant, Node-RED, or custom dashboards

This document catalogs every known endpoint, what it does, what data it returns, and whether it's safe to call.

**How these were discovered:** The hub's web interface is built with JavaScript files that contain all the URL patterns the UI uses. We extracted those patterns from the JS source code and then tested each one against real hubs to verify behavior and document responses.

---

## ⚠️ CRITICAL SAFETY WARNING

**Many endpoints trigger real actions when you simply visit the URL — there is no confirmation dialog or undo.** Unlike most web applications, Hubitat executes actions on plain **GET requests** (the kind your browser makes when you type a URL or click a link). There is no **CSRF protection** (a security mechanism that would normally prevent accidental actions).

This means automated tools, scripts, or even your browser pre-loading links can accidentally reboot your hub, disable radios, or trigger firmware updates. Endpoints marked with 🔴 below **will execute an action** when visited. Do not open them unless you intend to trigger that action.

---

## Table of Contents

1. [WebSocket Endpoints (Live Streaming)](#1-websocket-endpoints-live-streaming)
2. [Hub Identity & Status](#2-hub-identity--status)
3. [Hub System Info (Plain Text)](#3-hub-system-info-plain-text)
4. [Device Endpoints](#4-device-endpoints)
5. [App Code Endpoints](#5-app-code-endpoints)
6. [Driver Code Endpoints](#6-driver-code-endpoints)
7. [Installed App Endpoints](#7-installed-app-endpoints)
8. [hub2/ Vue SPA API](#8-hub2-vue-spa-api)
9. [Location & Modes](#9-location--modes)
10. [Rooms](#10-rooms)
11. [Logs](#11-logs)
12. [Dashboard Endpoints](#12-dashboard-endpoints)
13. [Libraries & Bundles](#13-libraries--bundles)
14. [Zigbee Management](#14-zigbee-management)
15. [Z-Wave Management](#15-z-wave-management)
16. [Z-Wave 2 (Dual Radio/Antenna)](#16-z-wave-2-dual-radioantenna)
17. [Matter](#17-matter)
18. [Hub Advanced / Admin](#18-hub-advanced--admin)
19. [Hub Cloud & Updates](#19-hub-cloud--updates)
20. [Hub Settings & UI](#20-hub-settings--ui)
21. [Network Configuration](#21-network-configuration)
22. [File Manager](#22-file-manager)
23. [TTS (Text-to-Speech)](#23-tts-text-to-speech)
24. [User Management](#24-user-management)
25. [Code Publishing](#25-code-publishing)
26. [Mobile API & SmartStart](#26-mobile-api--smartstart)
27. [Maker API (Token Required)](#27-maker-api-token-required)
28. [Port 8081 (Diagnostic Tool)](#28-port-8081-diagnostic-tool)
29. [Onboarding & Setup Wizard](#29-onboarding--setup-wizard)
30. [Miscellaneous](#30-miscellaneous)
31. [Confirmed Non-Existent Endpoints (404)](#31-confirmed-non-existent-endpoints-404)

---

## How to Read This Document

### Safety Indicators

Every endpoint is marked with a colored circle showing whether it's safe:

| Symbol | Meaning | Example |
|--------|---------|---------|
| 🟢 | **Safe** — only reads data, changes nothing | Getting your device list |
| 🟡 | **Mostly safe** — reads data but kicks off a background task (like a scan) | Starting a Zigbee channel scan |
| 🔴 | **Action** — changes something on your hub. Don't call unless you mean it! | Rebooting, deleting a device, toggling a radio |

### URL Notation

| Notation | What It Means | Example |
|----------|---------------|---------|
| `{id}` | A placeholder — replace with an actual value | `/device/json/{id}` → `/device/json/42` |
| `?param=value` | A **query parameter** — extra info appended to the URL after a `?` | `/hub/updateName?name=MyHub` |
| POST | The endpoint requires a **POST request** (sending data to the hub), not just visiting the URL. Use `curl -X POST` or similar. | `curl -X POST http://hub/hub/reboot` |

### The "Type" Column

Every endpoint table has a **Type** column that tells you two things: what HTTP method to use, and what format the response comes back in.

**Response formats** (what you get back):
- **JSON** — structured data like `{"name": "Living Room", "id": 1}`. This is what scripts and integrations consume. **JSON** (JavaScript Object Notation) is the standard data format for web APIs.
- **HTML** — a full web page with the Hubitat UI. These are the same pages you see when clicking around in the hub's admin interface.
- **Text** — a simple plain-text value like a number (`674272`) or a word (`true`, `done`).
- **File** — a downloadable file (backup, export, etc.)
- **WebSocket** — a persistent live-streaming connection (not a normal HTTP request)

**Request methods** (how to call it):
- **GET** — the default. Just visit the URL in your browser or use `curl http://hub/path`. Most endpoints use GET.
- **POST** — you must send data TO the hub. You can't just visit the URL — use `curl -X POST` or a script. When you see POST in the Type column, the endpoint also notes what **format** to send the data in:
  - **POST (JSON body)** — send data as JSON with a `Content-Type: application/json` header. Example: `curl -X POST -H "Content-Type: application/json" -d '{"id":null,"source":"..."}' http://hub/path`
  - **POST (form body)** — send data as form fields with `Content-Type: application/x-www-form-urlencoded`. Example: `curl -X POST -d "name=value&other=value" http://hub/path`
  - **POST (multipart)** — send files using multipart form data. Example: `curl -X POST -F "file=@myfile.txt" http://hub/path`

> **Why does this matter?** Sending data in the wrong format causes silent failures. For example, `/app/saveOrUpdateJson` requires a JSON body — if you send form data instead, it returns `{"success":false,"message":"Unknown error occurred"}` with no further explanation.

---

## Hub Model & Firmware Compatibility

Most endpoints work the same regardless of which hub model you have. However, some features are hardware-specific (e.g., WiFi is only on certain models, Z-Wave 2 is only on dual-radio hubs). The table below lists the known differences:

| Endpoint | C-8 Pro | C-4 | Notes |
|----------|---------|-----|-------|
| `/hub/cpuInfo` | `Processors 4 Load Average 0.15` | 404 | Hardware-specific |
| `/hub/advanced/zipgatewayVersion` | `7.18.3` | `Unsupported` | C-4 lacks Z-Wave 700 gateway |
| `/hub/advanced/installDriver/all` | Works | `Hub not compatible` | |
| `/hub/advanced/getWiFiNetworkInfoAsyncStatus` | Works | `Hub not compatible` | C-4 has no WiFi |
| `/hub/publishCode/status` | JSON | 404 | Not available on firmware 2.4.3 |
| `/hub/zwaveLogs` | HTML page | 404 | C-4 legacy Z-Wave stack |
| `/hub/zwaveTopology` | HTML page | 500 | C-4 legacy Z-Wave stack |
| `/hub/zwaveRepair2Status` | JSON | 404 | Z-Wave JS only (C-7+) |
| `/hub/zwave/securityKeys` | HTML page | 404 | Not on C-4 |
| `/hub/zwave/securityCode` | HTML page | 404 | Not on C-4 |
| `/hub/zwave2/*` | Functional | 404 or error | Dual-radio only (C-8 Pro) |
| `/hub/matterDetails/json` | `installed: true` | `installed: false` | Matter not on C-4 |
| `/hub/advanced/freeOSMemoryHistory` | 6-column CSV | 5-column CSV | C-4 lacks "Direct Java" column |
| `/hub2/availableWiFiNetworks` | Lists SSIDs | Empty `[]` | C-4 has no WiFi hardware |
| `/hub2/setSetting/chatbot/` | Works | Not in JS | New chatbot toggle (2.4.4+) |
| `/app/createVisualRuleBuilderRule` | Works | Not in JS | Visual Rule Builder (2.4.4+) |
| `/app/ruleBuilderSuggestions` | Works | Not in JS | Rule Builder suggestions (2.4.4+) |
| `/hub/zwave2/startAntennaTest` | Works | 404 | C-8 Pro dual-antenna testing |
| `/hub/matter/openPairingWindow?node=` | Works | Not in JS | Matter commissioning (2.4.4+) |
| `/hub/updateLatLongTimezone` | Works | Uses `updateLatLogTimezone` | Typo fix in 2.4.4 |

> **What changed between firmware versions?** We compared the JavaScript source files across three firmware versions to find new and removed endpoints. Firmware 2.4.4 added 20 new endpoints (Visual Rule Builder, Z-Wave antenna testing, Matter commissioning, chatbot settings). **Firmware 2.5.0 (beta) adds ~30 more** including MQTT device import, Z-Wave over-the-air device firmware updates, Matter logging, hub variable mesh sharing, room occupancy lighting, and past log export. Endpoints marked with "(2.5.0 beta+)" below are only available on firmware 2.5.0 and newer.

---

## 1. WebSocket Endpoints (Live Streaming)

**WebSockets** are persistent connections that let your hub push live data to you in real-time (unlike normal HTTP where you have to ask for data each time). These are commonly used by dashboard apps and monitoring tools to get instant device updates.

To use these, you need a WebSocket client (built into browsers and most programming languages). A plain browser visit to these URLs won't work — they require a special "upgrade" handshake. No authentication is needed from your local network (**LAN** — the devices connected to your home router).

| Endpoint | Type | Description |
|----------|------|-------------|
| 🟢 `/eventsocket` | WebSocket | Streams device events as JSON objects in real-time |
| 🟢 `/logsocket` | WebSocket | Streams log messages as JSON in real-time |
| 🟢 `/zigbeeLogsocket` | WebSocket | Streams Zigbee radio log messages |
| 🟢 `/zwaveLogsocket` | WebSocket | Streams Z-Wave radio log messages |

> **C-4 note:** On the C-4, only `/eventsocket` returned a 101 Upgrade response during testing. The other three sockets timed out — they may require active radio logging to respond.

**Example (JavaScript):**
```javascript
const ws = new WebSocket("ws://192.168.1.100/eventsocket");
ws.onmessage = (e) => console.log(JSON.parse(e.data));
```

---

## 2. Hub Identity & Status

All return JSON. No authentication required.

| Endpoint | Type | Description |
|----------|------|-------------|
| 🟢 `/hub/details/json` | JSON | Hub identity: name, zip, lat/long, platform version, hardware version, hubUID, MAC, mDNS name, sunrise/sunset, timezone |
| 🟢 `/hub/list/data` | JSON | Hub hardware info: hardwareID, zigbeeEui, zigbeeChannel, zigbeePanID, localIP, localSrvPortTCP |
| 🟢 `/hub/data` | JSON | Hub identity + network: `{id, name, version, locationId, data: {hardwareID, zigbeeEui, zigbeeChannel, zigbeePanID, localSrvPortTCP, localIP}}` — singular-object variant of `/hub/list/data` that also wraps in a top-level `{id, name, version, locationId}` envelope |
| 🟢 `/hub/messages` | JSON | Current mode (id, name, icon), HSM status, hub alert messages |
| 🟢 `/hub/echoDiscovery` | JSON | Amazon Echo discovery status |
| 🟢 `/hubStatus` | JSON | Hub init status: serverInitPercentage, serverInitDetails, status |
| 🟢 `/hub/serverInitPercentage` | JSON | Single value: `{"serverInitPercentage": 100}` |
| 🟢 `/hub/isTOSAccepted` | Text | Returns plain text `true` or `false` (not JSON-wrapped) |
| 🟢 `/hub/rebootingJson` | JSON | Hub reboot status: `{hubId, ipAddress, version, registered}` — `registered` is `false` on unregistered hubs |
| 🟢 `/hub/backup/statusJson` | JSON | Backup in-progress status |
| 🟢 `/location/list/data` | JSON | Location: name, temperatureScale, timeZone, zipCode, lat, lon, sunrise, sunset, currentMode |
| 🟢 `/location/data` | JSON | Same as above plus modes list and hubs array |

<details>
<summary><b>Sample — /hub/details/json</b></summary>

```json
{
  "hubName": "<your-hub-name>",
  "zipCode": "<your-zip>",
  "latitude": <lat>,
  "longitude": <long>,
  "mdnsName": "hubitat",
  "platformVersion": "2.4.4.156",
  "hardwareVersion": "Rev C-8 Pro",
  "hubUID": "<your-hub-uid>",
  "macAddress": "<hub-mac>",
  "currentTime": "2026-04-16T01:20:50+0000",
  "sunrise": "2026-04-15T11:54:00+0000",
  "sunset": "2026-04-16T00:48:00+0000",
  "timeZone": "<your-timezone>"
}
```
</details>

<details>
<summary><b>Sample — /hub2/hubData</b></summary>

```json
{
  "hubId": "<your-hub-uid>",
  "ipAddress": "<hub-ip>",
  "version": "2.4.4.156",
  "model": "C-8 Pro",
  "name": "<your-hub-name>",
  "alerts": {
    "hubLoadElevated": false,
    "hubLoadSevere": false,
    "hubHighLoad": false,
    "hubLowMemory": false,
    "hubZwaveCrashed": false,
    "hubLargeishDatabase": false,
    "hubLargeDatabase": false,
    "zwaveOffline": false,
    "zigbeeOffline": false,
    "cloudDisconnected": false,
    "platformUpdateAvailable": false,
    "spammyDevicesMessage": null,
    "databaseSize": 5
  },
  "freshDatabase": false,
  "safeMode": false,
  "showGetStarted": false,
  "showConnectDevices": false,
  "showDeveloperMenu": false,
  "maintainRoomDashboards": false,
  "includeHiddenDevicesInSummary": false,
  "excludeVirtualDevicesFromSummary": false,
  "zwaveJSInstalled": false
}
```

The `alerts` sub-object is the source for all UI warning banners. `baseModel` (nested deeper) also includes `dashboard.accessToken`, `devMode`, `clock`, `zigbeeStatus`, `zwaveStatus`, and `ttsFriendly`.
</details>

<details>
<summary><b>Sample — /hub/messages</b></summary>

```json
{
  "messages": [],
  "mode": {"locationId": 1, "id": 2, "icon": "fa-sunset", "name": "Evening"},
  "hsm": null,
  "hsmAlert": null
}
```
</details>

<details>
<summary><b>Sample — /hubStatus</b></summary>

```json
{
  "serverInitPercentage": "Initializing Hub: 100%",
  "serverInitDetails": "preparing UI",
  "status": "running"
}
```
</details>

---

## 3. Hub System Info (Plain Text)

Return plain text values despite `text/html` content-type headers. No auth required.

| Endpoint | Returns | Example |
|----------|---------|---------|
| 🟢 `/hub/advanced/freeOSMemory` | Free OS memory in KB | `914716` |
| 🟢 `/hub/advanced/freeOSMemoryHistory` | CSV: Date/time, Free OS, 5m CPU avg, Total/Free/Direct Java (~3.7 days at 5-min intervals) | See below |
| 🟢 `/hub/advanced/freeOSMemoryLast` | Latest single row of above CSV | See below |
| 🟢 `/hub/advanced/internalTempCelsius` | Hub temp in °C | `38.2` (C-8 Pro) / `63.0` (C-4) |
| 🟢 `/hub/advanced/databaseSize` | Database size in MB | `6` |
| 🟢 `/hub/advanced/event/limit` | Current max event count | `Maximum event count: [11]` |
| 🟢 `/hub/advanced/zipgatewayVersion` | Z-Wave gateway firmware version | `7.18.3` (C-8 Pro) / `Unsupported` (C-4) |
| 🟢 `/hub/advanced/network/lanautonegconfigstatus` | LAN auto-negotiation status | `autoneg` |
| 🟢 `/hub/zwaveVersion` | Z-Wave SDK version | `7.22` (C-8 Pro) / `No Z-Wave controller instance is available` (C-4 after radio cycle) |
| 🟢 `/hub/cpuInfo` | Processor count and load average | `Processors 4 Load Average 0.15` — **404 on C-4** |

<details>
<summary><b>Sample — /hub/advanced/freeOSMemoryHistory</b></summary>

**C-8 Pro (6 columns):**
```
Date/time,Free OS,5m CPU avg,Total Java,Free Java,Direct Java
04-12 03:20:19,1397100,0.73,134796,25508,20748
04-12 03:25:19,1209796,1.2,174384,5897,22091
...
```

**C-4 (5 columns — no Direct Java):**
```
Date/time,Free OS,5m CPU avg,Total Java,Free Java
04-15 17:16:50,902124,1.15,221312,151339
04-15 17:21:51,856100,0.96,221312,135291
...
```
</details>

---

## 4. Device Endpoints

### Read-Only (JSON Data)

| Endpoint | Type | Description |
|----------|------|-------------|
| 🟢 `/device/list/data` | JSON | All devices: id, label, zigbeeId, deviceTypeName, status, disabled, roomAssigned, type (sys/usr) |
| 🟢 `/device/listJson` | JSON | Returns `[]` on current firmware — legacy/vestigial endpoint. Use `/device/list/data` instead. |
| 🟢 `/device/list/all/data` | JSON | All devices (extended variant) |
| 🟢 `/device/json/{id}` | JSON | Device summary: displayName, driverType, zigbeeId, currentStates, deviceNetworkId |
| 🟢 `/device/fullJson/{id}` | JSON | Rich detail: apps using device, commands with arguments, dashboardTypes, scheduledJobs |
| 🟢 `/device/eventsJson/{id}` | JSON | Device event history: name, value, unit, source, isStateChange, date |
| 🟢 `/device/listWithCapabilities/json` | JSON | All devices with capability lists and temperature |
| 🟢 `/device/sysDriverByIdJson/{id}` | JSON | System driver metadata for a device |
| 🟢 `/device/drivers` | JSON | All installed drivers (system + user) with capabilities, commands, attributes |
| 🟢 `/device/accessibleLinkedDevices` | JSON | Hub mesh linked devices |
| 🟢 `/device/availableTags` | JSON | Available device tags |
| 🟢 `/device/getReplacementOptions/{id}` | JSON | Replacement device options |
| 🟢 `/device/instructionSearch` | JSON | Device instruction/pairing search |
| 🟢 `/device/instructionSearch?show=all` | JSON | All device instructions |
| 🟢 `/device/showHubMeshToken` | JSON | Current hub mesh token |

<details>
<summary><b>Sample — /device/json/{id}</b></summary>

```json
{
  "deviceTypeId": 638,
  "displayName": "Elena's Table Lamp",
  "groupId": null,
  "label": "Elena's Table Lamp",
  "lanId": null,
  "version": 4,
  "driverType": "sys",
  "zigbeeId": "B0CE18140017830B",
  "currentStates": {},
  "parentDeviceId": null,
  "name": "Sengled Element Color Plus",
  "id": 2,
  "deviceNetworkId": "E46D"
}
```
</details>

<details>
<summary><b>Sample — /device/listWithCapabilities/json</b></summary>

```json
[
  {
    "id": 6,
    "label": "Kitchen Outlet",
    "capabilities": ["Configuration", "Actuator", "Refresh", "PowerMeter", "Outlet", "Switch", "Sensor"],
    "temperature": null
  }
]
```
</details>

### Read-Only (HTML Pages)

| Endpoint | Type | Description |
|----------|------|-------------|
| 🟢 `/device/list` | HTML | Device list page |
| 🟢 `/device/edit/{id}` | HTML | Device edit page |
| 🟢 `/device/events/{id}` | HTML | Device events page |
| 🟢 `/device/hubMesh` | HTML | Hub mesh devices page |
| 🟢 `/device/addDevice` | HTML | Add device page |
| 🟢 `/device/addDevice?type=zigbee` | HTML | Add Zigbee device |
| 🟢 `/device/addDevice?type=zwave` | HTML | Add Z-Wave device |
| 🟢 `/device/addDevice?type=matter` | HTML | Add Matter device |

### Action Endpoints

| Endpoint | Type | Description |
|----------|------|-------------|
| 🔴 `/device/save` | POST | Save device |
| 🔴 `/device/update` | POST | Update device |
| 🔴 `/device/preference/save` | POST | Save device preferences |
| 🔴 `/device/runmethod` | POST (form body) | Execute a device command — body: `id={deviceId}&method={commandName}` |
| 🔴 `/device/disable` | POST (form body) | Disable/enable device — body: `id={deviceId}&disable={true/false}` |
| 🔴 `/device/updateLabel?id={id}&label={label}` | GET → Text | Update device label — **returns `false` via API; may need different approach** |
| 🔴 `/device/updateRoom?id={id}&room={roomId}` | GET | Update device room |
| 🔴 `/device/setRoom?deviceId={id}&roomId={id}` | GET | Set device room |
| 🔴 `/device/setShowOnHome?deviceId={id}&show={bool}` | GET | Toggle show-on-home |
| 🔴 `/device/setDefaultCurrentState?id={id}&currentState={state}` | GET | Set default current state |
| 🔴 `/device/createVirtual?deviceTypeId={driverId}` | GET → JSON | Create a virtual device — param is the numeric driver type ID from `/device/drivers`. Returns `{"success": true, "deviceId": <id>}`. **Note:** Using wrong param names (e.g., `driver=Virtual Switch`) returns `deviceId: 0` — always use `deviceTypeId`. |
| 🔴 `/device/createLinked/{hubId}/{deviceId}` | GET | Create linked device from hub mesh |
| 🔴 `/device/replace?oldId={id}&newId={id}` | GET | Replace device |
| 🔴 `/device/forceDelete/{id}/json` | GET → JSON | **Force delete device** |
| 🔴 `/device/addToMesh/{id}` | GET | Add device to hub mesh |
| 🔴 `/device/removeFromMesh/{id}` | GET | Remove device from hub mesh |
| 🔴 `/device/generateHubMeshToken` | GET | Generate new mesh token |
| 🔴 `/device/setHubMeshToken` | POST | Set hub mesh token |
| 🔴 `/device/hubMeshFullRefreshNow` | GET | Trigger full mesh refresh |
| 🔴 `/device/hubMeshReconnect` | GET | Reconnect hub mesh |
| 🔴 `/device/setHubMeshFullRefreshInterval/{seconds}` | GET | Set mesh refresh interval |
| 🔴 `/device/followModes/{modeHubId}` | GET | Follow modes from hub |
| 🔴 `/device/migrateAllLinkedDevices?sourceHub={id}&targetHub={id}` | GET | Migrate all linked devices |
| 🔴 `/device/zwave/deleteStatus/{id}` | GET | Delete Z-Wave device status |

### MQTT Device Import (2.5.0 beta+)

| Endpoint | Type | Description |
|----------|------|-------------|
| 🟢 `/device/mqttImport/sourcesJson` | JSON | List available MQTT sources |
| 🟢 `/device/mqttImport/sourceTopicsJson?{params}` | JSON | List topics for a source |
| 🟢 `/device/mqttImport/capabilitiesJson` | JSON | Available capabilities for MQTT devices |
| 🟢 `/device/mqttImport/manualDeviceJson?{params}` | JSON | Preview manual MQTT device creation |
| 🔴 `/device/mqttImport/saveManualDevice` | POST | Save a manually configured MQTT device |

---

## 5. App Code Endpoints

| Endpoint | Type | Description |
|----------|------|-------------|
| 🟢 `/app/list` | HTML | App code list page |
| 🟢 `/app/list/data` | JSON | Parent/standalone app types only — fields: id, version, author, category, description, name, namespace, type (sys/usr), parentAppId, singleInstance, singleInstanceInstalledId. **Child apps (non-null `parentAppId`) are silently filtered out** — see notes below. |
| 🟢 `/app/list/single/data/{id}` | JSON | Single app type data |
| 🟢 `/app/editor/{id}` | HTML | App code editor page |
| 🟢 `/app/create` | HTML | Create new app page |
| 🟢 `/app/ajax/code?id={id}` | JSON | **Full Groovy source code** — `{id, name, version, source}`. System apps return empty `source`. |
| 🟢 `/app/downloadFull/{id}` | File | Download full app export |
| 🟢 `/app/ruleBuilderJson/{id}` | JSON | Rule builder data for an installed app |
| 🟢 `/app/ruleBuilderSuggestions` | JSON | Rule builder suggestions |
| 🔴 `/app/saveOrUpdateJson` | POST (JSON body) | Save/update app code (see notes below) |
| 🔴 `/app/deleteAppType/{id}` | GET | Delete app type |
| 🔴 `/app/edit/deleteJson/{id}` | GET | Delete app (JSON response) |
| 🔴 `/app/ruleBuilderPause/{id}/true` | GET | Pause a rule |
| 🔴 `/app/ruleBuilderPause/{id}/false` | GET | Resume a rule |
| 🔴 `/app/ruleBuilderGenerateRule?{params}` | GET | Generate a rule |
| 🔴 `/app/createVisualRuleBuilderRule` | GET | Create visual rule |
| 🔴 `/app/updateOAuth?{params}` | GET | Update OAuth settings |

<details>
<summary><b>Sample — /app/ajax/code?id=139</b></summary>

```json
{
  "id": 139,
  "name": "Zen32 LED Coordinator",
  "version": 6,
  "source": "/*\n * License: MIT\n * Author: ...\n */\ndefinition(\n    name: \"Zen32 LED Coordinator\",\n..."
}
```
</details>

> **`saveOrUpdateJson` — correct usage:** POST with `Content-Type: application/json`. Body is a JSON object: `{"id": null, "source": "<groovy source>", "version": 1}`. For updates, set `id` to the existing app ID (you can also omit the `id` key entirely for new apps — the UI does this). Returns `{"success": true, "id": <newId>, "version": <ver>}` on success. **Do NOT use form-urlencoded** — that returns `{"success": false, "message": "Unknown error occurred"}`. The browser UI sends this via `postJsonAndCallback()` using `JSON.stringify()`.
>
> **Groovy `definition()` requirements:** the `iconUrl` and `iconX2Url` keys **must be present** (empty strings `""` are fine, but the keys can't be missing) — the server returns `{"success": false, "message": "iconUrl,iconX2Url cannot be empty in definition section"}` if either key is absent.
>
> **🐛 UTF-8 BOM gotcha:** if the request body starts with a UTF-8 BOM (`EF BB BF`), the server's JSON parser silently fails and returns the generic `{"success": false, "message": "Unknown error occurred"}` — with no indication that the BOM is the cause. This masks all real errors (missing fields, Groovy compile errors, etc.). PowerShell's `Set-Content -Encoding UTF8` and `Out-File -Encoding UTF8` both add a BOM by default. Use `[System.IO.File]::WriteAllText($path, $body, (New-Object System.Text.UTF8Encoding $false))` on Windows PowerShell 5.1, or `Out-File -Encoding utf8NoBOM` on PowerShell 7+. Bash `echo >` and `cat > file << EOF` do not add a BOM. Confirmed on C-8 Pro FW 2.4.4.156 and C-4 FW 2.4.3.103.
>
> **Delete endpoints:** Use `/app/edit/deleteJson/{id}` for a JSON response (`{"status": true}`). `/app/deleteAppType/{id}` is a separate endpoint for deleting the app type definition.
>
> **`/app/list/data` hides child apps.** The endpoint silently drops any app whose `parentAppId` is not null, so child-type apps (those instantiated by a parent app's `app()` directive) never appear in the list — even though the web UI at `/app/list` shows them. The UI builds its list from `/hub2/userAppTypes` instead. Confirmed on C-8 Pro FW 2.4.4.156: the UI showed 6 user apps (2 parents + 2 children + 2 standalones), but `/app/list/data` returned only the 4 non-child rows. **Recommended workaround:** use `/hub2/userAppTypes` (see section 8) — it returns ALL user apps including children, and it's what the UI itself uses. `/app/list/single/data/{id}` and `/app/ajax/code?id={id}` also return child data once you have the ID.

---

## 6. Driver Code Endpoints

| Endpoint | Type | Description |
|----------|------|-------------|
| 🟢 `/driver/list` | HTML | Driver code list page |
| 🟢 `/driver/list/data` | JSON | All driver types: id, version, name, namespace, author, type (sys/usr), category |
| 🟢 `/driver/list/single/data/{id}` | JSON | Single driver type data |
| 🟢 `/driver/editor/{id}` | HTML | Driver code editor page |
| 🟢 `/driver/create` | HTML | Create new driver page |
| 🟢 `/driver/ajax/code?id={id}` | JSON | **Full Groovy source code** — system drivers return empty `source` |
| 🟢 `/driver/downloadFull/{id}` | File | Download full driver export |
| 🔴 `/driver/saveOrUpdateJson` | POST (JSON body) | Save/update driver code (same JSON body format as app version — see app section notes) |
| 🔴 `/driver/deleteDeviceType/{id}` | GET | Delete driver type |
| 🔴 `/driver/editor/deleteJson/{id}` | GET | Delete driver (JSON response) |

---

## 7. Installed App Endpoints

### Read-Only

| Endpoint | Type | Description |
|----------|------|-------------|
| 🟢 `/installedapp/list` | HTML | Installed apps page. In 2.5.0+: `?section=apps`, `?section=automations`, `?section=integrations` filter by category |
| 🟢 `/installedapp/list/data` | JSON | All installed app instances with full metadata and appType info |
| 🟢 `/installedapp/statusJson/{id}` | JSON | **Richest installed app endpoint** — includes appSettings (with device lists, booleans, enums), appState (including OAuth access tokens), event subscriptions, scheduled jobs, child apps/devices |
| 🟢 `/installedapp/json/{id}` | JSON | Installed app JSON |
| 🟢 `/installedapp/eventsJson/{id}` | JSON | Installed app events as JSON |
| 🟢 `/installedapp/sysAppByIdJson/{id}` | JSON | System app info by ID |
| 🟢 `/installedapp/configure/{id}` | HTML | App configuration page |
| 🟢 `/installedapp/configure/{id}?embed=true` | HTML | Embedded app config |
| 🟢 `/installedapp/configure/json/{id}` | JSON | **Configure page structure as JSON (2.5.0+ only)** — requires `Accept: application/json` header. Returns `{pageBreadcrumbs, formAction, removeButton, configPage: {name, title, sections: [{input: [{description, multiple, title, ...}], ...}], install, uninstall, refreshInterval, ...}}`. Full programmatic access to the install/update form definition an app would render. Without the Accept header, returns the SPA shell. **Confirmed working on 2.5.0.118 beta; on 2.4.4.156 stable the endpoint returns the SPA shell regardless of the Accept header.** |
| 🟢 `/installedapp/events/{id}` | HTML | App events page |
| 🟢 `/installedapp/status/{id}` | HTML | App status page |
| 🟢 `/installedapp/sysApp/{appName}` | HTML | System app shortcut (URL-encoded name) |
| 🟢 `/installedapp/sysAppApi/appCloner/app/0` | JSON | App cloner API (list all) |
| 🟢 `/installedapp/sysAppApi/appCloner/app/{id}` | JSON | Clone specific app info |

**Redirects:**
- `/installedapp/direct/hubVariables` → Hub variables page
- `/installedapp/direct/swapDevice` → Swap device page
- `/app/hubitatSafetyMonitor` → HSM page

### Action Endpoints

| Endpoint | Type | Description |
|----------|------|-------------|
| 🔴 `/installedapp/create/{appTypeId}` | GET | Install/create app instance |
| 🔴 `/installedapp/delete/{id}` | GET | Delete installed app |
| 🔴 `/installedapp/forcedelete/{id}/quiet` | GET | Force-delete installed app |
| 🔴 `/installedapp/disable` | POST (form body) | Disable/enable installed app |
| 🔴 `/appui/clearEmptyBasicRules` | GET | Clear empty basic rules |
| 🔴 `/appui/createBasicRulesChild` | GET | Create basic rules child app |

---

## 8. hub2/ API (Modern UI Data)

The `hub2/` endpoints power the modern Hubitat web interface. They return rich JSON data and are the best source for getting comprehensive device, room, and hub information. "hub2" refers to the second-generation UI framework (built with **Vue.js**, a JavaScript framework for web apps). All endpoints return JSON.

### Read-Only

| Endpoint | Type | Description |
|----------|------|-------------|
| 🟢 `/hub2/hubData?t={timestamp}` | JSON | Comprehensive hub data: alerts, model, version, IP, all alert flags, safeMode, showDeveloperMenu, freshDatabase |
| 🟢 `/hub2/appsList` | JSON | All available app types (system built-in apps) |
| 🟢 `/hub2/devicesList` | JSON | All devices as tree (parent/child) with current states, rooms, protocol info, dashboard types, tags, notes — richest device endpoint |
| 🟢 `/hub2/roomsList` | JSON | Tree structure: rooms containing their devices with full state. Room ID 999999 = "Unassigned" |
| 🟢 `/hub2/roomsList?includeDevices=false&includeFavorites=true` | JSON | Rooms variant — `includeDevices=false` may not take effect on older firmware |
| 🟢 `/hub2/hubMeshJson` | JSON | Hub mesh config: hubList, sharedDevices, token, enabled, refresh interval |
| 🟢 `/hub2/localBackups` | JSON | Local backup files with hubUID, version, size, creation time |
| 🟢 `/hub2/cloudBackups?force={bool}` | JSON | Cloud backup list with entitlement info |
| 🟢 `/hub2/backup/json` | JSON | Backup schedule: frequency, cleanup time, cloud entitlements, password status |
| 🟢 `/hub2/networkConfiguration` | JSON | Full network config: static IP, gateway, subnet, DNS, WiFi, LAN auto-neg, useDNSFallover, hasEthernet, hasWiFi, restartBonjourOnSchedule |
| 🟢 `/hub2/suggestedIntegrations` | JSON | Auto-discovered integrations (Matter, HomeKit, Chromecast) with IPs |
| 🟢 `/hub2/userAppTypes` | JSON | **All user app types — parents, standalones, AND children.** Fields: id, name, namespace, oauth, lastModified, usedBy (array of installed instance `{id, name}`). This is what the modern Apps Code UI uses. Use this instead of `/app/list/data` if you need child apps in the list. |
| 🟢 `/hub2/userDeviceTypes` | JSON | User-installed driver types with capabilities and which devices use them |
| 🟢 `/hub2/userLibraries` | JSON | User libraries |
| 🟢 `/hub2/userBundles` | JSON | User bundles |
| 🟢 `/hub2/availableWiFiNetworks?reload={bool}` | JSON | Visible WiFi SSIDs |
| 🟢 `/hub2/fetchHubHomePageBanner` | JSON | Home-page marketing feed: `{learningVideos: [{image, label, url}], whatsNew: [{image, label, url}], elevateYourH...}` — YouTube tutorial thumbnails and "what's new" cards shown on the hub's home page |
| 🟢 `/hub2/fetchShortcuts` | JSON | Global shortcut inventory: `{devices: [{id, label}], apps: [{id, label}]}` — flat list used by the UI's global search / quick-jump picker. Returns every device and every installed app instance with its current label |

<details>
<summary><b>Sample — /hub2/devicesList (abbreviated)</b></summary>

```json
{
  "suggestBackup": false,
  "devices": [{
    "key": "DEV-6",
    "data": {
      "id": 6,
      "name": "Generic Zigbee Outlet",
      "secondaryName": "",
      "type": "Generic Zigbee Outlet",
      "source": "sys",
      "dni": "124F",
      "zigbeeId": "00124B00251B581C",
      "lastActivity": "2026-04-16T01:20:50+0000",
      "disabled": false,
      "roomId": 2,
      "roomName": "Kitchen",
      "currentStates": [{"value": "off", "key": "switch"}],
      "hubMesh": false,
      "homekit": false,
      "showOnHome": false,
      "deviceTypeId": 124
    }
  }]
}
```
</details>

<details>
<summary><b>Sample — /hub2/roomsList (abbreviated)</b></summary>

```json
{
  "roomNodes": [{
    "key": "ROOM-2",
    "data": {"id": 2, "name": "Kitchen", "type": "Room"},
    "children": [{
      "key": "DEV-6",
      "data": {"id": 6, "name": "Kitchen Outlet", "currentStates": [{"value": "off", "key": "switch"}]}
    }]
  }]
}
```
</details>

<details>
<summary><b>Sample — /hub2/backup/json</b></summary>

```json
{
  "databaseCleanupTimeHour": 2,
  "localBackupFrequency": 1,
  "cloudBackupFrequency": 1,
  "lastCloudBackupSuccess": true,
  "hasCloudBackupEntitlements": true,
  "hasCloudRestoreEntitlements": true,
  "hasC8ProMigrationEntitlements": true,
  "backupPassword": null,
  "freshDatabase": false
}
```
</details>

<details>
<summary><b>Sample — /hub2/networkConfiguration</b></summary>

```json
{
  "usingStaticIP": false,
  "staticIP": "",
  "staticGateway": "",
  "staticSubnetMask": "",
  "staticNameServers": [],
  "dhcpNameServers": ["192.168.1.1"],
  "dnsServers": ["192.168.1.1"],
  "lanAddr": "192.168.1.100",
  "wlanAddr": null,
  "lanAutoneg": "autoneg",
  "hasEthernet": true,
  "wifiDriversInstalled": true
}
```
</details>

### Action Endpoints

| Endpoint | Type | Description |
|----------|------|-------------|
| 🔴 `/hub2/setSetting/chatbot/{bool}` | GET | Toggle chatbot |
| 🔴 `/hub2/setSetting/developerMenu/{bool}` | GET | Toggle developer menu |
| 🔴 `/hub2/setSetting/excludeVirtualDevicesFromSummary/{bool}` | GET | Toggle virtual device exclusion |
| 🔴 `/hub2/setSetting/includeHiddenDevicesInSummary/{bool}` | GET | Toggle hidden devices |
| 🔴 `/hub2/addVarToMesh/{name}` | GET | Add variable to hub mesh |
| 🔴 `/hub2/removeVarFromMesh/{name}` | GET | Remove variable from hub mesh |
| 🔴 `/hub2/createLinkedHubVar/{hubId}/{name}` | GET | Create linked hub variable |
| 🔴 `/hub2/updateLinkedHubVar/{params}` | GET | Update linked hub variable |
| 🔴 `/hub2/ignoreFoundDevice?{params}` | GET | Ignore a discovered device |
| 🔴 `/hub2/updateBackupSchedule` | POST | Update backup schedule |
| 🔴 `/hub2/uploadBackup` | POST | Upload backup file (multipart) |
| 🔴 `/hub2/restoreUploadedBackup` | GET | Restore uploaded backup |
| 🔴 `/hub2/restoreLocalBackup?{params}` | GET | Restore local backup |
| 🔴 `/hub2/restoreCloudBackup?{params}` | GET | Restore cloud backup |
| 🔴 `/hub2/deleteLocalBackup?{params}` | GET | Delete local backup |
| 🔴 `/hub2/deleteCloudBackup?{params}` | GET | Delete cloud backup |
| 🔴 `/hub2/zwave/nodeReplace` | GET | Z-Wave node replace |

### Hub Variable Mesh Sharing (2.5.0 beta+)

| Endpoint | Type | Description |
|----------|------|-------------|
| 🔴 `/hub2/addVarToMesh` | GET | Share a hub variable to hub mesh |
| 🔴 `/hub2/removeVarFromMesh` | GET | Remove a hub variable from mesh |
| 🔴 `/hub2/createLinkedHubVar/{hubId}/{varName}` | GET | Create a linked hub variable from mesh |
| 🔴 `/hub2/updateLinkedHubVar/{hubId}/{varName}` | GET | Update a linked hub variable |

### Other New hub2/ Endpoints (2.5.0 beta+)

| Endpoint | Type | Description |
|----------|------|-------------|
| 🔴 `/hub2/ignoreFoundDevice?{params}` | GET | Dismiss a discovered device from the "Connect Devices" banner |

---

## 9. Location & Modes

### Read-Only

| Endpoint | Type | Description |
|----------|------|-------------|
| 🟢 `/location/list/data` | JSON | Location: name, temperatureScale, timeZone, zipCode, lat, lon, sunrise, sunset, currentMode |
| 🟢 `/location/data` | JSON | Same plus modes list and hubs array |
| 🟢 `/location/zipcodelookup/{zipcode}` | JSON | Zip code lookup |
| 🟢 `/modes/json` | JSON | All modes with trigger conditions, currentModeId, mode manager config |
| 🟢 `/modes/list/json` | JSON | Simple list: `[{id, label}]` |
| 🟢 `/modes/easyModeManager/json` | JSON | Detailed mode transition rules with trigger types and conditions |
| 🟢 `/modes/edit` | HTML | Mode editor page |

<details>
<summary><b>Sample — /modes/json</b></summary>

```json
{
  "modes": [
    {"id": 1, "name": "Day", "icon": "fa-sun", "conditions": ["At <b>15 minutes</b> after sunrise"]},
    {"id": 2, "name": "Evening", "icon": "fa-sunset", "conditions": ["At <b>15 minutes</b> after sunset"]},
    {"id": 3, "name": "Night", "icon": "fa-moon", "conditions": ["When time of day is <b>9:30 pm</b>"]},
    {"id": 4, "name": "Away", "icon": "fa-plane-departure", "conditions": []}
  ],
  "currentModeId": 2,
  "selectedModeManager": "builtIn",
  "modeManagerAppId": 0,
  "easyModeManagerAppId": 61
}
```
</details>

### Action Endpoints

| Endpoint | Type | Description |
|----------|------|-------------|
| 🔴 `/location/mode/update` | POST | Change the current mode — **returns 500 via direct API call; may require session state** |
| 🔴 `/location/update` | POST (form body) | Update location settings — body: `city=X&state=X&timeZone=X&temperatureScale=F` |
| 🔴 `/location/updateTimeJson` | POST → JSON | Update time settings — **404 on firmware 2.4.3** |
| 🔴 `/modes/jsonCreate` | POST (JSON body) → JSON | Create a new mode — body: `{"name":"ModeName"}` |
| 🔴 `/modes/jsonUpdate` | POST (JSON body) → JSON | Update a mode — body: `{"id":1,"name":"NewName"}` |
| 🔴 `/modes/jsonDelete/{modeId}` | GET → JSON | Delete a mode (cannot delete active mode) |
| 🔴 `/modes/setModeManager/{appId}` | GET | Set mode manager app |

> **Content-Type matters:** `/modes/jsonCreate` and `/modes/jsonUpdate` **require** `Content-Type: application/json` with a JSON body like `{"name":"MyMode","icon":"fa-flask"}`. Using form-urlencoded silently fails with `{"success":false,"message":"Internal error"}`. Icons use FontAwesome class names (e.g., `fa-sun`, `fa-moon`, `fa-plane-departure`).

---

## 10. Rooms

| Endpoint | Type | Description |
|----------|------|-------------|
| 🟢 `/room/list` | HTML | Room list page |
| 🟢 `/room/listRoomNamesJson` | JSON | Room name strings: `["Attic", "Kitchen", ...]` |
| 🟢 `/room/listRoomsJson` | JSON | Rooms with IDs: `[{id, name}]` |
| 🔴 `/room/save` | POST (form body) | Save/create room — **returns 500 via direct API; see notes below** |
| 🔴 `/room/delete/{roomId}` | GET | Delete room |

> **Room creation gotcha:** `/room/save` returns 500 with form-urlencoded body. With `Content-Type: application/json` and `{"name":"TestRoom"}`, it returns `{"roomId":null,"error":"Invalid room id"}`. Room creation via the direct API may require additional fields or session state not available outside the browser UI.

---

## 11. Logs

| Endpoint | Type | Description |
|----------|------|-------------|
| 🟢 `/logs` | HTML | Live logs page |
| 🟢 `/logs?tab=past` | HTML | Past logs page |
| 🟢 `/logs/json` | JSON | Device statistics dashboard: event counts, state sizes, uptime, devicesUptime, cloud call counts, hub action counts |
| 🟢 `/logs/eventsJson` | JSON | Location-level events (sunrise/sunset, mode changes, etc.) |
| 🟢 `/logs/past/json` | JSON | Past log entries as tab-delimited strings: `{timestamp}\t{LEVEL}\t{source}\|{id}\|{name}\|{message}` |
| 🟢 `/logs/past/json?type=dev&id={deviceId}` | JSON | Past logs filtered by source type (`dev`, `app`, `sys`) and ID |
| 🟢 `/hub/eventsJson` | JSON | Hub-level events (systemStart, etc.) |
| 🟢 `/hub/zigbeeLogs` | HTML | Zigbee log viewer |
| 🟢 `/hub/zwaveLogs` | HTML | Z-Wave log viewer |
| 🟢 `/hub/loggerlist` | HTML | Logger configuration page |

<details>
<summary><b>Sample — /logs/json (device statistics, not log lines)</b></summary>

```json
{
  "maxEvents": 500,
  "maxStates": "100",
  "uptime": "2 days 14 hours",
  "deviceStats": [{
    "id": 6,
    "name": "Kitchen Outlet",
    "total": 42,
    "stateSize": 1024,
    "average": 2.1,
    "pct": 0.5,
    "eventsCount": 42,
    "statesCount": 8
  }]
}
```
</details>

<details>
<summary><b>Sample — /hub/eventsJson</b></summary>

```json
[{
  "id": 393346,
  "source": "HUB",
  "name": "systemStart",
  "value": "2.4.4.156",
  "descriptionText": "System startup with build: 2.4.4.156"
}]
```
</details>

---

## 12. Dashboard Endpoints

### Read-Only

| Endpoint | Type | Description |
|----------|------|-------------|
| 🟢 `/dashboard/all` | JSON | All **legacy** dashboards (does not include Easy Dashboards) |
| 🟢 `/dashboard/all?pinToken={token}` | JSON | All dashboards with PIN auth |
| 🟢 `/dashboard/devices` | JSON | All devices with current attributes, room, protocol, dashboard types, retry config |
| 🟢 `/dashboard/select` | HTML | Dashboard selector |
| 🟢 `/dashboard/mobileDevicesUI?showEdit=false` | HTML | Mobile devices UI |
| 🟢 `/dashboard/mobileFavoritesUI?showEdit=false` | HTML | Mobile favorites UI |
| 🟢 `/dashboard/ui/{id}` | Redirect | Dashboard UI |
| 🟢 `/dashboard/room/{id}` | Redirect | Room dashboard |
| 🟢 `/dashboard/menu` | Redirect | Dashboard menu |

### Action Endpoints

| Endpoint | Type | Description |
|----------|------|-------------|
| 🔴 `/dashboard/create?name={name}&type=easy` | GET → JSON | Create Easy Dashboard (returns `{success, id}` — id is an installed app ID) |
| 🔴 `/dashboard/update?id={id}&name={name}` | GET → JSON | Update/rename dashboard |
| 🔴 `/dashboard/delete?id={id}` | GET → JSON | Delete dashboard — **returns `success:false` for Easy Dashboards** |
| 🔴 `/dashboard/cloneAsEasy/{id}` | GET → JSON | Clone dashboard — **returns `success:false` if source has no tiles** |
| 🔴 `/dashboard/setGenerateForRooms/{bool}` | GET | Toggle room dashboard auto-generation (verified via `/hub2/hubData` `maintainRoomDashboards` field) |

> **Easy vs Legacy dashboards:** `/dashboard/create?type=easy` creates an Easy Dashboard as an installed app instance. These dashboards do **not** appear in `/dashboard/all` (which only lists legacy/classic dashboards). Easy Dashboards are managed via `/installedapp/*` endpoints instead. Delete and clone operations may return `success:false` for Easy Dashboards.

---

## 13. Libraries & Bundles

| Endpoint | Type | Description |
|----------|------|-------------|
| 🟢 `/library/list` | HTML | Library list page |
| 🟢 `/library/list/single/data/{id}` | JSON | Single library data |
| 🟢 `/library/editor/{id}` | HTML | Library code editor |
| 🔴 `/library/create` | HTML | Create library page |
| 🔴 `/library/saveOrUpdateJson` | POST (JSON body) | Save/update library (same JSON body format as app/driver). **Library names must consist of letters, numbers, underscores, dashes, and dots only — no spaces.** |
| 🔴 `/library/deleteLibrary/{id}` | GET | Delete library |
| 🔴 `/library/edit/deleteJson/{id}` | GET | Delete library (JSON response) |
| 🟢 `/bundle/list` | HTML | Bundle list page |
| 🟢 `/bundle/list/json` | JSON | All bundles: `[{id, name, sourceUrl, sourcePassword, appTypeIds, deviceTypeIds, appTypeIdsUpdateOrder, deviceTypeIdsUpdateOrder, installedAppTypeIds, ...}]` — full bundle metadata including source GitHub URL, included app/driver type IDs, and update-order config |
| 🟢 `/bundle/editor/{id}` | HTML | Bundle editor |
| 🔴 `/bundle/create` | HTML | Create bundle page |
| 🔴 `/bundle/deleteJson/{id}` | GET | Delete bundle |
| 🔴 `/bundle/deleteJson/{id}?full=true` | GET | Full delete bundle (removes apps/drivers too) |
| 🔴 `/bundle2/uploadZip` | POST (multipart) | Upload bundle zip file |
| 🔴 `/bundle2/uploadZipFromUrl?{params}` | GET | Upload bundle from URL |
| 🔴 `/bundle2/processUploadedZip` | POST | Process uploaded zip |

---

## 14. Zigbee Management

### Read-Only

| Endpoint | Type | Description |
|----------|------|-------------|
| 🟢 `/hub/zigbeeInfo` | HTML | Zigbee info page |
| 🟢 `/hub/zigbeeInfo/status` | JSON | Zigbee radio status |
| 🟢 `/hub/zigbeeDetails/json` | JSON | Zigbee device details: channel, panId, enabled, networkState, devices with message counts, LQI |
| 🟢 `/hub/zigbeeGraph` | HTML | Zigbee mesh graph visualization |
| 🟢 `/hub/zigbeeLogs` | HTML | Zigbee logs page |
| 🟢 `/hub/zigbee/getChildAndRouteInfoJson` | JSON | Mesh topology: children, neighbors (LQI, cost), routes |
| 🟢 `/hub/zigbee/getChildAndRouteInfo` | Text | Same data, human-readable format |
| 🟢 `/hub/zigbeeChannelScanJson` | JSON | Channel scan results — array of `{panId, lastHopRssi, stackProfile, lastHopLqi, extendedPanId, nwkUpdateId, channel, allowingJoin}` (must run scan first) |

<details>
<summary><b>Sample — /hub/zigbeeDetails/json (abbreviated)</b></summary>

```json
{
  "channel": 25,
  "panId": "37D5",
  "extendedPanId": "287681FFFED3D062",
  "weakChannel": false,
  "enabled": true,
  "networkState": "ONLINE",
  "inJoinMode": false,
  "firmwareUpdateInProgress": false,
  "firmwareUpdateAvailable": false,
  "channels": [11,12,13,14,15,16,17,18,19,20,21,22,23,24,25,26],
  "showPowerSelection": false,
  "powerLevel": 8,
  "powerLevels": [8],
  "showReboot": false,
  "showRebuild": false,
  "showRebuildNetworkOnReboot": false,
  "rebuildNetworkOnReboot": false,
  "showInactiveDevicePing": false,
  "inactiveDevicePingEnabled": false,
  "healthy": true,
  "devices": [{
    "id": 6,
    "name": "Kitchen Outlet",
    "type": "Router",
    "lastMessage": "2026-04-16T01:20:50+0000",
    "ping": true,
    "messageCount": 1542,
    "shortZigbeeId": "124F",
    "zigbeeId": "00124B00251B581C"
  }]
}
```

When the Zigbee radio is **disabled**, the `channel`, `panId`, `extendedPanId`, and `weakChannel` fields are **omitted** from the response, and `networkState` changes to `"DISABLED"`.
</details>

<details>
<summary><b>Sample — /hub/zigbee/getChildAndRouteInfoJson</b></summary>

```json
{
  "children": [
    {"id": "6373", "type": "EMBER_SLEEPY_END_DEVICE"}
  ],
  "neighbors": [
    {"id": "0A29", "lqi": 154, "age": 4, "inCost": 1, "outCost": 1},
    {"id": "124F", "lqi": 138, "age": 4, "inCost": 1, "outCost": 2}
  ]
}
```
</details>

### Action Endpoints

<details>
<summary><b>Sample — /hub/zigbeeChannelScanJson (one entry)</b></summary>

```json
{
  "panId": "2AB3",
  "lastHopRssi": -48,
  "stackProfile": 2,
  "lastHopLqi": 255,
  "extendedPanId": "B3EAED0100810700",
  "nwkUpdateId": 0,
  "channel": 11,
  "allowingJoin": false
}
```

Multiple networks can appear on the same channel. Channels with no detected network have null `panId`/`extendedPanId` and 0 RSSI/LQI.
</details>

| Endpoint | Type | Description |
|----------|------|-------------|
| 🔴 `/hub/zigbee/enable/true` | GET | Enable Zigbee radio |
| 🔴 `/hub/zigbee/enable/false` | GET | Disable Zigbee radio |
| 🔴 `/hub/zigbeeChannelScan` | GET → JSON | Start a Zigbee channel scan |
| 🔴 `/hub/zigbee/updateChannelAndPower?{params}` | GET | Update Zigbee channel and TX power |
| 🔴 `/hub/zigbee/updatePingDevice/{id}/{bool}` | GET | Toggle Zigbee device ping |
| 🔴 `/hub/zigbee/updateSettings?{params}` | GET | Update Zigbee settings |
| 🔴 `/hub/rebootZigbeeRadio` | GET | ⚠️ Reboot the Zigbee radio — **on C-4, triggers a full hub restart** (not just a radio reboot). |
| 🔴 `/hub/rebuildZigbeeNetwork` | GET | ⚠️ Rebuild entire Zigbee mesh — **on C-4, triggers full hub restart**. |
| 🔴 `/hub/zigbee/reset` | GET | ⚠️ **Reset the Zigbee stack** — returns HTTP 200. Radio stays enabled but network parameters may be reset. |
| 🔴 `/hub/zigbee/updateFirmware/latest` | GET | ⚠️ Update Zigbee firmware |
| 🔴 `/hub/searchZigbeeDevices/{type}` | GET → JSON | Start Zigbee device search (status: `{deviceMap, initMap, reconfiguredDevices}`) |

> **`/hub/stopJoin`** is a universal endpoint that stops both Zigbee and Z-Wave join/discovery modes.

---

## 15. Z-Wave Management

### Read-Only

| Endpoint | Type | Description |
|----------|------|-------------|
| 🟢 `/hub/zwaveInfo` | HTML | Z-Wave info page |
| 🟢 `/hub/zwaveInfo?fwupdate=true` | HTML | Z-Wave firmware update info |
| 🟢 `/hub/zwaveDetails/json` | JSON | Z-Wave radio status, all paired devices keyed by node ID |
| 🟢 `/hub/zwaveGraph` | HTML | Z-Wave mesh graph visualization |
| 🟢 `/hub/zwaveTopology` | HTML | Z-Wave topology page |
| 🟢 `/hub/zwaveLogs` | HTML | Z-Wave logs page |
| 🟢 `/hub/zwaveVersion` | Text | Z-Wave SDK version |
| 🟢 `/hub/zwave/getChildAndRouteInfoJson` | JSON | Z-Wave child/route info |
| 🟢 `/hub/searchZwaveDevices` | JSON | Z-Wave search/inclusion status: `{deviceMap, initMap, userRequest}` |
| 🟢 `/hub/zwaveExclude/status` | JSON | Exclusion mode status |
| 🟢 `/hub/zwaveRepair2Status` | JSON | Repair v2 status: `{stage, html}` |
| 🟢 `/hub/checkZwaveRepairRunning` | JSON | `{isZWaveNetworkHealRunning: "true"/"false"}` |
| 🟢 `/hub/zwave/nodeReplace/status` | JSON | Node replace status |
| 🟢 `/hub/zwave/nodeReplace/info` | HTML | Node replace info (HTML page, not JSON) |
| 🟢 `/hub/zwave/updateHubFirmwareStatus` | JSON | Firmware update progress |
| 🔴 `/hub/zwave/resetJson` | JSON | **Triggers Z-Wave controller reset** — returns `true` on success. Resets the Z-Wave radio to factory defaults. Despite being a GET, this is destructive. |
| 🟢 `/hub/zwave/securityKeys` | HTML | Z-Wave security keys page (HTML, not JSON) |
| 🟢 `/hub/zwave/securityCode` | HTML | Z-Wave DSK code page (HTML, not JSON) |

<details>
<summary><b>Sample — /hub/zwaveDetails/json (abbreviated)</b></summary>

```json
{
  "showSecureJoin": true,
  "showRegion": false,
  "secureJoin": 0,
  "zwaveIP": false,
  "zwaveJSAvailable": false,
  "zwaveJS": false,
  "enabled": true,
  "isRadioUpdateNeeded": false,
  "healthy": true,
  "updateInProgress": false,
  "zwDevices": {
    "23": {
      "displayName": "Front Lights",
      "label": "Front Lights",
      "driverType": "sys",
      "name": "Generic Z-Wave CentralScene Switch",
      "id": 23,
      "deviceNetworkId": "17"
    }
  },
  "nodes": [{
    "nodeId": 1, "manufacturer": 0, "router": false,
    "listening": false, "security": false, "beaming": false,
    "lastSent": null, "lastReceived": null, "nodeState": "ALIVE",
    "deviceType": 1, "deviceId": null, "msgCount": 0,
    "supportedCommandClasses": "[:]", "init": true
  }]
}
```

Notes:
- `zwDevices` is an object keyed by Z-Wave node ID (string), not an array
- `secureJoin` values: 0=no security, 1=S0, 2=S2
- `zwaveJS=false` on C-4 confirms legacy Z-Wave stack
- When disabled, `nodes` array is empty and `enabled=false`
- Node 1 is always the hub's own Z-Wave controller
</details>

### Action Endpoints

| Endpoint | Type | Description |
|----------|------|-------------|
| 🔴 `/hub/zwave/enable/true` | GET | Enable Z-Wave radio |
| 🔴 `/hub/zwave/enable/false` | GET | Disable Z-Wave radio |
| 🔴 `/hub/startZwaveJoin` | GET | Start Z-Wave inclusion mode |
| 🔴 `/hub/stopJoin` | GET | Stop join mode |
| 🔴 `/hub/zwaveExclude` | GET | Start Z-Wave exclusion mode |
| 🔴 `/hub/stopZWaveExclude` | GET | Stop exclusion mode |
| 🔴 `/hub/zwaveRepair` | GET | ⚠️ Start Z-Wave network repair |
| 🔴 `/hub/zwaveRepair2?resetStats=false&maxHealth=10` | GET | Z-Wave repair v2 |
| 🔴 `/hub/zwaveCancelRepair` | GET | Cancel Z-Wave repair |
| 🔴 `/hub/zwaveNodeRepair2?{params}` | GET | Repair specific Z-Wave node |
| 🔴 `/hub/zwaveDetails/update?{params}` | GET | Update Z-Wave details |
| 🟡 `/hub/zwaveNodeDetailGet` | GET | Trigger background node detail fetch |
| 🔴 `/hub/zwaveRefreshAllNodes` | GET | Refresh all Z-Wave nodes |
| 🔴 `/hub/zwave/discoverDevice` | POST (form body) | Discover Z-Wave device — body: `id={nodeId}` |
| 🔴 `/hub/zwave/nodeReinitialize` | POST (form body) | Reinitialize Z-Wave node — body: `id={nodeId}` |
| 🔴 `/hub/zwave/nodeRemove` | POST (form body) | Remove Z-Wave node — body: `id={nodeId}` |
| 🔴 `/hub/zwave/nodeCleanup` | GET | Clean up Z-Wave node |
| 🔴 `/hub/zwave/refreshNodeStatus` | POST (form body) | Refresh node status — body: `id={nodeId}` |
| 🔴 `/hub/zwave/pingNode` | GET | Ping a Z-Wave node |
| 🔴 `/hub/zwave/nodeReplace/securityKeys` | POST | Set replacement security keys |
| 🔴 `/hub/zwave/nodeReplace/stop` | POST | Stop node replacement |
| 🔴 `/hub/zwave/startUpdateHubFirmware` | GET | ⚠️ Start Z-Wave firmware update |

> **Z-Wave POST node operations:** The endpoints `/hub/zwave/discoverDevice`, `/hub/zwave/nodeReinitialize`, `/hub/zwave/nodeRemove`, `/hub/zwave/refreshNodeStatus`, and `/hub/zwave/pingNode` all accept POST with form body `id={nodeId}`. They return 404 on GET (POST only). All return 500 when targeting the hub controller node (id=1) — they require an actual paired device node.
>
> **C-4 Z-Wave limitations:** `/hub/zwaveLogs` returns 404. `/hub/zwaveTopology` returns 500. `/hub/zwaveVersion` returns "No Z-Wave controller instance is available" after a radio disable/enable cycle. `/hub/zwaveRefreshAllNodes` shows the same controller error. The C-4 uses the legacy Z-Wave stack, not ZWaveJS.

### Z-Wave Device OTA Firmware Update (2.5.0 beta+)

| Endpoint | Type | Description |
|----------|------|-------------|
| 🟢 `/hub/zwave/deviceFirmware/devices` | JSON | List devices eligible for OTA firmware update |
| 🟢 `/hub/zwave/deviceFirmware/files` | JSON | List available firmware files |
| 🟢 `/hub/zwave/deviceFirmware/details?{params}` | JSON | Firmware update details for a device |
| 🟢 `/hub/zwave/deviceFirmware/progress?{params}` | JSON | Poll OTA update progress |
| 🔴 `/hub/zwave/deviceFirmware/start` | POST | Start OTA firmware update on a device |
| 🔴 `/hub/zwave/deviceFirmware/abort` | GET | Abort in-progress firmware update |

### Z-Wave Node State (2.5.0 beta+)

| Endpoint | Type | Description |
|----------|------|-------------|
| 🟢 `/hub/zwaveNodeState?node={nodeId}` | JSON | Get Z-Wave node state |

---

## 16. Z-Wave 2 (Dual Radio / Antenna)

For hubs with dual Z-Wave radios (C-8 Pro). On the C-4, `/hub/zwave2/antennaTestProgress` returns 404; `enable` returns `{"success":false,"message":"ZWaveJS installation is invalid"}`; `disable` returns `{"success":false,"message":"ZWaveJS is already disabled"}`.

| Endpoint | Type | Description |
|----------|------|-------------|
| 🟢 `/hub/zwave2/antennaTestProgress` | JSON | Antenna test progress (404 on C-4) |
| 🟡 `/hub/zwave2/antennaTestContinue` | GET | Continue antenna test |
| 🔴 `/hub/zwave2/enable` | GET | Enable Z-Wave 2 |
| 🔴 `/hub/zwave2/disable` | GET | Disable Z-Wave 2 |
| 🔴 `/hub/zwave2/startAntennaTest?{params}` | GET | Start antenna test |
| 🟢 `/hub/zwave2/getNodeState?node={nodeId}` | JSON | Get Z-Wave 2 node state (2.5.0 beta+) |

---

## 17. Matter

| Endpoint | Type | Description |
|----------|------|-------------|
| 🟢 `/hub/matterDetails` | HTML | Matter details page |
| 🟢 `/hub/matterDetails/json` | JSON | Matter status: enabled, installed, networkState |
| 🟢 `/hub/matterPairDeviceStatus?nodeId={id}` | JSON | Matter pairing status |
| 🔴 `/hub/matter/enable/true` | GET | Enable Matter |
| 🔴 `/hub/matter/enable/false` | GET | Disable Matter |
| 🔴 `/hub/matter/pair?setupCode={code}` | GET | Pair Matter device |
| 🔴 `/hub/matter/openPairingWindow?node={id}` | GET | Open Matter pairing window |
| 🔴 `/hub/matter/reset` | GET | ⚠️ **Reset Matter** |
| 🟢 `/hub/matterLogs` | HTML | Matter logs page (2.5.0 beta+) |
| 🟢 `/hub/matterLogs/json` | JSON | Matter logs as JSON (2.5.0 beta+) |

<details>
<summary><b>Sample — /hub/matterDetails/json</b></summary>

```json
{
  "enabled": false,
  "installed": true,
  "installFailed": false,
  "installInProgress": false,
  "rebootRequired": false,
  "networkState": "Disabled"
}
```

On the **C-4**, `installed` is `false` and `networkState` is omitted. `/hub/matter/enable/true` returns `{"success":true}` but has no practical effect — Matter remains not-installed. The enable/disable cycle is a no-op on unsupported hardware.
</details>

---

## 18. Hub Advanced / Admin

### Read-Only

| Endpoint | Type | Description |
|----------|------|-------------|
| 🟢 `/hub/advanced/freeOSMemory` | Text | Free OS memory (KB) |
| 🟢 `/hub/advanced/freeOSMemoryHistory` | Text | Historical memory/CPU CSV |
| 🟢 `/hub/advanced/freeOSMemoryLast` | Text | Latest memory snapshot |
| 🟢 `/hub/advanced/internalTempCelsius` | Text | Hub temperature (°C) |
| 🟢 `/hub/advanced/databaseSize` | Text | Database size (MB) |
| 🟢 `/hub/advanced/event/limit` | Text | Current event limit |
| 🟢 `/hub/advanced/zipgatewayVersion` | Text | Z-Wave gateway version |
| 🟢 `/hub/advanced/latestBackupFileInProgress` | Text | Backup in-progress status |
| 🟢 `/hub/advanced/network/lanautonegconfigstatus` | Text | LAN auto-neg status |
| 🟢 `/hub/advanced/registeredUsersForLocalHubs` | JSON | Registered users |
| 🟢 `/hub/advanced/getWiFiNetworkInfoAsyncStatus` | JSON | WiFi config async status |
| 🟢 `/hub/advanced/certificate` | HTML | SSL certificate management page |
| 🟢 `/hub/advanced/cloudInfo` | HTML | Cloud info page — serves the SPA shell; the underlying data comes from `/hub2/hubData` and related `/hub2/*` endpoints |
| 🟢 `/hub/advanced/logSettings` | HTML | Log settings page (SPA shell) |
| 🟢 `/hub/advanced/safetyNotifications` | HTML | Safety notifications config page (SPA shell) |
| 🟢 `/hub/advanced/zigbeeDetails` | HTML | Zigbee advanced details page (SPA shell) — JSON data at `/hub/zigbeeDetails/json` |
| 🟢 `/hub/advanced/zwaveDetails` | HTML | Z-Wave advanced details page (SPA shell) — JSON data at `/hub/zwaveDetails/json` |

### Action Endpoints

| Endpoint | Type | Description |
|----------|------|-------------|
| 🔴 `/hub/advanced/event/limit/{1-2000}` | GET → Text | Set event limit |
| 🔴 `/hub/advanced/enableHubMesh` | GET → Text | Enable hub mesh — **returns "enabled, please reboot"; deferred until reboot** |
| 🔴 `/hub/advanced/disableHubMesh` | GET → Text | Disable hub mesh — **returns "disabled, please reboot"; deferred until reboot** |
| 🔴 `/hub/advanced/enableCloudController` | GET | Enable cloud controller |
| 🔴 `/hub/advanced/disableCloudController` | GET | Disable cloud controller |
| 🔴 `/hub/advanced/setConnectDevicesVisible/{bool}` | GET → Text | Toggle connect devices visibility |
| 🔴 `/hub/advanced/setGetStartedVisible/{bool}` | GET → Text | Toggle get started visibility |
| 🔴 `/hub/advanced/setRestartBonjour/{bool}` | GET | Toggle Bonjour restart |
| 🔴 `/hub/advanced/installDriver/all` | GET | Install all system drivers |
| 🔴 `/hub/advanced/deleteScheduledJob?{params}` | GET | Delete scheduled job |
| 🔴 `/hub/advanced/disablessl` | GET | Disable SSL |
| 🔴 `/hub/advanced/network/lanautonegconfigenable` | GET | Enable LAN auto-negotiation — **⚠️ see sticky state gotcha below** |
| 🔴 `/hub/advanced/network/lanautonegconfigdisable` | GET | Disable LAN auto-negotiation |

> **LAN auto-negotiation sticky state:** Toggling auto-negotiation off and back on leaves the status as `fixed_100` instead of `autoneg`. The state does not fully restore after a disable→enable cycle — it becomes "sticky" at the fixed setting. Exercise caution; you may need a reboot to fully restore auto-negotiation.
| 🔴 `/hub/advanced/network/ethernetMode/{mode}` | GET | Set ethernet mode |
| 🔴 `/hub/advanced/disconnectEthernet` | GET | Disconnect ethernet |
| 🔴 `/hub/advanced/disconnectWiFi` | GET | Disconnect WiFi |
| 🔴 `/hub/advanced/switchToDhcp?{params}` | GET | Switch to DHCP |
| 🔴 `/hub/advanced/switchToStaticIp?{params}` | GET | Switch to static IP |
| 🔴 `/hub/advanced/setWiFiNetworkInfo?{params}` | GET | Set WiFi config |
| 🔴 `/hub/advanced/setWiFiNetworkInfoAsync?{params}` | GET | Set WiFi config (async) |

### Dangerous — Hub Lifecycle

| Endpoint | Type | Description |
|----------|------|-------------|
| 🔴 `/hub/reboot` | POST | ⚠️ **Reboot the hub** — **POST only** (GET returns 404). Returns HTTP 200. Hub restarts in ~10 seconds. |
| 🔴 `/hub/shutdown` | POST | ⚠️ **Shut down the hub** — **POST only** (GET returns 404). Hub goes fully offline (port 80 and 8081). Requires physical power cycle to restart. |
| 🔴 `/hub/forceGC` | GET | Force Java garbage collection |
| 🔴 `/hub/cleanupDatabase` | GET | Clean up database |
| 🔴 `/hub/backupDB?fileName=latest` | GET → File | Trigger database backup (streams file download) |
| 🔴 `/hub/restoreWithReboot` | GET | Restore backup and reboot |
| 🔴 `/hub/restoreWithReboot?localOnly=yes&onboarding=yes` | GET | Restore during onboarding |
| 🟢 `/hub/rebooting?t={timestamp}` | HTML | Rebooting status page (HTML with startup animation) |
| 🟢 `/hub/shuttingDown` | HTML | Shutting down status page (HTTP 200 even when hub is running) |
| 🟢 `/hubStatus` | JSON | Boot progress JSON: `{serverInitPercentage: "Initializing Hub: 90%", serverInitDetails: "identifying hub", status: "starting"}`. Status values: `starting`, `running`, `brokenDatabase` |

---

## 19. Hub Cloud & Updates

| Endpoint | Type | Description |
|----------|------|-------------|
| 🟢 `/hub/cloud/isPlatformLatest` | JSON | `{platformLatest: true/false}` |
| 🟢 `/hub/cloud/isPlatformUpdatedRecently` | Text | Recent update check |
| 🟢 `/hub/cloud/checkUpdateStatus` | JSON | Poll update progress — `{status, percent}`. Status values: `IDLE`, `DOWNLOAD_IN_PROGRESS`, `DOWNLOAD_STARTED`, `DOWNLOAD_FAILED`, `DOWNLOAD_FINISHED`, `DOWNLOAD_VERIFY`, `EXTRACT_*`, `UPDATE_STARTED` |
| 🔴 `/hub/cloud/clearUpdateStatus` | GET | Clear update status — returns `{"success": "true"}` |
| 🔴 `/hub/cloud/checkForUpdate` | GET | Check for firmware updates — returns `{version, upgrade, releaseNotesUrl, status}` |
| 🔴 `/hub/cloud/updatePlatform` | GET | ⚠️ **Start firmware download and install** — returns `{"success": "true"}`. Hub auto-reboots after install. |
| 🔴 `/hub/cloud/triggerBackupWithStatus?migration={bool}` | GET | Trigger cloud backup |
| 🔴 `/hub/platformUpdate` | HTML | Firmware update page |
| 🔴 `/hub/dismissPlatformUpdate` | GET | Dismiss the update notification banner |

### Firmware Update Flow (Programmatic)

```bash
HUB=http://192.168.1.100

# 1. Check for updates
curl "$HUB/hub/cloud/checkForUpdate"
# → {"version":"2.5.0.116","upgrade":true,"status":"UPDATE_AVAILABLE"}

# 2. Start update (downloads + installs + reboots)
curl "$HUB/hub/cloud/updatePlatform"
# → {"success":"true"}

# 3. Poll progress
curl "$HUB/hub/cloud/checkUpdateStatus"
# → {"status":"DOWNLOAD_IN_PROGRESS","percent":0}
# → {"status":"DOWNLOAD_FINISHED"} → {"status":"UPDATE_STARTED"}
# → Hub reboots automatically, shows "Starting Up" page

# 4. Wait for hub to come back
# Poll /hubStatus until {"status":"running"}, then /hub2/hubData for new version
```

**Update status progression:** `DOWNLOAD_IN_PROGRESS` (with `percent`) → `DOWNLOAD_FINISHED` → `DOWNLOAD_VERIFY` → `EXTRACT_*` → `UPDATE_STARTED` → hub reboots → "Hubitat Starting Up" page (with empty `buildVersion=`) → hub comes back on new firmware.

**No update available:** `{"status":"NO_UPDATE_AVAILABLE"}` — returned when hub is unregistered, has no cloud connection, or is already on the latest firmware.

---

## 20. Hub Settings & UI

### Read-Only

| Endpoint | Type | Description |
|----------|------|-------------|
| 🟢 `/hub/edit` | HTML | Hub settings page |
| 🟢 `/hub/register` | HTML | Hub registration page |
| 🟢 `/hub/migrate` | HTML | Hub migration page |
| 🟢 `/hub/compatibleDevices` | HTML | Compatible devices list |
| 🟢 `/hub/getAllTimeZones` | JSON | All timezones JSON |
| 🟢 `/hub/getTop100TimeZones` | JSON | Top 100 timezones JSON |
| 🟢 `/hub/getLatLongBasedOnGeolocator?t={timestamp}` | JSON | Geolocation lookup |
| 🟢 `/alerts` | HTML | Alerts page |
| 🟢 `/tos` | HTML | Terms of Service |

### Action Endpoints

| Endpoint | Type | Description |
|----------|------|-------------|
| 🔴 `/hub/applyDarkMode/true` | GET | Enable dark mode |
| 🔴 `/hub/applyDarkMode/false` | GET | Disable dark mode |
| 🔴 `/hub/dismissPlatformUpdate?version={ver}` | GET | Dismiss update notification |
| 🔴 `/hub/dismissBuiltInApps` | GET | Dismiss built-in apps notice |
| 🔴 `/hub/dismissWeakZigbee` | GET | Dismiss weak Zigbee notice |
| 🔴 `/hub/toggleUISecurityEnabled` | GET | Toggle UI security on/off |
| 🔴 `/hub/acceptTOS` | GET | Accept Terms of Service |
| 🔴 `/hub/temporarilyAcceptTOS?noRedirect=true` | GET | Temporarily accept TOS |
| 🔴 `/hub/updateName?{params}` | POST | Update hub name |
| 🔴 `/hub/updatePostalCode?{params}` | POST | Update postal code |
| 🔴 `/hub/updateLatLongTimezone?{params}` | POST | Update location/timezone |
| 🔴 `/hub/commandRetryController/setDevices?{params}` | GET | Set command retry devices |
| 🔴 `/menuExpanded?prefix={prefix}&value={bool}` | GET | Toggle menu expansion state |

---

## 21. Network Configuration

| Endpoint | Type | Description |
|----------|------|-------------|
| 🟢 `/hub/networkTest` | HTML | Network test page |
| 🟢 `/hub/networkSetup` | HTML | Network setup page |
| 🟢 `/hub/networkTest/ping/gateway` | Text | Ping the gateway (live output) |
| 🟢 `/hub/networkTest/ping/{ip}` | Text | Ping a specific IP |
| 🟢 `/hub/networkTest/speedtest` | Text | Run a speed test |
| 🟢 `/hub/networkTest/traceroute/{ip}` | Text | Traceroute to an IP |
| 🟢 `/hub/advanced/scanForNtpServers` | HTML | Scan for NTP servers — returns HTML table with IP, reported time, and "Set" links |
| 🟢 `/hub/advanced/ntpServer` | Text | Current NTP server (returns "No value set" if not configured) |
| 🔴 `/hub/advanced/ntpServer/{ip}` | GET | Set NTP server to a specific IP |

---

## 22. File Manager

| Endpoint | Type | Description |
|----------|------|-------------|
| 🟢 `/hub/fileManager` | HTML | File manager page |
| 🟢 `/hub/fileManager/json` | JSON | File list: `{files: [{date, size, name, id, type}], path, freeSpace}` |
| 🟢 `/local/{path}/{filename}` | File | Access uploaded local files |
| 🔴 `/hub/fileManager/upload` | POST (multipart) | Upload file — **requires real multipart/form-data; piped stdin fails** |
| 🔴 `/hub/fileManager/delete` | POST (form body) | Delete file |

<details>
<summary><b>Sample — /hub/fileManager/json</b></summary>

```json
{
  "files": [
    {"date": "1766509518900", "size": "109838", "name": "deviceProfiles.json", "id": "-4260954271872655204", "type": "file"},
    {"size": "0", "name": "firmware", "id": "...", "type": "dir"}
  ],
  "path": null,
  "freeSpace": 999090294
}
```
</details>

---

## 23. TTS (Text-to-Speech)

| Endpoint | Type | Description |
|----------|------|-------------|
| 🟢 `/hub/tts/edit` | JSON | Current voice and all available voices: `{current, voices: [{name, gender, language}]}` |
| 🔴 `/hub/tts/updateUIFriendly` | POST | Update TTS UI name |
| 🔴 `/hub/tts/updateDefaultVoice` | GET | Update default TTS voice |

---

## 24. User Management

| Endpoint | Type | Description |
|----------|------|-------------|
| 🟢 `/hub/users` | HTML | User list |
| 🟢 `/hub/userAdmin` | HTML | User admin page |
| 🟢 `/hub/editUser/{id}` | HTML | Edit user page |
| 🔴 `/hub/addUser` | POST | Add new user |
| 🔴 `/hub/deleteUser/{id}` | GET | Delete user |

---

## 25. Code Publishing

| Endpoint | Type | Description |
|----------|------|-------------|
| 🟢 `/hub/publishCode/status` | JSON | `{success: true, completed: true, hubs: []}` |
| 🔴 `/hub/publishCode/app/{id}` | GET | Publish app code |
| 🔴 `/hub/publishCode/driver/{id}` | GET | Publish driver code |
| 🔴 `/hub/publishCode/file/{id}` | GET | Publish file |

---

## 26. Mobile API & SmartStart

| Endpoint | Type | Description |
|----------|------|-------------|
| 🟢 `/mobileapi/zwave/smartstart/list?t={timestamp}` | JSON | List SmartStart entries |
| 🔴 `/mobileapi/zwave/smartstart/delete` | POST | Delete SmartStart entry |

---

## 27. Maker API (Token Required)

The **Maker API** is Hubitat's official integration endpoint — it's the recommended way for external apps, dashboards, and home automation platforms to communicate with your hub. Unlike the internal endpoints above (which have no authentication), Maker API requires an **access token** (a long random string that acts as a password) appended to every request.

Requires the Maker API app to be installed (Settings → Apps in the hub UI). All endpoints use:
```
http://<HUB_IP>/apps/api/{appId}/{endpoint}?access_token={token}
```

**Installation via API:** Maker API is system app type 15. Install with `GET /installedapp/create/15`. Device selection requires the web UI configure flow — it cannot be done via API alone.

**Discovering the access token:** After installation, the token is in the app state:
```
GET /installedapp/statusJson/{installedAppId}
→ appState: [{"name": "accessToken", "value": "<token>", "type": "String"}]
```

**Authentication errors:** Invalid or missing token returns XML: `<oauth><error>invalid_token</error></oauth>`

**OAuth 2.0 authorization endpoints** (for 3rd-party OAuth-based integrations — used when an app sets `oauth` enabled in `/app/updateOAuth`):

| Endpoint | Type | Description |
|----------|------|-------------|
| 🟢 `/oauth/authorize` | GET | OAuth 2.0 authorize endpoint at the hub root. Empty body without `response_type`, `client_id`, and `redirect_uri` query params. Start of the OAuth consent flow. |
| 🟢 `/apps/api/oauth/authorize` | GET | Same authorize endpoint under the Maker-API path prefix. Empty body without required OAuth params. |
| 🟢 `/oauth/null` | HTML | SPA shell served when `redirect_uri` is missing or invalid — fallback error page for malformed OAuth redirects. |

**Settings (from `/installedapp/statusJson`):** `pickedDevices` (device list), `localAccess` (bool), `cloudAccess` (bool), `allowModes` (bool), `allowHSM` (bool), `postURL` (text), `corsHosts` (text), `logging` (bool)

### Device Endpoints

| Endpoint | Type | Description |
|----------|------|-------------|
| 🟢 `/devices` | JSON | All devices: `[{id, name, label, type, room, roomId}]` |
| 🟢 `/devices/all` | JSON | All devices with full attributes, capabilities, commands |
| 🟢 `/devices/{deviceId}` | JSON | Single device with typed attributes |
| 🟢 `/devices/deviceData/{deviceId}` | JSON | Device data section |
| 🟢 `/devices/{deviceId}/events?num={n}` | JSON | Device event history (optional `num` param limits results) |
| 🟢 `/devices/{deviceId}/commands` | JSON | Available commands: `[{command, type}]` |
| 🟢 `/devices/{deviceId}/capabilities` | JSON | Device capabilities with current values |
| 🟢 `/devices/{deviceId}/attribute/{attribute}` | JSON | Single attribute value |
| 🔴 `/devices/{deviceId}/{command}` | GET → JSON | Send command to device |
| 🔴 `/devices/{deviceId}/{command}/{secondaryValue}` | GET → JSON | Send command with value |
| 🔴 `/devices/{deviceId}/setLabel?label={label}` | GET → JSON | Set device label |
| 🔴 `/devices/{deviceId}/setDriver?namespace={ns}&name={name}` | GET → JSON | Set device driver |

### Hub Variables

| Endpoint | Type | Description |
|----------|------|-------------|
| 🟢 `/hubvariables` | JSON | Hub variables list |
| 🟢 `/hubvariables/{name}` | JSON | Get hub variable value |
| 🔴 `/hubvariables/{name}/{value}` | GET → JSON | Set hub variable value |

### Modes & HSM

| Endpoint | Type | Description |
|----------|------|-------------|
| 🟢 `/modes` | JSON | All modes: `[{active, id, name}]` |
| 🔴 `/modes/{modeId}` | GET → JSON | Set active mode |
| 🟢 `/hsm` | JSON | HSM status |
| 🔴 `/hsm/{status}` | GET → JSON | Set HSM status |

### Rooms

| Endpoint | Type | Description |
|----------|------|-------------|
| 🟢 `/rooms` | JSON | All rooms: `[{id, name, deviceIds}]` |
| 🟢 `/room/select/{roomId}` | JSON | Room details |
| 🔴 `/room/insert?name={name}&deviceIds={ids}` | GET → JSON | Create room |
| 🔴 `/room/update/{roomId}?name={name}&deviceIds={ids}` | GET → JSON | Update room |
| 🔴 `/room/delete/{roomId}` | GET → JSON | Delete room |

### Other

| Endpoint | Type | Description |
|----------|------|-------------|
| 🔴 `/postURL/{url}` | GET → JSON | Set webhook URL — returns `{"url": "<url>"}` |
| 🟢 `/notification/{text}` | JSON | Send notification (404 on firmware 2.4.3/2.4.4 — may require newer firmware) |
| 🟢 `/location` | JSON | Location info (404 on firmware 2.4.3/2.4.4 — may require newer firmware) |
| 🟢 `/info` | JSON | Hub info via Maker API (404 on firmware 2.4.3/2.4.4 — may require newer firmware) |

### Dashboard (via Maker API)

| Endpoint | Type | Description |
|----------|------|-------------|
| 🟢 `/apps/api/{appId}/dashboard/{dashId}?access_token={token}` | JSON | Dashboard data |
| 🟢 `/apps/api/{appId}/menu?access_token={token}&local=true` | JSON | Dashboard menu |

<details>
<summary><b>Sample — /devices/{id}/commands</b></summary>

```json
[
  {"command": "configure", "type": ["n/a"]},
  {"command": "off", "type": ["n/a"]},
  {"command": "on", "type": ["n/a"]},
  {"command": "setColor", "type": ["COLOR_MAP"]},
  {"command": "setColorTemperature", "type": ["NUMBER", "NUMBER", "NUMBER"]},
  {"command": "setLevel", "type": ["NUMBER", "NUMBER"]}
]
```
</details>

---

## 28. Port 8081 (Diagnostic / Recovery Tool)

Your hub runs a second, separate web server on **port 8081** (e.g., `http://192.168.1.100:8081`). This is the **Diagnostic Tool** — a lightweight recovery interface that stays running even when the main hub application (on port 80) is down or stuck. It's your lifeline when the hub won't boot properly.

Think of it like a BIOS recovery menu for your hub — you can reboot, restore backups, switch firmware versions, or factory reset from here even when the main UI is completely broken.

### Authentication

Auth uses the hub's MAC address (uppercase, no colons):

1. **Login:** `POST :8081/newLogin` with body = MAC address (e.g., `C44EAC23B218`), `Content-Type: application/x-www-form-urlencoded`
   - Returns `{"inProgress": false, "success": true, "message": "<token>"}`
2. **Subsequent requests:** Add `Authorization: Basic <token>` header
3. **Check login:** `GET :8081/checkLogin` with same header — returns 200 if valid
4. **Token is invalidated on reboot** — must re-login after each restart

### No Authentication Required

| Endpoint | Type | Description |
|----------|------|-------------|
| 🟢 `:8081/` | HTML | Diagnostic landing page (Vue SPA) |
| 🟢 `:8081/api/latestBackupFileInProgress` | Text | Returns `true`/`false` — whether a backup is currently being created |
| 🟢 `:8081/api/downloadLatestBackup` | File | **Downloads the latest hub backup** (LZF format, ~400KB). No authentication! |
| 🟢 `:8081/hubStartStatus` | GET → JSON | Hub startup progress — `{"inProgress": false, "success": true, "message": ""}`. The Vue update-tool polls this during a boot/restart to know when the main app (port 80) is ready. |
| 🟢 `:8081/setupStatus` | GET → JSON | First-boot setup progress — `{"inProgress": false, "success": false, "message": "Unknown status, please try again"}` on a running hub. Used during initial setup and after a soft/factory reset to surface the wizard step. |
| 🔴 `:8081/deleteDatabaseTraceFiles` | GET → Text | Delete database trace files — returns `deleted files: [...], could not delete: [...]` |

### Requires Authentication (401 Unauthorized without credentials)

| Endpoint | Type | Description |
|----------|------|-------------|
| 🟢 `:8081/api/hubInfo` | POST → JSON | Hub info: `{hubId, uv (update tool version), hubAvailable, pv (platform version), ip, hubTime, hv (hardware version), stableVersion}` |
| 🟢 `:8081/api/currentTime` | POST → JSON | `{success: true, currentDateTime: "Thu, 16 Apr 2026 01:31:55 CDT"}` |
| 🟢 `:8081/api/backups` | JSON | List backups: `{success, backups: [{name, createTime, fileSize}]}` — includes auto-backup from shutdown |
| 🟢 `:8081/api/versions` | POST → JSON | Available firmware: `{success, hubList: ["hub-2.4.4.156", ...], currentVersionExecuting}` |
| 🟢 `:8081/api/getLatest` | POST → JSON | `{"getLatest": "success"}` |
| 🟢 `:8081/api/downloadBackup/{filename}` | File | Download specific backup file by name |
| 🔴 `:8081/api/rebootHub` | POST → JSON | Reboot — `{"success": true}` |
| 🔴 `:8081/api/shutdownHub` | POST → JSON | Shutdown — `{"success": true}` |
| 🔴 `:8081/api/safeMode` | POST → JSON | Boot into safe mode — `{"success": true}`. Hub restarts with `safeMode: true` in hubData. Normal reboot exits safe mode. |
| 🔴 `:8081/api/softResetHub?clearPastLogs={bool}` | POST → JSON | **Soft reset** — wipes database (devices, apps, name, settings) but preserves radio configurations (Zigbee channel/PanId, Z-Wave nodes). Hub comes back with `freshDatabase: true` and name "My New Hub". |
| 🔴 `:8081/api/factoryResetHub` | POST → JSON | **Factory reset** — erases all data including radio config. **Fails on C-4** with `UnsatisfiedLinkError: gnu.io.RXTXPort.controlRs485` (RS-485 native library not available on C-4 hardware). ⚠️ The crash can corrupt the database — the main app (port 80) may fail to start afterward, requiring a soft reset via `:8081/api/softResetHub` to recover. |
| 🔴 `:8081/api/switchToStable` | POST → JSON | Switch to stable firmware channel |
| 🔴 `:8081/api/switchVersion/hub-{version}` | POST → JSON | Switch to specific firmware version (versions from `/api/versions`) |
| 🔴 `:8081/api/updateTime` | POST | Update hub time |

> **Security note:** `/api/downloadLatestBackup` requires no authentication on the LAN. Anyone on the local network can download a full hub backup containing device configurations, app settings, and potentially sensitive data. The auth mechanism itself uses only the hub's MAC address, which is discoverable via ARP.

---

## 29. Onboarding & Setup Wizard

The hub presents a setup wizard on first boot or after a soft/factory reset (`freshDatabase: true`). The entire wizard can be driven programmatically — no browser interaction required.

### Setup Flow (in order)

| Step | Endpoint | Type | Description |
|------|----------|------|-------------|
| 🔴 Accept TOS | `/hub/acceptTOS` | GET | Accept terms of service — required before any other setup |
| 🔴 Set hub name | `/hub/updateName?name={name}` | GET | Set the hub's display name |
| 🔴 Set location (zip) | `/hub/updatePostalCode?code={zip}` | GET → Text | Set location by postal/ZIP code — returns `true` |
| 🔴 Set location (coords) | `/hub/updateLatLogTimezone?latitude={lat}&longitude={lng}&timeZone={tz}&temperatureScale={C or F}` | GET → Text | Set location by coordinates with timezone — returns `true` |
| 🔴 Set location (POST) | `/location/update` | POST | Update location — accepts form-urlencoded body |
| 🔴 Register hub | `/onboardingRegisterHub?id_token={token}` | GET | Register with Hubitat cloud — requires a Cognito `IdToken` from prior login to `https://service.cloud.hubitat.com/au/auth` |
| 🟢 Check status | `/onboarding?step=10&checkHubStatus=true` | GET | Check if hub setup is complete |

### Dismissing the Wizard

| Endpoint | Type | Description |
|----------|------|-------------|
| 🔴 `/hub/advanced/setGetStartedVisible/false` | GET → Text | Hide the "Get Started" wizard page — returns `done` |
| 🔴 `/hub/advanced/setGetStartedVisible/true` | GET → Text | Show the "Get Started" page again |
| 🔴 `/hub/advanced/setConnectDevicesVisible/false` | GET → Text | Hide the "Connect Devices to Your Hub" discovery banner |
| 🔴 `/hub/advanced/setConnectDevicesVisible/true` | GET → Text | Show the device discovery banner |
| 🔴 `/getstarted?start=over` | GET | Reset and restart the entire setup wizard |

### Example: Programmatic Hub Setup

```bash
HUB=http://192.168.1.100

# 1. Accept TOS
curl "$HUB/hub/acceptTOS"

# 2. Set hub name
curl "$HUB/hub/updateName?name=MyHub"

# 3. Set location by coordinates
curl "$HUB/hub/updateLatLogTimezone?latitude=32.7767&longitude=-96.7970&timeZone=America/Chicago&temperatureScale=F"

# 4. Or set location by ZIP code
curl "$HUB/hub/updatePostalCode?code=75201"

# 5. Set location details via POST (city, state, country, timezone)
curl -X POST -d "city=Dallas&state=TX&country=US&timeZone=America/Chicago&temperatureScale=F&zipCode=75201" \
  "$HUB/location/update"

# 6. Dismiss the setup wizard
curl "$HUB/hub/advanced/setGetStartedVisible/false"
curl "$HUB/hub/advanced/setConnectDevicesVisible/false"
```

**`/hub/updateName` parameters:**
- `name` — Hub display name (string)

**`/hub/updateLatLogTimezone` parameters:**
- `latitude` — Decimal latitude (e.g., `32.7767`)
- `longitude` — Decimal longitude (e.g., `-96.7970`)
- `timeZone` — IANA timezone (e.g., `America/Chicago`, `America/New_York`, `Europe/London`)
- `temperatureScale` — `F` (Fahrenheit) or `C` (Celsius)

**`/location/update` POST body (form-urlencoded):**
- `city` — City name
- `state` — State/province
- `country` — Country code
- `timeZone` — IANA timezone
- `temperatureScale` — `F` or `C`
- `zipCode` — Postal/ZIP code
- `latitude` — Decimal latitude (optional)
- `longitude` — Decimal longitude (optional)

> **Note:** After a soft reset, the hub boots into the setup wizard at `/getstarted`. The main UI (sidebar, dashboard) is functional underneath — the wizard is just an overlay. Calling `/hub/advanced/setGetStartedVisible/false` dismisses it without completing setup. The cloud controller may need to be enabled separately via `/hub/advanced/enableCloudController` + reboot.

---

## 30. Miscellaneous

| Endpoint | Type | Description |
|----------|------|-------------|
| 🟢 `/hubStatus` | JSON | Boot progress: `{serverInitPercentage, serverInitDetails, status}` — status is `starting` during boot, `running` when ready, `brokenDatabase` if corrupt. Returns 404 when hub is fully booted. |
| 🟢 `/runtimeStats/displayStat/check{statName}/{value}` | JSON | Check/toggle runtime stat display |
| 🔴 `/logout` | Redirect | Log out |
| 🔴 `/getstarted?start=over` | Redirect | Restart onboarding |
| 🔴 `/setupWiFi` | Redirect | WiFi setup |
| 🔴 `/onboardingRegisterHub?id_token={token}` | GET | Register hub during onboarding |
| 🔴 `/hub/dismissAppsSplitMessage` | GET | Dismiss the apps section split notification (2.5.0 beta+) |
| 🟢 `/app/roomOccupancyLighting/{roomId}/config` | JSON | Room occupancy lighting configuration (2.5.0 beta+) |

**External cloud endpoint:**
- `https://service.cloud.hubitat.com/au/auth` — POST: Cloud authentication

---

## 31. Confirmed Non-Existent Endpoints (404)

The following commonly guessed paths do **not** exist on firmware 2.4.4.156 (C-8 Pro):

`/hub/memoryUsage`, `/hub/threadInfo`, `/hub/stats`, `/hub/info`, `/hub/hubInfo`, `/hub/variable/list`, `/hub/variables`, `/hub/mode`, `/hub/modes`, `/hub/hsm`, `/hub/rooms`, `/hub/room/list`, `/hub/schedule`, `/hub/schedules`, `/hub/subscription`, `/hub/subscriptions`, `/hub/state`, `/hub/firmware`, `/hub/firmwareVersion`, `/hub/platform`, `/hub/advanced/platformData`, `/hub/advanced/hubModel`, `/hub/advanced/hubUID`, `/hub/hubMesh`, `/hub/hubMeshInfo`, `/hub/notifications`, `/hub/dashboard`, `/hub/dashboards`, `/hub/advanced/settings`, `/hub/advanced/config`, `/hub/advanced/network` (base path), `/hub/advanced/zipGateway`, `/hub/enableStats`, `/hub/disableStats`, `/room/list/data`, `/subscription/list/data`, `/hub/cloud/token`, `/hub/cloud/info`, `/device/fullListJson`, `/device/data/{id}`, `/device/ajax/data`, `/hub/advanced/databaseSizeHistory`, `/hub/zigbeeVersion`, `/hub/advanced/uptime`, `/hubsocket`

**Additional 404s on C-4 (firmware 2.4.3.103):**
`/hub/cpuInfo`, `/hub/publishCode/status`, `/hub/zwaveLogs`, `/hub/zwave/securityKeys`, `/hub/zwave/securityCode`, `/hub/zwaveRepair2Status`, `/hub/zwave2/antennaTestProgress`, `/location/updateTimeJson`

---

## Response Header Notes

Every HTTP response includes **headers** — metadata about the response that comes before the actual data. Here's what Hubitat's headers look like:

```
HTTP/1.1 200 OK
Set-Cookie: HUBSESSION=node0...; Path=/
Expires: Thu, 01 Jan 1970 00:00:00 GMT
Content-Type: text/html;charset=utf-8
Transfer-Encoding: chunked
```

- **No `Server` header** — the hub doesn't reveal what web server software it runs
- **`Content-Type` is often wrong** — many JSON responses claim to be `text/html`. Don't trust this header; check the response body instead
- **Session cookies** — the hub issues a `HUBSESSION` cookie with every response (used by the web UI to track your browsing session)
- **No caching** — the `Expires` date is set to 1970 (the past), telling browsers to never cache responses
- **Chunked encoding** — responses are sent in chunks rather than all at once (a standard HTTP technique for dynamically generated content)

---

## Important Things to Know

### Security

1. **No login required for internal endpoints.** Anyone on your local network can access every endpoint listed here (except Maker API, which needs a token). There is no username/password protection. This is by design — Hubitat assumes your home network is trusted.

2. **Visiting a URL can trigger actions.** Most web apps require you to click a button to do something dangerous. Hubitat doesn't — simply visiting a URL like `/hub/reboot` will reboot your hub. Be careful with bookmarks, link-checking tools, and browser extensions that pre-load URLs.

3. **The diagnostic tool (port 8081) uses your hub's MAC address as a password.** A **MAC address** is a hardware identifier printed on your hub and discoverable by any device on your network (via a protocol called **ARP**). This means the "authentication" on port 8081 is effectively security by obscurity. Anyone on your LAN who knows your hub's MAC can log in. Tokens expire on reboot.

4. **Hub backups can be downloaded without any authentication** via the port 8081 diagnostic tool (`/api/downloadLatestBackup`). Backups contain your device configurations, app settings, and potentially sensitive data.

5. **`/installedapp/statusJson/{id}` exposes internal app data** including OAuth access tokens, device lists, and app state — all without authentication. This is the most information-rich endpoint for any installed app.

### Common Gotchas

6. **Some endpoints need JSON, others need form data — there's no consistency.** When sending data to the hub via POST, some endpoints require the data as **JSON** (structured data with `Content-Type: application/json` header), while others need **form-urlencoded** data (like an HTML form submission). Using the wrong format causes silent failures or "Unknown error" messages. Each endpoint section notes the correct format.

7. **The hub's `Content-Type` response headers are unreliable.** Many endpoints that return JSON data incorrectly label it as `text/html`. Don't rely on the header to determine the response format — check the actual response body instead.

8. **System (built-in) code is hidden.** When you request the source code for built-in apps or drivers via `/app/ajax/code` or `/driver/ajax/code`, the `source` field comes back empty. Only user-installed code is readable.

9. **`/device/createVirtual` needs a numeric driver ID, not a name.** Use the `deviceTypeId` parameter with the number from `/device/drivers` (e.g., `?deviceTypeId=221` for Virtual Switch). Using a driver name string returns `deviceId: 0`.

10. **Some endpoints that work in the browser UI fail via direct API calls.** `/location/mode/update` and `/room/save` return errors (HTTP 500) when called directly but work fine in the web UI. These likely depend on browser session cookies or headers that the UI sends automatically.

11. **Easy Dashboards and Legacy Dashboards are different.** `/dashboard/all` only lists legacy dashboards. Easy Dashboards are actually installed app instances and must be queried via `/installedapp/*` endpoints instead.

### Reboot & Reset Behavior

12. **Reboot and shutdown require POST requests.** Unlike most other action endpoints, `/hub/reboot` and `/hub/shutdown` return 404 on GET — you must send a POST request (e.g., `curl -X POST http://hub/hub/reboot`). This is one of the few endpoints with proper method enforcement.

13. **Hub Mesh changes need a reboot.** Enabling or disabling Hub Mesh returns a "please reboot" message. The change doesn't take effect until you actually reboot.

14. **Soft reset wipes your database but preserves radio settings.** The soft reset on port 8081 (`/api/softResetHub`) deletes all your devices, apps, hub name, and settings — but keeps your Zigbee channel/network ID and Z-Wave node table intact. The hub comes back with a fresh database and the name "My New Hub".

15. **Factory reset can break C-4 hubs.** The `/api/factoryResetHub` endpoint crashes on C-4 hardware due to a missing serial port driver. Worse, the crash can corrupt the database, leaving the main hub application unable to start. Recovery requires a soft reset via port 8081. This likely works fine on newer hub models (C-7, C-8).

### Maker API Notes

16. **`postURL` is misleadingly named.** Despite the name, `GET /postURL/{url}` sets a webhook callback URL — it does not make an outbound POST request. It returns `{"url": "<url>"}`.

17. **`/location` and `/info` don't exist yet.** These Maker API endpoints return 404 on firmware 2.4.3 and 2.4.4. They may be added in newer firmware versions or require specific settings to be enabled.

---

*Tested on C-8 Pro (firmware 2.4.4.156) and C-4 (firmware 2.4.3.103). Endpoints may vary by hub model and firmware version — see the Hub Model Compatibility table for known differences. Always exercise caution with 🔴 action endpoints.*

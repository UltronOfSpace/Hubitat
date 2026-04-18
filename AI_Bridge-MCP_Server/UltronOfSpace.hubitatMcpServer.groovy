/**
 *  AI Bridge - MCP Server
 *
 *  Exposes a native MCP (Model Context Protocol) and OpenAPI endpoint on the
 *  hub itself, so any AI client (Claude Desktop, ChatGPT Custom GPTs, Grok,
 *  Gemini) can control the hub with no intermediate server required.
 */

definition(
    name: "AI Bridge - MCP Server",
    namespace: "UltronOfSpace",
    author: "Ultronumus Of Space",
    contributor: "Claude (Anthropic)",
    description: "AI assistant integration for Hubitat (Claude, ChatGPT, Grok, Gemini) via MCP + OpenAPI.",
    category: "Integrations",
    iconUrl: "",
    iconX2Url: "",
    importUrl: "https://raw.githubusercontent.com/UltronOfSpace/Hubitat/main/AI_Bridge-MCP_Server/UltronOfSpace.hubitatMcpServer.groovy",
    oauth: true,
    singleInstance: true
)

#include UltronOfSpace.mcpProtocol

import groovy.json.JsonOutput
import groovy.json.JsonSlurper

// ---------------------------------------------------------------------------
// Preferences
// ---------------------------------------------------------------------------

preferences {
    page(name: "mainPage")
}

def mainPage() {
    if (!state.accessToken) {
        try {
            createAccessToken()
        } catch (e) {
            return oauthRequiredPage()
        }
    }

    dynamicPage(name: "mainPage", title: "AI Bridge - MCP Server", install: true, uninstall: true) {

        // Step 1 — Devices
        section("Step 1: Pick your devices") {
            paragraph "<small style='color:#666'>The AI will only see and control these devices. Nothing else on your hub is exposed.</small>"
            input name: "selectedDevices", type: "capability.*",
                  title: "Which devices should the AI control?", multiple: true, required: false
        }

        if (!state.accessToken) return  // shouldn't happen, but guard

        // Step 2 — Pick your AI
        section("Step 2: Pick your AI") {
            input name: "aiClient", type: "enum",
                  title: "Which AI will you use?",
                  options: [
                      "chatgpt": "ChatGPT (phone app, website, or desktop)",
                      "claude":  "Claude Desktop (computer app)",
                      "grok":    "Grok",
                      "gemini":  "Google Gemini",
                      "other":   "Other / show me everything"
                  ],
                  required: false,
                  submitOnChange: true
        }

        // Step 3 — Follow the instructions for that AI
        if (settings.aiClient) {
            renderAiInstructions(settings.aiClient)
        } else {
            section {
                paragraph "<i>⬆️ Choose your AI above, then copy-paste instructions will appear here.</i>"
            }
        }

        section("Advanced") {
            input name: "logging", type: "bool",
                  title: "Enable debug logging", defaultValue: false
        }
    }
}

private void renderAiInstructions(String client) {
    String localBase = getBaseUrl()
    String cloudBase = getCloudBaseUrl()
    String token = state.accessToken
    String localMcpUrl = "${localBase}/mcp?access_token=${token}"
    String localOpenApiUrl = "${localBase}/openapi.json?access_token=${token}"
    String cloudMcpUrl = cloudBase ? "${cloudBase}/mcp?access_token=${token}" : null
    String cloudOpenApiUrl = cloudBase ? "${cloudBase}/openapi.json?access_token=${token}" : null

    switch (client) {
        case "chatgpt":  renderChatGptInstructions(cloudOpenApiUrl, localOpenApiUrl, cloudBase != null, token); break
        case "claude":   renderClaudeInstructions(cloudMcpUrl, localMcpUrl, cloudBase != null); break
        case "grok":     renderGrokInstructions(cloudOpenApiUrl, localOpenApiUrl, cloudBase != null, token); break
        case "gemini":   renderGeminiInstructions(cloudOpenApiUrl, localOpenApiUrl, cloudBase != null, token); break
        case "other":    renderEverything(localMcpUrl, localOpenApiUrl, cloudMcpUrl, cloudOpenApiUrl, token); break
    }
}

private void renderChatGptInstructions(String cloudUrl, String localUrl, boolean hasCloud, String token) {
    String urlToUse = hasCloud ? cloudUrl : localUrl

    section("Step 3: Copy this URL") {
        paragraph buildBigCopyBox(urlToUse)
    }

    section("Step 4: Paste it into ChatGPT") {
        paragraph """
<ol style='line-height:1.9;font-size:14px'>
  <li>Open <b>ChatGPT</b> (website, phone app, or desktop — doesn't matter)</li>
  <li>Click <b>your profile picture</b> → <b>My GPTs</b> → <b>Create a GPT</b><br>
      <small style='color:#666'>(or: <b>Explore GPTs</b> → <b>+ Create</b>)</small></li>
  <li>Click the <b>Configure</b> tab</li>
  <li>Scroll down, click <b>Create new action</b></li>
  <li>Click <b>Import from URL</b> and paste the URL from Step 3</li>
  <li>Scroll down to <b>Authentication</b>, click the gear icon:
    <ul>
      <li>Auth Type: <b>API Key</b></li>
      <li>API Key: <code>${token}</code> (copy this)</li>
      <li>Auth Type dropdown: <b>Custom</b></li>
      <li>Custom Header Name: leave empty</li>
    </ul>
  </li>
  <li>Click <b>Save</b>, then <b>Create</b> at the top</li>
  <li>Give the GPT a name (like "My Smart Home") and save</li>
</ol>
<div style='background:#e8f5e9;border-left:4px solid #27ae60;padding:10px;border-radius:4px;margin-top:8px'>
  <b>✅ Done forever.</b> Now say things like <i>"turn on the kitchen light"</i> in any future chat with that GPT.
  Works from your phone, computer, anywhere.
</div>
""".toString()
    }

    if (!hasCloud) {
        section {
            paragraph """
<div style='background:#fdf2f2;border-left:4px solid #e74c3c;padding:10px;border-radius:4px'>
<b>⚠️ Heads up:</b> your hub isn't registered with Hubitat cloud, so ChatGPT can only reach it when you're <b>on your home Wi-Fi</b>.
To fix: <b>Settings → Hub Details → Register Hub</b> (free, 10 seconds). Then reload this page.
</div>
""".toString()
        }
    }
}

private void renderClaudeInstructions(String cloudUrl, String localUrl, boolean hasCloud) {
    String urlToUse = hasCloud ? cloudUrl : localUrl

    section("Step 3: Copy this URL") {
        paragraph buildBigCopyBox(urlToUse)
    }

    section("Step 4: Paste it into Claude Desktop's config") {
        paragraph """
<ol style='line-height:1.9;font-size:14px'>
  <li>Open <b>Claude Desktop</b> on your computer</li>
  <li>Click the hamburger menu (three lines, top left) → <b>File</b> → <b>Settings</b><br>
      <small style='color:#666'>(on Mac: <b>Claude</b> menu → <b>Settings</b>)</small></li>
  <li>Click <b>Developer</b> in the left sidebar</li>
  <li>Click <b>Edit Config</b> — this opens a file in your text editor</li>
  <li>Replace everything in the file with this (the URL is already filled in for you):</li>
</ol>
<pre style='background:#f4f4f4;padding:12px;border-radius:4px;font-size:12px;overflow-x:auto;border:1px solid #ddd'>{
  "mcpServers": {
    "hubitat": {
      "command": "npx",
      "args": ["mcp-remote", "${urlToUse}"]
    }
  }
}</pre>
<ol start='6' style='line-height:1.9;font-size:14px'>
  <li>Save the file</li>
  <li>Completely quit Claude Desktop and reopen it</li>
</ol>
<div style='background:#e8f5e9;border-left:4px solid #27ae60;padding:10px;border-radius:4px;margin-top:8px'>
  <b>✅ Done forever.</b> Now ask: <i>"What devices do I have?"</i> or <i>"Turn on the kitchen light"</i>.
</div>
""".toString()
    }
}

private void renderGrokInstructions(String cloudUrl, String localUrl, boolean hasCloud, String token) {
    String urlToUse = hasCloud ? cloudUrl : localUrl

    section("Step 3: Copy this URL") {
        paragraph buildBigCopyBox(urlToUse)
    }

    section("Step 4: Paste it into Grok") {
        paragraph """
<ol style='line-height:1.9;font-size:14px'>
  <li>Open <b>Grok</b> (on x.com or in the app)</li>
  <li>Go to <b>custom tools</b> / <b>actions</b> / <b>integrations</b> (the wording may vary)</li>
  <li>Paste the URL from Step 3</li>
  <li>When asked for an API key or token, paste: <code>${token}</code></li>
</ol>
<div style='background:#e8f5e9;border-left:4px solid #27ae60;padding:10px;border-radius:4px;margin-top:8px'>
  <b>✅ Done.</b> Grok can now control your smart home.
</div>
<small style='color:#666'>Grok's custom tool feature is still evolving — if the UI doesn't match these steps, look for "custom actions" or "external tools" in your settings.</small>
""".toString()
    }

    if (!hasCloud) {
        section {
            paragraph """
<div style='background:#fdf2f2;border-left:4px solid #e74c3c;padding:10px;border-radius:4px'>
<b>⚠️ Register your hub first.</b> Grok runs in the cloud and needs cloud access.
<b>Settings → Hub Details → Register Hub</b> (free). Then reload this page.
</div>
""".toString()
        }
    }
}

private void renderGeminiInstructions(String cloudUrl, String localUrl, boolean hasCloud, String token) {
    String urlToUse = hasCloud ? cloudUrl : localUrl

    section("Step 3: Copy this URL") {
        paragraph buildBigCopyBox(urlToUse)
    }

    section("Step 4: Paste it into Gemini") {
        paragraph """
<ol style='line-height:1.9;font-size:14px'>
  <li>Open <b>Gemini</b> (gemini.google.com or the mobile app)</li>
  <li>Go to <b>Extensions</b> or <b>custom tools</b></li>
  <li>Add a new custom tool, paste the URL from Step 3</li>
  <li>When asked for an API key, paste: <code>${token}</code></li>
</ol>
<div style='background:#e8f5e9;border-left:4px solid #27ae60;padding:10px;border-radius:4px;margin-top:8px'>
  <b>✅ Done.</b> Gemini can now control your smart home.
</div>
""".toString()
    }

    if (!hasCloud) {
        section {
            paragraph """
<div style='background:#fdf2f2;border-left:4px solid #e74c3c;padding:10px;border-radius:4px'>
<b>⚠️ Register your hub first.</b> Gemini runs in the cloud and needs cloud access.
<b>Settings → Hub Details → Register Hub</b> (free). Then reload this page.
</div>
""".toString()
        }
    }
}

private void renderEverything(String localMcpUrl, String localOpenApiUrl, String cloudMcpUrl, String cloudOpenApiUrl, String token) {
    section("All URLs") {
        paragraph """
<table style='width:100%;border-collapse:collapse;font-size:13px'>
<tr style='background:#f4f4f4'><th style='text-align:left;padding:6px'>URL</th><th style='text-align:left;padding:6px'>For</th></tr>
<tr><td style='padding:6px;border-top:1px solid #ddd'><code style='word-break:break-all'>${localMcpUrl}</code></td><td style='padding:6px;border-top:1px solid #ddd'>MCP clients on home Wi-Fi</td></tr>
<tr><td style='padding:6px;border-top:1px solid #ddd'><code style='word-break:break-all'>${localOpenApiUrl}</code></td><td style='padding:6px;border-top:1px solid #ddd'>OpenAPI clients on home Wi-Fi</td></tr>
${cloudMcpUrl ? "<tr><td style='padding:6px;border-top:1px solid #ddd'><code style='word-break:break-all'>${cloudMcpUrl}</code></td><td style='padding:6px;border-top:1px solid #ddd'>MCP clients anywhere (cloud)</td></tr>" : ""}
${cloudOpenApiUrl ? "<tr><td style='padding:6px;border-top:1px solid #ddd'><code style='word-break:break-all'>${cloudOpenApiUrl}</code></td><td style='padding:6px;border-top:1px solid #ddd'>OpenAPI clients anywhere (cloud)</td></tr>" : ""}
<tr><td style='padding:6px;border-top:1px solid #ddd'><code>${token}</code></td><td style='padding:6px;border-top:1px solid #ddd'>Access token (standalone)</td></tr>
</table>
""".toString()
    }

    section("What to use where") {
        paragraph """
<ul style='line-height:1.7'>
  <li><b>MCP URL</b> = for clients that speak the Model Context Protocol directly (Claude Desktop, Cursor, etc.)</li>
  <li><b>OpenAPI URL</b> = for clients that use OpenAPI actions (ChatGPT Custom GPTs, Grok, Gemini)</li>
  <li><b>Local URL</b> = faster, only works on the same Wi-Fi as your hub</li>
  <li><b>Cloud URL</b> = works from anywhere on the internet, routes through Hubitat's free cloud relay</li>
</ul>
""".toString()
    }
}

private String buildBigCopyBox(String url) {
    return """
<div style='background:#eaf4fe;border:2px solid #3498db;border-radius:6px;padding:12px;margin-bottom:4px'>
  <div style='font-size:11px;color:#666;margin-bottom:4px'>Tap and hold to copy (mobile) — or triple-click to select</div>
  <code style='display:block;word-break:break-all;background:#fff;padding:10px;border-radius:4px;font-size:12px;user-select:all;-webkit-user-select:all'>${url}</code>
</div>
""".toString()
}

private oauthRequiredPage() {
    Integer appTypeId = app.getAppTypeId() ?: 0
    String editorUrl = "/app/editor/${appTypeId}"

    dynamicPage(name: "mainPage", title: "One-time setup: Enable OAuth", install: false, refreshInterval: 0) {
        section {
            paragraph """
<div style='background:#fff4e6;border-left:4px solid #f39c12;padding:12px;border-radius:4px;margin-bottom:8px'>
<b>⚡ One-time Hubitat platform step</b><br>
Hubitat requires user-installed apps to manually enable OAuth before exposing HTTP endpoints.
This takes about 5 seconds and only needs to be done once.
</div>
""".toString()
        }

        section("Steps") {
            paragraph """
<ol style='line-height:1.8'>
  <li>Open <a href='${editorUrl}' target='_blank'><b>Apps code → AI Bridge - MCP Server</b></a> (opens in new tab)</li>
  <li>In the top-right of the code editor, click the <b>OAuth</b> button</li>
  <li>Click <b>Enable OAuth in App</b> (leave all defaults)</li>
  <li>Click <b>Update</b></li>
  <li>Come back to this page and click <b>Done</b> below — the app will initialize automatically</li>
</ol>
""".toString()
        }

        section {
            paragraph """
<small style='color:#666'>
<b>Why this step exists:</b> Hubitat blocks user apps from exposing HTTP endpoints by default as a security measure.
Every Hubitat app with an external API (Maker API, HomeBridge, WebCoRE, etc.) has this same one-time gate.
Once enabled, it stays enabled — you'll never see this page again.
</small>
""".toString()
        }
    }
}

private String getBaseUrl() {
    return "${getFullLocalApiServerUrl()}"
}

private String getCloudBaseUrl() {
    try {
        String url = getFullApiServerUrl()
        // getFullApiServerUrl returns a cloud URL (cloud.hubitat.com) when the
        // hub is registered. On unregistered hubs it may return null, an
        // empty string, or the local URL.
        if (!url) return null
        if (url.contains("cloud.hubitat.com")) return url
        return null
    } catch (e) {
        return null
    }
}

private String buildUrlPanel(String label, String value, String subtitle = null) {
    String sub = subtitle ? "<div style='color:#666;font-size:12px;margin-bottom:4px'>${subtitle}</div>" : ""
    return """
<div style='background:#f8f9fa;border:1px solid #dee2e6;border-radius:4px;padding:10px;margin-bottom:8px'>
  <b>${label}</b>
  ${sub}
  <code style='display:block;word-break:break-all;background:#fff;padding:6px;border-radius:3px;border:1px solid #e9ecef;font-size:11px'>${value}</code>
</div>
""".toString()
}

def installed() { initialize() }
def updated() { initialize() }
def initialize() {
    if (!state.accessToken) {
        try { createAccessToken() } catch (e) { log.error "OAuth disabled: ${e.message}" }
    }
}

// ---------------------------------------------------------------------------
// HTTP mappings
// ---------------------------------------------------------------------------

mappings {
    path("/mcp") {
        action: [POST: "handleMcp"]
    }
    path("/openapi.json") {
        action: [GET: "handleOpenApi"]
    }
    path("/api/hub/details")     { action: [GET: "apiHubDetails"] }
    path("/api/hub/status")      { action: [GET: "apiHubStatus"] }
    path("/api/hub/memory")      { action: [GET: "apiHubMemory"] }
    path("/api/hub/temperature") { action: [GET: "apiHubTemperature"] }
    path("/api/hub/database")    { action: [GET: "apiHubDatabaseSize"] }
    path("/api/devices")         { action: [GET: "apiListDevices"] }
    path("/api/devices/:deviceId")           { action: [GET: "apiGetDevice"] }
    path("/api/devices/:deviceId/events")    { action: [GET: "apiGetDeviceEvents"] }
    path("/api/devices/:deviceId/commands")  { action: [GET: "apiGetDeviceCommands"] }
    path("/api/devices/:deviceId/command")   { action: [POST: "apiSendDeviceCommand"] }
    path("/api/modes")            { action: [GET: "apiListModes"] }
    path("/api/modes/:modeId")    { action: [POST: "apiSetMode"] }
    path("/api/hsm")              { action: [GET: "apiGetHsm"] }
    path("/api/hsm/:status")      { action: [POST: "apiSetHsm"] }
    path("/api/rooms")            { action: [GET: "apiListRooms"] }
    path("/api/rooms/devices")    { action: [GET: "apiListRoomsWithDevices"] }
    path("/api/variables")        { action: [GET: "apiListHubVariables"] }
    path("/api/variables/:name")  { action: [GET: "apiGetHubVariable", POST: "apiSetHubVariable"] }
    path("/api/location")         { action: [GET: "apiGetLocation"] }
    path("/api/zigbee")           { action: [GET: "apiZigbeeDetails"] }
    path("/api/zigbee/topology")  { action: [GET: "apiZigbeeTopology"] }
    path("/api/zwave")            { action: [GET: "apiZwaveDetails"] }
    path("/api/apps")             { action: [GET: "apiListApps"] }
    path("/api/apps/:appId")      { action: [GET: "apiGetApp"] }
    path("/api/logs")             { action: [GET: "apiGetLogs"] }
    path("/api/logs/stats")       { action: [GET: "apiGetDeviceStats"] }
    path("/api/network")          { action: [GET: "apiNetwork"] }
    path("/api/drivers")          { action: [GET: "apiListDrivers"] }
}

// ---------------------------------------------------------------------------
// MCP JSON-RPC handler
// ---------------------------------------------------------------------------

def handleMcp() {
    Map req = (request.JSON ?: [:]) as Map
    if (settings.logging) log.debug "MCP: ${req}"

    Map response = handleJsonRpc(req) { handler, args -> invokeTool(handler, args) }
    if (response == null) {
        render contentType: "application/json", data: "", status: 204
        return
    }
    render contentType: "application/json", data: JsonOutput.toJson(response), status: 200
}

def handleOpenApi() {
    String serverUrl = getBaseUrl()
    Map spec = generateOpenApiSpec(serverUrl, "AI Bridge - MCP Server")
    render contentType: "application/json", data: JsonOutput.toJson(spec), status: 200
}

// Dispatch tool name → method call
private invokeTool(String handlerName, Map args) {
    return "$handlerName"(args)
}

// ---------------------------------------------------------------------------
// Tool handlers (MCP → same methods used by REST endpoints)
// ---------------------------------------------------------------------------

def toolGetHubDetails(args) { return collectHubDetails() }
def toolGetHubStatus(args) { return collectHubStatus() }
def toolGetHubMemory(args) { return [freeOSMemoryKB: safeGetNum { location.hub?.getDataValue("freeMemory") }] }
def toolGetHubTemperature(args) { return [temperatureC: safeGetNum { location.hub?.getDataValue("temperature") }] }
def toolGetHubDatabaseSize(args) { return [databaseMB: safeGetNum { location.hub?.getDataValue("databaseSize") }] }
def toolGetHubMemoryHistory(args) { return [note: "Not available on-hub; use /hub/advanced/freeOSMemoryHistory directly."] }

def toolListDevices(args) { return allowedDevices().collect { deviceSummary(it) } }
def toolGetDevice(args) {
    def dev = findDevice(args.deviceId)
    return dev ? deviceDetail(dev) : errorMap("Device not found or not authorized")
}
def toolGetDeviceEvents(args) {
    def dev = findDevice(args.deviceId)
    if (!dev) return errorMap("Device not found or not authorized")
    int n = (args.count ?: 20) as int
    return dev.events(max: n).collect {
        [name: it.name, value: it.value, date: it.date?.toString(), source: it.source]
    }
}
def toolGetDeviceCommands(args) {
    def dev = findDevice(args.deviceId)
    if (!dev) return errorMap("Device not found or not authorized")
    return dev.supportedCommands.collect {
        [command: it.name, arguments: it.arguments?.collect { a -> a.name }]
    }
}
def toolSendDeviceCommand(args) {
    def dev = findDevice(args.deviceId)
    if (!dev) return errorMap("Device not found or not authorized")
    String cmd = args.command
    List values = (args.values ?: []) as List
    if (!cmd) return errorMap("command is required")
    try {
        if (values.isEmpty()) {
            dev."${cmd}"()
        } else {
            dev."${cmd}"(*values)
        }
        return [success: true, deviceId: dev.id, command: cmd, values: values]
    } catch (Throwable t) {
        return errorMap("Command failed: ${t.message}")
    }
}

def toolListModes(args) {
    def current = location.currentMode?.id
    return [
        modes: location.modes.collect { [id: it.id, name: it.name, active: it.id == current] },
        currentModeId: current
    ]
}
def toolSetMode(args) {
    def mode = location.modes.find { it.id == (args.modeId as Long) }
    if (!mode) return errorMap("Mode not found: ${args.modeId}")
    location.setMode(mode.name)
    return [success: true, mode: mode.name]
}

def toolGetHsmStatus(args) { return [hsmStatus: location.hsmStatus] }
def toolSetHsm(args) {
    String s = args.status
    if (!s) return errorMap("status is required")
    sendLocationEvent(name: "hsmSetArm", value: s)
    return [success: true, requested: s]
}

def toolListRooms(args) {
    def rooms = []
    try { rooms = getAllRoomsJson() } catch (e) { /* best-effort */ }
    return rooms
}
def toolListRoomsWithDevices(args) {
    def rooms = [:]
    allowedDevices().each { d ->
        String r = d.roomName ?: "Unassigned"
        if (!rooms[r]) rooms[r] = []
        rooms[r] << deviceSummary(d)
    }
    return rooms.collect { k, v -> [room: k, devices: v] }
}

def toolListHubVariables(args) {
    try {
        return getAllGlobalVars().collect { name, m ->
            [name: name, value: m.value, type: m.type]
        }
    } catch (e) { return [] }
}
def toolGetHubVariable(args) {
    def v = getGlobalVar(args.name)
    return v ? [name: args.name, value: v.value, type: v.type] : errorMap("Variable not found")
}
def toolSetHubVariable(args) {
    def ok = setGlobalVar(args.name, args.value)
    return [success: ok, name: args.name, value: args.value]
}

def toolGetLocation(args) {
    return [
        name: location.name,
        timeZone: location.timeZone?.ID,
        latitude: location.latitude,
        longitude: location.longitude,
        temperatureScale: location.temperatureScale,
        sunrise: location.sunrise?.toString(),
        sunset: location.sunset?.toString(),
        currentMode: location.currentMode?.name
    ]
}

def toolGetZigbeeDetails(args) { return [note: "Use direct endpoint /hub/zigbeeDetails/json"] }
def toolGetZigbeeTopology(args) { return [note: "Use direct endpoint /hub/zigbee/getChildAndRouteInfoJson"] }
def toolGetZwaveDetails(args) { return [note: "Use direct endpoint /hub/zwaveDetails/json"] }

def toolListInstalledApps(args) {
    return getChildApps().collect { [id: it.id, label: it.label, name: it.name] }
}
def toolGetInstalledAppStatus(args) { return [note: "Use direct endpoint /installedapp/statusJson/${args.appId}"] }

def toolGetLogs(args) { return [note: "Use direct endpoint /logs/past/json"] }
def toolGetDeviceStatistics(args) { return [note: "Use direct endpoint /logs/json"] }
def toolGetHubEvents(args) { return [note: "Use direct endpoint /hub/eventsJson"] }
def toolGetNetworkConfig(args) { return [note: "Use direct endpoint /hub2/networkConfiguration"] }
def toolListDashboards(args) { return [note: "Use direct endpoint /dashboard/all"] }
def toolListLocalBackups(args) { return [note: "Use direct endpoint /hub2/localBackups"] }
def toolListDrivers(args) { return [note: "Use direct endpoint /driver/list/data"] }
def toolListAppTypes(args) { return [note: "Use direct endpoint /app/list/data"] }
def toolGetAppSource(args) { return [note: "Use direct endpoint /app/ajax/code?id=${args.appId}"] }
def toolGetDriverSource(args) { return [note: "Use direct endpoint /driver/ajax/code?id=${args.driverId}"] }

// ---------------------------------------------------------------------------
// REST handlers (thin wrappers around tool handlers)
// ---------------------------------------------------------------------------

def apiHubDetails()       { renderJson(toolGetHubDetails([:])) }
def apiHubStatus()        { renderJson(toolGetHubStatus([:])) }
def apiHubMemory()        { renderJson(toolGetHubMemory([:])) }
def apiHubTemperature()   { renderJson(toolGetHubTemperature([:])) }
def apiHubDatabaseSize()  { renderJson(toolGetHubDatabaseSize([:])) }
def apiListDevices()      { renderJson(toolListDevices([:])) }
def apiGetDevice()        { renderJson(toolGetDevice(deviceId: params.deviceId as Long)) }
def apiGetDeviceEvents()  { renderJson(toolGetDeviceEvents(deviceId: params.deviceId as Long, count: (params.count ?: "20") as Integer)) }
def apiGetDeviceCommands(){ renderJson(toolGetDeviceCommands(deviceId: params.deviceId as Long)) }
def apiSendDeviceCommand(){
    Map body = (request.JSON ?: [:]) as Map
    renderJson(toolSendDeviceCommand(deviceId: params.deviceId as Long,
        command: body.command, values: body.values ?: []))
}
def apiListModes()        { renderJson(toolListModes([:])) }
def apiSetMode()          { renderJson(toolSetMode(modeId: params.modeId as Long)) }
def apiGetHsm()           { renderJson(toolGetHsmStatus([:])) }
def apiSetHsm()           { renderJson(toolSetHsm(status: params.status)) }
def apiListRooms()        { renderJson(toolListRooms([:])) }
def apiListRoomsWithDevices(){ renderJson(toolListRoomsWithDevices([:])) }
def apiListHubVariables() { renderJson(toolListHubVariables([:])) }
def apiGetHubVariable()   { renderJson(toolGetHubVariable(name: params.name)) }
def apiSetHubVariable()   {
    Map body = (request.JSON ?: [:]) as Map
    renderJson(toolSetHubVariable(name: params.name, value: body.value))
}
def apiGetLocation()      { renderJson(toolGetLocation([:])) }
def apiZigbeeDetails()    { renderJson(toolGetZigbeeDetails([:])) }
def apiZigbeeTopology()   { renderJson(toolGetZigbeeTopology([:])) }
def apiZwaveDetails()     { renderJson(toolGetZwaveDetails([:])) }
def apiListApps()         { renderJson(toolListInstalledApps([:])) }
def apiGetApp()           { renderJson(toolGetInstalledAppStatus(appId: params.appId as Long)) }
def apiGetLogs()          { renderJson(toolGetLogs(sourceType: params.sourceType, sourceId: params.sourceId)) }
def apiGetDeviceStats()   { renderJson(toolGetDeviceStatistics([:])) }
def apiNetwork()          { renderJson(toolGetNetworkConfig([:])) }
def apiListDrivers()      { renderJson(toolListDrivers([:])) }

private void renderJson(data) {
    render contentType: "application/json", data: JsonOutput.toJson(data), status: 200
}

// ---------------------------------------------------------------------------
// Helpers
// ---------------------------------------------------------------------------

private List allowedDevices() {
    return (settings.selectedDevices ?: []) as List
}

private findDevice(id) {
    if (id == null) return null
    Long wanted = id as Long
    return allowedDevices().find { it.id as Long == wanted }
}

private Map deviceSummary(dev) {
    return [
        id: dev.id as Long,
        label: dev.displayName,
        name: dev.name,
        type: dev.typeName,
        roomName: dev.roomName,
        disabled: dev.isDisabled()
    ]
}

private Map deviceDetail(dev) {
    Map summary = deviceSummary(dev)
    summary.capabilities = dev.capabilities?.collect { it.name }
    summary.currentStates = dev.currentStates?.collect {
        [name: it.name, value: it.value, unit: it.unit]
    }
    summary.commands = dev.supportedCommands?.collect { it.name }
    return summary
}

private Map collectHubDetails() {
    def hub = location.hubs?.getAt(0)
    return [
        name: location.name,
        hubName: hub?.name,
        ipAddress: hub?.localIP,
        firmwareVersion: hub?.firmwareVersionString,
        hardwareID: hub?.hardwareID,
        zigbeeEui: hub?.zigbeeEui,
        zigbeeChannel: hub?.zigbeeChannel,
        temperatureScale: location.temperatureScale,
        timeZone: location.timeZone?.ID
    ]
}

private Map collectHubStatus() {
    def hub = location.hubs?.getAt(0)
    return [
        status: hub?.status,
        mode: location.currentMode?.name,
        hsm: location.hsmStatus,
        deviceCount: allowedDevices().size()
    ]
}

private Number safeGetNum(Closure c) {
    try { def v = c(); return v == null ? null : (v as Number) } catch (e) { return null }
}

private Map errorMap(String msg) {
    return [error: true, message: msg]
}

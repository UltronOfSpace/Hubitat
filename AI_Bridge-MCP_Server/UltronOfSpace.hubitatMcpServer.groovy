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

        // Step 3 & 4 — Follow the instructions for that AI
        if (settings.aiClient) {
            renderAiInstructions(settings.aiClient)
            renderExamplePrompts()
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
    // ChatGPT lives in the cloud. Without cloud access, it literally can't reach the hub.
    if (!hasCloud) {
        renderRegisterHubCard("ChatGPT runs in the cloud, so it needs a public URL to reach your hub. Your hub isn't registered yet — this is free and takes about 2 minutes.")
        return
    }

    section("⚠️ Requires ChatGPT Plus") {
        paragraph "ChatGPT Custom GPT Actions are only available on a <b>ChatGPT Plus subscription</b> (\$20/month). The free tier can't create Custom GPTs. If you're on free ChatGPT, pick <b>Claude Desktop</b> above instead."
    }

    section("Step 3: Copy this URL") {
        paragraph buildBigCopyBox(cloudUrl)
    }

    section("Step 4: Create a Custom GPT in ChatGPT") {
        paragraph """
<b>Note:</b> You must do this on a <b>computer</b> (chat.openai.com). The iPhone/Android ChatGPT apps cannot create Custom GPTs, but once created here, you can use your GPT from any device.
<ol style='line-height:1.9;font-size:14px'>
  <li>On your computer, go to <a href='https://chat.openai.com' target='_blank'>chat.openai.com</a></li>
  <li>In the left sidebar, click <b>GPTs</b> → <b>+ Create</b></li>
  <li>Click the <b>Configure</b> tab at the top</li>
  <li>Scroll down to the bottom, click <b>Create new action</b></li>
  <li>Under <b>Schema</b>, click <b>Import from URL</b> and paste the URL from Step 3</li>
  <li>Scroll up to <b>Authentication</b> (near the top), click the gear icon</li>
  <li>Choose <b>API Key</b> as the Authentication Type</li>
  <li>Paste your access token:<br>
      ${buildBigCopyBox(token)}
  </li>
  <li>Leave <b>Auth Type</b> as <b>Custom</b>. ChatGPT will use the OpenAPI spec to know where to put the token — you don't need to set anything else.</li>
  <li>Click <b>Save</b></li>
  <li>Back at the top, click <b>Create</b> — give your GPT a name like "My Smart Home", save</li>
</ol>
<div style='background:#e8f5e9;border-left:4px solid #27ae60;padding:10px;border-radius:4px;margin-top:8px'>
  <b>✅ Setup complete.</b> Now say things like <i>"turn on the kitchen light"</i> in any chat with that GPT — from your phone, computer, anywhere.
</div>
""".toString()
    }
}

private void renderClaudeInstructions(String cloudUrl, String localUrl, boolean hasCloud) {
    String urlToUse = hasCloud ? cloudUrl : localUrl

    section("Step 3: Copy this URL") {
        paragraph buildBigCopyBox(urlToUse)
    }

    section("Before you start: do you have Node.js?") {
        paragraph """
Claude Desktop uses a small helper called <code>npx</code> to bridge to your hub, which comes with <b>Node.js</b> (free).
<ul style='line-height:1.7;font-size:14px'>
  <li><b>Already have it?</b> Open a terminal (Command Prompt on Windows, Terminal on Mac), type <code>npx --version</code>. If a number shows up, you're set.</li>
  <li><b>Don't have it?</b> Grab it from <a href='https://nodejs.org' target='_blank'>nodejs.org</a> — takes ~2 minutes. Use the LTS version.</li>
</ul>
<small style='color:#666'>Without Node.js, Claude Desktop will show "MCP server failed to start" with no other hint.</small>
""".toString()
    }

    section("Step 4: Paste into Claude Desktop's config") {
        paragraph """
<ol style='line-height:1.9;font-size:14px'>
  <li>Open <b>Claude Desktop</b> on your computer</li>
  <li>Open Settings:<br>
      <small>Windows: hamburger menu (☰ top-left) → <b>File</b> → <b>Settings</b></small><br>
      <small>Mac: <b>Claude</b> menu → <b>Settings</b></small>
  </li>
  <li>Click <b>Developer</b> in the left sidebar</li>
  <li>Click <b>Edit Config</b> — opens a JSON file in your text editor</li>
  <li>Find the <code>mcpServers</code> section (or create one if empty) and <b>merge</b> this entry into it (don't replace existing servers):</li>
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
  <li>Completely quit Claude Desktop:<br>
      <small>Windows: right-click Claude in the system tray → <b>Quit</b> (closing the window isn't enough)</small><br>
      <small>Mac: <b>Cmd+Q</b> in Claude</small>
  </li>
  <li>Reopen Claude Desktop. Look for a small hammer / tools icon near the chat box — that means it connected.</li>
</ol>
<div style='background:#e8f5e9;border-left:4px solid #27ae60;padding:10px;border-radius:4px;margin-top:8px'>
  <b>✅ Setup complete.</b> Ask: <i>"What devices do I have?"</i> or <i>"Turn on the kitchen light"</i>.
  Claude will ask you to approve each tool the first time it uses it.
</div>
""".toString()
    }

    if (!hasCloud) {
        section {
            paragraph """
<div style='background:#fff4e6;border-left:4px solid #f39c12;padding:10px;border-radius:4px'>
<b>Note:</b> your hub isn't registered with Hubitat cloud, so Claude Desktop can only reach it when you're <b>on your home Wi-Fi</b>.
If you use your laptop away from home and want it to still work, go to <b>Settings → Hub Details → Register Hub</b> (free, ~2 minutes), then reload this page and copy the new URL.
</div>
""".toString()
        }
    }
}

private void renderGrokInstructions(String cloudUrl, String localUrl, boolean hasCloud, String token) {
    if (!hasCloud) {
        renderRegisterHubCard("Grok runs in the cloud and needs a public URL to reach your hub. Your hub isn't registered yet — this is free and takes about 2 minutes.")
        return
    }

    section("⚠️ Grok OpenAPI support is still evolving") {
        paragraph "Grok's custom-tool / external-action feature has been changing frequently. These instructions are a best guess — if Grok's UI doesn't match, try <b>Claude Desktop</b> instead (very reliable) or <b>ChatGPT</b> (very reliable with ChatGPT Plus)."
    }

    section("Step 3: Copy this URL") {
        paragraph buildBigCopyBox(cloudUrl)
    }

    section("Step 4: Paste into Grok") {
        paragraph """
Grok doesn't currently have a standard OpenAPI import in its UI. When they add one:
<ol style='line-height:1.9;font-size:14px'>
  <li>Open Grok's settings or custom tools / actions / integrations area</li>
  <li>Paste the URL from Step 3 as an OpenAPI source</li>
  <li>When asked for an API key, paste:<br>
      ${buildBigCopyBox(token)}
  </li>
</ol>
<small style='color:#666'>If you can't find the right menu, we recommend switching to Claude Desktop or ChatGPT Plus — both have tested, first-class support.</small>
""".toString()
    }
}

private void renderGeminiInstructions(String cloudUrl, String localUrl, boolean hasCloud, String token) {
    if (!hasCloud) {
        renderRegisterHubCard("Gemini runs in the cloud and needs a public URL to reach your hub. Your hub isn't registered yet — this is free and takes about 2 minutes.")
        return
    }

    section("⚠️ Gemini doesn't support arbitrary OpenAPI tools on the free tier") {
        paragraph """
Google's Gemini currently doesn't have a general-purpose "paste an OpenAPI URL" feature on the free tier. Gemini <b>Extensions</b> is a curated first-party list (Gmail, Drive, etc.) — you can't add custom tools to it. <b>Gems</b> (the custom-assistant feature) does allow some tool integration but it's evolving and the UI varies.
<br><br>
<b>Our recommendation:</b> use <b>Claude Desktop</b> (free, rock-solid) or <b>ChatGPT Plus</b> (\$20/mo, very reliable) instead. When Gemini adds OpenAPI support, this guide will be updated.
"""
    }

    section("Step 3: Copy this URL (for when you need it)") {
        paragraph buildBigCopyBox(cloudUrl)
    }

    section("Access token") {
        paragraph "If Gemini asks for an API key, paste:"
        paragraph buildBigCopyBox(token)
    }
}

private void renderRegisterHubCard(String reason) {
    section("⚠️ One-time step: register your hub with Hubitat's free cloud") {
        paragraph """
<div style='background:#fdf2f2;border-left:4px solid #e74c3c;padding:12px;border-radius:4px'>
${reason}
<br><br>
<b>How:</b>
<ol style='line-height:1.7'>
  <li>In Hubitat, go to <b>Settings → Hub Details</b></li>
  <li>Click <b>Register Hub</b></li>
  <li>Create a free Hubitat account (or sign in to an existing one). You'll need to confirm your email.</li>
  <li>Come back to this page and reload it. The cloud URL will appear automatically.</li>
</ol>
<small>This is Hubitat's built-in free cloud relay. It's the same mechanism Maker API uses. You do <b>not</b> need Hub Protect (\$30/year) — that's a different, separate subscription for cloud backups.</small>
</div>
""".toString()
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

private void renderExamplePrompts() {
    section("Step 5: What to say to your AI") {
        paragraph """
<div style='background:#f0f7ff;border-left:4px solid #3498db;padding:12px;border-radius:4px;margin-bottom:10px'>
<b>🗣️ Your AI is now room-aware and capability-aware.</b> It knows every device you picked, what room it's in, and what it can do. Talk to it like a person.
</div>

<b>🏠 Control lights, switches, outlets</b>
<ul style='line-height:1.6;color:#222'>
  <li><i>"Turn on the kitchen light"</i></li>
  <li><i>"Turn off everything in the living room"</i></li>
  <li><i>"Dim the bedroom lamp to 30%"</i></li>
  <li><i>"Make the accent lights red"</i></li>
  <li><i>"Set the kitchen bulbs to warm white"</i></li>
  <li><i>"Turn off all the lights in the house"</i></li>
</ul>

<b>🔎 Ask about your home</b>
<ul style='line-height:1.6;color:#222'>
  <li><i>"What devices do I have?"</i></li>
  <li><i>"What's on right now?"</i></li>
  <li><i>"Is anyone home?"</i> (checks motion/presence)</li>
  <li><i>"What's the temperature in the kitchen?"</i></li>
  <li><i>"Are any doors unlocked?"</i></li>
  <li><i>"Show me every device that hasn't checked in today"</i></li>
</ul>

<b>🔐 Security, modes, scenes</b>
<ul style='line-height:1.6;color:#222'>
  <li><i>"Arm the security system"</i> (HSM armAway)</li>
  <li><i>"Set mode to Night"</i></li>
  <li><i>"Lock the front door"</i></li>
  <li><i>"What mode is the house in?"</i></li>
</ul>

<b>🌡️ Hub diagnostics (power user stuff)</b>
<ul style='line-height:1.6;color:#222'>
  <li><i>"What's my hub's temperature and memory usage?"</i></li>
  <li><i>"Show me my Zigbee mesh"</i></li>
  <li><i>"How many events has my kitchen outlet fired today?"</i></li>
</ul>

<b>💡 Pro tips</b>
<ul style='line-height:1.6;color:#444;font-size:13px'>
  <li>The AI refers to your devices by their <b>labels</b> — whatever you called them in Hubitat. So "kitchen light" works if that's what you named it.</li>
  <li>Room names come from Hubitat's room assignments. Use <b>Rooms</b> in your hub UI to organize devices for better natural language control.</li>
  <li>The AI can chain commands: <i>"Set the house to bedtime — turn off everything, lock all doors, and set mode to Night"</i></li>
  <li>You can ask it to verify: <i>"Turn off the TV and confirm it's off"</i></li>
  <li>The AI <b>cannot</b> reboot the hub, delete devices, disable radios, or do anything destructive — we didn't expose those capabilities on purpose.</li>
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
    String localUrl = getBaseUrl()
    String cloudUrl = getCloudBaseUrl()
    Map spec = generateOpenApiSpec(cloudUrl, localUrl, "AI Bridge - MCP Server")
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
def toolGetHubMemory(args) {
    def raw = fetchHubText("/hub/advanced/freeOSMemory")
    return [freeOSMemoryKB: parseNumeric(raw)]
}
def toolGetHubTemperature(args) {
    def raw = fetchHubText("/hub/advanced/internalTempCelsius")
    return [temperatureC: parseNumeric(raw)]
}
def toolGetHubDatabaseSize(args) {
    def raw = fetchHubText("/hub/advanced/databaseSize")
    return [databaseMB: parseNumeric(raw)]
}
def toolGetHubMemoryHistory(args) {
    def raw = fetchHubText("/hub/advanced/freeOSMemoryHistory")
    return [csv: raw]
}

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

    // Validate that the device actually supports this command. Prevents calling
    // driver internals or arbitrary methods via the AI.
    if (!dev.hasCommand(cmd)) {
        List available = dev.supportedCommands?.collect { it.name }?.unique()?.sort() ?: []
        return errorMap("Command '${cmd}' not supported. Available: ${available.join(', ')}")
    }

    // Coerce numeric strings to numbers. Without this, setLevel("50") throws
    // MissingMethodException because the driver expects an Integer, not a String.
    List coerced = values.collect { v -> coerceArg(v) }

    try {
        if (coerced.isEmpty()) {
            dev."${cmd}"()
        } else {
            dev."${cmd}"(*coerced)
        }
        return [success: true, deviceId: dev.id, command: cmd, values: coerced]
    } catch (Throwable t) {
        return errorMap("Command failed: ${t.message}")
    }
}

private coerceArg(v) {
    if (v == null) return null
    if (v instanceof Number || v instanceof Boolean || v instanceof Map) return v
    String s = v.toString()
    if (s.isInteger()) return s.toInteger()
    if (s.isBigDecimal() || s.isDouble()) return s.toBigDecimal()
    if (s.equalsIgnoreCase("true")) return true
    if (s.equalsIgnoreCase("false")) return false
    return s
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
    // Prefer the internal endpoint. Fall back to deriving rooms from the
    // allowed device list if the hub endpoint isn't reachable.
    def fromHub = fetchHubJson("/room/listRoomsJson")
    if (fromHub != null && !(fromHub instanceof Map && fromHub.error)) return fromHub
    return allowedDevices()
        .collect { it.roomName }
        .findAll { it }
        .unique()
        .collect { [name: it] }
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

def toolGetZigbeeDetails(args) { return fetchHubJson("/hub/zigbeeDetails/json") }
def toolGetZigbeeTopology(args) { return fetchHubJson("/hub/zigbee/getChildAndRouteInfoJson") }
def toolGetZwaveDetails(args) { return fetchHubJson("/hub/zwaveDetails/json") }

def toolListInstalledApps(args) { return fetchHubJson("/installedapp/list/data") }
def toolGetInstalledAppStatus(args) {
    if (!args.appId) return errorMap("appId is required")
    return fetchHubJson("/installedapp/statusJson/${args.appId}")
}

def toolGetLogs(args) {
    String path = "/logs/past/json"
    List qs = []
    if (args.sourceType && args.sourceType != "all") qs << "type=${args.sourceType}"
    if (args.sourceId) qs << "id=${args.sourceId}"
    if (qs) path += "?" + qs.join("&")
    return fetchHubJson(path)
}
def toolGetDeviceStatistics(args) { return fetchHubJson("/logs/json") }
def toolGetHubEvents(args) { return fetchHubJson("/hub/eventsJson") }
def toolGetNetworkConfig(args) { return fetchHubJson("/hub2/networkConfiguration") }
def toolListDashboards(args) { return fetchHubJson("/dashboard/all") }
def toolListLocalBackups(args) { return fetchHubJson("/hub2/localBackups") }
def toolListDrivers(args) { return fetchHubJson("/driver/list/data") }
def toolListAppTypes(args) { return fetchHubJson("/app/list/data") }
def toolGetAppSource(args) {
    if (!args.appId) return errorMap("appId is required")
    return fetchHubJson("/app/ajax/code?id=${args.appId}")
}
def toolGetDriverSource(args) {
    if (!args.driverId) return errorMap("driverId is required")
    return fetchHubJson("/driver/ajax/code?id=${args.driverId}")
}

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

// Loopback HTTP call to the hub's own web server. Used to fetch data from
// internal endpoints (/hub/..., /hub2/..., /device/..., etc.) that don't have
// a direct Groovy API. These endpoints are unauthenticated on localhost.
private fetchHubJson(String path) {
    try {
        def result = null
        httpGet([uri: "http://127.0.0.1:8080${path}", contentType: "application/json", timeout: 10]) { resp ->
            result = resp.data
        }
        return result
    } catch (e) {
        return errorMap("Hub endpoint ${path} failed: ${e.message}")
    }
}

private String fetchHubText(String path) {
    // The hub returns plain text values but with Content-Type: text/html,
    // so textParser: true is required to avoid Groovy trying to parse HTML.
    try {
        String result = null
        httpGet([uri: "http://127.0.0.1:8080${path}", textParser: true, timeout: 10]) { resp ->
            result = resp.data?.text
        }
        return result?.trim()
    } catch (e) {
        log.warn "fetchHubText(${path}) failed: ${e.message}"
        return null
    }
}

private Number parseNumeric(String s) {
    if (!s) return null
    String trimmed = s.trim()
    if (trimmed.isInteger()) return trimmed.toInteger()
    if (trimmed.isBigDecimal() || trimmed.isDouble()) return trimmed.toBigDecimal()
    return null
}

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

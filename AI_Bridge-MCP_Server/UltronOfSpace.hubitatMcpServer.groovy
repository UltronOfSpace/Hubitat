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
            return dynamicPage(name: "mainPage", title: "OAuth Required", install: false) {
                section {
                    paragraph "Enable OAuth in the app's code (Apps Code → AI Bridge - MCP Server → OAuth) before installing."
                }
            }
        }
    }

    dynamicPage(name: "mainPage", title: "AI Bridge - MCP Server", install: true, uninstall: true) {
        section("Devices") {
            paragraph "Pick the devices the AI can see and control."
            input name: "selectedDevices", type: "capability.*",
                  title: "Devices", multiple: true, required: false
        }

        section("Access") {
            input name: "logging", type: "bool",
                  title: "Enable debug logging", defaultValue: false
        }

        if (app.installationState == "COMPLETE" && state.accessToken) {
            String base = getBaseUrl()
            String token = state.accessToken

            section("Endpoint URLs (copy these into your AI client)") {
                paragraph buildUrlPanel("MCP endpoint (Claude Desktop, Cursor)",
                    "${base}/mcp?access_token=${token}")
                paragraph buildUrlPanel("OpenAPI spec (ChatGPT Custom GPT, Grok, Gemini)",
                    "${base}/openapi.json?access_token=${token}")
                paragraph buildUrlPanel("Access token",
                    token)
            }

            section("Setup guides") {
                paragraph "<b>Claude Desktop:</b> Settings → Developer → Edit Config. Add an MCP server entry with command <code>npx</code>, args <code>[\"mcp-remote\", \"&lt;MCP endpoint URL&gt;\"]</code>. Restart Claude."
                paragraph "<b>ChatGPT:</b> Create a new Custom GPT → Actions → Import from URL → paste the OpenAPI URL. Select <i>API Key</i> auth with the access token in a query parameter named <code>access_token</code>."
                paragraph "<b>Grok / Gemini:</b> Use their \"custom tool\" or \"action\" feature. Paste the OpenAPI URL. Same auth."
            }
        }
    }
}

private String getBaseUrl() {
    return "${getFullLocalApiServerUrl()}"
}

private String buildUrlPanel(String label, String value) {
    return "<b>${label}:</b><br><code style='word-break:break-all'>${value}</code>"
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

/**
 *  MCP Protocol Library
 *
 *  JSON-RPC 2.0 dispatcher, OpenAPI 3.1.0 spec generator, and tool schema registry.
 *  Shared by the Hubitat MCP Server app.
 */

library(
    name: "mcpProtocol",
    namespace: "UltronOfSpace",
    author: "Ultronumus Of Space",
    contributor: "Claude (Anthropic)",
    description: "MCP (Model Context Protocol) JSON-RPC + OpenAPI 3.1.0 helpers for Hubitat",
    category: "Integrations",
    importUrl: "https://raw.githubusercontent.com/UltronOfSpace/Hubitat/main/AI_Bridge-MCP_Server/UltronOfSpace.mcpProtocol.groovy"
)

import groovy.json.JsonOutput
import groovy.json.JsonSlurper

// ---------------------------------------------------------------------------
// Tool registry
// ---------------------------------------------------------------------------
//
// Each tool descriptor has:
//   name         - snake_case identifier
//   description  - human-readable (also used by ChatGPT to pick actions)
//   inputSchema  - JSON Schema for arguments (MCP tools/list)
//   openApi      - OpenAPI path/method/params (or null for MCP-only)
//   handler      - method name on the app to invoke

def getToolRegistry() {
    return [
        // ---- Hub info ----
        [name: "get_hub_details", description: "Hub identity: name, firmware, model, MAC, location.",
         inputSchema: emptyObjSchema(),
         openApi: [path: "/api/hub/details", method: "get"],
         handler: "toolGetHubDetails"],

        [name: "get_hub_status", description: "Hub running status, alerts, safe mode, database size.",
         inputSchema: emptyObjSchema(),
         openApi: [path: "/api/hub/status", method: "get"],
         handler: "toolGetHubStatus"],

        [name: "get_hub_memory", description: "Free OS memory in KB.",
         inputSchema: emptyObjSchema(),
         openApi: [path: "/api/hub/memory", method: "get"],
         handler: "toolGetHubMemory"],

        [name: "get_hub_temperature", description: "Hub internal temperature in Celsius.",
         inputSchema: emptyObjSchema(),
         openApi: [path: "/api/hub/temperature", method: "get"],
         handler: "toolGetHubTemperature"],

        [name: "get_hub_database_size", description: "Database size in MB.",
         inputSchema: emptyObjSchema(),
         openApi: [path: "/api/hub/database", method: "get"],
         handler: "toolGetHubDatabaseSize"],

        [name: "get_hub_memory_history",
         description: "Memory/CPU history CSV (~3.7 days at 5-min intervals).",
         inputSchema: emptyObjSchema(),
         openApi: null, // MCP-only
         handler: "toolGetHubMemoryHistory"],

        // ---- Devices ----
        [name: "list_devices",
         description: "List all devices the server is authorized to access (configured in app).",
         inputSchema: emptyObjSchema(),
         openApi: [path: "/api/devices", method: "get"],
         handler: "toolListDevices"],

        [name: "get_device",
         description: "Get detailed info for a single device (state, capabilities, commands).",
         inputSchema: objSchema([
             deviceId: [type: "integer", description: "The device ID"]
         ], ["deviceId"]),
         openApi: [path: "/api/devices/{deviceId}", method: "get",
                   pathParams: [[name: "deviceId", type: "integer"]]],
         handler: "toolGetDevice"],

        [name: "get_device_events",
         description: "Recent event history for a device.",
         inputSchema: objSchema([
             deviceId: [type: "integer", description: "The device ID"],
             count: [type: "integer", description: "Max events (default 20)"]
         ], ["deviceId"]),
         openApi: [path: "/api/devices/{deviceId}/events", method: "get",
                   pathParams: [[name: "deviceId", type: "integer"]],
                   queryParams: [[name: "count", type: "integer", required: false]]],
         handler: "toolGetDeviceEvents"],

        [name: "get_device_commands",
         description: "Available commands for a device (on, off, setLevel, setColor, etc).",
         inputSchema: objSchema([
             deviceId: [type: "integer", description: "The device ID"]
         ], ["deviceId"]),
         openApi: [path: "/api/devices/{deviceId}/commands", method: "get",
                   pathParams: [[name: "deviceId", type: "integer"]]],
         handler: "toolGetDeviceCommands"],

        [name: "send_device_command",
         description: "Send a command to a device. Examples: command='on', command='setLevel' values=['50'], command='setColorTemperature' values=['2700','80','3'].",
         inputSchema: objSchema([
             deviceId: [type: "integer", description: "The device ID"],
             command: [type: "string", description: "Command name"],
             values: [type: "array", items: [type: "string"],
                      description: "Positional command values"]
         ], ["deviceId", "command"]),
         openApi: [path: "/api/devices/{deviceId}/command", method: "post",
                   pathParams: [[name: "deviceId", type: "integer"]],
                   bodySchema: [
                     type: "object",
                     properties: [
                       command: [type: "string"],
                       values: [type: "array", items: [type: "string"]]
                     ],
                     required: ["command"]
                   ]],
         handler: "toolSendDeviceCommand"],

        // ---- Modes ----
        [name: "list_modes", description: "List all hub modes with currently active mode.",
         inputSchema: emptyObjSchema(),
         openApi: [path: "/api/modes", method: "get"],
         handler: "toolListModes"],

        [name: "set_mode",
         description: "Activate a hub mode by ID.",
         inputSchema: objSchema([
             modeId: [type: "integer", description: "Mode ID to activate"]
         ], ["modeId"]),
         openApi: [path: "/api/modes/{modeId}", method: "post",
                   pathParams: [[name: "modeId", type: "integer"]]],
         handler: "toolSetMode"],

        // ---- HSM ----
        [name: "get_hsm_status", description: "Current Hubitat Safety Monitor status.",
         inputSchema: emptyObjSchema(),
         openApi: [path: "/api/hsm", method: "get"],
         handler: "toolGetHsmStatus"],

        [name: "set_hsm",
         description: "Set HSM: armAway, armHome, armNight, disarm, disarmAll, armRules, disarmRules, cancelAlerts.",
         inputSchema: objSchema([
             status: [type: "string", description: "HSM arm state"]
         ], ["status"]),
         openApi: [path: "/api/hsm/{status}", method: "post",
                   pathParams: [[name: "status", type: "string"]]],
         handler: "toolSetHsm"],

        // ---- Rooms ----
        [name: "list_rooms", description: "List all rooms with IDs.",
         inputSchema: emptyObjSchema(),
         openApi: [path: "/api/rooms", method: "get"],
         handler: "toolListRooms"],

        [name: "list_rooms_with_devices",
         description: "Rooms with their devices and current states.",
         inputSchema: emptyObjSchema(),
         openApi: [path: "/api/rooms/devices", method: "get"],
         handler: "toolListRoomsWithDevices"],

        // ---- Hub variables ----
        [name: "list_hub_variables", description: "All hub variables and values.",
         inputSchema: emptyObjSchema(),
         openApi: [path: "/api/variables", method: "get"],
         handler: "toolListHubVariables"],

        [name: "get_hub_variable",
         description: "Get a specific hub variable value.",
         inputSchema: objSchema([
             name: [type: "string", description: "Variable name"]
         ], ["name"]),
         openApi: [path: "/api/variables/{name}", method: "get",
                   pathParams: [[name: "name", type: "string"]]],
         handler: "toolGetHubVariable"],

        [name: "set_hub_variable",
         description: "Set a hub variable value.",
         inputSchema: objSchema([
             name: [type: "string", description: "Variable name"],
             value: [type: "string", description: "New value"]
         ], ["name", "value"]),
         openApi: [path: "/api/variables/{name}", method: "post",
                   pathParams: [[name: "name", type: "string"]],
                   bodySchema: [
                     type: "object",
                     properties: [value: [type: "string"]],
                     required: ["value"]
                   ]],
         handler: "toolSetHubVariable"],

        // ---- Location ----
        [name: "get_location",
         description: "Location: name, timezone, coordinates, sunrise/sunset, current mode.",
         inputSchema: emptyObjSchema(),
         openApi: [path: "/api/location", method: "get"],
         handler: "toolGetLocation"],

        // ---- Zigbee ----
        [name: "get_zigbee_details",
         description: "Zigbee radio details: channel, PAN ID, network state, device list.",
         inputSchema: emptyObjSchema(),
         openApi: [path: "/api/zigbee", method: "get"],
         handler: "toolGetZigbeeDetails"],

        [name: "get_zigbee_topology",
         description: "Zigbee mesh topology: children, neighbors with LQI, routes.",
         inputSchema: emptyObjSchema(),
         openApi: [path: "/api/zigbee/topology", method: "get"],
         handler: "toolGetZigbeeTopology"],

        // ---- Z-Wave ----
        [name: "get_zwave_details",
         description: "Z-Wave radio details and device nodes.",
         inputSchema: emptyObjSchema(),
         openApi: [path: "/api/zwave", method: "get"],
         handler: "toolGetZwaveDetails"],

        // ---- Apps ----
        [name: "list_installed_apps", description: "All installed app instances.",
         inputSchema: emptyObjSchema(),
         openApi: [path: "/api/apps", method: "get"],
         handler: "toolListInstalledApps"],

        [name: "get_installed_app_status",
         description: "Detailed installed app status (settings, state, subscriptions, scheduled jobs).",
         inputSchema: objSchema([
             appId: [type: "integer", description: "Installed app ID"]
         ], ["appId"]),
         openApi: [path: "/api/apps/{appId}", method: "get",
                   pathParams: [[name: "appId", type: "integer"]]],
         handler: "toolGetInstalledAppStatus"],

        // ---- Logs ----
        [name: "get_logs",
         description: "Past log entries, optionally filtered by source type and ID.",
         inputSchema: objSchema([
             sourceType: [type: "string", description: "all, dev, app, or sys"],
             sourceId: [type: "integer", description: "Device or app ID"]
         ], []),
         openApi: [path: "/api/logs", method: "get",
                   queryParams: [[name: "sourceType", type: "string", required: false],
                                 [name: "sourceId", type: "integer", required: false]]],
         handler: "toolGetLogs"],

        [name: "get_device_statistics",
         description: "Device event statistics: counts, state sizes, uptime.",
         inputSchema: emptyObjSchema(),
         openApi: [path: "/api/logs/stats", method: "get"],
         handler: "toolGetDeviceStatistics"],

        [name: "get_hub_events",
         description: "Hub-level events (system start, etc).",
         inputSchema: emptyObjSchema(),
         openApi: null, // MCP-only (less commonly needed)
         handler: "toolGetHubEvents"],

        // ---- Network ----
        [name: "get_network_config",
         description: "Network: IP, gateway, DNS, WiFi, LAN settings.",
         inputSchema: emptyObjSchema(),
         openApi: [path: "/api/network", method: "get"],
         handler: "toolGetNetworkConfig"],

        // ---- Dashboards / backups ----
        [name: "list_dashboards",
         description: "Legacy dashboards.",
         inputSchema: emptyObjSchema(),
         openApi: null, // MCP-only
         handler: "toolListDashboards"],

        [name: "list_local_backups",
         description: "Local backup files with version, size, creation time.",
         inputSchema: emptyObjSchema(),
         openApi: null, // MCP-only
         handler: "toolListLocalBackups"],

        // ---- Drivers / code ----
        [name: "list_drivers",
         description: "All installed drivers with capabilities.",
         inputSchema: emptyObjSchema(),
         openApi: [path: "/api/drivers", method: "get"],
         handler: "toolListDrivers"],

        [name: "list_app_types",
         description: "All app types (system + user).",
         inputSchema: emptyObjSchema(),
         openApi: null, // MCP-only
         handler: "toolListAppTypes"],

        [name: "get_app_source",
         description: "Groovy source code for a user app.",
         inputSchema: objSchema([
             appId: [type: "integer", description: "App type ID"]
         ], ["appId"]),
         openApi: null, // MCP-only
         handler: "toolGetAppSource"],

        [name: "get_driver_source",
         description: "Groovy source code for a user driver.",
         inputSchema: objSchema([
             driverId: [type: "integer", description: "Driver type ID"]
         ], ["driverId"]),
         openApi: null, // MCP-only
         handler: "toolGetDriverSource"],
    ]
}

// ---------------------------------------------------------------------------
// Schema builders
// ---------------------------------------------------------------------------

def emptyObjSchema() {
    return [type: "object", properties: [:]]
}

def objSchema(Map properties, List required) {
    Map schema = [type: "object", properties: properties]
    if (required) schema.required = required
    return schema
}

// ---------------------------------------------------------------------------
// JSON-RPC dispatcher
// ---------------------------------------------------------------------------
//
// Call from app's POST /mcp handler. Delegates tool/call to the app via
// callers passing a closure/method reference.

def handleJsonRpc(Map req, Closure invokeTool) {
    String method = req?.method
    def id = req?.id

    // Notifications (no id, no response)
    if (method == "notifications/initialized") {
        return null
    }

    switch (method) {
        case "initialize":
            return rpcResult(id, [
                protocolVersion: "2025-03-26",
                capabilities: [tools: [:]],
                serverInfo: [
                    name: "hubitat-mcp",
                    version: "1.0.0"
                ]
            ])

        case "ping":
            return rpcResult(id, [:])

        case "tools/list":
            def tools = getToolRegistry().collect { t ->
                [name: t.name, description: t.description, inputSchema: t.inputSchema]
            }
            return rpcResult(id, [tools: tools])

        case "tools/call":
            String toolName = req?.params?.name
            Map args = (req?.params?.arguments ?: [:]) as Map
            def descriptor = getToolRegistry().find { it.name == toolName }
            if (!descriptor) {
                return rpcError(id, -32602, "Unknown tool: ${toolName}")
            }
            try {
                def result = invokeTool(descriptor.handler, args)
                String text = (result instanceof String) ? result : JsonOutput.toJson(result)
                return rpcResult(id, [content: [[type: "text", text: text]]])
            } catch (Throwable t) {
                return rpcError(id, -32603, "Tool error: ${t.message}")
            }

        default:
            return rpcError(id, -32601, "Method not found: ${method}")
    }
}

def rpcResult(id, result) {
    return [jsonrpc: "2.0", id: id, result: result]
}

def rpcError(id, int code, String message) {
    return [jsonrpc: "2.0", id: id, error: [code: code, message: message]]
}

// ---------------------------------------------------------------------------
// OpenAPI 3.1.0 generator
// ---------------------------------------------------------------------------

def generateOpenApiSpec(String serverUrl, String appTitle) {
    Map paths = [:]
    getToolRegistry().findAll { it.openApi != null }.each { t ->
        Map oa = t.openApi
        String p = oa.path
        String m = oa.method
        if (!paths[p]) paths[p] = [:]
        paths[p][m] = buildOperation(t, oa)
    }

    return [
        openapi: "3.1.0",
        info: [
            title: appTitle ?: "Hubitat MCP Server",
            description: "Control and query your Hubitat Elevation hub from AI assistants.",
            version: "1.0.0"
        ],
        servers: [[url: serverUrl]],
        components: [
            securitySchemes: [
                access_token: [
                    type: "apiKey",
                    in: "query",
                    name: "access_token"
                ]
            ]
        ],
        security: [[access_token: []]],
        paths: paths
    ]
}

private Map buildOperation(Map tool, Map oa) {
    Map op = [
        operationId: toCamelCase(tool.name),
        summary: tool.description,
        description: tool.description,
        responses: [
            "200": [
                description: "Success",
                content: ["application/json": [schema: [type: "object"]]]
            ]
        ]
    ]

    List parameters = []
    oa.pathParams?.each { pp ->
        parameters << [
            name: pp.name,
            "in": "path",
            required: true,
            schema: [type: pp.type]
        ]
    }
    oa.queryParams?.each { qp ->
        parameters << [
            name: qp.name,
            "in": "query",
            required: qp.required != false,
            schema: [type: qp.type]
        ]
    }
    if (parameters) op.parameters = parameters

    if (oa.bodySchema) {
        op.requestBody = [
            required: true,
            content: [
                "application/json": [schema: oa.bodySchema]
            ]
        ]
    }

    return op
}

private String toCamelCase(String snake) {
    def parts = snake.split("_")
    StringBuilder sb = new StringBuilder(parts[0])
    for (int i = 1; i < parts.size(); i++) {
        sb.append(parts[i].capitalize())
    }
    return sb.toString()
}

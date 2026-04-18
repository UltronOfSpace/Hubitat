#!/usr/bin/env bash
# Integration tests for the AI Bridge - MCP Server Hubitat app.
#
# Hits a live hub, exercises the MCP JSON-RPC endpoint, the OpenAPI spec
# endpoint, and a curated subset of REST endpoints. Reports pass/fail.
#
# See tests/README.md for setup and the "what's not tested" list.

set -uo pipefail

# ---------------------------------------------------------------------------
# Config
# ---------------------------------------------------------------------------

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"

# Optional .env at tests/.env (gitignored by parent .gitignore).
if [[ -f "$SCRIPT_DIR/.env" ]]; then
    set -a
    # shellcheck disable=SC1091
    source "$SCRIPT_DIR/.env"
    set +a
fi

VERBOSE=0
DRY_RUN=0
for arg in "$@"; do
    case "$arg" in
        -v|--verbose) VERBOSE=1 ;;
        --dry-run)    DRY_RUN=1 ;;
        -h|--help)
            cat <<EOF
Usage: $0 [-v|--verbose] [--dry-run]

Required env vars:
  MCP_BASE_URL   e.g. http://192.168.1.139/apps/api/123
  MCP_TOKEN      access token from the app preferences

Optional env vars:
  TEST_DEVICE_ID  device id safe to send 'refresh' to (enables command tests)
  MCP_ADMIN_STATE auto|on|off (default: auto)
EOF
            exit 0 ;;
    esac
done

if [[ -z "${MCP_BASE_URL:-}" ]]; then
    echo "ERROR: MCP_BASE_URL is not set." >&2
    echo "       Set it in tests/.env or export it before running. See tests/README.md." >&2
    exit 2
fi
if [[ -z "${MCP_TOKEN:-}" ]]; then
    echo "ERROR: MCP_TOKEN is not set." >&2
    echo "       Set it in tests/.env or ~/.claude/secrets/.env. See tests/README.md." >&2
    exit 2
fi

MCP_BASE_URL="${MCP_BASE_URL%/}"
MCP_ADMIN_STATE="${MCP_ADMIN_STATE:-auto}"

# Full set of admin tool names (must match `admin: true` entries in
# UltronOfSpace.mcpProtocol.groovy::getToolRegistry).
ADMIN_TOOLS=(
    list_installed_apps get_installed_app_status
    get_app_source get_driver_source
    list_drivers list_app_types
    list_hub_variables get_hub_variable set_hub_variable
    get_logs get_device_statistics get_hub_events
    get_network_config
    get_zigbee_details get_zigbee_topology get_zwave_details
    list_dashboards list_local_backups
)

# Counters
PASS=0
FAIL=0
SKIP=0
FAIL_LOG=()

# ---------------------------------------------------------------------------
# TTY-aware color helpers
# ---------------------------------------------------------------------------

if [[ -t 1 ]]; then
    C_RED='\033[31m'; C_GREEN='\033[32m'; C_YELLOW='\033[33m'
    C_BOLD='\033[1m'; C_DIM='\033[2m'; C_RESET='\033[0m'
else
    C_RED=''; C_GREEN=''; C_YELLOW=''; C_BOLD=''; C_DIM=''; C_RESET=''
fi

# ---------------------------------------------------------------------------
# HTTP helpers (curl wrappers, capture body + status separately)
# ---------------------------------------------------------------------------

# Globals set by each call:
#   HTTP_BODY    response body
#   HTTP_STATUS  HTTP status code
#   HTTP_OK      "1" if curl succeeded, "0" if connection failed

http_post_json() {
    local url="$1" body="$2"
    local tmp; tmp="$(mktemp)"
    HTTP_STATUS=$(curl -s -o "$tmp" -w "%{http_code}" \
        -X POST -H "Content-Type: application/json" \
        --max-time 15 \
        -d "$body" "$url" 2>/dev/null)
    local rc=$?
    HTTP_BODY="$(cat "$tmp")"
    rm -f "$tmp"
    if [[ $rc -ne 0 || -z "$HTTP_STATUS" ]]; then HTTP_OK=0; else HTTP_OK=1; fi
    [[ $VERBOSE -eq 1 ]] && {
        echo -e "${C_DIM}  POST $url${C_RESET}" >&2
        echo -e "${C_DIM}    body: $body${C_RESET}" >&2
        echo -e "${C_DIM}    -> $HTTP_STATUS, ${HTTP_BODY:0:200}${C_RESET}" >&2
    }
}

http_get() {
    local url="$1"
    local tmp; tmp="$(mktemp)"
    HTTP_STATUS=$(curl -s -o "$tmp" -w "%{http_code}" \
        --max-time 15 "$url" 2>/dev/null)
    local rc=$?
    HTTP_BODY="$(cat "$tmp")"
    rm -f "$tmp"
    if [[ $rc -ne 0 || -z "$HTTP_STATUS" ]]; then HTTP_OK=0; else HTTP_OK=1; fi
    [[ $VERBOSE -eq 1 ]] && {
        echo -e "${C_DIM}  GET $url${C_RESET}" >&2
        echo -e "${C_DIM}    -> $HTTP_STATUS, ${HTTP_BODY:0:200}${C_RESET}" >&2
    }
}

# ---------------------------------------------------------------------------
# MCP / REST URL builders
# ---------------------------------------------------------------------------

mcp_url()     { echo "${MCP_BASE_URL}/mcp?access_token=${MCP_TOKEN}"; }
openapi_url() { echo "${MCP_BASE_URL}/openapi.json?access_token=${MCP_TOKEN}"; }
rest_url()    { echo "${MCP_BASE_URL}$1?access_token=${MCP_TOKEN}"; }

mcp_call() {
    local method="$1" params="${2:-null}"
    local body
    body=$(printf '{"jsonrpc":"2.0","id":1,"method":"%s","params":%s}' "$method" "$params")
    http_post_json "$(mcp_url)" "$body"
}

mcp_call_raw() {
    local body="$1"
    http_post_json "$(mcp_url)" "$body"
}

# Convenience: shape a tools/call payload. Default args = empty object.
tools_call_payload() {
    local name="$1"
    local args="${2:-}"
    [[ -z "$args" ]] && args='{}'
    printf '{"name":"%s","arguments":%s}' "$name" "$args"
}

# ---------------------------------------------------------------------------
# Assertion helpers
# ---------------------------------------------------------------------------

contains() {
    # contains "$haystack" "needle"
    case "$1" in *"$2"*) return 0 ;; *) return 1 ;; esac
}

pass() {
    PASS=$((PASS+1))
    echo -e "  ${C_GREEN}PASS${C_RESET}  $1"
}

fail() {
    FAIL=$((FAIL+1))
    echo -e "  ${C_RED}FAIL${C_RESET}  $1"
    [[ -n "${2:-}" ]] && echo -e "        ${C_DIM}$2${C_RESET}"
    FAIL_LOG+=("$1")
}

skip() {
    SKIP=$((SKIP+1))
    echo -e "  ${C_YELLOW}SKIP${C_RESET}  $1"
    [[ -n "${2:-}" ]] && echo -e "        ${C_DIM}$2${C_RESET}"
}

section() {
    echo
    echo -e "${C_BOLD}$1${C_RESET}"
}

# ---------------------------------------------------------------------------
# Dry-run short-circuit
# ---------------------------------------------------------------------------

if [[ $DRY_RUN -eq 1 ]]; then
    cat <<EOF
DRY RUN — would test against:
  MCP_BASE_URL  = $MCP_BASE_URL
  MCP_TOKEN     = (length ${#MCP_TOKEN})
  TEST_DEVICE_ID= ${TEST_DEVICE_ID:-(unset, command tests will be skipped)}
  ADMIN_STATE   = $MCP_ADMIN_STATE

Test groups: protocol, discovery, admin gate, non-admin happy path, validation.
No requests will be made.
EOF
    exit 0
fi

# ---------------------------------------------------------------------------
# Connectivity smoke test
# ---------------------------------------------------------------------------

echo -e "${C_BOLD}Probing $MCP_BASE_URL ...${C_RESET}"
mcp_call "ping"
if [[ $HTTP_OK -ne 1 ]]; then
    echo -e "${C_RED}ERROR: cannot reach $MCP_BASE_URL — curl failed.${C_RESET}" >&2
    echo "        Check the URL, the hub is on, and your laptop is on the same network." >&2
    exit 3
fi
if [[ "$HTTP_STATUS" != "200" ]]; then
    echo -e "${C_RED}ERROR: ping returned HTTP $HTTP_STATUS.${C_RESET}" >&2
    echo "        Body: ${HTTP_BODY:0:400}" >&2
    echo "        Likely causes: wrong token, OAuth not enabled, app not installed." >&2
    exit 3
fi
echo -e "  ${C_GREEN}reachable${C_RESET}"

# ---------------------------------------------------------------------------
# Detect admin state
# ---------------------------------------------------------------------------

mcp_call "tools/list"
TOOLS_LIST_BODY="$HTTP_BODY"

case "$MCP_ADMIN_STATE" in
    on|off) DETECTED_ADMIN="$MCP_ADMIN_STATE" ;;
    auto)
        if contains "$TOOLS_LIST_BODY" "list_installed_apps"; then
            DETECTED_ADMIN="on"
        else
            DETECTED_ADMIN="off"
        fi ;;
    *)
        echo "ERROR: MCP_ADMIN_STATE must be auto, on, or off (got $MCP_ADMIN_STATE)" >&2
        exit 2 ;;
esac
echo -e "  detected admin state: ${C_BOLD}$DETECTED_ADMIN${C_RESET}"

# ---------------------------------------------------------------------------
# Group 1 — JSON-RPC protocol
# ---------------------------------------------------------------------------

section "Group 1 — JSON-RPC protocol"

mcp_call "initialize"
if [[ "$HTTP_STATUS" == "200" ]] \
   && contains "$HTTP_BODY" '"protocolVersion":"2025-03-26"' \
   && contains "$HTTP_BODY" '"name":"hubitat-mcp"'; then
    pass "initialize returns protocolVersion + serverInfo"
else
    fail "initialize returns protocolVersion + serverInfo" "got status=$HTTP_STATUS body=${HTTP_BODY:0:200}"
fi

mcp_call "ping"
if [[ "$HTTP_STATUS" == "200" ]] && contains "$HTTP_BODY" '"result":{}'; then
    pass "ping returns empty result"
else
    fail "ping returns empty result" "got status=$HTTP_STATUS body=${HTTP_BODY:0:200}"
fi

# notifications/initialized has no id and the server returns 204 with no body
mcp_call_raw '{"jsonrpc":"2.0","method":"notifications/initialized","params":{}}'
if [[ "$HTTP_STATUS" == "204" && -z "$HTTP_BODY" ]]; then
    pass "notifications/initialized returns 204 no body"
else
    fail "notifications/initialized returns 204 no body" "got status=$HTTP_STATUS body=${HTTP_BODY:0:200}"
fi

mcp_call "foo/bar"
if contains "$HTTP_BODY" '"code":-32601' && contains "$HTTP_BODY" "Method not found"; then
    pass "unknown method returns -32601"
else
    fail "unknown method returns -32601" "got body=${HTTP_BODY:0:200}"
fi

# ---------------------------------------------------------------------------
# Group 2 — Discovery
# ---------------------------------------------------------------------------

section "Group 2 — Discovery"

mcp_call "tools/list"
if [[ "$HTTP_STATUS" == "200" ]] \
   && contains "$HTTP_BODY" '"tools":' \
   && contains "$HTTP_BODY" "list_devices" \
   && contains "$HTTP_BODY" "send_device_command"; then
    pass "tools/list returns expected non-admin tools"
else
    fail "tools/list returns expected non-admin tools" "body=${HTTP_BODY:0:300}"
fi

# set_hsm enum check (regression test for Change 5)
if contains "$HTTP_BODY" "set_hsm" \
   && contains "$HTTP_BODY" '"armAway"' \
   && contains "$HTTP_BODY" '"cancelAlerts"'; then
    pass "set_hsm inputSchema includes enum of valid arm states"
else
    fail "set_hsm inputSchema includes enum" "set_hsm enum not found in tools/list response"
fi

http_get "$(openapi_url)"
if [[ "$HTTP_STATUS" == "200" ]] \
   && contains "$HTTP_BODY" '"openapi":"3.1.0"' \
   && contains "$HTTP_BODY" '"paths"' \
   && contains "$HTTP_BODY" '"securitySchemes"'; then
    pass "GET /openapi.json returns valid OpenAPI 3.1.0"
else
    fail "GET /openapi.json returns valid OpenAPI 3.1.0" "status=$HTTP_STATUS body=${HTTP_BODY:0:300}"
fi
OPENAPI_BODY="$HTTP_BODY"

# ---------------------------------------------------------------------------
# Group 3 — Admin gate
# ---------------------------------------------------------------------------

section "Group 3 — Admin gate (state: $DETECTED_ADMIN)"

if [[ "$DETECTED_ADMIN" == "off" ]]; then
    # 3a. tools/list must not contain any admin tool
    mcp_call "tools/list"
    missing_count=0
    for t in "${ADMIN_TOOLS[@]}"; do
        contains "$HTTP_BODY" "\"$t\"" && missing_count=$((missing_count+1))
    done
    if [[ $missing_count -eq 0 ]]; then
        pass "tools/list excludes all 18 admin tools"
    else
        fail "tools/list excludes all 18 admin tools" "$missing_count admin tool name(s) leaked into the list"
    fi

    # 3b. tools/call on an admin tool returns the gate error
    mcp_call "tools/call" "$(tools_call_payload list_installed_apps)"
    if contains "$HTTP_BODY" "administrative tool" || contains "$HTTP_BODY" "administrative tools"; then
        pass "tools/call name=list_installed_apps rejected with admin gate error"
    else
        fail "tools/call admin gate error" "body=${HTTP_BODY:0:300}"
    fi

    mcp_call "tools/call" "$(tools_call_payload get_app_source '{"appId":1}')"
    if contains "$HTTP_BODY" "administrative tool" || contains "$HTTP_BODY" "administrative tools"; then
        pass "tools/call name=get_app_source rejected with admin gate error"
    else
        fail "tools/call get_app_source admin gate" "body=${HTTP_BODY:0:300}"
    fi

    # 3c. REST endpoints return 403
    for path in /api/apps /api/logs /api/variables /api/zigbee /api/zwave /api/network /api/drivers; do
        http_get "$(rest_url "$path")"
        if [[ "$HTTP_STATUS" == "403" ]] && contains "$HTTP_BODY" "Administrative tools are disabled"; then
            pass "GET $path returns 403 with admin-disabled message"
        else
            fail "GET $path returns 403 with admin-disabled message" "status=$HTTP_STATUS body=${HTTP_BODY:0:200}"
        fi
    done

    # 3d. OpenAPI spec excludes admin paths.
    # Use prefix match (no closing quote) so both /api/apps and /api/apps/{appId} are caught.
    leaked=0
    for prefix in '"/api/apps' '"/api/logs' '"/api/variables' '"/api/zigbee' '"/api/zwave' '"/api/network' '"/api/drivers'; do
        contains "$OPENAPI_BODY" "$prefix" && leaked=$((leaked+1))
    done
    if [[ $leaked -eq 0 ]]; then
        pass "OpenAPI spec excludes all admin paths"
    else
        fail "OpenAPI spec excludes all admin paths" "$leaked admin path prefix(es) leaked into spec"
    fi

    skip "ON-suite tests" "admin is OFF — enable the toggle in app preferences and re-run to test"
else
    # admin ON
    found=0
    for t in "${ADMIN_TOOLS[@]}"; do
        contains "$TOOLS_LIST_BODY" "\"$t\"" && found=$((found+1))
    done
    if [[ $found -eq ${#ADMIN_TOOLS[@]} ]]; then
        pass "tools/list includes all ${#ADMIN_TOOLS[@]} admin tools"
    else
        fail "tools/list includes all admin tools" "$found / ${#ADMIN_TOOLS[@]} admin tools present"
    fi

    mcp_call "tools/call" "$(tools_call_payload list_installed_apps)"
    if [[ "$HTTP_STATUS" == "200" ]] && ! contains "$HTTP_BODY" "administrative tool"; then
        pass "tools/call list_installed_apps succeeds"
    else
        fail "tools/call list_installed_apps succeeds" "status=$HTTP_STATUS body=${HTTP_BODY:0:200}"
    fi

    http_get "$(rest_url /api/apps)"
    if [[ "$HTTP_STATUS" == "200" ]]; then
        pass "GET /api/apps returns 200"
    else
        fail "GET /api/apps returns 200" "status=$HTTP_STATUS body=${HTTP_BODY:0:200}"
    fi

    if contains "$OPENAPI_BODY" '"/api/apps"' && contains "$OPENAPI_BODY" '"/api/drivers"'; then
        pass "OpenAPI spec includes admin paths"
    else
        fail "OpenAPI spec includes admin paths" "expected /api/apps and /api/drivers"
    fi

    skip "OFF-suite tests" "admin is ON — disable the toggle in app preferences and re-run to test"
fi

# ---------------------------------------------------------------------------
# Group 4 — Non-admin happy path (always)
# ---------------------------------------------------------------------------

section "Group 4 — Non-admin happy path"

# tool name -> substring expected in result (loose check)
declare -a NON_ADMIN_TOOLS=(
    "list_devices|content"
    "get_hub_status|status"
    "get_hub_details|firmwareVersion"
    "get_location|timeZone"
    "list_modes|currentModeId"
    "list_rooms|content"
    "get_hsm_status|hsmStatus"
)
for entry in "${NON_ADMIN_TOOLS[@]}"; do
    name="${entry%%|*}"
    expect="${entry##*|}"
    mcp_call "tools/call" "$(tools_call_payload "$name")"
    if [[ "$HTTP_STATUS" == "200" ]] \
       && contains "$HTTP_BODY" '"result"' \
       && contains "$HTTP_BODY" "$expect" \
       && ! contains "$HTTP_BODY" "administrative tool"; then
        pass "tools/call $name returns result containing $expect"
    else
        fail "tools/call $name returns result containing $expect" "status=$HTTP_STATUS body=${HTTP_BODY:0:200}"
    fi
done

for path in /api/devices /api/hub/details /api/modes; do
    http_get "$(rest_url "$path")"
    if [[ "$HTTP_STATUS" == "200" ]]; then
        pass "GET $path returns 200"
    else
        fail "GET $path returns 200" "status=$HTTP_STATUS"
    fi
done

# ---------------------------------------------------------------------------
# Group 5 — Validation (the hardening fixes)
# ---------------------------------------------------------------------------

section "Group 5 — Validation"

# HSM whitelist — DOES NOT actually arm anything
mcp_call "tools/call" "$(tools_call_payload set_hsm '{"status":"definitely_not_real"}')"
if contains "$HTTP_BODY" "Invalid HSM status" && contains "$HTTP_BODY" "armAway"; then
    pass "set_hsm rejects invalid status with valid-list message"
else
    fail "set_hsm rejects invalid status" "body=${HTTP_BODY:0:300}"
fi

# parseLongArg — non-numeric deviceId returns clean error, not 500
mcp_call "tools/call" "$(tools_call_payload get_device '{"deviceId":"abc"}')"
if [[ "$HTTP_STATUS" == "200" ]] \
   && contains "$HTTP_BODY" "Device not found" \
   && ! contains "$HTTP_BODY" "GroovyCastException"; then
    pass "get_device with non-numeric ID returns clean error"
else
    fail "get_device with non-numeric ID returns clean error" "status=$HTTP_STATUS body=${HTTP_BODY:0:300}"
fi

# URL encoding (only meaningful when admin is ON — get_logs is admin-gated)
if [[ "$DETECTED_ADMIN" == "on" ]]; then
    mcp_call "tools/call" "$(tools_call_payload get_logs '{"sourceType":"bad&injected=value"}')"
    if [[ "$HTTP_STATUS" == "200" ]] && ! contains "$HTTP_BODY" "GroovyCastException"; then
        pass "get_logs with injection-y sourceType does not 500"
    else
        fail "get_logs with injection-y sourceType does not 500" "status=$HTTP_STATUS body=${HTTP_BODY:0:200}"
    fi
else
    skip "URL encoding (get_logs)" "admin is OFF; toggle ON to run this"
fi

# Device command validation — needs a real device id
if [[ -n "${TEST_DEVICE_ID:-}" ]]; then
    mcp_call "tools/call" "$(tools_call_payload send_device_command "{\"deviceId\":$TEST_DEVICE_ID,\"command\":\"definitelyFakeMethod\"}")"
    if contains "$HTTP_BODY" "not supported"; then
        pass "send_device_command rejects unsupported command name"
    else
        fail "send_device_command rejects unsupported command name" "body=${HTTP_BODY:0:300}"
    fi

    mcp_call "tools/call" "$(tools_call_payload send_device_command "{\"deviceId\":$TEST_DEVICE_ID,\"command\":\"refresh\"}")"
    if contains "$HTTP_BODY" '"success":true'; then
        pass "send_device_command refresh returns success"
    else
        fail "send_device_command refresh returns success" "body=${HTTP_BODY:0:300}"
    fi
else
    skip "send_device_command validation" "TEST_DEVICE_ID not set"
fi

# ---------------------------------------------------------------------------
# Summary
# ---------------------------------------------------------------------------

TOTAL=$((PASS + FAIL + SKIP))
echo
echo -e "${C_BOLD}Summary${C_RESET}"
echo -e "  Ran ${TOTAL} tests: ${C_GREEN}${PASS} passed${C_RESET}, ${C_RED}${FAIL} failed${C_RESET}, ${C_YELLOW}${SKIP} skipped${C_RESET}"

if [[ $FAIL -gt 0 ]]; then
    echo
    echo -e "${C_RED}${C_BOLD}Failures:${C_RESET}"
    for f in "${FAIL_LOG[@]}"; do
        echo "  - $f"
    done
    exit 1
fi
exit 0

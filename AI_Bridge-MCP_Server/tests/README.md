# AI Bridge MCP Server — Integration Tests

Alright boys — live-hub HTTP integration tests. This thing hits a
running Hubitat hub with the AI Bridge MCP Server installed and asserts
on responses.

Hubitat has no native unit-test framework, and this app's behavior is
deeply tied to the hub runtime — so testing against a real hub is the
realistic option. **Always run against the sandbox hub (.139), never
production (.20).**

---

## Setup

### 1. Install the app on the sandbox hub

If not already installed:

1. Run [build.ps1](../build.ps1) to produce `dist/AI_Bridge-MCP_Server.zip`.
2. In the sandbox hub UI: **Apps Code → Import** the zip.
3. **Apps → Add User App → AI Bridge - MCP Server**.
4. Walk the setup: select a few test devices, enable OAuth when prompted.
5. Open the app and copy the **MCP base URL** (without the `/mcp` suffix
   and without the `?access_token=...`) and the **access token**.

### 2. Configure the test runner

Create [tests/.env](.env) (gitignored — it'll match `*.env` in the parent
[`.gitignore`](../../.gitignore)):

```bash
MCP_BASE_URL=http://192.168.1.139/apps/api/123
MCP_TOKEN=paste-the-token-here
TEST_DEVICE_ID=42        # optional — a real device id safe to refresh
```

The `123` in the URL is the app's installed-app ID — it's part of the
URL the app shows you. Find it in the app's preferences page.

Per [`~/.claude/CLAUDE.md`](~/.claude/CLAUDE.md) secrets rules, the
canonical place for the token is `~/.claude/secrets/.env`. The test
script reads `tests/.env` if it exists, but you can also `export
MCP_TOKEN=...` from the secrets file before running.

### 3. Run

```bash
cd ~/Code/Hubitat/AI_Bridge-MCP_Server
./tests/integration_test.sh
```

Flags:
- `-v` / `--verbose` — print every request and response (truncated to 200 chars)
- `--dry-run` — print what would be tested without making any requests
- `-h` / `--help` — usage

---

## What's tested

| Group | Coverage |
| ----- | -------- |
| 1 — JSON-RPC protocol | `initialize`, `ping`, `notifications/initialized` (204), unknown method (`-32601`) |
| 2 — Discovery | `tools/list` shape, `set_hsm` enum present, OpenAPI 3.1.0 spec valid |
| 3 — Admin gate | OFF state: 18 admin tools hidden, REST endpoints return 403, OpenAPI excludes admin paths. ON state: all admin tools present, REST returns 200, OpenAPI includes admin paths |
| 4 — Non-admin happy path | `list_devices`, `get_hub_status`, `get_hub_details`, `get_location`, `list_modes`, `list_rooms`, `get_hsm_status`, plus matching REST endpoints |
| 5 — Validation | HSM whitelist rejects bogus status, non-numeric `deviceId` returns clean error (no `GroovyCastException`), URL-encoded `get_logs` survives injection-y input, `send_device_command` rejects unknown commands, `refresh` succeeds |

The script auto-detects whether admin is ON or OFF by looking for
`list_installed_apps` in `tools/list`, then runs the appropriate suite.
To exercise **both** suites, run once, toggle the **Allow administrative
tools** preference in the app, then run again.

---

## What's NOT tested (intentionally)

These would have side effects on the live hub. Run them manually if
you want to verify them.

- **HSM arm/disarm** — would actually trigger Hubitat Safety Monitor.
- **Mode change** — would actually change the hub's mode.
- **Hub variable write** — would mutate state visible to other automations.
- **Device commands beyond `refresh`** — could turn lights on/off, lock/unlock doors, etc.
- **Cloud URL routing** — tests use the local URL only.
- **OAuth flow** — assumes OAuth is already enabled on the app.
- **The Groovy Hubitat sandbox itself** — no way to test that
  `@Field static` or `private static final` work without installing.

---

## Exit codes

| Code | Meaning |
| ---- | ------- |
| 0 | All tests passed |
| 1 | One or more tests failed |
| 2 | Misconfiguration (missing env var, bad `MCP_ADMIN_STATE` value) |
| 3 | Connectivity failure (hub unreachable, wrong token, OAuth not enabled) |

---

## Troubleshooting

**"cannot reach $MCP_BASE_URL — curl failed"** — the URL is wrong, the
hub is off, or you're not on the same network. Sanity-check with
`curl -v "$MCP_BASE_URL/mcp?access_token=$MCP_TOKEN" -d '{}'`.

**"ping returned HTTP 401" or "404"** — wrong access token, or the app
isn't installed on this hub. Recopy the token from the app preferences.

**Lots of FAILs in Group 3** — you may have toggled admin without
re-running. The script detects state once at startup; re-run the script
after each toggle.

**`GroovyCastException` shows up in any response** — that's a real bug
in the app code. The validation tests in Group 5 specifically guard
against this.

**Test claims a non-admin tool is missing from `tools/list`** — make
sure you've installed the latest version of the app (the build that
includes the admin-tagged tool registry).

---

*Run the tests, keep the sandbox sweatin', and may the jalopy's warnin' lights stay off.*

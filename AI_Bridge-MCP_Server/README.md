# AI Bridge - MCP Server

A native Hubitat app that lets **any AI assistant** — Claude Desktop, ChatGPT Custom GPTs, Grok, Gemini, Cursor, and more — control your Hubitat hub directly. No external server, no computer running 24/7, no cloud service. Just one app on your hub.

Exposes **two protocols** from a single install:

- **MCP (Model Context Protocol)** — for Claude Desktop, Cursor, and other MCP-native clients
- **OpenAPI 3.1.0 spec** — for ChatGPT Custom GPTs, Grok Actions, Gemini extensions

Both are served over your hub's local network (and optionally the Hubitat cloud endpoint) using the hub's built-in OAuth token authentication.

---

## Install

### Option A — via Hubitat Package Manager (recommended)

1. In Hubitat, open **Hubitat Package Manager**.
2. Click **Install** → **From a URL** and paste:
   `https://raw.githubusercontent.com/UltronOfSpace/Hubitat/main/AI_Bridge-MCP_Server/packageManifest.json`
3. Go to **Apps** → **Add User App** → **AI Bridge - MCP Server**.

### Option B — direct bundle install

1. In Hubitat, go to **Bundles** → **Import ZIP** (or use **Import from URL**).
2. URL:
   `https://github.com/UltronOfSpace/Hubitat/raw/main/AI_Bridge-MCP_Server/dist/AI_Bridge-MCP_Server.zip`
3. Go to **Apps** → **Add User App** → **AI Bridge - MCP Server**.

---

## Configure

1. Pick the devices the AI should see and control (same pattern as Maker API).
2. Save. The app page will show two sets of URLs:
   - **🏠 Local URLs** — use from the same Wi-Fi as your hub (computer, phone on home network)
   - **☁️ Cloud URLs** — use from cloud-based AI apps (ChatGPT iPhone, Grok, Gemini). These go through Hubitat's **free** cloud relay.
3. Plus the access token (only needed if your client asks for it separately).

### Do I need a subscription?

**No.** The cloud relay URLs (`https://cloud.hubitat.com/api/...`) are free and built into every registered Hubitat hub. This is the same mechanism Maker API uses. You **don't need** Hub Protect ($30/year) — that covers cloud backups, which are separate.

If your hub isn't registered yet: **Settings → Hub Details → Register Hub** (free, one-time).

---

## AI client setup

### Claude Desktop

Claude Desktop speaks MCP natively but only supports stdio — use `mcp-remote` as a bridge. Edit the config:

- macOS: `~/Library/Application Support/Claude/claude_desktop_config.json`
- Windows: `%APPDATA%\Claude\claude_desktop_config.json`

```json
{
  "mcpServers": {
    "hubitat": {
      "command": "npx",
      "args": ["mcp-remote", "http://<YOUR_HUB_IP>/apps/api/<APP_ID>/mcp?access_token=<TOKEN>"]
    }
  }
}
```

Restart Claude. Try: *"What devices do I have?"* or *"Turn on the kitchen light."*

### ChatGPT Custom GPT

1. In ChatGPT, **Explore GPTs** → **Create a GPT**.
2. **Configure** → **Actions** → **Create new action**.
3. **Import from URL** — paste the **OpenAPI URL**.
4. **Authentication** → **API Key**:
   - Auth type: **Custom** (query parameter)
   - Parameter name: `access_token`
   - Value: the access token from your hub
5. Save the GPT. The action persists forever — every future chat with that GPT can control your hub.

### Grok

Use Grok's custom action feature. Paste the OpenAPI URL and the access token.

### Gemini

Use Gemini Extension / custom tool. Paste the OpenAPI URL with the same access token.

---

## What can the AI do?

**Your AI is fully room-aware and capability-aware.** Every device it sees includes its label, room, and what it can do. Talk to it like a person.

### Example prompts

**🏠 Control lights, switches, outlets**
- *"Turn on the kitchen light"*
- *"Turn off everything in the living room"*
- *"Dim the bedroom lamp to 30%"*
- *"Make the accent lights red"*
- *"Set the kitchen bulbs to warm white"*
- *"Turn off all the lights in the house"*

**🔎 Ask about your home**
- *"What devices do I have?"*
- *"What's on right now?"*
- *"Is anyone home?"* (checks motion/presence sensors)
- *"What's the temperature in the kitchen?"*
- *"Are any doors unlocked?"*
- *"Show me every device that hasn't checked in today"*

**🔐 Security, modes, scenes**
- *"Arm the security system"* (HSM armAway)
- *"Set mode to Night"*
- *"Lock the front door"*
- *"What mode is the house in?"*

**🌡️ Hub diagnostics (power user stuff)**
- *"What's my hub's temperature and memory usage?"*
- *"Show me my Zigbee mesh"*
- *"How many events has my kitchen outlet fired today?"*

### Pro tips

- The AI refers to your devices by their **labels** — whatever you named them in Hubitat. So "kitchen light" works if that's what you called it.
- **Room awareness comes from Hubitat's room assignments.** Organize devices into rooms in your hub UI for better natural language control.
- The AI can chain commands: *"Set the house to bedtime — turn off everything, lock all doors, and set mode to Night"*
- You can ask it to verify: *"Turn off the TV and confirm it's off"*

### Under the hood: 33 tools

- **Devices** — list, get details, events, commands (on/off/setLevel/setColor/etc.)
- **Modes** — list, switch active mode
- **HSM** — status + arm/disarm
- **Rooms** — list with devices and states
- **Hub variables** — list, get, set
- **Hub info** — details, status, memory, temperature, database size
- **Location** — timezone, coordinates, sunrise/sunset, current mode
- **Logs** — past logs, device statistics
- **Zigbee / Z-Wave** — radio details, mesh topology
- **Apps & drivers** — list, inspect

Destructive operations (reboot, delete, disable radios, firmware update) are **not exposed** — the AI is sandboxed to safe operations only.

---

## Security

- Every request requires the OAuth `access_token` — no unauthenticated access.
- Only devices you explicitly picked in the app's configuration page are visible.
- Destructive hub operations are never exposed as tools.
- Uninstalling the app revokes the token.

For remote (cloud) access, use the hub's cloud endpoint URL in place of the local one, or use a tunnel like `mcp-remote`.

---

## Credits

- **Author:** Ultronumus Of Space (UltronOfSpace)
- **Collaborator:** Claude (Anthropic)

## License

MIT

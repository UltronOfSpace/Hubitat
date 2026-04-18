# Hubitat

Home automation tooling for [Hubitat Elevation](https://hubitat.com) hubs — three sub-projects that share a repo.

## Sub-projects

### [endpoints-wiki/](endpoints-wiki/)
Community documentation of every HTTP endpoint exposed by a Hubitat hub (~270 endpoints across 30+ sections, with safety indicators, response samples, and request format notes). Drafted for the [community wiki HTTP endpoints post](https://community.hubitat.com/t/wiki-http-features-and-endpoints/49141).

Contains:
- `hubitat-endpoints-wiki-post.md` — the full wiki document
- `preview/` — HTML render of the wiki + interactive device-viewer demo
- `analysis/` — firmware JS endpoint extraction artifacts (for diffing across Hubitat versions)

### [hubitat-mcp-app/](hubitat-mcp-app/)
**AI Bridge — MCP Server** — a Hubitat app that lets AI assistants (Claude, ChatGPT, Gemini) control your hub through the Model Context Protocol. Installable as a Hubitat bundle from the `dist/` folder.

### [he-tile-dashboard/](he-tile-dashboard/)
Tile-based dashboard SPA for Hubitat — responsive grid of tiles showing device status with tap-to-control. Self-hostable, no build step required.

## Private/local files

The following paths are gitignored on purpose and never leave your machine:

- `.claude/` — Claude Code configuration, `CLAUDE.md` with hub details, `.env` with API tokens
- `*.env`, `**/secrets.md`, `**/*.local.md` — general-purpose "keep this private" patterns
- `endpoints-wiki/scan/` — raw research data containing hub-specific PII

Drop a file matching any of those patterns anywhere in the tree and git will skip it automatically.

## License

MIT — see [LICENSE](LICENSE).

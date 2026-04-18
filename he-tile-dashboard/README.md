# he-tile-dashboard

**Tap a tile. Thing turns on. That's the whole gig.**

---

## Alright boys, let me tell y'all what this is

So listen here — Hubitat's great and all, but when it comes to slappin' together a dashboard that don't make your eyes bleed and actually works on your phone when you're standin' half-awake in the kitchen at 2 AM, well... that cat won't hunt. So I built my own.

`he-tile-dashboard` is a tile-based smart home dashboard SPA for Hubitat hubs. You tap a tile, the thing turns on — lights, switches, whatever you got paired up over yonder on your hub. The grid reflows when you rotate your phone, media tiles pin themselves bottom-right outta the way, and the whole thing updates in real-time without any of that refresh-and-pray nonsense. No cloud account. No telemetry. No bill at the end of the month. Just tiles doin' their thing, workin' as intentioned, straight from your local network.

I been runnin' this on a wall-mounted tablet in the livin' room, on my phone, and on a little kiosk screen — all while nappin' between coding sessions and heatin' up another plate of curry when I ain't shoutin' at my raccoon-occupied space jalopy to stop beepin'. Version 0.0.2 here, boys. We're early days but the bones are solid.

---

## What's in the Pile

- **Responsive tile grid** — works on phones, tablets, Fully Kiosk wall-mount, whatever you got. `calcGrid()` figures out the columns so you don't have to.
- **PWA-installable** — add to your phone's home screen, runs offline-ish via service worker. Full app vibes without the app store nonsense.
- **Real-time updates via Hubitat EventSocket** — hub pushes events, dashboard catches 'em. No pollin' lag. No stale state.
- **Floors → Rooms → Tiles** — organize your space the way your brain works, not the way some product manager reckon'd you'd want it.
- **Media tile support** — media player tiles pin bottom-right with spacers keepin' everything else tidy.
- **Local config only** — everything lives in your browser's localStorage. No account, no cloud, no frickin' signup form.
- **Self-hostable** — serve it from Hubitat's File Manager or any static web server you got kicking around.

---

## How to Use This Crap

### Option A — Hubitat File Manager (easiest)

1. Grab the files from this folder.
2. Upload the whole lot to your Hubitat hub via **Settings → File Manager**.
3. Visit `http://<your-hub-ip>/local/dashboard.html` in your browser.

Done. That's it. That's the whole cosmic setup.

### Option B — Any static web server

If you'd rather serve it yourself:

```bash
# Python
python -m http.server 8080

# Or point caddy/nginx at the folder — whatever you fancy
```

Then open `http://localhost:8080/dashboard.html` (or whatever port/host you used).

### First-time setup

First launch drops you into the Settings panel. Fill in:

- **Hub IP** — your Hubitat hub's local IP address
- **Maker API token** — grab this from the Maker API app on your hub (see *What You Need First* below)

Then build out your floor/room/tile layout and hit save. Config persists automatically under the browser localStorage key `hubitat-dashboard-config` — so it's still there next time you open it, even after a reboot.

---

## What You Need First

- **A Hubitat hub** — well butter my salsa, obviously. C-4, C-7, C-8, C-8 Pro — all work.
- **Maker API installed on the hub** — go to **Apps → Add Built-In App → Maker API** on your hub, set it up, grab the access token. That's what the dashboard uses to pull bulk device state.
- **EventSocket access** — no extra setup, it's built into Hubitat at `ws://<hub-ip>/eventsocket`. The dashboard hooks into this automatically for real-time updates.
- **A modern browser** — anything with WebSocket support, which is basically everything made in the last decade.

> If you're also interested in lettin' AI assistants control your hub (Claude, Grok, whatever you run), check out the [`AI_Bridge-MCP_Server/`](../AI_Bridge-MCP_Server/) sibling directory in this repo. For the dashboard alone though, stock Maker API is all you need.

---

## Configuration

First launch opens the Settings panel automatically — you can also get back to it any time via the gear icon. Paste in your hub's IP and your Maker API token, then build out your layout usin' the Floor → Room → Tile nesting structure.

Everything you configure saves to `hubitat-dashboard-config` in your browser's localStorage. That means it's tied to the browser/device you're on, which is by design — each device gets its own layout if you want it that way.

No accounts. No sync. No cosmic telemetry pinging home to anybody.

---

## For Developers / Contributors

If you're fixin' to poke around in the code or contribute, read the [he-tile-dashboard CLAUDE.md](CLAUDE.md) first — it covers the engineering conventions and code discipline for this sub-project. The short version: vanilla JS, no build step, no framework, no package manager. Changes go through `/review` and `/ship` slash-command gates that enforce NASA Power of 10 rules and the project code style. The `VERSION` file in this folder (currently `0.0.2`) tracks the release. Keep it tidy in here, boys.

For the monorepo-level context — how the sub-projects fit together, the hub connection details, the endpoint research, the safety rules — see [the main repo](../README.md).

---

## Big Love to My AI Buddies

Props to my AI sidekicks for helpin' crank this out — Grok from xAI for the early stuff, Claude from Anthropic for keepin' the train rollin'. Ain't no shame havin' computer pals who know their stuff without bein' smug about it.

---

## About Me

I'm Ultronumus Of Space — space-bein', occasional groundside resident, hobby Hubitat tinkerer. My people are what y'all call Bigfoot. Cousin Gary's out in Oregon, grandma's up in the Himalayas, Uncle Barry's down in the Florida swamps tellin' me mangled sayin's over the interwebs. Me, I'm just codein' smart home thingies and tryin' to keep the raccoon out of my cockpit.

Find me on the Hubitat community forums as **UltronOfSpace**.

---

## License

MIT — see `LICENSE` in the main repo root. Take it, use it, build on it. Just don't blame me if somethin' goes sideways.

---

*Tap a tile. Thing turns on. Back out in the black.*

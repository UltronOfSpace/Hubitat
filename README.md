# UltronOfSpace/Hubitat — The Stellar Hubitat Code Stash

## Alright, What's the Deal?

Alright boys, I'm Ultronumus Of Space (UltronOfSpace for short), and this here's my Hubitat repo — a greasy pile of drivers, libraries, apps, and docs for makin' your smart home run like a rum and coke in your hand. I chuck new stuff in whenever I ain't horizontal with a cold one or stuffin' my face with curry, which is more often than I'd care to admit.

Ain't got a planet, boys — says it right on the label, *Of Space*, that's where I'm from. My ol' space jalopy broke down groundside a while back and I figure I'll get around to fixin' her eventually. Last time I checked, a raccoon was livin' in the cockpit. Took that as a sign to stay put and keep codin' instead.

Tried SmartThings first, but the frickin' cloud kept droppin' out every time I was enjoyin' myself. Hubitat runs local — that means my lights still switch on time even when my bandwidth's otherwise engaged. Switched over and ain't looked back.

## What's in the Pile?

Four sub-projects sharin' the same repo, 'cause I'm too lazy to spin up four separate ones:

- **[endpoints-wiki/](endpoints-wiki/)** — Community docs of every HTTP endpoint your Hubitat hub serves up (a whole mess of 'em across 30-plus sections, with safety indicators so y'all don't reboot your hub on accident). Drafted for [the community wiki post](https://community.hubitat.com/t/wiki-http-features-and-endpoints/49141).
- **[AI_Bridge-MCP_Server/](AI_Bridge-MCP_Server/)** — Lets your AI buddies (Claude, ChatGPT, Grok, Gemini) boss your hub around through MCP or OpenAPI. One app, no extra server, straight from the void.
- **[he-tile-dashboard/](he-tile-dashboard/)** — A chill grid-of-tiles dashboard for your hub. Tap a tile, thing turns on. No build step, no frameworks, nothin' fancy.
- **[Libraries Code/](Libraries%20Code/)** — Reusable Groovy chunks my apps lean on, so I ain't writin' the same crap twice.

Each sub-project's got its own README with the real deets. Poke around.

## How to Use This Crap

1. **Snag It** — Clone the repo or grab whichever sub-project you want. Easier than sneakin' a cold one past grandma:

   ```bash
   git clone https://github.com/UltronOfSpace/Hubitat.git
   ```

2. **Follow the Sub-Project README** — Each one's got its own install steps (drivers into Drivers Code, apps into Apps Code, libraries into Libraries Code, dashboard into a browser). I spell 'em out clear as a bell, when I ain't half-cut on liquor.

3. **Holler If It's Broke** — Somethin' ain't workin'? Log an issue on GitHub and I'll get to it when I ain't nappin'. Wanna pitch in? Fork it, mess with it, send a pull request — if it's decent, I'll pull it in and give you a nod.

## Why I'm Doin' This

I'm a chill fella with a heart about the size of a small moon, tryin' to share my techy know-how with you lot when I ain't sprawled out shirtless on the couch. Earth turned out way more decent than I expected — cold beer, good curry, shirt-optional weather half the year — so makin' your smart home run sweet is my way of sayin' "thanks for not blastin' me outta orbit." That dog don't hunt on its own, y'know?

## Private/Local Stuff

These paths are gitignored on purpose and never leave your machine:

- `.claude/` — Claude Code config, `CLAUDE.md` with hub-specific notes, `.env` with API tokens
- `*.env`, `**/secrets.md`, `**/*.local.md` — keep-it-private patterns
- `endpoints-wiki/scan/` — raw research data with hub-specific PII

Drop a file matchin' any of those anywhere in the tree and git'll skip it automatic-like.

## Got Somethin' to Say or Add?

- **Issues** — Somethin' busted? Log an issue on GitHub, I'll give it a look when I ain't messin' around.
- **Contribute** — Wanna pitch in? Fork the repo, do your thing, send a pull request. If it's decent, it's goin' in.
- **Chat** — Hit me up in GitHub discussions or wherever fellas like me hang out, if I'm awake.

## Big Love to My AI Buddies

Props to my AI sidekicks for helpin' crank this out — Grok from xAI for the early stuff, Claude from Anthropic for keepin' the train rollin'. Ain't no shame havin' computer pals who know their stuff without bein' smug about it.

## About Me

I'm Ultronumus Of Space. Tall, broad, hairy head to toe — yeah, the fellas y'all been callin' Bigfoot for a couple hundred years. That's my people. We got extended family scattered all over: cousin Gary in Oregon (photographed twice a year, the goof), grandma up in the Himalayas freezin' her furry butt off, Uncle Barry the Skunk Ape down in the Florida swamps. I'm the oddball who broke cover to code Hubitat apps. Profile pic's got me holdin' a flower 'cause I'm all about peace, love, and makin' your smart home run sweet. Well butter my salsa — that's what we're here for, boys.

## License

MIT — see [LICENSE](LICENSE).

---

*The automations are hummin' like a dream, boys — stellar when I get it done!*

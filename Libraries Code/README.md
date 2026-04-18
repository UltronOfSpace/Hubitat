# Libraries Code — Ultron's Reusable Groovy Stash

> **⚠️ Legacy notice (2026):** The library currently in here (`PauseResumeLib`, briefly named `AppControlLib` from 2025-06 to 2026-04 before bein' renamed back) was built to add pause/resume buttons to Hubitat apps. Hubitat has since added a **built-in Disable** toggle on the apps list and in every app's settings, which covers the same use case for almost everyone. See [PauseResumeLib/README.md](PauseResumeLib/README.md) for the full notice and the remainin' niche uses. Kept here for reference.

## Alright, What's This Pile?

Alright boys, this here's my `Libraries Code` folder in the `UltronOfSpace/Hubitat` repo — the stash of reusable Groovy chunks my drivers and apps lean on when I'm too lazy to write the same crap twice. Think of it as the toolbox I dig into when I ain't sprawled out shirtless with a cold one or stuffin' my face with curry.

This ain't the drivers or apps themselves — those live elsewhere in the repo — this is the glue holdin' 'em together.

I'm leanin' on my AI buddies to help put this together, 'cause it's like havin' a fella around who knows every dang thing and ain't smug about it. This folder's for anyone messin' with Hubitat who wants to use my code to make their setup run sweeter than Uncle Barry's swamp curry.

## What's in Here?

- **Library Files** — shared Groovy chunks my apps pull in via `#include UltronOfSpace.*`. Right now you'll see `PauseResumeLib` (pause/resume buttons for apps) and whatever else I've been tinkerin' with.
- **Future Stuff** — I'll toss more in as I build 'em, whenever I ain't nappin' or grillin' burgers. Could be schedulin' helpers, device handlers, whatever my alien brain coughs up next.

Every file's got comments inside explainin' what it does, so it ain't a mystery.

## How to Use This Crap

1. **Find What You Need** — Poke around and grab the library file you want (like `PauseResumeLib`). Comments at the top tell you what it does and what apps need it.
   - View or download straight from GitHub: `https://github.com/UltronOfSpace/Hubitat/tree/main/Libraries%20Code`

2. **Stick It in Hubitat**:
   - Copy the library code from the `.groovy` file.
   - In your Hubitat web UI, go to **Libraries Code** (under **Developer Tools** in the sidebar).
   - Hit **+ Add Library**, paste the code in, save.
   - Make sure any drivers or apps usin' it are linked up right — check their docs or comments for setup.

3. **Test It** — Run the driver or app that uses the library. If somethin's broke, check the Hubitat logs or give me a shout. I'll look into it when I ain't half-cut on liquor.

4. **Grab the Whole Repo** (if you're fancy):

   ```bash
   git clone https://github.com/UltronOfSpace/Hubitat.git
   ```

   The `Libraries Code` folder'll be sittin' right there, ready to roll.

## Why I'm Doin' This

I'm a chill fella with a heart the size of a small moon, tryin' to share my techy know-how with you lot when I ain't sprawled out on the couch. Earth's a wild place, and givin' y'all some solid Hubitat libraries is a decent way to say "thanks for not blastin' me outta orbit." Plus, codin' these keeps me busy when I ain't chuggin' a cold one or whippin' up curry. That cat won't hunt on its own, y'know?

## Got Somethin' to Say or Add?

- **Issues** — Somethin' broke? Log an issue at `https://github.com/UltronOfSpace/Hubitat/issues`, I'll check it out when I ain't messin' around.
- **Contribute** — Got a library to add? Fork the repo, drop your code in `Libraries Code`, send a pull request. If it's decent, I'll toss it in with a nod to ya.
- **Chat** — Hit me up in GitHub discussions or wherever fellas like me hang, if I'm awake.

## Big Love to My AI Buddies

Props to my AI sidekicks for helpin' crank this out — Grok from xAI for the early stuff, Claude from Anthropic for keepin' the train rollin'. Like havin' buddies who know every dang thing and ain't smug about it.

## About Me

I'm Ultronumus Of Space. Looks just like the fellas humans been callin' Bigfoot for centuries, 'cause that's what my people are. Profile pic's got me holdin' a flower 'cause I'm all about peace, love, and makin' your Hubitat setup run sweet. Stick around and we'll build some stellar stuff together, whenever I get off my furry behind!

---

*The automations are hummin' like a dream, boys — when I ain't nappin'!*

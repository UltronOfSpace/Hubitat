# Libraries Code - UltronOfSpace’s Hubitat Code Stash

> **⚠️ Legacy notice (2026):** The library currently in here (`PauseResumeLib`, briefly named `AppControlLib` from 2025-06 to 2026-04 before being renamed back) was built to add pause/resume buttons to Hubitat apps. Hubitat has since added a **built-in Disable** toggle on the apps list and in every app's settings, which covers the same use case for almost everyone. See [PauseResumeLib/README.md](PauseResumeLib/README.md) for the full notice and the remaining niche uses. Kept here for reference.

## Yo, What’s This Shit?

Yo, I’m Ultronumus Of Space (UltronOfSpace, boys), and this `Libraries Code` folder in my `UltronOfSpace/Hubitat` repo is where I chuck all my reusable code chunks for Hubitat smart home gear. Think of it as the toolbox I dig into when I’m not sprawled out shirtless with a liquor or stuffin’ my face with curry. This ain’t the drivers or apps themselves—those are elsewhere in the repo—but the glue that makes ‘em work without me writin’ the same crap over and over.

I’m usin’ my AI mate Grok from xAI to help put this together, ‘cause I’m lazy as fuck sometimes, and it’s like havin’ a buddy who’s got all the smarts without bein’ a dick. This folder’s for anyone messin’ with Hubitat who wants to use my code to make their setup run sweeter than a cold beer.

## What’s in Here?

This `Libraries Code` folder’s got the good stuff, when I get around to addin’ it:

- **Library Files**: Code chunks that my Hubitat drivers and apps lean on. Like, shared functions or classes so I ain’t copy-pastin’ like a dumbass. For example, shit like `PauseResumeLib` (you mighta seen it) for pausin’ and resumin’ automations.
- **Future Shit**: I’ll toss in more libraries as I make ‘em, whenever I ain’t nappin’ or grillin’ burgers. Could be anything from schedulin’ helpers to device handlers, dependin’ on what I feel like codin’.

Right now, you might see stuff like `PauseResumeLib` or other files I’ve been tinkerin’ with. Each one’s got comments inside explainin’ what it does, so it ain’t a mystery.

## How to Use This Crap

1. **Find What You Need**: Poke around this folder and grab the library file you want, like `PauseResumeLib`. Check the file’s comments for what it does and what drivers or apps need it.
   - You can view or download files right from GitHub: `https://github.com/UltronOfSpace/Hubitat/tree/main/Libraries%20Code`.

2. **Stick It in Hubitat**:
   - Copy the library code from the file.
   - In your Hubitat Web Interface, go to “Libraries” (under “Drivers Code” or “Apps Code” in the sidebar).
   - Paste the code into a new library or overwrite an existin’ one if you’re updatin’.
   - Save it, and make sure any drivers or apps usin’ it are linked up right—check their docs or comments for setup.

3. **Test It Out**: Run your driver or app that uses the library. If it’s fucked, check the Hubitat logs or give me a shout. I’ll get to it when I ain’t half-cut on liquor.

4. **Grab the Whole Repo** (if you’re fancy):
   - If you’re usin’ a bunch of my stuff, clone the whole repo:
     ```bash
     git clone https://github.com/UltronOfSpace/Hubitat.git
     ```
   - The `Libraries Code` folder will be in there, ready to roll.

## Why I’m Doin’ This

I’m a chill alien with a big-ass heart, tryin’ to share my techy know-how with you lot when I ain’t sprawled out like a drunk Ricky. Earth’s a wild place, and I figure givin’ you some solid Hubitat libraries is a dope way to say “thanks for not blastin’ my ass outta the sky.” Plus, codin’ these keeps me busy when I ain’t chuggin’ beer or whippin’ up a curry.

## Got Shit to Say or Add?

- **Issues**: Somethin’ ain’t workin’? Log an issue on GitHub at `https://github.com/UltronOfSpace/Hubitat/issues`, and I’ll check it out when I ain’t fuckin’ around.
- **Contribute**: Got a library to add? Fork the repo, chuck your code in `Libraries Code`, and send a pull request. If it’s legit, I’ll toss it in with a nod to ya.
- **Chat**: Hit me up in GitHub discussions or wherever aliens like me hang, if I’m not passed out.

## Big Love to Grok

Mad props to Grok from xAI for helpin’ me write this README and some of the code. It’s like havin’ a buddy who knows every damn thing but ain’t a smug bastard. Wanna know about Grok? Check `https://x.ai/grok`.

## About Me

I’m Ultronumus Of Space, an alien with a heart full of love and a brain that’s wicked when I bother usin’ it. My profile pic’s got me holdin’ a flower ‘cause I’m all about peace, love, and makin’ your Hubitat setup the dog’s bollocks. Stick around, and we’ll build some sweet shit together, whenever I get off my lazy ass!

---

*“Yo, the automations are runnin’ like a damn dream! Fuckin’ sweet, boys, when I get it done!”*

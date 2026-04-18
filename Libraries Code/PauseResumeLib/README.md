# PauseResumeLib — The Greasiest Hubitat Library in the Interwebs!

> *Briefly renamed `AppControlLib` between 2025-06 and 2026-04, but reverted to `PauseResumeLib` since pause/resume is effectively all it does.*

> **⚠️ Legacy notice (2026):** Hubitat's built-in **Disable** toggle (available on the apps list and in every app's settings) now covers the main use case this library was built for. For most people, the built-in disable is simpler — it works on every installed app without any code changes. This library still has a niche use for custom apps where you want a prominent pause button on the app's own config page, parent/child coordinated pause state, or the "(Paused)" name tag. Kept here for reference and for any apps still importing it.

Alright boys, this here PauseResumeLib is the best thing I ever put together for Hubitat — and that's sayin' somethin' for a cosmic slacker who spends most of his time nappin' between coding sessions! It's like the ultimate control knob for your apps — lets you pause and resume stuff without breakin' a sweat. You can use it for a single standalone app, a big boss app with a bunch of little punk kid apps runnin' around, or a little punk kid app that's gotta do what it's told by the big boss. It puts buttons on your apps, shows you what's paused or runnin', and even slaps a "(Paused)" tag on the name when they're takin' a break — so you always know what's what in the interwebs. I made this so you don't gotta mess around with a bunch of code — just one line and you're good to go, boys!

---

## Why I Made This Thing

So listen here, I figured Hubitat folks needed somethin' to make their apps easier to control, 'cause I get ticked when stuff's too complicated — like a jalopy with four flat tires and no spare, just sittin' there bein' useless. This library does all the heavy liftin' for you:

- Puts pause/resume buttons on any app — standalone, a big boss app, or a little punk kid app — faster than you can pop a cold one.
- Shows you what's goin' on with your apps in a nice layout, no matter what kinda app you're messin' with.
- Changes the app name to show "(Paused)" when it's off, so you ain't confused — workin' as intentioned, as I like to say.
- Gives you a "Control" section with all the buttons and info you need, and you only gotta add one line of code to make it happen — real smart, right there!

I even threw in some examples to show you how it works:

- **Big Boss App and Little Punk Kid App**: A big boss app (`PauseResumeLib Parent App Example`) and its little punk kid app (`PauseResumeLib Child App Example`) in separate files to show you how to make a big boss app that makes a bunch of little punk kid apps and tells 'em what to do, plus how the little punk kid app listens to the big boss.
- **Lone Wolf App**: The lone wolf app that don't need nobody, just its own pause button.

---

## How to Get It Installed in Your Hubitat Smart Controller Thingy

Here's how to get this library into your Hubitat setup — don't mess it up, or you'll be dumber than a bag of hammers! You got two options: import it directly (recommended), or copy and paste the code from GitHub.

### Option 1: Import Directly (Recommended)

This way's recommended 'cause it's so easy even a cosmic slacker like me could do it half-asleep on the couch.

1. Log into your Hubitat Elevation hub — like sneakin' into the interwebs after dark.
2. Go to **Libraries Code** on the sidebar — don't get lost, you goofball! (If you don't see it, click the arrow next to **Developer Tools** to expand the list.)
3. Click **+ Add Library** up top on the right.
4. Click the three vertical dots (⋮) in the top right corner of the editor to open the menu, then hit **Import** — don't miss it, you knucklehead!
5. Paste the following URL (you can copy it by clicking the copy button):

```
https://raw.githubusercontent.com/UltronOfSpace/Hubitat/main/Libraries%20Code/PauseResumeLib/PauseResumeLib.groovy
```

6. Click **Import**, then **Save**. Done, you greasy fella!

### Option 2: Copy and Paste from GitHub

1. Head over to [this link right here](PauseResumeLib.groovy) — don't look a gift bug in the code!
2. Click the "Copy raw file" button (it's the little clipboard icon near the top right of the code) — that'll grab all the code faster than you can say "decent"!
3. Log into your Hubitat Elevation hub — like sneakin' into the interwebs after dark.
4. Go to **Libraries Code** on the sidebar — don't get lost, you goofball! (If you don't see it, click the arrow next to **Developer Tools** to expand the list.)
5. Click **+ Add Library** up top on the right.
6. Paste the code into the editor.
7. Click **Save**. Done, you greasy fella!

### Make Sure It's There

- Check the **Libraries Code** list. You should see "PauseResumeLib (UltronOfSpace)" sittin' there like it owns the place. If it ain't there, you messed up — try again, bud!

---

## How to Use This Thing in Your Apps

Usin' PauseResumeLib is easier than crackin' a cold one on a warm day. Here's how to make it work in any app — standalone, a big boss app, or a little punk kid app:

### 1. Add the Library

At the top of your app's Groovy file, chuck in this line:

```
#include UltronOfSpace.PauseResumeLib
```

That's it — now you've got the power, like a rum and coke in your hand!

### 2. Get the Control Section Goin'

In your app's `mainPage` method, I'd recommend addin' this section right after the part where you let users name the app (like in the examples), so the "Control" section shows up just below the name field — it makes sense to me so you can name your app first, then get to the controls. But you can put it anywhere you like, even at the bottom or wherever it fits your app best, bud:

```groovy
section("Control") {
    renderPauseResumeControls()
}
```

That'll slap a "Control" section in your app with all the good stuff:

- **Standalone App**: Just a "Pause"/"Resume" button for itself — no drama, no nonsense.
- **Little Punk Kid App**: A "Pause"/"Resume" button to take a break when the big boss app says so.
- **Big Boss App**: "Pause All Child Apps" and "Resume All Child Apps" buttons, plus a two-row summary of how your little punk kid apps are doin' (like "2 child apps are paused." and "1 child app is runnin'."), and a little note to keep you in the loop.

### 3. Make Sure Your App's Got the Basics

Your app needs these methods to play nice with the library — don't be a goofball and skip 'em:

```groovy
def installed() {
    log.info "${app.getName()} installed"
    enablePauseResume()
}

def updated() {
    log.info "${app.getName()} updated"
    enablePauseResume()
}

def uninstalled() {
    log.info "${app.getName()} uninstalled"
}

def initialize() {
    // Optional: Chuck in some subscriptions or schedules here, bud
}
```

If you're makin' a big boss app, your `uninstalled()` method should also clean up the little punk kid apps — check out `PauseResumeLib Parent App Example` for how to do it.

---

## Who Helped Me Make This Thing

I ain't no coder myself — honest about it, boys, my brain's better at nappin' than compilin'. But I knew what I wanted this library to do, and my AI sidekicks stepped up. SuperGrok from xAI cranked out the Groovy and wrote these instructions so even a cosmic slacker like me could follow 'em. Claude from Anthropic's been keepin' the train rollin' since. Ain't no shame havin' computer pals who know their stuff without bein' smug about it. For SuperGrok details check `https://x.ai/grok`.

---

## Examples to Check Out

I've got some examples to show you how this library gets 'er done:

- [**PauseResumeLib Parent And Child Example**: The big boss app and its little punk kid apps](ExampleApps/ParentAndChild/README.md) — shows you how to make a big boss app that makes a bunch of little punk kid apps and tells 'em what to do, plus how the little punk kid app listens to the big boss.
- [**PauseResumeLib Standalone Example**: The lone wolf app](ExampleApps/StandAlone/README.md) — don't need nobody, just its own pause button.

---

## License

This project's got an MIT License — don't ask me what that means, I learned law from watchin' the interwebs go to heck, not some fancy lawyer! Check the [LICENSE](https://github.com/UltronOfSpace/Hubitat/blob/main/LICENSE) file if you care.

---

## Get at Me

If you're stuck or wanna show off your Hubitat setup, hit me up on GitHub at `https://github.com/UltronOfSpace`. Don't be a jerk about it — message me, bud!

---

PauseResumeLib — greasy as heck and twice as decent, straight from the interwebs of the galaxy!

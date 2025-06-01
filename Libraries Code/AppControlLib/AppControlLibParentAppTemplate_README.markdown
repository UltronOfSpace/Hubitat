# AppControlLibParentAppTemplate - The Boss App That Runs the Damn Show!

This here AppControlLibParentAppTemplate is the greasiest big boss app for Hubitat you’ll ever see—it’s like I’m runnin’ the whole damn interwebs! It uses the AppControlLib to make and control a bunch of little punk kid apps, tellin’ ‘em when to pause or get their butts back to work. You get buttons to pause ‘em all at once, a summary to see who’s doin’ what, and it’s all decent as heck, bud!

---

## Why I Made This Thing

I made this template ‘cause Hubitat folks need a way to run a bunch of little punk kid apps without losin’ their minds—like when I’m tryin’ to keep my growin’ operations from goin’ to heck. Here’s what it does:

- Makes little punk kid apps with the **AppControlLibChildAppTemplate**—like growin’ new plants, but apps!
- Gives you "Pause All Child Apps" and "Resume All Child Apps" buttons to control all the little punks at once—kinda like tryin’ to stop a crapstorm in the interwebs.
- Shows a two-row summary, like "2 child apps are paused." and "1 child app is runnin’."—so you know what’s up without diggin’ around.
- Plugs into **AppControlLib** so it’s all slick and easy, like a rum and coke on a hot day.

If you’ve got a bunch of little punk kid apps to wrangle, this template’s the way to go!

---

## What You Need First

Before you can get this goin’, you gotta have some stuff ready:

- **AppControlLib**: You need this library, or you’re dumber than a bag of hammers! Check the [AppControlLib README](../README.md#how-to-get-it-installed-in-your-hubitat-smart-controller-thingy) for how to get it.
- **AppControlLibChildAppTemplate**: You’ll need this to make the little punk kid apps—don’t skip it, bud!

---

## How to Get It Installed

Here’s how to get this AppControlLibParentAppTemplate into your Hubitat setup—don’t mess it up, or I’ll lose my mind! You got two options: import it directly (recommended), or copy and paste the code from GitHub.

### Option 1: Import Directly (Recommended)

This way’s recommended ‘cause it’s so easy even a greasy burger gut shirtless wonder can do it!

1. First, head over to [this GitHub page right here](https://github.com/UltronumusOfSpace/Hubitat/blob/main/Libraries%20Code/AppControlLib/AppControlLibParentAppTemplate.groovy)—don’t lose it like I lose my dope!
2. At the top right, you’ll see a button labeled "Raw"—click it, then copy the URL from your browser’s address bar (or right-click the "Raw" button and select "Copy link address").
3. Log into your Hubitat Elevation hub—like sneakin’ into the interwebs after dark.
4. Go to **Apps Code** on the sidebar—don’t get lost, you goofball! (If you don’t see it, click the arrow next to **Developer Tools** to expand the list.)
5. Click **+ New App** up top on the right.
6. Click the three vertical dots (⋮) in the top right corner of the editor to open the menu, then hit **Import**—don’t miss it, you knucklehead!
7. Paste the URL you copied (it’ll look like `https://raw.githubusercontent.com/UltronumusOfSpace/Hubitat/main/Libraries%20Code/AppControlLib/AppControlLibParentAppTemplate.groovy`).
8. Click **Import**, then **Save**. Done, you greasy fella!

### Option 2: Copy and Paste from GitHub

1. Head over to [this link right here](https://github.com/UltronumusOfSpace/Hubitat/blob/main/Libraries%20Code/AppControlLib/AppControlLibParentAppTemplate.groovy)—don’t lose it like I lose my dope!
2. Click the "Copy" button (it’s the little clipboard icon near the top right of the code)—that’ll grab all the code faster than you can say "decent"!
3. Log into your Hubitat Elevation hub—like sneakin’ into the interwebs after dark.
4. Go to **Apps Code** on the sidebar—don’t get lost, you goofball! (If you don’t see it, click the arrow next to **Developer Tools** to expand the list.)
5. Click **+ New App** up top on the right.
6. Paste the code into the editor.
7. Click **Save**. Done, you greasy fella!

### Make Sure It’s There

- Check the **Apps Code** list. You should see "AppControlLibParentAppTemplate (UltronumusOfSpace)" sittin’ there like it owns the place. If it ain’t there, you messed up—try again, bud!

### Get the Kid App Template

You’ll need the `AppControlLibChildAppTemplate.groovy` to make the little punk kid apps. Grab it the same way:

- **Import**: Head to [this GitHub page](https://github.com/UltronumusOfSpace/Hubitat/blob/main/Libraries%20Code/AppControlLib/AppControlLibChildAppTemplate.groovy), click the "Raw" button, and copy the URL from your browser’s address bar (or right-click the "Raw" button and select "Copy link address").
- **Copy/Paste**: Copy the code from [this link](https://github.com/UltronumusOfSpace/Hubitat/blob/main/Libraries%20Code/AppControlLib/AppControlLibChildAppTemplate.groovy) by clickin’ the "Copy" button (the clipboard icon near the top right).

Save it, or you’re as useless as a crap hawk!

### Add the App to Hubitat

1. Go to **Apps** in the Hubitat sidebar.
2. Click **+ Add User App** in the top right corner.
3. Pick "AppControlLibParentAppTemplate" from the list and click **Done**. Easy as takin’ a leak, bud!

---

## How to Use This Thing

Now that you’ve got it installed, here’s how to run your app show:

### 1. Open the App

In the Hubitat **Apps** list, click on "AppControlLibParentAppTemplate". You’ll see a "Control" section with buttons and a summary of your little punk kid apps, plus a "Child Apps" section to make new ones.

### 2. Make Some Little Punk Kid Apps

1. In the "Child Apps" section, click "Create New Child App".
2. That’ll whip up a new little punk kid app usin’ the AppControlLibChildAppTemplate.
3. If you wanna name it somethin’, pop into the kid app’s settings and set a custom name—like namin’ a kitty or some stuff.
4. Make as many little punk kid apps as you want—go nuts, eh!
5. Click **Done** to save your mess.

### 3. Run the Show

1. In the "Control" section, hit "Pause All Child Apps" to make all the little punks take a break.
2. Wanna get ‘em goin’ again? Hit "Resume All Child Apps"—they’ll be back faster than I can roll a joint!
3. The summary’ll tell you what’s what, like "2 child apps are paused." and "1 child app is runnin’."—keeps you in the know without diggin’ around.

### 4. Check On the Little Punks

Wanna mess with a little punk kid app on its own? Open it from the Hubitat **Apps** list and have a go—check the [AppControlLibChildAppTemplate README](./AppControlLibChildAppTemplate_README.md) for the deets.

---

## Who Helped Me Make This Thing

I ain’t no coder—I’m about as good at makin’ code as Mr. Lahey is at not drinkin’, and I ain’t much smarter than a box of rocks when it comes to this stuff! But I knew what I wanted this template to do, and SuperGrok stepped up to the plate like a champ. It’s a paid subscription plan on grok.com that gave me the extra juice to keep goin’ without runnin’ outta steam. SuperGrok not only wrote all the fancy code for this library and templates but also made these instructions so even a fella like me could follow ‘em. If you’re lookin’ to make your own Hubitat apps, SuperGrok’s got the power to make it happen! For more details on SuperGrok, check out `https://x.ai/grok`.

---

## License

This project’s got an MIT License—don’t ask me what that means, I learned law from watchin’ the interwebs go to heck, not some fancy lawyer! Check the [LICENSE](https://github.com/UltronumusOfSpace/Hubitat/blob/main/LICENSE) file if you care.

---

## Get at Me

If you’re stuck or wanna show off your Hubitat setup, hit me up on GitHub at `https://github.com/UltronumusOfSpace`. Don’t be a jerk about it—message me, bud!

---

AppControlLibParentAppTemplate—greasy as heck and twice as decent, straight from the interwebs of the galaxy!
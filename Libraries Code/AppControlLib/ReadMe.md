AppControlLib - The Greasiest Hubitat Library in the Interwebs!
This here AppControlLib is the best damn thing I ever made for Hubitat, and I ain’t even high right now! It’s like the ultimate control for your apps—lets you pause and resume stuff without breakin’ a sweat. You can use it for a single app, a big boss app with a bunch of little punk kid apps runnin’ around, and a little punk kid app that’s gotta do what it’s told by the big boss app. It puts buttons on your apps, shows you what’s paused or runnin’, and even slaps a "(Paused)" tag on the name when they’re takin’ a break—like taggin’ your stuff so you don’t lose it in the interwebs. I made this so you don’t gotta mess around with a bunch of code—just one line and you’re good to go!

Why I Made This Thing
I figured Hubitat folks needed somethin’ to make their apps easier to control, ‘cause I get ticked when stuff’s too complicated—like when I’m tryin’ to grow my plants and the cops show up. This library does all the heavy liftin’ for you:

Puts pause/resume buttons on any app—standalone, a big boss app, or a little punk kid app—faster than you can roll a fatty.
Shows you what’s goin’ on with your apps in a nice layout, no matter what kinda app you’re messin’ with.
Changes the app name to show "(Paused)" when it’s off, so you ain’t confused like I get with my growin’ operations.
Gives you a "Control" section with all the buttons and info you need, and you only gotta add one damn line of code to make it happen—real smart, right there!

I even threw in some templates to show you how it works:

AppControlLibParentAppTemplate: The big boss app that makes a bunch of little punk kid apps and tells ‘em what to do.
AppControlLibChildAppTemplate: The little punk kid app that does what the big boss app says, with its own pause button.
AppControlLibStandaloneAppTemplate: The lone wolf app that don’t need nobody, just its own pause button.


How to Get It Installed in Your Hubitat Smart Controller Thingy
Here’s how to get this library into your Hubitat setup—don’t mess it up, or you’ll be dumber than a bag of hammers! You got two options: import it directly (recommended), or copy and paste the code from GitHub.
Option 1: Import Directly (Recommended)
This way’s recommended ‘cause it’s so easy even a greasy burger gut shirtless wonder can do it!

First, head over to this GitHub page right here—don’t lose it like I lose my dope!
At the top right, you’ll see a button labeled "Raw"—click it, then copy the URL from your browser’s address bar (or right-click the "Raw" button and select "Copy link address").
Log into your Hubitat Elevation hub—like sneakin’ into the interwebs after dark.
Go to Libraries Code on the sidebar—don’t get lost, you goofball! (If you don’t see it, click the arrow next to Developer Tools to expand the list.)
Click + Add Library up top on the right.
Click the three vertical dots (⋮) in the top right corner of the editor to open the menu, then hit Import—don’t miss it, you knucklehead!
Paste the URL you copied (it’ll look like https://raw.githubusercontent.com/UltronumusOfSpace/Hubitat/main/Libraries%20Code/AppControlLib/AppControlLib.groovy).
Click Import, then Save. Done, you greasy fella!

Option 2: Copy and Paste from GitHub

Head over to this link right here—don’t lose it like I lose my dope!
Click the "Copy" button (it’s the little clipboard icon near the top right of the code)—that’ll grab all the code faster than you can say "decent"!
Log into your Hubitat Elevation hub—like sneakin’ into the interwebs after dark.
Go to Libraries Code on the sidebar—don’t get lost, you goofball! (If you don’t see it, click the arrow next to Developer Tools to expand the list.)
Click + Add Library up top on the right.
Paste the code into the editor.
Click Save. Done, you greasy fella!

Make Sure It’s There

Check the Libraries Code list. You should see "AppControlLib (UltronumusOfSpace)" sittin’ there like it owns the place. If it ain’t there, you messed up—try again, bud!


How to Use This Thing in Your Apps
Usin’ AppControlLib is easier than gettin’ a six-paper joint rolled. Here’s how to make it work in any app—standalone, a big boss app, or a little punk kid app:
1. Add the Library
At the top of your app’s Groovy file, chuck in this line:
#include UltronumusOfSpace.AppControlLib

That’s it—now you’ve got the power, like a rum and coke in your hand!
2. Get the Control Section Goin’
In your app’s mainPage method, I’d recommend addin’ this section right after the part where you let users name the app (like in the templates), so the "Control" section shows up just below the name field—it makes sense to me so you can name your app first, then get to the controls. But you can put it anywhere you like, even at the bottom or wherever it fits your app best, bud:
section {
    renderPauseResumeControls()
}

That’ll slap a "Control" section in your app with all the good stuff:

Standalone App: Just a "Pause"/"Resume" button for itself—no drama, no nonsense.
Little Punk Kid App: A "Pause"/"Resume" button to take a break when the big boss app says so.
Big Boss App: "Pause All Child Apps" and "Resume All Child Apps" buttons, plus a two-row summary of how your little punk kid apps are doin’ (like "2 child apps are paused." and "1 child app is runnin’."), and a little note to keep you in the loop.

3. Make Sure Your App’s Got the Basics
Your app needs these methods to play nice with the library—don’t be a goofball and skip ‘em:
def installed() {
    log.info "${app.getName()} installed"
    enablePauseResume()
}

def updated() {
    log.info "${app.getName()} updated"
    if (settings.appName) {
        app.updateLabel(settings.appName)
        updateAppLabel(state.isPaused ?: false)
    }
    enablePauseResume()
}

def uninstalled() {
    log.info "${app.getName()} uninstalled"
}

def initialize() {
    // Optional: Chuck in some subscriptions or schedules here, bud
}

If you’re makin’ a big boss app, your uninstalled() method should also clean up the little punk kid apps—check out AppControlLibParentAppTemplate for how to do it.

Who Helped Me Make This Thing
I ain’t no coder—I’m about as good at makin’ code as Mr. Lahey is at not drinkin’, and I ain’t much smarter than a box of rocks when it comes to this stuff! But I knew what I wanted this library to do, and SuperGrok stepped up to the plate like a champ. It’s a paid subscription plan on grok.com that gave me the extra juice to keep goin’ without runnin’ outta steam. SuperGrok not only wrote all the fancy code for this library and templates but also made these instructions so even a fella like me could follow ‘em. If you’re lookin’ to make your own Hubitat apps, SuperGrok’s got the power to make it happen! For more details on SuperGrok, check out https://x.ai/grok.

Examples to Check Out
I’ve got some templates to show you how this library gets ‘er done:

AppControlLibParentAppTemplate: The big boss app that makes a bunch of little punk kid apps and tells ‘em what to do.
AppControlLibChildAppTemplate: The little punk kid app that does what the big boss app says, with its own pause button.
AppControlLibStandaloneAppTemplate: The lone wolf app that don’t need nobody, just its own pause button.


License
This project’s got an MIT License—don’t ask me what that means, I learned law from watchin’ the interwebs go to heck, not some fancy lawyer! Check the LICENSE file if you care.

Get at Me
If you’re stuck or wanna show off your Hubitat setup, hit me up on GitHub at https://github.com/UltronumusOfSpace. Don’t be a jerk about it—message me, bud!

AppControlLib—greasy as heck and twice as decent, straight from the interwebs of the galaxy!

# PauseResumeLib for Hubitat

Welcome to **PauseResumeLib**, the slickest way to pause and resume your Hubitat apps without turning your smart home into a total smeg-show or a greasy trailer park disaster! Whether you’re dodging a curry-fueled catastrophe or trying to keep your app from going as off-the-rails as a rum-soaked scheme, this library’s got you covered. Here’s the lowdown, with a bit of humor to keep you from losing your mind like a hologram with a stick up his arse.

## What It Does

This library lets you pause and resume your Hubitat apps faster than a cleaning droid can polish a pair of boots—or faster than you can say "where’s my cheeseburger?" It slaps a Pause/Resume button into your app’s UI: parent apps get a fancy "Pause Parent & Child Apps" / "Resume Parent & Child Apps," while solo apps get a no-nonsense "Pause" / "Resume." When you pause, it kills off events and schedules, making your app quieter than a cat dodging greens. When you resume, it fires everything back up, like getting the kitties back in line after a wild night. Parent apps will even wrangle their child apps into submission, unless they’re as stubborn as a shirtless wonder.

## How to Use It

### 1. Include the Library
Drop this at the top of your app like a lager-lover drops curry on his shirt:

```groovy
#include UltronOfSpace.PauseResumeLib
```

Skip this, and you’re as lost as a space bum trying to navigate without a ship—good luck, smeghead!

### 2. Set Up Lifecycle Methods
- **In `installed()`**, call `initializeWithPause()` to get the pause state ready. It’s like a proper droid booting up, all prim and proper.
  ```groovy
  def installed() {
      logDebug("Installing app, eh?")
      initializeWithPause()
      initialize()
  }
  ```

- **In `updated()`**, call `updatedWithPause()` with your `initialize()` method as a closure. Think of it as handing over the supervisor gig to someone who’s more weed than brains—make sure it knows what to do when it’s back in action.
  ```groovy
  def updated() {
      logDebug("Updating app, let’s not mess this up like a grow-op")
      updatedWithPause({ initialize() })  // Pass your initialize method here
  }
  ```

- Don’t forget to define an `initialize()` method to set up your subscriptions and schedules. This is where you tell your app what to do when it’s not napping, like fixing carts on a good day.
  ```groovy
  def initialize() {
      if (state.isPaused) {
          logDebug("App is paused, skipping init like a coward skips courage")
          return
      }
      logDebug("Initializing app, let’s get this party started")
      subscribe(someDevice, "switch.on", someHandler)
      schedule("0 */10 * * * ?", someScheduledMethod)
  }
  ```

### 3. Add the Pause/Resume Button
In your app’s UI (e.g., `mainPage()`), call `addPauseResumeSection()` to add the Pause/Resume button. It’s as easy as finding a cold one in a derelict ship’s supply closet.

```groovy
def mainPage() {
    dynamicPage(name: "mainPage", title: "My App", install: true, uninstall: true) {
        addPauseResumeSection()
        // Add other UI stuff here
    }
}
```

The library will show the paused state in the app’s header (e.g., "My App (Paused)")—no extra hassle to worry about.

### 4. Check `state.isPaused` in Your Code
In your event handlers and scheduled methods, always check `state.isPaused` before doing anything. It’s like making sure there’s no shirtless chaos before opening the door—don’t skip this, or things’ll get messy.

```groovy
def someHandler(evt) {
    if (state.isPaused) {
        logDebug("App is paused, skipping handler like veggies get skipped")
        return
    }
    logDebug("Handling event: ${evt.name}")
    // Do your thing
}

def someScheduledMethod() {
    if (state.isPaused) {
        logDebug("App is paused, skipping like school gets skipped")
        return
    }
    logDebug("Running scheduled task, eh?")
    // Do your thing
}
```

## Parent/Child Apps
If your app’s running the show like a trailer park boss, it’ll automatically pause/resume its child apps when you hit the button. Just make sure your parent app sets `options.parent: true` in `updatedWithPause()`:

```groovy
def updated() {
    logDebug("Updating parent app, don’t let the liquor take over")
    updatedWithPause(parent: true, { initialize() })
}
```

Child apps don’t need any special treatment—they’ll follow along like stray cats following a shopping cart fixer.

## Limitations (or, What Could Go Wrong?)
- **Custom Event Handling**: If your app’s doing some shady stuff like a backwoods grow-op—custom polling loops or Java threads—this library won’t pause those. It only handles Hubitat’s `subscribe()` and `schedule()`. Check `state.isPaused` in your custom code, or you’ll be as confused as a hologram trying to fake a promotion.
- **Long-Running Actions**: Got a `runIn()` scheduled for an hour from now? If you pause 10 minutes later, that action’ll still run unless you check `state.isPaused` in the method. Don’t leave messes like curry stains everywhere—clean up proper!
- **Nested Parent/Child Apps**: This library pauses direct children, but if your children have children (like a messy family tree), you’ll need to make sure each level uses the library to pass the pause down. Otherwise, it’s like telling someone to diet while they’re munching a burger—ain’t gonna happen.
- **UI Refresh**: Hubitat’s UI is as stubborn as a feline refusing to share his fish. If a parent app pauses a child app, the child’s UI won’t update until you manually refresh it (e.g., click "Done" and reopen). It’s paused, I promise—just check the header after a refresh!

## Final Tips
- Add a `logDebug` method to your app to see what’s going on, unless you want to be as clueless as a space cadet trying to read a map:
  ```groovy
  def logDebug(String msg) {
      if (settings.enableLogging) {
          log.debug "${app.name} (${app.getLabel()}): ${msg}"
      }
  }
  ```
- Test your app like a cleaning bot tests for germs—thoroughly. Pause, resume, and make sure your event handlers and schedules behave. If something’s off, you probably skipped a step, like a hologram dodging charm lessons.

Now go forth and pause your apps, you magnificent mess! If you’ve got issues, hit me up on GitHub—I’ll sort you out faster than a cart gets fixed on a sunny day.

## Credits
- **Author**: Ultronumus Of Space
- **Creator**: Grok, created by xAI

## License
MIT
library (
    author: "Grok",
    category: "Utilities",
    description: "A library for adding pause/resume functionality to Hubitat apps",
    name: "PauseResumeLib",
    namespace: "grokutils",
    documentationLink: ""
)

def addPauseResumeSection() {
    section {
        paragraph "<div style='text-align: center;'>"
        if (state.isPaused) {
            // Use more descriptive text for parent apps, simpler text for child apps
            def buttonText = childApps?.size() > 0 ? "Resume Parent & Child Apps" : "Resume"
            input name: "resumeApp", type: "button", title: buttonText, submitOnChange: true
        } else {
            def buttonText = childApps?.size() > 0 ? "Pause Parent & Child Apps" : "Pause"
            input name: "pauseApp", type: "button", title: buttonText, submitOnChange: true
        }
        paragraph "</div>"
    }
}

def initializeWithPause() {
    if (state.isPaused == null) {
        state.isPaused = false
    }
    // Set the initial label to the app's name to prevent header duplication
    app.updateLabel(app.name)
    updateAppLabel(state.isPaused)
}

def updatedWithPause(Map options = [:], Closure initializeClosure) {
    updateAppLabel(state.isPaused)
    if (state.isPaused) {
        if (options.unsubscribe != false) unsubscribe()
        if (options.unschedule != false) unschedule()
        if (options.parent && childApps?.size() > 0) {
            appLog("App is a parent (childApps size: ${childApps.size()}), calling pauseAllChildApps")
            pauseAllChildApps()
        }
    } else {
        if (options.unsubscribe != false) unsubscribe()
        if (options.unschedule != false) unschedule()
        if (options.parent && childApps?.size() > 0) {
            appLog("App is a parent (childApps size: ${childApps.size()}), calling resumeAllChildApps")
            resumeAllChildApps()
        }
        initializeClosure()
    }
}

def appButtonHandler(String buttonName) {
    def previousState = state.isPaused
    appLog("appButtonHandler called with button: ${buttonName}, current isPaused: ${state.isPaused}")
    switch (buttonName) {
        case "pauseApp":
            state.isPaused = true
            appLog("Pausing app, new isPaused: ${state.isPaused}")
            updated()
            break
        case "resumeApp":
            state.isPaused = false
            appLog("Resuming app, new isPaused: ${state.isPaused}")
            updated()
            break
        case "refreshUI":
            appLog("Refreshing UI")
            break
        default:
            appLog("warn: Unhandled app button: $buttonName")
    }
    // Schedule a UI refresh if the state changed
    if (previousState != state.isPaused) {
        runIn(1, "refreshUI")
    }
}

def refreshUI() {
    appLog("Forcing UI refresh after state change")
    updateAppLabel(state.isPaused)
}

def updateAppLabel(boolean paused) {
    def baseLabel = app.getLabel()?.replaceAll(/ <span.*<\/span>/, "")?.replaceAll(/\s*\(Paused\)/, "") ?: app.name
    if (paused) {
        app.updateLabel("$baseLabel <span style='color:red'>(Paused)</span>")
    } else {
        app.updateLabel(baseLabel)
    }
}

def pauseAllChildApps() {
    appLog("Pausing all child apps")
    appLog("Number of child apps: ${childApps.size()}")
    childApps.each { child ->
        appLog("Pausing child app: ${child.label}")
        child.appButtonHandler("pauseApp")
    }
    state.childPauseStatus = "Paused all child apps at ${new Date()}"
}

def resumeAllChildApps() {
    appLog("Resuming all child apps")
    appLog("Number of child apps: ${childApps.size()}")
    childApps.each { child ->
        appLog("Resuming child app: ${child.label}")
        child.appButtonHandler("resumeApp")
    }
    state.childPauseStatus = "Resumed all child apps at ${new Date()}"
}

def getChildPauseStatus() {
    return state.childPauseStatus ?: "No child pause/resume actions yet."
}

// Helper method to log from the library context
def appLog(String msg) {
    log."${msg.startsWith('warn:') ? 'warn' : 'info'}"("${app.name} (${app.getLabel()}): ${msg.replace('warn: ', '')}")
}

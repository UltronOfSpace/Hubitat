/**
 *  PauseResumeLib
 *
 *  Author: Ultronumus Of Space
 *  Creator: Grok, created by xAI
 *  Version: 1.0.0
 *  Date: May 30, 2025
 *  Description: A library for adding pause/resume functionality to Hubitat apps. Supports both single apps
 *  and parent/child app structures, with UI integration and event/schedule management.
 *  License: MIT
 */

library (
    author: "Ultronumus Of Space",
    contributor: "Grok (xAI)",
    category: "Utilities",
    description: "A library for adding pause/resume functionality to Hubitat apps",
    name: "PauseResumeLib",
    namespace: "UltronOfSpace",
    documentationLink: "https://github.com/UltronOfSpace/Hubitat/tree/main/Libraries%20Code/PauseResumeLib"
)

// Add a Pause/Resume button to the app's UI
def addPauseResumeSection() {
    section {
        paragraph "<div style='text-align: center;'>"
        if (state.isPaused) {
            def buttonText = childApps?.size() > 0 ? "Resume Parent & Child Apps" : "Resume"
            input name: "resumeApp", type: "button", title: buttonText, submitOnChange: true
        } else {
            def buttonText = childApps?.size() > 0 ? "Pause Parent & Child Apps" : "Pause"
            input name: "pauseApp", type: "button", title: buttonText, submitOnChange: true
        }
        paragraph "</div>"
    }
}

// Initialize the app with pause state
def initializeWithPause() {
    if (state.isPaused == null) {
        state.isPaused = false
    }
    app.updateLabel(app.name)
    updateAppLabel(state.isPaused)
}

// Update the app with pause state handling
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

// Handle button clicks for pause/resume
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
    if (previousState != state.isPaused) {
        runIn(1, "refreshUI")
    }
}

// Attempt to force a UI refresh
def refreshUI() {
    appLog("Forcing UI refresh after state change")
    updateAppLabel(state.isPaused)
}

// Update the app's label to reflect paused state
def updateAppLabel(boolean paused) {
    def baseLabel = app.getLabel()?.replaceAll(/ <span.*<\/span>/, "")?.replaceAll(/\s*\(Paused\)/, "") ?: app.name
    if (paused) {
        app.updateLabel("$baseLabel <span style='color:red'>(Paused)</span>")
    } else {
        app.updateLabel(baseLabel)
    }
}

// Pause all child apps
def pauseAllChildApps() {
    appLog("Pausing all child apps")
    appLog("Number of child apps: ${childApps.size()}")
    childApps.each { child ->
        appLog("Pausing child app: ${child.label}")
        child.appButtonHandler("pauseApp")
    }
    state.childPauseStatus = "Paused all child apps at ${new Date()}"
}

// Resume all child apps
def resumeAllChildApps() {
    appLog("Resuming all child apps")
    appLog("Number of child apps: ${childApps.size()}")
    childApps.each { child ->
        appLog("Resuming child app: ${child.label}")
        child.appButtonHandler("resumeApp")
    }
    state.childPauseStatus = "Resumed all child apps at ${new Date()}"
}

// Get the pause/resume status of child apps
def getChildPauseStatus() {
    return state.childPauseStatus ?: "No child pause/resume actions yet."
}

// Helper method to log from the library context
def appLog(String msg) {
    log."${msg.startsWith('warn:') ? 'warn' : 'info'}"("${app.name} (${app.getLabel()}): ${msg.replace('warn: ', '')}")
}

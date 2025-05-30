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
    appLog("Adding pause/resume section to UI")
    section {
        paragraph "<div style='text-align: center;'>"
        if (state.isPaused == null) {
            appLog("WARN: state.isPaused is null, initializing to false")
            state.isPaused = false
        }
        if (state.isPaused) {
            def buttonText = childApps?.size() > 0 ? "Resume Parent & Child Apps" : "Resume"
            appLog("Displaying resume button with text: ${buttonText}")
            input name: "resumeApp", type: "button", title: buttonText, submitOnChange: true
        } else {
            def buttonText = childApps?.size() > 0 ? "Pause Parent & Child Apps" : "Pause"
            appLog("Displaying pause button with text: ${buttonText}")
            input name: "pauseApp", type: "button", title: buttonText, submitOnChange: true
        }
        paragraph "</div>"
    }
}

// Initialize the app with pause state
def initializeWithPause() {
    appLog("Initializing app with pause state")
    if (state.isPaused == null) {
        appLog("state.isPaused is null, setting to false")
        state.isPaused = false
    } else if (!(state.isPaused instanceof Boolean)) {
        appLog("WARN: state.isPaused is not a boolean (${state.isPaused}), resetting to false")
        state.isPaused = false
    }
    appLog("Setting initial label to app name: ${app.name}")
    app.updateLabel(app.name)
    updateAppLabel(state.isPaused)
}

// Update the app with pause state handling
def updatedWithPause(Map options = [:], Closure initializeClosure) {
    appLog("Updating app with pause state, options: ${options}")
    // Validate inputs
    if (initializeClosure == null) {
        appLog("ERROR: initializeClosure is null, cannot proceed with update")
        return
    }
    if (!(options instanceof Map)) {
        appLog("WARN: options is not a Map (${options}), using default empty map")
        options = [:]
    }

    updateAppLabel(state.isPaused)
    if (state.isPaused) {
        appLog("App is paused, unsubscribing and unscheduling")
        if (options.unsubscribe != false) {
            try {
                unsubscribe()
                appLog("Successfully unsubscribed from events")
            } catch (Exception e) {
                appLog("ERROR: Failed to unsubscribe events: ${e.message}")
            }
        }
        if (options.unschedule != false) {
            try {
                unschedule()
                appLog("Successfully unscheduled tasks")
            } catch (Exception e) {
                appLog("ERROR: Failed to unschedule tasks: ${e.message}")
            }
        }
        if (options.parent && childApps?.size() > 0) {
            appLog("App is a parent (childApps size: ${childApps.size()}), calling pauseAllChildApps")
            pauseAllChildApps()
        }
    } else {
        appLog("App is not paused, unsubscribing and unscheduling before reinitializing")
        if (options.unsubscribe != false) {
            try {
                unsubscribe()
                appLog("Successfully unsubscribed from events")
            } catch (Exception e) {
                appLog("ERROR: Failed to unsubscribe events: ${e.message}")
            }
        }
        if (options.unschedule != false) {
            try {
                unschedule()
                appLog("Successfully unscheduled tasks")
            } catch (Exception e) {
                appLog("ERROR: Failed to unschedule tasks: ${e.message}")
            }
        }
        if (options.parent && childApps?.size() > 0) {
            appLog("App is a parent (childApps size: ${childApps.size()}), calling resumeAllChildApps")
            resumeAllChildApps()
        }
        try {
            appLog("Calling initializeClosure to reinitialize app")
            initializeClosure()
            appLog("Successfully reinitialized app")
        } catch (Exception e) {
            appLog("ERROR: Failed to reinitialize app: ${e.message}")
        }
    }
}

// Handle button clicks for pause/resume
def appButtonHandler(String buttonName) {
    appLog("Handling button click: ${buttonName}")
    if (buttonName == null) {
        appLog("ERROR: buttonName is null, cannot handle button click")
        return
    }
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
        appLog("State changed from ${previousState} to ${state.isPaused}, scheduling UI refresh")
        runIn(1, "refreshUI")
    } else {
        appLog("State unchanged (${state.isPaused}), no UI refresh needed")
    }
}

// Attempt to force a UI refresh
def refreshUI() {
    appLog("Forcing UI refresh after state change")
    try {
        updateAppLabel(state.isPaused)
        appLog("Successfully updated label for UI refresh")
    } catch (Exception e) {
        appLog("ERROR: Failed to update label for UI refresh: ${e.message}")
    }
}

// Update the app's label to reflect paused state
def updateAppLabel(boolean paused) {
    appLog("Updating app label, paused: ${paused}")
    if (paused == null) {
        appLog("WARN: paused parameter is null, defaulting to false")
        paused = false
    }
    try {
        def baseLabel = app.getLabel()?.replaceAll(/ <span.*<\/span>/, "")?.replaceAll(/\s*\(Paused\)/, "") ?: app.name
        if (baseLabel == null) {
            appLog("WARN: baseLabel is null, using app.name: ${app.name}")
            baseLabel = app.name
        }
        if (paused) {
            appLog("Setting label to paused state: ${baseLabel} (Paused)")
            app.updateLabel("$baseLabel <span style='color:red'>(Paused)</span>")
        } else {
            appLog("Setting label to unpaused state: ${baseLabel}")
            app.updateLabel(baseLabel)
        }
    } catch (Exception e) {
        appLog("ERROR: Failed to update app label: ${e.message}")
    }
}

// Pause all child apps
def pauseAllChildApps() {
    appLog("Pausing all child apps")
    if (childApps == null) {
        appLog("ERROR: childApps is null, cannot pause child apps")
        return
    }
    appLog("Number of child apps: ${childApps.size()}")
    childApps.each { child ->
        try {
            if (child == null) {
                appLog("WARN: Encountered null child app, skipping")
                return
            }
            appLog("Pausing child app: ${child.label}")
            child.appButtonHandler("pauseApp")
        } catch (Exception e) {
            appLog("ERROR: Failed to pause child app: ${e.message}")
        }
    }
    state.childPauseStatus = "Paused all child apps at ${new Date()}"
    appLog("Updated childPauseStatus: ${state.childPauseStatus}")
}

// Resume all child apps
def resumeAllChildApps() {
    appLog("Resuming all child apps")
    if (childApps == null) {
        appLog("ERROR: childApps is null, cannot resume child apps")
        return
    }
    appLog("Number of child apps: ${childApps.size()}")
    childApps.each { child ->
        try {
            if (child == null) {
                appLog("WARN: Encountered null child app, skipping")
                return
            }
            appLog("Resuming child app: ${child.label}")
            child.appButtonHandler("resumeApp")
        } catch (Exception e) {
            appLog("ERROR: Failed to resume child app: ${e.message}")
        }
    }
    state.childPauseStatus = "Resumed all child apps at ${new Date()}"
    appLog("Updated childPauseStatus: ${state.childPauseStatus}")
}

// Get the pause/resume status of child apps
def getChildPauseStatus() {
    def status = state.childPauseStatus ?: "No child pause/resume actions yet."
    appLog("Returning child pause status: ${status}")
    return status
}

// Helper method to log from the library context
def appLog(String msg) {
    try {
        if (app?.name == null || app?.getLabel() == null) {
            log."${msg.startsWith('warn:') || msg.startsWith('ERROR:') ? 'warn' : 'info'}"("PauseResumeLib: ${msg.replace('warn: ', '').replace('ERROR: ', '')}")
        } else {
            log."${msg.startsWith('warn:') || msg.startsWith('ERROR:') ? 'warn' : 'info'}"("${app.name} (${app.getLabel()}): ${msg.replace('warn: ', '').replace('ERROR: ', '')}")
        }
    } catch (Exception e) {
        log.warn("PauseResumeLib: Failed to log message: ${msg}, error: ${e.message}")
    }
}

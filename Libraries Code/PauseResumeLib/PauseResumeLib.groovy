/**
 *  PauseResumeLib
 *
 *  Author: Ultronumus Of Space
 *  Creator: Grok, created by xAI
 *  Version: 1.0.0
 *  Date: May 30, 2025
 *  Description: A library for managing pause/resume functionality for child apps in Hubitat. Provides UI controls
 *  to pause or resume all child apps, with support for individual child app pause/resume, including visual indication
 *  in the app header.
 *  License: MIT
 */

library (
    author: "Ultronumus Of Space",
    contributor: "Grok (xAI)",
    category: "Utilities",
    description: "A library for managing pause/resume functionality for child apps in Hubitat",
    name: "PauseResumeLib",
    namespace: "UltronOfSpace",
    documentationLink: "https://github.com/UltronOfSpace/Hubitat/tree/main/Libraries%20Code/PauseResumeLib"
)

// Add a Pause/Resume section for child apps to the UI
def addPauseResumeSection() {
    appLog("Adding pause/resume section to UI")
    // Check if this is a parent app (no parent) or a child app (has a parent)
    def isChildApp = app.getParent() != null

    if (isChildApp) {
        // Child app UI
        updateAppLabel(state.isPaused ?: false)
        section("App Control") {
            if (state.isPaused) {
                input name: "resumeApp", type: "button", title: "Resume", submitOnChange: true
            } else {
                input name: "pauseApp", type: "button", title: "Pause", submitOnChange: true
            }
        }
    } else {
        // Parent app UI
        def summary = getChildAppsPauseSummary()
        section("Child Apps Control") {
            if (childApps?.size() > 0) {
                def total = summary.total ?: 0
                def paused = summary.paused ?: 0
                def running = summary.running ?: 0
                // Construct grammatically correct summary
                def pausedText = paused == 1 ? "1 child app is paused" : "${paused} child apps are paused"
                def runningText = running == 1 ? "1 child app is running" : "${running} child apps are running"
                paragraph "${pausedText}, ${runningText}."
                paragraph "Note: State changes from other devices may require clicking 'Done' and reopening this app to refresh."
                if (paused == total && total > 0) {
                    input name: "resumeAllChildren", type: "button", title: "Resume All Child Apps", submitOnChange: true
                } else if (running == total && total > 0) {
                    input name: "pauseAllChildren", type: "button", title: "Pause All Child Apps", submitOnChange: true
                } else if (total > 0) {
                    input name: "pauseAllChildren", type: "button", title: "Pause All Child Apps", submitOnChange: true
                    input name: "resumeAllChildren", type: "button", title: "Resume All Child Apps", submitOnChange: true
                }
            } else {
                paragraph "No child apps."
            }
        }
    }
}

// Initialize the app (simplified, no parent pause state)
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
    if (app.getParent() != null) { // Only child apps update their labels
        updateAppLabel(state.isPaused)
    }
}

// Update the app with simplified pause state handling (for child apps only)
def updatedWithPause(Map options = [:], Closure initializeClosure) {
    appLog("Updating app with pause state, options: ${options}")
    if (initializeClosure == null) {
        appLog("ERROR: initializeClosure is null, cannot proceed with update")
        return
    }
    if (!(options instanceof Map)) {
        appLog("WARN: options is not a Map (${options}), using default empty map")
        options = [:]
    }

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
        try {
            appLog("Calling initializeClosure to reinitialize app")
            initializeClosure()
            appLog("Successfully reinitialized app")
        } catch (Exception e) {
            appLog("ERROR: Failed to reinitialize app: ${e.message}")
        }
    }
    // Update the child app's label after state change
    if (app.getParent() != null) {
        updateAppLabel(state.isPaused)
    }
}

// Update the app's label to reflect paused state (for child apps only)
def updateAppLabel(boolean paused) {
    appLog("Updating app label, paused: ${paused}")
    if (paused == null) {
        appLog("WARN: paused parameter is null, defaulting to false")
        paused = false
    }
    try {
        def baseLabel = app.getLabel() ?: app.name
        baseLabel = baseLabel.replaceAll(/ <span.*<\/span>/, "").replaceAll(/\s*\(Paused\)/, "")
        def newLabel = paused ? "$baseLabel <span style='color:red'>(Paused)</span>" : baseLabel
        appLog("Setting label to: ${newLabel}")
        app.updateLabel(newLabel)
    } catch (Exception e) {
        appLog("ERROR: Failed to update app label: ${e.message}")
    }
}

// Handle button clicks for pause/resume (simplified for child apps only)
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
            updatedWithPause(parent: true, { initialize() })
            break
        case "resumeApp":
            state.isPaused = false
            appLog("Resuming app, new isPaused: ${state.isPaused}")
            updatedWithPause(parent: true, { initialize() })
            break
        case "pauseAllChildren":
            pauseAllChildApps()
            break
        case "resumeAllChildren":
            resumeAllChildApps()
            break
        default:
            appLog("warn: Unhandled app button: $buttonName")
    }
}

// Pause all child apps that are not already paused
def pauseAllChildApps() {
    appLog("Pausing all child apps that are not already paused")
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
            if (!child.isPaused()) {
                appLog("Pausing child app: ${child.label}")
                child.appButtonHandler("pauseApp")
            } else {
                appLog("Child app ${child.label} is already paused, skipping")
            }
        } catch (Exception e) {
            appLog("ERROR: Failed to pause child app: ${e.message}")
        }
    }
    state.childPauseStatus = "Paused all child apps at ${new Date()}"
    appLog("Updated childPauseStatus: ${state.childPauseStatus}")
}

// Resume all child apps that are not already resumed
def resumeAllChildApps() {
    appLog("Resuming all child apps that are not already resumed")
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
            if (child.isPaused()) {
                appLog("Resuming child app: ${child.label}")
                child.appButtonHandler("resumeApp")
            } else {
                appLog("Child app ${child.label} is already running, skipping")
            }
        } catch (Exception e) {
            appLog("ERROR: Failed to resume child app: ${e.message}")
        }
    }
    state.childPauseStatus = "Resumed all child apps at ${new Date()}"
    appLog("Updated childPauseStatus: ${state.childPauseStatus}")
}

// Get the pause/resume status of child apps
def getChildAppsPauseSummary() {
    appLog("Getting pause summary for child apps")
    def summary = [paused: 0, running: 0, total: 0]
    if (childApps) {
        childApps.each { child ->
            try {
                if (child.isPaused()) {
                    summary.paused++
                } else {
                    summary.running++
                }
                summary.total++
            } catch (Exception e) {
                appLog("ERROR: Failed to get pause state for child app: ${e.message}")
            }
        }
    } else {
        appLog("No child apps found")
    }
    appLog("Child apps pause summary: paused=${summary.paused}, running=${summary.running}, total=${summary.total}")
    return summary
}

// Check if the app is paused (used by child apps)
def isPaused() {
    return state.isPaused ?: false
}

// Helper method to log from the library context
def appLog(String msg) {
    try {
        if (app?.name == null || app?.getLabel() == null) {
            log."${msg.startsWith('warn:') || msg.startsWith('ERROR:') ? 'warn' : 'info'}"("PauseResumeLib: ${msg.replace('warn: ', '').replace('ERROR: ', '')}")
        } else {
            log."${msg.startsWith('warn:') || msg.startsWith('ERROR:') ? 'warn' : 'info'}"("${app.name} (${app.getLabel() ?: app.name}): ${msg.replace('warn: ', '').replace('ERROR: ', '')}")
        }
    } catch (Exception e) {
        log.warn("PauseResumeLib: Failed to log message: ${msg}, error: ${e.message}")
    }
}

/**
 *  AppControlLib
 *
 *  Author: Ultronumus Of Space
 *  Creator: Grok, created by xAI
 *  Version: 1.0.2
 *  Date: June 2, 2025
 *  Description: A library for managing app control functionality, including pause/resume, state display, and naming for standalone, parent, and child apps in Hubitat.
 *  License: MIT
 */

library (
    author: "Ultronumus Of Space",
    contributor: "Grok (xAI)",
    category: "Utilities",
    description: "A library for managing app control functionality, including pause/resume, state display, and naming for standalone, parent, and child apps in Hubitat",
    name: "AppControlLib",
    namespace: "UltronOfSpace",
    importUrl: "https://raw.githubusercontent.com/UltronOfSpace/Hubitat/main/Libraries%20Code/AppControlLib/AppControlLib.groovy"
)

// Single entry point to enable pause/resume functionality
def enablePauseResume() {
    initializeWithPause()
    try {
        app.metaClass.appButtonHandler = this.&appButtonHandler
    } catch (Exception e) {
        log.error "AppControlLib: Failed to set up appButtonHandler - ${e.message}"
    }
}

// Render the control section for standalone, parent, and child apps
def renderPauseResumeControls() {
    try {
        enablePauseResume()
        def isChildApp = app.getParent() != null
        def isStandaloneApp = !isChildApp && (!childApps || childApps?.size() == 0)
        def uiData = isChildApp || isStandaloneApp ? getChildAppUIData() : getParentAppUIData()

        if (uiData == null || (isChildApp && uiData.buttonName == null) || (!isChildApp && !isStandaloneApp && uiData.pausedText == null)) {
            log.error "${isChildApp ? 'Child' : isStandaloneApp ? 'Standalone' : 'Parent'} App: UI data is null"
            section("Control") {
                paragraph "Error: Failed to load app control data."
            }
        } else {
            section("Control") {
                if (isChildApp || isStandaloneApp) {
                    // Child or standalone app: Render single Pause/Resume button
                    input name: uiData.buttonName, type: "button", title: uiData.buttonTitle, submitOnChange: true
                } else {
                    // Parent app: Render Pause All/Resume All buttons and summary
                    if (uiData.showPauseAll) {
                        input name: "pauseAllChildren", type: "button", title: "Pause All Child Apps", submitOnChange: true
                    }
                    if (uiData.showResumeAll) {
                        input name: "resumeAllChildren", type: "button", title: "Resume All Child Apps", submitOnChange: true
                    }
                    paragraph uiData.pausedText
                    if (uiData.runningText) {
                        paragraph uiData.runningText
                    }
                    paragraph "    Note: State changes from other devices may require clicking 'Done' and reopening this app to refresh."
                }
            }
        }
    } catch (Exception e) {
        log.error "${app.getParent() != null ? 'Child' : (childApps?.size() > 0 ? 'Parent' : 'Standalone')} App: Failed to enable pause/resume functionality - ${e.message}"
        section("Control") {
            paragraph "Error: Failed to enable pause/resume functionality - ${e.message}"
        }
    }
}

// Get UI data for a child or standalone app (Pause/Resume button)
def getChildAppUIData() {
    if (state.isPaused == null) {
        state.isPaused = false
        log.info "Initialized state.isPaused to false"
    }
    assert state.isPaused != null : "state.isPaused must not be null"
    updateAppLabel(state.isPaused)
    return [buttonName: state.isPaused ? "resumeApp" : "pauseApp", buttonTitle: state.isPaused ? "Resume" : "Pause"]
}

// Get UI data for a parent app (summary and buttons)
def getParentAppUIData() {
    def summary = getChildAppsPauseSummary()
    if (summary == null) {
        log.error "Summary is null"
        return [pausedText: "No child apps.", runningText: "", showPauseAll: false, showResumeAll: false]
    }
    def total = summary.total ?: 0
    def paused = summary.paused ?: 0
    def running = summary.running ?: 0

    def data = [:]
    if (childApps?.size() > 0) {
        def pausedText = paused == 1 ? "1 child app is paused." : "${paused} child apps are paused."
        def runningText = running == 1 ? "1 child app is running." : "${running} child apps are running."
        data.pausedText = pausedText
        data.runningText = runningText
        data.showPauseAll = running > 0
        data.showResumeAll = paused > 0
    } else {
        data.pausedText = "No child apps."
        data.runningText = ""
        data.showPauseAll = false
        data.showResumeAll = false
    }
    return data
}

// Initialize the app (simplified, no parent pause state)
def initializeWithPause() {
    if (state.isPaused == null) {
        state.isPaused = false
    } else if (!(state.isPaused instanceof Boolean)) {
        state.isPaused = false
    }
    assert state.isPaused != null && state.isPaused instanceof Boolean : "state.isPaused must be a boolean"
    if (app.getParent() != null || (!childApps || childApps?.size() == 0)) {
        updateAppLabel(state.isPaused)
    }
}

// Helper methods for updatedWithPause
private def unsubscribeEvents(Map options) {
    if (options.unsubscribe != false) {
        try {
            unsubscribe()
        } catch (Exception e) {
            log.error "AppControlLib: Failed to unsubscribe events: ${e.message}"
        }
    }
}

private def unscheduleTasks(Map options) {
    if (options.unschedule != false) {
        try {
            unschedule()
        } catch (Exception e) {
            log.error "AppControlLib: Failed to unschedule tasks: ${e.message}"
        }
    }
}

// Update the app with simplified pause state handling (for child or standalone apps)
def updatedWithPause(Map options = [:], Closure initializeClosure) {
    if (initializeClosure == null) {
        log.error "AppControlLib: initializeClosure is null, cannot proceed with update"
        return
    }
    if (!(options instanceof Map)) {
        options = [:]
    }

    unsubscribeEvents(options)
    unscheduleTasks(options)

    if (!state.isPaused) {
        try {
            initializeClosure()
        } catch (Exception e) {
            log.error "AppControlLib: Failed to reinitialize app: ${e.message}"
        }
    }
    if (app.getParent() != null || (!childApps || childApps?.size() == 0)) {
        updateAppLabel(state.isPaused)
    }
}

// Update the app's label to reflect paused state (for child or standalone apps)
def updateAppLabel(boolean paused) {
    if (paused == null) {
        paused = false
    }
    try {
        def baseLabel = app.getLabel() ?: app.name ?: "New App"
        baseLabel = baseLabel.replaceAll(/ <span.*<\/span>/, "").replaceAll(/\s*\(Paused\)/, "")
        def newLabel = paused ? "${baseLabel} <span style='color:red'>(Paused)</span>" : baseLabel
        app.updateLabel(newLabel)
        log.info "After updateAppLabel, app.getLabel() = ${app.getLabel()}"
    } catch (Exception e) {
        log.error "AppControlLib: Failed to update app label: ${e.message}"
    }
}

// Handle button clicks for pause/resume and saving app name
def appButtonHandler(String buttonName) {
    if (buttonName == null) {
        log.error "AppControlLib: buttonName is null, cannot handle button click"
        return
    }
    switch (buttonName) {
        case "saveAppName":
            if (settings?.appName) {
                def newLabel = settings.appName
                log.info "AppControlLib: Saving new app name: ${newLabel}"
                try {
                    app.updateLabel(newLabel)
                    log.info "After updateLabel, app.getLabel() = ${app.getLabel()}"
                    updateAppLabel(state.isPaused ?: false)
                    app.removeSetting("appName")
                } catch (Exception e) {
                    log.error "AppControlLib: Failed to save new app name - ${e.message}"
                }
            } else {
                log.warn "AppControlLib: No new app name provided to save"
            }
            break
        case "pauseApp":
            state.isPaused = true
            updatedWithPause(parent: true, { if (this.respondsTo('initialize')) initialize() })
            break
        case "resumeApp":
            state.isPaused = false
            updatedWithPause(parent: true, { if (this.respondsTo('initialize')) initialize() })
            break
        case "pauseAllChildren":
            pauseAllChildApps()
            break
        case "resumeAllChildren":
            resumeAllChildApps()
            break
        default:
            log.warn "AppControlLib: Unhandled app button: ${buttonName}"
    }
}

// Pause all child apps that are not already paused
def pauseAllChildApps() {
    if (childApps == null) {
        log.error "AppControlLib: childApps is null, cannot pause child apps"
        return
    }
    childApps.each { child ->
        try {
            if (child == null) {
                return
            }
            if (!child.isPaused()) {
                child.appButtonHandler("pauseApp")
            }
        } catch (Exception e) {
            log.error "AppControlLib: Failed to pause child app: ${e.message}"
        }
    }
    state.childPauseStatus = "Paused all child apps at ${new Date()}"
}

// Resume all child apps that are not already resumed
def resumeAllChildApps() {
    if (childApps == null) {
        log.error "AppControlLib: childApps is null, cannot resume child apps"
        return
    }
    childApps.each { child ->
        try {
            if (child == null) {
                return
            }
            if (child.isPaused()) {
                child.appButtonHandler("resumeApp")
            }
        } catch (Exception e) {
            log.error "AppControlLib: Failed to resume child app: ${e.message}"
        }
    }
    state.childPauseStatus = "Resumed all child apps at ${new Date()}"
}

// Get the pause/resume status of child apps
def getChildAppsPauseSummary() {
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
                log.error "AppControlLib: Failed to get pause state for child app: ${e.message}"
            }
        }
    }
    assert summary.total == summary.paused + summary.running : "Inconsistent summary counts"
    return summary
}

// Check if the app is paused (used by child or standalone apps)
def isPaused() {
    return state.isPaused ?: false
}

// Helper method to log from the library context
def appLog(String msg) {
    try {
        if (app?.name == null || app?.getLabel() == null) {
            log."${msg.startsWith('warn:') || msg.startsWith('ERROR:') ? 'warn' : 'info'}"("AppControlLib: ${msg.replace('warn: ', '').replace('ERROR: ', '')}")
        } else {
            log."${msg.startsWith('warn:') || msg.startsWith('ERROR:') ? 'warn' : 'info'}"("${app.name} (${app.getLabel() ?: app.name}): ${msg.replace('warn: ', '').replace('ERROR: ', '')}")
        }
    } catch (Exception e) {
        log.warn "AppControlLib: Failed to log message: ${msg}, error: ${e.message}"
    }
}
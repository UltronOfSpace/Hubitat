/**
 *  AppControlLib Child App Example
 *
 *  Author: Ultronumus Of Space
 *  Creator: Grok, created by xAI
 *  Version: 1.0.3
 *  Date: June 2, 2025
 *  Description: A child app example that displays its name and includes a placeholder for additional code.
 *  Integrates with AppControlLib for app control functionality.
 *  License: MIT
 */

#include UltronOfSpace.AppControlLib

definition(
    name: "AppControlLib Child App Example",
    namespace: "UltronOfSpace",
    author: "Ultronumus Of Space",
    description: "A child app example with pause/resume functionality",
    category: "Utilities",
    parent: "UltronOfSpace:AppControlLib Parent App Example",
    iconUrl: "",
    iconX2Url: "",
    importUrl: "https://raw.githubusercontent.com/UltronOfSpace/Hubitat/main/ExampleApps/ParentAndChild/AppControlLib_Child_App_Example.groovy"
)

preferences {
    page(name: "mainPage", install: true, uninstall: true)
}

def mainPage() {
    dynamicPage(name: "mainPage") {
        section {
            input name: "appName", type: "text", title: "Enter a name for this instance of the application (optional)", defaultValue: "Little Punk Kid App"
        }
        section {
            renderPauseResumeControls()
        }
        section("Placeholder for Additional Code") {
            paragraph "This section is a placeholder for additional code or settings."
        }
    }
}

def installed() {
    log.info "${app.getName()} installed"
    // Set the label to the value of appName if provided, otherwise use the default "Little Punk Kid App"
    def initialLabel = settings?.appName ?: "Little Punk Kid App"
    app.updateLabel(initialLabel)
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
    // This method can be used to set up subscriptions or schedules
}

def getAppBaseLabel() {
    def baseLabel = app.getLabel() ?: app.name ?: "Little Punk Kid App"
    log.info "getAppBaseLabel: Retrieved baseLabel as ${baseLabel}"
    return baseLabel.replaceAll(/ <span.*<\/span>/, "").replaceAll(/\s*\(Paused\)/, "")
}
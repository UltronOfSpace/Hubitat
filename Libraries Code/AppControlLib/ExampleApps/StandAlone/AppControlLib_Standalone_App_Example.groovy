/**
 *  AppControlLib Standalone App Example
 *
 *  Author: Ultronumus Of Space
 *  Creator: Grok, created by xAI
 *  Version: 1.0.3
 *  Date: June 2, 2025
 *  Description: A standalone app example that uses AppControlLib for pause/resume functionality.
 *  License: MIT
 */

#include UltronOfSpace.AppControlLib

definition(
    name: "AppControlLib Standalone App Example",
    namespace: "UltronOfSpace",
    author: "Ultronumus Of Space",
    description: "A standalone app example with pause/resume functionality",
    category: "Utilities",
    iconUrl: "",
    iconX2Url: "",
    singleInstance: true,
    importUrl: "https://raw.githubusercontent.com/UltronOfSpace/Hubitat/main/ExampleApps/Standalone/AppControlLib_Standalone_App_Example.groovy"
)

preferences {
    page(name: "mainPage", install: true, uninstall: true)
}

def mainPage() {
    dynamicPage(name: "mainPage") {
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
    // This method can be used to set up subscriptions or schedules
}

def getAppBaseLabel() {
    def baseLabel = app.getLabel() ?: app.name ?: "Lone Wolf App"
    log.info "getAppBaseLabel: Retrieved baseLabel as ${baseLabel}"
    return baseLabel.replaceAll(/ <span.*<\/span>/, "").replaceAll(/\s*\(Paused\)/, "")
}
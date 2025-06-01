/**
 * AppControlLibParentAppTemplate
 *
 * A parent app template for Hubitat using AppControlLib to manage child apps with pause/resume functionality.
 *
 * Author: Rostov (with help from SuperGrok)
 * Date: 2025-06-01
 * License: MIT
 */

definition(
    name: "AppControlLibParentAppTemplate",
    namespace: "UltronOfSpace",
    author: "Rostov",
    description: "A greasy big boss app for Hubitat",
    category: "Convenience",
    iconUrl: "",
    iconX2Url: "",
    importUrl: "https://raw.githubusercontent.com/UltronOfSpace/Hubitat/main/Libraries%20Code/AppControlLib/AppControlLibParentAppTemplate.groovy"
)

#include UltronOfSpace.AppControlLib

preferences {
    page(name: "mainPage", title: "", install: true, uninstall: true)
}

def mainPage() {
    dynamicPage(name: "mainPage", title: "", install: true, uninstall: true) {
        section("App Name") {
            input "appName", "text", title: "Enter a name for this app (optional)", required: false, submitOnChange: true
        }
        section {
            renderPauseResumeControls()
        }
        section("Child Apps") {
            app(name: "childApps", appName: "AppControlLibChildAppTemplate", namespace: "UltronOfSpace", title: "Create New Child App", multiple: true)
        }
    }
}

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
    // Clean up child apps
    getAllChildApps().each { child ->
        deleteChildApp(child.id)
    }
}

def initialize() {
    // Optional: Add subscriptions or schedules here
}
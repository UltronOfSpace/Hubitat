/**
 * AppControlLibStandaloneAppTemplate
 *
 * A standalone app template for Hubitat using AppControlLib to provide pause/resume functionality.
 *
 * Author: Rostov (with help from SuperGrok)
 * Date: 2025-06-01
 * License: MIT
 */

definition(
    name: "AppControlLibStandaloneAppTemplate",
    namespace: "UltronOfSpace",
    author: "Rostov",
    description: "A greasy lone wolf app for Hubitat",
    category: "Convenience",
    iconUrl: "",
    iconX2Url: "",
    importUrl: "https://raw.githubusercontent.com/UltronOfSpace/Hubitat/main/Libraries%20Code/AppControlLib/AppControlLibStandaloneAppTemplate.groovy"
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
        section("Placeholder for Additional Code") {
            paragraph "Add your custom settings here, bud!"
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
}

def initialize() {
    // Optional: Add subscriptions or schedules here
}
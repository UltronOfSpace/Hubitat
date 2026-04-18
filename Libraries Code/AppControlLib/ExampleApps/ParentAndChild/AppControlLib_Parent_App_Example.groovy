/**
 *  AppControlLib Parent App Example
 *
 *  Author: Ultronumus Of Space
 *  Creator: Grok, created by xAI
 *  Version: 1.0.3
 *  Date: June 2, 2025
 *  Description: A parent app example that allows creating child apps.
 *  Integrates with AppControlLib for app control functionality.
 *  License: MIT
 */

#include UltronOfSpace.AppControlLib

definition(
    name: "AppControlLib Parent App Example",
    namespace: "UltronOfSpace",
    author: "Ultronumus Of Space",
    description: "A parent app example that manages child apps with pause/resume functionality",
    category: "Utilities",
    iconUrl: "",
    iconX2Url: "",
    singleInstance: true,
    importUrl: "https://raw.githubusercontent.com/UltronOfSpace/Hubitat/main/ExampleApps/ParentAndChild/AppControlLib_Parent_App_Example.groovy"
)

preferences {
    page(name: "mainPage", install: true, uninstall: true)
}

def mainPage() {
    dynamicPage(name: "mainPage") {
        section {
            renderPauseResumeControls()
        }
        section("Child Apps") {
            app(
                name: "childApps",
                appName: "AppControlLib Child App Example",
                namespace: "UltronOfSpace",
                title: "Create New Child App",
                submitOnChange: true,
                multiple: true
            )
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
    // Remove all child apps, including potential orphans
    def allChildApps = getAllChildApps()
    def childAppsToRemove = allChildApps.findAll { app ->
        app.name == "AppControlLib Child App Example" && app.getNamespace() == "UltronOfSpace"
    }
    childAppsToRemove.each { child ->
        try {
            log.info "Removing child app: ${child.getLabel()} (ID: ${child.id})"
            deleteChildApp(child.id)
        } catch (Exception e) {
            log.error "Failed to remove child app ${child.getLabel()} (ID: ${child.id}): ${e.message}"
        }
    }
}

def getAppBaseLabel() {
    def baseLabel = app.getLabel() ?: app.name ?: "Big Boss App"
    log.info "getAppBaseLabel: Retrieved baseLabel as ${baseLabel}"
    return baseLabel.replaceAll(/ <span.*<\/span>/, "").replaceAll(/\s*\(Paused\)/, "")
}
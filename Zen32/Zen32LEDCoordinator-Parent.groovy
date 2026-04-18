/*
 * License: MIT
 *
 * Author: Ultronumus Of Space
 * Coded by: SuperGrok
 * Date: 2025-06-02
 *
 * Description:
 * The Zen32 LED Coordinator - Parent app is a Hubitat Elevation app that manages multiple child apps to synchronize
 * the LED states (color and brightness) of Zooz Zen32 scene controllers with associated devices. It provides a clean
 * interface to create and manage child apps for different Zen32 devices or configurations. Version 1.2.1.0 includes
 * UI improvements in the child app with immediate global settings application.
 *
 * Installation:
 * 1. Upload this file to Hubitat Apps Code as "Zen32LEDCoordinator-Parent.groovy".
 * 2. Upload the child app file "Zen32LEDCoordinator-Child.groovy" to Apps Code.
 * 3. Install the parent app from Apps > Add User App.
 * 4. Create child apps within the parent app for each Zen32 device.
 *
 * Usage:
 * Use the parent app to create and manage child apps, each handling a specific Zen32 device. Configure child apps to
 * sync LED states with devices. For detailed instructions, refer to the README.md file. Now, let’s get those LEDs
 * shining brighter than a supernova, without any pesky gremlins messing with your setup!
 */

definition(
    name: "Zen32 LED Coordinator",
    namespace: "UltronOfSpace",
    author: "Ultronumus Of Space",
    description: "Manages child apps to synchronize Zooz Zen32 LED states with devices",
    category: "Convenience",
    iconUrl: "",
    iconX2Url: "",
    singleInstance: true
)

preferences {
    page(name: "mainPage")
}

def mainPage() {
    dynamicPage(name: "mainPage", hideTitle: true, install: true, uninstall: true) {
        section {
            app(
                name: "childApps1",
                appName: "Zen32 LED Coordinator - Child",
                namespace: "UltronOfSpace",
                title: "Create New Zen32 Child App",
                submitOnChange: true,
                multiple: true
            )
        }
    }
}

def installed() {
    initialize()
}

def updated() {
    unsubscribe()
    initialize()
}

def initialize() {
    state.version = "1.2.1.0" // Set version once during initialization
    log.debug "There are ${childApps.size()} child apps."
    childApps.each { child ->
        log.debug "Child app: ${child.label}"
    }
}

def checkSlavesExist(slaves, master, iterations) {
    if (childApps.isEmpty()) {
        return slaves.contains(master) && iterations > 0
    }
    for (child in childApps) {
        for (slave in slaves) {
            if (child.getMasterId() == slave || master == slave) {
                log.debug "Master Child: ${child.getMasterId()}, Master: ${master}, Slave: ${slave}"
                if (iterations <= 0) {
                    return true
                } else {
                    log.warn "Iterations remaining: ${iterations}"
                    return checkSlavesExist(slaves, master, iterations - 1)
                }
            }
        }
    }
    return false
}
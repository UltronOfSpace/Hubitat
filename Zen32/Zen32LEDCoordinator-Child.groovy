/*
 * License: MIT
 *
 * Author: Ultronumus Of Space
 * Coded by: Grok
 * Date: 2025-05-19
 *
 * Description:
 * The Adaptive Lighting Emulator app emulates Apple's HomeKit Adaptive Lighting feature for Hubitat-compatible devices.
 * It adjusts the color temperature of selected individual lights throughout the day to mimic natural light patterns,
 * using device-specific transitions and a cosine-based Kelvin curve for smooth changes. The app includes optimized
 * device profiling to determine the best transition times for each device. Note: This app is designed for individual
 * lights and may not fully support group devices.
 *
 * Installation:
 * 1. Upload this file to Hubitat Apps Code as "AdaptiveLightingEmulator.groovy".
 * 2. Create an instance of the app from the Hubitat Apps interface.
 *
 * Usage:
 * Configure the app by selecting the color temperature-capable lights, setting timing adjustments for sunrise and sunset
 * offsets, defining the minimum and maximum color temperatures, and optionally enabling brightness interaction and debug
 * logging. The app will automatically profile the devices to determine optimal transition times and adjust the lights'
 * color temperatures accordingly.
 */

definition(
    name: "Zen32 LED Coordinator - Child",
    namespace: "UltronOfSpace",
    parent: "UltronOfSpace:Zen32 LED Coordinator",
    author: "Ultronumus Of Space",
    description: "Synchronizes Zooz Zen32 button LED states with devices",
    category: "Convenience",
    iconUrl: "",
    iconX2Url: ""
)

// Returns brightness options as a map of labels to values (0–100).
def getBrightnessOptions() {
    return ["Off": 0, "Low": 30, "Medium": 60, "High": 100]
}

// Returns color options as a list of color names in alphabetical order.
def getColorOptions() {
    return ["blue", "cyan", "green", "magenta", "red", "white", "yellow"]
}

// Configures color and brightness input dropdowns with a formatted header and titles.
def configureColorAndBrightnessInputs(colorSettingName, brightnessSettingName, defaultBrightness, state) {
    paragraph "<h3><u>When synced device is ${state}</u></h3>"
    input name: colorSettingName, type: "enum", title: "<b><i>LED Color...</i></b>", width: 2, options: getColorOptions(), required: false, defaultValue: "white"
    input name: brightnessSettingName, type: "enum", title: "<b><i>LED Brightness..</i></b>", width: 2, options: getBrightnessOptions().keySet(), required: false, defaultValue: defaultBrightness
}

preferences {
    page(name: "mainPage", install: true, uninstall: true)
    page(name: "buttonPage1", title: "Button 1 Configuration")
    page(name: "buttonPage2", title: "Button 2 Configuration")
    page(name: "buttonPage3", title: "Button 3 Configuration")
    page(name: "buttonPage4", title: "Button 4 Configuration")
    page(name: "buttonPage5", title: "Button 5 Configuration")
}

// Defines the main configuration page for the app.
def mainPage() {
    dynamicPage(name: "mainPage", title: "<h1><u>Zen32 Button LED Sync</u></h1>", install: true, uninstall: true) {
        section("This app helps you synchronize LED colors and states for your Zen32 buttons. Configure your settings below.") {
            configureMainPageInputs()
            configureButtonLinks()
        }
    }
}

// Configures inputs for Zooz device, global settings, and synchronization logic.
def configureMainPageInputs() {
    label title: "Enter a name for this instance of the application (optional):", required: false
    paragraph "<h2>Select the Zen32 to configure.</h2>"
    input name: "ZoozZen32", type: "device.ZoozSceneController(ZEN32)", title: "<b>Zooz Zen32 Device</b>", multiple: false
    input name: "useGlobalSettings", type: "bool", title: "<i>Use Global LED Settings for All Buttons?</i>", defaultValue: false, submitOnChange: true

    if (settings.useGlobalSettings != state.useGlobalSettings) {
        assert settings.useGlobalSettings != null : "useGlobalSettings is null"
        state.useGlobalSettings = settings.useGlobalSettings
        synchronizeButtonSettings()
        updateDeviceLEDs()
    }

    if (settings.useGlobalSettings) {
        paragraph "<b>The options below set a single color and brightness for all buttons based on its sync device state (On or Off).<br> <i>This will override individual button selections.</i></b>"
        configureColorAndBrightnessInputs("setAllOnColor", "setAllOnBrightness", "High", "ON")
        configureColorAndBrightnessInputs("setAllOffColor", "setAllOffBrightness", "Off", "OFF")
    }
}

// Configures navigation links to individual button configuration pages.
def configureButtonLinks() {
    paragraph "<h2>Button Configuration</h2>"
    for (int i = 1; i <= 5; i++) {
        configureButtonLink(i)
    }
    paragraph "Enable Debug Logging?"
    input name: "enableLogging", type: "bool", title: "Enable Debug Logging?", defaultValue: false
}

// Configures a single button navigation link with dynamic description.
def configureButtonLink(buttonNumber) {
    def actionVerbs = [
        "Toggle": "Toggle",
        "Switch On": "Turn On",
        "Switch Off": "Turn Off"
    ]
    def descriptionLines = []

    // Handle Pushed action first
    if (settings."Button${buttonNumber}Action" && settings."Button${buttonNumber}SyncToDevice") {
        def action = settings."Button${buttonNumber}Action"
        def deviceName = settings."Button${buttonNumber}SyncToDevice".displayName
        def actionVerb = actionVerbs[action] ?: action
        descriptionLines << "When Pressed, ${actionVerb} ${deviceName}"
    }

    // Define the order for additional actions
    def actionOrder = ["Double-Tapped", "Held", "Released"]

    // Handle additional actions in the specified order
    actionOrder.each { action ->
        if (settings."Button${buttonNumber}SelectedActions"?.contains(action)) {
            def deviceSetting = "Button${buttonNumber}${action.replace('-', '')}Device"
            def actionSetting = "Button${buttonNumber}${action.replace('-', '')}Action"
            if (settings."${actionSetting}" && settings."${deviceSetting}") {
                def act = settings."${actionSetting}"
                def devName = settings."${deviceSetting}".displayName
                def actVerb = actionVerbs[act] ?: act
                descriptionLines << "When ${action}, ${actVerb} ${devName}"
            }
        }
    }

    def description = descriptionLines ? descriptionLines.join("<br>") : "Click to Configure"
    href name: "buttonPage${buttonNumber}", title: "<b>Button ${buttonNumber}</b>", description: description, page: "buttonPage${buttonNumber}"
}

def buttonPage1() { buttonPage(1) }
def buttonPage2() { buttonPage(2) }
def buttonPage3() { buttonPage(3) }
def buttonPage4() { buttonPage(4) }
def buttonPage5() { buttonPage(5) }

// Defines the configuration page for a specific button.
def buttonPage(int buttonNumber) {
    dynamicPage(name: "buttonPage${buttonNumber}", title: "Button ${buttonNumber} Configuration", install: false, uninstall: false) {
        section("Button ${buttonNumber} Configuration") {
            configureButtonSyncInputs(buttonNumber)
            configureButtonActionInputs(buttonNumber)
            configureButtonLEDInputs(buttonNumber)
        }
    }
}

// Configures input for synchronizing a button’s LED with a device.
def configureButtonSyncInputs(int buttonNumber) {
    paragraph "<h3><u>Synchronize Button ${buttonNumber} LED to this Device or Group State</u></h3>"
    input name: "Button${buttonNumber}SyncToDevice", type: "capability.switch", title: "Sync Device", multiple: false
}

// Configures inputs for button actions (pushed, held, double-tapped, released).
def configureButtonActionInputs(int buttonNumber) {
    input name: "Button${buttonNumber}Action", type: "enum", title: "Select the action when the button is Pushed", width: 3, required: false, options: ["Toggle", "Switch On", "Switch Off"], defaultValue: "Toggle"
    input name: "Button${buttonNumber}SelectedActions", type: "enum", title: "Select additional button actions to configure for Button ${buttonNumber}", width: 3, multiple: true, options: ["Double-Tapped", "Held", "Released"], submitOnChange: true

    def selectedActions = settings."Button${buttonNumber}SelectedActions" ?: []
    def actionOrder = ["Double-Tapped", "Held", "Released"]

    actionOrder.each { action ->
        if (selectedActions.contains(action)) {
            switch (action) {
                case "Double-Tapped":
                    configureDoubleTappedActionInputs(buttonNumber)
                    break
                case "Held":
                    configureHeldActionInputs(buttonNumber)
                    break
                case "Released":
                    configureReleasedActionInputs(buttonNumber)
                    break
            }
        } else {
            // Clean up both the device and action settings when this action is deselected
            def base = "Button${buttonNumber}${action.replace('-', '')}"
            app.removeSetting("${base}Device")
            app.removeSetting("${base}Action")
        }
    }
}

// Configures inputs for the "Double-Tapped" button action.
def configureDoubleTappedActionInputs(int buttonNumber) {
    paragraph "Button ${buttonNumber} Double-Tapped"
    input name: "Button${buttonNumber}DoubleTappedDevice", type: "capability.switch", title: "Select device to control when you DOUBLE-TAP Button ${buttonNumber}", required: true, submitOnChange: true
    input name: "Button${buttonNumber}DoubleTappedAction", type: "enum", title: "Select the action when Button ${buttonNumber} is Double-Tapped", required: true, options: ["Toggle", "Switch On", "Switch Off"]
}

// Configures inputs for the "Held" button action.
def configureHeldActionInputs(int buttonNumber) {
    paragraph "Button ${buttonNumber} Held"
    input name: "Button${buttonNumber}HeldDevice", type: "capability.switch", title: "Select device to control when you HOLD Button ${buttonNumber}", required: true, submitOnChange: true
    input name: "Button${buttonNumber}HeldAction", type: "enum", title: "Select the action when Button ${buttonNumber} is Held", required: true, options: ["Toggle", "Switch On", "Switch Off"]
}

// Configures inputs for the "Released" button action.
def configureReleasedActionInputs(int buttonNumber) {
    paragraph "Button ${buttonNumber} Released"
    input name: "Button${buttonNumber}ReleasedDevice", type: "capability.switch", title: "Select device to control when you RELEASE Button ${buttonNumber}", required: true, submitOnChange: true
    input name: "Button${buttonNumber}ReleasedAction", type: "enum", title: "Select the action when Button ${buttonNumber} is Released", required: true, options: ["Toggle", "Switch On", "Switch Off"]
}

// Configures inputs for individual button LED color and brightness when global settings are disabled.
def configureButtonLEDInputs(int buttonNumber) {
    if (!state.useGlobalSettings) {
        configureColorAndBrightnessInputs("Button${buttonNumber}LEDOnColor", "Button${buttonNumber}LEDOnBrightness", "High", "ON")
        configureColorAndBrightnessInputs("Button${buttonNumber}LEDOffColor", "Button${buttonNumber}LEDOffBrightness", "Off", "OFF")
    }
}

// Applies global LED settings to all buttons when enabled.
def synchronizeButtonSettings() {
    if (settings.useGlobalSettings) {
        for (int i = 1; i <= 5; i++) {
            assert i >= 1 && i <= 5 : "Invalid button index: ${i}"
            def globalOnColor = settings.setAllOnColor ?: "white"
            def globalOffColor = settings.setAllOffColor ?: "white"
            def globalOnBrightness = settings.setAllOnBrightness ?: "High"
            def globalOffBrightness = settings.setAllOffBrightness ?: "Off"

            app.updateSetting("Button${i}LEDOnColor", [type: "enum", value: globalOnColor])
            app.updateSetting("Button${i}LEDOffColor", [type: "enum", value: globalOffColor])
            app.updateSetting("Button${i}LEDOnBrightness", [type: "enum", value: globalOnBrightness])
            app.updateSetting("Button${i}LEDOffBrightness", [type: "enum", value: globalOffBrightness])

            state."Button${i}LEDOnColor" = globalOnColor
            state."Button${i}LEDOffColor" = globalOffColor
            state."Button${i}LEDOnBrightness" = globalOnBrightness
            state."Button${i}LEDOffBrightness" = globalOffBrightness
        }
    }
}

// Updates app state and subscriptions when settings change.
def updated() {
    debugLog("Updated")
    assert settings != null : "Settings object is null"
    state.useGlobalSettings = settings.useGlobalSettings
    unsubscribe()
    subscribeToEvents()
    synchronizeButtonSettings()
    updateDeviceLEDs()
    updateRelayLEDBehavior()
}

// Initializes app state, sets default label, and sets up subscriptions.
def installed() {
    debugLog("Installed")
    state.version = "1.2.10.3"
    state.useGlobalSettings = settings.useGlobalSettings ?: false
    if (!app.getLabel() || app.getLabel() == "Zen32 LED Coordinator - Child") {
        app.updateLabel("Zen32 LED Coordinator")
    }
    subscribeToEvents()
    updateDeviceLEDs()
    updateRelayLEDBehavior()
}

// Subscribes to Zooz Zen32 and associated device events.
def subscribeToEvents() {
    if (ZoozZen32) {
        debugLog("Subscribing to ZoozZen32 button events")
        subscribe(ZoozZen32, "pushed", "buttonEventHandler")
        subscribe(ZoozZen32, "held", "buttonEventHandler")
        subscribe(ZoozZen32, "doubleTapped", "buttonEventHandler")
        subscribe(ZoozZen32, "released", "buttonEventHandler")
    } else {
        log.warn "ZoozZen32 device is not selected."
    }

    for (int i = 1; i <= 5; i++) {
        assert i >= 1 && i <= 5 : "Invalid button index: ${i}"
        subscribeToDeviceSwitchEvents(i)
    }
}

// Subscribes to switch events for a button’s sync device only.
def subscribeToDeviceSwitchEvents(i) {
    def syncDevice = settings."Button${i}SyncToDevice"
    if (syncDevice) {
        debugLog("Subscribing to switch events for Button${i}SyncToDevice (${syncDevice.displayName})")
        subscribe(syncDevice, "switch", "Button${i}SyncHandler")
    } else {
        debugLog("No sync device selected for Button ${i}")
    }
}

// Updates LED states for all buttons based on sync device states.
def updateDeviceLEDs() {
    boolean useGlobal = state.useGlobalSettings ?: false
    for (int i = 1; i <= 5; i++) {
        def syncDevice = settings."Button${i}SyncToDevice"
        if (syncDevice) {
            def deviceState = syncDevice.currentValue("switch")
            debugLog("Button ${i} - Sync device state is: ${deviceState}")
            def color = deviceState == "on" ? (useGlobal ? settings.setAllOnColor : settings."Button${i}LEDOnColor") 
                                            : (useGlobal ? settings.setAllOffColor : settings."Button${i}LEDOffColor")
            def brightnessLabel = deviceState == "on" ? (useGlobal ? settings.setAllOnBrightness : settings."Button${i}LEDOnBrightness") 
                                                     : (useGlobal ? settings.setAllOffBrightness : settings."Button${i}LEDOffBrightness")
            def brightness = getBrightnessOptions()[brightnessLabel] ?: 0
            ZoozZen32.setLED(i, color, brightness)
        } else {
            log.warn "No sync device selected for Button ${i}."
        }
    }
}

// Updates a button’s LED based on a sync device’s switch event.
def updateLEDForDevice(buttonIndex, evt) {
    assert evt != null : "Event is null for button ${buttonIndex}"
    def deviceState = evt.value
    assert deviceState != null : "Device state is null for button ${buttonIndex}"
    boolean useGlobal = state.useGlobalSettings ?: false
    debugLog("Button ${buttonIndex} - Device state is: ${deviceState}")

    def color = deviceState == "on" 
                ? (useGlobal ? settings.setAllOnColor : settings."Button${buttonIndex}LEDOnColor") 
                : (useGlobal ? settings.setAllOffColor : settings."Button${buttonIndex}LEDOffColor")
    def brightnessLabel = deviceState == "on" 
                          ? (useGlobal ? settings.setAllOnBrightness : settings."Button${buttonIndex}LEDOnBrightness") 
                          : (useGlobal ? settings.setAllOffBrightness : settings."Button${buttonIndex}LEDOffBrightness")
    def brightness = getBrightnessOptions()[brightnessLabel] ?: 0
    assert ZoozZen32 != null : "ZoozZen32 device is null for button ${buttonIndex}"
    debugLog("Setting LED for Button ${buttonIndex} to ${deviceState.toUpperCase()} color: ${color} with brightness: ${brightness}")
    ZoozZen32.setLED(buttonIndex, color, brightness)
}

// Handles switch events for a button’s sync device.
def ButtonSyncHandler(buttonIndex, evt) {
    debugLog("Button ${buttonIndex} Sync Handler triggered with event value: ${evt.value}")
    updateLEDForDevice(buttonIndex, evt)
}

def Button1SyncHandler(evt) { ButtonSyncHandler(1, evt) }
def Button2SyncHandler(evt) { ButtonSyncHandler(2, evt) }
def Button3SyncHandler(evt) { ButtonSyncHandler(3, evt) }
def Button4SyncHandler(evt) { ButtonSyncHandler(4, evt) }
def Button5SyncHandler(evt) { ButtonSyncHandler(5, evt)
                             
}

// Handles button events (pushed, held, double-tapped, released) and performs associated actions.
def buttonEventHandler(evt) {
    if (!evt) return

    def rawButton = (evt.value ?: "0").toInteger()
    def rawAction = evt.name

    Integer physicalButton = rawButton
    Integer tapCount = 1
    String actionType = rawAction

    // Community ZEN32 driver: multi-taps come in as pushed w/ button #s 6-25
    if (rawAction == "pushed" && rawButton > 5) {
        physicalButton = ((rawButton - 1) % 5) + 1          // 1..5
        tapCount = ((rawButton - 1) / 5).toInteger() + 1    // 1..5

        // Only map double-tap to your "doubleTapped" path
        if (tapCount == 2) {
            actionType = "doubleTapped"
        } else {
            // ignore triple/quad/quint unless you want to add support
            debugLog("Ignoring multi-tap ${tapCount} for physical button ${physicalButton} (raw button ${rawButton})")
            return
        }
    }

    // For held/released in that community driver, it's typically still buttons 1..5.
    if (!(physicalButton >= 1 && physicalButton <= 5)) {
        debugLog("Ignoring event with unexpected button number: raw=${rawButton}, physical=${physicalButton}, action=${rawAction}")
        return
    }

    debugLog("Button event received: Physical Button ${physicalButton}, Action: ${actionType} (raw: ${rawAction} ${rawButton})")

    def associatedDevice = null
    def action = null

    switch (actionType) {
        case "pushed":
            associatedDevice = settings."Button${physicalButton}SyncToDevice"
            action = settings."Button${physicalButton}Action"
            break
        case "held":
            associatedDevice = settings."Button${physicalButton}HeldDevice"
            action = settings."Button${physicalButton}HeldAction"
            break
        case "doubleTapped":
            associatedDevice = settings."Button${physicalButton}DoubleTappedDevice"
            action = settings."Button${physicalButton}DoubleTappedAction"
            break
        case "released":
            associatedDevice = settings."Button${physicalButton}ReleasedDevice"
            action = settings."Button${physicalButton}ReleasedAction"
            break
    }

    if (associatedDevice && action) {
        performActionOnDevice(associatedDevice, action)
    } else {
        infoLog("Button ${physicalButton} ${actionType} on ${ZoozZen32?.displayName}. No action/device configured.")
    }
}

// Performs the specified action (Toggle, Switch On, Switch Off) on a device.
def performActionOnDevice(device, action) {
    assert device != null : "Device is null in performActionOnDevice"
    assert action in ["Toggle", "Switch On", "Switch Off"] : "Invalid action: ${action}"
    switch (action) {
        case "Toggle":
            def switchState = device.currentValue("switch")
            assert switchState != null : "Switch state is null for device ${device}"
            switchState == "on" ? device.off() : device.on()
            break
        case "Switch On":
            device.on()
            break
        case "Switch Off":
            device.off()
            break
    }
}

// Updates the relay LED behavior for the Zooz Zen32 device.
def updateRelayLEDBehavior() {
    if (settings.ZoozZen32) {
        try {
            assert settings.ZoozZen32 != null : "ZoozZen32 is null in updateRelayLEDBehavior"
            settings.ZoozZen32.updateSetting("relayLEDBehavior", [value: 4, type: "NUMBER"])
            log.info "relayLEDBehavior updated to '4' for ${settings.ZoozZen32.displayName}"
            settings.ZoozZen32.configure()
        } catch (e) {
            log.error "Error updating relayLEDBehavior for ${settings.ZoozZen32.displayName}: ${e.message}"
        }
    } else {
        log.warn "ZoozZen32 device is not selected or is null."
    }
}

// Logs debug messages if debug logging is enabled.
def debugLog(msg) {
    if (settings.enableLogging) {
        assert msg != null : "Debug log message is null"
        log.debug msg
    }
}

// Logs info messages if debug logging is enabled.
def infoLog(msg) {
    if (settings.enableLogging) {
        assert msg != null : "Info log message is null"
        log.info msg
    }
}
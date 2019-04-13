
metadata {
    definition (name: "Yeelight White", namespace: "simontether", author: "Simon Tether", mnmn: "SmartThings", vid: "generic-dimmer") {
    capability "Switch Level"
    capability "Color Temperature"
    capability "Switch"
    capability "Relay Switch"
    capability "Refresh"
    capability "Actuator"

    command "reset"
    command "refresh"

    attribute "colorName", "string"

    command "coolWhite"
    command "warmWhite"
    command "daylight"
    }

    simulator {
    }

    //Tiles on Device Panel
    standardTile("switch", "device.switch", width: 1, height: 1, canChangeIcon: true) {
        state "on", label:'${name}', action:"switch.off", icon:"st.lights.philips.hue-single", backgroundColor:"#00a0dc", nextState:"turningOff"
        state "off", label:'${name}', action:"switch.on", icon:"st.lights.philips.hue-single", backgroundColor:"#ffffff", nextState:"turningOn"
        state "turningOn", label:'${name}', action:"switch.off", icon:"st.lights.philips.hue-single", backgroundColor:"#00a0dc", nextState:"turningOff"
        state "turningOff", label:'${name}', action:"switch.on", icon:"st.lights.philips.hue-single", backgroundColor:"#ffffff", nextState:"turningOn"
    }
    standardTile("refresh", "device.switch", inactiveLabel: false, decoration: "flat") {
        state "default", label:"", action:"refresh.refresh", icon:"st.secondary.refresh"
    }
    standardTile("reset", "device.reset", inactiveLabel: false, decoration: "flat") {
        state "default", label:"Reset Color", action:"reset", icon:"st.lights.philips.hue-single"
    }
    controlTile("levelSliderControl", "device.level", "slider", height: 1, width: 2, inactiveLabel: false, range:"(0..100)") {
        state "level", action:"switch level.setLevel"
    }
    valueTile("level", "device.level", inactiveLabel: false, decoration: "flat") {
        state "level", label: 'Level ${currentValue}%'
    }
    controlTile("colorTempControl", "device.colorTemperature", "slider", height: 1, width: 1, inactiveLabel: false, range:"(2700..6500)") {
        state "colorTemperature", action:"setColorTemperature"
    }
    standardTile("coolWhite", "device.colorName", height: 1, width: 1, decoration: "flat", inactiveLabel: false, canChangeIcon: false) {
        state "off", label:"cool white", action:"coolWhite", icon:"st.illuminance.illuminance.dark", backgroundColor:"#D8D8D8", defaultState: true
        //state "coolWhite", label:"cool white", action:"off", icon:"st.illuminance.illuminance.bright", backgroundColor:"#FF33CB"
    }
    standardTile("warmWhite", "device.colorName", height: 1, width: 1, decoration: "flat", inactiveLabel: false, canChangeIcon: false) {
        state "off", label:"warm white", action:"warmWhite", icon:"st.illuminance.illuminance.dark", backgroundColor:"#D8D8D8", defaultState: true
        //state "warmWhite", label:"warm white", action:"off", icon:"st.illuminance.illuminance.bright", backgroundColor:"#FF33CB"
    }
    standardTile("daylight", "device.colorName", height: 1, width: 1, decoration: "flat", inactiveLabel: false, canChangeIcon: false) {
        state "off", label:"daylight", action:"daylight", icon:"st.illuminance.illuminance.dark", backgroundColor:"#D8D8D8", defaultState: true
        //state "daylight", label:"daylight", action:"off", icon:"st.illuminance.illuminance.bright", backgroundColor:"#FF33CB"
        }
    main(["switch"])
    details(["switch", "levelSliderControl", "reset", "colorTempControl", "refresh", "coolWhite", "warmWhite", "daylight"])
}

    //Settings Page
    preferences {
        input name: "DeviceLocalLan", type: "string", title:"Yeelight IP Address", description:"Enter Bulb's IP address", defaultValue:"", required: true, displayDuringSetup: true
        input name: "defaultONLevel", type: "number", title: "Default ON Level:", description:"Enter Default ON Level", defaultValue:75, range: "1..100", required: false, displayDuringSetup: true
        input name: "coolWhiteValue", type: "number", title: "Cool White Value (K):", description:"Enter Cool White Value", defaultValue:6500, range: "1700..6500", required: false, displayDuringSetup: true
        input name: "warmWhiteValue", type: "number", title: "Warm White Value (K):", description:"Enter Warm White Value", defaultValue:3000, range: "1700..6500", required: false, displayDuringSetup: true       
        input name: "dayWhiteValue", type: "number", title: "Daylight White Value (K):", description:"Enter Daylight White Value", defaultValue:4500, range: "1700..6500", required: false, displayDuringSetup: true
    }

def installed()  {
    log.debug "installed"
}

def updated()  {
    log.debug "updated"
}

//Parse incoming from Yeelight -----NOT WORKING-----
def parse(String description) {
    log.debug "Response '${description}'"
}

//Reset Yeelight to White, 6000K
def reset() {
    log.debug "reset"
    //getProp()
    delayBetween([
        on(),
        setColorTemperature(6000)
    ], 300)
}

//Send Command to Yeelight
def transmit(yeelightCommand) {
    def String ipaddr = DeviceLocalLan
    def String hexIp = ipaddr.tokenize('.').collect {String.format('%02X', it.toInteger())}.join()
    def String myNetworkID = "${hexIp}:D893"
    device.deviceNetworkId = myNetworkID
    log.debug "Device Network ID " + myNetworkID
    def transmittedData = new physicalgraph.device.HubAction(yeelightCommand, physicalgraph.device.Protocol.LAN, myNetworkID)
    log.debug "Sent " + transmittedData
    return sendHubCommand(transmittedData)
}

//Get Current Properties of Yeelight
def getProp() {
    log.debug "getProp"
    transmit("""{"id":1,"method":"get_prop","params":["power", "bright", "ct", "rgb", "name"]}\r\n""")
}

//Turn Yeelight ON
def on() {
    //getProp()
    delayBetween([
        transmit("""{"id": 1, "method": "set_power", "params":["on", "smooth", 500]}\r\n"""),
        transmit("""{"id": 1, "method": "set_bright", "params":[${defaultONLevel}, "smooth", 100]}\r\n""")
    ], 300)
    sendEvent(name: "switch", value: "on")
    sendEvent(name: "level", value: defaultONLevel)
}

//Turn Yeelight OFF
def off() {
    //getProp()
    transmit("""{"id": 1, "method": "set_power", "params":["off", "smooth", 500]}\r\n""")
    sendEvent(name: "switch", value: "off")
}

//Check if Yeelight is off and turn on if it is - called from setLevel and setColorTemp
def powerCheck() {
    def powerState = device.currentValue("switch")
    if (powerState == "off") {
        transmit("""{"id": 1, "method": "set_power", "params":["on", "sudden"]}\r\n""")
        sendEvent(name: "switch", value: "on")
    }
}

//Set Yeelight Dim Level
def setLevel(level) {
    //getProp()
    powerCheck()
    if(level < 2) {
        off()
        sendEvent(name: "level", value: 0)
    }
    else {
        transmit("""{"id": 1, "method": "set_bright", "params":[$level, "smooth", 100]}\r\n""")
        sendEvent(name: "level", value: level)
    }
}

//Set Yeelight Colour Temperature
def setColorTemperature(kelvin) {
    //getProp()
    powerCheck()
    if(kelvin > 6500) kelvin = 6500
    log.debug "setColorTemperature: ${kelvin}K"
    transmit("""{"id": 1, "method": "set_ct_abx", "params":[${kelvin}, "smooth", 500]}\r\n""")
    sendEvent(name: "colorTemperature", value: kelvin)
}

// Colour Temperature Shortcuts
def coolWhite() {
    setColorTemperature(coolWhiteValue)
}
def warmWhite() {
    setColorTemperature(warmWhiteValue)
}
def daylight() {
    setColorTemperature(dayWhiteValue)
}

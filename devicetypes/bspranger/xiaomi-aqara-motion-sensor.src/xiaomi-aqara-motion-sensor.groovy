/**
 *  Xiaomi Aqara Motion Sensor
 *  Version 1.2
 *
 *
 *  Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 *  in compliance with the License. You may obtain a copy of the License at:
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software distributed under the License is distributed
 *  on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License
 *  for the specific language governing permissions and limitations under the License.
 *
 *  Original device handler code by a4refillpad, adapted for use with Aqara model by bspranger
 *  Additional contributions to code by alecm, alixjg, bspranger, gn0st1c, foz333, jmagnuson, rinkek, ronvandegraaf, snalee, tmleafs, twonk, & veeceeoh 
 * 
 *  Known issues:
 *  Xiaomi sensors do not seem to respond to refresh requests
 *  Inconsistent rendering of user interface text/graphics between iOS and Android devices - This is due to SmartThings, not this device handler
 *  Pairing Xiaomi sensors can be difficult as they were not designed to use with a SmartThings hub. See 
 *
 */

metadata {
    definition (name: "Xiaomi Aqara Motion Sensor", namespace: "bspranger", author: "bspranger") {
        capability "Motion Sensor"
        capability "Illuminance Measurement"
        capability "Configuration"
        capability "Battery"
        capability "Sensor"
        capability "Health Check"

        attribute "lastCheckin", "String"
        attribute "lastCheckinDate", "String"
        attribute "lastMotion", "String"
        attribute "batteryRuntime", "String"

        fingerprint endpointId: "01", profileId: "0104", deviceId: "0107", inClusters: "0000,FFFF,0406,0400,0500,0001,0003", outClusters: "0000,0019", manufacturer: "LUMI", model: "lumi.sensor_motion.aq2", deviceJoinName: "Xiaomi Aqara Motion Sensor"
        fingerprint profileId: "0104", deviceId: "0104", inClusters: "0000, 0400, 0406, FFFF", outClusters: "0000, 0019", manufacturer: "LUMI", model: "lumi.sensor_motion", deviceJoinName: "Xiaomi Aqara Motion Sensor"

        command "resetBatteryRuntime"
        command "stopMotion"
    }

    simulator {
    }

    tiles(scale: 2) {
        multiAttributeTile(name:"motion", type:"generic", width: 6, height: 4) {
            tileAttribute ("device.motion", key: "PRIMARY_CONTROL") {
                attributeState "active", label:'Motion', icon:"st.motion.motion.active", backgroundColor:"#00a0dc"
                attributeState "inactive", label:'No Motion', icon:"st.motion.motion.inactive", backgroundColor:"#ffffff"
            }
            tileAttribute("device.lastMotion", key: "SECONDARY_CONTROL") {
                attributeState("default", label:'${currentValue}')
            }
        }
        valueTile("battery", "device.battery", decoration:"flat", inactiveLabel: false, width: 2, height: 2) {
            state "battery", label:'${currentValue}%', unit:"%", icon:"https://raw.githubusercontent.com/bspranger/Xiaomi/master/images/XiaomiBattery.png",
            backgroundColors:[
                [value: 10, color: "#bc2323"],
                [value: 26, color: "#f1d801"],
                [value: 51, color: "#44b621"]
            ]
        }
        valueTile("spacer", "spacer", decoration: "flat", inactiveLabel: false, width: 1, height: 1) {
            state "default", label:''
        }
        valueTile("illuminance", "device.illuminance", decoration:"flat", inactiveLabel: false, width: 2, height: 2) {
            state "default", label:'${currentValue}\nlux', unit:"lux",
		backgroundColors: [
            	[value:0, color:"#000000"],
            	[value:1, color:"#07212c"],
            	[value:25, color:"#0f4357"],
            	[value:50, color:"#12536d"],
            	[value:100, color:"#1a7599"],
            	[value:150, color:"#2196c4"],
            	[value:250, color:"#3bb0de"],
            	[value:500, color:"#51b8e1"],
            	[value:750, color:"#66c1e5"],
            	[value:1000, color:"#7ccae9"],
            	[value:1500, color: "#92d3ed"] 
           ]
        }
        standardTile("reset", "device.reset", inactiveLabel: false, decoration:"flat", width: 2, height: 2) {
            state "default", action:"stopMotion", label:'Reset Motion', icon:"st.motion.motion.active"
        }
        valueTile("lastcheckin", "device.lastCheckin", decoration:"flat", inactiveLabel: false, width: 4, height: 1) {
            state "default", label:'Last Event:\n ${currentValue}'
        }
        valueTile("batteryRuntime", "device.batteryRuntime", inactiveLabel: false, decoration:"flat", width: 4, height: 1) {
             state "batteryRuntime", label:'Battery Changed:\n ${currentValue}'
        }
		main(["motion"])
		details(["motion", "battery", "illuminance", "reset", "spacer", "lastcheckin", "spacer", "spacer", "batteryRuntime", "spacer"])
	}

	preferences {
		//Test Mode & Motion Reset Config
		input description: "", type: "paragraph", element: "paragraph", title: "MOTION RESET & TESTING MODE"
		input description: "This setting only changes how long MOTION DETECTED is reported in SmartThings. After the number of seconds entered here, if no new activity is detected then NO MOTION is reported. If this setting is left blank, a default of 61 seconds is used to match the behavior of the sensor's hardware in Normal Mode (for more information, please refer to the text under the next setting.)", type: "paragraph", element: "paragraph", title: "MOTION RESET"
		input "motionreset", "number", title: "", description: "Number of seconds (default = 61)", range: "1..7200"
		input description: "After it is paired or the reset button is short-pressed, the sensor hardware goes into Test Mode. During the hardware Test Mode the sensor will report any detected motion every 6 seconds, to help with choosing a suitable installation location. After 1-2 hours of inactivity the sensor will go into Normal Mode which means the hardware remains 'blind' to all activity for 60 seconds after motion is detected.\n\nTurning on this Testing Mode setting will override the Motion Reset time setting to match the behavior of the sensor during its hardware Test Mode. After turning on Testing Mode using the toggle below, it will remain active for 60 minutes, but can be manually turned off by using the toggle a second time. During Testing Mode a reminder message is displayed in the main tile.", type: "paragraph", element: "paragraph", title: "TESTING MODE"
		input name: "testMode", type: "bool", title: "Toggle Testing Mode?"
		//Date & Time Config
		input description: "", type: "paragraph", element: "paragraph", title: "DATE & CLOCK"    
		input name: "dateformat", type: "enum", title: "Set Date Format\n US (MDY) - UK (DMY) - Other (YMD)", description: "Date Format", options:["US","UK","Other"]
		input name: "clockformat", type: "bool", title: "Use 24 hour clock?"
		//Battery Reset Config
		input description: "If you have installed a new battery, the toggle below will reset the Changed Battery date to help remember when it was changed.", type: "paragraph", element: "paragraph", title: "CHANGED BATTERY DATE RESET"
		input name: "battReset", type: "bool", title: "Battery Changed?"
		//Battery Voltage Offset
		input description: "Only change the settings below if you know what you're doing.", type: "paragraph", element: "paragraph", title: "ADVANCED SETTINGS"
		input name: "voltsmax", title: "Max Volts\nA battery is at 100% at __ volts\nRange 2.8 to 3.4", type: "decimal", range: "2.8..3.4", defaultValue: 3
		input name: "voltsmin", title: "Min Volts\nA battery is at 0% (needs replacing) at __ volts\nRange 2.0 to 2.7", type: "decimal", range: "2..2.7", defaultValue: 2.5
	}	
}

// Parse incoming device messages to generate events
def parse(String description) {
    log.debug "${device.displayName} parsing: $description"

	// Determine current time and date in the user-selected date format and clock style
    def now = formatDate()    
    def nowDate = new Date(now).getTime()

	// Any report - motion, lux & Battery - results in a lastCheckin event and update to Last Event tile
	// However, only a non-parseable report results in lastCheckin being displayed in events log
    sendEvent(name: "lastCheckin", value: now, displayed: false)
    sendEvent(name: "lastCheckinDate", value: nowDate, displayed: false)

    Map map = [:]
	
	// Send message data to appropriate parsing function based on the type of report	
    if (description?.startsWith('illuminance:')) {
        map = parseIlluminance(description)
    }
    else if (description?.startsWith('read attr -')) {
        map = parseReportAttributeMessage(description)
    }
    else if (description?.startsWith('catchall:')) {
        map = parseCatchAllMessage(description)
    }

    log.debug "${device.displayName} parse returned: $map"
    def result = map ? createEvent(map) : null

    return result
}

// Parse illuminance report
private Map parseIlluminance(String description) {
    def lux = ((description - "illuminance: ").trim()) as int

    def result = [
        name: 'illuminance',
        value: lux,
        unit: "lux",
        isStateChange: true,
        descriptionText : "${device.displayName} illuminance was ${lux} lux"
    ]
    return result
}
// Parse motion active report or model name message on reset button press
private Map parseReportAttributeMessage(String description) {
    def cluster = description.split(",").find {it.split(":")[0].trim() == "cluster"}?.split(":")[1].trim()
    def attrId = description.split(",").find {it.split(":")[0].trim() == "attrId"}?.split(":")[1].trim()
    def value = description.split(",").find {it.split(":")[0].trim() == "value"}?.split(":")[1].trim()
	Map resultMap = [:]

	// Because the sensor only sends motion detected messages, a reset to no motion is performed in code
    if (cluster == "0406" & value == "01" & state.ignoreMotion == false) {
		log.debug "${device.displayName} detected motion"
		def secondsReset = motionreset ? motionreset : 61
		resultMap = [
			name: 'motion',
			value: 'active',
			descriptionText: "${device.displayName} detected motion"
		]
		sendEvent(name: "lastMotion", value: (state.testModeActive ? ">>>>>  Testing Mode is ACTIVE  <<<<<" : "Last Motion: ${formatDate()}"), displayed: false)
		if (state.testModeActive == true) {
			runIn(7, stopMotion)
		} else {
			if (secondsReset > 10) {
				state.ignoreMotion = true
				runIn(secondsReset - 6, stopIgnoreMotion)
				log.debug "${device.displayName}: Motion detected, additional motion detected messages will be ignored for ${secondsReset - 6} seconds"
			}
			runIn(secondsReset, stopMotion)
		}
	}
	else if (cluster == "0000" && attrId == "0005") {
        def modelName = ""
        // Parsing the model
        for (int i = 0; i < value.length(); i+=2) {
            def str = value.substring(i, i+2);
            def NextChar = (char)Integer.parseInt(str, 16);
            modelName = modelName + NextChar
        }
        log.debug "${device.displayName} reported: cluster: ${cluster}, attrId: ${attrId}, model:${modelName}"
    }
    return resultMap
}

// Check catchall for battery voltage data to pass to getBatteryResult for conversion to percentage report
private Map parseCatchAllMessage(String description) {
	Map resultMap = [:]
	def catchall = zigbee.parse(description)
	log.debug catchall

	if (catchall.clusterId == 0x0000) {
		def MsgLength = catchall.data.size()
		// Xiaomi CatchAll does not have identifiers, first UINT16 is Battery
		if ((catchall.data.get(0) == 0x01 || catchall.data.get(0) == 0x02) && (catchall.data.get(1) == 0xFF)) {
			for (int i = 4; i < (MsgLength-3); i++) {
				if (catchall.data.get(i) == 0x21) { // check the data ID and data type
					// next two bytes are the battery voltage
					resultMap = getBatteryResult((catchall.data.get(i+2)<<8) + catchall.data.get(i+1))
					break
				}
			}
		}
	}
	return resultMap
}

// Convert raw 4 digit integer voltage value into percentage based on minVolts/maxVolts range
private Map getBatteryResult(rawValue) {
    // raw voltage is normally supplied as a 4 digit integer that needs to be divided by 1000
    // but in the case the final zero is dropped then divide by 100 to get actual voltage value 
    def rawVolts = rawValue / 1000
	def minVolts
    def maxVolts

    if (voltsmin == null || voltsmin == "")
    	minVolts = 2.5
    else
   	minVolts = voltsmin
    
    if (voltsmax == null || voltsmax == "")
    	maxVolts = 3.0
    else
	maxVolts = voltsmax
    
    def pct = (rawVolts - minVolts) / (maxVolts - minVolts)
    def roundedPct = Math.min(100, Math.round(pct * 100))

    def result = [
        name: 'battery',
        value: roundedPct,
        unit: "%",
        isStateChange: true,
        descriptionText : "${device.displayName} Battery at ${roundedPct}% (${rawVolts} Volts)"
    ]

    return result
}

// If currently in 'active' motion detected state, stopMotion() resets to 'inactive' state and displays 'no motion'
def stopMotion() {
	def secondsReset = motionreset ? motionreset : 61
	if (device.currentState('motion')?.value != "inactive") {
		sendEvent(name:"motion", value:"inactive", isStateChange: true)
		log.debug "${device.displayName}: Reset to no motion (inactive)${state.testModeActive ? "" : " after ${motionreset?:61} seconds"}"
	}
}

// Resume allowing the DTH to handle motion detected messages
def stopIgnoreMotion() {
	state.ignoreMotion = false
	if (!state.testModeActive)
		log.debug "${device.displayName}: Finished ignoring motion detected messages"
}

// Deactivate Testing Mode setting
def finishTestMode() {
	if (state.testModeActive) {
		log.debug "${device.displayName}: Testing Mode has been deactivated"
		state.testModeActive = false
	}
}

//Reset the date displayed in Battery Changed tile to current date
def resetBatteryRuntime(paired) {
	def now = formatDate(true)
	def newlyPaired = paired ? " for newly paired sensor" : ""
	sendEvent(name: "batteryRuntime", value: now)
	log.debug "${device.displayName}: Setting Battery Changed to current date${newlyPaired}"
}

// installed() runs just after a sensor is paired using the "Add a Thing" method in the SmartThings mobile app
def installed() {
	state.ignoreMotion = false
	state.testModeActive = false
	if (!batteryRuntime) resetBatteryRuntime(true)
	checkIntervalEvent("installed")
}

// configure() runs after installed() when a sensor is paired
def configure() {
	log.debug "${device.displayName}: configuring"
	state.ignoreMotion = false
	state.testModeActive = false
	stopMotion()
	if (!batteryRuntime) resetBatteryRuntime(true)
	checkIntervalEvent("configured")
	return
}

// updated() will run twice every time user presses save in preference settings page
def updated() {
	checkIntervalEvent("updated")
    stopMotion()
	if (testMode) {
		if (!state.testModeActive) {
			runIn(120, finishTestMode)
			state.testModeActive = true
			log.debug "${device.displayName}: Testing Mode activated for 60 minutes"
		} else
			finishTestMode()
		device.updateSetting("testMode", false)
	}
	if (!state.testModeActive)
			log.debug "${device.displayName}: Motion Reset time set to ${(motionreset ? motionreset : 61)} seconds"
	stopIgnoreMotion()
	if (battReset) {
		resetBatteryRuntime()
		device.updateSetting("battReset", false)
	}
}

private checkIntervalEvent(text) {
    // Device wakes up every 1 hours, this interval allows us to miss one wakeup notification before marking offline
    log.debug "${device.displayName}: Configured health checkInterval when ${text}()"
    sendEvent(name: "checkInterval", value: 2 * 60 * 60 + 2 * 60, displayed: false, data: [protocol: "zigbee", hubHardwareId: device.hub.hardwareID])
}

def formatDate(batteryReset) {
    def correctedTimezone = ""
    def timeString = clockformat ? "HH:mm:ss" : "h:mm:ss aa"

	// If user's hub timezone is not set, display error messages in log and events log, and set timezone to GMT to avoid errors
    if (!(location.timeZone)) {
        correctedTimezone = TimeZone.getTimeZone("GMT")
        log.error "${device.displayName}: Time Zone not set, so GMT was used. Please set up your location in the SmartThings mobile app."
        sendEvent(name: "error", value: "", descriptionText: "ERROR: Time Zone not set, so GMT was used. Please set up your location in the SmartThings mobile app.")
    } 
    else {
        correctedTimezone = location.timeZone
    }
    if (dateformat == "US" || dateformat == "" || dateformat == null) {
        if (batteryReset)
            return new Date().format("MMM dd yyyy", correctedTimezone)
        else
            return new Date().format("EEE MMM dd yyyy ${timeString}", correctedTimezone)
    }
    else if (dateformat == "UK") {
        if (batteryReset)
            return new Date().format("dd MMM yyyy", correctedTimezone)
        else
            return new Date().format("EEE dd MMM yyyy ${timeString}", correctedTimezone)
        }
    else {
        if (batteryReset)
            return new Date().format("yyyy MMM dd", correctedTimezone)
        else
            return new Date().format("EEE yyyy MMM dd ${timeString}", correctedTimezone)
    }
}

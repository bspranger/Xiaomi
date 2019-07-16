/**
 *  Xiaomi Aqara Vibration Sensor
 *  Model DJT11LM
 *  Version 0.91b
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
 *  Pairing Xiaomi sensors can be difficult as they were not designed to use with a SmartThings hub.
 *
 */

metadata {
	definition (name: "Xiaomi Aqara Vibration Sensor", namespace: "bspranger", author: "bspranger", mnmn: "Xiaomi", ocfDeviceType: "x.com.st.d.sensor.multifunction") {
		capability "Acceleration Sensor"
		capability "Battery"
		capability "Button"
		capability "Configuration"
		capability "Contact Sensor"
		capability "Health Check"
		capability "Motion Sensor"
		capability "Refresh"
		capability "Sensor"
		capability "Three Axis"

		attribute "accelSensitivity", "String"
		attribute "angleX", "number"
		attribute "angleY", "number"
		attribute "angleZ", "number"
		attribute "batteryRuntime", "String"
		attribute "lastCheckin", "String"
		attribute "lastCheckinCoRE", "Date"
		attribute "lastDrop", "String"
		attribute "lastDropCoRE", "Date"
		attribute "lastStationary", "String"
		attribute "lastStationaryCoRE", "Date"
		attribute "lastTilt", "String"
		attribute "lastTiltCoRE", "Date"
		attribute "lastVibration", "String"
		attribute "lastVibrationCoRE", "Date"
		attribute "tiltAngle", "String"
		attribute "sensorStatus", "enum", ["vibrating", "tilted", "dropped", "Stationary"]
		attribute "activityLevel", "String"

		fingerprint endpointId: "01", profileId: "0104", deviceId: "000A", inClusters: "0000,0003,0019,0101", outClusters: "0000,0004,0003,0005,0019,0101", manufacturer: "LUMI", model: "lumi.vibration.aq1", deviceJoinName: "Xiaomi Aqara Vibration Sensor"

		command "resetBatteryRuntime"
		command "changeSensitivity"
		command "setOpenPosition"
		command "setClosedPosition"
	}

	simulator {
	}

	tiles(scale: 2) {
		// Motion used for sensor vibration/shake events
		multiAttributeTile(name:"sensorStatus", type: "lighting", width: 6, height: 4) {
			tileAttribute("device.sensorStatus", key: "PRIMARY_CONTROL") {
				attributeState "default", label:'Stationary', backgroundColor:"#ffffff"
				attributeState "Vibration", label:'Vibration', backgroundColor:"#00a0dc"
				attributeState "Tilt", label:'Tilt', backgroundColor:"#00a0dc"
				attributeState "Drop", label:'Drop', backgroundColor:"#00a0dc"
				attributeState "Stationary", label:'Stationary', backgroundColor:"#ffffff"
			}
			tileAttribute("device.lastCheckin", key: "SECONDARY_CONTROL") {
				attributeState("lastCheckin", label:'Last Event: ${currentValue}')
			}
		}
		valueTile("battery", "device.battery", decoration: "flat", inactiveLabel: false, width: 2, height: 2) {
			state "battery", label:'${currentValue}%', unit:"%", icon:"https://raw.githubusercontent.com/bspranger/Xiaomi/master/images/XiaomiBattery.png",
			backgroundColors:[
				[value: 10, color: "#bc2323"],
				[value: 26, color: "#f1d801"],
				[value: 51, color: "#44b621"]
			]
		}
		standardTile("contact", "device.contact", decoration: "flat", width: 2, height: 2) {
			state("default", label: 'Unknown', icon: "st.contact.contact.closed", backgroundColor: "#cccccc")
			state("open", label: 'Open', icon: "st.contact.contact.open", backgroundColor: "#e86d13")
			state("closed", label: 'Closed', icon: "st.contact.contact.closed", backgroundColor: "#00a0dc")
			state("unknown", label: 'Unknown', icon: "st.contact.contact.closed", backgroundColor: "#cccccc")
		}
		standardTile("motion", "device.motion", decoration: "flat", width: 2, height: 2) {
			state("active", label: 'Motion', icon:"st.motion.motion.active", backgroundColor:"#00a0dc")
			state("inactive", label: '${name}', icon: "st.motion.motion.inactive", backgroundColor: "#cccccc")
		}
		standardTile("acceleration", "device.acceleration", decoration: "flat", width: 2, height: 2) {
			state("active", label: 'Accel', icon:"st.motion.acceleration.active", backgroundColor:"#00a0dc")
			state("inactive", label: '${name}', icon: "st.motion.acceleration.inactive", backgroundColor: "#cccccc")
		}
		standardTile("setClosedPosition", "device.setClosedPosition", decoration: "flat", width: 2, height: 2) {
			state "default", action:"setClosedPosition", icon: "st.contact.contact.closed", label:'Set Close'
		}
		standardTile("setOpenPosition", "device.setOpenPosition", decoration: "flat", width: 2, height: 2) {
			state "default", action:"setOpenPosition", icon: "st.contact.contact.open", label:'Set Open'
		}
		valueTile("accelSensitivity", "device.accelSensitivity", decoration: "flat", width: 2, height: 2) {
			state "default", action: "changeSensitivity", label:'Sensitivity\nLevel:\n${currentValue}'
		}
		valueTile("activityLevel", "device.activityLevel", decoration: "flat", width: 2, height: 2) {
			state "default", label:'Recent Activity Level:\n${currentValue}'
		}
		valueTile("tiltAngle", "device.tiltAngle", decoration: "flat", width: 2, height: 2) {
			state "default", label:'Last Angle Change:\n${currentValue}°'
		}
		valueTile("threeAxis", "device.threeAxis", decoration: "flat", width: 4, height: 1) {
			state "default", label:'3-Axis Angles: ${currentValue}'
		}
		valueTile("batteryRuntime", "device.batteryRuntime", decoration: "flat", width: 4, height: 1) {
			state "batteryRuntime", label:'Battery Changed: ${currentValue}'
		}
		standardTile("refresh", "device.refresh", decoration: "flat", width: 2, height: 2) {
			state "default", action:"refresh.refresh", icon: "st.secondary.refresh"
		}
		valueTile("spacer", "spacer", decoration: "flat", width: 1, height: 1) {
			state "default", label:''
		}
		valueTile("spacerlg", "spacerlg", decoration: "flat", width: 2, height: 2) {
			state "default", label:''
		}

	main (["sensorStatus"])
	details(["sensorStatus","motion","contact","acceleration","setClosedPosition","accelSensitivity","setOpenPosition","tiltAngle","battery","activityLevel","threeAxis","batteryRuntime","refresh"])
	}

	preferences {
		//Reset to No Motion Config
		input description: "This setting changes how long MOTION ACTIVE is reported in SmartThings when the sensor detects vibration/shock. NOTE: The hardware waits about 60 seconds between vibration/shock detections.", type: "paragraph", element: "paragraph", title: "MOTION RESET"
		input "motionreset", "number", title: "", description: "Number of seconds (default = 65)", range: "1..7200"
		// Date & Time Config
		input description: "", type: "paragraph", element: "paragraph", title: "DATE & CLOCK"
		input name: "dateformat", type: "enum", title: "Set Date Format\nUS (MDY) - UK (DMY) - Other (YMD)", description: "Date Format", options:["US","UK","Other"]
		input name: "clockformat", type: "bool", title: "Use 24 hour clock?"
		// Battery Reset Config
		input description: "If you have installed a new battery, the toggle below will reset the Changed Battery date to help remember when it was changed.", type: "paragraph", element: "paragraph", title: "CHANGED BATTERY DATE RESET"
		input name: "battReset", type: "bool", title: "Battery Changed?"
		// Advanced Settings
		input description: "Only change the settings below if you know what you're doing.", type: "paragraph", element: "paragraph", title: "ADVANCED SETTINGS"
		// Battery Voltage Range
		input description: "", type: "paragraph", element: "paragraph", title: "BATTERY VOLTAGE RANGE"
		input name: "voltsmax", type: "decimal", title: "Max Volts\nA battery is at 100% at __ volts\nRange 2.8 to 3.4", range: "2.8..3.4", defaultValue: 3
		input name: "voltsmin", type: "decimal", title: "Min Volts\nA battery is at 0% (needs replacing) at __ volts\nRange 2.0 to 2.7", range: "2..2.7", defaultValue: 2.5
		// Live Logging Message Display Config
		input description: "These settings affect the display of messages in the Live Logging tab of the SmartThings IDE.", type: "paragraph", element: "paragraph", title: "LIVE LOGGING"
		input name: "infoLogging", type: "bool", title: "Display info log messages?", defaultValue: true
		input name: "debugLogging", type: "bool", title: "Display debug log messages?"
	}
}

// Parse incoming device messages to generate events
def parse(String description) {
	displayDebugLog(": Parsing '${description}'")
	def result = [:]

	// Any report - button press & Battery - results in a lastCheckin event and update to Last Checkin tile
	sendEvent(name: "lastCheckin", value: formatDate(), displayed: false)
	sendEvent(name: "lastCheckinCoRE", value: now(), displayed: false)

	// Send message data to appropriate parsing function based on the type of report
	if (description?.startsWith("read attr - raw: ")) {
		result = parseReadAttrMessage(description)
	} else if (description?.startsWith('catchall:')) {
		result = parseCatchAllMessage(description)
	}
	if (result != [:]) {
		displayDebugLog(": Creating event $result")
		return createEvent(result)
	} else {
		displayDebugLog(": Unable to parse unrecognized message")
		return [:]
	}
}

// Check catchall for battery voltage data to pass to getBatteryResult for conversion to percentage report
private Map parseCatchAllMessage(String description) {
	Map resultMap = [:]
	def catchall = zigbee.parse(description)
	displayDebugLog(": $catchall")

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

// Parse read attr - raw messages (includes all sensor event messages and reset button press, and )
private Map parseReadAttrMessage(String description) {
	def cluster = description.split(",").find {it.split(":")[0].trim() == "cluster"}?.split(":")[1].trim()
	def attrId = description.split(",").find {it.split(":")[0].trim() == "attrId"}?.split(":")[1].trim()
	def value = description.split(",").find {it.split(":")[0].trim() == "value"}?.split(":")[1].trim()
	def eventType
	Map resultMap = [:]

	if (cluster == "0101") {
		// Handles vibration (value 01), tilt (value 02), and drop (value 03) event messages
		if (attrId == "0055") {
			if (value?.endsWith('0002')) {
				eventType = 2
				parseTiltAngle(value[0..3])
			} else {
				eventType = Integer.parseInt(value,16)
			}
			resultMap = mapSensorEvent(eventType)
		}
		// Handles XYZ Accelerometer values
		else if (attrId == "0508") {
			short x = (short)Integer.parseInt(value[8..11],16)
			short y = (short)Integer.parseInt(value[4..7],16)
			short z = (short)Integer.parseInt(value[0..3],16)
			float Psi = Math.round(Math.atan(x/Math.sqrt(z*z+y*y))*1800/Math.PI)/10
			float Phi = Math.round(Math.atan(y/Math.sqrt(x*x+z*z))*1800/Math.PI)/10
			float Theta = Math.round(Math.atan(z/Math.sqrt(x*x+y*y))*1800/Math.PI)/10
			def descText = ": Calculated angles are Psi = ${Psi}°, Phi = ${Phi}°, Theta = ${Theta}° "
			displayDebugLog(": Raw accelerometer XYZ axis values = $x, $y, $z")
			displayDebugLog(descText)
			sendEvent(name: "angleX", value: Psi, displayed: false)
			sendEvent(name: "angleY", value: Phi, displayed: false)
			sendEvent(name: "angleZ", value: Theta, displayed: false)
			resultMap = [
				name: 'threeAxis',
				value: [Psi, Phi, Theta],
				linkText: getLinkText(device),
				isStateChange: true,
				descriptionText: "$device.displayName$descText",
			]
			if (!state.closedX || !state.openX)
				displayInfoLog(": Open/Closed position is unknown because Open and/or Closed positions have not been set")
			else {
				def float cX = Float.parseFloat(state.closedX)
				def float cY = Float.parseFloat(state.closedY)
				def float cZ = Float.parseFloat(state.closedZ)
				def float oX = Float.parseFloat(state.openX)
				def float oY = Float.parseFloat(state.openY)
				def float oZ = Float.parseFloat(state.openZ)
				def float e = 10.0 // Sets range for margin of error
				def ocPosition = "unknown"
				if ((Psi < cX + e) && (Psi > cX - e) && (Phi < cY + e) && (Phi > cY - e) && (Theta < cZ + e) && (Theta > cZ - e))
					ocPosition = "closed"
				else if ((Psi < oX + e) && (Psi > oX - e) && (Phi < oY + e) && (Phi > oY - e) && (Theta < oZ + e) && (Theta > oZ - e))
					ocPosition = "open"
				else
					displayDebugLog(": The current calculated angle position does not match either stored open/closed positions")
				sendpositionEvent(ocPosition)
			}
		}
		// Handles Recent Activity level value messages
		else if (attrId == "0505") {
			def level = Integer.parseInt(value[0..3],16)
			def descText = ": Recent activity level reported at $level"
			displayInfoLog(descText)
			resultMap = [
				name: 'activityLevel',
				value: level,
				descriptionText: "$device.displayName$descText",
			]
		}
	}
	else if (cluster == "0000" && attrId == "0005")	{
		displayInfoLog(": reset button short press detected")
		def modelName = ""
		// Parsing the model
		for (int i = 0; i < value.length(); i+=2) {
			def str = value.substring(i, i+2);
			def NextChar = (char)Integer.parseInt(str, 16);
				modelName = modelName + NextChar
		}
		displayDebugLog(" reported model name:${modelName}")
	}
	return resultMap
}

// Create map of values to be used for vibration, tilt, or drop event
private Map mapSensorEvent(value) {
	def seconds = (value == 1 || value == 4) ? (motionreset ? motionreset : 65) : 2
	def time = new Date(now() + (seconds * 1000))
	def statusType = ["Stationary", "Vibration", "Tilt", "Drop", "", ""]
	def eventName = ["", "motion", "acceleration", "button", "motion", "acceleration"]
	def eventType = ["", "active", "active", "pushed", "inactive", "inactive"]
	def eventMessage = [" is stationary", " was vibrating or moving (Motion active)", " was tilted (Acceleration active)", " was dropped (Button pushed)", ": Motion reset to inactive after $seconds seconds", ": Acceleration reset to inactive"]
	if (value < 4) {
		sendEvent(name: "sensorStatus", value: statusType[value], descriptionText: "$device.displayName${eventMessage[value]}", isStateChange: true, displayed: (value == 0) ? true : false)
		updateLastMovementEvent(statusType[value])
	}
	displayInfoLog("${eventMessage[value]}")
	if (value == 0)
		return
	else if (value == 1) {
		runOnce(time, clearmotionEvent)
		state.motionactive = 1
	}
	else if (value == 2)
		runOnce(time, clearaccelEvent)
	else if (value == 3)
		runOnce(time, cleardropEvent)
	return [
		name: eventName[value],
		value: eventType[value],
		descriptionText: "$device.displayName${eventMessage[value]}",
		isStateChange: true,
		displayed: true
	]
}

// Handles tilt angle change message and posts event to update UI tile display
private parseTiltAngle(value) {
	def angle = Integer.parseInt(value,16)
	def descText = ": tilt angle changed by $angle°"
	sendEvent(
		name: 'tiltAngle',
		value: angle,
		// unit: "°",  // Need to check whether this works or is needed at all
		descriptionText : "$device.displayName$descText",
		isStateChange:true,
		displayed: true
	)
	displayInfoLog(descText)
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

		def descText = ": Battery at ${roundedPct}% (${rawVolts} Volts)"
		def result = [
				name: 'battery',
				value: roundedPct,
				unit: "%",
				isStateChange:true,
				descriptionText : "$device.displayName$descText"
		]

		displayInfoLog(descText)
		return result
}

//Reset the date displayed in Battery Changed tile to current date
def resetBatteryRuntime(paired) {
	def newlyPaired = paired ? " for newly paired sensor" : ""
	sendEvent(name: "batteryRuntime", value: formatDate(true))
	displayInfoLog(": Setting Battery Changed to current date${newlyPaired}")
}

// installed() runs just after a sensor is paired using the "Add a Thing" method in the SmartThings mobile app
def installed() {
	state.prefsSetCount = 0
	displayInfoLog(": Installing")
	init(0)
	checkIntervalEvent("")
}

// configure() runs after installed() when a sensor is paired
def configure() {
	displayInfoLog(": Configuring")
	mapSensorEvent(0)
	refresh()
	init(1)
	checkIntervalEvent("configured")
	return
}

// updated() will run twice every time user presses save in preference settings page
def updated() {
	displayInfoLog(": Updating preference settings")
	if (!state.prefsSetCount)
		state.prefsSetCount = 1
	else if (state.prefsSetCount < 3)
		state.prefsSetCount = state.prefsSetCount + 1
	init(0)
	if (battReset) {
		resetBatteryRuntime()
		device.updateSetting("battReset", false)
	}
	refresh()
	checkIntervalEvent("preferences updated")
	displayInfoLog(": Info message logging enabled")
	displayDebugLog(": Debug message logging enabled")
}

def init(displayLog) {
	if (!device.currentState('batteryRuntime')?.value)
		resetBatteryRuntime(true)
	sendEvent(name: "numberOfButtons", value: 1, displayed: false)
}

// update lastStationary, lastVibration, lastTilt, or lastDrop to current date/time
def updateLastMovementEvent(pressType) {
	displayDebugLog(": Setting Last $pressType to current date/time")
	sendEvent(name: "last${pressType}", value: formatDate(), displayed: false)
	sendEvent(name: "last${pressType}CoRE", value: now(), displayed: false)
}

def clearmotionEvent() {
	def result = [:]
	if (device.currentState('sensorStatus')?.value == "Vibration")
		mapSensorEvent(0)
	result = mapSensorEvent(4)
	state.motionactive = 0
	displayDebugLog(": Sending event $result")
	sendEvent(result)
}

def clearaccelEvent() {
	def result = [:]
	if (device.currentState('sensorStatus')?.value == "Tilt") {
		if (state.motionactive == 1)
			sendEvent(name: "sensorStatus", value: "Vibration", displayed: false)
		else
			mapSensorEvent(0)
	}
	result = mapSensorEvent(5)
	displayDebugLog(": Sending event $result")
	sendEvent(result)
}

def cleardropEvent() {
	if (device.currentState('sensorStatus')?.value == "Drop") {
		if (state.motionactive == 1)
			sendEvent(name: "sensorStatus", value: "Vibration", displayed: false)
		else
			mapSensorEvent(0)
	}
}

private checkIntervalEvent(text) {
	// Device wakes up every 1 hours, this interval allows us to miss one wakeup notification before marking offline
	if (text)
		displayInfoLog(": Set health checkInterval when ${text}")
	sendEvent(name: "checkInterval", value: 3 * 60 * 60 + 2 * 60, displayed: false, data: [protocol: "zigbee", hubHardwareId: device.hub.hardwareID])
}

def setClosedPosition() {
	if (device.currentValue('angleX')) {
		state.closedX = device.currentState('angleX').value
		state.closedY = device.currentState('angleY').value
		state.closedZ = device.currentState('angleZ').value
		sendpositionEvent("closed")
		displayInfoLog(": Closed position successfully set")
		displayDebugLog(": Closed position set to $state.closedX°, $state.closedY°, $state.closedZ°")
	}
	else
		displayDebugLog(": Closed position NOT set because no 3-axis accelerometer reports have been received yet")
}

def setOpenPosition() {
	if (device.currentValue('angleX')) {
		state.openX = device.currentState('angleX').value
		state.openY = device.currentState('angleY').value
		state.openZ = device.currentState('angleZ').value
		sendpositionEvent("open")
		displayInfoLog(": Open position successfully set")
		displayDebugLog(": Open position set to $state.openX°, $state.openY°, $state.openZ°")
	}
	else
		displayDebugLog(": Open position NOT set because no 3-axis accelerometer reports have been received yet")
}

def sendpositionEvent(String ocPosition) {
	def descText = ": Calculated position is $ocPosition"
	displayInfoLog(descText)
	sendEvent(
		name: "contact",
		value: ocPosition,
		isStateChange: true,
		descriptionText: "$device.displayName$descText")
}

def changeSensitivity() {
	state.sensitivity = (state.sensitivity < 3) ? state.sensitivity + 1 : 1
	def attrValue = [0, 0x15, 0x0B, 0x01]
	def levelText = ["", "Low", "Medium", "High"]
	def descText = ": Sensitivity level set to ${levelText[state.sensitivity]}"
	zigbee.writeAttribute(0x0000, 0xFF0D, 0x20, attrValue[state.sensitivity], [mfgCode: 0x115F])
	zigbee.readAttribute(0x0000, 0xFF0D, [mfgCode: 0x115F])
/**  ALTERNATE METHOD FOR WRITE & READ ATTRUBUTE COMMANDS
	def cmds = zigbee.writeAttribute(0x0000, 0xFF0D, 0x20, attrValue[level], [mfgCode: 0x115F]) + zigbee.readAttribute(0x0000, 0xFF0D, [mfgCode: 0x115F])
	for (String cmdString : commands) {
		sendHubCommand([cmdString].collect {new physicalgraph.device.HubAction(it)}, 0)
	}
*/
	sendEvent(name: "accelSensitivity", value: levelText[state.sensitivity], isStateChange: true, descriptionText: descText)
	displayInfoLog(descText)
}

def displayDebugLog(String message) {
	if (debugLogging)
		log.debug "$device.displayName$message"
}

def displayInfoLog(String message) {
	if (infoLogging || state.prefsSetCount < 3)
		log.info "$device.displayName$message"
}

def refresh() {
	displayInfoLog(": Refreshing UI display")
	if (!state.sensitivity) {
		state.sensitivity = 0
		changeSensitivity()
	}
	if (device.currentValue('tiltAngle') == null)
		sendEvent(name: 'tiltAngle', value: "--", isStateChange: true, displayed: false)
	if (device.currentValue('activityLevel') == null)
		sendEvent(name: 'activityLevel', value: "--", isStateChange: true, displayed: false)
	zigbee.readAttribute(0x0000, 0xFF0D, [mfgCode: 0x115F])
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

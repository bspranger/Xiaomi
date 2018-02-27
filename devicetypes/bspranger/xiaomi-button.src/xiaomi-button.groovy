/**
 *	Xiaomi Zigbee Button
 *	Version 1.1b
 *
 *
 *	Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 *	in compliance with the License. You may obtain a copy of the License at:
 *
 *			http://www.apache.org/licenses/LICENSE-2.0
 *
 *	Unless required by applicable law or agreed to in writing, software distributed under the License is distributed
 *	on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License
 *	for the specific language governing permissions and limitations under the License.
 *
 *	Original device handler code by a4refillpad, adapted for use with Aqara model by bspranger
 *	Additional contributions to code by alecm, alixjg, bspranger, gn0st1c, foz333, jmagnuson, rinkek, ronvandegraaf, snalee, tmleafs, twonk, & veeceeoh
 *
 *	Known issues:
 *	Xiaomi sensors do not seem to respond to refresh requests
 *	Inconsistent rendering of user interface text/graphics between iOS and Android devices - This is due to SmartThings, not this device handler
 *	Pairing Xiaomi sensors can be difficult as they were not designed to use with a SmartThings hub.
 *
 *
 */

metadata {
		definition (name: "Xiaomi Button", namespace: "bspranger", author: "bspranger") {
				capability "Battery"
				capability "Sensor"
				capability "Button"
				capability "Actuator"
				capability "Momentary"
				capability "Configuration"
				capability "Health Check"

				attribute "lastCheckin", "string"
				attribute "lastCheckinDate", "Date"
				attribute "lastpressed", "string"
				attribute "lastpressedDate", "string"
				attribute "batteryRuntime", "string"

				fingerprint endpointId: "01", profileId: "0104", deviceId: "0104", inClusters: "0000,0003,FFFF,0019", outClusters: "0000,0004,0003,0006,0008,0005,0019", manufacturer: "LUMI", model: "lumi.sensor_switch", deviceJoinName: "Original Xiaomi Button"

				command "resetBatteryRuntime"
}

		simulator {
					status "Pressed": "on/off: 0"
					status "Released": "on/off: 1"
		}

		tiles(scale: 2) {

				multiAttributeTile(name:"button", type: "lighting", width: 6, height: 4, canChangeIcon: true) {
			tileAttribute ("device.button", key: "PRIMARY_CONTROL") {
									 attributeState("pushed", label:'Push', action: "momentary.push", backgroundColor:"#00a0dc")
								attributeState("released", label:'Push', action: "momentary.push", backgroundColor:"#ffffff", nextState: "pushed")
						 }
						tileAttribute("device.lastpressed", key: "SECONDARY_CONTROL") {
								attributeState "default", label:'Last Pressed: ${currentValue}'
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
				valueTile("lastcheckin", "device.lastCheckin", decoration: "flat", inactiveLabel: false, width: 4, height: 1) {
						state "default", label:'Last Event:\n${currentValue}'
				}
	valueTile("batteryRuntime", "device.batteryRuntime", inactiveLabel: false, decoration: "flat", width: 4, height: 1) {
			state "batteryRuntime", label:'Battery Changed: ${currentValue}'
	}
				main (["button"])
				details(["button","battery","lastcheckin","batteryRuntime"])
	 }
	 preferences {
		//Button Config
		input name: "PressType", type: "enum", options: ["Momentary", "Toggle"], title: "Momentary or Toggle mode? ", defaultValue: "Momentary"
		input "waittoHeld", "number", title: "If the button is held, wait how many seconds until sending a 'held' message?", description: "Enter number of seconds (default = 3)"
		//Date & Time Config
		input description: "", type: "paragraph", element: "paragraph", title: "DATE & CLOCK"
		input name: "dateformat", type: "enum", title: "Set Date Format\n US (MDY) - UK (DMY) - Other (YMD)", description: "Date Format", options:["US","UK","Other"]
		input name: "clockformat", type: "bool", title: "Use 24 hour clock?"
		//Battery Reset Config
		input description: "If you have installed a new battery, the toggle below will reset the Changed Battery date to help remember when it was changed.", type: "paragraph", element: "paragraph", title: "CHANGED BATTERY DATE RESET"
		input name: "battReset", type: "bool", title: "Battery Changed?"
		//Battery Voltage Offset
		input description: "Only change the settings below if you know what you're doing.", type: "paragraph", element: "paragraph", title: "ADVANCED SETTINGS"
		input name: "voltsmax", title: "Max Volts\nA battery is at 100% at __ volts\nRange 2.8 to 3.4", type: "decimal", range: "2.8..3.4", defaultValue: 3, required: false
		input name: "voltsmin", title: "Min Volts\nA battery is at 0% (needs replacing) at __ volts\nRange 2.0 to 2.7", type: "decimal", range: "2..2.7", defaultValue: 2.5, required: false
		}
}

//adds functionality to press the centre tile as a virtualApp Button
def push() {
	log.debug "Virtual App Button Pressed"
	def now = formatDate()
	def nowDate = new Date(now).getTime()
	sendEvent(name: "lastpressed", value: now, displayed: false)
	sendEvent(name: "lastpressedDate", value: nowDate, displayed: false)
	sendEvent(name: "button", value: "pushed", data: [buttonNumber: 1], descriptionText: "$device.displayName app button was pushed", isStateChange: true)
	sendEvent(name: "button", value: "released", data: [buttonNumber: 1], descriptionText: "$device.displayName app button was released", isStateChange: true)
}

// Parse incoming device messages to generate events
def parse(String description) {
	log.debug "${device.displayName}: Parsing '${description}'"
	def results

	// Determine current time and date in the user-selected date format and clock style
		def now = formatDate()
		def nowDate = new Date(now).getTime()
	// Any report - button press & Battery - results in a lastCheckin event and update to Last Checkin tile
	// However, only a non-parseable report results in lastCheckin being displayed in events log
		sendEvent(name: "lastCheckin", value: now, displayed: false)
		sendEvent(name: "lastCheckinDate", value: nowDate, displayed: false)

		Map map = [:]

	// Send message data to appropriate parsing function based on the type of report
		if (description?.startsWith('on/off: ')) {
				map = parseButtonMessage(description)
		//update lastpressed if button is pressed
		if (description == "on/off: 0") {
			sendEvent(name: "lastpressed", value: now, displayed: false)
			sendEvent(name: "lastpressedDate", value: now, displayed: false)
		}
		} else if (description?.startsWith('catchall:')) {
				map = parseCatchAllMessage(description)
		} else if (description?.startsWith("read attr - raw: ")) {
				map = parseReadAttrMessage(description)
		}
		log.debug "${device.displayName}: Parse returned $map"
		results = map ? createEvent(map) : null
		return results
}

private parseButtonMessage(description) {
	def result = [:]
	def onOff = (description - "on/off: ")

	// in toggle mode only toggle when button is pressed
	if (PressType == "Toggle") {
		if (onOff == '0') {
			if (state.button != "pushed")
				result = getContactResult("pushed")
			else
				result = getContactResult("released")
		}
	}
	// momentary mode
	else {
		// when button is pressed start runIn countdown to set held state
		if (onOff == '0') {
			state.countdownActive = true
			runIn((waittoHeld ?: 3), heldState)
		}
		// when button is released if countdown is not finished set pushed state
		else if (state.countdownActive == true) {
			state.countdownActive = false
			result = getContactResult("pushed")
			//sendEvent(result)
		}
	}
	return result
}

private getContactResult(value) {
	log.debug "BUTTON TEST: Creating map to change to ${value} state"
		return [
				name: 'button',
				value: value,
				data: [buttonNumber: "1"],
				descriptionText: "${device.displayName} was ${value}",
				isStateChange: true
		]
}

//if button has not been released (countdownActive = true) then set held state
def heldState() {
	Map map = [:]
	if (state.countdownActive == true) {
		state.countdownActive = false
		map = getContactResult("held")
		sendEvent(map)
	}
}


private Map parseReadAttrMessage(String description) {
	def buttonRaw = (description - "read attr - raw:")
	Map resultMap = [:]

	def cluster = description.split(",").find {it.split(":")[0].trim() == "cluster"}?.split(":")[1].trim()
	def attrId = description.split(",").find {it.split(":")[0].trim() == "attrId"}?.split(":")[1].trim()
	def value = description.split(",").find {it.split(":")[0].trim() == "value"}?.split(":")[1].trim()
	def model = value.split("01FF")[0]
	def data = value.split("01FF")[1]
	log.debug "cluster: ${cluster}, attrId: ${attrId}, value: ${value}, model:${model}, data:${data}"

	if (data[4..7] == "0121") {
		def BatteryVoltage = (Integer.parseInt((data[10..11] + data[8..9]),16))
			resultMap = getBatteryResult(BatteryVoltage)
			log.debug "${device.displayName}: Parse returned $resultMap"
			createEvent(resultMap)
	}

		if (cluster == "0000" && attrId == "0005")	{
				resultMap.name = 'Model'
				resultMap.value = ""
				resultMap.descriptionText = "device model"
				// Parsing the model
				for (int i = 0; i < model.length(); i+=2)
				{
						def str = model.substring(i, i+2);
						def NextChar = (char)Integer.parseInt(str, 16);
						resultMap.value = resultMap.value + NextChar
				}
				return resultMap
		}

		return [:]
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
	def minVolts = voltsmin ? voltsmin : 2.5
	def maxVolts = voltsmax ? voltsmax : 3.0
	def pct = (rawVolts - minVolts) / (maxVolts - minVolts)
	def roundedPct = Math.min(100, Math.round(pct * 100))
	def result = [
		name: 'battery',
		value: roundedPct,
		unit: "%",
		isStateChange:true,
		descriptionText : "${device.displayName} Battery at ${roundedPct}% (${rawVolts} Volts)"
	]

	log.debug "${device.displayName}: ${result}"
	return createEvent(result)
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
	log.debug "${device.displayName}: Installing"
	state.countdownActive = false
	if (!batteryRuntime)
		resetBatteryRuntime(true)
	checkIntervalEvent("installed")
}

// configure() runs after installed() when a sensor is paired
def configure() {
	log.debug "${device.displayName}: Configuring"
	state.countdownActive = false
	if (!batteryRuntime)
		resetBatteryRuntime(true)
	checkIntervalEvent("configured")
	return
}

// updated() will run twice every time user presses save in preference settings page
def updated() {
	checkIntervalEvent("updated")
	state.countdownActive = false
	if(battReset){
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

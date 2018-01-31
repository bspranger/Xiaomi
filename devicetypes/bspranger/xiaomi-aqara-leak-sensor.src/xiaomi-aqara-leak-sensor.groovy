/**
 *  Xiaomi Aqara Leak Sensor
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
 *  Based on original DH by Eric Maycock 2015 and Rave from Lazcad
 *
 *  Change log:
 *  Added DH Colours
 *  Added 100% battery max
 *  Fixed battery parsing problem
 *  Added lastcheckin attribute and tile
 *  Added extra tile to show when last opened
 *  Colours to confirm to new smartthings standards
 *  Added ability to force override current state to Open or Closed.
 *  Added experimental health check as worked out by rolled54.Why
 *  bspranger - Adding Aqara Support
 *  Rinkelk - added date-attribute support for Webcore
 *  Rinkelk - Changed battery percentage with code from cancrusher
 *  Rinkelk - Changed battery icon according to Mobile785
 *  sulee - Added endpointId copied from GvnCampbell's DH - Detects sensor when adding
 *  sulee - Track battery as average of min and max over time
 *  sulee - Clean up some of the code
 *  bspranger - renamed to bspranger to remove confusion of a4refillpad
 *  veeceeoh - added battery parse on button press
 *  veeceeoh - added wet/dry override capability
 */

preferences {
	input name: "dateformat", type: "enum", title: "Set Date Format\n US (MDY) - UK (DMY) - Other (YMD)", description: "Date Format", required: false, options:["US","UK","Other"]
	input description: "Only change the settings below if you know what you're doing", displayDuringSetup: false, type: "paragraph", element: "paragraph", title: "ADVANCED SETTINGS"
	input name: "voltsmax", title: "Max Volts\nA battery is at 100% at __ volts\nRange 2.8 to 3.4", type: "decimal", range: "2.8..3.4", defaultValue: 3, required: false
	input name: "voltsmin", title: "Min Volts\nA battery is at 0% (needs replacing) at __ volts\nRange 2.0 to 2.7", type: "decimal", range: "2..2.7", defaultValue: 2.5, required: false
}

metadata {
	definition (name: "Xiaomi Aqara Leak Sensor", namespace: "bspranger", author: "bspranger") {
		capability "Configuration"
		capability "Sensor"
		capability "Water Sensor"
		capability "Battery"
		capability "Health Check"

		attribute "lastCheckin", "String"
		attribute "lastWet", "String"
		attribute "lastWetDate", "Date"
		attribute "lastCheckinDate", "Date"
		attribute "batteryRuntime", "String"

		fingerprint endpointId: "01", profileId: "0104", deviceId: "0402", inClusters: "0000,0003,0001", outClusters: "0019", manufacturer: "LUMI", model: "lumi.sensor_wleak.aq1", deviceJoinName: "Xiaomi Leak Sensor"

		command "resetDry"
		command "resetWet"
		command "resetBatteryRuntime"
	}

	simulator {
		status "dry": "on/off: 0"
		status "wet": "on/off: 1"
	}

	tiles(scale: 2) {
		multiAttributeTile(name:"water", type: "generic", width: 6, height: 4) {
			tileAttribute("device.water", key: "PRIMARY_CONTROL") {
				attributeState "dry", label:'Dry', icon:"st.alarm.water.dry", backgroundColor:"#ffffff"
				attributeState "wet", label:'Wet', icon:"st.alarm.water.wet", backgroundColor:"#00a0dc"
			}
			tileAttribute("device.lastWet", key: "SECONDARY_CONTROL") {
				attributeState "default", label:'Last Wet: ${currentValue}'
			}
		}
		valueTile("battery", "device.battery", decoration: "flat", inactiveLabel: false, width: 2, height: 2) {
			state "default", label:'${currentValue}%', unit:"%",
			backgroundColors:[
				[value: 10, color: "#bc2323"],
				[value: 26, color: "#f1d801"],
				[value: 51, color: "#44b621"]
			]
		}
		valueTile("lastcheckin", "device.lastCheckin", decoration: "flat", inactiveLabel: false, width: 4, height: 1) {
			state "default", label:'Last Checkin:\n${currentValue}'
		}
		standardTile("resetWet", "device.resetWet", inactiveLabel: false, decoration: "flat", width: 2, height: 2) {
			state "default", action:"resetWet", label:'Override Wet', icon:"st.alarm.water.wet"
		}
		standardTile("resetDry", "device.resetDry", inactiveLabel: false, decoration: "flat", width: 2, height: 2) {
			state "default", action:"resetDry", label:'Override Dry', icon:"st.alarm.water.dry"
		}
		valueTile("batteryRuntime", "device.batteryRuntime", inactiveLabel: false, decoration: "flat", width: 4, height: 1) {
			state "batteryRuntime", label:'Battery Changed (tap to reset):\n ${currentValue}', unit:"", action:"resetBatteryRuntime"
		}
		standardTile("empty2x2", "null", width: 2, height: 2, decoration: "flat") {
			state "emptySmall", label:'', defaultState: true
		}

		main (["water"])
		details(["water","battery","resetDry","resetWet","lastcheckin","batteryRuntime","empty2x2"])
	}
}

def parse(String description) {
	log.debug "${device.displayName} Description:${description}"

	// send event for heartbeat
	def now = formatDate()
	def nowDate = new Date(now).getTime()
	sendEvent(name: "lastCheckin", value: now)
	sendEvent(name: "lastCheckinDate", value: nowDate, displayed: false)

	Map map = [:]

	if (description?.startsWith('zone status')) {
		map = parseZoneStatusMessage(description)
		if (map.value == "wet") {
			sendEvent(name: "lastWet", value: now, displayed: false)
			sendEvent(name: "lastWetDate", value: nowDate, displayed: false)
		}
	} else if (description?.startsWith('catchall:')) {
		map = parseCatchAllMessage(description)
	} else if (description?.startsWith('read attr - raw:')) {
		map = parseReadAttr(description)
	}

	log.debug "${device.displayName}: Parse returned ${map}"
	def results = map ? createEvent(map) : null
	return results
}

private Map parseZoneStatusMessage(String description) {
	def result = [
		name: 'water',
		value: value,
		descriptionText: 'water contact'
	]
	if (description?.startsWith('zone status')) {
		if (description?.startsWith('zone status 0x0001')) { // detected water
			result.value = "wet"
			result.descriptionText = "${device.displayName} has detected water"
		} else if (description?.startsWith('zone status 0x0000')) { // did not detect water
			result.value = "dry"
			result.descriptionText = "${device.displayName} is dry"
		}
		return result
	}
	return [:]
}

private Map getBatteryResult(rawValue) {
	def rawVolts = rawValue / 1000
	def minVolts
	def maxVolts

	if(voltsmin == null || voltsmin == "")
	minVolts = 2.5
	else
	minVolts = voltsmin

	if(voltsmax == null || voltsmax == "")
	maxVolts = 3.0
	else
	maxVolts = voltsmax

	def pct = (rawVolts - minVolts) / (maxVolts - minVolts)
	def roundedPct = Math.min(100, Math.round(pct * 100))

	def result = [
		name: 'battery',
		value: roundedPct,
		unit: "%",
		isStateChange:true,
		descriptionText : "${device.displayName} raw battery is ${rawVolts}v"
	]

	log.debug "${device.displayName}: ${result}"
	return result
}

private Map parseCatchAllMessage(String description) {
	Map resultMap = [:]
	def i
	def cluster = zigbee.parse(description)
	log.debug "${device.displayName}: Parsing CatchAll: '${cluster}'"

	if (cluster) {
		switch(cluster.clusterId) {
			case 0x0000:
				def MsgLength = cluster.data.size();
				for (i = 0; i < (MsgLength-3); i++) {
					if ((cluster.data.get(i) == 0x01) && (cluster.data.get(i+1) == 0x21))  // check the data ID and data type
					{
						// next two bytes are the battery voltage.
						resultMap = getBatteryResult((cluster.data.get(i+3)<<8) + cluster.data.get(i+2))
					}
				}
			break
		}
	}
	return resultMap
}

// Parse raw data on reset button press to retrieve reported battery voltage
private Map parseReadAttr(String description) {
	def buttonRaw = (description - "read attr - raw:")
	Map resultMap = [:]

	def cluster = description.split(",").find {it.split(":")[0].trim() == "cluster"}?.split(":")[1].trim()
	def attrId = description.split(",").find {it.split(":")[0].trim() == "attrId"}?.split(":")[1].trim()
	def value = description.split(",").find {it.split(":")[0].trim() == "value"}?.split(":")[1].trim()
	def model = value.split("01FF")[0]
	def data = value.split("01FF")[1]
	//log.debug "cluster: ${cluster}, attrId: ${attrId}, value: ${value}, model:${model}, data:${data}"

	if (data[4..7] == "0121") {
		def MaxBatteryVoltage = (Integer.parseInt((data[10..11] + data[8..9]),16))/1000
		state.maxBatteryVoltage = MaxBatteryVoltage
	}

	if (cluster == "0000" && attrId == "0005")  {
		resultMap.name = 'Model'
		resultMap.value = ""
		resultMap.descriptionText = "device model"
		// Parsing the model
		for (int i = 0; i < model.length(); i+=2) {
			def str = model.substring(i, i+2);
			def NextChar = (char)Integer.parseInt(str, 16);
			resultMap.value = resultMap.value + NextChar
		}
		return resultMap
	}

	return [:]
}

def resetDry() {
	sendEvent(name:"water", value:"dry")
}

def resetWet() {
	def now = formatDate()
	def nowDate = new Date(now).getTime()
	sendEvent(name:"water", value:"wet")
	sendEvent(name: "lastWet", value: now, displayed: false)
	sendEvent(name: "lastWetDate", value: nowDate, displayed: false)
}

def resetBatteryRuntime() {
	def now = formatDate(true)
	sendEvent(name: "batteryRuntime", value: now)
}

def configure() {
	log.debug "${device.displayName}: configuring"
	state.battery = 0
	checkIntervalEvent("configure");
}

def installed() {
	state.battery = 0
	checkIntervalEvent("installed");
}

def updated() {
	checkIntervalEvent("updated");
}

private checkIntervalEvent(text) {
	// Device wakes up every 1 hours, this interval allows us to miss one wakeup notification before marking offline
	log.debug "${device.displayName}: Configured health checkInterval when ${text}()"
	sendEvent(name: "checkInterval", value: 2 * 60 * 60 + 2 * 60, displayed: false, data: [protocol: "zigbee", hubHardwareId: device.hub.hardwareID])
}

def formatDate(batteryReset) {
	def correctedTimezone = ""

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
			return new Date().format("EEE MMM dd yyyy h:mm:ss a", correctedTimezone)
	}
	else if (dateformat == "UK") {
		if (batteryReset)
			return new Date().format("dd MMM yyyy", correctedTimezone)
		else
			return new Date().format("EEE dd MMM yyyy h:mm:ss a", correctedTimezone)
		}
	else {
		if (batteryReset)
			return new Date().format("yyyy MMM dd", correctedTimezone)
		else
			return new Date().format("EEE yyyy MMM dd h:mm:ss a", correctedTimezone)
	}
}

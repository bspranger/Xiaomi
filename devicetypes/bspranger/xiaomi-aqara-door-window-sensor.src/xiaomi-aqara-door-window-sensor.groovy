/**
 *  Xiaomi Aqara Door/Window Sensor
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
 *  veeceoh - added new refresh & configure code, fixed open/close override code
 */

preferences {
	input name: "dateformat", type: "enum", title: "Set Date Format\n US (MDY) - UK (DMY) - Other (YMD)", description: "Date Format", required: false, options:["US","UK","Other"]
	input description: "Only change the settings below if you know what you're doing", displayDuringSetup: false, type: "paragraph", element: "paragraph", title: "ADVANCED SETTINGS"
	input name: "voltsmax", title: "Max Volts\nA battery is at 100% at __ volts\nRange 2.8 to 3.4", type: "decimal", range: "2.8..3.4", defaultValue: 3, required: false
	input name: "voltsmin", title: "Min Volts\nA battery is at 0% (needs replacing) at __ volts\nRange 2.0 to 2.7", type: "decimal", range: "2..2.7", defaultValue: 2.5, required: false
}

metadata {
	definition (name: "Xiaomi Aqara Door/Window Sensor", namespace: "bspranger", author: "bspranger") {
		capability "Configuration"
		capability "Sensor"
		capability "Contact Sensor"
		capability "Battery"
		capability "Health Check"

		attribute "lastCheckin", "String"
		attribute "lastOpened", "String"
		attribute "lastOpenedDate", "Date"
		attribute "lastCheckinDate", "Date"
		attribute "batteryRuntime", "String"

		fingerprint endpointId: "01", profileId: "0104", deviceId: "5F01", inClusters: "0000,0003,FFFF,0006", outClusters: "0000,0004,FFFF", manufacturer: "LUMI", model: "lumi.sensor_magnet.aq2", deviceJoinName: "Xiaomi Aqara Door Sensor"

		command "resetBatteryRuntime"
		command "resetClosed"
		command "resetOpen"
	}

	simulator {
		status "closed": "on/off: 0"
		status "open": "on/off: 1"
	}

	tiles(scale: 2) {
		multiAttributeTile(name:"contact", type: "generic", width: 6, height: 4) {
			tileAttribute("device.contact", key: "PRIMARY_CONTROL") {
				attributeState "open", label:'${name}', icon:"st.contact.contact.open", backgroundColor:"#e86d13"
				attributeState "closed", label:'${name}', icon:"st.contact.contact.closed", backgroundColor:"#00a0dc"
			}
			tileAttribute("device.lastOpened", key: "SECONDARY_CONTROL") {
				attributeState("default", label:'Last Opened: ${currentValue}')
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
		standardTile("resetClosed", "device.resetClosed", inactiveLabel: false, decoration: "flat", width: 2, height: 2) {
			state "default", action:"resetClosed", label:'Override Close', icon:"st.contact.contact.closed"
		}
		standardTile("resetOpen", "device.resetOpen", inactiveLabel: false, decoration: "flat", width: 2, height: 2) {
			state "default", action:"resetOpen", label:'Override Open', icon:"st.contact.contact.open"
		}
		valueTile("batteryRuntime", "device.batteryRuntime", inactiveLabel: false, decoration: "flat", width: 4, height: 1) {
			state "batteryRuntime", label:'Battery Changed (tap to reset):\n ${currentValue}', unit:"", action:"resetBatteryRuntime"
		}
		standardTile("empty2x2", "null", width: 2, height: 2, decoration: "flat") {
			state "emptySmall", label:'', defaultState: true
		}

		main (["contact"])
		details(["contact","battery","resetClosed","resetOpen","lastcheckin", "batteryRuntime", "empty2x2"])
	}
}

def parse(String description) {
	def result = zigbee.getEvent(description)

	// send event for heartbeat
	def now = formatDate()
	def nowDate = new Date(now).getTime()
	sendEvent(name: "lastCheckin", value: now)
	sendEvent(name: "lastCheckinDate", value: nowDate, displayed: false)

	Map map = [:]

	if (result) {
		log.debug "${device.displayName} Event: ${result}"
		map = getContactResult(result);
		sendEvent(name: "lastOpened", value: now, displayed: false)
		sendEvent(name: "lastOpenedDate", value: nowDate, displayed: false)
	} else if (description?.startsWith('catchall:')) {
		map = parseCatchAllMessage(description)
	} else if (description?.startsWith('read attr - raw:')) {
		map = parseReadAttr(description)
	}

	log.debug "${device.displayName}: Parse returned ${map}"
	def results = map ? createEvent(map) : null
	return results
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
			for (i = 0; i < (MsgLength-3); i++)
			{
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
	log.debug "${device.displayName}: button press detected"
	def buttonRaw = (description - "read attr - raw:")
	Map resultMap = [:]

	def cluster = description.split(",").find {it.split(":")[0].trim() == "cluster"}?.split(":")[1].trim()
	def attrId = description.split(",").find {it.split(":")[0].trim() == "attrId"}?.split(":")[1].trim()
	def value = description.split(",").find {it.split(":")[0].trim() == "value"}?.split(":")[1].trim()
	def model = value.split("01FF")[0]
	def data = value.split("01FF")[1]

	if (cluster == "0000" && attrId == "0005")  {
		def modelName = ""
		// Parsing the model
		for (int i = 0; i < model.length(); i+=2)
		{
			def str = model.substring(i, i+2);
			def NextChar = (char)Integer.parseInt(str, 16);
			modelName = modelName + NextChar
		}
		log.debug "${device.displayName} reported: cluster: ${cluster}, attrId: ${attrId}, value: ${value}, model:${modelName}, data:${data}"
	}
	if (data[4..7] == "0121") {
		resultMap = getBatteryResult(Integer.parseInt((data[10..11] + data[8..9]),16))
	}
	return resultMap
}

private Map getContactResult(result) {
	def value = result.value == "on" ? "open" : "closed"
	def descriptionText = "${device.displayName} was ${value == "open" ? value + "ed" : value}"
	return [
		name: 'contact',
		value: value,
		descriptionText: descriptionText
	]
}

def resetClosed() {
	sendEvent(name:"contact", value:"closed")
}

def resetOpen() {
	def now = formatDate()
	def nowDate = new Date(now).getTime()
	sendEvent(name: "lastOpened", value: now, displayed: false)
	sendEvent(name: "lastOpenedDate", value: nowDate, displayed: false)
	sendEvent(name:"contact", value:"open")
}

def resetBatteryRuntime() {
	def now = formatDate(true)
	sendEvent(name: "batteryRuntime", value: now)
}

def configure() {
	log.debug "${device.displayName}: configuring"
	state.battery = 0
	checkIntervalEvent("configure");
	return
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

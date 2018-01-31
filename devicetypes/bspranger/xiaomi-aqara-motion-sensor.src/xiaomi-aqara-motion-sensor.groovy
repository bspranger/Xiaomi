/**
 *  Xiaomi Aqara Motion Sensor
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
 *  Based on original DH by Eric Maycock 2015
 *
 *  Change log:
 *  modified 29/12/2016 a4refillpad
 *  Added fingerprinting
 *  Added heartbeat/lastcheckin for monitoring
 *  Added battery and refresh
 *  Motion background colours consistent with latest DH
 *  Fixed max battery percentage to be 100%
 *  Added Last update to main tile
 *  Added last motion tile
 *  Heartdeat icon plus improved localisation of date
 *  Removed non working tiles and changed layout and incorporated latest colours
 *  Added experimental health check as worked out by rolled54.Why
 *  bspranger - renamed to bspranger to remove confusion of a4refillpad
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
		attribute "lastMotion", "String"
		attribute "light", "number"
		attribute "batteryRuntime", "String"

		fingerprint profileId: "0104", deviceId: "0104", inClusters: "0000, 0003, FFFF, 0019", outClusters: "0000, 0004, 0003, 0006, 0008, 0005, 0019", manufacturer: "LUMI", model: "lumi.sensor_motion", deviceJoinName: "Xiaomi Motion"
		fingerprint endpointId: "01", profileId: "0104", deviceId: "0107", inClusters: "0000,FFFF,0406,0400,0500,0001,0003", outClusters: "0000,0019", manufacturer: "LUMI", model: "lumi.sensor_motion.aq2", deviceJoinName: "Xiaomi Aqara Motion Sensor"
		fingerprint profileID: "0104", deviceId: "0104", inClusters: "0000, 0400, 0406, FFFF", outClusters: "0000,0019", manufacturer: "LUMI", model: "lumi.sensor_motion", deviceJoinName: "Xiaomi Aqara Motion Sensor"

		command "resetBatteryRuntime"
		command "reset"
		command "Refresh"
	}

	simulator {
	}

	preferences {
		input name: "motionReset", "number", title: "Number of seconds after the last reported activity to report that motion is inactive (in seconds). \n\n(The device will always remain blind to motion for 60seconds following first detected motion. This value just clears the 'active' status after the number of seconds you set here but the device will still remain blind for 60seconds in normal operation.)", description: "", value:120, displayDuringSetup: true
		input name: "dateformat", type: "enum", title: "Set Date Format\n US (MDY) - UK (DMY) - Other (YMD)", description: "Date Format", required: false, options:["US","UK","Other"]
		input description: "Only change the settings below if you know what you're doing", displayDuringSetup: false, type: "paragraph", element: "paragraph", title: "ADVANCED SETTINGS"
		input name: "voltsmax", title: "Max Volts\nA battery is at 100% at __ volts\nRange 2.8 to 3.4", type: "decimal", range: "2.8..3.4", defaultValue: 3, required: false
		input name: "voltsmin", title: "Min Volts\nA battery is at 0% (needs replacing) at __ volts\nRange 2.0 to 2.7", type: "decimal", range: "2..2.7", defaultValue: 2.5, required: false
	}

	tiles(scale: 2) {
		multiAttributeTile(name:"motion", type:"generic", width: 6, height: 4) {
			tileAttribute ("device.motion", key: "PRIMARY_CONTROL") {
				attributeState "active", label:'Motion', icon:"st.motion.motion.active", backgroundColor:"#00a0dc"
				attributeState "inactive", label:'No Motion', icon:"st.motion.motion.inactive", backgroundColor:"#ffffff"
			}
			tileAttribute("device.lastMotion", key: "SECONDARY_CONTROL") {
				attributeState("default", label:'Last Motion: ${currentValue}')
			}
		}
		valueTile("battery", "device.battery", decoration:"flat", inactiveLabel: false, width: 2, height: 2) {
			state "default", label:'${currentValue}%', unit:"%",
			backgroundColors:[
				[value: 10, color: "#bc2323"],
				[value: 26, color: "#f1d801"],
				[value: 51, color: "#44b621"]
			]
		}
		valueTile("illuminance", "device.illuminance", decoration:"flat", inactiveLabel: false, width: 2, height: 2) {
			state "default", label:'${currentValue} lux', unit:"lux"
		}
		standardTile("reset", "device.reset", inactiveLabel: false, decoration:"flat", width: 2, height: 2) {
			state "default", action:"reset", label:'Reset Motion', icon:"st.motion.motion.active"
		}
		valueTile("lastcheckin", "device.lastCheckin", decoration:"flat", inactiveLabel: false, width: 4, height: 1) {
			state "default", label:'Last Checkin:\n ${currentValue}'
		}
		valueTile("batteryRuntime", "device.batteryRuntime", inactiveLabel: false, decoration:"flat", width: 4, height: 1) {
			state "batteryRuntime", label:'Battery Changed (tap to reset):\n ${currentValue}', action:"resetBatteryRuntime"
		}
		standardTile("empty2x2", "null", width: 2, height: 2, decoration: "flat") {
			state "emptySmall", label:'', defaultState: true
		}

		main(["motion"])
		details(["motion", "battery", "illuminance", "reset", "lastcheckin", "batteryRuntime", "empty2x2"])
	}
}

def parse(String description) {
	log.debug "${device.displayName} Parsing: $description"

	//  send event for heartbeat
	def now = formatDate()
	def nowDate = new Date(now).getTime()
	sendEvent(name: "lastCheckin", value: now)
	sendEvent(name: "lastCheckinDate", value: nowDate, displayed: false)

	Map map = [:]
	if (description?.startsWith('catchall:')) {
		map = parseCatchAllMessage(description)
	}
	else if (description?.startsWith('read attr -')) {
		map = parseReportAttributeMessage(description)
	}
	else if (description?.startsWith('illuminance:')) {
		map = parseIlluminanceMessage(description)
	}
	else if (description?.startsWith('enroll request')) {
		List cmds = enrollResponse()
		log.debug "${device.displayName} enroll response: ${cmds}"
		result = cmds?.collect { new physicalgraph.device.HubAction(it) }
	}

	log.debug "${device.displayName} Parse returned: $map"
	def result = map ? createEvent(map) : null

	return result
}

private Map parseIlluminanceMessage(String description) {

	def Lux = ((description - "illuminance: ").trim()) as Float

	def result = [
		name: 'illuminance',
		value: Lux,
		unit: "lux",
		isStateChange:true,
		descriptionText : "${device.displayName} illuminance was ${Lux} lux"
	]
	return result;
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
	def i
	Map resultMap = [:]
	def cluster = zigbee.parse(description)
	log.debug cluster
	if (cluster) {
		switch(cluster.clusterId)
		{
			case 0x0000:
			def MsgLength = cluster.data.size();

			// Original Xiaomi CatchAll does not have identifiers, first UINT16 is Battery
			if ((cluster.data.get(0) == 0x02) && (cluster.data.get(1) == 0xFF))
			{
				for (i = 0; i < (MsgLength-3); i++)
				{
					if (cluster.data.get(i) == 0x21) // check the data ID and data type
					{
						// next two bytes are the battery voltage.
						resultMap = getBatteryResult((cluster.data.get(i+2)<<8) + cluster.data.get(i+1))
						break
					}
				}
			}
			else if ((cluster.data.get(0) == 0x01) && (cluster.data.get(1) == 0xFF))
			{
				for (i = 0; i < (MsgLength-3); i++)
				{
					if ((cluster.data.get(i) == 0x01) && (cluster.data.get(i+1) == 0x21))  // check the data ID and data type
					{
						// next two bytes are the battery voltage.
						resultMap = getBatteryResult((cluster.data.get(i+3)<<8) + cluster.data.get(i+2))
						break
					}
				}
			}
			break
		}
	}
	return resultMap
}

def enrollResponse() {
	log.debug "${device.displayName}: Enrolling device into the IAS Zone"
	[
		// Enrolling device into the IAS Zone
		"raw 0x500 {01 23 00 00 00}", "delay 200",
		"send 0x${device.deviceNetworkId} 1 1"
	]
}

private Map parseReportAttributeMessage(String description) {
	def cluster = description.split(",").find {it.split(":")[0].trim() == "cluster"}?.split(":")[1].trim()
	def attrId = description.split(",").find {it.split(":")[0].trim() == "attrId"}?.split(":")[1].trim()
	def value = description.split(",").find {it.split(":")[0].trim() == "value"}?.split(":")[1].trim()

	Map resultMap = [:]
	def now = new Date().format("EEE MMM dd yyyy h:mm:ss a", location.timeZone)

	if ((cluster == "0406") && (attrId == "0000"))
	{
		def motion = (value == "01") ? "active" : "inactive"
		sendEvent(name: "lastMotion", value: now)
		if (settings.motionReset == null || settings.motionReset == "" ) settings.motionReset = 120
		if (motion == "active") runIn(settings.motionReset, stopMotion)
		resultMap = getMotionResult(motion)
	}
	else if (cluster == "0000" && attrId == "0005")
	{
		def modelName = ""
		// Parsing the model
		for (int i = 0; i < value.length(); i+=2)
		{
			def str = value.substring(i, i+2);
			def NextChar = (char)Integer.parseInt(str, 16);
			modelName = modelName + NextChar
		}
		log.debug "${device.displayName} reported: cluster: ${cluster}, attrId: ${attrId}, value: ${value}, model:${modelName}"
	}
	return resultMap
}

private Map parseIasMessage(String description) {
	List parsedMsg = description.split(' ')
	String msgCode = parsedMsg[2]

	Map resultMap = [:]
	switch(msgCode) {
		case '0x0020': // Closed/No Motion/Dry
		resultMap = getMotionResult('inactive')
		break

		case '0x0021': // Open/Motion/Wet
		resultMap = getMotionResult('active')
		break

		case '0x0022': // Tamper Alarm
		log.debug '${device.displayName}: motion with tamper alarm'
		resultMap = getMotionResult('active')
		break

		case '0x0023': // Battery Alarm
		break

		case '0x0024': // Supervision Report
		log.debug '${device.displayName}: no motion with tamper alarm'
		resultMap = getMotionResult('inactive')
		break

		case '0x0025': // Restore Report
		break

		case '0x0026': // Trouble/Failure
		log.debug '${device.displayName}: motion with failure alarm'
		resultMap = getMotionResult('active')
		break

		case '0x0028': // Test Mode
		break
	}
	return resultMap
}

private Map getMotionResult(value) {
	String descriptionText = value == 'active' ? "${device.displayName} detected motion" : "${device.displayName} motion has stopped"
	def commands = [
		name: 'motion',
		value: value,
		descriptionText: descriptionText
	]
	return commands
}

def stopMotion() {
	sendEvent(name:"motion", value:"inactive")
}

def reset() {
	sendEvent(name:"motion", value:"inactive")
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

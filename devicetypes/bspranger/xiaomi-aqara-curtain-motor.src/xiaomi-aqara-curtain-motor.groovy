/**
 *  Xiaomi Aqara Curtain Motor - Model ZNCLDJ11LM
 *  Device Handler for SmartThings
 *  Version 0.3b
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
 *  Contributions to code from alecm, alixjg, bspranger, gn0st1c, Inpier, foz333, jmagnuson, KennethEvers, rinkek, ronvandegraaf, snalee, tmleaf
 *  Discussion board for this DH: https://community.smartthings.com/t/original-aqara-xiaomi-zigbee-sensors-contact-temp-motion-button-outlet-leak-etc
 *
 *  Useful Links:
 *  Xiaomi website product page... https://xiaomi-mi.com/sockets-and-sensors/xiaomi-aqara-smart-curtain-controller-white/
 *  Manual (translated to English)... http://files.xiaomi-mi.com/files/aqara/Aqara_Smart_Curtain_Controller_EN.pdf
 *  YouTube review... https://www.youtube.com/watch?v=GkZ16IuoT-c
 *
 *  Known issues:
 *	Inconsistent rendering of user interface text/graphics between iOS and Android devices - This is due to SmartThings, not this device handler
 *	Pairing Xiaomi sensors can be difficult as they were not designed to use with a SmartThings hub
 *
 *  Fingerprint Endpoint data:
 *        01 - endpoint id
 *        0260 - profile id
 *        0514 - device id
 *        01 - ignored
 *        ?? - number of in clusters (TO BE DETERMINED)
 *        ???? - inClusters  (TO BE DETERMINED)
 *        ?? - number of out clusters  (TO BE DETERMINED)
 *        ???? - outClusters  (TO BE DETERMINED)
 *        manufacturer "LUMI" - must match manufacturer field in fingerprint
 *        model "lumi.curtain" - must match model in fingerprint
 *
 *  Change Log:
 *	15.05.2018 - veeceeoh - Started work on DTH
 *	18.05.2018 - veeceeoh - Major changes, including changing from Switch capability to Door Control
 */

metadata {
	definition (name: "Xiaomi Aqara Curtain Motor", namespace: "bspranger", author: "veeceeoh") {
		capability "Actuator"
		capability "Configuration"
		capability "Door Control"
		capability "Health Check"
		capability "Refresh"

		attribute "lastCheckinCoRE", "string"

		fingerprint endpointId: "01", profileID: "0260", deviceID: "0514", inClusters: "0000,0001", outClusters: "0019", manufacturer: "LUMI", model: "lumi.curtain", deviceJoinName: "Aqara Curtain Motor"
	}

	// simulator metadata
	simulator {
	}

	tiles(scale: 2) {
		multiAttributeTile(name:"door", type: "toggle", width: 6, height: 4, canChangeIcon: true){
			tileAttribute ("device.door", key: "PRIMARY_CONTROL") {
				// For now, ST's contact sensor open - close icons are used until better ones are found or created
				attributeState "unknown", label:'${name}', action:"door control.open", icon:"st.contact.contact.open", backgroundColor:"#00a0dc"
				attributeState "closed", label:'${name}', action:"door control.open", icon:"st.contact.contact.closed", backgroundColor:"#ffffff", nextState:"opening"
				attributeState "open", label:'${name}', action:"door control.close", icon:"st.contact.contact.open", backgroundColor:"#00a0dc", nextState:"closing"
				attributeState "opening", label:'${name}', icon:"st.contact.contact.closed", backgroundColor:"#00a0dc"
				attributeState "closing", label:'${name}', icon:"st.contact.contact.open", backgroundColor:"#ffffff"
			}
			// Secondary Tile attribute display to be set up in future
			//tileAttribute("device.curtainPosition", key: "SECONDARY_CONTROL") {
				//attributeState("default", label:'Current Position: ${currentValue}')
			//}
		}
		standardTile("refresh", "device.door", inactiveLabel: false, decoration: "flat", width: 2, height: 2) {
			state "default", label:'', action:"refresh.refresh", icon:"st.secondary.refresh"
		}
		standardTile("open", "device.door", inactiveLabel: false, decoration: "flat", width: 2, height: 2) {
			state "default", label:'Open', action:"door control.open", icon:"st.contact.contact.open"
		}
		standardTile("close", "device.door", inactiveLabel: false, decoration: "flat", width: 2, height: 2) {
			state "default", label:'Close', action:"door control.close", icon:"st.contact.contact.closed"
		}
		main (["door"])
		details(["door", "open", "refresh", "close"])
	}

	preferences {
		input description: "These settings affect the display of messages in the Live Logging tab of the SmartThings IDE.", type: "paragraph", element: "paragraph", title: "LIVE LOGGING"
		input name: "infoLogging", type: "bool", title: "Display info log messages?", defaultValue: true
		input name: "debugLogging", type: "bool", title: "Display debug log messages?"
	}
}

// Parse incoming device messages to generate events
def parse(String description) {
	displayDebugLog(": Parsing $description")
	Map result = [:]

	// catchall messages include verification that an on-off command was received
	// and also a regular check-in message
	if (description?.startsWith('catchall:')) {
		result = parseCatchAllMessage(description)
	}
	// The device's read attribute messages seem to all be sent from cluster 000D, attribute 0055,
	// which is supposed to be the present value of analog output, but instead the values received
	// don't seem to follow the correct Zigbee specification and are likely proprietary values.
	// Most of the info log output that needs to be examined will be generated by this function call.
	else if (description?.startsWith('read attr -')) {
		result = parseReportAttributeMessage(description)
	}
	// on-off messages are sent shortly after the curtain motor receives an on-off command
	// and do not indicate whether the motor has actually finished opening or closing
	else if (description?.startsWith('on/off: ')){
		result = parseOpenCloseReport(description - "on/off: ")
	}

	if (result != [:]) {
		displayDebugLog(": Creating event $result")
		return createEvent(result)
	} else
		return [:]
}

private Map parseCatchAllMessage(String description) {
	Map result = [:]
	def catchall = zigbee.parse(description)
	displayDebugLog(": Zigbee Parse: $catchall")

	if (catchall.clusterId == 0x0006 && catchall.command == 0x0B){
		def onoff = catchall.data[0]
		if (onoff == 1) {
			result = [name: "door", value: "opening", descriptionText: "$device.displayName is opening"]
			displayInfoLog(": Received command to open")
		}
		else {
			result = [name: "door", value: "closing", descriptionText: "$device.displayName is closing"]
			displayInfoLog(": Received command to close")
		}
	}
	return result
}

private Map parseReportAttributeMessage(String description) {
	Map descMap = (description - "read attr - ").split(",").inject([:]) { map, param ->
		def nameAndValue = param.split(":")
		map += [(nameAndValue[0].trim()):nameAndValue[1].trim()]
	}
	Map result = [:]

	// Report messages about the state of the motor appear to be sent from Cluster 000D / attrId 0055
	if (descMap.clusterId == "000D" && descMap.attrId == "0055"){
		// Output log messages of the data of interest from the attribute report value received
		displayInfoLog ": Cluster 000D Message: Byte #3 = ${descMap.raw[22..23]} (decimal ${Integer.parseInt(descMap.raw[22..23],16)})"
		displayInfoLog ": Cluster 000D Message: Byte #4 = ${descMap.raw[24..25]}"
		displayInfoLog ": Cluster 000D Message: Last 4 bytes = ${descMap.raw[32..39]}"
		// Handle report messages that the motor has finished opening or closing the curtain
		if (descMap.value?.startsWith('00000000')) {
			def newState = "open"
			if (descMap.value?.endsWith('0000000000')) {
				newState = "closed"
				displayInfoLog "has finished closing"
			}
			else
				displayInfoLog "has finished opening"
			result = [name: "door", value: newState, descriptionText: "$device.displayName is $newState"]
		}
	}
	else
		displayInfoLog ": Unrecognized Read Attribute Message: $descMap"
	return result
}

private Map parseOpenCloseReport(String description) {
	if (description == '0')
		displayInfoLog ": Close command confirmed"
	else
		displayInfoLog ": Open command confirmed"
	runIn(15, motorFinishedCountdown)
	return [:]
}

def close() {
	def currState = device.currentState('door')?.value
	if (currState?.startsWith('clos'))
		displayInfoLog ": Ignoring close request, curtain is already $currentState"
	else {
		displayInfoLog ": Sending close command"
		zigbee.off()
	}
}

def open() {
	def currState = device.currentState('door')?.value
	if (currState?.startsWith('open'))
		displayInfoLog ": Ignoring open request, curtain is already $currentState"
	else {
		displayInfoLog ": Sending open command"
		zigbee.on()
	}
}

def motorFinishedCountdown() {
	def currState = device.currentState('door')?.value
	def newState = "open"
	if (currState?.endsWith('ing')) {
		if (newState == "closing")
			newState = "closed"
		sendEvent(name: "door", value: newState, descriptionText: "$device.displayName is $newState")
		displayInfoLog ": Automatically set state to $newState after 15 seconds"
	}
}

def refresh() {
	displayInfoLog(": Refreshing")
	zigbee.onOffRefresh() + zigbee.onOffConfig()
/**
	// The read attribute commands below use a deprecated method
	[
		"st rattr 0x${device.deviceNetworkId} 1 6 0", "delay 500",
		"st rattr 0x${device.deviceNetworkId} 1 6 0", "delay 250",
		"st rattr 0x${device.deviceNetworkId} 1 2 0", "delay 250",
		"st rattr 0x${device.deviceNetworkId} 1 1 0", "delay 250",
		"st rattr 0x${device.deviceNetworkId} 1 0 0"
	]
**/
}

private def displayDebugLog(message) {
	if (debugLogging)
		log.debug "${device.displayName}${message}"
}

private def displayInfoLog(message) {
	if (infoLogging || state.prefsSetCount < 3)
		log.info "${device.displayName}${message}"
}

// installed() runs just after a device is paired using the "Add a Thing" method in the SmartThings mobile app
def installed() {
	state.prefsSetCount = 0
	displayInfoLog(": Installing")
	checkIntervalEvent("")
}

// configure() runs after installed() when a device is paired
def configure() {
	displayInfoLog(": Configuring")
	refresh()
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
	displayInfoLog(": Info message logging enabled")
	displayDebugLog(": Debug message logging enabled")
	checkIntervalEvent("preferences updated")
}

private checkIntervalEvent(text) {
		// Device wakes up every 50 or 60 minutes, this interval allows us to miss one wakeup notification before marking offline
		if (text)
			displayInfoLog(": Set health checkInterval when ${text}")
		sendEvent(name: "checkInterval", value: 2 * 60 * 60 + 2 * 60, displayed: false, data: [protocol: "zigbee", hubHardwareId: device.hub.hardwareID])
}

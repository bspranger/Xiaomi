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
 *  https://github.com/bspranger/Xiaomi/tree/master/devicetypes/a4refillpad/xiaomi-aqara-door-window-sensor.src
 *  https://github.com/GvnCampbell/SmartThings-Xiaomi
 *
 * Based on original DH by Eric Maycock 2015 and Rave from Lazcad
 *  change log:
 *	added DH Colours
 *  added 100% battery max
 *  fixed battery parsing problem
 *  added lastcheckin attribute and tile
 *  added extra tile to show when last opened
 *  colours to confirm to new smartthings standards
 *  added ability to force override current state to Open or Closed.
 *  added experimental health check as worked out by rolled54.Why
 *  Bspranger - Adding Aqara Support
 *  Rinkelk - added date-attribute support for Webcore
 *  Rinkelk - Changed battery percentage with code from cancrusher
 *  Rinkelk - Changed battery icon according to Mobile785
 *  sulee - Added endpointId copied from GvnCampbell's DH - Detects sensor when adding
 *  sulee - Track battery as average of min and max over time
 *  sulee - Clean up some of the code
 *
 */
metadata {
   definition (name: "Xiaomi Aqara Door/Window Sensor", namespace: "a4refillpad", author: "a4refillpad") {
      capability "Configuration"
      capability "Sensor"
      capability "Contact Sensor"
      capability "Refresh"
      capability "Battery"
      capability "Health Check"

      attribute "lastCheckin", "String"
      attribute "lastOpened", "String"
      attribute "lastOpenedDate", "Date" 
      attribute "lastCheckinDate", "Date"

      fingerprint endpointId: "01", profileId: "0104", deviceId: "5F01", inClusters: "0000,0003,FFFF,0006", outClusters: "0000,0004,FFFF", manufacturer: "LUMI", model: "lumi.sensor_magnet.aq2", deviceJoinName: "Xiaomi Aqara Door Sensor"

      command "Refresh"
   }
    
   simulator {
      status "closed": "on/off: 0"
      status "open": "on/off: 1"
   }
    
   tiles(scale: 2) {
      multiAttributeTile(name:"contact", type: "generic", width: 6, height: 4) {
         tileAttribute ("device.contact", key: "PRIMARY_CONTROL") {
            attributeState "open", label:'${name}', icon:"st.contact.contact.open", backgroundColor:"#e86d13"
            attributeState "closed", label:'${name}', icon:"st.contact.contact.closed", backgroundColor:"#00a0dc"
         }
         tileAttribute("device.lastOpened", key: "SECONDARY_CONTROL") {
            attributeState("default", label:'Last Opened: ${currentValue}')
         }
      }

      valueTile("battery", "device.battery", decoration: "flat", inactiveLabel: false, width: 2, height: 2) {
         state "default", label:'${currentValue}%', unit:"",
         backgroundColors: [
            [value: 10, color: "#bc2323"],
            [value: 26, color: "#f1d801"],
            [value: 51, color: "#44b621"]
         ]
      }
      standardTile("resetClosed", "device.resetClosed", inactiveLabel: false, decoration: "flat", width: 2, height: 2) {
         state "default", action:"resetClosed", label: "Override Close", icon:"st.contact.contact.closed"
      }
      standardTile("resetOpen", "device.resetOpen", inactiveLabel: false, decoration: "flat", width: 2, height: 2) {
         state "default", action:"resetOpen", label: "Override Open", icon:"st.contact.contact.open"
      }
      valueTile("lastCheckin", "device.lastCheckin", decoration: "flat", inactiveLabel: false, width: 5, height: 1) {
         state "default", label:'Last Update: ${currentValue}'
      }
      standardTile("refresh", "device.refresh", inactiveLabel: false, decoration: "flat", width: 1, height: 1) {
         state "default", action:"refresh.refresh", icon:"st.secondary.refresh"
      }
      standardTile("refresh", "command.refresh", inactiveLabel: false) {
         state "default", label:'refresh', action:"refresh.refresh", icon:"st.secondary.refresh-icon"
      }

      main (["contact"])
      details(["contact","battery","resetClosed","resetOpen","lastCheckin","refresh"])
   }
}

def parse(String description) {
   def linkText = getLinkText(device)
   def result = zigbee.getEvent(description)
   
//  send event for heartbeat    
   def now = new Date().format("yyyy MMM dd EEE h:mm:ss a", location.timeZone)
   def nowDate = new Date(now).getTime()
   sendEvent(name: "lastCheckin", value: now)
   sendEvent(name: "lastCheckinDate", value: nowDate) 
    
   Map map = [:]

   if (result) {
   	   log.debug "${linkText} Event: ${result}"
       map = getContactResult(result);
       sendEvent(name: "lastOpened", value: now)
       sendEvent(name: "lastOpenedDate", value: nowDate)
   } else if (description?.startsWith('catchall:')) {
       map = parseCatchAllMessage(description)
   }
   log.debug "${linkText}: Parse returned ${map}"
   def results = map ? createEvent(map) : null

   return results;
}

private Map getBatteryResult(rawValue) {
    def linkText = getLinkText(device)
    def result = [
		name: 'battery',
		value: '--',
        unit: "%",
        translatable: true
    ]
    
    def rawVolts = rawValue / 1000

	def maxBattery = state.maxBattery ?: 0
    def minBattery = state.minBattery ?: 0

	if (maxBattery == 0 || rawVolts > minBattery)
    	state.maxBattery = maxBattery = rawVolts
        
    if (minBattery == 0 || rawVolts < minBattery)
    	state.minBattery = minBattery = rawVolts
    
    def volts = (maxBattery + minBattery) / 2

	def minVolts = 2.7
    def maxVolts = 3.0
    def pct = (volts - minVolts) / (maxVolts - minVolts)
    def roundedPct = Math.round(pct * 100)
    result.value = Math.min(100, roundedPct)
    result.descriptionText = "${linkText}: raw battery is ${rawVolts}v, state: ${volts}v, ${minBattery}v - ${maxBattery}v"
    
    return result
}

private Map parseCatchAllMessage(String description) {
    def linkText = getLinkText(device)
	Map resultMap = [:]
	def cluster = zigbee.parse(description)
	log.debug "${linkText}: Parsing CatchAll: '${cluster}'"
	if (cluster) {
		switch(cluster.clusterId) {
			case 0x0000:
            	if ((cluster.data.get(4) == 1) && (cluster.data.get(5) == 0x21)) // Check CMD and Data Type
            		resultMap = getBatteryResult((cluster.data.get(7)<<8) + cluster.data.get(6))
			break
		}
	}

	return resultMap
}

def configure() {
	def linkText = getLinkText(device)
    log.debug "${linkText}: configuring"
    return zigbee.readAttribute(0x0001, 0x0021) + zigbee.configureReporting(0x0001, 0x0021, 0x20, 600, 21600, 0x01)
}

def refresh() {
	def linkText = getLinkText(device)
    log.debug "${linkText}: refreshing"
    return zigbee.readAttribute(0x0001, 0x0021) + zigbee.configureReporting(0x0001, 0x0021, 0x20, 600, 21600, 0x01)
}

private Map getContactResult(result) {
   def linkText = getLinkText(device)
   def value = result.value == "on" ? "open" : "closed"
   def descriptionText = "${linkText} was ${value == "open" ? value + "ed" : value}"
   return [
      name: 'contact',
      value: value,
      descriptionText: descriptionText
	]
}

def installed() {
	checkIntervalEvent("installed");
}

def updated() {
	checkIntervalEvent("updated");
}

private checkIntervalEvent(text) {
// Device wakes up every 1 hours, this interval allows us to miss one wakeup notification before marking offline
    def linkText = getLinkText(device)
    log.debug "${linkText}: Configured health checkInterval when ${text}()"
	sendEvent(name: "checkInterval", value: 2 * 60 * 60 + 2 * 60, displayed: false, data: [protocol: "zigbee", hubHardwareId: device.hub.hardwareID])
}

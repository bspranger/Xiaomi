/**
 *  Xiaomi Door/Window Sensor
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
 * Based on original DH by Eric Maycock 2015 and Rave from Lazcad
 *  change log:
 *    added DH Colours
 *  added 100% battery max
 *  fixed battery parsing problem
 *  added lastcheckin attribute and tile
 *  added extra tile to show when last opened
 *  colours to confirm to new smartthings standards
 *  added ability to force override current state to Open or Closed.
 *  added experimental health check as worked out by rolled54.Why
 *  Rinkelk - added date-attribute support for Webcore
 *  Rinkelk - Changed battery icon according to Mobile785
 *  bspranger - renamed to bspranger to remove confusion of a4refillpad
 *
 */
metadata {
   definition (name: "Xiaomi Door/Window Sensor", namespace: "bspranger", author: "bspranger") {
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
   attribute "batteryRuntime", "String"

   fingerprint endpointId: "01", profileId: "0104", deviceId: "0104", inClusters: "0000, 0003, FFFF, 0019", outClusters: "0000, 0004, 0003, 0006, 0008, 0005 0019", manufacturer: "LUMI", model: "lumi.sensor_magnet", deviceJoinName: "Xiaomi Door Sensor"
   
   command "resetBatteryRuntime"
   command "enrollResponse"
   command "resetClosed"
   command "resetOpen"
   command "Refresh"
   }
    
   simulator {
      status "closed": "on/off: 0"
      status "open": "on/off: 1"
   }
    
   tiles(scale: 2) {
        multiAttributeTile(name:"contact", type: "generic", width: 6, height: 4){
            tileAttribute ("device.contact", key: "PRIMARY_CONTROL") {
                attributeState "open", label:'${name}', icon:"st.contact.contact.open", backgroundColor:"#e86d13"
                attributeState "closed", label:'${name}', icon:"st.contact.contact.closed", backgroundColor:"#00a0dc"
            }
        }
        valueTile("battery", "device.battery", decoration: "flat", inactiveLabel: false, width: 2, height: 2) {
            state "default", label:'${currentValue}%', unit:"",
			backgroundColors:[
				[value: 0, color: "#c0392b"],
				[value: 25, color: "#f1c40f"],
				[value: 50, color: "#e67e22"],
				[value: 75, color: "#27ae60"]
			]
        }
        valueTile("lastcheckin", "device.lastCheckin", decoration: "flat", inactiveLabel: false, width: 4, height: 1) {
            state "default", label:'Last Checkin:\n${currentValue}'
        }
        valueTile("lastopened", "device.lastOpened", decoration: "flat", inactiveLabel: false, width: 4, height: 1) {
            state "default", label:'Last Open:\n${currentValue}'
        }
        standardTile("resetClosed", "device.resetClosed", inactiveLabel: false, decoration: "flat", width: 2, height: 1) {
            state "default", action:"resetClosed", label: "Override Close", icon:"st.contact.contact.closed"
        }
        standardTile("resetOpen", "device.resetOpen", inactiveLabel: false, decoration: "flat", width: 2, height: 1) {
            state "default", action:"resetOpen", label: "Override Open", icon:"st.contact.contact.open"
        }
        standardTile("refresh", "command.refresh", inactiveLabel: false, decoration: "flat", width: 2, height: 1) {
            state "default", label:'refresh', action:"refresh.refresh", icon:"st.secondary.refresh-icon"
        }
		  standardTile("batteryRuntime", "device.batteryRuntime", inactiveLabel: false, decoration: "flat", width: 6, height: 2) {
			   state "batteryRuntime", label:'Battery Changed: ${currentValue} - Tap to reset Date', unit:"", action:"resetBatteryRuntime"
		  } 
        main (["contact"])
        details(["contact","battery","lastcheckin","lastopened","resetClosed","resetOpen","refresh","batteryRuntime"])
   }
}

def parse(String description) {
    log.debug "${device.displayName}: Parsing '${description}'"

    //  send event for heartbeat    
    def now = new Date().format("yyyy MMM dd EEE h:mm:ss a", location.timeZone)
    def nowDate = new Date(now).getTime()
    sendEvent(name: "lastCheckin", value: now)
    sendEvent(name: "lastCheckinDate", value: nowDate, displayed: false) 

    Map map = [:]

    if (description?.startsWith('on/off: ')) 
    {
        map = parseCustomMessage(description) 
        sendEvent(name: "lastOpened", value: now)
        sendEvent(name: "lastOpenedDate", value: nowDate) 
    }
    else if (description?.startsWith('catchall:')) 
    {
        map = parseCatchAllMessage(description)
    }
    else if (description?.startsWith("read attr - raw: "))
    {
        map = parseReadAttrMessage(description)  
    }
    log.debug "${device.displayName}: Parse returned $map"
    def results = map ? createEvent(map) : null

    return results;
}

private Map parseReadAttrMessage(String description) {
    def result = [
        name: 'Model',
        value: ''
    ]
    def cluster
    def attrId
    def value
        
    cluster = description.split(",").find {it.split(":")[0].trim() == "cluster"}?.split(":")[1].trim()
    attrId = description.split(",").find {it.split(":")[0].trim() == "attrId"}?.split(":")[1].trim()
    value = description.split(",").find {it.split(":")[0].trim() == "value"}?.split(":")[1].trim()
    //log.debug "cluster: ${cluster}, attrId: ${attrId}, value: ${value}"
    
    if (cluster == "0000" && attrId == "0005") 
    {
        // Parsing the model
        for (int i = 0; i < value.length(); i+=2) 
        {
            def str = value.substring(i, i+2);
            def NextChar = (char)Integer.parseInt(str, 16);
            result.value = result.value + NextChar
        }
    }
    //log.debug "Result: ${result}"
    return result
}

private Map getBatteryResult(rawValue) {
    def rawVolts = rawValue / 1000

    def minVolts = 2.7
    def maxVolts = 3.3
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
    if (state.battery != result.value)
    {
    	state.battery = result.value
        resetBatteryRuntime()
    }
    return result
}

private Map parseCatchAllMessage(String description) {
    Map resultMap = [:]
    def cluster = zigbee.parse(description)
    def i
    log.debug "${device.displayName}: Parsing CatchAll: ${cluster}"
    if (cluster) {
        switch(cluster.clusterId) {
            case 0x0000:
                def MsgLength = cluster.data.size();
                for (i = 0; i < (MsgLength-3); i++)
                {
                    // Original Xiaomi CatchAll does not have identifiers, first UINT16 is Battery
                    if ((cluster.data.get(0) == 0x02) && (cluster.data.get(1) == 0xFF))
                    {
                        if (cluster.data.get(i) == 0x21) // check the data ID and data type
                        {
                            // next two bytes are the battery voltage.
                            resultMap = getBatteryResult((cluster.data.get(i+2)<<8) + cluster.data.get(i+1))
                            return resultMap
                        }
                    }
                }
                break
        }
    }

    return resultMap
}


def configure() {
	state.battery = 0
    String zigbeeEui = swapEndianHex(device.hub.zigbeeEui)
    log.debug "${device.displayName}: ${device.deviceNetworkId}"
    def endpointId = 1
    log.debug "${device.displayName}: ${device.zigbeeId}"
    log.debug "${device.displayName}: ${zigbeeEui}"
    def configCmds = [
        //battery reporting and heartbeat
        // send-me-a-report 3600 43200 is min and max reporting time range
        "zdo bind 0x${device.deviceNetworkId} 1 ${endpointId} 1 {${device.zigbeeId}} {}", "delay 200",
        "zcl global send-me-a-report 1 0x20 0x20 3600 43200 {01}", "delay 200",
        "send 0x${device.deviceNetworkId} 1 ${endpointId}", "delay 1500",


        // Writes CIE attribute on end device to direct reports to the hub's EUID
        "zcl global write 0x500 0x10 0xf0 {${zigbeeEui}}", "delay 200",
        "send 0x${device.deviceNetworkId} 1 1", "delay 500",
    ]

    log.debug "${device.displayName}: configure: Write IAS CIE"
    return configCmds
}

def enrollResponse() {
    log.debug "${device.displayName}: Enrolling device into the IAS Zone"
    [
        // Enrolling device into the IAS Zone
        "raw 0x500 {01 23 00 00 00}", "delay 200",
        "send 0x${device.deviceNetworkId} 1 1"
    ]
}

def refresh(){
    log.debug "${device.displayName}: refreshing"
    return zigbee.readAttribute(0x0001, 0x0020) + zigbee.configureReporting(0x0001, 0x0020, 0x21, 600, 21600, 0x01)
}


private Map parseCustomMessage(String description) {
    def result
    if (description?.startsWith('on/off: ')) {
        if (description == 'on/off: 0')         //contact closed
            result = getContactResult("closed")
        else if (description == 'on/off: 1')     //contact opened
            result = getContactResult("open")
        return result
    }
}

private Map getContactResult(value) {
    def descriptionText = "${device.displayName} was ${value == 'open' ? 'opened' : 'closed'}"
    return [
        name: 'contact',
        value: value,
        descriptionText: descriptionText
    ]
}

private String swapEndianHex(String hex) {
    reverseArray(hex.decodeHex()).encodeHex()
}


private byte[] reverseArray(byte[] array) {
    int i = 0;
    int j = array.length - 1;
    byte tmp;

    while (j > i) {
        tmp = array[j];
        array[j] = array[i];
        array[i] = tmp;
        j--;
        i++;
    }

    return array
}

def resetClosed() {
    sendEvent(name:"contact", value:"closed")
} 

def resetOpen() {
    sendEvent(name:"contact", value:"open")
}

def resetBatteryRuntime() {
   	def now = new Date().format("EEE dd MMM yyyy h:mm:ss a", location.timeZone)
    sendEvent(name: "batteryRuntime", value: now)
}

def installed() {
// Device wakes up every 1 hour, this interval allows us to miss one wakeup notification before marking offline
    log.debug "${device.displayName}: Configured health checkInterval when installed()"
    sendEvent(name: "checkInterval", value: 2 * 60 * 60 + 2 * 60, displayed: false, data: [protocol: "zigbee", hubHardwareId: device.hub.hardwareID])
}

def updated() {
// Device wakes up every 1 hours, this interval allows us to miss one wakeup notification before marking offline
    log.debug "${device.displayName}: Configured health checkInterval when updated()"
    sendEvent(name: "checkInterval", value: 2 * 60 * 60 + 2 * 60, displayed: false, data: [protocol: "zigbee", hubHardwareId: device.hub.hardwareID])
}

/**
 *  Hubitat Import URL: https://raw.githubusercontent.com/JonoPorter/BetterLaundryMonitor/master/BetterLaundryMonitor_Child.groovy
 */

/**
 *  Alert on Power Consumption
 *
 *  Copyright 2019 Jonathan Porter, Kevin Tierney, C Steele
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
 */
	public static String version()      {  return "v1.4.6"  }


import groovy.time.*

definition(
	name: "Better Laundry Monitor - Power Switch",
	namespace: "tierneykev",
	author: "Jonathan Porter, Kevin Tierney, ChrisUthe, CSteele",
	description: "Child: powerMonitor capability, monitor the laundry cycle and alert when it's done.",
	category: "Green Living",
	    
	parent: "tierneykev:Better Laundry Monitor",
	
	iconUrl: "",
	iconX2Url: "",
	iconX3Url: ""
)


preferences {
	page (name: "mainPage")
	page (name: "sensorPage")
	page (name: "thresholdPage")
	page (name: "informPage")
}
//<div style='color:#ffffff;font-weight: bold;background-color:#81BC00;border: 1px solid;box-shadow: 2px 3px #A9A9A9'>${myText}</div>

def mainPage() {
	dynamicPage(name: "mainPage", install: true, uninstall: true) {
		updateMyLabel()
		section("<h2>${app.label ?: app.name}</h2>"){
			if (!atomicState.isPaused) {
				input(name: "pauseButton", type: "button", title: "Pause", backgroundColor: "Green", textColor: "white", submitOnChange: true)
			} else {
				input(name: "resumeButton", type: "button", title: "Resume", backgroundColor: "Crimson", textColor: "white", submitOnChange: true)
			}
		}
		section("-= <b>Main Menu</b> =-") 
		{
			input (name: "deviceType", title: "Type of Device", type: "enum", options: [powerMeter:"Power Meter", jporterAccelSensor:"Vibration Sensor "], required:true, submitOnChange:true)
		}

		if (deviceType) {
			section
			{
				href "sensorPage", title: "Sensors", description: "Sensors to be monitored", state: selectOk?.sensorPage ? "complete" : null
				href "thresholdPage", title: "Thresholds", description: "Thresholds to be monitored", state: selectOk?.thresholdPage ? "complete" : null
				href "informPage", title: "Inform", description: "Who and what to Inform", state: selectOk?.informPage ? "complete" : null
			}
		}
		section (title: "<b>Name/Rename</b>") {
			label title: "This child app's Name (optional)", required: false, submitOnChange: true
			if (!app.label) {
				app.updateLabel(app.name)
				atomicState.appDisplayName = app.name
			}
			if (app.label.contains('<span ')) {
				if (atomicState?.appDisplayName != null) {
					app.updateLabel(atomicState.appDisplayName)
				} else {
					String myLabel = app.label.substring(0, app.label.indexOf('<span '))
					atomicState.appDisplayName = myLabel
					app.updateLabel(myLabel)
				}				
			}
		}
		display()
	}
}


def sensorPage() {
	dynamicPage(name: "sensorPage") {
		if (deviceType == "powerMeter") {
			section ("<b>When this device starts/stops drawing power</b>") {
				input "pwrMeter", "capability.powerMeter", title: "Power Meter" , multiple: false, required: false, defaultValue: null
			}
		}
		if (deviceType == "jporterAccelSensor") {
			section("<b>Device to monitor Acceleration</b>") {
				input "accelSensor", "capability.accelerationSensor", title: "Acceleration Sensor" , multiple: false, required: false, defaultValue: null
			}
		}

		section("<b>Device to monitor if machine is open</b>") {
			input "contactSensor", "capability.contactSensor", title: "Contact Sensor" , multiple: false, required: false, defaultValue: null
		}
	}
}


def thresholdPage() {
	dynamicPage(name: "thresholdPage") {
 
		if (deviceType == "powerMeter") {
			section ("<b>Power Thresholds</b>", hidden: false, hideable: true) {
				input "startThreshold", "decimal", title: "Start cycle when power raises above (W)", defaultValue: "8", required: false
				input "ignoreThreshold", "decimal", title: "Optional: Ignore extraneous power readings above (W)", defaultValue: "1500", required: false
			}
		}
		section("<b>Time Thresholds (in minutes)</b>", hidden: false, hideable: true) {
			input "activeTimeout", "decimal", title: "Minumum amount of time (in minutes) for a cycle to be considered running.", required: false, defaultValue: 10
			input "inactiveTimeout", "number", title: "Max allowed inactivity time (in seconds) before its considered finished.", required: false, defaultValue: 60
		}
	}
}
 

def informPage() {
	dynamicPage(name: "informPage") {
		section ("<b>Send this message</b>", hidden: false, hideable: true) {
			input "messageStart", "text", title: "Notification message Start (optional)", description: "Laundry is started!", required: false
			input "message", "text", title: "Notification message End", description: "Laundry is done!", required: true
		}
		section (title: "<b>Using this Notification Method</b>", hidden: false, hideable: true) {
			input "textNotification", "capability.notification", title: "Send Via: (Notification)", multiple: true, required: false
			input "speechOut", "capability.speechSynthesis", title:"Speak Via: (Speech Synthesis)", multiple: true, required: false
			input "speechEchoAnnouncement", "bool", title:"Speak Via 'Echo Speaks' Play Announcement All (only need one echo device in above list)", defaultValue: false, required: false
			input "player", "capability.musicPlayer", title:"Speak Via: (Music Player -> TTS)", multiple: true, required: false
			input "blockIt", "capability.switch", title: "Switch to Block Speak if ON", multiple: false, required: false
			input "sendRepeat", "number", title:"Number of extra times to notify while contact sensor is closed", defaultValue: 0, required: false
			input "sendRepeatDelay", "number", title:"Delay in seconds between the each announcements", defaultValue: 100, required: false
		}
		section ("<b>Choose Additional Devices</b>") {
		  	input "switchList", "capability.switch", title: "Which Switches?", description: "Switches to follow the active state", multiple: true, hideWhenEmpty: false, required: false             
		}
	}
}


def getSelectOk()
{
	def status =
	[
		sensorPage: pwrMeter ?: accelSensor,
		thresholdPage: startThreshold ?: inactiveTimeout ?: activeTimeout,
		informPage: messageStart?.size() ?: message?.size()
	]
	status << [all: status.sensorPage ?: status.thresholdPage ?: status.informPage]
}


 
def accelerationActiveHandlerV3(evt) {
	toRunning();
}
def accelerationInactiveHandlerV3(evt) {
	toInactive();
}
def powerHandler(evt) {
	def latestPower = pwrMeter.currentValue("power")
	if(latestPower < ignoreThreshold)	
	{
		if(latestPower >= startThreshold )
		{
			toRunning();
		}
		else
		{
			toInactive();
		}
	}
}
def contactOpenHandler(evt) {
	toOpen();
}

def getNowTime()
{
	return new Date().getTime();
}

def state_running(){ "Running" }
def state_inactive(){ "Inactive" }
def state_finished(){ "Finished" }
def state_opened(){ "Opened" }

def setState(newState)
{
	def oldState = atomicState.current;
	if(oldState != newState)
	{
		atomicState.current = newState;
        if(debugOutput)
        {
		    log.debug "from ${oldState} to ${newState}"
        }
	} 
}
def isState(state)
{
	return atomicState.current == state;
}
//state machine: Running -> Inactive -> Finished -> Opened
//with all states being able to return to Running

def toRunning() {
	setState(state_running())
	if(getNowTime() - atomicState.lastActivity >= inactiveTimeout * 1000)
	{
		startCycle();
	}
	atomicState.lastActivity = getNowTime()
}
def toInactive(evt) {
	if(!isState(state_running())){ return }
	def time =  getNowTime();
	if(getNowTime() - atomicState.firstActivity >= activeTimeout * 60 * 1000)
	{
		setState(state_inactive())
		runIn(inactiveTimeout, toFinished,[data: [lastActivity : time]])
	}
	atomicState.lastActivity = time
}
def toFinished(data){
	//if its not inactive state or another event has happened since this was started then abort. 
	if(!isState(state_inactive()) 
	|| data.lastActivity != atomicState.lastActivity  ){ return }
	setState(state_finished())
	endCycle();

	runRepeat(sendRepeat)
}
def runRepeat(repeatCount)
{
	if(repeatCount > 0)
	{
		runIn(sendRepeatDelay, repeatHandler, [data: [repeatCount: repeatCount]])
	}
}
def repeatHandler(data)
{
	if(!isState(state_finished())){ return }
	send(message,true)
	runRepeat(data.repeatCount - 1)
}
def toOpen()
{
	if(!isState(state_finished())){ return }
	setState(state_opened())
}

def startCycle()
{
	atomicState.firstActivity = getNowTime()

	if (debugOutput) log.debug "Sending cycle start notification"
	send(messageStart)

	if (switchList) switchList*.off() 
	//legacy states:
	atomicState.cycleStart = now()
	atomicState.cycleOn = true
	updateMyLabel()
}
def endCycle()
{
	if (debugOutput) log.debug "Sending cycle complete notification"
	send(message)

	if (switchList) switchList*.on() 
	//legacy states:
	atomicState.cycleEnd = now()
	atomicState.cycleOn = false
	updateMyLabel()
}
 
private send(msg) {
	send(msg,false)
}
private send(msg, speechOnly) {
	if (!msg) return // no message 
	if (!speechOnly && textNotification) { textNotification*.deviceNotification(msg) }
	if (debugOutput) { log.debug "send: $msg" }
	if (blockIt && blockIt.currentValue("switch") == "on") return // no noise please.
	if (speechOut) 
	{ 
		if(speechEchoAnnouncement)
		{
			speechOut*.playAnnouncementAll(msg)
		}
		else
		{
			speechOut*.speak(msg)
		}
	}
	if (player){ player*.playText(msg) }
}


def installed() {
	// Initialize the states only when first installed...
	atomicState.cycleOn = null		// we don't know if we're running yet


	if (switchList) switchList*.off() 
 
	
	initialize()
	app.clearSetting("debugOutput")	// app.updateSetting() only updates, won't create.
	app.clearSetting("descTextEnable")
	if (descTextEnable) log.info "Installed with settings: ${settings}"
}


def updated() {
	unsubscribe()
	unschedule()
	initialize()
	if (descTextEnable) log.info "Updated with settings: ${settings}"
}
 

def initialize() {
	if (atomicState.isPaused) {
		updateMyLabel()
		return
	}
	if (settings.deviceType == "powerMeter") {
		subscribe(pwrMeter, "power", powerHandler)
		if (debugOutput) log.debug "Cycle: ${atomicState.cycleOn} thresholds: ${startThreshold}   "
	} 
	else if (settings.deviceType == "jporterAccelSensor") {
		subscribe(accelSensor, "acceleration.active", accelerationActiveHandlerV3)
		subscribe(accelSensor, "acceleration.inactive", accelerationInactiveHandlerV3)

	}
	subscribe(contactSensor, "contact.open", contactOpenHandler)
	atomicState.firstActivity = 0;
	atomicState.lastActivity = 0;
	atomicState.current = state_running()
	//	schedule("0 0 14 ? * FRI *", updateCheck) It's run every time it's displayed
	schedule("17 5 0 * * ?", updateMyLabel)	// Fix the date string after the day changes
	updateMyLabel()
	
//	app.clearSetting("debugOutput")	// app.updateSetting() only updates, won't create.
//	app.clearSetting("descTextEnable") // un-comment these, click Done then replace the // comment
}

def appButtonHandler(btn) {
    switch(btn) {
        case "pauseButton":
			atomicState.isPaused = true
			updateMyLabel()
            break
		case "resumeButton":
			atomicState.isPaused = false
			updateMyLabel()
			break
    }
}

 


def setDebug(dbg, inf) {
	app.updateSetting("debugOutput",[value:dbg, type:"bool"])
	app.updateSetting("descTextEnable",[value:inf, type:"bool"])
	if (descTextEnable) log.info "debugOutput: $debugOutput, descTextEnable: $descTextEnable"
}


def display()
{
	//updateCheck()
	section {
		paragraph "\n<hr style='background-color:#1A77C9; height: 1px; border: 0;'></hr>"
		paragraph "<div style='color:#1A77C9;text-align:center;font-weight:small;font-size:9px'>Developed by: Kevin Tierney, ChrisUthe, C Steele, Barry Burke<br/>Version Status: $state.Status<br>Current Version: ${version()} -  ${thisCopyright}</div>"
    }
}


// Check Version   ***** with great thanks and acknowledgment to Cobra (CobraVmax) for his original code ****
def updateCheck()
{    
	def paramsUD = [uri: "https://hubitatcommunity.github.io/Hubitat-BetterLaundryMonitor/version2.json"]
	
 	asynchttpGet("updateCheckHandler", paramsUD) 
}


def updateCheckHandler(resp, data) 
{
	state.InternalName = "BLMchild"
	
	if (resp.getStatus() == 200 || resp.getStatus() == 207) {
		respUD = parseJson(resp.data)
		//log.warn " Version Checking - Response Data: $respUD"   // Troubleshooting Debug Code - Uncommenting this line should show the JSON response from your webserver 
		state.Copyright = "${thisCopyright} -- ${version()}"
		// uses reformattted 'version2.json' 
		def newVer = padVer(respUD.application.(state.InternalName).ver)
		def currentVer = padVer(version())               
		state.UpdateInfo = (respUD.application.(state.InternalName).updated)
            // log.debug "updateCheck: ${respUD.driver.(state.InternalName).ver}, $state.UpdateInfo, ${respUD.author}"
	
		switch(newVer) {
			case { it == "NLS"}:
			      state.Status = "<b>** This Application is no longer supported by ${respUD.author}  **</b>"       
			      log.warn "** This Application is no longer supported by ${respUD.author} **"      
				break
			case { it > currentVer}:
			      state.Status = "<b>New Version Available (Version: ${respUD.application.(state.InternalName).ver})</b>"
			      log.warn "** There is a newer version of this Application available  (Version: ${respUD.application.(state.InternalName).ver}) **"
			      log.warn "** $state.UpdateInfo **"
				break
			case { it < currentVer}:
			      state.Status = "<b>You are using a Test version of this Application (Expecting: ${respUD.application.(state.InternalName).ver})</b>"
				break
			default:
				state.Status = "Current"
				if (descTextEnable) log.info "You are using the current version of this Application"
				break
		}

	      sendEvent(name: "chkUpdate", value: state.UpdateInfo)
	      sendEvent(name: "chkStatus", value: state.Status)
      }
      else
      {
           log.error "Something went wrong: CHECK THE JSON FILE AND IT'S URI"
      }
}

void updateMyLabel() {
	boolean ST = false
	String flag = '<span '
	
	// Display Ecobee connection status as part of the label...
	String myLabel = atomicState.appDisplayName
	if ((myLabel == null) || !app.label.startsWith(myLabel)) {
		myLabel = app.label ?: app.name
		if (!myLabel.contains(flag)) atomicState.appDisplayName = myLabel
	} 
	if (myLabel.contains(flag)) {
		// strip off any connection status tag
		myLabel = myLabel.substring(0, myLabel.indexOf(flag))
		atomicState.appDisplayName = myLabel
	}
	String newLabel
	if (atomicState.isPaused) {
		newLabel = myLabel + '<span style="color:Crimson"> (paused)</span>'
	} else if (atomicState.cycleOn) {
		String beganAt = atomicState.cycleStart ? "started " + fixDateTimeString(atomicState.cycleStart) : 'running'
		newLabel = myLabel + "<span style=\"color:Green\"> (${beganAt})</span>"
	} else if ((atomicState.cycleOn != null) && (atomicState.cycleOn == false)) {
		String endedAt = atomicState.cycleEnd ? "finished " + fixDateTimeString(atomicState.cycleEnd) : 'idle'
		newLabel = myLabel + "<span style=\"color:Green\"> (${endedAt})</span>"
	} else {
		newLabel = myLabel
	}
	if (app.label != newLabel) app.updateLabel(newLabel)
}
				   
String fixDateTimeString( eventDate) {
	def today = new Date(now()).clearTime()
	def target = new Date(eventDate).clearTime()
	
	String resultStr = ''
	String myDate = ''
	String myTime = ''
	boolean showTime = true
	
	if (target == today) {
		myDate = 'today'	
	} else if (target == today-1) {
		myDate = 'yesterday'
	} else if (target == today+1) {
		myDate = 'tomorrow'
	} else if (dateStr == '2035-01-01' ) {		// to Infinity
		myDate = 'a long time from now'
		showTime = false
	} else {
		myDate = 'on '+target.format('MM-dd')
	}	 
	if (showTime) {
		myTime = new Date(eventDate).format('h:mma').toLowerCase()
	}
	if (myDate || myTime) {
		resultStr = myTime ? "${myDate} at ${myTime}" : "${myDate}"
	}
 
	return resultStr
}

/*
	padVer

	Version progression of 1.4.9 to 1.4.10 would mis-compare unless each column is padded into two-digits first.

*/ 
def padVer(ver) {
	def pad = ""
	ver.replaceAll( "[vV]", "" ).split( /\./ ).each { pad += it.padLeft( 2, '0' ) }
	return pad
}

def getThisCopyright(){"&copy; 2019 J Porter, C Steele "}
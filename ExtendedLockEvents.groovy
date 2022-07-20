// Derived from code provided by Russ Vrolyk - https://github.com/rvrolyk
// This app adds functionnality that was missing from Rule Machine: the ability to run an action when a lock is locked specifically by keypad (from outside)
// https://community.hubitat.com/t/lock-code-condition/16022
// https://community.hubitat.com/t/rule-machine-request-locked-manually-vs-keypad/5946
//
// The original code required to create virtual switch for each lock mode. I prefer to call rules directly.
// This code has been working for a Weiser Lock with generic ZigBee driver.

import hubitat.helper.RMUtils

definition(
    name: "Extended Lock Events",
    namespace: "cramezul",
    author: "Patrick Beland",
    description: "Allows to perform different actions whether a lock is locked manually, by keypad or digitally",
    category: "Convenience",
	iconUrl: "",
    iconX2Url: "",
    iconX3Url: "")



def lock = [
    name: "lock",
    type: "capability.lock",
    title: "Lock",
    description: "Select the lock to monitor.",
    required: true,
    multiple: false
]

def rules = RMUtils.getRuleList("5.0")

def keypadRule = [
    name: "keypadRule",
    type: "enum",
    title: "Rule to run when locked by the keypad",
    description: "Select the rule to run when locked by the keypad",
    required: false,
    options: rules,
    multiple: false
]

def manualRule = [
    name: "manualRule",
    type: "enum",
    title: "Rule to run when locked manually",
    description: "Select the rule to run when locked manually (from inside or by key)",
    required: false,
    options: rules,
    multiple: false
]

def digitalRule = [
    name: "digitalRule",
    type: "enum",
    title: "Rule to run when locked digitally",
    description: "Select the rule to run when locked by digital command",
    required: false,
    options: rules,
    multiple: false
]

def enableLogging = [
    name:				"enableLogging",
    type:				"bool",
    title:				"Enable debug Logging?",
    defaultValue:		false,
    required:			true
]

preferences {
	page(name: "mainPage", title: "<b>Extened Lock Events</b>", install: true, uninstall: true) {
		section("") {
            paragraph "Select a lock to monitor and rules to run on extended events."
			input lock
            input keypadRule
            input manualRule
            input digitalRule
			label title: "Assign an app name", required: false
		}
		section ("<b>Advanced Settings</b>") {
			input enableLogging
		}
	}
}

def installed() {
	log.info "Installed with settings: ${settings}"
	initialize()
}

def updated() {
	log.info "Updated with settings: ${settings}"
	unsubscribe()
	initialize()
}

def initialize() {
    logDebug "Initializing extended events for ${lock.displayName} current status: " + lock.currentValue("lock")
	subscribe(lock, "lock.locked", lockHandler)
}


def lockHandler(evt) {
    logDebug("Lock event: ${evt.name} - ${evt.descriptionText} type: ${evt.type}")
    
    if (evt.type == 'physical') {
        if (evt.descriptionText.endsWith('locked by keypad')) {
		    logDebug "${lock.displayName} was locked by keypad, running rule ${keypadRule}"
            if (keypadRule)
                RMUtils.sendAction([keypadRule], "runRuleAct", app.label, "5.0")
        }
        else if (evt.descriptionText.endsWith('locked by manual')) {
            logDebug "${lock.displayName} was locked manually, running rule ${manualRule}"
            if (manualRule) 
                RMUtils.sendAction([manualRule], "runRuleAct", app.label, "5.0")
        }
    }
    else if (evt.type == 'digital') {
        logDebug "${lock.displayName} was locked by digital command, running rule ${digitalRule}"
        if (digitalRule) 
                RMUtils.sendAction([digitalRule], "runRuleAct", app.label, "5.0")
	}
    else {
        logDebug "Unhandled lock action on ${lock.displayName}"
    }        
}

def logDebug(msg) {
    if (enableLogging) {
        log.debug msg
    }
}

# Headset Harry
Reads events through a connected headset.

Events can be:
* Incoming SMS
* Calendar event
* ...

One core concept is that Headset Harry will try to be smart about what
language should be used for what events.

Headset Harry will support all the languages you have TTS support for.

# TODO Before Installing on Johan's Phone
* Listen for incoming SMS messages
* When a message comes in, ignore it unless sound is being routed
through a headset
* When a message comes in, ignore it if a phone call is in progress
* Inventory available voices on the system to determine which languages
to support
* When a message arrives, determine what language to use for reading it
* Use an appropriate TTS engine / voice for reading the incoming SMS

# TODO Before Getting Beta Users
* Think about whether we think we'll survive app upgrades
* Log to Crashlytics

# TODO Before Releasing on Android Market
* When a message comes in, enqueue it if a phone call is in progress.
* When a phone call ends, read all events received during the call
* If the user is playing music, pause or turn down the music while the
message is being read.

# TODO Misc
* Add support for calendar events. 
* Add support for incoming Google Inbox e-mails.
* Add Caller ID support.

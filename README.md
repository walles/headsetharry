# Headset Harry
Reads events through a connected headset.

Events can be:
* Incoming SMS
* Calendar event
* ...

One core concept is that Headset Harry will try to be smart about what
language should be used for what events.

Headset Harry will support all the languages you have TTS support for.

# Building
Before building the project you need to add an `app/fabric.properties`
file. Here's one that will enable you to build and run:
```
apiKey=0
```

If you want to do it properly, [set up a (free) Crashlytics
account](http://try.crashlytics.com/) and use this `fabric.properties`:
```
apiSecret=YOUR_BUILD_SECRET_HERE
apiKey=YOUR_API_KEY_HERE_
```
The values can be retrieved from https://fabric.io/settings/organizations
by clicking the (very small) `API Key` and `Build Secret` links.

# TODO Before Installing on Johan's Phone
* When a message comes in, ignore it unless sound is being routed
through a headset

# TODO Before Getting Beta Users
* Make sure we use the notification stream for our messages
* Look up message sender's phone number for incoming messages and say
the name of the sender rather than their phone number.
* For incoming messages, identify exactly the languages for which there
are voices installed on the system.
* Add a `.travis.yml` configuration to lint and run the unit tests
* Make up a release process and document it
* Make sure we don't say / do weird things on incoming MMS messages
* Think about whether we think we'll survive app upgrades
* Think about whether we think we'll survive device reboots

# TODO Before Releasing on Android Market
* Add a license
* Look up incoming phone numbers and read contact names rather than
phone numbers.
* When a message comes in, enqueue it if a phone call is in progress.
* When a phone call ends, read all events received during the call
* If the user is playing music, pause or turn down the music while the
message is being read.
* Make an icon / all the other graphics needed by Market
* Add a Settings activity where:
    * You can see what languages are available
    * You can see What languages are enabled (there's a TTS voice for the
    language)
    * Clicking a checkbox next to an available but not enabled language
    takes you to the setup for the corresponding speech engine
    * There's a button for installing more languages, taking you to
    https://play.google.com/store/search?q=tts&c=apps

# TODO Misc
* In Settings, should it be possible to tag contacts with their most
likely language?
* "Phone call from NN" (NN is name if in contacts, not number)
* "Calendar event: ..."
* Google Inbox: "Mail from NN: SUBJECT"
* "Connected to WIFI-NAME"
* "Wireless network connection lost"
* Think about long messages, should we have an upper limit on how much
we read?
* Think about multipart messages, how do we handle them? How should we
handle them?

# DONE
* Log to Crashlytics
* Listen for incoming SMS messages in `SmsReceiver.java`
* Upload to GitHub
* When a message arrives, determine what language to use for reading it
* Use an appropriate TTS engine / voice for reading the incoming SMS
* When a message comes in, ignore it if a phone call is in progress

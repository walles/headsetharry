[![Build Status](https://travis-ci.org/walles/headsetharry.svg?branch=master)](https://travis-ci.org/walles/headsetharry)

# Headset Harry
Reads events through a connected headset.

Events can be:
* Incoming SMS
* Incoming MMS
* Incoming e-mail
* Wireless network connected / disconnected
* ...

One core concept is that Headset Harry tries to be smart about what
language should be used for what events.

Headset Harry supports all the languages you have TTS support for.

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

## Releasing
1. Do ```git tag``` and think about what the next version number should be.
2. Do ```git tag --annotate version-1.2.3``` to set the next version number.
3. ```./gradlew --no-daemon clean build```
4. Upload ```app/build/outputs/apk/app-release.apk``` to
  [Google Play](https://play.google.com/apps/publish/)
5. ```git push --tags```

# TODO Before Releasing on Android Market
* Make an icon / all the other graphics needed by Market

# TODO Misc
* Report battery status for attached bluetooth devices,
http://stackoverflow.com/a/19701412/473672,
https://developer.bluetooth.org/gatt/characteristics/Pages/CharacteristicViewer.aspx?u=org.bluetooth.characteristic.battery_level.xml
* When a message comes in, enqueue it if a phone call is in progress.
* When a phone call ends, read all events received during the call
* What do we do if there's no TTS for a configured language when we try
to say something? Something notification based perhaps.
* Enable selecting language flavors. US or GB English? Swedish Swedish
or Finnish Swedish?
* GMail: "Mail from NN: SUBJECT"
* "Phone call from NN" (NN is name if in contacts, not number)
* Think about long messages, should we have an upper limit on how much
we read?
* Think about multipart messages, how do we handle them? How should we
handle them?
* Remove `HardCodedStringLiteral` suppressions and remedy all warnings
* Enable PMD NLS warnings if it has those
* Read any text parts of MMS messages. To get the contents, maybe watch
the MMS database: http://stackoverflow.com/a/6152073/473672

# DONE
* Log to Crashlytics
* Listen for incoming SMS messages in `SmsReceiver.java`
* Upload to GitHub
* When a message arrives, determine what language to use for reading it
* Use an appropriate TTS engine / voice for reading the incoming SMS
* When a message comes in, ignore it if a phone call is in progress
* When a message comes in, ignore it unless sound is being routed
through a headset
* Verify that we work properly with a Bluetooth headset connected
* Set app version name from `git describe`
* On incoming messages, try to find a template in the same language as
the message
* Look up message sender's phone number for incoming messages and say
the name of the sender rather than their phone number.
* Present phone numbers for which we have no contact info as unknown
* When finding TTS engines supporting the required language, try the
system default TTS engine first
* Make a Settings activity where one can set which languages to support.
* For incoming messages, identify exactly the languages that have been
configured in the settings.
* Default the configured-languages list to the system language
* Support speaking over Bluetooth SCO if available and supported.
* Caps the language names in the settings, or at least the first one
* Announce incoming MMS. We don't access their content; just announcing
them will be enough for now.
* Look up incoming phone numbers and read contact names rather than
phone numbers.
* Announce connections to and disconnections from Wifi networks
* Add support for announcements over wired headsets
* Somehow try to help / inform the user if the language they want has no
TTS available.
* Don't look up "phone number"s with letters in them, just pretend they
are already looked up.
* Think about whether we think we'll survive app upgrades. Yes, we're
just receiving broadcasts, nothing that needs to be restarted for that.
* Think about whether we think we'll survive device reboots. Yes, we're
just receiving broadcasts, nothing that needs to be restarted for that.
* Add a `.travis.yml` configuration to run lint and the unit tests
* Add Findbugs to the Travis build
* Don't log to Crashlytics if running in the emulator
* Set app version code from `git` somehow
* Make up a release process and document it
* Protect the master branch on GitHub
* Add pre-commit and pre-push hooks that verifies stuff
* Add a license and put copyright headers in all files
* Handle announcing in locales we don't have translations for.
* Make sure people don't deselect all languages.
* In the language selection activity, test-speak a language when it's
chosen. Then, if that doesn't work, refer people to the system language
settings or to Google Play Store for installing more engines /
languages.
* Announce Google Inbox incoming e-mail by reading system notifications
* If we get multiple events at once, speak one at a time rather than just
messing up.
* Report most popular notification apps to Crashlytics.
* If the user is playing music, pause or turn down the music while the
message is being read.
* "Calendar event: ..."

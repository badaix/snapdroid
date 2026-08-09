# Minimal deploy helper: build the debug APK and push it to an Android
# device over ADB. Override variables on the command line, e.g.
#
#   make deploy DEVICE=192.168.1.50:5555
#   make deploy ADB=$ANDROID_HOME/platform-tools/adb
#   make deploy GRADLE=gradlew APK=/tmp/Snapcast-debug.apk

DEVICE  ?= 192.168.0.106:5555
ADB     ?= adb
GRADLE  ?= ./gradlew
APK     ?= Snapcast/build/outputs/apk/debug/Snapcast-debug.apk
PACKAGE ?= de.badaix.snapcast

.PHONY: deploy

deploy:
	$(GRADLE) assembleDebug
	$(ADB) connect $(DEVICE)
	$(ADB) -s $(DEVICE) install -r $(APK)
	$(ADB) -s $(DEVICE) shell am start -n $(PACKAGE)/.MainActivity

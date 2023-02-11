# Snapdroid: Privacy policy

This is an open source Android app developed by Johannes Pohl and [contributors](https://github.com/badaix/snapdroid/graphs/contributors). The source code is available on GitHub under the GPL-3.0; the app is also available on Google Play.

As an avid Android user myself, I take privacy very seriously.
I know how irritating it is when apps collect your data without your knowledge.

I hereby state, to the best of my knowledge and belief, that I have not programmed this app to collect any personally identifiable information.

## Explanation of permissions requested in the app

The list of permissions required by the app can be found in the `AndroidManifest.xml` file:

https://github.com/badaix/snapdroid/blob/master/Snapcast/src/main/AndroidManifest.xml

<br/>

| Permission | Why it is required |
| :---: | --- |
| `android.permission.WAKE_LOCK` | Required to play audio in the background and while the screen is locked. Permission automatically granted by the system; can't be revoked by user. |
| `android.permission.INTERNET` | Required to open a TCP connection to the Snapcast server. Permission automatically granted by the system; can't be revoked by user. |
| `android.permission.FOREGROUND_SERVICE` | Enables the app to create spawn the native Snapcast client as a server, running independly from the control app. Permission automatically granted by the system; can't be revoked by user. |
| `android.permission.RECEIVE_BOOT_COMPLETED` | This permission enables the app to receive a message from the system once the system has rebooted and to start the Snapcast client automatically, if "auto start" is enabled. Permission automatically granted by the system; can't be revoked by user. |
| `android.permission.CHANGE_WIFI_MULTICAST_STATE` | Enables automatic discovery of the Snapcast server in the LAN via mDNS. Permission automatically granted by the system; can't be revoked by user. |
| `android.permission.POST_NOTIFICATIONS` | Required to show a notification while audio is being played. |

 <hr style="border:1px solid gray">

If you find any security vulnerability that has been inadvertently caused by me, or have any question regarding how the app protectes your privacy, please send me an email or post a discussion on GitHub, and I will surely try to fix it/help you.

Yours sincerely,  
Johannes Pohl.  
Aachen, Germany.  
snapcast@badaix.de

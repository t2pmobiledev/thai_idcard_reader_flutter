## 0.0.33
* Android: add `startPermissionWatch()` / `stopPermissionWatch()` to request USB permission as soon as the reading screen opens or a reader is plugged in.
* Android: getInfo permission wait reduced to 15 s (below the Dart 20 s timeout); drop cached reader that was unplugged before permission was granted.
## 0.0.32
* Android: request USB permission at read time and wait for the user's answer (no "Always allow" / USB_DEVICE_ATTACHED needed). New code 010 = permission denied.
* Android: fix permission receiver leak, check grant result, register receiver as not exported.
## 0.0.7
* improve filepath.
## 0.0.6
* fix bug andd Support Android 12.
## 0.0.5
* add credit.
## 0.0.4
* fixed some bug.
## 0.0.3
* Improve Changelog.
## 0.0.2
* Improve code.
## 0.0.1
* First publish package.

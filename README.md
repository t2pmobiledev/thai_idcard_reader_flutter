# thai_idcard_reader_flutter

A plugin for communicating with ACS ACR39U Smart Card reader to read Thai ID card instantly.

Credits. ADPU Command from [ThaiNationalIDCard](https://github.com/chakphanu/ThaiNationalIDCard) and android source code project from [android_thai_idcard_reader](https://github.com/anoochit/android_thai_idcard_reader)

## Support
- Android 5.0 or newer
- iOS is unavailable

## Tested Devices 
- ACR39U-NF PocketMate II Smart Card Reader (USB Type-C)
- or Any Products tha made from ACS if it works.



## Getting Started

```dart

// import packages
import 'package:thai_idcard_reader_flutter/thai_idcard_reader_flutter.dart';

// put this line to initState() for listening Reader if it connect to device.
ThaiIdcardReaderFlutter.deviceHandlerStream.listen(_onUSB);


// create function to handle reader connects to app.
void _onUSB(usbEvent) {
    try {
        // if reader connected and accepted permission to device.
        if (usbEvent.hasPermission) {
        // add subscription to listen to card insert to reader.
        subscription = ThaiIdcardReaderFlutter.cardHandlerStream.listen(_onData);
        } else {
        // if reader is disconnected. cancel listen to card from reader
        if (subscription == null) {
            subscription?.cancel();
            subscription = null;
        }
        _clear();
        }
        setState(() {
        _device = usbEvent;
        });
    } catch (e) {
        setState(() {
        _error = "_onUSB " + e.toString();
        });
    }
}

// create function to listen card to read data.
void _onData(readerEvent) {
    try {
        setState(() {
        _card = readerEvent;
        });
        if (readerEvent.isReady) {
        // to read all data from ID card.
        readCard();
        } else {
        _clear();
        }
    } catch (e) {
        setState(() {
        _error = "_onData " + e.toString();
        });
    }
}

// create function to read all data ID card if card has inserted to reader.
readCard({List<String> only = const []}) async {
    try {
        var response = await ThaiIdcardReaderFlutter.read(only: only);
        setState(() {
        _data = response;
        });
    } catch (e) {
        setState(() {
        _error = 'ERR readCard $e';
        });
    }
}

```

## Android USB Always Allow Permission

By default, Android's USB permission dialog does **not** show the "Always allow from this app" checkbox. This means users must tap "Allow" every time they reconnect the USB smart card reader or relaunch the app.

Android will show the "Always allow" checkbox only when the host app's `AndroidManifest.xml` declares an `<intent-filter>` for `android.hardware.usb.action.USB_DEVICE_ATTACHED` together with a `<meta-data>` pointing to a `device_filter.xml` that matches the connected USB device.

To enable this in your own app, follow the steps below.

### Step 1 — Create `res/xml/device_filter.xml`

Create the file at `android/app/src/main/res/xml/device_filter.xml` (create the `xml/` folder if it does not exist):

```xml
<?xml version="1.0" encoding="utf-8"?>
<resources>
    <!-- ACS vendor-only entry: matches ALL ACS products (vendorId 1839 = 0x072F) -->
    <usb-device vendor-id="1839" />

    <!-- Specific ACS product entries -->
    <usb-device vendor-id="1839" product-id="8704" />  <!-- ACR122U -->
    <usb-device vendor-id="1839" product-id="8730" />  <!-- ACR1281U / ACR39U -->
</resources>
```

> **Note:** `vendor-id` and `product-id` accept **decimal integers only** — do not use hex literals such as `0x072F`.

### Step 2 — Update `AndroidManifest.xml`

Inside your `MainActivity` `<activity>` element, add an `<intent-filter>` and a `<meta-data>` element:

```xml
<activity
    android:name=".MainActivity"
    android:launchMode="singleTop"
    ...>

    <!-- Existing intent-filter for app launch -->
    <intent-filter>
        <action android:name="android.intent.action.MAIN"/>
        <category android:name="android.intent.category.LAUNCHER"/>
    </intent-filter>

    <!-- NEW: USB device attached intent-filter -->
    <intent-filter>
        <action android:name="android.hardware.usb.action.USB_DEVICE_ATTACHED" />
    </intent-filter>

    <!-- NEW: USB device filter reference -->
    <meta-data
        android:name="android.hardware.usb.action.USB_DEVICE_ATTACHED"
        android:resource="@xml/device_filter" />

</activity>
```

Once both files are in place, Android will display the "Always allow from this app" checkbox in the USB permission dialog whenever a matching ACS reader is connected.

## Optional from `ThaiIdcardReaderFlutter.read()`


```dart

// for reall all data from ID card.
await ThaiIdcardReaderFlutter.read();


// for read any data from ID card. you can add/remove in List as you want.
await ThaiIdcardReaderFlutter.read(only: [
    ThaiIDType.cid,
    ThaiIDType.photo,
    ThaiIDType.nameTH,
    ThaiIDType.nameEN,
    ThaiIDType.gender,
    ThaiIDType.birthdate,
    ThaiIDType.address,
    ThaiIDType.issueDate,
    ThaiIDType.expireDate,
  ]);

```


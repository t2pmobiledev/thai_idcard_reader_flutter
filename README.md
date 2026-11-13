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

## Android USB Permission

The plugin requests USB permission at read time. When `read()` is called and the reader has no permission yet, the plugin shows the system "Allow access" dialog and waits for the user's answer (up to 30 seconds) before reading the card. The permission lasts until the reader is unplugged; after that, the dialog is shown again on the next read.

The host app does **not** need a `USB_DEVICE_ATTACHED` intent-filter or a `device_filter.xml`. Without them Android shows no "Always allow / use by default" option and does not launch the app when the reader is plugged in.

Response codes related to the reader and permission:

| code | meaning |
|------|---------|
| `004` | No USB reader found, or it was unplugged while waiting for permission |
| `009` | Permission not granted in time (dialog not answered within 30 s), or a request is already waiting |
| `010` | User denied the USB permission dialog |

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


import 'dart:async';
import 'package:flutter/services.dart';
import 'package:flutter/material.dart';
import 'package:thai_idcard_reader_flutter/thai_idcard_reader_flutter.dart';
import 'package:intl/date_symbol_data_local.dart';
import 'dart:typed_data';

import 'package:intl/intl.dart';

void main() {
  Intl.defaultLocale = 'th_TH';
  initializeDateFormatting('th_TH', null);
  runApp(const MyApp());
}

class MyApp extends StatefulWidget {
  const MyApp({Key? key}) : super(key: key);

  @override
  State<MyApp> createState() => _MyAppState();
}

class _MyAppState extends State<MyApp> {
  ThaiIDCard? _data;
  var _error;
  String? _rawResponse;
  bool _loading = false;
  bool _initializing = true;

  @override
  void initState() {
    super.initState();
    _initDevice();
  }

  Future<void> _initDevice() async {
    // Trigger getInfo once to initialize the SmartCardDevice and request USB permission.
    // We ignore the result — this just warms up the native side.
    try {
      await ThaiIdcardReaderFlutter.read();
    } catch (_) {}
    setState(() {
      _initializing = false;
    });
  }

  final List<String> _idCardType = [
    ThaiIDType.cid,
    ThaiIDType.photo,
    ThaiIDType.nameTH,
    ThaiIDType.nameEN,
    ThaiIDType.gender,
    ThaiIDType.birthdate,
    ThaiIDType.address,
    ThaiIDType.issueDate,
    ThaiIDType.expireDate,
    ThaiIDType.laserID,
  ];
  List<String> selectedTypes = [];

  readCard({List<String> only = const []}) async {
    setState(() {
      _loading = true;
      _error = null;
      _data = null;
      _rawResponse = null;
    });
    try {
      var response = await ThaiIdcardReaderFlutter.read(only: only);
      setState(() {
        _data = response;
        _rawResponse = response.isError()
            ? 'code: ${response.code} | ${response.message}'
            : null;
        _loading = false;
      });
    } catch (e) {
      setState(() {
        _error = 'ERR readCard $e';
        _loading = false;
      });
    }
  }

  formattedDate(dt) {
    try {
      DateTime dateTime = DateTime.parse(dt);
      String formattedDate = DateFormat.yMMMMd('th_TH').format(dateTime);
      return formattedDate;
    } catch (e) {
      return dt.split('').toString() + e.toString();
    }
  }

  _clear() {
    setState(() {
      _data = null;
      _error = null;
      _rawResponse = null;
    });
  }

  bool get _showReadButton => _data == null && !_loading;

  @override
  Widget build(BuildContext context) {
    return MaterialApp(
      home: Scaffold(
        appBar: AppBar(
          title: const Text('Thai ID Card Reader'),
          actions: [
            if (_data != null)
              IconButton(
                icon: const Icon(Icons.refresh),
                onPressed: _clear,
                tooltip: 'Clear',
              ),
          ],
        ),
        body: SingleChildScrollView(
          child: Column(
            mainAxisAlignment: MainAxisAlignment.center,
            children: [
              if (_error != null)
                Padding(
                  padding: const EdgeInsets.all(8.0),
                  child: Text(
                    _error.toString(),
                    style: const TextStyle(color: Colors.red),
                  ),
                ),
              if (_rawResponse != null)
                Padding(
                  padding: const EdgeInsets.all(8.0),
                  child: Card(
                    color: Colors.orange.shade50,
                    child: Padding(
                      padding: const EdgeInsets.all(12.0),
                      child: Text(
                        _rawResponse!,
                        style: const TextStyle(color: Colors.deepOrange, fontSize: 14),
                      ),
                    ),
                  ),
                ),
              if (_showReadButton) ...[
                const EmptyHeader(
                  icon: Icons.credit_card,
                  text: 'เสียบบัตรประชาชนแล้วกดอ่าน',
                ),
                Padding(
                  padding: const EdgeInsets.symmetric(horizontal: 16.0),
                  child: SizedBox(
                    height: 220,
                    child: Wrap(children: [
                      Row(
                        mainAxisSize: MainAxisSize.min,
                        children: [
                          Checkbox(
                              value: selectedTypes.isEmpty,
                              onChanged: (val) {
                                setState(() {
                                  if (selectedTypes.isNotEmpty) {
                                    selectedTypes = [];
                                  }
                                });
                              }),
                          const Text('readAll'),
                        ],
                      ),
                      for (var ea in _idCardType)
                        Row(
                          mainAxisSize: MainAxisSize.min,
                          children: [
                            Checkbox(
                                value: selectedTypes.contains(ea),
                                onChanged: (val) {
                                  setState(() {
                                    if (selectedTypes.contains(ea)) {
                                      selectedTypes.remove(ea);
                                    } else {
                                      selectedTypes.add(ea);
                                    }
                                  });
                                }),
                            Text('$ea'),
                          ],
                        ),
                    ]),
                  ),
                ),
                Padding(
                  padding: const EdgeInsets.all(16.0),
                  child: ElevatedButton.icon(
                    icon: const Icon(Icons.credit_card),
                    label: const Text('อ่านบัตร', style: TextStyle(fontSize: 20)),
                    style: ElevatedButton.styleFrom(
                      minimumSize: const Size.fromHeight(56),
                    ),
                    onPressed: () => readCard(only: selectedTypes),
                  ),
                ),
              ],
              if (_loading)
                const Padding(
                  padding: EdgeInsets.all(32.0),
                  child: Column(
                    children: [
                      CircularProgressIndicator(),
                      SizedBox(height: 16),
                      Text('กำลังอ่านบัตร...', style: TextStyle(fontSize: 18)),
                    ],
                  ),
                ),
              if (_data != null) ...[
                const Padding(padding: EdgeInsets.all(8.0)),
                if (_data!.photo.isNotEmpty)
                  Center(
                    child: Image.memory(
                      Uint8List.fromList(_data!.photo),
                    ),
                  ),
                if (_data!.cid != null)
                  DisplayInfo(title: 'เลขบัตรประชาชน', value: _data!.cid!),
                if (_data!.firstnameTH != null)
                  DisplayInfo(
                      title: 'ชื่อ-นามสกุล (ภาษาไทย)',
                      value:
                          '${_data!.titleTH} ${_data!.firstnameTH} ${_data?.lastnameTH!}'),
                if (_data!.firstnameEN != null)
                  DisplayInfo(
                      title: 'ชื่อ-นามสกุล (ภาษาอังกฤษ)',
                      value:
                          '${_data!.titleEN} ${_data!.firstnameEN} ${_data!.lastnameEN}'),
                if (_data!.gender != null)
                  DisplayInfo(
                      title: 'เพศ',
                      value:
                          '(${_data!.gender}) ${_data!.gender == 1 ? 'ชาย' : 'หญิง'}'),
                if (_data!.birthdate != null)
                  DisplayInfo(
                      title: 'วันเดือนปีเกิด',
                      value:
                          '${_data!.birthdate.toString()}\n${formattedDate(_data!.birthdate)}'),
                if (_data!.address != null)
                  DisplayInfo(title: 'ที่อยู่', value: _data!.address!),
                if (_data!.issueDate != null)
                  DisplayInfo(
                      title: 'วันออกบัตร',
                      value:
                          '${_data!.issueDate.toString()}\n${formattedDate(_data!.issueDate)}'),
                if (_data!.expireDate != null)
                  DisplayInfo(
                      title: 'วันหมดอายุ',
                      value:
                          '${_data!.expireDate.toString()}\n${formattedDate(_data!.expireDate)}'),
                if (_data!.laserID != null)
                  DisplayInfo(title: 'Laser ID', value: _data!.laserID!),
              ],
            ],
          ),
        ),
      ),
    );
  }
}

class EmptyHeader extends StatelessWidget {
  final IconData? icon;
  final String? text;
  const EmptyHeader({
    this.icon,
    this.text,
    Key? key,
  }) : super(key: key);

  @override
  Widget build(BuildContext context) {
    return Card(
        child: SizedBox(
            height: 200,
            child: Column(
              mainAxisAlignment: MainAxisAlignment.center,
              children: [
                Icon(
                  icon ?? Icons.usb,
                  size: 60,
                ),
                Center(
                    child: Text(
                  text ?? 'Empty',
                  textAlign: TextAlign.center,
                  style: const TextStyle(
                    fontSize: 28,
                    fontWeight: FontWeight.bold,
                  ),
                )),
              ],
            )));
  }
}

class DisplayInfo extends StatelessWidget {
  const DisplayInfo({
    Key? key,
    required this.title,
    required this.value,
  }) : super(key: key);

  final String title;
  final String value;

  @override
  Widget build(BuildContext context) {
    TextStyle sTitle =
        const TextStyle(fontSize: 24, fontWeight: FontWeight.bold);
    TextStyle sVal = const TextStyle(fontSize: 28);

    _copyFn(value) {
      Clipboard.setData(ClipboardData(text: value)).then((_) {
        ScaffoldMessenger.of(context)
            .showSnackBar(const SnackBar(content: Text("Copy it already")));
      });
    }

    return Padding(
      padding: const EdgeInsets.all(8.0),
      child: Column(
        children: [
          Row(
            children: [
              Text(
                '$title : ',
                style: sTitle,
              ),
            ],
          ),
          Stack(
            alignment: Alignment.centerRight,
            children: [
              Row(
                children: [
                  Flexible(
                    child: Text(
                      value,
                      style: sVal,
                    ),
                  ),
                ],
              ),
              GestureDetector(
                onTap: () => _copyFn(value),
                child: const Icon(Icons.copy),
              )
            ],
          ),
          const Divider(
            color: Colors.black,
          ),
        ],
      ),
    );
  }
}

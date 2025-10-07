package com.example.thai_idcard_reader_flutter

import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.hardware.usb.*
import android.hardware.usb.UsbDevice
import android.hardware.usb.UsbManager
import android.os.Build
import android.util.Log
import androidx.annotation.NonNull
import androidx.core.content.ContextCompat
import com.acs.smartcard.Reader
import io.flutter.embedding.engine.plugins.FlutterPlugin
import io.flutter.plugin.common.EventChannel
import io.flutter.plugin.common.MethodCall
import io.flutter.plugin.common.MethodChannel
import io.flutter.plugin.common.MethodChannel.MethodCallHandler
import io.flutter.plugin.common.MethodChannel.Result
import java.nio.charset.*
import java.util.*
import org.json.JSONObject

const val ACTION_USB_PERMISSION = "com.example.thai_idcard_reader_flutter.USB_PERMISSION"
const val ACTION_USB_ATTACHED = "android.hardware.usb.action.USB_DEVICE_ATTACHED"
const val ACTION_USB_DETACHED = "android.hardware.usb.action.USB_DEVICE_DETACHED"
const val ACTION_USB_GRANTED = "android.hardware.usb.action.EXTRA_PERMISSION_GRANTED"

private fun pendingPermissionIntent(context: Context) =
    PendingIntent.getBroadcast(context, 0, Intent(ACTION_USB_PERMISSION), PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT)

/** ThaiIdcardReaderFlutterPlugin */
class ThaiIdcardReaderFlutterPlugin : FlutterPlugin, MethodCallHandler, EventChannel.StreamHandler {

  private lateinit var channel: MethodChannel

  private var usbEventChannel: EventChannel? = null

  private var readerEventChannel: EventChannel? = null
  private var eventSink: EventChannel.EventSink? = null

  private var applicationContext: Context? = null
  private var usbManager: UsbManager? = null

  // acs
  private var mReader: Reader? = null
  private var device: UsbDevice? = null

  private var readerStreamHandler: ReaderStream? = null

  private val usbReceiver: BroadcastReceiver =
      object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
          val action = intent.action
          val reader = mReader
          var dev: HashMap<String, Any?>?
          device = device ?: intent.getParcelableExtra(UsbManager.EXTRA_DEVICE)
          if (action == ACTION_USB_ATTACHED) {
            Log.e("ThaiIdcard", "ACTION_USB_ATTACHED")
            if (usbManager!!.hasPermission(device)) {
              Log.e("ThaiIdcard", "ACTION_USB_ATTACHED/hasPermission")
              dev = serializeDevice(device)
              dev["isAttached"] = true
              dev["hasPermission"] = true
              eventSink?.success(dev)
            } else {
              Log.e("ThaiIdcard", "ACTION_USB_ATTACHED/noPermission")
              usbManager?.requestPermission(device, pendingPermissionIntent(context))
              dev = serializeDevice(device)
              dev["isAttached"] = true
              dev["hasPermission"] = false
              eventSink?.success(dev)
            }
          } else if (action == ACTION_USB_DETACHED) {
            Log.e("ThaiIdcard", "ACTION_USB_DETACHED")
            reader?.close()
            dev = serializeDevice(device)
            dev["isAttached"] = false
            dev["hasPermission"] = false
            eventSink?.success(dev)
          } else if (action == ACTION_USB_PERMISSION) {
            Log.e("ThaiIdcard", "ACTION_USB_PERMISSION")
            if (usbManager!!.hasPermission(device)) {
              dev = serializeDevice(device)
              reader?.open(device)
              dev["isAttached"] = true
              dev["hasPermission"] = true
              eventSink?.success(dev)
              if (reader!!.isSupported(device)) {
                readerStreamHandler?.setReader(reader)
              }
            }
          }
        }
      }

  fun serializeDevice(device: UsbDevice?): HashMap<String, Any?> {
    val dev: HashMap<String, Any?> = HashMap()
    dev["identifier"] = device?.deviceName
    dev["vendorId"] = device?.vendorId
    dev["productId"] = device?.productId
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
      dev["manufacturerName"] = device?.manufacturerName
      dev["productName"] = device?.productName
      dev["interfaceCount"] = device?.interfaceCount
    }
    dev["deviceId"] = device?.deviceId
    return dev
  }

  override fun onListen(arguments: Any?, eventSink: EventChannel.EventSink?) {
    this.eventSink = eventSink
    usbManager?.deviceList?.values?.forEach { device ->
      if (mReader?.isSupported(device) ?: false) {
        this.device = device
        usbManager?.requestPermission(device, pendingPermissionIntent(applicationContext!!))
      }
    }
  }

  override fun onCancel(arguments: Any?) {
    mReader?.close()
    eventSink = null
    usbEventChannel = null
  }

  override fun onAttachedToEngine(
      @NonNull flutterPluginBinding: FlutterPlugin.FlutterPluginBinding
  ) {
    channel =
        MethodChannel(flutterPluginBinding.binaryMessenger, "thai_idcard_reader_flutter_channel")
    channel.setMethodCallHandler(this)
    applicationContext = flutterPluginBinding.applicationContext
    usbManager = applicationContext?.getSystemService(Context.USB_SERVICE) as UsbManager
    mReader = Reader(usbManager)

    val usbEventChannel = EventChannel(flutterPluginBinding.binaryMessenger, "usb_stream_channel")
    usbEventChannel.setStreamHandler(this)

    val readerEventChannel =
        EventChannel(flutterPluginBinding.binaryMessenger, "reader_stream_channel")
    readerStreamHandler = ReaderStream()
    readerEventChannel.setStreamHandler(readerStreamHandler)

    val filter = IntentFilter(ACTION_USB_PERMISSION)
    filter.addAction(ACTION_USB_DETACHED)
    filter.addAction(ACTION_USB_ATTACHED)
    ContextCompat.registerReceiver(
      applicationContext!!,
      usbReceiver,
      filter,
      ContextCompat.RECEIVER_EXPORTED
    )

    usbManager?.deviceList?.values?.forEach { device ->
      if (mReader?.isSupported(device) ?: false) {
        this.device = device
        mReader?.close()
        usbManager?.requestPermission(device, pendingPermissionIntent(applicationContext!!))
      }
    }
  }

  override fun onDetachedFromEngine(@NonNull binding: FlutterPlugin.FlutterPluginBinding) {
    applicationContext?.unregisterReceiver(usbReceiver)
    channel.setMethodCallHandler(null)
    mReader?.close()
    mReader = null
    usbManager = null
    applicationContext = null
    device = null
    usbEventChannel?.setStreamHandler(null)
    readerEventChannel?.setStreamHandler(null)
    readerStreamHandler = null
  }

  override fun onMethodCall(@NonNull call: MethodCall, @NonNull result: Result) {
    when (call.method) {
      "getPlatformVersion" -> {
        result.success("Android ${Build.VERSION.RELEASE}")
      }
      "readAll" -> {
        var apdu = ThaiADPU()
        val reader = mReader ?: return result.error("IllegalState", "mReader null", null)
        try {
          val res: Map<String, Any?> = apdu.readAll(reader)
          result.success(JSONObject(res).toString())
        } catch (e: Exception) {
          result.success("ERR/kt/readAll ${e.toString()}")
        }
      }
      "read" -> {
        var apdu = ThaiADPU()
        val selected = call.argument<List<String>>("selected")
        val selectedArray: Array<String> = selected!!.toTypedArray()
        val reader = mReader ?: return result.error("IllegalState", "mReader null", null)
        try {
          val res: Map<String, Any?> = apdu.readSpecific(reader, selectedArray)

          result.success(JSONObject(res).toString())
        } catch (e: Exception) {
          result.success("ERR/kt/read ${e.toString()}")
        }
      }
      "requestPermission" -> {
        val context =
            applicationContext
                ?: return result.error("IllegalState", "applicationContext null", null)
        val manager = usbManager ?: return result.error("IllegalState", "usbManager null", null)
        val identifier = call.argument<String>("identifier")
        val device = manager.deviceList[identifier]
        if (!manager.hasPermission(device)) {
          manager.requestPermission(device, pendingPermissionIntent(context))
        }
        result.success(null)
      }
      else -> result.notImplemented()
    }
  }
}

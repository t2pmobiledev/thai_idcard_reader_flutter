package com.example.thai_idcard_reader_flutter

import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.graphics.Bitmap
import android.hardware.usb.*
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.widget.Toast
import androidx.annotation.NonNull
import androidx.core.content.ContextCompat
import com.acs.smartcard.Reader
import com.t2pco.thaiidcard.SmartCardDevice
import com.t2pco.thaiidcard.ThaiSmartCard
import io.flutter.embedding.engine.plugins.FlutterPlugin
import io.flutter.plugin.common.EventChannel
import io.flutter.plugin.common.MethodCall
import io.flutter.plugin.common.MethodChannel
import io.flutter.plugin.common.MethodChannel.MethodCallHandler
import io.flutter.plugin.common.MethodChannel.Result
import org.json.JSONObject
import java.nio.charset.*
import java.util.*
import kotlin.concurrent.thread

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

  private var smartCardDevice: SmartCardDevice? = null

  private val usbReceiver: BroadcastReceiver =
      object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
          val action = intent.action
          if (action == ACTION_USB_ATTACHED) {
            Log.e("ThaiIdcard", "ACTION_USB_ATTACHED")
            getSmartCardDevice()
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
//    this.eventSink = eventSink
//    usbManager?.deviceList?.values?.forEach { device ->
//      if (mReader?.isSupported(device) ?: false) {
//        this.device = device
//        usbManager?.requestPermission(device, pendingPermissionIntent(applicationContext!!))
//      }
//    }
  }

  override fun onCancel(arguments: Any?) {
//    mReader?.close()
//    eventSink = null
//    usbEventChannel = null
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

    val filter = IntentFilter(ACTION_USB_ATTACHED)
    ContextCompat.registerReceiver(
      applicationContext!!,
      usbReceiver,
      filter,
      ContextCompat.RECEIVER_EXPORTED
    )
  }

  private fun getSmartCardDevice() {
    smartCardDevice = SmartCardDevice.getSmartCardDevice(
      applicationContext!!,
      "",
      object : SmartCardDevice.SmartCardDeviceEvent {

        override fun OnReady(device: SmartCardDevice?) {
          smartCardDevice = device
          if (mReader?.isSupported(device?.device) ?: false) {
            mReader?.open(device?.device)
          }
        }

        override fun OnDetached(device: SmartCardDevice?) {
          Log.d("SmartCard","Smart Card is removed")
          smartCardDevice = null
          mReader?.close()
        }
      })
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
      "getInfo" -> {
        readCardReader(result)
      }
      else -> result.notImplemented()
    }
  }

  private fun readCardReader(result: Result) {
    try {
      Toast.makeText(applicationContext, "version 21", Toast.LENGTH_SHORT).show()

      if (smartCardDevice == null) {
        getSmartCardDevice()

        if (smartCardDevice == null) {
          val response = HashMap<String, Any>()
          response.put("code", "004")
          response.put("message", "Smart Card device not found")
          result.success(JSONObject(response).toString())
        }
        return
      }

      val havePermission = smartCardDevice?.havePermission ?: false
      if (!havePermission) {
        smartCardDevice?.requestPermission()
        return
      }

      if (mReader?.isSupported(smartCardDevice?.device) ?: false) {
        try {
          var apdu = ThaiADPU()
          val res: HashMap<String, Any> = apdu.readAll(mReader!!)
          res.put("code", "000")
          res.put("message", "Success")
          result.success(JSONObject(res).toString())
        } catch (e: Exception) {
          val response = HashMap<String, Any>()
          response.put("code", "008")
          response.put("message", "${e.toString()}")
          result.success(JSONObject(response).toString())
        }
        return
      }

      val thaiSmartCard = ThaiSmartCard(smartCardDevice)

      if (!thaiSmartCard.isInserted) {
        Log.d("SmartCard","Smart Card not found")
        val response = HashMap<String, Any>()
        response.put("code", "001")
        response.put("message", "Smart Card not found")
        result.success(JSONObject(response).toString())
        return
      }

      thread(start = true) {
        try {

          val info: ThaiSmartCard.PersonalInformation? = thaiSmartCard.getPersonalInformation()
          if (info == null) {
            Log.d("SmartCard","Read Smart Card information failed")
            val response = HashMap<String, Any>()
            response.put("code", "002")
            response.put("message", "Read Smart Card information failed")
            result.success(JSONObject(response).toString())
            return@thread
          }

          val personalPic: Bitmap? = thaiSmartCard.getPersonalPicture()

          if (personalPic == null) {
            Log.d("SmartCard","Read Smart Card personal picture failed")
            val response = HashMap<String, Any>()
            response.put("code", "003")
            response.put("message", "Read Smart Card personal picture failed")
            result.success(JSONObject(response).toString())
            return@thread
          }

          val response = HashMap<String, Any>()
          response.put("code", "000")
          response.put("message", "Success")
          response.put("cid", info.PersonalID)
          response.put("nameTH", info.NameTH)
          response.put("nameEN", info.NameEN)
          response.put("birthdate", info.BirthDate)
          response.put("gender", info.Gender)
          response.put("address", info.Address)
          response.put("cardIssuer", info.Issuer)
          response.put("issueDate", info.IssueDate)
          response.put("expireDate", info.ExpireDate)
          response.put("photo", thaiSmartCard.bytePersonalPicture)
          result.success(JSONObject(response).toString())
          Log.d("SmartCard","Read Smart Card Success")
        } catch (e: Exception) {
          val response = HashMap<String, Any>()
          response.put("code", "007")
          response.put("message", "${e.toString()}")
          result.success(JSONObject(response).toString())
        }
      }
    } catch (e: Exception) {
      val response = HashMap<String, Any>()
      response.put("code", "005")
      response.put("message", "${e.toString()}")
      result.success(JSONObject(response).toString())
    }
  }
}

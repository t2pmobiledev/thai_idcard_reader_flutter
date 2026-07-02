package com.example.thai_idcard_reader_flutter

import android.content.Context
import android.graphics.Bitmap
import android.hardware.usb.*
import android.os.Build
import android.util.Log
import androidx.annotation.NonNull
import com.acs.smartcard.Reader
import com.t2pco.thaiidcard.SmartCardDevice
import com.t2pco.thaiidcard.ThaiSmartCard
import io.flutter.embedding.engine.plugins.FlutterPlugin
import io.flutter.plugin.common.MethodCall
import io.flutter.plugin.common.MethodChannel
import io.flutter.plugin.common.MethodChannel.MethodCallHandler
import io.flutter.plugin.common.MethodChannel.Result
import org.json.JSONObject
import java.nio.charset.*
import java.util.*
import kotlin.concurrent.thread

/** ThaiIdcardReaderFlutterPlugin */
class ThaiIdcardReaderFlutterPlugin : FlutterPlugin, MethodCallHandler {

  private lateinit var channel: MethodChannel

  private var applicationContext: Context? = null
  private var usbManager: UsbManager? = null

  // acs
  private var mReader: Reader? = null
  private var device: UsbDevice? = null

  private var smartCardDevice: SmartCardDevice? = null

  private var vendorSdkInfo: HashMap<String, Any>? = null

  private var retryCount: Int = 0;

  // Card state listener
  private val cardStateListener = Reader.OnStateChangeListener { slotNum, prevState, currState ->
    Log.d("ThaiIdcard", "Slot state changed")

    when {
      currState > prevState -> {
        // Card inserted
        Log.d("ThaiIdcard", "Card inserted")
        onCardInserted(slotNum)
      }
      currState < prevState -> {
        // Card removed
        Log.d("ThaiIdcard", "Card removed")
        onCardRemoved(slotNum)
      }
    }
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
  }

  private fun getSmartCardDevice() {
    smartCardDevice = SmartCardDevice.getSmartCardDevice(
      applicationContext!!,
      "",
      object : SmartCardDevice.SmartCardDeviceEvent {

        override fun OnReady(device: SmartCardDevice?) {
          smartCardDevice = device
          if (mReader?.isSupported(device?.device) ?: false) {
            try {
              mReader?.open(device?.device)
              mReader?.setOnStateChangeListener(cardStateListener)
            } catch (e: Exception) {
              Log.w("ThaiIdcard", "Cannot open reader: ${e.toString()}")
            }
          }
        }

        override fun OnDetached(device: SmartCardDevice?) {
          Log.d("SmartCard","Smart Card is removed")
          smartCardDevice = null
          mReader?.setOnStateChangeListener(null)
          mReader?.close()
        }
      })
  }

  private fun onCardInserted(slotNum: Int) {
    Log.d("ThaiIdcard", "Processing card insertion in slot")
    if (mReader?.isSupported(smartCardDevice?.device) ?: false) {
      try {
        val apdu = ThaiADPU()
        val res: HashMap<String, Any> = apdu.readAll(mReader!!)
        res.put("code", "000")
        res.put("message", "Success")
        vendorSdkInfo = res
      } catch (e: Exception) {
        vendorSdkInfo = null
      }
    }
  }

  private fun onCardRemoved(slotNum: Int) {
    Log.d("ThaiIdcard", "Processing card removal from slot")
    vendorSdkInfo = null
  }

  override fun onDetachedFromEngine(@NonNull binding: FlutterPlugin.FlutterPluginBinding) {
    channel.setMethodCallHandler(null)
    mReader?.setOnStateChangeListener(null)
    mReader?.close()
    mReader = null
    usbManager = null
    applicationContext = null
    device = null
  }

  override fun onMethodCall(@NonNull call: MethodCall, @NonNull result: Result) {
    when (call.method) {
      "getPlatformVersion" -> {
        result.success("Android ${Build.VERSION.RELEASE}")
      }
      "getInfo" -> {
        retryCount = 0;
        readCardReader(result)
      }
      else -> result.notImplemented()
    }
  }

  private fun readCardReader(result: Result) {
    try {
      // Find the device and open it. When launched via USB_DEVICE_ATTACHED intent-filter,
      // Android has already granted permission before the app starts, so
      // SmartCardDevice.start() will call openDeviceWithPermission() directly
      // without showing any dialog.
      if (smartCardDevice == null) {
        smartCardDevice = SmartCardDevice.getSmartCardDevice(
          applicationContext!!,
          "",
          object : SmartCardDevice.SmartCardDeviceEvent {
            override fun OnReady(device: SmartCardDevice?) {
              smartCardDevice = device
              if (mReader?.isSupported(device?.device) ?: false) {
                try {
                  mReader?.open(device?.device)
                  mReader?.setOnStateChangeListener(cardStateListener)
                } catch (e: Exception) {
                  Log.w("ThaiIdcard", "Cannot open reader: ${e.toString()}")
                }
              }
            }
            override fun OnDetached(device: SmartCardDevice?) {
              smartCardDevice = null
              mReader?.setOnStateChangeListener(null)
              mReader?.close()
            }
          })
      }

      if (smartCardDevice == null) {
        val response = HashMap<String, Any>()
        response.put("code", "004")
        response.put("message", "Smart Card device not found")
        result.success(JSONObject(response).toString())
        return
      }

      // If permission has not been granted yet (app was opened manually, not via USB intent),
      // return a "permission required" response. The user should plug in the reader while
      // the app is closed so Android shows the combined permission+open dialog,
      // or grant permission via system settings.
      if (!smartCardDevice!!.havePermission) {
        val response = HashMap<String, Any>()
        response.put("code", "009")
        response.put("message", "USB permission not granted. Please replug the reader.")
        result.success(JSONObject(response).toString())
        return
      }

      // Open ACS reader if supported but not yet opened
      if ((mReader?.isSupported(smartCardDevice!!.device) ?: false) && !(mReader?.isOpened ?: false)) {
        try {
          mReader?.open(smartCardDevice!!.device)
          mReader?.setOnStateChangeListener(cardStateListener)
        } catch (e: Exception) {
          Log.w("ThaiIdcard", "Cannot open reader: ${e.toString()}")
        }
      }

      thread(start = true) {
        try {
          if ((mReader?.isSupported(smartCardDevice?.device) ?: false) && (mReader?.isOpened ?: false)) {
            try {
              if (vendorSdkInfo != null) {
                result.success(JSONObject(vendorSdkInfo).toString())
                return@thread
              }
              Thread.sleep(2000)
              val apdu = ThaiADPU()
              val res: HashMap<String, Any> = apdu.readAll(mReader!!)
              res.put("code", "000")
              res.put("message", "Success")
              vendorSdkInfo = res
              result.success(JSONObject(vendorSdkInfo).toString())
            } catch (e: Exception) {
              val response = HashMap<String, Any>()
              response.put("code", "008")
              response.put("message", "${e.toString()}")
              result.success(JSONObject(response).toString())
            }
            return@thread
          }

          val thaiSmartCard = ThaiSmartCard(smartCardDevice)

          if (!thaiSmartCard.isInserted()) {
            val response = HashMap<String, Any>()
            response.put("code", "001")
            response.put("message", "Smart Card not found")
            result.success(JSONObject(response).toString())
            return@thread
          }

          val info: ThaiSmartCard.PersonalInformation? = thaiSmartCard.getPersonalInformation()
          if (info == null) {
            val response = HashMap<String, Any>()
            response.put("code", "002")
            response.put("message", "Read Smart Card information failed")
            result.success(JSONObject(response).toString())
            return@thread
          }

          val personalPic: Bitmap? = thaiSmartCard.getPersonalPicture()

          if (personalPic == null) {
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
          try {
            val adm = thaiSmartCard.getChipCardADM()
            val laserID: String? = adm?.LaserNumber
                ?.trimEnd('\u0000', ' ')
                ?.takeIf { it.isNotEmpty() }
            if (laserID != null) {
              response["laserID"] = laserID
            }
          } catch (_: Exception) {
            // laserID absent from response → Flutter receives null
          }
          result.success(JSONObject(response).toString())
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

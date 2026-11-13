package com.example.thai_idcard_reader_flutter

import android.content.Context
import android.graphics.Bitmap
import android.hardware.usb.*
import android.os.Build
import android.os.Handler
import android.os.Looper
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

  // getInfo call waiting for the user to answer the USB permission dialog.
  private var pendingPermissionResult: Result? = null
  private val mainHandler = Handler(Looper.getMainLooper())

  companion object {
    private const val PERMISSION_TIMEOUT_MS = 30_000L
  }

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
    mainHandler.removeCallbacks(permissionTimeoutRunnable)
    pendingPermissionResult = null
    smartCardDevice?.stop()
    smartCardDevice = null
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
        vendorSdkInfo = null
        readCardReader(result)
      }
      else -> result.notImplemented()
    }
  }

  private fun respond(result: Result, code: String, message: String) {
    val response = HashMap<String, Any>()
    response.put("code", code)
    response.put("message", message)
    result.success(JSONObject(response).toString())
  }

  /** Takes the getInfo call waiting for the USB permission dialog (if any) and stops its timeout. */
  private fun takePendingPermissionResult(): Result? {
    val pending = pendingPermissionResult ?: return null
    pendingPermissionResult = null
    mainHandler.removeCallbacks(permissionTimeoutRunnable)
    return pending
  }

  private val permissionTimeoutRunnable = Runnable {
    takePendingPermissionResult()?.let {
      respond(it, "009", "USB permission not granted. Please allow access to the card reader.")
    }
  }

  private val smartCardDeviceEvent = object : SmartCardDevice.SmartCardDeviceEvent {
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
      // Permission granted while a getInfo call was waiting — continue reading.
      takePendingPermissionResult()?.let { performRead(it) }
    }

    override fun OnDetached(device: SmartCardDevice?) {
      smartCardDevice = null
      mReader?.setOnStateChangeListener(null)
      mReader?.close()
      takePendingPermissionResult()?.let {
        respond(it, "004", "Smart Card device not found")
      }
    }

    override fun OnPermissionDenied(device: SmartCardDevice?) {
      takePendingPermissionResult()?.let {
        respond(it, "010", "USB permission denied")
      }
    }
  }

  private fun readCardReader(result: Result) {
    try {
      // The app no longer declares a USB_DEVICE_ATTACHED intent-filter, so Android does not
      // grant permission on plug-in (and offers no "use by default" option). Permission is
      // requested here at read time and is valid until the reader is unplugged.
      if (smartCardDevice == null) {
        smartCardDevice = SmartCardDevice.getSmartCardDevice(applicationContext!!, "", smartCardDeviceEvent)
      }

      if (smartCardDevice == null) {
        respond(result, "004", "Smart Card device not found")
        return
      }

      if (!smartCardDevice!!.havePermission) {
        if (pendingPermissionResult != null) {
          // A previous getInfo call is already waiting for the dialog.
          respond(result, "009", "USB permission is being requested. Please allow access to the card reader.")
          return
        }
        pendingPermissionResult = result
        mainHandler.postDelayed(permissionTimeoutRunnable, PERMISSION_TIMEOUT_MS)
        // Shows the system dialog (no-op if a request is already in flight).
        // If permission is already granted, OnReady fires synchronously and resolves the call.
        smartCardDevice!!.requestPermission()
        return
      }

      performRead(result)
    } catch (e: Exception) {
      takePendingPermissionResult()
      respond(result, "005", "${e.toString()}")
    }
  }

  private fun performRead(result: Result) {
    try {
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

          // Read chipNo FIRST — before selecting Storage Applet.
          // The chip responds to GET DATA (80 CA 9F 7F) in its default state
          // right after card insertion without needing an applet selection.
          var chipId: String? = null
          try {
            chipId = thaiSmartCard.getCardID()
                ?.trimEnd('\u0000', ' ')
                ?.takeIf { it.isNotEmpty() && !it.all { c -> c == '0' } }
            if (chipId == null) {
              // Retry once
              chipId = thaiSmartCard.getCardID()
                  ?.trimEnd('\u0000', ' ')
                  ?.takeIf { it.isNotEmpty() && !it.all { c -> c == '0' } }
            }
          } catch (_: Exception) {}

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
          if (chipId != null) {
            response["chipNo"] = chipId
          }
          try {
            val bp1: String? = thaiSmartCard.getBP1No()
                ?.trimEnd('\u0000', ' ')
                ?.takeIf { it.isNotEmpty() }
            if (bp1 != null) {
              response["bp1No"] = bp1
            }
          } catch (_: Exception) {}
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

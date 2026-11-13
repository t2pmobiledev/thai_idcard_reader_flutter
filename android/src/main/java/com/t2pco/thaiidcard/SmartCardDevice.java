package com.t2pco.thaiidcard;

import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.hardware.usb.UsbDevice;
import android.hardware.usb.UsbDeviceConnection;
import android.hardware.usb.UsbEndpoint;
import android.hardware.usb.UsbInterface;
import android.hardware.usb.UsbManager;
import android.util.Log;
import androidx.core.content.ContextCompat;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.HashMap;

public class SmartCardDevice {
    private static final String ACTION_USB_PERMISSION = "ninkoman.smartcardreader.USB_PERMISSION";
    private static final String TAG = "SmartCardDevice";

    private Context context;
    public UsbDevice device;
    private SmartCardMessage message;
    private PendingIntent mPermissionIntent;
    public boolean havePermission = false;
    private UsbDeviceConnection deviceConnection = null;
    private UsbInterface deviceInterface = null;
    private UsbEndpoint inputEndpoint = null;
    private UsbEndpoint outputEndpoint = null;

    private int infIndex = 0;
    private int endpointInputIndex = 0;
    private int endpointOutputIndex = 0;
    private boolean stopped = true;
    private boolean started = false;
    private boolean deviceDetachedRegister = false;
    private boolean permissionRequested = false;
    private boolean permissionReceiverRegistered = false;

    private SmartCardDeviceEvent eventCallback = null;

    public SmartCardDevice(Context context, UsbDevice device, int infIndex, int endpointInputIndex, int endpointOutputIndex, SmartCardDeviceEvent eventCallback) {
        if (context == null || device == null) {
            throw new NullPointerException();
        }

        this.context = context;
        this.device = device;
        this.message = new SmartCardMessage();

        this.infIndex = infIndex;
        this.endpointInputIndex = endpointInputIndex;
        this.endpointOutputIndex = endpointOutputIndex;
        this.eventCallback = eventCallback;

        // Runtime permission request via broadcast. The intent is explicit (setPackage) and the
        // receiver is not exported, so only the system / this app can deliver the result.
        // The grant result is read back with UsbManager.hasPermission() instead of intent extras,
        // because FLAG_IMMUTABLE prevents the system from filling EXTRA_PERMISSION_GRANTED.
        Intent permIntent = new Intent(ACTION_USB_PERMISSION);
        permIntent.setPackage(context.getPackageName());
        mPermissionIntent = PendingIntent.getBroadcast(context, 0, permIntent, PendingIntent.FLAG_IMMUTABLE | PendingIntent.FLAG_UPDATE_CURRENT);
    }

    private void registerPermissionReceiver() {
        if (this.permissionReceiverRegistered) {
            return;
        }
        IntentFilter filter = new IntentFilter(ACTION_USB_PERMISSION);
        ContextCompat.registerReceiver(this.context, this.mUsbPermissionReceiver, filter, ContextCompat.RECEIVER_NOT_EXPORTED);
        this.permissionReceiverRegistered = true;
    }

    private void unregisterPermissionReceiver() {
        if (!this.permissionReceiverRegistered) {
            return;
        }
        try {
            this.context.unregisterReceiver(this.mUsbPermissionReceiver);
        } catch (IllegalArgumentException e) {
            Log.w(TAG, "Permission receiver was not registered");
        }
        this.permissionReceiverRegistered = false;
    }

    private void requestUsbPermission(UsbManager manager) {
        Log.d(TAG, "Start request permission");
        registerPermissionReceiver();
        this.permissionRequested = true;
        manager.requestPermission(device, mPermissionIntent);
    }

    @Override
    protected void finalize() throws Throwable {
        super.finalize();
        this.stop();
    }

    public interface SmartCardDeviceEvent {
        void OnReady(SmartCardDevice device);
        void OnDetached(SmartCardDevice device);
        /** Called when the user denies (or dismisses) the USB permission dialog. */
        default void OnPermissionDenied(SmartCardDevice device) {}
    }

    public static SmartCardDevice getSmartCardDevice(Context context, String nameContain, int infIndex, int endpointInputIndex, int endpointOutputIndex, SmartCardDeviceEvent eventCallback) {
        UsbManager manager;
        HashMap<String, UsbDevice> deviceList;
        UsbDevice device = null;
        SmartCardDevice cardDevice = null;

        manager = (UsbManager)context.getSystemService(Context.USB_SERVICE);
        if (manager == null) {
            Log.w(TAG, "USB manager not found");
            return null;
        }

        deviceList = manager.getDeviceList();
        if (deviceList == null) {
            Log.w(TAG, "USB device list not found");
            return null;
        }

        for (String key : deviceList.keySet()) {
//            Log.d(TAG, "Search device contain name [" + nameContain + "] with [" + deviceList.get(key).getProductName() + "] [" + deviceList.get(key).getDeviceName() + "]");
            if (deviceList.get(key).getProductName().contains(nameContain)) {
                device = deviceList.get(key);
//                Log.d(TAG, "Found device: " + device.getProductName());
                break;
            }
        }

        if (device == null) {
//            Log.w(TAG, "Device name [" + nameContain + "] not found");
            return null;
        }

        cardDevice = new SmartCardDevice(context, device, infIndex, endpointInputIndex, endpointOutputIndex, eventCallback);

        cardDevice.start();

        return cardDevice;
    }

    public static SmartCardDevice getSmartCardDevice(Context context, String nameContain, SmartCardDeviceEvent eventCallback) {
        UsbManager manager;
        HashMap<String, UsbDevice> deviceList;
        UsbDevice device = null;
        SmartCardDevice cardDevice = null;

        manager = (UsbManager)context.getSystemService(Context.USB_SERVICE);
        if (manager == null) {
            Log.w(TAG, "USB manager not found");
            return null;
        }

        deviceList = manager.getDeviceList();
        if (deviceList == null || deviceList.isEmpty()) {
            Log.w(TAG, "USB device list not found");
            return null;
        }


        for (String key : deviceList.keySet()) {
            if (nameContain.isEmpty()) {
                device = deviceList.get(key);
                break;
            }
            if (deviceList.get(key).getProductName().contains(nameContain)) {
                device = deviceList.get(key);
                break;
            }
        }

        if (device == null) {
//            Log.w(TAG, "Device name [" + nameContain + "] not found");
            return null;
        }

        UsbInterface intf = device.getInterface(0);
        int[] endpointIndex = new int[2];
        int index = 0;

        for (int i = 0; i < intf.getEndpointCount(); i++) {
            if (intf.getEndpoint(i).getAttributes() == 2) {
                endpointIndex[index] = i;
                index++;

                if (index >= 2) {
                    break;
                }
            }
        }
        if (index < 2) {
            Log.d(TAG, "Smart Card device endpoint detect failed");
            return null;
        }

        cardDevice = new SmartCardDevice(context, device, 0, endpointIndex[0], endpointIndex[1], eventCallback);

        cardDevice.start();

        return cardDevice;
    }

    public void start() {
        UsbManager manager = (UsbManager)this.context.getSystemService(Context.USB_SERVICE);
        if (this.started) {
            return;
        }
        this.started = true;

        if (manager != null) {
            // If permission was already granted in this session, skip the dialog and go
            // straight to OnReady.
            if (manager.hasPermission(device)) {
                Log.d(TAG, "Permission already granted, opening device directly");
                openDeviceWithPermission(manager);
                return;
            }
            if (!this.havePermission && !this.permissionRequested) {
                requestUsbPermission(manager);
            }
        }
    }

    public boolean isPermissionRequested() {
        return this.permissionRequested;
    }

    public void requestPermission() {
        UsbManager manager = (UsbManager)this.context.getSystemService(Context.USB_SERVICE);
        if (manager != null) {
            // If permission was already granted, open device directly without showing dialog.
            if (manager.hasPermission(device)) {
                Log.d(TAG, "Permission already granted, opening device directly");
                openDeviceWithPermission(manager);
                return;
            }
            if (!this.havePermission && !this.permissionRequested) {
                requestUsbPermission(manager);
            } else {
                Log.d(TAG, "Permission already granted or already requested, skipping");
            }
        }
    }

    private void openDeviceWithPermission(UsbManager manager) {
        // Permission flow is finished (granted) — the receiver is no longer needed.
        unregisterPermissionReceiver();
        this.permissionRequested = false;
        try {
            UsbInterface intf = device.getInterface(infIndex);
            UsbEndpoint ep0 = (intf == null) ? null : intf.getEndpoint(endpointInputIndex);
            UsbEndpoint ep1 = (intf == null) ? null : intf.getEndpoint(endpointOutputIndex);
            if (intf == null || ep0 == null || ep1 == null) {
                Log.d(TAG, "openDeviceWithPermission: invalid USB interface or endpoint");
                if (this.eventCallback != null) {
                    this.eventCallback.OnDetached(this);
                }
                return;
            }

            UsbDeviceConnection conn = manager.openDevice(device);
            if (conn == null) {
                Log.d(TAG, "openDeviceWithPermission: openDevice returned null");
                if (this.eventCallback != null) {
                    this.eventCallback.OnDetached(this);
                }
                return;
            }

            this.deviceConnection = conn;
            this.deviceInterface = intf;

            if (ep0.getDirection() == android.hardware.usb.UsbConstants.USB_DIR_IN) {
                this.inputEndpoint = ep0;
                this.outputEndpoint = ep1;
            } else {
                this.inputEndpoint = ep1;
                this.outputEndpoint = ep0;
            }

            this.havePermission = true;
            this.stopped = false;
            this.permissionRequested = false;

            if (this.eventCallback != null) {
                this.eventCallback.OnReady(this);
            }

            if (!this.deviceDetachedRegister) {
                IntentFilter filter = new IntentFilter(UsbManager.ACTION_USB_DEVICE_DETACHED);
                this.context.registerReceiver(this.mUsbDetachedReceiver, filter);
                this.deviceDetachedRegister = true;
            }
        } catch (Exception e) {
            Log.e(TAG, "openDeviceWithPermission failed: " + e.getMessage(), e);
            if (this.eventCallback != null) {
                this.eventCallback.OnDetached(this);
            }
        }
    }

    public void stop() {
        if (!this.stopped) {
            //this.deviceConnection.close();
            this.stopped = true;
        }
        this.started = false;
        this.havePermission = false;
        this.permissionRequested = false;
        unregisterPermissionReceiver();
    }

    public String getDeviceProductName() {
        if (this.device != null) {
            return this.device.getProductName();
        }

        return "";
    }

    public boolean reset() {
        SmartCardMessage.DataBlock dataBlock;
        byte[] data;
        byte[] message = this.message.getMessageSlotReset();

        if (this.havePermission) {
            if (!this.prepareConnection()) {
                Log.w(TAG, "prepareConnection() failed");
                return false;
            }

            if (!this.sendRequestMessage(message)) {
                Log.w(TAG, "sendRequestMessage() error");
                return false;
            }

            try {
                if ((data = this.receiveResponseMessage()) == null) {
                    Log.w(TAG, "receiveResponseMessage() error");
                    return false;
                }
            } catch (IOException e) {
                Log.w(TAG, "receiveResponseMessage() failed");
                return false;
            }

            dataBlock = this.message.parseDataBlock(data);
            if (dataBlock != null && dataBlock.data != null) {
                StringBuilder sb = new StringBuilder();

                for (byte b: dataBlock.data) {
                    sb.append(String.format("%02x", b));
                }

//                Log.d(TAG, "Card reset response data[" + sb.toString() + "]");

                if (dataBlock.status != 0 || dataBlock.error != 0) {
//                    Log.w(TAG, "Card reset return abnormal status " + dataBlock.status + ":" + dataBlock.error);
                    return false;
                }

                if (dataBlock.data[0] != (byte)0x82) {
//                    Log.w(TAG, String.format("Card reset return abnormal code 0x%02x", dataBlock.data[0]));
                    return false;
                }

                return true;
            } else {
                Log.w(TAG, "Card reset return fail");
                return false;
            }
        }

        return false;
    }

    public boolean isStarted() {
        return !this.started;
    }

    //public void setMessageTypeT1(boolean isT1) {
    //    this.message.setMessageTypeT1(isT1);
    //}

    private final BroadcastReceiver mUsbPermissionReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (!ACTION_USB_PERMISSION.equals(intent.getAction())) {
                return;
            }
            Log.d(TAG, "USB permission broadcast received");
            synchronized (this) {
                try {
                    UsbManager manager = (UsbManager)context.getSystemService(Context.USB_SERVICE);
                    if (manager == null) {
                        Log.d(TAG, "USB manager not found");
                        return;
                    }

                    // EXTRA_DEVICE / EXTRA_PERMISSION_GRANTED are not filled into an immutable
                    // PendingIntent, so rely on our own device and UsbManager.hasPermission().
                    if (manager.hasPermission(SmartCardDevice.this.device)) {
                        openDeviceWithPermission(manager);
                    } else {
                        Log.d(TAG, "USB permission denied");
                        unregisterPermissionReceiver();
                        SmartCardDevice.this.permissionRequested = false;
                        SmartCardDevice.this.havePermission = false;
                        if (SmartCardDevice.this.eventCallback != null) {
                            SmartCardDevice.this.eventCallback.OnPermissionDenied(SmartCardDevice.this);
                        }
                    }
                } catch (Exception e) {
                    Log.e(TAG, "Error handling USB permission broadcast: " + e.getMessage(), e);
                }
            }
        }
    };

    private final BroadcastReceiver mUsbDetachedReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (action != null && action.equals(UsbManager.ACTION_USB_DEVICE_DETACHED)) {
                synchronized (this) {
                    UsbDevice device = intent.getParcelableExtra(UsbManager.EXTRA_DEVICE);

                    Log.d(TAG, "USB device is detached");

                    if (device.getDeviceName().equals(SmartCardDevice.this.device.getDeviceName())) {
                        if (SmartCardDevice.this.eventCallback != null)
                            SmartCardDevice.this.eventCallback.OnDetached(SmartCardDevice.this);
                        SmartCardDevice.this.stop();

                        SmartCardDevice.this.context.unregisterReceiver(SmartCardDevice.this.mUsbDetachedReceiver);
                        SmartCardDevice.this.deviceDetachedRegister = false;
                    }
                }
            }
        }
    };

    public SmartCardMessage.DataBlock getATR() {
        if (!this.havePermission) {
            Log.w(TAG, "USB permission require, please call start() first");
            return null;
        }

        byte[] message = this.message.getMessageIccPowerOn();

        if (!this.prepareConnection()) {
            Log.w(TAG, "prepareConnection() failed");
            return null;
        }

        if (!this.sendRequestMessage(message)) {
            Log.w(TAG, "sendRequestMessage() failed");
            return null;
        }

        try {
            if ((message = this.receiveResponseMessage()) == null) {
                Log.w(TAG, "receiveResponseMessage() error");
                return null;
            }
        } catch (IOException e) {
            Log.w(TAG, "receiveResponseMessage() failed: " + e.getMessage());
            return null;
        }

        return this.message.parseDataBlock(message);
    }

    public SmartCardMessage.DataBlock sendAPDU(byte[] dataAPDU) {
        byte[] data;

        if (!this.havePermission) {
            Log.w(TAG, "USB permission require, please call start() first");
            return null;
        }

        byte[][] messages = this.message.getMessageXfrBlock(dataAPDU);

        if (messages == null) {
            Log.w(TAG, "getMessageXfrBlock() return failed");
            return null;
        }

        if (!this.prepareConnection()) {
            Log.w(TAG, "prepareConnection() failed");
            return null;
        }

        for (byte[] message: messages) {
            if (!this.sendRequestMessage(message)) {
                Log.w(TAG, "sendRequestMessage() error");
                return null;
            }
        }

        try {
            if ((data = this.receiveResponseMessage()) == null) {
                Log.w(TAG, "receiveResponseMessage() failed");
                return null;
            }
        } catch (IOException e) {
            Log.w(TAG, "receiveResponseMessage() error");
            return null;
        }

        SmartCardMessage.DataBlock dataBlock = this.message.parseDataBlock(data);
        if (dataBlock != null && dataBlock.data != null) {
            StringBuilder sb = new StringBuilder();

            for (byte b: dataBlock.data) {
                sb.append(String.format("%02x", b));
            }

//            Log.d(TAG, "Response data[" + sb.toString() + "]");
        }

        return dataBlock;
    }

    public SmartCardMessage.EscapeResponseBlock sendEscapeCommand(byte[] dataEscape) {
        byte[] data;

        if (!this.havePermission) {
            Log.w(TAG, "USB permission require, please call start() first");
            return null;
        }

        byte[] message = this.message.getMessageEscapeRequest(dataEscape);

        if (message == null) {
            Log.w(TAG, "getMessageEscapeRequest() return failed");
            return null;
        }

        if (!this.prepareConnection()) {
            Log.w(TAG, "prepareConnection() failed");
            return null;
        }

        if (!this.sendRequestMessage(message)) {
            Log.w(TAG, "sendRequestMessage() error");
            return null;
        }

        try {
            if ((data = this.receiveResponseMessage()) == null) {
                Log.w(TAG, "receiveResponseMessage() failed");
                return null;
            }
        } catch (IOException e) {
            Log.w(TAG, "receiveResponseMessage() error");
            return null;
        }

        SmartCardMessage.EscapeResponseBlock response = this.message.parseEscapeResponseBlock(data);

        if (response != null && response.data != null) {
            StringBuilder sb = new StringBuilder();

            for (byte b: response.data) {
                sb.append(String.format("%02x", b));
            }

//            Log.d(TAG, "Response escape data [" + sb.toString() + "]");
        }

        return response;
    }

    private boolean prepareConnection() {
        if (!this.havePermission) {
            Log.w(TAG, "USB permission require, please call start() first");
            return false;
        }

        this.deviceConnection.claimInterface(this.deviceInterface, true);

        return true;
    }

    private boolean sendRequestMessage(byte[] message) {
        int length;

        if (!this.havePermission) {
            Log.w(TAG, "USB permission require, please call start() first");
            return false;
        }

        if (message == null || message.length == 0) {
            Log.w(TAG, "message is null or invalid length");
            return false;
        }

        length = this.deviceConnection.bulkTransfer(this.outputEndpoint, message, message.length, 800);

        if (length <= 0 || length != message.length) {
            Log.w(TAG, "Send failed: returned " + length + "/" + message.length + " bytes");
            return false;
        }

        return true;
    }

    private byte[] receiveResponseMessage() throws IOException {
        int length, totalLength;
        byte[] buffer;
        ByteArrayOutputStream dataStream = new ByteArrayOutputStream();

        if (!this.havePermission) {
            Log.w(TAG, "USB permission require, please call start() first");
            return null;
        }

        buffer = new byte[this.inputEndpoint.getMaxPacketSize()];

        length = this.deviceConnection.bulkTransfer(this.inputEndpoint, buffer, buffer.length, 1500);

        if (length < 10) {
            Log.w(TAG, "receive message invalid length " + length);
            return null;
        }

        dataStream = new ByteArrayOutputStream();
        dataStream.write(buffer, 0, length);

        totalLength = (((int)buffer[4] & 0xff) << 24) | (((int)buffer[3] & 0xff) << 16) | (((int)buffer[2] & 0xff) << 8) | ((int)buffer[1] & 0xff);
        totalLength += 10;

        while (dataStream.size() < totalLength) {
            length = this.deviceConnection.bulkTransfer(this.inputEndpoint, buffer, buffer.length, 500);
            if (length < 0) {
                Log.w(TAG, "receive continues message return error");
                dataStream.close();
                return null;
            } else if (length == 0) {
                if (dataStream.size() < totalLength) {
                    Log.w(TAG, "receive continues message not success");
                    dataStream.close();
                    return null;
                } else {
                    break;
                }
            }

            dataStream.write(buffer, 0, length);
        }

        buffer = dataStream.toByteArray();
        dataStream.close();

        return buffer;
    }
}

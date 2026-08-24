package com.example.thai_idcard_reader_flutter

import android.util.Log
import com.acs.smartcard.Reader
import java.io.ByteArrayOutputStream
import java.nio.charset.Charset

class ThaiADPU {
        val select =
                        byteArrayOf(
                                        0x00.toByte(),
                                        0xA4.toByte(),
                                        0x04.toByte(),
                                        0x00.toByte(),
                                        0x08.toByte(),
                                        0xA0.toByte(),
                                        0x00.toByte(),
                                        0x00.toByte(),
                                        0x00.toByte(),
                                        0x54.toByte(),
                                        0x48.toByte(),
                                        0x00.toByte(),
                                        0x01.toByte()
                        )

        var cid =
                        byteArrayOf(
                                        0x80.toByte(),
                                        0xB0.toByte(),
                                        0x00.toByte(),
                                        0x04.toByte(),
                                        0x02.toByte(),
                                        0x00.toByte(),
                                        0x0D.toByte()
                        )
        var cidGetdata =
                        byteArrayOf(
                                        0x00.toByte(),
                                        0xC0.toByte(),
                                        0x00.toByte(),
                                        0x00.toByte(),
                                        0x0D.toByte()
                        )

        val nameTH =
                        byteArrayOf(
                                        0x80.toByte(),
                                        0xB0.toByte(),
                                        0x00.toByte(),
                                        0x11.toByte(),
                                        0x02.toByte(),
                                        0x00.toByte(),
                                        0x64.toByte()
                        )
        val nameTHGetdata =
                        byteArrayOf(
                                        0x00.toByte(),
                                        0xC0.toByte(),
                                        0x00.toByte(),
                                        0x00.toByte(),
                                        0x64.toByte()
                        )

        val nameEN =
                        byteArrayOf(
                                        0x80.toByte(),
                                        0xB0.toByte(),
                                        0x00.toByte(),
                                        0x75.toByte(),
                                        0x02.toByte(),
                                        0x00.toByte(),
                                        0x64.toByte()
                        )
        val nameENGetdata =
                        byteArrayOf(
                                        0x00.toByte(),
                                        0xC0.toByte(),
                                        0x00.toByte(),
                                        0x00.toByte(),
                                        0x64.toByte()
                        )

        val birthdate =
                        byteArrayOf(
                                        0x80.toByte(),
                                        0xB0.toByte(),
                                        0x00.toByte(),
                                        0xD9.toByte(),
                                        0x02.toByte(),
                                        0x00.toByte(),
                                        0x08.toByte()
                        )
        val birthdateGetdata =
                        byteArrayOf(
                                        0x00.toByte(),
                                        0xC0.toByte(),
                                        0x00.toByte(),
                                        0x00.toByte(),
                                        0x08.toByte()
                        )

        val gender =
                        byteArrayOf(
                                        0x80.toByte(),
                                        0xB0.toByte(),
                                        0x00.toByte(),
                                        0xE1.toByte(),
                                        0x02.toByte(),
                                        0x00.toByte(),
                                        0x01.toByte()
                        )
        val genderGetdata =
                        byteArrayOf(
                                        0x00.toByte(),
                                        0xC0.toByte(),
                                        0x00.toByte(),
                                        0x00.toByte(),
                                        0x01.toByte()
                        )

        val address =
                        byteArrayOf(
                                        0x80.toByte(),
                                        0xB0.toByte(),
                                        0x15.toByte(),
                                        0x79.toByte(),
                                        0x02.toByte(),
                                        0x00.toByte(),
                                        0x64.toByte()
                        )
        val addressGetdata =
                        byteArrayOf(
                                        0x00.toByte(),
                                        0xC0.toByte(),
                                        0x00.toByte(),
                                        0x00.toByte(),
                                        0x64.toByte()
                        )

        val cardIssuer =
                        byteArrayOf(
                                        0x80.toByte(),
                                        0xB0.toByte(),
                                        0x01.toByte(),
                                        0xF6.toByte(),
                                        0x02.toByte(),
                                        0x00.toByte(),
                                        0x64.toByte()
                        )
        val cardIssuerGetdata =
                        byteArrayOf(
                                        0x00.toByte(),
                                        0xC0.toByte(),
                                        0x00.toByte(),
                                        0x00.toByte(),
                                        0x64.toByte()
                        )

        val issueDate =
                        byteArrayOf(
                                        0x80.toByte(),
                                        0xB0.toByte(),
                                        0x01.toByte(),
                                        0x67.toByte(),
                                        0x02.toByte(),
                                        0x00.toByte(),
                                        0x08.toByte()
                        )
        val issueDateGetdata =
                        byteArrayOf(
                                        0x00.toByte(),
                                        0xC0.toByte(),
                                        0x00.toByte(),
                                        0x00.toByte(),
                                        0x08.toByte()
                        )

        val expireDate =
                        byteArrayOf(
                                        0x80.toByte(),
                                        0xB0.toByte(),
                                        0x01.toByte(),
                                        0x6F.toByte(),
                                        0x02.toByte(),
                                        0x00.toByte(),
                                        0x08.toByte()
                        )
        val expireDateGetdata =
                        byteArrayOf(
                                        0x00.toByte(),
                                        0xC0.toByte(),
                                        0x00.toByte(),
                                        0x00.toByte(),
                                        0x08.toByte()
                        )

        val photo =
                        arrayOf(
                                        byteArrayOf(
                                                        0x80.toByte(),
                                                        0xB0.toByte(),
                                                        0x01.toByte(),
                                                        0x7B.toByte(),
                                                        0x02.toByte(),
                                                        0x00.toByte(),
                                                        0xFF.toByte()
                                        ),
                                        byteArrayOf(
                                                        0x80.toByte(),
                                                        0xB0.toByte(),
                                                        0x02.toByte(),
                                                        0x7A.toByte(),
                                                        0x02.toByte(),
                                                        0x00.toByte(),
                                                        0xFF.toByte()
                                        ),
                                        byteArrayOf(
                                                        0x80.toByte(),
                                                        0xB0.toByte(),
                                                        0x03.toByte(),
                                                        0x79.toByte(),
                                                        0x02.toByte(),
                                                        0x00.toByte(),
                                                        0xFF.toByte()
                                        ),
                                        byteArrayOf(
                                                        0x80.toByte(),
                                                        0xB0.toByte(),
                                                        0x04.toByte(),
                                                        0x78.toByte(),
                                                        0x02.toByte(),
                                                        0x00.toByte(),
                                                        0xFF.toByte()
                                        ),
                                        byteArrayOf(
                                                        0x80.toByte(),
                                                        0xB0.toByte(),
                                                        0x05.toByte(),
                                                        0x77.toByte(),
                                                        0x02.toByte(),
                                                        0x00.toByte(),
                                                        0xFF.toByte()
                                        ),
                                        byteArrayOf(
                                                        0x80.toByte(),
                                                        0xB0.toByte(),
                                                        0x06.toByte(),
                                                        0x76.toByte(),
                                                        0x02.toByte(),
                                                        0x00.toByte(),
                                                        0xFF.toByte()
                                        ),
                                        byteArrayOf(
                                                        0x80.toByte(),
                                                        0xB0.toByte(),
                                                        0x07.toByte(),
                                                        0x75.toByte(),
                                                        0x02.toByte(),
                                                        0x00.toByte(),
                                                        0xFF.toByte()
                                        ),
                                        byteArrayOf(
                                                        0x80.toByte(),
                                                        0xB0.toByte(),
                                                        0x08.toByte(),
                                                        0x74.toByte(),
                                                        0x02.toByte(),
                                                        0x00.toByte(),
                                                        0xFF.toByte()
                                        ),
                                        byteArrayOf(
                                                        0x80.toByte(),
                                                        0xB0.toByte(),
                                                        0x09.toByte(),
                                                        0x73.toByte(),
                                                        0x02.toByte(),
                                                        0x00.toByte(),
                                                        0xFF.toByte()
                                        ),
                                        byteArrayOf(
                                                        0x80.toByte(),
                                                        0xB0.toByte(),
                                                        0x0A.toByte(),
                                                        0x72.toByte(),
                                                        0x02.toByte(),
                                                        0x00.toByte(),
                                                        0xFF.toByte()
                                        ),
                                        byteArrayOf(
                                                        0x80.toByte(),
                                                        0xB0.toByte(),
                                                        0x0B.toByte(),
                                                        0x71.toByte(),
                                                        0x02.toByte(),
                                                        0x00.toByte(),
                                                        0xFF.toByte()
                                        ),
                                        byteArrayOf(
                                                        0x80.toByte(),
                                                        0xB0.toByte(),
                                                        0x0C.toByte(),
                                                        0x70.toByte(),
                                                        0x02.toByte(),
                                                        0x00.toByte(),
                                                        0xFF.toByte()
                                        ),
                                        byteArrayOf(
                                                        0x80.toByte(),
                                                        0xB0.toByte(),
                                                        0x0D.toByte(),
                                                        0x6F.toByte(),
                                                        0x02.toByte(),
                                                        0x00.toByte(),
                                                        0xFF.toByte()
                                        ),
                                        byteArrayOf(
                                                        0x80.toByte(),
                                                        0xB0.toByte(),
                                                        0x0E.toByte(),
                                                        0x6E.toByte(),
                                                        0x02.toByte(),
                                                        0x00.toByte(),
                                                        0xFF.toByte()
                                        ),
                                        byteArrayOf(
                                                        0x80.toByte(),
                                                        0xB0.toByte(),
                                                        0x0F.toByte(),
                                                        0x6D.toByte(),
                                                        0x02.toByte(),
                                                        0x00.toByte(),
                                                        0xFF.toByte()
                                        ),
                                        byteArrayOf(
                                                        0x80.toByte(),
                                                        0xB0.toByte(),
                                                        0x10.toByte(),
                                                        0x6C.toByte(),
                                                        0x02.toByte(),
                                                        0x00.toByte(),
                                                        0xFF.toByte()
                                        ),
                                        byteArrayOf(
                                                        0x80.toByte(),
                                                        0xB0.toByte(),
                                                        0x11.toByte(),
                                                        0x6B.toByte(),
                                                        0x02.toByte(),
                                                        0x00.toByte(),
                                                        0xFF.toByte()
                                        ),
                                        byteArrayOf(
                                                        0x80.toByte(),
                                                        0xB0.toByte(),
                                                        0x12.toByte(),
                                                        0x6A.toByte(),
                                                        0x02.toByte(),
                                                        0x00.toByte(),
                                                        0xFF.toByte()
                                        ),
                                        byteArrayOf(
                                                        0x80.toByte(),
                                                        0xB0.toByte(),
                                                        0x13.toByte(),
                                                        0x69.toByte(),
                                                        0x02.toByte(),
                                                        0x00.toByte(),
                                                        0xFF.toByte()
                                        ),
                                        byteArrayOf(
                                                        0x80.toByte(),
                                                        0xB0.toByte(),
                                                        0x14.toByte(),
                                                        0x68.toByte(),
                                                        0x02.toByte(),
                                                        0x00.toByte(),
                                                        0xFF.toByte()
                                        )
                        )

        val photoGetdata =
                        byteArrayOf(
                                        0x00.toByte(),
                                        0xC0.toByte(),
                                        0x00.toByte(),
                                        0x00.toByte(),
                                        0xFF.toByte()
                        )

        // BP1 No / Request Number (offset 0x00E2, length 0x0B = 11 bytes in Storage Applet)
        val bp1No =
                        byteArrayOf(
                                        0x80.toByte(),
                                        0xB0.toByte(),
                                        0x00.toByte(),
                                        0xE2.toByte(),
                                        0x02.toByte(),
                                        0x00.toByte(),
                                        0x0B.toByte()
                        )
        val bp1NoGetdata =
                        byteArrayOf(
                                        0x00.toByte(),
                                        0xC0.toByte(),
                                        0x00.toByte(),
                                        0x00.toByte(),
                                        0x0B.toByte()
                        )

        // SELECT Chip Data applet (no AID)
        val selectChipData =
                        byteArrayOf(
                                        0x00.toByte(),
                                        0xA4.toByte(),
                                        0x04.toByte(),
                                        0x00.toByte()
                        )

        // GET DATA command for Chip Serial Number (80 CA 9F 7F)
        val chipNoCmd =
                        byteArrayOf(
                                        0x80.toByte(),
                                        0xCA.toByte(),
                                        0x9F.toByte(),
                                        0x7F.toByte()
                        )

        // SELECT Extension Applet command (AID: A0 00 00 00 84 06 00 02)
        val selectExtension =
                        byteArrayOf(
                                        0x00.toByte(),
                                        0xA4.toByte(),
                                        0x04.toByte(),
                                        0x00.toByte(),
                                        0x08.toByte(),
                                        0xA0.toByte(),
                                        0x00.toByte(),
                                        0x00.toByte(),
                                        0x00.toByte(),
                                        0x84.toByte(),
                                        0x06.toByte(),
                                        0x00.toByte(),
                                        0x02.toByte()
                        )

        // Read ADM data command
        val laserIDCmd =
                        byteArrayOf(
                                        0x80.toByte(),
                                        0x00.toByte(),
                                        0x00.toByte(),
                                        0x00.toByte()
                        )
        val laserIDGetdata =
                        byteArrayOf(
                                        0x00.toByte(),
                                        0xC0.toByte(),
                                        0x00.toByte(),
                                        0x00.toByte(),
                                        0x17.toByte()
                        )

        val allDataList: Array<String> =
                        arrayOf(
                                        "cid",
                                        "nameTH",
                                        "nameEN",
                                        "birthdate",
                                        "gender",
                                        "address",
                                        "cardIssuer",
                                        "issueDate",
                                        "expireDate",
                                        "bp1No",
                                        "photo",
                                        "chipNo",
                                        "laserID"
                        )

        fun readAll(r: Reader): HashMap<String, Any> {
                return readSpecific(r, allDataList)
        }

        fun readSpecific(r: Reader, reqList: Array<String>): HashMap<String, Any> {
                val response = HashMap<String, Any>()
                val respArray = ByteArray(500)
                var responsLength: Int
                var slotNum = 0
                resetCard(r)

                // Read chipNo FIRST — right after reset, before selecting Storage Applet.
                // The chip responds to GET DATA (80 CA 9F 7F) in its default state
                // without needing an explicit applet selection.
                if ("chipNo" in reqList) {
                        try {
                                r.setProtocol(slotNum, Reader.PROTOCOL_T0)
                                // Send GET DATA: 80 CA 9F 7F directly (no SELECT needed)
                                val chipInfoResp = ByteArray(300)
                                val chipInfoLen = r.transmit(slotNum, chipNoCmd, chipNoCmd.size, chipInfoResp, chipInfoResp.size)
                                val chipHex = extractChipSerial(chipInfoResp, chipInfoLen, r, slotNum)
                                if (chipHex != null && chipHex.isNotEmpty() && !chipHex.all { it == '0' }) {
                                        response["chipNo"] = chipHex
                                } else {
                                        // Retry once if all-zeros or empty
                                        Thread.sleep(100)
                                        val retryResp = ByteArray(300)
                                        val retryLen = r.transmit(slotNum, chipNoCmd, chipNoCmd.size, retryResp, retryResp.size)
                                        val retryHex = extractChipSerial(retryResp, retryLen, r, slotNum)
                                        if (retryHex != null && retryHex.isNotEmpty() && !retryHex.all { it == '0' }) {
                                                response["chipNo"] = retryHex
                                        }
                                }
                        } catch (_: Exception) {
                                // chipNo not added — non-fatal
                        }
                }

                // Now select Storage Applet for reading personal data
                setProtocol(r)
                if ("cid" in reqList) {
                        r.transmit(slotNum, cid, cid.size, respArray, respArray.size)
                        responsLength =
                                        r.transmit(
                                                        slotNum,
                                                        cidGetdata,
                                                        cidGetdata.size,
                                                        respArray,
                                                        respArray.size
                                        )
                        byteArrayToHexString(respArray, 0, responsLength)?.let {
                                response.put("cid", it)
                        }
                }
                if ("nameTH" in reqList) {
                        r.transmit(slotNum, nameTH, nameTH.size, respArray, respArray.size)
                        responsLength =
                                        r.transmit(
                                                        slotNum,
                                                        nameTHGetdata,
                                                        nameTHGetdata.size,
                                                        respArray,
                                                        respArray.size
                                        )
                        byteArrayToHexString(respArray, 0, responsLength)?.let {
                                response.put("nameTH", it)
                        }
                }
                if ("nameEN" in reqList) {
                        r.transmit(slotNum, nameEN, nameEN.size, respArray, respArray.size)
                        responsLength =
                                        r.transmit(
                                                        slotNum,
                                                        nameENGetdata,
                                                        nameENGetdata.size,
                                                        respArray,
                                                        respArray.size
                                        )
                        byteArrayToHexString(respArray, 0, responsLength)?.let {
                                response.put("nameEN", it)
                        }
                }
                if ("birthdate" in reqList) {
                        r.transmit(slotNum, birthdate, birthdate.size, respArray, respArray.size)
                        responsLength =
                                        r.transmit(
                                                        slotNum,
                                                        birthdateGetdata,
                                                        birthdateGetdata.size,
                                                        respArray,
                                                        respArray.size
                                        )
                        byteArrayToHexString(respArray, 0, responsLength)?.let {
                                response.put("birthdate", it)
                        }
                }
                if ("gender" in reqList) {
                        r.transmit(slotNum, gender, gender.size, respArray, respArray.size)
                        responsLength =
                                        r.transmit(
                                                        slotNum,
                                                        genderGetdata,
                                                        genderGetdata.size,
                                                        respArray,
                                                        respArray.size
                                        )
                        byteArrayToHexString(respArray, 0, responsLength)?.let {
                                response.put("gender", it)
                        }
                }
                if ("address" in reqList) {
                        r.transmit(slotNum, address, address.size, respArray, respArray.size)
                        responsLength =
                                        r.transmit(
                                                        slotNum,
                                                        addressGetdata,
                                                        addressGetdata.size,
                                                        respArray,
                                                        respArray.size
                                        )
                        byteArrayToHexString(respArray, 0, responsLength)?.let {
                                response.put("address", it)
                        }
                }
                if ("cardIssuer" in reqList) {
                        r.transmit(slotNum, cardIssuer, cardIssuer.size, respArray, respArray.size)
                        responsLength =
                                        r.transmit(
                                                        slotNum,
                                                        cardIssuerGetdata,
                                                        cardIssuerGetdata.size,
                                                        respArray,
                                                        respArray.size
                                        )
                        byteArrayToHexString(respArray, 0, responsLength)?.let {
                                response.put("cardIssuer", it)
                        }
                }
                if ("issueDate" in reqList) {
                        r.transmit(slotNum, issueDate, issueDate.size, respArray, respArray.size)
                        responsLength =
                                        r.transmit(
                                                        slotNum,
                                                        issueDateGetdata,
                                                        issueDateGetdata.size,
                                                        respArray,
                                                        respArray.size
                                        )
                        byteArrayToHexString(respArray, 0, responsLength)?.let {
                                response.put("issueDate", it)
                        }
                }
                if ("expireDate" in reqList) {
                        r.transmit(slotNum, expireDate, expireDate.size, respArray, respArray.size)
                        responsLength =
                                        r.transmit(
                                                        slotNum,
                                                        expireDateGetdata,
                                                        expireDateGetdata.size,
                                                        respArray,
                                                        respArray.size
                                        )
                        byteArrayToHexString(respArray, 0, responsLength)?.let {
                                response.put("expireDate", it)
                        }
                }
                if ("bp1No" in reqList) {
                        r.transmit(slotNum, bp1No, bp1No.size, respArray, respArray.size)
                        responsLength =
                                        r.transmit(
                                                        slotNum,
                                                        bp1NoGetdata,
                                                        bp1NoGetdata.size,
                                                        respArray,
                                                        respArray.size
                                        )
                        byteArrayToHexString(respArray, 0, responsLength)?.let {
                                response.put("bp1No", it)
                        }
                }
                if ("photo" in reqList) {
                        val buffer = ByteArrayOutputStream()
                        for (i in photo.indices) {
                                r.transmit(
                                                slotNum,
                                                photo[i],
                                                photo[i].size,
                                                respArray,
                                                respArray.size
                                )
                                responsLength =
                                                r.transmit(
                                                                slotNum,
                                                                photoGetdata,
                                                                photoGetdata.size,
                                                                respArray,
                                                                respArray.size
                                                )
                                buffer.write(respArray, 0, responsLength - 2)
                        }
                        val photoBuffer: ByteArray = buffer.toByteArray()
                        response["photo"] = photoBuffer
                }
                if ("laserID" in reqList) {
                        try {
                                // Card reset + re-init before accessing Extension Applet
                                try { r.power(slotNum, Reader.CARD_COLD_RESET) } catch (_: Exception) {}
                                Thread.sleep(100)
                                try { r.setProtocol(slotNum, Reader.PROTOCOL_T0) } catch (_: Exception) {}
                                // SELECT Extension Applet (AID: A0 00 00 00 84 06 00 02)
                                val selectExtResp = ByteArray(300)
                                val selectExtLen = r.transmit(slotNum, selectExtension, selectExtension.size, selectExtResp, selectExtResp.size)
                                val sw1 = selectExtResp[selectExtLen - 2]
                                // Send laserIDCmd (80 00 00 00) — card may respond 6Cxx or 61xx
                                val laserCmdResp = ByteArray(300)
                                val laserCmdLen = r.transmit(slotNum, laserIDCmd, laserIDCmd.size, laserCmdResp, laserCmdResp.size)
                                val sw1b = laserCmdResp[laserCmdLen - 2]
                                val sw2b = laserCmdResp[laserCmdLen - 1]
                                // Handle T=0 SW responses
                                val finalCmd: ByteArray = when (sw1b) {
                                        0x6C.toByte() -> byteArrayOf(0x80.toByte(), 0x00, 0x00, 0x00, sw2b) // retry with correct Le
                                        0x61.toByte() -> byteArrayOf(0x00, 0xC0.toByte(), 0x00, 0x00, sw2b) // GET RESPONSE
                                        else -> laserIDGetdata
                                }
                                responsLength = r.transmit(slotNum, finalCmd, finalCmd.size, respArray, respArray.size)
                                if (responsLength >= 0x17 + 2) {
                                        val laserBytes = respArray.copyOfRange(7, 23)
                                        val laserStr = String(laserBytes, Charsets.US_ASCII)
                                                .trimEnd('\u0000', ' ')
                                        if (laserStr.isNotEmpty()) {
                                                response["laserID"] = laserStr
                                        }
                                }
                        } catch (_: Exception) {
                                // laserID not added to response — non-fatal
                        } finally {
                                // Always SELECT main applet back to reset applet state
                                try {
                                        val mainAppletResp = ByteArray(300)
                                        r.transmit(slotNum, select, select.size, mainAppletResp, mainAppletResp.size)
                                } catch (_: Exception) {}
                        }
                }
                return response
        }

        /**
         * Extracts chip serial number from a GET DATA (80 CA 9F 7F) response.
         * Handles SW=9000 (data inline), SW=61xx (GET RESPONSE), SW=6Cxx (retry with Le).
         * Parses TLV dynamically with fallback to hardcoded offset 13.
         */
        private fun extractChipSerial(chipInfoResp: ByteArray, chipInfoLen: Int, r: Reader, slotNum: Int): String? {
                val chipSw1 = chipInfoResp[chipInfoLen - 2]
                val chipSw2 = chipInfoResp[chipInfoLen - 1]

                val chipRawData: ByteArray
                val chipRawLen: Int

                if (chipSw1 == 0x90.toByte() && chipSw2 == 0x00.toByte()) {
                        // SW=9000: data already in response
                        chipRawData = chipInfoResp
                        chipRawLen = chipInfoLen
                } else if (chipSw1 == 0x61.toByte() || chipSw1 == 0x6C.toByte()) {
                        // Need follow-up command
                        val chipGetCmd: ByteArray = when (chipSw1) {
                                0x61.toByte() -> byteArrayOf(0x00, 0xC0.toByte(), 0x00, 0x00, chipSw2)
                                else -> byteArrayOf(0x80.toByte(), 0xCA.toByte(), 0x9F.toByte(), 0x7F.toByte(), chipSw2)
                        }
                        val chipDataResp = ByteArray(300)
                        val chipDataLen = r.transmit(slotNum, chipGetCmd, chipGetCmd.size, chipDataResp, chipDataResp.size)
                        chipRawData = chipDataResp
                        chipRawLen = chipDataLen
                } else {
                        // Unknown SW — cannot extract
                        return null
                }

                // Parse TLV dynamically to extract chip serial number
                val dataLen = chipRawLen - 2
                if (dataLen < 21) return null

                var offset = 0
                // Skip Tag bytes (9F7F = multi-byte tag)
                if (offset < dataLen && (chipRawData[offset].toInt() and 0x1F) == 0x1F) {
                        offset++
                        while (offset < dataLen && (chipRawData[offset].toInt() and 0x80) != 0) {
                                offset++
                        }
                        offset++
                } else if (offset < dataLen) {
                        offset++
                }
                // Read Length field
                if (offset < dataLen) {
                        if ((chipRawData[offset].toInt() and 0x80) != 0) {
                                val lenBytes = chipRawData[offset].toInt() and 0x7F
                                offset += 1 + lenBytes
                        } else {
                                offset++
                        }
                }
                // Chip serial is 8 bytes at relative offset 10 within Value
                val serialOffset = offset + 10
                val extractOffset = if (serialOffset + 8 <= dataLen) serialOffset else 13
                if (extractOffset + 8 <= dataLen) {
                        val chipSerial = chipRawData.copyOfRange(extractOffset, extractOffset + 8)
                        return chipSerial.joinToString("") { String.format("%02x", it) }
                }
                return null
        }

        private fun resetCard(r: Reader) {
                r.power(0, Reader.CARD_WARM_RESET)
        }
        private fun setProtocol(r: Reader) {
                r.setProtocol(0, Reader.PROTOCOL_T0)
                val response = ByteArray(300)
                r.transmit(0, select, select.size, response, response.size)
        }

        private fun byteArrayToHexString(input: ByteArray, index: Int, length: Int): String? {
                var length = length
                if (length + index > input.size) {
                        length = input.size - index
                }
                val selectBytes: ByteArray = ByteArray(length)
                System.arraycopy(input, index, selectBytes, 0, length - 2)
                return showByteString(selectBytes)
        }

        private fun showByteString(input: ByteArray?): String? {
                val output: StringBuilder = StringBuilder()
                for (b in input!!) {
                        output.append(String.format("%02x", b))
                }
                var result: String? = null
                result = input.toString(Charset.forName("TIS620"))
                return result
        }
}

package space.diomentia.ptm_dct.data

import android.device.DeviceManager
import android.util.Log
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ProcessLifecycleOwner
import com.rfid.trans.BaseReader
import com.ubx.usdk.USDKManager
import com.ubx.usdk.rfid.RfidManager
import com.ubx.usdk.rfid.aidl.IRfidCallback
import com.ubx.usdk.util.QueryMode

object RfidController : DefaultLifecycleObserver {
    data class RfidTag(
        val tid: String,
        var epc: String,
        var userData: String? = null
    )

    interface RfidListener {
        fun onTagFound(tag: RfidTag?)
        fun onReadStopped()
    }

    var isAvailable by mutableStateOf(false)
        private set

    private const val PASSWD = "00000000"
    private const val DEFAULT_LENGTH: Byte = 6

    private var mManager: RfidManager? = null
    private var mListener: RfidListener? = null

    init {
        ProcessLifecycleOwner.get().lifecycle.addObserver(this)
    }

    fun startRead(
        listener: RfidListener
    ) {
        stopRead()
        mListener = listener
        resumeRead()
    }

    fun resumeRead() {
        if (!isAvailable || mManager == null || mListener == null) {
            stopRead()
            return
        }
        mManager!!.let { manager ->
            manager.queryMode = QueryMode.EPC_TID
            manager.registerCallback(object : IRfidCallback {
                override fun onInventoryTag(epc: String?, data: String?, rssi: String?) {
                    if (data == null) {
                        return
                    }
                    val tag = RfidTag(data, epc ?: "")
                    tag.userData = manager.readDataByTid(
                        tag.tid,
                        3,
                        0,
                        DEFAULT_LENGTH,
                        PASSWD
                    )
                    mListener!!.onTagFound(tag)
                }

                override fun onInventoryTagEnd() {
                    stopRead()
                }
            })
            manager.startRead()
        }
    }

    fun pauseRead() {
        mManager?.stopInventory()
    }

    fun stopRead() {
        pauseRead()
        mListener?.onReadStopped()
        mListener = null
    }

    fun readByTid(tid: String): RfidTag? {
        if (!isAvailable || mManager == null) {
            return null
        }
        var tag: RfidTag? = null
        mManager?.let { manager ->
            tag = RfidTag(
                tid,
                manager.readDataByTid(
                    tid,
                    2,
                    0,
                    DEFAULT_LENGTH,
                    PASSWD
                ),
                manager.readDataByTid(
                    tid,
                    2,
                    0,
                    DEFAULT_LENGTH,
                    PASSWD
                )
            )
        }
        return tag
    }

    fun writeDataByTid(tid: String, userData: String) {
        if (!isAvailable || mManager == null) {
            return
        }
        mManager?.writeTagByTid(
            tid,
            3,
            0,
            BaseReader().hexStringToBytes(PASSWD),
            userData.takeLast(DEFAULT_LENGTH.toInt())
        )
    }

    override fun onStart(owner: LifecycleOwner) {
        if (mManager == null) {
            try {
                DeviceManager().deviceId
                USDKManager.getInstance().init() { status ->
                    if (status) {
                        mManager = USDKManager.getInstance().rfidManager
                        isAvailable = true
                    }
                }
            } catch (stub: RuntimeException) {
                Log.e("RFID Init", "This is not an Urovo device")
            }
        } else {
            mManager?.connect()
            isAvailable = true
        }
    }

    override fun onResume(owner: LifecycleOwner) {
        resumeRead()
    }

    override fun onPause(owner: LifecycleOwner) {
        pauseRead()
    }

    override fun onStop(owner: LifecycleOwner) {
        stopRead()
        mManager?.disConnect()
        isAvailable = false
    }
}
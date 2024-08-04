package space.diomentia.ptm_dct.data

import android.device.DeviceManager
import android.util.Log
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ProcessLifecycleOwner
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
        fun onTagFound(tag: RfidTag)
        fun onReadStopped()
    }

    var isAvailable by mutableStateOf(false)
        private set

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

    private fun resumeRead() {
        if (mListener != null) {
            if (mManager == null) {
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
                        var tag = RfidTag(data, epc ?: "")
                        tag.userData = manager.readDataByTid(
                            tag.tid,
                            3,
                            0,
                            3,
                            "00000000"
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
    }

    private fun pauseRead() {
        mManager?.stopInventory()
    }

    fun stopRead() {
        pauseRead()
        mListener?.onReadStopped()
        mListener = null
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
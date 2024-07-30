package space.diomentia.mcmcontroller

import android.device.DeviceManager
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.ubx.usdk.USDKManager
import com.ubx.usdk.rfid.aidl.IRfidCallback
import com.ubx.usdk.util.QueryMode
import space.diomentia.mcmcontroller.ui.theme.MCMControllerTheme

@OptIn(ExperimentalMaterial3Api::class)
class InitialActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MCMControllerTheme {
                Scaffold(
                    modifier = Modifier.fillMaxSize(),
                    topBar = { TopAppBar(title = { Text(this.title.toString()) }) }
                ) { innerPadding ->
                    RfidInfo(
                        modifier = Modifier.padding(innerPadding)
                    )
                }
            }
        }
    }
}

@Composable
fun RfidInfo(modifier: Modifier = Modifier) {
    Column(modifier = modifier) {
        val deviceId: String = try {
            val deviceManager = DeviceManager()
            deviceManager.deviceId
        } catch (e: Exception) {
            e.message ?: "Error!"
        }
        Text("Device ID: $deviceId")
        var rfidStatus by remember { mutableStateOf<String>(null.toString()) }
        var rfidEpc by remember { mutableStateOf<String>(null.toString()) }
        var rfidTid by remember { mutableStateOf<String>(null.toString()) }
        var rfidData by remember { mutableStateOf<String>(null.toString()) }
        var rfidRssi by remember { mutableStateOf<String>(null.toString()) }
        USDKManager.getInstance().init() { status ->
            val rfidManager = USDKManager.getInstance().rfidManager
            rfidStatus = "${if (status) "SUCCESS" else "FAIL"} at ${rfidManager.outputPower}"
            rfidManager.queryMode = QueryMode.EPC_TID
            rfidManager.registerCallback(object : IRfidCallback {
                override fun onInventoryTag(epc: String?, data: String?, rssi: String?) {
                    rfidEpc = epc.toString()
                    rfidTid = data.toString()
                    rfidData = rfidManager.inventoryParameter.MaskData.toString()
                    rfidRssi = rssi.toString()
                }

                override fun onInventoryTagEnd() {}
            })
            rfidManager.startRead()
        }
        Text("RFID manager: $rfidStatus")
        Text("RFID epc: $rfidEpc")
        Text("RFID tid: $rfidTid")
        Text("RFID data: $rfidData")
        Text("RFID rssi: $rfidRssi")
        /*
        TextField(value = rfidMemory, onValueChange = {
            newMemory ->
            // TODO: write into the RFID tag
        })
        */
    }
}

@Preview(showBackground = true)
@Composable
fun GreetingPreview() {
    MCMControllerTheme {
        RfidInfo()
    }
}
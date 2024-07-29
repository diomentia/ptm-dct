package space.diomentia.mcmcontroller

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.ubx.usdk.USDKManager
import com.ubx.usdk.rfid.aidl.IRfidCallback
import com.ubx.usdk.rfid.aidl.ITag6BCallback
import com.ubx.usdk.util.QueryMode
import space.diomentia.mcmcontroller.ui.theme.MCMControllerTheme

class InitialActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MCMControllerTheme {
                Scaffold(
                    modifier = Modifier.fillMaxSize()
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
        /*
        // from platform SDK
        val deviceId: String = try {
            val deviceManager = DeviceManager()
            deviceManager.deviceId
        } catch (e: Exception) {
            e.message ?: "Error!"
        }
        Text("Device ID: $deviceId")
        val scanStatus = try {
            val scanManager = ScanManager()
            val x = scanManager.openScanner()
            scanManager.closeScanner()
            x.toString()
        } catch (e: Exception) {
            e.message ?: "Error!"
        }
        Text("Barcode scanner: $scanStatus")
        Spacer(modifier = Modifier.padding(20.dp))
        */
        var rfidStatus by remember { mutableStateOf<String>(null.toString()) }
        var rfidFound by remember { mutableStateOf<String>(null.toString()) }
        var rfidMemory by remember { mutableStateOf<String>(null.toString()) }
        var rfidRssi by remember { mutableStateOf<String>(null.toString()) }
        val context = LocalContext.current
        USDKManager.getInstance().init(context) { status: USDKManager.STATUS ->
            val rfidManager = USDKManager.getInstance().rfidManager
            rfidStatus = "${status.name} at ${rfidManager.outputPower}"
            rfidManager.queryMode = QueryMode.EPC_TID
            rfidManager.registerCallback(object : IRfidCallback {
                override fun onInventoryTag(epc: String?, mem: String?, rssi: String?) {
                    rfidFound = epc.toString()
                    rfidMemory = mem.toString()
                    rfidRssi = rssi.toString()
                }

                override fun onInventoryTagEnd() {}
            })
            rfidManager.startRead()
        }
        Text("RFID manager: $rfidStatus")
        Text("RFID found: $rfidFound")
        Text("RFID memory: $rfidMemory")
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
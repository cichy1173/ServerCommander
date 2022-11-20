package com.example.servercommander.fragments

import android.content.Context
import android.content.SharedPreferences
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.example.servercommander.R
import com.example.servercommander.SshConnection
import com.example.servercommander.databinding.FragmentHomeBinding
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.async
import kotlinx.coroutines.launch
import org.json.JSONObject
import org.json.JSONTokener

class HomeFragment : Fragment() {

    private var _binding: FragmentHomeBinding? = null
    private val binding get() = _binding!!

    private lateinit var sharedPref: SharedPreferences
    private lateinit var sshConnection: SshConnection

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        sharedPref = requireActivity().getSharedPreferences(
            getString(R.string.app_name), Context.MODE_PRIVATE
        )
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        _binding = FragmentHomeBinding.inflate(inflater, container, false)

        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val refreshWidget = binding.refreshWidget
        val tempText = binding.dashTemperatureTextView
        val cpuUsage = binding.dashCpuTextView
        val ramUsage = binding.dashRamTextView

        val linuxKernelVersion = binding.kernelInfo.linuxKernelVersion
        val hostname = binding.kernelInfo.hostname
        val uptime = binding.uptimeInfo.upTimeValue
        val localIpAddress = binding.localIpInfo.localIpAddresValue
        val publicIpAddress = binding.publicIpInfo.publicAddressValue
        val diskUsage = binding.diskInfo.diskUsageValue
        val diskName = binding.diskInfo.diskNameText
        val heaviestApp = binding.HeaviestProcessInfo.heaviestProcessValue

        refreshWidget.setOnClickListener {
            if(sharedPref.contains(getString(R.string.server_url)) and
                sharedPref.contains(getString(R.string.username)) and
                sharedPref.contains(getString(R.string.pubkey))) {
                sshConnection = SshConnection(
                    sharedPref.getString(getString(R.string.server_url), "").toString(),
                    22,
                    sharedPref.getString(getString(R.string.username), "").toString(),
                    sharedPref.getString(getString(R.string.pubkey), "").toString()
                )

                val coroutineScope = MainScope()
                coroutineScope.launch {
                    val defer = async(Dispatchers.IO) {
                        sshConnection.executeRemoteCommandOneCall("python3 ~/copilot/main.py --dash")
                    }

                    refreshWidget.animate().apply {
                        duration = 1000
                        rotationBy(360f)
                    }.withEndAction{
                        Toast.makeText(context, "Refreshed manually", Toast.LENGTH_SHORT).show()
                    }.start()

                    val output = defer.await()

                    val jsonObject = JSONTokener(output).nextValue() as JSONObject

                    tempText.text = jsonObject.getString("cpu_temp")
                    cpuUsage.text = jsonObject.getString("cpu_usage")
                    ramUsage.text = jsonObject.getString("ram_usage")

                    linuxKernelVersion.text = jsonObject.getString("kernel")
                    hostname.text = jsonObject.getString("hostname")

                    uptime.text = jsonObject.getString("uptime")
                    localIpAddress.text = jsonObject.getString("local_ip")
                    publicIpAddress.text = jsonObject.getString("public_ip")
                    diskUsage.text = jsonObject.getString("disk_usage")
                    diskName.text = jsonObject.getString("disk_name")
                    heaviestApp.text = jsonObject.getString("stress_app")

                }
            }
            else
            {
                Toast.makeText(context, "Connection to server is not possible with given settings", Toast.LENGTH_SHORT).show()
            }
        }
    }
}
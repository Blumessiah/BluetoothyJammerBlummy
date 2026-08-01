package com.eikarna.bluetoothjammer

import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.graphics.text.LineBreaker
import android.os.Build
import android.os.Bundle
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Spinner
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.core.text.isDigitsOnly
import androidx.core.widget.doAfterTextChanged
import api.AttackType
import api.BluetoothAttack
import com.google.android.material.button.MaterialButton
import com.google.android.material.materialswitch.MaterialSwitch
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textview.MaterialTextView
import util.Logger

class AttackActivity : AppCompatActivity() {

    private lateinit var viewDeviceName: MaterialTextView
    private lateinit var viewDeviceAddress: MaterialTextView
    private lateinit var viewThreads: TextInputEditText
    private lateinit var spinnerAttackType: Spinner
    private lateinit var buttonStartStop: MaterialButton
    private lateinit var logAttack: MaterialTextView
    private lateinit var switchLog: MaterialSwitch

    private lateinit var deviceName: String
    private lateinit var address: String
    private var threads: Int = 8

    private var currentAttack: BluetoothAttack? = null

    companion object {
        @JvmStatic
        var isAttacking = false
        var FrameworkVersion = 1.0
        var loggingStatus = true
    }

    @RequiresApi(Build.VERSION_CODES.O)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.attack_layout)

        // Get data from Intent
        deviceName = intent.getStringExtra("DEVICE_NAME") ?: "Unknown Device"
        address = intent.getStringExtra("ADDRESS") ?: "Unknown Address"
        threads = intent.getIntExtra("THREADS", 8)

        // Get Element ID
        viewDeviceName = findViewById(R.id.textViewDeviceName)
        viewDeviceAddress = findViewById(R.id.textViewAddress)
        viewThreads = findViewById(R.id.editTextThreads)
        spinnerAttackType = findViewById(R.id.spinnerAttackType)
        buttonStartStop = findViewById(R.id.buttonStartStop)
        logAttack = findViewById(R.id.logTextView)
        switchLog = findViewById(R.id.switchLogView)

        // Set text views
        viewDeviceName.text = "Device Name: $deviceName"
        viewDeviceAddress.text = "Address: $address"
        viewThreads.setText("$threads")
        logAttack.justificationMode = LineBreaker.JUSTIFICATION_MODE_INTER_WORD
        Logger.appendLog(logAttack, "Bluetooth Jammer Framework Version: $FrameworkVersion")

        // Attack type selector
        val attackTypes = AttackType.values()
        val typeAdapter = ArrayAdapter(
            this,
            android.R.layout.simple_spinner_dropdown_item,
            attackTypes.map { it.displayName }
        )
        spinnerAttackType.adapter = typeAdapter
        spinnerAttackType.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: android.view.View?, position: Int, id: Long) {
                val type = attackTypes[position]
                if (currentAttack == null) {
                    logAttack.append("\n> ${type.displayName}: ${type.description}")
                }
            }

            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }

        // Set button listener
        buttonStartStop.setOnClickListener {
            if (isAttacking) {
                stopAttack()
            } else {
                startAttack()
            }
        }

        // Threading Input listener
        viewThreads.doAfterTextChanged { str ->
            if (str != null && str.toString().isNotEmpty() && str.isDigitsOnly()) {
                threads = str.toString().toInt()
            }
        }

        // Logging Switch listener
        switchLog.setOnCheckedChangeListener { _, isChecked ->
            loggingStatus = isChecked
            Toast.makeText(
                this@AttackActivity,
                if (isChecked) "Logging habilitado" else "Logging deshabilitado",
                Toast.LENGTH_LONG
            ).show()
        }
    }

    @SuppressLint("MissingPermission")
    private fun startAttack() {
        if (currentAttack?.isRunning() == true) return
        isAttacking = true
        buttonStartStop.text = "Stop"
        BluetoothAdapter.getDefaultAdapter().cancelDiscovery()

        val selectedType = AttackType.values()[spinnerAttackType.selectedItemPosition]
        val attack = selectedType.create(address, threads)
        currentAttack = attack
        Logger.appendLog(logAttack, "Ataque iniciado: ${attack.displayName} -> $address ($deviceName) | Intensidad: $threads")
        Toast.makeText(
            this,
            "Usa esto SOLO con dispositivos de tu propiedad. Stop para detener.",
            Toast.LENGTH_LONG
        ).show()
        attack.start(this) { message ->
            runOnUiThread {
                if (loggingStatus && isAttacking) Logger.appendLog(logAttack, message)
            }
        }
    }

    @SuppressLint("MissingPermission")
    private fun stopAttack() {
        isAttacking = false
        buttonStartStop.text = "Start"
        Logger.appendLog(logAttack, "Ataque detenido.")
        currentAttack?.stop()
        currentAttack = null
        BluetoothAdapter.getDefaultAdapter().startDiscovery()
    }

    override fun onDestroy() {
        super.onDestroy()
        if (isAttacking) {
            stopAttack()
        }
    }

    override fun onPause() {
        super.onPause()
        if (isAttacking) {
            stopAttack()
        }
    }
}

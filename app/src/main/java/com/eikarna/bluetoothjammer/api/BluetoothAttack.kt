package api

import android.content.Context

/**
 * Common contract for the Bluetooth disruption techniques.
 * Implementations run on their own background coroutines and report progress
 * through [onLog] (called from background threads — the caller marshals to UI).
 */
interface BluetoothAttack {
    val displayName: String
    val description: String
    fun start(context: Context, onLog: (String) -> Unit)
    fun stop()
    fun isRunning(): Boolean
}

/**
 * Selectable attack types. [create] builds the concrete attack for a target.
 */
enum class AttackType(val displayName: String, val description: String) {
    L2CAP_FLOOD(
        "L2CAP Flood (clásico)",
        "Inunda conexiones RFCOMM/L2CAP con UUIDs aleatorios y satura el socket."
    ),
    GATT_FLOOD(
        "GATT Flood (BLE)",
        "Llena la tabla de conexiones GATT del periférico BLE para bloquear a su dueño."
    ),
    PAIRING_FLOOD(
        "Pairing Flood",
        "Inunda al objetivo de solicitudes de emparejamiento (spam de diálogos)."
    ),
    SDP_FLOOD(
        "SDP Query Storm",
        "Satura el servidor SDP del objetivo con consultas de servicios repetidas."
    ),
    ADVERTISE_FLOOD(
        "Advertising Flood (BLE)",
        "Contamina el canal de anuncios BLE con UUIDs aleatorios."
    );

    fun create(address: String, threads: Int): BluetoothAttack = when (this) {
        L2CAP_FLOOD -> L2capFloodAttack(address, threads)
        GATT_FLOOD -> GattFloodAttack(address, threads)
        PAIRING_FLOOD -> PairingFloodAttack(address, threads)
        SDP_FLOOD -> SdpFloodAttack(address, threads)
        ADVERTISE_FLOOD -> AdvertiseFloodAttack(address)
    }
}

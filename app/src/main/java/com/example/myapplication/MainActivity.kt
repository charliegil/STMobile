package com.example.myapplication

import android.app.PendingIntent
import android.content.Intent
import android.content.IntentFilter
import android.nfc.NfcAdapter
import android.nfc.tech.IsoDep
import android.nfc.tech.NfcB
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import java.time.LocalDate
import java.time.Month
import java.util.*


class MainActivity : AppCompatActivity() {
    private var pendingIntent: PendingIntent? = null
    private var techListsArray: Array<Array<String>>? = null
    private var intentFiltersArray: Array<IntentFilter>? = null
    private var adapter: NfcAdapter? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        adapter = NfcAdapter.getDefaultAdapter(this)
        if (adapter == null) {
            // Stop here, we definitely need NFC
            Toast.makeText(this, "This device doesn't support NFC.", Toast.LENGTH_LONG).show()
            finish()
            return
        }
        setContentView(R.layout.activity_main)
        val intent = Intent(this, javaClass).apply {
            addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP)
        }
        pendingIntent = PendingIntent.getActivity(
            this, 0, intent,
            PendingIntent.FLAG_MUTABLE
        )
        val ndef = IntentFilter(NfcAdapter.ACTION_TECH_DISCOVERED).apply {
        }

        intentFiltersArray = arrayOf(ndef)
        techListsArray = arrayOf(arrayOf<String>(IsoDep::class.java.name, NfcB::class.java.name))
    }

    @RequiresApi(Build.VERSION_CODES.N)
    private fun handleIntent(intent: Intent) {
        val isoDep = IsoDep.get(intent.getParcelableExtra(NfcAdapter.EXTRA_TAG))
        isoDep.use {
            isoDep.connect()
            isoDep.transceive(
                byteArrayOf(
                    0x94.toByte(),
                    0xA4.toByte(),
                    0x00,
                    0x00,
                    0x02,
                    0x00,
                    0x02
                )
            )
            val idField = isoDep.transceive(byteArrayOf(0x94.toByte(), 0xB2.toByte(), 0x01, 0x04))
            val id_b = idField.slice(16..19)
            val id = ((id_b[0].toUInt() and 0xFFu) shl 24) or
                    ((id_b[1].toUInt() and 0xFFu) shl 16) or
                    ((id_b[2].toUInt() and 0xFFu) shl 8) or
                    (id_b[3].toUInt() and 0xFFu)
            Log.i("Numéro de carte", "%d (%s)".format(id.toLong(), formatBytes(id_b)))
            // Devrait contenir: Version de carte OPUS, réseau STM, date d'expiration & info sur l'utilisateur(?)
            val envField = read(isoDep, arrayOf<UShort>().asIterable(), 0x07) ?: throw Exception("???? No Env?")
            val expiration = readBits(envField[0], 45, 14);
            val expirationDate = LocalDate.of(1997, Month.JANUARY, 1).plusDays(expiration.toLong())
            val expired = expirationDate < LocalDate.now()
            Log.i("Expiration de la carte", expirationDate.toString() + if (expired) " (expired)" else "")
            // Passe mensuelle
            // Peut contenir le type de billet, l'expiration, la localité (?), les restrictions et des infos d'achat
            val subsField = read(isoDep, arrayOf<UShort>().asIterable(), 0x09) ?: throw Exception("???? No Subs?")
            /*
            mensuelle, avec billets:
             1E:02:14:69:C9:B7:27:C7:F4:82:4D:F4:94:00:00:00:00:00:20:00:00:00:00:00:00:00:00:00:00
             1E:02:14:69:C9:F3:28:BF:F4:82:4F:B6:D2:00:00:00:00:00:20:00:00:00:00:00:00:00:00:00:00
             1E:02:14:69:CA:31:29:B7:F4:82:52:24:86:00:00:00:00:00:20:00:00:00:00:00:00:00:00:00:00
             00:00:00:00:00:00:00:00:00:00:00:00:00:00:00:00:00:00:00:00:00:00:00:00:00:00:00:00:00

            sans mensuelle, avec billets:
             1E:02:14:8B:80:00:00:07:F4:82:53:25:8C:00:00:00:00:00:00:00:00:00:00:00:00:00:00:00:00
             00:00:00:00:00:00:00:00:00:00:00:00:00:00:00:00:00:00:00:00:00:00:00:00:00:00:00:00:00
             00:00:00:00:00:00:00:00:00:00:00:00:00:00:00:00:00:00:00:00:00:00:00:00:00:00:00:00:00
             00:00:00:00:00:00:00:00:00:00:00:00:00:00:00:00:00:00:00:00:00:00:00:00:00:00:00:00:00

            mensuelle, sans billets:
             1E:02:14:87:C9:B7:2A:97:F4:82:4D:D6:80:00:00:00:00:00:20:00:00:00:00:00:00:00:00:00:00
             00:00:00:00:00:00:00:00:00:00:00:00:00:00:00:00:00:00:00:00:00:00:00:00:00:00:00:00:00
             00:00:00:00:00:00:00:00:00:00:00:00:00:00:00:00:00:00:00:00:00:00:00:00:00:00:00:00:00
             00:00:00:00:00:00:00:00:00:00:00:00:00:00:00:00:00:00:00:00:00:00:00:00:00:00:00:00:00

            -> billets prennent des places dans les subs
             */
            //subsField.forEach { sub -> Log.d("sub", formatBytes(sub.asIterable())) }
            // Passages
            /*val passagesField: Vector<ByteArray> = (0x19..0x1C).flatMap {
                read(isoDep, arrayOf<UShort>().asIterable(), it.toByte())
                    ?: throw Exception("???? No Passages?")
            }.toCollection(Vector<ByteArray>())*/ // Only one pass??
            // Devrait contenir: compte de billets encore actifs
            val passagesField: Vector<ByteArray?> = (0x202A..0x202D).map {
                (read(isoDep, arrayOf(0x0002U, it.toUShort()).asIterable())
                    ?: throw Exception("???? No Passages?")).getOrNull(0)
            }.toCollection(Vector<ByteArray?>())
            //passagesField.filter { it != null }.forEach { passage -> Log.d("p", formatBytes(passage!!.asIterable())) }
            subsField.zip(passagesField).forEach { (sub, p) ->
                val isTicket = p?.let { !it.slice(0..10).all { byte -> byte == 0x00.toByte() } } ?: true
                val hasSub = !sub.all { byte -> byte == 0x00.toByte() }
                if (isTicket) {
                    val numberOfTickets = readBits(p!!, 16, 8)
                    Log.i("Billets", "Nombre de billets: %d".format(numberOfTickets))
                } else if (hasSub) {
                    // Braindead date format, 14 bits??? And since 1997?? Year of the initial Calypso spec, but still
                    val expiration = readBits(sub, 47, 14)
                    val expirationDate = LocalDate.of(1997, Month.JANUARY, 1).plusDays(expiration.toLong())
                    val expired = expirationDate < LocalDate.now()
                    Log.i("Abonnement", expirationDate.toString() + if (expired) " (expirée)" else "")
                } else {
                    // empty
                }
            }
            // Derniers transits
            val transitsField = read(isoDep, arrayOf<UShort>().asIterable(), 0x08) ?: throw Exception("???? No Transits?")
            // Kossé cé?
            val specialField = read(isoDep, arrayOf<UShort>().asIterable(), 0x1D) ?: throw Exception("???? No Special?")
            // Kossé cé?
            val subsListField = read(isoDep, arrayOf<UShort>().asIterable(), 0x1E) ?: throw Exception("???? No Subs List?")

            // list(isoDep) // Auto-découverte

            // 0002 (0) -> 00:00:00:00:00:00:00:00:00:00:00:00:00:00:00:00:C4:2F:75:CE:2E:BB:20:00:08:00:71:00:34:90:00
            // 0002:0002 (0) -> 00:00:00:00:00:00:00:00:00:00:00:00:00:00:00:00:C4:2F:75:CE:2E:BB:20:00:08:00:71:00:34:90:00
            // 0002:0003 (x) -> 69:82 // x = 0..=3, mais est-ce que 4+ aussi?
            // 0002:2000 (x) -> 69:86 // x = 0..=3, mais est-ce que 4+ aussi?
            // 0002:2001 (0) -> 04:78:92:00:09:05:5D:6A:82:22:00:00:00:00:00:00:00:00:00:00:00:00:00:00:00:00:00:00:00:90:00
            // 0002:2004 (0) -> 31:54:49:43:2E:49:43:41:00:00:00:00:00:00:00:00:90:00
            // 0002:2010 (x) -> 00:00:00:00:00:00:00:00:00:00:00:00:00:00:00:00:00:00:00:00:00:00:00:00:00:00:00:00:00:90:00 // x = 0..=2
            // 0002:2020 (0) -> 1E:02:14:8B:80:00:00:07:F4:82:53:25:84:00:00:00:00:00:00:00:00:00:00:00:00:00:00:00:00:90:00
            // 0002:2020 (x) -> 00:00:00:00:00:00:00:00:00:00:00:00:00:00:00:00:00:00:00:00:00:00:00:00:00:00:00:00:00:90:00 // x = 1..=3
            // 0002:202A (0) -> 00:00:01:00:00:00:00:00:00:00:00:FF:FF:FF:00:00:00:00:00:00:00:00:00:00:00:00:00:00:00:90:00
            // 0002:202B (0) -> 00:00:00:00:00:00:00:00:00:00:00:FF:FF:FF:00:00:00:00:00:00:00:00:00:00:00:00:00:00:00:90:00
            // 0002:202C (0) -> 00:00:00:00:00:00:00:00:00:00:00:FF:FF:FF:00:00:00:00:00:00:00:00:00:00:00:00:00:00:00:90:00
            // 0002:202D (0) -> 00:00:00:00:00:00:00:00:00:00:00:FF:FF:FF:00:00:00:00:00:00:00:00:00:00:00:00:00:00:00:90:00
            // 0002:202E (0) -> 3F:FF:FF:00:00:00:00:00:00:00:00:FF:FF:FF:00:00:00:00:00:00:00:00:00:00:00:00:00:00:00:90:00
            // 0002:202F (0) -> 00:00:00:00:00:00:00:00:00:00:00:FF:FF:FF:00:00:00:00:00:00:00:00:00:00:00:00:00:00:00:90:00
            // 0002:2030 (0) -> 1F:01:0A:45:B0:80:00:00:00:00:00:00:00:00:00:00:00:00:00:00:00:00:00:00:00:00:00:00:00:90:00
            // 0002:2040 (0) -> 00:00:00:00:00:00:00:00:00:00:00:00:00:00:00:00:00:00:00:00:00:00:00:00:00:00:00:00:00:90:00
            // 0002:2050 (0) -> 1F:01:0A:45:B0:80:00:00:00:00:00:00:00:00:00:00:00:00:00:00:00:00:00:00:00:00:00:00:00:90:00
            // 0002:2060 (0) -> 00:00:00:00:00:00:00:00:00:00:00:FF:FF:FF:00:00:00:00:00:00:00:00:00:00:00:00:00:00:00:90:00
            // 0002:2061 (0) -> 00:00:00:00:00:00:00:00:00:00:00:FF:FF:FF:00:00:00:00:00:00:00:00:00:00:00:00:00:00:00:90:00
            // 0002:2062 (0) -> 00:00:00:00:00:00:00:00:00:00:00:FF:FF:FF:00:00:00:00:00:00:00:00:00:00:00:00:00:00:00:90:00
        }
    }

    private fun readBits(data: ByteArray, index: Int, length: Int): Int {
        var firstBit = index % 8
        val lastByte = (index + length - 1) / 8
        val lastBit = (index + length - 1) % 8
        var result = 0
        var offset = 8 * (lastByte - index / 8 - 1) + lastBit + 1
        for (currentByte in index / 8 until lastByte) {
            result += (data[currentByte].toInt() and (0xFF shr firstBit)) shl offset
            firstBit = 0
            offset -= 8
        }
        result += (data[lastByte].toInt() and (0xFF shr firstBit)) shr (7 - lastBit)
        return result
    }

    private fun read(tag: IsoDep, path: Iterable<UShort>, shortID: Byte = 0x00): Vector<ByteArray>? {
        if (shortID == 0x00.toByte()) {
            val address = path.flatMap { listOf((it.toInt() shr 8).toByte(), it.toByte()) }
            val select = byteArrayOf(0x94.toByte(), 0xA4.toByte(), 0x08, 0x00) +
                    address.size.toByte() + address

            val ret = tag.transceive(select)
            if (ret[ret.size - 2] != 0x90.toByte() || ret[ret.size - 1] != 0x00.toByte()) {
                //Log.d("Error reading tag", formatBytes(select.asIterable()) +" => " + formatBytes(ret.asIterable()))
                return null
            }
        }
        val record = if (shortID == 0x00.toByte()) 0x04 else (shortID.toInt() shl 3).toByte()
        val read = byteArrayOf(0x94.toByte(), 0xB2.toByte(), 0x01, record)
        val out = Vector<ByteArray>()
        for (i in 1..4) {
            read[2] = i.toByte()
            val ret = tag.transceive(read)
            if (ret[0] == 0x6A.toByte()) break
            if (ret[ret.size - 2] != 0x90.toByte() || ret[ret.size - 1] != 0x00.toByte()) {
                Log.d("Error reading tag", formatBytes(read.asIterable()) +" => " + formatBytes(ret.asIterable()))
            }
            out.add(ret.slice(0 until ret.size-2).toByteArray())
        }
        return out
    }

    private fun formatBytes(bytes: Iterable<Byte>): String {
        return bytes.joinToString(":") { "%02X".format(it.toInt() and 0xFF) }
    }

    public override fun onPause() {
        super.onPause()
        adapter?.disableForegroundDispatch(this)
    }

    public override fun onResume() {
        super.onResume()
        adapter?.enableForegroundDispatch(this, pendingIntent, intentFiltersArray, techListsArray)
    }

    @RequiresApi(Build.VERSION_CODES.N)
    public override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        handleIntent(intent)
    }

    private fun list(tag: IsoDep) {
        val start = Vector<UShort>()
        recList(tag, start)
    }

    private fun recList(tag: IsoDep, path: Vector<UShort>) {
        val MAX = (1.toInt() shl 16) - 1
        val MIN = 1//if (path.size == 0) 2 else 1
        val MAX_DEPTH = 5
        Log.d("started", path.joinToString(":") { "%d".format(it.toInt()) })
        path.add(0U)
        for (i in MIN..MAX) {
            path[path.size - 1] = i.toUShort()
            if (test(tag, path) && path.size < MAX_DEPTH) recList(tag, path)
        }
        path.removeAt(path.size - 1)
        Log.d("done", path.joinToString(":") { "%d".format(it.toInt()) })
    }

    private fun test(tag: IsoDep, path: Vector<UShort>): Boolean {
        val pathString = path.joinToString(":") { "%04X".format(it.toInt() and 0xFFFF) }
        val out = read(tag, path) ?: return false
        out.forEachIndexed { index, ret ->
            Log.e(
                "RARARAR",
                "%s (%d) -> %s".format(
                    pathString,
                    index,
                    ret.joinToString(":") { "%02X".format(it.toInt() and 0xFF) })
            )
        }
        return true
    }
}
package com.example.myapplication

import android.app.PendingIntent
import android.content.Intent
import android.content.IntentFilter
import android.nfc.NfcAdapter
import android.nfc.Tag
import android.nfc.tech.IsoDep
import android.nfc.tech.NfcB
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity


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
        pendingIntent = PendingIntent.getActivity(this, 0, intent,
            PendingIntent.FLAG_MUTABLE)
        val ndef = IntentFilter(NfcAdapter.ACTION_TECH_DISCOVERED).apply {
        }

        intentFiltersArray = arrayOf(ndef)
        techListsArray = arrayOf(arrayOf<String>(IsoDep::class.java.name, NfcB::class.java.name))
        handleIntent(getIntent())
    }

    private fun handleIntent(intent: Intent) {
        Log.e("bob","RRRRRRRRRRRRRRRRRRRRRRRRRRRRRRRRRRRARRRRRRRRRRRRRRRRRRRRRRR")
        val isoDep = IsoDep.get(intent.getParcelableExtra(NfcAdapter.EXTRA_TAG))
        //do something with tagFromIntent
        // TODO
    }

    public override fun onPause() {
        super.onPause()
        adapter?.disableForegroundDispatch(this)
    }

    public override fun onResume() {
        super.onResume()
        adapter?.enableForegroundDispatch(this, pendingIntent, intentFiltersArray, techListsArray)
    }

    public override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        handleIntent(intent)
    }
}
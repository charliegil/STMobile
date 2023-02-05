package com.example.myapplication

import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.widget.Button
import android.widget.EditText
import androidx.appcompat.app.AppCompatActivity


class DisplayActivity: AppCompatActivity()  {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.card_fragment)
        val buttonClick = findViewById<Button>(R.id.button)
        buttonClick.setOnClickListener {
            val intent = Intent(this, MainActivity::class.java)
            startActivity(intent)
        }

        val exp = intent.getLongExtra("CardID", -1)
        var id = findViewById<EditText>(R.id.editTextTextPersonName5)
        id.setText(exp.toString())

        val ticket = intent.getIntExtra("nbTickets", -1)
        id = findViewById<EditText>(R.id.ticketsLeft)
        id.setText(if(ticket != -1) ticket.toString() else "Error")

        val date = intent.getStringExtra("subExpiration")
        id = findViewById<EditText>(R.id.editTextDateSub)
        id.setText(if(date != null && date != "2000-01-01") date else "No Monthly Subscription")

    }

}
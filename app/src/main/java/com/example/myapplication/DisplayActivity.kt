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

//        val exp = intent.getStringExtra("Expiration")
//        val date = findViewById<EditText>(R.id.editTextDate)
//        date.setText(if (exp != null) exp else "None" )

    }

}
package com.example.starbucksmapapp

import android.os.Bundle
import android.widget.ArrayAdapter
import android.widget.ListView
import androidx.appcompat.app.AppCompatActivity

class StampsActivity : AppCompatActivity() {
    private lateinit var stampsManager: StampsManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_stamps)
        stampsManager = StampsManager(this)

        val listView: ListView = findViewById(R.id.list_stamps)
        val stamps = stampsManager.getAllStamps().values.toList()
        listView.adapter = ArrayAdapter(this, android.R.layout.simple_list_item_1, stamps)
    }
}

package com.example.starbucksmapapp

import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.widget.ArrayAdapter
import android.widget.ListView
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity

class StampsActivity : AppCompatActivity() {
    private lateinit var stampsManager: StampsManager
    private lateinit var listView: ListView
    private lateinit var emptyView: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_stamps)

        // アクションバーに戻るボタンを表示
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = "スタンプ一覧"

        stampsManager = StampsManager(this)

        listView = findViewById(R.id.list_stamps)
        emptyView = findViewById(R.id.empty_view)

        loadStamps()
    }

    private fun loadStamps() {
        val stamps = stampsManager.getAllStamps()

        if (stamps.isEmpty()) {
            listView.visibility = android.view.View.GONE
            emptyView.visibility = android.view.View.VISIBLE
        } else {
            listView.visibility = android.view.View.VISIBLE
            emptyView.visibility = android.view.View.GONE

            val displayItems = stamps.map { stamp ->
                "${stamp.name}\n${stamp.timestamp}"
            }

            val adapter = ArrayAdapter(
                this,
                android.R.layout.simple_list_item_2,
                android.R.id.text1,
                displayItems
            )
            listView.adapter = adapter

            // 長押しで削除機能
            listView.setOnItemLongClickListener { _, _, position, _ ->
                showDeleteConfirmDialog(stamps[position])
                true
            }
        }

        // スタンプ数をタイトルに表示
        supportActionBar?.title = "スタンプ一覧 (${stamps.size}枚)"
    }

    private fun showDeleteConfirmDialog(stamp: StampInfo) {
        AlertDialog.Builder(this)
            .setTitle("スタンプを削除")
            .setMessage("「${stamp.name}」のスタンプを削除しますか？")
            .setPositiveButton("削除") { _, _ ->
                stampsManager.removeStamp(stamp.placeId)
                loadStamps()
            }
            .setNegativeButton("キャンセル", null)
            .show()
    }

    private fun showClearAllDialog() {
        AlertDialog.Builder(this)
            .setTitle("全スタンプを削除")
            .setMessage("すべてのスタンプを削除しますか？この操作は取り消せません。")
            .setPositiveButton("削除") { _, _ ->
                stampsManager.clearAllStamps()
                loadStamps()
            }
            .setNegativeButton("キャンセル", null)
            .show()
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.stamps_menu, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            android.R.id.home -> {
                finish()
                true
            }
            R.id.action_clear_all -> {
                if (stampsManager.getStampCount() > 0) {
                    showClearAllDialog()
                }
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }
}
package com.mueblespaty

import android.os.Bundle
import android.view.MenuItem
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView

class FaqActivity : BaseActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_faq)

        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = getString(R.string.faq_title)

        val rootView = findViewById<android.widget.LinearLayout>(R.id.faq_root)
        setupWindowInsetsForView(rootView)

        val questions = resources.getStringArray(R.array.faq_questions)
        val answers = resources.getStringArray(R.array.faq_answers)
        val items = questions.zip(answers).map { (q, a) -> FaqAdapter.FaqItem(q, a) }

        val recyclerView = findViewById<RecyclerView>(R.id.rv_faq)
        recyclerView.layoutManager = LinearLayoutManager(this)
        recyclerView.adapter = FaqAdapter(items)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == android.R.id.home) {
            finish()
            return true
        }
        return super.onOptionsItemSelected(item)
    }
}

package com.mueblespaty

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class FaqAdapter(private val items: List<FaqItem>) : RecyclerView.Adapter<FaqAdapter.ViewHolder>() {

    data class FaqItem(val question: String, val answer: String, var isExpanded: Boolean = false)

    inner class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val tvQuestion: TextView = view.findViewById(R.id.tv_question)
        val tvAnswer: TextView = view.findViewById(R.id.tv_answer)
        val tvChevron: TextView = view.findViewById(R.id.tv_chevron)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_faq, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = items[position]
        holder.tvQuestion.text = item.question
        holder.tvAnswer.text = item.answer

        holder.tvAnswer.visibility = if (item.isExpanded) View.VISIBLE else View.GONE
        holder.tvChevron.rotation = if (item.isExpanded) 180f else 0f

        holder.itemView.setOnClickListener {
            item.isExpanded = !item.isExpanded
            holder.tvAnswer.visibility = if (item.isExpanded) View.VISIBLE else View.GONE
            holder.tvChevron.animate()
                .rotation(if (item.isExpanded) 180f else 0f)
                .setDuration(200)
                .start()
        }
    }

    override fun getItemCount() = items.size
}

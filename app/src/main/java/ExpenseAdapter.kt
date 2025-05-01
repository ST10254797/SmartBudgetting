package com.st10254797.smartbudgetting

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import com.bumptech.glide.Glide

class ExpenseAdapter(
    private val context: Context,
    private val expenses: MutableList<Expense>, // Use MutableList for modification
    private val onDeleteClick: (Expense) -> Unit // Callback for delete action
) : BaseAdapter() {

    override fun getCount(): Int = expenses.size

    override fun getItem(position: Int): Any = expenses[position]

    override fun getItemId(position: Int): Long = expenses[position].id.toLong()

    override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
        val view = convertView ?: LayoutInflater.from(context).inflate(R.layout.expense_list_item, parent, false)

        val expense = expenses[position]

        val imageView = view.findViewById<ImageView>(R.id.expenseImage)
        val descriptionView = view.findViewById<TextView>(R.id.expenseDescription)
        val amountView = view.findViewById<TextView>(R.id.expenseAmount)
        val dateView = view.findViewById<TextView>(R.id.expenseDate)
        val deleteButton = view.findViewById<Button>(R.id.deleteButton)

        descriptionView.text = expense.description
        amountView.text = "R${expense.amount}"
        dateView.text = "Date: ${expense.date}"

        // Load image if available
        if (!expense.imageUrl.isNullOrEmpty()) {
            Glide.with(context).load(Uri.parse(expense.imageUrl)).into(imageView)
        } else {
            imageView.setImageResource(R.drawable.ic_launcher_foreground) // fallback image
        }

        // Open full-screen image on tap
        imageView.setOnClickListener {
            val intent = Intent(context, ImageViewActivity::class.java)
            intent.putExtra("imageUrl", expense.imageUrl) // Pass the image URL
            context.startActivity(intent)
        }

        // Set delete button listener
        deleteButton.setOnClickListener {
            onDeleteClick(expense) // Call the delete callback
        }

        return view
    }
}
//GeeksforGeeks, 2021.How to Perform CRUD Operations in Room Database in Android? [online] Available at: https://www.geeksforgeeks.org/how-to-perform-crud-operations-in-room-database-in-android/ (Accessed 28 April 2025)
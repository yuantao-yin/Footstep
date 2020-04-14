package org.footstep.activities

import android.app.Activity
import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.view.View
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import kotlinx.android.synthetic.main.activity_main.*
import org.footstep.R
import org.footstep.adapter.FootstepAdapter
import org.footstep.database.DatabaseHandler
import org.footstep.models.FootstepModel
import org.footstep.utils.SwipeToEditCallback

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        fabAddHappyPlace.setOnClickListener {
            val intent = Intent(this, AddFootstepActivity::class.java)
            //startActivity(intent)
            startActivityForResult(intent, ADD_ACTIVITY_REQUEST_CODE)
        }

        getFootstepListFromLocalDB()
    }

    private fun setupFootstepRecyclerView(footstepList: ArrayList<FootstepModel>){
        rv_footstep_list.layoutManager = LinearLayoutManager(this)
        rv_footstep_list.setHasFixedSize(true)
        val footstepAdapter = FootstepAdapter(this, footstepList)
        rv_footstep_list.adapter = footstepAdapter

        footstepAdapter.setOnClickListener(object: FootstepAdapter.OnClickListener{
            override fun onClick(position: Int, model: FootstepModel) {
                val intent = Intent(this@MainActivity, FootstepDetailActivity::class.java)
                // model need to be serializable
                intent.putExtra(EXTRA_DETAILS, model)
                startActivity(intent)
            }
        })

        val editSwipeHandler = object: SwipeToEditCallback(this){
            override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {
                val adapter = rv_footstep_list.adapter as FootstepAdapter
                adapter.notifyEditItem(this@MainActivity, viewHolder.adapterPosition, ADD_ACTIVITY_REQUEST_CODE)
            }
        }

        val editItemTouchHelper = ItemTouchHelper(editSwipeHandler)
        editItemTouchHelper.attachToRecyclerView(rv_footstep_list)
    }



    private fun getFootstepListFromLocalDB() {
        val dbHandler = DatabaseHandler(this)

        val footstepList = dbHandler.getFootstepList()

        if (footstepList.size > 0) {
            rv_footstep_list.visibility = View.VISIBLE
            tv_no_records_available.visibility = View.GONE
            setupFootstepRecyclerView(footstepList)
        } else {
            rv_footstep_list.visibility = View.GONE
            tv_no_records_available.visibility = View.VISIBLE
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == ADD_ACTIVITY_REQUEST_CODE) {
            if (resultCode == Activity.RESULT_OK) {
                getFootstepListFromLocalDB()
            } else {
                Log.i("Activity", "Cancelled or Back pressed")
            }
        }
    }
    companion object {
        var ADD_ACTIVITY_REQUEST_CODE = 1
        internal const val EXTRA_DETAILS = "extra_details"
    }
}

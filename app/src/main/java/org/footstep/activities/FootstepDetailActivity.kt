package org.footstep.activities

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import kotlinx.android.synthetic.main.activity_footstep_detail.*
import org.footstep.R
import org.footstep.models.FootstepModel

class FootstepDetailActivity : AppCompatActivity() {

    /**
     * This function is auto created by Android when the Activity Class is created.
     */
    override fun onCreate(savedInstanceState: Bundle?) {
        //This call the parent constructor
        super.onCreate(savedInstanceState)
        // This is used to align the xml view to this class
        setContentView(R.layout.activity_footstep_detail)

        var footstepDetailModel: FootstepModel? = null

        if (intent.hasExtra(MainActivity.EXTRA_DETAILS)) {
            // get the Serializable data model class with the details in it
            footstepDetailModel =
                intent.getParcelableExtra(MainActivity.EXTRA_DETAILS) as FootstepModel
        }

        if (footstepDetailModel != null) {

            setSupportActionBar(toolbar_footstep_detail)
            supportActionBar!!.setDisplayHomeAsUpEnabled(true)
            supportActionBar!!.title = footstepDetailModel.title

            toolbar_footstep_detail.setNavigationOnClickListener {
                onBackPressed()
            }

            iv_place_image.setImageURI(Uri.parse(footstepDetailModel.image))
            tv_description.text = footstepDetailModel.description
            tv_location.text = footstepDetailModel.location
        }

        btn_view_on_map.setOnClickListener {
            val intent = Intent(this@FootstepDetailActivity, MapActivity::class.java)
            intent.putExtra(MainActivity.EXTRA_DETAILS, footstepDetailModel)
            startActivity(intent)
        }
    }
}
package com.example.happyplaces.activities

import android.content.Intent
import android.net.Uri
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import com.example.happyplaces.HappyPlaceEntity
import com.example.happyplaces.databinding.ActivityHappyPlaceDetailBinding

class HappyPlaceDetailActivity : AppCompatActivity() {

    private lateinit var binding: ActivityHappyPlaceDetailBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityHappyPlaceDetailBinding.inflate(layoutInflater)
        setContentView(binding.root)

        var happyPlaceDetails: HappyPlaceEntity? = null

        if (intent.hasExtra(MainActivity.EXTRA_PLACE_DETAILS)){

            happyPlaceDetails = intent.getSerializableExtra(MainActivity.EXTRA_PLACE_DETAILS) as HappyPlaceEntity
        }

        if (happyPlaceDetails != null){
            setSupportActionBar(binding.toolbarHappyPlaceDetail)
            supportActionBar!!.setDisplayHomeAsUpEnabled(true)
            supportActionBar!!.title = happyPlaceDetails.title

            binding.toolbarHappyPlaceDetail.setNavigationOnClickListener {
                onBackPressed()
            }
            binding.ivPlaceImage.setImageURI(Uri.parse(happyPlaceDetails.image))
            binding.tvDescription.text = happyPlaceDetails.description
            binding.tvLocation.text = happyPlaceDetails.location
            binding.tvDate.text = happyPlaceDetails.date

            binding.btnViewOnMap.setOnClickListener {
                val intent = Intent(this,MapActivity::class.java)
                intent.putExtra(MainActivity.EXTRA_PLACE_DETAILS,happyPlaceDetails)
                startActivity(intent)
            }
        }
    }
}
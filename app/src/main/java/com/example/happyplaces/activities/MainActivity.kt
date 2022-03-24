package com.example.happyplaces.activities


import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.happyplaces.HappyPlaceApp
import com.example.happyplaces.HappyPlaceEntity
import com.example.happyplaces.HappyPlacesAdapter
import com.example.happyplaces.SwipeToDeleteCallback
import com.example.happyplaces.databinding.ActivityMainBinding
import kotlinx.coroutines.launch
import pl.kitek.rvswipetodelete.SwipeToEditCallback

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.fabAddHappyPlace.setOnClickListener {
            val intent = Intent(this, AddHappyPlaceActivity::class.java)
            startActivity(intent)
        }

        getAllHappyPlacesList()
    }

    fun deleteRecord(id: Int){
        val happyPlaceDao = (application as HappyPlaceApp).db.happyPlaceDao()
        lifecycleScope.launch {
            happyPlaceDao.delete(HappyPlaceEntity(id))
            Toast.makeText(
                applicationContext,
                "Record deleted successfully.",
                Toast.LENGTH_LONG
            ).show()
        }
    }


    private fun getAllHappyPlacesList(){
        val happyPlaceDao = (application as HappyPlaceApp).db.happyPlaceDao()
        lifecycleScope.launch {
            happyPlaceDao.fetchAllHappyPlaces().collect { allHappyPlacesList ->
                if (allHappyPlacesList.isNotEmpty()){

                    binding.rvHappyPlacesList.visibility = View.VISIBLE
                    binding.tvNoRecordAvailable.visibility = View.GONE

                    val happyPlacesList = mutableListOf<HappyPlaceEntity>()

                    for (happyPlaces in allHappyPlacesList){
                        happyPlacesList.add(happyPlaces)
                    }

                    val happyPlacesAdapter = HappyPlacesAdapter(this@MainActivity,happyPlacesList)
                    binding.rvHappyPlacesList.adapter = happyPlacesAdapter

                    setupHappyPlacesRecyclerView(happyPlacesList)

                }else{
                    binding.rvHappyPlacesList.visibility = View.GONE
                    binding.tvNoRecordAvailable.visibility = View.VISIBLE
                }
            }
        }
    }

    private fun setupHappyPlacesRecyclerView(happyPlacesList: MutableList<HappyPlaceEntity>){
        val happyPlacesAdapter = HappyPlacesAdapter(this,happyPlacesList)
        binding.rvHappyPlacesList.adapter = happyPlacesAdapter
        binding.rvHappyPlacesList.layoutManager = LinearLayoutManager(this@MainActivity)
        binding.rvHappyPlacesList.setHasFixedSize(true)

        happyPlacesAdapter.setOnClickListener(object : HappyPlacesAdapter.OnClickListener {
            override fun onClick(position: Int, happyPlaceEntity: HappyPlaceEntity) {
                val intent = Intent(this@MainActivity, HappyPlaceDetailActivity::class.java)
                intent.putExtra(EXTRA_PLACE_DETAILS,happyPlaceEntity)
                startActivity(intent)
            }
        })

        val editSwipeHandler = object : SwipeToEditCallback(this){
            override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {
                val adapter = binding.rvHappyPlacesList.adapter as HappyPlacesAdapter
                adapter.notifyEditItem(this@MainActivity,viewHolder.adapterPosition,
                    ADD_PLACE_ACTIVITY_REQUEST_CODE
                )
            }
        }
        val editItemTouchHelper = ItemTouchHelper(editSwipeHandler)
        editItemTouchHelper.attachToRecyclerView(binding.rvHappyPlacesList)

        val deleteSwipeHandler = object : SwipeToDeleteCallback(this) {
            override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {

                val adapter = binding.rvHappyPlacesList.adapter as HappyPlacesAdapter
                //deleteRecord(happyPlacesList[viewHolder.adapterPosition].id)
                //adapter.removeAt(viewHolder.adapterPosition)
                fun alertDialogForDeletePlace(title: String) {
                    val builder = AlertDialog.Builder(this@MainActivity)
                    //set title for alert dialog
                    builder.setTitle("Alert")
                    //set message for alert dialog
                    builder.setMessage("Are you sure you want to delete $title?")
                    builder.setIcon(android.R.drawable.ic_dialog_alert)
                    //performing positive action
                    builder.setPositiveButton("Yes") { dialogInterface, _ ->
                        dialogInterface.dismiss() // Dialog will be dismissed

                        deleteRecord(happyPlacesList[viewHolder.adapterPosition].id)
                        adapter.removeAt(viewHolder.adapterPosition)

                    }

                    //performing negative action
                    builder.setNegativeButton("No") { dialogInterface, _ ->
                        dialogInterface.dismiss() // Dialog will be dismissed
                    }
                    // Create the AlertDialog
                    val alertDialog: AlertDialog = builder.create()
                    // Set other dialog properties
                    alertDialog.setCancelable(false) // Will not allow user to cancel after clicking on remaining screen area.
                    alertDialog.show()  // show the dialog to UI
                }
                alertDialogForDeletePlace(happyPlacesList[viewHolder.adapterPosition].title)
            }
        }
        val deleteItemTouchHelper = ItemTouchHelper(deleteSwipeHandler)
        deleteItemTouchHelper.attachToRecyclerView(binding.rvHappyPlacesList)
    }



    companion object{
        private const val ADD_PLACE_ACTIVITY_REQUEST_CODE = 1
        var EXTRA_PLACE_DETAILS = "extra_place_details"
    }
}
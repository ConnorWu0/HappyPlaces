package com.example.happyplaces

import android.content.Context
import android.location.Address
import android.location.Geocoder
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Dispatchers.Main

import kotlinx.coroutines.launch

import kotlinx.coroutines.withContext

import java.util.*


import kotlin.text.StringBuilder


class GetAddressFromLatLng(
    context: Context,
    private val latitude: Double,
    private val longitude: Double){

    private val geocoder: Geocoder = Geocoder(context, Locale.getDefault())

    private lateinit var myAddressListener: AddressListener




    suspend fun launchBackgroundProcessForRequest() {
            val address = getAddress()
            withContext(Main){
                //switch to Main thread, cuz we're going to update the UI related values from here on
                // if we get a valid address
                if (address.isEmpty()) {
                    myAddressListener.onError()
                } else {
                    myAddressListener.onAddressFound(address)  //updating UI
                }
            }
    }


    private suspend fun getAddress():String = withContext(Dispatchers.IO){
        try {
            //there may be multiple locations/places associated with the lat and lng, we take the top/most relevant address
            val addressList:List<Address>?= geocoder.getFromLocation(latitude,longitude,1)

            if(!addressList.isNullOrEmpty()){
                val address:Address=addressList[0]
                val stringBuilder=StringBuilder()
                for(i in 0..address.maxAddressLineIndex){  //Returns the largest index currently in use to specify an address line.
                    stringBuilder.append(address.getAddressLine(i)+" ")
                }
                stringBuilder.deleteCharAt(stringBuilder.length-1)   //to remove the last " "

                return@withContext stringBuilder.toString()
            }
        }
        catch (e:Exception){
            e.printStackTrace()
        }
        return@withContext ""
    }





    fun setAddressListener(addressListener: AddressListener){
        this.myAddressListener = addressListener
    }

    interface AddressListener{
        fun onAddressFound(address:String?)
        fun onError()
    }
}
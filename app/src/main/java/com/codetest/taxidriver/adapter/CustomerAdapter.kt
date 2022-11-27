package com.codetest.taxidriver.adapter

import android.app.Activity
import android.content.Intent
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.app.ActivityOptionsCompat
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import coil.load
import com.codetest.taxidriver.LocationActivity
import com.codetest.taxidriver.databinding.CustomerItemBinding
import com.codetest.taxidriver.model.Customer
import com.codetest.taxidriver.utils.Constant
import com.google.android.gms.maps.model.LatLng

class CustomerAdapter(activity: Activity, driverName: String, currentLocation: LatLng) : ListAdapter<Customer,CustomerAdapter.MyViewHolder>(DiffCallBack) {
    private val activity = activity
    private val driverName = driverName
    val currentLocation = currentLocation

    class MyViewHolder(binding: CustomerItemBinding) : RecyclerView.ViewHolder(binding.root) {
        val binding = binding

        fun bind(customer: Customer,currentLocation: LatLng) {

            val customerLocation = LatLng(customer.latLog.latitude, customer.latLog.longitude)

            binding.apply {
                customerProfile.load(customer.customerPhoto){
                    crossfade(true)
                }
                customerName.text = customer.name
                customerPhNo.text = customer.phone
                customerDistance.text = Constant.getDistance(currentLocation,customerLocation)
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MyViewHolder {
        val binding = CustomerItemBinding.inflate(LayoutInflater.from(parent.context),parent,false)
        return MyViewHolder(binding)
    }

    override fun onBindViewHolder(holder: MyViewHolder, position: Int) {
        holder.bind(getItem(position),currentLocation)
        holder.itemView.setOnClickListener {

            //set transition
            it.transitionName = "location"

            val intent = Intent(activity,LocationActivity::class.java)
            intent.putExtra(Constant.LATITUDE,getItem(position).latLog.latitude)
            intent.putExtra(Constant.LONGITUDE,getItem(position).latLog.longitude)
            intent.putExtra(Constant.PROFILE,getItem(position).customerPhoto)
            intent.putExtra(Constant.CUSTOMER_NAME,getItem(position).name)
            intent.putExtra(Constant.DRIVER_NAME,driverName)

            val options = ActivityOptionsCompat.makeSceneTransitionAnimation(activity,it,it.transitionName)

            //intent.putExtra(Constant.PERSON,getItem(position) as Customer)
            activity.startActivity(intent,options.toBundle())
        }
    }

    companion object{
        private val DiffCallBack = object: DiffUtil.ItemCallback<Customer>(){
            override fun areItemsTheSame(oldItem: Customer, newItem: Customer): Boolean {
                return oldItem == newItem
            }

            override fun areContentsTheSame(oldItem: Customer, newItem: Customer): Boolean {
                return oldItem.name == newItem.name && oldItem.phone == newItem.phone
            }

        }
    }

}
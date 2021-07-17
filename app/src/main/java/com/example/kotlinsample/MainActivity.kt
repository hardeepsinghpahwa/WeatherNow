package com.example.kotlinsample

import android.Manifest
import android.content.pm.PackageManager
import android.location.Location
import android.os.AsyncTask
import android.os.Build
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.airbnb.lottie.LottieAnimationView
import com.example.kotlinsample.data.WeatherData
import com.google.android.gms.location.*
import org.json.JSONArray
import org.json.JSONObject
import java.net.URL
import java.text.SimpleDateFormat
import java.util.*
import kotlin.collections.ArrayList

class MainActivity : AppCompatActivity() {

    val API: String = "a15c520c473550f742cc2e0e67b483f2";
    lateinit var lottie: LottieAnimationView
    val nightstart: String = "07:00 PM"
    val nightend: String = "05:00 AM"
    var mylat: Double = 0.0
    var mylong: Double = 0.0
    lateinit var hourlyrecyclerview: RecyclerView
    lateinit var dailyrecyclerview: RecyclerView
    var hourlydetails = ArrayList<WeatherData>()
    var dailydetails = ArrayList<WeatherData>()

    private lateinit var fusedLocationClient: FusedLocationProviderClient

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        lottie = findViewById(R.id.image);
        hourlyrecyclerview = findViewById(R.id.hourlyrecyclerview)
        dailyrecyclerview = findViewById(R.id.dailyrecyclerview)

        requestLocationUpdates()
    }

    inner class get_Location_Name : AsyncTask<Void, Void, String>() {
        override fun doInBackground(vararg params: Void?): String? {
            var locationdetails: String?

            try {
                locationdetails =
                    URL("http://api.openweathermap.org/geo/1.0/reverse?lat=${mylat}&lon=${mylong}&limit=1&appid=${API}").readText(
                        Charsets.UTF_8
                    )

            } catch (e: Exception) {
                print(e.message);
                locationdetails = null
            }

            return locationdetails
        }

        override fun onPostExecute(result: String?) {
            super.onPostExecute(result)

            var loc = JSONArray(result)

            findViewById<TextView>(R.id.place).setText(loc.getJSONObject(0).getString("name"));

        }

    }

    inner class Get_Weather : AsyncTask<String, Void, String>() {

        override fun onPreExecute() {
            super.onPreExecute()
        }

        override fun doInBackground(vararg params: String?): String? {

            var response: String?
            var locationdetails: String?

            try {
                response =
                    URL("https://api.openweathermap.org/data/2.5/onecall?lat=${mylat}&lon=${mylong}&exclude=minutely&appid=${API}&units=metric").readText(
                        Charsets.UTF_8
                    )

                get_Location_Name()

            } catch (e: Exception) {
                print(e.message);
                response = null
            }

            return response
        }


        override fun onPostExecute(result: String?) {
            super.onPostExecute(result)

            val dateformat = SimpleDateFormat("hh:mm aa")

            val startDate: Date = dateformat.parse(nightstart)
            val enddate: Date = dateformat.parse(nightend)
            val currentdate: Date = dateformat.parse(dateformat.format(Date()))

            findViewById<ConstraintLayout>(R.id.layout).visibility = View.VISIBLE
            findViewById<ProgressBar>(R.id.progressBar).visibility = View.GONE

            try {
                val jsonresult = JSONObject(result)
                val main = jsonresult.getJSONObject("current")
                val weather = main.getJSONArray("weather")

                val hourly = jsonresult.getJSONArray("hourly")

                val daily = jsonresult.getJSONArray("daily")


                hourlydetails.clear()
                dailydetails.clear()

                for (i in 1..(hourly.length())) {
                    var ob = hourly.getJSONObject(i - 1)
                    var weather = ob.getJSONArray("weather").getJSONObject(0)
                    hourlydetails.add(
                        WeatherData(
                            ob.getString("dt"),
                            ob.getString("temp"),
                            weather.getString("id")
                        )
                    )
                }

                for (i in 1..(daily.length())) {
                    var ob = daily.getJSONObject(i - 1)
                    var weather = ob.getJSONArray("weather").getJSONObject(0)
                    dailydetails.add(
                        WeatherData(
                            ob.getString("dt"),
                            weather.getString("main"),
                            weather.getString("id")
                        )
                    )
                }

                print(hourlydetails.size)
                hourlyrecyclerview.layoutManager =
                    LinearLayoutManager(applicationContext, LinearLayoutManager.HORIZONTAL, false)
                hourlyrecyclerview.adapter = HourlyAdapter(hourlydetails, true)

                dailyrecyclerview.layoutManager =
                    LinearLayoutManager(applicationContext, LinearLayoutManager.HORIZONTAL, false)
                dailyrecyclerview.adapter = HourlyAdapter(dailydetails, false)

                val weatherdetails = weather.getJSONObject(0)

                var format = SimpleDateFormat("dd MMMM yyyy");

                findViewById<TextView>(R.id.date).setText(format.format(Date()));

                findViewById<TextView>(R.id.temp).setText(
                    main.getString("temp").substring(0, main.getString("temp").indexOf(".")) + "°C"
                );
                findViewById<TextView>(R.id.humidity).setText(main.getString("humidity") + "%");
                findViewById<TextView>(R.id.status).setText(weatherdetails.getString("main"));
                findViewById<TextView>(R.id.wind).setText(
                    Math.round(
                        main.getString("wind_speed").toFloat() * 3.6
                    ).toString() + " km/hr"
                );
                findViewById<TextView>(R.id.pressure).setText(main.getString("pressure") + " hPa");

                val id = Integer.valueOf(weatherdetails.getString("id"))

                if (id >= 200 && id <= 232) {
                    lottie.setAnimation(R.raw.thunder)
                    lottie.playAnimation()
                    findViewById<ConstraintLayout>(R.id.mainlayout).setBackgroundResource(R.drawable.cloudy)
                } else if (id >= 300 && id <= 321) {
                    lottie.setAnimation(R.raw.drizzle)
                    lottie.playAnimation()
                    findViewById<ConstraintLayout>(R.id.mainlayout).setBackgroundResource(R.drawable.rainy)
                } else if (id >= 500 && id <= 531) {
                    lottie.setAnimation(R.raw.rain)
                    lottie.playAnimation()
                    findViewById<ConstraintLayout>(R.id.mainlayout).setBackgroundResource(R.drawable.rainy)
                } else if (id >= 600 && id <= 622) {
                    lottie.setAnimation(R.raw.snow)
                    lottie.playAnimation()
                    lottie.speed = 2f
                    findViewById<ConstraintLayout>(R.id.mainlayout).setBackgroundResource(R.drawable.cloudy)
                } else if (id >= 701 && id <= 781) {
                    lottie.setAnimation(R.raw.hazesmoke)
                    lottie.playAnimation()
                    findViewById<ConstraintLayout>(R.id.mainlayout).setBackgroundResource(R.drawable.cloudy)
                } else if (id > 800) {
                    if (currentdate.after(startDate) || currentdate.before(enddate)) {
                        lottie.setAnimation(R.raw.cloudynight)
                        lottie.playAnimation()
                        findViewById<ConstraintLayout>(R.id.mainlayout).setBackgroundResource(R.drawable.cloudy)

                    } else {
                        lottie.setAnimation(R.raw.cloudy)
                        lottie.playAnimation()
                        findViewById<ConstraintLayout>(R.id.mainlayout).setBackgroundResource(R.drawable.cloudy)
                    }
                } else {
                    if (currentdate.after(startDate) || currentdate.before(enddate)) {
                        lottie.setAnimation(R.raw.clearnight)
                        lottie.playAnimation()
                        findViewById<ConstraintLayout>(R.id.mainlayout).setBackgroundResource(R.drawable.cloudy)

                    } else {
                        lottie.setAnimation(R.raw.sunny)
                        lottie.playAnimation()
                        findViewById<ConstraintLayout>(R.id.mainlayout).setBackgroundResource(R.drawable.sunny)
                    }
                }


            } catch (e: Exception) {
                println("Exception : " + e.message)
            }
        }

    }


/*
    private fun getStatusImage(id:Integer) {
        if (id >= 200 && id <= 232) {
            lottie?.setAnimation(R.raw.thunder)
            lottie?.playAnimation()
            findViewById<ConstraintLayout>(R.id.mainlayout).setBackgroundResource(R.drawable.cloudy)
        } else if (id >= 300 && id <= 321) {
            lottie?.setAnimation(R.raw.drizzle)
            lottie?.playAnimation()
            findViewById<ConstraintLayout>(R.id.mainlayout).setBackgroundResource(R.drawable.rainy)
        } else if (id >= 500 && id <= 531) {
            lottie?.setAnimation(R.raw.rain)
            lottie?.playAnimation()
            findViewById<ConstraintLayout>(R.id.mainlayout).setBackgroundResource(R.drawable.rainy)
        } else if (id >= 600 && id <= 622) {
            lottie?.setAnimation(R.raw.snow)
            lottie?.playAnimation()
            lottie?.speed = 2f
            findViewById<ConstraintLayout>(R.id.mainlayout).setBackgroundResource(R.drawable.cloudy)
        } else if (id >= 701 && id <= 781) {
            lottie?.setAnimation(R.raw.hazesmoke)
            lottie?.playAnimation()
            findViewById<ConstraintLayout>(R.id.mainlayout).setBackgroundResource(R.drawable.cloudy)
        } else if (id > 800) {
            if (currentdate.after(startDate) || currentdate.before(enddate)) {
                lottie?.setAnimation(R.raw.cloudynight)
                lottie?.playAnimation()
                findViewById<ConstraintLayout>(R.id.mainlayout).setBackgroundResource(R.drawable.cloudy)

            } else {
                lottie?.setAnimation(R.raw.cloudy)
                lottie?.playAnimation()
                findViewById<ConstraintLayout>(R.id.mainlayout).setBackgroundResource(R.drawable.cloudy)
            }
        } else {
            if (currentdate.after(startDate) || currentdate.before(enddate)) {
                lottie?.setAnimation(R.raw.clearnight)
                lottie?.playAnimation()
                findViewById<ConstraintLayout>(R.id.mainlayout).setBackgroundResource(R.drawable.cloudy)

            } else {
                lottie?.setAnimation(R.raw.sunny)
                lottie?.playAnimation()
                findViewById<ConstraintLayout>(R.id.mainlayout).setBackgroundResource(R.drawable.sunny)
            }
        }
    }
*/


    private fun requestLocationUpdates() {

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)

        val permission = ContextCompat.checkSelfPermission(
            this,
            Manifest.permission.ACCESS_FINE_LOCATION
        )
        if (permission == PackageManager.PERMISSION_GRANTED) { // Request location updates and when an update is
            // received, store the location in Firebase
            fusedLocationClient.lastLocation
                .addOnSuccessListener { location: Location? ->
                    if (location != null) {
                        Get_Weather().execute()
                    } else {
                        println("location null")
                    }
                }
        } else {
            requestLocationPermission()
        }
    }

    private fun requestLocationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_BACKGROUND_LOCATION
                ),
                123
            )
        } else {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),
                123
            )
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        when (requestCode) {
            123 -> {
                // If request is cancelled, the result arrays are empty.
                if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {

                    // permission was granted, yay! Do the
                    // location-related task you need to do.
                    if (ContextCompat.checkSelfPermission(
                            this,
                            Manifest.permission.ACCESS_FINE_LOCATION
                        ) == PackageManager.PERMISSION_GRANTED
                    ) {
                        Get_Weather().execute()
                    }

                } else {

                    Toast.makeText(this, "permission denied", Toast.LENGTH_LONG).show()
                }
                return
            }
        }
    }


    inner class HourlyAdapter(
        private val dataSet: ArrayList<WeatherData>,
        private val hourly: Boolean
    ) :
        RecyclerView.Adapter<HourlyAdapter.ViewHolder>() {


        inner class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
            val time: TextView
            val temp: TextView
            val icon: ImageView

            init {
                // Define click listener for the ViewHolder's View.
                time = view.findViewById(R.id.time)
                temp = view.findViewById(R.id.temp)
                icon = view.findViewById(R.id.icon)

            }
        }

        // Create new views (invoked by the layout manager)
        override fun onCreateViewHolder(viewGroup: ViewGroup, viewType: Int): ViewHolder {
            // Create a new view, which defines the UI of the list item
            val view = LayoutInflater.from(viewGroup.context)
                .inflate(R.layout.weatheritem, viewGroup, false)

            return ViewHolder(view)
        }

        override fun onBindViewHolder(viewHolder: ViewHolder, position: Int) {


            if (hourly == true) {
                var time = Date((dataSet.get(position).time.toLong()) * 1000)
                val timeformat = SimpleDateFormat("hh:mm")
                viewHolder.time.setText(timeformat.format(time))

                if (dataSet.get(position).temp.contains("."))
                    viewHolder.temp.setText(
                        dataSet.get(position).temp.substring(
                            0,
                            dataSet.get(position).temp.indexOf(".")
                        ) + "°C"
                    )
                else
                    viewHolder.temp.setText(dataSet.get(position).temp + "°C")

            } else {
                var time = Date((dataSet.get(position).time.toLong()) * 1000)
                val timeformat = SimpleDateFormat("dd MMMM")
                viewHolder.time.setText(timeformat.format(time))

                viewHolder.temp.setText(dataSet.get(position).temp)
            }

            viewHolder.icon.setImageDrawable(
                ContextCompat.getDrawable(
                    applicationContext,
                    getIcon(Integer.valueOf(dataSet.get(position).id))
                )
            )

        }

        override fun getItemCount() = dataSet.size
    }


    fun getIcon(id: Int): Int {
        if (id >= 200 && id <= 232) {
            return R.drawable.thunderstormicon
        } else if (id >= 300 && id <= 321) {
            return R.drawable.rainicon
        } else if (id >= 500 && id <= 531) {
            return R.drawable.rainicon
        } else if (id >= 600 && id <= 622) {
            return R.drawable.snowicon
        } else if (id >= 701 && id <= 781) {
            return R.drawable.hazeicon
        } else if (id > 800) {
            return R.drawable.cloudyicon
        } else {
            val dateformat = SimpleDateFormat("hh:mm aa")

            val startDate: Date = dateformat.parse(nightstart)
            val enddate: Date = dateformat.parse(nightend)
            val currentdate: Date = dateformat.parse(dateformat.format(Date()))

            if (currentdate.after(startDate) || currentdate.before(enddate)) {
                return R.drawable.moonicon

            } else {
                return R.drawable.sunnyicon
            }
        }
    }
}
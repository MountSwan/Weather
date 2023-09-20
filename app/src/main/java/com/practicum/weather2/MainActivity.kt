package com.practicum.weather2

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.recyclerview.widget.LinearLayoutManager
import com.practicum.weather2.databinding.ActivityMainBinding
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

class MainActivity : AppCompatActivity() {

    private val forecaBaseUrl = "https://pfa.foreca.com"
    private var token = ""

    private val retrofit = Retrofit.Builder()
        .baseUrl(forecaBaseUrl)
        .addConverterFactory(GsonConverterFactory.create())
        .build()

    private val forecaService = retrofit.create(ForecaApi::class.java)

    private val locations = ArrayList<ForecastLocation>()
    private val adapter = LocationsAdapter {
        showWeather(it)
    }

    private lateinit var binding: ActivityMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        adapter.locations = locations

        binding.locations.layoutManager = LinearLayoutManager(this, LinearLayoutManager.VERTICAL, false)
        binding.locations.adapter = adapter

        binding.searchButton.setOnClickListener {
            if (binding.queryInput.text.isNotEmpty()) {
                if (token.isEmpty()) {
                    authenticate()
                } else {
                    search()
                }
            }
        }

    }

    private fun showMessage(text: String, additionalMessage: String) {
        if (text.isNotEmpty()) {
            binding.placeholderMessage.visibility = View.VISIBLE
            locations.clear()
            adapter.notifyDataSetChanged()
            binding.placeholderMessage.text = text
            if (additionalMessage.isNotEmpty()) {
                Toast.makeText(applicationContext, additionalMessage, Toast.LENGTH_LONG)
                    .show()
            }
        } else {
            binding.placeholderMessage.visibility = View.GONE
        }
    }

    private fun authenticate() {
        forecaService.authenticate(ForecaAuthRequest("sadness2", "hWQZhkWY0Qh4"))
            .enqueue(object : Callback<ForecaAuthResponse> {
                override fun onResponse(call: Call<ForecaAuthResponse>,
                                        response: Response<ForecaAuthResponse>
                ) {
                    if (response.code() == 200) {
                        token = response.body()?.token.toString()
                        search()
                    } else {
                        showMessage(getString(R.string.something_went_wrong), response.code().toString())
                    }
                }

                override fun onFailure(call: Call<ForecaAuthResponse>, t: Throwable) {
                    showMessage(getString(R.string.something_went_wrong), t.message.toString())
                }

            })
    }

    private fun search() {
        forecaService.getLocations("Bearer $token", binding.queryInput.text.toString())
            .enqueue(object : Callback<LocationsResponse> {
                override fun onResponse(call: Call<LocationsResponse>,
                                        response: Response<LocationsResponse>) {
                    when (response.code()) {
                        200 -> {
                            if (response.body()?.locations?.isNotEmpty() == true) {
                                locations.clear()
                                locations.addAll(response.body()?.locations!!)
                                adapter.notifyDataSetChanged()
                                showMessage("", "")
                            } else {
                                showMessage(getString(R.string.nothing_found), "")
                            }

                        }
                        401 -> authenticate()
                        else -> showMessage(getString(R.string.something_went_wrong), response.code().toString())
                    }

                }

                override fun onFailure(call: Call<LocationsResponse>, t: Throwable) {
                    showMessage(getString(R.string.something_went_wrong), t.message.toString())
                }

            })
    }

    private fun showWeather(location: ForecastLocation) {
        forecaService.getForecast("Bearer $token", location.id)
            .enqueue(object : Callback<ForecastResponse> {
                override fun onResponse(call: Call<ForecastResponse>,
                                        response: Response<ForecastResponse>) {
                    if (response.body()?.current != null) {
                        val message = "${location.name} t: ${response.body()?.current?.temperature}\n(Ощущается как ${response.body()?.current?.feelsLikeTemp})"
                        Toast.makeText(applicationContext, message, Toast.LENGTH_LONG).show()
                    }
                }

                override fun onFailure(call: Call<ForecastResponse>, t: Throwable) {
                    Toast.makeText(applicationContext, t.message, Toast.LENGTH_LONG).show()
                }

            })
    }

}


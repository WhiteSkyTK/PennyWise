package com.example.pennywise

import android.app.Activity
import android.content.Context
import android.util.Log
import com.google.android.gms.ads.AdError
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.FullScreenContentCallback
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.appopen.AppOpenAd
import java.util.Date

class AppOpenAdManager(private val context: Context) {

    private var appOpenAd: AppOpenAd? = null
    private var isLoadingAd = false
    var isShowingAd = false
    private var loadTime: Long = 0

    // Use your actual Ad Unit ID
    private val AD_UNIT_ID = "ca-app-pub-5040172786842435/7708652322"
    // private val AD_UNIT_ID = "YOUR_APP_OPEN_AD_UNIT_ID" // TODO: Replace with your real ID

    init {
        loadAd()
    }

    private fun loadAd() {
        if (isLoadingAd || isAdAvailable()) {
            return
        }

        isLoadingAd = true
        val request = AdRequest.Builder().build()
        AppOpenAd.load(
            context,
            AD_UNIT_ID,
            request,
            object : AppOpenAd.AppOpenAdLoadCallback() {
                override fun onAdLoaded(ad: AppOpenAd) {
                    appOpenAd = ad
                    isLoadingAd = false
                    loadTime = Date().time
                    Log.d("AppOpenAdManager", "App Open Ad loaded.")
                }

                override fun onAdFailedToLoad(loadAdError: LoadAdError) {
                    isLoadingAd = false
                    appOpenAd = null // Important to nullify
                    Log.e("AppOpenAdManager", "App Open Ad failed to load: ${loadAdError.message}")
                }
            }
        )
    }

    private fun isAdAvailable(): Boolean {
        // Ad is considered available if it's loaded and not expired (e.g., within 4 hours).
        return appOpenAd != null && (Date().time - loadTime < (3.9 * 60 * 60 * 1000)) // 3.9 hours
    }

    fun showAdIfAvailable(activity: Activity, onShowFullScreenContent: () -> Unit = {}) {
        if (isShowingAd) {
            Log.d("AppOpenAdManager", "The app open ad is already showing.")
            return
        }

        if (!isAdAvailable()) {
            Log.d("AppOpenAdManager", "The app open ad is not ready yet.")
            loadAd() // Try to load another one for next time
            onShowFullScreenContent() // Proceed without ad
            return
        }

        appOpenAd?.fullScreenContentCallback = object : FullScreenContentCallback() {
            override fun onAdDismissedFullScreenContent() {
                // Called when fullscreen content is dismissed.
                // Set the reference to null so isAdAvailable() returns false.
                appOpenAd = null
                isShowingAd = false
                Log.d("AppOpenAdManager", "App Open Ad dismissed.")
                onShowFullScreenContent()
                loadAd() // Preload the next ad
            }

            override fun onAdFailedToShowFullScreenContent(adError: AdError) {
                // Called when fullscreen content failed to show.
                // Set the reference to null so isAdAvailable() returns false.
                appOpenAd = null
                isShowingAd = false
                Log.e("AppOpenAdManager", "App Open Ad failed to show: ${adError.message}")
                onShowFullScreenContent()
                loadAd() // Preload the next ad
            }

            override fun onAdShowedFullScreenContent() {
                // Called when fullscreen content is shown.
                Log.d("AppOpenAdManager", "App Open Ad showed.")
            }
        }
        isShowingAd = true
        appOpenAd?.show(activity)
    }
}
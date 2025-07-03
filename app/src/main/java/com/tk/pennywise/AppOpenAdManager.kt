package com.tk.pennywise


import android.app.Activity
import android.content.Context
import android.util.Log
import com.google.android.gms.ads.AdError
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.FullScreenContentCallback
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.MobileAds
import com.google.android.gms.ads.appopen.AppOpenAd
import java.util.Date

class AppOpenAdManager(private val appContext: Context) {

    private var isMobileAdsSdkInitialized = false
    private var appOpenAd: AppOpenAd? = null
    private var isLoadingAd = false
    var isShowingAd = false
    private var loadTime: Long = 0

    init {
        MobileAds.initialize(appContext) { initializationStatus ->
            Log.d("AppOpenAdManager", "Mobile Ads SDK Initialized. Status: $initializationStatus")
            isMobileAdsSdkInitialized = true
            // Now it's safer to start loading ads
            loadAd()
        }
    }

    private fun loadAd() {
        if (!isMobileAdsSdkInitialized) {
            Log.d("AppOpenAdManager", "SDK not initialized yet. Cannot load ad.")
            return
        }
        if (isLoadingAd || isAdAvailable()) {
            return
        }

        isLoadingAd = true
        val adUnitId = BuildConfig.APP_OPEN_AD_UNIT_ID // <--- CORRECTED
        Log.d("AdMob_AppOpen", "Loading App Open Ad with Unit ID: $adUnitId")
        val request = AdRequest.Builder().build()
        AppOpenAd.load(
            appContext,
            adUnitId,
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
                    Log.e(
                        "AppOpenAdManager",
                        "App Open Ad failed to load. Code: ${loadAdError.code}, Message: ${loadAdError.message}, Domain: ${loadAdError.domain}"
                    )
                }
            }
        )
    }


    private fun isAdAvailable(): Boolean {
        // Ad is considered available if it's loaded and not expired (e.g., within 4 hours).
        val available = appOpenAd != null && (Date().time - loadTime < (3.9 * 60 * 60 * 1000))
        if (appOpenAd == null) Log.d("AppOpenAdManager", "isAdAvailable: appOpenAd is null")
        else Log.d("AppOpenAdManager", "isAdAvailable: Ad is ${if(available) "available" else "expired"}")
        return available
    }

    fun showAdIfAvailable(activity: Activity, onShowFullScreenContentCallback: () -> Unit = {}) {
        if (isShowingAd) {
            Log.d("AppOpenAdManager", "The app open ad is already showing.")
            onShowFullScreenContentCallback() // Consider if you should still call this
            return
        }

        if (!isAdAvailable()) {
            Log.d("AppOpenAdManager", "The app open ad is not ready yet for showing.")
            onShowFullScreenContentCallback() // Proceed without ad
            loadAd() // Try to load another one for next time (already called in isAdAvailable check indirectly if it leads here, but can be explicit)
            return
        }

        appOpenAd?.fullScreenContentCallback = object : FullScreenContentCallback() {
            override fun onAdDismissedFullScreenContent() {
                appOpenAd = null
                isShowingAd = false
                Log.d("AppOpenAdManager", "App Open Ad dismissed.")
                onShowFullScreenContentCallback()
                loadAd() // Preload the next ad
            }

            override fun onAdFailedToShowFullScreenContent(adError: AdError) {
                appOpenAd = null
                isShowingAd = false
                Log.e("AppOpenAdManager", "App Open Ad failed to show: ${adError.message}, Code: ${adError.code}, Domain: ${adError.domain}")
                onShowFullScreenContentCallback()
                loadAd() // Preload the next ad
            }

            override fun onAdShowedFullScreenContent() {
                isShowingAd = true // Set this when ad is actually shown
                Log.d("AppOpenAdManager", "App Open Ad showed.")
                // Note: The original callback 'onShowFullScreenContentCallback' from the function parameter
                // is typically called on dismissal or failure to show.
                // If you need a callback immediately *before* showing, that's different.
                // If it's for *after* the ad content is displayed, this is the spot.
            }
        }
        // It's safer to set isShowingAd to true just before calling show or in onAdShowedFullScreenContent.
        // If show() itself fails before onAdShowedFullScreenContent, isShowingAd might be incorrectly true.
        // However, for App Open, onAdShowedFullScreenContent is usually reliable.
        Log.d("AppOpenAdManager", "Attempting to show App Open Ad.")
        appOpenAd?.show(activity)
    }
}
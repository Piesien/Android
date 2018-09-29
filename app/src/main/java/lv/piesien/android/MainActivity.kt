package lv.piesien.android

import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.isGone
import androidx.core.view.isVisible
import androidx.lifecycle.ViewModelProviders
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.TileOverlayOptions
import com.google.android.gms.maps.model.UrlTileProvider
import com.google.maps.android.clustering.ClusterManager
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.schedulers.Schedulers
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.android.synthetic.main.spot_info_window.view.*
import lv.piesien.android.data.Spot
import java.net.URL


class MainActivity : AppCompatActivity(), OnMapReadyCallback {

    private val viewModel by lazy { ViewModelProviders.of(this)[MainViewModel::class.java] }
    private lateinit var map: GoogleMap

    private var dialog: AlertDialog? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        setSupportActionBar(toolbar)

        val mapFragment = supportFragmentManager.findFragmentById(R.id.map) as SupportMapFragment
        mapFragment.getMapAsync(this)

        overlay_view.setOnClickListener { overlay_group.isGone = true }
    }

    override fun onMapReady(googleMap: GoogleMap) {
        fab.isVisible = true

        map = googleMap
        here()

        googleMap.animateCamera(CameraUpdateFactory.newLatLngZoom(LatLng(56.946285, 24.105078), 13f))

        with(googleMap.uiSettings) {
            isZoomControlsEnabled = false
            isScrollGesturesEnabled = true
            isRotateGesturesEnabled = false
            isTiltGesturesEnabled = false
        }

        val clusterManager = ClusterManager<Spot>(this, map)
        clusterManager.renderer = ClusterRenderer(this, map, clusterManager)

        map.setOnCameraIdleListener(clusterManager)

        viewModel.getState()
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe { (state, spots) -> onState(state, spots, clusterManager) }
    }

    private fun onState(mainState: MainState, spots: List<Spot>, clusterManager: ClusterManager<Spot>) {
        clusterManager.clearItems()

        map.setOnMarkerClickListener { marker ->
            val spot = spots.first { it.position == marker.position }

            dialog?.dismiss()
            dialog = AlertDialog.Builder(this)
                    .setView(layoutInflater.inflate(R.layout.spot_info_window, null).also { view ->
                        view.title_text_view.text = spot.title

                        if (spot.votes != null) {
                            view.votes_text_view.isVisible = true
                            view.votes_text_view.text = getString(R.string.vote_template, spot.votes)
                            view.vote_button.isVisible = true
                            view.vote_button.setOnClickListener {
                                viewModel.onVoteSpot(spot)
                                        .subscribeOn(Schedulers.io())
                                        .observeOn(AndroidSchedulers.mainThread())
                                        .subscribe {
                                            dialog?.dismiss()
                                            Toast.makeText(this, getString(R.string.you_just_voted_for, spot.title), Toast.LENGTH_SHORT).show()
                                        }
                            }
                        } else {
                            view.votes_text_view.isVisible = false
                            view.vote_button.isVisible = false
                        }
                    })
                    .show()

            true
        }

        when (mainState) {
            MainState.LIST -> {
                add_pin_view.isVisible = false
                clusterManager.addItems(spots)
                clusterManager.cluster()

                toolbar.navigationIcon = null
                toolbar.setNavigationOnClickListener(null)

                fab.setImageResource(R.drawable.ic_add_location)
                fab.setOnClickListener {
                    if (overlay_group.isVisible) {
                        viewModel.onState(MainState.ADD)
                        overlay_group.isGone = true
                    } else {
                        overlay_group.isVisible = true
                    }
                }
            }
            MainState.ADD -> {
                add_pin_view.isVisible = true
                clusterManager.cluster()

                toolbar.setNavigationIcon(R.drawable.ic_close)
                toolbar.setNavigationOnClickListener { viewModel.onState(MainState.LIST) }

                fab.setImageResource(R.drawable.ic_create)
                fab.setOnClickListener {
                    viewModel.onAddSpot(map.cameraPosition.target)
                            .subscribeOn(Schedulers.io())
                            .observeOn(AndroidSchedulers.mainThread())
                            .subscribe { viewModel.onState(MainState.LIST) }
                }
            }
        }
    }

    private fun here() {
        findViewById<ViewGroup>(R.id.map)
                .findViewWithTag<View>("GoogleWatermark")
                .visibility = View.GONE

        map.mapType = GoogleMap.MAP_TYPE_NONE
        map.addTileOverlay(TileOverlayOptions()
                .tileProvider(object : UrlTileProvider(256, 256) {
                    override fun getTileUrl(x: Int, y: Int, zoom: Int): URL? {
                        return URL("https://1.base.maps.api.here.com/maptile/2.1/maptile/newest/normal.day.grey/$zoom/$x/$y/256/png8?app_id=devportal-demo-20180625&app_code=9v2BkviRwi9Ot26kp2IysQ&ppi=250")
                    }
                }))
    }
}

package lv.piesien.android

import android.content.Context
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.MarkerOptions
import com.google.maps.android.clustering.Cluster
import com.google.maps.android.clustering.ClusterManager
import com.google.maps.android.clustering.view.DefaultClusterRenderer
import lv.piesien.android.data.Spot


/**
 * Created by krokyze on 05/02/2018.
 */
class ClusterRenderer(
        context: Context,
        googleMap: GoogleMap,
        clusterManager: ClusterManager<Spot>
) : DefaultClusterRenderer<Spot>(context, googleMap, clusterManager) {

    override fun onBeforeClusterItemRendered(spot: Spot, markerOptions: MarkerOptions) {
        when (spot.type) {
            "exists" -> markerOptions.icon(BitmapDescriptorFactory.fromResource(R.drawable.ic_place_exists))
            "r" -> markerOptions.icon(BitmapDescriptorFactory.fromResource(R.drawable.ic_place_r))
            else -> markerOptions.icon(BitmapDescriptorFactory.fromResource(R.drawable.ic_place_needed))
        }
    }

    override fun shouldRenderAsCluster(cluster: Cluster<Spot>?) = false
}

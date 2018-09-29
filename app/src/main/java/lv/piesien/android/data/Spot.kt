package lv.piesien.android.data

import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.clustering.ClusterItem
import kotlin.properties.Delegates

class Spot : ClusterItem {
    var name: String by Delegates.notNull()
    var description: String by Delegates.notNull()
    var type: String by Delegates.notNull()
    var latitude: Double by Delegates.notNull()
    var longitude: Double by Delegates.notNull()
    var votes: Int? = null

    override fun getTitle() = name

    override fun getSnippet() = description

    override fun getPosition() = LatLng(latitude, longitude)
}

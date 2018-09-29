package lv.piesien.android

import androidx.lifecycle.ViewModel
import com.google.android.gms.maps.model.LatLng
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import durdinapps.rxfirebase2.DataSnapshotMapper
import durdinapps.rxfirebase2.RxFirebaseDatabase
import io.reactivex.*
import io.reactivex.rxkotlin.Flowables
import io.reactivex.rxkotlin.Singles
import io.reactivex.subjects.BehaviorSubject
import lv.piesien.android.data.Spot


class MainViewModel : ViewModel() {

    private val database by lazy { FirebaseDatabase.getInstance().reference.child("spots") }
    private val stateSubject = BehaviorSubject.createDefault(MainState.LIST)

    fun getState(): Flowable<Pair<MainState, List<Spot>>> {
        val spots = database.observeValueEvent(DataSnapshotMapper.listOf(Spot::class.java))
        return Flowables.combineLatest(stateSubject.toFlowable(BackpressureStrategy.DROP), spots)
    }

    fun onState(state: MainState) {
        stateSubject.onNext(state)
    }

    fun onAddSpot(latLng: LatLng): Completable {
        return RxFirebaseDatabase.setValue(database.push(), Spot().apply {
            name = "Velonovietne"
            description = ""
            type = "needed"
            latitude = latLng.latitude
            longitude = latLng.longitude
            votes = 0
        })
    }

    fun onVoteSpot(spot: Spot): Completable {
        val query = database.orderByChild("latitude").equalTo(spot.latitude)

        return RxFirebaseDatabase.requestFilteredReferenceKeys(database, query)
                .flatMapObservable { references ->
                    Observable.fromIterable(references.toList())
                            .flatMapSingle { reference ->
                                Singles.zip(
                                        Single.just(reference),
                                        reference.child("longitude").observeValueEvent(DataSnapshotMapper.of(Double::class.java)).firstOrError()
                                )
                            }
                }
                .toList()
                .map { it.first { it.second == spot.longitude }.first }
                .flatMapCompletable { RxFirebaseDatabase.updateChildren(database.child(it.key!!), mapOf("votes" to spot.votes!! + 1)) }
    }

    private fun <T> DatabaseReference.observeValueEvent(mapper: io.reactivex.functions.Function<in DataSnapshot, out T>): Flowable<T> {
        return RxFirebaseDatabase.observeValueEvent(this, mapper)
    }
}

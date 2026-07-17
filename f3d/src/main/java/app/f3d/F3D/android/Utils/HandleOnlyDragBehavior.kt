package app.f3d.F3D.android.Utils

import android.content.Context
import android.util.AttributeSet
import android.view.View
import androidx.coordinatorlayout.widget.CoordinatorLayout
import com.google.android.material.bottomsheet.BottomSheetBehavior

class HandleOnlyDragBehavior<V : View?>(context: Context, attrs: AttributeSet) :
    BottomSheetBehavior<V>(context, attrs) {

    override fun onStartNestedScroll(
        coordinatorLayout: CoordinatorLayout,
        child: V & Any,
        directTargetChild: View,
        target: View,
        axes: Int,
        type: Int,
    ): Boolean = false
}

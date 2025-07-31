import android.app.Application
import android.app.FragmentManager

public class FragmentActivityLifecycleCallbacks(
    private val spanTracker: SpanTracker,
    private val spanFactory: SpanFactory,
    private val autoInstrumentationCache: AutoInstrumentationCache,
) : Application.ActivityLifecycleCallbacks, FragmentManager.FragmentLifecycleCallbacks() {

    private val viewLoadOpts = SpanOptions.makeCurrentContext(false)

    override fun onActivityCreated(
        activity: Activity,
        savedInstanceState: Bundle?,
    ) {
        if (activity !is FragmentActivity) return

        activity.supportFragmentManager
            .registerFragmentLifecycleCallbacks(this, true)
    }

    override fun onFragmentPreCreated(
        fm: FragmentManager,
        f: Fragment,
        savedInstanceState: Bundle?,
    ) {
        if (autoInstrumentationCache.isInstrumentationEnabled(f::class.java)) {
            // we start both ViewLoad & ViewLoadPhase/Create at exactly the same timestamp
            val timestamp = SystemClock.elapsedRealtimeNanos()
            val viewLoad =
                spanTracker.associate(f) {
                    spanFactory.createViewLoadSpan(
                        ViewType.FRAGMENT,
                        viewNameForFragment(f),
                        viewLoadOpts.startTime(timestamp),
                    )
                }

            spanTracker.associate(f, ViewLoadPhase.CREATE) {
                spanFactory.createViewLoadPhaseSpan(
                    viewNameForFragment(f),
                    ViewType.FRAGMENT,
                    ViewLoadPhase.CREATE,
                    SpanOptions.DEFAULTS
                        .within(viewLoad)
                        .startTime(timestamp),
                )
            }
        }
    }

    override fun onFragmentCreated(
        fm: FragmentManager,
        f: Fragment,
        savedInstanceState: Bundle?,
    ) {
        if (!f.isAdded) {
            // remove & discard the Fragment span
            spanTracker.removeAssociation(f, ViewLoadPhase.CREATE)?.discard()
            spanTracker.removeAssociation(f)?.discard()
        } else {
            spanTracker.endSpan(f, ViewLoadPhase.CREATE)
        }
    }

    override fun onFragmentResumed(
        fm: FragmentManager,
        f: Fragment,
    ) {
        spanTracker.endAllSpans(f)
    }

    override fun onActivityStarted(activity: Activity): Unit = Unit

    override fun onActivityResumed(activity: Activity): Unit = Unit

    override fun onActivityPaused(activity: Activity): Unit = Unit

    override fun onActivityStopped(activity: Activity): Unit = Unit

    override fun onActivitySaveInstanceState(
        activity: Activity,
        outState: Bundle,
    ): Unit = Unit

    override fun onActivityDestroyed(activity: Activity): Unit = Unit
}
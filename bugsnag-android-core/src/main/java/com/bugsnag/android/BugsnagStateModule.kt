package com.bugsnag.android

import com.bugsnag.android.internal.dag.ConfigModule
import com.bugsnag.android.internal.dag.DependencyModule

/**
 * A dependency module which constructs the objects that track state in Bugsnag. For example, this
 * class is responsible for creating classes which track the current breadcrumb/metadata state.
 */
internal class BugsnagStateModule(
    configModule: ConfigModule,
    configuration: Configuration
) : DependencyModule() {

    private val cfg = configModule.config

    val clientObservable = ClientObservable()

    val callbackState = configuration.impl.callbackState.copy()

    val contextState = ContextState().apply {
        if (configuration.context != null) {
            setManualContext(configuration.context)
        }
    }

    val breadcrumbState = BreadcrumbState(cfg.maxBreadcrumbs, callbackState, cfg.logger)

    val metadataState = copyMetadataState(configuration)

    val featureFlagState = configuration.impl.featureFlagState.copy()

    private fun copyMetadataState(configuration: Configuration): MetadataState {
        // performs deep copy of metadata to preserve immutability of Configuration interface
        val orig = configuration.impl.metadataState.metadata
        return configuration.impl.metadataState.copy(metadata = orig.copy())
    }
}

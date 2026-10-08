package com.eltavine.sereingram.core

/**
 * One SereinGram feature. Modules know nothing of each other; whatever they
 * share goes through [Options] or a hook.
 */
public interface SereinModule {
    /** Stable lower_snake_case name, used in logs. */
    public val id: String

    /** Every option the module reads, so they can be listed and checked for clashes. */
    public val options: List<Option<*>>
        get() = emptyList()

    /** Runs once while the application starts; install hook handlers here. */
    public fun start(context: ModuleContext) {}

    /**
     * Forgets whatever the module keeps for [account] apart from its options,
     * once the account has logged out and before another account may take its
     * place. Runs on the UI thread; slow work may go to the module's own thread.
     */
    public fun forgetAccount(account: Int) {}
}

public class ModuleContext(
    public val options: Options,
    /** Reports a failure the app keeps running through, for example a module that did not start. */
    public val report: (message: String, error: Throwable) -> Unit,
)

/** The modules of the app, in start order, checked for duplicate ids and option keys. */
public class ModuleRegistry(public val modules: List<SereinModule>) {
    public val options: List<Option<*>> = modules.flatMap { it.options }

    init {
        duplicates(modules.map { it.id })?.let { throw IllegalArgumentException("duplicate module id $it") }
        duplicates(options.map { it.key })?.let { throw IllegalArgumentException("duplicate option key $it") }
    }

    /** Starts every module; one that throws is reported and skipped, and the rest still start. */
    public fun start(context: ModuleContext) {
        modules.forEach { module -> guarded(context, "module ${module.id} failed to start") { module.start(context) } }
    }

    /** Resets [account]'s options and has every module forget what else it keeps for the account. */
    public fun forgetAccount(account: Int, context: ModuleContext) {
        options.filter { it.scope == OptionScope.ACCOUNT }.forEach { context.options.reset(it, account) }
        modules.forEach { module ->
            guarded(context, "module ${module.id} failed to forget an account") { module.forgetAccount(account) }
        }
    }

    private inline fun guarded(context: ModuleContext, failure: String, run: () -> Unit) {
        try {
            run()
        } catch (error: Throwable) {
            if (error is VirtualMachineError) {
                throw error
            }
            context.report(failure, error)
        }
    }

    private fun duplicates(values: List<String>): String? =
        values.groupingBy { it }.eachCount().entries.firstOrNull { it.value > 1 }?.key
}

package com.hideandseek.utils

import org.bukkit.Bukkit
import org.bukkit.plugin.Plugin
import org.bukkit.scheduler.BukkitTask

/**
 * Wrapper for Bukkit scheduler operations
 *
 * Provides convenient methods for sync/async task execution
 */
class TaskScheduler(private val plugin: Plugin) {

    /**
     * Run task on next tick (sync with main thread)
     *
     * @param task Task to run
     * @return BukkitTask handle
     */
    fun runTask(task: () -> Unit): BukkitTask {
        return Bukkit.getScheduler().runTask(plugin, Runnable { task() })
    }

    /**
     * Run task after delay (sync with main thread)
     *
     * @param delay Delay in ticks (20 ticks = 1 second)
     * @param task Task to run
     * @return BukkitTask handle
     */
    fun runTaskLater(delay: Long, task: () -> Unit): BukkitTask {
        return Bukkit.getScheduler().runTaskLater(plugin, Runnable { task() }, delay)
    }

    /**
     * Run repeating task (sync with main thread)
     *
     * @param delay Initial delay in ticks
     * @param period Period between executions in ticks
     * @param task Task to run
     * @return BukkitTask handle
     */
    fun runTaskTimer(delay: Long, period: Long, task: () -> Unit): BukkitTask {
        return Bukkit.getScheduler().runTaskTimer(plugin, Runnable { task() }, delay, period)
    }

    /**
     * Run task asynchronously (off main thread)
     *
     * WARNING: Cannot use Bukkit API from async tasks!
     * Use for database, file I/O, HTTP requests, etc.
     *
     * @param task Task to run
     * @return BukkitTask handle
     */
    fun runTaskAsync(task: () -> Unit): BukkitTask {
        return Bukkit.getScheduler().runTaskAsynchronously(plugin, Runnable { task() })
    }

    /**
     * Run task asynchronously after delay
     *
     * WARNING: Cannot use Bukkit API from async tasks!
     *
     * @param delay Delay in ticks
     * @param task Task to run
     * @return BukkitTask handle
     */
    fun runTaskLaterAsync(delay: Long, task: () -> Unit): BukkitTask {
        return Bukkit.getScheduler().runTaskLaterAsynchronously(plugin, Runnable { task() }, delay)
    }

    /**
     * Run repeating task asynchronously
     *
     * WARNING: Cannot use Bukkit API from async tasks!
     *
     * @param delay Initial delay in ticks
     * @param period Period between executions in ticks
     * @param task Task to run
     * @return BukkitTask handle
     */
    fun runTaskTimerAsync(delay: Long, period: Long, task: () -> Unit): BukkitTask {
        return Bukkit.getScheduler().runTaskTimerAsynchronously(plugin, Runnable { task() }, delay, period)
    }

    /**
     * Cancel all tasks for this plugin
     */
    fun cancelAll() {
        Bukkit.getScheduler().cancelTasks(plugin)
    }
}

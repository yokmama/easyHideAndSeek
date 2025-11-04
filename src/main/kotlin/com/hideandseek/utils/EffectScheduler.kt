package com.hideandseek.utils

import org.bukkit.plugin.Plugin
import org.bukkit.scheduler.BukkitTask

/**
 * Utility for scheduling effect-related tasks
 * Manages BukkitScheduler tasks for effect expiration and cleanup
 */
class EffectScheduler(private val plugin: Plugin) {
    /**
     * Active scheduled tasks (taskId -> BukkitTask)
     * Used for cancellation when effects are manually removed
     */
    private val tasks = mutableMapOf<Int, BukkitTask>()

    /**
     * Schedule a task to run after a delay
     *
     * @param delaySeconds Delay in seconds
     * @param callback Function to execute when delay expires
     * @return Task ID (can be used to cancel)
     */
    fun scheduleDelayed(delaySeconds: Int, callback: () -> Unit): Int {
        val task = plugin.server.scheduler.runTaskLater(
            plugin,
            Runnable { callback() },
            delaySeconds * 20L // Convert seconds to ticks (20 ticks/second)
        )

        tasks[task.taskId] = task
        return task.taskId
    }

    /**
     * Schedule a repeating task
     *
     * @param intervalSeconds Interval in seconds
     * @param callback Function to execute on each interval
     * @return Task ID (can be used to cancel)
     */
    fun scheduleRepeating(intervalSeconds: Int, callback: () -> Unit): Int {
        val task = plugin.server.scheduler.runTaskTimer(
            plugin,
            Runnable { callback() },
            intervalSeconds * 20L, // Initial delay
            intervalSeconds * 20L  // Repeat interval
        )

        tasks[task.taskId] = task
        return task.taskId
    }

    /**
     * Cancel a scheduled task
     *
     * @param taskId Task ID returned from schedule method
     * @return true if task was found and cancelled
     */
    fun cancelTask(taskId: Int): Boolean {
        val task = tasks.remove(taskId) ?: return false
        task.cancel()
        return true
    }

    /**
     * Cancel all scheduled tasks
     * Should be called during plugin disable or game reset
     */
    fun cancelAll() {
        tasks.values.forEach { it.cancel() }
        tasks.clear()
    }

    /**
     * Get number of active scheduled tasks
     * @return Active task count
     */
    fun getActiveTaskCount(): Int = tasks.size

    /**
     * Clean up completed tasks from tracking
     * Removes tasks that are no longer scheduled
     */
    fun cleanupCompletedTasks() {
        val iterator = tasks.iterator()
        while (iterator.hasNext()) {
            val entry = iterator.next()
            if (entry.value.isCancelled) {
                iterator.remove()
            }
        }
    }
}

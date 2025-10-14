package com.ustadmobile.meshrabiya.sensor.meshrabiya

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.IBinder
import android.os.Handler
import android.os.Looper
import com.ustadmobile.meshrabiya.api.IMeshrabiyaService
import java.util.concurrent.ArrayBlockingQueue
import java.util.concurrent.TimeUnit

/**
 * Small helper to bind to the Meshrabiya AIDL service and fetch the onion pubkey.
 * This is deliberately blocking with a short timeout to make callers simpler.
 */
object MeshrabiyaAidlClient {

    private const val ACTION_BIND = "com.ustadmobile.meshrabiya.ACTION_BIND"
    private const val BIND_TIMEOUT_MS = 3000L

    fun fetchOnionPubKeyBlocking(context: Context): String? {
        val queue = ArrayBlockingQueue<IMeshrabiyaService?>(1)

        val intent = Intent(ACTION_BIND).setPackage(context.packageName)

        val conn = object : ServiceConnection {
            override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
                try {
                    val stub = IMeshrabiyaService.Stub.asInterface(service)
                    queue.offer(stub)
                } catch (t: Throwable) {
                    queue.offer(null)
                }
            }

            override fun onServiceDisconnected(name: ComponentName?) {
                // no-op
            }
        }

        // Bind on main looper (Android requires bindService be called on a Looper thread)
        val handler = Handler(Looper.getMainLooper())
        var bound = false
        try {
            val runnable = Runnable {
                bound = context.applicationContext.bindService(intent, conn, Context.BIND_AUTO_CREATE)
            }
            if (Looper.myLooper() == Looper.getMainLooper()) runnable.run() else handler.post(runnable)

            val stub = queue.poll(BIND_TIMEOUT_MS, TimeUnit.MILLISECONDS)
            val pub = try { stub?.getOnionPubKey() } catch (_: Exception) { null }

            // Unbind if we bound
            if (bound) {
                val unbindRunnable = Runnable { try { context.applicationContext.unbindService(conn) } catch (_: Exception) {} }
                if (Looper.myLooper() == Looper.getMainLooper()) unbindRunnable.run() else handler.post(unbindRunnable)
            }

            return pub
        } catch (e: InterruptedException) {
            return null
        }
    }
}

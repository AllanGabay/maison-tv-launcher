package fr.maisontv.launcher;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.os.Handler;
import android.util.Log;

/** One boot event only; inactive if the user restores a different home screen. */
public final class BootReceiver extends BroadcastReceiver {
    @Override public void onReceive(final Context context, Intent event) {
        if (!Intent.ACTION_BOOT_COMPLETED.equals(event.getAction())) return;
        final PendingResult result = goAsync();
        new Handler().postDelayed(new Runnable() {
            @Override public void run() {
                try {
                    Intent home = new Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_HOME);
                    ResolveInfo selected = context.getPackageManager().resolveActivity(home, PackageManager.MATCH_DEFAULT_ONLY);
                    if (selected == null || !context.getPackageName().equals(selected.activityInfo.packageName)) return;
                    Log.i("MaisonBoot", "Opening selected family home after boot");
                    Intent launch = new Intent(context, HomeActivity.class);
                    launch.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED);
                    context.startActivity(launch);
                } catch (Exception e) {
                    Log.w("MaisonBoot", "Could not open family home", e);
                } finally {
                    result.finish();
                }
            }
        }, 2000L);
    }
}

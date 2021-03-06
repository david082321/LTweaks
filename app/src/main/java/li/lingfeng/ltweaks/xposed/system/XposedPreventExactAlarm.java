package li.lingfeng.ltweaks.xposed.system;

import android.os.Build;

import de.robv.android.xposed.XC_MethodHook;
import li.lingfeng.ltweaks.R;
import li.lingfeng.ltweaks.lib.XposedLoad;
import li.lingfeng.ltweaks.prefs.ClassNames;
import li.lingfeng.ltweaks.prefs.PackageNames;
import li.lingfeng.ltweaks.utils.Logger;

/**
 * Created by smallville on 2017/3/25.
 */
@XposedLoad(packages = PackageNames.ANDROID, prefs = {})
public class XposedPreventExactAlarm extends XposedPreventRunning {

    private static final long WINDOW_EXACT = 0;
    private static final long WINDOW_HEURISTIC = -1;
    private static final int FLAG_ALLOW_WHILE_IDLE = 1<<2;

    private static final int INDEX_WINDOW_MILLIS = 2;
    private static final int INDEX_FLAGS = Build.VERSION.SDK_INT >= Build.VERSION_CODES.N ? 7 : 5;

    @Override
    protected int getPreventListKey() {
        return R.string.key_prevent_list_prevent_exact_alarm;
    }

    @Override
    protected void handleLoadPackage() throws Throwable {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) {
            return;
        }
        super.handleLoadPackage();
        if (mPreventList.isEmpty()) {
            return;
        }

        hookAllMethods(ClassNames.ALARM_MANAGER_SERVICE, "setImpl", new XC_MethodHook() {
            @Override
            protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                int uidIndex = Build.VERSION.SDK_INT >= Build.VERSION_CODES.N ? param.args.length - 2 : param.args.length - 1;
                int uid = (int) param.args[uidIndex];
                if (mPreventUids.contains(uid)) {
                    long windowMillis = (long) param.args[INDEX_WINDOW_MILLIS];
                    int flags = (int) param.args[INDEX_FLAGS];
                    //Logger.d("Alarm from " + uid + " " + windowMillis + " " + Integer.toBinaryString(flags));

                    boolean isSet = false;
                    if (windowMillis >= 0) {  // 0 is WINDOW_EXACT, >0 is no later in milliseconds.
                        windowMillis = WINDOW_HEURISTIC;
                        param.args[INDEX_WINDOW_MILLIS] = windowMillis;
                        isSet = true;
                    }
                    if ((flags & FLAG_ALLOW_WHILE_IDLE) != 0) {
                        flags &= ~FLAG_ALLOW_WHILE_IDLE;
                        param.args[INDEX_FLAGS] = flags;
                        isSet = true;
                    }

                    if (isSet) {
                        Logger.i("Alarm from " + uid + " is set to " + windowMillis + ", " + Integer.toBinaryString(flags));
                    }
                }
            }
        });
    }
}

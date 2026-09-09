package com.motorola.netspeed.service;

import android.content.ComponentName;
import android.content.Context;
import android.content.ServiceConnection;
import android.os.IBinder;

import com.motorola.netspeed.BuildConfig;
import com.motorola.netspeed.IUserService;

import rikka.shizuku.Shizuku;

public final class ShizukuServiceManager {

    public interface Callback {
        void onConnected(IUserService service);
        void onDisconnected();
    }

    private IUserService userService;
    private Callback callback;
    private final Shizuku.UserServiceArgs args;

    public ShizukuServiceManager(Context context) {
        args = new Shizuku.UserServiceArgs(
                new ComponentName(context, UserService.class)
        )
            .daemon(false)
            .processNameSuffix("moto_service")
            .debuggable(BuildConfig.DEBUG)
            .version(BuildConfig.VERSION_CODE);
    }

    private final ServiceConnection connection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            userService = IUserService.Stub.asInterface(service);
            if (callback != null) callback.onConnected(userService);
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            userService = null;
            if (callback != null) callback.onDisconnected();
        }
    };

    public void setCallback(Callback callback) {
        this.callback = callback;
    }

    public boolean bind() {
        if (!Shizuku.pingBinder()) return false;
        if (Shizuku.checkSelfPermission() != android.content.pm.PackageManager.PERMISSION_GRANTED) {
            return false;
        }
        try {
            Shizuku.bindUserService(args, connection);
            return true;
        } catch (RuntimeException e) {
            return false;
        }
    }

    public void unbind() {
        try {
            Shizuku.unbindUserService(args, connection, true);
        } catch (RuntimeException ignored) {
        }
        userService = null;
    }

    public IUserService getService() {
        return userService;
    }
}

package com.motorola.netspeed.service;

import android.os.SystemClock;

import com.motorola.netspeed.IUserService;

import java.io.BufferedReader;
import java.io.InputStreamReader;

public class UserService extends IUserService.Stub {

    @Override
    public void destroy() {
        System.exit(0);
    }

    @Override
    public boolean setInternetSpeed(boolean enable) {
        String value = enable ? "1" : "0";
        return runCommand(new String[] {
                "/system/bin/cmd",
                "motsettings",
                "put",
                "global",
                "internet_speed_switch",
                value
        }) == 0;
    }

    @Override
    public boolean getInternetSpeed() {
        CommandResult result = runCommandWithOutput(new String[] {
                "/system/bin/cmd",
                "motsettings",
                "get",
                "global",
                "internet_speed_switch"
        });
        return result.exitCode == 0 && "1".equals(result.output.trim());
    }

    private int runCommand(String[] command) {
        try {
            Process process = new ProcessBuilder(command)
                    .redirectErrorStream(true)
                    .start();
            process.getInputStream().close();
            return process.waitFor();
        } catch (Exception e) {
            return -1;
        }
    }

    private CommandResult runCommandWithOutput(String[] command) {
        try {
            Process process = new ProcessBuilder(command)
                    .redirectErrorStream(false)
                    .start();

            StringBuilder output = new StringBuilder();
            try (BufferedReader reader =
                         new BufferedReader(new InputStreamReader(process.getInputStream()))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    if (output.length() > 0) output.append('\n');
                    output.append(line);
                }
            }

            int exitCode = process.waitFor();
            return new CommandResult(exitCode, output.toString());
        } catch (Exception e) {
            return new CommandResult(-1, "");
        }
    }

    private static final class CommandResult {
        final int exitCode;
        final String output;

        CommandResult(int exitCode, String output) {
            this.exitCode = exitCode;
            this.output = output;
        }
    }
}

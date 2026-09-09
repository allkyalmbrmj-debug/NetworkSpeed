package com.motorola.netspeed;

interface IUserService {
    void destroy() = 16777114;
    boolean setInternetSpeed(boolean enable) = 2;
    boolean getInternetSpeed() = 3;
}

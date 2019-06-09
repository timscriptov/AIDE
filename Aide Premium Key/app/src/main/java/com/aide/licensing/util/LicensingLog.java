package com.aide.licensing.util;

import android.util.Log;

public class LicensingLog
{

    public static void d(String msg)
	{
    }

    public static void d(Object obj)
	{
    }

    public static void d(int i)
	{
    }

    public static void s(Object obj, String method)
	{
        Log.i("aidelicensing", obj.getClass().getName() + "." + method);
    }

    public static void s(String state)
	{
        Log.i("aidelicensing", state);
    }

    public static void e(Throwable t)
	{
        Log.e("aidelicensing", t.toString(), t);
    }

    public static void e(String msg)
	{
        Log.e("aidelicensing", msg);
    }

    public static void e(String msg, Throwable t)
	{
        Log.e("aidelicensing", msg, t);
    }

    public static void w(String msg)
	{
        Log.w("aidelicensing", msg);
    }

    public static void crash(Throwable t)
	{
        Log.e("aidelicensing", t.toString(), t);
    }
}

package com.aide.licensing;

import android.os.Binder;
import android.os.IBinder;
import android.os.IInterface;
import android.os.Parcel;
import android.os.RemoteException;

public interface IAideLicenseResultListener extends IInterface
{

    public static abstract class Stub extends Binder implements IAideLicenseResultListener
	{
        private static final String DESCRIPTOR = "com.aide.licensing.IAideLicenseResultListener";
        static final int TRANSACTION_licenseNotVerified = 2;
        static final int TRANSACTION_licenseVerified = 1;

        private static class Proxy implements IAideLicenseResultListener
		{
            private IBinder mRemote;

            Proxy(IBinder remote)
			{
                this.mRemote = remote;
            }

            public IBinder asBinder()
			{
                return this.mRemote;
            }

            public String getInterfaceDescriptor()
			{
                return Stub.DESCRIPTOR;
            }

            public void licenseVerified(String signedData, String signature) throws RemoteException
			{
                Parcel _data = Parcel.obtain();
                try
				{
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeString(signedData);
                    _data.writeString(signature);
                    mRemote.transact(1, _data, null, 1);
                }
				finally
				{
                    _data.recycle();
                }
            }

            public void licenseNotVerified() throws RemoteException
			{
                Parcel _data = Parcel.obtain();
                try
				{
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    mRemote.transact(2, _data, null, 1);
                }
				finally
				{
                    _data.recycle();
                }
            }
        }

        public Stub()
		{
            attachInterface(this, DESCRIPTOR);
        }

        public static IAideLicenseResultListener asInterface(IBinder obj)
		{
            if (obj == null)
			{
                return null;
            }
            IInterface iin = obj.queryLocalInterface(DESCRIPTOR);
            if (iin == null || !(iin instanceof IAideLicenseResultListener))
			{
                return new Proxy(obj);
            }
            return (IAideLicenseResultListener) iin;
        }

        public IBinder asBinder()
		{
            return this;
        }

        public boolean onTransact(int code, Parcel data, Parcel reply, int flags) throws RemoteException
		{
            switch (code)
			{
                case 1:
                    data.enforceInterface(DESCRIPTOR);
                    licenseVerified(data.readString(), data.readString());
                    return true;
                case 2:
                    data.enforceInterface(DESCRIPTOR);
                    licenseNotVerified();
                    return true;
                case 1598968902:
                    reply.writeString(DESCRIPTOR);
                    return true;
                default:
                    return super.onTransact(code, data, reply, flags);
            }
        }
    }

    void licenseNotVerified() throws RemoteException;

    void licenseVerified(String str, String str2) throws RemoteException;
}

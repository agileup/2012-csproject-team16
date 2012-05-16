package kaistcs.android.dontkoala;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Parcel;
import android.os.Parcelable;
import android.preference.PreferenceManager;
import android.telephony.TelephonyManager;

/** SharedPreferences wrapper for accessing Profile (except ECN) */
public class UserInfo {
	public static final String NAME = "profile_name";
	public static final String HOME_LOCATION = "profile_home_location";
	public static final String SEND_LOCATION = "profile_send_location";
	public static final String PRESET_TEXT = "profile_preset_text";
	
	SharedPreferences sharedPref;
	Context mContext;
	
	public UserInfo(Context context) {
		mContext = context;
		sharedPref = PreferenceManager.getDefaultSharedPreferences(mContext);
	}
	
	public String getName() {
		return sharedPref.getString(NAME, "");
	}
	
	public boolean setName(String newName) {
		return sharedPref.edit().putString(NAME, newName).commit();
	}
	
	public String getPhoneNumber() {
		return ((TelephonyManager)mContext.getSystemService(Context.TELEPHONY_SERVICE)).getLine1Number();
	}
		
	/** helper class for Home Location preference */
	public static class HomeLocationInfo implements Parcelable {
		private int latE6;
		private int longE6;
		private String addr = "";
		
		public HomeLocationInfo(int latE6, int longE6, String addr) {
			this.latE6 = latE6;
			this.longE6 = longE6;
			this.addr = addr;
		}
		
		@Override
		public String toString() {
			return latE6 + "," + longE6 + "," + addr;
		}
		
		public int getLatitudeE6() {
			return latE6;
		}
		
		public int getLongitudeE6() {
			return longE6;
		}
		
		public String getAddress() {
			return addr;
		}
		
		@Override
		public int describeContents() {
			return 0;
		}
		
		@Override
		public void writeToParcel(Parcel out, int flags) {
			out.writeInt(latE6);
			out.writeInt(longE6);
			out.writeString(addr);
		}
		
		public static final Parcelable.Creator<HomeLocationInfo> CREATOR = new Parcelable.Creator<UserInfo.HomeLocationInfo>() {
			@Override
			public HomeLocationInfo createFromParcel(Parcel in) {
				return new HomeLocationInfo(in);
			}
			
			@Override
			public HomeLocationInfo[] newArray(int size) {
				return new HomeLocationInfo[size];
			}
		};
		
		public HomeLocationInfo(Parcel in) {
			latE6 = in.readInt();
			longE6 = in.readInt();
			addr = in.readString();
		}
	}

	public HomeLocationInfo getHomeLocation() {
		String[] prefStrings = sharedPref.getString(HOME_LOCATION, "").split(",", -1);
		
		if (prefStrings.length == 3)
		{
			int latE6 = Integer.parseInt(prefStrings[0]);
			int longE6 = Integer.parseInt(prefStrings[1]);
			String addr = prefStrings[2];
			
			return new HomeLocationInfo(latE6, longE6, addr);
		}
		
		return null;
	}
	
	public boolean setHomeLocation(HomeLocationInfo info) {
		return sharedPref.edit().putString(HOME_LOCATION, info.toString()).commit();
	}
	
	public boolean getSendLocation() {
		return sharedPref.getBoolean(SEND_LOCATION, false);
	}
	
	public boolean setSendLocation(boolean newValue) {
		return sharedPref.edit().putBoolean(SEND_LOCATION, newValue).commit();
	}
	
	public String getPresetText() {
		return sharedPref.getString(PRESET_TEXT, "");
	}
	
	public boolean setPresetText(String newText) {
		return sharedPref.edit().putString(PRESET_TEXT, newText).commit();
	}
}

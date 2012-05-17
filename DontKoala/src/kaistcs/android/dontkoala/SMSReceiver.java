package kaistcs.android.dontkoala;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.telephony.SmsMessage;
import android.util.Log;

public class SMSReceiver extends BroadcastReceiver {

	protected static final String LOG_TAG = "SMSReceiver";
	private static final String ACTION = "android.provider.Telephony.SMS_RECEIVED";
	
	@Override
	public void onReceive(Context context, Intent intent) {
		Log.i(LOG_TAG, "onReceive");
		abortBroadcast();
		
		if (intent.getAction().equals(ACTION)) {
			
			Bundle bundle = intent.getExtras();
			
			if (bundle != null) {
				Object[] pdusObj = (Object[])bundle.get("pdus");
				SmsMessage[] messages = new SmsMessage[pdusObj.length];
				for (int i=0; i<pdusObj.length; i++) {
					messages[i] = SmsMessage.createFromPdu((byte[])pdusObj[i]);
				}
				Log.i(LOG_TAG, "(SMSApp Bundle)"+bundle.toString());
				
				if (messages.length > 0) {
					String sendNum = messages[0].getOriginatingAddress();
					String sendData = messages[0].getMessageBody().toString();
					Log.i(LOG_TAG, "Number="+sendNum+" Msg="+sendData);
					
					Intent tmpIntent = new Intent(context, NotificationActivity.class);
					tmpIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_SINGLE_TOP);
					tmpIntent.putExtra("sendNum", sendNum);
					tmpIntent.putExtra("sendData", sendData);
					
					context.startActivity(tmpIntent);
				}
				
/*				for (SmsMessage currentMessage : messages) {
					sb.append("Received compressed SMS\nFrom: ");
					sb.append(currentMessage.getDisplayOriginatingAddress());
				}
*/			}
		}
		
		this.abortBroadcast();
		
	}
}

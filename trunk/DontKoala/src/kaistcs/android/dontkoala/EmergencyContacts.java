package kaistcs.android.dontkoala;

import android.app.Activity;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.provider.ContactsContract;
import android.telephony.PhoneNumberUtils;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.ListView;

public class EmergencyContacts extends Activity{
	private ListView mListContacts;
	private Button mBtnAdd;
	private Button mBtnDel;
	
	private static final int CONTACT_PICK = 0;
	
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.emergencycontacts);
		
		mListContacts = (ListView)findViewById(R.id.listContacts);
		mBtnAdd = (Button)findViewById(R.id.btnAdd);
		mBtnDel = (Button)findViewById(R.id.btnDel);
		
		mBtnAdd.setOnClickListener(new OnClickListener(){
			@Override
			public void onClick(View v) {
				Intent i = new Intent(Intent.ACTION_PICK, ContactsContract.Contacts.CONTENT_URI);
				EmergencyContacts.this.startActivityForResult(i, CONTACT_PICK);
			}
		});
		
		mBtnDel.setOnClickListener(new OnClickListener(){
			@Override
			public void onClick(View v) {
				// TODO: delete -> checkbox shown on the list -> confirm delete
			}
		});
	}
	
	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		if (requestCode == CONTACT_PICK && resultCode == RESULT_OK) {
			// Query contacts
			Uri contactData = data.getData();
			Cursor c =  getContentResolver().query(contactData, 
					new String[]{
						ContactsContract.Contacts.LOOKUP_KEY,
						ContactsContract.Contacts.DISPLAY_NAME,
						ContactsContract.Contacts.HAS_PHONE_NUMBER
					}, null, null, null);
			
			String contactKey = "";
			String name = "";
			String hasPhoneNum = "";
			String phoneNumber = "";
			
			try {
				if (c.moveToFirst()) {
					contactKey = c.getString(c.getColumnIndex(ContactsContract.Contacts.LOOKUP_KEY));
					name = c.getString(c.getColumnIndex(ContactsContract.Contacts.DISPLAY_NAME));					
					hasPhoneNum = c.getString(c.getColumnIndex(ContactsContract.Contacts.HAS_PHONE_NUMBER));
		        }
			} finally {
				c.close();
			}
			
			if (hasPhoneNum.equals("1") == false) return;
			
			// Query mobile phone number
			c = getContentResolver().query(ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
					new String[]{ ContactsContract.CommonDataKinds.Phone.NUMBER }, 
					ContactsContract.CommonDataKinds.Phone.LOOKUP_KEY + "=? and " + ContactsContract.CommonDataKinds.Phone.TYPE + "=?",
					new String[]{ contactKey, String.valueOf(ContactsContract.CommonDataKinds.Phone.TYPE_MOBILE) },
					null);
			
			try {
				if (c.moveToFirst())
					phoneNumber = c.getString(c.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER));
			} finally {
				c.close();
			}
			
			if (name.isEmpty() == true || phoneNumber.isEmpty() == true) return;
			
			phoneNumber = PhoneNumberUtils.formatNumber(phoneNumber);
			// TODO: add name, phoneNumber to the list
			// TODO: make a custom CursorAdapater to retrieve the emergency contacts from DB
		}
	}
}

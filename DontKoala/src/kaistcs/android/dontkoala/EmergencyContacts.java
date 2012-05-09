package kaistcs.android.dontkoala;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.provider.ContactsContract;
import android.telephony.PhoneNumberUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.CursorAdapter;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

class ECNCursorAdapter extends CursorAdapter {
	public ECNCursorAdapter(Context context, Cursor c) {
		super(context, c);
	}
	
	@Override
	public View newView(Context context, Cursor cursor, ViewGroup parent) {
		LayoutInflater inflater = LayoutInflater.from(context);
		View v = inflater.inflate(R.layout.ecnitem, parent, false);
		
		return v;
	}

	@Override
	public void bindView(View view, Context context, Cursor cursor) {
		TextView textName = (TextView)view.findViewById(R.id.ecnitem_name);
		TextView textNumber = (TextView)view.findViewById(R.id.ecnitem_number);
		
		textName.setText(cursor.getString(cursor.getColumnIndex(EmergencyContactsDB.Columns.NAME)));
		textNumber.setText(cursor.getString(cursor.getColumnIndex(EmergencyContactsDB.Columns.NUMBER)));
	}
}

public class EmergencyContacts extends Activity {
	private EmergencyContactsDB mDB;
	private Cursor mDBCursor; 
	
	private ListView mListContacts;
	private ECNCursorAdapter mListAdapter;
	private Button mBtnAdd;
	private Button mBtnDel;
	
	private static final int CONTACT_PICK = 0;
	
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.emergencycontacts);
		
		mDB = new EmergencyContactsDB(this);
		mDB.open();
		mDBCursor = mDB.queryAll();
		startManagingCursor(mDBCursor);
		
		mListContacts = (ListView)findViewById(R.id.listContacts);
		mListAdapter = new ECNCursorAdapter(this, mDBCursor);
		mListContacts.setAdapter(mListAdapter);
		mListContacts.setChoiceMode(ListView.CHOICE_MODE_MULTIPLE);
		mListContacts.setItemsCanFocus(false);
		
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
				long[] ids = mListContacts.getCheckedItemIds();
				
				for (long id : ids)
					mDB.deleteECN(id);
				
				stopManagingCursor(mDBCursor);
				mDBCursor.close();
				mDBCursor = mDB.queryAll();
				startManagingCursor(mDBCursor);
				mListAdapter.changeCursor(mDBCursor);
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
			
			if (hasPhoneNum.equals("1") == false) {
				Toast.makeText(this, "No mobile phone number available", Toast.LENGTH_SHORT).show();
				return;
			}
			
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
			
			if (name.isEmpty() == true || phoneNumber.isEmpty() == true) {
				Toast.makeText(this, "No mobile phone number available", Toast.LENGTH_SHORT).show();
				return;
			}
			
			phoneNumber = PhoneNumberUtils.formatNumber(phoneNumber);
			mDB.addECN(name, phoneNumber);
		}
	}
}

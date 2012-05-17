package kaistcs.android.dontkoala;

import java.util.ArrayList;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
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
import android.widget.EditText;
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
	private Button mBtnAddManual;
	private Button mBtnDel;
	
	private static final int CONTACT_PICK = 0;
	private static final int InputDialog = 0;
	
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
		mBtnAddManual = (Button)findViewById(R.id.btnAddManual);
		mBtnDel = (Button)findViewById(R.id.btnDel);
		
		mBtnAdd.setOnClickListener(new OnClickListener(){
			@Override
			public void onClick(View v) {
				Intent i = new Intent(Intent.ACTION_PICK, ContactsContract.Contacts.CONTENT_URI);
				EmergencyContacts.this.startActivityForResult(i, CONTACT_PICK);
			}
		});
		
		mBtnAddManual.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				showDialog(InputDialog);
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
    protected Dialog onCreateDialog(int id)
    {
		if (id == InputDialog) {
			LayoutInflater inflator = LayoutInflater.from(this);
			final View dialogView = inflator.inflate(R.layout.ecnprompt, null);
            
			AlertDialog.Builder builder = new AlertDialog.Builder(this)
						.setTitle("Enter emergency contact")
						.setView(dialogView)
						.setPositiveButton("OK", new DialogInterface.OnClickListener() {
							@Override
							public void onClick(DialogInterface dialog, int whichButton) {
								EditText editName = (EditText) dialogView.findViewById(R.id.editName);
								EditText editNumber = (EditText) dialogView.findViewById(R.id.editNumber);
								String name = editName.getText().toString();
								String number = editNumber.getText().toString();
								
								if (name.isEmpty() == false && number.isEmpty() == false) {
									mDB.addECN(name, number);
								}
								
								stopManagingCursor(mDBCursor);
								mDBCursor.close();
								mDBCursor = mDB.queryAll();
								startManagingCursor(mDBCursor);
								mListAdapter.changeCursor(mDBCursor);
							}
						}).setNegativeButton("Cancel", null);
			
	        return builder.create();
		}
		
		return null;
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
			ArrayList<String> phoneNumbers = new ArrayList<String>();
			
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
					ContactsContract.CommonDataKinds.Phone.LOOKUP_KEY + "=?",
					new String[]{ contactKey },
					null);
			
			try {
				c.moveToFirst();
				
				while (c.isAfterLast() == false) {
					phoneNumbers.add( c.getString(c.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER)) );
					c.moveToNext();
				}
			} finally {
				c.close();
			}
			
			if (name.isEmpty() == true || phoneNumbers.isEmpty() == true) {
				Toast.makeText(this, "No phone number available", Toast.LENGTH_SHORT).show();
				return;
			}
			
			if (phoneNumbers.size() == 1) {
				mDB.addECN(name, phoneNumbers.get(0));
				return;
			}
			
			final String _name = name;
			final String[] _phoneNumbers = new String[phoneNumbers.size()];
			
			int i = 0;
			for (String s : phoneNumbers) {
				_phoneNumbers[i] = s;
				i++;
			}
						
			AlertDialog.Builder builder = new AlertDialog.Builder(this)
				.setTitle("Choose phone number")
				.setSingleChoiceItems(_phoneNumbers, resultCode, new DialogInterface.OnClickListener() {
					@Override
					public void onClick(DialogInterface dialog, int which) {
						String phoneNumber = PhoneNumberUtils.formatNumber(_phoneNumbers[which]);
						mDB.addECN(_name, phoneNumber);
						
						stopManagingCursor(mDBCursor);
						mDBCursor.close();
						mDBCursor = mDB.queryAll();
						startManagingCursor(mDBCursor);
						mListAdapter.changeCursor(mDBCursor);
						dialog.dismiss();
					}
				});
			
			builder.show();
		}
	}
}

package kaistcs.android.dontkoala;

import android.app.Dialog;
import android.content.Context;
import android.content.Intent;
import android.view.View;
import android.view.Window;
import android.widget.Button;
import android.widget.TextView;

public class CustomizeDialog extends Dialog implements android.view.View.OnClickListener  {

	Button btn_ok;
	Button btn_detail;
	Context mContext;
	TextView mTitle = null;
	TextView mMessage = null;
	View v = null;
	
	public CustomizeDialog(Context context) {
		super(context);
		mContext = context;
		requestWindowFeature(Window.FEATURE_NO_TITLE);
		setContentView(R.layout.customizedialog);
		v = getWindow().getDecorView();
		v.setBackgroundResource(android.R.color.transparent);
		
		mTitle = (TextView) findViewById(R.id.dialogTitle);  
        mMessage = (TextView) findViewById(R.id.dialogMessage);
        
        btn_ok = (Button) findViewById(R.id.OkButton);  
        btn_ok.setOnClickListener(this);
        btn_detail = (Button) findViewById(R.id.DetailButton);
        btn_detail.setOnClickListener(this);
    }
	
    @Override  
    public void onClick(View v) {  
        /** When OK Button is clicked, dismiss the dialog */  
        if (v == btn_ok) {
            dismiss();
        }
        else if (v == btn_detail) {
        	// NotificationTab 으로 이동 (아마 PendingIntent 로 해야할꺼같음)
        	dismiss();
        	
        	Intent tmpIntent = new Intent(v.getContext(), DontKoalaActivity.class);
			tmpIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_SINGLE_TOP);
			tmpIntent.putExtra("tag", "dialog");
			v.getContext().startActivity(tmpIntent);
        }
    }  
    @Override  
    public void setTitle(CharSequence title) {  
        super.setTitle(title);  
        mTitle.setText(title);  
    }  
    @Override  
    public void setTitle(int titleId) {  
        super.setTitle(titleId);  
        mTitle.setText(mContext.getResources().getString(titleId));  
    }  
    /**  
     * Set the message text for this dialog's window.  
     *   
     * @param message  
     *      - The new message to display in the title.  
     */  
    public void setMessage(CharSequence message) {  
        mMessage.setText(message);  
        //mMessage.setMovementMethod(ScrollingMovementMethod.getInstance());  
    }  
    /**  
     * Set the message text for this dialog's window. The text is retrieved from the resources with the supplied  
     * identifier.  
     *   
     * @param messageId  
     *      - the message's text resource identifier <br>  
     * @see <b>Note : if resourceID wrong application may get crash.</b><br>  
     *   Exception has not handle.  
     */  
    public void setMessage(int messageId) {  
        mMessage.setText(mContext.getResources().getString(messageId));  
        //mMessage.setMovementMethod(ScrollingMovementMethod.getInstance());  
    }  
}

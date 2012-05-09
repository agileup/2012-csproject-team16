package kaistcs.android.dontkoala;

import android.content.Context;
import android.content.res.TypedArray;
import android.util.AttributeSet;
import android.widget.Checkable;
import android.widget.LinearLayout;

public class ECNItemLayout extends LinearLayout implements Checkable{
	int checkableId;
	Checkable checkable;

	public ECNItemLayout(Context context, AttributeSet attrs) {
		super(context, attrs);
		TypedArray ar = context.obtainStyledAttributes(attrs, R.styleable.ECNItemLayout);
		checkableId = ar.getResourceId(R.styleable.ECNItemLayout_checkableId, 0);
		ar.recycle();
	}
	
	@Override
	protected void onFinishInflate() {
		checkable = (Checkable) findViewById(checkableId);
	}

	@Override
	public boolean isChecked() {
		if (checkable == null)
			return false;
		return checkable.isChecked();
	}

	@Override
	public void setChecked(boolean checked) {
		if (checkable == null)
			return;
		checkable.setChecked(checked);
	}

	@Override
	public void toggle() {
		if (checkable == null)
			return;
		checkable.toggle();
	}
}

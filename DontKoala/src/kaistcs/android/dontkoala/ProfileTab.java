package kaistcs.android.dontkoala;

import android.app.Activity;
import android.os.Bundle;
import android.widget.TextView;

public class ProfileTab extends Activity {

	/** Called when the activity is first created. */
	@Override
	public void onCreate(Bundle savedInstanceState) {
	    super.onCreate(savedInstanceState);
	
	    // res\layout\ 에 각 탭마다 화면구성하는 xml파일 생성
	    // 지금보니깐 거의 리스트뷰일것 같은데, 찾아보시고 profile.xml 부분 알맞게 수정하시면 됩니다
        setContentView(R.layout.profile);
        
        // 이부분은 테스트코드라 실제 코딩하실때 삭제하시면 됩니다
        TextView textView = new TextView(this);
	    textView.setText("여기에 프로필 부분 채워주세요");
	    setContentView(textView);

	}

}

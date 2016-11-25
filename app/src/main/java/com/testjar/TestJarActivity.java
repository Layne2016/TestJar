package com.testjar;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ListView;

import com.identity.Files;
import com.identity.Shell;
import com.identity.globalEnum;

import java.io.IOException;
import java.util.Calendar;
import java.util.Set;


public class TestJarActivity extends AppCompatActivity {
	/** Called when the activity is first created. */
	private ImageView  iv; 
	private Button btnInit;
	private Button btnExit; 
	private Button btnGetData; 
	private Button btnRegist;  
	private ListView lvInfoView;
	private BluetoothAdapter mAdapter;
	private BluetoothDevice mDevice;
	private ArrayAdapter<String> mInfoView; 
	private static final int REQUEST_ENABLE_BT = 2;
	private Shell shell=null;
	private boolean bInitial = false;
	private boolean bStop = false;
	private boolean bConnected = false;
	private Context context;
	
	private int m_sec1,m_sec2;
	private int m_msec1,m_msec2;
	private Calendar c;
	 public boolean OnKeyDown(int keyCode,KeyEvent event){   
	        if (keyCode==KeyEvent.KEYCODE_BACK ) {
	        	bStop = true;
	        	finish();
	        }      
	        return false;   
	     }  
	private class ButtonExitOnClick implements OnClickListener {
		public void onClick(View v) {
        	bStop = true;
        	finish();
		}
	}

	@Override 
	protected void onDestroy() {
		super.onDestroy();
		bStop = true;
		try {
			Thread.sleep(1000);
		} catch (InterruptedException e) {
			e.printStackTrace();  
		}			
		try {
			shell.Destroy();
		} catch (IOException e) {
			e.printStackTrace();
		} 
	}


	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.main);
		iv = (ImageView) findViewById(R.id.ivImageview);        
		mInfoView = new ArrayAdapter<String>(this, R.layout.tv_infoview);
		lvInfoView = (ListView) findViewById(R.id.lvInfoview);
		lvInfoView.setAdapter(mInfoView);
		lvInfoView.setVisibility(View.VISIBLE);
		mAdapter = BluetoothAdapter.getDefaultAdapter();
		btnInit = (Button) findViewById(R.id.btnInit);
		btnExit = (Button) findViewById(R.id.btnExit);
		btnInit.setOnClickListener(new ButtonInitOnClick());
		btnExit.setOnClickListener(new ButtonExitOnClick());
		Files file = new Files(this.getApplicationContext());
		
		if (mAdapter == null) {
			mInfoView.add("mAdapter is null!");
		}
		if (!mAdapter.isEnabled()) {
			Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
			startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);
		}  
		c = Calendar.getInstance();
		m_sec1 = c.get(Calendar.SECOND);
		m_msec1 = c.get(Calendar.MILLISECOND);
		Set<BluetoothDevice> pairedDevices = mAdapter.getBondedDevices();
		if (pairedDevices.size() > 0) {
			for (BluetoothDevice device : pairedDevices) {
				 	String str;
				 	str = device.getName().substring(0, 3);
					Log.w("pairedDevices", "device.getName().substring(0, 3) is:"+str);				
					if(str.equalsIgnoreCase("SYN")){	
						Log.w("onCreate", "device.getName() is SYNTHESIS");			
						mDevice = device;	
					}else   //是否能进入Else
					{
						Log.w("onCreate", "device.getName() is not SYNTHESIS");
						boolean bAllNum=false;
						if(device.getName().length()>9){
							str = device.getName().substring(0, 10);
							bAllNum = str.matches("[0-9]+");
							if(bAllNum==true){			
								mDevice = device;							
							}
						}
					} 
				mInfoView.add(device.getName() + "====" + device.getAddress());
			}
			try {
				mAdapter.cancelDiscovery();
				context = this.getApplicationContext();
				shell = new Shell(context, mDevice);
				c = Calendar.getInstance();
				m_sec2 = c.get(Calendar.SECOND);
				m_msec1 = c.get(Calendar.MILLISECOND);
				int d = m_sec2-m_sec1;
				int md = m_msec2-m_msec1;
				if(d<0)
					d = d + 60;
				if(md<0)
					md = md + 1000;
			//	mInfoView.add("connect timeee is:  "+d+"."+md+"s");
				
			} catch (IOException e) {
				// TODO Auto-generated catch block	
				e.printStackTrace();
				Log.w("test", "Socket connect error！");
				mInfoView.add("与机具建立连接失败，请尝试重新启动应用程序!");
			}
			Log.w("test", "Socket connect OK！");
		}
	}

	private class ButtonInitOnClick implements OnClickListener {
		public void onClick(View v) {
			globalEnum ge = globalEnum.NONE;
			//mInfoView.add("ButtonInitOnClick");
			Log.w("TestJarActivity","In ButtonInitOnClick 00");
				if(shell == null){
					Log.w("TestJarActivity","In ButtonInitOnClick shell is null");
					try {
						shell = new Shell(context, mDevice);
					} catch (IOException e) {
						e.printStackTrace();
					}
				}
					
				try {
					if (shell.Register()){ 
						Log.w("TestJarActivity","In ButtonInitOnClick 11");
						//0316 btnRegist.setEnabled(false);
						mInfoView.add("取机具编号成功！");	
						ge = shell.Init(); 
						if (ge == globalEnum.INITIAL_SUCCESS) {
							bInitial = true;  
							btnInit.setEnabled(false);  
							mInfoView.add("建立连接成功！");	
							bConnected = true;					    
							new Thread(new GetDataThread()).start();
						} else {
							Log.w("TestJarActivity","In ButtonInitOnClick 22");
							shell.EndCommunication();//0316
							mInfoView.add("建立连接失败,请重新执行应用程序");
						}
					}else {
						Log.w("TestJarActivity","In ButtonInitOnClick 33");
						mInfoView.add("没搜索到蓝牙设备，请重新执行应用程序！");					
					}
				} catch (Exception e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
		}
	}

	class ButtonRegistOnClick implements OnClickListener {

		public void onClick(View v) {
			try {
				if (shell.Register())
				{
					mInfoView.add("取机具编号成功！");
				}else
				{  
					mInfoView.add("没搜索到蓝牙设备，请重新执行应用程序！");					
				}
			} catch (Exception e) {
				e.printStackTrace();
			} 
		}
	}

	private class ButtonGetDataOnClick implements OnClickListener {
		public void onClick(View v) {
			btnGetData.setEnabled(false); 		    
			new Thread(new GetDataThread()).start();		
		} 
	} 
	//0316
	private class GetDataThread implements Runnable{
		private String data ;	
		private byte[] cardInfo = new byte[256];		
		private int count = 0 ;
		private Message msg;//主要改了这个地方，好像启作用了
		private String wltPath="";
		private String termBPath="";
		private boolean bRet = false;
		
		
		public GetDataThread(){
			}        
		public void run() {	
			globalEnum ge = globalEnum.GetIndentiyCardData_GetData_Failed;
			try {		  
				Thread.sleep(2000);
				
				globalEnum gFindCard = globalEnum.NONE;
				long start = System.currentTimeMillis();
				while (!bStop) {
					count += 1;  
				    if(count == 10) 
					{
						System.gc(); 
						System.runFinalization(); 
						count = 0;
					}					
				    data = null;//
					msg = handler.obtainMessage(71, data);//发送消息
					handler.sendMessage(msg);		
				    bRet = shell.SearchCard(); 
					if (bRet) {  			
					    data = null;//
						msg = handler.obtainMessage(1, data);//发送消息
						handler.sendMessage(msg);				      
						bRet = shell.SelectCard();
						if(bRet){  
							data = null;//
							msg = handler.obtainMessage(2, data);//发送消息
							handler.sendMessage(msg);						  
							//Thread.sleep(100);  
						
							ge = shell.ReadCard();
							if (ge == globalEnum.GetDataSuccess) {
								data = null;//
								msg = handler.obtainMessage(3, data);//发送消息
								handler.sendMessage(msg);
							
								cardInfo = shell.GetCardInfoBytes();
								data = String.format(
									"姓名：%s 性别：%s 民族：%s 出生日期：%s 住址：%s 身份证号：%s 签发机关：%s 有效期：%s-%s",
									shell.GetName(cardInfo), shell.GetGender(cardInfo), shell.GetNational(cardInfo),
									shell.GetBirthday(cardInfo), shell.GetAddress(cardInfo),
									shell.GetIndentityCard(cardInfo), shell.GetIssued(cardInfo),
									shell.GetStartDate(cardInfo), shell.GetEndDate(cardInfo));
								msg = handler.obtainMessage(0, data);//发送消息
								handler.sendMessage(msg);
								
							//	Log.w("777"," shell.GetEndDate(cardInfo) is:"+ shell.GetEndDate(cardInfo));
							 
								// 没有模块号，所以屏蔽
								wltPath="/data/data/com.testjar/files/";
								termBPath="/mnt/sdcard/";
								int nret = shell.GetPic(wltPath,termBPath); 
								if(nret > 0)
								{
									Bitmap bm = BitmapFactory.decodeFile("/data/data/com.testjar/files/zp.bmp");
									msg = handler.obtainMessage(100, bm);//发送消息
									handler.sendMessage(msg);

								}else if(nret == -5)
								{
									msg = handler.obtainMessage(101, data);//发送消息
									handler.sendMessage(msg);
							  	}else if(nret == -1)
							  	{ 
							  		msg = handler.obtainMessage(102, data);//发送消息
							  		handler.sendMessage(msg);								  
							  	} 
								//break;//0316  调试用，所以增加
							}else{
								msg = handler.obtainMessage(6, data);//发送消息
								handler.sendMessage(msg);	//readCard error					
							}							
						}else{
							msg = handler.obtainMessage(5, data);//发送消息
							handler.sendMessage(msg);	//selectCard error					
						}						
					}else{
						msg = handler.obtainMessage(4, data);//发送消息
						handler.sendMessage(msg);	//searchCard error					
					}
					Thread.sleep(50);
				}
			} catch (IOException e) {
				e.printStackTrace();
			} catch (InterruptedException e) {
				e.printStackTrace();
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
	}
	

	 public Handler handler = new Handler(){//处理UI绘制
	  private String data;
	  private Bitmap bm;
	  private int t_sec1,t_sec2;
	  private int t_msec1,t_msec2;
	  
	  private Calendar t; 
	  @SuppressWarnings("unchecked")
	  @Override
	  public void handleMessage(Message msg) {//M_ERROR  M_VALIDATE_ERROR I_ERROR I_VALIDATE_ERROR
	   switch (msg.what) {                    //C_ERROR  C_VALIDATE_ERROR D_ERROR D_VALIDATE_ERROR
	   case 0:
		    data = (String) msg.obj;
		    if(data == null){
		    }else {	
				//mInfoView.clear();
				t = Calendar.getInstance();
				t_sec2 = t.get(Calendar.SECOND);
				t_msec2 = t.get(Calendar.MILLISECOND);  
				int d = t_sec2-t_sec1;
				int md = t_msec2-t_msec1;
				if(d<0)
					d = d + 60;
				if(md<0)
					md = md + 1000;
			//	mInfoView.add("readcard time is:  "+d+"."+md+"s");
				mInfoView.add(data);	    
		    }
		    break; 
	   case 71:
			t = Calendar.getInstance();
			t_sec1 = t.get(Calendar.SECOND);  
			t_msec1 = t.get(Calendar.MILLISECOND);   
		    break; 
	   case 100:
		   bm = (Bitmap) msg.obj;
	       iv.setImageBitmap(bm);
	       
	       deleteFile("zp.bmp");
	       
		   break; 
	   case 101:
			mInfoView.add("照片解码授权文件不正确");
		   break; 
	   case 102:
			mInfoView.add("照片原始数据不正确");
		   break; 
		case 1:
			mInfoView.clear();
		    iv.setImageBitmap(null);
			//mInfoView.add("SearchCard ok"); 
			break; 
		case 4:
			//mInfoView.clear();
			//mInfoView.add("正在寻卡...");
			break; 
		case 5:
			mInfoView.clear();
			mInfoView.add("SelectCard error");
			break; 
		case 6:
			mInfoView.clear();
			mInfoView.add("ReadCard error");
			break;
		case 87:
			mInfoView.clear();
			mInfoView.add("读卡初始化中，请稍候...");
			break;
		case 88:
			mInfoView.clear();
			mInfoView.add("机具信息监听中...");
			break;
		case 99:
			mInfoView.clear();
	        iv.setImageBitmap(null);
			break;
		   default:
		    break;
	   }
	  }
	 };
	//0316
}
package ioio.examples.hello;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.text.DecimalFormat;
import java.util.Timer;
import java.util.TimerTask;


//import com.thousandthoughts.tutorials.SensorFusionActivity.calculateFusedOrientationTask;




import ioio.lib.util.BaseIOIOLooper;
import ioio.lib.util.IOIOLooper;
import ioio.lib.util.android.IOIOActivity;
import ioio.lib.api.AnalogInput;
import ioio.lib.api.DigitalInput;
import ioio.lib.api.DigitalOutput;
import ioio.lib.api.IOIO;
import ioio.lib.api.PulseInput;
import ioio.lib.api.PwmOutput;
import ioio.lib.api.IOIO.VersionType;
import ioio.lib.api.exception.ConnectionLostException;
import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.media.MediaScannerConnection;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ToggleButton;
import android.annotation.SuppressLint;
import android.annotation.TargetApi;
import android.app.Activity;

/**
 * This is the main activity of the HelloIOIO example application.
 * 
 * It displays a toggle button on the screen, which enables control of the
 * on-board LED. This example shows a very simple usage of the IOIO, bdy using
 * the {@link IOIOActivity} class. For a more advanced use case, see the
 * HelloIOIOPower example.
 */

public class MainActivity extends IOIOActivity{
	
	double Ttotal=0;
	int ngravador=20002;
	int n=0;
	int N=1;
	
	public static float Va2;
	public static float Va20=(float) 1.52;   //Tens�o do Amp 2 em respouso
	public static float Vponte=0;
	public static float voffset=3;
	public static float Gan=360;
	
	
	
	
    public float Dt=0;   
	private ToggleButton button_;
	
	private double[] Strainpilha = new double[ngravador];
	private double[] timepilha = new double[ngravador];
	private double straink=0;
	
	public static final float EPSILON = 0.000000001f;
    private static final float NS2S = 1.0f / 1000000000.0f;
	private long timestamp;
	private boolean timeiniciador=false;
	private long timem1;
	private boolean initState = true;
	private GraphView mGraphView;
	private TextView mTextView;
	private TextView mTextView2;
	private TextView mTextView3;
	private TextView Ganho;
	private TextView Offset;
	private TextView VA20;
	private Button Bt;
	
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.main);
		button_ = (ToggleButton) findViewById(R.id.button);  
		mGraphView = (GraphView) findViewById(R.id.graph);
		mGraphView.setMaxValue(300);
		
		mTextView = (TextView) findViewById(R.id.tensao);
		mTextView2 = (TextView) findViewById(R.id.va2);
		mTextView3 = (TextView) findViewById(R.id.dva2);
		Ganho = (TextView) findViewById(R.id.Ganho);
		Offset= (TextView) findViewById(R.id.Offset);
		VA20= (TextView) findViewById(R.id.VA20);
		Bt= (Button) findViewById(R.id.button1);
		
	}

	public void Setar(View v)
	{
	Gan=Float.valueOf(Ganho.getText().toString());
	voffset=Float.valueOf(Offset.getText().toString());
	Va20=Float.valueOf(VA20.getText().toString());
	}
	
	@Override
    public void onStop() {
    	super.onStop();
    	// unregister sensor listeners to prevent the activity from draining the device's battery.
    	
    }
	
    @Override
    protected void onPause() {
        super.onPause();
        // unregister sensor listeners to prevent the activity from draining the device's battery.
        
    }
    
    @Override
    public void onResume() {
    	super.onResume();
    	// restore the sensor listeners when user resumes the application.
    	
    }
    
 
	
	
	
	
	@Override
	protected IOIOLooper createIOIOLooper() {
		return new Looper();
	}

	private void showVersions(IOIO ioio, String title) {
		toast(String.format("%s\n" +
				"IOIOLib: %s\n" +
				"Application firmware: %s\n" +
				"Bootloader firmware: %s\n" +
				"Hardware: %s",
				title,
				ioio.getImplVersion(VersionType.IOIOLIB_VER),
				ioio.getImplVersion(VersionType.APP_FIRMWARE_VER),
				ioio.getImplVersion(VersionType.BOOTLOADER_VER),
				ioio.getImplVersion(VersionType.HARDWARE_VER)));
	}

	private void toast(final String message) {
		final Context context = this;
		runOnUiThread(new Runnable() {
			@Override
			public void run() {
				Toast.makeText(context, message, Toast.LENGTH_LONG).show();
			}
		});
	}

	private int numConnected_ = 0;

	private void enableUi(final boolean enable) {
		// This is slightly trickier than expected to support a multi-IOIO use-case.
		runOnUiThread(new Runnable() {
			@Override
			public void run() {
				if (enable) {
					if (numConnected_++ == 0) {
						button_.setEnabled(true);
					}
				} else {
					if (--numConnected_ == 0) {
						button_.setEnabled(false);
					}
				}
			}
		});
	}
	
	
	
	 

	
	 
	        
	public void gravardados(){
		
		try
		   {
			int i=0;
		    
			
		   File traceFileamostras = new File(((Context)this).getExternalFilesDir(null), "Amostras "+N+".txt");
		   if (!traceFileamostras.exists())
		      traceFileamostras.createNewFile();
		   
		   File traceFiletempo = new File(((Context)this).getExternalFilesDir(null), "Tempo "+N+".txt");
		   if (!traceFiletempo.exists())
		      traceFiletempo.createNewFile();
		   
		   
		   BufferedWriter writera = new BufferedWriter(new FileWriter(traceFileamostras, true /*append*/));
		   BufferedWriter writert = new BufferedWriter(new FileWriter(traceFiletempo, true /*append*/));
		   
		   writera.write("INICIO GRAVACAO - Amostras (V)");
		   writera.write("\n\n");
		
		   writert.write("INICIO GRAVACAO - Tempo (s)");
		   writert.write("\n\n");
		
		   
		   while(i<ngravador-2)
			  
		{
			   
			   writera.write(Double.toString(Strainpilha[i]));
			   writera.write("\n");
			   
			   writert.write(Double.toString(timepilha[i]));
			   writert.write("\n");
			
		   
		   
		   
		   i++;
		   }
		   
		   
		   i=0;
		   writera.close();
		   writert.close();                
		   
		    MediaScannerConnection.scanFile((Context)(this),
		                                     new String[] { traceFileamostras.toString() },
		                                     null,
		                                     null);
		    
		    MediaScannerConnection.scanFile((Context)(this),
                    new String[] { traceFiletempo.toString() },
                    null,
                    null);
		    
		    }
		
		catch (IOException e)
		    {
		    Log.e("com.cindypotvin.FileTest", "Unable to write to the TraceFile.txt file.");
		    }
		
		n=0;
		N++;
		
		
		}
		
	
	
	/**
	 * This is the thread on which all the IOIO activity happens. It will be run
	 * every time the application is resumed and aborted when it is paused. The
	 * method setup() will be called right after a connection with the IOIO has
	 * been established (which might happen several times!). Then, loop() will
	 * be called repetitively until the IOIO gets disconnected.
	 */
	class Looper extends BaseIOIOLooper {

		private DigitalOutput led_;
		private AnalogInput EntradaStrain;
		
		
		
		 
		@Override
		protected void setup() throws ConnectionLostException {
			showVersions(ioio_, "IOIO conectado!");
			led_ = ioio_.openDigitalOutput(0, true);
			EntradaStrain= ioio_.openAnalogInput(31);
			EntradaStrain.setBuffer(1000);
			
		}
		
		
		
		
		private void addPoint(final double d) {
			runOnUiThread(new Runnable() {
				@Override
				public void run() {
					mGraphView.addDataPoint(d);
					//mTextView.setText("Shoryuken");
				}
			});
		}
		
		private void printar(final double d,final double d2,final double d3) {
			runOnUiThread(new Runnable() {
				@Override
				public void run() {
					
					mTextView.setText("Vioio(V): "+Float.toString((float)(d)));;
					mTextView2.setText("Overflow: "+Float.toString((float)(d2)));;
					mTextView3.setText("T amostragem "+Float.toString((float)(d3)));;
				}
			});
		}
		
		
		
		@Override
		public void loop() throws ConnectionLostException, InterruptedException {
			
			//mTextView = (TextView) findViewById(R.id.tensao);
			//mTextView.setText("Shoryuken");
			
			/*timestamp=System.nanoTime();
			if(!timeiniciador)
			{
				timem1=timestamp;
				timeiniciador=true;
			}
			
			
			
			//amostragem= EntradaStrain.getSampleRate();
			Dt=(float) ((timestamp-timem1)*NS2S);
	        timem1=timestamp;
	        Ttotal=Dt+Ttotal;
			*/
			
	        led_.write(!button_.isChecked());
	        int j=1;
	        
	        while(j<500)
	        {
			straink=EntradaStrain.getVoltageBuffered();
			addPoint(straink * 100);
			Strainpilha[n]=straink;
			
			//timepilha[n]=Ttotal;
			
			n=n+1;
			j=j+1;
			if(n==ngravador-1)
			{
			gravardados();
			n=0;
			}
		}
	        j=0;
	        printar(straink,EntradaStrain.available(),EntradaStrain.getSampleRate());
	        
			//Va2=Math.abs((float) (straink-voffset));
			//Vponte=Math.abs((float) ((Va2-Va20))/(Gan*20));
			
			//addPoint(straink * 100);
			
			
			
			
			
			
			
			//Thread.sleep(1);
			
			
				
	        
			
				
			
		}
			
		public void disconnected() {
			enableUi(false);
			toast("IOIO desconectado");
		}

		/**
		 * Called when the IOIO is connected, but has an incompatible firmware version.
		 *
		 * @see ioio.lib.util.IOIOLooper#incompatible(IOIO)
		 */
		@Override
		public void incompatible() {
			showVersions(ioio_, "Incompatible firmware version!");
		}
			
		}
	
	}

	/**
	 * A method to create our IOIO thread.
	 * 
	 * @see ioio.lib.util.AbstractIOIOActivity#createIOIOThread()
	 */
	
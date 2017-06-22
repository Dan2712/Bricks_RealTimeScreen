import java.awt.BorderLayout;
import java.awt.Canvas;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.Panel;
import java.awt.Toolkit;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.awt.image.BufferedImage;
import java.awt.image.DirectColorModel;
import java.awt.image.IndexColorModel;
import java.awt.image.WritableRaster;

import javax.swing.JFrame;
import javax.swing.WindowConstants;
import javax.swing.border.Border;

import org.apache.log4j.Logger;
import org.eclipse.swt.awt.SWT_AWT;
import org.eclipse.swt.events.PaintEvent;
import org.eclipse.swt.events.PaintListener;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.ImageData;
import org.eclipse.swt.graphics.PaletteData;
import org.eclipse.swt.graphics.RGB;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;

import com.android.ddmlib.IDevice;

import MiniDecode.AndroidScreenObserver;
import MiniDecode.MiniCapUtil;
import Util.ADB;

public class RealtimeUI extends Panel implements AndroidScreenObserver {
	private static final Logger LOG = Logger.getLogger("RealtimeUI.class");
	
	private int width = 400;
	private int height = 500;
	
	private IDevice device;
	private BufferedImage image = null;
	private MiniCapUtil minicap = null;
	
	private Canvas screenCanvas = null;
	private DisplayThread displayThread = null;
	
	public RealtimeUI() {
		ADB adb = new ADB();
		if (adb.getDevices().length <= 0) {
			LOG.error("无连接设备,请检查");
			return;
		}
		device = adb.getDevices()[0];
		minicap = new MiniCapUtil(device);
		minicap.registerObserver(this);
//		minicap.takeScreenShotOnce();
		minicap.startScreenListener();
		
//		this.setSize(500, height);
//		Dimension dim = Toolkit.getDefaultToolkit().getScreenSize();
//		this.setLocation((dim.width - this.getWidth()) / 2, 0);
		
//		screenCanvas = new Canvas();
//		container = this.getContentPane();
//		container.add(screenCanvas, BorderLayout.CENTER);
//		display = new Display();
//		shell = SWT_AWT.new_Shell(display.getCurrent(), screenCanvas);
//		shell.setLayout(new FillLayout());
//		
//		shell.addPaintListener(new PaintListener() {
//			
//			@Override
//			public void paintControl(PaintEvent e) {
//				e.gc.drawImage(new Image(display, convertToSWT(image)), 0, 0);
//			}
//		});
		
		displayThread = new DisplayThread();  
        displayThread.start();  
        screenCanvas = new Canvas();  
        setLayout(new BorderLayout());  
        add(screenCanvas, BorderLayout.CENTER);
	}
	
	private ImageData convertToSWT(BufferedImage bufferedImage) {
	    if (bufferedImage.getColorModel() instanceof DirectColorModel) {
	        DirectColorModel colorModel = (DirectColorModel) bufferedImage.getColorModel();
	        PaletteData palette = new PaletteData(
	            colorModel.getRedMask(),
	            colorModel.getGreenMask(),
	            colorModel.getBlueMask()
	        );
	        ImageData data = new ImageData(
	            bufferedImage.getWidth(),
	            bufferedImage.getHeight(), colorModel.getPixelSize(),
	            palette
	        );
	        WritableRaster raster = bufferedImage.getRaster();
	        int[] pixelArray = new int[3];
	        for (int y = 0; y < data.height; y++) {
	            for (int x = 0; x < data.width; x++) {
	                raster.getPixel(x, y, pixelArray);
	                int pixel = palette.getPixel(
	                    new RGB(pixelArray[0], pixelArray[1], pixelArray[2])
	                );
	                data.setPixel(x, y, pixel);
	            }
	        }
	        return data;
	    } else if (bufferedImage.getColorModel() instanceof IndexColorModel) {
	        IndexColorModel colorModel = (IndexColorModel) bufferedImage.getColorModel();
	        int size = colorModel.getMapSize();
	        byte[] reds = new byte[size];
	        byte[] greens = new byte[size];
	        byte[] blues = new byte[size];
	        colorModel.getReds(reds);
	        colorModel.getGreens(greens);
	        colorModel.getBlues(blues);
	        RGB[] rgbs = new RGB[size];
	        for (int i = 0; i < rgbs.length; i++) {
	            rgbs[i] = new RGB(reds[i] & 0xFF, greens[i] & 0xFF, blues[i] & 0xFF);
	        }
	        PaletteData palette = new PaletteData(rgbs);
	        ImageData data = new ImageData(
	            bufferedImage.getWidth(),
	            bufferedImage.getHeight(),
	            colorModel.getPixelSize(),
	            palette
	        );
	        data.transparentPixel = colorModel.getTransparentPixel();
	        WritableRaster raster = bufferedImage.getRaster();
	        int[] pixelArray = new int[1];
	        for (int y = 0; y < data.height; y++) {
	            for (int x = 0; x < data.width; x++) {
	                raster.getPixel(x, y, pixelArray);
	                data.setPixel(x, y, pixelArray[0]);
	            }
	        }
	        return data;
	    }
	    return null;
	}
	
	@Override
	public void frameImageChange(BufferedImage image) {
		this.image = image;
	}

	public void addNotify()   
    {  
        super.addNotify();  
        Display dis = displayThread.getDisplay();  
        dis.syncExec( new Runnable() {  
            //此处添加SWT界面的代码  
            public void run() {  
            Shell shell = SWT_AWT.new_Shell(displayThread.getDisplay(), screenCanvas);  
            shell.setSize(800, 800);  
           
//            shell.addPaintListener(new PaintListener() {
//				
//				@Override
//				public void paintControl(PaintEvent e) {
//					e.gc.drawImage(new Image(dis, convertToSWT(image)), 0, 0);
//				}
//			});
           }
        });  
    }
	
	class DisplayThread extends Thread{  
        private Display display;  
        Object lock = new Object();  
          
        public void run(){  
            synchronized (lock) {  
                display = Display.getDefault();  
                lock.notifyAll();  
            }  
            swtEventLoop();  
        }  
          
        private void swtEventLoop(){  
            while(true)  
            {  
                if(!display.readAndDispatch())  
                {  
                    display.sleep();  
                }  
            }  
        }  
          
        public Display getDisplay() {  
            synchronized (lock) {  
                while (display == null)   
                {  
                    try {  
                        lock.wait();  
                    } catch (InterruptedException e) {  
                        e.printStackTrace();  
                    }  
                }  
                return display;  
            }  
        }  
    }
	
	public static void main(String[] args) {
		JFrame frame = new JFrame();  
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);  
        frame.setVisible(true);  
        frame.setLayout(new BorderLayout());  
        frame.setSize(800, 800);  
          
        RealtimeUI swtPanel = new RealtimeUI();  
        frame.add(swtPanel,BorderLayout.CENTER);  
	}
}

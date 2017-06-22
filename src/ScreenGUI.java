
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Rectangle;
import java.awt.Toolkit;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.awt.image.BufferedImage;

import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.WindowConstants;

import org.apache.log4j.Logger;

import com.android.ddmlib.IDevice;

import MiniDecode.AndroidScreenObserver;
import MiniDecode.MiniCapUtil;
import Util.ADB;

public class ScreenGUI extends JFrame {
	private static final Logger LOG = Logger.getLogger("PageTest.class");
	
	private static final int IMG_BORDER = 2;

	private MyPanel mp = null;
	private IDevice device;
	private int width;
	private int height;
	
	private float mScale = 1.0f;
    private int mDx, mDy;

	public ScreenGUI() {
		ADB adb = new ADB();
		if (adb.getDevices().length <= 0) {
			LOG.error("无连接设备,请检查");
			return;
		}
		device = adb.getDevices()[0];
		mp = new MyPanel(device,this);
		this.getContentPane().add(mp);
		this.setSize(700, 700);
		Dimension dim = Toolkit.getDefaultToolkit().getScreenSize();
		this.setLocation((dim.width - this.getWidth()) / 2, 0);
		setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
		addWindowListener(new WindowAdapter() {
			@Override
			public void windowClosing(WindowEvent e) {

			}
		});
		this.setVisible(true);
//		pack();

	}

	public static void main(String[] args) {
		new ScreenGUI();
	}
	
	class MyPanel extends JPanel implements AndroidScreenObserver {

		BufferedImage image = null;
		MiniCapUtil minicap = null;

		public MyPanel(IDevice device,ScreenGUI frame) {
			minicap = new MiniCapUtil(device);
			minicap.registerObserver(this);
//			minicap.takeScreenShotOnce();
			minicap.startScreenListener();

		}

		public void paint(Graphics g) {
			try {
				if (image == null)
					return;
				ScreenGUI.this.setSize(image.getWidth(), image.getHeight());
				g.drawImage(image, 0, 0, width, height, null);
				this.setSize(image.getWidth(), image.getHeight());
				image.flush();
			} catch (Exception e) {
				e.printStackTrace();
			}
		}

		@Override
		public void frameImageChange(BufferedImage image) {
			this.image = image;
			int w = this.image.getWidth();
			int h = this.image.getHeight();
//			float radio = (float) width / (float) w;
			height = (int) (h / 2);
			width = (int) (w / 2);
			this.repaint();
		}
		
		private int getScaledSize(int size) {
	        if (mScale == 1.0f) {
	            return size;
	        } else {
	            return new Double(Math.floor((size * mScale))).intValue();
	        }
	    }

	    private int getInverseScaledSize(int size) {
	        if (mScale == 1.0f) {
	            return size;
	        } else {
	            return new Double(Math.floor((size / mScale))).intValue();
	        }
	    }

	    private void updateScreenshotTransformation() {
	        Rectangle canvas = mp.getBounds();
	        float scaleX = (canvas.width - 2 * IMG_BORDER - 1) / (float) image.getWidth();
	        float scaleY = (canvas.height - 2 * IMG_BORDER - 1) / (float) image.getHeight();

	        // use the smaller scale here so that we can fit the entire screenshot
	        mScale = Math.min(scaleX, scaleY);
	        // calculate translation values to center the image on the canvas
	        mDx = (canvas.width - getScaledSize(image.getWidth()) - IMG_BORDER * 2) / 2 + IMG_BORDER;
	        mDy = (canvas.height - getScaledSize(image.getHeight()) - IMG_BORDER * 2) / 2 + IMG_BORDER;
	    }
	}

}

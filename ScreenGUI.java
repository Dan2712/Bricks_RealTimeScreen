
import java.awt.Canvas;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Image;
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

	private MyPanel mp = null;
	private IDevice device;
	private int width = 400;
	private int height = 500;
	private Thread thread = null;

	public ScreenGUI() {
		ADB adb = new ADB();
		if (adb.getDevices().length <= 0) {
			LOG.error("无连接设备,请检查");
			return;
		}
		device = adb.getDevices()[0];
		mp = new MyPanel(device,this);
		this.getContentPane().add(mp);
		this.setSize(500, height);
		Dimension dim = Toolkit.getDefaultToolkit().getScreenSize();
		this.setLocation((dim.width - this.getWidth()) / 2, 0);
		setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
		addWindowListener(new WindowAdapter() {
			@Override
			public void windowClosing(WindowEvent e) {

			}
		});
		this.setVisible(true);
		pack();

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
			minicap.takeScreenShotOnce();
			minicap.startScreenListener();

		}

		public void paint(Graphics g) {
			try {
				if (image == null)
					return;
				ScreenGUI.this.setSize(image.getWidth(), image.getHeight());
				g.drawImage(image, 0, 0, width, height, null);
				this.setSize(image.getWidth(), image.getHeight() + 300);
				image.flush();
			} catch (Exception e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}

		@Override
		public void frameImageChange(BufferedImage image) {
			this.image = image;
			int w = this.image.getWidth();
			int h = this.image.getHeight();
			float radio = (float) width / (float) w;
			height = (int) (radio * h);
			System.out.println("width : " + w + ",height : " + h);
			this.repaint();
		}
	}

}

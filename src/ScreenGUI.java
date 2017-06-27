
import java.awt.Dimension;
import java.awt.Toolkit;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

import javax.swing.JFrame;
import javax.swing.WindowConstants;

import org.apache.log4j.Logger;

import com.android.ddmlib.IDevice;

import NodeSelection.RealTimeScreenUI;
import Util.ADB;

public class ScreenGUI extends JFrame {
	private static final Logger LOG = Logger.getLogger("PageTest.class");
	
	private RealTimeScreenUI screenPanel = null;
	private IDevice device;
	
	public ScreenGUI() {
		ADB adb = new ADB();
		if (adb.getDevices().length <= 0) {
			LOG.error("无连接设备,请检查");
			return;
		}
		device = adb.getDevices()[0];
		screenPanel = new RealTimeScreenUI(device);
		screenPanel.addMouseListener(screenPanel);
		screenPanel.addMouseMotionListener(screenPanel);
		this.getContentPane().add(screenPanel);
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
	}

	public static void main(String[] args) {
		new ScreenGUI();
	}
}

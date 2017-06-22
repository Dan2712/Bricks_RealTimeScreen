package MiniDecode;
import java.awt.Image;
import java.awt.Toolkit;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.LinkedBlockingQueue;

import javax.imageio.ImageIO;

import org.apache.log4j.Logger;

import com.android.ddmlib.AdbCommandRejectedException;
import com.android.ddmlib.CollectingOutputReceiver;
import com.android.ddmlib.IDevice;
import com.android.ddmlib.IDevice.DeviceUnixSocketNamespace;
import com.android.ddmlib.ShellCommandUnresponsiveException;
import com.android.ddmlib.SyncException;
import com.android.ddmlib.TimeoutException;

import Util.Constant;
import Util.TimeUtil;

/**
 * @author Dan
 */

public class MiniCapUtil implements ScreenSubject{
	private static final Logger LOG = Logger.getLogger(MiniCapUtil.class);
	
	public static final String ABIS_ARM64_V8A = "arm64-v8a";
	public static final String ABIS_ARMEABI_V7A = "armeabi-v7a";
	public static final String ABIS_X86 = "x86";
	public static final String ABIS_X86_64 = "x86_64";
	
	private String REMOTE_PATH = "/data/local/tmp";
	private String ABI_COMMAND = "ro.product.cpu.abi";
	private String SDK_COMMAND = "ro.build.version.sdk";
	private String MINICAP_BIN = "minicap";
	private String MINICAP_SO = "minicap.so";
	private String MINICAP_CHMOD_COMMAND = "chmod 777 %s/%s";
	private String MINICAP_WM_SIZE_COMMAND = "wm size";
	private String MINICAP_START_COMMAND = "LD_LIBRARY_PATH=/data/local/tmp /data/local/tmp/minicap -P %s@%s/90";
	private String MINICAP_TAKESCREENSHOT_COMMAND = "LD_LIBRARY_PATH=/data/local/tmp /data/local/tmp/minicap -P %s@%s/0 -s >%s";
	private String ADB_PULL_COMMAND = "adb -s %s pull %s %s";
	
	private Queue<byte[]> dataQueue = new LinkedBlockingQueue<byte[]>();
	private List<AndroidScreenObserver> observers = new ArrayList<AndroidScreenObserver>();
	
	private Banner banner = new Banner();
	private String size;
	private boolean isRunning = false;
	
	private static final int PORT = 1717;
	private IDevice device;
	private Socket socket;
	
	public MiniCapUtil(IDevice device) {
		this.device = device;
		this.init();
	}
	
	/**
	 * start minicap service, and push the necessary file first
	 */
	private void init() {
		
		String abi = device.getProperty(ABI_COMMAND);
		String sdk = device.getProperty(SDK_COMMAND);
		File miniCapBin = new File(Constant.getMinicapBin(), abi + File.separator + MINICAP_BIN);
		File miniCapSo = new File(Constant.getMinicapSo(), "android-" + sdk
				+ File.separator + abi + File.separator + MINICAP_SO);
		
		// push .so and minicap file to specified path
		try {
			device.pushFile(miniCapBin.getAbsolutePath(), REMOTE_PATH + "/" + MINICAP_BIN);
			device.pushFile(miniCapSo.getAbsolutePath(), REMOTE_PATH + "/" + MINICAP_SO);
			executeShellCommand(String.format(MINICAP_CHMOD_COMMAND,
					REMOTE_PATH, MINICAP_BIN));
			
			// port transmission
			this.device.createForward(PORT, "minicap", DeviceUnixSocketNamespace.ABSTRACT);
			
			// get the screen size
			String output = this.executeShellCommand(MINICAP_WM_SIZE_COMMAND);
			size = output.split(":")[1].trim();
		} catch (SyncException | IOException | AdbCommandRejectedException | TimeoutException e1) {
			e1.printStackTrace();
		}
	}
	
	// judge if the device support minicap
	public boolean isSupoort(){
		String supportCommand = String.format("LD_LIBRARY_PATH=/data/local/tmp /data/local/tmp/minicap -P %s@%s/0 -t", size,size);
		String output = executeShellCommand(supportCommand);
		if(output.trim().endsWith("OK")){
			return true;
		}
		return false;
	}
	
	private String executeShellCommand(String command) {
		CollectingOutputReceiver receiver = new CollectingOutputReceiver();
		
		try {
			device.executeShellCommand(command, receiver, 0);
		} catch (TimeoutException | AdbCommandRejectedException | ShellCommandUnresponsiveException | IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		return receiver.getOutput();
	}
	
	private byte[] subByteArray(byte[] byte1, int start, int end) {
		byte[] byte2 = new byte[0];
		try {
			byte2 = new byte[end - start];
		} catch (NegativeArraySizeException e) {
			e.printStackTrace();
		}
		System.arraycopy(byte1, start, byte2, 0, end - start);
		return byte2;
	}
	
	private static byte[] byteMerger(byte[] byte_1, byte[] byte_2) {
		byte[] byte_3 = new byte[byte_1.length + byte_2.length];
		System.arraycopy(byte_1, 0, byte_3, 0, byte_1.length);
		System.arraycopy(byte_2, 0, byte_3, byte_1.length, byte_2.length);
		return byte_3;
	}
	
	public void startScreenListener() {
		isRunning = true;
		Thread frame = new Thread(new DataFrameCollector());
		frame.start();
		Thread convert = new Thread(new ImageConvert());
		convert.start();
	}

	public void stopScreenListener() {
		isRunning = false;
	}

	public void takeScreenShotOnce() {
		String savePath = "/data/local/tmp/screenshot.jpg";
		String takeScreenShotCommand = String.format(
				MINICAP_TAKESCREENSHOT_COMMAND, size,
				size, savePath);
		String localPath = System.getProperty("user.dir") + "/screenshot.jpg";
		String pullCommand = String.format(ADB_PULL_COMMAND,
				device.getSerialNumber(), savePath, localPath);
		try {
			executeShellCommand(takeScreenShotCommand);
			device.pullFile(savePath, localPath);
			Runtime.getRuntime().exec(pullCommand);
		} catch (IOException e) {
			e.printStackTrace();
		} catch (SyncException e) {
			e.printStackTrace();
		} catch (AdbCommandRejectedException e) {
			e.printStackTrace();
		} catch (TimeoutException e) {
			e.printStackTrace();
		}

	}
	
	private BufferedImage createImage(byte[] data) {
		ByteArrayInputStream bais = new ByteArrayInputStream(data); 
		BufferedImage image = null;
		try {
			image = ImageIO.read(bais);
		} catch (IOException e) {
			e.printStackTrace();
		}
		LOG.info("创建成功");
		try {
			bais.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
		return image;
	}
	
	class DataFrameCollector implements Runnable {

		private InputStream input = null;
		
		@Override
		public void run() {
			LOG.debug("start receiving data");
			
			try {
				String start_command = String.format(MINICAP_START_COMMAND, size, size);
				
				// start the minicap in background
				new Thread(new Runnable() {
					
					@Override
					public void run() {
						LOG.info("minicap start: " + start_command);
						executeShellCommand(start_command);
					}
				}).start();
				
				Thread.sleep(1000);
				
				socket = new Socket("localhost", PORT);
				input = socket.getInputStream();
				
				int len = 4096;
				while(isRunning) {
					byte[] buffer = new byte[len];
					int realLen = input.read(buffer);
					if (buffer.length != realLen) {
						buffer = subByteArray(buffer, 0, realLen);
					}
					dataQueue.add(buffer);
				}
				
			} catch (IOException e) {
				e.printStackTrace();
			} catch (InterruptedException e) {
				e.printStackTrace();
			} finally {
				if (input != null)
					try {
						input.close();
					} catch (IOException e) {
						e.printStackTrace();
					}
				
				if (socket != null && socket.isConnected()) {
					try {
						socket.close();
					} catch (IOException e) {
						e.printStackTrace();
					}
				}
			}
			LOG.debug("stop receiving data");
		}
	
	}
	
	class ImageConvert implements Runnable {
		
		private int readBannerBytes = 0;
		private int bannerLength = 2;
		private int readFrameBytes = 0;
		private int frameLength = 0;
		private byte[] frameBody = new byte[0];
		
		@Override
		public void run() {
			while(isRunning) {
				if (dataQueue.isEmpty())
					continue;
				
				byte[] buffer = dataQueue.poll();
				int cursor = 0;
				int buf_length = buffer.length;
				while(cursor < buf_length) {
					int read_byte = buffer[cursor] & 0xff;				// convert to byte to int
					if (readBannerBytes < bannerLength) {				// 1st buffer zone, and position 2 is banner length
						cursor = parserBanner(cursor, read_byte);
					} else if (readFrameBytes < 4) {					// 2nd buffer zone, and first 4 bytes represents the size
						frameLength += (read_byte << (readFrameBytes * 8)) >>> 0;
						cursor += 1;
						readFrameBytes += 1;
					} else {
						if (buf_length - cursor >= frameLength) {		// capture the image frame body
							LOG.debug("frameBodyLength = " + frameLength);
							byte[] subByte = subByteArray(buffer, cursor,
									cursor + frameLength);
							frameBody = byteMerger(frameBody, subByte);
							if ((frameBody[0] != -1) || frameBody[1] != -40) {
								LOG.error(String
										.format("Frame body does not start with JPG header"));
								return;
							}
							final byte[] finalBytes = subByteArray(frameBody,
									0, frameBody.length);
						
							new Thread(new Runnable() {					// convert to bufferedimage

								@Override
								public void run() {
									// TODO Auto-generated method stub
									BufferedImage image = createImage(finalBytes);
									notifyObservers(image);
								}
							}).start();

							cursor += frameLength;
							restore();
						} else {
							LOG.debug("所需数据大小 : " + frameLength);
							byte[] subByte = subByteArray(buffer, cursor, buf_length);
							frameBody = byteMerger(frameBody, subByte);
							frameLength -= (buf_length - cursor);
							readFrameBytes += (buf_length - cursor);
							cursor = buf_length;
						}
					}
					
				}
			}
		}
		
		private void restore() {
			frameLength = 0;
			readFrameBytes = 0;
			frameBody = new byte[0];
		}
		
		private int parserBanner(int cursor, int read_byte) {
			switch (readBannerBytes) {
			case 0:
				// version
				banner.setVersion(read_byte);
				break;
			case 1:
				// banner length
				bannerLength = read_byte;
				banner.setLength(read_byte);
				break;
			case 2:
			case 3:
			case 4:
			case 5:
				// pid
				int pid = banner.getPid();
				pid += (read_byte << ((readBannerBytes - 2) * 8)) >>> 0;
				banner.setPid(pid);
				break;
			case 6:
			case 7:
			case 8:
			case 9:
				// real width
				int realWidth = banner.getReadWidth();
				realWidth += (read_byte << ((readBannerBytes - 6) * 8)) >>> 0;
				banner.setReadWidth(realWidth);
				break;
			case 10:
			case 11:
			case 12:
			case 13:
				// real height
				int realHeight = banner.getReadHeight();
				realHeight += (read_byte << ((readBannerBytes - 10) * 8)) >>> 0;
				banner.setReadHeight(realHeight);
				break;
			case 14:
			case 15:
			case 16:
			case 17:
				// virtual width
				int virtualWidth = banner.getVirtualWidth();
				virtualWidth += (read_byte << ((readBannerBytes - 14) * 8)) >>> 0;
				banner.setVirtualWidth(virtualWidth);
				break;
			case 18:
			case 19:
			case 20:
			case 21:
				// virtual height
				int virtualHeight = banner.getVirtualHeight();
				virtualHeight += (read_byte << ((readBannerBytes - 18) * 8)) >>> 0;
				banner.setVirtualHeight(virtualHeight);
				break;
			case 22:
				// orientation
				banner.setOrientation(read_byte * 90);
				break;
			case 23:
				// quirks
				banner.setQuirks(read_byte);
				break;
			}

			cursor += 1;
			readBannerBytes += 1;

			if (readBannerBytes == bannerLength) {
				LOG.debug(banner.toString());
			}
			return cursor;
		}
		
	}
	
	public void registerObserver(AndroidScreenObserver o) {
		// TODO Auto-generated method stub
		observers.add(o);

	}
	
	public void removeObserver(AndroidScreenObserver o) {
		// TODO Auto-generated method stub
		int index = observers.indexOf(o);
		if (index != -1) {
			observers.remove(o);
		}
	}
	
	@Override
	public void notifyObservers(BufferedImage image) {
		for (AndroidScreenObserver observer : observers) {
			observer.frameImageChange(image);
		}
		// TODO Auto-generated method stub

	}
}
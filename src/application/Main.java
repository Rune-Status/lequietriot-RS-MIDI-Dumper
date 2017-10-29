package application;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import javax.swing.JDialog;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JSeparator;

import com.alex.store.Index;
import com.alex.store.ReferenceTable;
import com.alex.store.Store;

import application.util.MidiDecoder;
import net.openrs.cache.Cache;
import net.openrs.cache.FileStore;
import net.openrs.cache.track.Track;
import net.openrs.cache.track.Tracks;

public class Main extends JFrame {
	
	private static final long serialVersionUID = 1L;
	public static String RSHDCachePath;
	public static String OSRSCachePath;
	
	public static void main(String[] args) throws IOException {
		run();
		
	}
	public static void run() {
		try {
		Main frame = new Main();
		JFrame.setDefaultLookAndFeelDecorated(true);
		JDialog.setDefaultLookAndFeelDecorated(true);
		frame.setTitle("RuneScape MIDI Dumper");
		frame.setVisible(true);
		frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		frame.setExtendedState(JFrame.MAXIMIZED_BOTH);
		frame.setVisible(true);
		
		JMenuBar menuBar = new JMenuBar();
		frame.setJMenuBar(menuBar);

		JMenu file_menu = new JMenu("File");
		menuBar.add(file_menu);
		
		JSeparator seperator = new JSeparator();

		JMenuItem load_OSRS_cache = new JMenuItem("Dump OSRS Cache Music");
		
		JMenuItem load_RSHD_cache = new JMenuItem("Dump RSHD Cache Music");
		
		load_RSHD_cache.addActionListener(new ActionListener() {
			
			public void actionPerformed(ActionEvent arg1) {
				try {
					promptFolderChooseRSHD(arg1);
				} catch (IOException e) {
					e.printStackTrace();
				}
			}

			public void promptFolderChooseRSHD(ActionEvent arg1) throws IOException {
					JFileChooser RSHDchooser = new JFileChooser();
					RSHDchooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
					if (RSHDchooser.showOpenDialog(RSHDchooser) == JFileChooser.APPROVE_OPTION) {
						String selected = RSHDchooser.getSelectedFile().toString();
						RSHDCachePath = selected + "/";
					
					Store store = new Store(RSHDCachePath);
					Index musicIndex = store.getIndexes()[6];
					Index music2Index = store.getIndexes()[11];
					
					File track1Dir = new File(RSHDCachePath + "/MIDI Dump/Music/");

					if (!track1Dir.exists()) {
						track1Dir.mkdirs();
					}

					File track2Dir = new File(RSHDCachePath + "/MIDI Dump/Fanfares/");

					if (!track2Dir.exists()) {
						track2Dir.mkdirs();
					}
					
					for (int i = 0; i < musicIndex.getValidArchivesCount(); i++) {
						
					if (!store.getIndexes()[6].fileExists(i, 0))
						return;
					
					byte[] data = store.getIndexes()[6].getFile(i, 0);
					FileOutputStream fos = new FileOutputStream(RSHDCachePath + "MIDI Dump/Music/" + i + ".mid");
					DataOutputStream dos = new DataOutputStream(fos);
					
					MidiDecoder mididecoder = new MidiDecoder();
					mididecoder.decode(data, true);
					
					try {
					dos.write(mididecoder.decoded);
					}
					catch (IOException e) {
						e.printStackTrace();
					}
					}
					try {
					
					for (int i2 = 0; i2 < music2Index.getValidArchivesCount(); i2++) {
						
						if (!store.getIndexes()[11].fileExists(i2, 0))
							return;
						
						byte[] data2 = store.getIndexes()[11].getFile(i2, 0);
						FileOutputStream fos2 = new FileOutputStream(RSHDCachePath + "MIDI Dump/Fanfares/" + i2 + ".mid");
						DataOutputStream dos2 = new DataOutputStream(fos2);
						
						MidiDecoder mididecoder2 = new MidiDecoder();
						mididecoder2.decode(data2, true);
						
						try {
						dos2.write(mididecoder2.decoded);
					}
						catch (IOException e) {
							e.printStackTrace();
					}
			}
					}
					finally {
					}
		}
			}
		});
		file_menu.add(load_RSHD_cache);
		
		file_menu.addSeparator();
		
		load_OSRS_cache.addActionListener(new ActionListener() {
			
			public void actionPerformed(ActionEvent arg0) {
				try {
					promptFolderChooseOSRS(arg0);
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		
			public void promptFolderChooseOSRS(ActionEvent arg0) throws IOException {
				JFileChooser chooser = new JFileChooser();
				chooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
				if (chooser.showOpenDialog(chooser) == JFileChooser.APPROVE_OPTION) {
					String selected = chooser.getSelectedFile().toString();
					OSRSCachePath = selected + "/";
					
					System.out.print("Dumping MIDI files from " + OSRSCachePath);
					
				File track1Dir = new File(OSRSCachePath + "/MIDI Dump/Music/");

				if (!track1Dir.exists()) {
					track1Dir.mkdirs();
				}

				File track2Dir = new File(OSRSCachePath + "/MIDI Dump/Fanfares/");

				if (!track2Dir.exists()) {
					track2Dir.mkdirs();
				}

				try (Cache cache = new Cache(FileStore.open(OSRSCachePath))) {
					Tracks list = new Tracks();
					list.initialize(cache);

					for (int i = 0; i < list.getTrack1Count(); i++) {
						Track track1 = list.getTrack1(i);

						if (track1 == null) {
							continue;
						}

						try (DataOutputStream dos = new DataOutputStream(
								new FileOutputStream(new File(track1Dir, i + ".mid")))) {
							dos.write(track1.getDecoded());
						}

						double progress = (double) (i + 1) / list.getTrack1Count() * 100;
						System.out.printf("dumping track1 %d out of %d %.2f%s\n", (i + 1), list.getTrack1Count(), progress, "%");
					}

					for (int i = 0; i < list.getTrack2Count(); i++) {
						Track track2 = list.getTrack2(i);

						if (track2 == null) {
							continue;
						}

						try (DataOutputStream dos = new DataOutputStream(
								new FileOutputStream(new File(track2Dir, i + ".mid")))) {
							dos.write(track2.getDecoded());
						}

						double progress = (double) (i + 1) / list.getTrack2Count() * 100;
						System.out.printf("dumping track2 %d out of %d %.2f%s\n", (i + 1), list.getTrack2Count(), progress, "%");
					}
				}
			}
			}
		});
		file_menu.add(load_OSRS_cache);
	
	} catch (Exception e) {
		e.printStackTrace();
	}
		
		
		
		}
	}
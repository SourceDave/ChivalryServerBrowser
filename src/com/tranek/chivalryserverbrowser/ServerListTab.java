package com.tranek.chivalryserverbrowser;

import java.awt.BorderLayout;
import java.io.File;
import java.util.Vector;

import javax.swing.JPanel;
import javax.swing.table.DefaultTableModel;

import com.almworks.sqlite4java.SQLiteConnection;
import com.almworks.sqlite4java.SQLiteException;
import com.almworks.sqlite4java.SQLiteStatement;

/**
 * 
 * Superclass for the server list tabs. All server list tabs must extend this class.
 *
 */
@SuppressWarnings("serial")
public abstract class ServerListTab extends JPanel implements ServerListInterface {

	/** A reference to the MainWindow. */
	protected final MainWindow mw;
	/** The server filters for this server list. */
	protected final ServerFilters sf = new ServerFilters();
	/** The refresher for this server list. */
	protected Refresher refresher;
	/** The filters panel for this server list. */
	protected final FiltersPanel fp;
	/** The server table for this server list. */
	protected final ServerTable st;
	/** The name of this server list's filter database. */
	protected String filterTableName;
	
	/**
	 * Creates a new ServerListTab.
	 * 
	 * @param mw the MainWindow
	 */
	public ServerListTab(MainWindow mw) {
		super();
		this.mw = mw;
		fp = new FiltersPanel(this);
		st = new ServerTable(this);
		initialize();
	}
	
	/**
	 * Sets up the {@link JPanel} layout and adds the {@link FiltersPanel} and
	 * {@link ServerTable} to it.
	 */
	public void initialize() {
		setLayout(new BorderLayout(0, 0));
		add(st, BorderLayout.NORTH);
		add(fp, BorderLayout.CENTER);
	}
	
	/**
	 * Refreshes the server list. It does a final check to make sure that its refresher
	 * has been stopped before updating the server filters, clearing the table, and then
	 * calling {@link #startRefresher()}.
	 */
	@Override
	public void RefreshServers() {
		if ( refresher != null && refresher.isRefreshing()) {
			refresher.stopRefreshing();
			mw.printlnMC("Stopping from refresh function. If you see this message, please let me know on the forums >:(");
		}
		
		updateFilters();

		((DefaultTableModel)st.dataModel).setRowCount(0);
		
		startRefresher();
	}
	
	/**
	 * Refreshes the server list.
	 */
	protected abstract void startRefresher();
	
	/**
	 * Updates the {@link ServerFilters} from the {@link FiltersPanel}.
	 */
	public void updateFilters() {
		sf.name = fp.serverNameFilter.getText();
		sf.type = (String) fp.gameModeList.getSelectedItem();
		if ( !fp.maxPingFilter.getText().equals("") ) {
			sf.maxPing = Integer.parseInt(fp.maxPingFilter.getText());
		} else {
			sf.maxPing = -1;
		}
		sf.hidePassword = fp.chckbxHidePasswordedServers.isSelected();
		sf.hideEmpty = fp.chckbxHideEmptyServers.isSelected();
		sf.hideFull = fp.chckbxHideFullServers.isSelected();
		if ( !fp.minRankFilter.getText().equals("") ) {
			sf.minRank = Integer.parseInt(fp.minRankFilter.getText());
		} else {
			sf.minRank = -1;
		}
		if ( !fp.maxRankFilter.getText().equals("") ) {
			sf.maxRank = Integer.parseInt(fp.maxRankFilter.getText());
		} else {
			sf.maxRank = -1;
		}
		sf.officialservers = fp.chckbxOfficialServersOnly.isSelected();
		sf.perspective = fp.cBPerspective.getSelectedIndex();
		sf.numThreads = (int) fp.spNumThreads.getValue();
	}
		
	/**
	 * Sets the {@link FiltersPanel} values. This is typically called after loading
	 * the saved filter settings from the database.
	 * 
	 * @param name the server name filter
	 * @param type the gamemode filter
	 * @param minrank the server's minimum rank filter
	 * @param maxrank the server's maximum rank filter
	 * @param maxping the server's maximum ping filter
	 * @param hidepassword hide servers with a password filter
	 * @param hideempty hide empty servers filter
	 * @param hidefull hide full servers filter
	 * @param officialonly show only official servers filter
	 * @param perspective the server's allowed player perspectives filter
	 * @param numthreads the number of threads to query servers with
	 */
	public void setUIFilters(String name, String type, int minrank, int maxrank,
			int maxping, boolean hidepassword, boolean hideempty, boolean hidefull,
			boolean officialonly, int perspective, int numthreads) {
		fp.serverNameFilter.setText(name);
		if ( type.equals("All") ) {
			type = "ALL";
		}
		fp.gameModeList.setSelectedItem(type);
		if ( minrank > -1 ) {
			fp.minRankFilter.setText("" + minrank);
		}
		if ( maxrank > -1 ) {
			fp.maxRankFilter.setText("" + maxrank);
		}
		if ( maxping > -1 ) {
			fp.maxPingFilter.setText("" + maxping);
		}
		if ( hidepassword ) {
			fp.chckbxHidePasswordedServers.setSelected(true);
		}
		if ( hideempty ) {
			fp.chckbxHideEmptyServers.setSelected(true);
		}
		if ( hidefull ) {
			fp.chckbxHideFullServers.setSelected(true);
		}
		if ( officialonly ) {
			fp.chckbxOfficialServersOnly.setSelected(true);
			fp.serverNameFilter.setText("");
			fp.serverNameFilter.setEnabled(false);
		}
		fp.cBPerspective.setSelectedIndex(perspective);
		fp.spNumThreads.setValue(numthreads);
	}
	
	/**
	 * Load the server filters from the database and call 
	 * {@link #setUIFilters(String, String, int, int, int, boolean, boolean, boolean, boolean, int, int)}
	 */
	public void loadFilters() {
		SQLiteConnection db = new SQLiteConnection(new File("browserdb"));
		try {
			db.open(true);		
			SQLiteStatement st = db.prepare("CREATE TABLE IF NOT EXISTS " + filterTableName +
					"(" +
					"id INTEGER PRIMARY KEY AUTOINCREMENT," +
					"name varchar(255) not null default ''," +
					"type varchar(255) not null default ''," +
					"minrank int not null default -1," +
					"maxrank int not null default -1," +
					"maxping int not null default -1," +
					"hidepassword int not null default 0," +
					"hideempty int not null default 0," +
					"hidefull int not null default 0," +
					"officialonly int not null default 0," +
					"perspective int not null default 0," +
					"numthreads int not null default 0" +
					")");
			try {
				st.step();
			} finally {
				st.dispose();
			}
			
			boolean hasEntry = false;
			st = db.prepare("SELECT COUNT(*) FROM " + filterTableName);
			try {
				st.step();
				if ( st.columnInt(0) > 0 ) {
					hasEntry = true;
				}
			} finally {
				st.dispose();
			}

			if ( hasEntry ) {
				String name = "";
				String type = "";
				int minrank = -1;
				int maxrank = -1;
				int maxping = -1;
				boolean hidepassword = false;
				boolean hideempty = false;
				boolean hidefull = false;
				boolean officialonly = false;
				int perspective = 0;
				int numthreads = 8;
				st = db.prepare("SELECT * FROM " + filterTableName + " WHERE id=1");
				try {
					st.step();
					name = st.columnString(1);
					type = st.columnString(2);
					minrank = st.columnInt(3);
					maxrank = st.columnInt(4);
					maxping = st.columnInt(5);
					if ( st.columnInt(6) == 1 ) {
						hidepassword = true;
					}
					if ( st.columnInt(7) == 1 ) {
						hideempty = true;
					}
					if ( st.columnInt(8) == 1 ) {
						hidefull = true;
					}
					if ( st.columnInt(9) == 1 ) {
						officialonly = true;
					}
					perspective = st.columnInt(10);
					numthreads = st.columnInt(11);
					
					setUIFilters(name, type, minrank, maxrank, maxping, hidepassword, hideempty, hidefull,
							officialonly, perspective, numthreads);
				} finally {
					st.dispose();
				}
			} else {
				System.out.println("No entries");
			}
			
		} catch (SQLiteException e) {
			e.printStackTrace();
		}
		db.dispose();
	}
	
	/**
	 * Call {@link #updateFilters()} and then save the server filters to the database.
	 */
	public void saveFilters() {
		updateFilters();
		
		SQLiteConnection db = new SQLiteConnection(new File("browserdb"));
		try {
			db.open(true);		
			SQLiteStatement st = db.prepare("CREATE TABLE IF NOT EXISTS " + filterTableName +
					"(" +
					"id INTEGER PRIMARY KEY AUTOINCREMENT," +
					"name varchar(255) not null default ''," +
					"type varchar(255) not null default ''," +
					"minrank int not null default -1," +
					"maxrank int not null default -1," +
					"maxping int not null default -1," +
					"hidepassword int not null default 0," +
					"hideempty int not null default 0," +
					"hidefull int not null default 0," +
					"officialonly int not null default 0," +
					"perspective int not null default 0," +
					"numthreads int not null default 0" +
					")");
			try {
				st.step();
			} finally {
				st.dispose();
			}
			
			int password = 0;
			if ( sf.hidePassword ) {
				password = 1;
			}
			int empty = 0;
			if ( sf.hideEmpty ) {
				empty = 1;
			}
			int full = 0;
			if ( sf.hideFull ) {
				full = 1;
			}
			int official = 0;
			if ( sf.officialservers ) {
				official = 1;
			}
			
			st = db.prepare("INSERT OR REPLACE INTO " + filterTableName + " (id, name, type, minrank, maxrank, maxping, " +
					"hidepassword, hideempty, hidefull, officialonly, perspective, numthreads) " +
					"VALUES (  1, '" + sf.name + "', '" + sf.type + "', " + sf.minRank + ", " + sf.maxRank +
					", " + sf.maxPing + ", " + password + ", " + empty + ", " + full + ", " + official + ", " +
					sf.perspective + ", " + sf.numThreads + ")");
			try {
				st.step();
			} finally {
				st.dispose();
			}
			
		} catch (SQLiteException e) {
			e.printStackTrace();
		}
		db.dispose();
	}
	
	/**
	 * Get the reference to the {@link MainWindow}.
	 */
	@Override
	public MainWindow getMW() {
		return mw;
	}

	/**
	 * Get this server list tab's currently queried server list from the {@link MainWindow}.
	 * 
	 * @see ChivServer
	 */
	@Override
	public abstract Vector<ChivServer> getServerList();

	/**
	 * Get a reference to this server list's {@link ServerFilters}.
	 */
	@Override
	public ServerFilters getFilters() {
		return sf;
	}

	/**
	 * Get this server lists table model.
	 * 
	 * @see DefaultTableModel
	 */
	@Override
	public DefaultTableModel getTableModel() {
		return (DefaultTableModel)(st.dataModel);
	}

	/**
	 * Gets whether or not this server list is currently refreshing.
	 * 
	 * @see Refresher#isRefreshing()
	 */
	@Override
	public boolean isRefreshing() {
		if ( refresher == null ) {
			return false;
		}	
		return refresher.isRefreshing();
	}

}

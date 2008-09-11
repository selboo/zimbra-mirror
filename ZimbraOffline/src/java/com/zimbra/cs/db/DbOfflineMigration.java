package com.zimbra.cs.db;

import java.util.ArrayList;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import org.apache.derby.iapi.error.StandardException;

import com.zimbra.cs.db.DbPool.Connection;

public class DbOfflineMigration {

	public void testRun() throws Exception {
		runInternal(true);
	}
	
	public void run() throws Exception {
		runInternal(false);
	}
	
	public void runInternal(boolean isTestRun) throws Exception {
        Connection conn = null;
        PreparedStatement stmt = null;
        ResultSet rs = null;
        try {
            conn = DbPool.getConnection();
            stmt = conn.prepareStatement("SELECT value FROM zimbra.config WHERE name = 'db.version'");
            rs = stmt.executeQuery();
            rs.next();
            int oldDbVersion = Integer.parseInt(rs.getString(1));
            rs.close();
            stmt.close();
            
            int newDbVersion = Integer.parseInt(Versions.DB_VERSION);
            System.out.println("oldDbVersion=" + oldDbVersion + " newDbVersion=" + newDbVersion);
            
            if (oldDbVersion == newDbVersion)
            	return;
            
            switch (oldDbVersion) {
            case 51:
            	migrateFromVersion51(conn, isTestRun);
            	break;
            case 52:
            	migrateFromVersion52(conn, isTestRun);
            	break;
            default:
            	throw new DbUnsupportedVersionException();
            }
        } catch (Exception x) {
        	x.printStackTrace(System.err);
        	if (x.getCause() instanceof SQLException) {
        		SQLException sqlException = (SQLException)x.getCause();
        		if (sqlException.getSQLState().equalsIgnoreCase("XJ040")) {
    				throw new DbExclusiveAccessException();
        		} else if (sqlException.getSQLState().equalsIgnoreCase("XJ004")) {
        			throw new DbNotFoundException();
        		} else {
        			throw new DbDataCorruptedException();
        		}
        	} else if (x.getCause() instanceof StandardException) {
        		throw new DbUpdateException();
        	} else {
        		throw x;
        	}
        } finally {
            DbPool.closeResults(rs);
            DbPool.closeStatement(stmt);
            DbPool.quietClose(conn);
            DbPool.close();
        }
	}
	
	private void migrateFromVersion51(Connection conn, boolean isTestRun) throws Exception {
        PreparedStatement stmt = null;
        boolean isSuccess = false;
        try {
            stmt = conn.prepareStatement("ALTER TABLE zimbra.mailbox ADD COLUMN idx_deferred_count INTEGER NOT NULL DEFAULT 0");
            stmt.executeUpdate();
            stmt.close();
           
            stmt = conn.prepareStatement("DELETE FROM zimbra.directory_attrs WHERE name='offlineModifiedAttrs'");
            stmt.executeUpdate();
            stmt.close();
            
            stmt = conn.prepareStatement("UPDATE zimbra.config set value='52' where name='db.version'");
            stmt.executeUpdate();
            stmt.close();
            
            isSuccess = true;
        } finally {
            DbPool.closeStatement(stmt);
            if (isTestRun || !isSuccess)
            	conn.rollback();
            else
            	conn.commit();
        }
	}
	
	private void migrateFromVersion52(Connection conn, boolean isTestRun) throws Exception {
        PreparedStatement stmt = null;
        boolean isSuccess = false;
        ArrayList<String> mboxgroups = new ArrayList<String>();
        try {
            stmt = conn.prepareStatement("SELECT schemaname FROM SYS.SYSSCHEMAS");
            //stmt = conn.prepareStatement("SHOW DATABASES LIKE 'mboxgroup%'");
            ResultSet rs = stmt.executeQuery();
            while (rs.next()) {
            	String name = rs.getString(1);
            	if (name.toLowerCase().startsWith("mboxgroup"))
            		mboxgroups.add(name);
            }
            	
            rs.close();
            stmt.close();

            for (String mboxgroup : mboxgroups) {
                String stmtStr = 
                	"CREATE TABLE " + mboxgroup + ".data_source_item (" +
            		   "mailbox_id     INTEGER NOT NULL," +
            		   "data_source_id CHAR(36) NOT NULL," +
            		   "item_id        INTEGER NOT NULL," +
            		   "remote_id      VARCHAR(255) NOT NULL," +
            		   "metadata       CLOB," +
            		   "PRIMARY KEY (mailbox_id, item_id)," +
            		   "CONSTRAINT fk_data_source_item_mailbox_id FOREIGN KEY (mailbox_id) REFERENCES zimbra.mailbox(id)" +
            		")";
                stmt = conn.prepareStatement(stmtStr);
                stmt.executeUpdate();
                stmt.close();
                stmtStr = 
            		"CREATE UNIQUE INDEX i_remote_id ON " +
            		mboxgroup + ".data_source_item (mailbox_id, data_source_id, remote_id)";
                stmt = conn.prepareStatement(stmtStr);
                stmt.executeUpdate();
                stmt.close();
            }
            
            stmt = conn.prepareStatement("UPDATE zimbra.config set value='53' where name='db.version'");
            stmt.executeUpdate();
            stmt.close();
            
            isSuccess = true;
        } finally {
            DbPool.closeStatement(stmt);
            if (isTestRun || !isSuccess)
            	conn.rollback();
            else
            	conn.commit();
        }
	}
	
	public static void main(String[] args) throws Exception {
		System.setProperty("zimbra.config", "/opt/zimbra/zdesktop dev/conf/localconfig.xml");
		
		new DbOfflineMigration().testRun();
	}
}
